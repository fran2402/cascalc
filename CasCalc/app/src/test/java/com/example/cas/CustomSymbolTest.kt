package com.example.cas

import com.example.cas.ui.pinId
import com.example.cas.cas.Accent
import com.example.cas.cas.CustomSymbol
import com.example.cas.cas.Printer
import com.example.cas.editor.MathCodec
import com.example.cas.editor.MathRow
import com.example.cas.editor.Sym
import com.example.cas.engine.Evaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomSymbolTest {
    private val xHat1 = CustomSymbol("x", Accent.Hat, "1", "")
    @Test fun roundTrip() = assertEquals(xHat1, CustomSymbol.decode(xHat1.encode()))
    @Test fun latex() = assertEquals("\\hat{x}_{1}", xHat1.latex)
    @Test fun latexWithEverything() = assertEquals("\\vec{\\mathfrak{g}}_{i}^{2}", CustomSymbol(com.example.cas.editor.MathAlphabets.fraktur('g'), Accent.Vector, "i", "2").latex)
    @Test fun greekBaseAndPrime() = assertEquals("\\dot{\\theta}^{\\prime}", CustomSymbol("θ", Accent.Dot, "", "′").latex)
    @Test fun plainText() = assertEquals("x\u0302_1", xHat1.plain)
    @Test fun survivesTheCodec() {
        val row = MathRow(mutableListOf(Sym(xHat1.encode())))
        assertEquals(MathCodec.encode(row), MathCodec.encode(MathCodec.decode(MathCodec.encode(row))))
    }
    @Test fun isAVariable() {
        // 2 x̂₁ + x̂₁ = 3 x̂₁
        val s = xHat1.encode()
        val v = Evaluator().evaluate(MathRow(mutableListOf(Sym("2"), Sym(s), Sym("+"), Sym(s))))
        assertEquals("3" + xHat1.plain, Printer.plain(v))
    }
    @Test fun differentiateWithRespectToIt() {
        val s = xHat1.encode()
        val d = Evaluator().evaluate(MathRow(mutableListOf(com.example.cas.editor.Derivative(MathRow(mutableListOf(Sym(s))), MathRow(mutableListOf(Sym(s), com.example.cas.editor.Pow(com.example.cas.editor.row("2"))))))))
        assertEquals("2" + xHat1.plain, Printer.plain(d))
    }
    @Test fun differentSymbolsAreDifferentVariables() {
        val a = CustomSymbol("x", Accent.Hat).encode(); val b = CustomSymbol("x", Accent.Tilde).encode()
        val v = Evaluator().evaluate(MathRow(mutableListOf(Sym(a), Sym("−"), Sym(b))))
        assertTrue(Printer.plain(v).contains("-"))
    }

    // LaTeX accents make the same symbols as the builder.
    @Test fun latexAccentsMakeSymbols() {
        val row = com.example.cas.engine.LatexParser.parse("\\hat{x}")
        assertEquals(CustomSymbol("x", Accent.Hat), CustomSymbol.decode((row.items.single() as Sym).text))
    }
    // Valid LaTeX: the scripted base is braced.
    // \hat{x}_1 reads back as the one symbol x̂₁ (not x̂ with a subscript box), so it stays whole in the editor.
    @Test fun latexAccentWithScripts() = assertEquals("\\hat{x}_{1}", com.example.cas.engine.Latex.of(com.example.cas.engine.LatexParser.parse("\\hat{x}_1")))
    @Test fun builtSymbolSurvivesLatex() = assertEquals(xHat1.encode(), (com.example.cas.engine.LatexParser.parse(xHat1.latex).items.single() as com.example.cas.editor.Sym).text)
    @Test fun lettersTabStartsWithTheBuilder() {
        val rows = com.example.cas.ui.letterRows(listOf(xHat1.encode()))
        // The builder and := first, then the symbols you built.
        assertEquals("symbol builder", rows[0][0].spoken)
        assertEquals("store in variable", rows[0][1].spoken)
        assertEquals("saved symbol", rows[0][2].spoken)
    }
    @Test fun pinnedKeysComeAfterTheSpecialKeys() {
        val rows = com.example.cas.ui.letterRows(listOf(xHat1.encode()), pinned = listOf("β", xHat1.encode())).flatten()
        assertEquals(listOf("symbol builder", "store in variable", "beta", "saved symbol", "letter a"), rows.take(5).map { it.spoken })
        // Each key appears once.
        assertEquals(rows.size, rows.distinctBy { it.pinId }.size)
    }
    @Test fun pinnedConstantsComeAfterTheList() {
        val rows = com.example.cas.ui.constantRows(listOf("Speed of light in vacuum")).flatten()
        assertEquals("list of constants with names", rows[0].spoken)
        assertEquals("Speed of light in vacuum", rows[1].spoken)
    }
}
