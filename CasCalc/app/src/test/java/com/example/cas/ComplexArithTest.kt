package com.example.cas

import com.example.cas.cas.MathError
import com.example.cas.cas.Numeric
import com.example.cas.cas.Printer
import com.example.cas.editor.Frac
import com.example.cas.editor.Func
import com.example.cas.editor.MathRow
import com.example.cas.editor.Node
import com.example.cas.editor.Pow
import com.example.cas.editor.Sqrt
import com.example.cas.editor.Sym
import com.example.cas.engine.Evaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Constants with i in them come out as a + bi, exactly. */
class ComplexArithTest {
    private fun m(vararg parts: Any): MathRow = MathRow(parts.flatMap { p -> when (p) { is String -> p.map { Sym(it.toString()) }; is Node -> listOf(p); else -> error("") } }.toMutableList())
    private fun cas(r: MathRow) = try { Printer.plain(Evaluator().evaluate(r)) } catch (e: MathError) { "Error: " + e.message }
    private fun value(r: MathRow) = Numeric.eval(Evaluator().evaluate(r))

    @Test fun products() = assertEquals("5+5i", cas(m("(1+2i)(3−i)")))
    @Test fun squares() = assertEquals("2i", cas(m("(1+i)", Pow(m("2")))))
    @Test fun highPowers() = assertEquals("16", cas(m("(1+i)", Pow(m("8")))))
    @Test fun reciprocal() = assertEquals("1/2-i/2", cas(m(Frac(m("1"), m("1+i")))))
    @Test fun quotient() = assertEquals("1+i", cas(m(Frac(m("3+i"), m("2−i")))))
    @Test fun negativePower() = assertEquals("-i", cas(m("i", Pow(m("−1")))))
    @Test fun euler() = assertEquals("0", cas(m("e", Pow(m("iπ")), "+1")))
    @Test fun eulerQuarter() = assertEquals("√2/2+i√2/2", cas(m("e", Pow(m(Frac(m("iπ"), m("4")))))))
    @Test fun iToTheI() = assertEquals("e^(-π/2)", cas(m("i", Pow(m("i")))))
    @Test fun withSurds() = assertEquals("-2+2i√3", cas(m("(1+", Sqrt(m("3")), "i)", Pow(m("2")))))
    @Test fun moduloSurds() {
        val v = value(m("(", Sqrt(m("2")), "+i)", Pow(m("3"))))
        assertTrue(abs(v.re - (-0.0 + 2 * Math.sqrt(2.0) - 3 * Math.sqrt(2.0))) < 1e-12)
    }
    @Test fun exactLeftAlone() = assertEquals("e^i", cas(m("e", Pow(m("i")))))
    @Test fun variablesUntouched() = assertEquals("x+2i", cas(m("x+(1+i)", Pow(m("2")))))
    @Test fun divideByZero() = assertTrue(cas(m(Frac(m("1"), m("i−i")))).startsWith("Error"))
    @Test fun absStillWorks() = assertEquals("5", cas(m(Func("abs", listOf(m("3+4i"))))))
}
