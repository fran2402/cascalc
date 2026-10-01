package com.example.cas.graph

import kotlin.math.hypot

/**
 * A vector field F(x, y) = (P, Q) drawn as arrows on a grid: each arrow centered on its grid
 * point, its length set by [Length], its head by [Tip]; colored with the line's color or by
 * |F| along a colormap.
 */
object VectorField {
    /** How long the arrows are drawn. */
    enum class Length(val label: String) {
        /** In proportion to |F|, the longest filling its grid cell. */
        Scaled("Scaled"),
        /** All the same length: only the direction shows (|F| can still show as color). */
        Equal("Same length"),
        /** Exactly F in the graph's units, times the scale. */
        True("True length"),
    }

    /** The arrowhead. */
    enum class Tip(val label: String) {
        Triangle("Triangle"),
        Stealth("Stealth"),
        Open("Open"),
        Dot("Dot"),
        None("None"),
    }

    /** One arrow, from ([x0], [y0]) to ([x1], [y1]) in the graph's units, and |F| there. */
    class Arrow(val x0: Double, val y0: Double, val x1: Double, val y1: Double, val magnitude: Double)

    /** The arrows over [view] drawn [widthPx] × [heightPx], [columns] across (square cells on screen). */
    fun arrows(
        f: (Double, Double) -> Pair<Double, Double>?,
        view: Viewport, widthPx: Double, heightPx: Double,
        columns: Int, length: Length, scale: Double,
    ): List<Arrow> {
        if (widthPx <= 0 || heightPx <= 0) return emptyList()
        val cell = widthPx / columns
        val rows = (heightPx / cell).toInt().coerceAtLeast(1)
        val kx = widthPx / view.width; val ky = heightPx / view.height
        val cx = view.width / columns; val cy = view.height / rows
        // Values first, so the scale can follow the largest.
        class V(val x: Double, val y: Double, val p: Double, val q: Double, val m: Double)
        val vs = ArrayList<V>(columns * rows)
        for (j in 0 until rows) for (i in 0 until columns) {
            val x = view.xMin + (i + 0.5) * cx; val y = view.yMin + (j + 0.5) * cy
            val (p, q) = f(x, y) ?: continue
            if (!p.isFinite() || !q.isFinite()) continue
            vs += V(x, y, p, q, hypot(p, q))
        }
        val largest = robustMax(vs.map { it.m })
        val full = 0.86 * cell * scale
        val out = ArrayList<Arrow>(vs.size)
        for (v in vs) {
            if (v.m == 0.0) { out += Arrow(v.x, v.y, v.x, v.y, 0.0); continue }
            val (dx, dy) = when (length) {
                Length.True -> v.p * scale to v.q * scale
                else -> {
                    // On screen the direction is (P·kx, Q·ky); its length in pixels is picked, then mapped back.
                    val sx = v.p * kx; val sy = v.q * ky
                    val s = hypot(sx, sy)
                    val px = if (length == Length.Equal) full else full * (v.m / largest).coerceAtMost(1.0)
                    (sx / s * px / kx) to (sy / s * px / ky)
                }
            }
            out += Arrow(v.x - dx / 2, v.y - dy / 2, v.x + dx / 2, v.y + dy / 2, v.m)
        }
        return out
    }

    /**
     * The largest |F| that sets the scale: the largest, unless a few values near a singularity
     * (1/r²) dwarf the rest, when it's the 95th percentile and those few are capped.
     */
    fun robustMax(ms: List<Double>): Double {
        if (ms.isEmpty()) return 1.0
        val sorted = ms.sorted()
        val top = sorted.last()
        val p95 = sorted[((sorted.size - 1) * 0.95).toInt()]
        val m = if (p95 > 0 && top > 4 * p95) p95 else top
        return if (m > 0) m else 1.0
    }

    /** |F|'s range over [arrows] for coloring, with the same cap as the lengths. */
    fun range(arrows: List<Arrow>): Pair<Double, Double> {
        if (arrows.isEmpty()) return 0.0 to 1.0
        val lo = arrows.minOf { it.magnitude }
        val hi = robustMax(arrows.map { it.magnitude })
        return lo to if (hi > lo) hi else lo + 1
    }

    /**
     * An arrowhead in screen coordinates, at ([hx], [hy]) pointing along ([ux], [uy]) (a unit
     * vector), [size] long: filled polygons (x, y, x, y…), strokes, and where the shaft should
     * stop so it doesn't poke through the point.
     */
    class Head(val fills: List<DoubleArray>, val strokes: List<DoubleArray>, val shaftEndX: Double, val shaftEndY: Double, val dot: DoubleArray? = null)

    fun head(tip: Tip, hx: Double, hy: Double, ux: Double, uy: Double, size: Double): Head {
        val bx = hx - ux * size; val by = hy - uy * size
        // Perpendicular.
        val nx = -uy; val ny = ux
        return when (tip) {
            Tip.Triangle -> Head(
                listOf(doubleArrayOf(hx, hy, bx + nx * size * 0.45, by + ny * size * 0.45, bx - nx * size * 0.45, by - ny * size * 0.45)),
                emptyList(), hx - ux * size * 0.7, hy - uy * size * 0.7,
            )
            Tip.Stealth -> {
                // A swept-back head with a notch, as TikZ's Stealth.
                val notchX = hx - ux * size * 0.62; val notchY = hy - uy * size * 0.62
                Head(
                    listOf(doubleArrayOf(hx, hy, bx + nx * size * 0.5, by + ny * size * 0.5, notchX, notchY, bx - nx * size * 0.5, by - ny * size * 0.5)),
                    emptyList(), notchX + ux * size * 0.1, notchY + uy * size * 0.1,
                )
            }
            Tip.Open -> Head(
                emptyList(),
                listOf(
                    doubleArrayOf(bx + nx * size * 0.5, by + ny * size * 0.5, hx, hy),
                    doubleArrayOf(bx - nx * size * 0.5, by - ny * size * 0.5, hx, hy),
                ),
                hx, hy,
            )
            // A dot at the tail would hide the direction: at the head.
            Tip.Dot -> Head(emptyList(), emptyList(), hx, hy, dot = doubleArrayOf(hx, hy, size * 0.32))
            Tip.None -> Head(emptyList(), emptyList(), hx, hy)
        }
    }

    /** Where in the colormap |F| = [m] falls, 0 to 1. */
    fun position(m: Double, range: Pair<Double, Double>): Double =
        ((m - range.first) / (range.second - range.first)).let { if (it.isFinite()) it.coerceIn(0.0, 1.0) else 0.0 }
}
