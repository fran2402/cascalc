package com.example.cas.engine

import com.example.cas.cas.Expr
import com.example.cas.cas.Flt
import com.example.cas.cas.Num
import com.example.cas.math.Rational
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/** Physical dimension as exponents of mass, length, time, current, temperature and amount of substance. */
data class Dim(val m: Int = 0, val l: Int = 0, val t: Int = 0, val i: Int = 0, val k: Int = 0, val n: Int = 0)

/** One part of a constant's symbol: text with an optional subscript and superscript, e.g. m with subscript e. */
data class Piece(val text: String, val sub: String = "", val sup: String = "", val italic: Boolean = text.length == 1 && text[0].isLetter() && text[0].isLowerCase() || text in ITALIC_CAPITALS)

private val ITALIC_CAPITALS = setOf("G", "R", "F", "M", "V", "E", "K", "Z", "N")

/**
 * Systems of units for constants, chosen with the SI / Planck / atomic /
 * natural switch. A constant's SI value is divided by the system's unit for
 * its dimension, so in Planck units c = ħ = G = k_B = 1, in atomic units
 * ħ = mₑ = e = 1 and c = 1/α ≈ 137, and in natural units ħ = c = ε₀ = k_B = 1
 * with energies in eV. Amount of substance stays in moles in every system.
 */
enum class UnitSystem(val label: String) {
    SI("SI"), Planck("Planck"), Atomic("Atomic"), Natural("Natural");

    /** SI size of this system's unit of mass, length, time, current and temperature. */
    val units: DoubleArray by lazy {
        val c = 299792458.0
        val h = 6.62607015e-34
        val hbar = h / (2 * PI)
        val kB = 1.380649e-23
        val e = 1.602176634e-19
        val eps0 = 8.8541878188e-12
        val gN = 6.67430e-11
        when (this) {
            SI -> doubleArrayOf(1.0, 1.0, 1.0, 1.0, 1.0)
            Planck -> {
                val m = sqrt(hbar * c / gN)
                val l = sqrt(hbar * gN / c.pow(3))
                val t = l / c
                val q = sqrt(4 * PI * eps0 * hbar * c)
                doubleArrayOf(m, l, t, q / t, m * c * c / kB)
            }
            Atomic -> {
                val me = 9.1093837139e-31
                val a0 = 5.29177210544e-11
                val eh = 4.3597447222060e-18
                val t = hbar / eh
                doubleArrayOf(me, a0, t, e / t, eh / kB)
            }
            Natural -> {
                val ev = e // 1 eV in joules
                val t = hbar / ev
                val q = sqrt(eps0 * hbar * c)
                doubleArrayOf(ev / (c * c), hbar * c / ev, t, q / t, ev / kB)
            }
        }
    }

    fun scale(d: Dim): Double {
        val u = units
        return u[0].pow(d.m) * u[1].pow(d.l) * u[2].pow(d.t) * u[3].pow(d.i) * u[4].pow(d.k)
    }
}

/**
 * Every constant in Wikipedia's "List of physical constants" (CODATA 2022),
 * plus standard gravity. [exact] constants are defined exactly by the SI and
 * have terminating decimals, so they stay exact fractions in SI mode; the
 * rest are decimals. [unit] is the SI unit as shown in the list.
 */
enum class Constant(
    val id: String,
    val pieces: List<Piece>,
    val description: String,
    val unit: String,
    val dim: Dim,
    val decimal: String,
    val exact: Boolean = false,
) {
    SpeedOfLight("c0", listOf(Piece("c")), "Speed of light in vacuum", "m/s", Dim(l = 1, t = -1), "299792458", exact = true),
    Planck("h", listOf(Piece("h")), "Planck constant", "J·s", Dim(1, 2, -1), "6.62607015e-34", exact = true),
    ReducedPlanck("hbar", listOf(Piece("ℏ", italic = false)), "Reduced Planck constant", "J·s", Dim(1, 2, -1), "1.054571817646156e-34"),
    Boltzmann("kB", listOf(Piece("k", sub = "B")), "Boltzmann constant", "J/K", Dim(1, 2, -2, 0, -1), "1.380649e-23", exact = true),
    Gravitation("G", listOf(Piece("G")), "Newtonian constant of gravitation", "m³/(kg·s²)", Dim(-1, 3, -2), "6.67430e-11"),
    Cosmological("Lambda", listOf(Piece("Λ", italic = false)), "Cosmological constant", "m⁻²", Dim(l = -2), "1.089e-52"),
    StefanBoltzmann("sigma", listOf(Piece("σ", italic = true)), "Stefan–Boltzmann constant", "W/(m²·K⁴)", Dim(1, 0, -3, 0, -4), "5.670374419184429e-8"),
    FirstRadiation("c1", listOf(Piece("c", sub = "1")), "First radiation constant", "W·m²", Dim(1, 4, -3), "3.741771852192758e-16"),
    FirstRadiationL("c1L", listOf(Piece("c", sub = "1L")), "First radiation constant for spectral radiance", "W·m²/sr", Dim(1, 4, -3), "1.19104297239718841407948920e-16", exact = true),
    SecondRadiation("c2", listOf(Piece("c", sub = "2")), "Second radiation constant", "m·K", Dim(l = 1, k = 1), "1.438776877503933e-2"),
    WienWavelength("b", listOf(Piece("b")), "Wien wavelength displacement law constant", "m·K", Dim(l = 1, k = 1), "2.897771955185172e-3"),
    WienFrequency("bprime", listOf(Piece("b", sup = "′")), "Wien frequency displacement law constant", "Hz/K", Dim(t = -1, k = -1), "5.878925757646824e10"),
    WienEntropy("bentropy", listOf(Piece("b", sub = "entropy")), "Wien entropy displacement law constant", "m·K", Dim(l = 1, k = 1), "3.002916077e-3"),
    ElementaryCharge("qe", listOf(Piece("e", sup = "−")), "Elementary charge", "C", Dim(t = 1, i = 1), "1.602176634e-19", exact = true),
    ConductanceQuantum("G0", listOf(Piece("G", sub = "0")), "Conductance quantum", "S", Dim(-1, -2, 3, 2), "7.748091729863650e-5"),
    InverseConductanceQuantum("G0inv", listOf(Piece("G", sub = "0", sup = "−1")), "Inverse conductance quantum", "Ω", Dim(1, 2, -3, -2), "12906.40372965225"),
    VonKlitzing("RK", listOf(Piece("R", sub = "K")), "von Klitzing constant", "Ω", Dim(1, 2, -3, -2), "25812.80745930450"),
    Josephson("KJ", listOf(Piece("K", sub = "J")), "Josephson constant", "Hz/V", Dim(-1, -2, 2, 1), "483597.8484169836e9"),
    FluxQuantum("Phi0", listOf(Piece("Φ", sub = "0", italic = false)), "Magnetic flux quantum", "Wb", Dim(1, 2, -2, -1), "2.067833848461929e-15"),
    FineStructure("alpha", listOf(Piece("α", italic = true)), "Fine-structure constant", "", Dim(), "7.2973525643e-3"),
    InverseFineStructure("alphainv", listOf(Piece("α", sup = "−1", italic = true)), "Inverse fine-structure constant", "", Dim(), "137.035999177"),
    Permeability("mu0", listOf(Piece("μ", sub = "0", italic = true)), "Vacuum magnetic permeability", "N/A²", Dim(1, 1, -2, -2), "1.25663706127e-6"),
    Impedance("Z0", listOf(Piece("Z", sub = "0")), "Characteristic impedance of vacuum", "Ω", Dim(1, 2, -3, -2), "376.730313412"),
    Permittivity("eps0", listOf(Piece("ε", sub = "0", italic = true)), "Vacuum electric permittivity", "F/m", Dim(-1, -3, 4, 2), "8.8541878188e-12"),
    ElectronMass("me", listOf(Piece("m", sub = "e")), "Electron mass", "kg", Dim(m = 1), "9.1093837139e-31"),
    MuonMass("mmu", listOf(Piece("m", sub = "μ")), "Muon mass", "kg", Dim(m = 1), "1.883531627e-28"),
    TauMass("mtau", listOf(Piece("m", sub = "τ")), "Tau mass", "kg", Dim(m = 1), "3.16754e-27"),
    ProtonMass("mp", listOf(Piece("m", sub = "p")), "Proton mass", "kg", Dim(m = 1), "1.67262192595e-27"),
    NeutronMass("mn", listOf(Piece("m", sub = "n")), "Neutron mass", "kg", Dim(m = 1), "1.67492750056e-27"),
    ProtonElectronRatio("mpme", listOf(Piece("m", sub = "p"), Piece("/", italic = false), Piece("m", sub = "e")), "Proton-to-electron mass ratio", "", Dim(), "1836.152673426"),
    WZRatio("mWmZ", listOf(Piece("m", sub = "W"), Piece("/", italic = false), Piece("m", sub = "Z")), "W-to-Z mass ratio", "", Dim(), "0.88145"),
    WeakMixing("sin2thetaW", listOf(Piece("sin", sup = "2", italic = false), Piece("θ", sub = "W", italic = true)), "Sine-square weak mixing angle", "", Dim(), "0.22305"),
    ElectronG("ge", listOf(Piece("g", sub = "e")), "Electron g-factor", "", Dim(), "-2.00231930436092"),
    MuonG("gmu", listOf(Piece("g", sub = "μ")), "Muon g-factor", "", Dim(), "-2.00233184123"),
    ProtonG("gp", listOf(Piece("g", sub = "p")), "Proton g-factor", "", Dim(), "5.5856946893"),
    QuantumOfCirculation("h2me", listOf(Piece("h"), Piece("/2", italic = false), Piece("m", sub = "e")), "Quantum of circulation", "m²/s", Dim(l = 2, t = -1), "3.6369475467e-4"),
    BohrMagneton("muB", listOf(Piece("μ", sub = "B", italic = true)), "Bohr magneton", "J/T", Dim(l = 2, i = 1), "9.2740100657e-24"),
    NuclearMagneton("muN", listOf(Piece("μ", sub = "N", italic = true)), "Nuclear magneton", "J/T", Dim(l = 2, i = 1), "5.0507837393e-27"),
    ElectronRadius("re", listOf(Piece("r", sub = "e")), "Classical electron radius", "m", Dim(l = 1), "2.8179403205e-15"),
    ThomsonCrossSection("sigmae", listOf(Piece("σ", sub = "e", italic = true)), "Thomson cross section", "m²", Dim(l = 2), "6.6524587051e-29"),
    BohrRadius("a0", listOf(Piece("a", sub = "0")), "Bohr radius", "m", Dim(l = 1), "5.29177210544e-11"),
    Rydberg("Rinf", listOf(Piece("R", sub = "∞")), "Rydberg constant", "m⁻¹", Dim(l = -1), "10973731.568157"),
    RydbergEnergy("Ry", listOf(Piece("Ry", italic = false)), "Rydberg unit of energy", "J", Dim(1, 2, -2), "2.1798723611030e-18"),
    Hartree("Eh", listOf(Piece("E", sub = "h")), "Hartree energy", "J", Dim(1, 2, -2), "4.3597447222060e-18"),
    // G_F/(ħc)³ = 1.1663787×10⁻⁵ GeV⁻², stored in J⁻².
    Fermi("GF", listOf(Piece("G", sub = "F")), "Fermi coupling constant, G_F/(ħc)³", "J⁻²", Dim(-2, -4, 4), "4.543795662612158e14"),
    Avogadro("NA", listOf(Piece("N", sub = "A")), "Avogadro constant", "mol⁻¹", Dim(n = -1), "6.02214076e23", exact = true),
    GasConstant("R", listOf(Piece("R")), "Molar gas constant", "J/(mol·K)", Dim(1, 2, -2, 0, -1, -1), "8.31446261815324", exact = true),
    Faraday("F", listOf(Piece("F")), "Faraday constant", "C/mol", Dim(t = 1, i = 1, n = -1), "96485.3321233100184", exact = true),
    MolarPlanck("NAh", listOf(Piece("N", sub = "A"), Piece("h")), "Molar Planck constant", "J·s/mol", Dim(1, 2, -1, 0, 0, -1), "3.9903127128934314e-10", exact = true),
    MolarMassC12("MC12", listOf(Piece("M(¹²C)", italic = false)), "Molar mass of carbon-12", "kg/mol", Dim(m = 1, n = -1), "12.0000000126e-3"),
    AtomicMass("mu", listOf(Piece("m", sub = "u")), "Atomic mass constant", "kg", Dim(m = 1), "1.66053906892e-27"),
    MolarMass("Mu", listOf(Piece("M", sub = "u")), "Molar mass constant", "kg/mol", Dim(m = 1, n = -1), "1.00000000105e-3"),
    MolarVolumeSi("VmSi", listOf(Piece("V", sub = "m"), Piece("(Si)", italic = false)), "Molar volume of silicon", "m³/mol", Dim(l = 3, n = -1), "1.205883199e-5"),
    CaesiumFrequency("nuCs", listOf(Piece("Δν", sub = "Cs", italic = false)), "Hyperfine transition frequency of ¹³³Cs", "Hz", Dim(t = -1), "9192631770", exact = true),
    Gravity("g0", listOf(Piece("g", sub = "0")), "Standard gravity (not in the list; a defined value)", "m/s²", Dim(l = 1, t = -2), "9.80665", exact = true),
    ;

    /** The value in [system]: exact in SI when defined exactly, otherwise a decimal. */
    fun value(system: UnitSystem = UnitSystem.SI): Expr {
        if (system == UnitSystem.SI) {
            return if (exact) Num(Rational.ofDecimalString(decimal)) else Flt(decimal.toDouble())
        }
        val v = decimal.toDouble() / system.scale(dim)
        // Constants that the system sets to 1 come out as exactly 1.
        if (abs(v - 1) < 1e-9) return Num(1)
        return Flt(v)
    }

    /** Backwards compatible: the SI value. */
    val value: Expr get() = value(UnitSystem.SI)

    companion object {
        fun byId(id: String) = entries.firstOrNull { it.id == id }
    }
}
