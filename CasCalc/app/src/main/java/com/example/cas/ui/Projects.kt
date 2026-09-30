package com.example.cas.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cas.editor.MathCodec
import com.example.cas.editor.MathRow
import com.example.cas.editor.Sym
import com.example.cas.graph.ProjectSummary
import java.text.DateFormat
import java.util.Calendar
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

/** A saved graph with the mode it belongs to. */
private class Saved(val mode: Mode, val vm: GraphViewModel, val project: GraphViewModel.Project)

/** How the list is ordered. */
private enum class Order(val label: String) { Newest("Newest first"), Oldest("Oldest first"), Name("Name, A–Z") }

/**
 * Saved graphs, like projects in Desmos. All graph modes are listed together, grouped by when
 * they were saved (Today, Yesterday, This week, Earlier); a connected button group narrows the
 * list to one mode, with the counts in brackets; search by name and change the order at the
 * top. Each card shows the mode's icon in its corner, the first lines with their colours (data
 * sets as a chip, never drawn point by point) and a menu to rename, duplicate or delete. The
 * button at the bottom saves the graph on screen. Cards fill the width on a phone and form a grid
 * on a tablet.
 */
@Composable
private fun ProjectsPage(current: Mode, graphs: Map<Mode, GraphViewModel>, onSwitch: (Mode) -> Unit, onClose: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val vm = graphs.getValue(current)
    var naming by remember { mutableStateOf<Saved?>(null) }
    var saving by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Saved?>(null) }
    var filter by remember { mutableStateOf<Mode?>(null) }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var order by remember { mutableStateOf(Order.Newest) }
    var orderMenu by remember { mutableStateOf(false) }
    val tablet = isTabletLayout()
    val context = androidx.compose.ui.platform.LocalContext.current
    val writeFile = rememberGraphFileWriter()
    // A graph file from elsewhere joins the saved graphs of its mode, at the top.
    val importFile = rememberGraphFileReader { c ->
        val target = graphs[c.kind.mode]
        if (target == null) {
            android.widget.Toast.makeText(context, "Can't open ${c.kind.label.lowercase()}s here", android.widget.Toast.LENGTH_LONG).show()
        } else {
            val p = target.importProject(c.name, c.data)
            filter = null; query = ""; searching = false; order = Order.Newest
            android.widget.Toast.makeText(context, "Imported “${p.name}” (${c.kind.mode.label})", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    val modes = graphs.keys.toList()
    val all = graphs.flatMap { (m, g) -> g.projects.map { Saved(m, g, it) } }
    val shown = all
        .filter { filter == null || it.mode == filter }
        .filter { query.isBlank() || it.project.name.contains(query.trim(), ignoreCase = true) }
        .let { list ->
            when (order) {
                Order.Newest -> list.sortedByDescending { it.project.savedAt }
                Order.Oldest -> list.sortedBy { it.project.savedAt }
                Order.Name -> list.sortedBy { it.project.name.lowercase() }
            }
        }
    // By date when ordered by date; one group when by name.
    val groups = if (order == Order.Name) listOf("" to shown) else shown.groupBy { whenSaved(it.project.savedAt) }.toList()

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler(onBack = onClose)
        Surface(Modifier.fillMaxSize(), color = colors.surface) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                Column(Modifier.fillMaxSize()) {
                    // The top bar: back, the title (or the search field), search and order.
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.onSurface) }
                        if (searching) {
                            TextField(
                                value = query, onValueChange = { query = it }, singleLine = true,
                                placeholder = { Text("Search by name") },
                                shape = CircleShape,
                                colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                            )
                            IconButton(onClick = { searching = false; query = "" }) { Icon(Icons.Default.Close, contentDescription = "Close the search", tint = colors.onSurfaceVariant) }
                        } else {
                            Text("Saved graphs", style = MaterialTheme.typography.headlineSmall, color = colors.onSurface, modifier = Modifier.weight(1f).padding(start = 4.dp))
                            IconButton(onClick = { searching = true }) { Icon(Icons.Default.Search, contentDescription = "Search", tint = colors.onSurfaceVariant) }
                            IconButton(onClick = importFile) { Icon(Icons.Default.FileOpen, contentDescription = "Import a graph file (.g2d, .g3d, .gcp)", tint = colors.onSurfaceVariant) }
                        }
                        Box {
                            TextButton(onClick = { orderMenu = true }) { Text(order.label) }
                            DropdownMenu(expanded = orderMenu, onDismissRequest = { orderMenu = false }) {
                                Order.entries.forEach { o -> DropdownMenuItem(text = { Text(o.label) }, onClick = { order = o; orderMenu = false }) }
                            }
                        }
                    }
                    // Which modes: all, or one; counts in brackets.
                    Box(Modifier.padding(horizontal = 16.dp).widthIn(max = 720.dp)) {
                        ConnectedButtonGroup(
                            modes.size + 1,
                            selected = if (filter == null) 0 else modes.indexOf(filter) + 1,
                            onSelect = { k -> filter = if (k == 0) null else modes[k - 1] },
                            description = { k -> if (k == 0) "All saved graphs, ${all.size}" else "${modes[k - 1].label}, ${all.count { it.mode == modes[k - 1] }}" },
                        ) { k, on ->
                            val fg = if (on) colors.onPrimary else colors.onSurface
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (k == 0) Text("All (${all.size})", color = fg, style = MaterialTheme.typography.labelLarge)
                                else {
                                    val m = modes[k - 1]
                                    Icon(m.icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                                    Text(
                                        (if (tablet) m.label + " " else "") + "(${all.count { it.mode == m }})",
                                        color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                    if (shown.isEmpty()) {
                        EmptyProjects(searching = query.isNotBlank(), modifier = Modifier.weight(1f).fillMaxWidth())
                    } else {
                        LazyVerticalGrid(
                            columns = if (tablet) GridCells.Adaptive(300.dp) else GridCells.Fixed(1),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        ) {
                            groups.forEach { (label, list) ->
                                if (label.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                                    Text(label, style = MaterialTheme.typography.titleSmall, color = colors.primary, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
                                }
                                items(list, key = { it.mode.name + it.project.id }, contentType = { "card" }) { s ->
                                    ProjectCard(
                                        s, current = s.mode == current,
                                        onOpen = { s.vm.openProject(s.project); if (s.mode != current) onSwitch(s.mode); onClose() },
                                        onRename = { naming = s },
                                        onDuplicate = { s.vm.duplicateProject(s.project) },
                                        onExportFile = { share -> s.mode.graphFileKind?.let { k -> writeFile(com.example.cas.graph.GraphFile.Contents(k, s.project.name, s.project.data), share) } },
                                        onDelete = { deleting = s },
                                    )
                                }
                            }
                        }
                    }
                }
                // Saving the graph on screen.
                ExtendedFloatingActionButton(
                    onClick = { saving = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Save this ${if (current == Mode.Complex) "plot" else "graph"}") },
                    expanded = true,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                    containerColor = if (vm.functions.any { !it.editor.isEmpty }) colors.primaryContainer else colors.surfaceContainerHighest,
                    contentColor = if (vm.functions.any { !it.editor.isEmpty }) colors.onPrimaryContainer else colors.onSurfaceVariant,
                )
            }
        }
    }

    if (saving) {
        if (vm.functions.all { it.editor.isEmpty }) {
            AlertDialog(
                onDismissRequest = { saving = false },
                title = { Text("Nothing to save") },
                text = { Text("The ${current.label.lowercase()} on screen has no lines yet.") },
                confirmButton = { TextButton(onClick = { saving = false }) { Text("OK") } },
            )
        } else NameDialog("Save graph", "Graph ${vm.projects.size + 1}", onDone = { vm.saveProject(it); saving = false }, onDismiss = { saving = false })
    }
    naming?.let { s -> NameDialog("Rename", s.project.name, onDone = { s.vm.renameProject(s.project, it); naming = null }, onDismiss = { naming = null }) }
    deleting?.let { s ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${s.project.name}?") },
            text = { Text("The saved graph will be removed. The graph on screen isn't affected.") },
            confirmButton = { TextButton(onClick = { s.vm.deleteProject(s.project); deleting = null }) { Text("Delete", color = colors.error) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

/** "Today", "Yesterday", "This week" or "Earlier". */
private fun whenSaved(time: Long): String {
    val day = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    val dayMs = 24L * 60 * 60 * 1000
    return when {
        time >= day -> "Today"
        time >= day - dayMs -> "Yesterday"
        time >= day - 6 * dayMs -> "This week"
        else -> "Earlier"
    }
}

@Composable
private fun EmptyProjects(searching: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
        Box(Modifier.size(96.dp).clip(RoundedCornerShape(32.dp)).background(colors.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(if (searching) Icons.Default.Search else Icons.Default.FolderOpen, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(44.dp))
        }
        Text(if (searching) "No saved graphs with that name" else "No saved graphs yet", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
        if (!searching) Text(
            "Save a graph to keep its lines, colours and sliders, and open it again later.",
            style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

/**
 * A saved graph: its mode's icon top right, its name and when it was saved, then up to three
 * lines in their colours (drawn as maths when short; data sets as a chip) and "+ n more".
 */
@Composable
private fun ProjectCard(s: Saved, current: Boolean, onOpen: () -> Unit, onRename: () -> Unit, onDuplicate: () -> Unit, onExportFile: (Boolean) -> Unit, onDelete: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val p = s.project
    val noteCode = remember { MathCodec.encode(MathRow(mutableListOf(Sym("…")))) }
    val summary = remember(p) { ProjectSummary.of(p.data, noteCode) }
    val palette = plotColors(colors)
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    var menu by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.surfaceContainer)
            .then(if (current) Modifier.border(1.dp, colors.outlineVariant, RoundedCornerShape(28.dp)) else Modifier)
            .clickable(onClickLabel = "Open ${p.name}") { onOpen() }
            .padding(start = 18.dp, top = 14.dp, end = 8.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${summary.lines.size} line${if (summary.lines.size == 1) "" else "s"} · ${dateFormat.format(Date(p.savedAt))}",
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1,
                )
            }
            // The mode it belongs to.
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(colors.secondaryContainer).semantics { contentDescription = s.mode.label },
                contentAlignment = Alignment.Center,
            ) { Icon(s.mode.icon, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(22.dp)) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Options for ${p.name}", tint = colors.onSurfaceVariant) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text("Duplicate") }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) }, onClick = { menu = false; onDuplicate() })
                    val ext = s.mode.graphFileKind?.extension.orEmpty()
                    DropdownMenuItem(text = { Text("Share as .$ext file") }, leadingIcon = { Icon(Icons.Default.Share, null) }, onClick = { menu = false; onExportFile(true) })
                    DropdownMenuItem(text = { Text("Save as .$ext file") }, leadingIcon = { Icon(Icons.Default.SaveAlt, null) }, onClick = { menu = false; onExportFile(false) })
                    DropdownMenuItem(text = { Text("Delete", color = colors.error) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = colors.error) }, onClick = { menu = false; onDelete() })
                }
            }
        }
        // The lines, as on the graph's list: a colour dot, then the maths.
        Column(
            Modifier.padding(end = 10.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surfaceContainerLowest).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (summary.lines.isEmpty()) Text("No lines", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            summary.lines.take(3).forEach { line ->
                val dot = line.color?.let { Color(it) } ?: palette[line.slot.mod(palette.size)]
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
                    val row = line.code?.let { code -> remember(code) { runCatching { MathCodec.decode(code) }.getOrNull() } }
                    if (row == null) {
                        // A data set (or a very long line): named, not drawn.
                        Row(
                            Modifier.clip(CircleShape).background(colors.tertiaryContainer).padding(horizontal = 10.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (line.isData) Icon(Icons.Default.TableChart, contentDescription = null, tint = colors.onTertiaryContainer, modifier = Modifier.size(14.dp))
                            Text(if (line.isData) "Data set" else "Long formula", style = MaterialTheme.typography.labelMedium, color = colors.onTertiaryContainer)
                        }
                    } else {
                        // One line, cut off at the card's edge.
                        Box(Modifier.weight(1f).clipToBounds()) {
                            Box(Modifier.wrapContentWidth(Alignment.Start, unbounded = true)) { MathView(row, 17.sp, colors.onSurface) }
                        }
                    }
                }
            }
            if (summary.lines.size > 3) Text("+ ${summary.lines.size - 3} more", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 20.dp))
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
