package com.example.cas.cas

import com.example.cas.math.Rational
import java.math.BigInteger

/**
 * Linear algebra on matrices with exact or symbolic entries: row reduction,
 * rank, trace, characteristic polynomial, eigenvalues and eigenvectors,
 * dot and cross products.
 */
object LinearAlgebra {
    val LAMBDA = Sym("λ")

    fun mat(e: Expr, what: String): Mat = e as? Mat ?: throw MathError("$what needs a matrix")

    /** Zero test that works for exact, symbolic and decimal entries: (1 − √2)(1 + √2) + 1 is zero. */
    fun isZero(e: Expr): Boolean {
        if (e == ZERO) return true
        val s = Algebra.simplify(Algebra.expand(e))
        if (s == ZERO) return true
        if (s.isConstant) return runCatching { Numeric.eval(s).abs() < 1e-11 }.getOrDefault(false)
        return false
    }

    private fun clean(e: Expr) = Algebra.simplify(Algebra.expand(e)).let { if (isZero(it)) ZERO else it }

    /** Reduced row echelon form and the pivot columns. */
    fun rrefWithPivots(m: Mat): Pair<Mat, List<Int>> {
        val a = MutableList(m.rows) { r -> MutableList(m.cols) { c -> m[r, c] } }
        val pivots = ArrayList<Int>()
        var row = 0
        for (col in 0 until m.cols) {
            if (row >= m.rows) break
            val p = (row until m.rows).firstOrNull { !isZero(a[it][col]) } ?: continue
            val t = a[p]; a[p] = a[row]; a[row] = t
            val piv = a[row][col]
            for (k in 0 until m.cols) a[row][k] = clean(div(a[row][k], piv))
            for (r in 0 until m.rows) if (r != row && !isZero(a[r][col])) {
                val f = a[r][col]
                for (k in 0 until m.cols) a[r][k] = clean(sub(a[r][k], mul(f, a[row][k])))
            }
            pivots += col
            row++
        }
        return Mat(m.rows, m.cols, a.flatten()) to pivots
    }

    fun rref(e: Expr) = rrefWithPivots(mat(e, "rref")).first

    fun rank(e: Expr): Expr = Num(rrefWithPivots(mat(e, "rank")).second.size.toLong())

    fun trace(e: Expr): Expr {
        val m = square(e, "trace")
        return add((0 until m.rows).map { m[it, it] })
    }

    private fun square(e: Expr, what: String): Mat {
        val m = mat(e, what)
        if (m.rows != m.cols) throw MathError("$what needs a square matrix")
        return m
    }

    /** det(λI − A), in λ. */
    fun charpoly(e: Expr): Expr {
        val m = square(e, "charpoly")
        val shifted = Mat(m.rows, m.cols, List(m.rows * m.cols) { idx ->
            val r = idx / m.cols
            val c = idx % m.cols
            if (r == c) sub(LAMBDA, m[r, c]) else neg(m[r, c])
        })
        return Algebra.expand(Matrices.det(shifted))
    }

    /** The distinct eigenvalues, as λ = … equations. */
    fun eigenvalues(e: Expr): Expr {
        if (e.contains { it == LAMBDA }) throw MathError("\$\\lambda\$ is used for the eigenvalues; use another letter in the matrix")
        return Algebra.solve(Eq(charpoly(e), ZERO), LAMBDA)
    }

    private fun values(solutions: Expr): List<Expr> = when (solutions) {
        is Eq -> listOf(solutions.rhs)
        is Seq -> solutions.items.map { (it as Eq).rhs }
        else -> emptyList()
    }

    /** Each eigenvalue followed by a basis of its eigenvectors: λ = 1, v = [1; −1], λ = 3, v = [1; 1]. */
    fun eigenvectors(e: Expr): Expr {
        val m = square(e, "eigvecs")
        val out = ArrayList<Expr>()
        for (value in values(eigenvalues(m))) {
            val shifted = Mat(m.rows, m.cols, List(m.rows * m.cols) { idx ->
                val r = idx / m.cols
                val c = idx % m.cols
                if (r == c) sub(m[r, c], value) else m[r, c]
            })
            val basis = nullspace(shifted)
            if (basis.isEmpty()) continue
            out += Eq(LAMBDA, value)
            out += Eq(Sym("v"), if (basis.size == 1) basis[0] else Seq(basis))
        }
        if (out.isEmpty()) throw MathError("Couldn't find the eigenvectors")
        return Seq(out)
    }

    /** Basis of { x : A x = 0 }, as column vectors with whole-number entries when possible. */
    fun nullspace(m: Mat): List<Mat> {
        val (r, pivots) = rrefWithPivots(m)
        val free = (0 until m.cols).filter { it !in pivots }
        return free.map { f ->
            val v = MutableList<Expr>(m.cols) { ZERO }
            v[f] = ONE
            pivots.forEachIndexed { row, pc -> v[pc] = clean(neg(r[row, f])) }
            Mat(m.cols, 1, integerScaled(v))
        }
    }

    /** Scales a vector of fractions to whole numbers: [1/2, 1/3] → [3, 2]. */
    private fun integerScaled(v: List<Expr>): List<Expr> {
        if (!v.all { it is Num }) return v
        var lcm = BigInteger.ONE
        for (x in v) { val d = (x as Num).q.den; lcm = lcm / lcm.gcd(d) * d }
        val scaled = v.map { (it as Num).q * Rational.of(lcm) }
        var g = BigInteger.ZERO
        for (q in scaled) g = g.gcd(q.num)
        if (g.signum() == 0) return v
        return scaled.map { Num(it / Rational.of(g)) }
    }

    private fun vector(e: Expr, what: String): List<Expr> {
        val m = mat(e, what)
        if (m.rows != 1 && m.cols != 1) throw MathError("$what needs vectors (one row or one column)")
        return m.cells
    }

    fun dot(a: Expr, b: Expr): Expr {
        val u = vector(a, "dot")
        val v = vector(b, "dot")
        if (u.size != v.size) throw MathError("The vectors must be the same length")
        return Algebra.simplify(add(u.indices.map { mul(u[it], v[it]) }))
    }

    fun cross(a: Expr, b: Expr): Expr {
        val u = vector(a, "cross")
        val v = vector(b, "cross")
        if (u.size != 3 || v.size != 3) throw MathError("cross needs two vectors of length 3")
        val cells = listOf(
            sub(mul(u[1], v[2]), mul(u[2], v[1])),
            sub(mul(u[2], v[0]), mul(u[0], v[2])),
            sub(mul(u[0], v[1]), mul(u[1], v[0])),
        ).map { clean(it) }
        val m = a as Mat
        return if (m.rows == 1) Mat(1, 3, cells) else Mat(3, 1, cells)
    }

    /** |v| for a vector: √(Σ vᵢ²). */
    fun norm(e: Expr): Expr {
        val v = vector(e, "norm")
        return sqrt(add(v.map { pow(fn("abs", it), TWO) }))
    }
}
