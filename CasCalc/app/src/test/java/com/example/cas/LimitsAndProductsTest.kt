package com.example.cas

import com.example.cas.cas.MathError
import com.example.cas.cas.Printer
import com.example.cas.editor.Editor
import com.example.cas.editor.Func
import com.example.cas.editor.MathCodec
import com.example.cas.editor.Matrix
import com.example.cas.engine.Evaluator
import com.example.cas.engine.LatexParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LimitsAndProductsTest {
    private fun lim(latex: String): String = try {
        Printer.plain(Evaluator().evaluate(LatexParser.parse(latex)))
    } catch (e: MathError) { "Error: " + e.message }

    @Test fun fractionalPowerOfAProduct() =
        assertEquals("1/6", lim("\\lim_{x\\to 0^{+}}\\left(\\frac{x-\\sin\\left(x\\right)}{(x\\sin\\left(x\\right))^{\\frac{3}{2}}}\\right)"))

    @Test fun evenPowerUnderARootTakesTheSide() {
        assertEquals("1/6", lim("\\lim_{x\\to 0^{+}}\\frac{x-\\sin(x)}{(x^2)^{\\frac{3}{2}}}"))
        assertEquals("-1/6", lim("\\lim_{x\\to 0^{-}}\\frac{x-\\sin(x)}{(x^2)^{\\frac{3}{2}}}"))
        assertEquals("Error: The limits from the left and right differ", lim("\\lim_{x\\to 0}\\frac{x-\\sin(x)}{(x^2)^{\\frac{3}{2}}}"))
    }

    @Test fun rootInTheDenominator() = assertEquals("0", lim("\\lim_{x\\to 0^{+}}\\frac{\\sin(x)}{\\sqrt{x}}"))

    @Test fun earlierLimitsStillWork() {
        assertEquals("1/4", lim("\\lim_{x\\to 4}\\frac{\\sqrt{x}-2}{x-4}"))
        assertEquals("1/2", lim("\\lim_{x\\to 0^{+}}\\frac{\\sqrt{1+x}-1}{x}"))
        assertEquals("0", lim("\\lim_{x\\to 0^{+}}x\\ln(x)"))
    }

    /** The dot product key right after a matrix makes it the first operand: [..]·□, not □·□. */
    @Test fun productKeyTakesTheMatrixBefore() {
        val ed = Editor()
        val m = Matrix(2, 1)
        ed.insert(m)
        ed.insert(Func("dot", 2), 0)
        val dot = ed.root.items.single() as Func
        assertSame(m, dot.args[0].items.single())
        assertSame(dot.args[1], ed.row)
        assertEquals(0, ed.index)
    }

    @Test fun productKeyTakesALetterOrBrackets() {
        val ed = Editor()
        "2u".forEach { ed.type(it.toString()) }
        ed.insert(Func("cross", 2), 0)
        assertEquals("'2;fn:cross{'u;|}", MathCodec.encode(ed.root))
        val ed2 = Editor()
        "(a+b)".forEach { ed2.type(it.toString()) }
        ed2.insert(Func("kron", 2), 0)
        assertEquals("fn:kron{'(;'a;'+;'b;');|}", MathCodec.encode(ed2.root))
    }

    @Test fun productKeyBeforeAMatrixTakesItAsTheSecond() {
        val ed = Editor()
        val m = Matrix(2, 1)
        ed.insert(m)
        ed.moveLeft(); ed.moveLeft(); ed.moveLeft()   // out to the start, before the matrix
        while (ed.row !== ed.root || ed.index != 0) ed.moveLeft()
        ed.insert(Func("cross", 2), 0)
        val cross = ed.root.items.single() as Func
        assertSame(m, cross.args[1].items.single())
        assertSame(cross.args[0], ed.row)
    }

    @Test fun productKeyAfterAnOperatorStaysEmpty() {
        val ed = Editor()
        "2+".forEach { ed.type(it.toString()) }
        ed.insert(Func("dot", 2), 0)
        assertEquals("'2;'+;fn:dot{|}", MathCodec.encode(ed.root))
    }
}
