package com.example.cas.graph

/**
 * How a column's numbers are shown, as a spreadsheet's number format: [decimals] fixed places
 * (null: as typed), as a percentage, in scientific notation, with thousands grouped. A
 * [colorScale] shades each number from the column's smallest (red) through yellow to its
 * largest (green), as Excel's color scale does. Only the look changes: the cells keep their
 * values for formulas, fits and plotting.
 */
data class ColumnFormat(
    val decimals: Int? = null,
    val percent: Boolean = false,
    val scientific: Boolean = false,
    val thousands: Boolean = false,
    val colorScale: Boolean = false,
    /** A bar inside each cell, as long as its number is far between the column's smallest and largest (Excel's data bars). */
    val dataBars: Boolean = false,
    /** Cells meeting a rule tinted in one of [HighlightRule.COLORS] (conditional formatting). */
    val highlight: HighlightRule? = null,
) {
    val isDefault get() = this == ColumnFormat()
    /** Whether numbers are shown differently from how they're typed. */
    val changesNumbers get() = decimals != null || percent || scientific || thousands

    /** [value] as this format shows it: 1 234.50, 12.5%, 1.23 × 10⁴. */
    fun show(value: Double): String {
        if (!value.isFinite()) return value.toString()
        if (scientific) {
            if (value == 0.0) return fixed(0.0, decimals ?: 2)
            val e = kotlin.math.floor(kotlin.math.log10(kotlin.math.abs(value))).toInt()
            var m = value / Math.pow(10.0, e.toDouble())
            var power = e
            val places = decimals ?: 2
            // 9.999 rounded to 10.00 moves up a power.
            if (kotlin.math.abs(java.math.BigDecimal(m).setScale(places, java.math.RoundingMode.HALF_UP).toDouble()) >= 10) { m /= 10; power++ }
            return fixed(m, places) + " × 10" + superscript(power)
        }
        val v = if (percent) value * 100 else value
        val text = if (decimals != null) fixed(v, decimals) else DataTable.text(v).replace("-", "−")
        val grouped = if (thousands) group(text) else text
        return grouped + if (percent) "%" else ""
    }

    fun encode(): String = buildList {
        decimals?.let { add("d$it") }
        if (percent) add("p"); if (scientific) add("s"); if (thousands) add("t"); if (colorScale) add("c"); if (dataBars) add("b")
        highlight?.let { add("h" + it.encode()) }
    }.joinToString(".")

    companion object {
        fun decode(s: String): ColumnFormat = s.split('.').fold(ColumnFormat()) { f, t ->
            when {
                t.startsWith("d") -> f.copy(decimals = t.drop(1).toIntOrNull()?.coerceIn(0, 10))
                t == "p" -> f.copy(percent = true); t == "s" -> f.copy(scientific = true)
                t == "t" -> f.copy(thousands = true); t == "c" -> f.copy(colorScale = true); t == "b" -> f.copy(dataBars = true)
                t.startsWith("h") -> f.copy(highlight = HighlightRule.decode(t.drop(1)))
                else -> f
            }
        }

        private fun fixed(v: Double, places: Int): String =
            java.math.BigDecimal(v).setScale(places, java.math.RoundingMode.HALF_UP).toPlainString().replace("-", "−").let { if (it == "−0" || it.matches(Regex("−0\\.0*"))) it.drop(1) else it }

        /** Thousands grouped by a thin space in the whole part: 1 234 567.8. */
        private fun group(text: String): String {
            val neg = text.startsWith("−")
            val body = text.removePrefix("−")
            val whole = body.substringBefore('.')
            val rest = body.drop(whole.length)
            if (whole.length <= 4 || !whole.all { it.isDigit() }) return text
            val grouped = whole.reversed().chunked(3).joinToString(" ").reversed()
            return (if (neg) "−" else "") + grouped + rest
        }

        private fun superscript(n: Int) = n.toString().map { c -> if (c == '-') '⁻' else "⁰¹²³⁴⁵⁶⁷⁸⁹"[c - '0'] }.joinToString("")
    }
}

/**
 * A conditional format: cells whose value passes [op] against [value] are tinted with color
 * [color] (an index into [COLORS]), as Sheets' and Excel's highlight rules.
 */
data class HighlightRule(val op: SheetTools.FilterOp, val value: String = "", val color: Int = 0) {
    /** Whether row [row] of column [column] gets the tint. */
    fun matches(t: DataTable, column: Int, row: Int): Boolean = SheetTools.Filter(column, op, value).keeps(t, row)

    /** Kept inside a format's dot-separated tokens: the op's index, the value (escaped), the color. */
    fun encode(): String = "${op.ordinal}~${escape(value)}~$color"

    companion object {
        /** The tints offered: red, amber, green, blue, purple (light, so the text stays readable). */
        val COLORS = listOf(0xFFF28B82.toInt(), 0xFFFDD663.toInt(), 0xFF81C995.toInt(), 0xFF8AB4F8.toInt(), 0xFFC58AF9.toInt())
        val COLOR_NAMES = listOf("Red", "Amber", "Green", "Blue", "Purple")

        fun decode(s: String): HighlightRule? = runCatching {
            val parts = s.split('~')
            HighlightRule(SheetTools.FilterOp.entries[parts[0].toInt()], unescape(parts.getOrElse(1) { "" }), parts.getOrElse(2) { "0" }.toInt().coerceIn(0, COLORS.size - 1))
        }.getOrNull()

        // The format is saved as dot- and comma-separated tokens on a tab-separated line.
        private fun escape(v: String) = buildString { v.forEach { c -> if (c in "%.,~\t\n") append('%').append("%02X".format(c.code)) else append(c) } }
        private fun unescape(v: String) = Regex("%([0-9A-F]{2})").replace(v) { it.groupValues[1].toInt(16).toChar().toString() }
    }
}

/** Spreadsheet tools for the data table: find and replace, filters, tidying, smart fill, a range's summary. */
object SheetTools {

    // ---- Find and replace ----------------------------------------------------------------

    /** Whether cell text [t] matches [query]: anywhere in it, or the whole cell. */
    fun matches(t: String, query: String, matchCase: Boolean, wholeCell: Boolean): Boolean {
        if (query.isEmpty()) return false
        return if (wholeCell) t.trim().equals(query.trim(), ignoreCase = !matchCase) else t.contains(query, ignoreCase = !matchCase)
    }

    /** Every cell matching, as (column, row), row by row as you'd read them. */
    fun find(cells: List<List<String>>, query: String, matchCase: Boolean = false, wholeCell: Boolean = false): List<Pair<Int, Int>> {
        val rows = cells.maxOfOrNull { it.size } ?: 0
        return (0 until rows).flatMap { r -> cells.indices.filter { c -> matches(cells[c].getOrElse(r) { "" }, query, matchCase, wholeCell) }.map { c -> c to r } }
    }

    /** [t] with [query] replaced by [with]: every place in it, or the whole cell when it matches whole. */
    fun replace(t: String, query: String, with: String, matchCase: Boolean = false, wholeCell: Boolean = false): String = when {
        !matches(t, query, matchCase, wholeCell) -> t
        wholeCell -> with
        else -> Regex(Regex.escape(query), if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)).replace(t) { with }
    }

    // ---- Filters --------------------------------------------------------------------------

    /** A filter's test, as a spreadsheet's filter by condition. */
    enum class FilterOp(val label: String, val needsValue: Boolean = true) {
        Equals("Is equal to"), NotEquals("Isn't equal to"), Greater("Greater than"), Less("Less than"),
        Contains("Text contains"), Empty("Is empty", false), NotEmpty("Isn't empty", false),
    }

    /** Rows kept when column [column] passes [op] with [value] (numbers compared as numbers). */
    class Filter(val column: Int, val op: FilterOp, val value: String = "") {
        fun keeps(t: DataTable, row: Int): Boolean {
            val text = t.cell(column, row)
            val shown = if (Sheet.isFormula(text)) t.value(column, row)?.let { DataTable.text(it) } ?: t.sheet.value(column, row).toString() else text
            val n = t.value(column, row)
            val target = DataTable.number(value)
            return when (op) {
                FilterOp.Empty -> text.isBlank()
                FilterOp.NotEmpty -> text.isNotBlank()
                FilterOp.Contains -> shown.contains(value.trim(), ignoreCase = true)
                FilterOp.Equals -> if (n != null && target != null) n == target else shown.trim().equals(value.trim(), ignoreCase = true)
                FilterOp.NotEquals -> if (n != null && target != null) n != target else !shown.trim().equals(value.trim(), ignoreCase = true)
                FilterOp.Greater -> n != null && target != null && n > target
                FilterOp.Less -> n != null && target != null && n < target
            }
        }

        /** As a chip shows it: "> 3", "contains a", "empty". */
        val summary: String get() = when (op) {
            FilterOp.Equals -> "= $value"; FilterOp.NotEquals -> "≠ $value"
            FilterOp.Greater -> "> $value"; FilterOp.Less -> "< $value"
            FilterOp.Contains -> "contains “$value”"; FilterOp.Empty -> "empty"; FilterOp.NotEmpty -> "not empty"
        }
    }

    /** The rows every filter keeps, in order. */
    fun visibleRows(t: DataTable, filters: Collection<Filter>): List<Int> =
        (0 until t.rowCount).filter { r -> filters.all { it.keeps(t, r) } }

    // ---- Tidying ----------------------------------------------------------------------------

    /** The rows to keep so no row repeats an earlier one exactly (the first of each stays). */
    fun withoutDuplicates(cells: List<List<String>>): List<Int> {
        val rows = cells.maxOfOrNull { it.size } ?: 0
        val seen = HashSet<List<String>>()
        return (0 until rows).filter { r -> seen.add(cells.map { it.getOrElse(r) { "" }.trim() }) }
    }

    /** Spaces trimmed from the ends of every cell, and runs of spaces inside made one (as TRIM does); formulas are left alone. */
    fun trimmed(t: String): String = if (Sheet.isFormula(t)) t else t.trim().replace(Regex(" {2,}"), " ")

    /**
     * Rows and columns swapped. Column names, if any, become the first column, and the first
     * column becomes the names, so a table with headers keeps them.
     */
    fun transpose(names: List<String>, cells: List<List<String>>): Pair<List<String>, List<List<String>>> {
        val rows = cells.maxOfOrNull { it.size } ?: 0
        val hasNames = names.any { it.isNotBlank() }
        // Rows of the table as it is, the names row first.
        val grid = (if (hasNames) listOf(cells.indices.map { names.getOrElse(it) { "" } }) else emptyList()) +
            (0 until rows).map { r -> cells.map { it.getOrElse(r) { "" } } }
        if (grid.isEmpty()) return names to cells
        val newCols = grid.map { it }  // each old row becomes a column
        return if (hasNames) newCols.map { it.first() } to newCols.map { it.drop(1).ifEmpty { listOf("") } }
        else List(newCols.size) { "" } to newCols
    }

    // ---- Smart fill -------------------------------------------------------------------------

    private val monthsShort = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val monthsLong = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    private val daysShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private val daysLong = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    /**
     * What dragging the fill handle puts [steps] cells on from [t] (negative: up or left), as
     * Excel fills one cell: month and weekday names go on (Jan, Feb…; Monday, Tuesday…), text
     * ending in a whole number counts on (Week 1, Week 2; Q09, Q10), and anything else, plain
     * numbers included, is copied.
     */
    fun fillValue(t: String, steps: Int): String {
        val word = t.trim()
        for (list in listOf(monthsLong, monthsShort, daysLong, daysShort)) {
            val k = list.indexOfFirst { it.equals(word, ignoreCase = true) }
            if (k >= 0) {
                val next = list[Math.floorMod(k + steps, list.size)]
                return when {
                    word.all { !it.isLetter() || it.isUpperCase() } -> next.uppercase()
                    word.first().isLowerCase() -> next.lowercase()
                    else -> next
                }
            }
        }
        // Text then a whole number (not a plain number, which is copied, as Excel does).
        val m = Regex("""^(.*\D)(\d+)$""").find(t) ?: return t
        if (DataTable.number(t) != null) return t
        val digits = m.groupValues[2]
        val n = digits.toLong() + steps
        if (n < 0) return t
        return m.groupValues[1] + n.toString().padStart(digits.length, '0')
    }

    // ---- A range's summary ------------------------------------------------------------------

    /** What a spreadsheet shows for a selected range: its numbers' sum, average, count, smallest and largest, and how many cells are filled. */
    class Summary(val sum: Double, val average: Double?, val numbers: Int, val filled: Int, val min: Double?, val max: Double?)

    fun summary(t: DataTable, c0: Int, r0: Int, c1: Int, r1: Int): Summary {
        val values = ArrayList<Double>()
        var filled = 0
        for (c in minOf(c0, c1)..maxOf(c0, c1)) for (r in minOf(r0, r1)..maxOf(r0, r1)) {
            if (t.cell(c, r).isNotBlank()) filled++
            t.value(c, r)?.let { values += it }
        }
        return Summary(values.sum(), if (values.isEmpty()) null else values.sum() / values.size, values.size, filled, values.minOrNull(), values.maxOrNull())
    }

    /** A range as tab-separated text, as spreadsheets copy it (formulas give their values). */
    fun rangeText(t: DataTable, c0: Int, r0: Int, c1: Int, r1: Int): String =
        (minOf(r0, r1)..maxOf(r0, r1)).joinToString("\n") { r ->
            (minOf(c0, c1)..maxOf(c0, c1)).joinToString("\t") { c ->
                val text = t.cell(c, r)
                if (Sheet.isFormula(text)) t.value(c, r)?.let { DataTable.text(it) } ?: t.sheet.value(c, r).toString() else text
            }
        }

    // ---- AutoSum ---------------------------------------------------------------------------

    /** The functions AutoSum offers, as Excel's Σ button does. */
    val AUTO_FUNCTIONS = listOf("SUM", "AVERAGE", "COUNT", "MIN", "MAX")

    /**
     * Excel's AutoSum for cell ([c], [r]): [fn] of the run of numbers directly above it (or, if
     * there are none above, directly to its left), as a formula; null when there's nothing to sum.
     */
    fun autoSum(t: DataTable, c: Int, r: Int, fn: String = "SUM"): String? {
        fun isNumber(cc: Int, rr: Int) = t.value(cc, rr) != null
        var top = r
        while (top - 1 >= 0 && isNumber(c, top - 1)) top--
        if (top < r) return "=$fn(${Sheet.columnName(c)}${top + 1}:${Sheet.columnName(c)}$r)"
        var left = c
        while (left - 1 >= 0 && isNumber(left - 1, r)) left--
        if (left < c) return "=$fn(${Sheet.columnName(left)}${r + 1}:${Sheet.columnName(c - 1)}${r + 1})"
        return null
    }

    // ---- Insights ----------------------------------------------------------------------------

    /** A histogram: [edges] has one more entry than [counts]. */
    class Histogram(val edges: List<Double>, val counts: List<Int>)

    /** [values] in about √n bins (5 to 12) between their smallest and largest, with tidy edges. */
    fun histogram(values: List<Double>): Histogram? {
        if (values.isEmpty()) return null
        val lo = values.min(); val hi = values.max()
        if (hi == lo) return Histogram(listOf(lo - 0.5, lo + 0.5), listOf(values.size))
        val bins = kotlin.math.sqrt(values.size.toDouble()).toInt().coerceIn(5, 12)
        val step = Plot2D.niceStep(hi - lo, bins)
        val start = kotlin.math.floor(lo / step) * step
        val n = (kotlin.math.ceil((hi - start) / step).toInt()).coerceAtLeast(1)
        val counts = IntArray(n)
        values.forEach { v -> counts[((v - start) / step).toInt().coerceIn(0, n - 1)]++ }
        return Histogram((0..n).map { start + it * step }, counts.toList())
    }

    /** A straight-line trend y = slope·x + intercept through paired values, with Pearson's r. */
    class Trend(val slope: Double, val intercept: Double, val r: Double, val n: Int) { val r2 get() = r * r }

    fun trend(xs: List<Double>, ys: List<Double>): Trend? {
        val n = minOf(xs.size, ys.size)
        if (n < 3) return null
        val mx = xs.take(n).average(); val my = ys.take(n).average()
        var sxx = 0.0; var syy = 0.0; var sxy = 0.0
        for (k in 0 until n) { val dx = xs[k] - mx; val dy = ys[k] - my; sxx += dx * dx; syy += dy * dy; sxy += dx * dy }
        if (sxx == 0.0 || syy == 0.0) return null
        val slope = sxy / sxx
        return Trend(slope, my - slope * mx, sxy / kotlin.math.sqrt(sxx * syy), n)
    }

    /** The rows of [column] whose numbers lie beyond 1.5 interquartile ranges of the quartiles (Tukey's fences). */
    fun outliers(t: DataTable, column: Int): List<Int> {
        val rows = (0 until t.rowCount).filter { t.value(column, it) != null }
        if (rows.size < 4) return emptyList()
        val sorted = rows.map { t.value(column, it)!! }.sorted()
        fun quantile(p: Double): Double { val pos = p * (sorted.size - 1); val i = pos.toInt(); return sorted[i] + (sorted[minOf(i + 1, sorted.size - 1)] - sorted[i]) * (pos - i) }
        val q1 = quantile(0.25); val q3 = quantile(0.75); val iqr = q3 - q1
        return rows.filter { val v = t.value(column, it)!!; v < q1 - 1.5 * iqr || v > q3 + 1.5 * iqr }
    }

    /** Where [value] sits between [min] and [max], 0 to 1, for a color scale. */
    fun scalePosition(value: Double, min: Double, max: Double): Float = if (max <= min) 0.5f else ((value - min) / (max - min)).toFloat().coerceIn(0f, 1f)

    /** The color scale's color at [p] (0 red, 0.5 yellow, 1 green), as ARGB: Excel's red–yellow–green. */
    fun scaleColor(p: Float): Int {
        val (a, b, f) = if (p < 0.5f) Triple(0xF8696B, 0xFFEB84, p * 2) else Triple(0xFFEB84, 0x63BE7B, (p - 0.5f) * 2)
        fun ch(shift: Int) = (((a shr shift) and 0xFF) + (((b shr shift) and 0xFF) - ((a shr shift) and 0xFF)) * f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
}
