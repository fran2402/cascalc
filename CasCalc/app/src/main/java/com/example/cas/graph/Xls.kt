package com.example.cas.graph

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Reads the old binary Excel format (.xls, Excel 97–2003, and the Excel 5/95 one before it):
 * an OLE compound file holding a "Workbook" stream of BIFF records. Only cell values are read,
 * each sheet as rows of cells; a formula gives the value it was last worked out to.
 */
internal object Xls {
    /** The sheets in workbook order, named, each as rows of cells; empty if the file can't be read. */
    fun sheets(bytes: ByteArray): List<Pair<String, List<List<String>>>> =
        runCatching { workbook(stream(bytes, setOf("Workbook", "Book")) ?: return emptyList()) }.getOrDefault(emptyList())

    // --- The compound file: a little FAT file system in 512-byte (or 4096-byte) sectors ---

    private const val END_OF_CHAIN = -2
    private const val FREE = -1

    private fun stream(file: ByteArray, names: Set<String>): ByteArray? {
        val b = ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN)
        val sectorSize = 1 shl b.getShort(0x1E).toInt()
        val miniSize = 1 shl b.getShort(0x20).toInt()
        if (sectorSize !in listOf(512, 4096) || miniSize > sectorSize) return null
        val sectors = (file.size - sectorSize) / sectorSize
        fun offset(s: Int) = (s + 1) * sectorSize

        // Where the FAT's own sectors are: 109 in the header, the rest in a chain of DIFAT sectors.
        val fatSectors = ArrayList<Int>()
        for (k in 0 until 109) b.getInt(0x4C + 4 * k).takeIf { it >= 0 }?.let { fatSectors += it }
        var difat = b.getInt(0x44)
        var guard = 0
        while (difat in 0 until sectors && guard++ < sectors) {
            val per = sectorSize / 4 - 1
            for (k in 0 until per) b.getInt(offset(difat) + 4 * k).takeIf { it >= 0 }?.let { fatSectors += it }
            difat = b.getInt(offset(difat) + 4 * per)
        }
        val perFat = sectorSize / 4
        val fat = IntArray(fatSectors.size * perFat) { k ->
            val s = fatSectors[k / perFat]
            if (s in 0 until sectors) b.getInt(offset(s) + 4 * (k % perFat)) else FREE
        }
        fun chain(start: Int, next: IntArray, count: Int): List<Int> {
            val out = ArrayList<Int>()
            var s = start
            while (s in 0 until count && s < next.size && out.size <= count) { out += s; s = next[s] }
            return out
        }
        fun read(start: Int): ByteArray {
            val ids = chain(start, fat, sectors)
            val out = ByteArray(ids.size * sectorSize)
            ids.forEachIndexed { k, s -> System.arraycopy(file, offset(s), out, k * sectorSize, sectorSize) }
            return out
        }

        // The directory: 128-byte entries, the first the root (whose data is the mini stream).
        val dir = ByteBuffer.wrap(read(b.getInt(0x30))).order(ByteOrder.LITTLE_ENDIAN)
        fun name(e: Int): String {
            val len = (dir.getShort(e * 128 + 0x40).toInt() / 2 - 1).coerceIn(0, 31)
            return String(CharArray(len) { dir.getChar(e * 128 + 2 * it) })
        }
        val entries = dir.capacity() / 128
        val entry = (1 until entries).firstOrNull { e -> dir.get(e * 128 + 0x42).toInt() == 2 && name(e) in names } ?: return null
        val start = dir.getInt(entry * 128 + 0x74)
        val size = dir.getInt(entry * 128 + 0x78).toLong().and(0xFFFFFFFFL).toInt()
        val cutoff = b.getInt(0x38)
        if (size >= cutoff) return read(start).copyOf(size)

        // A small stream lives in the root's mini stream, chained by the mini FAT.
        val miniStream = read(dir.getInt(0x74))
        val miniFatBytes = ByteBuffer.wrap(read(b.getInt(0x3C))).order(ByteOrder.LITTLE_ENDIAN)
        val miniFat = IntArray(miniFatBytes.capacity() / 4) { miniFatBytes.getInt(4 * it) }
        val ids = chain(start, miniFat, miniStream.size / miniSize)
        val out = ByteArray(ids.size * miniSize)
        ids.forEachIndexed { k, s -> System.arraycopy(miniStream, s * miniSize, out, k * miniSize, miniSize) }
        return out.copyOf(minOf(size, out.size))
    }

    // --- BIFF records ---

    private class Record(val type: Int, val data: ByteBuffer, val start: Int)

    private fun records(w: ByteArray, from: Int): Sequence<Record> = sequence {
        var p = from
        while (p + 4 <= w.size) {
            val type = (w[p].toInt() and 0xFF) or ((w[p + 1].toInt() and 0xFF) shl 8)
            val len = (w[p + 2].toInt() and 0xFF) or ((w[p + 3].toInt() and 0xFF) shl 8)
            if (p + 4 + len > w.size) break
            yield(Record(type, ByteBuffer.wrap(w, p + 4, len).slice().order(ByteOrder.LITTLE_ENDIAN), p))
            p += 4 + len
        }
    }

    private const val BOF = 0x0809
    private const val EOF = 0x000A
    private const val CONTINUE = 0x003C
    private const val FILEPASS = 0x002F
    private const val BOUNDSHEET = 0x0085
    private const val SST = 0x00FC
    private const val LABELSST = 0x00FD
    private const val LABEL = 0x0204
    private const val RSTRING = 0x00D6
    private const val NUMBER = 0x0203
    private const val RK = 0x027E
    private const val MULRK = 0x00BD
    private const val FORMULA = 0x0006
    private const val STRING = 0x0207
    private const val BOOLERR = 0x0205

    private fun ByteBuffer.u8(at: Int) = get(at).toInt() and 0xFF
    private fun ByteBuffer.u16(at: Int) = getShort(at).toInt() and 0xFFFF

    private fun workbook(w: ByteArray): List<Pair<String, List<List<String>>>> {
        val sheets = ArrayList<Pair<String, Int>>()
        var biff8 = true
        val sst = ArrayList<String>()
        val globals = records(w, 0).toList().let { all -> all.take(all.indexOfFirst { it.type == EOF }.let { if (it < 0) all.size else it + 1 }) }
        globals.forEachIndexed { k, r ->
            when (r.type) {
                BOF -> if (k == 0) biff8 = r.data.u16(0) == 0x0600
                // Encrypted workbooks can't be read without the password.
                FILEPASS -> return emptyList()
                BOUNDSHEET -> if (r.data.u8(5) == 0) {
                    val at = r.data.getInt(0)
                    val n = r.data.u8(6)
                    val name = if (biff8) Text(listOf(r.data), 7).chars(n, r.data.u8(7) and 1 == 1, skipFlag = true) else Text(listOf(r.data), 7).chars(n, false)
                    sheets += name to at
                }
                SST -> {
                    val parts = listOf(r.data) + globals.drop(k + 1).takeWhile { it.type == CONTINUE }.map { it.data }
                    sst += sharedStrings(parts)
                }
            }
        }
        return sheets.map { (name, at) -> name to sheet(w, at, sst, biff8) }
    }

    /** Reads across a record and its CONTINUE records; characters split by one restate their width. */
    private class Text(val parts: List<ByteBuffer>, var pos: Int = 0) {
        var part = 0
        private fun cur(): ByteBuffer { while (part < parts.size - 1 && pos >= parts[part].limit()) { pos -= parts[part].limit(); part++ }; return parts[part] }
        fun u8(): Int { val p = cur(); return p.get(pos++).toInt() and 0xFF }
        fun u16() = u8() or (u8() shl 8)
        fun u32() = u16() or (u16() shl 16)
        fun skip(n: Int) { pos += n }

        /** [n] characters, 16-bit ones if [wide]; [skipFlag] when the flags byte is still to come. */
        fun chars(n: Int, wide0: Boolean, skipFlag: Boolean = false): String {
            if (skipFlag) u8()
            var wide = wide0
            val sb = StringBuilder(n)
            while (sb.length < n) {
                if (pos >= parts[part].limit()) {
                    if (part >= parts.size - 1) break
                    pos -= parts[part].limit(); part++
                    wide = (parts[part].get(pos++).toInt() and 1) == 1
                }
                val p = parts[part]
                sb.append(if (wide) (p.getShort(pos).toInt() and 0xFFFF).toChar() else (p.get(pos).toInt() and 0xFF).toChar())
                pos += if (wide) 2 else 1
            }
            return sb.toString()
        }
    }

    private fun sharedStrings(parts: List<ByteBuffer>): List<String> {
        val t = Text(parts)
        t.skip(4)
        val count = t.u32()
        val out = ArrayList<String>(minOf(count, 100_000))
        repeat(count) {
            if (t.part == parts.size - 1 && t.pos >= parts.last().limit()) return out
            val n = t.u16()
            val flags = t.u8()
            val runs = if (flags and 8 != 0) t.u16() else 0
            val ext = if (flags and 4 != 0) t.u32() else 0
            out += t.chars(n, flags and 1 == 1)
            t.skip(4 * runs + ext)
        }
        return out
    }

    private fun rk(v: Int): Double {
        val d = if (v and 2 != 0) (v shr 2).toDouble() else java.lang.Double.longBitsToDouble((v and 3.inv()).toLong() shl 32)
        return if (v and 1 != 0) d / 100 else d
    }

    private fun number(v: Double) = if (v.isFinite()) v.toString() else ""

    private fun sheet(w: ByteArray, at: Int, sst: List<String>, biff8: Boolean): List<List<String>> {
        if (at !in w.indices) return emptyList()
        val rows = sortedMapOf<Int, MutableMap<Int, String>>()
        fun put(r: Int, c: Int, v: String) { if (r <= 200_000 && c <= 1000) rows.getOrPut(r) { HashMap() }[c] = v }
        var stringCell: Pair<Int, Int>? = null
        var depth = 0
        for (rec in records(w, at)) {
            val d = rec.data
            when (rec.type) {
                // Charts and the like nest their own BOF … EOF inside a sheet.
                BOF -> depth++
                EOF -> { depth--; if (depth <= 0) break }
                LABELSST -> put(d.u16(0), d.u16(2), sst.getOrElse(d.getInt(6)) { "" })
                NUMBER -> put(d.u16(0), d.u16(2), number(d.getDouble(6)))
                RK -> put(d.u16(0), d.u16(2), number(rk(d.getInt(6))))
                MULRK -> {
                    val r = d.u16(0); val first = d.u16(2)
                    for (k in 0 until (d.limit() - 6) / 6) put(r, first + k, number(rk(d.getInt(4 + 6 * k + 2))))
                }
                LABEL, RSTRING -> {
                    val n = d.u16(6)
                    put(d.u16(0), d.u16(2), if (biff8) Text(listOf(d), 9).chars(n, d.u8(8) and 1 == 1) else Text(listOf(d), 8).chars(n, false))
                }
                BOOLERR -> put(d.u16(0), d.u16(2), if (d.u8(7) == 1) "#ERROR" else d.u8(6).toString())
                FORMULA -> {
                    val r = d.u16(0); val c = d.u16(2)
                    if (d.u8(12) == 0xFF && d.u8(13) == 0xFF) when (d.u8(6)) {
                        0 -> stringCell = r to c // its text is in the STRING record after it
                        1 -> put(r, c, d.u8(8).toString())
                        2 -> put(r, c, "#ERROR")
                    } else put(r, c, number(d.getDouble(6)))
                }
                STRING -> stringCell?.let { (r, c) ->
                    val n = d.u16(0)
                    put(r, c, if (biff8) Text(listOf(d), 3).chars(n, d.u8(2) and 1 == 1) else Text(listOf(d), 2).chars(n, false))
                    stringCell = null
                }
            }
        }
        return rows.values.map { cells -> List((cells.keys.maxOrNull() ?: -1) + 1) { cells[it] ?: "" } }
    }
}
