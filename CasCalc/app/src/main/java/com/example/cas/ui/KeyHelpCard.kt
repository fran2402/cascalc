package com.example.cas.ui

import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cas.editor.MathCodec
import com.example.cas.editor.MathRow
import com.example.cas.engine.LatexParser

/**
 * A key's long-press card: the key as it looks, its name and where it is, its formula, what it is,
 * how to use it step by step, and worked examples, each with its answer (worked out by the
 * calculator) and Try it, which types it in. On a phone it's a sheet over the keypad; on a tablet a
 * two-column dialog. [actions] are a symbol's own buttons (pin, undefine, remove), if any.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun KeyHelpCard(spec: KeySpec, onDismiss: () -> Unit, onTry: (MathRow) -> Unit, actions: (@Composable RowScope.() -> Unit)? = null) {
    val help = remember(spec.spoken) { KeyHelps.of(spec.spoken) }
    val where = remember(spec.spoken) { placeOf(spec) }
    if (isTabletLayout()) {
        androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.widthIn(max = 1040.dp).fillMaxWidth(0.88f).heightIn(max = 760.dp), shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.padding(start = 28.dp, end = 28.dp, top = 26.dp, bottom = 14.dp)) {
                    Row(Modifier.weight(1f, fill = false), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        Column(Modifier.weight(1.05f).verticalScroll(rememberScrollState())) {
                            Header(spec, help, where)
                            Formula(help)
                            About(help)
                            Graph(help)
                            Link(help)
                        }
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            Steps(help, first = true)
                            Examples(help, onTry)
                        }
                    }
                    Actions(onDismiss, actions)
                }
            }
        }
    } else {
        StillSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
                Header(spec, help, where)
                Formula(help)
                About(help)
                Graph(help)
                Steps(help)
                Examples(help, onTry)
                Link(help)
                Actions(onDismiss, actions)
            }
        }
    }
}

/** Where a key lives: its tab, and the page or button that shows it. */
private fun placeOf(spec: KeySpec): String {
    fun has(rows: List<List<KeySpec>>) = rows.any { r -> r.any { it.spoken == spec.spoken } }
    return when {
        has(ReciprocalTrigKeys) -> "Trigonometry tab · 1/△ page"
        has(DistributionKeys) -> "Statistics tab · pdf/cdf page"
        has(SignalKeys) -> "Graphs · the signals button"
        has(MainKeys) -> "Number pad"
        else -> FunctionTabs.firstOrNull { has(it.keys) || (it.title == "Symbols" && (spec.label as? KeyLabel.Math)?.latex == true) }?.let { "${tr(it.title)} tab" } ?: ""
    }
}

@Composable
private fun Header(spec: KeySpec, help: KeyHelp, where: String) {
    val colors = MaterialTheme.colorScheme
    // The key's own two-tone picture, as on the keypad, stands for its name.
    val picture = KeyIcons.forKey(spec.spoken)
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (picture != null) {
            val aspect = picture.viewportWidth / picture.viewportHeight
            Box(
                Modifier.height(72.dp).widthIn(min = 72.dp).clip(RoundedCornerShape(28.dp)).background(colors.surfaceContainerHighest)
                    .padding(horizontal = 16.dp).semantics { contentDescription = help.title; heading() },
                contentAlignment = Alignment.Center,
            ) { DuoIcon(picture, colors.onSurface, colors.primary, Modifier.size(width = (44 * aspect).dp, height = 44.dp)) }
            if (where.isNotEmpty()) {
                Spacer(Modifier.width(14.dp))
                Text(where, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
            }
        } else {
            // Keys without a picture (digits, letters, constants): the label and the name.
            Box(
                Modifier.size(width = 76.dp, height = 56.dp).clip(RoundedCornerShape(26.dp)).background(colors.surfaceContainerHighest).padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) { LabelView(spec.label, colors.onSurface, fontSize = 20f, iconSize = 30.dp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(help.title, style = MaterialTheme.typography.titleLarge, color = colors.onSurface, modifier = Modifier.semantics { heading() })
                if (where.isNotEmpty()) Text(where, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Formula(help: KeyHelp) {
    if (help.formula.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    val lines = remember(help.formula) { LatexParser.lines(help.formula) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.primaryContainer).horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { lines.forEach { MathView(it, 22.sp, colors.onPrimaryContainer) } }
}

@Composable
private fun SectionTitle(text: String, first: Boolean = false) {
    Text(tr(text), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = if (first) 0.dp else 18.dp, bottom = 8.dp).semantics { heading() })
}

@Composable
private fun About(help: KeyHelp) {
    if (help.about.isEmpty()) return
    SectionTitle("What it is")
    MathText(help.about, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
}

/**
 * A small graph of the key's function (or a family of them): just the curves, in the theme's graph
 * colors, over the axes where they're in view, with a legend in LaTeX.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Graph(help: KeyHelp) {
    val plot = help.plot ?: return
    val colors = MaterialTheme.colorScheme
    // The curves spread round the theme's nine graph hues, so even two or three are far apart.
    val palette = plotColors(colors)
    val lines = List(plot.curves.size) { k -> palette[(k * palette.size / plot.curves.size.coerceAtLeast(1)) % palette.size] }
    val samples by produceState<List<DoubleArray>?>(null, plot) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { runCatching { KeyGuides.sample(plot) }.getOrNull() }
    }
    Column(Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(24.dp)).background(colors.surfaceContainer).padding(14.dp)) {
        val ys = samples ?: return@Column
        val yRange = plot.y ?: run {
            val all = ys.flatMap { it.filter { v -> v.isFinite() } }.sorted()
            if (all.isEmpty()) return@Column
            val lo = all[all.size / 20]; val hi = all[all.size - 1 - all.size / 20]
            val pad = ((hi - lo) * 0.1).coerceAtLeast(0.5)
            (lo - pad)..(hi + pad)
        }
        val axis = colors.outline
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(170.dp).semantics { contentDescription = "Graph" }) {
            val x0 = plot.x.start; val x1 = plot.x.endInclusive
            val y0 = yRange.start; val y1 = yRange.endInclusive
            fun px(x: Double) = ((x - x0) / (x1 - x0) * size.width).toFloat()
            fun py(y: Double) = ((y1 - y) / (y1 - y0) * size.height).toFloat()
            if (x0 < 0 && x1 > 0) drawLine(axis, androidx.compose.ui.geometry.Offset(px(0.0), 0f), androidx.compose.ui.geometry.Offset(px(0.0), size.height), 1.5f)
            if (y0 < 0 && y1 > 0) drawLine(axis, androidx.compose.ui.geometry.Offset(0f, py(0.0)), androidx.compose.ui.geometry.Offset(size.width, py(0.0)), 1.5f)
            clipRect {
                // The first curve last, so it's drawn on top, as in the graphs.
                ys.indices.reversed().forEach { k ->
                    val v = ys[k]
                    val path = androidx.compose.ui.graphics.Path()
                    var open = false
                    for (i in v.indices) {
                        val y = v[i]
                        // A gap where the curve is undefined, or jumps across the view (a pole or a step).
                        val jump = i > 0 && v[i - 1].isFinite() && y.isFinite() && kotlin.math.abs(y - v[i - 1]) > (y1 - y0) * 0.75
                        if (!y.isFinite() || jump) { open = false; if (!y.isFinite()) continue }
                        val x = x0 + (x1 - x0) * i / (v.size - 1)
                        val yy = y.coerceIn(y0 - (y1 - y0) * 4, y1 + (y1 - y0) * 4)
                        if (open) path.lineTo(px(x), py(yy)) else { path.moveTo(px(x), py(yy)); open = true }
                    }
                    drawPath(path, lines[k % lines.size], style = androidx.compose.ui.graphics.drawscope.Stroke(2.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                }
            }
        }
        if (plot.curves.size > 1 || plot.curves[0].label.isNotEmpty()) {
            androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                plot.curves.forEachIndexed { k, c ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(width = 16.dp, height = 3.dp).clip(RoundedCornerShape(2.dp)).background(lines[k % lines.size]))
                        Spacer(Modifier.width(6.dp))
                        MathText("\\(${c.label}\\)", style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun Steps(help: KeyHelp, first: Boolean = false) {
    if (help.steps.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    SectionTitle("How to use", first)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        help.steps.forEachIndexed { k, step ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainer).padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(colors.primary), contentAlignment = Alignment.Center) {
                    Text("${k + 1}", style = MaterialTheme.typography.labelMedium, color = colors.onPrimary)
                }
                Spacer(Modifier.width(10.dp))
                MathText(step, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
            }
        }
    }
}

@Composable
private fun Examples(help: KeyHelp, onTry: (MathRow) -> Unit) {
    if (help.examples.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    SectionTitle("Examples")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        help.examples.forEach { e ->
            // The answer, worked out by the calculator itself (off the main thread).
            val answer by produceState<Pair<MathRow, Boolean>?>(null, e) {
                value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    runCatching { KeyGuides.answer(e) }.getOrNull()
                }
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.secondaryContainer).padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                        MathView(e.row, 20.sp, colors.onSecondaryContainer)
                        answer?.let { (a, approx) ->
                            Text(if (approx) "  ≈  " else "  =  ", style = TextStyle(fontFamily = com.example.cas.ui.theme.CasFonts.CmRoman, fontSize = 20.sp), color = colors.onSecondaryContainer)
                            MathView(a, 20.sp, colors.primary)
                        }
                    }
                    val note = listOfNotNull(e.note.takeIf { it.isNotEmpty() }, if (e.degrees && "degree" !in e.note) tr("In degrees.") else null).joinToString(" ")
                    if (note.isNotEmpty()) MathText(note, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.height(36.dp).clip(CircleShape).background(colors.primary).clickable(onClickLabel = tr("Type this example")) { onTry(MathCodec.copy(e.row)) }
                        .padding(horizontal = 14.dp).semantics { contentDescription = tr("Try it") },
                    contentAlignment = Alignment.Center,
                ) { Text(tr("Try it"), style = MaterialTheme.typography.labelLarge, color = colors.onPrimary) }
            }
        }
    }
}

@Composable
private fun Link(help: KeyHelp) {
    val url = help.link ?: return
    val colors = MaterialTheme.colorScheme
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    Row(Modifier.padding(top = 12.dp).clip(RoundedCornerShape(12.dp)).clickable(onClickLabel = tr("Open the NIST page")) { runCatching { uri.openUri(url) } }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIcon(TableIcons.External, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(tr("NIST CODATA value and uncertainty"), style = MaterialTheme.typography.bodyMedium.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), color = colors.primary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.Actions(onDismiss: () -> Unit, actions: (@Composable RowScope.() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
        actions?.invoke(this)
        TextButton(onClick = onDismiss) { Text(tr("Close")) }
    }
}
