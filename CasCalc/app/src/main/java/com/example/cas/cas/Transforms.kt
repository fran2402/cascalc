package com.example.cas.cas

/**
 * Integral transforms, worked out symbolically from tables and rules (linearity, shifts,
 * scaling, modulation, multiplication by t):
 *
 * - Laplace: ℒ{f}(s) = ∫₀^∞ f(t) e^(−st) dt, and its inverse by partial fractions.
 * - Fourier: ℱ{f}(ω) = ∫ f(t) e^(−iωt) dt (angular frequency, no factor in front), and its
 *   inverse f(t) = (1/2π) ∫ F(ω) e^(iωt) dω.
 *
 * The step H and the impulse δ appear as functions named "heaviside" and "dirac"; rect and tri
 * as "rect" and "tri" (the evaluator keeps them by name inside a transform).
 */
object Transforms {
    private fun fail(what: String): Nothing = throw MathError("No $what transform found for that")

    /** u as k·x + b (k and b free of x), or null. */
    private fun linear(u: Expr, x: Sym): Pair<Expr, Expr>? {
        val c = Algebra.coefficients(Algebra.expand(u), x) ?: return null
        if (c.size > 2 || c.any { !it.freeOf(x) }) return null
        return c.getOrElse(1) { ZERO } to c[0]
    }

    private fun positive(e: Expr): Boolean = runCatching { Numeric.real(e) > 0 }.getOrDefault(false)
    private fun negative(e: Expr): Boolean = runCatching { Numeric.real(e) < 0 }.getOrDefault(false)
    private fun s(e: Expr) = Algebra.simplify(e)

    /** The terms of a sum, each split into its constant part and the factors with [x] in them. */
    private fun terms(e: Expr, x: Sym): List<Pair<Expr, List<Expr>>> {
        val all = Algebra.expand(e).let { if (it is Add) it.terms else listOf(it) }
        return all.map { t ->
            val f = if (t is Mul) t.factors else listOf(t)
            mul(f.filter { it.freeOf(x) }) to f.filter { !it.freeOf(x) }
        }
    }

    /** Powers of x among [fs] taken out: (n, the rest). */
    private fun power(fs: List<Expr>, x: Sym): Pair<Expr, List<Expr>> {
        var n: Expr = ZERO
        val rest = ArrayList<Expr>()
        for (f in fs) when {
            f == x -> n = add(n, ONE)
            f is Pow && f.base == x && f.exp.freeOf(x) -> n = add(n, f.exp)
            else -> rest += f
        }
        return s(n) to rest
    }

    /** A factor e^u with u linear in x: (k, b) for e^(kx + b). */
    private fun exponential(f: Expr, x: Sym): Pair<Expr, Expr>? =
        if (f is Pow && f.base == E) linear(f.exp, x) else null

    // ---- Laplace -------------------------------------------------------------------------

    /** ℒ{f(t)}(s). */
    fun laplace(f: Expr, t: Sym, sv: Sym): Expr = s(add(terms(f, t).map { (c, g) -> mul(c, laplaceOf(g, t, sv)) }))

    private fun laplaceOf(fs: List<Expr>, t: Sym, sv: Sym): Expr {
        if (fs.isEmpty()) return div(ONE, sv)
        // e^(kt + b) g(t) → e^b G(s − k).
        fs.indexOfFirst { exponential(it, t) != null }.takeIf { it >= 0 }?.let { k ->
            val (a, b) = exponential(fs[k], t)!!
            val g = laplaceOf(fs.filterIndexed { j, _ -> j != k }, t, sv)
            return mul(pow(E, b), g.subst(sv, sub(sv, a)))
        }
        // H(kt + b) g(t), the step at t₀ = −b/k: e^(−t₀s) ℒ{g(t + t₀)} (nothing before 0).
        fs.indexOfFirst { it is Fn && it.name == "heaviside" }.takeIf { it >= 0 }?.let { k ->
            val (a, b) = linear((fs[k] as Fn).args[0], t) ?: fail("Laplace")
            if (!positive(a)) fail("Laplace")
            val t0 = s(neg(div(b, a)))
            val rest = fs.filterIndexed { j, _ -> j != k }
            if (!positive(t0)) return laplaceOf(rest, t, sv)
            val shifted = mul(rest).subst(t, add(t, t0))
            return mul(pow(E, neg(mul(t0, sv))), laplace(shifted, t, sv))
        }
        // sgn(t − t₀) = 2H(t − t₀) − 1.
        fs.indexOfFirst { it is Fn && it.name == "sgn" }.takeIf { it >= 0 }?.let { k ->
            val u = (fs[k] as Fn).args[0]
            val rest = fs.filterIndexed { j, _ -> j != k }
            return sub(mul(TWO, laplace(mul(listOf(Fn("heaviside", listOf(u))) + rest), t, sv)), laplace(mul(rest), t, sv))
        }
        // rect(u) = H(u + ½) − H(u − ½).
        fs.indexOfFirst { it is Fn && it.name == "rect" }.takeIf { it >= 0 }?.let { k ->
            val u = (fs[k] as Fn).args[0]
            val rest = mul(fs.filterIndexed { j, _ -> j != k })
            return sub(laplace(mul(Fn("heaviside", listOf(add(u, HALF))), rest), t, sv), laplace(mul(Fn("heaviside", listOf(sub(u, HALF))), rest), t, sv))
        }
        // δ(kt + b) g(t): g(t₀) e^(−t₀s)/|k|.
        fs.indexOfFirst { it is Fn && it.name == "dirac" }.takeIf { it >= 0 }?.let { k ->
            val (a, b) = linear((fs[k] as Fn).args[0], t) ?: fail("Laplace")
            val t0 = s(neg(div(b, a)))
            if (negative(t0)) return ZERO
            val g = mul(fs.filterIndexed { j, _ -> j != k }).subst(t, t0)
            return mul(div(g, fn("abs", a)), pow(E, neg(mul(t0, sv))))
        }
        val (n, rest) = power(fs, t)
        if (rest.isEmpty()) {
            // tᵖ: Γ(p + 1)/s^(p + 1) (n!/s^(n + 1) for whole n).
            return div(fn("gamma", add(n, ONE)), pow(sv, add(n, ONE)))
        }
        // tⁿ g(t) → (−1)ⁿ dⁿG/dsⁿ.
        if (n != ZERO) {
            val k = (n as? Num)?.q?.takeIf { it.isInteger && it.signum > 0 }?.num?.toInt() ?: fail("Laplace")
            var g = laplaceOf(rest, t, sv)
            repeat(k) { g = s(Calculus.diff(g, sv)) }
            return mul(pow(MINUS_ONE, n), g)
        }
        val one = rest.singleOrNull() as? Fn ?: return integralLaplace(mul(fs), t, sv)
        val (a, b) = linear(one.args.getOrNull(0) ?: fail("Laplace"), t) ?: return integralLaplace(mul(fs), t, sv)
        val s2 = pow(sv, 2); val a2 = pow(a, 2)
        // sin(at + b) = sin(at)cos b + cos(at) sin b, and so on.
        return when (one.name) {
            "sin" -> div(add(mul(a, fn("cos", b)), mul(sv, fn("sin", b))), add(s2, a2))
            "cos" -> div(sub(mul(sv, fn("cos", b)), mul(a, fn("sin", b))), add(s2, a2))
            "sinh" -> div(add(mul(a, fn("cosh", b)), mul(sv, fn("sinh", b))), sub(s2, a2))
            "cosh" -> div(add(mul(sv, fn("cosh", b)), mul(a, fn("sinh", b))), sub(s2, a2))
            else -> integralLaplace(mul(fs), t, sv)
        }
    }

    /** No rule fits: the integral itself, when it can be done. */
    private fun integralLaplace(f: Expr, t: Sym, sv: Sym): Expr {
        val r = runCatching { Calculus.definite(mul(f, pow(E, neg(mul(sv, t)))), t, ZERO, INF) }.getOrNull()
        if (r == null || !r.freeOf(t) || r.contains { it is Fn && it.name in setOf("integral", "lim") }) fail("Laplace")
        return r
    }

    /** ℒ⁻¹{F(s)}(t). */
    fun inverseLaplace(big: Expr, sv: Sym, t: Sym): Expr {
        // Group the terms by their delay e^(−as): each group comes back shifted, times H(t − a).
        val groups = LinkedHashMap<Expr, MutableList<Expr>>()
        for ((c, fs) in terms(big, sv)) {
            var delay: Expr = ZERO
            var konst = c
            val rest = ArrayList<Expr>()
            for (f in fs) {
                val ex = exponential(f, sv)
                if (ex != null) { delay = s(add(delay, neg(ex.first))); konst = mul(konst, pow(E, ex.second)) } else rest += f
            }
            groups.getOrPut(delay) { ArrayList() } += mul(listOf(konst) + rest)
        }
        return s(add(groups.map { (delay, parts) ->
            val g = rational(add(parts), sv, t)
            if (delay == ZERO) g else {
                if (!positive(delay)) fail("inverse Laplace")
                mul(g.subst(t, sub(t, delay)), Fn("heaviside", listOf(sub(t, delay))))
            }
        }))
    }

    private fun rational(big: Expr, sv: Sym, t: Sym): Expr {
        val parts = runCatching { Algebra.apart(big, sv) }.getOrDefault(big)
        val list = if (parts is Add) parts.terms else listOf(parts)
        return add(list.map { term ->
            // Each partial fraction as written: the factors with a negative whole power are below the line.
            val fs = if (term is Mul) term.factors else listOf(term)
            val below = fs.filter { it is Pow && it.exp is Num && (it.exp as Num).q.isInteger && (it.exp as Num).q.signum < 0 && !it.base.freeOf(sv) }
            val num = mul(fs.filter { it !in below })
            when {
                below.isEmpty() && num.freeOf(sv) -> mul(num, Fn("dirac", listOf(t)))
                below.size == 1 -> {
                    val d = below[0] as Pow
                    fraction(num, d.base, (-(d.exp as Num).q.num.toInt()), sv, t) ?: power(term, sv, t) ?: fail("inverse Laplace")
                }
                else -> power(term, sv, t) ?: fail("inverse Laplace")
            }
        })
    }

    /** c·s^(−p): c t^(p−1)/Γ(p). */
    private fun power(term: Expr, sv: Sym, t: Sym): Expr? {
        val fs = if (term is Mul) term.factors else listOf(term)
        val c = mul(fs.filter { it.freeOf(sv) })
        val (n, rest) = power(fs.filter { !it.freeOf(sv) }, sv)
        if (rest.isNotEmpty() || !negative(n)) return null
        val p = neg(n)
        return mul(c, div(pow(t, sub(p, ONE)), fn("gamma", p)))
    }

    /** num/baseⁿ with base linear or quadratic in s. */
    private fun fraction(num: Expr, base: Expr, n: Int, sv: Sym, t: Sym): Expr? {
        val k: Expr = ONE
        val bc = Algebra.coefficients(Algebra.expand(base), sv) ?: return null
        val nc = Algebra.coefficients(Algebra.expand(num), sv) ?: return null
        if (bc.any { !it.freeOf(sv) } || nc.any { !it.freeOf(sv) }) return null
        return when (bc.size) {
            // c/(as + b)ⁿ = (c/aⁿ)/(s + b/a)ⁿ → (c/aⁿ) tⁿ⁻¹ e^(−bt/a)/(n − 1)!
            2 -> {
                if (nc.size != 1) return null
                val a = bc[1]; val root = neg(div(bc[0], a))
                mul(div(nc[0], mul(k, pow(a, n.toLong()))), div(pow(t, n - 1L), fn("fact", num((n - 1).toLong()))), pow(E, mul(root, t)))
            }
            // (As + B)/(a(s² + ps + q)): e^(αt)(A cos βt + (B + Aα)/β sin βt)/a, α = −p/2, β² = q − p²/4.
            3 -> {
                if (n != 1 || nc.size > 2) return null
                val a = bc[2]; val p = div(bc[1], a); val q = div(bc[0], a)
                val bigA = div(nc.getOrElse(1) { ZERO }, mul(k, a)); val bigB = div(nc[0], mul(k, a))
                val alpha = s(neg(div(p, TWO)))
                val beta2 = s(sub(q, div(pow(p, 2), num(4))))
                val second = add(bigB, mul(bigA, alpha))
                when {
                    positive(beta2) -> { val beta = sqrt(beta2); mul(pow(E, mul(alpha, t)), add(mul(bigA, fn("cos", mul(beta, t))), mul(div(second, beta), fn("sin", mul(beta, t))))) }
                    negative(beta2) -> { val g = sqrt(neg(beta2)); mul(pow(E, mul(alpha, t)), add(mul(bigA, fn("cosh", mul(g, t))), mul(div(second, g), fn("sinh", mul(g, t))))) }
                    else -> mul(pow(E, mul(alpha, t)), add(bigA, mul(second, t)))
                }
            }
            else -> null
        }
    }

    // ---- Fourier ------------------------------------------------------------------------

    /** ℱ{f(t)}(ω). */
    fun fourier(f: Expr, t: Sym, w: Sym): Expr = s(add(terms(f, t).map { (c, g) -> mul(c, fourierOf(g, t, w)) }))

    private fun fourierOf(fs: List<Expr>, t: Sym, w: Sym): Expr {
        val iw = mul(I, w)
        if (fs.isEmpty()) return mul(TWO, PI, Fn("dirac", listOf(w)))
        // t g(t) → i dG/dω.
        val (n, rest0) = power(fs, t)
        if (n != ZERO) {
            val k = (n as? Num)?.q?.takeIf { it.isInteger && it.signum > 0 }?.num?.toInt() ?: fail("Fourier")
            var g = fourierOf(rest0, t, w)
            repeat(k) { g = s(mul(I, Calculus.diff(g, w))) }
            return g
        }
        // δ(kt + b) g(t): g(t₀) e^(−iωt₀)/|k|.
        fs.indexOfFirst { it is Fn && it.name == "dirac" }.takeIf { it >= 0 }?.let { k ->
            val (a, b) = linear((fs[k] as Fn).args[0], t) ?: fail("Fourier")
            val t0 = s(neg(div(b, a)))
            val g = mul(fs.filterIndexed { j, _ -> j != k }).subst(t, t0)
            return mul(div(g, fn("abs", a)), pow(E, neg(mul(iw, t0))))
        }
        // cos(kt + b) g(t) and sin(kt + b) g(t): G shifted to ω ∓ k (modulation).
        fs.indexOfFirst { it is Fn && it.name in setOf("cos", "sin") && linear(it.args[0], t) != null }.takeIf { it >= 0 }?.let { k ->
            val f = fs[k] as Fn
            val (a, b) = linear(f.args[0], t)!!
            val g = fourierOf(fs.filterIndexed { j, _ -> j != k }, t, w)
            val up = mul(pow(E, mul(I, b)), g.subst(w, sub(w, a)))
            val down = mul(pow(E, neg(mul(I, b))), g.subst(w, add(w, a)))
            return if (f.name == "cos") div(add(up, down), TWO) else div(sub(up, down), mul(TWO, I))
        }
        // e^(iat) g(t): G(ω − a). Other exponentials are handled with their partner below.
        fs.indexOfFirst { f -> exponential(f, t)?.first?.let { k -> s(mul(k, I)).let { r -> r.freeOf(I) && r != ZERO } } == true }.takeIf { it >= 0 }?.let { k ->
            val (a, b) = exponential(fs[k], t)!!
            val shift = s(div(a, I))
            val g = fourierOf(fs.filterIndexed { j, _ -> j != k }, t, w)
            return mul(pow(E, b), g.subst(w, sub(w, shift)))
        }
        val one = fs.singleOrNull()
        // A step times a decaying exponential: e^(λt) for t past t₀ (or before it).
        val step = fs.firstOrNull { it is Fn && it.name == "heaviside" } as Fn?
        if (step != null) {
            val (k, b) = linear(step.args[0], t) ?: fail("Fourier")
            val t0 = s(neg(div(b, k)))
            val others = fs.filter { it !== step }
            if (others.isEmpty()) {
                // H(t − t₀) = e^(−iωt₀)(πδ(ω) + 1/(iω)); H(t₀ − t) with the sign of the second part flipped.
                val sign = if (positive(k)) ONE else MINUS_ONE
                return mul(pow(E, neg(mul(iw, t0))), add(mul(PI, Fn("dirac", listOf(w))), div(sign, iw)))
            }
            val ex = others.singleOrNull()?.let { exponential(it, t) } ?: fail("Fourier")
            val (lam, c) = ex
            val rate = sub(lam, iw)
            return when {
                positive(k) && negative(lam) -> mul(pow(E, c), div(pow(E, mul(rate, t0)), neg(rate)))
                !positive(k) && positive(lam) -> mul(pow(E, c), div(pow(E, mul(rate, t0)), rate))
                else -> fail("Fourier")
            }
        }
        if (one is Fn) {
            val (a, b) = linear(one.args[0], t) ?: fail("Fourier")
            val t0 = s(neg(div(b, a)))
            val shift = pow(E, neg(mul(iw, t0)))
            val v = div(w, a)
            return when (one.name) {
                // rect(at + b): sin(ω/2a)/(ω/2a) / |a|, shifted.
                "rect" -> mul(shift, div(ONE, fn("abs", a)), div(fn("sin", div(v, TWO)), div(v, TWO)))
                "tri" -> mul(shift, div(ONE, fn("abs", a)), pow(div(fn("sin", div(v, TWO)), div(v, TWO)), 2))
                "sgn" -> mul(shift, div(TWO, mul(I, w)), if (negative(a)) MINUS_ONE else ONE)
                else -> fail("Fourier")
            }
        }
        if (one is Pow && one.base == E) {
            // e^(−a|kt + b|): 2a/(a² + ω²) scaled and shifted.
            val ex = one.exp
            val abs = (if (ex is Mul) ex.factors else listOf(ex)).firstOrNull { it is Fn && it.name == "abs" } as Fn?
            if (abs != null) {
                val coef = s(div(ex, abs))
                val (k, b) = linear(abs.args[0], t) ?: fail("Fourier")
                if (!coef.freeOf(t) || !negative(coef)) fail("Fourier")
                val a = s(neg(mul(coef, fn("abs", k))))
                val t0 = s(neg(div(b, k)))
                return mul(pow(E, neg(mul(iw, t0))), div(mul(TWO, a), mul(fn("abs", k), add(pow(div(a, fn("abs", k)), 2), pow(w, 2)))).let { s(it) })
            }
            // e^(−at² + bt + c): √(π/a) e^c e^((b − iω)²/4a).
            val c = Algebra.coefficients(Algebra.expand(ex), t)
            if (c != null && c.size == 3 && c.all { it.freeOf(t) } && negative(c[2])) {
                val a = neg(c[2])
                return mul(sqrt(div(PI, a)), pow(E, c[0]), pow(E, div(pow(sub(c[1], iw), 2), mul(num(4), a))))
            }
        }
        // 1/(t² + a²): (π/a) e^(−a|ω|).
        if (one is Pow && one.exp == MINUS_ONE) {
            val c = Algebra.coefficients(Algebra.expand(one.base), t)
            if (c != null && c.size == 3 && c[1] == ZERO && c.all { it.freeOf(t) } && positive(c[0]) && positive(c[2])) {
                val a = sqrt(div(c[0], c[2]))
                return div(mul(div(PI, a), pow(E, neg(mul(a, fn("abs", w))))), c[2])
            }
            // 1/(a + ikt): from the step table by duality.
            val l = linear(one.base, t)
            if (l != null) {
                val (k, a0) = l
                val r = s(div(k, I))
                if (r.freeOf(I) && r != ZERO && positive(div(a0, r))) {
                    // 1/(a + i r t) = (1/r)/(a/r + i t) → (2π/r) e^(aω/r) H(−ω), a/r > 0.
                    val aa = div(a0, r)
                    return mul(div(mul(TWO, PI), fn("abs", r)), pow(E, mul(aa, w)), Fn("heaviside", listOf(neg(w))))
                }
            }
        }
        fail("Fourier")
    }

    /** ℱ⁻¹{F(ω)}(t) = (1/2π) ∫ F(ω) e^(iωt) dω, by duality: ℱ⁻¹{F}(t) = ℱ{F}(−t)/2π. */
    fun inverseFourier(big: Expr, w: Sym, t: Sym): Expr {
        val v = Sym("ν·")
        val forward = runCatching { fourier(big, w, v) }.getOrNull() ?: fail("inverse Fourier")
        return s(div(forward.subst(v, neg(t)), mul(TWO, PI)))
    }
}
