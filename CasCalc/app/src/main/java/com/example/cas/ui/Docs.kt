package com.example.cas.ui

import com.example.cas.editor.BigOp
import com.example.cas.editor.BigOpKind
import com.example.cas.editor.Binom
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

/**
 * The documentation in Settings: chapters of sections, each a run of paragraphs, notes,
 * formulas and tables of worked examples. A calculator example is the row exactly as the keys
 * would type it; what it gives ([DocsResults]) is worked out by the calculator itself, and a
 * test keeps the two in step.
 */
object Docs {
    /** A worked example: what's typed, and a line about it. Its answer is in [DocsResults]. */
    class Example(val row: MathRow, val note: String = "", val degrees: Boolean = false)

    sealed class Block {
        /** Text with inline math between $ signs. */
        class Para(val text: String) : Block()
        /** A highlighted aside: a tip, or something to watch for. */
        class Note(val text: String, val warning: Boolean = false) : Block()
        /** A displayed formula, in LaTeX. */
        class Formula(val latex: String) : Block()
        /** A list of short points. */
        class Bullets(val items: List<String>) : Block()
        /** Worked examples: typed → answer, with notes. */
        class Examples(val examples: List<Example>) : Block()
        /** Something to do and what it does, as a two-column table (gestures, keys). */
        class Table(val rows: List<Pair<String, String>>) : Block()
        /** The key reference: every key in these groups, with its formula and how to use it. */
        class Keys(val groups: List<String>) : Block()
        /** The searchable list of spreadsheet functions. */
        object SheetFunctions : Block()
    }

    class Section(val title: String, val blocks: List<Block>)

    class Chapter(val title: String, val icon: String, val summary: String, val sections: List<Section>, val part: String)

    // ---------- building rows as the keys type them ----------

    /** Text is one symbol per character; nodes go in as they are. */
    private fun m(vararg parts: Any): MathRow = MathRow(
        parts.flatMap { p ->
            when (p) {
                is String -> p.map { Sym(it.toString()) }
                is Node -> listOf(p)
                is MathRow -> p.items
                else -> error("not math: $p")
            }
        }.toMutableList(),
    )
    private fun f(name: String, vararg args: Any) = Func(name, args.map { if (it is MathRow) it else m(it) })
    private fun fr(num: Any, den: Any) = Frac(m(num), m(den))
    private fun p(e: Any) = Pow(m(e))
    private fun sq(x: Any) = Sqrt(m(x))
    private fun rt(n: Any, x: Any) = Root(m(n), m(x))
    private fun int(body: Any, v: String = "x", lo: Any? = null, hi: Any? = null) = Integral(lo?.let { m(it) } ?: MathRow(), hi?.let { m(it) } ?: MathRow(), m(body), m(v))
    private fun d(body: Any, v: String = "x", order: String = "", at: Any? = null, partial: Boolean = false) =
        Derivative(m(v), m(body), at?.let { m(it) } ?: MathRow(), if (order.isEmpty()) MathRow() else m(order), partial)
    private fun sum(v: String, lo: Any, hi: Any, body: Any) = BigOp(BigOpKind.Sum, m(v), m(lo), m(hi), m(body))
    private fun prod(v: String, lo: Any, hi: Any, body: Any) = BigOp(BigOpKind.Product, m(v), m(lo), m(hi), m(body))
    private fun lim(body: Any, to: Any) = Func("lim", listOf(m(body), m(to)))
    private fun mat(rows: Int, cols: Int, vararg cells: Any) = Matrix(rows, cols, cells.map { m(it) })
    private fun vec(vararg cells: Any) = mat(cells.size, 1, *cells)
    private fun bin(n: Any, k: Any) = Binom(m(n), m(k))

    private fun ex(vararg parts: Any, note: String = "", degrees: Boolean = false) = Example(m(*parts), note, degrees)
    private fun para(t: String) = Block.Para(t)
    private fun note(t: String) = Block.Note(t)
    private fun warn(t: String) = Block.Note(t, warning = true)
    private fun formula(t: String) = Block.Formula(t)
    private fun bullets(vararg t: String) = Block.Bullets(t.toList())
    private fun examples(vararg e: Example) = Block.Examples(e.toList())
    private fun table(vararg r: Pair<String, String>) = Block.Table(r.toList())
    private fun section(title: String, vararg blocks: Block) = Section(title, blocks.toList())

    private const val CALC = "The calculator"
    private const val APP = "Graphs and tools"
    private const val REF = "Reference"

    val chapters: List<Chapter> = listOf(
        Chapter(
            "Getting started", "start", "Typing math, exact answers and the decimal, and where results go.", part = CALC, sections = listOf(
                section(
                    "Typing math",
                    para("The keys type math the way it's written on paper. ÷ starts a fraction: what you type next goes on top, and → moves down to the bottom. A power key raises the cursor into an exponent; → brings it back down. Roots, integrals, sums and matrices are boxes to fill, and the cursor moves through them with the arrow keys or a tap."),
                    para("Press ↵ (or =) to work out the answer. It's added to the history above, and what you typed stays so you can change it."),
                    examples(
                        ex(fr("1", "3"), "+", fr("1", "6"), note = "Fractions stay exact"),
                        ex("2", p("10"), note = "A power"),
                        ex(sq("72"), note = "Roots are simplified"),
                    ),
                ),
                section(
                    "Exact answers and decimals",
                    para("Answers are exact whenever they can be: fractions stay fractions, roots stay roots and \$\\pi\$ stays \$\\pi\$. When there's a decimal too, the ≈ chip beside the answer shows it; tap exact to go back. Settings › Numbers can show decimals first, and sets how many significant digits they keep."),
                    examples(
                        ex(sq("2"), sq("8"), note = "\$\\sqrt{2}\\sqrt{8}\$ simplifies to a whole number"),
                        ex("2π+", fr("π", "2"), note = "\$\\pi\$ stays a symbol"),
                    ),
                    note("Very long numbers switch to \$a \\times 10^n\$ past the number of digits set in Settings › Numbers."),
                ),
                section(
                    "Reusing results",
                    bullets(
                        "Tap a past question or answer to put it at the cursor.",
                        "Hold one to copy it as text.",
                        "Ans is the last answer; an operator pressed straight after = starts from it.",
                        "\$a := 3\$ stores a value in a letter; \$f(x) = x^2 + 1\$ defines a function.",
                    ),
                    examples(
                        ex("a", Sym(":="), "3", note = "Stores \$a\$ for later lines"),
                        ex("f(x)=x", p("2"), "+1", note = "Defines \$f\$; then \$f(2)\$ gives \$5\$"),
                    ),
                ),
            ),
        ),
        Chapter(
            "Arithmetic and numbers", "numbers", "Fractions, powers, roots, factorials and scientific notation.", part = CALC, sections = listOf(
                section(
                    "Fractions and powers",
                    examples(
                        ex(fr("2", "3"), "×", fr("9", "4")),
                        ex(fr(fr("1", "2"), fr("3", "4")), note = "A fraction of fractions"),
                        ex("2", p("−3"), note = "Negative powers are reciprocals"),
                        ex("8", p(fr("2", "3")), note = "Fractional powers are roots"),
                        ex("(−2)", p("5")),
                        ex("0.125+", fr("3", "8"), note = "Decimals become exact fractions when they can"),
                    ),
                ),
                section(
                    "Roots",
                    examples(
                        ex(sq("72")),
                        ex(rt("3", "−27"), note = "Odd roots of negatives are real"),
                        ex(rt("4", "16")),
                        ex(fr("1", sq("2")), note = "Denominators are made rational"),
                        ex(sq("12"), "+", sq("27")),
                        ex(sq("−16"), note = "Square roots of negatives are imaginary"),
                    ),
                ),
                section(
                    "Whole numbers",
                    examples(
                        ex("20!", note = "Factorials"),
                        ex(bin("10", "3"), note = "\$n\$ choose \$k\$"),
                        ex(f("perm", "10", "3"), note = "Arrangements: \$\\frac{n!}{(n-k)!}\$"),
                        ex("17", Sym("mod"), "5", note = "The remainder"),
                    ),
                ),
                section(
                    "Absolute value and percent",
                    examples(
                        ex(f("abs", "−7")),
                        ex("25%", note = "Percent is a hundredth"),
                    ),
                ),
                section(
                    "Scientific notation",
                    para("× 10ⁿ types a power of ten to fill in. Results that are very large or very small show this way too."),
                    examples(
                        ex("6.02×10", p("23"), "×2"),
                        ex("1.6×10", p("−19"), "×6.24×10", p("18")),
                    ),
                ),
            ),
        ),
        Chapter(
            "Powers, exponentials and logs", "exp", "Exact exponentials and logarithms, real and complex.", part = CALC, sections = listOf(
                section(
                    "Exponentials",
                    formula("e^x = \\sum_{n=0}^{\\infty} \\frac{x^n}{n!}"),
                    examples(
                        ex("e", p("0")),
                        ex("e", p(m(f("ln", "3"))), note = "Exponentials undo logs"),
                        ex("e", p("iπ"), note = "Euler's identity, exactly"),
                        ex("x", p("2"), "x", p("3"), note = "Powers with the same base combine"),
                    ),
                ),
                section(
                    "Logarithms",
                    para("ln is the natural logarithm; log takes its base in the small box."),
                    examples(
                        ex(f("ln", m("e", p("3")))),
                        ex(f("log", "2", "1024")),
                        ex(f("log", "10", "0.001")),
                        ex(f("ln", "−1"), note = "Logs of negatives are complex"),
                        ex(f("log", "3", "81")),
                    ),
                ),
            ),
        ),
        Chapter(
            "Trigonometry", "trig", "Exact values, inverses, hyperbolic functions, radians and degrees.", part = CALC, sections = listOf(
                section(
                    "Angle units",
                    para("Rad or Deg above the keys sets the angle unit for sin, cos, tan and their inverses. Multiples of 30° and 45° (\$\\frac{\\pi}{6}\$, \$\\frac{\\pi}{4}\$) give exact answers."),
                    examples(
                        ex(f("sin", fr("π", "4"))),
                        ex(f("cos", fr("2π", "3"))),
                        ex(f("tan", fr("π", "3"))),
                        ex(f("sin", "30"), note = "In degrees", degrees = true),
                        ex(f("cos", "135"), note = "In degrees", degrees = true),
                    ),
                ),
                section(
                    "Inverse functions",
                    examples(
                        ex(f("asin", fr("1", "2"))),
                        ex(f("acos", "0")),
                        ex(f("atan", "1")),
                        ex(f("atan", sq("3")), note = "In degrees", degrees = true),
                    ),
                ),
                section(
                    "Hyperbolic functions",
                    formula("\\sinh x = \\frac{e^x - e^{-x}}{2},\\quad \\cosh x = \\frac{e^x + e^{-x}}{2}"),
                    examples(
                        ex(f("sinh", "0")),
                        ex(f("cosh", m(f("ln", "2"))), note = "Tap ≈ for the decimal"),
                        ex(f("tanh", m(f("ln", "3"))), note = "Tap ≈ for the decimal"),
                    ),
                ),
                section(
                    "Identities",
                    examples(
                        ex(f("sin", "x"), p("2"), "+", f("cos", "x"), p("2")),
                    ),
                ),
            ),
        ),
        Chapter(
            "Complex numbers", "complex", "\$a + bi\$ or polar, with parts, conjugates and arguments.", part = CALC, sections = listOf(
                section(
                    "Arithmetic",
                    para("\$i\$ is on the complex keys. Answers come out as \$a + bi\$; Settings › Numbers can show decimals in polar form."),
                    examples(
                        ex("(2+3i)(1−i)"),
                        ex(fr("1", "1+i")),
                        ex("(1+i)", p("8")),
                        ex(sq("−16")),
                        ex("i", p("i"), note = "A real number"),
                    ),
                ),
                section(
                    "Parts and polar form",
                    examples(
                        ex(f("abs", "3+4i"), note = "The modulus"),
                        ex(f("Re", "3+4i")),
                        ex(f("Im", "3+4i")),
                        ex(f("conj", "3+4i")),
                        ex(f("arg", "1+i"), note = "The argument, in radians"),
                        ex("2e", p(m("i", fr("π", "3"))), note = "Polar to \$a + bi\$"),
                    ),
                ),
            ),
        ),
        Chapter(
            "Calculus", "calculus", "Derivatives, integrals, limits, series, sums and differential equations.", part = CALC, sections = listOf(
                section(
                    "Derivatives",
                    para("The derivative key takes the expression and the variable; the small boxes give a higher order or a point to evaluate at."),
                    examples(
                        ex(d(m(f("sin", m("x", p("2")))))),
                        ex(d(m("x", p("x")))),
                        ex(d(m(f("ln", m("x", p("2"), "+1"))))),
                        ex(d(m("x", p("2"), f("sin", "x"))), note = "Product rule"),
                        ex(d(m("x", p("5")), order = "3"), note = "A third derivative"),
                        ex(d(m("x", p("3")), at = "2"), note = "At a point"),
                        ex(d(m("x", p("2"), "y+y", p("3")), v = "y", partial = true), note = "Partial: other letters are constant"),
                    ),
                ),
                section(
                    "Integrals",
                    para("Leave the limits empty for an antiderivative; fill them for a definite integral. ∞ works as a limit."),
                    examples(
                        ex(int(m("x", p("2")))),
                        ex(int(m("xe", p("x"))), note = "By parts"),
                        ex(int(fr("1", m("1+x", p("2"))))),
                        ex(int(m(f("sin", "x"), p("2")))),
                        ex(int(m("x", p("2")), lo = "0", hi = "1")),
                        ex(int(m("e", p("−x")), lo = "0", hi = "∞"), note = "Improper"),
                        ex(int(m("e", p(m("−x", p("2")))), lo = "−∞", hi = "∞"), note = "The Gaussian integral"),
                        ex(int(fr(m(f("sin", "x")), "x"), lo = "0", hi = "∞"), note = "The Dirichlet integral"),
                        ex(int(fr(m(f("sin", "x")), "x")), note = "Answers in special functions"),
                    ),
                ),
                section(
                    "Limits",
                    para("lim takes the expression and where the variable goes, like \$x \\to 0\$; add \$^{+}\$ or \$^{-}\$ for one side."),
                    examples(
                        ex(lim(fr(m(f("sin", "x")), "x"), "x→0")),
                        ex(lim(m("(1+", fr("1", "x"), ")", p("x")), "x→∞")),
                        ex(lim(fr(m("x−", f("sin", "x")), m("x", p("3"))), "x→0"), note = "From the series"),
                        ex(lim(fr("1", "x"), m("x→0", p("+"))), note = "One-sided"),
                    ),
                ),
                section(
                    "Sums and products",
                    examples(
                        ex(sum("k", "1", "n", "k"), note = "In closed form"),
                        ex(sum("k", "1", "n", m("k", p("2")))),
                        ex(sum("n", "1", "∞", fr("1", m("n", p("2")))), note = "The Basel problem"),
                        ex(sum("n", "0", "∞", fr("1", "n!"))),
                        ex(sum("n", "1", "∞", fr(m("(−1)", p("n+1")), "n")), note = "The alternating harmonic series"),
                        ex(prod("k", "1", "5", "k")),
                    ),
                ),
                section(
                    "Taylor series",
                    para("taylor takes the expression, the point and how many terms."),
                    examples(
                        ex(f("taylor", m(f("sin", "x")), m("x→0"), m("7"))),
                        ex(f("taylor", m("e", p("x")), m("x→0"), m("4"))),
                    ),
                ),
                section(
                    "Differential equations",
                    para("dsolve solves an equation in \$y\$, \$y'\$ and \$y''\$ (y′ and y″ are on the calculus keys)."),
                    examples(
                        ex(f("dsolve", m("y′=y"))),
                        ex(f("dsolve", m("y′′+y=0"))),
                    ),
                ),
            ),
        ),
        Chapter(
            "Vector calculus", "vector", "Gradients, divergence, curl, Jacobians and multiple integrals.", part = CALC, sections = listOf(
                section(
                    "Vectors",
                    examples(
                        ex(f("dot", m(vec("1", "2", "3")), m(vec("4", "5", "6")))),
                        ex(f("cross", m(vec("1", "0", "0")), m(vec("0", "1", "0")))),
                    ),
                ),
                section(
                    "Differential operators",
                    para("The ∇ keys work in Cartesian, cylindrical or spherical coordinates, chosen above the keys."),
                    examples(
                        ex(f("grad", m("x", p("2"), "y"), MathRow())),
                        ex(f("div", m(vec(m("x", p("2")), "xy", "z")))),
                        ex(f("curl", m(vec("−y", "x", "0")))),
                        ex(f("laplacian", m("x", p("2"), "+y", p("2")))),
                        ex(f("jacobian", m(vec("xy", "x+y")))),
                        ex(f("hessian", m("x", p("3"), "+xy"))),
                    ),
                ),
            ),
        ),
        Chapter(
            "Linear algebra", "matrix", "Matrices: products, determinants, inverses, eigenvalues and more.", part = CALC, sections = listOf(
                section(
                    "Matrices",
                    para("The matrix key starts a grid that grows as you type into its last row or column."),
                    examples(
                        ex(mat(2, 2, "1", "2", "3", "4"), mat(2, 1, "5", "6"), note = "A product"),
                        ex(mat(2, 2, "1", "2", "3", "4"), p("−1"), note = "The inverse"),
                        ex(mat(2, 3, "1", "2", "3", "4", "5", "6"), p("T"), note = "The transpose"),
                        ex(f("det", m(mat(2, 2, "1", "2", "3", "4")))),
                        ex(f("det", m(mat(3, 3, "2", "0", "1", "1", "3", "2", "1", "1", "1")))),
                        ex(f("trace", m(mat(2, 2, "1", "2", "3", "4")))),
                    ),
                ),
                section(
                    "Eigenvalues and decompositions",
                    examples(
                        ex(f("eigvals", m(mat(2, 2, "2", "1", "1", "2")))),
                        ex(f("eigvecs", m(mat(2, 2, "2", "1", "1", "2")))),
                        ex(f("charpoly", m(mat(2, 2, "1", "2", "3", "4")))),
                        ex(f("rref", m(mat(2, 3, "1", "2", "3", "4", "5", "6")))),
                        ex(f("rank", m(mat(2, 2, "1", "2", "2", "4")))),
                    ),
                ),
                section(
                    "Element-wise products",
                    examples(
                        ex(f("hadamard", m(mat(2, 2, "1", "2", "3", "4")), m(mat(2, 2, "5", "6", "7", "8")))),
                        ex(f("kron", m(mat(2, 1, "1", "2")), m(mat(1, 2, "3", "4")))),
                    ),
                ),
            ),
        ),
        Chapter(
            "Statistics and probability", "stats", "Averages and spread of lists, counting, and distributions.", part = CALC, sections = listOf(
                section(
                    "Lists",
                    para("Separate values with commas. Lists in brackets, \$[1, 2, 3]\$, work everywhere a number does."),
                    examples(
                        ex(f("mean", "1,2,3,4")),
                        ex(f("median", "3,1,4,1,5")),
                        ex(f("sd", "2,4,4,4,5,5,7,9"), note = "Sample standard deviation"),
                        ex(f("psd", "2,4,4,4,5,5,7,9"), note = "Population standard deviation"),
                        ex(f("var", "1,2,3,4")),
                        ex(f("total", "1,2,3,4")),
                    ),
                ),
                section(
                    "Distributions",
                    examples(
                        ex(f("binompdf", "10", "0.5", "3"), note = "\$P(X = 3)\$ for 10 tries at \$p = 0.5\$"),
                        ex(f("binomcdf", "10", "0.5", "3"), note = "\$P(X \\le 3)\$"),
                        ex(f("poissonpdf", "2", "3"), note = "\$P(X = 3)\$ with mean 2"),
                        ex(f("normcdf", "1.96", "0", "1")),
                        ex(f("normpdf", "0", "0", "1")),
                        ex(f("invnorm", "0.975")),
                    ),
                ),
            ),
        ),
        Chapter(
            "Special functions", "special", "Gamma, zeta, Bessel, error and exponential integrals, and more.", part = CALC, sections = listOf(
                section(
                    "Gamma and zeta",
                    examples(
                        ex(f("gamma", "5"), note = "\$\\Gamma(n) = (n - 1)!\$"),
                        ex(f("gamma", fr("1", "2"))),
                        ex(f("zeta", "2")),
                        ex(f("zeta", "4")),
                    ),
                ),
                section(
                    "Bessel and Lambert W",
                    examples(
                        ex(f("besselj", "0", "0")),
                        ex(f("lambertw", "0")),
                        ex(f("lambertw", "e")),
                    ),
                ),
                section(
                    "Integral functions",
                    para("Integrals with no elementary answer come out in these: the sine and cosine integrals Si and Ci, the exponential integral Ei, the logarithmic integral li, erf and erfi, the Fresnel integrals, the incomplete gamma function and elliptic integrals."),
                    examples(
                        ex(int(fr(m("e", p("x")), "x"))),
                        ex(int(m("e", p(m("−x", p("2")))))),
                        ex(int(fr("1", m(f("ln", "x"))))),
                        ex(int(m(f("sin", m("x", p("2")))))),
                    ),
                ),
            ),
        ),
        Chapter(
            "Contour integrals", "contour", "Loop integrals by residues, on circles in the complex plane.", part = CALC, sections = listOf(
                section(
                    "Residues",
                    para("residue takes a function of \$z\$ and the point; the ∮ key integrates around a circle such as \$|z| = 1\$, adding \$2\\pi i\$ times the residues inside."),
                    examples(
                        ex(f("residue", m(fr("1", "z")), m("z=0"))),
                        ex(f("residue", m(fr(m("e", p("z")), m("z", p("2")))), m("z=0"))),
                        ex(f("contour", m(fr("1", "z")), m(f("abs", "z"), "=1"))),
                        ex(f("contour", m(fr("1", m("z", p("2"), "+1"))), m(f("abs", "z"), "=2")), note = "Both poles are inside"),
                    ),
                ),
            ),
        ),
        Chapter(
            "Constants, units and settings", "units", "Physical constants, the unit converter, number formats and worked steps.", part = CALC, sections = listOf(
                section(
                    "Physical constants",
                    para("The ⚛ keys hold CODATA constants (\$c\$, \$h\$, \$\\hbar\$, \$k_B\$, \$G\$, \$e\$, \$m_e\$…) with their units, in SI or another system chosen above the keys. The list button shows every constant with its name and value."),
                ),
                section(
                    "Number formats",
                    bullets(
                        "Auto, scientific or engineering notation, and how many digits before \$a \\times 10^n\$.",
                        "How many decimals in \$a \\times 10^n\$, and significant digits for decimals.",
                        "Digit grouping (1 000 000), and complex decimals as \$a + bi\$ or polar.",
                    ),
                ),
                section(
                    "Worked steps",
                    para("With New calculator icons on (Settings › Calculator, beta), the function keys show their own math symbols, drawn with the app's round strokes: π, e, ln, ∫, Σ, lim, ∇, x̄, σ, Φ, ℜ, Γ and so on, the operator or constant in the accent color and the boxes and letters it works on in ink. Trigonometry, matrices, letters and constants keep their labels. Holding a key still explains it."),
                    para("With Show steps on (Settings › Calculator, beta), cards for integrals, derivatives, limits, sums, series, algebra, complex numbers, matrices, differential equations, statistics and vector calculus get Steps: every step at once, with nested operations worked from the inside out."),
                ),
            ),
        ),
        Chapter(
            "History", "history", "Everything you've worked out, searchable, pinned and in folders.", part = APP, sections = listOf(
                section(
                    "Cards",
                    table(
                        "Tap the question or answer" to "Puts it at the cursor",
                        "Hold the question or answer" to "Copies it",
                        "Tap the card" to "Shows its actions",
                        "Swipe the card" to "Deletes it (Settings can ask first)",
                    ),
                    para("A card's actions: Graph (or Use), Steps when there are any, copy and pin, and ⋮ for moving to a folder, sharing as an image or LaTeX, and deleting."),
                ),
                section(
                    "The full history",
                    para("Open the history to see every calculation, pinned ones first, then by day. Search finds what you typed or the answer; the chips show only pinned calculations or one folder (hold a folder's chip to rename or empty it). Export saves them as LaTeX or a PDF."),
                    note("Pinned calculations and those in folders are kept whatever the history limit, and Clear leaves them."),
                ),
            ),
        ),
        ModeGuides.graph2D.toChapter("graph2d", "Functions, curves, regions, points and fields in \$x\$ and \$y\$."),
        ModeGuides.graph3D.toChapter("graph3d", "Surfaces, implicit surfaces, curves and solids in three dimensions."),
        ModeGuides.complex.toChapter("complexplane", "Domain coloring of \$f(z)\$, with curves, points and loop integrals."),
        Chapter(
            "Data tables", "table", "Points from a table: typed, pasted or imported.", part = APP, sections = listOf(
                section(
                    "Columns and roles",
                    para("In the 2D graph, ＋ › Table adds a table; tap its line to open it. Tap a column's card for its role: \$x\$, \$y\$, \$\\sigma(x)\$ or \$\\sigma(y)\$ (error bars), or not used. Without an \$x\$ column, rows are numbered 1, 2, 3…"),
                ),
                section(
                    "Importing files",
                    para("The import button left of the bottom bar reads a CSV or TSV file, an Excel workbook (.xlsx, .xlsm, or the older .xls) or an OpenDocument sheet (.ods). For Google Sheets, use File › Download as .xlsx, .ods or .csv. Formulas give their last value, and a row of words above the numbers names the columns. A workbook with numbers on several sheets asks which to import: each becomes its own line, named after its sheet."),
                ),
                section(
                    "Editing",
                    table(
                        "Tap a cell" to "On a tablet, select it and type in the formula bar above the grid. On a phone, as in Excel and Sheets there, a tap selects it and a second tap (or the formula bar, or Edit) types in it, in the formula bar at the bottom. Next moves down, adding a row at the end; 123 / abc switches a phone between the number pad and the whole keyboard",
                        "The toolbar over a selection (phone)" to "The selected cell's or range's address, Edit, Cut, Copy, Paste, Clear, Insert (a row above or below, a column) and Delete (its rows or columns); ✕ lets go",
                        "The bar at the bottom (phone)" to "The commands, as Excel's bar on a phone: the tab from a menu (Home ▾) and its commands as icons that scroll sideways; ⌃ shows them all with their names, by group, a menu opening in place",
                        "The ribbon (tablet)" to "As Excel's, in tabs: Home (Clipboard: paste, cut, copy; Alignment; Number formats; Conditional formatting; Insert and Delete cells; AutoSum, Fill, Clear, Sort & Filter, Find), Insert (rows, columns, a pasted table, a function), Formulas (Insert function, AutoSum, the function library by category, Show formulas), Data (sorts, Custom sort, Filter, Text to columns, Remove duplicates, Trim, Transpose, Insights, Statistics, Share) and View (Show formulas, Zoom, Freeze). Groups have large and small buttons, ▾ for menus and a corner arrow for all the options; ⌃ folds the ribbon away",
                        "Tap a column's card" to "Its sheet: the role as a segmented button and every action as a tile",
                        "The pill, bottom left" to "The selected column's (or range's) sum; tap for the average, count, smallest and largest. A tablet shows them all, and a side panel the column's role, format, filter and statistics",
                        "＋ (bottom right)" to "A row, a column, or a table pasted from the clipboard",
                        "Tap a row's number" to "Insert a row above or below, duplicate it, or remove it",
                        "Drag a grip" to "Resize a column (between the cards) or a row (under its number)",
                        "Double-tap a grip" to "Back to the usual size",
                        "A column's sheet" to "Its role; sort up or down; a filter; statistics (count, sum, mean, median, standard deviation, smallest, largest); a number format; a color scale; fill 1, 2, 3…, a series or the formula down; duplicate, move left or right, clear, delete",
                        "⋮ at the top" to "Share or copy the table as CSV (formulas as their values)",
                        "Hold a cell" to "Select from it; tap another cell to stretch the range. The bar shows its sum, average, count, smallest and largest, with Copy (to paste into a spreadsheet) and Clear",
                    ),
                ),
                section(
                    "Spreadsheet tools",
                    para("Find and replace outlines every matching cell, steps through them with the arrows, and replaces one or all, matching case or whole cells if asked. A filter (a column's ⋮ › Filter…) shows only the rows where that column is equal to, greater or less than a value, contains some text, or is empty or not; filters on several columns all apply, and the rows they hide are still plotted."),
                    para("A number format shows a column's numbers with fixed decimals, as percentages or in scientific notation (1.23 × 10⁴), with thousands grouped if you like; only the look changes, so formulas, fits and the graph use the full numbers. A color scale shades each number from the column's smallest (red) through yellow to its largest (green). Both are kept with the table, and so is a frozen first column, which stays in view as the table scrolls sideways."),
                    para("The formula bar works as Excel's: the Name Box on its left shows the selected cell; tap it and type an address (B12, or a range like A1:C5) to go there. While typing, ✕ puts the cell back as it was and ✓ finishes, and ƒx opens Insert function: every function, searchable and by category, with how it's written and what it does; pick a name to start it in the cell."),
                    para("Copy and Cut take the selected cell or range; Paste puts them at the selected cell, a formula's references moving with it, and Paste values pastes what formulas work out to. Clear empties the cells, or resets their columns' formats, or both. Custom sort sorts by up to four columns in turn, each smallest or largest first. Text to columns splits a column at a comma, semicolon, space, tab, slash or anything else, the rest going into new columns after it. Show formulas shows what's typed in every cell instead of its value; Zoom makes the cells smaller or larger; pinching the table with two fingers zooms too. On a tablet a status bar along the bottom shows the mode (Ready, Enter, Edit, Select), the selection's average, count and sum, and a zoom slider."),
                    para("More from Excel and Sheets: Format Painter (tap it, then a cell in another column, to give that column this one's format); Paste formats and Paste transposed; Cells are written in Computer Modern's typewriter, with its own italic. Bold, Italic and a fill color for the selected cell or range (each cell keeps its own look, moving with it when rows are sorted, inserted or deleted, and pasted along with it), and Change case (UPPERCASE, lowercase, Proper Case), in Font; Currency ($, €, £, ¥) in Number; Find & Select with Go to and Select all, a column or a row. Insert has random numbers (whole, between 0 and 1, or normal), a series, 1, 2, 3…, and today's date, the time or =TODAY(). Formulas has every category of function, Fill formula down and Formulas to values (each formula replaced by what it works out to). Data adds Randomize and Reverse for the rows, Fill blanks (each empty cell takes the one above), To numbers (text like 1,234.5, 12%, $40 or (3) made into numbers), Unique (each value once, in a new column), and New column, which works out a running total, the difference from the row before, the % of the total, the rank, the z-score or the numbers scaled 0 to 1 from the selected column. View hides columns (they're still saved and plotted) and shows them again."),
                    para("AutoSum (Home) puts the sum, average, count, smallest or largest of the numbers just above the selected cell (or, with none above, just left of it) into it as a formula, as Excel's Σ does."),
                    para("Format also has data bars, a bar inside each cell as long as its number is far along the column's range, and a highlight rule, which tints the cells that are equal to, greater or less than a value, contain some text, or are empty, in a color you pick. A rule's tint shows over a color scale."),
                    para("Insights (Data, or a column's sheet) show the column's distribution as a histogram, its mean, median and standard deviation, its straight-line trend against the x column (or the row number) with slope, intercept, r and r², said in words, and its outliers, the numbers beyond 1.5 interquartile ranges of the quartiles, which Mark them outlines in the table."),
                    para("Dragging the fill handle from text counts on, as Excel does: Week 1, Week 2…; Q09, Q10…; Jan, Feb…; Monday, Tuesday… Plain numbers are copied; Fill with a series counts in any step."),
                ),
                section(
                    "Spreadsheet formulas",
                    para("Columns are lettered A, B, C… and a cell starting with = is worked out, as in Excel: =B1*2, =SUM(A:A), =IF(A1>0, A1, 0)."),
                    para("Dragging the fill handle copies a formula and moves its references with it. A \\\$ fixes a part: \\\$A\\\$1 stays put, A\\\$1 keeps its row while the column changes, and \\\$A1 keeps its column while the row changes. While typing, the \\\$ button under the cell anchors the reference you just typed the next way round, as Excel's F4 does."),
                    bullets(
                        "References: A1 a cell, A1:B10 a range, A:A a whole column; \$A\$1 stays put when copied.",
                        "While typing a formula, the cells it uses are outlined in color, and matching functions are suggested above the keyboard.",
                        "Drag the small square at a formula cell's corner down to copy it to the cells below, its references moving with it.",
                        "Errors: #DIV/0!, #VALUE!, #REF!, #NAME?, #NUM!, #N/A, and #CYCLE! for a formula that uses itself.",
                    ),
                ),
            ),
        ),
        Chapter(
            "Fitting", "fit", "Least-squares fits of any formula to the points, with statistics.", part = APP, sections = listOf(
                section(
                    "Fitting a model",
                    para("Type a line with letters other than \$x\$, like \$y = ax + b\$ or \$y = Ae^{-x/\\tau}\$: each letter gets a slider. Once the graph has points, Fit appears under the line; tap it and the sliders move to the best fit."),
                ),
                section(
                    "Statistics",
                    para("Hold Fit for the statistics: each parameter with its standard error, \$R^2\$, the RMSE, the reduced \$\\chi^2\$ and the degrees of freedom \$\\nu\$, with the curve over the points and the residuals."),
                    formula("\\chi^2/\\nu = \\frac{1}{\\nu}\\sum_i \\left(\\frac{y_i - f(x_i)}{\\sigma_i}\\right)^2,\\quad \\nu = n - p"),
                    note("With a \$\\sigma(y)\$ column the fit is weighted by it, and \$\\chi^2/\\nu \\approx 1\$ means the uncertainties match the scatter."),
                ),
            ),
        ),
        Chapter(
            "Unit converter", "converter", "Any units, however they're combined.", part = APP, sections = listOf(
                section(
                    "Converting",
                    para("Open it from the ⋮ menu. Type a value and pick the From and To units: anything like km/s/Mpc, erg, N m² kg⁻², lea/Å. × 10ⁿ and ± beside the value help on keyboards without e or a minus."),
                    para("With \$c\$, \$h\$, \$\\hbar\$ or \$k_B\$ allowed, it also connects mass and energy, energy and frequency or wavelength, and energy and temperature."),
                ),
            ),
        ),
        Chapter(
            "Geometry mode", "geometry", "Constructions in the 2D graph, in space in the 3D graph, and on the complex plane (alpha).", part = APP, sections = listOf(
                section(
                    "Points and objects",
                    para("Turn geometry mode on in Settings › Graphs. Each line of the 2D graph's list can then be a construction: name a point with a capital, like A = (1, 2), and build on it with commands, like Segment(A, B), c = Circle(A, B) or P = Intersect(c, l, 1). A line can use names from any other line. Lowercase letters stay sliders, so Circle(A, r) gets a slider r."),
                    para("Drag a point written with plain numbers and everything built on it follows. A point on an object, P = Point(c, 0.25), slides along it when dragged. Numbers like d = Distance(A, B), Area(t) or Angle(A, B, C) show their value under the line and can be used in other lines."),
                    para("Arithmetic mixes in: Distance(A, B)/2, M = (A + B)/2, B = A + (2, 0) or v = B − A, 3A, and x(A) and y(A) for coordinates. Functions defined on the graph, like f(x) = x², can be used too: Intersect(f, l), Tangent(A, f) at x = x(A), Point(f, 2)."),
                ),
                section(
                    "Commands",
                    para("Every command, with what it takes and what it makes. Type one on a line of the 2D graph's list; names are capitals for points, other letters for the rest."),
                ),
                Section("Every command", listOf(Block.Table(com.example.cas.graph.Geometry.COMMANDS.map { c -> c.usage + (if (c.aliases.isEmpty()) "" else " (also " + c.aliases.joinToString(", ") + ")") to c.help }))),
                section(
                    "In space (the 3D graph)",
                    para("Geometry mode works in the 3D graph too: name a point with three coordinates, A = (1, 2, 3), and build planes, spheres and solids on it. Planes are cut to the box and drawn see-through with spheres and solids; Construct has tools for them (Plane through 3, Sphere, Cube, Tetrahedron, Pyramid, Prism, Reflect in plane, Rotate about line), and a tap on empty space puts a point on the floor (z = 0)."),
                    para("Drag a free point (one written with three plain numbers) to move it: it moves level, keeping its height, stays inside the box and snaps to tidy values. Everything built on it follows. To change its height, edit its line."),
                ),
                Section("Every command in space", listOf(Block.Table(com.example.cas.graph.Geometry3D.COMMANDS.map { c -> c.usage + (if (c.aliases.isEmpty()) "" else " (also " + c.aliases.joinToString(", ") + ")") to c.help }))),
                section(
                    "On the complex plane",
                    para("On the complex plane, points are numbers: A = 1 + 2i, or A = (1, 2). A·B, A/B and powers multiply and divide them as complex numbers, |A|, arg A, √A and e^A work on them, and Conjugate, Modulus, Argument, RootsOfUnity(n) and ComplexRoots(A, n) are commands. With f(z) defined in the list, Image(f, c) draws where f takes a point, line, circle or curve, and f(A) is the point A goes to. Construct has a Complex group of tools for these (Multiply, Divide, Conjugate, nth roots, Modulus, Argument, Image under f)."),
                    para("Drag a point to move it, as on the 2D graph: it snaps to tidy values, a point typed as a number (A = 1 + 1.5i) stays written as one, and a point on a path slides along it. Constructions are drawn like the 2D graph's lines, in their own color, thickness, line style and point shape, with no outline."),
                ),
                section(
                    "Building by tapping",
                    para("The first time Construct opens in each graph, a short guide walks through it at the bottom of the graph: pick a tool, tap to build, drag a point, then what's special to that graph. Each step ticks itself off when you've done it, Show me does it for you, and Skip ends it. The ? on the status card shows the guide again."),
                    para("Construct, top right of the graph, starts building. On a phone the tool palette takes the list's place under the graph (the list comes back when you're done, or when you edit a line or add something), always the same size, its tiles all alike, scrolling when a group has more; on a tablet the tools stand in a rail beside the graph, and touches on it never move the graph. The tools come in groups (points, lines, circles and shapes, conics, measure, transform, and planes and solids in 3D or complex on the complex plane), each with an icon, and each graph offers only the tools that work there; Move, the first, just drags points. A card at the top shows each step (its dots, what it's for, what to tap) and what you've picked, with Undo, Close for a polygon, Finish for tools that take any number of points (polyline, best-fit line), and Done."),
                    para("Some tools ask for a number after the taps: a segment's length, a circle's radius, a regular polygon's sides, an angle's size, a rotation's angle or a dilation's factor. Compass takes a radius from two points and a center from a third. The Transform group mirrors in a line or point, inverts in a circle, rotates, translates by a vector and dilates."),
                    para("Tap points, or empty space to make one there; tools that need a line, circle or curve take a tap on it, and whatever the next tap can take glows. New lines are named on their own (A, B… for points, f, g… for others, α, β… for angles)."),
                    para("A construction's options (tap its color dot) set a point's size, whether its name and coordinates show, a shape's fill, and for a point on an object, Move along its path: it goes round once in 10 seconds, or back and forth, and everything built on it moves too, a locus included."),
                ),
            ),
        ),
        Chapter(
            "Files and export", "files", "Saved graphs, graph files and exports.", part = APP, sections = listOf(
                section(
                    "Graphs and files",
                    para("Graphs are saved as you go. The file button lists them: save, rename, duplicate and open. Save a graph as a .g2d, .g3d or .gcp file to keep or share it; such files open from a file manager, a download or a message with Open with › CAS Calculator."),
                ),
                section(
                    "Export",
                    para("PNG, SVG or PDF, in the style of pgfplots with LaTeX fonts and a legend; 3D surfaces also as STL for printing."),
                ),
            ),
        ),
        Chapter(
            "Keys", "keys", "Every key: its formula, a line of theory and how to use it.", part = REF, sections = KeyHelps.groups.map { (title, _) ->
                Section(title, listOf(Block.Keys(listOf(title))))
            },
        ),
        Chapter(
            "Spreadsheet functions", "sheet", "Over 370 of Excel's functions for data tables, searchable.", part = REF, sections = listOf(
                Section("Functions", listOf(Block.SheetFunctions)),
            ),
        ),
    )

    /** Every calculator example, for working out the answers and testing them. */
    val allExamples: List<Example> get() = chapters.flatMap { c -> c.sections.flatMap { s -> s.blocks.filterIsInstance<Block.Examples>().flatMap { it.examples } } }

    /** A mode's guide as a chapter: what it is, its examples (with what each draws), gestures and tips. */
    private fun ModeGuides.Guide.toChapter(icon: String, summary: String) = Chapter(
        title, icon, summary, part = APP, sections = listOf(
            section("Overview", para(tagline)),
            Section("Examples", listOf(Block.Table(examples.map { "\$" + com.example.cas.engine.Latex.of(it.row) + "\$" to it.note }))),
            Section("Gestures", listOf(Block.Table(gestures.map { it.title to it.text }))),
            Section("Tips", listOf(Block.Table(tips.map { it.title to it.text }))),
        ),
    )
}
