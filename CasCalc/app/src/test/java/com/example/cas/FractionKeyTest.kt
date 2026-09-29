package com.example.cas

import com.example.cas.editor.Editor
import com.example.cas.editor.Func
import com.example.cas.editor.MathCodec
import com.example.cas.editor.row
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The ÷ key always makes a fraction; an implicit product before it moves up whole. */
class FractionKeyTest {
    private fun keys(vararg k: String): Editor {
        val ed = Editor()
        for (s in k) when (s) {
            "÷" -> ed.insertFraction()
            "^2" -> ed.insertPower(row("2"))
            "sin(" -> ed.insert(Func("sin"), 0)
            "→" -> ed.moveRight()
            else -> s.forEach { ed.type(it.toString()) }
        }
        return ed
    }
    private fun code(ed: Editor) = MathCodec.encode(ed.root)

    @Test fun emptyFractionStartsInTheNumerator() {
        val ed = keys("÷")
        assertEquals("frac{|}", code(ed))
        ed.type("1")
        assertEquals("frac{'1;|}", code(ed))
    }
    @Test fun numberMovesUp() {
        val ed = keys("12", "÷")
        assertEquals("frac{'1;'2;|}", code(ed))
        ed.type("5")
        assertEquals("frac{'1;'2;|'5;}", code(ed))   // the cursor was in the denominator
    }
    @Test fun implicitProductMovesUpWhole() = assertEquals("frac{'2;'x;'y;|}", code(keys("2xy", "÷")))
    @Test fun powersStayInTheProduct() = assertEquals("frac{'3;'x;pow{'2;}'y;|}", code(keys("3x", "^2", "y", "÷")))
    @Test fun bracketsAndFunctionsToo() = assertEquals("frac{'3;'(;'x;'+;'1;');fn:sin{'x;}|}", code(keys("3(x+1)", "sin(", "x", "→", "÷")))
    @Test fun stopsAtPlus() = assertEquals("'a;'+;frac{'2;'b;|}", code(keys("a+2b", "÷")))
    @Test fun stopsAtExplicitTimes() = assertEquals("'a;'×;frac{'6;|}", code(keys("a×6", "÷")))
    @Test fun afterAnOperatorItsEmpty() = assertEquals("'2;'+;frac{|}", code(keys("2+", "÷")))

    @Test fun limitArrowCantBeDeleted() {
        val ed = Editor()
        ed.insert(Func("lim", listOf(com.example.cas.editor.MathRow(), com.example.cas.editor.MathRow(mutableListOf(com.example.cas.editor.Sym("x"), com.example.cas.editor.Sym("→"), com.example.cas.editor.Sym("0"))))), 1)
        // Cursor after "0" in x → 0: backspace deletes 0, then steps over →, then deletes x.
        ed.setCursor((ed.root.items[0] as Func).args[1], 3)
        ed.backspace(); ed.backspace(); ed.backspace()
        assertEquals("fn:lim{|'→;}", code(ed))
    }
    @Test fun contourCircleCantBeDeleted() {
        val ed = Editor()
        val spec = com.example.cas.editor.MathRow(mutableListOf(Func("abs", listOf(row("z"))), com.example.cas.editor.Sym("="), com.example.cas.editor.Sym("1")))
        ed.insert(Func("contour", listOf(com.example.cas.editor.MathRow(), spec)), 1)
        ed.setCursor((ed.root.items[0] as Func).args[1], 3)
        repeat(3) { ed.backspace() }
        // 1 goes; = stays; the cursor moves into |z|.
        assertEquals("fn:contour{|fn:abs{'z;}'=;}", code(ed))
    }

    // ≤ and ≥ are typed as <= or =< and >= or =>.
    @Test fun lessOrEqualCombines() {
        val ed = Editor(); ed.type("x"); ed.type("<"); ed.type("=")
        assertEquals("'x;'≤;", code(ed))
    }
    @Test fun otherOrderAlsoCombines() {
        val ed = Editor(); ed.type("="); ed.type("<")
        assertEquals("'≤;", code(ed))
    }
    @Test fun greaterOrEqualCombines() {
        val ed = Editor(); ed.type(">"); ed.type("=")
        assertEquals("'≥;", code(ed))
    }
    @Test fun plainEqualsIsUntouched() {
        val ed = Editor(); ed.type("x"); ed.type("=")
        assertEquals("'x;'=;", code(ed))
    }
    @Test fun undoAfterCombining() {
        val ed = Editor(); ed.type("<"); ed.type("="); ed.undo()
        assertEquals("'<;", code(ed))
    }

    // Backspace in the empty main field of a construction removes the whole thing.
    @Test fun backspaceRemovesAnEmptyIntegral() {
        val ed = Editor()
        ed.insert(com.example.cas.editor.Integral(variable = row("x")), 2)
        ed.backspace()
        assertEquals("", code(ed))
    }
    @Test fun backspaceRemovesAnEmptyRoot() {
        val ed = Editor()
        ed.type("1"); ed.insert(com.example.cas.editor.Sqrt(), 0); ed.backspace()
        assertEquals("'1;", code(ed))
    }
    @Test fun aFilledConstructionIsKept() {
        val ed = Editor()
        ed.insert(com.example.cas.editor.Sqrt(), 0); ed.type("9"); ed.backspace()
        assertEquals("sqrt{}", code(ed))
    }
    // An empty root index means a square root.
    @Test fun emptyRootIndexIsASquareRoot() {
        val r = com.example.cas.editor.MathRow(mutableListOf(com.example.cas.editor.Root(com.example.cas.editor.MathRow(), row("9"))))
        assertEquals("3", com.example.cas.cas.Printer.plain(com.example.cas.engine.Evaluator().evaluate(r)))
    }

    // The power key with nothing to raise gives the base a box (brackets) and the exponent a box.
    @Test fun powerAtTheStartGetsABaseBox() {
        val ed = Editor(); ed.insertPower()
        assertEquals("'(;');pow{}", code(ed))
    }
    @Test fun powerAfterAnOperatorGetsABaseBox() {
        val ed = Editor(); ed.type("2"); ed.type("+"); ed.insertPower()
        assertEquals("'2;'+;'(;');pow{}", code(ed))
    }
    @Test fun powerAfterANumberIsUnchanged() {
        val ed = Editor(); ed.type("2"); ed.insertPower()
        assertEquals("'2;pow{}", code(ed))
    }
    @Test fun baseBoxThenExponentEvaluates() {
        val ed = Editor(); ed.insertPower(); ed.type("3")
        // the cursor sits between the brackets: (3)^□, then fill the exponent
        val pow = ed.root.items.last() as com.example.cas.editor.Pow
        pow.exp.items.add(com.example.cas.editor.Sym("2"))
        assertEquals("9", com.example.cas.cas.Printer.plain(com.example.cas.engine.Evaluator().evaluate(ed.root)))
    }
    // An empty main box removes its whole construction, even with limits filled in.
    @Test fun emptyIntegralBodyRemovesTheIntegral() {
        val ed = Editor()
        val integral = com.example.cas.editor.Integral(row("0"), row("1"), com.example.cas.editor.MathRow(), row("x"))
        ed.insert(integral, 2)
        ed.backspace()
        assertEquals("", code(ed))
    }
    @Test fun fractionWithADenominatorStays() {
        val ed = Editor()
        val f = com.example.cas.editor.Frac(com.example.cas.editor.MathRow(), row("2"))
        ed.insert(f, 0)
        ed.backspace()
        assertTrue(code(ed).contains("frac"))
    }

    // ⁻¹, ᵀ and ᴴ pressed with the cursor inside a matrix apply to the whole matrix.
    @Test fun transposeFromInsideAMatrix() {
        val ed = Editor()
        val m = com.example.cas.editor.Matrix(2, 2, listOf(row("1"), row("2"), row("3"), row("4")))
        ed.insert(m, 0)
        ed.setCursor(m.cell(1, 1), 1)
        ed.insertPower(row("T"))
        assertEquals("mat:2x2{'1;|'2;|'3;|'4;}pow{'T;}", code(ed))
    }
    @Test fun ordinaryPowerInsideAMatrixStaysInside() {
        val ed = Editor()
        val m = com.example.cas.editor.Matrix(2, 2, listOf(row("1"), row("2"), row("3"), row("x")))
        ed.insert(m, 0)
        ed.setCursor(m.cell(1, 1), 1)
        ed.insertPower(row("2"))
        assertEquals("mat:2x2{'1;|'2;|'3;|'x;pow{'2;}}", code(ed))
    }
}
