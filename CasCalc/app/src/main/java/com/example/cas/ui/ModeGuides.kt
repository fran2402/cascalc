package com.example.cas.ui

import com.example.cas.editor.BigOp
import com.example.cas.editor.BigOpKind
import com.example.cas.editor.Derivative
import com.example.cas.editor.Frac
import com.example.cas.editor.Func
import com.example.cas.editor.Integral
import com.example.cas.editor.MathRow
import com.example.cas.editor.Matrix
import com.example.cas.editor.Node
import com.example.cas.editor.Pow
import com.example.cas.editor.Sqrt
import com.example.cas.editor.Sym

/**
 * What each mode does and how, for the guide that opens on holding its button: examples you
 * can try (each a row exactly as the keys would type it), then gestures and tips.
 */
object ModeGuides {
    /** Something to try: what it is, the math, what happens. */
    class Example(val label: String, val row: MathRow, val note: String, val advanced: Boolean = false)

    /** A gesture or a tip: a short title and a line about it. */
    class Tip(val icon: String, val title: String, val text: String)

    class Guide(
        val key: String,
        val title: String,
        val tagline: String,
        val examples: List<Example>,
        val gestures: List<Tip>,
        val tips: List<Tip>,
    )

    /** A row from text (one symbol per character) and nodes. */
    private fun m(vararg parts: Any): MathRow = MathRow(
        parts.flatMap { p ->
            when (p) {
                is String -> p.map { Sym(it.toString()) }
                is Node -> listOf(p)
                else -> error("not math: $p")
            }
        }.toMutableList(),
    )
    private fun fn(name: String, vararg args: MathRow) = Func(name, args.toList())
    private fun sq(e: String = "2") = Pow(m(e))

    val calculator = Guide(
        "calculator", "Calculator",
        "Exact answers, like a CAS: fractions, roots and \$\\pi\$ stay as they are, with the decimal a tap away.",
        listOf(
            Example("Exact roots", m(Sqrt(m("8"))), "Simplified: \$2\\sqrt{2}\$, and \$\\approx 2.828\$ under it"),
            Example("Fractions", m(Frac(m("1"), m("3")), "+", Frac(m("1"), m("6"))), "Stay exact: 1/2"),
            Example("Trigonometry", m(fn("sin", m(Frac(m("π"), m("6"))))), "Exact values: \$\\frac{1}{2}\$ (Rad or Deg above the keys)"),
            Example("Complex numbers", m("(1+2i)(3−i)"), "Worked out exactly: \$5 + 5i\$"),
            Example("Euler's identity", m("e", Pow(m("iπ")), "+1"), "\$0\$, exactly", advanced = true),
            Example("Gamma function", m(fn("gamma", m(Frac(m("1"), m("2"))))), "Exact special values: \$\\sqrt{\\pi}\$", advanced = true),
            Example("Equations", m(fn("solve", m("x", sq(), "−5x+6=0"), m("x"))), "Solved exactly: \$x = 2,\\ x = 3\$"),
            Example("Integrals", m(Integral(body = m("xe", Pow(m("x"))), variable = m("x"))), "Antiderivatives, and definite integrals with limits"),
            Example("Derivatives", m(Derivative(variable = m("x"), body = m(fn("sin", m("x")), "x", sq()))), "Exact, with the product and chain rules"),
            Example("Limits", m(fn("lim", m(Frac(m(fn("sin", m("x"))), m("x"))), m("x→0"))), "Found from the series: \$1\$"),
            Example("Sums", m(BigOp(BigOpKind.Sum, variable = m("k"), lower = m("1"), upper = m("n"), body = m("k", sq()))), "In closed form: n(n + 1)(2n + 1)/6"),
            Example("Gaussian integral", m(Integral(lower = m("−∞"), upper = m("∞"), body = m("e", Pow(m("−x", sq()))), variable = m("x"))), "Improper integrals in closed form: \$\\sqrt{\\pi}\$", advanced = true),
            Example("Basel problem", m(BigOp(BigOpKind.Sum, variable = m("n"), lower = m("1"), upper = m("∞"), body = m(Frac(m("1"), m("n", sq()))))), "Infinite series recognized: \$\\frac{\\pi^2}{6}\$", advanced = true),
            Example("Alternating series", m(BigOp(BigOpKind.Sum, variable = m("n"), lower = m("1"), upper = m("∞"), body = m(Frac(m("(−1)", Pow(m("n+1"))), m("n"))))), "The \$\\eta\$ function at \$1\$: \$\\ln 2\$", advanced = true),
            Example("Series limit", m(fn("lim", m(Frac(m("x−", fn("sin", m("x"))), m("x", sq("3")))), m("x→0"))), "From the Taylor series: \$\\frac{1}{6}\$", advanced = true),
            Example("Special integral", m(Integral(body = m(Frac(m(fn("sin", m("x"))), m("x"))), variable = m("x"))), "Answers in special functions: Si(x)", advanced = true),
            Example("Dirichlet integral", m(Integral(lower = m("0"), upper = m("∞"), body = m(Frac(m(fn("sin", m("x"))), m("x"))), variable = m("x"))), "Classic improper integral: \$\\frac{\\pi}{2}\$", advanced = true),
            Example("Matrices", m(fn("eigvecs", m(Matrix(2, 2, listOf(m("2"), m("1"), m("1"), m("2")))))), "Eigenvalues and eigenvectors, det, inverse…"),
            Example("Variables", m("a=3"), "Stores \$a\$; then \$2a\$ gives \$6\$"),
            Example("Functions", m("f(x)=x", sq(), "+1"), "Defines \$f\$; then \$f(2)\$ gives \$5\$"),
        ),
        listOf(
            Tip("tap", "Tap a past question or answer", "Puts it at the cursor, to use again"),
            Tip("hold", "Hold a past answer", "Copies it"),
            Tip("swipe", "Swipe a card sideways", "Deletes it from the history"),
            Tip("hold", "Hold any key", "Says what it does, with an example"),
            Tip("swipe", "Swipe the function keys", "Moves to the next group (the dots show which)"),
        ),
        listOf(
            Tip("swap", "Exact or decimal", "Tap the \$\\approx\$ chip beside an answer for its decimal, then exact to go back; Settings › Numbers picks which comes first"),
            Tip("graph", "Graph an answer", "Graph on a card draws it in the right graph"),
            Tip("units", "Constants and units", "The ⚛ group: \$c\$, \$h\$, \$k_B\$… in SI or other systems"),
        ),
    )

    val graph2D = Guide(
        "graph2d", "2D graphing",
        "Type anything in \$x\$ and \$y\$: functions, curves, regions, points, fields. Each line is drawn as soon as it makes sense.",
        listOf(
            Example("Function", m("y=", fn("sin", m("x"))), "\$y = f(x)\$, or just \$f(x)\$"),
            Example("Sliders", m("y=a", fn("sin", m("bx"))), "Other letters get sliders: drag, or press ▶"),
            Example("Curve", m("x", sq(), "+y", sq(), "=4"), "Any equation in \$x\$ and \$y\$"),
            Example("Region", m("y<x", sq()), "Inequalities are shaded"),
            Example("Parametric", m("(", fn("cos", m("t")), ",", fn("sin", m("2t")), ")"), "\$(x(t), y(t))\$, with \$t\$ in both"),
            Example("Polar", m("r=1+", fn("cos", m("θ"))), "\$r\$ as a function of \$\\theta\$"),
            Example("Point", m("(2,1)"), "A point; \$[(1, 2), (3, 4)]\$ is a list"),
            Example("Restricted", m("y=x", sq(), ",0<x<2"), "Conditions after a comma"),
            Example("Heat map", m("x", sq(), "−y", sq()), "\$f(x, y)\$ colored with a colormap"),
            Example("Vector field", m("(−y,x)"), "A pair in \$x\$ and \$y\$: arrows"),
            Example("Rose", m("r=", fn("sin", m("4θ"))), "an eight-petal polar rose", advanced = true),
            Example("Heart", m("(x", sq(), "+y", sq(), "−1)", Pow(m("3")), "=x", sq(), "y", Pow(m("3"))), "an implicit sextic", advanced = true),
            Example("Folium", m("x", Pow(m("3")), "+y", Pow(m("3")), "=3xy"), "Descartes' folium, with its loop", advanced = true),
            Example("Butterfly", m("(", fn("sin", m("t")), "(e", Pow(m(fn("cos", m("t")))), "−2", fn("cos", m("4t")), "),", fn("cos", m("t")), "(e", Pow(m(fn("cos", m("t")))), "−2", fn("cos", m("4t")), "))"), "Fay's butterfly curve", advanced = true),
            Example("Annulus", m("1<x", sq(), "+y", sq(), "<4"), "a chain of inequalities", advanced = true),
            Example("Family", m("y=[1,2,3]x", sq()), "a list draws one curve per entry", advanced = true),
            Example("Ripples", m(fn("sin", m("x", sq(), "+y", sq()))), "a heat map of \$\\sin(x^2 + y^2)\$", advanced = true),
            Example("Saddle flow", m("(x,−y)"), "a hyperbolic vector field", advanced = true),
        ),
        listOf(
            Tip("drag", "Drag", "Moves the view; pinch to zoom, double-tap to reset"),
            Tip("tap", "Tap a curve", "Its point: zeros, extrema and crossings, and the area from there"),
            Tip("hold", "Hold a line's dot", "Color, line style, points, arrows"),
            Tip("hold", "Hold a line", "Renames it in the legend"),
            Tip("swipe", "Swipe a line", "Deletes it"),
        ),
        listOf(
            Tip("folder", "Folders and notes", "＋ adds lines, notes and folders; drag to reorder"),
            Tip("table", "Data", "Import a CSV or edit points as a table; a line with unknowns (\$y = ax + b\$) then gets Fit"),
            Tip("export", "Export", "PNG, JPG, SVG or PDF in a LaTeX style, and a graph file to share"),
        ),
    )

    val graph3D = Guide(
        "graph3d", "3D graphing",
        "Surfaces, solids, curves and points in space, in Cartesian, cylindrical or spherical coordinates.",
        listOf(
            Example("Surface", m("z=", fn("sin", m("x")), fn("cos", m("y"))), "\$z = f(x, y)\$, or just \$f(x, y)\$"),
            Example("Implicit", m("x", sq(), "+y", sq(), "+z", sq(), "=4"), "Any equation in \$x\$, \$y\$ and \$z\$"),
            Example("Solid", m("x", sq(), "+y", sq(), "<z"), "Inequalities fill the region"),
            Example("Space curve", m("(", fn("cos", m("t")), ",", fn("sin", m("t")), ",", Frac(m("t"), m("4")), ")"), "\$(x(t), y(t), z(t))\$"),
            Example("Point", m("(1,2,3)"), "A point in space"),
            Example("Sliders", m("z=a", fn("sin", m("x")), "+y", sq()), "Other letters get sliders"),
            Example("Saddle", m("z=x", sq(), "−y", sq()), "a hyperbolic paraboloid", advanced = true),
            Example("Ripple", m("z=", fn("sin", m(Sqrt(m("x", sq(), "+y", sq()))))), "circular waves", advanced = true),
            Example("Torus", m("(", Sqrt(m("x", sq(), "+y", sq())), "−2)", sq(), "+z", sq(), "=1"), "an implicit doughnut", advanced = true),
            Example("Gyroid", m(fn("sin", m("x")), fn("cos", m("y")), "+", fn("sin", m("y")), fn("cos", m("z")), "+", fn("sin", m("z")), fn("cos", m("x")), "=0"), "a triply periodic minimal surface", advanced = true),
            Example("Trefoil knot", m("(", fn("sin", m("t")), "+2", fn("sin", m("2t")), ",", fn("cos", m("t")), "−2", fn("cos", m("2t")), ",−", fn("sin", m("3t")), ")"), "a knotted space curve", advanced = true),
        ),
        listOf(
            Tip("drag", "Drag", "Turns the view; pinch to zoom, double-tap to reset"),
            Tip("tap", "Tap a surface", "Its point's \$x\$, \$y\$ and \$z\$"),
            Tip("hold", "Hold a line's dot", "Its color and colormap"),
        ),
        listOf(
            Tip("axes", "Coordinates", "The chip above the keys: Cartesian, cylindrical \$(r, \\theta, z)\$ or spherical \$(\\rho, \\theta, \\varphi)\$, with letters you choose"),
            Tip("export", "Export", "An image, or an STL file to 3D-print a surface"),
        ),
    )

    val complex = Guide(
        "complex", "Complex plotting",
        "A function of \$z\$, drawn by domain coloring: the hue is \$\\arg f(z)\$, the brightness its size. Zeros and poles show as color wheels.",
        listOf(
            Example("Zeros and poles", m(Frac(m("z", sq(), "−1"), m("(z−2−i)", sq()))), "Two zeros, a double pole: count the wheels"),
            Example("Essential singularity", m("e", Pow(m(Frac(m("1"), m("z"))))), "Every color near \$0\$"),
            Example("Function of z", m("f(z)=", fn("sin", m("z"))), "\$f(z) = \\ldots\$ as well as plain expressions"),
            Example("Curve", m(fn("abs", m("z−1")), "=2"), "Equations in \$z\$ are drawn as curves"),
            Example("Points", m("[1+i,−2,3i]"), "A number, or a list of them"),
            Example("Path", m("e", Pow(m("it"))), "z(t): an expression in t"),
            Example("Roots of unity", m("z", Pow(m("5")), "−1"), "five zeros on the unit circle", advanced = true),
            Example("Gamma function", m(fn("gamma", m("z"))), "poles at \$0, -1, -2, \\ldots\$", advanced = true),
            Example("Riemann zeta", m(fn("zeta", m("z"))), "the pole at 1 and the trivial zeros", advanced = true),
            Example("Möbius map", m(Frac(m("z−i"), m("z+i"))), "the upper half-plane onto the disc", advanced = true),
            Example("Branch cut", m(Sqrt(m("z"))), "the jump along the negative axis", advanced = true),
            Example("Loop integral", m(Func("contour", listOf(m(Frac(m("1"), m("z"))), m(fn("abs", m("z")), "=1")))), "\$\\oint\$ around a circle: \$2\\pi i\$ here"),
        ),
        listOf(
            Tip("drag", "Drag", "Moves the plane; pinch to zoom, double-tap to reset"),
            Tip("tap", "Tap the plane", "\$z\$ and \$f(z)\$ there, in \$a + bi\$ and polar form"),
            Tip("tap", "Tap a curve or a point", "Its \$z\$, and for a closed curve the area inside and \$\\oint f(z)\\,dz\$ around it"),
            Tip("hold", "Hold a line's dot", "The colormap for \$\\arg f\$"),
        ),
        listOf(
            Tip("bands", "Toolbar", "Modulus bands, phase lines, the Re/Im grid and the polar grid"),
            Tip("export", "Export", "PNG, JPG, SVG or PDF, with the legend set in LaTeX's font"),
        ),
    )

    val all = listOf(calculator, graph2D, graph3D, complex)
}
