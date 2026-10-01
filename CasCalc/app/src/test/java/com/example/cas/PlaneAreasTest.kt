package com.example.cas

import com.example.cas.cas.CD
import com.example.cas.graph.PlaneAreas
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaneAreasTest {
    private fun path(n: Int, t0: Double, t1: Double, z: (Double) -> CD) = (0..n).map { z(t0 + (t1 - t0) * it / n) }

    @Test fun circleInside() = assertEquals(Math.PI, PlaneAreas.shoelace(path(4000, 0.0, 2 * Math.PI) { CD(Math.cos(it), Math.sin(it)) }.dropLast(1)), 1e-5)
    @Test fun parabolaToAxis() {
        val (signed, total) = PlaneAreas.toAxis(path(2000, 0.0, 1.0) { CD(it, it * it) })
        assertEquals(1.0 / 3, signed, 1e-6); assertEquals(1.0 / 3, total, 1e-6)
    }
    @Test fun axisCrossingCountsBothSides() {
        val (signed, total) = PlaneAreas.toAxis(path(4000, -1.0, 1.0) { CD(it, it) })
        assertEquals(0.0, signed, 1e-9); assertEquals(1.0, total, 1e-6)
    }
    @Test fun betweenParabolaAndLine() {
        val a = path(2000, -0.5, 1.5) { CD(it, it * it) }
        val b = path(2000, -0.5, 1.5) { CD(it, it) }
        val region = PlaneAreas.between(a, b, CD(0.0, 0.0))!!
        assertEquals(1.0 / 6, kotlin.math.abs(PlaneAreas.shoelace(region)), 1e-5)
    }
}
