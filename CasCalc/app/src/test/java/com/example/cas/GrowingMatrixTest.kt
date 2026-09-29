package com.example.cas

import com.example.cas.cas.Printer
import com.example.cas.editor.Editor
import com.example.cas.editor.MathCodec
import com.example.cas.editor.Matrix
import com.example.cas.editor.trimMatrices
import com.example.cas.engine.Evaluator
import org.junit.Assert.assertEquals
import org.junit.Test

class GrowingMatrixTest {
    /** A growable matrix as the matrix key inserts it: 1 × 1 plus an empty row and column. */
    private fun fresh(): Pair<Editor, Matrix> {
        val ed = Editor()
        val m = Matrix(2, 2, growable = true)
        ed.insert(m, 0)
        return ed to (ed.root.items[0] as Matrix)
    }
    private fun typeAt(ed: Editor, m: Matrix, r: Int, c: Int, text: String) {
        ed.setCursor(m.cell(r, c), m.cell(r, c).items.size)
        text.forEach { ed.type(it.toString()) }
    }
    private fun value(ed: Editor) = Printer.plain(Evaluator().evaluate(ed.root))

    @Test fun startsAsOneByOne() {
        val (ed, m) = fresh()
        typeAt(ed, m, 0, 0, "5")
        assertEquals(2 to 2, m.rows to m.cols)    // no growth yet: row 1 and column 1 are the empty spares
        assertEquals("[[5]]", value(ed))
    }
    @Test fun typingInTheLastColumnAddsOne() {
        val (ed, m) = fresh()
        typeAt(ed, m, 0, 0, "1"); typeAt(ed, m, 0, 1, "2")
        assertEquals(2 to 3, m.rows to m.cols)
        assertEquals("[[1,2]]", value(ed))
    }
    @Test fun typingInTheLastRowAddsOne() {
        val (ed, m) = fresh()
        typeAt(ed, m, 0, 0, "1"); typeAt(ed, m, 1, 0, "3")
        assertEquals(3 to 2, m.rows to m.cols)
        assertEquals("[[1],[3]]", value(ed))
    }
    @Test fun twoByTwoWithAGap() {
        val (ed, m) = fresh()
        typeAt(ed, m, 0, 0, "1"); typeAt(ed, m, 0, 1, "2"); typeAt(ed, m, 1, 1, "4")
        // The empty (1, 0) inside counts as 0; the spare row and column don't count.
        assertEquals(3 to 3, m.rows to m.cols)
        assertEquals("[[1,2],[0,4]]", value(ed))
    }
    @Test fun historyKeepsOnlyWhatsUsed() {
        val (ed, m) = fresh()
        typeAt(ed, m, 0, 0, "1"); typeAt(ed, m, 0, 1, "2")
        assertEquals("mat:1x2{'1;|'2;}", MathCodec.encode(trimMatrices(ed.root)))
    }
    @Test fun growableSurvivesTheCodec() {
        val (ed, m) = fresh()
        typeAt(ed, m, 0, 0, "7")
        val back = MathCodec.decode(MathCodec.encode(ed.root)).items[0] as Matrix
        assertEquals(true, back.growable)
    }
    @Test fun fixedVectorsDontGrow() {
        val ed = Editor()
        val v = Matrix(2, 1)
        ed.insert(v, 0)
        typeAt(ed, v, 1, 0, "3")
        assertEquals(2 to 1, v.rows to v.cols)
    }

    @Test fun spareRowsDontPileUp() {
        val (ed, m) = fresh()
        typeAt(ed, m, 0, 0, "1"); typeAt(ed, m, 1, 0, "2")   // 3 rows now
        assertEquals(3, m.rows)
        // Empty the second row again: the last two rows are empty, so one goes.
        ed.setCursor(m.cell(0, 0), 1)
        val second = m.cell(1, 0); second.items.clear(); ed.type("")
        ed.backspace(); ed.type("1")   // any edit runs the tidy-up
        assertEquals(2, m.rows)
    }
    @Test fun historyMatricesGrowAgain() {
        val fixed = com.example.cas.editor.MathRow(mutableListOf(Matrix(2, 2, listOf(com.example.cas.editor.row("1"), com.example.cas.editor.row("2"), com.example.cas.editor.row("3"), com.example.cas.editor.row("4")))))
        val back = com.example.cas.editor.makeMatricesGrowable(fixed).items[0] as Matrix
        assertEquals(true, back.growable)
        assertEquals(3 to 3, back.rows to back.cols)
        assertEquals("[[1,2],[3,4]]", Printer.plain(Evaluator().evaluate(com.example.cas.editor.MathRow(mutableListOf(back)))))
    }
}
