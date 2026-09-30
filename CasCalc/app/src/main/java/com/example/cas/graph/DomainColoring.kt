package com.example.cas.graph

import com.example.cas.cas.Add
import com.example.cas.cas.CD
import com.example.cas.cas.ComplexMath
import com.example.cas.cas.E
import com.example.cas.cas.Eq
import com.example.cas.cas.Expr
import com.example.cas.cas.Flt
import com.example.cas.cas.Fn
import com.example.cas.cas.Mat
import com.example.cas.cas.MathError
import com.example.cas.cas.Mul
import com.example.cas.cas.Num
import com.example.cas.cas.Pow
import com.example.cas.cas.Rel
import com.example.cas.cas.Seq
import com.example.cas.cas.Sym
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt

/** A compiled function of the complex variable z; [p] holds the (real) slider values. */
fun interface ComplexFunction {
    operator fun invoke(z: CD, p: DoubleArray): CD
}

/**
 * Compiles a symbolic expression in z (plus real parameters) into a complex
 * function, the complex counterpart of [Compiler]. Every function uses its
 * principal branch, so ln and √ have their cuts along the negative real axis.
 */
object ComplexCompiler {
    fun compile(e: Expr, vars: List<String>): ComplexFunction = compile(e, vars, emptyMap())

    /** [bound]: letters with values of their own, an integral's variable while it's integrated. */
    private fun compile(e: Expr, vars: List<String>, bound: Map<String, ComplexFunction>): ComplexFunction = when (e) {
        is Num -> CD(e.q.toDouble()).let { c -> ComplexFunction { _, _ -> c } }
        is Flt -> CD(e.d).let { c -> ComplexFunction { _, _ -> c } }
        is Sym -> when (e.name) {
            "π" -> CD(PI).let { c -> ComplexFunction { _, _ -> c } }
            "e" -> CD(Math.E).let { c -> ComplexFunction { _, _ -> c } }
            "i" -> CD(0.0, 1.0).let { c -> ComplexFunction { _, _ -> c } }
            "∞" -> CD(Double.POSITIVE_INFINITY).let { c -> ComplexFunction { _, _ -> c } }
            in bound -> bound.getValue(e.name)
            else -> {
                val k = vars.indexOf(e.name)
                when {
                    k == 0 -> ComplexFunction { z, _ -> z }
                    k > 0 -> ComplexFunction { _, p -> CD(p[k - 1]) }
                    else -> throw MathError("${e.name} has no value")
                }
            }
        }
        is Add -> {
            val fs = e.terms.map { compile(it, vars, bound) }.toTypedArray()
            ComplexFunction { z, p -> var s = CD(0.0); for (f in fs) s = s + f(z, p); s }
        }
        is Mul -> {
            val fs = e.factors.map { compile(it, vars, bound) }.toTypedArray()
            ComplexFunction { z, p -> var s = CD(1.0); for (f in fs) s = s * f(z, p); s }
        }
        is Pow -> power(e, vars, bound)
        is Fn -> function(e, vars, bound)
        is Mat, is Eq, is Seq, is Rel -> throw MathError("That can't be plotted")
    }

    private fun power(e: Pow, vars: List<String>, bound: Map<String, ComplexFunction>): ComplexFunction {
        if (e.base == E) {
            val x = compile(e.exp, vars, bound)
            return ComplexFunction { z, p -> ComplexMath.exp(x(z, p)) }
        }
        val b = compile(e.base, vars, bound)
        val n = (e.exp as? Num)?.q?.takeIf { it.isInteger && it.num.bitLength() < 16 }?.num?.toInt()
        if (n != null) {
            // Integer powers by repeated squaring: exact at z = 0 and fast.
            return ComplexFunction { z, p ->
                val base = b(z, p)
                var result = CD(1.0)
                var sq = base
                var k = kotlin.math.abs(n)
                while (k > 0) {
                    if (k and 1 == 1) result = result * sq
                    sq = sq * sq
                    k = k shr 1
                }
                if (n < 0) CD(1.0) / result else result
            }
        }
        if (e.exp is Num && e.exp.q == com.example.cas.math.Rational.of(1, 2)) {
            return ComplexFunction { z, p -> ComplexMath.sqrt(b(z, p)) }
        }
        val x = compile(e.exp, vars, bound)
        return ComplexFunction { z, p ->
            val base = b(z, p)
            if (base.re == 0.0 && base.im == 0.0) CD(0.0) else ComplexMath.exp(x(z, p) * ComplexMath.ln(base))
        }
    }

    private fun function(e: Fn, vars: List<String>, bound: Map<String, ComplexFunction>): ComplexFunction {
        if (e.name == "integral") return integral(e, vars, bound)
        val a = e.args.map { compile(it, vars, bound) }
        val f = a[0]
        fun one(op: (CD) -> CD) = ComplexFunction { z, p -> op(f(z, p)) }
        return when (e.name) {
            "sin" -> one(ComplexMath::sin)
            "cos" -> one(ComplexMath::cos)
            "tan" -> one(ComplexMath::tan)
            "asin" -> one(ComplexMath::asin)
            "acos" -> one(ComplexMath::acos)
            "atan" -> one(ComplexMath::atan)
            "sinh" -> one(ComplexMath::sinh)
            "cosh" -> one(ComplexMath::cosh)
            "tanh" -> one(ComplexMath::tanh)
            "asinh" -> one(ComplexMath::asinh)
            "acosh" -> one(ComplexMath::acosh)
            "atanh" -> one(ComplexMath::atanh)
            "ln" -> one(ComplexMath::ln)
            "log" -> { val g = a[1]; ComplexFunction { z, p -> ComplexMath.ln(g(z, p)) / ComplexMath.ln(f(z, p)) } }
            "abs" -> one { CD(it.abs()) }
            "Re" -> one { CD(it.re) }
            "Im" -> one { CD(it.im) }
            "conj" -> one { CD(it.re, -it.im) }
            "arg" -> one { CD(it.arg()) }
            "sgn" -> one { val m = it.abs(); if (m == 0.0) CD(0.0) else CD(it.re / m, it.im / m) }
            "fact" -> one { ComplexMath.gamma(it + CD(1.0)) }
            "gamma" -> one(ComplexMath::gamma)
            "zeta" -> one(ComplexMath::zeta)
            // What derivatives of Γ, ζ and erf come to.
            "digamma" -> one(ComplexMath::digamma)
            "trigamma" -> one(ComplexMath::trigamma)
            "zetaprime" -> one { ComplexMath.zetaDerivative(it, 1) }
            "zetaprime2" -> one { ComplexMath.zetaDerivative(it, 2) }
            "erf" -> one(ComplexMath::erf)
            "binom" -> { val g = a[1]; ComplexFunction { z, p -> val n = f(z, p); val k = g(z, p); ComplexMath.gamma(n + CD(1.0)) / (ComplexMath.gamma(k + CD(1.0)) * ComplexMath.gamma(n - k + CD(1.0))) } }
            "perm" -> { val g = a[1]; ComplexFunction { z, p -> val n = f(z, p); val k = g(z, p); ComplexMath.gamma(n + CD(1.0)) / ComplexMath.gamma(n - k + CD(1.0)) } }
            // J_a(z), Y_a(z): the order is real (a number or a slider), z anywhere on the plane.
            "besselj", "bessely" -> {
                val order = a[0]; val arg = a[1]
                val second = e.name == "bessely"
                ComplexFunction { z, p -> val n = order(z, p).re; val w = arg(z, p); if (second) ComplexMath.besselY(n, w) else ComplexMath.besselJ(n, w) }
            }
            "floor" -> one { CD(floor(it.re), floor(it.im)) }
            "ceil" -> one { CD(kotlin.math.ceil(it.re), kotlin.math.ceil(it.im)) }
            "round" -> one { CD(floor(it.re + 0.5), floor(it.im + 0.5)) }
            "frac" -> one { CD(it.re - floor(it.re), it.im - floor(it.im)) }
            else -> throw MathError("${e.name} can't be plotted on the complex plane")
        }
    }

    /**
     * ∫ₐᵇ f(t) dt with a or b depending on z: along the straight path from a to b, by 10-point
     * Gauss–Legendre on pieces about half a unit long. (The integration letter is bound to each
     * node in turn; the path is the straight one, so a pole on it gives a jump.)
     */
    private fun integral(e: Fn, vars: List<String>, bound: Map<String, ComplexFunction>): ComplexFunction {
        val t = (e.args[1] as? Sym)?.name ?: throw MathError("That integral can't be plotted")
        val node = ThreadLocal.withInitial { CD(0.0) }
        val body = compile(e.args[0], vars, bound + (t to ComplexFunction { _, _ -> node.get() }))
        val lo = compile(e.args[2], vars, bound)
        val hi = compile(e.args[3], vars, bound)
        return ComplexFunction { z, p ->
            val a = lo(z, p); val b = hi(z, p)
            val span = b - a
            val pieces = (span.abs() * 2).toInt().coerceIn(1, 24)
            val half = span * CD(0.5 / pieces)
            var sum = CD(0.0)
            for (k in 0 until pieces) {
                val mid = a + span * CD((k + 0.5) / pieces)
                for (j in GAUSS_X.indices) {
                    node.set(mid + half * CD(GAUSS_X[j]))
                    sum = sum + body(z, p) * CD(GAUSS_W[j])
                }
            }
            // Every piece is the same length, so dt's scale (half a piece) multiplies the whole sum.
            sum * half
        }
    }

    // 10-point Gauss–Legendre nodes and weights on [−1, 1].
    private val GAUSS_X = doubleArrayOf(-0.9739065285171717, -0.8650633666889845, -0.6794095682990244, -0.4333953941292472, -0.1488743389816312, 0.1488743389816312, 0.4333953941292472, 0.6794095682990244, 0.8650633666889845, 0.9739065285171717)
    private val GAUSS_W = doubleArrayOf(0.0666713443086881, 0.1494513491505806, 0.2190863625159820, 0.2692667193099963, 0.2955242247147529, 0.2955242247147529, 0.2692667193099963, 0.2190863625159820, 0.1494513491505806, 0.0666713443086881)
}

/** Which extra features the domain colouring shows. */
data class ColoringOptions(
    /** Brightness steps at each doubling of |f|: contours of the modulus. */
    val modulusBands: Boolean = true,
    /** Dark lines every 30° of arg f: contours of the phase. */
    val phaseLines: Boolean = false,
    /** Lines where Re f or Im f is a whole number: the image of the grid, showing conformality. */
    val grid: Boolean = false,
    /** The colours for arg f. */
    val colormap: Colormap = Colormap.CLASSIC,
    /** The colormap run backwards (matplotlib's _r maps). */
    val reversed: Boolean = false,
)

/**
 * Domain colouring: each point z is coloured by f(z). Hue is the argument
 * (red = positive real, then yellow, green, cyan, blue, magenta going
 * anticlockwise), brightness is the modulus (zeros black, poles white).
 */
object DomainColoring {
    fun color(w: CD, o: ColoringOptions): Int {
        if (w.re.isNaN() || w.im.isNaN()) return 0xFF808080.toInt()
        val m = w.abs()
        if (m.isInfinite()) return 0xFFFFFFFF.toInt()
        var hue = w.arg() / (2 * PI)
        if (hue < 0) hue += 1.0
        // Brightness from log |f|, symmetric between zeros and poles:
        // 0 → black, 1 → middle, ∞ → white (|f| = 10 is about 80%).
        var light = 0.5 + atan(ln(m) / 1.5) / PI
        if (o.modulusBands && m > 0) {
            val b = ln(m) / ln(2.0)
            val f = b - floor(b)
            light *= 0.82 + 0.18 * f
        }
        if (o.phaseLines) {
            val f = hue * 12 - floor(hue * 12)
            if (f < 0.04 || f > 0.96) light *= 0.55
        }
        if (o.grid) {
            val fr = abs(w.re - Math.rint(w.re))
            val fi = abs(w.im - Math.rint(w.im))
            if (fr < 0.04 || fi < 0.04) light = light * 0.5
        }
        val l = light.coerceIn(0.0, 1.0)
        if (o.colormap == Colormap.CLASSIC) return hsl(if (o.reversed) (1 - hue) % 1.0 else hue, 1.0, l)
        // Cyclic maps start at the positive real axis like the classic wheel; the others run
        // from arg f = −π to π.
        val t = if (o.colormap.cyclic) hue else (w.arg() + PI) / (2 * PI)
        val c = o.colormap.rgb(if (o.reversed) 1 - t else t)
        return shade(c, l)
    }

    /** Darkens towards black below the middle brightness and lightens towards white above it, as HSL does. */
    fun shade(rgb: Int, light: Double): Int {
        fun ch(shift: Int): Int {
            val v = ((rgb shr shift) and 0xFF) / 255.0
            val out = if (light < 0.5) v * 2 * light else v + (1 - v) * (2 * light - 1)
            return (out * 255).roundToInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    private fun hsl(h: Double, s: Double, l: Double): Int {
        val c = (1 - abs(2 * l - 1)) * s
        val hp = h * 6
        val x = c * (1 - abs(hp % 2 - 1))
        val (r1, g1, b1) = when (hp.toInt().coerceIn(0, 5)) {
            0 -> Triple(c, x, 0.0)
            1 -> Triple(x, c, 0.0)
            2 -> Triple(0.0, c, x)
            3 -> Triple(0.0, x, c)
            4 -> Triple(x, 0.0, c)
            else -> Triple(c, 0.0, x)
        }
        val mm = l - c / 2
        fun byte(v: Double) = ((v + mm) * 255).roundToInt().coerceIn(0, 255)
        return (0xFF shl 24) or (byte(r1) shl 16) or (byte(g1) shl 8) or byte(b1)
    }

    /**
     * Colours a [width] × [height] image of the view, one pixel per sample
     * (row 0 at the top, like the screen). [shouldStop] lets a newer render
     * cancel this one.
     */
    fun render(
        f: ComplexFunction,
        params: DoubleArray,
        view: Viewport,
        width: Int,
        height: Int,
        o: ColoringOptions,
        shouldStop: () -> Boolean = { false },
    ): IntArray? {
        val px = IntArray(width * height)
        for (j in 0 until height) {
            if (j % 16 == 0 && shouldStop()) return null
            val y = view.yMax - (j + 0.5) / height * view.height
            for (i in 0 until width) {
                val x = view.xMin + (i + 0.5) / width * view.width
                val w = try { f(CD(x, y), params) } catch (e: RuntimeException) { CD(Double.NaN) }
                px[j * width + i] = color(w, o)
            }
        }
        return px
    }

    /**
     * ∮ f(z) dz along a closed path (the last point joins the first), by the
     * midpoint rule on each segment split into [steps] pieces. Dividing by 2πi
     * gives the sum of the residues inside (for a path going once anticlockwise).
     */
    fun contourIntegral(f: ComplexFunction, params: DoubleArray, path: List<CD>, steps: Int = 8): CD {
        if (path.size < 3) throw MathError("Draw a closed loop")
        var sum = CD(0.0)
        for (k in path.indices) {
            val a = path[k]
            val b = path[(k + 1) % path.size]
            val d = (b - a) / CD(steps.toDouble())
            for (s in 0 until steps) {
                val mid = a + d * CD(s + 0.5)
                sum = sum + f(mid, params) * d
            }
        }
        return sum
    }

    fun residueSum(integral: CD): CD = integral / CD(0.0, 2 * PI)
}
