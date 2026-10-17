package com.example.cas

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
            Kind.ErrorBar to listOf(m("g"), m("w"), m("2")), Kind.FillBetween to listOf(m(f("sin", "x")), m(f("cos", "x"))), Kind.Stack to listOf(m("g,g")),
            Kind.Parametric to listOf(m(f("cos", "t")), m(f("sin", "t"))), Kind.Polar to listOf(m("1+", f("cos", "θ"))), Kind.Fill to listOf(m("g"), m("w")),
            Kind.Hist to listOf(m("h")), Kind.Box to listOf(m("h,w")), Kind.Violin to listOf(m("h,w")), Kind.Ecdf to listOf(m("h")),
            Kind.Hist2D to listOf(m("h"), m("w")), Kind.Hexbin to listOf(m("h"), m("w")), Kind.Bar to listOf(m("g")), Kind.BarH to listOf(m("g")),
            Kind.Pie to listOf(m("g")), Kind.Events to listOf(m("h,w")), Kind.Contour to listOf(m("x", Pow(m("2")), "+y", Pow(m("2")))),
            Kind.ContourF to listOf(m("x", Pow(m("2")), "−y")), Kind.Heatmap to listOf(m(f("sin", "xy"))), Kind.Quiver to listOf(m("−y"), m("x")),
            Kind.Stream to listOf(m("−y"), m("x")), Kind.HLine to listOf(m("1")), Kind.VLine to listOf(m("2")), Kind.HSpan to listOf(m("0"), m("1")),
            Kind.VSpan to listOf(m("0"), m("1")), Kind.Text to listOf(m("0"), m("0")), Kind.Annotate to listOf(m("0"), m("0"), m("1"), m("1")),
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

    @Test fun guideExamplesDraw() {
        for (e in com.example.cas.ui.ModeGuides.plots.examples) {
            val w = Figure.build(doc(Figure.Layer(Kind.Plot, listOf(e.row))), eval, app).worked.single()
            assertNull(e.label, w.drawn.single().error)
        }
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
