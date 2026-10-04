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

    /** Scissors: cut. */
    val Cut: ImageVector by lazy { tableIcon("Cut", stroke = listOf(oval(6.5f, 17.5f, 3f, 3f), oval(17.5f, 17.5f, 3f, 3f), "M8.6 15.4L18.5 3.5", "M15.4 15.4L5.5 3.5")) }

    /** Lines flush left: align left. */
    val AlignLeft: ImageVector by lazy { tableIcon("AlignLeft", stroke = listOf("M4 5h16", "M4 10h10", "M4 15h16", "M4 20h10")) }

    /** Lines centered: align center. */
    val AlignCenter: ImageVector by lazy { tableIcon("AlignCenter", stroke = listOf("M4 5h16", "M7 10h10", "M4 15h16", "M7 20h10")) }

    /** Lines flush right: align right. */
    val AlignRight: ImageVector by lazy { tableIcon("AlignRight", stroke = listOf("M4 5h16", "M10 10h10", "M4 15h16", "M10 20h10")) }

    /** A grid with some cells tinted and a bar: conditional formatting. */
    val ConditionalFormat: ImageVector by lazy { tableIcon("ConditionalFormat", stroke = listOf("M3.5 3.5h17v17h-17z"), thin = listOf("M3.5 9.2h17", "M3.5 14.8h17", "M12 3.5v17"), shade = listOf("M12 3.5h8.5v5.7H12z"), fill = listOf("M3.5 14.8H12v5.7H3.5z", "M13.5 10.7h4.5v2.6h-4.5z")) }

    /** A grid with a cell added: insert cells. */
    val InsertCells: ImageVector by lazy { tableIcon("InsertCells", stroke = listOf("M17.5 14v7", "M14 17.5h7"), thin = listOf("M3.5 3.5h13v10h-13z", "M3.5 8.5h13", "M10 3.5v10")) }

    /** A grid with a cell taken away: delete cells. */
    val DeleteCells: ImageVector by lazy { tableIcon("DeleteCells", stroke = listOf("M14.5 15l6 6", "M20.5 15l-6 6"), thin = listOf("M3.5 3.5h13v10h-13z", "M3.5 8.5h13", "M10 3.5v10")) }

    /** An eraser: clear. */
    val Eraser: ImageVector by lazy { tableIcon("Eraser", stroke = listOf("M8 20.5h12.5", "M3.8 14.7l9.9-9.9a1.5 1.5 0 0 1 2.1 0l4.4 4.4a1.5 1.5 0 0 1 0 2.1L12 19.5H8.5z"), thin = listOf("M8.3 10.2l6.5 6.5")) }

    /** Sort levels one under another: custom sort. */
    val CustomSort: ImageVector by lazy { tableIcon("CustomSort", stroke = listOf("M3.5 5h9", "M6.5 10.5h9", "M9.5 16h9", "M19 3.5v6", "M17 7.5l2 2l2-2"), thin = listOf("M3.5 5v11h6")) }

    /** One column splitting into two: text to columns. */
    val TextToColumns: ImageVector by lazy { tableIcon("TextToColumns", stroke = listOf("M2.5 9.5h6v5h-6z", "M11 12h3", "M12.5 10.5L14 12l-1.5 1.5"), thin = listOf("M16 5h5.5v14H16z", "M16 9.5h5.5", "M16 14.5h5.5")) }

    /** ƒx over cells: show formulas. */
    val ShowFormulas: ImageVector by lazy { tableIcon("ShowFormulas", stroke = listOf("M10 3.5c-2-.5-3.2.5-3.2 2.6V14", "M4.5 8.5h5", "M12.5 8l4 5", "M16.5 8l-4 5"), thin = listOf("M3.5 17.5h17v3.5h-17z")) }

    /** A magnifier with +: zoom in. */
    val ZoomIn: ImageVector by lazy { tableIcon("ZoomIn", stroke = listOf(oval(10.5f, 10.5f, 6.5f, 6.5f), "M15.3 15.3l5.2 5.2", "M7.5 10.5h6", "M10.5 7.5v6")) }

    /** A magnifier with −: zoom out. */
    val ZoomOut: ImageVector by lazy { tableIcon("ZoomOut", stroke = listOf(oval(10.5f, 10.5f, 6.5f, 6.5f), "M15.3 15.3l5.2 5.2", "M7.5 10.5h6")) }

    /** 1:1 in a frame: zoom back to 100%. */
    val Zoom100: ImageVector by lazy { tableIcon("Zoom100", stroke = listOf("M7 9.5l1.5-1V16", "M15.5 9.5l1.5-1V16"), thin = listOf("M3.5 4.5h17v15h-17z"), fill = listOf(oval(12f, 10.5f, 1f, 1f), oval(12f, 14f, 1f, 1f))) }

    /** ƒx: insert a function. */
    val InsertFunction: ImageVector by lazy { tableIcon("InsertFunction", stroke = listOf("M11.5 4c-2.4-.6-3.8.6-3.8 3V20", "M4.5 10h6.5", "M13.5 10l6 8", "M19.5 10l-6 8")) }

    /** The little arrow at a ribbon group's corner: more options. */
    val Launcher: ImageVector by lazy { tableIcon("Launcher", stroke = listOf("M9 15l8-8", "M11.5 7H17v5.5"), thin = listOf("M5 9V19h10")) }

    /** π: math and trigonometry functions. */
    val MathFunctions: ImageVector by lazy { tableIcon("MathFunctions", stroke = listOf("M4 7.5h16", "M8.5 7.5c0 5-1 9.5-3 12", "M15 7.5v9.5c0 1.5.8 2.5 2.2 2.5c.9 0 1.6-.3 2.3-.9")) }

    /** A fork in a path: logical functions (IF). */
    val LogicFunctions: ImageVector by lazy { tableIcon("LogicFunctions", stroke = listOf("M12 21v-8", "M12 13L6 6", "M12 13l6-7", "M3.5 7.5L6 6l1 2.8", "M20.5 7.5L18 6l-1 2.8")) }

    /** "Aa": text functions. */
    val TextFunctions: ImageVector by lazy { tableIcon("TextFunctions", stroke = listOf("M2.5 19L7 5l4.5 14", "M4.2 14h5.6", "M21.5 19v-6.5a3 3 0 0 0-5.5-1.6", "M21.5 15.5c-3.5-.6-6.5 0-6.5 2a2 2 0 0 0 3.7 1")) }

    /** A calendar: date and time functions. */
    val DateFunctions: ImageVector by lazy { tableIcon("DateFunctions", stroke = listOf("M4.5 6h15v14.5h-15z", "M8 3.5v4", "M16 3.5v4"), thin = listOf("M4.5 10.5h15"), fill = listOf("M8 13h3v3H8z")) }

    /** A magnifier over a table: lookup functions. */
    val LookupFunctions: ImageVector by lazy { tableIcon("LookupFunctions", stroke = listOf(oval(15.5f, 15.5f, 3.5f, 3.5f), "M18 18l3 3"), thin = listOf("M3.5 3.5h15v6", "M3.5 3.5v15h7", "M3.5 8.5h15", "M10 3.5v15")) }

    /** A coin: financial functions. */
    val FinancialFunctions: ImageVector by lazy { tableIcon("FinancialFunctions", stroke = listOf(oval(12f, 12f, 8.5f, 8.5f)), thin = listOf("M14.5 9c-.5-.9-1.5-1.4-2.5-1.4c-1.5 0-2.6.9-2.6 2.1c0 2.8 5.4 1.6 5.4 4.5c0 1.2-1.2 2.2-2.8 2.2c-1.1 0-2.1-.5-2.6-1.4", "M12 6v1.6", "M12 16.4V18")) }

    /** A bell curve: statistical functions. */
    val StatisticsFunctions: ImageVector by lazy { tableIcon("StatisticsFunctions", stroke = listOf("M2.5 19.5h19", "M3 18.5c3.5 0 5-12 9-12s5.5 12 9 12"), thin = listOf("M12 6.5v13")) }

    /** "123" with a down arrow: the number format menu. */
    val NumberFormat: ImageVector by lazy { tableIcon("NumberFormat", stroke = listOf("M2.5 9l1.8-1.5V16", "M7.5 9.2c.3-1 1.1-1.6 2.1-1.6c1.1 0 1.9.8 1.9 1.8c0 2-4 3.2-4 6.6h4.2", "M14.5 8.2c.5-.4 1.1-.6 1.8-.6c1.2 0 2 .7 2 1.8s-.9 1.7-2 1.7c1.3 0 2.3.7 2.3 2c0 1.4-1.1 2.3-2.4 2.3c-.8 0-1.5-.3-2-.8")) }

    /** A heavy B: bold. */
    val Bold: ImageVector by lazy { tableIcon("Bold", stroke = listOf("M7 4.5h6a3.75 3.75 0 0 1 0 7.5H7z", "M7 12h7a4 4 0 0 1 0 8H7z")) }

    /** A slanted I: italic. */
    val Italic: ImageVector by lazy { tableIcon("Italic", stroke = listOf("M10.5 4.5h7", "M6.5 19.5h7", "M14 4.5l-4 15")) }

    /** A tipped paint bucket and a drop: fill color. */
    val FillColor: ImageVector by lazy { tableIcon("FillColor", stroke = listOf("M11 3.5l7.5 7.5l-6.5 6.5a1.5 1.5 0 0 1-2.1 0l-5.4-5.4a1.5 1.5 0 0 1 0-2.1z", "M19.5 15.5c0 0-1.5 2-1.5 3a1.5 1.5 0 0 0 3 0c0-1-1.5-3-1.5-3z"), thin = listOf("M4.5 11.5h13.5"), shade = listOf("M4.5 11.5h13.5l-5.9 5.9a1.5 1.5 0 0 1-2.1 0z")) }

    /** Big A and small a: change case. */
    val ChangeCase: ImageVector by lazy { tableIcon("ChangeCase", stroke = listOf("M2.5 19L7.5 5l5 14", "M4.4 14h6.2", "M21.5 19v-6a2.8 2.8 0 0 0-5.2-1.4", "M21.5 15.4c-3.2-.5-6 0-6 1.9a1.9 1.9 0 0 0 3.5 1")) }

    /** A banknote: currency. */
    val Currency: ImageVector by lazy { tableIcon("Currency", stroke = listOf("M2.5 6.5h19v11h-19z", "M12 9.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5"), thin = listOf("M5.5 9.5v5", "M18.5 9.5v5")) }

    /** A paint brush: copy a column's format to another. */
    val FormatPainter: ImageVector by lazy { tableIcon("FormatPainter", stroke = listOf("M4.5 3.5h12v5h-12z", "M16.5 6h3v5h-8v3", "M10 14h3v6.5h-3z"), shade = listOf("M4.5 3.5h12v5h-12z")) }

    /** A dashed box round a grid: select all. */
    val SelectAll: ImageVector by lazy { tableIcon("SelectAll", stroke = listOf("M3.5 7V3.5H7", "M17 3.5h3.5V7", "M20.5 17v3.5H17", "M7 20.5H3.5V17"), thin = listOf("M7.5 7.5h9v9h-9z", "M7.5 12h9", "M12 7.5v9")) }

    /** An arrow into a cell: go to. */
    val GoTo: ImageVector by lazy { tableIcon("GoTo", stroke = listOf("M3 12h10", "M9.5 8.5L13 12l-3.5 3.5"), thin = listOf("M14.5 4.5h6v15h-6z"), shade = listOf("M14.5 9.5h6v5h-6z")) }

    /** A die: random numbers. */
    val Random: ImageVector by lazy { tableIcon("Random", stroke = listOf("M4.5 4.5h15v15h-15z"), fill = listOf("M7 8.5a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0-3 0z", "M10.5 12a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0-3 0z", "M14 15.5a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0-3 0z")) }

    /** Crossing arrows: randomize the rows. */
    val Shuffle: ImageVector by lazy { tableIcon("Shuffle", stroke = listOf("M3 7h3.5c4.5 0 6.5 10 11 10H21", "M3 17h3.5c1.8 0 3.1-1.6 4.2-3.5", "M13.3 10.5c1.1-1.9 2.4-3.5 4.2-3.5H21", "M18.5 4.5L21 7l-2.5 2.5", "M18.5 14.5L21 17l-2.5 2.5")) }

    /** Arrows up and down: reverse the rows. */
    val Reverse: ImageVector by lazy { tableIcon("Reverse", stroke = listOf("M7.5 20V4", "M4.5 7l3-3l3 3", "M16.5 4v16", "M13.5 17l3 3l3-3")) }

    /** A cell copied into the gap below it: fill blanks. */
    val FillBlanks: ImageVector by lazy { tableIcon("FillBlanks", stroke = listOf("M5 3.5h9v5H5z", "M18.5 6v10", "M16 13.5l2.5 2.5l2.5-2.5"), thin = listOf("M5 15.5h9v5H5z"), shade = listOf("M5 15.5h9v5H5z")) }

    /** Quoted text becoming a number: convert to numbers. */
    val TextToNumber: ImageVector by lazy { tableIcon("TextToNumber", stroke = listOf("M3.5 6.5v3", "M6.5 6.5v3", "M13 12h4", "M15.5 10l2 2l-2 2"), thin = listOf("M2.5 15.5h8v5h-8z"), fill = listOf("M19 6a1 1 0 1 0 2 0a1 1 0 1 0-2 0z")) }

    /** One of each: unique values. */
    val Unique: ImageVector by lazy { tableIcon("Unique", stroke = listOf("M4 5h4", "M4 10h4", "M4 15h4", "M4 20h4", "M12 12h3", "M13.5 10.5L15 12l-1.5 1.5"), thin = listOf("M17 5h4v14h-4z")) }

    /** A column worked out from another: a new column from its numbers. */
    val NewColumn: ImageVector by lazy { tableIcon("NewColumn", stroke = listOf("M14.5 3.5h6v17h-6z", "M8.5 12h4", "M11 9.5l2 2.5l-2 2.5"), thin = listOf("M3.5 3.5h4v17h-4z", "M14.5 9h6", "M14.5 14.5h6")) }

    /** ƒ turning into a number: formulas to values. */
    val ToValues: ImageVector by lazy { tableIcon("ToValues", stroke = listOf("M8.5 4c-2-.5-3.2.5-3.2 2.6V16", "M3 9h5", "M12 12h4", "M14.5 10l2 2l-2 2"), shade = listOf("M18 8.5h3.5v7H18z")) }

    /** A column with a slash: hide it. */
    val HideColumn: ImageVector by lazy { tableIcon("HideColumn", stroke = listOf("M3.5 3.5l17 17"), thin = listOf("M8 3.5h8v17H8z")) }

    /** Columns spreading apart: show hidden columns. */
    val UnhideColumns: ImageVector by lazy { tableIcon("UnhideColumns", stroke = listOf("M12 4v16", "M3.5 12h5", "M6 9.5L3.5 12L6 14.5", "M20.5 12h-5", "M18 9.5l2.5 2.5l-2.5 2.5")) }

    /** A calendar with today marked. */
    val Today: ImageVector by lazy { tableIcon("Today", stroke = listOf("M4.5 6h15v14.5h-15z", "M8 3.5v4", "M16 3.5v4"), thin = listOf("M4.5 10.5h15"), fill = listOf("M13.5 14h3v3h-3z")) }
}
