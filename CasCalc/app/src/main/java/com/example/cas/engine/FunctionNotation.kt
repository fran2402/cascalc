package com.example.cas.engine

/**
 * How the combinatorics and polynomial functions are written, as on their keys: a letter with the
 * index as a subscript (and an order as a superscript), then the variable in brackets. Lah numbers
 * are L(n, k), Legendre's associated functions Pₙᵐ(x), Hermite's Hₙ(x) and so on. Used by the math
 * view and by LaTeX, so the editor, the answers and exports all agree.
 */
object FunctionNotation {
    /**
     * [base] the letter(s); [sub] and [sup] the arguments written as scripts (by position); the
     * rest go in brackets after it ([args]). [italic] for Latin letters, upright for Φ and sf.
     */
    class Spec(val base: String, val italic: Boolean, val sub: Int? = null, val sup: Int? = null, val args: List<Int>, val latex: String = base) {
        val arity get() = listOfNotNull(sub, sup).size + args.size
    }

    private fun call(base: String, n: Int, latex: String = base) = Spec(base, true, args = (0 until n).toList(), latex = latex)
    private fun seq(base: String) = Spec(base, true, sub = 0, args = emptyList())
    private fun poly(base: String, latex: String = base) = Spec(base, true, sub = 0, args = listOf(1), latex = latex)
    private fun poly2(base: String) = Spec(base, true, sub = 0, sup = 1, args = listOf(2))

    val specs: Map<String, Spec> = mapOf(
        // Combinatorics.
        "stirling1" to call("s", 2), "stirling2" to call("S", 2), "bell" to call("B", 1),
        "narayana" to call("N", 2), "lah" to call("L", 2), "eulerian" to call("A", 2),
        "superfactorial" to Spec("sf", false, args = listOf(0), latex = "\\operatorname{sf}"),
        "harmonic" to seq("H"), "triangular" to seq("T"), "motzkin" to seq("M"), "pell" to seq("P"),
        // Polynomials.
        "legendre" to poly("P"), "hermite" to poly("H"), "hermitehe" to poly("He", "\\mathit{He}"), "laguerre" to poly("L"),
        "genlaguerre" to poly2("L"), "chebyshevt" to poly("T"), "chebyshevu" to poly("U"), "gegenbauer" to poly2("C"),
        "assoclegendre" to poly2("P"), "bernoullipoly" to poly("B"), "fibpoly" to poly("F"), "lucaspoly" to poly("L"),
        "besselpoly" to poly("y"), "touchard" to poly("T"),
        "cyclotomic" to Spec("Φ", false, sub = 0, args = listOf(1), latex = "\\Phi"),
    )

    /** LaTeX for a call written this way, from its arguments' LaTeX; null when the name isn't one of these. */
    fun latex(name: String, a: List<String>): String? {
        val s = specs[name] ?: return when (name) {
            "subfactorial" -> a.singleOrNull()?.let { "{!}" + wrap(it) }
            "primorial" -> a.singleOrNull()?.let { wrap(it) + "\\#" }
            "rising" -> if (a.size == 2) wrap(a[0]) + "^{(" + a[1] + ")}" else null
            "falling" -> if (a.size == 2) "\\left(" + a[0] + "\\right)_{" + a[1] + "}" else null
            else -> null
        }
        if (a.size != s.arity) return null
        val sub = s.sub?.let { "_{" + a[it] + "}" } ?: ""
        val sup = s.sup?.let { "^{" + a[it] + "}" } ?: ""
        val args = if (s.args.isEmpty()) "" else "\\left(" + s.args.joinToString(", ") { a[it] } + "\\right)"
        return s.latex + sub + sup + args
    }

    /** A single letter or number goes as it is; anything longer in brackets. */
    private fun wrap(t: String) = if (t.length == 1 || t.all { it.isDigit() }) t else "\\left($t\\right)"
}
