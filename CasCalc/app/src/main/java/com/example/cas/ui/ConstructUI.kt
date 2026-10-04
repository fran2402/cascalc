package com.example.cas.ui

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SegmentedButton
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import kotlin.math.PI
import com.example.cas.graph.Curves
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cas.graph.Plot2D
import com.example.cas.graph.Viewport
import com.example.cas.graph.Scene
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.example.cas.graph.Pgf
import androidx.compose.ui.graphics.toArgb
import com.example.cas.ui.theme.CasFonts
import kotlin.math.abs

/** Starts construct mode (geometry mode's tools). */
@Composable
internal fun ConstructButton(onClick: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.shadow(4.dp, CircleShape).clip(CircleShape).background(colors.secondaryContainer)
            .clickable(onClickLabel = tr("Start constructing"), onClick = onClick).padding(start = 12.dp, end = 14.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AppIcon(PlotIcons.Geometry, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(20.dp))
        Text(tr("Construct"), color = colors.onSecondaryContainer, style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 14.sp, fontWeight = FontWeight.Medium))
        BetaBadge("Alpha")
    }
}

/**
 * What's being built: the tool, which step it's on (dots, and the step's role), what to tap
 * next, the picks so far as chips, and Undo, Close (a polygon) and Done.
 */
@Composable
internal fun ConstructStatus(vm: GraphViewModel, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val tool = vm.geometryTool ?: GeometryTool.Move
    val picks = vm.geometryPicks
    Column(
        modifier.widthIn(max = 460.dp).fillMaxWidth().shadow(6.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceContainerHigh).blockGraphTouches().padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                val ink = colors.onPrimaryContainer; val accent = colors.primary
                androidx.compose.foundation.Canvas(Modifier.size(24.dp)) { drawToolIcon(tool, ink, accent) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                val stepName = when {
                    tool == GeometryTool.Move -> tr(if (isTabletLayout()) "Pick a tool beside the graph" else "Pick a tool below")
                    tool.multi -> if (picks.isEmpty()) tr(tool.steps.first()) else if (tool == GeometryTool.Polygon) (if (picks.size == 1) tr("1 corner") else tr("{0} corners", picks.size)) else (if (picks.size == 1) tr("1 point") else tr("{0} points", picks.size))
                    else -> tr("Step {0} of {1}", picks.size + 1, tool.slots.length) + " · " + tr(tool.steps.getOrElse(picks.size) { "" })
                }
                Text(tool.label, style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 16.sp, fontWeight = FontWeight.Medium), color = colors.onSurface)
                Text(stepName, style = MaterialTheme.typography.labelMedium, color = colors.primary)
            }
            if (picks.isNotEmpty()) IconButton(onClick = { vm.undoPick() }) {
                AppIcon(TableIcons.Undo, contentDescription = tr("Take back the last pick"), tint = colors.onSurfaceVariant)
            }
            if (tool.multi && picks.size >= tool.least) androidx.compose.material3.FilledTonalButton(onClick = { vm.finishMulti() }, contentPadding = PaddingValues(horizontal = 14.dp)) { Text(tr(if (tool == GeometryTool.Polygon) "Close" else "Finish")) }
            // The guide again, from its start.
            if (!vm.guideOpen) IconButton(onClick = { vm.guideStep = 0; vm.guideOpen = true }) {
                AppIcon(TableIcons.Help, contentDescription = tr("Show the geometry guide"), tint = colors.onSurfaceVariant)
            }
            androidx.compose.material3.TextButton(onClick = { vm.stopConstructing() }) { Text(tr("Done")) }
        }
        // Progress: a dot per step, filled when done, ringed for the current one.
        if (tool != GeometryTool.Move && !tool.multi && tool.slots.length > 1) Row(Modifier.padding(start = 48.dp, top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(tool.slots.length) { k ->
                val done = k < picks.size; val now = k == picks.size
                Box(Modifier.size(if (now) 10.dp else 8.dp).clip(CircleShape).background(if (done) colors.primary else if (now) colors.primary.copy(alpha = 0.35f) else colors.outlineVariant))
            }
        }
        Text(
            tool.instruction(vm.geometryNeeds),
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
            modifier = Modifier.padding(start = 48.dp, top = 4.dp, end = 8.dp),
        )
        if (picks.isNotEmpty()) Row(Modifier.padding(start = 48.dp, top = 8.dp).horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            picks.forEachIndexed { k, name ->
                Row(
                    Modifier.clip(CircleShape).background(colors.secondaryContainer).padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(tr(tool.steps.getOrElse(k) { tool.steps.firstOrNull() ?: "" }) + " ", style = MaterialTheme.typography.labelSmall, color = colors.onSecondaryContainer.copy(alpha = 0.7f))
                    Text(geometryLabel(name), style = TextStyle(fontFamily = CasFonts.CmItalic, fontSize = 15.sp), color = colors.onSecondaryContainer)
                }
            }
        }
    }
}

/**
 * The tools on a phone, as an M3 sheet under the graph: groups as tabs, each tool a tile of the
 * same size with a drawn icon. Every group's grid is the same height (it scrolls when a group
 * has more tools), so the sheet, and the graph above it, never change size.
 */
@Composable
internal fun ConstructPalette(vm: GraphViewModel, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    var category by remember { mutableStateOf(GeometryCategory.Points) }
    Column(
        modifier.fillMaxWidth().wrapContentWidth().widthIn(max = 520.dp).fillMaxWidth().shadow(4.dp, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp))
            .background(colors.surfaceContainer).padding(10.dp),
    ) {
        // The groups, as tabs.
        Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            GeometryTool.categories(vm.geometrySpace).forEach { c ->
                val on = c == category
                Text(
                    c.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) colors.onSecondaryContainer else colors.onSurfaceVariant,
                    modifier = Modifier.clip(CircleShape).background(if (on) colors.secondaryContainer else Color.Transparent)
                        .clickable(onClickLabel = tr("Show {0}", c.label)) { category = c }.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        val tools = GeometryTool.of(vm.geometrySpace, category)
        // Two and a half rows show, so a group with more says it scrolls; all groups alike.
        val scroll = androidx.compose.foundation.rememberScrollState()
        androidx.compose.runtime.LaunchedEffect(category) { scroll.scrollTo(0) }
        Column(Modifier.height(TILE_HEIGHT * 2.5f + 12.dp).verticalScroll(scroll)) {
            tools.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    row.forEach { t -> Box(Modifier.weight(1f)) { ToolChip(vm, t, compact = false) } }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** A tool's tile in the phone palette: every one the same height. */
private val TILE_HEIGHT = 70.dp

/**
 * The tools on a tablet: a rail at the graph's edge, each group under a small heading, its
 * tools as round icon buttons two abreast (the tool's name shows in the card on top). Scrolls
 * when the graph is short.
 */
@Composable
internal fun ConstructRail(vm: GraphViewModel, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.width(118.dp).shadow(6.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).background(colors.surfaceContainer)
            .blockGraphTouches()
            .verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GeometryTool.categories(vm.geometrySpace).forEachIndexed { k, c ->
            if (k > 0) Spacer(Modifier.height(8.dp))
            Text(c.label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1, modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 4.dp))
            val tools = GeometryTool.of(vm.geometrySpace, c)
            tools.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                    pair.forEach { t -> ToolChip(vm, t, compact = true) }
                    if (pair.size == 1) Spacer(Modifier.size(44.dp))
                }
            }
        }
    }
}

/** A tool: its drawn icon, and its name under it (or, compact, just the icon in a round button). */
@Composable
internal fun ToolChip(vm: GraphViewModel, tool: GeometryTool, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    val on = (vm.geometryTool ?: GeometryTool.Move) == tool
    val bg = if (on) colors.primary else if (compact) colors.surfaceContainerHigh else colors.surfaceContainerLow
    val ink = if (on) colors.onPrimary else colors.onSurface
    val accent = if (on) colors.onPrimary else colors.primary
    if (compact) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(bg).clickable(onClickLabel = tool.label) { vm.selectTool(tool) }
                .semantics { contentDescription = tool.label + if (on) tr(", in use") else "" },
            contentAlignment = Alignment.Center,
        ) { androidx.compose.foundation.Canvas(Modifier.size(26.dp)) { drawToolIcon(tool, ink, accent) } }
    } else {
        Column(
            Modifier.fillMaxWidth().height(TILE_HEIGHT).clip(RoundedCornerShape(18.dp)).background(bg).clickable(onClickLabel = tr("Use {0}", tool.label)) { vm.selectTool(tool) }
                .padding(top = 8.dp, bottom = 4.dp, start = 3.dp, end = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            androidx.compose.foundation.Canvas(Modifier.size(28.dp)) { drawToolIcon(tool, ink, accent) }
            Spacer(Modifier.height(3.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                // Translated names can have long words: those get a smaller size, and may break at a hyphen.
                val name = tool.tile
                val longest = name.split(' ').maxOf { it.replace("\u00AD", "").length }
                val size = when { longest > 13 -> 9.sp; longest > 10 -> 10.sp; else -> 11.sp }
                Text(
                    name, style = MaterialTheme.typography.labelSmall.copy(fontSize = size, lineHeight = 12.sp, hyphens = androidx.compose.ui.text.style.Hyphens.Auto), color = ink, maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

/**
 * A tool's icon, drawn: what it makes in [accent], what it's made from in [ink] (points as
 * dots).
 */
internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawToolIcon(tool: GeometryTool, ink: Color, accent: Color) {
    val w = size.minDimension
    fun o(x: Float, y: Float) = Offset(x * w, y * w)
    val thin = w * 0.065f; val bold = w * 0.09f
    fun dot(x: Float, y: Float, c: Color = ink) = drawCircle(c, w * 0.085f, o(x, y))
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, c: Color = accent, width: Float = bold) = drawLine(c, o(x1, y1), o(x2, y2), width, StrokeCap.Round)
    fun stroke(path: Path, c: Color = accent, width: Float = bold) = drawPath(path, c, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    fun arc(cx: Float, cy: Float, r: Float, start: Float, sweep: Float, c: Color = accent, fill: Boolean = false) =
        drawArc(c, start, sweep, fill, topLeft = o(cx - r, cy - r), size = androidx.compose.ui.geometry.Size(2 * r * w, 2 * r * w), style = if (fill) androidx.compose.ui.graphics.drawscope.Fill else Stroke(bold, cap = StrokeCap.Round))
    fun poly(vararg p: Float, closed: Boolean = true): Path = Path().apply { moveTo(p[0] * w, p[1] * w); var k = 2; while (k < p.size) { lineTo(p[k] * w, p[k + 1] * w); k += 2 }; if (closed) close() }
    when (tool) {
        GeometryTool.Move -> drawPath(poly(0.28f, 0.14f, 0.28f, 0.8f, 0.44f, 0.64f, 0.56f, 0.9f, 0.66f, 0.85f, 0.54f, 0.6f, 0.76f, 0.6f), accent)
        GeometryTool.Point -> { dot(0.5f, 0.5f, accent); drawCircle(accent.copy(alpha = 0.3f), w * 0.2f, o(0.5f, 0.5f)) }
        GeometryTool.PointOn -> { arc(0.5f, 0.5f, 0.32f, 0f, 360f, ink); drawCircle(ink, w * 0.32f, o(0.5f, 0.5f), style = Stroke(thin)); dot(0.73f, 0.27f, accent) }
        GeometryTool.Intersect -> { line(0.12f, 0.8f, 0.88f, 0.25f, ink, thin); line(0.12f, 0.25f, 0.88f, 0.8f, ink, thin); dot(0.5f, 0.525f, accent) }
        GeometryTool.Midpoint -> { line(0.14f, 0.7f, 0.86f, 0.3f, ink, thin); dot(0.14f, 0.7f); dot(0.86f, 0.3f); dot(0.5f, 0.5f, accent) }
        GeometryTool.Segment -> { line(0.18f, 0.74f, 0.82f, 0.26f); dot(0.18f, 0.74f); dot(0.82f, 0.26f) }
        GeometryTool.Line -> { line(0.02f, 0.88f, 0.98f, 0.12f); dot(0.32f, 0.65f); dot(0.68f, 0.37f) }
        GeometryTool.Ray -> { line(0.2f, 0.75f, 0.98f, 0.15f); dot(0.2f, 0.75f); dot(0.55f, 0.48f) }
        GeometryTool.Vector -> { line(0.18f, 0.78f, 0.78f, 0.24f); drawPath(poly(0.86f, 0.16f, 0.62f, 0.24f, 0.78f, 0.4f), accent); dot(0.18f, 0.78f) }
        GeometryTool.Perpendicular -> { line(0.06f, 0.72f, 0.94f, 0.72f, ink, thin); line(0.5f, 0.06f, 0.5f, 0.94f); dot(0.5f, 0.3f); drawRect(ink, o(0.5f, 0.6f), androidx.compose.ui.geometry.Size(0.12f * w, 0.12f * w), style = Stroke(thin * 0.8f)) }
        GeometryTool.Parallel -> { line(0.06f, 0.78f, 0.94f, 0.52f, ink, thin); line(0.06f, 0.46f, 0.94f, 0.2f); dot(0.4f, 0.36f) }
        GeometryTool.Bisector -> { line(0.12f, 0.7f, 0.88f, 0.7f, ink, thin); dot(0.12f, 0.7f); dot(0.88f, 0.7f); line(0.5f, 0.08f, 0.5f, 0.94f) }
        GeometryTool.AngleBisector -> { line(0.14f, 0.82f, 0.9f, 0.82f, ink, thin); line(0.14f, 0.82f, 0.62f, 0.12f, ink, thin); line(0.14f, 0.82f, 0.94f, 0.36f); dot(0.14f, 0.82f) }
        GeometryTool.Tangent -> { drawCircle(ink, w * 0.26f, o(0.42f, 0.6f), style = Stroke(thin)); line(0.04f, 0.34f, 0.96f, 0.34f); dot(0.86f, 0.34f) }
        GeometryTool.Polygon -> { drawPath(poly(0.2f, 0.78f, 0.12f, 0.36f, 0.5f, 0.12f, 0.88f, 0.4f, 0.74f, 0.82f), accent.copy(alpha = 0.25f)); stroke(poly(0.2f, 0.78f, 0.12f, 0.36f, 0.5f, 0.12f, 0.88f, 0.4f, 0.74f, 0.82f), width = thin); dot(0.5f, 0.12f) ; dot(0.2f, 0.78f); dot(0.88f, 0.4f) }
        GeometryTool.Circle -> { drawCircle(accent, w * 0.34f, o(0.5f, 0.5f), style = Stroke(bold)); dot(0.5f, 0.5f); dot(0.74f, 0.26f) }
        GeometryTool.Circle3 -> { drawCircle(accent, w * 0.34f, o(0.5f, 0.5f), style = Stroke(bold)); dot(0.16f, 0.5f); dot(0.74f, 0.26f); dot(0.7f, 0.79f) }
        GeometryTool.Semicircle -> { arc(0.5f, 0.62f, 0.36f, 180f, 180f); line(0.14f, 0.62f, 0.86f, 0.62f, ink, thin); dot(0.14f, 0.62f); dot(0.86f, 0.62f) }
        GeometryTool.Arc -> { arc(0.3f, 0.7f, 0.56f, -80f, 70f); dot(0.3f, 0.7f); line(0.3f, 0.7f, 0.86f, 0.62f, ink, thin * 0.8f) }
        GeometryTool.Sector -> { drawArc(accent.copy(alpha = 0.3f), -75f, 75f, true, topLeft = o(-0.32f, 0.12f), size = androidx.compose.ui.geometry.Size(1.24f * w, 1.24f * w)); drawArc(accent, -75f, 75f, true, topLeft = o(-0.32f, 0.12f), size = androidx.compose.ui.geometry.Size(1.24f * w, 1.24f * w), style = Stroke(thin, join = StrokeJoin.Round)); dot(0.3f, 0.74f) }
        GeometryTool.Ellipse -> { drawOval(accent, o(0.06f, 0.26f), androidx.compose.ui.geometry.Size(0.88f * w, 0.48f * w), style = Stroke(bold)); dot(0.3f, 0.5f); dot(0.7f, 0.5f) }
        GeometryTool.Hyperbola -> {
            fun branch(sign: Float) = Path().apply { for (k in 0..20) { val u = -1.4f + 2.8f * k / 20; val x = 0.5f + sign * 0.16f * kotlin.math.cosh(u); val y = 0.5f + 0.2f * kotlin.math.sinh(u); if (k == 0) moveTo(x * w, y * w) else lineTo(x * w, y * w) } }
            stroke(branch(1f)); stroke(branch(-1f))
        }
        GeometryTool.Parabola -> {
            // A cup opening upward, its focus inside and the directrix under it.
            stroke(Path().apply { for (k in 0..20) { val x = 0.12f + 0.76f * k / 20; val y = 0.66f - 2.4f * (x - 0.5f) * (x - 0.5f); if (k == 0) moveTo(x * w, y * w) else lineTo(x * w, y * w) } })
            line(0.08f, 0.88f, 0.92f, 0.88f, ink, thin); dot(0.5f, 0.46f)
        }
        GeometryTool.Conic -> { rotate(-25f) { drawOval(accent, o(0.08f, 0.28f), androidx.compose.ui.geometry.Size(0.84f * w, 0.44f * w), style = Stroke(bold)) }; dot(0.12f, 0.62f); dot(0.5f, 0.2f); dot(0.88f, 0.38f); dot(0.5f, 0.8f); dot(0.8f, 0.66f) }
        GeometryTool.Angle -> { line(0.14f, 0.82f, 0.92f, 0.82f, ink, thin); line(0.14f, 0.82f, 0.7f, 0.18f, ink, thin); arc(0.14f, 0.82f, 0.38f, -49f, 49f); dot(0.14f, 0.82f) }
        GeometryTool.Distance -> { line(0.14f, 0.5f, 0.86f, 0.5f); line(0.14f, 0.34f, 0.14f, 0.66f, ink, thin); line(0.86f, 0.34f, 0.86f, 0.66f, ink, thin); for (k in 1..5) line(0.14f + 0.12f * k, 0.5f, 0.14f + 0.12f * k, 0.6f, ink, thin * 0.6f) }
        GeometryTool.Reflect -> {
            drawLine(ink, o(0.5f, 0.06f), o(0.5f, 0.94f), thin, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(w * 0.08f, w * 0.06f)))
            stroke(poly(0.1f, 0.74f, 0.4f, 0.74f, 0.4f, 0.3f), ink, thin); drawPath(poly(0.9f, 0.74f, 0.6f, 0.74f, 0.6f, 0.3f), accent.copy(alpha = 0.35f)); stroke(poly(0.9f, 0.74f, 0.6f, 0.74f, 0.6f, 0.3f), width = thin)
        }
        GeometryTool.Root -> { stroke(Path().apply { for (k in 0..20) { val x = 0.08f + 0.84f * k / 20; val y = 0.5f + 0.32f * kotlin.math.sin((x - 0.5f) * 7f); if (k == 0) moveTo(x * w, y * w) else lineTo(x * w, y * w) } }, ink, thin); line(0.04f, 0.5f, 0.96f, 0.5f, ink, thin * 0.6f); dot(0.5f, 0.5f, accent); dot(0.05f + 0.45f - 0.449f, 0.5f, accent); dot(0.949f, 0.5f, accent) }
        GeometryTool.Extremum -> { stroke(Path().apply { for (k in 0..20) { val x = 0.08f + 0.84f * k / 20; val y = 0.5f - 0.3f * kotlin.math.sin((x - 0.08f) * 7.5f); if (k == 0) moveTo(x * w, y * w) else lineTo(x * w, y * w) } }, ink, thin); dot(0.29f, 0.2f, accent); dot(0.71f, 0.8f, accent) }
        GeometryTool.SegmentLength -> { line(0.14f, 0.64f, 0.86f, 0.64f); dot(0.14f, 0.64f); line(0.14f, 0.34f, 0.86f, 0.34f, ink, thin * 0.6f); line(0.14f, 0.26f, 0.14f, 0.42f, ink, thin * 0.6f); line(0.86f, 0.26f, 0.86f, 0.42f, ink, thin * 0.6f) }
        GeometryTool.Polyline -> { stroke(poly(0.1f, 0.78f, 0.32f, 0.3f, 0.6f, 0.62f, 0.9f, 0.2f, closed = false)); dot(0.1f, 0.78f); dot(0.32f, 0.3f); dot(0.6f, 0.62f); dot(0.9f, 0.2f) }
        GeometryTool.FitLine -> { line(0.06f, 0.84f, 0.94f, 0.16f); dot(0.2f, 0.62f); dot(0.38f, 0.62f); dot(0.52f, 0.42f); dot(0.72f, 0.36f); dot(0.84f, 0.18f) }
        GeometryTool.Polar -> { drawCircle(ink, w * 0.26f, o(0.36f, 0.5f), style = Stroke(thin)); dot(0.9f, 0.5f); line(0.5f, 0.08f, 0.5f, 0.92f) }
        GeometryTool.RegularPolygon -> { val pts = (0 until 6).flatMap { k -> val t = Math.PI / 3 * k; listOf(0.5f + 0.38f * kotlin.math.cos(t).toFloat(), 0.5f + 0.38f * kotlin.math.sin(t).toFloat()) }.toFloatArray(); drawPath(poly(*pts), accent.copy(alpha = 0.25f)); stroke(poly(*pts), width = thin); dot(pts[0], pts[1]); dot(pts[2], pts[3]) }
        GeometryTool.CircleRadius -> { drawCircle(accent, w * 0.34f, o(0.5f, 0.5f), style = Stroke(bold)); dot(0.5f, 0.5f); line(0.5f, 0.5f, 0.84f, 0.5f, ink, thin * 0.7f) }
        GeometryTool.Compass -> { line(0.5f, 0.12f, 0.22f, 0.86f, ink, thin); line(0.5f, 0.12f, 0.78f, 0.86f, ink, thin); dot(0.5f, 0.12f); arc(0.5f, 0.12f, 0.78f, 60f, 60f) }
        GeometryTool.CircumSector -> { drawArc(accent.copy(alpha = 0.3f), 180f, 180f, true, topLeft = o(0.12f, 0.3f), size = androidx.compose.ui.geometry.Size(0.76f * w, 0.76f * w)); drawArc(accent, 180f, 180f, true, topLeft = o(0.12f, 0.3f), size = androidx.compose.ui.geometry.Size(0.76f * w, 0.76f * w), style = Stroke(thin, join = StrokeJoin.Round)); dot(0.12f, 0.68f); dot(0.5f, 0.3f); dot(0.88f, 0.68f) }
        GeometryTool.AngleSize -> { line(0.14f, 0.82f, 0.92f, 0.82f, ink, thin); line(0.14f, 0.82f, 0.66f, 0.2f); arc(0.14f, 0.82f, 0.36f, -50f, 50f); dot(0.14f, 0.82f); dot(0.66f, 0.2f, accent) }
        GeometryTool.Length -> { stroke(poly(0.12f, 0.72f, 0.4f, 0.3f, 0.86f, 0.5f, closed = false), ink, thin); line(0.12f, 0.88f, 0.88f, 0.88f); line(0.12f, 0.8f, 0.12f, 0.96f, accent, thin); line(0.88f, 0.8f, 0.88f, 0.96f, accent, thin) }
        GeometryTool.Area -> { drawPath(poly(0.14f, 0.8f, 0.24f, 0.24f, 0.82f, 0.16f, 0.88f, 0.76f), accent.copy(alpha = 0.45f)); stroke(poly(0.14f, 0.8f, 0.24f, 0.24f, 0.82f, 0.16f, 0.88f, 0.76f), ink, thin) }
        GeometryTool.Slope -> { line(0.1f, 0.86f, 0.9f, 0.14f, ink, thin); stroke(poly(0.34f, 0.65f, 0.66f, 0.65f, 0.66f, 0.36f, closed = false), width = thin) }
        GeometryTool.Relation -> { drawCircle(ink, w * 0.22f, o(0.36f, 0.5f), style = Stroke(thin)); drawCircle(accent, w * 0.22f, o(0.64f, 0.5f), style = Stroke(thin)) ; line(0.42f, 0.86f, 0.58f, 0.86f, accent, thin); line(0.42f, 0.94f, 0.58f, 0.94f, accent, thin) }
        GeometryTool.ReflectPoint -> { stroke(poly(0.1f, 0.2f, 0.36f, 0.2f, 0.1f, 0.44f), ink, thin); drawPath(poly(0.9f, 0.8f, 0.64f, 0.8f, 0.9f, 0.56f), accent.copy(alpha = 0.35f)); stroke(poly(0.9f, 0.8f, 0.64f, 0.8f, 0.9f, 0.56f), width = thin); dot(0.5f, 0.5f) }
        GeometryTool.Invert -> { drawCircle(ink, w * 0.3f, o(0.42f, 0.5f), style = Stroke(thin)); dot(0.42f, 0.5f); dot(0.58f, 0.5f, ink); dot(0.92f, 0.5f, accent) }
        GeometryTool.Rotate -> { stroke(poly(0.2f, 0.8f, 0.52f, 0.8f, 0.2f, 0.56f), ink, thin); arc(0.2f, 0.8f, 0.5f, -80f, 60f); drawPath(poly(0.24f, 0.24f, 0.4f, 0.34f, 0.22f, 0.42f), accent); dot(0.2f, 0.8f) }
        GeometryTool.Translate -> { stroke(poly(0.08f, 0.82f, 0.32f, 0.82f, 0.08f, 0.6f), ink, thin); drawPath(poly(0.66f, 0.42f, 0.9f, 0.42f, 0.66f, 0.2f), accent.copy(alpha = 0.35f)); stroke(poly(0.66f, 0.42f, 0.9f, 0.42f, 0.66f, 0.2f), width = thin); line(0.3f, 0.6f, 0.58f, 0.36f, accent, thin) }
        GeometryTool.Dilate -> { stroke(poly(0.34f, 0.7f, 0.5f, 0.7f, 0.34f, 0.56f), ink, thin); drawPath(poly(0.5f, 0.86f, 0.9f, 0.86f, 0.5f, 0.5f), accent.copy(alpha = 0.3f)); stroke(poly(0.5f, 0.86f, 0.9f, 0.86f, 0.5f, 0.5f), width = thin); dot(0.14f, 0.5f); line(0.14f, 0.5f, 0.9f, 0.86f, ink, thin * 0.5f) }
        GeometryTool.Inflection -> { stroke(Path().apply { for (k in 0..20) { val x = 0.08f + 0.84f * k / 20; val u = (x - 0.5f) * 2.2f; val y = 0.5f - 0.36f * (u * u * u - 0.6f * u); if (k == 0) moveTo(x * w, y * w) else lineTo(x * w, y * w) } }, ink, thin); dot(0.5f, 0.5f, accent) }
        GeometryTool.ClosestPoint -> { drawCircle(ink, w * 0.3f, o(0.42f, 0.56f), style = Stroke(thin)); dot(0.88f, 0.14f); line(0.88f, 0.14f, 0.63f, 0.35f, ink, thin * 0.6f); dot(0.63f, 0.35f, accent) }
        GeometryTool.CommonTangent -> { drawCircle(ink, w * 0.2f, o(0.27f, 0.6f), style = Stroke(thin)); drawCircle(ink, w * 0.13f, o(0.74f, 0.67f), style = Stroke(thin)); line(0.04f, 0.43f, 0.96f, 0.54f); line(0.06f, 0.8f, 0.96f, 0.8f) }
        GeometryTool.Incircle -> { stroke(poly(0.1f, 0.84f, 0.9f, 0.84f, 0.42f, 0.14f), ink, thin); drawCircle(accent, w * 0.19f, o(0.46f, 0.62f), style = Stroke(bold)) }
        GeometryTool.Locus -> {
            val path = Path().apply { for (k in 0..24) { val t = k / 24f * 6.283f; val x = 0.5f + 0.36f * kotlin.math.cos(t); val y = 0.5f + 0.22f * kotlin.math.sin(2 * t); if (k == 0) moveTo(x * w, y * w) else lineTo(x * w, y * w) } }
            drawPath(path, accent, style = Stroke(bold, cap = StrokeCap.Round, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(w * 0.02f, w * 0.1f))))
            dot(0.86f, 0.5f, accent)
        }
        // Space: planes drawn as parallelograms seen at a slant, solids as wire frames.
        GeometryTool.Plane3 -> { val pl = poly(0.08f, 0.66f, 0.5f, 0.88f, 0.92f, 0.34f, 0.5f, 0.12f); drawPath(pl, accent.copy(alpha = 0.3f)); stroke(pl, width = thin); dot(0.32f, 0.6f); dot(0.62f, 0.66f); dot(0.56f, 0.34f) }
        GeometryTool.PlaneParallel -> { stroke(poly(0.08f, 0.78f, 0.45f, 0.94f, 0.92f, 0.62f, 0.55f, 0.46f), ink, thin); val pl = poly(0.08f, 0.42f, 0.45f, 0.58f, 0.92f, 0.26f, 0.55f, 0.1f); drawPath(pl, accent.copy(alpha = 0.3f)); stroke(pl, width = thin); dot(0.5f, 0.34f) }
        GeometryTool.PlanePerpendicular -> { line(0.5f, 0.06f, 0.5f, 0.94f, ink, thin); val pl = poly(0.08f, 0.56f, 0.45f, 0.74f, 0.92f, 0.44f, 0.55f, 0.26f); drawPath(pl, accent.copy(alpha = 0.3f)); stroke(pl, width = thin); dot(0.5f, 0.5f) }
        GeometryTool.PerpendicularToPlane -> { stroke(poly(0.08f, 0.7f, 0.45f, 0.88f, 0.92f, 0.58f, 0.55f, 0.4f), ink, thin); line(0.5f, 0.64f, 0.5f, 0.08f); dot(0.5f, 0.2f) }
        GeometryTool.Sphere -> { drawCircle(accent, w * 0.36f, o(0.5f, 0.5f), style = Stroke(bold)); drawOval(ink, topLeft = o(0.14f, 0.42f), size = androidx.compose.ui.geometry.Size(0.72f * w, 0.16f * w), style = Stroke(thin)); dot(0.5f, 0.5f); dot(0.75f, 0.24f) }
        GeometryTool.SphereRadius -> { drawCircle(accent, w * 0.36f, o(0.5f, 0.5f), style = Stroke(bold)); drawOval(ink, topLeft = o(0.14f, 0.42f), size = androidx.compose.ui.geometry.Size(0.72f * w, 0.16f * w), style = Stroke(thin)); dot(0.5f, 0.5f); line(0.5f, 0.5f, 0.86f, 0.5f, ink, thin) }
        GeometryTool.Cube -> { stroke(poly(0.12f, 0.36f, 0.62f, 0.36f, 0.62f, 0.86f, 0.12f, 0.86f)); stroke(poly(0.12f, 0.36f, 0.36f, 0.14f, 0.86f, 0.14f, 0.62f, 0.36f), width = thin); line(0.86f, 0.14f, 0.86f, 0.64f, accent, thin); line(0.86f, 0.64f, 0.62f, 0.86f, accent, thin); dot(0.12f, 0.86f); dot(0.62f, 0.86f) }
        GeometryTool.Tetrahedron -> { stroke(poly(0.1f, 0.8f, 0.58f, 0.9f, 0.9f, 0.62f)); line(0.1f, 0.8f, 0.5f, 0.1f, accent, thin); line(0.58f, 0.9f, 0.5f, 0.1f, accent, thin); line(0.9f, 0.62f, 0.5f, 0.1f, accent, thin); dot(0.5f, 0.1f) }
        GeometryTool.Pyramid -> { drawPath(poly(0.1f, 0.74f, 0.4f, 0.9f, 0.9f, 0.76f, 0.6f, 0.62f), ink.copy(alpha = 0.25f)); stroke(poly(0.1f, 0.74f, 0.4f, 0.9f, 0.9f, 0.76f, 0.6f, 0.62f), ink, thin); for ((x, y) in listOf(0.1f to 0.74f, 0.4f to 0.9f, 0.9f to 0.76f, 0.6f to 0.62f)) line(x, y, 0.5f, 0.1f, accent, thin); dot(0.5f, 0.1f) }
        GeometryTool.Prism -> { stroke(poly(0.1f, 0.84f, 0.5f, 0.94f, 0.78f, 0.78f), ink, thin); stroke(poly(0.22f, 0.3f, 0.62f, 0.4f, 0.9f, 0.24f)); line(0.1f, 0.84f, 0.22f, 0.3f, accent, thin); line(0.5f, 0.94f, 0.62f, 0.4f, accent, thin); line(0.78f, 0.78f, 0.9f, 0.24f, accent, thin) }
        GeometryTool.Volume -> { drawPath(poly(0.12f, 0.36f, 0.62f, 0.36f, 0.62f, 0.86f, 0.12f, 0.86f), accent.copy(alpha = 0.3f)); drawPath(poly(0.12f, 0.36f, 0.36f, 0.14f, 0.86f, 0.14f, 0.62f, 0.36f), accent.copy(alpha = 0.18f)); drawPath(poly(0.62f, 0.36f, 0.86f, 0.14f, 0.86f, 0.64f, 0.62f, 0.86f), accent.copy(alpha = 0.45f)); stroke(poly(0.12f, 0.36f, 0.62f, 0.36f, 0.62f, 0.86f, 0.12f, 0.86f), width = thin) }
        GeometryTool.ReflectPlane -> { stroke(poly(0.42f, 0.06f, 0.58f, 0.2f, 0.58f, 0.94f, 0.42f, 0.8f), ink, thin); stroke(poly(0.08f, 0.66f, 0.3f, 0.66f, 0.18f, 0.4f), ink, thin); stroke(poly(0.92f, 0.66f, 0.7f, 0.66f, 0.82f, 0.4f), width = thin) }
        GeometryTool.RotateAxis -> { line(0.5f, 0.04f, 0.5f, 0.96f, ink, thin); drawArc(accent, 200f, 280f, false, topLeft = o(0.14f, 0.36f), size = androidx.compose.ui.geometry.Size(0.72f * w, 0.3f * w), style = Stroke(bold, cap = StrokeCap.Round)); drawPath(poly(0.14f, 0.5f, 0.06f, 0.4f, 0.24f, 0.4f), accent) }
        // The complex plane: points are numbers.
        GeometryTool.Multiply -> { line(0.06f, 0.82f, 0.94f, 0.82f, ink, thin * 0.6f); line(0.14f, 0.94f, 0.14f, 0.06f, ink, thin * 0.6f); dot(0.4f, 0.66f); dot(0.62f, 0.5f); line(0.14f, 0.82f, 0.46f, 0.18f, accent, thin); dot(0.46f, 0.18f, accent) }
        GeometryTool.Divide -> { line(0.06f, 0.82f, 0.94f, 0.82f, ink, thin * 0.6f); line(0.14f, 0.94f, 0.14f, 0.06f, ink, thin * 0.6f); dot(0.36f, 0.3f); dot(0.7f, 0.5f); line(0.14f, 0.82f, 0.86f, 0.72f, accent, thin); dot(0.86f, 0.72f, accent) }
        GeometryTool.Conjugate -> { line(0.04f, 0.5f, 0.96f, 0.5f, ink, thin * 0.7f); dot(0.62f, 0.2f); dot(0.62f, 0.8f, accent); drawLine(ink, o(0.62f, 0.28f), o(0.62f, 0.72f), thin * 0.6f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(w * 0.05f, w * 0.05f))) }
        GeometryTool.ComplexRoots -> { drawCircle(ink, w * 0.34f, o(0.5f, 0.5f), style = Stroke(thin)); for (k in 0 until 5) { val t = (k * 72 - 90) * kotlin.math.PI.toFloat() / 180; dot(0.5f + 0.34f * kotlin.math.cos(t), 0.5f + 0.34f * kotlin.math.sin(t), accent) } }
        GeometryTool.Modulus -> { line(0.06f, 0.82f, 0.94f, 0.82f, ink, thin * 0.6f); line(0.14f, 0.94f, 0.14f, 0.06f, ink, thin * 0.6f); line(0.14f, 0.82f, 0.74f, 0.26f); dot(0.74f, 0.26f) }
        GeometryTool.Argument -> { line(0.1f, 0.82f, 0.94f, 0.82f, ink, thin); line(0.1f, 0.82f, 0.74f, 0.2f, ink, thin); dot(0.74f, 0.2f); arc(0.1f, 0.82f, 0.36f, -44f, 44f) }
        GeometryTool.Image -> { stroke(Path().apply { moveTo(0.06f * w, 0.7f * w); quadraticTo(0.2f * w, 0.2f * w, 0.36f * w, 0.62f * w) }, ink, thin); line(0.42f, 0.5f, 0.6f, 0.5f, ink, thin); drawPath(poly(0.62f, 0.5f, 0.54f, 0.42f, 0.54f, 0.58f), ink); stroke(Path().apply { moveTo(0.66f * w, 0.3f * w); cubicTo(0.98f * w, 0.2f * w, 0.62f * w, 0.9f * w, 0.94f * w, 0.8f * w) }) }
    }
}


/** Asks for a tool's number: the radius, the number of sides, an angle in degrees, a factor or a length. */
@Composable
internal fun ToolNumberDialog(tool: GeometryTool, onDone: (String) -> Boolean, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val start = when (tool) { GeometryTool.RegularPolygon -> "5"; GeometryTool.Rotate, GeometryTool.AngleSize, GeometryTool.RotateAxis -> "45"; GeometryTool.Dilate -> "2"; GeometryTool.ComplexRoots -> "3"; GeometryTool.Image -> "f"; else -> "2" }
    var text by remember { mutableStateOf(start) }
    var bad by remember { mutableStateOf(false) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = { androidx.compose.foundation.Canvas(Modifier.size(28.dp)) { drawToolIcon(tool, colors.onSurface, colors.primary) } },
        title = { Text(tool.label) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = text, onValueChange = { text = it; bad = false },
                label = { Text(tr(tool.ask ?: "Number")) },
                singleLine = true, isError = bad,
                supportingText = { if (bad) Text(when (tool) { GeometryTool.RegularPolygon -> "A whole number, 3 or more"; GeometryTool.ComplexRoots -> "A whole number, 1 or more"; GeometryTool.Image -> "A function of z defined in the list, like f(z) = z²"; else -> "Type a number" }) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = if (tool == GeometryTool.Image) androidx.compose.ui.text.input.KeyboardType.Text else androidx.compose.ui.text.input.KeyboardType.Decimal),
            )
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { if (!onDone(text)) bad = true }) { Text(tr("Make")) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}



/** A step of the geometry guide: what it says, and what you do to finish it (null: just Next). */
private class GuideStep(val title: String, val text: String, val goal: GuideGoal?, val show: ((GraphViewModel) -> Unit)? = null)

/** What finishes a guide step, checked against how things were when it began. */
private enum class GuideGoal { PickTool, Build, Drag }

/** The guide's steps for [space]: the same three to do everywhere, then what's special there. */
private fun guideSteps(space: GeometrySpace, tablet: Boolean): List<GuideStep> = listOf(
    GuideStep(
        "Pick a tool",
        if (tablet) "The tools stand in groups beside the graph. Tap Segment to try one, or let the guide pick it." else "The tools are under the graph, in groups. Tap Segment to try one, or let the guide pick it.",
        GuideGoal.PickTool, show = { it.selectTool(GeometryTool.Segment) },
    ),
    GuideStep(
        "Tap to build",
        when (space) {
            GeometrySpace.Space -> "Tap two points, or empty spots to put new ones on the floor. The card at the top says what each tap is for."
            else -> "Tap two points, or empty spots to make new ones. The card at the top says what each tap is for."
        },
        GuideGoal.Build,
    ),
    GuideStep(
        "Drag a point",
        when (space) {
            GeometrySpace.Plane -> "Drag a point you made: everything built on it follows. A point on a line or circle slides along it."
            GeometrySpace.Space -> "Drag a point you made: it moves level, keeping its height, and everything built on it follows. Edit its line to change its height."
            GeometrySpace.Complex -> "Drag a point you made: everything built on it follows. Here points are numbers, so its line reads A = 1 + 2i."
        },
        GuideGoal.Drag,
    ),
    when (space) {
        GeometrySpace.Plane -> GuideStep("More to build", "Measure gives lengths, areas and angles; Transform reflects, rotates and dilates. Tools on curves (roots, extrema, tangents) work on your functions too.", null)
        GeometrySpace.Space -> GuideStep("Planes and solids", "Build planes through points, spheres, cubes, pyramids and prisms. Planes are cut to the box and drawn see-through, so what's behind still shows.", null)
        GeometrySpace.Complex -> GuideStep("Complex tools", "Multiply and divide points as numbers, take nth roots, or draw where a function f(z) from your list takes a point or a shape.", null)
    },
    GuideStep("Options and names", "Tap a line's color dot for its size, name and fill. Every construction is a line of text too: edit A = (1, 2), or type Circle(A, 3).", null),
)

/**
 * The geometry guide, a card over the bottom of the graph the first time Construct opens in
 * each graph (the ? on the status card opens it again). Each step says what to do and is
 * ticked off when you've done it: pick a tool, build something, drag a point. The rest are read
 * and passed with Next. Skip or Done ends it for this graph.
 */
@Composable
internal fun GeometryGuide(vm: GraphViewModel, modifier: Modifier) {
    val space = vm.geometrySpace
    androidx.compose.runtime.LaunchedEffect(vm.constructing) {
        if (vm.constructing && !AppSettings.geometryGuideSeen(space.code)) { vm.guideStep = 0; vm.guideOpen = true }
    }
    if (!vm.guideOpen || !vm.constructing) return
    val colors = MaterialTheme.colorScheme
    val steps = guideSteps(space, isTabletLayout())
    val k = vm.guideStep.coerceIn(0, steps.lastIndex)
    val step = steps[k]
    fun finish() { vm.guideOpen = false; AppSettings.markGeometryGuideSeen(space.code) }
    // How things were when the step began, to tell when its goal is met.
    val start = remember(k) { Triple(vm.functions.size, vm.pointsMoved, vm.geometryTool) }
    val met = when (step.goal) {
        GuideGoal.PickTool -> vm.geometryTool != null
        GuideGoal.Build -> vm.functions.size > start.first && vm.geometryPicks.isEmpty() && vm.geometryTool != null
        GuideGoal.Drag -> vm.pointsMoved > start.second
        null -> false
    }
    // A step done moves on by itself, after a moment to see its tick.
    androidx.compose.runtime.LaunchedEffect(k, met) {
        if (met) { kotlinx.coroutines.delay(900); if (vm.guideStep == k) vm.guideStep = k + 1 }
    }
    Column(
        modifier.widthIn(max = 460.dp).fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
            .background(colors.tertiaryContainer).blockGraphTouches().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 6.dp)
            .semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // A dot per step: done ones filled, this one wider.
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                steps.indices.forEach { j ->
                    Box(Modifier.height(6.dp).width(if (j == k) 18.dp else 6.dp).clip(CircleShape).background(if (j <= k) colors.onTertiaryContainer else colors.onTertiaryContainer.copy(alpha = 0.3f)))
                }
            }
            Text(tr("Step {0} of {1}", k + 1, steps.size), style = MaterialTheme.typography.labelMedium, color = colors.onTertiaryContainer.copy(alpha = 0.8f))
            IconButton(onClick = { finish() }) { AppIcon(TableIcons.Close, contentDescription = tr("Close the guide"), tint = colors.onTertiaryContainer) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr(step.title), style = MaterialTheme.typography.titleMedium, color = colors.onTertiaryContainer, modifier = Modifier.semantics { heading() })
            AnimatedVisibility(met, enter = fadeIn() + androidx.compose.animation.scaleIn(), exit = fadeOut()) {
                AppIcon(TableIcons.Check, contentDescription = tr("Done"), tint = colors.onTertiaryContainer, modifier = Modifier.padding(start = 8.dp).size(20.dp))
            }
        }
        Text(tr(step.text), style = MaterialTheme.typography.bodyMedium, color = colors.onTertiaryContainer, modifier = Modifier.padding(top = 4.dp, end = 8.dp))
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (k < steps.lastIndex) androidx.compose.material3.TextButton(onClick = { finish() }) { Text(tr("Skip the guide"), color = colors.onTertiaryContainer) }
            Spacer(Modifier.weight(1f))
            if (k > 0) androidx.compose.material3.TextButton(onClick = { vm.guideStep = k - 1 }) { Text(tr("Back"), color = colors.onTertiaryContainer) }
            step.show?.takeIf { !met }?.let { show -> androidx.compose.material3.TextButton(onClick = { show(vm) }) { Text(tr("Show me"), color = colors.onTertiaryContainer) } }
            androidx.compose.material3.FilledTonalButton(
                onClick = { if (k == steps.lastIndex) finish() else vm.guideStep = k + 1 },
                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(containerColor = colors.onTertiaryContainer, contentColor = colors.tertiaryContainer),
            ) { Text(tr(if (k == steps.lastIndex) "Done" else "Next")) }
        }
    }
}
