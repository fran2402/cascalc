package com.example.cas
import com.example.cas.cas.Printer
import com.example.cas.editor.*
import com.example.cas.engine.Evaluator
import org.junit.Assert.assertEquals
import org.junit.Test
/** ℒ, ℱ and their inverses, δ and H, as the calculator works them out. */
class TransformTest {
    private fun r(text: String): MathRow { val out = MathRow(); for (c in text) out.add(when (c) { '²' -> Pow(row("2")); '³' -> Pow(row("3")); else -> Sym(c.toString()) }); return out }
    private fun r(vararg parts: Any): MathRow = MathRow(parts.flatMap { p -> if (p is String) r(p).items else listOf(p as Node) }.toMutableList())
    private fun f(name: String, vararg a: Any) = Func(name, a.map { when (it) { is MathRow -> it; is String -> r(it); else -> MathRow(mutableListOf(it as Node)) } })
    private fun L(body: MathRow, inv: Boolean = false) = r(Func("laplace", listOf(if (inv) r("−1") else MathRow(), body)))
    private fun F(body: MathRow, inv: Boolean = false) = r(Func("fourier", listOf(if (inv) r("−1") else MathRow(), body)))
    private fun e(exp: MathRow) = r(Sym("e"), Pow(exp))
    private val expected = mapOf("L 1" to "1/s", "L t" to "1/s^2", "L t\u00b2" to "2/s^3", "L e^2t" to "1/(s-2)", "L sin3t" to "3/(s^2+9)", "L cos t" to "s/(s^2+1)", "L t e^t" to "1/(s-1)^2", "L e^-t sin t" to "1/(s^2+2s+2)", "L H(t-2)" to "e^(-2s)/s", "L \u03b4(t-1)" to "e^(-s)", "L t sin t" to "2s/(s^2+1)^2", "Li 1/(s-2)" to "e^(2t)", "Li 1/s\u00b2" to "t", "Li (s+1)/(s\u00b2+2s+5)" to "cos(2t)e^(-t)", "Li 1/(s(s+1))" to "-e^(-t)+1", "Li e^-2s/s" to "heaviside(t-2)", "Li 1" to "dirac(t)", "Li 1/(s+1)\u00b3" to "t^2e^(-t)/2", "F \u03b4(t)" to "1", "F 1" to "2\u03c0dirac(\u03c9)", "F e^-|t|" to "2/(\u03c9^2+1)", "F e^-t\u00b2" to "e^(-\u03c9^2/4)\u221a\u03c0", "F rect" to "2sin(\u03c9/2)/\u03c9", "F H e^-t" to "1/(i\u03c9+1)", "F cos t" to "\u03c0(dirac(\u03c9-1)+dirac(\u03c9+1))", "Fi 1" to "dirac(t)", "Fi 2/(1+\u03c9\u00b2)" to "e^(-|t|)", "Fi 1/(1+i\u03c9)" to "heaviside(t)e^(-t)", "Fi e^-\u03c9\u00b2" to "e^(-t^2/4)/(2\u221a\u03c0)", "dirac 2" to "0", "graph L sin" to "1/(x^2+1)")

    @Test fun transforms() {
        val cases = listOf(
            "L 1" to L(r("1")), "L t" to L(r("t")), "L t²" to L(r("t²")), "L e^2t" to L(e(r("2t"))),
            "L sin3t" to L(r(f("sin", "3t"))), "L cos t" to L(r(f("cos", "t"))), "L t e^t" to L(r(Sym("t"), Sym("e"), Pow(r("t")))),
            "L e^-t sin t" to L(r(Sym("e"), Pow(r("−t")), f("sin", "t"))), "L H(t-2)" to L(r(f("heaviside", "t−2"))), "L δ(t-1)" to L(r(f("dirac", "t−1"))),
            "L t sin t" to L(r(Sym("t"), f("sin", "t"))),
            "Li 1/(s-2)" to L(r(Frac(r("1"), r("s−2"))), true), "Li 1/s²" to L(r(Frac(r("1"), r("s²"))), true),
            "Li (s+1)/(s²+2s+5)" to L(r(Frac(r("s+1"), r("s²+2s+5"))), true), "Li 1/(s(s+1))" to L(r(Frac(r("1"), r("s(s+1)"))), true),
            "Li e^-2s/s" to L(r(Frac(e(r("−2s")), r("s"))), true), "Li 1" to L(r("1"), true), "Li 1/(s+1)³" to L(r(Frac(r("1"), r("(s+1)³"))), true),
            "F δ(t)" to F(r(f("dirac", "t"))), "F 1" to F(r("1")), "F e^-|t|" to F(e(r(Sym("−"), Func("abs", listOf(r("t")))))),
            "F e^-t²" to F(e(r("−t²"))), "F rect" to F(r(f("rect", "t"))), "F H e^-t" to F(r(f("heaviside", "t"), Sym("e"), Pow(r("−t")))),
            "F cos t" to F(r(f("cos", "t"))),
            "Fi 1" to F(r("1"), true), "Fi 2/(1+ω²)" to F(r(Frac(r("2"), r("1+ω²"))), true), "Fi 1/(1+iω)" to F(r(Frac(r("1"), r("1+iω"))), true),
            "Fi e^-ω²" to F(e(r("−ω²")), true),
            "dirac 2" to r(f("dirac", "2")), "graph L sin" to L(r(f("sin", "t"))),
        )
        val wrong = cases.mapNotNull { (n, row) ->
            val res = try { Printer.plain(Evaluator(solveEquations = true, transformVariable = if (n.startsWith("graph")) "x" else null).evaluate(row)).replace(" ", "") } catch (e: Exception) { "ERROR ${e.message}" }
            if (res == expected[n]) null else "$n: $res"
        }
        assertEquals("", wrong.joinToString("\n"))
    }

    // δ in the graphs: each c·g(x)·δ(kx + b) is an arrow of height c·g(a)/|k| at a = −b/k.
    private fun impulses(e: com.example.cas.cas.Expr) = com.example.cas.graph.Impulses.of(e, com.example.cas.cas.Sym("x")).map { Printer.plain(it.at) to Printer.plain(it.height) }
    @Test fun impulseArrow() = assertEquals(listOf("2" to "3"), impulses(Evaluator().evaluate(r("3", f("dirac", "x−2")))))
    @Test fun impulseScaled() = assertEquals(listOf("1/2" to "1/2"), impulses(Evaluator().evaluate(r(f("dirac", "2x−1")))))
    @Test fun impulseSamplesItsFactor() = assertEquals(listOf("1" to "1"), impulses(Evaluator().evaluate(r("x²", f("dirac", "x−1")))))
    @Test fun impulsesOfACosine() = assertEquals(2, impulses(Evaluator(transformVariable = "x").evaluate(F(r(f("cos", "t"))))).size)
    @Test fun noImpulses() = assertEquals(0, impulses(Evaluator().evaluate(r(f("sin", "x")))).size)
}
