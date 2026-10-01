package com.example.cas.cas

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sinh

/** Double-precision complex number for approximate evaluation. */
data class CD(val re: Double, val im: Double = 0.0) {
    operator fun plus(o: CD) = CD(re + o.re, im + o.im)
    operator fun minus(o: CD) = CD(re - o.re, im - o.im)
    operator fun times(o: CD) = CD(re * o.re - im * o.im, re * o.im + im * o.re)
    operator fun div(o: CD): CD {
        val d = o.re * o.re + o.im * o.im
        if (d == 0.0) throw MathError("Can't divide by 0")
        return CD((re * o.re + im * o.im) / d, (im * o.re - re * o.im) / d)
    }
    operator fun unaryMinus() = CD(-re, -im)
    val isReal get() = abs(im) <= 1e-12 * maxOf(1.0, abs(re))
    fun abs() = hypot(re, im)
    fun arg() = atan2(im, re)
    fun ln(): CD {
        if (re == 0.0 && im == 0.0) throw MathError("ln(0) is undefined")
        return CD(kotlin.math.ln(abs()), arg())
    }
    fun exp(): CD { val m = exp(re); return CD(m * cos(im), m * sin(im)) }
    fun pow(e: CD): CD {
        if (re == 0.0 && im == 0.0) {
            if (e.re > 0) return CD(0.0)
            throw MathError("Can't divide by 0")
        }
        if (im == 0.0 && e.im == 0.0 && (re > 0 || e.re == Math.rint(e.re))) return CD(Math.pow(re, e.re))
        return (ln() * e).exp()
    }
    fun real(what: String): Double {
        if (!isReal) throw MathError("$what needs a real number")
        return re
    }
}

/** Approximate value of an expression; variables come from [env]. */
object Numeric {
    fun eval(e: Expr, env: Map<String, Double> = emptyMap()): CD = ev(e, env)

    /** ∫ₐᵇ body d(var) by Gauss–Kronrod, with the outer variables' values in [env] (for double integrals). */
    private fun heldIntegral(e: Fn, env: Map<String, Double>): CD {
        val v = (e.args[1] as Sym).name
        val a = ev(e.args[2], env).real("A limit")
        val b = ev(e.args[3], env).real("A limit")
        return CD(Numerics.integrate({ t -> ev(e.args[0], env + (v to t)).real("The integrand") }, a, b))
    }

    fun real(e: Expr, env: Map<String, Double> = emptyMap()): Double = ev(e, env).real("This")

    private fun ev(e: Expr, env: Map<String, Double>): CD = if (e is Fn && e.name == "integral") heldIntegral(e, env) else when (e) {
        is Num -> CD(e.q.toDouble())
        is Flt -> CD(e.d)
        is Sym -> when (e.name) {
            "π" -> CD(PI)
            "e" -> CD(Math.E)
            "i" -> CD(0.0, 1.0)
            "∞" -> CD(Double.POSITIVE_INFINITY)
            else -> CD(env[e.name] ?: throw MathError("${e.name} has no value"))
        }
        is Add -> e.terms.fold(CD(0.0)) { a, t -> a + ev(t, env) }
        is Mul -> e.factors.fold(CD(1.0)) { a, t -> a * ev(t, env) }
        is Pow -> ev(e.base, env).pow(ev(e.exp, env))
        // Held Σ and Π bind their variable, so they're added up before the arguments are evaluated.
        is Fn -> if ((e.name == "sum" || e.name == "product") && e.args.size == 4) heldSum(e, env) else fn(e, e.args.map { ev(it, env) })
        else -> throw MathError("That isn't a number")
    }

    /** A held Σ or Π with numeric bounds, added up term by term. */
    /**
     * Σ_{k≥lo} term(k) added up until the terms stop mattering (at most [max] of them); for an
     * alternating series the mean of the last two partial sums, which converges much faster.
     */
    fun series(lo: Long, max: Int = 200_000, term: (Long) -> CD): CD {
        var acc = CD(0.0)
        var prev = CD(0.0)
        var small = 0
        var k = lo
        var last = CD(0.0)
        for (n in 0 until max) {
            val t = term(k)
            if (t.re.isNaN() || t.im.isNaN()) return CD(Double.NaN)
            prev = acc
            acc = acc + t
            last = t
            small = if (t.abs() <= 1e-15 * maxOf(acc.abs(), 1e-300)) small + 1 else 0
            if (small >= 5) return acc
            k++
        }
        // Not settled: an alternating series lands between its last two partial sums.
        return if (last.abs() < prev.abs() + acc.abs()) (prev + acc) * CD(0.5) else CD(Double.NaN)
    }

    private fun heldSum(e: Fn, env: Map<String, Double>): CD {
        val k = (e.args[1] as? Sym)?.name ?: return ev(e.args[0], env)
        if (e.name == "sum" && e.args[3] == INF) {
            val lo0 = Math.round(real(e.args[2], env))
            return series(lo0) { i -> ev(e.args[0], env + (k to i.toDouble())) }
        }
        val lo = Math.round(real(e.args[2], env))
        val hi = Math.round(real(e.args[3], env))
        if (hi - lo > 2_000_00L) throw MathError("That's too many terms")
        var acc = if (e.name == "product") CD(1.0) else CD(0.0)
        var i = lo
        while (i <= hi) {
            val v = ev(e.args[0], env + (k to i.toDouble()))
            acc = if (e.name == "product") acc * v else acc + v
            i++
        }
        return acc
    }

    private fun fn(e: Fn, a: List<CD>): CD {
        val x = a[0]
        fun r() = x.real(e.name)
        return when (e.name) {
            "sin" -> if (x.isReal) CD(sin(x.re)) else CD(sin(x.re) * cosh(x.im), cos(x.re) * sinh(x.im))
            "cos" -> if (x.isReal) CD(cos(x.re)) else CD(cos(x.re) * cosh(x.im), -sin(x.re) * sinh(x.im))
            "tan" -> if (!x.isReal) ComplexMath.tan(x) else {
                val c = cos(r())
                if (abs(c) < 1e-15) throw MathError("tan is undefined here")
                CD(sin(x.re) / c)
            }
            "gamma" -> if (x.isReal) CD(gamma(x.re)) else ComplexMath.gamma(x)
            "zeta" -> ComplexMath.zeta(x)
            "asin" -> { check(abs(r()) <= 1, "asin needs −1 ≤ x ≤ 1"); CD(kotlin.math.asin(x.re)) }
            "acos" -> { check(abs(r()) <= 1, "acos needs −1 ≤ x ≤ 1"); CD(kotlin.math.acos(x.re)) }
            "atan" -> if (x.isReal) CD(kotlin.math.atan(x.re)) else ComplexMath.atan(x)
            "sinh" -> if (x.isReal) CD(sinh(x.re)) else ComplexMath.sinh(x)
            "cosh" -> if (x.isReal) CD(cosh(x.re)) else ComplexMath.cosh(x)
            "tanh" -> if (x.isReal) CD(kotlin.math.tanh(x.re)) else ComplexMath.tanh(x)
            "asinh" -> if (x.isReal) CD(kotlin.math.asinh(x.re)) else ComplexMath.asinh(x)
            "acosh" -> { check(r() >= 1, "acosh needs x ≥ 1"); CD(kotlin.math.acosh(x.re)) }
            "atanh" -> { check(abs(r()) < 1, "atanh needs −1 < x < 1"); CD(kotlin.math.atanh(x.re)) }
            "ln" -> x.ln()
            "log" -> a[1].ln() / x.ln()
            "abs" -> CD(x.abs())
            "floor" -> CD(kotlin.math.floor(r()))
            "ceil" -> CD(kotlin.math.ceil(r()))
            "round" -> CD(Math.floor(r() + 0.5))
            "Re" -> CD(x.re)
            "Im" -> CD(x.im)
            "conj" -> CD(x.re, -x.im)
            "arg" -> CD(x.arg())
            "fact" -> CD(gamma(r() + 1))
            "binom" -> {
                val n = r(); val k = a[1].real("binom")
                CD(gamma(n + 1) / (gamma(k + 1) * gamma(n - k + 1)))
            }
            "mod" -> {
                val p = r(); val q = a[1].real("mod")
                if (q == 0.0) throw MathError("mod 0 is undefined")
                CD(p - q * kotlin.math.floor(p / q))
            }
            "sgn" -> CD(kotlin.math.sign(r()))
            "erf" -> CD(Statistics.erf(r()))
            "digamma" -> CD(Statistics.digamma(r()))
            "trigamma" -> ComplexMath.trigamma(x)
            "hurwitz" -> ComplexMath.hurwitz(x, a[1].real("hurwitz"))
            "si" -> Special.si(x)
            "ci" -> Special.ci(x)
            "shi" -> Special.shi(x)
            "chi" -> Special.chi(x)
            "ei" -> Special.ei(x)
            "li" -> Special.li(x)
            "erfi" -> Special.erfi(x)
            "fresnels" -> Special.fresnelS(x)
            "fresnelc" -> Special.fresnelC(x)
            "gammainc" -> CD(Special.gammaUpper(x.real("Γ"), a[1].real("Γ")))
            "ellipticf" -> CD(Special.ellipticF(x.real("F"), a[1].real("F")))
            "elliptice" -> CD(Special.ellipticE(x.real("E"), a[1].real("E")))
            "polylog" -> ComplexMath.polylog(x, a[1]).also { if (it.re.isNaN()) throw MathError("The polylogarithm needs |z| ≤ 1") }
            "zetaprime2" -> ComplexMath.zetaDerivative(x, 2)
            "lambertw" -> CD(Statistics.lambertW(r()))
            "besselj" -> CD(Statistics.besselJ(a[0].real("besselj"), a[1].real("besselj")))
            "bessely" -> CD(Statistics.besselY(a[0].real("bessely"), a[1].real("bessely")))
            // ζ′(s) by a central difference of ζ (accurate to about 10 digits).
            "zetaprime" -> {
                val h = 1e-5
                (ComplexMath.zeta(x + CD(h)) - ComplexMath.zeta(x - CD(h))) * CD(1 / (2 * h))
            }
            "perm" -> CD(gamma(r() + 1) / gamma(r() - a[1].real("perm") + 1))
            "invnorm" -> CD(Statistics.inverseNormal(r()))
            "frac" -> CD(r() - kotlin.math.floor(r()))
            "min" -> CD(minOf(r(), a[1].real("min")))
            "max" -> CD(maxOf(r(), a[1].real("max")))
            else -> throw MathError("Can't evaluate ${e.name} numerically")
        }
    }

    private fun check(ok: Boolean, message: String) { if (!ok) throw MathError(message) }

    /** Lanczos approximation of Γ(x). */
    fun gamma(x: Double): Double {
        if (x == Math.rint(x) && x <= 0) throw MathError("Γ is undefined at 0 and negative integers")
        if (x < 0.5) return PI / (sin(PI * x) * gamma(1 - x))
        val c = doubleArrayOf(
            0.99999999999980993, 676.5203681218851, -1259.1392167224028, 771.32342877765313,
            -176.61502916214059, 12.507343278686905, -0.13857109526572012,
            9.9843695780195716e-6, 1.5056327351493116e-7,
        )
        val xm = x - 1
        var s = c[0]
        val t = xm + 7.5
        for (i in 1 until 9) s += c[i] / (xm + i)
        return kotlin.math.sqrt(2 * PI) * Math.pow(t, xm + 0.5) * exp(-t) * s
    }

    /** Turns constant sub-expressions into decimals, keeping variables: x² + √2·x → x² + 1.414…·x. */
    fun approx(e: Expr): Expr {
        if (e.isConstant && e !is Mat && e !is Eq && e !is Seq && e !is Rel && e !is Num && !e.contains { it == INF }) {
            val v = runCatching { eval(e) }.getOrNull()
            if (v != null && v.re.isFinite() && v.im.isFinite()) return fromCD(v)
        }
        return when (e) {
            is Num -> if (e.q.isInteger) e else Flt(e.q.toDouble())
            is Add -> Add(e.terms.map { approx(it) })
            is Mul -> Mul(e.factors.map { approx(it) })
            is Pow -> if (e.exp is Num && (e.exp.q.isInteger)) Pow(approx(e.base), e.exp) else Pow(approx(e.base), approx(e.exp))
            is Fn -> if ((e.name == "sum" || e.name == "product") && e.args.size == 4) fromCD(eval(e)) else Fn(e.name, e.args.map { approx(it) })
            is Mat -> Mat(e.rows, e.cols, e.cells.map { approx(it) })
            is Eq -> Eq(approx(e.lhs), approx(e.rhs))
            is Seq -> Seq(e.items.map { approx(it) }, e.joiner)
            is Rel -> Rel(e.parts.map { approx(it) }, e.ops)
            else -> e
        }
    }

    fun fromCD(v: CD): Expr {
        val re = if (abs(v.re) < 1e-300) 0.0 else v.re
        if (v.isReal) return Flt(re)
        val imPart = if (v.im == 1.0) I else Mul(listOf(Flt(v.im), I))
        return if (re == 0.0) imPart else Add(listOf(Flt(re), imPart))
    }
}
