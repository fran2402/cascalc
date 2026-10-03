package com.example.cas

import com.example.cas.graph.DataTable
import com.example.cas.graph.Sheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SheetTest {
    private fun book(vararg cols: List<String>) = Sheet.Book(cols.toList())
    private fun v(b: Sheet.Book, c: Int, r: Int) = b.value(c, r).number!!
    private fun err(b: Sheet.Book, c: Int, r: Int) = b.value(c, r).error

    @Test fun columnNames() {
        assertEquals("A", Sheet.columnName(0)); assertEquals("Z", Sheet.columnName(25))
        assertEquals("AA", Sheet.columnName(26)); assertEquals(27, Sheet.columnIndex("AB"))
    }

    @Test fun referencesAndArithmetic() {
        val b = book(listOf("1", "2", "3"), listOf("=A1*2", "=A2+B1^2", "=-A3%"))
        assertEquals(2.0, v(b, 1, 0), 0.0)
        assertEquals(6.0, v(b, 1, 1), 0.0)
        assertEquals(-0.03, v(b, 1, 2), 1e-12)
    }

    @Test fun rangesAndStatistics() {
        val b = book(listOf("1", "2", "3", "4", "", "text"), listOf("=SUM(A1:A6)", "=AVERAGE(A:A)", "=COUNT(A:A)", "=COUNTA(A:A)", "=MEDIAN(A1:A4)", "=STDEV(A1:A4)", "=MAX(A:A)-MIN(A:A)"))
        assertEquals(10.0, v(b, 1, 0), 0.0)
        assertEquals(2.5, v(b, 1, 1), 0.0)
        assertEquals(4.0, v(b, 1, 2), 0.0)
        assertEquals(5.0, v(b, 1, 3), 0.0)
        assertEquals(2.5, v(b, 1, 4), 0.0)
        assertEquals(1.2909944487, v(b, 1, 5), 1e-9)
        assertEquals(3.0, v(b, 1, 6), 0.0)
    }

    @Test fun conditionsAndLogic() {
        val b = book(listOf("1", "5", "7", "9"), listOf("=COUNTIF(A:A, \">5\")", "=SUMIF(A1:A4, \">=5\")", "=IF(A1>0, 10, 20)", "=AND(A1>0, A2<3)", "=IFERROR(1/0, 42)", "=AVERAGEIF(A:A, \"<>5\")", "=IF(A1>0, 3, 1/0)", "=IFERROR(SQRT(-1), -1)"))
        assertEquals(2.0, v(b, 1, 0), 0.0)
        assertEquals(21.0, v(b, 1, 1), 0.0)
        assertEquals(10.0, v(b, 1, 2), 0.0)
        assertEquals(0.0, v(b, 1, 3), 0.0)
        assertEquals(42.0, v(b, 1, 4), 0.0)
        assertEquals(17.0 / 3, v(b, 1, 5), 1e-12)
        assertEquals(3.0, v(b, 1, 6), 0.0)
        assertEquals(-1.0, v(b, 1, 7), 0.0)
    }

    @Test fun regression() {
        val b = book(listOf("1", "2", "3", "4"), listOf("3", "5", "7", "9"), listOf("=SLOPE(B:B, A:A)", "=INTERCEPT(B1:B4, A1:A4)", "=RSQ(B:B, A:A)", "=SUMPRODUCT(A1:A4, B1:B4)"))
        assertEquals(2.0, v(b, 2, 0), 1e-12)
        assertEquals(1.0, v(b, 2, 1), 1e-12)
        assertEquals(1.0, v(b, 2, 2), 1e-12)
        assertEquals(70.0, v(b, 2, 3), 0.0)
    }

    @Test fun functions() {
        val b = book(listOf("=ROUND(2.345, 2)", "=MOD(-7, 3)", "=LOG(8, 2)", "=LOG10(1000)", "=FACT(5)", "=COMBIN(10, 3)", "=SQRT(3^2+4^2)", "=ROW()", "=DEGREES(PI())", "=ATAN2(1, 1)"))
        val want = listOf(2.35, 2.0, 3.0, 3.0, 120.0, 120.0, 5.0, 8.0, 180.0, Math.PI / 4)
        want.forEachIndexed { r, w -> assertEquals("row $r", w, v(b, 0, r), 1e-12) }
    }

    @Test fun errors() {
        val b = book(listOf("=1/0", "=FOO(1)", "=SQRT(-1)", "=A4", "=A5+1", "=A1+1", "=\"a\"+1", "=AVERAGE(B:B)"))
        assertEquals("#DIV/0!", err(b, 0, 0))
        assertEquals("#NAME?", err(b, 0, 1))
        assertEquals("#NUM!", err(b, 0, 2))
        assertEquals("#CYCLE!", err(b, 0, 3))
        assertEquals("#CYCLE!", err(b, 0, 4))
        assertEquals("#DIV/0!", err(b, 0, 5))
        assertEquals("#VALUE!", err(b, 0, 6))
        assertEquals("#DIV/0!", err(b, 0, 7))
    }

    @Test fun absoluteReferences() {
        val b = book(listOf("2", "3"), listOf("=A1*\$A\$1", "=A2*\$A\$1"))
        assertEquals(4.0, v(b, 1, 0), 0.0)
        assertEquals(6.0, v(b, 1, 1), 0.0)
    }

    @Test fun fillDown() {
        assertEquals("=A2*\$A\$1+B\$1", Sheet.shift("=A1*\$A\$1+B\$1", 1))
        assertEquals("=SUM(A3:A12)", Sheet.shift("=SUM(A1:A10)", 2))
        assertEquals("=LOG10(B5)", Sheet.shift("=LOG10(B4)", 1))
        assertEquals("=COUNTIF(A:A, \"A1\")+C2", Sheet.shift("=COUNTIF(A:A, \"A1\")+B1", 1, 1))
    }

    @Test fun dataTableUsesFormulas() {
        val t = DataTable(listOf("x", "y"), listOf(listOf("1", "2", "3"), listOf("=A1^2", "=A2^2", "=1/0")), x = 0, y = 1)
        assertEquals(listOf(1.0 to 1.0, 2.0 to 4.0), t.points())
        assertEquals(1, t.badCells())
        assertNull(t.value(1, 2))
    }

    @Test fun referencesInAFormula() {
        val r = Sheet.references("=SUM(A1:B3)+\$C\$2*D:D+\"E5\"")
        assertEquals(3, r.size)
        assertEquals(listOf(0, 0, 1, 2), listOf(r[0].c0, r[0].r0, r[0].c1, r[0].r1))
        assertEquals(listOf(2, 1), listOf(r[1].c0, r[1].r0))
        assertEquals(3, r[2].c0); assertEquals(Int.MAX_VALUE, r[2].r1)
        assertEquals("A1:B3", "=SUM(A1:B3)+".substring(r[0].at.first, r[0].at.last + 1))
    }

    @Test fun suggestionsWhileTyping() {
        assertEquals("SU", Sheet.typingName("=SU"))
        assertEquals("AV", Sheet.typingName("=1+AV"))
        assertNull(Sheet.typingName("=SUM(A1"))
        assertNull(Sheet.typingName("12"))
        assertEquals("SUM", Sheet.suggestions("SU").first())
        assert(Sheet.suggestions("VLO").contains("VLOOKUP"))
        assert(Sheet.NAMES.size > 350)
    }
}
