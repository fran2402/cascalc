package com.example.cas

import com.example.cas.engine.LatexParser
import com.example.cas.engine.Units
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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

    @Test fun unknown() { assertTrue(runCatching { Units.parse("furlong") }.exceptionOrNull() is Units.UnitError) }

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
