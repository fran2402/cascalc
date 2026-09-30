package com.example.cas.graph

import com.example.cas.cas.Algebra
import com.example.cas.cas.CD
import com.example.cas.cas.Calculus
import com.example.cas.cas.Expr
import com.example.cas.cas.Flt
import com.example.cas.cas.I
import com.example.cas.cas.MathError
import com.example.cas.cas.Num
import com.example.cas.cas.Numeric
import com.example.cas.cas.PI
import com.example.cas.cas.Sym
import com.example.cas.cas.TWO
import com.example.cas.cas.ZERO
import com.example.cas.cas.add
import com.example.cas.cas.div
import com.example.cas.cas.freeVars
import com.example.cas.cas.freeOf
import com.example.cas.cas.isConstant
import com.example.cas.cas.mul
import com.example.cas.cas.pow
import com.example.cas.cas.sub
import com.example.cas.cas.subst
import com.example.cas.math.Rational
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Contour integrals around circles and residues at points, for the calculator.
 * ∮ is done numerically by the trapezoid rule on the circle (exponentially
 * accurate for functions that are smooth on it), then written exactly when
 * ∮/(2πi) is a simple fraction, which it is whenever the residues are.
 */
object ComplexIntegrals {
    /** ∮ over |z − [center]| = [radius], counterclockwise. */
    fun circle(f: Expr, z: Sym, center: Expr, radius: Expr): Expr {
        if (!center.isConstant || !radius.isConstant) throw MathError("The circle's center and radius must be numbers")
        if (!f.freeVars().all { it == z.name }) throw MathError("∮ needs a function of ${z.name} only")
        val c = Numeric.eval(center)
        val r = Numeric.real(radius)
        if (r <= 0) throw MathError("The radius must be positive")
        val g = ComplexCompiler.compile(f, listOf(z.name))
        val n = 4096
        var sum = CD(0.0)
        for (k in 0 until n) {
            val t = 2 * Math.PI * k / n
            val e = CD(cos(t), sin(t))
            val w = try { g(c + CD(r) * e, DoubleArray(0)) } catch (ex: MathError) { throw MathError("f has a pole on the circle") }
            if (!w.re.isFinite() || !w.im.isFinite()) throw MathError("f has a pole on the circle")
            sum = sum + w * CD(0.0, r) * e
        }
        val integral = sum * CD(2 * Math.PI / n)
        return exactOr(integral)
    }

    /**
     * For f = N/D with D a polynomial: divides (z − a) out of D as often as it goes, so
     * D = (z − a)^m Q, and Res = (N/Q)^(m−1)(a)/(m − 1)!, by substituting (no limits needed).
     * Null if D isn't a polynomial or a isn't a root of it.
     */
    private fun rationalResidue(f: Expr, z: Sym, a: Expr): Expr? {
        val (num, den) = Algebra.together(f)
        if (den.freeOf(z)) return ZERO
        var cs = Algebra.coefficients(Algebra.expand(den), z)?.reversed() ?: return null // highest first
        var m = 0
        while (cs.size > 1) {
            // Synthetic division by (z − a).
            val quotient = ArrayList<Expr>()
            var acc: Expr = ZERO
            for (c in cs) { acc = Algebra.simplify(add(mul(acc, a), c)); quotient += acc }
            val remainder = quotient.removeAt(quotient.lastIndex)
            val isZero = runCatching { Numeric.eval(remainder).let { kotlin.math.abs(it.re) + kotlin.math.abs(it.im) < 1e-12 } }.getOrDefault(remainder == ZERO)
            if (!isZero) break
            cs = quotient; m++
        }
        if (m == 0) return ZERO // not a pole
        val q = add(cs.reversed().mapIndexed { k, c -> mul(c, pow(z, Num(k.toLong()))) })
        var g: Expr = div(num, q)
        repeat(m - 1) { g = Calculus.diff(g, z) }
        var fact = 1L
        for (k in 2 until m) fact *= k
        return Algebra.simplify(div(Algebra.simplify(g.subst(z, a)), Num(fact)))
    }

    /** Res f at z = a: lim (z − a)^m f differentiated m − 1 times, for the pole's order m (up to 8). */
    fun residue(f: Expr, z: Sym, a: Expr): Expr {
        runCatching { rationalResidue(f, z, a) }.getOrNull()?.let { return it }
        for (m in 1..8) {
            val g = Algebra.simplify(mul(pow(sub(z, a), Num(m.toLong())), f))
            val l = runCatching { Calculus.limit(g, z, a) }.getOrNull() ?: continue
            if (!finite(l)) continue
            if (m == 1) return Algebra.simplify(l)
            var d = g
            repeat(m - 1) { d = Algebra.simplify(Calculus.diff(d, z)) }
            var fact = 1L
            for (k in 2 until m) fact *= k
            val v = runCatching { Calculus.limit(d, z, a) }.getOrNull() ?: break
            return Algebra.simplify(div(v, Num(fact)))
        }
        // Essential singularity or no closed form: a small circle round the point.
        if (!a.isConstant) throw MathError("Couldn't find this residue")
        val small = circle(f, z, a, Flt(1e-3))
        return Algebra.simplify(div(small, mul(TWO, PI, I)))
    }

    /**
     * The sum of f's residues at all its poles: the roots of its denominator (so f must be
     * a quotient with a polynomial denominator, like e^{iz}/(z² + 1)²).
     */
    fun allResidues(f: Expr, z: Sym): Expr {
        val (_, den) = Algebra.together(f)
        if (den.freeOf(z)) return ZERO
        val cs = Algebra.coefficients(Algebra.expand(den), z) ?: throw MathError("Couldn't find the poles: the denominator must be a polynomial in ${z.name}")
        if (cs.size > 9) throw MathError("Too many poles to find exactly")
        val roots = com.example.cas.cas.Algebra.solve(com.example.cas.cas.Eq(den, ZERO), z).let { sol ->
            val eqs = if (sol is com.example.cas.cas.Seq) sol.items else listOf(sol)
            eqs.mapNotNull { (it as? com.example.cas.cas.Eq)?.takeIf { e -> e.lhs == z }?.rhs }
        }
        if (roots.isEmpty()) throw MathError("Couldn't find the poles exactly")
        return Algebra.simplify(add(roots.distinctBy { com.example.cas.cas.Printer.plain(it) }.map { residue(f, z, it) }))
    }

    private fun finite(e: Expr): Boolean {
        if (!e.isConstant) return true
        val v = runCatching { Numeric.eval(e) }.getOrNull() ?: return false
        return v.re.isFinite() && v.im.isFinite()
    }

    /** 2πi times a simple fraction a + bi when the value is one, otherwise the decimal. */
    private fun exactOr(integral: CD): Expr {
        val r = integral / CD(0.0, 2 * Math.PI)
        val re = fraction(r.re)
        val im = fraction(r.im)
        if (re != null && im != null) {
            val residues = add(Num(re), mul(Num(im), I))
            return Algebra.simplify(mul(TWO, PI, I, residues))
        }
        return Numeric.fromCD(integral)
    }

    private fun fraction(x: Double): Rational? {
        if (abs(x) < 1e-9) return Rational.ZERO
        for (q in 1..24) {
            val p = Math.rint(x * q)
            if (abs(x * q - p) < 1e-8 * maxOf(1.0, abs(x * q))) return Rational.of(p.toLong(), q.toLong())
        }
        return null
    }
}
