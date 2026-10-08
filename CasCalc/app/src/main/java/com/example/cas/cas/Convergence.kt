package com.example.cas.cas

import com.example.cas.math.Rational
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Whether Σ_{k=lo}^{∞} a_k converges, for a sum with no closed form: the tests a textbook
 * tries, in its order, stopping at the first that decides. Each verdict keeps what its steps
 * need to show the working (the limit found, the comparison power, the integral).
 */
object Convergence {
    enum class Test(val label: String) {
        Divergence("divergence test"), Geometric("geometric series"), PSeries("p-series"),
        Alternating("alternating series test"), Ratio("ratio test"), Root("root test"),
        Comparison("limit comparison"), Integral("integral test"),
    }

    /**
     * [converges] by [test]. [limit]: the limit the test turns on (of a_k, the ratio, the k-th
     * root, a_k·k^p, or the integral); [ratio]: a_{k+1}/a_k simplified, or the common ratio;
     * [power]: p for a p-series or the comparison; [body]: |a_k| for the alternating test.
     */
    class Verdict(
        val converges: Boolean, val test: Test,
        val limit: Expr? = null, val ratio: Expr? = null, val power: Expr? = null, val body: Expr? = null,
    )

    fun of(term: Expr, k: Sym, lo: Expr): Verdict? = runCatching { decide(Algebra.simplify(term), k, lo) }.getOrNull()

    private fun decide(a: Expr, k: Sym, lo: Expr): Verdict? {
        if (a.freeVars().any { it != k.name }) return null
        val start = (lo as? Num)?.q?.takeIf { it.isInteger }?.num?.toLong() ?: return null
        val alt = alternating(a, k)
        // 1. The terms must go to 0.
        val lim = if (alt == null) limitAtInfinity(a, k) else limitAtInfinity(alt, k)
        if (lim != null && !isZero(lim)) return Verdict(false, Test.Divergence, limit = if (alt == null) lim else null, body = alt)
        // 2. Geometric: a constant ratio.
        val r = ratio(a, k)
        if (r != null && r.freeOf(k)) {
            val m = runCatching { Numeric.eval(r).abs() }.getOrNull() ?: return null
            return Verdict(m < 1, Test.Geometric, ratio = r)
        }
        // 3. A p-series c·k^p.
        pSeries(a, k)?.let { p -> return Verdict(Numeric.real(p) < -1, Test.PSeries, power = neg(p)) }
        // 4. Alternating: (−1)^k b_k with b_k decreasing to 0.
        if (alt != null && lim != null && isZero(lim) && decreasing(alt, k, start)) return Verdict(true, Test.Alternating, body = alt)
        // 5. Ratio test, when decisive.
        if (r != null) limitAtInfinity(r, k)?.let { l -> decisive(l)?.let { return Verdict(it, Test.Ratio, limit = l, ratio = r) } }
        // 6. Root test, for k-th powers.
        if (a.contains { it is Pow && !it.exp.freeOf(k) }) {
            limitAtInfinity(kthRoot(a, k), k)?.let { l -> decisive(l)?.let { return Verdict(it, Test.Root, limit = l) } }
        }
        // Positive terms from here on.
        if (!positive(a, k, start)) return null
        // 7. Limit comparison with 1/k^p, p read off from how fast the terms shrink.
        estimatePower(a, k)?.let { p ->
            val l = limitAtInfinity(mul(a, pow(k, p)), k)
            if (l != null && !isZero(l) && !Calculus.isInfinite(l)) return Verdict(Numeric.real(p) > 1, Test.Comparison, limit = l, power = p)
        }
        // 8. Integral test.
        if (start >= 1 && decreasing(a, k, start)) {
            // ∫ a dk from the antiderivative's limit at ∞ (ln ln k → ∞), else the definite integral.
            val integral = runCatching {
                val big = Calculus.antiderivative(a, k)!!
                val top = limitAtInfinity(big, k)!!
                if (Calculus.isInfinite(top)) top else Algebra.simplify(sub(top, big.subst(k, Num(start))))
            }.getOrNull() ?: runCatching { Calculus.definite(a, k, Num(start), INF) }.getOrNull()
            if (integral != null && !integral.contains { it is Fn && it.name in setOf("integral", "lim") }) {
                val diverges = Calculus.isInfinite(integral)
                val finite = !diverges && runCatching { Numeric.real(integral).isFinite() }.getOrDefault(false)
                if (diverges || finite) return Verdict(finite, Test.Integral, limit = integral)
            }
        }
        return null
    }

    /** True below 1, false above (or ∞), null at 1 (or when it can't be told). */
    private fun decisive(l: Expr): Boolean? {
        if (Calculus.isPlusInfinity(l)) return false
        val v = runCatching { Numeric.real(l) }.getOrNull() ?: return null
        return when { v < 1 - 1e-9 -> true; v > 1 + 1e-9 -> false; else -> null }
    }

    private fun isZero(e: Expr) = e == ZERO || runCatching { abs(Numeric.real(e)) < 1e-14 }.getOrDefault(false)

    /** lim_{k→∞}, exact when the limit engine gets it, else a clear numerical value, else null. */
    private fun limitAtInfinity(e: Expr, k: Sym): Expr? {
        runCatching { Calculus.limit(e, k, INF) }.getOrNull()
            ?.takeIf { !it.contains { f -> f is Fn && f.name == "lim" } && (Calculus.isInfinite(it) || runCatching { Numeric.real(it).isFinite() }.getOrDefault(false)) }
            ?.let { return it }
        val vs = listOf(1e4, 1e5, 1e6).map { runCatching { Numeric.real(e, mapOf(k.name to it)) }.getOrDefault(Double.NaN) }
        if (vs.any { !it.isFinite() }) return if (vs.all { it.isNaN() || it.isInfinite() } && vs.last() == Double.POSITIVE_INFINITY) INF else null
        if (vs[2] > 1e12 && vs[2] > vs[1] && vs[1] > vs[0]) return INF
        if (abs(vs[2] - vs[1]) > 1e-3 * maxOf(1.0, abs(vs[2]))) return null
        return Flt(vs[2])
    }

    /** a_{k+1}/a_k simplified, with factorials of k + 1 written as (k + 1)·k! so they cancel. */
    fun ratio(a: Expr, k: Sym): Expr? = runCatching {
        val facts = ArrayList<Expr>()
        fun collect(e: Expr) { if (e is Fn && e.name == "fact") facts += e.args[0]; e.children.forEach { collect(it) } }
        collect(a)
        fun unfold(e: Expr): Expr = if (e is Fn && e.name == "fact") {
            val m = facts.firstOrNull { Printer.plain(Algebra.expand(sub(e.args[0], ONE))) == Printer.plain(Algebra.expand(it)) }
            if (m != null) mul(e.args[0], Fn("fact", listOf(m))) else e
        } else e.map { unfold(it) }
        Algebra.simplify(div(unfold(a.subst(k, add(k, ONE))), a))
    }.getOrNull()

    /** |a|^(1/k), taken factor by factor so bᵏ becomes b (and nothing overflows). */
    fun kthRoot(a: Expr, k: Sym): Expr {
        val fs = if (a is Mul) a.factors else listOf(a)
        return Algebra.simplify(mul(fs.map { f ->
            if (f is Pow && !f.exp.freeOf(k)) pow(fn("abs", f.base), Algebra.simplify(div(f.exp, k))) else pow(fn("abs", f), div(ONE, k))
        }))
    }

    /** c·kᵖ: p; null for anything else. */
    private fun pSeries(a: Expr, k: Sym): Expr? {
        val fs = if (a is Mul) a.factors else listOf(a)
        var p: Expr? = null
        for (f in fs) when {
            f == k -> { if (p != null) return null; p = ONE }
            f is Pow && f.base == k && f.exp.freeOf(k) -> { if (p != null) return null; p = f.exp }
            f.freeOf(k) -> {}
            else -> return null
        }
        return p
    }

    /** (−1)ᵏ·b_k (or (−1)^(k+c)·b_k, cos(πk)·b_k): |b_k|; null when the signs don't alternate. */
    fun alternating(a: Expr, k: Sym): Expr? {
        val fs = if (a is Mul) a.factors else listOf(a)
        val sign = fs.indexOfFirst { f ->
            (f is Pow && f.base == MINUS_ONE && Algebra.coefficients(Algebra.expand(f.exp), k)?.let { it.size == 2 && it[1] == ONE } == true) ||
                (f is Fn && f.name == "cos" && Printer.plain(Algebra.simplify(div(f.args[0], k))) == "π")
        }
        if (sign < 0) return null
        val rest = mul(fs.filterIndexed { i, _ -> i != sign })
        return Algebra.simplify(if (runCatching { Numeric.real(rest, mapOf(k.name to 1e3)) < 0 }.getOrDefault(false)) neg(rest) else rest)
    }

    /** b_k decreasing from some point on (checked across a wide range of k). */
    private fun decreasing(b: Expr, k: Sym, start: Long): Boolean {
        val ks = generateSequence(maxOf(start, 1L) + 5) { (it * 1.6).toLong() + 1 }.takeWhile { it < 2_000_000 }.toList()
        val vs = ks.map { runCatching { Numeric.real(b, mapOf(k.name to it.toDouble())) }.getOrDefault(Double.NaN) }
        return vs.all { it.isFinite() } && vs.zipWithNext().all { (u, v) -> v <= u + 1e-15 * abs(u) }
    }

    private fun positive(a: Expr, k: Sym, start: Long): Boolean =
        listOf(start + 3, start + 50, start + 1000, start + 100000).all { runCatching { Numeric.real(a, mapOf(k.name to it.toDouble())) > 0 }.getOrDefault(false) }

    /** p with a_k ~ c/kᵖ, from how much the terms shrink between k and 10k, as a whole or half number. */
    private fun estimatePower(a: Expr, k: Sym): Expr? {
        val v1 = runCatching { Numeric.real(a, mapOf(k.name to 1e5)) }.getOrNull() ?: return null
        val v2 = runCatching { Numeric.real(a, mapOf(k.name to 1e6)) }.getOrNull() ?: return null
        if (v1 <= 0 || v2 <= 0) return null
        val p = (ln(v1 / v2) / ln(10.0))
        val half = (p * 2).roundToInt()
        if (abs(p * 2 - half) > 0.08) return null
        return Num(Rational.of(half.toLong(), 2))
    }
}
