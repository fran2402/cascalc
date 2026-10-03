package com.example.cas.ui

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.cas.graph.GraphFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/** The kind of graph file each graph mode writes and reads. */
val Mode.graphFileKind: GraphFile.Kind?
    get() = when (this) {
        Mode.Graph2D -> GraphFile.Kind.Graph2D
        Mode.Graph3D -> GraphFile.Kind.Graph3D
        Mode.Complex -> GraphFile.Kind.Complex
        Mode.Calculator -> null
    }

val GraphFile.Kind.mode: Mode
    get() = when (this) {
        GraphFile.Kind.Graph2D -> Mode.Graph2D
        GraphFile.Kind.Graph3D -> Mode.Graph3D
        GraphFile.Kind.Complex -> Mode.Complex
    }

/**
 * Writing graph files (.g2d, .g3d, .gcp): call the result with the file's contents and whether to
 * share it (the share sheet) or save it (the system's save dialog). Names get the date and time,
 * as picture exports do: "Pendulum-2026-09-30_14-05-12.g2d".
 */
@Composable
fun rememberGraphFileWriter(): (GraphFile.Contents, Boolean) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<GraphFile.Contents?>(null) }
    fun failed(e: Throwable) = Toast.makeText(context, "Couldn't save the graph file" + (e.message?.let { ": $it" } ?: ""), Toast.LENGTH_LONG).show()
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val c = pending
        pending = null
        if (uri == null || c == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(GraphFile.write(c)) } ?: error("the file couldn't be opened") }
            }.onSuccess { Toast.makeText(context, "Graph file saved", Toast.LENGTH_SHORT).show() }.onFailure(::failed)
        }
    }
    fun fileName(c: GraphFile.Contents): String {
        val safe = c.name.replace(Regex("[\\\\/:*?\"<>|\\n\\t]"), "").trim().ifEmpty { "graph" }
        return safe + "-" + java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(java.util.Date()) + "." + c.kind.extension
    }
    return { c, share ->
        if (share) {
            scope.launch {
                runCatching {
                    val uri = withContext(Dispatchers.IO) {
                        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                        val file = File(dir, fileName(c))
                        file.writeBytes(GraphFile.write(c))
                        FileProvider.getUriForFile(context, context.packageName + ".files", file)
                    }
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "application/octet-stream"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(send, "Share graph file"))
                }.onFailure(::failed)
            }
        } else {
            pending = c
            save.launch(fileName(c))
        }
    }
}

/**
 * Opening a graph file: the system's file picker, then [onRead] with what's in it (on the main
 * thread). Files that aren't graph files are refused with the reason.
 */
@Composable
fun rememberGraphFileReader(onRead: (GraphFile.Contents) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                        if (c.moveToFirst()) c.getString(0) else null
                    }
                    val size = context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                        if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
                    }
                    // Graph files are text; anything very large isn't one.
                    require(size == null || size < 64L * 1024 * 1024) { "The file is too large to be a graph file" }
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("the file couldn't be opened")
                    GraphFile.read(bytes, name)
                }
            }.onSuccess(onRead).onFailure { e ->
                Toast.makeText(context, e.message ?: "Couldn't open the graph file", Toast.LENGTH_LONG).show()
            }
        }
    }
    return { open.launch(arrayOf("*/*")) }
}

/** A graph file the app was asked to open (from a file manager), waiting for [OpenGraphFileEffect]. */
object OpenedGraphFile {
    var uri by mutableStateOf<Uri?>(null)
}

/**
 * A graph file the app was opened with (Open with, or shared to it): read, then plotted in its
 * mode, replacing the graph there. If that graph has lines, it asks first; either way the file
 * is also kept in the saved graphs.
 */
@Composable
fun OpenGraphFileEffect(graphs0: Map<Mode, GraphViewModel>, onSwitch0: (Mode) -> Unit) {
    val context = LocalContext.current
    // The latest of each, for the long-running effect below.
    val graphs by androidx.compose.runtime.rememberUpdatedState(graphs0)
    val onSwitch by androidx.compose.runtime.rememberUpdatedState(onSwitch0)
    var pending by remember { mutableStateOf<GraphFile.Contents?>(null) }
    fun plot(c: GraphFile.Contents) {
        val vm = graphs[c.kind.mode] ?: return
        vm.openProject(vm.importProject(c.name, c.data))
        onSwitch(c.kind.mode)
        Toast.makeText(context, "Opened “${c.name}”", Toast.LENGTH_SHORT).show()
    }
    pending?.let { c ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pending = null },
            title = { androidx.compose.material3.Text("Open “${c.name}”?") },
            text = { androidx.compose.material3.Text("It replaces what's in the ${c.kind.label} now. The file is kept in your saved graphs, but the current lines are lost unless you've saved them.") },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { pending = null; plot(c) }) { androidx.compose.material3.Text("Open and replace") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { pending = null }) { androidx.compose.material3.Text("Cancel") } },
        )
    }
    // Watched for the life of the screen: the link is taken (and cleared) and the file read in
    // the same coroutine. (Keying an effect on the link and clearing it ended that effect before
    // the file was read: "The coroutine scope left the composition".)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        androidx.compose.runtime.snapshotFlow { OpenedGraphFile.uri }.collect { uri ->
            if (uri == null) return@collect
            OpenedGraphFile.uri = null
            val read = try {
                Result.success(withContext(Dispatchers.IO) {
                    val name = runCatching {
                        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
                    }.getOrNull() ?: uri.lastPathSegment
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("the file couldn't be opened")
                    GraphFile.read(bytes, name)
                })
            } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { Result.failure(e) }
            read.onSuccess { c ->
                val vm = graphs[c.kind.mode] ?: return@onSuccess
                // Ask before replacing lines that are there; an empty graph is simply filled.
                if (vm.hasContent()) { onSwitch(c.kind.mode); pending = c } else plot(c)
            }.onFailure { e -> Toast.makeText(context, e.message ?: "Couldn't open the graph file", Toast.LENGTH_LONG).show() }
        }
    }
}
