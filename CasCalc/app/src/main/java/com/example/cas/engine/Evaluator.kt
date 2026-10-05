package com.example.cas.engine

import com.example.cas.cas.Algebra
import com.example.cas.cas.Fn
import com.example.cas.cas.Calculus
import com.example.cas.cas.E
import com.example.cas.cas.Eq
import com.example.cas.cas.Expr
import com.example.cas.cas.HALF
import com.example.cas.cas.I
import com.example.cas.cas.LinearAlgebra
import com.example.cas.cas.Mat
import com.example.cas.cas.MathError
import com.example.cas.cas.Num
import com.example.cas.cas.Numeric
import com.example.cas.cas.ONE
import com.example.cas.cas.PI
import com.example.cas.cas.Sym
import com.example.cas.cas.add
import com.example.cas.cas.div
import com.example.cas.cas.fn
import com.example.cas.cas.freeVars
import com.example.cas.cas.freeOf
import com.example.cas.cas.isConstant
import com.example.cas.cas.mul
import com.example.cas.cas.neg
import com.example.cas.cas.num
import com.example.cas.cas.pow
import com.example.cas.cas.sub
import com.example.cas.cas.subst
import com.example.cas.editor.BigOp
import com.example.cas.editor.BigOpKind
import com.example.cas.editor.Binom
import com.example.cas.editor.Const
import com.example.cas.editor.Derivative
import com.example.cas.editor.Frac
import com.example.cas.editor.Func
import com.example.cas.editor.Integral
import com.example.cas.editor.MathRow
import com.example.cas.editor.Matrix
import com.example.cas.editor.Node
import com.example.cas.editor.Pow
import com.example.cas.editor.Root
import com.example.cas.editor.Sqrt
import com.example.cas.math.Rational

enum class AngleUnit { Radians, Degrees }

/**
 * Turns the editor's 2D tree into a symbolic expression and runs any CAS
 * commands in it (solve, expand, factor, simplify, taylor, ∫, d/dx, Σ).
 *
 * Letters without a stored value stay symbolic, so 2x + 3x gives 5x.
 * Precedence, lowest first: :=  =  + −  × ÷ mod (and implicit ×)  unary −  powers ! %.
 */
class Evaluator(
    private val angle: AngleUnit = AngleUnit.Radians,
    private val ans: Expr? = null,
    private val variables: Map<String, Expr> = emptyMap(),
    /** The unit system for physical constants (SI, Planck, atomic, natural). */
    private val units: UnitSystem = UnitSystem.SI,
    /** The coordinate system for ∇, ∇·, ∇×, ∇², J and H. */
    private val coordinates: com.example.cas.cas.Coordinates = com.example.cas.cas.Coordinates(),
    /** Functions defined like f(x) = x², usable as f(2), f(x + 1) and f′(x). */
    private val functions: Map<String, UserFunction> = emptyMap(),
) {
    /** Set when the input was an assignment like a := 5. */
    var assigned: Pair<String, Expr>? = null
        private set

    /** Put answers with letters in their simplest form (off for the steps, which show how). */
    var autoSimplify = true

    /** Set when the input defined a function, like f(x) := x². */
    var definedFunction: Pair<String, UserFunction>? = null
        private set

    fun evaluate(row: MathRow): Expr {
        val items = clean(row)
        // f(x) := … (or = …) defines a function; the result shows the definition.
        UserFunction.definition(items)?.let { (name, vs, body) ->
            // Already defined elsewhere (functions passed in): then this line is an equation using it.
            if (name !in functions) {
                if (body.isEmpty()) throw MathError("Write what $name(${vs.joinToString(", ")}) equals")
                val value = RowParser(body, vs.associateWith { Sym(it) }).parse()
                definedFunction = name to UserFunction(vs, value)
                return Eq(Fn(name, vs.map { Sym(it) }), value)
            }
        }
        val assign = items.indexOfFirst { (it as? com.example.cas.editor.Sym)?.text == ":=" }
        if (assign >= 0) {
            val name = items.take(assign).singleOrNull()?.let { (it as? com.example.cas.editor.Sym)?.text }
            if (name == null || name.length != 1 || !name[0].isLetter() || name in setOf("e", "i")) {
                throw MathError("Put a single letter before :=")
            }
            val value = RowParser(items.drop(assign + 1), emptyMap()).parse()
            assigned = name to value
            return Eq(Sym(name), value)
        }
        // Constants with i in them come out as a + bi: (1 + 2i)(3 − i) = 5 + 5i; anything with letters
        // in its simplest form: (x² − 1)/(x − 1) = x + 1.
        val parsed = RowParser(items, emptyMap()).parse()
        // (With automatic simplification off, for the steps, complex numbers stay as typed too.)
        val value = if (autoSimplify) com.example.cas.cas.ComplexArith.normalize(parsed) else parsed
        return if (autoSimplify && !asksForAForm(items)) com.example.cas.cas.AutoSimplify.simplify(value) else value
    }

    /** factor, expand, apart…: the form was asked for, so it's kept as it is. */
    private fun asksForAForm(items: List<Node>): Boolean = items.any { n ->
        (n is com.example.cas.editor.Func && n.name in FORMS) || n.slots.any { asksForAForm(it.items) }
    }

    private val FORMS = setOf("factor", "expand", "apart", "together", "simplify", "cancel", "collect", "taylor", "series")

    /** For the steps: a differential equation typed in dsolve(…), as (equation, y, x). */
    fun odeParts(row: MathRow): Triple<Expr, Sym, Sym>? = runCatching { RowParser(emptyList(), emptyMap()).odeParts(row.items) }.getOrNull()

    /** For the steps: a list typed with commas (or a vector), as its values. */
    fun listValues(row: MathRow): List<Expr>? = runCatching {
        val parts = splitTopCommas(clean(row)).filter { it.isNotEmpty() }.map { RowParser(it, emptyMap()).parse() }
        if (parts.size == 1 && parts[0] is com.example.cas.cas.Mat) (parts[0] as com.example.cas.cas.Mat).cells else parts
    }.getOrNull()

    private fun splitTopCommas(items: List<Node>): List<List<Node>> {
        val out = ArrayList<List<Node>>(); var depth = 0; var start = 0
        items.forEachIndexed { k, n ->
            when ((n as? com.example.cas.editor.Sym)?.text) { "(" -> depth++; ")" -> depth--; "," -> if (depth == 0) { out += items.subList(start, k); start = k + 1 } }
        }
        out += items.subList(start, items.size)
        return out
    }

    private fun clean(row: MathRow) = row.items.filter { (it as? com.example.cas.editor.Sym)?.text != Formatter.THIN_SPACE }

    private fun eval(row: MathRow, env: Map<String, Expr>): Expr {
        if (row.isEmpty) throw MathError("Fill in the empty box")
        return RowParser(clean(row), env).parse()
    }

    private inner class RowParser(private val items: List<Node>, private val env: Map<String, Expr>) {
        private var i = 0

        fun parse(): Expr {
            if (items.isEmpty()) throw MathError("Fill in the empty box")
            val lhs = sum()
            val v = when {
                accept("=") -> Eq(lhs, sum())
                peek() in RELATIONS -> {
                    // x < 3, or a chain like −2 < x ≤ 2.
                    val parts = mutableListOf(lhs)
                    val ops = mutableListOf<String>()
                    while (peek() in RELATIONS) { ops += peek()!!; i++; parts += sum() }
                    com.example.cas.cas.Rel(parts, ops)
                }
                else -> lhs
            }
            if (i < items.size) {
                val t = (items[i] as? com.example.cas.editor.Sym)?.text
                throw MathError(if (t == ")") "Unmatched )" else "Unexpected ${t ?: "item"}")
            }
            return v
        }

        private fun peek(): String? = (items.getOrNull(i) as? com.example.cas.editor.Sym)?.text
        private fun accept(t: String) = (peek() == t).also { if (it) i++ }

        private fun sum(): Expr {
            var v = product()
            while (true) {
                v = when {
                    accept("+") -> add(v, product())
                    accept("−") -> sub(v, product())
                    else -> return v
                }
            }
        }

        private fun product(): Expr {
            var v = signed()
            while (true) {
                v = when {
                    accept("×") -> mul(v, signed())
                    accept("÷") -> div(v, signed())
                    // mod takes the whole product after it: x mod 2π is x mod (2π), not (x mod 2)·π.
                    accept("mod") -> fn("mod", v, product())
                    startsOperand() -> mul(v, postfix())
                    else -> return v
                }
            }
        }

        private fun signed(): Expr = when {
            accept("−") -> neg(signed())
            accept("+") -> signed()
            else -> postfix()
        }

        private fun startsOperand(): Boolean {
            val n = items.getOrNull(i) ?: return false
            val t = (n as? com.example.cas.editor.Sym)?.text ?: return n !is Pow
            return t !in setOf("+", "−", "×", "÷", "mod", ")", "!", "%", ",", "=", ":=", "→", "′") && t !in RELATIONS
        }

        private fun postfix(): Expr {
            var v = primary()
            while (true) {
                val n = items.getOrNull(i)
                v = when {
                    n is Pow -> {
                        i++
                        // Aᵀ is the transpose and Aᴴ the Hermitian conjugate (conjugate transpose).
                        when (n.exp.plainText()) {
                            "T" -> fn("transpose", v)
                            "H" -> fn("hermitian", v)
                            else -> pow(v, eval(n.exp, env))
                        }
                    }
                    peek() == "′" && v is Sym -> {
                        var k = 0
                        while (accept("′")) k++
                        Sym(v.name + "′".repeat(k))
                    }
                    accept("!") -> fn("fact", v)
                    accept("%") -> div(v, num(100))
                    else -> return v
                }
            }
        }

        /** f(…) or f′(…) for a defined function f: the body with its variable replaced (differentiated for each ′). */
        private fun userCall(name: String): Expr? {
            val f = functions[name] ?: return null
            var j = i
            var primes = 0
            while ((items.getOrNull(j) as? com.example.cas.editor.Sym)?.text == "′") { primes++; j++ }
            if ((items.getOrNull(j) as? com.example.cas.editor.Sym)?.text != "(") return null
            // The matching bracket.
            var depth = 0
            var end = j
            while (end < items.size) {
                when ((items[end] as? com.example.cas.editor.Sym)?.text) { "(" -> depth++; ")" -> { depth--; if (depth == 0) break } }
                end++
            }
            if (end >= items.size) throw MathError("A bracket isn't closed")
            // Arguments split at top-level commas: f(1, 2, 3).
            val inner = items.subList(j + 1, end)
            val args = splitCommas(inner).map { RowParser(it, env).parse() }
            i = end + 1
            if (args.size != f.variables.size) throw MathError("$name takes ${f.variables.size} value${if (f.variables.size == 1) "" else "s"}")
            if (primes > 0 && f.variables.size != 1) throw MathError("\$$name'\$ needs a function of one variable; use \$\\partial\$ for the others")
            var body = f.body
            repeat(primes) { body = com.example.cas.cas.Calculus.diff(body, Sym(f.variable)) }
            // Substitute all at once (through placeholders), so f(y, x) swaps correctly.
            val temps = f.variables.indices.map { Sym("\u0001$it") }
            f.variables.forEachIndexed { k, v -> body = body.subst(Sym(v), temps[k]) }
            args.forEachIndexed { k, a -> body = body.subst(temps[k], a) }
            return Algebra.simplify(body)
        }

        private fun primary(): Expr {
            val n = items.getOrNull(i) ?: throw MathError("Something is missing at the end")
            i++
            if (n is com.example.cas.editor.Sym) userCall(n.text)?.let { return it }
            return when (n) {
                is com.example.cas.editor.Sym -> symbol(n.text)
                is Const -> Constant.byId(n.id)?.value(units) ?: com.example.cas.engine.MathConstant.byId(n.id)?.value ?: throw MathError("Unknown constant")
                is Frac -> div(eval(n.num, env), eval(n.den, env))
                is Sqrt -> pow(eval(n.arg, env), HALF)
                // An empty index means a square root.
                is Root -> pow(eval(n.arg, env), div(ONE, if (n.index.isEmpty) num(2) else eval(n.index, env)))
                is Func -> function(n)
                is BigOp -> {
                    val k = variable(n.variable)
                    Calculus.sum(eval(n.body, env + (k.name to k)), k, eval(n.lower, env), eval(n.upper, env), n.kind == BigOpKind.Product)
                }
                is Integral -> integral(n)
                is Derivative -> derivative(n)
                is Binom -> fn("binom", eval(n.n, env), eval(n.k, env))
                is com.example.cas.editor.Scripted -> throw MathError("Subscripts are only for showing formulas")
                // Cells left empty count as 0.
                // Only the rows and columns in use (a growable matrix's empty trailing ones don't count);
                // empty cells inside count as 0.
                is Matrix -> Mat(n.usedRows, n.usedCols, (0 until n.usedRows).flatMap { r -> (0 until n.usedCols).map { c -> n.cell(r, c).let { if (it.isEmpty) com.example.cas.cas.ZERO else eval(it, env) } } })
                is Pow -> throw MathError("A power needs something before it")
            }
        }

        private fun symbol(t: String): Expr = when {
            t[0].isDigit() || t == "." -> {
                i--
                val sb = StringBuilder()
                while (true) {
                    val s = peek() ?: break
                    if (s[0].isDigit() || s == ".") { sb.append(s); i++ } else break
                }
                val text = sb.toString()
                if (text.count { it == '.' } > 1 || text == ".") throw MathError("That number has too many points")
                Num(Rational.parseDecimal(text))
            }
            t == "(" -> {
                val v = sum()
                if (!accept(")") && i < items.size) throw MathError("Missing )")
                v
            }
            t == "π" -> PI
            t == "e" -> E
            t == "i" -> I
            t == "∞" -> com.example.cas.cas.INF
            Regex("C[0-9]+").matches(t) -> Sym(t)
            t.length > 1 && t[0].isLetter() && t.drop(1).all { it == '′' } -> Sym(t)
            t == "ans" -> ans ?: throw MathError("There's no previous answer yet")
            t in FUNCTION_WORDS -> fn(t, postfix())
            (t.length == 1 && t[0].isLetter()) || com.example.cas.editor.MathAlphabets.isMathLetter(t) || com.example.cas.cas.CustomSymbol.isCustom(t) -> env[t] ?: variables[t] ?: Sym(t)
            else -> throw MathError("Unexpected $t")
        }

        private fun variable(row: MathRow): Sym {
            val t = row.plainText()
            if (!((t.length == 1 && t[0].isLetter()) || com.example.cas.editor.MathAlphabets.isMathLetter(t) || com.example.cas.cas.CustomSymbol.isCustom(t)) || t == "e" || t == "i") throw MathError("Use a single letter as the variable")
            return Sym(t)
        }

        /** Symbolic d/dx; at a point, falls back to a numerical derivative if the symbolic one fails. */
        private fun derivative(n: Derivative): Expr {
            val x = variable(n.variable)
            val body = eval(n.body, env + (x.name to x))
            val order = if (n.order.isEmpty) 1 else
                ((eval(n.order, env) as? Num)?.q?.takeIf { it.isInteger && it.signum > 0 }?.num?.toInt()
                    ?: throw MathError("The order of a derivative must be a whole number"))
            if (order > 20) throw MathError("Use an order up to 20")
            val symbolic = runCatching {
                var d = body
                repeat(order) { d = Algebra.simplify(Calculus.diff(d, x)) }
                d
            }
            if (n.at.isEmpty) return symbolic.getOrThrow()
            val at = eval(n.at, env)
            symbolic.getOrNull()?.let { d -> runCatching { return Algebra.simplify(d.subst(x, at)) } }
            if (!body.freeVars().all { it == x.name }) symbolic.getOrThrow()
            val point = Numeric.real(at)
            if (order != 1) symbolic.getOrThrow()
            return com.example.cas.cas.Flt(com.example.cas.cas.Numerics.derivative({ t -> Numeric.real(body, mapOf(x.name to t)) }, point))
        }

        private fun integral(n: Integral): Expr {
            val x = variable(n.variable)
            val body = eval(n.body, env + (x.name to x))
            if (n.lower.isEmpty && n.upper.isEmpty) {
                return Calculus.antiderivative(body, x) ?: throw MathError("No antiderivative found. Add limits for a numerical answer")
            }
            return Calculus.definite(body, x, eval(n.lower, env), eval(n.upper, env))
        }

        private fun function(f: Func): Expr {
            when (f.name) {
                "solve" -> {
                    // Several equations and unknowns are separated by commas: solve(x + y = 3, x − y = 1; x, y).
                    val vars = splitCommas(f.args[1].items).filter { it.isNotEmpty() }.map { variable(MathRow(it.toMutableList())) }
                    val bound = env + vars.associate { it.name to it }
                    val eqs = splitCommas(f.args[0].items).map { RowParser(it, bound).parse() }
                    if (eqs.size > 1 || vars.size > 1) {
                        val unknowns = vars.ifEmpty { pickVariables(eqs) }
                        return com.example.cas.cas.Systems.solve(eqs, unknowns)
                    }
                    val eq = eqs.single()
                    val v = vars.singleOrNull() ?: pickVariable(eq)
                    if (eq is com.example.cas.cas.Rel) return com.example.cas.cas.Systems.solveInequality(eq, v)
                    return Algebra.solve(eq, v)
                }
                "dsolve" -> return dsolve(f.args[0].items)
                // Statistics on a list typed with commas, mean(2, 4, 4, 5), or on a vector.
                "mean", "median", "sd", "psd", "var", "total" -> {
                    val xs = listArgument(f.args[0])
                    return when (f.name) {
                        "total" -> com.example.cas.cas.Statistics.total(xs)
                        "mean" -> com.example.cas.cas.Statistics.mean(xs)
                        "median" -> com.example.cas.cas.Statistics.median(xs)
                        "sd" -> com.example.cas.cas.Statistics.sampleSd(xs)
                        "psd" -> com.example.cas.cas.Statistics.populationSd(xs)
                        else -> com.example.cas.cas.Statistics.variance(xs)
                    }
                }
                // Finance: npv(r, c₀, c₁, …) and irr(c₀, c₁, …) take lists of cash flows.
                in com.example.cas.cas.MoreMath.FINANCE -> {
                    val items = splitCommas(f.args.flatMap { r -> r.items + listOf(com.example.cas.editor.Sym(",")) }.dropLast(1)).filter { it.isNotEmpty() }.map { RowParser(it, env).parse() }
                    val flat = if (items.size == 1 && items[0] is Mat) (items[0] as Mat).cells else items
                    return com.example.cas.cas.MoreMath.finance(f.name, if (f.name in setOf("npv", "irr")) flat else f.args.map { eval(it, env) }) { from -> flat.drop(from) }
                }
                // Statistics II: more of a list's statistics.
                in com.example.cas.cas.Statistics.MORE -> return com.example.cas.cas.Statistics.more(f.name, listArgument(f.args[0]))
                    ?: throw MathError("Unknown statistic")
                "normpdf", "normcdf" -> {
                    val (x, mu, sigma) = f.args.map { eval(it, env) }
                    return if (f.name == "normpdf") com.example.cas.cas.Statistics.normalPdf(x, mu, sigma)
                    else com.example.cas.cas.Statistics.normalCdf(x, mu, sigma)
                }
                "poissonpdf" -> {
                    val (lambda, k) = f.args.map { eval(it, env) }
                    return com.example.cas.cas.Statistics.poissonPmf(lambda, k)
                }
                "binomcdf" -> {
                    val (n, p, k) = f.args.map { eval(it, env) }
                    return com.example.cas.cas.Statistics.binomialCdf(n, p, k)
                }
                "binompdf" -> {
                    val (n, p, k) = f.args.map { eval(it, env) }
                    return com.example.cas.cas.Statistics.binomialPmf(n, p, k)
                }
                // ∮ over the circle written underneath: |z − a| = r.
                "contour" -> {
                    val spec = RowParser(f.args[1].items, env).let { parser -> parser.parse() }
                    val (z, center, radius) = circleSpec(spec)
                    val body = eval(f.args[0], env + (z.name to z))
                    // A circle with letters in it (|z| = R) stays as ∮ until they're known, e.g. in a limit.
                    if (!center.freeVars().isEmpty() || !radius.freeVars().isEmpty()) return com.example.cas.cas.Fn("contour", listOf(body, z, center, radius))
                    return com.example.cas.graph.ComplexIntegrals.circle(body, z, center, radius)
                }
                // Res f at z = a.
                "residue" -> {
                    val (z, at) = pointSpec(f.args[1], "Res")
                    val body = eval(f.args[0], env + (z.name to z))
                    return com.example.cas.graph.ComplexIntegrals.residue(body, z, at)
                }
                "taylor" -> {
                    // taylor(f, x = a, n), or taylor(f, x, n) around 0.
                    val (x, at) = pointSpec(f.args[1], "taylor")
                    val order = eval(f.args[2], env)
                    val n = (order as? Num)?.q?.takeIf { it.isInteger }?.num?.toInt() ?: throw MathError("The order must be a whole number")
                    return Calculus.taylor(eval(f.args[0], env + (x.name to x)), x, at, n)
                }
                "lim" -> {
                    // lim(f, x → a), x → a+ from the right, x → a− from the left, a may be ±∞.
                    // The side is a + or − written above the point (an empty box means neither),
                    // or typed straight after it.
                    val spec = f.args[1].items
                    val last = spec.lastOrNull()
                    val raised = (last as? com.example.cas.editor.Pow)?.exp?.items?.singleOrNull()
                    val lastSym = ((raised ?: last) as? com.example.cas.editor.Sym)?.text
                    val side = when {
                        spec.size > 3 && lastSym == "+" -> 1
                        spec.size > 3 && lastSym == "−" -> -1
                        else -> 0
                    }
                    // Drop the side (or an empty box) before reading "x → a".
                    val bare = if (side != 0 || (last is com.example.cas.editor.Pow && last.exp.isEmpty)) spec.dropLast(1) else spec
                    val (x, at) = pointSpec(MathRow(bare.toMutableList()), "lim")
                    return Calculus.limit(eval(f.args[0], env + (x.name to x)), x, at, side)
                }
                "apart" -> {
                    val x = eval(f.args[0], env)
                    val vars = x.freeVars()
                    val v = if ("x" in vars) Sym("x") else vars.singleOrNull()?.let { Sym(it) } ?: throw MathError("apart needs one variable")
                    return Algebra.apart(x, v)
                }
                "log" -> {
                    val x = eval(f.args[1], env)
                    val base = if (f.args[0].isEmpty) num(10) else eval(f.args[0], env)
                    return fn("log", base, x)
                }
            }
            // ∇ is handled first: its order box may be empty, and empty boxes are an error below.
            if (f.name == "grad") {
                        val x0 = eval(f.args[0], env)
                        val order = if (f.args.size < 2 || f.args[1].isEmpty) 1
                        else com.example.cas.cas.Numeric.real(eval(f.args[1], env)).let {
                            if (it < 1 || it > 4 || it != Math.rint(it)) throw MathError("The order of \$\\nabla\$ must be 1, 2, 3 or 4") else it.toInt()
                        }
                        return when (order) {
                            1 -> com.example.cas.cas.VectorCalculus.grad(x0, coordinates)
                            2 -> com.example.cas.cas.VectorCalculus.laplacian(x0, coordinates)
                            else -> {
                                var v = com.example.cas.cas.VectorCalculus.laplacian(x0, coordinates)
                                repeat(order - 2) { v = com.example.cas.cas.VectorCalculus.laplacian(v, coordinates) }
                                v
                            }
                        }
            }
            val args = f.args.map { eval(it, env) }
            val x = args[0]
            return when (f.name) {
                "expand" -> Algebra.expand(x)
                "together" -> Algebra.together(x).let { (n, d) -> if (d == ONE) n else div(n, Algebra.factor(d)) }
                // ∇ⁿ: n gradients in a row (∇² is the Laplacian); an empty order means one.
                "div" -> com.example.cas.cas.VectorCalculus.div(x, coordinates)
                "curl" -> com.example.cas.cas.VectorCalculus.curl(x, coordinates)
                "laplacian" -> com.example.cas.cas.VectorCalculus.laplacian(x, coordinates)
                "jacobian" -> com.example.cas.cas.VectorCalculus.jacobian(x, coordinates)
                "hessian" -> com.example.cas.cas.VectorCalculus.hessian(x, coordinates)
                "eigvals" -> LinearAlgebra.eigenvalues(x)
                "eigvecs" -> LinearAlgebra.eigenvectors(x)
                "charpoly" -> LinearAlgebra.charpoly(x)
                "factor" -> Algebra.factor(x)
                "simplify" -> Algebra.simplify(x)
                "approx" -> Numeric.approx(x)
                "sin", "cos", "tan" -> fn(f.name, if (angle == AngleUnit.Degrees) mul(x, div(PI, num(180))) else x)
                "asin", "acos", "atan" -> fn(f.name, x).let { if (angle == AngleUnit.Degrees) mul(it, div(num(180), PI)) else it }
                in MORE_TRIG -> moreTrig(f.name, args)
                in SIGNALS -> signal(f.name, args)
                in com.example.cas.cas.NumberTheory.NAMES -> com.example.cas.cas.NumberTheory.eval(f.name, args) ?: com.example.cas.cas.Fn(f.name, args)
                in DISTRIBUTIONS -> distribution(f.name, args)
                in com.example.cas.cas.MoreMath.VECTORS -> com.example.cas.cas.MoreMath.vectors(f.name, args, ::outAngle, ::inAngle)
                in com.example.cas.cas.MoreMath.INTEGERS -> com.example.cas.cas.MoreMath.integers(f.name, args) ?: com.example.cas.cas.Fn(f.name, args)
                in com.example.cas.cas.MoreMath.POLYNOMIALS -> com.example.cas.cas.MoreMath.polynomial(f.name, args) ?: com.example.cas.cas.Fn(f.name, args)
                // Rising x(x + 1)…(x + n − 1) and falling x(x − 1)…(x − n + 1) factorials: x may be a letter.
                "rising", "falling" -> {
                    if (args.size != 2) throw MathError("${f.name} takes 2 values")
                    val n = (args[1] as? Num)?.q?.takeIf { it.isInteger && it.signum >= 0 }?.num?.toInt()?.takeIf { it <= 200 }
                        ?: throw MathError("The second value must be a whole number up to 200")
                    (0 until n).fold(ONE as Expr) { acc, i -> Algebra.expand(mul(acc, add(x, num(if (f.name == "rising") i.toLong() else -i.toLong())))) }
                }
                else -> fn(f.name, *args.toTypedArray())
            }
        }

        /** An angle in, in the angle unit set: radians for the functions below. */
        private fun inAngle(x: Expr) = if (angle == AngleUnit.Degrees) mul(x, div(PI, num(180))) else x
        /** An angle out, in the angle unit set. */
        private fun outAngle(x: Expr) = if (angle == AngleUnit.Degrees) mul(x, div(num(180), PI)) else x

        /**
         * The reciprocal trigonometric and hyperbolic functions and their inverses, atan2, hypot
         * and sinc, written in the six basic ones, so they simplify, differentiate, integrate and
         * graph as those do.
         */
        private fun moreTrig(name: String, a: List<Expr>): Expr {
            val x = a[0]
            fun need(n: Int) { if (a.size != n) throw MathError("$name takes $n values") }
            return when (name) {
                "sec" -> div(ONE, fn("cos", inAngle(x)))
                "csc" -> div(ONE, fn("sin", inAngle(x)))
                "cot" -> div(fn("cos", inAngle(x)), fn("sin", inAngle(x)))
                "asec" -> outAngle(fn("acos", div(ONE, x)))
                "acsc" -> outAngle(fn("asin", div(ONE, x)))
                "acot" -> outAngle(fn("atan", div(ONE, x)))
                "sech" -> div(ONE, fn("cosh", x))
                "csch" -> div(ONE, fn("sinh", x))
                "coth" -> div(fn("cosh", x), fn("sinh", x))
                "asech" -> fn("acosh", div(ONE, x))
                "acsch" -> fn("asinh", div(ONE, x))
                "acoth" -> fn("atanh", div(ONE, x))
                // sinc x = sin x / x, with sinc 0 = 1.
                "sinc" -> if (x == com.example.cas.cas.ZERO) ONE else div(fn("sin", x), x)
                // hypot(x, y) = √(x² + y²).
                "hypot" -> { need(2); pow(add(pow(x, 2), pow(a[1], 2)), HALF) }
                // atan2(y, x): the angle of the point (x, y), from −π to π.
                // At numbers, exactly (atan2(1, 1) = π/4); with letters, 2 atan(y / (√(x² + y²) + x)), which graphs.
                "atan2" -> { need(2); outAngle(
                    if (x.isConstant && a[1].isConstant) fn("arg", add(a[1], mul(I, x)))
                    else mul(num(2), fn("atan", div(x, add(pow(add(pow(x, 2), pow(a[1], 2)), HALF), a[1]))))
                ) }
                else -> fn(name, *a.toTypedArray())
            }
        }

        /**
         * Waveforms and piecewise functions for signals, written in |x|, ⌊x⌋, sgn, min and max,
         * so they graph, and simplify at numbers.
         */
        private fun signal(name: String, a: List<Expr>): Expr {
            val x = a[0]
            fun need(n: Int) { if (a.size != n) throw MathError("$name takes $n values") }
            fun heaviside(t: Expr) = div(add(ONE, fn("sgn", t)), num(2))
            fun clamp(t: Expr, lo: Expr, hi: Expr) = fn("min", fn("max", t, lo), hi)
            return when (name) {
                "heaviside" -> heaviside(x)
                // The unit box: 1 for |x| < ½.
                "rect" -> heaviside(sub(HALF, fn("abs", x)))
                // The unit triangle: 1 − |x|, down to 0.
                "tri" -> fn("max", sub(ONE, fn("abs", x)), com.example.cas.cas.ZERO)
                "ramp" -> fn("max", x, com.example.cas.cas.ZERO)
                // Period-1 waves: the sawtooth from 0 up to 1, the square ±1, the triangle ±1.
                "sawtooth" -> sub(x, fn("floor", x))
                "squarewave" -> fn("sgn", fn("sin", mul(num(2), PI, x)))
                "trianglewave" -> sub(mul(num(4), fn("abs", sub(x, fn("floor", add(x, HALF))))), ONE)
                "clamp" -> { need(3); clamp(x, a[1], a[2]) }
                // lerp(a, b, t) = a + (b − a) t.
                "lerp" -> { need(3); add(x, mul(sub(a[1], x), a[2])) }
                // 3u² − 2u³ with u = x clamped to [0, 1].
                "smoothstep" -> clamp(x, com.example.cas.cas.ZERO, ONE).let { u -> mul(pow(u, 2), sub(num(3), mul(num(2), u))) }
                "sigmoid" -> div(ONE, add(ONE, pow(E, neg(x))))
                "softplus" -> fn("ln", add(ONE, pow(E, x)))
                "gauss" -> pow(E, neg(pow(x, 2)))
                // 1 between a and b, 0 elsewhere.
                "pulse" -> { need(3); sub(heaviside(sub(x, a[1])), heaviside(sub(x, a[2]))) }
                // x brought into [a, b) by whole periods b − a.
                "wrap" -> { need(3); val w = sub(a[2], a[1]); sub(x, mul(w, fn("floor", div(sub(x, a[1]), w)))) }
                else -> fn(name, *a.toTypedArray())
            }
        }

        /**
         * Probability distributions: densities and distribution functions, written in e, ln, erf,
         * atan, Γ and the incomplete Γ, so they simplify, graph and integrate.
         */
        private fun distribution(name: String, a: List<Expr>): Expr {
            fun need(n: Int) { if (a.size != n) throw MathError("$name takes $n values") }
            val x = a[0]
            val zero = com.example.cas.cas.ZERO
            return when (name) {
                // Exponential with rate λ.
                "exppdf" -> { need(2); mul(a[1], pow(E, neg(mul(a[1], x)))) }
                "expcdf" -> { need(2); sub(ONE, pow(E, neg(mul(a[1], x)))) }
                // Uniform on [a, b].
                "unifpdf" -> { need(3); div(sub(div(add(ONE, fn("sgn", sub(x, a[1]))), num(2)), div(add(ONE, fn("sgn", sub(x, a[2]))), num(2))), sub(a[2], a[1])) }
                "unifcdf" -> { need(3); fn("min", fn("max", div(sub(x, a[1]), sub(a[2], a[1])), zero), ONE) }
                // Geometric: the first success on trial k, each with probability p.
                "geompdf" -> { need(2); mul(pow(sub(ONE, x), sub(a[1], ONE)), x) }
                "geomcdf" -> { need(2); sub(ONE, pow(sub(ONE, x), a[1])) }
                // Poisson: at most k events at rate λ.
                "poissoncdf" -> {
                    need(2)
                    val k = (a[1] as? Num)?.q?.takeIf { it.isInteger && it.signum >= 0 }?.num?.toInt()?.takeIf { it <= 1000 } ?: throw MathError("k must be a whole number up to 1000")
                    Algebra.simplify(mul(pow(E, neg(x)), add((0..k).map { i -> div(pow(x, num(i.toLong())), fn("fact", num(i.toLong()))) })))
                }
                // χ² with k degrees of freedom.
                "chi2pdf" -> { need(2); val h = div(a[1], num(2)); div(mul(pow(x, sub(h, ONE)), pow(E, neg(div(x, num(2))))), mul(pow(num(2), h), fn("gamma", h))) }
                "chi2cdf" -> { need(2); val h = div(a[1], num(2)); sub(ONE, div(fn("gammainc", h, div(x, num(2))), fn("gamma", h))) }
                // Log-normal: ln X normal with mean μ and standard deviation σ.
                "lognpdf" -> { need(3); div(pow(E, neg(div(pow(sub(fn("ln", x), a[1]), 2), mul(num(2), pow(a[2], 2))))), mul(x, a[2], pow(mul(num(2), PI), HALF))) }
                "logncdf" -> { need(3); div(add(ONE, fn("erf", div(sub(fn("ln", x), a[1]), mul(a[2], pow(num(2), HALF))))), num(2)) }
                // Cauchy with centre x₀ and scale γ.
                "cauchypdf" -> { need(3); div(ONE, mul(PI, a[2], add(ONE, pow(div(sub(x, a[1]), a[2]), 2)))) }
                "cauchycdf" -> { need(3); add(HALF, div(fn("atan", div(sub(x, a[1]), a[2])), PI)) }
                // Weibull with shape k and scale λ.
                "weibpdf" -> { need(3); val u = div(x, a[2]); mul(div(a[1], a[2]), pow(u, sub(a[1], ONE)), pow(E, neg(pow(u, a[1])))) }
                "weibcdf" -> { need(3); sub(ONE, pow(E, neg(pow(div(x, a[2]), a[1])))) }
                else -> fn(name, *a.toTypedArray())
            }
        }

        /** "x → a" or "x = a" (or just "x", meaning a = 0): the variable and the point. */
        private fun pointSpec(row: MathRow, what: String): Pair<Sym, Expr> {
            val spec = row.items
            val name = (spec.firstOrNull() as? com.example.cas.editor.Sym)?.text
            if (name == null || name.length != 1 || !name[0].isLetter()) throw MathError("In $what, write the variable as x = a")
            val at = if (spec.size > 2 && (spec[1] as? com.example.cas.editor.Sym)?.text in setOf("=", "→")) RowParser(spec.drop(2), env).parse() else com.example.cas.cas.ZERO
            return Sym(name) to at
        }

        /** The values of a list argument: "2, 4, 4, 5" or a single vector [2, 4, 4, 5]. */
        private fun listArgument(r: MathRow): List<Expr> {
            val parts = splitCommas(r.items).filter { it.isNotEmpty() }.map { RowParser(it, env).parse() }
            if (parts.size == 1 && parts[0] is com.example.cas.cas.Mat) return (parts[0] as com.example.cas.cas.Mat).cells
            return parts
        }

        /** |z − a| = r (or |z| = r): the variable, the center a and the radius r. */
        private fun circleSpec(e: Expr): Triple<Sym, Expr, Expr> {
            val eq = e as? com.example.cas.cas.Eq ?: throw MathError("Write the circle as \$|z - a| = r\$")
            val abs = eq.lhs as? com.example.cas.cas.Fn
            if (abs == null || abs.name != "abs") throw MathError("Write the circle as \$|z - a| = r\$")
            val inner = abs.args[0]
            val z = inner.freeVars().singleOrNull()?.let { Sym(it) } ?: throw MathError("Write the circle as \$|z - a| = r\$")
            val cs = Algebra.coefficients(Algebra.expand(inner), z)
            if (cs == null || cs.size != 2 || cs[1] != com.example.cas.cas.ONE) throw MathError("Write the circle as \$|z - a| = r\$")
            return Triple(z, Algebra.simplify(com.example.cas.cas.neg(cs[0])), eq.rhs)
        }

        /** Splits items at commas that aren't inside brackets. */
        private fun splitCommas(items: List<Node>): List<List<Node>> {
            val out = mutableListOf(mutableListOf<Node>())
            var depth = 0
            for (n in items) {
                val t = (n as? com.example.cas.editor.Sym)?.text
                if (t == "(") depth++
                if (t == ")") depth--
                if (t == "," && depth == 0) out += mutableListOf<Node>() else out.last() += n
            }
            return out
        }

        private fun pickVariables(eqs: List<Expr>): List<Sym> {
            val vars = eqs.flatMap { it.freeVars() }.toSortedSet().map { Sym(it) }
            if (vars.size > eqs.size) throw MathError("Write the unknowns to solve for in the second box")
            return vars
        }

        /**
         * dsolve(y′ = 2y, y(0) = 3): the first part is the equation, the rest
         * are conditions y(a) = b, y′(a) = b. The unknown is the letter with
         * primes; the variable is x (or t if the equation uses t).
         */
        /** A differential equation's parts for the steps: the equation (y′ as a symbol), y and x. */
        fun odeParts(items: List<Node>): Triple<Expr, Sym, Sym> {
            val first = splitCommas(items).first()
            val y = first.indices.firstNotNullOfOrNull { k ->
                val a = (first[k] as? com.example.cas.editor.Sym)?.text
                val b = (first.getOrNull(k + 1) as? com.example.cas.editor.Sym)?.text
                if (a != null && a.length == 1 && a[0].isLetter() && b == "′") Sym(a) else null
            } ?: throw MathError("Write the equation with \$y'\$, e.g. \$y' = 2y\$")
            val bound = env + (y.name to y) + (1..4).associate { y.name + "′".repeat(it) to com.example.cas.cas.Ode.derivativeSymbol(y, it) }
            val eq = RowParser(first, bound + ("x" to Sym("x")) + ("t" to Sym("t"))).parse()
            val x = if (!eq.freeOf(Sym("t")) && eq.freeOf(Sym("x"))) Sym("t") else Sym("x")
            return Triple(eq, y, x)
        }

        private fun dsolve(items: List<Node>): Expr {
            val parts = splitCommas(items)
            val first = parts.first()
            val y = first.indices.firstNotNullOfOrNull { k ->
                val a = (first[k] as? com.example.cas.editor.Sym)?.text
                val b = (first.getOrNull(k + 1) as? com.example.cas.editor.Sym)?.text
                if (a != null && a.length == 1 && a[0].isLetter() && b == "′") Sym(a) else null
            } ?: throw MathError("Write the equation with \$y'\$, e.g. \$y' = 2y\$")
            val bound = env + (y.name to y) + (1..4).associate { y.name + "′".repeat(it) to com.example.cas.cas.Ode.derivativeSymbol(y, it) }
            val eq = RowParser(first, bound + ("x" to Sym("x")) + ("t" to Sym("t"))).parse()
            val x = if (!eq.freeOf(Sym("t")) && eq.freeOf(Sym("x"))) Sym("t") else Sym("x")
            val conditions = parts.drop(1).map { cond ->
                // y(a) = b  or  y′(a) = b
                var k = 0
                val name = (cond.getOrNull(k++) as? com.example.cas.editor.Sym)?.text
                if (name != y.name) throw MathError("Write conditions as ${y.name}(0) = 1")
                var order = 0
                while ((cond.getOrNull(k) as? com.example.cas.editor.Sym)?.text == "′") { order++; k++ }
                if ((cond.getOrNull(k) as? com.example.cas.editor.Sym)?.text != "(") throw MathError("Write conditions as ${y.name}(0) = 1")
                val close = cond.indexOfFirst { (it as? com.example.cas.editor.Sym)?.text == ")" }
                val eqAt = cond.indexOfFirst { (it as? com.example.cas.editor.Sym)?.text == "=" }
                if (close < 0 || eqAt != close + 1) throw MathError("Write conditions as ${y.name}(0) = 1")
                com.example.cas.cas.Ode.Condition(order, RowParser(cond.subList(k + 1, close), env).parse(), RowParser(cond.drop(eqAt + 1), env).parse())
            }
            return com.example.cas.cas.Ode.solve(eq, y, x, conditions)
        }

        private fun pickVariable(eq: Expr): Sym {
            val vars = eq.freeVars()
            return when {
                "x" in vars -> Sym("x")
                vars.size == 1 -> Sym(vars.first())
                vars.isEmpty() -> throw MathError("There's no variable to solve for")
                else -> throw MathError("Write the variable to solve for in the second box")
            }
        }
    }

    companion object {
        /** Function names that may appear as plain words in a reused answer, e.g. "ln" in ln|x|. */
        val FUNCTION_WORDS = setOf("ln", "sin", "cos", "tan")
        val MORE_TRIG = setOf("sec", "csc", "cot", "asec", "acsc", "acot", "sech", "csch", "coth", "asech", "acsch", "acoth", "sinc", "hypot", "atan2")
        val DISTRIBUTIONS = setOf("exppdf", "expcdf", "unifpdf", "unifcdf", "geompdf", "geomcdf", "poissoncdf", "chi2pdf", "chi2cdf", "lognpdf", "logncdf", "cauchypdf", "cauchycdf", "weibpdf", "weibcdf")
        val SIGNALS = setOf("heaviside", "rect", "tri", "ramp", "sawtooth", "squarewave", "trianglewave", "clamp", "lerp", "smoothstep", "sigmoid", "softplus", "gauss", "wrap", "pulse")
        val RELATIONS = setOf("<", ">", "≤", "≥")
    }
}

/** A function defined on its own line, like f(x) = x² or f(x, y, z) = …: its variables and its body. */
data class UserFunction(val variables: List<String>, val body: Expr) {
    /** For one-variable functions. */
    constructor(variable: String, body: Expr) : this(listOf(variable), body)
    val variable: String get() = variables.first()

    companion object {
        /**
         * If [items] is a definition f(x) = …, f(x, y) = … (or with :=), its name, variables
         * and the body's items; otherwise null. The name and variables are single letters.
         */
        fun definition(items: List<com.example.cas.editor.Node>): Triple<String, List<String>, List<com.example.cas.editor.Node>>? {
            fun t(k: Int) = (items.getOrNull(k) as? com.example.cas.editor.Sym)?.text
            val name = t(0) ?: return null
            if (name.length != 1 || !name[0].isLetter() || name in setOf("e", "i", "y", "r") || t(1) != "(") return null
            val vars = ArrayList<String>()
            var k = 2
            while (true) {
                val v = t(k) ?: return null
                if (v.length != 1 || !v[0].isLetter()) return null
                vars += v
                when (t(k + 1)) {
                    "," -> k += 2
                    ")" -> { k += 2; break }
                    else -> return null
                }
            }
            if (vars.toSet().size != vars.size) return null
            if (t(k) != "=" && t(k) != ":=") return null
            return Triple(name, vars, items.drop(k + 1))
        }
    }
}
