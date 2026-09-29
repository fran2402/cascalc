package com.example.cas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

/** The top-left button in a graph mode: opens that mode's saved graphs. */
@Composable
fun ProjectsButton(vm: GraphViewModel, title: String) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(Icons.Default.FolderOpen, contentDescription = "Saved graphs", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (open) ProjectsPage(vm, title, onClose = { open = false })
}

/**
 * Saved graphs of one mode, like projects in Desmos: save the graph as it is now under a name,
 * open one (replacing the graph on screen), rename or delete. Each keeps its lines, colors,
 * line styles and sliders.
 */
@Composable
private fun ProjectsPage(vm: GraphViewModel, title: String, onClose: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var naming by remember { mutableStateOf<GraphViewModel.Project?>(null) }
    var saving by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<GraphViewModel.Project?>(null) }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    FullScreenPage(title, onBack = onClose) {
        Button(onClick = { saving = true }, modifier = Modifier.fillMaxWidth(), enabled = vm.functions.any { !it.editor.isEmpty }) {
            Text("Save the current graph")
        }
        if (vm.projects.isEmpty()) {
            Text(
                "Nothing saved yet. Save a graph to keep its lines, colors and sliders, and open it again later.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        vm.projects.toList().forEach { p ->
            val lines = p.data["functions"].orEmpty().lines().filter { it.isNotBlank() }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surfaceContainerHigh)
                    .clickable(onClickLabel = "Open ${p.name}") { vm.openProject(p); onClose() }
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    // The first line, drawn as maths, so the graph is recognisable.
                    lines.firstOrNull()?.let { code ->
                        runCatching { MathCodec.decode(code) }.getOrNull()?.let { row ->
                            MathView(row, 18.sp, colors.onSurfaceVariant)
                        }
                    }
                    Text(
                        "${lines.size} line${if (lines.size == 1) "" else "s"} · saved ${dateFormat.format(Date(p.savedAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { naming = p }) { Icon(Icons.Default.Edit, contentDescription = "Rename ${p.name}", tint = colors.onSurfaceVariant) }
                IconButton(onClick = { deleting = p }) { Icon(Icons.Default.Delete, contentDescription = "Delete ${p.name}", tint = colors.onSurfaceVariant) }
            }
        }
    }

    if (saving) NameDialog("Save graph", "Graph ${vm.projects.size + 1}", onDone = { vm.saveProject(it); saving = false }, onDismiss = { saving = false })
    naming?.let { p -> NameDialog("Rename", p.name, onDone = { vm.renameProject(p, it); naming = null }, onDismiss = { naming = null }) }
    deleting?.let { p ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${p.name}?") },
            text = { Text("The saved graph will be removed. The graph on screen isn't affected.") },
            confirmButton = { TextButton(onClick = { vm.deleteProject(p); deleting = null }) { Text("Delete", color = colors.error) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
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
