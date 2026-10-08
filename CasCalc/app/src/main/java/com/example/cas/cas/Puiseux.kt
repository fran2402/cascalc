package com.example.cas.cas

import com.example.cas.math.Rational
import java.util.TreeMap

/**
 * Limits from series. The function is expanded around the point in a small t > 0 (x = a ± t, or
 * x = ±1/t at infinity) as a Puiseux series Σ cₑ tᵉ + O(t^order): exponents are fractions,
 * coefficients exact (they may contain L = ln t, so x ln x and xˣ work). Elementary and special
 * functions compose with their Taylor series at a finite point, and at infinity with what they
 * tend to: e^(−∞) and erf(∞) − 1 are smaller than any power (order ∞), sin and cos of ∞ are only
 * bounded (order 0), Si(x) − π/2 is O(1/x). The limit is then read off the leading term; terms
 * that cancel exactly are found exactly, so (sin tan x − tan sin x)/x⁷ comes out −1/30.
 */
object Puiseux {
    private class Fail : RuntimeException()
    private fun fail(): Nothing = throw Fail()

    private val L = Sym("\u0001L")
    /** Prints why an expansion gave up (for tests). */
    @Volatile var debug = false
    // The working precision (exponents up to it are kept), per thread.
    private val capLocal = ThreadLocal.withInitial { Rational.of(8) }
    private var cap: Rational
        get() = capLocal.get()
        set(v) = capLocal.set(v)

    /** Σ terms + O(t^order); order null means exact (or smaller than any power). */
    private class S(terms: Map<Rational, Expr>, order: Rational?) {
        val terms = TreeMap<Rational, Expr>()
        val order: Rational? = order.let { o -> if (o == null || o > cap) cap else o }
        init {
            for ((e, c) in terms) {
                if (this.order != null && e >= this.order) continue
                val s = Algebra.simplify(c)
                if (!isZero(s)) this.terms[e] = s
            }
        }
        val lead get() = terms.entries.firstOrNull()
    }

    private fun isZero(c: Expr): Boolean {
        if (c == ZERO) return true
        if (c.isConstant && !c.contains { it == L }) {
            val v = runCatching { Numeric.eval(c) }.getOrNull() ?: return false
            return v.abs() < 1e-13
        }
        return false
    }

    private fun const(c: Expr) = S(mapOf(Rational.ZERO to c), null)
    private fun minO(a: Rational?, b: Rational?) = if (a == null) b else if (b == null) a else if (a < b) a else b
    private fun plusO(a: Rational?, b: Rational) = a?.let { it + b }

    private fun add(a: S, b: S): S {
        val m = HashMap<Rational, Expr>()
        for ((e, c) in a.terms) m[e] = c
        for ((e, c) in b.terms) m[e] = m[e]?.let { add(it, c) } ?: c
        return S(m, minO(a.order, b.order))
    }

    private fun scale(a: S, k: Expr) = S(a.terms.mapValues { mul(it.value, k) }, a.order)

    private fun mul(a: S, b: S): S {
        // Each factor's error times the other's size: a series with no terms is O(t^order) itself.
        val la = a.lead?.key ?: a.order!!
        val lb = b.lead?.key ?: b.order!!
        val order = minO(a.order!! + lb, b.order!! + la)
        val m = HashMap<Rational, Expr>()
        for ((e1, c1) in a.terms) for ((e2, c2) in b.terms) {
            val e = e1 + e2
            if (order != null && e >= order) continue
            m[e] = m[e]?.let { add(it, mul(c1, c2)) } ?: mul(c1, c2)
        }
        return S(m, order)
    }

    /** a = c₀tᵉ⁰(1 + u): (c₀, e₀, u) with u of positive exponents. */
    private fun split(a: S): Triple<Expr, Rational, S> {
        val (e0, c0) = a.lead ?: fail()
        val u = S(a.terms.filterKeys { it != e0 }.map { (e, c) -> e - e0 to div(c, c0) }.toMap(), a.order?.let { it - e0 })
        return Triple(c0, e0, u)
    }

    private fun shift(a: S, by: Rational) = S(a.terms.mapKeys { it.key + by }, a.order?.let { it + by })

    /** Σ cₖ uᵏ for u → 0 (positive exponents), as many terms as the precision needs. */
    private fun compose(cs: (Int) -> Expr, u: S): S {
        val lu = u.lead?.key
        if (lu == null) {
            // u is only O(t^order): the constant term, and that error.
            return S(mapOf(Rational.ZERO to cs(0)), u.order)
        }
        if (lu.signum <= 0) fail()
        var result = const(cs(0))
        var power = const(ONE)
        var k = 1
        while (true) {
            power = mul(power, u)
            val pl = power.lead?.key ?: power.order
            if (pl == null || pl >= cap || k > 60) break
            val ck = cs(k)
            if (ck != ZERO) result = add(result, scale(power, ck))
            k++
        }
        // The first term left out bounds the error.
        val left = power.lead?.key ?: power.order
        return S(result.terms, minO(left, u.order))
    }

    private fun inverse(a: S): S {
        val (c0, e0, u) = split(a)
        // 1/(1 + u) = Σ (−u)ᵏ
        val g = compose({ k -> if (k % 2 == 0) ONE else MINUS_ONE }, u)
        return scale(shift(g, -e0), div(ONE, c0))
    }

    private fun power(a: S, r: Expr): S {
        val n = (r as? Num)?.q
        if (n != null && n.isInteger && n.signum >= 0 && n.num.toInt() <= 30) {
            var p = const(ONE); repeat(n.num.toInt()) { p = mul(p, a) }; return p
        }
        if (n != null && n.isInteger && n.signum < 0 && -n.num.toInt() <= 30) return inverse(power(a, Num(-n)))
        val (c0, e0, u) = split(a)
        if (n == null && e0.signum != 0) fail()
        if (c0.contains { it == L }) fail()
        // (1 + u)^r = Σ C(r, k) uᵏ
        val g = compose({ k -> var c: Expr = ONE; for (j in 0 until k) c = mul(c, div(sub(r, Num(j.toLong())), Num((j + 1).toLong()))); c }, u)
        return scale(shift(g, if (n != null) e0 * n else Rational.ZERO), pow(c0, r))
    }

    private fun exp(a: S): S {
        val neg = a.terms.filterKeys { it.signum < 0 }
        if (neg.isNotEmpty()) {
            // e^(−∞) is smaller than any power; e^(+∞) has no limit to read.
            val (_, c) = neg.entries.first()
            if (c.contains { it == L }) fail()
            val v = runCatching { Numeric.eval(c) }.getOrNull() ?: fail()
            if (!v.isReal) fail()
            return if (v.re < 0) S(emptyMap(), null).let { S(it.terms, cap) } else fail()
        }
        var c0 = a.terms[Rational.ZERO] ?: ZERO
        var tPower = Rational.ZERO
        if (c0.contains { it == L }) {
            // e^(pL + q) = tᵖ e^q
            val cs = Algebra.coefficients(Algebra.expand(c0), L) ?: fail()
            if (cs.size != 2) fail()
            tPower = (Algebra.simplify(cs[1]) as? Num)?.q ?: fail()
            c0 = cs[0]
        }
        val u = S(a.terms.filterKeys { it.signum > 0 }, a.order)
        var fact = Rational.ONE
        val g = compose({ k -> if (k > 0) fact = fact / Rational.of(k.toLong()); Num(fact) }, u)
        return scale(shift(g, tPower), pow(E, c0))
    }

    private fun ln(a: S): S {
        val (c0, e0, u) = split(a)
        if (c0.contains { it == L }) fail()
        val g = compose({ k -> if (k == 0) ZERO else Num(Rational.of(if (k % 2 == 1) 1 else -1, k.toLong())) }, u)
        return add(add(const(fn("ln", c0)), const(mul(Num(e0), L))), g)
    }

    /**
     * f(c + u), u → 0: the Taylor series of f at c, from its derivatives, as far as they can be
     * worked out (Γ's derivatives run out after ψ′: the error is then the first missing term).
     */
    private fun taylor(name: String, c: Expr, u: S): S {
        val z = Sym("\u0001z")
        var d: Expr = fn(name, z)
        val cs = ArrayList<Expr>()
        var fact = Rational.ONE
        val lu = u.lead?.key
        val needed = if (lu == null || lu.signum <= 0) 1 else (cap / lu).let { (it.num / it.den).toInt() + 1 }.coerceAtMost(60)
        for (k in 0..needed) {
            if (k > 0) {
                d = runCatching { Algebra.simplify(Calculus.diff(d, z)) }.getOrNull() ?: break
                fact = fact / Rational.of(k.toLong())
            }
            val v = runCatching { Algebra.simplify(d.subst(z, c)) }.getOrNull() ?: break
            if (v.contains { it == INF } || runCatching { Numeric.eval(v) }.getOrNull()?.let { !it.re.isFinite() } == true) { if (k == 0) fail() else break }
            cs += mul(Num(fact), v)
        }
        if (cs.isEmpty()) fail()
        val r = compose({ k -> cs.getOrElse(k) { ZERO } }, u)
        // Terms past the last known derivative are unknown: O(u^known).
        if (cs.size <= needed) {
            var power = const(ONE); repeat(cs.size) { power = mul(power, u) }
            val known = power.lead?.key ?: power.order
            return S(r.terms, minO(r.order, known))
        }
        return r
    }

    private fun function(name: String, args: List<S>): S {
        if (args.size != 1) fail()
        val a = args[0]
        val lead = a.lead
        when (name) {
            "ln" -> return ln(a)
            // Through sin and cos, so poles (tan at π/2) come out of the division.
            "tan" -> return mul(function("sin", args), inverse(function("cos", args)))
            // Γ at a pole (0, −1, −2, …): Γ(a) = Γ(a + k)/(a(a + 1)…(a + k − 1)).
            "gamma" -> {
                val l = lead
                val c0 = if (l == null || l.key.signum > 0) ZERO else if (l.key.signum == 0) l.value else null
                val n = (c0 as? Num)?.q?.takeIf { it.isInteger && it.signum <= 0 }?.num?.toInt()
                if (n != null) {
                    val k = 1 - n
                    var den = const(ONE)
                    for (j in 0 until k) den = mul(den, add(a, const(Num(j.toLong()))))
                    return mul(function("gamma", listOf(add(a, const(Num(k.toLong()))))), inverse(den))
                }
            }
            "abs" -> {
                val c = lead?.value ?: fail()
                val v = runCatching { Numeric.eval(c.subst(L, Num(-1000))) }.getOrNull() ?: fail()
                return if (v.re >= 0) a else scale(a, MINUS_ONE)
            }
        }
        if (lead == null) {
            // Only an error bound: fine if the argument shrinks to 0.
            if (a.order == null || a.order.signum <= 0) fail()
            return taylor(name, ZERO, a)
        }
        val (e0, c0) = lead
        if (c0.contains { it == L } && e0.signum <= 0) fail()
        return when {
            e0.signum > 0 -> taylor(name, ZERO, a)
            e0.signum == 0 -> taylor(name, c0, S(a.terms.filterKeys { it.signum > 0 }, a.order))
            else -> atInfinity(name, a, c0, e0)
        }
    }

    /** f of an argument tending to ±∞ (its leading term c₀tᵉ⁰, e₀ < 0). */
    private fun atInfinity(name: String, a: S, c0: Expr, e0: Rational): S {
        val v = runCatching { Numeric.eval(c0) }.getOrNull() ?: fail()
        if (!v.isReal) fail()
        val s = if (v.re > 0) 1L else -1L
        val gap = -e0 // O(1/a) = O(t^gap)
        return when (name) {
            "atan" -> add(const(mul(Num(s), div(PI, TWO))), scale(function("atan", listOf(inverse(a))), MINUS_ONE))
            "tanh", "erf" -> S(mapOf(Rational.ZERO to Num(s)), cap)
            "sin", "cos" -> S(emptyMap(), Rational.ZERO)
            "si" -> S(mapOf(Rational.ZERO to mul(Num(s), div(PI, TWO))), gap)
            "ci" -> if (s > 0) S(emptyMap(), gap) else fail()
            "fresnels", "fresnelc" -> S(mapOf(Rational.ZERO to Num(Rational.of(s, 2))), gap)
            "ei" -> if (s < 0) S(emptyMap(), cap) else fail()
            "sinh" -> add(scale(exp(a), HALF), scale(exp(scale(a, MINUS_ONE)), num(-1, 2)))
            "cosh" -> add(scale(exp(a), HALF), scale(exp(scale(a, MINUS_ONE)), HALF))
            else -> fail()
        }
    }

    /** The series of [e] with x replaced by [xs]. */
    private fun series(e: Expr, x: Sym, xs: S): S = when {
        e.freeOf(x) -> if (e.contains { it == INF }) fail() else const(e)
        e == x -> xs
        e is Add -> e.terms.map { series(it, x, xs) }.reduce { p, q -> add(p, q) }
        e is Mul -> e.factors.map { series(it, x, xs) }.reduce { p, q -> mul(p, q) }
        e is Pow && e.exp.freeOf(x) -> power(series(e.base, x, xs), e.exp)
        e is Pow && e.base == E -> exp(series(e.exp, x, xs))
        e is Pow -> exp(mul(series(e.exp, x, xs), ln(series(e.base, x, xs))))
        e is Fn && e.name == "gammainc" && e.args.size == 2 && e.args[0].freeOf(x) -> {
            val a = series(e.args[1], x, xs)
            val lead = a.lead ?: fail()
            if (lead.key.signum < 0 && runCatching { Numeric.eval(lead.value).re > 0 }.getOrDefault(false)) S(emptyMap(), cap) else fail()
        }
        // ln cosh u and ln sinh u for u → ±∞: |u| − ln 2 + ln(1 ± e^(−2|u|)).
        e is Fn && e.name == "ln" && e.args[0] is Fn && (e.args[0] as Fn).name in setOf("cosh", "sinh") && run {
            val us = series((e.args[0] as Fn).args[0], x, xs)
            (us.lead?.key?.signum ?: 0) < 0
        } -> {
            val inner = e.args[0] as Fn
            val us = series(inner.args[0], x, xs)
            val sgn = runCatching { Numeric.eval(us.lead!!.value).re }.getOrNull() ?: fail()
            val su = if (sgn > 0) us else scale(us, MINUS_ONE)
            if (inner.name == "sinh" && sgn < 0) fail()
            val small = exp(scale(su, Num(-2)))
            val rest = ln(add(const(ONE), if (inner.name == "cosh") small else scale(small, MINUS_ONE)))
            add(add(su, const(neg(fn("ln", TWO)))), rest)
        }
        e is Fn -> function(e.name, e.args.map { series(it, x, xs) })
        else -> fail()
    }

    /**
     * lim f as x → [a] from [side] (+1 right, −1 left, 0 both): null when the series can't
     * decide (the caller then tries other ways).
     */
    fun limit(f: Expr, x: Sym, a: Expr, side: Int): Expr? {
        direct(f, x, a, side)?.let { return it }
        val atInf = Calculus.isPlusInfinity(a)
        // Only ln x in it (at ∞ or 0⁺): x = eʸ, y → ±∞.
        if (atInf || (a == ZERO && side == 1)) {
            val y = Sym("\u0001y")
            val g = Algebra.simplify(f.subst(fn("ln", x), y))
            if (g.freeOf(x) && g != f) direct(g, y, if (atInf) INF else neg(INF), 0)?.let { return it }
        }
        // Positive near the point: lim f = exp(lim ln f), with ln f written as a sum of logs (eˣ/x⁵).
        if (positiveNear(f, x, a, side)) {
            val lf = runCatching { Algebra.simplify(logExpand(fn("ln", f))) }.getOrNull() ?: return null
            val l = direct(lf, x, a, side) ?: return null
            return when {
                l == INF -> INF
                Calculus.isMinusInfinity(l) -> ZERO
                l.contains { it == INF } -> null
                else -> Algebra.simplify(pow(E, l))
            }
        }
        return null
    }

    /** ln of products, quotients and powers taken apart: ln(eˣ/x⁵) = x − 5 ln x. */
    private fun logExpand(e: Expr): Expr = when {
        e is Fn && e.name == "ln" && e.args[0] is Mul -> add((e.args[0] as Mul).factors.map { logExpand(fn("ln", it)) })
        e is Fn && e.name == "ln" && e.args[0] is Pow -> {
            val p = e.args[0] as Pow
            if (p.base == E) p.exp else mul(p.exp, logExpand(fn("ln", p.base)))
        }
        else -> e
    }

    private fun positiveNear(f: Expr, x: Sym, a: Expr, side: Int): Boolean {
        val pts = when {
            Calculus.isPlusInfinity(a) -> listOf(20.0, 35.0, 60.0)
            Calculus.isMinusInfinity(a) -> listOf(-20.0, -35.0, -60.0)
            else -> {
                val c = runCatching { Numeric.real(a) }.getOrNull() ?: return false
                val sides = if (side == 0) listOf(1, -1) else listOf(side)
                sides.flatMap { s -> listOf(1e-3, 1e-5).map { c + s * it } }
            }
        }
        return pts.all { p -> runCatching { Numeric.eval(f, mapOf(x.name to p)) }.getOrNull()?.let { it.isReal && it.re > 0 } == true }
    }

    private fun direct(f: Expr, x: Sym, a: Expr, side: Int): Expr? {
        val t = Sym("\u0001t")
        val sides = when {
            Calculus.isPlusInfinity(a) || Calculus.isMinusInfinity(a) -> listOf(1)
            side == 0 -> listOf(1, -1)
            else -> listOf(side)
        }
        val values = sides.map { sd ->
            val xs = when {
                Calculus.isPlusInfinity(a) -> S(mapOf(Rational.of(-1) to ONE), null)
                Calculus.isMinusInfinity(a) -> S(mapOf(Rational.of(-1) to MINUS_ONE), null)
                else -> S(mapOf(Rational.ZERO to a, Rational.ONE to Num(sd.toLong())), null)
            }
            fromSeries(f, x, xs) ?: return null
        }
        if (values.size == 2) {
            val (r, l) = values
            if (r == l) return r
            if (r.contains { it == INF } || l.contains { it == INF }) throw MathError("The limits from the left and right differ")
            val rv = runCatching { Numeric.eval(r) }.getOrNull(); val lv = runCatching { Numeric.eval(l) }.getOrNull()
            if (rv != null && lv != null && (rv - lv).abs() <= 1e-12 * maxOf(1.0, rv.abs())) return r
            throw MathError("The limits from the left and right differ")
        }
        return values[0]
    }

    private fun fromSeries(f: Expr, x: Sym, xs0: S): Expr? {
        for (n in listOf(6, 12, 20, 32)) {
            cap = Rational.of(n.toLong())
            // x itself is exact: rebuilt at this precision.
            val xs = S(xs0.terms, null)
            // Not enough terms (a high power truncated away) or a dead end: try more terms.
            val s = try { series(f, x, xs) } catch (e: Fail) { if (debug) e.printStackTrace(); continue } catch (e: MathError) { if (debug) e.printStackTrace(); continue } catch (e: ArithmeticException) { if (debug) e.printStackTrace(); continue }
            val lead = s.lead
            if (debug) println("PUISEUX cap=$n lead=${lead?.key} ${lead?.value?.let { Printer.plain(it) }} order=${s.order}")
            if (lead == null) {
                if (s.order != null && s.order.signum > 0) return ZERO
                continue
            }
            val (e, c) = lead
            fun signAt(c: Expr): Int? = runCatching { Numeric.eval(c.subst(L, Num(-1000))) }.getOrNull()?.takeIf { it.isReal }?.let { if (it.re > 0) 1 else if (it.re < 0) -1 else null }
            return when {
                e.signum > 0 -> ZERO
                e.signum < 0 -> when (signAt(c)) { 1 -> INF; -1 -> neg(INF); else -> null }
                // A constant term in L = ln t: its own limit as L → −∞ (1/ln x → 0, 2 + 1/ln x → 2, ln x → ∞).
                c.contains { it == L } -> {
                    val w = Sym("\u0001w")
                    val l = runCatching { Calculus.limit(c.subst(L, w), w, neg(INF)) }.getOrNull()
                    when {
                        l == null || l.contains { it is Fn && it.name == "lim" } -> when (signAt(c)) { 1 -> INF; -1 -> neg(INF); else -> null }
                        Calculus.isInfinite(l) -> l
                        s.order != null && s.order.signum <= 0 -> null
                        else -> Algebra.simplify(l)
                    }
                }
                // A constant term known exactly only if the error is smaller.
                s.order != null && s.order.signum <= 0 -> null
                else -> Algebra.simplify(c)
            }
        }
        return null
    }
}
