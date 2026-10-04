package com.example.cas.graph

/**
 * A table of data behind a list of points: every column of an imported file (or typed in the
 * table editor) as text, and which columns are x, y and their uncertainties σ(x) and σ(y).
 * The points plotted are the rows whose x and y are numbers; with no x column they're plotted
 * against the row number 1, 2, 3…
 */
class DataTable(
    val names: List<String>,
    /** Columns of cells, all the same length. */
    val columns: List<List<String>>,
    val x: Int?,
    val y: Int?,
    val sigmaX: Int? = null,
    val sigmaY: Int? = null,
    /** Each column's number format and color scale (missing ones are the default). */
    val formats: List<ColumnFormat> = emptyList(),
    /** The first column stays in view while the table scrolls sideways. */
    val frozen: Boolean = false,
) {
    fun format(column: Int): ColumnFormat = formats.getOrElse(column) { ColumnFormat() }

    val rowCount get() = columns.maxOfOrNull { it.size } ?: 0

    fun cell(column: Int?, row: Int): String = column?.let { columns.getOrNull(it)?.getOrNull(row) } ?: ""

    /** The cells worked out, for formulas ("=A1*2", see [Sheet]). */
    val sheet by lazy { Sheet.Book(columns) }

    /** A cell's number: as typed, or what its formula works out to. */
    fun value(column: Int?, row: Int): Double? {
        val c = column ?: return null
        val t = cell(c, row)
        return if (Sheet.isFormula(t)) sheet.value(c, row).number?.takeIf { it.isFinite() } else number(t)
    }

    /** The rows that are plotted: x and y both numbers (x is the row number without an x column). */
    private fun plottedRows(): List<Int> {
        val yc = y ?: return emptyList()
        return (0 until rowCount).filter { r -> (x == null || value(x, r) != null) && value(yc, r) != null }
    }

    fun points(): List<Pair<Double, Double>> =
        plottedRows().map { r -> (if (x == null) r + 1.0 else value(x, r)!!) to value(y, r)!! }

    /** σ(x) and σ(y) for each plotted point (NaN where a cell is empty), or null without that column. */
    fun errors(): Pair<DoubleArray?, DoubleArray?> {
        val rows = plottedRows()
        fun column(c: Int?) = c?.let { col -> DoubleArray(rows.size) { k -> value(col, rows[k])?.let { kotlin.math.abs(it) } ?: Double.NaN } }
        return column(sigmaX) to column(sigmaY)
    }

    /** Cells with something in them that isn't a number, in the columns that are used. */
    fun badCells(): Int = listOfNotNull(x, y, sigmaX, sigmaY).distinct().sumOf { c ->
        columns.getOrNull(c)?.indices?.count { r -> cell(c, r).isNotBlank() && value(c, r) == null } ?: 0
    }

    fun withRoles(x: Int?, y: Int?, sigmaX: Int?, sigmaY: Int?) = DataTable(names, columns, x, y, sigmaX, sigmaY, formats, frozen)

    /** A column's numbers (formulas worked out); text and empty cells left out. */
    fun numbers(column: Int): List<Double> = (0 until rowCount).mapNotNull { value(column, it) }

    /** A column's summary, or null with no numbers in it. */
    fun stats(column: Int): Stats? = Companion.stats(numbers(column))

    /**
     * The table as CSV, a row of names first if any column has one; formulas give their value.
     * Cells with a comma, quote or line break are quoted, as spreadsheets expect.
     */
    fun csv(): String = buildString {
        fun quoted(t: String) = if (t.any { it == ',' || it == '"' || it == '\n' }) "\"" + t.replace("\"", "\"\"") + "\"" else t
        if (names.any { it.isNotBlank() }) appendLine(columns.indices.joinToString(",") { quoted(names.getOrElse(it) { "" }) })
        for (r in 0 until rowCount) {
            appendLine(columns.indices.joinToString(",") { c ->
                val t = cell(c, r)
                quoted(if (Sheet.isFormula(t)) value(c, r)?.let { text(it) } ?: sheet.value(c, r).toString() else t)
            })
        }
    }

    /**
     * Saved as text: the roles (then F: the columns' formats and Z for a frozen first column,
     * when there are any), the names, then one line per column; cells split by tabs.
     */
    fun encode(): String = buildString {
        append(listOf(x, y, sigmaX, sigmaY).joinToString("\t") { (it ?: -1).toString() })
        if (formats.any { !it.isDefault }) append("\tF:").append(formats.joinToString(",") { it.encode() })
        if (frozen) append("\tZ")
        append('\n')
        append(names.joinToString("\t") { esc(it) })
        columns.forEach { c -> append('\n').append(c.joinToString("\t") { esc(it) }) }
    }

    /** A column's count, total, mean, median, sample standard deviation, smallest and largest. */
    class Stats(val n: Int, val sum: Double, val mean: Double, val median: Double, val sd: Double?, val min: Double, val max: Double)

    companion object {
        fun stats(v: List<Double>): Stats? {
            if (v.isEmpty()) return null
            val sorted = v.sorted()
            val mean = v.sum() / v.size
            val median = if (v.size % 2 == 1) sorted[v.size / 2] else (sorted[v.size / 2 - 1] + sorted[v.size / 2]) / 2
            val sd = if (v.size > 1) kotlin.math.sqrt(v.sumOf { (it - mean) * (it - mean) } / (v.size - 1)) else null
            return Stats(v.size, v.sum(), mean, median, sd, sorted.first(), sorted.last())
        }

        /** [n] numbers from [start] going up by [step], as cells (5, 7.5, 10…). */
        fun series(start: Double, step: Double, n: Int): List<String> =
            List(n) { k -> text(java.math.BigDecimal(start + k * step).round(java.math.MathContext(12)).toDouble()) }

        /** The column order after moving column [from] to [to] (every other column keeps its order). */
        fun moved(count: Int, from: Int, to: Int): List<Int> = (0 until count).toMutableList().apply { add(to, removeAt(from)) }

        /** A cell as a number: a decimal point or comma, − or - for minus, spaces ignored. */
        fun number(s: String): Double? =
            s.trim().replace("−", "-").replace("\u00A0", "").replace(" ", "").replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

        fun text(v: Double): String = Csv.numberText(v).replace("−", "-")

        /** An imported file: the first column is x and the second y; the rest are kept, unused. */
        fun fromCsv(t: Csv.Table): DataTable {
            val cols = t.columns.map { c -> c.map { text(it) } }
            return when (cols.size) {
                0 -> DataTable(emptyList(), emptyList(), null, null)
                1 -> DataTable(t.names, cols, x = null, y = 0)
                else -> DataTable(t.names, cols, x = 0, y = 1)
            }
        }

        /** A typed list of points as a two-column table. */
        fun fromPoints(xs: DoubleArray, ys: DoubleArray) =
            DataTable(listOf("", ""), listOf(xs.map { text(it) }, ys.map { text(it) }), x = 0, y = 1)

        fun decode(s: String): DataTable? = runCatching {
            val lines = s.split('\n')
            val head = lines[0].split('\t')
            val roles = head.mapNotNull { it.toIntOrNull() }
            val formats = head.firstOrNull { it.startsWith("F:") }?.removePrefix("F:")?.split(',')?.map { ColumnFormat.decode(it) }.orEmpty()
            val frozen = "Z" in head
            val names = lines.getOrElse(1) { "" }.split('\t').map { unesc(it) }
            val columns = lines.drop(2).map { l -> l.split('\t').map { unesc(it) } }
            fun role(k: Int) = roles.getOrNull(k)?.takeIf { it in columns.indices }
            DataTable(names, columns, role(0), role(1), role(2), role(3), formats, frozen)
        }.getOrNull()

        private fun esc(t: String) = t.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n")
        private fun unesc(t: String) = buildString {
            var k = 0
            while (k < t.length) {
                val c = t[k]
                if (c == '\\' && k + 1 < t.length) {
                    append(when (t[k + 1]) { 't' -> '\t'; 'n' -> '\n'; else -> t[k + 1] }); k += 2
                } else { append(c); k++ }
            }
        }
    }
}
