package com.example.cas

import com.example.cas.cas.MathError
import com.example.cas.cas.Printer
import com.example.cas.editor.*
import com.example.cas.engine.Evaluator
import com.example.cas.engine.Steps
import org.junit.Assert.assertEquals
import org.junit.Test

/** d/dx of an equation: implicit differentiation, y as y(x). */
class ImplicitTest {
    private fun r(text: String): MathRow { val out = MathRow(); for (c in text) out.add(when (c) { '²' -> Pow(row("2")); '³' -> Pow(row("3")); else -> Sym(c.toString()) }); return out }
    private fun r(vararg parts: Any): MathRow = MathRow(parts.flatMap { p -> if (p is String) r(p).items else listOf(p as Node) }.toMutableList())
    private fun d(body: MathRow, at: String = "", order: String = "", partial: Boolean = false) =
        r(Derivative(r("x"), body, if (at.isEmpty()) MathRow() else r(at), if (order.isEmpty()) MathRow() else r(order), partial))
    private fun eval(row: MathRow, solve: Boolean = false) = try { Printer.plain(Evaluator(solveEquations = solve).evaluate(row)).replace(" ", "") } catch (e: MathError) { "Error: ${e.message}" }

    @Test fun circle() = assertEquals("y′=-x/y", eval(d(r("x²+y²=25"))))
    @Test fun circleAtPoint() = assertEquals("y′=-3/4", eval(d(r("x²+y²=25"), at = "(3,4)")))
    @Test fun onEnterToo() = assertEquals("y′=-x/y", eval(d(r("x²+y²=25")), solve = true))
    @Test fun keypadPartialKey() = assertEquals("y′=-x/y", eval(d(r("x²+y²=25"), partial = true)))
    @Test fun secondOrder() = assertEquals("y′′=-(x^2+y^2)/y^3", eval(d(r("x²+y²=25"), order = "2")))
    @Test fun productInside() = assertEquals(true, eval(d(r("xy=1"))).let { it == "y′=-y/x" })
    @Test fun otherLetter() = assertEquals("t′=-x/t", eval(d(r("x²+t²=1"))))
    @Test fun noSecondLetter() = assertEquals(true, eval(d(r("x²=4"))).startsWith("Error"))
    @Test fun ordinaryDerivativeUnchanged() = assertEquals("2x", eval(d(r("x²"))))

    @Test fun steps() {
        val s = Steps.of(d(r("x²+y²=25")))!!
        assertEquals("Implicit differentiation", s.method)
        assertEquals(true, s.steps.any { it.title == "Differentiate both sides" })
        assertEquals(true, Steps.supports(d(r("x²+y²=25"))))
    }
}
