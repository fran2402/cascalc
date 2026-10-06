package com.example.cas

import com.example.cas.editor.Editor
import com.example.cas.editor.MathCodec
import com.example.cas.editor.row
import com.example.cas.engine.Evaluator
import com.example.cas.ui.FunctionTabs
import com.example.cas.ui.KeyAction
import com.example.cas.ui.MainKeys
import com.example.cas.ui.letterRows
import org.junit.Assert.fail
import org.junit.Test

/** Every key pressed on a fresh input, typed into, evaluated and backspaced away: nothing may throw. */
class EveryKeyTest {
    private fun press(ed: Editor, a: KeyAction) {
        when (a) {
            is KeyAction.Type -> ed.type(a.text)
            is KeyAction.Insert -> { ed.insert(a.make(), a.slot); if (a.path.isNotEmpty()) ed.enter(a.path) }
            KeyAction.Fraction -> ed.insertFraction()
            is KeyAction.Power -> ed.insertPower(a.exponent?.let { row(it) })
            KeyAction.Paren -> ed.smartParen()
            KeyAction.Backspace -> ed.backspace()
            KeyAction.Clear -> ed.clear()
            is KeyAction.Sequence -> a.steps.forEach { press(ed, it) }
            else -> Unit
        }
    }

    @Test fun noKeyThrows() {
        val keys = FunctionTabs.flatMap { t -> (if (t.title == "Symbols") letterRows(emptyList()) else t.keys).flatten() } + MainKeys.flatten() + com.example.cas.ui.ExtraKeyPages.flatMap { it.flatten() }
        for (k in keys) {
            try {
                val ed = Editor()
                press(ed, k.action)
                ed.type("2"); ed.type("x")
                runCatching { Evaluator().evaluate(MathCodec.copy(ed.root)) }.exceptionOrNull()?.let { e ->
                    if (e !is com.example.cas.cas.MathError && e !is ArithmeticException) throw e
                }
                MathCodec.decode(MathCodec.encode(ed.root))
                repeat(40) { ed.backspace() }
                ed.moveLeft(); ed.moveRight()
            } catch (e: Throwable) {
                fail("${k.spoken}: $e")
            }
        }
    }
}
