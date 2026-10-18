package com.example.cas

import com.example.cas.editor.Editor
import com.example.cas.editor.MathCodec
import com.example.cas.engine.Formatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorSelectionTest {
    private fun typed(s: String) = Editor().also { e -> s.forEach { e.type(it.toString()) } }

    @Test fun selectCutPaste() {
        val e = typed("12+34")
        e.select(e.root, 0, 2)
        assertTrue(e.hasSelection)
        assertEquals("12", Formatter.plain(e.selected()))
        val clip = MathCodec.encode(e.selected())
        assertTrue(e.deleteSelection())
        assertEquals("+34", Formatter.plain(e.root))
        e.setCursor(e.root, e.root.items.size)
        e.insertRow(MathCodec.decode(clip))
        assertEquals("+3412", Formatter.plain(e.root))
        // The original stays intact: copying doesn't move its nodes.
        e.selectAll(); e.selected()
        assertEquals("+3412", Formatter.plain(e.root))
    }

    @Test fun arrowsExtendTheSelection() {
        val e = typed("abc")
        e.select(e.root, 0, 1)
        e.moveRight()
        assertEquals("ab", Formatter.plain(e.selected()))
        e.clearSelection()
        assertFalse(e.hasSelection)
    }

    @Test fun wordsAndAcrossStructures() {
        val e = typed("12+")
        e.insertFraction(); "34".forEach { e.type(it.toString()) }; e.moveRight(); e.type("5")
        e.setCursor(e.root, e.root.items.size); "+67".forEach { e.type(it.toString()) }
        val root = e.root
        // Holding a digit takes the whole number.
        e.selectWord(root, 1)
        assertEquals("12", Formatter.plain(e.selected()))
        // From "1" to inside the numerator: the fraction comes whole.
        val frac = root.items[3] as com.example.cas.editor.Frac
        e.selectBetween(root, 1, frac.num, 1)
        assertEquals(1, e.selectionStart); assertEquals(4, e.selectionEnd)
        // Numerator to denominator: just the fraction.
        e.selectBetween(frac.num, 0, frac.den, 1)
        assertEquals(3, e.selectionStart); assertEquals(4, e.selectionEnd)
        // Within the numerator: only there.
        e.selectBetween(frac.num, 0, frac.num, 1)
        assertTrue(e.row === frac.num)
        assertEquals("3", Formatter.plain(e.selected()))
        // Backwards: the anchor stays on the first place's side.
        e.selectBetween(root, 7, frac.den, 0)
        assertEquals(3, e.selectionStart); assertEquals(7, e.selectionEnd); assertEquals(7, e.anchor)
    }

    @Test fun latexRoundTrip() {
        val e = typed("1+")
        e.insertFraction(); e.type("x"); e.moveRight(); e.type("2")
        e.selectAll()
        val latex = com.example.cas.engine.Latex.of(e.selected())
        assertTrue(latex, latex.contains("\\frac"))
        val back = com.example.cas.engine.LatexParser.parse(latex)
        assertEquals(MathCodec.encode(e.selected()), MathCodec.encode(back))
    }
}
