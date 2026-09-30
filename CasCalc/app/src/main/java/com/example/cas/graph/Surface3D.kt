package com.example.cas.graph

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** How the 3D view is turned: [yaw] around the vertical axis, [pitch] up/down, both in radians. */
data class Camera(val yaw: Double = 0.7, val pitch: Double = 0.55, val zoom: Double = 1.0) {
    fun rotateBy(dYaw: Double, dPitch: Double) = copy(yaw = yaw + dYaw, pitch = (pitch + dPitch).coerceIn(-1.45, 1.45))
    fun zoomBy(f: Double) = copy(zoom = (zoom * f).coerceIn(0.3, 5.0))
}

/** The box drawn: x from [x0] to [x1], y from [y0] to [y1], z from [z0] to [z1]. */
data class Bounds(val x0: Double, val x1: Double, val y0: Double, val y1: Double, val z0: Double, val z1: Double) {
    init { require(x1 > x0 && y1 > y0 && z1 > z0) { "Each upper limit must be above its lower limit" } }

    /** Scene coordinates: x and y in [−1, 1], z in [−0.8, 0.8]. */
    fun scene(x: Double, y: Double, z: Double) = doubleArrayOf(
        -1 + 2 * (x - x0) / (x1 - x0),
        -1 + 2 * (y - y0) / (y1 - y0),
        -0.8 + 1.6 * (z - z0) / (z1 - z0),
    )

    companion object {
        fun square(r: Double) = Bounds(-r, r, -r, r, -r, r)
    }
}

/** A flat piece of a surface in data coordinates (x, y, z), belonging to surface [surface]. */
class Polygon(val points: List<DoubleArray>, val surface: Int, val wall: Boolean = false)

/** One polygon projected to the screen, ready to paint back to front. [center] is its middle in data coordinates. */
class Face(val xs: FloatArray, val ys: FloatArray, val depth: Double, val shade: Float, val height: Float, val surface: Int, val center: DoubleArray, val wall: Boolean = false)

/** A line of the axis box, projected. */
class Segment(val x1: Float, val y1: Float, val x2: Float, val y2: Float)

object Surface3D {
    // ---- Building surfaces --------------------------------------------------------------

    /** z = f(x, y) sampled on an n × n grid as quads; undefined corners leave holes. */
    fun explicit(f: (Double, Double) -> Double, b: Bounds, n: Int = 40, surface: Int = 0): List<Polygon> {
        val z = Array(n + 1) { i -> DoubleArray(n + 1) { j -> f(b.x0 + (b.x1 - b.x0) * i / n, b.y0 + (b.y1 - b.y0) * j / n) } }
        val out = ArrayList<Polygon>()
        val span = b.z1 - b.z0
        for (i in 0 until n) for (j in 0 until n) {
            val corners = listOf(i to j, i + 1 to j, i + 1 to j + 1, i to j + 1)
            if (corners.any { (a, c) -> !z[a][c].isFinite() }) continue
            // Leave out quads entirely above or below the box; clamp the rest to just outside it.
            if (corners.all { (a, c) -> z[a][c] > b.z1 } || corners.all { (a, c) -> z[a][c] < b.z0 }) continue
            out += Polygon(corners.map { (a, c) ->
                doubleArrayOf(b.x0 + (b.x1 - b.x0) * a / n, b.y0 + (b.y1 - b.y0) * c / n, z[a][c].coerceIn(b.z0 - span, b.z1 + span))
            }, surface)
        }
        return out
    }

    /** A z range that fits the middle 96% of a surface's values (so one spike doesn't flatten the rest). */
    fun autoZ(f: (Double, Double) -> Double, x0: Double, x1: Double, y0: Double, y1: Double, n: Int = 40): Pair<Double, Double> {
        val vs = ArrayList<Double>()
        for (i in 0..n) for (j in 0..n) {
            val v = f(x0 + (x1 - x0) * i / n, y0 + (y1 - y0) * j / n)
            if (v.isFinite()) vs += v
        }
        if (vs.isEmpty()) return -1.0 to 1.0
        vs.sort()
        var lo = vs[(vs.size * 0.02).toInt()]
        var hi = vs[((vs.size - 1) * 0.98).toInt()]
        if (hi - lo < 1e-9) { lo -= 1.0; hi += 1.0 }
        return lo to hi
    }

    /** The six tetrahedra of a cube (corner indices 0–7, bit 0 = x, bit 1 = y, bit 2 = z), all sharing the 0–7 diagonal. */
    private val TETS = arrayOf(
        intArrayOf(0, 1, 3, 7), intArrayOf(0, 1, 5, 7), intArrayOf(0, 2, 3, 7),
        intArrayOf(0, 2, 6, 7), intArrayOf(0, 4, 5, 7), intArrayOf(0, 4, 6, 7),
    )

    /** F(x, y, z) = 0 as triangles, by marching tetrahedra on an n³ grid. */
    fun implicit(f: (Double, Double, Double) -> Double, b: Bounds, n: Int = 24, surface: Int = 0): List<Polygon> {
        val xs = DoubleArray(n + 1) { b.x0 + (b.x1 - b.x0) * it / n }
        val ys = DoubleArray(n + 1) { b.y0 + (b.y1 - b.y0) * it / n }
        val zs = DoubleArray(n + 1) { b.z0 + (b.z1 - b.z0) * it / n }
        val v = Array(n + 1) { i -> Array(n + 1) { j -> DoubleArray(n + 1) { k -> f(xs[i], ys[j], zs[k]) } } }
        val out = ArrayList<Polygon>()
        val p = Array(8) { DoubleArray(3) }
        val s = DoubleArray(8)
        for (i in 0 until n) for (j in 0 until n) for (k in 0 until n) {
            var finite = true
            for (c in 0 until 8) {
                val ci = i + (c and 1); val cj = j + ((c shr 1) and 1); val ck = k + ((c shr 2) and 1)
                p[c][0] = xs[ci]; p[c][1] = ys[cj]; p[c][2] = zs[ck]
                s[c] = v[ci][cj][ck]
                if (!s[c].isFinite()) finite = false
            }
            if (!finite) continue
            if (s.all { it > 0 } || s.all { it < 0 }) continue
            for (t in TETS) {
                val inside = t.filter { s[it] < 0 }
                val outside = t.filter { s[it] >= 0 }
                fun cut(a: Int, c: Int): DoubleArray {
                    val w = s[a] / (s[a] - s[c])
                    return DoubleArray(3) { d -> p[a][d] + w * (p[c][d] - p[a][d]) }
                }
                when (inside.size) {
                    1 -> out += Polygon(outside.map { cut(inside[0], it) }, surface)
                    3 -> out += Polygon(inside.map { cut(outside[0], it) }, surface)
                    2 -> {
                        val (a, c) = inside; val (d, e) = outside
                        out += Polygon(listOf(cut(a, d), cut(a, e), cut(c, e), cut(c, d)), surface)
                    }
                }
            }
        }
        return out
    }

    /**
     * The solid where g(x, y, z) < 0, inside the box: its boundary g = 0, and the parts of the
     * box's six walls that are inside it ([Polygon.wall]), so it reads as a closed shape.
     */
    fun solid(g: (Double, Double, Double) -> Double, b: Bounds, n: Int = 24, surface: Int = 0): List<Polygon> =
        implicit(g, b, n, surface) + walls(g, b, n, surface)

    /** Each wall of the box on an n × n grid, each cell cut to where g < 0 (the corners' values interpolated). */
    fun walls(g: (Double, Double, Double) -> Double, b: Bounds, n: Int = 24, surface: Int = 0): List<Polygon> {
        val out = ArrayList<Polygon>()
        // A wall: which axis is fixed (0 x, 1 y, 2 z) and at which end.
        for (axis in 0..2) for (end in listOf(0.0, 1.0)) {
            val (u, v) = when (axis) { 0 -> 1 to 2; 1 -> 0 to 2; else -> 0 to 1 }
            val lo = doubleArrayOf(b.x0, b.y0, b.z0); val hi = doubleArrayOf(b.x1, b.y1, b.z1)
            fun point(i: Int, j: Int) = DoubleArray(3).also { p ->
                p[axis] = lo[axis] + (hi[axis] - lo[axis]) * end
                p[u] = lo[u] + (hi[u] - lo[u]) * i / n
                p[v] = lo[v] + (hi[v] - lo[v]) * j / n
            }
            val pts = Array(n + 1) { i -> Array(n + 1) { j -> point(i, j) } }
            val vals = Array(n + 1) { i -> DoubleArray(n + 1) { j -> pts[i][j].let { g(it[0], it[1], it[2]) } } }
            for (i in 0 until n) for (j in 0 until n) {
                val corners = listOf(i to j, i + 1 to j, i + 1 to j + 1, i to j + 1)
                val sv = corners.map { (a, c) -> vals[a][c] }
                if (sv.any { !it.isFinite() } || sv.all { it >= 0 }) continue
                val cp = corners.map { (a, c) -> pts[a][c] }
                if (sv.all { it < 0 }) { out += Polygon(cp, surface, wall = true); continue }
                // Cut the square along g = 0: keep inside corners and the crossings between.
                val kept = ArrayList<DoubleArray>()
                for (k in 0 until 4) {
                    val a = sv[k]; val c = sv[(k + 1) % 4]
                    if (a < 0) kept += cp[k]
                    if ((a < 0) != (c < 0)) {
                        val w = a / (a - c)
                        kept += DoubleArray(3) { d -> cp[k][d] + w * (cp[(k + 1) % 4][d] - cp[k][d]) }
                    }
                }
                if (kept.size >= 3) out += Polygon(kept, surface, wall = true)
            }
        }
        return out
    }

    // ---- Projection -------------------------------------------------------------------

    /** Rotates into view space: x right, y away from the viewer, z up. */
    fun rotate(p: DoubleArray, cam: Camera): DoubleArray {
        val cy = cos(cam.yaw); val sy = sin(cam.yaw)
        val x1 = p[0] * cy - p[1] * sy
        val y1 = p[0] * sy + p[1] * cy
        val cp = cos(cam.pitch); val sp = sin(cam.pitch)
        // A positive pitch looks down from above (as pgfplots does): the top is nearer, the far
        // side higher on the page.
        return doubleArrayOf(x1, y1 * cp - p[2] * sp, y1 * sp + p[2] * cp)
    }

    private fun screen(r: DoubleArray, cam: Camera, w: Float, h: Float): Pair<Float, Float> {
        // The box's diagonal (2√2 across) must fit the width when turned 45°.
        val s = 0.3 * minOf(w, h) * cam.zoom
        // Mild perspective.
        val k = 3.2 / (3.2 + r[1])
        return (w / 2 + r[0] * s * k).toFloat() to (h / 2 - r[2] * s * k).toFloat()
    }

    /** All polygons projected and sorted far to near (painter's algorithm), shaded by light and height. */
    fun faces(polys: List<Polygon>, b: Bounds, cam: Camera, w: Float, h: Float): List<Face> {
        val light = normalize(doubleArrayOf(-0.4, -0.6, 0.7))
        val out = ArrayList<Face>(polys.size)
        for (poly in polys) {
            val scene = poly.points.map { b.scene(it[0], it[1], it[2]) }
            val r = scene.map { rotate(it, cam) }
            val xs = FloatArray(r.size); val ys = FloatArray(r.size)
            r.forEachIndexed { k, q -> val (x, y) = screen(q, cam, w, h); xs[k] = x; ys[k] = y }
            val depth = r.sumOf { it[1] } / r.size
            val nrm = normalize(cross(sub(r[1], r[0]), sub(r[r.size - 1], r[0])))
            val lambert = abs(nrm[0] * light[0] + nrm[1] * light[1] + nrm[2] * light[2])
            val height = ((scene.sumOf { it[2] } / scene.size + 0.8) / 1.6).coerceIn(0.0, 1.0)
            val center = DoubleArray(3) { d -> poly.points.sumOf { it[d] } / poly.points.size }
            out += Face(xs, ys, depth, (0.35 + 0.65 * lambert).toFloat(), height.toFloat(), poly.surface, center, poly.wall)
        }
        return out.sortedByDescending { it.depth }
    }

    /** The nearest face under a screen point, or null. Faces are sorted far to near, so search from the end. */
    fun pick(faces: List<Face>, px: Float, py: Float): Face? {
        for (k in faces.indices.reversed()) {
            val f = faces[k]
            var inside = false
            var j = f.xs.size - 1
            for (i in f.xs.indices) {
                if ((f.ys[i] > py) != (f.ys[j] > py) && px < (f.xs[j] - f.xs[i]) * (py - f.ys[i]) / (f.ys[j] - f.ys[i]) + f.xs[i]) inside = !inside
                j = i
            }
            if (inside) return f
        }
        return null
    }

    /** The 12 edges of the bounding box, projected. */
    fun box(cam: Camera, w: Float, h: Float): List<Segment> {
        val c = listOf(-1.0, 1.0)
        val pts = c.flatMap { x -> c.flatMap { y -> c.map { z -> doubleArrayOf(x, y, z * 0.8) } } }
        val out = ArrayList<Segment>()
        for (a in pts.indices) for (b in a + 1 until pts.size) {
            if ((0..2).count { pts[a][it] != pts[b][it] } != 1) continue
            val (x1, y1) = screen(rotate(pts[a], cam), cam, w, h)
            val (x2, y2) = screen(rotate(pts[b], cam), cam, w, h)
            out += Segment(x1, y1, x2, y2)
        }
        return out
    }

    /** Screen positions of the x, y and z axis labels (at the ends of the box). */
    fun axisLabels(cam: Camera, w: Float, h: Float): List<Pair<String, Pair<Float, Float>>> = listOf(
        "x" to screen(rotate(doubleArrayOf(1.2, -1.0, -0.8), cam), cam, w, h),
        "y" to screen(rotate(doubleArrayOf(-1.0, 1.2, -0.8), cam), cam, w, h),
        "z" to screen(rotate(doubleArrayOf(-1.0, -1.0, 1.0), cam), cam, w, h),
    )

    /** How far a data point is from the viewer (larger is farther), to tell back from front. */
    fun depth(x: Double, y: Double, z: Double, b: Bounds, cam: Camera): Double = rotate(b.scene(x, y, z), cam)[1]

    /** Where a data point appears on screen (for marking a picked point). */
    fun project(x: Double, y: Double, z: Double, b: Bounds, cam: Camera, w: Float, h: Float): Pair<Float, Float> =
        screen(rotate(b.scene(x, y, z), cam), cam, w, h)

    private fun sub(a: DoubleArray, b: DoubleArray) = doubleArrayOf(a[0] - b[0], a[1] - b[1], a[2] - b[2])
    private fun cross(a: DoubleArray, b: DoubleArray) = doubleArrayOf(a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])
    private fun normalize(a: DoubleArray): DoubleArray {
        val l = max(1e-12, sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]))
        return doubleArrayOf(a[0] / l, a[1] / l, a[2] / l)
    }
}
