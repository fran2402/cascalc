package com.example.cas.engine

import com.example.cas.editor.BigOp
import com.example.cas.editor.BigOpKind
import com.example.cas.editor.Binom
import com.example.cas.editor.Frac
import com.example.cas.editor.Func
import com.example.cas.editor.Integral
import com.example.cas.editor.MathRow
import com.example.cas.editor.Matrix
import com.example.cas.editor.Node
import com.example.cas.editor.Pow
import com.example.cas.editor.Root
import com.example.cas.editor.Scripted
import com.example.cas.editor.Sqrt
import com.example.cas.editor.Sym

/**
 * Reads the LaTeX used in formulas (the key help is written in it) into the
 * calculator's own math tree, so it's drawn by the same renderer as answers:
 * \frac, \sqrt, ^ and _, \int … dx, \oint, \sum, \prod, \lim, \binom,
 * bmatrix, \left/\right, \text, \operatorname, \bar, Greek letters and the
 * usual symbols. Anything unknown is shown as its name.
 */
object LatexParser {
    /** Marks a one-letter symbol as upright (\mathrm{m}); the renderer doesn't draw it. U+2060, word joiner. */
    const val UPRIGHT = "\u2060"

    private val SYMBOLS = mapOf(
        "alpha" to "α", "beta" to "β", "gamma" to "γ", "delta" to "δ", "epsilon" to "ε", "varepsilon" to "ε",
        "zeta" to "ζ", "eta" to "η", "theta" to "θ", "vartheta" to "θ", "iota" to "ι", "kappa" to "κ",
        "lambda" to "λ", "mu" to "μ", "nu" to "ν", "xi" to "ξ", "pi" to "π", "rho" to "ρ", "sigma" to "σ",
        "tau" to "τ", "upsilon" to "υ", "phi" to "φ", "varphi" to "φ", "chi" to "χ", "psi" to "ψ", "omega" to "ω",
        "Gamma" to "Γ", "Delta" to "Δ", "Theta" to "Θ", "Lambda" to "Λ", "Xi" to "Ξ", "Pi" to "Π",
        "Sigma" to "Σ", "Phi" to "Φ", "Psi" to "Ψ", "Omega" to "Ω",
        "infty" to "∞", "partial" to "∂", "nabla" to "∇", "cdot" to "·", "times" to "×", "div" to "÷",
        "pm" to "±", "mp" to "∓", "le" to "≤", "leq" to "≤", "ge" to "≥", "geq" to "≥", "ne" to "≠", "neq" to "≠",
        "approx" to "≈", "to" to "→", "rightarrow" to "→", "mapsto" to "↦", "sim" to "∼", "in" to "∈",
        "ldots" to "…", "dots" to "…", "cdots" to "⋯", "mid" to "|", "vert" to "|", "lfloor" to "⌊", "rfloor" to "⌋",
        "lceil" to "⌈", "rceil" to "⌉", "langle" to "⟨", "rangle" to "⟩", "{" to "{", "}" to "}", "angle" to "∠",
        "Re" to "ℜ", "Im" to "ℑ", "hbar" to "ℏ", "aleph" to "ℵ", "beth" to "ℶ", "gimel" to "ℷ", "daleth" to "ℸ", "varrho" to "ϱ", "varsigma" to "ς", "varpi" to "ϖ", "ell" to "ℓ", "circ" to "∘", "otimes" to "⊗", "oplus" to "⊕", "prime" to "′",
    )
    /** Upright function names: \sin → sin. */
    private val WORDS = setOf(
        "sin", "cos", "tan", "sec", "csc", "cot", "sinh", "cosh", "tanh", "arcsin", "arccos", "arctan",
        "ln", "log", "exp", "det", "min", "max", "gcd", "lcm", "arg", "deg", "dim", "ker", "Pr", "sgn", "tr", "rk",
    )
    private const val THIN = Formatter.THIN_SPACE

    fun parse(latex: String): MathRow = Reader(latex).row(stopAt = null)

    /** Formulas written over several lines (separated by newlines), one row per line. */
    fun lines(latex: String): List<MathRow> = latex.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.map { parse(it) }

    /** The LaTeX commands in [latex] that aren't supported (for checking formulas). */
    fun unknownCommands(latex: String): Set<String> = Reader(latex).also { it.row(stopAt = null) }.unknown

    private class Reader(val s: String) {
        var i = 0
        /** Commands this parser doesn't know (shown as their names). */
        val unknown = LinkedHashSet<String>()

        fun row(stopAt: Char?): MathRow {
            val out = MathRow()
            while (i < s.length) {
                val c = s[i]
                if (stopAt != null && c == stopAt) { i++; return out }
                if (c == '&' || (c == '\\' && s.startsWith("\\\\", i))) return out
                if (c == '\\' && s.startsWith("\\end", i)) return out
                atom(out)
            }
            return out
        }

        /** A group {…} or a single atom, as a row (for \frac arguments, scripts…). */
        fun argument(): MathRow {
            skipSpaces()
            if (i < s.length && s[i] == '{') { i++; return row('}') }
            val out = MathRow()
            if (i < s.length) atom(out, allowScripts = false)
            return out
        }

        fun skipSpaces() { while (i < s.length && s[i] == ' ') i++ }

        /** The whole name of the command starting at [k] (so \\left isn't mistaken for \\le), or null. */
        fun commandAt(k: Int): String? {
            if (k >= s.length || s[k] != '\\') return null
            var e = k + 1
            while (e < s.length && s[e].isLetter()) e++
            return s.substring(k + 1, e)
        }

        fun command(): String {
            i++ // backslash
            if (i < s.length && !s[i].isLetter()) return s[i++].toString()
            val start = i
            while (i < s.length && s[i].isLetter()) i++
            return s.substring(start, i)
        }

        /** Reads _{…} and ^{…} after something, in either order. */
        fun scripts(): Pair<MathRow?, MathRow?> {
            var sub: MathRow? = null
            var sup: MathRow? = null
            while (true) {
                skipSpaces()
                if (i < s.length && s[i] == '_' && sub == null) { i++; sub = argument(); continue }
                if (i < s.length && s[i] == '^' && sup == null) { i++; sup = argument(); continue }
                return sub to sup
            }
        }

        fun atom(out: MathRow, allowScripts: Boolean = true) {
            val c = s[i]
            when {
                c == ' ' -> { i++; return }
                c == '{' -> { i++; out.items.addAll(row('}').items.also { }); }
                c == '\\' -> commandAtom(out)
                c == '-' -> { i++; out.add(Sym("−")) }
                c == '*' -> { i++; out.add(Sym("·")) }
                // f' and f'': primes are raised, as in TeX (^{\prime}).
                c == '\'' -> {
                    var k = 0
                    while (i < s.length && s[i] == '\'') { k++; i++ }
                    out.add(Pow(MathRow(mutableListOf(Sym("′".repeat(k))))))
                    return
                }
                c == '^' || c == '_' -> { /* scripts on nothing: attach to an empty base */
                    val (sub, sup) = scripts()
                    out.add(Scripted(MathRow(), sub ?: MathRow(), sup ?: MathRow()))
                    return
                }
                else -> { i++; out.add(Sym(c.toString())) }
            }
            if (allowScripts) attachScripts(out)
        }

        /** x^2 becomes a power; anything with a subscript becomes a scripted node. */
        fun attachScripts(out: MathRow) {
            val (sub, sup) = scripts()
            if (sub == null && sup == null) return
            if (sub == null) { out.add(Pow(sup!!)); return }
            // After a closing bracket the base is the whole bracketed group: (A^T)_{ij}.
            val last = (out.items.lastOrNull() as? Sym)?.text
            val baseItems = if (last == ")" || last == "]") {
                var depth = 0
                var k = out.items.lastIndex
                while (k >= 0) {
                    when ((out.items[k] as? Sym)?.text) { ")", "]" -> depth++; "(", "[" -> depth-- }
                    if (depth == 0) break
                    k--
                }
                val start = k.coerceAtLeast(0)
                val group = out.items.subList(start, out.items.size).toMutableList()
                repeat(out.items.size - start) { out.items.removeAt(out.items.lastIndex) }
                group
            } else listOfNotNull(out.items.removeLastOrNull()).toMutableList()
            out.add(Scripted(MathRow(baseItems), sub, sup ?: MathRow()))
        }

        fun commandAtom(out: MathRow) {
            when (val name = command()) {
                "frac", "dfrac", "tfrac" -> out.add(Frac(argument(), argument()))
                "sqrt" -> {
                    skipSpaces()
                    if (i < s.length && s[i] == '[') {
                        i++
                        val index = row(']')
                        out.add(Root(index, argument()))
                    } else out.add(Sqrt(argument()))
                }
                "binom" -> out.add(Binom(argument(), argument()))
                // Accents on a letter (\hat{x}, \vec{v}, \dot{\theta}): the same symbols the symbol builder makes.
                "hat", "widehat", "dot", "ddot", "tilde", "widetilde", "bar", "overline", "vec", "check", "breve", "acute", "grave" -> {
                    val arg = argument()
                    val t = (arg.items.singleOrNull() as? Sym)?.text?.removePrefix(UPRIGHT)
                    val accent = when (name) {
                        "hat", "widehat" -> com.example.cas.cas.Accent.Hat
                        "dot" -> com.example.cas.cas.Accent.Dot
                        "ddot" -> com.example.cas.cas.Accent.DoubleDot
                        "tilde", "widetilde" -> com.example.cas.cas.Accent.Tilde
                        "bar", "overline" -> com.example.cas.cas.Accent.Bar
                        "vec" -> com.example.cas.cas.Accent.Vector
                        "check" -> com.example.cas.cas.Accent.Check
                        "breve" -> com.example.cas.cas.Accent.Breve
                        "acute" -> com.example.cas.cas.Accent.Acute
                        else -> com.example.cas.cas.Accent.Grave
                    }
                    if (t != null && t.isNotEmpty()) out.add(Sym(com.example.cas.cas.CustomSymbol(t, accent).encode()))
                    else out.items.addAll(arg.items)
                }
                "text", "mathrm", "operatorname", "textrm", "mathit" -> {
                    skipSpaces()
                    if (i < s.length && s[i] == '{') {
                        val end = s.indexOf('}', i)
                        val t = s.substring(i + 1, end)
                        // A single upright letter (the m of a unit, the n of m_n) is marked, since one-letter
                        // symbols are otherwise drawn italic; \mathit leaves it italic.
                        if (t.isNotEmpty()) out.add(Sym(if (t.length == 1 && t[0].isLetter() && name != "mathit") UPRIGHT + t else t))
                        i = end + 1
                    }
                    attachScripts(out)
                    if (name == "operatorname") operatorSpace(out)
                    return
                }
                "left", "right", "big", "Big", "bigg", "Bigg", "bigl", "bigr", "displaystyle", "limits" -> {
                    // Sizes come from the renderer; keep the delimiter that follows (unless it's ".").
                    skipSpaces()
                    if (i < s.length && s[i] == '.') i++
                    return
                }
                ",", ";", ":", "!", " " -> { out.add(Sym(THIN)); return }
                "quad" -> { out.add(Sym(THIN)); out.add(Sym(THIN)); out.add(Sym(THIN)); return }
                "qquad" -> { repeat(6) { out.add(Sym(THIN)) }; return }
                "int", "iint", "iiint" -> { integral(out, name); return }
                "oint" -> { contour(out); return }
                "sum", "prod" -> { bigOperator(out, if (name == "sum") BigOpKind.Sum else BigOpKind.Product); return }
                "lim" -> { limit(out); return }
                "begin" -> { environment(out); return }
                "bmod", "mod" -> out.add(Sym("mod"))
                // \mathcal{A} and \mathfrak{g}: the Unicode math letters.
                "mathcal", "mathfrak", "mathbb" -> {
                    skipSpaces()
                    val arg = if (i < s.length && s[i] == '{') { val end = s.indexOf('}', i); s.substring(i + 1, end).also { i = end + 1 } } else s[i++].toString()
                    for (c in arg) out.add(Sym(when {
                        !c.isLetter() -> c.toString()
                        name == "mathcal" -> com.example.cas.editor.MathAlphabets.calligraphic(c.uppercaseChar())
                        name == "mathbb" -> com.example.cas.editor.MathAlphabets.doubleStruck(c)
                        else -> com.example.cas.editor.MathAlphabets.fraktur(c)
                    }))
                }
                else -> {
                    if (name !in SYMBOLS && name !in WORDS && name.length > 1) unknown += name
                    // A lone backslash (\\ where a line break can't be) adds nothing.
                    (SYMBOLS[name] ?: name).takeIf { it.isNotEmpty() }?.let { out.add(Sym(it)) }
                    if (name in WORDS) { attachScripts(out); operatorSpace(out); return }
                }
            }
            attachScripts(out)
        }

        /** TeX's thin space after an operator name (det A, sin θ, Res f), but not before a bracket. */
        fun operatorSpace(out: MathRow) {
            skipSpaces()
            if (i >= s.length) return
            val c = s[i]
            if (c.isLetterOrDigit() || (c == '\\' && i + 1 < s.length && s[i + 1].isLetter() && !s.startsWith("\\left", i))) out.add(Sym(THIN))
        }

        /**
         * The rest of a term, for the body of Σ, Π and lim: up to a +, −, =, comma or relation
         * that isn't inside brackets, or the end of the group. Brackets are counted, so the
         * body of Σ f⁽ⁿ⁾(a)/n! (x − a)ⁿ keeps (x − a) whole, and lim (1 + 1/n)ⁿ keeps its bracket.
         */
        fun term(): MathRow {
            val out = MathRow()
            var depth = 0
            while (i < s.length) {
                val c = s[i]
                if (c == '}' || c == '&') break
                if (depth == 0) {
                    if (c == '+' || c == '-' || c == '=' || c == ',' || c == '<' || c == '>') break
                    if (s.startsWith("\\\\", i) || commandAt(i) in setOf("end", "quad", "qquad", "le", "leq", "ge", "geq", "approx", "to", "ne", "neq", "rightarrow")) break
                }
                // Count brackets, including \left( … \right) (whose delimiters follow the command).
                val next = when {
                    c == '(' || c == '[' -> 1
                    c == ')' || c == ']' -> -1
                    s.startsWith("\\left", i) -> 1
                    s.startsWith("\\right", i) -> -1
                    else -> 0
                }
                if (next < 0 && depth == 0) break
                depth += next
                if (s.startsWith("\\left", i) || s.startsWith("\\right", i)) {
                    // Keep the delimiter itself as an ordinary bracket.
                    i += if (s.startsWith("\\left", i)) 5 else 6
                    skipSpaces()
                    if (i < s.length && s[i] != '.') { out.items.add(Sym(s[i].toString())); i++ } else if (i < s.length) i++
                    if (next < 0) attachScripts(out)
                    continue
                }
                atom(out)
            }
            return out
        }

        /** ∫_a^b f dx: the body runs up to d followed by a letter (dx, \,dx or \mathrm{d}x). */
        fun integral(out: MathRow, name: String) {
            val (lo, hi) = scripts()
            val body = MathRow()
            var variable: MathRow? = null
            while (i < s.length) {
                skipSpaces()
                if (i >= s.length || s[i] == '}' || s[i] == '&') break
                if (s.startsWith("\\mathrm{d}", i) || (s[i] == 'd' && i + 1 < s.length && s[i + 1].isLetter() && (i == 0 || !s[i - 1].isLetter()))) {
                    i += if (s[i] == '\\') "\\mathrm{d}".length else 1
                    skipSpaces()
                    val v = MathRow()
                    if (i < s.length && s[i] == '\\') commandAtom(v) else if (i < s.length) { v.add(Sym(s[i].toString())); i++ }
                    variable = v
                    break
                }
                if (s.startsWith("\\,", i)) { i += 2; continue }
                atom(body)
            }
            // Drop a trailing thin space before dx.
            while (body.items.lastOrNull().let { it is Sym && it.text == THIN }) body.items.removeAt(body.items.lastIndex)
            val lower = lo ?: MathRow()
            val upper = hi ?: MathRow()
            val repeat = when (name) { "iint" -> 2; "iiint" -> 3; else -> 1 }
            var inner = Integral(lower, upper, body, variable ?: MathRow(mutableListOf(Sym("x"))))
            repeat(repeat - 1) { inner = Integral(MathRow(), MathRow(), MathRow(mutableListOf(inner)), MathRow(mutableListOf(Sym("x")))) }
            out.add(inner)
        }

        /** ∮_{circle} f dz. */
        fun contour(out: MathRow) {
            val (spec, _) = scripts()
            val body = MathRow()
            while (i < s.length) {
                skipSpaces()
                if (i >= s.length || s[i] == '}' || s[i] == '&') break
                if (s[i] == 'd' && i + 1 < s.length && s[i + 1].isLetter()) { i += 2; break }
                if (s.startsWith("\\mathrm{d}", i)) { i += "\\mathrm{d}".length + 1; break }
                if (s.startsWith("\\,", i)) { i += 2; continue }
                atom(body)
            }
            out.add(Func("contour", listOf(body, spec ?: MathRow())))
        }

        /** Σ_{k=a}^{b} term, as the calculator's own sum; Σ with other scripts just gets them. */
        fun bigOperator(out: MathRow, kind: BigOpKind) {
            val (lo, hi) = scripts()
            val eq = lo?.items?.indexOfFirst { it is Sym && it.text == "=" } ?: -1
            if (lo != null && eq > 0) {
                val variable = MathRow(lo.items.subList(0, eq).toMutableList())
                val lower = MathRow(lo.items.drop(eq + 1).toMutableList())
                skipSpaces()
                out.add(BigOp(kind, variable, lower, hi ?: MathRow(), term()))
            } else {
                out.add(Scripted(MathRow(mutableListOf(Sym(if (kind == BigOpKind.Sum) "Σ" else "Π"))), lo ?: MathRow(), hi ?: MathRow()))
            }
        }

        /** lim_{x→a} term. */
        fun limit(out: MathRow) {
            val (spec, _) = scripts()
            skipSpaces()
            out.add(Func("lim", listOf(term(), spec ?: MathRow())))
        }

        /** \begin{bmatrix} a & b \\ c & d \end{bmatrix} (pmatrix and matrix too). */
        fun environment(out: MathRow) {
            val name = argumentText()
            val rows = ArrayList<List<MathRow>>()
            var cells = ArrayList<MathRow>()
            while (i < s.length) {
                cells += row(stopAt = null)
                when {
                    i < s.length && s[i] == '&' -> i++
                    s.startsWith("\\\\", i) -> { i += 2; rows += cells; cells = ArrayList() }
                    s.startsWith("\\end", i) -> { i += 4; argumentText(); rows += cells; break }
                    else -> { rows += cells; break }
                }
            }
            val cols = rows.maxOf { it.size }
            val m = Matrix(rows.size, cols, rows.flatMap { r -> List(cols) { c -> r.getOrNull(c) ?: MathRow() } })
            if (name == "pmatrix") { out.add(Sym("(")); out.add(m); out.add(Sym(")")) } else out.add(m)
        }

        fun argumentText(): String {
            skipSpaces()
            if (i >= s.length || s[i] != '{') return ""
            val end = s.indexOf('}', i)
            return s.substring(i + 1, end).also { i = end + 1 }
        }
    }

    private fun MathRow.add(n: Node) { items.add(n) }

    /** Splits text with inline math \( … \) into (isMaths, piece) parts. */
    fun inline(text: String): List<Pair<Boolean, String>> {
        val out = ArrayList<Pair<Boolean, String>>()
        var k = 0
        while (k < text.length) {
            val open = text.indexOf("\\(", k)
            if (open < 0) { out += false to text.substring(k); break }
            if (open > k) out += false to text.substring(k, open)
            val close = text.indexOf("\\)", open + 2).let { if (it < 0) text.length else it }
            out += true to text.substring(open + 2, close)
            k = close + 2
        }
        return out.filter { it.second.isNotEmpty() }
    }
}
