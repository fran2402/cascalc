package com.example.cas

import com.example.cas.cas.MathError
import com.example.cas.cas.Printer
import com.example.cas.cas.Statistics
import com.example.cas.editor.Frac
import com.example.cas.editor.Func
import com.example.cas.editor.MathRow
import com.example.cas.editor.Pow
import com.example.cas.editor.Sym
import com.example.cas.editor.row
import com.example.cas.engine.Evaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class StatsResidueTest {
    private fun r(text: String): MathRow {
        val out = MathRow()
        for (c in text) when (c) { '²' -> out.add(Pow(row("2"))); '³' -> out.add(Pow(row("3"))); else -> out.add(Sym(c.toString())) }
        return out
    }
    private fun cas(x: MathRow) = try { Printer.plain(Evaluator().evaluate(x)) } catch (e: MathError) { "Error: " + e.message }
    private fun f(name: String, vararg a: MathRow) = row(Func(name, a.toList()))
    private fun near(expected: Double, text: String, tol: Double = 1e-9) = assertTrue("$text vs $expected", abs(text.toDouble() - expected) <= tol * maxOf(1.0, abs(expected)))

    // ---- Lists
    private val data = r("2,4,4,4,5,5,7,9")
    @Test fun mean() = assertEquals("5", cas(f("mean", data)))
    @Test fun meanExactFraction() = assertEquals("7/3", cas(f("mean", r("1,2,4"))))
    @Test fun medianEven() = assertEquals("9/2", cas(f("median", data)))
    @Test fun medianOdd() = assertEquals("4", cas(f("median", r("9,1,4"))))
    @Test fun populationSd() = assertEquals("2", cas(f("psd", data)))
    @Test fun sampleVariance() = assertEquals("32/7", cas(f("var", data)))
    @Test fun sampleSdExact() = assertEquals("4√14/7", cas(f("sd", data)))
    @Test fun statsOfSymbols() = assertEquals("(a+b)/2", cas(f("mean", r("a,b"))))

    // ---- Counting
    @Test fun permutations() = assertEquals("60", cas(f("perm", r("5"), r("3"))))
    @Test fun combinations() = assertEquals("10", cas(f("binom", r("5"), r("3"))))
    @Test fun binomialPmf() = assertEquals("5/16", cas(f("binompdf", r("5"), row(Frac(r("1"), r("2"))), r("2"))))

    // ---- The normal distribution
    @Test fun normalPdfAtZero() = assertEquals("√2/(2√π)", cas(f("normpdf", r("0"), r("0"), r("1"))))
    @Test fun normalPdfIsAFormula() = assertTrue(cas(f("normpdf", r("x"), r("0"), r("1"))).contains("e^"))
    @Test fun normalCdfAtMean() = assertEquals("1/2", cas(f("normcdf", r("0"), r("0"), r("1"))))
    @Test fun normalCdfOneSigma() = near(0.8413447460685429, Printer.plain(com.example.cas.cas.Numeric.approx(Evaluator().evaluate(f("normcdf", r("1"), r("0"), r("1"))))))
    @Test fun erfValues() {
        assertTrue(abs(Statistics.erf(0.5) - 0.5204998778130465) < 1e-15)
        assertTrue(abs(Statistics.erf(2.0) - 0.9953222650189527) < 1e-15)
        assertTrue(abs(Statistics.erf(4.0) - 0.9999999845827421) < 1e-15)
    }
    @Test fun inverseNormal() = assertTrue(abs(Statistics.inverseNormal(0.975) - 1.959963984540054) < 1e-13)
    @Test fun inverseNormalShown() = assertEquals("1.959963985", cas(f("invnorm", row(Frac(r("39"), r("40"))))))
    @Test fun inverseNormalHalf() = assertEquals("0", cas(f("invnorm", row(Frac(r("1"), r("2"))))))

    // ---- Contour integrals and residues
    private fun circle(spec: MathRow, body: MathRow) = cas(row(Func("contour", listOf(body, spec))))
    private fun absSpec(inner: String, radius: String) = row(Func("abs", listOf(r(inner))), Sym("="), *r(radius).items.toTypedArray())
    @Test fun contourOneOverZ() = assertEquals("2πi", circle(absSpec("z", "1"), row(Frac(r("1"), r("z")))))
    @Test fun contourAnalyticIsZero() = assertEquals("0", circle(absSpec("z", "1"), r("z²")))
    @Test fun contourAroundOnePole() = assertEquals("π", circle(absSpec("z−i", "1"), row(Frac(r("1"), r("z²+1")))))
    @Test fun contourAroundBothPoles() = assertEquals("0", circle(absSpec("z", "3"), row(Frac(r("1"), r("z²+1")))))
    @Test fun contourDoublePole() = assertEquals("πi", circle(absSpec("z", "1"), row(Frac(row(Sym("e"), Pow(r("z"))), r("z³")))))
    @Test fun contourPoleOnTheCircle() = assertEquals("Error: f has a pole on the circle", circle(absSpec("z", "1"), row(Frac(r("1"), r("z−1")))))
    @Test fun residueSimplePole() = assertEquals("-i/2", cas(f("residue", row(Frac(r("1"), r("z²+1"))), r("z=i"))))
    @Test fun residueDoublePole() = assertEquals("1", cas(f("residue", row(Frac(row(Sym("e"), Pow(r("z"))), r("z²"))), r("z=0"))))
    @Test fun residueOfSinOverZSquared() = assertEquals("1", cas(f("residue", row(Frac(row(Func("sin", listOf(r("z")))), r("z²"))), r("z=0"))))
    @Test fun residueAtRegularPoint() = assertEquals("0", cas(f("residue", r("z²"), r("z=1"))))

    @Test fun total() = assertEquals("40", cas(f("total", data)))
    @Test fun poisson() = assertEquals("9/(2e^3)", cas(f("poissonpdf", r("3"), r("2"))))
    @Test fun binomialCdf() = assertEquals("11/16", cas(f("binomcdf", r("4"), row(Frac(r("1"), r("2"))), r("2"))))
}
