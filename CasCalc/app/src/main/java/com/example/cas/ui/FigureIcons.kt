package com.example.cas.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.example.cas.graph.Figure

/*
 * Icons for the Plots mode and each of its layer types, drawn like [TableIcons] on a 24×24 grid
 * with round 2 dp strokes, two-tone: the axes and what's measured in ink, the mark the layer
 * makes (the curve, the bars, the bands) in the accent color.
 */

private fun dot(x: Float, y: Float, r: Float = 1.6f) = "M${x - r} ${y}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

/** Thin axes, bottom left. */
private const val AXES = "M3.5 3v17.5h17.5"

object FigureIcons {
    /** Bars with a curve over them: the Plots mode. */
    val Mode: ImageVector by lazy { tableIcon("PlotsMode", stroke = listOf("M3.5 20.5h17", "M6 20.5v-5", "M10.5 20.5v-8.5", "M15 20.5v-6", "M19.5 20.5v-3", "M3.5 10c2.5-3.5 5-5.5 8-5.5s5 2.5 9 6"), accent = setOf("M3.5 10c2.5-3.5 5-5.5 8-5.5s5 2.5 9 6")) }

    /** A new, empty figure: a frame with a plus. */
    val NewFigure: ImageVector by lazy { tableIcon("NewFigure", stroke = listOf("M4 5.5h16v13H4z", "M12 9v6", "M9 12h6"), accent = setOf("M12 9v6", "M9 12h6")) }

    /** Python's code brackets: a script. */
    val Python: ImageVector by lazy { tableIcon("Python", stroke = listOf("M8 7l-5 5 5 5", "M16 7l5 5-5 5", "M13.5 5l-3 14"), accent = setOf("M13.5 5l-3 14")) }

    /** A frame split into cells: the layout. */
    val Layout: ImageVector by lazy { tableIcon("FigureLayout", stroke = listOf("M3.5 4.5h17v15h-17z"), thin = listOf("M14 4.5v15", "M3.5 12h10.5"), shade = listOf("M3.5 4.5H14V12H3.5z"), accent = setOf("M3.5 4.5H14V12H3.5z")) }

    /** Axes with ticks: the axes' settings. */
    val Axes: ImageVector by lazy { tableIcon("FigureAxes", stroke = listOf("M4.5 3.5v16h16"), thin = listOf("M4.5 15.5h-2", "M4.5 11.5h-2", "M4.5 7.5h-2", "M8.5 19.5v2", "M12.5 19.5v2", "M16.5 19.5v2"), fill = listOf(dot(9f, 13f), dot(13f, 9f), dot(17f, 6f)), accent = setOf(dot(9f, 13f), dot(13f, 9f), dot(17f, 6f))) }

    /** Open eye / closed eye: shown, hidden. */
    val Shown: ImageVector by lazy { tableIcon("LayerShown", stroke = listOf("M2.5 12s3.5-6.5 9.5-6.5S21.5 12 21.5 12s-3.5 6.5-9.5 6.5S2.5 12 2.5 12z"), fill = listOf(dot(12f, 12f, 2.8f)), accent = setOf(dot(12f, 12f, 2.8f))) }
    val Hidden: ImageVector by lazy { tableIcon("LayerHidden", stroke = listOf("M2.5 12s3.5-6.5 9.5-6.5S21.5 12 21.5 12s-3.5 6.5-9.5 6.5S2.5 12 2.5 12z", "M4 20L20 4"), accent = setOf("M4 20L20 4")) }

    private val icons = HashMap<Figure.Kind, ImageVector>()

    /** The icon for a layer type. */
    fun of(kind: Figure.Kind): ImageVector = icons.getOrPut(kind) { make(kind) }

    private fun make(k: Figure.Kind): ImageVector {
        val n = "Plot" + k.name
        return when (k) {
            Figure.Kind.Plot -> tableIcon(n, stroke = listOf("M5.5 15.5c2.5 0 3-9 6-9s3.5 7.5 8 5"), thin = listOf(AXES), accent = setOf("M5.5 15.5c2.5 0 3-9 6-9s3.5 7.5 8 5"))
            Figure.Kind.Scatter -> {
                val d = listOf(dot(7f, 16f), dot(10f, 12.5f), dot(13.5f, 14f), dot(15.5f, 9f), dot(18.5f, 6.5f), dot(11.5f, 7.5f))
                tableIcon(n, stroke = emptyList(), thin = listOf(AXES), fill = d, accent = d.toSet())
            }
            Figure.Kind.Step -> tableIcon(n, stroke = listOf("M5.5 17h3.5v-5h4v-4h3.5V5h3"), thin = listOf(AXES), accent = setOf("M5.5 17h3.5v-5h4v-4h3.5V5h3"))
            Figure.Kind.Stem -> {
                val tips = listOf(dot(7f, 7.5f), dot(11f, 17f), dot(15f, 5.5f), dot(19f, 15.5f))
                tableIcon(n, stroke = listOf("M7 12V7.5", "M11 12v5", "M15 12V5.5", "M19 12v3.5"), thin = listOf("M3.5 12h17.5"), fill = tips, accent = tips.toSet())
            }
            Figure.Kind.FillBetween -> tableIcon(n, stroke = listOf("M5 9c3-3 6 2 9-1s4-3 6-2", "M5 16c3-2 6 1 9-1s4-2 6-1"), thin = listOf(AXES), shade = listOf("M5 9c3-3 6 2 9-1s4-3 6-2v10c-2-1-3-1-6 1s-6-1-9 1z"), accent = setOf("M5 9c3-3 6 2 9-1s4-3 6-2v10c-2-1-3-1-6 1s-6-1-9 1z"))
            Figure.Kind.Stack -> tableIcon(n, stroke = listOf("M4.5 13c3-2 5 1 8-1s5-3 8-1", "M4.5 8c3-3 5 1 8-1.5s5-2 8-.5"), thin = listOf(AXES), shade = listOf("M4.5 13c3-2 5 1 8-1s5-3 8-1v8.5h-16z"), accent = setOf("M4.5 13c3-2 5 1 8-1s5-3 8-1v8.5h-16z", "M4.5 8c3-3 5 1 8-1.5s5-2 8-.5"))
            Figure.Kind.Parametric -> tableIcon(n, stroke = listOf("M12 12c2.5-4 7.5-4 7.5 0s-5 4-7.5 0-7.5-4-7.5 0 5 4 7.5 0z"), accent = setOf("M12 12c2.5-4 7.5-4 7.5 0s-5 4-7.5 0-7.5-4-7.5 0 5 4 7.5 0z"))
            Figure.Kind.Polar -> tableIcon(n, stroke = listOf("M15.3 12L15.12 10.66L14.61 9.41L13.8 8.36L12.78 7.57L11.62 7.09L10.43 6.93L9.29 7.08L8.29 7.49L7.5 8.1L6.94 8.83L6.62 9.59L6.53 10.31L6.6 10.93L6.8 11.41L7.05 11.74L7.28 11.92L7.5 12L7.28 12.08L7.05 12.26L6.8 12.59L6.6 13.07L6.53 13.69L6.62 14.41L6.94 15.17L7.5 15.9L8.29 16.51L9.29 16.92L10.42 17.07L11.62 16.91L12.78 16.43L13.8 15.64L14.61 14.59L15.12 13.34z"), thin = listOf("M7.5 3.5v17", "M3 12h18", "M7.5 12m-6 0a6 6 0 1 0 12 0a6 6 0 1 0-12 0"), accent = setOf("M15.3 12L15.12 10.66L14.61 9.41L13.8 8.36L12.78 7.57L11.62 7.09L10.43 6.93L9.29 7.08L8.29 7.49L7.5 8.1L6.94 8.83L6.62 9.59L6.53 10.31L6.6 10.93L6.8 11.41L7.05 11.74L7.28 11.92L7.5 12L7.28 12.08L7.05 12.26L6.8 12.59L6.6 13.07L6.53 13.69L6.62 14.41L6.94 15.17L7.5 15.9L8.29 16.51L9.29 16.92L10.42 17.07L11.62 16.91L12.78 16.43L13.8 15.64L14.61 14.59L15.12 13.34z"))
            Figure.Kind.Fill -> tableIcon(n, stroke = listOf("M12 3.5l2.5 5.5 6 .7-4.5 4 1.3 6-5.3-3-5.3 3 1.3-6-4.5-4 6-.7z"), shade = listOf("M12 3.5l2.5 5.5 6 .7-4.5 4 1.3 6-5.3-3-5.3 3 1.3-6-4.5-4 6-.7z"), accent = setOf("M12 3.5l2.5 5.5 6 .7-4.5 4 1.3 6-5.3-3-5.3 3 1.3-6-4.5-4 6-.7z"))
            Figure.Kind.Hist -> tableIcon(n, stroke = listOf("M4.5 20.5v-5h3v5", "M7.5 20.5V9h3v11.5", "M10.5 20.5V5h3v15.5", "M13.5 20.5V10h3v10.5", "M16.5 20.5V15h3v5.5"), thin = listOf("M3 20.5h18"), shade = listOf("M10.5 5h3v15.5h-3z"), accent = setOf("M10.5 5h3v15.5h-3z", "M10.5 20.5V5h3v15.5"))
            Figure.Kind.Box -> tableIcon(n, stroke = listOf("M7.5 8h9v8h-9z", "M12 8V4M12 16v4", "M10 4h4M10 20h4"), thin = emptyList(), shade = listOf("M7.5 8h9v8h-9z"), fill = listOf("M7.5 11.25h9v1.5h-9z"), accent = setOf("M7.5 8h9v8h-9z", "M7.5 11.25h9v1.5h-9z"))
            Figure.Kind.Violin -> tableIcon(n, stroke = listOf("M12 3.5c-1 3-5 4.5-5 8.5s3 4 4 6l1 2.5 1-2.5c1-2 4-2 4-6s-4-5.5-5-8.5z", "M9.5 12.5h5"), shade = listOf("M12 3.5c-1 3-5 4.5-5 8.5s3 4 4 6l1 2.5 1-2.5c1-2 4-2 4-6s-4-5.5-5-8.5z"), accent = setOf("M12 3.5c-1 3-5 4.5-5 8.5s3 4 4 6l1 2.5 1-2.5c1-2 4-2 4-6s-4-5.5-5-8.5z"))
            Figure.Kind.Ecdf -> tableIcon(n, stroke = listOf("M5 18.5h3v-2h2.5v-3h2v-4h2v-3h2.5v-2h3"), thin = listOf(AXES), accent = setOf("M5 18.5h3v-2h2.5v-3h2v-4h2v-3h2.5v-2h3"))
            Figure.Kind.Hist2D -> tableIcon(n, stroke = listOf("M4 4h16v16H4z"), thin = listOf("M9.33 4v16", "M14.67 4v16", "M4 9.33h16", "M4 14.67h16"), shade = listOf("M9.33 9.33h5.34v5.34H9.33z", "M14.67 4H20v5.33h-5.33z", "M4 14.67h5.33V20H4z"), fill = listOf("M9.33 9.33h5.34v5.34H9.33z"), accent = setOf("M9.33 9.33h5.34v5.34H9.33z", "M14.67 4H20v5.33h-5.33z", "M4 14.67h5.33V20H4z"))
            Figure.Kind.Hexbin -> {
                fun hex(cx: Float, cy: Float, r: Float = 3.4f) = (0 until 6).joinToString("", "M", "z") { i -> val a = Math.PI / 6 + i * Math.PI / 3; "${if (i > 0) "L" else ""}${"%.2f".format(java.util.Locale.ROOT, cx + r * Math.cos(a))} ${"%.2f".format(java.util.Locale.ROOT, cy + r * Math.sin(a))}" }
                val a = hex(8.6f, 8.5f); val b = hex(14.5f, 8.5f); val c = hex(11.55f, 13.6f); val d = hex(17.45f, 13.6f); val e = hex(5.65f, 13.6f)
                tableIcon(n, stroke = listOf(a, b, e), thin = emptyList(), shade = listOf(c, d), fill = listOf(c), accent = setOf(c, d))
            }
            Figure.Kind.Bar -> tableIcon(n, stroke = listOf("M5 20.5V12h3.5v8.5", "M10.25 20.5V6h3.5v14.5", "M15.5 20.5V9.5H19v11"), thin = listOf("M3 20.5h18"), shade = listOf("M10.25 6h3.5v14.5h-3.5z"), accent = setOf("M10.25 20.5V6h3.5v14.5", "M10.25 6h3.5v14.5h-3.5z"))
            Figure.Kind.BarH -> tableIcon(n, stroke = listOf("M3.5 5h8.5v3.5H3.5", "M3.5 10.25H18v3.5H3.5", "M3.5 15.5H14V19H3.5"), thin = listOf("M3.5 3v18"), shade = listOf("M3.5 10.25H18v3.5H3.5z"), accent = setOf("M3.5 10.25H18v3.5H3.5", "M3.5 10.25H18v3.5H3.5z"))
            Figure.Kind.Pie -> tableIcon(n, stroke = listOf("M12 3.5a8.5 8.5 0 1 0 8.5 8.5H12z", "M14.5 2.5v7h7a7 7 0 0 0-7-7z"), shade = listOf("M14.5 2.5v7h7a7 7 0 0 0-7-7z"), accent = setOf("M14.5 2.5v7h7a7 7 0 0 0-7-7z"))
            Figure.Kind.Events -> tableIcon(n, stroke = listOf("M5 4.5v5", "M8 4.5v5", "M13 4.5v5", "M18.5 4.5v5", "M6.5 14.5v5", "M11 14.5v5", "M12.5 14.5v5", "M17 14.5v5"), accent = setOf("M6.5 14.5v5", "M11 14.5v5", "M12.5 14.5v5", "M17 14.5v5"))
            Figure.Kind.Contour -> tableIcon(n, stroke = listOf("M12 4c5 0 8.5 3.5 8.5 8s-4 7.5-8.5 7.5S3.5 16.5 3.5 12 7 4 12 4z", "M12 8c3 0 5 1.8 5 4s-2.3 3.8-5 3.8-5-1.6-5-3.8S9 8 12 8z"), fill = listOf(dot(12f, 12f, 1.4f)), accent = setOf("M12 8c3 0 5 1.8 5 4s-2.3 3.8-5 3.8-5-1.6-5-3.8S9 8 12 8z", dot(12f, 12f, 1.4f)))
            Figure.Kind.ContourF -> tableIcon(n, stroke = listOf("M3.5 3.5h17v17h-17z"), shade = listOf("M12 5.5c4.5 0 7 3 7 6.5s-3 6.5-7 6.5-7-3-7-6.5 2.5-6.5 7-6.5z", "M12 9c2.5 0 4 1.3 4 3s-1.8 3-4 3-4-1.3-4-3 1.5-3 4-3z"), fill = listOf("M12 9c2.5 0 4 1.3 4 3s-1.8 3-4 3-4-1.3-4-3 1.5-3 4-3z"), accent = setOf("M12 5.5c4.5 0 7 3 7 6.5s-3 6.5-7 6.5-7-3-7-6.5 2.5-6.5 7-6.5z", "M12 9c2.5 0 4 1.3 4 3s-1.8 3-4 3-4-1.3-4-3 1.5-3 4-3z"))
            Figure.Kind.Heatmap -> tableIcon(n, stroke = listOf("M4 4h16v16H4z"), thin = listOf("M9.33 4v16", "M14.67 4v16", "M4 9.33h16", "M4 14.67h16"), shade = listOf("M4 4h5.33v5.33H4z", "M9.33 9.33h5.34v5.34H9.33z", "M14.67 14.67H20V20h-5.33z", "M9.33 4h5.34v5.33H9.33z"), fill = listOf("M14.67 14.67H20V20h-5.33z", "M4 4h5.33v5.33H4z"), accent = setOf("M14.67 14.67H20V20h-5.33z", "M4 4h5.33v5.33H4z", "M9.33 9.33h5.34v5.34H9.33z", "M9.33 4h5.34v5.33H9.33z"))
            Figure.Kind.Quiver -> tableIcon(n, stroke = listOf("M4.5 9l4-3M6.5 5.8l2 .2-.4 2", "M13 7.5l5-1M16.2 5l1.8 1.5-1.4 1.6", "M5 17.5l4.5 1M8 16.7l1.5 1.8-1.7 1.1", "M13.5 18l4.5-4M16.2 13.8l1.8.2v1.9"), accent = setOf("M13 7.5l5-1M16.2 5l1.8 1.5-1.4 1.6", "M13.5 18l4.5-4M16.2 13.8l1.8.2v1.9"))
            Figure.Kind.Stream -> tableIcon(n, stroke = listOf("M3.5 7c5-3 9 3 17 0", "M3.5 12c5-3 9 3 17 0", "M3.5 17c5-3 9 3 17 0", "M10.5 6.5l1.5.9-1.2 1.3", "M10.5 16.5l1.5.9-1.2 1.3"), accent = setOf("M3.5 12c5-3 9 3 17 0"))
            Figure.Kind.HLine -> tableIcon(n, stroke = listOf("M3.5 11h17.5"), thin = listOf(AXES, "M5.5 17c3-6 6-9 9-7s3 2 5-1"), accent = setOf("M3.5 11h17.5"))
            Figure.Kind.VLine -> tableIcon(n, stroke = listOf("M13 3v17.5"), thin = listOf(AXES, "M5.5 17c3-6 6-9 9-7s3 2 5-1"), accent = setOf("M13 3v17.5"))
            Figure.Kind.HSpan -> tableIcon(n, stroke = listOf("M3.5 8.5h17.5", "M3.5 13.5h17.5"), thin = listOf(AXES), shade = listOf("M3.5 8.5H21v5H3.5z"), accent = setOf("M3.5 8.5H21v5H3.5z", "M3.5 8.5h17.5", "M3.5 13.5h17.5"))
            Figure.Kind.VSpan -> tableIcon(n, stroke = listOf("M10 3v17.5", "M15 3v17.5"), thin = listOf(AXES), shade = listOf("M10 3h5v17.5h-5z"), accent = setOf("M10 3h5v17.5h-5z", "M10 3v17.5", "M15 3v17.5"))
            Figure.Kind.Text -> tableIcon(n, stroke = listOf("M6 6V4.5h12V6", "M12 4.5v15", "M9.5 19.5h5"), accent = setOf("M12 4.5v15"))
            Figure.Kind.Fit -> {
                val d = listOf(dot(6.5f, 16.5f, 1.4f), dot(9.5f, 12f, 1.4f), dot(12.5f, 13f, 1.4f), dot(15.5f, 8.5f, 1.4f), dot(18.5f, 7.5f, 1.4f))
                tableIcon(n, stroke = listOf("M4.5 18.5L20 5.5"), thin = listOf(AXES), fill = d, accent = setOf("M4.5 18.5L20 5.5"))
            }
            Figure.Kind.Kde -> tableIcon(n, stroke = listOf("M4 19c3 0 4-11 7-11s3 6 5 7 3 4 5 4"), thin = listOf("M3 20.5h18"), shade = listOf("M4 19c3 0 4-11 7-11s3 6 5 7 3 4 5 4v1.5H4z"), accent = setOf("M4 19c3 0 4-11 7-11s3 6 5 7 3 4 5 4", "M4 19c3 0 4-11 7-11s3 6 5 7 3 4 5 4v1.5H4z"))
            Figure.Kind.Annotate -> tableIcon(n, stroke = listOf("M13 11L5.5 18.5", "M5.5 13.5v5h5"), thin = listOf("M14 6h7", "M14 9.5h5"), accent = setOf("M13 11L5.5 18.5", "M5.5 13.5v5h5"))
        }
    }
}
