package com.example.cas.graph

import com.example.cas.editor.Const
import com.example.cas.editor.Frac
import com.example.cas.editor.Func
import com.example.cas.editor.MathRow
import com.example.cas.editor.Node
import com.example.cas.editor.Pow
import com.example.cas.editor.Root
import com.example.cas.editor.Scripted
import com.example.cas.editor.Sqrt
import com.example.cas.editor.Sym
import com.example.cas.engine.Latex
import com.example.cas.engine.LatexParser

/**
 * Names of graph lines in the legend. A name is written like a matplotlib label: text, with
 * math in LaTeX between dollar signs ("Data", "$\sin x$", "Fit $a = 2$"). A line's default name
 * is what's typed, as math; a list of points is "Data".
 */
object Legend {
    /** The name a line has until it's renamed: its math, or "Data" for a list of points. */
    fun defaultSource(row: MathRow, isData: Boolean): String = when {
        isData -> "Data"
        row.items.isEmpty() -> ""
        else -> "$" + Latex.of(row) + "$"
    }

    /** The name split into (piece, is it math?); \$ is a dollar sign in the text. */
    fun segments(source: String): List<Pair<String, Boolean>> {
        val out = ArrayList<Pair<String, Boolean>>()
        val cur = StringBuilder()
        var math = false
        var k = 0
        while (k < source.length) {
            val c = source[k]
            if (c == '\\' && k + 1 < source.length && source[k + 1] == '$') { cur.append('$'); k += 2; continue }
            if (c == '$') {
                if (cur.isNotEmpty()) out += cur.toString() to math
                cur.clear(); math = !math; k++; continue
            }
            cur.append(c); k++
        }
        if (cur.isNotEmpty()) out += cur.toString() to math
        return out
    }

    /** The name as a row for drawing: text pieces as upright words, math as parsed. */
    fun row(source: String): MathRow {
        val out = MathRow()
        for ((piece, math) in segments(source)) {
            // Text is upright, even a single letter (which in math would be italic).
            if (!math) { out.add(Sym(if (piece.length == 1 && piece[0].isLetter()) LatexParser.UPRIGHT + piece else piece)); continue }
            val parsed = runCatching { LatexParser.parse(piece) }.getOrNull()
            if (parsed == null) { out.add(Sym(piece)); continue }
            parsed.items.toList().forEach { n -> parsed.items.remove(n); out.add(n) }
        }
        return out
    }

    /** The name as plain text, for screen readers. */
    fun plain(source: String): String = segments(source).joinToString("") { it.first }

    // ---- As runs of text, for exported graphs -------------------------------------------------

    private val SPACED = mapOf("=" to " = ", "+" to " + ", "−" to " − ", "-" to " − ", "<" to " < ", ">" to " > ", "≤" to " ≤ ", "≥" to " ≥ ", "×" to " × ", "·" to " · ", "," to ", ", ":=" to " := ", "→" to " → ")
    private val COMBINING = mapOf(
        com.example.cas.cas.Accent.Dot to "\u0307", com.example.cas.cas.Accent.DoubleDot to "\u0308", com.example.cas.cas.Accent.Hat to "\u0302",
        com.example.cas.cas.Accent.Tilde to "\u0303", com.example.cas.cas.Accent.Bar to "\u0304", com.example.cas.cas.Accent.Vector to "\u20D7",
        com.example.cas.cas.Accent.Check to "\u030C", com.example.cas.cas.Accent.Breve to "\u0306", com.example.cas.cas.Accent.Acute to "\u0301",
        com.example.cas.cas.Accent.Grave to "\u0300", com.example.cas.cas.Accent.Ring to "\u030A",
    )
    private val INVERSE = mapOf("asin" to "sin", "acos" to "cos", "atan" to "tan", "asinh" to "sinh", "acosh" to "cosh", "atanh" to "tanh")

    /**
     * A row as runs of Computer Modern: letters in math italic (upright capital Greek, as in
     * TeX), numbers, words and function names upright, exponents raised and indices lowered, and
     * fractions written a/b.
     */
    fun spans(row: MathRow): List<Scene.Span> {
        val out = ArrayList<Scene.Span>()
        fun add(text: String, italic: Boolean, shift: Int) {
            if (text.isEmpty()) return
            // The thin space only between two things: not first, nor after a space or bracket.
            if (text == "\u2009") {
                val before = out.lastOrNull()?.text?.lastOrNull()
                if (before == null || before == ' ' || before == '(' || before == '\u2009' || before == '|') return
            }
            val last = out.lastOrNull()
            if (last != null && last.italic == italic && last.shift == shift) out[out.lastIndex] = Scene.Span(last.text + text, italic, shift)
            else out += Scene.Span(text, italic, shift)
        }
        fun rowOf(r: MathRow, shift: Int) { r.items.forEach { node(it, shift, ::add) } }
        rowOf(row, 0)
        return out.ifEmpty { listOf(Scene.Span("", false)) }.let { spans ->
            // No space at the very start or end.
            spans.mapIndexed { i, sp ->
                val t = sp.text.let { if (i == 0) it.trimStart() else it }.let { if (i == spans.lastIndex) it.trimEnd() else it }
                Scene.Span(t, sp.italic, sp.shift)
            }
        }
    }

    private fun italicLetter(t: String) = t.length == 1 && (t[0] in 'a'..'z' || t[0] in 'A'..'Z' || t[0] in 'α'..'ω' || t[0] == 'ϕ' || t[0] == 'ϑ')

    private fun node(n: Node, shift: Int, add: (String, Boolean, Int) -> Unit) {
        // A function name after a number or letter gets a thin space, as TeX gives 2 sin x.
        if (n is Func && n.name != "abs" || n is Sym && n.text.length > 1 && n.text.all { it.isLetter() } && n.text !in SPACED) add("\u2009", false, shift)
        val up = (shift + 1).coerceAtMost(1)
        val down = (shift - 1).coerceAtLeast(-1)
        fun group(r: MathRow, s: Int, brackets: Boolean) {
            if (brackets) add("(", false, s)
            r.items.forEach { node(it, s, add) }
            if (brackets) add(")", false, s)
        }
        // One thing (with its exponents), or a plain number: no brackets needed around it.
        fun simple(r: MathRow): Boolean {
            val core = r.items.filter { it !is Pow }
            return core.size <= 1 && r.items.firstOrNull() !is Pow || r.items.all { it is Sym && (it.text.length == 1 && it.text[0].isDigit() || it.text == ".") }
        }
        when (n) {
            is Sym -> {
                val t = n.text
                when {
                    t.startsWith(LatexParser.UPRIGHT) -> add(t.removePrefix(LatexParser.UPRIGHT), false, shift)
                    t in SPACED -> add(if (shift == 0) SPACED.getValue(t) else t.trim().replace("-", "−"), false, shift)
                    italicLetter(t) -> add(t, true, shift)
                    else -> {
                        val custom = com.example.cas.cas.CustomSymbol.decode(t)
                        if (custom == null) add(t, false, shift) else {
                            // Its scripts raised and lowered, not written _1^2.
                            if (custom.preSup.isNotEmpty()) add(custom.preSup, false, up)
                            if (custom.preSub.isNotEmpty()) add(custom.preSub, false, down)
                            add(custom.base + (custom.accent?.let { COMBINING[it] } ?: ""), !custom.isUpright('u') && custom.base.length == 1, shift)
                            if (custom.sub.isNotEmpty()) add(custom.sub, false, down)
                            if (custom.sup.isNotEmpty()) add(custom.sup, false, up)
                        }
                    }
                }
            }
            is Const -> {
                val c = com.example.cas.engine.Constant.byId(n.id)
                if (c == null) add(n.id, false, shift)
                else c.pieces.forEach { p ->
                    add(p.text, p.italic, shift)
                    if (p.sub.isNotEmpty()) add(p.sub, false, down)
                    if (p.sup.isNotEmpty()) add(p.sup, false, up)
                }
            }
            is Pow -> group(n.exp, up, false)
            is Frac -> { group(n.num, shift, !simple(n.num)); add("/", false, shift); group(n.den, shift, !simple(n.den)) }
            is Sqrt -> { add("√", false, shift); group(n.arg, shift, true) }
            is Root -> { group(n.index, up, false); add("√", false, shift); group(n.arg, shift, true) }
            is Scripted -> { group(n.base, shift, false); group(n.sub, down, false); group(n.sup, up, false) }
            is Func -> {
                val args = n.args
                when {
                    n.name == "abs" -> { add("|", false, shift); group(args[0], shift, false); add("|", false, shift) }
                    n.name in INVERSE -> { add(INVERSE.getValue(n.name), false, shift); add("−1", false, up); group(args[0], shift, true) }
                    n.name == "log" && args.size == 2 -> { add("log", false, shift); group(args[0], down, false); group(args[1], shift, true) }
                    else -> {
                        add(n.name, false, shift)
                        add("(", false, shift)
                        args.forEachIndexed { i, a -> if (i > 0) add(", ", false, shift); group(a, shift, false) }
                        add(")", false, shift)
                    }
                }
            }
            // Anything bigger (sums, integrals, matrices): its LaTeX, upright.
            else -> add(Latex.of(MathRow(mutableListOf(n))).let { it }, false, shift)
        }
    }
}
