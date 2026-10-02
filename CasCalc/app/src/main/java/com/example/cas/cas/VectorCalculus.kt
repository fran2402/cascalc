package com.example.cas.cas

/** The coordinate systems ∇, ∇·, ∇× and ∇² work in. */
enum class CoordinateKind(val label: String, val defaults: List<String>) {
    Cartesian("Cartesian", listOf("x", "y", "z")),
    /** (r, θ, z); with no z it's plane polar (r, θ). */
    Cylindrical("Cylindrical", listOf("r", "θ", "z")),
    /** (r, θ, φ) in the physics convention: θ from the z-axis, φ around it. */
    Spherical("Spherical", listOf("r", "θ", "φ")),
}

/** A coordinate system and the letters used for its three coordinates. */
data class Coordinates(val kind: CoordinateKind = CoordinateKind.Cartesian, val names: List<String> = kind.defaults) {
    init { require(names.size == 3 && names.toSet().size == 3) { "Three different coordinate letters are needed" } }
    val symbols get() = names.map { Sym(it) }
}

/**
 * Gradient, divergence, curl, Laplacian, Jacobian and Hessian.
 *
 * ∇, ∇·, ∇× and ∇² use the general formulas for orthogonal coordinates
 * q₁, q₂, q₃ with scale factors h₁, h₂, h₃ (H = h₁h₂h₃):
 *   (∇f)ᵢ = (1/hᵢ) ∂f/∂qᵢ
 *   ∇·F  = (1/H) Σ ∂(H Fᵢ / hᵢ)/∂qᵢ
 *   (∇×F)₁ = (1/(h₂h₃)) (∂(h₃F₃)/∂q₂ − ∂(h₂F₂)/∂q₃), and cyclically
 *   ∇²f  = (1/H) Σ ∂((H/hᵢ²) ∂f/∂qᵢ)/∂qᵢ
 * with h = (1, 1, 1) Cartesian, (1, r, 1) cylindrical and (1, r, r sin θ) spherical.
 * Vector components are along the unit vectors of the system (e.g. F_r, F_θ, F_φ).
 * The Jacobian and Hessian are plain matrices of partial derivatives in the chosen letters.
 */
object VectorCalculus {
    private fun components(e: Expr, what: String): Mat {
        val m = e as? Mat ?: throw MathError("$what needs a vector field, like [P, Q, R]")
        if (m.rows != 1 && m.cols != 1) throw MathError("$what needs a vector (one row or one column)")
        return m
    }

    private fun d(f: Expr, x: Sym) = Calculus.diff(f, x)
    private fun s(e: Expr) = Algebra.simplify(e)

    /** Scale factors for [c], for 2 or 3 dimensions. */
    private fun scale(c: Coordinates, dims: Int): List<Expr> {
        val (q1, q2, _) = c.symbols
        return when (c.kind) {
            CoordinateKind.Cartesian -> List(dims) { ONE }
            CoordinateKind.Cylindrical -> listOf(ONE, q1, ONE).take(dims)
            CoordinateKind.Spherical -> {
                if (dims != 3) throw MathError("Spherical coordinates need three components")
                listOf(ONE, q1, mul(q1, fn("sin", q2)))
            }
        }
    }

    /**
     * How many dimensions a scalar lives in: Cartesian uses the letters that appear
     * (so x²y is 2D), cylindrical drops to plane polar when there's no z, spherical is 3D.
     */
    private fun scalarDims(f: Expr, c: Coordinates): Int {
        val vars = f.freeVars()
        return when (c.kind) {
            CoordinateKind.Cartesian -> if (c.names[2] in vars) 3 else 2
            CoordinateKind.Cylindrical -> if (c.names[2] in vars) 3 else 2
            CoordinateKind.Spherical -> 3
        }
    }

    private fun column(cells: List<Expr>, like: Mat? = null) =
        if (like != null && like.rows == 1) Mat(1, cells.size, cells) else Mat(cells.size, 1, cells)

    /** ∇f as a column vector of components along the unit vectors. */
    fun grad(f: Expr, c: Coordinates = Coordinates()): Expr {
        val n = scalarDims(f, c)
        val q = c.symbols.take(n)
        val h = scale(c, n)
        return column(q.indices.map { i -> s(div(d(f, q[i]), h[i])) })
    }

    /** ∇·F. */
    fun div(field: Expr, c: Coordinates = Coordinates()): Expr {
        val m = components(field, "∇·")
        val n = m.cells.size
        if (n !in 2..3) throw MathError("\$\\nabla \\cdot\$ works in 2 or 3 dimensions")
        val q = c.symbols.take(n)
        val h = scale(c, n)
        val big = mul(h)
        return s(div(add(q.indices.map { i -> d(div(mul(big, m.cells[i]), h[i]), q[i]) }), big))
    }

    /** ∇×F: a vector in 3D, and in 2D the scalar (1/(h₁h₂)) (∂(h₂F₂)/∂q₁ − ∂(h₁F₁)/∂q₂). */
    fun curl(field: Expr, c: Coordinates = Coordinates()): Expr {
        val m = components(field, "∇×")
        val n = m.cells.size
        val q = c.symbols.take(n)
        val h = scale(c, n)
        val f = m.cells
        return when (n) {
            2 -> s(div(sub(d(mul(h[1], f[1]), q[0]), d(mul(h[0], f[0]), q[1])), mul(h[0], h[1])))
            3 -> {
                fun comp(i: Int, j: Int, k: Int) = s(div(sub(d(mul(h[k], f[k]), q[j]), d(mul(h[j], f[j]), q[k])), mul(h[j], h[k])))
                column(listOf(comp(0, 1, 2), comp(1, 2, 0), comp(2, 0, 1)), m)
            }
            else -> throw MathError("\$\\nabla \\times\$ works in 2 or 3 dimensions")
        }
    }

    /** ∇²f. */
    fun laplacian(f: Expr, c: Coordinates = Coordinates()): Expr {
        val n = scalarDims(f, c)
        val q = c.symbols.take(n)
        val h = scale(c, n)
        val big = mul(h)
        return s(div(add(q.indices.map { i -> d(mul(div(big, pow(h[i], TWO)), d(f, q[i])), q[i]) }), big))
    }

    /** The coordinates a function depends on, in the system's order; any other letters after. */
    private fun variablesOf(vars: Set<String>, c: Coordinates): List<Sym> {
        val ordered = c.names.filter { it in vars }.map { Sym(it) }
        return ordered.ifEmpty { vars.sorted().map { Sym(it) } }.ifEmpty { throw MathError("There's no variable to differentiate by") }
    }

    /** The Jacobian matrix ∂Fᵢ/∂qⱼ. */
    fun jacobian(field: Expr, c: Coordinates = Coordinates()): Expr {
        val m = components(field, "J")
        val xs = variablesOf(m.cells.flatMap { it.freeVars() }.toSet(), c)
        return Mat(m.cells.size, xs.size, m.cells.flatMap { cell -> xs.map { s(d(cell, it)) } })
    }

    /** The Hessian matrix ∂²f/∂qᵢ∂qⱼ. */
    fun hessian(f: Expr, c: Coordinates = Coordinates()): Expr {
        val xs = variablesOf(f.freeVars(), c)
        return Mat(xs.size, xs.size, xs.flatMap { a -> xs.map { b -> s(d(d(f, a), b)) } })
    }
}
