package com.example.cas

import com.example.cas.editor.Sym
import com.example.cas.editor.row
import com.example.cas.engine.Evaluator
import com.example.cas.graph.Compiler
import com.example.cas.graph.Coordinates3D
import com.example.cas.graph.Coordinates3D.Mode
import com.example.cas.graph.PlotSpec3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

class Coordinates3DTest {
    private fun spec(text: String, mode: Mode) = Coordinates3D.classify(Evaluator().evaluate(row(*text.map { Sym(it.toString()) }.toTypedArray())), mode)
    private fun implicitAt(s: PlotSpec3D, x: Double, y: Double, z: Double): Double {
        assertTrue("implicit expected", s is PlotSpec3D.Implicit)
        return Compiler.compile((s as PlotSpec3D.Implicit).f, listOf("x", "y", "z"))(doubleArrayOf(x, y, z))
    }

    @Test fun sphere() {
        val s = spec("ρ=2", Mode.Cartesian)
        assertEquals(0.0, implicitAt(s, 2.0, 0.0, 0.0), 1e-12)
        assertEquals(0.0, implicitAt(s, 0.0, 1.2, 1.6), 1e-12)
        assertTrue(abs(implicitAt(s, 1.0, 0.0, 0.0)) > 0.5)
    }

    @Test fun bareNumberFollowsTheMode() {
        // "2": a plane z = 2, a cylinder r = 2, a sphere ρ = 2.
        assertTrue(spec("2", Mode.Cartesian) is PlotSpec3D.Explicit)
        assertEquals(0.0, implicitAt(spec("2", Mode.Cylindrical), 0.0, 2.0, 5.0), 1e-12)
        assertEquals(0.0, implicitAt(spec("2", Mode.Spherical), 0.0, 0.0, -2.0), 1e-12)
    }

    @Test fun coneFromPhi() {
        val s = spec("φ=1", Mode.Spherical)
        // φ = 1 rad: points at angle 1 from the z-axis.
        val z = 1.0; val rho = z / kotlin.math.cos(1.0); val x = rho * kotlin.math.sin(1.0)
        assertEquals(0.0, implicitAt(s, x, 0.0, z), 1e-9)
    }

    @Test fun thetaGoesRoundTheZAxis() {
        val s = spec("θ=1", Mode.Cylindrical)
        assertEquals(0.0, implicitAt(s, kotlin.math.cos(1.0), kotlin.math.sin(1.0), 3.0), 1e-9)
    }

    @Test fun surfaceOverThePlaneStaysExplicit() {
        // z = r² is a paraboloid, drawn as a surface over x and y.
        val s = spec("z=r²".replace("²", "") + "", Mode.Cartesian)
        assertTrue(s is PlotSpec3D.Explicit)
        val f = Compiler.compile((s as PlotSpec3D.Explicit).f, listOf("x", "y"))
        assertEquals(sqrt(2.0), f(doubleArrayOf(1.0, 1.0)), 1e-12)
    }
}
