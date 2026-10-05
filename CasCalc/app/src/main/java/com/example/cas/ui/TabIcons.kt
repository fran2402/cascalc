package com.example.cas.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/*
 * Icons for the function tabs, drawn on a 24×24 grid with 2 dp round strokes
 * to sit alongside Material Symbols Rounded. The same path data draws the
 * preview images (preview-render/icons.py), so both match exactly.
 */

private fun icon(name: String, stroke: List<String>, thin: List<String> = emptyList(), fill: List<String> = emptyList(), shade: List<String> = emptyList(), accent: Set<String> = emptySet()): ImageVector =
    // Two-tone like the data table's icons: the accent paths are what the tab or option is about.
    tableIcon("Tab$name", stroke, thin, fill, shade, accent)


object TabIcons {
    /** √x: powers, roots and logarithms. */
    val Roots: ImageVector by lazy { icon("Roots", stroke = listOf("M3 13l2.5-1.5L8.5 19L12.5 5H21", "M14.5 10l4.5 5.5M19 10l-4.5 5.5"), accent = setOf("M14.5 10l4.5 5.5M19 10l-4.5 5.5")) }

    /** A right triangle with its angle marked: trigonometry. */
    val Triangle: ImageVector by lazy { icon("Triangle", stroke = listOf("M4 19H20V5Z"), thin = listOf("M9.5 19A5.5 5.5 0 0 0 8.1 15.4", "M17 19V16H20"), accent = setOf("M9.5 19A5.5 5.5 0 0 0 8.1 15.4", "M17 19V16H20")) }

    /** A balance, both sides of an equation: algebra. */
    val Balance: ImageVector by lazy { icon("Balance", stroke = listOf("M12 3.5V20", "M7.5 20h9", "M4 7.5h16", "M4 7.5L1.8 13", "M4 7.5L6.2 13", "M20 7.5L17.8 13", "M20 7.5L22.2 13"), fill = listOf("M1.2 13h5.6a2.8 2.4 0 0 1-5.6 0z", "M17.2 13h5.6a2.8 2.4 0 0 1-5.6 0z"), accent = setOf("M1.2 13h5.6a2.8 2.4 0 0 1-5.6 0z", "M17.2 13h5.6a2.8 2.4 0 0 1-5.6 0z")) }

    /** The area under a curve: calculus. */
    val Area: ImageVector by lazy { icon("Area", stroke = listOf("M3 20h18", "M3 17C6.5 17 8.5 6 12 6s5 7.5 9 6"), shade = listOf("M7.5 20v-6.3C9 9.5 10.3 6 12 6s3.2 3.6 5 5.6V20z"), accent = setOf("M7.5 20v-6.3C9 9.5 10.3 6 12 6s3.2 3.6 5 5.6V20z")) }

    /** Two crossing curves with the region between them shaded: area between curves. */
    val AreaBetween: ImageVector by lazy { icon("AreaBetween", stroke = listOf("M3 18C8 18 10 6 21 5", "M3 8c6 0 9 11 18 12"), shade = listOf("M12 12.3C14.6 8.5 17 6.2 21 5v15c-4-.4-6.6-3.3-9-7.7z"), accent = setOf("M12 12.3C14.6 8.5 17 6.2 21 5v15c-4-.4-6.6-3.3-9-7.7z")) }

    /** A curve with the straight line touching it at a point: the tangent there. */
    val Tangent: ImageVector by lazy { icon("Tangent", stroke = listOf("M5 20.9L20 9.3"), thin = listOf("M3 19Q12 19 21 5"), fill = listOf("M10.2 15.5a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0-3.6 0z"), accent = setOf("M5 20.9L20 9.3")) }

    /** A curve with the line at right angles to it through a point: the normal there. */
    val Normal: ImageVector by lazy { icon("Normal", stroke = listOf("M7.1 9.2L15.4 19.9"), thin = listOf("M3 19Q12 19 21 5", "M13.5 14.4L14.6 15.8L16 14.7"), fill = listOf("M10.2 15.5a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0-3.6 0z"), accent = setOf("M7.1 9.2L15.4 19.9")) }

    /** A stretch of a curve picked out between two points: its arc length. */
    val ArcLength: ImageVector by lazy { icon("ArcLength", stroke = listOf("M8.4 17.7Q12.9 15.6 17.4 10"), thin = listOf("M3 19Q12 19 21 5"), fill = listOf("M6.8 17.7a1.6 1.6 0 1 0 3.2 0a1.6 1.6 0 1 0-3.2 0z", "M15.8 10a1.6 1.6 0 1 0 3.2 0a1.6 1.6 0 1 0-3.2 0z"), accent = setOf("M8.4 17.7Q12.9 15.6 17.4 10")) }

    /** ∇, nabla: vector calculus. */
    val Nabla: ImageVector by lazy { icon("Nabla", stroke = listOf("M3.5 5h17L12 20.5z"), thin = listOf("M7.2 6.8l5.6 10.2"), accent = setOf("M3.5 5h17L12 20.5z")) }

    /** A 2×2 matrix: linear algebra. */
    val Matrix: ImageVector by lazy { icon("Matrix", stroke = listOf("M7 4H4v16h3", "M17 4h3v16h-3"), fill = listOf("M7.5 9a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0-3.4 0z", "M13.1 9a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0-3.4 0z", "M7.5 15a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0-3.4 0z", "M13.1 15a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0-3.4 0z"), accent = setOf("M7.5 9a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0-3.4 0z", "M13.1 9a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0-3.4 0z", "M7.5 15a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0-3.4 0z", "M13.1 15a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0-3.4 0z")) }

    /** An atom: physical constants. */
    val Atom: ImageVector by lazy { icon("Atom", stroke = listOf(), thin = listOf("M2.5 12a9.5 3.6 0 1 0 19 0a9.5 3.6 0 1 0-19 0z", "M7.25 3.77A9.5 3.6 60 1 0 16.75 20.23A9.5 3.6 60 1 0 7.25 3.77z", "M7.25 20.23A9.5 3.6 -60 1 0 16.75 3.77A9.5 3.6 -60 1 0 7.25 20.23z"), fill = listOf("M10.2 12a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0-3.6 0z"), accent = setOf("M10.2 12a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0-3.6 0z")) }

    /** |∨|, the graph of an absolute value: more functions. */
    val Abs: ImageVector by lazy { icon("Abs", stroke = listOf("M3 4v16", "M21 4v16", "M7 7l5 9.5L17 7"), accent = setOf("M7 7l5 9.5L17 7")) }

    /** An Argand diagram, z as a vector from 0 with its angle: the complex-plot mode. */
    val Complex: ImageVector by lazy { icon("Complex", stroke = listOf("M4 20H21", "M4 20V3", "M4 20L15.5 8.5"), thin = listOf("M9.2 20A5.2 5.2 0 0 0 7.68 16.32"), fill = listOf("M13.4 8.5a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0-4.2 0z"), accent = setOf("M4 20L15.5 8.5", "M13.4 8.5a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0-4.2 0z")) }

    /** ℂ, a double-struck C: the complex-numbers tab. */
    val ComplexC: ImageVector by lazy { icon("ComplexC", stroke = listOf("M18.5 7.2A7.8 7.8 0 1 0 18.5 16.8"), thin = listOf("M9 5.6V18.4"), accent = setOf("M18.5 7.2A7.8 7.8 0 1 0 18.5 16.8")) }

    /** A bell curve over a small histogram: statistics. */
    val Stats: ImageVector by lazy { icon("Stats", stroke = listOf("M3 20.5H21", "M3 18.5c3.5 0 5-12.5 9-12.5s5.5 12.5 9 12.5"), fill = listOf("M7 20.5v-5.5h2.2v5.5z", "M10.9 20.5V10h2.2v10.5z", "M14.8 20.5v-5.5H17v5.5z"), accent = setOf("M7 20.5v-5.5h2.2v5.5z", "M10.9 20.5V10h2.2v10.5z", "M14.8 20.5v-5.5H17v5.5z")) }

    /** The symbol builder: an x with a hat, a superscript box and a +. */
    val SymbolBuilder: ImageVector by lazy { icon("SymbolBuilder", stroke = listOf("M4 10.5l7 9.5", "M11 10.5l-7 9.5", "M4.5 7.5l3-3l3 3"), thin = listOf("M14 3.5h5v5h-5z", "M15 17.5h6", "M18 14.5v6")) }

    /** ℤ, a double-struck Z: the number theory tab. */
    val Integers: ImageVector by lazy { icon("Integers", stroke = listOf("M6 5h12L6 19h12"), thin = listOf("M14.6 5L6 15"), accent = setOf("M6 5h12L6 19h12")) }

    /** The S-curve of erf crossing its axes: the special functions tab. */
    val Special: ImageVector by lazy { icon("Special", stroke = listOf("M3 18.5C9 18.5 9.5 12 12 12S15 5.5 21 5.5"), thin = listOf("M3 12h18", "M12 3v18"), accent = setOf("M3 18.5C9 18.5 9.5 12 12 12S15 5.5 21 5.5")) }

    /** The unit circle with the tangent line that gives tan and sec: more trigonometry. */
    val TrigMore: ImageVector by lazy { icon("TrigMore", stroke = listOf("M5 12a7 7 0 1 0 14 0a7 7 0 1 0-14 0", "M19 3v18"), thin = listOf("M12 12L19 6.5"), accent = setOf("M19 3v18")) }

    /** A square wave over its axis: signals and piecewise functions. */
    val Signals: ImageVector by lazy { icon("Signals", stroke = listOf("M3 16h4V7h5v9h5V7h4"), thin = listOf("M3 20h18"), accent = setOf("M3 16h4V7h5v9h5V7h4")) }

    /** A sieve of the numbers 2 to 10, the primes picked out: primes and sequences. */
    val Primes: ImageVector by lazy { icon("Primes", stroke = emptyList(), fill = listOf("M17.9 5a1.1 1.1 0 1 0 2.2 0a1.1 1.1 0 1 0 -2.2 0z", "M10.9 12a1.1 1.1 0 1 0 2.2 0a1.1 1.1 0 1 0 -2.2 0z", "M3.9 19a1.1 1.1 0 1 0 2.2 0a1.1 1.1 0 1 0 -2.2 0z", "M10.9 19a1.1 1.1 0 1 0 2.2 0a1.1 1.1 0 1 0 -2.2 0z", "M17.9 19a1.1 1.1 0 1 0 2.2 0a1.1 1.1 0 1 0 -2.2 0z", "M3.3 5a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0 -3.4 0z", "M10.3 5a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0 -3.4 0z", "M3.3 12a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0 -3.4 0z", "M17.3 12a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0 -3.4 0z"), accent = setOf("M3.3 5a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0 -3.4 0z", "M10.3 5a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0 -3.4 0z", "M3.3 12a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0 -3.4 0z", "M17.3 12a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0 -3.4 0z")) }

    /** A golden rectangle with its spiral: mathematical constants. */
    val MathConst: ImageVector by lazy { icon("MathConst", stroke = listOf("M3.5 17.5A11 11 0 0 1 14.5 6.5", "M14.5 6.5A6 6 0 0 1 20.5 12.5"), thin = listOf("M3.5 6.5h17v11h-17z", "M14.5 6.5v11"), accent = setOf("M3.5 17.5A11 11 0 0 1 14.5 6.5", "M14.5 6.5A6 6 0 0 1 20.5 12.5")) }

    /** A density with its right tail shaded: probability distributions. */
    val Distributions: ImageVector by lazy { icon("Distributions", stroke = listOf("M3 19.5h18", "M3 19c4 0 5.5-13 9-13s5 13 9 13"), shade = listOf("M15.3 19.5V12.3c1.4 3.6 3 6.7 5.7 6.7v.5z"), accent = setOf("M15.3 19.5V12.3c1.4 3.6 3 6.7 5.7 6.7v.5z")) }

    /** Pascal's triangle as dots, its edges picked out: combinatorics. */
    val Combinatorics: ImageVector by lazy { icon("Combinatorics", stroke = emptyList(), fill = listOf("M10.5 14.2a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0z", "M8.0 18.8a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0z", "M13.0 18.8a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0z", "M10.5 5.0a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0z", "M8.0 9.6a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0z", "M13.0 9.6a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0z", "M5.5 14.2a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0z", "M15.5 14.2a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0z", "M3.0 18.8a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0z", "M18.0 18.8a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0z"), accent = setOf("M10.5 5.0a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0z", "M8.0 9.6a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0z", "M13.0 9.6a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0z", "M5.5 14.2a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0z", "M15.5 14.2a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0z", "M3.0 18.8a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0z", "M18.0 18.8a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0z")) }

    /** A box plot: quartiles, median and whiskers, more statistics. */
    val StatsMore: ImageVector by lazy { icon("StatsMore", stroke = listOf("M7.5 7.5h9v9h-9z", "M12 7.5v9"), thin = listOf("M4 12h3.5", "M16.5 12H20", "M4 9.5v5", "M20 9.5v5"), accent = setOf("M12 7.5v9")) }

    /** Aα: Latin and Greek letters. */
    val Letters: ImageVector by lazy { icon("Letters", stroke = listOf("M2.5 19L7 5l4.5 14", "M4.2 14h5.6", "M22 11.5c-.8 3-2.2 7.5-4.3 7.5c-1.9 0-3.2-1.6-3.2-3.8c0-2.2 1.4-3.9 3.3-3.9c2.6 0 3.2 5.1 4.7 7.7"), accent = setOf("M22 11.5c-.8 3-2.2 7.5-4.3 7.5c-1.9 0-3.2-1.6-3.2-3.8c0-2.2 1.4-3.9 3.3-3.9c2.6 0 3.2 5.1 4.7 7.7")) }
}

/** Toolbar icons for the complex plot, in the same style. */
object PlotIcons {
    /** Three axes from a corner, z up: Cartesian coordinates (x, y, z) in 3D. */
    val Cartesian: ImageVector by lazy {
        icon("Cartesian",
            stroke = listOf("M9 15V3.5", "M9 15L20.5 15", "M9 15L3 20.5"),
            thin = listOf("M7.2 5.3L9 3.5l1.8 1.8", "M18.7 13.2l1.8 1.8l-1.8 1.8"), accent = setOf("M7.2 5.3L9 3.5l1.8 1.8", "M18.7 13.2l1.8 1.8l-1.8 1.8"))
    }
    /** A cylinder: cylindrical coordinates (r, θ, z) in 3D. */
    val Cylindrical: ImageVector by lazy {
        icon("Cylindrical",
            stroke = listOf("M5 6.5a7 2.6 0 1 0 14 0a7 2.6 0 1 0-14 0z", "M5 6.5v11", "M19 6.5v11", "M5 17.5a7 2.6 0 0 0 14 0"),
            thin = listOf("M12 6.5v11", "M12 6.5l6 1.6"), accent = setOf("M12 6.5l6 1.6"))
    }
    /** A globe of meridian and equator: spherical coordinates (ρ, θ, φ) in 3D. */
    val Spherical: ImageVector by lazy {
        icon("Spherical",
            stroke = listOf("M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0-17 0z"),
            thin = listOf("M3.5 12a8.5 3 0 1 0 17 0a8.5 3 0 1 0-17 0z", "M12 3.5a3.2 8.5 0 1 0 0 17a3.2 8.5 0 1 0 0-17z"), accent = setOf("M3.5 12a8.5 3 0 1 0 17 0a8.5 3 0 1 0-17 0z"))
    }
    /** Circles and rays from the origin: the polar grid. */
    val PolarGrid: ImageVector by lazy { icon("PolarGrid", stroke = listOf("M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0-17 0z"), thin = listOf("M8 12a4 4 0 1 0 8 0a4 4 0 1 0-8 0z", "M12 12H20.5", "M12 12L18 6", "M12 12V3.5", "M12 12L6 6"), fill = listOf("M10.8 12a1.2 1.2 0 1 0 2.4 0a1.2 1.2 0 1 0-2.4 0z"), accent = setOf("M8 12a4 4 0 1 0 8 0a4 4 0 1 0-8 0z", "M10.8 12a1.2 1.2 0 1 0 2.4 0a1.2 1.2 0 1 0-2.4 0z")) }
    /** Concentric rings: contours of |f|. */
    val Bands: ImageVector by lazy { icon("Bands", stroke = listOf("M9.5 12a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0z"), thin = listOf("M6 12a6 6 0 1 0 12 0a6 6 0 1 0-12 0z", "M2.5 12a9.5 9.5 0 1 0 19 0a9.5 9.5 0 1 0-19 0z"), accent = setOf("M9.5 12a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0z")) }
    /** Rays from a point: contours of arg f. */
    val Phase: ImageVector by lazy { icon("Phase", stroke = listOf("M12 12H21.5", "M12 12L16.75 3.77", "M12 12L7.25 3.77", "M12 12H2.5", "M12 12L7.25 20.23", "M12 12L16.75 20.23"), accent = setOf("M12 12H21.5", "M12 12L7.25 3.77", "M12 12L7.25 20.23")) }
    /** A bent grid: where Re f and Im f are whole numbers. */
    val Grid: ImageVector by lazy { icon("Grid", stroke = listOf("M3 8c6-3 12 3 18 0", "M3 16c6-3 12 3 18 0", "M8 3c-3 6 3 12 0 18", "M16 3c-3 6 3 12 0 18"), accent = setOf("M8 3c-3 6 3 12 0 18", "M16 3c-3 6 3 12 0 18")) }
    /** A compass drawing an arc between two marked points: geometry mode. */
    val Geometry: ImageVector by lazy {
        icon("Geometry",
            stroke = listOf("M12 6.2L6.6 20", "M12 6.2L16.4 17.4", "M12 2.8V4"),
            thin = listOf("M4 15.5A10 10 0 0 0 20 15.5"),
            fill = listOf("M10.3 5.4a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0-3.4 0z", "M2.6 15.5a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0-2.8 0z", "M18.6 15.5a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0-2.8 0z"), accent = setOf("M4 15.5A10 10 0 0 0 20 15.5", "M2.6 15.5a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0-2.8 0z", "M18.6 15.5a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0-2.8 0z"))
    }
    /** A loop with a counterclockwise arrow around a point: ∮ f dz. */
    val Loop: ImageVector by lazy { icon("Loop", stroke = listOf("M19 12a7 7 0 1 0-7 7", "M9.2 16.2L12 19l-2.8 2.8"), fill = listOf("M10.4 12a1.6 1.6 0 1 0 3.2 0a1.6 1.6 0 1 0-3.2 0z"), accent = setOf("M19 12a7 7 0 1 0-7 7", "M9.2 16.2L12 19l-2.8 2.8")) }
}
