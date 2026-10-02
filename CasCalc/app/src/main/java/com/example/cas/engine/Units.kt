package com.example.cas.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * The unit converter's engine. A unit expression ("km/s/Mpc", "erg", "kg m^2 s^-2",
 * "W/(m² sr Hz)") is read into a factor times SI base units; two of them convert when their
 * dimensions match, or, with physical equivalences allowed, when the constants c, h (or ħ)
 * and k_B bridge them (eV ↔ K, nm ↔ eV, kg ↔ J). Results come as LaTeX.
 */
object Units {
    /** Exponents of m, kg, s, A, K, mol, cd and bit. */
    class Dims(val e: DoubleArray) {
        operator fun plus(o: Dims) = Dims(DoubleArray(N) { e[it] + o.e[it] })
        operator fun times(k: Double) = Dims(DoubleArray(N) { e[it] * k })
        operator fun unaryMinus() = times(-1.0)
        fun same(o: Dims) = (0 until N).all { abs(e[it] - o.e[it]) < 1e-9 }
        val isNone get() = e.all { abs(it) < 1e-9 }
        override fun equals(other: Any?) = other is Dims && same(other)
        override fun hashCode() = e.map { (it * 1000).roundToInt() }.hashCode()

        /** As dimension symbols: L T⁻¹ for a speed. */
        fun latex(): String {
            if (isNone) return "1"
            return (0 until N).filter { abs(e[it]) > 1e-9 }.joinToString("\\,") { k -> "\\mathrm{${DIM_SYMBOLS[k]}}" + power(e[k]) }
        }
    }

    private const val N = 8
    private val DIM_SYMBOLS = listOf("L", "M", "T", "I", "Θ", "N", "J", "B")
    private fun dims(m: Double = 0.0, kg: Double = 0.0, s: Double = 0.0, a: Double = 0.0, k: Double = 0.0, mol: Double = 0.0, cd: Double = 0.0, bit: Double = 0.0) =
        Dims(doubleArrayOf(m, kg, s, a, k, mol, cd, bit))
    val NONE = dims()

    /** A unit: its symbol, how it's written in LaTeX, what it is, its size in SI base units, and whether it takes prefixes. */
    class Unit(
        val symbol: String,
        val name: String,
        val factor: Double,
        val dims: Dims,
        val category: String,
        val prefixes: Boolean = false,
        val latex: String = "\\mathrm{$symbol}",
        val aliases: List<String> = emptyList(),
        /** For °C and °F: the SI value is (x + offset)·factor. */
        val offset: Double = 0.0,
        /** Shown in "also equals" lists for its dimension. */
        val common: Boolean = true,
    )

    class Prefix(val symbol: String, val factor: Double, val latex: String = symbol)

    private val SI_PREFIXES = listOf(
        Prefix("Q", 1e30), Prefix("R", 1e27), Prefix("Y", 1e24), Prefix("Z", 1e21), Prefix("E", 1e18), Prefix("P", 1e15),
        Prefix("T", 1e12), Prefix("G", 1e9), Prefix("M", 1e6), Prefix("k", 1e3), Prefix("h", 1e2), Prefix("da", 1e1),
        Prefix("d", 1e-1), Prefix("c", 1e-2), Prefix("m", 1e-3), Prefix("µ", 1e-6, "\\mu "), Prefix("μ", 1e-6, "\\mu "), Prefix("u", 1e-6, "\\mu "),
        Prefix("n", 1e-9), Prefix("p", 1e-12), Prefix("f", 1e-15), Prefix("a", 1e-18), Prefix("z", 1e-21), Prefix("y", 1e-24),
        Prefix("r", 1e-27), Prefix("q", 1e-30),
    )
    private val BINARY_PREFIXES = listOf(
        Prefix("Ki", 1024.0), Prefix("Mi", 1024.0.pow(2)), Prefix("Gi", 1024.0.pow(3)), Prefix("Ti", 1024.0.pow(4)), Prefix("Pi", 1024.0.pow(5)), Prefix("Ei", 1024.0.pow(6)),
    )
    /** The prefixes tried when writing a value in its best-sized unit: the everyday ones. */
    private val NICE_PREFIXES = listOf("T", "G", "M", "k", "", "m", "µ", "n", "p")
    /** Units that only take a few prefixes in practice. */
    private val FEW_PREFIXES = mapOf(
        "erg" to listOf(""), "cal" to listOf("", "k"), "Wh" to listOf("", "k", "M", "G", "T"), "t" to listOf("", "k", "M", "G"),
        "bar" to listOf("", "m"), "pc" to listOf("", "k", "M", "G"), "ly" to listOf("", "k", "M", "G"), "yr" to listOf("", "k", "M", "G"),
        "L" to listOf("", "m", "µ"), "b" to listOf("", "m", "µ", "n", "p", "f"), "Jy" to listOf("", "m", "µ", "k", "M"), "dyn" to listOf(""),
        "P" to listOf("", "c"), "Da" to listOf("", "k", "M"), "eV" to listOf("", "m", "k", "M", "G", "T"),
    )

    // Constants (CODATA 2022), in SI.
    const val C = 299792458.0
    const val H = 6.62607015e-34
    const val HBAR = H / (2 * PI)
    const val KB = 1.380649e-23
    const val E_CHARGE = 1.602176634e-19
    private const val G_N = 6.67430e-11
    private const val AU = 149597870700.0
    private const val PC = AU * 648000 / PI
    private const val LY = C * 365.25 * 86400
    private const val YEAR = 365.25 * 86400
    private const val ME = 9.1093837139e-31
    private const val MP = 1.67262192595e-27
    private const val DA = 1.66053906892e-27
    private const val HARTREE = 4.3597447222060e-18
    private const val BOHR = 5.29177210544e-11
    private const val INCH = 0.0254
    private const val FOOT = 0.3048
    private const val POUND = 0.45359237
    private const val GALLON = 3.785411784e-3
    private const val G0 = 9.80665
    private const val CAL = 4.184

    val L = dims(m = 1.0); val M = dims(kg = 1.0); val T = dims(s = 1.0); val I = dims(a = 1.0)
    val TEMP = dims(k = 1.0); val MOL = dims(mol = 1.0); val CD = dims(cd = 1.0); val BIT = dims(bit = 1.0)
    private val AREA = L * 2.0; private val VOLUME = L * 3.0
    private val SPEED = L + -T; private val ACCEL = L + T * -2.0
    private val FORCE = M + ACCEL; private val ENERGY = FORCE + L; private val POWER = ENERGY + -T
    private val PRESSURE = FORCE + AREA * -1.0; private val FREQ = -T
    private val CHARGE = I + T; private val VOLT = POWER + -I; private val OHM = VOLT + -I
    private val FARAD = CHARGE + -VOLT; private val WEBER = VOLT + T; private val TESLA = WEBER + AREA * -1.0
    private val HENRY = WEBER + -I; private val SIEMENS = -OHM; private val VISCOSITY = PRESSURE + T
    private val DIPOLE = CHARGE + L; private val SPECTRAL_FLUX = POWER + AREA * -1.0 + -FREQ

    private val PLANCK_LENGTH = kotlin.math.sqrt(HBAR * G_N / C.pow(3))
    private val PLANCK_MASS = kotlin.math.sqrt(HBAR * C / G_N)
    private val PLANCK_TIME = PLANCK_LENGTH / C

    /** Every unit, by category. */
    val ALL: List<Unit> = listOf(
        // SI base and derived
        Unit("m", "meter", 1.0, L, "Length", prefixes = true, aliases = listOf("meter", "metre", "meters", "metres")),
        Unit("g", "gram", 1e-3, M, "Mass", prefixes = true, aliases = listOf("gram", "grams")),
        Unit("s", "second", 1.0, T, "Time", prefixes = true, aliases = listOf("sec", "second", "seconds")),
        Unit("A", "ampere", 1.0, I, "Electromagnetism", prefixes = true, aliases = listOf("amp", "ampere")),
        Unit("K", "kelvin", 1.0, TEMP, "Temperature", prefixes = true, aliases = listOf("kelvin")),
        Unit("mol", "mole", 1.0, MOL, "Amount", prefixes = true, aliases = listOf("mole")),
        Unit("cd", "candela", 1.0, CD, "Light", prefixes = true, aliases = listOf("candela")),
        Unit("Hz", "hertz", 1.0, FREQ, "Frequency", prefixes = true, aliases = listOf("hertz")),
        Unit("N", "newton", 1.0, FORCE, "Force", prefixes = true, aliases = listOf("newton")),
        Unit("Pa", "pascal", 1.0, PRESSURE, "Pressure", prefixes = true, aliases = listOf("pascal")),
        Unit("J", "joule", 1.0, ENERGY, "Energy", prefixes = true, aliases = listOf("joule", "joules")),
        Unit("W", "watt", 1.0, POWER, "Power", prefixes = true, aliases = listOf("watt", "watts")),
        Unit("C", "coulomb", 1.0, CHARGE, "Electromagnetism", prefixes = true, aliases = listOf("coulomb")),
        Unit("V", "volt", 1.0, VOLT, "Electromagnetism", prefixes = true, aliases = listOf("volt", "volts")),
        Unit("Ω", "ohm", 1.0, OHM, "Electromagnetism", prefixes = true, latex = "\\Omega", aliases = listOf("ohm", "Ohm")),
        Unit("F", "farad", 1.0, FARAD, "Electromagnetism", prefixes = true, aliases = listOf("farad")),
        Unit("S", "siemens", 1.0, SIEMENS, "Electromagnetism", prefixes = true, aliases = listOf("siemens")),
        Unit("Wb", "weber", 1.0, WEBER, "Electromagnetism", prefixes = true, aliases = listOf("weber")),
        Unit("T", "tesla", 1.0, TESLA, "Electromagnetism", prefixes = true, aliases = listOf("tesla")),
        Unit("H", "henry", 1.0, HENRY, "Electromagnetism", prefixes = true, aliases = listOf("henry")),
        Unit("lm", "lumen", 1.0, CD, "Light", prefixes = true, aliases = listOf("lumen")),
        Unit("lx", "lux", 1.0, CD + AREA * -1.0, "Light", prefixes = true, aliases = listOf("lux")),
        Unit("Bq", "becquerel", 1.0, FREQ, "Radiation", prefixes = true, aliases = listOf("becquerel"), common = false),
        Unit("Gy", "gray", 1.0, ENERGY + -M, "Radiation", prefixes = true, aliases = listOf("gray")),
        Unit("Sv", "sievert", 1.0, ENERGY + -M, "Radiation", prefixes = true, aliases = listOf("sievert"), common = false),
        Unit("kat", "katal", 1.0, MOL + -T, "Amount", prefixes = true, aliases = listOf("katal")),
        Unit("rad", "radian", 1.0, NONE, "Angle", prefixes = true, aliases = listOf("radian", "radians"), common = false),
        Unit("sr", "steradian", 1.0, NONE, "Angle", prefixes = true, aliases = listOf("steradian"), common = false),
        // Length
        Unit("Å", "ångström", 1e-10, L, "Length", latex = "\\mathrm{Å}", aliases = listOf("AA", "angstrom", "Angstrom")),
        Unit("in", "inch", INCH, L, "Length", aliases = listOf("inch", "inches", "\"")),
        Unit("ft", "foot", FOOT, L, "Length", aliases = listOf("foot", "feet", "'")),
        Unit("yd", "yard", 0.9144, L, "Length", aliases = listOf("yard", "yards")),
        Unit("mi", "mile", 1609.344, L, "Length", aliases = listOf("mile", "miles")),
        Unit("nmi", "nautical mile", 1852.0, L, "Length", aliases = listOf("NM")),
        Unit("au", "astronomical unit", AU, L, "Astronomy", aliases = listOf("AU")),
        Unit("ly", "light-year", LY, L, "Astronomy", prefixes = true, aliases = listOf("lyr")),
        Unit("pc", "parsec", PC, L, "Astronomy", prefixes = true, aliases = listOf("parsec", "parsecs")),
        Unit("R_⊙", "solar radius", 6.957e8, L, "Astronomy", latex = "R_{\\odot}", aliases = listOf("Rsun", "R_sun", "R☉", "R_☉")),
        Unit("R_⊕", "Earth radius", 6.3781e6, L, "Astronomy", latex = "R_{\\oplus}", aliases = listOf("Rearth", "R_earth", "R⊕")),
        Unit("R_J", "Jupiter radius", 7.1492e7, L, "Astronomy", latex = "R_{\\mathrm{J}}", aliases = listOf("Rjup", "R_jup")),
        Unit("a_0", "Bohr radius", BOHR, L, "Atomic & particle", latex = "a_{0}", aliases = listOf("a0", "bohr")),
        Unit("l_P", "Planck length", PLANCK_LENGTH, L, "Atomic & particle", latex = "\\ell_{\\mathrm{P}}", aliases = listOf("lP", "l_Planck")),
        // Mass
        Unit("t", "tonne", 1000.0, M, "Mass", prefixes = true, aliases = listOf("tonne", "tonnes")),
        Unit("lb", "pound", POUND, M, "Mass", aliases = listOf("lbs", "pound", "pounds")),
        Unit("oz", "ounce", POUND / 16, M, "Mass", aliases = listOf("ounce", "ounces")),
        Unit("st", "stone", POUND * 14, M, "Mass", aliases = listOf("stone"), common = false),
        Unit("ton", "short ton", POUND * 2000, M, "Mass", aliases = listOf("tons"), common = false),
        Unit("u", "atomic mass unit", DA, M, "Atomic & particle", aliases = listOf("amu")),
        Unit("Da", "dalton", DA, M, "Atomic & particle", prefixes = true, aliases = listOf("dalton"), common = false),
        Unit("m_e", "electron mass", ME, M, "Atomic & particle", latex = "m_{e}", aliases = listOf("me")),
        Unit("m_p", "proton mass", MP, M, "Atomic & particle", latex = "m_{p}", aliases = listOf("mp")),
        Unit("m_P", "Planck mass", PLANCK_MASS, M, "Atomic & particle", latex = "m_{\\mathrm{P}}", aliases = listOf("mP", "m_Planck"), common = false),
        Unit("M_⊙", "solar mass", 1.98841e30, M, "Astronomy", latex = "M_{\\odot}", aliases = listOf("Msun", "M_sun", "M☉", "M_☉")),
        Unit("M_⊕", "Earth mass", 5.97217e24, M, "Astronomy", latex = "M_{\\oplus}", aliases = listOf("Mearth", "M_earth", "M⊕")),
        Unit("M_J", "Jupiter mass", 1.89813e27, M, "Astronomy", latex = "M_{\\mathrm{J}}", aliases = listOf("Mjup", "M_jup")),
        // Time
        Unit("min", "minute", 60.0, T, "Time", aliases = listOf("minute", "minutes")),
        Unit("h", "hour", 3600.0, T, "Time", aliases = listOf("hr", "hour", "hours")),
        Unit("d", "day", 86400.0, T, "Time", aliases = listOf("day", "days")),
        Unit("wk", "week", 604800.0, T, "Time", aliases = listOf("week", "weeks"), common = false),
        Unit("yr", "year (Julian)", YEAR, T, "Time", prefixes = true, aliases = listOf("a", "year", "years")),
        Unit("t_P", "Planck time", PLANCK_TIME, T, "Atomic & particle", latex = "t_{\\mathrm{P}}", aliases = listOf("tP"), common = false),
        // Speed and acceleration
        Unit("c", "speed of light", C, SPEED, "Speed", latex = "c"),
        Unit("mph", "mile per hour", 1609.344 / 3600, SPEED, "Speed"),
        Unit("kn", "knot", 1852.0 / 3600, SPEED, "Speed", aliases = listOf("knot", "knots", "kt")),
        Unit("Gal", "gal", 0.01, ACCEL, "CGS", prefixes = true, common = false),
        Unit("g_0", "standard gravity", G0, ACCEL, "Speed", latex = "g_{0}", aliases = listOf("g0", "gn")),
        // Area and volume
        Unit("ha", "hectare", 1e4, AREA, "Area", aliases = listOf("hectare")),
        Unit("acre", "acre", 4046.8564224, AREA, "Area", aliases = listOf("acres")),
        Unit("b", "barn", 1e-28, AREA, "Atomic & particle", prefixes = true, aliases = listOf("barn")),
        Unit("L", "liter", 1e-3, VOLUME, "Volume", prefixes = true, aliases = listOf("l", "liter", "litre", "liters", "litres")),
        Unit("gal", "US gallon", GALLON, VOLUME, "Volume", aliases = listOf("gallon", "gallons")),
        Unit("qt", "US quart", GALLON / 4, VOLUME, "Volume", aliases = listOf("quart"), common = false),
        Unit("pt", "US pint", GALLON / 8, VOLUME, "Volume", aliases = listOf("pint"), common = false),
        Unit("cup", "US cup", GALLON / 16, VOLUME, "Volume", aliases = listOf("cups")),
        Unit("fl oz", "US fluid ounce", GALLON / 128, VOLUME, "Volume", latex = "\\mathrm{fl\\,oz}", aliases = listOf("floz", "fl_oz")),
        Unit("tbsp", "tablespoon", GALLON / 256, VOLUME, "Volume", common = false),
        Unit("tsp", "teaspoon", GALLON / 768, VOLUME, "Volume", common = false),
        // Energy
        Unit("eV", "electronvolt", E_CHARGE, ENERGY, "Energy", prefixes = true, aliases = listOf("electronvolt")),
        Unit("erg", "erg", 1e-7, ENERGY, "CGS", prefixes = true, aliases = listOf("ergs")),
        Unit("cal", "calorie", CAL, ENERGY, "Energy", prefixes = true, aliases = listOf("calorie", "calories")),
        Unit("Wh", "watt-hour", 3600.0, ENERGY, "Energy", prefixes = true),
        Unit("Btu", "British thermal unit", 1055.05585262, ENERGY, "Energy", aliases = listOf("BTU")),
        Unit("E_h", "hartree", HARTREE, ENERGY, "Atomic & particle", latex = "E_{\\mathrm{h}}", aliases = listOf("Eh", "hartree")),
        Unit("Ry", "rydberg", HARTREE / 2, ENERGY, "Atomic & particle", aliases = listOf("rydberg")),
        Unit("foe", "foe (10⁵¹ erg)", 1e44, ENERGY, "Astronomy", aliases = listOf("bethe"), common = false),
        // Power
        Unit("hp", "horsepower", 745.69987158227022, POWER, "Power", aliases = listOf("horsepower")),
        Unit("L_⊙", "solar luminosity", 3.828e26, POWER, "Astronomy", latex = "L_{\\odot}", aliases = listOf("Lsun", "L_sun", "L☉", "L_☉")),
        // Pressure
        Unit("bar", "bar", 1e5, PRESSURE, "Pressure", prefixes = true),
        Unit("atm", "atmosphere", 101325.0, PRESSURE, "Pressure", aliases = listOf("atmosphere")),
        Unit("Torr", "torr", 101325.0 / 760, PRESSURE, "Pressure", aliases = listOf("torr")),
        Unit("mmHg", "millimeter of mercury", 133.322387415, PRESSURE, "Pressure", common = false),
        Unit("psi", "pound per square inch", POUND * G0 / (INCH * INCH), PRESSURE, "Pressure"),
        Unit("Ba", "barye", 0.1, PRESSURE, "CGS", common = false),
        // Force
        Unit("dyn", "dyne", 1e-5, FORCE, "CGS", prefixes = true, aliases = listOf("dyne")),
        Unit("kgf", "kilogram-force", G0, FORCE, "Force"),
        Unit("lbf", "pound-force", POUND * G0, FORCE, "Force"),
        // Temperature
        Unit("°C", "degree Celsius", 1.0, TEMP, "Temperature", latex = "{}^{\\circ}\\mathrm{C}", aliases = listOf("degC", "℃", "celsius", "Celsius"), offset = 273.15),
        Unit("°F", "degree Fahrenheit", 5.0 / 9, TEMP, "Temperature", latex = "{}^{\\circ}\\mathrm{F}", aliases = listOf("degF", "℉", "fahrenheit", "Fahrenheit"), offset = 459.67),
        Unit("°R", "degree Rankine", 5.0 / 9, TEMP, "Temperature", latex = "{}^{\\circ}\\mathrm{R}", aliases = listOf("degR", "rankine"), common = false),
        // Angle
        Unit("°", "degree", PI / 180, NONE, "Angle", latex = "{}^{\\circ}", aliases = listOf("deg", "degree", "degrees")),
        Unit("′", "arcminute", PI / 10800, NONE, "Angle", latex = "{}'", aliases = listOf("arcmin")),
        Unit("″", "arcsecond", PI / 648000, NONE, "Angle", latex = "{}''", aliases = listOf("arcsec")),
        Unit("mas", "milliarcsecond", PI / 648000e3, NONE, "Angle", common = false),
        Unit("µas", "microarcsecond", PI / 648000e6, NONE, "Angle", latex = "\\mu\\mathrm{as}", aliases = listOf("uas", "μas"), common = false),
        Unit("rev", "revolution", 2 * PI, NONE, "Angle", aliases = listOf("turn", "turns")),
        Unit("rpm", "revolutions per minute", 2 * PI / 60, FREQ, "Frequency", common = false),
        Unit("%", "percent", 0.01, NONE, "Angle", latex = "\\%", aliases = listOf("percent"), common = false),
        Unit("ppm", "part per million", 1e-6, NONE, "Angle", common = false),
        // Electromagnetism, CGS (Gaussian, matched to SI by the usual convention)
        Unit("statC", "statcoulomb", 1 / (10 * C), CHARGE, "CGS", latex = "\\mathrm{statC}", aliases = listOf("esu", "Fr")),
        Unit("statA", "statampere", 1 / (10 * C), I, "CGS", common = false),
        Unit("statV", "statvolt", C / 1e6, VOLT, "CGS"),
        Unit("abA", "abampere", 10.0, I, "CGS", aliases = listOf("Bi"), common = false),
        Unit("G", "gauss", 1e-4, TESLA, "CGS", aliases = listOf("Gs", "gauss")),
        Unit("Mx", "maxwell", 1e-8, WEBER, "CGS", aliases = listOf("maxwell")),
        Unit("Oe", "oersted", 1000 / (4 * PI), I + -L, "CGS", aliases = listOf("oersted")),
        Unit("D", "debye", 1e-21 / C, DIPOLE, "Atomic & particle", aliases = listOf("debye")),
        Unit("P", "poise", 0.1, VISCOSITY, "CGS", prefixes = true, aliases = listOf("poise")),
        Unit("St", "stokes", 1e-4, AREA + -T, "CGS", prefixes = true, aliases = listOf("stokes"), common = false),
        Unit("Jy", "jansky", 1e-26, SPECTRAL_FLUX, "Astronomy", prefixes = true, aliases = listOf("jansky")),
        Unit("Ci", "curie", 3.7e10, FREQ, "Radiation", prefixes = true, aliases = listOf("curie"), common = false),
        Unit("rem", "rem", 0.01, ENERGY + -M, "Radiation", prefixes = true, common = false),
        // Data
        Unit("bit", "bit", 1.0, BIT, "Data", prefixes = true, aliases = listOf("bits")),
        Unit("B", "byte", 8.0, BIT, "Data", prefixes = true, aliases = listOf("byte", "bytes")),
        // Constants that work as units (natural units)
        Unit("ħ", "reduced Planck constant", HBAR, ENERGY + T, "Atomic & particle", latex = "\\hbar", aliases = listOf("hbar"), common = false),
        Unit("k_B", "Boltzmann constant", KB, ENERGY + -TEMP, "Atomic & particle", latex = "k_{B}", aliases = listOf("kB"), common = false),
    )

    private val BY_NAME: Map<String, Unit> = buildMap {
        for (u in ALL) { put(u.symbol, u); for (a in u.aliases) putIfAbsent(a, u) }
    }

    /** A unit with its prefix, as read from a unit expression. */
    class Part(val prefix: Prefix?, val unit: Unit, val power: Double) {
        val factor get() = ((prefix?.factor ?: 1.0) * unit.factor).pow(power)
        val dims get() = unit.dims * power
        fun latex(): String {
            val p = prefix?.latex?.trim() ?: ""
            val u = unit.latex
            val body = when {
                p.isEmpty() -> u
                p.startsWith("\\") -> "$p $u"
                u.startsWith("\\mathrm{") -> "\\mathrm{$p" + u.removePrefix("\\mathrm{")
                else -> "\\mathrm{$p}$u"
            }
            return body + power(power)
        }
    }

    /** A unit expression read: its size in SI base units, its dimensions, and its parts in order. */
    class Quantity(val factor: Double, val dims: Dims, val parts: List<Part>) {
        /** A lone °C or °F (to the power 1): converts with its offset. Otherwise temperatures are differences. */
        val affine: Unit? get() = parts.singleOrNull()?.takeIf { it.power == 1.0 && it.prefix == null && it.unit.offset != 0.0 }?.unit
        fun latex(): String = parts.joinToString("\\,") { it.latex() }.ifEmpty { "1" }
        fun toSI(x: Double) = affine?.let { (x + it.offset) * it.factor } ?: (x * factor)
        fun fromSI(x: Double) = affine?.let { x / it.factor - it.offset } ?: (x / factor)
    }

    class UnitError(message: String) : Exception(message)

    /** Looks up one unit name: as written, then a prefix and a unit that takes prefixes. */
    fun lookup(token: String): Pair<Prefix?, Unit>? {
        BY_NAME[token]?.let { return null to it }
        for (p in (BINARY_PREFIXES + SI_PREFIXES).sortedByDescending { it.symbol.length }) {
            if (!token.startsWith(p.symbol) || token.length == p.symbol.length) continue
            val u = BY_NAME[token.substring(p.symbol.length)] ?: continue
            val binary = p in BINARY_PREFIXES
            if (u.prefixes && (!binary || u.dims == BIT) && u.symbol == token.substring(p.symbol.length)) return p to u
        }
        return null
    }

    private val SUPERSCRIPTS = mapOf('⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4', '⁵' to '5', '⁶' to '6', '⁷' to '7', '⁸' to '8', '⁹' to '9', '⁻' to '-', '⁺' to '+', '·' to '*', '⋅' to '*', '×' to '*', '−' to '-')

    /** Reads a unit expression; an empty one (or "1") is dimensionless. */
    fun parse(text: String): Quantity {
        val src = buildString {
            var inSup = false
            for (c in text.trim()) {
                val sup = c in "⁰¹²³⁴⁵⁶⁷⁸⁹⁻⁺"
                if (sup && !inSup) append('^')
                inSup = sup
                append(SUPERSCRIPTS[c] ?: c)
            }
        }
        if (src.isBlank() || src == "1") return Quantity(1.0, NONE, emptyList())
        return Reader(src).run()
    }

    private class Reader(val s: String) {
        var i = 0
        val parts = ArrayList<Part>()

        fun run(): Quantity {
            val q = expr(1.0)
            skip()
            if (i < s.length) throw UnitError("Unexpected “${s[i]}”")
            return q.let { Quantity(it.first, it.second, parts) }
        }

        fun skip() { while (i < s.length && s[i] == ' ') i++ }

        /** product ('/' product)*: a/b/c is a/(b·c), and "J/kg K" is J/(kg·K). */
        fun expr(sign: Double): Pair<Double, Dims> {
            var (f, d) = product(sign)
            while (true) {
                skip()
                if (i < s.length && s[i] == '/') {
                    i++
                    val (f2, d2) = product(-sign)
                    f /= f2; d += -d2
                } else return f to d
            }
        }

        fun product(sign: Double): Pair<Double, Dims> {
            var (f, d) = power(sign)
            while (true) {
                skip()
                if (i < s.length && s[i] == '*') { i++; skip() }
                if (i >= s.length || s[i] == '/' || s[i] == ')') return f to d
                val (f2, d2) = power(sign)
                f *= f2; d += d2
            }
        }

        fun power(sign: Double): Pair<Double, Dims> {
            skip()
            val start = parts.size
            val (f, d) = atom(sign)
            skip()
            val k = when {
                i < s.length && s[i] == '^' -> { i++; exponent() }
                s.startsWith("**", i) -> { i += 2; exponent() }
                else -> 1.0
            }
            if (k == 1.0) return f to d
            // The exponent applies to every unit read inside (a bracket, or one unit).
            for (j in start until parts.size) parts[j] = Part(parts[j].prefix, parts[j].unit, parts[j].power * k)
            return f.pow(k) to d * k
        }

        fun exponent(): Double {
            skip()
            if (i < s.length && s[i] == '(') {
                i++
                val a = number()
                skip()
                val r = if (i < s.length && s[i] == '/') { i++; a / number() } else a
                skip()
                if (i < s.length && s[i] == ')') i++ else throw UnitError("Missing )")
                return r
            }
            return number()
        }

        fun number(): Double {
            skip()
            val m = Regex("""[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?""").matchAt(s, i) ?: throw UnitError("Expected a number")
            i += m.value.length
            return m.value.toDouble()
        }

        fun atom(sign: Double): Pair<Double, Dims> {
            skip()
            if (i >= s.length) throw UnitError("Missing a unit")
            val c = s[i]
            if (c == '(') {
                i++
                val r = expr(sign)
                skip()
                if (i < s.length && s[i] == ')') i++ else throw UnitError("Missing )")
                return r
            }
            if (c.isDigit() || c == '.') {
                var v = number()
                // 10^6 yr: a power of a number.
                skip()
                if (i < s.length && s[i] == '^') { i++; v = v.pow(exponent()) }
                return v.pow(sign) to NONE
            }
            val m = Regex("""[^\s*/()^0-9.+-][^\s*/()^]*""").matchAt(s, i) ?: throw UnitError("Unexpected “$c”")
            var token = m.value
            var k = 1.0
            // m2, cm-3, s-1: a power written straight after the unit.
            if (lookup(token) == null) {
                Regex("""^(.*?[^\d_-])(-?\d+)$""").find(token)?.let { r -> if (lookup(r.groupValues[1]) != null) { token = r.groupValues[1]; k = r.groupValues[2].toDouble() } }
            }
            i += m.value.length
            if (k == 1.0 && i < s.length && s[i] == '-' && i + 1 < s.length && s[i + 1].isDigit()) {
                i++
                k = -number()
            }
            val (prefix, unit) = lookup(token) ?: throw UnitError("Unknown unit “$token”")
            val part = Part(prefix, unit, k * sign)
            parts += part
            return (part.factor).pow(sign) to part.dims * sign
        }
    }

    // ---- Converting ----------------------------------------------------------------------------

    /** Which constants may bridge dimensions: c, h (or ħ instead), k_B. */
    class Bridges(val c: Boolean = true, val h: Boolean = true, val hbar: Boolean = false, val kB: Boolean = true)

    /** How two dimensions were bridged: x (or 1/x when [inverse]) times c^a h^b k_B^k. */
    class Bridge(val a: Int, val b: Int, val k: Int, val inverse: Boolean, val hbar: Boolean) {
        val factor get() = C.pow(a) * (if (hbar) HBAR else H).pow(b) * KB.pow(k)
        /** The relation in words and LaTeX, e.g. "Mass–energy, $E = mc^2$". */
        fun describe(): String {
            val hs = if (hbar) "\\hbar" else "h"
            val known = when (Triple(a, b, k) to inverse) {
                Triple(2, 0, 0) to false, Triple(-2, 0, 0) to false -> "Mass–energy equivalence, \$E = mc^2\$"
                Triple(0, 1, 0) to false, Triple(0, -1, 0) to false -> if (hbar) "Angular frequency, \$E = \\hbar\\omega\$" else "Photon energy, \$E = h\\nu\$"
                Triple(1, 1, 0) to true, Triple(-1, -1, 0) to true -> "Photon energy from wavelength, \$E = $hs c/\\lambda\$"
                Triple(1, 0, 0) to true, Triple(-1, 0, 0) to true -> "Frequency and wavelength, \$\\nu = c/\\lambda\$"
                Triple(1, 0, 0) to false, Triple(-1, 0, 0) to false -> "Light travel, \$d = ct\$"
                Triple(0, 0, 1) to false, Triple(0, 0, -1) to false -> "Thermal energy, \$E = k_B T\$"
                Triple(0, 1, -1) to false, Triple(0, -1, 1) to false -> "Photon temperature, \$k_B T = $hs\\nu\$"
                Triple(1, 1, -1) to true, Triple(-1, -1, 1) to true -> "Photon temperature from wavelength, \$k_B T = $hs c/\\lambda\$"
                Triple(2, 0, -1) to false, Triple(-2, 0, 1) to false -> "Rest-mass temperature, \$k_B T = mc^2\$"
                Triple(1, -1, 0) to false, Triple(-1, 1, 0) to false -> "Compton relation, \$\\lambda = $hs/mc\$"
                Triple(1, -1, 0) to true, Triple(-1, 1, 0) to true -> "Compton wavelength, \$\\lambda = $hs/mc\$"
                else -> null
            }
            val parts = listOfNotNull(
                if (a != 0) "c" + power(a.toDouble()) else null,
                if (b != 0) hs + power(b.toDouble()) else null,
                if (k != 0) "k_B" + power(k.toDouble()) else null,
            ).joinToString("\\,")
            val how = if (inverse) "\$\\dfrac{$parts}{x}\$" else "\$x\\,$parts\$"
            return (known?.let { "$it: " } ?: "Using ") + how
        }
    }

    class Result(val value: Double, val from: Quantity, val to: Quantity, val bridge: Bridge?)

    /** [value] in [from] as [to]; null target means SI. Throws [UnitError] when they can't convert. */
    fun convert(value: Double, from: Quantity, to: Quantity, bridges: Bridges = Bridges()): Result {
        if (from.dims.same(to.dims)) return Result(to.fromSI(from.toSI(value)), from, to, null)
        val bridge = bridge(from.dims, to.dims, bridges) ?: throw UnitError("Can't convert \$${from.dims.latex()}\$ to \$${to.dims.latex()}\$")
        val si = from.toSI(value)
        val out = (if (bridge.inverse) 1 / si else si) * bridge.factor
        return Result(to.fromSI(out), from, to, bridge)
    }

    /** The simplest c^a h^b k_B^k (perhaps of 1/x) taking [from] to [to], or null. */
    fun bridge(from: Dims, to: Dims, allowed: Bridges): Bridge? {
        val cD = SPEED; val hD = ENERGY + T; val kD = ENERGY + -TEMP
        var best: Bridge? = null
        var cost = Int.MAX_VALUE
        val range = -2..2
        for (inverse in listOf(false, true)) for (a in if (allowed.c) range else 0..0) for (b in if (allowed.h || allowed.hbar) range else 0..0) for (k in if (allowed.kB) range else 0..0) {
            if (a == 0 && b == 0 && k == 0) continue
            val src = if (inverse) -from else from
            if (!(src + cD * a.toDouble() + hD * b.toDouble() + kD * k.toDouble()).same(to)) continue
            val c = abs(a) + abs(b) + abs(k) + (if (inverse) 1 else 0)
            if (c < cost) { cost = c; best = Bridge(a, b, k, inverse, hbar = allowed.hbar && !allowed.h) }
        }
        return best
    }

    // ---- Writing ---------------------------------------------------------------------------------

    /** SI names for derived dimensions, then base units for anything else. */
    private val SI_NAMED = listOf("J", "W", "N", "Pa", "C", "V", "Ω", "F", "H", "T", "Wb", "S", "Hz", "Gy", "lx")
    private val CGS_NAMED = listOf("erg", "dyn", "Ba", "Gal", "P", "G", "Mx", "statC", "statV")

    /** [dims] in a system's units: "SI" (named where there is one), "base" (SI base) or "cgs". */
    fun inSystem(dims: Dims, system: String): Quantity {
        val named = when (system) { "SI" -> SI_NAMED; "cgs" -> CGS_NAMED; else -> emptyList() }
        for (n in named) {
            val u = BY_NAME.getValue(n)
            if (u.dims.same(dims)) return Quantity(u.factor, dims, listOf(Part(null, u, 1.0)))
        }
        val base = if (system == "cgs") listOf(null to "m", null to "g", null to "s", null to "A", null to "K", null to "mol", null to "cd", null to "bit")
        else listOf(null to "m", SI_PREFIXES.first { it.symbol == "k" } to "g", null to "s", null to "A", null to "K", null to "mol", null to "cd", null to "bit")
        val parts = ArrayList<Part>()
        var factor = 1.0
        for (k in 0 until N) {
            val e = dims.e[k]
            if (abs(e) < 1e-9) continue
            val (p, sym) = base[k]
            // CGS: centimeters and grams.
            val prefix = if (system == "cgs" && k == 0) SI_PREFIXES.first { it.symbol == "c" } else p
            val part = Part(prefix, BY_NAME.getValue(sym), e)
            parts += part
            factor *= part.factor
        }
        return Quantity(factor, dims, parts)
    }

    /** "SI", "base", "cgs" (any case), or a unit expression. */
    fun target(text: String, from: Dims): Quantity = when (text.trim().lowercase()) {
        "si" -> inSystem(from, "SI")
        "base", "si base" -> inSystem(from, "base")
        "cgs" -> inSystem(from, "cgs")
        else -> parse(text)
    }

    /** A value in another unit: the number, the unit in LaTeX, and as typed. */
    class Alternative(val value: Double, val latex: String, val text: String)

    /** The same quantity in other common units of its dimension, each with its best prefix. */
    fun alternatives(si: Double, dims: Dims, exclude: Set<String> = emptySet(), limit: Int = 8): List<Alternative> {
        if (dims.isNone || si == 0.0 || !si.isFinite()) return emptyList()
        val out = ArrayList<Pair<Alternative, Double>>()
        for (u in ALL) {
            if (!u.common || !u.dims.same(dims) || u.offset != 0.0 || u.symbol in exclude) continue
            val allowed = if (!u.prefixes) listOf("") else FEW_PREFIXES[u.symbol] ?: if (u.dims == BIT) listOf("", "k", "M", "G", "T") else NICE_PREFIXES
            var best: Pair<Double, Prefix?>? = null
            var score = Double.MAX_VALUE
            for (sym in allowed) {
                if (u.symbol == "g" && sym in setOf("T", "G", "M")) continue
                val p = if (sym.isEmpty()) null else SI_PREFIXES.first { it.symbol == sym }
                val v = si / ((p?.factor ?: 1.0) * u.factor)
                // Closest to 1 ≤ v < 1000 (so 1 Mpc beats 1000 kpc), with a nudge towards no prefix.
                val l = log10(abs(v))
                val s = (if (l < 0) -l else if (l >= 3 - 1e-9) l - 3 + 0.1 else 0.0) + (if (p == null) 0.0 else 0.05)
                if (s < score) { score = s; best = v to p }
            }
            val (v, p) = best ?: continue
            // A value that would still need a long power of ten isn't worth listing.
            if (score > 3.5) continue
            out += Alternative(v, Part(p, u, 1.0).latex(), (p?.symbol ?: "") + u.symbol) to score
        }
        // Temperatures: °C and °F as well.
        if (dims.same(TEMP)) for (u in ALL.filter { it.offset != 0.0 && it.common }) out += Alternative(si / u.factor - u.offset, u.latex, u.symbol) to 0.0
        return out.sortedBy { it.second }.take(limit).map { it.first }
    }

    /** What a dimension measures, if it's a familiar one. */
    fun quantityName(d: Dims): String? = listOf(
        L to "length", M to "mass", T to "time", I to "current", TEMP to "temperature", MOL to "amount of substance", CD to "luminous intensity",
        AREA to "area", VOLUME to "volume", SPEED to "speed", ACCEL to "acceleration", FORCE to "force", ENERGY to "energy", POWER to "power",
        PRESSURE to "pressure", FREQ to "frequency or rate", CHARGE to "charge", VOLT to "voltage", OHM to "resistance", FARAD to "capacitance",
        WEBER to "magnetic flux", TESLA to "magnetic field", HENRY to "inductance", SIEMENS to "conductance", VISCOSITY to "viscosity",
        DIPOLE to "dipole moment", SPECTRAL_FLUX to "spectral flux density", ENERGY + T to "action", ENERGY + -M to "absorbed dose",
        M + -VOLUME to "density", BIT to "information", BIT + -T to "data rate", ENERGY + -TEMP to "heat capacity or entropy", NONE to "dimensionless",
    ).firstOrNull { it.first.same(d) }?.second

    // ---- Numbers -------------------------------------------------------------------------------

    /** [v] to [digits] significant figures, in LaTeX: 2.268 \times 10^{-18}. */
    fun number(v: Double, digits: Int = 6): String {
        if (v.isNaN()) return "\\text{undefined}"
        if (v.isInfinite()) return if (v > 0) "\\infty" else "-\\infty"
        if (v == 0.0) return "0"
        val e = floor(log10(abs(v))).toInt()
        val sci = e >= 6 || e < -3
        val mant = if (sci) v / 10.0.pow(e) else v
        val decimals = (digits - 1 - (if (sci) 0 else e)).coerceIn(0, 15)
        var m = java.math.BigDecimal(mant).round(java.math.MathContext(digits)).setScale(decimals, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        var exp = e
        // 9.9999995 rounds up to 10: renormalize.
        if (sci && (m.trimStart('-').startsWith("10"))) { m = m.replaceFirst("10", "1").let { if (it.endsWith(".")) it.dropLast(1) else it }; exp += 1 }
        if (m == "-0") m = "0"
        return if (sci) "$m \\times 10^{$exp}" else m
    }

    /** A plain number for the clipboard and the calculator: 2.268e-18. */
    fun plain(v: Double, digits: Int = 6): String = java.math.BigDecimal(v).round(java.math.MathContext(digits)).stripTrailingZeros().toString().replace("E+", "e").replace("E", "e")

    private fun power(k: Double): String = when {
        abs(k - 1) < 1e-9 -> ""
        abs(k - k.roundToInt()) < 1e-9 -> "^{${k.roundToInt()}}"
        abs(2 * k - (2 * k).roundToInt()) < 1e-9 -> "^{${(2 * k).roundToInt()}/2}"
        else -> "^{$k}"
    }

    /** Units whose symbol, alias or name starts with [text], for suggestions. */
    fun suggest(text: String, limit: Int = 12): List<Unit> {
        if (text.isEmpty()) return emptyList()
        val t = text.lowercase()
        return ALL.filter { u -> u.symbol.startsWith(text) || u.aliases.any { it.startsWith(text) } || u.name.lowercase().startsWith(t) }
            .sortedBy { if (it.symbol.startsWith(text)) 0 else 1 }.take(limit)
    }

    val CATEGORIES: List<String> = ALL.map { it.category }.distinct()

    /** The prefixed forms people actually use, offered beside the bare unit. */
    private val FAVORITE_PREFIXED = mapOf(
        "m" to listOf("km", "cm", "mm", "µm", "nm"), "g" to listOf("kg", "mg"), "s" to listOf("ms", "µs", "ns"),
        "eV" to listOf("keV", "MeV", "GeV"), "Hz" to listOf("kHz", "MHz", "GHz"), "pc" to listOf("kpc", "Mpc", "Gpc"),
        "J" to listOf("kJ", "MJ"), "W" to listOf("kW", "MW"), "Pa" to listOf("kPa", "MPa"), "L" to listOf("mL"),
        "yr" to listOf("Myr", "Gyr"), "B" to listOf("kB", "MB", "GB", "KiB", "MiB", "GiB"), "Wh" to listOf("kWh"),
        "cal" to listOf("kcal"), "V" to listOf("mV", "kV"), "A" to listOf("mA"), "T" to listOf("mT"), "N" to listOf("kN"),
        "bar" to listOf("mbar"), "Jy" to listOf("mJy"), "b" to listOf("mb"),
    )

    /** Prefixed forms offered when a unit is reached through c, h or k_B. */
    private val BRIDGED_FORMS = mapOf("m" to listOf("nm", "µm"), "Hz" to listOf("MHz", "GHz"), "eV" to listOf("keV", "MeV"), "g" to listOf("kg"), "J" to emptyList<String>())

    /** The relations offered as one-tap targets, most familiar first. */
    private val RELATION_RANK = listOf(
        "Thermal energy", "Photon energy,", "Photon energy from wavelength", "Frequency and wavelength", "Mass–energy",
        "Rest-mass temperature", "Photon temperature,", "Photon temperature from wavelength", "Angular frequency", "Light travel", "Compton",
    )

    /** A bridge worth offering: a named relation between energy, mass, temperature, frequency, length and time. */
    private fun familiar(b: Bridge, from: Dims, to: Dims): Boolean {
        val kinds = listOf(ENERGY, M, TEMP, FREQ, L, T)
        if (kinds.none { it.same(from) } || kinds.none { it.same(to) }) return false
        if (b.describe().startsWith("Using")) return false
        if (b.b == 0 && b.k == 0 && abs(b.a) == 1 && !b.inverse) return (from.same(L) && to.same(T)) || (from.same(T) && to.same(L))
        if (b.b == 0 && b.k == 0 && abs(b.a) == 1 && b.inverse) return (from.same(L) && to.same(FREQ)) || (from.same(FREQ) && to.same(L))
        return true
    }

    /** One-tap targets for [from]: common units of the same kind first, then ones that c, h or k_B reach, simplest relation first. */
    fun compatible(from: Dims, bridges: Bridges, limit: Int = 36): List<String> {
        if (from.isNone) return emptyList()
        val same = ArrayList<String>()
        val bridged = ArrayList<Triple<String, Int, Dims>>()
        for (u in ALL) {
            if (!u.common) continue
            val sym = if (u.symbol == "fl oz") "floz" else u.symbol
            if (u.dims.same(from)) { same += sym; same += FAVORITE_PREFIXED[u.symbol].orEmpty(); continue }
            val b = bridge(from, u.dims, bridges) ?: continue
            if (!familiar(b, from, u.dims)) continue
            val cost = RELATION_RANK.indexOfFirst { b.describe().startsWith(it) }.let { if (it < 0) RELATION_RANK.size else it }
            // The bare unit, and its everyday prefixed forms for a few (nm for photons, GHz for radio).
            // The bare unit, and its everyday forms for this use (nm for photons, GHz for radio).
            bridged += Triple(sym, cost, u.dims)
            BRIDGED_FORMS[u.symbol]?.forEach { bridged += Triple(it, cost, u.dims) }
        }
        // At most four of each kind, so a length offers frequencies and temperatures too, not only energies.
        val ranked = bridged.sortedBy { it.second }.groupBy { it.third }.values.flatMap { it.take(4) }.sortedBy { it.second }.map { it.first }.distinct()
        val bridgedRoom = minOf(ranked.size, limit / 3)
        return (same.distinct().take(limit - bridgedRoom) + ranked.take(bridgedRoom)).distinct()
    }
}
