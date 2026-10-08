package com.example.cas

import com.example.cas.cas.*
import org.junit.Assert.assertEquals
import org.junit.Test

/** Σ to ∞ with no closed form: converges or diverges, and by which test. */
class ConvergenceTest {
    private val n = Sym("n")
    private fun v(a: Expr, lo: Long = 1) = Convergence.of(a, n, Num(lo))?.let { (if (it.converges) "C " else "D ") + it.test.name } ?: "?"
    private fun f(name: String, vararg a: Expr) = Fn(name, a.toList())

    @Test fun cases() {
        val cases = listOf(
            "n!/n^n" to (div(f("fact", n), pow(n, n)) to "C Ratio"),
            "n/2^n" to (div(n, pow(Num(2), n)) to "C Ratio"),
            "2^n/n!" to (div(pow(Num(2), n), f("fact", n)) to "C Ratio"),
            "n!/2^n" to (div(f("fact", n), pow(Num(2), n)) to "D Ratio"),
            "1/(n ln n)" to (div(ONE, mul(n, f("ln", n))) to "D Integral"),
            "1/(n ln² n)" to (div(ONE, mul(n, pow(f("ln", n), 2))) to "C Integral"),
            "(-1)^n/n" to (div(pow(MINUS_ONE, n), n) to "C Alternating"),
            "(-1)^n n/(n+1)" to (div(mul(pow(MINUS_ONE, n), n), add(n, ONE)) to "D Divergence"),
            "1/(n²+1)" to (div(ONE, add(pow(n, 2), ONE)) to "C Comparison"),
            "n^n/(n+1)^(n²)" to (div(pow(n, n), pow(add(n, ONE), pow(n, 2))) to "C Root"),
            "1/√(n+1)" to (div(ONE, sqrt(add(n, ONE))) to "D Comparison"),
            "n/(n+1)" to (div(n, add(n, ONE)) to "D Divergence"),
            "(n/(2n+1))^n" to (pow(div(n, add(mul(Num(2), n), ONE)), n) to "C Ratio"),
            "sin(1/n)" to (f("sin", div(ONE, n)) to "D Comparison"),
            "1/n^(3/2)" to (pow(n, num(-3, 2)) to "C PSeries"),
        )
        val wrong = cases.mapNotNull { (name, c) -> val got = v(c.first, if (name.contains("ln")) 2 else 1); if (got == c.second) null else "$name: $got (expected ${c.second})" }
        assertEquals("", wrong.joinToString("\n"))
    }

    @Test fun divergentSumSaysSo() {
        val s = Calculus.sum(div(ONE, mul(n, f("ln", n))), n, Num(2), INF, false)
        assertEquals("diverges", (s as Fn).name)
    }

    // Typed as Σ on the keypad: the answer, and the steps naming the test.
    private fun sigma(lo: String, body: com.example.cas.editor.MathRow) = com.example.cas.editor.MathRow(mutableListOf(
        com.example.cas.editor.BigOp(com.example.cas.editor.BigOpKind.Sum, com.example.cas.editor.row("n"), com.example.cas.editor.row(lo), com.example.cas.editor.row("∞"), body)))
    private val nLnN = com.example.cas.editor.MathRow(mutableListOf(com.example.cas.editor.Frac(com.example.cas.editor.row("1"),
        com.example.cas.editor.MathRow(mutableListOf(com.example.cas.editor.Sym("n"), com.example.cas.editor.Func("ln", listOf(com.example.cas.editor.row("n"))))))))

    @Test fun keypadDivergent() {
        val v = com.example.cas.engine.Evaluator().evaluate(sigma("2", nLnN))
        assertEquals("diverges", (v as Fn).name)
        val s = com.example.cas.engine.Steps.of(sigma("2", nLnN))!!
        assertEquals("Diverges · integral test", s.method)
    }

    @Test fun keypadConvergentStepsAndValue() {
        val body = com.example.cas.editor.MathRow(mutableListOf(com.example.cas.editor.Frac(
            com.example.cas.editor.MathRow(mutableListOf(com.example.cas.editor.Sym("n"), com.example.cas.editor.Sym("!"))),
            com.example.cas.editor.MathRow(mutableListOf(com.example.cas.editor.Sym("n"), com.example.cas.editor.Pow(com.example.cas.editor.row("n")))))))
        val s = com.example.cas.engine.Steps.of(sigma("1", body))!!
        assertEquals("Converges · ratio test", s.method)
        val v = Numeric.real(com.example.cas.engine.Evaluator().evaluate(sigma("1", body)))
        assertEquals(1.879853862, v, 1e-6)
    }
}
