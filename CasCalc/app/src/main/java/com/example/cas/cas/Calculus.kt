package com.example.cas.cas

import com.example.cas.math.Rational
import java.math.BigInteger

/** Symbolic derivatives, antiderivatives, Taylor polynomials and closed-form sums. */
object Calculus {

    // ---- Derivatives -----------------------------------------------------------------

    fun diff(e: Expr, x: Sym): Expr = when (e) {
        // Matrices and vectors: entry by entry.
        is Mat -> Mat(e.rows, e.cols, e.cells.map { diff(it, x) })
        is Num, is Flt -> ZERO
        is Sym -> if (e == x) ONE else ZERO
        is Add -> add(e.terms.map { diff(it, x) })
        is Mul -> add(e.factors.indices.map { i ->
            mul(e.factors.mapIndexed { j, f -> if (i == j) diff(f, x) else f })
        })
        is Pow -> when {
            e.freeOf(x) -> ZERO
            e.exp.freeOf(x) -> mul(e.exp, pow(e.base, sub(e.exp, ONE)), diff(e.base, x))
            e.base.freeOf(x) -> mul(e, fn("ln", e.base), diff(e.exp, x))
            else -> mul(e, add(mul(diff(e.exp, x), fn("ln", e.base)), div(mul(e.exp, diff(e.base, x)), e.base)))
        }
        is Fn -> diffFn(e, x)
        is Mat -> Mat(e.rows, e.cols, e.cells.map { diff(it, x) })
        is Eq -> Eq(diff(e.lhs, x), diff(e.rhs, x))
        is Seq -> Seq(e.items.map { diff(it, x) }, e.joiner)
        is Rel -> throw MathError("Can't differentiate an inequality")
    }

    private fun diffFn(f: Fn, x: Sym): Expr {
        if (f.freeOf(x)) return ZERO
        // Factorials, binomials and permutations through Γ: n! = Γ(n + 1), and so on.
        when (f.name) {
            "fact" -> return diff(fn("gamma", add(f.args[0], ONE)), x)
            "binom" -> {
                val (n, k) = f.args
                return diff(div(fn("gamma", add(n, ONE)), mul(fn("gamma", add(k, ONE)), fn("gamma", add(sub(n, k), ONE)))), x)
            }
            "perm" -> {
                val (n, k) = f.args
                return diff(div(fn("gamma", add(n, ONE)), fn("gamma", add(sub(n, k), ONE))), x)
            }
        }
        // Bessel functions of fixed order a: J′ₐ(u) = (Jₐ₋₁(u) − Jₐ₊₁(u))/2, the same for Y.
        if ((f.name == "besselj" || f.name == "bessely") && f.args.size == 2 && f.args[0].freeOf(x)) {
            val (a, u) = f.args
            return mul(div(sub(fn(f.name, sub(a, ONE), u), fn(f.name, add(a, ONE), u)), TWO), diff(u, x))
        }
        val u = f.args.last()
        val du = diff(u, x)
        val outer: Expr = when (f.name) {
            "sin" -> fn("cos", u)
            "cos" -> neg(fn("sin", u))
            "tan" -> pow(fn("cos", u), num(-2))
            "asin" -> pow(sub(ONE, pow(u, TWO)), num(-1, 2))
            "acos" -> neg(pow(sub(ONE, pow(u, TWO)), num(-1, 2)))
            "atan" -> pow(add(ONE, pow(u, TWO)), MINUS_ONE)
            "sinh" -> fn("cosh", u)
            "cosh" -> fn("sinh", u)
            "tanh" -> pow(fn("cosh", u), num(-2))
            "asinh" -> pow(add(pow(u, TWO), ONE), num(-1, 2))
            "acosh" -> pow(sub(pow(u, TWO), ONE), num(-1, 2))
            "atanh" -> pow(sub(ONE, pow(u, TWO)), MINUS_ONE)
            "ln" -> pow(u, MINUS_ONE)
            "log" -> {
                if (!f.args[0].freeOf(x)) return diff(div(fn("ln", u), fn("ln", f.args[0])), x)
                pow(mul(u, fn("ln", f.args[0])), MINUS_ONE)
            }
            "abs" -> div(u, fn("abs", u))
            "sgn" -> ZERO
            // d/du erf u = 2/√π e^(−u²)
            "erf" -> div(mul(TWO, pow(E, neg(pow(u, TWO)))), sqrt(PI))
            "frac" -> ONE
            // Piecewise constant: 0 wherever the derivative exists (as Desmos draws it).
            "floor", "ceil", "round", "arg" -> ZERO
            // d/du Γ(u) = Γ(u) ψ(u), ψ the digamma function.
            "gamma" -> mul(fn("gamma", u), fn("digamma", u))
            // ζ′ has no closed form: worked out numerically (and ζ″ the same way).
            "zeta" -> fn("zetaprime", u)
            "zetaprime" -> fn("zetaprime2", u)
            // d/du ψ(u) = ψ′(u), the trigamma function.
            "digamma" -> fn("trigamma", u)
            // d/dp Φ⁻¹(p) = 1/φ(Φ⁻¹(p)) = √(2π) e^{Φ⁻¹(p)²/2}
            "invnorm" -> mul(sqrt(mul(TWO, PI)), pow(E, div(pow(fn("invnorm", u), TWO), TWO)))
            // ψ′ and ζ″ aren't used further; say so plainly.
            else -> throw MathError("Can't differentiate ${f.name}")
        }
        return mul(outer, du)
    }

    // ---- Antiderivatives ------------------------------------------------------------------

    /** An antiderivative of e, or null if none of the methods finds one. */
    fun integrate(e: Expr, x: Sym, depth: Int = 0): Expr? {
        if (e is Mat) {
            val cells = e.cells.map { integrate(it, x, depth) ?: return null }
            return Mat(e.rows, e.cols, cells)
        }
        if (depth > 6) return null
        if (e.freeOf(x)) return mul(e, x)
        if (e is Add) return add(e.terms.map { integrate(it, x, depth) ?: return null })
        if (e is Mul) {
            val (consts, rest) = e.factors.partition { it.freeOf(x) }
            if (consts.isNotEmpty()) return integrate(mul(rest), x, depth)?.let { mul(mul(consts), it) }
        }
        return table(e, x)
            ?: rational(e, x)
            ?: byParts(e, x, depth)
            ?: substitution(e, x, depth)
            ?: expandFirst(e, x, depth)
    }

    /** u = a·x + b: the linear coefficient a, or null if u isn't linear in x. */
    private fun linear(u: Expr, x: Sym): Expr? {
        val cs = Algebra.coefficients(Algebra.expand(u), x) ?: return null
        if (cs.size != 2 || cs[1] == ZERO) return null
        return cs[1]
    }

    private fun table(e: Expr, x: Sym): Expr? {
        // Powers of a linear expression, including x itself.
        if (e is Pow && e.exp.freeOf(x)) {
            val a = linear(e.base, x)
            if (a != null) {
                if (e.exp == MINUS_ONE) return div(fn("ln", fn("abs", e.base)), a)
                val n1 = add(e.exp, ONE)
                return div(pow(e.base, n1), mul(a, n1))
            }
        }
        if (e == x) return div(pow(x, TWO), TWO)
        // c^(a x + b)
        if (e is Pow && e.base.freeOf(x)) {
            val a = linear(e.exp, x)
            if (a != null) return if (e.base == E) div(e, a) else div(e, mul(a, fn("ln", e.base)))
        }
        if (e is Fn && e.args.size == 1 && linear(e.args[0], x) != null) {
            val u = e.args[0]
            val a = linear(u, x)!!
            val g: Expr = when (e.name) {
                "sin" -> neg(fn("cos", u))
                "cos" -> fn("sin", u)
                "tan" -> neg(fn("ln", fn("abs", fn("cos", u))))
                "sinh" -> fn("cosh", u)
                "cosh" -> fn("sinh", u)
                "tanh" -> fn("ln", fn("cosh", u))
                "ln" -> sub(mul(u, fn("ln", u)), u)
                "asin" -> add(mul(u, fn("asin", u)), sqrt(sub(ONE, pow(u, TWO))))
                "acos" -> sub(mul(u, fn("acos", u)), sqrt(sub(ONE, pow(u, TWO))))
                "atan" -> sub(mul(u, fn("atan", u)), div(fn("ln", add(ONE, pow(u, TWO))), TWO))
                else -> return null
            }
            return div(g, a)
        }
        // 1/√(1 − x²), 1/(1 + x²), 1/√(x² + 1) in their standard forms.
        if (e is Pow && e.exp == num(-1, 2)) {
            val cs = Algebra.coefficients(Algebra.expand(e.base), x) ?: return null
            if (cs.size == 3 && cs[1] == ZERO && cs[0] is Num && cs[2] is Num) {
                val c0 = (cs[0] as Num).q
                val c2 = (cs[2] as Num).q
                if (c0.signum > 0 && c2.signum < 0) {
                    // 1/√(c0 − k x²) = asin(x √(k/c0)) / √k
                    val k = -c2
                    return div(fn("asin", mul(x, sqrt(Num(k / c0)))), sqrt(Num(k)))
                }
                if (c0.signum > 0 && c2.signum > 0) {
                    return div(fn("asinh", mul(x, sqrt(Num(c2 / c0)))), sqrt(Num(c2)))
                }
            }
        }
        return null
    }

    /** Rational functions P/Q over the rationals, by partial fractions. */
    private fun rational(e: Expr, x: Sym): Expr? {
        val (n, d) = Algebra.together(e)
        if (d.freeOf(x)) return null
        val p = Algebra.qpoly(n, x) ?: return null
        val q = Algebra.qpoly(d, x) ?: return null
        val (whole, rem) = p.divMod(q)
        val parts = ArrayList<Expr>()
        if (!whole.isZero) parts += integrate(whole.toExpr(x), x) ?: return null
        if (rem.isZero) return add(parts)

        // Factor the denominator into linear factors (with multiplicity) and one leftover.
        var rest = q.monic()
        val linear = ArrayList<Pair<Rational, Int>>()
        for (r in Algebra.rationalRoots(rest)) {
            var m = 0
            val lin = QPoly(listOf(-r, Rational.ONE))
            while (true) {
                val (qq, rr) = rest.divMod(lin)
                if (!rr.isZero) break
                rest = qq; m++
            }
            if (m > 0) linear += r to m
        }
        if (rest.degree > 2) return null
        // Unknowns: A[r,k] for 1/(x−r)^k, and B x + C over the quadratic.
        data class Term(val root: Rational?, val power: Int, val xPower: Int)
        val terms = ArrayList<Term>()
        for ((r, m) in linear) for (k in 1..m) terms += Term(r, k, 0)
        if (rest.degree == 2) { terms += Term(null, 1, 1); terms += Term(null, 1, 0) }
        if (rest.degree == 1) return null
        val qm = q.monic()
        // Numerator each term contributes after multiplying through by Q.
        val basis = terms.map { t ->
            var poly = QPoly(listOf(Rational.ONE))
            if (t.root != null) {
                val lin = QPoly(listOf(-t.root, Rational.ONE))
                var den = QPoly(listOf(Rational.ONE))
                repeat(t.power) { den *= lin }
                poly = qm.divMod(den).first
            } else {
                poly = qm.divMod(rest).first
                if (t.xPower == 1) poly *= QPoly(listOf(Rational.ZERO, Rational.ONE))
            }
            poly
        }
        val target = rem.scale(q.lead.reciprocal())
        val size = terms.size
        val matrix = Array(size) { row -> Array(size + 1) { col -> if (col < size) basis[col][row] else target[row] } }
        val coef = solveLinear(matrix) ?: return null
        terms.forEachIndexed { i, t ->
            val a = Num(coef[i])
            if (a == ZERO) return@forEachIndexed
            if (t.root != null) {
                val u = sub(x, Num(t.root))
                parts += if (t.power == 1) mul(a, fn("ln", fn("abs", u)))
                else div(neg(a), mul(Num((t.power - 1).toLong()), pow(u, (t.power - 1).toLong())))
            }
        }
        if (rest.degree == 2) {
            // (B x + C)/(x² + p x + s) = B/2·ln(x² + p x + s) + (C − B p/2)·2/√(4s − p²)·atan((2x + p)/√(4s − p²))
            val b = coef[size - 2]
            val c = coef[size - 1]
            val pp = rest[1]
            val s = rest[0]
            val quad = rest.toExpr(x)
            val disc = s * Rational.of(4) - pp * pp
            if (disc.signum <= 0) return null
            val root = sqrt(Num(disc))
            if (b.signum != 0) parts += mul(Num(b / Rational.of(2)), fn("ln", quad))
            val k = c - b * pp / Rational.of(2)
            if (k.signum != 0) parts += mul(Num(k * Rational.of(2)), pow(root, MINUS_ONE), fn("atan", div(add(mul(TWO, x), Num(pp)), root)))
        }
        return add(parts)
    }

    private fun solveLinear(m: Array<Array<Rational>>): List<Rational>? {
        val n = m.size
        for (c in 0 until n) {
            val p = (c until n).firstOrNull { m[it][c].signum != 0 } ?: return null
            val t = m[p]; m[p] = m[c]; m[c] = t
            val piv = m[c][c]
            for (k in c..n) m[c][k] = m[c][k] / piv
            for (r in 0 until n) if (r != c && m[r][c].signum != 0) {
                val f = m[r][c]
                for (k in c..n) m[r][k] = m[r][k] - f * m[c][k]
            }
        }
        return List(n) { m[it][n] }
    }

    /** Polynomial × (e^(ax+b), sin, cos) and polynomial × ln x, by repeated integration by parts. */
    private fun byParts(e: Expr, x: Sym, depth: Int): Expr? {
        if (e !is Mul) return null
        val (polyParts, others) = e.factors.partition { Algebra.coefficients(it, x) != null }
        if (polyParts.isEmpty() || others.size != 1) return null
        val p = mul(polyParts)
        val f = others[0]
        val isExpLike = (f is Pow && f.base.freeOf(x) && linear(f.exp, x) != null) ||
            (f is Fn && f.name in setOf("sin", "cos", "sinh", "cosh") && linear(f.args[0], x) != null)
        if (isExpLike) {
            val v = integrate(f, x, depth + 1) ?: return null
            val rest = integrate(mul(diff(p, x), v), x, depth + 1) ?: return null
            return sub(mul(p, v), rest)
        }
        if (f is Fn && f.name in setOf("ln", "atan", "asin") && linear(f.args[0], x) != null) {
            val pi = integrate(p, x, depth + 1) ?: return null
            val rest = integrate(Algebra.simplify(mul(pi, diff(f, x))), x, depth + 1) ?: return null
            return sub(mul(pi, f), rest)
        }
        return null
    }

    /**
     * Derivative-divides: if e = g(u)·u′ for some inner expression u, then
     * ∫ e dx = G(u). Tries every function argument, power base and exponent.
     */
    private fun substitution(e: Expr, x: Sym, depth: Int): Expr? {
        val t = Sym("\u0001t")
        val candidates = LinkedHashSet<Expr>()
        fun collect(z: Expr) {
            when (z) {
                is Fn -> z.args.forEach { if (!it.freeOf(x) && it != x) candidates += it }
                is Pow -> {
                    if (!z.base.freeOf(x) && z.base != x) candidates += z.base
                    if (!z.exp.freeOf(x)) candidates += z.exp
                }
                else -> {}
            }
            // Whole functions/powers are candidates too (∫ ln(x)/x dx with u = ln x).
            if ((z is Fn || z is Pow) && !z.freeOf(x) && z != e) candidates += z
            z.children.forEach { collect(it) }
        }
        collect(e)
        for (u in candidates) {
            val du = diff(u, x)
            if (du == ZERO) continue
            val q = Algebra.simplify(div(e, du))
            val inT = q.subst(u, t)
            if (!inT.freeOf(x)) continue
            if (inT == q) continue
            val g = integrate(inT, t, depth + 1) ?: continue
            return g.subst(t, u)
        }
        return null
    }

    private fun expandFirst(e: Expr, x: Sym, depth: Int): Expr? {
        val ex = Algebra.expand(e)
        if (ex == e) return null
        return integrate(ex, x, depth + 1)
    }

    // ---- Definite integrals ------------------------------------------------------------------

    /** Exact when an antiderivative exists (checked against a numerical answer), numerical otherwise. */
    fun definite(body: Expr, x: Sym, a: Expr, b: Expr): Expr {
        if (body is Mat) return Mat(body.rows, body.cols, body.cells.map { definite(it, x, a, b) })
        val bothConstant = a.isConstant && b.isConstant && body.freeVars().all { it == x.name }
        val numeric = if (bothConstant) runCatching { numericIntegral(body, x, a, b) }.getOrNull() else null
        val f = runCatching { integrate(body, x) }.getOrNull()
        if (f != null) {
            // F(b) − F(a), with limits at infinite ends: ∫₀^∞ e^(−x) dx = 1.
            fun at(end: Expr, fromBelow: Boolean): Expr =
                if (isInfinite(end)) limit(f, x, end) else Algebra.simplify(f.subst(x, end))
            val exact = runCatching { Algebra.simplify(sub(at(b, true), at(a, false))) }.getOrNull()?.takeIf { !it.contains { e -> e == INF } }
            if (exact != null) {
                if (numeric == null) return exact
                val v = runCatching { Numeric.real(exact) }.getOrNull()
                if (v != null && kotlin.math.abs(v - numeric) <= 1e-7 * maxOf(1.0, kotlin.math.abs(numeric))) return exact
            }
        }
        if (numeric != null) return Flt(numeric)
        // No closed form, and it depends on other letters (like the inner integral of a
        // double integral): keep it as an integral, to be done numerically later.
        if (!bothConstant) return Fn("integral", listOf(body, x, a, b))
        throw MathError("Couldn't find this integral")
    }

    /** Gauss–Kronrod, with infinite ends mapped onto a finite interval: x = a + t/(1 − t) and so on. */
    private fun numericIntegral(body: Expr, x: Sym, a: Expr, b: Expr): Double {
        val f = { t: Double ->
            val v = runCatching { Numeric.real(body, mapOf(x.name to t)) }.getOrNull()
            // Far out along an infinite range, overflow means the integrand has died away.
            if ((v == null || !v.isFinite()) && kotlin.math.abs(t) > 1e12) 0.0 else v ?: Double.NaN
        }
        return when {
            !isInfinite(a) && !isInfinite(b) -> Numerics.integrate(f, Numeric.real(a), Numeric.real(b))
            isMinusInfinity(a) && isPlusInfinity(b) ->
                Numerics.integrate({ t -> val d = 1 - t * t; f(t / d) * (1 + t * t) / (d * d) }, -1.0, 1.0)
            isPlusInfinity(b) -> {
                val lo = Numeric.real(a)
                Numerics.integrate({ t -> val d = 1 - t; f(lo + t / d) / (d * d) }, 0.0, 1.0)
            }
            isMinusInfinity(a) -> {
                val hi = Numeric.real(b)
                Numerics.integrate({ t -> val d = 1 - t; f(hi - t / d) / (d * d) }, 0.0, 1.0)
            }
            else -> -numericIntegral(body, x, b, a)
        }
    }

    // ---- Limits --------------------------------------------------------------------------------

    fun isPlusInfinity(e: Expr) = e == INF
    fun isMinusInfinity(e: Expr) = e is Mul && e.factors == listOf(MINUS_ONE, INF)
    fun isInfinite(e: Expr) = isPlusInfinity(e) || isMinusInfinity(e)

    /**
     * lim f(x) as x → a, from both sides ([side] 0), the right (+1) or the left (−1).
     * At ±∞, x is replaced by ±1/t with t → 0⁺. Otherwise: direct substitution,
     * then cancelling, then L'Hôpital's rule for 0/0, and numerically as a last
     * resort, which also recognizes limits that are ±∞.
     */
    /** A held Σ whose upper limit → ∞ as x → [a] is worked out as an infinite sum. */
    private fun replaceInfiniteSums(e: Expr, x: Sym, a: Expr): Expr = when {
        e is Fn && e.name == "sum" && e.args.size == 4 && e.args[2].freeOf(x) &&
            runCatching { isInfinite(limit(e.args[3], x, a)) }.getOrDefault(false) ->
            (e.args[1] as? Sym)?.let { infiniteSum(e.args[0], it, e.args[2]) } ?: e
        e is Add -> Add(e.terms.map { replaceInfiniteSums(it, x, a) })
        e is Mul -> Mul(e.factors.map { replaceInfiniteSums(it, x, a) })
        e is Pow -> Pow(replaceInfiniteSums(e.base, x, a), replaceInfiniteSums(e.exp, x, a))
        e is Fn -> Fn(e.name, e.args.map { replaceInfiniteSums(it, x, a) })
        else -> e
    }

    /** Held ∮s whose radius → ∞ as x → [a] (center fixed) become 2πi times the sum of all residues. */
    private fun replaceGrowingContours(e: Expr, x: Sym, a: Expr): Expr = when {
        e is Fn && e.name == "contour" && e.args.size == 4 && e.args[2].freeOf(x) &&
            runCatching { isInfinite(limit(e.args[3], x, a)) }.getOrDefault(false) ->
            mul(TWO, PI, I, com.example.cas.graph.ComplexIntegrals.allResidues(e.args[0], e.args[1] as Sym))
        e is Add -> Add(e.terms.map { replaceGrowingContours(it, x, a) })
        e is Mul -> Mul(e.factors.map { replaceGrowingContours(it, x, a) })
        e is Pow -> Pow(replaceGrowingContours(e.base, x, a), replaceGrowingContours(e.exp, x, a))
        e is Fn -> Fn(e.name, e.args.map { replaceGrowingContours(it, x, a) })
        else -> e
    }

    fun limit(f: Expr, x: Sym, a: Expr, side: Int = 0, depth: Int = 0): Expr {
        // Matrices and vectors: the limit of each entry.
        if (f is Mat) return Mat(f.rows, f.cols, f.cells.map { limit(it, x, a, side, depth) })
        if (isInfinite(a) && f.contains { it is Fn && it.name == "sum" && it.args.size == 4 }) {
            val done = replaceInfiniteSums(f, x, a)
            if (done != f) return limit(Algebra.simplify(done), x, a, side, depth)
        }
        if (isInfinite(a) && f.contains { it is Fn && it.name == "contour" && it.args.size == 4 }) {
            // A circle growing without bound eventually encloses every pole: ∮ → 2πi Σ Res.
            val grown = replaceGrowingContours(f, x, a)
            if (grown != f) return limit(Algebra.simplify(grown), x, a, side, depth)
        }
        if (isInfinite(a)) {
            val t = Sym("\u0001t")
            val sign = if (isPlusInfinity(a)) ONE else MINUS_ONE
            return limit(Algebra.simplify(f.subst(x, div(sign, t))), t, ZERO, side = 1, depth = depth)
        }
        runCatching { Algebra.simplify(f.subst(x, a)) }.getOrNull()?.let { if (finite(it)) return it }
        // |B| and (B²ᵐ)ʳ near a: from one side B has one sign, so they can be written without the
        // absolute value (x → 0⁺: (x²)^{3/2} = x³), which L'Hôpital can then work with. For a
        // limit from both sides, each side is worked out that way and they must agree.
        if (depth < 6 && a.isConstant && f.contains { hasSignedRoot(it) }) {
            if (side != 0) {
                val resolved = resolveSigns(f, x, a, side)
                if (resolved != f) return limit(Algebra.simplify(resolved), x, a, side, depth + 1)
                leadingPowers(f, x, a, side, depth)?.let { (c, rest) ->
                    return Algebra.simplify(mul(c, limit(Algebra.simplify(rest), x, a, side, depth + 1)))
                }
            } else {
                val l = runCatching { limit(f, x, a, -1, depth) }.getOrNull()
                val r = runCatching { limit(f, x, a, 1, depth) }.getOrNull()
                if (l != null && r != null) {
                    if (l == r) return r
                    val lv = runCatching { Numeric.real(l) }.getOrNull()
                    val rv = runCatching { Numeric.real(r) }.getOrNull()
                    if (lv != null && rv != null && kotlin.math.abs(lv - rv) <= 1e-9 * maxOf(1.0, kotlin.math.abs(lv))) return r
                    throw MathError("The limits from the left and right differ")
                }
            }
        }
        val simplified = Algebra.simplify(f)
        if (simplified != f) runCatching { Algebra.simplify(simplified.subst(x, a)) }.getOrNull()?.let { if (finite(it)) return it }
        if (depth < 6) {
            // Negative fractional powers count as the denominator too: sin x/√x is sin x over x^{1/2}.
            val (n, d) = Algebra.together(f).let { nd -> if (nd.second.freeOf(x)) negativePowersApart(f) ?: nd else nd }
            if (!d.freeOf(x)) {
                val nv = runCatching { Algebra.simplify(n.subst(x, a)) }.getOrNull()
                val dv = runCatching { Algebra.simplify(d.subst(x, a)) }.getOrNull()
                if (nv != null && dv != null && LinearAlgebra.isZero(nv) && LinearAlgebra.isZero(dv)) {
                    val next = runCatching { limit(Algebra.simplify(div(diff(n, x), diff(d, x))), x, a, side, depth + 1) }.getOrNull()
                    if (next != null) return next
                }
            }
        }
        return numericLimit(f, x, a, side)
    }

    /** A product with some negative powers as (the other factors, those powers made positive); null if none. */
    private fun negativePowersApart(f: Expr): Pair<Expr, Expr>? {
        val factors = if (f is Mul) f.factors else listOf(f)
        val num = ArrayList<Expr>()
        val den = ArrayList<Expr>()
        for (g in factors) {
            val r = ((g as? Pow)?.exp as? Num)?.q
            if (g is Pow && r != null && r.num.signum() < 0) den += pow(g.base, Num(Rational.of(r.num.negate(), r.den))) else num += g
        }
        if (den.isEmpty()) return null
        return mul(num) to mul(den)
    }

    /** |B|, or a fractional power Bʳ: both depend on the sign of B near the point. */
    private fun hasSignedRoot(e: Expr): Boolean = (e is Fn && e.name == "abs" && e.args.size == 1) ||
        (e is Pow && (e.exp as? Num)?.q?.isInteger == false)

    private fun isEvenRoot(e: Expr): Boolean = e is Pow && e.base is Pow &&
        ((e.base as Pow).exp as? Num)?.q?.let { it.isInteger && it.num.testBit(0).not() } == true && (e.exp as? Num)?.q?.isInteger == false

    /**
     * Fractional powers Bʳ among [f]'s factors whose base → 0 at [a] from [side]: B is t^k·Q with
     * t = |x − a| and Q → c > 0, so Bʳ is t^{kr}·Qʳ and Qʳ → cʳ. Returns (the product of the cʳ,
     * f with each Bʳ replaced by t^{kr}), or null if there's none. So
     * (x − sin x)/(x sin x)^{3/2} as x → 0⁺ is 1 · lim (x − sin x)/x³ = 1/6, which L'Hôpital can do.
     */
    private fun leadingPowers(f: Expr, x: Sym, a: Expr, side: Int, depth: Int): Pair<Expr, Expr>? {
        val t = if (side > 0) sub(x, a) else sub(a, x)
        val factors = if (f is Mul) f.factors else listOf(f)
        val constants = ArrayList<Expr>()
        var changed = false
        val out = factors.map { g ->
            val r = ((g as? Pow)?.exp as? Num)?.q
            if (g !is Pow || r == null || r.isInteger) return@map g
            val b0 = runCatching { limit(g.base, x, a, side, depth + 1) }.getOrNull()
            if (b0 == null || !LinearAlgebra.isZero(b0)) return@map g
            for (k in 1..6) {
                val q = runCatching { limit(Algebra.simplify(div(g.base, pow(t, k.toLong()))), x, a, side, depth + 1) }.getOrNull() ?: break
                if (isInfinite(q)) break
                if (LinearAlgebra.isZero(q)) continue
                val qv = runCatching { Numeric.real(q) }.getOrNull() ?: break
                if (qv <= 0) break
                val replaced = pow(t, Num(Rational.of(k.toLong()) * r))
                // Already a plain power of x − a: nothing to gain.
                if (replaced == g) break
                constants += pow(q, g.exp)
                changed = true
                return@map replaced
            }
            g
        }
        if (!changed) return null
        return mul(constants) to mul(out)
    }

    /** [e] with |B| and (B²ᵐ)ʳ rewritten by the sign B has just beside [a] on [side]. */
    private fun resolveSigns(e: Expr, x: Sym, a: Expr, side: Int): Expr {
        val at = runCatching { Numeric.real(a) }.getOrNull() ?: return e
        fun sign(b: Expr): Int {
            // The sign a little way off, checked twice so a sign change right there doesn't mislead.
            val near = listOf(1e-6, 1e-8).map { h -> runCatching { Numeric.real(b, mapOf(x.name to at + side * h)) }.getOrNull() ?: return 0 }
            return when { near.all { it > 0 } -> 1; near.all { it < 0 } -> -1; else -> 0 }
        }
        fun walk(e: Expr): Expr = when {
            e is Fn && e.name == "abs" && e.args.size == 1 -> {
                val b = walk(e.args[0])
                when (sign(b)) { 1 -> b; -1 -> neg(b); else -> Fn("abs", listOf(b)) }
            }
            e is Pow && isEvenRoot(e) -> {
                val inner = e.base as Pow
                val b = walk(inner.base)
                val power = Num((inner.exp as Num).q * (e.exp as Num).q)
                when (sign(b)) { 1 -> Pow(b, power); -1 -> Pow(neg(b), power); else -> Pow(Pow(b, inner.exp), e.exp) }
            }
            e is Add -> Add(e.terms.map { walk(it) })
            e is Mul -> Mul(e.factors.map { walk(it) })
            e is Pow -> Pow(walk(e.base), walk(e.exp))
            e is Fn -> Fn(e.name, e.args.map { walk(it) })
            else -> e
        }
        return walk(e)
    }

    private fun finite(e: Expr): Boolean {
        if (e.contains { it == INF }) return false
        if (!e.isConstant) return true
        val v = runCatching { Numeric.eval(e) }.getOrNull() ?: return false
        return v.re.isFinite() && v.im.isFinite()
    }

    private fun numericLimit(f: Expr, x: Sym, a: Expr, sideWanted: Int): Expr {
        // f^g with f → 1 and g → ±∞ (or 0^0, ∞^0): exp(lim g·ln f), which L'Hôpital can do.
        // This also covers letters other than x, as in Π_k lim_{x→∞} (1 + k/x)^x = e^k.
        if (f is Pow) {
            val base = runCatching { limit(f.base, x, a, sideWanted) }.getOrNull()
            val exp = runCatching { limit(f.exp, x, a, sideWanted) }.getOrNull()
            val indeterminate = (base == ONE && exp != null && isInfinite(exp)) ||
                (base == ZERO && exp == ZERO) || (base != null && isInfinite(base) && exp == ZERO)
            if (indeterminate) {
                val logLimit = limit(mul(f.exp, fn("ln", f.base)), x, a, sideWanted)
                return when {
                    isInfinite(logLimit) -> if (Simplify.isNegative(logLimit)) ZERO else INF
                    else -> Algebra.simplify(pow(E, logLimit))
                }
            }
        }
        if (!f.freeVars().all { it == x.name } || !a.isConstant) throw MathError("Couldn't find this limit")
        val p = Numeric.real(a)
        /** The limit from one side: a number, ±Infinity, or null if it doesn't settle. */
        fun side(sign: Int): Double? {
            val vals = (3..10).mapNotNull { k ->
                runCatching { Numeric.real(f, mapOf(x.name to p + sign * Math.pow(10.0, -k.toDouble()))) }.getOrNull()?.takeIf { !it.isNaN() }
            }
            if (vals.size < 4) return null
            val last = vals.last()
            val prev = vals[vals.size - 2]
            if (!last.isFinite()) return last
            // Growing without bound in one direction.
            val tail = vals.takeLast(4)
            if (kotlin.math.abs(last) > 1e7 && tail.zipWithNext().all { (u, w) -> kotlin.math.abs(w) > kotlin.math.abs(u) * 1.5 && u * w > 0 }) {
                return if (last > 0) Double.POSITIVE_INFINITY else Double.NEGATIVE_INFINITY
            }
            if (kotlin.math.abs(last - prev) <= 1e-5 * maxOf(1.0, kotlin.math.abs(last))) return last
            // Shrinking steadily towards 0 (like √h): the limit is 0.
            if (kotlin.math.abs(last) < 1e-4 && tail.zipWithNext().all { (u, w) -> kotlin.math.abs(w) < kotlin.math.abs(u) * 0.8 }) return 0.0
            return null
        }
        fun toExpr(v: Double): Expr = when {
            v == Double.POSITIVE_INFINITY -> INF
            v == Double.NEGATIVE_INFINITY -> neg(INF)
            else -> Flt(Math.round(v * 1e8) / 1e8)
        }
        if (sideWanted != 0) return toExpr(side(sideWanted) ?: throw MathError("The limit doesn't exist"))
        val l = side(-1)
        val r = side(1)
        if (l == null || r == null) throw MathError("The limit doesn't exist")
        if (l.isInfinite() || r.isInfinite()) {
            if (l == r) return toExpr(l)
            throw MathError("The limits from the left and right differ")
        }
        if (kotlin.math.abs(l - r) > 1e-5 * maxOf(1.0, kotlin.math.abs(l))) throw MathError("The limits from the left and right differ")
        return toExpr((l + r) / 2)
    }

    // ---- Taylor polynomials ---------------------------------------------------------------------

    fun taylor(f: Expr, x: Sym, at: Expr, order: Int): Expr {
        if (order !in 0..30) throw MathError("Use an order from 0 to 30")
        var d = f
        val terms = ArrayList<Expr>()
        var factorial = BigInteger.ONE
        for (k in 0..order) {
            if (k > 0) { d = Algebra.simplify(diff(d, x)); factorial *= BigInteger.valueOf(k.toLong()) }
            val c = Algebra.simplify(d.subst(x, at))
            terms += mul(c, Num(Rational.of(BigInteger.ONE, factorial)), pow(sub(x, at), k.toLong()))
        }
        return add(terms)
    }

    // ---- Sums ---------------------------------------------------------------------------------------

    /**
     * Σ body for k from lo to hi. With numeric limits the terms are added up;
     * with a symbolic upper limit and a polynomial body, the closed form is
     * found by interpolation: Σ k = n(n + 1)/2.
     */
    /** Σ or Π kept unevaluated: args are body, variable, lower, upper. */
    private fun held(body: Expr, k: Sym, lo: Expr, hi: Expr, product: Boolean) =
        Fn(if (product) "product" else "sum", listOf(body, k, lo, hi))

    /**
     * Σ_{k=lo}^{∞} of c·k^p: ζ(−p) when p ≤ −2 (minus the terms before lo), ∞ when it diverges.
     * Null when the body isn't a power of k.
     */
    fun infiniteSum(body: Expr, k: Sym, lo: Expr): Expr? {
        geometricLimit(body, k, lo)?.let { return it }
        val factors = if (body is Mul) body.factors else listOf(body)
        var power: Rational? = null
        val rest = ArrayList<Expr>()
        for (f in factors) {
            when {
                f == k -> { if (power != null) return null; power = Rational.ONE }
                f is Pow && f.base == k && f.exp is Num -> { if (power != null) return null; power = (f.exp as Num).q }
                f.freeOf(k) -> rest += f
                else -> return null
            }
        }
        val p = power ?: Rational.ZERO
        val c = if (rest.isEmpty()) ONE else mul(rest)
        val s = -p   // Σ 1/k^s
        if (s <= Rational.ONE) return if (Numeric.real(c) >= 0) INF else neg(INF)
        val start = (lo as? Num)?.q?.takeIf { it.isInteger }?.num?.toLong() ?: return null
        if (start < 1) return null
        // ζ(s) counts from k = 1, so drop the terms before lo.
        val head = (1 until start).map { i -> pow(Num(Rational.of(i)), Num(p)) }
        return Algebra.simplify(mul(c, sub(fn("zeta", Num(s)), add(head))))
    }

    /** Σ_{k=lo}^{∞} c·rᵏ = c·r^lo/(1 − r) when |r| < 1; ±∞ otherwise. */
    private fun geometricLimit(body: Expr, k: Sym, lo: Expr): Expr? {
        val whole = geometricSum(body, k, lo, Sym("\u0001hi")) ?: return null
        val r = geometricRatio(body, k) ?: return null
        val rv = runCatching { kotlin.math.abs(Numeric.real(r)) }.getOrNull() ?: return null
        if (rv >= 1) return INF
        // The r^{hi+1} term vanishes: substitute 0 for it.
        return Algebra.simplify(whole.subst(pow(r, add(Sym("\u0001hi"), ONE)), ZERO))
    }

    /** c·rᵏ (r free of k, r ≠ 1) summed from lo to hi, or null if the body isn't of that shape. */
    private var lastRatio: Expr? = null

    /** The r of a c·rᵏ body, found while working out its sum. */
    private fun geometricRatio(body: Expr, k: Sym): Expr? {
        lastRatio = null
        geometricSum(body, k, ZERO, ONE)
        return lastRatio
    }

    private fun geometricSum(body: Expr, k: Sym, lo: Expr, hi: Expr): Expr? {
        val factors = if (body is Mul) body.factors else listOf(body)
        var ratio: Expr? = null
        val rest = ArrayList<Expr>()
        for (f in factors) {
            // rᵏ, or 1/rᵏ written as r^(−k).
            val isPower = f is Pow && f.base.freeOf(k) && !f.exp.freeOf(k)
            if (isPower) {
                val p = f as Pow
                val coeff = Algebra.coefficients(Algebra.expand(p.exp), k) ?: return null
                // The exponent must be k itself (degree 1, no constant term beyond a factor).
                if (coeff.size != 2 || coeff[0] != ZERO) return null
                val m = coeff[1]
                if (!m.isConstant) return null
                if (ratio != null) return null
                ratio = Algebra.simplify(pow(p.base, m))
            } else {
                if (!f.freeOf(k)) return null
                rest += f
            }
        }
        val r = ratio ?: return null
        if (r == ONE) return null
        lastRatio = r
        val c = if (rest.isEmpty()) ONE else mul(rest)
        return Algebra.simplify(div(mul(c, sub(pow(r, lo), pow(r, add(hi, ONE)))), sub(ONE, r)))
    }

    fun sum(body: Expr, k: Sym, lo: Expr, hi: Expr, product: Boolean): Expr {
        val loN = (lo as? Num)?.q?.takeIf { it.isInteger }?.num
        val hiN = (hi as? Num)?.q?.takeIf { it.isInteger }?.num
        if (loN != null && hiN != null) {
            val count = hiN - loN + BigInteger.ONE
            if (count.signum() <= 0) return if (product) ONE else ZERO
            if (count > BigInteger.valueOf(5000)) {
                if (!body.freeVars().all { it == k.name }) throw MathError("That's too many terms")
                var acc = if (product) 1.0 else 0.0
                var i = loN
                while (i <= hiN) {
                    val v = Numeric.real(body, mapOf(k.name to i.toDouble()))
                    acc = if (product) acc * v else acc + v
                    i += BigInteger.ONE
                }
                return Flt(acc)
            }
            val terms = ArrayList<Expr>()
            var i = loN
            while (i <= hiN) { terms += body.subst(k, Num(Rational.of(i))); i += BigInteger.ONE }
            return if (product) mul(terms) else Algebra.simplify(add(terms))
        }
        // A symbolic upper limit with no closed form stays as it is (a limit may finish it:
        // lim_{n→∞} Σ 1/k² = ζ(2)); the display and numerics handle a held sum.
        if (product || loN == null) return held(body, k, lo, hi, product)
        // Geometric: Σ_{k=a}^{b} c·rᵏ = c(rᵃ − rᵇ⁺¹)/(1 − r), so lim_{b→∞} works for |r| < 1.
        geometricSum(body, k, lo, hi)?.let { return it }
        val cs = Algebra.coefficients(Algebra.expand(body), k) ?: return held(body, k, lo, hi, product)
        val degree = cs.size - 1
        // S(m) for m = lo … lo + degree + 1, then the polynomial through those points.
        val points = ArrayList<Pair<Expr, Expr>>()
        var acc: Expr = ZERO
        for (j in 0..degree + 1) {
            val m = loN + BigInteger.valueOf(j.toLong())
            acc = add(acc, body.subst(k, Num(Rational.of(m))))
            points += Num(Rational.of(m)) to acc
        }
        val n = Sym("\u0001n")
        val poly = add(points.mapIndexed { i, (xi, yi) ->
            var term: Expr = yi
            points.forEachIndexed { j, (xj, _) -> if (j != i) term = mul(term, div(sub(n, xj), sub(xi, xj))) }
            term
        })
        val closed = Algebra.expand(poly)
        return Algebra.factor(closed).let { if (it.freeVars().size <= 1) it else closed }.subst(n, hi)
    }
}
