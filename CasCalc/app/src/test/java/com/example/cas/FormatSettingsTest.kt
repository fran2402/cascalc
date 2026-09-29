package com.example.cas

import com.example.cas.cas.Flt
import com.example.cas.editor.MathCodec
import com.example.cas.engine.Formatter
import org.junit.Assert.assertEquals
import org.junit.Test

/** Number format and complex form settings, as the formatter draws them. Each test starts from the defaults. */
class FormatSettingsTest {
    private fun defaults() { Formatter.numberFormat = 0; Formatter.polarComplex = false; Formatter.groupDigits = true }
    private fun plain(row: com.example.cas.editor.MathRow) = MathCodec.encode(row).replace("'", "").replace(";", "").replace("\u2009", "")
    private fun shown(d: Double) = plain(Formatter.row(Flt(d)))

    @Test fun autoKeepsOrdinaryNumbersPlain() { defaults(); assertEquals("12345.6", shown(12345.6)) }
    @Test fun autoUsesPowersForHugeNumbers() { defaults(); assertEquals("6.02214076×10pow{23}", shown(6.02214076e23)) }
    @Test fun scientificAlways() { defaults(); Formatter.numberFormat = 1; assertEquals("1.23456×10pow{4}", shown(12345.6)); defaults() }
    @Test fun scientificLeavesOnesAlone() { defaults(); Formatter.numberFormat = 1; assertEquals("7.5", shown(7.5)); defaults() }
    @Test fun engineeringUsesMultiplesOfThree() { defaults(); Formatter.numberFormat = 2; assertEquals("12.3456×10pow{3}", shown(12345.6)); defaults() }
    @Test fun engineeringSmall() { defaults(); Formatter.numberFormat = 2; assertEquals("4.7×10pow{−6}", shown(4.7e-6)); defaults() }
    @Test fun groupingOff() { defaults(); Formatter.groupDigits = false; assertEquals("1234567", plain(Formatter.row(com.example.cas.cas.Num(1234567L)))); defaults() }
    @Test fun polarComplexAnswer() {
        defaults(); Formatter.polarComplex = true
        val a = Formatter.answer(com.example.cas.cas.Algebra.simplify(com.example.cas.cas.add(com.example.cas.cas.ONE, com.example.cas.cas.I)))
        // 1 + i ≈ 1.414213562 e^{i 0.7853981634}
        assertEquals("1.414213562epow{i0.7853981634}", plain(a.approx!!))
        defaults()
    }
}
