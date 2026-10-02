package com.example.cas

import com.example.cas.graph.AxisScale
import com.example.cas.graph.Viewport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AxisScaleTest {
    private val log = AxisScale(logX = true, logY = true)

    @Test fun valuesAndScaledCoordinates() {
        assertEquals(2.0, log.x(100.0), 1e-12)
        assertEquals(0.001, log.realY(-3.0), 1e-15)
        assertTrue(log.x(0.0).isNaN())
        assertTrue(log.y(-5.0).isNaN())
        assertEquals(-5.0, AxisScale().x(-5.0), 0.0)
    }

    @Test fun functionsAreSampledInValues() {
        // y = x² on log–log axes is a straight line of slope 2.
        val f = log.function { x -> x * x }
        assertEquals(2.0, f(1.0), 1e-12)
        assertEquals(-4.0, f(-2.0), 1e-12)
        assertEquals(4.0, f(2.0) - f(0.0), 1e-12)
    }

    @Test fun pathsBreakWhereThereIsNoPlace() {
        val line = listOf(1.0 to 1.0, 10.0 to 10.0, -1.0 to 5.0, 100.0 to 1.0, 1000.0 to 10.0)
        val out = log.paths(listOf(line))
        assertEquals(2, out.size)
        assertEquals(listOf(0.0 to 0.0, 1.0 to 1.0), out[0])
    }

    @Test fun decadeTicks() {
        val t = log.ticks(-2.0, 3.0, 6, true)
        assertEquals(listOf(-2.0, -1.0, 0.0, 1.0, 2.0, 3.0), t.major)
        assertEquals(listOf("0.01", "0.1", "1", "10", "100", "1000"), t.labels.map { it.first })
        // 2 to 9 of each decade between them.
        assertEquals(8 * 5, t.minor.size)
        // Far out: 10 to a power, every few decades.
        val wide = log.ticks(-20.0, 20.0, 5, true)
        assertEquals(listOf(-20.0, -15.0, -10.0, -5.0, 0.0, 5.0, 10.0, 15.0, 20.0), wide.major)
        assertEquals("10" to "−20", wide.labels.first())
        // Five decades on a phone (about four labels' room): still every decade.
        assertEquals(6, log.ticks(-2.0, 3.0, 4, true).major.size)
    }

    @Test fun withinADecade() {
        val t = log.ticks(0.0, 1.0, 6, true)
        assertEquals(listOf("1", "2", "5", "10"), t.labels.map { it.first })
        val close = log.ticks(kotlin.math.log10(2.0), kotlin.math.log10(2.5), 5, true)
        assertTrue(close.labels.map { it.first }.contains("2.2"))
    }

    @Test fun convertingTheView() {
        val v = Viewport(-10.0, 100.0, -5.0, 5.0)
        val c = AxisScale.convert(v, AxisScale(), AxisScale(logX = true))
        assertEquals(-1.0, c.xMin, 1e-12)
        assertEquals(2.0, c.xMax, 1e-12)
        assertEquals(-5.0, c.yMin, 0.0)
        val back = AxisScale.convert(c, AxisScale(logX = true), AxisScale())
        assertEquals(0.1, back.xMin, 1e-12)
        assertEquals(100.0, back.xMax, 1e-9)
    }

    @Test fun tidyValues() {
        assertEquals(1230.0, AxisScale().tidy(1234.5), 1e-9)
        assertEquals(0.00457, AxisScale().tidy(0.0045678), 1e-12)
    }
}
