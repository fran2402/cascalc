package com.example.cas.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.cas.graph.SessionExport
import com.example.cas.engine.GraphRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Over the history, when it's open full screen: a search pill (questions and answers as
 * typed), chips to list everything, the pinned calculations or one folder (hold a folder to
 * rename or empty it), and Export for what's listed.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryToolbar(vm: CalculatorViewModel, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var exporting by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<String?>(null) }
    val visible = vm.visibleHistory()
    if (exporting) ExportHistoryDialog(visible, onDismiss = { exporting = false })
    editingFolder?.let { name -> FolderEditDialog(name, onRename = { vm.renameFolder(name, it); editingFolder = null }, onEmpty = { vm.removeFolder(name); editingFolder = null }, onDismiss = { editingFolder = null }) }
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).height(52.dp).clip(CircleShape).background(colors.surfaceContainerHigh).padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = colors.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (vm.historyQuery.isEmpty()) Text("Search calculations", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                    BasicTextField(
                        vm.historyQuery, { vm.historyQuery = it }, singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                        cursorBrush = SolidColor(colors.primary),
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search calculations" },
                    )
                }
                if (vm.historyQuery.isNotEmpty()) IconButton(onClick = { vm.historyQuery = "" }) { Icon(Icons.Default.Close, contentDescription = "Clear the search") }
            }
            Spacer(Modifier.width(6.dp))
            // Export what's listed, as a PDF or LaTeX.
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(colors.secondaryContainer)
                    .combinedClickable(onClickLabel = "Export these calculations", enabled = visible.isNotEmpty()) { exporting = true },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.IosShare, contentDescription = "Export", tint = colors.onSecondaryContainer) }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            @Composable
            fun chip(label: String, on: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector?, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
                Row(
                    Modifier.height(36.dp).clip(if (on) CircleShape else RoundedCornerShape(10.dp))
                        .background(if (on) colors.secondaryContainer else colors.surfaceContainer)
                        .combinedClickable(onClickLabel = "Show $label", onLongClickLabel = onLongClick?.let { "Rename or empty $label" }, onLongClick = onLongClick, onClick = onClick)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (icon != null) { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (on) colors.onSecondaryContainer else colors.onSurfaceVariant); Spacer(Modifier.width(6.dp)) }
                    Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) colors.onSecondaryContainer else colors.onSurfaceVariant)
                }
            }
            chip("All", vm.historyFilter == null, null, { vm.historyFilter = null })
            if (vm.history.any { it.pinned }) chip("Pinned", vm.historyFilter == CalculatorViewModel.PINNED, Icons.Default.PushPin, { vm.historyFilter = CalculatorViewModel.PINNED })
            vm.historyFolders.forEach { f -> chip(f, vm.historyFilter == f, Icons.Default.Folder, { vm.historyFilter = f }, onLongClick = { editingFolder = f }) }
        }
        if (vm.historyQuery.isNotBlank() || vm.historyFilter != null) Text(
            if (visible.isEmpty()) "Nothing matches" else "${visible.size} of ${vm.history.size}",
            style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** A folder's name to change, or Empty to take its calculations out of it. */
@Composable
private fun FolderEditDialog(name: String, onRename: (String) -> Unit, onEmpty: () -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Folder, contentDescription = null) },
        title = { Text("Folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(text, { text = it }, singleLine = true, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = onEmpty) { Icon(Icons.Default.FolderOff, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Empty the folder (calculations stay)") }
            }
        },
        confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { onRename(text) }) { Text("Rename") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Choose a calculation's folder: one in use, a new one, or none. */
@Composable
fun MoveToFolderDialog(current: String?, folders: List<String>, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
        title = { Text("Move to folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                folders.forEach { f ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (f == current) colors.secondaryContainer else colors.surfaceContainerHigh)
                            .combinedClickable(onClickLabel = "Move to $f") { onPick(f) }.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Folder, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp))
                        Text(f, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("New folder") }, placeholder = { Text("e.g. Homework 3") }, modifier = Modifier.fillMaxWidth())
                if (current != null) TextButton(onClick = { onPick(null) }) { Icon(Icons.Default.FolderOff, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Take it out of “$current”") }
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onPick(name) }) { Text("Create and move") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * Export the listed calculations: a PDF typeset in Computer Modern (A4, numbered, answers on the
 * right) or a LaTeX document to compile or paste into notes. Save chooses where; Share sends it.
 */
@Composable
private fun ExportHistoryDialog(items: List<HistoryItem>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pdf by remember { mutableStateOf(true) }
    var title by remember { mutableStateOf("Calculations") }
    val stamp = remember { java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(java.util.Date()) }
    val date = remember { java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.LONG, java.text.DateFormat.SHORT).format(java.util.Date()) }
    fun entries() = items.map { i ->
        val shown = if (i.showApprox && i.answer.approx != null) i.answer.approx else i.answer.exact
        SessionExport.Entry(i.expression, shown, approximate = i.answer.isApproximate || i.showApprox, statement = i.answer.isStatement)
    }
    fun bytes(): ByteArray = if (pdf) {
        SceneExport.installMetrics(context)
        SceneExport.pdfPages(context, SessionExport.pages(entries(), title.ifBlank { "Calculations" }, "${items.size} calculation${if (items.size == 1) "" else "s"} · $date · CasCalc"))
    } else SessionExport.latex(entries(), title.ifBlank { "Calculations" }).toByteArray()
    val mime = if (pdf) "application/pdf" else "application/x-tex"
    val fileName = { (title.ifBlank { "calculations" }.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().replace(' ', '-').ifEmpty { "calculations" }) + "-" + stamp + if (pdf) ".pdf" else ".tex" }
    fun failed(e: Throwable) = Toast.makeText(context, "Couldn't export" + (e.message?.let { ": $it" } ?: ""), Toast.LENGTH_LONG).show()
    val savePdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri -> uri?.let { u -> scope.launch { runCatching { val b = withContext(Dispatchers.Default) { bytes() }; withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(u)?.use { it.write(b) } } }.onSuccess { Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show(); onDismiss() }.onFailure(::failed) } } }
    val saveTex = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-tex")) { uri -> uri?.let { u -> scope.launch { runCatching { val b = withContext(Dispatchers.Default) { bytes() }; withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(u)?.use { it.write(b) } } }.onSuccess { Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show(); onDismiss() }.onFailure(::failed) } } }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.IosShare, contentDescription = null) },
        title = { Text("Export ${items.size} calculation${if (items.size == 1) "" else "s"}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(true to "PDF", false to "LaTeX").forEachIndexed { k, (isPdf, label) ->
                        SegmentedButton(selected = pdf == isPdf, onClick = { pdf = isPdf }, shape = SegmentedButtonDefaults.itemShape(k, 2), label = { Text(label) })
                    }
                }
                OutlinedTextField(title, { title = it }, singleLine = true, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                Text(
                    if (pdf) "A4 pages in Computer Modern: each question with its answer on the right, numbered." else "A .tex document (amsmath), one numbered equation per calculation, ready to compile.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = {
                    scope.launch {
                        runCatching {
                            val uri = withContext(Dispatchers.Default) {
                                val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                                val file = File(dir, fileName()).apply { writeBytes(bytes()) }
                                FileProvider.getUriForFile(context, context.packageName + ".files", file)
                            }
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = mime; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share calculations"))
                            onDismiss()
                        }.onFailure(::failed)
                    }
                }) { Text("Share") }
                TextButton(onClick = { if (pdf) savePdf.launch(fileName()) else saveTex.launch(fileName()) }) { Text("Save") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** What kind of calculation a history item is, for its badge: a symbol and a tone. */
private enum class HistoryKind(val symbol: String, val tone: Int) {
    Integral("∫", 0), Derivative("d/dx", 1), Limit("lim", 2), Sum("Σ", 1), Matrix("[ ]", 2), Equation("x =", 0), Number("=", 3);

    companion object {
        fun of(item: HistoryItem): HistoryKind {
            val items = item.expression.items.filter { !(it is com.example.cas.editor.Sym && it.text.isBlank()) }
            val first = items.firstOrNull()
            return when {
                first is com.example.cas.editor.Integral -> Integral
                first is com.example.cas.editor.Derivative -> Derivative
                first is com.example.cas.editor.Func && first.name == "lim" -> Limit
                first is com.example.cas.editor.Func && first.name == "contour" -> Integral
                first is com.example.cas.editor.BigOp -> Sum
                items.any { it is com.example.cas.editor.Matrix } -> Matrix
                items.any { (it as? com.example.cas.editor.Sym)?.text == "=" } -> Equation
                else -> Number
            }
        }
    }
}

/** Which section of the history a time falls in: Today, Yesterday, This week, This month, then the month. */
private fun sectionOf(time: Long, now: java.util.Calendar): String {
    if (time <= 0L) return "Earlier"
    val c = java.util.Calendar.getInstance().apply { timeInMillis = time }
    fun dayIndex(cal: java.util.Calendar) = cal.get(java.util.Calendar.YEAR) * 400 + cal.get(java.util.Calendar.DAY_OF_YEAR)
    val days = dayIndex(now) - dayIndex(c)
    return when {
        days <= 0 -> "Today"
        days == 1 -> "Yesterday"
        days < 7 -> "This week"
        c.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) && c.get(java.util.Calendar.MONTH) == now.get(java.util.Calendar.MONTH) -> "This month"
        else -> java.text.SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(java.util.Date(time))
    }
}

/**
 * The full-screen history, organized: pinned calculations first, then by when they were done
 * (Today, Yesterday, This week…), newest first. Each section is a rounded group of rows (a
 * badge for the kind of calculation, the question small, the answer large); a tap opens the
 * row into its full card with every action, a swipe deletes.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryBrowser(vm: CalculatorViewModel, onGraph: (GraphRequest) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val items = vm.visibleHistory()
    val now = remember(items.size) { java.util.Calendar.getInstance() }
    // Pinned on top (unless the list is already only pinned ones), then sections by time.
    val sections: List<Pair<String, List<HistoryItem>>> = remember(items.toList(), vm.historyFilter, items.map { it.pinned }) {
        val pinnedFirst = vm.historyFilter != CalculatorViewModel.PINNED
        val pinned = if (pinnedFirst) items.filter { it.pinned }.asReversed() else emptyList()
        val rest = (if (pinnedFirst) items.filter { !it.pinned } else items).asReversed()
        listOfNotNull(("Pinned" to pinned).takeIf { pinned.isNotEmpty() }) + rest.groupBy { sectionOf(it.time, now) }.toList()
    }
    if (items.isEmpty()) {
        Column(modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Search, contentDescription = null, tint = colors.outline, modifier = Modifier.size(40.dp))
            Text(if (vm.history.isEmpty()) "Your calculations will appear here" else "Nothing matches", style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
        }
        return
    }
    androidx.compose.foundation.lazy.LazyColumn(modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp)) {
        sections.forEach { (title, list) ->
            stickyHeader(key = "h:$title") {
                Row(
                    Modifier.fillMaxWidth().background(colors.surface).padding(start = 8.dp, end = 8.dp, top = 14.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (title == "Pinned") { Icon(Icons.Default.PushPin, null, tint = colors.primary, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
                    Text(title, style = MaterialTheme.typography.titleMedium, color = colors.primary, modifier = Modifier.weight(1f))
                    Text("${list.size}", style = MaterialTheme.typography.labelLarge, color = colors.onSecondaryContainer,
                        modifier = Modifier.clip(CircleShape).background(colors.secondaryContainer).padding(horizontal = 10.dp, vertical = 2.dp))
                }
            }
            list.forEachIndexed { k, item ->
                item(key = "i:" + System.identityHashCode(item) + ":" + title) {
                    val first = k == 0; val last = k == list.lastIndex
                    val big = 24.dp; val small = 6.dp
                    val shape = RoundedCornerShape(topStart = if (first) big else small, topEnd = if (first) big else small, bottomStart = if (last) big else small, bottomEnd = if (last) big else small)
                    var confirm by remember { mutableStateOf(false) }
                    if (confirm) AlertDialog(
                        onDismissRequest = { confirm = false },
                        title = { Text("Delete this calculation?") },
                        text = { Text("It will be removed from the history. This can't be undone.") },
                        confirmButton = { TextButton(onClick = { confirm = false; vm.deleteHistory(item) }) { Text("Delete") } },
                        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
                    )
                    Box(Modifier.animateItem().padding(bottom = 3.dp)) {
                        SwipeToDelete(onDelete = { if (AppSettings.confirmDeleteEntry) confirm = true else vm.deleteHistory(item) }) {
                            if (vm.focused === item) Box(Modifier.padding(vertical = 4.dp)) { HistoryCard(item, vm, onGraph) }
                            else HistoryRow(item, shape, hideFolder = vm.historyFilter) { vm.focused = item }
                        }
                    }
                }
            }
        }
    }
}

/** One calculation as a compact row: its kind's badge, the question small, the answer large, pin and folder. */
@Composable
private fun HistoryRow(item: HistoryItem, shape: androidx.compose.ui.graphics.Shape, hideFolder: String? = null, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val kind = remember(item) { HistoryKind.of(item) }
    val (bg, fg) = when (kind.tone) {
        0 -> colors.primaryContainer to colors.onPrimaryContainer
        1 -> colors.secondaryContainer to colors.onSecondaryContainer
        2 -> colors.tertiaryContainer to colors.onTertiaryContainer
        else -> colors.surfaceContainerHighest to colors.onSurfaceVariant
    }
    val shown = if (item.showApprox && item.answer.approx != null) item.answer.approx else item.answer.exact
    Row(
        Modifier.fillMaxWidth().clip(shape).background(colors.surfaceContainer)
            .combinedClickable(onClickLabel = "Open this calculation") { onOpen() }
            .padding(start = 12.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(bg), contentAlignment = Alignment.Center) {
            Text(kind.symbol, style = androidx.compose.ui.text.TextStyle(fontFamily = com.example.cas.ui.theme.CasFonts.CmItalic, fontSize = if (kind.symbol.length > 2) 13.sp else 20.sp), color = fg)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                MathView(item.expression, 15.sp, colors.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (item.answer.isStatement) "" else if (item.answer.isApproximate || item.showApprox) "≈ " else "= ",
                    style = androidx.compose.ui.text.TextStyle(fontFamily = com.example.cas.ui.theme.CasFonts.CmRoman, fontSize = 19.sp), color = colors.primary)
                Box(Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState())) { MathView(shown, 19.sp, colors.onSurface) }
            }
        }
        // The folder's tag is left off when that folder is what's listed.
        val folder = item.folder?.takeIf { it != hideFolder }
        if (item.pinned || folder != null) Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(start = 8.dp)) {
            if (item.pinned) Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = colors.primary, modifier = Modifier.size(16.dp))
            folder?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = colors.onSecondaryContainer, maxLines = 1, modifier = Modifier.widthIn(max = 96.dp).clip(CircleShape).background(colors.secondaryContainer).padding(horizontal = 8.dp, vertical = 2.dp)) }
        }
    }
}
