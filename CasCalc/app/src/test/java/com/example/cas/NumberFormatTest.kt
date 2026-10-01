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

    private fun shown(e: com.example.cas.cas.Expr) = Formatter.plain(Formatter.shown(Formatter.answer(e), false)).replace(Formatter.THIN_SPACE, "")

    @Test fun longWholeNumbersTurnScientific() {
        val was = Formatter.sciAfter
        try {
            Formatter.sciAfter = 10
            // 2⁶⁴ = 18446744073709551616, exact, has 20 digits.
            assertEquals("1.844674407×10^(19)", shown(Num(Rational.of(java.math.BigInteger.TWO.pow(64)))))
            assertEquals("−1.844674407×10^(19)", shown(Num(Rational.of(java.math.BigInteger.TWO.pow(64).negate()))))
            assertEquals("1234567890", shown(Num(Rational.of(1234567890L))))
            Formatter.sciAfter = 25
            assertEquals("18446744073709551616", shown(Num(Rational.of(java.math.BigInteger.TWO.pow(64)))))
        } finally { Formatter.sciAfter = was }
    }

    @Test fun longFractionsFollowTheSlider() {
        val was = Formatter.sciAfter
        try {
            Formatter.sciAfter = 12
            assertFalse(Formatter.answer(Num(Rational.of(12345678901, 7))).preferApprox)
            Formatter.sciAfter = 5
            assertTrue(Formatter.answer(Num(Rational.of(1, 1234567))).preferApprox)
        } finally { Formatter.sciAfter = was }
    }

    @Test fun scientificAndEngineeringFormats() {
        val was = Formatter.numberFormat
        try {
            Formatter.numberFormat = 1
            assertEquals("1.5×10^3", Formatter.plain(Formatter.row(Flt(1500.0))))
            assertEquals("2.5", Formatter.plain(Formatter.row(Flt(2.5))))
            Formatter.numberFormat = 2
            assertEquals("15×10^3", Formatter.plain(Formatter.row(Flt(15000.0))))
            assertEquals("250×10^(−6)", Formatter.plain(Formatter.row(Flt(0.00025))))
            // Short whole numbers stay as they are.
            assertEquals("120", Formatter.plain(Formatter.row(Num(Rational.of(120L)))))
        } finally { Formatter.numberFormat = was }
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
