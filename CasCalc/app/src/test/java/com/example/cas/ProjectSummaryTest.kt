package com.example.cas

import com.example.cas.graph.ProjectSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectSummaryTest {
    @Test fun longLinesAreNotDecoded() {
        val big = "(" + (1..2000).joinToString("),(") { "$it,$it" } + ")"
        val s = ProjectSummary.of(mapOf("functions" to "y=x\nNOTE\n$big", "slots" to "0,1,2", "colors" to ",,-16776961"), "NOTE")
        assertEquals(2, s.lines.size)
        assertEquals("y=x", s.lines[0].code)
        assertNull(s.lines[1].code)
        assertEquals(1, s.dataSets)
        assertEquals(2, s.lines[1].slot)
        assertEquals(-16776961, s.lines[1].color)
    }

    @Test fun emptyGraph() {
        assertEquals(0, ProjectSummary.of(emptyMap(), "NOTE").lines.size)
    }
}
