package com.example.cas

import com.example.cas.graph.Csv
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvTest {
    @Test fun commaWithHeader() {
        val t = Csv.parse("time,height\n0,1.5\n1,2.25\n2,3\n")
        assertEquals(listOf("time", "height"), t.names)
        assertArrayEquals(doubleArrayOf(0.0, 1.0, 2.0), t.columns[0], 0.0)
        assertArrayEquals(doubleArrayOf(1.5, 2.25, 3.0), t.columns[1], 0.0)
    }

    @Test fun semicolonWithDecimalComma() {
        val t = Csv.parse("x;y\n1,5;−2\n2,5;4,75\n")
        assertArrayEquals(doubleArrayOf(1.5, 2.5), t.columns[0], 0.0)
        assertArrayEquals(doubleArrayOf(-2.0, 4.75), t.columns[1], 0.0)
    }

    @Test fun tabsSpacesQuotesAndJunk() {
        assertEquals(3, Csv.parse("1\t2\n3\t4\n5\t6").rows)
        assertEquals(2, Csv.parse("1  2\n  3 4 \n").rows)
        val q = Csv.parse("\"a\",\"b\"\n\"1\",\"2\"\n# comment\nn/a,3\n4,5\n")
        assertArrayEquals(doubleArrayOf(1.0, 4.0), q.columns[0], 0.0)
    }

    @Test fun oneColumnIsPlottedAgainstItsIndex() {
        val lists = Csv.pointLists(Csv.parse("10\n20\n30"))
        assertEquals(listOf(1.0 to 10.0, 2.0 to 20.0, 3.0 to 30.0), lists.single().second)
    }

    @Test fun eachExtraColumnIsItsOwnList() {
        val lists = Csv.pointLists(Csv.parse("x,a,b\n0,1,2\n1,3,4"))
        assertEquals(listOf("a", "b"), lists.map { it.first })
        assertEquals(listOf(0.0 to 2.0, 1.0 to 4.0), lists[1].second)
    }

    @Test fun numbersAsTyped() {
        assertEquals("1.5", Csv.numberText(1.5))
        assertEquals("−0.000123", Csv.numberText(-0.000123))
        assertEquals("1234567", Csv.numberText(1234567.0))
        assertEquals("0.1", Csv.numberText(0.1))
    }
}
