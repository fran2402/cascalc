package com.example.cas.graph

import com.example.cas.cas.Expr
import com.example.cas.cas.Mat
import com.example.cas.cas.freeVars
import com.example.cas.editor.MathCodec
import com.example.cas.editor.MathRow
import com.example.cas.editor.Sym
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A figure, after matplotlib's: one or more axes laid out by a mosaic ("AB;CD"), each holding
 * layers drawn in order, one layer for each of pyplot's plotting functions (plot, scatter,
 * hist, boxplot, contourf…). A layer's inputs are math: an expression in x (a function), a list
 * or a table column (data), or a number. The figure becomes a [Scene], so the screen and every
 * export (PDF, PNG, SVG) show the same picture; [FigurePython] writes it as a matplotlib script.
 */
object Figure {
    enum class Group(val title: String) {
        Lines("Lines"), Distributions("Distributions"), Categorical("Categorical"), Fields("Fields"), Guides("Guides & text")
    }

    /**
     * The layer types: pyplot's name, the name shown, its group, the inputs it takes (math), and
     * [text], the name of its text input (labels, a note), if it has one. Saved by name, so the
     * order can change.
     */
    enum class Kind(val py: String, val title: String, val group: Group, val inputs: List<String>, val text: String? = null, val about: String) {
        Plot("plot", "Line", Group.Lines, listOf("x", "y", "x error", "y error"), about = "y against x: a function of x (x left empty), or columns; errors as ± bars"),
        Scatter("scatter", "Scatter", Group.Lines, listOf("x", "y", "x error", "y error"), about = "Points (x, y); errors as ± bars"),
        Step("step", "Steps", Group.Lines, listOf("x", "y"), about = "A staircase through the values"),
        Stem("stem", "Stems", Group.Lines, listOf("x", "y"), about = "A stem from the axis to each value"),
        FillBetween("fill_between", "Fill between", Group.Lines, listOf("x", "y₁", "y₂"), about = "The region between two curves or columns (y₂ is 0 when empty)"),
        Stack("stackplot", "Stacked areas", Group.Lines, listOf("x", "y₁, y₂, …"), about = "Several curves or columns, stacked"),
        Parametric("plot", "Parametric", Group.Lines, listOf("x(t)", "y(t)", "from", "to"), about = "A curve (x(t), y(t)); t from 0 to 2π unless given"),
        Polar("plot", "Polar curve", Group.Lines, listOf("r(θ)", "from", "to"), about = "r as a function of θ, from 0 to 2π unless given"),
        Fill("fill", "Polygon", Group.Lines, listOf("x", "y"), about = "A filled shape through the points"),
        Fit("plot", "Fitted curve", Group.Lines, listOf("x", "y"), about = "The best curve through the points (line, polynomial, exponential, power, logarithm), with its equation and R² in the legend"),
        Hist("hist", "Histogram", Group.Distributions, listOf("data"), about = "Counts in bins; set the bins, density and cumulative in the style"),
        Box("boxplot", "Box plot", Group.Distributions, listOf("data, data, …"), text = "Labels", about = "Quartiles, whiskers at 1.5 IQR and outliers; one box per list"),
        Violin("violinplot", "Violin", Group.Distributions, listOf("data, data, …"), text = "Labels", about = "The shape of each list's distribution, with its median"),
        Ecdf("ecdf", "ECDF", Group.Distributions, listOf("data"), about = "The share of values ≤ x"),
        Kde("plot", "Density curve", Group.Distributions, listOf("data"), about = "A smooth estimate of the data's distribution (Gaussian kernel), to lay over a histogram with density on"),
        Hist2D("hist2d", "2D histogram", Group.Distributions, listOf("x", "y"), about = "Counts of (x, y) pairs in a grid of bins"),
        Hexbin("hexbin", "Hexbin", Group.Distributions, listOf("x", "y"), about = "Counts of (x, y) pairs in hexagons"),
        Bar("bar", "Bars", Group.Categorical, listOf("heights"), text = "Labels", about = "One bar per value; several bar layers stand side by side"),
        BarH("barh", "Horizontal bars", Group.Categorical, listOf("widths"), text = "Labels", about = "Bars lying along x"),
        Pie("pie", "Pie", Group.Categorical, listOf("values"), text = "Labels", about = "Each value's share of the whole"),
        Events("eventplot", "Events", Group.Categorical, listOf("positions, positions, …"), about = "A tick at each position; one row per list"),
        Contour("contour", "Contour lines", Group.Fields, listOf("x", "y", "z or f(x, y)"), about = "Lines where z is constant: f(x, y), or columns x, y, z"),
        ContourF("contourf", "Filled contours", Group.Fields, listOf("x", "y", "z or f(x, y)"), about = "Bands of z, with a color bar: f(x, y), or columns x, y, z"),
        Heatmap("imshow", "Heat map", Group.Fields, listOf("x", "y", "z, f(x, y) or a matrix"), about = "z as colors: f(x, y), columns x, y, z, or a matrix's entries"),
        Quiver("quiver", "Arrows", Group.Fields, listOf("x", "y", "u", "v"), about = "The vector (u, v) as arrows: functions of x and y on a grid, or columns at points (x, y)"),
        Stream("streamplot", "Stream lines", Group.Fields, listOf("x", "y", "u", "v"), about = "Paths that follow the vector (u, v): functions of x and y, or columns"),
        HLine("axhline", "Horizontal line", Group.Guides, listOf("y"), about = "A line across at this y (a list: one at each)"),
        VLine("axvline", "Vertical line", Group.Guides, listOf("x"), about = "A line up at this x (a list: one at each)"),
        HSpan("axhspan", "Horizontal band", Group.Guides, listOf("from", "to"), about = "A shaded band between two y values"),
        VSpan("axvspan", "Vertical band", Group.Guides, listOf("from", "to"), about = "A shaded band between two x values"),
        Text("text", "Text", Group.Guides, listOf("x", "y"), text = "Text", about = "A note at (x, y); \$math\$ in dollars"),
        Annotate("annotate", "Arrow & note", Group.Guides, listOf("x", "y", "text x", "text y"), text = "Text", about = "An arrow to (x, y) from a note");

        /** Layers drawn with a colormap (and a color bar) rather than one color. */
        val mapped get() = this == ContourF || this == Heatmap || this == Hist2D || this == Hexbin || this == Contour
        /** Layers whose look is a line (so line style and markers apply). */
        val lined get() = this in setOf(Plot, Step, Stem, Parametric, Polar, Ecdf, HLine, VLine, Scatter, Contour, Quiver, Stream, Annotate, Fit, Kde)

        companion object {
            fun named(name: String): Kind? = entries.firstOrNull { it.name == name }
            /** Kinds that take ± errors: where their y error and x error inputs are. */
            val ERRORS = mapOf(Plot to (3 to 2), Scatter to (3 to 2))
        }
    }

    /**
     * How a layer looks. [color] is a place in the theme's color cycle, or −1 to take the next
     * one; [line] a [LineStyle] and [marker] a [Marker] by position (−1: none); [bins] 0 for
     * matplotlib's "auto"; [levels] contour levels.
     */
    class Style(
        val color: Int = -1, val line: Int = 0, val marker: Int = -1, val width: Double = 1.5, val alpha: Double = 1.0,
        val label: String = "", val bins: Int = 0, val levels: Int = 10, val cmap: String = "viridis",
        val density: Boolean = false, val cumulative: Boolean = false, val size: Double = 3.0,
        /** A color picked freely (ARGB), over [color]; 0 for none. */
        val argb: Int = 0,
        /** A fitted curve's model ([FitModel] by position; [CUSTOM_FIT] for [formula]). */
        val model: Int = 0,
        /** The colorbar's label (text with \$math\$), for layers drawn through a colormap. */
        val cbar: String = "",
        /** A fitted curve's own model as plain text in x and its parameters: a*e^(-b*x) + c. */
        val formula: String = "",
        /** Error bars: their caps' half width (0: none), thickness, and drawn as a shaded band rather than bars. */
        val errCap: Double = 3.0, val errWidth: Double = 0.9, val errBand: Boolean = false,
        /** Arrows (as the 2D graph's vector fields): colored by length along [cmap], [VectorField.Length] and scale, [VectorField.Tip] and its size, how many across. */
        val arrowByLength: Boolean = true, val arrowLength: Int = 0, val arrowScale: Double = 0.9, val arrowTip: Int = 0, val arrowTipSize: Double = 0.8, val arrows: Int = 17,
    ) {
        fun copy(color: Int = this.color, line: Int = this.line, marker: Int = this.marker, width: Double = this.width, alpha: Double = this.alpha,
                 label: String = this.label, bins: Int = this.bins, levels: Int = this.levels, cmap: String = this.cmap,
                 density: Boolean = this.density, cumulative: Boolean = this.cumulative, size: Double = this.size, argb: Int = this.argb, model: Int = this.model,
                 cbar: String = this.cbar, formula: String = this.formula, errCap: Double = this.errCap, errWidth: Double = this.errWidth, errBand: Boolean = this.errBand,
                 arrowByLength: Boolean = this.arrowByLength, arrowLength: Int = this.arrowLength, arrowScale: Double = this.arrowScale, arrowTip: Int = this.arrowTip,
                 arrowTipSize: Double = this.arrowTipSize, arrows: Int = this.arrows) =
            Style(color, line, marker, width, alpha, label, bins, levels, cmap, density, cumulative, size, argb, model, cbar, formula, errCap, errWidth, errBand,
                arrowByLength, arrowLength, arrowScale, arrowTip, arrowTipSize, arrows)
    }

    /** [Style.model] for a fitted curve's own formula, after the ready-made [FitModel]s. */
    val CUSTOM_FIT get() = FitModel.entries.size

    class Layer(val kind: Kind, val inputs: List<MathRow>, val text: String = "", val style: Style = Style(), val visible: Boolean = true) {
        fun input(k: Int): MathRow = inputs.getOrNull(k) ?: MathRow()
        fun copy(inputs: List<MathRow> = this.inputs, text: String = this.text, style: Style = this.style, visible: Boolean = this.visible) = Layer(kind, inputs, text, style, visible)
    }

    /**
     * One axes: its titles (text with \$math\$), limits (null: fitted to what's drawn), log
     * scales, grid (0 none, 1 major, 2 major and minor), legend (−1 none, 0 best, 1–9 the
     * places upper left … lower right), spines (0 box, 1 left and bottom, 2 through zero,
     * 3 none) and equal scales.
     */
    class Axes(
        val layers: List<Layer> = emptyList(),
        val title: String = "", val xLabel: String = "", val yLabel: String = "",
        val xMin: Double? = null, val xMax: Double? = null, val yMin: Double? = null, val yMax: Double? = null,
        val xLog: Boolean = false, val yLog: Boolean = false, val grid: Int = 1, val legend: Int = 0, val spines: Int = 0, val equal: Boolean = false,
        /** The numbers on each axis shown; each axis turned round (high to low); ticks pointing in; the legend in a box. */
        val xNumbers: Boolean = true, val yNumbers: Boolean = true, val xInvert: Boolean = false, val yInvert: Boolean = false,
        val ticksIn: Boolean = false, val legendFrame: Boolean = true,
    ) {
        fun copy(layers: List<Layer> = this.layers, title: String = this.title, xLabel: String = this.xLabel, yLabel: String = this.yLabel,
                 xMin: Double? = this.xMin, xMax: Double? = this.xMax, yMin: Double? = this.yMin, yMax: Double? = this.yMax,
                 xLog: Boolean = this.xLog, yLog: Boolean = this.yLog, grid: Int = this.grid, legend: Int = this.legend, spines: Int = this.spines, equal: Boolean = this.equal,
                 xNumbers: Boolean = this.xNumbers, yNumbers: Boolean = this.yNumbers, xInvert: Boolean = this.xInvert, yInvert: Boolean = this.yInvert,
                 ticksIn: Boolean = this.ticksIn, legendFrame: Boolean = this.legendFrame) =
            Axes(layers, title, xLabel, yLabel, xMin, xMax, yMin, yMax, xLog, yLog, grid, legend, spines, equal, xNumbers, yNumbers, xInvert, yInvert, ticksIn, legendFrame)
    }

    /**
     * A figure: its title (matplotlib's suptitle), the [layout] mosaic (rows split by ';', one
     * letter per axes, '.' for an empty cell), its axes in the order their letters first appear,
     * the [theme] (see [THEMES]), its height as a share of its width, and its own data table,
     * whose columns named with letters stand for their values in the layers.
     */
    class Doc(
        val id: String, val title: String, val axes: List<Axes>, val layout: String = "A", val theme: Int = 0,
        val aspect: Double = 0.75, val data: DataTable = emptyData(), val edited: Long = 0L, val name: String = "",
        /** The layout's columns' and rows' shares of the width and height (empty or mismatched: equal). */
        val widths: List<Double> = emptyList(), val heights: List<Double> = emptyList(),
        /** More tables after [data] (a workbook's other sheets, other files), and [data]'s own name. */
        val more: List<Sheet> = emptyList(), val sheetName: String = "",
    ) {
        fun copy(title: String = this.title, axes: List<Axes> = this.axes, layout: String = this.layout, theme: Int = this.theme,
                 aspect: Double = this.aspect, data: DataTable = this.data, edited: Long = this.edited, name: String = this.name,
                 widths: List<Double> = this.widths, heights: List<Double> = this.heights, more: List<Sheet> = this.more, sheetName: String = this.sheetName) =
            Doc(id, title, axes, layout, theme, aspect, data, edited, name, widths, heights, more, sheetName)

        /** Every table of the figure, [data] first. */
        val sheets: List<Sheet> get() = listOf(Sheet(sheetName, data)) + more

        /** The figure with its tables [list] (at least one). */
        fun withSheets(list: List<Sheet>): Doc = if (list.isEmpty()) copy(data = emptyData(), sheetName = "", more = emptyList()) else copy(data = list[0].table, sheetName = list[0].name, more = list.drop(1))

        /** The figure with table [k] replaced by [t]. */
        fun withTable(k: Int, t: DataTable): Doc = withSheets(sheets.mapIndexed { i, s -> if (i == k) Sheet(s.name, t) else s })
    }

    /** One of a figure's tables: its name (a file's or a sheet's) and its numbers. */
    class Sheet(val name: String, val table: DataTable)

    /** Every table's columns as variables (the first table's name wins when two share one). */
    fun variables(d: Doc): Map<String, Expr> {
        val out = LinkedHashMap<String, Expr>()
        d.sheets.forEach { s -> tableVariables(s.table).forEach { (k, v) -> out.putIfAbsent(k, v) } }
        return out
    }

    /**
     * [added] tables put in the figure after its own. Column names are made typeable, and a
     * name another table already has gets _2, _3 (so each column can be named on its own).
     * An empty first table is replaced rather than kept.
     */
    fun addSheets(d: Doc, added: List<Sheet>): Doc {
        val empty = d.more.isEmpty() && d.data.columns.all { c -> c.all { it.isBlank() } }
        val kept = if (empty) emptyList() else d.sheets
        val taken = HashSet<String>(kept.flatMap { s -> s.table.names.map { it.trim() } })
        val fresh = added.map { s ->
            val t = s.table
            val names = t.names.mapIndexed { c, raw ->
                var n = columnName(raw).ifEmpty { "c${c + 1}" }
                if (n in taken || n == "e" || n == "i") n = (2..999).map { k -> "${n}_$k" }.first { it !in taken }
                taken += n
                n
            }
            Sheet(s.name, DataTable(names, t.columns, t.x, t.y, t.sigmaX, t.sigmaY, t.formats, t.frozen, t.styles))
        }
        return d.withSheets(kept + fresh)
    }

    /** Whether [name] can be typed as a column's name: a letter, then letters, digits or _ (x, freq, Signal, amp_2). */
    fun isColumnName(name: String) = name.isNotEmpty() && name[0].isLetter() && name.all { it.isLetterOrDigit() || it == '_' }

    /** [raw] made into a column name that can be typed: spaces and other marks as _, a letter first. */
    fun columnName(raw: String): String {
        val s = raw.trim().map { if (it.isLetterOrDigit() || it == '_') it else '_' }.joinToString("").replace(Regex("_+"), "_").trim('_')
        return when { s.isEmpty() -> ""; s[0].isLetter() -> s; else -> "c$s" }
    }

    /** A table's columns with names that can be typed ([isColumnName]), each as a list (a column vector) of its numbers. */
    fun tableVariables(t: DataTable): Map<String, Expr> {
        val out = LinkedHashMap<String, Expr>()
        t.names.forEachIndexed { c, raw ->
            val name = raw.trim().takeIf { isColumnName(it) } ?: return@forEachIndexed
            val col = (0 until t.rowCount).mapNotNull { r -> t.value(c, r)?.let { com.example.cas.cas.Flt(it) } }
            if (col.isNotEmpty()) out[name] = Mat(col.size, 1, col)
        }
        return out
    }

    /** The letters functions are drawn against: a table column with one of these names gives way when it makes no sense there (sin of a column). */
    val PLOT_LETTERS = setOf("x", "y", "t", "θ")

    /** [eval] with [vars], reading table columns as in a spreadsheet: see [ColumnEval]. */
    fun withFallback(vars: Map<String, Expr>, eval: (Map<String, Expr>, MathRow) -> Expr): (MathRow) -> Expr = ColumnEval(vars, eval)

    /**
     * Inputs worked out with the table's columns: first as whole columns (mean(y), 2y); if that
     * fails, row by row, so sin(y), y^2 or x·y give a column of their own (each row's value); and
     * last without the plotting letters, so x is the variable. Layers that draw functions of x and
     * y (contours, arrows) take x and y as variables first, through [asFunction].
     */
    class ColumnEval(given: Map<String, Expr>, private val eval: (Map<String, Expr>, MathRow) -> Expr) : (MathRow) -> Expr {
        // Names longer than a letter (freq, Signal) are typed letter by letter; each stands for one
        // symbol of its own, which the row's run of letters is turned into before it's worked out.
        private val aliases: Map<String, String> = given.keys.filter { it.length > 1 && isColumnName(it) }.sortedByDescending { it.length }
            .mapIndexed { k, n -> n to (ALIAS_BASE + k).toString() }.toMap()
        private val base: Map<String, Expr> = given.mapKeys { (k, _) -> aliases[k] ?: k }
        /** Parts of columns (Signal[3:10]) met so far, each with its own symbol. */
        private val slices = LinkedHashMap<String, Pair<String, Expr>>()
        private val vars: Map<String, Expr> get() = if (slices.isEmpty()) base else base + slices.values.toMap()
        private val free get() = vars - PLOT_LETTERS
        private val columns get() = vars.filterValues { it is Mat && it.cols == 1 && it.rows > 1 }.mapValues { it.value as Mat }

        override fun invoke(row: MathRow): Expr = named(row).let { r ->
            val all = vars; val f = free
            try { eval(all, r) } catch (e: Exception) {
                rowwise(r) ?: if (f.size == all.size) throw e else eval(f, r)
            }
        }

        /** As a function of the plotting letters where it can be one. */
        fun asFunction(row: MathRow): Expr { val r = named(row); val f = free; return if (f.size == vars.size) invoke(row) else try { eval(f, r) } catch (e: Exception) { invoke(row) } }

        /**
         * [row] with each long column name, spelled out in letters, as its one symbol; and a
         * column followed by rows in brackets ([3:10] rows 3 to 10 as the table numbers them,
         * [5] row 5's number, [:20], [-10:] the last ten) as that part of it.
         */
        private fun named(row: MathRow): MathRow {
            val out = MathCodec.copy(row)
            fun letter(n: com.example.cas.editor.Node?) = n is Sym && n.text.length == 1 && (n.text[0].isLetterOrDigit() || n.text[0] == '_')
            val short = base.keys.filter { it.length == 1 && base[it] is Mat }.toSet()
            fun walk(r: MathRow) {
                var k = 0
                while (k < r.items.size) {
                    val prev = r.items.getOrNull(k - 1)
                    val afterLetter = prev is Sym && prev.text.length == 1 && prev.text[0].isLetter()
                    // A name, then not a letter, or straight away another column (freqSignal is freq · Signal).
                    fun spells(at: Int, name: String) = at + name.length <= r.items.size && name.indices.all { j -> (r.items[at + j] as? Sym)?.text == name[j].toString() }
                    fun startsColumn(at: Int): Boolean = aliases.keys.any { spells(at, it) } || ((r.items.getOrNull(at) as? Sym)?.text in short && !letter(r.items.getOrNull(at + 1)))
                    val afterColumn = (prev as? Sym)?.text?.let { it.length == 1 && (it[0] >= ALIAS_BASE || it in short) } == true
                    val hit = if (afterLetter && !afterColumn) null else aliases.entries.firstOrNull { (name, _) ->
                        spells(k, name) && (!letter(r.items.getOrNull(k + name.length)) || startsColumn(k + name.length))
                    }
                    if (hit != null) {
                        repeat(hit.key.length) { r.removeAt(k) }
                        r.add(k, Sym(hit.value))
                    }
                    val here = (r.items[k] as? Sym)?.text
                    if (here != null && (hit != null || (here in short && !afterLetter)) && (r.items.getOrNull(k + 1) as? Sym)?.text == "[") {
                        val end = (k + 2 until r.items.size).firstOrNull { (r.items[it] as? Sym)?.text == "]" }
                        val inside = end?.let { e -> (k + 2 until e).map { (r.items[it] as? Sym)?.text ?: "?" }.joinToString("") }
                        val part = inside?.let { slice(here, it) }
                        if (end != null && part != null) {
                            repeat(end - k + 1) { r.removeAt(k) }
                            r.add(k, Sym(part))
                        }
                    }
                    r.items.getOrNull(k)?.slots?.forEach(::walk)
                    k++
                }
            }
            walk(out)
            return out
        }

        /** The symbol for rows [spec] ("3:10", "5", ":20", "-10:") of the column whose symbol is [col]; null if it isn't one. */
        private fun slice(col: String, spec: String): String? {
            val m = base[col] as? Mat ?: return null
            val t = spec.replace("−", "-")
            val n = m.cells.size
            fun at(s: String, def: Int): Int? = if (s.isBlank()) def else s.toIntOrNull()?.let { if (it < 0) n + it + 1 else it }
            val key = "$col[$t]"
            slices[key]?.let { return it.first }
            val value: Expr = if (':' in t) {
                val a = at(t.substringBefore(':'), 1) ?: return null; val b = at(t.substringAfter(':'), n) ?: return null
                val lo = a.coerceIn(1, n); val hi = b.coerceIn(lo, n)
                Mat(hi - lo + 1, 1, m.cells.subList(lo - 1, hi))
            } else {
                val i = at(t, 0) ?: return null
                m.cells.getOrNull(i - 1) ?: return null
            }
            val symbol = (ALIAS_BASE + 200 + slices.size).toString()
            slices[key] = symbol to value
            return symbol
        }

        private fun uses(row: MathRow, name: String): Boolean = row.items.any { n -> (n is Sym && n.text == name) || n.slots.any { uses(it, name) } }

        private fun rowwise(row: MathRow): Expr? {
            val used = columns.keys.filter { uses(row, it) }
            if (used.isEmpty()) return null
            val n = used.minOf { columns.getValue(it).rows }
            val cols = used.map { columns.getValue(it).cells }
            // Quick: the columns left as letters, the rest compiled once, then each row's numbers put in.
            val e = runCatching { eval(vars - used.toSet(), row) }.getOrNull()
            val letters = e?.freeVars()?.filter { it != "i" }?.toSet()
            if (e != null && letters != null && letters == used.toSet()) {
                val f = runCatching { Compiler.compile(e, used) }.getOrNull()
                if (f != null) {
                    val arg = DoubleArray(used.size)
                    return Mat(n, 1, List(n) { r -> for (k in used.indices) arg[k] = real(cols[k][r]); com.example.cas.cas.Flt(runCatching { f(arg) }.getOrDefault(Double.NaN)) })
                }
            }
            // Otherwise each row worked out on its own.
            var ok = 0
            val out = List(n) { r ->
                val v = runCatching { real(eval(vars + used.indices.associate { used[it] to cols[it][r] }, row)) }.getOrDefault(Double.NaN)
                if (v.isFinite()) ok++
                com.example.cas.cas.Flt(v)
            }
            return if (ok > 0) Mat(n, 1, out) else null
        }
    }

    /** Where the stand-in symbols for long column names start (letters nobody types). */
    private const val ALIAS_BASE = '\u3041'


    /** A fitted curve's own model: its parameters (its letters other than x), as a function of x and them, and the formula. */
    class CustomModel(val params: List<String>, val f: (Double, DoubleArray) -> Double, val row: MathRow)

    /**
     * A fitted curve's own model kept exactly as typed on the keypad (symbols you build, x₀,
     * included): the calculator's own encoding after a marker. Plain text (a*e^(-b*x) + c) is
     * still read, as before.
     */
    private const val EXACT = "\u0001"
    fun formulaOf(row: MathRow): String = EXACT + MathCodec.encode(row)
    fun formulaRow(formula: String): MathRow =
        if (formula.startsWith(EXACT)) runCatching { MathCodec.decode(formula.removePrefix(EXACT)) }.getOrDefault(MathRow())
        else com.example.cas.engine.PlainMath.parse(formula.substringAfter('=').trim())
    /** The formula as plain text (for the Python export). */
    fun formulaText(formula: String): String = com.example.cas.engine.PlainMath.text(formulaRow(formula))

    fun customModel(formula: String): CustomModel? {
        val row = formulaRow(formula)
        if (row.items.isEmpty()) return null
        val e = runCatching { com.example.cas.engine.Evaluator().evaluate(MathCodec.copy(row)) }.getOrNull() ?: return null
        val params = e.freeVars().filter { it != "i" && it != "x" }.distinct().sorted()
        if (params.isEmpty()) return null
        val c = runCatching { Compiler.compile(e, listOf("x") + params) }.getOrNull() ?: return null
        val arg = DoubleArray(params.size + 1)
        return CustomModel(params, { x, p -> arg[0] = x; for (k in p.indices) arg[k + 1] = p[k]; runCatching { c(arg) }.getOrDefault(Double.NaN) }, row)
    }

    /** [row] with each of [params] replaced by its fitted number. */
    private fun withNumbers(row: MathRow, params: List<String>, values: DoubleArray): MathRow {
        val out = MathCodec.copy(row)
        fun walk(r: MathRow) {
            var k = 0
            while (k < r.items.size) {
                val n = r.items[k]
                val at = (n as? Sym)?.let { params.indexOf(it.text) } ?: -1
                if (at >= 0) {
                    r.removeAt(k)
                    val v = values[at]
                    val digits = tickText(abs(v)).replace("−", "")
                    val syms = (if (v < 0) listOf("(", "−") else emptyList()) + digits.map { it.toString() } + (if (v < 0) listOf(")") else emptyList())
                    syms.forEach { r.add(k++, Sym(it)) }
                } else { n.slots.forEach(::walk); k++ }
            }
        }
        walk(out)
        return out
    }

    fun emptyData() = DataTable(listOf("x", "y"), listOf(List(5) { "" }, List(5) { "" }), null, null)

    // ---- Layouts ------------------------------------------------------------------------------

    /** The layouts offered: one axes, side by side, stacked, a 2 × 2 grid, and two mosaics. */
    val LAYOUTS = listOf("A", "AB", "A;B", "AB;CD", "AAB;CDB", "AA;BC", "ABC")

    /** Each letter's cell in the mosaic as (column, row, columns spanned, rows spanned), in order of first appearance; with the grid's size. */
    fun cells(layout: String): Triple<List<IntArray>, Int, Int> {
        val rows = layout.split(';').map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("A") }
        val cols = rows.maxOf { it.length }
        val order = ArrayList<Char>()
        rows.forEach { r -> r.forEach { c -> if (c != '.' && c !in order) order += c } }
        val out = order.map { ch ->
            var c0 = cols; var c1 = -1; var r0 = rows.size; var r1 = -1
            rows.forEachIndexed { r, line -> line.forEachIndexed { c, x -> if (x == ch) { c0 = min(c0, c); c1 = max(c1, c); r0 = min(r0, r); r1 = max(r1, r) } } }
            intArrayOf(c0, r0, c1 - c0 + 1, r1 - r0 + 1)
        }
        return Triple(out, cols, rows.size)
    }

    /** How many axes a layout has. */
    fun axesCount(layout: String) = cells(layout).first.size

    /** The figure's axes fitted to [layout]: existing ones kept in order, new empty ones added. */
    fun withLayout(d: Doc, layout: String): Doc {
        val n = axesCount(layout)
        val axes = List(n) { d.axes.getOrNull(it) ?: Axes() }
        val (_, cols, rows) = cells(layout)
        return d.copy(layout = layout, axes = axes, widths = d.widths.takeIf { it.size == cols } ?: emptyList(), heights = d.heights.takeIf { it.size == rows } ?: emptyList())
    }

    // ---- Themes -------------------------------------------------------------------------------

    /** Colors a figure is drawn in: page, plot face, ink (frame, text), muted ink (ticks), grid, and the color cycle. */
    class Theme(val name: String, val background: Int, val face: Int, val ink: Int, val muted: Int, val grid: Int, val palette: List<Int>, val gridOnTop: Boolean = false,
                /** Labels in the app's sans-serif rather than LaTeX's Computer Modern. */
                val sans: Boolean = false)

    private fun c(v: Long) = v.toInt()

    /**
     * The looks offered, after plt.style: the app's own colors (0, given when drawing), then
     * SciencePlots, SciencePlots dark, ggplot, grayscale and seaborn.
     */
    val THEMES: List<Theme> = listOf(
        Theme("App", c(0xFFFFFFFF), c(0xFFFFFFFF), c(0xFF000000), c(0xFF555555), c(0xFFDDDDDD), Pgf.SCIENCE),
        Theme("Science", c(0xFFFFFFFF), c(0xFFFFFFFF), c(0xFF000000), c(0xFF333333), c(0xFFD9D9D9), Pgf.SCIENCE),
        Theme("Science dark", c(0xFF141414), c(0xFF141414), c(0xFFEDEDED), c(0xFFBDBDBD), c(0xFF3A3A3A), Pgf.SCIENCE.dropLast(1) + c(0xFFBDBDBD)),
        Theme("ggplot", c(0xFFFFFFFF), c(0xFFE5E5E5), c(0xFF333333), c(0xFF555555), c(0xFFFFFFFF),
            listOf(c(0xFFE24A33), c(0xFF348ABD), c(0xFF988ED5), c(0xFF777777), c(0xFFFBC15E), c(0xFF8EBA42), c(0xFFFFB5B8)), sans = true),
        Theme("Grayscale", c(0xFFFFFFFF), c(0xFFFFFFFF), c(0xFF000000), c(0xFF444444), c(0xFFDDDDDD),
            listOf(c(0xFF000000), c(0xFF555555), c(0xFF888888), c(0xFFAAAAAA), c(0xFF333333), c(0xFF777777))),
        Theme("Seaborn", c(0xFFFFFFFF), c(0xFFEAEAF2), c(0xFF262626), c(0xFF4D4D4D), c(0xFFFFFFFF),
            listOf(c(0xFF4C72B0), c(0xFFDD8452), c(0xFF55A868), c(0xFFC44E52), c(0xFF8172B3), c(0xFF937860), c(0xFFDA8BC3), c(0xFF8C8C8C)), sans = true),
        // Added later (saved by position): journals, popular plt.style sheets and editor themes.
        Theme("Nature", c(0xFFFFFFFF), c(0xFFFFFFFF), c(0xFF000000), c(0xFF333333), c(0xFFE6E6E6),
            listOf(c(0xFFE64B35), c(0xFF4DBBD5), c(0xFF00A087), c(0xFF3C5488), c(0xFFF39B7F), c(0xFF8491B4), c(0xFF91D1C2)), sans = true),
        Theme("IEEE", c(0xFFFFFFFF), c(0xFFFFFFFF), c(0xFF000000), c(0xFF222222), c(0xFFDDDDDD),
            listOf(c(0xFF000000), c(0xFFFF0000), c(0xFF0000FF), c(0xFF008000), c(0xFFFF8000), c(0xFF800080))),
        Theme("FiveThirtyEight", c(0xFFF0F0F0), c(0xFFF0F0F0), c(0xFF222222), c(0xFF555555), c(0xFFCBCBCB),
            listOf(c(0xFF008FD5), c(0xFFFC4F30), c(0xFFE5AE38), c(0xFF6D904F), c(0xFF8B8B8B), c(0xFF810F7C)), sans = true),
        Theme("Tableau", c(0xFFFFFFFF), c(0xFFFFFFFF), c(0xFF333333), c(0xFF555555), c(0xFFE0E0E0),
            listOf(c(0xFF4E79A7), c(0xFFF28E2B), c(0xFFE15759), c(0xFF76B7B2), c(0xFF59A14F), c(0xFFEDC948), c(0xFFB07AA1), c(0xFFFF9DA7)), sans = true),
        Theme("Colorblind", c(0xFFFFFFFF), c(0xFFFFFFFF), c(0xFF000000), c(0xFF333333), c(0xFFDDDDDD),
            listOf(c(0xFF0072B2), c(0xFFE69F00), c(0xFF009E73), c(0xFFCC79A7), c(0xFF56B4E9), c(0xFFD55E00), c(0xFFF0E442), c(0xFF000000))),
        Theme("Solarized", c(0xFFFDF6E3), c(0xFFFDF6E3), c(0xFF586E75), c(0xFF657B83), c(0xFFEEE8D5),
            listOf(c(0xFF268BD2), c(0xFFDC322F), c(0xFF859900), c(0xFFB58900), c(0xFF6C71C4), c(0xFF2AA198), c(0xFFCB4B16), c(0xFFD33682)), sans = true),
        Theme("Solarized dark", c(0xFF002B36), c(0xFF002B36), c(0xFF93A1A1), c(0xFF839496), c(0xFF073642),
            listOf(c(0xFF268BD2), c(0xFFDC322F), c(0xFF859900), c(0xFFB58900), c(0xFF6C71C4), c(0xFF2AA198), c(0xFFCB4B16), c(0xFFD33682)), sans = true),
        Theme("Nord", c(0xFF2E3440), c(0xFF2E3440), c(0xFFECEFF4), c(0xFFD8DEE9), c(0xFF3B4252),
            listOf(c(0xFF88C0D0), c(0xFFBF616A), c(0xFFA3BE8C), c(0xFFEBCB8B), c(0xFFB48EAD), c(0xFFD08770), c(0xFF5E81AC)), sans = true),
        Theme("Dracula", c(0xFF282A36), c(0xFF282A36), c(0xFFF8F8F2), c(0xFFBFBFBF), c(0xFF44475A),
            listOf(c(0xFFBD93F9), c(0xFFFF79C6), c(0xFF50FA7B), c(0xFFFFB86C), c(0xFF8BE9FD), c(0xFFFF5555), c(0xFFF1FA8C)), sans = true),
        Theme("Pastel", c(0xFFFFFFFF), c(0xFFFBFAF7), c(0xFF3A3A3A), c(0xFF6A6A6A), c(0xFFECEAE4),
            listOf(c(0xFF8DB3E2), c(0xFFF4A6A6), c(0xFFA8D5BA), c(0xFFFFD59E), c(0xFFC5B3E6), c(0xFF9ED9D9), c(0xFFF7C4E0)), sans = true),
        Theme("Paper", c(0xFFF7F1E3), c(0xFFF7F1E3), c(0xFF3B2F2F), c(0xFF5C4B43), c(0xFFE6DCC6),
            listOf(c(0xFF8C2D19), c(0xFF2F5D62), c(0xFFB08401), c(0xFF4E3B6E), c(0xFF6B8E23), c(0xFF555555))),
        Theme("Blueprint", c(0xFF0F3057), c(0xFF0F3057), c(0xFFE6F0FF), c(0xFFB8CCE6), c(0xFF1E4A7A),
            listOf(c(0xFFFFFFFF), c(0xFFFFD166), c(0xFF06D6A0), c(0xFFEF476F), c(0xFF8ECAE6), c(0xFFF4A261))),
        Theme("Neon", c(0xFF0B0B12), c(0xFF0B0B12), c(0xFFE8E8F0), c(0xFFA0A0B8), c(0xFF22222E),
            listOf(c(0xFF00F5D4), c(0xFFF15BB5), c(0xFFFEE440), c(0xFF00BBF9), c(0xFF9B5DE5), c(0xFFFF6B35)), sans = true),
    )

    // ---- The file -----------------------------------------------------------------------------

    private const val MAGIC = "CASFIG 1"

    private fun esc(s: String) = s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\r", "")
    private fun unesc(s: String): String = buildString {
        var k = 0
        while (k < s.length) {
            val ch = s[k]
            if (ch == '\\' && k + 1 < s.length) {
                when (s[k + 1]) { 'n' -> append('\n'); 't' -> append('\t'); else -> append(s[k + 1]) }
                k += 2
            } else { append(ch); k++ }
        }
    }
    private fun num(v: Double?) = v?.toString() ?: ""

    fun encode(d: Doc): String = buildString {
        appendLine(MAGIC)
        appendLine("format\t2")
        appendLine("title\t" + esc(d.title))
        appendLine("name\t" + esc(d.name))
        appendLine("layout\t" + esc(d.layout))
        appendLine("theme\t" + d.theme)
        appendLine("aspect\t" + d.aspect)
        appendLine("edited\t" + d.edited)
        appendLine("data\t" + esc(d.data.encode()))
        if (d.sheetName.isNotEmpty()) appendLine("sheetname\t" + esc(d.sheetName))
        d.more.forEach { appendLine("sheet\t" + esc(it.name) + "\t" + esc(it.table.encode())) }
        if (d.widths.isNotEmpty() || d.heights.isNotEmpty()) appendLine("ratios\t" + d.widths.joinToString(",") + "\t" + d.heights.joinToString(","))
        for (a in d.axes) {
            appendLine(listOf("A", esc(a.title), esc(a.xLabel), esc(a.yLabel), num(a.xMin), num(a.xMax), num(a.yMin), num(a.yMax),
                if (a.xLog) "1" else "0", if (a.yLog) "1" else "0", a.grid, a.legend, a.spines, if (a.equal) "1" else "0",
                listOf(a.xNumbers, a.yNumbers, a.xInvert, a.yInvert, a.ticksIn, a.legendFrame).joinToString("") { if (it) "1" else "0" }).joinToString("\t"))
            for (l in a.layers) {
                val s = l.style
                val flags = (if (s.density) 1 else 0) or (if (s.cumulative) 2 else 0)
                appendLine((listOf("L", l.kind.name, if (l.visible) "1" else "0", s.color, s.line, s.marker, s.width, s.alpha, s.bins, s.levels, esc(s.cmap), flags, s.size,
                    esc(s.label), esc(l.text), l.inputs.size) + l.inputs.map { esc(MathCodec.encode(it)) } + listOf(s.argb, s.model, esc(s.cbar), esc(s.formula), s.errCap, s.errWidth, if (s.errBand) "1" else "0",
                    if (s.arrowByLength) "1" else "0", s.arrowLength, s.arrowScale, s.arrowTip, s.arrowTipSize, s.arrows)).joinToString("\t"))
            }
        }
    }

    fun decode(id: String, text: String): Doc {
        val lines = text.lines()
        require(lines.firstOrNull()?.trim() == MAGIC) { "Not a figure file" }
        var title = ""; var name = ""; var layout = "A"; var theme = 0; var aspect = 0.75; var edited = 0L
        var data = emptyData()
        var widths = emptyList<Double>(); var heights = emptyList<Double>()
        var sheetName = ""; val more = ArrayList<Sheet>()
        // Files before format 2 had y before x and the fields' functions first.
        var format = 1
        val axes = ArrayList<Axes>()
        val layers = ArrayList<ArrayList<Layer>>()
        for (line in lines.drop(1)) {
            if (line.isBlank()) continue
            val f = line.split('\t')
            fun s(k: Int) = unesc(f.getOrElse(k) { "" })
            fun d(k: Int) = f.getOrNull(k)?.toDoubleOrNull()
            fun i(k: Int, def: Int) = f.getOrNull(k)?.toIntOrNull() ?: def
            when (f[0]) {
                "format" -> format = i(1, 1)
                "title" -> title = s(1)
                "name" -> name = s(1)
                "layout" -> layout = s(1).ifBlank { "A" }
                "theme" -> theme = i(1, 0)
                "aspect" -> aspect = d(1) ?: 0.75
                "edited" -> edited = f.getOrNull(1)?.toLongOrNull() ?: 0L
                "data" -> data = DataTable.decode(s(1)) ?: emptyData()
                "sheetname" -> sheetName = s(1)
                "sheet" -> DataTable.decode(s(2))?.let { more += Sheet(s(1), it) }
                "ratios" -> { widths = s(1).split(',').mapNotNull { it.toDoubleOrNull() }; heights = s(2).split(',').mapNotNull { it.toDoubleOrNull() } }
                "A" -> {
                    val opts = f.getOrNull(14) ?: "110001"
                    fun o(k: Int, def: Boolean) = opts.getOrNull(k)?.let { it == '1' } ?: def
                    axes += Axes(emptyList(), s(1), s(2), s(3), d(4), d(5), d(6), d(7), f.getOrNull(8) == "1", f.getOrNull(9) == "1", i(10, 1), i(11, 0), i(12, 0), f.getOrNull(13) == "1",
                        o(0, true), o(1, true), o(2, false), o(3, false), o(4, false), o(5, true))
                    layers.add(ArrayList())
                }
                "L" -> {
                    val kindName = f.getOrElse(1) { "" }
                    // Error bars were a kind of their own: now points with a y error.
                    val kind = (if (kindName == "ErrorBar") Kind.Scatter else Kind.named(kindName)) ?: continue
                    if (layers.isEmpty()) { axes += Axes(); layers.add(ArrayList()) }
                    val flags = i(11, 0)
                    val style = Style(i(3, -1), i(4, 0), i(5, -1), d(6) ?: 1.5, d(7) ?: 1.0, s(13), i(8, 0), i(9, 10), s(10).ifBlank { "viridis" }, flags and 1 != 0, flags and 2 != 0, d(12) ?: 3.0)
                    val n = i(15, 0)
                    val inputs = (0 until n).map { k -> runCatching { MathCodec.decode(s(16 + k)) }.getOrDefault(MathRow()) }
                    val style2 = style.copy(argb = i(16 + n, 0), model = i(17 + n, 0), cbar = s(18 + n), formula = s(19 + n),
                        errCap = d(20 + n) ?: 3.0, errWidth = d(21 + n) ?: 0.9, errBand = f.getOrNull(22 + n) == "1",
                        arrowByLength = f.getOrNull(23 + n) != "0", arrowLength = i(24 + n, 0), arrowScale = d(25 + n) ?: 0.9, arrowTip = i(26 + n, 0), arrowTipSize = d(27 + n) ?: 0.8, arrows = i(28 + n, 17))
                    fun at(k: Int) = inputs.getOrElse(k) { MathRow() }
                    val ins = if (format >= 2) inputs else when (kindName) {
                        "ErrorBar" -> listOf(at(0), at(1), MathRow(), at(2))
                        "Plot", "Step", "Stem" -> listOf(at(1), at(0), at(3), at(2))
                        "Scatter" -> listOf(at(0), at(1), at(4), at(3))
                        "FillBetween" -> listOf(at(2), at(0), at(1))
                        "Stack" -> listOf(at(1), at(0))
                        "Contour", "ContourF", "Heatmap" -> listOf(MathRow(), MathRow(), at(0))
                        "Quiver", "Stream" -> listOf(MathRow(), MathRow(), at(0), at(1))
                        else -> inputs
                    }.let { list -> list.dropLastWhile { it.items.isEmpty() } }
                    layers.last() += Layer(kind, ins, s(14), style2, f.getOrNull(2) != "0")
                }
            }
        }
        val all = axes.mapIndexed { k, a -> a.copy(layers = layers[k]) }
        return withLayout(Doc(id, title, all, layout, theme, aspect, data, edited, name, widths, heights, more, sheetName), layout)
    }

    // ---- Inputs as values ---------------------------------------------------------------------

    /** What an input works out to: nothing, a number, data, a matrix, a function of one letter, or of two. */
    sealed class Value {
        object Empty : Value()
        class Num(val v: Double) : Value()
        class Data(val v: DoubleArray) : Value()
        class Matrix(val rows: Int, val cols: Int, val v: DoubleArray) : Value()
        class Fn(val f: (Double) -> Double, val letter: String) : Value()
        class Fn2(val f: (Double, Double) -> Double, val letters: List<String>) : Value()
        class Bad(val message: String) : Value()
    }

    private fun real(e: Expr): Double = runCatching { com.example.cas.cas.Numeric.real(e) }.getOrDefault(Double.NaN)

    private fun sym(n: com.example.cas.editor.Node?, t: String) = n is Sym && n.text == t

    /** A list typed in brackets, [1, 4, 9] (data), or a list of lists, [[1, 2], [3, 4]] (a matrix); null if [row] isn't one. */
    private fun bracketed(row: MathRow, eval: (MathRow) -> Expr): Value? {
        val items = row.items
        if (items.size < 2 || !sym(items.first(), "[") || !sym(items.last(), "]")) return null
        val inner = MathCodec.decode(MathCodec.encode(items.subList(1, items.size - 1)))
        val cells = parts(inner)
        if (cells.isNotEmpty() && cells.all { c -> c.items.size >= 2 && sym(c.items.first(), "[") && sym(c.items.last(), "]") }) {
            val rows = cells.map { c -> (bracketed(c, eval) as? Value.Data)?.v ?: return Value.Bad("Each row of a matrix is a list of numbers") }
            val n = rows.maxOf { it.size }
            return Value.Matrix(rows.size, n, DoubleArray(rows.size * n) { k -> rows[k / n].getOrElse(k % n) { Double.NaN } })
        }
        val v = DoubleArray(cells.size)
        cells.forEachIndexed { k, c ->
            val e = try { eval(c) } catch (ex: Exception) { return Value.Bad((ex as? com.example.cas.cas.MathError)?.message ?: "Couldn't work this out") }
            v[k] = real(e)
        }
        return Value.Data(v)
    }

    /** [row] worked out by [eval] as one of [Value]. */
    fun value(row: MathRow, eval: (MathRow) -> Expr): Value {
        if (row.items.isEmpty()) return Value.Empty
        try { bracketed(row, eval)?.let { return it } } catch (ex: StackOverflowError) { return Value.Bad("That's too complicated") }
        val e = try { eval(row) } catch (ex: StackOverflowError) { return Value.Bad("That's too complicated") } catch (ex: Exception) {
            return Value.Bad((ex as? com.example.cas.cas.MathError)?.message ?: "Couldn't work this out")
        }
        if (e is Mat) {
            val v = DoubleArray(e.cells.size) { real(e.cells[it]) }
            return if (e.rows > 1 && e.cols > 1) Value.Matrix(e.rows, e.cols, v) else Value.Data(v)
        }
        val vars = e.freeVars().filter { it != "i" }.sorted()
        return when (vars.size) {
            0 -> real(e).let { if (it.isNaN()) Value.Bad("This isn't a real number") else Value.Num(it) }
            1 -> {
                val f = runCatching { Compiler.compile(e, vars) }.getOrNull() ?: return Value.Bad("Couldn't draw this")
                val arg = DoubleArray(1)
                Value.Fn({ x -> arg[0] = x; runCatching { f(arg) }.getOrDefault(Double.NaN) }, vars[0])
            }
            2 -> {
                val order = if ("x" in vars && "y" in vars) listOf("x", "y") else vars
                val f = runCatching { Compiler.compile(e, order) }.getOrNull() ?: return Value.Bad("Couldn't draw this")
                val arg = DoubleArray(2)
                Value.Fn2({ x, y -> arg[0] = x; arg[1] = y; runCatching { f(arg) }.getOrDefault(Double.NaN) }, order)
            }
            else -> Value.Bad("Too many letters: " + vars.joinToString(", "))
        }
    }

    /** [row] split at its top-level commas. */
    fun parts(row: MathRow): List<MathRow> {
        val out = ArrayList<MathRow>(); val cur = ArrayList<com.example.cas.editor.Node>()
        fun flush() { if (cur.isNotEmpty()) out += MathCodec.decode(MathCodec.encode(cur)); cur.clear() }
        var depth = 0
        for (n in row.items) {
            if (n is Sym && (n.text == "[" || n.text == "(")) depth++
            if (n is Sym && (n.text == "]" || n.text == ")")) depth--
            if (n is Sym && n.text == "," && depth == 0) flush() else cur += n
        }
        flush()
        return out
    }

    // ---- Working a figure out -----------------------------------------------------------------

    /** A rectangle of a field's samples: z[j][i] at x = x0 + i·(x1 − x0)/(nx − 1), y likewise. */
    class Grid(val x0: Double, val x1: Double, val y0: Double, val y1: Double, val nx: Int, val ny: Int, val z: Array<DoubleArray>) {
        fun x(i: Int) = x0 + (x1 - x0) * i / (nx - 1)
        fun y(j: Int) = y0 + (y1 - y0) * j / (ny - 1)
        val finite get() = z.flatMap { r -> r.filter { it.isFinite() } }
    }

    class BoxStats(val q1: Double, val median: Double, val q3: Double, val lo: Double, val hi: Double, val outliers: List<Double>, val mean: Double)

    /**
     * A layer worked out: its color, an error (shown under it in the list), and what it draws in
     * data units: [series] as (xs, ys) lines or points, [lower] the second edge of a band or the
     * error of each point, [grid] (and [grid2] for vectors) a field's samples, [values] and
     * [labels] for bars and pies, [boxes] for box and violin plots, [shapes] for violins and hexagons.
     */
    class Drawn(
        val layer: Layer, val color: Int, val error: String? = null,
        val series: List<Pair<DoubleArray, DoubleArray>> = emptyList(),
        val lower: List<DoubleArray> = emptyList(),
        val grid: Grid? = null, val grid2: Grid? = null,
        val values: DoubleArray = DoubleArray(0), val labels: List<String> = emptyList(),
        val boxes: List<BoxStats> = emptyList(), val shapes: List<Pair<DoubleArray, DoubleArray>> = emptyList(), val shade: DoubleArray = DoubleArray(0),
        val raw: List<DoubleArray> = emptyList(), val offset: Double = 0.0, val barWidth: Double = 0.8,
        /** ± errors of the points in y and in x (empty: none). */
        val errY: DoubleArray = DoubleArray(0), val errX: DoubleArray = DoubleArray(0),
    ) {
        fun copy(color: Int = this.color, error: String? = this.error) = Drawn(layer, color, error, series, lower, grid, grid2, values, labels, boxes, shapes, shade, raw, offset, barWidth, errY, errX)
        fun withLayer(l: Layer) = Drawn(l, color, error, series, lower, grid, grid2, values, labels, boxes, shapes, shade, raw, offset, barWidth, errY, errX)
    }

    /** An axes worked out: each layer drawn, the ranges shown, and its category ticks (bars, boxes). */
    class Worked(val axes: Axes, val drawn: List<Drawn>, val x0: Double, val x1: Double, val y0: Double, val y1: Double,
                 val xCategories: List<String>?, val yCategories: List<String>?, val pie: Boolean, val zMin: Double, val zMax: Double, val mapped: Drawn?) {
        /** Where it was drawn: the plot's frame (left, top, right, bottom) in the figure and the x and y it shows there (x0, x1, y0, y1). */
        var frame: DoubleArray = DoubleArray(0)

        /** The data point at figure position ([sx], [sy]), or null before it's drawn (straight axes; log axes in their logarithms). */
        fun toData(sx: Double, sy: Double): Pair<Double, Double>? {
            if (frame.size < 8) return null
            val (l, t, r, b) = frame.take(4)
            var fx = (sx - l) / (r - l); var fy = (b - sy) / (b - t)
            if (axes.xInvert) fx = 1 - fx
            if (axes.yInvert) fy = 1 - fy
            fun at(f: Double, lo: Double, hi: Double, log: Boolean) = if (log) 10.0.pow(log10(lo) + f * (log10(hi) - log10(lo))) else lo + f * (hi - lo)
            return at(fx, frame[4], frame[5], axes.xLog) to at(fy, frame[6], frame[7], axes.yLog)
        }

        /** Where data point ([x], [y]) is in the figure. */
        fun toFigure(x: Double, y: Double): Pair<Double, Double>? {
            if (frame.size < 8) return null
            val (l, t, r, b) = frame.take(4)
            fun f(v: Double, lo: Double, hi: Double, log: Boolean) = if (log) (log10(v) - log10(lo)) / (log10(hi) - log10(lo)) else (v - lo) / (hi - lo)
            var fx = f(x, frame[4], frame[5], axes.xLog); var fy = f(y, frame[6], frame[7], axes.yLog)
            if (axes.xInvert) fx = 1 - fx
            if (axes.yInvert) fy = 1 - fy
            return (l + fx * (r - l)) to (b - fy * (b - t))
        }
    }

    private class Bounds {
        var x0 = Double.POSITIVE_INFINITY; var x1 = Double.NEGATIVE_INFINITY; var y0 = Double.POSITIVE_INFINITY; var y1 = Double.NEGATIVE_INFINITY
        fun x(v: Double) { if (v.isFinite()) { x0 = min(x0, v); x1 = max(x1, v) } }
        fun y(v: Double) { if (v.isFinite()) { y0 = min(y0, v); y1 = max(y1, v) } }
        val hasX get() = x0 <= x1
        val hasY get() = y0 <= y1
    }

    private fun seq(n: Int) = DoubleArray(n) { (it + 1).toDouble() }
    private fun finite(v: DoubleArray) = v.filter { it.isFinite() }.toDoubleArray()

    /** The q-th quantile of sorted data, interpolated as numpy does. */
    fun quantile(sorted: DoubleArray, q: Double): Double {
        if (sorted.isEmpty()) return Double.NaN
        val p = (sorted.size - 1) * q
        val k = floor(p).toInt().coerceIn(0, sorted.lastIndex)
        val k1 = min(k + 1, sorted.lastIndex)
        return sorted[k] + (sorted[k1] - sorted[k]) * (p - k)
    }

    /** A box plot's numbers: quartiles, whiskers at the furthest data within 1.5 IQR, and the points beyond. */
    fun boxStats(data: DoubleArray): BoxStats? {
        val s = finite(data).sorted().toDoubleArray()
        if (s.isEmpty()) return null
        val q1 = quantile(s, 0.25); val q3 = quantile(s, 0.75); val iqr = q3 - q1
        val inside = s.filter { it >= q1 - 1.5 * iqr && it <= q3 + 1.5 * iqr }
        return BoxStats(q1, quantile(s, 0.5), q3, inside.minOrNull() ?: q1, inside.maxOrNull() ?: q3, s.filter { it < q1 - 1.5 * iqr || it > q3 + 1.5 * iqr }, s.average())
    }

    /** numpy's "auto" bin count: the larger of Sturges' and Freedman–Diaconis'. */
    fun autoBins(data: DoubleArray): Int {
        val s = finite(data).sorted().toDoubleArray()
        if (s.size < 2) return 1
        val sturges = ceil(ln(s.size.toDouble()) / ln(2.0)).toInt() + 1
        val iqr = quantile(s, 0.75) - quantile(s, 0.25)
        val span = s.last() - s.first()
        val fd = if (iqr > 0 && span > 0) ceil(span / (2 * iqr / s.size.toDouble().pow(1.0 / 3))).toInt() else 0
        return max(sturges, fd).coerceIn(1, 200)
    }

    /** Bin edges and counts (densities, running totals) for a histogram. */
    fun histogram(data: DoubleArray, bins: Int, density: Boolean = false, cumulative: Boolean = false): Pair<DoubleArray, DoubleArray> {
        val s = finite(data)
        if (s.isEmpty()) return DoubleArray(0) to DoubleArray(0)
        val n = if (bins > 0) bins else autoBins(s)
        var lo = s.min(); var hi = s.max()
        if (hi - lo < 1e-12) { lo -= 0.5; hi += 0.5 }
        val edges = DoubleArray(n + 1) { lo + (hi - lo) * it / n }
        val counts = DoubleArray(n)
        for (v in s) counts[min(((v - lo) / (hi - lo) * n).toInt(), n - 1)]++
        if (cumulative) for (k in 1 until n) counts[k] += counts[k - 1]
        if (density) { val w = (hi - lo) / n; val total = if (cumulative) counts.last() else s.size.toDouble(); for (k in 0 until n) counts[k] /= if (cumulative) total else total * w }
        return edges to counts
    }

    private const val SAMPLES = 400

    /** Kinds whose first input is several things split by commas. */
    private val MULTI = setOf(Kind.Box, Kind.Violin, Kind.Events, Kind.Stack)

    /**
     * Works out every layer of [a]: values from [eval], ranges fitted to what's drawn (unless
     * set), functions sampled across the x range. [palette] is the color cycle.
     */
    /**
     * A layer's inputs in the order the working-out reads them. People type x first, then y, then
     * the errors (x error, y error), and a field's x and y columns before its z (or u, v); a line
     * with only its first input filled takes it as y (a function of x).
     */
    private fun internal(l: Layer): Layer {
        fun i(k: Int) = l.input(k)
        val ins = when (l.kind) {
            Kind.Plot, Kind.Step, Kind.Stem -> {
                val (x, y) = if (i(1).items.isEmpty() && i(0).items.isNotEmpty()) MathRow() to i(0) else i(0) to i(1)
                listOf(y, x, i(3), i(2))
            }
            Kind.Scatter -> listOf(i(0), i(1), MathRow(), i(3), i(2))
            Kind.FillBetween -> listOf(i(1), i(2), i(0))
            // Only the first input filled: it's what's drawn (the curves, the field, the vector), not x.
            Kind.Stack -> if (i(1).items.isEmpty()) listOf(i(0), MathRow()) else listOf(i(1), i(0))
            Kind.Contour, Kind.ContourF, Kind.Heatmap -> if (i(2).items.isEmpty() && i(1).items.isEmpty()) listOf(i(0), MathRow(), MathRow()) else listOf(i(2), i(0), i(1))
            Kind.Quiver, Kind.Stream -> if (i(2).items.isEmpty() && i(3).items.isEmpty()) listOf(i(0), i(1), MathRow(), MathRow()) else listOf(i(2), i(3), i(0), i(1))
            else -> return l
        }
        return l.copy(inputs = ins)
    }

    /** Where the errors are in [internal]'s order. */
    private val INTERNAL_ERRORS = mapOf(Kind.Plot to (2 to 3), Kind.Scatter to (3 to 4))

    /** Inputs (in [internal]'s order) that are functions of the plotting letters rather than columns. */
    private val FUNCTION_INPUTS = mapOf(Kind.Contour to setOf(0), Kind.ContourF to setOf(0), Kind.Heatmap to setOf(0), Kind.Quiver to setOf(0, 1), Kind.Stream to setOf(0, 1), Kind.Parametric to setOf(0, 1), Kind.Polar to setOf(0))

    /**
     * Columns x, y, z as a grid: as they are when they make one (every x with every y), else
     * averaged into cells, with empty cells filled from their neighbors.
     */
    fun gridOf(x: DoubleArray, y: DoubleArray, z: DoubleArray): Grid? {
        val ks = (0 until minOf(x.size, y.size, z.size)).filter { x[it].isFinite() && y[it].isFinite() && z[it].isFinite() }
        if (ks.size < 4) return null
        fun key(v: Double) = (v * 1e9).roundToLong()
        val ux = ks.map { key(x[it]) }.distinct().sorted(); val uy = ks.map { key(y[it]) }.distinct().sorted()
        if (ux.size >= 2 && uy.size >= 2 && ux.size * uy.size == ks.size) {
            val ix = ux.withIndex().associate { it.value to it.index }; val iy = uy.withIndex().associate { it.value to it.index }
            val z2 = Array(uy.size) { DoubleArray(ux.size) { Double.NaN } }
            ks.forEach { z2[iy.getValue(key(y[it]))][ix.getValue(key(x[it]))] = z[it] }
            // Evenly spaced columns and rows are drawn as they are; uneven ones are close enough for a picture.
            return Grid(ux.first() / 1e9, ux.last() / 1e9, uy.first() / 1e9, uy.last() / 1e9, ux.size, uy.size, z2)
        }
        val x0 = ks.minOf { x[it] }; val x1 = ks.maxOf { x[it] }; val y0 = ks.minOf { y[it] }; val y1 = ks.maxOf { y[it] }
        if (x1 <= x0 || y1 <= y0) return null
        val n = sqrt(ks.size.toDouble()).roundToInt().coerceIn(6, 60)
        val sum = Array(n) { DoubleArray(n) }; val cnt = Array(n) { IntArray(n) }
        ks.forEach { k ->
            val i = ((x[k] - x0) / (x1 - x0) * (n - 1)).roundToInt().coerceIn(0, n - 1); val j = ((y[k] - y0) / (y1 - y0) * (n - 1)).roundToInt().coerceIn(0, n - 1)
            sum[j][i] += z[k]; cnt[j][i]++
        }
        var g = Array(n) { j -> DoubleArray(n) { i -> if (cnt[j][i] > 0) sum[j][i] / cnt[j][i] else Double.NaN } }
        repeat(n) {
            var gaps = false
            g = Array(n) { j -> DoubleArray(n) { i ->
                if (g[j][i].isFinite()) g[j][i] else {
                    val near = listOf(j - 1 to i, j + 1 to i, j to i - 1, j to i + 1).mapNotNull { (a, b) -> g.getOrNull(a)?.getOrNull(b)?.takeIf { it.isFinite() } }
                    if (near.isEmpty()) { gaps = true; Double.NaN } else near.average()
                }
            } }
            if (!gaps) return Grid(x0, x1, y0, y1, n, n, g)
        }
        return Grid(x0, x1, y0, y1, n, n, g)
    }

    fun work(a: Axes, eval: (MathRow) -> Expr, palette: List<Int>): Worked {
        // Worked out in their own order; the drawn layers handed back are the ones given.
        val given = a.layers.filter { it.visible }
        val layers = given.map { internal(it) }
        val original = HashMap<Layer, Layer>().apply { layers.indices.forEach { put(layers[it], given[it]) } }
        var auto = 0
        val colorOf = HashMap<Layer, Int>()
        for (l in layers) {
            val wantsColor = l.kind != Kind.Text && l.kind != Kind.Annotate && !l.kind.mapped
            val idx = if (l.style.color >= 0) l.style.color else if (wantsColor) auto++ else 0
            colorOf[l] = if (l.style.argb != 0) l.style.argb else palette[Math.floorMod(idx, palette.size)]
        }
        val ce = eval as? ColumnEval
        val values = layers.associateWith { l -> l.inputs.mapIndexed { k, row -> value(row, if (ce != null && k in FUNCTION_INPUTS[l.kind].orEmpty()) ce::asFunction else eval) } }
        val b = Bounds()
        val drawn = LinkedHashMap<Layer, Drawn>()
        var xCats: List<String>? = null; var yCats: List<String>? = null
        val bars = layers.filter { it.kind == Kind.Bar }; val barsH = layers.filter { it.kind == Kind.BarH }

        fun bad(l: Layer, msg: String) { drawn[l] = Drawn(l, colorOf.getValue(l), msg) }
        fun data(v: Value): DoubleArray? = when (v) { is Value.Data -> v.v; is Value.Num -> doubleArrayOf(v.v); is Value.Matrix -> v.v; else -> null }
        fun firstBad(vs: List<Value>) = vs.firstOrNull { it is Value.Bad } as Value.Bad?
        fun labels(l: Layer) = l.text.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        fun DoubleArray.at(k: Int) = if (isEmpty()) 0.0 else this[k % size].let { if (it.isFinite()) it else 0.0 }
        /** A layer's ± errors in y and x for its [n] points, from its error inputs (a number or a list; empty: none). */
        fun errors(l: Layer, vs: List<Value>, n: Int): Pair<DoubleArray, DoubleArray> {
            val (ky, kx) = INTERNAL_ERRORS[l.kind] ?: return DoubleArray(0) to DoubleArray(0)
            fun e(k: Int) = data(vs.getOrElse(k) { Value.Empty })?.let { s -> if (s.isEmpty()) DoubleArray(0) else DoubleArray(n) { abs(s[it % s.size]) } } ?: DoubleArray(0)
            return e(ky) to e(kx)
        }

        fun sampleT(v: Value, t: DoubleArray): DoubleArray? = when (v) {
            is Value.Fn -> DoubleArray(t.size) { v.f(t[it]) }
            is Value.Num -> DoubleArray(t.size) { v.v }
            else -> null
        }

        // Data first: what it covers sets the range functions are drawn over.
        for (l in layers) {
            val vs = values.getValue(l)
            val wrong = if (l.kind in MULTI) firstBad(vs.drop(1)) else firstBad(vs)
            if (wrong != null) { bad(l, wrong.message); continue }
            val color = colorOf.getValue(l)
            fun v(k: Int) = vs.getOrElse(k) { Value.Empty }
            when (l.kind) {
                Kind.Plot, Kind.Step, Kind.Stem -> {
                    val y = data(v(0)) ?: continue
                    if (v(0) is Value.Num && v(1) !is Value.Data) continue // a constant: drawn as a function below
                    // No x given: the table's x column when it fits, else 0, 1, 2, …
                    val x = data(v(1)) ?: (value(MathRow(mutableListOf(Sym("x"))), eval) as? Value.Data)?.v?.takeIf { it.size == y.size } ?: seq(y.size)
                    val n = min(x.size, y.size)
                    val xs = x.copyOf(n); val ys = y.copyOf(n)
                    val (ey, ex) = errors(l, vs, n)
                    xs.indices.forEach { k -> b.x(xs[k] - ex.at(k)); b.x(xs[k] + ex.at(k)); b.y(ys[k] - ey.at(k)); b.y(ys[k] + ey.at(k)) }
                    drawn[l] = Drawn(l, color, series = listOf(xs to ys), errY = ey, errX = ex)
                }
                Kind.Scatter, Kind.Fill -> {
                    val x = data(v(0)); val y = data(v(1))
                    if (x == null || y == null) { bad(l, "Give x and y as lists (or table columns)"); continue }
                    val n = min(x.size, y.size)
                    val xs = x.copyOf(n); val ys = y.copyOf(n)
                    val (ey, ex) = errors(l, vs, n)
                    xs.indices.forEach { k -> b.x(xs[k] - ex.at(k)); b.x(xs[k] + ex.at(k)); b.y(ys[k] - ey.at(k)); b.y(ys[k] + ey.at(k)) }
                    val sizes = if (l.kind == Kind.Scatter) data(v(2))?.let { s -> DoubleArray(n) { s[it % s.size] } } ?: DoubleArray(0) else DoubleArray(0)
                    drawn[l] = Drawn(l, color, series = listOf(xs to ys), shade = sizes, errY = ey, errX = ex)
                }
                Kind.FillBetween -> {
                    // Columns (or lists): against the x input, else the table's x column, else 1, 2, 3, …
                    val d1 = v(0) as? Value.Data; val d2 = v(1) as? Value.Data
                    if (d1 == null && d2 == null && v(2) !is Value.Data) continue
                    val n = listOfNotNull(d1?.v?.size, d2?.v?.size, (v(2) as? Value.Data)?.v?.size).min()
                    val x = (v(2) as? Value.Data)?.v ?: (value(MathRow(mutableListOf(Sym("x"))), eval) as? Value.Data)?.v?.takeIf { it.size >= n } ?: seq(n)
                    fun col(w: Value): DoubleArray? = when (w) {
                        is Value.Data -> w.v
                        is Value.Num -> DoubleArray(n) { w.v }
                        is Value.Empty -> DoubleArray(n)
                        is Value.Fn -> DoubleArray(n) { k -> w.f(x[k]) }
                        else -> null
                    }
                    val y1 = col(v(0)); val y2 = col(v(1))
                    if (y1 == null || y2 == null) { bad(l, "Give two columns, lists, functions or numbers"); continue }
                    val m = minOf(n, x.size, y1.size, y2.size)
                    val xs = x.copyOf(m); val a1 = y1.copyOf(m); val a2 = y2.copyOf(m)
                    xs.forEach(b::x); a1.forEach(b::y); a2.forEach(b::y)
                    drawn[l] = Drawn(l, color, series = listOf(xs to a1), lower = listOf(a2))
                }
                Kind.HLine, Kind.VLine -> {
                    val d = (v(0) as? Value.Data)?.v ?: continue
                    if (l.kind == Kind.HLine) d.forEach(b::y) else d.forEach(b::x)
                    drawn[l] = Drawn(l, color, values = d.filter { it.isFinite() }.toDoubleArray())
                }
                Kind.Hist -> {
                    val d = data(v(0)) ?: run { bad(l, "Give the data as a list or a table column"); null } ?: continue
                    val (edges, counts) = histogram(d, l.style.bins, l.style.density, l.style.cumulative)
                    if (edges.isEmpty()) { bad(l, "No numbers to count"); continue }
                    b.x(edges.first()); b.x(edges.last()); b.y(0.0); counts.forEach(b::y)
                    drawn[l] = Drawn(l, color, series = listOf(edges to counts), raw = listOf(d))
                }
                Kind.Box, Kind.Violin -> {
                    val sets = parts(l.input(0)).map { value(it, eval) }.map { data(it) ?: DoubleArray(0) }.filter { it.isNotEmpty() }
                    if (sets.isEmpty()) { bad(l, "Give one or more lists, split by commas"); continue }
                    val stats = sets.mapNotNull { boxStats(it) }
                    sets.forEach { s -> s.forEach(b::y) }
                    b.x(0.5); b.x(sets.size + 0.5)
                    val names = labels(l)
                    xCats = List(sets.size) { names.getOrElse(it) { "${it + 1}" } }
                    val shapes = if (l.kind == Kind.Violin) sets.map { kde(it) } else emptyList()
                    shapes.forEach { (ys, _) -> ys.forEach(b::y) }
                    drawn[l] = Drawn(l, color, boxes = stats, shapes = shapes, raw = sets)
                }
                Kind.Fit -> {
                    val x = data(v(0)); val y = data(v(1))
                    if (x == null || y == null) { bad(l, "Give x and y as lists (or table columns)"); continue }
                    for (k in 0 until min(x.size, y.size)) { b.x(x[k]); b.y(y[k]) }
                }
                Kind.Kde -> {
                    val d = data(v(0)) ?: run { bad(l, "Give the data as a list or a table column"); null } ?: continue
                    val (ys, dens) = kde(d)
                    if (ys.isEmpty()) continue
                    ys.forEach(b::x); b.y(0.0); dens.forEach(b::y)
                    drawn[l] = Drawn(l, color, series = listOf(ys to dens), raw = listOf(d))
                }
                Kind.Ecdf -> {
                    val d = data(v(0))?.let { finite(it).sorted().toDoubleArray() } ?: run { bad(l, "Give the data as a list or a table column"); null } ?: continue
                    if (d.isEmpty()) continue
                    val ys = DoubleArray(d.size) { (it + 1.0) / d.size }
                    d.forEach(b::x); b.y(0.0); b.y(1.0)
                    drawn[l] = Drawn(l, color, series = listOf(d to ys), raw = listOf(d))
                }
                Kind.Hist2D, Kind.Hexbin -> {
                    val x = data(v(0)); val y = data(v(1))
                    if (x == null || y == null) { bad(l, "Give x and y as lists (or table columns)"); continue }
                    val n = min(x.size, y.size)
                    val pairs = (0 until n).filter { x[it].isFinite() && y[it].isFinite() }
                    if (pairs.isEmpty()) continue
                    pairs.forEach { b.x(x[it]); b.y(y[it]) }
                    drawn[l] = Drawn(l, color, raw = listOf(DoubleArray(pairs.size) { x[pairs[it]] }, DoubleArray(pairs.size) { y[pairs[it]] }))
                }
                Kind.Bar, Kind.BarH -> {
                    val h = data(v(0)) ?: run { bad(l, "Give the heights as a list or a table column"); null } ?: continue
                    val group = if (l.kind == Kind.Bar) bars else barsH
                    val k = group.indexOf(l); val m = group.size
                    val width = 0.8 / m
                    val off = -0.4 + width * (k + 0.5)
                    val names = labels(l)
                    val cats = List(h.size) { names.getOrElse(it) { "${it + 1}" } }
                    if (l.kind == Kind.Bar) { b.x(-0.5); b.x(h.size - 0.5); b.y(0.0); h.forEach(b::y); if (xCats == null || names.isNotEmpty()) xCats = cats }
                    else { b.y(-0.5); b.y(h.size - 0.5); b.x(0.0); h.forEach(b::x); if (yCats == null || names.isNotEmpty()) yCats = cats }
                    drawn[l] = Drawn(l, color, values = h, labels = cats, offset = off, barWidth = width)
                }
                Kind.Pie -> {
                    val p = data(v(0))?.map { max(it, 0.0) }?.toDoubleArray() ?: run { bad(l, "Give the values as a list or a table column"); null } ?: continue
                    if (p.sum() <= 0) { bad(l, "The values add up to nothing"); continue }
                    drawn[l] = Drawn(l, color, values = p, labels = labels(l))
                }
                Kind.Events -> {
                    val sets = parts(l.input(0)).map { data(value(it, eval)) ?: DoubleArray(0) }.filter { it.isNotEmpty() }
                    if (sets.isEmpty()) { bad(l, "Give one or more lists of positions"); continue }
                    sets.forEach { s -> s.forEach(b::x) }; b.y(0.5); b.y(sets.size + 0.5)
                    drawn[l] = Drawn(l, color, raw = sets)
                }
                Kind.Heatmap, Kind.Contour, Kind.ContourF -> {
                    val m = v(0)
                    val xd = (v(1) as? Value.Data)?.v; val yd = (v(2) as? Value.Data)?.v
                    if (m is Value.Matrix) {
                        val z = Array(m.rows) { r -> DoubleArray(m.cols) { cc -> m.v[(m.rows - 1 - r) * m.cols + cc] } }
                        b.x(-0.5); b.x(m.cols - 0.5); b.y(-0.5); b.y(m.rows - 0.5)
                        drawn[l] = Drawn(l, color, grid = Grid(0.0, (m.cols - 1).toDouble(), 0.0, (m.rows - 1).toDouble(), m.cols, m.rows, z), offset = if (l.kind == Kind.Heatmap) 1.0 else 0.0)
                    } else if (xd != null && yd != null) {
                        // Columns x, y and z (or f(x, y) at each point): made into a grid.
                        val n = min(xd.size, yd.size)
                        val zd = when (m) {
                            is Value.Data -> m.v
                            is Value.Fn2 -> DoubleArray(n) { m.f(xd[it], yd[it]) }
                            is Value.Fn -> DoubleArray(n) { if (m.letter == "y") m.f(yd[it]) else m.f(xd[it]) }
                            is Value.Num -> DoubleArray(n) { m.v }
                            else -> null
                        }
                        val g = zd?.let { gridOf(xd, yd, it) } ?: run { bad(l, "Give z as a column (or f(x, y)) with columns x and y: at least 4 points"); null } ?: continue
                        b.x(g.x0); b.x(g.x1); b.y(g.y0); b.y(g.y1)
                        drawn[l] = Drawn(l, color, grid = g)
                    } else if (m is Value.Data) bad(l, "A column for z needs columns for x and y too")
                }
                Kind.Quiver, Kind.Stream -> {
                    val xd = (v(2) as? Value.Data)?.v; val yd = (v(3) as? Value.Data)?.v
                    if (xd == null || yd == null) continue
                    val n = min(xd.size, yd.size)
                    fun at(w: Value): DoubleArray? = when (w) {
                        is Value.Data -> w.v
                        is Value.Fn2 -> DoubleArray(n) { w.f(xd[it], yd[it]) }
                        is Value.Fn -> DoubleArray(n) { if (w.letter == "y") w.f(yd[it]) else w.f(xd[it]) }
                        is Value.Num -> DoubleArray(n) { w.v }
                        else -> null
                    }
                    val ud = at(v(0)); val vd = at(v(1))
                    if (ud == null || vd == null) { bad(l, "Give u and v: columns, numbers or functions of x and y"); continue }
                    val m2 = minOf(n, ud.size, vd.size)
                    xd.copyOf(m2).forEach(b::x); yd.copyOf(m2).forEach(b::y)
                    if (l.kind == Kind.Quiver) drawn[l] = Drawn(l, color, raw = listOf(xd.copyOf(m2), yd.copyOf(m2), ud.copyOf(m2), vd.copyOf(m2)))
                    else {
                        val gu = gridOf(xd, yd, ud); val gv = gridOf(xd, yd, vd)
                        if (gu == null || gv == null || gu.nx != gv.nx || gu.ny != gv.ny) { bad(l, "Not enough points for stream lines"); continue }
                        drawn[l] = Drawn(l, color, grid = gu, grid2 = gv, series = streamlines(gu, gv))
                    }
                }
                Kind.Parametric, Kind.Polar -> {
                    val polar = l.kind == Kind.Polar
                    val lim = if (polar) 1 else 2
                    val t0 = (v(lim) as? Value.Num)?.v ?: 0.0; val t1 = (v(lim + 1) as? Value.Num)?.v ?: (2 * Math.PI)
                    val ts = DoubleArray(SAMPLES * 2 + 1) { t0 + (t1 - t0) * it / (SAMPLES * 2) }
                    val px: DoubleArray; val py: DoubleArray
                    if (polar) {
                        val r = sampleT(v(0), ts) ?: run { bad(l, "Give r as a function of θ"); null } ?: continue
                        px = DoubleArray(ts.size) { r[it] * cos(ts[it]) }; py = DoubleArray(ts.size) { r[it] * sin(ts[it]) }
                    } else {
                        px = sampleT(v(0), ts) ?: run { bad(l, "Give x(t) and y(t)"); null } ?: continue
                        py = sampleT(v(1), ts) ?: run { bad(l, "Give x(t) and y(t)"); null } ?: continue
                    }
                    px.forEach(b::x); py.forEach(b::y)
                    drawn[l] = Drawn(l, color, series = listOf(px to py))
                }
                Kind.HSpan -> { listOf(v(0), v(1)).mapNotNull { (it as? Value.Num)?.v }.forEach(b::y) }
                Kind.VSpan -> { listOf(v(0), v(1)).mapNotNull { (it as? Value.Num)?.v }.forEach(b::x) }
                Kind.Text, Kind.Annotate -> {
                    val pts = vs.mapNotNull { (it as? Value.Num)?.v }
                    if (pts.size >= 2) { b.x(pts[0]); b.y(pts[1]) }
                    if (pts.size >= 4) { b.x(pts[2]); b.y(pts[3]) }
                }
                Kind.Stack -> {
                    val sets = parts(l.input(0)).map { value(it, eval) }
                    if (sets.all { it is Value.Data }) {
                        val ys = sets.map { (it as Value.Data).v }
                        val n = ys.minOf { it.size }
                        val x = data(v(1)) ?: seq(n)
                        val m = min(n, x.size)
                        val cum = ArrayList<DoubleArray>(); var acc = DoubleArray(m)
                        for (y in ys) { acc = DoubleArray(m) { acc[it] + (y[it].takeIf { v -> v.isFinite() } ?: 0.0) }; cum += acc }
                        x.copyOf(m).forEach(b::x); b.y(0.0); cum.forEach { c -> c.forEach(b::y) }
                        drawn[l] = Drawn(l, color, series = cum.map { x.copyOf(m) to it })
                    }
                }
                else -> Unit
            }
        }

        // The x range: as set, else the data's, else where the first function shows.
        val fn0 = layers.firstNotNullOfOrNull { l -> if (l in drawn) null else values.getValue(l).firstOrNull { it is Value.Fn } as Value.Fn? }
        val hasFields = layers.any { l -> l !in drawn && values.getValue(l).any { it is Value.Fn2 } }
        var xLo: Double; var xHi: Double
        if (b.hasX) {
            val pad = if (xCats != null || layers.any { it.kind == Kind.Hist || it.kind == Kind.Heatmap }) 0.0 else (b.x1 - b.x0) * 0.05
            xLo = b.x0 - pad; xHi = b.x1 + pad
            if (xHi - xLo < 1e-12) { xLo -= 1; xHi += 1 }
        } else if (fn0 != null) {
            val r = Sketch.range(fn0.f)
            xLo = r?.first ?: -5.0; xHi = r?.second ?: 5.0
        } else if (hasFields) { xLo = -3.0; xHi = 3.0 } else { xLo = -5.0; xHi = 5.0 }
        a.xMin?.let { xLo = it }; a.xMax?.let { xHi = it }
        if (a.xLog && xLo <= 0) xLo = if (xHi > 0) xHi / 1000 else 0.1
        if (xHi <= xLo) xHi = xLo + 1

        fun xs(): DoubleArray = DoubleArray(SAMPLES + 1) {
            if (a.xLog) 10.0.pow(log10(xLo) + (log10(xHi) - log10(xLo)) * it / SAMPLES) else xLo + (xHi - xLo) * it / SAMPLES
        }
        fun sample(v: Value, x: DoubleArray): DoubleArray? = when (v) {
            is Value.Fn -> DoubleArray(x.size) { v.f(x[it]) }
            is Value.Num -> DoubleArray(x.size) { v.v }
            is Value.Empty -> DoubleArray(x.size)
            else -> null
        }
        val sampled = Bounds()

        // Then the functions, over that range.
        for (l in layers) {
            if (l in drawn) continue
            val vs = values.getValue(l)
            val color = colorOf.getValue(l)
            fun v(k: Int) = vs.getOrElse(k) { Value.Empty }
            when (l.kind) {
                Kind.Plot, Kind.Step, Kind.Stem -> {
                    val x = if (l.kind == Kind.Stem) DoubleArray(21) { xLo + (xHi - xLo) * it / 20 } else xs()
                    val y = sample(v(0), x) ?: run { bad(l, "Give a function of one letter, or data"); null } ?: continue
                    if (v(0) is Value.Empty) continue
                    y.forEach(sampled::y)
                    drawn[l] = Drawn(l, color, series = listOf(x to y))
                }
                Kind.FillBetween -> {
                    val x = xs()
                    val y1 = sample(v(0), x); val y2 = sample(v(1), x)
                    if (y1 == null || y2 == null || v(0) is Value.Empty) { bad(l, "Give two functions, numbers or columns"); continue }
                    y1.forEach(sampled::y); y2.forEach(sampled::y)
                    drawn[l] = Drawn(l, color, series = listOf(x to y1), lower = listOf(y2))
                }
                Kind.Stack -> {
                    val x = xs()
                    val sets = parts(l.input(0)).map { value(it, eval) }
                    val ys = sets.map { sample(it, x) ?: run { bad(l, "Give functions, or lists of the same length"); null } ?: return@map null }
                    if (ys.any { it == null } || ys.isEmpty()) continue
                    val cum = ArrayList<DoubleArray>(); var acc = DoubleArray(x.size)
                    for (y in ys) { acc = DoubleArray(x.size) { acc[it] + (y!![it].takeIf { v -> v.isFinite() } ?: 0.0) }; cum += acc }
                    sampled.y(0.0); cum.forEach { c -> c.forEach(sampled::y) }
                    drawn[l] = Drawn(l, color, series = cum.map { x to it })
                }
                Kind.Fit -> {
                    val x0 = (v(0) as? Value.Data)?.v; val y0 = (v(1) as? Value.Data)?.v
                    if (x0 == null || y0 == null) { bad(l, "Give x and y as lists (or table columns)"); continue }
                    val ks = (0 until min(x0.size, y0.size)).filter { x0[it].isFinite() && y0[it].isFinite() }
                    val px = DoubleArray(ks.size) { x0[ks[it]] }; val py = DoubleArray(ks.size) { y0[ks[it]] }
                    val custom = if (l.style.model >= CUSTOM_FIT) customModel(l.style.formula) ?: run { bad(l, "Write the model in x and its parameters, like a*e^(-b*x) + c"); null } ?: continue else null
                    val model = FitModel.entries.getOrElse(l.style.model) { FitModel.Linear }
                    val fn: (Double, DoubleArray) -> Double = custom?.f ?: model::value
                    val count = custom?.params?.size ?: model.params.size
                    // Its own model starts from a few guesses; the best fit is kept.
                    val r = if (px.size <= count) null else if (custom == null) Fit.leastSquares(fn, px, py, model.start(px, py))
                        else listOf(1.0, 0.5, -1.0, 0.1, 2.0).mapNotNull { g -> runCatching { Fit.leastSquares(fn, px, py, DoubleArray(count) { g }) }.getOrNull() }
                            .filter { it.parameters.all { p -> p.isFinite() } }.maxByOrNull { if (it.rSquared.isFinite()) it.rSquared else -1e300 }
                    if (r == null) { bad(l, "Couldn't fit this model: more points, or another model"); continue }
                    val x = xs()
                    val y = DoubleArray(x.size) { fn(x[it], r.parameters) }
                    y.filter { it.isFinite() }.let { f -> if (f.isNotEmpty()) { sampled.y(f.min()); sampled.y(f.max()) } }
                    // The legend: the model as written (its parameters' numbers are listed in the layer) and R².
                    val eq = if (custom != null) "y = " + com.example.cas.engine.Latex.of(custom.row) else model.latex
                    val r2 = if (r.rSquared.isFinite()) ",\\; R^2 = " + String.format(java.util.Locale.ROOT, "%.4f", r.rSquared) else ""
                    drawn[l] = Drawn(l, color, series = listOf(x to y), raw = listOf(px, py), labels = listOf("$" + eq + r2 + "$"), values = r.parameters)
                }
                Kind.HLine -> (v(0) as? Value.Num)?.let { sampled.y(it.v); drawn[l] = Drawn(l, color, values = doubleArrayOf(it.v)) } ?: bad(l, "Give a number or a list")
                Kind.VLine -> (v(0) as? Value.Num)?.let { drawn[l] = Drawn(l, color, values = doubleArrayOf(it.v)) } ?: bad(l, "Give a number or a list")
                Kind.HSpan, Kind.VSpan -> {
                    val lo = (v(0) as? Value.Num)?.v; val hi = (v(1) as? Value.Num)?.v
                    if (lo == null || hi == null) { bad(l, "Give two numbers"); continue }
                    drawn[l] = Drawn(l, color, values = doubleArrayOf(min(lo, hi), max(lo, hi)))
                }
                Kind.Text, Kind.Annotate -> {
                    val pts = vs.map { (it as? Value.Num)?.v }
                    val need = if (l.kind == Kind.Text) 2 else 4
                    if (pts.size < need || pts.take(need).any { it == null }) { bad(l, if (need == 2) "Give x and y as numbers" else "Give the point and the note's place as numbers"); continue }
                    drawn[l] = Drawn(l, color, values = pts.take(need).map { it!! }.toDoubleArray(), labels = listOf(l.text))
                }
                else -> Unit
            }
        }

        // The y range: as set, else everything drawn (curves without their wildest 2 %).
        var yLo: Double; var yHi: Double
        val curveYs = drawn.values.filter { it.layer.kind in setOf(Kind.Plot, Kind.Step, Kind.Stem, Kind.FillBetween) && it.layer !in layers.filter { l -> values.getValue(l).firstOrNull() is Value.Data } }
            .flatMap { d -> d.series.flatMap { it.second.toList() } + d.lower.flatMap { it.toList() } }.filter { it.isFinite() }.sorted()
        val yb = Bounds()
        if (b.hasY) { yb.y(b.y0); yb.y(b.y1) }
        if (curveYs.isNotEmpty()) { yb.y(curveYs[(curveYs.size * 0.02).toInt()]); yb.y(curveYs[min((curveYs.size * 0.98).toInt(), curveYs.lastIndex)]) }
        drawn.values.filter { it.layer.kind in setOf(Kind.Parametric, Kind.Polar, Kind.Stack, Kind.HLine) }.forEach { d -> d.series.forEach { s -> s.second.forEach(yb::y) }; if (d.layer.kind == Kind.HLine) d.values.forEach(yb::y) }
        if (yb.hasY) {
            val zeroBased = layers.any { it.kind in setOf(Kind.Bar, Kind.Hist, Kind.Stack) }
            val pad = if (yCats != null || layers.any { it.kind == Kind.Heatmap }) 0.0 else (yb.y1 - yb.y0) * 0.06
            yLo = yb.y0 - if (zeroBased && yb.y0 >= 0) 0.0 else pad; yHi = yb.y1 + pad
            if (yHi - yLo < 1e-12) { yLo -= 1; yHi += 1 }
        } else if (hasFields) { yLo = xLo; yHi = xHi } else { yLo = -1.0; yHi = 1.0 }
        if (hasFields && !b.hasY && a.yMin == null && a.yMax == null) { yLo = xLo; yHi = xHi }
        a.yMin?.let { yLo = it }; a.yMax?.let { yHi = it }
        if (a.yLog && yLo <= 0) yLo = if (yHi > 0) yHi / 1000 else 0.1
        if (yHi <= yLo) yHi = yLo + 1

        // Fields last: they fill the whole view.
        for (l in layers) {
            if (l in drawn) continue
            val vs = values.getValue(l)
            val color = colorOf.getValue(l)
            fun v(k: Int) = vs.getOrElse(k) { Value.Empty }
            fun field(f: Value, n: Int): Grid? {
                val g = f as? Value.Fn2 ?: (f as? Value.Fn)?.let { one -> Value.Fn2({ x, y -> if (one.letter == "y") one.f(y) else one.f(x) }, listOf("x", "y")) } ?: (f as? Value.Num)?.let { k -> Value.Fn2({ _, _ -> k.v }, listOf("x", "y")) } ?: return null
                val ny = max(8, (n * 0.75).roundToInt())
                return Grid(xLo, xHi, yLo, yHi, n, ny, Array(ny) { j -> DoubleArray(n) { i -> g.f(xLo + (xHi - xLo) * i / (n - 1), yLo + (yHi - yLo) * j / (ny - 1)) } })
            }
            when (l.kind) {
                Kind.Contour, Kind.ContourF, Kind.Heatmap -> {
                    val g = field(v(0), if (l.kind == Kind.Heatmap) 140 else 90) ?: run { bad(l, "Give a function of x and y"); null } ?: continue
                    drawn[l] = Drawn(l, color, grid = g)
                }
                Kind.Quiver, Kind.Stream -> {
                    val n = if (l.kind == Kind.Quiver) l.style.arrows.coerceIn(4, 60) else 60
                    val u = field(v(0), n); val w = field(v(1), n)
                    if (u == null || w == null) { bad(l, "Give u and v as functions of x and y"); continue }
                    val lines = if (l.kind == Kind.Stream) streamlines(u, w) else emptyList()
                    drawn[l] = Drawn(l, color, grid = u, grid2 = w, series = lines)
                }
                else -> Unit
            }
        }

        // The colorbar's range: from the first mapped layer.
        val mapped = layers.firstNotNullOfOrNull { l -> drawn[l]?.takeIf { it.error == null && l.kind.mapped && l.kind != Kind.Contour } }
        var zMin = 0.0; var zMax = 1.0
        mapped?.let { d ->
            val z = d.grid?.finite ?: emptyList()
            if (z.isNotEmpty()) { zMin = z.min(); zMax = z.max() }
            if (d.layer.kind == Kind.Hist2D || d.layer.kind == Kind.Hexbin) {
                val counts = if (d.layer.kind == Kind.Hist2D) hist2d(d.raw[0], d.raw[1], xLo, xHi, yLo, yHi, binsOf(d.layer, 24)).finite else hexCounts(d.raw[0], d.raw[1], xLo, xHi, yLo, yHi, binsOf(d.layer, 18)).map { it.third }
                zMin = 0.0; zMax = counts.maxOrNull() ?: 1.0
            }
            if (zMax - zMin < 1e-12) zMax = zMin + 1
        }

        val pie = drawn.values.any { it.layer.kind == Kind.Pie } && drawn.values.all { it.layer.kind == Kind.Pie || it.error != null }
        return Worked(a, layers.mapNotNull { l -> drawn[l]?.withLayer(original.getValue(l)) }, xLo, xHi, yLo, yHi, xCats, yCats, pie, zMin, zMax, mapped?.let { it.withLayer(original.getValue(it.layer)) })
    }

    private fun binsOf(l: Layer, def: Int) = if (l.style.bins > 0) l.style.bins else def

    /** A text's size: [Style.size] 3 is the labels' own. */
    fun textSize(s: Style) = LABEL * s.size / 3.0

    /** A Gaussian kernel density estimate (Scott's bandwidth) of [data]: the ys it's taken at, and the density there. */
    fun kde(data: DoubleArray): Pair<DoubleArray, DoubleArray> {
        val s = finite(data)
        if (s.isEmpty()) return DoubleArray(0) to DoubleArray(0)
        val mean = s.average()
        val sd = sqrt(s.sumOf { (it - mean) * (it - mean) } / max(1, s.size - 1)).takeIf { it > 0 } ?: 1.0
        val h = sd * s.size.toDouble().pow(-0.2)
        val lo = s.min() - 2 * h; val hi = s.max() + 2 * h
        val ys = DoubleArray(60) { lo + (hi - lo) * it / 59 }
        val d = DoubleArray(60) { k -> s.sumOf { v -> kotlin.math.exp(-0.5 * ((ys[k] - v) / h).pow(2)) } / (s.size * h * sqrt(2 * Math.PI)) }
        return ys to d
    }

    /** Counts of (x, y) pairs in an n × n grid of bins over the view. */
    fun hist2d(x: DoubleArray, y: DoubleArray, x0: Double, x1: Double, y0: Double, y1: Double, n: Int): Grid {
        val z = Array(n) { DoubleArray(n) }
        for (k in x.indices) {
            val i = ((x[k] - x0) / (x1 - x0) * n).toInt(); val j = ((y[k] - y0) / (y1 - y0) * n).toInt()
            if (i in 0 until n && j in 0 until n) z[j][i]++
            else if (x[k] == x1 && j in 0 until n) z[j][n - 1]++
        }
        return Grid(x0, x1, y0, y1, n, n, z)
    }

    /** Hexagon centers (in the view's fractions 0…1) and how many pairs fall in each. */
    fun hexCounts(x: DoubleArray, y: DoubleArray, x0: Double, x1: Double, y0: Double, y1: Double, n: Int): List<Triple<Double, Double, Double>> {
        val dx = 1.0 / n; val dy = dx * sqrt(3.0) / 2
        val counts = HashMap<Pair<Int, Int>, Int>()
        for (k in x.indices) {
            val u = (x[k] - x0) / (x1 - x0); val v = (y[k] - y0) / (y1 - y0)
            if (u < 0 || u > 1 || v < 0 || v > 1) continue
            // The nearest center of two offset lattices.
            val j = (v / dy).roundToInt()
            val i = ((u - if (j % 2 != 0) dx / 2 else 0.0) / dx).roundToInt()
            var best = i to j; var bestD = Double.MAX_VALUE
            for (jj in j - 1..j + 1) for (ii in i - 1..i + 1) {
                val cx = ii * dx + if (jj % 2 != 0) dx / 2 else 0.0; val cy = jj * dy
                val dd = (u - cx).pow(2) + (v - cy).pow(2)
                if (dd < bestD) { bestD = dd; best = ii to jj }
            }
            counts[best] = (counts[best] ?: 0) + 1
        }
        return counts.map { (k, c) -> Triple(k.first * dx + if (k.second % 2 != 0) dx / 2 else 0.0, k.second * dy, c.toDouble()) }
    }

    /** Stream lines of the field (u, v): paths traced both ways from a spread of starting points. */
    fun streamlines(u: Grid, v: Grid): List<Pair<DoubleArray, DoubleArray>> {
        fun at(g: Grid, x: Double, y: Double): Double {
            val fi = (x - g.x0) / (g.x1 - g.x0) * (g.nx - 1); val fj = (y - g.y0) / (g.y1 - g.y0) * (g.ny - 1)
            val i = floor(fi).toInt().coerceIn(0, g.nx - 2); val j = floor(fj).toInt().coerceIn(0, g.ny - 2)
            val a = fi - i; val c = fj - j
            return g.z[j][i] * (1 - a) * (1 - c) + g.z[j][i + 1] * a * (1 - c) + g.z[j + 1][i] * (1 - a) * c + g.z[j + 1][i + 1] * a * c
        }
        val w = u.x1 - u.x0; val h = u.y1 - u.y0
        val occupied = HashSet<Pair<Int, Int>>()
        val cells = 24
        fun cell(x: Double, y: Double) = ((x - u.x0) / w * cells).toInt() to ((y - u.y0) / h * cells).toInt()
        val out = ArrayList<Pair<DoubleArray, DoubleArray>>()
        val step = min(w, h) / 120
        for (sj in 0 until 9) for (si in 0 until 12) {
            val sx = u.x0 + w * (si + 0.5) / 12; val sy = u.y0 + h * (sj + 0.5) / 9
            if (cell(sx, sy) in occupied) continue
            val line = ArrayList<DoubleArray>()
            for (dir in listOf(-1.0, 1.0)) {
                var x = sx; var y = sy
                val half = ArrayList<DoubleArray>()
                val mine = HashSet<Pair<Int, Int>>()
                for (k in 0 until 400) {
                    val a = at(u, x, y); val c = at(v, x, y); val m = hypot(a, c)
                    if (!m.isFinite() || m < 1e-12) break
                    val mx = x + dir * step * a / m / 2; val my = y + dir * step * c / m / 2
                    val a2 = at(u, mx, my); val c2 = at(v, mx, my); val m2 = hypot(a2, c2)
                    if (!m2.isFinite() || m2 < 1e-12) break
                    x += dir * step * a2 / m2; y += dir * step * c2 / m2
                    if (x < u.x0 || x > u.x1 || y < u.y0 || y > u.y1) break
                    val ce = cell(x, y)
                    if (ce in occupied && ce !in mine) break
                    mine += ce
                    half += doubleArrayOf(x, y)
                }
                occupied += mine
                if (dir < 0) line.addAll(half.reversed()) else { line += doubleArrayOf(sx, sy); line.addAll(half) }
            }
            if (line.size > 6) out += DoubleArray(line.size) { line[it][0] } to DoubleArray(line.size) { line[it][1] }
        }
        return out
    }

    /** [src] drawn into [dst] with its top left at (x, y), [scale] times its size (a figure on a notebook's page). */
    fun embed(dst: Scene, src: Scene, x: Double, y: Double, scale: Double) {
        fun pts(p: DoubleArray) = DoubleArray(p.size) { k -> if (k % 2 == 0) x + p[k] * scale else y + p[k] * scale }
        dst.add(Scene.Fill(listOf(doubleArrayOf(x, y, x + src.width * scale, y, x + src.width * scale, y + src.height * scale, x, y + src.height * scale)), src.background))
        dst.add(Scene.ClipStart(x, y, src.width * scale, src.height * scale))
        for (it in src.items) dst.add(when (it) {
            is Scene.Stroke -> Scene.Stroke(it.paths.map(::pts), it.color, it.strokeWidth * scale, it.dash?.let { d -> DoubleArray(d.size) { k -> d[k] * scale } })
            is Scene.Fill -> Scene.Fill(it.polygons.map(::pts), it.color)
            is Scene.Circle -> Scene.Circle(x + it.cx * scale, y + it.cy * scale, it.r * scale, it.fill, it.stroke, it.strokeWidth * scale)
            is Scene.Label -> Scene.Label(x + it.x * scale, y + it.y * scale, it.text, it.size * scale, it.color, it.anchor, it.font, it.angle, it.italic, it.spans, it.baseline)
            is Scene.ClipStart -> Scene.ClipStart(x + it.x * scale, y + it.y * scale, it.w * scale, it.h * scale)
            Scene.ClipEnd -> Scene.ClipEnd
            is Scene.Image -> Scene.Image(x + it.x * scale, y + it.y * scale, it.w * scale, it.h * scale, it.pixels, it.pixelWidth, it.pixelHeight)
        })
        dst.add(Scene.ClipEnd)
    }

    // ---- Drawing ------------------------------------------------------------------------------

    /** The figure drawn: its scene, each axes worked out, and each layer's error. */
    class Built(val scene: Scene, val worked: List<Worked>) {
        fun error(axes: Int, layer: Layer): String? = worked.getOrNull(axes)?.drawn?.firstOrNull { it.layer === layer }?.error
    }

    private fun withAlpha(c: Int, a: Double) = (((c ushr 24) * a.coerceIn(0.0, 1.0)).roundToInt() shl 24) or (c and 0xFFFFFF)
    private fun mix(a: Int, b: Int, t: Double): Int {
        fun ch(s: Int) = (((a shr s) and 0xFF) * (1 - t) + ((b shr s) and 0xFF) * t).roundToInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    private const val TICK = 8.5
    private const val LABEL = 10.0
    private const val TITLE = 11.0

    private fun spans(text: String): List<Scene.Span> = runCatching { Legend.spans(Legend.row(text)) }.getOrElse { listOf(Scene.Span(text, false)) }
    private fun width(sp: List<Scene.Span>, size: Double) = sp.sumOf { MathScene.metrics.width(it.text, it.italic, size * it.scale) }

    private fun label(t: Theme, scene: Scene, x: Double, y: Double, text: String, size: Double, color: Int, anchor: Scene.Anchor = Scene.Anchor.Middle, angle: Double = 0.0) {
        if (text.isBlank()) return
        val sp = spans(text)
        // Every style sets its labels in the same LaTeX font (styles change colors, not type).
        scene.add(Scene.Label(x, y, sp.joinToString("") { it.text }, size, color, anchor, Scene.Font.Roman, angle, spans = sp))
    }

    /** Tick positions for a range: 1, 2 or 5 times a power of ten, about [count] of them. */
    fun ticks(lo: Double, hi: Double, count: Int = 6): DoubleArray {
        val span = hi - lo
        if (!span.isFinite() || span <= 0) return doubleArrayOf(lo)
        val raw = span / count
        val e = 10.0.pow(floor(log10(raw)))
        val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).first { it * e >= raw * 0.999 } * e
        val out = ArrayList<Double>()
        var t = ceil(lo / step - 1e-9) * step
        while (t <= hi + step * 1e-9) { out += if (abs(t) < step * 1e-9) 0.0 else t; t += step }
        return out.toDoubleArray()
    }

    /** A tick's label: short, with a true minus sign. */
    fun tickText(v: Double): String {
        if (v == 0.0) return "0"
        val a = abs(v)
        val s = if (a >= 1e5 || a < 1e-3) {
            val e = floor(log10(a)).toInt(); val m = v / 10.0.pow(e)
            (if (abs(m - m.roundToInt()) < 1e-9) m.roundToInt().toString() else String.format(java.util.Locale.ROOT, "%.2g", m)) + "e$e"
        } else java.math.BigDecimal(v).round(java.math.MathContext(6)).stripTrailingZeros().toPlainString()
        return s.replace("-", "−")
    }

    /**
     * The figure as a [Scene] [width] wide. Theme 0 takes [app] (the screen's colors); [eval]
     * works out each input.
     */
    fun build(d: Doc, eval: (MathRow) -> Expr, app: Theme, width: Double = 480.0): Built {
        val theme = if (d.theme == 0) app else THEMES.getOrElse(d.theme) { app }
        val height = width * d.aspect.coerceIn(0.3, 2.0)
        val scene = Scene(width, height, theme.background)
        val boxes = cellBoxes(d, width)
        val worked = ArrayList<Worked>()
        if (d.title.isNotBlank()) label(theme, scene, width / 2, 13.0, d.title, 13.0, theme.ink)
        boxes.forEachIndexed { k, box ->
            val a = d.axes.getOrNull(k) ?: Axes()
            val w = runCatching { work(a, eval, theme.palette) }.getOrElse { Worked(a, emptyList(), -5.0, 5.0, -1.0, 1.0, null, null, false, 0.0, 1.0, null) }
            worked += w
            drawAxes(scene, w, theme, box[0], box[1], box[2], box[3])
        }
        return Built(scene, worked)
    }

    /** Where the layout's columns and rows start and end, as shares (0 … 1) of the width and of the height under the title. */
    fun edges(d: Doc): Pair<DoubleArray, DoubleArray> {
        val (_, cols, rows) = cells(d.layout)
        fun of(r: List<Double>, n: Int): DoubleArray {
            val w = if (r.size == n && r.all { it > 0 }) r else List(n) { 1.0 }
            val sum = w.sum(); val out = DoubleArray(n + 1)
            for (k in 0 until n) out[k + 1] = out[k] + w[k] / sum
            out[n] = 1.0
            return out
        }
        return of(d.widths, cols) to of(d.heights, rows)
    }

    /** The space above the axes: room for the figure's title. */
    fun titleSpace(d: Doc) = if (d.title.isNotBlank()) 24.0 else 4.0

    /** Each axes' box in a figure [width] wide, as (x, y, width, height). */
    fun cellBoxes(d: Doc, width: Double = 480.0): List<DoubleArray> {
        val height = width * d.aspect.coerceIn(0.3, 2.0)
        val top = titleSpace(d)
        val (cells, _, _) = cells(d.layout)
        val (xe, ye) = edges(d)
        return cells.map { c ->
            val x0 = xe[c[0]] * width; val x1 = xe[c[0] + c[2]] * width
            val y0 = top + ye[c[1]] * (height - top); val y1 = top + ye[c[1] + c[3]] * (height - top)
            doubleArrayOf(x0, y0, x1 - x0, y1 - y0)
        }
    }

    /** One axes inside the box at (bx, by), bw × bh. */
    private fun drawAxes(scene: Scene, w: Worked, t: Theme, bx: Double, by: Double, bw: Double, bh: Double) {
        val a = w.axes
        val bar = w.mapped != null
        // Without numbers the ticks go too, and the axes take the room they had.
        val xTickRoom = if (!a.xNumbers) 6.0 else 22.0
        var left = bx + (if (a.yNumbers) 40 else 10) + if (a.yLabel.isNotBlank()) 13 else 0
        var right = bx + bw - 10 - if (bar) 46 + (if (w.mapped?.layer?.style?.cbar?.isNotBlank() == true) 14 else 0) else 0
        var top = by + 8 + if (a.title.isNotBlank()) 16 else 0
        var bottom = by + bh - xTickRoom - if (a.xLabel.isNotBlank()) 14 else 0
        if (w.yCategories != null) left += (w.yCategories.maxOfOrNull { it.length } ?: 0).coerceAtMost(12) * 3.5
        if (right - left < 30) { right = left + 30 }
        if (bottom - top < 30) { bottom = top + 30 }
        if (a.title.isNotBlank()) label(t, scene, (left + right) / 2, by + 12, a.title, TITLE, t.ink)
        if (w.pie) { drawPies(scene, w, t, left, top, right, bottom); return }

        // Equal scales: the frame shrinks along one side so a unit is as long on both.
        var x0 = w.x0; var x1 = w.x1; var y0 = w.y0; var y1 = w.y1
        if (a.equal && !a.xLog && !a.yLog) {
            val sx = (right - left) / (x1 - x0); val sy = (bottom - top) / (y1 - y0)
            if (sx > sy) { val c = (x0 + x1) / 2; val half = (right - left) / sy / 2; x0 = c - half; x1 = c + half }
            else { val c = (y0 + y1) / 2; val half = (bottom - top) / sx / 2; y0 = c - half; y1 = c + half }
        }
        w.frame = doubleArrayOf(left, top, right, bottom, x0, x1, y0, y1)
        val lx0 = if (a.xLog) log10(x0) else x0; val lx1 = if (a.xLog) log10(x1) else x1
        val ly0 = if (a.yLog) log10(y0) else y0; val ly1 = if (a.yLog) log10(y1) else y1
        fun px(x: Double): Double = ((if (a.xLog) (if (x > 0) log10(x) else Double.NaN) else x) - lx0) / (lx1 - lx0) * (right - left).let { d -> if (a.xInvert) -d else d } + if (a.xInvert) right else left
        fun py(y: Double): Double = ((if (a.yLog) (if (y > 0) log10(y) else Double.NaN) else y) - ly0) / (ly1 - ly0) * (bottom - top).let { d -> if (a.yInvert) d else -d } + if (a.yInvert) top else bottom

        scene.add(Scene.Fill(listOf(doubleArrayOf(left, top, right, top, right, bottom, left, bottom)), t.face))
        val xt = if (w.xCategories != null) DoubleArray(w.xCategories.size) { if (w.drawn.any { it.layer.kind == Kind.Bar }) it.toDouble() else it + 1.0 } else if (a.xLog) logTicks(x0, x1) else ticks(x0, x1, max(3, ((right - left) / 55).toInt()))
        val yt = if (w.yCategories != null) DoubleArray(w.yCategories.size) { it.toDouble() } else if (a.yLog) logTicks(y0, y1) else ticks(y0, y1, max(3, ((bottom - top) / 40).toInt()))
        fun grid() {
            if (a.grid >= 2) {
                val minor = ArrayList<DoubleArray>()
                if (!a.xLog && xt.size > 1 && w.xCategories == null) { val s = (xt[1] - xt[0]) / 5; var v = xt[0] - s * 5; while (v <= x1) { if (v >= x0) minor += doubleArrayOf(px(v), top, px(v), bottom); v += s } }
                if (!a.yLog && yt.size > 1 && w.yCategories == null) { val s = (yt[1] - yt[0]) / 5; var v = yt[0] - s * 5; while (v <= y1) { if (v >= y0) minor += doubleArrayOf(left, py(v), right, py(v)); v += s } }
                scene.add(Scene.Stroke(minor, withAlpha(t.grid, 0.55), 0.4))
            }
            if (a.grid >= 1) {
                val lines = xt.filter { it in x0..x1 }.map { doubleArrayOf(px(it), top, px(it), bottom) } + yt.filter { it in y0..y1 }.map { doubleArrayOf(left, py(it), right, py(it)) }
                scene.add(Scene.Stroke(lines, t.grid, 0.7))
            }
        }
        if (!t.gridOnTop) grid()

        scene.add(Scene.ClipStart(left, top, right - left, bottom - top))
        // The first layer in the list is on top: drawn last.
        for (dr in w.drawn.asReversed()) if (dr.error == null) runCatching { drawLayer(scene, dr, w, t, ::px, ::py, left, top, right, bottom, x0, x1, y0, y1) }
        scene.add(Scene.ClipEnd)

        // Spines, ticks and their labels.
        val ink = t.ink
        val zeroX = if (a.spines == 2 && 0.0 in y0..y1) py(0.0) else bottom
        val zeroY = if (a.spines == 2 && 0.0 in x0..x1) px(0.0) else left
        when (a.spines) {
            0 -> scene.add(Scene.Stroke(listOf(doubleArrayOf(left, top, right, top, right, bottom, left, bottom, left, top)), ink, 0.8))
            1 -> scene.add(Scene.Stroke(listOf(doubleArrayOf(left, top, left, bottom, right, bottom)), ink, 0.8))
            2 -> scene.add(Scene.Stroke(listOf(doubleArrayOf(left, zeroX, right, zeroX), doubleArrayOf(zeroY, top, zeroY, bottom)), ink, 0.8))
            else -> Unit
        }
        val tickMarks = ArrayList<DoubleArray>()
        xt.forEachIndexed { k, v ->
            if (v < x0 - 1e-9 || v > x1 + 1e-9) return@forEachIndexed
            val x = px(v)
            if (a.spines != 3 && a.xNumbers) tickMarks += doubleArrayOf(x, zeroX, x, zeroX + if (a.ticksIn) -3.5 else 3.5)
            val text = w.xCategories?.getOrNull(k) ?: if (a.xLog) "10^${log10(v).roundToInt()}".let { s -> "$" + s.replace("^", "^{") + "}$" } else tickText(v)
            if (a.xNumbers) label(t, scene, x, zeroX + 11, text, TICK, t.muted)
        }
        yt.forEachIndexed { k, v ->
            if (v < y0 - 1e-9 || v > y1 + 1e-9) return@forEachIndexed
            val y = py(v)
            if (a.spines != 3 && a.yNumbers) tickMarks += doubleArrayOf(zeroY + if (a.ticksIn) 3.5 else -3.5, y, zeroY, y)
            val text = w.yCategories?.getOrNull(k) ?: if (a.yLog) "$10^{${log10(v).roundToInt()}}$" else tickText(v)
            if (a.yNumbers) label(t, scene, zeroY - 6, y, text, TICK, t.muted, Scene.Anchor.End)
        }
        scene.add(Scene.Stroke(tickMarks, ink, 0.7))
        if (t.gridOnTop) grid()
        if (a.xLabel.isNotBlank()) label(t, scene, (left + right) / 2, bottom + xTickRoom + 8, a.xLabel, LABEL, ink)
        if (a.yLabel.isNotBlank()) label(t, scene, left - (if (a.yNumbers) 36 else 8) - (w.yCategories?.let { c -> (c.maxOfOrNull { it.length } ?: 0).coerceAtMost(12) * 3.5 } ?: 0.0), (top + bottom) / 2, a.yLabel, LABEL, ink, angle = 90.0)
        if (bar) drawColorbar(scene, w, t, right + 10, top, bottom)
        if (a.legend >= 0) drawLegend(scene, w, t, left, top, right, bottom)
    }

    private fun logTicks(lo: Double, hi: Double): DoubleArray {
        val a = floor(log10(lo)).toInt(); val b = ceil(log10(hi)).toInt()
        val step = max(1, (b - a) / 6)
        return (a..b step step).map { 10.0.pow(it) }.filter { it in lo * 0.999..hi * 1.001 }.toDoubleArray()
    }

    private fun cmap(name: String) = Colormap.parse(name)
    private fun cmapColor(name: String, t: Double) = cmap(name).let { (m, rev) -> (0xFF shl 24) or m.rgb((if (rev) 1 - t else t).coerceIn(0.0, 1.0)) }

    private fun drawLayer(scene: Scene, dr: Drawn, w: Worked, t: Theme, px: (Double) -> Double, py: (Double) -> Double,
                          left: Double, top: Double, right: Double, bottom: Double, x0: Double, x1: Double, y0: Double, y1: Double) {
        val l = dr.layer; val s = l.style
        val color = withAlpha(dr.color, s.alpha)
        val dash = LineStyle.of(s.line).pattern(s.width)
        fun polyline(xs: DoubleArray, ys: DoubleArray): List<DoubleArray> {
            val out = ArrayList<DoubleArray>(); val cur = ArrayList<Double>()
            for (k in xs.indices) {
                val X = px(xs[k]); val Y = py(ys[k])
                if (!X.isFinite() || !Y.isFinite() || abs(Y) > 1e5 || abs(X) > 1e5) { if (cur.size >= 4) out += cur.toDoubleArray(); cur.clear(); continue }
                cur += X; cur += Y
            }
            if (cur.size >= 4) out += cur.toDoubleArray()
            return out
        }
        fun markers(xs: DoubleArray, ys: DoubleArray, def: Int = -1, sizes: DoubleArray = DoubleArray(0)) {
            val m = if (s.marker >= 0) s.marker else def
            if (m < 0) return
            val mk = Marker.of(m)
            for (k in xs.indices) { val X = px(xs[k]); val Y = py(ys[k]); if (X.isFinite() && Y.isFinite()) mk.addTo(scene, X, Y, if (sizes.isNotEmpty()) sqrt(abs(sizes[k])).coerceIn(0.5, 30.0) else s.size, color) }
        }
        fun rect(xa: Double, ya: Double, xb: Double, yb: Double, c: Int) = scene.add(Scene.Fill(listOf(doubleArrayOf(xa, ya, xb, ya, xb, yb, xa, yb)), c))
        // ± errors under the points: bars with caps, or a shaded band.
        if (dr.errY.isNotEmpty() || dr.errX.isNotEmpty()) dr.series.firstOrNull()?.let { (xs, ys) ->
            val ew = max(0.4, s.errWidth); val cap = s.errCap
            val ec = withAlpha(dr.color, s.alpha * 0.9)
            fun e(a: DoubleArray, k: Int) = if (a.isEmpty()) 0.0 else a[k]
            if (s.errBand && l.kind == Kind.Scatter) {
                // Points: a shaded box round each, its width the x error and its height the y error (a marker wide when there's none).
                val half = s.size * 1.4
                val boxes = xs.indices.filter { xs[it].isFinite() && ys[it].isFinite() }.map { k ->
                    val ey = e(dr.errY, k); val ex = e(dr.errX, k)
                    val x0 = if (ex > 0) px(xs[k] - ex) else px(xs[k]) - half; val x1 = if (ex > 0) px(xs[k] + ex) else px(xs[k]) + half
                    val y0 = if (ey > 0) py(ys[k] - ey) else py(ys[k]) + half; val y1 = if (ey > 0) py(ys[k] + ey) else py(ys[k]) - half
                    doubleArrayOf(x0, y0, x1, y0, x1, y1, x0, y1)
                }
                boxes.forEach { scene.add(Scene.Fill(listOf(it), withAlpha(dr.color, s.alpha * 0.3))) }
            } else if (s.errBand) {
                // A line: y errors as a band along x, x errors as a band along y.
                val ok = xs.indices.filter { xs[it].isFinite() && ys[it].isFinite() }
                if (dr.errY.isNotEmpty()) ok.sortedBy { xs[it] }.takeIf { it.size >= 2 }?.let { ks ->
                    scene.add(Scene.Fill(listOf((ks.flatMap { listOf(px(xs[it]), py(ys[it] + e(dr.errY, it))) } + ks.reversed().flatMap { listOf(px(xs[it]), py(ys[it] - e(dr.errY, it))) }).toDoubleArray()), withAlpha(dr.color, s.alpha * 0.25)))
                }
                if (dr.errX.isNotEmpty()) ok.sortedBy { ys[it] }.takeIf { it.size >= 2 }?.let { ks ->
                    scene.add(Scene.Fill(listOf((ks.flatMap { listOf(px(xs[it] + e(dr.errX, it)), py(ys[it])) } + ks.reversed().flatMap { listOf(px(xs[it] - e(dr.errX, it)), py(ys[it])) }).toDoubleArray()), withAlpha(dr.color, s.alpha * 0.25)))
                }
            } else {
                val bars = ArrayList<DoubleArray>()
                for (k in xs.indices) {
                    if (!xs[k].isFinite() || !ys[k].isFinite()) continue
                    val X = px(xs[k]); val Y = py(ys[k])
                    val ey = e(dr.errY, k); val ex = e(dr.errX, k)
                    if (ey > 0) { val a = py(ys[k] - ey); val b = py(ys[k] + ey); bars += doubleArrayOf(X, a, X, b); if (cap > 0) { bars += doubleArrayOf(X - cap, a, X + cap, a); bars += doubleArrayOf(X - cap, b, X + cap, b) } }
                    if (ex > 0) { val a = px(xs[k] - ex); val b = px(xs[k] + ex); bars += doubleArrayOf(a, Y, b, Y); if (cap > 0) { bars += doubleArrayOf(a, Y - cap, a, Y + cap); bars += doubleArrayOf(b, Y - cap, b, Y + cap) } }
                }
                scene.add(Scene.Stroke(bars, ec, ew))
            }
        }
        when (l.kind) {
            Kind.Plot, Kind.Parametric, Kind.Polar -> dr.series.forEach { (xs, ys) ->
                scene.add(Scene.Stroke(polyline(xs, ys), color, s.width, dash))
                if (xs.size <= 200) markers(xs, ys)
            }
            Kind.Fit -> {
                // Only the curve: the points are their own layer (a scatter) when wanted.
                dr.series.forEach { (xs, ys) -> scene.add(Scene.Stroke(polyline(xs, ys), color, s.width, dash)) }
            }
            Kind.Kde -> dr.series.forEach { (xs, ys) ->
                val poly = polyline(xs, ys)
                scene.add(Scene.Fill(poly.map { p -> p + doubleArrayOf(p[p.size - 2], py(max(0.0, y0)), p[0], py(max(0.0, y0))) }, withAlpha(dr.color, s.alpha * 0.18)))
                scene.add(Scene.Stroke(poly, color, s.width, dash))
            }
            Kind.Step -> dr.series.forEach { (xs, ys) ->
                val sx = ArrayList<Double>(); val sy = ArrayList<Double>()
                for (k in xs.indices) { if (k > 0) { sx += xs[k]; sy += ys[k - 1] }; sx += xs[k]; sy += ys[k] }
                scene.add(Scene.Stroke(polyline(sx.toDoubleArray(), sy.toDoubleArray()), color, s.width, dash))
                if (xs.size <= 200) markers(xs, ys)
            }
            Kind.Stem -> dr.series.forEach { (xs, ys) ->
                val base = if (0.0 in y0..y1) 0.0 else y0
                scene.add(Scene.Stroke(xs.indices.filter { ys[it].isFinite() }.map { doubleArrayOf(px(xs[it]), py(base), px(xs[it]), py(ys[it])) }, color, s.width, dash))
                scene.add(Scene.Stroke(listOf(doubleArrayOf(left, py(base), right, py(base))), withAlpha(t.ink, 0.6), 0.8))
                markers(xs, ys, def = Marker.Circle.ordinal)
            }
            Kind.Scatter -> dr.series.forEach { (xs, ys) -> markers(xs, ys, def = Marker.Circle.ordinal, sizes = dr.shade) }
            Kind.Fill -> dr.series.forEach { (xs, ys) ->
                val poly = DoubleArray(xs.size * 2) { k -> if (k % 2 == 0) px(xs[k / 2]) else py(ys[k / 2]) }
                scene.add(Scene.Fill(listOf(poly), withAlpha(dr.color, s.alpha * 0.6)))
                scene.add(Scene.Stroke(listOf(poly + poly.copyOf(2)), color, s.width * 0.7))
            }
            Kind.FillBetween -> {
                val (xs, a) = dr.series.single(); val b = dr.lower.single()
                val polys = ArrayList<DoubleArray>(); var run = ArrayList<Int>()
                fun flush() {
                    if (run.size >= 2) polys += (run.flatMap { listOf(px(xs[it]), py(a[it])) } + run.reversed().flatMap { listOf(px(xs[it]), py(b[it])) }).toDoubleArray()
                    run = ArrayList()
                }
                for (k in xs.indices) if (a[k].isFinite() && b[k].isFinite()) run += k else flush()
                flush()
                scene.add(Scene.Fill(polys, withAlpha(dr.color, s.alpha * 0.35)))
                scene.add(Scene.Stroke(polyline(xs, a), color, s.width * 0.8, dash))
            }
            Kind.Stack -> {
                var below: DoubleArray? = null
                dr.series.forEachIndexed { k, (xs, ys) ->
                    val c = t.palette[Math.floorMod((if (s.color >= 0) s.color else w.drawn.indexOf(dr)) + k, t.palette.size)]
                    val low = below ?: DoubleArray(xs.size)
                    val poly = (xs.indices.flatMap { listOf(px(xs[it]), py(ys[it])) } + xs.indices.reversed().flatMap { listOf(px(xs[it]), py(low[it])) }).toDoubleArray()
                    scene.add(Scene.Fill(listOf(poly), withAlpha(c, s.alpha * 0.85)))
                    below = ys
                }
            }
            Kind.Hist -> {
                val (edges, counts) = dr.series.single()
                if (s.line == 1) {
                    // histtype="step"
                    val path = ArrayList<Double>(); path += px(edges[0]); path += py(0.0)
                    for (k in counts.indices) { path += px(edges[k]); path += py(counts[k]); path += px(edges[k + 1]); path += py(counts[k]) }
                    path += px(edges.last()); path += py(0.0)
                    scene.add(Scene.Stroke(listOf(path.toDoubleArray()), color, s.width))
                } else for (k in counts.indices) {
                    if (counts[k] <= 0) continue
                    rect(px(edges[k]), py(counts[k]), px(edges[k + 1]), py(max(0.0, y0)), color)
                    scene.add(Scene.Stroke(listOf(doubleArrayOf(px(edges[k]), py(max(0.0, y0)), px(edges[k]), py(counts[k]), px(edges[k + 1]), py(counts[k]), px(edges[k + 1]), py(max(0.0, y0)))), t.face, 0.6))
                }
            }
            Kind.Box -> dr.boxes.forEachIndexed { k, b ->
                val c = (k + 1).toDouble()
                val xa = px(c - 0.25); val xb = px(c + 0.25)
                rect(xa, py(b.q3), xb, py(b.q1), withAlpha(dr.color, s.alpha * 0.3))
                scene.add(Scene.Stroke(listOf(doubleArrayOf(xa, py(b.q1), xb, py(b.q1), xb, py(b.q3), xa, py(b.q3), xa, py(b.q1))), color, 1.1))
                scene.add(Scene.Stroke(listOf(doubleArrayOf(xa, py(b.median), xb, py(b.median))), color, 2.0))
                scene.add(Scene.Stroke(listOf(doubleArrayOf(px(c), py(b.q1), px(c), py(b.lo)), doubleArrayOf(px(c), py(b.q3), px(c), py(b.hi)),
                    doubleArrayOf(px(c - 0.12), py(b.lo), px(c + 0.12), py(b.lo)), doubleArrayOf(px(c - 0.12), py(b.hi), px(c + 0.12), py(b.hi))), color, 1.0))
                b.outliers.forEach { o -> scene.add(Scene.Circle(px(c), py(o), 2.4, null, color, 0.9)) }
            }
            Kind.Violin -> dr.shapes.forEachIndexed { k, (ys, dens) ->
                val c = (k + 1).toDouble(); val m = dens.maxOrNull()?.takeIf { it > 0 } ?: 1.0
                val poly = (ys.indices.flatMap { listOf(px(c - 0.4 * dens[it] / m), py(ys[it])) } + ys.indices.reversed().flatMap { listOf(px(c + 0.4 * dens[it] / m), py(ys[it])) }).toDoubleArray()
                scene.add(Scene.Fill(listOf(poly), withAlpha(dr.color, s.alpha * 0.45)))
                scene.add(Scene.Stroke(listOf(poly + poly.copyOf(2)), color, 0.9))
                dr.boxes.getOrNull(k)?.let { b ->
                    scene.add(Scene.Stroke(listOf(doubleArrayOf(px(c), py(b.lo), px(c), py(b.hi))), color, 1.0))
                    scene.add(Scene.Stroke(listOf(doubleArrayOf(px(c - 0.12), py(b.median), px(c + 0.12), py(b.median))), color, 2.0))
                }
            }
            Kind.Ecdf -> dr.series.forEach { (xs, ys) ->
                val sx = ArrayList<Double>(); val sy = ArrayList<Double>()
                sx += x0; sy += 0.0
                for (k in xs.indices) { sx += xs[k]; sy += if (k == 0) 0.0 else ys[k - 1]; sx += xs[k]; sy += ys[k] }
                sx += x1; sy += 1.0
                scene.add(Scene.Stroke(polyline(sx.toDoubleArray(), sy.toDoubleArray()), color, s.width, dash))
            }
            Kind.Hist2D -> {
                val g = hist2d(dr.raw[0], dr.raw[1], x0, x1, y0, y1, binsOf(l, 24))
                for (j in 0 until g.ny) for (i in 0 until g.nx) {
                    val v = g.z[j][i]; if (v <= 0) continue
                    val xa = x0 + (x1 - x0) * i / g.nx; val ya = y0 + (y1 - y0) * j / g.ny
                    rect(px(xa), py(ya + (y1 - y0) / g.ny), px(xa + (x1 - x0) / g.nx), py(ya), withAlpha(cmapColor(s.cmap, (v - w.zMin) / (w.zMax - w.zMin)), s.alpha))
                }
            }
            Kind.Hexbin -> {
                val n = binsOf(l, 18)
                val rw = (right - left) / n / sqrt(3.0); val rh = (bottom - top) / n / sqrt(3.0)
                hexCounts(dr.raw[0], dr.raw[1], x0, x1, y0, y1, n).forEach { (u, v, c) ->
                    val cx = left + u * (right - left); val cy = bottom - v * (bottom - top)
                    val poly = DoubleArray(12) { k -> val ang = Math.PI / 6 + (k / 2) * Math.PI / 3; if (k % 2 == 0) cx + rw * cos(ang) else cy + rh * sin(ang) }
                    scene.add(Scene.Fill(listOf(poly), withAlpha(cmapColor(s.cmap, (c - w.zMin) / (w.zMax - w.zMin)), s.alpha)))
                }
            }
            Kind.Bar -> dr.values.forEachIndexed { k, h ->
                if (!h.isFinite()) return@forEachIndexed
                val c = k + dr.offset
                rect(px(c - dr.barWidth / 2), py(h), px(c + dr.barWidth / 2), py(max(0.0, y0).let { if (y0 > 0) y0 else 0.0 }), color)
            }
            Kind.BarH -> dr.values.forEachIndexed { k, h ->
                if (!h.isFinite()) return@forEachIndexed
                val c = k + dr.offset
                rect(px(if (x0 > 0) x0 else 0.0), py(c + dr.barWidth / 2), px(h), py(c - dr.barWidth / 2), color)
            }
            Kind.Events -> dr.raw.forEachIndexed { k, set ->
                val c = (k + 1).toDouble()
                scene.add(Scene.Stroke(set.filter { it.isFinite() }.map { v -> doubleArrayOf(px(v), py(c - 0.35), px(v), py(c + 0.35)) }, t.palette[Math.floorMod((if (s.color >= 0) s.color else 0) + k, t.palette.size)].let { withAlpha(it, s.alpha) }, s.width))
            }
            Kind.Heatmap -> {
                val g = dr.grid ?: return
                val matrix = dr.offset == 1.0
                val pixels = IntArray(g.nx * g.ny) { k ->
                    val i = k % g.nx; val j = g.ny - 1 - k / g.nx
                    val v = g.z[j][i]
                    if (v.isFinite()) withAlpha(cmapColor(s.cmap, (v - w.zMin) / (w.zMax - w.zMin)), s.alpha) else 0
                }
                if (matrix) scene.add(Scene.Image(px(-0.5), py(g.ny - 0.5), px(g.nx - 0.5) - px(-0.5), py(-0.5) - py(g.ny - 0.5), pixels, g.nx, g.ny))
                else scene.add(Scene.Image(px(g.x0), py(g.y1), px(g.x1) - px(g.x0), py(g.y0) - py(g.y1), pixels, g.nx, g.ny))
            }
            Kind.ContourF -> {
                val g = dr.grid ?: return
                val n = s.levels.coerceIn(2, 40)
                // Upsampled, each pixel takes its band's color.
                val nx = (g.nx - 1) * 3 + 1; val ny = (g.ny - 1) * 3 + 1
                val pixels = IntArray(nx * ny) { k ->
                    val fi = (k % nx) / 3.0; val fj = (ny - 1 - k / nx) / 3.0
                    val i = floor(fi).toInt().coerceIn(0, g.nx - 2); val j = floor(fj).toInt().coerceIn(0, g.ny - 2)
                    val a = fi - i; val c = fj - j
                    val v = g.z[j][i] * (1 - a) * (1 - c) + g.z[j][i + 1] * a * (1 - c) + g.z[j + 1][i] * (1 - a) * c + g.z[j + 1][i + 1] * a * c
                    if (!v.isFinite()) 0 else {
                        val band = floor((v - w.zMin) / (w.zMax - w.zMin) * n).coerceIn(0.0, n - 1.0)
                        withAlpha(cmapColor(s.cmap, (band + 0.5) / n), s.alpha)
                    }
                }
                scene.add(Scene.Image(px(g.x0), py(g.y1), px(g.x1) - px(g.x0), py(g.y0) - py(g.y1), pixels, nx, ny))
            }
            Kind.Contour -> {
                val g = dr.grid ?: return
                val z = g.finite; if (z.isEmpty()) return
                val lo = z.min(); val hi = z.max(); val n = s.levels.coerceIn(1, 40)
                for (k in 1..n) {
                    val level = lo + (hi - lo) * k / (n + 1)
                    val c = if (s.color >= 0) color else withAlpha(cmapColor(s.cmap, k.toDouble() / (n + 1)), s.alpha)
                    scene.add(Scene.Stroke(contour(g, level).map { seg -> doubleArrayOf(px(seg[0]), py(seg[1]), px(seg[2]), py(seg[3])) }, c, s.width * 0.8, dash))
                }
            }
            Kind.Quiver -> {
                // As the 2D graph's vector fields: each arrow centered on its point, its length by
                // the style (in proportion, all the same, or true), its head, colored by length or not.
                val pts = ArrayList<DoubleArray>()
                if (dr.raw.size == 4) { val (xs, ys, us, vs) = dr.raw; for (k in xs.indices) pts += doubleArrayOf(xs[k], ys[k], us[k], vs[k]) }
                else { val u = dr.grid ?: return; val v = dr.grid2 ?: return; for (j in 0 until u.ny) for (i in 0 until u.nx) pts += doubleArrayOf(u.x(i), u.y(j), u.z[j][i], v.z[j][i]) }
                val good = pts.filter { p -> p.all { it.isFinite() } }
                if (good.isEmpty()) return
                val mags = good.map { hypot(it[2], it[3]) }
                val largest = VectorField.robustMax(mags)
                val range = (mags.minOrNull() ?: 0.0) to largest.let { if (it > (mags.minOrNull() ?: 0.0)) it else (mags.minOrNull() ?: 0.0) + 1 }
                val kx = (px(x1) - px(x0)) / (x1 - x0); val ky = (py(y1) - py(y0)) / (y1 - y0)
                val cols = if (dr.raw.size == 4) sqrt(good.size.toDouble()).coerceAtLeast(4.0) else (dr.grid?.nx ?: 17).toDouble()
                val cell = (right - left) / cols
                val full = 0.86 * cell * s.arrowScale
                val length = VectorField.Length.entries.getOrElse(s.arrowLength) { VectorField.Length.Scaled }
                val tip = VectorField.Tip.entries.getOrElse(s.arrowTip) { VectorField.Tip.Triangle }
                val lw = max(0.5, s.width * 0.7)
                good.forEachIndexed { k, p ->
                    val m = mags[k]; if (m == 0.0) return@forEachIndexed
                    val sx = p[2] * kx; val sy = p[3] * ky; val sl = hypot(sx, sy); if (sl == 0.0) return@forEachIndexed
                    val len = when (length) { VectorField.Length.Equal -> full; VectorField.Length.True -> sl * s.arrowScale; else -> full * min(m / largest, 1.0) }
                    if (len < 0.5) return@forEachIndexed
                    val ux = sx / sl; val uy = sy / sl
                    val cx = px(p[0]); val cy = py(p[1])
                    val ax = cx - ux * len / 2; val ay = cy - uy * len / 2; val bx = cx + ux * len / 2; val by = cy + uy * len / 2
                    val c = if (s.arrowByLength) withAlpha(cmapColor(s.cmap, VectorField.position(m, range)), s.alpha) else color
                    val head = VectorField.head(tip, bx, by, ux, uy, (2.5 + lw * 2.2) * s.arrowTipSize * 1.6)
                    scene.add(Scene.Stroke(listOf(doubleArrayOf(ax, ay, head.shaftEndX, head.shaftEndY)), c, lw))
                    if (head.fills.isNotEmpty()) scene.add(Scene.Fill(head.fills, c))
                    if (head.strokes.isNotEmpty()) scene.add(Scene.Stroke(head.strokes, c, lw))
                    head.dot?.let { d -> scene.add(Scene.Fill(listOf(DoubleArray(24) { q -> val t = q / 2 * Math.PI / 6; if (q % 2 == 0) d[0] + d[2] * cos(t) else d[1] + d[2] * sin(t) }), c)) }
                }
            }
            Kind.Stream -> dr.series.forEach { (xs, ys) ->
                val line = polyline(xs, ys)
                scene.add(Scene.Stroke(line, color, s.width * 0.7))
                line.firstOrNull()?.let { p ->
                    val k = (p.size / 4) * 2
                    if (k >= 2 && k + 1 < p.size) {
                        val ax = p[k]; val ay = p[k + 1]; val ang = atan2(ay - p[k - 1], ax - p[k - 2])
                        scene.add(Scene.Stroke(listOf(doubleArrayOf(ax - 3.5 * cos(ang - 0.5), ay - 3.5 * sin(ang - 0.5), ax, ay, ax - 3.5 * cos(ang + 0.5), ay - 3.5 * sin(ang + 0.5))), color, s.width * 0.7))
                    }
                }
            }
            Kind.HLine -> scene.add(Scene.Stroke(dr.values.map { v -> doubleArrayOf(left, py(v), right, py(v)) }, color, s.width, dash))
            Kind.VLine -> scene.add(Scene.Stroke(dr.values.map { v -> doubleArrayOf(px(v), top, px(v), bottom) }, color, s.width, dash))
            Kind.HSpan -> rect(left, py(dr.values[1]), right, py(dr.values[0]), withAlpha(dr.color, s.alpha * 0.22))
            Kind.VSpan -> rect(px(dr.values[0]), top, px(dr.values[1]), bottom, withAlpha(dr.color, s.alpha * 0.22))
            // Text: its own size, and its color once one is picked (else the theme's ink).
            Kind.Text -> label(t, scene, px(dr.values[0]), py(dr.values[1]), l.text, textSize(s), if (s.color >= 0 || s.argb != 0) color else t.ink)
            Kind.Annotate -> {
                // The arrow as the arrows' options have it (head, its size, the line), arrow and note in one color.
                val ink = if (s.color >= 0 || s.argb != 0) color else t.ink
                val tx = px(dr.values[2]); val ty = py(dr.values[3]); val ax = px(dr.values[0]); val ay = py(dr.values[1])
                val ang = atan2(ay - ty, ax - tx); val gap = textSize(s) * 0.8
                val sx = tx + gap * cos(ang); val sy = ty + gap * sin(ang)
                val lw = max(0.6, s.width * 0.6)
                val tip = VectorField.Tip.entries.getOrElse(s.arrowTip) { VectorField.Tip.Triangle }
                val head = VectorField.head(tip, ax, ay, cos(ang), sin(ang), (3.0 + lw * 2.5) * s.arrowTipSize * 1.4)
                scene.add(Scene.Stroke(listOf(doubleArrayOf(sx, sy, head.shaftEndX, head.shaftEndY)), ink, lw, dash))
                if (head.fills.isNotEmpty()) scene.add(Scene.Fill(head.fills, ink))
                if (head.strokes.isNotEmpty()) scene.add(Scene.Stroke(head.strokes, ink, lw))
                head.dot?.let { d -> scene.add(Scene.Fill(listOf(DoubleArray(24) { q -> val a = q / 2 * Math.PI / 6; if (q % 2 == 0) d[0] + d[2] * cos(a) else d[1] + d[2] * sin(a) }), ink)) }
                label(t, scene, tx, ty, l.text, textSize(s), ink)
            }
            Kind.Pie -> Unit
        }
    }

    /** The segments of a field's contour at [level] (marching squares), each x₀, y₀, x₁, y₁ in data units. */
    fun contour(g: Grid, level: Double): List<DoubleArray> {
        val out = ArrayList<DoubleArray>()
        for (j in 0 until g.ny - 1) for (i in 0 until g.nx - 1) {
            val v = doubleArrayOf(g.z[j][i], g.z[j][i + 1], g.z[j + 1][i + 1], g.z[j + 1][i])
            if (v.any { !it.isFinite() }) continue
            val px = doubleArrayOf(g.x(i), g.x(i + 1), g.x(i + 1), g.x(i)); val py = doubleArrayOf(g.y(j), g.y(j), g.y(j + 1), g.y(j + 1))
            val pts = ArrayList<Double>()
            for (k in 0 until 4) {
                val a = v[k] - level; val b = v[(k + 1) % 4] - level
                if ((a < 0) != (b < 0)) { val f = a / (a - b); pts += px[k] + (px[(k + 1) % 4] - px[k]) * f; pts += py[k] + (py[(k + 1) % 4] - py[k]) * f }
            }
            if (pts.size >= 4) out += doubleArrayOf(pts[0], pts[1], pts[2], pts[3])
            if (pts.size == 8) out += doubleArrayOf(pts[4], pts[5], pts[6], pts[7])
        }
        return out
    }

    private fun drawPies(scene: Scene, w: Worked, t: Theme, left: Double, top: Double, right: Double, bottom: Double) {
        val pies = w.drawn.filter { it.layer.kind == Kind.Pie && it.error == null }
        pies.forEachIndexed { p, dr ->
            val cw = (right - left) / pies.size
            val cx = left + cw * (p + 0.5); val cy = (top + bottom) / 2
            val r = min(cw, bottom - top) * 0.42
            val total = dr.values.sum()
            var a = -Math.PI / 2
            dr.values.forEachIndexed { k, v ->
                val sweep = v / total * 2 * Math.PI
                val c = t.palette[Math.floorMod((if (dr.layer.style.color >= 0) dr.layer.style.color else 0) + k, t.palette.size)]
                val n = max(4, (sweep * 30).toInt())
                val poly = ArrayList<Double>(); poly += cx; poly += cy
                for (q in 0..n) { val ang = a + sweep * q / n; poly += cx + r * cos(ang); poly += cy + r * sin(ang) }
                scene.add(Scene.Fill(listOf(poly.toDoubleArray()), withAlpha(c, dr.layer.style.alpha)))
                scene.add(Scene.Stroke(listOf(poly.toDoubleArray() + doubleArrayOf(cx, cy)), t.background, 1.2))
                val mid = a + sweep / 2
                if (sweep > 0.25) label(t, scene, cx + r * 0.62 * cos(mid), cy + r * 0.62 * sin(mid), String.format(java.util.Locale.ROOT, "%.0f%%", v / total * 100), TICK, t.background.let { if (luminance(c) > 0.5) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() })
                dr.labels.getOrNull(k)?.let { name -> label(t, scene, cx + r * 1.14 * cos(mid), cy + r * 1.14 * sin(mid), name, TICK, t.ink, if (cos(mid) > 0.2) Scene.Anchor.Start else if (cos(mid) < -0.2) Scene.Anchor.End else Scene.Anchor.Middle) }
                a += sweep
            }
        }
    }

    private fun luminance(c: Int): Double = (0.2126 * ((c shr 16) and 0xFF) + 0.7152 * ((c shr 8) and 0xFF) + 0.0722 * (c and 0xFF)) / 255

    private fun drawColorbar(scene: Scene, w: Worked, t: Theme, x: Double, top: Double, bottom: Double) {
        val d = w.mapped ?: return
        val n = 64
        val pixels = IntArray(n) { k -> cmapColor(d.layer.style.cmap, 1 - k / (n - 1.0)) }
        scene.add(Scene.Image(x, top, 10.0, bottom - top, pixels, 1, n))
        scene.add(Scene.Stroke(listOf(doubleArrayOf(x, top, x + 10, top, x + 10, bottom, x, bottom, x, top)), t.ink, 0.6))
        ticks(w.zMin, w.zMax, 5).filter { it in w.zMin..w.zMax }.forEach { v ->
            val y = bottom - (v - w.zMin) / (w.zMax - w.zMin) * (bottom - top)
            scene.add(Scene.Stroke(listOf(doubleArrayOf(x + 10, y, x + 13, y)), t.ink, 0.6))
            label(t, scene, x + 15, y, tickText(v), TICK, t.muted, Scene.Anchor.Start)
        }
        d.layer.style.cbar.takeIf { it.isNotBlank() }?.let { label(t, scene, x + 46, (top + bottom) / 2, it, TICK, t.ink, Scene.Anchor.Middle, 90.0) }
    }

    private fun drawLegend(scene: Scene, w: Worked, t: Theme, left: Double, top: Double, right: Double, bottom: Double) {
        val entries = w.drawn.filter { it.error == null && legendName(it).isNotBlank() && it.layer.kind != Kind.Pie }
        if (entries.isEmpty()) return
        val size = 8.5; val line = 12.0; val sample = 16.0
        val texts = entries.map { spans(legendName(it)) }
        val boxW = 8 + sample + 5 + (texts.maxOf { width(it, size) }) + 8
        val boxH = 6 + entries.size * line
        // "Best": the corner whose region holds the fewest drawn points.
        val place = if (w.axes.legend in 1..9) w.axes.legend else bestCorner(w, left, top, right, bottom, boxW, boxH)
        val col = (place - 1) % 3; val row = (place - 1) / 3
        val x = when (col) { 0 -> left + 6; 1 -> (left + right - boxW) / 2; else -> right - 6 - boxW }
        val y = when (row) { 0 -> top + 6; 1 -> (top + bottom - boxH) / 2; else -> bottom - 6 - boxH }
        if (w.axes.legendFrame) {
            scene.add(Scene.Fill(listOf(doubleArrayOf(x, y, x + boxW, y, x + boxW, y + boxH, x, y + boxH)), withAlpha(t.face, 0.85)))
            scene.add(Scene.Stroke(listOf(doubleArrayOf(x, y, x + boxW, y, x + boxW, y + boxH, x, y + boxH, x, y)), withAlpha(t.muted, 0.5), 0.5))
        }
        entries.forEachIndexed { k, dr ->
            val cy = y + 3 + line * (k + 0.5)
            val s = dr.layer.style
            val c = withAlpha(dr.color, s.alpha)
            val sx = x + 8
            when {
                dr.layer.kind in setOf(Kind.Bar, Kind.BarH, Kind.Hist, Kind.FillBetween, Kind.HSpan, Kind.VSpan, Kind.Fill, Kind.Stack, Kind.Violin, Kind.Box) ->
                    scene.add(Scene.Fill(listOf(doubleArrayOf(sx, cy - 4, sx + sample, cy - 4, sx + sample, cy + 4, sx, cy + 4)), if (dr.layer.kind in setOf(Kind.Bar, Kind.BarH, Kind.Hist)) c else withAlpha(dr.color, s.alpha * 0.45)))
                dr.layer.kind == Kind.Scatter -> Marker.of(if (s.marker >= 0) s.marker else 0).addTo(scene, sx + sample / 2, cy, 3.0, c)
                dr.layer.kind.mapped -> scene.add(Scene.Image(sx, cy - 4, sample, 8.0, IntArray(8) { k2 -> cmapColor(s.cmap, k2 / 7.0) }, 8, 1))
                else -> {
                    scene.add(Scene.Stroke(listOf(doubleArrayOf(sx, cy, sx + sample, cy)), c, s.width, LineStyle.of(s.line).pattern(s.width)))
                    if (s.marker >= 0) Marker.of(if (s.marker >= 0) s.marker else 0).addTo(scene, sx + sample / 2, cy, 2.6, c)
                }
            }
            scene.add(Scene.Label(sx + sample + 5, cy, texts[k].joinToString("") { it.text }, size, t.ink, Scene.Anchor.Start, Scene.Font.Roman, spans = texts[k]))
        }
    }

    /** A layer's name in the legend: its label; a fitted curve without one shows its equation and R². */
    fun legendName(d: Drawn): String = d.layer.style.label.ifBlank { if (d.layer.kind == Kind.Fit) d.labels.firstOrNull() ?: "" else "" }

    private fun bestCorner(w: Worked, left: Double, top: Double, right: Double, bottom: Double, bw: Double, bh: Double): Int {
        val pts = ArrayList<Pair<Double, Double>>()
        val lx0 = w.x0; val lx1 = w.x1; val ly0 = w.y0; val ly1 = w.y1
        for (d in w.drawn) d.series.forEach { (xs, ys) -> for (k in 0 until min(xs.size, ys.size) step max(1, xs.size / 80)) pts += ((xs[k] - lx0) / (lx1 - lx0)) to ((ys[k] - ly0) / (ly1 - ly0)) }
        val fw = bw / (right - left); val fh = bh / (bottom - top)
        val options = listOf(3, 1, 9, 7)
        return options.minByOrNull { place ->
            val col = (place - 1) % 3; val row = (place - 1) / 3
            val ux0 = if (col == 0) 0.0 else 1 - fw; val uy1 = if (row == 0) 1.0 else fh
            pts.count { (u, v) -> u in ux0..(ux0 + fw) && v in (uy1 - fh)..uy1 }
        } ?: 3
    }
}
