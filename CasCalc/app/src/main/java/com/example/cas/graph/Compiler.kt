package com.example.cas.graph

import com.example.cas.cas.Add
import com.example.cas.cas.Eq
import com.example.cas.cas.Expr
import com.example.cas.cas.Flt
import com.example.cas.cas.Fn
import com.example.cas.cas.Mat
import com.example.cas.cas.MathError
import com.example.cas.cas.Mul
import com.example.cas.cas.Num
import com.example.cas.cas.Numeric
import com.example.cas.cas.Pow
import com.example.cas.cas.Seq
import com.example.cas.cas.Sym
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor

/** A compiled real function of the variables, in the order given to [Compiler.compile]. */
fun interface RealFunction {
    operator fun invoke(v: DoubleArray): Double
}

/**
 * Turns a symbolic expression into a fast numeric function for plotting.
 * Plotting evaluates each function thousands of times per frame, so the
 * expression tree is turned into a tree of small closures once. Results are
 * real: anything complex or undefined gives NaN, which leaves a gap in the
 * graph. Odd roots of negative numbers stay real (∛−8 = −2), as on a graph
 * you'd expect.
 */
object Compiler {
    fun compile(e: Expr, vars: List<String>): RealFunction = when (e) {
        is Num -> e.q.toDouble().let { c -> RealFunction { c } }
        is Flt -> e.d.let { c -> RealFunction { c } }
        is Sym -> when (e.name) {
            "π" -> RealFunction { PI }
            "e" -> RealFunction { Math.E }
            "i" -> RealFunction { Double.NaN }
            "∞" -> RealFunction { Double.POSITIVE_INFINITY }
            else -> {
                val k = vars.indexOf(e.name)
                if (k < 0) throw MathError("${e.name} has no value")
                RealFunction { it[k] }
            }
        }
        is Add -> {
            val fs = e.terms.map { compile(it, vars) }.toTypedArray()
            RealFunction { v -> var s = 0.0; for (f in fs) s += f(v); s }
        }
        is Mul -> {
            val fs = e.factors.map { compile(it, vars) }.toTypedArray()
            RealFunction { v -> var p = 1.0; for (f in fs) p *= f(v); p }
        }
        is Pow -> power(e, vars)
        is Fn -> function(e, vars)
        is Mat, is Eq, is Seq, is com.example.cas.cas.Rel -> throw MathError("That can't be graphed")
    }

    private fun power(e: Pow, vars: List<String>): RealFunction {
        val b = compile(e.base, vars)
        val x = e.exp
        if (x is Num && x.q.isInteger && x.q.num.bitLength() < 31) {
            val n = x.q.num.toInt()
            return when (n) {
                2 -> RealFunction { v -> val t = b(v); t * t }
                3 -> RealFunction { v -> val t = b(v); t * t * t }
                -1 -> RealFunction { v -> 1.0 / b(v) }
                else -> RealFunction { v -> Math.pow(b(v), n.toDouble()) }
            }
        }
        if (x is Num && x.q.den.testBit(0) && x.q.den.bitLength() < 31 && x.q.num.bitLength() < 31) {
            // Odd denominator: real root of negative numbers too.
            val p = x.q.num.toInt()
            val q = x.q.den.toDouble()
            val oddNumerator = p % 2 != 0
            return RealFunction { v ->
                val t = b(v)
                val r = Math.pow(abs(t), p / q)
                if (t < 0 && oddNumerator) -r else r
            }
        }
        if (e.base == com.example.cas.cas.E) {
            val ex = compile(x, vars)
            return RealFunction { v -> kotlin.math.exp(ex(v)) }
        }
        val ex = compile(x, vars)
        return RealFunction { v -> Math.pow(b(v), ex(v)) }
    }

    private fun function(e: Fn, vars: List<String>): RealFunction {
        // Σ kept as it is (no closed form): its letter is one more variable; added up at each point.
        if ((e.name == "sum" || e.name == "product") && e.args.size == 4 && e.args[1] is com.example.cas.cas.Sym) {
            val body = compile(e.args[0], vars + (e.args[1] as com.example.cas.cas.Sym).name)
            val lo = compile(e.args[2], vars)
            val infinite = e.args[3] == com.example.cas.cas.INF
            val hi = if (infinite) null else compile(e.args[3], vars)
            val product = e.name == "product"
            return RealFunction { v ->
                val w = v.copyOf(v.size + 1)
                val from = Math.round(lo(v))
                fun term(i: Long): Double { w[v.size] = i.toDouble(); return body(w) }
                if (infinite && !product) com.example.cas.cas.Numeric.series(from, 20_000) { i -> com.example.cas.cas.CD(term(i)) }.re
                else {
                    val to = hi?.let { Math.round(it(v)) } ?: return@RealFunction Double.NaN
                    if (to - from > 200_000) return@RealFunction Double.NaN
                    var acc = if (product) 1.0 else 0.0
                    var i = from
                    while (i <= to) { acc = if (product) acc * term(i) else acc + term(i); i++ }
                    acc
                }
            }
        }
        val a = e.args.map { compile(it, vars) }
        val f = a[0]
        fun one(op: (Double) -> Double) = RealFunction { v -> op(f(v)) }
        return when (e.name) {
            "sin" -> one { kotlin.math.sin(it) }
            "cos" -> one { kotlin.math.cos(it) }
            "tan" -> one { kotlin.math.tan(it) }
            "asin" -> one { kotlin.math.asin(it) }
            "acos" -> one { kotlin.math.acos(it) }
            "atan" -> one { kotlin.math.atan(it) }
            "sinh" -> one { kotlin.math.sinh(it) }
            "cosh" -> one { kotlin.math.cosh(it) }
            "tanh" -> one { kotlin.math.tanh(it) }
            "asinh" -> one { kotlin.math.asinh(it) }
            "acosh" -> one { kotlin.math.acosh(it) }
            "atanh" -> one { kotlin.math.atanh(it) }
            "ln" -> one { if (it > 0) kotlin.math.ln(it) else Double.NaN }
            "log" -> { val g = a[1]; RealFunction { v -> val b = f(v); val x = g(v); if (b > 0 && x > 0) kotlin.math.ln(x) / kotlin.math.ln(b) else Double.NaN } }
            "abs" -> one { abs(it) }
            "floor" -> one { floor(it) }
            "ceil" -> one { kotlin.math.ceil(it) }
            "round" -> one { floor(it + 0.5) }
            "fact" -> one { if (it == Math.rint(it) && it < 0) Double.NaN else Numeric.gamma(it + 1) }
            "binom" -> { val g = a[1]; RealFunction { v -> val n = f(v); val k = g(v); Numeric.gamma(n + 1) / (Numeric.gamma(k + 1) * Numeric.gamma(n - k + 1)) } }
            "mod" -> { val g = a[1]; RealFunction { v -> val p = f(v); val q = g(v); p - q * floor(p / q) } }
            "min" -> { val g = a[1]; RealFunction { v -> minOf(f(v), g(v)) } }
            "max" -> { val g = a[1]; RealFunction { v -> maxOf(f(v), g(v)) } }
            "sgn" -> one { kotlin.math.sign(it) }
            "erf" -> one { com.example.cas.cas.Statistics.erf(it) }
            "digamma" -> one { com.example.cas.cas.Statistics.digamma(it) }
            // What derivatives of ψ and ζ come to (as on the complex plane, on the real line).
            "trigamma" -> one { com.example.cas.cas.ComplexMath.trigamma(com.example.cas.cas.CD(it)).re }
            "zetaprime" -> one { com.example.cas.cas.ComplexMath.zetaDerivative(com.example.cas.cas.CD(it), 1).re }
            "zetaprime2" -> one { com.example.cas.cas.ComplexMath.zetaDerivative(com.example.cas.cas.CD(it), 2).re }
            "gamma" -> one { if (it == Math.rint(it) && it <= 0) Double.NaN else Numeric.gamma(it) }
            "hurwitz" -> { val q = a[1]; RealFunction { v -> com.example.cas.cas.ComplexMath.hurwitz(com.example.cas.cas.CD(f(v)), q(v)).let { w -> if (kotlin.math.abs(w.im) < 1e-9) w.re else Double.NaN } } }
            "polylog" -> { val z = a[1]; RealFunction { v -> com.example.cas.cas.ComplexMath.polylog(com.example.cas.cas.CD(f(v)), com.example.cas.cas.CD(z(v))).let { w -> if (kotlin.math.abs(w.im) < 1e-9) w.re else Double.NaN } } }
            "zeta" -> one { val w = com.example.cas.cas.ComplexMath.zeta(com.example.cas.cas.CD(it)); if (kotlin.math.abs(w.im) < 1e-9) w.re else Double.NaN }
            "frac" -> one { it - floor(it) }
            "Re", "conj" -> f
            "Im" -> RealFunction { 0.0 }
            "arg" -> one { if (it < 0) PI else 0.0 }
            // atan2(y, x): the angle of (x, y), from −π to π (θ in cylindrical and spherical coordinates).
            "atan2" -> { val g = a[1]; RealFunction { v -> kotlin.math.atan2(f(v), g(v)) } }
            else -> throw MathError("${e.name} can't be graphed")
        }
    }
}
