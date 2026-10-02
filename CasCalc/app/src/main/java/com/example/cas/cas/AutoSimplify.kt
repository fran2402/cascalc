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
    )

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
        is Pow -> size(e.base) + size(e.exp) + 1
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
