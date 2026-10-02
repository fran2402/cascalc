package com.example.cas.graph

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Least-squares fitting of y = f(x; p₁…pₙ) to points, by Levenberg–Marquardt: Gauss–Newton
 * steps, damped towards gradient descent when a step doesn't help. Derivatives with respect to
 * the parameters are taken numerically, so any function the graph can draw can be fitted.
 */
object Fit {
    /**
     * The fitted parameters and the root-mean-square error; with [stats], each parameter's
     * standard error, R², and (when the points had uncertainties) χ² per degree of freedom.
     */
    data class Result(
        val parameters: DoubleArray,
        val rmse: Double,
        val errors: DoubleArray? = null,
        val rSquared: Double = Double.NaN,
        val reducedChiSquared: Double? = null,
        val dof: Int = 0,
    )

    /**
     * [f] takes (x, p₁, …, pₙ). Starts from [start]; returns null if there are fewer points than
     * parameters, or the fit runs into undefined values or doesn't converge.
     */
    fun leastSquares(f: (Double, DoubleArray) -> Double, xs: DoubleArray, ys: DoubleArray, start: DoubleArray, iterations: Int = 200, sigmas: DoubleArray? = null): Result? {
        val fit = minimize(f, xs, ys, start, iterations, sigmas) ?: return null
        return withStatistics(f, xs, ys, fit, sigmas)
    }

    /**
     * Each parameter's standard error from the covariance (JᵀWJ)⁻¹: scaled by the residual
     * variance when the points have no uncertainties, as given when they do (then χ²/dof says
     * how well the uncertainties match the scatter).
     */
    private fun withStatistics(f: (Double, DoubleArray) -> Double, xs: DoubleArray, ys: DoubleArray, fit: Result, sigmas: DoubleArray?): Result {
        val p = fit.parameters
        val n = p.size; val m = xs.size
        val w = DoubleArray(m) { i -> sigmas?.get(i)?.takeIf { it.isFinite() && it > 0 }?.let { 1 / (it * it) } ?: 1.0 }
        val jac = Array(m) { DoubleArray(n) }
        for (k in 0 until n) {
            val h = 1e-6 * maxOf(1.0, abs(p[k]))
            val up = p.copyOf().also { it[k] += h }
            val down = p.copyOf().also { it[k] -= h }
            for (i in 0 until m) jac[i][k] = (f(xs[i], up) - f(xs[i], down)) / (2 * h)
        }
        val a = Array(n) { k -> DoubleArray(n) { l -> (0 until m).sumOf { i -> w[i] * jac[i][k] * jac[i][l] } } }
        val inverse = invert(a)
        val dof = m - n
        val residuals = DoubleArray(m) { i -> ys[i] - f(xs[i], p) }
        val ssr = residuals.sumOf { it * it }
        val mean = ys.average()
        val sst = ys.sumOf { (it - mean) * (it - mean) }
        val chi2 = (0 until m).sumOf { i -> w[i] * residuals[i] * residuals[i] }
        val weighted = sigmas != null && sigmas.any { it.isFinite() && it > 0 }
        val scale = if (weighted) 1.0 else if (dof > 0) ssr / dof else Double.NaN
        val errors = inverse?.let { inv -> DoubleArray(n) { k -> kotlin.math.sqrt(abs(inv[k][k]) * scale) } }
        return fit.copy(
            errors = errors,
            rSquared = if (sst > 0) 1 - ssr / sst else Double.NaN,
            reducedChiSquared = if (weighted && dof > 0) chi2 / dof else null,
            dof = dof,
        )
    }

    private fun invert(a: Array<DoubleArray>): Array<DoubleArray>? {
        val n = a.size
        val cols = (0 until n).map { k -> solve(a, DoubleArray(n) { if (it == k) 1.0 else 0.0 }) ?: return null }
        return Array(n) { r -> DoubleArray(n) { c -> cols[c][r] } }
    }

    private fun minimize(f: (Double, DoubleArray) -> Double, xs: DoubleArray, ys: DoubleArray, start: DoubleArray, iterations: Int, sigmas: DoubleArray?): Result? {
        val n = start.size
        val m = xs.size
        if (m < n || n == 0) return null
        var p = start.copyOf()
        // Each residual divided by its point's σ (when given), so surer points count for more.
        val scale = DoubleArray(m) { i -> sigmas?.get(i)?.takeIf { it.isFinite() && it > 0 }?.let { 1 / it } ?: 1.0 }
        val ws = DoubleArray(m) { scale[it] }
        fun residuals(q: DoubleArray) = DoubleArray(m) { i -> (ys[i] - f(xs[i], q)) * ws[i] }
        fun cost(r: DoubleArray) = r.sumOf { it * it }
        var r = residuals(p)
        var c = cost(r)
        if (!c.isFinite()) return null
        var lambda = 1e-3
        repeat(iterations) {
            // Jacobian of the model by central differences.
            val jac = Array(m) { DoubleArray(n) }
            for (k in 0 until n) {
                val h = 1e-6 * maxOf(1.0, abs(p[k]))
                val up = p.copyOf().also { it[k] += h }
                val down = p.copyOf().also { it[k] -= h }
                for (i in 0 until m) jac[i][k] = (f(xs[i], up) - f(xs[i], down)) / (2 * h) * ws[i]
            }
            // Normal equations (JᵀJ + λ diag) δ = Jᵀr.
            val a = Array(n) { DoubleArray(n) }
            val g = DoubleArray(n)
            for (i in 0 until m) for (k in 0 until n) {
                g[k] += jac[i][k] * r[i]
                for (l in 0 until n) a[k][l] += jac[i][k] * jac[i][l]
            }
            var improved = false
            for (attempt in 0 until 10) {
                val damped = Array(n) { k -> DoubleArray(n) { l -> a[k][l] + if (k == l) lambda * maxOf(a[k][k], 1e-12) else 0.0 } }
                val step = solve(damped, g) ?: break
                val q = DoubleArray(n) { p[it] + step[it] }
                val rq = residuals(q)
                val cq = cost(rq)
                if (cq.isFinite() && cq < c) {
                    val done = abs(c - cq) <= 1e-14 * (1 + c)
                    p = q; r = rq; c = cq
                    lambda = maxOf(lambda / 10, 1e-12)
                    improved = true
                    if (done) return Result(p, rms(f, xs, ys, p))
                    break
                }
                lambda *= 10
            }
            if (!improved) return if (c.isFinite()) Result(p, rms(f, xs, ys, p)) else null
        }
        return if (c.isFinite()) Result(p, rms(f, xs, ys, p)) else null
    }

    /** The root-mean-square of the plain (unweighted) residuals. */
    private fun rms(f: (Double, DoubleArray) -> Double, xs: DoubleArray, ys: DoubleArray, p: DoubleArray) =
        sqrt(xs.indices.sumOf { i -> (ys[i] - f(xs[i], p)).let { it * it } } / xs.size)

    /** Gaussian elimination with partial pivoting; null if the system is singular. */
    internal fun solve(a0: Array<DoubleArray>, b0: DoubleArray): DoubleArray? {
        val n = b0.size
        val a = Array(n) { a0[it].copyOf() }
        val b = b0.copyOf()
        for (col in 0 until n) {
            val pivot = (col until n).maxByOrNull { abs(a[it][col]) } ?: return null
            if (abs(a[pivot][col]) < 1e-300) return null
            a[col] = a[pivot].also { a[pivot] = a[col] }
            b[col] = b[pivot].also { b[pivot] = b[col] }
            for (row in col + 1 until n) {
                val factor = a[row][col] / a[col][col]
                for (k in col until n) a[row][k] -= factor * a[col][k]
                b[row] -= factor * b[col]
            }
        }
        val x = DoubleArray(n)
        for (row in n - 1 downTo 0) {
            var s = b[row]
            for (k in row + 1 until n) s -= a[row][k] * x[k]
            x[row] = s / a[row][row]
        }
        return x
    }
}
