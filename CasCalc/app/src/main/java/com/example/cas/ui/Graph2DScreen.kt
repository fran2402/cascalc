package com.example.cas.ui

import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
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
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.North
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Apps
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Hexagon
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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.UploadFile
import com.example.cas.graph.Pgf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.filled.IosShare
import com.example.cas.ui.theme.CasFonts
import kotlin.math.abs

/** A curve ready to draw, with its interesting points. */
/** A line ready to draw: polylines, implicit-curve segments or a shaded region, plus its interesting points. */
private class Plotted(
    val f: PlotFunction,
    val lines: List<List<Pair<Double, Double>>>,
    val points: List<Special>,
    /** Marching-squares segments (x₁, y₁, x₂, y₂) of an equation in x and y, or of a region's edge. */
    val segments: List<DoubleArray> = emptyList(),
    /** A vector: an arrowhead is drawn at the end of the line. */
    val arrow: Boolean = false,
    /** For a region: which grid cells are inside (row 0 at the top), and whether its edge is dashed (strict < or >). */
    val mask: BooleanArray? = null,
    val maskSize: IntSize = IntSize.Zero,
    val dashed: Boolean = false,
    /** A closed list of points: the polygon to fill. */
    val fill: List<Pair<Double, Double>>? = null,
    /** Error bars of a data table's points: (x, y, σx, σy), with NaN for a missing σ. */
    val errors: List<DoubleArray> = emptyList(),
    /** A scalar field: its cells' colors (ARGB, row 0 at the top), their grid, the value range and the image to draw. */
    val field: IntArray? = null,
    val fieldSize: IntSize = IntSize.Zero,
    val fieldRange: Pair<Double, Double>? = null,
    val fieldImage: androidx.compose.ui.graphics.ImageBitmap? = null,
    /** A vector field's arrows and the range of |F| their colors span. */
    val arrows: List<com.example.cas.graph.VectorField.Arrow> = emptyList(),
    val arrowRange: Pair<Double, Double> = 0.0 to 1.0,
    /** The [segments] are a slope field's short marks: drawn thin and light, and not tapped. */
    val slopeMarks: Boolean = false,
    /** A construction's words on the graph: a point's name, an angle's size. */
    val labels: List<GeoLabel> = emptyList(),
)

/** Text on the graph at (x, y): beside a point (its name), or centered there (an angle's value). */
private class GeoLabel(val x: Double, val y: Double, val text: String, val centered: Boolean)

/** A construction's name as written on the graph: A, or A₁ for a built symbol with a subscript. */
internal fun geometryLabel(name: String): String {
    val c = com.example.cas.cas.CustomSymbol.decode(name) ?: return name
    val sub = c.sub.map { ch -> "₀₁₂₃₄₅₆₇₈₉".getOrNull(ch - '0')?.takeIf { ch.isDigit() } ?: ch }.joinToString("")
    return c.preSup + c.base + sub + c.sup
}

private data class Special(val x: Double, val y: Double, val label: String, val colorIndex: Int, /** At a crossing, the other curve's color slot. */ val other: Int? = null)

@Composable
fun Graph2DScreen(vm: Graph2DViewModel, onUseValue: (Double) -> Unit, modifier: Modifier = Modifier) {
    BackHandler(enabled = vm.active != null) { vm.edit(null) }
    GraphScaffold(vm, outputLabel = "y", modifier = modifier) {
        Graph2DCanvas(vm, onUseValue, Modifier.fillMaxSize())
    }
}

fun Graph2DViewModel.resetView(size: IntSize) {
    if (size.width > 0) view = com.example.cas.graph.AxisScale.standard(Viewport.standard(size.height.toDouble() / size.width, halfWidth = AppSettings.viewHalfWidth.toDouble()), scale)
}

@Composable
private fun Graph2DCanvas(vm: Graph2DViewModel, onUseValue: (Double) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    var size by remember { mutableStateOf(IntSize.Zero) }
    var trace by remember { mutableStateOf<Special?>(null) }
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    // A free point's drag has moved it (the first move is the undo step).
    var moved by remember { mutableStateOf(false) }
    // Lines are read again when geometry is turned on or off in settings.
    androidx.compose.runtime.LaunchedEffect(AppSettings.geometry) { vm.refreshAll(); if (!AppSettings.geometry) vm.stopConstructing() }
    val themeColors = (0 until GraphViewModel.PLOT_COLOR_COUNT).map { plotColor(it) }
    // Colors picked by long-pressing a function's dot replace the theme's.
    val picked = vm.functions.mapNotNull { f -> f.customColor?.let { f.colorIndex to Color(it) } }.toMap()
    val palette = themeColors.indices.map { picked[it] ?: themeColors[it] }

    val view = vm.view
    val version = vm.version
    // Sampling and point-finding only redo when the view or a function changes.
    val highlighted = vm.highlighted
    val plotted = remember(view, version, size, vm.parameters.toMap(), highlighted, vm.polarGrid, vm.scale, AppSettings.specialPoints, AppSettings.fieldQuality) {
        // Whatever a line does while being sampled, drawing carries on (the line just isn't drawn).
        if (view == null || size.width == 0) emptyList() else runCatching { plot(vm, view, size, highlighted) }.getOrElse { emptyList() }
    }
    // The drawing as it is now, for gestures that outlive one frame (a dragged point).
    val latestPlotted by androidx.compose.runtime.rememberUpdatedState(plotted)

    Box(
        modifier
            .clipToBounds()
            .onSizeChanged { size = it; if (vm.view == null) vm.resetView(it) }
            .pointerInput(Unit) {
                // Drag to move; pinch to zoom around your fingers. A sideways pinch stretches only x,
                // an up-and-down pinch only y.
                detectAxisTransformGestures { centroid, pan, zoomX, zoomY ->
                    val v = vm.view ?: return@detectAxisTransformGestures
                    val w = size.width.toDouble(); val h = size.height.toDouble()
                    vm.view = v.panBy(pan.x / w, pan.y / h).zoomAxes(zoomX, zoomY, centroid.x / w, centroid.y / h)
                    trace = null
                }
            }
            .pointerInput(plotted) {
                // Drag along a curve to read it continuously: a drag that starts on a curve follows
                // that curve (the bubble moving with the finger); anywhere else the graph pans.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    if (vm.areaStart != null) return@awaitEachGesture
                    val reach = 28.dp.toPx()
                    val first = findTrace(vm, plotted, down.position, size, reach)?.takeIf { it.label.isEmpty() || it.label == "point" }
                        ?: return@awaitEachGesture
                    val slop = viewConfiguration.touchSlop
                    var tracing = false
                    while (true) {
                        val event = awaitPointerEvent()
                        // A second finger means a pinch: leave it to the zoom.
                        if (event.changes.count { it.pressed } > 1) break
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        // A construction's point being dragged takes the gesture.
                        if (change.isConsumed) { trace = null; break }
                        if (!tracing && (change.position - down.position).getDistance() > slop) tracing = true
                        if (tracing) {
                            change.consume()
                            traceAlong(vm, plotted, first.colorIndex, change.position, size)?.let { trace = it }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                // Drag an edge of the shaded area along the graph; its value follows the finger.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val ar = vm.area ?: return@awaitEachGesture
                    val v0 = vm.view ?: return@awaitEachGesture
                    val reach = 28.dp.toPx()
                    val sc = vm.scale
                    fun screen(x: Double) = Offset(((sc.x(x) - v0.xMin) / v0.width * size.width).toFloat(), ((v0.yMax - areaY(sc, v0, areaHeight(vm, ar, x))) / v0.height * size.height).toFloat())
                    val da = (screen(ar.a) - down.position).getDistance(); val db = (screen(ar.b) - down.position).getDistance()
                    if (minOf(da, db) > reach) return@awaitEachGesture
                    val end = db < da
                    down.consume()
                    trace = null
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        val v = vm.view ?: break
                        vm.moveAreaEdge(end, vm.scale.realX(v.xMin + change.position.x / size.width * v.width))
                    }
                }
            }
            .pointerInput(plotted) {
                // Drag a point like (a, b) to move its sliders, as in Desmos. Handled here, before
                // the view's own drag, which gives way when this one takes the gesture.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val v0 = vm.view ?: return@awaitEachGesture
                    val reach = 28.dp.toPx()
                    val hit = plotted.asSequence().mapNotNull { p -> vm.movableLetters(p.f)?.let { letters -> p to letters } }
                        .flatMap { (p, letters) -> p.points.asSequence().map { it to letters } }
                        .map { (pt, letters) ->
                            val sx = ((pt.x - v0.xMin) / v0.width * size.width).toFloat(); val sy = ((v0.yMax - pt.y) / v0.height * size.height).toFloat()
                            Triple(pt, letters, kotlin.math.hypot(sx - down.position.x, sy - down.position.y))
                        }
                        .filter { it.third < reach }.minByOrNull { it.third } ?: return@awaitEachGesture
                    val (lx, ly) = hit.second
                    down.consume()
                    trace = null
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        val v = vm.view ?: break
                        val sc = vm.scale
                        val x = sc.realX(v.xMin + change.position.x / size.width * v.width)
                        val y = sc.realY(v.yMax - change.position.y / size.height * v.height)
                        // Snap to a tidy value (a hundredth of the view; on a log axis, three figures) as the finger moves.
                        val snapX = Plot2D.niceStep(v.width, 100); val snapY = Plot2D.niceStep(v.height, 100)
                        lx?.let { vm.dragSlider(it, if (sc.logX) sc.tidy(x) else Math.round(x / snapX) * snapX) }
                        ly?.let { vm.dragSlider(it, if (sc.logY) sc.tidy(y) else Math.round(y / snapY) * snapY) }
                    }
                }
            }
            .pointerInput(Unit) {
                // Drag a free point of a construction (A = (1, 2)) to move it; what's built on it follows.
                // (Keyed on nothing, as the drawing changes with every move: it reads the latest.)
                awaitEachGesture {
                    val plotted = latestPlotted
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!AppSettings.geometry) return@awaitEachGesture
                    val v0 = vm.view ?: return@awaitEachGesture
                    val reach = 28.dp.toPx()
                    val hit = plotted.asSequence().filter { it.f.geometry?.isFree == true || it.f.geometry?.onPath == true }
                        .mapNotNull { p -> p.points.firstOrNull()?.let { pt -> p.f to kotlin.math.hypot(((pt.x - v0.xMin) / v0.width * size.width).toFloat() - down.position.x, ((v0.yMax - pt.y) / v0.height * size.height).toFloat() - down.position.y) } }
                        .filter { it.second < reach }.minByOrNull { it.second } ?: return@awaitEachGesture
                    val slop = viewConfiguration.touchSlop
                    var dragging = false
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } > 1) break
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (!dragging && (change.position - down.position).getDistance() > slop) { dragging = true; trace = null }
                        if (!dragging) continue
                        change.consume()
                        val v = vm.view ?: break
                        val sc = vm.scale
                        // Snapped to a tidy value, a hundredth of the view.
                        val snapX = Plot2D.niceStep(v.width, 100); val snapY = Plot2D.niceStep(v.height, 100)
                        val x = Math.round(sc.realX(v.xMin + change.position.x / size.width * v.width) / snapX) * snapX
                        val y = Math.round(sc.realY(v.yMax - change.position.y / size.height * v.height) / snapY) * snapY
                        // A point on a path slides along it (unsnapped); a free point goes where the finger is.
                        if (hit.first.geometry?.onPath == true) vm.movePathPoint(hit.first, sc.realX(v.xMin + change.position.x / size.width * v.width), sc.realY(v.yMax - change.position.y / size.height * v.height), first = !moved)
                        else vm.moveFreePoint(hit.first, x, y, first = !moved)
                        moved = true
                    }
                    moved = false
                }
            }
            .pointerInput(plotted) {
                detectTapGestures(
                    onDoubleTap = { vm.resetView(size); trace = null },
                    onTap = { tap ->
                        // Building with a tool: tap a point (or empty space, which makes one there), or an object.
                        vm.geometryTool?.let { tool ->
                            val v = vm.view ?: return@detectTapGestures
                            val reach = 28.dp.toPx()
                            fun sx(x: Double) = ((x - v.xMin) / v.width * size.width).toFloat()
                            fun sy(y: Double) = ((v.yMax - y) / v.height * size.height).toFloat()
                            val wx = vm.scale.realX(v.xMin + tap.x / size.width * v.width)
                            val wy = vm.scale.realY(v.yMax - tap.y / size.height * v.height)
                            // The nearest construction point, named if it wasn't (so the new line can use it).
                            fun pointNear(): String? = plotted.asSequence().filter { it.f.geometry != null && vm.geometryOf(it.f) is com.example.cas.graph.Geometry.Point }
                                .mapNotNull { p -> p.points.firstOrNull()?.let { pt -> p.f to kotlin.math.hypot(sx(pt.x) - tap.x, sy(pt.y) - tap.y) } }
                                .filter { it.second < reach }.minByOrNull { it.second }?.first?.let { vm.nameLine(it) }
                            // The nearest object of the kind wanted: a construction, or a function f(x) = … for O.
                            fun objectNear(kind: Char): String? {
                                fun fits(o: com.example.cas.graph.Geometry.Obj?) = when (kind) {
                                    'L' -> o is com.example.cas.graph.Geometry.Line || o is com.example.cas.graph.Geometry.Segment || o is com.example.cas.graph.Geometry.Ray || o is com.example.cas.graph.Geometry.Vector
                                    'C' -> o is com.example.cas.graph.Geometry.Circle || o is com.example.cas.graph.Geometry.Conic || o is com.example.cas.graph.Geometry.Arc
                                    else -> o != null && o !is com.example.cas.graph.Geometry.Point && o !is com.example.cas.graph.Geometry.Number && o !is com.example.cas.graph.Geometry.Angle
                                }
                                val best = plotted.asSequence().mapNotNull { p ->
                                    val isFunction = p.f.geometry == null && (kind == 'O' || kind == 'X') && com.example.cas.engine.UserFunction.definition(p.f.editor.root.items)?.second?.size == 1
                                    if (!isFunction && !(p.f.geometry != null && fits(vm.geometryOf(p.f)))) return@mapNotNull null
                                    val d = p.lines.minOfOrNull { line -> polylineDistance(line.map { Offset(sx(it.first), sy(it.second)) }, tap) } ?: return@mapNotNull null
                                    Triple(p.f, isFunction, d)
                                }.filter { it.third < reach }.minByOrNull { it.third } ?: return null
                                return if (best.second) com.example.cas.engine.UserFunction.definition(best.first.editor.root.items)?.first else vm.nameLine(best.first)
                            }
                            val snapX = Plot2D.niceStep(v.width, 100); val snapY = Plot2D.niceStep(v.height, 100)
                            fun newPoint() = vm.addFreePoint(Math.round(wx / snapX) * snapX, Math.round(wy / snapY) * snapY)
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            when (tool) {
                                GeometryTool.Move -> {}
                                GeometryTool.Point -> if (pointNear() == null) newPoint()
                                GeometryTool.PointOn -> objectNear('O')?.let { vm.addPointOn(it, wx, wy) }
                                else -> when (val need = vm.geometryNeeds ?: 'P') {
                                    'P' -> vm.pickForTool(pointNear() ?: newPoint())
                                    'X' -> (pointNear() ?: objectNear('O'))?.let { vm.pickForTool(it) }
                                    else -> objectNear(need)?.let { vm.pickForTool(it) }
                                }
                            }
                            trace = null
                            return@detectTapGestures
                        }
                        val start = vm.areaStart
                        if (start != null) {
                            // The second point of an area: a marked point if one is near, else where you tapped.
                            val v = vm.view
                            if (v != null) {
                                val near = findTrace(vm, plotted, tap, size, 28.dp.toPx())?.takeIf { it.label.isNotEmpty() }
                                vm.finishArea(vm.scale.realX(near?.x ?: (v.xMin + tap.x / size.width * v.width)))
                            }
                            trace = null
                            return@detectTapGestures
                        }
                        val t = findTrace(vm, plotted, tap, size, 28.dp.toPx())
                        // Over a slope field, a tap on empty space starts a solution curve there.
                        val field = (vm.highlighted?.takeIf { it.plot is Plot2DKind.SlopeField && it.visible } ?: vm.functions.firstOrNull { it.visible && it.plot is Plot2DKind.SlopeField })
                        if ((t == null || t.label.startsWith("value ") || t.label.startsWith("vector ")) && field != null && trace == null) {
                            vm.view?.let { v -> vm.addSeed(field, vm.scale.realX(v.xMin + tap.x / size.width * v.width), vm.scale.realY(v.yMax - tap.y / size.height * v.height)) }
                            return@detectTapGestures
                        }
                        trace = t
                        // Tapping a curve highlights it; tapping empty space clears.
                        vm.focus = t?.let { hit -> vm.functions.firstOrNull { it.colorIndex == hit.colorIndex } }
                    },
                )
            }
            .semantics { contentDescription = "Graph. Drag to move, pinch to zoom, tap a curve to read a point, double-tap to reset." },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // The canvas's own size (in pixels, as Float), not the view's IntSize state of the same name.
            val size = this.size
            val v = view ?: return@Canvas
            val sc = vm.scale
            // Fields first, under the grid, the axes and every line (the first in the list on top).
            plotted.asReversed().forEach { p ->
                p.fieldImage?.let { img -> drawImage(img, dstSize = IntSize(size.width.toInt(), size.height.toInt()), filterQuality = FilterQuality.Low) }
            }
            val overField = plotted.any { it.fieldImage != null }
            if (vm.polarGrid && sc.linear) drawPolarGrid(v, colors.outlineVariant, colors.onSurfaceVariant, measurer, vm.angle == com.example.cas.engine.AngleUnit.Degrees, halo = if (overField) colors.surface else null)
            // The grid can be turned off in settings (the axes stay).
            else drawGrid(v, if (AppSettings.showGrid) colors.outlineVariant else Color.Transparent, colors.onSurfaceVariant, measurer, halo = if (overField) colors.surface else null, scale = sc)
            vm.area?.let { ar ->
                if (ar.arc && !ar.signed.isNaN()) arcPath(vm, ar, sc).let { pts ->
                    // The measured stretch of curve, as a broad translucent band over it.
                    val path = Path()
                    pts.forEachIndexed { k, (x, y) -> val o = toScreen(v, x, y); if (k == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y) }
                    drawPath(path, palette[ar.f.colorIndex].copy(alpha = 0.35f), style = Stroke(10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    listOf(ar.a, ar.b).forEach { x ->
                        val top = toScreen(v, sc.x(x), areaY(sc, v, areaHeight(vm, ar, x)))
                        drawCircle(colors.surface, radius = 8.dp.toPx(), center = top)
                        drawCircle(palette[ar.f.colorIndex], radius = 8.dp.toPx(), center = top, style = Stroke(2.5.dp.toPx()))
                    }
                } else if (!ar.signed.isNaN()) areaOutline(vm, ar, v, sc)?.let { poly ->
                    val path = Path()
                    poly.forEachIndexed { k, (x, y) -> val o = toScreen(v, x, y); if (k == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y) }
                    path.close()
                    drawPath(path, palette[ar.f.colorIndex].copy(alpha = 0.28f))
                    // The edges, as handles to drag.
                    listOf(ar.a, ar.b).forEach { x ->
                        val top = toScreen(v, sc.x(x), areaY(sc, v, areaHeight(vm, ar, x)))
                        drawLine(palette[ar.f.colorIndex], Offset(top.x, 0f), Offset(top.x, size.height), 1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())))
                        drawCircle(colors.surface, radius = 8.dp.toPx(), center = top)
                        drawCircle(palette[ar.f.colorIndex], radius = 8.dp.toPx(), center = top, style = Stroke(2.5.dp.toPx()))
                    }
                }
            }
            // Line by line, the first in the list last, so it sits on top of all the others: its
            // curve, region and points together, over everything from the lines below it.
            plotted.asReversed().forEach { p ->
                drawCurve(v, p, palette[p.f.colorIndex])
                drawErrorBars(v, p.errors, palette[p.f.colorIndex], sc)
                p.points.forEach { s ->
                val o = toScreen(v, s.x, s.y)
                // Plotted points in their line's mark and size; found points (zeros, extrema…) are rings.
                if (s.label == "point") {
                    drawMarker(com.example.cas.graph.Marker.of(p.f.pointShape), o, p.f.pointSize.dp.toPx(), palette[s.colorIndex])
                } else {
                    drawCircle(colors.surface, radius = 5.dp.toPx(), center = o)
                    drawCircle(palette[s.colorIndex], radius = 5.dp.toPx(), center = o, style = Stroke(2.dp.toPx()))
                }
            } }
            vm.areaStart?.let { (f, _, a) ->
                val fn = (f.plot as? Plot2DKind.Explicit)?.f
                if (fn != null) {
                    val o = toScreen(v, sc.x(a), areaY(sc, v, vm.call(f, fn, a)))
                    drawLine(palette[f.colorIndex], Offset(o.x, 0f), Offset(o.x, size.height), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())))
                    drawCircle(palette[f.colorIndex], radius = 6.dp.toPx(), center = o)
                }
            }
            // Coordinates beside points whose line has "Show coordinates" on.
            val labelStyle = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 13.sp, color = colors.onSurface)
            plotted.filter { it.f.showLabel }.forEach { p ->
                p.points.filter { it.label == "point" }.take(200).forEach { s ->
                    val o = toScreen(v, s.x, s.y)
                    if (o.x in -40f..size.width && o.y in 0f..size.height + 20f) {
                        val t = measurer.measure("(" + shortNumber(sc.realX(s.x)) + ", " + shortNumber(sc.realY(s.y)) + ")", labelStyle)
                        drawText(t, topLeft = Offset(o.x + 8.dp.toPx(), o.y - t.size.height - 4.dp.toPx()))
                    }
                }
            }
            // Constructions' names and angle values, in math italic beside their points.
            val nameStyle = TextStyle(fontFamily = CasFonts.CmItalic, fontSize = 16.sp, color = colors.onSurface)
            val valueStyle = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 14.sp, color = colors.onSurface)
            plotted.forEach { p ->
                p.labels.forEach { l ->
                    val o = toScreen(v, l.x, l.y)
                    val t = measurer.measure(l.text, if (l.centered) valueStyle else nameStyle)
                    val at = if (l.centered) Offset(o.x - t.size.width / 2f, o.y - t.size.height / 2f) else Offset(o.x + 6.dp.toPx(), o.y - t.size.height - 2.dp.toPx())
                    drawText(t, topLeft = at)
                }
            }
            // What the next tap can take, softly lit: objects of the kind wanted, or the points to choose from.
            vm.geometryNeeds?.let { need ->
                val glow = colors.primary.copy(alpha = 0.14f)
                plotted.forEach { p ->
                    val o = if (p.f.geometry != null) vm.geometryOf(p.f) else null
                    val isFunction = p.f.geometry == null && com.example.cas.engine.UserFunction.definition(p.f.editor.root.items)?.second?.size == 1
                    val fits = when (need) {
                        'L' -> o is com.example.cas.graph.Geometry.Line || o is com.example.cas.graph.Geometry.Segment || o is com.example.cas.graph.Geometry.Ray || o is com.example.cas.graph.Geometry.Vector
                        'C' -> o is com.example.cas.graph.Geometry.Circle || o is com.example.cas.graph.Geometry.Conic || o is com.example.cas.graph.Geometry.Arc
                        'O', 'X' -> isFunction || o != null && o !is com.example.cas.graph.Geometry.Point && o !is com.example.cas.graph.Geometry.Number && o !is com.example.cas.graph.Geometry.Angle && o !is com.example.cas.graph.Geometry.Bool
                        else -> false
                    }
                    if (fits) p.lines.forEach { line ->
                        val path = Path()
                        line.forEachIndexed { k, (x, y) -> val q = toScreen(v, x, y); if (k == 0) path.moveTo(q.x, q.y) else path.lineTo(q.x, q.y) }
                        drawPath(path, glow, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    if ((need == 'P' || need == 'X') && o is com.example.cas.graph.Geometry.Point) p.points.firstOrNull()?.let { s ->
                        drawCircle(glow, radius = 13.dp.toPx(), center = toScreen(v, s.x, s.y))
                    }
                }
            }
            // What's picked so far with a tool: points ringed, objects drawn over in a broad band.
            if (vm.geometryTool != null) vm.geometryPicks.forEach { name ->
                val p = plotted.firstOrNull { it.f.geometry?.name == name || it.f.geometry == null && com.example.cas.engine.UserFunction.definition(it.f.editor.root.items)?.first == name } ?: return@forEach
                p.points.firstOrNull()?.let { s ->
                    val o = toScreen(v, s.x, s.y)
                    drawCircle(colors.primary, radius = 10.dp.toPx(), center = o, style = Stroke(2.5.dp.toPx()))
                }
                p.lines.forEach { line ->
                    val path = Path()
                    line.forEachIndexed { k, (x, y) -> val o = toScreen(v, x, y); if (k == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y) }
                    drawPath(path, colors.primary.copy(alpha = 0.35f), style = Stroke(9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            trace?.let { t ->
                val o = toScreen(v, t.x, t.y)
                drawCircle(palette[t.colorIndex], radius = 7.dp.toPx(), center = o)
                drawCircle(colors.inverseSurface, radius = 3.dp.toPx(), center = o)
            }
        }
        // The legend, top left (each drawn line's name, in list order), and under it the area card.
        // (Beside the construct rail, when it stands on this side of a tablet's graph.)
        val railHere = vm.constructing && AppSettings.geometry && isTabletLayout() && AppSettings.keypadSide == 0
        Column(Modifier.align(Alignment.TopStart).padding(start = if (railHere) 138.dp else 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GraphLegend(
            remember(version, palette) {
                vm.functions.filter { it.visible && !it.isText && (it.plot != null || it.family.isNotEmpty()) && legendSource(it).isNotBlank() }
                    .map { screenLegendEntry2D(it, palette[it.colorIndex]) }
            },
            )
            vm.area?.let { ar -> AreaCard(ar, onClose = { vm.clearArea() }, onUse = { v -> onUseValue(v) }) }
        }
        // Construct mode: a button to start it; then a status card on top and the tool palette below.
        if (AppSettings.geometry) {
            if (!vm.constructing) ConstructButton(onClick = { vm.constructing = true }, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
            else {
                // On a tablet the tools stand in a rail at the graph's edge by the list, always open
                // (GeoGebra's tablet toolbar); on a phone they're a sheet at the bottom that folds away.
                if (isTabletLayout()) {
                    val byList = if (AppSettings.keypadSide == 0) Alignment.TopStart else Alignment.TopEnd
                    ConstructStatus(vm, Modifier.align(Alignment.TopCenter).padding(top = 10.dp, start = 140.dp, end = 140.dp))
                    ConstructRail(vm, Modifier.align(byList).padding(top = 10.dp, bottom = 84.dp, start = 10.dp, end = 10.dp))
                } else {
                    ConstructStatus(vm, Modifier.align(Alignment.TopCenter).padding(top = 10.dp, start = 12.dp, end = 12.dp))
                    ConstructPalette(vm, Modifier.align(Alignment.BottomCenter).padding(bottom = 78.dp, start = 10.dp, end = 10.dp))
                }
            }
        }
        if (vm.areaStart != null) {
            Text(
                when { vm.areaStart?.arc == true -> "Tap where the arc should end"; vm.areaStart?.g != null -> "Tap where the area between the curves should end"; else -> "Tap where the area should end" },
                color = colors.inverseOnSurface,
                style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 14.sp),
                modifier = Modifier.align(Alignment.TopCenter).padding(12.dp).clip(CircleShape).background(colors.inverseSurface)
                    .clickable(onClickLabel = "Cancel") { vm.areaStart = null }.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        val tap = rememberKeyTap()
        // Export as PDF, PNG, JPG or SVG, in the shape of the graph on screen.
        var exporting by remember { mutableStateOf(false) }
        var graphSettings by remember { mutableStateOf(false) }
        val build = { r: ExportRequest -> graph2DScene(vm, r.view, EXPORT_SIZE, r.dark) }
        val export = rememberGraphExporter("graph", build)
        val writeFile = rememberGraphFileWriter()
        if (exporting && view != null) ExportDialog(
            view, build, onExport = { r, share -> exporting = false; export(r, share) }, onDismiss = { exporting = false },
            graphFile = com.example.cas.graph.GraphFile.Kind.Graph2D,
            scale = vm.scale,
            onGraphFile = { share -> exporting = false; writeFile(com.example.cas.graph.GraphFile.Contents(com.example.cas.graph.GraphFile.Kind.Graph2D, "graph", vm.graphData()), share) },
        )
        // Import data points from a CSV file or a spreadsheet (Excel, Google Sheets, LibreOffice).
        val context = androidx.compose.ui.platform.LocalContext.current
        val scope = rememberCoroutineScope()
        // Several sheets with numbers: the ones to import are picked first.
        var sheetChoice by remember { mutableStateOf<List<com.example.cas.graph.Spreadsheet.Sheet>?>(null) }
        fun importTables(tables: List<Pair<String?, com.example.cas.graph.Csv.Table>>) {
            // One line per table: its first column against its second. The other columns
            // (more y values, σ(x), σ(y)) are picked in the table.
            val n = tables.sumOf { (name, t) -> if (t.columns.isEmpty()) 0 else vm.importTable(t, name) }
            val columns = tables.singleOrNull()?.second?.columns?.size ?: 0
            android.widget.Toast.makeText(
                context,
                when {
                    n == 0 -> "No numbers found in that file"
                    tables.size > 1 -> "$n points from ${tables.size} sheets"
                    columns > 2 -> "$n points · $columns columns, pick more in the table"
                    else -> "$n points"
                },
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
        val importer = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val sheets = runCatching {
                    withContext(Dispatchers.IO) {
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                        // Workbooks (.xlsx, .xls, .ods) are known by their first bytes; anything else is read as text.
                        com.example.cas.graph.Spreadsheet.sheets(bytes)?.filter { it.table.columns.isNotEmpty() }
                            ?: listOf(com.example.cas.graph.Spreadsheet.Sheet("", com.example.cas.graph.Csv.parse(bytes.toString(Charsets.UTF_8))))
                    }
                }.getOrDefault(emptyList())
                if (sheets.size > 1) sheetChoice = sheets else importTables(sheets.map { null to it.table })
            }
        }
        sheetChoice?.let { sheets ->
            SheetPickerDialog(sheets, onImport = { picked ->
                sheetChoice = null
                importTables(picked.map { (if (picked.size > 1) it.name.ifBlank { null }?.replace("$", "") else null) to it.table })
            }, onDismiss = { sheetChoice = null })
        }
        GraphBottomBar(vm, Modifier.align(Alignment.BottomCenter), onExport = { if (size.width > 0) exporting = true }, tables = true, leading = {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(48.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(colors.secondaryContainer)
                    .clickable(onClickLabel = "Import points from a CSV, Excel or Google Sheets file") {
                        tap()
                        importer.launch(arrayOf(
                            "text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream", "application/zip",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel.sheet.macroEnabled.12",
                            "application/vnd.oasis.opendocument.spreadsheet", "application/x-msexcel",
                        ))
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = "Import data", tint = colors.onSecondaryContainer)
            }
        }, tools = {
            // Circles of constant r mean nothing on log axes: turning the grid on goes back to linear ones.
            ToolToggle(PlotIcons.PolarGrid, "Polar grid", vm.polarGrid) { if (!vm.polarGrid) vm.setLogAxes(false, false); vm.polarGrid = !vm.polarGrid }
            // Equal scales on both axes, so circles look round.
            IconButton(onClick = { tap(); vm.zoomSquare(size.width, size.height) }) {
                Icon(Icons.Default.CropSquare, contentDescription = "Square zoom: equal scales", tint = colors.onSurface)
            }
            // The graph's settings, like Desmos's wrench: limits, grid, numbers, angle unit.
            IconButton(onClick = { tap(); graphSettings = true }) {
                Icon(Icons.Default.Tune, contentDescription = "Graph settings", tint = colors.onSurface)
            }
        })
        if (graphSettings && view != null) GraphSettingsDialog(vm, view, onDismiss = { graphSettings = false })
        // The point's coordinates, with buttons to use x or y (or r and θ) in the calculator.
        val t = trace
        if (t != null && view != null) {
            val px = ((t.x - view.xMin) / view.width * size.width).toFloat()
            val py = ((view.yMax - t.y) / view.height * size.height).toFloat()
            val areaFunction = vm.functions.firstOrNull { it.colorIndex == t.colorIndex && it.plot is Plot2DKind.Explicit }
            // At a crossing of two functions of x: the area between them, from here.
            val otherFunction = t.other?.let { o -> vm.functions.firstOrNull { it.colorIndex == o && it.plot is Plot2DKind.Explicit } }
            val line = vm.functions.firstOrNull { it.colorIndex == t.colorIndex && it.visible && !it.isText }
            val degrees = vm.angle == com.example.cas.engine.AngleUnit.Degrees
            // Points are kept in the view's coordinates (log₁₀ on a log axis): read them as values.
            val sc = vm.scale
            val tx = sc.realX(t.x); val ty = sc.realY(t.y)
            // With the polar grid on, points read as (r, θ).
            val r = kotlin.math.hypot(tx, ty)
            var theta = kotlin.math.atan2(ty, tx)
            if (theta < 0) theta += 2 * PI
            val shownTheta = if (degrees) theta * 180 / PI else theta
            // Over a field, its value there as a third row.
            val fieldValue = t.label.takeIf { it.startsWith("value ") }?.removePrefix("value ")?.replace("−", "-")?.toDoubleOrNull()
            // Over a vector field: F there, and |F|.
            val vectorValue = t.label.takeIf { it.startsWith("vector ") }?.removePrefix("vector ")?.split(" ")?.mapNotNull { it.toDoubleOrNull() }?.takeIf { it.size == 2 }
            fun use(v: Double): () -> Unit = { trace = null; onUseValue(v) }
            val rows = (if (vm.polarGrid && sc.linear) listOf(CardValue("r", shortNumber(r), onUse = use(r)), CardValue("θ", shortNumber(shownTheta) + if (degrees) "°" else "", onUse = use(shownTheta)))
                else listOf(CardValue("x", shortNumber(tx), onUse = use(tx)), CardValue("y", shortNumber(ty), onUse = use(ty)))) +
                listOfNotNull(fieldValue?.let { CardValue("f", shortNumber(it), onUse = use(it)) }) +
                (vectorValue?.let { (p, q) -> val m = kotlin.math.hypot(p, q); listOf(CardValue("P", shortNumber(p), onUse = use(p)), CardValue("Q", shortNumber(q), onUse = use(q)), CardValue("|F|", shortNumber(m), onUse = use(m))) } ?: emptyList())
            val kind = when {
                fieldValue != null -> "Value"
                vectorValue != null -> "Vector"
                t.label == "point" || t.label.isEmpty() -> null
                t.label == "y-intercept" -> "y-intercept"
                t.label == "start" -> "Starting point"
                else -> t.label.replaceFirstChar { it.uppercase() }
            }
            val actions = listOfNotNull(
                // Area: this point is where it starts; the next tap on the graph is where it ends.
                areaFunction?.let { f -> CardAction(TabIcons.Area, if (otherFunction != null) "Area" else "Area from here", "Area from here") { vm.clearArea(); vm.areaStart = Graph2DViewModel.AreaStart(f, null, tx); trace = null } },
                // Area between the two curves that cross here.
                if (areaFunction != null && otherFunction != null) CardAction(TabIcons.AreaBetween, "Between curves", "Area between the curves from here") { vm.clearArea(); vm.areaStart = Graph2DViewModel.AreaStart(areaFunction, otherFunction, tx); trace = null } else null,
                // The tangent and normal here, added as lines; the arc's length to where you tap next.
                areaFunction?.let { f -> CardAction(Icons.AutoMirrored.Filled.TrendingUp, "Tangent", "Add the tangent line here") { addTangent(vm, f, tx, normal = false); trace = null } },
                areaFunction?.let { f -> CardAction(Icons.Default.North, "Normal", "Add the normal line here") { addTangent(vm, f, tx, normal = true); trace = null } },
                areaFunction?.let { f -> CardAction(Icons.Default.Straighten, "Arc length", "Arc length from here") { vm.clearArea(); vm.areaStart = Graph2DViewModel.AreaStart(f, null, tx, arc = true); trace = null } },
                // On a slope field's solution: remove it, or all of them.
                line?.takeIf { it.plot is Plot2DKind.SlopeField && it.seeds.isNotEmpty() }?.let { f -> CardAction(Icons.Default.Close, "Remove", "Remove this solution") { vm.removeSeedNear(f, tx, ty); trace = null } },
                line?.takeIf { it.plot is Plot2DKind.SlopeField && it.seeds.size > 1 }?.let { f -> CardAction(Icons.Default.ClearAll, "Clear all", "Remove every solution") { vm.clearSeeds(f); trace = null } },
            )
            PointCardAt(px, py, palette[t.colorIndex], kind, line?.let { legendSource(it) }?.takeIf { it.isNotBlank() }, rows, actions, onClose = { trace = null })
        }
    }
}

/**
 * Samples every visible curve. Zeros, extrema, the y-intercept and crossings
 * with other curves are found only for the highlighted curve, so the graph
 * doesn't fill up with dots.
 */
/**
 * Samples every visible line by its kind. Zeros, extrema, the y-intercept and
 * crossings are found for the highlighted function of x, so the graph doesn't
 * fill up with dots.
 */
private fun plot(vm: Graph2DViewModel, view: Viewport, size: IntSize, highlighted: PlotFunction?): List<Plotted> {
    val fns = vm.functions.filter { it.visible && (it.plot != null || it.family.isNotEmpty()) }
    val samples = (size.width / 2).coerceIn(200, 900)
    // The view is in scaled coordinates (log₁₀ on a log axis); lines are worked out in values and
    // placed by [sc]. Points and lines below are all in scaled coordinates.
    val sc = vm.scale
    val realView = sc.realView(view)
    fun at(x: Double, y: Double) = (sc.x(x) to sc.y(y)).takeIf { it.first.isFinite() && it.second.isFinite() }
    val nx = (size.width / 6).coerceIn(40, 240)
    val ny = (size.height / 6).coerceIn(40, 320)
    val out = ArrayList<Plotted>()
    // A line with a list in it is drawn as each of its members (g), in the line's own style (f).
    for (f in fns) for (g in f.family.ifEmpty { listOf(f) }) {
        val k = g.plot ?: continue
        when (k) {
            is Plot2DKind.Geometry -> {
                val o = vm.geometryOf(g) ?: continue
                val d = com.example.cas.graph.Geometry.draw(o, realView) { vm.angleText(it) }
                val pts = d.points.mapNotNull { p -> at(p.x, p.y)?.let { Special(it.first, it.second, "point", f.colorIndex) } }
                val labels = ArrayList<GeoLabel>()
                val name = g.geometry?.name
                if (name != null && !g.hideName && o is com.example.cas.graph.Geometry.Point) pts.firstOrNull()?.let { labels += GeoLabel(it.x, it.y, geometryLabel(name), false) }
                d.labels.forEach { (p, t) -> at(p.x, p.y)?.let { labels += GeoLabel(it.first, it.second, t, true) } }
                out += Plotted(f, sc.paths(d.lines), pts, fill = d.fill?.mapNotNull { at(it.first, it.second) }, arrow = d.arrow, labels = labels)
            }
            is Plot2DKind.Vector -> {
                val x = vm.call(g, k.x); val y = vm.call(g, k.y)
                val x0 = k.fromX?.let { vm.call(g, it) } ?: 0.0
                val y0 = k.fromY?.let { vm.call(g, it) } ?: 0.0
                val a = at(x0, y0); val b = at(x, y)
                if (a != null && b != null) out += Plotted(f, listOf(listOf(a, b)), emptyList(), arrow = true)
            }
            is Plot2DKind.SlopeField -> {
                val slope = vm.caller2(g, k.f)
                // The marks are laid out in the view's coordinates: on a log axis the slope becomes
                // dv/du = f · (dx/du) / (dy/dv).
                val ln10 = kotlin.math.ln(10.0)
                val marks = com.example.cas.graph.SlopeField.segments({ u, w ->
                    val x = sc.realX(u); val y = sc.realY(w)
                    slope(x, y) * (if (sc.logX) x * ln10 else 1.0) / (if (sc.logY) y * ln10 else 1.0)
                }, view, size.width.toDouble(), size.height.toDouble(), spacing = size.width / (f.arrowDensity * 1.8))
                // A solution through each starting point, worked out in values.
                val solutions = f.seeds.flatMap { (x0, y0) -> sc.paths(com.example.cas.graph.SlopeField.solution(slope, x0, y0, realView)) }
                val starts = f.seeds.mapNotNull { (x0, y0) -> at(x0, y0)?.let { Special(it.first, it.second, "start", f.colorIndex) } }
                out += Plotted(f, solutions, starts, segments = marks, slopeMarks = true)
            }
            is Plot2DKind.VectorField -> {
                val p = vm.caller2(g, k.p); val q = vm.caller2(g, k.q); val ok = vm.allowedCaller(g)
                // On a log axis the arrows are laid out in the view's coordinates: a component along it
                // becomes d(log₁₀ x) = dx / (x ln 10).
                val arrows = com.example.cas.graph.VectorField.arrows(
                    { u, w ->
                        val x = sc.realX(u); val y = sc.realY(w)
                        if (ok(x, y)) (p(x, y) / (if (sc.logX) x * kotlin.math.ln(10.0) else 1.0)) to (q(x, y) / (if (sc.logY) y * kotlin.math.ln(10.0) else 1.0)) else null
                    }, view, size.width.toDouble(), size.height.toDouble(),
                    f.arrowDensity, com.example.cas.graph.VectorField.Length.entries[f.arrowLength], f.arrowScale.toDouble(),
                )
                out += Plotted(f, emptyList(), emptyList(), arrows = arrows, arrowRange = com.example.cas.graph.VectorField.range(arrows))
            }
            is Plot2DKind.PointList -> {
                val shown = k.xs.indices.filter { at(k.xs[it], k.ys[it]) != null }
                val pts = shown.map { val (u, w) = at(k.xs[it], k.ys[it])!!; Special(u, w, "point", f.colorIndex) }
                // σ(x) and σ(y) from the line's table, when columns were picked for them.
                val (ex, ey) = if (g === f) vm.errorsOf(f) else null to null
                val errors = if (ex == null && ey == null) emptyList() else shown.map { i ->
                    doubleArrayOf(k.xs[i], k.ys[i], ex?.getOrNull(i) ?: Double.NaN, ey?.getOrNull(i) ?: Double.NaN)
                }
                // "Join the points": a line through them in order; a closed shape also joins the last to
                // the first and is filled, as a polygon.
                val path = pts.map { it.x to it.y }
                val joined = when {
                    f.closedShape && pts.size > 2 -> listOf(path + path.first())
                    f.connectPoints && pts.size > 1 -> listOf(path)
                    else -> emptyList()
                }
                out += Plotted(f, joined, pts, fill = if (f.closedShape && pts.size > 2) path else null, errors = errors)
            }
            is Plot2DKind.Point -> {
                val px = vm.call(g, k.x); val py = vm.call(g, k.y)
                val pt = at(px, py)
                if (pt != null && vm.allowed(g, px, py)) out += Plotted(f, emptyList(), listOf(Special(pt.first, pt.second, "point", f.colorIndex)))
            }
            is Plot2DKind.Explicit -> {
                // Conditions after commas (0 < x < 2) leave gaps where they fail.
                // In scaled coordinates: u ↦ (scaled) f(x) at x = realX(u).
                val fx = sc.function { x: Double -> vm.call(g, k.f, x).let { y -> if (vm.allowed(g, x, y)) y else Double.NaN } }
                val points = ArrayList<Special>()
                if (f === highlighted && AppSettings.specialPoints) {
                    // Zeros and the y-intercept have no place on a log axis (y = 0, x = 0); extrema keep theirs.
                    if (!sc.logY) Plot2D.zeros(fx, view.xMin, view.xMax, 400).forEach { points += Special(it, 0.0, "zero", f.colorIndex) }
                    Plot2D.extrema(fx, view.xMin, view.xMax, 400).forEach { (x, kind) ->
                        points += Special(x, fx(x), if (kind == Plot2D.Kind.Maximum) "maximum" else "minimum", f.colorIndex)
                    }
                    val y0 = if (sc.logX) Double.NaN else fx(0.0)
                    if (y0.isFinite() && view.xMin < 0 && view.xMax > 0) points += Special(0.0, y0, "y-intercept", f.colorIndex)
                }
                out += Plotted(f, Plot2D.sample(fx, view, samples), points)
            }
            is Plot2DKind.Polar -> {
                val r = { t: Double -> vm.call(g, k.r, t).let { rr -> if (vm.allowed(g, rr * kotlin.math.cos(t), rr * kotlin.math.sin(t), theta = t, r = rr)) rr else Double.NaN } }
                out += Plotted(f, sc.paths(Curves.polar(r, realView, turns = periodTurns(listOf(r)).toDouble())), emptyList())
            }
            is Plot2DKind.Parametric -> {
                val x = { t: Double -> vm.call(g, k.x, t).let { xx -> if (vm.allowed(g, xx, vm.call(g, k.y, t), t = t)) xx else Double.NaN } }
                val y = { t: Double -> vm.call(g, k.y, t) }
                // Closed curves go round once; open ones like (t, t²) run over −10 ≤ t ≤ 10.
                val turns = periodTurns(listOf(x, y))
                val (t0, t1) = if (turns <= 6) 0.0 to 2 * PI * turns else -10.0 to 10.0
                out += Plotted(f, sc.paths(Curves.parametric(x, y, t0, t1, realView, Curves.samplesFor(t0, t1))), emptyList())
            }
            is Plot2DKind.Implicit -> {
                // Sliders read once for the whole grid.
                val g0 = vm.caller2(g, k.f); val ok = vm.allowedCaller(g)
                val g = sc.function2 { x: Double, y: Double -> if (ok(x, y)) g0(x, y) else Double.NaN }
                out += Plotted(f, emptyList(), emptyList(), segments = Curves.implicit(g, view, nx, ny))
            }
            is Plot2DKind.Field -> {
                // Colored cells 12, 6 or 3 px square (the field quality setting; low by default, as
                // every cell is worked out again on each pan); the scale follows the values in view.
                val cell = when (AppSettings.fieldQuality) { 2 -> 3; 1 -> 6; else -> 12 }
                val fx = (size.width / cell).coerceIn(24, 420); val fy = (size.height / cell).coerceIn(24, 560)
                val h = vm.caller2(g, k.f); val ok = vm.allowedCaller(g)
                val values = com.example.cas.graph.Field.sample(sc.function2 { x: Double, y: Double -> if (ok(x, y)) h(x, y) else Double.NaN }, view, fx, fy)
                val range = com.example.cas.graph.Field.range(values)
                val px = com.example.cas.graph.Field.colors(values, range.first, range.second, f.colormap, f.colormapReversed)
                val image = runCatching { android.graphics.Bitmap.createBitmap(px, fx, fy, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap() }.getOrNull()
                out += Plotted(f, emptyList(), emptyList(), field = px, fieldSize = IntSize(fx, fy), fieldRange = range, fieldImage = image)
            }
            is Plot2DKind.Region -> {
                val values = DoubleArray(k.parts.size)
                val parts = k.parts.map { vm.caller2(g, it) }
                val ok = vm.allowedCaller(g)
                val test = { u: Double, w: Double ->
                    val x = sc.realX(u); val y = sc.realY(w)
                    for (p in parts.indices) values[p] = parts[p](x, y)
                    Curves.holds(values, k.ops) && ok(x, y)
                }
                // A 4 px grid for the shading, so its edge isn't visibly stepped; tested cell by cell
                // only near the edge (elsewhere 4 × 4 cells share one test).
                val mx = (size.width / 4).coerceIn(40, 360); val my = (size.height / 4).coerceIn(40, 480)
                val mask = Curves.regionAdaptive(test, view, mx, my)
                // The edge: where each compared pair is equal.
                val edges = k.ops.indices.flatMap { p ->
                    val a = parts[p]; val b = vm.caller2(g, k.parts[p + 1])
                    Curves.implicit(sc.function2 { x, y -> a(x, y) - b(x, y) }, view, nx, ny)
                }
                out += Plotted(f, emptyList(), emptyList(), segments = edges, mask = mask, maskSize = IntSize(mx, my), dashed = k.ops.all { it == "<" || it == ">" })
            }
        }
    }
    // Where two functions of x cross.
    val explicit = fns.filter { it.plot is Plot2DKind.Explicit && it.family.isEmpty() }
    val crossings = ArrayList<Special>()
    for (a in explicit.indices) for (b in a + 1 until explicit.size) {
        if (!AppSettings.specialPoints || (explicit[a] !== highlighted && explicit[b] !== highlighted)) continue
        val fa = (explicit[a].plot as Plot2DKind.Explicit).f
        val fb = (explicit[b].plot as Plot2DKind.Explicit).f
        val d = { u: Double -> val x = sc.realX(u); vm.call(explicit[a], fa, x) - vm.call(explicit[b], fb, x) }
        Plot2D.zeros(d, view.xMin, view.xMax, 400).forEach { u ->
            val y = sc.y(vm.call(explicit[a], fa, sc.realX(u)))
            if (y.isFinite()) crossings += Special(u, y, "intersection", explicit[a].colorIndex, explicit[b].colorIndex)
        }
    }
    if (crossings.isNotEmpty() && out.isNotEmpty()) out += Plotted(out.first().f, emptyList(), crossings)
    return out
}

/** How many turns of 2π until the curve repeats (1 to 6), or 7 if it doesn't within 12π. */
private fun periodTurns(fs: List<(Double) -> Double>): Int {
    val probe = DoubleArray(24) { 0.3 + it * 0.26 }
    for (k in 1..6) {
        val shift = 2 * PI * k
        if (probe.all { t -> fs.all { g -> val a = g(t); val b = g(t + shift); !a.isFinite() && !b.isFinite() || abs(a - b) <= 1e-7 * (1 + abs(a)) } }) return k
    }
    return 7
}

private fun DrawScope.toScreen(v: Viewport, x: Double, y: Double) =
    Offset(((x - v.xMin) / v.width * size.width).toFloat(), ((v.yMax - y) / v.height * size.height).toFloat())

/**
 * The grid, axes and numbers, in the theme's colors (shared by the 2D graph and the complex
 * plane: [ySuffix] "i" labels the imaginary axis, and [halo] keeps numbers readable over color).
 */
internal fun DrawScope.drawGrid(v: Viewport, gridColor: Color, axisColor: Color, measurer: TextMeasurer, ySuffix: String = "", halo: Color? = null, scale: com.example.cas.graph.AxisScale = com.example.cas.graph.AxisScale()) {
    val targetX = (size.width / 90.dp.toPx()).toInt().coerceAtLeast(3)
    val targetY = (size.height / 90.dp.toPx()).toInt().coerceAtLeast(3)
    // Each axis's ticks, linear or log (see AxisScale.ticks).
    val tx = scale.ticks(v.xMin, v.xMax, targetX, scale.logX)
    val ty = scale.ticks(v.yMin, v.yMax, targetY, scale.logY)
    val minor = gridColor.copy(alpha = 0.35f)
    val major = gridColor.copy(alpha = 0.9f)
    for (x in tx.minor) {
        val sx = toScreen(v, x, 0.0).x
        drawLine(minor, Offset(sx, 0f), Offset(sx, size.height), 1f)
    }
    for (y in ty.minor) {
        val sy = toScreen(v, 0.0, y).y
        drawLine(minor, Offset(0f, sy), Offset(size.width, sy), 1f)
    }
    tx.major.forEach { x -> val sx = toScreen(v, x, 0.0).x; drawLine(major, Offset(sx, 0f), Offset(sx, size.height), 1.2f) }
    ty.major.forEach { y -> val sy = toScreen(v, 0.0, y).y; drawLine(major, Offset(0f, sy), Offset(size.width, sy), 1.2f) }
    // Axes, and labels that stay on screen when an axis scrolls away. A log axis has no 0, so the
    // other axis isn't drawn and its numbers sit along the edge.
    val origin = toScreen(v, 0.0, 0.0)
    val axisW = 2.dp.toPx()
    if (!scale.logY && origin.y in 0f..size.height) drawLine(axisColor, Offset(0f, origin.y), Offset(size.width, origin.y), axisW)
    if (!scale.logX && origin.x in 0f..size.width) drawLine(axisColor, Offset(origin.x, 0f), Offset(origin.x, size.height), axisW)
    // Numbers along the axes can be turned off in the graph's settings.
    if (!AppSettings.axisNumbers) return
    val style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 11.sp, color = axisColor, shadow = halo?.let { androidx.compose.ui.graphics.Shadow(it, blurRadius = 5f) })
    fun text(label: Pair<String, String?>, suffix: String = "") = androidx.compose.ui.text.buildAnnotatedString {
        append(label.first)
        label.second?.let { e -> withStyle(androidx.compose.ui.text.SpanStyle(fontSize = 8.sp, baselineShift = androidx.compose.ui.text.style.BaselineShift(0.45f))) { append(e) } }
        append(suffix)
    }
    val stepX = Plot2D.niceStep(v.width, targetX)
    val stepY = Plot2D.niceStep(v.height, targetY)
    val labelY = if (scale.logY) size.height - 16.dp.toPx() else pinInside(origin.y, 2.dp.toPx(), size.height - 16.dp.toPx())
    tx.major.forEachIndexed { k, x ->
        // 0 is left out where the axes cross (both linear).
        if (!scale.logX && !scale.logY && abs(x) <= stepX / 2) return@forEachIndexed
        val t = measurer.measure(text(tx.labels[k]), style)
        val sx = toScreen(v, x, 0.0).x
        drawText(t, topLeft = Offset(sx - t.size.width / 2f, labelY + 3.dp.toPx()))
    }
    val labelX = if (scale.logX) 2.dp.toPx() else pinInside(origin.x, 2.dp.toPx(), size.width - 40.dp.toPx())
    ty.major.forEachIndexed { k, y ->
        if (!scale.logX && !scale.logY && abs(y) <= stepY / 2) return@forEachIndexed
        val t = measurer.measure(text(ty.labels[k], ySuffix), style)
        val sy = toScreen(v, 0.0, y).y
        drawText(t, topLeft = Offset(labelX + 4.dp.toPx(), sy - t.size.height / 2f))
    }
}

/** Circles of constant r and rays every 30°, labeled in radians (π/6…) or degrees. */
internal fun DrawScope.drawPolarGrid(v: Viewport, gridColor: Color, axisColor: Color, measurer: TextMeasurer, degrees: Boolean, halo: Color? = null) {
    val origin = toScreen(v, 0.0, 0.0)
    val corners = listOf(v.xMin to v.yMin, v.xMin to v.yMax, v.xMax to v.yMin, v.xMax to v.yMax)
    val far = corners.maxOf { (x, y) -> kotlin.math.hypot(x, y) }
    val step = Plot2D.niceStep(minOf(v.width, v.height) / 2, 4)
    val pxPerUnit = size.width / v.width.toFloat()
    val minor = gridColor.copy(alpha = 0.35f)
    val major = gridColor.copy(alpha = 0.9f)
    var r = step / 2
    var k = 1
    while (r <= far + step) {
        drawCircle(if (k % 2 == 0) major else minor, radius = (r * pxPerUnit).toFloat(), center = origin, style = Stroke(if (k % 2 == 0) 1.2f else 1f))
        r += step / 2; k++
    }
    val reach = (far * pxPerUnit).toFloat() + size.width
    for (a in 0 until 12) {
        val ang = a * PI / 6
        val end = Offset(origin.x + (reach * kotlin.math.cos(ang)).toFloat(), origin.y - (reach * kotlin.math.sin(ang)).toFloat())
        drawLine(if (a % 3 == 0) axisColor else major, origin, end, if (a % 3 == 0) 2.dp.toPx() else 1.2f)
    }
    // Radius labels along the positive x-axis, angle labels around a circle that fits on screen.
    val style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 11.sp, color = axisColor, shadow = halo?.let { androidx.compose.ui.graphics.Shadow(it, blurRadius = 5f) })
    var lr = step
    while (lr <= far) {
        val t = measurer.measure(Plot2D.label(lr, step), style)
        val at = Offset(origin.x + (lr * pxPerUnit).toFloat(), origin.y)
        if (at.x in 0f..size.width && at.y in 0f..size.height) drawText(t, topLeft = Offset(at.x - t.size.width / 2f, at.y + 3.dp.toPx()))
        lr += step
    }
    val names = listOf("0", "π/6", "π/3", "π/2", "2π/3", "5π/6", "π", "7π/6", "4π/3", "3π/2", "5π/3", "11π/6")
    val labelR = minOf(origin.x, size.width - origin.x, origin.y, size.height - origin.y) - 18.dp.toPx()
    if (labelR > 40.dp.toPx()) for (a in 0 until 12) {
        val ang = a * PI / 6
        val t = measurer.measure(if (degrees) "${a * 30}°" else names[a], style)
        val c = Offset(origin.x + (labelR * kotlin.math.cos(ang)).toFloat(), origin.y - (labelR * kotlin.math.sin(ang)).toFloat())
        drawText(t, topLeft = Offset(c.x - t.size.width / 2f, c.y - t.size.height / 2f))
    }
}

/** A point's mark: the shape's outlines filled (the cross stroked), [r] pixels across from the center. */
/** Error bars: a line from y − σy to y + σy (and x − σx to x + σx), with short caps at the ends. */
private fun DrawScope.drawErrorBars(v: Viewport, errors: List<DoubleArray>, color: Color, sc: com.example.cas.graph.AxisScale) {
    if (errors.isEmpty()) return
    // The bars are in values; on a log axis each end goes where its value is (so they're lopsided).
    fun toScreen(v: Viewport, x: Double, y: Double) = Offset(((sc.x(x) - v.xMin) / v.width * size.width).toFloat(), ((v.yMax - sc.y(y).let { if (it.isFinite()) it else v.yMin - v.height }) / v.height * size.height).toFloat())
    val cap = 4.dp.toPx()
    val w = 1.5.dp.toPx()
    for (e in errors) {
        val (x, y, sx, sy) = e.toList()
        if (sy.isFinite() && sy > 0) {
            val a = toScreen(v, x, y - sy); val b = toScreen(v, x, y + sy)
            drawLine(color, a, b, w)
            drawLine(color, Offset(a.x - cap, a.y), Offset(a.x + cap, a.y), w)
            drawLine(color, Offset(b.x - cap, b.y), Offset(b.x + cap, b.y), w)
        }
        if (sx.isFinite() && sx > 0) {
            val a = toScreen(v, x - sx, y); val b = toScreen(v, x + sx, y)
            drawLine(color, a, b, w)
            drawLine(color, Offset(a.x, a.y - cap), Offset(a.x, a.y + cap), w)
            drawLine(color, Offset(b.x, b.y - cap), Offset(b.x, b.y + cap), w)
        }
    }
}

internal fun DrawScope.drawMarker(marker: com.example.cas.graph.Marker, center: Offset, r: Float, color: Color) {
    val (fills, strokes) = marker.outline(center.x.toDouble(), center.y.toDouble(), r.toDouble())
    if (fills.isNotEmpty()) {
        val path = Path()
        fills.forEach { pts ->
            path.moveTo(pts[0].toFloat(), pts[1].toFloat())
            var k = 2
            while (k + 1 < pts.size) { path.lineTo(pts[k].toFloat(), pts[k + 1].toFloat()); k += 2 }
            path.close()
        }
        drawPath(path, color)
    }
    strokes.forEach { sg -> drawLine(color, Offset(sg[0].toFloat(), sg[1].toFloat()), Offset(sg[2].toFloat(), sg[3].toFloat()), r / 2.5f, cap = StrokeCap.Round) }
}

/** The arrowhead at the end of a vector. */
private fun DrawScope.drawArrowHead(from: Offset, to: Offset, color: Color, width: Float) {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val len = kotlin.math.hypot(dx, dy)
    if (len < 1f) return
    val ux = dx / len
    val uy = dy / len
    val size = (width * 4f).coerceAtLeast(10f)
    val base = Offset(to.x - ux * size, to.y - uy * size)
    val head = Path().apply {
        moveTo(to.x, to.y)
        lineTo(base.x - uy * size * 0.45f, base.y + ux * size * 0.45f)
        lineTo(base.x + uy * size * 0.45f, base.y - ux * size * 0.45f)
        close()
    }
    drawPath(head, color)
}

/** A vector field's arrows, each in the line's color or its |F| along the colormap. */
private fun DrawScope.drawArrows(v: Viewport, p: Plotted, color: Color) {
    val f = p.f
    val w = (f.thickness * 0.5f).dp.toPx().coerceAtLeast(1f)
    val tip = com.example.cas.graph.VectorField.Tip.entries[f.arrowTip]
    val headSize = (4.5f + f.thickness).dp.toPx() * f.arrowTipSize
    for (a in p.arrows) {
        val c = if (f.arrowsByLength) Color(0xFF000000.toInt() or f.colormap.rgb(com.example.cas.graph.VectorField.position(a.magnitude, p.arrowRange).let { if (f.colormapReversed) 1 - it else it })) else color
        val from = toScreen(v, a.x0, a.y0); val to = toScreen(v, a.x1, a.y1)
        val len = (to - from).getDistance()
        if (len < 0.5f) { drawCircle(c, w, to); continue }
        val ux = (to.x - from.x) / len; val uy = (to.y - from.y) / len
        val h = com.example.cas.graph.VectorField.head(tip, to.x.toDouble(), to.y.toDouble(), ux.toDouble(), uy.toDouble(), minOf(headSize, len * 0.6f).toDouble())
        drawLine(c, from, Offset(h.shaftEndX.toFloat(), h.shaftEndY.toFloat()), w, cap = StrokeCap.Round)
        h.fills.forEach { pts ->
            val path = Path()
            path.moveTo(pts[0].toFloat(), pts[1].toFloat())
            var k = 2
            while (k + 1 < pts.size) { path.lineTo(pts[k].toFloat(), pts[k + 1].toFloat()); k += 2 }
            path.close()
            drawPath(path, c)
        }
        h.strokes.forEach { sg -> drawLine(c, Offset(sg[0].toFloat(), sg[1].toFloat()), Offset(sg[2].toFloat(), sg[3].toFloat()), w, cap = StrokeCap.Round) }
        h.dot?.let { d -> drawCircle(c, d[2].toFloat().coerceAtLeast(w), Offset(d[0].toFloat(), d[1].toFloat())) }
    }
}

private fun DrawScope.drawCurve(v: Viewport, p: Plotted, color: Color) {
    if (p.arrows.isNotEmpty()) drawArrows(v, p, color)
    // The function's own thickness and style (long-press its dot): solid, dashed, dotted…
    val w = p.f.thickness.dp.toPx()
    val style = com.example.cas.graph.LineStyle.of(p.f.lineStyle).pattern(w.toDouble())?.let { d -> PathEffect.dashPathEffect(FloatArray(d.size) { d[it].toFloat() }) }
    val stroke = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = style)
    // Shaded region: runs of inside cells along each row.
    p.mask?.let { mask ->
        val (mx, my) = p.maskSize
        val cw = size.width / mx; val ch = size.height / my
        // One path for all the runs, filled once, so touching cells don't double up into stripes.
        val area = Path()
        for (j in 0 until my) {
            var i = 0
            while (i < mx) {
                if (!mask[j * mx + i]) { i++; continue }
                val start = i
                while (i < mx && mask[j * mx + i]) i++
                area.addRect(androidx.compose.ui.geometry.Rect(start * cw, j * ch, i * cw + 0.5f, (j + 1) * ch + 0.5f))
            }
        }
        drawPath(area, color.copy(alpha = p.f.fillOpacity))
    }
    p.fill?.let { poly ->
        val area = Path()
        poly.forEachIndexed { i, (x, y) -> val o = toScreen(v, x, y); if (i == 0) area.moveTo(o.x, o.y) else area.lineTo(o.x, o.y) }
        area.close()
        drawPath(area, color.copy(alpha = p.f.fillOpacity))
    }
    if (p.segments.isNotEmpty() && p.slopeMarks) {
        val path = Path()
        p.segments.forEach { s -> val a = toScreen(v, s[0], s[1]); val b = toScreen(v, s[2], s[3]); path.moveTo(a.x, a.y); path.lineTo(b.x, b.y) }
        drawPath(path, color.copy(alpha = 0.55f), style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round))
    } else if (p.segments.isNotEmpty()) {
        val segW = if (p.mask != null) 2.dp.toPx() else w
        val dash = if (p.dashed) PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())) else style
        val path = Path()
        p.segments.forEach { s ->
            val a = toScreen(v, s[0], s[1]); val b = toScreen(v, s[2], s[3])
            path.moveTo(a.x, a.y); path.lineTo(b.x, b.y)
        }
        drawPath(path, color, style = Stroke(segW, cap = StrokeCap.Round, pathEffect = dash))
    }
    p.lines.forEach { line ->
        val path = Path()
        line.forEachIndexed { i, (x, y) ->
            val o = toScreen(v, x, y)
            if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
        }
        drawPath(path, color, style = stroke)
        // A vector's arrowhead at its far end.
        if (p.arrow && line.size >= 2) {
            drawArrowHead(toScreen(v, line[line.size - 2].first, line[line.size - 2].second), toScreen(v, line.last().first, line.last().second), color, w)
        }
    }
}

/**
 * The 2D graph over [view] as a square [Scene] [size] units across, drawn like pgfplots: a
 * framed plot with inward ticks and Computer Modern labels (see [Pgf]), pgfplots' colors for
 * lines without a picked color, light or [dark]. Shaded areas, curves, points and vectors are
 * as on screen; found points (zeros, extrema) and the tapped point are left out.
 */
internal fun graph2DScene(vm: Graph2DViewModel, view: Viewport, size: Double, dark: Boolean): Scene {
    val style = if (dark) Pgf.Style.DARK else Pgf.Style.LIGHT
    val scene = Scene(size, size, style.background)
    val v = view
    val frame = Pgf.frame(size)
    fun sx(x: Double) = Pgf.sx(v, frame, x)
    fun sy(y: Double) = Pgf.sy(v, frame, y)
    fun withAlpha(c: Int, a: Float) = (((c ushr 24) * a).toInt().coerceIn(0, 255) shl 24) or (c and 0xFFFFFF)
    // The app's colors are for the screen: exported, the lines take SciencePlots' colors in turn,
    // in the order of the list.
    val drawn = vm.functions.filter { it.visible && (it.plot != null || it.family.isNotEmpty()) }
    fun colorOf(f: PlotFunction): Int = style.cycle[(drawn.indexOf(f).coerceAtLeast(0)) % style.cycle.size]
    val sc = vm.scale
    val polar = vm.polarGrid && sc.linear
    if (AppSettings.showGrid && !polar) Pgf.grid(scene, v, frame, style, sc)
    scene.add(Scene.ClipStart(frame.left, frame.top, frame.width, frame.height))
    if (polar) {
        // Circles of constant r and rays every 30°, light like the grid.
        val ox = sx(0.0); val oy = sy(0.0)
        val corners = listOf(v.xMin to v.yMin, v.xMin to v.yMax, v.xMax to v.yMin, v.xMax to v.yMax)
        val far = corners.maxOf { (x, y) -> kotlin.math.hypot(x, y) }
        val step = Plot2D.niceStep(minOf(v.width, v.height) / 2, 4)
        val perUnitX = frame.width / v.width; val perUnitY = frame.height / v.height
        val circles = ArrayList<DoubleArray>()
        var r = step
        while (r <= far) {
            circles += DoubleArray(2 * 97) { k -> val t = (k / 2) * 2 * PI / 96; if (k % 2 == 0) ox + r * perUnitX * kotlin.math.cos(t) else oy - r * perUnitY * kotlin.math.sin(t) }
            r += step
        }
        val reach = far * maxOf(perUnitX, perUnitY) + size
        val rays = (0 until 12).map { a -> val ang = a * PI / 6; doubleArrayOf(ox, oy, ox + reach * kotlin.math.cos(ang), oy - reach * kotlin.math.sin(ang)) }
        scene.add(Scene.Stroke(circles + rays, style.grid, 0.5))
    }
    // The area picked with Area (or between two curves), shaded.
    vm.area?.let { ar ->
        if (ar.arc && !ar.signed.isNaN()) arcPath(vm, ar, sc).let { pts ->
            scene.add(Scene.Stroke(listOf(DoubleArray(pts.size * 2) { k -> if (k % 2 == 0) sx(pts[k / 2].first) else sy(pts[k / 2].second) }), withAlpha(colorOf(ar.f), 0.35f), 5.0))
        } else if (!ar.signed.isNaN()) areaOutline(vm, ar, v, sc)?.let { poly ->
            val pts = DoubleArray(poly.size * 2) { k -> if (k % 2 == 0) sx(poly[k / 2].first) else sy(poly[k / 2].second) }
            scene.add(Scene.Fill(listOf(pts), withAlpha(colorOf(ar.f), 0.25f)))
        }
    }
    // Sampled finely: three samples per unit of width.
    val plotted = runCatching { plot(vm, v, IntSize((frame.width * 3).toInt().coerceAtLeast(1), (frame.height * 3).toInt().coerceAtLeast(1)), null) }.getOrElse { emptyList() }
    // Fields first, under every line.
    for (p in plotted.asReversed()) p.field?.let { px -> scene.add(Scene.Image(frame.left, frame.top, frame.width, frame.height, px, p.fieldSize.width, p.fieldSize.height)) }
    // The first line in the list last, on top, as on screen.
    for (p in plotted.asReversed()) {
        val color = colorOf(p.f)
        // pgfplots' "thick" for the usual 3 dp line, scaled with the line's own thickness.
        val lw = p.f.thickness * 0.5
        val dash = com.example.cas.graph.LineStyle.of(p.f.lineStyle).pattern(lw)
        p.mask?.let { mask ->
            val mx = p.maskSize.width; val my = p.maskSize.height
            val cw = frame.width / mx; val ch = frame.height / my
            val rects = ArrayList<DoubleArray>()
            for (j in 0 until my) {
                var i = 0
                while (i < mx) {
                    if (!mask[j * mx + i]) { i++; continue }
                    val start = i
                    while (i < mx && mask[j * mx + i]) i++
                    val x0 = frame.left + start * cw; val x1 = frame.left + i * cw + 0.25
                    val y0 = frame.top + j * ch; val y1 = frame.top + (j + 1) * ch + 0.25
                    rects += doubleArrayOf(x0, y0, x1, y0, x1, y1, x0, y1)
                }
            }
            scene.add(Scene.Fill(rects, withAlpha(color, p.f.fillOpacity)))
        }
        p.fill?.let { poly -> scene.add(Scene.Fill(listOf(poly.flatMap { (x, y) -> listOf(sx(x), sy(y)) }.toDoubleArray()), withAlpha(color, p.f.fillOpacity))) }
        if (p.segments.isNotEmpty() && p.slopeMarks) {
            scene.add(Scene.Stroke(p.segments.map { sg -> doubleArrayOf(sx(sg[0]), sy(sg[1]), sx(sg[2]), sy(sg[3])) }, withAlpha(color, 0.6f), 0.7))
        } else if (p.segments.isNotEmpty()) {
            val segW = if (p.mask != null) 1.0 else lw
            scene.add(Scene.Stroke(p.segments.map { sg -> doubleArrayOf(sx(sg[0]), sy(sg[1]), sx(sg[2]), sy(sg[3])) }, color, segW, if (p.dashed) doubleArrayOf(6.0, 4.0) else dash))
        }
        if (p.lines.isNotEmpty()) {
            scene.add(Scene.Stroke(p.lines.map { line -> DoubleArray(line.size * 2) { k -> if (k % 2 == 0) sx(line[k / 2].first) else sy(line[k / 2].second) } }, color, lw, dash))
        }
        // A vector's arrowhead (pgfplots' -> style: a small filled triangle).
        if (p.arrow) p.lines.forEach { line ->
            if (line.size < 2) return@forEach
            val (fx, fy) = line[line.size - 2].let { sx(it.first) to sy(it.second) }
            val (tx, ty) = line.last().let { sx(it.first) to sy(it.second) }
            val len = kotlin.math.hypot(tx - fx, ty - fy)
            if (len >= 0.5) {
                val ux = (tx - fx) / len; val uy = (ty - fy) / len
                val head = maxOf(lw * 5, 7.0)
                val bx = tx - ux * head; val by = ty - uy * head
                scene.add(Scene.Fill(listOf(doubleArrayOf(tx, ty, bx - uy * head * 0.4, by + ux * head * 0.4, bx + uy * head * 0.4, by - ux * head * 0.4)), color))
            }
        }
        // A vector field's arrows: the shafts in one stroke per color, then the heads.
        if (p.arrows.isNotEmpty()) {
            val f = p.f
            val tip = com.example.cas.graph.VectorField.Tip.entries[f.arrowTip]
            val shaft = maxOf(f.thickness * 0.25, 0.4)
            val headSize = (2.6 + f.thickness * 0.5) * f.arrowTipSize
            val shafts = LinkedHashMap<Int, MutableList<DoubleArray>>()
            val strokes = LinkedHashMap<Int, MutableList<DoubleArray>>()
            val fills = LinkedHashMap<Int, MutableList<DoubleArray>>()
            for (a in p.arrows) {
                val c = if (f.arrowsByLength) 0xFF000000.toInt() or f.colormap.rgb(com.example.cas.graph.VectorField.position(a.magnitude, p.arrowRange).let { if (f.colormapReversed) 1 - it else it }) else color
                val fx = sx(a.x0); val fy = sy(a.y0); val tx = sx(a.x1); val ty = sy(a.y1)
                val len = kotlin.math.hypot(tx - fx, ty - fy)
                if (len < 0.2) continue
                val h = com.example.cas.graph.VectorField.head(tip, tx, ty, (tx - fx) / len, (ty - fy) / len, minOf(headSize, len * 0.6))
                shafts.getOrPut(c) { ArrayList() } += doubleArrayOf(fx, fy, h.shaftEndX, h.shaftEndY)
                h.strokes.forEach { strokes.getOrPut(c) { ArrayList() } += it }
                h.fills.forEach { fills.getOrPut(c) { ArrayList() } += it }
                h.dot?.let { d -> fills.getOrPut(c) { ArrayList() } += DoubleArray(2 * 16) { k -> val t = (k / 2) * 2 * PI / 16; if (k % 2 == 0) d[0] + d[2] * kotlin.math.cos(t) else d[1] + d[2] * kotlin.math.sin(t) } }
            }
            shafts.forEach { (c, list) -> scene.add(Scene.Stroke(list, c, shaft)) }
            strokes.forEach { (c, list) -> scene.add(Scene.Stroke(list, c, shaft)) }
            fills.forEach { (c, list) -> scene.add(Scene.Fill(list, c)) }
        }
        // Error bars, thin with short caps (as matplotlib's errorbar), under the marks.
        if (p.errors.isNotEmpty()) {
            val bars = ArrayList<DoubleArray>()
            val cap = 2.5
            for (e in p.errors) {
                val (x, y, ex, ey) = e.toList()
                // In values: on a log axis each end goes where its value is.
                fun sx(x: Double) = Pgf.sx(v, frame, sc.x(x))
                fun sy(y: Double) = Pgf.sy(v, frame, sc.y(y).let { if (it.isFinite()) it else v.yMin - v.height })
                if (ey.isFinite() && ey > 0) {
                    val px = sx(x); val a = sy(y - ey); val b = sy(y + ey)
                    bars += doubleArrayOf(px, a, px, b); bars += doubleArrayOf(px - cap, a, px + cap, a); bars += doubleArrayOf(px - cap, b, px + cap, b)
                }
                if (ex.isFinite() && ex > 0) {
                    val py = sy(y); val a = sx(x - ex); val b = sx(x + ex)
                    bars += doubleArrayOf(a, py, b, py); bars += doubleArrayOf(a, py - cap, a, py + cap); bars += doubleArrayOf(b, py - cap, b, py + cap)
                }
            }
            scene.add(Scene.Stroke(bars, color, 0.8))
        }
        // Points as pgfplots' mark=*: small filled dots.
        // Marks at the line's shape, scaled like its size on screen (pgfplots' marks are small).
        p.points.forEach { pt -> com.example.cas.graph.Marker.of(p.f.pointShape).addTo(scene, sx(pt.x), sy(pt.y), p.f.pointSize * 0.43, color) }
        if (p.f.showLabel) p.points.filter { it.label == "point" }.take(200).forEach { pt ->
            scene.add(Scene.Label(sx(pt.x) + 5, sy(pt.y) - 9, "(" + shortNumber(sc.realX(pt.x)) + ", " + shortNumber(sc.realY(pt.y)) + ")", Pgf.TICK_SIZE * 0.85, style.ink, Scene.Anchor.Start, Scene.Font.Roman))
        }
        // Constructions' point names (math italic) and angle values.
        p.labels.forEach { l ->
            if (l.centered) scene.add(Scene.Label(sx(l.x), sy(l.y), l.text, Pgf.TICK_SIZE * 0.9, style.ink, Scene.Anchor.Middle, Scene.Font.Roman))
            else scene.add(Scene.Label(sx(l.x) + 4, sy(l.y) - 8, l.text, Pgf.TICK_SIZE, style.ink, Scene.Anchor.Start, Scene.Font.Italic))
        }
    }
    scene.add(Scene.ClipEnd)
    Pgf.axes(scene, v, frame, style, scale = sc)
    if (AppSettings.showLegend) Pgf.legend(scene, frame, style, drawn.filter { legendSource(it).isNotBlank() }.map { f -> legendEntry2D(f, colorOf(f)) })
    return scene
}

/** A 2D line's legend entry in an export: a stroke in its style, a mark, or a shaded swatch. */
internal fun legendEntry2D(f: PlotFunction, color: Int): Pgf.LegendEntry {
    val name = com.example.cas.graph.Legend.row(legendSource(f))
    val spans = com.example.cas.graph.Legend.spans(name)
    val lw = f.thickness * 0.5
    val dash = com.example.cas.graph.LineStyle.of(f.lineStyle).pattern(lw)
    fun alpha(c: Int, a: Float) = ((255 * a).toInt().coerceIn(0, 255) shl 24) or (c and 0xFFFFFF)
    return when (f.plot) {
        is Plot2DKind.PointList, is Plot2DKind.Point -> Pgf.LegendEntry(math = name, spans = spans, color =  color, line = f.connectPoints || f.closedShape, width = lw,
            marker = com.example.cas.graph.Marker.of(f.pointShape), markerSize = f.pointSize * 0.43,
        )
        is Plot2DKind.Region -> Pgf.LegendEntry(math = name, spans = spans, color =  color, line = true, width = 1.0, fill = alpha(color, f.fillOpacity))
        // A vector field: an arrow, or three (short to long) in their colors when colored by |F|.
        is Plot2DKind.VectorField -> Pgf.LegendEntry(math = name, spans = spans, color = color, line = false, width = maxOf(f.thickness * 0.25, 0.5),
            arrows = if (f.arrowsByLength) (0..2).map { k -> 0xFF000000.toInt() or f.colormap.rgb(if (f.colormapReversed) 1 - k / 2.0 else k / 2.0) } else listOf(color))
        // A field: its colormap as a strip.
        is Plot2DKind.Field -> Pgf.LegendEntry(math = name, spans = spans, color = color, line = false, strip = (0..7).map { k -> 0xFF000000.toInt() or f.colormap.rgb(if (f.colormapReversed) 1 - k / 7.0 else k / 7.0) })
        else -> Pgf.LegendEntry(math = name, spans = spans, color =  color, dash = dash, width = lw)
    }
}

/** A special point near the tap if there is one, otherwise the nearest curve at the tapped x. */
private fun findTrace(vm: Graph2DViewModel, plotted: List<Plotted>, tap: Offset, size: IntSize, reach: Float): Special? {
    val v = vm.view ?: return null
    fun sx(x: Double) = ((x - v.xMin) / v.width * size.width).toFloat()
    fun sy(y: Double) = ((v.yMax - y) / v.height * size.height).toFloat()
    val nearPoint = plotted.flatMap { it.points }
        .minByOrNull { (Offset(sx(it.x), sy(it.y)) - tap).getDistance() }
        ?.takeIf { (Offset(sx(it.x), sy(it.y)) - tap).getDistance() < reach }
    if (nearPoint != null) return nearPoint
    // Found points are in the view's coordinates (log₁₀ on a log axis), values only to evaluate.
    val sc = vm.scale
    val u = v.xMin + tap.x / size.width * v.width
    val x = sc.realX(u)
    val onGraph = vm.functions.filter { it.visible && it.compiled != null }
        .map { f -> Special(u, sc.y(vm.evaluate(f, x)), "", f.colorIndex) }
        .filter { it.y.isFinite() }
        .minByOrNull { abs(sy(it.y) - tap.y) }
        ?.takeIf { abs(sy(it.y) - tap.y) < reach * 1.5f }
    if (onGraph != null) return onGraph
    // Polar, parametric and implicit curves: the nearest sampled point.
    var best: Special? = null
    var bestD = reach * 1.5f
    for (p in plotted) {
        if (p.f.plot is Plot2DKind.Explicit) continue
        val pts = p.lines.flatten() + (if (p.slopeMarks) emptyList() else p.segments.flatMap { listOf(it[0] to it[1], it[2] to it[3]) })
        for ((px, py) in pts) {
            val d = (Offset(sx(px), sy(py)) - tap).getDistance()
            if (d < bestD) { bestD = d; best = Special(px, py, "", p.f.colorIndex) }
        }
    }
    if (best != null) return best
    // Over a field: its value there.
    val w = v.yMax - tap.y / size.height * v.height
    val y = sc.realY(w)
    vm.functions.firstOrNull { it.visible && it.plot is Plot2DKind.VectorField }?.let { f ->
        val k = f.plot as Plot2DKind.VectorField
        val p = vm.caller2(f, k.p)(x, y); val q = vm.caller2(f, k.q)(x, y)
        if (p.isFinite() && q.isFinite()) return Special(u, w, "vector $p $q", f.colorIndex)
    }
    return vm.functions.firstOrNull { it.visible && it.plot is Plot2DKind.Field }?.let { f ->
        val value = vm.caller2(f, (f.plot as Plot2DKind.Field).f)(x, y)
        if (value.isFinite()) Special(u, w, "value " + shortNumber(value), f.colorIndex) else null
    }
}


/**
 * The point of curve [colorIndex] under a finger at [at]: on a function of x, the curve's own
 * value at the finger's x; on any other curve, its nearest sampled point.
 */
private fun traceAlong(vm: Graph2DViewModel, plotted: List<Plotted>, colorIndex: Int, at: Offset, size: IntSize): Special? {
    val v = vm.view ?: return null
    val sc = vm.scale
    val u = v.xMin + at.x / size.width * v.width
    vm.functions.firstOrNull { it.colorIndex == colorIndex && it.visible && it.plot is Plot2DKind.Explicit && it.compiled != null }?.let { f ->
        val y = sc.y(vm.evaluate(f, sc.realX(u)))
        return if (y.isFinite()) Special(u, y, "", colorIndex) else null
    }
    fun sx(px: Double) = ((px - v.xMin) / v.width * size.width).toFloat()
    fun sy(py: Double) = ((v.yMax - py) / v.height * size.height).toFloat()
    val p = plotted.firstOrNull { it.f.colorIndex == colorIndex } ?: return null
    val pts = p.lines.flatten() + (if (p.slopeMarks) emptyList() else p.segments.flatMap { listOf(it[0] to it[1], it[2] to it[3]) }) + p.points.map { it.x to it.y }
    return pts.minByOrNull { (px, py) -> (Offset(sx(px), sy(py)) - at).getDistance() }?.let { (px, py) -> Special(px, py, "", colorIndex) }
}

/**
 * Keeps an axis label inside the canvas. While the keypad opens or closes the canvas can be
 * only a few pixels tall, so [max] falls below [min]; then the label just goes at [min]
 * instead of crashing (coerceIn throws on an empty range).
 */
internal fun pinInside(value: Float, min: Float, max: Float): Float =
    if (!value.isFinite()) min else if (max <= min) min else value.coerceIn(min, max)


/** The same guard for integer offsets (bubbles pinned inside a canvas that may be tiny). */
internal fun pinInside(value: Int, min: Int, max: Int): Int = if (max <= min) min else value.coerceIn(min, max)


/** Which sheets of a workbook to import, each as its own line; all are ticked to start with. */
@Composable
private fun SheetPickerDialog(
    sheets: List<com.example.cas.graph.Spreadsheet.Sheet>,
    onImport: (List<com.example.cas.graph.Spreadsheet.Sheet>) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var picked by remember { mutableStateOf(sheets.indices.toSet()) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
        title = { Text("Import sheets") },
        text = {
            Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                Text("Each sheet becomes its own line.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                sheets.forEachIndexed { k, sheet ->
                    val on = k in picked
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .toggleable(value = on, role = androidx.compose.ui.semantics.Role.Checkbox) { picked = if (it) picked + k else picked - k }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Checkbox(checked = on, onCheckedChange = null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sheet.name.ifBlank { "Sheet ${k + 1}" }, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            val cols = sheet.table.columns.size
                            Text("${sheet.table.rows} rows · $cols column" + if (cols == 1) "" else "s", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(enabled = picked.isNotEmpty(), onClick = { onImport(sheets.filterIndexed { k, _ -> k in picked }) }) {
                Text(if (picked.size == sheets.size) "Import all" else "Import ${picked.size}")
            }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * The 2D graph's settings, as Desmos's wrench has them: the limits typed exactly, linear or log
 * for each axis, the grid, the numbers along the axes, and radians or degrees for polar labels
 * and trigonometry.
 */
@Composable
private fun GraphSettingsDialog(vm: Graph2DViewModel, view: Viewport, onDismiss: () -> Unit) {
    val range = remember { RangeFields(view, vm.scale) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Graph settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                RangeAndScaleSettings(range, "x", "y")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Grid lines", modifier = Modifier.weight(1f))
                    androidx.compose.material3.Switch(checked = AppSettings.showGrid, onCheckedChange = AppSettings::changeShowGrid)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Numbers on the axes", modifier = Modifier.weight(1f))
                    androidx.compose.material3.Switch(checked = AppSettings.axisNumbers, onCheckedChange = AppSettings::changeAxisNumbers)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Degrees", modifier = Modifier.weight(1f))
                    androidx.compose.material3.Switch(checked = vm.angle == com.example.cas.engine.AngleUnit.Degrees, onCheckedChange = { vm.toggleAngle() })
                }
                Text("Field quality", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.material3.SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("Low", "Medium", "High").forEachIndexed { k, name ->
                        SegmentedButton(
                            selected = AppSettings.fieldQuality == k,
                            onClick = { AppSettings.changeFieldQuality(k) },
                            shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(k, 3),
                            icon = {},
                            label = { Text(name) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(enabled = range.valid, onClick = { range.apply(vm) { vm.view = it }; onDismiss() }) { Text("Done") }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** A height for the shaded area in the view's coordinates; on a log axis, below the view where it has no place. */
private fun areaY(sc: com.example.cas.graph.AxisScale, v: Viewport, y: Double): Double =
    sc.y(y).let { if (it.isFinite()) it.coerceIn(v.yMin - v.height, v.yMax + v.height) else if (sc.logY) v.yMin - v.height else 0.0 }

/** The measured stretch of curve for an arc length, in the view's coordinates. */
private fun arcPath(vm: GraphViewModel, ar: AreaResult, sc: com.example.cas.graph.AxisScale): List<Pair<Double, Double>> {
    val fn = (ar.f.plot as? Plot2DKind.Explicit)?.f ?: return emptyList()
    val lo = minOf(ar.a, ar.b); val hi = maxOf(ar.a, ar.b)
    return sc.paths(listOf((0..240).map { k -> val x = lo + (hi - lo) * k / 240; x to vm.call(ar.f, fn, x) })).flatten()
}

/** Adds the tangent (or the normal) to [f] at x = [a] as a new line, y = m x + c (or x = a when it's upright). */
internal fun addTangent(vm: Graph2DViewModel, f: PlotFunction, a: Double, normal: Boolean) {
    val fn = (f.plot as? Plot2DKind.Explicit)?.f ?: return
    val g = { x: Double -> vm.call(f, fn, x) }
    val b = g(a)
    val d = com.example.cas.graph.Plot2D.derivative(g, a)
    if (!b.isFinite() || !d.isFinite()) return
    fun n(v: Double) = java.math.BigDecimal(v).round(java.math.MathContext(6)).stripTrailingZeros().toPlainString().let { if (it == "-0") "0" else it }
    val m = if (normal) (if (kotlin.math.abs(d) < 1e-12) Double.NaN else -1 / d) else d
    val latex = if (!m.isFinite()) "x = " + n(a) else {
        val c = b - m * a
        // 1x is x, −1x is −x, and a 0 intercept is left off.
        val slope = when (n(m)) { "1" -> "x"; "-1" -> "-x"; "0" -> ""; else -> n(m) + "x" }
        val tail = if (n(c) == "0") "" else if (c < 0) " - " + n(-c) else " + " + n(c)
        "y = " + (slope.ifEmpty { n(c) }) + (if (slope.isEmpty()) "" else tail)
    }
    vm.addLatexLine(latex)
}

/** The top of the shaded area at x: the curve's height (clamped near the view). */
private fun areaHeight(vm: GraphViewModel, ar: AreaResult, x: Double): Double {
    val fn = (ar.f.plot as? Plot2DKind.Explicit)?.f ?: return 0.0
    return vm.call(ar.f, fn, x).takeIf { it.isFinite() } ?: 0.0
}

/**
 * The shaded region as a polygon in graph coordinates: along f from a to b, then back along g
 * (the other curve) or the x-axis. Heights are clamped a view's height beyond the view.
 */
private fun areaOutline(vm: GraphViewModel, ar: AreaResult, v: com.example.cas.graph.Viewport, sc: com.example.cas.graph.AxisScale = com.example.cas.graph.AxisScale()): List<Pair<Double, Double>>? {
    val fn = (ar.f.plot as? Plot2DKind.Explicit)?.f ?: return null
    val gn = ar.g?.let { (it.plot as? Plot2DKind.Explicit)?.f }
    val lo = minOf(ar.a, ar.b); val hi = maxOf(ar.a, ar.b)
    val steps = 200
    // In the view's coordinates; on a log x-axis, only the part at x > 0.
    fun clamp(y: Double) = areaY(sc, v, y)
    val xs = (0..steps).map { k -> lo + (hi - lo) * k / steps }.filter { sc.x(it).isFinite() }
    if (xs.size < 2) return null
    val top = xs.map { x -> sc.x(x) to clamp(vm.call(ar.f, fn, x)) }
    val bottom = xs.asReversed().map { x -> sc.x(x) to (if (gn != null) clamp(vm.call(ar.g!!, gn, x)) else clamp(0.0)) }
    return top + bottom
}

/** ∫ₐᵇ f dx (or ∫ₐᵇ (f − g) dx) and the total area, under the legend; × clears it. */
@Composable
private fun AreaCard(ar: AreaResult, onClose: () -> Unit, onUse: (Double) -> Unit) {
    val between = ar.g != null
    val icon = if (ar.arc) Icons.Default.Straighten else if (between) TabIcons.AreaBetween else TabIcons.Area
    val title = if (ar.arc) "Arc length" else if (between) "Area between the curves" else "Area under the curve"
    if (ar.signed.isNaN()) {
        ResultCard(icon, title, null, "—", note = if (ar.arc) "Couldn't measure the curve between these points" else "Couldn't integrate between these points", onClose = onClose)
        return
    }
    val lo = shortNumber(minOf(ar.a, ar.b)); val hi = shortNumber(maxOf(ar.a, ar.b))
    if (ar.arc) {
        // L = ∫ₐᵇ √(1 + f′(x)²) dx.
        ResultCard(
            icon, title, com.example.cas.graph.Legend.row("\$\\int_{$lo}^{$hi} \\sqrt{1 + f'(x)^{2}}\\,dx\$"), "≈ " + shortNumber(ar.total),
            stats = listOf("From" to "$lo to $hi"),
            note = "Drag the ends to change the stretch.",
            copyText = ar.total.toString(),
            onUse = { onClose(); onUse(ar.total) },
            onClose = onClose,
        )
        return
    }
    // The area itself large; the signed integral (what's above the axis less what's below) under it.
    ResultCard(
        icon, title, areaRow(between, lo, hi), "≈ " + shortNumber(ar.total),
        stats = listOf("Signed" to shortNumber(ar.signed), "From" to "$lo to $hi"),
        note = "Drag the dashed edges to change the limits.",
        copyText = ar.total.toString(),
        onUse = { onClose(); onUse(ar.total) },
        onClose = onClose,
    )
}

/** ∫ₐᵇ f(x) dx, or ∫ₐᵇ (f(x) − g(x)) dx, as math for the area card. */
private fun areaRow(between: Boolean, lo: String, hi: String): com.example.cas.editor.MathRow {
    fun text(t: String) = com.example.cas.editor.MathRow(t.map { com.example.cas.editor.Sym(it.toString()) }.toMutableList())
    val body = if (between) text("f(x)−g(x)") else text("f(x)")
    return com.example.cas.editor.MathRow(mutableListOf(com.example.cas.editor.Integral(text(lo), text(hi), body, text("x"))))
}


/** How far [p] is from a polyline (in screen pixels). */
private fun polylineDistance(line: List<Offset>, p: Offset): Float {
    var best = Float.MAX_VALUE
    for (k in 0 until line.size - 1) {
        val a = line[k]; val b = line[k + 1]
        val d = b - a
        val len2 = d.x * d.x + d.y * d.y
        val t = if (len2 == 0f) 0f else (((p.x - a.x) * d.x + (p.y - a.y) * d.y) / len2).coerceIn(0f, 1f)
        val q = Offset(a.x + d.x * t, a.y + d.y * t)
        best = minOf(best, (p - q).getDistance())
    }
    if (line.size == 1) best = (line[0] - p).getDistance()
    return best
}


/** Starts construct mode: GeoGebra's toolbar, in short. */
@Composable
private fun ConstructButton(onClick: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.shadow(4.dp, CircleShape).clip(CircleShape).background(colors.secondaryContainer)
            .clickable(onClickLabel = "Start constructing", onClick = onClick).padding(start = 12.dp, end = 14.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Default.Hexagon, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(18.dp))
        Text("Construct", color = colors.onSecondaryContainer, style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 14.sp, fontWeight = FontWeight.Medium))
        BetaBadge("Alpha")
    }
}

/**
 * What's being built: the tool, which step it's on (dots, and the step's role), what to tap
 * next, the picks so far as chips, and Undo, Close (a polygon) and Done.
 */
@Composable
private fun ConstructStatus(vm: Graph2DViewModel, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val tool = vm.geometryTool ?: GeometryTool.Move
    val picks = vm.geometryPicks
    Column(
        modifier.widthIn(max = 460.dp).fillMaxWidth().shadow(6.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceContainerHigh).padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                val ink = colors.onPrimaryContainer; val accent = colors.primary
                androidx.compose.foundation.Canvas(Modifier.size(24.dp)) { drawToolIcon(tool, ink, accent) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                val stepName = when {
                    tool == GeometryTool.Move -> "Pick a tool below"
                    tool == GeometryTool.Polygon -> if (picks.isEmpty()) "Corners" else "${picks.size} corner" + (if (picks.size == 1) "" else "s")
                    else -> "Step ${picks.size + 1} of ${tool.slots.length} · " + tool.steps.getOrElse(picks.size) { "" }
                }
                Text(tool.label, style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 16.sp, fontWeight = FontWeight.Medium), color = colors.onSurface)
                Text(stepName, style = MaterialTheme.typography.labelMedium, color = colors.primary)
            }
            if (picks.isNotEmpty()) IconButton(onClick = { vm.undoPick() }) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Take back the last pick", tint = colors.onSurfaceVariant)
            }
            if (tool == GeometryTool.Polygon && picks.size >= 3) androidx.compose.material3.FilledTonalButton(onClick = { vm.closePolygon() }, contentPadding = PaddingValues(horizontal = 14.dp)) { Text("Close") }
            androidx.compose.material3.TextButton(onClick = { vm.stopConstructing() }) { Text("Done") }
        }
        // Progress: a dot per step, filled when done, ringed for the current one.
        if (tool != GeometryTool.Move && tool != GeometryTool.Polygon && tool.slots.length > 1) Row(Modifier.padding(start = 48.dp, top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    Text(tool.steps.getOrElse(k) { tool.steps.firstOrNull() ?: "" } + " ", style = MaterialTheme.typography.labelSmall, color = colors.onSecondaryContainer.copy(alpha = 0.7f))
                    Text(geometryLabel(name), style = TextStyle(fontFamily = CasFonts.CmItalic, fontSize = 15.sp), color = colors.onSecondaryContainer)
                }
            }
        }
    }
}

/**
 * The tools, GeoGebra's toolbar as an M3 sheet: groups as tabs, each tool a tile with a drawn
 * icon; recent tools first. While a tool is in use it folds to one row (the tool, recent ones,
 * and a button to open it again), so the graph stays clear for tapping.
 */
@Composable
private fun ConstructPalette(vm: Graph2DViewModel, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(true) }
    var category by remember { mutableStateOf(GeometryCategory.Points) }
    // A tool picked folds the sheet; Move (or none) opens it.
    androidx.compose.runtime.LaunchedEffect(vm.geometryTool) { open = vm.geometryTool == null }
    Column(
        modifier.widthIn(max = 520.dp).fillMaxWidth().shadow(8.dp, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp))
            .background(colors.surfaceContainer).padding(10.dp),
    ) {
        if (!open) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (listOf(GeometryTool.Move) + vm.recentTools).distinct().take(5).forEach { t -> ToolChip(vm, t, compact = true) }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.clip(CircleShape).background(colors.secondaryContainer).clickable(onClickLabel = "All tools") { open = true }.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Apps, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Tools", style = MaterialTheme.typography.labelLarge, color = colors.onSecondaryContainer)
                }
            }
            return@Column
        }
        // The groups, as tabs.
        Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            GeometryCategory.entries.forEach { c ->
                val on = c == category
                Text(
                    c.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) colors.onSecondaryContainer else colors.onSurfaceVariant,
                    modifier = Modifier.clip(CircleShape).background(if (on) colors.secondaryContainer else Color.Transparent)
                        .clickable(onClickLabel = "Show ${c.label}") { category = c }.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        val tools = (if (category == GeometryCategory.Points) listOf(GeometryTool.Move) else emptyList()) + GeometryTool.entries.filter { it.category == category && it != GeometryTool.Move }
        tools.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                row.forEach { t -> Box(Modifier.weight(1f)) { ToolChip(vm, t, compact = false) } }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (vm.recentTools.isNotEmpty()) {
            Text("Recent", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { vm.recentTools.forEach { t -> ToolChip(vm, t, compact = true) } }
        }
    }
}

/**
 * The tools on a tablet: a rail at the graph's edge, each group under a small heading, its
 * tools as round icon buttons two abreast (the tool's name shows in the card on top). Scrolls
 * when the graph is short.
 */
@Composable
private fun ConstructRail(vm: Graph2DViewModel, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.width(118.dp).shadow(6.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).background(colors.surfaceContainer)
            .verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GeometryCategory.entries.forEachIndexed { k, c ->
            if (k > 0) Spacer(Modifier.height(8.dp))
            Text(c.label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1, modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 4.dp))
            val tools = (if (c == GeometryCategory.Points) listOf(GeometryTool.Move) else emptyList()) + GeometryTool.entries.filter { it.category == c && it != GeometryTool.Move }
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
private fun ToolChip(vm: Graph2DViewModel, tool: GeometryTool, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    val on = (vm.geometryTool ?: GeometryTool.Move) == tool
    val bg = if (on) colors.primary else if (compact) colors.surfaceContainerHigh else colors.surfaceContainerLow
    val ink = if (on) colors.onPrimary else colors.onSurface
    val accent = if (on) colors.onPrimary else colors.primary
    if (compact) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(bg).clickable(onClickLabel = tool.label) { vm.selectTool(tool) }
                .semantics { contentDescription = tool.label + if (on) ", in use" else "" },
            contentAlignment = Alignment.Center,
        ) { androidx.compose.foundation.Canvas(Modifier.size(26.dp)) { drawToolIcon(tool, ink, accent) } }
    } else {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(bg).clickable(onClickLabel = "Use ${tool.label}") { vm.selectTool(tool) }
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            androidx.compose.foundation.Canvas(Modifier.size(30.dp)) { drawToolIcon(tool, ink, accent) }
            Spacer(Modifier.height(4.dp))
            Text(tool.label, style = MaterialTheme.typography.labelSmall, color = ink, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 13.sp)
        }
    }
}

/**
 * A tool's icon, drawn: what it makes in [accent], what it's made from in [ink] (points as
 * dots), like GeoGebra's tool icons.
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
        GeometryTool.Locus -> {
            val path = Path().apply { for (k in 0..24) { val t = k / 24f * 6.283f; val x = 0.5f + 0.36f * kotlin.math.cos(t); val y = 0.5f + 0.22f * kotlin.math.sin(2 * t); if (k == 0) moveTo(x * w, y * w) else lineTo(x * w, y * w) } }
            drawPath(path, accent, style = Stroke(bold, cap = StrokeCap.Round, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(w * 0.02f, w * 0.1f))))
            dot(0.86f, 0.5f, accent)
        }
    }
}
