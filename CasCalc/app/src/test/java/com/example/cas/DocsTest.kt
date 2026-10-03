package com.example.cas

import com.example.cas.editor.MathCodec
import com.example.cas.engine.AngleUnit
import com.example.cas.engine.Evaluator
import com.example.cas.engine.Formatter
import com.example.cas.engine.Latex
import com.example.cas.ui.Docs
import com.example.cas.ui.DocsResults
import com.example.cas.ui.KeyHelps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The documentation says what the calculator really does. */
class DocsTest {
    @Test fun everyExampleGivesWhatTheDocsSay() {
        for (e in Docs.allExamples) {
            val said = DocsResults.of(e)
            assertNotNull("No answer for ${Latex.of(e.row)}", said)
            val a = Formatter.answer(Evaluator(if (e.degrees) AngleUnit.Degrees else AngleUnit.Radians).evaluate(MathCodec.copy(e.row)))
            assertEquals(Latex.of(e.row), said!!.first, Latex.of(a.exact))
        }
    }

    @Test fun manyExamples() = assertTrue(Docs.allExamples.size >= 150)

    @Test fun textUsesKnownLatex() {
        fun check(t: String) {
            assertEquals("unbalanced $ in: $t", 0, t.count { it == '$' } % 2)
            for ((isMath, piece) in com.example.cas.engine.LatexParser.inline(t)) if (isMath) {
                assertEquals("unknown LaTeX in: $t", emptySet<String>(), com.example.cas.engine.LatexParser.unknownCommands(piece))
            }
        }
        for (c in Docs.chapters) {
            check(c.summary)
            for (s in c.sections) for (b in s.blocks) when (b) {
                is Docs.Block.Para -> check(b.text)
                is Docs.Block.Note -> check(b.text)
                is Docs.Block.Bullets -> b.items.forEach(::check)
                is Docs.Block.Examples -> b.examples.forEach { check(it.note) }
                is Docs.Block.Table -> b.rows.forEach { (a, d) -> check(a); check(d) }
                else -> {}
            }
        }
    }

    @Test fun everyKeyGroupHasKeys() {
        for ((title, keys) in KeyHelps.groups) assertTrue(title, keys.isNotEmpty())
    }
}
