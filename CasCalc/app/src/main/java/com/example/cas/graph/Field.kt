package com.example.cas.graph

/**
 * A scalar field f(x, y) on the 2D graph, drawn as matplotlib's imshow draws one: each cell of a
 * grid over the view coloured by the value there, through a colormap. The colour scale runs over
 * the values in view, from their 2nd to 98th percentile, so a spike (a pole) doesn't wash out the
 * rest; values beyond clip to the ends.
 */
object Field {
    /** Values on an [nx] × [ny] grid of cell centres, row 0 at the top (NaN where undefined). */
    fun sample(f: (Double, Double) -> Double, view: Viewport, nx: Int, ny: Int): DoubleArray = DoubleArray(nx * ny) { k ->
        val i = k % nx; val j = k / nx
        val x = view.xMin + (i + 0.5) / nx * view.width
        val y = view.yMax - (j + 0.5) / ny * view.height
        runCatching { f(x, y) }.getOrDefault(Double.NaN)
    }

    /** The colour scale's ends: the 2nd and 98th percentiles of the finite values (widened if flat). */
    fun range(values: DoubleArray): Pair<Double, Double> {
        val finite = values.filter { it.isFinite() }.sorted()
        if (finite.isEmpty()) return 0.0 to 1.0
        val lo = finite[(finite.size * 0.02).toInt().coerceIn(0, finite.lastIndex)]
        val hi = finite[(finite.size * 0.98).toInt().coerceIn(0, finite.lastIndex)]
        return if (hi - lo > 1e-12 * maxOf(1.0, kotlin.math.abs(hi))) lo to hi else (lo - 0.5) to (hi + 0.5)
    }

    /** Each value's colour (ARGB, [alpha] 0–255) through [map]; undefined values are transparent. */
    fun colors(values: DoubleArray, lo: Double, hi: Double, map: Colormap, reversed: Boolean, alpha: Int = 235): IntArray {
        val a = alpha.coerceIn(0, 255) shl 24
        return IntArray(values.size) { k ->
            val v = values[k]
            if (!v.isFinite()) 0 else {
                val t = ((v - lo) / (hi - lo)).coerceIn(0.0, 1.0)
                a or (map.rgb(if (reversed) 1 - t else t) and 0xFFFFFF)
            }
        }
    }
}
