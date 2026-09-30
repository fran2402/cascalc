package com.example.cas

import com.example.cas.cas.Expr
import com.example.cas.cas.Printer
import com.example.cas.editor.Func
import com.example.cas.editor.MathRow
import com.example.cas.editor.Pow
import com.example.cas.editor.Sym
import com.example.cas.editor.row
import com.example.cas.engine.Evaluator
import com.example.cas.graph.Camera
import com.example.cas.graph.Compiler
import com.example.cas.graph.Plot2D
import com.example.cas.graph.Surface3D
import com.example.cas.graph.Viewport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GraphTest {
    private fun expr(r: MathRow): Expr = Evaluator().evaluate(r)
    private fun f1(r: MathRow): (Double) -> Double {
        val c = Compiler.compile(expr(r), listOf("x"))
        return { x -> c(doubleArrayOf(x)) }
    }
    private fun near(a: Double, b: Double) = assertTrue("$a vs $b", abs(a - b) < 1e-6)

    @Test fun compilesPolynomial() = near(f1(row(Sym("x"), Pow(row("2")), Sym("+"), Sym("1")))(3.0), 10.0)
    @Test fun compilesTrig() = near(f1(row(Func("sin", listOf(row("x")))))(Math.PI / 2), 1.0)
    @Test fun oddRootOfNegative() = near(f1(row(Sym("x"), Pow(row(com.example.cas.editor.Frac(row("1"), row("3"))))))(-8.0), -2.0)
    @Test fun undefinedIsNaN() = assertTrue(f1(row(Func("ln", listOf(row("x")))))(-1.0).isNaN())
    @Test fun twoVariables() {
        val c = Compiler.compile(expr(row(Sym("x"), Sym("y"))), listOf("x", "y"))
        near(c(doubleArrayOf(3.0, 4.0)), 12.0)
    }
    @Test fun parameter() {
        val c = Compiler.compile(expr(row(Sym("a"), Sym("x"))), listOf("x", "a"))
        near(c(doubleArrayOf(2.0, 5.0)), 10.0)
    }

    @Test fun niceSteps() {
        assertEquals(5.0, Plot2D.niceStep(20.0, 4), 0.0)
        assertEquals(0.2, Plot2D.niceStep(1.0, 6), 1e-12)
        assertEquals(listOf(-10.0, -5.0, 0.0, 5.0, 10.0), Plot2D.ticks(-10.0, 10.0, 4))
    }
    @Test fun tickLabels() = assertEquals("0.3", Plot2D.label(0.30000000000000004, 0.1))

    @Test fun zerosOfQuadratic() {
        val z = Plot2D.zeros(f1(row(Sym("x"), Pow(row("2")), Sym("−"), Sym("2"))), -10.0, 10.0)
        assertEquals(2, z.size); near(z[0], -Math.sqrt(2.0)); near(z[1], Math.sqrt(2.0))
    }
    @Test fun noZerosAtPoles() = assertEquals(0, Plot2D.zeros(f1(row(com.example.cas.editor.Frac(row("1"), row("x")))), -5.0, 5.0).size)
    @Test fun extremaOfCubic() {
        val e = Plot2D.extrema(f1(row(Sym("x"), Pow(row("3")), Sym("−"), Sym("3"), Sym("x"))), -5.0, 5.0)
        assertEquals(2, e.size)
        near(e[0].first, -1.0); assertEquals(Plot2D.Kind.Maximum, e[0].second)
        near(e[1].first, 1.0); assertEquals(Plot2D.Kind.Minimum, e[1].second)
    }
    @Test fun tanBreaksAtAsymptotes() {
        val lines = Plot2D.sample(f1(row(Func("tan", listOf(row("x"))))), Viewport(-5.0, 5.0, -5.0, 5.0))
        assertEquals(5, lines.size) // five branches, split at ±π/2 and ±3π/2
    }
    @Test fun gapsWhereUndefined() {
        val lines = Plot2D.sample(f1(row(com.example.cas.editor.Sqrt(row("x")))), Viewport(-5.0, 5.0, -5.0, 5.0))
        assertEquals(1, lines.size); assertTrue(lines[0].first().first >= 0)
    }
    @Test fun zoomKeepsPoint() {
        val v = Viewport(-10.0, 10.0, -10.0, 10.0).zoomBy(2.0, 0.75, 0.25)
        near(v.xMin + 0.75 * v.width, 5.0); near(v.yMax - 0.25 * v.height, 5.0); near(v.width, 10.0)
    }

    private val box = com.example.cas.graph.Bounds.square(2.0)
    @Test fun surfaceFacesSortedFarToNear() {
        val polys = Surface3D.explicit({ x, y -> (x * x - y * y) / 2 }, box, 20)
        val faces = Surface3D.faces(polys, box, Camera(), 400f, 400f)
        assertEquals(400, faces.size)
        assertTrue(faces.zipWithNext().all { (a, b) -> a.depth >= b.depth })
        assertTrue(faces.all { it.height in 0f..1f && it.shade in 0.35f..1f })
    }
    @Test fun surfaceSkipsUndefined() = assertEquals(200, Surface3D.explicit({ x, y -> Math.sqrt(x) + y / 4 }, box, 20).size)
    @Test fun boxHasTwelveEdges() = assertEquals(12, Surface3D.box(Camera(), 400f, 400f).size)
    @Test fun customLimits() {
        val b = com.example.cas.graph.Bounds(0.0, 4.0, -1.0, 1.0, 0.0, 16.0)
        val polys = Surface3D.explicit({ x, _ -> x * x }, b, 10)
        assertTrue(polys.all { p -> p.points.all { it[0] in 0.0..4.0 && it[1] in -1.0..1.0 } })
    }
    @Test fun implicitSphere() {
        val polys = Surface3D.implicit({ x, y, z -> x * x + y * y + z * z - 1 }, box, 20)
        assertTrue(polys.size > 300)
        assertTrue(polys.all { p -> p.points.all { q -> abs(Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2]) - 1) < 0.05 } })
    }
    @Test fun pickFindsTheNearestFace() {
        // A flat plane z = 0.5 seen from above: the point under the screen center is near (0, 0, 0.5).
        val polys = Surface3D.explicit({ _, _ -> 0.5 }, box, 20)
        val cam = Camera(yaw = 0.0, pitch = 1.45)
        val faces = Surface3D.faces(polys, box, cam, 400f, 400f)
        val (cx, cy) = Surface3D.project(0.0, 0.0, 0.5, box, cam, 400f, 400f)
        val hit = Surface3D.pick(faces, cx, cy)!!
        assertTrue(abs(hit.center[0]) < 0.2 && abs(hit.center[1]) < 0.2 && abs(hit.center[2] - 0.5) < 1e-9)
    }
    @Test fun pickMissesEmptySpace() = assertEquals(null, Surface3D.pick(Surface3D.faces(Surface3D.explicit({ _, _ -> 0.0 }, box, 10), box, Camera(), 400f, 400f), 2f, 2f))

    // ---- What a line in the 3D graph means
    private fun spec3(x: MathRow) = com.example.cas.graph.PlotSpec3D.classify(Evaluator().evaluate(x))
    @Test fun explicitSurface() = assertTrue(spec3(row(Sym("x"), Sym("y"))) is com.example.cas.graph.PlotSpec3D.Explicit)
    @Test fun explicitWithZ() = assertTrue(spec3(row(Sym("z"), Sym("="), Sym("x"), Sym("y"))) is com.example.cas.graph.PlotSpec3D.Explicit)
    @Test fun implicitSurface() = assertTrue(spec3(row(Sym("x"), Pow(row("2")), Sym("+"), Sym("y"), Pow(row("2")), Sym("+"), Sym("z"), Pow(row("2")), Sym("="), Sym("4"))) is com.example.cas.graph.PlotSpec3D.Implicit)
    @Test fun bareZIsAnError() = assertEquals("Add = … to make it an equation in x, y and z", runCatching { spec3(row(Sym("x"), Sym("z"))) }.exceptionOrNull()?.message)

    // ---- Area under a curve between two points
    @Test fun areaOfSine() {
        val (signed, total) = Plot2D.area({ Math.sin(it) }, 0.0, 2 * Math.PI)
        assertTrue(abs(signed) < 1e-9); assertTrue(abs(total - 4) < 1e-9)
    }
    @Test fun areaBackwardsIsNegative() {
        val (signed, total) = Plot2D.area({ it * it }, 3.0, 0.0)
        assertTrue(abs(signed + 9) < 1e-9); assertTrue(abs(total - 9) < 1e-9)
    }

    // ---- Stretching one axis with two fingers
    @Test fun sidewaysPinchStretchesOnlyX() {
        val (zx, zy) = com.example.cas.graph.AxisPinch.factors(200f, 5f, 400f, 6f, 48f)
        assertEquals(2.0, zx, 1e-9); assertEquals(1.0, zy, 1e-9)
    }
    @Test fun verticalPinchSqueezesOnlyY() {
        val (zx, zy) = com.example.cas.graph.AxisPinch.factors(3f, 300f, 4f, 150f, 48f)
        assertEquals(1.0, zx, 1e-9); assertEquals(0.5, zy, 1e-9)
    }
    @Test fun diagonalPinchScalesBoth() {
        val (zx, zy) = com.example.cas.graph.AxisPinch.factors(100f, 100f, 150f, 120f, 48f)
        assertEquals(1.5, zx, 1e-6); assertEquals(1.2, zy, 1e-6)
    }
    @Test fun zoomAxesKeepsTheOtherAxis() {
        val v = Viewport(-10.0, 10.0, -5.0, 5.0).zoomAxes(2.0, 1.0)
        assertEquals(-5.0, v.xMin, 1e-9); assertEquals(5.0, v.xMax, 1e-9); assertEquals(-5.0, v.yMin, 1e-9); assertEquals(5.0, v.yMax, 1e-9)
    }
}
