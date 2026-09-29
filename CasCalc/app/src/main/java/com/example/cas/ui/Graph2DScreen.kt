package com.example.cas.ui

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
)

private data class Special(val x: Double, val y: Double, val label: String, val colorIndex: Int)

@Composable
fun Graph2DScreen(vm: Graph2DViewModel, onUseValue: (Double) -> Unit, modifier: Modifier = Modifier) {
    BackHandler(enabled = vm.active != null) { vm.edit(null) }
    Column(modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Graph2DCanvas(vm, onUseValue, Modifier.fillMaxSize())
            if (vm.active != null && vm.keypadHidden) {
                ShowKeypadButton(onClick = { vm.keypadHidden = false }, modifier = Modifier.align(Alignment.BottomStart).padding(12.dp))
            }
        }
        FunctionList(vm, outputLabel = "y")
        AnimatedVisibility(visible = vm.active != null && !vm.keypadHidden, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Keypad(vm)
        }
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
    // Colours picked by long-pressing a function's dot replace the theme's.
    val picked = vm.functions.mapNotNull { f -> f.customColor?.let { f.colorIndex to Color(it) } }.toMap()
    val palette = themeColors.indices.map { picked[it] ?: themeColors[it] }

    val view = vm.view
    val version = vm.version
    // Sampling and point-finding only redo when the view or a function changes.
    val highlighted = vm.highlighted
    val plotted = remember(view, version, size, vm.parameters.toMap(), highlighted, vm.polarGrid, AppSettings.specialPoints) {
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
            if (vm.polarGrid) drawPolarGrid(v, colors.outlineVariant, colors.onSurfaceVariant, measurer, vm.angle == com.example.cas.engine.AngleUnit.Degrees)
            // The grid can be turned off in settings (the axes stay).
            else drawGrid(v, if (AppSettings.showGrid) colors.outlineVariant else Color.Transparent, colors.onSurfaceVariant, measurer)
            vm.area?.let { ar ->
                val fn = (ar.f.plot as? Plot2DKind.Explicit)?.f
                if (fn != null && !ar.signed.isNaN()) {
                    val lo = minOf(ar.a, ar.b); val hi = maxOf(ar.a, ar.b)
                    val path = Path()
                    val steps = 200
                    path.moveTo(toScreen(v, lo, 0.0).x, toScreen(v, lo, 0.0).y)
                    for (k in 0..steps) {
                        val x = lo + (hi - lo) * k / steps
                        val y = vm.call(ar.f, fn, x).let { if (it.isFinite()) it.coerceIn(v.yMin - v.height, v.yMax + v.height) else 0.0 }
                        val o = toScreen(v, x, y); path.lineTo(o.x, o.y)
                    }
                    path.lineTo(toScreen(v, hi, 0.0).x, toScreen(v, hi, 0.0).y)
                    path.close()
                    drawPath(path, palette[ar.f.colorIndex].copy(alpha = 0.28f))
                }
            }
            plotted.forEach { p -> drawCurve(v, p, palette[p.f.colorIndex]) }
            vm.areaStart?.let { (f, a) ->
                val fn = (f.plot as? Plot2DKind.Explicit)?.f
                if (fn != null) {
                    val o = toScreen(v, a, vm.call(f, fn, a))
                    drawLine(palette[f.colorIndex], Offset(o.x, 0f), Offset(o.x, size.height), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())))
                    drawCircle(palette[f.colorIndex], radius = 6.dp.toPx(), center = o)
                }
            }
            plotted.flatMap { it.points }.forEach { s ->
                val o = toScreen(v, s.x, s.y)
                drawCircle(colors.surface, radius = 5.dp.toPx(), center = o)
                // Plotted points are filled dots; found points (zeros, extrema…) are rings.
                if (s.label == "point") drawCircle(palette[s.colorIndex], radius = 6.dp.toPx(), center = o)
                else drawCircle(palette[s.colorIndex], radius = 5.dp.toPx(), center = o, style = Stroke(2.dp.toPx()))
            }
            trace?.let { t ->
                val o = toScreen(v, t.x, t.y)
                drawCircle(palette[t.colorIndex], radius = 7.dp.toPx(), center = o)
                drawCircle(colors.inverseSurface, radius = 3.dp.toPx(), center = o)
            }
        }
        if (vm.areaStart != null) {
            Text(
                "Tap where the area should end",
                color = colors.inverseOnSurface,
                style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 14.sp),
                modifier = Modifier.align(Alignment.TopCenter).padding(12.dp).clip(CircleShape).background(colors.inverseSurface)
                    .clickable(onClickLabel = "Cancel") { vm.areaStart = null }.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        vm.area?.let { ar ->
            Row(
                Modifier.align(Alignment.TopStart).padding(12.dp).clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerHigh)
                    .padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    if (ar.signed.isNaN()) {
                        Text("Couldn't integrate between these points", color = colors.error, style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 14.sp))
                    } else {
                        Text("∫ from ${shortNumber(ar.a)} to ${shortNumber(ar.b)}  ≈ ${shortNumber(ar.signed)}", color = colors.onSurface, style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 17.sp))
                        Text("area between curve and axis ≈ ${shortNumber(ar.total)}", color = colors.onSurfaceVariant, style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 13.sp))
                    }
                }
                IconButton(onClick = { vm.area = null }) { Icon(Icons.Default.Close, contentDescription = "Clear the area", tint = colors.onSurfaceVariant) }
            }
        }
        val tap = rememberKeyTap()
        // Export as PDF, PNG, JPG or SVG, in the shape of the graph on screen.
        var exporting by remember { mutableStateOf(false) }
        val context = androidx.compose.ui.platform.LocalContext.current
        val density = androidx.compose.ui.platform.LocalDensity.current.density
        val export = rememberGraphExporter("graph") { r ->
            graph2DScene(vm, r.view, size.width / density.toDouble(), size.height / density.toDouble(), com.example.cas.ui.theme.appColorScheme(context, r.dark))
        }
        if (exporting && view != null) ExportDialog(view, onExport = { r, share -> exporting = false; export(r, share) }, onDismiss = { exporting = false })
        ExpressiveToolbar(Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
            ToolToggle(PlotIcons.PolarGrid, "Polar grid", vm.polarGrid) { vm.polarGrid = !vm.polarGrid }
            // Equal scales on both axes, so circles look round.
            IconButton(onClick = { tap(); vm.zoomSquare(size.width, size.height) }) {
                Icon(Icons.Default.CropSquare, contentDescription = "Square zoom: equal scales", tint = colors.onSurface)
            }
            IconButton(onClick = { tap(); exporting = true }, enabled = size.width > 0) {
                Icon(Icons.Default.IosShare, contentDescription = "Export the graph", tint = colors.onSurface)
            }
        }
        // The point's coordinates, with buttons to use x or y (or r and θ) in the calculator.
        val t = trace
        if (t != null && view != null) {
            val px = ((t.x - view.xMin) / view.width * size.width).toFloat()
            val py = ((view.yMax - t.y) / view.height * size.height).toFloat()
            val areaFunction = vm.functions.firstOrNull { it.colorIndex == t.colorIndex && it.plot is Plot2DKind.Explicit }
            Layout(content = {
                TraceBubble(
                    t, polar = vm.polarGrid, degrees = vm.angle == com.example.cas.engine.AngleUnit.Degrees,
                    onUse = { value -> trace = null; onUseValue(value) },
                    onArea = areaFunction?.let { f -> { vm.area = null; vm.areaStart = f to t.x; trace = null } },
                )
            }, modifier = Modifier.fillMaxSize()) { ms, c ->
                val p = ms[0].measure(Constraints())
                layout(c.maxWidth, c.maxHeight) {
                    val gap = 16.dp.roundToPx()
                    val x = pinInside((px - p.width / 2f).toInt(), 8, c.maxWidth - p.width - 8)
                    val y = if (py - p.height - gap > 0) (py - p.height - gap).toInt() else (py + gap).toInt()
                    p.place(x, y)
                }
            }
        }
    }
}

@Composable
private fun TraceBubble(s: Special, polar: Boolean, degrees: Boolean, onUse: (Double) -> Unit, onArea: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    val tap = rememberKeyTap()
    // With the polar grid on, points read as (r, θ).
    val r = kotlin.math.hypot(s.x, s.y)
    var theta = kotlin.math.atan2(s.y, s.x)
    if (theta < 0) theta += 2 * PI
    val shownTheta = if (degrees) theta * 180 / PI else theta
    val coords = if (polar) "r = ${shortNumber(r)}, θ = ${shortNumber(shownTheta)}${if (degrees) "°" else ""}" else "(" + shortNumber(s.x) + ", " + shortNumber(s.y) + ")"
    val uses = if (polar) listOf("r" to r, "θ" to shownTheta) else listOf("x" to s.x, "y" to s.y)
    Row(
        Modifier.clip(CircleShape).background(colors.inverseSurface).padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            (if (s.label.isNotEmpty()) s.label + "  " else "") + coords,
            color = colors.inverseOnSurface,
            style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 13.sp),
        )
        // Area: this point is where it starts; the next tap on the graph is where it ends.
        if (onArea != null) {
            Box(
                Modifier
                    .padding(start = 6.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.inversePrimary)
                    .clickable(onClickLabel = "Area from here") { tap(); onArea() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(TabIcons.Area, contentDescription = null, tint = colors.inverseSurface, modifier = Modifier.size(20.dp))
            }
        }
        uses.forEach { (name, value) ->
            Box(
                Modifier
                    .padding(start = 6.dp)
                    .clip(CircleShape)
                    .background(colors.inversePrimary)
                    .clickable(onClickLabel = "Use $name in the calculator") { tap(); onUse(value) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text("Use $name", color = colors.inverseSurface, style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 13.sp))
            }
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
    val fns = vm.functions.filter { it.visible && it.plot != null }
    val samples = (size.width / 2).coerceIn(200, 900)
    val nx = (size.width / 6).coerceIn(40, 240)
    val ny = (size.height / 6).coerceIn(40, 320)
    val out = ArrayList<Plotted>()
    for (f in fns) {
        when (val k = f.plot!!) {
            is Plot2DKind.Vector -> {
                val x = vm.call(f, k.x); val y = vm.call(f, k.y)
                val x0 = k.fromX?.let { vm.call(f, it) } ?: 0.0
                val y0 = k.fromY?.let { vm.call(f, it) } ?: 0.0
                if (listOf(x, y, x0, y0).all { it.isFinite() }) out += Plotted(f, listOf(listOf(x0 to y0, x to y)), emptyList(), arrow = true)
            }
            is Plot2DKind.PointList -> {
                val pts = k.xs.indices.filter { k.xs[it].isFinite() && k.ys[it].isFinite() }.map { Special(k.xs[it], k.ys[it], "point", f.colorIndex) }
                out += Plotted(f, emptyList(), pts)
            }
            is Plot2DKind.Point -> {
                val px = vm.call(f, k.x); val py = vm.call(f, k.y)
                if (px.isFinite() && py.isFinite() && vm.allowed(f, px, py)) out += Plotted(f, emptyList(), listOf(Special(px, py, "point", f.colorIndex)))
            }
            is Plot2DKind.Explicit -> {
                // Conditions after commas (0 < x < 2) leave gaps where they fail.
                val fx = { x: Double -> vm.call(f, k.f, x).let { y -> if (vm.allowed(f, x, y)) y else Double.NaN } }
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
                val r = { t: Double -> vm.call(f, k.r, t).let { rr -> if (vm.allowed(f, rr * kotlin.math.cos(t), rr * kotlin.math.sin(t), theta = t, r = rr)) rr else Double.NaN } }
                out += Plotted(f, Curves.polar(r, view, turns = periodTurns(listOf(r)).toDouble()), emptyList())
            }
            is Plot2DKind.Parametric -> {
                val x = { t: Double -> vm.call(f, k.x, t).let { xx -> if (vm.allowed(f, xx, vm.call(f, k.y, t), t = t)) xx else Double.NaN } }
                val y = { t: Double -> vm.call(f, k.y, t) }
                // Closed curves go round once; open ones like (t, t²) run over −10 ≤ t ≤ 10.
                val turns = periodTurns(listOf(x, y))
                val (t0, t1) = if (turns <= 6) 0.0 to 2 * PI * turns else -10.0 to 10.0
                out += Plotted(f, Curves.parametric(x, y, t0, t1, view), emptyList())
            }
            is Plot2DKind.Implicit -> {
                val g = { x: Double, y: Double -> if (vm.allowed(f, x, y)) vm.call(f, k.f, x, y) else Double.NaN }
                out += Plotted(f, emptyList(), emptyList(), segments = Curves.implicit(g, view, nx, ny))
            }
            is Plot2DKind.Region -> {
                val values = DoubleArray(k.parts.size)
                val test = { x: Double, y: Double ->
                    for (p in k.parts.indices) values[p] = vm.call(f, k.parts[p], x, y)
                    Curves.holds(values, k.ops) && vm.allowed(f, x, y)
                }
                // A 4 px grid for the shading, so its edge isn't visibly stepped.
                val mx = (size.width / 4).coerceIn(40, 360); val my = (size.height / 4).coerceIn(40, 480)
                val mask = Curves.region(test, view, mx, my)
                // The edge: where each compared pair is equal.
                val edges = k.ops.indices.flatMap { p ->
                    Curves.implicit({ x, y -> vm.call(f, k.parts[p], x, y) - vm.call(f, k.parts[p + 1], x, y) }, view, nx, ny)
                }
                out += Plotted(f, emptyList(), emptyList(), segments = edges, mask = mask, maskSize = IntSize(mx, my), dashed = k.ops.all { it == "<" || it == ">" })
            }
        }
    }
    // Where two functions of x cross.
    val explicit = fns.filter { it.plot is Plot2DKind.Explicit }
    val crossings = ArrayList<Special>()
    for (a in explicit.indices) for (b in a + 1 until explicit.size) {
        if (!AppSettings.specialPoints || (explicit[a] !== highlighted && explicit[b] !== highlighted)) continue
        val fa = (explicit[a].plot as Plot2DKind.Explicit).f
        val fb = (explicit[b].plot as Plot2DKind.Explicit).f
        val d = { x: Double -> vm.call(explicit[a], fa, x) - vm.call(explicit[b], fb, x) }
        Plot2D.zeros(d, view.xMin, view.xMax, 400).forEach { x ->
            crossings += Special(x, vm.call(explicit[a], fa, x), "intersection", explicit[a].colorIndex)
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

private fun DrawScope.drawGrid(v: Viewport, gridColor: Color, axisColor: Color, measurer: TextMeasurer) {
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
    val style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 11.sp, color = axisColor)
    val labelY = pinInside(origin.y, 2.dp.toPx(), size.height - 16.dp.toPx())
    xs.filter { abs(it) > stepX / 2 }.forEach { x ->
        val t = measurer.measure(Plot2D.label(x, stepX), style)
        val sx = toScreen(v, x, 0.0).x
        drawText(t, topLeft = Offset(sx - t.size.width / 2f, labelY + 3.dp.toPx()))
    }
    val labelX = pinInside(origin.x, 2.dp.toPx(), size.width - 40.dp.toPx())
    ys.filter { abs(it) > stepY / 2 }.forEach { y ->
        val t = measurer.measure(Plot2D.label(y, stepY), style)
        val sy = toScreen(v, 0.0, y).y
        drawText(t, topLeft = Offset(labelX + 4.dp.toPx(), sy - t.size.height / 2f))
    }
}

/** Circles of constant r and rays every 30°, labelled in radians (π/6…) or degrees. */
private fun DrawScope.drawPolarGrid(v: Viewport, gridColor: Color, axisColor: Color, measurer: TextMeasurer, degrees: Boolean) {
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
    val style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 11.sp, color = axisColor)
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
    // The function's own thickness and style (long-press its dot): solid, dashed or dotted.
    val w = p.f.thickness.dp.toPx()
    val style = when (p.f.lineStyle) {
        1 -> PathEffect.dashPathEffect(floatArrayOf(4 * w, 3 * w))
        2 -> PathEffect.dashPathEffect(floatArrayOf(0.01f, 2.5f * w))
        else -> null
    }
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
        drawPath(area, color.copy(alpha = 0.22f))
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
 * The 2D graph over [view] as a [Scene] [w] × [h] units (dp) in [scheme]'s colors, drawn as on
 * screen: grid (or polar grid), shaded areas, curves, points and vectors. Found points (zeros,
 * extrema) and the tapped point are left out.
 */
internal fun graph2DScene(vm: Graph2DViewModel, view: Viewport, w: Double, h: Double, scheme: androidx.compose.material3.ColorScheme): Scene {
    val scene = Scene(w, h, scheme.surface.toArgb())
    val v = view
    fun sx(x: Double) = (x - v.xMin) / v.width * w
    fun sy(y: Double) = (v.yMax - y) / v.height * h
    fun argb(c: Color, alpha: Float = 1f) = c.copy(alpha = c.alpha * alpha).toArgb()
    val themeColors = plotColors(scheme)
    val picked = vm.functions.mapNotNull { f -> f.customColor?.let { f.colorIndex to Color(it) } }.toMap()
    val palette = themeColors.indices.map { picked[it] ?: themeColors[it] }
    val axisColor = scheme.onSurfaceVariant
    val gridColor = if (AppSettings.showGrid) scheme.outlineVariant else Color.Transparent
    val label = 11.0
    if (vm.polarGrid) {
        val ox = sx(0.0); val oy = sy(0.0)
        val corners = listOf(v.xMin to v.yMin, v.xMin to v.yMax, v.xMax to v.yMin, v.xMax to v.yMax)
        val far = corners.maxOf { (x, y) -> kotlin.math.hypot(x, y) }
        val step = Plot2D.niceStep(minOf(v.width, v.height) / 2, 4)
        val perUnit = w / v.width
        var r = step / 2; var k = 1
        while (r <= far + step) {
            scene.add(Scene.Circle(ox, oy, r * perUnit, fill = null, stroke = argb(scheme.outlineVariant, if (k % 2 == 0) 0.9f else 0.35f), strokeWidth = if (k % 2 == 0) 0.6 else 0.5))
            r += step / 2; k++
        }
        val reach = far * perUnit + w
        for (a in 0 until 12) {
            val ang = a * PI / 6
            scene.add(Scene.Stroke(listOf(doubleArrayOf(ox, oy, ox + reach * kotlin.math.cos(ang), oy - reach * kotlin.math.sin(ang))), argb(if (a % 3 == 0) axisColor else scheme.outlineVariant, if (a % 3 == 0) 1f else 0.9f), if (a % 3 == 0) 2.0 else 0.6))
        }
        var lr = step
        while (lr <= far) {
            val x = ox + lr * perUnit
            if (x in 0.0..w && oy in 0.0..h) scene.add(Scene.Label(x, oy + 10, Plot2D.label(lr, step), label, argb(axisColor), Scene.Anchor.Middle))
            lr += step
        }
        val names = listOf("0", "π/6", "π/3", "π/2", "2π/3", "5π/6", "π", "7π/6", "4π/3", "3π/2", "5π/3", "11π/6")
        val labelR = minOf(ox, w - ox, oy, h - oy) - 18
        if (labelR > 40) for (a in 0 until 12) {
            val ang = a * PI / 6
            val text = if (vm.angle == com.example.cas.engine.AngleUnit.Degrees) "${a * 30}°" else names[a]
            scene.add(Scene.Label(ox + labelR * kotlin.math.cos(ang), oy - labelR * kotlin.math.sin(ang), text, label, argb(axisColor), Scene.Anchor.Middle))
        }
    } else {
        val targetX = (w / 90).toInt().coerceAtLeast(3)
        val targetY = (h / 90).toInt().coerceAtLeast(3)
        val stepX = Plot2D.niceStep(v.width, targetX)
        val stepY = Plot2D.niceStep(v.height, targetY)
        // Minor lines at a fifth of a step, then the major ones.
        val minor = Plot2D.ticks(v.xMin, v.xMax, targetX * 5).map { doubleArrayOf(sx(it), 0.0, sx(it), h) } +
            Plot2D.ticks(v.yMin, v.yMax, targetY * 5).map { doubleArrayOf(0.0, sy(it), w, sy(it)) }
        scene.add(Scene.Stroke(minor, argb(gridColor, 0.35f), 0.5))
        val xs = Plot2D.ticks(v.xMin, v.xMax, targetX)
        val ys = Plot2D.ticks(v.yMin, v.yMax, targetY)
        scene.add(Scene.Stroke(xs.map { doubleArrayOf(sx(it), 0.0, sx(it), h) } + ys.map { doubleArrayOf(0.0, sy(it), w, sy(it)) }, argb(gridColor, 0.9f), 0.6))
        val ox = sx(0.0); val oy = sy(0.0)
        val axes = ArrayList<DoubleArray>()
        if (oy in 0.0..h) axes += doubleArrayOf(0.0, oy, w, oy)
        if (ox in 0.0..w) axes += doubleArrayOf(ox, 0.0, ox, h)
        scene.add(Scene.Stroke(axes, argb(axisColor), 2.0))
        // Labels stay on the page when an axis is off it.
        val labelY = pinInside(oy.toFloat(), 2f, (h - 16).toFloat()).toDouble() + 3 + 7
        xs.filter { abs(it) > stepX / 2 }.forEach { x -> scene.add(Scene.Label(sx(x), labelY, Plot2D.label(x, stepX), label, argb(axisColor), Scene.Anchor.Middle)) }
        val labelX = pinInside(ox.toFloat(), 2f, (w - 40).toFloat()).toDouble() + 4
        ys.filter { abs(it) > stepY / 2 }.forEach { y -> scene.add(Scene.Label(labelX, sy(y), Plot2D.label(y, stepY), label, argb(axisColor))) }
    }
    // The area picked with Area, shaded.
    vm.area?.let { ar ->
        val fn = (ar.f.plot as? Plot2DKind.Explicit)?.f
        if (fn != null && !ar.signed.isNaN()) {
            val lo = minOf(ar.a, ar.b); val hi = maxOf(ar.a, ar.b)
            val pts = ArrayList<Double>()
            pts += sx(lo); pts += sy(0.0)
            for (k in 0..200) {
                val x = lo + (hi - lo) * k / 200
                val y = vm.call(ar.f, fn, x).let { if (it.isFinite()) it.coerceIn(v.yMin - v.height, v.yMax + v.height) else 0.0 }
                pts += sx(x); pts += sy(y)
            }
            pts += sx(hi); pts += sy(0.0)
            scene.add(Scene.Fill(listOf(pts.toDoubleArray()), argb(palette[ar.f.colorIndex], 0.28f)))
        }
    }
    // Sampled finely: three samples per unit of width.
    val plotted = runCatching { plot(vm, v, IntSize((w * 3).toInt().coerceAtLeast(1), (h * 3).toInt().coerceAtLeast(1)), null) }.getOrElse { emptyList() }
    for (p in plotted) {
        val color = palette[p.f.colorIndex]
        val lw = p.f.thickness.toDouble()
        val dash = when (p.f.lineStyle) { 1 -> doubleArrayOf(4 * lw, 3 * lw); 2 -> doubleArrayOf(0.01, 2.5 * lw); else -> null }
        p.mask?.let { mask ->
            val mx = p.maskSize.width; val my = p.maskSize.height
            val cw = w / mx; val ch = h / my
            val rects = ArrayList<DoubleArray>()
            for (j in 0 until my) {
                var i = 0
                while (i < mx) {
                    if (!mask[j * mx + i]) { i++; continue }
                    val start = i
                    while (i < mx && mask[j * mx + i]) i++
                    val x0 = start * cw; val x1 = i * cw + 0.25; val y0 = j * ch; val y1 = (j + 1) * ch + 0.25
                    rects += doubleArrayOf(x0, y0, x1, y0, x1, y1, x0, y1)
                }
            }
            scene.add(Scene.Fill(rects, argb(color, 0.22f)))
        }
        if (p.segments.isNotEmpty()) {
            val segW = if (p.mask != null) 2.0 else lw
            scene.add(Scene.Stroke(p.segments.map { sg -> doubleArrayOf(sx(sg[0]), sy(sg[1]), sx(sg[2]), sy(sg[3])) }, argb(color), segW, if (p.dashed) doubleArrayOf(8.0, 6.0) else dash))
        }
        if (p.lines.isNotEmpty()) {
            scene.add(Scene.Stroke(p.lines.map { line -> DoubleArray(line.size * 2) { k -> if (k % 2 == 0) sx(line[k / 2].first) else sy(line[k / 2].second) } }, argb(color), lw, dash))
        }
        // A vector's arrowhead.
        if (p.arrow) p.lines.forEach { line ->
            if (line.size < 2) return@forEach
            val (fx, fy) = line[line.size - 2].let { sx(it.first) to sy(it.second) }
            val (tx, ty) = line.last().let { sx(it.first) to sy(it.second) }
            val len = kotlin.math.hypot(tx - fx, ty - fy)
            if (len >= 0.5) {
                val ux = (tx - fx) / len; val uy = (ty - fy) / len
                val head = maxOf(lw * 4, 10.0)
                val bx = tx - ux * head; val by = ty - uy * head
                scene.add(Scene.Fill(listOf(doubleArrayOf(tx, ty, bx - uy * head * 0.45, by + ux * head * 0.45, bx + uy * head * 0.45, by - ux * head * 0.45)), argb(color)))
            }
        }
        p.points.forEach { pt ->
            val cx = sx(pt.x); val cy = sy(pt.y)
            scene.add(Scene.Circle(cx, cy, 5.0, fill = argb(scheme.surface)))
            // Plotted points are filled dots; others are rings.
            if (pt.label == "point") scene.add(Scene.Circle(cx, cy, 6.0, fill = argb(palette[pt.colorIndex])))
            else scene.add(Scene.Circle(cx, cy, 5.0, fill = null, stroke = argb(palette[pt.colorIndex]), strokeWidth = 2.0))
        }
    }
    return scene
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
    return best
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
