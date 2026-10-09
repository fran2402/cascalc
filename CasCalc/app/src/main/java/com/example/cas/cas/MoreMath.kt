package com.example.cas.cas

import com.example.cas.math.Rational
import java.math.BigInteger

/**
 * The functions behind four of the extra tabs: vectors and coordinates, modular arithmetic and
 * bits, finance, and orthogonal (and other special) polynomials. Each [eval] gives null for a
 * name it doesn't know.
 */
object MoreMath {
    val VECTORS = setOf("norm", "unit", "vangle", "proj", "reject", "dist", "midpoint", "triple", "triarea", "outer",
        "cart2pol", "pol2cart", "cart2sph", "sph2cart", "cart2cyl")
    val INTEGERS = setOf("powmod", "modinv", "isqrt", "iroot", "digitsum", "ndigits", "revdigits", "popcount",
        "band", "bor", "bxor", "shl", "shr", "jacobi", "issquare")
    val FINANCE = setOf("fv", "pv", "pmt", "nper", "annuityfv", "annuitypv", "effrate", "contcomp", "simpleint",
        "cagr", "doubling", "fisher", "npv", "irr", "sldep")
    val POLYNOMIALS = setOf("legendre", "hermite", "hermitehe", "laguerre", "genlaguerre", "chebyshevt", "chebyshevu",
        "gegenbauer", "assoclegendre", "bernoullipoly", "fibpoly", "lucaspoly", "besselpoly", "touchard", "cyclotomic")

    private fun need(name: String, a: List<Expr>, n: Int) { if (a.size != n) throw MathError("$name takes $n values") }

    // ---- Vectors and coordinates -------------------------------------------------------------

    private fun vec(e: Expr, name: String): List<Expr> {
        val m = e as? Mat ?: throw MathError("$name needs vectors")
        if (m.rows != 1 && m.cols != 1) throw MathError("$name needs vectors (one row or one column)")
        return m.cells
    }
    private fun column(cells: List<Expr>) = Mat(cells.size, 1, cells.map { Algebra.simplify(it) })
    private fun dot(u: List<Expr>, v: List<Expr>): Expr {
        if (u.size != v.size) throw MathError("The vectors must be the same length")
        return add(u.indices.map { mul(u[it], v[it]) })
    }
    private fun len(u: List<Expr>) = pow(add(u.map { pow(it, TWO) }), HALF)

    /** [toAngle] turns radians into the angle unit set, [fromAngle] the other way. */
    fun vectors(name: String, a: List<Expr>, toAngle: (Expr) -> Expr, fromAngle: (Expr) -> Expr): Expr {
        // The answer has the shape of the first vector: a row in, a row out.
        val asRow = (a.firstOrNull() as? Mat)?.let { it.rows == 1 && it.cols > 1 } ?: false
        fun out(cells: List<Expr>) = column(cells).let { if (asRow) Mat(1, it.rows, it.cells) else it }
        return when (name) {
            "norm" -> { need(name, a, 1); len(vec(a[0], name)) }
            "unit" -> { need(name, a, 1); val u = vec(a[0], name); val l = len(u); if (Algebra.simplify(l) == ZERO) throw MathError("The zero vector has no direction"); out(u.map { div(it, l) }) }
            "vangle" -> { need(name, a, 2); val u = vec(a[0], name); val v = vec(a[1], name); toAngle(fn("acos", div(dot(u, v), mul(len(u), len(v))))) }
            "proj" -> { need(name, a, 2); val u = vec(a[0], name); val v = vec(a[1], name); val k = div(dot(u, v), dot(v, v)); out(v.map { mul(k, it) }) }
            "reject" -> { need(name, a, 2); val u = vec(a[0], name); val v = vec(a[1], name); val k = div(dot(u, v), dot(v, v)); out(u.indices.map { sub(u[it], mul(k, v[it])) }) }
            "dist" -> { need(name, a, 2); val p = vec(a[0], name); val q = vec(a[1], name); if (p.size != q.size) throw MathError("The points must have the same number of coordinates"); len(p.indices.map { sub(p[it], q[it]) }) }
            "midpoint" -> { need(name, a, 2); val p = vec(a[0], name); val q = vec(a[1], name); if (p.size != q.size) throw MathError("The points must have the same number of coordinates"); out(p.indices.map { div(add(p[it], q[it]), TWO) }) }
            "triple" -> { need(name, a, 3); LinearAlgebra.dot(a[0], LinearAlgebra.cross(a[1], a[2])) }
            "triarea" -> {
                // Half the length of AB × AC (points in the plane get z = 0).
                need(name, a, 3)
                val pts = a.map { vec(it, name).let { c -> if (c.size == 2) c + ZERO else c } }
                if (pts.any { it.size != 3 }) throw MathError("The corners need 2 or 3 coordinates")
                val ab = (0..2).map { sub(pts[1][it], pts[0][it]) }; val ac = (0..2).map { sub(pts[2][it], pts[0][it]) }
                div(len(listOf(sub(mul(ab[1], ac[2]), mul(ab[2], ac[1])), sub(mul(ab[2], ac[0]), mul(ab[0], ac[2])), sub(mul(ab[0], ac[1]), mul(ab[1], ac[0])))), TWO)
            }
            "outer" -> { need(name, a, 2); val u = vec(a[0], name); val v = vec(a[1], name); Mat(u.size, v.size, u.flatMap { x -> v.map { y -> Algebra.simplify(mul(x, y)) } }) }
            // (x, y) → (r, θ), θ = atan2(y, x).
            "cart2pol" -> { need(name, a, 1); val (x, y) = vec(a[0], name).also { if (it.size != 2) throw MathError("cart2pol needs (x, y)") }; out(listOf(len(listOf(x, y)), toAngle(angleOf(x, y)))) }
            "pol2cart" -> { need(name, a, 2); val t = fromAngle(a[1]); out(listOf(mul(a[0], fn("cos", t)), mul(a[0], fn("sin", t)))) }
            // (x, y, z) → (ρ, θ, φ): θ around the z-axis, φ down from it (as on the 3D graph).
            "cart2sph" -> {
                need(name, a, 1)
                val c = vec(a[0], name).also { if (it.size != 3) throw MathError("cart2sph needs (x, y, z)") }
                val rho = len(c)
                out(listOf(rho, toAngle(angleOf(c[0], c[1])), toAngle(fn("acos", div(c[2], rho)))))
            }
            "sph2cart" -> {
                need(name, a, 3)
                val t = fromAngle(a[1]); val p = fromAngle(a[2])
                out(listOf(mul(a[0], fn("sin", p), fn("cos", t)), mul(a[0], fn("sin", p), fn("sin", t)), mul(a[0], fn("cos", p))))
            }
            "cart2cyl" -> { need(name, a, 1); val c = vec(a[0], name).also { if (it.size != 3) throw MathError("cart2cyl needs (x, y, z)") }; out(listOf(len(listOf(c[0], c[1])), toAngle(angleOf(c[0], c[1])), c[2])) }
            else -> throw MathError("Unknown function $name")
        }
    }

    /** The angle of (x, y): exactly at numbers, else 2 atan(y / (r + x)). */
    private fun angleOf(x: Expr, y: Expr): Expr =
        if (x.isConstant && y.isConstant) fn("arg", add(x, mul(I, y)))
        else mul(TWO, fn("atan", div(y, add(len(listOf(x, y)), x))))

    // ---- Modular arithmetic and bits ---------------------------------------------------------

    private fun int(e: Expr, name: String): BigInteger =
        (e as? Num)?.q?.takeIf { it.isInteger }?.num ?: if (e.isConstant) throw MathError("$name takes whole numbers") else throw Unknown
    private object Unknown : RuntimeException()
    private fun big(b: BigInteger) = Num(Rational.of(b))

    fun integers(name: String, a: List<Expr>): Expr? = try {
        fun i(k: Int) = int(a.getOrNull(k) ?: throw MathError("$name needs more values"), name)
        when (name) {
            "powmod" -> { need(name, a, 3); val m = i(2); if (m.signum() <= 0) throw MathError("The modulus must be positive"); big(i(0).modPow(i(1), m)) }
            "modinv" -> { need(name, a, 2); val m = i(1); if (m.signum() <= 0) throw MathError("The modulus must be positive")
                try { big(i(0).modInverse(m)) } catch (e: ArithmeticException) { throw MathError("${i(0)} has no inverse mod $m") } }
            "isqrt" -> { need(name, a, 1); val n = i(0); if (n.signum() < 0) throw MathError("isqrt takes a number ≥ 0"); big(isqrt(n)) }
            "iroot" -> {
                need(name, a, 2)
                val n = i(0); val k = i(1).toInt()
                if (n.signum() < 0 || k < 1) throw MathError("iroot(n, k) takes n ≥ 0 and k ≥ 1")
                // The largest r with rᵏ ≤ n, by bisection.
                var lo = BigInteger.ZERO; var hi = BigInteger.ONE.shiftLeft(n.bitLength() / k + 1)
                while (lo < hi) { val mid = (lo + hi + BigInteger.ONE).shiftRight(1); if (mid.pow(k) <= n) lo = mid else hi = mid - BigInteger.ONE }
                big(lo)
            }
            "digitsum" -> { need(name, a, 1); Num(i(0).abs().toString().sumOf { (it - '0').toLong() }) }
            "ndigits" -> { need(name, a, 1); Num(i(0).abs().toString().length.toLong()) }
            "revdigits" -> { need(name, a, 1); val n = i(0); big(BigInteger(n.abs().toString().reversed()).let { if (n.signum() < 0) it.negate() else it }) }
            "popcount" -> { need(name, a, 1); val n = i(0); if (n.signum() < 0) throw MathError("popcount takes a number ≥ 0"); Num(n.bitCount().toLong()) }
            "band" -> { need(name, a, 2); big(i(0).and(i(1))) }
            "bor" -> { need(name, a, 2); big(i(0).or(i(1))) }
            "bxor" -> { need(name, a, 2); big(i(0).xor(i(1))) }
            "shl" -> { need(name, a, 2); big(i(0).shiftLeft(i(1).toInt().coerceIn(-100_000, 100_000))) }
            "shr" -> { need(name, a, 2); big(i(0).shiftRight(i(1).toInt().coerceIn(-100_000, 100_000))) }
            "jacobi" -> { need(name, a, 2); Num(jacobi(i(0), i(1)).toLong()) }
            "issquare" -> { need(name, a, 1); val n = i(0); Num(if (n.signum() >= 0 && isqrt(n).pow(2) == n) 1L else 0L) }
            else -> null
        }
    } catch (e: Unknown) { null }

    /** The Jacobi symbol (a/n) for odd positive n. */
    private fun jacobi(a0: BigInteger, n0: BigInteger): Int {
        if (n0.signum() <= 0 || !n0.testBit(0)) throw MathError("The Jacobi symbol needs an odd positive n")
        var a = a0.mod(n0); var n = n0; var r = 1
        val three = BigInteger.valueOf(3); val four = BigInteger.valueOf(4); val five = BigInteger.valueOf(5); val eight = BigInteger.valueOf(8)
        while (a.signum() != 0) {
            while (!a.testBit(0)) { a = a.shiftRight(1); val m = n.mod(eight); if (m == three || m == five) r = -r }
            val t = a; a = n; n = t
            if (a.mod(four) == three && n.mod(four) == three) r = -r
            a = a.mod(n)
        }
        return if (n == BigInteger.ONE) r else 0
    }

    // ---- Finance -----------------------------------------------------------------------------

    /** Interest rates are fractions per period (5% is 0.05); n is the number of periods. */
    fun finance(name: String, a: List<Expr>, list: (Int) -> List<Expr>): Expr {
        fun g(r: Expr) = add(ONE, r)
        return when (name) {
            "fv" -> { need(name, a, 3); mul(a[0], pow(g(a[1]), a[2])) }
            "pv" -> { need(name, a, 3); div(a[0], pow(g(a[1]), a[2])) }
            // The payment that pays off PV in n periods.
            "pmt" -> { need(name, a, 3); div(mul(a[0], a[1]), sub(ONE, pow(g(a[1]), neg(a[2])))) }
            // The periods to pay off PV with payments PMT.
            "nper" -> { need(name, a, 3); div(neg(fn("ln", sub(ONE, div(mul(a[0], a[2]), a[1])))), fn("ln", g(a[2]))) }
            "annuityfv" -> { need(name, a, 3); mul(a[0], div(sub(pow(g(a[1]), a[2]), ONE), a[1])) }
            "annuitypv" -> { need(name, a, 3); mul(a[0], div(sub(ONE, pow(g(a[1]), neg(a[2]))), a[1])) }
            // The effective annual rate of r compounded m times a year.
            "effrate" -> { need(name, a, 2); sub(pow(add(ONE, div(a[0], a[1])), a[1]), ONE) }
            "contcomp" -> { need(name, a, 3); mul(a[0], pow(E, mul(a[1], a[2]))) }
            "simpleint" -> { need(name, a, 3); mul(a[0], a[1], a[2]) }
            "cagr" -> { need(name, a, 3); sub(pow(div(a[1], a[0]), div(ONE, a[2])), ONE) }
            "doubling" -> { need(name, a, 1); div(fn("ln", TWO), fn("ln", g(a[0]))) }
            // The real rate from the nominal rate i and inflation π.
            "fisher" -> { need(name, a, 2); sub(div(g(a[0]), g(a[1])), ONE) }
            "npv" -> {
                // npv(r, c₀, c₁, …): c₀ now, c₁ after one period…
                val r = a[0]; val cs = list(1)
                add(cs.mapIndexed { k, c -> div(c, pow(g(r), num(k.toLong()))) })
            }
            "irr" -> Flt(irr(list(0).map { Numeric.real(it) }))
            "sldep" -> { need(name, a, 3); div(sub(a[0], a[1]), a[2]) }
            else -> throw MathError("Unknown function $name")
        }
    }

    /** The rate where the cash flows' present value is 0, by bisection on a sign change. */
    private fun irr(cs: List<Double>): Double {
        if (cs.size < 2) throw MathError("irr needs at least two cash flows")
        fun f(r: Double) = cs.withIndex().sumOf { (k, c) -> c / Math.pow(1 + r, k.toDouble()) }
        var lo = -0.9999; var hi = 10.0
        if (f(lo) * f(hi) > 0) { hi = 1000.0; if (f(lo) * f(hi) > 0) throw MathError("No rate makes these cash flows balance") }
        repeat(200) { val mid = (lo + hi) / 2; if (f(lo) * f(mid) <= 0) hi = mid else lo = mid }
        return (lo + hi) / 2
    }

    // ---- Polynomials -------------------------------------------------------------------------

    private val T = Sym("\u0000t")

    fun polynomial(name: String, a: List<Expr>): Expr? {
        fun n(k: Int, lo: Int = 0): Int {
            val e = a.getOrNull(k) ?: throw MathError("$name needs more values")
            val v = (e as? Num)?.q?.takeIf { it.isInteger }?.num ?: if (e.isConstant) throw MathError("The degree must be a whole number") else return -1
            if (v < BigInteger.valueOf(lo.toLong()) || v > BigInteger.valueOf(120)) throw MathError("Use a degree from $lo to 120")
            return v.toInt()
        }
        val deg = n(0, if (name == "cyclotomic") 1 else 0)
        if (deg < 0) return null
        val x = a.last()
        fun rec(p0: Expr, p1: Expr, step: (Int, Expr, Expr) -> Expr): Expr {
            if (deg == 0) return p0
            var a0 = p0; var a1 = p1
            for (k in 1 until deg) { val next = Algebra.expand(step(k, a1, a0)); a0 = a1; a1 = next }
            return a1
        }
        val t = T
        val p: Expr = when (name) {
            "legendre" -> { need(name, a, 2); rec(ONE, t) { k, pk, pm -> div(sub(mul(num(2L * k + 1), t, pk), mul(num(k.toLong()), pm)), num(k + 1L)) } }
            "hermite" -> { need(name, a, 2); rec(ONE, mul(TWO, t)) { k, pk, pm -> sub(mul(TWO, t, pk), mul(num(2L * k), pm)) } }
            "hermitehe" -> { need(name, a, 2); rec(ONE, t) { k, pk, pm -> sub(mul(t, pk), mul(num(k.toLong()), pm)) } }
            "laguerre" -> { need(name, a, 2); rec(ONE, sub(ONE, t)) { k, pk, pm -> div(sub(mul(sub(num(2L * k + 1), t), pk), mul(num(k.toLong()), pm)), num(k + 1L)) } }
            "genlaguerre" -> { need(name, a, 3); val al = a[1]; rec(ONE, sub(add(ONE, al), t)) { k, pk, pm -> div(sub(mul(sub(add(num(2L * k + 1), al), t), pk), mul(add(num(k.toLong()), al), pm)), num(k + 1L)) } }
            "chebyshevt" -> { need(name, a, 2); rec(ONE, t) { _, pk, pm -> sub(mul(TWO, t, pk), pm) } }
            "chebyshevu" -> { need(name, a, 2); rec(ONE, mul(TWO, t)) { _, pk, pm -> sub(mul(TWO, t, pk), pm) } }
            "gegenbauer" -> { need(name, a, 3); val al = a[1]; rec(ONE, mul(TWO, al, t)) { k, pk, pm -> div(sub(mul(TWO, add(num(k.toLong()), al), t, pk), mul(add(num(k - 1L), mul(TWO, al)), pm)), num(k + 1L)) } }
            "assoclegendre" -> {
                // Pₙᵐ(x) = (−1)ᵐ (1 − x²)^(m/2) dᵐ/dxᵐ Pₙ(x).
                need(name, a, 3)
                val m = (a[1] as? Num)?.q?.takeIf { it.isInteger }?.num?.toInt() ?: throw MathError("m must be a whole number")
                if (m < 0 || m > deg) return Num(0)
                var d = polynomial("legendre", listOf(a[0], t))!!
                repeat(m) { d = Algebra.simplify(Calculus.diff(d, t)) }
                mul(if (m % 2 == 0) ONE else MINUS_ONE, pow(sub(ONE, pow(t, TWO)), num(m.toLong(), 2)), d)
            }
            "bernoullipoly" -> {
                need(name, a, 2)
                add((0..deg).map { k -> mul(Num(Rational.of(binomial(deg, k))), Num(NumberTheory.bernoulli(k)), pow(t, num((deg - k).toLong()))) })
            }
            "fibpoly" -> { need(name, a, 2); rec(ZERO, ONE) { _, pk, pm -> add(mul(t, pk), pm) } }
            "lucaspoly" -> { need(name, a, 2); rec(TWO, t) { _, pk, pm -> add(mul(t, pk), pm) } }
            "besselpoly" -> { need(name, a, 2); rec(ONE, add(t, ONE)) { k, pk, pm -> add(mul(num(2L * k + 1), t, pk), pm) } }
            "touchard" -> { need(name, a, 2); add((0..deg).map { k -> mul(Num(Rational.of(NumberTheory.stirling2Of(deg, k))), pow(t, num(k.toLong()))) }) }
            "cyclotomic" -> { need(name, a, 2); add(cyclotomic(deg).mapIndexed { k, c -> mul(Num(Rational.of(c)), pow(t, num(k.toLong()))) }) }
            else -> return null
        }
        return Algebra.simplify(Algebra.expand(Algebra.expand(p).subst(T, x)))
    }

    private fun binomial(n: Int, k: Int): BigInteger = (1..k).fold(BigInteger.ONE) { acc, j -> acc * BigInteger.valueOf((n - k + j).toLong()) / BigInteger.valueOf(j.toLong()) }

    /** Φₙ's coefficients (lowest first): xⁿ − 1 divided by every Φ_d for d a proper divisor of n. */
    private fun cyclotomic(n: Int): List<BigInteger> {
        var p = MutableList(n + 1) { BigInteger.ZERO }.also { it[0] = BigInteger.ONE.negate(); it[n] = BigInteger.ONE }
        for (d in 1 until n) if (n % d == 0) p = divide(p, cyclotomic(d)).toMutableList()
        return p
    }

    private fun divide(num: List<BigInteger>, den: List<BigInteger>): List<BigInteger> {
        val r = num.toMutableList()
        val q = MutableList(num.size - den.size + 1) { BigInteger.ZERO }
        for (i in q.indices.reversed()) {
            val c = r[i + den.size - 1] / den.last()
            q[i] = c
            for (j in den.indices) r[i + j] = r[i + j] - c * den[j]
        }
        return q
    }

    /** ⌊√n⌋ for n ≥ 0, by Newton's method (BigInteger.sqrt needs Android 13). */
    private fun isqrt(n: java.math.BigInteger): java.math.BigInteger {
        if (n.signum() == 0) return n
        var x = java.math.BigInteger.ONE.shiftLeft((n.bitLength() + 1) / 2)
        while (true) {
            val y = x.add(n.divide(x)).shiftRight(1)
            if (y >= x) return x
            x = y
        }
    }
}
