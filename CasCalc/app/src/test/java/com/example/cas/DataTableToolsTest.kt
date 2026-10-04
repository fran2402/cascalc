package com.example.cas

import com.example.cas.graph.DataTable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DataTableToolsTest {
    private val t = DataTable(listOf("t", "h, m"), listOf(listOf("1", "2", "3", "4"), listOf("2", "x", "=A3*2", "10")), 0, 1)

    @Test fun columnStatistics() {
        val s = t.stats(1)!!
        // 2, 6 (the formula) and 10; the word is left out.
        assertEquals(3, s.n); assertEquals(18.0, s.sum, 1e-12); assertEquals(6.0, s.mean, 1e-12)
        assertEquals(6.0, s.median, 1e-12); assertEquals(4.0, s.sd!!, 1e-12)
        assertEquals(2.0, s.min, 0.0); assertEquals(10.0, s.max, 0.0)
        assertNull(DataTable.stats(emptyList()))
        assertNull(DataTable.stats(listOf(5.0))!!.sd)
    }

    @Test fun csvWithNamesQuotesAndValues() {
        assertEquals("t,\"h, m\"\n1,2\n2,x\n3,6\n4,10\n", t.csv())
    }

    @Test fun seriesAndMoves() {
        assertEquals(listOf("0", "0.1", "0.2", "0.3"), DataTable.series(0.0, 0.1, 4))
        assertEquals(listOf(1, 0, 2), DataTable.moved(3, 0, 1))
        assertEquals(listOf(0, 2, 1), DataTable.moved(3, 2, 1))
    }
}
