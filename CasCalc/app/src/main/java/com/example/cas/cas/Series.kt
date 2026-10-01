package com.example.cas.cas

import com.example.cas.math.Rational

/**
 * Infinite sums Σ_{k=lo}^{∞} in closed form, found from the shape of the term rather than a list
 * of known series. The term is split into factors:
 *  - a ratio rᵏ (any c·b^(mk+q): (−1)ᵏ, 2⁻ᵏ, xᵏ, e^(ik)…),
 *  - powers of polynomials in k with whole exponents (a rational function of k),
 *  - powers (ak + b)^e of linear terms with other exponents (e = −s, symbolic s included),
 *  - 1/(ak + b)! with (ak + b) one of k, 2k, 2k + 1.
 * Then:
 *  - rational × rᵏ: partial fractions, each c/(k + β)ᵐ summed as Hurwitz ζ (r = 1), its
 *    alternating form (r = −1), or a polylogarithm (other r, whole β); the divergent 1/k parts
 *    cancel through the digamma function, exactly where ψ is known;
 *  - polynomial × rᵏ: (r d/dr)ʲ of 1/(1 − r);
 *  - polynomial × (ak + b)^e × rᵏ: Hurwitz ζ, the Dirichlet η (= (1 − 2¹⁻ˢ)ζ(s)) and its relatives
 *    for r = ±1, the polylogarithm Li_s(r) for b = 0;
 *  - polynomial × rᵏ/(ak + b)!: eʳ, cosh √r, sinh √r/√r (cos and sin when r < 0), with (r d/dr)ʲ.
 * Null when the term isn't any of these; the caller then keeps the sum and adds it up numerically.
 */
object Series {
    private class Special(val a: Rational, val b: Rational, var e: Expr)

    fun infinite(body: Expr, k: Sym, lo: Expr): Expr? {
        val low = (lo as? Num)?.q?.takeIf { it.isInteger }?.num?.toLong() ?: return null
        if (body.freeOf(k)) {
            // A constant term: ∞ or 0.
            if (body == ZERO) return ZERO
            return divergent(body)
        }
        // A sum of terms that each have a closed form (when every one converges).
        if (body is Add) {
            runCatching { analyzeAndSum(body, k, low) }.getOrNull()?.let { return it }
            val parts = body.terms.map { infinite(it, k, lo) ?: return null }
            if (parts.any { it.contains { e -> e == INF } }) return null
            return Algebra.simplify(add(parts))
        }
        return analyzeAndSum(body, k, low)
    }

    private fun analyzeAndSum(body: Expr, k: Sym, lo: Long): Expr? {
        val factors = if (body is Mul) body.factors else listOf(body)
        var c: Expr = ONE
        var ratio: Expr = ONE
        var rational: Expr = ONE
        val specials = ArrayList<Special>()
        var fact: Pair<Rational, Rational>? = null
        for (f in factors) {
            when {
                f.freeOf(k) -> c = mul(c, f)
                f is Pow && f.base.freeOf(k) -> {
                    // b^(mk + q) = (b^m)ᵏ · b^q
                    val cs = Algebra.coefficients(Algebra.expand(f.exp), k) ?: return null
                    if (cs.size != 2 || !cs[1].freeOf(k)) return null
                    ratio = mul(ratio, pow(f.base, cs[1]))
                    c = mul(c, pow(f.base, cs[0]))
                }
                f is Pow && f.base is Fn && (f.base as Fn).name == "fact" && f.exp == MINUS_ONE -> {
                    if (fact != null) return null
                    val (a, b) = linear((f.base as Fn).args[0], k) ?: return null
                    if (!(a == Rational.ONE && b == Rational.ZERO) && !(a == Rational.of(2) && (b == Rational.ZERO || b == Rational.ONE))) return null
                    fact = a to b
                }
                f is Pow && !f.exp.freeOf(k) -> return null
                f is Pow && (f.exp as? Num)?.q?.isInteger == true && polynomialIn(f.base, k) -> rational = mul(rational, f)
                f is Pow -> {
                    val (a, b) = linear(f.base, k) ?: return null
                    specials.firstOrNull { it.a == a && it.b == b }?.let { it.e = add(it.e, f.exp) } ?: run { specials += Special(a, b, f.exp) }
                }
                polynomialIn(f, k) -> rational = mul(rational, f)
                else -> return null
            }
        }
        ratio = Algebra.simplify(ratio)
        c = Algebra.simplify(c)
        if (specials.size > 1) return null
        val f = fact
        val (num, den) = Algebra.together(rational)
        val polynomial = den.freeOf(k)
        return when {
            f != null -> if (specials.isEmpty() && polynomial) mul(c, div(ONE, den), factorialSeries(num, k, ratio, f, lo)) else null
            specials.isNotEmpty() -> if (polynomial) mul(c, div(ONE, den), powerSeries(num, k, specials[0], ratio, lo)) else null
            polynomial -> mul(c, div(ONE, den), geometricPolynomial(num, k, ratio, lo))
            else -> rationalSeries(Algebra.simplify(rational), k, ratio, lo)?.let { mul(c, it) }
        }?.let { Algebra.simplify(it) }
    }

    /** (a, b) for a·k + b with rational a ≠ 0 and b. */
    private fun linear(e: Expr, k: Sym): Pair<Rational, Rational>? {
        val cs = Algebra.coefficients(Algebra.expand(e), k) ?: return null
        if (cs.size != 2) return null
        val a = (cs[1] as? Num)?.q ?: return null
        val b = (cs[0] as? Num)?.q ?: return null
        return if (a.signum == 0) null else a to b
    }

    private fun polynomialIn(e: Expr, k: Sym): Boolean = Algebra.coefficients(Algebra.expand(e), k)?.all { it.freeOf(k) } == true

    /** Σ_{k=lo}^{hi} body, terms added exactly. */
    private fun finite(body: (Long) -> Expr, from: Long, to: Long): Expr =
        if (to < from) ZERO else add((from..to).map(body))

    private fun numeric(e: Expr): CD? = if (e.isConstant) runCatching { Numeric.eval(e) }.getOrNull() else null

    private fun divergent(sign: Expr): Expr {
        val v = numeric(sign) ?: throw MathError("The series diverges")
        if (!v.isReal) throw MathError("The series diverges")
        return if (v.re > 0) INF else neg(INF)
    }

    // ---- Σ P(k) rᵏ -------------------------------------------------------------------------------

    /** Σ_{k≥lo} P(k)·rᵏ for a polynomial P: from Σ_{k≥0} kʲ ρᵏ = (ρ d/dρ)ʲ 1/(1 − ρ). */
    private fun geometricPolynomial(p: Expr, k: Sym, r: Expr, lo: Long): Expr {
        val cs = Algebra.coefficients(Algebra.expand(p), k) ?: throw MathError("The series diverges")
        numeric(r)?.let { rv ->
            if (rv.abs() >= 1 - 1e-15) {
                // r = 1: the terms don't shrink. Otherwise they grow or keep their size.
                if (rv.isReal && rv.re > 0) return divergent(cs.last())
                throw MathError("The series diverges")
            }
        }
        val rho = Sym("\u0001ρ")
        var g: Expr = div(ONE, sub(ONE, rho))
        val parts = ArrayList<Expr>()
        cs.forEachIndexed { j, cj ->
            if (j > 0) g = Algebra.simplify(mul(rho, Calculus.diff(g, rho)))
            parts += mul(cj, g)
        }
        val total = add(parts).subst(rho, r)
        return adjust(total, 0, lo) { n -> mul(p.subst(k, Num(n)), pow(r, Num(n))) }
    }

    /** [total] is the sum from [natural]; the sum from [lo] drops (or adds) the terms between. */
    private fun adjust(total: Expr, natural: Long, lo: Long, term: (Long) -> Expr): Expr = when {
        lo > natural -> sub(total, finite(term, natural, lo - 1))
        lo < natural -> add(total, finite(term, lo, natural - 1))
        else -> total
    }

    // ---- Σ P(k) rᵏ/(ak + b)! ----------------------------------------------------------------------

    private fun factorialSeries(p: Expr, k: Sym, r: Expr, f: Pair<Rational, Rational>, lo: Long): Expr {
        val cs = Algebra.coefficients(Algebra.expand(p), k) ?: return ZERO
        val (a, b) = f
        // r < 0 turns cosh and sinh into cos and sin: with r = −w², √w.
        val negative = Simplify.isNegative(r) || numeric(r)?.let { it.isReal && it.re < 0 } == true
        fun root(w: Expr): Expr = if (w is Pow && w.exp is Num && (w.exp as Num).q == Rational.of(2)) w.base else sqrt(w)
        val g: (Expr) -> Expr = when {
            a == Rational.ONE -> { x -> pow(E, x) }
            b == Rational.ZERO -> if (negative) { x -> fn("cos", root(Algebra.simplify(neg(x)))) } else { x -> fn("cosh", root(x)) }
            else -> if (negative) { x -> root(Algebra.simplify(neg(x))).let { s -> div(fn("sin", s), s) } } else { x -> root(x).let { s -> div(fn("sinh", s), s) } }
        }
        val total = if (cs.size == 1) mul(cs[0], g(r)) else {
            val rho = Sym("\u0001ρ")
            var h = g(rho)
            val parts = ArrayList<Expr>()
            cs.forEachIndexed { j, cj ->
                if (j > 0) h = Algebra.simplify(mul(rho, Calculus.diff(h, rho)))
                parts += mul(cj, h)
            }
            add(parts).subst(rho, r)
        }
        val factorialOf = { n: Long -> fn("fact", Num(a * Rational.of(n) + b)) }
        return adjust(total, 0, lo) { n -> div(mul(p.subst(k, Num(n)), pow(r, Num(n))), factorialOf(n)) }
    }

    // ---- Σ P(k) (ak + b)^e rᵏ ---------------------------------------------------------------------

    private fun powerSeries(p: Expr, k: Sym, s: Special, r: Expr, lo: Long): Expr {
        // P(k) in powers of L = ak + b: k = (L − b)/a.
        val l = Sym("\u0001L")
        val inL = Algebra.coefficients(Algebra.expand(p.subst(k, div(sub(l, Num(s.b)), Num(s.a)))), l) ?: return ZERO
        val q = Rational.of(lo) + s.b / s.a      // first L/a
        val rv = numeric(r)
        val parts = ArrayList<Expr>()
        inL.forEachIndexed { j, dj ->
            if (dj == ZERO) return@forEachIndexed
            val e = Algebra.simplify(add(s.e, Num(j.toLong())))      // (ak + b)^e
            val sArg = Algebra.simplify(neg(e))                        // as a ζ argument
            val scale = pow(Num(s.a), e)                               // (a(k + b/a))^e = aᵉ (k + b/a)^e
            val sum = when {
                rv != null && rv.isReal && kotlin.math.abs(rv.re - 1) < 1e-15 -> {
                    numeric(sArg)?.let { sv -> if (sv.isReal && sv.re <= 1) return divergent(mul(dj, scale)) }
                    hurwitz(sArg, q)
                }
                rv != null && rv.isReal && kotlin.math.abs(rv.re + 1) < 1e-15 -> {
                    numeric(sArg)?.let { sv -> if (sv.isReal && sv.re <= 0) throw MathError("The series diverges") }
                    mul(if (lo % 2 == 0L) ONE else MINUS_ONE, alternating(sArg, q))
                }
                s.b == Rational.ZERO -> {
                    // Σ_{k≥lo} rᵏ (ak)^e = aᵉ [Li_s(r) − Σ_{k<lo}]
                    rv?.let { if (it.abs() > 1 + 1e-15) throw MathError("The series diverges") }
                    if (lo < 1) return ZERO.also { throw MathError("The first term divides by 0") }
                    sub(fn("polylog", sArg, r), finite({ n -> mul(pow(r, Num(n)), pow(Num(n), e)) }, 1, lo - 1))
                }
                else -> throw MathError("No closed form")
            }
            parts += mul(dj, scale, sum)
        }
        return add(parts)
    }

    /** ζ(s, q) = Σ_{n≥0} (n + q)^(−s), exact where it reduces to ζ. */
    private fun hurwitz(s: Expr, q: Rational): Expr = fn("hurwitz", s, Num(q))

    /** Σ_{n≥0} (−1)ⁿ (n + q)^(−s): the Dirichlet η at q = 1, ψ differences at s = 1, else Hurwitz ζ. */
    private fun alternating(s: Expr, q: Rational): Expr = when {
        s == ONE -> digammas(listOf(HALF to (q + Rational.ONE) / Rational.of(2), neg(HALF) to q / Rational.of(2)))
        q == Rational.ONE -> mul(sub(ONE, pow(TWO, sub(ONE, s))), fn("zeta", s))
        else -> mul(pow(TWO, neg(s)), sub(hurwitz(s, q / Rational.of(2)), hurwitz(s, (q + Rational.ONE) / Rational.of(2))))
    }

    // ---- rational functions of k ------------------------------------------------------------------

    /** Σ_{k≥lo} R(k) rᵏ by partial fractions, for r = ±1 (or any r with whole shifts: polylogarithms). */
    private fun rationalSeries(rational: Expr, k: Sym, r: Expr, lo: Long): Expr? {
        val parts = Algebra.apart(rational, k).let { if (it is Add) it.terms else listOf(it) }
        // Each part: c/(k − ρ)ᵐ.
        class Part(val c: Expr, val root: Rational, val m: Int)
        val list = ArrayList<Part>()
        for (t in parts) {
            val fs = if (t is Mul) t.factors else listOf(t)
            var c: Expr = ONE
            var root: Rational? = null
            var m = 0
            for (f in fs) {
                when {
                    f.freeOf(k) -> c = mul(c, f)
                    f is Pow && (f.exp as? Num)?.q?.isInteger == true && (f.exp as Num).q.signum < 0 -> {
                        val (a, b) = linear(f.base, k) ?: return null
                        if (root != null) return null
                        val power = -(f.exp as Num).q.num.toInt()
                        // (ak + b)^(−m) = a^(−m) (k + b/a)^(−m)
                        c = mul(c, pow(Num(a), Num(-power.toLong())))
                        root = -b / a; m = power
                    }
                    else -> return null   // a polynomial part: the terms don't shrink
                }
            }
            if (root == null) return divergent(c)
            list += Part(c, root, m)
        }
        // A ratio with letters in it (xᵏ/k): polylogarithms, valid where they converge.
        val rv = numeric(r) ?: CD(0.0)
        val out = ArrayList<Expr>()
        when {
            r.isConstant && rv.isReal && kotlin.math.abs(rv.re - 1) < 1e-15 -> {
                // 1/k parts only converge together: Σ cᵢ/(k + βᵢ) = −Σ cᵢ ψ(lo + βᵢ) when Σ cᵢ = 0.
                val ones = list.filter { it.m == 1 }
                if (ones.isNotEmpty()) {
                    val total = Algebra.simplify(add(ones.map { it.c }))
                    if (total != ZERO) return divergent(total)
                    out += neg(digammas(ones.map { it.c to Rational.of(lo) - it.root }))
                }
                list.filter { it.m > 1 }.forEach { out += mul(it.c, hurwitz(Num(it.m.toLong()), Rational.of(lo) - it.root)) }
            }
            r.isConstant && rv.isReal && kotlin.math.abs(rv.re + 1) < 1e-15 -> {
                val sign = if (lo % 2 == 0L) ONE else MINUS_ONE
                list.forEach { out += mul(sign, it.c, alternating(Num(it.m.toLong()), Rational.of(lo) - it.root)) }
            }
            rv.abs() <= 1 + 1e-15 -> {
                // Σ_{k≥lo} rᵏ/(k − ρ)ᵐ with ρ whole: j = k − ρ, r^ρ Σ_{j≥lo−ρ} rʲ/jᵐ.
                for (p in list) {
                    if (!p.root.isInteger) return null
                    val shift = p.root.num.toLong()
                    val start = lo - shift
                    if (start < 1) throw MathError("A term divides by 0")
                    val m = Num(p.m.toLong())
                    out += mul(p.c, pow(r, Num(shift)), sub(fn("polylog", m, r), finite({ n -> div(pow(r, Num(n)), pow(Num(n), m)) }, 1, start - 1)))
                }
            }
            else -> throw MathError("The series diverges")
        }
        return add(out)
    }

    /**
     * Σ cᵢ ψ(xᵢ): arguments a whole number apart are brought to the smallest with
     * ψ(x + 1) = ψ(x) + 1/x, and ψ at ½, ¼, ¾ and whole numbers is exact up to −γ, which
     * cancels when the cᵢ add to 0. Anything left stays as ψ.
     */
    private fun digammas(terms: List<Pair<Expr, Rational>>): Expr {
        val out = ArrayList<Expr>()
        var gamma: Expr = ZERO // coefficient of −γ
        val groups = terms.groupBy { (_, x) -> x - Rational.of(x.floor()) }
        for ((_, group) in groups) {
            val base = group.minOf { it.second }
            if (base.signum <= 0 && base.isInteger) throw MathError("A term divides by 0")
            var coef: Expr = ZERO
            for ((c, x) in group) {
                coef = add(coef, c)
                // ψ(x) = ψ(base) + Σ_{j=0}^{x−base−1} 1/(base + j)
                val steps = (x - base).num.toLong()
                for (j in 0 until steps) out += mul(c, Num((base + Rational.of(j)).reciprocal()))
            }
            coef = Algebra.simplify(coef)
            if (coef == ZERO) continue
            val known = knownDigamma(base)
            if (known != null) { out += mul(coef, known); gamma = add(gamma, coef) } else out += mul(coef, fn("digamma", Num(base)))
        }
        gamma = Algebra.simplify(gamma)
        // What's left of −γ: −γ = ψ(1).
        if (gamma != ZERO) out += mul(gamma, fn("digamma", ONE))
        return add(out)
    }

    /** ψ(x) + γ for x = 1, ½, ¼, ¾ plus a whole number (null otherwise). */
    private fun knownDigamma(x: Rational): Expr? {
        val frac = x - Rational.of(x.floor())
        val whole = x.floor().toLong()
        val ln2 = fn("ln", TWO)
        val (start, value) = when (frac) {
            Rational.ZERO -> Rational.ONE to ZERO
            Rational.of(1, 2) -> Rational.of(1, 2) to mul(Num(-2), ln2)
            Rational.of(1, 4) -> Rational.of(1, 4) to sub(neg(div(PI, TWO)), mul(Num(3), ln2))
            Rational.of(3, 4) -> Rational.of(3, 4) to sub(div(PI, TWO), mul(Num(3), ln2))
            else -> return null
        }
        if (x < start) return null
        // ψ(start + n) = ψ(start) + Σ_{j<n} 1/(start + j)
        val n = (x - start).num.toLong()
        if (whole > 10_000) return null
        return add(listOf(value) + (0 until n).map { j -> Num((start + Rational.of(j)).reciprocal()) })
    }
}
