package com.example.cas.engine

import com.example.cas.editor.Const
import com.example.cas.editor.Frac
import com.example.cas.editor.Func
import com.example.cas.editor.MathCodec
import com.example.cas.editor.MathRow
import com.example.cas.editor.Matrix
import com.example.cas.editor.Node
import com.example.cas.editor.Pow
import com.example.cas.editor.Sqrt
import com.example.cas.editor.Sym

/**
 * Math typed as plain text on the phone's keyboard, as in a spreadsheet or Python:
 * `e^(-x/6)*sin(2x)`, `x/2`, `sqrt(x)`, `|x|`, `[1, 4, 9]`, `mean(h)`, `pi`, `theta`. It becomes
 * the editor's own nodes (fractions, powers, roots, functions), so everything else reads it as if
 * it had been typed on the keypad; [text] writes nodes back as such text.
 */
object PlainMath {
    private val GREEK = mapOf(
        "pi" to "π", "theta" to "θ", "phi" to "φ", "alpha" to "α", "beta" to "β", "gamma" to "γ", "delta" to "δ", "epsilon" to "ε",
        "lambda" to "λ", "mu" to "μ", "sigma" to "σ", "tau" to "τ", "omega" to "ω", "rho" to "ρ", "inf" to "∞", "infinity" to "∞",
    )

    /** Names read as functions even without brackets (sin x). */
    private val FUNCTIONS = setOf(
        "sin", "cos", "tan", "sec", "csc", "cot", "asin", "acos", "atan", "arcsin", "arccos", "arctan", "sinh", "cosh", "tanh", "asinh", "acosh", "atanh",
        "ln", "log", "exp", "sqrt", "abs", "floor", "ceil", "round", "sign", "sgn",
    )
    private val RENAMED = mapOf("arcsin" to "asin", "arccos" to "acos", "arctan" to "atan", "sgn" to "sign")

    private sealed class Tok {
        data class Num(val s: String) : Tok()
        data class Name(val s: String) : Tok()
        data class Op(val c: Char) : Tok()
        data class Other(val s: String) : Tok()
    }

    private fun tokens(text: String): List<Tok> {
        val t = text.replace("**", "^").replace('·', '*').replace('×', '*').replace('÷', '/').replace('−', '-').replace('–', '-')
            .replace("<=", "≤").replace(">=", "≥").replace("!=", "≠")
        val out = ArrayList<Tok>()
        var k = 0
        while (k < t.length) {
            val c = t[k]
            when {
                c.isWhitespace() -> k++
                c.isDigit() || (c == '.' && k + 1 < t.length && t[k + 1].isDigit()) -> {
                    val st = k
                    while (k < t.length && (t[k].isDigit() || t[k] == '.')) k++
                    // A power of ten: 1.5e3, 2E-4.
                    if (k + 1 < t.length && (t[k] == 'e' || t[k] == 'E') && (t[k + 1].isDigit() || (t[k + 1] in "+-" && k + 2 < t.length && t[k + 2].isDigit()))) {
                        val mant = t.substring(st, k); k++
                        val es = k; if (t[k] in "+-") k++
                        while (k < t.length && t[k].isDigit()) k++
                        out += Tok.Num(mant); out += Tok.Op('*'); out += Tok.Num("10"); out += Tok.Op('^'); out += Tok.Num(t.substring(es, k))
                    } else out += Tok.Num(t.substring(st, k))
                }
                c.isLetter() -> {
                    val st = k
                    while (k < t.length && (t[k].isLetter() || t[k] == '_' || (t[k].isDigit() && k > st && t[k - 1] == '_'))) k++
                    out += Tok.Name(t.substring(st, k))
                }
                c == '√' -> { out += Tok.Name("sqrt"); k++ }
                c in "+-*/^()[],|" -> { out += Tok.Op(c); k++ }
                else -> { out += Tok.Other(c.toString()); k++ }
            }
        }
        return out
    }

    private class Reader(val toks: List<Tok>) {
        var k = 0
        /** Inside |…|: a bar then closes, rather than opening another. */
        var bars = 0
        fun peek(): Tok? = toks.getOrNull(k)
        fun isOp(c: Char) = (peek() as? Tok.Op)?.c == c
        fun take(): Tok? = toks.getOrNull(k)?.also { k++ }

        private fun row(nodes: List<Node>) = MathRow(nodes.toMutableList())
        private fun syms(s: String) = s.map { Sym(it.toString()) }

        /** Brackets around the whole thing dropped (a fraction's or power's own brackets). */
        private fun unwrap(n: List<Node>): List<Node> {
            if (n.size < 2 || (n.first() as? Sym)?.text != "(" || (n.last() as? Sym)?.text != ")") return n
            var depth = 0
            for (i in n.indices) {
                val t = (n[i] as? Sym)?.text
                if (t == "(") depth++ else if (t == ")") depth--
                if (depth == 0 && i < n.lastIndex) return n
            }
            return n.subList(1, n.size - 1)
        }

        /** Things split by commas and relations (=, <, …) at this level. */
        fun sequence(): List<Node> {
            val out = ArrayList<Node>(expr())
            while (true) {
                val p = peek()
                when {
                    p is Tok.Op && p.c == ',' -> { take(); out += Sym(","); out += expr() }
                    p is Tok.Other && p.s in setOf("=", "<", ">", "≤", "≥", "≠") -> { take(); out += Sym(p.s); out += expr() }
                    else -> return out
                }
            }
        }

        fun expr(): List<Node> {
            val out = ArrayList<Node>(term())
            while (isOp('+') || isOp('-')) {
                val op = (take() as Tok.Op).c
                out += Sym(if (op == '+') "+" else "−")
                out += term()
            }
            return out
        }

        private fun startsFactor(t: Tok?) = t is Tok.Num || t is Tok.Name || (t is Tok.Op && (t.c == '(' || (t.c == '|' && bars == 0)))

        fun term(): List<Node> {
            var cur: List<Node> = factor()
            while (true) {
                val p = peek()
                when {
                    p is Tok.Op && p.c == '*' -> { take(); cur = cur + Sym("×") + factor() }
                    p is Tok.Op && p.c == '/' -> { take(); val den = factor(); cur = listOf(Frac(row(unwrap(cur)), row(unwrap(den)))) }
                    // Side by side: 2x, x(x + 1), 3 sin x.
                    startsFactor(p) && !(p is Tok.Num && cur.lastOrNull().let { it is Sym && it.text.firstOrNull()?.isDigit() == true }) -> cur = cur + factor()
                    else -> return cur
                }
            }
        }

        fun factor(): List<Node> {
            if (isOp('-')) { take(); return listOf<Node>(Sym("−")) + factor() }
            if (isOp('+')) { take(); return factor() }
            val base = primary()
            if (isOp('^')) {
                take()
                val e = factor()
                return base + Pow(row(unwrap(e)))
            }
            return base
        }

        private fun args(): List<MathRow> {
            // After "(": arguments split by commas, up to ")".
            val out = ArrayList<MathRow>()
            if (isOp(')')) { take(); return out }
            while (true) {
                out += row(expr())
                if (isOp(',')) { take(); continue }
                if (isOp(')')) take()
                return out
            }
        }

        private fun function(name: String, args: List<MathRow>): List<Node> = when (name) {
            "sqrt" -> listOf(Sqrt(args.firstOrNull() ?: MathRow()))
            "exp" -> listOf(Sym("e"), Pow(args.firstOrNull() ?: MathRow()))
            "log" -> if (args.size >= 2) listOf(Func("log", listOf(args[1], args[0]))) else listOf(Func("log", args))
            else -> listOf(Func(RENAMED[name] ?: name, args.ifEmpty { listOf(MathRow()) }))
        }

        fun primary(): List<Node> {
            val t = take() ?: return emptyList()
            return when (t) {
                is Tok.Num -> syms(t.s)
                is Tok.Other -> listOf(Sym(t.s))
                is Tok.Op -> when (t.c) {
                    '(' -> { val inner = sequence(); if (isOp(')')) take(); listOf<Node>(Sym("(")) + inner + Sym(")") }
                    '[' -> { val inner = sequence(); if (isOp(']')) take(); listOf<Node>(Sym("[")) + inner + Sym("]") }
                    '|' -> { bars++; val inner = expr(); bars--; if (isOp('|')) take(); listOf(Func("abs", listOf(row(inner)))) }
                    else -> listOf(Sym(t.c.toString()))
                }
                is Tok.Name -> {
                    val n = t.s
                    val lower = n.lowercase()
                    when {
                        GREEK.containsKey(lower) -> listOf(Sym(GREEK.getValue(lower)))
                        // A function: with brackets (any name of two or more letters), or a known one without (sin x).
                        n.length > 1 && isOp('(') -> { take(); function(lower.takeIf { it in FUNCTIONS } ?: n, args()) }
                        lower in FUNCTIONS -> function(lower, listOf(row(factor())))
                        n.length == 1 -> listOf(Sym(n))
                        // Other names letter by letter, as the keypad types them (a column called "time").
                        else -> syms(n)
                    }
                }
            }
        }
    }

    /** [text] as a row of the editor's nodes (empty text, an empty row). */
    fun parse(text: String): MathRow {
        val r = Reader(tokens(text))
        val nodes = ArrayList<Node>()
        while (r.peek() != null) {
            nodes += r.sequence()
            // Something left that doesn't fit (a stray bracket): kept as it is, and on from there.
            when (val t = r.take()) {
                is Tok.Op -> nodes += Sym(t.c.toString())
                is Tok.Num -> nodes += Sym(t.s)
                is Tok.Name -> nodes += Sym(t.s)
                is Tok.Other -> nodes += Sym(t.s)
                null -> Unit
            }
        }
        return MathRow(nodes)
    }

    /** [row] written as plain text that [parse] reads back the same. */
    fun text(row: MathRow): String = buildString { row.items.forEach { append(node(it)) } }

    private fun atom(s: String) = s.length <= 1 || s.all { it.isLetterOrDigit() || it == '.' }
    private fun wrap(r: MathRow) = text(r).let { if (atom(it)) it else "($it)" }

    private fun node(n: Node): String = when (n) {
        is Sym -> when (n.text) { "×", "·" -> "*"; "−" -> "-"; "π" -> "pi"; "θ" -> "theta"; else -> n.text }
        is Frac -> wrap(n.num) + "/" + wrap(n.den)
        is Pow -> "^" + wrap(n.exp)
        is Sqrt -> "sqrt(" + text(n.arg) + ")"
        is Func -> if (n.name == "abs") "|" + text(n.args.firstOrNull() ?: MathRow()) + "|"
            else if (n.name == "log" && n.args.size == 2) "log(" + text(n.args[1]) + ", " + text(n.args[0]) + ")"
            else n.name + n.args.joinToString(", ", "(", ")") { text(it) }
        is Matrix -> (0 until n.usedRows).joinToString(", ", "[", "]") { i -> (0 until n.usedCols).joinToString(", ", "[", "]") { j -> text(n.cell(i, j)) } }
        is Const -> n.id
        else -> Formatter.plain(MathCodec.copyOf(listOf(n)))
    }
}
