package com.example.cas.cas


import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** Numerical methods for things that have no exact answer in general. */
object Numerics {

    // 15-point Gauss–Kronrod nodes and weights, with the embedded 7-point Gauss rule.
    private val XGK = doubleArrayOf(
        0.991455371120812639206854697526329, 0.949107912342758524526189684047851,
        0.864864423359769072789712788640926, 0.741531185599394439863864773280788,
        0.586087235467691130294144845693013, 0.405845151377397166906606412076961,
        0.207784955007898467600689403773245, 0.000000000000000000000000000000000,
    )
    private val WGK = doubleArrayOf(
        0.022935322010529224963732008058970, 0.063092092629978553290700663189204,
        0.104790010322250183839876322541518, 0.140653259715525918745189590510238,
        0.169004726639267902826583426598550, 0.190350578064785409913256402421014,
        0.204432940075298892414161999234649, 0.209482141084727828012999174891714,
    )
    private val WG = doubleArrayOf(
        0.129484966168869693270611432679082, 0.279705391489276667901467771423780,
        0.381830050505118944950369775488975, 0.417959183673469387755102040816327,
    )

    private class Segment(val a: Double, val b: Double, val value: Double, val error: Double)

    private fun kronrod(f: (Double) -> Double, a: Double, b: Double): Segment {
        val c = (a + b) / 2
        val h = (b - a) / 2
        val fc = f(c)
        var kronrod = fc * WGK[7]
        var gauss = fc * WG[3]
        for (j in 0 until 7) {
            val x = h * XGK[j]
            val sum = f(c - x) + f(c + x)
            kronrod += WGK[j] * sum
            if (j % 2 == 1) gauss += WG[j / 2] * sum
        }
        return Segment(a, b, kronrod * h, abs((kronrod - gauss) * h))
    }

    /** Adaptive Gauss–Kronrod integration, accurate to about 10 digits for smooth functions. */
    fun integrate(f: (Double) -> Double, a: Double, b: Double): Double {
        if (a == b) return 0.0
        if (!a.isFinite() || !b.isFinite()) throw MathError("Limits must be finite")
        val segments = mutableListOf(kronrod(f, a, b))
        repeat(2000) {
            val total = segments.sumOf { it.value }
            val err = segments.sumOf { it.error }
            if (err <= max(1e-12, 1e-11 * abs(total))) return check(total)
            val worst = segments.maxBy { it.error }
            segments.remove(worst)
            val mid = (worst.a + worst.b) / 2
            segments += kronrod(f, worst.a, mid)
            segments += kronrod(f, mid, worst.b)
        }
        return check(segments.sumOf { it.value })
    }

    /** Ridders' method: central differences extrapolated to step size 0. */
    fun derivative(f: (Double) -> Double, x: Double): Double {
        val n = 10
        val con = 1.4
        val con2 = con * con
        var h = 0.1 * max(1.0, abs(x))
        val table = Array(n) { DoubleArray(n) }
        table[0][0] = (f(x + h) - f(x - h)) / (2 * h)
        var best = table[0][0]
        var err = Double.MAX_VALUE
        for (i in 1 until n) {
            h /= con
            table[0][i] = (f(x + h) - f(x - h)) / (2 * h)
            var fac = con2
            for (j in 1..i) {
                table[j][i] = (table[j - 1][i] * fac - table[j - 1][i - 1]) / (fac - 1)
                fac *= con2
                val e = max(abs(table[j][i] - table[j - 1][i]), abs(table[j][i] - table[j - 1][i - 1]))
                if (e <= err) { err = e; best = table[j][i] }
            }
            if (abs(table[i][i] - table[i - 1][i - 1]) >= 2 * err) break
        }
        return check(if (abs(best) < 1e-12) 0.0 else best)
    }

    private fun check(d: Double): Double {
        if (d.isNaN()) throw MathError("The result isn't a number")
        if (d.isInfinite()) throw MathError("The result is too large")
        return d
    }
}
