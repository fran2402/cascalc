package com.example.cas.engine

import com.example.cas.cas.Add
import com.example.cas.cas.Algebra
import com.example.cas.cas.Calculus
import com.example.cas.cas.E
import com.example.cas.cas.Eq
import com.example.cas.cas.Expr
import com.example.cas.cas.Fn
import com.example.cas.cas.MINUS_ONE
import com.example.cas.cas.Mul
import com.example.cas.cas.Num
import com.example.cas.cas.Numeric
import com.example.cas.cas.Pow
import com.example.cas.cas.Printer
import com.example.cas.cas.Seq
import com.example.cas.cas.ZERO
import com.example.cas.cas.add
import com.example.cas.cas.contains
import com.example.cas.cas.div
import com.example.cas.cas.freeOf
import com.example.cas.cas.freeVars
import com.example.cas.cas.mul
import com.example.cas.cas.neg
import com.example.cas.cas.sub
import com.example.cas.cas.subst
import com.example.cas.editor.Func
import com.example.cas.editor.Integral
import com.example.cas.editor.MathRow
import com.example.cas.editor.Node
import com.example.cas.editor.Sym as SymNode
import com.example.cas.cas.Sym

/**
 * Worked steps for a calculation (beta): an integral, definite or not, or a ∮ loop integral.
 * The steps follow the methods the integrator itself uses (sum rule, constant multiple, the
 * standard integrals, partial fractions, by parts, substitution), so they lead to the answer the
 * calculator gave; an indefinite integral ends with its check by differentiation, a loop
 * integral with the residue theorem.
 */
object Steps {
    enum class Kind { Rule, Result, Check, Note }

    /** One step: the rule used, a line about it, the math it gives, and the smaller steps inside it. */
    class Step(
        val title: String,
        val text: String? = null,
        val math: MathRow? = null,
        val kind: Kind = Kind.Rule,
        val substeps: List<Step> = emptyList(),
    )

    /** The whole working: the method in a few words, the steps, and the final line. */
    class Solution(val method: String, val steps: List<Step>, val answer: MathRow)

    /** Whether there are steps for [row]: a lone integral or ∮ (steps are worked out only when asked for). */
    fun supports(row: MathRow): Boolean = target(row) != null

    private fun target(row: MathRow): Node? =
        row.items.filter { !(it is SymNode && it.text.isBlank()) }.singleOrNull()?.takeIf { it is Integral || (it is Func && it.name == "contour") }

    /** The working for [row], or null if there is none (or it couldn't be found). */
    fun of(row: MathRow, angle: AngleUnit = AngleUnit.Radians): Solution? = runCatching {
        when (val n = target(row)) {
            is Integral -> integral(n, angle)
            is Func -> contour(n, angle)
            else -> null
        }
    }.getOrNull()

    // ---- Math rows ---------------------------------------------------------------------------

    private fun ex(e: Expr): MathRow = Formatter.row(e)
    private fun sym(t: String) = SymNode(t)

    /** Rows and nodes joined into one row. */
    private fun line(vararg parts: Any): MathRow {
        val out = MathRow()
        for (p in parts) when (p) {
            is MathRow -> com.example.cas.editor.MathCodec.copy(p).items.toList().forEach { out.add(it) }
            is Node -> out.add(p)
            is String -> out.add(sym(p))
            is Expr -> ex(p).items.toList().forEach { out.add(it) }
        }
        return out
    }

    /** ∫ body d[x], with limits when given. */
    private fun int(body: Expr, x: Sym, lo: Expr? = null, hi: Expr? = null): MathRow =
        MathRow(mutableListOf(Integral(lo?.let { ex(it) } ?: MathRow(), hi?.let { ex(it) } ?: MathRow(), ex(body), MathRow(mutableListOf(sym(x.name))))))

    private fun eq(a: Any, b: Any) = line(a, "=", b)

    private fun same(a: Expr, b: Expr) = Printer.plain(a) == Printer.plain(b)

    // ---- Integrals -----------------------------------------------------------------------------

    private class Traced(val result: Expr, val steps: List<Step>)

    private fun integral(n: Integral, angle: AngleUnit): Solution? {
        val name = n.variable.items.joinToString("") { (it as? SymNode)?.text ?: "" }
        if (name.length != 1) return null
        val x = Sym(name)
        val ev = { r: MathRow -> Evaluator(angle).evaluate(r) }
        val body = ev(n.body)
        val definite = !n.lower.isEmpty || !n.upper.isEmpty
        val f = Calculus.antiderivative(body, x)
        val steps = ArrayList<Step>()
        if (f == null) {
            if (!definite) return null
            val a = ev(n.lower); val b = ev(n.upper)
            val value = Calculus.definite(body, x, a, b)
            steps += Step("No closed form", "No antiderivative in standard functions was found, so the area is computed numerically.", eq(int(body, x, a, b), value), Kind.Note)
            return Solution("Numerical integration", steps, eq(int(body, x, a, b), value))
        }
        val traced = trace(body, x, 0)
        steps += traced?.steps ?: listOf(Step("Integrate", "Found by rewriting the integrand.", eq(int(body, x), f)))
        if (traced != null && !same(traced.result, f)) steps += Step("Simplify", null, eq(traced.result, f))
        val method = methodOf(steps)
        if (!definite) {
            // The check: differentiating gives the integrand back.
            val back = Algebra.simplify(Calculus.diff(f, x))
            if (agrees(back, body, x)) {
                steps += Step(
                    "Check", "Differentiating the answer gives the integrand back.",
                    line(com.example.cas.editor.Derivative(MathRow(mutableListOf(sym(x.name))), ex(f)), "=", back), Kind.Check,
                )
            }
            val answer = line(int(body, x), "=", f, "+", "C")
            steps += Step("Answer", "Add a constant C: every antiderivative differs by one.", answer, Kind.Result)
            return Solution(method, steps, answer)
        }
        val a = ev(n.lower); val b = ev(n.upper)
        val value = Calculus.definite(body, x, a, b)
        val fa = at(f, x, a, fromBelow = false); val fb = at(f, x, b, fromBelow = true)
        steps += Step(
            "Fundamental theorem", "F(b) − F(a): the antiderivative at the upper limit minus its value at the lower one" +
                (if (Calculus.isInfinite(a) || Calculus.isInfinite(b)) ", as a limit at ∞." else "."),
            line(int(body, x, a, b), "=", fb, "−", paren(fa)),
        )
        val answer = eq(int(body, x, a, b), value)
        steps += Step("Answer", null, answer, Kind.Result)
        return Solution(method, steps, answer)
    }

    private fun paren(e: Expr): MathRow = if (e is Add || (e is Num && e.q.signum < 0) || (e is Mul && Printer.plain(e).startsWith("-"))) line("(", e, ")") else ex(e)

    /** F at an end: substituted, or as a limit (at ±∞, or where F isn't defined). */
    private fun at(f: Expr, x: Sym, v: Expr, fromBelow: Boolean): Expr {
        if (!Calculus.isInfinite(v)) {
            val s = runCatching { Algebra.simplify(f.subst(x, v)) }.getOrNull()
            if (s != null && runCatching { Numeric.eval(s) }.getOrNull()?.let { it.re.isFinite() && it.im.isFinite() } == true) return s
        }
        return Calculus.limit(f, x, v, if (fromBelow) -1 else 1)
    }

    /** Whether two expressions agree at a few sample points. */
    private fun agrees(a: Expr, b: Expr, x: Sym): Boolean {
        var compared = 0
        for (p in listOf(0.37, 1.13, 2.71, -0.83, 0.61, 1.9)) {
            val env = mapOf(x.name to p) + (a.freeVars() + b.freeVars() - x.name).associateWith { 0.7 }
            val u = runCatching { Numeric.eval(a, env) }.getOrNull() ?: continue
            val v = runCatching { Numeric.eval(b, env) }.getOrNull() ?: continue
            if (!u.re.isFinite() || !v.re.isFinite()) continue
            if ((u - v).abs() > 1e-7 * maxOf(1.0, v.abs())) return false
            compared++
        }
        return compared >= 2
    }

    private fun methodOf(steps: List<Step>): String {
        val titles = steps.flatMap { listOf(it.title) + it.substeps.map { s -> s.title } }
        return listOf("Integration by parts", "Substitution", "Partial fractions", "Special function", "Power rule", "Sum rule")
            .firstOrNull { it in titles } ?: titles.firstOrNull() ?: "Integration"
    }

    /** The steps to ∫ [e] d[x], following the integrator's methods; null if it can't follow them. */
    private fun trace(e: Expr, x: Sym, depth: Int): Traced? {
        if (depth > 5) return blackBox(e, x)
        if (e.freeOf(x)) return Traced(mul(e, x), listOf(Step("Constant", "The integral of a constant c is c·${x.name}.", eq(int(e, x), mul(e, x)))))
        if (e is Add) {
            val parts = e.terms.map { trace(it, x, depth + 1) ?: return null }
            val result = add(parts.map { it.result })
            val split = line(*e.terms.flatMapIndexed { k, t -> if (k == 0) listOf(int(t, x)) else listOf("+", int(t, x)) }.toTypedArray())
            return Traced(result, listOf(Step("Sum rule", "Integrate term by term.", eq(int(e, x), split), substeps = parts.flatMap { it.steps })))
        }
        if (e is Mul) {
            val (consts, rest) = e.factors.partition { it.freeOf(x) }
            if (consts.isNotEmpty()) {
                val c = mul(consts); val r = mul(rest)
                val inner = trace(r, x, depth + 1) ?: return null
                return Traced(mul(c, inner.result), listOf(Step("Constant multiple", "Take ${Printer.plain(c)} out of the integral.", eq(int(e, x), line(paren(c), int(r, x))), substeps = inner.steps)))
            }
        }
        standard(e, x)?.let { return it }
        partialFractions(e, x, depth)?.let { return it }
        byParts(e, x, depth)?.let { return it }
        substitution(e, x, depth)?.let { return it }
        return blackBox(e, x)
    }

    /** A result the integrator finds by a method too involved to spell out here. */
    private fun blackBox(e: Expr, x: Sym): Traced? {
        val f = Calculus.integrate(e, x) ?: return null
        val special = f.contains { it is Fn && it.name in SPECIAL }
        return Traced(f, listOf(
            if (special) Step("Special function", "This has no antiderivative in elementary functions; it's written with a function defined by this very integral.", eq(int(e, x), f))
            else Step("Rewrite and integrate", "Rewrite the integrand (identities, a substitution) and integrate the pieces.", eq(int(e, x), f)),
        ))
    }

    private val SPECIAL = setOf("si", "ci", "ei", "li", "shi", "chi", "erf", "erfi", "fresnels", "fresnelc", "gammainc", "ellipticf", "elliptice", "polylog")

    /** u = a·x + b: a, or null. */
    private fun linear(u: Expr, x: Sym): Expr? {
        val cs = Algebra.coefficients(Algebra.expand(u), x) ?: return null
        return if (cs.size == 2 && cs[1] != ZERO) cs[1] else null
    }

    /** The table of standard integrals, as the integrator's own. */
    private fun standard(e: Expr, x: Sym): Traced? {
        val (title, text) = when {
            e == x -> "Power rule" to "∫ xⁿ dx = xⁿ⁺¹/(n + 1), with n = 1."
            e is Pow && e.exp.freeOf(x) && linear(e.base, x) != null ->
                if (e.exp == MINUS_ONE) "Logarithm" to "∫ 1/u du = ln|u|" + inner(e.base, x)
                else "Power rule" to "∫ uⁿ du = uⁿ⁺¹/(n + 1), with n = ${Printer.plain(e.exp)}" + inner(e.base, x)
            e is Pow && e.base.freeOf(x) && linear(e.exp, x) != null ->
                "Exponential" to (if (e.base == E) "∫ eᵘ du = eᵘ" else "∫ aᵘ du = aᵘ/ln a") + inner(e.exp, x)
            e is Fn && e.args.size == 1 && e.name in TABLE && linear(e.args[0], x) != null ->
                "Standard integral" to TABLE.getValue(e.name) + inner(e.args[0], x)
            else -> return null
        }
        val f = Calculus.integrate(e, x) ?: return null
        return Traced(f, listOf(Step(title, text, eq(int(e, x), f))))
    }

    private fun inner(u: Expr, x: Sym): String {
        val a = linear(u, x) ?: return "."
        return if (u == x) "." else ", with u = ${Printer.plain(u)}" + (if (a == com.example.cas.cas.ONE) "." else ", so divide by ${Printer.plain(a)}.")
    }

    private val TABLE = mapOf(
        "sin" to "∫ sin u du = −cos u", "cos" to "∫ cos u du = sin u", "tan" to "∫ tan u du = −ln|cos u|",
        "sinh" to "∫ sinh u du = cosh u", "cosh" to "∫ cosh u du = sinh u", "tanh" to "∫ tanh u du = ln cosh u",
        "ln" to "∫ ln u du = u ln u − u", "asin" to "∫ asin u du = u asin u + √(1 − u²)",
        "acos" to "∫ acos u du = u acos u − √(1 − u²)", "atan" to "∫ atan u du = u atan u − ln(1 + u²)/2",
    )

    /** A fraction of polynomials: split into partial fractions, then each piece. */
    private fun partialFractions(e: Expr, x: Sym, depth: Int): Traced? {
        val (n, d) = Algebra.together(e)
        if (d.freeOf(x) || Algebra.qpoly(n, x) == null || Algebra.qpoly(d, x) == null) return null
        val split = runCatching { Algebra.apart(e, x) }.getOrNull() ?: return null
        if (split !is Add || same(split, e)) return null
        val inner = trace(split, x, depth + 1) ?: return null
        return Traced(inner.result, listOf(Step("Partial fractions", "Split the fraction into simpler ones.", eq(ex(e), ex(split)))) + inner.steps)
    }

    /** By parts, as the integrator: u a polynomial with eˣ, sin, cos…; or u = ln, atan, asin. */
    private fun byParts(e: Expr, x: Sym, depth: Int): Traced? {
        if (e !is Mul) return null
        val (polyParts, others) = e.factors.partition { Algebra.coefficients(it, x) != null }
        if (polyParts.isEmpty() || others.isEmpty()) return null
        val p = mul(polyParts)
        val g = if (others.size == 1) others[0] else Mul(others)
        val logLike = g is Fn && g.name in setOf("ln", "atan", "asin") && linear(g.args[0], x) != null
        val (u, dv) = if (logLike) g to p else p to g
        if (!logLike && g.contains { it is Fn && it.name in setOf("ln", "atan", "asin", "acos", "erf") }) return null
        val v = Calculus.integrate(dv, x) ?: return null
        val du = Calculus.diff(u, x)
        val restBody = Algebra.simplify(mul(v, du))
        val rest = trace(restBody, x, depth + 1) ?: return null
        val result = sub(mul(u, v), rest.result)
        val choose = Step("Choose u and dv", "u = ${Printer.plain(u)} (it gets simpler when differentiated), dv = ${Printer.plain(dv)} d${x.name}.",
            line(com.example.cas.editor.MathCodec.decode("'u;'=;"), u, ",", " ", "v", "=", v))
        return Traced(result, listOf(Step(
            "Integration by parts", "∫ u dv = uv − ∫ v du",
            eq(int(e, x), line(mul(u, v), "−", int(restBody, x))),
            substeps = listOf(choose) + rest.steps,
        )))
    }

    /** Substitution: e = g(u)·u′ for an inner u, so ∫ e dx = ∫ g(u) du. */
    private fun substitution(e: Expr, x: Sym, depth: Int): Traced? {
        val uName = listOf("u", "w", "s").firstOrNull { it !in e.freeVars() } ?: return null
        val t = Sym(uName)
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
            if ((z is Fn || z is Pow) && !z.freeOf(x) && z != e) candidates += z
            when (z) { is Add -> z.terms.forEach { collect(it) }; is Mul -> z.factors.forEach { collect(it) }; is Pow -> { collect(z.base); collect(z.exp) }; is Fn -> z.args.forEach { collect(it) }; else -> {} }
        }
        collect(e)
        for (u in candidates) {
            val du = runCatching { Calculus.diff(u, x) }.getOrNull() ?: continue
            if (du == ZERO) continue
            val q = Algebra.simplify(div(e, du))
            val inT = q.subst(u, t)
            if (!inT.freeOf(x) || inT == q) continue
            val inner = trace(inT, t, depth + 1) ?: continue
            val result = inner.result.subst(t, u)
            return Traced(result, listOf(
                Step("Substitution", "Let $uName = ${Printer.plain(u)}, so d$uName = ${Printer.plain(du)} d${x.name}.", eq(int(e, x), int(inT, t)), substeps = inner.steps),
                Step("Back-substitute", "Put $uName = ${Printer.plain(u)} back.", eq(inner.result, result)),
            ))
        }
        return null
    }

    // ---- Loop integrals ---------------------------------------------------------------------------

    private fun contour(n: Func, angle: AngleUnit): Solution? {
        if (n.args.size != 2) return null
        val spec = Evaluator(angle).evaluate(n.args[1]) as? Eq ?: return null
        val abs = spec.lhs as? Fn ?: return null
        if (abs.name != "abs") return null
        val inside = abs.args[0]
        val z = inside.freeVars().singleOrNull()?.let { Sym(it) } ?: return null
        val center = Algebra.simplify(neg(sub(inside, z)))
        val radius = spec.rhs
        val body = Evaluator(angle).evaluate(n.args[0])
        val value = com.example.cas.graph.ComplexIntegrals.circle(body, z, center, radius)
        val question = MathRow(mutableListOf(n)).let { com.example.cas.editor.MathCodec.copy(it) }
        val answer = line(question, "=", value)
        val steps = ArrayList<Step>()
        val c = Numeric.eval(center); val r = Numeric.real(radius)
        steps += Step("The circle", "Center ${Printer.plain(center)}, radius ${Printer.plain(radius)}, run counterclockwise.", ex(spec))
        val (_, den) = Algebra.together(body)
        val poles: List<Expr>? = if (den.freeOf(z)) emptyList() else runCatching {
            val sol = Algebra.solve(Eq(den, ZERO), z)
            (if (sol is Seq) sol.items else listOf(sol)).mapNotNull { (it as? Eq)?.takeIf { e -> e.lhs == z }?.rhs }
        }.getOrNull()?.takeIf { it.isNotEmpty() }
        if (poles == null) {
            steps += Step("Numerical", "The singularities can't be found exactly (or f isn't a fraction of polynomials), so the integral is computed numerically around the circle.", answer, Kind.Note)
            return Solution("Numerical", steps, answer)
        }
        if (poles.isEmpty()) {
            steps += Step("Cauchy's theorem", "f has no poles, so it's analytic inside the circle and the integral is 0.", answer, Kind.Result)
            return Solution("Cauchy's theorem", steps, answer)
        }
        steps += Step("Singularities", "Where the denominator is 0.", line(*poles.flatMapIndexed { k, p -> (if (k > 0) listOf(",", " ") else emptyList()) + listOf<Any>(z.name, "=", p) }.toTypedArray()))
        val within = poles.filter { p -> (Numeric.eval(p) - c).abs() < r }
        val outside = poles - within.toSet()
        steps += Step(
            "Inside the circle",
            if (within.isEmpty()) "None of them is inside |${z.name} − ${Printer.plain(center)}| < ${Printer.plain(radius)}."
            else "Only poles inside count" + (if (outside.isEmpty()) "; all of them are." else "; ${outside.joinToString { Printer.plain(it) }} ${if (outside.size == 1) "is" else "are"} outside."),
            if (within.isEmpty()) null else line(*within.flatMapIndexed { k, p -> (if (k > 0) listOf(",", " ") else emptyList()) + listOf<Any>(z.name, "=", p) }.toTypedArray()),
        )
        if (within.isEmpty()) {
            steps += Step("Cauchy's theorem", "No poles inside: the integral is 0.", answer, Kind.Result)
            return Solution("Cauchy's theorem", steps, answer)
        }
        val residues = within.map { p ->
            val res = com.example.cas.graph.ComplexIntegrals.residue(body, z, p)
            val order = orderOf(den, z, p)
            val how = if (order == 1) "Simple pole: Res = lim (${z.name} − a) f(${z.name}) as ${z.name} → a."
            else "Pole of order $order: Res = 1/${order - 1}! · the limit of the ${ordinal(order - 1)} derivative of (${z.name} − a)^$order f(${z.name})."
            Step("Residue at ${z.name} = ${Printer.plain(p)}", how, line(com.example.cas.editor.MathCodec.decode("'R;'e;'s;"), "(", z.name, "=", p, ")", "=", res)) to res
        }
        steps += residues.map { it.first }
        val total = Algebra.simplify(add(residues.map { it.second }))
        steps += Step("Residue theorem", "∮ f dz = 2πi × the sum of the residues inside.", line(question, "=", "2", "π", "i", "·", paren(total), "=", value), Kind.Result)
        return Solution("Residue theorem", steps, answer)
    }

    /** How many times [p] is a root of [den]. */
    private fun orderOf(den: Expr, z: Sym, p: Expr): Int {
        var d = den
        for (m in 1..8) {
            d = Calculus.diff(d, z)
            val v = runCatching { Numeric.eval(Algebra.simplify(d.subst(z, p))) }.getOrNull() ?: return m
            if (v.abs() > 1e-9) return m
        }
        return 1
    }

    private fun ordinal(k: Int) = when (k) { 1 -> "first"; 2 -> "second"; 3 -> "third"; else -> "${k}th" }
}
