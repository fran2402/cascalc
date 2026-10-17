package com.example.cas.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

enum class Mode(val english: String, val icon: ImageVector) {
    Calculator("Calculator", TableIcons.ModeCalculator),
    Graph2D("2D graphing", TableIcons.Mode2D),
    Graph3D("3D graphing", TableIcons.Mode3D),
    Complex("Complex plotting", TableIcons.ModeComplex),
    Plots("Plots", FigureIcons.Mode);

    /** Its name in the app's language. */
    val label: String get() = tr(english)
}

/**
 * Material 3 Expressive button group, as in the reference design: a
 * floating pill holding round buttons, where the selected one stretches into
 * a wide filled pill with a springy width change.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ModeSwitcher(selected: Mode, onSelect: (Mode) -> Unit, modifier: Modifier = Modifier, onGuide: (Mode) -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    // Buttons shrink to fit narrow phones: the chosen one a little under twice as wide as the others.
    androidx.compose.foundation.layout.BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
    val n = Mode.entries.size
    val room = maxWidth - 12.dp - 6.dp * (n - 1)
    val unit = minOf(48.dp, room / (n + 0.8f))
    Row(
        Modifier
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(colors.surfaceContainerLowest)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Mode.entries.forEach { mode ->
            val on = mode == selected
            val width by animateDpAsState(
                if (on) unit * 1.8f else unit,
                spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
                label = "width",
            )
            val bg by animateColorAsState(if (on) colors.primary else colors.surfaceContainerHigh, label = "bg")
            val fg by animateColorAsState(if (on) colors.onPrimary else colors.onSurface, label = "fg")
            Box(
                Modifier
                    .width(width)
                    .height(unit)
                    .clip(CircleShape)
                    .background(bg)
                    // Hold for the mode's guide: what it does and examples to try.
                    .combinedClickable(
                        onClickLabel = tr("Open {0}", mode.label),
                        onLongClickLabel = tr("Guide to {0}", mode.label),
                        onLongClick = { haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); onGuide(mode) },
                        onClick = { onSelect(mode) },
                    )
                    .semantics {
                        contentDescription = mode.label
                        role = Role.Tab
                        this.selected = on
                    },
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(mode.icon, contentDescription = null, tint = fg, modifier = Modifier.size(minOf(24.dp, unit * 0.55f)))
            }
        }
    }
    }
}
