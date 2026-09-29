package com.example.cas

import com.example.cas.cas.MathError
import com.example.cas.cas.Printer
import com.example.cas.editor.MathCodec
import com.example.cas.editor.MathRow
import com.example.cas.editor.Pow
import com.example.cas.editor.Sym
import com.example.cas.editor.row
import com.example.cas.engine.Evaluator
import com.example.cas.engine.UserFunction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserFunctionTest {
    private fun r(t: String): MathRow {
        val o = MathRow()
        for (c in t) when (c) { '²' -> o.add(Pow(row("2"))); '³' -> o.add(Pow(row("3"))); else -> o.add(Sym(c.toString())) }
        return o
    }
    private fun define(text: String): Pair<String, UserFunction> = Evaluator().let { ev -> ev.evaluate(r(text)); ev.definedFunction!! }
    private fun with(vararg defs: String) = defs.associate { define(it) }
    private fun cas(x: MathRow, fs: Map<String, UserFunction>) =
        try { Printer.plain(Evaluator(functions = fs).evaluate(x)) } catch (e: MathError) { "Error: " + e.message }

    @Test fun definitionIsRecognised() = assertEquals("f", UserFunction.definition(r("f(x)=x²").items)?.first)
    @Test fun notADefinition() = assertEquals(null, UserFunction.definition(r("f(x+1)=2").items))
    @Test fun callWithANumber() = assertEquals("9", cas(r("f(3)"), with("f(x)=x²")))
    @Test fun callWithAnExpression() = assertEquals("x^2+2x+1", cas(r("f(x+1)"), with("f(x)=x²")).replace("(x+1)^2", "x^2+2x+1"))
    @Test fun derivativeWithPrime() = assertEquals("6x", cas(r("f′′(x)"), with("f(x)=x³")))
    @Test fun firstDerivativeAtAPoint() = assertEquals("12", cas(r("f′(2)"), with("f(x)=x³")))
    @Test fun twoFunctions() = assertEquals("13", cas(r("f(2)+g(3)"), with("f(x)=x²", "g(t)=3t")))
    @Test fun calculatorDefinitionShowsItself() {
        // The := key types one symbol.
        val row = MathRow((r("f(x)").items + Sym(":=") + r("x²").items).toMutableList())
        assertEquals("f(x)=x^2", Printer.plain(Evaluator().evaluate(row)))
    }
    @Test fun letterWithoutDefinitionStillMultiplies() = assertEquals("2x", cas(r("2x"), with("f(x)=x²")))
    @Test fun emptyBodyIsAnError() = assertTrue(runCatching { Evaluator().evaluate(r("f(x)=")) }.exceptionOrNull() is MathError)

    // Functions of several variables, f(x, y, z) = …
    @Test fun threeVariableDefinition() = assertEquals(listOf("x", "y", "z"), UserFunction.definition(r("f(x,y,z)=x+y+z").items)?.second)
    @Test fun threeVariableCall() = assertEquals("6", cas(r("f(1,2,3)"), with("f(x,y,z)=x+y+z")))
    @Test fun argumentsSwapCorrectly() = assertEquals("-1", cas(r("f(2,1)").let { r("g(1,2)") }, with("g(x,y)=x−y")))
    @Test fun swappedLettersSubstituteAtOnce() = assertEquals("y-x", cas(r("g(y,x)"), with("g(x,y)=x−y")).replace("-x+y", "y-x"))
    @Test fun wrongNumberOfValues() = assertEquals("Error: f takes 3 values", cas(r("f(1,2)"), with("f(x,y,z)=x+y+z")))
    @Test fun alreadyDefinedMeansEquation() {
        // With f defined, "f(x,y,z)=4" is an equation using it, not a new definition.
        val fs = with("f(x,y,z)=x²+y²+z²")
        val e = Evaluator(functions = fs).evaluate(r("f(x,y,z)=4"))
        assertEquals("x^2+y^2+z^2=4", Printer.plain(e))
    }

    // \mathcal and \mathfrak letters: stored as Unicode maths letters, usable as variables.
    @Test fun calligraphicLetters() {
        assertEquals("\uD835\uDC9C", com.example.cas.editor.MathAlphabets.calligraphic('A'))   // 𝒜
        assertEquals("\u212C", com.example.cas.editor.MathAlphabets.calligraphic('B'))          // ℬ
    }
    @Test fun frakturLetters() {
        assertEquals("\u212D", com.example.cas.editor.MathAlphabets.fraktur('C'))               // ℭ
        assertEquals("\uD835\uDD1E", com.example.cas.editor.MathAlphabets.fraktur('a'))        // 𝔞
    }
    @Test fun decodesBack() {
        for (c in 'A'..'Z') {
            assertEquals(com.example.cas.editor.MathAlphabets.Style.Calligraphic to c, com.example.cas.editor.MathAlphabets.decode(com.example.cas.editor.MathAlphabets.calligraphic(c).codePointAt(0)))
            assertEquals(com.example.cas.editor.MathAlphabets.Style.Fraktur to c, com.example.cas.editor.MathAlphabets.decode(com.example.cas.editor.MathAlphabets.fraktur(c).codePointAt(0)))
        }
    }
    @Test fun mathLettersAreVariables() {
        val g = com.example.cas.editor.MathAlphabets.fraktur('g')
        val row = MathRow(mutableListOf(Sym("2"), Sym(g), Sym("+"), Sym(g)))
        assertEquals("3" + g, Printer.plain(Evaluator().evaluate(row)))
    }
    @Test fun mathLettersInLatex() {
        val row = MathRow(mutableListOf(Sym(com.example.cas.editor.MathAlphabets.calligraphic('L'))))
        assertEquals("\\mathcal{L}", com.example.cas.engine.Latex.of(row))
        assertEquals(MathCodec.encode(row), MathCodec.encode(com.example.cas.engine.LatexParser.parse("\\mathcal{L}")))
    }
}
