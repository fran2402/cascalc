package com.example.cas.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.cas.ui.theme.CasFonts
import kotlin.math.abs
import androidx.compose.ui.graphics.toArgb
import com.example.cas.graph.Viewport
import com.example.cas.graph.Scene
import kotlin.math.PI

@Composable
fun Graph3DScreen(vm: Graph3DViewModel, modifier: Modifier = Modifier) {
    BackHandler(enabled = vm.active != null) { vm.edit(null) }
    var plotSize by remember { mutableStateOf(IntSize.Zero) }
    var exporting by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val build = { r: ExportRequest ->
        surface3DScene(vm, r, plotSize.width / density.toDouble(), plotSize.height / density.toDouble(), com.example.cas.ui.theme.appColorScheme(context, r.dark))
    }
    val export = rememberGraphExporter("graph-3d", build)
    if (exporting) {
        val b = surfaceBounds(vm)
        ExportDialog(Viewport(vm.xMin, vm.xMax, vm.yMin, vm.yMax), build, onExport = { r, share -> exporting = false; export(r, share) }, onDismiss = { exporting = false }, z = b.z0 to b.z1)
    }
    Column(modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth().onSizeChanged { plotSize = it }) {
            SurfaceCanvas(vm, Modifier.fillMaxSize())
            RangeControl(vm, Modifier.align(Alignment.TopStart).padding(12.dp))
            GraphBottomBar(vm, Modifier.align(Alignment.BottomCenter), onExport = { if (plotSize.width > 0) exporting = true })
        }
        FunctionList(vm, outputLabel = "z")
        AnimatedVisibility(visible = vm.active != null && !vm.keypadHidden, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Keypad(vm)
        }
    }
}

/** "x ∈ [a, b],  y ∈ [c, d]" with the letters in italic Computer Modern, like the maths. */
private fun limitsText(vm: Graph3DViewModel): AnnotatedString = buildAnnotatedString {
    fun letter(v: String) = withStyle(SpanStyle(fontFamily = CasFonts.CmItalic)) { append(v) }
    letter("x"); append(" ∈ [${shortNumber(vm.xMin)}, ${shortNumber(vm.xMax)}],  ")
    letter("y"); append(" ∈ [${shortNumber(vm.yMin)}, ${shortNumber(vm.yMax)}]")
}

/** − and + zoom the ranges; tapping the ranges opens the limits. */
@Composable
private fun RangeControl(vm: Graph3DViewModel, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val tap = rememberKeyTap()
    var editing by remember { mutableStateOf(false) }
    if (editing) LimitsDialog(vm, onDismiss = { editing = false })
    Row(modifier.clip(CircleShape).background(colors.surfaceContainerHigh), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { tap(); vm.scaleRanges(0.5) }) {
            Icon(Icons.Default.Remove, contentDescription = "Zoom in", tint = colors.onSurface)
        }
        Text(
            limitsText(vm),
            color = colors.onSurface,
            style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 15.sp),
            modifier = Modifier.clickable(onClickLabel = "Set the limits") { editing = true }.padding(vertical = 8.dp),
        )
        IconButton(onClick = { tap(); vm.scaleRanges(2.0) }) {
            Icon(Icons.Default.Add, contentDescription = "Zoom out", tint = colors.onSurface)
        }
    }
}

/** Custom limits for x, y and (optionally) z. */
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
    val valid = ok(xs) && ok(ys) && (autoZ || ok(zs))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Limits") },
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
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                vm.xMin = xs.first!!; vm.xMax = xs.second!!
                vm.yMin = ys.first!!; vm.yMax = ys.second!!
                vm.zRange = if (autoZ) null else zs.first!! to zs.second!!
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
private fun SurfaceCanvas(vm: Graph3DViewModel, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    var size by remember { mutableStateOf(IntSize.Zero) }
    var picked by remember { mutableStateOf<DoubleArray?>(null) }
    val version = vm.version
    // Low and high colours of each surface's height gradient, following picked colours.
    val gradients = (0 until GraphViewModel.PLOT_COLOR_COUNT).map { k ->
        val c = vm.functions.firstOrNull { it.colorIndex == k }?.customColor?.let { Color(it) } ?: plotColor(k)
        lerp(c, Color.Black, 0.35f) to lerp(c, Color.White, 0.45f)
    }
    val params = vm.parameters.toMap()
    val limits = listOf(vm.xMin, vm.xMax, vm.yMin, vm.yMax)

    // The box: x and y as set; z as set, or fitted to the explicit surfaces.
    val bounds = remember(version, limits, vm.zRange, params) { surfaceBounds(vm) }
    val polygons = remember(version, bounds, params, AppSettings.surfaceDetail) { surfacePolygons(vm, bounds) }
    val camera = vm.camera
    val faces: List<Face> = remember(polygons, camera, size) {
        if (size.width == 0) emptyList() else Surface3D.faces(polygons, bounds, camera, size.width.toFloat(), size.height.toFloat())
    }

    Box(
        modifier
            .clipToBounds()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                // Drag to turn the surface, pinch to zoom.
                detectTransformGestures { _, pan, zoom, _ ->
                    vm.camera = vm.camera.rotateBy(-pan.x * 0.008, pan.y * 0.008).zoomBy(zoom.toDouble())
                    picked = null
                }
            }
            .pointerInput(faces) {
                detectTapGestures(
                    onDoubleTap = { vm.camera = Camera(); picked = null },
                    // Tap the surface to read its (x, y, z) there.
                    onTap = { o ->
                        val face = Surface3D.pick(faces, o.x, o.y)
                        picked = face?.let { hit ->
                            val c = hit.center.copyOf()
                            // On a z = f(x, y) surface, take z exactly at the tapped (x, y).
                            vm.functions.firstOrNull { it.colorIndex == hit.surface && it.implicit3D == null && it.compiled != null }
                                ?.let { f -> vm.evaluate(f, c[0], c[1]).takeIf { v -> v.isFinite() }?.let { c[2] = it } }
                            c
                        }
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
                val lit = Color(base.red * face.shade, base.green * face.shade, base.blue * face.shade, 1f)
                drawPath(path, lit)
                if (face.xs.size == 4) drawPath(path, wire, style = hairline)
            }
            val labelStyle = TextStyle(fontFamily = CasFonts.CmItalic, fontSize = 18.sp, color = colors.onSurfaceVariant)
            Surface3D.axisLabels(camera, w, h).forEach { (name, p) ->
                val t = measurer.measure(name, labelStyle)
                drawText(t, topLeft = Offset(p.first - t.size.width / 2f, p.second - t.size.height / 2f))
            }
            val style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 11.sp, color = colors.onSurfaceVariant)
            val zText = measurer.measure("z from ${shortNumber(bounds.z0)} to ${shortNumber(bounds.z1)}", style)
            drawText(zText, topLeft = Offset(w - zText.size.width - 12.dp.toPx(), 12.dp.toPx()))
            // Points and space curves, drawn over the surfaces.
            vm.functions.filter { it.visible && it.space != null }.forEach { fn ->
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
            picked?.let { p ->
                val (sx, sy) = Surface3D.project(p[0], p[1], p[2], bounds, camera, w, h)
                drawCircle(colors.inverseSurface, 7.dp.toPx(), Offset(sx, sy))
                drawCircle(colors.inverseOnSurface, 3.dp.toPx(), Offset(sx, sy))
            }
        }
        // The picked point's coordinates.
        picked?.let { p ->
            val (sx, sy) = Surface3D.project(p[0], p[1], p[2], bounds, camera, size.width.toFloat(), size.height.toFloat())
            Text(
                buildAnnotatedString {
                    append("(")
                    append(shortNumber(p[0])); append(", "); append(shortNumber(p[1])); append(", "); append(shortNumber(p[2]))
                    append(")")
                },
                color = colors.inverseOnSurface,
                style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 13.sp),
                modifier = Modifier
                    .offset { IntOffset(pinInside((sx - 70.dp.toPx()).toInt(), 8, size.width - 160.dp.roundToPx()), (sy - 52.dp.toPx()).toInt().coerceAtLeast(8)) }
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.inverseSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
            implicit != null -> Surface3D.implicit({ x, y, z -> vm.call(f, implicit, x, y, z) }, bounds, AppSettings.surfaceGrid.second, f.colorIndex)
            f.compiled != null -> Surface3D.explicit({ x, y -> vm.evaluate(f, x, y) }, bounds, AppSettings.surfaceGrid.first, f.colorIndex)
            else -> emptyList<Polygon>()
        }
    }.getOrElse { emptyList() } }

/**
 * The 3D graph for exporting, [w] × [h] units (dp), seen from the current camera: the box, the
 * shaded surfaces (as filled polygons, back to front), axis names, points and space curves.
 */
internal fun surface3DScene(vm: Graph3DViewModel, r: ExportRequest, w: Double, h: Double, scheme: androidx.compose.material3.ColorScheme): Scene {
    val scene = Scene(w, h, scheme.surface.toArgb())
    val base = surfaceBounds(vm, r.view.xMin, r.view.xMax, r.view.yMin, r.view.yMax)
    val bounds = r.z?.let { (a, b) -> base.copy(z0 = a, z1 = b) } ?: base
    val camera = vm.camera
    val fw = w.toFloat(); val fh = h.toFloat()
    val themeColors = plotColors(scheme)
    val gradients = (0 until GraphViewModel.PLOT_COLOR_COUNT).map { k ->
        val c = vm.functions.firstOrNull { it.colorIndex == k }?.customColor?.let { Color(it) } ?: themeColors[k]
        lerp(c, Color.Black, 0.35f) to lerp(c, Color.White, 0.45f)
    }
    scene.add(Scene.Stroke(Surface3D.box(camera, fw, fh).map { doubleArrayOf(it.x1.toDouble(), it.y1.toDouble(), it.x2.toDouble(), it.y2.toDouble()) }, scheme.outlineVariant.toArgb(), 1.0))
    val faces = Surface3D.faces(surfacePolygons(vm, bounds), bounds, camera, fw, fh)
    val wire = scheme.onSurface.copy(alpha = 0.12f).toArgb()
    for (face in faces) {
        val pts = DoubleArray(face.xs.size * 2) { k -> if (k % 2 == 0) face.xs[k / 2].toDouble() else face.ys[k / 2].toDouble() }
        val (lo, hi) = gradients[face.surface % gradients.size]
        val c = lerp(lo, hi, face.height)
        scene.add(Scene.Fill(listOf(pts), Color(c.red * face.shade, c.green * face.shade, c.blue * face.shade, 1f).toArgb()))
        if (face.xs.size == 4) scene.add(Scene.Stroke(listOf(pts + doubleArrayOf(pts[0], pts[1])), wire, 0.6))
    }
    Surface3D.axisLabels(camera, fw, fh).forEach { (name, p) ->
        scene.add(Scene.Label(p.first.toDouble(), p.second.toDouble(), name, 18.0, scheme.onSurfaceVariant.toArgb(), Scene.Anchor.Middle))
    }
    scene.add(Scene.Label(w - 12, 20.0, "z from ${shortNumber(bounds.z0)} to ${shortNumber(bounds.z1)}", 11.0, scheme.onSurfaceVariant.toArgb(), Scene.Anchor.End))
    vm.functions.filter { it.visible && it.space != null }.forEach { fn ->
        val (fx, fy, fz) = fn.space!!
        val (lo, hi) = gradients[fn.colorIndex % gradients.size]
        val color = lerp(lo, hi, 0.5f).toArgb()
        if (fn.spaceIsCurve) {
            val periodic = listOf(0.3, 1.1, 2.9).all { t -> abs(vm.call(fn, fx, t) - vm.call(fn, fx, t + 2 * PI)) < 1e-9 && abs(vm.call(fn, fz, t) - vm.call(fn, fz, t + 2 * PI)) < 1e-9 }
            val (a, b) = if (periodic) 0.0 to 2 * PI else -10.0 to 10.0
            val paths = ArrayList<DoubleArray>()
            var cur = ArrayList<Double>()
            for (k in 0..600) {
                val t = a + (b - a) * k / 600
                val x = vm.call(fn, fx, t); val y = vm.call(fn, fy, t); val z = vm.call(fn, fz, t)
                if (!x.isFinite() || !y.isFinite() || !z.isFinite()) { if (cur.size >= 4) paths += cur.toDoubleArray(); cur = ArrayList(); continue }
                val (sx, sy) = Surface3D.project(x, y, z, bounds, camera, fw, fh)
                cur += sx.toDouble(); cur += sy.toDouble()
            }
            if (cur.size >= 4) paths += cur.toDoubleArray()
            scene.add(Scene.Stroke(paths, color, 3.0))
        } else {
            val x = vm.call(fn, fx); val y = vm.call(fn, fy); val z = vm.call(fn, fz)
            if (x.isFinite() && y.isFinite() && z.isFinite()) {
                val (sx, sy) = Surface3D.project(x, y, z, bounds, camera, fw, fh)
                scene.add(Scene.Circle(sx.toDouble(), sy.toDouble(), 7.0, fill = color))
                scene.add(Scene.Circle(sx.toDouble(), sy.toDouble(), 2.5, fill = scheme.surface.toArgb()))
            }
        }
    }
    return scene
}
