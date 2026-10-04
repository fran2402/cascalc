package com.example.cas

import com.example.cas.editor.Editor
import com.example.cas.editor.Scripted
import com.example.cas.editor.Sym
import com.example.cas.editor.MathRow
import com.example.cas.graph.Sheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** The cursor stays at a line's ends, skips scripts that aren't drawn; empty brackets delete as one. */
class CursorEdgesTest {
    @Test fun arrowsStopAtTheEnds() {
        val ed = Editor()
        "y=x".forEach { ed.type(it.toString()) }
        ed.insertFraction(); ed.type("1")
        repeat(10) { ed.moveRight() }
        assertSame(ed.root, ed.row); assertEquals(ed.root.items.size, ed.index)
        repeat(20) { ed.moveLeft() }
        assertSame(ed.root, ed.row); assertEquals(0, ed.index)
    }

    @Test fun emptyScriptsArePassedOver() {
        val s = Scripted(MathRow(mutableListOf(Sym("v"))), MathRow(), MathRow(mutableListOf(Sym("2"))))
        val ed = Editor(MathRow(mutableListOf(s, Sym("+"))))
        ed.setCursor(ed.root, 0)
        ed.moveRight(); assertSame(s.base, ed.row)
        ed.moveRight(); ed.moveRight()
        // Past the base: straight to the superscript, the empty subscript skipped.
        assertSame(s.sup, ed.row)
        ed.moveRight(); ed.moveRight()
        assertSame(ed.root, ed.row); assertEquals(1, ed.index)
    }

    @Test fun emptyBracketsGoTogether() {
        val ed = Editor()
        "2+(".forEach { ed.type(it.toString()) }; ed.type(")"); ed.moveLeft()
        ed.backspace()
        assertEquals(listOf("2", "+"), ed.root.items.map { (it as Sym).text })
        ed.type("["); ed.type("]"); ed.moveLeft(); ed.backspace()
        assertEquals(listOf("2", "+"), ed.root.items.map { (it as Sym).text })
    }

    @Test fun anchorsCycleLikeF4() {
        assertEquals("=SUM(A1)*\$B\$2", Sheet.cycleAnchor("=SUM(A1)*B2"))
        assertEquals("=B\$2", Sheet.cycleAnchor("=\$B\$2"))
        assertEquals("=\$B2", Sheet.cycleAnchor("=B\$2"))
        assertEquals("=B2", Sheet.cycleAnchor("=\$B2"))
        assertNull(Sheet.cycleAnchor("=SUM("))
        assertTrue(Sheet.shift("=\$B2*C\$1", 3, 1) == "=\$B5*D\$1")
    }

    @Test fun copyingPartOfALineLeavesItIntact() {
        // The graph reads y = cos(▢) by copying the part after "=": the line itself must not change.
        val ed = Editor()
        ed.type("y"); ed.type("=")
        ed.insert(com.example.cas.editor.Func("cos"), 0)
        val cos = ed.root.items[2]
        com.example.cas.editor.MathCodec.copyOf(ed.root.items.drop(2))
        assertSame(ed.root, cos.parent)
        // ⌫ inside the empty brackets removes cos, and the cursor stays in the line.
        ed.backspace()
        assertEquals(2, ed.root.items.size)
        assertSame(ed.root, ed.row)
        // Stepping out of an element at the end lands in the line, not in a stray copy.
        ed.insert(com.example.cas.editor.Func("sin"), 0); ed.type("x")
        com.example.cas.editor.MathCodec.copyOf(ed.root.items.drop(2))
        ed.moveRight()
        assertSame(ed.root, ed.row); assertEquals(3, ed.index)
    }
}
