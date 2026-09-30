package com.example.cas

import com.example.cas.engine.Evaluator
import com.example.cas.engine.LatexParser
import com.example.cas.graph.Bounds
import com.example.cas.graph.Coordinates3D
import com.example.cas.graph.Coordinates3D.Mode
import com.example.cas.graph.PlotSpec3D
import com.example.cas.graph.Stl
import com.example.cas.graph.Surface3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Graph3DExtrasTest {
    private fun spec(latex: String, mode: Mode = Mode.Cartesian) = Coordinates3D.classify(Evaluator().evaluate(LatexParser.parse(latex)), mode)

    @Test fun inequalitiesAreSolids() {
        val s = spec("x^{2}+y^{2}+z^{2}\\le 4")
        assertTrue(s is PlotSpec3D.Region)
        assertTrue(!(s as PlotSpec3D.Region).strict)
        assertTrue(spec("z<x^{2}+y^{2}") is PlotSpec3D.Region)
        // Spherical letters in an inequality: ρ < 2 is the ball.
        assertTrue(spec("\\rho<2") is PlotSpec3D.Region)
    }

    @Test fun aBallInsideTheBoxHasNoWalls() {
        val b = Bounds.square(3.0)
        val ball = Surface3D.solid({ x, y, z -> x * x + y * y + z * z - 4 }, b, 16)
        assertTrue(ball.isNotEmpty())
        assertTrue(ball.none { it.wall })
    }

    @Test fun aSolidCutByTheBoxGetsWalls() {
        // Below z = 1 − x² − y² (down to the floor): the floor and the sides are walls.
        val b = Bounds.square(1.0)
        val solid = Surface3D.solid({ x, y, z -> z - (1 - x * x - y * y) }, b, 12)
        assertTrue(solid.any { it.wall })
        assertTrue(solid.filter { it.wall }.all { p -> p.points.all { q -> q.any { v -> kotlin.math.abs(kotlin.math.abs(v) - 1.0) < 1e-9 } } })
    }

    @Test fun stlHasOneTriangleOrMorePerPolygon() {
        val b = Bounds.square(3.0)
        val ball = Surface3D.solid({ x, y, z -> x * x + y * y + z * z - 4 }, b, 10)
        val bytes = Stl.write(ball, b)
        val n = Stl.triangleCount(bytes)
        assertTrue(n >= ball.size)
        assertEquals(84 + 50 * n, bytes.size)
    }

    @Test fun ownLettersAreRenamed() {
        val map = Coordinates3D.renaming(mapOf(Mode.Cartesian to listOf("i", "j", "k"), Mode.Cylindrical to listOf("r", "θ", "k"), Mode.Spherical to listOf("ρ", "θ", "φ")))
        assertEquals(mapOf("i" to "x", "j" to "y", "k" to "z"), map)
        // Unchanged letters need nothing.
        assertEquals(emptyMap<String, String>(), Coordinates3D.renaming(Coordinates3D.DEFAULT_LETTERS))
    }
}
