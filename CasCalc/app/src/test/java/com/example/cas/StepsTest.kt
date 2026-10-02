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

class MoreStepsTest {
    private fun m(vararg parts: Any): MathRow = MathRow(parts.flatMap { p -> when (p) { is String -> p.map { Sym(it.toString()) }; is Node -> listOf(p); else -> error("") } }.toMutableList())
    private fun fn(name: String, vararg a: MathRow) = Func(name, a.toList())
    private fun d(body: MathRow, order: String = "", at: String = "") = m(com.example.cas.editor.Derivative(m("x"), body, m(at), m(order)))
    private fun titles(s: Steps.Solution): List<String> = s.steps.flatMap { listOf(it.title) + it.substeps.map { t -> "  " + t.title } }
    private fun text(r: MathRow) = Formatter.plain(r)
    private fun lim(body: MathRow, to: String) = m(fn("lim", body, m("x→$to")))
    private fun sum(body: MathRow, lo: String, hi: String) = m(com.example.cas.editor.BigOp(com.example.cas.editor.BigOpKind.Sum, m("k"), m(lo), m(hi), body))

    @Test fun productAndChain() {
        val s = Steps.of(d(m("x", Pow(m("2")), fn("sin", m("3x")))))!!
        println(titles(s))
        assertEquals("Product rule", s.steps.first().title)
        assertTrue(titles(s).any { it.contains("chain rule") })
    }

    @Test fun quotient() {
        val s = Steps.of(d(m(Frac(m(fn("sin", m("x"))), m("x")))))!!
        println(titles(s))
        assertTrue(titles(s).contains("Quotient rule"))
    }

    @Test fun higherOrderAtAPoint() {
        val s = Steps.of(d(m("x", Pow(m("3"))), order = "2", at = "2"))!!
        println(titles(s)); println(text(s.answer))
        assertTrue(titles(s).contains("Differentiate again"))
        assertTrue(text(s.answer).endsWith("=12"))
    }

    @Test fun limitDirect() = assertEquals("Direct substitution", Steps.of(lim(m("x", Pow(m("2")), "+1"), "2"))!!.method)

    @Test fun limitLHopital() {
        val s = Steps.of(lim(m(Frac(m(fn("sin", m("x"))), m("x"))), "0"))!!
        println(titles(s)); println(s.steps.map { it.math?.let { r -> text(r) } })
        assertEquals("L'Hôpital's rule", s.method)
        assertTrue(text(s.answer).endsWith("=1"))
    }

    @Test fun limitTwiceLHopital() {
        val s = Steps.of(lim(m(Frac(m("1−", fn("cos", m("x"))), m("x", Pow(m("2"))))), "0"))!!
        println(titles(s))
        assertEquals("L'Hôpital's rule", s.method)
        assertTrue(titles(s).contains("L'Hôpital's rule again"))
    }

    @Test fun limitAtInfinity() {
        val s = Steps.of(lim(m(Frac(m("3x", Pow(m("2")), "+1"), m("x", Pow(m("2")), "−5"))), "∞"))!!
        println(titles(s)); println(text(s.answer))
        assertTrue(text(s.answer).endsWith("=3"))
    }

    @Test fun finiteSumWrittenOut() = assertEquals("Adding the terms", Steps.of(sum(m("k", Pow(m("2"))), "1", "4"))!!.method)

    @Test fun powerSums() {
        val s = Steps.of(sum(m("k", Pow(m("2"))), "1", "n"))!!
        println(titles(s)); println(s.steps.map { it.text })
        assertEquals("Power sums", s.method)
    }

    @Test fun basel() = assertEquals("Zeta function", Steps.of(sum(m(Frac(m("1"), m("k", Pow(m("2"))))), "1", "∞"))!!.method)

    @Test fun alternating() = assertEquals("Eta function", Steps.of(sum(m(Frac(m("(−1)", Pow(m("k+1"))), m("k"))), "1", "∞"))!!.method)

    @Test fun geometric() = assertEquals("Geometric series", Steps.of(sum(m(Frac(m("1"), m("2", Pow(m("k"))))), "0", "∞"))!!.method)
}

class EvenMoreStepsTest {
    private fun m(vararg parts: Any): MathRow = MathRow(parts.flatMap { p -> when (p) { is String -> p.map { Sym(it.toString()) }; is Node -> listOf(p); else -> error("") } }.toMutableList())
    private fun fn(name: String, vararg a: MathRow) = Func(name, a.toList())
    private fun prod(body: MathRow, lo: String, hi: String) = m(com.example.cas.editor.BigOp(com.example.cas.editor.BigOpKind.Product, m("k"), m(lo), m(hi), body))
    private fun text(r: MathRow) = Formatter.plain(r)
    private fun of(r: MathRow) = Steps.of(r)!!.also { println(it.method + ": " + it.steps.map { s -> s.title } + " → " + text(it.answer)) }

    @Test fun productWrittenOut() = assertEquals("Multiplying the factors", of(prod(m("k+1"), "1", "4")).method)
    @Test fun factorial() = assertEquals("Factorial", of(prod(m("k"), "1", "n")).method)
    @Test fun constantFactor() = assertEquals("Powers", of(prod(m("3"), "1", "n")).method)
    @Test fun telescoping() = assertEquals("Telescoping product", of(prod(m(Frac(m("k+1"), m("k"))), "1", "n")).method)

    @Test fun simplePoleResidue() {
        val s = of(m(fn("residue", m(Frac(m("1"), m("z", Pow(m("2")), "+1"))), m("z=i"))))
        assertEquals("Simple pole", s.method)
    }
    @Test fun doublePoleResidue() {
        val s = of(m(fn("residue", m(Frac(m("e", Pow(m("z"))), m("z", Pow(m("2"))))), m("z=0"))))
        assertEquals("Pole of order 2", s.method)
        assertTrue(text(s.answer).endsWith("=1"))
    }
    @Test fun taylorSeries() {
        val s = of(m(fn("taylor", m("e", Pow(m("x"))), m("x→0"), m("3"))))
        assertEquals("Taylor series", s.method)
        assertEquals(4, s.steps[0].substeps.size)
    }
    @Test fun det2() {
        val s = of(m(fn("det", m(com.example.cas.editor.Matrix(2, 2, listOf(m("1"), m("2"), m("3"), m("4")))))))
        assertTrue(text(s.answer).endsWith("=-2") || text(s.answer).endsWith("=−2"))
    }
    @Test fun det3() {
        val s = of(m(fn("det", m(com.example.cas.editor.Matrix(3, 3, listOf(m("2"), m("0"), m("1"), m("1"), m("3"), m("2"), m("1"), m("1"), m("1")))))))
        assertEquals("Cofactor expansion", s.method)
        assertEquals(3, s.steps[0].substeps.size)
    }
    @Test fun complexDivision() {
        assertTrue(Steps.supports(m(Frac(m("3+i"), m("2−i")))))
        val s = of(m(Frac(m("3+i"), m("2−i"))))
        assertEquals("Complex division", s.method)
        assertTrue(text(s.answer).endsWith("=1+i"))
    }
    @Test fun complexProduct() = assertEquals("Complex multiplication", of(m("(1+2i)(3−i)")).method)
    @Test fun euler() = assertEquals("Euler's formula", of(m("e", Pow(m("iπ")), "+1")).method)
    @Test fun notForPlainArithmetic() = assertFalse(Steps.supports(m("2+3")))
    @Test fun notForVariables() = assertFalse(Steps.supports(m("(x+i)", Pow(m("2")))))
    @Test fun copyText() {
        val q = m(Frac(m("3+i"), m("2−i")))
        val t = Steps.text(q, Steps.of(q)!!)
        println(t)
        assertTrue(t.contains("1. Multiply by the conjugate"))
    }
}

class MethodStepsTest {
    private fun m(vararg parts: Any): MathRow = MathRow(parts.flatMap { p -> when (p) { is String -> p.map { Sym(it.toString()) }; is Node -> listOf(p); else -> error("") } }.toMutableList())
    private fun fn(name: String, vararg a: MathRow) = Func(name, a.toList())
    private fun integral(body: MathRow) = m(Integral(m(""), m(""), body, m("x")))
    private fun all(s: Steps.Solution): List<Pair<String, String?>> = s.steps.flatMap { listOf(it.title to it.text) + it.substeps.map { t -> t.title to t.text } }
    private fun of(r: MathRow) = Steps.of(r)!!.also { println(all(it)) }

    @Test fun trigSubstitutionSine() {
        val s = of(integral(m(com.example.cas.editor.Sqrt(m("4−x", Pow(m("2")))))))
        assertTrue(all(s).any { it.first == "Trigonometric substitution" && it.second!!.contains("sine") })
    }
    @Test fun trigSubstitutionTangent() {
        val s = of(integral(m(com.example.cas.editor.Sqrt(m("x", Pow(m("2")), "+9")))))
        assertTrue(all(s).any { it.first == "Trigonometric substitution" || it.first.contains("Standard") || it.first.contains("Substitution") })
    }
    @Test fun oddPowerOfSine() {
        val s = of(integral(m(fn("sin", m("x")), Pow(m("3")))))
        assertTrue(all(s).any { it.first.contains("power") || it.first.contains("Half-angle") || it.first.contains("Substitution") })
    }
    @Test fun weierstrassOrOther() {
        val s = of(integral(m(Frac(m("1"), m("2+", fn("cos", m("x")))))))
        assertTrue(all(s).isNotEmpty())
    }
    @Test fun multiplyOutFirst() {
        val s = of(integral(m("(x+1)", Pow(m("2")))))
        assertTrue(all(s).isNotEmpty())
    }
    @Test fun radical() {
        val s = of(integral(m(Frac(m("x"), m(com.example.cas.editor.Sqrt(m("x+1")))))))
        assertTrue(all(s).isNotEmpty())
    }
}

class WrittenSubstitutionTest {
    private fun m(vararg parts: Any): MathRow = MathRow(parts.flatMap { p -> when (p) { is String -> p.map { Sym(it.toString()) }; is Node -> listOf(p); else -> error("") } }.toMutableList())
    private fun fn(name: String, vararg a: MathRow) = Func(name, a.toList())
    private fun integral(body: MathRow) = m(Integral(m(""), m(""), body, m("x")))
    private fun titles(s: Steps.Solution) = s.steps.flatMap { listOf(it.title) + it.substeps.map { t -> "  " + t.title } }
    private fun of(r: MathRow) = Steps.of(r)!!.also { s -> println(titles(s)); s.steps.forEach { st -> st.math?.let { println("   " + st.title + ": " + Formatter.plain(it)) } } }

    @Test fun trigSubstitutionWrittenOut() {
        val s = of(integral(m(com.example.cas.editor.Sqrt(m("4−x", Pow(m("2")))))))
        assertTrue(titles(s).contains("The new integral"))
        assertTrue(titles(s).contains("Back-substitute"))
    }
    @Test fun oneOverRoot() {
        val s = of(integral(m(Frac(m("x", Pow(m("2"))), m(com.example.cas.editor.Sqrt(m("4−x", Pow(m("2")))))))))
        assertTrue(titles(s).isNotEmpty())
    }
    @Test fun oddSine() {
        val s = of(integral(m(fn("sin", m("x")), Pow(m("3")))))
        assertTrue(titles(s).contains("The new integral"))
    }
    @Test fun rationalizing() {
        val s = of(integral(m(Frac(m("1"), m("1+", com.example.cas.editor.Sqrt(m("x")))))))
        assertTrue(titles(s).isNotEmpty())
    }
    @Test fun exponential() {
        val s = of(integral(m(Frac(m("1"), m("e", Pow(m("x")), "+1")))))
        assertTrue(titles(s).isNotEmpty())
    }
}

class RemainingMethodsTest {
    private fun m(vararg parts: Any): MathRow = MathRow(parts.flatMap { p -> when (p) { is String -> p.map { Sym(it.toString()) }; is Node -> listOf(p); else -> error("") } }.toMutableList())
    private fun fn(name: String, vararg a: MathRow) = Func(name, a.toList())
    private fun integral(body: MathRow) = m(Integral(m(""), m(""), body, m("x")))
    private fun deep(ss: List<Steps.Step>, pre: String): List<String> = ss.flatMap { listOf(pre + it.title) + deep(it.substeps, "  ") }
    private fun titles(s: Steps.Solution) = deep(s.steps, "")
    private fun of(r: MathRow) = Steps.of(r)!!.also { s -> println(titles(s)) }

    @Test fun quadratic() = assertTrue(titles(of(integral(m(Frac(m("x+3"), m("x", Pow(m("2")), "+2x+5")))))).contains("Quadratic denominator"))
    @Test fun partsTwice() = assertTrue(titles(of(integral(m("e", Pow(m("x")), fn("sin", m("x")))))).contains("By parts twice"))
    @Test fun productToSum() = assertTrue(titles(of(integral(m(fn("sin", m("3x")), fn("cos", m("2x")))))).contains("Product-to-sum"))
    @Test fun powerReduction() = assertTrue(titles(of(integral(m(fn("cos", m("x")), Pow(m("2")))))).let { it.contains("Power reduction") || it.any { t -> "Half" in t } })
    @Test fun gaussian() = assertTrue(titles(of(integral(m("e", Pow(m("−x", Pow(m("2")), "+2x")))))).contains("Complete the square"))
    @Test fun special() = assertTrue(titles(of(integral(m(Frac(m("e", Pow(m("x"))), m("x")))))).contains("  Defining derivative"))
    @Test fun inverseParts() = assertTrue(titles(of(integral(m("x", fn("atan", m("x")))))).any { it.contains("parts") })
    @Test fun hyperbolicForm() = assertTrue(titles(of(integral(m("x", fn("sinh", m("x")))))).isNotEmpty())
    @Test fun quarticDenominator() = assertTrue(titles(of(integral(m(Frac(m("1"), m("x", Pow(m("4")), "+1")))))).isNotEmpty())
}

class FullStepsTest {
    private fun m(vararg parts: Any): MathRow = MathRow(parts.flatMap { p -> when (p) { is String -> p.map { Sym(it.toString()) }; is Node -> listOf(p); else -> error("") } }.toMutableList())
    private fun fn(name: String, vararg a: MathRow) = Func(name, a.toList())
    private fun integral(body: MathRow) = m(Integral(m(""), m(""), body, m("x")))
    private fun deep(ss: List<Steps.Step>, pre: String): List<String> = ss.flatMap { listOf(pre + it.title) + deep(it.substeps, "  ") }
    private fun titles(s: Steps.Solution) = deep(s.steps, "").also { println(it) }

    @Test fun quarticWrittenOut() {
        val t = titles(Steps.of(integral(m(Frac(m("1"), m("x", Pow(m("4")), "+1")))))!!)
        assertTrue(t.contains("  First piece") && t.contains("  Second piece"))
    }
    @Test fun atanByPartsHasNoBlackBox() {
        val t = titles(Steps.of(integral(m("x", fn("atan", m("x")))))!!)
        assertFalse(t.any { it.contains("Rewrite and integrate") })
    }
    @Test fun solveForIWrittenOut() {
        val t = titles(Steps.of(integral(m("e", Pow(m("2x")), fn("cos", m("3x")))))!!)
        assertTrue(t.any { it.contains("Collect the I terms") })
    }
    @Test fun logarithmicDifferentiation() {
        val t = titles(Steps.of(m(com.example.cas.editor.Derivative(m("x"), m("x", Pow(m("x"))), m(""), m(""))))!!)
        assertTrue(t.any { it.contains("Take logarithms") })
    }
    @Test fun divideFirst() {
        val t = titles(Steps.of(integral(m(Frac(m("x", Pow(m("3"))), m("x", Pow(m("2")), "−1")))))!!)
        assertTrue(t.any { it.contains("Divide, then split") })
    }
}

class MoreFullStepsTest {
    private fun m(vararg parts: Any): MathRow = MathRow(parts.flatMap { p -> when (p) { is String -> p.map { Sym(it.toString()) }; is Node -> listOf(p); else -> error("") } }.toMutableList())
    private fun fn(name: String, vararg a: MathRow) = Func(name, a.toList())
    private fun of(r: MathRow) = Steps.of(r)!!.also { s -> println(s.method + ": " + s.steps.map { st -> st.title + " | " + (st.math?.let { Formatter.plain(it) } ?: "") }) }

    @Test fun complexProduct() = assertEquals("Complex multiplication", of(m("(1+2i)(3−i)")).method)
    @Test fun complexPower() = assertEquals("Complex powers", of(m("(1+i)", Pow(m("8")))).method)
    @Test fun det4() {
        val cells = listOf("1", "0", "2", "1", "0", "1", "0", "3", "2", "0", "1", "0", "1", "1", "0", "1").map { m(it) }
        val s = of(m(fn("det", m(com.example.cas.editor.Matrix(4, 4, cells)))))
        assertEquals(4, s.steps[0].substeps.size)
    }
    @Test fun powerSumValues() {
        val s = of(m(com.example.cas.editor.BigOp(com.example.cas.editor.BigOpKind.Sum, m("k"), m("1"), m("n"), m("2k+1"))))
        assertTrue(s.steps.any { it.math != null && it.title.startsWith("Sum of") })
    }
    @Test fun geometricValues() {
        val s = of(m(com.example.cas.editor.BigOp(com.example.cas.editor.BigOpKind.Sum, m("k"), m("0"), m("∞"), m(Frac(m("1"), m("3", Pow(m("k"))))))))
        assertTrue(s.steps.first().math != null)
    }
}

class LastStepsTest {
    private fun m(vararg parts: Any): MathRow = MathRow(parts.flatMap { p -> when (p) { is String -> p.map { Sym(it.toString()) }; is Node -> listOf(p); else -> error("") } }.toMutableList())
    private fun fn(name: String, vararg a: MathRow) = Func(name, a.toList())
    private fun integral(body: MathRow) = m(Integral(m(""), m(""), body, m("x")))
    private fun deep(ss: List<Steps.Step>, pre: String): List<String> = ss.flatMap { listOf(pre + it.title) + deep(it.substeps, "  ") }
    private fun of(r: MathRow) = Steps.of(r)!!.also { s -> println(s.method + ": " + deep(s.steps, "")) }

    @Test fun multipleAngles() = assertTrue(deep(of(integral(m(Frac(m(fn("sin", m("2x"))), m(fn("cos", m("x"))))))).steps, "").contains("Multiple angles"))
    @Test fun oddComplexPower() = assertTrue(of(m("(1+i)", Pow(m("5")))).steps.any { it.title == "Times z" })
    @Test fun det5() {
        val cells = listOf("2", "1", "0", "0", "0", "1", "2", "1", "0", "0", "0", "1", "2", "1", "0", "0", "0", "1", "2", "1", "0", "0", "0", "1", "2").map { m(it) }
        val s = of(m(fn("det", m(com.example.cas.editor.Matrix(5, 5, cells)))))
        assertEquals("Row reduction", s.method)
        assertTrue(Formatter.plain(s.answer).endsWith("=6"))
    }
}
