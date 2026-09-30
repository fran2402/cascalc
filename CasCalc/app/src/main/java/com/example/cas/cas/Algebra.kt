package com.example.cas.cas

import com.example.cas.math.Rational
import java.math.BigInteger

/** Univariate polynomial with rational coefficients, lowest degree first. */
class QPoly(coefficients: List<Rational>) {
    val c: List<Rational> = coefficients.dropLastWhile { it.signum == 0 }
    val degree get() = c.size - 1
    val isZero get() = c.isEmpty()
    val lead get() = c.last()
    operator fun get(i: Int) = c.getOrElse(i) { Rational.ZERO }

    operator fun plus(o: QPoly) = QPoly(List(maxOf(c.size, o.c.size)) { this[it] + o[it] })
    operator fun minus(o: QPoly) = QPoly(List(maxOf(c.size, o.c.size)) { this[it] - o[it] })
    operator fun times(o: QPoly): QPoly {
        if (isZero || o.isZero) return QPoly(emptyList())
        val r = MutableList(c.size + o.c.size - 1) { Rational.ZERO }
        for (i in c.indices) for (j in o.c.indices) r[i + j] = r[i + j] + c[i] * o.c[j]
        return QPoly(r)
    }
    fun scale(k: Rational) = QPoly(c.map { it * k })

    fun divMod(d: QPoly): Pair<QPoly, QPoly> {
        if (d.isZero) throw MathError("Can't divide by 0")
        var r = this
        val q = MutableList(maxOf(0, degree - d.degree + 1)) { Rational.ZERO }
        while (!r.isZero && r.degree >= d.degree) {
            val k = r.degree - d.degree
            val f = r.lead / d.lead
            q[k] = f
            r = r - QPoly(List(k) { Rational.ZERO } + d.c.map { it * f })
        }
        return QPoly(q) to r
    }

    fun monic() = if (isZero) this else scale(lead.reciprocal())

    fun eval(x: Rational): Rational = c.foldRight(Rational.ZERO) { a, acc -> acc * x + a }

    fun derivative() = QPoly(c.drop(1).mapIndexed { i, a -> a * Rational.of((i + 1).toLong()) })

    fun toExpr(x: Expr): Expr = add(c.mapIndexed { i, a -> mul(Num(a), pow(x, i.toLong())) })

    /** Content and primitive part: this = content · primitive, integer coefficients with gcd 1 and a positive leading one. */
    fun primitive(): Pair<Rational, QPoly> {
        var lcm = BigInteger.ONE
        for (a in c) lcm = lcm / lcm.gcd(a.den) * a.den
        val ints = c.map { (it * Rational.of(lcm)).num }
        var g = BigInteger.ZERO
        for (a in ints) g = g.gcd(a)
        if (g.signum() == 0) g = BigInteger.ONE
        if (ints.last().signum() < 0) g = -g
        val prim = QPoly(ints.map { Rational.of(it / g) })
        return Rational.of(g, lcm) to prim
    }

    companion object {
        fun gcd(a: QPoly, b: QPoly): QPoly {
            var x = a
            var y = b
            while (!y.isZero) {
                val r = x.divMod(y).second
                x = y
                y = r
            }
            return x.monic()
        }
    }
}

/**
 * Algebra on symbolic expressions: expand, combine over a common
 * denominator, cancel, factor and solve equations.
 */
object Algebra {
    private const val MAX_TERMS = 4000

    // ---- Expand -------------------------------------------------------------------

    fun expand(e: Expr): Expr = when (e) {
        is Add -> add(e.terms.map { expand(it) })
        is Mul -> e.factors.map { expand(it) }.fold(ONE as Expr) { acc, f -> distribute(acc, f) }
        is Pow -> {
            val b = expand(e.base)
            val n = (e.exp as? Num)?.q?.takeIf { it.isInteger }?.num?.toInt()
            when {
                b is Add && n != null && n in 2..40 -> (1 until n).fold(b as Expr) { acc, _ -> distribute(acc, b) }
                b is Add && n != null && n in -40..-2 -> pow((1 until -n).fold(b as Expr) { acc, _ -> distribute(acc, b) }, MINUS_ONE)
                else -> pow(b, expand(e.exp))
            }
        }
        is Fn -> Simplify.function(e.name, e.args.map { expand(it) })
        is Mat -> Mat(e.rows, e.cols, e.cells.map { expand(it) })
        is Eq -> Eq(expand(e.lhs), expand(e.rhs))
        is Seq -> Seq(e.items.map { expand(it) }, e.joiner)
        is Rel -> Rel(e.parts.map { expand(it) }, e.ops)
        else -> e
    }

    private fun distribute(a: Expr, b: Expr): Expr {
        val ta = if (a is Add) a.terms else listOf(a)
        val tb = if (b is Add) b.terms else listOf(b)
        if (ta.size * tb.size > MAX_TERMS) throw MathError("That's too large to expand")
        if (ta.size == 1 && tb.size == 1) return mul(a, b)
        return add(ta.flatMap { x -> tb.map { y -> mul(x, y) } })
    }

    // ---- Polynomials ------------------------------------------------------------------

    /** Coefficients of e as a polynomial in x (lowest degree first), or null if it isn't one. */
    fun coefficients(e: Expr, x: Sym): List<Expr>? {
        val terms = if (e is Add) e.terms else listOf(e)
        val byDegree = HashMap<Int, MutableList<Expr>>()
        for (t in terms) {
            val (deg, coef) = monomial(t, x) ?: return null
            byDegree.getOrPut(deg) { mutableListOf() }.add(coef)
        }
        val top = byDegree.keys.maxOrNull() ?: return listOf(ZERO)
        return List(top + 1) { add(byDegree[it] ?: emptyList()) }
    }

    private fun monomial(t: Expr, x: Sym): Pair<Int, Expr>? {
        if (t.freeOf(x)) return 0 to t
        if (t == x) return 1 to ONE
        if (t is Pow && t.base == x) {
            val n = (t.exp as? Num)?.q?.takeIf { it.isInteger && it.signum > 0 }?.num?.toInt() ?: return null
            return n to ONE
        }
        if (t is Mul) {
            var deg = 0
            val coef = ArrayList<Expr>()
            for (f in t.factors) {
                if (f.freeOf(x)) { coef += f; continue }
                val (d, c) = monomial(f, x) ?: return null
                if (c != ONE) return null
                deg += d
            }
            return deg to mul(coef)
        }
        return null
    }

    fun qpoly(e: Expr, x: Sym): QPoly? {
        val cs = coefficients(expand(e), x) ?: return null
        return QPoly(cs.map { (it as? Num)?.q ?: return null })
    }

    // ---- Rational functions ---------------------------------------------------------------

    /** e = numerator / denominator, both expanded. */
    fun together(e: Expr): Pair<Expr, Expr> = when (e) {
        is Add -> e.terms.map { together(it) }.reduce { (n1, d1), (n2, d2) ->
            if (d1 == d2) expand(add(n1, n2)) to d1
            else expand(add(mul(n1, d2), mul(n2, d1))) to expand(mul(d1, d2))
        }
        is Mul -> e.factors.map { together(it) }.fold(ONE as Expr to ONE as Expr) { (n1, d1), (n2, d2) ->
            expand(mul(n1, n2)) to expand(mul(d1, d2))
        }
        is Pow -> {
            val k = (e.exp as? Num)?.q?.takeIf { it.isInteger }?.num?.toInt()
            if (k != null && k < 0) {
                val (bn, bd) = together(e.base)
                expand(pow(bd, (-k).toLong())) to expand(pow(bn, (-k).toLong()))
            } else if (k != null && k > 0) {
                val (bn, bd) = together(e.base)
                expand(pow(bn, k.toLong())) to expand(pow(bd, k.toLong()))
            } else e to ONE
        }
        is Num -> Num(Rational.of(e.q.num)) to Num(Rational.of(e.q.den))
        else -> e to ONE
    }

    /** Cancels common factors of a fraction: (x² − 1)/(x − 1) = x + 1. Keeps the input if nothing gets simpler. */
    fun simplify(e: Expr): Expr {
        if (e is Mat) return Mat(e.rows, e.cols, e.cells.map { simplify(it) })
        if (e is Eq) return Eq(simplify(e.lhs), simplify(e.rhs))
        if (e is Seq) return Seq(e.items.map { simplify(it) }, e.joiner)
        if (e is Rel) return Rel(e.parts.map { simplify(it) }, e.ops)
        val inner = if (e is Fn) Simplify.function(e.name, e.args.map { simplify(it) }) else e
        val (n, d) = together(inner)
        // Try the canceled fraction and the expanded form; keep whichever is shortest.
        // (2 + 2√2)/2 → 1 + √2 comes from expanding.
        val candidates = listOfNotNull(inner, runCatching { cancel(n, d) }.getOrNull(), runCatching { expand(inner) }.getOrNull())
        return candidates.minBy { Printer.plain(it).length }
    }

    private fun cancel(n: Expr, d: Expr): Expr {
        if (d == ONE) return n
        val vars = n.freeVars() + d.freeVars()
        if (vars.size == 1) {
            val x = Sym(vars.first())
            val pn = qpoly(n, x)
            val pd = qpoly(d, x)
            if (pn != null && pd != null) {
                val g = QPoly.gcd(pn, pd)
                var top = pn.divMod(g).first
                var bottom = pd.divMod(g).first
                val lc = bottom.lead
                top = top.scale(lc.reciprocal())
                bottom = bottom.scale(lc.reciprocal())
                return if (bottom.degree == 0) top.toExpr(x) else div(top.toExpr(x), bottom.toExpr(x))
            }
        }
        // Keep the denominator's leading sign positive: 1/(−C₁ − x) → −1/(x + C₁).
        if (d is Add && Simplify.isNegative(Printer.sortedTerms(d).first())) return div(neg(n), expand(neg(d)))
        return div(n, d)
    }

    /** Partial fractions: 1/(x² − 1) = 1/(2(x − 1)) − 1/(2(x + 1)). */
    fun apart(e: Expr, x: Sym): Expr {
        val (n, d) = together(e)
        if (d.freeOf(x)) return e
        val p = qpoly(n, x) ?: throw MathError("apart needs a fraction of polynomials in ${x.name}")
        val q = qpoly(d, x) ?: throw MathError("apart needs a fraction of polynomials in ${x.name}")
        val (whole, rem) = p.divMod(q)
        val parts = ArrayList<Expr>()
        if (!whole.isZero) parts += whole.toExpr(x)
        if (rem.isZero) return add(parts)
        var rest = q.monic()
        val linear = ArrayList<Pair<Rational, Int>>()
        for (r in rationalRoots(rest)) {
            var m = 0
            val lin = QPoly(listOf(-r, Rational.ONE))
            while (true) {
                val (qq, rr) = rest.divMod(lin)
                if (!rr.isZero) break
                rest = qq; m++
            }
            if (m > 0) linear += r to m
        }
        // Unknowns: A/(x − r)^k for each root, and a polynomial of lower degree over the leftover factor.
        class Term(val root: Rational?, val power: Int, val xPower: Int)
        val terms = ArrayList<Term>()
        for ((r, m) in linear) for (k in 1..m) terms += Term(r, k, 0)
        for (j in 0 until rest.degree) terms += Term(null, 1, j)
        val qm = q.monic()
        val basis = terms.map { t ->
            if (t.root != null) {
                var den = QPoly(listOf(Rational.ONE))
                repeat(t.power) { den *= QPoly(listOf(-t.root, Rational.ONE)) }
                qm.divMod(den).first
            } else qm.divMod(rest).first * QPoly(List(t.xPower) { Rational.ZERO } + Rational.ONE)
        }
        val target = rem.scale(q.lead.reciprocal())
        val size = terms.size
        val a = Array(size) { row -> Array(size + 1) { col -> if (col < size) basis[col][row] else target[row] } }
        for (c in 0 until size) {
            val piv = (c until size).firstOrNull { a[it][c].signum != 0 } ?: throw MathError("Couldn't split this fraction")
            val t = a[piv]; a[piv] = a[c]; a[c] = t
            val pv = a[c][c]
            for (k in c..size) a[c][k] = a[c][k] / pv
            for (r in 0 until size) if (r != c && a[r][c].signum != 0) {
                val f = a[r][c]
                for (k in c..size) a[r][k] = a[r][k] - f * a[c][k]
            }
        }
        var leftoverNumerator: Expr = ZERO
        terms.forEachIndexed { i, t ->
            val coef = a[i][size]
            if (coef.signum == 0) return@forEachIndexed
            if (t.root != null) parts += div(Num(coef), pow(sub(x, Num(t.root)), t.power.toLong()))
            else leftoverNumerator = add(leftoverNumerator, mul(Num(coef), pow(x, t.xPower.toLong())))
        }
        if (leftoverNumerator != ZERO) parts += div(leftoverNumerator, rest.toExpr(x))
        return add(parts)
    }

    // ---- Factor -----------------------------------------------------------------------------

    fun factor(e: Expr): Expr {
        if (e is Mat) return Mat(e.rows, e.cols, e.cells.map { factor(it) })
        if (e is Eq) return Eq(factor(e.lhs), factor(e.rhs))
        val (n, d) = together(e)
        if (d != ONE) return div(factorPolynomial(n), factorPolynomial(d))
        return factorPolynomial(n)
    }

    private fun factorPolynomial(e: Expr): Expr {
        val vars = e.freeVars()
        if (vars.size == 1) {
            val x = Sym(vars.first())
            qpoly(e, x)?.let { return factorQ(it, x) }
        }
        return commonFactor(expand(e))
    }

    /** Pulls out the numeric content and lowest powers shared by every term: 2xy + 4x = 2x(y + 2). */
    private fun commonFactor(e: Expr): Expr {
        if (e !is Add) return e
        val parts = e.terms.map { Simplify.splitCoefficient(it) }
        var g = BigInteger.ZERO
        var lcm = BigInteger.ONE
        for ((c, _) in parts) {
            val q = (c as? Num)?.q ?: return e
            g = g.gcd(q.num)
            lcm = lcm / lcm.gcd(q.den) * q.den
        }
        val content = Rational.of(g, lcm)
        // Lowest power of each base across all terms.
        fun powers(t: Expr): Map<Expr, Rational> {
            val m = HashMap<Expr, Rational>()
            val fs = if (t is Mul) t.factors else listOf(t)
            for (f in fs) when {
                f.isNumber -> {}
                f is Pow && f.exp is Num -> m[f.base] = f.exp.q
                f is Pow -> {}
                else -> m[f] = Rational.ONE
            }
            return m
        }
        val maps = parts.map { powers(it.second) }
        val shared = maps[0].keys.filter { b -> maps.all { it.containsKey(b) } }
        val common = shared.map { b -> pow(b, Num(maps.minOf { it.getValue(b) })) }
        val commonExpr = mul(listOf(Num(content)) + common)
        if (commonExpr == ONE) return e
        return mul(commonExpr, add(e.terms.map { div(it, commonExpr) }))
    }

    fun factorQ(p: QPoly, x: Sym): Expr {
        if (p.degree <= 0) return p.toExpr(x)
        val (content, prim0) = p.primitive()
        val factors = ArrayList<Expr>()
        var prim = prim0
        // x^k
        val k = prim.c.indexOfFirst { it.signum != 0 }
        if (k > 0) { factors += pow(x, k.toLong()); prim = QPoly(prim.c.drop(k)) }
        // Linear factors from rational roots.
        for (r in rationalRoots(prim)) {
            val lin = QPoly(listOf(-Rational.of(r.num), Rational.of(r.den))) // den·x − num
            while (prim.degree > 0) {
                val (q, rem) = prim.divMod(lin)
                if (!rem.isZero) break
                prim = q
                factors += lin.toExpr(x)
            }
        }
        // x⁴ + ax² + b: factor as a polynomial in x².
        if (prim.degree >= 4 && prim.c.indices.all { it % 2 == 0 || prim[it].signum == 0 }) {
            val y = QPoly(prim.c.filterIndexed { i, _ -> i % 2 == 0 })
            val inY = factorQ(y, Sym("\u0001y"))
            if (inY is Mul || inY is Pow) {
                factors += inY.subst(Sym("\u0001y"), pow(x, TWO))
                prim = QPoly(listOf(Rational.ONE))
            }
        }
        if (prim.degree > 0) factors += prim.toExpr(x)
        return mul(listOf(Num(content)) + factors)
    }

    /** Rational roots p/q of an integer polynomial (p divides the constant term, q the leading one). */
    fun rationalRoots(p: QPoly): List<Rational> {
        if (p.degree < 1) return emptyList()
        val k = p.c.indexOfFirst { it.signum != 0 }
        val roots = ArrayList<Rational>()
        if (k > 0) roots += Rational.ZERO
        val poly = QPoly(p.c.drop(k))
        if (poly.degree < 1) return roots
        val prim = poly.primitive().second
        val a0 = prim[0].num.abs()
        val an = prim.lead.num.abs()
        val ps = divisors(a0) ?: return roots
        val qs = divisors(an) ?: return roots
        if (ps.size * qs.size > 20_000) return roots
        val seen = HashSet<Rational>()
        for (pp in ps) for (qq in qs) for (s in listOf(1L, -1L)) {
            val r = Rational.of(BigInteger.valueOf(s) * pp, qq)
            if (seen.add(r) && prim.eval(r).signum == 0) roots += r
        }
        return roots
    }

    private fun divisors(n: BigInteger): List<BigInteger>? {
        if (n.signum() == 0) return listOf(BigInteger.ONE)
        if (n.bitLength() > 60) return null
        val f = Simplify.factorize(n) ?: return null
        var ds = listOf(BigInteger.ONE)
        for ((p, e) in f) ds = ds.flatMap { d -> (0..e).map { d * p.pow(it) } }
        return ds
    }

    // ---- Solve --------------------------------------------------------------------------------

    /** Solutions of eq for x, as x = … equations (several in a [Seq]). */
    fun solve(eq: Expr, x: Sym): Expr {
        val (l, r) = if (eq is Eq) eq.lhs to eq.rhs else eq to ZERO
        val f = sub(l, r)
        val (n, d) = together(f)
        val numerator = expand(n)
        if (numerator.freeOf(x)) {
            if (numerator == ZERO) throw MathError("True for every ${x.name}")
            throw MathError("No solution")
        }
        val raw = solvePolynomial(numerator, x) ?: isolate(l, r, x) ?: numericRoots(f, x)
        val valid = raw.filter { s -> denominatorOk(d, x, s) }
        val unique = ArrayList<Expr>()
        for (s in valid.map { simplify(it) }) if (unique.none { it == s || sameNumber(it, s) }) unique += s
        if (unique.isEmpty()) throw MathError("No solution")
        // Real solutions in increasing order, complex ones after.
        val sorted = unique.sortedWith(compareBy({ solutionKey(it).first }, { solutionKey(it).second }))
        val eqs = sorted.map { Eq(x, it) }
        return if (eqs.size == 1) eqs[0] else Seq(eqs)
    }

    private fun solutionKey(s: Expr): Pair<Int, Double> {
        if (!s.isConstant) return 2 to 0.0
        val v = runCatching { Numeric.eval(s) }.getOrNull() ?: return 2 to 0.0
        return if (v.isReal) 0 to v.re else 1 to v.re * 1000 + v.im
    }

    private fun sameNumber(a: Expr, b: Expr): Boolean {
        if (!a.isConstant || !b.isConstant) return false
        val x = runCatching { Numeric.eval(a) }.getOrNull() ?: return false
        val y = runCatching { Numeric.eval(b) }.getOrNull() ?: return false
        return (x - y).abs() < 1e-9 * maxOf(1.0, x.abs())
    }

    private fun denominatorOk(d: Expr, x: Sym, s: Expr): Boolean {
        if (d.freeOf(x)) return true
        val v = runCatching { simplify(d.subst(x, s)) }.getOrNull() ?: return false
        if (v == ZERO) return false
        if (v.isConstant) return runCatching { Numeric.eval(v).abs() > 1e-12 }.getOrDefault(true)
        return true
    }

    private fun solvePolynomial(p: Expr, x: Sym): List<Expr>? {
        val cs = coefficients(p, x) ?: return null
        return when (cs.size - 1) {
            1 -> listOf(neg(div(cs[0], cs[1])))
            2 -> quadratic(cs[2], cs[1], cs[0])
            else -> if (cs.all { it is Num }) solveQ(QPoly(cs.map { (it as Num).q }), x) else null
        }
    }

    private fun quadratic(a: Expr, b: Expr, c: Expr): List<Expr> {
        val disc = expand(sub(pow(b, TWO), mul(Num(4), a, c)))
        val twoA = mul(TWO, a)
        if (disc == ZERO) return listOf(simplify(div(neg(b), twoA)))
        val root = sqrt(disc)
        return listOf(
            simplify(div(add(neg(b), neg(root)), twoA)),
            simplify(div(add(neg(b), root), twoA)),
        )
    }

    private fun solveQ(p0: QPoly, x: Sym): List<Expr> {
        // a xⁿ + c: all n roots exactly, r·(cos θ + i sin θ).
        val nonzero = p0.c.indices.filter { p0[it].signum != 0 }
        if (nonzero.size == 2 && nonzero[0] == 0 && p0.degree >= 3) {
            val n = p0.degree
            val v = -p0[0] / p0.lead
            val r = pow(Num(v.abs()), Num(Rational.of(1, n.toLong())))
            return (0 until n).map { k ->
                val angle = mul(Num(Rational.of((2 * k + if (v.signum < 0) 1 else 0).toLong(), n.toLong())), PI)
                expandComplex(mul(r, add(fn("cos", angle), mul(I, fn("sin", angle)))))
            }.sortedBy { if (it.contains { e -> e == I || e is Flt && false }) 1 else 0 }
        }
        var p = p0
        val out = ArrayList<Expr>()
        for (r in rationalRoots(p)) {
            out += Num(r)
            val lin = QPoly(listOf(-r, Rational.ONE))
            while (p.degree > 0) {
                val (q, rem) = p.divMod(lin)
                if (!rem.isZero) break
                p = q
            }
        }
        when {
            p.degree == 1 -> out += Num(-p[0] / p[1])
            p.degree == 2 -> out += quadratic(Num(p[2]), Num(p[1]), Num(p[0]))
            p.degree >= 3 && p.c.indices.all { it % 2 == 0 || p[it].signum == 0 } && p.degree <= 8 -> {
                // Polynomial in x²: solve for y = x², then x = ±√y.
                val y = QPoly(p.c.filterIndexed { i, _ -> i % 2 == 0 })
                for (s in solveQ(y, x)) {
                    val root = sqrt(s)
                    out += neg(root); out += root
                }
            }
            p.degree >= 3 -> out += durandKerner(p).map { Numeric.fromCD(it) }
        }
        return out
    }

    private fun expandComplex(e: Expr): Expr = expand(e)

    /** All complex roots of a polynomial, numerically. */
    private fun durandKerner(p: QPoly): List<CD> {
        val n = p.degree
        val a = p.monic().c.map { it.toDouble() }
        var z = List(n) { k -> CD(0.4, 0.9).let { seed -> var w = CD(1.0); repeat(k) { w *= seed }; w } }
        repeat(500) {
            z = z.mapIndexed { i, zi ->
                var num = CD(1.0)
                for (k in n - 1 downTo 0) num = num * zi + CD(a[k])
                var den = CD(1.0)
                for (j in z.indices) if (j != i) den *= (zi - z[j])
                zi - num / den
            }
        }
        return z.map { if (kotlin.math.abs(it.im) < 1e-10) CD(it.re) else it }
            .sortedWith(compareBy({ it.im != 0.0 }, { it.re }, { it.im }))
    }

    /**
     * Undoes the operations around x one at a time: 2^(x+1) = 8 → x + 1 = log₂ 8 → x = 2.
     * Works when x appears once.
     */
    private fun isolate(l: Expr, r: Expr, x: Sym): List<Expr>? {
        val f = expand(sub(l, r))
        val terms = if (f is Add) f.terms else listOf(f)
        val withX = terms.filter { !it.freeOf(x) }
        val rest = neg(add(terms.filter { it.freeOf(x) }))
        if (withX.size != 1) {
            // Try the equation as written, before moving terms.
            if (!l.freeOf(x) && r.freeOf(x)) return invert(l, r, x)
            if (!r.freeOf(x) && l.freeOf(x)) return invert(r, l, x)
            return null
        }
        return invert(withX[0], rest, x)
    }

    private fun invert(h: Expr, v: Expr, x: Sym): List<Expr>? {
        if (h == x) return listOf(v)
        return when (h) {
            is Add -> {
                val (withX, without) = h.terms.partition { !it.freeOf(x) }
                if (withX.size != 1) null else invert(withX[0], sub(v, add(without)), x)
            }
            is Mul -> {
                val (withX, without) = h.factors.partition { !it.freeOf(x) }
                if (withX.size != 1) null else invert(withX[0], div(v, mul(without)), x)
            }
            is Pow -> when {
                h.exp.freeOf(x) -> {
                    val root = pow(v, pow(h.exp, MINUS_ONE))
                    val even = (h.exp as? Num)?.q?.let { it.isInteger && !it.num.testBit(0) } ?: false
                    val branches = if (even) listOf(neg(root), root) else listOf(root)
                    branches.flatMap { invert(h.base, it, x) ?: return null }
                }
                h.base.freeOf(x) -> invert(h.exp, fn("log", h.base, v), x)
                else -> null
            }
            is Fn -> {
                val a = h.args.last()
                val branches = when (h.name) {
                    "sin" -> listOf(fn("asin", v), sub(PI, fn("asin", v)))
                    "cos" -> listOf(fn("acos", v), neg(fn("acos", v)))
                    "tan" -> listOf(fn("atan", v))
                    "asin" -> listOf(fn("sin", v))
                    "acos" -> listOf(fn("cos", v))
                    "atan" -> listOf(fn("tan", v))
                    "ln" -> listOf(pow(E, v))
                    "log" -> listOf(pow(h.args[0], v))
                    "sinh" -> listOf(fn("asinh", v))
                    "cosh" -> listOf(fn("acosh", v), neg(fn("acosh", v)))
                    "tanh" -> listOf(fn("atanh", v))
                    "abs" -> listOf(neg(v), v)
                    else -> return null
                }
                branches.flatMap { invert(a, it, x) ?: return null }
            }
            else -> null
        }
    }

    /** Real roots found by scanning for sign changes, then bisecting. */
    private fun numericRoots(f: Expr, x: Sym): List<Expr> {
        val g = { t: Double -> runCatching { Numeric.real(f, mapOf(x.name to t)) }.getOrNull() }
        val roots = ArrayList<Double>()
        val n = 4000
        var prevT = -100.0
        var prev = g(prevT)
        for (i in 1..n) {
            val t = -100.0 + 200.0 * i / n
            val v = g(t)
            if (prev != null && v != null) {
                if (v == 0.0) roots += t
                else if (prev * v < 0) {
                    var a = prevT
                    var b = t
                    var fa: Double = prev
                    repeat(100) {
                        val m = (a + b) / 2
                        val fm = g(m) ?: return@repeat
                        if (fa * fm <= 0) b = m else { a = m; fa = fm }
                    }
                    val root = (a + b) / 2
                    // Skip poles like tan x at π/2, where the sign flips through infinity.
                    val check = g(root)
                    if (check != null && kotlin.math.abs(check) < 1e-6 * maxOf(1.0, kotlin.math.abs(prev), kotlin.math.abs(v))) roots += root
                }
            }
            prevT = t
            prev = v
            if (roots.size > 12) break
        }
        if (roots.isEmpty()) throw MathError("No solution found")
        return roots.map { Flt(if (kotlin.math.abs(it) < 1e-12) 0.0 else it) }
    }
}
