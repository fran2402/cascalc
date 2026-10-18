package com.example.cas

import com.example.cas.editor.MathCodec
import com.example.cas.engine.Evaluator
import com.example.cas.engine.Formatter
import com.example.cas.engine.PlainMath
import org.junit.Assert.assertEquals
import org.junit.Test

class PlainMathTest {
    private fun value(text: String, vars: Map<String, com.example.cas.cas.Expr> = emptyMap()) =
        Formatter.plain(Formatter.row(Evaluator(variables = vars).evaluate(MathCodec.copy(PlainMath.parse(text)))))

    @Test fun readsTypedMath() {
        assertEquals("6", value("2*3"))
        assertEquals("1500", value("1.5e3"))
        assertEquals("3", value("log(8, 2)"))
        assertEquals("2", value("mean(h)", mapOf("h" to com.example.cas.cas.Mat(3, 1, listOf(1, 2, 3).map { com.example.cas.cas.Flt(it.toDouble()) }))))
        assertEquals("abs(x−1)", value("|x-1|"))
        assertEquals("3sin(x)", value("3 sin x"))
        assertEquals("(1)/(x^2)", value("x^-2"))
        assertEquals("πx", value("pi*x"))
    }

    @Test fun writesWhatItReads() {
        for (t in listOf("sin(x)", "2x^2+1", "sqrt(x)", "(x+1)/(x-1)", "|x-1|", "log(8, 2)", "1+cos(theta)", "[1, 4, 9]", "x^(-2)", "e^(x/2)")) {
            val once = PlainMath.parse(t)
            assertEquals(t, MathCodec.encode(once), MathCodec.encode(PlainMath.parse(PlainMath.text(once))))
        }
    }
}
