package com.example.cas

import com.example.cas.editor.Binom
import com.example.cas.editor.Func
import com.example.cas.editor.MathRow
import com.example.cas.editor.Node
import com.example.cas.editor.Sqrt
import com.example.cas.editor.Sym
import com.example.cas.engine.Evaluator
import com.example.cas.engine.Formatter
import com.example.cas.engine.Steps
import com.example.cas.ui.ExtraKeyPages
import com.example.cas.ui.FunctionTabs
import com.example.cas.ui.KeyAction
import com.example.cas.ui.MainKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Steps for what the calculator offers: function calls one at a time, and every function key. */
class StepsCoverageTest {
    private fun m(vararg parts: Any): MathRow = MathRow(parts.flatMap { p -> when (p) { is String -> p.map { Sym(it.toString()) }; is Node -> listOf(p); else -> error("") } }.toMutableList())
    private fun all(steps: List<Steps.Step>): List<Steps.Step> = steps.flatMap { listOf(it) + all(it.substeps) }

    @Test fun gcdByEuclid() {
        val s = latexOk(Steps.of(m(Func("gcd", listOf(m("48"), m("18")))))!!)
        assertEquals("Greatest common divisor", s.steps.first().title)
        assertEquals(3, s.steps.first().substeps.size) // 48 = 2·18 + 12, 18 = 1·12 + 6, 12 = 2·6 + 0
        assertEquals("6", Formatter.plain(s.answer).substringAfterLast("=").trim())
    }

    @Test fun combinationsInAnExpression() {
        // 2·C(5, 2) + √72: each call on its own, then the whole.
        val s = latexOk(Steps.of(m("2", Binom(m("5"), m("2")), "+", Sqrt(m("72"))))!!)
        val titles = s.steps.map { it.title }
        assertTrue(titles.toString(), "Combinations" in titles && "Simplify the root" in titles)
    }

    @Test fun everyFunctionKeyHasSteps() {
        val keys = (FunctionTabs.flatMap { it.keys } + MainKeys + ExtraKeyPages.flatten()).flatten()
        val funcs = keys.mapNotNull { k -> (k.action as? KeyAction.Insert)?.let { runCatching { it.make() }.getOrNull() as? Func } }.distinctBy { it.name }
        val missing = ArrayList<String>()
        var tried = 0
        for (f in funcs) {
            // Small whole numbers in every box: 3, 2, 1… (a list function gets a short list).
            val args = f.args.indices.map { k -> m(listOf("3", "2", "1", "1")[k.coerceAtMost(3)]) }
            val row = m(Func(f.name, args))
            val ok = runCatching { Evaluator().evaluate(com.example.cas.editor.MathCodec.copy(row)) }.isSuccess
            if (!ok) continue
            tried++
            val s = runCatching { Steps.of(row) }.getOrNull()
            if (s == null) missing += f.name else latexOk(s)
        }
        assertTrue("checked only $tried", tried > 60)
        assertEquals("no steps for: $missing", emptyList<String>(), missing)
    }
}

class MatrixProductStepsTest {
    private fun r(t: String) = MathRow(t.map { Sym(it.toString()) }.toMutableList<Node>())
    @Test fun rowTimesColumn() {
        val a = com.example.cas.editor.Matrix(2, 2, listOf(r("1"), r("2"), r("3"), r("4")))
        val b = com.example.cas.editor.Matrix(2, 1, listOf(r("5"), r("6")))
        val s = latexOk(Steps.of(MathRow(mutableListOf(a, Sym("×"), b)))!!)
        assertEquals(2, s.steps.first().substeps.size)
        assertNotNull(s.steps.first().substeps.first().title)
    }
}
