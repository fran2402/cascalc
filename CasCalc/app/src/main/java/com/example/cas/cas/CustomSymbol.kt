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
    // Added later (saved symbols name their accent, so the order doesn't matter).
    Ring("\\mathring", "˚", "ring"),
}

/**
 * A symbol built in the symbol builder: a base letter, an optional accent, subscripts and
 * superscripts after it and (as in isotopes, ²₁H) before it, and bold (x̂₁, ṽ², 𝔤ᵢ*, ¹⁴C,
 * bold v). It's stored as one symbol text, so it's a variable like any letter: a private-use
 * marker, then the parts separated by U+001F (the last three only when they're used, so
 * symbols saved before they existed read the same).
 */
data class CustomSymbol(
    val base: String,
    val accent: Accent? = null,
    val sub: String = "",
    val sup: String = "",
    val preSub: String = "",
    val preSup: String = "",
    val bold: Boolean = false,
    /**
     * Scripts written upright, as text rather than as math (typed from the keyboard, or with
     * italic off): letters from "s" subscript, "p" superscript, "l" left subscript, "q" left
     * superscript; "u" is the letter itself upright (\mathrm{d}, or a word: \text{max}).
     */
    val upright: String = "",
) {
    fun isUpright(slot: Char) = slot in upright

    init { require(base.isNotEmpty()) { "A symbol needs a letter" } }

    fun encode(): String {
        val core = MARK + base + SEP + (accent?.name ?: "") + SEP + sub + SEP + sup
        val flags = (if (bold) "b" else "") + upright.toSortedSet().joinToString("")
        return if (preSub.isEmpty() && preSup.isEmpty() && flags.isEmpty()) core else core + SEP + preSub + SEP + preSup + SEP + flags
    }

    /** LaTeX: {}_{1}^{2}\hat{x}_{1}^{2}, \boldsymbol{v}. Letters in scripts stay as typed; ′ becomes \prime. */
    val latex: String
        get() {
            val letter = com.example.cas.editor.MathAlphabets.decode(base.codePointAt(0))?.let { (style, c) -> com.example.cas.editor.MathAlphabets.latex(style, c) }
                ?: LATEX_NAMES[base] ?: base
            val plainLetter = if (isUpright('u')) (if (base.length == 1) "\\mathrm{$base}" else "\\text{$base}") else letter
            val b = if (bold) "\\boldsymbol{$plainLetter}" else plainLetter
            val withAccent = accent?.let { "${it.latex}{$b}" } ?: b
            fun math(s: String) = s.map { LATEX_NAMES[it.toString()]?.let { n -> "$n " } ?: it.toString().replace("−", "-") }.joinToString("").trim()
            // An upright script is text: \text{max}.
            fun script(s: String, slot: Char) = if (isUpright(slot)) "\\text{$s}" else math(s)
            val pre = if (preSub.isEmpty() && preSup.isEmpty()) "" else "{}" + (if (preSub.isNotEmpty()) "_{${script(preSub, 'l')}}" else "") + (if (preSup.isNotEmpty()) "^{${script(preSup, 'q')}}" else "")
            return pre + withAccent + (if (sub.isNotEmpty()) "_{${script(sub, 's')}}" else "") + (if (sup.isNotEmpty()) "^{${script(sup, 'p')}}" else "")
        }

    /** Plain text for copying: x̂_1^2 (the accent as a combining mark). */
    val plain: String
        get() = (if (preSub.isNotEmpty()) "_$preSub" else "") + (if (preSup.isNotEmpty()) "^$preSup" else "") +
            base + (accent?.let { COMBINING[it] } ?: "") + (if (sub.isNotEmpty()) "_$sub" else "") + (if (sup.isNotEmpty()) "^$sup" else "")

    companion object {
        const val MARK = ""
        const val SEP = "\u001F"

        fun isCustom(text: String) = text.startsWith(MARK)

        fun decode(text: String): CustomSymbol? {
            if (!isCustom(text)) return null
            val parts = text.removePrefix(MARK).split(SEP)
            if ((parts.size != 4 && parts.size != 7) || parts[0].isEmpty()) return null
            val flags = parts.getOrElse(6) { "" }
            return CustomSymbol(
                parts[0], parts[1].takeIf { it.isNotEmpty() }?.let { n -> Accent.entries.firstOrNull { it.name == n } }, parts[2], parts[3],
                parts.getOrElse(4) { "" }, parts.getOrElse(5) { "" }, 'b' in flags, flags.filter { it in "splqu" },
            )
        }

        /**
         * A symbol written in LaTeX, as the builder's text field takes it: an optional
         * {}_{a}^{b} before, an accent (\hat, \vec…), \boldsymbol or \mathbf, a letter (x, \alpha,
         * \aleph, \mathcal{A}, \mathfrak{g}, \mathbb{R}…), then _ and ^ (braced, or one character).
         * Null if it isn't one symbol.
         */
        fun fromLatex(latex: String): CustomSymbol? = runCatching { LatexReader(latex.trim()).symbol() }.getOrNull()

        private val COMBINING = mapOf(
            Accent.Dot to "̇", Accent.DoubleDot to "̈", Accent.Hat to "̂", Accent.Tilde to "̃", Accent.Bar to "̄",
            Accent.Vector to "⃗", Accent.Check to "̌", Accent.Breve to "̆", Accent.Acute to "́", Accent.Grave to "̀",
            Accent.Ring to "̊",
        )

        /** Letters and signs with a LaTeX name. */
        val LATEX_NAMES: Map<String, String> = mapOf(
            "α" to "\\alpha", "β" to "\\beta", "γ" to "\\gamma", "δ" to "\\delta", "ε" to "\\epsilon", "ζ" to "\\zeta", "η" to "\\eta",
            "θ" to "\\theta", "ι" to "\\iota", "κ" to "\\kappa", "λ" to "\\lambda", "μ" to "\\mu", "ν" to "\\nu", "ξ" to "\\xi",
            "ο" to "o", "π" to "\\pi", "ρ" to "\\rho", "σ" to "\\sigma", "τ" to "\\tau", "υ" to "\\upsilon", "φ" to "\\phi", "χ" to "\\chi",
            "ψ" to "\\psi", "ω" to "\\omega", "ϵ" to "\\varepsilon", "ϑ" to "\\vartheta", "ϕ" to "\\varphi", "ϱ" to "\\varrho",
            "ς" to "\\varsigma", "ϖ" to "\\varpi", "ϰ" to "\\varkappa",
            "Α" to "\\mathrm{A}", "Β" to "\\mathrm{B}", "Γ" to "\\Gamma", "Δ" to "\\Delta", "Ε" to "\\mathrm{E}", "Ζ" to "\\mathrm{Z}", "Η" to "\\mathrm{H}",
            "Θ" to "\\Theta", "Ι" to "\\mathrm{I}", "Κ" to "\\mathrm{K}", "Λ" to "\\Lambda", "Μ" to "\\mathrm{M}", "Ν" to "\\mathrm{N}", "Ξ" to "\\Xi",
            "Ο" to "\\mathrm{O}", "Π" to "\\Pi", "Ρ" to "\\mathrm{P}", "Σ" to "\\Sigma", "Τ" to "\\mathrm{T}", "Υ" to "\\Upsilon", "Φ" to "\\Phi",
            "Χ" to "\\mathrm{X}", "Ψ" to "\\Psi", "Ω" to "\\Omega",
            "ℵ" to "\\aleph", "ℶ" to "\\beth", "ℷ" to "\\gimel", "ℸ" to "\\daleth", "ℓ" to "\\ell", "ℏ" to "\\hbar", "∂" to "\\partial", "∇" to "\\nabla",
            "′" to "\\prime", "″" to "\\prime\\prime", "∞" to "\\infty", "†" to "\\dagger", "‡" to "\\ddagger", "∘" to "\\circ", "⊥" to "\\perp",
            "∥" to "\\parallel", "±" to "\\pm", "∓" to "\\mp", "·" to "\\cdot", "×" to "\\times", "⋆" to "\\star", "°" to "^\\circ",
        )

        /** Back from a LaTeX name to its character (\alpha → α). */
        private val FROM_LATEX: Map<String, String> by lazy {
            LATEX_NAMES.entries.filter { it.value.startsWith("\\") && !it.value.startsWith("\\mathrm") && !it.value.contains("\\prime\\prime") && !it.value.startsWith("^") }
                .associate { it.value.removePrefix("\\") to it.key } +
                mapOf("ast" to "*", "varepsilon" to "ϵ", "le" to "≤", "ge" to "≥")
        }

        private class LatexReader(val s: String) {
            var i = 0
            fun skip() { while (i < s.length && s[i] == ' ') i++ }
            fun peek() = s.getOrNull(i)
            fun command(): String {
                i++ // the backslash
                val start = i
                while (i < s.length && s[i].isLetter()) i++
                if (i == start && i < s.length) i++
                return s.substring(start, i)
            }
            /** A braced group's raw text, or one character (or one command). */
            fun group(): String {
                skip()
                return when (peek()) {
                    '{' -> {
                        var depth = 0; val start = i
                        while (i < s.length) { if (s[i] == '{') depth++; if (s[i] == '}') { depth--; if (depth == 0) break }; i++ }
                        require(i < s.length) { "unclosed" }
                        s.substring(start + 1, i).also { i++ }
                    }
                    '\\' -> "\\" + command()
                    null -> error("missing")
                    else -> s[i++].toString()
                }
            }
            /** A script's text: commands for letters and signs become the characters. */
            fun scriptText(t: String): String {
                val out = StringBuilder()
                var k = 0
                while (k < t.length) {
                    val c = t[k]
                    when {
                        c == '\\' -> {
                            var e = k + 1
                            while (e < t.length && t[e].isLetter()) e++
                            val name = t.substring(k + 1, e)
                            out.append(FROM_LATEX[name] ?: error("unknown \\$name")); k = e
                        }
                        c == '{' || c == '}' || c == ' ' -> k++
                        c == '-' -> { out.append('−'); k++ }
                        c == '\'' -> { out.append('′'); k++ }
                        else -> { out.append(c); k++ }
                    }
                }
                return out.toString()
            }
            /** The letter was written \mathrm{…} or \text{…}: upright. */
            var uprightBase = false
            /** Which scripts were written as \text{…} (upright): sub, sup. */
            var uprightSub = false
            var uprightSup = false
            /** A script's content: \text{…}, \mathrm{…} or \textrm{…} (upright, kept as typed), or math. */
            fun script(g: String, set: (Boolean) -> Unit): String {
                val m = Regex("""^\s*\\(text|mathrm|textrm|textit|operatorname)\s*\{(.*)\}\s*$""").find(g)
                return if (m != null) { set(true); m.groupValues[2] } else { set(false); scriptText(g) }
            }
            fun scripts(): Pair<String, String> {
                var sub = ""; var sup = ""
                uprightSub = false; uprightSup = false
                while (true) {
                    skip()
                    when (peek()) {
                        '_' -> { i++; sub = script(group()) { uprightSub = it } }
                        '^' -> { i++; sup = script(group()) { uprightSup = it } }
                        '\'' -> { i++; sup += "′" }
                        else -> return sub to sup
                    }
                }
            }
            /** The letter, possibly inside an accent and \boldsymbol: (letter, accent, bold). */
            fun base(): Triple<String, Accent?, Boolean> {
                skip()
                if (peek() == '{') {
                    val inner = LatexReader(group()).base()
                    return inner
                }
                if (peek() != '\\') { require(peek()?.isLetter() == true) { "a letter" }; return Triple(s[i++].toString(), null, false) }
                val name = command()
                val accent = Accent.entries.firstOrNull { it.latex == "\\$name" } ?: when (name) { "overline" -> Accent.Bar; "widehat" -> Accent.Hat; "widetilde" -> Accent.Tilde; "overrightarrow" -> Accent.Vector; else -> null }
                if (accent != null) { val (l, _, bold) = LatexReader(group()).base(); return Triple(l, accent, bold) }
                return when (name) {
                    "boldsymbol", "mathbf", "bm" -> { val (l, a, _) = LatexReader(group()).base(); Triple(l, a, true) }
                    "mathcal" -> Triple(com.example.cas.editor.MathAlphabets.calligraphic(group().single()), null, false)
                    "mathfrak" -> Triple(com.example.cas.editor.MathAlphabets.fraktur(group().single()), null, false)
                    "mathbb" -> Triple(com.example.cas.editor.MathAlphabets.doubleStruck(group().single()), null, false)
                    // \text{…} as the letter: upright text (a word works too, as a name: \text{max}).
                    "mathrm", "text", "textrm", "operatorname" -> { uprightBase = true; Triple(group().trim().also { require(it.isNotEmpty()) { "a letter" } }, null, false) }
                    "mathit" -> Triple(group().trim().also { require(it.length == 1) { "one letter" } }, null, false)
                    else -> {
                        val greek = FROM_LATEX[name] ?: GREEK_CAPS[name] ?: error("unknown \\$name")
                        Triple(greek, null, false)
                    }
                }
            }
            fun symbol(): CustomSymbol {
                skip()
                var preSub = ""; var preSup = ""
                var upright = ""
                if (s.startsWith("{}", i)) {
                    i += 2
                    val (a, b) = scripts(); preSub = a; preSup = b
                    if (uprightSub) upright += "l"; if (uprightSup) upright += "q"
                }
                val (letter, accent, bold) = base()
                val (sub, sup) = scripts()
                if (uprightSub) upright += "s"; if (uprightSup) upright += "p"
                if (uprightBase) upright += "u"
                skip()
                require(i >= s.length) { "one symbol" }
                return CustomSymbol(letter, accent, sub, sup, preSub, preSup, bold, upright.filter { c -> when (c) { 's' -> sub.isNotEmpty(); 'p' -> sup.isNotEmpty(); 'l' -> preSub.isNotEmpty(); 'q' -> preSup.isNotEmpty(); else -> true } })
            }
        }

        private val GREEK_CAPS = mapOf("Alpha" to "Α", "Beta" to "Β", "Epsilon" to "Ε", "Zeta" to "Ζ", "Eta" to "Η", "Iota" to "Ι", "Kappa" to "Κ", "Mu" to "Μ", "Nu" to "Ν", "Omicron" to "Ο", "Rho" to "Ρ", "Tau" to "Τ", "Chi" to "Χ", "omicron" to "ο")
    }
}
