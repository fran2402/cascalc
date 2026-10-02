package com.example.cas.graph

import kotlin.math.abs
import kotlin.math.hypot

/**
 * A slope field for dy/dx = f(x, y): a short line segment of slope f at each point of a grid,
 * the same length on screen whatever the axes' scales; and solution curves through a point, by
 * fourth-order Runge–Kutta forwards and backwards until they leave the view.
 */
object SlopeField {
    /** Segments (x₀, y₀, x₁, y₁) on a grid about [spacing] pixels apart, in the view's coordinates. */
    fun segments(slope: (Double, Double) -> Double, view: Viewport, widthPx: Double, heightPx: Double, spacing: Double = 30.0): List<DoubleArray> {
        val nx = (widthPx / spacing).toInt().coerceIn(4, 80)
        val ny = (heightPx / spacing).toInt().coerceIn(4, 120)
        val ppx = widthPx / view.width; val ppy = heightPx / view.height
        val half = spacing * 0.36
        val out = ArrayList<DoubleArray>(nx * ny)
        for (j in 0 until ny) for (i in 0 until nx) {
            val x = view.xMin + (i + 0.5) / nx * view.width
            val y = view.yMin + (j + 0.5) / ny * view.height
            val s = slope(x, y)
            if (!s.isFinite()) continue
            // The direction (1, s) on screen, made [half] pixels long either side.
            val dxPx = ppx; val dyPx = s * ppy
            val len = hypot(dxPx, dyPx)
            if (len == 0.0) continue
            val dx = dxPx / len * half / ppx; val dy = dyPx / len * half / ppy
            out += doubleArrayOf(x - dx, y - dy, x + dx, y + dy)
        }
        return out
    }

    /** The solution through (x₀, y₀), as one polyline from left to right (broken where it runs off or blows up). */
    fun solution(slope: (Double, Double) -> Double, x0: Double, y0: Double, view: Viewport, steps: Int = 1500): List<List<Pair<Double, Double>>> {
        val h = view.width / 500
        val yLo = view.yMin - 2 * view.height; val yHi = view.yMax + 2 * view.height
        fun run(dir: Double): List<Pair<Double, Double>> {
            val pts = ArrayList<Pair<Double, Double>>()
            var x = x0; var y = y0
            pts += x to y
            for (k in 0 until steps) {
                val step = dir * h
                val k1 = slope(x, y)
                val k2 = slope(x + step / 2, y + step / 2 * k1)
                val k3 = slope(x + step / 2, y + step / 2 * k2)
                val k4 = slope(x + step, y + step * k3)
                val ny = y + step / 6 * (k1 + 2 * k2 + 2 * k3 + k4)
                x += step
                if (!ny.isFinite() || abs(k1) > 1e8) break
                y = ny
                pts += x to y
                if (x < view.xMin - view.width * 0.05 || x > view.xMax + view.width * 0.05 || y < yLo || y > yHi) break
            }
            return pts
        }
        val back = run(-1.0).asReversed()
        val forward = run(1.0)
        val line = back + forward.drop(1)
        return if (line.size > 1) listOf(line) else emptyList()
    }
}
