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
}
