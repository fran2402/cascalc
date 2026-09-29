package com.example.cas.graph

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Least-squares fitting of y = f(x; p₁…pₙ) to points, by Levenberg–Marquardt: Gauss–Newton
 * steps, damped towards gradient descent when a step doesn't help. Derivatives with respect to
 * the parameters are taken numerically, so any function the graph can draw can be fitted.
 */
object Fit {
    /** The fitted parameters and the root-mean-square error, or null if it didn't settle. */
    data class Result(val parameters: DoubleArray, val rmse: Double)

    /**
     * [f] takes (x, p₁, …, pₙ). Starts from [start]; returns null if there are fewer points than
     * parameters, or the fit runs into undefined values or doesn't converge.
     */
    fun leastSquares(f: (Double, DoubleArray) -> Double, xs: DoubleArray, ys: DoubleArray, start: DoubleArray, iterations: Int = 200): Result? {
        val n = start.size
        val m = xs.size
        if (m < n || n == 0) return null
        var p = start.copyOf()
        fun residuals(q: DoubleArray) = DoubleArray(m) { i -> ys[i] - f(xs[i], q) }
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
                for (i in 0 until m) jac[i][k] = (f(xs[i], up) - f(xs[i], down)) / (2 * h)
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
                    if (done) return Result(p, sqrt(c / m))
                    break
                }
                lambda *= 10
            }
            if (!improved) return if (c.isFinite()) Result(p, sqrt(c / m)) else null
        }
        return if (c.isFinite()) Result(p, sqrt(c / m)) else null
    }

    /** Gaussian elimination with partial pivoting; null if the system is singular. */
    private fun solve(a0: Array<DoubleArray>, b0: DoubleArray): DoubleArray? {
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
