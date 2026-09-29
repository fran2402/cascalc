package com.example.cas

import com.example.cas.graph.Curves
import com.example.cas.graph.Viewport
import org.junit.Assert.assertTrue
import org.junit.Test

/** The adaptive shading of inequalities gives the same picture as testing every cell, with far fewer tests. */
class RegionTest {
    private val view = Viewport(-10.0, 10.0, -16.0, 16.0)

    private fun compare(name: String, test: (Double, Double) -> Boolean) {
        var calls = 0
        val counted = { x: Double, y: Double -> calls++; test(x, y) }
        val full = Curves.region(test, view, 360, 480)
        val fast = Curves.regionAdaptive(counted, view, 360, 480)
        val differ = full.indices.count { full[it] != fast[it] }
        assertTrue("$name: $differ cells differ", differ == 0)
        assertTrue("$name: $calls tests", calls < full.size / 3)
    }

    @Test fun disk() = compare("disk") { x, y -> x * x + y * y < 20 }
    @Test fun halfPlane() = compare("half plane") { x, y -> y > 2 * x - 1 }
    @Test fun ring() = compare("ring") { x, y -> x * x + y * y in 9.0..36.0 }
    @Test fun underSine() = compare("under sine") { x, y -> y < 3 * kotlin.math.sin(x) }
    @Test fun empty() = compare("empty") { _, _ -> false }
}
