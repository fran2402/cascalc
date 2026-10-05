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
}
