package com.example.cas

import com.example.cas.cas.MathError
import com.example.cas.cas.Numeric
import com.example.cas.cas.Printer
import com.example.cas.editor.Func
import com.example.cas.editor.MathRow
import com.example.cas.editor.Sym
import com.example.cas.editor.row
import com.example.cas.engine.Evaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Complex values that have a closed form get it, rather than staying unevaluated (ln(−2) = ln 2 + πi). */
class ComplexExactTest {
    private fun r(t: String): MathRow { val o = MathRow(); for (c in t) o.add(Sym(c.toString())); return o }
    private fun cas(x: MathRow) = try { Printer.plain(Evaluator().evaluate(x)) } catch (e: MathError) { "Error: " + e.message }
    private fun f(name: String, a: String) = row(Func(name, listOf(r(a))))
    /** The exact answer's numerical value matches Python's cmath. */
    private fun near(x: MathRow, re: Double, im: Double) {
        val v = Numeric.eval(Evaluator().evaluate(x))
        assertTrue("${Printer.plain(Evaluator().evaluate(x))} = $v, want $re + ${im}i", abs(v.re - re) < 1e-12 && abs(v.im - im) < 1e-12)
    }

    @Test fun lnOfNegative() = assertEquals("ln(2)+πi", cas(f("ln", "−2")))
    @Test fun lnOfMinusOne() = assertEquals("πi", cas(f("ln", "−1")))
    @Test fun lnOfI() = assertEquals("πi/2", cas(f("ln", "i")))
    @Test fun lnOfOnePlusI() = near(f("ln", "1+i"), 0.34657359027997264, 0.7853981633974483)
    @Test fun lnThirdQuadrant() = near(f("ln", "−1−i"), 0.34657359027997264, -2.356194490192345)
    @Test fun argIsExact() = assertEquals("3π/4", cas(f("arg", "−1+i")))
    @Test fun argOfNegative() = assertEquals("π", cas(f("arg", "−5")))
    @Test fun asinOutsideRange() = near(f("asin", "2"), 1.5707963267948966, 1.3169578969248166)
    @Test fun asinNegativeOutsideRange() = near(f("asin", "−2"), -1.5707963267948966, 1.3169578969248166)
    @Test fun acosOutsideRange() = near(f("acos", "2"), 0.0, -1.3169578969248166)
    @Test fun acosNegativeOutsideRange() = near(f("acos", "−2"), Math.PI, -1.3169578969248166)
    @Test fun asinInsideRangeUnchanged() = assertEquals("π/6", cas(row(Func("asin", listOf(row(com.example.cas.editor.Frac(r("1"), r("2"))))))))
    @Test fun complexSqrt() = assertEquals("2+i", cas(row(com.example.cas.editor.Sqrt(r("3+4i")))))
    @Test fun complexSqrtNegativeImaginary() = assertEquals("2-i", cas(row(com.example.cas.editor.Sqrt(r("3−4i")))))
    @Test fun lnOfPositiveUnchanged() = assertEquals("ln(2)", cas(f("ln", "2")))
    @Test fun hugeNumbersAreShownApproximately() {
        val v = Evaluator().evaluate(row(Sym("2"), com.example.cas.editor.Pow(r("20000"))))
        val a = com.example.cas.engine.Formatter.answer(v)
        assertTrue(a.isApproximate)
    }
    @Test fun absurdPowersStop() = assertEquals("Error: That number is too large", cas(row(Sym("7"), com.example.cas.editor.Pow(r("9999999")))))

    @Test fun negatedSumKeepsBrackets() {
        val a = com.example.cas.cas.Sym("a"); val b = com.example.cas.cas.Sym("b")
        val neg = com.example.cas.cas.Mul(listOf(com.example.cas.cas.MINUS_ONE, com.example.cas.cas.Add(listOf(a, b))))
        assertEquals("-(a+b)", Printer.plain(neg))
        assertEquals("x-(a+b)", Printer.plain(com.example.cas.cas.Add(listOf(com.example.cas.cas.Sym("x"), neg))))
        assertEquals("'−;'(;'a;'+;'b;');", com.example.cas.editor.MathCodec.encode(com.example.cas.engine.Formatter.row(neg)))
    }

    @Test fun emptyMatrixCellsAreZero() = assertEquals("[[1,0],[0,2]]", cas(row(com.example.cas.editor.Matrix(2, 2, listOf(r("1"), MathRow(), MathRow(), r("2"))))))
}
