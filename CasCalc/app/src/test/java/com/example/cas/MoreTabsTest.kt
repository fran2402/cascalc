package com.example.cas

import com.example.cas.cas.Numeric
import com.example.cas.cas.Printer
import com.example.cas.editor.Func
import com.example.cas.editor.MathRow
import com.example.cas.editor.row
import com.example.cas.engine.Evaluator
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

/** The functions behind the extra tabs: more trigonometry, signals and piecewise, primes and sequences. */
class MoreTabsTest {
    // "a/b" is typed as a fraction.
    private fun arg(t: String): MathRow = if ('/' in t) t.split('/').let { (n, d) -> MathRow(mutableListOf(com.example.cas.editor.Frac(row(n), row(d)))) } else row(t)
    private fun call(name: String, vararg args: String) = Evaluator().evaluate(MathRow(mutableListOf(Func(name, args.map { arg(it) }))))
    private fun exact(name: String, vararg args: String) = Printer.plain(call(name, *args))
    private fun value(name: String, vararg args: String) = Numeric.real(call(name, *args))
    private fun near(expected: Double, name: String, vararg args: String) =
        assertEquals("$name(${args.joinToString()})", expected, value(name, *args), 1e-9 * maxOf(1.0, abs(expected)))

    @Test fun reciprocalTrig() {
        near(1 / kotlin.math.cos(0.7), "sec", "0.7")
        near(1 / kotlin.math.sin(0.7), "csc", "0.7")
        near(1 / kotlin.math.tan(0.7), "cot", "0.7")
        near(kotlin.math.acos(1 / 2.5), "asec", "2.5")
        near(kotlin.math.asin(1 / 2.5), "acsc", "2.5")
        near(kotlin.math.atan(1 / 2.5), "acot", "2.5")
        near(1 / kotlin.math.cosh(0.7), "sech", "0.7")
        near(1 / kotlin.math.sinh(0.7), "csch", "0.7")
        near(1 / kotlin.math.tanh(0.7), "coth", "0.7")
        near(kotlin.math.acosh(1 / 0.4), "asech", "0.4")
        near(kotlin.math.asinh(1 / 0.4), "acsch", "0.4")
        near(kotlin.math.atanh(1 / 2.5), "acoth", "2.5")
        assertEquals("2", exact("sec", "π/3"))
    }

    @Test fun hypotAtan2Sinc() {
        assertEquals("5", exact("hypot", "3", "4"))
        assertEquals("π/4", exact("atan2", "1", "1"))
        near(kotlin.math.atan2(-1.0, -2.0), "atan2", "−1", "−2")
        assertEquals("1", exact("sinc", "0"))
        near(kotlin.math.sin(2.0) / 2.0, "sinc", "2")
    }

    @Test fun signals() {
        assertEquals("1", exact("heaviside", "3")); assertEquals("0", exact("heaviside", "−3"))
        assertEquals("1", exact("rect", "0.2")); assertEquals("0", exact("rect", "2"))
        assertEquals("1/2", exact("tri", "1/2")); assertEquals("0", exact("tri", "3"))
        assertEquals("2", exact("ramp", "2")); assertEquals("0", exact("ramp", "−2"))
        assertEquals("1/4", exact("sawtooth", "9/4"))
        near(1.0, "squarewave", "0.25"); near(-1.0, "squarewave", "0.75")
        near(-1.0, "trianglewave", "0"); near(1.0, "trianglewave", "0.5")
        assertEquals("1", exact("clamp", "5", "−1", "1"))
        assertEquals("5", exact("lerp", "0", "10", "1/2"))
        assertEquals("1/2", exact("smoothstep", "1/2"))
        assertEquals("1/2", exact("sigmoid", "0"))
        near(kotlin.math.ln(2.0), "softplus", "0")
        assertEquals("1", exact("gauss", "0"))
        assertEquals("1/2", exact("wrap", "7/2", "0", "1"))
        assertEquals("1", exact("pulse", "2", "1", "3")); assertEquals("0", exact("pulse", "4", "1", "3"))
    }

    @Test fun primes() {
        assertEquals("1", exact("isprime", "97")); assertEquals("0", exact("isprime", "91"))
        assertEquals("101", exact("nextprime", "97"))
        assertEquals("89", exact("prevprime", "97"))
        assertEquals("541", exact("prime", "100"))
        assertEquals("25", exact("primepi", "100"))
        assertEquals("40", exact("totient", "100"))
        assertEquals("9", exact("ndivisors", "100"))
        assertEquals("217", exact("sigma", "100"))
        assertEquals("0", exact("moebius", "12")); assertEquals("-1", exact("moebius", "30"))
    }

    @Test fun sequences() {
        assertEquals("354224848179261915075", exact("fib", "100"))
        assertEquals("123", exact("lucas", "10"))
        assertEquals("16796", exact("catalan", "10"))
        assertEquals("1/6", exact("bernoulli", "2")); assertEquals("-1/30", exact("bernoulli", "4")); assertEquals("0", exact("bernoulli", "5"))
        assertEquals("190569292", exact("partitions", "100"))
        assertEquals("945", exact("dfact", "9")); assertEquals("3840", exact("dfact", "10"))
    }

    @Test fun lettersStay() {
        // A letter isn't a number yet: the call stays as it is.
        assertEquals("fib(n)", exact("fib", "n"))
    }

    @Test fun mathConstants() {
        fun c(id: String) = Evaluator().evaluate(MathRow(mutableListOf(com.example.cas.editor.Const(id))))
        assertEquals(1.618033988749895, Numeric.real(c("m:phi")), 1e-12)
        assertEquals(2.414213562373095, Numeric.real(c("m:silver")), 1e-12)
        assertEquals(1.324717957244746, Numeric.real(c("m:plastic")), 1e-12)
        assertEquals(1.2020569031595942, Numeric.real(c("m:apery")), 1e-9)
        assertEquals(23.140692632779267, Numeric.real(c("m:gelfond")), 1e-9)
        for (k in com.example.cas.engine.MathConstant.entries) try { Numeric.real(k.value) } catch (e: Exception) { throw AssertionError("${k.description}: ${e.message} ${Printer.plain(k.value)}") }
    }

    @Test fun distributions() {
        near(0.5 * kotlin.math.exp(-1.0), "exppdf", "2", "0.5")
        near(1 - kotlin.math.exp(-1.0), "expcdf", "2", "0.5")
        assertEquals("1/4", exact("unifpdf", "3", "1", "5")); assertEquals("1/2", exact("unifcdf", "3", "1", "5"))
        assertEquals("4/27", exact("geompdf", "1/3", "3")); assertEquals("19/27", exact("geomcdf", "1/3", "3"))
        near(0.857123460498547, "poissoncdf", "2", "3")
        near(0.24197072451914337, "chi2pdf", "1", "3")
        near(0.1987480430987992, "chi2cdf", "1", "3")
        near(0.3989422804014327, "lognpdf", "1", "0", "1")
        near(0.5, "logncdf", "1", "0", "1")
        near(1 / Math.PI, "cauchypdf", "0", "0", "1"); near(0.75, "cauchycdf", "1", "0", "1")
        near(2 * kotlin.math.exp(-1.0), "weibpdf", "1", "2", "1"); near(1 - kotlin.math.exp(-1.0), "weibcdf", "1", "2", "1")
    }

    @Test fun combinatorics() {
        assertEquals("175", exact("stirling1", "7", "5"))
        assertEquals("25", exact("stirling2", "5", "3"))
        assertEquals("52", exact("bell", "5"))
        assertEquals("44", exact("subfactorial", "5"))
        assertEquals("36", exact("lah", "4", "2"))
        assertEquals("66", exact("eulerian", "5", "2"))
        assertEquals("137/60", exact("harmonic", "5"))
        assertEquals("55", exact("triangular", "10"))
        assertEquals("51", exact("motzkin", "6"))
        assertEquals("70", exact("pell", "6"))
        assertEquals("210", exact("primorial", "10"))
        assertEquals("34560", exact("superfactorial", "5"))
        assertEquals("20", exact("narayana", "5", "3"))
        assertEquals("24", exact("rising", "2", "3")); assertEquals("x(x^2+3x+2)", exact("rising", "x", "3").replace(" ", ""))
        assertEquals("120", exact("falling", "6", "3"))
    }

    @Test fun moreStatistics() {
        fun list(name: String) = Printer.plain(Evaluator().evaluate(MathRow(mutableListOf(Func(name, listOf(com.example.cas.editor.row("2,4,4,4,5,5,7,9")))))))
        assertEquals("4", list("mode")); assertEquals("7", list("range")); assertEquals("8", list("count"))
        assertEquals("4", list("q1")); assertEquals("6", list("q3")); assertEquals("2", list("iqr"))
        assertEquals("232", list("sumsq")); assertEquals("201600", list("prodlist"))
        assertEquals("√29", list("rms"))
        assertEquals("3/2", list("mad"))
        list("geomean"); list("harmean"); list("cv"); list("skew"); list("kurt")
    }

    // Vectors are typed as one-row matrices.
    private fun v(vararg c: String) = MathRow(mutableListOf(com.example.cas.editor.Matrix(1, c.size, c.map { arg(it) })))
    private fun vec(name: String, vararg args: MathRow) = Printer.plain(Evaluator().evaluate(MathRow(mutableListOf(Func(name, args.toList())))))

    @Test fun vectors() {
        assertEquals("5", vec("norm", v("3", "4")))
        assertEquals("5", vec("dist", v("1", "2"), v("4", "6")))
        assertEquals("[[3/2,2]]", vec("midpoint", v("1", "2"), v("2", "2")))
        assertEquals("1", vec("triple", v("1", "0", "0"), v("0", "1", "0"), v("0", "0", "1")))
        assertEquals("1/2", vec("triarea", v("0", "0"), v("1", "0"), v("0", "1")))
        assertEquals("[[2,0]]", vec("proj", v("2", "3"), v("1", "0")))
        assertEquals("[[0,3]]", vec("reject", v("2", "3"), v("1", "0")))
        vec("vangle", v("1", "0"), v("0", "1")); vec("unit", v("3", "4")); vec("outer", v("1", "2"), v("3", "4"))
        vec("cart2pol", v("1", "1")); vec("cart2sph", v("1", "1", "1")); vec("cart2cyl", v("1", "1", "1"))
    }

    @Test fun integers() {
        assertEquals("24", exact("powmod", "2", "10", "1000"))
        assertEquals("4", exact("modinv", "3", "11"))
        assertEquals("31", exact("isqrt", "1000"))
        assertEquals("10", exact("iroot", "1000", "3")); assertEquals("9", exact("iroot", "999", "3"))
        assertEquals("15", exact("digitsum", "12345")); assertEquals("5", exact("ndigits", "12345"))
        assertEquals("54321", exact("revdigits", "12345"))
        assertEquals("3", exact("popcount", "7"))
        assertEquals("8", exact("band", "12", "10")); assertEquals("14", exact("bor", "12", "10")); assertEquals("6", exact("bxor", "12", "10"))
        assertEquals("40", exact("shl", "5", "3")); assertEquals("2", exact("shr", "20", "3"))
        assertEquals("-1", exact("jacobi", "2", "3")); assertEquals("1", exact("jacobi", "2", "7"))
        assertEquals("1", exact("issquare", "144")); assertEquals("0", exact("issquare", "145"))
        assertEquals("powmod(a,2,5)", exact("powmod", "a", "2", "5").replace(" ", ""))
    }

    @Test fun finance() {
        assertEquals("121", exact("fv", "100", "1/10", "2"))
        assertEquals("100", exact("pv", "121", "1/10", "2"))
        near(1000 * 0.05 / (1 - Math.pow(1.05, -10.0)), "pmt", "1000", "0.05", "10")
        near(10.0, "nper", "1000", "129.50457496545667", "0.05")
        assertEquals("331", exact("annuityfv", "100", "1/10", "3"))
        near(100 * (1 - Math.pow(1.1, -3.0)) / 0.1, "annuitypv", "100", "0.1", "3")
        near(Math.pow(1.01, 12.0) - 1, "effrate", "0.12", "12")
        near(100 * kotlin.math.exp(0.1), "contcomp", "100", "0.05", "2")
        assertEquals("30", exact("simpleint", "100", "1/10", "3"))
        assertEquals("1", exact("cagr", "1", "8", "3"))
        near(kotlin.math.ln(2.0) / kotlin.math.ln(1.07), "doubling", "0.07")
        near(1.05 / 1.02 - 1, "fisher", "0.05", "0.02")
        assertEquals("100", exact("sldep", "1100", "100", "10"))
        near(-100 + 60 / 1.1 + 60 / 1.21, "npv", "0.1", "−100", "60", "60")
        near(120 / (kotlin.math.sqrt(27600.0) - 60) - 1, "irr", "−100", "60", "60")
    }

    @Test fun polynomials() {
        fun p(name: String, vararg a: String) = exact(name, *a).replace(" ", "")
        near(0.5 * (3 * 0.09 - 1), "legendre", "2", "0.3")
        assertEquals("4x^2-2", p("hermite", "2", "x"))
        assertEquals("x^2-1", p("hermitehe", "2", "x"))
        assertEquals("4x^3-3x", p("chebyshevt", "3", "x"))
        assertEquals("4x^2-1", p("chebyshevu", "2", "x"))
        assertEquals("x^2-x+1", p("cyclotomic", "6", "x"))
        assertEquals("x^4+x^3+x^2+x+1", p("cyclotomic", "5", "x"))
        assertEquals("x^2+1", p("fibpoly", "3", "x")); assertEquals("x", p("fibpoly", "2", "x"))
        assertEquals("x^2+2", p("lucaspoly", "2", "x"))
        assertEquals("3x^2+3x+1", p("besselpoly", "2", "x"))
        assertEquals("x^3+3x^2+x", p("touchard", "3", "x"))
        near(-7.0 / 16, "legendre", "3", "0.5")
        near(1.0 / 6, "bernoullipoly", "2", "0")
        near(0.5 * (2 - 4 * 0.3 + 0.09), "laguerre", "2", "0.3")
        near(-1.5 * kotlin.math.sqrt(0.75), "assoclegendre", "2", "1", "0.5")
        assertEquals("legendre(n,x)", p("legendre", "n", "x"))
    }
}
