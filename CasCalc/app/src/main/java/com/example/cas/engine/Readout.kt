package com.example.cas.engine

/**
 * Number readouts (a point's coordinates, an area, a slider's value) as Markdown with the
 * math in LaTeX, for MathText: "−1.234e−05" is $-1.234 \times 10^{-5}$, "1.5∠π/3" is
 * $1.5\angle\frac{\pi}{3}$, and words stay words ("1 to 3" is $1$ to $3$).
 */
object Readout {
    private val SYMBOLS = mapOf(
        '−' to "-", 'π' to "\\pi ", '∠' to "\\angle ", '≈' to "\\approx ", '∞' to "\\infty ", '×' to "\\times ",
        '±' to "\\pm ", '∈' to "\\in ", '→' to "\\to ", '≤' to "\\le ", '≥' to "\\ge ", 'θ' to "\\theta ", '·' to "\\cdot ",
    )
    private val EXPONENT = Regex("""(\d+(?:\.\d+)?)e([+−-]?)0*(\d+)""")
    private val PI_FRACTION = Regex("""(\d*)π/(\d+)""")

    /** [text] as Markdown: the runs of numbers and symbols in $ … $, the words as they are. */
    fun markdown(text: String): String {
        val out = StringBuilder()
        val run = ArrayList<String>()
        fun flush() {
            if (run.isEmpty()) return
            if (out.isNotEmpty()) out.append(' ')
            out.append('$').append(latex(run.joinToString(" "))).append('$')
            run.clear()
        }
        for (token in text.split(' ').filter { it.isNotEmpty() }) {
            if (isWord(token)) {
                flush()
                if (out.isNotEmpty()) out.append(' ')
                out.append(token)
            } else run += token
        }
        flush()
        return out.toString()
    }

    /** Prose rather than math: a word of two or more letters with no digits or math symbols, or the dash for "none". */
    private fun isWord(token: String): Boolean {
        val core = token.trimEnd(',', '.', ':', ';')
        if (core == "—" || core == "–") return true
        return core.count { it.isLetter() } > 1 && core.none { it.isDigit() || it in SYMBOLS || it == '°' }
    }

    /** One readout, all math, as LaTeX. */
    fun latex(text: String): String {
        var s = EXPONENT.replace(text) { m ->
            val sign = if (m.groupValues[2] == "-" || m.groupValues[2] == "−") "-" else ""
            "${m.groupValues[1]} \\times 10^{$sign${m.groupValues[3]}}"
        }
        s = PI_FRACTION.replace(s) { m -> "\\frac{${m.groupValues[1]}π}{${m.groupValues[2]}}" }
        val b = StringBuilder()
        for (c in s) when {
            c == '°' -> b.append("^{\\circ}")
            c in SYMBOLS -> b.append(SYMBOLS.getValue(c))
            else -> b.append(c)
        }
        return b.toString().replace(" }", "}").replace(Regex(" +"), " ").trim()
    }
}
