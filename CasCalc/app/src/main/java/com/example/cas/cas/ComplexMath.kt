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

    /**
     * Bessel J of real order [a] at complex [z] (principal branch, cut along the negative real
     * axis for fractional orders): the power series Σ (−1)ᵏ (z/2)^{2k+a} / (k! Γ(k+a+1)) for
     * |z| < 20, Hankel's asymptotic expansion further out.
     */
    fun besselJ(a: Double, z: CD): CD {
        if (a.isNaN() || z.re.isNaN() || z.im.isNaN()) return CD(Double.NaN)
        // Negative whole orders: J₋ₙ = (−1)ⁿ Jₙ.
        if (a < 0 && a == Math.rint(a)) return besselJ(-a, z) * CD(if ((-a).toLong() % 2 == 0L) 1.0 else -1.0)
        val m = z.abs()
        if (m == 0.0) return CD(if (a == 0.0) 1.0 else if (a > 0) 0.0 else Double.POSITIVE_INFINITY)
        if (m < 20) {
            val half = z * CD(0.5)
            // First term (z/2)^a / Γ(a + 1), then each from the last.
            var term = exp(CD(a) * ln(half)) * CD(1 / Statistics.gammaReal(a + 1))
            var sum = term
            val q = -(half * half)
            for (k in 1..300) {
                term = term * q / CD(k * (k + a))
                sum = sum + term
                if (term.abs() < 1e-17 * sum.abs() && k > 3) break
            }
            return sum
        }
        // Left half-plane: reflect, J_a(z) = e^{±iπa} J_a(−z), so the expansion stays valid.
        if (z.re < 0) {
            val sign = if (z.im >= 0) 1.0 else -1.0
            return exp(CD(0.0, sign * PI * a)) * hankel(a, -z).first
        }
        return hankel(a, z).first
    }

    /** Bessel Y of real order [a] at complex [z]: (J_a cos aπ − J₋ₐ) / sin aπ, and its limit at whole orders. */
    fun besselY(a: Double, z: CD): CD {
        if (a.isNaN() || z.re.isNaN() || z.im.isNaN()) return CD(Double.NaN)
        if (z.abs() == 0.0) return CD(Double.NEGATIVE_INFINITY)
        if (z.abs() >= 20 && z.re >= 0) return hankel(a, z).second
        fun y(nu: Double): CD = (besselJ(nu, z) * CD(kotlin.math.cos(nu * PI)) - besselJ(-nu, z)) / CD(kotlin.math.sin(nu * PI))
        // Whole orders: the average either side, which is exact to second order.
        if (a == Math.rint(a)) { val e = 1e-4; return (y(a + e) + y(a - e)) * CD(0.5) }
        return y(a)
    }

    /**
     * Hankel's expansion for large |z| (Re z ≥ 0):
     * J = √(2/πz) (P cos ω − Q sin ω), Y = √(2/πz) (P sin ω + Q cos ω), ω = z − aπ/2 − π/4,
     * with P and Q summed until their terms stop shrinking.
     */
    private fun hankel(a: Double, z: CD): Pair<CD, CD> {
        val mu = 4 * a * a
        var p = CD(1.0); var q = CD(0.0)
        var term = CD(1.0)
        var last = Double.MAX_VALUE
        for (k in 1..40) {
            // a_k = a_{k−1} (μ − (2k−1)²) / (8k), over z each time.
            term = term * CD((mu - (2 * k - 1.0) * (2 * k - 1.0)) / (8.0 * k)) / z
            val size = term.abs()
            if (size > last || size < 1e-17) break
            last = size
            // Signs of P: +, −, +… on even k; of Q: +, −… on odd k.
            when (k % 4) { 1 -> q = q + term; 2 -> p = p - term; 3 -> q = q - term; else -> p = p + term }
        }
        val w = z - CD(a * PI / 2 + PI / 4)
        val f = sqrt(CD(2 / PI) / z)
        return f * (p * cos(w) - q * sin(w)) to f * (p * sin(w) + q * cos(w))
    }

    /** Magnitude helper that doesn't overflow as easily. */
    fun abs(z: CD) = hypot(z.re, z.im)
}
