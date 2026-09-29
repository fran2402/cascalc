package com.example.cas

import com.example.cas.graph.Fit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.exp

class FitTest {
    private fun close(a: Double, b: Double, tol: Double = 1e-6) = assertTrue("$a vs $b", kotlin.math.abs(a - b) < tol)

    @Test fun straightLine() {
        // y = 2x + 1 exactly
        val xs = doubleArrayOf(0.0, 1.0, 2.0, 3.0); val ys = doubleArrayOf(1.0, 3.0, 5.0, 7.0)
        val r = Fit.leastSquares({ x, p -> p[0] * x + p[1] }, xs, ys, doubleArrayOf(1.0, 1.0))!!
        close(r.parameters[0], 2.0); close(r.parameters[1], 1.0); close(r.rmse, 0.0)
    }
    @Test fun noisyLineIsTheLeastSquaresLine() {
        // the ordinary least-squares line of these points: slope Sxy/Sxx = 4.75/5 = 0.95, intercept 3.625 − 0.95·2.5 = 1.25
        val xs = doubleArrayOf(1.0, 2.0, 3.0, 4.0); val ys = doubleArrayOf(2.0, 3.5, 4.0, 5.0)
        val r = Fit.leastSquares({ x, p -> p[0] * x + p[1] }, xs, ys, doubleArrayOf(1.0, 1.0))!!
        close(r.parameters[0], 0.95); close(r.parameters[1], 1.25)
    }
    @Test fun parabola() {
        val xs = doubleArrayOf(-2.0, -1.0, 0.0, 1.0, 2.0); val ys = xs.map { 3 * it * it - it + 2 }.toDoubleArray()
        val r = Fit.leastSquares({ x, p -> p[0] * x * x + p[1] * x + p[2] }, xs, ys, doubleArrayOf(1.0, 1.0, 1.0))!!
        close(r.parameters[0], 3.0); close(r.parameters[1], -1.0); close(r.parameters[2], 2.0)
    }
    @Test fun exponential() {
        // nonlinear: y = a e^{bx}
        val xs = doubleArrayOf(0.0, 0.5, 1.0, 1.5, 2.0); val ys = xs.map { 2.5 * exp(0.8 * it) }.toDoubleArray()
        val r = Fit.leastSquares({ x, p -> p[0] * exp(p[1] * x) }, xs, ys, doubleArrayOf(1.0, 1.0))!!
        close(r.parameters[0], 2.5, 1e-5); close(r.parameters[1], 0.8, 1e-5)
    }
    @Test fun tooFewPoints() = assertEquals(null, Fit.leastSquares({ x, p -> p[0] * x * x + p[1] * x + p[2] }, doubleArrayOf(1.0, 2.0), doubleArrayOf(1.0, 2.0), doubleArrayOf(1.0, 1.0, 1.0)))
    @Test fun undefinedEverywhereFails() = assertEquals(null, Fit.leastSquares({ _, _ -> Double.NaN }, doubleArrayOf(1.0, 2.0, 3.0), doubleArrayOf(1.0, 2.0, 3.0), doubleArrayOf(1.0)))
}
