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
import com.example.cas.cas.children
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
 * Worked steps for a calculation (beta): an integral (definite or not), a ∮ loop integral, a
 * derivative, a limit or a sum.
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
    fun supports(row: MathRow): Boolean = target(row) != null || isComplexArithmetic(row) || simplificationMoves(row, AngleUnit.Radians).isNotEmpty()

    /** The rewrites that simplify a plain algebraic expression (empty if there are none, or it isn't one). */
    private fun simplificationMoves(row: MathRow, angle: AngleUnit): List<com.example.cas.cas.AutoSimplify.Move> {
        fun plain(r: MathRow): Boolean = r.items.all { n ->
            !(n is SymNode && n.text in setOf("=", "<", ">", "≤", "≥", ":=")) && n !is Integral && n !is com.example.cas.editor.Derivative &&
                n !is com.example.cas.editor.BigOp && n !is com.example.cas.editor.Matrix && n.slots.all { plain(it) }
        }
        if (!plain(row)) return emptyList()
        val raw = runCatching { Evaluator(angle).also { it.autoSimplify = false }.evaluate(com.example.cas.editor.MathCodec.copy(row)) }.getOrNull() ?: return emptyList()
        return runCatching { com.example.cas.cas.AutoSimplify.moves(raw) }.getOrDefault(emptyList())
    }

    private fun simplification(row: MathRow, angle: AngleUnit): Solution? {
        val moves = simplificationMoves(row, angle)
        if (moves.isEmpty()) return null
        val raw = Evaluator(angle).also { it.autoSimplify = false }.evaluate(com.example.cas.editor.MathCodec.copy(row))
        val steps = ArrayList<Step>()
        steps += Step("Start", "The expression as typed.", ex(raw))
        var before = raw
        for (mv in moves) {
            steps += Step(mv.rule, mv.text, eq(before, mv.result))
            before = mv.result
        }
        val answer = line(row, "=", before)
        steps += Step("Simplest form", "Nothing makes it any shorter now.", answer, Kind.Result)
        return Solution(moves.first().rule.let { if (moves.size > 1) "Simplification" else it }, steps, answer)
    }

    /** Arithmetic with i in it that's worth spelling out: a quotient, product or power, or e^(iθ). */
    private fun isComplexArithmetic(row: MathRow): Boolean {
        var hasI = false; var hasOp = false
        fun walk(r: MathRow) {
            for (n in r.items) {
                if (n is SymNode && n.text == "i") hasI = true
                if (n is com.example.cas.editor.Frac || n is com.example.cas.editor.Pow || (n is SymNode && n.text in setOf("(", "×", "·"))) hasOp = true
                if (n is SymNode && n.text.length == 1 && n.text[0].isLetter() && n.text !in setOf("i", "e", "π")) return
                n.slots.forEach { walk(it) }
            }
        }
        walk(row)
        return hasI && hasOp && row.items.none { it is Integral || it is com.example.cas.editor.Derivative || it is com.example.cas.editor.BigOp } &&
            !row.items.any { n -> n is SymNode && n.text.length == 1 && n.text[0].isLetter() && n.text !in setOf("i", "e", "π") }
    }

    private fun target(row: MathRow): Node? =
        row.items.filter { !(it is SymNode && it.text.isBlank()) }.singleOrNull()?.takeIf {
            it is Integral || it is com.example.cas.editor.Derivative || it is com.example.cas.editor.BigOp ||
                (it is Func && it.name in setOf("contour", "lim", "residue", "taylor", "det"))
        }

    /** The working for [row], or null if there is none (or it couldn't be found). */
    fun of(row: MathRow, angle: AngleUnit = AngleUnit.Radians): Solution? = runCatching {
        when (val n = target(row)) {
            is Integral -> integral(n, angle)
            is com.example.cas.editor.Derivative -> derivative(n, angle)
            is com.example.cas.editor.BigOp -> if (n.kind == com.example.cas.editor.BigOpKind.Sum) sum(n, angle) else product(n, angle)
            is Func -> when (n.name) {
                "lim" -> limit(n, angle)
                "residue" -> residue(n, angle)
                "taylor" -> taylor(n, angle)
                "det" -> determinant(n, angle)
                else -> contour(n, angle)
            }
            else -> if (isComplexArithmetic(row)) complex(row, angle) else simplification(row, angle)
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
        multiplyOut(e, x, depth)?.let { return it }
        return blackBox(e, x)
    }

    /** A result the integrator finds by a method too involved to spell out here. */
    /** Multiplied out first, then term by term. */
    private fun multiplyOut(e: Expr, x: Sym, depth: Int): Traced? {
        val ex = Algebra.expand(e)
        if (ex !is Add || same(ex, e)) return null
        val inner = trace(ex, x, depth + 1) ?: return null
        return Traced(inner.result, listOf(Step("Multiply out", "Expand first: a sum is easier to integrate term by term.", eq(ex(e), ex(ex)))) + inner.steps)
    }

    /** A result from one of the integrator's other methods, named and explained. */
    private fun blackBox(e: Expr, x: Sym): Traced? {
        val found = runCatching { com.example.cas.cas.Integrals.identify(e, x) }.getOrNull()
        val f = found?.second ?: Calculus.integrate(e, x) ?: return null
        val (title, text) = found?.first?.let { describe(it, e, x) }
            ?: if (f.contains { it is Fn && it.name in SPECIAL }) "Special function" to "This has no antiderivative in elementary functions; it's written with a function defined by this very integral."
            else "Rewrite and integrate" to "Rewrite the integrand (identities, a substitution) and integrate the pieces."
        return Traced(f, listOf(Step(title, text, eq(int(e, x), f))))
    }

    /** The name and explanation of an integration method, with the substitution it makes where there is one. */
    private fun describe(method: String, e: Expr, x: Sym): Pair<String, String> {
        val v = x.name
        return when (method) {
            "rationalFull" -> "Partial fractions" to "Factor the denominator into linear and quadratic pieces and split the fraction; linear pieces give logarithms, quadratic ones ln and atan (after completing the square)."
            "gaussian" -> "Complete the square" to "The exponent is quadratic: write it as −a($v − h)² + k, so the integral becomes the Gaussian one, √π/2 · erf."
            "overLinear" -> "Special function" to "A sine, cosine or exponential over a linear term: substitute u = the linear term; the result is Si, Ci, Shi, Chi or Ei, functions defined by these integrals."
            "fresnel" -> "Fresnel integrals" to "sin or cos of a quadratic: complete the square, then u = √(2/π)($v − h) gives the Fresnel integrals S and C."
            "logIntegral" -> "Logarithmic integral" to "Substitute u = ln $v: ${v}ᵏ/ln $v becomes e^((k+1)u)/u, whose integral is Ei (li when k = 0)."
            "incompleteGamma" -> "Incomplete gamma function" to "Substitute u = a$v: ${v}ᵖ e^(−a$v) becomes uᵖ e^(−u), the integrand of Γ(p + 1, u)."
            "dilog" -> "Dilogarithm" to "A logarithm over a linear term: the integral of −ln(1 − u)/u defines Li₂(u)."
            "elliptic" -> "Elliptic integral" to "√(c₀ + c₁ sin²u): written as an elliptic integral F or E with parameter m = −c₁/c₀."
            "sqrtQuadratic" -> trigSubstitution(e, x)
            "sinCos" -> sinCosMethod(e, x)
            "expTrig" -> "By parts twice" to "Integrate eᵃ$v sin b$v by parts twice: the same integral comes back on the right, so solve for it."
            "quartic" -> "Factor the quartic" to "$v⁴ + p$v² + q = ($v² + s$v + r)($v² − s$v + r) with r = √q, s = √(2r − p); then partial fractions."
            "parts" -> "Integration by parts" to "u = the logarithm or inverse function (simpler once differentiated), dv = the rest: ∫ u dv = uv − ∫ v du."
            "multipleAngles" -> "Multiple angles" to "Write sin k$v and cos k$v with sin $v and cos $v (sin 2u = 2 sin u cos u, cos 2u = 2cos²u − 1, …), then integrate."
            "radical" -> radicalSubstitution(e, x)
            "powerSub" -> "Substitution u = ${v}ⁿ" to "${v}ⁿ⁻¹ d$v appears beside a function of ${v}ⁿ: with u = ${v}ⁿ, du = n${v}ⁿ⁻¹ d$v."
            "expSub" -> "Substitution u = eᵍ$v" to "Everything is a function of eᵍ$v: with u = eᵍ$v, d$v = du/(g u), the integrand becomes a rational function of u."
            "logSub" -> "Substitution $v = eᵘ" to "A function of ln $v: with u = ln $v, $v = eᵘ and d$v = eᵘ du."
            "productToSum" -> "Product-to-sum" to "sin A cos B = ½[sin(A + B) + sin(A − B)] (and the like) turn products of sines and cosines into single ones."
            "weierstrass" -> "Weierstrass substitution" to "t = tan(u/2): sin u = 2t/(1 + t²), cos u = (1 − t²)/(1 + t²), du = 2 dt/(1 + t²); a rational function of t, by partial fractions."
            "hyperbolic" -> "Exponential form" to "Write sinh, cosh and tanh with eᵘ and e⁻ᵘ, then integrate the exponentials."
            else -> "Rewrite and integrate" to "Rewrite the integrand and integrate the pieces."
        }
    }

    /** √Q, Q quadratic: complete the square, then x = h + r sin θ, r tan θ or r sec θ. */
    private fun trigSubstitution(e: Expr, x: Sym): Pair<String, String> {
        val v = x.name
        var found: Expr? = null
        fun walk(z: Expr) {
            if (z is Pow && z.exp is Num && !(z.exp as Num).q.isInteger && (z.exp as Num).q.den.toInt() == 2 && !z.base.freeOf(x)) found = z.base
            z.children.forEach { walk(it) }
        }
        walk(e)
        val q = found ?: return "Trigonometric substitution" to "Complete the square under the root and substitute a sine, tangent or secant."
        val cs = Algebra.coefficients(Algebra.expand(q), x)?.map { (it as? Num)?.q }
        if (cs == null || cs.size != 3 || cs.any { it == null }) return "Substitution" to "R($v, √Q): rationalize the root with a substitution."
        val (c0, c1, c2) = cs.map { it!! }
        val h = -c1 / (c2 * com.example.cas.math.Rational.of(2))
        val k = c0 - c1 * c1 / (c2 * com.example.cas.math.Rational.of(4))
        val shift = if (h.signum == 0) v else "$v ${if (h.signum > 0) "−" else "+"} ${Printer.plain(Num(h.abs()))}"
        val square = "${if (c2 == com.example.cas.math.Rational.ONE) "" else Printer.plain(Num(c2))}($shift)² ${if (k.signum >= 0) "+" else "−"} ${Printer.plain(Num(k.abs()))}"
        val r = Printer.plain(Algebra.simplify(com.example.cas.cas.sqrt(Num((k / c2).abs())))).replace("-", "−")
        val rk = Printer.plain(Algebra.simplify(com.example.cas.cas.sqrt(Num(k.abs()))))
        val (sub, identity, rest) = when {
            c2.signum < 0 && k.signum > 0 -> Triple("$shift = $r sin θ", "1 − sin²θ = cos²θ", "the root becomes $rk cos θ and d$v = $r cos θ dθ")
            c2.signum > 0 && k.signum > 0 -> Triple("$shift = $r tan θ", "1 + tan²θ = sec²θ", "the root becomes $rk sec θ and d$v = $r sec²θ dθ")
            c2.signum > 0 && k.signum < 0 -> Triple("$shift = $r sec θ", "sec²θ − 1 = tan²θ", "the root becomes $rk tan θ and d$v = $r sec θ tan θ dθ")
            else -> return "Substitution" to "Complete the square: $square."
        }
        val completed = if (c1.signum == 0) "" else "Complete the square: Q = $square. "
        return "Trigonometric substitution" to "${completed}Let $sub. Since $identity, $rest. Integrate in θ, then put θ back in terms of $v."
    }

    /** sinᵐ cosⁿ: an odd power gives a substitution, even powers the half-angle formulas. */
    private fun sinCosMethod(e: Expr, x: Sym): Pair<String, String> {
        var m = 0; var n = 0
        fun walk(z: Expr) {
            when {
                z is Fn && z.name == "sin" -> m = maxOf(m, 1)
                z is Fn && z.name == "cos" -> n = maxOf(n, 1)
                z is Pow && z.base is Fn && z.exp is Num -> {
                    val k = (z.exp as Num).q.num.toInt()
                    if ((z.base as Fn).name == "sin") m = k else if ((z.base as Fn).name == "cos") n = k
                }
            }
            z.children.forEach { walk(it) }
        }
        walk(e)
        return when {
            m % 2 != 0 && m > 0 -> "Odd power of sine" to "Keep one sin u, write the rest with sin²u = 1 − cos²u, then substitute w = cos u (dw = −sin u du)."
            n % 2 != 0 && n > 0 -> "Odd power of cosine" to "Keep one cos u, write the rest with cos²u = 1 − sin²u, then substitute w = sin u (dw = cos u du)."
            else -> "Half-angle formulas" to "Even powers: sin²u = (1 − cos 2u)/2 and cos²u = (1 + cos 2u)/2 lower the powers until each piece integrates directly."
        }
    }

    /** (ax + b)^(p/q) inside: u = (ax + b)^(1/q). */
    private fun radicalSubstitution(e: Expr, x: Sym): Pair<String, String> {
        var found: Pow? = null
        fun walk(z: Expr) {
            if (z is Pow && z.exp is Num && !(z.exp as Num).q.isInteger && !z.base.freeOf(x)) found = z
            z.children.forEach { walk(it) }
        }
        walk(e)
        val p = found ?: return "Substitution" to "Substitute the root to get a rational function."
        val q = (p.exp as Num).q.den
        return "Rationalizing substitution" to "Let u = (${Printer.plain(p.base)})^(1/$q), so ${Printer.plain(p.base)} = u^$q and every root becomes a power of u; the integrand is then a rational function of u."
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

    // ---- Derivatives -------------------------------------------------------------------------------

    /** d/d[x] of [body] as the editor writes it. */
    private fun d(body: Expr, x: Sym): MathRow =
        MathRow(mutableListOf(com.example.cas.editor.Derivative(MathRow(mutableListOf(sym(x.name))), ex(body))))

    private fun derivative(n: com.example.cas.editor.Derivative, angle: AngleUnit): Solution? {
        val name = n.variable.items.joinToString("") { (it as? SymNode)?.text ?: "" }
        if (name.length != 1) return null
        val x = Sym(name)
        val ev = { r: MathRow -> Evaluator(angle).evaluate(r) }
        val body = ev(n.body)
        val order = if (n.order.isEmpty) 1 else ((ev(n.order) as? Num)?.q?.num?.toInt() ?: return null)
        if (order !in 1..6) return null
        val question = MathRow(mutableListOf(n)).let { com.example.cas.editor.MathCodec.copy(it) }
        val value = Evaluator(angle).evaluate(com.example.cas.editor.MathCodec.copy(question))
        val steps = ArrayList<Step>()
        val traced = dtrace(body, x, 0)
        steps += traced.steps
        var current = Algebra.simplify(Calculus.diff(body, x))
        if (!same(traced.result, current)) steps += Step("Simplify", null, eq(traced.result, current))
        for (k in 2..order) {
            val next = Algebra.simplify(Calculus.diff(current, x))
            steps += Step("Differentiate again", "The ${ordinal(k)} derivative is the derivative of the ${ordinal(k - 1)}.", eq(d(current, x), next))
            current = next
        }
        if (!n.at.isEmpty) {
            val a = ev(n.at)
            steps += Step("Evaluate", "Put ${x.name} = ${Printer.plain(a)} into the derivative.", line(current, ",", " ", x.name, "=", a, "  ", "⇒", " ", value))
        }
        val answer = line(question, "=", value)
        steps += Step("Answer", null, answer, Kind.Result)
        return Solution(methodOf(steps), steps, answer)
    }

    /** The steps to d/d[x] of [e], rule by rule. */
    private fun dtrace(e: Expr, x: Sym, depth: Int): Traced {
        val result = Calculus.diff(e, x)
        fun leaf(title: String, text: String) = Traced(result, listOf(Step(title, text, eq(d(e, x), result))))
        if (depth > 4) return leaf("Differentiate", "Rule by rule, as above.")
        if (e.freeOf(x)) return leaf("Constant rule", "The derivative of a constant is 0.")
        if (e == x) return leaf("Power rule", "d/d${x.name} ${x.name} = 1.")
        if (e is Add) {
            val parts = e.terms.map { dtrace(it, x, depth + 1) }
            val split = line(*e.terms.flatMapIndexed { k, t -> (if (k == 0) emptyList() else listOf<Any>("+")) + listOf<Any>(d(t, x)) }.toTypedArray())
            return Traced(result, listOf(Step("Sum rule", "Differentiate term by term.", eq(d(e, x), split), substeps = parts.flatMap { it.steps })))
        }
        if (e is Mul) {
            val (consts, rest) = e.factors.partition { it.freeOf(x) }
            if (consts.isNotEmpty()) {
                val c = mul(consts); val r = mul(rest)
                val inner = dtrace(r, x, depth + 1)
                return Traced(result, listOf(Step("Constant multiple", "Keep ${Printer.plain(c)} in front.", eq(d(e, x), line(paren(c), d(r, x))), substeps = inner.steps)))
            }
            // A quotient u/v: a factor with a negative power.
            val den = rest.filter { it is Pow && it.exp is Num && (it.exp as Num).q.signum < 0 }
            if (den.isNotEmpty() && den.size < rest.size) {
                val u = mul(rest - den.toSet())
                val v = mul(den.map { com.example.cas.cas.pow((it as Pow).base, neg(it.exp)) })
                val du = dtrace(u, x, depth + 1); val dv = dtrace(v, x, depth + 1)
                return Traced(result, listOf(Step(
                    "Quotient rule", "(u/v)′ = (u′v − uv′)/v², with u = ${Printer.plain(u)} and v = ${Printer.plain(v)}.",
                    eq(d(e, x), ex(div(sub(mul(du.result, v), mul(u, dv.result)), com.example.cas.cas.pow(v, com.example.cas.cas.TWO)))),
                    substeps = du.steps + dv.steps,
                )))
            }
            val u = rest.first(); val v = mul(rest.drop(1))
            val du = dtrace(u, x, depth + 1); val dv = dtrace(v, x, depth + 1)
            return Traced(result, listOf(Step(
                "Product rule", "(uv)′ = u′v + uv′, with u = ${Printer.plain(u)} and v = ${Printer.plain(v)}.",
                eq(d(e, x), ex(add(mul(du.result, v), mul(u, dv.result)))),
                substeps = du.steps + dv.steps,
            )))
        }
        if (e is Pow) {
            val (b, k) = e.base to e.exp
            if (k.freeOf(x)) {
                if (b == x) return leaf("Power rule", "d/d${x.name} ${x.name}ⁿ = n ${x.name}ⁿ⁻¹, with n = ${Printer.plain(k)}.")
                return chain("Power rule", "(uⁿ)′ = n uⁿ⁻¹ · u′, with n = ${Printer.plain(k)}", e, b, x, depth, result)
            }
            if (b.freeOf(x)) {
                val rule = if (b == E) "(eᵘ)′ = eᵘ · u′" else "(aᵘ)′ = aᵘ ln a · u′"
                return if (k == x) leaf("Exponential", rule.replace(" · u′", "").replace("u", x.name)) else chain("Exponential", rule, e, k, x, depth, result)
            }
            return leaf("Logarithmic differentiation", "For f^g write e^(g ln f): (f^g)′ = f^g (g′ ln f + g f′/f).")
        }
        if (e is Fn && e.args.size == 1 && e.name in DTABLE) {
            val u = e.args[0]
            return if (u == x) leaf("Standard derivative", DTABLE.getValue(e.name).replace("u", x.name).replace(" · ${x.name}′", ""))
            else chain("Standard derivative", DTABLE.getValue(e.name), e, u, x, depth, result)
        }
        return leaf("Differentiate", "Using the derivative of each function involved.")
    }

    /** f(u(x)) by the chain rule, with u′ worked out as a smaller step. */
    private fun chain(title: String, rule: String, e: Expr, u: Expr, x: Sym, depth: Int, result: Expr): Traced {
        val du = dtrace(u, x, depth + 1)
        return Traced(result, listOf(Step(
            "$title and chain rule", "$rule, with u = ${Printer.plain(u)}.", eq(d(e, x), result),
            substeps = du.steps,
        )))
    }

    private val DTABLE = mapOf(
        "sin" to "(sin u)′ = cos u · u′", "cos" to "(cos u)′ = −sin u · u′", "tan" to "(tan u)′ = (1 + tan²u) · u′",
        "ln" to "(ln u)′ = u′/u", "sinh" to "(sinh u)′ = cosh u · u′", "cosh" to "(cosh u)′ = sinh u · u′",
        "asin" to "(asin u)′ = u′/√(1 − u²)", "acos" to "(acos u)′ = −u′/√(1 − u²)", "atan" to "(atan u)′ = u′/(1 + u²)",
        "tanh" to "(tanh u)′ = (1 − tanh²u) · u′", "abs" to "|u|′ = (u/|u|) · u′", "erf" to "(erf u)′ = (2/√π) e^(−u²) · u′",
    )

    // ---- Limits ---------------------------------------------------------------------------------------

    private fun lim(body: Expr, x: Sym, a: Expr, side: Int): MathRow {
        val spec = line(x.name, "→", a).also { if (side != 0) it.add(com.example.cas.editor.Pow(MathRow(mutableListOf(sym(if (side > 0) "+" else "−"))))) }
        return MathRow(mutableListOf(Func("lim", listOf(ex(body), spec))))
    }

    private fun limit(n: Func, angle: AngleUnit): Solution? {
        if (n.args.size != 2) return null
        val spec = n.args[1].items
        val last = spec.lastOrNull()
        val raised = (last as? com.example.cas.editor.Pow)?.exp?.items?.singleOrNull()
        val lastSym = ((raised ?: last) as? SymNode)?.text
        val side = when { spec.size > 3 && lastSym == "+" -> 1; spec.size > 3 && lastSym == "−" -> -1; else -> 0 }
        val bare = if (side != 0 || (last is com.example.cas.editor.Pow && last.exp.isEmpty)) spec.dropLast(1) else spec
        val arrow = bare.indexOfFirst { (it as? SymNode)?.text == "→" }
        if (arrow != 1) return null
        val x = Sym((bare[0] as? SymNode)?.text ?: return null)
        val a = Evaluator(angle).evaluate(MathRow(bare.drop(2).toMutableList()))
        val body = Evaluator(angle).evaluate(n.args[0])
        val question = com.example.cas.editor.MathCodec.copy(MathRow(mutableListOf(n)))
        val value = Evaluator(angle).evaluate(com.example.cas.editor.MathCodec.copy(question))
        val answer = line(question, "=", value)
        val steps = ArrayList<Step>()
        val infinite = Calculus.isInfinite(a)
        // Straight substitution, when it gives a number.
        if (!infinite) {
            val direct = runCatching { Algebra.simplify(body.subst(x, a)) }.getOrNull()
            if (direct != null && finiteValue(direct)) {
                steps += Step("Direct substitution", "f is continuous at ${x.name} = ${Printer.plain(a)}: put it in.", eq(lim(body, x, a, side), direct))
                steps += Step("Answer", null, answer, Kind.Result)
                return Solution("Direct substitution", steps, answer)
            }
        }
        val (num, den) = Algebra.together(body)
        val ln = runCatching { Calculus.limit(num, x, a, side) }.getOrNull()
        val ld = runCatching { Calculus.limit(den, x, a, side) }.getOrNull()
        val form = when {
            ln == null || ld == null || den.freeOf(x) -> null
            ln == ZERO && ld == ZERO -> "0/0"
            Calculus.isInfinite(ln) && Calculus.isInfinite(ld) -> "∞/∞"
            else -> null
        }
        // Rational function at ∞: the highest powers decide.
        if (infinite && form != null && Algebra.qpoly(num, x) != null && Algebra.qpoly(den, x) != null) {
            val p = Algebra.qpoly(num, x)!!; val q = Algebra.qpoly(den, x)!!
            steps += Step("Highest powers", "Divide the numerator and the denominator by ${x.name}^${q.degree}: only the leading terms survive.",
                line(ex(com.example.cas.cas.mul(Num(p.lead), com.example.cas.cas.pow(x, p.degree.toLong()))), "/", ex(com.example.cas.cas.mul(Num(q.lead), com.example.cas.cas.pow(x, q.degree.toLong())))))
            steps += Step("Answer", null, answer, Kind.Result)
            return Solution("Highest powers", steps, answer)
        }
        if (form != null) {
            steps += Step("Indeterminate form", "Substituting gives $form, which could be anything: work further.", ex(div(num, den)), Kind.Note)
            // L'Hôpital: differentiate top and bottom, up to three times.
            var p = num; var q = den
            for (round in 1..3) {
                p = Algebra.simplify(Calculus.diff(p, x)); q = Algebra.simplify(Calculus.diff(q, x))
                val quotient = Algebra.simplify(div(p, q))
                val got = runCatching { if (infinite) Calculus.limit(quotient, x, a, side) else Algebra.simplify(quotient.subst(x, a)) }.getOrNull()
                steps += Step(
                    "L'Hôpital's rule" + if (round > 1) " again" else "",
                    "Differentiate the numerator and the denominator separately.",
                    if (round == 1) line(lim(body, x, a, side), "=", lim(div(p, q), x, a, side)) else line("=", lim(div(p, q), x, a, side)),
                )
                if (got != null && finiteOrInfinite(got) && sameValue(got, value)) {
                    steps += Step("Substitute", "Now substitution works.", line(lim(div(p, q), x, a, side), "=", got))
                    steps += Step("Answer", null, answer, Kind.Result)
                    return Solution("L'Hôpital's rule", steps, answer)
                }
            }
            steps.removeAll { it.title.startsWith("L'Hôpital") }
        }
        // Otherwise the series around the point (as the calculator does).
        if (!infinite) runCatching { Calculus.taylor(body, x, a, 4) }.getOrNull()?.let { t ->
            steps += Step("Series expansion", "Expand around ${x.name} = ${Printer.plain(a)}; the leading term gives the limit.", eq(ex(body), line(t, "+", "…")))
            steps += Step("Answer", null, answer, Kind.Result)
            return Solution("Series expansion", steps, answer)
        }
        steps += Step("Limit", "From the leading behavior of each part as ${x.name} → ${Printer.plain(a)}.", answer, Kind.Result)
        return Solution("Leading behavior", steps, answer)
    }

    private fun finiteValue(e: Expr) = runCatching { Numeric.eval(e) }.getOrNull()?.let { it.re.isFinite() && it.im.isFinite() } == true
    private fun finiteOrInfinite(e: Expr) = Calculus.isInfinite(e) || finiteValue(e)
    private fun sameValue(a: Expr, b: Expr): Boolean {
        if (Calculus.isInfinite(a) || Calculus.isInfinite(b)) return same(a, b)
        val u = runCatching { Numeric.eval(a) }.getOrNull() ?: return false
        val v = runCatching { Numeric.eval(b) }.getOrNull() ?: return false
        return (u - v).abs() < 1e-9 * maxOf(1.0, v.abs())
    }

    // ---- Sums -------------------------------------------------------------------------------------------

    private fun sigma(body: Expr, k: Sym, lo: Expr, hi: Expr): MathRow = MathRow(mutableListOf(
        com.example.cas.editor.BigOp(com.example.cas.editor.BigOpKind.Sum, MathRow(mutableListOf(sym(k.name))), ex(lo), ex(hi), ex(body)),
    ))

    private fun sum(n: com.example.cas.editor.BigOp, angle: AngleUnit): Solution? {
        val name = n.variable.items.joinToString("") { (it as? SymNode)?.text ?: "" }
        if (name.length != 1) return null
        val k = Sym(name)
        val ev = { r: MathRow -> Evaluator(angle).evaluate(r) }
        val body = ev(n.body); val lo = ev(n.lower); val hi = ev(n.upper)
        val question = com.example.cas.editor.MathCodec.copy(MathRow(mutableListOf(n)))
        val value = Evaluator(angle).evaluate(com.example.cas.editor.MathCodec.copy(question))
        val answer = line(question, "=", value)
        val steps = ArrayList<Step>()
        val count = (hi as? Num)?.q?.takeIf { it.isInteger }?.let { h -> (lo as? Num)?.q?.takeIf { it.isInteger }?.let { l -> (h - l).num.toLong() + 1 } }
        // A few terms: write them out.
        if (count != null && count in 1..8) {
            val terms = (0 until count).map { j -> Algebra.simplify(body.subst(k, add(lo, Num(j)))) }
            steps += Step("Write out the terms", "${k.name} = ${Printer.plain(lo)}, …, ${Printer.plain(hi)}.", line(*terms.flatMapIndexed { j, t -> (if (j > 0) listOf<Any>("+") else emptyList()) + listOf<Any>(paren(t)) }.toTypedArray()))
            steps += Step("Add them up", null, answer, Kind.Result)
            return Solution("Adding the terms", steps, answer)
        }
        // Infinite series of known kinds.
        if (Calculus.isPlusInfinity(hi)) {
            geometric(body, k)?.let { r ->
                steps += Step("Geometric series", "Each term is the last times r = ${Printer.plain(r)}.", ex(r))
                steps += Step("Converges", "|r| < 1, so Σ a rᵏ = (first term)/(1 − r).", null, Kind.Check)
                steps += Step("Answer", null, answer, Kind.Result)
                return Solution("Geometric series", steps, answer)
            }
            pSeries(body, k)?.let { (p, alternating) ->
                val pText = Printer.plain(p)
                steps += Step(if (alternating) "Alternating p-series" else "p-series", "The term is ${if (alternating) "±" else ""}1/${k.name}^$pText.", ex(body))
                steps += Step("Converges", if (alternating) "The terms shrink to 0 and alternate in sign." else "p = $pText > 1, so the series converges.", null, Kind.Check)
                steps += Step(
                    if (alternating) "Dirichlet eta function" else "Riemann zeta function",
                    if (alternating) "Σ (−1)^(${k.name}+1)/${k.name}^p = η(p), and η(1) = ln 2, η(p) = (1 − 2^(1−p)) ζ(p)." else "Σ 1/${k.name}^p from 1 is ζ(p); ζ(2) = π²/6, ζ(4) = π⁴/90.",
                    answer, Kind.Result,
                )
                return Solution(if (alternating) "Eta function" else "Zeta function", steps, answer)
            }
            steps += Step("Closed form", "Recognized from the form of the term.", answer, Kind.Result)
            return Solution("Known series", steps, answer)
        }
        // A polynomial in k up to n: linearity and the power sums.
        val cs = Algebra.coefficients(Algebra.expand(body), k)
        if (cs != null && cs.size <= 4) {
            val pieces = cs.mapIndexedNotNull { p, c -> if (c == ZERO) null else Triple(p, c, POWER_SUMS[p]) }
            steps += Step("Linearity", "Split the sum and take constants out.",
                line(sigma(body, k, lo, hi), "=", *pieces.flatMapIndexed { j, (p, c, _) -> (if (j > 0) listOf<Any>("+") else emptyList()) + listOf<Any>(paren(c), sigma(com.example.cas.cas.pow(k, p.toLong()), k, lo, hi)) }.toTypedArray()))
            if (lo != com.example.cas.cas.ONE) steps += Step("Shift", "The formulas below start at ${k.name} = 1: subtract the terms before ${Printer.plain(lo)}.", null, Kind.Note)
            pieces.forEach { (p, _, formula) -> steps += Step("Sum of ${if (p == 0) "a constant" else k.name + if (p > 1) "^$p" else ""}", formula, null) }
            steps += Step("Simplify", "Put the pieces together and factor.", answer, Kind.Result)
            return Solution("Power sums", steps, answer)
        }
        geometric(body, k)?.let { r ->
            steps += Step("Geometric sum", "Ratio r = ${Printer.plain(r)}: Σ a rᵏ = a (r^(n+1) − 1)/(r − 1) from 0 to n.", ex(r))
            steps += Step("Answer", null, answer, Kind.Result)
            return Solution("Geometric sum", steps, answer)
        }
        steps += Step("Closed form", "Found from the form of the term.", answer, Kind.Result)
        return Solution("Closed form", steps, answer)
    }

    private val POWER_SUMS = listOf(
        "Σ 1 from 1 to n = n", "Σ k from 1 to n = n(n + 1)/2", "Σ k² from 1 to n = n(n + 1)(2n + 1)/6", "Σ k³ from 1 to n = (n(n + 1)/2)²",
    )

    /** r when the term is a·rᵏ (r constant), else null. */
    private fun geometric(body: Expr, k: Sym): Expr? {
        val r = runCatching { Algebra.simplify(div(body.subst(k, add(k, com.example.cas.cas.ONE)), body)) }.getOrNull() ?: return null
        if (!r.freeOf(k) || r == com.example.cas.cas.ONE) return null
        val v = runCatching { Numeric.eval(r) }.getOrNull() ?: return null
        return r.takeIf { v.abs() < 1 || !body.freeOf(k) }
    }

    /** (p, alternating) when the term is 1/kᵖ or (−1)^(k±1)/kᵖ. */
    private fun pSeries(body: Expr, k: Sym): Pair<Expr, Boolean>? {
        val fs = if (body is Mul) body.factors else listOf(body)
        var p: Expr? = null; var alternating = false
        for (f in fs) when {
            f is Pow && f.base == k && f.exp.freeOf(k) -> p = neg(f.exp)
            f is Pow && f.base == MINUS_ONE -> alternating = true
            f is Num -> {}
            else -> return null
        }
        return p?.let { it to alternating }
    }

    // ---- Products ---------------------------------------------------------------------------------------

    private fun pi(body: Expr, k: Sym, lo: Expr, hi: Expr): MathRow = MathRow(mutableListOf(
        com.example.cas.editor.BigOp(com.example.cas.editor.BigOpKind.Product, MathRow(mutableListOf(sym(k.name))), ex(lo), ex(hi), ex(body)),
    ))

    private fun product(n: com.example.cas.editor.BigOp, angle: AngleUnit): Solution? {
        val name = n.variable.items.joinToString("") { (it as? SymNode)?.text ?: "" }
        if (name.length != 1) return null
        val k = Sym(name)
        val ev = { r: MathRow -> Evaluator(angle).evaluate(r) }
        val body = ev(n.body); val lo = ev(n.lower); val hi = ev(n.upper)
        val question = com.example.cas.editor.MathCodec.copy(MathRow(mutableListOf(n)))
        val value = Evaluator(angle).evaluate(com.example.cas.editor.MathCodec.copy(question))
        val answer = line(question, "=", value)
        val steps = ArrayList<Step>()
        val count = (hi as? Num)?.q?.takeIf { it.isInteger }?.let { h -> (lo as? Num)?.q?.takeIf { it.isInteger }?.let { l -> (h - l).num.toLong() + 1 } }
        if (count != null && count in 1..8) {
            val fs = (0 until count).map { j -> Algebra.simplify(body.subst(k, add(lo, Num(j)))) }
            steps += Step("Write out the factors", "${k.name} = ${Printer.plain(lo)}, …, ${Printer.plain(hi)}.", line(*fs.flatMapIndexed { j, t -> (if (j > 0) listOf<Any>("·") else emptyList()) + listOf<Any>(paren(t)) }.toTypedArray()))
            steps += Step("Multiply them", null, answer, Kind.Result)
            return Solution("Multiplying the factors", steps, answer)
        }
        if (body.freeOf(k) && !Calculus.isInfinite(hi)) {
            val m = Algebra.simplify(add(sub(hi, lo), com.example.cas.cas.ONE))
            steps += Step("Constant factor", "The same factor ${Printer.plain(body)}, ${Printer.plain(m)} times.", ex(com.example.cas.cas.pow(body, m)))
            steps += Step("Answer", null, answer, Kind.Result)
            return Solution("Powers", steps, answer)
        }
        if (body == k && lo == com.example.cas.cas.ONE) {
            steps += Step("Factorial", "1 · 2 · 3 ⋯ ${Printer.plain(hi)} is ${Printer.plain(hi)}!.", answer, Kind.Result)
            return Solution("Factorial", steps, answer)
        }
        // Telescoping: each factor is g(k + 1)/g(k), so everything cancels but the ends.
        val (num, den) = Algebra.together(body)
        val shifted = runCatching { Algebra.simplify(den.subst(k, add(k, com.example.cas.cas.ONE))) }.getOrNull()
        if (!den.freeOf(k) && shifted != null && runCatching { Algebra.simplify(sub(num, shifted)) }.getOrNull() == ZERO) {
            steps += Step("Telescoping", "Each factor is g(${k.name} + 1)/g(${k.name}) with g(${k.name}) = ${Printer.plain(den)}: neighbours cancel.", eq(ex(body), ex(div(shifted, den))))
            steps += Step("What's left", "Only the last numerator and the first denominator remain: g(${Printer.plain(add(hi, com.example.cas.cas.ONE))})/g(${Printer.plain(lo)}).", null)
            steps += Step("Answer", null, answer, Kind.Result)
            return Solution("Telescoping product", steps, answer)
        }
        if (Calculus.isPlusInfinity(hi)) {
            steps += Step("Take logarithms", "ln of the product is Σ ln(factor): a product converges when that sum does.", line("ln", pi(body, k, lo, hi), "=", sigma(com.example.cas.cas.fn("ln", body), k, lo, hi)), Kind.Note)
            steps += Step("Answer", null, answer, Kind.Result)
            return Solution("Infinite product", steps, answer)
        }
        steps += Step("Closed form", "Found from the form of the factor.", answer, Kind.Result)
        return Solution("Closed form", steps, answer)
    }

    // ---- Residues ---------------------------------------------------------------------------------------

    /** "z = a" as (z, a). */
    private fun point(r: MathRow, angle: AngleUnit): Pair<Sym, Expr>? {
        val name = (r.items.firstOrNull() as? SymNode)?.text ?: return null
        if (name.length != 1 || r.items.size < 3 || (r.items[1] as? SymNode)?.text !in setOf("=", "→")) return null
        return Sym(name) to Evaluator(angle).evaluate(MathRow(r.items.drop(2).toMutableList()))
    }

    private fun residue(n: Func, angle: AngleUnit): Solution? {
        if (n.args.size != 2) return null
        val (z, a) = point(n.args[1], angle) ?: return null
        val f = Evaluator(angle).evaluate(n.args[0])
        val question = com.example.cas.editor.MathCodec.copy(MathRow(mutableListOf(n)))
        val value = Evaluator(angle).evaluate(com.example.cas.editor.MathCodec.copy(question))
        val answer = line(question, "=", value)
        val steps = ArrayList<Step>()
        val (_, den) = Algebra.together(f)
        val atPole = !den.freeOf(z) && runCatching { Numeric.eval(Algebra.simplify(den.subst(z, a))) }.getOrNull()?.let { it.abs() < 1e-12 } == true
        if (!atPole) {
            steps += Step("Not a pole", "The denominator isn't 0 at ${z.name} = ${Printer.plain(a)} (or f is analytic there), so the residue is 0 unless f has an essential singularity.", answer, Kind.Result)
            return Solution("Analytic point", steps, answer)
        }
        val m = orderOf(den, z, a)
        steps += Step("Order of the pole", "${Printer.plain(a)} is a root of the denominator ${m} time${if (m > 1) "s" else ""}: a pole of order $m.", ex(den))
        val g = Algebra.simplify(mul(com.example.cas.cas.pow(sub(z, a), Num(m.toLong())), f))
        steps += Step("Remove the pole", "Multiply by (${z.name} − ${Printer.plain(a)})${if (m > 1) "^$m" else ""}.", ex(g))
        if (m == 1) {
            steps += Step("Simple pole", "Res = lim (${z.name} − a) f(${z.name}): put ${z.name} = ${Printer.plain(a)} in.", answer, Kind.Result)
        } else {
            var dg = g
            repeat(m - 1) { dg = Algebra.simplify(Calculus.diff(dg, z)) }
            steps += Step("Differentiate ${m - 1} time${if (m > 2) "s" else ""}", "Res = 1/(${m - 1})! · the limit of the ${ordinal(m - 1)} derivative.", ex(dg))
            steps += Step("Divide by (${m - 1})!", null, answer, Kind.Result)
        }
        return Solution(if (m == 1) "Simple pole" else "Pole of order $m", steps, answer)
    }

    // ---- Taylor series ------------------------------------------------------------------------------

    private fun taylor(n: Func, angle: AngleUnit): Solution? {
        if (n.args.size != 3) return null
        val (x, a) = point(n.args[1], angle) ?: return null
        val order = (Evaluator(angle).evaluate(n.args[2]) as? Num)?.q?.num?.toInt() ?: return null
        if (order !in 0..10) return null
        val f = Evaluator(angle).evaluate(n.args[0])
        val question = com.example.cas.editor.MathCodec.copy(MathRow(mutableListOf(n)))
        val value = Evaluator(angle).evaluate(com.example.cas.editor.MathCodec.copy(question))
        val answer = line(question, "=", value)
        val derivatives = ArrayList<Step>()
        var g = f
        val coefficients = ArrayList<Expr>()
        var fact = com.example.cas.cas.ONE as Expr
        for (k in 0..order) {
            if (k > 0) { g = Algebra.simplify(Calculus.diff(g, x)); fact = mul(fact, Num(k.toLong())) }
            val at = runCatching { Algebra.simplify(g.subst(x, a)) }.getOrNull()?.takeIf { finiteValue(it) } ?: Calculus.limit(g, x, a)
            derivatives += Step("f${"′".repeat(minOf(k, 3)).ifEmpty { "" }}${if (k > 3) "⁽$k⁾" else ""}(${Printer.plain(a)})", null, line(ex(g), "  ", "→", " ", at))
            coefficients += Algebra.simplify(div(at, fact))
        }
        val steps = ArrayList<Step>()
        steps += Step("Derivatives at ${x.name} = ${Printer.plain(a)}", "Differentiate $order times and put ${x.name} = ${Printer.plain(a)} in each.", null, substeps = derivatives)
        steps += Step("Coefficients", "Divide the k-th derivative by k!.", line(*coefficients.flatMapIndexed { k, c -> (if (k > 0) listOf<Any>(",", " ") else emptyList()) + listOf<Any>(c) }.toTypedArray()))
        steps += Step("Taylor's formula", "f(${x.name}) ≈ Σ f⁽ᵏ⁾(a)/k! (${x.name} − a)ᵏ, up to k = $order.", answer, Kind.Result)
        return Solution("Taylor series", steps, answer)
    }

    // ---- Determinants --------------------------------------------------------------------------------

    private fun determinant(n: Func, angle: AngleUnit): Solution? {
        val mat = Evaluator(angle).evaluate(n.args.firstOrNull() ?: return null) as? com.example.cas.cas.Mat ?: return null
        if (mat.rows != mat.cols || mat.rows !in 2..3) return null
        val question = com.example.cas.editor.MathCodec.copy(MathRow(mutableListOf(n)))
        val value = Evaluator(angle).evaluate(com.example.cas.editor.MathCodec.copy(question))
        val answer = line(question, "=", value)
        val c = mat.cells
        val steps = ArrayList<Step>()
        if (mat.rows == 2) {
            steps += Step("2 × 2 formula", "det = ad − bc: the diagonal product minus the other one.", line(paren(c[0]), "·", paren(c[3]), "−", paren(c[1]), "·", paren(c[2])))
            steps += Step("Answer", null, answer, Kind.Result)
            return Solution("ad − bc", steps, answer)
        }
        val minors = (0 until 3).map { j ->
            val cells = ArrayList<Expr>()
            for (r in 1 until 3) for (k in 0 until 3) if (k != j) cells += c[r * 3 + k]
            val d = Algebra.simplify(sub(mul(cells[0], cells[3]), mul(cells[1], cells[2])))
            Step("Minor of ${Printer.plain(c[j])}", "Cross out row 1 and column ${j + 1}; its 2 × 2 determinant.", line(ex(com.example.cas.cas.Mat(2, 2, cells)), "→", d)) to d
        }
        steps += Step("Cofactor expansion", "Along the first row, with signs + − +.",
            line(paren(c[0]), "·", paren(minors[0].second), "−", paren(c[1]), "·", paren(minors[1].second), "+", paren(c[2]), "·", paren(minors[2].second)),
            substeps = minors.map { it.first })
        steps += Step("Answer", null, answer, Kind.Result)
        return Solution("Cofactor expansion", steps, answer)
    }

    // ---- Complex arithmetic -----------------------------------------------------------------------------

    private fun complex(row: MathRow, angle: AngleUnit): Solution? {
        val value = Evaluator(angle).evaluate(com.example.cas.editor.MathCodec.copy(row))
        val answer = line(row, "=", value)
        val steps = ArrayList<Step>()
        val single = row.items.singleOrNull()
        if (single is com.example.cas.editor.Frac && single.den.items.any { it is SymNode && it.text == "i" }) {
            val top = Evaluator(angle).evaluate(single.num)
            val bottom = Evaluator(angle).evaluate(single.den)
            val (a, b) = com.example.cas.cas.ComplexArith.split(bottom) ?: return null
            val conj = add(a, mul(neg(b), com.example.cas.cas.I))
            val norm = Algebra.simplify(add(com.example.cas.cas.pow(a, com.example.cas.cas.TWO), com.example.cas.cas.pow(b, com.example.cas.cas.TWO)))
            val numerator = com.example.cas.cas.ComplexArith.normalize(mul(top, conj))
            steps += Step("Multiply by the conjugate", "Multiply the top and the bottom by ${Printer.plain(conj)}: the bottom becomes real.", MathRow(mutableListOf(com.example.cas.editor.Frac(line("(", top, ")", "(", conj, ")"), line("(", bottom, ")", "(", conj, ")")))))
            steps += Step("The bottom", "(a + bi)(a − bi) = a² + b².", ex(norm))
            steps += Step("The top", "Multiply out, with i² = −1.", ex(numerator))
            steps += Step("Answer", "Divide each part by ${Printer.plain(norm)}.", answer, Kind.Result)
            return Solution("Complex division", steps, answer)
        }
        val euler = row.items.any { it is SymNode && it.text == "e" } && row.items.any { n -> n is com.example.cas.editor.Pow && n.exp.items.any { it is SymNode && it.text == "i" } }
        if (euler) {
            steps += Step("Euler's formula", "e^(iθ) = cos θ + i sin θ, and eˣ⁺ⁱʸ = eˣ (cos y + i sin y).", null)
            steps += Step("Exact values", "Use the exact cosine and sine of the angle, then collect real and imaginary parts.", answer, Kind.Result)
            return Solution("Euler's formula", steps, answer)
        }
        steps += Step("Multiply out", "Expand the products and powers like ordinary algebra.", null)
        steps += Step("Use i² = −1", "Every i² becomes −1 (and i³ = −i, i⁴ = 1).", null)
        steps += Step("Collect", "Real parts together, imaginary parts together: a + bi.", answer, Kind.Result)
        return Solution("Complex arithmetic", steps, answer)
    }

    /** The working as plain text, for copying. */
    fun text(question: MathRow, s: Solution): String = buildString {
        appendLine(Formatter.plain(question))
        appendLine("Method: ${s.method}")
        var n = 0
        for (st in s.steps) {
            n++
            append("$n. ${st.title}")
            st.text?.let { append(" — $it") }
            appendLine()
            st.math?.let { appendLine("   " + Formatter.plain(it)) }
            for ((k, sub) in flattenSubsteps(st.substeps).withIndex()) {
                append("   ${'a' + k}) ${sub.title}")
                sub.text?.let { append(" — $it") }
                appendLine()
                sub.math?.let { appendLine("      " + Formatter.plain(it)) }
            }
        }
    }

    private fun flattenSubsteps(steps: List<Step>): List<Step> = steps.flatMap { listOf(it) + flattenSubsteps(it.substeps) }
}
