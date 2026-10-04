package com.example.cas.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.SwipeLeft
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cas.editor.MathRow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The guide to a mode, opened by holding its button: what the mode does, examples to try (a tap
 * types it in that mode), then gestures and tips. A sheet on a phone; on a tablet a two-pane
 * dialog with every mode listed on the left.
 */
@Composable
fun ModeGuide(start: Mode, onTry: (Mode, MathRow) -> Unit, onDismiss: () -> Unit) {
    var mode by remember { mutableStateOf(start) }
    if (isTabletLayout()) TabletGuide(mode, onMode = { mode = it }, onTry = onTry, onDismiss = onDismiss)
    else PhoneGuide(mode, onMode = { mode = it }, onTry = onTry, onDismiss = onDismiss)
}

private fun guideOf(mode: Mode) = ModeGuides.all[mode.ordinal]

/** A soft nine-scalloped circle, as M3 Expressive's "cookie" shape. */
private val Cookie = GenericShape { size, _ ->
    val cx = size.width / 2; val cy = size.height / 2
    val r = minOf(cx, cy)
    for (k in 0..180) {
        val t = k * 2 * PI / 180
        val rr = r * (1 - 0.07 * (1 - cos(9 * t)) / 2)
        val x = (cx + rr * cos(t - PI / 2)).toFloat(); val y = (cy + rr * sin(t - PI / 2)).toFloat()
        if (k == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

internal fun tipIcon(key: String): ImageVector = when (key) {
    "tap" -> Icons.Default.TouchApp
    "hold" -> Icons.Default.PanTool
    "drag" -> Icons.Default.OpenWith
    "swipe" -> Icons.Default.SwipeLeft
    "swap" -> Icons.Default.SwapVert
    "graph" -> TableIcons.Mode2D
    "units" -> Icons.Default.Science
    "folder" -> TableIcons.Folder
    "table" -> TableIcons.Table
    "export" -> TableIcons.Share
    "axes" -> TableIcons.Mode3D
    else -> TableIcons.Settings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneGuide(mode: Mode, onMode: (Mode) -> Unit, onTry: (Mode, MathRow) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = colors.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            ModeChips(mode, onMode)
            Spacer(Modifier.height(16.dp))
            AnimatedContent(mode, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "guide") { m ->
                Column {
                    Hero(m)
                    Spacer(Modifier.height(20.dp))
                    Examples(m, columns = 2, onTry = onTry)
                    Spacer(Modifier.height(20.dp))
                    Tips("Gestures", guideOf(m).gestures, tertiary = false)
                    Spacer(Modifier.height(12.dp))
                    Tips("Good to know", guideOf(m).tips, tertiary = true)
                }
            }
        }
    }
}

@Composable
private fun TabletGuide(mode: Mode, onMode: (Mode) -> Unit, onTry: (Mode, MathRow) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(36.dp),
            color = colors.surfaceContainerLow,
            modifier = Modifier.width(980.dp).heightIn(max = 720.dp).fillMaxHeight(0.88f),
        ) {
            Row(Modifier.fillMaxSize()) {
                // Every mode, as a rail of large items.
                Column(
                    Modifier.width(260.dp).fillMaxHeight().background(colors.surfaceContainer).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(tr("Guide"), style = MaterialTheme.typography.titleLarge, color = colors.onSurface, modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp))
                    Mode.entries.forEach { m ->
                        val on = m == mode
                        val bg by animateColorAsState(if (on) colors.secondaryContainer else colors.surfaceContainer, label = "rail")
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(if (on) 28.dp else 16.dp)).background(bg)
                                .clickable(onClickLabel = tr("Show the guide to {0}", m.label)) { onMode(m) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIcon(m.icon, contentDescription = null, tint = if (on) colors.onSecondaryContainer else colors.onSurfaceVariant, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(guideOf(m).title, style = MaterialTheme.typography.titleSmall, color = if (on) colors.onSecondaryContainer else colors.onSurface)
                                Text("${guideOf(m).examples.size} examples", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    AnimatedContent(mode, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "guide") { m ->
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp)) {
                            Hero(m, large = true)
                            Spacer(Modifier.height(24.dp))
                            Examples(m, columns = 3, onTry = onTry)
                            Spacer(Modifier.height(24.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(Modifier.weight(1f)) { Tips("Gestures", guideOf(m).gestures, tertiary = false) }
                                Column(Modifier.weight(1f)) { Tips("Good to know", guideOf(m).tips, tertiary = true) }
                            }
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                        Icon(Icons.Default.Close, contentDescription = tr("Close"), tint = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** The four modes as chips, to move between their guides (phone). */
@Composable
private fun ModeChips(mode: Mode, onMode: (Mode) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Mode.entries.forEach { m ->
            val on = m == mode
            val bg by animateColorAsState(if (on) colors.primary else colors.surfaceContainerHigh, label = "chip")
            Row(
                Modifier.height(40.dp).clip(CircleShape).background(bg)
                    .clickable(onClickLabel = tr("Show the guide to {0}", m.label)) { onMode(m) }
                    .padding(start = 12.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(m.icon, contentDescription = null, tint = if (on) colors.onPrimary else colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(guideOf(m).title, style = MaterialTheme.typography.labelLarge, color = if (on) colors.onPrimary else colors.onSurface, maxLines = 1)
            }
        }
    }
}

/** The mode's icon in a scalloped badge, its name and what it's for. */
@Composable
private fun Hero(mode: Mode, large: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val g = guideOf(mode)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(if (large) 88.dp else 72.dp).clip(Cookie).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
            AppIcon(mode.icon, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(if (large) 40.dp else 34.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f).padding(end = if (large) 40.dp else 0.dp)) {
            Text(g.title, style = if (large) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall, color = colors.onSurface, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(4.dp))
            MathText(g.tagline, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(text: String, trailing: String? = null) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = colors.primary, modifier = Modifier.weight(1f).semantics { heading() })
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    }
}

/** The examples as cards in [columns]: what it is, the math, what happens, and Try. */
@Composable
private fun Examples(mode: Mode, columns: Int, onTry: (Mode, MathRow) -> Unit) {
    val (advanced, basics) = guideOf(mode).examples.partition { it.advanced }
    SectionTitle("Try these", "Tap one to type it in")
    ExampleGrid(mode, basics, columns, onTry)
    if (advanced.isNotEmpty()) {
        Spacer(Modifier.height(20.dp))
        SectionTitle("Go further", "${advanced.size} advanced")
        ExampleGrid(mode, advanced, columns, onTry, emphasized = true)
    }
}

@Composable
private fun ExampleGrid(mode: Mode, examples: List<ModeGuides.Example>, columns: Int, onTry: (Mode, MathRow) -> Unit, emphasized: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val tap = rememberKeyTap()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        examples.chunked(columns).forEach { line ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                line.forEach { e ->
                    // Advanced ones in the tertiary container, solid (a see-through tint went muddy in the dark theme).
                    val ink = if (emphasized) colors.onTertiaryContainer else colors.onSurface
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(22.dp))
                            .background(if (emphasized) colors.tertiaryContainer else colors.surfaceContainerHighest)
                            .clickable(onClickLabel = "Try ${e.label}") { tap(); onTry(mode, e.row) }
                            .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 10.dp),
                    ) {
                        Text(e.label, style = MaterialTheme.typography.labelLarge, color = if (emphasized) colors.onTertiaryContainer else colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                            MathView(e.row, 19.sp, ink, modifier = Modifier.semantics { contentDescription = com.example.cas.engine.Formatter.plain(e.row) })
                        }
                        Spacer(Modifier.height(6.dp))
                        MathText(e.note, style = MaterialTheme.typography.bodySmall, color = if (emphasized) colors.onTertiaryContainer.copy(alpha = 0.8f) else colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.align(Alignment.End).height(30.dp).clip(CircleShape).background(if (emphasized) colors.tertiary else colors.primaryContainer).padding(start = 12.dp, end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val tryInk = if (emphasized) colors.onTertiary else colors.onPrimaryContainer
                            Text(tr("Try"), style = MaterialTheme.typography.labelMedium, color = tryInk)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = tryInk, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                // An unfilled last row keeps its cards the same width.
                repeat(columns - line.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Gestures or tips: an icon in a tinted circle, a title and a line. */
@Composable
private fun ColumnScope.Tips(title: String, tips: List<ModeGuides.Tip>, tertiary: Boolean) {
    val colors = MaterialTheme.colorScheme
    SectionTitle(title)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.surfaceContainer).padding(vertical = 6.dp),
    ) {
        tips.forEach { t ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(if (tertiary) colors.tertiaryContainer else colors.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(tipIcon(t.icon), contentDescription = null, tint = if (tertiary) colors.onTertiaryContainer else colors.onSecondaryContainer, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                    MathText(t.text, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}
