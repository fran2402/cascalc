package com.example.cas.ui

import androidx.compose.ui.zIndex
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cas.cas.CD
import com.example.cas.graph.DomainColoring
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import com.example.cas.graph.Scene
import com.example.cas.graph.Pgf
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.ui.graphics.toArgb
import com.example.cas.graph.Plot2D
import com.example.cas.graph.Viewport
import com.example.cas.ui.theme.CasFonts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Complex functions by domain coloring, after samuelj.li's complex function
 * plotter: color shows arg f(z), brightness |f(z)| (zeros black, poles white).
 * Drag and pinch to move and zoom; tap a point to read z and f(z); letters
 * other than z get sliders (f(z) = z − t); draw a loop to integrate around it.
 */
@Composable
fun ComplexScreen(vm: ComplexViewModel, onUseValue: (CD) -> Unit = {}, modifier: Modifier = Modifier) {
    // Points, curves and loops take the 2D graph's colors.
    PlotPalette.colors = plotColors(MaterialTheme.colorScheme).map { it.toArgb() }
    BackHandler(enabled = vm.active != null) { vm.edit(null) }
    // The plot's size, for exporting it in the same shape.
    var plotSize by remember { mutableStateOf(IntSize.Zero) }
    var exporting by remember { mutableStateOf(false) }
    val build = { r: ExportRequest -> complexScene(vm, r.view, EXPORT_SIZE, r.dark, quick = r.preview) }
    val export = rememberGraphExporter("complex-plot", build)
    val shownView = vm.view
    val writeFile = rememberGraphFileWriter()
    if (exporting && shownView != null) ExportDialog(
        shownView, build, onExport = { r, share -> exporting = false; export(r, share) }, onDismiss = { exporting = false },
        graphFile = com.example.cas.graph.GraphFile.Kind.Complex,
        scale = vm.scale,
        onGraphFile = { share -> exporting = false; writeFile(com.example.cas.graph.GraphFile.Contents(com.example.cas.graph.GraphFile.Kind.Complex, "complex-plot", vm.graphData()), share) },
    )
    // Geometry mode: lines read again when it's turned on or off; editing a line on a phone closes construct mode.
    androidx.compose.runtime.LaunchedEffect(AppSettings.geometry) { vm.refreshAll(); if (!AppSettings.geometry) vm.stopConstructing() }
    val phone = !isTabletLayout()
    androidx.compose.runtime.LaunchedEffect(vm.active, phone) { if (phone && vm.active != null) vm.stopConstructing() }
    GraphScaffold(vm, outputLabel = "f(z)", modifier = modifier) {
        Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth().onSizeChanged { plotSize = it }) {
            ComplexCanvas(vm, Modifier.fillMaxSize(), onUseValue)
            // (Top left holds the contour result, bottom right the toolbar.)
            GraphBottomBar(vm, Modifier.align(Alignment.BottomCenter), onExport = { if (vm.view != null) exporting = true }, tools = { PlotTools(vm) })
            // Top left: the contour result, and the legend under it.
            Column(Modifier.align(Alignment.TopStart).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.contourResult?.let { ContourCard(it, onClose = vm::clearContour, onUse = { v -> vm.clearContour(); onUseValue(v) }) }
                // Typed ∮ lines: their values on the same card (closed until the line changes).
                val closedLoops = remember(vm.version) { androidx.compose.runtime.mutableStateListOf<PlotFunction>() }
                vm.functions.filter { it.visible && it.contour != null && it !in closedLoops }.forEach { fn ->
                    val value = runCatching { com.example.cas.cas.Numeric.eval(fn.contour!!.value) }.getOrNull()
                    if (value != null) ContourCard(value, onClose = { closedLoops += fn }, onUse = { v -> onUseValue(v) }, math = remember(fn.version) { com.example.cas.graph.Legend.row(legendSource(fn)) })
                }
                vm.complexArea?.let { ar -> ComplexAreaCard(ar, onClose = { vm.complexArea = null }, onUse = { v -> vm.complexArea = null; onUseValue(CD(v)) }) }
                val version = vm.version
                GraphLegend(remember(version, vm.plotted) {
                    complexLegendLines(vm).map { fn ->
                        when {
                            fn === vm.plotted -> ScreenLegendEntry(legendSource(fn), Color.White, line = false, strip = colormapStops(fn.colormap, 7, fn.colormapReversed))
                            fn.complexPoints != null -> ScreenLegendEntry(legendSource(fn), complexLineColor(fn), line = fn.connectPoints || fn.closedShape, marker = com.example.cas.graph.Marker.of(fn.pointShape))
                            // Constructions as on the 2D graph: a point's mark, a line's style.
                            fn.geometry != null && vm.constructionKind(fn) == 'P' -> ScreenLegendEntry(legendSource(fn), complexLineColor(fn), line = false, marker = com.example.cas.graph.Marker.of(fn.pointShape))
                            fn.geometry != null -> ScreenLegendEntry(legendSource(fn), complexLineColor(fn), style = fn.lineStyle)
                            else -> ScreenLegendEntry(legendSource(fn), complexLineColor(fn))
                        }
                    }
                })
            }
            // A tool's number (n roots, a factor) or function name, asked once its taps are done.
            vm.pendingAsk?.let { ask -> ToolNumberDialog(ask.tool, onDone = { vm.answerAsk(it) }, onDismiss = { vm.pendingAsk = null }) }
            // Geometry on the complex plane: Construct starts building, as on the 2D graph.
            if (AppSettings.geometry) {
                if (!vm.constructing) ConstructButton(onClick = { vm.constructing = true }, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
                else if (isTabletLayout()) {
                    val byList = if (AppSettings.keypadSide == 0) Alignment.TopStart else Alignment.TopEnd
                    ConstructStatus(vm, Modifier.align(Alignment.TopCenter).padding(top = 10.dp, start = 140.dp, end = 140.dp))
                    GeometryGuide(vm, Modifier.zIndex(10f).align(Alignment.BottomCenter).padding(bottom = 76.dp, start = 140.dp, end = 140.dp))
                    ConstructRail(vm, Modifier.align(byList).padding(top = 10.dp, bottom = 84.dp, start = 10.dp, end = 10.dp))
                } else {
                    ConstructStatus(vm, Modifier.align(Alignment.TopCenter).padding(top = 10.dp, start = 12.dp, end = 12.dp))
                    GeometryGuide(vm, Modifier.zIndex(10f).align(Alignment.BottomCenter).padding(bottom = 76.dp, start = 12.dp, end = 12.dp))
                }
            }
        }
        // On a phone the tools sit under the plane, in the list's place.
        if (AppSettings.geometry && vm.constructing && phone) {
            ConstructPalette(vm, Modifier.background(MaterialTheme.colorScheme.surface).padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 6.dp))
        }
        }
    }
}

fun ComplexViewModel.resetView(size: IntSize) {
    if (size.width > 0) view = com.example.cas.graph.AxisScale.standard(Viewport.standard(size.height.toDouble() / size.width, halfWidth = 3.0), scale)
}

@Composable
private fun ComplexCanvas(vm: ComplexViewModel, modifier: Modifier, onUseValue: (CD) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    var size by remember { mutableStateOf(IntSize.Zero) }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var refining by remember { mutableStateOf(false) }
    var probe by remember { mutableStateOf<CD?>(null) }
    // A tapped point, curve or loop: which line, where, and t on a curve z(t).
    var hit by remember { mutableStateOf<PlaneHit?>(null) }
    val view = vm.view
    val f = vm.plotted
    val params = vm.parameters.toMap()
    // Log axes: the view is in log₁₀ of Re z or Im z on them; every z here is a value (see AxisScale).
    val sc = vm.scale

    // Equations drawn as curves over the coloring (|z − 1| = 2, x² + y² = 4).
    // Keyed on the lines other than constructions, so a dragged point doesn't redo them (or the coloring below).
    val plotKey = vm.plotKey
    val curves = remember(view, plotKey, size, params, sc) {
        val v = view
        if (v == null || size.width == 0) emptyList() else vm.functions.filter { it.visible && it.complexCurve != null }.mapNotNull { fn ->
            val g = fn.complexCurve!!
            // Found in the view's coordinates, kept as values.
            runCatching { com.example.cas.graph.Curves.implicit(sc.function2 { x, y -> vm.call(fn, g, x, y) }, v, (size.width / 6).coerceIn(40, 200), (size.height / 6).coerceIn(40, 260)) }.getOrNull()
                ?.map { sg -> doubleArrayOf(sc.realX(sg[0]), sc.realY(sg[1]), sc.realX(sg[2]), sc.realY(sg[3])) }
                ?.let { fn to it }
        }
    }
    val curvesState = androidx.compose.runtime.rememberUpdatedState(curves)
    // Geometry on the plane: each construction's drawing, in values (A = 1 + 2i, Circle(A, 2), Image(f, c)…).
    val geometry = remember(view, vm.version, params, sc, AppSettings.geometry) {
        val v = view
        if (!AppSettings.geometry || v == null) emptyList() else vm.functions.filter { it.visible && it.geometry != null }.mapNotNull { fn ->
            vm.geometryOf(fn)?.let { o -> runCatching { Triple(fn, o, com.example.cas.graph.Geometry.draw(o, sc.realView(v)) { vm.angleText(it) }) }.getOrNull() }
        }
    }
    val geometryState = androidx.compose.runtime.rememberUpdatedState(geometry)
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current


    // Render coarse first so panning feels live, then sharper once the view settles.
    LaunchedEffect(view, plotKey, size, vm.options, params, f, f?.colormap, f?.colormapReversed, AppSettings.complexQuality, sc) {
        val v = view ?: return@LaunchedEffect
        val c = f?.complexCompiled
        if (c == null || size.width == 0) { image = null; return@LaunchedEffect }
        val p = vm.parameterValues(f!!)
        try {
            // Medium quality stops at half resolution; high renders every pixel.
            // Low stops at a quarter.
            val passes = when (AppSettings.complexQuality) { 1 -> listOf(8 to 0L, 2 to 140L, 1 to 60L); 2 -> listOf(8 to 0L, 4 to 140L); else -> listOf(8 to 0L, 2 to 140L) }
            for ((divisor, wait) in passes) {
                delay(wait)
                // The sharp pass takes a moment: the expressive loading indicator shows meanwhile.
                refining = divisor <= 2
                val w = (size.width / divisor).coerceAtLeast(1)
                val h = (size.height / divisor).coerceAtLeast(1)
                val ctx = coroutineContext
                val px = withContext(Dispatchers.Default) {
                    DomainColoring.render(c, p, v, w, h, vm.options.copy(colormap = f.colormap, reversed = f.colormapReversed), { !ctx.isActive }, sc)
                } ?: return@LaunchedEffect
                image = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
            }
        } finally {
            refining = false
        }
    }

    Box(
        modifier
            .clipToBounds()
            .background(MaterialTheme.colorScheme.surface)
            .onSizeChanged { size = it; if (vm.view == null) vm.resetView(it) }
            .pointerInput(vm.contourMode) {
                if (vm.contourMode) {
                    // Draw a loop with one finger; on release it closes and ∮ f dz is shown.
                    detectDragGestures(
                        onDragStart = { o -> vm.view?.let { v -> vm.contour = listOf(toPlane(v, o, size, sc)); vm.contourResult = null } },
                        onDrag = { change, _ -> vm.view?.let { v -> vm.contour = vm.contour + toPlane(v, change.position, size, sc) } },
                        onDragEnd = { vm.finishContour() },
                    )
                } else {
                    detectAxisTransformGestures(skip = { OverlayTouch.owns(it) }) { centroid, pan, zoomX, zoomY ->
                        val v = vm.view ?: return@detectAxisTransformGestures
                        val w = size.width.toDouble(); val h = size.height.toDouble()
                        vm.view = v.panBy(pan.x / w, pan.y / h).zoomAxes(zoomX, zoomY, centroid.x / w, centroid.y / h)
                        probe = null
                    }
                }
            }
            .pointerInput(Unit) {
                // Drag a construction's free point (A = 1 + 2i, A = (1, 2)) to move it, or a point on
                // a path along it; what's built on it follows. (Reads the latest drawing as it moves.)
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!AppSettings.geometry || OverlayTouch.owns(down)) return@awaitEachGesture
                    val v0 = vm.view ?: return@awaitEachGesture
                    val reach = 28.dp.toPx()
                    val hitFn = geometryState.value.asSequence().filter { (fn, o, _) -> o is com.example.cas.graph.Geometry.Point && vm.isDraggablePoint(fn) }
                        .map { (fn, o, _) -> val p = o as com.example.cas.graph.Geometry.Point; fn to (toScreen(v0, CD(p.x, p.y), size.width.toFloat(), size.height.toFloat(), sc) - down.position).getDistance() }
                        .filter { it.second < reach }.minByOrNull { it.second }?.first ?: return@awaitEachGesture
                    val slop = viewConfiguration.touchSlop
                    var dragging = false
                    var moved = false
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } > 1) break
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (!dragging && (change.position - down.position).getDistance() > slop) { dragging = true; probe = null; hit = null }
                        if (!dragging) continue
                        change.consume()
                        val v = vm.view ?: break
                        val z = toPlane(v, change.position, size, sc)
                        if (hitFn.geometry?.onPath == true) vm.movePathPoint(hitFn, z.re, z.im, first = !moved)
                        else {
                            // Snapped to a tidy value, a hundredth of the view.
                            val snapX = com.example.cas.graph.Plot2D.niceStep(v.width, 100); val snapY = com.example.cas.graph.Plot2D.niceStep(v.height, 100)
                            vm.moveFreePoint(hitFn, Math.round(z.re / snapX) * snapX, Math.round(z.im / snapY) * snapY, first = !moved)
                        }
                        moved = true
                    }
                }
            }
            // Hold and drag to read f(z) continuously as the finger moves.
            .holdToTrace(Unit) { o -> vm.view?.let { v -> hit = null; probe = toPlane(v, o, size, sc) } }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { vm.resetView(size); probe = null; hit = null },
                    onTap = { o ->
                        val v = vm.view ?: return@detectTapGestures
                        // Building with a geometry tool: tap points (or empty space, making one), or objects.
                        vm.geometryTool?.takeIf { AppSettings.geometry }?.let { tool ->
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            constructTapComplex(vm, tool, o, geometryState.value, v, size, sc, 28.dp.toPx())
                            hit = null; probe = null
                            return@detectTapGestures
                        }
                        val from = vm.areaFrom
                        if (from != null) {
                            // The end of an area to the axis: the nearest point of the same curve.
                            vm.areaFrom = null
                            val samples = vm.pathSamples(from.first, v)
                            val end = samples.minByOrNull { (toScreen(v, it.second, size.width, size.height, sc) - o).getDistance() } ?: return@detectTapGestures
                            val (t0, t1) = minOf(from.second, end.first) to maxOf(from.second, end.first)
                            val stretch = samples.filter { it.first in t0..t1 }.map { it.second }
                            if (stretch.size > 1) {
                                val (signed, total) = com.example.cas.graph.PlaneAreas.toAxis(stretch)
                                vm.complexArea = PlaneArea(from.first, 1, total, signed, stretch + CD(stretch.last().re, 0.0) + CD(stretch.first().re, 0.0), shortNumber(t0) + " to " + shortNumber(t1))
                            }
                            return@detectTapGestures
                        }
                        val h = planeHit(vm, curvesState.value, v, o, size, 24.dp.toPx())
                        when {
                            h != null -> { hit = h; probe = null }
                            hit != null || probe != null -> { hit = null; probe = null }
                            else -> probe = toPlane(v, o, size, sc)
                        }
                    },
                )
            }
            .semantics { contentDescription = tr("Complex plane. Color shows the argument of f(z), brightness its size. Drag to move, pinch to zoom, tap to read a value. f can use r and θ for |z| and arg z.") },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // The canvas's own size (in pixels, as Float), not the view's IntSize state of the same name.
            val size = this.size
            val v = view ?: return@Canvas
            image?.let { drawImage(it, dstSize = IntSize(size.width.toInt(), size.height.toInt()), filterQuality = FilterQuality.Low) }
            // The grid (or the polar grid), axes and numbers exactly as on the 2D graph, the numbers
            // with a halo in the surface color so they read over the coloring.
            if (vm.polarGrid && sc.linear) drawPolarGrid(v, if (AppSettings.showGrid) colors.outlineVariant else Color.Transparent, colors.onSurfaceVariant, measurer, vm.angle == com.example.cas.engine.AngleUnit.Degrees, halo = colors.surface)
            else drawGrid(v, if (AppSettings.showGrid) colors.outlineVariant else Color.Transparent, colors.onSurfaceVariant, measurer, ySuffix = "i", halo = colors.surface, scale = sc)
            // Curves, in their line's thickness and style, as on the 2D graph.
            // The first line in the list last, so it's on top.
            curves.asReversed().forEach { (fn, segs) ->
                val lineColor = complexLineColor(fn)
                val path = Path()
                segs.forEach { sg ->
                    val a = toScreen(v, CD(sg[0], sg[1]), size.width, size.height, sc); val b = toScreen(v, CD(sg[2], sg[3]), size.width, size.height, sc)
                    path.moveTo(a.x, a.y); path.lineTo(b.x, b.y)
                }
                drawPath(path, lineColor, style = lineStroke(fn))
            }
            // A picked area, shaded in its line's color.
            vm.complexArea?.let { ar ->
                if (ar.outline.size > 2) {
                    val c = complexLineColor(ar.f)
                    val path = Path().apply { ar.outline.forEachIndexed { k, z -> val o = toScreen(v, z, size.width, size.height, sc); if (k == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }; close() }
                    drawPath(path, c.copy(alpha = 0.3f))
                }
            }
            // Geometry, drawn as on the 2D graph: fills, lines (with a vector's arrowhead), points in
            // their mark and size, names in math italic and values (with a soft halo over the coloring).
            val halo = androidx.compose.ui.graphics.Shadow(colors.surface, blurRadius = 5f)
            val geoLabel = TextStyle(fontFamily = CasFonts.CmItalic, fontSize = 16.sp, color = colors.onSurface, shadow = halo)
            for ((fn, o, d) in geometry) {
                val c = complexLineColor(fn)
                fun at(x: Double, y: Double) = toScreen(v, CD(x, y), size.width, size.height, sc)
                d.fill?.takeIf { it.size > 2 }?.let { fill ->
                    drawPath(Path().apply { fill.forEachIndexed { k, (x, y) -> val q = at(x, y); if (k == 0) moveTo(q.x, q.y) else lineTo(q.x, q.y) }; close() }, c.copy(alpha = fn.fillOpacity.coerceIn(0.05f, 1f)))
                }
                d.lines.forEach { line ->
                    val path = Path()
                    line.forEachIndexed { k, (x, y) -> val q = at(x, y); if (k == 0) path.moveTo(q.x, q.y) else path.lineTo(q.x, q.y) }
                    drawPath(path, c, style = lineStroke(fn))
                    if (d.arrow && line.size >= 2) drawArrowHead(at(line[line.size - 2].first, line[line.size - 2].second), at(line.last().first, line.last().second), c, fn.thickness.dp.toPx())
                }
                val mark = com.example.cas.graph.Marker.of(fn.pointShape)
                d.points.forEach { p -> drawMarker(mark, at(p.x, p.y), fn.pointSize.dp.toPx(), c) }
                val name = fn.geometry?.name
                if (name != null && !fn.hideName && o is com.example.cas.graph.Geometry.Point) {
                    val q = at(o.x, o.y); val t = measurer.measure(geometryLabel(name), geoLabel)
                    drawText(t, topLeft = Offset(q.x + 6.dp.toPx(), q.y - t.size.height - 2.dp.toPx()))
                }
                d.labels.forEach { (p, text) ->
                    val q = at(p.x, p.y); val t = measurer.measure(text, geoLabel.copy(fontFamily = CasFonts.CmRoman, fontSize = 14.sp))
                    drawText(t, topLeft = Offset(q.x - t.size.width / 2f, q.y - t.size.height / 2f))
                }
            }
            // What a tool has picked so far: points ringed, objects in a broad band.
            if (vm.geometryTool != null) vm.geometryPicks.forEach { name ->
                geometry.firstOrNull { it.first.geometry?.name == name }?.let { (_, _, d) ->
                    d.points.forEach { p -> drawCircle(colors.primary, 11.dp.toPx(), toScreen(v, CD(p.x, p.y), size.width, size.height, sc), style = Stroke(2.5.dp.toPx())) }
                    d.lines.forEach { line ->
                        val path = Path()
                        line.forEachIndexed { k, (x, y) -> val q = toScreen(v, CD(x, y), size.width, size.height, sc); if (k == 0) path.moveTo(q.x, q.y) else path.lineTo(q.x, q.y) }
                        drawPath(path, colors.primary.copy(alpha = 0.4f), style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
            }
            hit?.let { h ->
                val o = toScreen(v, h.z, size.width, size.height, sc)
                drawCircle(colors.surface, 5.dp.toPx(), o)
                drawCircle(complexLineColor(h.fn), 5.dp.toPx(), o, style = Stroke(2.dp.toPx()))
            }
            // Points ([1 + i, 2]) and curves z(t) (e^{it}), styled like the curves above.
            fun screen(w: com.example.cas.cas.CD) = toScreen(v, w, size.width, size.height, sc)
            vm.functions.filter { it.visible && (it.complexPoints != null || it.complexPath != null) }.asReversed().forEach { fn ->
                val lineColor = complexLineColor(fn)
                val p = vm.parameterValues(fn)
                fn.complexPath?.let { c ->
                    val (t0, t1) = vm.pathRange(fn, v)
                    val n = com.example.cas.graph.Curves.samplesFor(t0, t1)
                    val path = Path()
                    var pen = false
                    var last: Offset? = null
                    for (k in 0..n) {
                        val w = runCatching { c(com.example.cas.cas.CD(t0 + (t1 - t0) * k / n), p) }.getOrNull()
                        val o = w?.takeIf { it.re.isFinite() && it.im.isFinite() }?.let(::screen)
                        // Breaks where it's undefined or jumps across the screen.
                        if (o == null || (last != null && (o - last!!).getDistance() > size.maxDimension)) { pen = false; last = o; continue }
                        if (pen) path.lineTo(o.x, o.y) else path.moveTo(o.x, o.y)
                        pen = true; last = o
                    }
                    drawPath(path, lineColor, style = lineStroke(fn))
                }
                fn.complexPoints?.let { cs ->
                    val ws = cs.mapNotNull { c -> runCatching { c(com.example.cas.cas.CD(0.0), p) }.getOrNull()?.takeIf { it.re.isFinite() && it.im.isFinite() } }
                    val os = ws.map(::screen)
                    // Joined in order, or closed into a filled polygon.
                    if (os.size > 1 && (fn.connectPoints || fn.closedShape)) {
                        val path = Path().apply { os.forEachIndexed { k, o -> if (k == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }; if (fn.closedShape) close() }
                        if (fn.closedShape) drawPath(path, lineColor.copy(alpha = fn.fillOpacity))
                        drawPath(path, lineColor, style = lineStroke(fn))
                    }
                    val marker = com.example.cas.graph.Marker.of(fn.pointShape)
                    os.forEachIndexed { k, o ->
                        drawMarker(marker, o, fn.pointSize.dp.toPx(), lineColor)
                        if (fn.showLabel) {
                            val t = measurer.measure(complexText(ws[k]), TextStyle(fontFamily = CasFonts.Ui, fontSize = 12.sp, color = colors.onSurface, shadow = androidx.compose.ui.graphics.Shadow(colors.surface, blurRadius = 5f)))
                            drawText(t, topLeft = Offset(o.x + 8.dp.toPx(), o.y - t.size.height - 4.dp.toPx()))
                        }
                    }
                }
            }
            // Typed contour integrals: the circle, an arrow showing it runs counterclockwise, and the value.
            vm.functions.filter { it.visible && it.contour != null }.asReversed().forEach { fn ->
                val c = fn.contour!!
                // Its own plain color, white unless one was picked.
                val lineColor = complexLineColor(fn)
                // The circle point by point, so it's placed right on log axes too.
                val ring = Path()
                for (k in 0..120) {
                    val a = 2 * Math.PI * k / 120
                    val o = toScreen(v, CD(c.centerRe + c.radius * kotlin.math.cos(a), c.centerIm + c.radius * kotlin.math.sin(a)), size.width, size.height, sc)
                    if (k == 0) ring.moveTo(o.x, o.y) else ring.lineTo(o.x, o.y)
                }
                drawPath(ring, lineColor, style = lineStroke(fn))
                // Arrowhead at the right of the circle, pointing up (counterclockwise).
                val right = toScreen(v, CD(c.centerRe + c.radius, c.centerIm), size.width, size.height, sc)
                val tip = Offset(right.x, right.y - 8.dp.toPx())
                val arrow = Path().apply {
                    moveTo(tip.x, tip.y); lineTo(tip.x - 6.dp.toPx(), tip.y + 10.dp.toPx()); lineTo(tip.x + 6.dp.toPx(), tip.y + 10.dp.toPx()); close()
                }
                drawPath(arrow, lineColor)
                // (Its value is on a result card, top left.)
            }
            // The drawn loop.
            if (vm.contour.size > 1) {
                val path = Path()
                vm.contour.forEachIndexed { k, z ->
                    val o = toScreen(v, z, size.width, size.height, sc)
                    if (k == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                }
                if (vm.contourResult != null) path.close()
                drawPath(path, Color.Black.copy(alpha = 0.5f), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(path, Color.White, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            probe?.let { z ->
                val o = toScreen(v, z, size.width, size.height, sc)
                drawCircle(Color.Black, 7.dp.toPx(), o)
                drawCircle(Color.White, 5.dp.toPx(), o)
            }
        }
        if (vm.areaFrom != null) Text(tr("Tap where the area should end"),
            color = colors.inverseOnSurface,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp).clip(CircleShape).background(colors.inverseSurface).padding(horizontal = 14.dp, vertical = 8.dp),
        )
        // A tapped point or curve: its z (and t, and f(z) if a function is colored), with areas.
        val h = hit
        if (h != null && view != null) {
            val o = toScreen(view, h.z, size.width.toFloat(), size.height.toFloat(), sc)
            fun use(v: CD): () -> Unit = { hit = null; onUseValue(v) }
            val w = f?.complexCompiled?.let { c -> runCatching { c(h.z, vm.parameterValues(f)) }.getOrNull()?.takeIf { it.re.isFinite() && it.im.isFinite() } }
            val rows = listOfNotNull(
                h.t?.let { t -> CardValue("t", shortNumber(t), onUse = use(CD(t))) },
                CardValue("z", complexText(h.z), polarText(h.z), onUse = use(h.z)),
                w?.let { CardValue("f(z)", complexText(it), polarText(it), onUse = use(it)) },
            )
            PointCardAt(o.x, o.y, complexLineColor(h.fn), null, legendSource(h.fn).takeIf { it.isNotBlank() }, rows, planeActions(vm, h, view, size, curves) { hit = null }, onClose = { hit = null })
        }
        // Value readout for the tapped point.
        if (refining) Busy(Modifier.align(Alignment.TopCenter).padding(top = 12.dp))
        val z = probe
        if (z != null && view != null && f?.complexCompiled != null) {
            val w = runCatching { f.complexCompiled!!(z, vm.parameterValues(f)) }.getOrNull()?.takeIf { it.re.isFinite() && it.im.isFinite() }
            val o = toScreen(view, z, size.width.toFloat(), size.height.toFloat(), sc)
            fun use(v: CD): () -> Unit = { probe = null; onUseValue(v) }
            // The same card as the 2D graph's: z, and f(z), each with its polar form.
            PointCardAt(
                o.x, o.y, null, null, legendSource(f).takeIf { it.isNotBlank() },
                listOf(
                    CardValue("z", complexText(z), polarText(z), onUse = use(z)),
                    if (w != null) CardValue("f(z)", complexText(w), polarText(w), onUse = use(w)) else CardValue("f(z)", "undefined"),
                ),
                onClose = { probe = null },
            )
        }
    }
}

/** The value of z under a point of the screen (on a log axis, 10 to the view's coordinate). */
/** A line's own thickness and style (solid, dashed, dotted…), as on the 2D graph. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.lineStroke(fn: PlotFunction): Stroke {
    val w = fn.thickness.dp.toPx()
    val dash = com.example.cas.graph.LineStyle.of(fn.lineStyle).pattern(w.toDouble())?.let { d -> androidx.compose.ui.graphics.PathEffect.dashPathEffect(FloatArray(d.size) { d[it].toFloat() }) }
    return Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = dash)
}

private fun toPlane(v: Viewport, o: Offset, size: IntSize, sc: com.example.cas.graph.AxisScale) =
    CD(sc.realX(v.xMin + o.x / size.width * v.width), sc.realY(v.yMax - o.y / size.height * v.height))

/** Where z is on the screen; on a log axis a part at or below 0 goes far off the edge. */
private fun toScreen(v: Viewport, z: CD, w: Float, h: Float, sc: com.example.cas.graph.AxisScale): Offset {
    val x = sc.x(z.re).let { if (it.isFinite()) it else v.xMin - 10 * v.width }
    val y = sc.y(z.im).let { if (it.isFinite()) it else v.yMin - 10 * v.height }
    return Offset(((x - v.xMin) / v.width * w).toFloat(), ((v.yMax - y) / v.height * h).toFloat())
}

private fun toScreen(v: Viewport, z: CD, w: Int, h: Int, sc: com.example.cas.graph.AxisScale) = toScreen(v, z, w.toFloat(), h.toFloat(), sc)

/** Polar form |z|∠arg z, with the angle in radians as a multiple of π when it's a simple one. */
fun polarText(z: CD): String {
    if (!z.re.isFinite() || !z.im.isFinite()) return "∞"
    val m = z.abs()
    if (m < 1e-12) return "0"
    val a = z.arg()
    val k = a / Math.PI * 12
    val nice = kotlin.math.abs(k - Math.rint(k)) < 1e-9
    val angle = if (nice) {
        val n = Math.rint(k).toInt()
        val g = gcd(kotlin.math.abs(n), 12).coerceAtLeast(1)
        val num = n / g; val den = 12 / g
        when {
            num == 0 -> "0"
            den == 1 -> (if (num == 1) "" else if (num == -1) "−" else shortNumber(num.toDouble())) + "π"
            else -> (if (num == 1) "" else if (num == -1) "−" else shortNumber(num.toDouble())) + "π/" + den
        }
    } else shortNumber(a)
    return shortNumber(m) + "∠" + angle
}

private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

fun complexText(z: CD): String {
    if (!z.re.isFinite() || !z.im.isFinite()) return "∞"
    val re = shortNumber(z.re)
    if (kotlin.math.abs(z.im) < 1e-10) return re
    val im = shortNumber(kotlin.math.abs(z.im)).let { if (it == "1") "" else it } + "i"
    return if (kotlin.math.abs(z.re) < 1e-10) (if (z.im < 0) "−" else "") + im else re + (if (z.im < 0) " − " else " + ") + im
}

/** A floating toolbar of toggles: modulus bands, phase lines, the conformal grid and the polar grid. */
@Composable
private fun androidx.compose.foundation.layout.RowScope.PlotTools(vm: ComplexViewModel) {
    var settings by remember { mutableStateOf(false) }
    if (settings) vm.view?.let { v -> ComplexSettingsDialog(vm, v, onDismiss = { settings = false }) }
    run {
        ToolToggle(PlotIcons.Bands, "Modulus bands", vm.options.modulusBands) { vm.options = vm.options.copy(modulusBands = !vm.options.modulusBands) }
        ToolToggle(PlotIcons.Phase, "Phase lines", vm.options.phaseLines) { vm.options = vm.options.copy(phaseLines = !vm.options.phaseLines) }
        ToolToggle(PlotIcons.Grid, "Grid lines of Re f and Im f", vm.options.grid) { vm.options = vm.options.copy(grid = !vm.options.grid) }
        // Circles of |z| mean nothing on log axes: turning the grid on goes back to linear ones.
        ToolToggle(PlotIcons.PolarGrid, "Polar grid: circles of |z| and rays of arg z", vm.polarGrid) { if (!vm.polarGrid) vm.setLogAxes(false, false); vm.polarGrid = !vm.polarGrid }
        IconButton(onClick = { settings = true }) {
            Icon(Icons.Default.Tune, contentDescription = tr("Graph settings"), tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/**
 * The complex plane over [view] as a square [Scene] [size] units across, framed like pgfplots
 * (see [Pgf]): the domain coloring as a picture (rendered afresh for the export) with the polar
 * grid, curves, ∮ loops and a drawn loop as lines on top, Re z and Im z on the axes.
 */
internal fun complexScene(vm: ComplexViewModel, view: Viewport, size: Double, dark: Boolean, quick: Boolean = false): Scene {
    val style = if (dark) Pgf.Style.DARK else Pgf.Style.LIGHT
    val scene = Scene(size, size, style.background)
    val v = view
    val frame = Pgf.frame(size)
    // Values placed through the axis scale (log₁₀ on a log axis); off the frame where they have no place.
    val sc = vm.scale
    fun sx(x: Double) = Pgf.sx(v, frame, sc.x(x).let { if (it.isFinite()) it else v.xMin - 10 * v.width })
    fun sy(y: Double) = Pgf.sy(v, frame, sc.y(y).let { if (it.isFinite()) it else v.yMin - 10 * v.height })
    val white = 0xFFFFFFFF.toInt()
    scene.add(Scene.ClipStart(frame.left, frame.top, frame.width, frame.height))
    val f = vm.plotted
    val c = f?.complexCompiled
    if (f != null && c != null) {
        // Four pixels per unit (half a pixel for the dialog's preview).
        val scale = if (quick) 0.5 else 4.0
        val pw = (frame.width * scale).toInt().coerceAtLeast(1); val ph = (frame.height * scale).toInt().coerceAtLeast(1)
        DomainColoring.render(c, vm.parameterValues(f), v, pw, ph, vm.options.copy(colormap = f.colormap, reversed = f.colormapReversed), scale = sc)?.let { px ->
            scene.add(Scene.Image(frame.left, frame.top, frame.width, frame.height, px, pw, ph))
        }
    }
    if (vm.polarGrid && sc.linear) {
        val ox = sx(0.0); val oy = sy(0.0)
        val corners = listOf(v.xMin to v.yMin, v.xMin to v.yMax, v.xMax to v.yMin, v.xMax to v.yMax)
        val far = corners.maxOf { (x, y) -> kotlin.math.hypot(x, y) }
        val step = Plot2D.niceStep(minOf(v.width, v.height) / 2, 4)
        val ux = frame.width / v.width; val uy = frame.height / v.height
        val lines = ArrayList<DoubleArray>()
        var r = step
        while (r <= far) { lines += DoubleArray(2 * 97) { k -> val t = (k / 2) * 2 * Math.PI / 96; if (k % 2 == 0) ox + r * ux * kotlin.math.cos(t) else oy - r * uy * kotlin.math.sin(t) }; r += step }
        val reach = far * maxOf(ux, uy) + size
        lines += (0 until 12).map { a -> val ang = a * Math.PI / 6; doubleArrayOf(ox, oy, ox + reach * kotlin.math.cos(ang), oy - reach * kotlin.math.sin(ang)) }
        scene.add(Scene.Stroke(lines, 0x8CFFFFFF.toInt(), 0.5))
    }
    // Curves and loops in their color (no outline, as on screen and on the 2D graph).
    fun outlined(paths: List<DoubleArray>, color: Int) {
        scene.add(Scene.Stroke(paths, color, 1.3))
    }
    vm.functions.filter { it.visible && it.complexCurve != null }.asReversed().forEach { fn ->
        val g = fn.complexCurve!!
        // Found in the view's coordinates, so placed directly.
        val segs = runCatching { com.example.cas.graph.Curves.implicit(sc.function2 { x, y -> vm.call(fn, g, x, y) }, v, 200, 200) }.getOrNull() ?: return@forEach
        outlined(segs.map { sg -> doubleArrayOf(Pgf.sx(v, frame, sg[0]), Pgf.sy(v, frame, sg[1]), Pgf.sx(v, frame, sg[2]), Pgf.sy(v, frame, sg[3])) }, complexLineColor(fn).toArgb())
    }
    vm.functions.filter { it.visible && it.contour != null }.asReversed().forEach { fn ->
        val cc = fn.contour!!
        val cx = sx(cc.centerRe); val cy = sy(cc.centerIm)
        val rx = sx(cc.centerRe + cc.radius) - cx; val ry = cy - sy(cc.centerIm + cc.radius)
        val color = complexLineColor(fn).toArgb()
        // Point by point, so it's placed right on log axes too.
        outlined(listOf(DoubleArray(2 * 97) { k -> val t = (k / 2) * 2 * Math.PI / 96; if (k % 2 == 0) sx(cc.centerRe + cc.radius * kotlin.math.cos(t)) else sy(cc.centerIm + cc.radius * kotlin.math.sin(t)) }), color)
        // Arrowhead at the right, pointing up (counterclockwise).
        val tx = cx + rx; val ty = cy - 5
        scene.add(Scene.Fill(listOf(doubleArrayOf(tx, ty, tx - 4, ty + 7, tx + 4, ty + 7)), color))
        val shown = runCatching { roundedComplex(com.example.cas.cas.Numeric.eval(cc.value)) }.getOrDefault("?")
        scene.add(Scene.Label(minOf(cx + rx * 0.72, frame.right - 4), maxOf(cy - ry * 0.72 - 8, frame.top + 10), "Integral ≈ $shown", 13.0, white, Scene.Anchor.Start, Scene.Font.Roman, italic = setOf('i')))
    }
    // Points and curves z(t), as on screen: points with their marks.
    vm.functions.filter { it.visible && (it.complexPoints != null || it.complexPath != null) }.asReversed().forEach { fn ->
        val color = complexLineColor(fn).toArgb()
        if (fn.complexPath != null) {
            val runs = ArrayList<DoubleArray>()
            var cur = ArrayList<Double>()
            var last: Pair<Double, Double>? = null
            for ((_, w) in vm.pathSamples(fn, v)) {
                val o = if (w.re.isFinite() && w.im.isFinite()) sx(w.re) to sy(w.im) else null
                // Breaks where it's undefined or jumps across the frame.
                if (o == null || (last != null && kotlin.math.hypot(o.first - last!!.first, o.second - last!!.second) > size)) {
                    if (cur.size >= 4) runs += cur.toDoubleArray()
                    cur = ArrayList(); last = o
                    if (o != null) { cur += o.first; cur += o.second }
                    continue
                }
                cur += o.first; cur += o.second; last = o
            }
            if (cur.size >= 4) runs += cur.toDoubleArray()
            outlined(runs, color)
        }
        if (fn.complexPoints != null) {
            val ws = vm.pointsOf(fn)
            val pts = ws.map { sx(it.re) to sy(it.im) }
            if (pts.size > 1 && (fn.connectPoints || fn.closedShape)) {
                val closed = if (fn.closedShape && pts.size > 2) pts + pts.first() else pts
                val flat = closed.flatMap { listOf(it.first, it.second) }.toDoubleArray()
                if (fn.closedShape && pts.size > 2) scene.add(Scene.Fill(listOf(flat), ((255 * fn.fillOpacity).toInt().coerceIn(0, 255) shl 24) or (color and 0xFFFFFF)))
                outlined(listOf(flat), color)
            }
            val marker = com.example.cas.graph.Marker.of(fn.pointShape)
            pts.forEachIndexed { k, (x, y) ->
                marker.addTo(scene, x, y, fn.pointSize * 0.43, color)
                if (fn.showLabel) scene.add(Scene.Label(x + 5, y - 9, complexText(ws[k]), Pgf.TICK_SIZE * 0.85, white, Scene.Anchor.Start, Scene.Font.Roman, italic = setOf('i')))
            }
        }
    }
    // Constructions, as on screen: fills, lines in their style (vectors with an arrowhead), points
    // with their marks, and names in math italic.
    if (AppSettings.geometry) vm.functions.filter { it.visible && it.geometry != null }.asReversed().forEach { fn ->
        val o = vm.geometryOf(fn) ?: return@forEach
        val d = runCatching { com.example.cas.graph.Geometry.draw(o, sc.realView(v)) { vm.angleText(it) } }.getOrNull() ?: return@forEach
        val color = complexLineColor(fn).toArgb()
        d.fill?.takeIf { it.size > 2 }?.let { fill ->
            scene.add(Scene.Fill(listOf(fill.flatMap { (x, y) -> listOf(sx(x), sy(y)) }.toDoubleArray()), ((255 * fn.fillOpacity.coerceIn(0.05f, 1f)).toInt() shl 24) or (color and 0xFFFFFF)))
        }
        val width = fn.thickness * 0.65
        val dash = com.example.cas.graph.LineStyle.of(fn.lineStyle).pattern(width)
        if (d.lines.isNotEmpty()) scene.add(Scene.Stroke(d.lines.map { line -> line.flatMap { (x, y) -> listOf(sx(x), sy(y)) }.toDoubleArray() }, color, width, dash))
        if (d.arrow) d.lines.forEach { line ->
            if (line.size < 2) return@forEach
            val (x0, y0) = line[line.size - 2]; val (x1, y1) = line.last()
            val ax = sx(x1); val ay = sy(y1); val dx = ax - sx(x0); val dy = ay - sy(y0); val len = kotlin.math.hypot(dx, dy)
            if (len > 0) {
                val ux = dx / len; val uy = dy / len; val h = 6.0
                scene.add(Scene.Fill(listOf(doubleArrayOf(ax, ay, ax - ux * h - uy * h * 0.45, ay - uy * h + ux * h * 0.45, ax - ux * h + uy * h * 0.45, ay - uy * h - ux * h * 0.45)), color))
            }
        }
        val marker = com.example.cas.graph.Marker.of(fn.pointShape)
        d.points.forEach { p -> marker.addTo(scene, sx(p.x), sy(p.y), fn.pointSize * 0.43, color) }
        val name = fn.geometry?.name
        if (name != null && !fn.hideName && o is com.example.cas.graph.Geometry.Point) {
            scene.add(Scene.Label(sx(o.x) + 4, sy(o.y) - 6, name, Pgf.TICK_SIZE, white, Scene.Anchor.Start, Scene.Font.Italic))
        }
        d.labels.forEach { (p, text) -> scene.add(Scene.Label(sx(p.x), sy(p.y), text, Pgf.TICK_SIZE * 0.9, white, Scene.Anchor.Middle, Scene.Font.Roman)) }
    }
    if (vm.contour.size > 1) {
        val pts = DoubleArray(vm.contour.size * 2 + if (vm.contourResult != null) 2 else 0)
        vm.contour.forEachIndexed { k, z -> pts[2 * k] = sx(z.re); pts[2 * k + 1] = sy(z.im) }
        if (vm.contourResult != null) { pts[pts.size - 2] = pts[0]; pts[pts.size - 1] = pts[1] }
        outlined(listOf(pts), white)
    }
    scene.add(Scene.ClipEnd)
    // Re and Im upright, z and i in math italic, as LaTeX sets them.
    Pgf.axes(scene, v, frame, style, xName = "Re z", yName = "Im z", ySuffix = "i", nameFont = Scene.Font.Roman, italic = setOf('z', 'i'), scale = sc)
    if (AppSettings.showLegend) {
        val entries = complexLegendLines(vm).map { fn ->
            val name = com.example.cas.graph.Legend.row(legendSource(fn))
    val spans = com.example.cas.graph.Legend.spans(name)
            when {
                fn === f -> Pgf.LegendEntry(math = name, spans = spans, color = style.ink, line = false, strip = (0..7).map { k -> 0xFF000000.toInt() or fn.colormap.rgb(if (fn.colormapReversed) 1 - k / 7.0 else k / 7.0) })
                // Points: their mark (on a line when they're joined).
                fn.complexPoints != null -> Pgf.LegendEntry(math = name, spans = spans, color = complexLineColor(fn).toArgb(), line = fn.connectPoints || fn.closedShape, width = 1.3,
                    marker = com.example.cas.graph.Marker.of(fn.pointShape), markerSize = fn.pointSize * 0.43)
                fn.geometry != null && vm.constructionKind(fn) == 'P' -> Pgf.LegendEntry(math = name, spans = spans, color = complexLineColor(fn).toArgb(), line = false,
                    marker = com.example.cas.graph.Marker.of(fn.pointShape), markerSize = fn.pointSize * 0.43)
                else -> Pgf.LegendEntry(math = name, spans = spans, color = complexLineColor(fn).toArgb(), width = 1.3)
            }
        }
        Pgf.legend(scene, frame, style, entries, panel = (style.background and 0xFFFFFF) or 0xD9000000.toInt())
    }
    return scene
}

/** The complex plane's lines in the legend: the colored function, then curves, ∮ loops, points and paths z(t). */
internal fun complexLegendLines(vm: ComplexViewModel): List<PlotFunction> =
    vm.functions.filter { fn ->
        fn.visible && !fn.isText && legendSource(fn).isNotBlank() &&
            (fn === vm.plotted || fn.complexCurve != null || fn.contour != null || fn.complexPoints != null || fn.complexPath != null || fn.geometry != null)
    }

@Composable
internal fun ToolToggle(icon: ImageVector, label: String, on: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tap = rememberKeyTap()
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (on) colors.primary else Color.Transparent)
            .clickable(onClickLabel = label) { tap(); onClick() }
            .semantics { contentDescription = label; selected = on },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (on) colors.onPrimary else colors.onSurface, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ContourCard(integral: CD, onClose: () -> Unit, onUse: (CD) -> Unit, math: com.example.cas.editor.MathRow? = null) {
    val residues = DomainColoring.residueSum(integral)
    val shownMath = math ?: remember { com.example.cas.graph.Legend.row("$\\oint f(z)\\,dz$") }
    // The integral large; the sum of the residues inside (the integral over 2πi) under it.
    ResultCard(
        PlotIcons.Loop, "Loop integral", shownMath, "≈ " + roundedComplex(integral),
        stats = listOf("Residues inside" to roundedComplex(residues)),
        note = "For a loop drawn counterclockwise.",
        copyText = roundedComplex(integral).replace("−", "-"),
        onUse = { onUse(integral) },
        onClose = onClose,
    )
}


/** a + bi with at most 4 decimals, trailing zeros dropped, and parts too small to matter left out. */
fun roundedComplex(z: CD): String {
    if (!z.re.isFinite() || !z.im.isFinite()) return "undefined"
    val scale = maxOf(kotlin.math.abs(z.re), kotlin.math.abs(z.im), 1e-300)
    fun part(v: Double) = java.math.BigDecimal(v).setScale(4, java.math.RoundingMode.HALF_EVEN).stripTrailingZeros().toPlainString()
        .let { if (it == "-0") "0" else it }
    val re = if (kotlin.math.abs(z.re) < 1e-9 * scale || kotlin.math.abs(z.re) < 5e-5) 0.0 else z.re
    val im = if (kotlin.math.abs(z.im) < 1e-9 * scale || kotlin.math.abs(z.im) < 5e-5) 0.0 else z.im
    return when {
        im == 0.0 -> part(re)
        re == 0.0 -> (if (im == 1.0) "" else if (im == -1.0) "-" else part(im)) + "i"
        else -> part(re) + (if (im < 0) " − " else " + ") + part(kotlin.math.abs(im)).let { if (it == "1") "" else it } + "i"
    }.replace("-", "−")
}


/** The complex plane's settings: its limits typed exactly, linear or log for each axis, and how finely it's colored. */
@Composable
private fun ComplexSettingsDialog(vm: ComplexViewModel, view: Viewport, onDismiss: () -> Unit) {
    val range = remember { RangeFields(view, vm.scale) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Graph settings")) },
        text = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                RangeAndScaleSettings(range, "Re z", "Im z")
                Text(tr("Plot quality"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    // Shown low to high; stored as 2 (low), 0 (medium), 1 (high).
                    listOf("Low" to 2, "Medium" to 0, "High" to 1).forEachIndexed { k, (name, value) ->
                        SegmentedButton(
                            selected = AppSettings.complexQuality == value,
                            onClick = { AppSettings.changeComplexQuality(value) },
                            shape = SegmentedButtonDefaults.itemShape(k, 3),
                            icon = {},
                            label = { Text(name) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(enabled = range.valid, onClick = { range.apply(vm) { vm.view = it }; onDismiss() }) { Text(tr("Done")) }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}

/** What a tap on the plane landed on: a line's point, curve or loop, at [z] (and [t] on a curve z(t)). */
class PlaneHit(val fn: PlotFunction, val z: CD, val t: Double? = null)

/**
 * The point, curve z(t), equation curve or ∮ circle nearest the tap, within [reach] pixels:
 * points first (they're small targets), then whichever line is closest.
 */
private fun planeHit(vm: ComplexViewModel, curves: List<Pair<PlotFunction, List<DoubleArray>>>, v: Viewport, o: Offset, size: IntSize, reach: Float): PlaneHit? {
    val sc = vm.scale
    fun screen(z: CD) = toScreen(v, z, size.width, size.height, sc)
    val lines = vm.functions.filter { it.visible }
    // Points.
    lines.filter { it.complexPoints != null }.flatMap { fn -> vm.pointsOf(fn).map { fn to it } }
        .minByOrNull { (screen(it.second) - o).getDistance() }
        ?.takeIf { (screen(it.second) - o).getDistance() <= reach }
        ?.let { return PlaneHit(it.first, it.second) }
    var best: PlaneHit? = null
    var bestD = reach
    fun offer(fn: PlotFunction, z: CD, t: Double? = null) {
        val d = (screen(z) - o).getDistance()
        if (d <= bestD) { bestD = d; best = PlaneHit(fn, z, t) }
    }
    // Curves z(t): the nearest sample.
    lines.filter { it.complexPath != null }.forEach { fn -> vm.pathSamples(fn, v).forEach { (t, z) -> offer(fn, z, t) } }
    // Joined points: the nearest point on each edge.
    lines.filter { it.complexPoints != null && (it.connectPoints || it.closedShape) }.forEach { fn ->
        val ps = vm.pointsOf(fn).let { if (fn.closedShape && it.size > 2) it + it.first() else it }
        for (k in 1 until ps.size) offer(fn, nearestOnSegment(ps[k - 1], ps[k], toPlane(v, o, size, sc)))
    }
    // Equation curves, from their drawn segments.
    curves.forEach { (fn, segs) -> segs.forEach { sg -> offer(fn, nearestOnSegment(CD(sg[0], sg[1]), CD(sg[2], sg[3]), toPlane(v, o, size, sc))) } }
    // ∮ circles.
    lines.filter { it.contour != null }.forEach { fn ->
        val c = fn.contour!!
        val p = toPlane(v, o, size, sc)
        val d = p - CD(c.centerRe, c.centerIm)
        if (d.abs() > 0) offer(fn, CD(c.centerRe, c.centerIm) + d * CD(c.radius / d.abs()))
    }
    return best
}

private fun nearestOnSegment(a: CD, b: CD, p: CD): CD {
    val ab = b - a
    val len = ab.re * ab.re + ab.im * ab.im
    if (len == 0.0) return a
    val t = (((p.re - a.re) * ab.re + (p.im - a.im) * ab.im) / len).coerceIn(0.0, 1.0)
    return CD(a.re + t * ab.re, a.im + t * ab.im)
}

/** A line as a polyline, for crossings: a curve's samples, or joined points. */
private fun outlineOf(vm: ComplexViewModel, fn: PlotFunction, v: Viewport): List<CD>? = when {
    fn.complexPath != null -> vm.pathSamples(fn, v).map { it.second }
    fn.complexPoints != null && (fn.connectPoints || fn.closedShape) -> vm.pointsOf(fn).let { if (fn.closedShape && it.size > 2) it + it.first() else it }
    else -> null
}

/** The areas a tapped line offers: inside it when it's closed, to the axis from here, between it and a curve crossing here. */
private fun planeActions(vm: ComplexViewModel, h: PlaneHit, view: Viewport, size: IntSize, curves: List<Pair<PlotFunction, List<DoubleArray>>>, done: () -> Unit): List<CardAction> {
    // Areas and distances in values: the view's extent as values on log axes.
    val v = vm.scale.realView(view)
    val fn = h.fn
    val out = ArrayList<CardAction>()
    fun inside(area: () -> PlaneArea?) = CardAction(TabIcons.Area, "Area inside", "Area inside this curve") { area()?.let { vm.complexArea = it }; done() }
    when {
        fn.complexPath != null -> {
            if (vm.closedPath(fn, v)) out += inside {
                val pts = vm.pathSamples(fn, v).map { it.second }
                val a = com.example.cas.graph.PlaneAreas.shoelace(pts)
                PlaneArea(fn, 0, kotlin.math.abs(a), a, pts)
            }
            h.t?.let { t -> out += CardAction(TabIcons.Area, "To the axis", "Area to the real axis from here") { vm.complexArea = null; vm.areaFrom = fn to t; done() } }
        }
        fn.complexPoints != null && fn.closedShape -> {
            val pts = vm.pointsOf(fn)
            if (pts.size > 2) out += inside { val a = com.example.cas.graph.PlaneAreas.shoelace(pts); PlaneArea(fn, 0, kotlin.math.abs(a), a, pts) }
        }
        fn.complexCurve != null -> out += inside {
            val a = vm.insideArea(fn, v) ?: return@inside null
            val segs = curves.firstOrNull { it.first === fn }?.second.orEmpty()
            // Shaded as its drawn outline is: the segments in order around the center.
            val pts = segs.map { CD((it[0] + it[2]) / 2, (it[1] + it[3]) / 2) }
            val c = if (pts.isEmpty()) CD(0.0) else CD(pts.sumOf { it.re } / pts.size, pts.sumOf { it.im } / pts.size)
            PlaneArea(fn, 0, a, null, pts.sortedBy { (it - c).arg() }, "In view")
        }
        fn.contour != null -> out += inside {
            val c = fn.contour!!
            val pts = (0 until 240).map { k -> CD(c.centerRe + c.radius * kotlin.math.cos(2 * Math.PI * k / 240), c.centerIm + c.radius * kotlin.math.sin(2 * Math.PI * k / 240)) }
            PlaneArea(fn, 0, Math.PI * c.radius * c.radius, null, pts)
        }
    }
    // Another line crossing here: the region between them, to their next crossing.
    val mine = outlineOf(vm, fn, v)
    if (mine != null) {
        val reach = 0.04 * maxOf(v.width, v.height)
        for (other in vm.functions.filter { it.visible && it !== fn }) {
            val theirs = outlineOf(vm, other, v) ?: continue
            val a = com.example.cas.graph.PlaneAreas.thin(mine); val b = com.example.cas.graph.PlaneAreas.thin(theirs)
            if (com.example.cas.graph.PlaneAreas.crossings(a, b).none { (it.at - h.z).abs() < reach }) continue
            out += CardAction(TabIcons.AreaBetween, "Between", "Area between the curves from here") {
                com.example.cas.graph.PlaneAreas.between(a, b, h.z)?.let { region ->
                    vm.complexArea = PlaneArea(fn, 2, kotlin.math.abs(com.example.cas.graph.PlaneAreas.shoelace(region)), null, region)
                }
                done()
            }
            break
        }
    }
    return out
}

/** The area picked on the plane, on the same card as the 2D graph's areas. */
@Composable
private fun ComplexAreaCard(ar: PlaneArea, onClose: () -> Unit, onUse: (Double) -> Unit) {
    val title = when (ar.kind) { 1 -> "Area to the real axis"; 2 -> "Area between the curves"; else -> "Area inside" }
    val math = remember(ar) { legendSource(ar.f).takeIf { it.isNotBlank() }?.let { com.example.cas.graph.Legend.row(it) } }
    // To the axis: the signed integral ∫ y dx and the range of t; inside an equation's curve: counted in view.
    val stats = listOfNotNull(
        ar.signed?.takeIf { ar.kind == 1 }?.let { "Signed" to shortNumber(it) },
        ar.detail?.let { if (ar.kind == 1) "t from" to it else "Counted" to "in the view" },
    )
    ResultCard(
        if (ar.kind == 2) TabIcons.AreaBetween else TabIcons.Area, title, math, "≈ " + shortNumber(ar.total),
        stats = stats,
        copyText = ar.total.toString(),
        onUse = { onUse(ar.total) },
        onClose = onClose,
    )
}

/**
 * A tap with a geometry tool on the complex plane: on a construction's point (named if it
 * wasn't), on an object of the kind the tool needs, or on empty space, which makes a point there.
 */
private fun constructTapComplex(
    vm: ComplexViewModel, tool: GeometryTool, tap: Offset,
    geometry: List<Triple<PlotFunction, com.example.cas.graph.Geometry.Obj, com.example.cas.graph.Geometry.Drawing>>,
    v: Viewport, size: IntSize, sc: com.example.cas.graph.AxisScale, reach: Float,
) {
    fun at(x: Double, y: Double) = toScreen(v, CD(x, y), size.width, size.height, sc)
    fun pointNear(): String? = geometry.filter { it.second is com.example.cas.graph.Geometry.Point }
        .map { (fn, o, _) -> val p = o as com.example.cas.graph.Geometry.Point; fn to (at(p.x, p.y) - tap).getDistance() }
        .filter { it.second < reach }.minByOrNull { it.second }?.first?.let { vm.nameLine(it) }
    fun fits(kind: Char, o: com.example.cas.graph.Geometry.Obj) = when (kind) {
        'L' -> o is com.example.cas.graph.Geometry.Line || o is com.example.cas.graph.Geometry.Segment || o is com.example.cas.graph.Geometry.Ray || o is com.example.cas.graph.Geometry.Vector
        'C' -> o is com.example.cas.graph.Geometry.Circle || o is com.example.cas.graph.Geometry.Conic || o is com.example.cas.graph.Geometry.Arc
        'V' -> o is com.example.cas.graph.Geometry.Vector
        'F' -> false
        else -> o !is com.example.cas.graph.Geometry.Point && o !is com.example.cas.graph.Geometry.Number && o !is com.example.cas.graph.Geometry.Angle
    }
    fun segmentDistance(a: Offset, c: Offset): Float {
        val d = c - a; val len2 = d.x * d.x + d.y * d.y
        val t = if (len2 == 0f) 0f else (((tap.x - a.x) * d.x + (tap.y - a.y) * d.y) / len2).coerceIn(0f, 1f)
        return (tap - Offset(a.x + d.x * t, a.y + d.y * t)).getDistance()
    }
    fun objectNear(kind: Char): String? = geometry.filter { fits(kind, it.second) }.mapNotNull { (fn, _, d) ->
        val dist = d.lines.minOfOrNull { line -> line.map { (x, y) -> at(x, y) }.zipWithNext { a, c -> segmentDistance(a, c) }.minOrNull() ?: Float.MAX_VALUE } ?: return@mapNotNull null
        fn to dist
    }.filter { it.second < reach }.minByOrNull { it.second }?.first?.let { vm.nameLine(it) }
    fun newPoint(): String {
        val z = toPlane(v, tap, size, sc)
        val snapX = com.example.cas.graph.Plot2D.niceStep(v.width, 100); val snapY = com.example.cas.graph.Plot2D.niceStep(v.height, 100)
        return vm.addFreePoint(Math.round(z.re / snapX) * snapX, Math.round(z.im / snapY) * snapY)
    }
    when (tool) {
        GeometryTool.Move -> {}
        GeometryTool.Point -> if (pointNear() == null) newPoint()
        GeometryTool.PointOn -> objectNear('O')?.let { val z = toPlane(v, tap, size, sc); vm.addPointOn(it, z.re, z.im) }
        else -> when (val need = vm.geometryNeeds ?: 'P') {
            'P' -> vm.pickForTool(pointNear() ?: newPoint())
            'X' -> (pointNear() ?: objectNear('O'))?.let { vm.pickForTool(it) }
            else -> objectNear(need)?.let { vm.pickForTool(it) }
        }
    }
}
