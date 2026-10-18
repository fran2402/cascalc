package com.example.cas.engine

import com.example.cas.cas.Eq
import com.example.cas.cas.Expr
import com.example.cas.cas.Mat
import com.example.cas.cas.MathError
import com.example.cas.cas.Rel
import com.example.cas.cas.Seq
import com.example.cas.cas.freeVars
import com.example.cas.cas.subst
import com.example.cas.editor.MathCodec
import com.example.cas.editor.MathRow
import com.example.cas.editor.Sym

/**
 * A notebook: a document of blocks you scroll through. Text (with $math$ in it), math (any
 * calculator input, its answer under it) and graphs (of anything defined above). Definitions
 * (a := 5, f(x) := …) hold for every block below them, so changing one updates all that follow.
 * Saved as a small text file (.casnb) and exported as LaTeX.
 */
object Notebook {
    sealed class Block(val id: Long) {
        class Text(id: Long, val text: String) : Block(id)
        class Math(id: Long, val row: MathRow) : Block(id)
        /** A graph of the functions in [row] (separated by commas), from [from] to [to] (math; a range that shows the curve when empty). */
        class Graph(id: Long, val row: MathRow, val from: MathRow = MathRow(), val to: MathRow = MathRow()) : Block(id)
        /**
         * A data table, edited like the graphs' tables (formulas, formats, sorting, fits…): each
         * column named with a letter stands for that column as a list in the blocks below
         * (mean(x), sd(y)…). [plot] draws the second and later columns against the first.
         */
        class Table(id: Long, val table: com.example.cas.graph.DataTable, val plot: Boolean = false) : Block(id)
    }

    class Doc(val id: String, val title: String, val blocks: List<Block>, val edited: Long)

    // ---- The file ------------------------------------------------------------------------------

    private const val MAGIC = "CASNB 1"

    private fun esc(s: String) = s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\r", "")
    private fun unesc(s: String): String = buildString {
        var k = 0
        while (k < s.length) {
            val c = s[k]
            if (c == '\\' && k + 1 < s.length) {
                when (s[k + 1]) { 'n' -> append('\n'); 't' -> append('\t'); else -> append(s[k + 1]) }
                k += 2
            } else { append(c); k++ }
        }
    }

    fun encode(d: Doc): String = buildString {
        appendLine(MAGIC)
        appendLine("title\t" + esc(d.title))
        appendLine("edited\t" + d.edited)
        for (b in d.blocks) appendLine(when (b) {
            is Block.Text -> "T\t${b.id}\t" + esc(b.text)
            is Block.Math -> "M\t${b.id}\t" + esc(MathCodec.encode(b.row))
            is Block.Graph -> "G\t${b.id}\t" + esc(MathCodec.encode(b.row)) + "\t" + esc(MathCodec.encode(b.from)) + "\t" + esc(MathCodec.encode(b.to))
            is Block.Table -> "D\t${b.id}\t${if (b.plot) 1 else 0}\t" + esc("v2\n" + b.table.encode())
        })
    }

    fun decode(id: String, text: String): Doc {
        val lines = text.lines()
        require(lines.firstOrNull()?.trim() == MAGIC) { "Not a notebook file" }
        var title = ""; var edited = 0L
        val blocks = ArrayList<Block>()
        for (line in lines.drop(1)) {
            if (line.isBlank()) continue
            val f = line.split('\t')
            when (f[0]) {
                "title" -> title = unesc(f.getOrElse(1) { "" })
                "edited" -> edited = f.getOrNull(1)?.toLongOrNull() ?: 0L
                "T" -> blocks += Block.Text(f[1].toLong(), unesc(f.getOrElse(2) { "" }))
                "M" -> blocks += Block.Math(f[1].toLong(), MathCodec.decode(unesc(f.getOrElse(2) { "" })))
                "G" -> blocks += Block.Graph(f[1].toLong(), MathCodec.decode(unesc(f.getOrElse(2) { "" })), limit(f.getOrNull(3)), limit(f.getOrNull(4)))
                "D" -> {
                    val field = unesc(f.getOrElse(3) { "" })
                    val table = if (field.startsWith("v2\n")) com.example.cas.graph.DataTable.decode(field.removePrefix("v2\n"))
                    else {
                        // Tables saved before they were text: names and cells as math.
                        val n = field.toIntOrNull() ?: 0
                        val names = (0 until n).map { k -> com.example.cas.engine.Formatter.plain(MathCodec.decode(unesc(f.getOrElse(4 + 2 * k) { "" }))) }
                        val cols = (0 until n).map { k -> unesc(f.getOrElse(5 + 2 * k) { "" }).let { c -> if (c.isEmpty()) emptyList() else c.split(CELL).map { Formatter.plain(MathCodec.decode(it)).replace("−", "-") } } }
                        com.example.cas.graph.DataTable(names, cols, null, null)
                    }
                    blocks += Block.Table(f[1].toLong(), table ?: emptyTable(), f.getOrNull(2) == "1")
                }
            }
        }
        return Doc(id, title, blocks, edited)
    }

    private const val CELL = "\u001E"

    /** A graph's limit as saved: math, or a plain number from before limits were math. */
    private fun limit(field: String?): MathRow {
        val t = field?.let { unesc(it) } ?: return MathRow()
        if (t.isEmpty()) return MathRow()
        t.toDoubleOrNull()?.let { v -> return MathRow(java.math.BigDecimal(v).round(java.math.MathContext(8)).stripTrailingZeros().toPlainString().replace("-", "−").map { Sym(it.toString()) }.toMutableList()) }
        return runCatching { MathCodec.decode(t) }.getOrDefault(MathRow())
    }

    /** A table column's name as the letter (or name) it defines: letters only (empty when it has none). */
    fun columnName(name: String): String = name.trim().takeIf { n -> n.isNotEmpty() && n.all { it.isLetter() } } ?: ""

    /** A new table: columns x and y, five empty rows. */
    fun emptyTable() = com.example.cas.graph.DataTable(listOf("x", "y"), listOf(List(5) { "" }, List(5) { "" }), null, null)

    /** A cell as an expression: a plain decimal exactly (1.5 is 3/2), anything else as its number. */
    private fun cellValue(t: com.example.cas.graph.DataTable, c: Int, r: Int, ev: Evaluator): Expr? {
        val text = t.cell(c, r).trim()
        if (text.isEmpty()) return null
        if (Regex("""^-?\d+(\.\d+)?$""").matches(text))
            return runCatching { ev.evaluate(MathRow(text.replace("-", "−").map { Sym(it.toString()) }.toMutableList())) }.getOrNull()
        return t.value(c, r)?.let { com.example.cas.cas.Flt(it) }
    }

    // ---- Working it out ------------------------------------------------------------------------

    /** A math block's outcome: its answer, or what went wrong; [defines] names what it defined (a, f). */
    class Outcome(val answer: Answer?, val error: String?, val defines: List<String> = emptyList())

    /** A graph block's curves: each function's points (NaN where it breaks) over the same xs, and the y range to show. */
    class Plot(val xs: DoubleArray, val curves: List<DoubleArray>, val yMin: Double, val yMax: Double, val labels: List<String>, val error: String? = null,
               /** The functions with the definitions put in and their letter written x, for the 2D graph. */
               val forGraph: List<MathRow> = emptyList(),
               /** A table's points: for each later column, its values against the first column's. */
               val points: List<Pair<DoubleArray, DoubleArray>> = emptyList())

    class Run(val outcomes: Map<Long, Outcome>, val plots: Map<Long, Plot>)

    /** Splits [row] at its top-level commas. */
    private fun commaParts(row: MathRow): List<MathRow> {
        val parts = ArrayList<MathRow>(); var cur = MathRow()
        for (n in row.items) {
            if (n is Sym && n.text == ",") { parts += cur; cur = MathRow() }
            else cur.add(MathCodec.copy(MathRow(mutableListOf(n))).items.single())
        }
        parts += cur
        return parts.filter { it.items.isNotEmpty() }
    }

    private fun isDefinition(r: MathRow) = r.items.any { it is Sym && it.text == ":=" }

    /**
     * Works the blocks out top to bottom, each with the definitions above it: a := 5 or
     * f(x) := x² defines for the blocks below; several definitions may share a block, split by
     * commas (g := 9.81, h := 1.5).
     */
    fun run(blocks: List<Block>, angle: AngleUnit = AngleUnit.Radians, units: UnitSystem = UnitSystem.SI,
            coordinates: com.example.cas.cas.Coordinates = com.example.cas.cas.Coordinates()): Run {
        val variables = LinkedHashMap<String, Expr>()
        val functions = LinkedHashMap<String, UserFunction>()
        var ans: Expr? = null
        val outcomes = HashMap<Long, Outcome>()
        val plots = HashMap<Long, Plot>()
        fun evaluator() = Evaluator(angle, ans, variables.toMap(), units, coordinates, functions.toMap(), solveEquations = true)
        for (b in blocks) when (b) {
            is Block.Text -> Unit
            is Block.Math -> {
                if (b.row.items.isEmpty()) continue
                val parts = commaParts(b.row)
                val defs = parts.size > 1 && parts.all(::isDefinition)
                try {
                    val defined = ArrayList<String>()
                    var last: Expr? = null
                    for (p in if (defs) parts else listOf(b.row)) {
                        val ev = evaluator()
                        val v = ev.evaluate(com.example.cas.editor.trimMatrices(MathCodec.copy(p)))
                        ev.definedFunction?.let { (n, f) -> functions[n] = f; defined += n }
                        ev.assigned?.let { (n, value) -> variables[n] = value; defined += n }
                        last = v
                    }
                    val v = last!!
                    if (defined.isEmpty()) ans = v
                    outcomes[b.id] = Outcome(if (defs) null else Formatter.answer(v), null, defined)
                } catch (e: MathError) {
                    outcomes[b.id] = Outcome(null, e.message ?: "Couldn't work this out")
                } catch (e: com.example.cas.engine.ChooseUnknowns) {
                    outcomes[b.id] = Outcome(null, "Say what to solve for: solve(…; x)")
                } catch (e: ArithmeticException) {
                    outcomes[b.id] = Outcome(null, "That's too large to work out")
                } catch (e: StackOverflowError) {
                    outcomes[b.id] = Outcome(null, "That's too complicated to work out")
                } catch (e: RuntimeException) {
                    outcomes[b.id] = Outcome(null, "Couldn't work this out")
                }
            }
            is Block.Graph -> plots[b.id] = plot(b, evaluator())
            is Block.Table -> {
                val ev = evaluator()
                val t = b.table
                val defined = ArrayList<String>()
                var error: String? = null
                t.names.forEachIndexed { c, rawName ->
                    val name = columnName(rawName)
                    val col = (0 until t.rowCount).mapNotNull { r -> cellValue(t, c, r, ev).also { v -> if (v == null && t.cell(c, r).isNotBlank() && error == null) error = "A cell isn't a number: " + t.cell(c, r) } }
                    if (name.isNotEmpty() && col.isNotEmpty()) { variables[name] = Mat(col.size, 1, col); defined += name }
                }
                outcomes[b.id] = Outcome(null, error, defined)
                if (b.plot && t.columns.size >= 2) {
                    val xs = (0 until t.rowCount).map { r -> t.value(0, r) ?: Double.NaN }
                    val series = (1 until t.columns.size).map { c ->
                        val pairs = xs.indices.map { r -> xs[r] to (t.value(c, r) ?: Double.NaN) }.filter { it.first.isFinite() && it.second.isFinite() }
                        pairs.map { it.first }.toDoubleArray() to pairs.map { it.second }.toDoubleArray()
                    }
                    val allX = series.flatMap { it.first.toList() }; val allY = series.flatMap { it.second.toList() }
                    if (allX.isNotEmpty()) {
                        val xLo = allX.min(); val xHi = allX.max(); val yLo = minOf(allY.min(), 0.0); val yHi = maxOf(allY.max(), 0.0)
                        val xPad = maxOf((xHi - xLo) * 0.08, 0.5); val yPad = maxOf((yHi - yLo) * 0.1, 0.5)
                        plots[b.id] = Plot(doubleArrayOf(xLo - xPad, xHi + xPad), emptyList(), yLo - yPad, yHi + yPad, t.names.drop(1), points = series)
                    }
                }
            }
        }
        return Run(outcomes, plots)
    }

    private const val POINTS = 240

    /** The functions in a graph block, sampled across its range (or a range that shows them). */
    fun plot(b: Block.Graph, ev: Evaluator): Plot {
        val parts = commaParts(b.row)
        if (parts.isEmpty()) return Plot(DoubleArray(0), emptyList(), -1.0, 1.0, emptyList(), "Type a function to draw, like y(t) or sin x")
        val exprs = try { parts.map { ev.evaluate(MathCodec.copy(it)) } } catch (e: Exception) {
            return Plot(DoubleArray(0), emptyList(), -1.0, 1.0, emptyList(), (e as? MathError)?.message ?: "Couldn't draw this")
        }
        val fs = exprs.mapNotNull { e ->
            if (e is Eq || e is Seq || e is Rel || e is Mat) return@mapNotNull null
            val v = e.freeVars().filter { it != "i" }.singleOrNull() ?: if (e.freeVars().isEmpty()) "x" else return@mapNotNull null
            val compiled = runCatching { com.example.cas.graph.Compiler.compile(e, listOf(v)) }.getOrNull() ?: return@mapNotNull null
            val arg = DoubleArray(1)
            ({ x: Double -> arg[0] = x; runCatching { compiled(arg) }.getOrDefault(Double.NaN) }) to v
        }
        val forGraph = exprs.mapNotNull { e ->
            if (e is Eq || e is Seq || e is Rel || e is Mat) return@mapNotNull null
            val v = e.freeVars().filter { it != "i" }.singleOrNull()
            val inX = if (v == null || v == "x") e else e.subst(com.example.cas.cas.Sym(v), com.example.cas.cas.Sym("x"))
            MathRow(mutableListOf<com.example.cas.editor.Node>(Sym("y"), Sym("="))).also { r -> Formatter.row(inX).items.toList().forEach { r.add(it) } }
        }
        if (fs.isEmpty()) return Plot(DoubleArray(0), emptyList(), -1.0, 1.0, emptyList(), "Each function needs one letter to draw it against")
        fun bound(r: MathRow): Double? = if (r.items.isEmpty()) null else runCatching { com.example.cas.cas.Numeric.real(ev.evaluate(MathCodec.copy(r))) }.getOrNull()?.takeIf { it.isFinite() }
        val fromV = bound(b.from); val toV = bound(b.to)
        val (a, c) = if (fromV != null && toV != null && fromV < toV) fromV to toV
        else com.example.cas.graph.Sketch.range(fs.first().first)?.let { (sa, sc) -> (fromV ?: sa) to (toV ?: sc) }?.takeIf { it.first < it.second } ?: ((fromV ?: -5.0) to (toV ?: 5.0))
        val xs = DoubleArray(POINTS + 1) { a + (c - a) * it / POINTS }
        val curves = fs.map { (f, _) -> DoubleArray(xs.size) { f(xs[it]) } }
        val finite = curves.flatMap { cv -> cv.filter { it.isFinite() } }.sorted()
        if (finite.isEmpty()) return Plot(xs, curves, -1.0, 1.0, parts.map { Latex.of(it) }, "Nothing to draw in this range")
        var lo = finite[(finite.size * 0.02).toInt()]; var hi = finite[(finite.size * 0.98).toInt().coerceAtMost(finite.size - 1)]
        lo = minOf(lo, 0.0); hi = maxOf(hi, 0.0)
        if (hi - lo < 1e-9) { hi += 1; lo -= 1 }
        val pad = (hi - lo) * 0.1
        return Plot(xs, curves, lo - pad, hi + pad, parts.map { Latex.of(it) }, forGraph = forGraph)
    }

    // ---- LaTeX ---------------------------------------------------------------------------------

    /** The notebook as a LaTeX document: text as paragraphs (a line starting # as a section), math as display equations with their answers, graphs as pgfplots. */
    fun latex(d: Doc, run: Run): String = buildString {
        appendLine("\\documentclass[11pt]{article}")
        appendLine("\\usepackage{amsmath,amssymb}")
        appendLine("\\usepackage[normalem]{ulem}")
        appendLine("\\usepackage{hyperref}")
        appendLine("\\usepackage{pgfplots}")
        appendLine("\\pgfplotsset{compat=1.18}")
        appendLine("\\title{${texText(d.title.ifBlank { "Notebook" })}}")
        appendLine("\\date{}")
        appendLine("\\begin{document}")
        appendLine("\\maketitle")
        appendLine()
        for (b in d.blocks) when (b) {
            is Block.Text -> { append(Markdown.latex(Markdown.parse(b.text))); appendLine() }
            is Block.Table -> {
                val t = b.table
                val n = t.names.size
                if (n > 0) {
                    appendLine("\\begin{center}\\begin{tabular}{" + "r".repeat(n) + "}")
                    appendLine("\\hline")
                    appendLine(t.names.joinToString(" & ") { "\\texttt{" + texText(it) + "}" } + " \\\\ \\hline")
                    for (r in 0 until t.rowCount) appendLine((0 until n).joinToString(" & ") { c -> "\\texttt{" + texText(t.value(c, r)?.let { v -> if (com.example.cas.graph.Sheet.isFormula(t.cell(c, r))) com.example.cas.graph.DataTable.text(v) else t.cell(c, r) } ?: t.cell(c, r)) + "}" } + " \\\\")
                    appendLine("\\hline")
                    appendLine("\\end{tabular}\\end{center}")
                    appendLine()
                }
                run.plots[b.id]?.let { p -> if (p.points.isNotEmpty()) {
                    appendLine("\\begin{center}\\begin{tikzpicture}\\begin{axis}[width=0.8\\textwidth,height=6cm,grid=major]")
                    p.points.forEachIndexed { k, (px, py) ->
                        appendLine("\\addplot[only marks] coordinates {" + px.indices.joinToString(" ") { "(${num(px[it])},${num(py[it])})" } + "};")
                        appendLine("\\addlegendentry{" + texText(p.labels.getOrElse(k) { "" }) + "}")
                    }
                    appendLine("\\end{axis}\\end{tikzpicture}\\end{center}")
                } }
            }
            is Block.Math -> if (b.row.items.isNotEmpty()) {
                val o = run.outcomes[b.id]
                val defs = o?.defines?.isNotEmpty() == true
                val q = Latex.of(b.row).replace(":=", " = ").let { if (defs) it.replace(",", ",\\quad ") else it }
                val a = o?.answer?.let { ans -> (if (ans.isStatement) " \\;\\Rightarrow\\; " else if (ans.isApproximate) " \\approx " else " = ") + Latex.of(ans.exact) } ?: ""
                appendLine("\\[ $q$a \\]")
                appendLine()
            }
            is Block.Graph -> {
                val p = run.plots[b.id]
                if (p != null && p.error == null) {
                    appendLine("\\begin{center}\\begin{tikzpicture}\\begin{axis}[width=0.8\\textwidth,height=6cm,axis lines=middle,grid=major,ymin=${num(p.yMin)},ymax=${num(p.yMax)}]")
                    p.curves.forEachIndexed { k, cv ->
                        val coords = p.xs.indices.filter { cv[it].isFinite() }.joinToString(" ") { "(${num(p.xs[it])},${num(cv[it])})" }
                        appendLine("\\addplot[thick,no marks] coordinates {$coords};")
                        appendLine("\\addlegendentry{\$${p.labels.getOrElse(k) { "" }}\$}")
                    }
                    appendLine("\\end{axis}\\end{tikzpicture}\\end{center}")
                    appendLine()
                }
            }
        }
        appendLine("\\end{document}")
    }

    private fun num(v: Double) = String.format(java.util.Locale.ROOT, "%.5g", v)
    private fun texText(s: String) = s.replace("\\", "\\textbackslash{}").replace("&", "\\&").replace("%", "\\%").replace("#", "\\#").replace("_", "\\_")
    /** Text with $…$ math kept as math and the rest escaped. */
    private fun texInline(s: String): String = LatexParser.inline(s).joinToString("") { (math, piece) -> if (math) "\$$piece\$" else texText(piece) }
}
