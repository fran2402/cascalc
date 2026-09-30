package com.example.cas.graph

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A 3D graph as an STL file, for 3D printing or modeling programs: every polygon cut into
 * triangles, written as binary STL. The box is scaled so its longest side is [size]
 * millimeters, with its lowest corner at the origin.
 */
object Stl {
    fun write(polygons: List<Polygon>, b: Bounds, size: Double = 100.0, name: String = "CAS Scientific Calculator graph"): ByteArray {
        val span = maxOf(b.x1 - b.x0, b.y1 - b.y0, b.z1 - b.z0).takeIf { it > 0 } ?: 1.0
        val k = size / span
        fun mm(p: DoubleArray) = doubleArrayOf((p[0] - b.x0) * k, (p[1] - b.y0) * k, (p[2] - b.z0) * k)
        val triangles = ArrayList<Array<DoubleArray>>()
        for (poly in polygons) {
            val pts = poly.points.filter { p -> p.all { it.isFinite() } }.map(::mm)
            // A fan from the first corner (the polygons are convex).
            for (i in 1 until pts.size - 1) {
                val t = arrayOf(pts[0], pts[i], pts[i + 1])
                if (area(t) > 1e-12) triangles += t
            }
        }
        val buf = ByteBuffer.allocate(84 + 50 * triangles.size).order(ByteOrder.LITTLE_ENDIAN)
        val header = name.toByteArray(Charsets.US_ASCII).copyOf(80)
        buf.put(header)
        buf.putInt(triangles.size)
        for (t in triangles) {
            val n = normal(t)
            n.forEach { buf.putFloat(it.toFloat()) }
            t.forEach { v -> v.forEach { buf.putFloat(it.toFloat()) } }
            buf.putShort(0)
        }
        return buf.array()
    }

    /** The number of triangles in an STL file written by [write]. */
    fun triangleCount(bytes: ByteArray): Int = ByteBuffer.wrap(bytes, 80, 4).order(ByteOrder.LITTLE_ENDIAN).int

    private fun cross(t: Array<DoubleArray>): DoubleArray {
        val u = DoubleArray(3) { t[1][it] - t[0][it] }
        val v = DoubleArray(3) { t[2][it] - t[0][it] }
        return doubleArrayOf(u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0])
    }

    private fun area(t: Array<DoubleArray>) = cross(t).let { Math.sqrt(it[0] * it[0] + it[1] * it[1] + it[2] * it[2]) } / 2

    private fun normal(t: Array<DoubleArray>): DoubleArray {
        val c = cross(t)
        val l = Math.sqrt(c[0] * c[0] + c[1] * c[1] + c[2] * c[2]).takeIf { it > 0 } ?: 1.0
        return DoubleArray(3) { c[it] / l }
    }
}
