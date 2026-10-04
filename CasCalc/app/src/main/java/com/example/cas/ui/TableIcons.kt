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
 * strokes (thin ones 1.6 dp), to sit alongside Material Symbols Rounded. Two-tone, as the
 * geometry tools' icons: what a command acts on in the ink color, what it makes or does in the
 * accent color (see [DuoIcon]).
 */

internal fun tableIcon(name: String, stroke: List<String>, thin: List<String> = emptyList(), fill: List<String> = emptyList(), shade: List<String> = emptyList(), accent: Set<String> = emptySet(), width: Float = 24f): ImageVector {
    fun layer(layerName: String, keep: (String) -> Boolean) = ImageVector.Builder(layerName, width.dp, 24.dp, width, 24f).apply {
        val black = SolidColor(Color.Black)
        shade.filter(keep).forEach { addPath(PathParser().parsePathString(it).toNodes(), fill = black, fillAlpha = 0.35f) }
        stroke.filter(keep).forEach { addPath(PathParser().parsePathString(it).toNodes(), stroke = black, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) }
        thin.filter(keep).forEach { addPath(PathParser().parsePathString(it).toNodes(), stroke = black, strokeLineWidth = 1.6f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) }
        fill.filter(keep).forEach { addPath(PathParser().parsePathString(it).toNodes(), fill = black) }
    }.build()
    // The whole icon (for a plain one-color Icon), and its ink and accent layers for [AppIcon].
    if (accent.isNotEmpty()) {
        TableIcons.inks[name] = layer("$name ink") { it !in accent }
        TableIcons.accents[name] = layer("$name accent") { it in accent }
    }
    return layer(name) { true }
}

/** An ellipse as a path (a digit 0, a dot), centered at ([x], [y]). */
private fun oval(x: Float, y: Float, rx: Float, ry: Float) = "M${x - rx} ${y}a$rx $ry 0 1 0 ${2 * rx} 0a$rx $ry 0 1 0 ${-2 * rx} 0z"

object TableIcons {
    /** Each icon's second layer: the part drawn in the accent color (what the command makes or does), as the geometry tools' icons. */
    internal val accents = HashMap<String, ImageVector>()
    internal val inks = HashMap<String, ImageVector>()

    /** The accent layer of [icon], if it has one. */
    fun accentOf(icon: ImageVector): ImageVector? = accents[icon.name]

    /** [icon] without its accent layer (the icon itself if it has none). */
    fun inkOf(icon: ImageVector): ImageVector = inks[icon.name] ?: icon

    /** A pen writing a line: edit. */
    val Edit: ImageVector by lazy { tableIcon("Edit", stroke = listOf("M4.5 19.5l.8-4L15.6 5.2a2.1 2.1 0 0 1 3 3L8.3 18.5z", "M13.4 7.4l3 3", "M12.5 20.5h8"), shade = listOf("M4.5 19.5l.8-4l3 3z"), accent = setOf("M13.4 7.4l3 3", "M12.5 20.5h8", "M4.5 19.5l.8-4l3 3z")) }

    /** Two sheets, the copy in front: copy. */
    val Copy: ImageVector by lazy { tableIcon("Copy", stroke = listOf("M10 8.5h8.5a1.5 1.5 0 0 1 1.5 1.5v9a1.5 1.5 0 0 1-1.5 1.5H10A1.5 1.5 0 0 1 8.5 19v-9A1.5 1.5 0 0 1 10 8.5z", "M15.5 5V4.5A1.5 1.5 0 0 0 14 3H5.5A1.5 1.5 0 0 0 4 4.5V14a1.5 1.5 0 0 0 1.5 1.5H6"), shade = listOf("M10 8.5h8.5a1.5 1.5 0 0 1 1.5 1.5v9a1.5 1.5 0 0 1-1.5 1.5H10A1.5 1.5 0 0 1 8.5 19v-9A1.5 1.5 0 0 1 10 8.5z"), accent = setOf("M10 8.5h8.5a1.5 1.5 0 0 1 1.5 1.5v9a1.5 1.5 0 0 1-1.5 1.5H10A1.5 1.5 0 0 1 8.5 19v-9A1.5 1.5 0 0 1 10 8.5z")) }

    /** A clipboard with the lines being pasted onto it: paste. */
    val Paste: ImageVector by lazy { tableIcon("Paste", stroke = listOf("M8 4.5H6a1.5 1.5 0 0 0-1.5 1.5v13.5A1.5 1.5 0 0 0 6 21h12a1.5 1.5 0 0 0 1.5-1.5V6A1.5 1.5 0 0 0 18 4.5h-2", "M9 3h6v3H9z", "M8.5 11h7", "M8.5 14.5h7", "M8.5 18h4"), accent = setOf("M8.5 11h7", "M8.5 14.5h7", "M8.5 18h4")) }

    /** A push pin, its head filled: pinned (and Pin). */
    val Pin: ImageVector by lazy { tableIcon("Pin", stroke = listOf("M9.5 3.5h5v6l3 4.5h-11l3-4.5z", "M12 14v6.5", "M8 3.5h8"), shade = listOf("M9.5 3.5h5v6l3 4.5h-11l3-4.5z"), accent = setOf("M9.5 3.5h5v6l3 4.5h-11l3-4.5z", "M8 3.5h8")) }

    /** A push pin, outlined: not pinned (and Unpin). */
    val PinOutline: ImageVector by lazy { tableIcon("PinOutline", stroke = listOf("M9.5 3.5h5v6l3 4.5h-11l3-4.5z", "M12 14v6.5", "M8 3.5h8")) }

    /** A folder, its tab tinted. */
    val Folder: ImageVector by lazy { tableIcon("Folder", stroke = listOf("M3.5 7v11a1.5 1.5 0 0 0 1.5 1.5h14a1.5 1.5 0 0 0 1.5-1.5V9.5A1.5 1.5 0 0 0 19 8h-7.5l-2-2.5H5A1.5 1.5 0 0 0 3.5 7z"), shade = listOf("M3.5 7A1.5 1.5 0 0 1 5 5.5h4.5l2 2.5H3.5z"), accent = setOf("M3.5 7A1.5 1.5 0 0 1 5 5.5h4.5l2 2.5H3.5z")) }

    /** A folder open, its front flap tinted. */
    val FolderOpen: ImageVector by lazy { tableIcon("FolderOpen", stroke = listOf("M3.5 18V7A1.5 1.5 0 0 1 5 5.5h4.5l2 2.5H17a1.5 1.5 0 0 1 1.5 1.5V11", "M3.5 19.5l3-8h15l-3 8z"), shade = listOf("M3.5 19.5l3-8h15l-3 8z"), accent = setOf("M3.5 19.5l3-8h15l-3 8z")) }

    /** A folder with a plus: a new folder. */
    val NewFolder: ImageVector by lazy { tableIcon("NewFolder", stroke = listOf("M3.5 7v11a1.5 1.5 0 0 0 1.5 1.5h14a1.5 1.5 0 0 0 1.5-1.5V9.5A1.5 1.5 0 0 0 19 8h-7.5l-2-2.5H5A1.5 1.5 0 0 0 3.5 7z", "M12 11v5.5", "M9.25 13.75h5.5"), accent = setOf("M12 11v5.5", "M9.25 13.75h5.5")) }

    /** A folder struck through: out of its folder. */
    val FolderOff: ImageVector by lazy { tableIcon("FolderOff", stroke = listOf("M3.5 7v11a1.5 1.5 0 0 0 1.5 1.5h14a1.5 1.5 0 0 0 1.5-1.5V9.5A1.5 1.5 0 0 0 19 8h-7.5l-2-2.5H5A1.5 1.5 0 0 0 3.5 7z", "M3 3l18 18"), accent = setOf("M3 3l18 18")) }

    /** A bin, its lid lifted: delete. */
    val Delete: ImageVector by lazy { tableIcon("Delete", stroke = listOf("M6.5 7.5l1 12a1.5 1.5 0 0 0 1.5 1.4h6a1.5 1.5 0 0 0 1.5-1.4l1-12", "M4 7.5h16", "M9.5 7.5V5a1 1 0 0 1 1-1h3a1 1 0 0 1 1 1v2.5"), thin = listOf("M10.25 11v6.5", "M13.75 11v6.5"), accent = setOf("M4 7.5h16", "M9.5 7.5V5a1 1 0 0 1 1-1h3a1 1 0 0 1 1 1v2.5")) }

    /** ƒ(x) as a curve on axes: a line to plot. */
    val Line: ImageVector by lazy { tableIcon("Line", stroke = listOf("M5 16.5c2.8 0 3.8-9 7-9s4 7.5 7.5 5.5"), thin = listOf("M3 20.5h18", "M3.5 3v18"), accent = setOf("M5 16.5c2.8 0 3.8-9 7-9s4 7.5 7.5 5.5")) }

    /** Lines of text and a pencil: a note. */
    val Note: ImageVector by lazy { tableIcon("Note", stroke = listOf("M4 6h16", "M4 10.5h16", "M4 15h8", "M14.5 20.5l.6-2.6l5-5l2 2l-5 5z"), accent = setOf("M14.5 20.5l.6-2.6l5-5l2 2l-5 5z")) }

    /** A grid with its header row tinted: a table. */
    val Table: ImageVector by lazy { tableIcon("Table", stroke = listOf("M3.5 4.5h17v15h-17z"), thin = listOf("M3.5 9h17", "M3.5 14.25h17", "M9.2 4.5v15", "M14.8 4.5v15"), shade = listOf("M3.5 4.5h17V9h-17z"), accent = setOf("M3.5 4.5h17V9h-17z")) }

    /** A file with an arrow coming in: import data. */
    val ImportData: ImageVector by lazy { tableIcon("ImportData", stroke = listOf("M6 3.5h8l4.5 4.5v12.5H6z", "M12.25 18v-7", "M9.5 13.5l2.75-2.75L15 13.5"), thin = listOf("M14 3.5V8h4.5"), accent = setOf("M12.25 18v-7", "M9.5 13.5l2.75-2.75L15 13.5")) }

    /** A square with equal axes through it: equal scales (square zoom). */
    val SquareZoom: ImageVector by lazy { tableIcon("SquareZoom", stroke = listOf("M4.5 4.5h15v15h-15z", "M8.5 12h7", "M12 8.5v7"), accent = setOf("M8.5 12h7", "M12 8.5v7")) }

    /** Three sliders with their knobs: settings. */
    val Settings: ImageVector by lazy { tableIcon("Settings", stroke = listOf("M3.5 6.5h17", "M3.5 12h17", "M3.5 17.5h17"), fill = listOf(oval(9f, 6.5f, 2.4f, 2.4f), oval(15.5f, 12f, 2.4f, 2.4f), oval(7f, 17.5f, 2.4f, 2.4f)), accent = setOf(oval(9f, 6.5f, 2.4f, 2.4f), oval(15.5f, 12f, 2.4f, 2.4f), oval(7f, 17.5f, 2.4f, 2.4f))) }

    /** A calculator, its screen tinted: the calculator. */
    val ModeCalculator: ImageVector by lazy { tableIcon("ModeCalculator", stroke = listOf("M7 3h10a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z"), shade = listOf("M8 6h8v3.5H8z"), fill = listOf(oval(9f, 13.5f, 1.25f, 1.25f), oval(12f, 13.5f, 1.25f, 1.25f), oval(15f, 13.5f, 1.25f, 1.25f), oval(9f, 17.5f, 1.25f, 1.25f), oval(12f, 17.5f, 1.25f, 1.25f), oval(15f, 17.5f, 1.25f, 1.25f)), accent = setOf("M8 6h8v3.5H8z", oval(15f, 17.5f, 1.25f, 1.25f))) }

    /** A curve on axes: 2D graphing. */
    val Mode2D: ImageVector by lazy { tableIcon("Mode2D", stroke = listOf("M3.5 3.5v17h17", "M6 15c2.5 0 3.5-8 6.5-8s4 7 7.5 5"), accent = setOf("M6 15c2.5 0 3.5-8 6.5-8s4 7 7.5 5")) }

    /** A cube, its top face tinted: 3D graphing. */
    val Mode3D: ImageVector by lazy { tableIcon("Mode3D", stroke = listOf("M12 3l8 4.5v9L12 21l-8-4.5v-9z", "M4 7.5l8 4.5l8-4.5", "M12 12v9"), shade = listOf("M12 3l8 4.5l-8 4.5l-8-4.5z"), accent = setOf("M12 3l8 4.5l-8 4.5l-8-4.5z")) }

    /** A point on the complex plane, its angle marked: complex plotting. */
    val ModeComplex: ImageVector by lazy { tableIcon("ModeComplex", stroke = listOf("M4 20H21", "M4 20V3", "M4 20L15.5 8.5"), thin = listOf("M9.2 20A5.2 5.2 0 0 0 7.68 16.32"), fill = listOf(oval(15.5f, 8.5f, 2.1f, 2.1f)), accent = setOf("M4 20L15.5 8.5", oval(15.5f, 8.5f, 2.1f, 2.1f))) }

    /** A magnifier, its handle tinted: search. */
    val Search: ImageVector by lazy { tableIcon("Search", stroke = listOf(oval(10.5f, 10.5f, 6.5f, 6.5f), "M15.3 15.3l5.2 5.2"), accent = setOf("M15.3 15.3l5.2 5.2")) }

    /** A clock turning back: history. */
    val History: ImageVector by lazy { tableIcon("History", stroke = listOf("M4.5 12a7.5 7.5 0 1 0 2.2-5.3", "M3.8 4.3v3.6h3.6", "M12 8v4.4l3 2"), accent = setOf("M12 8v4.4l3 2")) }

    /** An arrow turning back: undo. */
    val Undo: ImageVector by lazy { tableIcon("Undo", stroke = listOf("M8.5 14L4 9.5L8.5 5", "M4 9.5h10a5.5 5.5 0 0 1 0 11h-3"), accent = setOf("M8.5 14L4 9.5L8.5 5")) }

    /** An arrow turning forward: redo. */
    val Redo: ImageVector by lazy { tableIcon("Redo", stroke = listOf("M15.5 14L20 9.5L15.5 5", "M20 9.5H10a5.5 5.5 0 0 0 0 11h3"), accent = setOf("M15.5 14L20 9.5L15.5 5")) }

    /** An open eye: shown. */
    val Visible: ImageVector by lazy { tableIcon("Visible", stroke = listOf("M2.5 12c2.5-4.5 6-7 9.5-7s7 2.5 9.5 7c-2.5 4.5-6 7-9.5 7s-7-2.5-9.5-7z", oval(12f, 12f, 3f, 3f)), accent = setOf(oval(12f, 12f, 3f, 3f))) }

    /** An eye struck through: hidden. */
    val Hidden: ImageVector by lazy { tableIcon("Hidden", stroke = listOf("M2.5 12c2.5-4.5 6-7 9.5-7s7 2.5 9.5 7c-2.5 4.5-6 7-9.5 7s-7-2.5-9.5-7z", "M4 4l16 16"), accent = setOf("M4 4l16 16")) }

    /** An i in a circle: information. */
    val Info: ImageVector by lazy { tableIcon("Info", stroke = listOf(oval(12f, 12f, 9f, 9f), "M12 11v6"), fill = listOf(oval(12f, 7.6f, 1.3f, 1.3f)), accent = setOf("M12 11v6", oval(12f, 7.6f, 1.3f, 1.3f))) }

    /** A light bulb, glowing: a tip. */
    val Tip: ImageVector by lazy { tableIcon("Tip", stroke = listOf("M12 3a6 6 0 0 0-3.5 10.9V16h7v-2.1A6 6 0 0 0 12 3z", "M9.5 19.5h5", "M10.5 22h3"), shade = listOf("M12 3a6 6 0 0 0-3.5 10.9V16h7v-2.1A6 6 0 0 0 12 3z"), accent = setOf("M12 3a6 6 0 0 0-3.5 10.9V16h7v-2.1A6 6 0 0 0 12 3z")) }

    /** A keyboard, its space bar tinted. */
    val Keyboard: ImageVector by lazy { tableIcon("Keyboard", stroke = listOf("M4 5.5h16a1.5 1.5 0 0 1 1.5 1.5v10a1.5 1.5 0 0 1-1.5 1.5H4A1.5 1.5 0 0 1 2.5 17V7A1.5 1.5 0 0 1 4 5.5z", "M8 15h8"), fill = listOf(oval(6.5f, 9f, 1f, 1f), oval(10f, 9f, 1f, 1f), oval(14f, 9f, 1f, 1f), oval(17.5f, 9f, 1f, 1f), oval(8.2f, 12f, 1f, 1f), oval(12f, 12f, 1f, 1f), oval(15.8f, 12f, 1f, 1f)), accent = setOf("M8 15h8")) }

    /** A palette with its paints: colors. */
    val Palette: ImageVector by lazy { tableIcon("Palette", stroke = listOf("M12 3a9 9 0 0 0 0 18c1.3 0 2-.8 2-1.8c0-1.4-1.3-1.8-1.3-3c0-1 .8-1.7 1.8-1.7H17a4 4 0 0 0 4-4C21 6.6 17 3 12 3z"), fill = listOf(oval(7.5f, 11f, 1.5f, 1.5f), oval(9.8f, 7f, 1.5f, 1.5f), oval(14.5f, 7f, 1.5f, 1.5f)), accent = setOf(oval(7.5f, 11f, 1.5f, 1.5f), oval(9.8f, 7f, 1.5f, 1.5f), oval(14.5f, 7f, 1.5f, 1.5f))) }

    /** A triangle pointing on: play. */
    val Play: ImageVector by lazy { tableIcon("Play", stroke = listOf("M8 5v14l11-7z"), shade = listOf("M8 5v14l11-7z"), accent = setOf("M8 5v14l11-7z")) }

    /** Two bars: pause. */
    val Pause: ImageVector by lazy { tableIcon("Pause", stroke = listOf("M8.5 5v14", "M15.5 5v14"), accent = setOf("M8.5 5v14", "M15.5 5v14")) }

    /** An arrow round a play triangle: play again. */
    val Replay: ImageVector by lazy { tableIcon("Replay", stroke = listOf("M5 12a7 7 0 1 0 2-4.9", "M4.4 3.8v3.8h3.8", "M10.5 9v6l4.5-3z"), accent = setOf("M10.5 9v6l4.5-3z")) }

    /** A ruler and its marks: a length. */
    val Ruler: ImageVector by lazy { tableIcon("Ruler", stroke = listOf("M3 15.5L15.5 3l5.5 5.5L8.5 21z"), thin = listOf("M7 11.5l2 2", "M10 8.5l1.5 1.5", "M13 5.5l2 2"), accent = setOf("M7 11.5l2 2", "M10 8.5l1.5 1.5", "M13 5.5l2 2")) }

    /** A pin struck through: unpin. */
    val Unpin: ImageVector by lazy { tableIcon("Unpin", stroke = listOf("M9.5 3.5h5v6l3 4.5h-11l3-4.5z", "M12 14v6.5", "M8 3.5h8", "M3.5 3.5l17 17"), accent = setOf("M3.5 3.5l17 17")) }

    /** A cross: close. */
    val Close: ImageVector by lazy { tableIcon("Close", stroke = listOf("M6 6l12 12", "M18 6L6 18")) }

    /** A tick: done, chosen. */
    val Check: ImageVector by lazy { tableIcon("Check", stroke = listOf("M4.5 12.5l5 5L19.5 7"), accent = setOf("M4.5 12.5l5 5L19.5 7")) }

    /** A plus: add. */
    val Add: ImageVector by lazy { tableIcon("Add", stroke = listOf("M12 5v14", "M5 12h14"), accent = setOf("M12 5v14", "M5 12h14")) }

    /** A minus: remove, zoom out. */
    val Remove: ImageVector by lazy { tableIcon("Remove", stroke = listOf("M5 12h14"), accent = setOf("M5 12h14")) }

    /** Three dots down: more. */
    val More: ImageVector by lazy { tableIcon("More", stroke = listOf(), fill = listOf(oval(12f, 5.5f, 1.9f, 1.9f), oval(12f, 12f, 1.9f, 1.9f), oval(12f, 18.5f, 1.9f, 1.9f))) }

    /** Three dots across: more. */
    val MoreHoriz: ImageVector by lazy { tableIcon("MoreHoriz", stroke = listOf(), fill = listOf(oval(5.5f, 12f, 1.9f, 1.9f), oval(12f, 12f, 1.9f, 1.9f), oval(18.5f, 12f, 1.9f, 1.9f))) }

    /** A key shape pointing back with a cross: delete back. */
    val Backspace: ImageVector by lazy { tableIcon("Backspace", stroke = listOf("M8 5.5h11.5a1.5 1.5 0 0 1 1.5 1.5v10a1.5 1.5 0 0 1-1.5 1.5H8L2.5 12z", "M11 9l6 6", "M17 9l-6 6"), accent = setOf("M11 9l6 6", "M17 9l-6 6")) }

    /** A return arrow: enter, use. */
    val Enter: ImageVector by lazy { tableIcon("Enter", stroke = listOf("M19.5 5v6.5a2.5 2.5 0 0 1-2.5 2.5H5", "M9 10l-4 4l4 4"), accent = setOf("M9 10l-4 4l4 4")) }

    /** An arrow bringing a line down into the input: use the answer. */
    val UseAnswer: ImageVector by lazy { tableIcon("UseAnswer", stroke = listOf("M4 5.5h10", "M4 9.5h6", "M17 4.5v9a2.5 2.5 0 0 1-2.5 2.5H8", "M11 13l-3 3l3 3", "M4 20.5h16"), accent = setOf("M17 4.5v9a2.5 2.5 0 0 1-2.5 2.5H8", "M11 13l-3 3l3 3")) }

    /** Two arrows trading places between units: the unit converter. */
    val Converter: ImageVector by lazy { tableIcon("Converter", stroke = listOf("M4 8h13", "M14 4.5L17.5 8L14 11.5", "M20 16H7", "M10 12.5L6.5 16l3.5 3.5"), accent = setOf("M20 16H7", "M10 12.5L6.5 16l3.5 3.5")) }

    /** A heart: acknowledgements. */
    // An award rosette: a medal on two ribbons (not a heart).
    val Thanks: ImageVector by lazy { tableIcon("Thanks", stroke = listOf("M12 3a5.5 5.5 0 1 0 0 11a5.5 5.5 0 1 0 0-11z", "M8.6 13l-1.6 8 5-2.6 5 2.6-1.6-8"), fill = listOf("M12 6.2a2.3 2.3 0 1 0 0 4.6a2.3 2.3 0 1 0 0-4.6z"), accent = setOf("M12 6.2a2.3 2.3 0 1 0 0 4.6a2.3 2.3 0 1 0 0-4.6z")) }

    /** The history clock with a cross: clear history. */
    val ClearHistory: ImageVector by lazy { tableIcon("ClearHistory", stroke = listOf("M4.5 12a7.5 7.5 0 1 0 2.2-5.3", "M3.8 4.3v3.6h3.6", "M12 8v4.4l3 2", "M16 16l5 5", "M21 16l-5 5"), accent = setOf("M16 16l5 5", "M21 16l-5 5")) }

    /** x := struck through: forget the variables. */
    val Undefine: ImageVector by lazy { tableIcon("Undefine", stroke = listOf("M3 7l6 9", "M9 7l-6 9", "M13.5 9.5h7.5", "M13.5 13.5h7.5", "M2.5 20.5L21.5 3.5"), fill = listOf(oval(11f, 9.5f, 1.1f, 1.1f), oval(11f, 13.5f, 1.1f, 1.1f)), accent = setOf("M2.5 20.5L21.5 3.5")) }

    /** An answer line and a copy: copy the last answer. */
    val CopyAnswer: ImageVector by lazy { tableIcon("CopyAnswer", stroke = listOf("M10 8.5h8.5a1.5 1.5 0 0 1 1.5 1.5v9a1.5 1.5 0 0 1-1.5 1.5H10A1.5 1.5 0 0 1 8.5 19v-9A1.5 1.5 0 0 1 10 8.5z", "M15.5 5V4.5A1.5 1.5 0 0 0 14 3H5.5A1.5 1.5 0 0 0 4 4.5V14a1.5 1.5 0 0 0 1.5 1.5H6", "M11.5 13h5", "M11.5 16.5h5"), accent = setOf("M11.5 13h5", "M11.5 16.5h5")) }

    /** Six dots: drag to move. */
    val Grip: ImageVector by lazy { tableIcon("Grip", stroke = listOf(), fill = listOf(oval(9f, 6f, 1.6f, 1.6f), oval(15f, 6f, 1.6f, 1.6f), oval(9f, 12f, 1.6f, 1.6f), oval(15f, 12f, 1.6f, 1.6f), oval(9f, 18f, 1.6f, 1.6f), oval(15f, 18f, 1.6f, 1.6f))) }

    /** Arrows up and down: swap. */
    val Swap: ImageVector by lazy { tableIcon("Swap", stroke = listOf("M8 19V5", "M4.5 8.5L8 5l3.5 3.5", "M16 5v14", "M12.5 15.5L16 19l3.5-3.5"), accent = setOf("M16 5v14", "M12.5 15.5L16 19l3.5-3.5")) }

    /** Arrows left and right: swap. */
    val SwapH: ImageVector by lazy { tableIcon("SwapH", stroke = listOf("M5 8h14", "M15.5 4.5L19 8l-3.5 3.5", "M19 16H5", "M8.5 12.5L5 16l3.5 3.5"), accent = setOf("M19 16H5", "M8.5 12.5L5 16l3.5 3.5")) }

    /** A box with an arrow leaving its corner: open elsewhere. */
    val External: ImageVector by lazy { tableIcon("External", stroke = listOf("M19 13.5V18a1.5 1.5 0 0 1-1.5 1.5h-11A1.5 1.5 0 0 1 5 18V7a1.5 1.5 0 0 1 1.5-1.5H11", "M14 4.5h5.5V10", "M19.5 4.5L11 13"), accent = setOf("M14 4.5h5.5V10", "M19.5 4.5L11 13")) }

    /** A globe: language. */
    val Globe: ImageVector by lazy { tableIcon("Globe", stroke = listOf(oval(12f, 12f, 8.5f, 8.5f)), thin = listOf("M3.5 12h17", oval(12f, 12f, 3.6f, 8.5f)), accent = setOf(oval(12f, 12f, 3.6f, 8.5f))) }

    /** A bug: report a problem. */
    val Bug: ImageVector by lazy { tableIcon("Bug", stroke = listOf("M8 10a4 4 0 0 1 8 0v4a4 4 0 0 1-8 0z", "M12 10v8"), thin = listOf("M8 12H4.5", "M19.5 12H16", "M8.5 7.5L6 5", "M15.5 7.5L18 5", "M8 16l-3 2", "M16 16l3 2"), accent = setOf("M8.5 7.5L6 5", "M15.5 7.5L18 5")) }

    /** A question mark in a circle: help. */
    val Help: ImageVector by lazy { tableIcon("Help", stroke = listOf(oval(12f, 12f, 9f, 9f), "M9.5 9.5a2.5 2.5 0 1 1 3.5 2.3c-.7.3-1 .9-1 1.6v.6"), fill = listOf(oval(12f, 17f, 1.25f, 1.25f)), accent = setOf("M9.5 9.5a2.5 2.5 0 1 1 3.5 2.3c-.7.3-1 .9-1 1.6v.6", oval(12f, 17f, 1.25f, 1.25f))) }

    /** A sparkle: something new. */
    val Sparkle: ImageVector by lazy { tableIcon("Sparkle", stroke = listOf("M12 3.5c.6 4.2 2.3 5.9 6.5 6.5c-4.2.6-5.9 2.3-6.5 6.5c-.6-4.2-2.3-5.9-6.5-6.5c4.2-.6 5.9-2.3 6.5-6.5z"), thin = listOf("M18 16v4", "M16 18h4"), shade = listOf("M12 3.5c.6 4.2 2.3 5.9 6.5 6.5c-4.2.6-5.9 2.3-6.5 6.5c-.6-4.2-2.3-5.9-6.5-6.5c4.2-.6 5.9-2.3 6.5-6.5z"), accent = setOf("M12 3.5c.6 4.2 2.3 5.9 6.5 6.5c-4.2.6-5.9 2.3-6.5 6.5c-.6-4.2-2.3-5.9-6.5-6.5c4.2-.6 5.9-2.3 6.5-6.5z")) }

    /** A triangle with an exclamation mark: a warning. */
    val Warning: ImageVector by lazy { tableIcon("Warning", stroke = listOf("M12 4l9 15.5H3z", "M12 10v4.5"), fill = listOf(oval(12f, 17f, 1.2f, 1.2f)), accent = setOf("M12 10v4.5", oval(12f, 17f, 1.2f, 1.2f))) }

    /** An exclamation mark in a circle: an error. */
    val ErrorCircle: ImageVector by lazy { tableIcon("ErrorCircle", stroke = listOf(oval(12f, 12f, 9f, 9f), "M12 7.5v5.5"), fill = listOf(oval(12f, 16.5f, 1.25f, 1.25f)), accent = setOf("M12 7.5v5.5", oval(12f, 16.5f, 1.25f, 1.25f))) }

    /** A star, filled: a favourite. */
    val Star: ImageVector by lazy { tableIcon("Star", stroke = listOf("M12 3.5l2.6 5.4l5.9.8l-4.3 4.1l1 5.8L12 16.8l-5.2 2.8l1-5.8l-4.3-4.1l5.9-.8z"), shade = listOf("M12 3.5l2.6 5.4l5.9.8l-4.3 4.1l1 5.8L12 16.8l-5.2 2.8l1-5.8l-4.3-4.1l5.9-.8z"), accent = setOf("M12 3.5l2.6 5.4l5.9.8l-4.3 4.1l1 5.8L12 16.8l-5.2 2.8l1-5.8l-4.3-4.1l5.9-.8z")) }

    /** A star, outlined: not a favourite. */
    val StarOutline: ImageVector by lazy { tableIcon("StarOutline", stroke = listOf("M12 3.5l2.6 5.4l5.9.8l-4.3 4.1l1 5.8L12 16.8l-5.2 2.8l1-5.8l-4.3-4.1l5.9-.8z")) }

    /** An arrow down into a tray: save. */
    val Download: ImageVector by lazy { tableIcon("Download", stroke = listOf("M12 4v10.5", "M8 10.5l4 4l4-4", "M4.5 15.5V19a1.5 1.5 0 0 0 1.5 1.5h12a1.5 1.5 0 0 0 1.5-1.5v-3.5"), accent = setOf("M12 4v10.5", "M8 10.5l4 4l4-4")) }

    /** A page with lines: a document. */
    val Document: ImageVector by lazy { tableIcon("Document", stroke = listOf("M6 3.5h8l4.5 4.5v12.5H6z"), thin = listOf("M14 3.5V8h4.5", "M9 12h6", "M9 15.5h6"), accent = setOf("M9 12h6", "M9 15.5h6")) }

    /** Aa: fonts. */
    val Font: ImageVector by lazy { tableIcon("Font", stroke = listOf("M2.5 19L7.5 5l5 14", "M4.4 14h6.2", "M21.5 19v-6a2.8 2.8 0 0 0-5.2-1.4", "M21.5 15.4c-3.2-.5-6 0-6 1.9a1.9 1.9 0 0 0 3.5 1"), accent = setOf("M21.5 19v-6a2.8 2.8 0 0 0-5.2-1.4", "M21.5 15.4c-3.2-.5-6 0-6 1.9a1.9 1.9 0 0 0 3.5 1")) }

    /** Chevrons apart: show more. */
    val Expand: ImageVector by lazy { tableIcon("Expand", stroke = listOf("M7.5 9.5L12 5l4.5 4.5", "M7.5 14.5L12 19l4.5-4.5")) }

    /** Chevrons together: show less. */
    val Collapse: ImageVector by lazy { tableIcon("Collapse", stroke = listOf("M7.5 5L12 9.5L16.5 5", "M7.5 19L12 14.5l4.5 4.5")) }

    /** Corners outward: bigger. */
    val Fullscreen: ImageVector by lazy { tableIcon("Fullscreen", stroke = listOf("M4 9V4h5", "M15 4h5v5", "M20 15v5h-5", "M9 20H4v-5")) }

    /** Corners inward: smaller. */
    val ExitFullscreen: ImageVector by lazy { tableIcon("ExitFullscreen", stroke = listOf("M9 4v5H4", "M20 9h-5V4", "M15 20v-5h5", "M4 15h5v5")) }

    /** An open hand: move the view. */
    val Hand: ImageVector by lazy { tableIcon("Hand", stroke = listOf("M8 12V6a1.5 1.5 0 0 1 3 0v5", "M11 10.5V4.5a1.5 1.5 0 0 1 3 0v6", "M14 10.5v-4a1.5 1.5 0 0 1 3 0v6.5", "M8 12v-1.5a1.5 1.5 0 0 0-3 0V14c0 4 2.7 6.5 6.5 6.5S17 18 17 14v-1")) }

    /** A wand with sparks: tidy up. */
    val Wand: ImageVector by lazy { tableIcon("Wand", stroke = listOf("M4 20L15 9", "M13.5 7.5l3 3"), thin = listOf("M18 3.5v3", "M16.5 5h3", "M20 10v2.5", "M18.8 11.2h2.5", "M10.5 3.5v2", "M9.5 4.5h2"), accent = setOf("M18 3.5v3", "M16.5 5h3", "M20 10v2.5", "M18.8 11.2h2.5", "M10.5 3.5v2", "M9.5 4.5h2")) }

    /** A flag: mark. */
    val Flag: ImageVector by lazy { tableIcon("Flag", stroke = listOf("M5 21V4", "M5 4.5h12l-2.5 4l2.5 4H5"), shade = listOf("M5 4.5h12l-2.5 4l2.5 4H5z"), accent = setOf("M5 4.5h12l-2.5 4l2.5 4H5", "M5 4.5h12l-2.5 4l2.5 4H5z")) }

    /** A bulleted list. */
    val Bullets: ImageVector by lazy { tableIcon("Bullets", stroke = listOf("M9 6h11", "M9 12h11", "M9 18h11"), fill = listOf(oval(4.5f, 6f, 1.5f, 1.5f), oval(4.5f, 12f, 1.5f, 1.5f), oval(4.5f, 18f, 1.5f, 1.5f)), accent = setOf(oval(4.5f, 6f, 1.5f, 1.5f), oval(4.5f, 12f, 1.5f, 1.5f), oval(4.5f, 18f, 1.5f, 1.5f))) }

    /** A path splitting in two. */
    val Split: ImageVector by lazy { tableIcon("Split", stroke = listOf("M12 20v-7", "M12 13L6 6", "M12 13l6-7", "M6 9.5V6h3.5", "M14.5 6H18v3.5"), accent = setOf("M6 9.5V6h3.5", "M14.5 6H18v3.5")) }

    /** Two paths joining. */
    val Merge: ImageVector by lazy { tableIcon("Merge", stroke = listOf("M12 4v7", "M12 11l-6 7", "M12 11l6 7", "M8.5 7.5L12 4l3.5 3.5"), accent = setOf("M8.5 7.5L12 4l3.5 3.5")) }

    /** Lines indented further. */
    val IndentMore: ImageVector by lazy { tableIcon("IndentMore", stroke = listOf("M4 5h16", "M11 10h9", "M11 14h9", "M4 19h16", "M4 9.5L7 12l-3 2.5"), accent = setOf("M4 9.5L7 12l-3 2.5")) }

    /** Lines indented less. */
    val IndentLess: ImageVector by lazy { tableIcon("IndentLess", stroke = listOf("M4 5h16", "M11 10h9", "M11 14h9", "M4 19h16", "M7 9.5L4 12l3 2.5"), accent = setOf("M7 9.5L4 12l3 2.5")) }

    /** A line climbing: a trend. */
    val TrendUp: ImageVector by lazy { tableIcon("TrendUp", stroke = listOf("M3 17l6-6l4 4l8-8", "M15 7h6v6"), accent = setOf("M15 7h6v6")) }

    /** A finger tapping: touch. */
    val Tap: ImageVector by lazy { tableIcon("Tap", stroke = listOf("M10 13V5.5a1.5 1.5 0 0 1 3 0V12l4.2.9a2 2 0 0 1 1.6 2.3l-.8 5.3H10.5L7 16.5a1.6 1.6 0 0 1 2.4-2.1z"), thin = listOf("M7.5 6a4 4 0 0 1 8 0"), accent = setOf("M7.5 6a4 4 0 0 1 8 0")) }
    // Gesture hands for the guides: hold (rings held around the fingertip), swipe (a sideways arrow), drag (a trail).
    val GestureHold: ImageVector by lazy { tableIcon("GestureHold", stroke = listOf("M10 13V5.5a1.5 1.5 0 0 1 3 0V12l4.2.9a2 2 0 0 1 1.6 2.3l-.8 5.3H10.5L7 16.5a1.6 1.6 0 0 1 2.4-2.1z", "M4.5 6a7 7 0 0 1 14 0"), thin = listOf("M7.5 6a4 4 0 0 1 8 0"), accent = setOf("M4.5 6a7 7 0 0 1 14 0", "M7.5 6a4 4 0 0 1 8 0")) }
    val GestureSwipe: ImageVector by lazy { tableIcon("GestureSwipe", stroke = listOf("M10 15V9.5a1.5 1.5 0 0 1 3 0V14l4.2.9a2 2 0 0 1 1.6 2.3l-.6 4H10.5L7 18.5a1.6 1.6 0 0 1 2.4-2.1z", "M4 4.5h15", "M6.3 2.3L4 4.5l2.3 2.2", "M16.7 2.3L19 4.5l-2.3 2.2"), accent = setOf("M4 4.5h15", "M6.3 2.3L4 4.5l2.3 2.2", "M16.7 2.3L19 4.5l-2.3 2.2")) }
    val GestureDrag: ImageVector by lazy { tableIcon("GestureDrag", stroke = listOf("M10 15V9.5a1.5 1.5 0 0 1 3 0V14l4.2.9a2 2 0 0 1 1.6 2.3l-.6 4H10.5L7 18.5a1.6 1.6 0 0 1 2.4-2.1z", "M5.2 7.6l2.9.2-.6 2.8"), thin = listOf("M3.5 21C3 15.5 4.2 11 8 8"), accent = setOf("M5.2 7.6l2.9.2-.6 2.8", "M3.5 21C3 15.5 4.2 11 8 8")) }

    /** A circle. */
    val Lens: ImageVector by lazy { tableIcon("Lens", stroke = listOf(oval(12f, 12f, 7f, 7f))) }

    /** Four arrows: move. */
    val Move: ImageVector by lazy { tableIcon("Move", stroke = listOf("M12 3v18", "M3 12h18", "M9.5 5.5L12 3l2.5 2.5", "M9.5 18.5L12 21l2.5-2.5", "M5.5 9.5L3 12l2.5 2.5", "M18.5 9.5L21 12l-2.5 2.5"), accent = setOf("M9.5 5.5L12 3l2.5 2.5", "M9.5 18.5L12 21l2.5-2.5", "M5.5 9.5L3 12l2.5 2.5", "M18.5 9.5L21 12l-2.5 2.5")) }

    /** A phone with a plus: add to the home screen. */
    val Shortcut: ImageVector by lazy { tableIcon("Shortcut", stroke = listOf("M8 2.5h8a1.5 1.5 0 0 1 1.5 1.5v16a1.5 1.5 0 0 1-1.5 1.5H8A1.5 1.5 0 0 1 6.5 20V4A1.5 1.5 0 0 1 8 2.5z", "M12 9v6", "M9 12h6"), accent = setOf("M12 9v6", "M9 12h6")) }

    /** An arrow pointing back. */
    val Back: ImageVector by lazy { tableIcon("Back", stroke = listOf("M19.5 12h-15", "M10.5 6l-6 6l6 6")) }

    /** An arrow pointing on. */
    val Forward: ImageVector by lazy { tableIcon("Forward", stroke = listOf("M4.5 12h15", "M13.5 6l6 6l-6 6")) }

    /** Chevrons: right, left, up, down. */
    val ChevronRight: ImageVector by lazy { tableIcon("ChevronRight", stroke = listOf("M9.5 6l6 6l-6 6")) }
    val ChevronLeft: ImageVector by lazy { tableIcon("ChevronLeft", stroke = listOf("M14.5 6l-6 6l6 6")) }
    val ChevronUp: ImageVector by lazy { tableIcon("ChevronUp", stroke = listOf("M6 14.5l6-6l6 6")) }
    val ChevronDown: ImageVector by lazy { tableIcon("ChevronDown", stroke = listOf("M6 9.5l6 6l6-6")) }

    /** A small rounded triangle: a menu opens here. */
    val DropDown: ImageVector by lazy { tableIcon("DropDown", stroke = listOf("M8 10.5h8l-4 4.5z"), fill = listOf("M8 10.5h8l-4 4.5z")) }

    /** A box with an arrow leaving it: share (the same drawing as [ShareTable]). */
    val Share: ImageVector get() = ShareTable

    /** Bars growing downwards and an arrow up: smallest first. */
    val SortUp: ImageVector by lazy { tableIcon("SortUp", stroke = listOf("M3.5 6h5", "M3.5 12h8", "M3.5 18h11", "M19 19V5", "M16 8l3-3l3 3"), accent = setOf("M19 19V5", "M16 8l3-3l3 3")) }

    /** Bars shrinking downwards and an arrow down: largest first. */
    val SortDown: ImageVector by lazy { tableIcon("SortDown", stroke = listOf("M3.5 6h11", "M3.5 12h8", "M3.5 18h5", "M19 5v14", "M16 16l3 3l3-3"), accent = setOf("M19 5v14", "M16 16l3 3l3-3")) }

    /** A funnel: a filter. */
    val Filter: ImageVector by lazy { tableIcon("Filter", stroke = listOf("M3.5 5h17l-6.5 8v5.5l-4 2.5v-8z"), shade = listOf("M6.5 7.5h11l-3.6 4.5h-3.8z"), accent = setOf("M6.5 7.5h11l-3.6 4.5h-3.8z")) }

    /** Points climbing evenly along a line: fill with a series. */
    val Series: ImageVector by lazy { tableIcon("Series", stroke = listOf(), thin = listOf("M3 20L21 4"), fill = listOf(oval(5f, 18.2f, 2f, 2f), oval(10f, 13.8f, 2f, 2f), oval(15f, 9.4f, 2f, 2f), oval(20f, 5f, 2f, 2f)), accent = setOf(oval(5f, 18.2f, 2f, 2f), oval(10f, 13.8f, 2f, 2f), oval(15f, 9.4f, 2f, 2f), oval(20f, 5f, 2f, 2f))) }

    /** 1, 2, 3 down a column: fill with whole numbers. */
    val Numbering: ImageVector by lazy { tableIcon("Numbering", stroke = listOf("M4 6.5l2-1.5v5", "M4 13.5c0-1 .8-1.6 1.8-1.6s1.7.7 1.7 1.5c0 1.6-3.5 2.5-3.5 4.6h3.6", "M11 7h9", "M11 12h9", "M11 17h9"), accent = setOf("M4 6.5l2-1.5v5", "M4 13.5c0-1 .8-1.6 1.8-1.6s1.7.7 1.7 1.5c0 1.6-3.5 2.5-3.5 4.6h3.6")) }

    /** ƒ with an arrow down: copy the column's formula down. */
    val FillFormula: ImageVector by lazy { tableIcon("FillFormula", stroke = listOf("M13 4.5c-2.6-.6-4 .6-4 3V20", "M6 11h6", "M18.5 8v11", "M16 16.5l2.5 2.5l2.5-2.5"), accent = setOf("M18.5 8v11", "M16 16.5l2.5 2.5l2.5-2.5")) }

    /** A magnifier: find and replace. */
    val Find: ImageVector by lazy { tableIcon("Find", stroke = listOf(oval(10.5f, 10.5f, 6f, 6f), "M15 15l5.5 5.5"), thin = listOf("M8 10.5h5"), accent = setOf("M8 10.5h5")) }

    /** Σ: a column's statistics, or a range's sum. */
    val Statistics: ImageVector by lazy { tableIcon("Statistics", stroke = listOf("M17.5 5H6.5l6 7l-6 7h11"), accent = setOf("M17.5 5H6.5l6 7l-6 7h11")) }

    /** A row added above a table. */
    val RowAbove: ImageVector by lazy { tableIcon("RowAbove", stroke = listOf("M12 2.5v6", "M9 5.5h6"), thin = listOf("M3.5 11h17v9.5h-17z", "M3.5 15.75h17", "M9.2 11v9.5", "M14.8 11v9.5"), accent = setOf("M12 2.5v6", "M9 5.5h6")) }

    /** A row added below a table. */
    val RowBelow: ImageVector by lazy { tableIcon("RowBelow", stroke = listOf("M12 15.5v6", "M9 18.5h6"), thin = listOf("M3.5 3.5h17V13h-17z", "M3.5 8.25h17", "M9.2 3.5V13", "M14.8 3.5V13"), accent = setOf("M12 15.5v6", "M9 18.5h6")) }

    /** A column added beside a table. */
    val ColumnAdd: ImageVector by lazy { tableIcon("ColumnAdd", stroke = listOf("M18 9v6", "M15 12h6"), thin = listOf("M3.5 3.5H12v17H3.5z", "M3.5 9.2H12", "M3.5 14.8H12", "M7.75 3.5v17"), accent = setOf("M18 9v6", "M15 12h6")) }

    /** A table pasted from a clipboard. */
    val PasteTable: ImageVector by lazy { tableIcon("PasteTable", stroke = listOf("M8 4.5H6a1.5 1.5 0 0 0-1.5 1.5v13.5A1.5 1.5 0 0 0 6 21h12a1.5 1.5 0 0 0 1.5-1.5V6A1.5 1.5 0 0 0 18 4.5h-2", "M9 3h6v3H9z"), thin = listOf("M8 10.5h8v7H8z", "M8 14h8", "M12 10.5v7"), accent = setOf("M8 10.5h8v7H8z", "M8 14h8", "M12 10.5v7")) }

    /** Two rows alike, one taken out: remove duplicate rows. */
    val Dedupe: ImageVector by lazy { tableIcon("Dedupe", stroke = listOf("M3.5 5h17", "M3.5 10h17", "M15 15.5l5 5", "M20 15.5l-5 5"), thin = listOf("M3.5 18h8"), accent = setOf("M15 15.5l5 5", "M20 15.5l-5 5")) }

    /** Spaces pushed in from both ends: trim. */
    val Trim: ImageVector by lazy { tableIcon("Trim", stroke = listOf("M2.5 12h4", "M4.5 9.5L7 12l-2.5 2.5", "M21.5 12h-4", "M19.5 9.5L17 12l2.5 2.5"), thin = listOf("M10 8v8", "M14 8v8"), accent = setOf("M2.5 12h4", "M4.5 9.5L7 12l-2.5 2.5", "M21.5 12h-4", "M19.5 9.5L17 12l2.5 2.5")) }

    /** A row turning into a column: swap rows and columns. */
    val Transpose: ImageVector by lazy { tableIcon("Transpose", stroke = listOf("M4 4h6v6H4z", "M13 7h4a3 3 0 0 1 3 3v6", "M17.5 13.5L20 16l2.5-2.5"), thin = listOf("M4 14h6v6H4z", "M14 14h6v6h-6z"), accent = setOf("M13 7h4a3 3 0 0 1 3 3v6", "M17.5 13.5L20 16l2.5-2.5")) }

    /** A snowflake: freeze the first column. */
    val Freeze: ImageVector by lazy { tableIcon("Freeze", stroke = listOf("M12 3v18", "M4.2 7.5l15.6 9", "M4.2 16.5l15.6-9"), thin = listOf("M9.8 4.5L12 6.5l2.2-2", "M9.8 19.5L12 17.5l2.2 2", "M3.9 11l2.8-.5l-.9-2.8", "M20.1 13l-2.8.5l.9 2.8", "M5.8 16.3l.9-2.8l-2.8-.5", "M18.2 7.7l-.9 2.8l2.8.5"), accent = setOf("M9.8 4.5L12 6.5l2.2-2", "M9.8 19.5L12 17.5l2.2 2", "M3.9 11l2.8-.5l-.9-2.8", "M20.1 13l-2.8.5l.9 2.8", "M5.8 16.3l.9-2.8l-2.8-.5", "M18.2 7.7l-.9 2.8l2.8.5")) }

    /** Rows with an empty one struck out: remove empty rows. */
    val RemoveEmpty: ImageVector by lazy { tableIcon("RemoveEmpty", stroke = listOf("M3.5 5h17", "M3.5 19h17"), thin = listOf("M3.5 10h17v4h-17z", "M9 9l6 6", "M15 9l-6 6"), accent = setOf("M3.5 10h17v4h-17z", "M9 9l6 6", "M15 9l-6 6")) }

    /** A copy of a column beside it. */
    val DuplicateColumn: ImageVector by lazy { tableIcon("DuplicateColumn", stroke = listOf("M4 3.5h6v17H4z"), thin = listOf("M14 3.5h6v17h-6z", "M14 9.2h6", "M14 14.8h6", "M4 9.2h6", "M4 14.8h6"), accent = setOf("M4 3.5h6v17H4z")) }

    /** A column moved left. */
    val MoveLeft: ImageVector by lazy { tableIcon("MoveLeft", stroke = listOf("M10 12H3", "M5.5 9.5L3 12l2.5 2.5"), thin = listOf("M13 3.5h7v17h-7z", "M13 9.2h7", "M13 14.8h7"), accent = setOf("M10 12H3", "M5.5 9.5L3 12l2.5 2.5")) }

    /** A column moved right. */
    val MoveRight: ImageVector by lazy { tableIcon("MoveRight", stroke = listOf("M14 12h7", "M18.5 9.5L21 12l-2.5 2.5"), thin = listOf("M4 3.5h7v17H4z", "M4 9.2h7", "M4 14.8h7"), accent = setOf("M14 12h7", "M18.5 9.5L21 12l-2.5 2.5")) }

    /** A column wiped: clear it. */
    val ClearColumn: ImageVector by lazy { tableIcon("ClearColumn", stroke = listOf("M13 20.5h7.5", "M5.5 13.5l6-6l6 6l-5 5H8.5z"), thin = listOf("M8.5 10.5l6 6"), accent = setOf("M13 20.5h7.5", "M8.5 10.5l6 6")) }

    /** A column with a cross: delete it. */
    val DeleteColumn: ImageVector by lazy { tableIcon("DeleteColumn", stroke = listOf("M15 10l6 6", "M21 10l-6 6"), thin = listOf("M3.5 3.5H11v17H3.5z", "M3.5 9.2H11", "M3.5 14.8H11"), accent = setOf("M15 10l6 6", "M21 10l-6 6")) }

    /** "A" and a spark: numbers as typed (automatic format). */
    val FormatAuto: ImageVector by lazy { tableIcon("FormatAuto", stroke = listOf("M3.5 19.5L8.5 5l5 14.5", "M5.4 14h6.2", "M18.5 3.5v5", "M16 6h5"), accent = setOf("M18.5 3.5v5", "M16 6h5")) }

    /** 0.00: a fixed number of decimals. */
    val FormatFixed: ImageVector by lazy { tableIcon("FormatFixed", stroke = listOf(oval(5.5f, 12f, 2.5f, 5f), oval(14.2f, 12f, 2.2f, 4.5f), oval(20.2f, 12f, 2.2f, 4.5f)), fill = listOf(oval(9.7f, 16.4f, 1.3f, 1.3f)), accent = setOf(oval(9.7f, 16.4f, 1.3f, 1.3f))) }

    /** %: a percentage. */
    val Percent: ImageVector by lazy { tableIcon("Percent", stroke = listOf("M19 5L5 19"), thin = listOf(oval(7f, 7f, 2.6f, 2.6f), oval(17f, 17f, 2.6f, 2.6f)), accent = setOf("M19 5L5 19")) }

    /** 10 with a power: scientific notation. */
    val Scientific: ImageVector by lazy { tableIcon("Scientific", stroke = listOf("M3.5 9.5l2.5-2V19", oval(12f, 13.2f, 2.8f, 5.8f)), thin = listOf("M17 10V6.6a1.8 1.8 0 0 1 3.6 0V10"), accent = setOf("M17 10V6.6a1.8 1.8 0 0 1 3.6 0V10")) }

    /** 1 000: thousands grouped. */
    val Thousands: ImageVector by lazy { tableIcon("Thousands", stroke = listOf("M2.5 9l1.8-1.5V17"), thin = listOf(oval(9.3f, 12.3f, 1.9f, 4.4f), oval(14.3f, 12.3f, 1.9f, 4.4f), oval(19.3f, 12.3f, 1.9f, 4.4f)), accent = setOf("M2.5 9l1.8-1.5V17")) }

    /** .0 and an arrow left: one decimal fewer. */
    val DecimalsLess: ImageVector by lazy { tableIcon("DecimalsLess", stroke = listOf(oval(9f, 15f, 2.3f, 4.5f), "M21 6h-7", "M16.5 3.5L14 6l2.5 2.5"), fill = listOf(oval(4f, 18.6f, 1.4f, 1.4f)), accent = setOf("M21 6h-7", "M16.5 3.5L14 6l2.5 2.5")) }

    /** .00 and an arrow right: one decimal more. */
    val DecimalsMore: ImageVector by lazy { tableIcon("DecimalsMore", stroke = listOf(oval(8.5f, 15f, 2.1f, 4.3f), oval(14.5f, 15f, 2.1f, 4.3f), "M14 6h7", "M18.5 3.5L21 6l-2.5 2.5"), fill = listOf(oval(3.6f, 18.6f, 1.4f, 1.4f)), accent = setOf("M14 6h7", "M18.5 3.5L21 6l-2.5 2.5")) }

    /** Three bands from light to dark: a color scale. */
    val ColorScale: ImageVector by lazy { tableIcon("ColorScale", stroke = listOf("M5 3.5h14a1.5 1.5 0 0 1 1.5 1.5v14a1.5 1.5 0 0 1-1.5 1.5H5A1.5 1.5 0 0 1 3.5 19V5A1.5 1.5 0 0 1 5 3.5z"), thin = listOf("M3.5 9.2h17", "M3.5 14.8h17"), shade = listOf("M3.5 9.2h17v5.6h-17z"), fill = listOf("M3.5 14.8h17V19a1.5 1.5 0 0 1-1.5 1.5H5A1.5 1.5 0 0 1 3.5 19z"), accent = setOf("M3.5 9.2h17v5.6h-17z", "M3.5 14.8h17V19a1.5 1.5 0 0 1-1.5 1.5H5A1.5 1.5 0 0 1 3.5 19z")) }

    /** CSV text going out: share the table. */
    val ShareTable: ImageVector by lazy { tableIcon("ShareTable", stroke = listOf("M12 14V3.5", "M8.5 7L12 3.5L15.5 7", "M5 11v8.5a1.5 1.5 0 0 0 1.5 1.5h11a1.5 1.5 0 0 0 1.5-1.5V11"), accent = setOf("M12 14V3.5", "M8.5 7L12 3.5L15.5 7")) }

    /** Two cards, one over the other: copy. */
    val CopyTable: ImageVector by lazy { tableIcon("CopyTable", stroke = listOf("M8.5 8.5h11v12h-11z"), thin = listOf("M5.5 15.5h-1v-12h11v1", "M8.5 12.5h11", "M14 8.5v12"), accent = setOf("M8.5 8.5h11v12h-11z", "M8.5 12.5h11", "M14 8.5v12")) }

    /** Σ over a column of cells: AutoSum. */
    val AutoSum: ImageVector by lazy { tableIcon("AutoSum", stroke = listOf("M13.5 4H4.5l4.5 6l-4.5 6h9"), thin = listOf("M17 4h4.5v4h-4.5z", "M17 10h4.5v4h-4.5z", "M17 16h4.5v4h-4.5z"), fill = listOf("M17 16h4.5v4h-4.5z"), accent = setOf("M13.5 4H4.5l4.5 6l-4.5 6h9", "M17 16h4.5v4h-4.5z")) }

    /** Cells with bars of different lengths inside: data bars. */
    val DataBars: ImageVector by lazy { tableIcon("DataBars", stroke = listOf("M3.5 3.5h17v17h-17z"), thin = listOf("M3.5 9.2h17", "M3.5 14.8h17"), fill = listOf("M5 5h6v2.7H5z", "M5 10.7h12.5v2.7H5z", "M5 16.3h9v2.7H5z"), accent = setOf("M5 5h6v2.7H5z", "M5 10.7h12.5v2.7H5z", "M5 16.3h9v2.7H5z")) }

    /** A highlighter over a cell: highlight cells by a rule. */
    val Highlight: ImageVector by lazy { tableIcon("Highlight", stroke = listOf("M14.5 3.5l6 6l-8 8h-6v-6z", "M3 21h18"), thin = listOf("M11 7l6 6"), shade = listOf("M6.5 11.5l6 6h-6z"), accent = setOf("M3 21h18", "M6.5 11.5l6 6h-6z")) }

    /** A light bulb over a little chart: insights about a column. */
    val Insights: ImageVector by lazy { tableIcon("Insights", stroke = listOf("M12 3a6 6 0 0 0-3.5 10.9V16h7v-2.1A6 6 0 0 0 12 3z", "M9.5 19.5h5", "M10.5 22h3"), thin = listOf("M9.5 10.5l1.5-1.5l1.5 1.5l2-2.5"), accent = setOf("M9.5 10.5l1.5-1.5l1.5 1.5l2-2.5")) }

    /** Scissors: cut. */
    val Cut: ImageVector by lazy { tableIcon("Cut", stroke = listOf(oval(6.5f, 17.5f, 3f, 3f), oval(17.5f, 17.5f, 3f, 3f), "M8.6 15.4L18.5 3.5", "M15.4 15.4L5.5 3.5"), accent = setOf(oval(6.5f, 17.5f, 3f, 3f), oval(17.5f, 17.5f, 3f, 3f))) }

    /** Lines flush left: align left. */
    val AlignLeft: ImageVector by lazy { tableIcon("AlignLeft", stroke = listOf("M4 5h16", "M4 10h10", "M4 15h16", "M4 20h10"), accent = setOf("M4 10h10", "M4 20h10")) }

    /** Lines centered: align center. */
    val AlignCenter: ImageVector by lazy { tableIcon("AlignCenter", stroke = listOf("M4 5h16", "M7 10h10", "M4 15h16", "M7 20h10"), accent = setOf("M7 10h10", "M7 20h10")) }

    /** Lines flush right: align right. */
    val AlignRight: ImageVector by lazy { tableIcon("AlignRight", stroke = listOf("M4 5h16", "M10 10h10", "M4 15h16", "M10 20h10"), accent = setOf("M10 10h10", "M10 20h10")) }

    /** A grid with some cells tinted and a bar: conditional formatting. */
    val ConditionalFormat: ImageVector by lazy { tableIcon("ConditionalFormat", stroke = listOf("M3.5 3.5h17v17h-17z"), thin = listOf("M3.5 9.2h17", "M3.5 14.8h17", "M12 3.5v17"), shade = listOf("M12 3.5h8.5v5.7H12z"), fill = listOf("M3.5 14.8H12v5.7H3.5z", "M13.5 10.7h4.5v2.6h-4.5z"), accent = setOf("M12 3.5h8.5v5.7H12z", "M3.5 14.8H12v5.7H3.5z", "M13.5 10.7h4.5v2.6h-4.5z")) }

    /** A grid with a cell added: insert cells. */
    val InsertCells: ImageVector by lazy { tableIcon("InsertCells", stroke = listOf("M17.5 14v7", "M14 17.5h7"), thin = listOf("M3.5 3.5h13v10h-13z", "M3.5 8.5h13", "M10 3.5v10"), accent = setOf("M17.5 14v7", "M14 17.5h7")) }

    /** A grid with a cell taken away: delete cells. */
    val DeleteCells: ImageVector by lazy { tableIcon("DeleteCells", stroke = listOf("M14.5 15l6 6", "M20.5 15l-6 6"), thin = listOf("M3.5 3.5h13v10h-13z", "M3.5 8.5h13", "M10 3.5v10"), accent = setOf("M14.5 15l6 6", "M20.5 15l-6 6")) }

    /** An eraser: clear. */
    val Eraser: ImageVector by lazy { tableIcon("Eraser", stroke = listOf("M8 20.5h12.5", "M3.8 14.7l9.9-9.9a1.5 1.5 0 0 1 2.1 0l4.4 4.4a1.5 1.5 0 0 1 0 2.1L12 19.5H8.5z"), thin = listOf("M8.3 10.2l6.5 6.5"), accent = setOf("M8 20.5h12.5", "M8.3 10.2l6.5 6.5")) }

    /** Sort levels one under another: custom sort. */
    val CustomSort: ImageVector by lazy { tableIcon("CustomSort", stroke = listOf("M3.5 5h9", "M6.5 10.5h9", "M9.5 16h9", "M19 3.5v6", "M17 7.5l2 2l2-2"), thin = listOf("M3.5 5v11h6"), accent = setOf("M19 3.5v6", "M17 7.5l2 2l2-2")) }

    /** One column splitting into two: text to columns. */
    val TextToColumns: ImageVector by lazy { tableIcon("TextToColumns", stroke = listOf("M2.5 9.5h6v5h-6z", "M11 12h3", "M12.5 10.5L14 12l-1.5 1.5"), thin = listOf("M16 5h5.5v14H16z", "M16 9.5h5.5", "M16 14.5h5.5"), accent = setOf("M11 12h3", "M12.5 10.5L14 12l-1.5 1.5", "M16 5h5.5v14H16z", "M16 9.5h5.5", "M16 14.5h5.5")) }

    /** ƒx over cells: show formulas. */
    val ShowFormulas: ImageVector by lazy { tableIcon("ShowFormulas", stroke = listOf("M10 3.5c-2-.5-3.2.5-3.2 2.6V14", "M4.5 8.5h5", "M12.5 8l4 5", "M16.5 8l-4 5"), thin = listOf("M3.5 17.5h17v3.5h-17z"), accent = setOf("M10 3.5c-2-.5-3.2.5-3.2 2.6V14", "M4.5 8.5h5")) }

    /** A magnifier with +: zoom in. */
    val ZoomIn: ImageVector by lazy { tableIcon("ZoomIn", stroke = listOf(oval(10.5f, 10.5f, 6.5f, 6.5f), "M15.3 15.3l5.2 5.2", "M7.5 10.5h6", "M10.5 7.5v6"), accent = setOf("M7.5 10.5h6", "M10.5 7.5v6")) }

    /** A magnifier with −: zoom out. */
    val ZoomOut: ImageVector by lazy { tableIcon("ZoomOut", stroke = listOf(oval(10.5f, 10.5f, 6.5f, 6.5f), "M15.3 15.3l5.2 5.2", "M7.5 10.5h6"), accent = setOf("M7.5 10.5h6")) }

    /** 1:1 in a frame: zoom back to 100%. */
    val Zoom100: ImageVector by lazy { tableIcon("Zoom100", stroke = listOf("M7 9.5l1.5-1V16", "M15.5 9.5l1.5-1V16"), thin = listOf("M3.5 4.5h17v15h-17z"), fill = listOf(oval(12f, 10.5f, 1f, 1f), oval(12f, 14f, 1f, 1f)), accent = setOf("M7 9.5l1.5-1V16", "M15.5 9.5l1.5-1V16", oval(12f, 10.5f, 1f, 1f), oval(12f, 14f, 1f, 1f))) }

    /** ƒx: insert a function. */
    val InsertFunction: ImageVector by lazy { tableIcon("InsertFunction", stroke = listOf("M11.5 4c-2.4-.6-3.8.6-3.8 3V20", "M4.5 10h6.5", "M13.5 10l6 8", "M19.5 10l-6 8"), accent = setOf("M13.5 10l6 8", "M19.5 10l-6 8")) }

    /** The little arrow at a ribbon group's corner: more options. */
    val Launcher: ImageVector by lazy { tableIcon("Launcher", stroke = listOf("M9 15l8-8", "M11.5 7H17v5.5"), thin = listOf("M5 9V19h10"), accent = setOf("M9 15l8-8", "M11.5 7H17v5.5")) }

    /** π: math and trigonometry functions. */
    val MathFunctions: ImageVector by lazy { tableIcon("MathFunctions", stroke = listOf("M4 7.5h16", "M8.5 7.5c0 5-1 9.5-3 12", "M15 7.5v9.5c0 1.5.8 2.5 2.2 2.5c.9 0 1.6-.3 2.3-.9"), accent = setOf("M8.5 7.5c0 5-1 9.5-3 12", "M15 7.5v9.5c0 1.5.8 2.5 2.2 2.5c.9 0 1.6-.3 2.3-.9")) }

    /** A fork in a path: logical functions (IF). */
    val LogicFunctions: ImageVector by lazy { tableIcon("LogicFunctions", stroke = listOf("M12 21v-8", "M12 13L6 6", "M12 13l6-7", "M3.5 7.5L6 6l1 2.8", "M20.5 7.5L18 6l-1 2.8"), accent = setOf("M12 13L6 6", "M12 13l6-7", "M3.5 7.5L6 6l1 2.8", "M20.5 7.5L18 6l-1 2.8")) }

    /** "Aa": text functions. */
    val TextFunctions: ImageVector by lazy { tableIcon("TextFunctions", stroke = listOf("M2.5 19L7 5l4.5 14", "M4.2 14h5.6", "M21.5 19v-6.5a3 3 0 0 0-5.5-1.6", "M21.5 15.5c-3.5-.6-6.5 0-6.5 2a2 2 0 0 0 3.7 1"), accent = setOf("M21.5 19v-6.5a3 3 0 0 0-5.5-1.6", "M21.5 15.5c-3.5-.6-6.5 0-6.5 2a2 2 0 0 0 3.7 1")) }

    /** A calendar: date and time functions. */
    val DateFunctions: ImageVector by lazy { tableIcon("DateFunctions", stroke = listOf("M4.5 6h15v14.5h-15z", "M8 3.5v4", "M16 3.5v4"), thin = listOf("M4.5 10.5h15"), fill = listOf("M8 13h3v3H8z"), accent = setOf("M8 13h3v3H8z")) }

    /** A magnifier over a table: lookup functions. */
    val LookupFunctions: ImageVector by lazy { tableIcon("LookupFunctions", stroke = listOf(oval(15.5f, 15.5f, 3.5f, 3.5f), "M18 18l3 3"), thin = listOf("M3.5 3.5h15v6", "M3.5 3.5v15h7", "M3.5 8.5h15", "M10 3.5v15"), accent = setOf(oval(15.5f, 15.5f, 3.5f, 3.5f), "M18 18l3 3")) }

    /** A coin: financial functions. */
    val FinancialFunctions: ImageVector by lazy { tableIcon("FinancialFunctions", stroke = listOf(oval(12f, 12f, 8.5f, 8.5f)), thin = listOf("M14.5 9c-.5-.9-1.5-1.4-2.5-1.4c-1.5 0-2.6.9-2.6 2.1c0 2.8 5.4 1.6 5.4 4.5c0 1.2-1.2 2.2-2.8 2.2c-1.1 0-2.1-.5-2.6-1.4", "M12 6v1.6", "M12 16.4V18"), accent = setOf("M14.5 9c-.5-.9-1.5-1.4-2.5-1.4c-1.5 0-2.6.9-2.6 2.1c0 2.8 5.4 1.6 5.4 4.5c0 1.2-1.2 2.2-2.8 2.2c-1.1 0-2.1-.5-2.6-1.4", "M12 6v1.6", "M12 16.4V18")) }

    /** A bell curve: statistical functions. */
    val StatisticsFunctions: ImageVector by lazy { tableIcon("StatisticsFunctions", stroke = listOf("M2.5 19.5h19", "M3 18.5c3.5 0 5-12 9-12s5.5 12 9 12"), thin = listOf("M12 6.5v13"), accent = setOf("M3 18.5c3.5 0 5-12 9-12s5.5 12 9 12")) }

    /** "123" with a down arrow: the number format menu. */
    val NumberFormat: ImageVector by lazy { tableIcon("NumberFormat", stroke = listOf("M2.5 9l1.8-1.5V16", "M7.5 9.2c.3-1 1.1-1.6 2.1-1.6c1.1 0 1.9.8 1.9 1.8c0 2-4 3.2-4 6.6h4.2", "M14.5 8.2c.5-.4 1.1-.6 1.8-.6c1.2 0 2 .7 2 1.8s-.9 1.7-2 1.7c1.3 0 2.3.7 2.3 2c0 1.4-1.1 2.3-2.4 2.3c-.8 0-1.5-.3-2-.8"), accent = setOf("M7.5 9.2c.3-1 1.1-1.6 2.1-1.6c1.1 0 1.9.8 1.9 1.8c0 2-4 3.2-4 6.6h4.2", "M14.5 8.2c.5-.4 1.1-.6 1.8-.6c1.2 0 2 .7 2 1.8s-.9 1.7-2 1.7c1.3 0 2.3.7 2.3 2c0 1.4-1.1 2.3-2.4 2.3c-.8 0-1.5-.3-2-.8")) }

    /** A heavy B: bold. */
    val Bold: ImageVector by lazy { tableIcon("Bold", stroke = listOf("M7 4.5h6a3.75 3.75 0 0 1 0 7.5H7z", "M7 12h7a4 4 0 0 1 0 8H7z"), accent = setOf("M7 12h7a4 4 0 0 1 0 8H7z")) }

    /** A slanted I: italic. */
    val Italic: ImageVector by lazy { tableIcon("Italic", stroke = listOf("M10.5 4.5h7", "M6.5 19.5h7", "M14 4.5l-4 15"), accent = setOf("M14 4.5l-4 15")) }

    /** A tipped paint bucket and a drop: fill color. */
    val FillColor: ImageVector by lazy { tableIcon("FillColor", stroke = listOf("M11 3.5l7.5 7.5l-6.5 6.5a1.5 1.5 0 0 1-2.1 0l-5.4-5.4a1.5 1.5 0 0 1 0-2.1z", "M19.5 15.5c0 0-1.5 2-1.5 3a1.5 1.5 0 0 0 3 0c0-1-1.5-3-1.5-3z"), thin = listOf("M4.5 11.5h13.5"), shade = listOf("M4.5 11.5h13.5l-5.9 5.9a1.5 1.5 0 0 1-2.1 0z"), accent = setOf("M19.5 15.5c0 0-1.5 2-1.5 3a1.5 1.5 0 0 0 3 0c0-1-1.5-3-1.5-3z", "M4.5 11.5h13.5l-5.9 5.9a1.5 1.5 0 0 1-2.1 0z")) }

    /** Big A and small a: change case. */
    val ChangeCase: ImageVector by lazy { tableIcon("ChangeCase", stroke = listOf("M2.5 19L7.5 5l5 14", "M4.4 14h6.2", "M21.5 19v-6a2.8 2.8 0 0 0-5.2-1.4", "M21.5 15.4c-3.2-.5-6 0-6 1.9a1.9 1.9 0 0 0 3.5 1"), accent = setOf("M21.5 19v-6a2.8 2.8 0 0 0-5.2-1.4", "M21.5 15.4c-3.2-.5-6 0-6 1.9a1.9 1.9 0 0 0 3.5 1")) }

    /** A banknote: currency. */
    val Currency: ImageVector by lazy { tableIcon("Currency", stroke = listOf("M2.5 6.5h19v11h-19z", "M12 9.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5"), thin = listOf("M5.5 9.5v5", "M18.5 9.5v5"), accent = setOf("M12 9.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5")) }

    /** A paint brush: copy a column's format to another. */
    val FormatPainter: ImageVector by lazy { tableIcon("FormatPainter", stroke = listOf("M4.5 3.5h12v5h-12z", "M16.5 6h3v5h-8v3", "M10 14h3v6.5h-3z"), shade = listOf("M4.5 3.5h12v5h-12z"), accent = setOf("M4.5 3.5h12v5h-12z")) }

    /** A dashed box round a grid: select all. */
    val SelectAll: ImageVector by lazy { tableIcon("SelectAll", stroke = listOf("M3.5 7V3.5H7", "M17 3.5h3.5V7", "M20.5 17v3.5H17", "M7 20.5H3.5V17"), thin = listOf("M7.5 7.5h9v9h-9z", "M7.5 12h9", "M12 7.5v9"), accent = setOf("M3.5 7V3.5H7", "M17 3.5h3.5V7", "M20.5 17v3.5H17", "M7 20.5H3.5V17")) }

    /** An arrow into a cell: go to. */
    val GoTo: ImageVector by lazy { tableIcon("GoTo", stroke = listOf("M3 12h10", "M9.5 8.5L13 12l-3.5 3.5"), thin = listOf("M14.5 4.5h6v15h-6z"), shade = listOf("M14.5 9.5h6v5h-6z"), accent = setOf("M3 12h10", "M9.5 8.5L13 12l-3.5 3.5", "M14.5 9.5h6v5h-6z")) }

    /** A die: random numbers. */
    val Random: ImageVector by lazy { tableIcon("Random", stroke = listOf("M4.5 4.5h15v15h-15z"), fill = listOf("M7 8.5a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0-3 0z", "M10.5 12a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0-3 0z", "M14 15.5a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0-3 0z"), accent = setOf("M7 8.5a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0-3 0z", "M10.5 12a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0-3 0z", "M14 15.5a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0-3 0z")) }

    /** Crossing arrows: randomize the rows. */
    val Shuffle: ImageVector by lazy { tableIcon("Shuffle", stroke = listOf("M3 7h3.5c4.5 0 6.5 10 11 10H21", "M3 17h3.5c1.8 0 3.1-1.6 4.2-3.5", "M13.3 10.5c1.1-1.9 2.4-3.5 4.2-3.5H21", "M18.5 4.5L21 7l-2.5 2.5", "M18.5 14.5L21 17l-2.5 2.5"), accent = setOf("M3 7h3.5c4.5 0 6.5 10 11 10H21", "M18.5 14.5L21 17l-2.5 2.5")) }

    /** Arrows up and down: reverse the rows. */
    val Reverse: ImageVector by lazy { tableIcon("Reverse", stroke = listOf("M7.5 20V4", "M4.5 7l3-3l3 3", "M16.5 4v16", "M13.5 17l3 3l3-3"), accent = setOf("M16.5 4v16", "M13.5 17l3 3l3-3")) }

    /** A cell copied into the gap below it: fill blanks. */
    val FillBlanks: ImageVector by lazy { tableIcon("FillBlanks", stroke = listOf("M5 3.5h9v5H5z", "M18.5 6v10", "M16 13.5l2.5 2.5l2.5-2.5"), thin = listOf("M5 15.5h9v5H5z"), shade = listOf("M5 15.5h9v5H5z"), accent = setOf("M18.5 6v10", "M16 13.5l2.5 2.5l2.5-2.5", "M5 15.5h9v5H5z")) }

    /** Quoted text becoming a number: convert to numbers. */
    val TextToNumber: ImageVector by lazy { tableIcon("TextToNumber", stroke = listOf("M3.5 6.5v3", "M6.5 6.5v3", "M13 12h4", "M15.5 10l2 2l-2 2"), thin = listOf("M2.5 15.5h8v5h-8z"), fill = listOf("M19 6a1 1 0 1 0 2 0a1 1 0 1 0-2 0z"), accent = setOf("M13 12h4", "M15.5 10l2 2l-2 2", "M19 6a1 1 0 1 0 2 0a1 1 0 1 0-2 0z")) }

    /** One of each: unique values. */
    val Unique: ImageVector by lazy { tableIcon("Unique", stroke = listOf("M4 5h4", "M4 10h4", "M4 15h4", "M4 20h4", "M12 12h3", "M13.5 10.5L15 12l-1.5 1.5"), thin = listOf("M17 5h4v14h-4z"), accent = setOf("M12 12h3", "M13.5 10.5L15 12l-1.5 1.5", "M17 5h4v14h-4z")) }

    /** A column worked out from another: a new column from its numbers. */
    val NewColumn: ImageVector by lazy { tableIcon("NewColumn", stroke = listOf("M14.5 3.5h6v17h-6z", "M8.5 12h4", "M11 9.5l2 2.5l-2 2.5"), thin = listOf("M3.5 3.5h4v17h-4z", "M14.5 9h6", "M14.5 14.5h6"), accent = setOf("M14.5 3.5h6v17h-6z", "M8.5 12h4", "M11 9.5l2 2.5l-2 2.5")) }

    /** ƒ turning into a number: formulas to values. */
    val ToValues: ImageVector by lazy { tableIcon("ToValues", stroke = listOf("M8.5 4c-2-.5-3.2.5-3.2 2.6V16", "M3 9h5", "M12 12h4", "M14.5 10l2 2l-2 2"), shade = listOf("M18 8.5h3.5v7H18z"), accent = setOf("M12 12h4", "M14.5 10l2 2l-2 2", "M18 8.5h3.5v7H18z")) }

    /** A column with a slash: hide it. */
    val HideColumn: ImageVector by lazy { tableIcon("HideColumn", stroke = listOf("M3.5 3.5l17 17"), thin = listOf("M8 3.5h8v17H8z"), accent = setOf("M3.5 3.5l17 17")) }

    /** Columns spreading apart: show hidden columns. */
    val UnhideColumns: ImageVector by lazy { tableIcon("UnhideColumns", stroke = listOf("M12 4v16", "M3.5 12h5", "M6 9.5L3.5 12L6 14.5", "M20.5 12h-5", "M18 9.5l2.5 2.5l-2.5 2.5"), accent = setOf("M3.5 12h5", "M6 9.5L3.5 12L6 14.5", "M20.5 12h-5", "M18 9.5l2.5 2.5l-2.5 2.5")) }

    /** A calendar with today marked. */
    val Today: ImageVector by lazy { tableIcon("Today", stroke = listOf("M4.5 6h15v14.5h-15z", "M8 3.5v4", "M16 3.5v4"), thin = listOf("M4.5 10.5h15"), fill = listOf("M13.5 14h3v3h-3z"), accent = setOf("M13.5 14h3v3h-3z")) }
}
