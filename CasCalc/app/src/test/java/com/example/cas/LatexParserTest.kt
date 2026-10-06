package com.example.cas

import com.example.cas.editor.MathCodec
import com.example.cas.engine.LatexParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LatexParserTest {
    private fun p(latex: String) = MathCodec.encode(LatexParser.parse(latex))

    @Test fun fraction() = assertEquals("frac{'a;|'b;}", p("\\frac{a}{b}"))
    // A letter with a plain subscript is one symbol, as the symbol builder makes it; a superscript on it is a power.
    @Test fun powerAndSubscript() = assertEquals("'x;pow{'2;}'" + com.example.cas.cas.CustomSymbol("a", sub = "ij").encode() + ";", p("x^2 a_{ij}"))
    @Test fun subAndSup() = assertEquals("'" + com.example.cas.cas.CustomSymbol("x", sub = "i").encode() + ";pow{'2;}", p("x_i^2"))
    @Test fun roots() = assertEquals("sqrt{'x;}root{'n;|'x;}", p("\\sqrt{x}\\sqrt[n]{x}"))
    @Test fun greekAndSymbols() = assertEquals("'π;'≤;'∞;'·;'θ;", p("\\pi \\le \\infty \\cdot \\theta"))
    @Test fun functionNames() = assertEquals("'sin;'\u2009;'θ;", p("\\sin\\theta"))
    @Test fun minusSign() = assertEquals("'a;'−;'b;", p("a-b"))
    @Test fun integralWithDx() = assertEquals("int{'a;|'b;|'f;'(;'x;');|'x;}", p("\\int_a^b f(x)\\,dx"))
    @Test fun integralMathrmD() = assertEquals("int{'0;|'∞;|'e;pow{'−;'t;}|'t;}", p("\\int_0^\\infty e^{-t}\\,\\mathrm{d}t"))
    @Test fun sumWithIndex() = assertEquals("sum{'k;|'1;|'n;|'k;pow{'2;}}", p("\\sum_{k=1}^{n} k^2"))
    @Test fun sumOfInvisibleIndex() = assertEquals("scr{'Σ;|'i;|}'x;", p("\\sum_i x"))
    @Test fun limit() = assertEquals("fn:lim{frac{'1;|'x;}|'x;'→;'∞;}", p("\\lim_{x\\to\\infty} \\frac{1}{x}"))
    @Test fun contour() = assertEquals("fn:contour{'f;'(;'z;');|fn:abs{'z;}'=;'1;}", p("\\oint_{|z|=1} f(z)\\,dz"))
    @Test fun binomial() = assertEquals("binom{'n;|'k;}", p("\\binom{n}{k}"))
    @Test fun matrix() = assertEquals("mat:2x2{'a;|'b;|'c;|'d;}", p("\\begin{bmatrix} a & b \\\\ c & d \\end{bmatrix}"))
    @Test fun textIsOneUprightPiece() = assertEquals("scr{'Res;|'z;'=;'a;|}", p("\\operatorname{Res}_{z=a}"))
    // \bar{x} is x with a bar accent, the same symbol the symbol builder makes.
    @Test fun barredX() = assertEquals(com.example.cas.cas.CustomSymbol("x", com.example.cas.cas.Accent.Bar).encode(), (LatexParser.parse("\\bar{x}").items.single() as com.example.cas.editor.Sym).text)
    @Test fun leftRightDropped() = assertEquals("'(;'x;');", p("\\left(x\\right)"))
    @Test fun inlineMaths() = assertEquals(listOf(false to "Area ", true to "\\int f", false to " here."), LatexParser.inline("Area \\(\\int f\\) here."))

    @Test fun everyHelpFormulaParses() {
        val keys = (com.example.cas.ui.FunctionTabs.flatMap { it.keys.flatten() } + com.example.cas.ui.MainKeys.flatten() + com.example.cas.ui.ExtraKeyPages.flatMap { it.flatten() }).map { it.spoken }.distinct()
        for (k in keys) {
            val h = com.example.cas.ui.KeyHelps.of(k)
            LatexParser.lines(h.formula)
            (LatexParser.inline(h.about) + h.steps.flatMap { LatexParser.inline(it) }).filter { it.first }.forEach { LatexParser.parse(it.second) }
            // Every command used is one the parser knows.
            val unknown = h.formula.split('\n').flatMap { LatexParser.unknownCommands(it) } +
                (LatexParser.inline(h.about) + h.steps.flatMap { LatexParser.inline(it) }).filter { it.first }.flatMap { LatexParser.unknownCommands(it.second) }
            assertTrue("$k uses unsupported LaTeX: $unknown", unknown.isEmpty())
        }
    }

    @Test fun semicolonSurvivesTheCodec() {
        val row = LatexParser.parse("\\varphi(x; \\mu)")
        val code = MathCodec.encode(row)
        assertEquals(code, MathCodec.encode(MathCodec.decode(code)))
        assertTrue(code.contains("'\\;;"))
    }
    @Test fun oldCodesStillDecode() = assertEquals("'x;'+;'1;", MathCodec.encode(MathCodec.decode("'x;'+;'1;")))

    @Test fun primesAreRaised() = assertEquals("'F;pow{'′;}'y;pow{'′′;}", p("F' y''"))

    @Test fun operatorSpacing() = assertEquals("'det;'\u2009;'A;'sin;'(;'x;');", p("\\det A \\sin(x)"))
    @Test fun severalLines() = assertEquals(2, LatexParser.lines("a = b\nc = d").size)

    // Constants' help formulas: proper LaTeX, upright units, every command supported.
    private fun constant(id: String) = com.example.cas.engine.Constant.entries.first { it.id == id }
    @Test fun neutronMassLatex() = assertEquals("m_{\\mathrm{n}} = 1.674\\,927\\,500\\,56 \\times 10^{-27}\\;\\mathrm{kg}", com.example.cas.ui.ConstantLatex.formula(constant("mn")))
    @Test fun unitsBecomeNegativePowers() = assertEquals("\\mathrm{m}^{3}\\,\\mathrm{kg}^{-1}\\,\\mathrm{s}^{-2}", com.example.cas.ui.ConstantLatex.unit("m³/(kg·s²)"))
    @Test fun thousandsGrouped() = assertEquals("12\\,906.403", com.example.cas.ui.ConstantLatex.value("12906.403"))
    @Test fun unitsAreUpright() {
        // kg is one upright symbol, not italic k and g.
        val row = LatexParser.parse("\\mathrm{kg}")
        assertEquals("'kg;", MathCodec.encode(row))
    }
    @Test fun everyConstantFormulaParses() {
        for (k in com.example.cas.engine.Constant.entries) {
            val f = com.example.cas.ui.ConstantLatex.formula(k)
            val unknown = f.split('\n').flatMap { LatexParser.unknownCommands(it) }
            assertTrue("${k.id}: $unknown in $f", unknown.isEmpty())
            f.split('\n').forEach { LatexParser.parse(it) }
            assertTrue(com.example.cas.ui.ConstantLatex.nistLink(k).startsWith("https://physics.nist.gov/"))
        }
    }

    @Test fun singleUprightLetterIsMarked() = assertEquals("'\u2060m;", MathCodec.encode(LatexParser.parse("\\mathrm{m}")))
    @Test fun mathitStaysItalic() = assertEquals("'m;", MathCodec.encode(LatexParser.parse("\\mathit{m}")))

    // Every row of every help formula has matching brackets (the Taylor and e formulas didn't).
    private fun balanced(row: com.example.cas.editor.MathRow): Boolean {
        var depth = 0
        for (n in row.items) {
            when ((n as? com.example.cas.editor.Sym)?.text) { "(" -> depth++; ")" -> { depth--; if (depth < 0) return false } }
            if (!n.slots.all { balanced(it) }) return false
        }
        return depth == 0
    }
    @Test fun taylorBodyKeepsItsBracket() = assertTrue(balanced(LatexParser.parse("\\sum_{n=0}^{N} \\frac{f^{(n)}(a)}{n!}(x - a)^n")))
    @Test fun limitBodyKeepsLeftRight() {
        val row = LatexParser.parse("\\lim_{n\\to\\infty} \\left(1 + \\frac{1}{n}\\right)^n = 2.71828")
        assertTrue(balanced(row))
        // The body is the whole bracket with its power, not an empty box.
        val lim = row.items.first { it is com.example.cas.editor.Func } as com.example.cas.editor.Func
        assertTrue(MathCodec.encode(lim.args[0]).startsWith("'(;'1;'+;frac"))
    }
    @Test fun everyHelpFormulaHasBalancedBrackets() {
        val keys = (com.example.cas.ui.FunctionTabs.flatMap { it.keys.flatten() } + com.example.cas.ui.MainKeys.flatten() + com.example.cas.ui.ExtraKeyPages.flatMap { it.flatten() }).map { it.spoken }.distinct()
        for (k in keys) {
            val h = com.example.cas.ui.KeyHelps.of(k)
            LatexParser.lines(h.formula).forEach { assertTrue("$k: ${MathCodec.encode(it)}", balanced(it)) }
            (LatexParser.inline(h.about) + h.steps.flatMap { LatexParser.inline(it) }).filter { it.first }.forEach { (_, m) -> assertTrue("$k: $m", balanced(LatexParser.parse(m))) }
        }
    }
}
