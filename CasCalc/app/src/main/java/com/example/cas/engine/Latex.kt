package com.example.cas.engine

import com.example.cas.editor.BigOp
import com.example.cas.editor.BigOpKind
import com.example.cas.editor.Binom
import com.example.cas.editor.Const
import com.example.cas.editor.Derivative
import com.example.cas.editor.Frac
import com.example.cas.editor.Func
import com.example.cas.editor.Integral
import com.example.cas.editor.MathRow
import com.example.cas.editor.Matrix
import com.example.cas.editor.Node
import com.example.cas.editor.Pow
import com.example.cas.editor.Root
import com.example.cas.editor.Sqrt
import com.example.cas.editor.Sym

/** LaTeX for any 2D row, input or answer, for sharing and copying. */
object Latex {
    private val SYMBOLS = mapOf(
        "×" to "\\times ", "÷" to "\\div ", "−" to "-", "π" to "\\pi ", "∞" to "\\infty ",
        "≤" to "\\le ", "≥" to "\\ge ", "→" to "\\to ", "′" to "'", "λ" to "\\lambda ", "θ" to "\\theta ",
        "α" to "\\alpha ", "ε" to "\\varepsilon ", "mod" to "\\bmod ", "or" to "\\quad\\text{or}\\quad ",
        ":=" to "\\coloneqq ", Formatter.THIN_SPACE to "\\,", "%" to "\\%", "ans" to "\\mathrm{Ans}",
        "ℜ" to "\\Re ", "ℑ" to "\\Im ", "∠" to "\\angle ",
    )
    private val TEXT_SYMBOLS = mapOf(
        "ℏ" to "\\hbar", "α" to "\\alpha", "ε" to "\\varepsilon", "μ" to "\\mu", "σ" to "\\sigma", "τ" to "\\tau",
        "θ" to "\\theta", "Λ" to "\\Lambda", "Φ" to "\\Phi", "Δν" to "\\Delta\\nu", "∞" to "\\infty", "sin" to "\\sin",
    )
    private val KNOWN = setOf("sin", "cos", "tan", "sinh", "cosh", "tanh", "ln", "det", "gcd", "min", "max", "arg")
    private val INVERSE = mapOf("asin" to "\\sin^{-1}", "acos" to "\\cos^{-1}", "atan" to "\\tan^{-1}", "asinh" to "\\sinh^{-1}", "acosh" to "\\cosh^{-1}", "atanh" to "\\tanh^{-1}")

    fun of(r: MathRow): String = r.items.joinToString("") { node(it) }.trim()

    private fun group(r: MathRow) = "{" + of(r) + "}"
    private fun paren(s: String) = "\\left($s\\right)"

    private fun node(n: Node): String = when (n) {
        is Sym -> SYMBOLS[n.text] ?: com.example.cas.cas.CustomSymbol.decode(n.text)?.latex ?: com.example.cas.editor.MathAlphabets.decode(n.text.codePointAt(0))?.takeIf { com.example.cas.editor.MathAlphabets.isMathLetter(n.text) }?.let { (style, c) ->
            com.example.cas.editor.MathAlphabets.latex(style, c)
        } ?: when {
            n.text.length > 1 && Regex("[A-Z][0-9]+").matches(n.text) -> n.text[0] + "_{" + n.text.drop(1) + "}"
            n.text.length > 1 && n.text[0].isLetter() && n.text.drop(1).all { it == '′' } -> n.text[0] + "'".repeat(n.text.length - 1)
            n.text.length > 1 && n.text.all { it.isLetter() } -> "\\operatorname{${n.text}}"
            else -> n.text
        }
        is Const -> constantPieces(n.id)?.joinToString("") { p ->
            val text = TEXT_SYMBOLS[p.text] ?: if (p.text.length > 1 && !p.text.all { it.isLetter() }) "\\mathrm{${p.text}}" else if (p.text.length > 1) "\\mathrm{${p.text}}" else p.text
            text + (if (p.sub.isNotEmpty()) "_{" + (TEXT_SYMBOLS[p.sub] ?: "\\mathrm{${p.sub}}") + "}" else "") +
                (if (p.sup.isNotEmpty()) "^{" + p.sup.replace("−", "-") + "}" else "")
        } ?: n.id
        is Frac -> "\\frac" + group(n.num) + group(n.den)
        is Pow -> "^" + group(n.exp)
        is Sqrt -> "\\sqrt" + group(n.arg)
        is Root -> "\\sqrt[" + of(n.index) + "]" + group(n.arg)
        is Func -> function(n)
        is BigOp -> (if (n.kind == BigOpKind.Sum) "\\sum" else "\\prod") +
            "_{" + of(n.variable) + "=" + of(n.lower) + "}^" + group(n.upper) + " " + of(n.body)
        is Integral -> "\\int" + (if (n.lower.isEmpty && n.upper.isEmpty) "" else "_" + group(n.lower) + "^" + group(n.upper)) +
            " " + of(n.body) + "\\,\\mathrm{d}" + of(n.variable)
        is Derivative -> {
            val order = if (n.order.isEmpty) "" else "^" + group(n.order)
            val d = if (n.partial) "\\partial" else "\\mathrm{d}"
            "\\frac{$d$order}{$d ${of(n.variable)}$order}" + paren(of(n.body)) +
                (if (n.at.isEmpty) "" else "\\Big|_{${of(n.variable)}=${of(n.at)}}")
        }
        is Binom -> "\\binom" + group(n.n) + group(n.k)
        is com.example.cas.editor.Scripted -> group(n.base) + (if (n.sub.isEmpty) "" else "_" + group(n.sub)) + (if (n.sup.isEmpty) "" else "^" + group(n.sup))
        is Matrix -> "\\begin{bmatrix}" + (0 until n.usedRows).joinToString(" \\\\ ") { r -> (0 until n.usedCols).joinToString(" & ") { c -> of(n.cell(r, c)) } } + "\\end{bmatrix}"
    }

    private fun function(f: Func): String {
        val a = f.args.map { of(it) }
        return when (f.name) {
            "abs" -> "\\left|" + a[0] + "\\right|"
            "floor" -> "\\left\\lfloor " + a[0] + "\\right\\rfloor"
            "ceil" -> "\\left\\lceil " + a[0] + "\\right\\rceil"
            "round" -> "\\left\\lfloor " + a[0] + "\\right\\rceil"
            "frac" -> "\\left\\{" + a[0] + "\\right\\}"
            "sgn" -> "\\operatorname{sgn}" + paren(a[0])
            "gamma" -> "\\Gamma" + paren(a[0])
            "contour" -> "\\oint_{" + a[1] + "} " + a[0] + "\\,\\mathrm{d}z"
            "residue" -> "\\operatorname{Res}_{" + a[1] + "}" + paren(a[0])
            "perm" -> "P" + paren(a.joinToString(", "))
            "normpdf" -> "\\varphi" + paren(a.joinToString(", "))
            "normcdf" -> "\\Phi" + paren(a.joinToString(", "))
            "invnorm" -> "\\Phi^{-1}" + paren(a[0])
            "var" -> "s^{2}" + paren(a[0])
            "sd" -> "s" + paren(a[0])
            "psd" -> "\\sigma" + paren(a[0])
            "mean" -> "\\bar{x}" + paren(a[0])
            "sum", "product" -> if (a.size == 4) {
                (if (f.name == "sum") "\\sum" else "\\prod") + "_{" + a[1] + " = " + a[2] + "}^{" + a[3] + "} " + a[0]
            } else "\\mathrm{${f.name}}" + paren(a[0])
            "digamma" -> "\\psi" + paren(a[0])
            "zetaprime" -> "\\zeta'" + paren(a[0])
            "grad" -> (if (f.args.size > 1 && !f.args[1].isEmpty) "\\nabla^{" + a[1] + "}" else "\\nabla ") + paren(a[0])
            "div" -> "\\nabla\\cdot " + paren(a[0])
            "curl" -> "\\nabla\\times " + paren(a[0])
            "laplacian" -> "\\nabla^{2}" + paren(a[0])
            "jacobian" -> "J" + paren(a[0])
            "hessian" -> "H" + paren(a[0])
            "zeta" -> "\\zeta" + paren(a[0])
            "hurwitz" -> "\\zeta(" + a[0] + ", " + a[1] + ")"
            "si", "ci", "shi", "chi", "ei", "li", "erfi" -> "\\operatorname{" + mapOf("si" to "Si", "ci" to "Ci", "shi" to "Shi", "chi" to "Chi", "ei" to "Ei", "li" to "li", "erfi" to "erfi")[f.name] + "}" + paren(a[0])
            "fresnels" -> "S" + paren(a[0])
            "fresnelc" -> "C" + paren(a[0])
            "gammainc" -> "\\Gamma(" + a[0] + ", " + a[1] + ")"
            "ellipticf" -> "F(" + a[0] + " \\mid " + a[1] + ")"
            "elliptice" -> "E(" + a[0] + " \\mid " + a[1] + ")"
            "polylog" -> "\\operatorname{Li}_{" + a[0] + "}" + paren(a[1])
            in NOTATION_NAMES -> FunctionNotation.latex(f.name, a) ?: ("\\operatorname{${f.name}}" + paren(a.joinToString(", ")))
            "besselj" -> "J_{" + a[0] + "}" + paren(a[1])
            "bessely" -> "Y_{" + a[0] + "}" + paren(a[1])
            "log" -> "\\log_" + group(f.args[0]) + paren(a[1])
            "lim" -> "\\lim_{" + a[1] + "}" + paren(a[0])
            "binom" -> "\\binom{${a[0]}}{${a[1]}}"
            in INVERSE -> INVERSE.getValue(f.name) + paren(a[0])
            in KNOWN -> "\\" + f.name + paren(a.joinToString(", "))
            else -> "\\operatorname{${f.name}}" + paren(a.joinToString(", "))
        }
    }
}

/** Functions written as on their keys (see [FunctionNotation]). */
private val NOTATION_NAMES = FunctionNotation.specs.keys + setOf("subfactorial", "primorial", "rising", "falling")
