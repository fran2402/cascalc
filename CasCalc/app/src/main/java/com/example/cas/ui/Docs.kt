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
                        "Tap a cell" to "Type in it; Next moves down, adding a row at the end",
                        "＋ (bottom right)" to "A row, a column, or a table pasted from the clipboard",
                        "Tap a row's number" to "Insert a row above or below, or remove it",
                        "Drag a grip" to "Resize a column (between the cards) or a row (under its number)",
                        "Double-tap a grip" to "Back to the usual size",
                        "A column's ⋮" to "Its role, sort by it, fill 1, 2, 3…, clear, remove",
                    ),
                ),
                section(
                    "Spreadsheet formulas (beta)",
                    para("With Spreadsheet formulas on (Settings › Calculator), columns are lettered A, B, C… and a cell starting with = is worked out, as in Excel: =B1*2, =SUM(A:A), =IF(A1>0, A1, 0)."),
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
            "Unit converter", "converter", "Any units, however they're combined (beta).", part = APP, sections = listOf(
                section(
                    "Converting",
                    para("Turn it on in Settings › Calculator, then open it from the ⋮ menu. Type a value and pick the From and To units: anything like km/s/Mpc, erg, N m² kg⁻², lea/Å. × 10ⁿ and ± beside the value help on keyboards without e or a minus."),
                    para("With \$c\$, \$h\$, \$\\hbar\$ or \$k_B\$ allowed, it also connects mass and energy, energy and frequency or wavelength, and energy and temperature."),
                ),
            ),
        ),
        Chapter(
            "Geometry", "geometry", "GeoGebra-style constructions in the 2D graph (alpha).", part = APP, sections = listOf(
                section(
                    "Points and objects",
                    para("Turn it on in Settings › Graphs. Each line of the 2D graph's list can then be a construction: name a point with a capital, like A = (1, 2), and build on it with GeoGebra's commands, like Segment(A, B), c = Circle(A, B) or P = Intersect(c, l, 1). A line can use names from any other line. Lowercase letters stay sliders, so Circle(A, r) gets a slider r."),
                    para("Drag a point written with plain numbers and everything built on it follows. A point on an object, P = Point(c, 0.25), slides along it when dragged. Numbers like d = Distance(A, B), Area(t) or Angle(A, B, C) show their value under the line and can be used in other lines."),
                    para("Arithmetic mixes in: Distance(A, B)/2, M = (A + B)/2, B = A + (2, 0) or v = B − A, 3A, and x(A) and y(A) for coordinates. Functions defined on the graph, like f(x) = x², can be used too: Intersect(f, l), Tangent(A, f) at x = x(A), Point(f, 2)."),
                ),
                section(
                    "Commands",
                    table(
                        "Segment(A, B), Line(A, B), Ray(A, B), Vector(A, B)" to "Straight objects through two points",
                        "Circle(A, B), Circle(A, r), Circle(A, B, C)" to "About A through B, with radius r, or through three points",
                        "Semicircle, CircularArc, CircumcircularArc, CircularSector" to "Parts of circles: on AB, about O from A to B, through three points, filled to the center",
                        "Polygon(A, B, C, …), RegularPolygon(A, B, n)" to "Filled, with three corners or more; regular with n sides on AB",
                        "Ellipse(F, G, a), Hyperbola(F, G, a), Parabola(F, l), Conic(A, B, C, D, E)" to "Conics from foci and a semi-major axis (or a point on them), focus and directrix, or five points",
                        "Point(c, t)" to "A point on a line, circle, arc, polygon, conic or function graph, t along it",
                        "Midpoint, Center, Intersect, Centroid" to "Points from other objects; Intersect(a, b, n) picks the nth",
                        "PerpendicularLine, ParallelLine, PerpendicularBisector, AngleBisector, Tangent" to "Lines built from points and other lines or circles",
                        "Incircle(A, B, C)" to "The circle inside a triangle",
                        "Circumcenter, Orthocenter, Incenter, Centroid(A, B, C)" to "A triangle's centers",
                        "Foci, Vertex, Asymptote, Directrix, Polar" to "A conic's parts, a polygon's corners, a point's polar line",
                        "Locus(P, Q)" to "The curve P traces as Q, a point on an object, moves along it",
                        "AreCollinear, AreConcyclic, AreParallel, ArePerpendicular, AreEqual" to "Checks that show true or false",
                        "Distance, Length, Perimeter, Area, Angle, Slope, Radius" to "Measurements (angles in the graph's degrees or radians)",
                        "Reflect, Rotate, Translate, Dilate" to "Copies moved: mirrored in a line, turned about a point, shifted by a vector, scaled",
                    ),
                ),
                section(
                    "Building by tapping",
                    para("Construct, top right of the graph, starts building. The tool palette at the bottom groups the tools as GeoGebra does (points, lines, circles and shapes, conics, measure and more), each with an icon; Move, the first, just drags points. Picking a tool folds the palette to a bar of recent tools, and a card at the top shows each step (its dots, what it's for, what to tap) and what you've picked, with Undo, Close for a polygon, and Done."),
                    para("Tap points, or empty space to make one there; tools that need a line, circle or curve take a tap on it, and whatever the next tap can take glows. New lines are named as GeoGebra names them (A, B… for points, f, g… for others, α, β… for angles)."),
                    para("＋ › Geometry lists every command; tap one to start a line with it."),
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
