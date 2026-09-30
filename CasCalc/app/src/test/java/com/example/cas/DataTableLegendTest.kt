package com.example.cas

import com.example.cas.editor.Frac
import com.example.cas.editor.Func
import com.example.cas.editor.MathRow
import com.example.cas.editor.Pow
import com.example.cas.editor.Sym
import com.example.cas.editor.row
import com.example.cas.graph.Csv
import com.example.cas.graph.DataTable
import com.example.cas.graph.Legend
import com.example.cas.graph.Pgf
import com.example.cas.graph.Scene
import com.example.cas.graph.SvgWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataTableLegendTest {
    @Test fun importUsesTheFirstTwoColumnsOnly() {
        val t = DataTable.fromCsv(Csv.parse("t,v,dv,w\n0,1,0.1,5\n1,3,0.2,6\n2,5,0.1,7"))
        assertEquals(listOf(0.0 to 1.0, 1.0 to 3.0, 2.0 to 5.0), t.points())
        // The other columns are kept, unused, until picked.
        assertEquals(4, t.columns.size)
        assertEquals(null to null, t.errors())
    }

    @Test fun errorColumnsGiveErrorBars() {
        val t = DataTable.fromCsv(Csv.parse("t,v,dv\n0,1,0.1\n1,3,-0.2\n2,5,")).withRoles(0, 1, null, 2)
        val (ex, ey) = t.errors()
        assertNull(ex)
        assertEquals(0.1, ey!![0], 1e-12)
        assertEquals(0.2, ey[1], 1e-12)   // a negative σ counts as its size
    }

    @Test fun oneColumnIsPlottedAgainstTheRowNumber() {
        val t = DataTable.fromCsv(Csv.parse("10\n20\n30"))
        assertEquals(listOf(1.0 to 10.0, 2.0 to 20.0, 3.0 to 30.0), t.points())
    }

    @Test fun rowsThatArentNumbersAreLeftOutAndCounted() {
        val t = DataTable(listOf("x", "y"), listOf(listOf("1", "2", "a"), listOf("1,5", "", "3")), 0, 1)
        assertEquals(listOf(1.0 to 1.5), t.points())
        assertEquals(1, t.badCells())
    }

    @Test fun encodesAndDecodes() {
        val t = DataTable(listOf("time\t(s)", "v"), listOf(listOf("0", "1"), listOf("2", "x\\y")), 0, 1, null, 1)
        val back = DataTable.decode(t.encode())!!
        assertEquals(t.names, back.names)
        assertEquals(t.columns, back.columns)
        assertEquals(listOf(0, 1, null, 1), listOf(back.x, back.y, back.sigmaX, back.sigmaY))
    }

    @Test fun defaultNames() {
        assertEquals("Data", Legend.defaultSource(row("1"), isData = true))
        assertEquals("$\\sin\\left(x\\right)$", Legend.defaultSource(MathRow(mutableListOf(Func("sin", listOf(row("x"))))), isData = false))
    }

    @Test fun textAndMathsSegments() {
        assertEquals(listOf("Fit " to false, "a = 2" to true, " (\$)" to false), Legend.segments("Fit \$a = 2\$ (\\\$)"))
        val r = Legend.row("Data")
        assertEquals("Data", (r.items.single() as Sym).text)
    }

    @Test fun spansItaliciseLettersAndRaiseExponents() {
        val r = MathRow(mutableListOf(Sym("y"), Sym("="), Sym("x"), Pow(row("2")), Sym("+"), Func("sin", listOf(row("x")))))
        val sp = Legend.spans(r)
        assertEquals("y = x2 + sin(x)", sp.joinToString("") { it.text })
        assertTrue(sp.first { it.text == "2" }.shift == 1)
        assertTrue(sp.first { it.text.contains("sin") }.italic.not())
        assertTrue(sp.first().italic)
        val frac = Legend.spans(MathRow(mutableListOf(Frac(row("1"), MathRow(mutableListOf(Sym("x"), Sym("+"), Sym("1")))))))
        assertEquals("1/(x + 1)", frac.joinToString("") { it.text })
    }

    @Test fun legendIsInTheSvg() {
        val scene = Scene(400.0, 400.0, -1)
        Pgf.legend(scene, Pgf.frame(400.0), Pgf.Style.LIGHT, listOf(Pgf.LegendEntry(Legend.spans(Legend.row("\$x^{2}\$")), Pgf.SCIENCE[0])))
        val svg = SvgWriter.write(scene)
        assertTrue(svg.contains("#165C99"))
        assertTrue(svg.contains("font-size=\"8.65\"") || svg.contains(">2</tspan>"))
    }
}
