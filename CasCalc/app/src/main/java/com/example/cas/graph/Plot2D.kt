package com.example.cas.graph

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** The visible part of the plane. */
data class Viewport(val xMin: Double, val xMax: Double, val yMin: Double, val yMax: Double) {
    val width get() = xMax - xMin
    val height get() = yMax - yMin

    /** Moves the view by a fraction of its size (drag). */
    fun panBy(dxFraction: Double, dyFraction: Double) =
        Viewport(xMin - dxFraction * width, xMax - dxFraction * width, yMin + dyFraction * height, yMax + dyFraction * height)

    /**
     * Stretches or squeezes each axis separately around a point (fractions across the view):
     * a sideways pinch changes only x, an up-and-down pinch only y, a diagonal one both.
     */
    fun zoomAxes(factorX: Double, factorY: Double, fx: Double = 0.5, fy: Double = 0.5): Viewport {
        val cx = xMin + fx * width
        val cy = yMax - fy * height
        val w = (width / factorX).coerceIn(1e-9, 1e9)
        val h = (height / factorY).coerceIn(1e-9, 1e9)
        return Viewport(cx - fx * w, cx + (1 - fx) * w, cy - (1 - fy) * h, cy + fy * h)
    }

    /** Zooms around a point given as fractions across the view (0..1, y from the top). */
    fun zoomBy(factor: Double, fx: Double = 0.5, fy: Double = 0.5): Viewport {
        val cx = xMin + fx * width
        val cy = yMax - fy * height
        val w = (width / factor).coerceIn(1e-9, 1e9)
        val h = (height / factor).coerceIn(1e-9, 1e9)
        return Viewport(cx - fx * w, cx + (1 - fx) * w, cy - (1 - fy) * h, cy + fy * h)
    }

    companion object {
        /** A square-ish view centered on the origin for a screen of the given aspect ratio (height / width). */
        fun standard(aspect: Double, halfWidth: Double = 10.0) = Viewport(-halfWidth, halfWidth, -halfWidth * aspect, halfWidth * aspect)
    }
}

object Plot2D {
    /** 1, 2 or 5 × 10ⁿ, so that roughly [target] steps fit in [span]. */
    fun niceStep(span: Double, target: Int = 6): Double {
        val raw = span / target
        val mag = 10.0.pow(floor(log10(raw)))
        val norm = raw / mag
        return mag * when {
            norm < 1.5 -> 1.0
            norm < 3.5 -> 2.0
            norm < 7.5 -> 5.0
            else -> 10.0
        }
    }

    fun ticks(min: Double, max: Double, target: Int = 6): List<Double> {
        val step = niceStep(max - min, target)
        val first = ceil(min / step) * step
        return generateSequence(first) { it + step }.takeWhile { it <= max + step * 1e-9 }
            .map { if (abs(it) < step * 1e-9) 0.0 else it }.toList()
    }

    /** Tick label without floating-point noise: 0.30000000000000004 → "0.3". */
    fun label(v: Double, step: Double): String {
        val decimals = maxOf(0, -floor(log10(step)).toInt())
        val s = String.format(java.util.Locale.US, "%.${decimals}f", v)
        return s.replace("-", "−").let { if (it == "−0") "0" else it }
    }

    /**
     * Samples y = f(x) across the view into polylines. Lines break where the
     * function is undefined and at jumps (like tan x at π/2) instead of
     * drawing a vertical line through the asymptote.
     */
    fun sample(f: (Double) -> Double, view: Viewport, samples: Int = 600): List<List<Pair<Double, Double>>> {
        val out = ArrayList<List<Pair<Double, Double>>>()
        var current = ArrayList<Pair<Double, Double>>()
        var prev: Pair<Double, Double>? = null
        val limit = view.height * 4
        for (i in 0..samples) {
            val x = view.xMin + view.width * i / samples
            val y = f(x).let { if (it.isFinite()) it else removable(f, x, view.width) }
            if (!y.isFinite()) {
                if (current.size > 1) out += current
                current = ArrayList(); prev = null
                continue
            }
            val p = prev
            if (p != null && abs(y - p.second) > view.height && (abs(y - view.yMid) > limit / 2 || abs(p.second - view.yMid) > limit / 2 || jumpsAcross(f, p.first, x))) {
                if (current.size > 1) out += current
                current = ArrayList()
            }
            current += x to y.coerceIn(view.yMin - limit, view.yMax + limit)
            prev = x to y
        }
        if (current.size > 1) out += current
        return out
    }

    private val Viewport.yMid get() = (yMin + yMax) / 2

    /**
     * The value a curve has a hair either side of [x] where it is undefined at [x] itself, when
     * the two sides agree: a removable gap, like sin(ω/2)/(ω/2) at 0, is filled; a pole or a jump
     * stays a gap (NaN).
     */
    fun removable(f: (Double) -> Double, x: Double, span: Double): Double {
        val h = maxOf(span * 1e-7, kotlin.math.abs(x) * 1e-12)
        val a = f(x - h); val b = f(x + h)
        if (!a.isFinite() || !b.isFinite()) return Double.NaN
        return if (abs(a - b) <= 1e-5 * maxOf(1.0, abs(a), abs(b))) (a + b) / 2 else Double.NaN
    }

    /** A steep section is a real jump if the midpoint isn't between its ends. */
    private fun jumpsAcross(f: (Double) -> Double, a: Double, b: Double): Boolean {
        val fa = f(a); val fb = f(b); val fm = f((a + b) / 2)
        if (!fm.isFinite()) return true
        return fm < minOf(fa, fb) - abs(fb - fa) || fm > maxOf(fa, fb) + abs(fb - fa)
    }

    /**
     * Points where the curve meets y = 0: crossings found by bisection (not poles), and touching
     * zeros (x² at 0). Values within rounding noise of 0 ([noiseFloor]) count as 0, so a curve that
     * is 0 up to rounding (δ's surroundings, a cancelled difference) wobbling across the axis isn't
     * a row of zeros: a near-zero stretch gives at most one point, and none when it's flat noise
     * (its raw values change sign more than once). Points closer than a sample apart are merged.
     */
    fun zeros(f: (Double) -> Double, xMin: Double, xMax: Double, samples: Int = 800): List<Double> {
        val h = (xMax - xMin) / samples
        val xs = DoubleArray(samples + 1) { xMin + h * it }
        val ys = DoubleArray(samples + 1) { f(xs[it]) }
        val eps = noiseFloor(ys)
        fun sign(y: Double) = if (abs(y) <= eps) 0 else if (y > 0) 1 else -1
        val out = ArrayList<Double>()
        fun bisect(lo: Double, hi: Double, flo: Double): Double {
            var a = lo; var b = hi; var fa = flo
            repeat(60) {
                val m = (a + b) / 2
                val fm = f(m)
                if (fa * fm <= 0) b = m else { a = m; fa = fm }
            }
            return (a + b) / 2
        }
        var i = 0
        while (i <= samples) {
            if (!ys[i].isFinite()) { i++; continue }
            if (sign(ys[i]) != 0) {
                // A sign change to the next sample (both clearly away from 0): bisect, unless it's a pole.
                val j = i + 1
                if (j <= samples && ys[j].isFinite() && sign(ys[j]) != 0 && sign(ys[j]) != sign(ys[i])) {
                    val r = bisect(xs[i], xs[j], ys[i])
                    if (abs(f(r)) < 1e-6 * maxOf(1.0, abs(ys[i]), abs(ys[j]))) out += r
                }
                i++; continue
            }
            // A stretch of samples at 0 (up to rounding), and the signs on either side of it.
            var j = i
            while (j + 1 <= samples && ys[j + 1].isFinite() && sign(ys[j + 1]) == 0) j++
            val before = if (i > 0 && ys[i - 1].isFinite()) sign(ys[i - 1]) else 0
            val after = if (j < samples && ys[j + 1].isFinite()) sign(ys[j + 1]) else 0
            var flips = 0; var last = 0; var zeroes = 0
            for (k in i..j) {
                val s = if (ys[k] > 0) 1 else if (ys[k] < 0) -1 else 0
                if (s == 0) zeroes++ else { if (last != 0 && s != last) flips++; last = s }
            }
            val flat = j - i >= 2 && (flips >= 2 || zeroes >= 2)
            if (!flat) {
                val lo = if (i > 0 && before != 0) i - 1 else i
                val hi = if (j < samples && after != 0) j + 1 else j
                out += if (before != 0 && after != 0 && before != after || flips == 1) {
                    // Crosses: bisect across the stretch (on the raw values).
                    if (ys[lo] == 0.0) xs[lo] else if (ys[hi] == 0.0) xs[hi]
                    else if (ys[lo] * ys[hi] < 0) bisect(xs[lo], xs[hi], ys[lo])
                    else xs[(i..j).minBy { abs(ys[it]) }]
                } else {
                    // Touches: the smallest |f| in the stretch.
                    val k = (i..j).minBy { abs(ys[it]) }
                    if (ys[k] == 0.0) xs[k] else refine({ abs(f(it)) }, xs[maxOf(0, k - 1)], xs[minOf(samples, k + 1)], Kind.Minimum)
                }
            }
            i = j + 1
        }
        // One point per zero: rounding can split one into neighbours a hair apart.
        val merged = ArrayList<Double>()
        for (x in out.sorted()) if (merged.isEmpty() || x - merged.last() > 1.5 * abs(h)) merged += x
        return merged
    }

    /**
     * How close to 0 a value must be to count as 0 for a curve with sampled values [ys]: rounding
     * noise, a billionth of the curve's typical size (its 99th-percentile |y|, so a pole doesn't
     * set it).
     */
    fun noiseFloor(ys: DoubleArray): Double {
        val a = ys.filter { it.isFinite() }.map { abs(it) }.sorted()
        if (a.isEmpty()) return 0.0
        return 1e-9 * a[((a.size - 1) * 0.99).toInt()]
    }

    /**
     * Between x = [a] and x = [b]: the signed integral ∫ f dx and the total area
     * between the curve and the x-axis, ∫ |f| dx (split at the zeros so each piece is smooth).
     */
    fun area(f: (Double) -> Double, a: Double, b: Double): Pair<Double, Double> {
        val lo = minOf(a, b); val hi = maxOf(a, b)
        val sign = if (a <= b) 1.0 else -1.0
        val signed = com.example.cas.cas.Numerics.integrate(f, lo, hi)
        val cuts = listOf(lo) + zeros(f, lo, hi, 400).filter { it > lo && it < hi } + listOf(hi)
        val total = cuts.zipWithNext().sumOf { (p, q) -> abs(com.example.cas.cas.Numerics.integrate(f, p, q)) }
        return sign * signed to total
    }

    enum class Kind { Maximum, Minimum }

    /** Local maxima and minima, where the slope changes sign, refined by golden-section search. */
    fun extrema(f: (Double) -> Double, xMin: Double, xMax: Double, samples: Int = 800): List<Pair<Double, Kind>> {
        val h = (xMax - xMin) / samples
        val ys = DoubleArray(samples + 1) { f(xMin + h * it) }
        val eps = noiseFloor(ys)
        val out = ArrayList<Pair<Double, Kind>>()
        for (i in 1 until samples) {
            val a = ys[i - 1]; val b = ys[i]; val c = ys[i + 1]
            if (!a.isFinite() || !b.isFinite() || !c.isFinite()) continue
            val kind = when {
                b > a && b >= c -> Kind.Maximum
                b < a && b <= c -> Kind.Minimum
                else -> continue
            }
            // Ignore flat noise (rounding wobbles where the curve is level) and cusps of jumps.
            if (abs(b - a) <= eps && abs(b - c) <= eps) continue
            if (abs(b - a) > (xMax - xMin) * 1e3) continue
            out += refine(f, xMin + h * (i - 1), xMin + h * (i + 1), kind) to kind
        }
        return out
    }

    private fun refine(f: (Double) -> Double, lo: Double, hi: Double, kind: Kind): Double {
        val g = 0.6180339887498949
        var a = lo; var b = hi
        val sign = if (kind == Kind.Maximum) -1.0 else 1.0
        repeat(80) {
            val c = b - g * (b - a)
            val d = a + g * (b - a)
            if (sign * f(c) < sign * f(d)) b = d else a = c
        }
        return (a + b) / 2
    }
    /** f′(x) by central differences. */
    fun derivative(f: (Double) -> Double, x: Double): Double {
        val h = 1e-5 * maxOf(1.0, abs(x))
        return (f(x + h) - f(x - h)) / (2 * h)
    }

    /** The length of y = f(x) from [a] to [b], ∫ √(1 + f′²) dx by Simpson's rule (NaN where f isn't defined). */
    fun arcLength(f: (Double) -> Double, a: Double, b: Double, n: Int = 2000): Double {
        if (a == b) return 0.0
        val lo = minOf(a, b); val hi = maxOf(a, b)
        val m = if (n % 2 == 0) n else n + 1
        val h = (hi - lo) / m
        fun g(x: Double) = kotlin.math.sqrt(1 + derivative(f, x).let { it * it })
        var sum = g(lo) + g(hi)
        for (k in 1 until m) sum += (if (k % 2 == 1) 4 else 2) * g(lo + k * h)
        return sum * h / 3
    }

}

/**
 * How a two-finger gesture scales each axis: the change in the fingers' horizontal
 * spread scales x, the change in their vertical spread scales y. A spread under
 * [minSpread] pixels in a direction (fingers side by side, or one above the other)
 * leaves that axis alone, so a sideways pinch doesn't wobble y.
 */
object AxisPinch {
    fun factors(beforeDx: Float, beforeDy: Float, afterDx: Float, afterDy: Float, minSpread: Float): Pair<Double, Double> {
        val bx = kotlin.math.abs(beforeDx); val by = kotlin.math.abs(beforeDy)
        val ax = kotlin.math.abs(afterDx); val ay = kotlin.math.abs(afterDy)
        val zx = if (bx > minSpread && ax > minSpread) (ax / bx).toDouble() else 1.0
        val zy = if (by > minSpread && ay > minSpread) (ay / by).toDouble() else 1.0
        return zx.coerceIn(0.5, 2.0) to zy.coerceIn(0.5, 2.0)
    }
}
