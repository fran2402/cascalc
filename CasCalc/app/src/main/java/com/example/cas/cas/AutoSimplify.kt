package com.example.cas.cas

/**
 * Algebraic simplification without asking: after a calculation with letters in it, the answer is
 * put in its simplest form. Each round tries multiplying out (and combining like terms), factoring
 * a fraction and cancelling, and the identities sin²u + cos²u = 1 and cosh²u − sinh²u = 1; a
 * rewrite is kept only when it makes the expression strictly smaller, so nothing gets longer:
 * (x² − 1)/(x − 1) = x + 1, (x + 1)² − x² = 2x + 1, sin²x + cos²x = 1, (2x + 2)/4 = (x + 1)/2.
 * The moves are kept too, for the steps.
 */
object AutoSimplify {
    /** One rewrite: what it was, a sentence about it, and the expression after it. */
    class Move(val rule: String, val text: String, val result: Expr)

    /** Expressions longer than this (printed) are left as they are: simplifying them would be slow. */
    private const val MAX_LENGTH = 400

    fun simplify(e: Expr): Expr = moves(e).lastOrNull()?.result ?: e

    /** The rewrites that lead from [e] to its simplest form (empty if it's already there). */
    fun moves(e: Expr): List<Move> {
        if (e is Mat || e is Eq || e is Seq || e is Rel || e.freeVars().isEmpty() || e.contains { it is Flt }) return emptyList()
        if (e.contains { it is Fn && it.name in HELD }) return emptyList()
        if (Printer.plain(e).length > MAX_LENGTH) return emptyList()
        val out = ArrayList<Move>()
        var current = e
        repeat(4) {
            val best = candidates(current)
                .filter { size(it.result) < size(current) && agrees(it.result, current) }
                .minByOrNull { size(it.result) } ?: return out
            out += best
            current = best.result
        }
        return out
    }

    /** Held operations whose arguments stay as typed. */
    private val HELD = setOf("integral", "sum", "product", "contour", "lim", "diff")

    private fun candidates(e: Expr): List<Move> = listOfNotNull(
        runCatching { Move("Expand", "Multiply out and combine like terms.", Algebra.expand(e)) }.getOrNull(),
        runCatching { cancel(e) }.getOrNull(),
        runCatching { pythagoras(e) }.getOrNull(),
        runCatching { combineLogs(e) }.getOrNull(),
        runCatching { doubleAngle(e) }.getOrNull(),
        runCatching { powerReduction(e) }.getOrNull(),
        runCatching { tangent(e) }.getOrNull(),
        runCatching { hyperbolic(e) }.getOrNull(),
        runCatching { expandLogs(e) }.getOrNull(),
        runCatching { angleSum(e) }.getOrNull(),
        runCatching { expandAngles(e) }.getOrNull(),
        runCatching { commonFactor(e) }.getOrNull(),
    )

    /** ln(uv) = ln u + ln v and ln(uᵃ) = a ln u, then like terms: ln(x²) − 2 ln x = 0. */
    private fun expandLogs(e: Expr): Move? {
        var changed = false
        fun split(z: Expr): Expr = when {
            z is Fn && z.name == "ln" && z.args[0] is Mul -> { changed = true; add((z.args[0] as Mul).factors.map { split(fn("ln", it)) }) }
            z is Fn && z.name == "ln" && z.args[0] is Pow && (z.args[0] as Pow).exp.freeVars().isEmpty() -> {
                changed = true; mul((z.args[0] as Pow).exp, split(fn("ln", (z.args[0] as Pow).base)))
            }
            z is Add -> add(z.terms.map { split(it) })
            z is Mul -> mul(z.factors.map { split(it) })
            else -> z
        }
        val r = Algebra.expand(split(e))
        return if (changed) Move("Expand logarithms", "ln(uv) = ln u + ln v and ln(uᵃ) = a ln u, then collect.", r) else null
    }

    /** sin A cos B ± cos A sin B = sin(A ± B), cos A cos B ∓ sin A sin B = cos(A ± B). */
    private fun angleSum(e: Expr): Move? {
        if (e !is Add) return null
        // Each term as coefficient · f(A) · g(B), f and g sin or cos.
        class T(val c: Expr, val f: String, val a: Expr, val g: String, val b: Expr)
        fun read(t: Expr): T? {
            val (c, m) = Simplify.splitCoefficient(t)
            val fs = (m as? Mul)?.factors ?: return null
            if (fs.size != 2 || fs.any { it !is Fn || (it.name != "sin" && it.name != "cos") }) return null
            val (p, q) = fs.map { it as Fn }
            return T(c, p.name, p.args[0], q.name, q.args[0])
        }
        val terms = e.terms
        for (i in terms.indices) for (j in terms.indices) {
            if (i >= j) continue
            val s = read(terms[i]) ?: continue; val t = read(terms[j]) ?: continue
            // Put each as (name of A's function, A, B's function, B) with A, B matched across both.
            fun pick(x: T, a: Expr): Pair<String, String>? = when (a) { x.a -> x.f to x.g; x.b -> x.g to x.f; else -> null }
            val a = s.a; val b = s.b
            if (a == b) continue
            val p1 = pick(s, a) ?: continue; val p2 = pick(t, a) ?: continue
            if (t.a != b && t.b != b) continue
            val c1 = s.c; val c2 = t.c
            val same = Algebra.simplify(sub(c1, c2)) == ZERO; val opposite = Algebra.simplify(add(c1, c2)) == ZERO
            val pair = setOf(p1, p2)
            val r: Expr = when {
                pair == setOf("sin" to "cos", "cos" to "sin") && same -> mul(c1, fn("sin", add(a, b)))
                pair == setOf("sin" to "cos", "cos" to "sin") && opposite -> mul(c1, fn("sin", if (p1 == ("sin" to "cos")) sub(a, b) else sub(b, a)))
                pair == setOf("cos" to "cos", "sin" to "sin") && opposite -> mul(if (p1 == ("cos" to "cos")) c1 else c2, fn("cos", add(a, b)))
                pair == setOf("cos" to "cos", "sin" to "sin") && same -> mul(c1, fn("cos", sub(a, b)))
                else -> continue
            }
            val rest = terms.filterIndexed { k, _ -> k != i && k != j }
            return Move("Angle sum", "sin(A ± B) = sin A cos B ± cos A sin B, cos(A ± B) = cos A cos B ∓ sin A sin B.", add(rest + r))
        }
        return null
    }

    /** sin(A ± B), cos(A ± B), sin 2u and cos 2u written out, then like terms (when that lets things cancel). */
    private fun expandAngles(e: Expr): Move? {
        var changed = false
        fun walk(z: Expr): Expr = when {
            z is Fn && (z.name == "sin" || z.name == "cos") && z.args[0] is Add && (z.args[0] as Add).terms.size == 2 -> {
                changed = true
                val (a, b) = (z.args[0] as Add).terms
                if (z.name == "sin") add(mul(fn("sin", a), fn("cos", b)), mul(fn("cos", a), fn("sin", b)))
                else sub(mul(fn("cos", a), fn("cos", b)), mul(fn("sin", a), fn("sin", b)))
            }
            z is Fn && (z.name == "sin" || z.name == "cos") && z.args[0] is Mul && (z.args[0] as Mul).factors.first() == TWO -> {
                changed = true
                val u = mul((z.args[0] as Mul).factors.drop(1))
                if (z.name == "sin") mul(TWO, fn("sin", u), fn("cos", u)) else sub(pow(fn("cos", u), TWO), pow(fn("sin", u), TWO))
            }
            z is Add -> add(z.terms.map { walk(it) })
            z is Mul -> mul(z.factors.map { walk(it) })
            z is Pow -> pow(walk(z.base), z.exp)
            else -> z
        }
        val r = Algebra.expand(walk(e))
        return if (changed) Move("Angle formulas", "Write out sin(A ± B), cos(A ± B), sin 2u = 2 sin u cos u and cos 2u = cos²u − sin²u, then collect.", r) else null
    }

    /** The numbers and powers every term shares, taken out: x²y + xy² = xy(x + y). */
    private fun commonFactor(e: Expr): Move? {
        if (e !is Add) return null
        val r = Algebra.commonFactor(e)
        return if (Printer.plain(r) == Printer.plain(e)) null else Move("Common factor", "Take out what every term shares.", r)
    }

    // ---- Logarithms -----------------------------------------------------------------------------

    /** a ln u + b ln v + … = ln(uᵃ vᵇ ⋯) (for positive u, v: the rules for logarithms). */
    private fun combineLogs(e: Expr): Move? {
        if (e !is Add) return null
        val logs = ArrayList<Expr>(); val rest = ArrayList<Expr>()
        for (t in e.terms) {
            val (c, m) = Simplify.splitCoefficient(t)
            if (m is Fn && m.name == "ln" && c is Num) logs += pow(m.args[0], c) else rest += t
        }
        if (logs.size < 2) return null
        val inside = Algebra.simplify(mul(logs))
        val text = "ln u + ln v = ln(uv), ln u − ln v = ln(u/v), a ln u = ln(uᵃ)."
        return Move("Combine logarithms", text, add(rest + fn("ln", inside)))
    }

    // ---- Double angles ------------------------------------------------------------------------------

    /** sin u · cos u = sin(2u)/2, in every product. */
    private fun doubleAngle(e: Expr): Move? {
        var changed = false
        fun walk(z: Expr): Expr = when (z) {
            is Mul -> {
                val fs = z.factors.map { walk(it) }.toMutableList()
                val s = fs.indexOfFirst { it is Fn && it.name == "sin" }
                val c = if (s >= 0) fs.indexOfFirst { it is Fn && it.name == "cos" && it.args == (fs[s] as Fn).args } else -1
                if (s >= 0 && c >= 0) {
                    val u = (fs[s] as Fn).args[0]
                    val rest = fs.filterIndexed { k, _ -> k != s && k != c }
                    changed = true
                    mul(rest + listOf(HALF, fn("sin", mul(TWO, u))))
                } else mul(fs)
            }
            is Add -> add(z.terms.map { walk(it) })
            is Pow -> pow(walk(z.base), z.exp)
            else -> z
        }
        val r = walk(e)
        return if (changed) Move("Double angle", "2 sin u cos u = sin 2u.", r) else null
    }

    /** cos²u = (1 + cos 2u)/2 and sin²u = (1 − cos 2u)/2, then multiplied out: cos²u − sin²u = cos 2u, 1 − 2 sin²u = cos 2u. */
    private fun powerReduction(e: Expr): Move? {
        if (e !is Add) return null
        var changed = false
        fun walk(z: Expr): Expr = when {
            z is Pow && z.exp == TWO && z.base is Fn && (z.base as Fn).name in setOf("sin", "cos") -> {
                changed = true
                val u = (z.base as Fn).args[0]
                val c2 = fn("cos", mul(TWO, u))
                if ((z.base as Fn).name == "cos") mul(HALF, add(ONE, c2)) else mul(HALF, sub(ONE, c2))
            }
            z is Add -> add(z.terms.map { walk(it) })
            z is Mul -> mul(z.factors.map { walk(it) })
            else -> z
        }
        val r = Algebra.expand(walk(e))
        return if (changed) Move("Double angle", "cos 2u = cos²u − sin²u = 1 − 2 sin²u = 2 cos²u − 1.", r) else null
    }

    // ---- Quotients and exponentials -----------------------------------------------------------------

    /** sin u / cos u = tan u (to any power). */
    private fun tangent(e: Expr): Move? {
        var changed = false
        fun walk(z: Expr): Expr = when (z) {
            is Mul -> {
                val fs = z.factors.map { walk(it) }
                fun power(f: Expr, name: String): Pair<Expr, Expr>? = when {
                    f is Fn && f.name == name -> f.args[0] to ONE
                    f is Pow && f.base is Fn && (f.base as Fn).name == name && f.exp is Num -> (f.base as Fn).args[0] to f.exp
                    else -> null
                }
                var out: Expr? = null
                for (a in fs) {
                    val (u, k) = power(a, "sin") ?: continue
                    val b = fs.firstOrNull { power(it, "cos")?.let { (v, j) -> v == u && Algebra.simplify(add(j, k)) == ZERO } == true } ?: continue
                    changed = true
                    out = mul(fs.filter { it !== a && it !== b } + pow(fn("tan", u), k))
                    break
                }
                out ?: mul(fs)
            }
            is Add -> add(z.terms.map { walk(it) })
            else -> z
        }
        val r = walk(e)
        return if (changed) Move("Tangent", "sin u / cos u = tan u.", r) else null
    }

    /** c(eᵘ + e⁻ᵘ) = 2c cosh u and c(eᵘ − e⁻ᵘ) = 2c sinh u. */
    private fun hyperbolic(e0: Expr): Move? {
        val e = if (e0 is Add) e0 else Algebra.expand(e0)
        if (e !is Add) return null
        val terms = e.terms.toMutableList()
        for (i in terms.indices) {
            val (c1, m1) = Simplify.splitCoefficient(terms[i])
            if (m1 !is Pow || m1.base != E) continue
            for (j in terms.indices) {
                if (j == i) continue
                val (c2, m2) = Simplify.splitCoefficient(terms[j])
                if (m2 !is Pow || m2.base != E || Algebra.simplify(add(m1.exp, m2.exp)) != ZERO) continue
                val u = m1.exp
                val sumC = Algebra.simplify(sub(c1, c2))
                val name = when {
                    Algebra.simplify(sub(c1, c2)) == ZERO -> "cosh"
                    Algebra.simplify(add(c1, c2)) == ZERO -> "sinh"
                    else -> continue
                }
                if (sumC == ZERO && name == "sinh") continue
                val replaced = mul(TWO, c1, fn(name, u))
                val rest = terms.filterIndexed { k, _ -> k != i && k != j }
                return Move(if (name == "cosh") "Hyperbolic cosine" else "Hyperbolic sine", if (name == "cosh") "eᵘ + e⁻ᵘ = 2 cosh u." else "eᵘ − e⁻ᵘ = 2 sinh u.", add(rest + replaced))
            }
        }
        return null
    }

    /** One fraction, then common factors of the top and bottom cancelled. */
    private fun cancel(e: Expr): Move? {
        val (n, d) = Algebra.together(e)
        if (d == ONE) return null
        val result = Algebra.cancel(n, d)
        val vars = n.freeVars() + d.freeVars()
        // Name the factor that cancels, for the steps.
        val common = if (vars.size == 1) {
            val x = Sym(vars.first())
            val pn = Algebra.qpoly(n, x); val pd = Algebra.qpoly(d, x)
            if (pn != null && pd != null) QPoly.gcd(pn, pd).takeIf { it.degree > 0 }?.monic()?.toExpr(x) else null
        } else null
        val fractions = (e as? Add)?.terms?.count { t -> Algebra.together(t).second != ONE } ?: 0
        return when {
            common != null -> Move("Factor and cancel", "${Printer.plain(common).replace("-", "−")} divides the top and the bottom: cancel it.", result)
            fractions > 1 -> Move("Common denominator", "Put the fractions over one denominator.", result)
            else -> Move("Cancel", "Cancel the common factors of the top and the bottom.", result)
        }
    }

    /** sin²u + cos²u = 1 (and cosh²u − sinh²u = 1): write sin² as 1 − cos², then multiply out. */
    private fun pythagoras(e: Expr): Move? {
        val squares = HashMap<String, MutableSet<Expr>>()
        fun walk(z: Expr) {
            if (z is Pow && z.exp is Num && (z.exp as Num).q.isInteger && (z.exp as Num).q.num.toInt() >= 2 && z.base is Fn) {
                val f = z.base as Fn
                if (f.name in setOf("sin", "cos", "sinh", "cosh") && f.args.size == 1) squares.getOrPut(f.name) { mutableSetOf() } += f.args[0]
            }
            z.children.forEach { walk(it) }
        }
        walk(e)
        val trig = squares["sin"].orEmpty().intersect(squares["cos"].orEmpty())
        val hyper = squares["sinh"].orEmpty().intersect(squares["cosh"].orEmpty())
        if (trig.isEmpty() && hyper.isEmpty()) return null
        var r = e
        for (u in trig) r = replaceSquares(r, "sin", u, sub(ONE, pow(fn("cos", u), TWO)))
        for (u in hyper) r = replaceSquares(r, "sinh", u, sub(pow(fn("cosh", u), TWO), ONE))
        val text = if (trig.isNotEmpty()) "sin²u + cos²u = 1" else "cosh²u − sinh²u = 1"
        return Move("Pythagorean identity", "$text: write each ${if (trig.isNotEmpty()) "sin²" else "sinh²"} with the other function, then simplify.", Algebra.expand(r))
    }

    /** [name](u)ⁿ → [square]^(n/2)·[name](u)^(n mod 2), everywhere in [e]. */
    private fun replaceSquares(e: Expr, name: String, u: Expr, square: Expr): Expr {
        if (e is Pow && e.base is Fn && (e.base as Fn).name == name && (e.base as Fn).args == listOf(u) && e.exp is Num && (e.exp as Num).q.isInteger) {
            val n = (e.exp as Num).q.num.toInt()
            if (n >= 2) return mul(pow(square, Num((n / 2).toLong())), pow(e.base, Num((n % 2).toLong())))
        }
        return when (e) {
            is Add -> add(e.terms.map { replaceSquares(it, name, u, square) })
            is Mul -> mul(e.factors.map { replaceSquares(it, name, u, square) })
            is Pow -> pow(replaceSquares(e.base, name, u, square), replaceSquares(e.exp, name, u, square))
            else -> e
        }
    }

    /** How big an expression is: its nodes, counted as they're drawn. */
    fun size(e: Expr): Int = when (e) {
        is Num -> if (e.q.isInteger) 1 else 3
        is Mul -> e.factors.sumOf { size(it) } + (if (e.factors.firstOrNull() == MINUS_ONE) 0 else e.factors.size - 1)
        is Add -> e.terms.sumOf { size(it) } + e.terms.size - 1
        // 1/u is a fraction bar under u.
        is Pow -> if (e.exp == MINUS_ONE) size(e.base) + 1 else size(e.base) + size(e.exp) + 1
        is Fn -> 1 + e.args.sumOf { size(it) }
        else -> 1
    }

    /** The same function, checked at a few points (cancelling only removes holes). */
    private fun agrees(a: Expr, b: Expr): Boolean {
        val vars = (a.freeVars() + b.freeVars()).sorted()
        var compared = 0
        for (k in 0 until 6) {
            val env = vars.mapIndexed { j, v -> v to (0.37 + 0.71 * k + 0.13 * j) }.toMap()
            val u = runCatching { Numeric.eval(a, env) }.getOrNull() ?: continue
            val v = runCatching { Numeric.eval(b, env) }.getOrNull() ?: continue
            if (!u.re.isFinite() || !v.re.isFinite()) continue
            if ((u - v).abs() > 1e-8 * maxOf(1.0, v.abs())) return false
            compared++
        }
        return compared >= 2
    }
}
