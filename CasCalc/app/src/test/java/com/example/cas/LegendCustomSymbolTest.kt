package com.example.cas

import com.example.cas.cas.Accent
import com.example.cas.cas.CustomSymbol
import com.example.cas.editor.MathRow
import com.example.cas.editor.Sym
import com.example.cas.graph.Legend
import com.example.cas.graph.MathScene
import com.example.cas.graph.Scene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Built symbols in an exported legend: set as math, not written out as x̂_1^2. */
class LegendCustomSymbolTest {
    private fun labels(symbol: CustomSymbol, vararg after: String): List<Scene.Label> {
        val row = MathRow(mutableListOf<com.example.cas.editor.Node>(Sym(symbol.encode())).also { l -> after.forEach { l += Sym(it) } })
        // The default name goes through LaTeX and back, as a renamed line's does.
        val name = Legend.row(Legend.defaultSource(row, isData = false))
        val scene = Scene(200.0, 100.0, 0)
        MathScene.layout(name, 10.0, 0).let { it.draw(scene, 0.0, 50.0) }
        return scene.items.filterIsInstance<Scene.Label>()
    }

    @Test fun scriptsAreRaisedAndLowered() {
        val l = labels(CustomSymbol("x", Accent.Hat, "1", "2"))
        assertFalse(l.joinToString("") { it.text }, l.any { '_' in it.text || '^' in it.text })
        val x = l.first { it.text == "x" }
        val sub = l.first { it.text == "1" }
        val sup = l.first { it.text == "2" }
        assertEquals(Scene.Font.Italic, x.font)
        assertTrue(sub.y > x.y && sub.size < x.size)
        assertTrue(sup.y < x.y && sup.size < x.size)
        assertTrue(sub.x > x.x && sup.x > x.x)
        // The hat over the x.
        val hat = l.first { it.text == Accent.Hat.glyph }
        assertTrue(hat.x >= x.x && hat.x < sub.x)
    }

    @Test fun prescriptsBeforeAndUprightLetter() {
        val l = labels(CustomSymbol("C", preSub = "6", preSup = "14", upright = "u"))
        val c = l.first { it.text == "C" }
        assertEquals(Scene.Font.Roman, c.font)
        assertTrue(l.filter { it.text in setOf("1", "4", "6") }.all { it.x < c.x })
    }

    @Test fun flatSpansToo() {
        val spans = Legend.spans(Legend.row("$" + CustomSymbol("v", sub = "0").latex + "$"))
        assertFalse(spans.any { '_' in it.text })
        assertTrue(spans.any { it.text == "0" && it.shift == -1 })
    }

    @Test fun boldAndRingSymbolsSurviveLatex() {
        for (c in listOf(CustomSymbol("F", bold = true, sub = "net"), CustomSymbol("x", Accent.Hat, bold = true), CustomSymbol("A", Accent.Ring), CustomSymbol("d", Accent.Bar, upright = "u"))) {
            val back = com.example.cas.engine.LatexParser.parse(c.latex).items.single() as Sym
            assertEquals(c.latex, c, CustomSymbol.decode(back.text))
        }
    }
}
