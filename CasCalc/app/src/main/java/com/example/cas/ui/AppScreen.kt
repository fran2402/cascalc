package com.example.cas.ui

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cas.graph.Camera

/**
 * The whole app: the calculator / 2D graph / 3D graph switcher at the top,
 * with each mode's own action on either side of it.
 */
@Composable
fun AppScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("calculator", Context.MODE_PRIVATE) }
    var mode by remember { mutableStateOf(runCatching { Mode.valueOf(prefs.getString("mode", Mode.Calculator.name)!!) }.getOrDefault(Mode.Calculator)) }
    val calculator: CalculatorViewModel = viewModel()
    val graph2d: Graph2DViewModel = viewModel()
    val graph3d: Graph3DViewModel = viewModel()
    val complex: ComplexViewModel = viewModel()
    val colors = MaterialTheme.colorScheme
    val switchTo = { m: Mode ->
        mode = m
        prefs.edit().putString("mode", m.name).apply()
    }

    // A graph file the app was opened with: imported, opened, and its mode shown.
    OpenGraphFileEffect(mapOf(Mode.Graph2D to graph2d, Mode.Graph3D to graph3d, Mode.Complex to complex), onSwitch = switchTo)

    Column(Modifier.fillMaxSize().background(colors.surface).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                // Graphs have no button here: double-tap a graph to reset its view.
                when (mode) {
                    Mode.Calculator -> CalculatorLeadingAction(calculator)
                    // Saved graphs of every graph mode; opening one switches to its mode.
                    else -> ProjectsButton(mode, mapOf(Mode.Graph2D to graph2d, Mode.Graph3D to graph3d, Mode.Complex to complex), onSwitch = switchTo)
                }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                ModeSwitcher(mode, onSelect = switchTo)
            }
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                // The ⋮ menu (settings, acknowledgements) is in every mode; the calculator's own
                // entries (copy, clear history) come with it there.
                if (mode == Mode.Calculator) CalculatorTrailingAction(calculator) else GraphMenuAction()
            }
        }
        AnimatedContent(targetState = mode, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "mode", modifier = Modifier.weight(1f)) { m ->
            when (m) {
                Mode.Calculator -> CalculatorScreen(calculator, onGraph = { request ->
                    // "Graph this": add the functions and switch to the right graph.
                    when (request.dimensions) {
                        1 -> { complex.show(request.rows); switchTo(Mode.Complex) }
                        2 -> { graph2d.show(request.rows); switchTo(Mode.Graph2D) }
                        else -> { graph3d.show(request.rows); switchTo(Mode.Graph3D) }
                    }
                })
                Mode.Graph2D -> Graph2DScreen(graph2d, onUseValue = { v ->
                    // A tapped point's x or y goes into the calculator.
                    calculator.insertNumber(v)
                    switchTo(Mode.Calculator)
                })
                Mode.Graph3D -> Graph3DScreen(graph3d, onUseValue = { v -> calculator.insertNumber(v); switchTo(Mode.Calculator) })
                Mode.Complex -> ComplexScreen(complex, onUseValue = { v -> calculator.insertComplex(v); switchTo(Mode.Calculator) })
            }
        }
    }
}


/** The ⋮ menu in the graph modes: settings and acknowledgements. */
@Composable
private fun GraphMenuAction() {
    val colors = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    var acknowledgements by remember { mutableStateOf(false) }
    if (settings) AppSettingsPage(onBack = { settings = false }, onAcknowledgements = { settings = false; acknowledgements = true })
    if (acknowledgements) AcknowledgementsDialog(onDismiss = { acknowledgements = false })
    Box {
        IconButton(onClick = { menu = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = colors.onSurfaceVariant)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Settings") }, onClick = { menu = false; settings = true })
            DropdownMenuItem(text = { Text("Acknowledgements") }, onClick = { menu = false; acknowledgements = true })
        }
    }
}
