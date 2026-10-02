package com.example.cas.graph

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Linear or logarithmic axes, each separately (the 2D graph and the complex plane).
 *
 * On a log axis the view holds log₁₀ of the values ("scaled" coordinates), so panning, zooming
 * and the mapping to the screen stay linear: 0 is 1, 1 is 10, −2 is 0.01. Lines are sampled by
 * turning a scaled coordinate back into a value ([realX]) and the result into a scaled one ([x]).
 * Values at or below 0 have no place on a log axis and come out as NaN, which breaks a line.
 */
data class AxisScale(val logX: Boolean = false, val logY: Boolean = false) {
    val linear get() = !logX && !logY

    /** A value of x as a scaled coordinate. */
    fun x(value: Double) = if (logX) logOf(value) else value
    fun y(value: Double) = if (logY) logOf(value) else value

    /** A scaled coordinate as the value it stands for. */
    fun realX(u: Double) = if (logX) 10.0.pow(u) else u
    fun realY(v: Double) = if (logY) 10.0.pow(v) else v

    /** The view's ranges as values (for samplers that work in values, like polar curves). */
    fun realView(v: Viewport) = if (linear) v else Viewport(realX(v.xMin), realX(v.xMax), realY(v.yMin), realY(v.yMax))

    /** Polylines in values as polylines in scaled coordinates, broken where a point has no place. */
    fun paths(lines: List<List<Pair<Double, Double>>>): List<List<Pair<Double, Double>>> {
        if (linear) return lines
        val out = ArrayList<List<Pair<Double, Double>>>()
        for (line in lines) {
            var run = ArrayList<Pair<Double, Double>>()
            for ((a, b) in line) {
                val p = x(a) to y(b)
                if (p.first.isFinite() && p.second.isFinite()) run.add(p)
                else { if (run.size > 1) out += run; run = ArrayList() }
            }
            if (run.size > 1) out += run
        }
        return out
    }

    /** A function of x as one from scaled x to scaled y. */
    fun function(f: (Double) -> Double): (Double) -> Double = if (linear) f else { u -> y(f(realX(u))) }

    /** A function of two values as one of two scaled coordinates. */
    fun function2(f: (Double, Double) -> Double): (Double, Double) -> Double = if (linear) f else { u, v -> f(realX(u), realY(v)) }

    companion object {
        private fun logOf(value: Double) = if (value > 0) log10(value) else Double.NaN

        /**
         * The same part of the plane in the other scale: a log axis shows the positive part of a
         * linear one (down to a thousandth of its top if it reaches 0), and a linear axis the
         * values a log one covered.
         */
        fun convert(v: Viewport, from: AxisScale, to: AxisScale): Viewport {
            fun range(lo: Double, hi: Double, wasLog: Boolean, isLog: Boolean): Pair<Double, Double> = when {
                wasLog == isLog -> lo to hi
                isLog -> if (hi <= 0) -1.0 to 2.0 else {
                    val bottom = if (lo > 0) lo else hi / 1000
                    log10(bottom) to log10(hi)
                }
                else -> 10.0.pow(lo) to 10.0.pow(hi)
            }
            val (x0, x1) = range(v.xMin, v.xMax, from.logX, to.logX)
            val (y0, y1) = range(v.yMin, v.yMax, from.logY, to.logY)
            return Viewport(x0, x1, y0, y1)
        }

        /** The view to start from (double-tap): the linear default, with 0.01 to 1000 on a log axis. */
        fun standard(linearView: Viewport, scale: AxisScale): Viewport {
            if (scale.linear) return linearView
            val aspect = linearView.height / linearView.width
            val (x0, x1) = if (scale.logX) -2.0 to 3.0 else linearView.xMin to linearView.xMax
            val (y0, y1) = when {
                !scale.logY -> linearView.yMin to linearView.yMax
                // Both log: decades the same size on both axes.
                scale.logX -> (0.5 - 2.5 * aspect) to (0.5 + 2.5 * aspect)
                else -> -2.0 to 3.0
            }
            return Viewport(x0, x1, y0, y1)
        }

        /** A decade's label: 0.01 … 1000 written out, beyond that 10 to the power, as (base, exponent). */
        fun decade(k: Int): Pair<String, String?> = when {
            k in 0..3 -> 10.0.pow(k).roundToInt().toString() to null
            k in -3..-1 -> ("0." + "0".repeat(-k - 1) + "1") to null
            else -> "10" to k.toString().replace("-", "−")
        }

        /** A value like 2, 50 or 0.03 between decades, written out (or m × 10ᵏ far from 1). */
        fun mantissaLabel(m: Int, k: Int): Pair<String, String?> = when {
            k in -3..3 -> java.math.BigDecimal(m).scaleByPowerOfTen(k).stripTrailingZeros().toPlainString() to null
            else -> "$m×10" to k.toString().replace("-", "−")
        }
    }

    /** One axis's ticks: major lines with their labels (text and an optional exponent), and minor lines. */
    class Ticks(val major: List<Double>, val labels: List<Pair<String, String?>>, val minor: List<Double>)

    /**
     * Ticks for an axis from [min] to [max] (scaled), about [target] labeled. Linear: 1, 2 or 5 ×
     * 10ⁿ steps. Log: whole decades (every few when there are many), the 2–9 between them as
     * minor lines; zoomed in to less than about a decade, 1, 2 and 5 of each are labeled; closer
     * still, the values themselves in linear steps.
     */
    fun ticks(min: Double, max: Double, target: Int, log: Boolean): Ticks {
        if (!log || !(max > min)) {
            val step = Plot2D.niceStep(max - min, target)
            val major = Plot2D.ticks(min, max, target)
            return Ticks(major, major.map { Plot2D.label(it, step) to null }, Plot2D.ticks(min, max, target * 5))
        }
        val span = max - min
        if (span > 1.5) {
            // Whole decades, every [step] of them (decade labels are short: more of them fit).
            val step = maxOf(1, ceil(span / (target * 1.6)).toInt())
            val first = ceil(min / step).toInt() * step
            val major = generateSequence(first) { it + step }.takeWhile { it <= max + 1e-9 }.toList()
            val minor = ArrayList<Double>()
            if (step == 1) {
                // 2 to 9 of each decade, while there's room for them.
                if (span <= target * 1.6) for (k in floor(min).toInt()..ceil(max).toInt()) for (m in 2..9) {
                    val u = k + log10(m.toDouble())
                    if (u in min..max) minor += u
                }
            } else for (k in ceil(min).toInt()..floor(max).toInt()) if (k % step != 0) minor += k.toDouble()
            return Ticks(major.map { it.toDouble() }, major.map { decade(it) }, minor)
        }
        if (span > 0.3) {
            // Within a decade or so: 1, 2 and 5 labeled (every digit when it's tighter), each digit a line.
            val labeled = if (span > 0.8) setOf(1, 2, 5) else (1..9).toSet()
            val major = ArrayList<Double>(); val labels = ArrayList<Pair<String, String?>>(); val minor = ArrayList<Double>()
            for (k in floor(min).toInt()..ceil(max).toInt()) for (m in 1..9) {
                val u = k + log10(m.toDouble())
                if (u < min || u > max) continue
                if (m in labeled) { major += u; labels += if (m == 1) decade(k) else mantissaLabel(m, k) } else minor += u
            }
            return Ticks(major, labels, minor)
        }
        // Closer still: the values in linear steps, placed on the log axis.
        val lo = 10.0.pow(min); val hi = 10.0.pow(max)
        val step = Plot2D.niceStep(hi - lo, target)
        val values = Plot2D.ticks(lo, hi, target).filter { it > 0 }
        val minor = Plot2D.ticks(lo, hi, target * 5).filter { it > 0 }.map { log10(it) }
        return Ticks(values.map { log10(it) }, values.map { Plot2D.label(it, step) to null }, minor)
    }

    /** A value read off the graph, tidied for a slider dragged on a log axis: three significant figures. */
    fun tidy(value: Double): Double {
        if (value == 0.0 || !value.isFinite()) return value
        val mag = 10.0.pow(floor(log10(abs(value))) - 2)
        return Math.round(value / mag) * mag
    }
}
