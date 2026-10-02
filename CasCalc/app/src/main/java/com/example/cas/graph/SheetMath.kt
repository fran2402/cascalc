package com.example.cas.graph

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * The numerics behind the spreadsheet's statistics: Γ, the incomplete Γ and β functions, erf,
 * and the normal, t, χ², F and other distributions with their inverses.
 */
internal object SheetMath {
    private val LANCZOS = doubleArrayOf(
        0.99999999999980993, 676.5203681218851, -1259.1392167224028, 771.32342877765313,
        -176.61502916214059, 12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7,
    )

    fun lnGamma(x: Double): Double {
        if (x < 0.5) return ln(Math.PI / abs(kotlin.math.sin(Math.PI * x))) - lnGamma(1 - x)
        val z = x - 1
        var a = LANCZOS[0]
        val t = z + 7.5
        for (k in 1 until 9) a += LANCZOS[k] / (z + k)
        return 0.5 * ln(2 * Math.PI) + (z + 0.5) * ln(t) - t + ln(a)
    }

    fun gamma(x: Double): Double {
        if (x == floor(x) && x <= 0) return Double.NaN
        if (x < 0.5) return Math.PI / (kotlin.math.sin(Math.PI * x) * gamma(1 - x))
        if (x == floor(x) && x < 171) { var p = 1.0; for (k in 2 until x.toInt()) p *= k; return p }
        return exp(lnGamma(x))
    }

    /** The regularized lower incomplete gamma function P(a, x). */
    fun gammaP(a: Double, x: Double): Double {
        if (x <= 0) return 0.0
        if (x < a + 1) {
            var sum = 1 / a; var term = sum; var n = a
            repeat(1000) { n += 1; term *= x / n; sum += term; if (abs(term) < abs(sum) * 1e-16) return sum * exp(-x + a * ln(x) - lnGamma(a)) }
            return sum * exp(-x + a * ln(x) - lnGamma(a))
        }
        return 1 - gammaQcf(a, x)
    }

    fun gammaQ(a: Double, x: Double): Double = if (x < a + 1) 1 - gammaP(a, x) else gammaQcf(a, x)

    private fun gammaQcf(a: Double, x: Double): Double {
        val tiny = 1e-300
        var b = x + 1 - a; var c = 1 / tiny; var d = 1 / b; var h = d
        for (i in 1..1000) {
            val an = -i * (i - a); b += 2
            d = an * d + b; if (abs(d) < tiny) d = tiny
            c = b + an / c; if (abs(c) < tiny) c = tiny
            d = 1 / d; val del = d * c; h *= del
            if (abs(del - 1) < 1e-16) break
        }
        return exp(-x + a * ln(x) - lnGamma(a)) * h
    }

    /** The regularized incomplete beta function I_x(a, b). */
    fun betaI(a: Double, b: Double, x: Double): Double {
        if (x <= 0) return 0.0
        if (x >= 1) return 1.0
        val front = exp(lnGamma(a + b) - lnGamma(a) - lnGamma(b) + a * ln(x) + b * ln(1 - x))
        return if (x < (a + 1) / (a + b + 2)) front * betaCf(a, b, x) / a else 1 - front * betaCf(b, a, 1 - x) / b
    }

    private fun betaCf(a: Double, b: Double, x: Double): Double {
        val tiny = 1e-300
        var c = 1.0; var d = 1 - (a + b) * x / (a + 1); if (abs(d) < tiny) d = tiny
        d = 1 / d; var h = d
        for (m in 1..1000) {
            val m2 = 2 * m
            var aa = m * (b - m) * x / ((a + m2 - 1) * (a + m2))
            d = 1 + aa * d; if (abs(d) < tiny) d = tiny
            c = 1 + aa / c; if (abs(c) < tiny) c = tiny
            d = 1 / d; h *= d * c
            aa = -(a + m) * (a + b + m) * x / ((a + m2) * (a + m2 + 1))
            d = 1 + aa * d; if (abs(d) < tiny) d = tiny
            c = 1 + aa / c; if (abs(c) < tiny) c = tiny
            d = 1 / d; val del = d * c; h *= del
            if (abs(del - 1) < 1e-16) break
        }
        return h
    }

    fun erf(x: Double): Double = if (x < 0) -gammaP(0.5, x * x) else gammaP(0.5, x * x)
    fun erfc(x: Double): Double = if (x < 0) 1 + gammaP(0.5, x * x) else gammaQ(0.5, x * x)

    fun normCdf(z: Double) = 0.5 * erfc(-z / sqrt(2.0))
    fun normPdf(z: Double) = exp(-z * z / 2) / sqrt(2 * Math.PI)

    /** The standard normal's inverse: Acklam's approximation, polished by a Halley step. */
    fun normInv(p: Double): Double {
        if (p <= 0 || p >= 1) return Double.NaN
        val a = doubleArrayOf(-3.969683028665376e+01, 2.209460984245205e+02, -2.759285104469687e+02, 1.383577518672690e+02, -3.066479806614716e+01, 2.506628277459239e+00)
        val b = doubleArrayOf(-5.447609879822406e+01, 1.615858368580409e+02, -1.556989798598866e+02, 6.680131188771972e+01, -1.328068155288572e+01)
        val c = doubleArrayOf(-7.784894002430293e-03, -3.223964580411365e-01, -2.400758277161838e+00, -2.549732539343734e+00, 4.374664141464968e+00, 2.938163982698783e+00)
        val d = doubleArrayOf(7.784695709041462e-03, 3.224671290700398e-01, 2.445134137142996e+00, 3.754408661907416e+00)
        val lo = 0.02425
        var x = when {
            p < lo -> { val q = sqrt(-2 * ln(p)); (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1) }
            p > 1 - lo -> { val q = sqrt(-2 * ln(1 - p)); -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1) }
            else -> { val q = p - 0.5; val r = q * q; (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q / (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1) }
        }
        repeat(2) {
            val e = normCdf(x) - p
            val u = e * sqrt(2 * Math.PI) * exp(x * x / 2)
            x -= u / (1 + x * u / 2)
        }
        return x
    }

    fun tCdf(t: Double, df: Double): Double {
        val x = df / (df + t * t)
        val tail = 0.5 * betaI(df / 2, 0.5, x)
        return if (t > 0) 1 - tail else tail
    }

    fun tPdf(t: Double, df: Double) = exp(lnGamma((df + 1) / 2) - lnGamma(df / 2) - 0.5 * ln(df * Math.PI) - (df + 1) / 2 * ln(1 + t * t / df))

    fun chi2Cdf(x: Double, k: Double) = if (x <= 0) 0.0 else gammaP(k / 2, x / 2)
    fun chi2Pdf(x: Double, k: Double) = if (x < 0) 0.0 else exp((k / 2 - 1) * ln(x) - x / 2 - k / 2 * ln(2.0) - lnGamma(k / 2))

    fun fCdf(x: Double, d1: Double, d2: Double) = if (x <= 0) 0.0 else betaI(d1 / 2, d2 / 2, d1 * x / (d1 * x + d2))
    fun fPdf(x: Double, d1: Double, d2: Double) = if (x < 0) 0.0 else
        exp(0.5 * (d1 * ln(d1 * x) + d2 * ln(d2) - (d1 + d2) * ln(d1 * x + d2)) - ln(x) - (lnGamma(d1 / 2) + lnGamma(d2 / 2) - lnGamma((d1 + d2) / 2)))

    fun gammaCdf(x: Double, alpha: Double, beta: Double) = if (x <= 0) 0.0 else gammaP(alpha, x / beta)
    fun gammaPdf(x: Double, alpha: Double, beta: Double) = if (x < 0) 0.0 else exp((alpha - 1) * ln(x) - x / beta - lnGamma(alpha) - alpha * ln(beta))

    fun betaPdf(x: Double, a: Double, b: Double) = if (x < 0 || x > 1) 0.0 else exp((a - 1) * ln(x) + (b - 1) * ln(1 - x) + lnGamma(a + b) - lnGamma(a) - lnGamma(b))

    fun lnChoose(n: Double, k: Double) = lnGamma(n + 1) - lnGamma(k + 1) - lnGamma(n - k + 1)

    /**
     * The x where an increasing [cdf] reaches [p], by bisection between [lo] and [hi] (the upper
     * end doubled until it brackets p).
     */
    fun invert(p: Double, lo0: Double, hi0: Double, cdf: (Double) -> Double): Double {
        var lo = lo0; var hi = hi0
        var guard = 0
        while (cdf(hi) < p && guard++ < 2000) { lo = hi; hi *= 2 }
        if (lo0 < 0) { guard = 0; while (cdf(lo) > p && guard++ < 2000) { hi = lo; lo *= 2 } }
        repeat(300) {
            val mid = (lo + hi) / 2
            if (mid == lo || mid == hi) return mid
            if (cdf(mid) < p) lo = mid else hi = mid
        }
        return (lo + hi) / 2
    }

    /** The modified Bessel function I_n(x), from its power series. */
    fun besselI(n: Int, x: Double): Double {
        var term = (x / 2).pow(n) / exp(lnGamma(n + 1.0))
        var sum = term
        for (k in 1..500) {
            term *= (x / 2) * (x / 2) / (k * (k + n).toDouble())
            sum += term
            if (abs(term) < abs(sum) * 1e-17) break
        }
        return sum
    }

    /** The modified Bessel function K_n(x) for x > 0: ∫₀^∞ e^(−x cosh t) cosh(nt) dt, by the trapezoid rule. */
    fun besselK(n: Int, x: Double): Double {
        val h = 0.02
        var sum = 0.5 * exp(-x)
        var t = h
        while (true) {
            val f = exp(-x * kotlin.math.cosh(t)) * kotlin.math.cosh(n * t)
            sum += f
            if (f < sum * 1e-18 || t > 50) break
            t += h
        }
        return sum * h
    }
}
