package com.example.cas

import com.example.cas.cas.MathError
import com.example.cas.cas.Printer
import com.example.cas.editor.Func
import com.example.cas.editor.Matrix
import com.example.cas.editor.MathRow
import com.example.cas.editor.Node
import com.example.cas.editor.Pow
import com.example.cas.editor.Sym
import com.example.cas.editor.row
import com.example.cas.engine.ChooseUnknowns
import com.example.cas.engine.Evaluator
import org.junit.Assert.assertEquals
import org.junit.Test

/** The calculator's Enter on an equation solves it. */
class SolveOnEnterTest {
    private fun solve(r: MathRow, vararg forVars: String): String = try {
        Printer.plain(Evaluator(solveEquations = true).also { if (forVars.isNotEmpty()) it.solveFor = forVars.toList() }.evaluate(r))
    } catch (e: ChooseUnknowns) { "Choose ${e.count} of ${e.candidates}" } catch (e: MathError) { "Error: " + e.message }

    /** A row from a compact string; ² ³ are exponents, ′ a prime. */
    private fun r(text: String): MathRow {
        val out = MathRow()
        for (c in text) out.add(when (c) { '²' -> Pow(row("2")); '³' -> Pow(row("3")); else -> Sym(c.toString()) })
        return out
    }
    private fun r(vararg parts: Any): MathRow = MathRow(parts.flatMap { p -> if (p is String) r(p).items else listOf(p as Node) }.toMutableList())
    private fun mat(rows: Int, cols: Int, vararg cells: String) = Matrix(rows, cols, cells.map { r(it) })

    @Test fun cubic() = assertEquals("x=∛2", solve(r("x³+1=3")).replace(" ", "").substringBefore(","))
    @Test fun quadraticTwoRoots() = assertEquals("x=-3, x=3", solve(r("x²=9")))
    @Test fun linear() = assertEquals("x=4", solve(r("2x+3=11")))
    @Test fun otherLetter() = assertEquals("t=5", solve(r("2t=10")))
    @Test fun exponential() = assertEquals("x=ln(5)", solve(r(Sym("e"), Pow(r("x")), "=5")))
    @Test fun logarithm() = assertEquals("x=e^2", solve(r(Func("ln", listOf(r("x"))), "=2")))
    @Test fun inequality() = assertEquals("x<3", solve(r("2x+1<7")))
    @Test fun inequalityBothSides() = assertEquals("-2<x<2", solve(r("x²<4")))

    @Test fun twoLettersAsks() = assertEquals("Choose 1 of [x, y]", solve(r("y=2x+1")))
    @Test fun twoLettersForY() = assertEquals("y=2x+1", solve(r("y=2x+1"), "y").replace(" ", ""))
    @Test fun twoLettersForX() = assertEquals("x=(y-1)/2", solve(r("y=2x+1"), "x").replace(" ", ""))

    @Test fun system() = assertEquals("x=2, y=1", solve(r("x+y=3,x−y=1")))
    @Test fun systemOfThree() = assertEquals("x=1, y=2, z=3", solve(r("x+y+z=6,2x−y+z=3,x+2y−z=2")))
    @Test fun underdeterminedAsks() = assertEquals("Choose 2 of [x, y, z]", solve(r("x+y+z=6,x−y=0")))
    @Test fun underdeterminedChosen() = assertEquals("x=-z/2+3, y=-z/2+3", solve(r("x+y+z=6,x−y=0"), "x", "y").replace(" ", "").replace(",", ", "))
    @Test fun nonlinearSystem() = assertEquals(true, solve(r("x²+y²=25,y=x+1")).let { "x=3" in it && "y=4" in it })

    @Test fun matrixEquation() = assertEquals("x=2, y=1", solve(r(mat(2, 2, "1", "1", "1", "−1"), "×", mat(2, 1, "x", "y"), "=", mat(2, 1, "3", "1"))))
    @Test fun matrixSizesMustMatch() = assertEquals(true, solve(r(mat(2, 1, "x", "y"), "=", mat(3, 1, "1", "2", "3"))).startsWith("Error"))

    @Test fun differentialEquation() = assertEquals("y=C1e^x", solve(r("y′=y")).replace(" ", "").replace("_", ""))
    @Test fun differentialWithCondition() = assertEquals("y=e^(2x)", solve(r("y′=2y,y(0)=1")).replace(" ", ""))
    @Test fun secondOrder() = assertEquals(true, solve(r("y′′+y=0")).let { "cos" in it && "sin" in it })

    // What isn't an equation to solve stays as it was.
    @Test fun noUnknowns() = assertEquals("5=5", solve(r("2+3=5")))
    @Test fun assignmentStillStores() = assertEquals("a=5", Printer.plain(Evaluator(solveEquations = true).evaluate(r(Sym("a"), Sym(":="), Sym("5")))))
    @Test fun functionDefinitionStillDefines() = assertEquals(true, Evaluator(solveEquations = true).let { ev -> ev.evaluate(r("f(x)=x²")); ev.definedFunction != null })
    @Test fun functionsOfListsStayValues() = assertEquals("5", solve(r(Func("mean", listOf(r("2,4,9"))))))
    @Test fun offOutsideTheCalculator() = assertEquals("x^2=9", Printer.plain(Evaluator().evaluate(r("x²=9"))))

    // Steps (long-press Enter) for each kind.
    private fun method(r: MathRow, vararg forVars: String) = com.example.cas.engine.Steps.of(r, solveFor = forVars.toList().ifEmpty { null })?.method
    @Test fun stepsLinear() = assertEquals("Linear equation", method(r("2x+3=11")))
    @Test fun stepsQuadratic() = assertEquals("Quadratic equation", method(r("x²−5x+6=0")))
    @Test fun stepsQuadraticHasDiscriminant() = assertEquals(true, com.example.cas.engine.Steps.of(r("x²−5x+6=0"))!!.steps.any { it.title == "Discriminant" })
    @Test fun stepsCubicFactors() = assertEquals(true, com.example.cas.engine.Steps.of(r("x³−x=0"))!!.steps.any { it.title == "Factor" })
    @Test fun stepsTranscendental() = assertEquals("Equation", method(r(Func("sin", listOf(r("x"))), "=1")))
    @Test fun stepsSystem() = assertEquals("System of equations", method(r("x+y=3,x−y=1")))
    @Test fun stepsInequality() = assertEquals("Inequality", method(r("2x+1<7")))
    @Test fun stepsFollowTheChoice() = assertEquals(true, com.example.cas.engine.Steps.of(r("y=2x+1"), solveFor = listOf("x"))!!.answer.items.any { (it as? Sym)?.text == "x" })
    @Test fun stepsDifferential() = assertEquals(true, com.example.cas.engine.Steps.of(r("y′=2y,y(0)=1")) != null)
    @Test fun solvedForReadsTheAnswer() = assertEquals(listOf("x", "y"), com.example.cas.engine.Steps.solvedFor(Evaluator(solveEquations = true).evaluate(r("x+y=3,x−y=1"))))

    // The derivative button (d/dx of y) works like the y′ keys.
    private fun dd(y: String, v: String = "x", order: String = "") = com.example.cas.editor.Derivative(r(v), r(y), MathRow(), if (order.isEmpty()) MathRow() else r(order))
    // Exactly what the keypad's derivative key inserts: ∂/∂x, filled in.
    @Test fun keypadDerivativeKey() = assertEquals("y=e^(2x)", solve(r(com.example.cas.editor.Derivative(r("x"), r("y"), partial = true), "=2y,y(0)=1")).replace(" ", ""))
    @Test fun keypadDerivativeKeyInTheta() = assertEquals(true, solve(r(com.example.cas.editor.Derivative(r("θ"), r("y"), partial = true), "=y")).contains("θ"))
    // The steps work in t, as the answer does: no x anywhere in them.
    @Test fun stepsInT() = com.example.cas.engine.Steps.of(r(dd("y", "t"), "=−y,y(0)=3"))!!.let { sol ->
        val tex = sol.steps.mapNotNull { st -> st.math?.let { com.example.cas.engine.Latex.of(it) } } + sol.steps.mapNotNull { it.text }
        assertEquals(true, tex.any { "t" in it.replace("\\text", "").replace("\\left", "").replace("\\right", "") })
        assertEquals(listOf<String>(), tex.filter { Regex("(?<![a-z\\\\])x").containsMatchIn(it.replace("\\exp", "")) })
    }
    @Test fun derivativeButton() = assertEquals("y=C1e^(2x)", solve(r(dd("y"), "=2y")).replace(" ", "").replace("_", ""))
    @Test fun derivativeButtonWithCondition() = assertEquals("y=e^(2x)", solve(r(dd("y"), "=2y,y(0)=1")).replace(" ", ""))
    @Test fun secondDerivativeButton() = assertEquals(true, solve(r(dd("y", order = "2"), "+y=0")).let { "cos" in it && "sin" in it })
    @Test fun mixedButtonAndPrime() = assertEquals(true, solve(r(dd("y", order = "2"), "−3y′+2y=0")).let { "e^x" in it.replace(" ", "") && "e^(2x)" in it.replace(" ", "") })
    @Test fun derivativeInT() = assertEquals("y=3e^(-t)", solve(r(dd("y", "t"), "=−y,y(0)=3")).replace(" ", ""))
    @Test fun derivativeOfAnExpressionIsNotAnOde() = assertEquals("x=2", solve(r(com.example.cas.editor.Derivative(r("x"), r("x²")), "=4")))
    @Test fun derivativeButtonSteps() = assertEquals(true, com.example.cas.engine.Steps.of(r(dd("y"), "=2y,y(0)=1")) != null)
}
