package com.example.cas

import com.example.cas.graph.ColumnFormat
import com.example.cas.graph.DataTable
import com.example.cas.graph.SheetTools
import com.example.cas.graph.SheetTools.FilterOp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The data table's spreadsheet tools: formats, find and replace, filters, tidying, smart fill, summaries. */
class SheetToolsTest {
    private fun table(vararg cols: List<String>) = DataTable(List(cols.size) { "" }, cols.toList(), 0, 1)

    @Test fun numberFormats() {
        assertEquals("3.14", ColumnFormat(decimals = 2).show(3.14159))
        assertEquals("3", ColumnFormat(decimals = 0).show(2.5))
        assertEquals("12.5%", ColumnFormat(percent = true).show(0.125))
        assertEquals("12.50%", ColumnFormat(percent = true, decimals = 2).show(0.125))
        assertEquals("1.23 × 10⁴", ColumnFormat(scientific = true).show(12345.0))
        assertEquals("1.00 × 10¹", ColumnFormat(scientific = true).show(9.999))
        assertEquals("−4.50 × 10⁻³", ColumnFormat(scientific = true).show(-0.0045))
        assertEquals("1 234 567.80", ColumnFormat(decimals = 2, thousands = true).show(1234567.8))
        assertEquals("−12 000", ColumnFormat(thousands = true).show(-12000.0))
        assertEquals("1234", ColumnFormat(thousands = true).show(1234.0))
    }

    @Test fun formatsAndFreezeAreSaved() {
        val t = DataTable(listOf("a", "b"), listOf(listOf("1"), listOf("2")), 0, 1, formats = listOf(ColumnFormat(decimals = 3, colorScale = true), ColumnFormat(percent = true)), frozen = true)
        val back = DataTable.decode(t.encode())!!
        assertEquals(t.formats, back.formats)
        assertTrue(back.frozen)
        assertEquals(1, back.y)
        // A table saved before formats existed still reads.
        val old = DataTable.decode("0\t1\t-1\t-1\na\tb\n1\n2")!!
        assertTrue(old.formats.isEmpty()); assertFalse(old.frozen); assertEquals(0, old.x)
    }

    @Test fun findAndReplace() {
        val cells = listOf(listOf("apple", "Pineapple", "pear"), listOf("1", "apple", "2"))
        assertEquals(listOf(0 to 0, 0 to 1, 1 to 1), SheetTools.find(cells, "apple"))
        assertEquals(listOf(0 to 0, 1 to 1), SheetTools.find(cells, "apple", wholeCell = true))
        assertEquals(listOf(0 to 0, 1 to 1), SheetTools.find(cells, "apple", matchCase = true).filter { (c, r) -> cells[c][r].startsWith("a") })
        assertEquals("Pinepear", SheetTools.replace("Pineapple", "apple", "pear"))
        assertEquals("Pineapple", SheetTools.replace("Pineapple", "apple", "pear", wholeCell = true))
        assertEquals("x x", SheetTools.replace("A a", "a", "x"))
        assertEquals("x a", SheetTools.replace("A a", "A", "x", matchCase = true))
    }

    @Test fun filters() {
        val t = table(listOf("1", "5", "", "10", "abc"), listOf("a", "b", "c", "d", "e"))
        assertEquals(listOf(1, 3), SheetTools.visibleRows(t, listOf(SheetTools.Filter(0, FilterOp.Greater, "2"))))
        assertEquals(listOf(2), SheetTools.visibleRows(t, listOf(SheetTools.Filter(0, FilterOp.Empty))))
        assertEquals(listOf(4), SheetTools.visibleRows(t, listOf(SheetTools.Filter(0, FilterOp.Contains, "B"))))
        assertEquals(listOf(1), SheetTools.visibleRows(t, listOf(SheetTools.Filter(0, FilterOp.Equals, "5.0"))))
        // Every filter must hold.
        assertEquals(listOf(3), SheetTools.visibleRows(t, listOf(SheetTools.Filter(0, FilterOp.Greater, "2"), SheetTools.Filter(1, FilterOp.NotEquals, "b"))))
    }

    @Test fun tidying() {
        val cells = listOf(listOf("1", "2", "1", "1 "), listOf("a", "b", "a", "a"))
        assertEquals(listOf(0, 1), SheetTools.withoutDuplicates(cells))
        assertEquals("a b", SheetTools.trimmed("  a   b "))
        assertEquals("=A1  +1", SheetTools.trimmed("=A1  +1"))
        // Without names: each row becomes a column.
        val (n1, c1) = SheetTools.transpose(listOf("", ""), listOf(listOf("1", "2", "3"), listOf("4", "5", "6")))
        assertEquals(listOf(listOf("1", "4"), listOf("2", "5"), listOf("3", "6")), c1)
        assertEquals(3, n1.size)
        // With names: the names become the first column and the first column the names.
        val (n2, c2) = SheetTools.transpose(listOf("t", "v"), listOf(listOf("0", "1"), listOf("5", "7")))
        assertEquals(listOf("t", "0", "1"), n2)
        assertEquals(listOf(listOf("v"), listOf("5"), listOf("7")), c2)
    }

    @Test fun smartFill() {
        assertEquals("Week 3", SheetTools.fillValue("Week 1", 2))
        assertEquals("Q10", SheetTools.fillValue("Q09", 1))
        assertEquals("Item-07", SheetTools.fillValue("Item-08", -1))
        assertEquals("Mar", SheetTools.fillValue("Jan", 2))
        assertEquals("January", SheetTools.fillValue("December", 1))
        assertEquals("SUN", SheetTools.fillValue("MON", -1))
        assertEquals("tuesday", SheetTools.fillValue("monday", 1))
        // Plain numbers and other text are copied, as Excel fills one cell.
        assertEquals("5", SheetTools.fillValue("5", 3))
        assertEquals("abc", SheetTools.fillValue("abc", 3))
    }

    @Test fun rangeSummaryAndCopy() {
        val t = table(listOf("1", "2", "x"), listOf("=A1*10", "", "4"))
        val s = SheetTools.summary(t, 0, 0, 1, 2)
        assertEquals(17.0, s.sum, 1e-12)
        assertEquals(4, s.numbers)
        assertEquals(5, s.filled)
        assertEquals(4.25, s.average!!, 1e-12)
        assertEquals(1.0, s.min!!, 0.0); assertEquals(10.0, s.max!!, 0.0)
        assertEquals("1\t10\n2\t", SheetTools.rangeText(t, 0, 0, 1, 1))
    }

    @Test fun colorScale() {
        assertEquals(0xFFF8696B.toInt(), SheetTools.scaleColor(0f))
        assertEquals(0xFFFFEB84.toInt(), SheetTools.scaleColor(0.5f))
        assertEquals(0xFF63BE7B.toInt(), SheetTools.scaleColor(1f))
        assertEquals(0.25f, SheetTools.scalePosition(2.0, 1.0, 5.0), 1e-6f)
    }
}
