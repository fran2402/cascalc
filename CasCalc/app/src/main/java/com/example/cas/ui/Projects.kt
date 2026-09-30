package com.example.cas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cas.editor.MathCodec
import java.text.DateFormat
import java.util.Date

/**
 * The top-left button in a graph mode: opens the saved graphs, those of every graph mode
 * ([graphs]). Opening one from another mode switches to that mode ([onSwitch]).
 */
@Composable
fun ProjectsButton(current: Mode, graphs: Map<Mode, GraphViewModel>, onSwitch: (Mode) -> Unit) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(Icons.Default.FolderOpen, contentDescription = "Saved graphs", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (open) ProjectsPage(current, graphs, onSwitch, onClose = { open = false })
}

/**
 * Saved graphs, like projects in Desmos: save the graph as it is now under a name, open one
 * (replacing the graph in its mode), rename or delete. Each keeps its lines, colors, line
 * styles and sliders. All modes are listed, newest first, each card marked with its mode's icon
 * in the corner; chips narrow the list to one mode. Cards are a grid on a tablet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectsPage(current: Mode, graphs: Map<Mode, GraphViewModel>, onSwitch: (Mode) -> Unit, onClose: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val vm = graphs.getValue(current)
    var naming by remember { mutableStateOf<Pair<GraphViewModel, GraphViewModel.Project>?>(null) }
    var saving by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Pair<GraphViewModel, GraphViewModel.Project>?>(null) }
    var filter by remember { mutableStateOf<Mode?>(null) }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    val tablet = isTabletLayout()
    val all = graphs.flatMap { (m, g) -> g.projects.map { Triple(m, g, it) } }.sortedByDescending { it.third.savedAt }
    val shown = all.filter { filter == null || it.first == filter }

    FullScreenPage("Saved graphs", onBack = onClose) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { saving = true }, enabled = vm.functions.any { !it.editor.isEmpty }) {
                Icon(current.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save the current graph")
            }
        }
        // Which modes to show.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("All  ${all.size}") })
            graphs.keys.forEach { m ->
                val n = all.count { it.first == m }
                FilterChip(
                    selected = filter == m, onClick = { filter = if (filter == m) null else m },
                    label = { Text("${m.label}  $n") },
                    leadingIcon = { Icon(m.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
        }
        if (shown.isEmpty()) {
            Text(
                "Nothing saved yet. Save a graph to keep its lines, colors and sliders, and open it again later.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        val perRow = if (tablet) 3 else 1
        shown.chunked(perRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (m, g, p) ->
                    ProjectCard(
                        p, m, dateFormat, tablet,
                        onOpen = { g.openProject(p); if (m != current) onSwitch(m); onClose() },
                        onRename = { naming = g to p },
                        onDelete = { deleting = g to p },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }

    if (saving) NameDialog("Save graph", "Graph ${vm.projects.size + 1}", onDone = { vm.saveProject(it); saving = false }, onDismiss = { saving = false })
    naming?.let { (g, p) -> NameDialog("Rename", p.name, onDone = { g.renameProject(p, it); naming = null }, onDismiss = { naming = null }) }
    deleting?.let { (g, p) ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${p.name}?") },
            text = { Text("The saved graph will be removed. The graph on screen isn't affected.") },
            confirmButton = { TextButton(onClick = { g.deleteProject(p); deleting = null }) { Text("Delete", color = colors.error) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

/** A saved graph: its name, its first lines drawn as maths, when it was saved, and its mode's icon top right. */
@Composable
private fun ProjectCard(
    p: GraphViewModel.Project,
    mode: Mode,
    dateFormat: DateFormat,
    tablet: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val lines = p.data["functions"].orEmpty().lines().filter { it.isNotBlank() }
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceContainerHigh)
            .clickable(onClickLabel = "Open ${p.name}") { onOpen() },
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, end = 8.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(p.name, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, modifier = Modifier.padding(end = 44.dp))
            // The first lines, drawn as maths, so the graph is recognisable.
            Column(Modifier.clip(RoundedCornerShape(12.dp)).fillMaxWidth().background(colors.surfaceContainerLowest).padding(horizontal = 10.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                lines.take(if (tablet) 3 else 2).forEach { code ->
                    runCatching { MathCodec.decode(code) }.getOrNull()?.let { row -> MathView(row, 17.sp, colors.onSurfaceVariant) }
                }
                if (lines.isEmpty()) Text("No lines", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${lines.size} line${if (lines.size == 1) "" else "s"} · ${dateFormat.format(Date(p.savedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRename) { Icon(Icons.Default.Edit, contentDescription = "Rename ${p.name}", tint = colors.onSurfaceVariant) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete ${p.name}", tint = colors.onSurfaceVariant) }
            }
        }
        // The mode it belongs to.
        Box(
            Modifier.align(Alignment.TopEnd).padding(10.dp).size(34.dp).clip(CircleShape).background(colors.secondaryContainer)
                .semantics { contentDescription = mode.label },
            contentAlignment = Alignment.Center,
        ) {
            Icon(mode.icon, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun NameDialog(title: String, initial: String, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Name") }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { onDone(name) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
