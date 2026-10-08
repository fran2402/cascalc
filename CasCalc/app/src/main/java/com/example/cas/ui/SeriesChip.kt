package com.example.cas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cas.cas.Convergence
import com.example.cas.cas.Expr
import com.example.cas.cas.Fn
import com.example.cas.cas.INF
import com.example.cas.cas.Sym
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** For an answer that is a Σ to ∞ with no closed form: whether it converges and by which test (worked out off the main thread). */
@Composable
fun rememberSeriesVerdict(value: Expr): Convergence.Verdict? {
    val held = (if (value is Fn && value.name == "diverges") value.args.firstOrNull() else value) as? Fn
    val series = held?.takeIf { it.name == "sum" && it.args.size == 4 && it.args[3] == INF && it.args[1] is Sym }
    val verdict by produceState<Convergence.Verdict?>(null, series) {
        this.value = series?.let { s -> withContext(Dispatchers.Default) { Convergence.of(s.args[0], s.args[1] as Sym, s.args[2]) } }
    }
    return verdict
}

/** "Converges · ratio test" in green, "Diverges · integral test" in red. */
@Composable
fun SeriesChip(v: Convergence.Verdict) {
    val dark = MaterialTheme.colorScheme.background.let { (it.red + it.green + it.blue) / 3 < 0.5f }
    val (bg, fg) = when {
        v.converges && dark -> Color(0xFF2A4A1C) to Color(0xFFCDEAC0)
        v.converges -> Color(0xFFCDEAC0) to Color(0xFF163A06)
        else -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Text(
        (if (v.converges) tr("Converges") else tr("Diverges")) + " · " + tr(v.test.label),
        style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1,
        modifier = Modifier.clip(CircleShape).background(bg).padding(horizontal = 12.dp, vertical = 5.dp),
    )
}
