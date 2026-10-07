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
    private val keys = (FunctionTabs.flatMap { t -> (if (t.title == "Symbols") letterRows(listOf("x_1")) else t.keys).flatten() } + MainKeys.flatten() + com.example.cas.ui.ExtraKeyPages.flatMap { it.flatten() })
        .filter { it.role != KeyRole.Digit }

    private fun emptySymbols(r: MathRow): Int = r.items.sumOf { n -> (if (n is Sym && n.text.isEmpty()) 1 else 0) + n.slots.sumOf { emptySymbols(it) } }

    @Test fun everyExplanationDraws() {
        for (k in keys) {
            val h = KeyHelps.of(k.spoken)
            val rows = LatexParser.lines(h.formula) + (listOf(h.about) + h.steps + h.examples.map { it.note }).flatMap { t -> LatexParser.inline(t).filter { it.first }.map { LatexParser.parse(it.second) } }
            // An empty symbol crashed the renderer (a doubled backslash in the help text made one).
            rows.forEach { assertTrue("${k.spoken}: empty symbol in ${h.formula} / ${h.about}", emptySymbols(it) == 0) }
            (listOf(h.formula, h.about) + h.steps + h.examples.map { it.note }).forEach { assertTrue("${k.spoken}: doubled backslash in $it", !it.contains("\\\\(") && !it.contains("\\\\frac")) }
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

    @Test fun everyKeyHasAGuide() {
        // Editing keys and constants need no example.
        val noExample = setOf("all clear", "backspace", "previous answer", "symbol builder", "saved symbol", "list of constants with names") +
            com.example.cas.engine.Constant.entries.map { it.description.substringBefore(" (") }
        val missing = keys.filter { k -> KeyHelps.of(k.spoken).let { it.about.isBlank() || it.steps.isEmpty() || (it.examples.isEmpty() && k.spoken !in noExample) } }.map { it.spoken }.distinct()
        assertTrue("No guide for: $missing", missing.isEmpty())
    }

    @Test fun everyGuideTextParses() {
        for (k in keys) {
            val h = KeyHelps.of(k.spoken)
            for (t in listOf(h.about) + h.steps + h.examples.map { it.note })
                for ((isMath, piece) in LatexParser.inline(t)) if (isMath)
                    assertTrue("${k.spoken}: unknown LaTeX in $t: ${LatexParser.unknownCommands(piece)}", LatexParser.unknownCommands(piece).isEmpty())
        }
    }

    /** Every example works out (the card shows its answer); the answers are written out for checking. */
    @Test fun everyExampleWorksOut() {
        val failures = ArrayList<String>()
        val out = StringBuilder()
        for (k in keys.distinctBy { it.spoken }) for (e in KeyHelps.of(k.spoken).examples) {
            try {
                if (!e.answer) { com.example.cas.editor.MathCodec.copy(e.row); continue }
                val (a, approx) = com.example.cas.ui.KeyGuides.answer(e) ?: throw IllegalStateException("works out to itself")
                out.append(k.spoken).append(": ").append(com.example.cas.engine.Latex.of(e.row)).append(if (approx) "  ≈  " else "  =  ").append(com.example.cas.engine.Latex.of(a)).append('\n')
            } catch (ex: Exception) { failures += "${k.spoken}: ${com.example.cas.engine.Latex.of(e.row)}: ${ex.message}" }
        }
        System.getenv("KEY_EXAMPLES")?.let { java.io.File(it).writeText(out.toString()) }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    /** Every card graph evaluates over most of its range, and its legend is valid LaTeX; samples go to $KEY_PLOTS. */
    @Test fun everyGraphDraws() {
        val problems = ArrayList<String>()
        val out = StringBuilder()
        for ((key, plot) in com.example.cas.ui.KeyGuides.plots) {
            if (KeyHelps.of(key).plot !== plot) problems += "$key: not on any keypad"
            val ys = try { com.example.cas.ui.KeyGuides.sample(plot, 120) } catch (e: Exception) { problems += "$key: ${e.message}"; continue }
            ys.forEachIndexed { k, v ->
                val c = plot.curves[k]
                if (v.count { it.isFinite() } < v.size / 3) problems += "$key: ${c.label} is mostly undefined"
                if (LatexParser.unknownCommands(c.label).isNotEmpty()) problems += "$key: ${c.label}: ${LatexParser.unknownCommands(c.label)}"
                out.append(key).append('\t').append(c.label).append('\t').append(plot.x.start).append('\t').append(plot.x.endInclusive).append('\t')
                    .append(plot.y?.start ?: "").append('\t').append(plot.y?.endInclusive ?: "").append('\t').append(v.joinToString(",")).append('\n')
            }
        }
        System.getenv("KEY_PLOTS")?.let { java.io.File(it).writeText(out.toString()) }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}
