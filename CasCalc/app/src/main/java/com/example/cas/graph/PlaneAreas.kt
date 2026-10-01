package com.example.cas.graph

import com.example.cas.cas.CD
import kotlin.math.abs

/**
 * Areas on the complex plane, from curves sampled as polylines: inside a closed curve (the
 * shoelace formula), between a stretch of curve and the real axis (∫ y dx), and between two
 * curves from one crossing to the next.
 */
object PlaneAreas {
    /** The signed area inside a closed polygon: positive when it runs counterclockwise. */
    fun shoelace(p: List<CD>): Double {
        if (p.size < 3) return 0.0
        var s = 0.0
        for (k in p.indices) {
            val a = p[k]; val b = p[(k + 1) % p.size]
            s += a.re * b.im - b.re * a.im
        }
        return s / 2
    }

    /**
     * ∫ y dx along the polyline (trapezoids): the signed area between it and the real axis, and
     * the total with every piece counted as positive (crossings of the axis split the pieces).
     */
    fun toAxis(p: List<CD>): Pair<Double, Double> {
        var signed = 0.0; var total = 0.0
        for (k in 1 until p.size) {
            val a = p[k - 1]; val b = p[k]
            val dx = b.re - a.re
            if (a.im * b.im < 0) {
                // Split where it crosses the axis.
                val f = a.im / (a.im - b.im)
                val m = a.re + f * dx
                signed += a.im / 2 * (m - a.re) + b.im / 2 * (b.re - m)
                total += abs(a.im / 2 * (m - a.re)) + abs(b.im / 2 * (b.re - m))
            } else {
                val piece = (a.im + b.im) / 2 * dx
                signed += piece; total += abs(piece)
            }
        }
        return signed to total
    }

    /** A crossing of two polylines: where along each (index + fraction of the segment) and the point. */
    class Crossing(val alongA: Double, val alongB: Double, val at: CD)

    fun crossings(a: List<CD>, b: List<CD>): List<Crossing> {
        val out = ArrayList<Crossing>()
        for (i in 1 until a.size) {
            val p = a[i - 1]; val r = a[i] - p
            for (j in 1 until b.size) {
                val q = b[j - 1]; val s = b[j] - q
                val den = r.re * s.im - r.im * s.re
                if (den == 0.0) continue
                val qp = q - p
                val t = (qp.re * s.im - qp.im * s.re) / den
                val u = (qp.re * r.im - qp.im * r.re) / den
                if (t in 0.0..1.0 && u in 0.0..1.0) out += Crossing(i - 1 + t, j - 1 + u, CD(p.re + t * r.re, p.im + t * r.im))
            }
        }
        // A crossing at a shared vertex shows up once for each segment that meets there: keep one.
        val sorted = out.sortedBy { it.alongA }
        val kept = ArrayList<Crossing>()
        for (c in sorted) if (kept.isEmpty() || (c.at - kept.last().at).abs() > 1e-9 * (1 + c.at.abs())) kept += c
        return kept
    }

    /**
     * The region between [a] and [b] from their crossing nearest [near] to the next crossing
     * along [a] (or the one before, at its end): its boundary, a's stretch then b's back.
     */
    fun between(a: List<CD>, b: List<CD>, near: CD): List<CD>? {
        val cs = crossings(a, b)
        if (cs.size < 2) return null
        val k = cs.indices.minBy { (cs[it].at - near).abs() }
        val (c0, c1) = if (k + 1 < cs.size) cs[k] to cs[k + 1] else cs[k - 1] to cs[k]
        val out = ArrayList<CD>()
        out += c0.at
        for (i in kotlin.math.ceil(c0.alongA).toInt()..kotlin.math.floor(c1.alongA).toInt()) if (i in a.indices) out += a[i]
        out += c1.at
        // Back along b from c1 to c0, whichever way that runs.
        if (c1.alongB >= c0.alongB) {
            for (j in kotlin.math.floor(c1.alongB).toInt() downTo kotlin.math.ceil(c0.alongB).toInt()) if (j in b.indices) out += b[j]
        } else {
            for (j in kotlin.math.ceil(c1.alongB).toInt()..kotlin.math.floor(c0.alongB).toInt()) if (j in b.indices) out += b[j]
        }
        return out
    }

    /** Every [step]-th point, so crossing tests stay quick. */
    fun thin(p: List<CD>, max: Int = 1500): List<CD> {
        if (p.size <= max) return p
        val step = (p.size + max - 1) / max
        return p.filterIndexed { i, _ -> i % step == 0 || i == p.lastIndex }
    }
}
