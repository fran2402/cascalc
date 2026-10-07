package com.example.cas

import com.example.cas.graph.Plot2D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

/** Zeros and extrema on the 2D graph: one point per zero, none from rounding noise. */
class ZerosTest {
    private fun near(expected: List<Double>, got: List<Double>) {
        assertEquals("$got", expected.size, got.size)
        expected.zip(got).forEach { (e, g) -> assertEquals(e, g, 1e-6) }
    }

    @Test fun crossingsAndTouches() {
        near(listOf(-PI, 0.0, PI), Plot2D.zeros({ sin(it) }, -4.0, 4.0, 400))
        near(listOf(0.0), Plot2D.zeros({ it * it }, -1.0, 1.0, 400))
        near(listOf(0.0), Plot2D.zeros({ it * it * it * it * it * it * it * it * it }, -10.0, 10.0, 400))
        near(listOf(0.0), Plot2D.zeros({ Math.pow(it, 10.0) }, -10.0, 10.0, 400))
        near(listOf(0.3), Plot2D.zeros({ it - 0.3 }, -1.0, 1.0, 400))
        // A pole isn't a zero.
        assertTrue(Plot2D.zeros({ 1 / it }, -1.03, 1.0, 400).isEmpty())
    }

    @Test fun roundingNoiseIsNotAZero() {
        // Zero up to rounding on the left (a cancelled difference wobbling across 0), a ramp on the right.
        val noisy = { x: Double -> if (x < 0) (sin(x * 1e3) * 1e-16) else x }
        val z = Plot2D.zeros(noisy, -5.0, 5.0, 400)
        assertTrue("$z", z.size <= 1)
        // A curve that is exactly 0 everywhere (δ terms drawn as arrows) has no marked zeros.
        assertTrue(Plot2D.zeros({ 0.0 }, -5.0, 5.0, 400).isEmpty())
        // Rounding noise around a level curve isn't a row of maxima and minima.
        val level = Plot2D.extrema({ x -> 2.0 + sin(x * 977) * 1e-15 }, -5.0, 5.0, 400)
        assertTrue("$level", level.isEmpty())
        // e^(-x²)cos(x)·δ-like spike-free part: genuine zeros of cos still found.
        near(listOf(-PI / 2, PI / 2), Plot2D.zeros({ x -> kotlin.math.exp(-x * x) * kotlin.math.cos(x) }, -2.0, 2.0, 400))
    }
}
