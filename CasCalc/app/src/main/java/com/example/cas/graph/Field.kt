package com.example.cas.graph

/**
 * A scalar field f(x, y) on the 2D graph, drawn as matplotlib's imshow draws one: each cell of a
 * grid over the view colored by the value there, through a colormap. The color scale runs over
 * the values in view, from their 2nd to 98th percentile, so a spike (a pole) doesn't wash out the
 * rest; values beyond clip to the ends. Sampling and coloring run on every core ([Parallel]).
 */
object Field {
    /** Values on an [nx] × [ny] grid of cell centers, row 0 at the top (NaN where undefined); [f] must be safe to call from several threads. */
    fun sample(f: (Double, Double) -> Double, view: Viewport, nx: Int, ny: Int): DoubleArray = sample({ f }, view, nx, ny)

    /** The same, with [make] giving each thread its own caller (one with its own argument array). */
    fun sample(make: () -> (Double, Double) -> Double, view: Viewport, nx: Int, ny: Int): DoubleArray {
        val out = DoubleArray(nx * ny)
        Parallel.rows(ny, make) { f, j0, j1 ->
            for (j in j0 until j1) {
                val y = view.yMax - (j + 0.5) / ny * view.height
                val row = j * nx
                for (i in 0 until nx) {
                    val x = view.xMin + (i + 0.5) / nx * view.width
                    out[row + i] = try { f(x, y) } catch (e: RuntimeException) { Double.NaN }
                }
            }
        }
        return out
    }

    /** The color scale's ends: the 2nd and 98th percentiles of the finite values (widened if flat). */
    fun range(values: DoubleArray): Pair<Double, Double> {
        // Percentiles of an evenly spread subset (at most 50 000 values), sorted as plain doubles.
        var count = 0
        for (v in values) if (v.isFinite()) count++
        if (count == 0) return 0.0 to 1.0
        val step = maxOf(1, count / 50_000)
        val finite = DoubleArray((count + step - 1) / step)
        var seen = 0; var k = 0
        for (v in values) if (v.isFinite()) { if (seen % step == 0 && k < finite.size) finite[k++] = v; seen++ }
        java.util.Arrays.sort(finite, 0, k)
        val lo = finite[(k * 0.02).toInt().coerceIn(0, k - 1)]
        val hi = finite[(k * 0.98).toInt().coerceIn(0, k - 1)]
        return if (hi - lo > 1e-12 * maxOf(1.0, kotlin.math.abs(hi))) lo to hi else (lo - 0.5) to (hi + 0.5)
    }

    /** Each value's color (ARGB, [alpha] 0–255) through [map]; undefined values are transparent. */
    fun colors(values: DoubleArray, lo: Double, hi: Double, map: Colormap, reversed: Boolean, alpha: Int = 235): IntArray {
        val a = alpha.coerceIn(0, 255) shl 24
        // The map looked up once at 1024 steps (finer than the eye tells apart), not per cell.
        val lut = IntArray(LUT) { k -> val t = k / (LUT - 1.0); a or (map.rgb(if (reversed) 1 - t else t) and 0xFFFFFF) }
        val out = IntArray(values.size)
        val scale = (LUT - 1) / (hi - lo)
        Parallel.rows(values.size, { }) { _, from, until ->
            for (k in from until until) {
                val v = values[k]
                out[k] = if (!v.isFinite()) 0 else lut[((v - lo) * scale).coerceIn(0.0, LUT - 1.0).toInt()]
            }
        }
        return out
    }

    private const val LUT = 1024
}
