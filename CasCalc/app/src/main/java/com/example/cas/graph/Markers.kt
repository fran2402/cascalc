package com.example.cas.graph

import kotlin.math.cos
import kotlin.math.sin

/** The shapes points can be drawn as (Desmos offers several; pgfplots calls them marks). */
enum class Marker(val label: String) {
    Circle("Circle"), Ring("Ring"), Square("Square"), Diamond("Diamond"), Triangle("Triangle"), Cross("Cross");

    /**
     * The shape centred on (cx, cy) with "radius" [r], as closed outlines to fill (circle and
     * ring as 32-sided polygons; the ring's inner edge runs the other way so it stays hollow)
     * or, for the cross, as two strokes. Both are x₀, y₀, x₁, y₁… in the same units.
     */
    fun outline(cx: Double, cy: Double, r: Double): Pair<List<DoubleArray>, List<DoubleArray>> = when (this) {
        Circle -> listOf(ring(cx, cy, r, false)) to emptyList()
        Ring -> listOf(ring(cx, cy, r, false), ring(cx, cy, r * 0.55, true)) to emptyList()
        Square -> listOf(doubleArrayOf(cx - r * 0.85, cy - r * 0.85, cx + r * 0.85, cy - r * 0.85, cx + r * 0.85, cy + r * 0.85, cx - r * 0.85, cy + r * 0.85)) to emptyList()
        Diamond -> listOf(doubleArrayOf(cx, cy - r * 1.15, cx + r * 1.15, cy, cx, cy + r * 1.15, cx - r * 1.15, cy)) to emptyList()
        Triangle -> listOf(doubleArrayOf(cx, cy - r * 1.2, cx + r * 1.1, cy + r * 0.75, cx - r * 1.1, cy + r * 0.75)) to emptyList()
        Cross -> emptyList<DoubleArray>() to listOf(doubleArrayOf(cx - r, cy - r, cx + r, cy + r), doubleArrayOf(cx - r, cy + r, cx + r, cy - r))
    }

    /** The mark added to [scene]: filled shapes, or the cross's strokes ([r] / 2.5 thick). */
    fun addTo(scene: Scene, cx: Double, cy: Double, r: Double, color: Int) {
        val (fills, strokes) = outline(cx, cy, r)
        if (fills.isNotEmpty()) scene.add(Scene.Fill(fills, color))
        if (strokes.isNotEmpty()) scene.add(Scene.Stroke(strokes, color, r / 2.5))
    }

    companion object {
        fun of(index: Int) = entries.getOrElse(index) { Circle }

        private fun ring(cx: Double, cy: Double, r: Double, backwards: Boolean) = DoubleArray(64) { k ->
            val a = (if (backwards) -1 else 1) * (k / 2) * 2 * Math.PI / 32
            if (k % 2 == 0) cx + r * cos(a) else cy + r * sin(a)
        }
    }
}
