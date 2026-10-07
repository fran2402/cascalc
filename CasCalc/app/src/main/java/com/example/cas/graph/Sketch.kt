package com.example.cas.graph

import com.example.cas.cas.Eq
import com.example.cas.cas.Expr
import com.example.cas.cas.Mat
import com.example.cas.cas.Rel
import com.example.cas.cas.Seq
import com.example.cas.cas.Sym
import com.example.cas.cas.contains
import com.example.cas.cas.freeVars
import kotlin.math.abs

/**
 * The little graph beside an answer that is a function of one letter: no axes or labels, just
 * the curve over a stretch chosen to show what it does (its zeros, peaks and dips, or where it is
 * defined), scaled to fill the box.
 */
object Sketch {
    /**
     * The curve as points across a unit box: [xs] from 0 to 1, [ys] from 0 (bottom) to 1 (top),
     * NaN where it breaks; [base] is where y = 0 is (clamped into the box), for the shading.
     */
    class Curve(val xs: DoubleArray, val ys: DoubleArray, val base: Double)

    private const val POINTS = 160

    fun of(e: Expr): Curve? {
        if (e is Eq || e is Seq || e is Rel || e is Mat) return null
        val v = e.freeVars().singleOrNull() ?: return null
        if (e.contains { it is Sym && it.name == "i" }) return null
        val compiled = runCatching { Compiler.compile(e, listOf(v)) }.getOrNull() ?: return null
        val arg = DoubleArray(1)
        val f = { x: Double -> arg[0] = x; runCatching { compiled(arg) }.getOrDefault(Double.NaN) }
        val (a, b) = range(f) ?: return null
        val xs = DoubleArray(POINTS + 1) { it / POINTS.toDouble() }
        val raw = DoubleArray(POINTS + 1) { f(a + (b - a) * xs[it]) }
        val finite = raw.filter { it.isFinite() }.sorted()
        if (finite.size < POINTS / 4) return null
        var lo = finite[(finite.size * 0.02).toInt()]
        var hi = finite[(finite.size * 0.98).toInt().coerceAtMost(finite.size - 1)]
        if (hi - lo < 1e-9 * maxOf(1.0, abs(hi))) return null
        val pad = (hi - lo) * 0.14
        lo -= pad; hi += pad
        val ys = DoubleArray(raw.size) { k ->
            val y = (raw[k] - lo) / (hi - lo)
            if (!y.isFinite() || y < -2 || y > 3) Double.NaN else y
        }
        // A jump (a pole) breaks the line rather than joining across it.
        for (k in 1 until ys.size) if (abs(ys[k] - ys[k - 1]) > 1.5) { ys[k] = Double.NaN }
        return Curve(xs, ys, ((0.0 - lo) / (hi - lo)).coerceIn(0.0, 1.0))
    }

    /** The stretch of x to draw: around the zeros and turning points near 0, else where it is defined. */
    private fun range(f: (Double) -> Double): Pair<Double, Double>? {
        val n = 400
        val xs = DoubleArray(n + 1) { -10.0 + 20.0 * it / n }
        val ok = xs.filter { f(it).isFinite() }
        if (ok.size < n / 20) return null
        // Where it's defined (ln x: x > 0), kept within ±5 when there's nothing to show.
        var dLo = ok.first(); var dHi = ok.last()
        val features = (Plot2D.zeros(f, dLo, dHi, n) + Plot2D.extrema(f, dLo, dHi, n).map { it.first })
            .sortedBy { abs(it) }.take(5)
        if (features.isEmpty()) {
            dLo = maxOf(dLo, -5.0); dHi = minOf(dHi, 5.0)
            if (dHi - dLo < 0.5) { dLo = ok.first(); dHi = ok.last() }
            // Start a hair inside a domain edge (ln x at 0 is −∞).
            val inset = (dHi - dLo) * 0.004
            return (dLo + if (dLo > -5.0) inset else 0.0) to dHi
        }
        val lo = features.min(); val hi = features.max()
        val span = maxOf(hi - lo, 2.0)
        val mid = (lo + hi) / 2
        val a = maxOf(mid - span * 0.95, ok.first()); val b = minOf(mid + span * 0.95, ok.last())
        return if (b - a > 1e-6) a to b else null
    }
}
