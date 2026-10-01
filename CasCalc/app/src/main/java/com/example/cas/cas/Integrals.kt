package com.example.cas.cas

import com.example.cas.math.Rational

/**
 * Antiderivatives beyond the basic table, from the shape of the integrand:
 *  - e^(ax² + bx + c) × polynomial → erf / erfi (completing the square, then reduction);
 *  - sin, cos, sinh, cosh, e^(…) of a linear term over a power of a linear term → Si, Ci, Shi,
 *    Chi, Ei (reduction by parts to the first power);
 *  - sin, cos of a quadratic → the Fresnel S and C; x^k/ln x → Ei, li;
 *  - x^p e^(−ax) for non-whole p → Γ(p + 1, ax);
 *  - ln(c + dx)/x, ln x/(x + c), Li_s(ax)/x → dilogarithms and polylogarithms;
 *  - (c₀ + c₁ sin²u)^(±½) → the elliptic integrals F and E;
 *  - √(quadratic) and its reciprocal → asin, asinh-type logarithms; other R(x, √quadratic) by a
 *    trigonometric or hyperbolic substitution;
 *  - sinᵐ u cosⁿ u (whole m, n of any sign: tan, sec, csc too) by reduction formulas;
 *  - e^(ax) sin(bx), e^(ax) cos(bx) directly;
 *  - quartic denominators x⁴ + px² + q split over the reals.
 * And rewrites tried when nothing else works: radicals (ax + b)^(p/q) and eᵃˣ and ln x
 * substituted away, rational functions of sin and cos by t = tan(u/2), products of sines and
 * cosines turned into sums, hyperbolic functions into exponentials, and integration by parts
 * with the logarithm or inverse function as the part to differentiate.
 */
object Integrals {
    private fun factors(e: Expr) = if (e is Mul) e.factors else listOf(e)

    /** (a, b) of a·x + b with a ≠ 0, both free of x. */
    fun linear(u: Expr, x: Sym): Pair<Expr, Expr>? {
        val cs = Algebra.coefficients(Algebra.expand(u), x) ?: return null
        if (cs.size != 2 || cs[1] == ZERO || !cs[0].freeOf(x) || !cs[1].freeOf(x)) return null
        return cs[1] to cs[0]
    }

    /** (a, b, c) of a·x² + b·x + c with a ≠ 0. */
    private fun quadratic(u: Expr, x: Sym): Triple<Expr, Expr, Expr>? {
        val cs = Algebra.coefficients(Algebra.expand(u), x) ?: return null
        if (cs.size != 3 || cs[2] == ZERO || cs.any { !it.freeOf(x) }) return null
        return Triple(cs[2], cs[1], cs[0])
    }

    /** The sign of a constant when it can be told (numbers, or a leading minus), else null. */
    private fun sign(c: Expr): Int? {
        if (c.isConstant) runCatching { Numeric.eval(c) }.getOrNull()?.takeIf { it.isReal }?.let { return if (it.re > 0) 1 else if (it.re < 0) -1 else 0 }
        return if (Simplify.isNegative(c)) -1 else null
    }

    private fun simp(e: Expr) = Algebra.simplify(e)

    // ---- The patterns -------------------------------------------------------------------------

    fun special(e: Expr, x: Sym, depth: Int): Expr? =
        rationalFull(e, x) ?: gaussian(e, x) ?: overLinear(e, x) ?: fresnel(e, x) ?: logIntegral(e, x) ?: incompleteGamma(e, x) ?: dilog(e, x) ?:
            elliptic(e, x) ?: sqrtQuadratic(e, x) ?: sinCos(e, x) ?: expTrig(e, x) ?: quartic(e, x)

    // ---- Rational functions in full ------------------------------------------------------------

    /**
     * P/Q over the rationals with any denominator that splits into rational linear and quadratic
     * factors (repeated or not) and at most one biquadratic quartic left over (x⁴ + 1): partial
     * fractions solved exactly, then each piece integrated, powers of a quadratic by reduction.
     */
    private fun rationalFull(e: Expr, x: Sym): Expr? {
        val (n, d) = Algebra.together(e)
        if (d.freeOf(x)) return null
        val p = Algebra.qpoly(n, x) ?: return null
        val q = Algebra.qpoly(d, x) ?: return null
        if (q.degree < 2 || q.degree > 16) return null
        val (whole, rem) = p.divMod(q)
        val out = ArrayList<Expr>()
        if (!whole.isZero) out += Calculus.integrate(whole.toExpr(x), x) ?: return null
        if (rem.isZero) return add(out)
        val qm = q.monic()
        // Linear factors.
        var rest = qm
        val linear = ArrayList<Pair<Rational, Int>>()
        for (r in Algebra.rationalRoots(rest)) {
            var m = 0
            val lin = QPoly(listOf(-r, Rational.ONE))
            while (true) { val (qq, rr) = rest.divMod(lin); if (!rr.isZero) break; rest = qq; m++ }
            if (m > 0) linear += r to m
        }
        // Quadratic factors with rational coefficients, from pairs of complex roots.
        val quads = ArrayList<Pair<QPoly, Int>>()
        if (rest.degree >= 2) {
            val roots = complexRoots(rest)
            val tried = HashSet<String>()
            for (z in roots) {
                if (kotlin.math.abs(z.im) < 1e-9) continue
                val b = rationalNear(-2 * z.re) ?: continue
                val c = rationalNear(z.re * z.re + z.im * z.im) ?: continue
                val quad = QPoly(listOf(c, b, Rational.ONE))
                if (!tried.add(quad.c.toString())) continue
                var m = 0
                while (rest.degree >= 2) { val (qq, rr) = rest.divMod(quad); if (!rr.isZero) break; rest = qq; m++ }
                if (m > 0) quads += quad to m
            }
        }
        // What's left: nothing, or one biquadratic quartic (x⁴ + px² + q).
        val quartic = when {
            rest.degree <= 0 -> null
            rest.degree == 4 && rest[1].signum == 0 && rest[3].signum == 0 -> rest
            else -> return null
        }
        if (linear.isEmpty() && quads.size <= 1 && quartic == null && quads.all { it.second == 1 }) return null // the basic method's case
        // Unknown numerators, each with its denominator.
        class Term(val den: QPoly, val xPower: Int, val kind: Int, val root: Rational? = null, val quad: QPoly? = null, val power: Int = 1)
        val terms = ArrayList<Term>()
        for ((r, m) in linear) for (j in 1..m) {
            var den = QPoly(listOf(Rational.ONE)); repeat(j) { den *= QPoly(listOf(-r, Rational.ONE)) }
            terms += Term(den, 0, 0, root = r, power = j)
        }
        for ((qd, m) in quads) for (j in 1..m) {
            var den = QPoly(listOf(Rational.ONE)); repeat(j) { den *= qd }
            terms += Term(den, 1, 1, quad = qd, power = j); terms += Term(den, 0, 1, quad = qd, power = j)
        }
        if (quartic != null) for (k in 0..3) terms += Term(quartic, k, 2)
        val size = terms.size
        if (size != qm.degree) return null
        val basis = terms.map { t -> qm.divMod(t.den).first * QPoly(List(t.xPower) { Rational.ZERO } + Rational.ONE) }
        val target = rem.scale(q.lead.reciprocal())
        val a = Array(size) { row -> Array(size + 1) { col -> if (col < size) basis[col][row] else target[row] } }
        val coef = solve(a) ?: return null
        // Integrate each piece.
        var i = 0
        while (i < size) {
            val t = terms[i]
            when (t.kind) {
                0 -> {
                    val c = coef[i]
                    if (c.signum != 0) {
                        val u = sub(x, Num(t.root!!))
                        out += if (t.power == 1) mul(Num(c), fn("ln", fn("abs", u))) else div(Num(-c), mul(Num((t.power - 1).toLong()), pow(u, (t.power - 1).toLong())))
                    }
                    i++
                }
                1 -> {
                    val bc = coef[i]; val cc = coef[i + 1]
                    if (bc.signum != 0 || cc.signum != 0) out += overQuadraticPower(Num(bc), Num(cc), t.quad!!, t.power, x)
                    i += 2
                }
                else -> {
                    val num = QPoly((0..3).map { coef[i + it] })
                    if (!num.isZero) out += quartic(div(num.toExpr(x), quartic!!.toExpr(x)), x) ?: return null
                    i += 4
                }
            }
        }
        return add(out)
    }

    /** ∫ (bx + c)/Qʲ, Q = x² + px + s irreducible: the ln/power part, then Iⱼ = ∫ dx/Qʲ by reduction. */
    private fun overQuadraticPower(b: Expr, c: Expr, quad: QPoly, j: Int, x: Sym): Expr {
        val pp = Num(quad[1]); val s = Num(quad[0])
        val q = quad.toExpr(x)
        val h = add(x, div(pp, TWO))
        val a2 = simp(sub(s, div(pow(pp, TWO), Num(4)))) // a² > 0
        val a = sqrt(a2)
        // bx + c = (b/2)(2x + p) + (c − bp/2)
        val first = if (j == 1) mul(div(b, TWO), fn("ln", q)) else mul(div(b, TWO), div(pow(q, Num((1 - j).toLong())), Num((1 - j).toLong())))
        val k = simp(sub(c, div(mul(b, pp), TWO)))
        fun ij(n: Int): Expr = if (n == 1) div(fn("atan", div(h, a)), a)
            else add(div(h, mul(TWO, a2, Num((n - 1).toLong()), pow(q, Num((n - 1).toLong())))), mul(div(Num((2 * n - 3).toLong()), mul(TWO, a2, Num((n - 1).toLong()))), ij(n - 1)))
        return add(first, mul(k, ij(j)))
    }

    private fun solve(m: Array<Array<Rational>>): List<Rational>? {
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

    /** All complex roots of a polynomial (Durand–Kerner). */
    private fun complexRoots(p: QPoly): List<CD> {
        val n = p.degree
        val a = (0..n).map { p[it].toDouble() / p.lead.toDouble() }
        fun eval(z: CD): CD { var r = CD(0.0); for (k in n downTo 0) r = r * z + CD(a[k]); return r }
        var z = List(n) { k -> CD(0.4, 0.9).let { b -> var w = CD(1.0); repeat(k) { w = w * b }; w } }
        repeat(500) {
            z = z.mapIndexed { i, zi ->
                var den = CD(1.0)
                for (j in z.indices) if (j != i) den = den * (zi - z[j])
                if (den.abs() == 0.0) zi else zi - eval(zi) / den
            }
        }
        return z
    }

    /** A fraction with a small denominator within 1e-8 of [v], if there's one. */
    private fun rationalNear(v: Double): Rational? {
        for (den in 1..60) {
            val num = Math.round(v * den)
            if (kotlin.math.abs(num.toDouble() / den - v) < 1e-8) return Rational.of(num, den.toLong())
        }
        return null
    }

    /** e^(Q(x)) × P(x), Q quadratic: completing the square, then xⁿ e^Q by reduction. */
    private fun gaussian(e: Expr, x: Sym): Expr? {
        var q: Triple<Expr, Expr, Expr>? = null
        val rest = ArrayList<Expr>()
        for (f in factors(e)) {
            if (f is Pow && f.base.freeOf(x) && !f.exp.freeOf(x)) {
                if (q != null) return null
                val exponent = if (f.base == E) f.exp else mul(f.exp, fn("ln", f.base))
                q = quadratic(exponent, x) ?: return null
            } else rest += f
        }
        val (a, b, c) = q ?: return null
        val p = Algebra.coefficients(Algebra.expand(mul(rest)), x) ?: return null
        if (p.any { !it.freeOf(x) }) return null
        val eq = pow(E, add(mul(a, pow(x, TWO)), mul(b, x), c))
        // I₀ = ∫ e^Q: e^(c − b²/4a) √π/(2√k) erf(√k (x + b/2a)), k = −a (erfi when a > 0).
        val shift = add(x, div(b, mul(TWO, a)))
        val front = pow(E, sub(c, div(pow(b, TWO), mul(Num(4), a))))
        val i0 = if (sign(a) == 1) mul(front, div(sqrt(PI), mul(TWO, sqrt(a))), fn("erfi", mul(sqrt(a), shift)))
            else mul(front, div(sqrt(PI), mul(TWO, sqrt(neg(a)))), fn("erf", mul(sqrt(neg(a)), shift)))
        // Iₙ = x^(n−1) e^Q/(2a) − (n−1)/(2a) Iₙ₋₂ − b/(2a) Iₙ₋₁
        val ins = ArrayList<Expr>()
        for (n in p.indices) {
            ins += when (n) {
                0 -> i0
                1 -> sub(div(eq, mul(TWO, a)), mul(div(b, mul(TWO, a)), ins[0]))
                else -> sub(sub(div(mul(pow(x, (n - 1).toLong()), eq), mul(TWO, a)), mul(div(Num((n - 1).toLong()), mul(TWO, a)), ins[n - 2])), mul(div(b, mul(TWO, a)), ins[n - 1]))
            }
        }
        return add(p.indices.map { mul(p[it], ins[it]) })
    }

    /** g(Mx + N)/(px + q)ⁿ with g one of sin, cos, sinh, cosh, exp: Si, Ci, Shi, Chi, Ei. */
    private fun overLinear(e: Expr, x: Sym): Expr? {
        var g: String? = null
        var m: Expr? = null
        var den: Pair<Expr, Int>? = null
        for (f in factors(e)) {
            when {
                f is Fn && f.name in setOf("sin", "cos", "sinh", "cosh") && f.args.size == 1 && linear(f.args[0], x) != null -> {
                    if (g != null) return null; g = f.name; m = f.args[0]
                }
                f is Pow && f.base.freeOf(x) && linear(f.exp, x) != null -> {
                    if (g != null) return null; g = "exp"; m = if (f.base == E) f.exp else mul(f.exp, fn("ln", f.base))
                }
                f is Pow && f.exp is Num && (f.exp as Num).q.isInteger && (f.exp as Num).q.signum < 0 && linear(f.base, x) != null -> {
                    if (den != null) return null; den = f.base to -(f.exp as Num).q.num.toInt()
                }
                else -> return null
            }
        }
        val name = g ?: return null
        val (l, n) = den ?: return null
        val (am, bm) = linear(m!!, x)!!
        val (pl, ql) = linear(l, x)!!
        // u = px + q: Mx + N = αu + β
        val alpha = simp(div(am, pl))
        val beta = simp(sub(bm, div(mul(am, ql), pl)))
        val u = Sym("\u0001u")
        // g(αu + β) = Σ c·h(αu) with h among sin, cos, sinh, cosh, exp
        val pieces: List<Pair<Expr, String>> = if (beta == ZERO) listOf(ONE to name) else when (name) {
            "sin" -> listOf(fn("cos", beta) to "sin", fn("sin", beta) to "cos")
            "cos" -> listOf(fn("cos", beta) to "cos", neg(fn("sin", beta)) to "sin")
            "sinh" -> listOf(fn("cosh", beta) to "sinh", fn("sinh", beta) to "cosh")
            "cosh" -> listOf(fn("cosh", beta) to "cosh", fn("sinh", beta) to "sinh")
            else -> listOf(pow(E, beta) to "exp")
        }
        fun h(name: String, arg: Expr): Expr = if (name == "exp") pow(E, arg) else fn(name, arg)
        fun derivative(name: String): Pair<Expr, String> = when (name) {
            "sin" -> ONE to "cos"; "cos" -> MINUS_ONE to "sin"; "sinh" -> ONE to "cosh"; "cosh" -> ONE to "sinh"; else -> ONE to "exp"
        }
        // J(h, n) = ∫ h(αu) u^(−n) du
        fun j(name: String, n: Int): Expr {
            val au = mul(alpha, u)
            if (n == 1) return when (name) {
                "sin" -> fn("si", au); "cos" -> fn("ci", au); "sinh" -> fn("shi", au); "cosh" -> fn("chi", au); else -> fn("ei", au)
            }
            if (n <= 0) return Calculus.integrate(mul(h(name, au), pow(u, Num((-n).toLong()))), u) ?: throw MathError("no")
            // −h(αu) u^(1−n)/(n−1) + α/(n−1) ∫ h′(αu) u^(1−n)
            val (c, d) = derivative(name)
            return add(div(neg(mul(h(name, au), pow(u, Num((1 - n).toLong())))), Num((n - 1).toLong())), mul(div(mul(alpha, c), Num((n - 1).toLong())), j(d, n - 1)))
        }
        return runCatching { div(add(pieces.map { (c, hn) -> mul(c, j(hn, n)) }).subst(u, l), pl) }.getOrNull()
    }

    /** sin(Q), cos(Q) for quadratic Q: the Fresnel integrals, after completing the square. */
    private fun fresnel(e: Expr, x: Sym): Expr? {
        val f = e as? Fn ?: return null
        if (f.name !in setOf("sin", "cos") || f.args.size != 1) return null
        val (a, b, c) = quadratic(f.args[0], x) ?: return null
        val s = sign(a) ?: 1
        val k = if (s < 0) neg(a) else a
        val w = add(x, div(b, mul(TWO, a)))
        val r = simp(sub(c, div(pow(b, TWO), mul(Num(4), a))))
        // ∫ sin(k w²) = √(π/2k) S(√(2k/π) w), ∫ cos(k w²) = √(π/2k) C(√(2k/π) w)
        val scale = sqrt(div(PI, mul(TWO, k)))
        val arg = mul(sqrt(div(mul(TWO, k), PI)), w)
        val sInt = mul(Num(s.toLong()), scale, fn("fresnels", arg))
        val cInt = mul(scale, fn("fresnelc", arg))
        // sin(±k w² + r) and cos(…) by the addition formulas.
        return if (f.name == "sin") add(mul(fn("cos", r), sInt), mul(fn("sin", r), cInt)) else sub(mul(fn("cos", r), cInt), mul(fn("sin", r), sInt))
    }

    /** x^k / ln x = Ei((k + 1) ln x) (li x when k = 0). */
    private fun logIntegral(e: Expr, x: Sym): Expr? {
        var k: Expr = ZERO
        var found = false
        for (f in factors(e)) when {
            f is Pow && f.exp == MINUS_ONE && f.base == fn("ln", x) -> found = true
            f == x -> k = add(k, ONE)
            f is Pow && f.base == x && f.exp.freeOf(x) -> k = add(k, f.exp)
            else -> return null
        }
        if (!found) return null
        k = simp(k)
        if (k == ZERO) return fn("li", x)
        if (k == MINUS_ONE) return null
        return fn("ei", mul(add(k, ONE), fn("ln", x)))
    }

    /** x^p e^(−ax + c), p not whole: −e^c a^(−p−1) Γ(p + 1, ax). */
    private fun incompleteGamma(e: Expr, x: Sym): Expr? {
        var p: Expr? = null
        var lin: Pair<Expr, Expr>? = null
        for (f in factors(e)) when {
            f is Pow && f.base == x && f.exp.freeOf(x) -> { if (p != null) return null; p = f.exp }
            f is Pow && f.base == E && linear(f.exp, x) != null -> { if (lin != null) return null; lin = linear(f.exp, x) }
            else -> return null
        }
        val power = p ?: return null
        val (s, c) = lin ?: return null
        if ((power as? Num)?.q?.isInteger == true) return null
        val a = simp(neg(s))
        if (sign(a) != 1) return null
        return mul(neg(pow(E, c)), pow(a, sub(MINUS_ONE, power)), fn("gammainc", add(power, ONE), mul(a, x)))
    }

    /** ln(c + dx)/x, ln x/(px + q), Li_s(ax)/x. */
    private fun dilog(e: Expr, x: Sym): Expr? {
        val fs = factors(e)
        if (fs.size != 2) return null
        val logF = fs.firstOrNull { it is Fn && it.name == "ln" } as? Fn
        val polyF = fs.firstOrNull { it is Fn && it.name == "polylog" } as? Fn
        val other = fs.first { it !== logF && it !== polyF }
        if (polyF != null && other == pow(x, MINUS_ONE)) {
            val (s, z) = polyF.args
            if (!s.freeOf(x)) return null
            val (a, b) = linear(z, x) ?: return null
            if (b != ZERO) return null
            return fn("polylog", add(s, ONE), mul(a, x))
        }
        logF ?: return null
        val arg = logF.args[0]
        if (other == pow(x, MINUS_ONE)) {
            // ln(c + dx)/x = ln c · ln|x| − Li₂(−dx/c)
            val (d, c) = linear(arg, x) ?: return null
            if (c == ZERO) return null
            return sub(mul(fn("ln", c), fn("ln", fn("abs", x))), fn("polylog", TWO, neg(div(mul(d, x), c))))
        }
        if (arg == x && other is Pow && other.exp == MINUS_ONE) {
            // ln x/(px + q) = (1/p)[ln x · ln(1 + x/c) + Li₂(−x/c)], c = q/p
            val (pp, qq) = linear(other.base, x) ?: return null
            if (qq == ZERO) return null
            val c = div(qq, pp)
            return div(add(mul(fn("ln", x), fn("ln", add(ONE, div(x, c)))), fn("polylog", TWO, neg(div(x, c)))), pp)
        }
        return null
    }

    /** (c₀ + c₁ sin²u)^(±½), u linear (cos² written as 1 − sin²): √c₀^(±1) F/E(u | −c₁/c₀)/a. */
    private fun elliptic(e: Expr, x: Sym): Expr? {
        val p = e as? Pow ?: return null
        val half = p.exp == HALF
        if (!half && p.exp != num(-1, 2)) return null
        var u: Expr? = null
        p.base.contains { if (it is Fn && (it.name == "sin" || it.name == "cos") && it.args.size == 1 && linear(it.args[0], x) != null) { if (u == null) u = it.args[0]; true } else false }
        val arg = u ?: return null
        val s = Sym("\u0001s")
        val inS = simp(p.base.subst(pow(fn("cos", arg), TWO), sub(ONE, pow(s, TWO))).subst(fn("sin", arg), s))
        if (!inS.freeOf(x)) return null
        val cs = Algebra.coefficients(Algebra.expand(inS), s) ?: return null
        if (cs.size != 3 || cs[1] != ZERO || cs.any { !it.isConstant }) return null
        val (c0, _, c1) = cs
        if (sign(c0) != 1) return null
        val m = simp(neg(div(c1, c0)))
        val (a, _) = linear(arg, x)!!
        return if (half) div(mul(sqrt(c0), fn("elliptice", arg, m)), a) else div(fn("ellipticf", arg, m), mul(sqrt(c0), a))
    }

    /** √Q and 1/√Q for quadratic Q, and other R(x, √Q) by substitution. */
    private fun sqrtQuadratic(e: Expr, x: Sym): Expr? {
        // The square root in it (only one kind).
        var root: Pow? = null
        fun halfPower(z: Expr) = z is Pow && z.exp is Num && (z.exp as Num).q.den == java.math.BigInteger.TWO
        e.contains { if (halfPower(it) && quadratic((it as Pow).base, x) != null) { if (root == null || root!!.base == it.base) { root = it }; true } else false }
        val r = root ?: return null
        val (c2, c1, c0) = quadratic(r.base, x)!!
        val s2 = sign(c2) ?: return null
        if (!c2.isConstant || !c1.isConstant || !c0.isConstant) return null
        val w = add(x, div(c1, mul(TWO, c2)))
        val d = simp(sub(c0, div(pow(c1, TWO), mul(Num(4), c2))))
        val sd = sign(d) ?: return null
        val q = r.base
        if (e == r && (r.exp == HALF || r.exp == num(-1, 2))) {
            return if (r.exp == HALF) when {
                s2 < 0 && sd > 0 -> { val k = neg(c2); add(div(mul(w, sqrt(q)), TWO), mul(div(d, mul(TWO, sqrt(k))), fn("asin", mul(w, sqrt(div(k, d)))))) }
                s2 > 0 -> add(div(mul(w, sqrt(q)), TWO), mul(div(d, mul(TWO, sqrt(c2))), fn("ln", fn("abs", add(mul(sqrt(c2), w), sqrt(q))))))
                else -> null
            } else when {
                s2 < 0 && sd > 0 -> { val k = neg(c2); div(fn("asin", mul(w, sqrt(div(k, d)))), sqrt(k)) }
                s2 > 0 -> div(fn("ln", fn("abs", add(mul(sqrt(c2), w), sqrt(q)))), sqrt(c2))
                else -> null
            }
        }
        // Otherwise substitute: w = √(d/k) sin θ (c₂ < 0), √(d/c₂) sinh θ (c₂, d > 0), √(−d/c₂) cosh θ (c₂ > 0 > d).
        val th = Sym("\u0001θ")
        val (wOf, rootOf, back) = when {
            s2 < 0 && sd > 0 -> { val k = neg(c2); Triple(mul(sqrt(div(d, k)), fn("sin", th)), mul(sqrt(d), fn("cos", th)), fn("asin", mul(w, sqrt(div(k, d))))) }
            s2 > 0 && sd > 0 -> Triple(mul(sqrt(div(d, c2)), fn("sinh", th)), mul(sqrt(d), fn("cosh", th)), fn("asinh", mul(w, sqrt(div(c2, d)))))
            s2 > 0 && sd < 0 -> Triple(mul(sqrt(div(neg(d), c2)), fn("cosh", th)), mul(sqrt(neg(d)), fn("sinh", th)), fn("acosh", mul(w, sqrt(div(c2, neg(d))))))
            else -> return null
        }
        val xOf = sub(wOf, div(c1, mul(TWO, c2)))
        val dx = Calculus.diff(xOf, th)
        val inTheta = simp(mul(e.subst(r.base, pow(rootOf, TWO)).subst(x, xOf), dx).let { replaceRoot(it, rootOf) })
        if (!inTheta.freeOf(x)) return null
        val g = Calculus.integrate(inTheta, th, 3) ?: return null
        return simp(g.subst(th, back))
    }

    /** (rootOf²)^(±½) back to rootOf^(±1) (the substitution keeps the root positive). */
    private fun replaceRoot(e: Expr, rootOf: Expr): Expr = when {
        e is Pow && e.base == pow(rootOf, TWO) && e.exp is Num && (e.exp as Num).q.den == java.math.BigInteger.TWO -> pow(rootOf, Num((e.exp as Num).q.num.toLong()))
        else -> e.map { replaceRoot(it, rootOf) }
    }

    /** sinᵐ(u) cosⁿ(u) (tan, sec, csc as powers of sin and cos), u = ax + b, whole m and n. */
    private fun sinCos(e: Expr, x: Sym): Expr? {
        var u: Expr? = null
        var m = 0; var n = 0
        for (f in factors(e)) {
            val (base, power) = if (f is Pow && f.exp is Num && (f.exp as Num).q.isInteger) f.base to (f.exp as Num).q.num.toInt() else f to 1
            val fnB = base as? Fn ?: return null
            if (fnB.name !in setOf("sin", "cos", "tan") || fnB.args.size != 1) return null
            if (u == null) u = fnB.args[0] else if (u != fnB.args[0]) return null
            when (fnB.name) { "sin" -> m += power; "cos" -> n += power; else -> { m += power; n -= power } }
        }
        val arg = u ?: return null
        val (a, _) = linear(arg, x) ?: return null
        if (m == 0 && n == 0) return null
        return runCatching { div(sinCosPowers(m, n, arg), a) }.getOrNull()
    }

    /** ∫ sinᵐ u cosⁿ u du for whole m, n (any signs), by the reduction formulas. */
    fun sinCosPowers(m: Int, n: Int, u: Expr): Expr {
        val s = fn("sin", u); val c = fn("cos", u)
        fun sc(i: Int, j: Int) = mul(pow(s, Num(i.toLong())), pow(c, Num(j.toLong())))
        return when {
            m == 0 && n == 0 -> u
            m == 1 && n == 0 -> neg(c)
            m == 0 && n == 1 -> s
            m == 0 && n == -1 -> fn("ln", fn("abs", div(add(ONE, s), c)))
            m == -1 && n == 0 -> fn("ln", fn("abs", div(sub(ONE, c), s)))
            m == -1 && n == -1 -> fn("ln", fn("abs", div(s, c)))
            // An odd positive power of cos: t = sin u, cos u du = dt, cos² = 1 − t².
            n > 0 && n % 2 == 1 -> {
                val t = Sym("\u0001t")
                val g = Calculus.integrate(Algebra.expand(mul(pow(t, Num(m.toLong())), pow(sub(ONE, pow(t, TWO)), Num(((n - 1) / 2).toLong())))), t) ?: throw MathError("no")
                g.subst(t, s)
            }
            m > 0 && m % 2 == 1 -> {
                val t = Sym("\u0001t")
                val g = Calculus.integrate(Algebra.expand(mul(pow(t, Num(n.toLong())), pow(sub(ONE, pow(t, TWO)), Num(((m - 1) / 2).toLong())))), t) ?: throw MathError("no")
                neg(g.subst(t, c))
            }
            n >= 2 && m + n != 0 -> add(div(sc(m + 1, n - 1), Num((m + n).toLong())), mul(Num(Rational.of((n - 1).toLong(), (m + n).toLong())), sinCosPowers(m, n - 2, u)))
            m >= 2 && m + n != 0 -> add(div(neg(sc(m - 1, n + 1)), Num((m + n).toLong())), mul(Num(Rational.of((m - 1).toLong(), (m + n).toLong())), sinCosPowers(m - 2, n, u)))
            n <= -2 -> add(div(neg(sc(m + 1, n + 1)), Num((n + 1).toLong())), mul(Num(Rational.of((m + n + 2).toLong(), (n + 1).toLong())), sinCosPowers(m, n + 2, u)))
            m <= -2 -> add(div(sc(m + 1, n + 1), Num((m + 1).toLong())), mul(Num(Rational.of((m + n + 2).toLong(), (m + 1).toLong())), sinCosPowers(m + 2, n, u)))
            // tanᵏ with m + n = 0, k ≥ 2: ∫ tanᵏ = tanᵏ⁻¹/(k − 1) − ∫ tanᵏ⁻²
            m + n == 0 && m >= 2 -> sub(div(sc(m - 1, -(m - 1)), Num((m - 1).toLong())), sinCosPowers(m - 2, n + 2, u))
            m + n == 0 && n >= 2 -> sub(neg(div(sc(-(n - 1), n - 1), Num((n - 1).toLong()))), sinCosPowers(m + 2, n - 2, u))
            else -> throw MathError("no")
        }
    }

    /** e^(ax + c) sin(bx + d) and cos: (a sin − b cos)/(a² + b²) and (a cos + b sin)/(a² + b²) times e^(…). */
    private fun expTrig(e: Expr, x: Sym): Expr? {
        val fs = factors(e)
        if (fs.size != 2) return null
        val ex = fs.firstOrNull { it is Pow && it.base.freeOf(x) && linear(it.exp, x) != null } as? Pow ?: return null
        val tr = fs.firstOrNull { it is Fn && (it.name == "sin" || it.name == "cos") && linear(it.args[0], x) != null } as? Fn ?: return null
        val a = linear(ex.exp, x)!!.first.let { if (ex.base == E) it else mul(it, fn("ln", ex.base)) }
        val (b, _) = linear(tr.args[0], x)!!
        val v = tr.args[0]
        val den = add(pow(a, TWO), pow(b, TWO))
        val inner = if (tr.name == "sin") sub(mul(a, fn("sin", v)), mul(b, fn("cos", v))) else add(mul(a, fn("cos", v)), mul(b, fn("sin", v)))
        return div(mul(ex, inner), den)
    }

    /** N(x)/(x⁴ + px² + q) with x⁴ + px² + q = (x² + sx + r)(x² − sx + r), r = √q, s = √(2r − p). */
    private fun quartic(e: Expr, x: Sym): Expr? {
        val (n, d) = Algebra.together(e)
        val dc = Algebra.coefficients(Algebra.expand(d), x) ?: return null
        if (dc.size != 5 || dc[1] != ZERO || dc[3] != ZERO || dc.any { it !is Num }) return null
        val lead = (dc[4] as Num).q
        val p = (dc[2] as Num).q / lead; val q = (dc[0] as Num).q / lead
        if (q.signum <= 0) return null
        val rv = kotlin.math.sqrt(q.toDouble())
        if (2 * rv - p.toDouble() <= 0) return null
        val nc = Algebra.coefficients(Algebra.expand(n), x) ?: return null
        if (nc.size > 4 || nc.any { !it.freeOf(x) }) return null
        val r = sqrt(Num(q)); val s = sqrt(sub(mul(TWO, r), Num(p)))
        // 1/D, x/D, x²/D, x³/D each as (Ax + B)/(x² + sx + r) + (Cx + D)/(x² − sx + r).
        fun parts(k: Int): List<Expr> = when (k) {
            0 -> listOf(div(ONE, mul(TWO, r, s)), div(ONE, mul(TWO, r)), div(MINUS_ONE, mul(TWO, r, s)), div(ONE, mul(TWO, r)))
            1 -> listOf(ZERO, div(MINUS_ONE, mul(TWO, s)), ZERO, div(ONE, mul(TWO, s)))
            2 -> listOf(div(MINUS_ONE, mul(TWO, s)), ZERO, div(ONE, mul(TWO, s)), ZERO)
            else -> listOf(HALF, div(r, mul(TWO, s)), HALF, div(neg(r), mul(TWO, s)))
        }
        fun overQuad(a: Expr, b: Expr, sl: Expr): Expr {
            // (ax + b)/(x² + sl x + r) = a/2 ln(x² + sl x + r) + (b − a sl/2)·2/√(4r − sl²) atan((2x + sl)/√(4r − sl²))
            val root = sqrt(sub(mul(Num(4), r), pow(sl, TWO)))
            return add(mul(div(a, TWO), fn("ln", add(pow(x, TWO), mul(sl, x), r))), mul(sub(b, div(mul(a, sl), TWO)), div(TWO, root), fn("atan", div(add(mul(TWO, x), sl), root))))
        }
        val out = ArrayList<Expr>()
        nc.forEachIndexed { k, ck ->
            if (ck == ZERO) return@forEachIndexed
            val (a1, b1, a2, b2) = parts(k)
            out += mul(ck, add(overQuad(a1, b1, s), overQuad(a2, b2, neg(s))))
        }
        return div(add(out), Num(lead))
    }

    // ---- Rewrites, when nothing else works ------------------------------------------------------

    fun rewrites(e: Expr, x: Sym, depth: Int): Expr? =
        parts(e, x, depth) ?: multipleAngles(e, x, depth) ?: radical(e, x, depth) ?: powerSub(e, x, depth) ?: expSub(e, x, depth) ?: logSub(e, x, depth) ?:
            productToSum(e, x, depth) ?: weierstrass(e, x, depth) ?: hyperbolic(e, x, depth)

    /** By parts with u the logarithm, inverse function or erf (to any power) and dv the rest. */
    private fun parts(e: Expr, x: Sym, depth: Int): Expr? {
        val fs = factors(e)
        val inverse = setOf("ln", "atan", "asin", "acos", "asinh", "acosh", "atanh", "erf", "erfi", "si", "ci", "ei", "polylog")
        val u = fs.firstOrNull { f ->
            val b = if (f is Pow && f.exp is Num && (f.exp as Num).q.isInteger && (f.exp as Num).q.signum > 0) f.base else f
            b is Fn && b.name in inverse && !b.freeOf(x)
        } ?: return null
        val dv = mul(fs.filter { it !== u })
        if (dv.contains { it is Fn && it.name in inverse && !it.freeOf(x) }) return null
        val v = Calculus.integrate(dv, x, depth + 1) ?: return null
        val rest = Calculus.integrate(simp(mul(v, Calculus.diff(u, x))), x, depth + 1) ?: return null
        return sub(mul(u, v), rest)
    }

    /** sin(kU), cos(kU) beside sin U, cos U (k = 2, 3, 4): written in sin U and cos U. */
    private fun multipleAngles(e: Expr, x: Sym, depth: Int): Expr? {
        val args = LinkedHashSet<Expr>()
        e.contains { if (it is Fn && (it.name == "sin" || it.name == "cos") && !it.freeOf(x)) args += it.args[0]; false }
        if (args.size < 2) return null
        val base = args.firstOrNull { u -> args.all { a -> a == u || (simp(div(a, u)) as? Num)?.q?.let { it.isInteger && it.num.toInt() in 2..4 } == true } } ?: return null
        val sn = fn("sin", base); val cs = fn("cos", base)
        val c2 = sub(pow(cs, TWO), pow(sn, TWO)); val s2 = mul(TWO, sn, cs)
        fun rw(z: Expr): Expr {
            if (z is Fn && (z.name == "sin" || z.name == "cos") && z.args[0] != base && !z.freeOf(x)) {
                val k = (simp(div(z.args[0], base)) as? Num)?.q?.num?.toInt()
                if (k != null) return when (z.name to k) {
                    "sin" to 2 -> s2; "cos" to 2 -> c2
                    "sin" to 3 -> sub(mul(Num(3), sn), mul(Num(4), pow(sn, Num(3)))); "cos" to 3 -> sub(mul(Num(4), pow(cs, Num(3))), mul(Num(3), cs))
                    "sin" to 4 -> mul(TWO, s2, c2); "cos" to 4 -> sub(mul(TWO, pow(c2, TWO)), ONE)
                    else -> z
                }
            }
            return z.map { rw(it) }
        }
        val r = rw(e)
        if (r == e) return null
        return Calculus.integrate(simp(r), x, depth + 1)
    }

    /** (ax + b)^(p/q) in it: u = (ax + b)^(1/q), x = (u^q − b)/a. */
    private fun radical(e: Expr, x: Sym, depth: Int): Expr? {
        var base: Expr? = null
        var q = 1L
        var ok = true
        e.contains {
            if (it is Pow && it.exp is Num && !(it.exp as Num).q.isInteger && !it.base.freeOf(x)) {
                if (linear(it.base, x) == null || (base != null && base != it.base)) ok = false
                base = it.base
                q = lcm(q, (it.exp as Num).q.den.toLong())
            }
            false
        }
        val l = base ?: return null
        if (!ok || q == 1L) return null
        val (a, b) = linear(l, x)!!
        val u = Sym("\u0001r")
        val xOf = div(sub(pow(u, Num(q)), b), a)
        val dx = Calculus.diff(xOf, u)
        val inU = simp(mul(substRoot(e, l, u, q).subst(x, xOf), dx))
        if (!inU.freeOf(x)) return null
        val g = Calculus.integrate(inU, u, depth + 1) ?: return null
        return g.subst(u, pow(l, Num(Rational.of(1, q))))
    }

    private fun substRoot(e: Expr, l: Expr, u: Sym, q: Long): Expr = when {
        e is Pow && e.base == l && e.exp is Num -> pow(u, Num((e.exp as Num).q * Rational.of(q)))
        e == l -> pow(u, Num(q))
        else -> e.map { substRoot(it, l, u, q) }
    }

    private tailrec fun gcdL(x: Long, y: Long): Long = if (y == 0L) x else gcdL(y, x % y)
    private fun lcm(a: Long, b: Long): Long = a / gcdL(a, b) * b

    /** x^(n−1)·R(xⁿ): u = xⁿ. */
    private fun powerSub(e: Expr, x: Sym, depth: Int): Expr? {
        val t = Sym("\u0001p")
        for (n in 2..4) {
            val q = simp(div(e, mul(Num(n.toLong()), pow(x, Num((n - 1).toLong())))))
            val inT = simp(q.subst(x, pow(t, Num(Rational.of(1, n.toLong())))))
            if (!inT.freeOf(x) || inT.contains { it is Pow && it.exp is Num && !(it.exp as Num).q.isInteger && !it.base.freeOf(t) }) continue
            val g = Calculus.integrate(inT, t, depth + 1) ?: continue
            return g.subst(t, pow(x, Num(n.toLong())))
        }
        return null
    }

    /** Everything in x inside e^(kx): u = e^(gx), dx = du/(gu). */
    private fun expSub(e: Expr, x: Sym, depth: Int): Expr? {
        val ks = ArrayList<Rational>()
        var ok = true
        fun scan(z: Expr) {
            if (!ok) return
            if (z is Pow && z.base == E && !z.exp.freeOf(x)) {
                val (a, _) = linear(z.exp, x) ?: run { ok = false; return }
                ks += (a as? Num)?.q ?: run { ok = false; return }
                return
            }
            if (z == x) { ok = false; return }
            z.children.forEach { scan(it) }
        }
        scan(e)
        if (!ok || ks.isEmpty()) return null
        var g = ks[0].abs()
        for (k in ks) g = gcdQ(g, k.abs())
        val u = Sym("\u0001e")
        fun sub(z: Expr): Expr = if (z is Pow && z.base == E && !z.exp.freeOf(x)) {
            val (a, b) = linear(z.exp, x)!!
            mul(pow(E, b), pow(u, Num((a as Num).q / g)))
        } else z.map { sub(it) }
        val inU = simp(div(sub(e), mul(Num(g), u)))
        if (!inU.freeOf(x)) return null
        val r = Calculus.integrate(inU, u, depth + 1) ?: return null
        return r.subst(u, pow(E, mul(Num(g), x)))
    }

    private fun gcdQ(a: Rational, b: Rational): Rational {
        // gcd of fractions: gcd(numerators)/lcm(denominators)
        val n = a.num.gcd(b.num); val d = a.den / a.den.gcd(b.den) * b.den
        return Rational.of(n, d)
    }

    /** f(ln x) with x = eᵘ: dx = eᵘ du. */
    private fun logSub(e: Expr, x: Sym, depth: Int): Expr? {
        if (!e.contains { it is Fn && it.name == "ln" && it.args[0] == x }) return null
        val u = Sym("\u0001l")
        val inU = simp(mul(e.subst(fn("ln", x), u).subst(x, pow(E, u)), pow(E, u)))
        if (!inU.freeOf(x) || inU.contains { it is Fn && it.name == "ln" }) return null
        val g = Calculus.integrate(inU, u, depth + 1) ?: return null
        return g.subst(u, fn("ln", x))
    }

    /** Products and powers of sin and cos into sums of single sines and cosines. */
    private fun productToSum(e: Expr, x: Sym, depth: Int): Expr? {
        val fs = factors(e)
        val trig = ArrayList<Fn>()
        val rest = ArrayList<Expr>()
        for (f in fs) {
            val (b, k) = if (f is Pow && f.exp is Num && (f.exp as Num).q.isInteger && (f.exp as Num).q.signum > 0) f.base to (f.exp as Num).q.num.toInt() else f to 1
            if (b is Fn && (b.name == "sin" || b.name == "cos") && linear(b.args[0], x) != null && k <= 8) repeat(k) { trig += b } else rest += f
        }
        if (trig.size < 2) return null
        var sum: Expr = trig[0]
        for (t in trig.drop(1)) sum = Algebra.expand(multiplyOut(sum, t))
        val whole = Algebra.expand(mul(mul(rest), sum))
        if (whole == e) return null
        return Calculus.integrate(whole, x, depth + 1)
    }

    /** (sum of c·trig) × trig, each product made a sum. */
    private fun multiplyOut(sum: Expr, t: Fn): Expr {
        val terms = if (sum is Add) sum.terms else listOf(sum)
        return add(terms.map { term ->
            val fs = factors(term)
            val tr = fs.firstOrNull { it is Fn && (it.name == "sin" || it.name == "cos") } as? Fn ?: return@map mul(term, t)
            val c = mul(fs.filter { it !== tr })
            val a = tr.args[0]; val b = t.args[0]
            val r = when (tr.name to t.name) {
                "sin" to "sin" -> div(sub(fn("cos", sub(a, b)), fn("cos", add(a, b))), TWO)
                "cos" to "cos" -> div(add(fn("cos", sub(a, b)), fn("cos", add(a, b))), TWO)
                "sin" to "cos" -> div(add(fn("sin", add(a, b)), fn("sin", sub(a, b))), TWO)
                else -> div(add(fn("sin", add(a, b)), fn("sin", sub(b, a))), TWO)
            }
            mul(c, r)
        })
    }

    /** A rational function of sin u and cos u with them in the denominator: t = tan(u/2). */
    private fun weierstrass(e: Expr, x: Sym, depth: Int): Expr? {
        var u: Expr? = null
        var ok = true
        e.contains {
            if (it is Fn && (it.name == "sin" || it.name == "cos" || it.name == "tan")) {
                if (u == null) u = it.args[0] else if (u != it.args[0]) ok = false
            }
            false
        }
        val arg = u ?: return null
        if (!ok) return null
        val (a, _) = linear(arg, x) ?: return null
        val (_, d) = Algebra.together(e)
        if (d.freeOf(x)) return null
        val t = Sym("\u0001w")
        val one = add(ONE, pow(t, TWO))
        val inT = simp(mul(e.subst(fn("sin", arg), div(mul(TWO, t), one)).subst(fn("cos", arg), div(sub(ONE, pow(t, TWO)), one))
            .subst(fn("tan", arg), div(mul(TWO, t), sub(ONE, pow(t, TWO)))), div(TWO, mul(a, one))))
        if (!inT.freeOf(x)) return null
        val g = Calculus.integrate(inT, t, depth + 1) ?: return null
        return g.subst(t, fn("tan", div(arg, TWO)))
    }

    /** sinh, cosh, tanh written with exponentials. */
    private fun hyperbolic(e: Expr, x: Sym, depth: Int): Expr? {
        if (!e.contains { it is Fn && it.name in setOf("sinh", "cosh", "tanh") && !it.freeOf(x) }) return null
        fun rw(z: Expr): Expr = if (z is Fn && !z.freeOf(x) && z.args.size == 1) {
            val a = rw(z.args[0])
            when (z.name) {
                "sinh" -> div(sub(pow(E, a), pow(E, neg(a))), TWO)
                "cosh" -> div(add(pow(E, a), pow(E, neg(a))), TWO)
                "tanh" -> div(sub(pow(E, mul(TWO, a)), ONE), add(pow(E, mul(TWO, a)), ONE))
                else -> Fn(z.name, listOf(a))
            }
        } else z.map { rw(it) }
        val ex = Algebra.expand(rw(e))
        if (ex == e) return null
        return Calculus.integrate(ex, x, depth + 1)
    }
}
