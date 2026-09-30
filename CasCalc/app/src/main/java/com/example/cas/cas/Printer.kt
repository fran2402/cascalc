package com.example.cas.cas

import com.example.cas.math.Rational
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Text forms of expressions.
 *  - [key]: a structural string used only to sort expressions canonically.
 *  - [plain]: readable one-line math in display order, e.g. "x^2+2x+1", "2√2", "(x+1)/2".
 * The display order rules here ([sortedTerms], [factorRank]) are shared with
 * the 2D formatter so both agree.
 */
object Printer {

    fun key(e: Expr): String = when (e) {
        is Num -> "#" + e.q
        is Flt -> "#" + e.d
        is Sym -> e.name
        is Add -> "+(" + e.terms.joinToString(",") { key(it) } + ")"
        is Mul -> "*(" + e.factors.joinToString(",") { key(it) } + ")"
        is Pow -> key(e.base) + "^" + key(e.exp)
        is Fn -> e.name + "(" + e.args.joinToString(",") { key(it) } + ")"
        is Mat -> "[" + e.cells.joinToString(",") { key(it) } + "]"
        is Eq -> key(e.lhs) + "=" + key(e.rhs)
        is Seq -> "{" + e.items.joinToString(e.joiner) { key(it) } + "}"
        is Rel -> e.parts.indices.joinToString("") { i -> key(e.parts[i]) + (e.ops.getOrNull(i) ?: "") }
    }

    // ---- Display order ------------------------------------------------------------

    /** Total degree in the variables, used to write polynomials highest power first. */
    private val INTEGRATION_CONSTANT = Regex("C[0-9]+")

    fun degree(t: Expr): Int = when (t) {
        is Sym -> if (t.name in CONSTANT_SYMBOLS || INTEGRATION_CONSTANT.matches(t.name)) 0 else 1
        is Pow -> if (t.base is Sym && t.base.name !in CONSTANT_SYMBOLS && t.exp is Num && t.exp.q.isInteger) t.exp.q.num.toInt() else if (t.isConstant) 0 else 1
        is Mul -> t.factors.sumOf { degree(it) }
        is Num, is Flt -> 0
        else -> if (t.isConstant) 0 else 1
    }

    /**
     * Polynomial order: highest total degree first, then lexicographic in the
     * variables (a³ − 3a²b + 3ab² − b³), plain numbers before other constants
     * (1 + √2) and any i-term last (11 − 2i).
     */
    fun sortedTerms(a: Add): List<Expr> {
        val vars = a.freeVars().sorted()
        return a.terms.sortedWith(
            compareBy<Expr>({ -degree(it) })
                .then { s, t ->
                    for (v in vars) {
                        val c = exponentOf(t, v).compareTo(exponentOf(s, v))
                        if (c != 0) return@then c
                    }
                    0
                }
                .thenBy { if (it.contains { e -> e == I }) 2 else if (it.isNumber) 0 else 1 }
                .thenBy { key(Simplify.splitCoefficient(it).second) },
        )
    }

    private fun exponentOf(t: Expr, v: String): Int {
        val fs = if (t is Mul) t.factors else listOf(t)
        for (f in fs) {
            if (f is Sym && f.name == v) return 1
            if (f is Pow && f.base is Sym && f.base.name == v && f.exp is Num && f.exp.q.isInteger) return f.exp.q.num.toInt()
        }
        return 0
    }

    fun factorRank(f: Expr): Int = when {
        f.isNumber -> 0
        // π before i (2πi), i before radicals (i√3).
        f == PI || f == E -> 1
        f == I -> 2
        f is Pow && f.base is Num -> 3
        f is Sym -> 4
        f is Pow && f.base is Sym && f.base !in listOf(PI, E, I) -> 4
        f is Fn -> 6
        f is Pow && (f.base == E || f.base == PI) -> 8
        else -> 7
    }

    /** Coefficient, π and e, i, radicals, variables, functions, then bracketed sums: 2πi, i√3, (x − 2)(x − 1)(x + 1). */
    fun sortedFactors(fs: List<Expr>): List<Expr> = fs.sortedWith(
        compareBy<Expr>({ factorRank(it) })
            .thenBy { degree((it as? Pow)?.base ?: it) }
            .thenBy { sumLead((it as? Pow)?.base ?: it) }
            .thenBy { sumConstant((it as? Pow)?.base ?: it) }
            .thenBy { key((it as? Pow)?.base ?: it) },
    )

    private fun sumLead(e: Expr): Double = if (e is Add) {
        val top = e.terms.maxBy { degree(it) }
        (Simplify.splitCoefficient(top).first as? Num)?.q?.toDouble() ?: 0.0
    } else 0.0

    private fun sumConstant(e: Expr): Double = if (e is Add) e.terms.filterIsInstance<Num>().sumOf { it.q.toDouble() } else 0.0

    /** Splits a product into (sign, numerator factors, denominator factors) for writing as a fraction. */
    class Parts(val negative: Boolean, val coefficient: Rational?, val decimal: Double?, val num: List<Expr>, val den: List<Expr>)

    fun parts(e: Expr): Parts {
        val factors = if (e is Mul) e.factors else listOf(e)
        var neg = false
        var q: Rational? = null
        var d: Double? = null
        val num = ArrayList<Expr>()
        val den = ArrayList<Expr>()
        for (f in factors) when {
            f is Num -> { q = f.q.abs(); neg = f.q.signum < 0 }
            f is Flt -> { d = kotlin.math.abs(f.d); neg = f.d < 0 }
            f is Pow && f.exp is Num && f.exp.q.signum < 0 -> den += pow(f.base, neg(f.exp))
            else -> num += f
        }
        return Parts(neg, q, d, sortedFactors(num), sortedFactors(den))
    }

    // ---- Plain text -------------------------------------------------------------------

    fun plain(e: Expr): String = when (e) {
        is Num -> rational(e.q)
        is Flt -> decimal(e.d)
        is Sym -> CustomSymbol.decode(e.name)?.plain ?: e.name
        is Add -> {
            val sb = StringBuilder()
            sortedTerms(e).forEachIndexed { i, t ->
                val p = parts(t)
                if (p.negative) sb.append("-") else if (i > 0) sb.append("+")
                sb.append(product(p))
            }
            sb.toString()
        }
        is Mul -> parts(e).let { (if (it.negative) "-" else "") + product(it) }
        is Pow -> power(e)
        is Fn -> if (e.name == "integral" || ((e.name == "sum" || e.name == "product") && e.args.size == 4)) {
            (if (e.name == "integral") "∫" else if (e.name == "sum") "Σ" else "Π") + "(" + e.args.joinToString(", ") { plain(it) } + ")"
        } else when (e.name) {
            "abs" -> "|" + plain(e.args[0]) + "|"
            "fact" -> wrap(e.args[0]) + "!"
            else -> e.name + "(" + e.args.joinToString(",") { plain(it) } + ")"
        }
        is Mat -> (0 until e.rows).joinToString(",", "[", "]") { r -> (0 until e.cols).joinToString(",", "[", "]") { c -> plain(e[r, c]) } }
        is Eq -> plain(e.lhs) + "=" + plain(e.rhs)
        is Seq -> e.items.joinToString(if (e.joiner == ",") ", " else " ${e.joiner} ") {
            if (it is Seq && it.joiner == "," && e.joiner != ",") "(" + plain(it) + ")" else plain(it)
        }
        is Rel -> e.parts.indices.joinToString("") { i -> plain(e.parts[i]) + (e.ops.getOrNull(i) ?: "") }
    }

    private fun product(p: Parts): String {
        val coefficientNum = p.coefficient?.num?.toString() ?: p.decimal?.let { decimal(it) }
        val coefficientDen = p.coefficient?.den?.takeIf { it.toString() != "1" }?.toString()
        val numText = buildString {
            if (coefficientNum != null && (coefficientNum != "1" || p.num.isEmpty())) append(coefficientNum)
            // −(a + b): a lone sum after a minus sign keeps its brackets (unless it's over a denominator).
            val signedAlone = p.negative && p.den.isEmpty() && coefficientDen == null
            p.num.forEach { append(wrapFactor(it, p.num.size + (if (isNotEmpty()) 1 else 0) > 1 || signedAlone)) }
            if (isEmpty()) append("1")
        }
        val denParts = listOfNotNull(coefficientDen) + p.den.map { if (it is Add) "(" + plain(it) + ")" else wrapFactor(it, true) }
        if (denParts.isEmpty()) return numText
        val den = denParts.joinToString("")
        val numWrapped = if (p.num.size == 1 && p.num[0] is Add && coefficientNum in listOf(null, "1")) "($numText)" else numText
        return "$numWrapped/" + if (denParts.size > 1) "($den)" else den
    }

    private fun wrapFactor(f: Expr, inProduct: Boolean): String = when {
        f is Add && inProduct -> "(" + plain(f) + ")"
        f is Add -> plain(f)
        else -> plain(f)
    }

    private fun wrap(f: Expr) = if (f is Sym || (f is Num && f.q.isInteger && f.q.signum >= 0)) plain(f) else "(" + plain(f) + ")"

    private fun power(e: Pow): String {
        val x = e.exp
        if (x is Num && x.q.signum < 0) return "1/" + pow(e.base, neg(x)).let { if (it is Add) "(" + plain(it) + ")" else plain(it) }
        if (x is Num && x.q.num == java.math.BigInteger.ONE) {
            val q = x.q.den.toInt()
            val root = when (q) { 2 -> "√"; 3 -> "∛"; 4 -> "∜"; else -> null }
            if (root != null) return root + (if (e.base is Sym || (e.base is Num && e.base.q.isInteger)) plain(e.base) else "(" + plain(e.base) + ")")
        }
        val base = if (e.base is Sym || e.base is Fn || (e.base is Num && e.base.q.isInteger && e.base.q.signum > 0)) plain(e.base) else "(" + plain(e.base) + ")"
        val exp = if (x is Sym || (x is Num && x.q.isInteger && x.q.signum >= 0)) plain(x) else "(" + plain(x) + ")"
        return "$base^$exp"
    }

    fun rational(q: Rational) = if (q.isInteger) q.num.toString() else "${q.num}/${q.den}"

    fun decimal(d: Double): String {
        if (d == 0.0) return "0"
        val bd = BigDecimal(d).round(MathContext(10, RoundingMode.HALF_EVEN)).stripTrailingZeros()
        val exp = bd.precision() - bd.scale() - 1
        return if (exp in -5..9) bd.toPlainString() else bd.round(MathContext(7)).toString().replace("E+", "e").replace("E", "e")
    }
}
