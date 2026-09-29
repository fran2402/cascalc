package com.example.cas.cas

import com.example.cas.math.Rational
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Statistics on lists of values, counting, and the normal and binomial
 * distributions. Exact wherever the inputs are (the mean of 1, 2, 4 is 7/3;
 * the sample standard deviation of 2, 4, 4, 4, 5, 5, 7, 9 is 2.138… = √(32/7)).
 */
object Statistics {
    private fun sorted(xs: List<Expr>): List<Expr> {
        if (xs.isEmpty()) throw MathError("The list is empty")
        if (!xs.all { it.isConstant }) throw MathError("The values must be numbers")
        return xs.sortedBy { Numeric.real(it) }
    }

    fun mean(xs: List<Expr>): Expr {
        if (xs.isEmpty()) throw MathError("The list is empty")
        return Algebra.simplify(div(add(xs), Num(xs.size.toLong())))
    }

    fun median(xs: List<Expr>): Expr {
        val s = sorted(xs)
        val n = s.size
        return if (n % 2 == 1) s[n / 2] else Algebra.simplify(div(add(s[n / 2 - 1], s[n / 2]), TWO))
    }

    private fun sumOfSquares(xs: List<Expr>): Expr {
        val m = mean(xs)
        return add(xs.map { pow(sub(it, m), TWO) })
    }

    /** Sample variance s² (divides by n − 1). */
    fun variance(xs: List<Expr>): Expr {
        if (xs.size < 2) throw MathError("The sample variance needs at least two values")
        return Algebra.simplify(Algebra.expand(div(sumOfSquares(xs), Num((xs.size - 1).toLong()))))
    }

    /** Sample standard deviation s. */
    fun sampleSd(xs: List<Expr>): Expr = Algebra.simplify(sqrt(variance(xs)))

    /** Population standard deviation σ (divides by n). */
    fun populationSd(xs: List<Expr>): Expr =
        Algebra.simplify(sqrt(Algebra.simplify(Algebra.expand(div(sumOfSquares(xs), Num(xs.size.toLong()))))))

    /** Permutations nPr = n!/(n − r)!. */
    fun permutations(n: Expr, r: Expr): Expr {
        val nq = (n as? Num)?.q; val rq = (r as? Num)?.q
        if (nq != null && rq != null && nq.isInteger && rq.isInteger) {
            if (rq.signum < 0 || rq > nq) return ZERO
            var p = java.math.BigInteger.ONE
            var k = nq.num
            repeat(rq.num.toInt()) { p *= k; k -= java.math.BigInteger.ONE }
            return Num(Rational.of(p))
        }
        return Fn("perm", listOf(n, r))
    }

    /** The normal density with mean μ and standard deviation σ, as a formula (so it can be graphed). */
    fun normalPdf(x: Expr, mu: Expr, sigma: Expr): Expr =
        Algebra.simplify(div(pow(E, neg(div(pow(sub(x, mu), TWO), mul(TWO, pow(sigma, TWO))))), mul(sigma, sqrt(mul(TWO, com.example.cas.cas.PI)))))

    /** The normal distribution function Φ = (1 + erf((x − μ)/(σ√2)))/2. */
    fun normalCdf(x: Expr, mu: Expr, sigma: Expr): Expr =
        Algebra.simplify(div(add(ONE, fn("erf", div(sub(x, mu), mul(sigma, sqrt(TWO))))), TWO))

    /** P(X = k) for X ~ Binomial(n, p): exact when p is. */
    fun binomialPmf(n: Expr, p: Expr, k: Expr): Expr =
        Algebra.simplify(mul(fn("binom", n, k), pow(p, k), pow(sub(ONE, p), sub(n, k))))

    /** P(X = k) for X ~ Poisson(λ): λᵏ e^(−λ) / k!. */
    fun poissonPmf(lambda: Expr, k: Expr): Expr =
        Algebra.simplify(div(mul(pow(lambda, k), pow(E, neg(lambda))), fn("fact", k)))

    /** P(X ≤ k) for X ~ Binomial(n, p), summed exactly. */
    fun binomialCdf(n: Expr, p: Expr, k: Expr): Expr {
        val kq = (k as? Num)?.q?.takeIf { it.isInteger && it.signum >= 0 } ?: throw MathError("k must be a whole number")
        val top = kq.num.toInt().coerceAtMost(((n as? Num)?.q?.num?.toInt()) ?: Int.MAX_VALUE)
        if (top > 2000) throw MathError("k is too large")
        return Algebra.simplify(add((0..top).map { i -> binomialPmf(n, p, Num(i.toLong())) }))
    }

    fun total(xs: List<Expr>): Expr = Algebra.simplify(add(xs))

    // ---- Numerics --------------------------------------------------------------------------

    /** erf by its Taylor series near 0 and a continued fraction for erfc further out (≈ 15 digits). */
    fun erf(x: Double): Double {
        if (x.isNaN()) return x
        if (x < 0) return -erf(-x)
        if (x < 3.0) {
            // 2/√π Σ (−1)ⁿ x^(2n+1) / (n! (2n+1))
            var term = x
            var sum = x
            var n = 0
            while (abs(term) > 1e-17 * abs(sum) && n < 200) {
                n++
                term *= -x * x / n
                sum += term / (2 * n + 1)
            }
            return 2 / sqrt(PI) * sum
        }
        return 1 - erfc(x)
    }

    /** erfc for x ≥ 3 by Lentz's continued fraction. */
    private fun erfc(x: Double): Double {
        // erfc x = e^(−x²)/√π · 1/(x + 1/2/(x + 1/(x + 3/2/(x + …))))
        val tiny = 1e-300
        var f = x
        var c = x
        var d = 0.0
        for (k in 1..300) {
            val a = k / 2.0
            d = x + a * d; if (abs(d) < tiny) d = tiny
            c = x + a / c; if (abs(c) < tiny) c = tiny
            d = 1 / d
            val delta = c * d
            f *= delta
            if (abs(delta - 1) < 1e-16) break
        }
        return exp(-x * x) / sqrt(PI) / f
    }

    fun normalCdfValue(z: Double) = 0.5 * (1 + erf(z / sqrt(2.0)))

    /**
     * The digamma function ψ = Γ′/Γ for real x: the recurrence ψ(x) = ψ(x + 1) − 1/x up to
     * x ≥ 8, then the asymptotic series; reflection ψ(1 − x) − ψ(x) = π cot πx for x < 0.
     */
    /** Lambert W (the principal branch): the w with w e^w = x, for x ≥ −1/e, by Newton's method. */
    fun lambertW(x: Double): Double {
        if (x.isNaN()) return x
        if (x < -1 / kotlin.math.E) return Double.NaN
        var w = if (x < 1) kotlin.math.max(-0.99, x * (1 - x)) else kotlin.math.ln(x) - kotlin.math.ln(kotlin.math.ln(x).coerceAtLeast(1e-9))
        repeat(60) {
            val e = kotlin.math.exp(w)
            val f = w * e - x
            val d = e * (w + 1) - (w + 2) * f / (2 * w + 2)
            if (d == 0.0) return w
            val next = w - f / d
            if (kotlin.math.abs(next - w) < 1e-15 * kotlin.math.max(1.0, kotlin.math.abs(w))) return next
            w = next
        }
        return w
    }

    /** Bessel J of order [a] at [x], by the series near 0 and the asymptotic form far out. */
    fun besselJ(a: Double, x: Double): Double = bessel(a, x, second = false)

    /** Bessel Y of order [a] at [x] (x > 0). */
    fun besselY(a: Double, x: Double): Double = bessel(a, x, second = true)

    private fun bessel(a: Double, x: Double, second: Boolean): Double {
        if (x.isNaN() || a.isNaN()) return Double.NaN
        if (second) {
            if (x <= 0) return Double.NaN
            // Y_a = (J_a cos aπ − J_{−a}) / sin aπ; whole orders are the limit of nearby ones.
            val order = if (a == Math.rint(a)) a + 1e-8 else a
            val c = kotlin.math.cos(order * PI)
            return (bessel(order, x, false) * c - bessel(-order, x, false)) / kotlin.math.sin(order * PI)
        }
        if (x == 0.0) return if (a == 0.0) 1.0 else if (a > 0) 0.0 else Double.NaN
        val ax = kotlin.math.abs(x)
        if (ax < 25) {
            // Σ (−1)ᵏ (x/2)^{2k+a} / (k! Γ(k + a + 1))
            var sum = 0.0
            var k = 0
            while (k < 200) {
                // Γ(k + a + 1) may have a negative argument (Y uses J of negative order),
                // so the gamma is taken directly rather than through its logarithm.
                val term = kotlin.math.exp((2 * k + a) * kotlin.math.ln(ax / 2) - lnGamma(k + 1.0)) / gammaReal(k + a + 1)
                sum += if (k % 2 == 0) term else -term
                if (kotlin.math.abs(term) < 1e-17 * kotlin.math.max(1.0, kotlin.math.abs(sum)) && k > 3) break
                k++
            }
            return if (x < 0 && a == Math.rint(a)) sum * (if (a.toInt() % 2 == 0) 1.0 else -1.0) else sum
        }
        // Far from 0: Hankel's expansion (whole orders are odd or even under x → −x).
        val far = ComplexMath.besselJ(a, CD(ax)).re
        return if (x < 0 && a == Math.rint(a)) far * (if (a.toLong() % 2 == 0L) 1.0 else -1.0) else far
    }

    /** Γ(x) for any x that isn't zero or a negative whole number (reflection below 0.5). */
    fun gammaReal(x: Double): Double {
        if (x >= 0.5) return kotlin.math.exp(lnGamma(x))
        if (x == Math.rint(x)) return Double.NaN
        return PI / (kotlin.math.sin(PI * x) * kotlin.math.exp(lnGamma(1 - x)))
    }

    /** ln Γ(x) for x > 0 (Lanczos), used by the Bessel series. */
    private fun lnGamma(x: Double): Double {
        if (x <= 0) return Double.NaN
        val g = doubleArrayOf(676.5203681218851, -1259.1392167224028, 771.32342877765313, -176.61502916214059,
            12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7)
        val z = x - 1
        var a = 0.99999999999980993
        for (i in g.indices) a += g[i] / (z + i + 1)
        val t = z + g.size - 0.5
        return 0.5 * kotlin.math.ln(2 * PI) + (z + 0.5) * kotlin.math.ln(t) - t + kotlin.math.ln(a)
    }

    fun digamma(x0: Double): Double {
        if (x0.isNaN()) return x0
        if (x0 <= 0 && x0 == Math.rint(x0)) return Double.NaN // poles at 0, −1, −2, …
        if (x0 < 0) return digamma(1 - x0) - PI / kotlin.math.tan(PI * x0)
        var x = x0
        var acc = 0.0
        while (x < 8) { acc -= 1 / x; x += 1 }
        val i2 = 1 / (x * x)
        val series = kotlin.math.ln(x) - 0.5 / x - i2 * (1.0 / 12 - i2 * (1.0 / 120 - i2 * (1.0 / 252 - i2 * (1.0 / 240 - i2 / 132))))
        return acc + series
    }

    /** The z with Φ(z) = p (Acklam's approximation, then Newton steps on Φ). */
    fun inverseNormal(p: Double): Double {
        if (p <= 0 || p >= 1) throw MathError("The probability must be between 0 and 1")
        val a = doubleArrayOf(-3.969683028665376e+01, 2.209460984245205e+02, -2.759285104469687e+02, 1.383577518672690e+02, -3.066479806614716e+01, 2.506628277459239e+00)
        val b = doubleArrayOf(-5.447609879822406e+01, 1.615858368580409e+02, -1.556989798598866e+02, 6.680131188771972e+01, -1.328068155288572e+01)
        val c = doubleArrayOf(-7.784894002430293e-03, -3.223964580411365e-01, -2.400758277161838e+00, -2.549732539343734e+00, 4.374664141464968e+00, 2.938163982698783e+00)
        val d = doubleArrayOf(7.784695709041462e-03, 3.224671290700398e-01, 2.445134137142996e+00, 3.754408661907416e+00)
        val lo = 0.02425
        var z = when {
            p < lo -> { val q = sqrt(-2 * kotlin.math.ln(p)); (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1) }
            p > 1 - lo -> { val q = sqrt(-2 * kotlin.math.ln(1 - p)); -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1) }
            else -> { val q = p - 0.5; val r = q * q; (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q / (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1) }
        }
        repeat(3) {
            val e = normalCdfValue(z) - p
            z -= e / (exp(-z * z / 2) / sqrt(2 * PI))
        }
        return z
    }
}
