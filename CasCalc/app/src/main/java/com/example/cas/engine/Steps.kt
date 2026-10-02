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
    private val FUNCS = setOf(
        "contour", "lim", "residue", "taylor", "det", "eigvals", "eigvecs", "charpoly", "rref", "dsolve",
        "mean", "median", "sd", "psd", "var", "grad", "div", "curl", "jacobian", "hessian",
    )

    /** A matrix raised to −1. */
    private fun inverseOf(row: MathRow): com.example.cas.editor.Matrix? {
        val items = row.items.filter { !(it is SymNode && it.text.isBlank()) }
        if (items.size != 2) return null
        val m = items[0] as? com.example.cas.editor.Matrix ?: return null
        val pw = items[1] as? com.example.cas.editor.Pow ?: return null
        return m.takeIf { pw.exp.items.joinToString("") { n -> (n as? SymNode)?.text ?: "?" } == "−1" }
    }

    fun supports(row: MathRow): Boolean = target(row) != null || inverseOf(row) != null || isComplexArithmetic(row) || simplificationMoves(row, AngleUnit.Radians).isNotEmpty()

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
                (it is Func && it.name in FUNCS)
        }

    /** The working for [row], or null if there is none (or it couldn't be found). */
    fun of(row: MathRow, angle: AngleUnit = AngleUnit.Radians): Solution? = runCatching {
        inverseOf(row)?.let { return@runCatching inverse(row, angle) }
        when (val n = target(row)) {
            is Integral -> integral(n, angle)
            is com.example.cas.editor.Derivative -> derivative(n, angle)
            is com.example.cas.editor.BigOp -> if (n.kind == com.example.cas.editor.BigOpKind.Sum) sum(n, angle) else product(n, angle)
            is Func -> when (n.name) {
                "lim" -> limit(n, angle)
                "residue" -> residue(n, angle)
                "taylor" -> taylor(n, angle)
                "det" -> determinant(n, angle)
                "eigvals", "eigvecs", "charpoly" -> eigen(n, angle)
                "rref" -> rrefSteps(n, angle)
                "dsolve" -> ode(n, angle)
                "mean", "median", "sd", "psd", "var" -> statistics(n, angle)
                "grad", "div", "curl", "jacobian", "hessian" -> vectorCalculus(n, angle)
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

    /** The question and its answer: "=" for a value, "⇒" for an answer that is itself an equation or list (y = …, λ = 1, λ = 3). */
    private fun ans(question: MathRow, value: Expr): MathRow =
        if (value is Eq || value is com.example.cas.cas.Seq || value is com.example.cas.cas.Rel) line(question, "  ", "⇒", "  ", value) else line(question, "=", value)

    private fun same(a: Expr, b: Expr) = Printer.plain(a) == Printer.plain(b)

    /** LaTeX for an expression, for the math in step texts; [tx] wraps it in $ … $. */
    private fun lx(e: Expr): String = Latex.of(Formatter.row(e))
    private fun tx(e: Expr): String = "\$" + lx(e) + "\$"
    /** [lx] in brackets when it's negative or a sum, for a factor (R₂ − (−3) R₁). */
    private fun lxp(e: Expr): String = lx(e).let { if (it.startsWith("-") || it.contains('+') || it.drop(1).contains('-')) "\\left($it\\right)" else it }

    // ---- Integrals -----------------------------------------------------------------------------

    /** An antiderivative and the steps to it; [sub] when the last move was a substitution (for changing the limits). */
    private class Traced(val result: Expr, val steps: List<Step>, val sub: SubUsed? = null)

    /** A substitution as made: the new variable [t] = [tOfX] (in x), the integrand in t, and its antiderivative in t. */
    private class SubUsed(val t: Sym, val tOfX: Expr, val integrand: Expr, val antiderivative: Expr)

    /**
     * An integral's working. A double or triple integral is worked from the inside out: the
     * inner integral first, in full, with the outer variables held constant, then the next one
     * out on its result, each as a step with its own working underneath.
     */
    private fun integral(n: Integral, angle: AngleUnit, held: List<String> = emptyList()): Solution? {
        val innerNode = n.body.items.firstOrNull { it is Integral } as? Integral ?: return single(n, angle)
        val xName = n.variable.items.joinToString("") { (it as? SymNode)?.text ?: "" }
        val inner = integral(innerNode, angle, held + xName) ?: return single(n, angle)
        val outer = single(n, angle) ?: return null
        val x = n.variable.items.joinToString("") { (it as? SymNode)?.text ?: "" }
        val y = innerNode.variable.items.joinToString("") { (it as? SymNode)?.text ?: "" }
        val question = com.example.cas.editor.MathCodec.copy(MathRow(mutableListOf(n)))
        val steps = ArrayList<Step>()
        if (held.isEmpty()) coordinatesNote(n)?.let { steps += it }
        val constants = (held + x).map { "\$$it\$" }
        val heldText = if (constants.size == 1) "${constants[0]} is held constant" else constants.dropLast(1).joinToString(", ") + " and ${constants.last()} are held constant"
        // An indefinite inner integral: its constant can depend on the outer variables, so it's left out here.
        val innerAnswer = MathRow(inner.answer.items.toMutableList()).also { r ->
            val k = r.items.size
            if (k >= 2 && (r.items[k - 1] as? SymNode)?.text == "C" && (r.items[k - 2] as? SymNode)?.text == "+") { r.items.removeAt(k - 1); r.items.removeAt(k - 2) }
        }
        steps += Step(
            "Inner integral, in \$$y\$", "Integrate in \$$y\$ first; $heldText." + if (inner.steps.any { it.title.startsWith("Inner integral") }) " It is itself an iterated integral, worked the same way." else "",
            innerAnswer, substeps = inner.steps.filter { it.kind != Kind.Result },
        )
        val outerWork = outer.steps.filter { it.kind != Kind.Result }
        val value = outer.answer.items.indexOfLast { (it as? SymNode)?.text == "=" }.let { k -> MathRow(outer.answer.items.drop(k + 1).toMutableList()) }
        val outerMath = outerWork.lastOrNull { it.math != null && it.kind != Kind.Check }?.math
        steps += Step("Outer integral, in \$$x\$", "Now integrate that result in \$$x\$." + if (held.isNotEmpty()) " " + held.joinToString(", ") { "\$$it\$" } + " still held constant." else "", outerMath, substeps = outerWork)
        val answer = line(question, "=", value)
        steps += Step("Answer", null, answer, Kind.Result)
        return Solution(steps.firstOrNull { it.kind == Kind.Note && it.title.endsWith("coordinates") }?.title ?: "Iterated integral", steps, answer)
    }

    /** Polar, cylindrical or spherical coordinates, read from the variables of an iterated integral: the area or volume element. */
    private fun coordinatesNote(n: Integral): Step? {
        val names = HashSet<String>()
        var cur: Integral? = n
        while (cur != null) {
            names += cur.variable.items.joinToString("") { (it as? SymNode)?.text ?: "" }
            cur = cur.body.items.firstOrNull { it is Integral } as? Integral
        }
        val (title, text, element) = when (names) {
            setOf("r", "θ") -> Triple("Polar coordinates", "\$x = r\\cos\\theta\$, \$y = r\\sin\\theta\$. The area element is \$dA = r\\,dr\\,d\\theta\$: the extra \$r\$ is the Jacobian of the change to polar coordinates.", "dA = r\\,dr\\,d\\theta")
            setOf("r", "θ", "z") -> Triple("Cylindrical coordinates", "\$x = r\\cos\\theta\$, \$y = r\\sin\\theta\$, \$z = z\$. The volume element is \$dV = r\\,dr\\,d\\theta\\,dz\$.", "dV = r\\,dr\\,d\\theta\\,dz")
            setOf("ρ", "θ", "φ"), setOf("r", "θ", "φ") -> {
                val rho = if ("ρ" in names) "\\rho" else "r"
                Triple("Spherical coordinates", "\$x = $rho\\sin\\varphi\\cos\\theta\$, \$y = $rho\\sin\\varphi\\sin\\theta\$, \$z = $rho\\cos\\varphi\$. The volume element is \$dV = $rho^2\\sin\\varphi\\,d$rho\\,d\\varphi\\,d\\theta\$.", "dV = $rho^2\\sin\\varphi\\,d$rho\\,d\\varphi\\,d\\theta")
            }
            else -> return null
        }
        return Step(title, text, LatexParser.parse(element), Kind.Note)
    }

    private fun single(n: Integral, angle: AngleUnit): Solution? {
        val name = n.variable.items.joinToString("") { (it as? SymNode)?.text ?: "" }
        if (name.length != 1) return null
        val x = Sym(name)
        val ev = { r: MathRow -> Evaluator(angle).evaluate(r) }
        // The integrand as typed (not simplified first), so any simplifying shows up as a step.
        val body = runCatching { Evaluator(angle).also { it.autoSimplify = false }.evaluate(com.example.cas.editor.MathCodec.copy(n.body)) }.getOrNull()
            ?.let { com.example.cas.cas.ComplexArith.normalize(it) } ?: ev(n.body)
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
        // ln|u|: the working is with ln u (the same derivative, u′/u), then the bars go back on.
        val absLogs = Calculus.absoluteLogs(body, x)
        val traced = if (absLogs != null) trace(absLogs.first, x, 0) else trace(body, x, 0)
        if (absLogs != null) steps += Step(
            "Absolute values", "\$\\frac{d}{d${x.name}}\\ln|u| = \\frac{u'}{u}\$, just like \$\\ln u\$: integrate with \$\\ln u\$, then put the absolute values back.",
            eq(int(body, x), int(absLogs.first, x)),
        )
        steps += traced?.steps ?: listOf(Step("Integrate", "Found by rewriting the integrand.", eq(int(body, x), f)))
        if (absLogs != null && traced != null) {
            val back = absLogs.second(traced.result)
            steps += Step("Put the absolute values back", "Each \$\\ln\$ of the answer becomes \$\\ln|\\cdot|\$, so it holds for negative values too.", eq(traced.result, back))
            if (!same(back, f)) steps += Step("Simplify", null, eq(back, f))
        } else if (traced != null && !same(traced.result, f)) steps += Step("Simplify", null, eq(traced.result, f))
        val method = methodOf(steps)
        if (!definite) {
            // The check: differentiating gives the integrand back.
            val back = Calculus.evenAbsPowers(Algebra.simplify(Calculus.diff(f, x))).let { Algebra.simplify(it) }
            if (agrees(back, body, x)) {
                steps += Step(
                    "Check", "Differentiating the answer gives the integrand back.",
                    line(com.example.cas.editor.Derivative(MathRow(mutableListOf(sym(x.name))), ex(f)), "=", back), Kind.Check,
                )
            }
            val answer = line(int(body, x), "=", f, "+", "C")
            steps += Step("Answer", "Add a constant \$C\$: every antiderivative differs by one.", answer, Kind.Result)
            return Solution(method, steps, answer)
        }
        val a = ev(n.lower); val b = ev(n.upper)
        // An infinite limit, or an integrand that blows up at an end: worked as a limit.
        if (absLogs == null) runCatching { improper(body, x, a, b, f) }.getOrNull()?.let { (work, answer) ->
            return Solution(method, steps + work + Step("Answer", null, answer, Kind.Result), answer)
        }
        val value = Calculus.definite(body, x, a, b)
        // A substitution: change the limits and finish in the new variable, with no back-substitution.
        if (absLogs == null && traced?.sub != null) changedLimits(body, x, a, b, value, traced)?.let { (work, method2) ->
            val answer = eq(int(body, x, a, b), value)
            return Solution(method2 ?: method, work + Step("Answer", null, answer, Kind.Result), answer)
        }
        val fa = at(f, x, a, fromBelow = false); val fb = at(f, x, b, fromBelow = true)
        steps += Step(
            "Fundamental theorem", "\$F(b) - F(a)\$: the antiderivative at the upper limit minus its value at the lower one" +
                (if (Calculus.isInfinite(a) || Calculus.isInfinite(b)) ", as a limit at \$\\infty\$." else "."),
            line(int(body, x, a, b), "=", fb, "−", paren(fa)),
        )
        val answer = eq(int(body, x, a, b), value)
        steps += Step("Answer", null, answer, Kind.Result)
        return Solution(method, steps, answer)
    }

    /**
     * An improper integral as a limit: an infinite end, or an end where the integrand blows up,
     * is replaced by t, the integral up to t is found from the antiderivative, and t goes to the
     * end. Two improper ends are split at a point between them. Ends with "converges to L" or
     * "diverges". Null when the integral isn't improper (or a limit can't be found).
     */
    private fun improper(body: Expr, x: Sym, a: Expr, b: Expr, f: Expr): Pair<List<Step>, MathRow>? {
        val others = body.freeVars() - x.name
        fun blowsUp(end: Expr): Boolean {
            if (Calculus.isInfinite(end) || others.isNotEmpty()) return false
            val v = runCatching { Numeric.eval(Algebra.simplify(body.subst(x, end))) }.getOrNull() ?: return true
            return !v.re.isFinite() || !v.im.isFinite()
        }
        val inside = runCatching { Calculus.singularities(body, x, a, b) }.getOrDefault(emptyList())
        val badA = Calculus.isInfinite(a) || blowsUp(a)
        val badB = Calculus.isInfinite(b) || blowsUp(b)
        if (!badA && !badB && inside.isEmpty()) return null
        val t = Sym(listOf("t", "s", "T").first { it != x.name && it !in body.freeVars() })
        fun why(end: Expr) = if (Calculus.isInfinite(end)) "\$${x.name} \\to ${lx(end)}\$" else "the integrand blows up at \$${x.name} = ${lx(end)}\$"
        fun limRow(r: MathRow, end: Expr, side: Int): MathRow {
            val spec = line(t.name, "→", end).also { if (side != 0) it.add(com.example.cas.editor.Pow(MathRow(mutableListOf(sym(if (side > 0) "+" else "−"))))) }
            return MathRow(mutableListOf(Func("lim", listOf(r, spec))))
        }
        /** One improper end: the steps and the limit (null if it can't be found). */
        fun oneEnd(lo: Expr, hi: Expr, upper: Boolean): Pair<List<Step>, Expr>? {
            val end = if (upper) hi else lo
            val fixed = if (upper) lo else hi
            val side = when { Calculus.isInfinite(end) -> 0; upper -> -1; else -> 1 }
            val ff = at(f, x, fixed, fromBelow = !upper)
            val ft = Algebra.simplify(f.subst(x, t))
            val diff = Algebra.simplify(if (upper) sub(ft, ff) else sub(ff, ft))
            val partial = if (upper) int(body, x, lo, t) else int(body, x, t, hi)
            val limit = runCatching { if (side == 0) Calculus.limit(diff, t, end) else Calculus.limit(diff, t, end, side) }.getOrNull() ?: return null
            val finite = !limit.contains { it == com.example.cas.cas.INF } && runCatching { Numeric.eval(limit).re.isFinite() }.getOrDefault(false)
            val steps = listOf(
                Step(
                    "As a limit", "${why(end).replaceFirstChar { it.uppercase() }}, so replace that end by \$${t.name}\$ and let \$${t.name} \\to ${lx(end)}\$" + (if (side > 0) " from above." else if (side < 0) " from below." else "."),
                    line(int(body, x, lo, hi), "=", limRow(partial, end, side)),
                ),
                Step(
                    if (upper) "Integrate up to \$${t.name}\$" else "Integrate from \$${t.name}\$", "With the antiderivative \$F = ${lx(f)}\$.",
                    line(partial, "=", if (upper) ex(ft) else ex(ff), "−", if (upper) paren(ff) else paren(ft)),
                ),
                Step(
                    "Take the limit", if (finite) "The limit is finite, so this integral converges." else "The limit is infinite, so the integral diverges.",
                    line(limRow(ex(diff), end, side), "=", limit), Kind.Check,
                ),
            )
            return steps to limit
        }
        val whole = int(body, x, a, b)
        if (inside.isNotEmpty()) {
            // Singular points inside: split there; each part is improper at its singular ends.
            val points = listOf(a) + inside + listOf(b)
            val bad = { e: Expr -> Calculus.isInfinite(e) || blowsUp(e) || inside.any { same(it, e) } }
            fun piece(lo: Expr, hi: Expr): Pair<List<Step>, Expr>? {
                val bl = bad(lo); val bh = bad(hi)
                return when {
                    bl && bh -> {
                        val c = Algebra.simplify(div(add(lo, hi), com.example.cas.cas.TWO)).let { if (Calculus.isInfinite(lo)) sub(hi, com.example.cas.cas.ONE) else if (Calculus.isInfinite(hi)) add(lo, com.example.cas.cas.ONE) else it }.let { Algebra.simplify(it) }
                        val (l, v1) = oneEnd(lo, c, upper = false) ?: return null
                        val (r, v2) = oneEnd(c, hi, upper = true) ?: return null
                        (l + r) to Algebra.simplify(add(v1, v2))
                    }
                    bl || bh -> oneEnd(lo, hi, upper = bh)
                    else -> {
                        val v = Algebra.simplify(sub(at(f, x, hi, fromBelow = true), at(f, x, lo, fromBelow = false)))
                        listOf(Step("Fundamental theorem", null, line(int(body, x, lo, hi), "=", v))) to v
                    }
                }
            }
            val pieces = points.zipWithNext { lo, hi -> Triple(lo, hi, piece(lo, hi) ?: return null) }
            val where = inside.joinToString(", ") { lx(it) }
            val split = Step(
                "Singular point inside", "The integrand blows up at \$${x.name} = $where\$, inside the interval, so the antiderivative can't be used across it: split there. The integral converges only if every part does.",
                line(whole, "=", *pieces.flatMapIndexed { k, (lo, hi, _) -> (if (k > 0) listOf<Any>("+") else emptyList()) + listOf<Any>(int(body, x, lo, hi)) }.toTypedArray()),
                Kind.Note,
            )
            val parts = pieces.mapIndexed { k, (lo, hi, work) -> Step("Part ${k + 1}", null, int(body, x, lo, hi), substeps = work.first) }
            val values = pieces.map { it.third.second }
            val diverges = values.any { v -> v.contains { it == com.example.cas.cas.INF } || runCatching { !Numeric.real(v).isFinite() }.getOrDefault(true) }
            val total = if (diverges) null else Algebra.simplify(add(values))
            val end = if (diverges) Step("Diverges", "A part is infinite, so the integral diverges; using the antiderivative straight across \$${x.name} = $where\$ would give a wrong, finite number.", null, Kind.Note)
            else Step("Add the parts", "Every part converges.", line(*values.flatMapIndexed { k, v -> (if (k > 0) listOf<Any>("+") else emptyList()) + listOf<Any>(paren(v)) }.toTypedArray(), "=", total!!), Kind.Check)
            val answer = if (total != null) line(whole, "=", total) else line(whole, "  ", "diverges")
            return (listOf(split) + parts + end) to answer
        }
        if (badA != badB) {
            val (work, limit) = oneEnd(a, b, upper = badB) ?: return null
            return work to line(whole, "=", limit)
        }
        // Both ends improper: split at a point c between them.
        val c: Expr = when {
            Calculus.isInfinite(a) && Calculus.isInfinite(b) -> if (blowsUp(ZERO)) com.example.cas.cas.ONE else ZERO
            Calculus.isInfinite(a) -> sub(b, com.example.cas.cas.ONE)
            Calculus.isInfinite(b) -> add(a, com.example.cas.cas.ONE)
            else -> div(add(a, b), com.example.cas.cas.TWO)
        }.let { Algebra.simplify(it) }
        if (blowsUp(c)) return null
        val (left, l1) = oneEnd(a, c, upper = false) ?: return null
        val (right, l2) = oneEnd(c, b, upper = true) ?: return null
        val total = Algebra.simplify(add(l1, l2))
        val split = Step("Split the integral", "Both ends are improper, so split at \$${x.name} = ${lx(c)}\$ and take each limit on its own; it converges only if both do.", line(whole, "=", int(body, x, a, c), "+", int(body, x, c, b)))
        val parts = listOf(
            Step("Left part", null, int(body, x, a, c), substeps = left),
            Step("Right part", null, int(body, x, c, b), substeps = right),
        )
        val diverges = listOf(l1, l2).any { it.contains { e -> e == com.example.cas.cas.INF } }
        val end = Step(if (diverges) "Diverges" else "Add them", if (diverges) "One of the parts is infinite, so the whole integral diverges." else "Both parts converge.", line(l1, "+", paren(l2), "=", total), if (diverges) Kind.Note else Kind.Rule)
        return (listOf(split) + parts + end) to line(whole, "=", total)
    }

    /**
     * ∫ₐᵇ f dx by substitution with the limits changed: the substitution's steps (without putting
     * the old variable back), the new limits t(a) and t(b), and the new integral evaluated there.
     * Null when the limits can't be found or the result doesn't agree with [value].
     */
    private fun changedLimits(body: Expr, x: Sym, a: Expr, b: Expr, value: Expr, traced: Traced): Pair<List<Step>, String?>? {
        val su = traced.sub ?: return null
        if (Calculus.isInfinite(a) || Calculus.isInfinite(b)) return null
        val ta = runCatching { Algebra.simplify(su.tOfX.subst(x, a)) }.getOrNull() ?: return null
        val tb = runCatching { Algebra.simplify(su.tOfX.subst(x, b)) }.getOrNull() ?: return null
        fun finite(e: Expr) = runCatching { Numeric.eval(e) }.getOrNull()?.let { it.re.isFinite() && kotlin.math.abs(it.im) < 1e-12 } == true
        if (!finite(ta) || !finite(tb)) return null
        val big = su.antiderivative
        val gb = at(big, su.t, tb, fromBelow = true); val ga = at(big, su.t, ta, fromBelow = false)
        val got = runCatching { Algebra.simplify(sub(gb, ga)) }.getOrNull() ?: return null
        // The same number as the direct way, or don't use it (a substitution that jumps inside the interval).
        val v1 = runCatching { Numeric.eval(got).re }.getOrNull() ?: return null
        val v2 = runCatching { Numeric.eval(value).re }.getOrNull() ?: return null
        if (!v1.isFinite() || kotlin.math.abs(v1 - v2) > 1e-8 * maxOf(1.0, kotlin.math.abs(v2))) return null
        val t = su.t.name
        fun noBack(steps: List<Step>): List<Step> = steps.filter { it.title != "Back-substitute" }.map { Step(it.title, it.text, it.math, it.kind, noBack(it.substeps)) }
        val work = noBack(traced.steps).toMutableList()
        work += Step(
            "Change the limits", "With \$$t = ${lx(su.tOfX)}\$: when \$${x.name} = ${lx(a)}\$, \$$t = ${lx(ta)}\$; when \$${x.name} = ${lx(b)}\$, \$$t = ${lx(tb)}\$. Then there's no need to put \$${x.name}\$ back.",
            line(int(body, x, a, b), "=", int(su.integrand, su.t, ta, tb)),
        )
        work += Step(
            "Evaluate in \$$t\$", "The antiderivative in \$$t\$ at the new upper limit minus its value at the new lower one.",
            line(int(su.integrand, su.t, ta, tb), "=", gb, "−", paren(ga), "=", value),
        )
        return work to null
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
        return listOf(
            "Quartic denominator", "Trigonometric substitution", "Weierstrass substitution", "Rationalizing substitution", "By parts twice", "Quadratic denominator",
            "Complete the square", "Multiple angles", "Product-to-sum", "Power reduction", "Special function", "Integration by parts", "Substitution",
            "Divide, then split", "Partial fractions", "Power rule", "Sum rule",
        )
            .firstOrNull { it in titles } ?: titles.firstOrNull { it != "Absolute values" && it != "Put the absolute values back" } ?: "Integration"
    }

    /** The steps to ∫ [e] d[x], following the integrator's methods; null if it can't follow them. */
    private fun trace(e: Expr, x: Sym, depth: Int): Traced? {
        if (depth > 9) return blackBox(e, x, depth)
        if (e.freeOf(x)) return Traced(mul(e, x), listOf(Step("Constant", "The integral of a constant \$c\$ is \$c\\,${x.name}\$.", eq(int(e, x), mul(e, x)))))
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
                // A substitution inside still counts, scaled by c, so the limits can be changed.
                val su = inner.sub?.let { SubUsed(it.t, it.tOfX, mul(c, it.integrand), mul(c, it.antiderivative)) }
                return Traced(mul(c, inner.result), listOf(Step("Constant multiple", "Take ${tx(c)} out of the integral.", eq(int(e, x), line(paren(c), int(r, x))), substeps = inner.steps)), su)
            }
        }
        standard(e, x)?.let { return it }
        if (depth < 9) {
            runCatching { quadraticPiece(e, x, depth) }.getOrNull()?.let { return it }
            runCatching { partsTwice(e, x) }.getOrNull()?.let { return it }
            runCatching { quarticFull(e, x) }.getOrNull()?.let { return it }
        }
        partialFractions(e, x, depth)?.let { return it }
        runCatching { gaussianSteps(e, x) }.getOrNull()?.let { return it }
        // An answer in a special function: say so straight away, rather than substituting around it.
        if (e !is Add) runCatching { Calculus.integrate(e, x) }.getOrNull()?.takeIf { f -> f.contains { it is Fn && it.name in SPECIAL } }?.let { f ->
            runCatching { specialSteps(e, x, f, depth) }.getOrNull()?.let { return it }
        }
        // A substitution that lands on elementary functions beats integrating by parts (x e^(x²) is u = x², not erfi).
        substitution(e, x, depth)?.takeIf { t -> !t.result.contains { it is Fn && it.name in SPECIAL } }?.let { return it }
        byParts(e, x, depth)?.let { return it }
        substitution(e, x, depth)?.let { return it }
        multiplyOut(e, x, depth)?.let { return it }
        if (depth < 9) {
            runCatching { inverseByParts(e, x, depth) }.getOrNull()?.let { return it }
            runCatching { gaussianSteps(e, x) }.getOrNull()?.let { return it }
            runCatching { multipleAngles(e, x, depth) }.getOrNull()?.let { return it }
            runCatching { trigRewrite(e, x, depth) }.getOrNull()?.let { return it }
            runCatching { exponentialForm(e, x, depth) }.getOrNull()?.let { return it }
        }
        return blackBox(e, x, depth)
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
    private fun blackBox(e: Expr, x: Sym, depth: Int = 0): Traced? {
        val found = runCatching { com.example.cas.cas.Integrals.identify(e, x) }.getOrNull()
        // A substitution, carried out step by step when it can be.
        if (found != null && depth < 8) runCatching { substitutionFor(found.first, e, x) }.getOrNull()?.let { sub ->
            runCatching { substitutionSteps(e, x, sub, depth) }.getOrNull()?.let { return it }
        }
        val f = found?.second ?: Calculus.integrate(e, x) ?: return null
        if (f.contains { it is Fn && it.name in SPECIAL }) runCatching { specialSteps(e, x, f, depth) }.getOrNull()?.let { return it }
        if (found?.first == "quartic") runCatching { quarticSteps(e, x, f) }.getOrNull()?.let { return it }
        val (title, text) = found?.first?.let { describe(it, e, x) }
            ?: if (f.contains { it is Fn && it.name in SPECIAL }) "Special function" to "This has no antiderivative in elementary functions; it's written with a function defined by this very integral."
            else "Rewrite and integrate" to "Rewrite the integrand (identities, a substitution) and integrate the pieces."
        return Traced(f, listOf(Step(title, text, eq(int(e, x), f))))
    }

    // ---- The remaining methods written out ---------------------------------------------------------------

    private val one get() = com.example.cas.cas.ONE
    private val two get() = com.example.cas.cas.TWO
    private fun f1(name: String, a: Expr) = com.example.cas.cas.fn(name, a)
    private fun p(b: Expr, k: Expr) = com.example.cas.cas.pow(b, k)

    /** (Bx + C)/(x² + px + q), the quadratic irreducible: a logarithm part and an arctangent part. */
    private fun quadraticPiece(e: Expr, x: Sym, depth: Int): Traced? {
        val (n, d) = Algebra.together(e)
        val qn = Algebra.qpoly(n, x) ?: return null; val qd = Algebra.qpoly(d, x) ?: return null
        if (qd.degree != 2 || qn.degree > 1) return null
        val lead = qd.lead
        val pq = qd.scale(lead.reciprocal())
        if (pq[0] - pq[1] * pq[1] / com.example.cas.math.Rational.of(4) <= com.example.cas.math.Rational.ZERO) return null
        val (result, steps) = quadParts(Num(qn[1] / lead), Num(qn[0] / lead), Num(pq[1]), Num(pq[0]), x)
        if (!agrees(Algebra.simplify(Calculus.diff(result, x)), e, x)) return null
        return Traced(result, listOf(Step("Quadratic denominator", "It doesn't factor over the reals, so integrate it as a logarithm plus an arctangent.", eq(int(e, x), result), substeps = steps)))
    }

    /** ∫ (Bx + C)/(x² + px + q) dx with any (exact) coefficients: B/2 · ln Q, and the rest by completing the square. */
    private fun quadParts(bC: Expr, cC: Expr, pE: Expr, qE: Expr, x: Sym): Pair<Expr, List<Step>> {
        val quad = add(p(x, two), mul(pE, x), qE)
        val h = Algebra.simplify(div(pE, two))
        val k = Algebra.simplify(sub(qE, div(mul(pE, pE), Num(4))))
        val rk = Algebra.simplify(com.example.cas.cas.sqrt(k))
        val logCoef = Algebra.simplify(div(bC, two))
        val atanCoef = Algebra.simplify(sub(cC, div(mul(bC, pE), two)))
        val logPart = mul(logCoef, f1("ln", quad))
        val atanPart = mul(atanCoef, div(one, rk), f1("atan", div(add(x, h), rk)))
        val steps = ArrayList<Step>()
        val deriv = add(mul(two, x), pE)
        if (bC != ZERO) {
            steps += Step("Split the numerator", "Write the top as a multiple of the bottom's derivative (${tx(deriv)}) plus a constant.",
                eq(ex(div(add(mul(bC, x), cC), quad)), line(paren(logCoef), MathRow(mutableListOf(com.example.cas.editor.Frac(ex(deriv), ex(quad)))), "+", paren(atanCoef), MathRow(mutableListOf(com.example.cas.editor.Frac(MathRow(mutableListOf(sym("1"))), ex(quad)))))))
            steps += Step("Logarithm part", "The top is the derivative of the bottom: \$\\int \\frac{u'}{u}\\,du = \\ln u\$.", eq(int(div(deriv, quad), x), f1("ln", quad)))
        }
        if (atanCoef != ZERO) {
            steps += Step("Complete the square", null, eq(ex(quad), line("(", add(x, h), ")", com.example.cas.editor.Pow(MathRow(mutableListOf(sym("2")))), "+", paren(k))))
            steps += Step("Arctangent", "\$\\int \\frac{du}{u^2 + a^2} = \\frac{1}{a}\\tan^{-1}\\frac{u}{a}\$, with \$u = ${lx(add(x, h))}\$, \$a = ${lx(rk)}\$.", eq(int(div(one, quad), x), mul(div(one, rk), f1("atan", div(add(x, h), rk)))))
        }
        return add(logPart, atanPart) to steps
    }

    /** N/(x⁴ + px² + q): factor into two real quadratics, solve for the partial fractions, then each piece. */
    private fun quarticFull(e: Expr, x: Sym): Traced? {
        val (n, d) = Algebra.together(e)
        val qd = Algebra.qpoly(d, x) ?: return null; val qn = Algebra.qpoly(n, x) ?: return null
        if (qd.degree != 4 || qn.degree > 3) return null
        val m = qd.scale(qd.lead.reciprocal())
        if (m[1].signum != 0 || m[3].signum != 0) return null
        // Already splits over the rationals: ordinary partial fractions do it.
        if (Algebra.rationalRoots(m).isNotEmpty()) return null
        val pp = Num(m[2]); val qq = Num(m[0])
        if (m[0].signum <= 0) return null
        val r = Algebra.simplify(com.example.cas.cas.sqrt(qq))
        val two2r = Algebra.simplify(sub(mul(two, r), pp))
        if (Numeric.real(two2r) <= 0) return null
        val sv = Algebra.simplify(com.example.cas.cas.sqrt(two2r))
        val c = (0..3).map { Num(qn[it] / qd.lead) }
        // (Ax + B)(x² − sx + r) + (Cx + D)(x² + sx + r) = N, matched power by power.
        val n0r = div(c[0], r)
        val cMinusA = Algebra.simplify(div(sub(c[2], n0r), sv))
        val dMinusB = Algebra.simplify(div(sub(c[1], mul(r, c[3])), sv))
        val A = Algebra.simplify(div(sub(c[3], cMinusA), two)); val C = Algebra.simplify(div(add(c[3], cMinusA), two))
        val B = Algebra.simplify(div(sub(n0r, dMinusB), two)); val D = Algebra.simplify(div(add(n0r, dMinusB), two))
        val q1 = add(p(x, two), mul(sv, x), r); val q2 = add(p(x, two), neg(mul(sv, x)), r)
        val (r1, s1) = quadParts(A, B, sv, r, x)
        val (r2, s2) = quadParts(C, D, neg(sv), r, x)
        val result = add(r1, r2)
        if (!agrees(Algebra.simplify(Calculus.diff(result, x)), e, x)) return null
        val pieces = line(MathRow(mutableListOf(com.example.cas.editor.Frac(ex(add(mul(A, x), B)), ex(q1)))), "+", MathRow(mutableListOf(com.example.cas.editor.Frac(ex(add(mul(C, x), D)), ex(q2)))))
        val steps = listOf(
            Step("Factor the quartic", "\$x^4 + px^2 + q = (x^2 + r)^2 - (2r - p)x^2\$, a difference of squares: \$r = \\sqrt{q} = ${lx(r)}\$, \$s = \\sqrt{2r - p} = ${lx(sv)}\$.", eq(ex(m.toExpr(x)), line("(", q1, ")", "(", q2, ")"))),
            Step("Partial fractions", "Write it as \$\\frac{Ax + B}{\\text{first}} + \\frac{Cx + D}{\\text{second}}\$ and match the powers of \$${x.name}\$: \$A + C\$, \$B + D\$, \$s(C - A) + B + D\$ and \$r(A + C) + s(D - B)\$ give the coefficients.", eq(ex(e), pieces)),
            Step("First piece", null, eq(int(div(add(mul(A, x), B), q1), x), r1), substeps = s1),
            Step("Second piece", null, eq(int(div(add(mul(C, x), D), q2), x), r2), substeps = s2),
        )
        return Traced(result, listOf(Step("Quartic denominator", "It has no real roots, but it splits into two real quadratics.", eq(int(e, x), result), substeps = steps)))
    }

    /** e^(ax) sin(bx) or cos(bx): by parts twice, then solve for the integral that comes back. */
    private fun partsTwice(e: Expr, x: Sym): Traced? {
        val fs = (e as? Mul)?.factors ?: return null
        if (fs.size != 2) return null
        val ex0 = fs.firstOrNull { it is Pow && it.base == E } as? Pow ?: return null
        val tr = fs.firstOrNull { it is Fn && it.name in setOf("sin", "cos") } as? Fn ?: return null
        val a = linear(ex0.exp, x) ?: return null; val b = linear(tr.args[0], x) ?: return null
        val other = if (tr.name == "sin") "cos" else "sin"
        val i = sym("I")
        val f = Calculus.integrate(e, x) ?: return null
        val eOther = mul(ex0, f1(other, tr.args[0]))
        val sign = if (tr.name == "sin") "−" else "+"
        val first = line(i, "=", mul(div(one, a), e), sign, paren(div(b, a)), int(eOther, x))
        val second = line(int(eOther, x), "=", mul(div(one, a), eOther), if (tr.name == "sin") "+" else "−", paren(div(b, a)), i)
        val norm = Algebra.simplify(add(mul(a, a), mul(b, b)))
        val steps = listOf(
            Step("By parts", "\$u = \\${tr.name}(${lx(tr.args[0])})\$, \$dv = ${lx(ex0)}\\,d${x.name}\$.", first),
            Step("By parts again", "Now \$u = \\$other(${lx(tr.args[0])})\$: the original integral \$I\$ comes back.", second),
            Step("Substitute back", "Put the second line into the first.", line(i, "=", mul(div(one, a), e), sign, paren(div(b, a)), "[", mul(div(one, a), eOther), if (tr.name == "sin") "+" else "−", paren(div(b, a)), i, "]")),
            Step("Collect the I terms", "Move \$${lx(Algebra.simplify(div(mul(b, b), mul(a, a))))}\\,I\$ to the left: \$I\\left(1 + \\frac{b^2}{a^2}\\right) = I\\,\\frac{a^2 + b^2}{a^2}\$, with \$a^2 + b^2 = ${lx(norm)}\$.",
                line(paren(Algebra.simplify(div(norm, mul(a, a)))), i, "=", Algebra.simplify(add(mul(div(one, a), e), mul(if (tr.name == "sin") neg(div(b, mul(a, a))) else div(b, mul(a, a)), eOther))))),
            Step("Solve for I", "Divide by ${tx(Algebra.simplify(div(norm, mul(a, a))))}.", line(i, "=", f)),
        )
        return Traced(f, listOf(Step("By parts twice", "Call the integral \$I\$; integrating by parts twice brings \$I\$ back, so it can be solved for.", eq(int(e, x), f), substeps = steps)))
    }

    /** By parts with u a logarithm or inverse function and dv the rest (dv = dx when there's no rest). */
    private fun inverseByParts(e: Expr, x: Sym, depth: Int): Traced? {
        val fs = if (e is Mul) e.factors else listOf(e)
        val inverse = setOf("ln", "atan", "asin", "acos", "asinh", "acosh", "atanh", "erf", "erfi", "si", "ci", "ei", "polylog")
        val u = fs.firstOrNull { f -> val b = if (f is Pow && f.exp is Num && (f.exp as Num).q.isInteger && (f.exp as Num).q.signum > 0) f.base else f; b is Fn && b.name in inverse && !b.freeOf(x) } ?: return null
        val dv = mul(fs.filter { it !== u })
        if (dv.contains { it is Fn && it.name in inverse && !it.freeOf(x) }) return null
        // Plain ln x, atan x… are in the table; this is for the rest.
        if (dv == one && u is Fn && linear(u.args[0], x) != null) return null
        val v = Calculus.integrate(dv, x) ?: return null
        val du = Calculus.diff(u, x)
        val restBody = Algebra.simplify(mul(v, du))
        val rest = trace(restBody, x, depth + 1) ?: return null
        val result = sub(mul(u, v), rest.result)
        if (!agrees(Algebra.simplify(Calculus.diff(result, x)), e, x)) return null
        return Traced(result, listOf(Step(
            "Integration by parts", "\$u = ${lx(u)}\$ (simpler once differentiated), \$dv = ${lx(dv)}\\,d${x.name}\$: \$\\int u\\,dv = uv - \\int v\\,du\$.",
            eq(int(e, x), line(mul(u, v), "−", int(restBody, x))),
            substeps = listOf(Step("u and v", null, line(sym("d"), "u", "=", du, " ", "d", x.name, ",", "  ", "v", "=", v))) + rest.steps,
        )))
    }

    /** e^Q, Q quadratic with a negative x² coefficient: complete the square, substitute, erf. */
    private fun gaussianSteps(e: Expr, x: Sym): Traced? {
        if (e !is Pow || e.base != E) return null
        val cs = Algebra.coefficients(Algebra.expand(e.exp), x)?.map { (it as? Num)?.q } ?: return null
        if (cs.size != 3 || cs.any { it == null }) return null
        val (c0, c1, c2) = cs.map { it!! }
        if (c2.signum >= 0) return null
        val a = -c2
        val h = c1 / (a * com.example.cas.math.Rational.of(2))
        val k = c0 + c1 * c1 / (a * com.example.cas.math.Rational.of(4))
        val f = Calculus.integrate(e, x) ?: return null
        val shifted = sub(x, Num(h))
        val square = add(mul(Num(-a), p(shifted, two)), Num(k))
        val ra = Algebra.simplify(com.example.cas.cas.sqrt(Num(a)))
        val uExpr = mul(ra, shifted)
        val steps = ArrayList<Step>()
        if (c1.signum != 0 || c0.signum != 0) steps += Step("Complete the square", null, eq(ex(e.exp), ex(square)))
        steps += Step("Substitute", "\$u = ${lx(uExpr)}\$, so \$d${x.name} = \\frac{du}{${lx(ra)}}\$.", eq(int(e, x), line(paren(div(p(E, Num(k)), ra)), int(p(E, neg(p(Sym("u"), two))), Sym("u")))))
        steps += Step("Gaussian integral", "\$\\int e^{-u^2}\\,du = \\frac{\\sqrt{\\pi}}{2}\\operatorname{erf} u\$: erf is defined by this integral.", eq(int(e, x), f))
        return Traced(f, listOf(Step("Complete the square", "A quadratic exponent: complete the square, then the Gaussian integral.", eq(int(e, x), f), substeps = steps)))
    }

    /** sin ku, cos ku (k = 2, 3) beside sin u or cos u: written in sin u and cos u, simplified, then integrated. */
    private fun multipleAngles(e: Expr, x: Sym, depth: Int): Traced? {
        val singles = HashSet<Expr>()
        fun collect(z: Expr) { if (z is Fn && z.name in setOf("sin", "cos", "tan")) singles += z.args[0]; z.children.forEach { collect(it) } }
        collect(e)
        val hasSin = e.contains { it is Fn && it.name == "sin" }
        var used = false
        val r0 = replace(e, { z ->
            z is Fn && z.name in setOf("sin", "cos") && z.args[0] is Mul && (z.args[0] as Mul).factors.first().let { it == two || it == Num(3) } &&
                mul((z.args[0] as Mul).factors.drop(1)) in singles
        }) { z ->
            used = true
            val f = z as Fn
            val k = (f.args[0] as Mul).factors.first()
            val u = mul((f.args[0] as Mul).factors.drop(1))
            val su = f1("sin", u); val cu = f1("cos", u)
            when {
                f.name == "sin" && k == two -> mul(two, su, cu)
                f.name == "cos" && k == two -> if (hasSin) sub(one, mul(two, p(su, two))) else sub(mul(two, p(cu, two)), one)
                f.name == "sin" -> sub(mul(Num(3), su), mul(Num(4), p(su, Num(3))))
                else -> sub(mul(Num(4), p(cu, Num(3))), mul(Num(3), cu))
            }
        }
        if (!used) return null
        val r = Algebra.simplify(r0)
        if (same(r, e)) return null
        val inner = trace(r, x, depth + 1) ?: return null
        return Traced(inner.result, listOf(Step(
            "Multiple angles", "\$\\sin 2u = 2\\sin u\\cos u\$, \$\\cos 2u = 1 - 2\\sin^2 u = 2\\cos^2 u - 1\$, \$\\sin 3u = 3\\sin u - 4\\sin^3 u\$, \$\\cos 3u = 4\\cos^3 u - 3\\cos u\$; then simplify.",
            eq(ex(e), ex(r)),
        )) + inner.steps)
    }

    /** sin·cos products and even powers rewritten as single sines and cosines, then term by term. */
    private fun trigRewrite(e: Expr, x: Sym, depth: Int): Traced? {
        if (!e.contains { it is Fn && it.name in setOf("sin", "cos") }) return null
        var used = ""
        fun walk(z: Expr): Expr = when {
            z is Mul -> {
                val fs = z.factors.map { walk(it) }
                val trig = fs.filter { it is Fn && it.name in setOf("sin", "cos") }
                if (trig.size >= 2) {
                    val (s1, s2) = trig.take(2).map { it as Fn }
                    val a = s1.args[0]; val b = s2.args[0]
                    used = "Product-to-sum"
                    val r = when {
                        s1.name == "sin" && s2.name == "cos" -> mul(com.example.cas.cas.HALF, add(f1("sin", add(a, b)), f1("sin", sub(a, b))))
                        s1.name == "cos" && s2.name == "sin" -> mul(com.example.cas.cas.HALF, add(f1("sin", add(a, b)), f1("sin", sub(b, a))))
                        s1.name == "cos" -> mul(com.example.cas.cas.HALF, add(f1("cos", sub(a, b)), f1("cos", add(a, b))))
                        else -> mul(com.example.cas.cas.HALF, sub(f1("cos", sub(a, b)), f1("cos", add(a, b))))
                    }
                    mul(fs.filter { it !== s1 && it !== s2 } + r)
                } else mul(fs)
            }
            z is Pow && z.exp == two && z.base is Fn && (z.base as Fn).name in setOf("sin", "cos") -> {
                if (used.isEmpty()) used = "Power reduction"
                val u = (z.base as Fn).args[0]
                if ((z.base as Fn).name == "sin") mul(com.example.cas.cas.HALF, sub(one, f1("cos", mul(two, u)))) else mul(com.example.cas.cas.HALF, add(one, f1("cos", mul(two, u))))
            }
            z is Add -> add(z.terms.map { walk(it) })
            else -> z
        }
        val r = Algebra.expand(walk(e))
        if (used.isEmpty() || same(r, e) || r !is Add) return null
        val inner = trace(r, x, depth + 1) ?: return null
        val text = if (used == "Product-to-sum") "\$\\sin A\\cos B = \\frac{1}{2}[\\sin(A + B) + \\sin(A - B)]\$, \$\\cos A\\cos B = \\frac{1}{2}[\\cos(A - B) + \\cos(A + B)]\$, \$\\sin A\\sin B = \\frac{1}{2}[\\cos(A - B) - \\cos(A + B)]\$."
            else "\$\\sin^2 u = \\frac{1 - \\cos 2u}{2}\$, \$\\cos^2 u = \\frac{1 + \\cos 2u}{2}\$."
        return Traced(inner.result, listOf(Step(used, text, eq(ex(e), ex(r)))) + inner.steps)
    }

    /** sinh, cosh, tanh written with exponentials, then term by term. */
    private fun exponentialForm(e: Expr, x: Sym, depth: Int): Traced? {
        if (!e.contains { it is Fn && it.name in setOf("sinh", "cosh") } || e is Fn) return null
        val r = Algebra.expand(replace(e, { it is Fn && it.name in setOf("sinh", "cosh") }) { z ->
            val u = (z as Fn).args[0]
            if (z.name == "sinh") mul(com.example.cas.cas.HALF, sub(p(E, u), p(E, neg(u)))) else mul(com.example.cas.cas.HALF, add(p(E, u), p(E, neg(u))))
        })
        if (r !is Add) return null
        val inner = trace(r, x, depth + 1) ?: return null
        return Traced(inner.result, listOf(Step("Exponential form", "\$\\sinh u = \\frac{e^u - e^{-u}}{2}\$, \$\\cosh u = \\frac{e^u + e^{-u}}{2}\$.", eq(ex(e), ex(r)))) + inner.steps)
    }

    /** A special function: substitute its argument if needed, then recognize its defining derivative. */
    private fun specialSteps(e: Expr, x: Sym, f: Expr, depth: Int): Traced? {
        val special = mutableListOf<Fn>()
        fun walk(z: Expr) { if (z is Fn && z.name in SPECIAL) special += z; z.children.forEach { walk(it) } }
        walk(f)
        val sf = special.firstOrNull() ?: return null
        val name = SPECIAL_NAMES[sf.name] ?: sf.name
        val u = sf.args.last()
        val steps = ArrayList<Step>()
        val a = linear(u, x)
        if (a != null && u != x) steps += Step("Substitute", "\$u = ${lx(u)}\$, \$d${x.name} = \\frac{du}{${lx(a)}}\$.", null)
        val back = Algebra.simplify(Calculus.diff(f, x))
        steps += Step("Defining derivative", "$name is defined so that its derivative is this kind of integrand${SPECIAL_DEFS[sf.name]?.let { ", \$$it\$." } ?: "."}", eq(d(f, x), back))
        return Traced(f, listOf(Step("Special function", "No antiderivative in elementary functions: the answer is written with $name.", eq(int(e, x), f), substeps = steps)))
    }

    private val SPECIAL_NAMES = mapOf("si" to "the sine integral Si", "ci" to "the cosine integral Ci", "ei" to "the exponential integral Ei", "li" to "the logarithmic integral li",
        "shi" to "Shi", "chi" to "Chi", "erf" to "the error function erf", "erfi" to "erfi", "fresnels" to "the Fresnel integral S", "fresnelc" to "the Fresnel integral C",
        "gammainc" to "the incomplete gamma function \$\\Gamma(s, x)\$", "ellipticf" to "the elliptic integral F", "elliptice" to "the elliptic integral E", "polylog" to "the polylogarithm Li")
    private val SPECIAL_DEFS = mapOf(
        "si" to "\\operatorname{Si}'(u) = \\frac{\\sin u}{u}", "ci" to "\\operatorname{Ci}'(u) = \\frac{\\cos u}{u}",
        "ei" to "\\operatorname{Ei}'(u) = \\frac{e^u}{u}", "li" to "\\operatorname{li}'(u) = \\frac{1}{\\ln u}",
        "shi" to "\\operatorname{Shi}'(u) = \\frac{\\sinh u}{u}", "chi" to "\\operatorname{Chi}'(u) = \\frac{\\cosh u}{u}",
        "erf" to "\\operatorname{erf}'(u) = \\frac{2}{\\sqrt{\\pi}} e^{-u^2}", "erfi" to "\\operatorname{erfi}'(u) = \\frac{2}{\\sqrt{\\pi}} e^{u^2}",
        "fresnels" to "S'(u) = \\sin\\frac{\\pi u^2}{2}", "fresnelc" to "C'(u) = \\cos\\frac{\\pi u^2}{2}",
        "gammainc" to "\\frac{\\partial \\Gamma(s, u)}{\\partial u} = -u^{s-1} e^{-u}", "ellipticf" to "F'(\\varphi \\mid m) = \\frac{1}{\\sqrt{1 - m\\sin^2\\varphi}}",
        "elliptice" to "E'(\\varphi \\mid m) = \\sqrt{1 - m\\sin^2\\varphi}", "polylog" to "\\operatorname{Li}_2'(u) = -\\frac{\\ln(1 - u)}{u}",
    )

    /** x⁴ + px² + q: the factorization into two quadratics, then partial fractions. */
    private fun quarticSteps(e: Expr, x: Sym, f: Expr): Traced? {
        val (_, d) = Algebra.together(e)
        val q = Algebra.qpoly(d, x) ?: return null
        if (q.degree != 4) return null
        val m = q.scale(q.lead.reciprocal())
        val pp = m[2]; val qq = m[0]
        val r = Algebra.simplify(com.example.cas.cas.sqrt(Num(qq)))
        val s = Algebra.simplify(com.example.cas.cas.sqrt(sub(mul(two, r), Num(pp))))
        val f1q = add(p(x, two), mul(s, x), r); val f2q = add(p(x, two), neg(mul(s, x)), r)
        val steps = listOf(
            Step("Factor the quartic", "\$x^4 + px^2 + q = (x^2 + r)^2 - (2r - p)x^2 = (x^2 + sx + r)(x^2 - sx + r)\$, \$r = \\sqrt{q}\$, \$s = \\sqrt{2r - p}\$.", eq(ex(m.toExpr(x)), line("(", f1q, ")", "(", f2q, ")"))),
            Step("Partial fractions", "Split over the two quadratics; each gives a logarithm and an arctangent.", eq(int(e, x), f)),
        )
        return Traced(f, listOf(Step("Quartic denominator", null, eq(int(e, x), f), substeps = steps)))
    }

    // ---- Substitutions written out ----------------------------------------------------------------

    /**
     * A substitution: its name and why, the new variable, the integrand in it (dx included), the
     * new variable in terms of x (to put back), and the substitution itself as a line of math.
     */
    private class Sub(val title: String, val text: String, val t: Sym, val integrand: Expr, val back: Expr, val setup: MathRow)

    /** ∫ f dx = ∫ g dt, then ∫ g dt traced, then t put back, checked by differentiating. */
    private fun substitutionSteps(e: Expr, x: Sym, sub: Sub, depth: Int): Traced? {
        val g = Algebra.simplify(sub.integrand)
        if (!g.freeOf(x)) return null
        val inner = trace(g, sub.t, depth + 1) ?: return null
        val back = Algebra.simplify(inner.result.subst(sub.t, sub.back))
        if (!agrees(Algebra.simplify(Calculus.diff(back, x)), e, x)) return null
        return Traced(back, listOf(
            Step(sub.title, sub.text, sub.setup),
            Step("The new integral", "Everything in terms of \$${sub.t.name}\$, \$d${x.name}\$ included.", eq(int(e, x), int(g, sub.t)), substeps = inner.steps),
            Step("Back-substitute", "Put \$${sub.t.name} = ${lx(sub.back)}\$ back.", eq(inner.result, back)),
        ), SubUsed(sub.t, sub.back, g, inner.result))
    }

    /** A fresh letter for the new variable. */
    private fun fresh(e: Expr, vararg names: String): Sym = Sym(names.first { it !in e.freeVars() })

    /** [e] with every part matching [pred] replaced by [repl] of it, rebuilt through the simplifying builders. */
    private fun replace(e: Expr, pred: (Expr) -> Boolean, repl: (Expr) -> Expr): Expr = when {
        pred(e) -> repl(e)
        e is Add -> add(e.terms.map { replace(it, pred, repl) })
        e is Mul -> mul(e.factors.map { replace(it, pred, repl) })
        e is Pow -> com.example.cas.cas.pow(replace(e.base, pred, repl), replace(e.exp, pred, repl))
        e is Fn -> com.example.cas.cas.Simplify.function(e.name, e.args.map { replace(it, pred, repl) })
        else -> e
    }

    private fun substitutionFor(method: String, e: Expr, x: Sym): Sub? = when (method) {
        "sqrtQuadratic" -> trigSub(e, x)
        "weierstrass" -> weierstrassSub(e, x)
        "radical" -> radicalSub(e, x)
        "powerSub" -> powerSub(e, x)
        "expSub" -> expSub(e, x)
        "logSub" -> logSub(e, x)
        "sinCos" -> oddPowerSub(e, x)
        else -> null
    }

    /** √Q: x − h = r sin θ, r tan θ or r sec θ, every Q^(p/2) becoming (√k cos θ)^p and so on. */
    private fun trigSub(e: Expr, x: Sym): Sub? {
        var q: Expr? = null
        fun walk(z: Expr) {
            if (z is Pow && z.exp is Num && (z.exp as Num).q.den.toInt() == 2 && !z.base.freeOf(x)) q = z.base
            z.children.forEach { walk(it) }
        }
        walk(e)
        val quad = q ?: return null
        val cs = Algebra.coefficients(Algebra.expand(quad), x)?.map { (it as? Num)?.q } ?: return null
        if (cs.size != 3 || cs.any { it == null }) return null
        val (c0, c1, c2) = cs.map { it!! }
        val two = com.example.cas.math.Rational.of(2)
        val h = Num(-c1 / (c2 * two))
        val k = c0 - c1 * c1 / (c2 * com.example.cas.math.Rational.of(4))
        val r = Algebra.simplify(com.example.cas.cas.sqrt(Num((k / c2).abs())))
        val rk = Algebra.simplify(com.example.cas.cas.sqrt(Num(k.abs())))
        val th = fresh(e, "θ", "φ")
        val (trig, root, dx, back, name) = when {
            c2.signum < 0 && k.signum > 0 -> Quint(com.example.cas.cas.fn("sin", th), mul(rk, com.example.cas.cas.fn("cos", th)), mul(r, com.example.cas.cas.fn("cos", th)),
                com.example.cas.cas.fn("asin", div(sub(x, h), r)), "sine")
            c2.signum > 0 && k.signum > 0 -> Quint(com.example.cas.cas.fn("tan", th), mul(rk, com.example.cas.cas.pow(com.example.cas.cas.fn("cos", th), MINUS_ONE)),
                mul(r, com.example.cas.cas.pow(com.example.cas.cas.fn("cos", th), Num(-2))), com.example.cas.cas.fn("atan", div(sub(x, h), r)), "tangent")
            c2.signum > 0 && k.signum < 0 -> Quint(com.example.cas.cas.pow(com.example.cas.cas.fn("cos", th), MINUS_ONE), mul(rk, com.example.cas.cas.fn("tan", th)),
                mul(r, com.example.cas.cas.fn("tan", th), com.example.cas.cas.pow(com.example.cas.cas.fn("cos", th), MINUS_ONE)), com.example.cas.cas.fn("acos", div(r, sub(x, h))), "secant")
            else -> return null
        }
        val xOf = add(h, mul(r, trig))
        val target = Algebra.expand(quad)
        // Q^(p/2) → root^p; x elsewhere → h + r·trig.
        val replaced = replace(e, { z -> z is Pow && z.exp is Num && (z.exp as Num).q.den.toInt() == 2 && Printer.plain(Algebra.expand(z.base)) == Printer.plain(target) }) { z ->
            com.example.cas.cas.pow(root, Num((z as Pow).exp.let { (it as Num).q * two }))
        }
        val integrand = mul(replaced.subst(x, xOf), dx)
        val (_, identity) = when (name) { "sine" -> 0 to "1 - \\sin^2${th.name} = \\cos^2${th.name}"; "tangent" -> 0 to "1 + \\tan^2${th.name} = \\sec^2${th.name}"; else -> 0 to "\\sec^2${th.name} - 1 = \\tan^2${th.name}" }
        val shift = if (h == ZERO) ex(x) else ex(sub(x, h))
        return Sub(
            "Trigonometric substitution", "A $name substitution, since \$$identity\$: the root becomes ${tx(root)}.",
            th, integrand, back,
            line(shift, "=", mul(r, trig), ",", "  ", "d", x.name, "=", dx, " ", "d", th.name),
        )
    }

    private data class Quint(val a: Expr, val b: Expr, val c: Expr, val d: Expr, val e: String)

    /** t = tan(u/2) for a rational function of sin u and cos u. */
    private fun weierstrassSub(e: Expr, x: Sym): Sub? {
        var u: Expr? = null
        fun walk(z: Expr) { if (z is Fn && z.name in setOf("sin", "cos", "tan") && !z.args[0].freeOf(x)) u = z.args[0]; z.children.forEach { walk(it) } }
        walk(e)
        val arg = u ?: return null
        val a = linear(arg, x) ?: return null
        val t = fresh(e, "t", "s")
        val one = com.example.cas.cas.ONE
        val den = add(one, com.example.cas.cas.pow(t, com.example.cas.cas.TWO))
        val replaced = replace(e, { z -> z is Fn && z.args.singleOrNull() == arg && z.name in setOf("sin", "cos", "tan") }) { z ->
            when ((z as Fn).name) {
                "sin" -> div(mul(com.example.cas.cas.TWO, t), den)
                "cos" -> div(sub(one, com.example.cas.cas.pow(t, com.example.cas.cas.TWO)), den)
                else -> div(mul(com.example.cas.cas.TWO, t), sub(one, com.example.cas.cas.pow(t, com.example.cas.cas.TWO)))
            }
        }
        if (!replaced.freeOf(x)) return null
        val integrand = mul(replaced, div(com.example.cas.cas.TWO, mul(a, den)))
        return Sub(
            "Weierstrass substitution", "\$\\sin u = \\frac{2t}{1 + t^2}\$, \$\\cos u = \\frac{1 - t^2}{1 + t^2}\$: the integrand becomes a rational function of \$t\$.",
            t, integrand, com.example.cas.cas.fn("tan", div(arg, com.example.cas.cas.TWO)),
            line(t, "=", com.example.cas.cas.fn("tan", div(arg, com.example.cas.cas.TWO)), ",", "  ", "d", x.name, "=", div(com.example.cas.cas.TWO, mul(a, den)), " ", "d", t.name),
        )
    }

    /** u = (ax + b)^(1/q): x = (u^q − b)/a. */
    private fun radicalSub(e: Expr, x: Sym): Sub? {
        var base: Expr? = null; var q = 1
        fun walk(z: Expr) {
            if (z is Pow && z.exp is Num && !(z.exp as Num).q.isInteger && linear(z.base, x) != null) { base = z.base; q = maxOf(q, (z.exp as Num).q.den.toInt()) }
            z.children.forEach { walk(it) }
        }
        walk(e)
        val b = base ?: return null
        val a = linear(b, x)!!
        val c = Algebra.simplify(b.subst(x, ZERO))
        val u = fresh(e, "u", "w")
        val xOf = div(sub(com.example.cas.cas.pow(u, Num(q.toLong())), c), a)
        val replaced = replace(e, { z -> z is Pow && z.base == b && z.exp is Num }) { z -> com.example.cas.cas.pow(u, mul((z as Pow).exp, Num(q.toLong()))) }
        val dx = div(mul(Num(q.toLong()), com.example.cas.cas.pow(u, Num((q - 1).toLong()))), a)
        return Sub(
            "Rationalizing substitution", "Every root of ${tx(b)} becomes a power of \$u\$, so the integrand is a rational function of \$u\$.",
            u, mul(replaced.subst(x, xOf), dx), com.example.cas.cas.pow(b, Num(com.example.cas.math.Rational.of(1, q.toLong()))),
            line(u, "=", com.example.cas.cas.pow(b, Num(com.example.cas.math.Rational.of(1, q.toLong()))), ",", "  ", x.name, "=", xOf, ",", "  ", "d", x.name, "=", dx, " ", "d", u.name),
        )
    }

    /** u = xⁿ when x^(n−1) dx sits beside a function of xⁿ. */
    private fun powerSub(e: Expr, x: Sym): Sub? {
        val u = fresh(e, "u", "w")
        for (n in 2..6) {
            val g = Algebra.simplify(div(e, mul(Num(n.toLong()), com.example.cas.cas.pow(x, Num((n - 1).toLong())))))
            val inU = replace(g, { z -> z is Pow && z.base == x && z.exp is Num && (z.exp as Num).q.isInteger && (z.exp as Num).q.num.toInt() % n == 0 }) { z ->
                com.example.cas.cas.pow(u, Num((z as Pow).exp.let { (it as Num).q.num.toLong() / n }))
            }
            if (inU.freeOf(x)) return Sub(
                "Substitution \$u = ${x.name}^{$n}\$", "\$${x.name}^{${n - 1}}\\,d${x.name}\$ is there beside a function of \$${x.name}^{$n}\$.",
                u, inU, com.example.cas.cas.pow(x, Num(n.toLong())),
                line(u, "=", com.example.cas.cas.pow(x, Num(n.toLong())), ",", "  ", "d", u.name, "=", mul(Num(n.toLong()), com.example.cas.cas.pow(x, Num((n - 1).toLong()))), " ", "d", x.name),
            )
        }
        return null
    }

    /** u = e^(gx) when everything is a function of exponentials of x. */
    private fun expSub(e: Expr, x: Sym): Sub? {
        val coefficients = ArrayList<com.example.cas.math.Rational>()
        fun walk(z: Expr) { if (z is Pow && z.base == E) linear(z.exp, x)?.let { (it as? Num)?.q?.let { q -> coefficients += q.abs() } }; z.children.forEach { walk(it) } }
        walk(e)
        val g = coefficients.filter { it.signum > 0 }.minOrNull() ?: return null
        val u = fresh(e, "u", "w")
        val gx = mul(Num(g), x)
        val replaced = replace(e, { z -> z is Pow && z.base == E && linear(z.exp, x) != null }) { z ->
            val exp = (z as Pow).exp
            val k = div(linear(exp, x)!!, Num(g))
            mul(com.example.cas.cas.pow(E, Algebra.simplify(exp.subst(x, ZERO))), com.example.cas.cas.pow(u, k))
        }
        if (!replaced.freeOf(x)) return null
        val integrand = div(replaced, mul(Num(g), u))
        return Sub(
            "Substitution \$u = e^{${lx(gx)}}\$", "Everything is a function of \$e^{${lx(gx)}}\$; \$d${x.name} = \\frac{du}{${lx(Num(g))}u}\$.",
            u, integrand, com.example.cas.cas.pow(E, gx),
            line(u, "=", com.example.cas.cas.pow(E, gx), ",", "  ", "d", x.name, "=", div(com.example.cas.cas.ONE, mul(Num(g), u)), " ", "d", u.name),
        )
    }

    /** x = eᵘ for a function of ln x. */
    private fun logSub(e: Expr, x: Sym): Sub? {
        val u = fresh(e, "u", "w")
        val replaced = replace(e, { z -> z is Fn && z.name == "ln" && z.args[0] == x }) { u }
        val integrand = mul(replaced.subst(x, com.example.cas.cas.pow(E, u)), com.example.cas.cas.pow(E, u))
        return Sub(
            "Substitution \$${x.name} = e^u\$", "A function of \$\\ln ${x.name}\$: with \$u = \\ln ${x.name}\$, \$d${x.name} = e^u\\,du\$.",
            u, integrand, com.example.cas.cas.fn("ln", x),
            line(u, "=", com.example.cas.cas.fn("ln", x), ",", "  ", x.name, "=", com.example.cas.cas.pow(E, u), ",", "  ", "d", x.name, "=", com.example.cas.cas.pow(E, u), " ", "d", u.name),
        )
    }

    /** An odd power of sin (cos): keep one, the rest through cos² (sin²), then w = cos u (sin u). */
    private fun oddPowerSub(e: Expr, x: Sym): Sub? {
        var arg: Expr? = null; var m = 0; var n = 0
        fun walk(z: Expr) {
            when {
                z is Fn && z.name == "sin" -> { arg = z.args[0]; m = maxOf(m, 1) }
                z is Fn && z.name == "cos" -> { arg = z.args[0]; n = maxOf(n, 1) }
                z is Pow && z.base is Fn && z.exp is Num && (z.exp as Num).q.isInteger -> {
                    val k = (z.exp as Num).q.num.toInt(); arg = (z.base as Fn).args[0]
                    if ((z.base as Fn).name == "sin") m = k else if ((z.base as Fn).name == "cos") n = k
                }
            }
            if (!(z is Pow && z.base is Fn)) z.children.forEach { walk(it) }
        }
        walk(e)
        val u = arg ?: return null
        val a = linear(u, x) ?: return null
        val w = fresh(e, "w", "s")
        val sinOdd = m % 2 != 0 && m > 0
        if (!sinOdd && !(n % 2 != 0 && n > 0)) return null
        val (keep, other) = if (sinOdd) "sin" to "cos" else "cos" to "sin"
        val keepFn = com.example.cas.cas.fn(keep, u)
        // f = keep(u)·g(other(u)): divide out one keep, write keep² as 1 − other², put w for other(u).
        val rest = Algebra.simplify(div(e, keepFn))
        val inW = replace(rest, { z -> z is Pow && z.base == keepFn && z.exp is Num }) { z ->
            com.example.cas.cas.pow(sub(com.example.cas.cas.ONE, com.example.cas.cas.pow(w, com.example.cas.cas.TWO)), div((z as Pow).exp, com.example.cas.cas.TWO))
        }.let { replace(it, { z -> z == com.example.cas.cas.fn(other, u) }) { w } }
        if (!inW.freeOf(x)) return null
        val integrand = mul(inW, if (sinOdd) neg(div(com.example.cas.cas.ONE, a)) else div(com.example.cas.cas.ONE, a))
        return Sub(
            "Odd power of ${if (sinOdd) "sine" else "cosine"}", "Keep one \$\\$keep u\$, write the others with \$\\$keep^2 u = 1 - \\$other^2 u\$, then substitute \$w = \\$other u\$.",
            w, integrand, com.example.cas.cas.fn(other, u),
            line(w, "=", com.example.cas.cas.fn(other, u), ",", "  ", "d", w.name, "=", mul(if (sinOdd) neg(a) else a, keepFn), " ", "d", x.name),
        )
    }

    /** The name and explanation of an integration method, with the substitution it makes where there is one. */
    private fun describe(method: String, e: Expr, x: Sym): Pair<String, String> {
        val v = x.name
        return when (method) {
            "rationalFull" -> "Partial fractions" to "Factor the denominator into linear and quadratic pieces and split the fraction; linear pieces give logarithms, quadratic ones \$\\ln\$ and \$\\tan^{-1}\$ (after completing the square)."
            "gaussian" -> "Complete the square" to "The exponent is quadratic: write it as \$-a($v - h)^2 + k\$, so the integral becomes the Gaussian one, \$\\frac{\\sqrt{\\pi}}{2}\\operatorname{erf}\$."
            "overLinear" -> "Special function" to "A sine, cosine or exponential over a linear term: substitute \$u\$ = the linear term; the result is \$\\operatorname{Si}\$, \$\\operatorname{Ci}\$, \$\\operatorname{Shi}\$, \$\\operatorname{Chi}\$ or \$\\operatorname{Ei}\$, functions defined by these integrals."
            "fresnel" -> "Fresnel integrals" to "\$\\sin\$ or \$\\cos\$ of a quadratic: complete the square, then \$u = \\sqrt{2/\\pi}\\,($v - h)\$ gives the Fresnel integrals \$S\$ and \$C\$."
            "logIntegral" -> "Logarithmic integral" to "Substitute \$u = \\ln $v\$: \$\\frac{$v^k}{\\ln $v}\$ becomes \$\\frac{e^{(k+1)u}}{u}\$, whose integral is \$\\operatorname{Ei}\$ (\$\\operatorname{li}\$ when \$k = 0\$)."
            "incompleteGamma" -> "Incomplete gamma function" to "Substitute \$u = a$v\$: \$$v^p e^{-a$v}\$ becomes \$u^p e^{-u}\$, the integrand of \$\\Gamma(p + 1, u)\$."
            "dilog" -> "Dilogarithm" to "A logarithm over a linear term: the integral of \$-\\frac{\\ln(1 - u)}{u}\$ defines \$\\operatorname{Li}_2(u)\$."
            "elliptic" -> "Elliptic integral" to "\$\\sqrt{c_0 + c_1\\sin^2 u}\$: written as an elliptic integral \$F\$ or \$E\$ with parameter \$m = -c_1/c_0\$."
            "sqrtQuadratic" -> trigSubstitution(e, x)
            "sinCos" -> sinCosMethod(e, x)
            "expTrig" -> "By parts twice" to "Integrate \$e^{a$v}\\sin b$v\$ by parts twice: the same integral comes back on the right, so solve for it."
            "quartic" -> "Factor the quartic" to "\$$v^4 + p$v^2 + q = ($v^2 + s$v + r)($v^2 - s$v + r)\$ with \$r = \\sqrt{q}\$, \$s = \\sqrt{2r - p}\$; then partial fractions."
            "parts" -> "Integration by parts" to "\$u\$ = the logarithm or inverse function (simpler once differentiated), \$dv\$ = the rest: \$\\int u\\,dv = uv - \\int v\\,du\$."
            "multipleAngles" -> "Multiple angles" to "Write \$\\sin k$v\$ and \$\\cos k$v\$ with \$\\sin $v\$ and \$\\cos $v\$ (\$\\sin 2u = 2\\sin u\\cos u\$, \$\\cos 2u = 2\\cos^2 u - 1\$, …), then integrate."
            "radical" -> radicalSubstitution(e, x)
            "powerSub" -> "Substitution \$u = $v^n\$" to "\$$v^{n-1}\\,d$v\$ appears beside a function of \$$v^n\$: with \$u = $v^n\$, \$du = n$v^{n-1}\\,d$v\$."
            "expSub" -> "Substitution \$u = e^{g$v}\$" to "Everything is a function of \$e^{g$v}\$: with \$u = e^{g$v}\$, \$d$v = \\frac{du}{gu}\$, the integrand becomes a rational function of \$u\$."
            "logSub" -> "Substitution \$$v = e^u\$" to "A function of \$\\ln $v\$: with \$u = \\ln $v\$, \$$v = e^u\$ and \$d$v = e^u\\,du\$."
            "productToSum" -> "Product-to-sum" to "\$\\sin A\\cos B = \\frac{1}{2}[\\sin(A + B) + \\sin(A - B)]\$ (and the like) turn products of sines and cosines into single ones."
            "weierstrass" -> "Weierstrass substitution" to "\$t = \\tan\\frac{u}{2}\$: \$\\sin u = \\frac{2t}{1 + t^2}\$, \$\\cos u = \\frac{1 - t^2}{1 + t^2}\$, \$du = \\frac{2\\,dt}{1 + t^2}\$; a rational function of \$t\$, by partial fractions."
            "hyperbolic" -> "Exponential form" to "Write \$\\sinh\$, \$\\cosh\$ and \$\\tanh\$ with \$e^u\$ and \$e^{-u}\$, then integrate the exponentials."
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
        if (cs == null || cs.size != 3 || cs.any { it == null }) return "Substitution" to "\$R($v, \\sqrt{Q})\$: rationalize the root with a substitution."
        val (c0, c1, c2) = cs.map { it!! }
        val h = -c1 / (c2 * com.example.cas.math.Rational.of(2))
        val k = c0 - c1 * c1 / (c2 * com.example.cas.math.Rational.of(4))
        // All LaTeX, for the $ … $ in the text.
        val shift = if (h.signum == 0) v else "$v ${if (h.signum > 0) "-" else "+"} ${lx(Num(h.abs()))}"
        val square = "${if (c2 == com.example.cas.math.Rational.ONE) "" else lx(Num(c2))}($shift)^2 ${if (k.signum >= 0) "+" else "-"} ${lx(Num(k.abs()))}"
        val r = lx(Algebra.simplify(com.example.cas.cas.sqrt(Num((k / c2).abs())))).let { if (it == "1") "" else it }
        val rk = lx(Algebra.simplify(com.example.cas.cas.sqrt(Num(k.abs())))).let { if (it == "1") "" else it }
        val (sub, identity, rest) = when {
            c2.signum < 0 && k.signum > 0 -> Triple("$shift = $r\\sin\\theta", "1 - \\sin^2\\theta = \\cos^2\\theta", "the root becomes \$$rk\\cos\\theta\$ and \$d$v = $r\\cos\\theta\\,d\\theta\$")
            c2.signum > 0 && k.signum > 0 -> Triple("$shift = $r\\tan\\theta", "1 + \\tan^2\\theta = \\sec^2\\theta", "the root becomes \$$rk\\sec\\theta\$ and \$d$v = $r\\sec^2\\theta\\,d\\theta\$")
            c2.signum > 0 && k.signum < 0 -> Triple("$shift = $r\\sec\\theta", "\\sec^2\\theta - 1 = \\tan^2\\theta", "the root becomes \$$rk\\tan\\theta\$ and \$d$v = $r\\sec\\theta\\tan\\theta\\,d\\theta\$")
            else -> return "Substitution" to "Complete the square: \$$square\$."
        }
        val completed = if (c1.signum == 0) "" else "Complete the square: \$Q = $square\$. "
        return "Trigonometric substitution" to "${completed}Let \$$sub\$. Since \$$identity\$, $rest. Integrate in \$\\theta\$, then put \$\\theta\$ back in terms of \$$v\$."
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
            m % 2 != 0 && m > 0 -> "Odd power of sine" to "Keep one \$\\sin u\$, write the rest with \$\\sin^2 u = 1 - \\cos^2 u\$, then substitute \$w = \\cos u\$ (\$dw = -\\sin u\\,du\$)."
            n % 2 != 0 && n > 0 -> "Odd power of cosine" to "Keep one \$\\cos u\$, write the rest with \$\\cos^2 u = 1 - \\sin^2 u\$, then substitute \$w = \\sin u\$ (\$dw = \\cos u\\,du\$)."
            else -> "Half-angle formulas" to "Even powers: \$\\sin^2 u = \\frac{1 - \\cos 2u}{2}\$ and \$\\cos^2 u = \\frac{1 + \\cos 2u}{2}\$ lower the powers until each piece integrates directly."
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
        return "Rationalizing substitution" to "Let \$u = (${lx(p.base)})^{1/$q}\$, so \$${lx(p.base)} = u^{$q}\$ and every root becomes a power of \$u\$; the integrand is then a rational function of \$u\$."
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
            e == x -> "Power rule" to "\$\\int ${x.name}^n\\,d${x.name} = \\frac{${x.name}^{n+1}}{n + 1}\$, with \$n = 1\$."
            e is Pow && e.exp.freeOf(x) && linear(e.base, x) != null ->
                if (e.exp == MINUS_ONE) "Logarithm" to "\$\\int \\frac{1}{u}\\,du = \\ln|u|\$" + inner(e.base, x)
                else "Power rule" to "\$\\int u^n\\,du = \\frac{u^{n+1}}{n + 1}\$, with \$n = ${lx(e.exp)}\$" + inner(e.base, x)
            e is Pow && e.base.freeOf(x) && linear(e.exp, x) != null ->
                "Exponential" to (if (e.base == E) "\$\\int e^u\\,du = e^u\$" else "\$\\int a^u\\,du = \\frac{a^u}{\\ln a}\$") + inner(e.exp, x)
            e is Fn && e.args.size == 1 && e.name in TABLE && linear(e.args[0], x) != null ->
                "Standard integral" to TABLE.getValue(e.name) + inner(e.args[0], x)
            else -> return null
        }
        val f = Calculus.integrate(e, x) ?: return null
        return Traced(f, listOf(Step(title, text, eq(int(e, x), f))))
    }

    private fun inner(u: Expr, x: Sym): String {
        val a = linear(u, x) ?: return "."
        return if (u == x) "." else ", with \$u = ${lx(u)}\$" + (if (a == com.example.cas.cas.ONE) "." else ", so divide by ${tx(a)}.")
    }

    private val TABLE = mapOf(
        "sin" to "\$\\int \\sin u\\,du = -\\cos u\$", "cos" to "\$\\int \\cos u\\,du = \\sin u\$", "tan" to "\$\\int \\tan u\\,du = -\\ln|\\cos u|\$",
        "sinh" to "\$\\int \\sinh u\\,du = \\cosh u\$", "cosh" to "\$\\int \\cosh u\\,du = \\sinh u\$", "tanh" to "\$\\int \\tanh u\\,du = \\ln\\cosh u\$",
        "ln" to "\$\\int \\ln u\\,du = u\\ln u - u\$", "asin" to "\$\\int \\sin^{-1} u\\,du = u\\sin^{-1} u + \\sqrt{1 - u^2}\$",
        "acos" to "\$\\int \\cos^{-1} u\\,du = u\\cos^{-1} u - \\sqrt{1 - u^2}\$", "atan" to "\$\\int \\tan^{-1} u\\,du = u\\tan^{-1} u - \\frac{\\ln(1 + u^2)}{2}\$",
    )

    /** A fraction of polynomials: split into partial fractions, then each piece. */
    private fun partialFractions(e: Expr, x: Sym, depth: Int): Traced? {
        val (n, d) = Algebra.together(e)
        if (d.freeOf(x) || Algebra.qpoly(n, x) == null || Algebra.qpoly(d, x) == null) return null
        val split = runCatching { Algebra.apart(e, x) }.getOrNull() ?: return null
        if (split !is Add || same(split, e)) return null
        val inner = trace(split, x, depth + 1) ?: return null
        val divide = Algebra.qpoly(n, x)!!.degree >= Algebra.qpoly(d, x)!!.degree
        return Traced(inner.result, listOf(Step(
            if (divide) "Divide, then split" else "Partial fractions",
            if (divide) "The top's degree isn't below the bottom's: divide first (polynomial long division), then split what's left into simpler fractions." else "Split the fraction into simpler ones.",
            eq(ex(e), ex(split)),
        )) + inner.steps)
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
        val choose = Step("Choose u and dv", "\$u = ${lx(u)}\$ (it gets simpler when differentiated), \$dv = ${lx(dv)}\\,d${x.name}\$.",
            line(com.example.cas.editor.MathCodec.decode("'u;'=;"), u, ",", " ", "v", "=", v))
        return Traced(result, listOf(Step(
            "Integration by parts", "\$\\int u\\,dv = uv - \\int v\\,du\$",
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
            // u linear in x only renames the variable (the table already handles linear insides).
            if (du == ZERO || Algebra.simplify(du).freeOf(x)) continue
            val q = Algebra.simplify(div(e, du))
            val inT = q.subst(u, t)
            if (!inT.freeOf(x) || inT == q) continue
            val inner = trace(inT, t, depth + 1) ?: continue
            val result = inner.result.subst(t, u)
            return Traced(result, listOf(
                Step("Substitution", "Let \$$uName = ${lx(u)}\$, so \$d$uName = ${lx(du)}\\,d${x.name}\$.", eq(int(e, x), int(inT, t)), substeps = inner.steps),
                Step("Back-substitute", "Put \$$uName = ${lx(u)}\$ back.", eq(inner.result, result)),
            ), SubUsed(t, u, inT, inner.result))
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
        val answer = ans(question, value)
        val steps = ArrayList<Step>()
        val c = Numeric.eval(center); val r = Numeric.real(radius)
        steps += Step("The circle", "Center ${tx(center)}, radius ${tx(radius)}, run counterclockwise.", ex(spec))
        val (_, den) = Algebra.together(body)
        val poles: List<Expr>? = if (den.freeOf(z)) emptyList() else runCatching {
            val sol = Algebra.solve(Eq(den, ZERO), z)
            (if (sol is Seq) sol.items else listOf(sol)).mapNotNull { (it as? Eq)?.takeIf { e -> e.lhs == z }?.rhs }
        }.getOrNull()?.takeIf { it.isNotEmpty() }
        if (poles == null) {
            steps += Step("Numerical", "The singularities can't be found exactly (or \$f\$ isn't a fraction of polynomials), so the integral is computed numerically around the circle.", answer, Kind.Note)
            return Solution("Numerical", steps, answer)
        }
        if (poles.isEmpty()) {
            steps += Step("Cauchy's theorem", "\$f\$ has no poles, so it's analytic inside the circle and the integral is \$0\$.", answer, Kind.Result)
            return Solution("Cauchy's theorem", steps, answer)
        }
        steps += Step("Singularities", "Where the denominator is \$0\$.", line(*poles.flatMapIndexed { k, p -> (if (k > 0) listOf(",", " ") else emptyList()) + listOf<Any>(z.name, "=", p) }.toTypedArray()))
        val within = poles.filter { p -> (Numeric.eval(p) - c).abs() < r }
        val outside = poles - within.toSet()
        steps += Step(
            "Inside the circle",
            if (within.isEmpty()) "None of them is inside \$|${z.name} - ${lx(center)}| < ${lx(radius)}\$."
            else "Only poles inside count" + (if (outside.isEmpty()) "; all of them are." else "; ${outside.joinToString { tx(it) }} ${if (outside.size == 1) "is" else "are"} outside."),
            if (within.isEmpty()) null else line(*within.flatMapIndexed { k, p -> (if (k > 0) listOf(",", " ") else emptyList()) + listOf<Any>(z.name, "=", p) }.toTypedArray()),
        )
        if (within.isEmpty()) {
            steps += Step("Cauchy's theorem", "No poles inside: the integral is \$0\$.", answer, Kind.Result)
            return Solution("Cauchy's theorem", steps, answer)
        }
        val residues = within.map { p ->
            val res = com.example.cas.graph.ComplexIntegrals.residue(body, z, p)
            val order = orderOf(den, z, p)
            val how = if (order == 1) "Simple pole: \$\\operatorname{Res} = \\lim_{${z.name} \\to a} (${z.name} - a) f(${z.name})\$."
            else "Pole of order \$$order\$: \$\\operatorname{Res} = \\frac{1}{${order - 1}!}\$ times the limit of the ${ordinal(order - 1)} derivative of \$(${z.name} - a)^{$order} f(${z.name})\$."
            Step("Residue at \$${z.name} = ${lx(p)}\$", how, line(com.example.cas.editor.MathCodec.decode("'R;'e;'s;"), "(", z.name, "=", p, ")", "=", res)) to res
        }
        steps += residues.map { it.first }
        val total = Algebra.simplify(add(residues.map { it.second }))
        steps += Step("Residue theorem", "\$\\oint f\\,dz = 2\\pi i\$ times the sum of the residues inside.", line(question, "=", "2", "π", "i", "·", paren(total), "=", value), Kind.Result)
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
            steps += Step("Evaluate", "Put \$${x.name} = ${lx(a)}\$ into the derivative.", line(current, ",", " ", x.name, "=", a, "  ", "⇒", " ", value))
        }
        val answer = ans(question, value)
        steps += Step("Answer", null, answer, Kind.Result)
        return Solution(methodOf(steps), steps, answer)
    }

    /** The steps to d/d[x] of [e], rule by rule. */
    private fun dtrace(e: Expr, x: Sym, depth: Int): Traced {
        val result = Calculus.diff(e, x)
        fun leaf(title: String, text: String) = Traced(result, listOf(Step(title, text, eq(d(e, x), result))))
        if (depth > 8) return leaf("Differentiate", "Rule by rule, as above.")
        if (e.freeOf(x)) return leaf("Constant rule", "The derivative of a constant is 0.")
        if (e == x) return leaf("Power rule", "\$\\frac{d}{d${x.name}} ${x.name} = 1\$.")
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
                return Traced(result, listOf(Step("Constant multiple", "Keep ${tx(c)} in front.", eq(d(e, x), line(paren(c), d(r, x))), substeps = inner.steps)))
            }
            // A quotient u/v: a factor with a negative power.
            val den = rest.filter { it is Pow && it.exp is Num && (it.exp as Num).q.signum < 0 }
            if (den.isNotEmpty() && den.size < rest.size) {
                val u = mul(rest - den.toSet())
                val v = mul(den.map { com.example.cas.cas.pow((it as Pow).base, neg(it.exp)) })
                val du = dtrace(u, x, depth + 1); val dv = dtrace(v, x, depth + 1)
                return Traced(result, listOf(Step(
                    "Quotient rule", "\$\\left(\\frac{u}{v}\\right)' = \\frac{u'v - uv'}{v^2}\$, with \$u = ${lx(u)}\$ and \$v = ${lx(v)}\$.",
                    eq(d(e, x), ex(div(sub(mul(du.result, v), mul(u, dv.result)), com.example.cas.cas.pow(v, com.example.cas.cas.TWO)))),
                    substeps = du.steps + dv.steps,
                )))
            }
            val u = rest.first(); val v = mul(rest.drop(1))
            val du = dtrace(u, x, depth + 1); val dv = dtrace(v, x, depth + 1)
            return Traced(result, listOf(Step(
                "Product rule", "\$(uv)' = u'v + uv'\$, with \$u = ${lx(u)}\$ and \$v = ${lx(v)}\$.",
                eq(d(e, x), ex(add(mul(du.result, v), mul(u, dv.result)))),
                substeps = du.steps + dv.steps,
            )))
        }
        if (e is Pow) {
            val (b, k) = e.base to e.exp
            if (k.freeOf(x)) {
                if (b == x) return leaf("Power rule", "\$\\frac{d}{d${x.name}} ${x.name}^n = n\\,${x.name}^{n-1}\$, with \$n = ${lx(k)}\$.")
                return chain("Power rule", "\$(u^n)' = n\\,u^{n-1} \\cdot u'\$, with \$n = ${lx(k)}\$", e, b, x, depth, result)
            }
            if (b.freeOf(x)) {
                val v = x.name
                if (k == x) return leaf("Exponential", if (b == E) "\$\\frac{d}{d$v} e^$v = e^$v\$" else "\$\\frac{d}{d$v} a^$v = a^$v\\ln a\$")
                return chain("Exponential", if (b == E) "\$(e^u)' = e^u \\cdot u'\$" else "\$(a^u)' = a^u\\ln a \\cdot u'\$", e, k, x, depth, result)
            }
            val y = sym("y")
            val lnRhs = mul(k, f1("ln", b))
            val dln = Algebra.simplify(Calculus.diff(lnRhs, x))
            return Traced(result, listOf(Step(
                "Logarithmic differentiation", "A variable to a variable power: take logarithms first.", eq(d(e, x), result),
                substeps = listOf(
                    Step("Take logarithms", "\$y = ${lx(e)}\$, so \$\\ln y = ${lx(k)} \\cdot \\ln(${lx(b)})\$.", line("ln", " ", y, "=", lnRhs)),
                    Step("Differentiate both sides", "The left side by the chain rule: \$(\\ln y)' = \\frac{y'}{y}\$.", line(MathRow(mutableListOf(com.example.cas.editor.Frac(MathRow(mutableListOf(sym("y"), sym("′"))), MathRow(mutableListOf(sym("y")))))), "=", dln)),
                    Step("Multiply by y", "\$y' = y\$ times the right side.", line(sym("y"), "′", "=", paren(e), "·", paren(dln))),
                ),
            )))
        }
        if (e is Fn && e.args.size == 1 && e.name in DTABLE) {
            val u = e.args[0]
            return if (u == x) leaf("Standard derivative", drule(e.name, x.name, chain = false))
            else chain("Standard derivative", drule(e.name, "u", chain = true), e, u, x, depth, result)
        }
        return leaf("Differentiate", "Using the derivative of each function involved.")
    }

    /** f(u(x)) by the chain rule, with u′ worked out as a smaller step. */
    private fun chain(title: String, rule: String, e: Expr, u: Expr, x: Sym, depth: Int, result: Expr): Traced {
        val du = dtrace(u, x, depth + 1)
        return Traced(result, listOf(Step(
            "$title and chain rule", "$rule, with \$u = ${lx(u)}\$.", eq(d(e, x), result),
            substeps = du.steps,
        )))
    }

    private val DTABLE = setOf("sin", "cos", "tan", "ln", "sinh", "cosh", "asin", "acos", "atan", "tanh", "abs", "erf")

    /** The derivative of a standard function of [v], in LaTeX; with [chain], times u′. */
    private fun drule(name: String, v: String, chain: Boolean): String {
        val t = if (chain) " \\cdot u'" else ""
        val d = if (chain) "u'" else "1"
        return "\$" + when (name) {
            "sin" -> "(\\sin $v)' = \\cos $v$t"
            "cos" -> "(\\cos $v)' = -\\sin $v$t"
            "tan" -> "(\\tan $v)' = (1 + \\tan^2 $v)$t"
            "ln" -> "(\\ln $v)' = \\frac{$d}{$v}"
            "sinh" -> "(\\sinh $v)' = \\cosh $v$t"
            "cosh" -> "(\\cosh $v)' = \\sinh $v$t"
            "asin" -> "(\\sin^{-1} $v)' = \\frac{$d}{\\sqrt{1 - $v^2}}"
            "acos" -> "(\\cos^{-1} $v)' = -\\frac{$d}{\\sqrt{1 - $v^2}}"
            "atan" -> "(\\tan^{-1} $v)' = \\frac{$d}{1 + $v^2}"
            "tanh" -> "(\\tanh $v)' = (1 - \\tanh^2 $v)$t"
            "abs" -> "|$v|' = \\frac{$v}{|$v|}$t"
            else -> "(\\operatorname{erf} $v)' = \\frac{2}{\\sqrt{\\pi}} e^{-$v^2}$t"
        } + "\$"
    }

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
        val answer = ans(question, value)
        val steps = ArrayList<Step>()
        val infinite = Calculus.isInfinite(a)
        // Straight substitution, when it gives a number.
        if (!infinite) {
            val direct = runCatching { Algebra.simplify(body.subst(x, a)) }.getOrNull()
            if (direct != null && finiteValue(direct)) {
                steps += Step("Direct substitution", "\$f\$ is continuous at \$${x.name} = ${lx(a)}\$: put it in.", eq(lim(body, x, a, side), direct))
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
            steps += Step("Highest powers", "Divide the numerator and the denominator by \$${x.name}^{${q.degree}}\$: only the leading terms survive.",
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
            steps += Step("Series expansion", "Expand around \$${x.name} = ${lx(a)}\$; the leading term gives the limit.", eq(ex(body), line(t, "+", "…")))
            steps += Step("Answer", null, answer, Kind.Result)
            return Solution("Series expansion", steps, answer)
        }
        steps += Step("Limit", "From the leading behavior of each part as \$${x.name} \\to ${lx(a)}\$.", answer, Kind.Result)
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
        val answer = ans(question, value)
        val steps = ArrayList<Step>()
        val count = (hi as? Num)?.q?.takeIf { it.isInteger }?.let { h -> (lo as? Num)?.q?.takeIf { it.isInteger }?.let { l -> (h - l).num.toLong() + 1 } }
        // A few terms: write them out.
        if (count != null && count in 1..8) {
            val terms = (0 until count).map { j -> Algebra.simplify(body.subst(k, add(lo, Num(j)))) }
            steps += Step("Write out the terms", "\$${k.name} = ${lx(lo)}, \\ldots, ${lx(hi)}\$.", line(*terms.flatMapIndexed { j, t -> (if (j > 0) listOf<Any>("+") else emptyList()) + listOf<Any>(paren(t)) }.toTypedArray()))
            steps += Step("Add them up", null, answer, Kind.Result)
            return Solution("Adding the terms", steps, answer)
        }
        // Infinite series of known kinds.
        if (Calculus.isPlusInfinity(hi)) {
            geometric(body, k)?.let { r ->
                val first = Algebra.simplify(body.subst(k, lo))
                steps += Step("Geometric series", "Each term is the last times \$r = ${lx(r)}\$; the first term is \$a = ${lx(first)}\$.", line("a", "=", first, ",", "  ", "r", "=", r))
                steps += Step("Converges", "\$|r| < 1\$, so the sum is \$\\frac{a}{1 - r}\$.", line(MathRow(mutableListOf(com.example.cas.editor.Frac(ex(first), ex(sub(com.example.cas.cas.ONE, r))))), "=", value), Kind.Check)
                steps += Step("Answer", null, answer, Kind.Result)
                return Solution("Geometric series", steps, answer)
            }
            pSeries(body, k)?.let { (p, alternating) ->
                steps += Step(if (alternating) "Alternating \$p\$-series" else "\$p\$-series", "The term is \$${if (alternating) "\\pm" else ""}\\frac{1}{${k.name}^{${lx(p)}}}\$.", ex(body))
                steps += Step("Converges", if (alternating) "The terms shrink to \$0\$ and alternate in sign." else "\$p = ${lx(p)} > 1\$, so the series converges.", null, Kind.Check)
                steps += Step(
                    if (alternating) "Dirichlet eta function" else "Riemann zeta function",
                    if (alternating) "\$\\sum_{${k.name}=1}^{\\infty} \\frac{(-1)^{${k.name}+1}}{${k.name}^p} = \\eta(p)\$, and \$\\eta(1) = \\ln 2\$, \$\\eta(p) = (1 - 2^{1-p})\\,\\zeta(p)\$." else "\$\\sum_{${k.name}=1}^{\\infty} \\frac{1}{${k.name}^p} = \\zeta(p)\$; \$\\zeta(2) = \\frac{\\pi^2}{6}\$, \$\\zeta(4) = \\frac{\\pi^4}{90}\$.",
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
            if (lo != com.example.cas.cas.ONE) steps += Step("Shift", "The formulas below start at \$${k.name} = 1\$: subtract the terms before ${tx(lo)}.", null, Kind.Note)
            pieces.forEach { (p, c, formula) ->
                val term = com.example.cas.cas.pow(k, p.toLong())
                val v = runCatching { Algebra.simplify(Calculus.sum(term, k, lo, hi, false)) }.getOrNull()
                steps += Step("Sum of ${if (p == 0) "a constant" else "\$" + k.name + (if (p > 1) "^$p" else "") + "\$"}", formula + if (lo != com.example.cas.cas.ONE) " (shifted to start at ${tx(lo)})." else ".",
                    v?.let { line(paren(c), sigma(term, k, lo, hi), "=", paren(c), paren(it)) })
            }
            steps += Step("Simplify", "Put the pieces together and factor.", answer, Kind.Result)
            return Solution("Power sums", steps, answer)
        }
        geometric(body, k)?.let { r ->
            steps += Step("Geometric sum", "Ratio \$r = ${lx(r)}\$: \$\\sum_{k=0}^{n} a r^k = a\\,\\frac{r^{n+1} - 1}{r - 1}\$.", ex(r))
            steps += Step("Answer", null, answer, Kind.Result)
            return Solution("Geometric sum", steps, answer)
        }
        steps += Step("Closed form", "Found from the form of the term.", answer, Kind.Result)
        return Solution("Closed form", steps, answer)
    }

    private val POWER_SUMS = listOf(
        "\$\\sum_{k=1}^{n} 1 = n\$", "\$\\sum_{k=1}^{n} k = \\frac{n(n + 1)}{2}\$", "\$\\sum_{k=1}^{n} k^2 = \\frac{n(n + 1)(2n + 1)}{6}\$", "\$\\sum_{k=1}^{n} k^3 = \\left(\\frac{n(n + 1)}{2}\\right)^2\$",
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
        val answer = ans(question, value)
        val steps = ArrayList<Step>()
        val count = (hi as? Num)?.q?.takeIf { it.isInteger }?.let { h -> (lo as? Num)?.q?.takeIf { it.isInteger }?.let { l -> (h - l).num.toLong() + 1 } }
        if (count != null && count in 1..8) {
            val fs = (0 until count).map { j -> Algebra.simplify(body.subst(k, add(lo, Num(j)))) }
            steps += Step("Write out the factors", "\$${k.name} = ${lx(lo)}, \\ldots, ${lx(hi)}\$.", line(*fs.flatMapIndexed { j, t -> (if (j > 0) listOf<Any>("·") else emptyList()) + listOf<Any>(paren(t)) }.toTypedArray()))
            steps += Step("Multiply them", null, answer, Kind.Result)
            return Solution("Multiplying the factors", steps, answer)
        }
        if (body.freeOf(k) && !Calculus.isInfinite(hi)) {
            val m = Algebra.simplify(add(sub(hi, lo), com.example.cas.cas.ONE))
            steps += Step("Constant factor", "The same factor ${tx(body)}, ${tx(m)} times.", ex(com.example.cas.cas.pow(body, m)))
            steps += Step("Answer", null, answer, Kind.Result)
            return Solution("Powers", steps, answer)
        }
        if (body == k && lo == com.example.cas.cas.ONE) {
            steps += Step("Factorial", "\$1 \\cdot 2 \\cdot 3 \\cdots ${lx(hi)} = ${lx(hi)}!\$.", answer, Kind.Result)
            return Solution("Factorial", steps, answer)
        }
        // Telescoping: each factor is g(k + 1)/g(k), so everything cancels but the ends.
        val (num, den) = Algebra.together(body)
        val shifted = runCatching { Algebra.simplify(den.subst(k, add(k, com.example.cas.cas.ONE))) }.getOrNull()
        if (!den.freeOf(k) && shifted != null && runCatching { Algebra.simplify(sub(num, shifted)) }.getOrNull() == ZERO) {
            steps += Step("Telescoping", "Each factor is \$\\frac{g(${k.name} + 1)}{g(${k.name})}\$ with \$g(${k.name}) = ${lx(den)}\$: neighbors cancel.", eq(ex(body), ex(div(shifted, den))))
            steps += Step("What's left", "Only the last numerator and the first denominator remain: \$\\frac{g(${lx(add(hi, com.example.cas.cas.ONE))})}{g(${lx(lo)})}\$.", null)
            steps += Step("Answer", null, answer, Kind.Result)
            return Solution("Telescoping product", steps, answer)
        }
        if (Calculus.isPlusInfinity(hi)) {
            steps += Step("Take logarithms", "\$\\ln\$ of the product is the sum of \$\\ln\$ of each factor: a product converges when that sum does.", line("ln", pi(body, k, lo, hi), "=", sigma(com.example.cas.cas.fn("ln", body), k, lo, hi)), Kind.Note)
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
        val answer = ans(question, value)
        val steps = ArrayList<Step>()
        val (_, den) = Algebra.together(f)
        val atPole = !den.freeOf(z) && runCatching { Numeric.eval(Algebra.simplify(den.subst(z, a))) }.getOrNull()?.let { it.abs() < 1e-12 } == true
        if (!atPole) {
            steps += Step("Not a pole", "The denominator isn't \$0\$ at \$${z.name} = ${lx(a)}\$ (or \$f\$ is analytic there), so the residue is \$0\$ unless \$f\$ has an essential singularity.", answer, Kind.Result)
            return Solution("Analytic point", steps, answer)
        }
        val m = orderOf(den, z, a)
        steps += Step("Order of the pole", "${tx(a)} is a root of the denominator ${m} time${if (m > 1) "s" else ""}: a pole of order \$$m\$.", ex(den))
        val g = Algebra.simplify(mul(com.example.cas.cas.pow(sub(z, a), Num(m.toLong())), f))
        steps += Step("Remove the pole", "Multiply by \$(${z.name} - ${lx(a)})${if (m > 1) "^{$m}" else ""}\$.", ex(g))
        if (m == 1) {
            steps += Step("Simple pole", "\$\\operatorname{Res} = \\lim_{${z.name} \\to a} (${z.name} - a) f(${z.name})\$: put \$${z.name} = ${lx(a)}\$ in.", answer, Kind.Result)
        } else {
            var dg = g
            repeat(m - 1) { dg = Algebra.simplify(Calculus.diff(dg, z)) }
            steps += Step("Differentiate ${m - 1} time${if (m > 2) "s" else ""}", "\$\\operatorname{Res} = \\frac{1}{${m - 1}!}\$ times the limit of the ${ordinal(m - 1)} derivative.", ex(dg))
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
        val answer = ans(question, value)
        val derivatives = ArrayList<Step>()
        var g = f
        val coefficients = ArrayList<Expr>()
        var fact = com.example.cas.cas.ONE as Expr
        for (k in 0..order) {
            if (k > 0) { g = Algebra.simplify(Calculus.diff(g, x)); fact = mul(fact, Num(k.toLong())) }
            val at = runCatching { Algebra.simplify(g.subst(x, a)) }.getOrNull()?.takeIf { finiteValue(it) } ?: Calculus.limit(g, x, a)
            derivatives += Step("\$f${if (k > 3) "^{($k)}" else "'".repeat(k)}(${lx(a)})\$", null, line(ex(g), "  ", "→", " ", at))
            coefficients += Algebra.simplify(div(at, fact))
        }
        val steps = ArrayList<Step>()
        steps += Step("Derivatives at \$${x.name} = ${lx(a)}\$", "Differentiate $order times and put \$${x.name} = ${lx(a)}\$ in each.", null, substeps = derivatives)
        steps += Step("Coefficients", "Divide the \$k\$-th derivative by \$k!\$.", line(*coefficients.flatMapIndexed { k, c -> (if (k > 0) listOf<Any>(",", " ") else emptyList()) + listOf<Any>(c) }.toTypedArray()))
        steps += Step("Taylor's formula", "\$f(${x.name}) \\approx \\sum_{k=0}^{$order} \\frac{f^{(k)}(a)}{k!} (${x.name} - a)^k\$.", answer, Kind.Result)
        return Solution("Taylor series", steps, answer)
    }

    // ---- Determinants --------------------------------------------------------------------------------

    private fun determinant(n: Func, angle: AngleUnit): Solution? {
        val mat = Evaluator(angle).evaluate(n.args.firstOrNull() ?: return null) as? com.example.cas.cas.Mat ?: return null
        if (mat.rows != mat.cols || mat.rows < 2 || mat.rows > 20) return null
        if (mat.rows >= 5) return rowReduction(n, mat, angle)
        val question = com.example.cas.editor.MathCodec.copy(MathRow(mutableListOf(n)))
        val value = Evaluator(angle).evaluate(com.example.cas.editor.MathCodec.copy(question))
        val answer = ans(question, value)
        val c = mat.cells
        val steps = ArrayList<Step>()
        if (mat.rows == 2) {
            steps += Step("\$2 \\times 2\$ formula", "\$\\det = ad - bc\$: the diagonal product minus the other one.", line(paren(c[0]), "·", paren(c[3]), "−", paren(c[1]), "·", paren(c[2])))
            steps += Step("Answer", null, answer, Kind.Result)
            return Solution("\$ad - bc\$", steps, answer)
        }
        val size = mat.rows
        val minors = (0 until size).map { j ->
            val cells = ArrayList<Expr>()
            for (r in 1 until size) for (k in 0 until size) if (k != j) cells += c[r * size + k]
            val minor = com.example.cas.cas.Mat(size - 1, size - 1, cells)
            val d = Algebra.simplify(com.example.cas.cas.Matrices.det(minor))
            Step("Minor of ${tx(c[j])}", "Cross out row 1 and column ${j + 1}; its \$${size - 1} \\times ${size - 1}\$ determinant${if (size == 4) ", by the same expansion" else ""}.", line(ex(minor), "→", d)) to d
        }
        val signs = (0 until size).map { if (it % 2 == 0) "+" else "−" }
        steps += Step("Cofactor expansion", "Along the first row, with signs \$${signs.joinToString("\\;") { if (it == "+") "+" else "-" }}\$.",
            line(*minors.flatMapIndexed { j, (_, d) -> (if (j == 0) emptyList() else listOf<Any>(signs[j])) + listOf<Any>(paren(c[j]), "·", paren(d)) }.toTypedArray()),
            substeps = minors.map { it.first })
        steps += Step("Answer", null, answer, Kind.Result)
        return Solution("Cofactor expansion", steps, answer)
    }

    /** Larger determinants by row reduction: eliminate below each pivot, then multiply the diagonal. */
    private fun rowReduction(n: Func, mat: com.example.cas.cas.Mat, angle: AngleUnit): Solution? {
        val size = mat.rows
        val a = Array(size) { r -> Array(size) { c -> mat.cells[r * size + c] } }
        val question = com.example.cas.editor.MathCodec.copy(MathRow(mutableListOf(n)))
        val value = Evaluator(angle).evaluate(com.example.cas.editor.MathCodec.copy(question))
        val answer = ans(question, value)
        val steps = ArrayList<Step>()
        var sign = 1
        fun current() = com.example.cas.cas.Mat(size, size, a.flatMap { it.toList() })
        for (col in 0 until size) {
            // A nonzero pivot, swapping rows if needed (each swap flips the sign).
            val pivotRow = (col until size).firstOrNull { r -> runCatching { Numeric.eval(a[r][col]).abs() > 1e-12 }.getOrDefault(a[r][col] != ZERO) }
            if (pivotRow == null) {
                steps += Step("Zero column", "Column ${col + 1} has no nonzero entry from the diagonal down, so the determinant is 0.", answer, Kind.Result)
                return Solution("Row reduction", steps, answer)
            }
            if (pivotRow != col) {
                val t = a[col]; a[col] = a[pivotRow]; a[pivotRow] = t
                sign = -sign
                steps += Step("Swap rows ${col + 1} and ${pivotRow + 1}", "A swap changes the determinant's sign.", ex(current()))
            }
            val operations = ArrayList<Step>()
            for (r in col + 1 until size) {
                if (a[r][col] == ZERO) continue
                val entry = a[r][col]
                val factor = Algebra.simplify(div(entry, a[col][col]))
                for (c in col until size) a[r][c] = Algebra.simplify(sub(a[r][c], mul(factor, a[col][c])))
                // Each row operation, with the multiplier m = (entry)/(pivot) and the new row.
                operations += Step(
                    "\$R_{${r + 1}} \\leftarrow R_{${r + 1}} - ${lxp(factor)}\\,R_{${col + 1}}\$",
                    "\$m = \\frac{${lx(entry)}}{${lx(a[col][col])}}\$ (the entry over the pivot), so row ${r + 1} gets \$0\$ in column ${col + 1}.",
                    line("R", "${r + 1}", "=", "(", *a[r].flatMapIndexed { k, v -> (if (k > 0) listOf<Any>(",", " ") else emptyList()) + listOf<Any>(v) }.toTypedArray(), ")"),
                )
            }
            if (operations.isNotEmpty()) steps += Step(
                "Clear column ${col + 1}", "Subtract multiples of row ${col + 1} from the rows below; this doesn't change the determinant.",
                ex(current()), substeps = operations,
            )
        }
        val diagonal = (0 until size).map { a[it][it] }
        steps += Step("Multiply the diagonal", "The matrix is now triangular: its determinant is the product of the diagonal" + if (sign < 0) ", times \$-1\$ for the odd number of swaps." else ".",
            line(*(if (sign < 0) listOf<Any>("−") else emptyList<Any>()).toTypedArray(), *diagonal.flatMapIndexed { k, d -> (if (k > 0) listOf<Any>("·") else emptyList()) + listOf<Any>(paren(d)) }.toTypedArray(), "=", value))
        steps += Step("Answer", null, answer, Kind.Result)
        return Solution("Row reduction", steps, answer)
    }

    // ---- Linear algebra ------------------------------------------------------------------------------

    private fun matrixOf(r: MathRow, angle: AngleUnit) = Evaluator(angle).evaluate(r) as? com.example.cas.cas.Mat
    private fun isZeroExpr(e: Expr) = e == ZERO || runCatching { Numeric.eval(e).abs() < 1e-12 }.getOrDefault(false)

    /**
     * Gauss–Jordan on [m], pivoting in its first [pivotCols] columns: for each pivot a step with the
     * matrix after it, and the row operations (swap, scale, clear) inside.
     */
    private fun gaussJordan(m: com.example.cas.cas.Mat, pivotCols: Int): Pair<com.example.cas.cas.Mat, List<Step>> {
        val rows = m.rows; val cols = m.cols
        val a = Array(rows) { r -> Array(cols) { c -> m.cells[r * cols + c] } }
        fun current() = com.example.cas.cas.Mat(rows, cols, a.flatMap { it.toList() })
        val steps = ArrayList<Step>()
        var pr = 0
        for (col in 0 until pivotCols) {
            if (pr >= rows) break
            val found = (pr until rows).firstOrNull { !isZeroExpr(a[it][col]) } ?: continue
            val ops = ArrayList<Step>()
            if (found != pr) { val t = a[pr]; a[pr] = a[found]; a[found] = t; ops += Step("\$R_{${pr + 1}} \\leftrightarrow R_{${found + 1}}\$", "Swap so the pivot isn't \$0\$.") }
            val pivot = a[pr][col]
            if (pivot != com.example.cas.cas.ONE) {
                for (c in 0 until cols) a[pr][c] = Algebra.simplify(div(a[pr][c], pivot))
                ops += Step("\$R_{${pr + 1}} \\leftarrow R_{${pr + 1}} \\div ${lxp(pivot)}\$", "Make the pivot \$1\$.")
            }
            for (r in 0 until rows) {
                if (r == pr || isZeroExpr(a[r][col])) continue
                val factor = a[r][col]
                for (c in 0 until cols) a[r][c] = Algebra.simplify(sub(a[r][c], mul(factor, a[pr][c])))
                ops += Step("\$R_{${r + 1}} \\leftarrow R_{${r + 1}} - ${lxp(factor)}\\,R_{${pr + 1}}\$", "Clear column ${col + 1} in row ${r + 1}.")
            }
            steps += Step("Pivot in column ${col + 1}", "Row ${pr + 1}: a 1 here and 0 above and below it.", ex(current()), substeps = ops)
            pr++
        }
        return current() to steps
    }

    private fun rrefSteps(n: Func, angle: AngleUnit): Solution? {
        val m = matrixOf(n.args.firstOrNull() ?: return null, angle) ?: return null
        val question = com.example.cas.editor.MathCodec.copy(MathRow(mutableListOf(n)))
        val value = Evaluator(angle).evaluate(com.example.cas.editor.MathCodec.copy(question))
        val answer = ans(question, value)
        val (_, steps) = gaussJordan(m, m.cols)
        return Solution("Gauss–Jordan elimination", steps + Step("Reduced row echelon form", "Every pivot is \$1\$ with zeros above and below it.", answer, Kind.Result), answer)
    }

    private fun inverse(row: MathRow, angle: AngleUnit): Solution? {
        val m = matrixOf(MathRow(mutableListOf(inverseOf(row)!!)), angle) ?: return null
        if (m.rows != m.cols || m.rows > 8) return null
        val nn = m.rows
        val value = Evaluator(angle).evaluate(com.example.cas.editor.MathCodec.copy(row))
        val answer = ans(row, value)
        val aug = com.example.cas.cas.Mat(nn, 2 * nn, (0 until nn).flatMap { r -> (0 until 2 * nn).map { c -> if (c < nn) m.cells[r * nn + c] else if (c - nn == r) com.example.cas.cas.ONE else ZERO } })
        val steps = ArrayList<Step>()
        steps += Step("Augment with \$I\$", "Write \$A\$ and the identity side by side: \$[A \\mid I]\$. Row operations that turn \$A\$ into \$I\$ turn \$I\$ into \$A^{-1}\$.", ex(aug))
        val (done, ops) = gaussJordan(aug, nn)
        steps += ops
        val left = (0 until nn).all { r -> (0 until nn).all { c -> isZeroExpr(sub(done.cells[r * 2 * nn + c], if (r == c) com.example.cas.cas.ONE else ZERO)) } }
        if (!left) {
            steps += Step("Not invertible", "The left half can't be made the identity: a row of zeros appears, so \$\\det A = 0\$.", answer, Kind.Result)
            return Solution("Gauss–Jordan elimination", steps, answer)
        }
        steps += Step("Read off \$A^{-1}\$", "The left half is now \$I\$, so the right half is the inverse.", answer, Kind.Result)
        return Solution("Gauss–Jordan elimination", steps, answer)
    }

    private fun eigen(n: Func, angle: AngleUnit): Solution? {
        val m = matrixOf(n.args.firstOrNull() ?: return null, angle) ?: return null
        if (m.rows != m.cols) return null
        val question = com.example.cas.editor.MathCodec.copy(MathRow(mutableListOf(n)))
        val value = Evaluator(angle).evaluate(com.example.cas.editor.MathCodec.copy(question))
        val answer = ans(question, value)
        val lambda = Sym("λ")
        val size = m.rows
        val shifted = com.example.cas.cas.Mat(size, size, List(size * size) { k -> val r = k / size; val c = k % size; if (r == c) sub(lambda, m.cells[k]) else neg(m.cells[k]) })
        val poly = com.example.cas.cas.LinearAlgebra.charpoly(m)
        val steps = ArrayList<Step>()
        steps += Step("\$\\lambda I - A\$", "Subtract \$A\$ from \$\\lambda\$ times the identity.", ex(shifted))
        steps += Step("Characteristic polynomial", "Its determinant, multiplied out.", line("det", "(", "λ", "I", "−", "A", ")", "=", poly))
        if (n.name == "charpoly") {
            steps += Step("Answer", null, answer, Kind.Result)
            return Solution("Characteristic polynomial", steps, answer)
        }
        val factored = runCatching { Algebra.factor(poly) }.getOrNull()
        if (factored != null && !same(factored, poly)) steps += Step("Factor", "The eigenvalues are where it's 0.", eq(poly, factored))
        val roots = com.example.cas.cas.LinearAlgebra.eigenvalues(m).let { if (it is com.example.cas.cas.Seq) it.items else listOf(it) }.mapNotNull { (it as? Eq)?.rhs }
        steps += Step("Eigenvalues", "The roots of the characteristic polynomial.", line(*roots.flatMapIndexed { k, r -> (if (k > 0) listOf<Any>(",", "  ") else emptyList()) + listOf<Any>("λ", "=", r) }.toTypedArray()))
        if (n.name == "eigvecs") for (r in roots) {
            val am = com.example.cas.cas.Mat(size, size, List(size * size) { k -> val rr = k / size; val c = k % size; if (rr == c) sub(m.cells[k], r) else m.cells[k] })
            val (reduced, ops) = gaussJordan(am, size)
            val basis = com.example.cas.cas.LinearAlgebra.nullspace(am)
            steps += Step(
                "Eigenvector for \$\\lambda = ${lx(r)}\$", "Solve \$(A - \\lambda I)v = 0\$: row reduce, then set the free variable to \$1\$.",
                line(ex(reduced), "  ", "⇒", "  ", "v", "=", *basis.flatMapIndexed { k, b -> (if (k > 0) listOf<Any>(",", " ") else emptyList()) + listOf<Any>(b) }.toTypedArray()),
                substeps = listOf(Step("\$A - \\lambda I\$", null, ex(am))) + ops,
            )
        }
        steps += Step("Answer", null, answer, Kind.Result)
        return Solution(if (n.name == "eigvecs") "Eigenvectors" else "Eigenvalues", steps, answer)
    }

    // ---- Differential equations ------------------------------------------------------------------------

    private fun ode(n: Func, angle: AngleUnit): Solution? {
        val (eqn, y, x) = Evaluator(angle).odeParts(n.args[0]) ?: return null
        val question = com.example.cas.editor.MathCodec.copy(MathRow(mutableListOf(n)))
        val value = Evaluator(angle).evaluate(com.example.cas.editor.MathCodec.copy(question))
        val answer = ans(question, value)
        val f = if (eqn is Eq) sub(eqn.lhs, eqn.rhs) else eqn
        val y1 = com.example.cas.cas.Ode.derivativeSymbol(y, 1); val y2 = com.example.cas.cas.Ode.derivativeSymbol(y, 2)
        val c1 = Sym("C₁")
        val steps = ArrayList<Step>()
        val hasConditions = n.args[0].items.any { it is SymNode && it.text == "," }
        if (!f.freeOf(y2)) {
            val ex0 = Algebra.expand(f)
            fun coef(v: Sym, e: Expr): Pair<Expr, Expr> { val cs = Algebra.coefficients(e, v)!!; return cs.getOrElse(1) { ZERO } to cs[0] }
            val (a, r1) = coef(y2, ex0); val (b, r2) = coef(y1, r1); val (c, rest) = coef(y, r2)
            val g = Algebra.simplify(neg(rest))
            val r = Sym("r")
            val charEq = add(mul(a, p(r, two)), mul(b, r), c)
            steps += Step("Linear, constant coefficients", "\$a${y.name}'' + b${y.name}' + c${y.name} = g(${x.name})\$ with \$a = ${lx(a)}\$, \$b = ${lx(b)}\$, \$c = ${lx(c)}\$.", ex(Eq(add(mul(a, y2), mul(b, y1), mul(c, y)), g)))
            steps += Step("Characteristic equation", "Try \$${y.name} = e^{r${x.name}}\$: it works when \$ar^2 + br + c = 0\$.", ex(Eq(charEq, ZERO)))
            val roots = Algebra.solve(Eq(charEq, ZERO), r).let { if (it is com.example.cas.cas.Seq) it.items else listOf(it) }.mapNotNull { (it as? Eq)?.rhs }
            val kind = when {
                roots.size == 1 -> "A repeated root \$r\$: \$${y.name}_h = (C_1 + C_2${x.name})\\,e^{r${x.name}}\$."
                roots.all { Numeric.eval(it).isReal } -> "Two real roots \$r_1\$, \$r_2\$: \$${y.name}_h = C_1 e^{r_1${x.name}} + C_2 e^{r_2${x.name}}\$."
                else -> "Complex roots \$\\alpha \\pm \\beta i\$: \$${y.name}_h = e^{\\alpha ${x.name}} (C_1\\cos\\beta ${x.name} + C_2\\sin\\beta ${x.name})\$."
            }
            steps += Step("Roots", kind, line(*roots.flatMapIndexed { k, rt -> (if (k > 0) listOf<Any>(",", "  ") else emptyList()) + listOf<Any>("r", "=", rt) }.toTypedArray()))
            if (!LinearAlgebra0.isZero(g)) steps += Step("Particular solution", "Undetermined coefficients: guess a \$${y.name}_p\$ of the same form as \$g(${x.name}) = ${lx(g)}\$, put it in and match coefficients; \$${y.name} = ${y.name}_h + ${y.name}_p\$.", null)
        } else {
            val rhs = (Algebra.solve(Eq(f, ZERO), y1) as? Eq)?.rhs ?: return null
            steps += Step("First order", "Write it as \$${y.name}' = f(${x.name}, ${y.name})\$.", ex(Eq(y1, rhs)))
            val cs = Algebra.coefficients(Algebra.expand(rhs), y)
            if (cs != null && cs.size <= 2 && cs.all { it.freeOf(y) }) {
                val a = cs.getOrElse(1) { ZERO }; val b = cs[0]
                if (a == ZERO) {
                    val integral = Calculus.integrate(b, x)
                    steps += Step("Integrate both sides", "\$${y.name}'\$ depends on \$${x.name}\$ only.", line(y, "=", int(b, x), "=", integral ?: b, "+", c1))
                } else {
                    val ia = Calculus.integrate(a, x) ?: return null
                    val mu = Algebra.simplify(p(E, neg(ia)))
                    steps += Step("Linear", "\$${y.name}' - a(${x.name})\\,${y.name} = b(${x.name})\$ with \$a = ${lx(a)}\$, \$b = ${lx(b)}\$.", ex(Eq(sub(y1, mul(a, y)), b)))
                    steps += Step("Integrating factor", "\$\\mu = e^{-\\int a\\,d${x.name}}\$: then \$(\\mu ${y.name})' = \\mu(${y.name}' - a${y.name})\$.", line("μ", "=", mu))
                    val inner = if (b == ZERO) ZERO else Calculus.integrate(Algebra.simplify(mul(mu, b)), x) ?: return null
                    steps += Step("Multiply and integrate", "\$(\\mu ${y.name})' = \\mu b\$, so \$\\mu ${y.name} = \\int \\mu b\\,d${x.name} + C_1\$.", line(mu, y, "=", inner, "+", c1))
                    steps += Step("Solve for \$${y.name}\$", "Divide by \$\\mu\$.", line(y, "=", Algebra.simplify(Algebra.expand(div(add(inner, c1), mu)))))
                }
            } else {
                val factored = Algebra.factor(rhs)
                val fs = if (factored is Mul) factored.factors else listOf(factored)
                val fx = mul(fs.filter { it.freeOf(y) }); val gy = mul(fs.filter { !it.freeOf(y) })
                val left = Calculus.integrate(Algebra.simplify(p(gy, MINUS_ONE)), y); val right = Calculus.integrate(fx, x)
                steps += Step("Separable", "\$${y.name}' = f(${x.name})\\,g(${y.name})\$ with \$f = ${lx(fx)}\$, \$g = ${lx(gy)}\$: put the \$${y.name}\$ terms on one side.",
                    line(int(Algebra.simplify(p(gy, MINUS_ONE)), y), "=", int(fx, x)))
                if (left != null && right != null) steps += Step("Integrate both sides", "Add a constant \$C_1\$.", line(left, "=", right, "+", c1))
                steps += Step("Solve for \$${y.name}\$", "Where the equation can be solved for \$${y.name}\$.", null)
            }
        }
        if (hasConditions) steps += Step("Apply the conditions", "Put the given values in and solve for the constants.", null)
        steps += Step("Answer", null, answer, Kind.Result)
        return Solution(if (!f.freeOf(y2)) "Characteristic equation" else steps[1].title.let { if (it == "Integrate both sides") "Direct integration" else it }, steps, answer)
    }

    private object LinearAlgebra0 { fun isZero(e: Expr) = com.example.cas.cas.LinearAlgebra.isZero(e) }

    // ---- Statistics ------------------------------------------------------------------------------------

    private fun statistics(n: Func, angle: AngleUnit): Solution? {
        val xs = Evaluator(angle).listValues(n.args.firstOrNull() ?: return null) ?: return null
        if (xs.isEmpty() || xs.size > 60) return null
        val question = com.example.cas.editor.MathCodec.copy(MathRow(mutableListOf(n)))
        val value = Evaluator(angle).evaluate(com.example.cas.editor.MathCodec.copy(question))
        val answer = ans(question, value)
        val count = Num(xs.size.toLong())
        val total = Algebra.simplify(add(xs))
        val mean = Algebra.simplify(div(total, count))
        val steps = ArrayList<Step>()
        fun listRow(v: List<Expr>, sep: String = "+") = line(*v.flatMapIndexed { k, t -> (if (k > 0) listOf<Any>(sep) else emptyList()) + listOf<Any>(paren(t)) }.toTypedArray())
        if (n.name == "median") {
            val sorted = xs.sortedBy { Numeric.real(it) }
            steps += Step("Sort", "Put the ${xs.size} values in order.", listRow(sorted, ","))
            steps += Step(
                if (xs.size % 2 == 1) "Middle value" else "Middle two values",
                if (xs.size % 2 == 1) "An odd count: the value in position ${(xs.size + 1) / 2}." else "An even count: the mean of positions ${xs.size / 2} and ${xs.size / 2 + 1}.",
                answer, Kind.Result,
            )
            return Solution("Median", steps, answer)
        }
        steps += Step("Add them up", "${xs.size} values.", line(listRow(xs), "=", total))
        steps += Step("Mean", "Divide by \$n = ${xs.size}\$.", line(MathRow(mutableListOf(com.example.cas.editor.Frac(ex(total), ex(count)))), "=", mean))
        if (n.name == "mean") { steps += Step("Answer", null, answer, Kind.Result); return Solution("Mean", steps, answer) }
        val squares = xs.map { Algebra.simplify(p(sub(it, mean), two)) }
        val ss = Algebra.simplify(add(squares))
        steps += Step("Squared deviations", "\$(x - \\bar{x})^2\$ for each value.", line(listRow(squares), "=", ss))
        val population = n.name == "psd"
        val divisor = if (population) count else Num((xs.size - 1).toLong())
        val variance = Algebra.simplify(div(ss, divisor))
        steps += Step(if (population) "Population variance" else "Sample variance", if (population) "Divide by \$n = ${xs.size}\$." else "Divide by \$n - 1 = ${xs.size - 1}\$ (one degree of freedom goes to the mean).",
            line(MathRow(mutableListOf(com.example.cas.editor.Frac(ex(ss), ex(divisor)))), "=", variance))
        if (n.name != "var") steps += Step("Standard deviation", "The square root of the variance.", line(MathRow(mutableListOf(com.example.cas.editor.Sqrt(ex(variance)))), "=", value))
        steps += Step("Answer", null, answer, Kind.Result)
        return Solution(if (n.name == "var") "Variance" else "Standard deviation", steps, answer)
    }

    // ---- Vector calculus ----------------------------------------------------------------------------------

    private fun partial(f: Expr, v: Sym): MathRow = MathRow(mutableListOf(com.example.cas.editor.Derivative(MathRow(mutableListOf(sym(v.name))), ex(f), partial = true)))

    private fun vectorCalculus(n: Func, angle: AngleUnit): Solution? {
        val question = com.example.cas.editor.MathCodec.copy(MathRow(mutableListOf(n)))
        val value = Evaluator(angle).evaluate(com.example.cas.editor.MathCodec.copy(question))
        val answer = ans(question, value)
        val arg = Evaluator(angle).evaluate(n.args.firstOrNull() ?: return null)
        val steps = ArrayList<Step>()
        val xyz = listOf(Sym("x"), Sym("y"), Sym("z"))
        fun partials(f: Expr, vars: List<Sym>) = vars.map { v -> Step("\$\\frac{\\partial}{\\partial ${v.name}}\$", "Other letters held constant.", eq(partial(f, v), Algebra.simplify(Calculus.diff(f, v)))) }
        when (n.name) {
            "grad" -> {
                if (arg is com.example.cas.cas.Mat) return null
                if (n.args.getOrNull(1)?.isEmpty == false) return null
                val vars = xyz.filter { !arg.freeOf(it) }.ifEmpty { return null }.let { v -> if (v.contains(Sym("z"))) xyz else xyz.take(maxOf(2, v.size)) }
                steps += Step("Partial derivatives", "\$\\nabla f\$ lists the derivative of \$f\$ in each direction.", null, substeps = partials(arg, vars))
            }
            "div" -> {
                val m = arg as? com.example.cas.cas.Mat ?: return null
                val vars = xyz.take(m.cells.size)
                val terms = m.cells.mapIndexed { k, c -> Algebra.simplify(Calculus.diff(c, vars[k])) }
                steps += Step("Divergence", "\$\\nabla \\cdot F = \\frac{\\partial F_1}{\\partial x} + \\frac{\\partial F_2}{\\partial y} + \\frac{\\partial F_3}{\\partial z}\$ (two terms in the plane): each component by its own variable.",
                    line(*terms.flatMapIndexed { k, t -> (if (k > 0) listOf<Any>("+") else emptyList()) + listOf<Any>(paren(t)) }.toTypedArray()),
                    substeps = m.cells.mapIndexed { k, c -> Step("\$\\frac{\\partial F_{${k + 1}}}{\\partial ${vars[k].name}}\$", null, eq(partial(c, vars[k]), terms[k])) })
            }
            "curl" -> {
                val m = arg as? com.example.cas.cas.Mat ?: return null
                if (m.cells.size != 3) return null
                val (P, Q, R) = m.cells
                val (xx, yy, zz) = xyz
                val comps = listOf(
                    Triple("x", R to yy, Q to zz), Triple("y", P to zz, R to xx), Triple("z", Q to xx, P to yy),
                )
                steps += Step("Curl", "\$\\nabla \\times F = \\left(\\frac{\\partial R}{\\partial y} - \\frac{\\partial Q}{\\partial z}, \\frac{\\partial P}{\\partial z} - \\frac{\\partial R}{\\partial x}, \\frac{\\partial Q}{\\partial x} - \\frac{\\partial P}{\\partial y}\\right)\$.", null, substeps = comps.map { (name, a, b) ->
                    val da = Algebra.simplify(Calculus.diff(a.first, a.second)); val db = Algebra.simplify(Calculus.diff(b.first, b.second))
                    Step("\$$name\$-component", null, line(partial(a.first, a.second), "−", partial(b.first, b.second), "=", paren(da), "−", paren(db), "=", Algebra.simplify(sub(da, db))))
                })
            }
            "jacobian" -> {
                val m = arg as? com.example.cas.cas.Mat ?: return null
                val vars = xyz.filter { v -> m.cells.any { !it.freeOf(v) } }
                steps += Step("Jacobian", "Row \$i\$, column \$j\$: \$\\frac{\\partial F_i}{\\partial x_j}\$.", null, substeps = m.cells.flatMapIndexed { i, c -> vars.map { v -> Step("\$\\frac{\\partial F_{${i + 1}}}{\\partial ${v.name}}\$", null, eq(partial(c, v), Algebra.simplify(Calculus.diff(c, v)))) } })
            }
            "hessian" -> {
                val vars = xyz.filter { !arg.freeOf(it) }
                val first = vars.map { Algebra.simplify(Calculus.diff(arg, it)) }
                steps += Step("First derivatives", null, null, substeps = partials(arg, vars))
                steps += Step("Second derivatives", "Row \$i\$, column \$j\$: \$\\frac{\\partial^2 f}{\\partial x_i\\,\\partial x_j}\$ (the matrix is symmetric).", null, substeps = vars.flatMapIndexed { i, vi ->
                    vars.drop(i).map { vj -> Step("\$\\frac{\\partial^2 f}{\\partial ${vi.name}\\,\\partial ${vj.name}}\$", null, eq(partial(first[i], vj), Algebra.simplify(Calculus.diff(first[i], vj)))) }
                })
            }
        }
        steps += Step("Answer", null, answer, Kind.Result)
        return Solution(when (n.name) { "grad" -> "Gradient"; "div" -> "Divergence"; "curl" -> "Curl"; "jacobian" -> "Jacobian matrix"; else -> "Hessian matrix" }, steps, answer)
    }

    // ---- Complex arithmetic -----------------------------------------------------------------------------

    private fun complex(row: MathRow, angle: AngleUnit): Solution? {
        val value = Evaluator(angle).evaluate(com.example.cas.editor.MathCodec.copy(row))
        val answer = ans(row, value)
        val steps = ArrayList<Step>()
        val single = row.items.singleOrNull()
        if (single is com.example.cas.editor.Frac && single.den.items.any { it is SymNode && it.text == "i" }) {
            val top = Evaluator(angle).evaluate(single.num)
            val bottom = Evaluator(angle).evaluate(single.den)
            val (a, b) = com.example.cas.cas.ComplexArith.split(bottom) ?: return null
            val conj = add(a, mul(neg(b), com.example.cas.cas.I))
            val norm = Algebra.simplify(add(com.example.cas.cas.pow(a, com.example.cas.cas.TWO), com.example.cas.cas.pow(b, com.example.cas.cas.TWO)))
            val numerator = com.example.cas.cas.ComplexArith.normalize(mul(top, conj))
            steps += Step("Multiply by the conjugate", "Multiply the top and the bottom by ${tx(conj)}: the bottom becomes real.", MathRow(mutableListOf(com.example.cas.editor.Frac(line("(", top, ")", "(", conj, ")"), line("(", bottom, ")", "(", conj, ")")))))
            steps += Step("The bottom", "\$(a + bi)(a - bi) = a^2 + b^2\$.", ex(norm))
            steps += Step("The top", "Multiply out, with \$i^2 = -1\$.", ex(numerator))
            steps += Step("Answer", "Divide each part by ${tx(norm)}.", answer, Kind.Result)
            return Solution("Complex division", steps, answer)
        }
        val euler = row.items.any { it is SymNode && it.text == "e" } && row.items.any { n -> n is com.example.cas.editor.Pow && n.exp.items.any { it is SymNode && it.text == "i" } }
        if (euler) {
            steps += Step("Euler's formula", "\$e^{i\\theta} = \\cos\\theta + i\\sin\\theta\$, and \$e^{x + iy} = e^x(\\cos y + i\\sin y)\$.", null)
            steps += Step("Exact values", "Use the exact cosine and sine of the angle, then collect real and imaginary parts.", answer, Kind.Result)
            return Solution("Euler's formula", steps, answer)
        }
        // The parts as typed (before a + bi), for writing the products and powers out.
        val raw = runCatching { Evaluator(angle).also { it.autoSimplify = false }.evaluate(com.example.cas.editor.MathCodec.copy(row)) }.getOrNull()
        val ii = com.example.cas.cas.I
        fun z(a: Expr, b: Expr) = add(a, mul(b, ii))
        if (raw is Mul && raw.factors.size == 2) {
            val p1 = com.example.cas.cas.ComplexArith.split(raw.factors[0]); val p2 = com.example.cas.cas.ComplexArith.split(raw.factors[1])
            if (p1 != null && p2 != null) {
                val (a, b) = p1; val (c, d) = p2
                steps += Step("Multiply out", "Each part of the first times each part of the second (FOIL).",
                    line(paren(mul(a, c)), "+", paren(mul(a, d)), "i", "+", paren(mul(b, c)), "i", "+", paren(mul(b, d)), "i", com.example.cas.editor.Pow(MathRow(mutableListOf(sym("2"))))))
                steps += Step("Use \$i^2 = -1\$", "So the last term changes sign and joins the real part.", ex(z(Algebra.simplify(sub(mul(a, c), mul(b, d))), Algebra.simplify(add(mul(a, d), mul(b, c))))))
                steps += Step("Answer", null, answer, Kind.Result)
                return Solution("Complex multiplication", steps, answer)
            }
        }
        if (raw is Pow && raw.exp is Num && (raw.exp as Num).q.isInteger && (raw.exp as Num).q.num.toInt() in 2..16) {
            val base = com.example.cas.cas.ComplexArith.split(raw.base)
            if (base != null) {
                val n = (raw.exp as Num).q.num.toInt()
                val (a, b) = base
                steps += Step("Square", "\$(a + bi)^2 = a^2 - b^2 + 2abi\$, as \$i^2 = -1\$.", line("(", z(a, b), ")", com.example.cas.editor.Pow(MathRow(mutableListOf(sym("2")))), "=", com.example.cas.cas.ComplexArith.normalize(com.example.cas.cas.pow(z(a, b), two))))
                // Higher powers by squaring again (and one more factor for odd steps).
                var k = 2
                var current = com.example.cas.cas.ComplexArith.normalize(com.example.cas.cas.pow(z(a, b), two))
                while (k * 2 <= n) {
                    current = com.example.cas.cas.ComplexArith.normalize(com.example.cas.cas.pow(current, two)); k *= 2
                    steps += Step("Square again", "The ${k}th power is the square of the ${k / 2}th.", line("z", com.example.cas.editor.Pow(MathRow(mutableListOf(sym("$k")))), "=", current))
                }
                // The powers left over, one factor of z at a time.
                val zz = z(a, b)
                while (k < n) {
                    val (c, d) = com.example.cas.cas.ComplexArith.split(current) ?: break
                    val next = com.example.cas.cas.ComplexArith.normalize(mul(current, zz))
                    k++
                    steps += Step("Times \$z\$", "\$z^{$k} = z^{${k - 1}} \\cdot z\$: multiply out (FOIL), then \$i^2 = -1\$.",
                        line("(", z(c, d), ")", "(", zz, ")", "=", paren(mul(c, a)), "+", paren(mul(c, b)), "i", "+", paren(mul(d, a)), "i", "+", paren(mul(d, b)), "i",
                            com.example.cas.editor.Pow(MathRow(mutableListOf(sym("2")))), "=", next))
                    current = next
                }
                steps += Step("Answer", null, answer, Kind.Result)
                return Solution("Complex powers", steps, answer)
            }
        }
        steps += Step("Multiply out", "Expand the products and powers like ordinary algebra.", null)
        steps += Step("Use \$i^2 = -1\$", "Every \$i^2\$ becomes \$-1\$ (and \$i^3 = -i\$, \$i^4 = 1\$).", null)
        steps += Step("Collect", "Real parts together, imaginary parts together: \$a + bi\$.", answer, Kind.Result)
        return Solution("Complex arithmetic", steps, answer)
    }

    /** The working as plain text, for copying. */
    /** The working as Markdown, with the math in LaTeX ($ … $), for copying. */
    fun text(question: MathRow, s: Solution): String = buildString {
        appendLine("\$" + Latex.of(question) + "\$")
        appendLine("Method: ${markdown(s.method)}")
        var n = 0
        for (st in s.steps) {
            n++
            append("$n. ${markdown(st.title)}")
            st.text?.let { append(" — ${markdown(it)}") }
            appendLine()
            st.math?.let { appendLine("   \$" + Latex.of(it) + "\$") }
            for ((k, sub) in flattenSubsteps(st.substeps).withIndex()) {
                append("   ${'a' + k}) ${markdown(sub.title)}")
                sub.text?.let { append(" — ${markdown(it)}") }
                appendLine()
                sub.math?.let { appendLine("      \$" + Latex.of(it) + "\$") }
            }
        }
    }

    /** Greek letters typed as Unicode inside $ … $ become LaTeX commands, for the copied Markdown. */
    private fun markdown(text: String): String = LatexParser.inline(text).joinToString("") { (isMath, piece) ->
        if (!isMath) piece else "\$" + piece.map { c -> GREEK[c]?.let { "\\$it " } ?: c.toString() }.joinToString("").replace(" }", "}").trim() + "\$"
    }
    private val GREEK = mapOf('α' to "alpha", 'β' to "beta", 'γ' to "gamma", 'δ' to "delta", 'ε' to "varepsilon", 'θ' to "theta", 'λ' to "lambda", 'μ' to "mu",
        'ξ' to "xi", 'π' to "pi", 'ρ' to "rho", 'σ' to "sigma", 'τ' to "tau", 'φ' to "varphi", 'χ' to "chi", 'ψ' to "psi", 'ω' to "omega", 'Γ' to "Gamma", 'Δ' to "Delta")

    private fun flattenSubsteps(steps: List<Step>): List<Step> = steps.flatMap { listOf(it) + flattenSubsteps(it.substeps) }
}
