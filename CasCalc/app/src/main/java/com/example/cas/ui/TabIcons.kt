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

private fun icon(name: String, stroke: List<String>, thin: List<String> = emptyList(), fill: List<String> = emptyList(), shade: List<String> = emptyList()): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        val black = SolidColor(Color.Black)
        shade.forEach { addPath(PathParser().parsePathString(it).toNodes(), fill = black, fillAlpha = 0.35f) }
        stroke.forEach { addPath(PathParser().parsePathString(it).toNodes(), stroke = black, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) }
        thin.forEach { addPath(PathParser().parsePathString(it).toNodes(), stroke = black, strokeLineWidth = 1.6f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) }
        fill.forEach { addPath(PathParser().parsePathString(it).toNodes(), fill = black) }
    }.build()


object TabIcons {
    /** √x: powers, roots and logarithms. */
    val Roots: ImageVector by lazy { icon("Roots", stroke = listOf("M3 13l2.5-1.5L8.5 19L12.5 5H21", "M14.5 10l4.5 5.5M19 10l-4.5 5.5")) }

    /** A right triangle with its angle marked: trigonometry. */
    val Triangle: ImageVector by lazy { icon("Triangle", stroke = listOf("M4 19H20V5Z"), thin = listOf("M9.5 19A5.5 5.5 0 0 0 8.1 15.4", "M17 19V16H20")) }

    /** A balance, both sides of an equation: algebra. */
    val Balance: ImageVector by lazy { icon("Balance", stroke = listOf("M12 3.5V20", "M7.5 20h9", "M4 7.5h16", "M4 7.5L1.8 13", "M4 7.5L6.2 13", "M20 7.5L17.8 13", "M20 7.5L22.2 13"), fill = listOf("M1.2 13h5.6a2.8 2.4 0 0 1-5.6 0z", "M17.2 13h5.6a2.8 2.4 0 0 1-5.6 0z")) }

    /** The area under a curve: calculus. */
    val Area: ImageVector by lazy { icon("Area", stroke = listOf("M3 20h18", "M3 17C6.5 17 8.5 6 12 6s5 7.5 9 6"), shade = listOf("M7.5 20v-6.3C9 9.5 10.3 6 12 6s3.2 3.6 5 5.6V20z")) }

    /** ∇, nabla: vector calculus. */
    val Nabla: ImageVector by lazy { icon("Nabla", stroke = listOf("M3.5 5h17L12 20.5z"), thin = listOf("M7.2 6.8l5.6 10.2")) }

    /** A 2×2 matrix: linear algebra. */
    val Matrix: ImageVector by lazy { icon("Matrix", stroke = listOf("M7 4H4v16h3", "M17 4h3v16h-3"), fill = listOf("M7.5 9a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0-3.4 0z", "M13.1 9a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0-3.4 0z", "M7.5 15a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0-3.4 0z", "M13.1 15a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0-3.4 0z")) }

    /** An atom: physical constants. */
    val Atom: ImageVector by lazy { icon("Atom", stroke = listOf(), thin = listOf("M2.5 12a9.5 3.6 0 1 0 19 0a9.5 3.6 0 1 0-19 0z", "M7.25 3.77A9.5 3.6 60 1 0 16.75 20.23A9.5 3.6 60 1 0 7.25 3.77z", "M7.25 20.23A9.5 3.6 -60 1 0 16.75 3.77A9.5 3.6 -60 1 0 7.25 20.23z"), fill = listOf("M10.2 12a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0-3.6 0z")) }

    /** |∨|, the graph of an absolute value: more functions. */
    val Abs: ImageVector by lazy { icon("Abs", stroke = listOf("M3 4v16", "M21 4v16", "M7 7l5 9.5L17 7")) }

    /** An Argand diagram, z as a vector from 0 with its angle: the complex-plot mode. */
    val Complex: ImageVector by lazy { icon("Complex", stroke = listOf("M4 20H21", "M4 20V3", "M4 20L15.5 8.5"), thin = listOf("M9.2 20A5.2 5.2 0 0 0 7.68 16.32"), fill = listOf("M13.4 8.5a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0-4.2 0z")) }

    /** ℂ, a double-struck C: the complex-numbers tab. */
    val ComplexC: ImageVector by lazy { icon("ComplexC", stroke = listOf("M18.5 7.2A7.8 7.8 0 1 0 18.5 16.8"), thin = listOf("M9 5.6V18.4")) }

    /** A bell curve over a small histogram: statistics. */
    val Stats: ImageVector by lazy { icon("Stats", stroke = listOf("M3 20.5H21", "M3 18.5c3.5 0 5-12.5 9-12.5s5.5 12.5 9 12.5"), fill = listOf("M7 20.5v-5.5h2.2v5.5z", "M10.9 20.5V10h2.2v10.5z", "M14.8 20.5v-5.5H17v5.5z")) }

    /** The symbol builder: an x with a hat, a superscript box and a +. */
    val SymbolBuilder: ImageVector by lazy { icon("SymbolBuilder", stroke = listOf("M4 10.5l7 9.5", "M11 10.5l-7 9.5", "M4.5 7.5l3-3l3 3"), thin = listOf("M14 3.5h5v5h-5z", "M15 17.5h6", "M18 14.5v6")) }

    /** Aα: Latin and Greek letters. */
    val Letters: ImageVector by lazy { icon("Letters", stroke = listOf("M2.5 19L7 5l4.5 14", "M4.2 14h5.6", "M22 11.5c-.8 3-2.2 7.5-4.3 7.5c-1.9 0-3.2-1.6-3.2-3.8c0-2.2 1.4-3.9 3.3-3.9c2.6 0 3.2 5.1 4.7 7.7")) }
}

/** Toolbar icons for the complex plot, in the same style. */
object PlotIcons {
    /** Circles and rays from the origin: the polar grid. */
    val PolarGrid: ImageVector by lazy { icon("PolarGrid", stroke = listOf("M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0-17 0z"), thin = listOf("M8 12a4 4 0 1 0 8 0a4 4 0 1 0-8 0z", "M12 12H20.5", "M12 12L18 6", "M12 12V3.5", "M12 12L6 6"), fill = listOf("M10.8 12a1.2 1.2 0 1 0 2.4 0a1.2 1.2 0 1 0-2.4 0z")) }
    /** Concentric rings: contours of |f|. */
    val Bands: ImageVector by lazy { icon("Bands", stroke = listOf("M9.5 12a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0z"), thin = listOf("M6 12a6 6 0 1 0 12 0a6 6 0 1 0-12 0z", "M2.5 12a9.5 9.5 0 1 0 19 0a9.5 9.5 0 1 0-19 0z")) }
    /** Rays from a point: contours of arg f. */
    val Phase: ImageVector by lazy { icon("Phase", stroke = listOf("M12 12H21.5", "M12 12L16.75 3.77", "M12 12L7.25 3.77", "M12 12H2.5", "M12 12L7.25 20.23", "M12 12L16.75 20.23")) }
    /** A bent grid: where Re f and Im f are whole numbers. */
    val Grid: ImageVector by lazy { icon("Grid", stroke = listOf("M3 8c6-3 12 3 18 0", "M3 16c6-3 12 3 18 0", "M8 3c-3 6 3 12 0 18", "M16 3c-3 6 3 12 0 18")) }
    /** A loop with an anticlockwise arrow around a point: ∮ f dz. */
    val Loop: ImageVector by lazy { icon("Loop", stroke = listOf("M19 12a7 7 0 1 0-7 7", "M9.2 16.2L12 19l-2.8 2.8"), fill = listOf("M10.4 12a1.6 1.6 0 1 0 3.2 0a1.6 1.6 0 1 0-3.2 0z")) }
}
