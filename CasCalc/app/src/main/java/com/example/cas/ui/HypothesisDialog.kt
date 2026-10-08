package com.example.cas.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cas.graph.DataTable
import com.example.cas.ui.theme.CasFonts
import com.example.cas.graph.Hypothesis
import com.example.cas.graph.Hypothesis.Tail

/** The tests the dialog offers, and how many columns each takes. */
private enum class TestKind(val label: String, val columns: Int) {
    One("One sample", 1), Two("Two sample", 2), Paired("Paired", 2), Independence("χ² independence", -1), Fit("χ² fit", 1), Proportion("Proportion", 1)
}

/**
 * A hypothesis test on the data table's columns: the test, its columns and settings at the top;
 * underneath, the hypotheses, the statistic's distribution with the rejected tails shaded and the
 * observed value marked, the numbers (statistic, df, p, confidence interval) and the decision.
 * Everything updates as the choices change.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HypothesisDialog(table: DataTable, column: Int, names: List<String>, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current
    val count = table.columns.size
    fun nameOf(c: Int) = names.getOrElse(c) { "" }.ifBlank { com.example.cas.graph.Sheet.columnName(c) }
    var kind by remember { mutableStateOf(TestKind.One) }
    var first by remember { mutableStateOf(column) }
    var second by remember { mutableStateOf(((column + 1) % maxOf(count, 1)).takeIf { it != column }) }
    val chosen = remember { mutableStateListOf(column).apply { if (column + 1 < count) add(column + 1) } }
    var tail by remember { mutableStateOf(Tail.Both) }
    var value by remember { mutableStateOf("0") }
    var alphaText by remember { mutableStateOf("0.05") }
    val alpha = DataTable.number(alphaText)?.takeIf { it > 0 && it < 1 }

    // Numbers of a column; for paired tests, rows where both columns have one.
    fun numbers(c: Int) = table.numbers(c)
    val outcome: Result<Hypothesis.Result>? = if (alpha == null) null else runCatching {
        val h0 = DataTable.number(value)
        when (kind) {
            TestKind.One -> Hypothesis.oneSampleT(numbers(first), h0 ?: error("Type μ₀"), tail, alpha, short(nameOf(first)))
            TestKind.Two -> Hypothesis.twoSampleT(numbers(first), numbers(second ?: error("Choose a second column")), tail, alpha, short(nameOf(first)), short(nameOf(second!!)))
            TestKind.Paired -> {
                val b = second ?: error("Choose a second column")
                val rows = (0 until table.rowCount).filter { table.value(first, it) != null && table.value(b, it) != null }
                Hypothesis.pairedT(rows.map { table.value(first, it)!! }, rows.map { table.value(b, it)!! }, tail, alpha, short(nameOf(first)), short(nameOf(b)))
            }
            TestKind.Independence -> {
                val cs = chosen.sorted()
                val rows = (0 until table.rowCount).filter { r -> cs.all { table.value(it, r) != null } }
                Hypothesis.chiSquareIndependence(cs.map { c -> rows.map { table.value(c, it)!! } }, alpha)
            }
            TestKind.Fit -> {
                val e = second
                if (e == null) Hypothesis.chiSquareFit(numbers(first), null, alpha) else {
                    val rows = (0 until table.rowCount).filter { table.value(first, it) != null && table.value(e, it) != null }
                    Hypothesis.chiSquareFit(rows.map { table.value(first, it)!! }, rows.map { table.value(e, it)!! }, alpha)
                }
            }
            TestKind.Proportion -> {
                // The column as successes (1) and failures (0).
                val v = numbers(first)
                require(v.isNotEmpty() && v.all { it == 0.0 || it == 1.0 }) { "Use a column of 1s (successes) and 0s (failures)" }
                Hypothesis.oneProportionZ(v.count { it == 1.0 }.toDouble(), v.size.toDouble(), h0 ?: error("Type p₀"), tail, alpha)
            }
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.widthIn(max = 520.dp), shape = RoundedCornerShape(28.dp), color = colors.surfaceContainerHigh) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(TableIcons.HypothesisTest, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(outcome?.getOrNull()?.title ?: tr("Hypothesis test"), style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
                }
                // The test.
                Row(Modifier.padding(top = 14.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TestKind.entries.forEach { k -> Choice(tr(k.label), k == kind) {
                        kind = k
                        value = when (k) { TestKind.Proportion -> "0.5"; TestKind.One -> "0"; else -> value }
                        if (k == TestKind.Fit) second = null else if (k.columns == 2 && second == null) second = (0 until count).firstOrNull { it != first }
                    } }
                }
                // Its columns.
                Label(if (kind == TestKind.Independence) tr("Columns of counts") else if (kind.columns == 2) tr("First column") else tr("Column"))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (c in 0 until count) {
                        if (kind == TestKind.Independence) Choice(nameOf(c), c in chosen) { if (c in chosen) chosen.remove(c) else chosen += c }
                        else Choice(nameOf(c), c == first) { first = c; if (second == c) second = null }
                    }
                }
                if (kind.columns == 2 || kind == TestKind.Fit) {
                    Label(if (kind == TestKind.Fit) tr("Expected counts (optional; else equal shares)") else tr("Second column"))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (kind == TestKind.Fit) Choice(tr("None"), second == null) { second = null }
                        for (c in 0 until count) if (c != first) Choice(nameOf(c), c == second) { second = c }
                    }
                }
                // Its settings: the value under H₀, the direction, α.
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    val keys = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    if (kind == TestKind.One || kind == TestKind.Proportion)
                        OutlinedTextField(value, { value = it }, label = { Text(if (kind == TestKind.One) "μ₀" else "p₀") }, singleLine = true, keyboardOptions = keys, modifier = Modifier.weight(1f))
                    OutlinedTextField(alphaText, { alphaText = it }, label = { Text("α") }, singleLine = true, isError = alpha == null, keyboardOptions = keys, modifier = Modifier.weight(1f))
                }
                if (kind != TestKind.Independence && kind != TestKind.Fit) {
                    Label(tr("Alternative"))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Tail.entries.forEach { t -> Choice(t.symbol, t == tail) { tail = t } } }
                }
                Spacer(Modifier.height(14.dp))
                val r = outcome?.getOrNull()
                if (r == null) Text(outcome?.exceptionOrNull()?.message ?: tr("Choose α between 0 and 1"), color = colors.error, style = MaterialTheme.typography.bodyMedium)
                else {
                    MathText("\$H_0: ${r.h0}\$", color = colors.onSurface)
                    MathText("\$H_1: ${r.h1}\$", color = colors.onSurface)
                    DistributionPlot(r, Modifier.fillMaxWidth().height(120.dp).padding(top = 8.dp))
                    val rows = listOfNotNull(
                        r.statName to fmt(r.statistic), r.df?.let { "df" to fmt(it) }, "p" to fmt(r.p),
                        r.estimate?.let { tr(r.estimateLabel ?: "Estimate") to fmt(it) },
                        r.ci?.let { "${fmt((1 - r.alpha) * 100)}% CI" to "[${fmt(it.first)}, ${fmt(it.second)}]" },
                    )
                    rows.forEach { (k, v) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(k, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Text(v.replace("-", "−"), style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 18.sp), color = colors.onSurface)
                        }
                    }
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val (bg, fg) = if (r.reject) colors.errorContainer to colors.onErrorContainer else colors.secondaryContainer to colors.onSecondaryContainer
                        Text(if (r.reject) tr("Reject H₀") else tr("Keep H₀"), style = MaterialTheme.typography.labelLarge, color = fg,
                            modifier = Modifier.clip(CircleShape).background(bg).padding(horizontal = 12.dp, vertical = 5.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (r.reject) tr("p < α: the data are unlikely if H₀ were true.") else tr("p ≥ α: the data are consistent with H₀."),
                            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                        )
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.End) {
                    if (r != null) TextButton(onClick = {
                        clipboard.setText(AnnotatedString(buildString {
                            appendLine(r.title)
                            append("${r.statName} = ${fmt(r.statistic)}"); r.df?.let { append(", df = ${fmt(it)}") }; appendLine(", p = ${fmt(r.p)}")
                            r.ci?.let { appendLine("${fmt((1 - r.alpha) * 100)}% CI: [${fmt(it.first)}, ${fmt(it.second)}]") }
                            append(if (r.reject) "Reject H0 at α = ${fmt(r.alpha)}" else "Keep H0 at α = ${fmt(r.alpha)}")
                        }))
                    }) { Text(tr("Copy")) }
                    TextButton(onClick = onDismiss) { Text(tr("Done")) }
                }
            }
        }
    }
}

/** A column name short enough for a subscript (μ_before). */
private fun short(name: String) = name.take(12).replace(Regex("[^\\p{L}\\p{N}]"), "")

private fun fmt(v: Double): String = when {
    v == 0.0 -> "0"
    kotlin.math.abs(v) < 1e-4 -> String.format(java.util.Locale.ROOT, "%.2e", v)
    else -> String.format(java.util.Locale.ROOT, "%.4g", v).let { if ('.' in it && 'e' !in it) it.trimEnd('0').trimEnd('.') else it }
}

@Composable
private fun Label(text: String) = Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))

@Composable
private fun Choice(text: String, on: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Text(
        text, style = MaterialTheme.typography.labelLarge, color = if (on) colors.onSecondaryContainer else colors.onSurfaceVariant, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(10.dp))
            .then(if (on) Modifier.background(colors.secondaryContainer) else Modifier.border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp)))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

/** The statistic's distribution under H₀, its rejected tails shaded, and the observed value as a dashed line. */
@Composable
private fun DistributionPlot(r: Hypothesis.Result, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val line = colors.primary; val reject = colors.error; val ink = colors.onSurface; val axis = colors.outlineVariant
    Canvas(modifier) {
        val (lo, hi) = when (val d = r.dist) {
            is Hypothesis.Dist.Chi2 -> 0.0 to maxOf(d.inverse(0.999), r.statistic * 1.1, r.critical.max() * 1.2)
            else -> { val m = maxOf(4.0, kotlin.math.abs(r.statistic) * 1.15); -m to m }
        }
        val n = 160
        val xs = DoubleArray(n + 1) { lo + (hi - lo) * it / n }
        val ys = xs.map { r.dist.pdf(it).takeIf { v -> v.isFinite() } ?: 0.0 }
        val top = (ys.maxOrNull() ?: 1.0).coerceAtLeast(1e-9)
        val w = size.width; val h = size.height; val base = h - 4.dp.toPx()
        fun sx(x: Double) = ((x - lo) / (hi - lo) * w).toFloat()
        fun sy(y: Double) = (base - y / top * (base - 6.dp.toPx())).toFloat()
        // The rejected tails.
        fun tail(a: Double, b: Double) {
            if (b <= a) return
            val p = Path(); p.moveTo(sx(a), base)
            for (k in 0..40) { val x = a + (b - a) * k / 40; p.lineTo(sx(x), sy(r.dist.pdf(x).takeIf { it.isFinite() } ?: 0.0)) }
            p.lineTo(sx(b), base); p.close()
            drawPath(p, reject.copy(alpha = 0.3f))
        }
        when (r.tail) {
            Tail.Greater -> tail(r.critical.last(), hi)
            Tail.Less -> tail(lo, r.critical.first())
            Tail.Both -> { tail(lo, r.critical.first()); tail(r.critical.last(), hi) }
        }
        drawLine(axis, Offset(0f, base), Offset(w, base), 1.dp.toPx())
        val curve = Path()
        xs.forEachIndexed { k, x -> if (k == 0) curve.moveTo(sx(x), sy(ys[k])) else curve.lineTo(sx(x), sy(ys[k])) }
        drawPath(curve, line, style = Stroke(2.dp.toPx()))
        val sxv = sx(r.statistic.coerceIn(lo, hi))
        drawLine(ink, Offset(sxv, base), Offset(sxv, 4.dp.toPx()), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
    }
}
