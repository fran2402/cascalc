package com.example.cas.graph

/**
 * Reads data points from a CSV (or TSV, or space-separated) file for the 2D graph. The
 * separator is found from the file (comma, semicolon, tab, else spaces); with semicolons a
 * decimal comma is accepted (3,14), as spreadsheets in many countries write. Rows that aren't
 * numbers (headers, notes) are skipped; the first header row names the columns.
 */
object Csv {
    /** The numeric columns, all the same length, and their names (from a header, or "" each). */
    class Table(val names: List<String>, val columns: List<DoubleArray>) {
        val rows get() = columns.firstOrNull()?.size ?: 0
    }

    fun parse(text: String): Table {
        val lines = text.removePrefix("\uFEFF").lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        if (lines.isEmpty()) return Table(emptyList(), emptyList())
        val sample = lines.take(20)
        // Semicolons or tabs on every line win (their files use commas for decimals); then commas.
        val separator = listOf(';', '\t').firstOrNull { c -> sample.all { c in it } } ?: ','.takeIf { c -> sample.any { c in it } }
        fun cells(line: String): List<String> = when (separator) {
            null -> line.split(Regex("\\s+"))
            else -> splitQuoted(line, separator)
        }.map { it.trim().removeSurrounding("\"").trim() }
        val decimalComma = separator == ';' || separator == '\t' && sample.any { Regex("\\d,\\d").containsMatchIn(it) }
        fun number(s: String): Double? {
            var t = s.replace("−", "-").replace("\u00A0", "").replace(" ", "")
            if (decimalComma) t = t.replace(',', '.')
            return t.toDoubleOrNull()?.takeIf { it.isFinite() }
        }
        var names: List<String>? = null
        val rows = ArrayList<List<Double>>()
        for (line in lines) {
            val c = cells(line)
            val values = c.map { number(it) }
            if (values.all { it == null }) { if (names == null && rows.isEmpty()) names = c; continue }
            // Rows with a gap or a word in them are left out, rather than guessed.
            if (values.any { it == null }) continue
            rows += values.map { it!! }
        }
        val width = rows.groupingBy { it.size }.eachCount().maxByOrNull { it.value }?.key ?: 0
        val kept = rows.filter { it.size == width }
        val columns = (0 until width).map { j -> DoubleArray(kept.size) { i -> kept[i][j] } }
        val header = names?.let { n -> (0 until width).map { n.getOrElse(it) { "" } } } ?: List(width) { "" }
        return Table(header, columns)
    }

    /** Splits on [sep] outside double quotes ("1,5";2 has two cells). */
    private fun splitQuoted(line: String, sep: Char): List<String> {
        val out = ArrayList<String>()
        val cur = StringBuilder()
        var quoted = false
        for (ch in line) {
            when {
                ch == '"' -> { quoted = !quoted; cur.append(ch) }
                ch == sep && !quoted -> { out += cur.toString(); cur.clear() }
                else -> cur.append(ch)
            }
        }
        out += cur.toString()
        return out
    }

    /**
     * The point lists to plot: the first column is x and each other column a list of (x, y);
     * a single column is plotted against 1, 2, 3…
     */
    fun pointLists(t: Table): List<Pair<String, List<Pair<Double, Double>>>> = when {
        t.columns.isEmpty() -> emptyList()
        t.columns.size == 1 -> listOf(t.names[0] to t.columns[0].mapIndexed { i, y -> (i + 1.0) to y })
        else -> (1 until t.columns.size).map { j -> t.names[j] to t.columns[0].indices.map { i -> t.columns[0][i] to t.columns[j][i] } }
    }

    /** A number as the editor types it: plain decimals, up to 12 significant digits, "−" for minus. */
    fun numberText(v: Double): String {
        val s = java.math.BigDecimal(v).round(java.math.MathContext(12)).stripTrailingZeros().toPlainString()
        return (if (s == "-0") "0" else s).replace("-", "−")
    }
}
