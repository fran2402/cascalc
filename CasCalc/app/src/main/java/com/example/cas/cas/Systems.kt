package com.example.cas.cas

/**
 * Systems of equations and inequalities in one variable.
 */
object Systems {

    // ---- Systems ------------------------------------------------------------------------

    /**
     * Solves several equations for several unknowns by elimination: pick an
     * equation that's linear in some unknown (or has only one unknown left),
     * solve it for that unknown, substitute into the rest, and branch over
     * multiple solutions. Linear systems with infinitely many solutions keep
     * the free unknowns as parameters (x = 1 − z).
     */
    fun solve(equations: List<Expr>, vars: List<Sym>): Expr {
        val zeroForms = equations.map { e -> if (e is Eq) sub(e.lhs, e.rhs) else e }
        val sets = eliminate(zeroForms, vars, emptyMap(), 0)
            .map { resolve(it) }
            .distinctBy { m -> vars.joinToString("|") { v -> m[v]?.let { Printer.key(it) } ?: v.name } }
        if (sets.isEmpty()) throw MathError("No solution")
        val shown = sets.map { m ->
            Seq(vars.filter { it in m }.map { v -> Eq(v, m.getValue(v)) })
        }
        return if (shown.size == 1) shown[0] else Seq(shown, "or")
    }

    private fun eliminate(eqs0: List<Expr>, vars: List<Sym>, assigned: Map<Sym, Expr>, depth: Int): List<Map<Sym, Expr>> {
        if (depth > 12) throw MathError("That system is too complicated")
        val eqs = eqs0.map { Algebra.simplify(Algebra.expand(it.subst(assigned.mapKeys { k -> k.key.name }))) }
            .filterNot { LinearAlgebra.isZero(it) }
        // Something like 0 = 5 left over: inconsistent.
        if (eqs.any { e -> vars.none { !e.freeOf(it) } && e.isConstant }) return emptyList()
        if (eqs.isEmpty()) return listOf(assigned)
        val open = vars.filter { it !in assigned }

        // 1. An equation linear in some unknown with a constant, non-zero coefficient.
        // 2. Then one with a single unknown left (solve it outright, maybe several roots).
        // 3. Then linear with a symbolic coefficient.
        for (pass in 0..2) {
            for ((k, e) in eqs.withIndex()) {
                val inEq = open.filter { !e.freeOf(it) }
                if (pass == 1) {
                    if (inEq.size != 1) continue
                    val v = inEq[0]
                    val roots = runCatching { values(Algebra.solve(Eq(e, ZERO), v)) }.getOrElse { return emptyList() }
                    return roots.flatMap { r -> eliminate(eqs.filterIndexed { j, _ -> j != k }, vars, assigned + (v to r), depth + 1) }
                }
                for (v in inEq) {
                    val cs = Algebra.coefficients(Algebra.expand(e), v) ?: continue
                    if (cs.size != 2) continue
                    val c1 = cs[1]
                    val okCoefficient = if (pass == 0) c1.isConstant && !LinearAlgebra.isZero(c1) else !LinearAlgebra.isZero(c1)
                    if (!okCoefficient) continue
                    val value = Algebra.simplify(neg(div(cs[0], c1)))
                    return eliminate(eqs.filterIndexed { j, _ -> j != k }, vars, assigned + (v to value), depth + 1)
                }
            }
        }
        throw MathError("Can't solve this system exactly")
    }

    private fun values(solutions: Expr): List<Expr> = when (solutions) {
        is Eq -> listOf(solutions.rhs)
        is Seq -> solutions.items.map { (it as Eq).rhs }
        else -> emptyList()
    }

    /** Earlier unknowns may be written in terms of later ones; substitute until none are left. */
    private fun resolve(m: Map<Sym, Expr>): Map<Sym, Expr> {
        var cur = m
        repeat(m.size + 1) {
            cur = cur.mapValues { (_, v) -> Algebra.simplify(v.subst(cur.filterKeys { k -> !v.freeOf(k) }.mapKeys { it.key.name })) }
        }
        return cur
    }

    // ---- Inequalities ------------------------------------------------------------------------

    /**
     * Solves an inequality (or a chain like 0 < f(x) ≤ 5, as two conditions)
     * in one variable: find where each side of f = 0 changes sign (zeros and
     * poles), test a point in each interval, and join the intervals that work.
     */
    fun solveInequality(rel: Rel, x: Sym): Expr {
        var sets: List<Interval> = listOf(Interval(null, false, null, false))
        for (k in rel.ops.indices) {
            val one = solveOne(rel.parts[k], rel.ops[k], rel.parts[k + 1], x)
            sets = intersect(sets, one)
        }
        return show(sets, x)
    }

    /** An interval between two endpoints (null = infinite), each open or closed. */
    private data class Interval(val lo: Expr?, val loClosed: Boolean, val hi: Expr?, val hiClosed: Boolean)

    private fun value(e: Expr?) = if (e == null) Double.NaN else Numeric.real(e)

    private fun solveOne(a: Expr, op: String, b: Expr, x: Sym): List<Interval> {
        val f = sub(a, b)
        val (n, d) = Algebra.together(f)
        fun realRoots(p: Expr): List<Expr> {
            if (p.freeOf(x)) return emptyList()
            val s = runCatching { Algebra.solve(Eq(p, ZERO), x) }.getOrNull() ?: return emptyList()
            return values(s).filter { it.isConstant && runCatching { Numeric.eval(it).isReal }.getOrDefault(false) }
        }
        val zeros = realRoots(n)
        val poles = realRoots(d)
        val critical = (zeros + poles).distinctBy { Math.round(value(it) * 1e9) }.sortedBy { value(it) }
        val test = { t: Double ->
            val v = runCatching { Numeric.real(f, mapOf(x.name to t)) }.getOrNull()
            v != null && v.isFinite() && when (op) {
                "<" -> v < 0; ">" -> v > 0; "≤" -> v <= 0; else -> v >= 0
            }
        }
        val strict = op == "<" || op == ">"
        val out = ArrayList<Interval>()
        val pts = critical.map { value(it) }
        for (i in 0..critical.size) {
            val lo = critical.getOrNull(i - 1)
            val hi = critical.getOrNull(i)
            val mid = when {
                lo == null && hi == null -> 0.0
                lo == null -> pts[i] - 1.0 - kotlin.math.abs(pts[i])
                hi == null -> pts[i - 1] + 1.0 + kotlin.math.abs(pts[i - 1])
                else -> (pts[i - 1] + pts[i]) / 2
            }
            if (!test(mid)) continue
            // Endpoints are included for ≤ / ≥ when they're zeros, never at poles.
            val loClosed = lo != null && !strict && poles.none { it == lo }
            val hiClosed = hi != null && !strict && poles.none { it == hi }
            out += Interval(lo, loClosed, hi, hiClosed)
        }
        // Isolated points that satisfy ≤ / ≥ (like x² ≤ 0 at x = 0).
        if (!strict) for (z in zeros) if (out.none { contains(it, value(z)) }) out += Interval(z, true, z, true)
        return merge(out)
    }

    private fun contains(iv: Interval, t: Double): Boolean {
        val lo = iv.lo?.let { value(it) } ?: Double.NEGATIVE_INFINITY
        val hi = iv.hi?.let { value(it) } ?: Double.POSITIVE_INFINITY
        return (t > lo || (t == lo && iv.loClosed)) && (t < hi || (t == hi && iv.hiClosed))
    }

    /** Joins intervals that touch at a closed endpoint: (−∞, 1] ∪ [1, 2) = (−∞, 2). */
    private fun merge(list: List<Interval>): List<Interval> {
        val sorted = list.sortedBy { it.lo?.let { v -> value(v) } ?: Double.NEGATIVE_INFINITY }
        val out = ArrayList<Interval>()
        for (iv in sorted) {
            val last = out.lastOrNull()
            if (last != null && last.hi != null && iv.lo != null && last.hi == iv.lo && (last.hiClosed || iv.loClosed)) {
                out[out.size - 1] = Interval(last.lo, last.loClosed, iv.hi, iv.hiClosed)
            } else out += iv
        }
        return out
    }

    private fun intersect(a: List<Interval>, b: List<Interval>): List<Interval> {
        val out = ArrayList<Interval>()
        for (p in a) for (q in b) {
            val (lo, loClosed) = maxLo(p, q)
            val (hi, hiClosed) = minHi(p, q)
            if (lo != null && hi != null) {
                val l = value(lo); val h = value(hi)
                if (l > h || (l == h && !(loClosed && hiClosed))) continue
            }
            out += Interval(lo, loClosed, hi, hiClosed)
        }
        return merge(out)
    }

    private fun maxLo(p: Interval, q: Interval): Pair<Expr?, Boolean> = when {
        p.lo == null -> q.lo to q.loClosed
        q.lo == null -> p.lo to p.loClosed
        value(p.lo) > value(q.lo) -> p.lo to p.loClosed
        value(p.lo) < value(q.lo) -> q.lo to q.loClosed
        else -> p.lo to (p.loClosed && q.loClosed)
    }

    private fun minHi(p: Interval, q: Interval): Pair<Expr?, Boolean> = when {
        p.hi == null -> q.hi to q.hiClosed
        q.hi == null -> p.hi to p.hiClosed
        value(p.hi) < value(q.hi) -> p.hi to p.hiClosed
        value(p.hi) > value(q.hi) -> q.hi to q.hiClosed
        else -> p.hi to (p.hiClosed && q.hiClosed)
    }

    /** −2 < x < 2, x ≤ −1 or x ≥ 3, x = 0, "every real x", or no solution. */
    private fun show(sets: List<Interval>, x: Sym): Expr {
        if (sets.isEmpty()) throw MathError("No solution")
        val parts = sets.map { iv ->
            when {
                iv.lo == null && iv.hi == null -> throw MathError("True for every real ${x.name}")
                iv.lo != null && iv.hi != null && iv.lo == iv.hi -> Eq(x, iv.lo)
                iv.lo == null -> Rel(listOf(x, iv.hi!!), listOf(if (iv.hiClosed) "≤" else "<"))
                iv.hi == null -> Rel(listOf(x, iv.lo), listOf(if (iv.loClosed) "≥" else ">"))
                else -> Rel(listOf(iv.lo, x, iv.hi), listOf(if (iv.loClosed) "≤" else "<", if (iv.hiClosed) "≤" else "<"))
            }
        }
        return if (parts.size == 1) parts[0] else Seq(parts, "or")
    }
}
