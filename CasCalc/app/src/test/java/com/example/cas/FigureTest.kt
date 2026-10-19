package com.example.cas

import com.example.cas.editor.Frac
import com.example.cas.editor.Func
import com.example.cas.editor.MathRow
import com.example.cas.editor.Node
import com.example.cas.editor.Pow
import com.example.cas.editor.Sym
import com.example.cas.engine.Evaluator
import com.example.cas.graph.DataTable
import com.example.cas.graph.Figure
import com.example.cas.graph.Figure.Kind
import com.example.cas.graph.FigurePython
import com.example.cas.graph.Scene
import com.example.cas.graph.SvgWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FigureTest {
    private fun m(vararg parts: Any): MathRow = MathRow(parts.flatMap { p -> when (p) { is String -> p.map { Sym(it.toString()) }; is Node -> listOf(p); else -> error("") } }.toMutableList())

    private fun f(name: String, arg: String) = Func(name, listOf(m(arg)))

    private val table = DataTable(listOf("h", "w", "g"), listOf(
        listOf("150", "160", "165", "170", "171", "172", "180", "210"),
        listOf("50", "58", "61", "66", "64", "70", "77", "90"),
        listOf("1", "2", "3", "4", "5", "6", "7", "8"),
    ), null, null)
    private val vars = Figure.tableVariables(table) + ("M" to com.example.cas.cas.Mat(2, 2, listOf(1, 2, 3, 4).map { com.example.cas.cas.Flt(it.toDouble()) }))
    private val eval: (MathRow) -> com.example.cas.cas.Expr = { r -> Evaluator(variables = vars).evaluate(com.example.cas.editor.MathCodec.copy(r)) }
    private val app = Figure.THEMES[1]

    private fun doc(vararg layers: Figure.Layer, layout: String = "A") = Figure.withLayout(Figure.Doc("t", "", listOf(Figure.Axes(layers.toList())), layout), layout)

    @Test fun valuesOfInputs() {
        assertTrue(Figure.value(m("h"), eval) is Figure.Value.Data)
        assertTrue(Figure.value(m(f("sin", "x")), eval) is Figure.Value.Fn)
        assertTrue(Figure.value(m("x", Pow(m("2")), "+y"), eval) is Figure.Value.Fn2)
        assertEquals(3.0, (Figure.value(m("1+2"), eval) as Figure.Value.Num).v, 1e-12)
        assertTrue(Figure.value(MathRow(), eval) is Figure.Value.Empty)
        assertEquals(3, Figure.parts(m("h,w,g")).size)
        assertEquals(listOf(1.0, 4.0, 9.0), (Figure.value(m("[1,4,9]"), eval) as Figure.Value.Data).v.toList())
        val mat = Figure.value(m("[[1,2],[3,4]]"), eval) as Figure.Value.Matrix
        assertEquals(2, mat.rows); assertEquals(2, mat.cols)
        // Lists split at the commas between them, not inside.
        assertEquals(2, Figure.parts(m("[1,2,3],[4,5]")).size)
    }

    @Test fun statistics() {
        val b = Figure.boxStats(doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 100.0))!!
        assertEquals(3.0, b.q1, 1e-12); assertEquals(5.0, b.median, 1e-12); assertEquals(7.0, b.q3, 1e-12)
        assertEquals(8.0, b.hi, 1e-12); assertEquals(listOf(100.0), b.outliers)
        val (edges, counts) = Figure.histogram(doubleArrayOf(0.0, 1.0, 1.0, 2.0, 3.0), 3)
        assertEquals(4, edges.size); assertEquals(5.0, counts.sum(), 1e-12)
        val (_, dens) = Figure.histogram(doubleArrayOf(0.0, 1.0, 1.0, 2.0, 3.0), 3, density = true)
        assertEquals(1.0, dens.sum() * (edges[1] - edges[0]), 1e-9)
        assertEquals(listOf(0.0, 1.0, 2.0, 3.0, 4.0, 5.0), Figure.ticks(0.0, 5.0, 5).toList())
    }

    @Test fun functionsFollowTheData() {
        val d = doc(Figure.Layer(Kind.Scatter, listOf(m("h"), m("w"))), Figure.Layer(Kind.Plot, listOf(m("0.62x−41"))))
        val w = Figure.build(d, eval, app).worked.single()
        assertEquals(2, w.drawn.size)
        assertTrue(w.drawn.all { it.error == null })
        // The line is drawn across the data's range.
        val line = w.drawn[1].series.single()
        assertTrue(line.first.first() <= 150.0 && line.first.last() >= 210.0)
        assertTrue(w.x0 < 150 && w.x1 > 210)
    }

    @Test fun everyKindDraws() {
        val inputs = mapOf(
            Kind.Plot to listOf(m(f("sin", "x"))), Kind.Scatter to listOf(m("h"), m("w")), Kind.Step to listOf(m("w")), Kind.Stem to listOf(m("w")),
            Kind.FillBetween to listOf(MathRow(), m(f("sin", "x")), m(f("cos", "x"))), Kind.Stack to listOf(m("g,g")),
            Kind.Parametric to listOf(m(f("cos", "t")), m(f("sin", "t"))), Kind.Polar to listOf(m("1+", f("cos", "θ"))), Kind.Fill to listOf(m("g"), m("w")),
            Kind.Hist to listOf(m("h")), Kind.Box to listOf(m("h,w")), Kind.Violin to listOf(m("h,w")), Kind.Ecdf to listOf(m("h")),
            Kind.Hist2D to listOf(m("h"), m("w")), Kind.Hexbin to listOf(m("h"), m("w")), Kind.Bar to listOf(m("g")), Kind.BarH to listOf(m("g")),
            Kind.Pie to listOf(m("g")), Kind.Events to listOf(m("h,w")), Kind.Contour to listOf(m("x", Pow(m("2")), "+y", Pow(m("2")))),
            Kind.ContourF to listOf(m("x", Pow(m("2")), "−y")), Kind.Heatmap to listOf(m(f("sin", "xy"))), Kind.Quiver to listOf(MathRow(), MathRow(), m("−y"), m("x")),
            Kind.Stream to listOf(m("−y"), m("x")), Kind.HLine to listOf(m("1")), Kind.VLine to listOf(m("2")), Kind.HSpan to listOf(m("0"), m("1")),
            Kind.VSpan to listOf(m("0"), m("1")), Kind.Text to listOf(m("0"), m("0")), Kind.Annotate to listOf(m("0"), m("0"), m("1"), m("1")),
            Kind.Fit to listOf(m("h"), m("w")), Kind.Kde to listOf(m("h")),
        )
        assertEquals(Kind.entries.toSet(), inputs.keys)
        for ((kind, rows) in inputs) {
            val d = doc(Figure.Layer(kind, rows, text = "a, b", style = Figure.Style(label = "L")))
            val built = Figure.build(d, eval, app)
            val drawn = built.worked.single().drawn
            assertEquals("$kind", 1, drawn.size)
            assertNull("$kind: ${drawn.single().error}", drawn.single().error)
            assertTrue("$kind draws something", built.scene.items.size > 10)
            assertTrue(SvgWriter.write(built.scene).contains("<svg"))
            val py = FigurePython.write(d, built)
            assertTrue("$kind in python", py.contains("ax.${kind.py}("))
        }
    }

    @Test fun fitsAndDensities() {
        // y = 2x + 1 exactly: the line, R² = 1, in the legend.
        val t = DataTable(listOf("a", "b"), listOf(listOf("0", "1", "2", "3"), listOf("1", "3", "5", "7")), null, null)
        val ev: (MathRow) -> com.example.cas.cas.Expr = { r -> Evaluator(variables = Figure.tableVariables(t)).evaluate(com.example.cas.editor.MathCodec.copy(r)) }
        val w = Figure.build(doc(Figure.Layer(Kind.Fit, listOf(m("a"), m("b")))), ev, app).worked.single()
        val d = w.drawn.single()
        assertNull(d.error)
        assertEquals(2.0, d.values[0], 1e-6); assertEquals(1.0, d.values[1], 1e-6)
        assertTrue(Figure.legendName(d), Figure.legendName(d).contains("R^2 = 1.0000"))
        // An exponential model is chosen by its place in FitModel.
        val e = Figure.build(doc(Figure.Layer(Kind.Fit, listOf(m("a"), m("b")), style = Figure.Style(model = com.example.cas.graph.FitModel.Quadratic.ordinal))), ev, app).worked.single().drawn.single()
        assertEquals(3, e.values.size)
        // A density curve covers the data and its area is about 1.
        val k = Figure.build(doc(Figure.Layer(Kind.Kde, listOf(m("h")))), eval, app).worked.single().drawn.single()
        val (xs, ys) = k.series.single()
        val area = (1 until xs.size).sumOf { (xs[it] - xs[it - 1]) * (ys[it] + ys[it - 1]) / 2 }
        assertEquals(1.0, area, 0.05)
        // Custom colors and the model survive saving.
        val styled = doc(Figure.Layer(Kind.Fit, listOf(m("a"), m("b")), style = Figure.Style(argb = 0xFF123456.toInt(), model = 4)))
        val back = Figure.decode("x", Figure.encode(styled)).axes[0].layers[0].style
        assertEquals(0xFF123456.toInt(), back.argb); assertEquals(4, back.model)
        assertTrue(Figure.THEMES.size >= 18)
        assertTrue(Figure.THEMES.any { it.sans })
    }

    @Test fun columnNamedXGivesWayToTheVariable() {
        val t = DataTable(listOf("x", "y"), listOf(listOf("1", "2", "3"), listOf("2", "4", "6")), null, null)
        val ev = Figure.withFallback(Figure.tableVariables(t)) { v, r -> Evaluator(variables = v).evaluate(com.example.cas.editor.MathCodec.copy(r)) }
        val w = Figure.build(doc(Figure.Layer(Kind.Scatter, listOf(m("x"), m("y"))), Figure.Layer(Kind.Plot, listOf(m("3", f("sin", "x"))))), ev, app).worked.single()
        assertTrue(w.drawn.all { it.error == null })
        assertEquals(2, w.drawn.size)
    }

    @Test fun functionsOfColumnsWorkRowByRow() {
        val t = DataTable(listOf("x", "y"), listOf(listOf("1", "2", "3"), listOf("0", "1", "2")), null, null)
        val ev = Figure.withFallback(Figure.tableVariables(t)) { v, r -> Evaluator(variables = v).evaluate(com.example.cas.editor.MathCodec.copy(r)) }
        fun data(s: String) = (Figure.value(com.example.cas.engine.PlainMath.parse(s), ev) as Figure.Value.Data).v.toList()
        assertEquals(listOf(kotlin.math.sin(0.0), kotlin.math.sin(1.0), kotlin.math.sin(2.0)), data("sin(y)"))
        assertEquals(listOf(0.0, 2.0, 6.0), data("x*y"))
        assertEquals(listOf(1.0, 2.0, 5.0), data("y^2 + 1"))
        // A whole-column function still sees the whole column.
        assertEquals(1.0, (Figure.value(com.example.cas.engine.PlainMath.parse("mean(y)"), ev) as Figure.Value.Num).v, 1e-12)
        // A line of a function of a column is drawn against the x column.
        val w = Figure.build(doc(Figure.Layer(Kind.Plot, listOf(com.example.cas.engine.PlainMath.parse("y^2")))), ev, app).worked.single()
        assertEquals(listOf(1.0, 2.0, 3.0), w.drawn.single().series.single().first.toList())
        // Contours still take x and y as variables.
        val c = Figure.build(doc(Figure.Layer(Kind.ContourF, listOf(com.example.cas.engine.PlainMath.parse("x^2 + y^2")))), ev, app).worked.single()
        assertNull(c.drawn.single().error)
    }

    @Test fun columnsWithLongNames() {
        val t = DataTable(listOf("freq", "Signal", "amp_2", "x"), listOf(listOf("1", "2", "3"), listOf("2", "4", "6"), listOf("1", "1", "2"), listOf("0", "1", "2")), null, null)
        val ev = Figure.withFallback(Figure.tableVariables(t)) { v, r -> Evaluator(variables = v).evaluate(com.example.cas.editor.MathCodec.copy(r)) }
        fun data(s: String) = (Figure.value(com.example.cas.engine.PlainMath.parse(s), ev) as Figure.Value.Data).v.toList()
        assertEquals(listOf(1.0, 2.0, 3.0), data("freq"))
        assertEquals(listOf(2.0, 4.0, 6.0), data("2freq"))
        assertEquals(listOf(3.0, 6.0, 9.0), data("freq + Signal"))
        assertEquals(listOf(2.0, 8.0, 18.0), data("freq*Signal"))
        assertEquals(listOf(1.0, 2.0, 6.0), data("amp_2*freq"))
        assertEquals(listOf(kotlin.math.sin(1.0), kotlin.math.sin(2.0), kotlin.math.sin(3.0)), data("sin(freq)"))
        assertEquals(4.0, (Figure.value(com.example.cas.engine.PlainMath.parse("mean(Signal)"), ev) as Figure.Value.Num).v, 1e-12)
        val w = Figure.build(doc(Figure.Layer(Kind.Scatter, listOf(com.example.cas.engine.PlainMath.parse("freq"), com.example.cas.engine.PlainMath.parse("Signal")))), ev, app).worked.single()
        assertNull(w.drawn.single().error)
        assertEquals("Noise_dB", Figure.columnName("Noise (dB)"))
        assertEquals("c2nd", Figure.columnName("2nd"))
    }

    @Test fun rowsOfColumnsErrorsAndFillsFromColumns() {
        val t = DataTable(listOf("freq", "Signal", "x"), listOf(listOf("1", "2", "3", "4"), listOf("2", "4", "6", "8"), listOf("0", "1", "2", "3")), null, null)
        val ev = Figure.withFallback(Figure.tableVariables(t)) { v, r -> Evaluator(variables = v).evaluate(com.example.cas.editor.MathCodec.copy(r)) }
        fun p(s: String) = com.example.cas.engine.PlainMath.parse(s)
        fun data(s: String) = (Figure.value(p(s), ev) as Figure.Value.Data).v.toList()
        assertEquals(listOf(4.0, 6.0), data("Signal[2:3]"))
        assertEquals(listOf(6.0, 8.0), data("Signal[-2:]"))
        assertEquals(listOf(1.0, 2.0), data("x[2:]").take(2))
        assertEquals(6.0, (Figure.value(p("Signal[3]"), ev) as Figure.Value.Num).v, 1e-12)
        assertEquals(5.0, (Figure.value(p("mean(Signal[1:4])"), ev) as Figure.Value.Num).v, 1e-12)
        assertEquals("Signal[2:3]", com.example.cas.engine.PlainMath.text(p("Signal[2:3]")))
        // Fill between two columns, against the x column.
        val w = Figure.build(doc(Figure.Layer(Kind.FillBetween, listOf(MathRow(), p("Signal+2"), p("Signal-1")))), ev, app).worked.single()
        val d = w.drawn.single()
        assertNull(d.error)
        assertEquals(listOf(0.0, 1.0, 2.0, 3.0), d.series.single().first.toList())
        // Points with ± errors in y and x; a list of lines.
        val e = Figure.build(doc(Figure.Layer(Kind.Scatter, listOf(p("freq"), p("Signal"), p("freq/10"), p("0.5"))), Figure.Layer(Kind.HLine, listOf(p("[1, 2]")))), ev, app).worked.single()
        assertEquals(4, e.drawn[0].errY.size); assertEquals(0.4, e.drawn[0].errX[3], 1e-12)
        assertEquals(2, e.drawn[1].values.size)
        // An old error bar layer opens as points with a y error.
        val old = Figure.encode(doc(Figure.Layer(Kind.Scatter, listOf(m("a"), m("b"), m("e"))))).replace("\tScatter\t", "\tErrorBar\t").replace("format\t2\n", "")
        val back = Figure.decode("t", old).axes[0].layers[0]
        assertEquals(Kind.Scatter, back.kind); assertEquals("e", back.inputs[3].plainText())
        // Axes options survive saving.
        val ax = Figure.Doc("f", "", listOf(Figure.Axes(xNumbers = false, yInvert = true, ticksIn = true, legendFrame = false)))
        val a2 = Figure.decode("f", Figure.encode(ax)).axes[0]
        assertTrue(!a2.xNumbers && a2.yNumbers && a2.yInvert && !a2.xInvert && a2.ticksIn && !a2.legendFrame)
    }

    @Test fun fieldsFromColumnsAndColumnsSideBySide() {
        // A 3 × 3 grid of points with z = x + y: heat map, contours and arrows from columns.
        val xs = listOf(0, 1, 2, 0, 1, 2, 0, 1, 2); val ys = listOf(0, 0, 0, 1, 1, 1, 2, 2, 2)
        val t = DataTable(listOf("px", "py", "height"), listOf(xs.map { "$it" }, ys.map { "$it" }, xs.indices.map { "${xs[it] + ys[it]}" }), null, null)
        val ev = Figure.withFallback(Figure.tableVariables(t)) { v, r -> Evaluator(variables = v).evaluate(com.example.cas.editor.MathCodec.copy(r)) }
        fun p(s: String) = com.example.cas.engine.PlainMath.parse(s)
        for (k in listOf(Kind.Heatmap, Kind.Contour, Kind.ContourF)) {
            val d = Figure.build(doc(Figure.Layer(k, listOf(p("px"), p("py"), p("height")))), ev, app).worked.single().drawn.single()
            assertNull("$k", d.error)
            assertEquals(3, d.grid!!.nx); assertEquals(4.0, d.grid!!.z[2][2], 1e-12)
        }
        val q = Figure.build(doc(Figure.Layer(Kind.Quiver, listOf(p("px"), p("py"), p("height"), p("1")))), ev, app).worked.single().drawn.single()
        assertNull(q.error); assertEquals(4, q.raw.size)
        // Columns next to each other multiply.
        assertEquals(listOf(0.0, 0.0, 0.0, 0.0, 1.0, 2.0, 0.0, 2.0, 4.0), (Figure.value(p("pxpy"), ev) as Figure.Value.Data).v.toList())
        assertEquals(listOf(0.0, 1.0, 4.0), (Figure.value(p("px height"), ev) as Figure.Value.Data).v.toList().take(3))
        // x first: a line through columns.
        val l = Figure.build(doc(Figure.Layer(Kind.Plot, listOf(p("px"), p("height")))), ev, app).worked.single().drawn.single()
        assertEquals(xs.map { it.toDouble() }, l.series.single().first.toList())
    }

    @Test fun bandsSheetsAndWhereThingsAre() {
        fun p(s: String) = com.example.cas.engine.PlainMath.parse(s)
        val bars = doc(Figure.Layer(Kind.Scatter, listOf(p("[1, 2, 3]"), p("[1, 4, 9]"), MathRow(), p("1"))))
        val band = doc(Figure.Layer(Kind.Scatter, listOf(p("[1, 2, 3]"), p("[1, 4, 9]"), MathRow(), p("1")), style = Figure.Style(errBand = true)))
        fun fills(d: Figure.Doc) = Figure.build(d, eval, app).scene.items.count { it is com.example.cas.graph.Scene.Fill }
        assertTrue(fills(band) > fills(bars))
        // A band for x errors too.
        val bandX = doc(Figure.Layer(Kind.Scatter, listOf(p("[1, 2, 3]"), p("[1, 4, 9]"), p("0.2")), style = Figure.Style(errBand = true)))
        assertTrue(fills(bandX) > fills(doc(Figure.Layer(Kind.Scatter, listOf(p("[1, 2, 3]"), p("[1, 4, 9]"), p("0.2"))))))
        // Tables from several files: names kept apart, every column usable.
        val a = DataTable(listOf("x", "y"), listOf(listOf("1", "2"), listOf("3", "4")), null, null)
        val b = DataTable(listOf("x", "Noise (dB)"), listOf(listOf("5", "6"), listOf("7", "8")), null, null)
        val d = Figure.addSheets(Figure.addSheets(Figure.Doc("s", "", listOf(Figure.Axes())), listOf(Figure.Sheet("one.csv", a))), listOf(Figure.Sheet("Sheet2", b)))
        assertEquals(2, d.sheets.size); assertEquals("one.csv", d.sheets[0].name)
        assertEquals(listOf("x_2", "Noise_dB"), d.sheets[1].table.names)
        assertTrue(Figure.variables(d).keys.containsAll(listOf("x", "y", "x_2", "Noise_dB")))
        val back = Figure.decode("s", Figure.encode(d))
        assertEquals(2, back.sheets.size); assertEquals("Sheet2", back.sheets[1].name); assertEquals("one.csv", back.sheetName)
        // Where a data point is drawn, and back.
        val w = Figure.build(doc(Figure.Layer(Kind.Scatter, listOf(p("[0, 10]"), p("[0, 5]")))), eval, app).worked.single()
        val (fx, fy) = w.toFigure(3.0, 2.0)!!
        val (x, y) = w.toData(fx, fy)!!
        assertEquals(3.0, x, 1e-9); assertEquals(2.0, y, 1e-9)
    }

    @Test fun axesAddedOnEachSideMovedAndRemoved() {
        val one = Figure.Doc("l", "", listOf(Figure.Axes(title = "first")))
        val (right, r) = Figure.addBeside(one, 0, Figure.Side.Right)
        assertEquals("AB", right.layout); assertEquals(1, r); assertEquals("first", right.axes[0].title)
        val (left, l) = Figure.addBeside(one, 0, Figure.Side.Left)
        assertEquals("AB", left.layout); assertEquals(0, l); assertEquals("first", left.axes[1].title)
        // On the outer edge a new row spans the whole width; inside, only beside the axes.
        val (below, b) = Figure.addBeside(right, 0, Figure.Side.Below)
        assertEquals("AB;CC", below.layout); assertEquals(2, b)
        val (inner, i) = Figure.addBeside(right.copy(axes = listOf(Figure.Axes(title = "a"), Figure.Axes(title = "b"))), 0, Figure.Side.Right)
        assertEquals("ABC", inner.layout); assertEquals(1, i); assertEquals("b", inner.axes[2].title)
        val (above, _) = Figure.addBeside(below, 2, Figure.Side.Above)
        assertEquals(3, above.layout.split(';').size)
        assertEquals(4, Figure.cells(above.layout).first.size)
        // Neighbors and moving trade contents.
        assertEquals(1, Figure.neighbor(right, 0, Figure.Side.Right)); assertNull(Figure.neighbor(right, 0, Figure.Side.Left))
        val (moved, to) = Figure.moveAxes(right, 0, Figure.Side.Right)
        assertEquals(1, to); assertEquals("first", moved.axes[1].title)
        // Removing gives the room to a neighbor and folds the grid back.
        assertEquals("A", Figure.removeAxes(right, 1).layout)
        assertEquals("first", Figure.removeAxes(right, 1).axes.single().title)
        assertEquals("AB", Figure.removeAxes(below, 2).layout)
        val grid = Figure.withLayout(Figure.Doc("g", "", listOf(Figure.Axes())), "AB;CD")
        val noB = Figure.removeAxes(grid, 1)
        assertEquals(3, Figure.cells(noB.layout).first.size)
    }

    @Test fun draggedLegendAndArrowGapAreKept() {
        fun p(s: String) = com.example.cas.engine.PlainMath.parse(s)
        val d = Figure.Doc("g", "", listOf(Figure.Axes(listOf(
            Figure.Layer(Kind.Plot, listOf(MathRow(), p("x")), style = Figure.Style(label = "line")),
            Figure.Layer(Kind.Annotate, listOf(p("0"), p("0"), p("1"), p("1")), text = "Note", style = Figure.Style(gap = 14.5)),
        ), legend = Figure.LEGEND_FREE, legendX = 0.4, legendY = 0.3)))
        val back = Figure.decode("g", Figure.encode(d))
        assertEquals(Figure.LEGEND_FREE, back.axes[0].legend)
        assertEquals(0.4, back.axes[0].legendX, 1e-12); assertEquals(0.3, back.axes[0].legendY, 1e-12)
        assertEquals(14.5, back.axes[0].layers[1].style.gap, 1e-12)
        // The legend is drawn where it was left, and its box is known (to drag it again).
        val w = Figure.build(d, eval, app).worked.single()
        assertEquals(4, w.legendBox.size)
        assertEquals(w.frame[0] + 0.4 * (w.frame[2] - w.frame[0]), w.legendBox[0], 1e-6)
        val py = FigurePython.write(d, Figure.build(d, eval, app))
        assertTrue(py.contains("bbox_to_anchor=(0.4")); assertTrue(py.contains("shrinkA=14.5"))
    }

    @Test fun ownModelWithABuiltSymbolSurvives() {
        // y = (1 − x/x₀)^(−a), with x₀ a symbol from the builder: kept exactly, and fitted.
        val x0 = Sym(com.example.cas.cas.CustomSymbol("x", sub = "0").encode())
        val base = MathRow(mutableListOf(Sym("("), Sym("1"), Sym("−"), Frac(m("x"), MathRow(mutableListOf(x0))), Sym(")"), Pow(m("−a"))))
        val formula = Figure.formulaOf(base)
        assertEquals(com.example.cas.editor.MathCodec.encode(base), com.example.cas.editor.MathCodec.encode(Figure.formulaRow(formula)))
        val model = Figure.customModel(formula)
        assertNotNull(model); assertEquals(2, model!!.params.size)
        // Old plain-text formulas still read.
        assertNotNull(Figure.customModel("a*e^(-b*x) + c"))
    }

    @Test fun ownFitModelsColorbarLabelsAndCellSizes() {
        val xs = (0..12).map { it * 0.5 }
        val t = DataTable(listOf("a", "b"), listOf(xs.map { "$it" }, xs.map { "${3 * kotlin.math.exp(-0.7 * it) + 1}" }), null, null)
        val ev = Figure.withFallback(Figure.tableVariables(t)) { v, r -> Evaluator(variables = v).evaluate(com.example.cas.editor.MathCodec.copy(r)) }
        val l = Figure.Layer(Kind.Fit, listOf(m("a"), m("b")), style = Figure.Style(model = Figure.CUSTOM_FIT, formula = "y = p*e^(-k*x) + c"))
        val d = Figure.build(doc(l), ev, app).worked.single().drawn.single()
        assertNull(d.error)
        // Parameters in alphabetical order: c, k, p.
        assertEquals(1.0, d.values[0], 1e-4); assertEquals(0.7, d.values[1], 1e-4); assertEquals(3.0, d.values[2], 1e-4)
        assertTrue(Figure.legendName(d), Figure.legendName(d).contains("R^2"))
        val styled = Figure.Doc("f", "", listOf(Figure.Axes(listOf(l.copy(style = l.style.copy(cbar = "\$T\$ (K)")))), Figure.Axes()), "AB", widths = listOf(2.0, 1.0))
        val back = Figure.decode("f", Figure.encode(styled))
        assertEquals("\$T\$ (K)", back.axes[0].layers[0].style.cbar)
        assertEquals("y = p*e^(-k*x) + c", back.axes[0].layers[0].style.formula)
        assertEquals(listOf(2.0, 1.0), back.widths)
        val boxes = Figure.cellBoxes(back, 480.0)
        assertEquals(320.0, boxes[0][2], 1e-9); assertEquals(160.0, boxes[1][2], 1e-9)
        // Another layout drops sizes that no longer fit.
        assertTrue(Figure.withLayout(back, "A;B").widths.isEmpty())
    }

    @Test fun badInputsSayWhy() {
        val d = doc(Figure.Layer(Kind.Hist, listOf(m(f("sin", "x")))), Figure.Layer(Kind.Plot, listOf(m("x+y+z"))))
        val w = Figure.build(d, eval, app).worked.single()
        assertNotNull(w.drawn[0].error); assertNotNull(w.drawn[1].error)
    }

    @Test fun mosaicsAndFiles() {
        val (cells, cols, rows) = Figure.cells("AAB;CDB")
        assertEquals(4, cells.size); assertEquals(3, cols); assertEquals(2, rows)
        assertEquals(listOf(0, 0, 2, 1), cells[0].toList())
        assertEquals(listOf(2, 0, 1, 2), cells[1].toList())
        val d = Figure.withLayout(Figure.Doc("f", "Lab", listOf(
            Figure.Axes(listOf(Figure.Layer(Kind.Plot, listOf(m(f("sin", "x"))), style = Figure.Style(color = 2, line = 1, marker = 3, label = "\$\\sin x\$"))), title = "A", xLabel = "t", xMin = 0.0, xMax = 6.0, yLog = true, legend = 3),
            Figure.Axes(listOf(Figure.Layer(Kind.Bar, listOf(m("g")), text = "a, b, c", visible = false))),
        ), "AB", theme = 3, aspect = 0.5, data = table), "AB")
        val back = Figure.decode("f", Figure.encode(d))
        assertEquals(Figure.encode(d), Figure.encode(back))
        assertEquals("AB", back.layout); assertEquals(3, back.theme); assertEquals(2, back.axes.size)
        assertEquals(6.0, back.axes[0].xMax!!, 0.0); assertTrue(back.axes[0].yLog)
        assertEquals(3, back.axes[0].layers[0].style.marker)
        assertEquals(false, back.axes[1].layers[0].visible)
        assertEquals("a, b, c", back.axes[1].layers[0].text)
        assertEquals(table.encode(), back.data.encode())
        // A bigger layout keeps the axes there are.
        assertEquals(4, Figure.withLayout(back, "AB;CD").axes.size)
        val py = FigurePython.write(back, Figure.build(back, eval, app))
        assertTrue(py.contains("plt.subplot_mosaic(\"AB\""))
        assertTrue(py.contains("ax.set_yscale(\"log\")"))
        assertTrue(py.contains("plt.style.use(\"ggplot\")"))
    }

    @Test fun matrixHeatmapAndPies() {
        val d = doc(Figure.Layer(Kind.Heatmap, listOf(m("M"))))
        val w = Figure.build(d, eval, app).worked.single()
        assertNull(w.drawn.single().error)
        val pie = Figure.build(doc(Figure.Layer(Kind.Pie, listOf(m("g")), text = "a,b")), eval, app).worked.single()
        assertTrue(pie.pie)
        assertTrue(Figure.build(doc(Figure.Layer(Kind.Pie, listOf(m("g")))), eval, app).scene.items.any { it is Scene.Fill })
    }
}
