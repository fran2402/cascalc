package com.example.cas

import com.example.cas.cas.MathError
import com.example.cas.cas.Printer
import com.example.cas.editor.Derivative
import com.example.cas.editor.Editor
import com.example.cas.editor.Frac
import com.example.cas.editor.Func
import com.example.cas.editor.Integral
import com.example.cas.editor.MathCodec
import com.example.cas.editor.MathRow
import com.example.cas.editor.Pow
import com.example.cas.editor.Sym
import com.example.cas.editor.row
import com.example.cas.engine.Evaluator
import org.junit.Assert.assertEquals
import org.junit.Test

class NewFeaturesTest {
    private fun cas(r: MathRow): String = try {
        Printer.plain(Evaluator().evaluate(r))
    } catch (e: MathError) { "Error: " + e.message }

    /** Builds a row from a compact string; ² ³ are exponents, ′ is a prime. */
    private fun r(text: String): MathRow {
        val out = MathRow()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when (c) {
                '²' -> out.add(Pow(row("2")))
                '³' -> out.add(Pow(row("3")))
                else -> out.add(Sym(c.toString()))
            }
            i++
        }
        return out
    }
    private fun f(name: String, vararg a: MathRow) = row(Func(name, a.toList()))

    // ---- Systems
    @Test fun linearSystem() = assertEquals("x=2, y=1", cas(f("solve", r("x+y=3,x−y=1"), r("x,y"))))
    @Test fun linearSystemAutoUnknowns() = assertEquals("x=2, y=1", cas(f("solve", r("x+y=3,x−y=1"), MathRow())))
    @Test fun threeByThree() = assertEquals("x=1, y=2, z=3", cas(f("solve", r("x+y+z=6,2x−y+z=3,x+2y−z=2"), r("x,y,z"))))
    @Test fun infinitelyMany() = assertEquals("x=z+2, y=-z+1", cas(f("solve", r("x+y=3,x−z=2"), r("x,y"))))
    @Test fun inconsistent() = assertEquals("Error: No solution", cas(f("solve", r("x+y=1,x+y=2"), r("x,y"))))
    @Test fun nonlinearSystem() = assertEquals("(x=-2, y=-1) or (x=1, y=2)", cas(f("solve", r("x²+y²=5,y=x+1"), r("x,y"))).let { it })
    @Test fun symbolicSystem() = assertEquals("x=(a+b)/2, y=(a-b)/2", cas(f("solve", r("x+y=a,x−y=b"), r("x,y"))))

    // ---- Inequalities
    @Test fun quadraticInequality() = assertEquals("-2<x<2", cas(f("solve", r("x²<4"), r("x"))))
    @Test fun outsideInterval() = assertEquals("x≤-2 or x≥2", cas(f("solve", r("x²≥4"), r("x"))))
    @Test fun linearInequality() = assertEquals("x>3", cas(f("solve", r("2x−1>5"), r("x"))))
    @Test fun rationalInequality() = assertEquals("x<-1 or x>1", cas(f("solve", row(Frac(r("x+1"), r("x−1")), Sym(">"), Sym("0")), r("x"))))
    @Test fun chainInequality() = assertEquals("1<x≤3", cas(f("solve", r("0<x−1≤2"), r("x"))))
    @Test fun irrationalEnds() = assertEquals("-√2<x<√2", cas(f("solve", r("x²<2"), r("x"))))
    @Test fun alwaysTrue() = assertEquals("Error: True for every real x", cas(f("solve", r("x²+1>0"), r("x"))))
    @Test fun neverTrue() = assertEquals("Error: No solution", cas(f("solve", r("x²<0"), r("x"))))
    @Test fun touchingPoint() = assertEquals("x=0", cas(f("solve", r("x²≤0"), r("x"))))

    // ---- Limits at infinity, one-sided limits, improper integrals
    private fun lim(body: MathRow, spec: String) = cas(f("lim", body, r(spec)))
    @Test fun limitAtInfinityRational() = assertEquals("2", lim(row(Frac(r("2x+1"), r("x+3"))), "x→∞"))
    @Test fun limitAtInfinityExp() = assertEquals("0", lim(row(Sym("e"), Pow(r("−x"))), "x→∞"))
    @Test fun limitDiverges() = assertEquals("∞", lim(r("x²"), "x→∞"))
    @Test fun limitMinusInfinity() = assertEquals("-∞", lim(r("x³"), "x→−∞"))
    @Test fun oneSidedRight() = assertEquals("∞", lim(row(Frac(r("1"), r("x"))), "x→0+"))
    @Test fun oneSidedLeft() = assertEquals("-∞", lim(row(Frac(r("1"), r("x"))), "x→0−"))
    @Test fun twoSidedDiffer() = assertEquals("Error: The limits from the left and right differ", lim(row(Frac(r("1"), r("x"))), "x→0"))
    @Test fun limitAtInfinityOfInverse() = assertEquals("0", lim(row(Frac(r("1"), r("x"))), "x→∞"))
    @Test fun improperExact() = assertEquals("1", cas(row(Integral(r("0"), r("∞"), row(Sym("e"), Pow(r("−x"))), r("x")))))
    @Test fun improperOneOverXSquared() = assertEquals("1", cas(row(Integral(r("1"), r("∞"), row(Frac(r("1"), r("x²"))), r("x")))))
    @Test fun improperGaussian() = assertEquals("1.772453851", cas(row(Integral(r("−∞"), r("∞"), row(Sym("e"), Pow(row(Sym("−"), Sym("x"), Pow(row("2"))))), r("x")))))

    // ---- Higher derivatives
    @Test fun secondDerivative() = assertEquals("-sin(x)", cas(row(Derivative(r("x"), row(Func("sin", listOf(r("x")))), MathRow(), r("2")))))
    @Test fun thirdDerivativeAt() = assertEquals("24", cas(row(Derivative(r("x"), r("x⁴".replace("⁴", "")).also { it.add(Pow(row("4"))) }, r("1"), r("3")))))

    // ---- Differential equations
    private fun ode(text: String) = cas(f("dsolve", r(text)))
    @Test fun exponentialGrowth() = assertEquals("y=C1e^(2x)", ode("y′=2y"))
    @Test fun growthWithCondition() = assertEquals("y=3e^(2x)", ode("y′=2y,y(0)=3"))
    @Test fun linearFirstOrder() = assertEquals("y=C1e^(-x)+x-1", ode("y′+y=x"))
    @Test fun separable() = assertEquals("y=-1/(x+C1)", ode("y′=y²"))
    @Test fun harmonic() = assertEquals("y=C1cos(x)+C2sin(x)", ode("y″+y=0".replace("″", "′′")))
    @Test fun harmonicWithConditions() = assertEquals("y=cos(x)", ode("y′′+y=0,y(0)=1,y′(0)=0"))
    @Test fun distinctRealRoots() = assertEquals("y=C1e^(-3x)+C2e^(2x)", ode("y′′+y′−6y=0"))
    @Test fun repeatedRoot() = assertEquals("y=(C2x+C1)e^(2x)", ode("y′′−4y′+4y=0"))
    @Test fun dampedOscillation() = assertEquals("y=(C1cos(2x)+C2sin(2x))e^(-x)", ode("y′′+2y′+5y=0"))
    @Test fun polynomialForcing() = assertEquals("y=-x^2+C1e^(-x)+C2e^x-2", ode("y′′−y=x²"))
    @Test fun exponentialForcing() = assertEquals("y=C1cos(x)+C2sin(x)+e^(2x)/5", ode("y′′+y=e^(2x)".let { "y′′+y=e" }).let { cas(f("dsolve", row(Sym("y"), Sym("′"), Sym("′"), Sym("+"), Sym("y"), Sym("="), Sym("e"), Pow(r("2x"))))) })
    @Test fun resonance() = assertEquals("y=xsin(x)/2+C1cos(x)+C2sin(x)", cas(f("dsolve", row(Sym("y"), Sym("′"), Sym("′"), Sym("+"), Sym("y"), Sym("="), Func("cos", listOf(r("x")))))))

    // ---- Undo and redo
    @Test fun undoRedo() {
        val ed = Editor()
        ed.type("1"); ed.type("+"); ed.type("2")
        ed.undo(); assertEquals("'1;'+;", MathCodec.encode(ed.root))
        ed.undo(); assertEquals("'1;", MathCodec.encode(ed.root))
        ed.redo(); assertEquals("'1;'+;", MathCodec.encode(ed.root))
        ed.type("5"); assertEquals(false, ed.canRedo)
    }
    @Test fun undoKeepsCursorInsideFraction() {
        val ed = Editor()
        ed.type("3"); ed.insertFraction(); ed.type("4"); ed.type("5")
        ed.undo()
        assertEquals("frac{'3;|'4;}", MathCodec.encode(ed.root))
        ed.type("9")
        assertEquals("frac{'3;|'4;'9;}", MathCodec.encode(ed.root))
    }
    @Test fun undoClear() {
        val ed = Editor()
        ed.type("7"); ed.clear(); ed.undo()
        assertEquals("'7;", MathCodec.encode(ed.root))
    }

    // ---- LaTeX
    private fun tex(r: MathRow) = com.example.cas.engine.Latex.of(r)
    @Test fun latexFraction() = assertEquals("\\frac{x^{2}}{4}-2", tex(row(Frac(r("x²"), r("4")), Sym("−"), Sym("2"))))
    @Test fun latexIntegral() = assertEquals("\\int_{0}^{\\pi} \\sin\\left(x\\right)\\,\\mathrm{d}x", tex(row(Integral(r("0"), r("π"), row(Func("sin", listOf(r("x")))), r("x")))))
    @Test fun latexMatrix() = assertEquals("\\begin{bmatrix}1 & 2 \\\\ 3 & 4\\end{bmatrix}", tex(row(com.example.cas.editor.Matrix(2, 2, listOf(r("1"), r("2"), r("3"), r("4"))))))
    @Test fun latexAnswer() = assertEquals("x=\\frac{\\pi}{6},\\,\\,x=\\frac{5\\pi}{6}",
        tex(com.example.cas.engine.Formatter.row(Evaluator().evaluate(f("solve", row(Func("sin", listOf(r("x"))), Sym("="), Frac(r("1"), r("2"))), r("x"))))))
    @Test fun latexConstantOfIntegration() = assertEquals("y=C_{1}e^{2x}", tex(com.example.cas.engine.Formatter.row(Evaluator().evaluate(f("dsolve", r("y′=2y"))))))

    // ---- Sending results to the graphs
    private fun request(r: MathRow) = com.example.cas.engine.Graphing.request(r, Evaluator().evaluate(r))
    @Test fun graphSolveSendsBothSides() {
        val g = request(f("solve", row(Func("sin", listOf(r("x"))), Sym("="), Frac(r("1"), r("2"))), r("x")))!!
        assertEquals(2, g.rows.size); assertEquals(2, g.dimensions)
    }
    @Test fun graphFunctionOfX() = assertEquals(2, request(row(Derivative(r("x"), r("x³"))))!!.dimensions)
    @Test fun graphSurface() = assertEquals(3, request(r("xy"))!!.dimensions)
    @Test fun numbersAreNotGraphed() = assertEquals(null, request(r("2+2")))

    // ---- Decimal places setting
    @Test fun significantDigits() {
        val old = com.example.cas.engine.Formatter.significantDigits
        com.example.cas.engine.Formatter.significantDigits = 4
        try {
            val a = com.example.cas.engine.Formatter.answer(Evaluator().evaluate(row(com.example.cas.editor.Sqrt(r("2")))))
            assertEquals("1.414", com.example.cas.engine.Formatter.plain(a.approx!!))
        } finally { com.example.cas.engine.Formatter.significantDigits = old }
    }

    // ---- Letters and the new "more" functions
    @Test fun greekVariable() = assertEquals("5θ", cas(r("2θ+3θ")))
    @Test fun capitalVariable() = assertEquals("2AB", cas(r("AB+BA")))
    @Test fun solveForGreek() = assertEquals("ω=3", cas(f("solve", r("2ω=6"), r("ω"))))
    @Test fun sign() = assertEquals("-1", cas(f("sgn", r("−5"))))
    @Test fun signOfConstant() = assertEquals("1", cas(f("sgn", r("π−3"))))
    @Test fun fractionalPart() = assertEquals("3/4", cas(f("frac", row(Frac(r("7"), r("4"))))))
    @Test fun fractionalPartNegative() = assertEquals("1/4", cas(f("frac", row(Sym("−"), Frac(r("7"), r("4"))))))
    @Test fun latexFractionalPart() = assertEquals("\\left\\{x\\right\\}", com.example.cas.engine.Latex.of(f("frac", r("x"))))

    // ---- Physical constants in different unit systems
    private fun constant(id: String, units: com.example.cas.engine.UnitSystem): String =
        Printer.plain(Evaluator(units = units).evaluate(row(com.example.cas.editor.Const(id))))
    private fun near(expected: Double, text: String) = org.junit.Assert.assertTrue("$text vs $expected", kotlin.math.abs(text.toDouble() - expected) < 1e-6 * kotlin.math.abs(expected))
    @Test fun allConstantsListed() = assertEquals(55, com.example.cas.engine.Constant.entries.size)
    @Test fun siExact() = assertEquals("299792458", constant("c0", com.example.cas.engine.UnitSystem.SI))
    @Test fun planckOnes() {
        for (id in listOf("c0", "hbar", "G", "kB")) assertEquals("1", constant(id, com.example.cas.engine.UnitSystem.Planck))
    }
    @Test fun planckCharge() = near(Math.sqrt(7.2973525643e-3), constant("qe", com.example.cas.engine.UnitSystem.Planck))
    @Test fun planckPermittivity() = near(1 / (4 * Math.PI), constant("eps0", com.example.cas.engine.UnitSystem.Planck))
    @Test fun atomicOnes() {
        for (id in listOf("hbar", "me", "qe", "a0", "Eh")) assertEquals("1", constant(id, com.example.cas.engine.UnitSystem.Atomic))
    }
    @Test fun atomicSpeedOfLight() = near(137.035999177, constant("c0", com.example.cas.engine.UnitSystem.Atomic))
    @Test fun naturalOnes() {
        for (id in listOf("c0", "hbar", "kB", "eps0")) assertEquals("1", constant(id, com.example.cas.engine.UnitSystem.Natural))
    }
    @Test fun naturalCharge() = near(Math.sqrt(4 * Math.PI * 7.2973525643e-3), constant("qe", com.example.cas.engine.UnitSystem.Natural))
    @Test fun naturalElectronMass() = near(510998.95, constant("me", com.example.cas.engine.UnitSystem.Natural))
    @Test fun dimensionlessUnchanged() = assertEquals(constant("alpha", com.example.cas.engine.UnitSystem.SI), constant("alpha", com.example.cas.engine.UnitSystem.Planck))
    @Test fun latexConstant() = assertEquals("\\hbar", com.example.cas.engine.Latex.of(row(com.example.cas.editor.Const("hbar"))))
}
