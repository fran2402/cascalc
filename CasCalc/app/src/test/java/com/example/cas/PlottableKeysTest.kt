package com.example.cas

import com.example.cas.editor.Func
import com.example.cas.editor.MathRow
import com.example.cas.editor.Sym
import com.example.cas.editor.row
import com.example.cas.engine.Evaluator
import com.example.cas.graph.Compiler
import com.example.cas.ui.ExtraKeyPages
import com.example.cas.ui.FunctionTabs
import com.example.cas.ui.KeyLabel
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every function on the extra tabs and second pages can be graphed in its variable. */
class PlottableKeysTest {
    private val values = mapOf("n" to "3", "k" to "2", "λ" to "1", "a" to "0.5", "b" to "2", "p" to "0.3", "μ" to "0", "σ" to "1",
        "α" to "0.5", "m" to "1", "γ" to "1", "x₀" to "0", "s" to "2", "φ" to "1", "t" to "0.5", "c" to "1")
    private val variables = setOf("x", "z", "t")

    private fun calls(): List<Func> {
        val keys = FunctionTabs.filter { it.optional }.flatMap { it.keys.flatten() } + ExtraKeyPages.flatMap { it.flatten() }
        return keys.mapNotNull { k -> ((k.label as? KeyLabel.Math)?.row?.items?.singleOrNull() as? Func) }
    }

    private fun plotted(f: Func, at: Int): List<Double> {
        val args = f.args.mapIndexed { i, r ->
            val name = r.items.joinToString("") { (it as? Sym)?.text ?: "?" }
            if (i == at) row("x") else row(values[name] ?: "1")
        }
        val e = Evaluator().evaluate(MathRow(mutableListOf(Func(f.name, args))))
        val g = Compiler.compile(e, listOf("x"))
        return listOf(-2.5, -1.5, -0.5, 0.25, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 4.2).map { g(doubleArrayOf(it)) }
    }

    @Test fun everyKeyGraphs() {
        val failures = ArrayList<String>()
        for (f in calls()) {
            // The variable: the argument named x (or t, z), else the first.
            val names = f.args.map { r -> r.items.joinToString("") { (it as? Sym)?.text ?: "?" } }
            val at = names.indexOfFirst { it in variables }.let { if (it < 0) 0 else it }
            try {
                val ys = plotted(f, at)
                if (ys.count { it.isFinite() } < 3) failures += "${f.name}: $ys"
            } catch (e: Exception) { failures += "${f.name}: ${e.message}" }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
