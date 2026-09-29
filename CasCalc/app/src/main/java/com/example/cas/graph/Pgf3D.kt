package com.example.cas.graph

import kotlin.math.hypot

/**
 * The 3D graph's axes drawn as pgfplots draws a 3D axis: the three walls at the back (two sides
 * and the floor) outlined, with a light grid at the major ticks; tick labels in Computer Modern
 * beside the front edges of the floor (x and y) and the left upright edge (z); the axis names in
 * math italic beyond them. [back] goes under the surfaces, [front] over them.
 */
object Pgf3D {
    private class Edge(val a: DoubleArray, val b: DoubleArray)

    private fun ticks(lo: Double, hi: Double) = Plot2D.ticks(lo, hi, 5)

    /** The walls, their grids and outlines. */
    fun back(scene: Scene, b: Bounds, cam: Camera, size: Double, style: Pgf.Style) {
        val w = size.toFloat()
        fun p(x: Double, y: Double, z: Double) = Surface3D.project(x, y, z, b, cam, w, w).let { (sx, sy) -> doubleArrayOf(sx.toDouble(), sy.toDouble()) }
        fun d(x: Double, y: Double, z: Double) = Surface3D.depth(x, y, z, b, cam)
        val cx = (b.x0 + b.x1) / 2; val cy = (b.y0 + b.y1) / 2; val cz = (b.z0 + b.z1) / 2
        // The wall of each pair that's farther away.
        val xw = if (d(b.x0, cy, cz) > d(b.x1, cy, cz)) b.x0 else b.x1
        val yw = if (d(cx, b.y0, cz) > d(cx, b.y1, cz)) b.y0 else b.y1
        val zw = if (d(cx, cy, b.z0) > d(cx, cy, b.z1)) b.z0 else b.z1
        val grid = ArrayList<DoubleArray>()
        fun line(a: DoubleArray, c: DoubleArray) { grid += a + c }
        // x = const wall: lines of constant y and z.
        ticks(b.y0, b.y1).forEach { y -> line(p(xw, y, b.z0), p(xw, y, b.z1)) }
        ticks(b.z0, b.z1).forEach { z -> line(p(xw, b.y0, z), p(xw, b.y1, z)); line(p(b.x0, yw, z), p(b.x1, yw, z)) }
        ticks(b.x0, b.x1).forEach { x -> line(p(x, yw, b.z0), p(x, yw, b.z1)); line(p(x, b.y0, zw), p(x, b.y1, zw)) }
        ticks(b.y0, b.y1).forEach { y -> line(p(b.x0, y, zw), p(b.x1, y, zw)) }
        scene.add(Scene.Stroke(grid, style.grid, 0.5))
        fun wall(vararg c: DoubleArray) = c.toList().let { pts -> (pts + pts.first()).flatMap { it.toList() }.toDoubleArray() }
        scene.add(Scene.Stroke(listOf(
            wall(p(xw, b.y0, b.z0), p(xw, b.y1, b.z0), p(xw, b.y1, b.z1), p(xw, b.y0, b.z1)),
            wall(p(b.x0, yw, b.z0), p(b.x1, yw, b.z0), p(b.x1, yw, b.z1), p(b.x0, yw, b.z1)),
            wall(p(b.x0, b.y0, zw), p(b.x1, b.y0, zw), p(b.x1, b.y1, zw), p(b.x0, b.y1, zw)),
        ), style.ink, Pgf.FRAME_WIDTH))
    }

    /** Tick labels and axis names, outside the box. */
    fun front(scene: Scene, b: Bounds, cam: Camera, size: Double, style: Pgf.Style, names: Triple<String, String, String> = Triple("x", "y", "z")) {
        val w = size.toFloat()
        fun p(x: Double, y: Double, z: Double) = Surface3D.project(x, y, z, b, cam, w, w).let { (sx, sy) -> doubleArrayOf(sx.toDouble(), sy.toDouble()) }
        fun d(x: Double, y: Double, z: Double) = Surface3D.depth(x, y, z, b, cam)
        val center = p((b.x0 + b.x1) / 2, (b.y0 + b.y1) / 2, (b.z0 + b.z1) / 2)
        // The floor is the wall at the back vertically; its nearer edges carry x and y.
        val floor = if (d((b.x0 + b.x1) / 2, (b.y0 + b.y1) / 2, b.z0) >= d((b.x0 + b.x1) / 2, (b.y0 + b.y1) / 2, b.z1)) b.z0 else b.z1
        val yEdge = if (d((b.x0 + b.x1) / 2, b.y0, floor) < d((b.x0 + b.x1) / 2, b.y1, floor)) b.y0 else b.y1
        val xEdge = if (d(b.x0, (b.y0 + b.y1) / 2, floor) < d(b.x1, (b.y0 + b.y1) / 2, floor)) b.x0 else b.x1
        // z beside the upright edge furthest left on the page.
        val uprights = listOf(b.x0 to b.y0, b.x0 to b.y1, b.x1 to b.y0, b.x1 to b.y1)
        val (zx, zy) = uprights.minByOrNull { (x, y) -> p(x, y, (b.z0 + b.z1) / 2)[0] }!!
        fun outward(at: DoubleArray, by: Double): DoubleArray {
            val dx = at[0] - center[0]; val dy = at[1] - center[1]
            val l = hypot(dx, dy).coerceAtLeast(1e-6)
            return doubleArrayOf(at[0] + dx / l * by, at[1] + dy / l * by)
        }
        fun label(at: DoubleArray, text: String, textSize: Double, font: Scene.Font) =
            scene.add(Scene.Label(at[0], at[1], text, textSize, style.ink, Scene.Anchor.Middle, font))
        val sx = Plot2D.niceStep(b.x1 - b.x0, 5); val sy = Plot2D.niceStep(b.y1 - b.y0, 5); val sz = Plot2D.niceStep(b.z1 - b.z0, 5)
        val marks = ArrayList<DoubleArray>()
        // Floor labels that would sit on z's lowest label, at the corner they share, are left out.
        val zFoot = p(zx, zy, b.z0).let { doubleArrayOf(it[0] - 7, it[1]) }
        fun clear(at: DoubleArray) = hypot(at[0] - zFoot[0], at[1] - zFoot[1]) > 18
        ticks(b.x0, b.x1).forEach { x ->
            val at = p(x, yEdge, floor); val o = outward(at, 4.0)
            marks += at + o
            outward(at, 14.0).takeIf(::clear)?.let { label(it, Pgf.tick(x, sx), Pgf.TICK_SIZE, Scene.Font.Roman) }
        }
        ticks(b.y0, b.y1).forEach { y ->
            val at = p(xEdge, y, floor); val o = outward(at, 4.0)
            marks += at + o
            outward(at, 14.0).takeIf(::clear)?.let { label(it, Pgf.tick(y, sy), Pgf.TICK_SIZE, Scene.Font.Roman) }
        }
        ticks(b.z0, b.z1).forEach { z ->
            val at = p(zx, zy, z)
            marks += at + doubleArrayOf(at[0] - 4, at[1])
            scene.add(Scene.Label(at[0] - 7, at[1], Pgf.tick(z, sz), Pgf.TICK_SIZE, style.ink, Scene.Anchor.End, Scene.Font.Roman))
        }
        scene.add(Scene.Stroke(marks, style.ink, Pgf.FRAME_WIDTH * 0.9))
        label(outward(p((b.x0 + b.x1) / 2, yEdge, floor), 34.0), names.first, Pgf.NAME_SIZE, Scene.Font.Italic)
        label(outward(p(xEdge, (b.y0 + b.y1) / 2, floor), 34.0), names.second, Pgf.NAME_SIZE, Scene.Font.Italic)
        val zMid = p(zx, zy, (b.z0 + b.z1) / 2)
        scene.add(Scene.Label(zMid[0] - 38, zMid[1], names.third, Pgf.NAME_SIZE, style.ink, Scene.Anchor.Middle, Scene.Font.Italic, angle = 90.0))
    }
}
