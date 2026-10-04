package com.example.cas.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/*
 * Icons for the data table's tools, drawn like [TabIcons] on a 24×24 grid with 2 dp round
 * strokes (thin ones 1.6 dp), to sit alongside Material Symbols Rounded.
 */

private fun tableIcon(name: String, stroke: List<String>, thin: List<String> = emptyList(), fill: List<String> = emptyList(), shade: List<String> = emptyList()): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        val black = SolidColor(Color.Black)
        shade.forEach { addPath(PathParser().parsePathString(it).toNodes(), fill = black, fillAlpha = 0.35f) }
        stroke.forEach { addPath(PathParser().parsePathString(it).toNodes(), stroke = black, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) }
        thin.forEach { addPath(PathParser().parsePathString(it).toNodes(), stroke = black, strokeLineWidth = 1.6f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) }
        fill.forEach { addPath(PathParser().parsePathString(it).toNodes(), fill = black) }
    }.build()

/** An ellipse as a path (a digit 0, a dot), centered at ([x], [y]). */
private fun oval(x: Float, y: Float, rx: Float, ry: Float) = "M${x - rx} ${y}a$rx $ry 0 1 0 ${2 * rx} 0a$rx $ry 0 1 0 ${-2 * rx} 0z"

object TableIcons {
    /** Bars growing downwards and an arrow up: smallest first. */
    val SortUp: ImageVector by lazy { tableIcon("SortUp", stroke = listOf("M3.5 6h5", "M3.5 12h8", "M3.5 18h11", "M19 19V5", "M16 8l3-3l3 3")) }

    /** Bars shrinking downwards and an arrow down: largest first. */
    val SortDown: ImageVector by lazy { tableIcon("SortDown", stroke = listOf("M3.5 6h11", "M3.5 12h8", "M3.5 18h5", "M19 5v14", "M16 16l3 3l3-3")) }

    /** A funnel: a filter. */
    val Filter: ImageVector by lazy { tableIcon("Filter", stroke = listOf("M3.5 5h17l-6.5 8v5.5l-4 2.5v-8z")) }

    /** Points climbing evenly along a line: fill with a series. */
    val Series: ImageVector by lazy { tableIcon("Series", stroke = listOf(), thin = listOf("M3 20L21 4"), fill = listOf(oval(5f, 18.2f, 2f, 2f), oval(10f, 13.8f, 2f, 2f), oval(15f, 9.4f, 2f, 2f), oval(20f, 5f, 2f, 2f))) }

    /** 1, 2, 3 down a column: fill with whole numbers. */
    val Numbering: ImageVector by lazy { tableIcon("Numbering", stroke = listOf("M4 6.5l2-1.5v5", "M4 13.5c0-1 .8-1.6 1.8-1.6s1.7.7 1.7 1.5c0 1.6-3.5 2.5-3.5 4.6h3.6", "M11 7h9", "M11 12h9", "M11 17h9")) }

    /** ƒ with an arrow down: copy the column's formula down. */
    val FillFormula: ImageVector by lazy { tableIcon("FillFormula", stroke = listOf("M13 4.5c-2.6-.6-4 .6-4 3V20", "M6 11h6", "M18.5 8v11", "M16 16.5l2.5 2.5l2.5-2.5")) }

    /** A magnifier: find and replace. */
    val Find: ImageVector by lazy { tableIcon("Find", stroke = listOf(oval(10.5f, 10.5f, 6f, 6f), "M15 15l5.5 5.5"), thin = listOf("M8 10.5h5")) }

    /** Σ: a column's statistics, or a range's sum. */
    val Statistics: ImageVector by lazy { tableIcon("Statistics", stroke = listOf("M17.5 5H6.5l6 7l-6 7h11")) }

    /** A row added above a table. */
    val RowAbove: ImageVector by lazy { tableIcon("RowAbove", stroke = listOf("M12 2.5v6", "M9 5.5h6"), thin = listOf("M3.5 11h17v9.5h-17z", "M3.5 15.75h17", "M9.2 11v9.5", "M14.8 11v9.5")) }

    /** A row added below a table. */
    val RowBelow: ImageVector by lazy { tableIcon("RowBelow", stroke = listOf("M12 15.5v6", "M9 18.5h6"), thin = listOf("M3.5 3.5h17V13h-17z", "M3.5 8.25h17", "M9.2 3.5V13", "M14.8 3.5V13")) }

    /** A column added beside a table. */
    val ColumnAdd: ImageVector by lazy { tableIcon("ColumnAdd", stroke = listOf("M18 9v6", "M15 12h6"), thin = listOf("M3.5 3.5H12v17H3.5z", "M3.5 9.2H12", "M3.5 14.8H12", "M7.75 3.5v17")) }

    /** A table pasted from a clipboard. */
    val PasteTable: ImageVector by lazy { tableIcon("PasteTable", stroke = listOf("M8 4.5H6a1.5 1.5 0 0 0-1.5 1.5v13.5A1.5 1.5 0 0 0 6 21h12a1.5 1.5 0 0 0 1.5-1.5V6A1.5 1.5 0 0 0 18 4.5h-2", "M9 3h6v3H9z"), thin = listOf("M8 10.5h8v7H8z", "M8 14h8", "M12 10.5v7")) }

    /** Two rows alike, one taken out: remove duplicate rows. */
    val Dedupe: ImageVector by lazy { tableIcon("Dedupe", stroke = listOf("M3.5 5h17", "M3.5 10h17", "M15 15.5l5 5", "M20 15.5l-5 5"), thin = listOf("M3.5 18h8")) }

    /** Spaces pushed in from both ends: trim. */
    val Trim: ImageVector by lazy { tableIcon("Trim", stroke = listOf("M2.5 12h4", "M4.5 9.5L7 12l-2.5 2.5", "M21.5 12h-4", "M19.5 9.5L17 12l2.5 2.5"), thin = listOf("M10 8v8", "M14 8v8")) }

    /** A row turning into a column: swap rows and columns. */
    val Transpose: ImageVector by lazy { tableIcon("Transpose", stroke = listOf("M4 4h6v6H4z", "M13 7h4a3 3 0 0 1 3 3v6", "M17.5 13.5L20 16l2.5-2.5"), thin = listOf("M4 14h6v6H4z", "M14 14h6v6h-6z")) }

    /** A snowflake: freeze the first column. */
    val Freeze: ImageVector by lazy { tableIcon("Freeze", stroke = listOf("M12 3v18", "M4.2 7.5l15.6 9", "M4.2 16.5l15.6-9"), thin = listOf("M9.8 4.5L12 6.5l2.2-2", "M9.8 19.5L12 17.5l2.2 2", "M3.9 11l2.8-.5l-.9-2.8", "M20.1 13l-2.8.5l.9 2.8", "M5.8 16.3l.9-2.8l-2.8-.5", "M18.2 7.7l-.9 2.8l2.8.5")) }

    /** Rows with an empty one struck out: remove empty rows. */
    val RemoveEmpty: ImageVector by lazy { tableIcon("RemoveEmpty", stroke = listOf("M3.5 5h17", "M3.5 19h17"), thin = listOf("M3.5 10h17v4h-17z", "M9 9l6 6", "M15 9l-6 6")) }

    /** A copy of a column beside it. */
    val DuplicateColumn: ImageVector by lazy { tableIcon("DuplicateColumn", stroke = listOf("M4 3.5h6v17H4z"), thin = listOf("M14 3.5h6v17h-6z", "M14 9.2h6", "M14 14.8h6", "M4 9.2h6", "M4 14.8h6")) }

    /** A column moved left. */
    val MoveLeft: ImageVector by lazy { tableIcon("MoveLeft", stroke = listOf("M10 12H3", "M5.5 9.5L3 12l2.5 2.5"), thin = listOf("M13 3.5h7v17h-7z", "M13 9.2h7", "M13 14.8h7")) }

    /** A column moved right. */
    val MoveRight: ImageVector by lazy { tableIcon("MoveRight", stroke = listOf("M14 12h7", "M18.5 9.5L21 12l-2.5 2.5"), thin = listOf("M4 3.5h7v17H4z", "M4 9.2h7", "M4 14.8h7")) }

    /** A column wiped: clear it. */
    val ClearColumn: ImageVector by lazy { tableIcon("ClearColumn", stroke = listOf("M13 20.5h7.5", "M5.5 13.5l6-6l6 6l-5 5H8.5z"), thin = listOf("M8.5 10.5l6 6")) }

    /** A column with a cross: delete it. */
    val DeleteColumn: ImageVector by lazy { tableIcon("DeleteColumn", stroke = listOf("M15 10l6 6", "M21 10l-6 6"), thin = listOf("M3.5 3.5H11v17H3.5z", "M3.5 9.2H11", "M3.5 14.8H11")) }

    /** "A" and a spark: numbers as typed (automatic format). */
    val FormatAuto: ImageVector by lazy { tableIcon("FormatAuto", stroke = listOf("M3.5 19.5L8.5 5l5 14.5", "M5.4 14h6.2", "M18.5 3.5v5", "M16 6h5")) }

    /** 0.00: a fixed number of decimals. */
    val FormatFixed: ImageVector by lazy { tableIcon("FormatFixed", stroke = listOf(oval(5.5f, 12f, 2.5f, 5f), oval(14.2f, 12f, 2.2f, 4.5f), oval(20.2f, 12f, 2.2f, 4.5f)), fill = listOf(oval(9.7f, 16.4f, 1.3f, 1.3f))) }

    /** %: a percentage. */
    val Percent: ImageVector by lazy { tableIcon("Percent", stroke = listOf("M19 5L5 19"), thin = listOf(oval(7f, 7f, 2.6f, 2.6f), oval(17f, 17f, 2.6f, 2.6f))) }

    /** 10 with a power: scientific notation. */
    val Scientific: ImageVector by lazy { tableIcon("Scientific", stroke = listOf("M3.5 9.5l2.5-2V19", oval(12f, 13.2f, 2.8f, 5.8f)), thin = listOf("M17 10V6.6a1.8 1.8 0 0 1 3.6 0V10")) }

    /** 1 000: thousands grouped. */
    val Thousands: ImageVector by lazy { tableIcon("Thousands", stroke = listOf("M2.5 9l1.8-1.5V17"), thin = listOf(oval(9.3f, 12.3f, 1.9f, 4.4f), oval(14.3f, 12.3f, 1.9f, 4.4f), oval(19.3f, 12.3f, 1.9f, 4.4f))) }

    /** .0 and an arrow left: one decimal fewer. */
    val DecimalsLess: ImageVector by lazy { tableIcon("DecimalsLess", stroke = listOf(oval(9f, 15f, 2.3f, 4.5f), "M21 6h-7", "M16.5 3.5L14 6l2.5 2.5"), fill = listOf(oval(4f, 18.6f, 1.4f, 1.4f))) }

    /** .00 and an arrow right: one decimal more. */
    val DecimalsMore: ImageVector by lazy { tableIcon("DecimalsMore", stroke = listOf(oval(8.5f, 15f, 2.1f, 4.3f), oval(14.5f, 15f, 2.1f, 4.3f), "M14 6h7", "M18.5 3.5L21 6l-2.5 2.5"), fill = listOf(oval(3.6f, 18.6f, 1.4f, 1.4f))) }

    /** Three bands from light to dark: a color scale. */
    val ColorScale: ImageVector by lazy { tableIcon("ColorScale", stroke = listOf("M5 3.5h14a1.5 1.5 0 0 1 1.5 1.5v14a1.5 1.5 0 0 1-1.5 1.5H5A1.5 1.5 0 0 1 3.5 19V5A1.5 1.5 0 0 1 5 3.5z"), thin = listOf("M3.5 9.2h17", "M3.5 14.8h17"), shade = listOf("M3.5 9.2h17v5.6h-17z"), fill = listOf("M3.5 14.8h17V19a1.5 1.5 0 0 1-1.5 1.5H5A1.5 1.5 0 0 1 3.5 19z")) }

    /** CSV text going out: share the table. */
    val ShareTable: ImageVector by lazy { tableIcon("ShareTable", stroke = listOf("M12 14V3.5", "M8.5 7L12 3.5L15.5 7", "M5 11v8.5a1.5 1.5 0 0 0 1.5 1.5h11a1.5 1.5 0 0 0 1.5-1.5V11")) }

    /** Two cards, one over the other: copy. */
    val CopyTable: ImageVector by lazy { tableIcon("CopyTable", stroke = listOf("M8.5 8.5h11v12h-11z"), thin = listOf("M5.5 15.5h-1v-12h11v1", "M8.5 12.5h11", "M14 8.5v12")) }

    /** Σ over a column of cells: AutoSum. */
    val AutoSum: ImageVector by lazy { tableIcon("AutoSum", stroke = listOf("M13.5 4H4.5l4.5 6l-4.5 6h9"), thin = listOf("M17 4h4.5v4h-4.5z", "M17 10h4.5v4h-4.5z", "M17 16h4.5v4h-4.5z"), fill = listOf("M17 16h4.5v4h-4.5z")) }

    /** Cells with bars of different lengths inside: data bars. */
    val DataBars: ImageVector by lazy { tableIcon("DataBars", stroke = listOf("M3.5 3.5h17v17h-17z"), thin = listOf("M3.5 9.2h17", "M3.5 14.8h17"), fill = listOf("M5 5h6v2.7H5z", "M5 10.7h12.5v2.7H5z", "M5 16.3h9v2.7H5z")) }

    /** A highlighter over a cell: highlight cells by a rule. */
    val Highlight: ImageVector by lazy { tableIcon("Highlight", stroke = listOf("M14.5 3.5l6 6l-8 8h-6v-6z", "M3 21h18"), thin = listOf("M11 7l6 6"), shade = listOf("M6.5 11.5l6 6h-6z")) }

    /** A light bulb over a little chart: insights about a column. */
    val Insights: ImageVector by lazy { tableIcon("Insights", stroke = listOf("M12 3a6 6 0 0 0-3.5 10.9V16h7v-2.1A6 6 0 0 0 12 3z", "M9.5 19.5h5", "M10.5 22h3"), thin = listOf("M9.5 10.5l1.5-1.5l1.5 1.5l2-2.5")) }
}
