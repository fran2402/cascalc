package com.example.cas

import com.example.cas.cas.Accent
import com.example.cas.cas.CustomSymbol
import com.example.cas.editor.MathAlphabets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SymbolBuilderTest {
    @Test fun oldSymbolsReadTheSame() {
        // Saved before scripts could go in front: four parts.
        val old = CustomSymbol.MARK + "x" + CustomSymbol.SEP + "Hat" + CustomSymbol.SEP + "1" + CustomSymbol.SEP + ""
        assertEquals(CustomSymbol("x", Accent.Hat, "1"), CustomSymbol.decode(old))
        assertEquals(old, CustomSymbol("x", Accent.Hat, "1").encode())
    }

    @Test fun scriptsBeforeAndBold() {
        val carbon = CustomSymbol("C", preSub = "6", preSup = "14")
        assertEquals(carbon, CustomSymbol.decode(carbon.encode()))
        assertEquals("{}_{6}^{14}C", carbon.latex)
        assertEquals("\\vec{\\boldsymbol{v}}", CustomSymbol("v", Accent.Vector, bold = true).latex)
    }

    @Test fun fromLatex() {
        assertEquals(CustomSymbol("x", Accent.Hat, "1", "2"), CustomSymbol.fromLatex("\\hat{x}_{1}^{2}"))
        assertEquals(CustomSymbol("C", preSub = "6", preSup = "14"), CustomSymbol.fromLatex("{}^{14}_{6}C"))
        assertEquals(CustomSymbol("ℵ", sub = "0"), CustomSymbol.fromLatex("\\aleph_0"))
        assertEquals(CustomSymbol(MathAlphabets.doubleStruck('R')), CustomSymbol.fromLatex("\\mathbb{R}"))
        assertEquals(CustomSymbol("θ", Accent.Dot, sup = "′"), CustomSymbol.fromLatex("\\dot{\\theta}'"))
        assertEquals(CustomSymbol("v", Accent.Vector, bold = true), CustomSymbol.fromLatex("\\vec{\\boldsymbol{v}}"))
        assertNull(CustomSymbol.fromLatex("x+y"))
        assertNull(CustomSymbol.fromLatex("\\hat{"))
    }

    @Test fun latexRoundTrips() {
        for (s in listOf(CustomSymbol("x", Accent.Ring, "i"), CustomSymbol("Ω", bold = true), CustomSymbol(MathAlphabets.calligraphic('L'), sup = "∗".replace("∗", "*")), CustomSymbol("ϕ", preSup = "2")))
            assertEquals(s, CustomSymbol.fromLatex(s.latex))
    }

    @Test fun textScriptsAreUpright() {
        val eff = CustomSymbol("m", sub = "eff", upright = "s")
        assertEquals("m_{\\text{eff}}", eff.latex)
        assertEquals(eff, CustomSymbol.fromLatex("m_{\\text{eff}}"))
        assertEquals(eff, CustomSymbol.decode(eff.encode()))
        // An upright letter, and a word as the letter.
        assertEquals(CustomSymbol("d", upright = "u"), CustomSymbol.fromLatex("\\mathrm{d}"))
        assertEquals("\\text{max}_{i}", CustomSymbol("max", sub = "i", upright = "u").latex)
        assertEquals(CustomSymbol("max", sub = "i", upright = "u"), CustomSymbol.fromLatex("\\text{max}_i"))
    }

    @Test fun blackboardLetters() {
        assertEquals("ℝ", MathAlphabets.doubleStruck('R'))
        assertEquals(MathAlphabets.Style.DoubleStruck to 'R', MathAlphabets.decode("ℝ".codePointAt(0)))
        assertEquals("\\mathbb{R}", com.example.cas.engine.Latex.of(com.example.cas.engine.LatexParser.parse("\\mathbb{R}")))
    }
}
