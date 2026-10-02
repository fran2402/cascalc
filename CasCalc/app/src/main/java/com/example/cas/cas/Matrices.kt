package com.example.cas.cas

/** Matrices whose entries are symbolic expressions: det[[a,b],[c,d]] = ad − bc. */
object Matrices {
    fun addAll(xs: List<Expr>): Expr {
        val ms = xs.map { it as? Mat ?: throw MathError("Can't add a number and a matrix") }
        val first = ms[0]
        if (ms.any { it.rows != first.rows || it.cols != first.cols }) throw MathError("Matrix sizes don't match")
        return Mat(first.rows, first.cols, first.cells.indices.map { i -> add(ms.map { it.cells[i] }) })
    }

    /** Scalars commute; matrices multiply in the order given. */
    fun multiply(xs: List<Expr>): Expr {
        val scalar = mul(xs.filter { it !is Mat })
        val mats = xs.filterIsInstance<Mat>()
        var m = mats[0]
        for (k in 1 until mats.size) m = times(m, mats[k])
        return if (scalar == ONE) m else Mat(m.rows, m.cols, m.cells.map { mul(scalar, it) })
    }

    /** The Hadamard (elementwise) product: both matrices must be the same size. */
    fun hadamard(a: Mat, b: Mat): Mat {
        if (a.rows != b.rows || a.cols != b.cols) throw MathError("Both matrices must be the same size")
        return Mat(a.rows, a.cols, a.cells.indices.map { Algebra.simplify(mul(a.cells[it], b.cells[it])) })
    }

    /** The Kronecker product: every entry of a multiplied by the whole of b. */
    fun kronecker(a: Mat, b: Mat): Mat {
        val rows = a.rows * b.rows
        val cols = a.cols * b.cols
        if (rows * cols > 4096) throw MathError("That product is too large")
        val cells = ArrayList<Expr>(rows * cols)
        for (r in 0 until rows) for (c in 0 until cols) {
            val x = a.cells[(r / b.rows) * a.cols + c / b.cols]
            val y = b.cells[(r % b.rows) * b.cols + c % b.cols]
            cells += Algebra.simplify(mul(x, y))
        }
        return Mat(rows, cols, cells)
    }

    /** The conjugate transpose Aᴴ (the adjoint). */
    fun hermitian(a: Mat): Mat =
        Mat(a.cols, a.rows, (0 until a.cols).flatMap { c -> (0 until a.rows).map { r -> Algebra.simplify(fn("conj", a.cells[r * a.cols + c])) } })

    private fun times(a: Mat, b: Mat): Mat {
        if (a.cols != b.rows) throw MathError("Matrix sizes don't match for \$\\times\$")
        return Mat(a.rows, b.cols, List(a.rows * b.cols) { idx ->
            val i = idx / b.cols
            val j = idx % b.cols
            add((0 until a.cols).map { k -> mul(a[i, k], b[k, j]) })
        })
    }

    fun identity(n: Int) = Mat(n, n, List(n * n) { if (it / n == it % n) ONE else ZERO })

    fun power(m: Mat, e: Expr): Expr {
        val n = (e as? Num)?.q?.takeIf { it.isInteger }?.num?.toLong() ?: throw MathError("A matrix power must be a whole number")
        if (m.rows != m.cols) throw MathError("Only square matrices have powers")
        if (n < 0) return power(inverse(m) as Mat, Num(-n))
        var result = identity(m.rows)
        var base = m
        var k = n
        while (k > 0) {
            if (k and 1L == 1L) result = times(result, base)
            base = times(base, base)
            k = k shr 1
        }
        return result
    }

    fun transpose(x: Expr): Expr {
        val m = x as? Mat ?: return x
        return Mat(m.cols, m.rows, List(m.rows * m.cols) { idx -> m[idx % m.rows, idx / m.rows] })
    }

    /** Cofactor expansion for small matrices (works symbolically), elimination for larger ones. */
    fun det(x: Expr): Expr {
        val m = x as? Mat ?: return x
        if (m.rows != m.cols) throw MathError("det needs a square matrix")
        return Algebra.expand(detOf(m.cells, m.rows))
    }

    private fun detOf(c: List<Expr>, n: Int): Expr {
        if (n == 1) return c[0]
        if (n == 2) return sub(mul(c[0], c[3]), mul(c[1], c[2]))
        return add((0 until n).map { j ->
            val minor = ArrayList<Expr>()
            for (r in 1 until n) for (k in 0 until n) if (k != j) minor += c[r * n + k]
            val term = mul(c[j], detOf(minor, n - 1))
            if (j % 2 == 0) term else neg(term)
        })
    }

    fun inverse(x: Expr): Expr {
        val m = x as? Mat ?: return pow(x, MINUS_ONE)
        if (m.rows != m.cols) throw MathError("Only square matrices have an inverse")
        val n = m.rows
        if (n > 6) throw MathError("That matrix is too large to invert here")
        val d = det(m)
        if (d == ZERO) throw MathError("This matrix has no inverse")
        if (n == 1) return Mat(1, 1, listOf(Algebra.simplify(div(ONE, m.cells[0]))))
        // Adjugate / determinant.
        val cells = List(n * n) { idx ->
            val i = idx / n
            val j = idx % n
            val minor = ArrayList<Expr>()
            for (r in 0 until n) for (k in 0 until n) if (r != j && k != i) minor += m[r, k]
            val cof = detOf(minor, n - 1)
            Algebra.simplify(div(if ((i + j) % 2 == 0) cof else neg(cof), d))
        }
        return Mat(n, n, cells)
    }
}
