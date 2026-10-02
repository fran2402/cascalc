package com.example.cas

import com.example.cas.cas.Printer
import com.example.cas.editor.MathCodec
import com.example.cas.engine.Evaluator
import com.example.cas.ui.ModeGuides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeGuidesTest {
    /** Math in the notes and tips is LaTeX in $ … $, never Unicode in the prose. */
    @Test fun notesUseLatex() {
        for (g in ModeGuides.all) for (t in listOf(g.tagline) + g.examples.map { it.note } + (g.gestures + g.tips).map { it.text }) {
            assertEquals("unbalanced $ in: $t", 0, t.count { it == '$' } % 2)
            for ((isMath, piece) in com.example.cas.engine.LatexParser.inline(t)) {
                if (isMath) assertEquals("unknown LaTeX in: $t", emptySet<String>(), com.example.cas.engine.LatexParser.unknownCommands(piece))
                else assertTrue("Unicode math in prose: $t", piece.none { it in "²³ⁿ√∫∮Σ≤≥′⁻¹½∞∂∇±πθηρφ" })
            }
        }
    }

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
        assertEquals("1", got["Limits"])
        assertEquals("5+5i", got["Complex numbers"])
        assertEquals("0", got["Euler's identity"])
        assertEquals("√π", got["Gamma function"])
        assertEquals("√π", got["Gaussian integral"])
        assertEquals("π^2/6", got["Basel problem"])
        assertEquals("ln(2)", got["Alternating series"])
        assertEquals("1/6", got["Series limit"])
        assertEquals("π/2", got["Dirichlet integral"])
        assertTrue(got.getValue("Sums"), got.getValue("Sums").contains("n"))
        println(got)
    }

    /** Every graph example is read as the kind of line it says, and can be drawn somewhere. */
    @Test fun graphExamplesWork() {
        // (Pairs and lists are split up by the graph before they're read, so only single lines here.)
        val kinds = ModeGuides.graph2D.examples.filter { it.label in setOf("Rose", "Heart", "Folium", "Annulus", "Ripples") }.associate { e ->
            val v = Evaluator().evaluate(MathCodec.copy(e.row))
            e.label to com.example.cas.graph.PlotSpec.classify(listOf(v))::class.simpleName
        }
        assertEquals("Polar", kinds["Rose"])
        assertEquals("Implicit", kinds["Heart"])
        assertEquals("Implicit", kinds["Folium"])
        assertEquals("Region", kinds["Annulus"])
        assertEquals("Field", kinds["Ripples"])
        val kinds3 = ModeGuides.graph3D.examples.filter { it.label in setOf("Saddle", "Ripple", "Torus", "Gyroid") }.associate { e ->
            e.label to com.example.cas.graph.PlotSpec3D.classify(Evaluator().evaluate(MathCodec.copy(e.row)))::class.simpleName
        }
        assertEquals(mapOf("Saddle" to "Explicit", "Ripple" to "Explicit", "Torus" to "Implicit", "Gyroid" to "Implicit"), kinds3)
        for (e in ModeGuides.complex.examples.filter { it.label != "Points" }) Evaluator().evaluate(MathCodec.copy(e.row))
    }

    @Test fun everyGuideHasContent() {
        assertEquals(4, ModeGuides.all.size)
        ModeGuides.all.forEach { assertTrue(it.examples.size >= 5 && it.gestures.isNotEmpty()) }
    }
}
