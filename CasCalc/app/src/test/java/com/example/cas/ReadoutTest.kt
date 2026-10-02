package com.example.cas

import com.example.cas.engine.LatexParser
import com.example.cas.engine.Readout
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadoutTest {
    @Test fun plainNumber() = assertEquals("\$-3.25\$", Readout.markdown("−3.25"))
    @Test fun exponent() = assertEquals("\$2.500 \\times 10^{6}\$", Readout.markdown("2.500e+06"))
    @Test fun negativeExponent() = assertEquals("\$-1.234 \\times 10^{-5}\$", Readout.markdown("−1.234e−05"))
    @Test fun complex() = assertEquals("\$2 - 3i\$", Readout.markdown("2 − 3i"))
    @Test fun complexPlus() = assertEquals("\$2 + 3i\$", Readout.markdown("2 + 3i"))
    @Test fun polar() = assertEquals("\$1.5\\angle -\\frac{\\pi}{3}\$", Readout.markdown("1.5∠−π/3"))
    @Test fun approx() = assertEquals("\$\\approx 3.1416\$", Readout.markdown("≈ 3.1416"))
    @Test fun degrees() = assertEquals("\$30^{\\circ}\$", Readout.markdown("30°"))
    @Test fun words() = assertEquals("\$1\$ to \$3\$", Readout.markdown("1 to 3"))
    @Test fun onlyWords() = assertEquals("undefined", Readout.markdown("undefined"))
    @Test fun dash() = assertEquals("—", Readout.markdown("—"))
    @Test fun inTheView() = assertEquals("in the view", Readout.markdown("in the view"))

    /** Everything it writes is LaTeX the renderer knows. */
    @Test fun parses() {
        for (s in listOf("−1.234e−05", "1.5∠−π/3", "≈ 3.1416", "30°", "±10", "2 × 3", "x ∈ [−5, 5]", "short → long"))
            for ((isMath, p) in LatexParser.inline(Readout.markdown(s))) if (isMath) assertEquals(s, emptySet<String>(), LatexParser.unknownCommands(p))
    }
}
