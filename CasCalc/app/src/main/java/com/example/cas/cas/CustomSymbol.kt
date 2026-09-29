package com.example.cas.cas

/** Accents a custom symbol can carry, as LaTeX writes them, with a spacing glyph to draw above the letter. */
enum class Accent(val latex: String, val glyph: String, val label: String) {
    Dot("\\dot", "˙", "dot"),
    DoubleDot("\\ddot", "¨", "double dot"),
    Hat("\\hat", "ˆ", "hat"),
    Tilde("\\tilde", "˜", "tilde"),
    Bar("\\bar", "¯", "bar"),
    Vector("\\vec", "→", "arrow"),
    Check("\\check", "ˇ", "check"),
    Breve("\\breve", "˘", "breve"),
    Acute("\\acute", "´", "acute"),
    Grave("\\grave", "`", "grave"),
}

/**
 * A symbol built in the symbol builder: a base letter, an optional accent, and optional
 * subscript and superscript (x̂₁, ṽ², 𝔤ᵢ*). It's stored as one symbol text, so it's a variable
 * like any letter: a private-use marker, then the parts separated by U+001F.
 */
data class CustomSymbol(val base: String, val accent: Accent? = null, val sub: String = "", val sup: String = "") {
    init { require(base.isNotEmpty()) { "A symbol needs a letter" } }

    fun encode(): String = MARK + base + SEP + (accent?.name ?: "") + SEP + sub + SEP + sup

    /** LaTeX: \hat{x}_{1}^{2}. Letters in scripts stay as typed; ′ becomes \prime. */
    val latex: String
        get() {
            val b = com.example.cas.editor.MathAlphabets.decode(base.codePointAt(0))?.let { (style, c) ->
                (if (style == com.example.cas.editor.MathAlphabets.Style.Calligraphic) "\\mathcal{" else "\\mathfrak{") + c + "}"
            } ?: GREEK_LATEX[base] ?: base
            val withAccent = accent?.let { "${it.latex}{$b}" } ?: b
            fun script(s: String) = s.replace("′", "\\prime ").replace("−", "-").map { GREEK_LATEX[it.toString()] ?: it.toString() }.joinToString("")
            return withAccent + (if (sub.isNotEmpty()) "_{${script(sub)}}" else "") + (if (sup.isNotEmpty()) "^{${script(sup)}}" else "")
        }

    /** Plain text for copying: x̂_1^2 (the accent as a combining mark). */
    val plain: String
        get() = base + (accent?.let { COMBINING[it] } ?: "") + (if (sub.isNotEmpty()) "_$sub" else "") + (if (sup.isNotEmpty()) "^$sup" else "")

    companion object {
        const val MARK = "\uE000"
        const val SEP = "\u001F"

        fun isCustom(text: String) = text.startsWith(MARK)

        fun decode(text: String): CustomSymbol? {
            if (!isCustom(text)) return null
            val parts = text.removePrefix(MARK).split(SEP)
            if (parts.size != 4 || parts[0].isEmpty()) return null
            return CustomSymbol(parts[0], parts[1].takeIf { it.isNotEmpty() }?.let { n -> Accent.entries.firstOrNull { it.name == n } }, parts[2], parts[3])
        }

        private val COMBINING = mapOf(
            Accent.Dot to "\u0307", Accent.DoubleDot to "\u0308", Accent.Hat to "\u0302", Accent.Tilde to "\u0303", Accent.Bar to "\u0304",
            Accent.Vector to "\u20D7", Accent.Check to "\u030C", Accent.Breve to "\u0306", Accent.Acute to "\u0301", Accent.Grave to "\u0300",
        )
        private val GREEK_LATEX = mapOf(
            "α" to "\\alpha", "β" to "\\beta", "γ" to "\\gamma", "δ" to "\\delta", "ε" to "\\epsilon", "ζ" to "\\zeta", "η" to "\\eta",
            "θ" to "\\theta", "ι" to "\\iota", "κ" to "\\kappa", "λ" to "\\lambda", "μ" to "\\mu", "ν" to "\\nu", "ξ" to "\\xi",
            "π" to "\\pi", "ρ" to "\\rho", "σ" to "\\sigma", "τ" to "\\tau", "υ" to "\\upsilon", "φ" to "\\phi", "χ" to "\\chi",
            "ψ" to "\\psi", "ω" to "\\omega", "Γ" to "\\Gamma", "Δ" to "\\Delta", "Θ" to "\\Theta", "Λ" to "\\Lambda", "Ξ" to "\\Xi",
            "Π" to "\\Pi", "Σ" to "\\Sigma", "Φ" to "\\Phi", "Ψ" to "\\Psi", "Ω" to "\\Omega",
        )
    }
}
