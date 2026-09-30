package com.example.cas.graph

import kotlin.math.cos
import kotlin.math.sin

/** The shapes points can be drawn as (Desmos offers several; pgfplots calls them marks). */
enum class Marker(val label: String) {
    Circle("Circle"), Ring("Ring"), Square("Square"), Diamond("Diamond"), Triangle("Triangle"), Cross("Cross"),
    // Added later, so saved lines keep their shapes (they're stored by position).
    Plus("Plus"), Star("Star"), Pentagon("Pentagon"), Hexagon("Hexagon"), TriangleDown("Triangle down"), Asterisk("Asterisk"),
    OpenSquare("Open square"), OpenDiamond("Open diamond"), OpenTriangle("Open triangle");

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
        Plus -> emptyList<DoubleArray>() to listOf(doubleArrayOf(cx - r * 1.15, cy, cx + r * 1.15, cy), doubleArrayOf(cx, cy - r * 1.15, cx, cy + r * 1.15))
        Asterisk -> emptyList<DoubleArray>() to (0 until 3).map { k ->
            val a = Math.PI / 2 + k * Math.PI / 3
            doubleArrayOf(cx - r * 1.1 * cos(a), cy - r * 1.1 * sin(a), cx + r * 1.1 * cos(a), cy + r * 1.1 * sin(a))
        }
        Star -> listOf(DoubleArray(20) { k ->
            val a = -Math.PI / 2 + (k / 2) * Math.PI / 5
            val rr = if ((k / 2) % 2 == 0) r * 1.3 else r * 0.55
            if (k % 2 == 0) cx + rr * cos(a) else cy + rr * sin(a)
        }) to emptyList()
        Pentagon -> listOf(polygon(cx, cy, r * 1.1, 5, -Math.PI / 2)) to emptyList()
        Hexagon -> listOf(polygon(cx, cy, r * 1.05, 6, 0.0)) to emptyList()
        TriangleDown -> listOf(doubleArrayOf(cx, cy + r * 1.2, cx - r * 1.1, cy - r * 0.75, cx + r * 1.1, cy - r * 0.75)) to emptyList()
        // Hollow ones: the outline, and a smaller copy running the other way (so it stays open).
        OpenSquare, OpenDiamond, OpenTriangle -> {
            val solid = when (this) { OpenSquare -> Square; OpenDiamond -> Diamond; else -> Triangle }
            val outer = solid.outline(cx, cy, r).first.single()
            val inner = solid.outline(cx, cy, r * 0.55).first.single()
            // Inner points in reverse order.
            val back = DoubleArray(inner.size) { k -> val p = inner.size / 2 - 1 - k / 2; inner[2 * p + k % 2] }
            listOf(outer, back) to emptyList()
        }
    }

    /** The mark added to [scene]: filled shapes, or the cross's strokes ([r] / 2.5 thick). */
    fun addTo(scene: Scene, cx: Double, cy: Double, r: Double, color: Int) {
        val (fills, strokes) = outline(cx, cy, r)
        if (fills.isNotEmpty()) scene.add(Scene.Fill(fills, color))
        if (strokes.isNotEmpty()) scene.add(Scene.Stroke(strokes, color, r / 2.5))
    }

    companion object {
        fun of(index: Int) = entries.getOrElse(index) { Circle }

        private fun polygon(cx: Double, cy: Double, r: Double, n: Int, start: Double) = DoubleArray(2 * n) { k ->
            val a = start + (k / 2) * 2 * Math.PI / n
            if (k % 2 == 0) cx + r * cos(a) else cy + r * sin(a)
        }

        private fun ring(cx: Double, cy: Double, r: Double, backwards: Boolean) = DoubleArray(64) { k ->
            val a = (if (backwards) -1 else 1) * (k / 2) * 2 * Math.PI / 32
            if (k % 2 == 0) cx + r * cos(a) else cy + r * sin(a)
        }
    }
}
