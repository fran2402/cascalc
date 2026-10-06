package com.example.cas

import com.example.cas.cas.Accent
import com.example.cas.cas.CustomSymbol
import com.example.cas.editor.Derivative
import com.example.cas.editor.Func
import com.example.cas.editor.Integral
import com.example.cas.editor.MathRow
import com.example.cas.editor.Pow
import com.example.cas.editor.Sym
import com.example.cas.editor.row
import com.example.cas.engine.Definitions
import com.example.cas.engine.Steps
import com.example.cas.ui.ExtraKeyPages
import com.example.cas.ui.FunctionTabs
import com.example.cas.ui.KeyLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Steps for the extra functions (their definitions) and with symbols built in the symbol builder. */
class DefinitionStepsTest {
    private val values = mapOf("x" to "0.5", "t" to "0.5", "n" to "3", "k" to "2", "λ" to "1", "a" to "0", "b" to "2", "p" to "0.3", "μ" to "0", "σ" to "1",
        "α" to "0.5", "m" to "1", "γ" to "1", "x₀" to "0", "s" to "2", "φ" to "1", "c" to "1")

    @Test fun everyExtraKeyHasSteps() {
        val keys = FunctionTabs.filter { it.optional }.flatMap { it.keys.flatten() } + ExtraKeyPages.flatMap { it.flatten() }
        val calls = keys.mapNotNull { k -> ((k.label as? KeyLabel.Math)?.row?.items?.singleOrNull() as? Func) }.filter { it.name in Definitions.all }
        assertTrue(calls.size >= 60)
        val missing = calls.filter { f ->
            val args = f.args.map { r -> row(values[r.items.joinToString("") { (it as? Sym)?.text ?: "?" }] ?: "1") }
            val r = MathRow(mutableListOf(Func(f.name, args)))
            Steps.of(r)?.let { latexOk(it) } == null
        }.map { it.name }
        assertEquals(emptyList<String>(), missing)
    }

    private val hat = CustomSymbol("x", Accent.Hat, "1", "").encode()

    @Test fun integralInABuiltSymbol() {
        val r = MathRow(mutableListOf(Integral(MathRow(), MathRow(), MathRow(mutableListOf(Sym(hat), Pow(row("2")))), MathRow(mutableListOf(Sym(hat))))))
        assertNotNull(Steps.of(r)?.let { latexOk(it) })
    }

    @Test fun derivativeInABuiltSymbol() {
        val r = MathRow(mutableListOf(Derivative(MathRow(mutableListOf(Sym(hat))), MathRow(mutableListOf(Sym(hat), Pow(row("3")))))))
        assertNotNull(Steps.of(r)?.let { latexOk(it) })
    }

    @Test fun sumOfHarmonicNumbersAddsUp() {
        val v = com.example.cas.engine.Evaluator().evaluate(MathRow(mutableListOf(com.example.cas.editor.BigOp(com.example.cas.editor.BigOpKind.Sum, row("n"), row("1"), row("4"), MathRow(mutableListOf(Func("harmonic", listOf(row("n")))))))))
        assertEquals("77/12", com.example.cas.cas.Printer.plain(v))
    }
}
