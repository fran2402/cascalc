package com.example.cas

import com.example.cas.cas.CoordinateKind
import com.example.cas.cas.Coordinates
import com.example.cas.cas.MathError
import com.example.cas.cas.Printer
import com.example.cas.editor.Frac
import com.example.cas.editor.Func
import com.example.cas.editor.MathRow
import com.example.cas.editor.Matrix
import com.example.cas.editor.Pow
import com.example.cas.editor.Sym
import com.example.cas.editor.row
import com.example.cas.engine.Evaluator
import org.junit.Assert.assertEquals
import org.junit.Test

class CoordinatesTest {
    private fun r(text: String): MathRow {
        val out = MathRow()
        for (c in text) when (c) { '²' -> out.add(Pow(row("2"))); '³' -> out.add(Pow(row("3"))); else -> out.add(Sym(c.toString())) }
        return out
    }
    private val cyl = Coordinates(CoordinateKind.Cylindrical)
    private val sph = Coordinates(CoordinateKind.Spherical)
    private fun cas(x: MathRow, c: Coordinates) = try { Printer.plain(Evaluator(coordinates = c).evaluate(x)) } catch (e: MathError) { "Error: " + e.message }
    private fun f(name: String, a: MathRow) = row(Func(name, listOf(a)))
    private fun vec(vararg cells: MathRow) = row(Matrix(cells.size, 1, cells.toList()))
    private fun sinTheta() = row(Func("sin", listOf(r("θ"))))

    // ---- Cylindrical (r, θ, z), and plane polar when there's no z
    @Test fun cylGradient() = assertEquals("[[2r],[0],[1]]", cas(f("grad", r("r²+z")), cyl))
    @Test fun polarGradientOfAngle() = assertEquals("[[0],[1/r]]", cas(f("grad", r("θ")), cyl))
    @Test fun cylDivergenceOfRadial() = assertEquals("2", cas(f("div", vec(r("r"), r("0"))), cyl))
    @Test fun cylDivergence3d() = assertEquals("3", cas(f("div", vec(r("r"), r("0"), r("z"))), cyl))
    @Test fun rigidRotationCurl() = assertEquals("2", cas(f("curl", vec(r("0"), r("r"))), cyl)) // same as (−y, x) in Cartesian
    @Test fun rigidRotationCurl3d() = assertEquals("[[0],[0],[2]]", cas(f("curl", vec(r("0"), r("r"), r("0"))), cyl))
    @Test fun lineVortexIsIrrotational() = assertEquals("0", cas(f("curl", vec(r("0"), row(Frac(r("1"), r("r"))))), cyl))
    @Test fun lnRIsHarmonic() = assertEquals("0", cas(f("laplacian", row(Func("ln", listOf(r("r"))))), cyl))
    @Test fun cylLaplacian() = assertEquals("4", cas(f("laplacian", r("r²")), cyl))

    // ---- Spherical (r, θ, φ), θ from the z-axis
    @Test fun sphGradientOfRSquared() = assertEquals("[[2r],[0],[0]]", cas(f("grad", r("r²")), sph))
    @Test fun sphGradientOfTheta() = assertEquals("[[0],[1/r],[0]]", cas(f("grad", r("θ")), sph))
    @Test fun sphGradientOfPhi() = assertEquals("[[0],[0],[1/(rsin(θ))]]", cas(f("grad", r("φ")), sph))
    @Test fun sphDivergenceOfRadial() = assertEquals("3", cas(f("div", vec(r("r"), r("0"), r("0"))), sph))
    @Test fun inverseSquareFieldIsDivergenceFree() = assertEquals("0", cas(f("div", vec(row(Frac(r("1"), r("r²"))), r("0"), r("0"))), sph))
    @Test fun oneOverRIsHarmonic() = assertEquals("0", cas(f("laplacian", row(Frac(r("1"), r("r")))), sph))
    @Test fun sphLaplacianOfRSquared() = assertEquals("6", cas(f("laplacian", r("r²")), sph))
    @Test fun curlOfGradientIsZeroInSpherical() {
        val f = row(Sym("r"), Pow(row("2")), Func("cos", listOf(r("θ"))), Sym("φ"))
        val expr = row(Func("curl", listOf(row(Func("grad", listOf(f))))))
        assertEquals("[[0],[0],[0]]", cas(expr, sph))
    }
    @Test fun sphCurlOfRotation() {
        // Rotation about the z-axis has F_φ = r sin θ; its curl is 2 along z = (2cos θ, −2sin θ, 0).
        assertEquals("[[2cos(θ)],[-2sin(θ)],[0]]", cas(f("curl", vec(r("0"), r("0"), row(Sym("r"), Func("sin", listOf(r("θ")))))), sph))
    }
    @Test fun sphericalNeedsThreeComponents() = assertEquals("Error: Spherical coordinates need three components", cas(f("div", vec(r("r"), r("0"))), sph))

    // ---- Your own letters
    @Test fun customCylindricalNames() = assertEquals("[[2ρ],[0]]", cas(f("grad", r("ρ²")), Coordinates(CoordinateKind.Cylindrical, listOf("ρ", "φ", "z"))))
    @Test fun customCartesianNames() = assertEquals("[[v],[u]]", cas(f("grad", r("uv")), Coordinates(CoordinateKind.Cartesian, listOf("u", "v", "w"))))
    @Test fun jacobianInChosenLetters() {
        // The polar-to-Cartesian map (r cos θ, r sin θ).
        val m = Matrix(2, 1, listOf(row(Sym("r"), Func("cos", listOf(r("θ")))), row(Sym("r"), Func("sin", listOf(r("θ"))))))
        assertEquals("[[cos(θ),-rsin(θ)],[sin(θ),rcos(θ)]]", cas(row(Func("jacobian", listOf(row(m)))), cyl))
    }
    @Test fun lettersMustDiffer() = assertEquals("Three different coordinate letters are needed",
        runCatching { Coordinates(CoordinateKind.Cartesian, listOf("x", "x", "z")) }.exceptionOrNull()?.message)

    // ---- The previous answer
    @Test fun ansIsThePreviousAnswer() {
        val ans = Evaluator().evaluate(r("6×7"))
        assertEquals("43", Printer.plain(Evaluator(com.example.cas.engine.AngleUnit.Radians, ans).evaluate(row(Sym("ans"), Sym("+"), Sym("1")))))
    }
    @Test fun ansInLatex() = assertEquals("\\mathrm{Ans}+1", com.example.cas.engine.Latex.of(row(Sym("ans"), Sym("+"), Sym("1"))))
}
