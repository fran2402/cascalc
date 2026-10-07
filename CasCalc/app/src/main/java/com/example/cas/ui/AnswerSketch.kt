package com.example.cas.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.cas.cas.Expr
import com.example.cas.graph.Sketch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The faded sketch of an answer's curve, drawn behind the left of the answer line: the curve in
 * [color] with a light wash under it, fading out to the right (before the answer) and at the top
 * and bottom. Worked out off the main thread; nothing is drawn when the answer isn't a curve.
 */
/** The sketch's curve for [expr], worked out off the main thread (null until then, or when it isn't a curve). */
@Composable
fun rememberSketch(expr: Expr, enabled: Boolean): Sketch.Curve? {
    val curve by produceState<Sketch.Curve?>(null, expr, enabled) {
        value = if (!enabled) null else withContext(Dispatchers.Default) { runCatching { Sketch.of(expr) }.getOrNull() }
    }
    return curve
}

@Composable
fun AnswerSketch(c: Sketch.Curve, color: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    // It eases in once worked out, rather than popping in.
    val alpha = remember(c) { Animatable(0f) }
    LaunchedEffect(c) { alpha.animateTo(1f, tween(450)) }
    val tappable = if (onClick != null) Modifier.clickable(onClickLabel = tr("Graph this"), onClick = onClick) else Modifier
    Canvas(modifier.then(tappable).graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen; this.alpha = alpha.value }) {
        val w = size.width; val h = size.height
        // Inset so the stroke isn't cut at the top and bottom.
        val pad = 3.dp.toPx()
        fun px(k: Int) = (c.xs[k] * w).toFloat()
        fun py(y: Double) = (pad + (1 - y) * (h - 2 * pad)).toFloat()
        val line = Path(); val wash = Path()
        val base = py(c.base)
        var open = false; var runStart = 0f
        fun closeRun(endX: Float) { if (open) { wash.lineTo(endX, base); wash.lineTo(runStart, base); wash.close() } }
        var lastX = 0f
        for (k in c.ys.indices) {
            val y = c.ys[k]
            if (!y.isFinite()) { closeRun(lastX); open = false; continue }
            val x = px(k); val yy = py(y)
            if (!open) { line.moveTo(x, yy); wash.moveTo(x, base); wash.lineTo(x, yy); runStart = x; open = true }
            else { line.lineTo(x, yy); wash.lineTo(x, yy) }
            lastX = x
        }
        closeRun(lastX)
        drawPath(wash, Brush.verticalGradient(listOf(color.copy(alpha = 0.16f), color.copy(alpha = 0f))))
        drawPath(line, color.copy(alpha = 0.85f), style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        // The fades: solid on the left, gone before the answer; soft at the top and bottom.
        drawRect(Brush.horizontalGradient(0f to Color.Black, 0.3f to Color.Black, 0.58f to Color.Black.copy(alpha = 0.55f), 0.92f to Color.Transparent), blendMode = BlendMode.DstIn)
        drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.16f to Color.Black, 0.84f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
    }
}
