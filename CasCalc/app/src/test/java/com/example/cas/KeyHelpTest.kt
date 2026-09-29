package com.example.cas

import com.example.cas.editor.MathRow
import com.example.cas.editor.Sym
import com.example.cas.engine.LatexParser
import com.example.cas.ui.FunctionTabs
import com.example.cas.ui.KeyHelps
import com.example.cas.ui.KeyRole
import com.example.cas.ui.MainKeys
import com.example.cas.ui.letterRows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every key's long-press explanation: it parses, has nothing the renderer can't draw, and says what the key is. */
class KeyHelpTest {
    private val keys = (FunctionTabs.flatMap { t -> (if (t.title == "Symbols") letterRows(listOf("x_1")) else t.keys).flatten() } + MainKeys.flatten())
        .filter { it.role != KeyRole.Digit }

    private fun emptySymbols(r: MathRow): Int = r.items.sumOf { n -> (if (n is Sym && n.text.isEmpty()) 1 else 0) + n.slots.sumOf { emptySymbols(it) } }

    @Test fun everyExplanationDraws() {
        for (k in keys) {
            val h = KeyHelps.of(k.spoken)
            val rows = LatexParser.lines(h.formula) + listOf(h.about, h.usage).flatMap { t -> LatexParser.inline(t).filter { it.first }.map { LatexParser.parse(it.second) } }
            // An empty symbol crashed the renderer (a doubled backslash in the help text made one).
            rows.forEach { assertTrue("${k.spoken}: empty symbol in ${h.formula} / ${h.about}", emptySymbols(it) == 0) }
            listOf(h.formula, h.about, h.usage).forEach { assertTrue("${k.spoken}: doubled backslash in $it", !it.contains("\\\\(") && !it.contains("\\\\frac")) }
        }
    }

    @Test fun everyKeyHasADefinitionAndAnEquation() {
        // Editing keys have no equation to show.
        val noFormula = setOf("all clear", "backspace", "saved symbol")
        for (k in keys) {
            val h = KeyHelps.of(k.spoken)
            assertTrue("${k.spoken} has no description", h.about.isNotBlank())
            if (k.spoken !in noFormula) assertTrue("${k.spoken} has no formula", h.formula.isNotBlank())
        }
    }
}
