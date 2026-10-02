package com.example.cas.cas

/**
 * Ordinary differential equations. The unknown function y and its
 * derivatives appear as the symbols y, y′, y″.
 *
 * Solved exactly:
 *  - first order linear  y′ = a(x)·y + b(x)       (integrating factor)
 *  - first order separable  y′ = f(x)·g(y)          (separate and integrate)
 *  - second order linear with constant coefficients  a·y″ + b·y′ + c·y = g(x),
 *    with g a polynomial, k·e^(mx), sin/cos(ωx), or a sum of these
 *    (characteristic roots + undetermined coefficients)
 * Initial conditions like y(0) = 1, y′(0) = 0 fix the constants C₁, C₂.
 */
object Ode {
    class Condition(val order: Int, val at: Expr, val value: Expr)

    val C1 = Sym("C1")
    val C2 = Sym("C2")

    fun derivativeSymbol(y: Sym, k: Int) = Sym(y.name + "′".repeat(k))

    fun solve(equation: Expr, y: Sym, x: Sym, conditions: List<Condition>): Expr {
        val f = if (equation is Eq) sub(equation.lhs, equation.rhs) else equation
        val order = (1..4).lastOrNull { !f.freeOf(derivativeSymbol(y, it)) } ?: throw MathError("There's no \$y'\$ in that equation")
        val general: Expr = when (order) {
            1 -> firstOrder(f, y, x)
            2 -> secondOrder(f, y, x)
            else -> throw MathError("Only first- and second-order equations are supported")
        }
        if (conditions.isEmpty()) return general
        return applyConditions(general, y, x, conditions)
    }

    // ---- First order ----------------------------------------------------------------------

    private fun firstOrder(f: Expr, y: Sym, x: Sym): Expr {
        val y1 = derivativeSymbol(y, 1)
        val rhs = when (val s = Algebra.solve(Eq(f, ZERO), y1)) {
            is Eq -> s.rhs
            else -> throw MathError("Couldn't write the equation as \$y' = \\ldots\$")
        }
        // Linear: y′ = a(x) y + b(x).
        val cs = Algebra.coefficients(Algebra.expand(rhs), y)
        if (cs != null && cs.size <= 2 && cs.all { it.freeOf(y) }) {
            val a = cs.getOrElse(1) { ZERO }
            val b = cs[0]
            if (a == ZERO) {
                val integral = Calculus.integrate(b, x) ?: throw MathError("Couldn't integrate the right-hand side")
                return Eq(y, add(Algebra.simplify(integral), C1))
            }
            val ia = Calculus.integrate(a, x) ?: throw MathError("Couldn't integrate the coefficient of y")
            val mu = Algebra.simplify(pow(E, neg(ia)))          // integrating factor
            val inner = if (b == ZERO) ZERO else Calculus.integrate(Algebra.simplify(mul(mu, b)), x)
                ?: throw MathError("Couldn't integrate after multiplying by the integrating factor")
            return Eq(y, Algebra.simplify(Algebra.expand(div(add(inner, C1), mu))))
        }
        // Separable: y′ = f(x)·g(y).
        val factored = Algebra.factor(rhs)
        val factors = if (factored is Mul) factored.factors else listOf(factored)
        if (factors.any { !it.freeOf(x) && !it.freeOf(y) }) throw MathError("This equation isn't linear or separable")
        val fx = mul(factors.filter { it.freeOf(y) })
        val gy = mul(factors.filter { !it.freeOf(y) })
        val left = Calculus.integrate(Algebra.simplify(pow(gy, MINUS_ONE)), y) ?: throw MathError("Couldn't integrate 1/g(y)")
        val right = Calculus.integrate(fx, x) ?: throw MathError("Couldn't integrate f(x)")
        val implicit = Eq(Algebra.simplify(left), add(Algebra.simplify(right), C1))
        // Solve for y when possible, e.g. ln|y| = x + C₁ doesn't need to stay implicit.
        return runCatching { Algebra.solve(implicit, y) }.getOrNull()?.takeIf { it !is Seq || it.items.size <= 2 } ?: implicit
    }

    // ---- Second order, constant coefficients -----------------------------------------------

    private fun secondOrder(f: Expr, y: Sym, x: Sym): Expr {
        val y1 = derivativeSymbol(y, 1)
        val y2 = derivativeSymbol(y, 2)
        val ex = Algebra.expand(f)
        fun coefficientOf(v: Sym, e: Expr): Pair<Expr, Expr> {
            val cs = Algebra.coefficients(e, v) ?: throw MathError("The equation must be linear in \$y\$, \$y'\$ and \$y''\$")
            if (cs.size > 2) throw MathError("The equation must be linear in \$y\$, \$y'\$ and \$y''\$")
            return cs.getOrElse(1) { ZERO } to cs[0]
        }
        val (a, r1) = coefficientOf(y2, ex)
        val (b, r2) = coefficientOf(y1, r1)
        val (c, rest) = coefficientOf(y, r2)
        if (!a.isConstant || !b.isConstant || !c.isConstant) throw MathError("Only constant coefficients are supported for \$y''\$")
        val g = neg(rest) // a y″ + b y′ + c y = g(x)

        val r = Sym("\u0001r")
        val roots = values(Algebra.solve(Eq(add(mul(a, pow(r, TWO)), mul(b, r), c), ZERO), r))
        val homogeneous: Expr = when {
            roots.size == 1 -> mul(add(C1, mul(C2, x)), pow(E, mul(roots[0], x)))
            roots.all { Numeric.eval(it).isReal } -> add(mul(C1, pow(E, mul(roots[0], x))), mul(C2, pow(E, mul(roots[1], x))))
            else -> {
                val (alpha, betaRaw) = Simplify.splitComplex(Algebra.expand(roots[0]))
                val beta = fn("abs", betaRaw)
                mul(pow(E, mul(alpha, x)), add(mul(C1, fn("cos", mul(beta, x))), mul(C2, fn("sin", mul(beta, x)))))
            }
        }
        val particular = if (LinearAlgebra.isZero(g)) ZERO else particular(a, b, c, g, x, roots)
        return Eq(y, Algebra.simplify(add(homogeneous, particular)))
    }

    private fun values(solutions: Expr): List<Expr> = when (solutions) {
        is Eq -> listOf(solutions.rhs)
        is Seq -> solutions.items.map { (it as Eq).rhs }
        else -> emptyList()
    }

    private fun multiplicity(m: Expr, roots: List<Expr>, repeated: Boolean): Int {
        val n = roots.count { LinearAlgebra.isZero(sub(it, m)) }
        return if (n == 1 && repeated) 2 else n
    }

    /** Undetermined coefficients, one right-hand-side term type at a time. */
    private fun particular(a: Expr, b: Expr, c: Expr, g: Expr, x: Sym, roots: List<Expr>): Expr {
        val repeated = roots.size == 1
        val terms = if (Algebra.expand(g) is Add) (Algebra.expand(g) as Add).terms else listOf(Algebra.expand(g))
        val polyPart = terms.filter { Algebra.coefficients(it, x) != null }
        val others = terms - polyPart.toSet()
        val parts = ArrayList<Expr>()
        fun L(u: Expr) = add(mul(a, Calculus.diff(Calculus.diff(u, x), x)), mul(b, Calculus.diff(u, x)), mul(c, u))

        if (polyPart.isNotEmpty()) {
            val p = add(polyPart)
            val d = Algebra.coefficients(p, x)!!.size - 1
            val s = multiplicity(ZERO, roots, repeated)
            val unknowns = (0..d).map { Sym("\u0001A$it") }
            val trial = mul(pow(x, s.toLong()), add(unknowns.mapIndexed { k, u -> mul(u, pow(x, k.toLong())) }))
            parts += fit(trial, unknowns, Algebra.expand(sub(L(trial), p)), listOf(x))
        }
        // Group exponentials by exponent and trig terms by frequency.
        for (t in others) {
            val (k, core) = Simplify.splitCoefficient(t)
            when {
                core is Pow && core.base == E && linearCoefficient(core.exp, x) != null && core.exp.subst(x, ZERO).let { LinearAlgebra.isZero(it) } -> {
                    val m = linearCoefficient(core.exp, x)!!
                    val s = multiplicity(m, roots, repeated)
                    val u = Sym("\u0001A0")
                    val trial = mul(u, pow(x, s.toLong()), core)
                    // Divide out e^(mx): what's left must vanish identically.
                    val residual = Algebra.expand(Algebra.simplify(div(sub(L(trial), t), core)))
                    parts += fit(trial, listOf(u), residual, listOf(x))
                }
                core is Fn && core.name in setOf("sin", "cos") && linearCoefficient(core.args[0], x) != null -> {
                    val w = linearCoefficient(core.args[0], x)!!
                    val resonant = roots.any { r -> Simplify.splitComplex(Algebra.expand(r)).let { (re, im) -> LinearAlgebra.isZero(re) && LinearAlgebra.isZero(sub(fn("abs", im), fn("abs", w))) } }
                    val u = Sym("\u0001A0"); val v = Sym("\u0001A1")
                    val cosT = fn("cos", mul(w, x)); val sinT = fn("sin", mul(w, x))
                    val trial = mul(pow(x, if (resonant) 1 else 0), add(mul(u, cosT), mul(v, sinT)))
                    val cS = Sym("\u0001C"); val sS = Sym("\u0001S")
                    val residual = Algebra.expand(sub(L(trial), mul(k, core))).subst(cosT, cS).subst(sinT, sS)
                    parts += fit(trial, listOf(u, v), Algebra.expand(residual), listOf(cS, sS, x))
                }
                else -> throw MathError("Only polynomial, e^(mx), sin and cos right-hand sides are supported")
            }
        }
        return add(parts)
    }

    private fun linearCoefficient(u: Expr, x: Sym): Expr? {
        val cs = Algebra.coefficients(Algebra.expand(u), x) ?: return null
        return if (cs.size == 2) cs[1] else null
    }

    /** Chooses the unknowns so that [residual] is identically zero, by matching coefficients in [basis]. */
    private fun fit(trial: Expr, unknowns: List<Sym>, residual: Expr, basis: List<Sym>): Expr {
        var coeffs = listOf(residual)
        for (v in basis) coeffs = coeffs.flatMap { Algebra.coefficients(Algebra.expand(it), v) ?: throw MathError("Couldn't match coefficients") }
        val equations = coeffs.filterNot { LinearAlgebra.isZero(it) }
        if (equations.isEmpty()) return ZERO
        val sol = Systems.solve(equations, unknowns)
        val eqs = (sol as? Seq)?.items?.filterIsInstance<Eq>() ?: listOfNotNull(sol as? Eq)
        var out = trial
        for (e in eqs) out = out.subst(e.lhs, e.rhs)
        return Algebra.simplify(out)
    }

    // ---- Initial conditions ---------------------------------------------------------------------

    private fun applyConditions(general: Expr, y: Sym, x: Sym, conditions: List<Condition>): Expr {
        val constants = listOf(C1, C2).filter { !general.freeOf(it) }
        val equations = conditions.map { cond ->
            when {
                general is Eq && general.lhs == y -> {
                    var d = general.rhs
                    repeat(cond.order) { d = Calculus.diff(d, x) }
                    sub(Algebra.simplify(d.subst(x, cond.at)), cond.value)
                }
                general is Eq && cond.order == 0 -> sub(general.lhs.subst(y, cond.value).subst(x, cond.at), general.rhs.subst(y, cond.value).subst(x, cond.at))
                else -> throw MathError("Can't apply that condition to this solution")
            }
        }
        val sol = Systems.solve(equations, constants.take(equations.size))
        val eqs = (sol as? Seq)?.items?.filterIsInstance<Eq>() ?: listOfNotNull(sol as? Eq)
        var out = general
        for (e in eqs) out = out.subst(e.lhs, e.rhs)
        return if (out is Eq) Eq(out.lhs, Algebra.simplify(out.rhs)) else out
    }
}
