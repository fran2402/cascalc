package com.example.cas

import com.example.cas.cas.CD
import com.example.cas.cas.ComplexMath
import com.example.cas.cas.Numeric
import com.example.cas.cas.Printer
import com.example.cas.editor.BigOp
import com.example.cas.editor.BigOpKind
import com.example.cas.editor.Frac
import com.example.cas.editor.MathRow
import com.example.cas.editor.Node
import com.example.cas.editor.Pow
import com.example.cas.editor.Sym
import com.example.cas.engine.Evaluator
import com.example.cas.graph.ComplexCompiler
import com.example.cas.graph.Compiler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Infinite sums in closed form, from the shape of the term (values from mpmath). */
class SeriesTest {
    private fun r(vararg n: Node) = MathRow(n.toMutableList())
    private fun s(t: String) = r(*t.map { Sym(it.toString()) }.toTypedArray())
    private fun p(base: String, e: String) = r(*s(base).items.toTypedArray(), Pow(s(e)))
    private fun cat(vararg rows: MathRow) = r(*rows.flatMap { it.items }.toTypedArray())
    private fun fr(a: MathRow, b: MathRow) = r(Frac(a, b))
    private fun sum(lo: String, body: MathRow) = r(BigOp(BigOpKind.Sum, s("n"), s(lo), s("∞"), body))
    private fun cas(row: MathRow) = Printer.plain(Evaluator().evaluate(row))
    private fun value(row: MathRow) = Numeric.eval(Evaluator().evaluate(row))
    private fun close(expected: Double, actual: Double, tol: Double = 1e-9) = assertTrue("expected $expected, got $actual", kotlin.math.abs(expected - actual) <= tol * maxOf(1.0, kotlin.math.abs(expected)))

    @Test fun basel() = assertEquals("π^2/6", cas(sum("1", fr(s("1"), p("n", "2")))))
    @Test fun alternatingHarmonic() = assertEquals("ln(2)", cas(sum("1", fr(p("(−1)", "n+1"), s("n")))))
    @Test fun eta2() = assertEquals("π^2/12", cas(sum("1", fr(p("(−1)", "n+1"), p("n", "2")))))
    @Test fun etaSymbolic() = assertEquals("zeta(z)(-2^(-z+1)+1)", cas(sum("1", fr(p("(−1)", "n+1"), p("n", "z")))))
    @Test fun zetaSymbolic() = assertEquals("zeta(z)", cas(sum("1", fr(s("1"), p("n", "z")))))
    @Test fun leibniz() = assertEquals("π/4", cas(sum("0", fr(p("(−1)", "n"), s("2n+1")))))
    @Test fun catalan() = close(0.915965594177219, value(sum("0", fr(p("(−1)", "n"), p("(2n+1)", "2")))).re)
    @Test fun telescoping() = assertEquals("1", cas(sum("1", fr(s("1"), s("n(n+1)")))))
    @Test fun oddSquares() = assertEquals("π^2/8", cas(sum("1", fr(s("1"), p("(2n−1)", "2")))))
    @Test fun exponential() = assertEquals("e^x", cas(sum("0", fr(p("x", "n"), s("n!")))))
    @Test fun sine() = assertEquals("sin(x)", cas(sum("0", fr(cat(p("(−1)", "n"), p("x", "2n+1")), s("(2n+1)!")))))
    @Test fun cosine() = assertEquals("cos(x)", cas(sum("0", fr(cat(p("(−1)", "n"), p("x", "2n")), s("(2n)!")))))
    @Test fun eNumber() = assertEquals("e", cas(sum("0", fr(s("1"), s("n!")))))
    @Test fun geometricMoments() {
        assertEquals("2", cas(sum("1", fr(s("n"), p("2", "n")))))
        assertEquals("6", cas(sum("1", fr(p("n", "2"), p("2", "n")))))
    }
    @Test fun logSeries() = assertEquals("-ln(-x+1)", cas(sum("1", fr(p("x", "n"), s("n")))))
    @Test fun dilog() = assertEquals("polylog(2,x)", cas(sum("1", fr(p("x", "n"), p("n", "2")))))
    @Test fun harmonicDiverges() = assertEquals("∞", cas(sum("1", fr(s("1"), s("n")))))
    @Test fun laterStart() = assertEquals("-17/16+π^4/90", cas(sum("3", fr(s("1"), p("n", "4")))))
    @Test fun noClosedFormIsAddedUp() = close(1.0139591, value(sum("1", fr(r(com.example.cas.editor.Func("sin", listOf(s("n")))), p("n", "2")))).re, 1e-6)

    @Test fun etaPlotsOnTheComplexPlane() {
        val e = Evaluator().evaluate(sum("1", fr(p("(−1)", "n+1"), p("n", "z"))))
        val c = ComplexCompiler.compile(e, listOf("z"))
        close(0.8224670334241132, c(CD(2.0), DoubleArray(0)).re)
        // η(½ + 3i) from mpmath.altzeta
        val w = c(CD(0.5, 3.0), DoubleArray(0))
        close(0.997091432527484, w.re, 1e-8); close(0.52479272474704, w.im, 1e-8)
    }

    @Test fun etaPlotsOnTheRealLine() {
        val e = Evaluator().evaluate(cat(sum("1", fr(p("(−1)", "n+1"), p("n", "x")))))
        close(0.8224670334241132, Compiler.compile(e, listOf("x"))(doubleArrayOf(2.0)))
    }

    @Test fun specialFunctions() {
        // Li₂(½) = π²/12 − ln²2/2; Li₂(0.9); Li_{½}(0.95); ζ(3, ¼)
        close(0.5822405264650125, ComplexMath.polylog(CD(2.0), CD(0.5)).re)
        close(1.2997147230049588, ComplexMath.polylog(CD(2.0), CD(0.9)).re, 1e-8)
        close(6.37636137258554, ComplexMath.polylog(CD(0.5), CD(0.95)).re, 1e-8)
        close(64.6638699687685, ComplexMath.hurwitz(CD(3.0), 0.25).re, 1e-9)
        // Complex arguments: Li₂(−0.7 + 0.5i), ζ(0.3 + 2i, 0.7)
        val li = ComplexMath.polylog(CD(2.0), CD(-0.7, 0.5)); close(-0.634803441693774, li.re, 1e-9); close(0.375008404437177, li.im, 1e-9)
        // Beyond |z| = 1, by the inversion formula: Li₂(−2), Li₃(−5), Li₂(3)
        close(-1.43674636688368, ComplexMath.polylog(CD(2.0), CD(-2.0)).re, 1e-9)
        close(-3.53751143761861, ComplexMath.polylog(CD(3.0), CD(-5.0)).re, 1e-9)
        val l3 = ComplexMath.polylog(CD(2.0), CD(3.0)); close(2.3201804233131, l3.re, 1e-9); close(3.4513922952232, kotlin.math.abs(l3.im), 1e-9)
        val hz = ComplexMath.hurwitz(CD(0.3, 2.0), 0.7); close(0.455940037382897, hz.re, 1e-9); close(0.265156899857882, hz.im, 1e-9)
    }
}
