package com.example.cas.ui

import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
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
import com.example.cas.ui.theme.CasFonts
import kotlin.math.abs
import kotlin.math.roundToInt

/** A function's color: the one picked for it, or the theme's color for its place in the list. */
@Composable
fun functionColor(f: PlotFunction): Color = f.customColor?.let { Color(it) } ?: plotColor(f.colorIndex)

/** On the complex plane, ∮ loops and curves are lines in a plain color (white unless picked); the rest are colored by a colormap. */
fun isComplexLine(f: PlotFunction) = f.contour != null || f.complexCurve != null

/** The color of a ∮ loop or curve on the complex plane. */
fun complexLineColor(f: PlotFunction): Color = f.customColor?.let { Color(it) } ?: Color.White

/** A colormap's colors in order, for drawing it as a gradient. */
fun colormapStops(map: com.example.cas.graph.Colormap, n: Int = 24): List<Color> =
    (0..n).map { k -> Color(0xFF000000.toInt() or map.rgb(k.toDouble() / n)) }

/**
 * The colors for arg f on the complex plane, as in matplotlib: the classic wheel, the cyclic
 * twilight maps, and the perceptually uniform ones. Each shows as its own gradient.
 */
@Composable
fun ColormapPickerDialog(current: com.example.cas.graph.Colormap, onPick: (com.example.cas.graph.Colormap) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Colormap") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "The color shows arg f(z). Cyclic maps join up at ±π; the others have a seam along it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                com.example.cas.graph.Colormap.entries.forEach { map ->
                    val chosen = map == current
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (chosen) colors.secondaryContainer else Color.Transparent)
                            .clickable(onClickLabel = "Use ${map.label}") { onPick(map) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .semantics { contentDescription = map.label + if (chosen) ", chosen" else "" },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.width(112.dp)) {
                            Text(map.label, color = if (chosen) colors.onSecondaryContainer else colors.onSurface, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                map.matplotlibName + if (map.cyclic) " · cyclic" else "",
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Box(Modifier.weight(1f).height(20.dp).clip(RoundedCornerShape(10.dp)).background(Brush.horizontalGradient(colormapStops(map))))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

/** The standard set offered under the color picker. */
val STANDARD_COLORS = listOf(
    Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835), Color(0xFF43A047),
    Color(0xFF00897B), Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFFD81B60),
)

/**
 * Any color, typed or picked: HSV (with slider tracks showing what each value does),
 * RGB 0–255, OKLab, or hex, with the standard eight underneath and a way back to the
 * default. Conversions are in [ColorMath].
 */
@Composable
fun ColorPickerDialog(
    initial: Color,
    onPick: (Color?) -> Unit,
    onDismiss: () -> Unit,
    /** For 2D lines: the current style (0 solid, 1 dashed, 2 dotted) and thickness; null hides these. */
    lineStyle: Int? = null,
    thickness: Float = 3f,
    onStyle: (Int, Float) -> Unit = { _, _ -> },
) {
    var style by remember { mutableStateOf(lineStyle ?: 0) }
    var width by remember { mutableStateOf(thickness) }
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    var rgb by remember { mutableStateOf(ColorMath.fromArgb(initial.toArgb())) }
    var mode by remember { mutableStateOf(0) } // 0 HSV, 1 RGB, 2 OKLab, 3 Hex
    fun fieldsFor(c: ColorMath.Rgb, m: Int): List<String> = when (m) {
        0 -> ColorMath.toHsv(c).toList().map { "%.0f".format(it) }
        1 -> listOf(c.r, c.g, c.b).map { it.toString() }
        2 -> ColorMath.toOklab(c).toList().map { "%.3f".format(it) }
        else -> listOf(ColorMath.hex(c))
    }
    var fields by remember { mutableStateOf(fieldsFor(rgb, mode)) }
    // Something other than typing changed the color: show its numbers.
    fun set(c: ColorMath.Rgb) { rgb = c; fields = fieldsFor(c, mode) }
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
        if (parsed != null) rgb = parsed
    }
    val picked = Color(rgb.argb)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Color") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(12.dp)).background(picked))
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
                // Sliders in every mode, each track showing what moving it does to this color.
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
                    else -> {
                        val (h, sat, v) = ColorMath.toHsv(rgb)
                        ColorTrack("Hue", (h / 360).toFloat(), Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f % 360f, 1f, 1f) })) { set(ColorMath.fromHsv(it * 360.0, sat, v)) }
                        ColorTrack("Saturation", (sat / 100).toFloat(), Brush.horizontalGradient(listOf(Color(ColorMath.fromHsv(h, 0.0, v).argb), Color(ColorMath.fromHsv(h, 100.0, v).argb)))) { set(ColorMath.fromHsv(h, it * 100.0, v)) }
                        ColorTrack("Value", (v / 100).toFloat(), Brush.horizontalGradient(listOf(Color.Black, Color(ColorMath.fromHsv(h, sat, 100.0).argb)))) { set(ColorMath.fromHsv(h, sat, it * 100.0)) }
                    }
                }
                val labels = when (mode) {
                    0 -> listOf("H (°)", "S (%)", "V (%)")
                    1 -> listOf("R", "G", "B")
                    2 -> listOf("L", "a", "b")
                    else -> listOf("#RRGGBB")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    labels.forEachIndexed { k, label ->
                        OutlinedTextField(
                            fields.getOrElse(k) { "" }, { typed(k, it) },
                            singleLine = true, label = { Text(label) }, modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (lineStyle != null) {
                    // Line style and thickness, as Desmos offers.
                    Text("Line", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf("Solid", "Dashed", "Dotted").forEachIndexed { k, name ->
                            SegmentedButton(
                                selected = style == k,
                                onClick = { style = k },
                                shape = SegmentedButtonDefaults.itemShape(k, 3),
                                icon = {},
                                label = { Text(name, maxLines = 1) },
                            )
                        }
                    }
                    Text("Thickness: ${"%.1f".format(width)} dp", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    ExpressiveSlider(value = width, onValueChange = { width = it }, valueRange = 1f..8f, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Line thickness" })
                }
                Text("Standard", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    STANDARD_COLORS.forEach { c ->
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable(onClickLabel = "Use this color") {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    set(ColorMath.fromArgb(c.toArgb()))
                                },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (lineStyle != null) onStyle(style, width); onPick(picked) }) { Text("Done") } },
        dismissButton = { TextButton(onClick = { if (lineStyle != null) onStyle(0, 3f); onPick(null) }) { Text("Default") } },
    )
}

/** An expressive slider whose track is a gradient showing what moving it does. */
@Composable
private fun ColorTrack(label: String, fraction: Float, track: Brush, onChange: (Float) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ExpressiveSlider(
            value = fraction,
            onValueChange = onChange,
            valueRange = 0f..1f,
            trackBrush = track,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
        )
    }
}

/** Colors for successive functions; the first two follow the theme. */
@Composable
fun plotColor(index: Int): Color = plotColors(MaterialTheme.colorScheme)[index % GraphViewModel.PLOT_COLOR_COUNT]

/** The graph colors in a color scheme, in the order functions take them. */
fun plotColors(c: androidx.compose.material3.ColorScheme): List<Color> =
    listOf(c.primary, c.tertiary, Color(0xFF4C8DF6), Color(0xFFE8710A), Color(0xFF1E9E54), c.error)

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
 * The list of functions under a graph, each typed in maths notation with a
 * color dot (tap it to hide the curve), plus sliders for parameters.
 * While a function is being edited, only that one is shown so the keypad fits.
 */
@Composable
fun FunctionList(vm: GraphViewModel, outputLabel: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val editing = vm.active
    // While a line is being edited only that line shows, unless the keypad is hidden: then the
    // whole list is back, as it is after Enter.
    val shown = if (editing != null && !vm.keypadHidden) listOf(editing) else vm.functions.toList()
    val editingNow = editing?.takeIf { !vm.keypadHidden }
    Column(
        modifier
            .fillMaxWidth()
            .heightIn(max = if (editingNow != null) 200.dp else 280.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (editingNow != null) {
            shown.forEach { f -> FunctionRow(vm, f, outputLabel) }
        } else {
            // Hold a row and drag it up or down to reorder the list.
            ReorderableRows(vm) { f ->
                Column {
                    FunctionRow(vm, f, outputLabel)
                    if (vm.canFit(f)) FitButton(vm, f)
                }
            }
        }
        val params = shown.filter { it.visible }.flatMap { it.parameters }.distinct() - vm.definedLetters
        params.forEach { p -> ParameterSlider(vm, p) }
        // "Add" is always there, even while a line is being edited.
        run {
            // Add a line: type anything (a function, an equation, a point, a region…).
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(colors.secondaryContainer)
                    .clickable(onClickLabel = "Add a line") { vm.add() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add", color = colors.onSecondaryContainer, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
    // Moves playing sliders on every frame.
    val anyPlaying = vm.playing.isNotEmpty()
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
private fun FunctionRow(vm: GraphViewModel, f: PlotFunction, outputLabel: String) {
    val colors = MaterialTheme.colorScheme
    val active = vm.active === f
    val color = if (vm.isComplex && isComplexLine(f)) complexLineColor(f) else functionColor(f)
    // On the complex plane a function's dot shows its colormap round a ring; loops and curves keep a plain color.
    val colormapDot = vm.isComplex && !isComplexLine(f)
    var picking by remember { mutableStateOf(false) }
    if (picking && colormapDot) {
        ColormapPickerDialog(f.colormap, onPick = { vm.setColormap(f, it) }, onDismiss = { picking = false })
    } else if (picking) {
        ColorPickerDialog(
            initial = color,
            onPick = { vm.setColor(f, it?.toArgb()); picking = false },
            onDismiss = { picking = false },
            // 2D lines also get style and thickness.
            lineStyle = if (vm.plotVars == listOf("x")) f.lineStyle else null,
            thickness = f.thickness,
            onStyle = { st, w -> vm.setStyle(f, st, w) },
        )
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) colors.surfaceContainerHigh else colors.surfaceContainer)
            .clickable(onClickLabel = "Edit this function") { vm.edit(f) }
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
                if (colormapDot) {
                    val ring = Brush.sweepGradient(colormapStops(f.colormap))
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
            Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                MathView(
                    row = f.editor.root,
                    fontSize = 22.sp,
                    color = colors.onSurface,
                    accent = colors.primary,
                    cursorRow = if (active) f.editor.row else null,
                    cursorIndex = f.editor.index,
                    version = f.version,
                    onTap = { r, i -> vm.tapAt(f, r, i) },
                )
            }
            IconButton(onClick = { vm.remove(f) }) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = colors.onSurfaceVariant)
            }
        }
        f.error?.let {
            Text(it, color = colors.error, style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 13.sp), modifier = Modifier.padding(start = 48.dp, bottom = 2.dp))
        }
    }
}

@Composable
private fun ParameterSlider(vm: GraphViewModel, name: String) {
    val colors = MaterialTheme.colorScheme
    val value = vm.parameters[name] ?: 1.0
    val (lo, hi) = vm.rangeOf(name)
    var editing by remember { mutableStateOf(false) }
    if (editing) SliderDialog(name, value, lo, hi, onDone = { v, a, b -> vm.setSlider(name, v, a, b); editing = false }, onDismiss = { editing = false })
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        val playing = name in vm.playing
        IconButton(onClick = { vm.togglePlay(name) }, modifier = Modifier.size(40.dp)) {
            Icon(
                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (playing) "Pause $name" else "Animate $name",
                tint = if (playing) colors.primary else colors.onSurfaceVariant,
            )
        }
        Text(name, color = colors.onSurface, style = TextStyle(fontFamily = CasFonts.CmItalic, fontSize = 20.sp), modifier = Modifier.width(28.dp))
        val step = (hi - lo) / 200
        ExpressiveSlider(
            value = value.toFloat(),
            onValueChange = { vm.setParameter(name, Math.round(it / step) * step) },
            valueRange = lo.toFloat()..hi.toFloat(),
            modifier = Modifier.weight(1f).semantics { contentDescription = "Value of $name, from ${shortNumber(lo)} to ${shortNumber(hi)}" },
        )
        // Tap the value to type it, or change the slider's range.
        Box(
            Modifier
                .padding(start = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceContainerHigh)
                .clickable(onClickLabel = "Type a value for $name") { editing = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            // Always the same decimals for this range (its step), in a width that fits either end,
            // so the number doesn't jitter as the slider moves.
            val decimals = sliderDecimals(lo, hi)
            val widest = maxOf(sliderText(lo, decimals).length, sliderText(hi, decimals).length)
            Text(
                sliderText(value, decimals),
                color = colors.onSurface,
                style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 17.sp, textAlign = androidx.compose.ui.text.style.TextAlign.End),
                modifier = Modifier.widthIn(min = (widest * 9).dp),
            )
        }
    }
}

/** Type a slider's value and its range (−10 to 10 unless changed). A line like a = 3 also sets it. */
@Composable
private fun SliderDialog(name: String, value: Double, min: Double, max: Double, onDone: (Double, Double, Double) -> Unit, onDismiss: () -> Unit) {
    fun text(v: Double) = shortNumber(v).replace("−", "-")
    var v by remember { mutableStateOf(text(value)) }
    var a by remember { mutableStateOf(text(min)) }
    var b by remember { mutableStateOf(text(max)) }
    fun num(t: String) = t.trim().replace("−", "-").toDoubleOrNull()
    val nv = num(v); val na = num(a); val nb = num(b)
    val valid = nv != null && na != null && nb != null && na < nb
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(mathLabel(name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(v, { v = it }, singleLine = true, label = { Text("Value") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(a, { a = it }, singleLine = true, label = { Text("From") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(b, { b = it }, singleLine = true, label = { Text("To") }, modifier = Modifier.weight(1f))
                }
                Text(
                    "You can also set it with a line of its own, like $name = 3.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onDone(nv!!, na!!, nb!!) }) { Text("Done") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * A label like "f(z) =" or "(x, y) =": letters in italic Computer Modern, brackets, commas
 * and = upright, as in the maths.
 */
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
    onGesture: (centroid: Offset, pan: Offset, zoomX: Double, zoomY: Double) -> Unit,
) {
    val minSpread = 48.dp.toPx()
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
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
 * moves, and passes a neighbour once it's gone halfway past it.
 */
@Composable
private fun ReorderableRows(vm: GraphViewModel, row: @Composable (PlotFunction) -> Unit) {
    val heights = remember { mutableStateMapOf<PlotFunction, Int>() }
    var dragging by remember { mutableStateOf<PlotFunction?>(null) }
    var offset by remember { mutableStateOf(0f) }
    val tap = rememberKeyTap()
    val gap = with(LocalDensity.current) { 6.dp.toPx() }
    vm.functions.toList().forEach { f ->
        val lifted = dragging === f
        Box(
            Modifier
                .zIndex(if (lifted) 1f else 0f)
                .offset { IntOffset(0, if (lifted) offset.roundToInt() else 0) }
                .graphicsLayer { if (lifted) { shadowElevation = 12f; scaleX = 1.02f; scaleY = 1.02f } }
                .onGloballyPositioned { heights[f] = it.size.height }
                .pointerInput(f) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { tap(); dragging = f; offset = 0f },
                        onDragEnd = { dragging = null; offset = 0f },
                        onDragCancel = { dragging = null; offset = 0f },
                        onDrag = { change, amount ->
                            change.consume()
                            offset += amount.y
                            val i = vm.functions.indexOf(f)
                            // Past half of the next row down, or the one above: swap with it.
                            val below = vm.functions.getOrNull(i + 1)
                            val above = vm.functions.getOrNull(i - 1)
                            if (below != null && offset > (heights[below] ?: 0) / 2f + gap) {
                                offset -= (heights[below] ?: 0) + gap
                                vm.move(i, i + 1)
                                tap()
                            } else if (above != null && offset < -((heights[above] ?: 0) / 2f + gap)) {
                                offset += (heights[above] ?: 0) + gap
                                vm.move(i, i - 1)
                                tap()
                            }
                        },
                    )
                },
        ) { row(f) }
    }
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
    Row(Modifier.padding(start = 44.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .clip(CircleShape)
                .background(colors.tertiaryContainer)
                .clickable(onClickLabel = "Fit to the list") { tap(); failed = !vm.fit(f) }
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Fit", color = colors.onTertiaryContainer, style = MaterialTheme.typography.labelLarge)
        }
        if (failed) {
            Spacer(Modifier.width(10.dp))
            Text("Couldn't fit this to the points", color = colors.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
