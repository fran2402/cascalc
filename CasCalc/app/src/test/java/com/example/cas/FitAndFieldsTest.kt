package com.example.cas

import com.example.cas.graph.Fit
import com.example.cas.graph.FitModel
import com.example.cas.graph.Plot2D
import com.example.cas.graph.SlopeField
import com.example.cas.graph.Viewport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.exp

class FitAndFieldsTest {
    private val xs = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)

    @Test fun lineWithStatistics() {
        // y = 2x + 1 with a little noise: the textbook numbers.
        val ys = doubleArrayOf(3.1, 4.9, 7.2, 8.8, 11.1, 13.0)
        val r = Fit.leastSquares(FitModel.Linear::value, xs, ys, FitModel.Linear.start(xs, ys))!!
        assertEquals(1.991428571, r.parameters[0], 1e-7)
        assertEquals(1.046666667, r.parameters[1], 1e-7)
        assertEquals(4, r.dof)
        assertTrue(r.rSquared > 0.99)
        val e = r.errors!!
        // Standard errors from s² (XᵀX)⁻¹, as any statistics package gives them.
        assertEquals(0.0391056, e[0], 1e-5)
        assertEquals(0.1522946, e[1], 1e-5)
        assertEquals(0.99845993, r.rSquared, 1e-7)
        assertNull(r.reducedChiSquared)
    }

    @Test fun weightedFitGivesChiSquared() {
        val ys = doubleArrayOf(3.1, 4.9, 7.2, 8.8, 11.1, 13.0)
        val sig = DoubleArray(6) { 0.15 }
        val r = Fit.leastSquares(FitModel.Linear::value, xs, ys, FitModel.Linear.start(xs, ys), sigmas = sig)!!
        assertNotNull(r.reducedChiSquared)
        assertTrue(r.reducedChiSquared!! in 0.3..3.0)
        // With σ given, the errors come from σ alone: σ/√Sxx for the slope.
        assertEquals(0.15 / kotlin.math.sqrt(17.5), r.errors!![0], 1e-4)
    }

    @Test fun exponentialAndPowerStartWell() {
        val ye = DoubleArray(6) { 2 * exp(0.5 * xs[it]) }
        val re = Fit.leastSquares(FitModel.Exponential::value, xs, ye, FitModel.Exponential.start(xs, ye))!!
        assertEquals(2.0, re.parameters[0], 1e-6); assertEquals(0.5, re.parameters[1], 1e-8)
        val yp = DoubleArray(6) { 3 * Math.pow(xs[it], 1.5) }
        val rp = Fit.leastSquares(FitModel.Power::value, xs, yp, FitModel.Power.start(xs, yp))!!
        assertEquals(1.5, rp.parameters[1], 1e-8)
        assertEquals(1.0, rp.rSquared, 1e-12)
    }

    @Test fun polynomialStartIsExact() {
        val ys = DoubleArray(6) { 2 * xs[it] * xs[it] - 3 * xs[it] + 1 }
        val p = FitModel.Quadratic.start(xs, ys)
        assertEquals(2.0, p[0], 1e-9); assertEquals(-3.0, p[1], 1e-9); assertEquals(1.0, p[2], 1e-9)
    }

    @Test fun equations() {
        val n = { v: Double -> String.format(java.util.Locale.US, "%.3g", v) }
        assertEquals("y = 2.00x - 1.00", FitModel.Linear.equation(doubleArrayOf(2.0, -1.0), n))
        assertEquals("y = 3.00\\,e^{0.500x}", FitModel.Exponential.equation(doubleArrayOf(3.0, 0.5), n))
    }

    @Test fun slopeFieldSegmentsHaveTheSlope() {
        val v = Viewport(-5.0, 5.0, -5.0, 5.0)
        val segs = SlopeField.segments({ x, _ -> x }, v, 400.0, 400.0)
        assertTrue(segs.size > 100)
        segs.forEach { s -> val m = (s[3] - s[1]) / (s[2] - s[0]); assertEquals((s[0] + s[2]) / 2, m, 1e-9) }
    }

    @Test fun solutionsFollowTheEquation() {
        // y' = y through (0, 1) is eˣ.
        val v = Viewport(-2.0, 2.0, -1.0, 8.0)
        val line = SlopeField.solution({ _, y -> y }, 0.0, 1.0, v).single()
        val (x, y) = line.minByOrNull { kotlin.math.abs(it.first - 1.0) }!!
        assertEquals(exp(x), y, 1e-6)
        assertTrue(line.first().first < -1.9 && line.last().first > 1.9)
    }

    @Test fun arcLengthAndDerivative() {
        // A straight line from 0 to 3 with slope 4/3 is 5 long; the parabola's arc is a known value.
        assertEquals(5.0, Plot2D.arcLength({ x -> 4.0 / 3 * x }, 0.0, 3.0), 1e-9)
        assertEquals(1.4789428575, Plot2D.arcLength({ x -> x * x }, 0.0, 1.0), 1e-7)
        assertEquals(6.0, Plot2D.derivative({ x -> x * x * x }, Math.sqrt(2.0)), 1e-6)
    }
}

class SessionExportTest {
    private fun r(t: String) = com.example.cas.engine.LatexParser.parse(t)

    @Test fun latexDocument() {
        val doc = com.example.cas.graph.SessionExport.latex(listOf(
            com.example.cas.graph.SessionExport.Entry(r("\\int_{0}^{1} x^{2}\\,dx"), r("\\frac{1}{3}")),
            com.example.cas.graph.SessionExport.Entry(r("\\pi"), r("3.14159"), approximate = true),
        ), "Homework 3")
        assertTrue(doc.startsWith("\\documentclass"))
        assertTrue(doc.contains("\\section*{Homework 3}"))
        assertEquals(2, Regex("\\\\begin\\{align\\}").findAll(doc).count())
        assertTrue(doc.contains("&\\approx 3.14159"))
        assertTrue(doc.trimEnd().endsWith("\\end{document}"))
    }

    @Test fun pagesBreak() {
        val e = com.example.cas.graph.SessionExport.Entry(r("\\frac{x^{2}+1}{x-1}"), r("x+1+\\frac{2}{x-1}"))
        val one = com.example.cas.graph.SessionExport.pages(listOf(e), "t", "s")
        assertEquals(1, one.size)
        // Many calculations run onto more pages, each of them A4.
        val many = com.example.cas.graph.SessionExport.pages(List(60) { e }, "t", "s")
        assertTrue(many.size >= 3)
        assertTrue(many.all { it.width == 595.0 && it.height == 842.0 })
    }
}

class CustomFitTest {
    @Test fun customModel() {
        val m = com.example.cas.graph.CustomFitModel.of("a\\sin(bx)+c")!!
        assertEquals(listOf("a", "b", "c"), m.params)
        val xs = DoubleArray(30) { it * 0.2 }
        val ys = DoubleArray(30) { 2 * Math.sin(1.3 * xs[it]) + 0.5 }
        val r = com.example.cas.graph.Fit.leastSquares(m::value, xs, ys, doubleArrayOf(1.5, 1.2, 0.0))!!
        assertEquals(2.0, r.parameters[0], 1e-6); assertEquals(1.3, r.parameters[1], 1e-6); assertEquals(0.5, r.parameters[2], 1e-6)
        assertTrue(m.equation(r.parameters).startsWith("y = "))
        assertNull(com.example.cas.graph.CustomFitModel.of("x^2"))
        assertEquals(listOf("A", "τ"), com.example.cas.graph.CustomFitModel.of("y = A e^{-x/τ}")!!.params)
    }
}
