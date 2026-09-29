package com.example.cas.cas

import com.example.cas.math.Rational

class MathError(message: String) : RuntimeException(message)

/**
 * Symbolic expressions. Always build them through [add], [mul], [pow] and
 * [fn] (never the raw constructors), so every expression stays in the
 * simplified canonical form that [Simplify] produces: sums and products are
 * flat and sorted, like terms and equal bases are combined, numbers are exact.
 */
sealed class Expr {
    override fun toString(): String = Printer.plain(this)
}

/** Exact rational number. */
data class Num(val q: Rational) : Expr() {
    override fun toString() = Printer.plain(this)
    constructor(n: Long) : this(Rational.of(n))
}

/** Approximate number, e.g. from a measured constant or a numerical integral. */
data class Flt(val d: Double) : Expr() {
    override fun toString() = Printer.plain(this)
}

/** Variable or named constant: x, y, k…, and "π", "e", "i". */
data class Sym(val name: String) : Expr() {
    override fun toString() = Printer.plain(this)
}

data class Add(val terms: List<Expr>) : Expr() {
    override fun toString() = Printer.plain(this)
}
data class Mul(val factors: List<Expr>) : Expr() {
    override fun toString() = Printer.plain(this)
}
data class Pow(val base: Expr, val exp: Expr) : Expr() {
    override fun toString() = Printer.plain(this)
}

/** Function application: sin, ln, abs, fact, binom, log(base, x)… */
data class Fn(val name: String, val args: List<Expr>) : Expr() {
    override fun toString() = Printer.plain(this)
}

data class Mat(val rows: Int, val cols: Int, val cells: List<Expr>) : Expr() {
    override fun toString() = Printer.plain(this)
    operator fun get(r: Int, c: Int) = cells[r * cols + c]
}

data class Eq(val lhs: Expr, val rhs: Expr) : Expr() {
    override fun toString() = Printer.plain(this)
}

/**
 * Several results: the solutions x = −1, x = 3 ([joiner] ","), or
 * alternatives such as x < −2 or x > 2 and the solution sets of a system
 * ([joiner] "or").
 */
data class Seq(val items: List<Expr>, val joiner: String = ",") : Expr() {
    override fun toString() = Printer.plain(this)
}

/** A chain of comparisons: x < 3, or −2 < x ≤ 2. [ops] are "<", ">", "≤", "≥". */
data class Rel(val parts: List<Expr>, val ops: List<String>) : Expr() {
    override fun toString() = Printer.plain(this)
}

val ZERO = Num(0)
val ONE = Num(1)
val TWO = Num(2)
val MINUS_ONE = Num(-1)
val HALF = Num(Rational.of(1, 2))
val PI = Sym("π")
val E = Sym("e")
val I = Sym("i")
val INF = Sym("∞")

fun num(n: Long) = Num(n)
fun num(p: Long, q: Long) = Num(Rational.of(p, q))
fun sym(name: String) = Sym(name)

fun add(vararg xs: Expr) = Simplify.sum(xs.toList())
fun add(xs: List<Expr>) = Simplify.sum(xs)
fun mul(vararg xs: Expr) = Simplify.product(xs.toList())
fun mul(xs: List<Expr>) = Simplify.product(xs)
fun pow(b: Expr, e: Expr) = Simplify.power(b, e)
fun pow(b: Expr, n: Long) = Simplify.power(b, Num(n))
fun fn(name: String, vararg args: Expr) = Simplify.function(name, args.toList())
fun neg(x: Expr) = mul(MINUS_ONE, x)
fun sub(a: Expr, b: Expr) = add(a, neg(b))
fun div(a: Expr, b: Expr): Expr {
    if (b == ZERO) throw MathError("Can't divide by 0")
    return mul(a, pow(b, MINUS_ONE))
}
fun sqrt(x: Expr) = pow(x, HALF)

val Expr.isNumber get() = this is Num || this is Flt
val Expr.isConstantSymbol get() = this is Sym && name in CONSTANT_SYMBOLS
val CONSTANT_SYMBOLS = setOf("π", "e", "i", "∞")

/** Children, for generic traversal. */
val Expr.children: List<Expr>
    get() = when (this) {
        is Add -> terms
        is Mul -> factors
        is Pow -> listOf(base, exp)
        is Fn -> args
        is Mat -> cells
        is Eq -> listOf(lhs, rhs)
        is Seq -> items
        is Rel -> parts
        else -> emptyList()
    }

/** Variables that appear (π, e and i are not variables). */
fun Expr.freeVars(): Set<String> = when {
    this is Sym -> if (name in CONSTANT_SYMBOLS) emptySet() else setOf(name)
    // A held integral ∫ₐᵇ body d(var): its variable is bound, not free.
    this is Fn && name == "integral" -> (args[0].freeVars() - (args[1] as Sym).name) + args[2].freeVars() + args[3].freeVars()
    // A held contour integral ∮ body d(var) around |var − centre| = radius: var is bound too.
    this is Fn && name == "contour" && args.size == 4 -> (args[0].freeVars() - (args[1] as Sym).name) + args[2].freeVars() + args[3].freeVars()
    // A held Σ or Π: body, variable, lower, upper (the variable is bound).
    // (A substitution can replace the variable with a number; then nothing is bound.)
    this is Fn && (name == "sum" || name == "product") && args.size == 4 ->
        (args[0].freeVars() - setOfNotNull((args[1] as? Sym)?.name)) + args[1].freeVars() + args[2].freeVars() + args[3].freeVars()
    else -> children.flatMapTo(mutableSetOf()) { it.freeVars() }
}

fun Expr.freeOf(x: Sym): Boolean = when {
    this is Sym -> this != x
    this is Fn && name == "integral" && args[1] == x -> args[2].freeOf(x) && args[3].freeOf(x)
    this is Fn && name == "contour" && args.size == 4 && args[1] == x -> args[2].freeOf(x) && args[3].freeOf(x)
    this is Fn && (name == "sum" || name == "product") && args.size == 4 && args[1] == x -> args[2].freeOf(x) && args[3].freeOf(x)
    else -> children.all { it.freeOf(x) }
}

fun Expr.contains(pred: (Expr) -> Boolean): Boolean = pred(this) || children.any { it.contains(pred) }

val Expr.isConstant get() = freeVars().isEmpty()

/** Rebuilds the expression bottom-up through the simplifying builders. */
fun Expr.map(f: (Expr) -> Expr): Expr = when (this) {
    is Add -> add(terms.map(f))
    is Mul -> mul(factors.map(f))
    is Pow -> pow(f(base), f(exp))
    is Fn -> Simplify.function(name, args.map(f))
    is Mat -> Mat(rows, cols, cells.map(f))
    is Eq -> Eq(f(lhs), f(rhs))
    is Seq -> Seq(items.map(f), joiner)
    is Rel -> Rel(parts.map(f), ops)
    else -> this
}

/** Replaces every occurrence of [target] with [value] and re-simplifies. */
fun Expr.subst(target: Expr, value: Expr): Expr = if (this == target) value else map { it.subst(target, value) }

fun Expr.subst(values: Map<String, Expr>): Expr =
    if (this is Sym && name in values) values.getValue(name) else map { it.subst(values) }
