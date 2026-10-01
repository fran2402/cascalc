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
    class Example(val label: String, val row: MathRow, val note: String)

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
        "Exact answers, like a CAS: fractions, roots and π stay as they are, with the decimal a tap away.",
        listOf(
            Example("Exact roots", m(Sqrt(m("8"))), "Simplified: 2√2, and ≈ 2.828 under it"),
            Example("Fractions", m(Frac(m("1"), m("3")), "+", Frac(m("1"), m("6"))), "Stay exact: 1/2"),
            Example("Trigonometry", m(fn("sin", m(Frac(m("π"), m("6"))))), "Exact values: 1/2 (Rad or Deg above the keys)"),
            Example("Solve", m(fn("solve", m("x", sq(), "−5x+6=0"), m("x"))), "Every solution: x = 2, x = 3"),
            Example("Factor", m(fn("factor", m("x", sq("4"), "−5x", sq(), "+4"))), "(x − 2)(x − 1)(x + 1)(x + 2)"),
            Example("Integrals", m(Integral(body = m("xe", Pow(m("x"))), variable = m("x"))), "Antiderivatives, and definite integrals with limits"),
            Example("Derivatives", m(Derivative(variable = m("x"), body = m(fn("sin", m("x")), "x", sq()))), "Exact, with the product and chain rules"),
            Example("Limits", m(fn("lim", m(Frac(m(fn("sin", m("x"))), m("x"))), m("x→0"))), "Found from the series: 1"),
            Example("Sums", m(BigOp(BigOpKind.Sum, variable = m("k"), lower = m("1"), upper = m("n"), body = m("k", sq()))), "In closed form: n(n + 1)(2n + 1)/6"),
            Example("Matrices", m(fn("eigvecs", m(Matrix(2, 2, listOf(m("2"), m("1"), m("1"), m("2")))))), "Eigenvalues and eigenvectors, det, inverse…"),
            Example("Variables", m("a=3"), "Stores a; then 2a gives 6"),
            Example("Functions", m("f(x)=x", sq(), "+1"), "Defines f; then f(2) gives 5"),
        ),
        listOf(
            Tip("tap", "Tap a past question or answer", "Puts it at the cursor, to use again"),
            Tip("hold", "Hold a past answer", "Copies it"),
            Tip("swipe", "Swipe a card sideways", "Deletes it from the history"),
            Tip("hold", "Hold any key", "Says what it does, with an example"),
            Tip("swipe", "Swipe the function keys", "Moves to the next group (the dots show which)"),
        ),
        listOf(
            Tip("swap", "Exact or decimal", "Tap the form under an answer to swap them; Settings › Numbers picks which comes first"),
            Tip("graph", "Graph an answer", "Graph on a card draws it in the right graph"),
            Tip("units", "Constants and units", "The ⚛ group: c, h, k_B… in SI or other systems"),
        ),
    )

    val graph2D = Guide(
        "graph2d", "2D graphing",
        "Type anything in x and y: functions, curves, regions, points, fields. Each line is drawn as soon as it makes sense.",
        listOf(
            Example("Function", m("y=", fn("sin", m("x"))), "y = f(x), or just f(x)"),
            Example("Sliders", m("y=a", fn("sin", m("bx"))), "Other letters get sliders: drag, or press ▶"),
            Example("Curve", m("x", sq(), "+y", sq(), "=4"), "Any equation in x and y"),
            Example("Region", m("y<x", sq()), "Inequalities are shaded"),
            Example("Parametric", m("(", fn("cos", m("t")), ",", fn("sin", m("2t")), ")"), "(x(t), y(t)), with t in both"),
            Example("Polar", m("r=1+", fn("cos", m("θ"))), "r as a function of θ"),
            Example("Point", m("(2,1)"), "A point; [(1,2), (3,4)] is a list"),
            Example("Restricted", m("y=x", sq(), ",0<x<2"), "Conditions after a comma"),
            Example("Heat map", m("x", sq(), "−y", sq()), "f(x, y) colored with a colormap"),
            Example("Vector field", m("(−y,x)"), "A pair in x and y: arrows"),
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
            Tip("table", "Data", "Import a CSV or edit points as a table; a line with unknowns (y = ax + b) then gets Fit"),
            Tip("export", "Export", "PNG, JPG, SVG or PDF in a LaTeX style, and a graph file to share"),
        ),
    )

    val graph3D = Guide(
        "graph3d", "3D graphing",
        "Surfaces, solids, curves and points in space, in Cartesian, cylindrical or spherical coordinates.",
        listOf(
            Example("Surface", m("z=", fn("sin", m("x")), fn("cos", m("y"))), "z = f(x, y), or just f(x, y)"),
            Example("Implicit", m("x", sq(), "+y", sq(), "+z", sq(), "=4"), "Any equation in x, y and z"),
            Example("Solid", m("x", sq(), "+y", sq(), "<z"), "Inequalities fill the region"),
            Example("Space curve", m("(", fn("cos", m("t")), ",", fn("sin", m("t")), ",", Frac(m("t"), m("4")), ")"), "(x(t), y(t), z(t))"),
            Example("Point", m("(1,2,3)"), "A point in space"),
            Example("Sliders", m("z=a", fn("sin", m("x")), "+y", sq()), "Other letters get sliders"),
        ),
        listOf(
            Tip("drag", "Drag", "Turns the view; pinch to zoom, double-tap to reset"),
            Tip("tap", "Tap a surface", "Its point's x, y and z"),
            Tip("hold", "Hold a line's dot", "Its color and colormap"),
        ),
        listOf(
            Tip("axes", "Coordinates", "The chip above the keys: Cartesian, cylindrical (r, θ, z) or spherical (ρ, θ, φ), with letters you choose"),
            Tip("export", "Export", "An image, or an STL file to 3D-print a surface"),
        ),
    )

    val complex = Guide(
        "complex", "Complex plotting",
        "A function of z, drawn by domain coloring: the hue is arg f(z), the brightness its size. Zeros and poles show as color wheels.",
        listOf(
            Example("Zeros and poles", m(Frac(m("z", sq(), "−1"), m("(z−2−i)", sq()))), "Two zeros, a double pole: count the wheels"),
            Example("Essential singularity", m("e", Pow(m(Frac(m("1"), m("z"))))), "Every color near 0"),
            Example("Function of z", m("f(z)=", fn("sin", m("z"))), "f(z) = … as well as plain expressions"),
            Example("Curve", m(fn("abs", m("z−1")), "=2"), "Equations in z are drawn as curves"),
            Example("Points", m("[1+i,−2,3i]"), "A number, or a list of them"),
            Example("Path", m("e", Pow(m("it"))), "z(t): an expression in t"),
            Example("Loop integral", m(Func("contour", listOf(m(Frac(m("1"), m("z"))), m(fn("abs", m("z")), "=1")))), "∮ around a circle: 2πi here"),
        ),
        listOf(
            Tip("drag", "Drag", "Moves the plane; pinch to zoom, double-tap to reset"),
            Tip("tap", "Tap the plane", "z and f(z) there, in a + bi and polar form"),
            Tip("tap", "Tap a curve or a point", "Its z, and for a closed curve the area inside and ∮ f(z) dz around it"),
            Tip("hold", "Hold a line's dot", "The colormap for arg f"),
        ),
        listOf(
            Tip("bands", "Toolbar", "Modulus bands, phase lines, the Re/Im grid and the polar grid"),
            Tip("export", "Export", "PNG, JPG, SVG or PDF, with the legend set in LaTeX's font"),
        ),
    )

    val all = listOf(calculator, graph2D, graph3D, complex)
}
