package com.example.cas

import com.example.cas.cas.Printer
import com.example.cas.editor.Frac
import com.example.cas.editor.Func
import com.example.cas.editor.MathRow
import com.example.cas.editor.Node
import com.example.cas.editor.Pow
import com.example.cas.editor.Sym
import com.example.cas.engine.Evaluator
import org.junit.Assert.assertEquals
import org.junit.Test

/** Answers with letters come out in their simplest form, without asking. */
class AutoSimplifyTest {
    private fun m(vararg parts: Any): MathRow = MathRow(parts.flatMap { p -> when (p) { is String -> p.map { Sym(it.toString()) }; is Node -> listOf(p); else -> error("") } }.toMutableList())
    private fun sq() = Pow(m("2"))
    private fun f(n: String, vararg a: MathRow) = Func(n, a.toList())
    private fun cas(r: MathRow) = Printer.plain(Evaluator().evaluate(r))

    @Test fun cancelsFractions() = assertEquals("x+1", cas(m(Frac(m("x", sq(), "−1"), m("x−1")))))
    @Test fun cancelsSquares() = assertEquals("x+1", cas(m(Frac(m("x", sq(), "+2x+1"), m("x+1")))))
    @Test fun expandsWhenShorter() = assertEquals("2x+1", cas(m("(x+1)", sq(), "−x", sq())))
    @Test fun expandsDifferences() = assertEquals("-4", cas(m("(x+2)(x−2)−x", sq())))
    @Test fun pythagoras() = assertEquals("1", cas(m(f("sin", m("x")), sq(), "+", f("cos", m("x")), sq())))
    @Test fun pythagorasInside() = assertEquals("2", cas(m("1+", f("sin", m("2x")), sq(), "+", f("cos", m("2x")), sq())))
    @Test fun hyperbolic() = assertEquals("1", cas(m(f("cosh", m("x")), sq(), "−", f("sinh", m("x")), sq())))
    @Test fun shortSumsStay() = assertEquals("1+1/x", cas(m("1+", Frac(m("1"), m("x")))))
    @Test fun nestedFraction() = assertEquals("1/x", cas(m(Frac(m(Frac(m("1"), m("x")), "+1"), m("x+1")))))
    @Test fun keepsProducts() = assertEquals("(a-b)(a+b)", cas(m("(a+b)(a−b)")))
    @Test fun keepsPolynomials() = assertEquals("x^2+2x+1", cas(m("x", sq(), "+2x+1")))
    @Test fun keepsPowers() = assertEquals("(x+1)^10", cas(m("(x+1)", Pow(m("10")))))
    @Test fun factorStaysFactored() = assertEquals("(x-1)(x+1)", cas(m(f("factor", m("x", sq(), "−1")))))
}

class SimplifyStepsTest {
    private fun m(vararg parts: Any): MathRow = MathRow(parts.flatMap { p -> when (p) { is String -> p.map { Sym(it.toString()) }; is Node -> listOf(p); else -> error("") } }.toMutableList())
    private fun sq() = Pow(m("2"))
    private fun f(n: String, vararg a: MathRow) = Func(n, a.toList())

    @Test fun cancelSteps() {
        val r = m(Frac(m("x", sq(), "−1"), m("x−1")))
        org.junit.Assert.assertTrue(com.example.cas.engine.Steps.supports(r))
        val s = com.example.cas.engine.Steps.of(r)!!
        println(s.steps.map { it.title + ": " + it.text })
        assertEquals("Factor and cancel", s.method)
        org.junit.Assert.assertTrue(s.steps[1].text!!.contains("x-1") || s.steps[1].text!!.contains("x−1"))
    }

    @Test fun pythagorasSteps() = assertEquals("Pythagorean identity", com.example.cas.engine.Steps.of(m(f("sin", m("x")), sq(), "+", f("cos", m("x")), sq()))!!.method)
    @Test fun expandSteps() = assertEquals("Expand", com.example.cas.engine.Steps.of(m("(x+1)", sq(), "−x", sq()))!!.method)
    @Test fun nothingToDo() = org.junit.Assert.assertFalse(com.example.cas.engine.Steps.supports(m("x", sq(), "+2x+1")))
}
