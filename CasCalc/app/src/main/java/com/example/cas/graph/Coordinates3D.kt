package com.example.cas.graph

import com.example.cas.cas.Eq
import com.example.cas.cas.Expr
import com.example.cas.cas.Fn
import com.example.cas.cas.MathError
import com.example.cas.cas.Rel
import com.example.cas.cas.Sym
import com.example.cas.cas.add
import com.example.cas.cas.div
import com.example.cas.cas.pow
import com.example.cas.cas.sub
import com.example.cas.cas.freeVars
import com.example.cas.cas.subst

/**
 * Cylindrical (r, θ, z) and spherical (ρ, θ, φ) coordinates in the 3D graph, as polar ones are
 * in 2D. The letters r, θ, φ and ρ always mean coordinates: r = √(x² + y²), θ the angle round
 * the z-axis, ρ = √(x² + y² + z²) and φ the angle down from the z-axis, so ρ = 2 is a sphere,
 * r = 1 a cylinder and φ = π/4 a cone. The mode says what a line without "=" gives:
 * z = f(x, y), r = f(θ, z) or ρ = f(θ, φ).
 */
object Coordinates3D {
    enum class Mode { Cartesian, Cylindrical, Spherical }

    val LETTERS = setOf("r", "θ", "φ", "ρ")

    /** The letters each system works in (and offers until you choose others). */
    val DEFAULT_LETTERS: Map<Mode, List<String>> = mapOf(
        Mode.Cartesian to listOf("x", "y", "z"),
        Mode.Cylindrical to listOf("r", "θ", "z"),
        Mode.Spherical to listOf("ρ", "θ", "φ"),
    )

    /**
     * From your letters to the ones the graph works in (i → x, j → y, k → z…), for the letters
     * you changed. A letter chosen twice goes to the first system's coordinate.
     */
    fun renaming(chosen: Map<Mode, List<String>>): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (m in Mode.entries) {
            val mine = chosen[m] ?: continue
            DEFAULT_LETTERS.getValue(m).forEachIndexed { k, std -> val l = mine.getOrNull(k); if (l != null && l != std && l !in out) out[l] = std }
        }
        return out
    }
    private val X = Sym("x"); private val Y = Sym("y"); private val Z = Sym("z")
    private val HALF = com.example.cas.cas.Num(com.example.cas.math.Rational.of(1, 2))

    /** [e] with r, θ, φ and ρ written in x, y and z. */
    fun toCartesian(e: Expr): Expr {
        val used = e.freeVars()
        if (used.none { it in LETTERS }) return e
        val xy = add(pow(X, 2L), pow(Y, 2L))
        val rho = pow(add(xy, pow(Z, 2L)), HALF)
        return e.subst(
            mapOf(
                "r" to pow(xy, HALF),
                "ρ" to rho,
                // Built directly: the simplifier doesn't know atan2, and nothing needs simplifying.
                "θ" to Fn("atan2", listOf(Y, X)),
                "φ" to com.example.cas.cas.fn("acos", div(Z, rho)),
            ),
        )
    }

    /** What a 3D line means in [mode], with the coordinate letters turned into x, y and z. */
    fun classify(e: Expr, mode: Mode): PlotSpec3D {
        // An inequality in any of the coordinates: a solid.
        if (e is Rel) return PlotSpec3D.region(Rel(e.parts.map { toCartesian(it) }, e.ops))
        val uses = e.freeVars().any { it in LETTERS }
        if (!uses && mode == Mode.Cartesian) return PlotSpec3D.classify(e)
        val relation: Expr = when {
            e is Eq -> e
            mode == Mode.Cylindrical -> Eq(Sym("r"), e)
            mode == Mode.Spherical -> Eq(Sym("ρ"), e)
            else -> Eq(Z, e)
        }
        relation as Eq
        // z = f(x, y, r, θ) stays a surface over the plane (drawn smoothly); the rest are implicit.
        if (relation.lhs == Z && relation.rhs.freeVars().none { it == "z" || it == "ρ" || it == "φ" }) {
            return PlotSpec3D.classify(Eq(Z, toCartesian(relation.rhs)))
        }
        return PlotSpec3D.classify(Eq(toCartesian(sub(relation.lhs, relation.rhs)), com.example.cas.cas.ZERO))
    }
}
