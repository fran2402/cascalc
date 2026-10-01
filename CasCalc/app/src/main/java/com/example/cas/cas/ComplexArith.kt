package com.example.cas.cas

import com.example.cas.math.Rational

/**
 * Exact complex arithmetic on constants: anything without variables that has i in it is put in
 * the form a + bi, with a and b exact. (1 + 2i)(3 − i) = 5 + 5i, 1/(1 + i) = 1/2 − i/2,
 * (1 + i)⁸ = 16, e^(iπ) = −1, i^i = e^(−π/2). Parts it can't work out exactly (e^i, 2^i) are left
 * as they are, and so is everything around them that depends on them.
 */
object ComplexArith {
    /** The largest integer power multiplied out (by squaring). */
    private const val MAX_POWER = 256

    /** [e] with every constant part containing i in the form a + bi, where that's exact. */
    fun normalize(e: Expr): Expr = when {
        e is Eq -> Eq(normalize(e.lhs), normalize(e.rhs))
        e is Seq -> Seq(e.items.map { normalize(it) }, e.joiner)
        e is Rel -> Rel(e.parts.map { normalize(it) }, e.ops)
        e is Mat -> Mat(e.rows, e.cols, e.cells.map { normalize(it) })
        !e.contains { it == I } || e.contains { it == Sym("∞") } -> e
        e.isConstant && !e.contains { it is Flt } -> runCatching { split(e) }.getOrNull()?.let { (re, im) -> join(re, im) } ?: rebuild(e)
        else -> rebuild(e)
    }

    /** The same expression with its parts normalized (for an expression that isn't one constant). */
    private fun rebuild(e: Expr): Expr = when (e) {
        is Add -> Simplify.sum(e.terms.map { normalize(it) })
        is Mul -> Simplify.product(e.factors.map { normalize(it) })
        is Pow -> Simplify.power(normalize(e.base), normalize(e.exp))
        is Fn -> if (e.name in HELD) e else Simplify.function(e.name, e.args.map { normalize(it) })
        else -> e
    }

    /** Held operations (∫, Σ, ∮…) keep their arguments as typed. */
    private val HELD = setOf("integral", "sum", "product", "contour", "lim", "diff")

    private fun join(re: Expr, im: Expr): Expr = Simplify.sum(listOf(re, Simplify.product(listOf(im, I))))

    private fun tidy(x: Expr): Expr = Algebra.expand(x)

    /** (re, im) of a constant, both real and exact, or null. */
    fun split(e: Expr): Pair<Expr, Expr>? {
        if (e == I) return ZERO to ONE
        if (!e.contains { it == I }) return e to ZERO
        return when (e) {
            is Add -> {
                val parts = e.terms.map { split(it) ?: return null }
                tidy(Simplify.sum(parts.map { it.first })) to tidy(Simplify.sum(parts.map { it.second }))
            }
            is Mul -> e.factors.fold(Pair<Expr, Expr>(ONE, ZERO)) { acc, f -> times(acc, split(f) ?: return null) }
            is Pow -> power(e.base, e.exp)
            else -> null
        }
    }

    private fun times(a: Pair<Expr, Expr>, b: Pair<Expr, Expr>): Pair<Expr, Expr> {
        val (p, q) = a; val (r, s) = b
        val re = Simplify.sum(listOf(Simplify.product(listOf(p, r)), Simplify.product(listOf(MINUS_ONE, q, s))))
        val im = Simplify.sum(listOf(Simplify.product(listOf(p, s)), Simplify.product(listOf(q, r))))
        return tidy(re) to tidy(im)
    }

    /** 1/(a + bi) = (a − bi)/(a² + b²). */
    private fun reciprocal(z: Pair<Expr, Expr>): Pair<Expr, Expr>? {
        val (a, b) = z
        val n = tidy(Simplify.sum(listOf(Simplify.power(a, TWO), Simplify.power(b, TWO))))
        if (n == ZERO) throw MathError("Can't divide by 0")
        val inv = Simplify.power(n, MINUS_ONE)
        return tidy(Simplify.product(listOf(a, inv))) to tidy(Simplify.product(listOf(MINUS_ONE, b, inv)))
    }

    private fun power(base: Expr, exp: Expr): Pair<Expr, Expr>? {
        // An integer power: by squaring, and a reciprocal for a negative one.
        if (exp is Num && exp.q.isInteger && exp.q.num.abs().toInt() <= MAX_POWER && exp.q.num.bitLength() < 31) {
            var z = split(base) ?: return null
            var n = exp.q.num.toInt()
            if (n < 0) { z = reciprocal(z) ?: return null; n = -n }
            var out: Pair<Expr, Expr> = ONE to ZERO
            while (n > 0) {
                if (n and 1 == 1) out = times(out, z)
                n = n shr 1
                if (n > 0) z = times(z, z)
            }
            return out
        }
        // e^(x + iy) = eˣ (cos y + i sin y), when cos y and sin y are exact.
        if (base == E) {
            val (x, y) = split(exp) ?: return null
            return exp(x, y)
        }
        // z^w = e^(w ln z), when ln z is exact (i^i = e^(−π/2)).
        val ln = Simplify.function("ln", listOf(base))
        if (ln.contains { it is Fn && it.name == "ln" && it.args[0].contains { a -> a == I } }) return null
        val (x, y) = split(Simplify.product(listOf(exp, ln))) ?: return null
        return exp(x, y)
    }

    private fun exp(x: Expr, y: Expr): Pair<Expr, Expr>? {
        val m = Simplify.power(E, x)
        if (y == ZERO) return m to ZERO
        val c = Simplify.function("cos", listOf(y))
        val s = Simplify.function("sin", listOf(y))
        // Only exact values: e^i stays as it is rather than turning into cos 1 + i sin 1.
        if (c.contains { it is Fn && (it.name == "cos" || it.name == "sin") } || s.contains { it is Fn && (it.name == "cos" || it.name == "sin") }) return null
        return tidy(Simplify.product(listOf(m, c))) to tidy(Simplify.product(listOf(m, s)))
    }

    private val TWO = Num(Rational.of(2L))
}
