package com.example.cas

import com.example.cas.cas.CD
import com.example.cas.cas.ComplexMath
import com.example.cas.cas.Printer
import com.example.cas.editor.Frac
import com.example.cas.editor.Func
import com.example.cas.editor.MathRow
import com.example.cas.editor.Pow
import com.example.cas.editor.Sym
import com.example.cas.editor.row
import com.example.cas.engine.Evaluator
import com.example.cas.graph.ColoringOptions
import com.example.cas.graph.ComplexCompiler
import com.example.cas.graph.DomainColoring
import com.example.cas.graph.Viewport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

class ComplexPlotTest {
    private fun near(a: CD, b: CD, tol: Double = 1e-9) = assertTrue("$a vs $b", (a - b).abs() <= tol * maxOf(1.0, b.abs()))
    private fun f(r: MathRow, vararg params: String) = ComplexCompiler.compile(Evaluator().evaluate(r), listOf("z") + params)
    private fun cas(r: MathRow) = Printer.plain(Evaluator().evaluate(r))

    // ---- Complex functions
    @Test fun sinOfI() = near(ComplexMath.sin(CD(0.0, 1.0)), CD(0.0, Math.sinh(1.0)))
    @Test fun expOfIPi() = near(ComplexMath.exp(CD(0.0, PI)), CD(-1.0, 0.0))
    @Test fun lnOfMinusOne() = near(ComplexMath.ln(CD(-1.0)), CD(0.0, PI))
    @Test fun sqrtOfMinusFour() = near(ComplexMath.sqrt(CD(-4.0)), CD(0.0, 2.0))
    @Test fun asinRoundTrip() = near(ComplexMath.sin(ComplexMath.asin(CD(0.3, 0.7))), CD(0.3, 0.7))
    @Test fun atanRoundTrip() = near(ComplexMath.tan(ComplexMath.atan(CD(0.3, 0.7))), CD(0.3, 0.7))
    @Test fun gammaHalf() = near(ComplexMath.gamma(CD(0.5)), CD(Math.sqrt(PI)))
    @Test fun gammaComplex() = near(ComplexMath.gamma(CD(1.0, 1.0)), CD(0.4980156681183560, -0.1549498283018106), 1e-8)
    @Test fun zetaTwo() = near(ComplexMath.zeta(CD(2.0)), CD(PI * PI / 6), 1e-9)
    @Test fun zetaMinusOne() = near(ComplexMath.zeta(CD(-1.0)), CD(-1.0 / 12), 1e-9)
    @Test fun zetaCriticalLine() = near(ComplexMath.zeta(CD(0.5, 14.134725141734693)), CD(0.0, 0.0), 1e-6) // first non-trivial zero

    // ---- Exact values in the CAS
    @Test fun gammaExactInteger() = assertEquals("120", cas(row(Func("gamma", listOf(row("6"))))))
    @Test fun gammaExactHalf() = assertEquals("3√π/4", cas(row(Func("gamma", listOf(row(Frac(row("5"), row("2"))))))))
    @Test fun gammaNegativeHalf() = assertEquals("-2√π", cas(row(Func("gamma", listOf(row(Sym("−"), Frac(row("1"), row("2"))))))))
    @Test fun zetaExactTwo() = assertEquals("π^2/6", cas(row(Func("zeta", listOf(row("2"))))))
    @Test fun zetaExactFour() = assertEquals("π^4/90", cas(row(Func("zeta", listOf(row("4"))))))
    @Test fun zetaNegative() = assertEquals("-1/12", cas(row(Func("zeta", listOf(row(Sym("−"), Sym("1")))))))
    @Test fun zetaThreeStaysSymbolic() = assertEquals("zeta(3)", cas(row(Func("zeta", listOf(row("3"))))))

    // ---- Compiling expressions in z
    @Test fun compilePolynomial() = near(f(row(Sym("z"), Pow(row("2")), Sym("+"), Sym("1")))(CD(0.0, 1.0), DoubleArray(0)), CD(0.0))
    @Test fun compileWithParameter() = near(f(row(Sym("z"), Sym("−"), Sym("t")), "t")(CD(2.0, 1.0), doubleArrayOf(0.5)), CD(1.5, 1.0))
    @Test fun compileExp() = near(f(row(Sym("e"), Pow(row("z"))))(CD(0.0, PI), DoubleArray(0)), CD(-1.0))
    @Test fun compileSqrtBranch() = near(f(row(com.example.cas.editor.Sqrt(row("z"))))(CD(-4.0, -1e-12), DoubleArray(0)), CD(0.0, -2.0), 1e-6)

    // ---- Colors
    private fun rgb(c: Int) = Triple((c shr 16) and 255, (c shr 8) and 255, c and 255)
    @Test fun zeroIsBlack() = assertEquals(Triple(0, 0, 0), rgb(DomainColoring.color(CD(0.0), ColoringOptions(modulusBands = false))))
    @Test fun infinityIsWhite() = assertEquals(Triple(255, 255, 255), rgb(DomainColoring.color(CD(Double.POSITIVE_INFINITY), ColoringOptions())))
    @Test fun positiveRealIsRed() {
        val (r, g, b) = rgb(DomainColoring.color(CD(1.0), ColoringOptions(modulusBands = false)))
        assertTrue(r > 200 && g < 30 && b < 30)
    }
    @Test fun imaginaryUnitIsGreenish() {
        // arg = 90° → a quarter of the way round the hue circle: yellow-green.
        val (r, g, b) = rgb(DomainColoring.color(CD(0.0, 1.0), ColoringOptions(modulusBands = false)))
        assertTrue(g > 200 && b < 30 && r in 100..160)
    }
    @Test fun renderSize() {
        val px = DomainColoring.render(f(row("z")), DoubleArray(0), Viewport(-2.0, 2.0, -2.0, 2.0), 40, 30, ColoringOptions())!!
        assertEquals(1200, px.size)
    }

    // ---- Contour integrals and residues
    private fun circle(c: CD, r: Double, n: Int = 400) = List(n) { k -> c + CD(r * Math.cos(2 * PI * k / n), r * Math.sin(2 * PI * k / n)) }
    @Test fun oneOverZ() = near(DomainColoring.contourIntegral(f(row(Frac(row("1"), row("z")))), DoubleArray(0), circle(CD(0.0), 1.0)), CD(0.0, 2 * PI), 1e-4)
    @Test fun analyticIsZero() = near(DomainColoring.contourIntegral(f(row(Sym("z"), Pow(row("2")))), DoubleArray(0), circle(CD(0.0), 1.0)), CD(0.0), 1e-4)
    @Test fun residueOfSimplePole() {
        // 1/(z² + 1) around i only: residue 1/(2i) = −i/2.
        val g = f(row(Frac(row("1"), row(Sym("z"), Pow(row("2")), Sym("+"), Sym("1")))))
        near(DomainColoring.residueSum(DomainColoring.contourIntegral(g, DoubleArray(0), circle(CD(0.0, 1.0), 0.5))), CD(0.0, -0.5), 1e-4)
    }
    @Test fun clockwiseIsNegative() = near(DomainColoring.contourIntegral(f(row(Frac(row("1"), row("z")))), DoubleArray(0), circle(CD(0.0), 1.0).reversed()), CD(0.0, -2 * PI), 1e-4)
}
