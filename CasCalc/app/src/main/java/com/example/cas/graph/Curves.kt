package com.example.cas.graph

import com.example.cas.cas.Eq
import com.example.cas.cas.Expr
import com.example.cas.cas.MathError
import com.example.cas.cas.Rel
import com.example.cas.cas.Sym
import com.example.cas.cas.freeOf
import com.example.cas.cas.freeVars
import com.example.cas.cas.sub
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * What a line in the 2D graph's list means, decided from what was typed:
 *  - y = f(x), or just f(x)                          → [Explicit]
 *  - r = f(θ), or just f(θ)                          → [Polar]
 *  - (f(t), g(t))                                    → [Parametric]
 *  - any other equation in x and y (x² + y² = 1)     → [Implicit]
 *  - an inequality in x and/or y (y < x², x² + y² ≤ 4) → [Region]
 * Other letters become sliders ([parameters]).
 */
sealed class PlotSpec {
    abstract val parameters: List<String>

    class Explicit(val f: Expr, override val parameters: List<String>) : PlotSpec()
    class Polar(val r: Expr, override val parameters: List<String>) : PlotSpec()
    class Parametric(val x: Expr, val y: Expr, override val parameters: List<String>) : PlotSpec()
    class Implicit(val f: Expr, override val parameters: List<String>) : PlotSpec()
    class Region(val rel: Rel, override val parameters: List<String>) : PlotSpec()

    companion object {
        val X = Sym("x"); val Y = Sym("y"); val R = Sym("r"); val THETA = Sym("θ"); val T = Sym("t")
        private val PLANE = setOf("x", "y", "r", "θ", "t")

        /** Sliders: every letter except this kind of line's own coordinates. */
        private fun params(coords: Set<String>, vararg es: Expr) = es.flatMap { it.freeVars() }.toSet().minus(coords).sorted()
        private val XY = setOf("x", "y")

        /** [parts] is two expressions for a parametric pair, otherwise one. */
        fun classify(parts: List<Expr>): PlotSpec {
            // Parametric only when t runs through both coordinates; otherwise t is an ordinary slider.
            if (parts.size == 2) {
                val bothInT = parts.all { !it.freeOf(Sym("t")) }
                return Parametric(parts[0], parts[1], params(if (bothInT) setOf("t") else emptySet(), parts[0], parts[1]))
            }
            val e = parts.single()
            return when {
                e is Rel -> Region(e, params(XY, e))
                e is Eq && e.lhs == Y && e.rhs.freeOf(Y) -> Explicit(e.rhs, params(XY, e.rhs))
                e is Eq && e.lhs == R && e.rhs.freeOf(R) && e.rhs.freeOf(X) && e.rhs.freeOf(Y) -> Polar(e.rhs, params(setOf("r", "θ"), e.rhs))
                e is Eq -> {
                    val f = sub(e.lhs, e.rhs)
                    // r and θ mixed with x and y: polar curves are written r = f(θ).
                    if ((!f.freeOf(R) || !f.freeOf(THETA)) && (!f.freeOf(X) || !f.freeOf(Y)) && !f.freeOf(THETA)) throw MathError("Write polar curves as r = f(θ)")
                    Implicit(f, params(XY, f))
                }
                !e.freeOf(Y) -> throw MathError("Add = … to make it an equation in x and y")
                !e.freeOf(THETA) && e.freeOf(X) -> Polar(e, params(setOf("r", "θ"), e))
                // Anything else is a function of x; other letters (t, a, k…) get sliders.
                else -> Explicit(e, params(XY, e))
            }
        }
    }
}

/**
 * What a line in the 3D graph's list means: z = f(x, y) (or just f(x, y)) is a
 * surface over the plane; any other equation in x, y and z (x² + y² + z² = 4)
 * is an implicit surface.
 */
sealed class PlotSpec3D {
    abstract val parameters: List<String>
    class Explicit(val f: Expr, override val parameters: List<String>) : PlotSpec3D()
    class Implicit(val f: Expr, override val parameters: List<String>) : PlotSpec3D()

    companion object {
        private val SPACE = setOf("x", "y", "z")
        private fun params(e: Expr) = (e.freeVars() - SPACE).sorted()

        fun classify(e: Expr): PlotSpec3D {
            val z = Sym("z")
            return when {
                e is Eq && e.lhs == z && e.rhs.freeOf(z) -> Explicit(e.rhs, params(e.rhs))
                e is Eq -> sub(e.lhs, e.rhs).let { Implicit(it, params(it)) }
                e is Rel -> throw MathError("Inequalities can't be drawn in 3D")
                !e.freeOf(z) -> throw MathError("Add = … to make it an equation in x, y and z")
                else -> Explicit(e, params(e))
            }
        }
    }
}

object Curves {
    /**
     * Points of a parametric curve (x(t), y(t)) for t from [t0] to [t1],
     * split where it's undefined or jumps across the whole view.
     */
    fun parametric(x: (Double) -> Double, y: (Double) -> Double, t0: Double, t1: Double, view: Viewport, samples: Int = 1200): List<List<Pair<Double, Double>>> {
        val out = ArrayList<List<Pair<Double, Double>>>()
        var cur = ArrayList<Pair<Double, Double>>()
        val jump = maxOf(view.width, view.height)
        for (k in 0..samples) {
            val t = t0 + (t1 - t0) * k / samples
            val px = x(t); val py = y(t)
            if (!px.isFinite() || !py.isFinite()) { if (cur.size > 1) out += cur; cur = ArrayList(); continue }
            val last = cur.lastOrNull()
            if (last != null && (abs(px - last.first) > jump || abs(py - last.second) > jump)) { if (cur.size > 1) out += cur; cur = ArrayList() }
            cur += px to py
        }
        if (cur.size > 1) out += cur
        return out
    }

    /** r = f(θ) for θ from 0 to [turns]·2π, as x = r cos θ, y = r sin θ. */
    fun polar(r: (Double) -> Double, view: Viewport, turns: Double = 1.0, samples: Int = 1200) =
        parametric({ t -> r(t) * cos(t) }, { t -> r(t) * sin(t) }, 0.0, 2 * PI * turns, view, samples)

    /**
     * The curve F(x, y) = 0 by marching squares on an [nx] × [ny] grid, as line
     * segments (x₁, y₁, x₂, y₂). Cells where F jumps through a pole (large
     * values of opposite sign) are skipped, so 1/x = y doesn't draw a vertical line.
     */
    fun implicit(f: (Double, Double) -> Double, view: Viewport, nx: Int, ny: Int): List<DoubleArray> {
        val xs = DoubleArray(nx + 1) { view.xMin + view.width * it / nx }
        val ys = DoubleArray(ny + 1) { view.yMin + view.height * it / ny }
        val v = Array(ny + 1) { j -> DoubleArray(nx + 1) { i -> f(xs[i], ys[j]) } }
        val out = ArrayList<DoubleArray>()
        for (j in 0 until ny) for (i in 0 until nx) {
            val a = v[j][i]; val b = v[j][i + 1]; val c = v[j + 1][i + 1]; val d = v[j + 1][i]
            if (!a.isFinite() || !b.isFinite() || !c.isFinite() || !d.isFinite()) continue
            val pts = ArrayList<Pair<Double, Double>>(4)
            fun edge(p: Double, q: Double, x1: Double, y1: Double, x2: Double, y2: Double) {
                if ((p < 0) != (q < 0)) {
                    val t = p / (p - q)
                    pts += (x1 + t * (x2 - x1)) to (y1 + t * (y2 - y1))
                }
            }
            edge(a, b, xs[i], ys[j], xs[i + 1], ys[j])
            edge(b, c, xs[i + 1], ys[j], xs[i + 1], ys[j + 1])
            edge(c, d, xs[i + 1], ys[j + 1], xs[i], ys[j + 1])
            edge(d, a, xs[i], ys[j + 1], xs[i], ys[j])
            if (pts.size < 2) continue
            // A sign change through a pole: the centre value is huge, not near zero.
            val mid = f((xs[i] + xs[i + 1]) / 2, (ys[j] + ys[j + 1]) / 2)
            val scale = maxOf(abs(a), abs(b), abs(c), abs(d))
            if (!mid.isFinite() || abs(mid) > 4 * scale) continue
            out += doubleArrayOf(pts[0].first, pts[0].second, pts[1].first, pts[1].second)
            if (pts.size == 4) out += doubleArrayOf(pts[2].first, pts[2].second, pts[3].first, pts[3].second)
        }
        return out
    }

    /** Which cells of an [nx] × [ny] grid satisfy [test] at their centre (row 0 at the top). */
    fun region(test: (Double, Double) -> Boolean, view: Viewport, nx: Int, ny: Int): BooleanArray {
        val out = BooleanArray(nx * ny)
        for (j in 0 until ny) {
            val y = view.yMax - (j + 0.5) / ny * view.height
            for (i in 0 until nx) out[j * nx + i] = test(view.xMin + (i + 0.5) / nx * view.width, y)
        }
        return out
    }

    /**
     * The same as [region], but faster: the test is first run once per [block] × [block] group
     * of cells, and cells are only tested one by one in groups next to a change (where the
     * boundary is). Away from the boundary every cell of a group takes the group's answer.
     * Features thinner than a group can be missed, which at 4 cells (16 px) doesn't show.
     */
    fun regionAdaptive(test: (Double, Double) -> Boolean, view: Viewport, nx: Int, ny: Int, block: Int = 4): BooleanArray {
        if (block <= 1) return region(test, view, nx, ny)
        val bx = (nx + block - 1) / block
        val by = (ny + block - 1) / block
        // Each group tested at the centre of its middle cell.
        val coarse = BooleanArray(bx * by)
        for (J in 0 until by) for (I in 0 until bx) {
            val i = minOf(I * block + block / 2, nx - 1); val j = minOf(J * block + block / 2, ny - 1)
            coarse[J * bx + I] = test(view.xMin + (i + 0.5) / nx * view.width, view.yMax - (j + 0.5) / ny * view.height)
        }
        val out = BooleanArray(nx * ny)
        for (J in 0 until by) for (I in 0 until bx) {
            val v = coarse[J * bx + I]
            var uniform = true
            for (dj in -1..1) for (di in -1..1) {
                val a = I + di; val b = J + dj
                if (a in 0 until bx && b in 0 until by && coarse[b * bx + a] != v) uniform = false
            }
            for (j in J * block until minOf((J + 1) * block, ny)) {
                val y = view.yMax - (j + 0.5) / ny * view.height
                for (i in I * block until minOf((I + 1) * block, nx)) {
                    out[j * nx + i] = if (uniform) v else test(view.xMin + (i + 0.5) / nx * view.width, y)
                }
            }
        }
        return out
    }

    /** Evaluates a chain of comparisons like a < b ≤ c on numbers. */
    fun holds(values: DoubleArray, ops: List<String>): Boolean {
        for (k in ops.indices) {
            val a = values[k]; val b = values[k + 1]
            if (!a.isFinite() || !b.isFinite()) return false
            val ok = when (ops[k]) { "<" -> a < b; ">" -> a > b; "≤" -> a <= b; else -> a >= b }
            if (!ok) return false
        }
        return true
    }
}
