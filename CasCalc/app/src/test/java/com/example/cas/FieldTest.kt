package com.example.cas

import com.example.cas.graph.Colormap
import com.example.cas.graph.Field
import com.example.cas.graph.Viewport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldTest {
    private val view = Viewport(-2.0, 2.0, -2.0, 2.0)

    @Test fun samplesCellCentresTopRowFirst() {
        val v = Field.sample({ x, y -> x + 10 * y }, view, 4, 4)
        // Top-left cell center is (−1.5, 1.5).
        assertEquals(-1.5 + 15, v[0], 1e-12)
        // Bottom-right is (1.5, −1.5).
        assertEquals(1.5 - 15, v[15], 1e-12)
    }

    @Test fun rangeIgnoresSpikes() {
        val values = DoubleArray(1000) { it / 1000.0 } + doubleArrayOf(1e9, Double.NaN)
        val (lo, hi) = Field.range(values)
        assertTrue(lo < 0.05 && hi < 1.01)
        // A flat field still gets a usable scale.
        val (a, b) = Field.range(DoubleArray(10) { 3.0 })
        assertTrue(b > a)
    }

    @Test fun coloursFollowTheMapAndSkipUndefined() {
        val px = Field.colors(doubleArrayOf(0.0, 1.0, Double.NaN), 0.0, 1.0, Colormap.VIRIDIS, reversed = false)
        assertEquals(Colormap.VIRIDIS.rgb(0.0) and 0xFFFFFF, px[0] and 0xFFFFFF)
        assertEquals(Colormap.VIRIDIS.rgb(1.0) and 0xFFFFFF, px[1] and 0xFFFFFF)
        assertEquals(0, px[2])
        val rev = Field.colors(doubleArrayOf(0.0), 0.0, 1.0, Colormap.VIRIDIS, reversed = true)
        assertEquals(Colormap.VIRIDIS.rgb(1.0) and 0xFFFFFF, rev[0] and 0xFFFFFF)
    }
}
