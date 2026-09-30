package com.example.cas

import com.example.cas.cas.MathError
import com.example.cas.cas.Printer
import com.example.cas.editor.Derivative
import com.example.cas.editor.Frac
import com.example.cas.editor.Func
import com.example.cas.editor.Integral
import com.example.cas.editor.MathCodec
import com.example.cas.editor.MathRow
import com.example.cas.editor.Matrix
import com.example.cas.editor.Pow
import com.example.cas.editor.Sym
import com.example.cas.editor.row
import com.example.cas.engine.Evaluator
import com.example.cas.graph.Compiler
import com.example.cas.graph.Curves
import com.example.cas.graph.PlotSpec
import com.example.cas.graph.Viewport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

class MultivariableTest {
    private fun r(text: String): MathRow {
        val out = MathRow()
        for (c in text) when (c) { '²' -> out.add(Pow(row("2"))); '³' -> out.add(Pow(row("3"))); else -> out.add(Sym(c.toString())) }
        return out
    }
    private fun cas(x: MathRow) = try { Printer.plain(Evaluator().evaluate(x)) } catch (e: MathError) { "Error: " + e.message }
    private fun f(name: String, vararg a: MathRow) = row(Func(name, a.toList()))
    private fun vec(vararg c: String) = row(Matrix(c.size, 1, c.map { r(it) }))

    // ---- Partial derivatives and vector calculus
    @Test fun partialDerivative() = assertEquals("2xy^3", cas(row(Derivative(r("x"), r("x²y³"), partial = true))))
    @Test fun secondPartial() = assertEquals("6x^2y", cas(row(Derivative(r("y"), r("x²y³"), MathRow(), r("2"), partial = true))))
    @Test fun partialRoundTrip() = assertEquals("pdiff{'x;|'x;'y;||}", MathCodec.encode(MathCodec.decode(MathCodec.encode(row(Derivative(r("x"), r("xy"), partial = true))))))
    @Test fun gradient() = assertEquals("[[2xy],[x^2]]", cas(f("grad", r("x²y"))))
    // ∇ with its order box: empty is the gradient, 2 the Laplacian; LaTeX writes the order raised.
    @Test fun gradientOrderEmpty() = assertEquals("[[2xy],[x^2]]", cas(f("grad", r("x²y"), MathRow())))
    @Test fun gradientOrderTwo() = assertEquals("2y", cas(f("grad", r("x²y"), r("2"))))
    @Test fun gradientLatex() {
        assertEquals("\\nabla \\left(x^{2}y\\right)", com.example.cas.engine.Latex.of(f("grad", r("x²y"), MathRow())))
        assertEquals("\\nabla^{2}\\left(x^{2}y\\right)", com.example.cas.engine.Latex.of(f("grad", r("x²y"), r("2"))))
    }
    @Test fun gradientRoundTrip() = assertEquals("fn:grad{'f;|'2;}", MathCodec.encode(MathCodec.decode(MathCodec.encode(f("grad", r("f"), r("2"))))))
    @Test fun gradient3d() = assertEquals("[[yz],[xz],[xy]]", cas(f("grad", r("xyz"))))
    @Test fun divergence() = assertEquals("3", cas(f("div", vec("x", "y", "z"))))
    @Test fun curl3d() = assertEquals("[[0],[0],[2]]", cas(f("curl", vec("−y", "x", "0"))))
    @Test fun curl2d() = assertEquals("2", cas(f("curl", vec("−y", "x"))))
    @Test fun curlOfGradientIsZero() = assertEquals("[[0],[0],[0]]", cas(f("curl", f("grad", r("x²yz³")))))
    @Test fun laplacian() = assertEquals("6x+2y", cas(f("laplacian", r("x³+x²y"))))
    @Test fun laplacianHarmonic() = assertEquals("0", cas(f("laplacian", r("x²−y²"))))
    @Test fun jacobian() = assertEquals("[[y,x],[1,1]]", cas(f("jacobian", vec("xy", "x+y"))))
    @Test fun hessian() = assertEquals("[[2y,2x],[2x,0]]", cas(f("hessian", r("x²y"))))

    // ---- Double and triple integrals
    private fun integral(lo: String, hi: String, body: MathRow, v: String) = row(Integral(r(lo), r(hi), body, r(v)))
    @Test fun doubleIntegralExact() = assertEquals("1/8", cas(integral("0", "1", integral("0", "x", r("xy"), "y"), "x")))
    @Test fun doubleIntegralRectangle() = assertEquals("1/4", cas(integral("0", "1", integral("0", "1", r("xy"), "y"), "x")))
    @Test fun tripleIntegral() = assertEquals("1/6", cas(integral("0", "1", integral("0", "1−x", integral("0", "1−x−y", r("1"), "z"), "y"), "x")))
    @Test fun doubleIntegralNumeric() {
        // ∫₀¹∫₀¹ e^(−x²y²) dy dx has no closed form: the inner integral is held and done numerically.
        val body = row(Sym("e"), Pow(row(Sym("−"), Sym("x"), Pow(row("2")), Sym("y"), Pow(row("2")))))
        val v = cas(integral("0", "1", integral("0", "1", body, "y"), "x")).toDouble()
        assertTrue("$v", abs(v - 0.9059404763) < 1e-8)
    }

    // ---- What a line in the 2D graph means
    private fun spec(x: MathRow) = PlotSpec.classify(listOf(Evaluator().evaluate(x)))
    @Test fun explicitBare() = assertTrue(spec(r("x²")) is PlotSpec.Explicit)
    @Test fun explicitWithY() = assertTrue(spec(r("y=x²")) is PlotSpec.Explicit)
    @Test fun polarWithR() = assertTrue(spec(row(Sym("r"), Sym("="), Sym("1"), Sym("+"), Func("cos", listOf(r("θ"))))) is PlotSpec.Polar)
    @Test fun polarBare() = assertTrue(spec(r("2θ")) is PlotSpec.Polar)
    @Test fun implicitCircle() = assertTrue(spec(r("x²+y²=1")) is PlotSpec.Implicit)
    @Test fun verticalLine() = assertTrue(spec(r("x=2")) is PlotSpec.Implicit)
    @Test fun region() = assertTrue(spec(r("y<x²")) is PlotSpec.Region)
    @Test fun parameterOnImplicit() = assertEquals(listOf("a"), spec(r("x²+y²=a")).parameters)
    // x² + y² on its own is a scalar field, drawn in colour.
    @Test fun bareXYIsAField() = assertTrue(spec(r("x²+y²")) is PlotSpec.Field)
    @Test fun fieldSliders() = assertEquals(listOf("a"), spec(r("ax²−y²")).parameters)

    // ---- Samplers
    private val view = Viewport(-2.0, 2.0, -2.0, 2.0)
    @Test fun implicitCircleOnTheCircle() {
        val c = Compiler.compile(Evaluator().evaluate(r("x²+y²−1")), listOf("x", "y"))
        val segs = Curves.implicit({ x, y -> c(doubleArrayOf(x, y)) }, view, 80, 80)
        assertTrue(segs.size > 100)
        assertTrue(segs.all { s -> abs(hypot(s[0], s[1]) - 1) < 0.01 && abs(hypot(s[2], s[3]) - 1) < 0.01 })
    }
    @Test fun implicitSkipsPoles() {
        // xy = 1 is a hyperbola: no segments cross the axes.
        val c = Compiler.compile(Evaluator().evaluate(r("xy−1")), listOf("x", "y"))
        val segs = Curves.implicit({ x, y -> c(doubleArrayOf(x, y)) }, view, 60, 60)
        assertTrue(segs.all { s -> s[0] * s[1] > 0.5 && s[2] * s[3] > 0.5 })
    }
    @Test fun regionAreaOfDisc() {
        val mask = Curves.region({ x, y -> x * x + y * y <= 1 }, view, 200, 200)
        val area = mask.count { it } * (16.0 / (200 * 200))
        assertTrue("$area", abs(area - Math.PI) < 0.02)
    }
    @Test fun polarCircle() {
        val lines = Curves.polar({ 1.0 }, view)
        assertEquals(1, lines.size)
        assertTrue(lines[0].all { (x, y) -> abs(hypot(x, y) - 1) < 1e-12 })
    }
    @Test fun parametricBreaksWhereUndefined() {
        val lines = Curves.parametric({ t -> t }, { t -> Math.sqrt(t) }, -1.0, 1.0, view)
        assertEquals(1, lines.size); assertTrue(lines[0].first().first >= 0)
    }
    @Test fun chainedComparison() {
        assertTrue(Curves.holds(doubleArrayOf(0.0, 0.5, 1.0), listOf("<", "≤")))
        assertTrue(!Curves.holds(doubleArrayOf(0.0, 1.5, 1.0), listOf("<", "≤")))
    }

    @Test fun doubleIntegralKeyPutsCursorInside() {
        val ed = com.example.cas.editor.Editor()
        ed.insert(Integral(body = MathRow(mutableListOf(Integral(variable = r("y")))), variable = r("x")), 2)
        ed.enter(listOf(0, 2))
        ed.type("1")
        assertEquals("int{||int{||'1;|'y;}|'x;}", MathCodec.encode(ed.root))
    }

    // ---- Choosing the variable: edit the x in dx / ∂x like any other box
    private fun evalEditor(ed: com.example.cas.editor.Editor) = cas(ed.root)

    @Test fun derivativeWithRespectToY() {
        val ed = com.example.cas.editor.Editor()
        ed.insert(Derivative(variable = r("x"), partial = true), 1)   // the ∂/∂x key: cursor in the body
        "x²y³".forEach { c -> if (c == '²') ed.insertPower(row("2")) else if (c == '³') ed.insertPower(row("3")) else ed.type(c.toString()) }
        // Tap the x in ∂x (here: put the cursor there), delete it, type y.
        val d = ed.root.items[0] as Derivative
        ed.setCursor(d.variable, 1); ed.backspace(); ed.type("y")
        assertEquals("3x^2y^2", evalEditor(ed))
    }
    @Test fun leftArrowFromBodyReachesTheVariable() {
        val ed = com.example.cas.editor.Editor()
        ed.insert(Derivative(variable = r("x")), 1)
        ed.moveLeft()
        assertTrue(ed.row === (ed.root.items[0] as Derivative).variable)
    }
    @Test fun integralInT() = assertEquals("t^3/3", cas(row(Integral(body = r("t²"), variable = r("t")))))
    @Test fun integralTreatsOtherLettersAsConstants() = assertEquals("x^2y", cas(row(Integral(body = r("2xy"), variable = r("x")))))
    @Test fun integralInYOfTheSameBody() = assertEquals("xy^2", cas(row(Integral(body = r("2xy"), variable = r("y")))))
    @Test fun greekVariable() = assertEquals("-cos(θ)", cas(row(Integral(body = row(Func("sin", listOf(r("θ")))), variable = r("θ")))))
    @Test fun derivativeInT() = assertEquals("2t", cas(row(Derivative(r("t"), r("t²")))))
    @Test fun doubleIntegralOtherOrder() {
        // ∫₀¹ ∫₀ʸ x dx dy: inner over x up to y, outer over y.
        assertEquals("1/6", cas(row(Integral(r("0"), r("1"), row(Integral(r("0"), r("y"), r("x"), r("x"))), r("y")))))
    }
    @Test fun variableMustBeOneLetter() = assertEquals("Error: Use a single letter as the variable", cas(row(Derivative(r("xy"), r("x²")))))

    // Letters that aren't this line's coordinates get sliders: y = 3t has a slider for t.
    @Test fun explicitLineGivesTASlider() = assertEquals(listOf("t"), spec(r("y=3t")).parameters)
    @Test fun bareFunctionOfXWithSliders() = assertEquals(listOf("a", "k"), spec(r("ax+k")).parameters)
    @Test fun implicitCircleWithRadiusSlider() = assertEquals(listOf("r"), spec(r("x²+y²=r")).parameters)

    // A matrix applied to a vector is a vector, so it can be plotted as an arrow.
    @Test fun matrixTimesVectorIsAVector() {
        val m = com.example.cas.editor.Matrix(2, 2, listOf(r("0"), r("−1"), r("1"), r("0")))
        val v = com.example.cas.editor.Matrix(2, 1, listOf(r("3"), r("1")))
        assertEquals("[[-1],[3]]", com.example.cas.cas.Printer.plain(Evaluator().evaluate(row(m, v))))
    }
    @Test fun rotatedVectorWithASlider() {
        val m = com.example.cas.editor.Matrix(2, 2, listOf(row(Func("cos", listOf(r("a")))), row(Sym("−"), Func("sin", listOf(r("a")))), row(Func("sin", listOf(r("a")))), row(Func("cos", listOf(r("a"))))))
        val v = com.example.cas.editor.Matrix(2, 1, listOf(r("1"), r("0")))
        val e = Evaluator().evaluate(row(m, v))
        assertEquals("[[cos(a)],[sin(a)]]", com.example.cas.cas.Printer.plain(e))
    }

    // Aᴴ is a raised H: the conjugate transpose.
    @Test fun hermitianConjugate() {
        val m = com.example.cas.editor.Matrix(2, 2, listOf(r("1"), r("i"), r("2"), r("3")))
        assertEquals("[[1,2],[-i,3]]", com.example.cas.cas.Printer.plain(Evaluator().evaluate(row(m, Pow(r("H"))))))
    }
    @Test fun hadamardProduct() {
        val a = com.example.cas.editor.Matrix(2, 2, listOf(r("1"), r("2"), r("3"), r("4")))
        val b = com.example.cas.editor.Matrix(2, 2, listOf(r("5"), r("6"), r("7"), r("8")))
        assertEquals("[[5,12],[21,32]]", com.example.cas.cas.Printer.plain(Evaluator().evaluate(row(Func("hadamard", listOf(row(a), row(b)))))))
    }
    @Test fun kroneckerProduct() {
        val a = com.example.cas.editor.Matrix(2, 1, listOf(r("1"), r("2")))
        val b = com.example.cas.editor.Matrix(1, 2, listOf(r("3"), r("4")))
        assertEquals("[[3,4],[6,8]]", com.example.cas.cas.Printer.plain(Evaluator().evaluate(row(Func("kron", listOf(row(a), row(b)))))))
    }
}
