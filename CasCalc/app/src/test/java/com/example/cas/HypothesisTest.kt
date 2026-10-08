package com.example.cas

import com.example.cas.graph.Hypothesis
import com.example.cas.graph.Hypothesis.Tail
import org.junit.Assert.assertEquals
import org.junit.Test

/** The data table's hypothesis tests, against SciPy's values. */
class HypothesisTest {
    private fun d(vararg v: Number) = v.map { it.toDouble() }

    @Test fun paired() {
        val r = Hypothesis.pairedT(d(72, 80, 65, 90, 77, 84), d(68, 74, 66, 81, 70, 79), Tail.Both)
        assertEquals(3.595974761140381, r.statistic, 1e-9)
        assertEquals(0.015609356456466137, r.p, 1e-7)
        assertEquals(5.0, r.df!!, 0.0)
        assertEquals(true, r.reject)
        // 5 ± t(0.975, 5)·s/√n
        assertEquals(1.4258, r.ci!!.first, 1e-3); assertEquals(8.5742, r.ci!!.second, 1e-3)
    }

    @Test fun oneSample() {
        val r = Hypothesis.oneSampleT(d(5.1, 4.9, 5.6, 5.8, 6.0, 5.2), 5.0, Tail.Both)
        assertEquals(2.456769074559979, r.statistic, 1e-9); assertEquals(0.057456265269132285, r.p, 1e-7)
        assertEquals(false, r.reject)
    }

    @Test fun welch() {
        val x = d(20, 22, 19, 24, 25); val y = d(28, 27, 30, 26, 29, 31)
        val r = Hypothesis.twoSampleT(x, y, Tail.Both)
        assertEquals(-4.736415133683288, r.statistic, 1e-9); assertEquals(0.0019384443391590003, r.p, 1e-7); assertEquals(7.230464326160814, r.df!!, 1e-9)
        assertEquals(0.0009692221695795001, Hypothesis.twoSampleT(x, y, Tail.Less).p, 1e-7)
    }

    @Test fun chiSquare() {
        val r = Hypothesis.chiSquareIndependence(listOf(d(10, 20, 30), d(20, 25, 15)))
        assertEquals(8.88888888888889, r.statistic, 1e-9); assertEquals(0.011743628457021359, r.p, 1e-7); assertEquals(2.0, r.df!!, 0.0)
        val f = Hypothesis.chiSquareFit(d(18, 22, 20, 40))
        assertEquals(12.32, f.statistic, 1e-9); assertEquals(0.006363629995195269, f.p, 1e-7)
    }

    @Test fun proportion() {
        val r = Hypothesis.oneProportionZ(58.0, 100.0, 0.5, Tail.Both)
        assertEquals(1.6, r.statistic, 1e-9); assertEquals(0.10959858339911599, r.p, 1e-7)
    }

    @Test fun criticalValues() {
        val r = Hypothesis.pairedT(d(72, 80, 65, 90, 77, 84), d(68, 74, 66, 81, 70, 79), Tail.Both)
        assertEquals(2.570581835636314, r.critical[1], 1e-6); assertEquals(-2.570581835636314, r.critical[0], 1e-6)
    }
}
