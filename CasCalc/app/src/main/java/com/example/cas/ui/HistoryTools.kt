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
import androidx.core.content.FileProvider
import com.example.cas.graph.SessionExport
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
