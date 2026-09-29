package com.example.cas

import com.example.cas.cas.CD
import com.example.cas.cas.ComplexMath
import com.example.cas.cas.Statistics
import com.example.cas.editor.Func
import com.example.cas.editor.row
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Bessel J and Y against tabulated values (Abramowitz & Stegun, DLMF), on and off the real line. */
class BesselTest {
    private fun near(expected: CD, actual: CD, tol: Double = 1e-8) =
        assertTrue("$actual vs $expected", (actual - expected).abs() <= tol * maxOf(1.0, expected.abs()))

    @Test fun smallRealArguments() {
        near(CD(0.7651976865579666), ComplexMath.besselJ(0.0, CD(1.0)))
        near(CD(0.4970941024642741), ComplexMath.besselJ(1.0, CD(2.5)))
        near(CD(0.08825696421567697), ComplexMath.besselY(0.0, CD(1.0)))
        near(CD(-0.7812128213002887), ComplexMath.besselY(1.0, CD(1.0)))
    }

    @Test fun largeRealArgumentsUseHankel() {
        near(CD(0.1670246643405831), ComplexMath.besselJ(0.0, CD(20.0)))
        near(CD(0.06264059680939), ComplexMath.besselY(0.0, CD(20.0)))
        near(CD(-0.0863679835810402), ComplexMath.besselJ(0.0, CD(30.0)), 1e-9)
        // The real function now uses the same expansion far out.
        assertEquals(-0.0863679835810402, Statistics.besselJ(0.0, 30.0), 1e-9)
    }

    @Test fun imaginaryAxisIsTheModifiedBessel() {
        // J₀(ix) = I₀(x), J₁(ix) = i I₁(x).
        near(CD(1.2660658777520084), ComplexMath.besselJ(0.0, CD(0.0, 1.0)))
        near(CD(0.0, 0.5651591039924851), ComplexMath.besselJ(1.0, CD(0.0, 1.0)))
    }

    @Test fun halfOrderIsElementary() {
        // J_{1/2}(z) = √(2/πz) sin z, also for complex z.
        for (z in listOf(CD(1.3, 0.4), CD(0.2, -2.0), CD(25.0, 3.0))) {
            val expected = ComplexMath.sqrt(CD(2 / Math.PI) / z) * ComplexMath.sin(z)
            near(expected, ComplexMath.besselJ(0.5, z), 1e-7)
        }
    }

    @Test fun satisfiesTheRecurrence() {
        // J_{a−1} + J_{a+1} = (2a/z) J_a, and the same for Y, at a complex point.
        val z = CD(3.7, 1.9)
        for (a in listOf(1.0, 2.5)) {
            near(CD(2 * a) / z * ComplexMath.besselJ(a, z), ComplexMath.besselJ(a - 1, z) + ComplexMath.besselJ(a + 1, z), 1e-7)
            near(CD(2 * a) / z * ComplexMath.besselY(a, z), ComplexMath.besselY(a - 1, z) + ComplexMath.besselY(a + 1, z), 1e-6)
        }
    }

    @Test fun plotsOnTheComplexPlane() {
        val e = com.example.cas.engine.Evaluator().evaluate(row(Func("besselj", listOf(row("0"), row("z")))))
        val f = com.example.cas.graph.ComplexCompiler.compile(e, listOf("z"))
        near(CD(1.2660658777520084), f(CD(0.0, 1.0), DoubleArray(0)))
    }

    @Test fun latexWritesJWithTheOrder() =
        assertEquals("J_{a}\\left(z\\right)", com.example.cas.engine.Latex.of(row(Func("besselj", listOf(row("a"), row("z"))))))
}
