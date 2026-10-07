package com.example.cas.engine

import com.example.cas.cas.Add
import com.example.cas.cas.Eq
import com.example.cas.cas.Expr
import com.example.cas.cas.Flt
import com.example.cas.cas.Fn
import com.example.cas.cas.Mat
import com.example.cas.cas.MathError
import com.example.cas.cas.children
import com.example.cas.cas.Mul
import com.example.cas.cas.Num
import com.example.cas.cas.Numeric
import com.example.cas.cas.Pow
import com.example.cas.cas.Rel
import com.example.cas.cas.Printer
import com.example.cas.cas.Seq
import com.example.cas.cas.Sym
import com.example.cas.cas.freeVars
import com.example.cas.cas.contains
import com.example.cas.editor.Binom
import com.example.cas.editor.Const
import com.example.cas.editor.Frac
import com.example.cas.editor.Func
import com.example.cas.editor.MathCodec
import com.example.cas.editor.MathRow
import com.example.cas.editor.Matrix
import com.example.cas.editor.Node
import com.example.cas.editor.Root
import com.example.cas.editor.Sqrt
import com.example.cas.math.Rational
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import com.example.cas.editor.Pow as PowNode
import com.example.cas.editor.Sym as SymNode

/**
 * A result ready to show: the exact form first (like any CAS), and the
 * decimal approximation behind the "≈" chip when it says something different.
 */
data class Answer(val value: Expr, val exact: MathRow, val approx: MathRow?, val preferApprox: Boolean = false) {
    val text: String get() = Printer.plain(value)

    /** Solutions and assignments (x = 2, a = 5) already contain "=", so no extra "=" goes in front. */
    val isStatement: Boolean get() = value is Eq || value is Seq || value is com.example.cas.cas.Rel

    /** Came out as a decimal because there was no exact answer (numerical fallback); shown with ≈ instead of =. */
    val isApproximate: Boolean get() = value.contains { it is Flt }
}

/** Turns symbolic expressions into rows the math renderer draws: real fractions, roots, powers, matrices. */
object Formatter {
    const val THIN_SPACE = "\u2009"

    /** Show long numbers in groups of three (1 000 000); a setting. */
    @Volatile var groupDigits = true
    /** 0 auto, 1 scientific, 2 engineering; a setting. */
    @Volatile var numberFormat = 0
    /** Complex decimal answers as r·e^{iθ}; a setting. */
    @Volatile var polarComplex = false

    /**
     * Numbers with more digits than this are shown as a × 10ⁿ: whole numbers (exact ones too)
     * in every format, and decimals in the auto format. Fractions this long show their decimal
     * first. A setting (the slider in Settings › Numbers).
     */
    @Volatile var sciAfter: Int = 10

    /** Decimals in the a of a × 10ⁿ (7.361516 × 10¹⁸ with 6); a setting, 1 to 12. */
    @Volatile var sciDecimals: Int = 6

    /** Significant digits in decimal answers (a setting, 4 to 15). */
    @Volatile var significantDigits: Int = 10

    /** Numbers with more digits than this are shown in scientific notation (laying out more is slow). */
    const val MAX_EXACT_BITS = 3400 // about 1000 digits

    private fun huge(e: Expr): Boolean = when (e) {
        is Num -> e.q.num.bitLength() > MAX_EXACT_BITS || e.q.den.bitLength() > MAX_EXACT_BITS
        else -> e.children.any { huge(it) }
    }

    /** Replaces each huge number by m × 10ⁿ with a 10-digit decimal m (built without computing 10ⁿ). */
    private fun scientific(e: Expr): Expr = when {
        e is Num && (e.q.num.bitLength() > MAX_EXACT_BITS || e.q.den.bitLength() > MAX_EXACT_BITS) -> {
            val d = java.math.BigDecimal(e.q.num).divide(java.math.BigDecimal(e.q.den), java.math.MathContext(sciDecimals + 1, RoundingMode.HALF_UP))
            val exponent = d.precision() - d.scale() - 1
            val mantissa = d.movePointLeft(exponent).toDouble()
            com.example.cas.cas.Mul(listOf(Flt(mantissa), com.example.cas.cas.Pow(Num(10L), Num(exponent.toLong()))))
        }
        e is Num || e is Flt || e is Sym -> e
        e is com.example.cas.cas.Add -> com.example.cas.cas.Add(e.terms.map { scientific(it) })
        e is com.example.cas.cas.Mul -> com.example.cas.cas.Mul(e.factors.map { scientific(it) })
        e is com.example.cas.cas.Pow -> com.example.cas.cas.Pow(scientific(e.base), scientific(e.exp))
        e is Eq -> Eq(scientific(e.lhs), scientific(e.rhs))
        else -> e
    }

    fun answer(e: Expr): Answer {
        // A number too long to show in full: its scientific approximation instead.
        if (huge(e)) {
            val approx = scientific(e)
            return Answer(approx, row(approx), null)
        }
        val exact = row(e)
        // A decimal only helps when the answer is a number (or numbers): √8 ≈ 2.828, x = π/6 ≈ 0.5236.
        // For formulas like xeˣ − eˣ it would just turn e into 2.718….
        val approx = if (numeric(e)) runCatching { approxRow(e) }.getOrNull() else null
        val useful = approx != null && MathCodec.encode(approx) != MathCodec.encode(exact)
        return Answer(e, exact, if (useful) approx else null, preferApprox = useful && long(e))
    }

    /** Whether [e] has a fraction too long to read at a glance (12345678901/7): its decimal is shown first. */
    private fun long(e: Expr): Boolean = e.contains {
        it is Num && !it.q.isInteger && (it.q.num.abs().toString().length > sciAfter || it.q.den.toString().length > sciAfter)
    }

    /**
     * The separate values in an answer with several (x = −2, x = 2; eigenvalues; a system's
     * solutions), each as (what it is, the value to use): x = 2 gives (x = 2, 2). Empty when
     * there is only one.
     */
    fun choices(e: Expr): List<Pair<Expr, Expr>> {
        fun flat(x: Expr): List<Expr> = if (x is Seq) x.items.flatMap { flat(it) } else listOf(x)
        if (e !is Seq) return emptyList()
        val all = flat(e).map { x -> x to (if (x is Eq && x.lhs is Sym) x.rhs else x) }
        return if (all.size >= 2) all.distinctBy { Printer.plain(it.first) } else emptyList()
    }

    /** The row an answer shows: its decimal when that's preferred (or asked for), else the exact form. */
    fun shown(a: Answer, decimalFirst: Boolean): MathRow = if ((decimalFirst || a.preferApprox) && a.approx != null) a.approx else a.exact

    /** The decimal form; complex numbers as r·e^{iθ} when the polar setting is on. */
    private fun approxRow(e: Expr): MathRow {
        if (polarComplex && e.freeVars().isEmpty() && e !is Mat && e !is Eq && e !is com.example.cas.cas.Seq) {
            val c = Numeric.eval(e)
            if (c.im != 0.0 && c.re.isFinite() && c.im.isFinite()) {
                val out = MathRow()
                decimal(out, c.abs())
                out.add(SymNode("e"))
                val exp = MathRow()
                exp.add(SymNode("i"))
                decimal(exp, c.arg())
                out.add(PowNode(exp))
                return out
            }
        }
        return row(Numeric.approx(e))
    }

    private fun numeric(e: Expr): Boolean = when (e) {
        is Eq -> e.rhs.freeVars().isEmpty()
        is Seq -> e.items.all { numeric(it) }
        is com.example.cas.cas.Rel -> e.parts.all { p -> p.freeVars().size <= 1 } && e.parts.any { it.freeVars().isEmpty() }
        is Mat -> e.cells.all { it.freeVars().isEmpty() }
        else -> e.freeVars().isEmpty()
    }

    fun row(e: Expr): MathRow = MathRow().also { append(it, e) }

    private fun append(out: MathRow, e: Expr) {
        when (e) {
            is Num -> rational(out, e.q)
            is Flt -> decimal(out, e.d)
            is Sym -> out.add(SymNode(e.name))
            is Add -> Printer.sortedTerms(e).forEachIndexed { k, t ->
                val p = Printer.parts(t)
                if (p.negative) out.add(SymNode("−")) else if (k > 0) out.add(SymNode("+"))
                product(out, p)
            }
            is Mul, is Pow -> {
                if (e is Pow && !(e.exp is Num && e.exp.q.signum < 0)) power(out, e)
                else {
                    val p = Printer.parts(e)
                    if (p.negative) out.add(SymNode("−"))
                    product(out, p)
                }
            }
            is Fn -> if ((e.name == "sum" || e.name == "product") && e.args.size == 4) {
                // A held Σ or Π, drawn as it was typed.
                out.add(com.example.cas.editor.BigOp(
                    if (e.name == "sum") com.example.cas.editor.BigOpKind.Sum else com.example.cas.editor.BigOpKind.Product,
                    row(e.args[1]), row(e.args[2]), row(e.args[3]), row(e.args[0]),
                ))
            } else if (e.name == "contour" && e.args.size == 4) {
                // A held ∮: the circle |z − a| = r underneath, as typed.
                val circle = com.example.cas.cas.Eq(com.example.cas.cas.Fn("abs", listOf(com.example.cas.cas.sub(e.args[1], e.args[2]))), e.args[3])
                out.add(com.example.cas.editor.Func("contour", listOf(row(e.args[0]), row(circle))))
            } else if (e.name == "integral") {
                // A held integral shows as ∫ with its limits.
                out.add(com.example.cas.editor.Integral(row(e.args[2]), row(e.args[3]), row(e.args[0]), row(e.args[1])))
            } else function(out, e)
            is Mat -> out.add(Matrix(e.rows, e.cols, e.cells.map { row(it) }))
            is Eq -> { append(out, e.lhs); out.add(SymNode("=")); append(out, e.rhs) }
            is Seq -> e.items.forEachIndexed { k, item ->
                if (k > 0) {
                    if (e.joiner == ",") { out.add(SymNode(",")); out.add(SymNode(THIN_SPACE)); out.add(SymNode(THIN_SPACE)) }
                    else { out.add(SymNode(THIN_SPACE)); out.add(SymNode(e.joiner)); out.add(SymNode(THIN_SPACE)) }
                }
                // A solution set inside a list of alternatives is bracketed: (x = 1, y = 2) or (x = −2, y = −1).
                if (item is Seq && item.joiner == "," && e.joiner != ",") {
                    out.add(SymNode("(")); append(out, item); out.add(SymNode(")"))
                } else append(out, item)
            }
            is Rel -> e.parts.forEachIndexed { k, part ->
                append(out, part)
                e.ops.getOrNull(k)?.let { out.add(SymNode(it)) }
            }
        }
    }

    private fun product(out: MathRow, p: Printer.Parts) {
        val numerator = MathRow()
        val coefficientShown = when {
            p.coefficient != null && (p.coefficient.num != java.math.BigInteger.ONE || p.num.isEmpty()) -> {
                digits(numerator, p.coefficient.num.toString()); true
            }
            p.decimal != null && (p.decimal != 1.0 || p.num.isEmpty()) -> { decimal(numerator, p.decimal); true }
            p.num.isEmpty() -> { numerator.add(SymNode("1")); true }
            else -> false
        }
        // A lone sum needs brackets when a minus sign is written in front and it isn't in a
        // fraction: −(a + b), not −a + b.
        val signedAlone = p.negative && p.den.isEmpty() && (p.coefficient?.den ?: java.math.BigInteger.ONE) == java.math.BigInteger.ONE
        val many = p.num.size + (if (coefficientShown) 1 else 0) > 1 || signedAlone
        for (f in p.num) factor(numerator, f, many)
        val denominator = MathRow()
        val den = p.coefficient?.den?.takeIf { it != java.math.BigInteger.ONE }
        if (den != null) digits(denominator, den.toString())
        val manyDen = p.den.size + (if (den != null) 1 else 0) > 1
        for (f in p.den) factor(denominator, f, manyDen)
        if (denominator.isEmpty) {
            numerator.items.toList().forEach { numerator.items.remove(it); out.add(it) }
        } else {
            out.add(Frac(numerator, denominator))
        }
    }

    private fun factor(out: MathRow, f: Expr, inProduct: Boolean) {
        if (f is Add && inProduct) {
            out.add(SymNode("(")); append(out, f); out.add(SymNode(")"))
        } else append(out, f)
    }

    private fun power(out: MathRow, e: Pow) {
        val x = e.exp
        if (x is Num && x.q.num == java.math.BigInteger.ONE && !x.q.isInteger) {
            val q = x.q.den.toLong()
            if (q == 2L) { out.add(Sqrt(row(e.base))); return }
            out.add(Root(row(Num(Rational.of(q))), row(e.base))); return
        }
        val needsBrackets = when (val b = e.base) {
            is Add, is Mul, is Pow, is Eq, is Seq, is com.example.cas.cas.Rel -> true
            is Num -> b.q.signum < 0 || !b.q.isInteger
            is Flt -> b.d < 0
            else -> false
        }
        if (needsBrackets) { out.add(SymNode("(")); append(out, e.base); out.add(SymNode(")")) } else append(out, e.base)
        out.add(PowNode(row(x)))
    }

    private fun function(out: MathRow, f: Fn) {
        val args = f.args.map { row(it) }
        when (f.name) {
            "fact" -> {
                val a = f.args[0]
                if (a is Sym || (a is Num && a.q.isInteger && a.q.signum >= 0)) append(out, a)
                else { out.add(SymNode("(")); append(out, a); out.add(SymNode(")")) }
                out.add(SymNode("!"))
            }
            "binom" -> out.add(Binom(args[0], args[1]))
            "mod" -> { factor(out, f.args[0], true); out.add(SymNode("mod")); factor(out, f.args[1], true) }
            "transpose" -> { append(out, f.args[0]); out.add(PowNode(com.example.cas.editor.row("T"))) }
            "hermitian" -> { append(out, f.args[0]); out.add(PowNode(com.example.cas.editor.row("H"))) }
            else -> out.add(Func(f.name, args))
        }
    }

    private fun rational(out: MathRow, q: Rational) {
        if (q.signum < 0) out.add(SymNode("−"))
        val a = q.abs()
        if (a.isInteger) {
            val text = a.num.toString()
            // Too many digits to take in: 1.234567890 × 10¹⁵ (the full number is still copied).
            if (text.length > sciAfter) exponentForm(out, BigDecimal(a.num))
            else digits(out, text)
        }
        else out.add(Frac(MathRow().also { digits(it, a.num.toString()) }, MathRow().also { digits(it, a.den.toString()) }))
    }

    private fun decimal(out: MathRow, d: Double) {
        if (d == 0.0) { out.add(SymNode("0")); return }
        val bd = BigDecimal(d).round(MathContext(significantDigits, RoundingMode.HALF_EVEN)).stripTrailingZeros()
        val natural = bd.precision() - bd.scale() - 1
        // Auto: plain digits unless it has more whole digits than the setting, or is very small;
        // scientific and engineering: always a × 10ⁿ.
        val plain = when (numberFormat) {
            1 -> natural == 0
            2 -> natural in 0..2
            else -> natural in -5 until sciAfter
        }
        if (plain) digits(out, bd.toPlainString()) else exponentForm(out, BigDecimal(d))
    }

    /**
     * [bd] as a × 10ⁿ with [sciDecimals] decimals in a (trailing zeros dropped): 1 ≤ |a| < 10, or
     * in engineering n a multiple of 3 (so kilo, mega, milli… read off directly).
     */
    private fun exponentForm(out: MathRow, bd: BigDecimal) {
        fun exponentOf(v: BigDecimal): Int {
            val natural = v.precision() - v.scale() - 1
            return if (numberFormat == 2) Math.floorDiv(natural, 3) * 3 else natural
        }
        var exponent = exponentOf(bd)
        var mantissa = bd.movePointLeft(exponent).setScale(sciDecimals, RoundingMode.HALF_UP)
        // Rounding up can carry over: 9.9999996 → 10.000000, so 1.000000 × 10ⁿ⁺¹.
        val again = exponentOf(mantissa.movePointRight(exponent))
        if (again != exponent) { exponent = again; mantissa = bd.movePointLeft(exponent).setScale(sciDecimals, RoundingMode.HALF_UP) }
        mantissa = mantissa.stripTrailingZeros()
        if (bd.signum() < 0) out.add(SymNode("−"))
        digits(out, mantissa.abs().toPlainString())
        if (exponent == 0) return
        out.add(SymNode("×")); out.add(SymNode("1")); out.add(SymNode("0"))
        out.add(PowNode(com.example.cas.editor.row(exponent.toString().replace("-", "−"))))
    }

    /** Digits in groups of three with thin spaces: 1 380 953.123 45. */
    private fun digits(out: MathRow, text: String) {
        var t = text
        if (t.startsWith("-")) { out.add(SymNode("−")); t = t.substring(1) }
        val parts = t.split(".")
        val intPart = parts[0]
        intPart.forEachIndexed { k, ch ->
            if (groupDigits && k > 0 && (intPart.length - k) % 3 == 0 && intPart.length > 4) out.add(SymNode(THIN_SPACE))
            out.add(SymNode(ch.toString()))
        }
        if (parts.size > 1) {
            out.add(SymNode("."))
            val frac = parts[1]
            frac.forEachIndexed { k, ch ->
                if (groupDigits && k > 0 && k % 3 == 0 && frac.length > 4) out.add(SymNode(THIN_SPACE))
                out.add(SymNode(ch.toString()))
            }
        }
    }

    /** One-line text for copying. */
    fun plain(r: MathRow): String = r.items.joinToString("") { n -> plainNode(n) }

    private fun plainNode(n: Node): String = when (n) {
        is SymNode -> if (n.text == THIN_SPACE) "" else n.text
        is Frac -> "(${plain(n.num)})/(${plain(n.den)})"
        is PowNode -> "^" + plain(n.exp).let { if (it.length > 1) "($it)" else it }
        is Sqrt -> "√(${plain(n.arg)})"
        is Root -> "(${plain(n.arg)})^(1/${plain(n.index)})"
        is Func -> n.name + n.args.joinToString(",", "(", ")") { plain(it) }
        is Matrix -> (0 until n.usedRows).joinToString(", ", "[", "]") { i -> (0 until n.usedCols).joinToString(", ", "[", "]") { j -> plain(n.cell(i, j)) } }
        is Const -> n.id
        is Binom -> "binom(${plain(n.n)},${plain(n.k)})"
        else -> "…"
    }
}
