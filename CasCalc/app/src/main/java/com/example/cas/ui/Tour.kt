package com.example.cas.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cas.ui.theme.CasFonts

/**
 * The first-run tour: a welcome page, then a few stops on the calculator. Each stop dims the
 * screen, outlines one part of it (the input, the tabs, an answer, the mode bar, history, the
 * menu) and says what it does in a card. Skip tour is on the welcome page and in every card;
 * it's shown once, and again from Settings › Help & feedback › Take the tour.
 */
object Tour {
    /** Where each part the tour points at is on the screen, as its composable reports it. */
    val targets = mutableStateMapOf<String, Rect>()
    var welcome by mutableStateOf(false)
    var active by mutableStateOf(false)
    var step by mutableIntStateOf(0)

    class Stop(val target: String, val title: String, val text: String)

    val stops = listOf(
        Stop("input", "Type math as you write it", "Fractions, powers and roots build up as you type. The live answer shows underneath before you press Enter."),
        Stop("tabs", "More keys in the tabs", "Tap a tab for calculus, matrices, statistics, complex numbers, constants and units. Hold any key to see what it does, with examples."),
        Stop("answer", "Every answer can do more", "Steps shows how it was worked out, Graph sends it to a graph, and the answer's menu shares it, copies its LaTeX or saves it as an image."),
        Stop("modes", "Graphs, 3D and the complex plane", "Plot in 2D, in 3D or on the complex plane, and build geometry. What you type here goes there with one tap."),
        Stop("history", "Your history", "Every calculation is kept: search it, pin the ones you need, and sort them into folders."),
        Stop("more", "Settings and help", "Settings, the documentation with its worked examples, and this tour again, any time."),
    )

    /** The stops whose part is on the screen now (no answer yet: that stop is left out). */
    fun shown(): List<Stop> = stops.filter { targets[it.target]?.let { r -> r.width > 0f && r.height > 0f } == true }

    fun offerOnFirstRun() { if (!AppSettings.tourSeen) welcome = true }
    fun start() { welcome = false; step = 0; active = true }
    fun finish() { welcome = false; active = false; AppSettings.markTourSeen() }
}

/** Marks a part of the screen the tour can point at. */
fun Modifier.tourTarget(key: String): Modifier = composed {
    androidx.compose.runtime.DisposableEffect(key) { onDispose { Tour.targets.remove(key) } }
    onGloballyPositioned { Tour.targets[key] = it.boundsInRoot() }
}

/** The tour over the whole app: the welcome page, or the current stop. Put it last in the app's root box. */
@Composable
fun TourOverlay() {
    AnimatedVisibility(Tour.welcome, enter = fadeIn(), exit = fadeOut()) { Welcome() }
    AnimatedVisibility(Tour.active, enter = fadeIn(), exit = fadeOut()) { Stops() }
}

@Composable
private fun Welcome() {
    val colors = MaterialTheme.colorScheme
    BackHandler { Tour.finish() }
    Box(Modifier.fillMaxSize().background(colors.surface).pointerInput(Unit) { detectTapGestures { } }.systemBarsPadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(0.6f))
            Box(Modifier.size(112.dp).clip(RoundedCornerShape(34.dp)).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                Text("∫", style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 64.sp), color = colors.onPrimaryContainer)
            }
            Spacer(Modifier.height(28.dp))
            Text(tr("Welcome"), style = MaterialTheme.typography.displaySmall, color = colors.onSurface)
            Spacer(Modifier.height(10.dp))
            Text(tr("A calculator that does algebra, calculus and graphs, and shows its working."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            val features = listOf(
                Triple(TableIcons.ModeCalculator, "Exact answers", "Integrals, limits, equations, matrices"),
                Triple(TableIcons.Mode2D, "Graphs in 2D, 3D and the complex plane", "And geometry you can drag"),
                Triple(TableIcons.Help, "Step by step", "See how each answer was found"),
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                features.forEachIndexed { k, (icon, title, line) ->
                    val big = 24.dp; val small = 6.dp
                    val shape = RoundedCornerShape(if (k == 0) big else small, if (k == 0) big else small, if (k == features.lastIndex) big else small, if (k == features.lastIndex) big else small)
                    Row(Modifier.fillMaxWidth().clip(shape).background(colors.surfaceContainerHigh).padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(colors.secondaryContainer), contentAlignment = Alignment.Center) {
                            AppIcon(icon, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(tr(title), style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                            Text(tr(line), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = { Tour.start() }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(tr("Take the tour") + " · " + tr("1 minute")) }
            TextButton(onClick = { Tour.finish() }, modifier = Modifier.fillMaxWidth()) { Text(tr("Skip tour")) }
        }
    }
}

@Composable
private fun Stops() {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val stops = Tour.shown()
    if (stops.isEmpty()) return
    val k = Tour.step.coerceIn(0, stops.lastIndex)
    val stop = stops[k]
    BackHandler { if (k > 0) Tour.step = k - 1 else Tour.finish() }
    val pad = with(density) { 6.dp.toPx() }
    val target = Tour.targets[stop.target]?.let { Rect(it.left - pad, it.top - pad, it.right + pad, it.bottom + pad) } ?: Rect.Zero
    // The outline glides from one part to the next.
    val spot by animateValueAsState(target, Rect.VectorConverter, tween(380), label = "spot")
    BoxWithConstraints(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { } }) {
        val height = with(density) { maxHeight.toPx() }
        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            drawRect(Color.Black.copy(alpha = 0.66f))
            val radius = CornerRadius(minOf(30.dp.toPx(), spot.height / 2))
            drawRoundRect(Color.Transparent, Offset(spot.left, spot.top), Size(spot.width, spot.height), radius, blendMode = BlendMode.Clear)
            drawRoundRect(colors.primary, Offset(spot.left, spot.top), Size(spot.width, spot.height), radius, style = Stroke(3.dp.toPx()))
        }
        // The card below the part when it's in the top half, above it otherwise.
        val below = target.center.y < height / 2
        val gap = with(density) { 16.dp.toPx() }
        val offset = with(density) { (if (below) target.bottom + gap else height - target.top + gap).toDp() }
        Surface(
            shape = RoundedCornerShape(28.dp), color = colors.surfaceContainerHigh, shadowElevation = 6.dp,
            modifier = Modifier.align(if (below) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(top = if (below) offset else 0.dp, bottom = if (below) 0.dp else offset)
                .padding(horizontal = 16.dp).widthIn(max = 460.dp).fillMaxWidth(),
        ) {
            Column(Modifier.padding(start = 22.dp, end = 18.dp, top = 18.dp, bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("{0} of {1}", k + 1, stops.size), style = MaterialTheme.typography.labelMedium, color = colors.primary, modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        stops.indices.forEach { i ->
                            Box(Modifier.height(6.dp).width(if (i == k) 18.dp else 6.dp).clip(CircleShape).background(if (i == k) colors.primary else colors.outlineVariant))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(tr(stop.title), style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(tr(stop.text), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val last = k == stops.lastIndex
                    if (!last) TextButton(onClick = { Tour.finish() }) { Text(tr("Skip tour")) }
                    Spacer(Modifier.weight(1f))
                    if (k > 0) OutlinedButton(onClick = { Tour.step = k - 1 }) { Text(tr("Back")) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { if (last) Tour.finish() else Tour.step = k + 1 }) { Text(tr(if (last) "Done" else "Next")) }
                }
            }
        }
    }
}
