package com.example.cas.graph

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Hypothesis tests on the data table's columns: one-sample, two-sample (Welch) and paired
 * t-tests, χ² tests of independence and of goodness of fit, and the one-proportion z-test. Each
 * gives its statistic, degrees of freedom, p-value and (for means and proportions) a confidence
 * interval, and the distribution the statistic is read against, for drawing.
 */
object Hypothesis {
    /** Which way H₁ points: ≠ (both tails), < (the lower tail), > (the upper tail). */
    enum class Tail(val symbol: String, val latex: String) { Both("≠", "\\neq"), Less("<", "<"), Greater(">", ">") }

    /** The statistic's distribution under H₀: Student's t, χ², or the standard normal. */
    sealed class Dist {
        abstract fun pdf(x: Double): Double
        abstract fun cdf(x: Double): Double
        data class T(val df: Double) : Dist() { override fun pdf(x: Double) = SheetMath.tPdf(x, df); override fun cdf(x: Double) = SheetMath.tCdf(x, df) }
        data class Chi2(val df: Double) : Dist() { override fun pdf(x: Double) = SheetMath.chi2Pdf(x, df); override fun cdf(x: Double) = SheetMath.chi2Cdf(x, df) }
        object Normal : Dist() { override fun pdf(x: Double) = SheetMath.normPdf(x); override fun cdf(x: Double) = SheetMath.normCdf(x) }

        /** The x where the cdf reaches [p]. */
        fun inverse(p: Double): Double = when (this) {
            is Normal -> SheetMath.normInv(p)
            is Chi2 -> SheetMath.invert(p, 0.0, maxOf(1.0, df), ::cdf)
            is T -> SheetMath.invert(p, -1.0, 1.0, ::cdf)
        }
    }

    /**
     * A test's outcome. [h0] and [h1] are LaTeX; [statName] is t, χ² or z; [estimate] the
     * difference or mean the interval is about ([estimateLabel] says which); [critical] the
     * edges of the rejection region at [alpha] (one or two of them).
     */
    class Result(
        val title: String, val h0: String, val h1: String, val statName: String, val statistic: Double, val df: Double?,
        val p: Double, val alpha: Double, val dist: Dist, val tail: Tail, val critical: List<Double>,
        val estimate: Double? = null, val estimateLabel: String? = null, val ci: Pair<Double, Double>? = null, val n: Int,
    ) {
        val reject: Boolean get() = p < alpha
    }

    private fun mean(v: List<Double>) = v.sum() / v.size
    private fun variance(v: List<Double>): Double { val m = mean(v); return v.sumOf { (it - m) * (it - m) } / (v.size - 1) }

    private fun pValue(dist: Dist, stat: Double, tail: Tail): Double = when (tail) {
        Tail.Less -> dist.cdf(stat)
        Tail.Greater -> 1 - dist.cdf(stat)
        Tail.Both -> (2 * minOf(dist.cdf(stat), 1 - dist.cdf(stat))).coerceAtMost(1.0)
    }

    private fun critical(dist: Dist, alpha: Double, tail: Tail): List<Double> = when (tail) {
        Tail.Less -> listOf(dist.inverse(alpha))
        Tail.Greater -> listOf(dist.inverse(1 - alpha))
        Tail.Both -> listOf(dist.inverse(alpha / 2), dist.inverse(1 - alpha / 2))
    }

    /** The interval for a mean (or difference) [est] with standard error [se], at confidence 1 − α (two-sided). */
    private fun interval(dist: Dist, est: Double, se: Double, alpha: Double) = dist.inverse(1 - alpha / 2).let { q -> est - q * se to est + q * se }

    /** Is the mean of [x] equal to [mu0]? */
    fun oneSampleT(x: List<Double>, mu0: Double, tail: Tail, alpha: Double = 0.05, name: String = "x"): Result {
        require(x.size >= 2) { "Needs at least 2 numbers" }
        val n = x.size; val m = mean(x); val se = sqrt(variance(x) / n)
        require(se > 0) { "The numbers are all the same" }
        val t = (m - mu0) / se
        val d = Dist.T(n - 1.0)
        return Result("One-sample t-test", "\\mu_{$name} = ${fmt(mu0)}", "\\mu_{$name} ${tail.latex} ${fmt(mu0)}", "t", t, n - 1.0,
            pValue(d, t, tail), alpha, d, tail, critical(d, alpha, tail), m, "mean", interval(d, m, se, alpha), n)
    }

    /** Are the means of [x] and [y] equal? Welch's test (variances not assumed equal). */
    fun twoSampleT(x: List<Double>, y: List<Double>, tail: Tail, alpha: Double = 0.05, nx: String = "1", ny: String = "2"): Result {
        require(x.size >= 2 && y.size >= 2) { "Each column needs at least 2 numbers" }
        val vx = variance(x) / x.size; val vy = variance(y) / y.size
        val se = sqrt(vx + vy)
        require(se > 0) { "The numbers are all the same" }
        val diff = mean(x) - mean(y)
        val t = diff / se
        val df = (vx + vy) * (vx + vy) / (vx * vx / (x.size - 1) + vy * vy / (y.size - 1))
        val d = Dist.T(df)
        return Result("Two-sample t-test (Welch)", "\\mu_{$nx} = \\mu_{$ny}", "\\mu_{$nx} ${tail.latex} \\mu_{$ny}", "t", t, df,
            pValue(d, t, tail), alpha, d, tail, critical(d, alpha, tail), diff, "difference of means", interval(d, diff, se, alpha), x.size + y.size)
    }

    /** Is the mean difference of the pairs ([x]ᵢ, [y]ᵢ) zero? A one-sample test on the differences. */
    fun pairedT(x: List<Double>, y: List<Double>, tail: Tail, alpha: Double = 0.05, nx: String = "1", ny: String = "2"): Result {
        require(x.size == y.size) { "The two columns need the same number of rows" }
        val r = oneSampleT(x.zip(y) { a, b -> a - b }, 0.0, tail, alpha, "d")
        return Result("Paired t-test", "\\mu_{$nx - $ny} = 0", "\\mu_{$nx - $ny} ${tail.latex} 0", "t", r.statistic, r.df, r.p, alpha, r.dist, tail, r.critical,
            r.estimate, "mean difference", r.ci, x.size)
    }

    /** Are the row and column categories of a table of counts independent? [columns]: the counts, column by column. */
    fun chiSquareIndependence(columns: List<List<Double>>, alpha: Double = 0.05): Result {
        require(columns.size >= 2 && columns.all { it.size == columns[0].size } && columns[0].size >= 2) { "Choose two or more columns of counts with the same number of rows" }
        require(columns.all { c -> c.all { it >= 0 } }) { "Counts can't be negative" }
        val rows = columns[0].size
        val rowSum = (0 until rows).map { r -> columns.sumOf { it[r] } }
        val colSum = columns.map { it.sum() }
        val total = colSum.sum()
        require(total > 0 && rowSum.all { it > 0 } && colSum.all { it > 0 }) { "Every row and column needs some counts" }
        var chi = 0.0
        for (c in columns.indices) for (r in 0 until rows) { val e = rowSum[r] * colSum[c] / total; chi += (columns[c][r] - e) * (columns[c][r] - e) / e }
        val df = ((rows - 1) * (columns.size - 1)).toDouble()
        val d = Dist.Chi2(df)
        return Result("χ² test of independence", "\\text{rows and columns independent}", "\\text{they are related}", "χ²", chi, df,
            1 - d.cdf(chi), alpha, d, Tail.Greater, critical(d, alpha, Tail.Greater), n = total.toInt())
    }

    /** Do the counts [observed] fit the [expected] ones (or equal shares when there are none)? */
    fun chiSquareFit(observed: List<Double>, expected: List<Double>? = null, alpha: Double = 0.05): Result {
        require(observed.size >= 2) { "Needs at least 2 categories" }
        val total = observed.sum()
        val exp = expected?.let { e -> require(e.size == observed.size) { "Observed and expected need the same number of rows" }; val s = e.sum(); e.map { it * total / s } }
            ?: List(observed.size) { total / observed.size }
        require(exp.all { it > 0 }) { "Expected counts must be positive" }
        val chi = observed.indices.sumOf { (observed[it] - exp[it]) * (observed[it] - exp[it]) / exp[it] }
        val df = observed.size - 1.0
        val d = Dist.Chi2(df)
        return Result("χ² goodness of fit", if (expected == null) "\\text{equal shares}" else "\\text{the expected shares}", "\\text{they differ}", "χ²", chi, df,
            1 - d.cdf(chi), alpha, d, Tail.Greater, critical(d, alpha, Tail.Greater), n = total.toInt())
    }

    /** Is the share of successes, [successes] out of [n], equal to [p0]? */
    fun oneProportionZ(successes: Double, n: Double, p0: Double, tail: Tail, alpha: Double = 0.05): Result {
        require(n > 0 && successes in 0.0..n) { "Successes must be between 0 and n" }
        require(p0 > 0 && p0 < 1) { "p₀ must be between 0 and 1" }
        val ph = successes / n
        val z = (ph - p0) / sqrt(p0 * (1 - p0) / n)
        val d = Dist.Normal
        val se = sqrt(ph * (1 - ph) / n)
        return Result("One-proportion z-test", "p = ${fmt(p0)}", "p ${tail.latex} ${fmt(p0)}", "z", z, null,
            pValue(d, z, tail), alpha, d, tail, critical(d, alpha, tail), ph, "sample proportion", interval(d, ph, se, alpha), n.toInt())
    }

    /** A number for the hypotheses: as typed, without trailing zeros. */
    fun fmt(v: Double): String = if (v == Math.rint(v) && abs(v) < 1e12) v.toLong().toString() else String.format(java.util.Locale.ROOT, "%.6g", v).trimEnd('0').trimEnd('.')
}
