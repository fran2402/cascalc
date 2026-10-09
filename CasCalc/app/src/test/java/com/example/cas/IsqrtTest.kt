package com.example.cas

import com.example.cas.cas.Printer
import com.example.cas.editor.*
import com.example.cas.engine.Evaluator
import org.junit.Assert.assertEquals
import org.junit.Test

/** isqrt and issquare without BigInteger.sqrt (which needs Android 13). */
class IsqrtTest {
    private fun ev(name: String, arg: String) = Printer.plain(Evaluator().evaluate(MathRow(mutableListOf(Func(name, listOf(row(arg)))))))
    @Test fun values() {
        for ((n, r) in listOf("0" to "0", "1" to "1", "15" to "3", "16" to "4", "17" to "4", "99999999999999999999" to "9999999999", "100000000000000000000" to "10000000000"))
            assertEquals(n, r, ev("isqrt", n))
        assertEquals("1", ev("issquare", "144")); assertEquals("0", ev("issquare", "145"))
    }
}
