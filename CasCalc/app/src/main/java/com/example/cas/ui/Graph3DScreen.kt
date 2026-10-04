package com.example.cas.ui

import com.example.cas.engine.Readout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cas.graph.Bounds
import com.example.cas.graph.Camera
import com.example.cas.graph.Face
import com.example.cas.graph.Polygon
import com.example.cas.graph.Surface3D
import com.example.cas.graph.Geometry3D
import com.example.cas.ui.theme.CasFonts
import kotlin.math.abs
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Edit
import com.example.cas.graph.Coordinates3D
import androidx.compose.ui.graphics.toArgb
import com.example.cas.graph.Viewport
import com.example.cas.graph.Scene
import kotlin.math.PI

@Composable
fun Graph3DScreen(vm: Graph3DViewModel, onUseValue: (Double) -> Unit = {}, modifier: Modifier = Modifier) {
    BackHandler(enabled = vm.active != null) { vm.edit(null) }
    var plotSize by remember { mutableStateOf(IntSize.Zero) }
    var exporting by remember { mutableStateOf(false) }
    val build = { r: ExportRequest -> surface3DScene(vm, r, EXPORT_SIZE, r.dark) }
    val export = rememberGraphExporter("graph-3d", build, model = { r -> stlModel(vm, r) })
    val writeFile = rememberGraphFileWriter()
    if (exporting) {
        val b = surfaceBounds(vm)
        ExportDialog(
            Viewport(vm.xMin, vm.xMax, vm.yMin, vm.yMax), build, onExport = { r, share -> exporting = false; export(r, share) }, onDismiss = { exporting = false },
            z = b.z0 to b.z1, formats = com.example.cas.graph.ExportFormat.entries,
            graphFile = com.example.cas.graph.GraphFile.Kind.Graph3D,
            onGraphFile = { share -> exporting = false; writeFile(com.example.cas.graph.GraphFile.Contents(com.example.cas.graph.GraphFile.Kind.Graph3D, "graph-3d", vm.graphData()), share) },
        )
    }
    // Geometry mode: lines read again when it's turned on or off; editing a line on a phone closes construct mode.
    androidx.compose.runtime.LaunchedEffect(AppSettings.geometry) { vm.refreshAll(); if (!AppSettings.geometry) vm.stopConstructing() }
    val phone = !isTabletLayout()
    androidx.compose.runtime.LaunchedEffect(vm.active, phone) { if (phone && vm.active != null) vm.stopConstructing() }
    GraphScaffold(vm, outputLabel = "z", modifier = modifier) {
        Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth().onSizeChanged { plotSize = it }) {
            SurfaceCanvas(vm, Modifier.fillMaxSize(), onUseValue)
            // The range control top left, and the legend under it.
            Column(Modifier.align(Alignment.TopStart).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RangeControl(vm)
                val version = vm.version
                val theme = (0 until GraphViewModel.PLOT_COLOR_COUNT).map { k -> plotColor(k) }
                GraphLegend(remember(version, theme) { legend3D(vm, theme) })
            }
            var settings by remember { mutableStateOf(false) }
            if (settings) LimitsDialog(vm, onDismiss = { settings = false })
            GraphBottomBar(vm, Modifier.align(Alignment.BottomCenter), onExport = { if (plotSize.width > 0) exporting = true }, tools = {
                // The coordinate system: Cartesian, cylindrical or spherical, always exactly one.
                ToolToggle(PlotIcons.Cartesian, "Cartesian coordinates (x, y, z)", vm.coordinates3D == Coordinates3D.Mode.Cartesian) {
                    vm.setCoordinates(Coordinates3D.Mode.Cartesian)
                }
                ToolToggle(PlotIcons.Cylindrical, "Cylindrical coordinates (r, θ, z)", vm.coordinates3D == Coordinates3D.Mode.Cylindrical) {
                    vm.setCoordinates(Coordinates3D.Mode.Cylindrical)
                }
                ToolToggle(PlotIcons.Spherical, "Spherical coordinates (ρ, θ, φ)", vm.coordinates3D == Coordinates3D.Mode.Spherical) {
                    vm.setCoordinates(Coordinates3D.Mode.Spherical)
                }
                IconButton(onClick = { settings = true }) {
                    Icon(Icons.Default.Tune, contentDescription = "Graph settings", tint = MaterialTheme.colorScheme.onSurface)
                    // (geometry's construct UI follows the bar, below)
                }
            })
            // A tool's number (a radius, an angle), asked once its taps are done.
            vm.pendingAsk?.let { ask -> ToolNumberDialog(ask.tool, onDone = { vm.answerAsk(it) }, onDismiss = { vm.pendingAsk = null }) }
            // Geometry in space: Construct starts building; the status card on top, the tools in a rail on a tablet.
            if (AppSettings.geometry) {
                if (!vm.constructing) ConstructButton(onClick = { vm.constructing = true }, modifier = Modifier.align(Alignment.TopEnd).padding(top = 34.dp, end = 10.dp))
                else if (isTabletLayout()) {
                    val byList = if (AppSettings.keypadSide == 0) Alignment.TopStart else Alignment.TopEnd
                    ConstructStatus(vm, Modifier.align(Alignment.TopCenter).padding(top = 10.dp, start = 140.dp, end = 140.dp))
                    ConstructRail(vm, Modifier.align(byList).padding(top = 10.dp, bottom = 84.dp, start = 10.dp, end = 10.dp))
                } else ConstructStatus(vm, Modifier.align(Alignment.TopCenter).padding(top = 10.dp, start = 12.dp, end = 12.dp))
            }
        }
        // On a phone the tools sit under the graph, in the list's place.
        if (AppSettings.geometry && vm.constructing && phone) {
            ConstructPalette(vm, Modifier.background(MaterialTheme.colorScheme.surface).padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 6.dp))
        }
        }
    }
}

/** "x ∈ [a, b], y ∈ [c, d]" in LaTeX, for MathText. */
private fun limitsText(vm: Graph3DViewModel): String {
    val (lx, ly) = vm.letters3D.getValue(Coordinates3D.Mode.Cartesian)
    fun n(v: Double) = Readout.latex(shortNumber(v))
    return "\$$lx \\in [${n(vm.xMin)}, ${n(vm.xMax)}]\$, \$$ly \\in [${n(vm.yMin)}, ${n(vm.yMax)}]\$"
}

/** − and + zoom the ranges; tapping the ranges opens the limits. */
@Composable
private fun RangeControl(vm: Graph3DViewModel, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val tap = rememberKeyTap()
    var editing by remember { mutableStateOf(false) }
    if (editing) LimitsDialog(vm, onDismiss = { editing = false })
    Row(modifier.clip(CircleShape).background(colors.surfaceContainerHigh), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { tap(); vm.scaleRanges(0.5) }) {
            Icon(Icons.Default.Remove, contentDescription = "Zoom in", tint = colors.onSurface)
        }
        MathText(
            limitsText(vm),
            color = colors.onSurface,
            style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 15.sp),
            mathScale = 1f,
            modifier = Modifier.clickable(onClickLabel = "Set the limits") { editing = true }.padding(vertical = 8.dp),
        )
        IconButton(onClick = { tap(); vm.scaleRanges(2.0) }) {
            Icon(Icons.Default.Add, contentDescription = "Zoom out", tint = colors.onSurface)
        }
    }
}

/** The 3D graph's settings: limits for x, y and (optionally) z, the coordinates, and the surface detail. */
@Composable
private fun LimitsDialog(vm: Graph3DViewModel, onDismiss: () -> Unit) {
    fun text(v: Double) = shortNumber(v).replace("−", "-")
    var x0 by remember { mutableStateOf(text(vm.xMin)) }
    var x1 by remember { mutableStateOf(text(vm.xMax)) }
    var y0 by remember { mutableStateOf(text(vm.yMin)) }
    var y1 by remember { mutableStateOf(text(vm.yMax)) }
    var autoZ by remember { mutableStateOf(vm.zRange == null) }
    var z0 by remember { mutableStateOf(vm.zRange?.first?.let { text(it) } ?: "-3") }
    var z1 by remember { mutableStateOf(vm.zRange?.second?.let { text(it) } ?: "3") }
    fun num(s: String) = s.trim().replace("−", "-").toDoubleOrNull()
    val xs = num(x0) to num(x1)
    val ys = num(y0) to num(y1)
    val zs = num(z0) to num(z1)
    fun ok(p: Pair<Double?, Double?>) = p.first != null && p.second != null && p.first!! < p.second!!
    // Your letters for each coordinate system (i, j, k for x, y, z…), edited here.
    val letters = remember { androidx.compose.runtime.mutableStateMapOf<Coordinates3D.Mode, List<String>>().apply { putAll(vm.letters3D) } }
    fun lettersOk(l: List<String>) = l.size == 3 && l.all { it.isNotBlank() && it.length <= 3 } && l.toSet().size == 3
    val valid = ok(xs) && ok(ys) && (autoZ || ok(zs)) && letters.values.all(::lettersOk)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Graph settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LimitRow("x", x0, x1, { x0 = it }, { x1 = it })
                LimitRow("y", y0, y1, { y0 = it }, { y1 = it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Fit z to the surfaces", modifier = Modifier.weight(1f))
                    Switch(checked = autoZ, onCheckedChange = { autoZ = it })
                }
                if (!autoZ) LimitRow("z", z0, z1, { z0 = it }, { z1 = it })
                if (!valid) Text("Each lower limit must be below its upper limit.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Text("Coordinates", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // As on the calculator's ∇ tab: the systems by their letters, and ✎ to choose them.
                var choosing by remember { mutableStateOf(false) }
                if (choosing) {
                    val m = vm.coordinates3D
                    val roles = when (m) {
                        Coordinates3D.Mode.Cartesian -> listOf("First (across)", "Second (depth)", "Third (up)")
                        Coordinates3D.Mode.Cylindrical -> listOf("Distance from the axis", "Angle around the axis", "Height")
                        Coordinates3D.Mode.Spherical -> listOf("Distance from the origin", "Angle around the z-axis", "Angle down from the z-axis")
                    }
                    val name = when (m) { Coordinates3D.Mode.Cartesian -> "Cartesian"; Coordinates3D.Mode.Cylindrical -> "Cylindrical"; else -> "Spherical" }
                    CoordinateLettersDialog("$name coordinates", roles, letters.getValue(m), Coordinates3D.DEFAULT_LETTERS.getValue(m), onDone = { letters[m] = it; choosing = false }, onDismiss = { choosing = false })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                    Coordinates3D.Mode.entries.forEachIndexed { k, m ->
                        SegmentedButton(
                            selected = vm.coordinates3D == m,
                            onClick = { vm.setCoordinates(m) },
                            shape = SegmentedButtonDefaults.itemShape(k, 3),
                            icon = {},
                            label = { Text(letters.getValue(m).joinToString("\u200A"), maxLines = 1, style = TextStyle(fontFamily = CasFonts.CmItalic, fontSize = 16.sp)) },
                        )
                    }
                }
                IconButton(onClick = { choosing = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Choose the coordinate letters", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                }

                Text("Surface detail", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("Low", "Medium", "High").forEachIndexed { k, name ->
                        SegmentedButton(
                            selected = AppSettings.surfaceDetail == k,
                            onClick = { AppSettings.changeSurfaceDetail(k) },
                            shape = SegmentedButtonDefaults.itemShape(k, 3),
                            icon = {},
                            label = { Text(name, maxLines = 1) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                vm.xMin = xs.first!!; vm.xMax = xs.second!!
                vm.yMin = ys.first!!; vm.yMax = ys.second!!
                vm.zRange = if (autoZ) null else zs.first!! to zs.second!!
                letters.forEach { (m, l) -> if (l != vm.letters3D[m]) vm.setLetters3D(m, l) }
                onDismiss()
            }) { Text("Done") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun LimitRow(letter: String, lo: String, hi: String, onLo: (String) -> Unit, onHi: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(lo, onLo, singleLine = true, label = { Text("from") }, modifier = Modifier.weight(1f))
        Text(letter, style = TextStyle(fontFamily = CasFonts.CmItalic, fontSize = 22.sp))
        OutlinedTextField(hi, onHi, singleLine = true, label = { Text("to") }, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SurfaceCanvas(vm: Graph3DViewModel, modifier: Modifier, onUseValue: (Double) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    var size by remember { mutableStateOf(IntSize.Zero) }
    var picked by remember { mutableStateOf<DoubleArray?>(null) }
    val version = vm.version
    // Low and high colors of each surface's height gradient, following picked colors.
    val gradients = (0 until GraphViewModel.PLOT_COLOR_COUNT).map { k ->
        val c = vm.functions.firstOrNull { it.colorIndex == k }?.customColor?.let { Color(it) } ?: plotColor(k)
        lerp(c, Color.Black, 0.35f) to lerp(c, Color.White, 0.45f)
    }
    val params = vm.parameters.toMap()
    val limits = listOf(vm.xMin, vm.xMax, vm.yMin, vm.yMax)

    // The box: x and y as set; z as set, or fitted to the explicit surfaces.
    val bounds = remember(version, limits, vm.zRange, params) { surfaceBounds(vm) }
    val surfaces = remember(version, bounds, params, AppSettings.surfaceDetail) { surfacePolygons(vm, bounds) }
    // Geometry in space: each construction's drawing; its faces (planes, spheres, solids) join the
    // surfaces so they're sorted by depth with them, see-through.
    val geometry = remember(version, bounds, params, AppSettings.geometry) {
        if (!AppSettings.geometry) emptyList() else vm.functions.filter { it.visible && it.geometry != null }.mapNotNull { f ->
            vm.geometry3DOf(f)?.let { o -> Triple(f, o, runCatching { Geometry3D.draw(o, bounds) { vm.angleText(it) } }.getOrNull() ?: return@let null) }
        }
    }
    val geometryState = androidx.compose.runtime.rememberUpdatedState(geometry)
    val boundsState = androidx.compose.runtime.rememberUpdatedState(bounds)
    val polygons = remember(surfaces, geometry) { surfaces + geometry.flatMap { (f, _, d) -> d.faces.map { Polygon(it, f.colorIndex, wall = true) } } }
    val palette = (0 until GraphViewModel.PLOT_COLOR_COUNT).map { plotColor(it) }
    fun colorOf(f: PlotFunction) = f.customColor?.let { Color(it) } ?: palette[f.colorIndex % palette.size]
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val camera = vm.camera
    val faces: List<Face> = remember(polygons, camera, size) {
        if (size.width == 0) emptyList() else Surface3D.faces(polygons, bounds, camera, size.width.toFloat(), size.height.toFloat())
    }
    // The surface point under the finger: its (x, y, z), with z exact on a z = f(x, y) surface.
    // (x, y, z, and the surface's color slot.)
    fun pickAt(o: Offset): DoubleArray? = Surface3D.pick(faces, o.x, o.y)?.let { hit ->
        val c = hit.center.copyOf(4)
        c[3] = hit.surface.toDouble()
        vm.functions.firstOrNull { it.colorIndex == hit.surface && it.implicit3D == null && it.compiled != null }
            ?.let { f -> vm.evaluate(f, c[0], c[1]).takeIf { v -> v.isFinite() }?.let { c[2] = it } }
        c
    }

    Box(
        modifier
            .clipToBounds()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                // Drag to turn the surface, pinch to zoom.
                // (A touch that starts on the point card is the card's.)
                detectAxisTransformGestures(skip = { OverlayTouch.owns(it) }) { _, pan, zoomX, zoomY ->
                    vm.camera = vm.camera.rotateBy(-pan.x * 0.008, pan.y * 0.008).zoomBy(kotlin.math.sqrt(zoomX * zoomY))
                    picked = null
                }
            }
            .pointerInput(Unit) {
                // Drag a construction's free point (A = (1, 2, 3)) to move it level, its height kept;
                // what's built on it follows. (Reads the latest drawing as it moves.)
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!AppSettings.geometry || OverlayTouch.owns(down)) return@awaitEachGesture
                    val b0 = boundsState.value; val cam0 = vm.camera
                    val w = size.width.toFloat(); val h = size.height.toFloat()
                    val reach = 28.dp.toPx()
                    val hit = geometryState.value.asSequence().filter { (f, o, _) -> o is Geometry3D.Point && vm.isDraggablePoint(f) }
                        .map { (f, o, _) -> val p = o as Geometry3D.Point; Triple(f, p, Surface3D.project(p.x, p.y, p.z, b0, cam0, w, h).let { (Offset(it.first, it.second) - down.position).getDistance() }) }
                        .filter { it.third < reach }.minByOrNull { it.third } ?: return@awaitEachGesture
                    val (f, p0, _) = hit
                    val slop = viewConfiguration.touchSlop
                    var dragging = false
                    var moved = false
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } > 1) break
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (!dragging && (change.position - down.position).getDistance() > slop) { dragging = true; picked = null }
                        if (!dragging) continue
                        change.consume()
                        // In the box as it was when the drag began, so the point doesn't run as the box refits.
                        val (x, y) = levelPointUnder(change.position, p0.z, b0, cam0, w, h)
                        vm.moveFreePoint3(f, x, y, p0.z, first = !moved)
                        moved = true
                    }
                }
            }
            // Hold and drag over the surface to read (x, y, z) continuously.
            .holdToTrace(faces) { o -> pickAt(o)?.let { picked = it } }
            .pointerInput(faces, geometry) {
                detectTapGestures(
                    onDoubleTap = { vm.camera = Camera(); picked = null },
                    // Tap the surface to read its (x, y, z) there; with a tool, tap points and objects to build.
                    onTap = { o ->
                        val tool = vm.geometryTool
                        if (tool != null && AppSettings.geometry) {
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            constructTap3D(vm, tool, o, geometry, bounds, camera, size.width.toFloat(), size.height.toFloat(), 28.dp.toPx())
                            picked = null
                        } else picked = pickAt(o)
                    },
                )
            }
            .semantics { contentDescription = "3D graph. Drag to rotate, pinch to zoom, tap the surface to read a point, double-tap to reset." },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // The canvas's own size (in pixels, as Float), not the view's IntSize state of the same name.
            val size = this.size
            val w = size.width
            val h = size.height
            val edge = colors.outlineVariant
            Surface3D.box(camera, w, h).forEach { drawLine(edge, Offset(it.x1, it.y1), Offset(it.x2, it.y2), 1.dp.toPx()) }
            // Cylindrical or spherical guides: rings and rays on the floor, or a wire sphere.
            coordinateGuides(vm.coordinates3D, bounds).forEach { line ->
                val path = Path()
                line.forEachIndexed { k, p ->
                    val (sx, sy) = Surface3D.project(p[0], p[1], p[2], bounds, camera, w, h)
                    if (k == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
                }
                drawPath(path, edge.copy(alpha = 0.8f), style = Stroke(0.8.dp.toPx()))
            }
            val wire = colors.onSurface.copy(alpha = 0.12f)
            val hairline = Stroke(0.6.dp.toPx())
            for (face in faces) {
                val path = Path().apply {
                    moveTo(face.xs[0], face.ys[0])
                    for (k in 1 until face.xs.size) lineTo(face.xs[k], face.ys[k])
                    close()
                }
                val (lo, hi) = gradients[face.surface % gradients.size]
                val base = lerp(lo, hi, face.height)
                // A solid's walls (where the box cuts it) are lighter and see-through, as a cut face.
                val lit = if (face.wall) lerp(base, Color.White, 0.35f).copy(alpha = 0.55f)
                    else Color(base.red * face.shade, base.green * face.shade, base.blue * face.shade, 1f)
                drawPath(path, lit)
                if (face.xs.size == 4) drawPath(path, wire, style = hairline)
            }
            val labelStyle = TextStyle(fontFamily = CasFonts.CmItalic, fontSize = 18.sp, color = colors.onSurfaceVariant)
            val names = vm.letters3D.getValue(Coordinates3D.Mode.Cartesian)
            Surface3D.axisLabels(camera, w, h).forEachIndexed { k, (_, p) ->
                val t = measurer.measure(names[k], labelStyle)
                drawText(t, topLeft = Offset(p.first - t.size.width / 2f, p.second - t.size.height / 2f))
            }
            val style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 11.sp, color = colors.onSurfaceVariant)
            val zText = measurer.measure("${names[2]} from ${shortNumber(bounds.z0)} to ${shortNumber(bounds.z1)}", style)
            drawText(zText, topLeft = Offset(w - zText.size.width - 12.dp.toPx(), 12.dp.toPx()))
            // Points and space curves, drawn over the surfaces.
            vm.functions.filter { it.visible && it.space != null }.asReversed().forEach { fn ->
                val (fx, fy, fz) = fn.space!!
                val (lo, hi) = gradients[fn.colorIndex % gradients.size]
                val color = lerp(lo, hi, 0.5f)
                if (fn.spaceIsCurve) {
                    // Over one period if it repeats every 2π, otherwise for −10 ≤ t ≤ 10.
                    val periodic = listOf(0.3, 1.1, 2.9).all { t -> abs(vm.call(fn, fx, t) - vm.call(fn, fx, t + 2 * PI)) < 1e-9 && abs(vm.call(fn, fz, t) - vm.call(fn, fz, t + 2 * PI)) < 1e-9 }
                    val (a, b) = if (periodic) 0.0 to 2 * PI else -10.0 to 10.0
                    val path = Path()
                    var started = false
                    for (k in 0..600) {
                        val t = a + (b - a) * k / 600
                        val x = vm.call(fn, fx, t); val y = vm.call(fn, fy, t); val z = vm.call(fn, fz, t)
                        if (!x.isFinite() || !y.isFinite() || !z.isFinite()) { started = false; continue }
                        val (sx, sy) = Surface3D.project(x, y, z, bounds, camera, w, h)
                        if (started) path.lineTo(sx, sy) else { path.moveTo(sx, sy); started = true }
                    }
                    drawPath(path, color, style = Stroke(3.dp.toPx()))
                } else {
                    val x = vm.call(fn, fx); val y = vm.call(fn, fy); val z = vm.call(fn, fz)
                    if (x.isFinite() && y.isFinite() && z.isFinite()) {
                        val (sx, sy) = Surface3D.project(x, y, z, bounds, camera, w, h)
                        drawCircle(color, 7.dp.toPx(), Offset(sx, sy))
                        drawCircle(colors.surface, 2.5.dp.toPx(), Offset(sx, sy))
                    }
                }
            }
            // Geometry's lines, points and labels over the faces.
            val labelText = TextStyle(fontFamily = CasFonts.CmItalic, fontSize = 15.sp, color = colors.onSurface)
            for ((f, o, d) in geometry) {
                val c = colorOf(f)
                fun at(p: DoubleArray) = Surface3D.project(p[0], p[1], p[2], bounds, camera, w, h).let { Offset(it.first, it.second) }
                d.lines.forEach { line ->
                    val path = Path()
                    line.forEachIndexed { k, p -> val q = at(p); if (k == 0) path.moveTo(q.x, q.y) else path.lineTo(q.x, q.y) }
                    drawPath(path, c, style = Stroke(f.thickness.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }
                // A vector's arrowhead, at its end on screen.
                if (d.arrow) d.lines.firstOrNull()?.takeIf { it.size >= 2 }?.let { line ->
                    val e = at(line.last()); val s0 = at(line[line.size - 2])
                    val dir = (e - s0).let { v -> val l = v.getDistance(); if (l < 1e-3) null else v / l }
                    if (dir != null) {
                        val head = 11.dp.toPx(); val n = Offset(-dir.y, dir.x)
                        drawPath(Path().apply { moveTo(e.x, e.y); lineTo(e.x - dir.x * head + n.x * head * 0.45f, e.y - dir.y * head + n.y * head * 0.45f); lineTo(e.x - dir.x * head - n.x * head * 0.45f, e.y - dir.y * head - n.y * head * 0.45f); close() }, c)
                    }
                }
                d.points.forEach { p ->
                    val q = at(p)
                    drawCircle(c, (f.pointSize * 0.75f).dp.toPx().coerceAtLeast(4.dp.toPx()), q)
                    drawCircle(colors.surface, 2.dp.toPx(), q)
                }
                // The point's name beside it, and an angle's size at its arc.
                val name = f.geometry?.name
                if (name != null && !f.hideName && o is Geometry3D.Point) d.points.firstOrNull()?.let { p ->
                    val q = at(p); val t = measurer.measure(geometryLabel(name), labelText)
                    drawText(t, topLeft = Offset(q.x + 7.dp.toPx(), q.y - t.size.height - 2.dp.toPx()))
                }
                d.labels.forEach { (p, text) ->
                    val q = at(p); val t = measurer.measure(text, labelText.copy(fontFamily = CasFonts.CmRoman, fontSize = 13.sp))
                    drawText(t, topLeft = Offset(q.x - t.size.width / 2f, q.y - t.size.height / 2f))
                }
            }
            // What a tool has picked so far: points ringed.
            if (vm.geometryTool != null) vm.geometryPicks.forEach { name ->
                geometry.firstOrNull { it.first.geometry?.name == name }?.let { (_, o, d) ->
                    if (o is Geometry3D.Point) d.points.firstOrNull()?.let { p ->
                        val (sx, sy) = Surface3D.project(p[0], p[1], p[2], bounds, camera, w, h)
                        drawCircle(colors.primary, 11.dp.toPx(), Offset(sx, sy), style = Stroke(2.5.dp.toPx()))
                    }
                    d.lines.forEach { line ->
                        val path = Path()
                        line.forEachIndexed { k, p -> val (sx, sy) = Surface3D.project(p[0], p[1], p[2], bounds, camera, w, h); if (k == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy) }
                        drawPath(path, colors.primary.copy(alpha = 0.35f), style = Stroke(9.dp.toPx()))
                    }
                }
            }
            picked?.let { p ->
                val (sx, sy) = Surface3D.project(p[0], p[1], p[2], bounds, camera, w, h)
                drawCircle(colors.inverseSurface, 7.dp.toPx(), Offset(sx, sy))
                drawCircle(colors.inverseOnSurface, 3.dp.toPx(), Offset(sx, sy))
            }
        }
        // The picked point's coordinates, on the same card as the 2D graph's.
        picked?.let { p ->
            val (sx, sy) = Surface3D.project(p[0], p[1], p[2], bounds, camera, size.width.toFloat(), size.height.toFloat())
            val fn = vm.functions.firstOrNull { it.colorIndex == p[3].toInt() && it.visible && !it.isText }
            val color = fn?.let { f -> f.customColor?.let { Color(it) } ?: plotColor(f.colorIndex) }
            fun use(v: Double): () -> Unit = { picked = null; onUseValue(v) }
            PointCardAt(
                sx, sy, color, null, fn?.let { legendSource(it) }?.takeIf { it.isNotBlank() },
                listOf(CardValue("x", shortNumber(p[0]), onUse = use(p[0])), CardValue("y", shortNumber(p[1]), onUse = use(p[1])), CardValue("z", shortNumber(p[2]), onUse = use(p[2]))),
                onClose = { picked = null },
            )
        }
    }
}


/** The box: x and y as set; z as set, or fitted to the explicit surfaces. */
private fun surfaceBounds(vm: Graph3DViewModel, x0: Double = vm.xMin, x1: Double = vm.xMax, y0: Double = vm.yMin, y1: Double = vm.yMax): Bounds {
    val fns = vm.functions.filter { it.visible }
    val z = vm.zRange ?: run {
        val fitted = fns.filter { it.compiled != null }.map { f -> Surface3D.autoZ({ x, y -> vm.evaluate(f, x, y) }, x0, x1, y0, y1) }
        if (fitted.isEmpty()) x0 to x1 else fitted.minOf { it.first } to fitted.maxOf { it.second }
    }
    return Bounds(x0, x1, y0, y1, z.first, z.second)
}

/** Every visible surface as polygons, at the 3D detail setting's grid sizes. */
private fun surfacePolygons(vm: Graph3DViewModel, bounds: Bounds): List<Polygon> =
    vm.functions.filter { it.visible }.flatMap { f -> runCatching {
        val implicit = f.implicit3D
        when {
            // An inequality: the solid, its boundary and the walls of the box inside it.
            implicit != null && f.region3D -> Surface3D.solid({ x, y, z -> vm.call(f, implicit, x, y, z) }, bounds, AppSettings.surfaceGrid.second, f.colorIndex)
            implicit != null -> Surface3D.implicit({ x, y, z -> vm.call(f, implicit, x, y, z) }, bounds, AppSettings.surfaceGrid.second, f.colorIndex)
            f.compiled != null -> Surface3D.explicit({ x, y -> vm.evaluate(f, x, y) }, bounds, AppSettings.surfaceGrid.first, f.colorIndex)
            else -> emptyList<Polygon>()
        }
    }.getOrElse { emptyList() } }

/**
 * The 3D graph for exporting, drawn like a pgfplots 3D axis (see [com.example.cas.graph.Pgf3D]),
 * seen from the current camera: the back walls with their grid, surfaces as pgfplots' faceted
 * "surf" plots shaded from dark to light by height in the export colors (one per line, in list order), points and
 * space curves, then the tick labels and axis names. Square, [size] units across.
 */
internal fun surface3DScene(vm: Graph3DViewModel, r: ExportRequest, size: Double, dark: Boolean): Scene {
    val style = if (dark) com.example.cas.graph.Pgf.Style.DARK else com.example.cas.graph.Pgf.Style.LIGHT
    val scene = Scene(size, size, style.background)
    val base = surfaceBounds(vm, r.view.xMin, r.view.xMax, r.view.yMin, r.view.yMax)
    val bounds = r.z?.let { (a, b) -> base.copy(z0 = a, z1 = b) } ?: base
    // A little further back than on screen, so the labels fit round the box.
    val camera = vm.camera.copy(zoom = vm.camera.zoom * 0.78)
    val fw = size.toFloat()
    com.example.cas.graph.Pgf3D.back(scene, bounds, camera, size, style)
    val mesh = Color(style.ink).copy(alpha = 0.3f).toArgb()
    // Exported, each surface, curve and point takes the export colors in turn (list order): a
    // surface as a gradient of its color from dark (low) to light (high).
    val drawn = vm.functions.filter { drawn3D(it) }
    fun exportColor(f: PlotFunction?) = style.cycle[(drawn.indexOf(f).coerceAtLeast(0)) % style.cycle.size]
    fun shade(c: Int, t: Float) = lerp(lerp(Color(c), Color.Black, 0.35f), lerp(Color(c), Color.White, 0.45f), t).toArgb()
    val faces = Surface3D.faces(surfacePolygons(vm, bounds), bounds, camera, fw, fw)
    for (face in faces) {
        val pts = DoubleArray(face.xs.size * 2) { k -> if (k % 2 == 0) face.xs[k / 2].toDouble() else face.ys[k / 2].toDouble() }
        val fill = shade(exportColor(drawn.firstOrNull { it.colorIndex == face.surface }), face.height)
        // A solid's walls: lighter and see-through.
        scene.add(Scene.Fill(listOf(pts), if (face.wall) (lerp(Color(fill), Color.White, 0.35f).toArgb() and 0xFFFFFF) or 0x8C000000.toInt() else fill))
        scene.add(Scene.Stroke(listOf(pts + doubleArrayOf(pts[0], pts[1])), mesh, 0.3))
    }
    // Cylindrical or spherical guides, light like the grid.
    coordinateGuides(vm.coordinates3D, bounds).forEach { line ->
        scene.add(Scene.Stroke(listOf(line.flatMap { q -> Surface3D.project(q[0], q[1], q[2], bounds, camera, fw, fw).let { (a, b) -> listOf(a.toDouble(), b.toDouble()) } }.toDoubleArray()), style.grid, 0.5))
    }
    vm.functions.filter { it.visible && it.space != null }.asReversed().forEach { fn ->
        val (fx, fy, fz) = fn.space!!
        val color = exportColor(fn)
        if (fn.spaceIsCurve) {
            val periodic = listOf(0.3, 1.1, 2.9).all { t -> abs(vm.call(fn, fx, t) - vm.call(fn, fx, t + 2 * PI)) < 1e-9 && abs(vm.call(fn, fz, t) - vm.call(fn, fz, t + 2 * PI)) < 1e-9 }
            val (a, b) = if (periodic) 0.0 to 2 * PI else -10.0 to 10.0
            val paths = ArrayList<DoubleArray>()
            var cur = ArrayList<Double>()
            for (k in 0..600) {
                val t = a + (b - a) * k / 600
                val x = vm.call(fn, fx, t); val y = vm.call(fn, fy, t); val z = vm.call(fn, fz, t)
                if (!x.isFinite() || !y.isFinite() || !z.isFinite()) { if (cur.size >= 4) paths += cur.toDoubleArray(); cur = ArrayList(); continue }
                val (sx, sy) = Surface3D.project(x, y, z, bounds, camera, fw, fw)
                cur += sx.toDouble(); cur += sy.toDouble()
            }
            if (cur.size >= 4) paths += cur.toDoubleArray()
            scene.add(Scene.Stroke(paths, color, 1.5))
        } else {
            val x = vm.call(fn, fx); val y = vm.call(fn, fy); val z = vm.call(fn, fz)
            if (x.isFinite() && y.isFinite() && z.isFinite()) {
                val (sx, sy) = Surface3D.project(x, y, z, bounds, camera, fw, fw)
                scene.add(Scene.Circle(sx.toDouble(), sy.toDouble(), 2.8, fill = color))
            }
        }
    }
    val names = vm.letters3D.getValue(Coordinates3D.Mode.Cartesian)
    com.example.cas.graph.Pgf3D.front(scene, bounds, camera, size, style, Triple(names[0], names[1], names[2]))
    if (AppSettings.showLegend) {
        val entries = vm.functions.filter { drawn3D(it) && legendSource(it).isNotBlank() }.map { f ->
            val name = com.example.cas.graph.Legend.row(legendSource(f))
    val spans = com.example.cas.graph.Legend.spans(name)
            val color = exportColor(f)
            when {
                f.space != null && f.spaceIsCurve -> com.example.cas.graph.Pgf.LegendEntry(math = name, spans = spans, color =  color, width = 1.5)
                f.space != null -> com.example.cas.graph.Pgf.LegendEntry(math = name, spans = spans, color =  color, line = false, marker = com.example.cas.graph.Marker.Circle, markerSize = 2.8)
                else -> {
                    // A surface: its shades, low to high.
                    val strip = (0..5).map { k -> shade(color, k / 5f) }
                    com.example.cas.graph.Pgf.LegendEntry(math = name, spans = spans, color =  color, line = false, strip = strip)
                }
            }
        }
        com.example.cas.graph.Pgf.legend(scene, com.example.cas.graph.Pgf.Frame(0.0, 0.0, size, size), style, entries)
    }
    return scene
}

/**
 * The 3D graph as an STL model over the export's limits: z = f(x, y) as the solid under it (down
 * to the floor of the box), inequalities as their solids, other surfaces as they are.
 */
internal fun stlModel(vm: Graph3DViewModel, r: ExportRequest): ByteArray {
    val base = surfaceBounds(vm, r.view.xMin, r.view.xMax, r.view.yMin, r.view.yMax)
    val b = r.z?.let { (a, c) -> base.copy(z0 = a, z1 = c) } ?: base
    val n = AppSettings.surfaceGrid.second
    val polys = vm.functions.filter { it.visible && !it.isText }.flatMap { f ->
        runCatching {
            val implicit = f.implicit3D
            when {
                implicit != null && f.region3D -> Surface3D.solid({ x, y, z -> vm.call(f, implicit, x, y, z) }, b, n, f.colorIndex)
                implicit != null -> Surface3D.implicit({ x, y, z -> vm.call(f, implicit, x, y, z) }, b, n, f.colorIndex)
                f.compiled != null -> Surface3D.solid({ x, y, z -> z - vm.evaluate(f, x, y) }, b, n, f.colorIndex)
                else -> emptyList()
            }
        }.getOrElse { emptyList() }
    }
    return com.example.cas.graph.Stl.write(polys, b)
}

/** Whether a 3D line draws something: a surface, a point or a space curve. */
private fun drawn3D(f: PlotFunction) = f.visible && !f.isText && (f.compiled != null || f.implicit3D != null || f.space != null)

/** The 3D legend on screen: surfaces as a strip of their shades, curves as a stroke, points as a dot. */
private fun legend3D(vm: Graph3DViewModel, theme: List<Color>): List<ScreenLegendEntry> =
    vm.functions.filter { drawn3D(it) && legendSource(it).isNotBlank() }.map { f ->
        val c = f.customColor?.let { Color(it) } ?: theme[f.colorIndex % theme.size]
        when {
            f.space != null && f.spaceIsCurve -> ScreenLegendEntry(legendSource(f), c)
            f.space != null -> ScreenLegendEntry(legendSource(f), c, line = false, marker = com.example.cas.graph.Marker.Circle)
            else -> ScreenLegendEntry(legendSource(f), c, line = false, strip = (0..5).map { k -> lerp(lerp(c, Color.Black, 0.35f), lerp(c, Color.White, 0.45f), k / 5f) })
        }
    }


/**
 * Guides for the 3D coordinates, as polylines of (x, y, z) points: in cylindrical, circles of
 * constant r and rays every 30° on the floor of the box; in spherical, a wire sphere of
 * latitude and longitude circles. Nothing in Cartesian (the box is the guide).
 */
internal fun coordinateGuides(mode: Coordinates3D.Mode, b: Bounds): List<List<DoubleArray>> {
    val cx = (b.x0 + b.x1) / 2; val cy = (b.y0 + b.y1) / 2
    val half = minOf(b.x1 - b.x0, b.y1 - b.y0) / 2
    fun circle(r: Double, z: Double, x0: Double = cx, y0: Double = cy) = (0..64).map { k -> val t = k * 2 * PI / 64; doubleArrayOf(x0 + r * kotlin.math.cos(t), y0 + r * kotlin.math.sin(t), z) }
    return when (mode) {
        Coordinates3D.Mode.Cartesian -> emptyList()
        Coordinates3D.Mode.Cylindrical -> {
            val step = com.example.cas.graph.Plot2D.niceStep(half, 3)
            val rings = generateSequence(step) { it + step }.takeWhile { it <= half + 1e-9 }.map { circle(it, b.z0) }.toList()
            val rays = (0 until 12).map { a -> val t = a * PI / 6; listOf(doubleArrayOf(cx, cy, b.z0), doubleArrayOf(cx + half * kotlin.math.cos(t), cy + half * kotlin.math.sin(t), b.z0)) }
            // The z-axis, up the middle.
            rings + rays + listOf(listOf(doubleArrayOf(cx, cy, b.z0), doubleArrayOf(cx, cy, b.z1)))
        }
        Coordinates3D.Mode.Spherical -> {
            val cz = (b.z0 + b.z1) / 2
            val r = minOf(half, (b.z1 - b.z0) / 2) * 0.9
            val latitudes = listOf(-60.0, -30.0, 0.0, 30.0, 60.0).map { lat -> val phi = lat * PI / 180; circle(r * kotlin.math.cos(phi), cz + r * kotlin.math.sin(phi)) }
            val meridians = (0 until 6).map { m ->
                val t = m * PI / 6
                (0..64).map { k -> val phi = k * 2 * PI / 64; doubleArrayOf(cx + r * kotlin.math.sin(phi) * kotlin.math.cos(t), cy + r * kotlin.math.sin(phi) * kotlin.math.sin(t), cz + r * kotlin.math.cos(phi)) }
            }
            latitudes + meridians
        }
    }
}

/**
 * A tap with a geometry tool in space: on a construction's point (named if it wasn't), on an
 * object of the kind the tool needs (a line, a plane, a circle or sphere…), or on empty space,
 * which makes a point there on the floor (z = 0, or the box's bottom if 0 is outside it).
 */
/**
 * The point at height [z] under the finger at [tap]: the projection undone by Newton's method,
 * kept inside the box and at a tidy value (a fortieth of the box).
 */
private fun levelPointUnder(tap: Offset, z: Double, b: Bounds, cam: Camera, w: Float, h: Float): Pair<Double, Double> {
    fun at(x: Double, y: Double) = Surface3D.project(x, y, z, b, cam, w, h).let { Offset(it.first, it.second) }
    var x = (b.x0 + b.x1) / 2; var y = (b.y0 + b.y1) / 2
    val e = 1e-4 * maxOf(b.x1 - b.x0, b.y1 - b.y0)
    repeat(12) {
        val s0 = at(x, y); val sx = at(x + e, y); val sy = at(x, y + e)
        val a11 = (sx.x - s0.x) / e; val a21 = (sx.y - s0.y) / e; val a12 = (sy.x - s0.x) / e; val a22 = (sy.y - s0.y) / e
        val det = a11 * a22 - a12 * a21
        if (kotlin.math.abs(det) < 1e-9) return@repeat
        val rx = tap.x - s0.x; val ry = tap.y - s0.y
        x += (a22 * rx - a12 * ry) / det; y += (a11 * ry - a21 * rx) / det
    }
    val snap = com.example.cas.graph.Plot2D.niceStep(b.x1 - b.x0, 40)
    fun tidy(v: Double, lo: Double, hi: Double) = Math.round(v.coerceIn(lo, hi) / snap) * snap
    return tidy(x, b.x0, b.x1) to tidy(y, b.y0, b.y1)
}

private fun constructTap3D(
    vm: Graph3DViewModel, tool: GeometryTool, tap: Offset,
    geometry: List<Triple<PlotFunction, Geometry3D.Obj, Geometry3D.Drawing>>, b: Bounds, cam: Camera, w: Float, h: Float, reach: Float,
) {
    fun at(p: DoubleArray) = Surface3D.project(p[0], p[1], p[2], b, cam, w, h).let { Offset(it.first, it.second) }
    fun pointNear(): String? = geometry.filter { it.second is Geometry3D.Point }
        .mapNotNull { (f, _, d) -> d.points.firstOrNull()?.let { f to (at(it) - tap).getDistance() } }
        .filter { it.second < reach }.minByOrNull { it.second }?.first?.let { vm.nameLine(it) }
    fun fits(kind: Char, o: Geometry3D.Obj) = when (kind) {
        'L' -> o is Geometry3D.Line || o is Geometry3D.Segment || o is Geometry3D.Ray || o is Geometry3D.Vector
        'S' -> o is Geometry3D.Plane
        'C' -> o is Geometry3D.Circle || o is Geometry3D.Sphere
        'V' -> o is Geometry3D.Vector
        else -> o !is Geometry3D.Point && o !is Geometry3D.Number && o !is Geometry3D.Bool
    }
    fun segmentDistance(a: Offset, c: Offset): Float {
        val d = c - a; val len2 = d.x * d.x + d.y * d.y
        val t = if (len2 == 0f) 0f else (((tap.x - a.x) * d.x + (tap.y - a.y) * d.y) / len2).coerceIn(0f, 1f)
        return (tap - Offset(a.x + d.x * t, a.y + d.y * t)).getDistance()
    }
    fun inside(poly: List<Offset>): Boolean {
        var inside = false
        var j = poly.size - 1
        for (i in poly.indices) {
            val a = poly[i]; val c = poly[j]
            if ((a.y > tap.y) != (c.y > tap.y) && tap.x < (c.x - a.x) * (tap.y - a.y) / (c.y - a.y) + a.x) inside = !inside
            j = i
        }
        return inside
    }
    fun objectNear(kind: Char): String? = geometry.filter { fits(kind, it.second) }.mapNotNull { (f, _, d) ->
        val lineD = d.lines.minOfOrNull { line -> line.map { at(it) }.zipWithNext { a, c -> segmentDistance(a, c) }.minOrNull() ?: Float.MAX_VALUE } ?: Float.MAX_VALUE
        val faceD = if (d.faces.any { face -> inside(face.map { at(it) }) }) reach * 0.9f else Float.MAX_VALUE
        f to minOf(lineD, faceD)
    }.filter { it.second < reach }.minByOrNull { it.second }?.first?.let { vm.nameLine(it) }
    fun newPoint(): String {
        // The floor's point under the finger.
        val z = if (0.0 in b.z0..b.z1) 0.0 else b.z0
        val (x, y) = levelPointUnder(tap, z, b, cam, w, h)
        return vm.addFreePoint3(x, y, z)
    }
    when (tool) {
        GeometryTool.Move -> {}
        GeometryTool.Point -> if (pointNear() == null) newPoint()
        else -> when (val need = vm.geometryNeeds ?: 'P') {
            'P' -> vm.pickForTool(pointNear() ?: newPoint())
            'X' -> (pointNear() ?: objectNear('O'))?.let { vm.pickForTool(it) }
            else -> objectNear(need)?.let { vm.pickForTool(it) }
        }
    }
}
