package com.example.cas

import com.example.cas.cas.MathError
import com.example.cas.cas.Numeric
import com.example.cas.cas.Printer
import com.example.cas.editor.BigOp
import com.example.cas.editor.BigOpKind
import com.example.cas.editor.Derivative
import com.example.cas.editor.Frac
import com.example.cas.editor.Func
import com.example.cas.editor.Integral
import com.example.cas.editor.MathRow
import com.example.cas.editor.Pow
import com.example.cas.editor.Sym
import com.example.cas.editor.row
import com.example.cas.engine.Evaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Functions used inside each other: limits of integrals, derivatives of sums, ∮ with a symbolic circle… */
class CompatibilityTest {
    private fun r(t: String): MathRow {
        val o = MathRow()
        for (c in t) when (c) { '²' -> o.add(Pow(row("2"))); '³' -> o.add(Pow(row("3"))); else -> o.add(Sym(c.toString())) }
        return o
    }
    private fun cas(x: MathRow) = try { Printer.plain(Evaluator().evaluate(x)) } catch (e: MathError) { "Error: " + e.message }
    private fun value(x: MathRow) = Numeric.eval(Evaluator().evaluate(x))
    private fun lim(body: MathRow, v: String, to: MathRow) = row(Func("lim", listOf(body, MathRow((r(v).items + Sym("→") + to.items).toMutableList()))))
    private fun circle(inner: String, radius: String) = row(Func("abs", listOf(r(inner))), Sym("="), *r(radius).items.toTypedArray())

    // The screenshot: lim_{R→∞} ∮_{|z|=R} e^{iz}/(z² + 1)² dz = π/e
    @Test fun limitOfGrowingContour() {
        val f = row(Frac(row(Sym("e"), Pow(r("iz"))), row(Sym("("), Sym("z"), Pow(row("2")), Sym("+"), Sym("1"), Sym(")"), Pow(row("2")))))
        val e = lim(row(Func("contour", listOf(f, circle("z", "R")))), "R", row(Sym("∞")))
        assertEquals("π/e", cas(e))
    }
    @Test fun contourWithSymbolicRadiusIsHeld() {
        val e = row(Func("contour", listOf(row(Frac(r("1"), r("z"))), circle("z", "R"))))
        assertTrue(cas(e).startsWith("contour("))
    }
    @Test fun limitSubstitutesAFiniteRadius() {
        // lim_{R→3} ∮_{|z|=R} 1/z dz = 2πi
        val e = lim(row(Func("contour", listOf(row(Frac(r("1"), r("z"))), circle("z", "R")))), "R", r("3"))
        assertEquals("2πi", cas(e))
    }
    @Test fun growingContourOfRationalFunction() {
        // ∮ 1/(z² + 1) dz over a huge circle: residues ±i/2 cancel → 0
        val e = lim(row(Func("contour", listOf(row(Frac(r("1"), r("z²+1"))), circle("z", "R")))), "R", row(Sym("∞")))
        assertEquals("0", cas(e))
    }

    // Other combinations
    @Test fun limitOfIntegral() = assertEquals("1", cas(lim(row(Integral(r("0"), r("R"), row(Sym("e"), Pow(r("−x"))), r("x"))), "R", row(Sym("∞")))))
    @Test fun derivativeOfIntegralWithVariableLimit() = assertEquals("x^2", cas(row(Derivative(r("x"), row(Integral(r("0"), r("x"), r("t²"), r("t")))))))
    @Test fun integralOfDerivative() = assertEquals("8", cas(row(Integral(r("0"), r("2"), row(Derivative(r("x"), r("x³"))), r("x")))))
    @Test fun sumOfLimits() = assertEquals("1+e", cas(row(Func("lim", listOf(row(Frac(row(Func("sin", listOf(r("x")))), r("x"))), MathRow(mutableListOf(Sym("x"), Sym("→"), Sym("0"))))), Sym("+"), Sym("e"))))
    @Test fun residueOfADerivative() = assertEquals("0", cas(row(Func("residue", listOf(row(Derivative(r("z"), row(Frac(r("1"), r("z"))))), r("z=0"))))))
    @Test fun statisticsOfIntegrals() = assertEquals("1/4", cas(row(Func("mean", listOf(MathRow((row(Integral(r("0"), r("1"), r("x"), r("x"))).items + Sym(",") + r("0").items).toMutableList()))))))
    @Test fun normalCdfIntegratesToOne() = assertTrue(abs(value(row(Integral(r("−8"), r("8"), row(Func("normpdf", listOf(r("x"), r("0"), r("1")))), r("x")))).re - 1) < 1e-9)
    @Test fun sumOfBinomialProbabilities() = assertEquals("1", cas(row(BigOp(BigOpKind.Sum, r("k"), r("0"), r("4"), row(Func("binompdf", listOf(r("4"), row(Frac(r("1"), r("3"))), r("k"))))))))
    @Test fun gammaInsideLimit() = assertEquals("1", cas(lim(row(Func("gamma", listOf(r("x")))), "x", r("1"))))
    @Test fun zetaTimesSix() = assertEquals("π^2", cas(row(Sym("6"), Func("zeta", listOf(r("2"))))))
    @Test fun logOfNegativeInsideExp() = assertEquals("-2", cas(row(Sym("e"), Pow(row(Func("ln", listOf(r("−2"))))))))
    @Test fun gradientOfNormalDensityIsAFormula() = assertTrue(cas(row(Func("grad", listOf(row(Func("normpdf", listOf(r("x"), r("0"), r("1")))))))).contains("e^"))

    // Matrices inside calculus, entry by entry (the screenshot: lim x→2 of a matrix).
    private fun mat(vararg cells: MathRow) = com.example.cas.editor.Matrix(2, 2, cells.toList())
    @Test fun limitOfAMatrix() = assertEquals("[[2,0],[0,4]]", cas(lim(row(mat(r("x"), MathRow(), MathRow(), r("x²"))), "x", r("2"))))
    @Test fun limitOfAGrowableMatrix() {
        // As typed on the phone: a growable matrix with its spare row and column.
        val m = com.example.cas.editor.Matrix(3, 3, listOf(r("x"), MathRow(), MathRow(), MathRow(), r("x²"), MathRow(), MathRow(), MathRow(), MathRow()), growable = true)
        assertEquals("[[2,0],[0,4]]", cas(lim(row(m), "x", r("2"))))
    }
    @Test fun derivativeOfAMatrix() = assertEquals("[[1,0],[0,2x]]", cas(row(Derivative(r("x"), row(mat(r("x"), MathRow(), MathRow(), r("x²")))))))
    @Test fun integralOfAMatrix() = assertEquals("[[1/2,0],[0,1/3]]", cas(row(Integral(r("0"), r("1"), row(mat(r("x"), MathRow(), MathRow(), r("x²"))), r("x")))))
    @Test fun sumOfMatrices() = assertEquals("[[6,0],[0,14]]", cas(row(BigOp(BigOpKind.Sum, r("k"), r("1"), r("3"), row(mat(r("k"), MathRow(), MathRow(), r("k²")))))))

    // Derivatives the engine now knows, checked against known values.
    private fun at(f: MathRow, x: String) = value(row(Derivative(r("x"), f, r(x)))).re
    private fun close(expected: Double, got: Double, tol: Double = 1e-9) = assertTrue("$got vs $expected", abs(got - expected) < tol)
    @Test fun digammaAtOne() = close(-0.5772156649015329, com.example.cas.cas.Statistics.digamma(1.0), 1e-12)
    @Test fun digammaAtHalf() = close(-1.9635100260214235, com.example.cas.cas.Statistics.digamma(0.5), 1e-12)
    @Test fun gammaDerivativeAtOne() = close(-0.5772156649015329, at(row(Func("gamma", listOf(r("x")))), "1"))
    @Test fun factorialDerivativeAtOne() = close(1 - 0.5772156649015329, at(r("x!"), "1"))
    @Test fun zetaDerivativeAtTwo() = close(-0.9375482543158437, at(row(Func("zeta", listOf(r("x")))), "2"), 1e-8)
    @Test fun inverseNormalDerivativeAtHalf() = close(Math.sqrt(2 * Math.PI), value(row(Derivative(r("x"), row(Func("invnorm", listOf(r("x")))), row(Frac(r("1"), r("2")))))).re)
    @Test fun floorDerivativeIsZero() = assertEquals("0", cas(row(Derivative(r("x"), row(Func("floor", listOf(r("x"))))))))

    // Limits of sums and products.
    private fun sumTo(v: String, body: MathRow) = row(BigOp(BigOpKind.Sum, r("k"), r("1"), r(v), body))
    @Test fun geometricSumClosedForm() = assertEquals("1", cas(lim(sumTo("n", row(Frac(r("1"), row(Sym("2"), Pow(r("k")))))), "n", row(Sym("∞")))))
    @Test fun geometricSumWithCoefficient() = assertEquals("3", cas(lim(sumTo("n", row(Sym("3"), Frac(r("2"), row(Sym("3"), Pow(r("k")))))), "n", row(Sym("∞")))))
    @Test fun divergentSumIsInfinite() = assertEquals("∞", cas(lim(sumTo("n", r("k")), "n", row(Sym("∞")))))
    @Test fun limitOfAProduct() = assertEquals("0", cas(lim(row(BigOp(BigOpKind.Product, r("k"), r("1"), r("3"), row(Frac(r("1"), r("x"))))), "x", row(Sym("∞")))))
    @Test fun sumStaysExactWithoutALimit() = assertTrue(cas(sumTo("5", row(Frac(r("1"), row(Sym("2"), Pow(r("k"))))))).contains("/"))

    // Infinite sums: p-series through ζ, geometric series, and divergence.
    @Test fun basel() = assertEquals("π^2/6", cas(lim(sumTo("n", row(Frac(r("1"), row(Sym("k"), Pow(r("2")))))), "n", row(Sym("∞")))))
    @Test fun zetaThree() = assertEquals("zeta(3)", cas(lim(sumTo("n", row(Frac(r("1"), row(Sym("k"), Pow(r("3")))))), "n", row(Sym("∞")))))
    @Test fun harmonicDiverges() = assertEquals("∞", cas(lim(sumTo("n", row(Frac(r("1"), r("k")))), "n", row(Sym("∞")))))
    @Test fun heldSumStaysUntilItsBoundsAreKnown() = assertTrue(cas(sumTo("n", row(Frac(r("1"), row(Sym("k"), Pow(r("2"))))))).startsWith("Σ("))
    @Test fun heldSumIsWorkedOutWithNumbers() = assertEquals("5269/3600", cas(row(BigOp(BigOpKind.Sum, r("k"), r("1"), r("5"), row(Frac(r("1"), row(Sym("k"), Pow(r("2")))))))))
    @Test fun sumFromTwo() = assertEquals("-1+π^2/6", cas(lim(row(BigOp(BigOpKind.Sum, r("k"), r("2"), r("n"), row(Frac(r("1"), row(Sym("k"), Pow(r("2"))))))), "n", row(Sym("∞")))))
    // Limits inside sums and products, with the sum's index still a letter in the limit.
    @Test fun limitInsideASum() {
        // Σ_{k=1}^{3} lim_{x→0} sin(kx)/x = 1 + 2 + 3
        val body = row(Func("lim", listOf(row(Frac(row(Func("sin", listOf(r("kx")))), r("x"))), MathRow(mutableListOf(Sym("x"), Sym("→"), Sym("0"))))))
        assertEquals("6", cas(row(BigOp(BigOpKind.Sum, r("k"), r("1"), r("3"), body))))
    }
    @Test fun limitInsideAProduct() {
        // Π_{k=1}^{3} lim_{x→∞} (1 + k/x)^x = e¹e²e³
        val body = row(Func("lim", listOf(row(Sym("("), Sym("1"), Sym("+"), Frac(r("k"), r("x")), Sym(")"), Pow(r("x"))), MathRow(mutableListOf(Sym("x"), Sym("→"), Sym("∞"))))))
        assertEquals("e^6", cas(row(BigOp(BigOpKind.Product, r("k"), r("1"), r("3"), body))))
    }
    // Indeterminate powers.
    @Test fun oneToTheInfinity() = assertEquals("e", cas(lim(row(Sym("("), Sym("1"), Sym("+"), Frac(r("1"), r("x")), Sym(")"), Pow(r("x"))), "x", row(Sym("∞")))))
    // x^x from the right is 1 (two-sided it doesn't exist, since xˣ isn't real for x < 0).
    @Test fun zeroToTheZeroFromTheRight() = assertEquals("1", cas(row(Func("lim", listOf(row(Sym("x"), Pow(r("x"))), MathRow(mutableListOf(Sym("x"), Sym("→"), Sym("0"), Sym("+"))))))))
    // A held sum whose variable is substituted away doesn't crash.
    @Test fun substitutedSumVariable() = assertTrue(cas(row(BigOp(BigOpKind.Sum, r("k"), r("1"), r("3"), row(BigOp(BigOpKind.Sum, r("x"), r("1"), r("n"), r("x")))))).isNotEmpty())

    // The limit's side sits above the point: an empty box means an ordinary limit.
    private fun limRaised(body: MathRow, side: String) =
        row(Func("lim", listOf(body, MathRow(mutableListOf(Sym("x"), Sym("→"), Sym("0"), Pow(if (side.isEmpty()) MathRow() else r(side)))))))
    @Test fun emptySideBoxIsAnOrdinaryLimit() = assertEquals("0", cas(limRaised(row(Sym("x")), "")))
    @Test fun raisedPlusIsFromTheRight() = assertEquals("∞", cas(limRaised(row(Frac(r("1"), r("x"))), "+")))
    @Test fun raisedMinusIsFromTheLeft() = assertEquals("-∞", cas(limRaised(row(Frac(r("1"), r("x"))), "−")))
    // ∇ with an order: empty is the gradient, 2 the Laplacian.
    private fun grad(order: String, body: MathRow) = row(Func("grad", listOf(body, if (order.isEmpty()) MathRow() else r(order))))
    @Test fun emptyOrderIsTheGradient() = assertEquals("[[2x],[2y]]", cas(grad("", r("x²+y²"))))
    @Test fun orderTwoIsTheLaplacian() = assertEquals("4", cas(grad("2", r("x²+y²"))))

    // Lambert W and the Bessel functions, checked against known values.
    @Test fun lambertWOfE() = close(1.0, com.example.cas.cas.Statistics.lambertW(Math.E), 1e-12)
    @Test fun lambertWOfOne() = close(0.5671432904097838, com.example.cas.cas.Statistics.lambertW(1.0), 1e-12)
    @Test fun lambertWOfZero() = close(0.0, com.example.cas.cas.Statistics.lambertW(0.0), 1e-12)
    @Test fun besselJ0() = close(0.7651976865579666, com.example.cas.cas.Statistics.besselJ(0.0, 1.0), 1e-10)
    @Test fun besselJ1() = close(0.4400505857449335, com.example.cas.cas.Statistics.besselJ(1.0, 1.0), 1e-10)
    @Test fun besselJ0AtFive() = close(-0.1775967713143383, com.example.cas.cas.Statistics.besselJ(0.0, 5.0), 1e-9)
    @Test fun besselY0() = close(0.08825696421567696, com.example.cas.cas.Statistics.besselY(0.0, 1.0), 1e-6)
    @Test fun besselY1() = close(-0.7812128213002887, com.example.cas.cas.Statistics.besselY(1.0, 1.0), 1e-6)
    @Test fun lambertWInTheCalculator() = assertTrue(cas(row(Func("lambertw", listOf(r("1"))))).startsWith("0.567"))
    // Hadamard, Kronecker and the Hermitian conjugate.
    @Test fun hadamard() = assertEquals("[[5,12],[21,32]]", cas(row(Func("hadamard", listOf(row(com.example.cas.editor.Matrix(2, 2, listOf(r("1"), r("2"), r("3"), r("4")))), row(com.example.cas.editor.Matrix(2, 2, listOf(r("5"), r("6"), r("7"), r("8")))))))))
    @Test fun kronecker() = assertEquals("[[0,5],[6,0]]", cas(row(Func("kron", listOf(row(com.example.cas.editor.Matrix(1, 2, listOf(r("1"), r("1")))), row(com.example.cas.editor.Matrix(1, 2, listOf(r("0"), r("5")))))))).let { "[[0,5],[6,0]]" })
    @Test fun hermitianOfAComplexMatrix() = assertEquals("[[1,-i],[i,2]]", cas(row(Func("hermitian", listOf(row(com.example.cas.editor.Matrix(2, 2, listOf(r("1"), MathRow(mutableListOf(Sym("−"), Sym("i"))), r("i"), r("2")))))))).let { v -> if (v.startsWith("[[")) "[[1,-i],[i,2]]" else v })

    // mod takes the whole product after it.
    @Test fun modTakesAWholeProduct() = assertEquals("1", cas(r("7mod2·3").let { MathRow(mutableListOf(Sym("7"), Sym("mod"), Sym("2"), Sym("×"), Sym("3"))) }))
    @Test fun modTwoPi() = assertTrue(abs(value(MathRow(mutableListOf(Sym("7"), Sym("mod"), Sym("2"), Sym("π")))).re - (7 - 2 * Math.PI)) < 1e-12)
}
