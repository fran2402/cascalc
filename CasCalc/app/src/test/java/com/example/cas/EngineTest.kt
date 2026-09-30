package com.example.cas

import com.example.cas.cas.MathError
import com.example.cas.cas.Printer
import com.example.cas.editor.BigOp
import com.example.cas.editor.BigOpKind
import com.example.cas.editor.Binom
import com.example.cas.editor.Const
import com.example.cas.editor.Derivative
import com.example.cas.editor.Editor
import com.example.cas.editor.Frac
import com.example.cas.editor.Func
import com.example.cas.editor.Integral
import com.example.cas.editor.MathCodec
import com.example.cas.editor.MathRow
import com.example.cas.editor.Matrix
import com.example.cas.editor.Pow
import com.example.cas.editor.Root
import com.example.cas.editor.Sqrt
import com.example.cas.editor.Sym
import com.example.cas.editor.row
import com.example.cas.engine.AngleUnit
import com.example.cas.engine.Evaluator
import com.example.cas.engine.Formatter
import org.junit.Assert.assertEquals
import org.junit.Test

class EngineTest {
    /** Exact result as plain text, e.g. "x^2+2x+1", or "Error: …". */
    private fun cas(r: MathRow, angle: AngleUnit = AngleUnit.Radians, vars: Map<String, com.example.cas.cas.Expr> = emptyMap()): String = try {
        Printer.plain(Evaluator(angle, null, vars).evaluate(r))
    } catch (e: MathError) { "Error: " + e.message }

    /** Decimal form as plain text. */
    private fun approx(r: MathRow): String {
        val a = Formatter.answer(Evaluator().evaluate(r))
        return Formatter.plain(a.approx ?: a.exact)
    }

    /** Types into an editor. ÷ builds fractions, ^ opens an exponent, > moves right, r is √. */
    private fun type(keys: String): MathRow {
        val ed = Editor()
        for (c in keys) when (c) {
            '÷' -> ed.insertFraction()
            '^' -> ed.insertPower()
            '²' -> ed.insertPower(row("2"))
            '>' -> ed.moveRight()
            '<' -> ed.moveLeft()
            '⌫' -> ed.backspace()
            'r' -> ed.insert(Sqrt(), 0)
            else -> ed.type(c.toString())
        }
        return ed.root
    }
    private fun f(name: String, vararg args: MathRow) = Func(name, args.toList())
    private fun x2() = row(Sym("x"), Pow(row("2")))

    // ---- Exact arithmetic
    @Test fun exactFractions() = assertEquals("3/10", cas(type("0.1+0.2")))
    @Test fun fractionKey() = assertEquals("7/4", cas(type("7÷4")))
    @Test fun precedence() = assertEquals("14", cas(type("2+3×4")))
    @Test fun unaryMinusPower() = assertEquals("-4", cas(type("−2²")))
    @Test fun negativePower() = assertEquals("1/8", cas(type("2^−3")))
    @Test fun bigFactorial() = assertEquals("30414093201713378043612608166064768844377641568960512000000000000", cas(type("50!")))
    @Test fun surd() = assertEquals("2√2", cas(type("r8")))
    @Test fun rationalisedDenominator() = assertEquals("√2/2", cas(type("1÷r2")))
    @Test fun surdProduct() = assertEquals("√6", cas(type("r2>×r3")))
    @Test fun sqrtNegative() = assertEquals("2i", cas(type("r−4")))
    @Test fun cubeRoot() = assertEquals("-2", cas(row(Root(row("3"), row("−8")))))
    @Test fun complexProduct() = assertEquals("11-2i", cas(row(f("expand", type("(3+4i)(1−2i)")))))
    @Test fun iSquared() = assertEquals("-1", cas(type("i²")))
    @Test fun exactPi() = assertEquals("2π", cas(type("2π")))
    @Test fun piDecimal() = assertEquals("6.283185307", approx(type("2π")))
    @Test fun percent() = assertEquals("3/20", cas(type("15%")))
    @Test fun mod() = assertEquals("2", cas(row(Sym("1"), Sym("7"), Sym("mod"), Sym("5"))))
    @Test fun divideByZero() = assertEquals("Error: Can't divide by 0", cas(type("1÷0")))
    @Test fun emptySlot() = assertEquals("Error: Fill in the empty box", cas(type("1÷")))

    // ---- Symbolic algebra
    @Test fun likeTerms() = assertEquals("5x", cas(type("2x+3x")))
    @Test fun powersCombine() = assertEquals("x^3", cas(row(Sym("x"), Sym("×"), Sym("x"), Pow(row("2")))))
    @Test fun cancelsToOne() = assertEquals("1", cas(type("x÷x")))
    @Test fun expand() = assertEquals("x^2+2x+1", cas(row(f("expand", row(Sym("("), Sym("x"), Sym("+"), Sym("1"), Sym(")"), Pow(row("2")))))))
    @Test fun expandCube() = assertEquals("a^3-3a^2b+3ab^2-b^3", cas(row(f("expand", row(Sym("("), Sym("a"), Sym("−"), Sym("b"), Sym(")"), Pow(row("3")))))))
    @Test fun factorQuadratic() = assertEquals("(x-3)(x-2)", cas(row(f("factor", row(Sym("x"), Pow(row("2")), Sym("−"), Sym("5"), Sym("x"), Sym("+"), Sym("6"))))))
    @Test fun factorQuartic() = assertEquals("(x-2)(x-1)(x+1)(x+2)", cas(row(f("factor", row(Sym("x"), Pow(row("4")), Sym("−"), Sym("5"), Sym("x"), Pow(row("2")), Sym("+"), Sym("4"))))))
    @Test fun factorCommon() = assertEquals("2x(y+2)", cas(row(f("factor", type("2xy+4x")))))
    @Test fun factorWithContent() = assertEquals("2(x-1)(x+1)", cas(row(f("factor", row(Sym("2"), Sym("x"), Pow(row("2")), Sym("−"), Sym("2"))))))
    @Test fun simplifyCancels() = assertEquals("x+1", cas(row(f("simplify", row(Frac(row(Sym("x"), Pow(row("2")), Sym("−"), Sym("1")), row("x−1")))))))
    @Test fun storedVariable() = assertEquals("11", cas(type("2x+1"), vars = mapOf("x" to com.example.cas.cas.num(5))))
    @Test fun assignment() {
        val ev = Evaluator()
        assertEquals("a=5", Printer.plain(ev.evaluate(row(Sym("a"), Sym(":="), Sym("5")))))
        assertEquals("a", ev.assigned!!.first)
    }

    // ---- Solving
    private fun solve(eq: MathRow, v: String = "x") = cas(row(f("solve", eq, row(v))))
    @Test fun solveLinear() = assertEquals("x=3", solve(type("2x+1=7")))
    @Test fun solveQuadratic() = assertEquals("x=2, x=3", solve(row(Sym("x"), Pow(row("2")), Sym("−"), Sym("5"), Sym("x"), Sym("+"), Sym("6"), Sym("="), Sym("0"))))
    @Test fun solveIrrational() = assertEquals("x=-√2, x=√2", solve(row(Sym("x"), Pow(row("2")), Sym("="), Sym("2"))))
    @Test fun solveComplex() = assertEquals("x=-i, x=i", solve(row(Sym("x"), Pow(row("2")), Sym("+"), Sym("1"), Sym("="), Sym("0"))))
    @Test fun solveSymbolic() = assertEquals("x=-b/a", solve(type("ax+b=0")))
    @Test fun solveExponential() = assertEquals("x=2", solve(row(Sym("2"), Pow(row("x+1")), Sym("="), Sym("8"))))
    @Test fun solveTrig() = assertEquals("x=π/6, x=5π/6", solve(row(f("sin", row("x")), Sym("="), Frac(row("1"), row("2")))))
    @Test fun solveCubeRoot() = assertEquals("x=∛2", solve(row(Sym("x"), Pow(row("3")), Sym("="), Sym("2"))).substringBefore(","))
    @Test fun solveNumeric() = assertEquals("x=0.7390851332", solve(row(f("cos", row("x")), Sym("="), Sym("x"))))
    @Test fun solveRational() = assertEquals("x=3", solve(row(Frac(row(Sym("x"), Pow(row("2")), Sym("−"), Sym("9")), row("x+3")), Sym("="), Sym("0"))))
    @Test fun solveAutoVariable() = assertEquals("t=4", cas(row(f("solve", type("t−4=0"), MathRow()))))
    @Test fun noSolution() = assertEquals("Error: No solution", solve(type("x+1=x+2")))

    // ---- Calculus
    @Test fun derivativeSymbolic() = assertEquals("3x^2", cas(row(Derivative(row("x"), row(Sym("x"), Pow(row("3")))))))
    @Test fun derivativeChain() = assertEquals("2xcos(x^2)", cas(row(Derivative(row("x"), row(f("sin", x2()))))))
    @Test fun derivativeProduct() = assertEquals("xe^x+e^x", cas(row(Derivative(row("x"), row(Sym("x"), Sym("e"), Pow(row("x")))))))
    @Test fun derivativeAtPoint() = assertEquals("12", cas(row(Derivative(row("x"), row(Sym("x"), Pow(row("3"))), row("2")))))
    @Test fun derivativeCosAt() = assertEquals("-sin(1)", cas(row(Derivative(row("x"), row(f("cos", row("x"))), row("1")))))
    @Test fun indefinitePower() = assertEquals("x^3/3", cas(row(Integral(MathRow(), MathRow(), x2(), row("x")))))
    @Test fun indefiniteByParts() = assertEquals("xe^x-e^x", cas(row(Integral(MathRow(), MathRow(), row(Sym("x"), Sym("e"), Pow(row("x"))), row("x")))))
    @Test fun indefiniteLog() = assertEquals("ln(|x|)", cas(row(Integral(MathRow(), MathRow(), row(Frac(row("1"), row("x"))), row("x")))))
    @Test fun indefiniteAtan() = assertEquals("atan(x)", cas(row(Integral(MathRow(), MathRow(), row(Frac(row("1"), row(Sym("1"), Sym("+"), Sym("x"), Pow(row("2"))))), row("x")))))
    @Test fun indefiniteSubstitution() = assertEquals("e^(x^2)/2", cas(row(Integral(MathRow(), MathRow(), row(Sym("x"), Sym("e"), Pow(x2())), row("x")))))
    @Test fun definiteExact() = assertEquals("2", cas(row(Integral(row("0"), row("π"), row(f("sin", row("x"))), row("x")))))
    @Test fun definiteThird() = assertEquals("1/3", cas(row(Integral(row("0"), row("1"), x2(), row("x")))))
    @Test fun definiteSymbolicBound() = assertEquals("b^2/2", cas(row(Integral(row("0"), row("b"), row("x"), row("x")))))
    @Test fun definiteNumeric() = assertEquals("1.772453851", cas(row(Integral(row("−10"), row("10"), row(Sym("e"), Pow(row(Sym("−"), Sym("x"), Pow(row("2"))))), row("x")))))
    @Test fun taylor() = assertEquals("x^4/24+x^3/6+x^2/2+x+1", cas(row(f("taylor", row(Sym("e"), Pow(row("x"))), row("x=0"), row("4")))))
    @Test fun taylorSin() = assertEquals("x^5/120-x^3/6+x", cas(row(f("taylor", row(f("sin", row("x"))), row("x"), row("5")))))
    @Test fun sumNumeric() = assertEquals("5050", cas(row(BigOp(BigOpKind.Sum, row("k"), row("1"), row("100"), row("k")))))
    @Test fun sumClosedForm() = assertEquals("n(n+1)/2", cas(row(BigOp(BigOpKind.Sum, row("k"), row("1"), row("n"), row("k")))))
    @Test fun sumSquares() = assertEquals("n(n+1)(2n+1)/6", cas(row(BigOp(BigOpKind.Sum, row("k"), row("1"), row("n"), row(Sym("k"), Pow(row("2")))))))
    @Test fun baselSum() = assertEquals("9778141/6350400", cas(row(BigOp(BigOpKind.Sum, row("k"), row("1"), row("9"), row(Frac(row("1"), row(Sym("k"), Pow(row("2")))))))))
    @Test fun product() = assertEquals("120", cas(row(BigOp(BigOpKind.Product, row("n"), row("1"), row("5"), row("n")))))

    // ---- Functions
    @Test fun sinDegrees() = assertEquals("1/2", cas(row(f("sin", row("30"))), AngleUnit.Degrees))
    @Test fun asinDegrees() = assertEquals("60", cas(row(f("asin", row(Frac(row(Sqrt(row("3"))), row("2"))))), AngleUnit.Degrees))
    @Test fun cosExact() = assertEquals("-√2/2", cas(row(f("cos", row(Frac(row("3π"), row("4")))))))
    @Test fun lnE() = assertEquals("1", cas(row(f("ln", row("e")))))
    @Test fun logExact() = assertEquals("3", cas(row(f("log", row("2"), row("8")))))
    @Test fun binomial() = assertEquals("252", cas(row(Binom(row("10"), row("5")))))
    @Test fun gammaHalf() = assertEquals("0.8862269255", approx(type("0.5!")))
    @Test fun absComplex() = assertEquals("5", cas(row(f("abs", type("3+4i")))))

    // ---- Matrices
    private fun m22(a: String, b: String, c: String, d: String) = Matrix(2, 2, listOf(row(a), row(b), row(c), row(d)))
    @Test fun determinant() = assertEquals("-2", cas(row(f("det", row(m22("1", "2", "3", "4"))))))
    @Test fun symbolicDeterminant() = assertEquals("ad-bc", cas(row(f("det", row(m22("a", "b", "c", "d"))))))
    @Test fun inverse() = assertEquals("[[-2,1],[3/2,-1/2]]", cas(row(m22("1", "2", "3", "4"), Pow(row("−1")))))
    @Test fun transpose() = assertEquals("[[1,3],[2,4]]", cas(row(m22("1", "2", "3", "4"), Pow(row("T")))))
    @Test fun matrixProduct() = assertEquals("[[19,22],[43,50]]", cas(row(m22("1", "2", "3", "4"), Sym("×"), m22("5", "6", "7", "8"))))
    @Test fun singular() = assertEquals("Error: This matrix has no inverse", cas(row(m22("1", "2", "2", "4"), Pow(row("−1")))))

    // ---- Constants
    @Test fun speedOfLightSquared() = assertEquals("89875517873681764", cas(row(Const("c0"), Pow(row("2")))))

    // ---- Editor behavior
    @Test fun fractionTakesOperand() = assertEquals("'3;'+;frac{'1;'2;|'5;}", MathCodec.encode(type("3+12÷5")))
    @Test fun exitExponent() = assertEquals("'2;pow{'3;}'+;'1;", MathCodec.encode(type("2^3>+1")))
    @Test fun backspaceEmptyFraction() = assertEquals("'5;", MathCodec.encode(type("5+÷⌫⌫")))
    @Test fun backspaceUnwrapsSqrt() = assertEquals("'9;", MathCodec.encode(type("r9<⌫")))
    @Test fun codecRoundTrip() {
        val r = row(Integral(row("0"), row("1"), row(Frac(row("x"), row(Sqrt(row("2"))))), row("x")), Matrix(1, 2, listOf(row("1"), row(Const("c0")))))
        assertEquals(MathCodec.encode(r), MathCodec.encode(MathCodec.decode(MathCodec.encode(r))))
    }
    @Test fun answersReparse() {
        // Tapping an answer pastes its 2D form back into the input; it must evaluate to the same thing.
        for (input in listOf(type("r8"), type("7÷4"), row(f("expand", row(Sym("("), Sym("x"), Sym("+"), Sym("1"), Sym(")"), Pow(row("2"))))))) {
            val v = Evaluator().evaluate(input)
            val again = Evaluator().evaluate(Formatter.answer(v).exact)
            assertEquals(Printer.plain(v), Printer.plain(again))
        }
    }

    // ---- More CAS behavior
    @Test fun differenceOfSquares() = assertEquals("x^2-1", cas(row(f("expand", type("(x+1)(x−1)")))))
    @Test fun partialFractions() = assertEquals("ln(|x-1|)/2-ln(|x+1|)/2", cas(row(Integral(MathRow(), MathRow(), row(Frac(row("1"), row(Sym("x"), Pow(row("2")), Sym("−"), Sym("1")))), row("x")))))
    @Test fun trigSubstitution() = assertEquals("-cos(x)^2/2", cas(row(Integral(MathRow(), MathRow(), row(f("sin", row("x")), f("cos", row("x"))), row("x")))))
    @Test fun lnSquared() = assertEquals("ln(x)^2/2", cas(row(Integral(MathRow(), MathRow(), row(Frac(row(f("ln", row("x"))), row("x"))), row("x")))))
    @Test fun solveBiquadratic() = assertEquals("x=-2, x=-1, x=1, x=2", solve(row(Sym("x"), Pow(row("4")), Sym("−"), Sym("5"), Sym("x"), Pow(row("2")), Sym("+"), Sym("4"), Sym("="), Sym("0"))))
    @Test fun solveCubicRational() = assertEquals("x=1, x=2, x=3", solve(row(Sym("x"), Pow(row("3")), Sym("−"), Sym("6"), Sym("x"), Pow(row("2")), Sym("+"), Sym("1"), Sym("1"), Sym("x"), Sym("−"), Sym("6"), Sym("="), Sym("0"))))
    @Test fun exactTrigSum() = assertEquals("1", cas(row(f("sin", row("30")), Sym("+"), f("cos", row("60"))), AngleUnit.Degrees))
    @Test fun symbolicInverse() = assertEquals("[[d/(ad-bc),-b/(ad-bc)],[-c/(ad-bc),a/(ad-bc)]]", cas(row(m22("a", "b", "c", "d"), Pow(row("−1")))))

    // ---- Symbolic first, numerical fallback
    @Test fun derivativeNumericalFallback() = assertEquals("0.4227843351", cas(row(Derivative(row("x"), row(Sym("x"), Sym("!")), row("1")))))
    // x! = Γ(x + 1), so its derivative is Γ(x + 1) ψ(x + 1).
    @Test fun derivativeOfFactorialUsesDigamma() = assertEquals("digamma(x+1)gamma(x+1)", cas(row(Derivative(row("x"), row(Sym("x"), Sym("!"))))))
    @Test fun integralNumericalFallback() = assertEquals("0.7468241328", cas(row(Integral(row("0"), row("1"), row(Sym("e"), Pow(row(Sym("−"), Sym("x"), Pow(row("2"))))), row("x")))))
    @Test fun antiderivativeHint() = assertEquals("Error: No antiderivative found. Add limits for a numerical answer", cas(row(Integral(MathRow(), MathRow(), row(Sym("e"), Pow(row(Sym("−"), Sym("x"), Pow(row("2"))))), row("x")))))

    // ---- Linear algebra
    private fun m33(vararg c: String) = Matrix(3, 3, c.map { row(it) })
    @Test fun eigenvalues() = assertEquals("λ=1, λ=3", cas(row(f("eigvals", row(m22("2", "1", "1", "2"))))))
    @Test fun eigenvaluesIrrational() = assertEquals("λ=1-√2, λ=1+√2", cas(row(f("eigvals", row(m22("1", "1", "1", "1")))).let { row(f("eigvals", row(m22("0", "1", "1", "2")))) }))
    @Test fun eigenvaluesComplex() = assertEquals("λ=-i, λ=i", cas(row(f("eigvals", row(m22("0", "−1", "1", "0"))))))
    @Test fun eigenvectors() = assertEquals("λ=1, v=[[-1],[1]], λ=3, v=[[1],[1]]", cas(row(f("eigvecs", row(m22("2", "1", "1", "2"))))))
    @Test fun eigenvectorsRepeated() = assertEquals("λ=2, v=[[1],[0]], [[0],[1]]", cas(row(f("eigvecs", row(m22("2", "0", "0", "2"))))))
    @Test fun eigenvectors3x3() = assertEquals("λ=1, v=[[1],[0],[0]], λ=2, v=[[1],[1],[0]], λ=3, v=[[1],[2],[1]]",
        cas(row(f("eigvecs", row(m33("1", "1", "0", "0", "2", "2", "0", "0", "3"))))))
    @Test fun charpoly() = assertEquals("λ^2-4λ+3", cas(row(f("charpoly", row(m22("2", "1", "1", "2"))))))
    @Test fun trace() = assertEquals("a+d", cas(row(f("trace", row(m22("a", "b", "c", "d"))))))
    @Test fun rref() = assertEquals("[[1,0,-1],[0,1,2],[0,0,0]]", cas(row(f("rref", row(m33("1", "2", "3", "4", "5", "6", "7", "8", "9"))))))
    @Test fun rank() = assertEquals("2", cas(row(f("rank", row(m33("1", "2", "3", "4", "5", "6", "7", "8", "9"))))))
    @Test fun dot() = assertEquals("32", cas(row(f("dot", row(Matrix(1, 3, listOf(row("1"), row("2"), row("3")))), row(Matrix(1, 3, listOf(row("4"), row("5"), row("6"))))))))
    @Test fun cross() = assertEquals("[[-3,6,-3]]", cas(row(f("cross", row(Matrix(1, 3, listOf(row("1"), row("2"), row("3")))), row(Matrix(1, 3, listOf(row("4"), row("5"), row("6"))))))))
    @Test fun norm() = assertEquals("5", cas(row(f("abs", row(Matrix(1, 2, listOf(row("3"), row("4"))))))))

    // ---- Limits and partial fractions
    private fun lim(body: MathRow, at: String) = cas(row(f("lim", body, row(at))))
    @Test fun limitSinc() = assertEquals("1", lim(row(Frac(row(f("sin", row("x"))), row("x"))), "x=0"))
    @Test fun limitCancel() = assertEquals("2", lim(row(Frac(row(Sym("x"), Pow(row("2")), Sym("−"), Sym("1")), row("x−1"))), "x=1"))
    @Test fun limitDirect() = assertEquals("5", lim(type("2x+1"), "x=2"))
    @Test fun limitLHopitalTwice() = assertEquals("1/2", lim(row(Frac(row(Sym("1"), Sym("−"), f("cos", row("x"))), row(Sym("x"), Pow(row("2"))))), "x=0"))
    @Test fun limitDoesNotExist() = assertEquals("Error: The limits from the left and right differ", lim(row(Frac(row(f("abs", row("x"))), row("x"))), "x=0"))
    @Test fun apart() = assertEquals("1/(2(x-1))-1/(2(x+1))", cas(row(f("apart", row(Frac(row("1"), row(Sym("x"), Pow(row("2")), Sym("−"), Sym("1"))))))))
    @Test fun apartWithPolynomialPart() = assertEquals("x+1/(x-1)", cas(row(f("apart", row(Frac(row(Sym("x"), Pow(row("2")), Sym("−"), Sym("x"), Sym("+"), Sym("1")), row("x−1")))))))
    @Test fun together() = assertEquals("(2x+1)/(x(x+1))", cas(row(f("together", row(Frac(row("1"), row("x")), Sym("+"), Frac(row("1"), row("x+1")))))))

    @Test fun limitWithArrow() = assertEquals("1", cas(row(f("lim", row(Frac(row(f("sin", row("x"))), row("x"))), row(Sym("x"), Sym("→"), Sym("0"))))))
    @Test fun taylorWithArrow() = assertEquals("x^3/6+x^2/2+x+1", cas(row(f("taylor", row(Sym("e"), Pow(row("x"))), row(Sym("x"), Sym("→"), Sym("0")), row("3")))))
    @Test fun roundDisplayStaysRound() = assertEquals("round(x)", cas(row(f("round", row("x")))))
}
