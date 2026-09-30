package com.example.cas

import com.example.cas.cas.CD
import com.example.cas.cas.ComplexMath
import com.example.cas.editor.Derivative
import com.example.cas.editor.Func
import com.example.cas.editor.Integral
import com.example.cas.editor.MathRow
import com.example.cas.editor.Node
import com.example.cas.editor.Pow
import com.example.cas.editor.Sym
import com.example.cas.engine.Evaluator
import com.example.cas.graph.ComplexCompiler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Derivatives and integrals on the complex plane (reference values from mpmath). */
class ComplexCalculusTest {
    private fun r(vararg n: Node) = MathRow(n.toMutableList())
    private fun s(t: String) = r(*t.map { Sym(it.toString()) }.toTypedArray())
    private fun close(expected: CD, actual: CD, tol: Double = 1e-8) =
        assertTrue("expected $expected, got $actual", (expected - actual).abs() <= tol * maxOf(1.0, expected.abs()))

    private fun plot(row: MathRow, at: CD): CD = ComplexCompiler.compile(Evaluator().evaluate(row), listOf("z"))(at, DoubleArray(0))

    @Test fun specialFunctions() {
        close(CD(-0.5772156649015329), ComplexMath.digamma(CD(1.0)))
        close(CD(0.03648997397857652), ComplexMath.digamma(CD(-0.5)))
        close(CD(Math.PI * Math.PI / 6), ComplexMath.trigamma(CD(1.0)))
        close(CD(0.8427007929497149), ComplexMath.erf(CD(1.0)))
        close(CD(0.0, 1.6504257587975429), ComplexMath.erf(CD(0.0, 1.0)))
        close(CD(0.998963278856817, -1.15467243792906e-5), ComplexMath.erf(CD(3.0, 2.0)))
        close(CD(96103547.8255166, 101670558.358252), ComplexMath.erf(CD(2.0, 5.0)), 1e-6)
        close(CD(-5441.00800581369, -26543.6324043348), ComplexMath.erf(CD(0.5, 3.5)), 1e-6)
    }

    @Test fun derivativesPlot() {
        // Γ′(2) = 1 − γ; J₁′(2) = (J₀(2) − J₂(2))/2; ζ′(2).
        close(CD(0.42278433509846713), plot(r(Derivative(s("z"), r(Func("gamma", listOf(s("z")))))), CD(2.0)))
        close(CD(-0.0644716247372), plot(r(Derivative(s("z"), r(Func("besselj", listOf(s("1"), s("z")))))), CD(2.0)), 1e-6)
        close(CD(-0.9375482543158437), plot(r(Derivative(s("z"), r(Func("zeta", listOf(s("z")))))), CD(2.0)), 1e-7)
        // Γ″ needs ψ′.
        close(CD(0.8236806608528794), plot(r(Derivative(s("z"), r(Func("gamma", listOf(s("z")))), MathRow(), s("2"))), CD(2.0)), 1e-7)
    }

    @Test fun integralsWithZInALimitPlot() {
        // ∫₀ᶻ e^(−t²) dt = (√π/2) erf z, at z = 2 and z = 1 + i.
        val row = r(Integral(s("0"), s("z"), r(Sym("e"), Pow(r(Sym("−"), Sym("t"), Pow(s("2"))))), s("t")))
        close(CD(0.8820813907624215), plot(row, CD(2.0)), 1e-9)
        val w = ComplexMath.erf(CD(1.0, 1.0)) * CD(Math.sqrt(Math.PI) / 2)
        close(w, plot(row, CD(1.0, 1.0)), 1e-9)
        assertEquals(0.0, plot(row, CD(0.0)).abs(), 1e-15)
    }
}
