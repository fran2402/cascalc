package com.example.cas

import com.example.cas.cas.Flt
import com.example.cas.cas.Num
import com.example.cas.engine.Formatter
import com.example.cas.math.Rational
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberFormatTest {
    @Test fun longFractionsShowTheirDecimalFirst() {
        assertTrue(Formatter.answer(Num(Rational.of(12345678901, 7))).preferApprox)
        assertFalse(Formatter.answer(Num(Rational.of(22, 7))).preferApprox)
    }

    @Test fun keepExactWhenAskedTo() {
        val was = Formatter.longExact
        try {
            Formatter.longExact = 0
            assertFalse(Formatter.answer(Num(Rational.of(12345678901, 7))).preferApprox)
        } finally { Formatter.longExact = was }
    }

    @Test fun scientificFromTheChosenSize() {
        val was = Formatter.sciAfter
        try {
            Formatter.sciAfter = 6
            assertEquals("1.234567×10^6", Formatter.plain(Formatter.row(Flt(1234567.0))).replace(Formatter.THIN_SPACE, ""))
            Formatter.sciAfter = 10
            assertEquals("1234567", Formatter.plain(Formatter.row(Flt(1234567.0))).replace(Formatter.THIN_SPACE, ""))
        } finally { Formatter.sciAfter = was }
    }
}
