package com.example.cas

import com.example.cas.graph.Marker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkersTest {
    @Test fun everyClosedShapeHasAHollowTwin() {
        Marker.bases.filter { it.fillable }.forEach { b ->
            val open = b.filled(false)
            assertTrue(open.hollow)
            assertEquals(b, open.base)
            assertEquals(b, open.filled(true))
        }
        // Strokes stay as they are.
        assertEquals(Marker.Cross, Marker.Cross.filled(false))
    }

    @Test fun savedShapesKeepTheirPlace() {
        // Stored by position: the old ones mustn't move.
        assertEquals(Marker.Ring, Marker.of(1))
        assertEquals(Marker.OpenTriangle, Marker.of(14))
        assertEquals(Marker.Circle, Marker.Ring.base)
    }

    @Test fun hollowShapesHaveAHole() {
        Marker.entries.filter { it.hollow }.forEach { m -> assertEquals(2, m.outline(0.0, 0.0, 1.0).first.size) }
    }
}
