package com.example.cas

import com.example.cas.editor.Frac
import com.example.cas.editor.Func
import com.example.cas.editor.Integral
import com.example.cas.editor.MathRow
import com.example.cas.editor.Node
import com.example.cas.editor.Pow
import com.example.cas.editor.Sym
import com.example.cas.engine.Formatter
import com.example.cas.engine.Steps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StepsTest {
    private fun m(vararg parts: Any): MathRow = MathRow(parts.flatMap { p -> when (p) { is String -> p.map { Sym(it.toString()) }; is Node -> listOf(p); else -> error("") } }.toMutableList())
    private fun fn(name: String, vararg a: MathRow) = Func(name, a.toList())
    private fun integral(body: MathRow, lo: String = "", hi: String = "") = m(Integral(m(lo), m(hi), body, m("x")))
    private fun titles(s: Steps.Solution): List<String> = s.steps.flatMap { listOf(it.title) + it.substeps.map { t -> "  " + t.title } }
    private fun text(r: MathRow) = Formatter.plain(r)

    @Test fun onlyIntegralsAndLoops() {
        assertTrue(Steps.supports(integral(m("x"))))
        assertTrue(Steps.supports(m(fn("contour", m(Frac(m("1"), m("z"))), m(fn("abs", m("z")), "=1")))))
        assertFalse(Steps.supports(m("2+2")))
        assertFalse(Steps.supports(m(integral(m("x")).items[0], "+1")))
    }

    @Test fun byParts() {
        val s = Steps.of(integral(m("xe", Pow(m("x")))))!!
        println(titles(s))
        assertEquals("Integration by parts", s.method)
        assertTrue(titles(s).contains("Check"))
        assertTrue(text(s.answer).endsWith("+C"))
    }

    @Test fun sumAndPowerRule() {
        val s = Steps.of(integral(m("3x", Pow(m("2")), "+", fn("cos", m("x")))))!!
        println(titles(s))
        assertEquals("Sum rule", s.steps.first().title)
        assertTrue(titles(s).any { it.trim() == "Standard integral" })
    }

    @Test fun substitution() {
        val s = Steps.of(integral(m("2x", fn("cos", m("x", Pow(m("2")))))))!!
        println(titles(s)); println(s.steps.map { it.text })
        assertTrue(titles(s).any { it.contains("Substitution") || it.contains("Constant multiple") })
    }

    @Test fun partialFractions() {
        val s = Steps.of(integral(m(Frac(m("1"), m("x", Pow(m("2")), "−1")))))!!
        println(titles(s))
        assertTrue(titles(s).contains("Partial fractions"))
    }

    @Test fun definite() {
        val s = Steps.of(integral(m("x", Pow(m("2"))), "0", "3"))!!
        println(titles(s)); println(text(s.answer))
        assertTrue(titles(s).contains("Fundamental theorem"))
        assertTrue(text(s.answer).endsWith("=9"))
    }

    @Test fun specialFunction() {
        val s = Steps.of(integral(m(Frac(m(fn("sin", m("x"))), m("x")))))!!
        println(titles(s))
        assertTrue(titles(s).contains("Special function"))
    }

    @Test fun loopIntegralByResidues() {
        // ∮ 1/(z² + 1) around |z − i| = 1: only z = i is inside, Res = 1/(2i), so π.
        val s = Steps.of(m(fn("contour", m(Frac(m("1"), m("z", Pow(m("2")), "+1"))), m(fn("abs", m("z−i")), "=1"))))
        assertNotNull(s)
        println(titles(s!!)); println(s.steps.map { it.math?.let { r -> text(r) } })
        assertEquals("Residue theorem", s.method)
        assertTrue(text(s.answer).endsWith("=π"))
    }

    @Test fun loopWithoutPoles() {
        val s = Steps.of(m(fn("contour", m("z", Pow(m("2"))), m(fn("abs", m("z")), "=1"))))!!
        assertEquals("Cauchy's theorem", s.method)
    }

    @Test fun doublePole() {
        val s = Steps.of(m(fn("contour", m(Frac(m("1"), m("z", Pow(m("2"))))), m(fn("abs", m("z")), "=1"))))!!
        println(s.steps.map { it.title + ": " + it.text })
        assertTrue(s.steps.any { it.text?.contains("order 2") == true })
    }
}
