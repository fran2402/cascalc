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

/**
 * A flat piece of a surface in data coordinates (x, y, z), belonging to surface [surface].
 * [edges]: which sides get a mesh line (bit k: from corner k to the next); by default all four
 * sides of a quad and none of a triangle.
 */
class Polygon(val points: List<DoubleArray>, val surface: Int, val wall: Boolean = false, val edges: Int = if (points.size == 4) 0b1111 else 0)

/** One polygon projected to the screen, ready to paint back to front. [center] is its middle in data coordinates. */
class Face(val xs: FloatArray, val ys: FloatArray, val depth: Double, val shade: Float, val height: Float, val surface: Int, val center: DoubleArray, val wall: Boolean = false)

/** A line of the axis box, projected. */
class Segment(val x1: Float, val y1: Float, val x2: Float, val y2: Float)

/**
 * Polygons flattened into plain arrays once (when the surfaces change), so turning the view only
 * has to project numbers: no objects per face, per frame.
 */
class Mesh(polys: List<Polygon>, b: Bounds) {
    val count = polys.size
    /** Polygon k's corners are vertices start[k] until start[k + 1]. */
    val start = IntArray(count + 1)
    /** Corners in scene coordinates. */
    val vx: FloatArray; val vy: FloatArray; val vz: FloatArray
    val surface = IntArray(count)
    val wall = BooleanArray(count)
    val edges = IntArray(count)
    /** Height in the box (0 bottom, 1 top), for the color gradient. */
    val height = FloatArray(count)
    /** Unit normal and middle, in scene coordinates. */
    val nx = FloatArray(count); val ny = FloatArray(count); val nz = FloatArray(count)
    val mx = FloatArray(count); val my = FloatArray(count); val mz = FloatArray(count)
    /** The middle in data coordinates (what a tap reads). */
    val center = DoubleArray(3 * count)
    /**
     * Across each side (side s of polygon k is entry start[k] + s, from corner s to the next): the
     * polygon sharing it, or −1 on an open edge. An open edge, or one where the surface folds
     * over (one side faces the viewer, the other away), is an outline that gets a soft edge.
     */
    val neighbor: IntArray

    init {
        var total = 0
        for ((k, p) in polys.withIndex()) { start[k] = total; total += p.points.size }
        start[count] = total
        vx = FloatArray(total); vy = FloatArray(total); vz = FloatArray(total)
        for ((k, p) in polys.withIndex()) {
            surface[k] = p.surface; wall[k] = p.wall; edges[k] = p.edges
            var sx = 0.0; var sy = 0.0; var sz = 0.0
            for ((c, q) in p.points.withIndex()) {
                val s = b.scene(q[0], q[1], q[2])
                val v = start[k] + c
                vx[v] = s[0].toFloat(); vy[v] = s[1].toFloat(); vz[v] = s[2].toFloat()
                sx += s[0]; sy += s[1]; sz += s[2]
                for (d in 0..2) center[3 * k + d] += q[d] / p.points.size
            }
            val m = p.points.size
            mx[k] = (sx / m).toFloat(); my[k] = (sy / m).toFloat(); mz[k] = (sz / m).toFloat()
            height[k] = ((sz / m + 0.8) / 1.6).coerceIn(0.0, 1.0).toFloat()
            // Normal from the first corner to the second and the last (as the faces always did).
            val a = start[k]; val l = start[k + 1] - 1
            val ax = vx[a + 1] - vx[a]; val ay = vy[a + 1] - vy[a]; val az = vz[a + 1] - vz[a]
            val bx = vx[l] - vx[a]; val by = vy[l] - vy[a]; val bz = vz[l] - vz[a]
            val cx = ay * bz - az * by; val cy = az * bx - ax * bz; val cz = ax * by - ay * bx
            val len = max(1e-12f, sqrt(cx * cx + cy * cy + cz * cz))
            nx[k] = cx / len; ny[k] = cy / len; nz[k] = cz / len
        }
        // Sides matched by their two corners (positions rounded, either order): shared sides meet.
        neighbor = IntArray(total) { -1 }
        fun q(v: Float) = Math.round((v.coerceIn(-3.9f, 3.9f) + 4f) * 262_144f).toLong()
        fun point(v: Int) = (q(vx[v]) shl 42) or (q(vy[v]) shl 21) or q(vz[v])
        val open = HashMap<Pair<Long, Long>, Int>(total)
        for (k in 0 until count) for (v in start[k] until start[k + 1]) {
            val w = if (v + 1 == start[k + 1]) start[k] else v + 1
            val a = point(v); val b = point(w)
            val key = if (a <= b) a to b else b to a
            val other = open.remove(key)
            if (other == null) open[key] = v else { neighbor[v] = other.polygonOf(); neighbor[other] = k }
        }
    }

    /** Which polygon a corner (by its index) belongs to. */
    private fun Int.polygonOf(): Int {
        var lo = 0; var hi = count - 1
        while (lo < hi) { val mid = (lo + hi + 1) / 2; if (start[mid] <= this) lo = mid else hi = mid - 1 }
        return lo
    }
}

/** A [Mesh] seen from a camera: every corner on screen, each polygon's shading, and the order to paint them (far to near). */
class Projected(val mesh: Mesh, val px: FloatArray, val py: FloatArray, val shade: FloatArray, val order: IntArray)

/**
 * Triangles ready for one Canvas.drawVertices call: two floats per corner and a color each, the
 * polygons fanned into triangles and their mesh lines as thin quads, all in painting order (so a
 * line behind the surface stays hidden).
 */
class Triangles(val vertices: FloatArray, val colors: IntArray, val count: Int)

object Surface3D {
    // ---- Building surfaces --------------------------------------------------------------

    /**
     * z = f(x, y) sampled on an n × n grid as quads; undefined corners leave holes. [f] is called
     * from every core, so it must be safe to share. The mesh lines fall on every few grid lines,
     * about 24 across whatever the detail, so a fine surface isn't buried under its own mesh.
     */
    fun explicit(f: (Double, Double) -> Double, b: Bounds, n: Int = 40, surface: Int = 0): List<Polygon> {
        val z = Array(n + 1) { DoubleArray(n + 1) }
        Parallel.rows(n + 1, { }) { _, i0, i1 ->
            for (i in i0 until i1) for (j in 0..n) z[i][j] = try { f(b.x0 + (b.x1 - b.x0) * i / n, b.y0 + (b.y1 - b.y0) * j / n) } catch (e: RuntimeException) { Double.NaN }
        }
        val out = ArrayList<Polygon>()
        val span = b.z1 - b.z0
        val step = maxOf(1, (n + 23) / 24)
        for (i in 0 until n) for (j in 0 until n) {
            val z00 = z[i][j]; val z10 = z[i + 1][j]; val z11 = z[i + 1][j + 1]; val z01 = z[i][j + 1]
            if (!z00.isFinite() || !z10.isFinite() || !z11.isFinite() || !z01.isFinite()) continue
            // Leave out quads entirely above or below the box; clamp the rest to just outside it.
            if ((z00 > b.z1 && z10 > b.z1 && z11 > b.z1 && z01 > b.z1) || (z00 < b.z0 && z10 < b.z0 && z11 < b.z0 && z01 < b.z0)) continue
            fun corner(a: Int, c: Int, zz: Double) = doubleArrayOf(b.x0 + (b.x1 - b.x0) * a / n, b.y0 + (b.y1 - b.y0) * c / n, zz.coerceIn(b.z0 - span, b.z1 + span))
            val edges = (if (j % step == 0) 1 else 0) or (if ((i + 1) % step == 0) 2 else 0) or (if ((j + 1) % step == 0) 4 else 0) or (if (i % step == 0) 8 else 0)
            out += Polygon(listOf(corner(i, j, z00), corner(i + 1, j, z10), corner(i + 1, j + 1, z11), corner(i, j + 1, z01)), surface, edges = edges)
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
        // The n³ values on every core ([f] must be safe to share).
        val v = Array(n + 1) { Array(n + 1) { DoubleArray(n + 1) } }
        Parallel.rows(n + 1, { }) { _, i0, i1 ->
            for (i in i0 until i1) for (j in 0..n) for (k in 0..n) v[i][j][k] = try { f(xs[i], ys[j], zs[k]) } catch (e: RuntimeException) { Double.NaN }
        }
        // Each slab of cells (one i) on whichever core is free, into its own list; joined in order after.
        val slabs = arrayOfNulls<List<Polygon>>(n)
        class Work(val p: Array<DoubleArray> = Array(8) { DoubleArray(3) }, val s: DoubleArray = DoubleArray(8), val ins: IntArray = IntArray(4), val outs: IntArray = IntArray(4))
        Parallel.rows(n, { Work() }) { work, i0, i1 ->
            val p = work.p; val s = work.s; val ins = work.ins; val outs = work.outs
            for (i in i0 until i1) {
                val out = ArrayList<Polygon>()
                for (j in 0 until n) for (k in 0 until n) {
                    var finite = true
                    var pos = 0
                    for (c in 0 until 8) {
                        val ci = i + (c and 1); val cj = j + ((c shr 1) and 1); val ck = k + ((c shr 2) and 1)
                        p[c][0] = xs[ci]; p[c][1] = ys[cj]; p[c][2] = zs[ck]
                        s[c] = v[ci][cj][ck]
                        if (!s[c].isFinite()) finite = false
                        if (s[c] > 0) pos++
                    }
                    // No crossing (all one side) or undefined: nothing in this cell.
                    if (!finite || pos == 8 || (pos == 0 && s.none { it == 0.0 })) continue
                    for (t in TETS) {
                        // Corners inside (< 0) and outside, without lists.
                        var ni = 0; var no = 0
                        for (c in t) if (s[c] < 0) ins[ni++] = c else outs[no++] = c
                        fun cut(a: Int, c: Int): DoubleArray {
                            val w = s[a] / (s[a] - s[c])
                            return doubleArrayOf(p[a][0] + w * (p[c][0] - p[a][0]), p[a][1] + w * (p[c][1] - p[a][1]), p[a][2] + w * (p[c][2] - p[a][2]))
                        }
                        when (ni) {
                            1 -> out += Polygon(listOf(cut(ins[0], outs[0]), cut(ins[0], outs[1]), cut(ins[0], outs[2])), surface)
                            3 -> out += Polygon(listOf(cut(outs[0], ins[0]), cut(outs[0], ins[1]), cut(outs[0], ins[2])), surface)
                            2 -> out += Polygon(listOf(cut(ins[0], outs[0]), cut(ins[0], outs[1]), cut(ins[1], outs[1]), cut(ins[1], outs[0])), surface)
                        }
                    }
                }
                slabs[i] = out
            }
        }
        return slabs.flatMap { it.orEmpty() }
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

    // ---- Fast drawing: a flattened mesh, projected and painted in one call --------------------

    /** Projects every corner of [m] and sorts its polygons far to near, shaded as [faces] shades them. */
    fun project(m: Mesh, cam: Camera, w: Float, h: Float): Projected {
        val cy = cos(cam.yaw).toFloat(); val sy = sin(cam.yaw).toFloat()
        val cp = cos(cam.pitch).toFloat(); val sp = sin(cam.pitch).toFloat()
        val s = (0.3 * minOf(w, h) * cam.zoom).toFloat()
        val n = m.vx.size
        val px = FloatArray(n); val py = FloatArray(n)
        for (v in 0 until n) {
            val x1 = m.vx[v] * cy - m.vy[v] * sy
            val y1 = m.vx[v] * sy + m.vy[v] * cy
            val ry = y1 * cp - m.vz[v] * sp
            val rz = y1 * sp + m.vz[v] * cp
            val k = 3.2f / (3.2f + ry)
            px[v] = w / 2 + x1 * s * k
            py[v] = h / 2 - rz * s * k
        }
        val light = normalize(doubleArrayOf(-0.4, -0.6, 0.7))
        val lx = light[0].toFloat(); val ly = light[1].toFloat(); val lz = light[2].toFloat()
        val shade = FloatArray(m.count)
        // Depth (the middle's distance) and index packed in one long each, sorted as numbers.
        val keys = LongArray(m.count)
        for (k in 0 until m.count) {
            val nx1 = m.nx[k] * cy - m.ny[k] * sy
            val ny1 = m.nx[k] * sy + m.ny[k] * cy
            val lambert = abs(nx1 * lx + (ny1 * cp - m.nz[k] * sp) * ly + (ny1 * sp + m.nz[k] * cp) * lz)
            shade[k] = 0.35f + 0.65f * lambert
            val depth = (m.mx[k] * sy + m.my[k] * cy) * cp - m.mz[k] * sp
            // Far first: larger depth sorts first, so the key is the depth's negation, made sortable.
            // A float's bits order like a signed int once negative ones have their other bits flipped.
            val bits = java.lang.Float.floatToIntBits(-depth)
            val sortable = if (bits < 0) bits xor 0x7FFFFFFF else bits
            keys[k] = (sortable.toLong() shl 32) or k.toLong()
        }
        java.util.Arrays.sort(keys)
        return Projected(m, px, py, shade, IntArray(m.count) { (keys[it] and 0xFFFFFFFFL).toInt() })
    }

    /** The nearest polygon under a screen point, or −1. */
    fun pick(p: Projected, x: Float, y: Float): Int {
        val m = p.mesh
        for (o in p.order.indices.reversed()) {
            val k = p.order[o]
            var inside = false
            val a = m.start[k]; val e = m.start[k + 1]
            var j = e - 1
            for (i in a until e) {
                if ((p.py[i] > y) != (p.py[j] > y) && x < (p.px[j] - p.px[i]) * (y - p.py[i]) / (p.py[j] - p.py[i]) + p.px[i]) inside = !inside
                j = i
            }
            if (inside) return k
        }
        return -1
    }

    /**
     * Every polygon as triangles colored [fill] (ARGB, from the polygon and its shade), each mesh
     * line as a thin quad [line] px wide colored [wire] (from the fill under it), in painting order.
     * Outlines (open edges, and folds where the surface turns from facing the viewer to facing
     * away) get a [feather] px strip fading from the face's color to clear, drawn with the face so
     * nearer faces still cover it: antialiased edges, without drawing anything twice.
     */
    fun triangles(p: Projected, line: Float, fill: (k: Int, shade: Float) -> Int, feather: Float = 0f, wire: (fill: Int) -> Int): Triangles {
        val m = p.mesh
        // Which way each polygon winds on screen (facing the viewer or away).
        val facing = BooleanArray(m.count) { k ->
            var area = 0f
            val a = m.start[k]; val e = m.start[k + 1]
            for (i in a until e) { val j = if (i + 1 == e) a else i + 1; area += p.px[i] * p.py[j] - p.px[j] * p.py[i] }
            area > 0f
        }
        fun outline(k: Int, side: Int): Boolean { val n = m.neighbor[side]; return n < 0 || facing[n] != facing[k] }
        var tris = 0
        for (k in 0 until m.count) {
            tris += m.start[k + 1] - m.start[k] - 2
            tris += 2 * Integer.bitCount(m.edges[k])
            if (feather > 0f) for (v in m.start[k] until m.start[k + 1]) if (outline(k, v)) tris += 2
        }
        val verts = FloatArray(tris * 6)
        val colors = IntArray(tris * 3)
        var vi = 0; var ci = 0
        fun put(x: Float, y: Float, c: Int) { verts[vi++] = x; verts[vi++] = y; colors[ci++] = c }
        val half = line / 2
        for (k in p.order) {
            val a = m.start[k]; val e = m.start[k + 1]
            val c = fill(k, p.shade[k])
            for (i in a + 1 until e - 1) { put(p.px[a], p.py[a], c); put(p.px[i], p.py[i], c); put(p.px[i + 1], p.py[i + 1], c) }
            if (feather > 0f) {
                // The middle on screen, to tell which way is out of the face.
                var cx = 0f; var cy = 0f
                for (i in a until e) { cx += p.px[i]; cy += p.py[i] }
                cx /= (e - a); cy /= (e - a)
                val clear = c and 0x00FFFFFF
                for (i in a until e) {
                    if (!outline(k, i)) continue
                    val j = if (i + 1 == e) a else i + 1
                    val dx = p.px[j] - p.px[i]; val dy = p.py[j] - p.py[i]
                    val len = sqrt(dx * dx + dy * dy)
                    if (len == 0f) { repeat(6) { put(p.px[i], p.py[i], clear) }; continue }
                    var ox = -dy / len * feather; var oy = dx / len * feather
                    // Outwards: away from the middle.
                    if ((p.px[i] - cx) * ox + (p.py[i] - cy) * oy < 0f) { ox = -ox; oy = -oy }
                    put(p.px[i], p.py[i], c); put(p.px[j], p.py[j], c); put(p.px[j] + ox, p.py[j] + oy, clear)
                    put(p.px[i], p.py[i], c); put(p.px[j] + ox, p.py[j] + oy, clear); put(p.px[i] + ox, p.py[i] + oy, clear)
                }
            }
            val edges = m.edges[k]
            if (edges == 0) continue
            val lc = wire(c)
            for (s in 0 until e - a) {
                if (edges and (1 shl s) == 0) continue
                val i = a + s; val j = if (i + 1 == e) a else i + 1
                val dx = p.px[j] - p.px[i]; val dy = p.py[j] - p.py[i]
                val len = sqrt(dx * dx + dy * dy)
                val ox = if (len > 0f) -dy / len * half else 0f; val oy = if (len > 0f) dx / len * half else 0f
                put(p.px[i] + ox, p.py[i] + oy, lc); put(p.px[j] + ox, p.py[j] + oy, lc); put(p.px[j] - ox, p.py[j] - oy, lc)
                put(p.px[i] + ox, p.py[i] + oy, lc); put(p.px[j] - ox, p.py[j] - oy, lc); put(p.px[i] - ox, p.py[i] - oy, lc)
            }
        }
        return Triangles(verts, colors, vi)
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
