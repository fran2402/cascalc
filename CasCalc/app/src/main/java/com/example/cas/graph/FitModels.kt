package com.example.cas.graph

import com.example.cas.cas.freeVars
import com.example.cas.cas.subst
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

/**
 * The ready-made models for fitting a table: straight line, polynomials, exponential, power
 * and logarithm, each with its parameters, the equation as LaTeX (with the fitted numbers in),
 * and a good place to start from (exact least squares for polynomials; a straight-line fit of
 * the logarithms for the others), so the fit settles at once.
 */
enum class FitModel(val title: String, val params: List<String>, val latex: String) {
    Linear("Line", listOf("a", "b"), "y = ax + b"),
    Quadratic("Quadratic", listOf("a", "b", "c"), "y = ax^{2} + bx + c"),
    Cubic("Cubic", listOf("a", "b", "c", "d"), "y = ax^{3} + bx^{2} + cx + d"),
    Exponential("Exponential", listOf("a", "b"), "y = ae^{bx}"),
    Power("Power", listOf("a", "b"), "y = ax^{b}"),
    Logarithmic("Logarithm", listOf("a", "b"), "y = a\\ln x + b");

    fun value(x: Double, p: DoubleArray): Double = when (this) {
        Linear -> p[0] * x + p[1]
        Quadratic -> (p[0] * x + p[1]) * x + p[2]
        Cubic -> ((p[0] * x + p[1]) * x + p[2]) * x + p[3]
        Exponential -> p[0] * exp(p[1] * x)
        Power -> p[0] * x.pow(p[1])
        Logarithmic -> p[0] * ln(x) + p[1]
    }

    /** Where to start: exact for polynomials, from a line through the logarithms otherwise. */
    fun start(xs: DoubleArray, ys: DoubleArray): DoubleArray = when (this) {
        Linear -> polynomial(xs, ys, 1)
        Quadratic -> polynomial(xs, ys, 2)
        Cubic -> polynomial(xs, ys, 3)
        Exponential -> {
            val k = ys.indices.filter { ys[it] > 0 }
            if (k.size >= 2) polynomial(DoubleArray(k.size) { xs[k[it]] }, DoubleArray(k.size) { ln(ys[k[it]]) }, 1).let { (b, lnA) -> doubleArrayOf(exp(lnA), b) }
            else doubleArrayOf(ys.average(), 0.0)
        }
        Power -> {
            val k = ys.indices.filter { ys[it] > 0 && xs[it] > 0 }
            if (k.size >= 2) polynomial(DoubleArray(k.size) { ln(xs[k[it]]) }, DoubleArray(k.size) { ln(ys[k[it]]) }, 1).let { (b, lnA) -> doubleArrayOf(exp(lnA), b) }
            else doubleArrayOf(1.0, 1.0)
        }
        Logarithmic -> {
            val k = xs.indices.filter { xs[it] > 0 }
            if (k.size >= 2) polynomial(DoubleArray(k.size) { ln(xs[k[it]]) }, DoubleArray(k.size) { ys[k[it]] }, 1) else doubleArrayOf(1.0, 0.0)
        }
    }

    /** The model's equation with the fitted numbers in, as LaTeX. */
    fun equation(p: DoubleArray, number: (Double) -> String): String {
        fun term(c: Double, tail: String, first: Boolean): String {
            val sign = if (c < 0) "-" else if (first) "" else "+"
            val mag = number(abs(c))
            return (if (first) sign else " $sign ") + (if (tail.isNotEmpty() && mag == "1") "" else mag) + tail
        }
        return when (this) {
            Linear -> "y = " + term(p[0], "x", true) + term(p[1], "", false)
            Quadratic -> "y = " + term(p[0], "x^{2}", true) + term(p[1], "x", false) + term(p[2], "", false)
            Cubic -> "y = " + term(p[0], "x^{3}", true) + term(p[1], "x^{2}", false) + term(p[2], "x", false) + term(p[3], "", false)
            Exponential -> "y = " + term(p[0], "", true) + "\\,e^{" + term(p[1], "x", true) + "}"
            Power -> "y = " + term(p[0], "", true) + "\\,x^{" + number(p[1]).replace("−", "-") + "}"
            Logarithmic -> "y = " + term(p[0], "\\ln x", true) + term(p[1], "", false)
        }
    }

    companion object {
        /** Least-squares polynomial of [degree], highest power first; zeros if it can't be solved. */
        fun polynomial(xs: DoubleArray, ys: DoubleArray, degree: Int): DoubleArray {
            val n = degree + 1
            // Normal equations Σ x^(i+j) c = Σ x^i y, lowest power first.
            val a = Array(n) { i -> DoubleArray(n) { j -> xs.sumOf { it.pow(i + j) } } }
            val b = DoubleArray(n) { i -> xs.indices.sumOf { k -> xs[k].pow(i) * ys[k] } }
            val c = Fit.solve(a, b) ?: DoubleArray(n)
            return c.reversedArray()
        }
    }
}

/**
 * A model typed by hand, like "a sin(bx) + c" or "y = A e^{-x/τ}": every letter other than x is
 * a parameter to fit (in alphabetical order), starting from 1.
 */
class CustomFitModel private constructor(val expr: com.example.cas.cas.Expr, val params: List<String>, private val f: RealFunction) {
    fun value(x: Double, p: DoubleArray): Double = try { f(doubleArrayOf(x) + p) } catch (e: RuntimeException) { Double.NaN }

    /** The model with the fitted numbers in, as LaTeX ("y = …"). */
    fun equation(p: DoubleArray, digits: Int = 5): String {
        var e = expr
        params.forEachIndexed { k, name ->
            val v = java.math.BigDecimal(p[k]).round(java.math.MathContext(digits)).toDouble()
            e = com.example.cas.cas.Algebra.simplify(e.subst(com.example.cas.cas.Sym(name), com.example.cas.cas.Flt(v)))
        }
        return "y = " + com.example.cas.engine.Latex.of(com.example.cas.engine.Formatter.row(e))
    }

    companion object {
        /** The model in [text] (LaTeX or plain), or null if it can't be read or has nothing to fit. */
        fun of(text: String): CustomFitModel? = runCatching {
            // As typed on a keyboard: / is ÷ and * a product (the editor's own signs).
            val typed = text.trim().replace("/", "\\div ").replace("*", " ")
            var e = com.example.cas.engine.Evaluator().evaluate(com.example.cas.engine.LatexParser.parse(typed))
            // "y = …": the right side.
            if (e is com.example.cas.cas.Eq) e = e.rhs
            val params = e.freeVars().filter { it != "x" }.sorted()
            if (params.isEmpty()) return null
            CustomFitModel(e, params, Compiler.compile(e, listOf("x") + params))
        }.getOrNull()
    }
}
