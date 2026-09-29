package com.example.cas.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
 * Complex functions by domain colouring, after samuelj.li's complex function
 * plotter: colour shows arg f(z), brightness |f(z)| (zeros black, poles white).
 * Drag and pinch to move and zoom; tap a point to read z and f(z); letters
 * other than z get sliders (f(z) = z − t); draw a loop to integrate around it.
 */
@Composable
fun ComplexScreen(vm: ComplexViewModel, modifier: Modifier = Modifier) {
    BackHandler(enabled = vm.active != null) { vm.edit(null) }
    // The plot's size, for exporting it in the same shape.
    var plotSize by remember { mutableStateOf(IntSize.Zero) }
    var exporting by remember { mutableStateOf(false) }
    val build = { r: ExportRequest -> complexScene(vm, r.view, EXPORT_SIZE, r.dark, quick = r.preview) }
    val export = rememberGraphExporter("complex-plot", build)
    val shownView = vm.view
    if (exporting && shownView != null) ExportDialog(shownView, build, onExport = { r, share -> exporting = false; export(r, share) }, onDismiss = { exporting = false })
    Column(modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth().onSizeChanged { plotSize = it }) {
            ComplexCanvas(vm, Modifier.fillMaxSize())
            // (Top left holds the contour result, bottom right the toolbar.)
            GraphBottomBar(vm, Modifier.align(Alignment.BottomCenter), onExport = { if (vm.view != null) exporting = true }, tools = { PlotTools(vm) })
            vm.contourResult?.let { ContourCard(it, onClose = vm::clearContour, modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) }
        }
        FunctionList(vm, outputLabel = "f(z)")
        AnimatedVisibility(visible = vm.active != null && !vm.keypadHidden, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Keypad(vm)
        }
    }
}

fun ComplexViewModel.resetView(size: IntSize) {
    if (size.width > 0) view = Viewport.standard(size.height.toDouble() / size.width, halfWidth = 3.0)
}

@Composable
private fun ComplexCanvas(vm: ComplexViewModel, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    var size by remember { mutableStateOf(IntSize.Zero) }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var refining by remember { mutableStateOf(false) }
    var probe by remember { mutableStateOf<CD?>(null) }
    val view = vm.view
    val f = vm.plotted
    val params = vm.parameters.toMap()

    // Equations drawn as curves over the colouring (|z − 1| = 2, x² + y² = 4).
    val curves = remember(view, vm.version, size, params) {
        val v = view
        if (v == null || size.width == 0) emptyList() else vm.functions.filter { it.visible && it.complexCurve != null }.mapNotNull { fn ->
            val g = fn.complexCurve!!
            runCatching { com.example.cas.graph.Curves.implicit({ x, y -> vm.call(fn, g, x, y) }, v, (size.width / 6).coerceIn(40, 200), (size.height / 6).coerceIn(40, 260)) }.getOrNull()
                ?.let { complexLineColor(fn) to it }
        }
    }

    // Render coarse first so panning feels live, then sharper once the view settles.
    LaunchedEffect(view, vm.version, size, vm.options, params, f, f?.colormap, f?.colormapReversed, AppSettings.complexQuality) {
        val v = view ?: return@LaunchedEffect
        val c = f?.complexCompiled
        if (c == null || size.width == 0) { image = null; return@LaunchedEffect }
        val p = vm.parameterValues(f!!)
        try {
            // Standard quality stops at half resolution; high renders every pixel.
            val passes = if (AppSettings.complexQuality == 1) listOf(8 to 0L, 2 to 140L, 1 to 60L) else listOf(8 to 0L, 2 to 140L)
            for ((divisor, wait) in passes) {
                delay(wait)
                // The sharp pass takes a moment: the expressive loading indicator shows meanwhile.
                refining = divisor <= 2
                val w = (size.width / divisor).coerceAtLeast(1)
                val h = (size.height / divisor).coerceAtLeast(1)
                val ctx = coroutineContext
                val px = withContext(Dispatchers.Default) {
                    DomainColoring.render(c, p, v, w, h, vm.options.copy(colormap = f.colormap, reversed = f.colormapReversed)) { !ctx.isActive }
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
            .background(Color.Black)
            .onSizeChanged { size = it; if (vm.view == null) vm.resetView(it) }
            .pointerInput(vm.contourMode) {
                if (vm.contourMode) {
                    // Draw a loop with one finger; on release it closes and ∮ f dz is shown.
                    detectDragGestures(
                        onDragStart = { o -> vm.view?.let { v -> vm.contour = listOf(toPlane(v, o, size)); vm.contourResult = null } },
                        onDrag = { change, _ -> vm.view?.let { v -> vm.contour = vm.contour + toPlane(v, change.position, size) } },
                        onDragEnd = { vm.finishContour() },
                    )
                } else {
                    detectAxisTransformGestures { centroid, pan, zoomX, zoomY ->
                        val v = vm.view ?: return@detectAxisTransformGestures
                        val w = size.width.toDouble(); val h = size.height.toDouble()
                        vm.view = v.panBy(pan.x / w, pan.y / h).zoomAxes(zoomX, zoomY, centroid.x / w, centroid.y / h)
                        probe = null
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { vm.resetView(size); probe = null },
                    onTap = { o -> vm.view?.let { v -> probe = if (probe == null) toPlane(v, o, size) else null } },
                )
            }
            .semantics { contentDescription = "Complex plane. Color shows the argument of f(z), brightness its size. Drag to move, pinch to zoom, tap to read a value. f can use r and θ for |z| and arg z." },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // The canvas's own size (in pixels, as Float), not the view's IntSize state of the same name.
            val size = this.size
            val v = view ?: return@Canvas
            image?.let { drawImage(it, dstSize = IntSize(size.width.toInt(), size.height.toInt()), filterQuality = FilterQuality.Low) }
            // Axes and tick labels, white with a dark halo so they read on any colour.
            val origin = Offset(((0 - v.xMin) / v.width * size.width).toFloat(), ((v.yMax - 0) / v.height * size.height).toFloat())
            val axis = Color.White.copy(alpha = 0.7f)
            if (origin.y in 0f..size.height) drawLine(axis, Offset(0f, origin.y), Offset(size.width, origin.y), 1.5.dp.toPx())
            if (origin.x in 0f..size.width) drawLine(axis, Offset(origin.x, 0f), Offset(origin.x, size.height), 1.5.dp.toPx())
            val style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 11.sp, color = Color.White, shadow = androidx.compose.ui.graphics.Shadow(Color.Black, blurRadius = 4f))
            val stepX = Plot2D.niceStep(v.width, 5)
            Plot2D.ticks(v.xMin, v.xMax, 5).filter { kotlin.math.abs(it) > stepX / 2 }.forEach { x ->
                val t = measurer.measure(Plot2D.label(x, stepX), style)
                val sx = ((x - v.xMin) / v.width * size.width).toFloat()
                drawText(t, topLeft = Offset(sx - t.size.width / 2f, pinInside(origin.y, 2f, size.height - 18.dp.toPx()) + 4.dp.toPx()))
            }
            val stepY = Plot2D.niceStep(v.height, 6)
            Plot2D.ticks(v.yMin, v.yMax, 6).filter { kotlin.math.abs(it) > stepY / 2 }.forEach { y ->
                val t = measurer.measure(Plot2D.label(y, stepY) + "i", style)
                val sy = ((v.yMax - y) / v.height * size.height).toFloat()
                drawText(t, topLeft = Offset(pinInside(origin.x, 2f, size.width - 44.dp.toPx()) + 4.dp.toPx(), sy - t.size.height / 2f))
            }
            // Polar grid: circles of constant |z| and rays every 30° of arg z.
            if (vm.polarGrid) {
                val corners = listOf(v.xMin to v.yMin, v.xMin to v.yMax, v.xMax to v.yMin, v.xMax to v.yMax)
                val far = corners.maxOf { (x, y) -> kotlin.math.hypot(x, y) }
                val step = Plot2D.niceStep(minOf(v.width, v.height) / 2, 4)
                val px = size.width / v.width.toFloat()
                val line = Color.White.copy(alpha = 0.55f)
                var r = step
                while (r <= far) { drawCircle(line, (r * px).toFloat(), origin, style = Stroke(1.2f)); r += step }
                val reach = (far * px).toFloat() + size.width
                for (a in 0 until 12) {
                    val ang = a * Math.PI / 6
                    drawLine(line, origin, Offset(origin.x + (reach * kotlin.math.cos(ang)).toFloat(), origin.y - (reach * kotlin.math.sin(ang)).toFloat()), 1.2f)
                }
            }
            // Curves, outlined so they show on any colour.
            curves.forEach { (lineColor, segs) ->
                val path = Path()
                segs.forEach { sg ->
                    path.moveTo(((sg[0] - v.xMin) / v.width * size.width).toFloat(), ((v.yMax - sg[1]) / v.height * size.height).toFloat())
                    path.lineTo(((sg[2] - v.xMin) / v.width * size.width).toFloat(), ((v.yMax - sg[3]) / v.height * size.height).toFloat())
                }
                drawPath(path, Color.Black.copy(alpha = 0.55f), style = Stroke(4.5.dp.toPx(), cap = StrokeCap.Round))
                drawPath(path, lineColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            }
            // Typed contour integrals: the circle, an arrow showing it runs anticlockwise, and the value.
            vm.functions.filter { it.visible && it.contour != null }.forEach { fn ->
                val c = fn.contour!!
                // Its own plain color, white unless one was picked.
                val lineColor = complexLineColor(fn)
                val cx = ((c.centerRe - v.xMin) / v.width * size.width).toFloat()
                val cy = ((v.yMax - c.centerIm) / v.height * size.height).toFloat()
                val rx = (c.radius / v.width * size.width).toFloat()
                val ry = (c.radius / v.height * size.height).toFloat()
                val oval = androidx.compose.ui.geometry.Rect(cx - rx, cy - ry, cx + rx, cy + ry)
                drawOval(Color.Black.copy(alpha = 0.55f), oval.topLeft, oval.size, style = Stroke(4.5.dp.toPx()))
                drawOval(lineColor, oval.topLeft, oval.size, style = Stroke(2.dp.toPx()))
                // Arrowhead at the right of the circle, pointing up (anticlockwise).
                val tip = Offset(cx + rx, cy - 8.dp.toPx())
                val arrow = Path().apply {
                    moveTo(tip.x, tip.y); lineTo(tip.x - 6.dp.toPx(), tip.y + 10.dp.toPx()); lineTo(tip.x + 6.dp.toPx(), tip.y + 10.dp.toPx()); close()
                }
                drawPath(arrow, lineColor)
                drawPath(arrow, Color.Black.copy(alpha = 0.55f), style = Stroke(1.dp.toPx()))
                // The value in rounded Google Sans Flex, to at most 4 decimals.
                val shown = runCatching { roundedComplex(com.example.cas.cas.Numeric.eval(c.value)) }.getOrDefault("?")
                val label = measurer.measure("Integral ≈ $shown", TextStyle(fontFamily = CasFonts.Ui, fontSize = 14.sp, color = colors.inverseOnSurface))
                val at = Offset((cx + rx * 0.72f).coerceAtMost(size.width - label.size.width - 8f), (cy - ry * 0.72f - label.size.height).coerceAtLeast(4f))
                drawRoundRect(colors.inverseSurface, at - Offset(8f, 4f), androidx.compose.ui.geometry.Size(label.size.width + 16f, label.size.height + 8f), androidx.compose.ui.geometry.CornerRadius(20f))
                drawText(label, topLeft = at)
            }
            // The drawn loop.
            if (vm.contour.size > 1) {
                val path = Path()
                vm.contour.forEachIndexed { k, z ->
                    val o = toScreen(v, z, size.width, size.height)
                    if (k == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                }
                if (vm.contourResult != null) path.close()
                drawPath(path, Color.Black.copy(alpha = 0.5f), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(path, Color.White, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            probe?.let { z ->
                val o = toScreen(v, z, size.width, size.height)
                drawCircle(Color.Black, 7.dp.toPx(), o)
                drawCircle(Color.White, 5.dp.toPx(), o)
            }
        }
        // Value readout for the tapped point.
        if (refining) Busy(Modifier.align(Alignment.TopCenter).padding(top = 12.dp))
        val z = probe
        if (z != null && view != null && f?.complexCompiled != null) {
            val w = runCatching { f.complexCompiled!!(z, vm.parameterValues(f)) }.getOrNull()
            val o = toScreen(view, z, size.width.toFloat(), size.height.toFloat())
            Text(
                "z = " + complexText(z) + "  =  " + polarText(z) + "\nf(z) = " + (w?.let { complexText(it) + "  =  " + polarText(it) } ?: "undefined"),
                color = colors.inverseOnSurface,
                style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 13.sp),
                modifier = Modifier
                    .offset { IntOffset(pinInside((o.x - 80.dp.toPx()).toInt(), 8, size.width - 200.dp.roundToPx()), (o.y - 64.dp.toPx()).toInt().coerceAtLeast(8)) }
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.inverseSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

private fun toPlane(v: Viewport, o: Offset, size: IntSize) =
    CD(v.xMin + o.x / size.width * v.width, v.yMax - o.y / size.height * v.height)

private fun toScreen(v: Viewport, z: CD, w: Float, h: Float) =
    Offset(((z.re - v.xMin) / v.width * w).toFloat(), ((v.yMax - z.im) / v.height * h).toFloat())

private fun toScreen(v: Viewport, z: CD, w: Int, h: Int) = toScreen(v, z, w.toFloat(), h.toFloat())

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
    run {
        ToolToggle(PlotIcons.Bands, "Modulus bands", vm.options.modulusBands) { vm.options = vm.options.copy(modulusBands = !vm.options.modulusBands) }
        ToolToggle(PlotIcons.Phase, "Phase lines", vm.options.phaseLines) { vm.options = vm.options.copy(phaseLines = !vm.options.phaseLines) }
        ToolToggle(PlotIcons.Grid, "Grid lines of Re f and Im f", vm.options.grid) { vm.options = vm.options.copy(grid = !vm.options.grid) }
        ToolToggle(PlotIcons.PolarGrid, "Polar grid: circles of |z| and rays of arg z", vm.polarGrid) { vm.polarGrid = !vm.polarGrid }
    }
}

/**
 * The complex plane over [view] as a square [Scene] [size] units across, framed like pgfplots
 * (see [Pgf]): the domain colouring as a picture (rendered afresh for the export) with the polar
 * grid, curves, ∮ loops and a drawn loop as lines on top, Re z and Im z on the axes.
 */
internal fun complexScene(vm: ComplexViewModel, view: Viewport, size: Double, dark: Boolean, quick: Boolean = false): Scene {
    val style = if (dark) Pgf.Style.DARK else Pgf.Style.LIGHT
    val scene = Scene(size, size, style.background)
    val v = view
    val frame = Pgf.frame(size)
    fun sx(x: Double) = Pgf.sx(v, frame, x)
    fun sy(y: Double) = Pgf.sy(v, frame, y)
    val white = 0xFFFFFFFF.toInt()
    val halo = 0x8C000000.toInt()
    scene.add(Scene.ClipStart(frame.left, frame.top, frame.width, frame.height))
    val f = vm.plotted
    val c = f?.complexCompiled
    if (f != null && c != null) {
        // Four pixels per unit (half a pixel for the dialog's preview).
        val scale = if (quick) 0.5 else 4.0
        val pw = (frame.width * scale).toInt().coerceAtLeast(1); val ph = (frame.height * scale).toInt().coerceAtLeast(1)
        DomainColoring.render(c, vm.parameterValues(f), v, pw, ph, vm.options.copy(colormap = f.colormap, reversed = f.colormapReversed))?.let { px ->
            scene.add(Scene.Image(frame.left, frame.top, frame.width, frame.height, px, pw, ph))
        }
    }
    if (vm.polarGrid) {
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
    // Curves and loops: a dark outline, then their color.
    fun outlined(paths: List<DoubleArray>, color: Int) {
        scene.add(Scene.Stroke(paths, halo, 3.0))
        scene.add(Scene.Stroke(paths, color, 1.3))
    }
    vm.functions.filter { it.visible && it.complexCurve != null }.forEach { fn ->
        val g = fn.complexCurve!!
        val segs = runCatching { com.example.cas.graph.Curves.implicit({ x, y -> vm.call(fn, g, x, y) }, v, 200, 200) }.getOrNull() ?: return@forEach
        outlined(segs.map { sg -> doubleArrayOf(sx(sg[0]), sy(sg[1]), sx(sg[2]), sy(sg[3])) }, complexLineColor(fn).toArgb())
    }
    vm.functions.filter { it.visible && it.contour != null }.forEach { fn ->
        val cc = fn.contour!!
        val cx = sx(cc.centerRe); val cy = sy(cc.centerIm)
        val rx = cc.radius / v.width * frame.width; val ry = cc.radius / v.height * frame.height
        val color = complexLineColor(fn).toArgb()
        outlined(listOf(DoubleArray(2 * 97) { k -> val t = (k / 2) * 2 * Math.PI / 96; if (k % 2 == 0) cx + rx * kotlin.math.cos(t) else cy - ry * kotlin.math.sin(t) }), color)
        // Arrowhead at the right, pointing up (anticlockwise).
        val tx = cx + rx; val ty = cy - 5
        scene.add(Scene.Fill(listOf(doubleArrayOf(tx, ty, tx - 4, ty + 7, tx + 4, ty + 7)), color))
        val shown = runCatching { roundedComplex(com.example.cas.cas.Numeric.eval(cc.value)) }.getOrDefault("?")
        scene.add(Scene.Label(minOf(cx + rx * 0.72, frame.right - 4), maxOf(cy - ry * 0.72 - 8, frame.top + 10), "Integral ≈ $shown", 13.0, white, Scene.Anchor.Start, Scene.Font.Roman))
    }
    if (vm.contour.size > 1) {
        val pts = DoubleArray(vm.contour.size * 2 + if (vm.contourResult != null) 2 else 0)
        vm.contour.forEachIndexed { k, z -> pts[2 * k] = sx(z.re); pts[2 * k + 1] = sy(z.im) }
        if (vm.contourResult != null) { pts[pts.size - 2] = pts[0]; pts[pts.size - 1] = pts[1] }
        outlined(listOf(pts), white)
    }
    scene.add(Scene.ClipEnd)
    Pgf.axes(scene, v, frame, style, xName = "Re z", yName = "Im z", ySuffix = "i", nameFont = Scene.Font.Roman)
    return scene
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
private fun ContourCard(integral: CD, onClose: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val residues = DomainColoring.residueSum(integral)
    Row(
        modifier.clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerHigh).padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // All in rounded Google Sans Flex, numbers to at most 4 decimals.
        Column {
            Text("Integral around the loop ≈ " + roundedComplex(integral), color = colors.onSurface, style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 16.sp))
            Text("Residues inside add up to ≈ " + roundedComplex(residues), color = colors.onSurfaceVariant, style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 14.sp))
            Text("for a loop drawn anticlockwise", color = colors.onSurfaceVariant, style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 12.sp))
        }
        IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Clear the loop", tint = colors.onSurfaceVariant) }
    }
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
