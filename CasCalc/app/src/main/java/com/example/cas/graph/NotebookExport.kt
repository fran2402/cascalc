package com.example.cas.graph

import com.example.cas.editor.MathCodec
import com.example.cas.editor.Sym
import com.example.cas.engine.LatexParser
import com.example.cas.engine.Notebook

/**
 * A notebook on A4 pages (points), typeset in Computer Modern like the history's PDF: the title,
 * text as wrapped paragraphs (a # line as a heading, $math$ set as math), each calculation with
 * its answer on the right, and each graph as a framed figure with a grid.
 */
object NotebookExport {
    private const val W = SessionExport.PAGE_WIDTH
    private const val H = SessionExport.PAGE_HEIGHT
    private const val MARGIN = 56.0
    private val CURVES = intArrayOf(0xFF1F5FA8.toInt(), 0xFFC0392B.toInt(), 0xFF2E8B57.toInt(), 0xFF8E44AD.toInt(), 0xFFD68910.toInt())

    fun pages(d: Notebook.Doc, run: Notebook.Run, subtitle: String, ink: Int = 0xFF000000.toInt(), muted: Int = 0xFF5C5C5C.toInt()): List<Scene> {
        val out = ArrayList<Scene>()
        val width = W - 2 * MARGIN
        var scene = Scene(W, H, 0xFFFFFFFF.toInt())
        var y = MARGIN
        fun room(h: Double) { if (y + h > H - MARGIN && y > MARGIN + 1) { out += scene; scene = Scene(W, H, 0xFFFFFFFF.toInt()); y = MARGIN } }
        scene.add(Scene.Label(MARGIN, y + 14, d.title.ifBlank { "Notebook" }, 22.0, ink, Scene.Anchor.Start, Scene.Font.Roman))
        scene.add(Scene.Label(MARGIN, y + 34, subtitle, 10.0, muted, Scene.Anchor.Start, Scene.Font.Roman))
        y += 58
        fun fit(row: com.example.cas.editor.MathRow, size: Double, color: Int, max: Double): MathScene.Box {
            val b = MathScene.layout(row, size, color)
            return if (b.width <= max) b else MathScene.layout(row, maxOf(7.0, size * max / b.width), color)
        }

        /** One word or math piece of a paragraph, in its style. */
        class Piece(val box: MathScene.Box?, val word: String?, val w: Double, val ascent: Double, val descent: Double,
                    val font: Scene.Font = Scene.Font.Roman, val bold: Boolean = false, val strike: Boolean = false, val color: Int = ink, val underline: Boolean = false, val breakAfter: Boolean = false)

        /** Inline Markdown as pieces: bold, italic, struck, code and link styles, math as math. */
        fun pieces(list: List<com.example.cas.engine.Markdown.Inline>, size: Double, color: Int, bold: Boolean = false, italic: Boolean = false, strike: Boolean = false, link: Boolean = false, out: ArrayList<Piece> = ArrayList()): ArrayList<Piece> {
            for (i in list) when (i) {
                is com.example.cas.engine.Markdown.Inline.Text -> i.text.split(' ').filter { it.isNotEmpty() }.forEach { w ->
                    out += Piece(null, w, MathScene.metrics.width(w, italic, size) * (if (bold) 1.04 else 1.0), size * 0.72, size * 0.24,
                        if (italic) Scene.Font.Italic else Scene.Font.Roman, bold, strike, if (link) 0xFF1F5FA8.toInt() else color, link)
                }
                is com.example.cas.engine.Markdown.Inline.Bold -> pieces(i.children, size, color, true, italic, strike, link, out)
                is com.example.cas.engine.Markdown.Inline.Italic -> pieces(i.children, size, color, bold, true, strike, link, out)
                is com.example.cas.engine.Markdown.Inline.Strike -> pieces(i.children, size, color, bold, italic, true, link, out)
                is com.example.cas.engine.Markdown.Inline.Link -> pieces(i.children, size, color, bold, italic, strike, true, out)
                is com.example.cas.engine.Markdown.Inline.Code -> out += Piece(null, i.text, i.text.length * size * 0.92 * 0.525, size * 0.72, size * 0.24, Scene.Font.Mono, color = 0xFF7A3E00.toInt())
                is com.example.cas.engine.Markdown.Inline.Math -> runCatching { fit(LatexParser.parse(i.latex), size, color, width) }.getOrNull()?.let { b -> out += Piece(b, null, b.width, b.ascent, b.descent) }
                is com.example.cas.engine.Markdown.Inline.Image -> out += Piece(null, "[" + i.alt.ifBlank { "image" } + "]", MathScene.metrics.width("[" + i.alt + "]", true, size), size * 0.72, size * 0.24, Scene.Font.Italic, color = muted)
                com.example.cas.engine.Markdown.Inline.Break -> if (out.isNotEmpty()) out[out.lastIndex] = out.last().let { p -> Piece(p.box, p.word, p.w, p.ascent, p.descent, p.font, p.bold, p.strike, p.color, p.underline, true) }
            }
            return out
        }

        /** Pieces set along lines that wrap at the margin, from [left]. */
        fun setLines(list: List<Piece>, size: Double, left: Double) {
            val space = MathScene.metrics.width(" ", false, size)
            val room0 = MARGIN + width - left
            var line = ArrayList<Piece>(); var lineW = 0.0
            fun flush() {
                if (line.isEmpty()) return
                val asc = line.maxOf { it.ascent }; val desc = line.maxOf { it.descent }
                room(asc + desc + 4)
                val base = y + asc
                var x = left
                for (p in line) {
                    if (p.box != null) p.box.draw(scene, x, base)
                    else {
                        scene.add(Scene.Label(x, base, p.word!!, if (p.font == Scene.Font.Mono) size * 0.92 else size, p.color, Scene.Anchor.Start, p.font, baseline = true))
                        // Bold: the word again, a hair to the right.
                        if (p.bold) scene.add(Scene.Label(x + size * 0.035, base, p.word, size, p.color, Scene.Anchor.Start, p.font, baseline = true))
                        if (p.strike) scene.add(Scene.Stroke(listOf(doubleArrayOf(x, base - size * 0.28, x + p.w, base - size * 0.28)), p.color, 0.6))
                        if (p.underline) scene.add(Scene.Stroke(listOf(doubleArrayOf(x, base + size * 0.12, x + p.w, base + size * 0.12)), p.color, 0.5))
                    }
                    x += p.w + space
                }
                y = base + desc + size * 0.35
                line = ArrayList(); lineW = 0.0
            }
            for (p in list) {
                if (line.isNotEmpty() && lineW + space + p.w > room0) flush()
                lineW += (if (line.isEmpty()) 0.0 else space) + p.w
                line += p
                if (p.breakAfter) flush()
            }
            flush()
        }

        /** A grid of cells (pieces), with a rule under the header. */
        fun table(header: List<List<Piece>>, rows: List<List<List<Piece>>>, left: Double) {
            val n = header.size
            if (n == 0) return
            val colW = (0 until n).map { c -> (listOf(header[c]) + rows.map { it.getOrElse(c) { emptyList() } }).maxOf { cell -> cell.sumOf { it.w } + cell.size * 3.0 } + 14 }
            val scale = minOf(1.0, (MARGIN + width - left) / colW.sum())
            val widths = colW.map { it * scale }
            fun drawRow(cells: List<List<Piece>>, rowH: Double) {
                room(rowH)
                var x = left
                cells.forEachIndexed { c, cell ->
                    var cx = x + 6
                    val base = y + rowH * 0.68
                    for (p in cell) {
                        if (p.box != null) p.box.draw(scene, cx, base) else {
                            scene.add(Scene.Label(cx, base, p.word!!, 10.0 * scale.coerceAtLeast(0.7), p.color, Scene.Anchor.Start, p.font, baseline = true))
                            if (p.bold) scene.add(Scene.Label(cx + 0.35, base, p.word, 10.0 * scale.coerceAtLeast(0.7), p.color, Scene.Anchor.Start, p.font, baseline = true))
                        }
                        cx += p.w * scale + 3
                    }
                    x += widths.getOrElse(c) { 0.0 }
                }
                y += rowH
            }
            val rowH = 17.0
            scene.add(Scene.Stroke(listOf(doubleArrayOf(left, y, left + widths.sum(), y)), 0xFF444444.toInt(), 0.8))
            drawRow(header, rowH)
            scene.add(Scene.Stroke(listOf(doubleArrayOf(left, y, left + widths.sum(), y)), 0xFF444444.toInt(), 0.5))
            rows.forEach { drawRow(it, rowH) }
            scene.add(Scene.Stroke(listOf(doubleArrayOf(left, y, left + widths.sum(), y)), 0xFF444444.toInt(), 0.8))
            y += 10
        }

        /** Markdown blocks, indented by [left] (lists and quotes nest). */
        fun markdown(blocks: List<com.example.cas.engine.Markdown.Block>, left: Double) {
            for (mb in blocks) when (mb) {
                is com.example.cas.engine.Markdown.Block.Heading -> {
                    val size = when (mb.level) { 1 -> 20.0; 2 -> 17.0; 3 -> 14.5; 4 -> 13.0; 5 -> 12.0; else -> 11.5 }
                    y += size * 0.5
                    setLines(pieces(mb.inlines, size, ink, bold = true), size, left)
                    y += 2
                }
                is com.example.cas.engine.Markdown.Block.Paragraph -> { setLines(pieces(mb.inlines, 11.0, ink), 11.0, left); y += 4 }
                is com.example.cas.engine.Markdown.Block.ListBlock -> {
                    mb.items.forEachIndexed { k, item ->
                        room(16.0)
                        val mark = when { item.checked == true -> "☑"; item.checked == false -> "☐"; mb.ordered -> "${mb.start + k}."; else -> "•" }
                        scene.add(Scene.Label(left + 2, y + 9, mark, 11.0, ink, Scene.Anchor.Start, Scene.Font.Roman))
                        markdown(item.blocks, left + 18)
                    }
                    y += 2
                }
                is com.example.cas.engine.Markdown.Block.Quote -> {
                    val top = y
                    markdown(mb.blocks, left + 14)
                    scene.add(Scene.Stroke(listOf(doubleArrayOf(left + 4, top, left + 4, y - 4)), 0xFFB0B0B0.toInt(), 2.0))
                }
                is com.example.cas.engine.Markdown.Block.Code -> {
                    val lines = mb.text.lines()
                    val h = lines.size * 11.0 + 10
                    room(h)
                    scene.add(Scene.Fill(listOf(doubleArrayOf(left, y, MARGIN + width, y, MARGIN + width, y + h, left, y + h)), 0xFFF2F2F2.toInt()))
                    lines.forEachIndexed { k, l -> scene.add(Scene.Label(left + 6, y + 13 + k * 11.0, l, 8.5, 0xFF333333.toInt(), Scene.Anchor.Start, Scene.Font.Mono, baseline = true)) }
                    y += h + 6
                }
                is com.example.cas.engine.Markdown.Block.MathBlock -> runCatching { fit(LatexParser.parse(mb.latex), 13.0, ink, width) }.getOrNull()?.let { box ->
                    room(box.height + 10)
                    box.draw(scene, left + (MARGIN + width - left - box.width) / 2, y + 4 + box.ascent)
                    y += box.height + 12
                }
                com.example.cas.engine.Markdown.Block.Rule -> { room(12.0); scene.add(Scene.Stroke(listOf(doubleArrayOf(left, y + 5, MARGIN + width, y + 5)), 0xFFAAAAAA.toInt(), 0.6)); y += 12 }
                is com.example.cas.engine.Markdown.Block.Table -> table(mb.header.map { h -> pieces(h, 10.0, ink, bold = true) }, mb.rows.map { r -> r.map { c -> pieces(c, 10.0, ink) } }, left)
            }
        }

        /** A graph (or a table's points) as a framed figure with a grid. */
        fun figure(p: Notebook.Plot) {
            if (p.error != null || p.xs.isEmpty()) return
            val gh = 190.0
            room(gh + 30)
            val left = MARGIN + 20; val right = MARGIN + width - 20; val top = y; val bottom = y + gh
            val x0 = p.xs.first(); val x1 = p.xs.last()
            fun sx(x: Double) = left + (x - x0) / (x1 - x0) * (right - left)
            fun sy(v: Double) = bottom - (v - p.yMin) / (p.yMax - p.yMin) * (bottom - top)
            // Grid at round steps, then the axes, the frame and the curves.
            fun step(span: Double): Double { if (!span.isFinite() || span <= 0) return 1.0; val raw = span / 6; val e = Math.pow(10.0, Math.floor(Math.log10(raw))); return listOf(1.0, 2.0, 5.0, 10.0).first { it * e >= raw } * e }
            val grid = ArrayList<DoubleArray>()
            val sxStep = step(x1 - x0); var gx = Math.ceil(x0 / sxStep) * sxStep
            while (gx <= x1 + 1e-9) { grid += doubleArrayOf(sx(gx), top, sx(gx), bottom); scene.add(Scene.Label(sx(gx), bottom + 11, num(gx), 8.0, muted, Scene.Anchor.Middle, Scene.Font.Roman)); gx += sxStep }
            val syStep = step(p.yMax - p.yMin); var gy = Math.ceil(p.yMin / syStep) * syStep
            while (gy <= p.yMax + 1e-9) { grid += doubleArrayOf(left, sy(gy), right, sy(gy)); scene.add(Scene.Label(left - 3, sy(gy) + 3, num(gy), 8.0, muted, Scene.Anchor.End, Scene.Font.Roman)); gy += syStep }
            scene.add(Scene.Stroke(grid, 0xFFE3E3E3.toInt(), 0.5))
            if (p.yMin < 0 && p.yMax > 0) scene.add(Scene.Stroke(listOf(doubleArrayOf(left, sy(0.0), right, sy(0.0))), 0xFF8A8A8A.toInt(), 0.8))
            if (x0 < 0 && x1 > 0) scene.add(Scene.Stroke(listOf(doubleArrayOf(sx(0.0), top, sx(0.0), bottom)), 0xFF8A8A8A.toInt(), 0.8))
            scene.add(Scene.Stroke(listOf(doubleArrayOf(left, top, right, top, right, bottom, left, bottom, left, top)), 0xFF444444.toInt(), 0.7))
            p.curves.forEachIndexed { k, cv ->
                val runs = ArrayList<DoubleArray>(); var cur = ArrayList<Double>()
                for (i in p.xs.indices) {
                    val v = cv[i]
                    if (!v.isFinite() || v < p.yMin - (p.yMax - p.yMin) || v > p.yMax + (p.yMax - p.yMin)) { if (cur.size >= 4) runs += cur.toDoubleArray(); cur = ArrayList(); continue }
                    cur += sx(p.xs[i]); cur += sy(v.coerceIn(p.yMin, p.yMax))
                }
                if (cur.size >= 4) runs += cur.toDoubleArray()
                scene.add(Scene.Stroke(runs, CURVES[k % CURVES.size], 1.4))
                p.labels.getOrNull(k)?.let { l ->
                    runCatching { MathScene.layout(LatexParser.parse(l), 9.0, CURVES[k % CURVES.size]) }.getOrNull()?.let { box -> box.draw(scene, right - box.width - 6, top + 12 + k * 13) }
                }
            }
            p.points.forEachIndexed { k, (px, py) ->
                for (i in px.indices) if (py[i] in p.yMin..p.yMax) scene.add(Scene.Circle(sx(px[i]), sy(py[i]), 2.4, CURVES[k % CURVES.size]))
                p.labels.getOrNull(k)?.let { l -> runCatching { MathScene.layout(LatexParser.parse(l), 9.0, CURVES[k % CURVES.size]) }.getOrNull()?.let { box -> box.draw(scene, right - box.width - 6, top + 12 + k * 13) } }
            }
            y = bottom + 26
        }

        for (b in d.blocks) when (b) {
            is Notebook.Block.Text -> { markdown(com.example.cas.engine.Markdown.parse(b.text), MARGIN); y += 4 }
            is Notebook.Block.Table -> {
                val t = b.table
                fun cell(text: String, bold: Boolean = false): List<Piece> = if (text.isEmpty()) emptyList() else listOf(Piece(null, text, text.length * 10.0 * 0.525, 7.2, 2.4, Scene.Font.Mono, bold))
                val header: List<List<Piece>> = t.names.map { cell(it, bold = true) }
                val rows = (0 until t.rowCount).map { r ->
                    t.names.indices.map { c -> cell(t.cell(c, r).let { raw -> if (Sheet.isFormula(raw)) t.value(c, r)?.let { DataTable.text(it) } ?: raw else raw }) }
                }
                table(header, rows, MARGIN)
                run.plots[b.id]?.let { p -> figure(p) }
            }
            is Notebook.Block.Math -> if (b.row.items.isNotEmpty()) {
                val o = run.outcomes[b.id]
                val q = fit(b.row, 12.0, ink, width)
                val answer = o?.answer?.let { a ->
                    val r = MathCodec.copy(a.exact)
                    if (!a.isStatement) { r.add(0, Sym(" ")); r.add(0, Sym(if (a.isApproximate) "≈" else "=")) }
                    fit(r, 13.0, ink, width - 24)
                }
                val err = o?.error
                room(q.height + (answer?.height ?: 0.0) + 22)
                val qBase = y + q.ascent
                q.draw(scene, MARGIN + 12, qBase)
                y = qBase + q.descent + 6
                if (answer != null) {
                    val aBase = y + answer.ascent
                    answer.draw(scene, MARGIN + width - answer.width, aBase)
                    y = aBase + answer.descent + 6
                } else if (err != null) {
                    scene.add(Scene.Label(MARGIN + width, y + 9, err, 9.0, muted, Scene.Anchor.End, Scene.Font.Roman))
                    y += 14
                }
                y += 8
            }
            is Notebook.Block.Graph -> run.plots[b.id]?.let { figure(it) }
        }
        out += scene
        return out
    }

    private fun num(v: Double): String = if (kotlin.math.abs(v - Math.rint(v)) < 1e-9) Math.rint(v).toLong().toString().replace("-", "−")
        else String.format(java.util.Locale.ROOT, "%.3g", v).trimEnd('0').trimEnd('.').replace("-", "−")
}
