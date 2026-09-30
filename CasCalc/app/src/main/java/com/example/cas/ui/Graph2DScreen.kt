package com.example.cas.ui

import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
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
)

private data class Special(val x: Double, val y: Double, val label: String, val colorIndex: Int, /** At a crossing, the other curve's color slot. */ val other: Int? = null)

@Composable
fun Graph2DScreen(vm: Graph2DViewModel, onUseValue: (Double) -> Unit, modifier: Modifier = Modifier) {
    BackHandler(enabled = vm.active != null) { vm.edit(null) }
    GraphScaffold(vm, outputLabel = "y", modifier = modifier) {
        Graph2DCanvas(vm, onUseValue, Modifier.fillMaxSize())
    }
}

fun Graph2DViewModel.resetView(size: IntSize) {
    if (size.width > 0) view = Viewport.standard(size.height.toDouble() / size.width, halfWidth = AppSettings.viewHalfWidth.toDouble())
}

@Composable
private fun Graph2DCanvas(vm: Graph2DViewModel, onUseValue: (Double) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    var size by remember { mutableStateOf(IntSize.Zero) }
    var trace by remember { mutableStateOf<Special?>(null) }
    val themeColors = (0 until GraphViewModel.PLOT_COLOR_COUNT).map { plotColor(it) }
    // Colors picked by long-pressing a function's dot replace the theme's.
    val picked = vm.functions.mapNotNull { f -> f.customColor?.let { f.colorIndex to Color(it) } }.toMap()
    val palette = themeColors.indices.map { picked[it] ?: themeColors[it] }

    val view = vm.view
    val version = vm.version
    // Sampling and point-finding only redo when the view or a function changes.
    val highlighted = vm.highlighted
    val plotted = remember(view, version, size, vm.parameters.toMap(), highlighted, vm.polarGrid, AppSettings.specialPoints, AppSettings.fieldQuality) {
        // Whatever a line does while being sampled, drawing carries on (the line just isn't drawn).
        if (view == null || size.width == 0) emptyList() else runCatching { plot(vm, view, size, highlighted) }.getOrElse { emptyList() }
    }

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
                    fun screen(x: Double) = Offset(((x - v0.xMin) / v0.width * size.width).toFloat(), ((v0.yMax - areaHeight(vm, ar, x)) / v0.height * size.height).toFloat())
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
                        vm.moveAreaEdge(end, v.xMin + change.position.x / size.width * v.width)
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
                        val x = v.xMin + change.position.x / size.width * v.width
                        val y = v.yMax - change.position.y / size.height * v.height
                        // Snap to a tidy value (a hundredth of the view) as the finger moves.
                        val snap = Plot2D.niceStep(v.width, 100)
                        lx?.let { vm.dragSlider(it, Math.round(x / snap) * snap) }
                        ly?.let { vm.dragSlider(it, Math.round(y / snap) * snap) }
                    }
                }
            }
            .pointerInput(plotted) {
                detectTapGestures(
                    onDoubleTap = { vm.resetView(size); trace = null },
                    onTap = { tap ->
                        val start = vm.areaStart
                        if (start != null) {
                            // The second point of an area: a marked point if one is near, else where you tapped.
                            val v = vm.view
                            if (v != null) {
                                val near = findTrace(vm, plotted, tap, size, 28.dp.toPx())?.takeIf { it.label.isNotEmpty() }
                                vm.finishArea(near?.x ?: (v.xMin + tap.x / size.width * v.width))
                            }
                            trace = null
                            return@detectTapGestures
                        }
                        val t = findTrace(vm, plotted, tap, size, 28.dp.toPx())
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
            // Fields first, under the grid, the axes and every line (the first in the list on top).
            plotted.asReversed().forEach { p ->
                p.fieldImage?.let { img -> drawImage(img, dstSize = IntSize(size.width.toInt(), size.height.toInt()), filterQuality = FilterQuality.Low) }
            }
            val overField = plotted.any { it.fieldImage != null }
            if (vm.polarGrid) drawPolarGrid(v, colors.outlineVariant, colors.onSurfaceVariant, measurer, vm.angle == com.example.cas.engine.AngleUnit.Degrees, halo = if (overField) colors.surface else null)
            // The grid can be turned off in settings (the axes stay).
            else drawGrid(v, if (AppSettings.showGrid) colors.outlineVariant else Color.Transparent, colors.onSurfaceVariant, measurer, halo = if (overField) colors.surface else null)
            vm.area?.let { ar ->
                if (!ar.signed.isNaN()) areaOutline(vm, ar, v)?.let { poly ->
                    val path = Path()
                    poly.forEachIndexed { k, (x, y) -> val o = toScreen(v, x, y); if (k == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y) }
                    path.close()
                    drawPath(path, palette[ar.f.colorIndex].copy(alpha = 0.28f))
                    // The edges, as handles to drag.
                    listOf(ar.a, ar.b).forEach { x ->
                        val top = toScreen(v, x, areaHeight(vm, ar, x))
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
                drawErrorBars(v, p.errors, palette[p.f.colorIndex])
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
                    val o = toScreen(v, a, vm.call(f, fn, a))
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
                        val t = measurer.measure("(" + shortNumber(s.x) + ", " + shortNumber(s.y) + ")", labelStyle)
                        drawText(t, topLeft = Offset(o.x + 8.dp.toPx(), o.y - t.size.height - 4.dp.toPx()))
                    }
                }
            }
            trace?.let { t ->
                val o = toScreen(v, t.x, t.y)
                drawCircle(palette[t.colorIndex], radius = 7.dp.toPx(), center = o)
                drawCircle(colors.inverseSurface, radius = 3.dp.toPx(), center = o)
            }
        }
        // The legend, top left (each drawn line's name, in list order), and under it the area card.
        Column(Modifier.align(Alignment.TopStart).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GraphLegend(
            remember(version, palette) {
                vm.functions.filter { it.visible && !it.isText && (it.plot != null || it.family.isNotEmpty()) && legendSource(it).isNotBlank() }
                    .map { screenLegendEntry2D(it, palette[it.colorIndex]) }
            },
            )
            vm.area?.let { ar -> AreaCard(ar, onClose = { vm.clearArea() }, onUse = { v -> onUseValue(v) }) }
        }
        if (vm.areaStart != null) {
            Text(
                if (vm.areaStart?.g != null) "Tap where the area between the curves should end" else "Tap where the area should end",
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
            onGraphFile = { share -> exporting = false; writeFile(com.example.cas.graph.GraphFile.Contents(com.example.cas.graph.GraphFile.Kind.Graph2D, "graph", vm.graphData()), share) },
        )
        // Import a CSV file of data points as point lists.
        val context = androidx.compose.ui.platform.LocalContext.current
        val scope = rememberCoroutineScope()
        val importer = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val table = runCatching {
                    withContext(Dispatchers.IO) {
                        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
                        com.example.cas.graph.Csv.parse(text)
                    }
                }.getOrNull()
                // One line for the file: its first column against its second. The other columns
                // (more y values, σ(x), σ(y)) are picked in the table, which opens straight away.
                val n = if (table == null || table.columns.isEmpty()) 0 else vm.importTable(table)
                android.widget.Toast.makeText(
                    context,
                    if (n == 0) "No numbers found in that file" else "$n points" + if ((table?.columns?.size ?: 0) > 2) " · ${table!!.columns.size} columns, pick more in the table" else "",
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            }
        }
        GraphBottomBar(vm, Modifier.align(Alignment.BottomCenter), onExport = { if (size.width > 0) exporting = true }, tables = true, leading = {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(48.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(colors.secondaryContainer)
                    .clickable(onClickLabel = "Import points from a CSV file") { tap(); importer.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream")) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = "Import CSV", tint = colors.onSecondaryContainer)
            }
        }, tools = {
            ToolToggle(PlotIcons.PolarGrid, "Polar grid", vm.polarGrid) { vm.polarGrid = !vm.polarGrid }
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
            // With the polar grid on, points read as (r, θ).
            val r = kotlin.math.hypot(t.x, t.y)
            var theta = kotlin.math.atan2(t.y, t.x)
            if (theta < 0) theta += 2 * PI
            val shownTheta = if (degrees) theta * 180 / PI else theta
            // Over a field, its value there as a third row.
            val fieldValue = t.label.takeIf { it.startsWith("value ") }?.removePrefix("value ")?.replace("−", "-")?.toDoubleOrNull()
            fun use(v: Double): () -> Unit = { trace = null; onUseValue(v) }
            val rows = (if (vm.polarGrid) listOf(CardValue("r", shortNumber(r), onUse = use(r)), CardValue("θ", shortNumber(shownTheta) + if (degrees) "°" else "", onUse = use(shownTheta)))
                else listOf(CardValue("x", shortNumber(t.x), onUse = use(t.x)), CardValue("y", shortNumber(t.y), onUse = use(t.y)))) +
                listOfNotNull(fieldValue?.let { CardValue("f", shortNumber(it), onUse = use(it)) })
            val kind = when {
                fieldValue != null -> "Value"
                t.label == "point" || t.label.isEmpty() -> null
                t.label == "y-intercept" -> "y-intercept"
                else -> t.label.replaceFirstChar { it.uppercase() }
            }
            val actions = listOfNotNull(
                // Area: this point is where it starts; the next tap on the graph is where it ends.
                areaFunction?.let { f -> CardAction(TabIcons.Area, if (otherFunction != null) "Area" else "Area from here", "Area from here") { vm.clearArea(); vm.areaStart = Graph2DViewModel.AreaStart(f, null, t.x); trace = null } },
                // Area between the two curves that cross here.
                if (areaFunction != null && otherFunction != null) CardAction(TabIcons.AreaBetween, "Between curves", "Area between the curves from here") { vm.clearArea(); vm.areaStart = Graph2DViewModel.AreaStart(areaFunction, otherFunction, t.x); trace = null } else null,
            )
            PointCardAt(px, py, palette[t.colorIndex], kind, line?.let { legendSource(it) }?.takeIf { it.isNotBlank() }, rows, actions)
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
    val nx = (size.width / 6).coerceIn(40, 240)
    val ny = (size.height / 6).coerceIn(40, 320)
    val out = ArrayList<Plotted>()
    // A line with a list in it is drawn as each of its members (g), in the line's own style (f).
    for (f in fns) for (g in f.family.ifEmpty { listOf(f) }) {
        val k = g.plot ?: continue
        when (k) {
            is Plot2DKind.Vector -> {
                val x = vm.call(g, k.x); val y = vm.call(g, k.y)
                val x0 = k.fromX?.let { vm.call(g, it) } ?: 0.0
                val y0 = k.fromY?.let { vm.call(g, it) } ?: 0.0
                if (listOf(x, y, x0, y0).all { it.isFinite() }) out += Plotted(f, listOf(listOf(x0 to y0, x to y)), emptyList(), arrow = true)
            }
            is Plot2DKind.PointList -> {
                val shown = k.xs.indices.filter { k.xs[it].isFinite() && k.ys[it].isFinite() }
                val pts = shown.map { Special(k.xs[it], k.ys[it], "point", f.colorIndex) }
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
                if (px.isFinite() && py.isFinite() && vm.allowed(g, px, py)) out += Plotted(f, emptyList(), listOf(Special(px, py, "point", f.colorIndex)))
            }
            is Plot2DKind.Explicit -> {
                // Conditions after commas (0 < x < 2) leave gaps where they fail.
                val fx = { x: Double -> vm.call(g, k.f, x).let { y -> if (vm.allowed(g, x, y)) y else Double.NaN } }
                val points = ArrayList<Special>()
                if (f === highlighted && AppSettings.specialPoints) {
                    Plot2D.zeros(fx, view.xMin, view.xMax, 400).forEach { points += Special(it, 0.0, "zero", f.colorIndex) }
                    Plot2D.extrema(fx, view.xMin, view.xMax, 400).forEach { (x, kind) ->
                        points += Special(x, fx(x), if (kind == Plot2D.Kind.Maximum) "maximum" else "minimum", f.colorIndex)
                    }
                    val y0 = fx(0.0)
                    if (y0.isFinite() && view.xMin < 0 && view.xMax > 0) points += Special(0.0, y0, "y-intercept", f.colorIndex)
                }
                out += Plotted(f, Plot2D.sample(fx, view, samples), points)
            }
            is Plot2DKind.Polar -> {
                val r = { t: Double -> vm.call(g, k.r, t).let { rr -> if (vm.allowed(g, rr * kotlin.math.cos(t), rr * kotlin.math.sin(t), theta = t, r = rr)) rr else Double.NaN } }
                out += Plotted(f, Curves.polar(r, view, turns = periodTurns(listOf(r)).toDouble()), emptyList())
            }
            is Plot2DKind.Parametric -> {
                val x = { t: Double -> vm.call(g, k.x, t).let { xx -> if (vm.allowed(g, xx, vm.call(g, k.y, t), t = t)) xx else Double.NaN } }
                val y = { t: Double -> vm.call(g, k.y, t) }
                // Closed curves go round once; open ones like (t, t²) run over −10 ≤ t ≤ 10.
                val turns = periodTurns(listOf(x, y))
                val (t0, t1) = if (turns <= 6) 0.0 to 2 * PI * turns else -10.0 to 10.0
                out += Plotted(f, Curves.parametric(x, y, t0, t1, view), emptyList())
            }
            is Plot2DKind.Implicit -> {
                // Sliders read once for the whole grid.
                val g0 = vm.caller2(g, k.f); val ok = vm.allowedCaller(g)
                val g = { x: Double, y: Double -> if (ok(x, y)) g0(x, y) else Double.NaN }
                out += Plotted(f, emptyList(), emptyList(), segments = Curves.implicit(g, view, nx, ny))
            }
            is Plot2DKind.Field -> {
                // Colored cells 12, 6 or 3 px square (the field quality setting; low by default, as
                // every cell is worked out again on each pan); the scale follows the values in view.
                val cell = when (AppSettings.fieldQuality) { 2 -> 3; 1 -> 6; else -> 12 }
                val fx = (size.width / cell).coerceIn(24, 420); val fy = (size.height / cell).coerceIn(24, 560)
                val h = vm.caller2(g, k.f); val ok = vm.allowedCaller(g)
                val values = com.example.cas.graph.Field.sample({ x: Double, y: Double -> if (ok(x, y)) h(x, y) else Double.NaN }, view, fx, fy)
                val range = com.example.cas.graph.Field.range(values)
                val px = com.example.cas.graph.Field.colors(values, range.first, range.second, f.colormap, f.colormapReversed)
                val image = runCatching { android.graphics.Bitmap.createBitmap(px, fx, fy, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap() }.getOrNull()
                out += Plotted(f, emptyList(), emptyList(), field = px, fieldSize = IntSize(fx, fy), fieldRange = range, fieldImage = image)
            }
            is Plot2DKind.Region -> {
                val values = DoubleArray(k.parts.size)
                val parts = k.parts.map { vm.caller2(g, it) }
                val ok = vm.allowedCaller(g)
                val test = { x: Double, y: Double ->
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
                    Curves.implicit({ x, y -> a(x, y) - b(x, y) }, view, nx, ny)
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
        val d = { x: Double -> vm.call(explicit[a], fa, x) - vm.call(explicit[b], fb, x) }
        Plot2D.zeros(d, view.xMin, view.xMax, 400).forEach { x ->
            crossings += Special(x, vm.call(explicit[a], fa, x), "intersection", explicit[a].colorIndex, explicit[b].colorIndex)
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
internal fun DrawScope.drawGrid(v: Viewport, gridColor: Color, axisColor: Color, measurer: TextMeasurer, ySuffix: String = "", halo: Color? = null) {
    val targetX = (size.width / 90.dp.toPx()).toInt().coerceAtLeast(3)
    val targetY = (size.height / 90.dp.toPx()).toInt().coerceAtLeast(3)
    val stepX = Plot2D.niceStep(v.width, targetX)
    val stepY = Plot2D.niceStep(v.height, targetY)
    val minor = gridColor.copy(alpha = 0.35f)
    val major = gridColor.copy(alpha = 0.9f)
    // Minor lines at a fifth of a step.
    for (x in Plot2D.ticks(v.xMin, v.xMax, targetX * 5)) {
        val sx = toScreen(v, x, 0.0).x
        drawLine(minor, Offset(sx, 0f), Offset(sx, size.height), 1f)
    }
    for (y in Plot2D.ticks(v.yMin, v.yMax, targetY * 5)) {
        val sy = toScreen(v, 0.0, y).y
        drawLine(minor, Offset(0f, sy), Offset(size.width, sy), 1f)
    }
    val xs = Plot2D.ticks(v.xMin, v.xMax, targetX)
    val ys = Plot2D.ticks(v.yMin, v.yMax, targetY)
    xs.forEach { x -> val sx = toScreen(v, x, 0.0).x; drawLine(major, Offset(sx, 0f), Offset(sx, size.height), 1.2f) }
    ys.forEach { y -> val sy = toScreen(v, 0.0, y).y; drawLine(major, Offset(0f, sy), Offset(size.width, sy), 1.2f) }
    // Axes, and labels that stay on screen when an axis scrolls away.
    val origin = toScreen(v, 0.0, 0.0)
    val axisW = 2.dp.toPx()
    if (origin.y in 0f..size.height) drawLine(axisColor, Offset(0f, origin.y), Offset(size.width, origin.y), axisW)
    if (origin.x in 0f..size.width) drawLine(axisColor, Offset(origin.x, 0f), Offset(origin.x, size.height), axisW)
    // Numbers along the axes can be turned off in the graph's settings.
    if (!AppSettings.axisNumbers) return
    val style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 11.sp, color = axisColor, shadow = halo?.let { androidx.compose.ui.graphics.Shadow(it, blurRadius = 5f) })
    val labelY = pinInside(origin.y, 2.dp.toPx(), size.height - 16.dp.toPx())
    xs.filter { abs(it) > stepX / 2 }.forEach { x ->
        val t = measurer.measure(Plot2D.label(x, stepX), style)
        val sx = toScreen(v, x, 0.0).x
        drawText(t, topLeft = Offset(sx - t.size.width / 2f, labelY + 3.dp.toPx()))
    }
    val labelX = pinInside(origin.x, 2.dp.toPx(), size.width - 40.dp.toPx())
    ys.filter { abs(it) > stepY / 2 }.forEach { y ->
        val t = measurer.measure(Plot2D.label(y, stepY) + ySuffix, style)
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
private fun DrawScope.drawErrorBars(v: Viewport, errors: List<DoubleArray>, color: Color) {
    if (errors.isEmpty()) return
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

private fun DrawScope.drawCurve(v: Viewport, p: Plotted, color: Color) {
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
    if (p.segments.isNotEmpty()) {
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
    if (AppSettings.showGrid && !vm.polarGrid) Pgf.grid(scene, v, frame, style)
    scene.add(Scene.ClipStart(frame.left, frame.top, frame.width, frame.height))
    if (vm.polarGrid) {
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
        if (!ar.signed.isNaN()) areaOutline(vm, ar, v)?.let { poly ->
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
        if (p.segments.isNotEmpty()) {
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
        // Error bars, thin with short caps (as matplotlib's errorbar), under the marks.
        if (p.errors.isNotEmpty()) {
            val bars = ArrayList<DoubleArray>()
            val cap = 2.5
            for (e in p.errors) {
                val (x, y, ex, ey) = e.toList()
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
            scene.add(Scene.Label(sx(pt.x) + 5, sy(pt.y) - 9, "(" + shortNumber(pt.x) + ", " + shortNumber(pt.y) + ")", Pgf.TICK_SIZE * 0.85, style.ink, Scene.Anchor.Start, Scene.Font.Roman))
        }
    }
    scene.add(Scene.ClipEnd)
    Pgf.axes(scene, v, frame, style)
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
    val x = v.xMin + tap.x / size.width * v.width
    val onGraph = vm.functions.filter { it.visible && it.compiled != null }
        .map { f -> Special(x, vm.evaluate(f, x), "", f.colorIndex) }
        .filter { it.y.isFinite() }
        .minByOrNull { abs(sy(it.y) - tap.y) }
        ?.takeIf { abs(sy(it.y) - tap.y) < reach * 1.5f }
    if (onGraph != null) return onGraph
    // Polar, parametric and implicit curves: the nearest sampled point.
    var best: Special? = null
    var bestD = reach * 1.5f
    for (p in plotted) {
        if (p.f.plot is Plot2DKind.Explicit) continue
        val pts = p.lines.flatten() + p.segments.flatMap { listOf(it[0] to it[1], it[2] to it[3]) }
        for ((px, py) in pts) {
            val d = (Offset(sx(px), sy(py)) - tap).getDistance()
            if (d < bestD) { bestD = d; best = Special(px, py, "", p.f.colorIndex) }
        }
    }
    if (best != null) return best
    // Over a field: its value there.
    val y = v.yMax - tap.y / size.height * v.height
    return vm.functions.firstOrNull { it.visible && it.plot is Plot2DKind.Field }?.let { f ->
        val value = vm.caller2(f, (f.plot as Plot2DKind.Field).f)(x, y)
        if (value.isFinite()) Special(x, y, "value " + shortNumber(value), f.colorIndex) else null
    }
}


/**
 * The point of curve [colorIndex] under a finger at [at]: on a function of x, the curve's own
 * value at the finger's x; on any other curve, its nearest sampled point.
 */
private fun traceAlong(vm: Graph2DViewModel, plotted: List<Plotted>, colorIndex: Int, at: Offset, size: IntSize): Special? {
    val v = vm.view ?: return null
    val x = v.xMin + at.x / size.width * v.width
    vm.functions.firstOrNull { it.colorIndex == colorIndex && it.visible && it.plot is Plot2DKind.Explicit && it.compiled != null }?.let { f ->
        val y = vm.evaluate(f, x)
        return if (y.isFinite()) Special(x, y, "", colorIndex) else null
    }
    fun sx(px: Double) = ((px - v.xMin) / v.width * size.width).toFloat()
    fun sy(py: Double) = ((v.yMax - py) / v.height * size.height).toFloat()
    val p = plotted.firstOrNull { it.f.colorIndex == colorIndex } ?: return null
    val pts = p.lines.flatten() + p.segments.flatMap { listOf(it[0] to it[1], it[2] to it[3]) } + p.points.map { it.x to it.y }
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


/**
 * The 2D graph's settings, as Desmos's wrench has them: the limits typed exactly, the grid,
 * the numbers along the axes, and radians or degrees for polar labels and trigonometry.
 */
@Composable
private fun GraphSettingsDialog(vm: Graph2DViewModel, view: Viewport, onDismiss: () -> Unit) {
    fun text(v: Double) = shortNumber(v).replace("−", "-")
    val fields = remember { androidx.compose.runtime.mutableStateListOf(text(view.xMin), text(view.xMax), text(view.yMin), text(view.yMax)) }
    fun num(t: String) = t.trim().replace("−", "-").replace(",", ".").toDoubleOrNull()?.takeIf { it.isFinite() }
    val n = fields.map { num(it) }
    val valid = n.all { it != null } && n[0]!! < n[1]!! && n[2]!! < n[3]!!
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Graph settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "x", 2 to "y").forEach { (k, letter) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.OutlinedTextField(fields[k], { fields[k] = it }, singleLine = true, label = { Text("$letter from") }, modifier = Modifier.weight(1f))
                        androidx.compose.material3.OutlinedTextField(fields[k + 1], { fields[k + 1] = it }, singleLine = true, label = { Text("$letter to") }, modifier = Modifier.weight(1f))
                    }
                }
                if (!valid) Text("Each “from” must be a number below its “to”.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
            androidx.compose.material3.TextButton(enabled = valid, onClick = { vm.view = Viewport(n[0]!!, n[1]!!, n[2]!!, n[3]!!); onDismiss() }) { Text("Done") }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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
private fun areaOutline(vm: GraphViewModel, ar: AreaResult, v: com.example.cas.graph.Viewport): List<Pair<Double, Double>>? {
    val fn = (ar.f.plot as? Plot2DKind.Explicit)?.f ?: return null
    val gn = ar.g?.let { (it.plot as? Plot2DKind.Explicit)?.f }
    val lo = minOf(ar.a, ar.b); val hi = maxOf(ar.a, ar.b)
    val steps = 200
    fun clamp(y: Double) = if (y.isFinite()) y.coerceIn(v.yMin - v.height, v.yMax + v.height) else 0.0
    val top = (0..steps).map { k -> val x = lo + (hi - lo) * k / steps; x to clamp(vm.call(ar.f, fn, x)) }
    val bottom = (steps downTo 0).map { k -> val x = lo + (hi - lo) * k / steps; x to (if (gn != null) clamp(vm.call(ar.g!!, gn, x)) else 0.0) }
    return top + bottom
}

/** ∫ₐᵇ f dx (or ∫ₐᵇ (f − g) dx) and the total area, under the legend; × clears it. */
@Composable
private fun AreaCard(ar: AreaResult, onClose: () -> Unit, onUse: (Double) -> Unit) {
    val between = ar.g != null
    val icon = if (between) TabIcons.AreaBetween else TabIcons.Area
    val title = if (between) "Area between the curves" else "Area under the curve"
    if (ar.signed.isNaN()) {
        ResultCard(icon, title, null, "—", note = "Couldn't integrate between these points", onClose = onClose)
        return
    }
    val lo = shortNumber(ar.a); val hi = shortNumber(ar.b)
    // The area itself large; the signed integral (what's above the axis less what's below) under it.
    ResultCard(
        icon, title, areaRow(between, lo, hi), "≈ " + shortNumber(ar.total),
        stats = listOf("Signed integral" to shortNumber(ar.signed), "From" to "$lo to $hi"),
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
