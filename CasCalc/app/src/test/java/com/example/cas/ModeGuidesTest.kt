package com.example.cas

import com.example.cas.cas.Printer
import com.example.cas.editor.MathCodec
import com.example.cas.engine.Evaluator
import com.example.cas.ui.ModeGuides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeGuidesTest {
    @Test fun everyExampleSurvivesSavingAndCopying() {
        for (g in ModeGuides.all) for (e in g.examples) {
            val code = MathCodec.encode(e.row)
            assertEquals(code, MathCodec.encode(MathCodec.decode(code)))
        }
    }

    /** The calculator's examples give what their notes promise. */
    @Test fun calculatorExamplesWork() {
        val ev = Evaluator()
        val got = ModeGuides.calculator.examples.associate { it.label to Printer.plain(ev.evaluate(MathCodec.copy(it.row))) }
        assertEquals("2√2", got["Exact roots"])
        assertEquals("1/2", got["Fractions"])
        assertEquals("1/2", got["Trigonometry"])
        assertEquals("x=2, x=3", got["Solve"])
        assertEquals("1", got["Limits"])
        assertTrue(got.getValue("Sums"), got.getValue("Sums").contains("n"))
        println(got)
    }

    @Test fun everyGuideHasContent() {
        assertEquals(4, ModeGuides.all.size)
        ModeGuides.all.forEach { assertTrue(it.examples.size >= 5 && it.gestures.isNotEmpty()) }
    }
}
