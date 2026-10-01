package com.example.cas

import com.example.cas.graph.VectorField
import com.example.cas.graph.Viewport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class VectorFieldTest {
    private val view = Viewport(-5.0, 5.0, -5.0, 5.0)
    private val rotation = { x: Double, y: Double -> -y to x }

    @Test fun arrowsOnAGridCenteredOnTheirPoints() {
        val a = VectorField.arrows(rotation, view, 1000.0, 1000.0, 10, VectorField.Length.Scaled, 1.0)
        assertEquals(100, a.size)
        // Each arrow is centered on its cell: (−4.5, −4.5), …
        val first = a.first()
        assertEquals(-4.5, (first.x0 + first.x1) / 2, 1e-12)
        assertEquals(-4.5, (first.y0 + first.y1) / 2, 1e-12)
        // Perpendicular to the position: a rotation.
        for (r in a) {
            val cx = (r.x0 + r.x1) / 2; val cy = (r.y0 + r.y1) / 2
            assertEquals(0.0, cx * (r.x1 - r.x0) + cy * (r.y1 - r.y0), 1e-9)
        }
        // The longest fills 86% of its cell (1 unit here).
        val longest = a.maxOf { hypot(it.x1 - it.x0, it.y1 - it.y0) }
        assertEquals(0.86, longest, 1e-9)
    }

    @Test fun sameAndTrueLengths() {
        val equal = VectorField.arrows(rotation, view, 1000.0, 1000.0, 10, VectorField.Length.Equal, 0.5)
        equal.forEach { assertEquals(0.43, hypot(it.x1 - it.x0, it.y1 - it.y0), 1e-9) }
        val exact = VectorField.arrows({ _, _ -> 2.0 to 0.0 }, view, 1000.0, 1000.0, 10, VectorField.Length.True, 1.5)
        exact.forEach { assertEquals(3.0, it.x1 - it.x0, 1e-12); assertEquals(0.0, it.y1 - it.y0, 1e-12) }
    }

    @Test fun aSingularityDoesNotShrinkTheRest() {
        // 1/r² near the origin: the scale comes from the 95th percentile, so most arrows stay visible.
        val a = VectorField.arrows({ x, y -> val r2 = x * x + y * y; x / (r2 * r2) to y / (r2 * r2) }, view, 1000.0, 1000.0, 20, VectorField.Length.Scaled, 1.0)
        val lengths = a.map { hypot(it.x1 - it.x0, it.y1 - it.y0) }.sorted()
        assertTrue(lengths[lengths.size / 2] > 0.01)
        assertTrue(lengths.last() <= 0.86 * 0.5 + 1e-9)
    }

    @Test fun undefinedPointsAreLeftOut() {
        val a = VectorField.arrows({ x, _ -> if (x < 0) null else 1.0 to 1.0 }, view, 1000.0, 1000.0, 10, VectorField.Length.Scaled, 1.0)
        assertEquals(50, a.size)
    }

    @Test fun headsPointAlongTheArrow() {
        val tri = VectorField.head(VectorField.Tip.Triangle, 10.0, 0.0, 1.0, 0.0, 4.0)
        assertEquals(10.0, tri.fills[0][0], 0.0)
        assertTrue(tri.shaftEndX < 10.0)
        val open = VectorField.head(VectorField.Tip.Open, 10.0, 0.0, 1.0, 0.0, 4.0)
        assertEquals(2, open.strokes.size); assertTrue(open.fills.isEmpty())
        assertEquals(4, VectorField.head(VectorField.Tip.Stealth, 0.0, 0.0, 0.0, 1.0, 4.0).fills[0].size / 2)
        assertTrue(VectorField.head(VectorField.Tip.None, 0.0, 0.0, 1.0, 0.0, 4.0).let { it.fills.isEmpty() && it.strokes.isEmpty() })
    }

    @Test fun colorPositionFollowsTheMagnitude() {
        assertEquals(0.5, VectorField.position(2.0, 1.0 to 3.0), 1e-12)
        assertEquals(1.0, VectorField.position(9.0, 1.0 to 3.0), 0.0)
    }
}
