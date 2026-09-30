package com.example.cas.editor

/**
 * LaTeX's \mathcal (capitals) and \mathfrak (capitals and small letters) as the Unicode
 * mathematical letters, which is how they're stored in expressions (𝒜, 𝔄, 𝔞). Some live in
 * the older Letterlike Symbols block (ℬ, ℰ, ℜ…), hence the exceptions. They're drawn with
 * TeX's Caligraphic and Fraktur fonts, which put those shapes on the plain letters.
 */
object MathAlphabets {
    enum class Style { Calligraphic, Fraktur, DoubleStruck }

    /** The LaTeX for a letter in [style]: \mathcal{A}, \mathfrak{g}, \mathbb{R}. */
    fun latex(style: Style, c: Char): String = when (style) {
        Style.Calligraphic -> "\\mathcal{$c}"
        Style.Fraktur -> "\\mathfrak{$c}"
        Style.DoubleStruck -> "\\mathbb{$c}"
    }

    private val SCRIPT_EXCEPTIONS = mapOf('B' to 0x212C, 'E' to 0x2130, 'F' to 0x2131, 'H' to 0x210B, 'I' to 0x2110, 'L' to 0x2112, 'M' to 0x2133, 'R' to 0x211B)
    private val DOUBLE_STRUCK_EXCEPTIONS = mapOf('C' to 0x2102, 'H' to 0x210D, 'N' to 0x2115, 'P' to 0x2119, 'Q' to 0x211A, 'R' to 0x211D, 'Z' to 0x2124)
    private val FRAKTUR_EXCEPTIONS = mapOf('C' to 0x212D, 'H' to 0x210C, 'I' to 0x2111, 'R' to 0x211C, 'Z' to 0x2128)

    private fun cp(c: Int) = String(Character.toChars(c))

    /** 𝒜 for A (capitals only, as \mathcal). */
    fun calligraphic(c: Char): String = cp(SCRIPT_EXCEPTIONS[c] ?: (0x1D49C + (c - 'A')))

    /** 𝔄 for A, 𝔞 for a. */
    fun fraktur(c: Char): String =
        if (c.isUpperCase()) cp(FRAKTUR_EXCEPTIONS[c] ?: (0x1D504 + (c - 'A'))) else cp(0x1D51E + (c - 'a'))

    /** ℝ for R, 𝕒 for a (blackboard bold, \mathbb). */
    fun doubleStruck(c: Char): String =
        if (c.isUpperCase()) cp(DOUBLE_STRUCK_EXCEPTIONS[c] ?: (0x1D538 + (c - 'A'))) else cp(0x1D552 + (c - 'a'))

    private val decoded: Map<Int, Pair<Style, Char>> = buildMap {
        for (c in 'A'..'Z') {
            put(calligraphic(c).codePointAt(0), Style.Calligraphic to c)
            put(fraktur(c).codePointAt(0), Style.Fraktur to c)
        }
        for (c in 'a'..'z') put(fraktur(c).codePointAt(0), Style.Fraktur to c)
        for (c in ('A'..'Z') + ('a'..'z')) put(doubleStruck(c).codePointAt(0), Style.DoubleStruck to c)
    }

    /** The style and plain letter for one of these characters, or null. */
    fun decode(codePoint: Int): Pair<Style, Char>? = decoded[codePoint]

    /** Whether [t] is exactly one of these letters (so it can be a variable). */
    fun isMathLetter(t: String): Boolean = t.isNotEmpty() && t.length == Character.charCount(t.codePointAt(0)) && decode(t.codePointAt(0)) != null
}
