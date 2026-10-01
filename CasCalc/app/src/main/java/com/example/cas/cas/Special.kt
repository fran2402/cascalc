package com.example.cas.cas

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Special functions that integrals come out in: the exponential, sine, cosine and logarithmic
 * integrals (Ei, E₁, Si, Ci, Shi, Chi, li), erfi, the Fresnel integrals S and C (normalized,
 * S(x) = ∫₀ˣ sin(πt²/2) dt), the upper incomplete gamma function Γ(s, x), and the incomplete
 * elliptic integrals F(φ | m) and E(φ | m). Complex arguments where it's natural.
 *
 * Real arguments below 0 follow the conventions of ln|x|, so derivatives stay right on both
 * sides: Ci(−x) = Ci(x), Chi(−x) = Chi(x).
 */
object Special {
    private val ONE = CD(1.0)
    private val I = CD(0.0, 1.0)
    const val EULER_GAMMA = 0.57721566490153286061

    private fun ln(z: CD) = ComplexMath.ln(z)
    private fun exp(z: CD) = ComplexMath.exp(z)

    // ---- E₁ and Ei -----------------------------------------------------------------------------

    /** E₁(z) = ∫_z^∞ e^(−t)/t dt: the series near 0, the continued fraction further out. */
    fun e1(z: CD): CD {
        if (z.abs() == 0.0) return CD(Double.POSITIVE_INFINITY)
        if (z.abs() <= 2.0 || (z.re < 0 && abs(z.im) < z.abs() * 0.5 && z.abs() < 40)) {
            // −γ − ln z − Σ (−z)ᵏ/(k·k!)
            var sum = CD(0.0)
            var term = ONE
            for (k in 1..400) {
                term = term * (-z) / CD(k.toDouble())
                val t = term / CD(k.toDouble())
                sum = sum + t
                if (t.abs() < 1e-17 * sum.abs()) break
            }
            return CD(-EULER_GAMMA) - ln(z) - sum
        }
        // Lentz: E₁(z) = e^(−z) / (z + 1 − 1²/(z + 3 − 2²/(z + 5 − …)))
        val tiny = 1e-300
        var b = z + ONE
        var c = CD(1 / tiny)
        var d = ONE / b
        var h = d
        for (k in 1..2000) {
            val a = CD(-(k.toDouble() * k))
            b = b + CD(2.0)
            d = ONE / (a * d + b)
            c = b + a / c
            val delta = c * d
            h = h * delta
            if ((delta - ONE).abs() < 1e-16) break
        }
        return h * exp(-z)
    }

    /** Ei(z) = −PV ∫_{−z}^∞ e^(−t)/t dt: γ + ln z + Σ zᵏ/(k·k!), asymptotically eᶻ/z Σ k!/zᵏ. */
    fun ei(z: CD): CD {
        if (z.abs() == 0.0) return CD(Double.NEGATIVE_INFINITY)
        if (z.abs() < 40) {
            var sum = CD(0.0)
            var term = ONE
            for (k in 1..500) {
                term = term * z / CD(k.toDouble())
                val t = term / CD(k.toDouble())
                sum = sum + t
                if (t.abs() < 1e-17 * sum.abs()) break
            }
            // For negative reals, the real part (as ln|x|).
            val log = if (z.im == 0.0) CD(kotlin.math.ln(abs(z.re))) else ln(z)
            return CD(EULER_GAMMA) + log + sum
        }
        if (z.im == 0.0 && z.re > 0) {
            var sum = ONE; var term = ONE
            for (k in 1..60) {
                val next = term * CD(k / z.re)
                if (next.abs() > term.abs()) break
                term = next; sum = sum + term
            }
            return exp(z) / z * sum
        }
        // Ei(z) = −E₁(−z) ± iπ (the sign of Im z); real negative z: −E₁(−z).
        val s = if (z.im > 0) 1.0 else if (z.im < 0) -1.0 else 0.0
        return -e1(-z) + CD(0.0, s * PI)
    }

    // ---- Si, Ci, Shi, Chi, li ------------------------------------------------------------------

    fun si(z: CD): CD {
        if (z.re < 0) return -si(-z)
        if (z.abs() <= 4.0) {
            // Σ (−1)ᵏ z^(2k+1) / ((2k+1)(2k+1)!)
            var sum = CD(0.0); var term = z
            val z2 = z * z
            for (k in 0..200) {
                val t = term / CD(2.0 * k + 1)
                sum = sum + t
                if (t.abs() < 1e-17 * sum.abs()) break
                term = -term * z2 / CD((2.0 * k + 2) * (2.0 * k + 3))
            }
            return sum
        }
        // Si(z) = π/2 + (E₁(iz) − E₁(−iz))/(2i)
        return CD(PI / 2) + (e1(I * z) - e1(-(I * z))) / CD(0.0, 2.0)
    }

    fun ci(z: CD): CD {
        if (z.im == 0.0 && z.re < 0) return ci(CD(-z.re))
        if (z.abs() <= 4.0) {
            // γ + ln z + Σ (−1)ᵏ z^(2k) / (2k (2k)!)
            var sum = CD(0.0); var term = ONE
            val z2 = z * z
            for (k in 1..200) {
                term = -term * z2 / CD((2.0 * k - 1) * (2.0 * k))
                val t = term / CD(2.0 * k)
                sum = sum + t
                if (t.abs() < 1e-17 * sum.abs()) break
            }
            return CD(EULER_GAMMA) + ln(z) + sum
        }
        // Ci(z) = −(E₁(iz) + E₁(−iz))/2 for Re z > 0
        val w = if (z.re < 0) -z else z
        val c = -(e1(I * w) + e1(-(I * w))) * CD(0.5)
        return if (z.re < 0) c + CD(0.0, if (z.im >= 0) PI else -PI) else c
    }

    fun shi(z: CD): CD {
        if (z.abs() <= 4.0) {
            var sum = CD(0.0); var term = z
            val z2 = z * z
            for (k in 0..200) {
                val t = term / CD(2.0 * k + 1)
                sum = sum + t
                if (t.abs() < 1e-17 * sum.abs()) break
                term = term * z2 / CD((2.0 * k + 2) * (2.0 * k + 3))
            }
            return sum
        }
        return (ei(z) - ei(-z)) * CD(0.5)
    }

    fun chi(z: CD): CD {
        if (z.im == 0.0 && z.re < 0) return chi(CD(-z.re))
        if (z.abs() <= 4.0) {
            var sum = CD(0.0); var term = ONE
            val z2 = z * z
            for (k in 1..200) {
                term = term * z2 / CD((2.0 * k - 1) * (2.0 * k))
                val t = term / CD(2.0 * k)
                sum = sum + t
                if (t.abs() < 1e-17 * sum.abs()) break
            }
            return CD(EULER_GAMMA) + ln(z) + sum
        }
        return (ei(z) + ei(-z)) * CD(0.5)
    }

    /** li(z) = Ei(ln z), the logarithmic integral. */
    fun li(z: CD): CD = if (z.im == 0.0 && z.re > 0) ei(CD(kotlin.math.ln(z.re))) else ei(ln(z))

    // ---- erfi and the Fresnel integrals ---------------------------------------------------------

    fun erfi(z: CD): CD = -(I * ComplexMath.erf(I * z))

    private val ROOT_PI_HALF = sqrt(PI) / 2

    /** S(z) = ∫₀ᶻ sin(πt²/2) dt = (1 + i)/4 [erf(√π(1 + i)z/2) − i erf(√π(1 − i)z/2)]. */
    fun fresnelS(z: CD): CD {
        val a = ComplexMath.erf(CD(ROOT_PI_HALF, ROOT_PI_HALF) * z)
        val b = ComplexMath.erf(CD(ROOT_PI_HALF, -ROOT_PI_HALF) * z)
        return CD(0.25, 0.25) * (a - I * b)
    }

    /** C(z) = ∫₀ᶻ cos(πt²/2) dt = (1 − i)/4 [erf(√π(1 + i)z/2) + i erf(√π(1 − i)z/2)]. */
    fun fresnelC(z: CD): CD {
        val a = ComplexMath.erf(CD(ROOT_PI_HALF, ROOT_PI_HALF) * z)
        val b = ComplexMath.erf(CD(ROOT_PI_HALF, -ROOT_PI_HALF) * z)
        return CD(0.25, -0.25) * (a + I * b)
    }

    // ---- Γ(s, x) ---------------------------------------------------------------------------------

    /** The upper incomplete gamma function Γ(s, x) = ∫ₓ^∞ t^(s−1) e^(−t) dt, for real s and x ≥ 0. */
    fun gammaUpper(s: Double, x: Double): Double {
        if (x < 0 || s.isNaN() || x.isNaN()) return Double.NaN
        if (x == 0.0) return if (s > 0) Statistics.gammaReal(s) else Double.POSITIVE_INFINITY
        // Down to s > 0: Γ(s, x) = (Γ(s + 1, x) − xˢ e^(−x)) / s; s = 0 is E₁.
        if (s <= 0) {
            if (s == 0.0) return e1(CD(x)).re
            return (gammaUpper(s + 1, x) - Math.pow(x, s) * kotlin.math.exp(-x)) / s
        }
        val lead = kotlin.math.exp(-x + s * kotlin.math.ln(x))
        if (x < s + 1) {
            // Γ(s) − γ(s, x), with γ(s, x) = xˢ e^(−x) Σ xⁿ / (s(s+1)…(s+n))
            var term = 1.0 / s; var sum = term; var a = s
            for (n in 1..1000) { a += 1; term *= x / a; sum += term; if (abs(term) < abs(sum) * 1e-17) break }
            return Statistics.gammaReal(s) - sum * lead
        }
        // Continued fraction (Lentz).
        val tiny = 1e-300
        var b = x + 1 - s
        var c = 1 / tiny
        var d = 1 / b
        var h = d
        for (i in 1..1000) {
            val an = -i * (i - s)
            b += 2
            d = an * d + b; if (abs(d) < tiny) d = tiny
            c = b + an / c; if (abs(c) < tiny) c = tiny
            d = 1 / d
            val delta = d * c
            h *= delta
            if (abs(delta - 1) < 1e-16) break
        }
        return lead * h
    }

    // ---- Elliptic integrals ---------------------------------------------------------------------

    /** Carlson's R_F(x, y, z). */
    private fun rf(x0: Double, y0: Double, z0: Double): Double {
        var x = x0; var y = y0; var z = z0
        for (i in 0..200) {
            val sx = sqrt(x); val sy = sqrt(y); val sz = sqrt(z)
            val l = sx * sy + sx * sz + sy * sz
            x = (x + l) / 4; y = (y + l) / 4; z = (z + l) / 4
            val a = (x + y + z) / 3
            if (maxOf(abs(a - x), abs(a - y), abs(a - z)) < 1e-12 * abs(a)) break
        }
        val a = (x + y + z) / 3
        val dx = 1 - x / a; val dy = 1 - y / a; val dz = 1 - z / a
        val e2 = dx * dy - dz * dz; val e3 = dx * dy * dz
        return (1 - e2 / 10 + e3 / 14 + e2 * e2 / 24 - 3 * e2 * e3 / 44) / sqrt(a)
    }

    /** Carlson's R_D(x, y, z). */
    private fun rd(x0: Double, y0: Double, z0: Double): Double {
        var x = x0; var y = y0; var z = z0
        var sum = 0.0; var f = 1.0
        for (i in 0..200) {
            val sx = sqrt(x); val sy = sqrt(y); val sz = sqrt(z)
            val l = sx * sy + sx * sz + sy * sz
            sum += f / (sz * (z + l))
            f /= 4
            x = (x + l) / 4; y = (y + l) / 4; z = (z + l) / 4
            val a = (x + y + 3 * z) / 5
            if (maxOf(abs(a - x), abs(a - y), abs(a - z)) < 1e-12 * abs(a)) break
        }
        val a = (x + y + 3 * z) / 5
        val dx = 1 - x / a; val dy = 1 - y / a; val dz = 1 - z / a
        val ea = dx * dy; val eb = dz * dz; val ec = ea - eb; val ed = ea - 6 * eb; val ee = ed + ec + ec
        val s = 1 + ed * (-3.0 / 14 + 9.0 / 88 * ed - 9.0 / 52 * dz * ee) +
            dz * (ee / 6 + dz * (-9.0 / 22 * ec + dz * 3.0 / 26 * ea))
        return 3 * sum + f * s / (a * sqrt(a))
    }

    /** F(φ | m) = ∫₀^φ dθ/√(1 − m sin²θ), for m < 1 (and m ≤ 1/sin²φ). */
    fun ellipticF(phi: Double, m: Double): Double {
        val n = Math.rint(phi / PI)
        val p = phi - n * PI
        val s = kotlin.math.sin(p); val c = kotlin.math.cos(p)
        val part = s * rf(c * c, 1 - m * s * s, 1.0)
        return if (n == 0.0) part else part + 2 * n * rf(0.0, 1 - m, 1.0)
    }

    /** E(φ | m) = ∫₀^φ √(1 − m sin²θ) dθ. */
    fun ellipticE(phi: Double, m: Double): Double {
        fun piece(p: Double): Double {
            val s = kotlin.math.sin(p); val c = kotlin.math.cos(p)
            val y = 1 - m * s * s
            return s * rf(c * c, y, 1.0) - m / 3 * s * s * s * rd(c * c, y, 1.0)
        }
        val n = Math.rint(phi / PI)
        val part = piece(phi - n * PI)
        return if (n == 0.0) part else part + 2 * n * (rf(0.0, 1 - m, 1.0) - m / 3 * rd(0.0, 1 - m, 1.0))
    }
}
