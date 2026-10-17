package com.example.cas.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.cas.editor.Editor
import com.example.cas.editor.MathCodec
import com.example.cas.editor.MathRow
import com.example.cas.editor.Matrix
import com.example.cas.engine.GraphRequest
import com.example.cas.engine.Notebook
import com.example.cas.engine.Notebook.Block
import com.example.cas.ui.theme.CasFonts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Whether the notebooks page is open: from the ⋮ menu in any mode. */
object NotebookNav { var open by mutableStateOf(false) }

/** Notebooks on the phone: one .casnb file each in the app's own files. */
object NotebookStore {
    private fun dir(c: Context) = File(c.filesDir, "notebooks").apply { mkdirs() }

    fun list(c: Context): List<Notebook.Doc> = dir(c).listFiles { f -> f.name.endsWith(".casnb") }.orEmpty()
        .mapNotNull { f -> runCatching { Notebook.decode(f.nameWithoutExtension, f.readText()) }.getOrNull() }
        .sortedByDescending { it.edited }

    fun save(c: Context, d: Notebook.Doc) { File(dir(c), d.id + ".casnb").writeText(Notebook.encode(d)) }
    fun delete(c: Context, id: String) { File(dir(c), "$id.casnb").delete() }
    fun newId(): String = java.lang.Long.toString(System.currentTimeMillis(), 36) + (100..999).random()

    /** A copy of [d] under a new name and id, saved. */
    fun duplicate(c: Context, d: Notebook.Doc): Notebook.Doc =
        Notebook.Doc(newId(), d.title + " (copy)", d.blocks, System.currentTimeMillis()).also { save(c, it) }
}

/** The notebooks: search, the list (newest first), and new ones blank or from the history. */
@Composable
fun NotebooksPage(calculator: CalculatorViewModel, onBack: () -> Unit, onGraph: (GraphRequest) -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val page = rememberPageMotion(onBack)
    var docs by remember { mutableStateOf(NotebookStore.list(context)) }
    var open by remember { mutableStateOf<Notebook.Doc?>(null) }
    var query by remember { mutableStateOf("") }
    var fromHistory by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Notebook.Doc?>(null) }
    var deleting by remember { mutableStateOf<Notebook.Doc?>(null) }
    fun refresh() { docs = NotebookStore.list(context) }
    // A notebook file shared from another phone: read, given a new id, saved and opened.
    val importFile = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val text = context.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
            val d = Notebook.decode(NotebookStore.newId(), text)
            Notebook.Doc(d.id, d.title, d.blocks, System.currentTimeMillis()).also { NotebookStore.save(context, it) }
        }.onSuccess { refresh(); open = it }
            .onFailure { Toast.makeText(context, tr("That isn't a notebook file"), Toast.LENGTH_LONG).show() }
    }
    fun create(title: String, blocks: List<Block>) {
        val d = Notebook.Doc(NotebookStore.newId(), title, blocks, System.currentTimeMillis())
        NotebookStore.save(context, d); refresh(); open = d
    }

    open?.let { d -> NotebookEditor(d, onClose = { open = null; refresh() }, onGraph = { r -> open = null; refresh(); page.close(); onGraph(r) }) }
    if (fromHistory) FromHistoryDialog(calculator, onCreate = { rows ->
        fromHistory = false
        create(tr("From history"), listOf(Block.Text(1, "# " + tr("From history"))) + rows.mapIndexed { k, r -> Block.Math(k + 2L, r) })
    }, onDismiss = { fromHistory = false })
    renaming?.let { d ->
        var name by remember(d) { mutableStateOf(d.title) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(tr("Rename notebook")) },
            text = { OutlinedTextField(name, { name = it }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { NotebookStore.save(context, Notebook.Doc(d.id, name.trim(), d.blocks, System.currentTimeMillis())); renaming = null; refresh() }) { Text(tr("Rename")) } },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(tr("Cancel")) } },
        )
    }
    deleting?.let { d ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(tr("Delete this notebook?")) },
            text = { Text(tr("It can't be brought back.")) },
            confirmButton = { TextButton(onClick = { NotebookStore.delete(context, d.id); deleting = null; refresh() }) { Text(tr("Delete")) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(tr("Cancel")) } },
        )
    }

    Dialog(onDismissRequest = page.close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler(onBack = page.close)
        page.Content {
            Surface(Modifier.fillMaxSize(), color = colors.surface) {
                Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).clip(CircleShape).background(colors.surfaceContainerHigh).clickable(onClick = page.close), contentAlignment = Alignment.Center) {
                                AppIcon(TableIcons.Back, contentDescription = tr("Back"), tint = colors.onSurface)
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(tr("Notebooks"), style = MaterialTheme.typography.headlineMedium, color = colors.onSurface)
                        }
                        // Search: titles and what's written inside.
                        Row(
                            Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(colors.surfaceContainerHigh).padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIcon(TableIcons.Search, contentDescription = null, tint = colors.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Box(Modifier.weight(1f)) {
                                if (query.isEmpty()) Text(tr("Search notebooks"), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                                BasicTextField(query, { query = it }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface), cursorBrush = SolidColor(colors.primary), modifier = Modifier.fillMaxWidth())
                            }
                        }
                        val shown = docs.filter { d -> query.isBlank() || d.title.contains(query, true) || Notebook.encode(d).contains(query, true) }
                        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp)) {
                            if (shown.isNotEmpty()) {
                                item { PxHeader("Recent") }
                                item { PxCards(*shown.map { d -> @Composable { NotebookRow(d, onOpen = { open = d }, onRename = { renaming = d }, onDuplicate = { NotebookStore.duplicate(context, d); refresh() }, onShare = { shareNotebookFile(context, d) }, onDelete = { deleting = d }) } }.toTypedArray()) }
                            } else if (query.isNotBlank()) item { Text(tr("No notebook matches."), color = colors.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
                            item { PxHeader("Start from") }
                            item {
                                PxCards(
                                    { PxActionRow("Blank notebook", "Text, math and graphs, one after another", TableIcons.Add) { create(tr("Untitled notebook"), listOf(Block.Text(1, ""), Block.Math(2, MathRow()))) } },
                                    if (calculator.history.isNotEmpty()) ({ PxActionRow("From history", "Pick calculations to turn into a notebook", TableIcons.History) { fromHistory = true } }) else null,
                                    { PxActionRow("Open a notebook file", "A .casnb file shared from another phone", TableIcons.Download) { importFile.launch(arrayOf("*/*")) } },
                                )
                            }
                            if (docs.isEmpty()) item { PxFootnote("A notebook keeps text, calculations and graphs together. Definitions like \$g := 9.81\$ hold for everything below them, so changing one value updates every answer and graph after it.") }
                        }
                    }
                    ExtendedFloatingActionButton(
                        onClick = { create(tr("Untitled notebook"), listOf(Block.Text(1, ""), Block.Math(2, MathRow()))) },
                        icon = { AppIcon(TableIcons.Add, contentDescription = null) },
                        text = { Text(tr("New notebook")) },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NotebookRow(d: Notebook.Doc, onOpen: () -> Unit, onRename: () -> Unit, onDuplicate: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        // A small page: a line per block, the math ones in the accent color, a curve for a graph.
        Column(Modifier.size(width = 54.dp, height = 66.dp).clip(RoundedCornerShape(10.dp)).background(colors.surfaceContainerLowest).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp)).padding(7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            d.blocks.take(6).forEach { b ->
                when (b) {
                    is Block.Graph -> Canvas(Modifier.fillMaxWidth().height(14.dp)) {
                        val p = Path().apply { moveTo(0f, size.height); cubicTo(size.width * 0.3f, -size.height * 0.4f, size.width * 0.6f, -size.height * 0.4f, size.width, size.height * 0.8f) }
                        drawPath(p, colors.primary, style = Stroke(1.5.dp.toPx()))
                    }
                    is Block.Math -> Box(Modifier.fillMaxWidth(0.55f).height(4.dp).clip(CircleShape).background(colors.primary))
                    is Block.Text -> Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(colors.outlineVariant))
                    is Block.Table -> Box(Modifier.fillMaxWidth().height(10.dp).border(1.dp, colors.primary, RoundedCornerShape(2.dp)))
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(d.title.ifBlank { tr("Untitled notebook") }, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = colors.onSurface, maxLines = 1)
            val graphs = d.blocks.count { it is Block.Graph }
            val ago = android.text.format.DateUtils.getRelativeTimeSpanString(d.edited).toString()
            Text(ago + " · " + tr("{0} blocks", d.blocks.size) + if (graphs > 0) ", " + tr("{0} graphs", graphs) else "", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        Box {
            IconButton(onClick = { menu = true }) { AppIcon(TableIcons.More, contentDescription = tr("More options"), tint = colors.onSurfaceVariant) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(tr("Rename")) }, leadingIcon = { AppIcon(TableIcons.Edit, null) }, onClick = { menu = false; onRename() })
                DropdownMenuItem(text = { Text(tr("Duplicate")) }, leadingIcon = { AppIcon(TableIcons.Copy, null) }, onClick = { menu = false; onDuplicate() })
                DropdownMenuItem(text = { Text(tr("Share")) }, leadingIcon = { AppIcon(TableIcons.Share, null) }, onClick = { menu = false; onShare() })
                DropdownMenuItem(text = { Text(tr("Delete")) }, leadingIcon = { AppIcon(TableIcons.Delete, null) }, onClick = { menu = false; onDelete() })
            }
        }
    }
}

/** Picks calculations from the history (newest first) for a new notebook. */
@Composable
private fun FromHistoryDialog(calculator: CalculatorViewModel, onCreate: (List<MathRow>) -> Unit, onDismiss: () -> Unit) {
    val items = remember { calculator.history.asReversed().take(60) }
    val picked = remember { mutableStateListOf<Int>() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("From history")) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                items.forEachIndexed { k, item ->
                    Row(Modifier.fillMaxWidth().clickable { if (k in picked) picked.remove(k) else picked.add(k) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = k in picked, onCheckedChange = { if (it) picked.add(k) else picked.remove(k) })
                        Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) { MathView(item.expression, 17.sp, MaterialTheme.colorScheme.onSurface) }
                    }
                }
            }
        },
        // Oldest first in the notebook, as they were worked out.
        confirmButton = { TextButton(enabled = picked.isNotEmpty(), onClick = { onCreate(picked.sortedDescending().map { MathCodec.copy(items[it].expression) }) }) { Text(tr("Create")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}

private fun safeName(title: String) = title.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().replace(' ', '-').ifEmpty { "notebook" }

private fun share(context: Context, name: String, mime: String, bytes: ByteArray) {
    runCatching {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, name).apply { writeBytes(bytes) }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = mime; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, tr("Share notebook")))
    }.onFailure { Toast.makeText(context, tr("Couldn't share") + (it.message?.let { m -> ": $m" } ?: ""), Toast.LENGTH_LONG).show() }
}

private fun shareNotebookFile(context: Context, d: Notebook.Doc) = share(context, safeName(d.title) + ".casnb", "application/octet-stream", Notebook.encode(d).toByteArray())

// ---- The editor --------------------------------------------------------------------------------

/**
 * A block being edited: its text, or its math in [Editor]s: a math block's line, a graph's
 * functions and its from and to, a table's column names and cells. [focus] is the one the
 * calculator's keypad types into.
 */
private class EditBlock(
    val id: Long, val kind: Char, text: String = "", row: MathRow = MathRow(), from: MathRow = MathRow(), to: MathRow = MathRow(),
    names: List<MathRow> = emptyList(), columns: List<List<MathRow>> = emptyList(), plot: Boolean = false, private val onChange: () -> Unit,
) {
    var text by mutableStateOf(TextFieldValue(text))
    var version by mutableIntStateOf(0)
    private fun ed(r: MathRow) = Editor(MathCodec.copy(r)).also { e -> e.onChange = { version++; onChange() } }
    val editor = ed(row)
    val from = ed(from)
    val to = ed(to)
    val names = mutableStateListOf<Editor>().apply { addAll(names.map { ed(it) }) }
    val cells = mutableStateListOf<androidx.compose.runtime.snapshots.SnapshotStateList<Editor>>().apply {
        columns.forEach { col -> add(mutableStateListOf<Editor>().apply { addAll(col.map { ed(it) }) }) }
    }
    var plot by mutableStateOf(plot)
    var focus by mutableStateOf<Editor?>(null)
    val rows get() = cells.maxOfOrNull { it.size } ?: 0

    init {
        // A new table: columns x and y with three empty rows.
        if (kind == 'D' && this.names.isEmpty()) {
            this.names += ed(MathRow(mutableListOf(com.example.cas.editor.Sym("x")))); this.names += ed(MathRow(mutableListOf(com.example.cas.editor.Sym("y"))))
            repeat(2) { cells += mutableStateListOf<Editor>().apply { repeat(3) { add(ed(MathRow())) } } }
        }
    }

    fun target(): Editor = focus ?: editor
    fun changed() { version++; onChange() }
    fun addRow(after: Int = rows - 1) { cells.forEach { col -> col.add((after + 1).coerceIn(0, col.size), ed(MathRow())) }; changed() }
    fun addColumn() {
        val used = names.map { Notebook.columnName(it.root) }.toSet()
        val letter = listOf("z", "u", "v", "w", "a", "b", "c", "p", "q", "r").firstOrNull { it !in used } ?: ""
        names += ed(MathRow(letter.map { com.example.cas.editor.Sym(it.toString()) }.toMutableList()))
        cells += mutableStateListOf<Editor>().apply { repeat(rows.coerceAtLeast(1)) { add(ed(MathRow())) } }
        changed()
    }
    fun removeRow(r: Int) { if (rows <= 1) return; cells.forEach { col -> if (r in col.indices) col.removeAt(r) }; focus = null; changed() }
    fun removeColumn(c: Int) { if (names.size <= 1 || c !in names.indices) return; names.removeAt(c); cells.removeAt(c); focus = null; changed() }
    /** Where [focus] is in the table: column and row (row −1 for a name). */
    fun cellOf(e: Editor?): Pair<Int, Int>? {
        if (e == null) return null
        names.indexOf(e).takeIf { it >= 0 }?.let { return it to -1 }
        cells.forEachIndexed { c, col -> col.indexOf(e).takeIf { it >= 0 }?.let { return c to it } }
        return null
    }

    fun toBlock(): Block = when (kind) {
        'T' -> Block.Text(id, text.text)
        'G' -> Block.Graph(id, com.example.cas.editor.trimMatrices(MathCodec.copy(editor.root)), MathCodec.copy(from.root), MathCodec.copy(to.root))
        'D' -> Block.Table(id, names.map { MathCodec.copy(it.root) }, cells.map { col -> col.map { MathCodec.copy(it.root) } }, plot)
        else -> Block.Math(id, MathCodec.copy(editor.root))
    }

    companion object {
        fun of(b: Block, onChange: () -> Unit) = when (b) {
            is Block.Text -> EditBlock(b.id, 'T', text = b.text, onChange = onChange)
            is Block.Math -> EditBlock(b.id, 'M', row = b.row, onChange = onChange)
            is Block.Graph -> EditBlock(b.id, 'G', row = b.row, from = b.from, to = b.to, onChange = onChange)
            is Block.Table -> EditBlock(b.id, 'D', names = b.names, columns = b.columns, plot = b.plot, onChange = onChange)
        }

        /** A copy of [b] with the id [id]. */
        fun withId(b: Block, id: Long): Block = when (b) {
            is Block.Text -> Block.Text(id, b.text)
            is Block.Math -> Block.Math(id, b.row)
            is Block.Graph -> Block.Graph(id, b.row, b.from, b.to)
            is Block.Table -> Block.Table(id, b.names, b.columns, b.plot)
        }
    }
}

/** The keypad driving the open block's field: a math line, a graph's functions or limits, a table cell. */
private class NotebookKeys(private val active: () -> EditBlock?, private val onEnter: () -> Unit) : KeypadHost {
    private var hidden by mutableStateOf(false)
    override val angleUnit get() = KeypadState.angle
    override val panelOpen get() = KeypadState.panelOpen
    override val selectedTab get() = KeypadState.tab
    override fun toggleAngle() = KeypadState.toggleAngle()
    override fun togglePanel() = KeypadState.togglePanel()
    override fun selectTab(index: Int) = KeypadState.selectTab(index)
    override fun moveLeft() { active()?.target()?.moveLeft() }
    override fun moveRight() { active()?.target()?.moveRight() }
    override fun press(action: KeyAction) {
        val b = active() ?: return
        val ed = b.target()
        when (action) {
            is KeyAction.Type -> ed.type(action.text)
            is KeyAction.Insert -> { ed.insert(action.make(), action.slot); if (action.path.isNotEmpty()) ed.enter(action.path) }
            KeyAction.Fraction -> ed.insertFraction()
            is KeyAction.Power -> ed.insertPower(action.exponent?.let { t -> MathRow(t.map { com.example.cas.editor.Sym(it.toString()) }.toMutableList()) })
            KeyAction.Paren -> ed.smartParen()
            KeyAction.ListBrackets -> { ed.type("["); ed.type("]"); ed.moveLeft() }
            KeyAction.Backspace -> ed.backspace()
            KeyAction.Clear -> ed.clear()
            KeyAction.Enter -> {
                // In a table, Enter goes down a cell (adding a row at the bottom); in a graph, on to the next field.
                val at = if (b.kind == 'D') b.cellOf(b.focus) else null
                when {
                    at != null -> {
                        val (c, r) = at
                        if (r + 1 >= b.rows) b.addRow()
                        b.focus = b.cells[c][r + 1]
                    }
                    b.kind == 'G' && b.focus == null -> b.focus = b.from
                    b.kind == 'G' && b.focus === b.from -> b.focus = b.to
                    else -> onEnter()
                }
            }
            is KeyAction.Sequence -> action.steps.forEach { press(it) }
            is KeyAction.Paste -> ed.insertRow(action.row)
            KeyAction.PickMatrix, KeyAction.MoreConstants, KeyAction.OpenSymbolBuilder -> Unit
        }
    }
    override fun insertMatrix(rows: Int, cols: Int) { active()?.target()?.insert(Matrix(rows, cols, growable = true), 0) }
    override val canUndo get() = active()?.let { it.version; it.target().canUndo } ?: false
    override val canRedo get() = active()?.let { it.version; it.target().canRedo } ?: false
    override fun undo() { active()?.target()?.undo() }
    override fun redo() { active()?.target()?.redo() }
    override var keypadHidden: Boolean
        get() = hidden
        set(v) { hidden = v }
    override val coordinates get() = KeypadState.coordinates
    override fun selectCoordinates(c: com.example.cas.cas.Coordinates) = KeypadState.selectCoordinates(c)
    override fun coordinatesOf(kind: com.example.cas.cas.CoordinateKind) = KeypadState.coordinatesOf(kind)
    override val unitSystem get() = KeypadState.unitSystem
    override fun selectUnitSystem(units: com.example.cas.engine.UnitSystem) = KeypadState.selectUnitSystem(units)
}

/**
 * A notebook open for editing: its title, then the blocks. Tap a block to edit it (math and
 * graphs with the calculator's keypad, text with the phone's keyboard); the bar at the bottom
 * adds a block after the one open. Everything is worked out again a moment after each change,
 * top to bottom, and saved.
 */
@Composable
private fun NotebookEditor(doc: Notebook.Doc, onClose: () -> Unit, onGraph: (GraphRequest) -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    val bump: () -> Unit = { revision++ }
    val blocks = remember { mutableStateListOf<EditBlock>().apply { addAll(doc.blocks.map { EditBlock.of(it, bump) }) } }
    var title by remember { mutableStateOf(doc.title) }
    var active by remember { mutableStateOf<EditBlock?>(null) }
    var run by remember { mutableStateOf<Notebook.Run?>(null) }
    var exporting by remember { mutableStateOf(false) }
    // Undo for adding, removing and moving blocks (typing has the keypad's own undo).
    val undoStack = remember { ArrayDeque<List<Block>>() }
    val keys = remember { NotebookKeys(active = { active }, onEnter = { active = null }) }
    val list = rememberLazyListState()

    fun current() = Notebook.Doc(doc.id, title, blocks.map { it.toBlock() }, System.currentTimeMillis())
    fun structural(change: () -> Unit) { undoStack.addLast(blocks.map { it.toBlock() }); if (undoStack.size > 30) undoStack.removeFirst(); change(); bump() }
    fun nextId() = (blocks.maxOfOrNull { it.id } ?: 0L) + 1
    fun add(kind: Char) = structural {
        val b = EditBlock(nextId(), kind, onChange = bump)
        val at = active?.let { blocks.indexOf(it) + 1 } ?: blocks.size
        blocks.add(at, b); active = b; keys.keypadHidden = false
        if (kind == 'D') b.focus = b.cells.firstOrNull()?.firstOrNull()
        scope.launch { list.animateScrollToItem(at + 1) }
    }
    val close = { NotebookStore.save(context, current()); onClose() }

    // Worked out (off the main thread) and saved a moment after each change.
    LaunchedEffect(revision, title, KeypadState.angle, KeypadState.unitSystem, KeypadState.coordinates) {
        delay(250)
        val snapshot = blocks.map { it.toBlock() }
        val angle = KeypadState.angle; val units = KeypadState.unitSystem; val coords = KeypadState.coordinates
        run = withContext(Dispatchers.Default) { runCatching { Notebook.run(snapshot, angle, units, coords) }.getOrNull() }
        withContext(Dispatchers.IO) { runCatching { NotebookStore.save(context, Notebook.Doc(doc.id, title, snapshot, System.currentTimeMillis())) } }
    }

    if (exporting) NotebookExportSheet(current(), run, onDismiss = { exporting = false })

    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler { if (active != null) active = null else close() }
        Surface(Modifier.fillMaxSize(), color = colors.surface) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(colors.surfaceContainerHigh).clickable(onClick = close), contentAlignment = Alignment.Center) {
                        AppIcon(TableIcons.Back, contentDescription = tr("Back"), tint = colors.onSurface)
                    }
                    Spacer(Modifier.weight(1f))
                    val canUndo = revision >= 0 && undoStack.isNotEmpty()
                    IconButton(enabled = canUndo, onClick = {
                        val previous = undoStack.removeLastOrNull() ?: return@IconButton
                        active = null; blocks.clear(); blocks.addAll(previous.map { EditBlock.of(it, bump) }); bump()
                    }) { AppIcon(TableIcons.Undo, contentDescription = tr("Undo"), tint = if (canUndo) colors.onSurface else colors.outline) }
                    IconButton(onClick = { active = null; exporting = true }) { AppIcon(TableIcons.Share, contentDescription = tr("Share notebook"), tint = colors.onSurface) }
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item(key = "title") {
                        Column(Modifier.padding(horizontal = 10.dp)) {
                            Box {
                                if (title.isEmpty()) Text(tr("Untitled notebook"), style = MaterialTheme.typography.headlineMedium, color = colors.outline)
                                BasicTextField(title, { title = it }, singleLine = true, textStyle = MaterialTheme.typography.headlineMedium.copy(color = colors.onSurface), cursorBrush = SolidColor(colors.primary),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), modifier = Modifier.fillMaxWidth())
                            }
                            Text(tr("Saved automatically"), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                    items(blocks, key = { it.id }) { b ->
                        val k = blocks.indexOf(b)
                        BlockView(
                            b, isActive = active === b, run = run,
                            onActivate = { if (active !== b) { active = b; b.focus = if (b.kind == 'D') b.cells.firstOrNull()?.firstOrNull() else null }; keys.keypadHidden = false },
                            onTap = { e, r, i -> active = b; keys.keypadHidden = false; b.focus = if (e === b.editor) null else e; e.setCursor(r, i) },
                            onUp = if (k > 0) ({ structural { blocks.removeAt(k); blocks.add(k - 1, b) } }) else null,
                            onDown = if (k < blocks.lastIndex) ({ structural { blocks.removeAt(k); blocks.add(k + 1, b) } }) else null,
                            onDuplicate = { structural { blocks.add(k + 1, EditBlock.of(EditBlock.withId(b.toBlock(), nextId()), bump)) } },
                            onDelete = { structural { blocks.remove(b); if (active === b) active = null } },
                            onGraph = { rows -> NotebookStore.save(context, current()); onGraph(GraphRequest(rows, 2)) },
                        )
                    }
                    if (blocks.isEmpty()) item { PxFootnote("Add text, math or a graph with the bar below.") }
                }
                val a = active
                if (a != null && a.kind == 'T') MarkdownToolbar(a.text, { a.text = it; bump() }, Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                if (a != null && a.kind != 'T' && !keys.keypadHidden) Keypad(keys)
                else if (a == null || a.kind == 'T') AddBar(onAdd = { add(it) }, modifier = Modifier.navigationBarsPadding())
                else ShowKeypadButton(onClick = { keys.keypadHidden = false }, modifier = Modifier.align(Alignment.End).padding(16.dp))
            }
        }
    }
}

/** Text · Math · Graph · Table: adds a block after the one open (or at the end). */
@Composable
private fun AddBar(onAdd: (Char) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth().height(64.dp).clip(RoundedCornerShape(32.dp)).background(colors.surfaceContainerHigh),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(Triple('T', TableIcons.Note, "Text"), Triple('M', TableIcons.ModeCalculator, "Math"), Triple('G', TableIcons.Mode2D, "Graph"), Triple('D', TableIcons.Table, "Table")).forEach { (kind, icon, label) ->
            Column(Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClickLabel = tr("Add") + " " + tr(label)) { onAdd(kind) }.padding(horizontal = 18.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AppIcon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
                Text(tr(label), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BlockView(
    b: EditBlock, isActive: Boolean, run: Notebook.Run?, onActivate: () -> Unit, onTap: (Editor, MathRow, Int) -> Unit,
    onUp: (() -> Unit)?, onDown: (() -> Unit)?, onDuplicate: () -> Unit, onDelete: () -> Unit, onGraph: (List<MathRow>) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column {
        // The open block's own actions: move it, duplicate it, delete it.
        if (isActive) Row(Modifier.fillMaxWidth().padding(bottom = 2.dp), horizontalArrangement = Arrangement.End) {
            @Composable fun act(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: (() -> Unit)?) =
                IconButton(enabled = onClick != null, onClick = { onClick?.invoke() }) { AppIcon(icon, contentDescription = tr(label), tint = if (onClick != null) colors.onSurfaceVariant else colors.outlineVariant) }
            act(TableIcons.ChevronUp, "Move up", onUp)
            act(TableIcons.ChevronDown, "Move down", onDown)
            act(TableIcons.Copy, "Duplicate", onDuplicate)
            act(TableIcons.Delete, "Delete", onDelete)
        }
        val shape = RoundedCornerShape(22.dp)
        val outline = if (isActive) Modifier.border(2.dp, colors.primary, shape) else Modifier
        when (b.kind) {
            'T' -> Box(Modifier.fillMaxWidth().clip(shape).then(outline).clickable(enabled = !isActive, onClick = onActivate).padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (isActive) {
                    Box {
                        if (b.text.text.isEmpty()) Text(tr("Text in Markdown: # heading, **bold**, *italic*, - list, \$x^2\$ math…"), style = MaterialTheme.typography.bodyLarge, color = colors.outline)
                        BasicTextField(b.text, { b.text = it; b.changed() }, textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 15.sp),
                            cursorBrush = SolidColor(colors.primary), modifier = Modifier.fillMaxWidth())
                    }
                } else if (b.text.text.isBlank()) Text(tr("Text"), style = MaterialTheme.typography.bodyLarge, color = colors.outline)
                else MarkdownView(b.text.text, onToggleTask = { line -> b.text = TextFieldValue(com.example.cas.engine.Markdown.toggleTask(b.text.text, line)); b.changed() })
            }
            'M' -> {
                val o = run?.outcomes?.get(b.id)
                val defines = o?.defines.orEmpty()
                Column(Modifier.fillMaxWidth().clip(shape).background(if (defines.isNotEmpty()) colors.surfaceContainer else colors.surfaceContainerHigh).then(outline).clickable(onClick = onActivate).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    if (defines.isNotEmpty()) Text(tr("Definitions"), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        if (b.editor.isEmpty && !isActive) Text(tr("Math"), style = MaterialTheme.typography.bodyLarge, color = colors.outline)
                        else MathView(
                            b.editor.root, MathSizes.historyInput, colors.onSurface, accent = colors.primary,
                            cursorRow = if (isActive) b.editor.row else null, cursorIndex = b.editor.index, version = b.version, onTap = { r, i -> onTap(b.editor, r, i) },
                        )
                    }
                    val answer = o?.answer
                    when {
                        o?.error != null && !b.editor.isEmpty -> Text(o.error, color = colors.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
                        answer != null -> Row(Modifier.align(Alignment.End).padding(top = 4.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                            if (!answer.isStatement) MathText(if (answer.isApproximate) "\$\\approx\$ " else "\$=\$ ", color = colors.primary, style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = MathSizes.historyAnswer), mathScale = 1f)
                            else MathText("\$\\Rightarrow\$ ", color = colors.primary, style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = MathSizes.historyAnswer), mathScale = 1f)
                            MathView(answer.exact, MathSizes.historyAnswer, colors.primary)
                        }
                    }
                }
            }
            'D' -> DataTableView(b, isActive, run, shape, outline, onActivate, onTap)
            else -> {
                val p = run?.plots?.get(b.id)
                Column(Modifier.fillMaxWidth().clip(shape).background(colors.surfaceContainerHigh).then(outline).clickable(onClick = onActivate)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(TableIcons.Mode2D, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                            if (b.editor.isEmpty && !(isActive && b.focus == null)) Text(tr("Functions to draw, separated by commas"), style = MaterialTheme.typography.bodyMedium, color = colors.outline)
                            else MathView(b.editor.root, 20.sp, colors.onSurface, accent = colors.primary, cursorRow = if (isActive && b.focus == null) b.editor.row else null, cursorIndex = b.editor.index, version = b.version, onTap = { r, i -> onTap(b.editor, r, i) })
                        }
                    }
                    if (p != null && p.error == null && p.xs.isNotEmpty()) NotebookPlot(p, Modifier.fillMaxWidth().height(190.dp))
                    else Text(p?.error?.let { tr(it) } ?: "", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        // The range, typed on the calculator's keypad like everything else (π, 2π, a…).
                        MathField("from", b.from, isActive && b.focus === b.from, b.version) { e, r, i -> onTap(e, r, i) }
                        Text("  –  ", color = colors.onSurfaceVariant)
                        MathField("to", b.to, isActive && b.focus === b.to, b.version) { e, r, i -> onTap(e, r, i) }
                        Spacer(Modifier.weight(1f))
                        if (p?.forGraph?.isNotEmpty() == true) TextButton(onClick = { onGraph(p.forGraph) }) {
                            Text(tr("Open in Graphs")); Spacer(Modifier.width(4.dp)); AppIcon(TableIcons.External, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

/** A small box of math (a graph's from or to), outlined while the keypad types into it. */
@Composable
private fun MathField(label: String, e: Editor, focused: Boolean, version: Int, onTap: (Editor, MathRow, Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column {
        Text(tr(label), style = MaterialTheme.typography.labelSmall, color = if (focused) colors.primary else colors.onSurfaceVariant)
        Box(
            Modifier.widthIn(min = 64.dp).heightIn(min = 36.dp).clip(RoundedCornerShape(10.dp)).background(colors.surfaceContainerHighest)
                .then(if (focused) Modifier.border(2.dp, colors.primary, RoundedCornerShape(10.dp)) else Modifier)
                .clickable { onTap(e, e.root, e.root.items.size) }.padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (e.isEmpty && !focused) Text(tr("auto"), style = MaterialTheme.typography.bodySmall, color = colors.outline)
            else MathView(e.root, 17.sp, colors.onSurface, accent = colors.primary, cursorRow = if (focused) e.row else null, cursorIndex = e.index, version = version, onTap = { r, i -> onTap(e, r, i) })
        }
    }
}

/**
 * A data table: a name over each column (the letter it defines for the blocks below) and a
 * value in each cell, all typed on the calculator's keypad. Enter goes down a cell, adding a row
 * at the bottom; the bar under it adds and removes rows and columns and turns on a plot of the
 * columns against the first.
 */
@Composable
private fun DataTableView(b: EditBlock, isActive: Boolean, run: Notebook.Run?, shape: RoundedCornerShape, outline: Modifier, onActivate: () -> Unit, onTap: (Editor, MathRow, Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val o = run?.outcomes?.get(b.id)
    val p = run?.plots?.get(b.id)
    val at = b.cellOf(b.focus)
    Column(Modifier.fillMaxWidth().clip(shape).background(colors.surfaceContainerHigh).then(outline).clickable(onClick = onActivate).padding(vertical = 10.dp)) {
        Row(Modifier.padding(horizontal = 12.dp).horizontalScroll(rememberScrollState())) {
            // Row numbers.
            Column {
                Box(Modifier.size(width = 32.dp, height = 44.dp))
                (0 until b.rows).forEach { r -> Box(Modifier.size(width = 32.dp, height = 40.dp), contentAlignment = Alignment.Center) { Text("${r + 1}", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant) } }
            }
            b.names.indices.forEach { c ->
                Column(Modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Max)) {
                    val nameFocused = isActive && at == c to -1
                    Box(
                        Modifier.fillMaxWidth().widthIn(min = 84.dp).height(44.dp).background(colors.surfaceContainerHighest)
                            .then(if (nameFocused) Modifier.border(2.dp, colors.primary) else Modifier)
                            .clickable { onTap(b.names[c], b.names[c].root, b.names[c].root.items.size) }.padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) { MathView(b.names[c].root, 18.sp, colors.primary, accent = colors.primary, cursorRow = if (nameFocused) b.names[c].row else null, cursorIndex = b.names[c].index, version = b.version, onTap = { r, i -> onTap(b.names[c], r, i) }) }
                    (0 until b.rows).forEach { r ->
                        val cell = b.cells[c].getOrNull(r)
                        val focused = isActive && at == c to r
                        Box(
                            Modifier.fillMaxWidth().widthIn(min = 84.dp).height(40.dp).background(if (r % 2 == 1) colors.surfaceContainer else colors.surfaceContainerHigh)
                                .then(if (focused) Modifier.border(2.dp, colors.primary) else Modifier)
                                .clickable(enabled = cell != null) { cell?.let { onTap(it, it.root, it.root.items.size) } }.padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            if (cell != null) MathView(cell.root, 17.sp, colors.onSurface, accent = colors.primary, cursorRow = if (focused) cell.row else null, cursorIndex = cell.index, version = b.version, onTap = { rr, i -> onTap(cell, rr, i) })
                        }
                    }
                }
            }
        }
        if (o?.error != null) Text(o.error, color = colors.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        else if (o?.defines?.isNotEmpty() == true) MathText(tr("Defines") + " " + o.defines.joinToString(", ") { "\$$it\$" } + " " + tr("as lists for the blocks below: mean, sd, sum…"),
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        if (isActive) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { b.addRow(at?.second?.takeIf { it >= 0 } ?: (b.rows - 1)) }) { Text(tr("+ Row")) }
            TextButton(onClick = { b.addColumn() }) { Text(tr("+ Column")) }
            TextButton(enabled = at != null && at.second >= 0 && b.rows > 1, onClick = { at?.let { b.removeRow(it.second) } }) { Text(tr("Delete row")) }
            TextButton(enabled = at != null && b.names.size > 1, onClick = { at?.let { b.removeColumn(it.first) } }) { Text(tr("Delete column")) }
            Text(tr("Plot"), style = MaterialTheme.typography.labelLarge, color = colors.onSurface, modifier = Modifier.padding(start = 8.dp, end = 6.dp))
            androidx.compose.material3.Switch(checked = b.plot, onCheckedChange = { b.plot = it; b.changed() })
        }
        if (b.plot && p != null && p.points.isNotEmpty()) NotebookPlot(p, Modifier.fillMaxWidth().height(190.dp).padding(top = 8.dp))
    }
}

/** A graph block's picture: the grid, the axes and each curve in the graphs' colors, with the ends of each axis marked. */
@Composable
private fun NotebookPlot(p: Notebook.Plot, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val curveColors = (0 until maxOf(p.curves.size, p.points.size)).map { plotColor(it) }
    val label = TextStyle(fontSize = 10.sp, color = colors.onSurfaceVariant)
    Canvas(modifier.padding(horizontal = 12.dp).clip(RoundedCornerShape(14.dp)).background(colors.surfaceContainerLowest)) {
        val x0 = p.xs.first(); val x1 = p.xs.last()
        fun sx(x: Double) = ((x - x0) / (x1 - x0) * size.width).toFloat()
        fun sy(v: Double) = (size.height - (v - p.yMin) / (p.yMax - p.yMin) * size.height).toFloat()
        fun step(span: Double): Double { val raw = span / 6; val e = Math.pow(10.0, Math.floor(Math.log10(raw))); return listOf(1.0, 2.0, 5.0, 10.0).first { it * e >= raw } * e }
        val gx = step(x1 - x0); var x = Math.ceil(x0 / gx) * gx
        while (x <= x1 + 1e-9) { drawLine(colors.outlineVariant.copy(alpha = 0.5f), Offset(sx(x), 0f), Offset(sx(x), size.height), 1f); x += gx }
        val gy = step(p.yMax - p.yMin); var y = Math.ceil(p.yMin / gy) * gy
        while (y <= p.yMax + 1e-9) { drawLine(colors.outlineVariant.copy(alpha = 0.5f), Offset(0f, sy(y)), Offset(size.width, sy(y)), 1f); y += gy }
        if (p.yMin < 0 && p.yMax > 0) drawLine(colors.outline, Offset(0f, sy(0.0)), Offset(size.width, sy(0.0)), 1.5f)
        if (x0 < 0 && x1 > 0) drawLine(colors.outline, Offset(sx(0.0), 0f), Offset(sx(0.0), size.height), 1.5f)
        p.curves.forEachIndexed { k, cv ->
            val path = Path(); var open = false
            for (i in p.xs.indices) {
                val v = cv[i]
                if (!v.isFinite() || v < p.yMin - (p.yMax - p.yMin) || v > p.yMax + (p.yMax - p.yMin)) { open = false; continue }
                if (!open) { path.moveTo(sx(p.xs[i]), sy(v)); open = true } else path.lineTo(sx(p.xs[i]), sy(v))
            }
            drawPath(path, curveColors[k], style = Stroke(2.6.dp.toPx()))
        }
        p.points.forEachIndexed { k, (px, py) ->
            for (i in px.indices) if (py[i] in p.yMin..p.yMax) drawCircle(curveColors[k], 4.dp.toPx(), Offset(sx(px[i]), sy(py[i])))
        }
        drawText(measurer, shortNumber(x0), Offset(6f, size.height - 16.dp.toPx()), label)
        val right = measurer.measure(shortNumber(x1), label)
        drawText(right, topLeft = Offset(size.width - right.size.width - 6f, size.height - 16.dp.toPx()))
        drawText(measurer, shortNumber(p.yMax), Offset(6f, 4f), label)
    }
}

/** Share: a PDF typeset like a LaTeX document, the .tex itself, or the notebook file. */
@Composable
private fun NotebookExportSheet(d: Notebook.Doc, run: Notebook.Run?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    var busy by remember { mutableStateOf(false) }
    fun go(what: Int) {
        if (busy) return
        busy = true
        scope.launch {
            val r = run ?: withContext(Dispatchers.Default) { Notebook.run(d.blocks, KeypadState.angle, KeypadState.unitSystem, KeypadState.coordinates) }
            val name = safeName(d.title)
            runCatching {
                when (what) {
                    0 -> {
                        val date = java.text.DateFormat.getDateInstance(java.text.DateFormat.LONG).format(java.util.Date())
                        val bytes = withContext(Dispatchers.Default) {
                            SceneExport.installMetrics(context)
                            SceneExport.pdfPages(context, com.example.cas.graph.NotebookExport.pages(d, r, "$date · CasCalc"))
                        }
                        share(context, "$name.pdf", "application/pdf", bytes)
                    }
                    1 -> share(context, "$name.tex", "application/x-tex", Notebook.latex(d, r).toByteArray())
                    else -> shareNotebookFile(context, d)
                }
            }.onFailure { Toast.makeText(context, tr("Couldn't export") + (it.message?.let { m -> ": $m" } ?: ""), Toast.LENGTH_LONG).show() }
            busy = false
            onDismiss()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Share notebook")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf(
                    Triple("PDF", "Typeset like a LaTeX document, with the graphs as figures", TableIcons.Document),
                    Triple("LaTeX", "A .tex file for Overleaf or your own template; graphs as pgfplots", TableIcons.Font),
                    Triple("Notebook file", "Open it in Notebooks on another phone, fully editable", TableIcons.Share),
                ).forEachIndexed { k, (t, s, icon) ->
                    val big = 20.dp; val small = 5.dp
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(if (k == 0) big else small, if (k == 0) big else small, if (k == 2) big else small, if (k == 2) big else small))
                            .background(colors.surfaceContainerHighest).clickable(enabled = !busy) { go(k) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(icon, contentDescription = null, tint = colors.primary)
                        Spacer(Modifier.width(14.dp))
                        Column { Text(tr(t), style = MaterialTheme.typography.titleMedium); Text(tr(s), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Close")) } },
    )
}
