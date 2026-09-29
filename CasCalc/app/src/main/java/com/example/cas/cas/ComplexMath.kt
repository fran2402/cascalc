package com.example.cas.cas

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt

/** Elementary and special functions of a complex variable (principal branches). */
object ComplexMath {
    private val I = CD(0.0, 1.0)
    private val ONE = CD(1.0)

    fun sin(z: CD) = CD(sin(z.re) * cosh(z.im), cos(z.re) * sinh(z.im))
    fun cos(z: CD) = CD(cos(z.re) * cosh(z.im), -sin(z.re) * sinh(z.im))
    fun tan(z: CD) = sin(z) / cos(z)
    fun sinh(z: CD) = CD(sinh(z.re) * cos(z.im), cosh(z.re) * sin(z.im))
    fun cosh(z: CD) = CD(cosh(z.re) * cos(z.im), sinh(z.re) * sin(z.im))
    fun tanh(z: CD) = sinh(z) / cosh(z)

    fun sqrt(z: CD): CD {
        val r = z.abs()
        if (r == 0.0) return CD(0.0)
        val re = sqrt((r + z.re) / 2)
        val im = sqrt((r - z.re) / 2)
        return CD(re, if (z.im < 0) -im else im)
    }

    fun ln(z: CD) = CD(kotlin.math.ln(z.abs()), z.arg())
    fun exp(z: CD): CD { val m = exp(z.re); return CD(m * cos(z.im), m * sin(z.im)) }

    // asin z = −i ln(iz + √(1 − z²)), and so on.
    fun asin(z: CD) = CD(0.0, -1.0) * ln(I * z + sqrt(ONE - z * z))
    fun acos(z: CD) = CD(PI / 2) - asin(z)
    fun atan(z: CD) = CD(0.0, 0.5) * (ln(ONE - I * z) - ln(ONE + I * z))
    fun asinh(z: CD) = ln(z + sqrt(z * z + ONE))
    fun acosh(z: CD) = ln(z + sqrt(z + ONE) * sqrt(z - ONE))
    fun atanh(z: CD) = CD(0.5) * (ln(ONE + z) - ln(ONE - z))

    private val LANCZOS = doubleArrayOf(
        0.99999999999980993, 676.5203681218851, -1259.1392167224028, 771.32342877765313,
        -176.61502916214059, 12.507343278686905, -0.13857109526572012,
        9.9843695780195716e-6, 1.5056327351493116e-7,
    )

    /** Γ(z) by the Lanczos approximation, with the reflection formula for Re z < ½. */
    fun gamma(z: CD): CD {
        if (z.re < 0.5) return CD(PI) / (sin(CD(PI) * z) * gamma(ONE - z))
        val zm = z - ONE
        var a = CD(LANCZOS[0])
        for (k in 1 until 9) a = a + CD(LANCZOS[k]) / (zm + CD(k.toDouble()))
        val t = zm + CD(7.5)
        // √(2π) t^(z − ½) e^(−t) a
        return CD(sqrt(2 * PI)) * exp((zm + CD(0.5)) * ln(t) - t) * a
    }

    /**
     * Riemann ζ(s): Borwein's algorithm for the alternating (eta) series when
     * Re s ≥ ½, and the functional equation for Re s < ½. The pole at s = 1 is ∞.
     */
    fun zeta(s: CD): CD {
        if (abs(s.re - 1) < 1e-12 && abs(s.im) < 1e-12) return CD(Double.POSITIVE_INFINITY)
        if (s.re < 0.5) {
            // ζ(s) = 2^s π^(s−1) sin(πs/2) Γ(1−s) ζ(1−s)
            val one = ONE - s
            return exp(s * CD(ln(2.0))) * exp((s - ONE) * CD(ln(PI))) * sin(CD(PI / 2) * s) * gamma(one) * zeta(one)
        }
        val n = 40
        val d = DoubleArray(n + 1)
        var sum = 0.0
        // d_k = n Σ_{i=0}^{k} (n+i−1)! 4^i / ((n−i)! (2i)!)
        var t = 1.0 / n
        for (i in 0..n) {
            if (i > 0) t *= 4.0 * (n + i - 1) * (n - i + 1) / ((2.0 * i) * (2.0 * i - 1))
            sum += t
            d[i] = n * sum
        }
        var acc = CD(0.0)
        for (k in 0 until n) {
            val sign = if (k % 2 == 0) 1.0 else -1.0
            // (k+1)^(−s)
            val p = exp(CD(-ln(k + 1.0)) * s)
            acc = acc + CD(sign * (d[k] - d[n])) * p
        }
        val eta = CD(-1.0 / d[n]) * acc
        // ζ = η / (1 − 2^(1−s))
        return eta / (ONE - exp((ONE - s) * CD(ln(2.0))))
    }

    /** Magnitude helper that doesn't overflow as easily. */
    fun abs(z: CD) = hypot(z.re, z.im)
}
