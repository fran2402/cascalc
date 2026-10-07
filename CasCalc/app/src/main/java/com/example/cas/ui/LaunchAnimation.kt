package com.example.cas.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * The app's icon drawing itself as the app opens (from icon.svg, its 1080-unit artboard): the
 * three axes grow out from the middle, the hexagon is traced round, then the curve is written
 * in one stroke. Then the icon swells a little and the whole screen fades to the app. A tap skips it.
 */
@Composable
fun LaunchAnimation(onDone: () -> Unit) {
    val axes = remember {
        // Each axis from the centre outwards, so they grow from the middle.
        listOf("M538.54,559.05L169.6,766.19", "M538.54,559.05L910.4,761.28", "M538.54,559.05L538.54,127.17").map { PathParser().parsePathString(it).toPath() }
    }
    val hexagon = remember { PathParser().parsePathString(HEXAGON).toPath() }
    val curve = remember { PathParser().parsePathString(CURVE).toPath() }
    val time = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        // Brief: the icon draws itself in about half a second, then fades (a tap skips it).
        time.animateTo(1f, tween(550, easing = androidx.compose.animation.core.LinearEasing))
        exit.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
        onDone()
    }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = 1f - exit.value }
            .background(BACKGROUND)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                scope.launch { time.snapTo(1f) }
            }
            .semantics { contentDescription = "CAS Calculator" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(220.dp).graphicsLayer {
            // A gentle settle as it draws, then a swell as it leaves.
            val s = 0.94f + 0.06f * FastOutSlowInEasing.transform(time.value) + 0.18f * exit.value
            scaleX = s; scaleY = s
        }) {
            val k = size.minDimension / 1080f
            val t = time.value
            // Each part's share of the timeline: (start, end) as fractions.
            fun phase(a: Float, b: Float) = FastOutSlowInEasing.transform(((t - a) / (b - a)).coerceIn(0f, 1f))
            fun trimmed(p: Path, f: Float): Path {
                if (f >= 1f) return p
                val m = PathMeasure(); m.setPath(p, false)
                val out = Path()
                if (f > 0f) m.getSegment(0f, m.length * f, out, true)
                return out
            }
            scale(k, k, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                val axis = phase(0f, 0.38f)
                axes.forEach { drawPath(trimmed(it, axis), AXES, style = Stroke(45f, cap = StrokeCap.Round)) }
                drawPath(trimmed(hexagon, phase(0.18f, 0.72f)), LINE, style = Stroke(55f, miter = 10f, cap = StrokeCap.Round))
                drawPath(trimmed(curve, phase(0.45f, 1f)), LINE, style = Stroke(38f, cap = StrokeCap.Round))
            }
        }
    }
}

private val BACKGROUND = Color(0xFF3E4F3C)
private val AXES = Color(0xFF7F886B)
private val LINE = Color(0xFFF3ECDA)

private const val HEXAGON = "M809.6,684.27v-268.05c0-13.77-7.34-26.49-19.27-33.37l-232.14-134.03c-11.92-6.88-26.61-6.88-38.53,0l-232.14,134.03c-11.92,6.88-19.27,19.6-19.27,33.37v268.05c0,13.77,7.34,26.49,19.27,33.37l232.14,134.03c11.92,6.88,26.61,6.88,38.53,0l232.14-134.03c11.92-6.88,19.27-19.6,19.27-33.37Z"
private const val CURVE = "M363.24,545.48c3.62-15.66,9.46-30.7,17.31-44.8,9.48-18.06,21.36-34.57,36.04-48.77,24.91-25.67,57.69-46.18,94.01-48.86,31.68-2.14,58.82,17.31,71.71,45.59,10.58,23.27,10.96,49.82,8.42,74.86-4.5,35.08-17.67,69.32-38.97,97.65-12.04,16.1-27.75,32.11-47.92,36.54-10.86,2.33-21.88-1.89-27.19-11.53-7.14-13.15-5.19-29.39-2.25-43.6,6.01-25.9,20.46-48.74,36.26-69.79,10.1-13.54,22.12-25.4,35.73-35.4,17.11-12.9,36.03-24.34,57.18-28.71,27.58-5.75,57.69,0.2,75.79,23.19,15.81,19.43,22.08,45.19,22.65,69.83,0.2,16.68-2.41,33.26-6,49.47-3.4,14.93-9.25,28.92-16.06,42.6-4.3,8.53-8.81,17.12-14.16,25.08-10.27,14.72-23.11,28.84-38.72,37.64-15.22,8.68-35.43,7.83-42.74-10.08-4.46-10.56-3.21-22.61-2.31-33.85"
