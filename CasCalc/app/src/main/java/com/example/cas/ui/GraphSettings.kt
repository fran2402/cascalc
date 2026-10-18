package com.example.cas.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cas.ui.theme.CasFonts

/**
 * The frame every graph's settings share (2D, 3D and the complex plane), in the style of the
 * app's own settings: a title with the kind of graph under it, the settings in grouped cards
 * under section names (View, Axes, Coordinates, Detail), and Cancel and Done at the bottom.
 * Done applies the typed limits; switches and detail take effect at once.
 */
@Composable
internal fun GraphSettingsFrame(kind: String, valid: Boolean, onDone: () -> Unit, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tablet = isTabletLayout()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // As the color and colormap dialogs: the same card, title, flat rows and text buttons.
        Surface(shape = RoundedCornerShape(28.dp), color = colors.surfaceContainerHigh, modifier = if (tablet) Modifier.width(560.dp) else Modifier.fillMaxWidth(0.94f)) {
            Column(Modifier.padding(top = 20.dp, bottom = 12.dp)) {
                Text(tr("Graph settings"), style = MaterialTheme.typography.headlineSmall, color = colors.onSurface, modifier = Modifier.padding(horizontal = 24.dp))
                Text(tr(kind), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))
                CompositionLocalProvider(LocalCompactRows provides true) {
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                        content()
                        Spacer(Modifier.padding(top = 4.dp))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text(tr("Cancel")) }
                    TextButton(enabled = valid, onClick = onDone) { Text(tr("Done")) }
                }
            }
        }
    }
}

/** One axis's limits in a card: from, the axis's letter (in the math font), to. */
@Composable
internal fun LimitRow(math: String, lo: String, hi: String, onLo: (String) -> Unit, onHi: (String) -> Unit) {
    val compact = LocalCompactRows.current
    Row(Modifier.fillMaxWidth().padding(horizontal = if (compact) 0.dp else 16.dp, vertical = if (compact) 2.dp else 12.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(lo, onLo, singleLine = true, label = { Text(tr("from")) }, modifier = Modifier.weight(1f))
        Box(Modifier.width(if (compact) 52.dp else 64.dp), contentAlignment = Alignment.Center) {
            MathText("\$$math\$", style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 20.sp), color = MaterialTheme.colorScheme.onSurface, mathScale = 1f, maxLines = 1)
        }
        OutlinedTextField(hi, onHi, singleLine = true, label = { Text(tr("to")) }, modifier = Modifier.weight(1f))
    }
}

/** A line in the error color under the cards when a limit can't be used. */
@Composable
internal fun LimitError(text: String) =
    Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp))

/**
 * The View and Axes sections of the 2D graph and the complex plane: each axis's limits, then
 * linear or log for each axis, the grid lines and the numbers along the axes.
 */
@Composable
internal fun RangeAndAxesSections(state: RangeFields, xMath: String, yMath: String) {
    PxHeader("View")
    PxCards(
        { LimitRow(xMath, state.fields[0], state.fields[1], { state.edit(0, it) }, { state.edit(1, it) }) },
        { LimitRow(yMath, state.fields[2], state.fields[3], { state.edit(2, it) }, { state.edit(3, it) }) },
    )
    if (!state.valid) LimitError(tr("Each “from” must be a number below its “to”") + if (state.logX || state.logY) ", and above 0 on a log axis." else ".")
    PxHeader("Axes")
    PxCards(
        { PxChoiceRow("Scale of \$$xMath\$", listOf("Linear", "Log"), if (state.logX) 1 else 0) { state.setLog(it == 1, state.logY) } },
        { PxChoiceRow("Scale of \$$yMath\$", listOf("Linear", "Log"), if (state.logY) 1 else 0) { state.setLog(state.logX, it == 1) } },
        { PxToggleRow("Grid lines", null, AppSettings.showGrid, AppSettings::changeShowGrid) },
        { PxToggleRow("Numbers on the axes", null, AppSettings.axisNumbers, AppSettings::changeAxisNumbers) },
    )
}

/** A card row that opens something: a title, a line, and a trailing icon. */
@Composable
internal fun PxActionRow(title: String, detail: String?, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val compact = LocalCompactRows.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = if (compact) 48.dp else 72.dp).clickable(onClickLabel = tr(title), onClick = onClick).then(if (compact) Modifier.padding(vertical = 4.dp) else PxRowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (compact) Column(Modifier.weight(1f)) {
            MathText(tr(title), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (detail != null) MathText(tr(detail), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else PxRowTexts(title, detail, Modifier.weight(1f))
        Spacer(Modifier.width(16.dp))
        AppIcon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
