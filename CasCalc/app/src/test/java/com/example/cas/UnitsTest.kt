package com.example.cas

import com.example.cas.engine.LatexParser
import com.example.cas.engine.Units
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.cas.cas.subst

class UnitsTest {
    private fun conv(v: Double, from: String, to: String) = Units.convert(v, Units.parse(from), Units.target(to, Units.parse(from).dims)).value
    private fun close(expected: Double, got: Double, rel: Double = 1e-9) = assertEquals(expected, got, kotlin.math.abs(expected) * rel)

    @Test fun simple() = close(1000.0, conv(1.0, "km", "m"))
    @Test fun hubble() = close(70 * 1000 / 3.0856775814913673e22, conv(70.0, "km/s/Mpc", "1/s"))
    @Test fun hubbleSlash() = close(conv(70.0, "km/s/Mpc", "s^-1"), conv(70.0, "km s^-1 Mpc^-1", "Hz"))
    @Test fun ergToSI() = close(1e-7, conv(1.0, "erg", "SI"))
    @Test fun ergNamed() = assertEquals("J", Units.inSystem(Units.parse("erg").dims, "SI").parts.single().unit.symbol)
    @Test fun ergBase() = assertEquals(listOf("m", "g", "s"), Units.inSystem(Units.parse("erg").dims, "base").parts.map { it.unit.symbol })
    @Test fun jouleToCgs() = close(1e7, conv(1.0, "J", "cgs"))
    @Test fun celsius() = close(212.0, conv(100.0, "°C", "°F"))
    @Test fun kelvin() = close(273.15, conv(0.0, "degC", "K"))
    @Test fun temperatureDifference() = close(1.0, conv(1.0, "°C/s", "K/s"))
    @Test fun superscripts() = close(1e-6, conv(1.0, "cm³", "m^3"))
    @Test fun attachedPower() = close(1e6, conv(1.0, "cm-3", "m-3"))
    @Test fun brackets() = close(1.0, conv(1.0, "W/(m^2 Hz)", "kg s^-2"))
    @Test fun spectralFlux() = close(1e-26, conv(1.0, "Jy", "W m^-2 Hz^-1"))
    @Test fun gauss() = close(1e-4, conv(1.0, "G", "T"))
    @Test fun mpcPrefix() = close(3.0856775814913673e22, conv(1.0, "Mpc", "m"))
    @Test fun solarMass() = close(1.98841e30, conv(1.0, "Msun", "kg"))
    @Test fun lightYear() = close(0.30660139, conv(1.0, "ly", "pc"), 1e-7)
    @Test fun bytes() = close(8192.0, conv(1.0, "KiB", "bit"))
    @Test fun numbersInside() = close(1e6 * 365.25 * 86400, conv(1.0, "10^6 yr", "s"))
    @Test fun megayear() = close(1e6 * 365.25 * 86400, conv(1.0, "Myr", "s"))
    @Test fun ambiguity() { assertEquals("min", Units.lookup("min")!!.second.symbol); assertEquals("Pa", Units.lookup("Pa")!!.second.symbol); assertEquals("cd", Units.lookup("cd")!!.second.symbol) }
    @Test fun mu() = close(1e-6, conv(1.0, "µm", "m"))

    // Physical equivalences.
    @Test fun eVToKelvin() = close(11604.51812, conv(1.0, "eV", "K"), 1e-8)
    @Test fun wavelengthToEnergy() = close(1239.84198, conv(1.0, "nm", "eV"), 1e-8)
    @Test fun massToEnergy() = close(938.272089, conv(1.0, "m_p", "MeV"), 1e-8)
    @Test fun frequencyToWavelength() = close(0.211061140, conv(1420.405751768, "MHz", "m"), 1e-8)
    @Test fun noBridge() { assertNull(Units.bridge(Units.parse("kg").dims, Units.parse("A").dims, Units.Bridges())) }
    @Test fun bridgeOff() { assertNull(Units.bridge(Units.parse("eV").dims, Units.parse("K").dims, Units.Bridges(kB = false))) }
    @Test fun describe() = assertTrue(Units.bridge(Units.parse("kg").dims, Units.parse("J").dims, Units.Bridges())!!.describe().contains("mc^2"))

    @Test fun compatibleLength() {
        val c = Units.compatible(Units.parse("km").dims, Units.Bridges())
        assertTrue(c.containsAll(listOf("m", "pc", "Mpc", "ly", "au")))
        assertTrue("light travel time", c.contains("s"))
    }
    @Test fun compatibleEnergy() = assertTrue(Units.compatible(Units.parse("eV").dims, Units.Bridges()).containsAll(listOf("J", "erg", "K", "Hz", "nm")))
    @Test fun compatibleAllParse() {
        for (sym in Units.compatible(Units.parse("J").dims, Units.Bridges(), 200) + Units.compatible(Units.parse("m").dims, Units.Bridges(), 200)) Units.parse(sym)
    }

    @Test fun unknown() { assertTrue(runCatching { Units.parse("smoot") }.exceptionOrNull() is Units.UnitError) }

    @Test fun numberFormat() {
        assertEquals("2.26854 \\times 10^{-18}", Units.number(2.268544e-18))
        assertEquals("1000", Units.number(1000.0))
        assertEquals("0.5", Units.number(0.5))
        assertEquals("1 \\times 10^{7}", Units.number(9.9999999e6))
    }

    @Test fun alternatives() {
        val alt = Units.alternatives(3.0856775814913673e22, Units.parse("m").dims)
        assertTrue(alt.any { it.text == "Mpc" })
    }

    /** Every unit's LaTeX, with each prefix, is LaTeX the renderer knows. */
    @Test fun latexParses() {
        for (u in Units.ALL) for (t in listOf(u.symbol) + if (u.prefixes) listOf("k" + u.symbol, "µ" + u.symbol) else emptyList()) {
            val q = Units.parse(if (u.symbol == "fl oz") "floz" else t)
            for (latex in listOf(q.latex(), q.dims.latex())) assertEquals("$t: $latex", emptySet<String>(), LatexParser.unknownCommands(latex))
        }
        for (u in Units.ALL) assertNotNull(u.symbol, Units.quantityName(u.dims) ?: "")
    }
}

class AbsoluteLogIntegralTest {
    private val x = com.example.cas.cas.Sym("x")
    private fun lnAbs(u: com.example.cas.cas.Expr) = com.example.cas.cas.fn("ln", com.example.cas.cas.fn("abs", u))
    /** F′ = f at a few points, negative ones included. */
    private fun check(f: com.example.cas.cas.Expr) {
        val big = com.example.cas.cas.Calculus.integrate(f, x)
        org.junit.Assert.assertNotNull(f.toString(), big)
        val d = com.example.cas.cas.Calculus.diff(big!!, x)
        for (v in listOf(-2.5, -0.7, 0.4, 3.1)) {
            val a = com.example.cas.cas.Numeric.eval(d.subst(x, com.example.cas.cas.Flt(v))).re
            val b = com.example.cas.cas.Numeric.eval(f.subst(x, com.example.cas.cas.Flt(v))).re
            org.junit.Assert.assertEquals("$f at $v", b, a, 1e-9 * (1 + kotlin.math.abs(b)))
        }
    }
    @Test fun lnAbsX() = check(lnAbs(x))
    @Test fun xLnAbsX() = check(com.example.cas.cas.mul(x, lnAbs(x)))
    @Test fun lnAbsLinear() = check(lnAbs(com.example.cas.cas.add(com.example.cas.cas.mul(com.example.cas.cas.TWO, x), com.example.cas.cas.ONE)))
    @Test fun lnAbsSquared() = check(com.example.cas.cas.pow(lnAbs(x), com.example.cas.cas.TWO))
    @Test fun log10Abs() = check(com.example.cas.cas.fn("log", com.example.cas.cas.Num(10), com.example.cas.cas.fn("abs", x)))
    @Test fun lnAbsOverX() = check(com.example.cas.cas.div(lnAbs(x), x))
}

class MoreUnitsTest {
    @Test fun explicitTimesEndsADenominator() {
        // The picker's × writes ·: (lea/Å)·(in/Å), with "in" back in the numerator.
        val q = Units.parse("lea/Å·in/Å")
        assertEquals(listOf(1.0, -1.0, 1.0, -1.0), q.parts.map { it.power })
        // A space still stays in the denominator (J/kg K is J/(kg K)), and a/b/c is a/(b c).
        assertEquals(listOf(1.0, -1.0, -1.0), Units.parse("J/kg K").parts.map { it.power })
        assertEquals(listOf(1.0, -1.0, -1.0), Units.parse("km/s/Mpc").parts.map { it.power })
        assertEquals(listOf(1.0, -1.0, 1.0), Units.parse("m/s * kg").parts.map { it.power })
        assertEquals(listOf(1.0, 1.0, -2.0), Units.parse("kg·m/s^2").parts.map { it.power })
    }

    @Test fun plainNumbers() {
        assertEquals("70", Units.plain(70.0))
        assertEquals("0.0025", Units.plain(0.0025))
        assertEquals("1500", Units.plain(1500.0))
        assertEquals("3e-7", Units.plain(3e-7))
        assertEquals("2.99792e14", Units.plain(2.99792458e14))
        assertEquals("0", Units.plain(0.0))
    }

    private fun conv(v: Double, from: String, to: String) = Units.convert(v, Units.parse(from), Units.target(to, Units.parse(from).dims)).value
    private fun close(expected: Double, got: Double, rel: Double = 1e-9) = assertEquals(expected, got, kotlin.math.abs(expected) * rel)
    @Test fun furlong() = close(201.168, conv(1.0, "furlong", "m"))
    @Test fun troyOunce() = close(31.1034768, conv(1.0, "ozt", "g"))
    @Test fun imperialGallon() = close(1.2009499, conv(1.0, "galUK", "gal"), 1e-7)
    @Test fun barrel() = close(158.987294928, conv(1.0, "bbl", "L"))
    @Test fun tnt() = close(4.184e15, conv(1.0, "MtTNT", "J"))
    @Test fun metricHp() = close(735.49875, conv(1.0, "PS", "W"))
    @Test fun milliAmpHour() = close(3.6, conv(1.0, "mAh", "C"))
    @Test fun molar() = close(1.0, conv(1.0, "mM", "mol/m^3"))
    @Test fun reaumur() = close(100.0, conv(80.0, "°Ré", "°C"))
    @Test fun romer() = close(100.0, conv(60.0, "°Rø", "°C"))
    @Test fun tsubo() = close(3.305785, conv(1.0, "tsubo", "m^2"), 1e-6)
    @Test fun milliTorr() = close(101325.0 / 760 / 1000, conv(1.0, "mTorr", "Pa"))
    /** \\mathrm{…} is read as plain text, so no command may sit inside it (it would show as written). */
    @Test fun noCommandsInsideText() {
        for (u in Units.ALL) org.junit.Assert.assertFalse(u.latex, Regex("""\\mathrm\{[^}]*\\""").containsMatchIn(u.latex))
    }
    @Test fun allPrefixes() {
        for (p in listOf("Q", "R", "Y", "Z", "E", "P", "T", "G", "M", "k", "h", "da", "d", "c", "m", "µ", "n", "p", "f", "a", "z", "y", "r", "q"))
            org.junit.Assert.assertNotNull(p, Units.lookup(p + "m"))
        close(1e30, conv(1.0, "Qm", "m")); close(1e-30, conv(1.0, "qm", "m")); close(10.0, conv(1.0, "dam", "m"))
    }
}
