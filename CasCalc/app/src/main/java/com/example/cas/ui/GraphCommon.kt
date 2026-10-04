package com.example.cas.ui

import com.example.cas.engine.Readout
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.foundation.layout.fillMaxHeight

import androidx.compose.foundation.gestures.detectVerticalDragGestures

import kotlinx.coroutines.launch

import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease

import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease

import androidx.compose.material.icons.filled.MoreVert

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight

import androidx.compose.material.icons.filled.KeyboardArrowDown

import androidx.compose.material.icons.filled.VisibilityOff

import androidx.compose.material.icons.filled.Visibility

import androidx.compose.material.icons.filled.Functions

import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.graphics.luminance

import androidx.compose.material.icons.filled.Notes

import androidx.compose.material.icons.filled.TableChart



import androidx.compose.material.icons.filled.SwapHoriz


import androidx.compose.material.icons.filled.ExpandMore

import androidx.compose.material.icons.filled.ExpandLess




import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.RowScope

import androidx.compose.foundation.gestures.detectDragGestures

import androidx.compose.runtime.key

import androidx.compose.ui.draw.shadow

import androidx.compose.material.icons.filled.IosShare

import androidx.compose.material.icons.filled.Delete

import androidx.compose.material.icons.filled.DragIndicator

import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Path
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.Button
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ScatterPlot
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.example.cas.editor.MathRow
import com.example.cas.editor.Sym
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import androidx.compose.ui.text.withStyle
import com.example.cas.ui.theme.CasFonts
import kotlin.math.abs
import kotlin.math.roundToInt

/** A function's color: the one picked for it, or the theme's color for its place in the list. */
@Composable
fun functionColor(f: PlotFunction): Color = f.customColor?.let { Color(it) } ?: plotColor(f.colorIndex)

/**
 * On the complex plane, ∮ loops, curves, points and constructions (points, lines, circles) are
 * drawn in a plain color, as on the 2D graph; the rest are functions, colored by a colormap.
 */
fun isComplexLine(f: PlotFunction) = f.contour != null || f.complexCurve != null || f.complexPoints != null || f.complexPath != null || f.geometry != null

/** The color of a point, curve or ∮ loop on the complex plane: its own, or the 2D graph's color for its slot. */
fun complexLineColor(f: PlotFunction): Color = f.customColor?.let { Color(it) }
    ?: PlotPalette.colors.takeIf { it.isNotEmpty() }?.let { Color(it[f.colorIndex % it.size]) } ?: Color.White

/**
 * The theme's line colors (as on the 2D graph), kept for drawing and exporting outside
 * composition; the complex plane refreshes it as it's shown.
 */
object PlotPalette {
    @Volatile var colors: List<Int> = emptyList()
}

/** A colormap's colors in order, for drawing it as a gradient. */
fun colormapStops(map: com.example.cas.graph.Colormap, n: Int = 32, reversed: Boolean = false): List<Color> =
    (0..n).map { k -> val t = k.toDouble() / n; Color(0xFF000000.toInt() or map.rgb(if (reversed) 1 - t else t)) }

/**
 * The colors for arg f on the complex plane. At the top, the chosen map large, with Reversed
 * to run it backwards. Then your colormaps as cards: tap one to use it; Edit turns them into a
 * list to drag into order or swipe away. Then every other map, by kind (or searched by name),
 * each with a star to add it to yours. On a tablet it's wide: yours on the left, all on the right.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColormapPickerDialog(
    current: com.example.cas.graph.Colormap,
    reversed: Boolean,
    onPick: (com.example.cas.graph.Colormap, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val tablet = isTabletLayout()
    var editing by remember { mutableStateOf(false) }
    var kind by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    val favorites = FavoriteColormaps.list.map { com.example.cas.graph.Colormap.byName(it) }.distinct()
    val perRow = if (tablet) 3 else 2

    @Composable
    fun heading(t: String, trailing: (@Composable () -> Unit)? = null) = Row(verticalAlignment = Alignment.CenterVertically) {
        Text(t, style = MaterialTheme.typography.titleSmall, color = colors.primary, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }

    // The chosen map, large, and which way round it runs.
    val chosen: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {
        Box(Modifier.fillMaxWidth().height(if (tablet) 64.dp else 52.dp).clip(RoundedCornerShape(18.dp)).background(Brush.horizontalGradient(colormapStops(current, reversed = reversed)))) {
            Text(
                current.label + if (reversed) " (reversed)" else "",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 14.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.35f)).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Reversed"), style = MaterialTheme.typography.bodyLarge)
                Text(tr("Run the colors the other way round"), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            androidx.compose.material3.Switch(checked = reversed, onCheckedChange = { onPick(current, it) })
        }
    }

    // Your maps: cards to pick from, or a list to reorder and remove.
    val yours: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {
        heading("Your colormaps") {
            TextButton(onClick = { editing = !editing }) { Text(if (editing) "Done editing" else "Edit") }
        }
        if (editing) {
            Text(tr("Drag ⠿ to reorder · swipe a map away to remove it"), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            ReorderableColumn(items = favorites, key = { it.name }, onMove = { from, to -> FavoriteColormaps.move(from, to) }) { map, handle ->
                SwipeToRemove(onRemove = { FavoriteColormaps.remove(map.name) }) {
                    ColormapRow(map, current, reversed, onPick, handle = handle)
                }
            }
        } else {
            favorites.chunked(perRow).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { map -> ColormapCard(map, map == current, reversed && map == current, Modifier.weight(1f), onClick = { onPick(map, false) }) }
                    repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }

    // Every map: by kind, or by name.
    val browse: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {
        heading("All colormaps")
        OutlinedTextField(
            value = search, onValueChange = { search = it }, singleLine = true,
            placeholder = { Text(tr("Search by name")) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            (listOf<String?>(null) + com.example.cas.graph.Colormap.CATEGORIES).forEach { c ->
                androidx.compose.material3.FilterChip(selected = kind == c, onClick = { kind = c }, label = { Text(c ?: "All") })
            }
        }
        val shown = com.example.cas.graph.Colormap.ALL.filter { m ->
            (kind == null || m.category == kind) && (search.isBlank() || m.label.contains(search.trim(), ignoreCase = true) || m.name.contains(search.trim(), ignoreCase = true))
        }
        if (shown.isEmpty()) Text(tr("No colormap by that name"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        shown.chunked(perRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { map ->
                    val mine = map in favorites
                    ColormapCard(
                        map, map == current, reversed && map == current, Modifier.weight(1f), onClick = { onPick(map, false) },
                        star = mine, onStar = { if (mine) FavoriteColormaps.remove(map.name) else FavoriteColormaps.add(map.name) },
                    )
                }
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceContainerHigh,
            modifier = (if (tablet) Modifier.width(1000.dp) else Modifier.fillMaxWidth(0.94f)).heightIn(max = if (tablet) 760.dp else 780.dp),
        ) {
            Column(Modifier.padding(top = 20.dp, bottom = 12.dp)) {
                Text(tr("Colormap"), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(12.dp))
                if (tablet) {
                    Row(Modifier.weight(1f, fill = false).padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        Column(Modifier.weight(0.9f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) { chosen(); yours() }
                        Column(Modifier.weight(1.1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) { browse() }
                    }
                } else {
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        chosen(); yours(); browse()
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(tr("Done")) }
                }
            }
        }
    }
}

/** A colormap as a card: its colors across the top and its name, outlined when it's the one in use. */
@Composable
private fun ColormapCard(
    map: com.example.cas.graph.Colormap,
    chosen: Boolean,
    reversed: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    star: Boolean? = null,
    onStar: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (chosen) colors.secondaryContainer else colors.surfaceContainerHighest)
            .then(if (chosen) Modifier.border(2.dp, colors.primary, RoundedCornerShape(16.dp)) else Modifier)
            .clickable(onClickLabel = "Use ${map.label}") { onClick() }
            .padding(8.dp)
            .semantics { contentDescription = map.label + if (chosen) ", in use" else "" },
    ) {
        Box(Modifier.fillMaxWidth().height(26.dp).clip(RoundedCornerShape(8.dp)).background(Brush.horizontalGradient(colormapStops(map, reversed = reversed))))
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(map.label, style = MaterialTheme.typography.labelLarge, maxLines = 1, color = if (chosen) colors.onSecondaryContainer else colors.onSurface, modifier = Modifier.weight(1f).padding(start = 2.dp))
            if (star != null) {
                Icon(
                    if (star) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (star) "Remove ${map.label} from yours" else "Add ${map.label} to yours",
                    tint = if (star) colors.primary else colors.onSurfaceVariant,
                    modifier = Modifier.size(28.dp).clip(CircleShape).clickable { onStar() }.padding(3.dp),
                )
            }
        }
    }
}

/**
 * A colormap in the Edit list: a drag handle, the name and its colors. Tap to use it.
 */
@Composable
private fun ColormapRow(
    map: com.example.cas.graph.Colormap,
    current: com.example.cas.graph.Colormap,
    reversed: Boolean,
    onPick: (com.example.cas.graph.Colormap, Boolean) -> Unit,
    handle: Modifier? = null,
) {
    val colors = MaterialTheme.colorScheme
    val chosen = map == current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (chosen) colors.secondaryContainer else colors.surfaceContainerHighest)
            .clickable(onClickLabel = "Use ${map.label}") { onPick(map, false) }
            .padding(end = 12.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (handle != null) Icon(Icons.Default.DragIndicator, contentDescription = tr("Drag to reorder"), tint = colors.onSurfaceVariant, modifier = handle.size(40.dp).padding(10.dp))
        Text(map.label, color = if (chosen) colors.onSecondaryContainer else colors.onSurface, style = MaterialTheme.typography.bodyLarge, maxLines = 1, modifier = Modifier.width(96.dp))
        Box(Modifier.weight(1f).height(18.dp).clip(RoundedCornerShape(9.dp)).background(Brush.horizontalGradient(colormapStops(map, reversed = chosen && reversed))))
    }
}

/**
 * A column whose rows move by dragging their handle (the modifier passed to [row]), following
 * the finger past as many rows as it goes, as the graph's list of lines does.
 */
@Composable
fun <T> ReorderableColumn(items: List<T>, key: (T) -> Any, onMove: (Int, Int) -> Unit, row: @Composable (T, Modifier) -> Unit) {
    val heights = remember { mutableStateMapOf<Any, Int>() }
    var dragging by remember { mutableStateOf<Any?>(null) }
    var offset by remember { mutableStateOf(0f) }
    val tap = rememberKeyTap()
    val gap = with(LocalDensity.current) { 4.dp.toPx() }
    val latest = androidx.compose.runtime.rememberUpdatedState(items)
    fun dragBy(k: Any, dy: Float) {
        offset += dy
        while (true) {
            val list = latest.value
            val i = list.indexOfFirst { key(it) == k }
            if (i < 0) break
            val below = list.getOrNull(i + 1)?.let(key)
            val above = list.getOrNull(i - 1)?.let(key)
            if (below != null && offset > (heights[below] ?: 0) / 2f + gap) {
                offset -= (heights[below] ?: 0) + gap; onMove(i, i + 1); tap()
            } else if (above != null && offset < -((heights[above] ?: 0) / 2f + gap)) {
                offset += (heights[above] ?: 0) + gap; onMove(i, i - 1); tap()
            } else break
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { item ->
            val k = key(item)
            key(k) {
                val lifted = dragging == k
                val handle = Modifier.pointerInput(k) {
                    detectDragGestures(
                        onDragStart = { tap(); dragging = k; offset = 0f },
                        onDragEnd = { dragging = null; offset = 0f },
                        onDragCancel = { dragging = null; offset = 0f },
                        onDrag = { change, amount -> change.consume(); dragBy(k, amount.y) },
                    )
                }
                Box(
                    Modifier
                        .zIndex(if (lifted) 1f else 0f)
                        .offset { IntOffset(0, if (lifted) offset.roundToInt() else 0) }
                        .graphicsLayer { if (lifted) { shadowElevation = 12f; scaleX = 1.02f; scaleY = 1.02f } }
                        .onGloballyPositioned { heights[k] = it.size.height },
                ) { row(item, handle) }
            }
        }
    }
}

/** The standard colors, at the top of the color picker: two rows of eight, bright then deep. */
val STANDARD_COLORS = listOf(
    Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835), Color(0xFF43A047),
    Color(0xFF00897B), Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFFD81B60),
    Color(0xFFB71C1C), Color(0xFFE65100), Color(0xFFF9A825), Color(0xFF1B5E20),
    Color(0xFF004D40), Color(0xFF0D47A1), Color(0xFF4A148C), Color(0xFF212121),
)

/**
 * A line's color, in order of how often it's wanted: the theme's colors and the standard ones
 * first, then any color from a saturation–brightness square and a hue slider, then exact
 * values (HSV, RGB, OKLab or hex), then the line's style and its point or fill options. On a
 * tablet it's a wide dialog in two columns (colors on the left, the rest on the right).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(
    initial: Color,
    onPick: (Color?) -> Unit,
    onDismiss: () -> Unit,
    /** For 2D lines: the current style ([com.example.cas.graph.LineStyle] by position) and thickness; null hides these. */
    lineStyle: Int? = null,
    thickness: Float = 2f,
    onStyle: (Int, Float) -> Unit = { _, _ -> },
    /** More options for the line (labels, connecting points, fill opacity), under the colors. */
    extra: (@Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit)? = null,
    /** The dialog's title ("Folder" when it names a folder as well). */
    title: String = "Color",
    /** Above the colors: a folder's name and moving it, so one dialog does all of a folder. */
    header: (@Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit)? = null,
) {
    var style by remember { mutableStateOf(lineStyle ?: 0) }
    var width by remember { mutableStateOf(thickness) }
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val tablet = isTabletLayout()
    var rgb by remember { mutableStateOf(ColorMath.fromArgb(initial.toArgb())) }
    // Hue kept apart, so it survives dragging to gray or black (where it can't be read back).
    var hue by remember { mutableStateOf(ColorMath.toHsv(rgb).first) }
    var mode by remember { mutableStateOf(0) } // 0 HSV, 1 RGB, 2 OKLab, 3 Hex
    fun fieldsFor(c: ColorMath.Rgb, m: Int): List<String> = when (m) {
        0 -> ColorMath.toHsv(c).toList().map { "%.0f".format(it) }
        1 -> listOf(c.r, c.g, c.b).map { it.toString() }
        2 -> ColorMath.toOklab(c).toList().map { "%.3f".format(it) }
        else -> listOf(ColorMath.hex(c))
    }
    var fields by remember { mutableStateOf(fieldsFor(rgb, mode)) }
    // Something other than typing changed the color: show its numbers.
    fun set(c: ColorMath.Rgb, keepHue: Boolean = false) {
        rgb = c; fields = fieldsFor(c, mode)
        if (!keepHue && ColorMath.toHsv(c).second > 0.5) hue = ColorMath.toHsv(c).first
    }
    fun typed(k: Int, text: String) {
        fields = fields.toMutableList().also { it[k] = text }
        val n = fields.map { it.trim().replace("−", "-").replace(",", ".").toDoubleOrNull() }
        val parsed = runCatching {
            when (mode) {
                0 -> ColorMath.fromHsv(n[0]!!, n[1]!!, n[2]!!)
                1 -> ColorMath.Rgb(n[0]!!.toInt(), n[1]!!.toInt(), n[2]!!.toInt())
                2 -> ColorMath.fromOklab(n[0]!!, n[1]!!, n[2]!!)
                else -> ColorMath.parseHex(fields[0])
            }
        }.getOrNull()
        if (parsed != null) { rgb = parsed; if (ColorMath.toHsv(parsed).second > 0.5) hue = ColorMath.toHsv(parsed).first }
    }
    val picked = Color(rgb.argb)
    val theme = plotColors(colors)

    @Composable
    fun heading(t: String) = Text(t, style = MaterialTheme.typography.labelLarge, color = colors.primary)

    @Composable
    fun swatch(c: Color, label: String) {
        val on = c.toArgb() == picked.toArgb()
        Box(
            Modifier
                .size(if (tablet) 40.dp else 34.dp)
                .clip(CircleShape)
                .background(c)
                .then(if (on) Modifier.border(3.dp, colors.surface, CircleShape).border(5.dp, colors.onSurface, CircleShape) else Modifier.border(1.dp, colors.outlineVariant, CircleShape))
                .clickable(onClickLabel = label) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); set(ColorMath.fromArgb(c.toArgb())) },
        )
    }

    // 1. Ready-made colors: the theme's, then the standard ones.
    val swatches: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {
        heading("Theme")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            theme.forEach { swatch(it, "Use this theme color") }
        }
        heading("Standard")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 8) {
            STANDARD_COLORS.forEach { swatch(it, "Use this color") }
        }
    }

    // 2. Any color: saturation across, brightness up, and the hue under it.
    val space: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {
        heading("Any color")
        val (_, sat, v) = ColorMath.toHsv(rgb)
        val pure = Color.hsv(hue.toFloat().mod(360f), 1f, 1f)
        androidx.compose.foundation.Canvas(
            Modifier
                .fillMaxWidth()
                .height(if (tablet) 220.dp else 170.dp)
                .clip(RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    fun at(o: Offset) = set(ColorMath.fromHsv(hue, (o.x / size.width).coerceIn(0f, 1f) * 100.0, (1 - o.y / size.height).coerceIn(0f, 1f) * 100.0), keepHue = true)
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        at(down.position)
                        drag(down.id) { ch -> ch.consume(); at(ch.position) }
                    }
                }
                .semantics { contentDescription = tr("Saturation and brightness") },
        ) {
            drawRect(Brush.horizontalGradient(listOf(Color.White, pure)))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            val c = Offset((sat / 100).toFloat() * size.width, (1 - (v / 100).toFloat()) * size.height)
            drawCircle(Color.White, 10.dp.toPx(), c, style = Stroke(3.dp.toPx()))
            drawCircle(Color.Black.copy(alpha = 0.4f), 11.5.dp.toPx(), c, style = Stroke(1.dp.toPx()))
        }
        ColorTrack("Hue", (hue / 360).toFloat(), Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f % 360f, 1f, 1f) })) {
            hue = it * 360.0
            val (_, s0, v0) = ColorMath.toHsv(rgb)
            set(ColorMath.fromHsv(hue, s0, v0), keepHue = true)
        }
    }

    // 3. Exact values.
    val exact: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {
        heading("Exact values")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("HSV", "RGB", "OKLab", "Hex").forEachIndexed { k, name ->
                SegmentedButton(
                    selected = mode == k,
                    onClick = { mode = k; fields = fieldsFor(rgb, k) },
                    shape = SegmentedButtonDefaults.itemShape(k, 4),
                    icon = {},
                    label = { Text(name, maxLines = 1) },
                )
            }
        }
        when (mode) {
            1 -> {
                fun track(f: (Int) -> ColorMath.Rgb) = Brush.horizontalGradient(listOf(Color(f(0).argb), Color(f(255).argb)))
                ColorTrack("Red", rgb.r / 255f, track { rgb.copy(r = it) }) { set(rgb.copy(r = (it * 255).roundToInt())) }
                ColorTrack("Green", rgb.g / 255f, track { rgb.copy(g = it) }) { set(rgb.copy(g = (it * 255).roundToInt())) }
                ColorTrack("Blue", rgb.b / 255f, track { rgb.copy(b = it) }) { set(rgb.copy(b = (it * 255).roundToInt())) }
            }
            2 -> {
                val (l, a, b) = ColorMath.toOklab(rgb)
                // a and b run from −0.4 to 0.4.
                fun ab(t: Float) = (t - 0.5) * 0.8
                fun track(f: (Double) -> ColorMath.Rgb) = Brush.horizontalGradient((0..6).map { k -> Color(f(k / 6.0).argb) })
                ColorTrack("Lightness L", l.toFloat(), track { ColorMath.fromOklab(it, a, b) }) { set(ColorMath.fromOklab(it.toDouble(), a, b)) }
                ColorTrack("Green–red a", (a / 0.8 + 0.5).toFloat(), track { ColorMath.fromOklab(l, ab(it.toFloat()), b) }) { set(ColorMath.fromOklab(l, ab(it), b)) }
                ColorTrack("Blue–yellow b", (b / 0.8 + 0.5).toFloat(), track { ColorMath.fromOklab(l, a, ab(it.toFloat())) }) { set(ColorMath.fromOklab(l, a, ab(it))) }
            }
            else -> Unit
        }
        val labels = when (mode) {
            0 -> listOf("H (°)", "S (%)", "V (%)")
            1 -> listOf("R", "G", "B")
            2 -> listOf("L", "a", "b")
            else -> listOf("#RRGGBB")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEachIndexed { k, label ->
                OutlinedTextField(fields.getOrElse(k) { "" }, { typed(k, it) }, singleLine = true, label = { Text(tr(label)) }, modifier = Modifier.weight(1f))
            }
        }
    }

    // 4. The line itself, drawn in the color as it will look.
    val line: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {
        if (lineStyle != null) {
            heading("Line")
            com.example.cas.graph.LineStyle.entries.chunked(3).forEach { group -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                group.forEach { ls ->
                    val k = ls.ordinal; val name = ls.label
                    val on = style == k
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(if (on) colors.secondaryContainer else colors.surfaceContainerHigh)
                            .clickable(onClickLabel = name) { style = k }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(12.dp).padding(horizontal = 14.dp)) {
                            val w = 3.dp.toPx()
                            drawLine(picked, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), w, cap = StrokeCap.Round,
                                pathEffect = ls.pattern(w.toDouble())?.let { d -> PathEffect.dashPathEffect(FloatArray(d.size) { d[it].toFloat() }) })
                        }
                        Text(name, style = MaterialTheme.typography.labelMedium, color = if (on) colors.onSecondaryContainer else colors.onSurfaceVariant, maxLines = 1)
                    }
                }
            } }
            Text("Thickness: ${"%.1f".format(width)} dp", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            ExpressiveSlider(value = width, onValueChange = { width = it }, valueRange = 1f..8f, modifier = Modifier.fillMaxWidth().semantics { contentDescription = tr("Line thickness") })
        }
        extra?.invoke(this)
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceContainerHigh,
            modifier = (if (tablet) Modifier.width(920.dp) else Modifier.fillMaxWidth(0.94f)).heightIn(max = if (tablet) 720.dp else 760.dp),
        ) {
            Column(Modifier.padding(top = 20.dp, bottom = 12.dp)) {
                // Title, and the color as it is now with its hex code.
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr(title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    Row(
                        Modifier.clip(CircleShape).background(colors.surfaceContainerHighest).padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(picked).border(1.dp, colors.outlineVariant, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(ColorMath.hex(rgb), style = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 14.sp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (tablet) {
                    Row(Modifier.weight(1f, fill = false).padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) { header?.invoke(this); swatches(); space() }
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) { exact(); line() }
                    }
                } else {
                    Column(
                        Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) { header?.invoke(this); swatches(); space(); line(); exact() }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { if (lineStyle != null) onStyle(0, 3f); onPick(null) }) { Text(tr("Default")) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(tr("Cancel")) }
                    TextButton(onClick = { if (lineStyle != null) onStyle(style, width); onPick(picked) }) { Text(tr("Done")) }
                }
            }
        }
    }
}

/** An expressive slider whose track is a gradient showing what moving it does. */
@Composable
private fun ColorTrack(label: String, fraction: Float, track: Brush, onChange: (Float) -> Unit) {
    Column {
        Text(tr(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ExpressiveSlider(
            value = fraction,
            onValueChange = onChange,
            valueRange = 0f..1f,
            trackBrush = track,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
        )
    }
}

/** Colors for successive functions, all from the theme (Material You's when it's on). */
@Composable
fun plotColor(index: Int): Color = plotColors(MaterialTheme.colorScheme)[index % GraphViewModel.PLOT_COLOR_COUNT]

/**
 * The graph colors in a color scheme, in the order lines take them when no color has been
 * picked: the theme's primary, secondary, tertiary and error colors, then its surface's own
 * ink (the inverse surface, so it shows on the plot), and round again.
 */
fun plotColors(c: androidx.compose.material3.ColorScheme): List<Color> =
    listOf(c.primary, c.secondary, c.tertiary, c.error, c.inverseSurface)

/** A line's name as written (text with $math$): the one it was given, or its default. */
fun legendSource(f: PlotFunction): String =
    // Constructions stay out of the legend unless renamed.
    f.name ?: if (f.geometry != null) "" else com.example.cas.graph.Legend.defaultSource(f.editor.root, isData = f.plot is Plot2DKind.PointList || f.table != null)

/** One line of the legend on screen: the name (text with $math$) and how its sample is drawn. */
class ScreenLegendEntry(
    val source: String,
    val color: Color,
    val line: Boolean = true,
    /** The line's style ([com.example.cas.graph.LineStyle] by position). */
    val style: Int = 0,
    val marker: com.example.cas.graph.Marker? = null,
    val fill: Float? = null,
    /** A colormap's colors (the complex plane) or a surface's shades (3D), as a strip. */
    val strip: List<Color>? = null,
    /** A vector field: arrows in these colors, short to long (one: a single arrow). */
    val arrows: List<Color>? = null,
)

/**
 * The legend, SciencePlots-style: a short sample of each line and its name in LaTeX's font,
 * in the order of the list, on a faint panel so it reads over the graph. Tap it to fold it to
 * a small "Legend" chip, and again to open it.
 */
@Composable
fun GraphLegend(entries: List<ScreenLegendEntry>, modifier: Modifier = Modifier) {
    if (!AppSettings.showLegend || entries.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    var folded by remember { mutableStateOf(false) }
    Column(
        modifier
            .widthIn(max = 260.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface.copy(alpha = 0.82f))
            .clickable(onClickLabel = if (folded) "Show the legend" else "Fold the legend") { folded = !folded }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (folded) {
            Text(tr("Legend"), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            return@Column
        }
        entries.take(12).forEach { e ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Canvas(Modifier.size(width = 24.dp, height = 14.dp)) {
                    val mid = size.height / 2
                    e.strip?.let { strip ->
                        val w = size.width / strip.size
                        strip.forEachIndexed { k, c -> drawRect(c, topLeft = Offset(k * w, mid - 4.dp.toPx()), size = androidx.compose.ui.geometry.Size(w + 0.5f, 8.dp.toPx())) }
                    }
                    e.fill?.let { a -> drawRect(e.color.copy(alpha = a), topLeft = Offset(0f, mid - 5.dp.toPx()), size = androidx.compose.ui.geometry.Size(size.width, 10.dp.toPx())) }
                    if (e.line) drawLine(
                        e.color, Offset(0f, mid), Offset(size.width, mid), 2.5.dp.toPx(), cap = StrokeCap.Round,
                        pathEffect = com.example.cas.graph.LineStyle.of(e.style).pattern(2.5.dp.toPx().toDouble())?.let { d -> PathEffect.dashPathEffect(FloatArray(d.size) { d[it].toFloat() }) },
                    )
                    e.marker?.let { m -> drawMarker(m, Offset(size.width / 2, mid), 4.dp.toPx(), e.color) }
                    e.arrows?.let { cs ->
                        val n = cs.size
                        val slot = size.width / n
                        cs.forEachIndexed { k, c ->
                            val len = if (n == 1) size.width else slot * (0.45f + 0.5f * k / (n - 1).coerceAtLeast(1))
                            val ax = k * slot + (slot - len) / 2
                            val head = minOf(5.dp.toPx(), len * 0.5f)
                            drawLine(c, Offset(ax, mid), Offset(ax + len - head * 0.7f, mid), 1.6.dp.toPx(), cap = StrokeCap.Round)
                            drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(ax + len, mid); lineTo(ax + len - head, mid - head * 0.45f); lineTo(ax + len - head, mid + head * 0.45f); close() }, c)
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                val row = remember(e.source) { com.example.cas.graph.Legend.row(e.source) }
                Box(Modifier.horizontalScroll(rememberScrollState())) {
                    MathView(row, 14.sp, colors.onSurface, modifier = Modifier.semantics { contentDescription = com.example.cas.graph.Legend.plain(e.source) })
                }
            }
        }
    }
}

/** A 2D line's legend entry on screen. */
fun screenLegendEntry2D(f: PlotFunction, color: Color): ScreenLegendEntry = when (f.plot) {
    is Plot2DKind.PointList, is Plot2DKind.Point ->
        ScreenLegendEntry(legendSource(f), color, line = f.connectPoints || f.closedShape, marker = com.example.cas.graph.Marker.of(f.pointShape))
    is Plot2DKind.Region -> ScreenLegendEntry(legendSource(f), color, fill = f.fillOpacity)
    is Plot2DKind.Field -> ScreenLegendEntry(legendSource(f), color, line = false, strip = colormapStops(f.colormap, 8, f.colormapReversed))
    is Plot2DKind.VectorField -> ScreenLegendEntry(legendSource(f), color, line = false,
        arrows = if (f.arrowsByLength) colormapStops(f.colormap, 2, f.colormapReversed) else listOf(color))
    else -> ScreenLegendEntry(legendSource(f), color, style = f.lineStyle)
}

/**
 * A construction's value as math: a letter standing for a quantity (r) in italics, and a number
 * in scientific notation as 2.5 × 10⁶, the power raised, instead of 2.500e+06.
 */
fun geometryValueText(text: String): androidx.compose.ui.text.AnnotatedString = androidx.compose.ui.text.buildAnnotatedString {
    val sci = Regex("""(\d+(?:\.\d+)?)e([+\-−]?)(\d+)""")
    var k = 0
    fun plain(t: String) {
        // Single letters on their own (r = 2) are quantities, so italic; words (length) stay upright.
        // The italic is Computer Modern's own italic face, not the upright one slanted.
        var j = 0
        Regex("""(?<![\p{L}])\p{L}(?![\p{L}(])""").findAll(t).forEach { m ->
            append(t.substring(j, m.range.first))
            withStyle(androidx.compose.ui.text.SpanStyle(fontFamily = CasFonts.CmItalic, fontStyle = androidx.compose.ui.text.font.FontStyle.Normal)) { append(m.value) }
            j = m.range.last + 1
        }
        append(t.substring(j))
    }
    for (m in sci.findAll(text)) {
        plain(text.substring(k, m.range.first))
        val mantissa = m.groupValues[1].let { if ('.' in it) it.trimEnd('0').trimEnd('.') else it }
        val power = (if (m.groupValues[2] == "-" || m.groupValues[2] == "−") "−" else "") + m.groupValues[3].trimStart('0').ifEmpty { "0" }
        append(mantissa); append(" × 10")
        withStyle(androidx.compose.ui.text.SpanStyle(baselineShift = androidx.compose.ui.text.style.BaselineShift.Superscript, fontSize = 0.7.em)) { append(power) }
        k = m.range.last + 1
    }
    plain(text.substring(k))
}

/** Short number for labels: 1.4142, −3, 2.5e+06. */
fun shortNumber(v: Double): String {
    if (!v.isFinite()) return "undefined"
    if (abs(v) < 1e-10) return "0"
    if (abs(v) >= 1e6 || abs(v) < 1e-4) return String.format(java.util.Locale.US, "%.3e", v).replace("-", "−")
    val r = (v * 10000).roundToInt() / 10000.0
    val s = if (r == Math.rint(r)) r.toLong().toString() else r.toString()
    return s.replace("-", "−")
}

/**
 * The list of functions under a graph, each typed in math notation with a
 * color dot (tap it to hide the curve), plus sliders for parameters.
 * While a function is being edited, only that one is shown so the keypad fits.
 */
@Composable
fun FunctionList(vm: GraphViewModel, outputLabel: String, modifier: Modifier = Modifier, tablet: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val editing = vm.active
    // While a line is being edited only that line shows, unless the keypad is hidden: then the
    // whole list is back, as it is after Enter. On a tablet the whole list always shows, in a
    // column of its own beside the keyboard.
    // While typing with the graph away (a phone), every line shows, in a list that scrolls.
    val editingNow = editing?.takeIf { !vm.keypadHidden && !tablet && !vm.typingFocus }
    val shown = if (editingNow != null) listOf(editingNow) else vm.functions.toList()
    Column(
        modifier
            .fillMaxWidth()
            .then(if (tablet || vm.typingFocus) Modifier.fillMaxHeight() else Modifier.heightIn(max = if (editingNow != null) 200.dp else 280.dp))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (editingNow != null) {
            shown.forEach { f -> FunctionRow(vm, f, outputLabel) }
        } else {
            // Drag a row by its handle (or hold it anywhere) to move it; swipe it away to delete it.
            // Lines in a closed folder are left out.
            // Items are indented by how many folders they're in.
            ReorderableRows(vm, hidden = { f -> vm.hiddenByFolder(f) }, folders = { f -> vm.enclosing(f) }) { f, handle ->
                FunctionRow(vm, f, outputLabel, handle)
            }
        }
        vm.tableFor?.let { f -> PointTableDialog(vm, f, onDismiss = { vm.tableFor = null }) }
        // "Ask before deleting" (settings) covers graph lines too.
        vm.pendingRemoval?.let { f ->
            AlertDialog(
                onDismissRequest = { vm.pendingRemoval = null },
                title = { Text(if (f.isFolder) "Delete this folder?" else if (f.isText) "Delete this note?" else "Delete this line?") },
                text = { Text(if (f.isFolder) "The lines in it stay, outside the folder." else "It will be removed from the graph.") },
                confirmButton = { TextButton(onClick = { vm.pendingRemoval = null; vm.remove(f) }) { Text(tr("Delete")) } },
                dismissButton = { TextButton(onClick = { vm.pendingRemoval = null }) { Text(tr("Cancel")) } },
            )
        }
        val params = shown.filter { it.visible }.flatMap { it.parameters }.distinct() - vm.definedLetters
        params.forEach { p -> ParameterSlider(vm, p) }
    }
    // Moves playing sliders on every frame.
    val anyPlaying = vm.animating
    LaunchedEffect(anyPlaying) {
        if (!anyPlaying) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                vm.advance((now - last) / 1e9)
                last = now
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FunctionRow(vm: GraphViewModel, f: PlotFunction, outputLabel: String, handle: Modifier? = null) {
    if (f.isText) { TextRow(vm, f, handle); return }
    val colors = MaterialTheme.colorScheme
    val active = vm.active === f
    val color = if (vm.isComplex && isComplexLine(f)) complexLineColor(f) else functionColor(f)
    // On the complex plane a function's dot shows its colormap round a ring; loops and curves keep a plain color.
    // (A field f(x, y) on the 2D graph is drawn with a colormap too.)
    val colormapDot = (vm.isComplex && !isComplexLine(f)) || f.plot is Plot2DKind.Field
    var picking by remember { mutableStateOf(false) }
    if (picking && colormapDot) {
        ColormapPickerDialog(f.colormap, f.colormapReversed, onPick = { map, rev -> vm.setColormap(f, map, rev) }, onDismiss = { picking = false })
    } else if (picking) {
        ColorPickerDialog(
            initial = color,
            onPick = { vm.setColor(f, it?.toArgb()); picking = false },
            onDismiss = { picking = false },
            // 2D lines also get style and thickness (points and lists of points don't).
            lineStyle = if ((vm.plotVars == listOf("x") || vm.isComplex && f.geometry != null) && f.plot !is Plot2DKind.Point && f.plot !is Plot2DKind.PointList && f.plot !is Plot2DKind.VectorField && !isGeometryPoint(vm, f)) f.lineStyle else null,
            thickness = f.thickness,
            onStyle = { st, w -> vm.setStyle(f, st, w) },
            extra = lineOptions(vm, f),
        )
    }
    // A list of points (a data set) isn't edited here: its table (the button on the row) edits it.
    // Tapping it does nothing, so no keyboard opens over thousands of numbers.
    val isData = vm.isDataLine(f)
    var renaming by remember { mutableStateOf(false) }
    if (renaming) RenameDialog(f, onDone = { name -> vm.rename(f, name); renaming = false }, onDismiss = { renaming = false })
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) colors.surfaceContainerHigh else colors.surfaceContainer)
            // The line being edited is outlined.
            .then(if (active) Modifier.border(2.dp, colors.primary, RoundedCornerShape(20.dp)) else Modifier)
            // Tap to edit; hold to rename it in the legend.
            .combinedClickable(onClickLabel = if (isData) null else "Edit this function", onLongClickLabel = "Rename", onLongClick = { renaming = true }) { if (!isData) vm.edit(f) }
            .padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Color dot: filled when shown, a ring when hidden.
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    // Tap to hide or show; long-press to pick a color (a colormap for f(z) on the complex plane).
                    .combinedClickable(
                        onClickLabel = if (f.visible) "Hide" else "Show",
                        onClick = { vm.toggleVisible(f) },
                        onLongClickLabel = if (colormapDot) "Change colormap" else "Change color",
                        onLongClick = { picking = true },
                    )
                    .semantics { contentDescription = if (f.visible) "Shown" else "Hidden" },
                contentAlignment = Alignment.Center,
            ) {
                if (colormapDot || (f.plot is Plot2DKind.VectorField && f.arrowsByLength)) {
                    val ring = Brush.sweepGradient(colormapStops(f.colormap, reversed = f.colormapReversed))
                    Box(
                        Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .then(if (f.visible) Modifier.background(ring) else Modifier.border(2.dp, ring, CircleShape)),
                    )
                } else {
                    Box(
                        Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            // A white loop gets an outline so its dot shows on light surfaces.
                            .then(if (f.visible) Modifier.background(color).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape) else Modifier.border(2.dp, color, CircleShape)),
                    )
                }
            }
            // "y =", "r =", "(x, y) =", or nothing when the line is an equation or inequality itself.
            val typed = f.editor.root.items.any { (it as? Sym)?.text in setOf("=", "<", ">", "≤", "≥") }
            val label = when {
                // Definitions (a = 3), curves, contour integrals and typed equations carry their own "=".
                f.definition != null || f.complexCurve != null || f.contour != null -> null
                vm.plotVars != listOf("x") -> if (typed) null else "$outputLabel ="
                f.plot != null -> f.plot?.label
                typed -> null
                else -> "y ="
            }
            // No "y =", "z =" or "f(z) =": the line is read from what's typed, as in Desmos.
            // Long lines scroll sideways; a swipe that starts on the row's edges (the dot on the
            // left, the × and handle on the right) deletes the line instead.
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.weight(1f)) {
            val viewport = maxWidth
            val across = rememberScrollState()
            // With the cursor at the very end, the end of the line is scrolled into view.
            LaunchedEffect(f.version, active) {
                if (active && f.editor.row === f.editor.root && f.editor.index == f.editor.root.items.size) across.animateScrollTo(across.maxValue)
            }
            Box(Modifier.horizontalScroll(across)) {
                // A long list (an imported file) shows its start and how many points, until it's edited.
                val items = f.editor.root.items
                val shortened = if ((isData || !active) && items.size > (if (isData) 60 else 300)) remember(f.version) {
                    val cut = items.take(if (isData) 60 else 120).let { head -> head.subList(0, head.indexOfLast { (it as? Sym)?.text == ")" } + 1) }
                    val points = (f.plot as? Plot2DKind.PointList)?.xs?.size
                    MathRow((cut.map { com.example.cas.editor.MathCodec.decode(com.example.cas.editor.MathCodec.encode(MathRow(mutableListOf(it)))).items.single() } +
                        listOf(Sym(","), Sym("…"), Sym("]"))).toMutableList()) to points
                } else null
                // At least as wide as the row, so a tap after the end of the math puts the cursor at
                // the end of the line, and one in the margin before it at the start, to add something
                // in front (taps on the math itself are handled by it first).
                Column(
                    Modifier.widthIn(min = viewport).pointerInput(f, isData) {
                        detectTapGestures { o ->
                            val atStart = o.x < 14.dp.toPx()
                            if (isData) Unit
                            else if (f.editor.root.items.size <= 300 || vm.active === f) vm.tapAt(f, f.editor.root, if (atStart) 0 else f.editor.root.items.size)
                            else vm.edit(f)
                        }
                    },
                ) {
                    shortened?.second?.let { n ->
                        Text("$n points", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                    // The margin before the math: a tap here puts the cursor at the start.
                    Spacer(Modifier.width(14.dp))
                    MathView(
                        row = shortened?.first ?: f.editor.root,
                        fontSize = 22.sp,
                        color = colors.onSurface,
                        accent = colors.primary,
                        cursorRow = if (active) f.editor.row else null,
                        cursorIndex = f.editor.index,
                        version = f.version,
                        // The shortened copy isn't the line itself: a tap opens the whole line.
                        onTap = if (isData) null else if (shortened != null) ({ _, _ -> vm.edit(f) }) else ({ r, i -> vm.tapAt(f, r, i) }),
                    )
                    // Room past the end, so the cursor there shows and a tap there reaches it.
                    Spacer(Modifier.width(16.dp))
                    }
                }
            }
            }
            // Fit sits at the end of the row once the graph has a list of points.
            if (handle != null && vm.canFit(f)) FitButton(vm, f)
            // A list of points opens as a table (its columns, x, y and error bars), right here.
            if (vm.plotVars == listOf("x") && (f.plot is Plot2DKind.PointList || f.table != null)) {
                IconButton(onClick = { vm.tableFor = f }) {
                    Icon(Icons.Default.TableChart, contentDescription = tr("Edit as a table"), tint = colors.primary)
                }
            }
            IconButton(onClick = { vm.requestRemove(f) }) {
                Icon(Icons.Default.Close, contentDescription = tr("Remove"), tint = colors.onSurfaceVariant)
            }
            // Drag here to move the line up or down the list.
            if (handle != null) {
                Icon(
                    Icons.Default.DragIndicator,
                    contentDescription = tr("Drag to reorder"),
                    tint = colors.onSurfaceVariant,
                    modifier = handle.size(40.dp).padding(8.dp),
                )
            }
        }
        f.error?.let {
            MathText(it, color = colors.error, style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 13.sp), modifier = Modifier.padding(start = 48.dp, bottom = 2.dp))
        }
        // A construction's value (d = 5, P = (1, 2), an angle).
        if (f.error == null) f.valueText?.let {
            Text(geometryValueText(it), color = colors.onSurfaceVariant, style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 15.sp), modifier = Modifier.padding(start = 48.dp, bottom = 4.dp))
        }
    }
}

/**
 * Renaming a line for the legend: text, with math between dollar signs, shown as it will look.
 * Empty (or Default) goes back to the line's own name: its math, or "Data" for a list.
 */
@Composable
private fun RenameDialog(f: PlotFunction, onDone: (String?) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var text by remember { mutableStateOf(legendSource(f)) }
    val preview = remember(text) { com.example.cas.graph.Legend.row(text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Name in the legend")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text(tr("Name")) },
                    supportingText = { Text(tr("Math between \$ signs, in LaTeX: \$\\sin x\$")) },
                    textStyle = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 15.sp),
                    modifier = Modifier.fillMaxWidth(),
                )
                // How it will look.
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surfaceContainerHighest)
                        .horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    if (text.isBlank()) Text(tr("(no name: left out of the legend)"), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    else MathView(preview, 20.sp, colors.onSurface)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(text.trim()) }) { Text(tr("Save")) } },
        dismissButton = {
            Row {
                TextButton(onClick = { onDone(null) }) { Text(tr("Default")) }
                TextButton(onClick = onDismiss) { Text(tr("Cancel")) }
            }
        },
    )
}

/** Desmos-like options for a 2D line: labels on points, joining a list's points, a region's fill opacity. */
@OptIn(ExperimentalLayoutApi::class)
private fun lineOptions(vm: GraphViewModel, f: PlotFunction): (@Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit)? {
    // A construction (in any of the three graphs): its own options.
    if (f.geometry != null) return { GeometryOptions(vm, f) }
    if (vm.plotVars != listOf("x") && !vm.isComplex) return null
    val kind = f.plot
    val points = kind is Plot2DKind.Point || kind is Plot2DKind.PointList || f.complexPoints != null
    // Several points (a list): they can be joined, or closed into a polygon.
    val many = kind is Plot2DKind.PointList || (f.complexPoints?.size ?: 0) > 1
    val region = kind is Plot2DKind.Region
    if (kind is Plot2DKind.VectorField) return { ArrowOptions(vm, f) }
    if (kind is Plot2DKind.Geometry) return { GeometryOptions(vm, f) }
    if (!points && !region) return null
    return {
        val colors = MaterialTheme.colorScheme
        if (points) {
            Text(tr("Point"), style = MaterialTheme.typography.labelLarge, color = colors.primary)
            MarkChooser(vm, f)
            Text("Size: ${String.format(java.util.Locale.US, "%.1f", f.pointSize)} dp", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            // In tenths of a dp.
            ExpressiveSlider(value = f.pointSize, onValueChange = { vm.setOptions(f, size = kotlin.math.round(it * 10f) / 10f) }, valueRange = 1f..16f, modifier = Modifier.fillMaxWidth().semantics { contentDescription = tr("Point size") })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Show coordinates"), modifier = Modifier.weight(1f), color = colors.onSurface)
                androidx.compose.material3.Switch(checked = f.showLabel, onCheckedChange = { vm.setOptions(f, label = it) })
            }
        }
        if (many) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Join the points"), modifier = Modifier.weight(1f), color = colors.onSurface)
                androidx.compose.material3.Switch(checked = f.connectPoints, onCheckedChange = { vm.setOptions(f, connect = it) })
            }
            // A polygon: joined all the way round and filled.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Closed shape (polygon)"), modifier = Modifier.weight(1f), color = colors.onSurface)
                androidx.compose.material3.Switch(checked = f.closedShape, onCheckedChange = { vm.setOptions(f, closed = it) })
            }
            if (f.closedShape) {
                Text("Fill opacity: ${(f.fillOpacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                ExpressiveSlider(value = f.fillOpacity, onValueChange = { vm.setOptions(f, opacity = it) }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth().semantics { contentDescription = tr("Fill opacity") })
            }
            if (kind is Plot2DKind.PointList) androidx.compose.material3.OutlinedButton(onClick = { vm.tableFor = f }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(tr("Edit as a table"))
            }
        }
        if (region) {
            Text("Fill opacity: ${(f.fillOpacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            ExpressiveSlider(value = f.fillOpacity, onValueChange = { vm.setOptions(f, opacity = it) }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth().semantics { contentDescription = tr("Fill opacity") })
        }
    }
}

/** A geometry construction that is a point (or points): it has a mark, not a line style. */
private fun isGeometryPoint(vm: GraphViewModel, f: PlotFunction): Boolean = f.geometry != null && vm.constructionKind(f) == 'P'

/** A point's mark: its shape (each chip draws it), and filled or hollow. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MarkChooser(vm: GraphViewModel, f: PlotFunction) {
    val colors = MaterialTheme.colorScheme
    val current = com.example.cas.graph.Marker.of(f.pointShape)
    val filled = !current.hollow
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        com.example.cas.graph.Marker.bases.forEach { b ->
            val m = b.filled(filled)
            val chosen = current.base == b
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (chosen) colors.secondaryContainer else colors.surfaceContainerHigh)
                    .clickable(onClickLabel = b.label) { vm.setOptions(f, shape = m.ordinal) }
                    .semantics { contentDescription = b.label + if (chosen) ", chosen" else "" },
                contentAlignment = Alignment.Center,
            ) {
                val tint = if (chosen) colors.onSecondaryContainer else colors.onSurfaceVariant
                androidx.compose.foundation.Canvas(Modifier.size(20.dp)) { drawMarker(m, center, size.minDimension * 0.36f, tint) }
            }
        }
    }
    if (current.fillable) Row(verticalAlignment = Alignment.CenterVertically) {
        Text(tr("Filled"), modifier = Modifier.weight(1f), color = colors.onSurface)
        androidx.compose.material3.Switch(checked = filled, onCheckedChange = { vm.setOptions(f, shape = current.filled(it).ordinal) })
    }
}

/**
 * A construction's options: for points, the mark and its size, its name and coordinates shown,
 * and (on a path) moving by itself; for filled shapes (polygons, sectors, angles), the fill.
 */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.GeometryOptions(vm: GraphViewModel, f: PlotFunction) {
    val colors = MaterialTheme.colorScheme
    val isPoint = isGeometryPoint(vm, f)
    val kind = vm.constructionKind(f)
    val filled = kind == 'F' || kind == 'A'
    if (isPoint) {
        Text(tr("Point"), style = MaterialTheme.typography.labelLarge, color = colors.primary)
        MarkChooser(vm, f)
        Text("Size: ${String.format(java.util.Locale.US, "%.1f", f.pointSize)} dp", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        ExpressiveSlider(value = f.pointSize, onValueChange = { vm.setOptions(f, size = kotlin.math.round(it * 10f) / 10f) }, valueRange = 1f..16f, modifier = Modifier.fillMaxWidth().semantics { contentDescription = tr("Point size") })
        if (f.geometry?.name != null) Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Show name"), modifier = Modifier.weight(1f), color = colors.onSurface)
            androidx.compose.material3.Switch(checked = !f.hideName, onCheckedChange = { vm.setGeometryOptions(f, hideName = !it) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Show coordinates"), modifier = Modifier.weight(1f), color = colors.onSurface)
            androidx.compose.material3.Switch(checked = f.showLabel, onCheckedChange = { vm.setOptions(f, label = it) })
        }
        if (f.geometry?.onPath == true && vm.plotVars == listOf("x")) Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Move along its path"), color = colors.onSurface)
                Text(tr("Round once in 10 s, or back and forth"), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            androidx.compose.material3.Switch(checked = f.animate, onCheckedChange = { vm.setGeometryOptions(f, animate = it) })
        }
    }
    if (filled) {
        Text("Fill opacity: ${(f.fillOpacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        ExpressiveSlider(value = f.fillOpacity, onValueChange = { vm.setOptions(f, opacity = it) }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth().semantics { contentDescription = tr("Fill opacity") })
    }
    if (!isPoint && !filled) Text(tr("Color, line style and thickness are above."), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
}

/** Choices in a row of segments, M3's single-choice segmented buttons. */
@Composable
private fun Segments(labels: List<String>, selected: Int, description: String, onSelect: (Int) -> Unit) {
    androidx.compose.material3.SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().semantics { contentDescription = tr(description) }) {
        labels.forEachIndexed { i, label ->
            SegmentedButton(
                selected = i == selected,
                onClick = { onSelect(i) },
                shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(i, labels.size),
                icon = {},
                label = { Text(tr(label), maxLines = 1, style = MaterialTheme.typography.labelMedium) },
            )
        }
    }
}

/**
 * A vector field's arrows: colored by |F| along a colormap (the same picker as the complex
 * plane's) or in the line's color, how long they are, their heads, how many and how thick.
 */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ArrowOptions(vm: GraphViewModel, f: PlotFunction) {
    val colors = MaterialTheme.colorScheme
    var pickingMap by remember { mutableStateOf(false) }
    if (pickingMap) ColormapPickerDialog(f.colormap, f.colormapReversed, onPick = { map, rev -> vm.setColormap(f, map, rev) }, onDismiss = { pickingMap = false })
    Text(tr("Arrows"), style = MaterialTheme.typography.labelLarge, color = colors.primary)
    Text(tr("Color"), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    Segments(listOf("This color", "By length"), if (f.arrowsByLength) 1 else 0, "Arrow color") { vm.setArrows(f, byLength = it == 1) }
    if (f.arrowsByLength) {
        // The colormap, as a strip: tap it to pick another.
        Box(
            Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(colormapStops(f.colormap, reversed = f.colormapReversed)))
                .clickable(onClickLabel = tr("Change the colormap")) { pickingMap = true },
            contentAlignment = Alignment.CenterStart,
        ) {
            MathText(
                f.colormap.label + (if (f.colormapReversed) " (reversed)" else "") + " · short \$\\to\$ long",
                style = MaterialTheme.typography.labelLarge, color = Color.White,
                modifier = Modifier.padding(start = 10.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.35f)).padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
    Text(tr("Length"), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    Segments(com.example.cas.graph.VectorField.Length.entries.map { it.label }, f.arrowLength, "Arrow length") { vm.setArrows(f, length = it) }
    Text("Scale: × ${String.format(java.util.Locale.US, "%.2f", f.arrowScale)}", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    ExpressiveSlider(value = f.arrowScale, onValueChange = { vm.setArrows(f, scale = kotlin.math.round(it * 20f) / 20f) }, valueRange = 0.2f..3f, modifier = Modifier.fillMaxWidth().semantics { contentDescription = tr("Arrow scale") })
    Text(tr("Arrowhead"), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        com.example.cas.graph.VectorField.Tip.entries.forEach { tip ->
            val chosen = f.arrowTip == tip.ordinal
            Box(
                Modifier.size(width = 52.dp, height = 36.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (chosen) colors.secondaryContainer else colors.surfaceContainerHigh)
                    .clickable(onClickLabel = tip.label) { vm.setArrows(f, tip = tip.ordinal) }
                    .semantics { contentDescription = tip.label + if (chosen) ", chosen" else "" },
                contentAlignment = Alignment.Center,
            ) {
                val tint = if (chosen) colors.onSecondaryContainer else colors.onSurfaceVariant
                androidx.compose.foundation.Canvas(Modifier.size(width = 34.dp, height = 20.dp)) {
                    val y = size.height / 2
                    val h = com.example.cas.graph.VectorField.head(tip, size.width.toDouble() - 2, y.toDouble(), 1.0, 0.0, 9.dp.toPx().toDouble())
                    drawLine(tint, Offset(2f, y), Offset(h.shaftEndX.toFloat(), y), 1.8.dp.toPx(), cap = StrokeCap.Round)
                    h.fills.forEach { pts ->
                        drawPath(androidx.compose.ui.graphics.Path().apply {
                            moveTo(pts[0].toFloat(), pts[1].toFloat())
                            var k = 2
                            while (k + 1 < pts.size) { lineTo(pts[k].toFloat(), pts[k + 1].toFloat()); k += 2 }
                            close()
                        }, tint)
                    }
                    h.strokes.forEach { sg -> drawLine(tint, Offset(sg[0].toFloat(), sg[1].toFloat()), Offset(sg[2].toFloat(), sg[3].toFloat()), 1.8.dp.toPx(), cap = StrokeCap.Round) }
                    h.dot?.let { d -> drawCircle(tint, d[2].toFloat(), Offset(d[0].toFloat(), d[1].toFloat())) }
                }
            }
        }
    }
    if (f.arrowTip != com.example.cas.graph.VectorField.Tip.None.ordinal) {
        Text("Head size: × ${String.format(java.util.Locale.US, "%.1f", f.arrowTipSize)}", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        ExpressiveSlider(value = f.arrowTipSize, onValueChange = { vm.setArrows(f, tipSize = kotlin.math.round(it * 10f) / 10f) }, valueRange = 0.4f..3f, modifier = Modifier.fillMaxWidth().semantics { contentDescription = tr("Arrowhead size") })
    }
    Text("${f.arrowDensity} arrows across", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    ExpressiveSlider(value = f.arrowDensity.toFloat(), onValueChange = { vm.setArrows(f, density = it.roundToInt()) }, valueRange = 6f..50f, steps = 43, modifier = Modifier.fillMaxWidth().semantics { contentDescription = tr("Number of arrows across") })
    Text("Thickness: ${String.format(java.util.Locale.US, "%.1f", f.thickness)} dp", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    ExpressiveSlider(value = f.thickness, onValueChange = { vm.setStyle(f, f.lineStyle, kotlin.math.round(it * 10f) / 10f) }, valueRange = 1f..8f, modifier = Modifier.fillMaxWidth().semantics { contentDescription = tr("Arrow thickness") })
}

@Composable
private fun ParameterSlider(vm: GraphViewModel, name: String) {
    val colors = MaterialTheme.colorScheme
    val value = vm.parameters[name] ?: 1.0
    val (lo, hi) = vm.rangeOf(name)
    var editing by remember { mutableStateOf(false) }
    val integers = vm.integerSliders[name] == true
    if (editing) SliderDialog(name, value, lo, hi, integers, onDone = { v, a, b, ints ->
        if (ints != integers) vm.setIntegers(name, ints)
        val (a2, b2) = if (ints) kotlin.math.floor(a) to kotlin.math.ceil(b).let { if (it <= kotlin.math.floor(a)) kotlin.math.floor(a) + 1 else it } else a to b
        vm.setSlider(name, if (ints) Math.rint(v) else v, a2, b2); editing = false
    }, onDismiss = { editing = false })
    Column(Modifier.fillMaxWidth()) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        val playing = name in vm.playing
        IconButton(onClick = { vm.togglePlay(name) }, modifier = Modifier.size(40.dp)) {
            Icon(
                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (playing) "Pause ${spokenName(name)}" else "Animate ${spokenName(name)}",
                tint = if (playing) colors.primary else colors.onSurfaceVariant,
            )
        }
        // A built symbol (x̂₁) drawn as the math draws it, not as its stored text.
        SymbolName(name, 20.sp, colors.onSurface, Modifier.widthIn(min = 28.dp).padding(end = 4.dp))
        // ℝ or ℤ: real numbers, or integers only. Filled when integers are on.
        val tap = rememberKeyTap()
        androidx.compose.material3.FilledTonalIconToggleButton(
            checked = integers,
            onCheckedChange = { tap(); vm.setIntegers(name, it) },
            modifier = Modifier.size(36.dp).semantics { contentDescription = if (integers) "${spokenName(name)} moves through integers; switch to real numbers" else "${spokenName(name)} moves through real numbers; switch to integers" },
        ) {
            MathText(if (integers) "\$\\mathbb{Z}\$" else "\$\\mathbb{R}\$", style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 17.sp), mathScale = 1f)
        }
        Spacer(Modifier.width(4.dp))
        val step = (hi - lo) / 200
        // Integers: tick marks at each whole number when there are few enough to see.
        val ticks = (kotlin.math.round(hi - lo).toInt() - 1).takeIf { integers && it in 1..40 } ?: 0
        ExpressiveSlider(
            value = value.toFloat(),
            onValueChange = { vm.setParameter(name, if (integers) Math.rint(it.toDouble()) else Math.round(it / step) * step) },
            valueRange = lo.toFloat()..hi.toFloat(),
            steps = ticks,
            modifier = Modifier.weight(1f).semantics { contentDescription = "Value of ${spokenName(name)}, from ${shortNumber(lo)} to ${shortNumber(hi)}" + if (integers) ", integers only" else "" },
        )
        // Tap the value to type it, or change the slider's range.
        Box(
            Modifier
                .padding(start = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceContainerHigh)
                .clickable(onClickLabel = "Type a value for ${spokenName(name)}") { editing = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            // Always the same decimals for this range (its step), in a width that fits either end,
            // so the number doesn't jitter as the slider moves.
            val decimals = if (integers) 0 else sliderDecimals(lo, hi)
            val widest = maxOf(sliderText(lo, decimals).length, sliderText(hi, decimals).length)
            Box(Modifier.widthIn(min = (widest * 9).dp), contentAlignment = Alignment.CenterEnd) {
                MathText(Readout.markdown(sliderText(value, decimals)), color = colors.onSurface, style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 17.sp), mathScale = 1f)
            }
        }
    }
    }
}

/** Type a slider's value and its range (−10 to 10 unless changed). A line like a = 3 also sets it. */
@Composable
private fun SliderDialog(name: String, value: Double, min: Double, max: Double, integers0: Boolean, onDone: (Double, Double, Double, Boolean) -> Unit, onDismiss: () -> Unit) {
    fun text(v: Double) = shortNumber(v).replace("−", "-")
    var integers by remember { mutableStateOf(integers0) }
    var v by remember { mutableStateOf(text(value)) }
    var a by remember { mutableStateOf(text(min)) }
    var b by remember { mutableStateOf(text(max)) }
    fun num(t: String) = t.trim().replace("−", "-").toDoubleOrNull()
    val nv = num(v); val na = num(a); val nb = num(b)
    val valid = nv != null && na != null && nb != null && na < nb
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { SymbolName(name, 24.sp, MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Real numbers or integers only, as a Material 3 segmented button.
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(false to "Real numbers", true to "Integers").forEachIndexed { k, (ints, label) ->
                        SegmentedButton(icon = {}, 
                            selected = integers == ints,
                            onClick = { integers = ints },
                            shape = SegmentedButtonDefaults.itemShape(k, 2),
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    MathText(if (ints) "\$\\mathbb{Z}\$" else "\$\\mathbb{R}\$", style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 16.sp), mathScale = 1f)
                                    Spacer(Modifier.width(6.dp))
                                    Text(tr(label), maxLines = 1)
                                }
                            },
                        )
                    }
                }
                OutlinedTextField(v, { v = it }, singleLine = true, label = { Text(if (integers) "Value (rounded to an integer)" else "Value") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(a, { a = it }, singleLine = true, label = { Text(tr("From")) }, modifier = Modifier.weight(1f))
                    OutlinedTextField(b, { b = it }, singleLine = true, label = { Text(tr("To")) }, modifier = Modifier.weight(1f))
                }
                Text(
                    "You can also set it with a line of its own, like ${spokenName(name)} = 3.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onDone(nv!!, na!!, nb!!, integers) }) { Text(tr("Done")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}

/**
 * A label like "f(z) =" or "(x, y) =": letters in italic Computer Modern, brackets, commas
 * and = upright, as in the math.
 */
/** A slider's letter: a built symbol drawn with its accent and scripts, otherwise an italic letter. */
@Composable
fun SymbolName(name: String, size: androidx.compose.ui.unit.TextUnit, color: Color, modifier: Modifier = Modifier) {
    if (com.example.cas.cas.CustomSymbol.isCustom(name)) {
        Box(modifier) { MathView(MathRow(mutableListOf(Sym(name))), size, color) }
    } else {
        Text(mathLabel(name), color = color, style = TextStyle(fontFamily = CasFonts.CmItalic, fontSize = size), modifier = modifier)
    }
}

/** A letter as words can carry it: a built symbol as plain text (x̂_1), anything else as it is. */
fun spokenName(name: String): String = com.example.cas.cas.CustomSymbol.decode(name)?.plain ?: name

fun mathLabel(text: String): androidx.compose.ui.text.AnnotatedString = androidx.compose.ui.text.buildAnnotatedString {
    for (c in text) {
        if (c.isLetter()) {
            pushStyle(androidx.compose.ui.text.SpanStyle(fontFamily = CasFonts.CmItalic))
            append(c)
            pop()
        } else append(c)
    }
}


/**
 * Drag and pinch, with the pinch read per axis (see [com.example.cas.graph.AxisPinch]):
 * [onGesture] gets the centroid, the pan, and separate zoom factors for x and y.
 */
suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectAxisTransformGestures(
    /** A touch to leave alone (one that began on a card over the graph). */
    skip: (androidx.compose.ui.input.pointer.PointerInputChange) -> Boolean = { false },
    onGesture: (centroid: Offset, pan: Offset, zoomX: Double, zoomY: Double) -> Unit,
) {
    val minSpread = 48.dp.toPx()
    awaitEachGesture {
        val first = awaitFirstDown(requireUnconsumed = false)
        if (skip(first)) return@awaitEachGesture
        do {
            val event = awaitPointerEvent()
            if (event.changes.any { it.isConsumed }) break
            val down = event.changes.filter { it.pressed }
            val pan = event.calculatePan()
            val centroid = event.calculateCentroid(useCurrent = false)
            var zx = 1.0
            var zy = 1.0
            if (down.size >= 2) {
                val a = down[0]; val b = down[1]
                val (fx, fy) = com.example.cas.graph.AxisPinch.factors(
                    a.previousPosition.x - b.previousPosition.x, a.previousPosition.y - b.previousPosition.y,
                    a.position.x - b.position.x, a.position.y - b.position.y, minSpread,
                )
                zx = fx; zy = fy
            }
            if (pan != Offset.Zero || zx != 1.0 || zy != 1.0) {
                onGesture(centroid, pan, zx, zy)
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}


/** The keys' tap feel, for the plots' buttons: call the returned function on each press. */
@Composable
fun rememberKeyTap(): () -> Unit {
    val view = androidx.compose.ui.platform.LocalView.current
    return remember(view) { { if (AppSettings.haptics) view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) } }
}


/** Decimals that match a slider's step (a two-hundredth of its range): 1 for −10…10, 2 for 0…1. */
fun sliderDecimals(lo: Double, hi: Double): Int {
    val step = (hi - lo) / 200
    if (step <= 0 || !step.isFinite()) return 2
    return kotlin.math.ceil(-kotlin.math.log10(step)).toInt().coerceIn(0, 6)
}

/** A slider value with exactly [decimals] decimals and a proper minus sign. */
fun sliderText(v: Double, decimals: Int): String =
    String.format(java.util.Locale.US, "%.${decimals}f", if (v == 0.0) 0.0 else v).replace("-", "−")


/**
 * The lines as a list you can reorder: hold a row, then drag it up or down. It lifts while it
 * moves, and passes a neighbor once it's gone halfway past it.
 */
@Composable
private fun ReorderableRows(vm: GraphViewModel, hidden: (PlotFunction) -> Boolean = { false }, folders: (PlotFunction) -> List<PlotFunction> = { emptyList() }, row: @Composable (PlotFunction, Modifier) -> Unit) {
    val heights = remember { mutableStateMapOf<PlotFunction, Int>() }
    var dragging by remember { mutableStateOf<PlotFunction?>(null) }
    var offset by remember { mutableStateOf(0f) }
    val tap = rememberKeyTap()
    val gap = with(LocalDensity.current) { 6.dp.toPx() }
    // Follows the finger as far as it goes, stepping past each visible row (or closed folder) it
    // passes the middle of; a folder carries its lines with it.
    fun span(rows: List<PlotFunction>) = rows.sumOf { (heights[it] ?: 0) + gap.toInt() }.toFloat()
    fun dragBy(f: PlotFunction, dy: Float) {
        offset += dy
        while (true) {
            val below = vm.stepSpan(f, 1)
            val above = vm.stepSpan(f, -1)
            if (below.isNotEmpty() && offset > span(below) / 2f) {
                val h = span(below)
                if (!vm.step(f, 1)) break
                offset -= h
                tap()
            } else if (above.isNotEmpty() && offset < -span(above) / 2f) {
                val h = span(above)
                if (!vm.step(f, -1)) break
                offset += h
                tap()
            } else break
        }
    }
    val carried = dragging?.let { d -> if (d.isFolder) vm.folderMembers(d).toSet() else emptySet() } ?: emptySet()
    vm.functions.toList().forEach { f ->
        if (hidden(f)) { heights[f] = 0; return@forEach }
        // Keyed by the line, so its drag carries on after it moves past another.
        key(f) {
            val lifted = dragging === f || f in carried
            // Up and down only, so a sideways swipe from the handle still deletes the line.
            val handle = Modifier.pointerInput(f) {
                detectVerticalDragGestures(
                    onDragStart = { tap(); dragging = f; offset = 0f },
                    onDragEnd = { dragging = null; offset = 0f },
                    onDragCancel = { dragging = null; offset = 0f },
                    onVerticalDrag = { change, dy -> change.consume(); dragBy(f, dy) },
                )
            }
            Box(
                Modifier
                    .zIndex(if (lifted) 1f else 0f)
                    .offset { IntOffset(0, if (lifted) offset.roundToInt() else 0) }
                    .graphicsLayer { if (lifted) { shadowElevation = 12f; scaleX = 1.02f; scaleY = 1.02f } }
                    // (Holding a line renames it; lines move by their handle.)
                    .onGloballyPositioned { heights[f] = it.size.height },
            ) {
                // Inside a folder: indented, with a bar in each enclosing folder's color (primary
                // by default), outside the swipe box so its red strip doesn't show in the gap.
                val around = folders(f)
                val levels = around.size
                val scheme = MaterialTheme.colorScheme
                val bars = around.map { folderColor(it, scheme) }
                Box(
                    Modifier
                        .drawBehind {
                            bars.forEachIndexed { k, c ->
                                val x = (5 + 14 * k).dp.toPx()
                                drawRoundRect(c, topLeft = Offset(x, 6.dp.toPx()), size = Size(4.dp.toPx(), size.height - 12.dp.toPx()), cornerRadius = CornerRadius(2.dp.toPx()))
                            }
                        }
                        .padding(start = (14 * levels).dp),
                ) {
                    SwipeToRemove(onRemove = { vm.requestRemove(f) }, asks = { AppSettings.confirmDeleteEntry }) { row(f, handle) }
                }
            }
        }
    }
}

/** A folder's color: its own, or the theme's primary. */
internal fun folderColor(folder: PlotFunction, scheme: androidx.compose.material3.ColorScheme): Color =
    folder.customColor?.let { Color(it) } ?: scheme.primary

/** Swipe a line sideways (either way) to delete it; a red strip with a bin shows underneath. */
@Composable
private fun SwipeToRemove(onRemove: () -> Unit, asks: () -> Boolean = { false }, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val state = androidx.compose.material3.rememberSwipeToDismissBoxState(
        // When it asks first, the row snaps back and the dialog decides.
        confirmValueChange = { value -> if (value != androidx.compose.material3.SwipeToDismissBoxValue.Settled) { onRemove(); !asks() } else false },
    )
    androidx.compose.material3.SwipeToDismissBox(
        state = state,
        backgroundContent = {
            val end = state.dismissDirection == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(colors.errorContainer).padding(horizontal = 20.dp),
                contentAlignment = if (end) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = colors.onErrorContainer)
            }
        },
    ) { content() }
}


/**
 * "Fit" under a function with unknowns once the graph has a list of points: sets the sliders to
 * the least-squares fit. If it can't be fitted, says so and leaves the sliders as they were.
 */
@Composable
private fun FitButton(vm: GraphViewModel, f: PlotFunction) {
    val colors = MaterialTheme.colorScheme
    val tap = rememberKeyTap()
    var failed by remember(f.version) { mutableStateOf(false) }
    var stats by remember { mutableStateOf(false) }
    if (stats) FitStatsDialog(vm, f, onDismiss = { stats = false })
    // A failed fit turns the chip red ("No fit") until the line changes; holding it shows the statistics.
    Box(
        Modifier
            .padding(horizontal = 2.dp)
            .clip(CircleShape)
            .background(if (failed) colors.errorContainer else colors.tertiaryContainer)
            .combinedClickable(onClickLabel = tr("Fit to the list"), onLongClickLabel = "Show the fit's statistics", onLongClick = { tap(); stats = true }) { tap(); failed = !vm.fit(f) }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics { if (failed) contentDescription = tr("Couldn't fit this to the points") },
    ) {
        Text(if (failed) "No fit" else "Fit", color = if (failed) colors.onErrorContainer else colors.onTertiaryContainer, style = MaterialTheme.typography.labelLarge)
    }
}


/**
 * The Material 3 FAB menu for adding: + opens a stack of pill buttons above it (Line nearest,
 * then Note, Folder and, on the 2D graph, Table), each sliding in after the one below, and +
 * turns into ×. Tapping outside, or ×, closes it.
 */
@Composable
private fun AddFabMenu(vm: GraphViewModel, tables: Boolean) {
    // On a phone, something new to type in gets the screen: the graph steps aside until Enter
    // (or the keyboard's handle pulled down), and construct mode closes.
    val phone = !isTabletLayout()
    fun adding(f: () -> Unit) { if (phone) (vm as? Graph2DViewModel)?.stopConstructing(); f() }
    // Nearest the button first.
    val items = buildList {
        add(FabItem("Line", Icons.Default.Functions, "Add a line") { adding { vm.add(); if (phone) vm.typingFocus = true } })
        add(FabItem("Note", Icons.Default.Notes, "Add a note") { adding { vm.addText(folder = false); if (phone) vm.typingFocus = true } })
        add(FabItem("Folder", Icons.Default.CreateNewFolder, "Add a folder") { adding { vm.addText(folder = true); if (phone) vm.typingFocus = true } })
        if (tables) add(FabItem("Table", Icons.Default.TableChart, "Add a table") { adding { vm.addTable() } })
    }
    FabMenu(items, size = 48.dp, description = "Add a line, note, folder or table")
}

/** One of a [FabMenu]'s pills: its label, icon, what it says to screen readers, and its action. */
internal class FabItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val spoken: String, val action: () -> Unit)

/**
 * M3's FAB menu: + opens [items] as a stack of pills above it (the first nearest the button),
 * each sliding in after the one below, and + turns into ×. Tapping outside, or ×, closes it.
 */
@Composable
internal fun FabMenu(items: List<FabItem>, size: androidx.compose.ui.unit.Dp, description: String, alignEnd: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val tap = rememberKeyTap()
    var open by remember { mutableStateOf(false) }
    val turn by androidx.compose.animation.core.animateFloatAsState(if (open) 45f else 0f, label = "fab")
    val corner by androidx.compose.animation.core.animateDpAsState(if (open) size / 2 else size / 3, label = "fabShape")
    val density = LocalDensity.current
    Box {
        Box(
            Modifier
                .size(size)
                .shadow(6.dp, RoundedCornerShape(corner))
                .clip(RoundedCornerShape(corner))
                .background(if (open) colors.primary else colors.primaryContainer)
                .clickable(onClickLabel = if (open) "Close" else "Add") { tap(); open = !open }
                .semantics { contentDescription = if (open) "Close the add menu" else description },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = if (open) colors.onPrimary else colors.onPrimaryContainer, modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = turn })
        }
        if (open) {
            val lift = with(density) { (size + 10.dp).roundToPx() }
            androidx.compose.ui.window.Popup(
                alignment = if (alignEnd) Alignment.BottomEnd else Alignment.BottomStart,
                offset = IntOffset(0, -lift),
                onDismissRequest = { open = false },
                properties = androidx.compose.ui.window.PopupProperties(focusable = true),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
                    items.asReversed().forEachIndexed { k, item ->
                        val order = items.size - 1 - k // 0 for the one nearest the button
                        val shown = remember { androidx.compose.animation.core.MutableTransitionState(false) }.apply { targetState = true }
                        androidx.compose.animation.AnimatedVisibility(
                            visibleState = shown,
                            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(160, delayMillis = order * 40)) +
                                androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(220, delayMillis = order * 40)) { it / 2 } +
                                androidx.compose.animation.scaleIn(androidx.compose.animation.core.tween(220, delayMillis = order * 40), initialScale = 0.85f),
                        ) {
                            Row(
                                Modifier
                                    .height(56.dp)
                                    .shadow(3.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(colors.primaryContainer)
                                    .clickable(onClickLabel = tr(item.spoken)) { tap(); open = false; item.action() }
                                    .padding(start = 18.dp, end = 24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(item.icon, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(24.dp))
                                Text(tr(item.label), style = MaterialTheme.typography.titleMedium, color = colors.onPrimaryContainer)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The bar along the bottom of a graph: + (add a line) on the left (with the keyboard button while it's
 * hidden, and any [leading] controls), the [tools] on the right, and export in its own circle
 * at the far right.
 */
@Composable
fun GraphBottomBar(
    vm: GraphViewModel,
    modifier: Modifier = Modifier,
    onExport: (() -> Unit)?,
    leading: @Composable RowScope.() -> Unit = {},
    tools: (@Composable RowScope.() -> Unit)? = null,
    /** Offered in the + button's menu (hold +) where tables make sense (the 2D graph). */
    tables: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val tap = rememberKeyTap()
    Row(modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        // Add: the M3 FAB menu (a line, a note, a folder, a table).
        AddFabMenu(vm, tables)
        leading()
        // After + and the file button; tablets always show the keyboard, so never there.
        if (vm.active != null && vm.keypadHidden && !isTabletLayout()) {
            Spacer(Modifier.width(8.dp))
            // The keyboard back, and (on a phone) the graph steps aside for typing.
            ShowKeypadButton(onClick = { vm.keypadHidden = false; vm.typingFocus = true })
        }
        Spacer(Modifier.weight(1f))
        if (tools != null) ExpressiveToolbar(content = tools)
        if (onExport != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(48.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(colors.surfaceContainerHigh)
                    .clickable(onClickLabel = tr("Export the graph")) { tap(); onExport() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.IosShare, contentDescription = tr("Export"), tint = colors.onSurface)
            }
        }
    }
}


/**
 * A note (text between lines) or a folder (its title, an arrow that closes and opens it, and a
 * dot that hides or shows everything in it), typed with the phone's keyboard.
 */
@Composable
private fun TextRow(vm: GraphViewModel, f: PlotFunction, handle: Modifier?) {
    if (f.isFolder) { FolderRow(vm, f, handle); return }
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceContainerLow)
            .padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Notes, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp).size(20.dp))
        Box(Modifier.weight(1f).padding(vertical = 10.dp)) {
            val style = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface)
            val focus = remember { androidx.compose.ui.focus.FocusRequester() }
            val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
            // Just added: straight into typing.
            androidx.compose.runtime.LaunchedEffect(f) { if (vm.justAdded === f) { vm.justAdded = null; runCatching { focus.requestFocus() } } }
            androidx.compose.foundation.text.BasicTextField(
                value = f.note.orEmpty(),
                onValueChange = { vm.setNote(f, it) },
                textStyle = style,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primary),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus(); vm.typingFocus = false }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = tr("Note") },
            )
            if (f.note.isNullOrEmpty()) Text(tr("Note"), style = style, color = colors.onSurfaceVariant)
        }
        IconButton(onClick = { vm.requestRemove(f) }) {
            Icon(Icons.Default.Close, contentDescription = tr("Remove"), tint = colors.onSurfaceVariant)
        }
        if (handle != null) Icon(Icons.Default.DragIndicator, contentDescription = tr("Drag to reorder"), tint = colors.onSurfaceVariant, modifier = handle.size(40.dp).padding(8.dp))
    }
}

/**
 * A folder: tap to open or close it, long-press for its name, color and nesting. Its color
 * tints the row and marks the lines inside it; × deletes the folder (its lines stay).
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FolderRow(vm: GraphViewModel, f: PlotFunction, handle: Modifier?) {
    val colors = MaterialTheme.colorScheme
    val tint = folderColor(f, colors)
    var editing by remember { mutableStateOf(false) }
    // Just added: its name straight away.
    androidx.compose.runtime.LaunchedEffect(f) { if (vm.justAdded === f) { vm.justAdded = null; editing = true } }
    val tap = rememberKeyTap()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(androidx.compose.ui.graphics.lerp(colors.surfaceContainerHigh, tint, 0.16f))
            .combinedClickable(
                onClickLabel = if (f.collapsed) "Open the folder" else "Close the folder",
                onLongClickLabel = "Folder name and color",
                onClick = { vm.toggleCollapsed(f) },
                onLongClick = { tap(); editing = true },
            )
            .padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (f.collapsed) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.Default.KeyboardArrowDown,
            contentDescription = null, tint = colors.onSurface, modifier = Modifier.padding(start = 8.dp, end = 4.dp).size(24.dp),
        )
        Icon(if (f.collapsed) Icons.Default.Folder else Icons.Default.FolderOpen, contentDescription = null, tint = tint, modifier = Modifier.padding(horizontal = 6.dp).size(22.dp))
        Text(
            f.note?.takeIf { it.isNotBlank() } ?: "Folder",
            style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(vertical = 14.dp),
        )
        if (f.collapsed) {
            Text("${vm.folderMembers(f).count { !it.isText }}", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
        }
        IconButton(onClick = { vm.toggleVisible(f) }) {
            Icon(if (f.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = if (f.visible) "Hide the folder's lines" else "Show the folder's lines", tint = colors.onSurfaceVariant)
        }
        IconButton(onClick = { vm.requestRemove(f) }) {
            Icon(Icons.Default.Close, contentDescription = tr("Delete folder"), tint = colors.onSurfaceVariant)
        }
        if (handle != null) Icon(Icons.Default.DragIndicator, contentDescription = tr("Drag to reorder"), tint = colors.onSurfaceVariant, modifier = handle.size(40.dp).padding(8.dp))
    }
    if (editing) FolderDialog(vm, f, onDismiss = { editing = false; vm.typingFocus = false })
}

/** A folder's name, color and place, all in one dialog: the line color picker with the name on top. */
@Composable
private fun FolderDialog(vm: GraphViewModel, f: PlotFunction, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var name by remember { mutableStateOf(f.note.orEmpty()) }
    ColorPickerDialog(
        initial = folderColor(f, colors),
        // Default is the theme's color.
        onPick = { c -> vm.setFolder(f, name.trim(), c?.toArgb()); onDismiss() },
        onDismiss = onDismiss,
        title = "Folder",
        header = {
            OutlinedTextField(name, { name = it }, singleLine = true, label = { Text(tr("Name")) }, modifier = Modifier.fillMaxWidth())
            if (vm.canNest(f) || vm.folderOf(f) != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (vm.canNest(f)) androidx.compose.material3.OutlinedButton(onClick = { vm.nest(f, 1); onDismiss() }) {
                    Icon(Icons.AutoMirrored.Filled.FormatIndentIncrease, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(tr("Into folder above"))
                }
                if (vm.folderOf(f) != null) androidx.compose.material3.OutlinedButton(onClick = { vm.nest(f, -1); onDismiss() }) {
                    Icon(Icons.AutoMirrored.Filled.FormatIndentDecrease, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(tr("Out of folder"))
                }
            }
        },
    )
}

/**
 * A line's data as a table, in Material 3 Expressive: the columns as cards with their role
 * (x, y, σ(x), σ(y), or not used) as a colored badge and a menu (sort, fill 1, 2, 3…, clear,
 * remove), the cells in banded rows tinted by their column's role, and one + button (bottom
 * left) that opens to add a row or a column, or paste a table. Drag the grips between column
 * cards, or under a row's number, to resize; double-tap a grip to go back to the usual size.
 * On a tablet a side pane shows the counts and a live preview of the points.
 *
 * Long data stays quick: rows are built as they scroll into view, and a cell is plain text
 * until it's tapped, when it becomes the one field being typed in.
 */
@Composable
private fun PointTableDialog(vm: GraphViewModel, f: PlotFunction, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = androidx.compose.ui.platform.LocalContext.current
    val density = LocalDensity.current
    val start = remember(f) { vm.tableOf(f) }
    // Columns of cells, all kept the same length; roles are column positions.
    val names = remember(f) { androidx.compose.runtime.mutableStateListOf(*start.names.let { n -> List(start.columns.size) { n.getOrElse(it) { "" } } }.toTypedArray()) }
    val cells = remember(f) {
        androidx.compose.runtime.mutableStateListOf(*start.columns.map { c ->
            androidx.compose.runtime.mutableStateListOf(*List(maxOf(start.rowCount, 1)) { r -> c.getOrElse(r) { "" } }.toTypedArray())
        }.toTypedArray())
    }
    // Each column's number format and color scale, kept beside the columns; the first column frozen in view.
    val fmts = remember(f) { androidx.compose.runtime.mutableStateListOf(*List(start.columns.size) { start.format(it) }.toTypedArray()) }
    var frozen by remember(f) { mutableStateOf(start.frozen) }
    // Filters (one per column, all must hold): rows they hide stay in the table and are plotted.
    val filters = remember(f) { androidx.compose.runtime.mutableStateMapOf<Int, com.example.cas.graph.SheetTools.Filter>() }
    // A range picked by holding a cell (then tapping another to stretch it), as (column, row) corners.
    var selA by remember(f) { mutableStateOf<Pair<Int, Int>?>(null) }
    var selB by remember(f) { mutableStateOf<Pair<Int, Int>?>(null) }
    fun inSelection(c: Int, r: Int): Boolean {
        val a = selA ?: return false; val b = selB ?: a
        return c in minOf(a.first, b.first)..maxOf(a.first, b.first) && r in minOf(a.second, b.second)..maxOf(a.second, b.second)
    }
    // Find and replace: the bar's fields, and which match is current.
    var finding by remember(f) { mutableStateOf(false) }
    var query by remember(f) { mutableStateOf("") }
    var replacement by remember(f) { mutableStateOf("") }
    var matchCase by remember(f) { mutableStateOf(false) }
    var wholeCell by remember(f) { mutableStateOf(false) }
    var matchAt by remember(f) { mutableStateOf(0) }
    // A column's number format, or its filter, being set.
    var formatFor by remember(f) { mutableStateOf<Int?>(null) }
    var filterFor by remember(f) { mutableStateOf<Int?>(null) }
    // The toolbar's tab, the column its tools work on (the selected cell's, or the last one opened),
    // the column whose sheet is open, and the formula bar's focus.
    var tab by remember(f) { mutableStateOf(TableTab.Home) }
    var focusCol by remember(f) { mutableStateOf(start.y ?: 0) }
    var sheetFor by remember(f) { mutableStateOf<Int?>(null) }
    // A column's highlight rule or insights being shown; cells marked as outliers by the insights.
    var highlightFor by remember(f) { mutableStateOf<Int?>(null) }
    var insightsFor by remember(f) { mutableStateOf<Int?>(null) }
    var marked by remember(f) { mutableStateOf<Set<Pair<Int, Int>>>(emptySet()) }
    val barFocus = remember(f) { androidx.compose.ui.focus.FocusRequester() }
    var roleX by remember(f) { mutableStateOf(start.x) }
    var roleY by remember(f) { mutableStateOf(start.y) }
    var roleSx by remember(f) { mutableStateOf(start.sigmaX) }
    var roleSy by remember(f) { mutableStateOf(start.sigmaY) }
    // The one cell being typed in, as (column, row), and where its fill handle is being dragged to.
    var editing by remember(f) { mutableStateOf<Pair<Int, Int>?>(null) }
    var fillTo by remember(f) { mutableStateOf<Pair<Int, Int>?>(null) }
    // Undo and redo: whole-table snapshots, one per change (typing in a cell counts once per cell).
    class Snapshot(val names: List<String>, val cells: List<List<String>>, val roles: List<Int?>, val formats: List<com.example.cas.graph.ColumnFormat>, val frozen: Boolean)
    val undoStack = remember(f) { androidx.compose.runtime.mutableStateListOf<Snapshot>() }
    val redoStack = remember(f) { androidx.compose.runtime.mutableStateListOf<Snapshot>() }
    var typingRecorded by remember(f) { mutableStateOf<Pair<Int, Int>?>(null) }
    // Columns' widths and rows' heights that were dragged; the rest are the usual size.
    val defaultWidth = 112.dp
    val defaultHeight = 44.dp
    val widths = remember(f) { androidx.compose.runtime.mutableStateMapOf<Int, androidx.compose.ui.unit.Dp>() }
    val heights = remember(f) { androidx.compose.runtime.mutableStateMapOf<Int, androidx.compose.ui.unit.Dp>() }
    val formulas = AppSettings.sheetFormulas
    val rows = cells.maxOfOrNull { it.size } ?: 0
    fun table() = com.example.cas.graph.DataTable(names.toList(), cells.map { it.toList() }, roleX, roleY, roleSx, roleSy, fmts.toList(), frozen)
    // Worked out again only when a cell, name or role changes, not on every redraw.
    val current by remember(f) { androidx.compose.runtime.derivedStateOf { table() } }
    val points by remember(f) { androidx.compose.runtime.derivedStateOf { current.points().size } }
    val bad by remember(f) { androidx.compose.runtime.derivedStateOf { current.badCells() } }
    fun roleOf(c: Int) = when (c) { roleX -> "x"; roleY -> "y"; roleSx -> "σx"; roleSy -> "σy"; else -> null }
    fun snapshot() = Snapshot(names.toList(), cells.map { it.toList() }, listOf(roleX, roleY, roleSx, roleSy), fmts.toList(), frozen)
    /** Keeps the table as it is now, before a change, for undo. */
    val quiet = remember(f) { booleanArrayOf(false) }
    fun record() {
        // Within one action (a paste) only its first change is kept.
        if (quiet[0]) return
        undoStack.add(snapshot())
        if (undoStack.size > 60) undoStack.removeAt(0)
        redoStack.clear()
    }
    fun restore(s: Snapshot) {
        editing = null; fillTo = null; typingRecorded = null
        names.clear(); names.addAll(s.names)
        cells.clear(); s.cells.forEach { col -> cells.add(androidx.compose.runtime.mutableStateListOf(*col.toTypedArray())) }
        roleX = s.roles[0]; roleY = s.roles[1]; roleSx = s.roles[2]; roleSy = s.roles[3]
        fmts.clear(); fmts.addAll(s.formats); frozen = s.frozen
        filters.clear(); selA = null; selB = null
    }
    fun undo() { undoStack.removeLastOrNull()?.let { redoStack.add(snapshot()); restore(it) } }
    fun redo() { redoStack.removeLastOrNull()?.let { undoStack.add(snapshot()); restore(it) } }
    /** One role per column and one column per role. */
    fun assign(c: Int, role: String?) {
        record()
        if (roleX == c) roleX = null; if (roleY == c) roleY = null; if (roleSx == c) roleSx = null; if (roleSy == c) roleSy = null
        when (role) { "x" -> roleX = c; "y" -> roleY = c; "σx" -> roleSx = c; "σy" -> roleSy = c }
    }
    /** Sizes keyed by position, moved along when rows or columns come or go at [at]. */
    fun <V> shiftKeys(map: MutableMap<Int, V>, at: Int, by: Int) {
        val moved = map.filterKeys { it >= at }.toList()
        moved.forEach { (k, _) -> map.remove(k) }
        moved.forEach { (k, v) -> if (k + by >= at) map[k + by] = v }
    }
    fun addRow() { record(); cells.forEach { it.add("") } }
    fun insertRow(at: Int) { record(); cells.forEach { it.add(at.coerceIn(0, it.size), "") }; shiftKeys(heights, at, 1) }
    fun addColumn() { record(); names.add(""); fmts.add(com.example.cas.graph.ColumnFormat()); cells.add(androidx.compose.runtime.mutableStateListOf(*Array(maxOf(rows, 1)) { "" })) }
    fun removeRow(r: Int) {
        if (rows <= 1) return
        record()
        editing = null
        cells.forEach { if (r < it.size) it.removeAt(r) }
        heights.remove(r); shiftKeys(heights, r + 1, -1)
    }
    fun removeColumn(c: Int) {
        if (cells.size <= 1) return
        record()
        editing = null
        assign(c, null)
        names.removeAt(c); cells.removeAt(c); if (c < fmts.size) fmts.removeAt(c)
        filters.clear(); selA = null; selB = null
        widths.remove(c); shiftKeys(widths, c + 1, -1)
        fun shift(k: Int?) = k?.let { if (it > c) it - 1 else it }
        roleX = shift(roleX); roleY = shift(roleY); roleSx = shift(roleSx); roleSy = shift(roleSy)
    }
    /** Every column rewritten in the row order [order]. */
    fun reorder(order: List<Int>) {
        record()
        editing = null
        cells.forEach { col ->
            val copy = order.map { col.getOrElse(it) { "" } }
            col.clear(); col.addAll(copy.ifEmpty { listOf("") })
        }
    }
    /** Rows by column [c]'s numbers, smallest first (text and empty cells last). */
    fun sortBy(c: Int) { val t = table(); reorder((0 until rows).sortedWith(compareBy(nullsLast()) { r: Int -> t.value(c, r) })) }
    fun removeEmptyRows() = reorder((0 until rows).filter { r -> cells.any { it.getOrElse(r) { "" }.isNotBlank() } })
    /** Rows by column [c]'s numbers, largest first (text and empty cells still last). */
    fun sortDown(c: Int) {
        val t = table()
        reorder((0 until rows).sortedWith { a, b ->
            val va = t.value(c, a); val vb = t.value(c, b)
            when { va == null && vb == null -> 0; va == null -> 1; vb == null -> -1; else -> vb.compareTo(va) }
        })
    }
    /** A copy of column [c] just after it. */
    fun duplicateColumn(c: Int) {
        record(); editing = null
        names.add(c + 1, names[c]); cells.add(c + 1, androidx.compose.runtime.mutableStateListOf(*cells[c].toTypedArray()))
        fmts.add((c + 1).coerceAtMost(fmts.size), fmts.getOrElse(c) { com.example.cas.graph.ColumnFormat() }); filters.clear()
        shiftKeys(widths, c + 1, 1); widths[c]?.let { widths[c + 1] = it }
        fun shift(k: Int?) = k?.let { if (it > c) it + 1 else it }
        roleX = shift(roleX); roleY = shift(roleY); roleSx = shift(roleSx); roleSy = shift(roleSy)
    }
    /** Column [from] moved to [to]; its role and width go with it. */
    fun moveColumn(from: Int, to: Int) {
        if (to !in cells.indices || from == to) return
        record(); editing = null
        val order = com.example.cas.graph.DataTable.moved(cells.size, from, to)
        val oldNames = names.toList(); val oldCells = cells.toList(); val oldWidths = widths.toMap(); val oldFmts = fmts.toList()
        fmts.clear(); fmts.addAll(order.map { oldFmts.getOrElse(it) { com.example.cas.graph.ColumnFormat() } }); filters.clear()
        names.clear(); names.addAll(order.map { oldNames[it] })
        cells.clear(); cells.addAll(order.map { oldCells[it] })
        widths.clear(); order.forEachIndexed { k, old -> oldWidths[old]?.let { widths[k] = it } }
        fun place(k: Int?) = k?.let { order.indexOf(it) }
        roleX = place(roleX); roleY = place(roleY); roleSx = place(roleSx); roleSy = place(roleSy)
    }
    /** A copy of row [r] just below it. */
    fun duplicateRow(r: Int) {
        record(); editing = null
        cells.forEach { col -> col.add(r + 1, col.getOrElse(r) { "" }) }
        shiftKeys(heights, r + 1, 1)
    }
    /** Every row kept, in the order given, after the rows not kept (a tidy-up). */
    fun keepRows(keep: List<Int>) = reorder(keep.ifEmpty { listOf(0) })
    /** Rows that repeat an earlier row exactly are removed (the first stays), as Sheets' Remove duplicates. */
    fun removeDuplicates() {
        val keep = com.example.cas.graph.SheetTools.withoutDuplicates(cells.map { it.toList() })
        val gone = rows - keep.size
        if (gone > 0) keepRows(keep)
        android.widget.Toast.makeText(context, if (gone == 0) "No duplicate rows" else "$gone duplicate row${if (gone == 1) "" else "s"} removed", android.widget.Toast.LENGTH_SHORT).show()
    }
    /** Spaces trimmed from every cell's ends, and runs inside made one. */
    fun trimAll() {
        record(); editing = null
        cells.forEach { col -> col.indices.forEach { r -> val t = com.example.cas.graph.SheetTools.trimmed(col[r]); if (t != col[r]) col[r] = t } }
    }
    /** Rows become columns and columns rows (names and the first column swap places). */
    fun transpose() {
        record(); editing = null; filters.clear(); selA = null; selB = null
        val (n, c) = com.example.cas.graph.SheetTools.transpose(names.toList(), cells.map { it.toList() })
        names.clear(); names.addAll(n)
        cells.clear(); c.forEach { col -> cells.add(androidx.compose.runtime.mutableStateListOf(*col.toTypedArray())) }
        fmts.clear(); fmts.addAll(List(n.size) { com.example.cas.graph.ColumnFormat() })
        widths.clear(); heights.clear()
        roleX = null; roleY = if (cells.size >= 2) { roleX = 0; 1 } else 0; roleSx = null; roleSy = null
    }
    // A column's statistics, or its fill with a series, being shown.
    var statsFor by remember(f) { mutableStateOf<Int?>(null) }
    var seriesFor by remember(f) { mutableStateOf<Int?>(null) }
    /** The column's first formula copied down to the last row, its references moving with it. */
    fun fillFormulaDown(c: Int) {
        val col = cells[c]
        val top = col.indexOfFirst { com.example.cas.graph.Sheet.isFormula(it) }.takeIf { it >= 0 } ?: return
        record()
        for (r in top + 1 until col.size) col[r] = com.example.cas.graph.Sheet.shift(col[top], r - top)
    }
    /** A table copied from a spreadsheet or a CSV file, added below the rows already filled. */
    fun pasteClipboard() {
        @Suppress("DEPRECATION")
        val text = (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
        val t = runCatching { com.example.cas.graph.Csv.parse(text) }.getOrNull()
        if (t == null || t.columns.isEmpty()) {
            android.widget.Toast.makeText(context, "No numbers on the clipboard", android.widget.Toast.LENGTH_SHORT).show(); return
        }
        removeEmptyRows()
        while (cells.size < t.columns.size) addColumn()
        val rowsNow = cells.maxOfOrNull { it.size } ?: 0
        val from = if (rowsNow == 1 && cells.all { it[0].isBlank() }) 0 else rowsNow
        val n = t.rows
        cells.forEach { col -> while (col.size < from + n) col.add("") }
        t.columns.forEachIndexed { c, values -> values.forEachIndexed { r, v -> cells[c][from + r] = com.example.cas.graph.DataTable.text(v) } }
        if (roleY == null) { if (cells.size >= 2) { roleX = roleX ?: 0; roleY = 1 } else roleY = 0 }
        android.widget.Toast.makeText(context, "$n row${if (n == 1) "" else "s"} pasted", android.widget.Toast.LENGTH_SHORT).show()
    }
    fun paste() {
        record(); quiet[0] = true
        try { pasteClipboard() } finally { quiet[0] = false }
    }
    statsFor?.takeIf { it in cells.indices }?.let { c ->
        ColumnStatsDialog(names[c].ifBlank { "Column ${c + 1}" }, table().stats(c), onDismiss = { statsFor = null })
    }
    formatFor?.takeIf { it in cells.indices }?.let { c ->
        NumberFormatDialog(names[c].ifBlank { "Column ${c + 1}" }, fmts.getOrElse(c) { com.example.cas.graph.ColumnFormat() }, sample = current.numbers(c).firstOrNull() ?: 1234.5678, onDismiss = { formatFor = null }) { chosen ->
            record(); formatFor = null
            while (fmts.size <= c) fmts.add(com.example.cas.graph.ColumnFormat())
            fmts[c] = chosen
        }
    }
    filterFor?.takeIf { it in cells.indices }?.let { c ->
        FilterDialog(names[c].ifBlank { "Column ${c + 1}" }, filters[c], onDismiss = { filterFor = null }, onRemove = { filters.remove(c); filterFor = null }) { flt ->
            filters[c] = com.example.cas.graph.SheetTools.Filter(c, flt.op, flt.value); filterFor = null
        }
    }
    seriesFor?.takeIf { it in cells.indices }?.let { c ->
        SeriesDialog(rows = maxOf(rows, 1), onDismiss = { seriesFor = null }) { start, step ->
            record(); seriesFor = null
            com.example.cas.graph.DataTable.series(start, step, cells[c].size).forEachIndexed { r, v -> cells[c][r] = v }
        }
    }
    // While a formula is typed, the cells it uses are outlined, each reference in its own color, as Excel does.
    val editText = editing?.let { (c, r) -> cells.getOrNull(c)?.getOrNull(r) }
    val refs = if (formulas && editText != null && editText.trimStart().startsWith("=")) com.example.cas.graph.Sheet.references(editText) else emptyList()
    val refColors = listOf(Color(0xFF1A73E8), Color(0xFFD93025), Color(0xFF9334E6), Color(0xFF188038), Color(0xFFE37400), Color(0xFF12A4B8))
    fun refColor(c: Int, r: Int): Color? = refs.indexOfFirst { c in it.c0..it.c1 && r >= it.r0 && r <= it.r1 }.takeIf { it >= 0 }?.let { refColors[it % refColors.size] }
    val list = androidx.compose.foundation.lazy.rememberLazyListState()
    val across = rememberScrollState()
    val scope = rememberCoroutineScope()
    fun widthOf(c: Int) = widths[c] ?: defaultWidth
    fun heightOf(r: Int) = heights[r] ?: defaultHeight
    // The rows the filters keep; each color-scaled column's smallest and largest number; the cells find matches.
    val visible by remember(f) { androidx.compose.runtime.derivedStateOf { if (filters.isEmpty()) (0 until current.rowCount).toList() else com.example.cas.graph.SheetTools.visibleRows(current, filters.values) } }
    val scales by remember(f) { androidx.compose.runtime.derivedStateOf { fmts.indices.filter { fmts[it].colorScale || fmts[it].dataBars }.associateWith { c -> current.numbers(c).let { v -> if (v.isEmpty()) 0.0 to 0.0 else v.min() to v.max() } } } }
    val found by remember(f) { androidx.compose.runtime.derivedStateOf { if (!finding || query.isEmpty()) emptyList() else com.example.cas.graph.SheetTools.find(cells.map { it.toList() }, query, matchCase, wholeCell) } }
    /** The next (or previous) match: shown, scrolled to and opened, with its filters cleared if they hide it. */
    fun goToMatch(step: Int) {
        if (found.isEmpty()) return
        matchAt = Math.floorMod(matchAt + step, found.size)
        val (c, r) = found[matchAt]
        if (r !in visible) filters.clear()
        scope.launch {
            list.animateScrollToItem(maxOf(0, visible.indexOf(r) - 2).coerceAtLeast(0))
            across.animateScrollTo(with(density) { (0 until c).sumOf { (widthOf(it) + 10.dp).toPx().toInt() } })
        }
    }
    /** Every match replaced, one step to undo. */
    fun replaceAll() {
        val n = found.size
        if (n == 0) return
        record(); editing = null
        cells.forEach { col -> col.indices.forEach { r -> col[r] = com.example.cas.graph.SheetTools.replace(col[r], query, replacement, matchCase, wholeCell) } }
        android.widget.Toast.makeText(context, "$n cell${if (n == 1) "" else "s"} replaced", android.widget.Toast.LENGTH_SHORT).show()
    }
    /** Whether a cell is in the range the fill handle is being dragged over. */
    fun inFill(c: Int, r: Int): Boolean {
        val (sc, sr) = editing ?: return false
        val (tc, tr) = fillTo ?: return false
        return c in minOf(sc, tc)..maxOf(sc, tc) && r in minOf(sr, tr)..maxOf(sr, tr) && !(c == sc && r == sr)
    }
    /** The fill handle let go: the cell copied over the range, a formula's references moving with it (as Excel's fill). */
    fun fill() {
        val (sc, sr) = editing ?: return
        val (tc, tr) = fillTo ?: return
        fillTo = null
        val t = cells.getOrNull(sc)?.getOrNull(sr) ?: return
        record()
        for (c in minOf(sc, tc)..maxOf(sc, tc)) for (r in minOf(sr, tr)..maxOf(sr, tr)) {
            if (c == sc && r == sr) continue
            val col = cells.getOrNull(c) ?: continue
            if (r >= col.size) continue
            // Formulas move their references; text counts on (Week 1, Week 2; Jan, Feb), as Excel fills.
            col[r] = if (com.example.cas.graph.Sheet.isFormula(t)) com.example.cas.graph.Sheet.shift(t, r - sr, c - sc) else com.example.cas.graph.SheetTools.fillValue(t, (r - sr) + (c - sc))
        }
    }
    /**
     * Double-tapping the fill handle: down to the end of the data, as Excel does. The data ends
     * at the last filled row of the next column over (left, else right), else of any column.
     */
    fun fillToEnd() {
        val (sc, sr) = editing ?: return
        fun lastFilled(c: Int) = cells.getOrNull(c)?.indexOfLast { it.isNotBlank() } ?: -1
        val end = listOf(sc - 1, sc + 1).map { lastFilled(it) }.firstOrNull { it > sr }
            ?: cells.indices.filter { it != sc }.maxOfOrNull { lastFilled(it) }?.takeIf { it > sr }
            ?: return
        fillTo = sc to end
        fill()
    }
    /** The cell [px] pixels from (c, r) along one axis: whole rows (or columns) passed by half their size. */
    fun stepsAlong(start: Int, px: Float, count: Int, sizeOf: (Int) -> Float): Int {
        var at = start; var left = px
        if (px > 0) while (at + 1 < count && left > sizeOf(at + 1) / 2) { left -= sizeOf(at + 1); at++ }
        else while (at - 1 >= 0 && -left > sizeOf(at - 1) / 2) { left += sizeOf(at - 1); at-- }
        return at
    }
    // The formula bar: the selected cell's address and contents, typed in here.
    val formulaBar: @Composable (Modifier) -> Unit = { m -> Column(m) {
        val sel = editing?.takeIf { (c, r) -> c in cells.indices && r < cells[c].size }
        // Arrow keys: move the selected cell without leaving the keyboard (down adds a row at the end).
        if (sel != null) CellArrows(
            onMove = { dc, dr ->
                val (c, r) = sel
                val nc = (c + dc).coerceIn(0, cells.size - 1)
                if (r + dr >= rows) addRow()
                val nr = (r + dr).coerceAtLeast(0)
                editing = nc to nr; focusCol = nc
                scope.launch { list.animateScrollToItem(maxOf(0, visible.indexOf(nr) - 2)) }
            },
            modifier = Modifier.padding(bottom = 6.dp),
        )
        val anchoredNow = if (formulas && editText != null) com.example.cas.graph.Sheet.cycleAnchor(editText) else null
        FormulaBar(
            address = sel?.let { (c, r) -> com.example.cas.graph.Sheet.columnName(c) + (r + 1) },
            text = editText.orEmpty(), formulas = formulas,
            referenceColors = refs.mapIndexed { k, ref -> ref.at to refColors[k % refColors.size] },
            anchored = anchoredNow,
            onAnchor = {
                val (c, r) = editing ?: return@FormulaBar
                if (typingRecorded != (c to r)) { record(); typingRecorded = c to r }
                if (anchoredNow != null && r < cells[c].size) cells[c][r] = anchoredNow
            },
            focus = barFocus,
            onChange = { t ->
                val (c, r) = editing ?: return@FormulaBar
                // Typing in a cell is one step to undo, however many characters.
                if (typingRecorded != (c to r)) { record(); typingRecorded = c to r }
                if (r < cells[c].size) cells[c][r] = t
            },
            onNext = {
                // Down the column, adding a row at the end.
                val (c, r) = editing ?: return@FormulaBar
                if (r + 1 >= rows) addRow()
                editing = c to r + 1
                scope.launch { list.animateScrollToItem(maxOf(0, visible.indexOf(r) - 2)) }
            },
        )
    } }
    // A cell picked: straight to typing in it, as tapping a cell did before.
    androidx.compose.runtime.LaunchedEffect(editing) { if (editing != null) runCatching { barFocus.requestFocus() } }
    highlightFor?.takeIf { it in cells.indices }?.let { c ->
        val fmt = fmts.getOrElse(c) { com.example.cas.graph.ColumnFormat() }
        HighlightDialog(names[c].ifBlank { "Column ${c + 1}" }, fmt.highlight, onDismiss = { highlightFor = null }, onRemove = {
            record(); highlightFor = null; while (fmts.size <= c) fmts.add(com.example.cas.graph.ColumnFormat()); fmts[c] = fmt.copy(highlight = null)
        }) { rule -> record(); highlightFor = null; while (fmts.size <= c) fmts.add(com.example.cas.graph.ColumnFormat()); fmts[c] = fmt.copy(highlight = rule) }
    }
    insightsFor?.takeIf { it in cells.indices }?.let { c ->
        val x = roleX?.takeIf { it != c && it in cells.indices }
        val pairs = (0 until rows).mapNotNull { r -> val yv = current.value(c, r); val xv = if (x != null) current.value(x, r) else (r + 1).toDouble(); if (yv != null && xv != null) xv to yv else null }
        val out = com.example.cas.graph.SheetTools.outliers(current, c)
        InsightsSheet(
            letter = com.example.cas.graph.Sheet.columnName(c), name = names[c].ifBlank { "Column ${c + 1}" },
            stats = current.stats(c), histogram = com.example.cas.graph.SheetTools.histogram(current.numbers(c)),
            against = x?.let { names[it].ifBlank { com.example.cas.graph.Sheet.columnName(it) } } ?: "the row number",
            trend = com.example.cas.graph.SheetTools.trend(pairs.map { it.first }, pairs.map { it.second }),
            outliers = out.map { it + 1 },
            onMark = { marked = out.map { c to it }.toSet(); insightsFor = null },
            onDismiss = { insightsFor = null },
        )
    }
    // A column's sheet: its role and every action.
    sheetFor?.takeIf { it in cells.indices }?.let { c ->
        val st = current.stats(c)
        val fmt = fmts.getOrElse(c) { com.example.cas.graph.ColumnFormat() }
        fun close() { sheetFor = null }
        ColumnSheet(
            letter = com.example.cas.graph.Sheet.columnName(c),
            name = names[c].ifBlank { "Column ${c + 1}" },
            summary = if (st == null) "No numbers yet" else "${st.n} number${if (st.n == 1) "" else "s"} · mean ${shortNumber(st.mean)} · ${shortNumber(st.min)} to ${shortNumber(st.max)}",
            role = roleOf(c), onRole = { assign(c, it) },
            actions = listOfNotNull(
                ColumnAction(TableIcons.SortUp, "Sort up") { close(); sortBy(c) },
                ColumnAction(TableIcons.SortDown, "Sort down") { close(); sortDown(c) },
                ColumnAction(TableIcons.Filter, if (c in filters) "Change filter" else "Filter", on = c in filters) { close(); filterFor = c },
                ColumnAction(TableIcons.Statistics, "Statistics") { close(); statsFor = c },
                ColumnAction(TableIcons.FormatFixed, "Number format") { close(); formatFor = c },
                ColumnAction(TableIcons.ColorScale, "Color scale", on = fmt.colorScale) { record(); while (fmts.size <= c) fmts.add(com.example.cas.graph.ColumnFormat()); fmts[c] = fmt.copy(colorScale = !fmt.colorScale) },
                ColumnAction(TableIcons.DataBars, "Data bars", on = fmt.dataBars) { record(); while (fmts.size <= c) fmts.add(com.example.cas.graph.ColumnFormat()); fmts[c] = fmt.copy(dataBars = !fmt.dataBars) },
                ColumnAction(TableIcons.Highlight, "Highlight rule", on = fmt.highlight != null) { close(); highlightFor = c },
                ColumnAction(TableIcons.Insights, "Insights") { close(); insightsFor = c },
                ColumnAction(TableIcons.Numbering, "Fill 1, 2, 3") { close(); record(); cells[c].indices.forEach { r -> cells[c][r] = (r + 1).toString() } },
                ColumnAction(TableIcons.Series, "Fill series") { close(); seriesFor = c },
                if (formulas && cells[c].any { com.example.cas.graph.Sheet.isFormula(it) }) ColumnAction(TableIcons.FillFormula, "Fill formula down") { close(); fillFormulaDown(c) } else null,
                ColumnAction(TableIcons.DuplicateColumn, "Duplicate") { close(); duplicateColumn(c) },
                if (c > 0) ColumnAction(TableIcons.MoveLeft, "Move left") { sheetFor = c - 1; focusCol = c - 1; moveColumn(c, c - 1) } else null,
                if (c < cells.size - 1) ColumnAction(TableIcons.MoveRight, "Move right") { sheetFor = c + 1; focusCol = c + 1; moveColumn(c, c + 1) } else null,
                ColumnAction(TableIcons.ClearColumn, "Clear") { close(); record(); cells[c].indices.forEach { r -> cells[c][r] = "" } },
                if (cells.size > 1) ColumnAction(TableIcons.DeleteColumn, "Delete", danger = true) { close(); removeColumn(c); focusCol = focusCol.coerceAtMost(cells.size - 1) } else null,
            ),
            onDismiss = { close() },
        )
    }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = colors.surfaceContainerLow) {
            BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
                val wide = maxWidth >= 840.dp
                Column(Modifier.fillMaxSize()) {
                    // Top bar: close, the title, Done.
                    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = tr("Close without saving")) }
                        Column(Modifier.weight(1f).padding(start = 4.dp)) {
                            // Short on a phone, where the top bar is crowded.
                            Text(tr(if (wide) "Data table" else "Data"), style = MaterialTheme.typography.titleLarge, maxLines = 1)
                            // What's plotted, and anything wrong, in a line under the title.
                            val counts = (if (roleY == null) "Pick a y column" else "$points point${if (points == 1) "" else "s"}") + " · ${cells.size} column${if (cells.size == 1) "" else "s"}" +
                                (if (bad > 0) " · $bad not number${if (bad == 1) "" else "s"}" else "")
                            Text(counts, style = MaterialTheme.typography.bodySmall, color = if (roleY == null || bad > 0) colors.error else colors.onSurfaceVariant, maxLines = 1)
                        }
                        IconButton(onClick = { undo() }, enabled = undoStack.isNotEmpty()) { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = tr("Undo")) }
                        IconButton(onClick = { redo() }, enabled = redoStack.isNotEmpty()) { Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = tr("Redo")) }
                        // More: the table as CSV (shared or copied), and tidying up.
                        var more by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { more = true }) { Icon(Icons.Default.MoreVert, contentDescription = tr("More")) }
                            DropdownMenu(expanded = more, onDismissRequest = { more = false }, shape = RoundedCornerShape(16.dp)) {
                                DropdownMenuItem(leadingIcon = { Icon(Icons.Default.Share, null) }, text = { Text(tr("Share as CSV")) }, onClick = {
                                    more = false
                                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/csv").putExtra(android.content.Intent.EXTRA_TEXT, table().csv())
                                    context.startActivity(android.content.Intent.createChooser(send, "Share the table"))
                                })
                                DropdownMenuItem(leadingIcon = { Icon(Icons.Default.ContentCopy, null) }, text = { Text(tr("Copy as CSV")) }, onClick = {
                                    more = false
                                    @Suppress("DEPRECATION")
                                    (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("Table", table().csv()))
                                    android.widget.Toast.makeText(context, "Table copied", android.widget.Toast.LENGTH_SHORT).show()
                                })
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        Button(
                            enabled = roleY != null && points > 0,
                            onClick = { vm.setTable(f, table()); onDismiss() },
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 20.dp),
                            modifier = Modifier.height(48.dp),
                        ) { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(tr("Done")) }
                    }
                    // The toolbar: tabs, then their tools (on a phone, put away while the keyboard is up).
                    val keyboardUp = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(density) > 0
                    if (wide || !keyboardUp) {
                        val c = focusCol.coerceIn(0, maxOf(0, cells.size - 1))
                        val fmt = fmts.getOrElse(c) { com.example.cas.graph.ColumnFormat() }
                        fun setFmt(n: com.example.cas.graph.ColumnFormat) { record(); while (fmts.size <= c) fmts.add(com.example.cas.graph.ColumnFormat()); fmts[c] = n }
                        val selRow = editing?.second ?: selA?.second
                        val groups = when (tab) {
                            TableTab.Home -> listOf(
                                // Excel's AutoSum: the selected cell gets the function of the numbers above (or to its left).
                                ToolGroup("Formula", listOf(TableTool(TableIcons.AutoSum, "AutoSum", menu = com.example.cas.graph.SheetTools.AUTO_FUNCTIONS.map { fn ->
                                    fn to {
                                        val at = editing
                                        val formula = at?.let { (cc, rr) -> com.example.cas.graph.SheetTools.autoSum(current, cc, rr, fn) }
                                        if (at == null || formula == null) android.widget.Toast.makeText(context, "Select the cell under (or right of) some numbers", android.widget.Toast.LENGTH_SHORT).show()
                                        else { record(); cells[at.first][at.second] = formula }
                                    }
                                }))),
                                ToolGroup("Sort & filter", listOf(
                                    TableTool(TableIcons.SortUp, "Sort up") { sortBy(c) },
                                    TableTool(TableIcons.SortDown, "Sort down") { sortDown(c) },
                                    TableTool(TableIcons.Filter, "Filter", on = c in filters) { filterFor = c },
                                    TableTool(TableIcons.Find, "Find", on = finding) { finding = !finding },
                                )),
                                ToolGroup("Fill", listOf(
                                    TableTool(TableIcons.Numbering, "1, 2, 3") { record(); cells[c].indices.forEach { r -> cells[c][r] = (r + 1).toString() } },
                                    TableTool(TableIcons.Series, "Series") { seriesFor = c },
                                    TableTool(TableIcons.FillFormula, "Formula", enabled = formulas && cells.getOrNull(c)?.any { com.example.cas.graph.Sheet.isFormula(it) } == true) { fillFormulaDown(c) },
                                )),
                                ToolGroup("Column", listOf(TableTool(TableIcons.Statistics, "Stats") { statsFor = c })),
                            )
                            TableTab.Insert -> listOf(
                                ToolGroup("Rows", listOf(
                                    TableTool(TableIcons.RowAbove, "Row above") { insertRow(selRow ?: 0) },
                                    TableTool(TableIcons.RowBelow, "Row below") { if (selRow != null) insertRow(selRow + 1) else addRow() },
                                    TableTool(Icons.Default.ContentCopy, "Copy row", enabled = selRow != null) { selRow?.let { duplicateRow(it) } },
                                )),
                                ToolGroup("Columns", listOf(
                                    TableTool(TableIcons.ColumnAdd, "Column") { addColumn(); scope.launch { across.animateScrollTo(across.maxValue + 10_000) } },
                                    TableTool(TableIcons.DuplicateColumn, "Copy column") { duplicateColumn(c) },
                                )),
                                ToolGroup("Clipboard", listOf(TableTool(TableIcons.PasteTable, "Paste") { paste() })),
                            )
                            TableTab.Data -> listOf(
                                ToolGroup("Tidy", listOf(
                                    TableTool(TableIcons.Dedupe, "Duplicates") { removeDuplicates() },
                                    TableTool(TableIcons.RemoveEmpty, "Empty rows") { removeEmptyRows() },
                                    TableTool(TableIcons.Trim, "Trim") { trimAll() },
                                    TableTool(TableIcons.Transpose, "Transpose") { transpose() },
                                )),
                                ToolGroup("Explore", listOf(TableTool(TableIcons.Insights, "Insights") { insightsFor = c })),
                                ToolGroup("View", listOf(TableTool(TableIcons.Freeze, "Freeze", on = frozen) { record(); frozen = !frozen })),
                                ToolGroup("Share", listOf(
                                    TableTool(TableIcons.ShareTable, "Share CSV") {
                                        val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/csv").putExtra(android.content.Intent.EXTRA_TEXT, table().csv())
                                        context.startActivity(android.content.Intent.createChooser(send, "Share the table"))
                                    },
                                    TableTool(TableIcons.CopyTable, "Copy CSV") {
                                        @Suppress("DEPRECATION")
                                        (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("Table", table().csv()))
                                        android.widget.Toast.makeText(context, "Table copied", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                )),
                            )
                            TableTab.Format -> listOf(
                                ToolGroup("Number", listOf(
                                    TableTool(TableIcons.FormatAuto, "Auto", on = !fmt.changesNumbers) { setFmt(fmt.copy(decimals = null, percent = false, scientific = false, thousands = false)) },
                                    TableTool(TableIcons.FormatFixed, "0.00", on = fmt.decimals != null && !fmt.percent && !fmt.scientific) { setFmt(fmt.copy(decimals = fmt.decimals ?: 2, percent = false, scientific = false)) },
                                    TableTool(TableIcons.Percent, "Percent", on = fmt.percent) { setFmt(fmt.copy(percent = !fmt.percent, scientific = false)) },
                                    TableTool(TableIcons.Scientific, "Scientific", on = fmt.scientific) { setFmt(fmt.copy(scientific = !fmt.scientific, percent = false, thousands = false)) },
                                    TableTool(TableIcons.Thousands, "1 000", on = fmt.thousands, enabled = !fmt.scientific) { setFmt(fmt.copy(thousands = !fmt.thousands)) },
                                )),
                                ToolGroup("Decimals", listOf(
                                    TableTool(TableIcons.DecimalsLess, "Fewer", enabled = (fmt.decimals ?: 2) > 0) { setFmt(fmt.copy(decimals = ((fmt.decimals ?: 3) - 1).coerceAtLeast(0))) },
                                    TableTool(TableIcons.DecimalsMore, "More") { setFmt(fmt.copy(decimals = ((fmt.decimals ?: 1) + 1).coerceAtMost(8))) },
                                )),
                                ToolGroup("Highlight", listOf(
                                    TableTool(TableIcons.ColorScale, "Color scale", on = fmt.colorScale) { setFmt(fmt.copy(colorScale = !fmt.colorScale)) },
                                    TableTool(TableIcons.DataBars, "Data bars", on = fmt.dataBars) { setFmt(fmt.copy(dataBars = !fmt.dataBars)) },
                                    TableTool(TableIcons.Highlight, "Rule", on = fmt.highlight != null) { highlightFor = c },
                                )),
                            )
                        }
                        TableToolbar(tab, { tab = it }, groups, wide, Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                    }
                    if (finding) FindBar(
                        query = query, onQuery = { query = it; matchAt = 0 }, replacement = replacement, onReplacement = { replacement = it },
                        matchCase = matchCase, onMatchCase = { matchCase = it }, wholeCell = wholeCell, onWholeCell = { wholeCell = it },
                        count = found.size, at = matchAt, onNext = { goToMatch(1) }, onPrevious = { goToMatch(-1) },
                        onReplace = {
                            // The current match replaced, then on to the next.
                            found.getOrNull(matchAt)?.let { (c, r) ->
                                record(); cells[c][r] = com.example.cas.graph.SheetTools.replace(cells[c][r], query, replacement, matchCase, wholeCell)
                            }
                        },
                        onReplaceAll = { replaceAll() },
                        onClose = { finding = false },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                    // Filters in use: how many rows show, a chip for each (tap × to drop it).
                    if (filters.isNotEmpty()) FilterBanner(
                        shown = visible.size, total = rows,
                        chips = filters.toSortedMap().map { (c, flt) -> (names.getOrElse(c) { "" }.ifBlank { com.example.cas.graph.Sheet.columnName(c) } + " " + flt.summary) to c },
                        onEdit = { filterFor = it }, onRemove = { filters.remove(it) }, onClear = { filters.clear() },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    )
                    val sa = selA
                    if (sa != null) {
                        // A picked range: its address and its numbers summed up, as a spreadsheet's status bar.
                        val sb = selB ?: sa
                        val sum = com.example.cas.graph.SheetTools.summary(current, sa.first, sa.second, sb.first, sb.second)
                        val address = com.example.cas.graph.Sheet.columnName(minOf(sa.first, sb.first)) + (minOf(sa.second, sb.second) + 1) +
                            if (sa == sb) "" else ":" + com.example.cas.graph.Sheet.columnName(maxOf(sa.first, sb.first)) + (maxOf(sa.second, sb.second) + 1)
                        SelectionBar(
                            address, sum,
                            onCopy = {
                                @Suppress("DEPRECATION")
                                (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("Cells", com.example.cas.graph.SheetTools.rangeText(current, sa.first, sa.second, sb.first, sb.second)))
                                android.widget.Toast.makeText(context, "Copied", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onClear = {
                                record()
                                for (c in minOf(sa.first, sb.first)..maxOf(sa.first, sb.first)) for (r in minOf(sa.second, sb.second)..maxOf(sa.second, sb.second)) cells.getOrNull(c)?.let { col -> if (r < col.size) col[r] = "" }
                            },
                            onClose = { selA = null; selB = null },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        if (wide) TableSidePane(current, points, cells.size, bad, roleY == null, Modifier.width(320.dp).fillMaxHeight().padding(start = 16.dp, bottom = 16.dp)) {
                            // The selected column: its role, format, filter and statistics.
                            val c = focusCol.coerceIn(0, maxOf(0, cells.size - 1))
                            val fmt = fmts.getOrElse(c) { com.example.cas.graph.ColumnFormat() }
                            ColumnInspector(
                                com.example.cas.graph.Sheet.columnName(c), names.getOrElse(c) { "" }.ifBlank { "Column ${c + 1}" }, roleOf(c), { assign(c, it) },
                                format = listOfNotNull(
                                    when { fmt.scientific -> "scientific"; fmt.percent -> "percent"; fmt.decimals != null -> "${fmt.decimals} decimals"; else -> "as typed" },
                                    if (fmt.thousands) "1 000" else null, if (fmt.colorScale) "color scale" else null,
                                ).joinToString(" · "),
                                filter = filters[c]?.summary, stats = current.stats(c), onOpen = { sheetFor = c },
                            )
                        }
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            // The sheet: column cards on top, then the rows; both scroll sideways together.
                            Column(Modifier.fillMaxSize().padding(horizontal = if (wide) 16.dp else 8.dp).clip(RoundedCornerShape(28.dp)).background(colors.surface)) {
                                // On a tablet the formula bar sits above the grid, as in Excel.
                                if (wide) formulaBar(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp))
                                // A column's card and the grip after it (drag to resize, double-tap for the usual width).
                                val columnHeader: @Composable (Int) -> Unit = { c ->
                                    ColumnCard(
                                        role = roleOf(c), name = names[c], index = c, width = widthOf(c),
                                        letter = if (formulas) com.example.cas.graph.Sheet.columnName(c) else null,
                                        onName = { if (typingRecorded != (-1 to c)) { record(); typingRecorded = -1 to c }; names[c] = it }, onRole = { assign(c, it) },
                                        onSort = { sortBy(c) },
                                        onSortDown = { sortDown(c) },
                                        onFill = { record(); cells[c].indices.forEach { r -> cells[c][r] = (r + 1).toString() } },
                                        onSeries = { seriesFor = c },
                                        onStats = { statsFor = c },
                                        onDuplicate = { duplicateColumn(c) },
                                        onMoveLeft = if (c > 0) ({ moveColumn(c, c - 1) }) else null,
                                        onMoveRight = if (c < cells.size - 1) ({ moveColumn(c, c + 1) }) else null,
                                        onFillDown = if (formulas && cells[c].any { com.example.cas.graph.Sheet.isFormula(it) }) ({ fillFormulaDown(c) }) else null,
                                        onClear = { record(); cells[c].indices.forEach { r -> cells[c][r] = "" } },
                                        onRemove = if (cells.size > 1) ({ removeColumn(c) }) else null,
                                        format = fmts.getOrElse(c) { com.example.cas.graph.ColumnFormat() },
                                        onFormat = { formatFor = c },
                                        onColorScale = { on -> record(); while (fmts.size <= c) fmts.add(com.example.cas.graph.ColumnFormat()); fmts[c] = fmts[c].copy(colorScale = on) },
                                        filtered = c in filters,
                                        onFilter = { filterFor = c },
                                        onOpen = { sheetFor = c; focusCol = c },
                                        chosen = c == focusCol,
                                    )
                                    ResizeGrip(
                                        vertical = true,
                                        description = "Resize column ${c + 1}",
                                        onDrag = { px -> widths[c] = (widthOf(c) + with(density) { px.toDp() }).coerceIn(56.dp, 480.dp) },
                                        onReset = { widths.remove(c) },
                                        modifier = Modifier.width(10.dp).height(64.dp),
                                    )
                                }
                                // With the first column frozen it stands outside the sideways scroll.
                                val pinned = frozen && cells.size > 1
                                val scrolled = if (pinned) 1 until cells.size else cells.indices
                                Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
                                    Spacer(Modifier.width(52.dp))
                                    if (pinned) Row(Modifier.background(colors.surface)) { columnHeader(0) }
                                    Row(Modifier.weight(1f).horizontalScroll(across)) {
                                        scrolled.forEach { c -> columnHeader(c) }
                                        Spacer(Modifier.width(12.dp))
                                    }
                                }
                                // A cell: its text, or its formula's value, as its column's format shows numbers;
                                // shaded by the column's color scale; lit when selected or found.
                                val cellView: @Composable (Int, Int) -> Unit = { c, r ->
                                    val col = cells[c]
                                    val t = col.getOrElse(r) { "" }
                                    val role = roleOf(c)
                                    val fmt = fmts.getOrElse(c) { com.example.cas.graph.ColumnFormat() }
                                    // A formula shows what it works out to until it's tapped.
                                    val worked = if (formulas && com.example.cas.graph.Sheet.isFormula(t)) current.sheet.value(c, r) else null
                                    val error = worked?.error != null || (role != null && t.isNotBlank() && worked == null && com.example.cas.graph.DataTable.number(t) == null) || (role != null && worked != null && worked.number == null)
                                    val here = editing == (c to r)
                                    val number = if (fmt.changesNumbers || fmt.colorScale || fmt.dataBars) current.value(c, r) else null
                                    val display = if (fmt.changesNumbers) number?.let { fmt.show(it) } else null
                                    // A highlight rule's tint wins over the color scale, as the first matching rule does in Sheets.
                                    val ruleTint = fmt.highlight?.takeIf { it.matches(current, c, r) }?.let { Color(com.example.cas.graph.HighlightRule.COLORS[it.color]) }
                                    val shade = ruleTint ?: if (fmt.colorScale && number != null) scales[c]?.let { (lo, hi) -> Color(com.example.cas.graph.SheetTools.scaleColor(com.example.cas.graph.SheetTools.scalePosition(number, lo, hi))) } else null
                                    val barLength = if (fmt.dataBars && number != null) scales[c]?.let { (lo, hi) -> val a = minOf(0.0, lo); val b = maxOf(0.0, hi); if (b > a) ((number - a) / (b - a)).toFloat() else null } else null
                                    val found = finding && com.example.cas.graph.SheetTools.matches(t, query, matchCase, wholeCell)
                                    Box(Modifier.width(widthOf(c)).height(heightOf(r))) {
                                        TableCell(
                                            t, error, Modifier.fillMaxSize(), role = role, shown = if (worked != null) (if (display != null && worked.number != null) display else worked.toString()) else null, formulas = formulas,
                                            // Typed in the formula bar; the cell is outlined while it's the one selected.
                                            editing = false,
                                            highlight = if (inFill(c, r)) colors.primary else if (found) colors.tertiary else if ((c to r) in marked) colors.error else refColor(c, r),
                                            display = display, fill = shade, selected = here || inSelection(c, r), bar = barLength,
                                            onTap = {
                                                // With a range picked, a tap stretches it; otherwise it selects the cell to type in.
                                                if (selA != null) selB = c to r else { editing = c to r; focusCol = c }
                                            },
                                            onLongPress = { editing = null; selA = c to r; selB = c to r; focusCol = c },
                                        ) {}
                                        // The fill handle: drag it down or across to copy the cell, as in Excel.
                                        if (here && t.isNotBlank()) FillHandle(
                                            onStart = { fillTo = c to r },
                                            onDrag = { dx, dy ->
                                                val gap = with(density) { 6.dp.toPx() }
                                                fillTo = if (kotlin.math.abs(dy) >= kotlin.math.abs(dx))
                                                    c to stepsAlong(r, dy, rows) { k -> with(density) { heightOf(k).toPx() } + gap }
                                                else stepsAlong(c, dx, cells.size) { k -> with(density) { (widthOf(k) + 10.dp).toPx() } } to r
                                            },
                                            onEnd = { fill() },
                                            onDoubleTap = { fillToEnd() },
                                            onCancel = { fillTo = null },
                                            modifier = Modifier.align(Alignment.BottomEnd),
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                }
                                androidx.compose.foundation.lazy.LazyColumn(state = list, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 96.dp)) {
                                    // Only the rows the filters keep (all of them without filters).
                                    items(visible.size, key = { visible[it] }) { k ->
                                        val r = visible[k]
                                        Row(
                                            Modifier.fillMaxWidth().background(if (k % 2 == 0) Color.Transparent else colors.surfaceContainerLowest.copy(alpha = 0.6f)).padding(vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(Modifier.width(52.dp).height(heightOf(r))) {
                                                RowNumber(r, onInsertAbove = { insertRow(r) }, onInsertBelow = { insertRow(r + 1) }, onDuplicate = { duplicateRow(r) }, onRemove = if (rows > 1) ({ removeRow(r) }) else null, modifier = Modifier.align(Alignment.Center))
                                                // The grip under the number: drag to resize the row, double-tap for the usual height.
                                                ResizeGrip(
                                                    vertical = false,
                                                    description = "Resize row ${r + 1}",
                                                    onDrag = { px -> heights[r] = (heightOf(r) + with(density) { px.toDp() }).coerceIn(32.dp, 240.dp) },
                                                    onReset = { heights.remove(r) },
                                                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(12.dp),
                                                )
                                            }
                                            if (pinned) Row(Modifier.background(colors.surface)) { cellView(0, r) }
                                            Row(Modifier.weight(1f).horizontalScroll(across)) {
                                                scrolled.forEach { c -> cellView(c, r) }
                                                Spacer(Modifier.width(12.dp))
                                            }
                                        }
                                    }
                                }
                                // While a function's name is typed in a formula: matching functions, one tap to use.
                                val typing = editText?.let { com.example.cas.graph.Sheet.typingName(it) }
                                val matches = remember(typing) { typing?.let { com.example.cas.graph.Sheet.suggestions(it) }.orEmpty() }
                                if (formulas && matches.isNotEmpty()) FormulaSuggestions(matches) { name ->
                                    val (c, r) = editing ?: return@FormulaSuggestions
                                    val now = cells.getOrNull(c)?.getOrNull(r) ?: return@FormulaSuggestions
                                    if (typingRecorded != (c to r)) { record(); typingRecorded = c to r }
                                    cells[c][r] = now.dropLast(typing!!.length) + name + "("
                                }
                            }
                            // The summary pill, bottom left: the picked range's numbers, else the selected column's.
                            val pa = selA
                            if (pa != null) {
                                val pb = selB ?: pa
                                StatusPill("", com.example.cas.graph.SheetTools.summary(current, pa.first, pa.second, pb.first, pb.second), all = wide,
                                    Modifier.align(Alignment.BottomStart).padding(start = if (wide) 32.dp else 20.dp, bottom = 24.dp))
                            } else if (cells.isNotEmpty() && rows > 0) {
                                val c = focusCol.coerceIn(0, cells.size - 1)
                                StatusPill(com.example.cas.graph.Sheet.columnName(c), com.example.cas.graph.SheetTools.summary(current, c, 0, c, rows - 1), all = wide,
                                    Modifier.align(Alignment.BottomStart).padding(start = if (wide) 32.dp else 20.dp, bottom = 24.dp))
                            }
                            // One button, bottom right: + opens to add a row or a column, or paste.
                            Box(Modifier.align(Alignment.BottomEnd).padding(end = if (wide) 32.dp else 24.dp, bottom = 20.dp)) {
                                FabMenu(
                                    listOf(
                                        FabItem("Row", Icons.Default.TableRows, "Add a row") {
                                            addRow(); scope.launch { list.animateScrollToItem(maxOf(0, (cells.maxOfOrNull { it.size } ?: 1) - 1)) }
                                        },
                                        FabItem("Column", Icons.Default.ViewColumn, "Add a column") {
                                            addColumn(); scope.launch { across.animateScrollTo(across.maxValue + 10_000) }
                                        },
                                        FabItem("Paste", Icons.Default.ContentPaste, "Paste a table from the clipboard") { paste() },
                                    ),
                                    size = 56.dp,
                                    description = "Add a row or a column, or paste a table",
                                    alignEnd = true,
                                )
                            }
                        }
                    }
                    // On a phone the formula bar is at the bottom, in reach of a thumb and just over the keyboard.
                    if (!wide) formulaBar(Modifier.padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 8.dp))
                }
            }
        }
    }
}

/** A column's numbers summed up: how many, total, mean, median, spread, smallest and largest. */
@Composable
private fun ColumnStatsDialog(name: String, stats: com.example.cas.graph.DataTable.Stats?, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Calculate, contentDescription = null) },
        title = { Text(name) },
        text = {
            if (stats == null) Text(tr("No numbers in this column yet."), color = colors.onSurfaceVariant)
            else Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                listOfNotNull(
                    "Count" to stats.n.toString(), "Sum" to shortNumber(stats.sum), "Mean" to shortNumber(stats.mean), "Median" to shortNumber(stats.median),
                    stats.sd?.let { "Standard deviation" to shortNumber(it) }, "Smallest" to shortNumber(stats.min), "Largest" to shortNumber(stats.max),
                    "Range" to shortNumber(stats.max - stats.min),
                ).forEach { (label, value) ->
                    // Tap a value to copy it.
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClickLabel = "Copy $label") { clipboard.setText(AnnotatedString(value.replace("−", "-"))) }.padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(tr(label), modifier = Modifier.weight(1f), color = colors.onSurfaceVariant)
                        Text(geometryValueText(value), style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 18.sp), color = colors.onSurface)
                    }
                }
                Text(tr("Text and empty cells are left out; the standard deviation is the sample's. Tap a value to copy it."), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Close")) } },
    )
}

/** Fill a column with a series: a start and a step (0, 0.5, 1, 1.5…). */
@Composable
private fun SeriesDialog(rows: Int, onDismiss: () -> Unit, onFill: (Double, Double) -> Unit) {
    var start by remember { mutableStateOf("1") }
    var step by remember { mutableStateOf("1") }
    val a = com.example.cas.graph.DataTable.number(start); val d = com.example.cas.graph.DataTable.number(step)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Timeline, contentDescription = null) },
        title = { Text(tr("Fill with a series")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val keys = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                OutlinedTextField(start, { start = it }, label = { Text(tr("Start")) }, singleLine = true, isError = a == null, keyboardOptions = keys, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(step, { step = it }, label = { Text(tr("Step")) }, singleLine = true, isError = d == null, keyboardOptions = keys, modifier = Modifier.fillMaxWidth())
                if (a != null && d != null) Text(
                    com.example.cas.graph.DataTable.series(a, d, minOf(rows, 4)).joinToString(", ") + if (rows > 4) ", … (${rows} rows)" else "",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(enabled = a != null && d != null, onClick = { onFill(a!!, d!!) }) { Text(tr("Fill")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}

/** The fill handle: a small square at a cell's corner that drags over the cells to fill. */
@Composable
private fun FillHandle(onStart: () -> Unit, onDrag: (Float, Float) -> Unit, onEnd: () -> Unit, onCancel: () -> Unit, onDoubleTap: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val drag by androidx.compose.runtime.rememberUpdatedState(onDrag)
    val start by androidx.compose.runtime.rememberUpdatedState(onStart)
    val end by androidx.compose.runtime.rememberUpdatedState(onEnd)
    val cancel by androidx.compose.runtime.rememberUpdatedState(onCancel)
    val double by androidx.compose.runtime.rememberUpdatedState(onDoubleTap)
    // A larger touch area around the small square.
    Box(
        modifier
            .size(28.dp)
            .semantics { contentDescription = tr("Fill handle: drag to copy this cell, double-tap to fill to the end of the data") }
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { double() }) }
            .pointerInput(Unit) {
                var dx = 0f; var dy = 0f
                detectDragGestures(
                    onDragStart = { dx = 0f; dy = 0f; start() },
                    onDragEnd = { end() },
                    onDragCancel = { cancel() },
                ) { change, amount -> change.consume(); dx += amount.x; dy += amount.y; drag(dx, dy) }
            },
        contentAlignment = Alignment.BottomEnd,
    ) {
        // Inside the cell's corner, so the row's scroll doesn't clip it.
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(colors.primary).border(1.5.dp, colors.surface, RoundedCornerShape(2.dp)))
    }
}

/** Functions matching the name being typed, as chips above the keyboard, with how the first is written. */
@Composable
private fun FormulaSuggestions(names: List<String>, onPick: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().background(colors.surfaceContainerHigh).padding(top = 6.dp, bottom = 8.dp)) {
        com.example.cas.graph.Sheet.signature(names.first())?.let {
            Text("=$it", style = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp, color = colors.onSurfaceVariant), maxLines = 1, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            names.forEach { n ->
                Text(
                    n, style = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 14.sp, color = colors.onSecondaryContainer),
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(colors.secondaryContainer).clickable(onClickLabel = "Use $n") { onPick(n) }.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/**
 * Under a formula that ends in a reference: a button that anchors it with $ the next way
 * round (shown as what it will become), so a fill keeps that row or column fixed.
 */
@Composable
private fun AnchorBar(next: String, onAnchor: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ref = Regex("""\$?[A-Za-z]{1,3}\$?\d+$""").find(next)?.value ?: return
    Row(
        Modifier.fillMaxWidth().background(colors.surfaceContainerHigh).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "$ → $ref", style = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 14.sp, color = colors.onSecondaryContainer),
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(colors.secondaryContainer).clickable(onClickLabel = "Anchor the reference as $ref", onClick = onAnchor).padding(horizontal = 12.dp, vertical = 8.dp),
        )
        Text(tr("\$ keeps a column or row fixed when filling"), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

/**
 * A grip for resizing: a short bar that drags along [vertical] (a column's width) or across (a
 * row's height), and a double tap that goes back to the usual size.
 */
@Composable
private fun ResizeGrip(vertical: Boolean, description: String, onDrag: (Float) -> Unit, onReset: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val drag by androidx.compose.runtime.rememberUpdatedState(onDrag)
    val reset by androidx.compose.runtime.rememberUpdatedState(onReset)
    var active by remember { mutableStateOf(false) }
    Box(
        modifier
            .semantics { contentDescription = tr(description) }
            .pointerInput(vertical) {
                if (vertical) detectHorizontalDragGestures(onDragStart = { active = true }, onDragEnd = { active = false }, onDragCancel = { active = false }) { change, dx -> change.consume(); drag(dx) }
                else detectVerticalDragGestures(onDragStart = { active = true }, onDragEnd = { active = false }, onDragCancel = { active = false }) { change, dy -> change.consume(); drag(dy) }
            }
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { reset() }) },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .then(if (vertical) Modifier.width(if (active) 4.dp else 3.dp).height(24.dp) else Modifier.height(if (active) 4.dp else 3.dp).width(20.dp))
                .clip(CircleShape)
                .background(if (active) colors.primary else colors.outlineVariant),
        )
    }
}

/** The role's colors: x primary, y tertiary, the uncertainties secondary, unused neutral. */
@Composable
private fun roleColors(role: String?): Pair<Color, Color> {
    val colors = MaterialTheme.colorScheme
    return when (role) {
        "x" -> colors.primary to colors.onPrimary
        "y" -> colors.tertiary to colors.onTertiary
        "σx", "σy" -> colors.secondary to colors.onSecondary
        else -> colors.surfaceContainerHighest to colors.onSurfaceVariant
    }
}

/** A role as math: x, y, σ(x), σ(y), letters italic and brackets upright. */
private fun roleText(role: String) = androidx.compose.ui.text.buildAnnotatedString {
    (when (role) { "σx" -> "σ(x)"; "σy" -> "σ(y)"; else -> role }).forEach { ch ->
        pushStyle(androidx.compose.ui.text.SpanStyle(fontFamily = if (ch == '(' || ch == ')') CasFonts.CmRoman else CasFonts.CmItalic))
        append(ch); pop()
    }
}

/** The counts as pills: points, columns, and cells that aren't numbers (or that a y column is needed). */
@Composable
private fun TableCounts(points: Int, columns: Int, bad: Int, needY: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    @Composable
    fun pill(text: String, error: Boolean = false) = Text(
        text, style = MaterialTheme.typography.labelLarge,
        color = if (error) colors.onErrorContainer else colors.onSecondaryContainer,
        modifier = Modifier.clip(CircleShape).background(if (error) colors.errorContainer else colors.secondaryContainer).padding(horizontal = 12.dp, vertical = 6.dp),
    )
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (needY) pill("Pick a y column", error = true) else pill("$points point${if (points == 1) "" else "s"}")
        pill("$columns column${if (columns == 1) "" else "s"}")
        if (bad > 0) pill("$bad not number${if (bad == 1) "" else "s"}", error = true)
    }
}

/** The tablet's side pane: the counts, a live preview, and what the roles mean. */
@Composable
private fun TableSidePane(t: com.example.cas.graph.DataTable, points: Int, columns: Int, bad: Int, needY: Boolean, modifier: Modifier, inspector: @Composable () -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.verticalScroll(rememberScrollState()).padding(end = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TableCounts(points, columns, bad, needY)
        TablePreview(t, Modifier.fillMaxWidth().aspectRatio(1.2f))
        inspector()
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.surfaceContainer).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tr("Roles"), style = MaterialTheme.typography.titleSmall, color = colors.primary)
            listOf("x" to "across: without one, rows are numbered 1, 2, 3…", "y" to "up: needed for points", "σx" to "error bars across", "σy" to "error bars up").forEach { (r, what) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoleBadge(r)
                    Spacer(Modifier.width(10.dp))
                    Text(what, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
            Text(tr("Columns without a role are kept but not plotted. ＋ › Paste copies a table from a spreadsheet. Drag the grips to resize rows and columns."), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

/** The points as they'll be plotted (with their error bars), scaled to fit. */
@Composable
private fun TablePreview(t: com.example.cas.graph.DataTable, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val pts = remember(t) { t.points() }
    val (ex, ey) = remember(t) { t.errors() }
    Box(modifier.clip(RoundedCornerShape(24.dp)).background(colors.surfaceContainer), contentAlignment = Alignment.Center) {
        if (pts.isEmpty()) { Text(tr("No points yet"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant); return@Box }
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize().padding(18.dp)) {
            fun lo(v: Double, e: Double?) = v - (e?.takeIf { it.isFinite() } ?: 0.0)
            fun hi(v: Double, e: Double?) = v + (e?.takeIf { it.isFinite() } ?: 0.0)
            var x0 = pts.indices.minOf { lo(pts[it].first, ex?.get(it)) }; var x1 = pts.indices.maxOf { hi(pts[it].first, ex?.get(it)) }
            var y0 = pts.indices.minOf { lo(pts[it].second, ey?.get(it)) }; var y1 = pts.indices.maxOf { hi(pts[it].second, ey?.get(it)) }
            if (x1 - x0 < 1e-12) { x0 -= 1; x1 += 1 }
            if (y1 - y0 < 1e-12) { y0 -= 1; y1 += 1 }
            fun sx(x: Double) = ((x - x0) / (x1 - x0) * size.width).toFloat()
            fun sy(y: Double) = (size.height - (y - y0) / (y1 - y0) * size.height).toFloat()
            // The axes, where they're in view.
            if (0.0 in x0..x1) drawLine(colors.outlineVariant, Offset(sx(0.0), 0f), Offset(sx(0.0), size.height), 1.5f)
            if (0.0 in y0..y1) drawLine(colors.outlineVariant, Offset(0f, sy(0.0)), Offset(size.width, sy(0.0)), 1.5f)
            pts.forEachIndexed { k, (x, y) ->
                ey?.get(k)?.takeIf { it.isFinite() && it > 0 }?.let { e -> drawLine(colors.tertiary, Offset(sx(x), sy(y - e)), Offset(sx(x), sy(y + e)), 2f) }
                ex?.get(k)?.takeIf { it.isFinite() && it > 0 }?.let { e -> drawLine(colors.tertiary, Offset(sx(x - e), sy(y)), Offset(sx(x + e), sy(y)), 2f) }
            }
            pts.forEach { (x, y) -> drawCircle(colors.primary, 4.dp.toPx(), Offset(sx(x), sy(y))) }
        }
    }
}

/** A column's role as a small colored shape with its letter. */
@Composable
private fun RoleBadge(role: String?, modifier: Modifier = Modifier) {
    val (bg, fg) = roleColors(role)
    Box(
        modifier.height(30.dp).widthIn(min = 30.dp).clip(if (role == "x" || role == "y") CircleShape else RoundedCornerShape(9.dp)).background(bg).padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (role == null) Text(tr("–"), style = MaterialTheme.typography.labelLarge, color = fg)
        else Text(roleText(role), color = fg, style = TextStyle(fontFamily = CasFonts.CmItalic, fontSize = 17.sp))
    }
}

/**
 * A column's card: its role badge, its name (typed in place) and a menu with the roles and
 * what can be done with the column. Tinted with the role's color.
 */
@Composable
private fun ColumnCard(
    role: String?, name: String, index: Int, width: androidx.compose.ui.unit.Dp,
    onName: (String) -> Unit, onRole: (String?) -> Unit, onSort: () -> Unit, onFill: () -> Unit, onClear: () -> Unit, onRemove: (() -> Unit)?,
    letter: String? = null, onFillDown: (() -> Unit)? = null,
    onSortDown: () -> Unit = {}, onSeries: () -> Unit = {}, onStats: () -> Unit = {}, onDuplicate: () -> Unit = {},
    onMoveLeft: (() -> Unit)? = null, onMoveRight: (() -> Unit)? = null,
    format: com.example.cas.graph.ColumnFormat = com.example.cas.graph.ColumnFormat(), onFormat: () -> Unit = {}, onColorScale: (Boolean) -> Unit = {},
    filtered: Boolean = false, onFilter: () -> Unit = {},
    /** Opens the column's sheet instead of its menu. */
    onOpen: (() -> Unit)? = null,
    /** The column the toolbar works on: outlined. */
    chosen: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    val tint = roleColors(role).first
    Box {
        Column(
            Modifier.width(width).clip(RoundedCornerShape(18.dp)).background(if (role == null) colors.surfaceContainer else tint.copy(alpha = 0.16f))
                .border(if (chosen) 2.dp else 0.dp, if (chosen) colors.primary else Color.Transparent, RoundedCornerShape(18.dp))
                .clickable(onClickLabel = "Column ${index + 1}: its role and more") { if (onOpen != null) onOpen() else open = true }
                .padding(start = 8.dp, end = 2.dp, top = 8.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoleBadge(role)
                // The column's letter, for formulas.
                if (letter != null) Text(letter, style = MaterialTheme.typography.titleSmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
                Spacer(Modifier.weight(1f))
                // A filter in use on this column.
                if (filtered) Icon(Icons.Default.FilterList, contentDescription = tr("Filtered"), tint = colors.primary, modifier = Modifier.size(18.dp))
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            androidx.compose.foundation.text.BasicTextField(
                value = name, onValueChange = onName, singleLine = true,
                textStyle = TextStyle(fontFamily = CasFonts.Ui, fontSize = 13.sp, color = colors.onSurface),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primary),
                decorationBox = { inner -> Box { if (name.isEmpty()) Text("Column ${index + 1}", style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 13.sp, color = colors.outline)); inner() } },
                modifier = Modifier.fillMaxWidth().padding(end = 6.dp, bottom = 2.dp),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = RoundedCornerShape(16.dp)) {
            listOf("x" to "across", "y" to "up", "σx" to "error bars across", "σy" to "error bars up", null to "Not used").forEach { (r, what) ->
                DropdownMenuItem(
                    leadingIcon = { RoleBadge(r) },
                    text = { Text(if (r == null) what else what.replaceFirstChar { it.uppercase() }) },
                    trailingIcon = if (r == role) ({ Icon(Icons.Default.Check, contentDescription = tr("Chosen")) }) else null,
                    onClick = { open = false; onRole(r) },
                )
            }
            androidx.compose.material3.HorizontalDivider()
            DropdownMenuItem(leadingIcon = { Icon(Icons.Default.Calculate, null) }, text = { Text(tr("Statistics")) }, onClick = { open = false; onStats() })
            DropdownMenuItem(leadingIcon = { Icon(Icons.Default.FilterList, null) }, text = { Text(tr(if (filtered) "Change the filter…" else "Filter…")) }, onClick = { open = false; onFilter() })
            DropdownMenuItem(leadingIcon = { Icon(Icons.Default.Percent, null) }, text = { Text(tr("Number format…")) }, onClick = { open = false; onFormat() })
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.Palette, null) }, text = { Text(tr("Color scale")) },
                trailingIcon = if (format.colorScale) ({ Icon(Icons.Default.Check, contentDescription = tr("On")) }) else null,
                onClick = { open = false; onColorScale(!format.colorScale) },
            )
            DropdownMenuItem(leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null) }, text = { Text(tr("Sort smallest first")) }, onClick = { open = false; onSort() })
            DropdownMenuItem(leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null, modifier = Modifier.graphicsLayer(scaleY = -1f)) }, text = { Text(tr("Sort largest first")) }, onClick = { open = false; onSortDown() })
            DropdownMenuItem(leadingIcon = { Icon(Icons.Default.FormatListNumbered, null) }, text = { Text(tr("Fill with 1, 2, 3…")) }, onClick = { open = false; onFill() })
            DropdownMenuItem(leadingIcon = { Icon(Icons.Default.Timeline, null) }, text = { Text(tr("Fill with a series…")) }, onClick = { open = false; onSeries() })
            if (onFillDown != null) DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) }, text = { Text(tr("Fill the formula down")) }, onClick = { open = false; onFillDown() },
            )
            androidx.compose.material3.HorizontalDivider()
            DropdownMenuItem(leadingIcon = { Icon(Icons.Default.ContentCopy, null) }, text = { Text(tr("Duplicate the column")) }, onClick = { open = false; onDuplicate() })
            if (onMoveLeft != null) DropdownMenuItem(leadingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) }, text = { Text(tr("Move left")) }, onClick = { open = false; onMoveLeft() })
            if (onMoveRight != null) DropdownMenuItem(leadingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }, text = { Text(tr("Move right")) }, onClick = { open = false; onMoveRight() })
            DropdownMenuItem(leadingIcon = { Icon(Icons.Default.CleaningServices, null) }, text = { Text(tr("Clear the column")) }, onClick = { open = false; onClear() })
            if (onRemove != null) DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = colors.error) },
                text = { Text(tr("Remove the column"), color = colors.error) }, onClick = { open = false; onRemove() },
            )
        }
    }
}

/** A row's number as a pill; tap it to insert a row above or below, or remove this one. */
@Composable
private fun RowNumber(r: Int, onInsertAbove: () -> Unit, onInsertBelow: () -> Unit, onRemove: (() -> Unit)?, modifier: Modifier = Modifier, onDuplicate: () -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier.height(32.dp).widthIn(min = 36.dp).clip(CircleShape).clickable(onClickLabel = "Row ${r + 1}: insert or remove") { open = true }.padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) { Text("${r + 1}", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = RoundedCornerShape(16.dp)) {
            DropdownMenuItem(leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, null) }, text = { Text(tr("Insert a row above")) }, onClick = { open = false; onInsertAbove() })
            DropdownMenuItem(leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) }, text = { Text(tr("Insert a row below")) }, onClick = { open = false; onInsertBelow() })
            DropdownMenuItem(leadingIcon = { Icon(Icons.Default.ContentCopy, null) }, text = { Text("Duplicate row ${r + 1}") }, onClick = { open = false; onDuplicate() })
            if (onRemove != null) DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = colors.error) },
                text = { Text("Remove row ${r + 1}", color = colors.error) }, onClick = { open = false; onRemove() },
            )
        }
    }
}

/**
 * One cell, tinted with its column's role: plain text (cheap, so thousands of rows scroll
 * smoothly) until it's tapped, then a field being typed in, outlined in the primary color.
 * Cells that aren't numbers get a red edge; a formula shows its value with a small ƒx until
 * it's tapped. Next on the keyboard moves down to the next row.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TableCell(
    text: String, error: Boolean, modifier: Modifier, role: String? = null,
    shown: String? = null, formulas: Boolean = false, editing: Boolean = false,
    highlight: Color? = null, referenceColors: List<Pair<IntRange, Color>> = emptyList(),
    /** A number as its column's format shows it; a color scale's shade; part of a picked range. */
    display: String? = null, fill: Color? = null, selected: Boolean = false,
    /** A data bar's length, 0 to 1, drawn behind the text. */
    bar: Float? = null,
    onTap: () -> Unit = {}, onLongPress: (() -> Unit)? = null, onNext: () -> Unit = {}, onChange: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val tint = roleColors(role).first
    val formula = shown != null
    val shape = RoundedCornerShape(if (editing) 14.dp else 10.dp)
    val ink = if (role == null) colors.onSurfaceVariant else colors.onSurface
    val base = modifier
        .clip(shape)
        .background(
            if (formula && !editing) colors.tertiaryContainer.copy(alpha = 0.35f).compositeOver(colors.surfaceContainerHigh)
            else if (role == null) colors.surfaceContainerHigh else tint.copy(alpha = 0.08f).compositeOver(colors.surfaceContainerHighest),
        )
        .then(if (fill != null && !editing) Modifier.background(fill.copy(alpha = 0.55f)) else Modifier)
        .then(if (bar != null && !editing) {
            val barColor = colors.primary.copy(alpha = 0.28f)
            Modifier.drawBehind {
                val inset = 4.dp.toPx()
                drawRoundRect(barColor, topLeft = Offset(inset, inset), size = androidx.compose.ui.geometry.Size(((size.width - 2 * inset) * bar.coerceIn(0f, 1f)).coerceAtLeast(0f), size.height - 2 * inset), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
            }
        } else Modifier)
        .then(if (highlight != null && !editing) Modifier.background(highlight.copy(alpha = 0.10f)) else Modifier)
        .then(if (selected && !editing) Modifier.background(colors.primary.copy(alpha = 0.18f)) else Modifier)
        .border(
            if (editing || error || highlight != null || selected) 2.dp else 0.dp,
            when { editing || selected -> colors.primary; highlight != null -> highlight; error -> colors.error; else -> Color.Transparent },
            shape,
        )
    if (!editing) {
        Row(
            base.combinedClickable(onClickLabel = tr("Edit this cell"), onLongClickLabel = tr("Select cells from here"), onLongClick = onLongPress) { onTap() }.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                shown ?: display ?: text, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
                style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 17.sp, color = if (formula && error) colors.error else ink),
                modifier = Modifier.weight(1f),
            )
            if (formula) Text(tr("ƒx"), style = TextStyle(fontFamily = CasFonts.CmItalic, fontSize = 12.sp, color = colors.tertiary))
        }
        return
    }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    var value by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(text, androidx.compose.ui.text.TextRange(text.length))) }
    androidx.compose.runtime.LaunchedEffect(Unit) { focus.requestFocus() }
    // Changed from outside (a suggestion picked): the field follows, cursor at the end.
    androidx.compose.runtime.LaunchedEffect(text) { if (text != value.text) value = androidx.compose.ui.text.input.TextFieldValue(text, androidx.compose.ui.text.TextRange(text.length)) }
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = { value = it; if (it.text != text) onChange(it.text) },
        singleLine = true,
        textStyle = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 17.sp, color = ink),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primary),
        // With formulas, a keyboard with letters, = and brackets.
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            // The whole keyboard (letters too: words, units, a formula's =), not just a number pad.
            keyboardType = if (formulas) androidx.compose.ui.text.input.KeyboardType.Ascii else androidx.compose.ui.text.input.KeyboardType.Text,
            capitalization = if (formulas) androidx.compose.ui.text.input.KeyboardCapitalization.Characters else androidx.compose.ui.text.input.KeyboardCapitalization.None,
            imeAction = androidx.compose.ui.text.input.ImeAction.Next,
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { onNext() }),
        // Each reference in the formula in the color its cells are outlined in.
        visualTransformation = if (referenceColors.isEmpty()) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.VisualTransformation { t ->
            val styled = androidx.compose.ui.text.buildAnnotatedString {
                append(t.text)
                referenceColors.forEach { (at, color) ->
                    if (at.last < t.text.length) addStyle(androidx.compose.ui.text.SpanStyle(color = color, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), at.first, at.last + 1)
                }
            }
            androidx.compose.ui.text.input.TransformedText(styled, androidx.compose.ui.text.input.OffsetMapping.Identity)
        },
        decorationBox = { inner -> Box(contentAlignment = Alignment.CenterStart) { inner() } },
        modifier = base.focusRequester(focus).padding(horizontal = 12.dp),
    )
}


/**
 * The wide layout: on landscape tablets and unfolded foldables the keyboard sits beside the
 * math instead of under it (Settings chooses which side), and it's always shown.
 */
@Composable
fun isTabletLayout(): Boolean {
    val c = androidx.compose.ui.platform.LocalConfiguration.current
    return c.screenWidthDp >= 840 && c.smallestScreenWidthDp >= 600
}

/**
 * The divider between the list of lines and the graph on a tablet: a pill to drag sideways
 * (the list grows, the graph shrinks, within limits); double-tap puts it back.
 */
@Composable
private fun PaneHandle(onDrag: (Float) -> Unit, onDone: () -> Unit, onReset: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    var dragging by remember { mutableStateOf(false) }
    Box(
        Modifier
            .width(16.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false; onDone() },
                    onDragCancel = { dragging = false; onDone() },
                ) { change, dx -> change.consume(); onDrag(with(density) { dx.toDp().value }) }
            }
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { onReset() }) }
            .semantics { contentDescription = tr("Resize the list of lines: drag sideways, double-tap to reset") },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.width(if (dragging) 6.dp else 4.dp).height(if (dragging) 64.dp else 48.dp).clip(CircleShape).background(if (dragging) colors.primary else colors.outline))
    }
}

/** The keyboard column's width in the wide layout: about a third of the screen, 300–440 dp. */
@Composable
fun tabletKeypadWidth(): androidx.compose.ui.unit.Dp =
    (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp * 0.32f).dp.coerceIn(300.dp, 440.dp)

/**
 * A graph screen: the plot, its list of lines and the keyboard. On a phone they're stacked, and
 * the keyboard is measured first so it never gets squashed (the list gives way, then the plot).
 * On a tablet they're side by side: keyboard, lines, plot (mirrored with the keyboard on the right).
 */
@Composable
fun GraphScaffold(vm: GraphViewModel, outputLabel: String, modifier: Modifier = Modifier, canvas: @Composable () -> Unit) {
    if (isTabletLayout()) {
        val left = AppSettings.keypadSide == 0
        val colors = MaterialTheme.colorScheme
        val keypad = @Composable {
            Keypad(vm, Modifier.width(tabletKeypadWidth()).fillMaxHeight().background(colors.surfaceContainerLow), tablet = true)
        }
        // The list's width, dragged at its edge: at least 240 dp, and the graph keeps 320 dp.
        val screen = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
        val maxList = (screen - tabletKeypadWidth().value.toInt() - 320 - 16).coerceAtLeast(240)
        var listWidth by remember { mutableStateOf(AppSettings.graphListWidth.coerceIn(240, maxList)) }
        val list = @Composable {
            FunctionList(vm, outputLabel, Modifier.width(listWidth.dp).fillMaxHeight(), tablet = true)
        }
        val handle = @Composable {
            PaneHandle(
                onDrag = { dx -> listWidth = (listWidth + (if (left) dx else -dx)).roundToInt().coerceIn(240, maxList) },
                onDone = { AppSettings.changeGraphListWidth(listWidth) },
                onReset = { listWidth = 320.coerceAtMost(maxList); AppSettings.changeGraphListWidth(listWidth) },
            )
        }
        Row(modifier.fillMaxSize()) {
            if (left) { keypad(); list(); handle(); Box(Modifier.weight(1f).fillMaxHeight()) { canvas() } }
            else { Box(Modifier.weight(1f).fillMaxHeight()) { canvas() }; handle(); list(); keypad() }
        }
        return
    }
    // Construct mode on a phone: the tools take the list's place (it comes back when closed).
    val listAway = vm.constructing && AppSettings.geometry
    // Typing something new: the graph (and its buttons) step aside, and the list has the room.
    val focus = vm.typingFocus
    // A note's keyboard closing (Back, or Done) ends it too.
    val imeVisible = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
    var imeSeen by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(imeVisible, focus) {
        if (!focus) imeSeen = false
        else if (imeVisible) imeSeen = true
        else if (imeSeen && vm.active == null) vm.typingFocus = false
    }
    // Editing a line gets the room too (the keyboard's handle, or Enter, brings the graph back).
    androidx.compose.runtime.LaunchedEffect(vm.active, vm.keypadHidden) { if (vm.active != null && !vm.keypadHidden) vm.typingFocus = true }
    // The graph's last height, kept while it's away so it doesn't redraw at another size.
    val lastPlot = remember { intArrayOf(0) }
    androidx.compose.ui.layout.Layout(
        contents = listOf(
            { Box(Modifier.fillMaxSize()) { canvas() } },
            { if (!listAway) FunctionList(vm, outputLabel) },
            {
                androidx.compose.animation.AnimatedVisibility(
                    visible = vm.active != null && !vm.keypadHidden,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut(),
                ) { Keypad(vm) }
            },
        ),
        modifier = modifier.fillMaxSize(),
    ) { (plot, lines, keys), c ->
        val w = c.maxWidth; val h = c.maxHeight
        // The keyboard first, at its full height; then the lines; the plot keeps at least 120 dp.
        val kp = keys.map { it.measure(androidx.compose.ui.unit.Constraints(minWidth = w, maxWidth = w, maxHeight = h)) }
        val kh = kp.sumOf { it.height }
        val all = (h - kh).coerceAtLeast(0)
        val room = if (focus) all else (all - 120.dp.roundToPx()).coerceAtLeast(0)
        val lp = lines.map { it.measure(androidx.compose.ui.unit.Constraints(minWidth = w, maxWidth = w, minHeight = if (focus) room else 0, maxHeight = room)) }
        val lh = lp.sumOf { it.height }
        val ph = if (focus) 0 else (h - kh - lh).coerceAtLeast(0)
        if (ph > 0) lastPlot[0] = ph
        // Away, the graph keeps its last size but isn't placed (not drawn, not touched).
        val pp = plot.map { it.measure(androidx.compose.ui.unit.Constraints.fixed(w, if (focus) lastPlot[0].coerceAtLeast(1) else ph)) }
        layout(w, h) {
            var y = 0
            if (!focus) pp.forEach { it.place(0, y) }
            y += ph
            lp.forEach { it.place(0, y); y += it.height }
            kp.forEach { it.place(0, y); y += it.height }
        }
    }
}

/**
 * Touch and hold, then drag: [onTrace] gets the finger's position from the moment the hold is
 * recognized until it lifts, so a value can be read continuously without tapping again. A drag
 * that starts straight away is left to the graph (panning, turning).
 */
fun Modifier.holdToTrace(key: Any?, onTrace: (Offset) -> Unit): Modifier = pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (OverlayTouch.owns(down)) return@awaitEachGesture
        val held = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
        held.consume()
        onTrace(held.position)
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) { change.consume(); break }
            change.consume()
            onTrace(change.position)
        }
    }
}


/**
 * Keeps touches on a card or rail floating over the graph from reaching the graph. Only the
 * touch's first contact is marked (its id noted, the down consumed), after the card's own
 * buttons and scrolling have seen it; the graph's gestures then leave that touch alone. Moves
 * aren't consumed, so the rail's scrolling isn't cancelled by it.
 */
internal fun Modifier.blockGraphTouches(): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        OverlayTouch.id = down.id
        down.consume()
    }
}

/** The touch that began on a card over the graph, if the latest one did. */
internal object OverlayTouch {
    @Volatile var id: androidx.compose.ui.input.pointer.PointerId? = null
    fun owns(down: androidx.compose.ui.input.pointer.PointerInputChange) = down.id == id
}

/** A row of a point card: its letter (math), the value, a smaller line under it, and what Use does (none: no button). */
class CardValue(val letter: String, val text: String, val detail: String? = null, val onUse: (() -> Unit)? = null)

/** A tonal button along the bottom of a point card. */
class CardAction(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val spoken: String, val onClick: () -> Unit)

/**
 * The card for a point on a graph, floating over it at ([px], [py]) with a tail pointing there
 * (above the point, or below it when there's no room): the line's color and name with the kind
 * of point (maximum, zero…), each value large with a button to use it in the calculator, and
 * any actions (the area from here) as tonal buttons. Fills its parent, which should be the plot.
 */
@Composable
fun PointCardAt(px: Float, py: Float, color: Color?, kind: String?, name: String?, rows: List<CardValue>, actions: List<CardAction> = emptyList(), onClose: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val cardColor = colors.surfaceContainerHigh
    val below = py < with(androidx.compose.ui.platform.LocalDensity.current) { (90 + 46 * rows.size + if (actions.isEmpty()) 0 else 56).dp.toPx() }
    androidx.compose.ui.layout.Layout(content = {
        PointCard(color, kind, name, rows, actions, onClose)
        // The tail pointing at the point.
        androidx.compose.foundation.Canvas(Modifier.size(20.dp, 10.dp)) {
            val w = this.size.width; val h = this.size.height
            val tip = if (below) 0f else h
            val base = if (below) h + 1f else -1f
            drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(0f, base); lineTo(w / 2, tip); lineTo(w, base); close() }, cardColor)
        }
    }, modifier = Modifier.fillMaxSize()) { ms, c ->
        val p = ms[0].measure(androidx.compose.ui.unit.Constraints(maxWidth = c.maxWidth))
        val tail = ms[1].measure(androidx.compose.ui.unit.Constraints())
        layout(c.maxWidth, c.maxHeight) {
            val gap = 14.dp.roundToPx()
            val x = pinInside((px - p.width / 2f).toInt(), 8, c.maxWidth - p.width - 8)
            val y = if (!below) (py - p.height - gap).toInt() else (py + gap).toInt()
            p.place(x, y)
            val tx = pinInside((px - tail.width / 2f).toInt(), x + 20, x + p.width - 20 - tail.width)
            tail.place(tx, if (!below) y + p.height else y - tail.height)
        }
    }
}

@Composable
private fun PointCard(color: Color?, kind: String?, name: String?, rows: List<CardValue>, actions: List<CardAction>, onClose: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tap = rememberKeyTap()
    // As wide as the screen allows (a phone's is narrow): long values get a smaller size, then a
    // second line, rather than being cut off.
    val cardMax = minOf(340, androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp - 16).dp
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val valueStyle = TextStyle(fontFamily = CasFonts.Ui, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, fontFeatureSettings = "tnum")
    // Room for a value: the card less its padding, the letter, the gap and the Use button.
    val room = with(density) { (cardMax - 28.dp - 32.dp - 12.dp - (if (rows.any { it.onUse != null }) 86.dp else 0.dp)).toPx() }
    fun fitted(text: String): androidx.compose.ui.unit.TextUnit {
        var size = 21f
        while (size > 14f && measurer.measure(text, valueStyle.copy(fontSize = size.sp), maxLines = 1, softWrap = false).size.width > room) size -= 1f
        return size.sp
    }
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceContainerHigh,
        shadowElevation = 6.dp,
        // Touches on the card (scrolling its buttons) stay off the graph under it.
        modifier = Modifier.widthIn(min = minOf(216.dp, cardMax), max = cardMax).blockGraphTouches(),
    ) {
        Column(Modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Max).padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 12.dp)) {
            // The line's color, the kind of point and its name, and × to close the card.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (color != null) Box(Modifier.size(12.dp).clip(CircleShape).background(color))
                if (kind != null) Text(
                    tr(kind),
                    color = colors.onTertiaryContainer,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    modifier = Modifier.clip(CircleShape).background(colors.tertiaryContainer).padding(horizontal = 10.dp, vertical = 3.dp),
                )
                Spacer(Modifier.weight(1f))
                if (name != null) {
                    val row = remember(name) { com.example.cas.graph.Legend.row(name) }
                    Box(Modifier.widthIn(max = 150.dp).horizontalScroll(rememberScrollState())) { MathView(row, 15.sp, colors.onSurfaceVariant) }
                }
                Box(
                    Modifier.size(32.dp).clip(CircleShape).clickable(onClickLabel = tr("Close")) { tap(); onClose() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = tr("Close"), tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
            rows.forEach { v ->
                Row(Modifier.heightIn(min = 42.dp), verticalAlignment = Alignment.CenterVertically) {
                    val letter = remember(v.letter) { com.example.cas.graph.Legend.row("$" + v.letter + "$") }
                    Box(Modifier.widthIn(min = 22.dp), contentAlignment = Alignment.Center) { MathView(letter, 20.sp, colors.onSurfaceVariant) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        MathText(
                            Readout.markdown(v.text),
                            color = colors.onSurface,
                            style = valueStyle.copy(fontSize = remember(v.text, room) { fitted(v.text) }),
                        )
                        if (v.detail != null) MathText(Readout.markdown(v.detail), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    if (v.onUse != null) Row(
                        Modifier.height(34.dp).clip(CircleShape).background(colors.primaryContainer)
                            .clickable(onClickLabel = "Use ${v.letter} in the calculator") { tap(); v.onUse.invoke() }
                            .padding(start = 10.dp, end = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardReturn, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(tr("Use"), color = colors.onPrimaryContainer, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            if (actions.isNotEmpty()) {
                androidx.compose.material3.HorizontalDivider(Modifier.padding(top = 8.dp, bottom = 10.dp), color = colors.outlineVariant)
                // More buttons than fit (area, between curves, tangent, normal, arc length…) scroll sideways.
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    actions.forEach { a ->
                        Row(
                            Modifier.height(38.dp).clip(RoundedCornerShape(12.dp)).background(colors.secondaryContainer)
                                .clickable(onClickLabel = tr(a.spoken)) { tap(); a.onClick() }
                                .padding(start = 10.dp, end = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(a.icon, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(tr(a.label), color = colors.onSecondaryContainer, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}


/**
 * The result of an integral picked on a graph (an area, the area between two curves, ∮ around
 * a loop): its icon and title, the integral as math, the value large, smaller figures under it,
 * and buttons to use or copy the value. Top left of the plot, under the legend.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    math: com.example.cas.editor.MathRow?,
    value: String,
    stats: List<Pair<String, String>> = emptyList(),
    note: String? = null,
    copyText: String? = null,
    onUse: (() -> Unit)? = null,
    onClose: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val tap = rememberKeyTap()
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    // On a phone: one header line with the actions as icons, the integral, then the value with its
    // figures beside it. On a tablet: roomier, with labeled buttons and the note.
    if (!isTabletLayout()) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceContainerHigh,
            shadowElevation = 4.dp,
            modifier = Modifier.widthIn(min = 200.dp, max = 280.dp),
        ) {
            Column(Modifier.padding(start = 12.dp, end = 2.dp, top = 2.dp, bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tr(title), style = MaterialTheme.typography.labelLarge, color = colors.onSurface, modifier = Modifier.weight(1f), maxLines = 1)
                    @Composable
                    fun action(vector: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) =
                        IconButton(onClick = { tap(); onClick() }, modifier = Modifier.size(36.dp)) { Icon(vector, contentDescription = label, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
                    if (copyText != null) action(Icons.Default.ContentCopy, "Copy the value") { clipboard.setText(androidx.compose.ui.text.AnnotatedString(copyText)) }
                    if (onUse != null) action(androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardReturn, "Use the value in the calculator", onUse)
                    action(Icons.Default.Close, "Close", onClose)
                }
                Column(Modifier.padding(end = 10.dp)) {
                    if (math != null) Box(Modifier.horizontalScroll(rememberScrollState())) { MathView(math, 14.sp, colors.onSurfaceVariant, computerModern = true) }
                    Row(verticalAlignment = Alignment.Bottom) {
                        MathText(
                            Readout.markdown(value),
                            color = colors.onSurface,
                            style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, fontFeatureSettings = "tnum"),
                        )
                    }
                    if (stats.isNotEmpty()) MathText(
                        stats.joinToString("  ·  ") { (label, v) -> "$label ${Readout.markdown(v)}" },
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                    )
                    else if (note != null && value == "—") Text(note, style = MaterialTheme.typography.bodySmall, color = colors.error)
                }
            }
        }
        return
    }
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceContainerHigh,
        shadowElevation = 4.dp,
        modifier = Modifier.widthIn(min = 240.dp, max = 320.dp),
    ) {
        Column(Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(tr(title), style = MaterialTheme.typography.titleSmall, color = colors.onSurface, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = tr("Close"), tint = colors.onSurfaceVariant) }
            }
            Column(Modifier.padding(end = 8.dp)) {
                if (math != null) Box(Modifier.padding(top = 6.dp).horizontalScroll(rememberScrollState())) { MathView(math, 17.sp, colors.onSurfaceVariant, computerModern = true) }
                MathText(
                    Readout.markdown(value),
                    color = colors.onSurface,
                    style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 30.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, fontFeatureSettings = "tnum"),
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (stats.isNotEmpty()) androidx.compose.foundation.layout.FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.forEach { (label, v) ->
                        Column(Modifier.clip(RoundedCornerShape(12.dp)).background(colors.surfaceContainerHighest).padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(tr(label), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                            MathText(Readout.markdown(v), style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, fontFeatureSettings = "tnum"), color = colors.onSurface)
                        }
                    }
                }
                if (note != null) Text(note, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                if (onUse != null || copyText != null) Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onUse != null) Row(
                        Modifier.height(36.dp).clip(CircleShape).background(colors.primaryContainer)
                            .clickable(onClickLabel = tr("Use the value in the calculator")) { tap(); onUse() }
                            .padding(start = 12.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardReturn, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Use"), color = colors.onPrimaryContainer, style = MaterialTheme.typography.labelLarge)
                    }
                    if (copyText != null) Row(
                        Modifier.height(36.dp).clip(CircleShape).border(1.dp, colors.outlineVariant, CircleShape)
                            .clickable(onClickLabel = tr("Copy the value")) { tap(); clipboard.setText(androidx.compose.ui.text.AnnotatedString(copyText)) }
                            .padding(start = 12.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Copy"), color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

/**
 * A graph settings dialog's limits and axis scales: the limits are typed as values (0.01 to 1000
 * on a log axis, not −2 to 3). Until a limit is typed, switching an axis to log or back shows
 * where the view will go in the new scale.
 */
internal class RangeFields(private val base: com.example.cas.graph.Viewport, private val baseScale: com.example.cas.graph.AxisScale) {
    var logX by mutableStateOf(baseScale.logX)
        private set
    var logY by mutableStateOf(baseScale.logY)
        private set
    private var edited by mutableStateOf(false)
    val fields = androidx.compose.runtime.mutableStateListOf<String>().apply { addAll(shown()) }

    private fun text(v: Double) = shortNumber(v).replace("−", "-")
    private fun num(t: String) = t.trim().replace("−", "-").replace(",", ".").toDoubleOrNull()?.takeIf { it.isFinite() }

    private fun shown(): List<String> {
        val sc = com.example.cas.graph.AxisScale(logX, logY)
        val v = sc.realView(com.example.cas.graph.AxisScale.convert(base, baseScale, sc))
        return listOf(v.xMin, v.xMax, v.yMin, v.yMax).map { text(it) }
    }

    fun setLog(x: Boolean, y: Boolean) {
        logX = x; logY = y
        if (!edited) shown().forEachIndexed { k, t -> fields[k] = t }
    }

    fun edit(k: Int, t: String) { fields[k] = t; edited = true }

    private val numbers get() = fields.map { num(it) }

    /** Each "from" below its "to", and above 0 on a log axis. */
    val valid: Boolean get() {
        val n = numbers
        if (n.any { it == null }) return false
        return n[0]!! < n[1]!! && n[2]!! < n[3]!! && (!logX || n[0]!! > 0) && (!logY || n[2]!! > 0)
    }

    /** Sets the scales (which moves the view to match) and then the typed limits, if any were typed. */
    fun apply(vm: GraphViewModel, setView: (com.example.cas.graph.Viewport) -> Unit) {
        vm.setLogAxes(logX, logY)
        if (!edited) return
        val n = numbers.map { it!! }
        val sc = com.example.cas.graph.AxisScale(logX, logY)
        setView(com.example.cas.graph.Viewport(sc.x(n[0]), sc.x(n[1]), sc.y(n[2]), sc.y(n[3])))
    }
}

/** The limits typed exactly, then Linear | Log for each axis, as Material 3 segmented buttons. */
@Composable
internal fun RangeAndScaleSettings(state: RangeFields, xName: String, yName: String) {
    val colors = MaterialTheme.colorScheme
    listOf(0 to xName, 2 to yName).forEach { (k, name) ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(state.fields[k], { state.edit(k, it) }, singleLine = true, label = { Text("$name from") }, modifier = Modifier.weight(1f))
            OutlinedTextField(state.fields[k + 1], { state.edit(k + 1, it) }, singleLine = true, label = { Text("$name to") }, modifier = Modifier.weight(1f))
        }
    }
    if (!state.valid) Text(tr("Each “from” must be a number below its “to”") + if (state.logX || state.logY) ", and above 0 on a log axis." else ".",
        color = colors.error, style = MaterialTheme.typography.bodySmall,
    )
    Text(tr("Axis scale"), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    listOf(xName to state.logX, yName to state.logY).forEachIndexed { k, (name, log) ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(64.dp))
            SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                listOf(false to "Linear", true to "Log").forEachIndexed { i, (value, label) ->
                    SegmentedButton(icon = {}, 
                        selected = log == value,
                        onClick = { if (k == 0) state.setLog(value, state.logY) else state.setLog(state.logX, value) },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        label = { Text(tr(label)) },
                    )
                }
            }
        }
    }
}

/** A fitted value with its standard error, both rounded to the error's two significant figures (1.991 ± 0.039). */
internal fun withError(v: Double, e: Double?): String {
    if (e == null || !e.isFinite() || e <= 0) return shortNumber(v)
    val place = kotlin.math.floor(kotlin.math.log10(e)).toInt() - 1
    fun r(x: Double) = java.math.BigDecimal(x).setScale(-place, java.math.RoundingMode.HALF_EVEN).let { if (place < 0) it.toPlainString() else it.toBigInteger().toString() }
    return if (place in -8..6) "${r(v)} ± ${r(e)}".replace("-", "−") else shortNumber(v) + " ± " + shortNumber(e)
}

/**
 * A fit's statistics, from holding Fit: each parameter with its standard error, R², RMSE, the
 * reduced χ² (χ²/ν) and ν, then the curve over the points and the residuals, drawn with the
 * data's own point size. Weighted by σ(y) when every point has one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FitStatsDialog(vm: GraphViewModel, f: PlotFunction, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val stats = remember(f, f.version) { runCatching { vm.fitStats(f) }.getOrNull() }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        androidx.compose.material3.Surface(shape = RoundedCornerShape(28.dp), color = colors.surfaceContainerHigh, modifier = Modifier.padding(16.dp).widthIn(max = 600.dp).fillMaxWidth()) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column {
                    Text(tr("Fit statistics"), style = MaterialTheme.typography.headlineSmall)
                    if (stats != null) Text(
                        "${stats.xs.size} points" + if (stats.sigmas != null) " · weighted by σ(y)" else "",
                        style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
                    )
                }
                Box(Modifier.horizontalScroll(rememberScrollState())) { MathView(f.editor.root, 20.sp, colors.onSurfaceVariant) }
                if (stats == null) {
                    Text(tr("This line can't be fitted to the points (too few points, or values it can't take)."), color = colors.error, style = MaterialTheme.typography.bodyMedium)
                } else {
                    // Each parameter ± its standard error.
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.primaryContainer).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        stats.names.forEachIndexed { k, name ->
                            // A built symbol (x̂₁) shows as its LaTeX, not its stored form.
                            val n = com.example.cas.cas.CustomSymbol.decode(name)?.latex ?: name
                            MathText("\$$n = ${Readout.latex(withError(stats.result.parameters[k], stats.result.errors?.get(k)))}\$", style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 18.sp), color = colors.onPrimaryContainer, mathScale = 1f)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        @Composable fun stat(math: String, v: String) = Row(
                            Modifier.clip(RoundedCornerShape(12.dp)).background(colors.secondaryContainer).padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) { MathText("\$$math = $v\$", style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 15.sp), color = colors.onSecondaryContainer, mathScale = 1f) }
                        if (stats.result.rSquared.isFinite()) stat("R^{2}", Readout.latex(java.math.BigDecimal(stats.result.rSquared).round(java.math.MathContext(5)).toPlainString()))
                        stat("\\mathrm{RMSE}", Readout.latex(shortNumber(stats.result.rmse)))
                        stats.reducedChiSquared?.let { stat("\\chi^{2}/\\nu", Readout.latex(shortNumber(it))) }
                        stat("\\nu", stats.result.dof.toString())
                    }
                    if (stats.sigmas == null) Text(tr("No σ(y) on the points, so χ²/ν counts each as σ = 1: it's the residuals' mean square. Give the data a σ(y) column for a true reduced χ²."),
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                    )
                    FitPlot(stats.xs, stats.ys, stats.sigmas, stats.curve, Modifier.fillMaxWidth().height(240.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(tr("Close")) }
                    Spacer(Modifier.width(8.dp))
                    Button(enabled = stats != null, onClick = { vm.fit(f); onDismiss() }) { Text(tr("Apply the fit")) }
                }
            }
        }
    }
}

/** The size of the points in the fit's plots, small so they don't hide the curve. */
private const val FIT_POINT_SIZE = 1.7f

/** The points (with their σ(y) bars) and the fitted curve, and under them the residuals about 0. */
@Composable
private fun FitPlot(xs: DoubleArray, ys: DoubleArray, sig: DoubleArray?, f: (Double) -> Double, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainer).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val residuals = remember(xs, ys, f) { DoubleArray(xs.size) { ys[it] - f(xs[it]) } }
        val x0 = xs.min(); val x1 = xs.max().let { if (it > x0) it else x0 + 1 }
        val curve = remember(xs, f) { (0..200).map { k -> val x = x0 + (x1 - x0) * k / 200; x to f(x) } }
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().weight(3f)) {
            val ylo = minOf(ys.indices.minOf { ys[it] - (sig?.get(it)?.takeIf { s -> s.isFinite() } ?: 0.0) }, curve.filter { it.second.isFinite() }.minOfOrNull { it.second } ?: 0.0)
            val yhi = maxOf(ys.indices.maxOf { ys[it] + (sig?.get(it)?.takeIf { s -> s.isFinite() } ?: 0.0) }, curve.filter { it.second.isFinite() }.maxOfOrNull { it.second } ?: 1.0)
            val ya = ylo - (yhi - ylo) * 0.06 - 1e-12; val yb = yhi + (yhi - ylo) * 0.06 + 1e-12
            fun sx(x: Double) = ((x - x0) / (x1 - x0) * size.width).toFloat()
            fun sy(y: Double) = (size.height - (y - ya) / (yb - ya) * size.height).toFloat()
            sig?.forEachIndexed { i, s -> if (s.isFinite() && s > 0) drawLine(colors.tertiary, Offset(sx(xs[i]), sy(ys[i] - s)), Offset(sx(xs[i]), sy(ys[i] + s)), 2f) }
            val path = Path(); var pen = false
            curve.forEach { (x, y) -> if (!y.isFinite() || y < ya - (yb - ya) || y > yb + (yb - ya)) pen = false else { if (pen) path.lineTo(sx(x), sy(y)) else path.moveTo(sx(x), sy(y)); pen = true } }
            // The points first, so the fitted curve is drawn over them.
            xs.indices.forEach { i -> drawCircle(colors.onSurface, FIT_POINT_SIZE.dp.toPx(), Offset(sx(xs[i]), sy(ys[i]))) }
            drawPath(path, colors.primary, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
        Text(tr("Residuals"), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val m = residuals.maxOfOrNull { kotlin.math.abs(it) }?.takeIf { it > 0 } ?: 1.0
            fun sx(x: Double) = ((x - x0) / (x1 - x0) * size.width).toFloat()
            fun sy(r: Double) = (size.height / 2 - r / m * size.height / 2 * 0.9).toFloat()
            drawLine(colors.outline, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.5f)
            xs.indices.forEach { i ->
                drawLine(colors.tertiary.copy(alpha = 0.6f), Offset(sx(xs[i]), size.height / 2), Offset(sx(xs[i]), sy(residuals[i])), 2f)
                drawCircle(colors.tertiary, FIT_POINT_SIZE.dp.toPx(), Offset(sx(xs[i]), sy(residuals[i])))
            }
        }
    }
}


/**
 * Find and replace over the table: what to find (matching cells outlined, the count, and
 * arrows to step through them), and what to put instead, for the current match or all of them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FindBar(
    query: String, onQuery: (String) -> Unit, replacement: String, onReplacement: (String) -> Unit,
    matchCase: Boolean, onMatchCase: (Boolean) -> Unit, wholeCell: Boolean, onWholeCell: (Boolean) -> Unit,
    count: Int, at: Int, onNext: () -> Unit, onPrevious: () -> Unit, onReplace: () -> Unit, onReplaceAll: () -> Unit, onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerHigh).padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(query, onQuery, singleLine = true, label = { Text(tr("Find")) }, modifier = Modifier.weight(1f))
            Text(
                if (query.isEmpty()) "" else if (count == 0) tr("None") else "${at + 1}/$count",
                style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp),
            )
            IconButton(onClick = onPrevious, enabled = count > 0) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = tr("Previous match")) }
            IconButton(onClick = onNext, enabled = count > 0) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = tr("Next match")) }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = tr("Close find and replace")) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(replacement, onReplacement, singleLine = true, label = { Text(tr("Replace with")) }, modifier = Modifier.weight(1f))
            androidx.compose.material3.TextButton(onClick = onReplace, enabled = count > 0) { Text(tr("Replace")) }
            androidx.compose.material3.TextButton(onClick = onReplaceAll, enabled = count > 0) { Text(tr("All")) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = matchCase, onClick = { onMatchCase(!matchCase) }, label = { Text(tr("Match case")) })
            FilterChip(selected = wholeCell, onClick = { onWholeCell(!wholeCell) }, label = { Text(tr("Whole cell")) })
        }
    }
}

/** Filters in use: how many rows show, and a chip per filter (tap to change it, × to drop it). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterBanner(shown: Int, total: Int, chips: List<Pair<String, Int>>, onEdit: (Int) -> Unit, onRemove: (Int) -> Unit, onClear: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.secondaryContainer).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FilterList, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(18.dp))
            Text(
                "$shown of $total rows shown · hidden rows are still plotted",
                style = MaterialTheme.typography.labelLarge, color = colors.onSecondaryContainer, modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            androidx.compose.material3.TextButton(onClick = onClear) { Text(tr("Clear filters")) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            chips.forEach { (label, c) ->
                androidx.compose.material3.InputChip(
                    selected = true, onClick = { onEdit(c) }, label = { Text(label, maxLines = 1) },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = tr("Remove the filter"), modifier = Modifier.size(16.dp).clickable { onRemove(c) }) },
                )
            }
        }
    }
}

/** A picked range: its address, its numbers summed up (sum, average, count, smallest, largest), and Copy, Clear and close. */
@Composable
private fun SelectionBar(address: String, s: com.example.cas.graph.SheetTools.Summary, onCopy: () -> Unit, onClear: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.primaryContainer).padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(address, style = MaterialTheme.typography.titleSmall, color = colors.onPrimaryContainer)
            val parts = buildList {
                if (s.numbers > 0) { add("Sum " + shortNumber(s.sum)); s.average?.let { add("Average " + shortNumber(it)) } }
                add("Count " + s.filled)
                if (s.numbers > 1) { add("Min " + shortNumber(s.min!!)); add("Max " + shortNumber(s.max!!)) }
            }
            Text(
                parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = colors.onPrimaryContainer,
                modifier = Modifier.horizontalScroll(rememberScrollState()), maxLines = 1,
            )
        }
        IconButton(onClick = onCopy) { Icon(Icons.Default.ContentCopy, contentDescription = tr("Copy the cells"), tint = colors.onPrimaryContainer) }
        IconButton(onClick = onClear) { Icon(Icons.Default.CleaningServices, contentDescription = tr("Clear the cells"), tint = colors.onPrimaryContainer) }
        IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = tr("Stop selecting"), tint = colors.onPrimaryContainer) }
    }
}

/**
 * A column's number format, as a spreadsheet's: automatic, a fixed number of decimals, a
 * percentage or scientific notation, with thousands grouped or not; a sample shows the result.
 */
@Composable
private fun NumberFormatDialog(name: String, current: com.example.cas.graph.ColumnFormat, sample: Double, onDismiss: () -> Unit, onDone: (com.example.cas.graph.ColumnFormat) -> Unit) {
    // 0 automatic, 1 fixed decimals, 2 percent, 3 scientific.
    var kind by remember { mutableStateOf(when { current.scientific -> 3; current.percent -> 2; current.decimals != null -> 1; else -> 0 }) }
    var decimals by remember { mutableStateOf(current.decimals ?: 2) }
    var thousands by remember { mutableStateOf(current.thousands) }
    fun chosen() = com.example.cas.graph.ColumnFormat(
        decimals = if (kind == 0 || (kind == 2 && decimals < 0)) null else decimals,
        percent = kind == 2, scientific = kind == 3, thousands = thousands && kind != 3, colorScale = current.colorScale,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Number format: $name") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("Auto", "Fixed", "%", "Sci").forEachIndexed { k, label ->
                        SegmentedButton(selected = kind == k, onClick = { kind = k }, shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(k, 4), icon = {}, label = { Text(tr(label), maxLines = 1) })
                    }
                }
                if (kind != 0) {
                    Text("$decimals decimal${if (decimals == 1) "" else "s"}", style = MaterialTheme.typography.labelLarge)
                    ExpressiveSlider(value = decimals.toFloat(), onValueChange = { decimals = it.roundToInt() }, valueRange = 0f..8f, steps = 7, modifier = Modifier.fillMaxWidth().semantics { contentDescription = tr("Decimals") })
                }
                if (kind != 3) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Group thousands (1 234 567)"), modifier = Modifier.weight(1f))
                    androidx.compose.material3.Switch(checked = thousands, onCheckedChange = { thousands = it })
                }
                // What the column's first number looks like.
                Text(chosen().let { if (it.changesNumbers) it.show(sample) else com.example.cas.graph.DataTable.text(sample) },
                    style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 22.sp), color = MaterialTheme.colorScheme.primary)
                Text(tr("Only the look changes: formulas, fits and the graph use the full numbers."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onDone(chosen()) }) { Text(tr("Done")) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}

/** A filter by condition on one column: the test, and the value it's against (not for empty or not empty). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterDialog(name: String, current: com.example.cas.graph.SheetTools.Filter?, onDismiss: () -> Unit, onRemove: () -> Unit, onApply: (com.example.cas.graph.SheetTools.Filter) -> Unit) {
    var op by remember { mutableStateOf(current?.op ?: com.example.cas.graph.SheetTools.FilterOp.Greater) }
    var value by remember { mutableStateOf(current?.value ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter: $name") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("Show only the rows where this column…"), style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.example.cas.graph.SheetTools.FilterOp.entries.forEach { o ->
                        FilterChip(selected = op == o, onClick = { op = o }, label = { Text(tr(o.label)) })
                    }
                }
                if (op.needsValue) OutlinedTextField(value, { value = it }, singleLine = true, label = { Text(tr("Value")) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onApply(com.example.cas.graph.SheetTools.Filter(0, op, value)) }, enabled = !op.needsValue || value.isNotBlank()) { Text(tr("Apply")) }
        },
        dismissButton = {
            Row {
                if (current != null) androidx.compose.material3.TextButton(onClick = onRemove) { Text(tr("Remove")) }
                androidx.compose.material3.TextButton(onClick = onDismiss) { Text(tr("Cancel")) }
            }
        },
    )
}

/** A button in the data table's toolbar: its icon, label, whether it's on (a toggle), and what it does. */
internal class TableTool(
    val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val on: Boolean = false, val enabled: Boolean = true,
    /** Choices offered in a menu under the button instead of one action (AutoSum's functions). */
    val menu: List<Pair<String, () -> Unit>>? = null,
    val action: () -> Unit = {},
)

/** Tools that belong together (a ribbon group on a tablet, separated by a line on a phone). */
internal class ToolGroup(val label: String, val tools: List<TableTool>)

/** The toolbar's tabs, as in Sheets and Excel. */
internal enum class TableTab(val label: String) { Home("Home"), Insert("Insert"), Data("Data"), Format("Format") }

/**
 * The data table's toolbar: the tabs as an expressive connected button group (the chosen one
 * a filled pill whose corners tighten), then the tab's tools as big tonal buttons. On a phone the
 * tools scroll in one row; on a tablet ([wide]) they stand in labelled groups, as a ribbon, beside
 * the tabs.
 */
@Composable
internal fun TableToolbar(tab: TableTab, onTab: (TableTab) -> Unit, groups: List<ToolGroup>, wide: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val tap = rememberKeyTap()
    @Composable
    fun Tabs(m: Modifier) = Row(m.clip(RoundedCornerShape(24.dp)).background(colors.surfaceContainer).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        TableTab.entries.forEach { t ->
            val on = t == tab
            val corner by androidx.compose.animation.core.animateDpAsState(if (on) 12.dp else 20.dp, label = "tab corner")
            Box(
                Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(corner)).background(if (on) colors.secondaryContainer else Color.Transparent)
                    .selectable(selected = on, role = androidx.compose.ui.semantics.Role.Tab) { tap(); onTab(t) },
                contentAlignment = Alignment.Center,
            ) { Text(tr(t.label), style = MaterialTheme.typography.labelLarge, color = if (on) colors.onSecondaryContainer else colors.onSurfaceVariant, maxLines = 1) }
        }
    }
    @Composable
    fun Tool(t: TableTool) = Box {
        var menuOpen by remember { mutableStateOf(false) }
        val corner by androidx.compose.animation.core.animateDpAsState(if (t.on || menuOpen) 14.dp else 18.dp, label = "tool corner")
        Column(
            Modifier.widthIn(min = 60.dp).height(60.dp).clip(RoundedCornerShape(corner))
                .background(if (t.on) colors.primary else if (menuOpen) colors.secondaryContainer else if (wide) Color.Transparent else colors.surfaceContainer)
                .clickable(enabled = t.enabled, onClickLabel = tr(t.label)) { tap(); if (t.menu != null) menuOpen = true else t.action() }
                .padding(horizontal = 8.dp)
                .semantics { if (t.on) stateDescription = "on" },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            val ink = when { t.on -> colors.onPrimary; !t.enabled -> colors.onSurface.copy(alpha = 0.38f); else -> colors.primary }
            Icon(t.icon, contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(3.dp))
            Text(tr(t.label), style = MaterialTheme.typography.labelSmall, color = if (t.on) colors.onPrimary else if (t.enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.38f), maxLines = 1)
        }
        t.menu?.let { items ->
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, shape = RoundedCornerShape(16.dp)) {
                items.forEach { (label, act) -> DropdownMenuItem(text = { Text(label) }, onClick = { menuOpen = false; act() }) }
            }
        }
    }
    if (wide) {
        Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tabs(Modifier.width(360.dp))
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                groups.forEach { g ->
                    Column(Modifier.clip(RoundedCornerShape(22.dp)).background(colors.surfaceContainer).padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { g.tools.forEach { Tool(it) } }
                        Text(tr(g.label), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
    } else {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Tabs(Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                groups.forEachIndexed { k, g ->
                    if (k > 0) Box(Modifier.width(1.dp).height(36.dp).background(colors.outlineVariant))
                    g.tools.forEach { Tool(it) }
                }
            }
        }
    }
}

/**
 * The formula bar, as in Sheets and Excel: the selected cell's address in a pill, ƒx, and its
 * contents, typed in here (references in the colors their cells are outlined in). Next moves
 * down a row; $ anchors the reference just typed (F4). With no cell selected it says so.
 */
@Composable
internal fun FormulaBar(
    address: String?, text: String, formulas: Boolean, referenceColors: List<Pair<IntRange, Color>>,
    anchored: String?, onAnchor: () -> Unit, focus: androidx.compose.ui.focus.FocusRequester,
    onChange: (String) -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    var value by remember(address) { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(text, androidx.compose.ui.text.TextRange(text.length))) }
    // Changed from outside (a suggestion, an anchor, undo): the field follows, cursor at the end.
    androidx.compose.runtime.LaunchedEffect(text) { if (text != value.text) value = androidx.compose.ui.text.input.TextFieldValue(text, androidx.compose.ui.text.TextRange(text.length)) }
    Row(
        modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp))
            .background(if (focused) colors.surface else colors.surfaceContainerHigh)
            .border(if (focused) 2.dp else 0.dp, if (focused) colors.primary else Color.Transparent, RoundedCornerShape(26.dp))
            .padding(start = 7.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.height(38.dp).widthIn(min = 52.dp).clip(RoundedCornerShape(19.dp)).background(if (address != null) colors.primaryContainer else colors.surfaceContainerHighest).padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) { Text(address ?: "—", style = MaterialTheme.typography.labelLarge, color = if (address != null) colors.onPrimaryContainer else colors.onSurfaceVariant, maxLines = 1) }
        Text("ƒx", style = TextStyle(fontFamily = CasFonts.CmItalic, fontSize = 17.sp, color = colors.tertiary), modifier = Modifier.padding(horizontal = 10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (address == null) Text(tr("Tap a cell to type in it"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            else androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = { value = it; if (it.text != text) onChange(it.text) },
                singleLine = true,
                textStyle = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 18.sp, color = colors.onSurface),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primary),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    // The whole keyboard (letters too: words, units, a formula's =), not just a number pad.
                    keyboardType = if (formulas) androidx.compose.ui.text.input.KeyboardType.Ascii else androidx.compose.ui.text.input.KeyboardType.Text,
                    capitalization = if (formulas) androidx.compose.ui.text.input.KeyboardCapitalization.Characters else androidx.compose.ui.text.input.KeyboardCapitalization.None,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { onNext() }),
                visualTransformation = if (referenceColors.isEmpty()) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.VisualTransformation { t ->
                    val styled = androidx.compose.ui.text.buildAnnotatedString {
                        append(t.text)
                        referenceColors.forEach { (at, color) ->
                            if (at.last < t.text.length) addStyle(androidx.compose.ui.text.SpanStyle(color = color, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), at.first, at.last + 1)
                        }
                    }
                    androidx.compose.ui.text.input.TransformedText(styled, androidx.compose.ui.text.input.OffsetMapping.Identity)
                },
                modifier = Modifier.fillMaxWidth().focusRequester(focus).onFocusChanged { focused = it.isFocused }.semantics { contentDescription = "Contents of $address" },
            )
        }
        // The reference just typed, anchored the next way round ($A$1, A$1, $A1, A1).
        if (anchored != null) Box(
            Modifier.size(38.dp).clip(CircleShape).background(colors.secondaryContainer).clickable(onClickLabel = tr("Anchor the reference with \$")) { onAnchor() },
            contentAlignment = Alignment.Center,
        ) { Text("\$", style = MaterialTheme.typography.titleMedium, color = colors.onSecondaryContainer) }
    }
}

/**
 * The floating summary pill, as a spreadsheet's status bar: the picked range's (or the selected
 * column's) sum; tap for the average, count, smallest and largest in turn. On a tablet ([all]) it
 * shows them all at once.
 */
@Composable
internal fun StatusPill(label: String, s: com.example.cas.graph.SheetTools.Summary, all: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var which by remember { mutableStateOf(0) }
    val parts = buildList {
        if (s.numbers > 0) add("Sum" to shortNumber(s.sum))
        s.average?.let { add("Average" to shortNumber(it)) }
        add("Count" to s.filled.toString())
        s.min?.let { add("Min" to shortNumber(it)) }
        s.max?.let { add("Max" to shortNumber(it)) }
    }
    if (parts.isEmpty()) return
    val shown = if (all) parts else listOf(parts[which % parts.size])
    Row(
        modifier.height(48.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(colors.surfaceContainerHighest)
            .clickable(enabled = !all, onClickLabel = tr("Show the next summary")) { which++ }.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(TableIcons.Statistics, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
        shown.forEachIndexed { k, (name, v) ->
            if (k > 0) Text("·", color = colors.onSurfaceVariant)
            Text(tr(name), style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
            Text(geometryValueText(v), style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 17.sp, color = colors.onSurface))
        }
        if (!all && parts.size > 1) Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colors.onSurfaceVariant)
    }
}

/** A column's role as an expressive segmented button: x, y, σx, σy or none. */
@Composable
private fun RoleSegments(role: String?, onRole: (String?) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.surfaceContainerHighest).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf("x", "y", "σx", "σy", null).forEach { r ->
            val on = r == role
            val corner by androidx.compose.animation.core.animateDpAsState(if (on) 12.dp else 19.dp, label = "role corner")
            val tint = roleColors(r).first
            Box(
                Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(corner)).background(if (on) (if (r == null) colors.secondaryContainer else tint) else Color.Transparent)
                    .selectable(selected = on, role = androidx.compose.ui.semantics.Role.RadioButton) { onRole(r) }.semantics { contentDescription = r ?: "Not used" },
                contentAlignment = Alignment.Center,
            ) {
                val ink = if (!on) colors.onSurface else if (r == null) colors.onSecondaryContainer else roleColors(r).second
                if (r == null) Text(tr("None"), style = MaterialTheme.typography.labelLarge, color = ink)
                else Text(
                    androidx.compose.ui.text.buildAnnotatedString {
                        append(r.take(1))
                        if (r.length > 1) withStyle(androidx.compose.ui.text.SpanStyle(baselineShift = androidx.compose.ui.text.style.BaselineShift.Subscript, fontSize = 0.7.em)) { append(r.drop(1)) }
                    },
                    style = TextStyle(fontFamily = CasFonts.CmItalic, fontSize = 18.sp, color = ink),
                )
            }
        }
    }
}

/** One of a column's actions as a tile in its sheet. */
internal class ColumnAction(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val danger: Boolean = false, val on: Boolean = false, val action: () -> Unit)

/**
 * A column's bottom sheet: its letter and name, a line summing it up, its role as a segmented
 * button, and every action as a tile, instead of a long menu.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ColumnSheet(letter: String, name: String, summary: String, role: String?, onRole: (String?) -> Unit, actions: List<ColumnAction>, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(colors.tertiaryContainer), contentAlignment = Alignment.Center) {
                    Text(letter, style = MaterialTheme.typography.titleMedium, color = colors.onTertiaryContainer)
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text(name, style = MaterialTheme.typography.titleLarge, color = colors.onSurface, maxLines = 1)
                    Text(summary, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1)
                }
            }
            RoleSegments(role, onRole)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 4) {
                actions.forEach { a ->
                    val ink = if (a.danger) colors.error else if (a.on) colors.onPrimary else colors.primary
                    Column(
                        Modifier.weight(1f).height(78.dp).clip(RoundedCornerShape(if (a.on) 16.dp else 22.dp))
                            .background(if (a.on) colors.primary else colors.surfaceContainerHigh)
                            .clickable(onClickLabel = tr(a.label)) { a.action() }.padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(a.icon, contentDescription = null, tint = ink, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(tr(a.label), style = MaterialTheme.typography.labelSmall, color = if (a.danger) colors.error else if (a.on) colors.onPrimary else colors.onSurface, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                // Empty tiles keep the last row's tiles the same width.
                repeat((4 - actions.size % 4) % 4) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** The tablet's inspector for the selected column: its role, number format, filter and statistics. */
@Composable
internal fun ColumnInspector(letter: String, name: String, role: String?, onRole: (String?) -> Unit, format: String, filter: String?, stats: com.example.cas.graph.DataTable.Stats?, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.surfaceContainer).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(colors.tertiaryContainer), contentAlignment = Alignment.Center) {
                Text(letter, style = MaterialTheme.typography.labelLarge, color = colors.onTertiaryContainer)
            }
            Text(name, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, modifier = Modifier.weight(1f).padding(start = 10.dp), maxLines = 1)
            IconButton(onClick = onOpen) { Icon(Icons.Default.MoreVert, contentDescription = tr("All of the column's actions")) }
        }
        RoleSegments(role, onRole)
        @Composable
        fun row(k: String, v: androidx.compose.ui.text.AnnotatedString) = Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr(k), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(v, style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 16.sp, color = colors.onSurface))
        }
        row("Format", androidx.compose.ui.text.AnnotatedString(format))
        if (filter != null) row("Filter", androidx.compose.ui.text.AnnotatedString(filter))
        if (stats != null) {
            row("Count", androidx.compose.ui.text.AnnotatedString(stats.n.toString()))
            row("Mean", geometryValueText(shortNumber(stats.mean)))
            row("Median", geometryValueText(shortNumber(stats.median)))
            stats.sd?.let { row("Standard deviation", geometryValueText(shortNumber(it))) }
            row("Range", geometryValueText(shortNumber(stats.min) + " to " + shortNumber(stats.max)))
        } else Text(tr("No numbers in this column yet."), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

/** Arrow keys for the selected cell, beside the formula bar: left, up, down, right. */
@Composable
internal fun CellArrows(onMove: (Int, Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val tap = rememberKeyTap()
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)) {
        listOf(
            Triple(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Cell to the left", -1 to 0),
            Triple(Icons.Default.KeyboardArrowUp, "Cell above", 0 to -1),
            Triple(Icons.Default.KeyboardArrowDown, "Cell below", 0 to 1),
            Triple(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Cell to the right", 1 to 0),
        ).forEach { (icon, label, d) ->
            Box(
                Modifier.size(width = 52.dp, height = 36.dp).clip(RoundedCornerShape(18.dp)).background(colors.secondaryContainer)
                    .clickable(onClickLabel = tr(label)) { tap(); onMove(d.first, d.second) },
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = tr(label), tint = colors.onSecondaryContainer) }
        }
    }
}

/** A highlight rule for a column: the test, its value, and the tint (conditional formatting). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HighlightDialog(name: String, current: com.example.cas.graph.HighlightRule?, onDismiss: () -> Unit, onRemove: () -> Unit, onApply: (com.example.cas.graph.HighlightRule) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var op by remember { mutableStateOf(current?.op ?: com.example.cas.graph.SheetTools.FilterOp.Greater) }
    var value by remember { mutableStateOf(current?.value ?: "") }
    var tint by remember { mutableStateOf(current?.color ?: 0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(TableIcons.Highlight, contentDescription = null) },
        title = { Text("Highlight: $name") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("Tint the cells whose value…"), style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.example.cas.graph.SheetTools.FilterOp.entries.forEach { o -> FilterChip(selected = op == o, onClick = { op = o }, label = { Text(tr(o.label)) }) }
                }
                if (op.needsValue) OutlinedTextField(value, { value = it }, singleLine = true, label = { Text(tr("Value")) }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    com.example.cas.graph.HighlightRule.COLORS.forEachIndexed { k, argb ->
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(Color(argb))
                                .border(if (k == tint) 3.dp else 0.dp, if (k == tint) colors.onSurface else Color.Transparent, CircleShape)
                                .clickable(onClickLabel = com.example.cas.graph.HighlightRule.COLOR_NAMES[k]) { tint = k }
                                .semantics { contentDescription = com.example.cas.graph.HighlightRule.COLOR_NAMES[k] + if (k == tint) ", chosen" else "" },
                            contentAlignment = Alignment.Center,
                        ) { if (k == tint) Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black.copy(alpha = 0.7f)) }
                    }
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onApply(com.example.cas.graph.HighlightRule(op, value, tint)) }, enabled = !op.needsValue || value.isNotBlank()) { Text(tr("Apply")) } },
        dismissButton = {
            Row {
                if (current != null) androidx.compose.material3.TextButton(onClick = onRemove) { Text(tr("Remove")) }
                androidx.compose.material3.TextButton(onClick = onDismiss) { Text(tr("Cancel")) }
            }
        },
    )
}

/**
 * A column's insights, as Sheets' Explore: its histogram, its summary, its straight-line trend
 * against the x column (or the row number) with r and r², and its outliers (Tukey's fences), which
 * one button outlines in the table.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun InsightsSheet(
    letter: String, name: String, stats: com.example.cas.graph.DataTable.Stats?, histogram: com.example.cas.graph.SheetTools.Histogram?,
    against: String, trend: com.example.cas.graph.SheetTools.Trend?, outliers: List<Int>, onMark: () -> Unit, onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(colors.tertiaryContainer), contentAlignment = Alignment.Center) {
                    Icon(TableIcons.Insights, contentDescription = null, tint = colors.onTertiaryContainer)
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text(tr("Insights"), style = MaterialTheme.typography.titleLarge)
                    Text("$letter · $name", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            if (stats == null || histogram == null) { Text(tr("No numbers in this column yet."), color = colors.onSurfaceVariant); return@Column }
            // The histogram: one rounded bar per bin, the tallest full height.
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.surfaceContainer).padding(16.dp)) {
                Text(tr("Distribution"), style = MaterialTheme.typography.titleSmall, color = colors.primary)
                val barColor = colors.primary; val axis = colors.outlineVariant
                androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(140.dp).padding(top = 10.dp)) {
                    val n = histogram.counts.size; val most = histogram.counts.max().coerceAtLeast(1)
                    val gap = 4.dp.toPx(); val w = (size.width - gap * (n - 1)) / n
                    histogram.counts.forEachIndexed { k, count ->
                        val h = (size.height - 2.dp.toPx()) * count / most
                        if (count > 0) drawRoundRect(barColor, topLeft = Offset(k * (w + gap), size.height - h), size = androidx.compose.ui.geometry.Size(w, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
                    }
                    drawLine(axis, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(geometryValueText(shortNumber(histogram.edges.first())), style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 13.sp, color = colors.onSurfaceVariant))
                    Spacer(Modifier.weight(1f))
                    Text(geometryValueText(shortNumber(histogram.edges.last())), style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 13.sp, color = colors.onSurfaceVariant))
                }
            }
            @Composable
            fun tile(label: String, value: String, modifier: Modifier) = Column(modifier.clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerHigh).padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(tr(label), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Text(geometryValueText(value), style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 19.sp, color = colors.onSurface), maxLines = 1)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tile("Mean", shortNumber(stats.mean), Modifier.weight(1f)); tile("Median", shortNumber(stats.median), Modifier.weight(1f))
                tile("Std dev", stats.sd?.let { shortNumber(it) } ?: "—", Modifier.weight(1f))
            }
            // The trend against x: its line, how strong, and in words.
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.surfaceContainer).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Trend against $against", style = MaterialTheme.typography.titleSmall, color = colors.primary)
                if (trend == null) Text(tr("Needs at least three pairs of numbers that vary."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                else {
                    val strength = kotlin.math.abs(trend.r).let { a -> when { a >= 0.9 -> "very strong"; a >= 0.7 -> "strong"; a >= 0.4 -> "moderate"; a >= 0.2 -> "weak"; else -> "no clear" } }
                    val direction = if (kotlin.math.abs(trend.r) < 0.2) "" else if (trend.r > 0) " rising" else " falling"
                    Text(geometryValueText("slope " + shortNumber(trend.slope) + " · intercept " + shortNumber(trend.intercept)), style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 17.sp, color = colors.onSurface))
                    Text(geometryValueText("r = " + shortNumber(trend.r) + " · r² = " + shortNumber(trend.r2) + " · n = " + trend.n), style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 17.sp, color = colors.onSurface))
                    Text("A $strength$direction straight-line relationship${if (trend.r2 >= 0.5) ": ${Math.round(trend.r2 * 100)}% of the spread follows the line" else ""}.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
            // Outliers: beyond 1.5 interquartile ranges of the quartiles.
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(if (outliers.isEmpty()) colors.surfaceContainer else colors.errorContainer).padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (outliers.isEmpty()) "No outliers" else "${outliers.size} outlier${if (outliers.size == 1) "" else "s"}", style = MaterialTheme.typography.titleSmall, color = if (outliers.isEmpty()) colors.onSurface else colors.onErrorContainer)
                    Text(if (outliers.isEmpty()) "Every number is within 1.5 interquartile ranges of the quartiles." else "Row${if (outliers.size == 1) "" else "s"} " + outliers.take(12).joinToString(", ") + if (outliers.size > 12) "…" else "",
                        style = MaterialTheme.typography.bodySmall, color = if (outliers.isEmpty()) colors.onSurfaceVariant else colors.onErrorContainer)
                }
                if (outliers.isNotEmpty()) androidx.compose.material3.FilledTonalButton(onClick = onMark) { Text(tr("Mark them")) }
            }
        }
    }
}
