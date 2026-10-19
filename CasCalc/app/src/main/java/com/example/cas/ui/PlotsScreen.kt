package com.example.cas.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.cas.cas.Expr
import com.example.cas.editor.Editor
import com.example.cas.editor.MathCodec
import com.example.cas.editor.MathRow
import com.example.cas.editor.Matrix
import com.example.cas.editor.Sym
import com.example.cas.engine.Evaluator
import com.example.cas.engine.UserFunction
import com.example.cas.graph.DataTable
import com.example.cas.graph.ExportFormat
import com.example.cas.graph.Figure
import com.example.cas.graph.FigurePython
import com.example.cas.graph.Scene
import com.example.cas.ui.theme.CasFonts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/*
 * The Plots mode: figures made of matplotlib-style layers. The library lists saved figures and
 * starts new ones; the editor shows the figure live above its layers, with the gallery of layer
 * types behind +, a page for each layer's inputs and look, and pages for the axes and the figure.
 */

/** Saved figures: one .casfig file each, in the app's own storage. */
object FigureStore {
    private fun dir(c: Context) = File(c.filesDir, "figures").apply { mkdirs() }

    fun list(c: Context): List<Figure.Doc> = dir(c).listFiles { f -> f.name.endsWith(".casfig") }.orEmpty()
        .mapNotNull { f -> runCatching { Figure.decode(f.nameWithoutExtension, f.readText()) }.getOrNull() }
        .sortedByDescending { it.edited }

    fun load(c: Context, id: String): Figure.Doc? = runCatching { Figure.decode(id, File(dir(c), "$id.casfig").readText()) }.getOrNull()
    fun save(c: Context, d: Figure.Doc) { File(dir(c), d.id + ".casfig").writeText(Figure.encode(d)) }
    fun delete(c: Context, id: String) { File(dir(c), "$id.casfig").delete() }
    fun exists(c: Context, id: String) = File(dir(c), "$id.casfig").exists()
    fun newId(): String = java.lang.Long.toString(System.currentTimeMillis(), 36) + (100..999).random()
}

/**
 * The figure open in Plots (from the 2D graph's Open as figure, a table's Chart, or the saved
 * graphs' Plots), and one asked for from elsewhere, opened by the app.
 */
object PlotsNav {
    /** A figure was opened from elsewhere: the app switches to the Plots mode. */
    var open by mutableStateOf(false)
    /** The figure being edited: a saved one, or a new one not saved yet. */
    var doc by mutableStateOf<Figure.Doc?>(null)
    var request by mutableStateOf<Figure.Doc?>(null)
    /** Counts the figures opened, so each starts the editor afresh. */
    var session by mutableIntStateOf(0)
    /** The saved figures asked for from the top bar: the editor closes, asking about changes first. */
    var closeAsked by mutableStateOf(false)

    fun show(d: Figure.Doc) { doc = d; session++; open = true }
}

/** New figures: blank, from templates, from a table, from the 2D graph's lines. */
object FigureFactory {
    private fun m(text: String) = MathRow(text.map { Sym(it.toString()) }.toMutableList())

    fun blank(name: String = "") = Figure.Doc(FigureStore.newId(), "", listOf(Figure.Axes()), name = name, edited = System.currentTimeMillis())

    /** A figure of a table: its first two letter-named columns as [kind] (scatter, line, bars, histogram, box). */
    fun fromTable(t: DataTable, kind: Figure.Kind): Figure.Doc {
        val letters = t.names.map { it.trim() }.filter { Figure.isColumnName(it) }
        val a = letters.getOrNull(0) ?: "x"; val b = letters.getOrNull(1) ?: a
        val layer = when (kind) {
            Figure.Kind.Scatter -> Figure.Layer(kind, listOf(m(a), m(b)))
            Figure.Kind.Plot -> Figure.Layer(kind, listOf(m(a), m(b)), style = Figure.Style(marker = 0))
            Figure.Kind.Bar -> Figure.Layer(kind, listOf(m(b)))
            Figure.Kind.Hist, Figure.Kind.Ecdf -> Figure.Layer(kind, listOf(m(a)))
            Figure.Kind.Box, Figure.Kind.Violin -> Figure.Layer(kind, listOf(m(letters.ifEmpty { listOf(a) }.joinToString(","))), text = letters.joinToString(", "))
            else -> Figure.Layer(kind, listOf(m(a), m(b)))
        }
        val axes = Figure.Axes(listOf(layer), xLabel = if (kind == Figure.Kind.Bar || kind == Figure.Kind.Box || kind == Figure.Kind.Violin) "" else a, yLabel = when (kind) { Figure.Kind.Hist -> "count"; Figure.Kind.Ecdf -> "share"; Figure.Kind.Box, Figure.Kind.Violin -> ""; else -> b })
        return Figure.Doc(FigureStore.newId(), "", listOf(axes), data = t, name = "Table", edited = System.currentTimeMillis())
    }

    /** One expression (a mode guide's example) as a line layer. */
    fun fromRow(row: MathRow): Figure.Doc {
        val l = Figure.Layer(Figure.Kind.Plot, listOf(MathRow(), MathCodec.copy(row)), style = Figure.Style(label = "$" + com.example.cas.engine.Latex.of(row) + "$"))
        return Figure.Doc(FigureStore.newId(), "", listOf(Figure.Axes(listOf(l), xLabel = "\$x\$", legend = 0)), name = "Figure", edited = System.currentTimeMillis())
    }

    /**
     * The 2D graph as a figure: curves y = f(x) as lines and r = f(θ) as polar curves (the visible
     * ones), each in its line's color, style, thickness and point shape. Every list of points
     * brings its whole table (all its columns, not only the two it plots, hidden lines' too) into
     * the figure's table, and its visible ones are drawn as scatters (with error bars when the
     * table has σ). The limits fit what's drawn.
     */
    fun fromGraph(all: List<PlotFunction>): Figure.Doc {
        val names = ArrayList<String>()
        val sheets = ArrayList<Figure.Sheet>()
        val layers = ArrayList<Figure.Layer>()
        // Names the expressions can use: a column's own when it's letters and free, else a free letter.
        val letters = listOf("x", "y", "u", "v", "p", "q", "s", "w", "a", "b", "c", "d", "f", "g", "h", "k", "m", "n", "r", "z", "j", "l", "o")
        // The CSV's own names, made typeable (spaces as _), numbered when two are the same.
        fun fresh(want: String): String {
            val w = Figure.columnName(want)
            if (w.isNotEmpty() && w != "e" && w != "i" && w !in names) return w
            if (w.length > 1) return (2..999).map { "${w}_$it" }.first { it !in names }
            return letters.firstOrNull { it !in names } ?: "column${names.size + 1}"
        }
        fun add(name: String): String { val n = fresh(name); names += n; return n }
        val functions = all.filter { it.visible }
        fun style(f: PlotFunction, label: String) = Figure.Style(color = f.colorIndex, argb = f.customColor ?: 0, line = f.lineStyle, width = (f.thickness * 0.6).toDouble().coerceIn(0.5, 4.0), marker = -1, label = label)
        fun rhs(r: MathRow, left: String): MathRow? {
            val items = r.items
            val eq = items.indexOfFirst { it is Sym && it.text == "=" }
            if (eq < 0) return MathCodec.copy(r)
            val l = items.take(eq)
            if (l.size != 1 || (l[0] as? Sym)?.text != left) return null
            return MathCodec.decode(MathCodec.encode(items.drop(eq + 1)))
        }
        fun num(v: Double) = com.example.cas.graph.DataTable.text(v)
        // Every list's table, visible or not, so the figure has the same data as the graph: each its own sheet.
        val tableRoles = HashMap<PlotFunction, Triple<String?, String?, String?>>()
        for (f in all) {
            val k = f.plot as? Plot2DKind.PointList ?: continue
            val t = f.table?.takeIf { it.points().size == k.xs.size } ?: DataTable.fromPoints(k.xs, k.ys)
            val ns = t.names.indices.map { c -> add(t.names[c].ifBlank { if (c == t.x) "x" else if (c == t.y) "y" else "" }) }
            tableRoles[f] = Triple(t.x?.let { ns.getOrNull(it) }, t.y?.let { ns.getOrNull(it) }, t.sigmaY?.let { ns.getOrNull(it) })
            sheets += Figure.Sheet(f.name?.takeIf { it.isNotBlank() } ?: "Data ${sheets.size + 1}", DataTable(ns, t.columns, null, null))
        }
        for (f in functions) when (val k = f.plot) {
            is Plot2DKind.Explicit -> rhs(f.editor.root, "y")?.takeIf { it.items.isNotEmpty() }?.let { r ->
                layers += Figure.Layer(Figure.Kind.Plot, listOf(MathRow(), r), style = style(f, "$" + com.example.cas.engine.Latex.of(r) + "$"))
            }
            is Plot2DKind.Polar -> rhs(f.editor.root, "r")?.takeIf { it.items.isNotEmpty() }?.let { r ->
                layers += Figure.Layer(Figure.Kind.Polar, listOf(r), style = style(f, "\$r = " + com.example.cas.engine.Latex.of(r) + "\$"))
            }
            is Plot2DKind.PointList -> {
                val (a, b, sy) = tableRoles[f] ?: continue
                if (a == null || b == null) continue
                val label = f.name.takeIf { !it.isNullOrBlank() } ?: "Data"
                // The table's points without error bars; its σ columns can go in the errors (they're in the table).
                layers += Figure.Layer(Figure.Kind.Scatter, listOf(m(a), m(b)), style = style(f, label).copy(marker = f.pointShape, size = 3.0))
            }
            is Plot2DKind.Point -> {
                val (x, y) = runCatching { k.x(DoubleArray(8)) to k.y(DoubleArray(8)) }.getOrNull() ?: continue
                layers += Figure.Layer(Figure.Kind.Scatter, listOf(com.example.cas.engine.PlainMath.parse("[${num(x)}]"), com.example.cas.engine.PlainMath.parse("[${num(y)}]")),
                    style = style(f, "(${num(x)}, ${num(y)})").copy(marker = f.pointShape, size = 3.0))
            }
            else -> Unit
        }
        val axes = Figure.Axes(layers, xLabel = "\$x\$", yLabel = "\$y\$", legend = 0)
        return Figure.Doc(FigureStore.newId(), "", listOf(axes), name = "From 2D graph", edited = System.currentTimeMillis()).withSheets(sheets)
    }

    class Template(val name: String, val detail: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val make: () -> Figure.Doc)

    val templates: List<Template> get() = listOf(
        Template("Blank", "An empty figure", FigureIcons.NewFigure) { blank("Figure") },
        Template("Function", "A curve with its legend", Figure.Kind.Plot.let { FigureIcons.of(it) }) {
            val row = MathRow(mutableListOf(com.example.cas.editor.Func("sin", listOf(m("x")))))
            Figure.Doc(FigureStore.newId(), "", listOf(Figure.Axes(listOf(Figure.Layer(Figure.Kind.Plot, listOf(MathRow(), row), style = Figure.Style(label = "\$\\sin x\$"))), xLabel = "\$x\$", yLabel = "\$y\$")), name = "Function", edited = System.currentTimeMillis())
        },
        Template("Data", "A table, as points and a histogram", FigureIcons.of(Figure.Kind.Hist)) {
            val t = DataTable(listOf("x", "y"), listOf(listOf("1", "2", "3", "4", "5", "6", "7", "8"), listOf("2.1", "3.9", "6.2", "7.8", "10.1", "12.2", "13.8", "16.1")), null, null)
            Figure.withLayout(Figure.Doc(FigureStore.newId(), "", listOf(
                Figure.Axes(listOf(Figure.Layer(Figure.Kind.Scatter, listOf(m("x"), m("y")), style = Figure.Style(label = "data")), Figure.Layer(Figure.Kind.Plot, listOf(MathRow(), m("2x")), style = Figure.Style(line = 1, label = "\$y = 2x\$"))), xLabel = "\$x\$", yLabel = "\$y\$"),
                Figure.Axes(listOf(Figure.Layer(Figure.Kind.Hist, listOf(m("y")))), xLabel = "\$y\$", yLabel = "count"),
            ), "AB", aspect = 0.5, data = t, name = "Data", edited = System.currentTimeMillis()), "AB")
        },
        Template("Fields", "Contours and arrows of f(x, y)", FigureIcons.of(Figure.Kind.ContourF)) {
            val f = MathRow(mutableListOf(Sym("x"), com.example.cas.editor.Pow(m("2")), Sym("−"), Sym("y"), com.example.cas.editor.Pow(m("2"))))
            Figure.Doc(FigureStore.newId(), "", listOf(Figure.Axes(listOf(Figure.Layer(Figure.Kind.ContourF, listOf(MathRow(), MathRow(), f), style = Figure.Style(levels = 12)), Figure.Layer(Figure.Kind.Quiver, listOf(MathRow(), MathRow(), m("2x"), m("−2y")), style = Figure.Style(color = 5))), equal = true, grid = 0)), name = "Fields", edited = System.currentTimeMillis())
        },
        Template("Grid", "Four axes, 2 × 2", FigureIcons.Layout) { Figure.withLayout(blank("Grid"), "AB;CD").copy(aspect = 0.8) },
    )
}

/** How the screen draws theme 0: the app's own colors. */
@Composable
fun appFigureTheme(): Figure.Theme {
    val c = MaterialTheme.colorScheme
    // The same object while the colors stay (figures are redrawn when it changes).
    return remember(c) {
        Figure.Theme("App", c.surfaceContainerLowest.toArgb(), c.surfaceContainerLowest.toArgb(), c.onSurface.toArgb(), c.onSurfaceVariant.toArgb(),
            c.outlineVariant.copy(alpha = 0.6f).toArgb(), plotColors(c).map { it.toArgb() })
    }
}

/** Works a figure's inputs out: its own table's columns, then [variables] and [functions] (a notebook's, above it). */
fun figureEval(d: Figure.Doc, variables: Map<String, Expr> = emptyMap(), functions: Map<String, UserFunction> = emptyMap()): (MathRow) -> Expr {
    val vars = variables + Figure.variables(d)
    val angle = KeypadState.angle; val units = KeypadState.unitSystem; val coords = KeypadState.coordinates
    return Figure.withFallback(vars) { v, row -> Evaluator(angle, null, v, units, coords, functions).evaluate(com.example.cas.editor.trimMatrices(MathCodec.copy(row))) }
}

/** A figure's scene drawn to fill the box (which should have its shape). */
@Composable
fun FigureCanvas(scene: Scene?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val fonts = remember { SceneExport.font(context) }
    Canvas(modifier) {
        val s = scene ?: return@Canvas
        // Fitted into the box, in the middle (a short box shows the whole figure, smaller).
        val k = minOf(size.width / s.width.toFloat(), size.height / s.height.toFloat())
        val dx = (size.width - s.width.toFloat() * k) / 2; val dy = (size.height - s.height.toFloat() * k) / 2
        drawIntoCanvas { c ->
            val nc = c.nativeCanvas as Any as android.graphics.Canvas
            nc.save(); nc.translate(dx, dy); SceneExport.draw(nc, s, k, fonts); nc.restore()
        }
    }
}

private fun safeFileName(s: String) = s.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().replace(' ', '-').ifEmpty { "figure" }

private fun shareBytes(context: Context, name: String, mime: String, bytes: ByteArray) {
    runCatching {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, name).apply { writeBytes(bytes) }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = mime; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, tr("Share figure")))
    }.onFailure { Toast.makeText(context, tr("Couldn't share") + (it.message?.let { m -> ": $m" } ?: ""), Toast.LENGTH_LONG).show() }
}

// ---- The mode ----------------------------------------------------------------------------------

/**
 * The Plots mode (after complex plotting, when turned on in Settings › Graphs): the saved
 * figures and new ones, or the figure open in the editor. Leaving a figure asks first whether
 * to save its changes.
 */
@Composable
fun PlotsScreen(fromGraph: () -> Figure.Doc) {
    val context = LocalContext.current
    val open = PlotsNav.doc
    if (open == null) PlotsHome(onOpen = { PlotsNav.show(it) }, fromGraph = fromGraph)
    else key(PlotsNav.session) {
        Column(Modifier.fillMaxSize().imePadding()) {
            FigureEditor(open, eval = { figureEval(it) }, autosave = false,
                onSave = { d -> runCatching { FigureStore.save(context, d) }; PlotsNav.doc = d },
                onClose = { PlotsNav.doc = null })
        }
    }
}

/** The saved figures, and new ones: from templates, from the 2D graph, or from a .casfig file. */
@Composable
private fun PlotsHome(onOpen: (Figure.Doc) -> Unit, fromGraph: () -> Figure.Doc) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val tablet = isTabletLayout()
    var docs by remember { mutableStateOf(FigureStore.list(context)) }
    var renaming by remember { mutableStateOf<Figure.Doc?>(null) }
    var deleting by remember { mutableStateOf<Figure.Doc?>(null) }
    val opener = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val text = context.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
            onOpen(Figure.decode(FigureStore.newId(), text).copy(edited = System.currentTimeMillis()))
        }.onFailure { Toast.makeText(context, tr("That isn't a figure file"), Toast.LENGTH_LONG).show() }
    }
    renaming?.let { d ->
        var name by remember(d.id) { mutableStateOf(d.name) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(tr("Rename figure")) },
            text = { OutlinedTextField(name, { name = it }, singleLine = true, label = { Text(tr("Name")) }) },
            confirmButton = { TextButton(onClick = { FigureStore.save(context, d.copy(name = name.trim())); docs = FigureStore.list(context); renaming = null }) { Text(tr("Rename")) } },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(tr("Cancel")) } },
        )
    }
    deleting?.let { d ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(tr("Delete {0}?", d.name.ifBlank { tr("this figure") })) },
            text = { Text(tr("The saved figure will be removed.")) },
            confirmButton = { TextButton(onClick = { FigureStore.delete(context, d.id); docs = FigureStore.list(context); deleting = null }) { Text(tr("Delete"), color = colors.error) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(tr("Cancel")) } },
        )
    }
    // A new figure's card: its icon in a circle, its name and what it starts with.
    @Composable
    fun NewCard(name: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
        Column(modifier.clip(RoundedCornerShape(24.dp)).background(colors.surfaceContainerHigh).clickable(onClickLabel = tr(name), onClick = onClick).padding(14.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                AppIcon(icon, contentDescription = null, tint = colors.onPrimaryContainer, accent = colors.primary)
            }
            Spacer(Modifier.height(10.dp))
            Text(tr(name), style = MaterialTheme.typography.titleMedium, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(tr(detail), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
    val columns = if (tablet) 2 else 1
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            PxHeader("New figure")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NewCard("From 2D graph", "Its curves and data, as layers", FigureIcons.of(Figure.Kind.Plot), Modifier.width(140.dp)) { onOpen(fromGraph()) }
                FigureFactory.templates.forEach { t -> NewCard(t.name, t.detail, t.icon, Modifier.width(140.dp)) { onOpen(t.make()) } }
                NewCard("Open file", "A .casfig figure", TableIcons.ImportData, Modifier.width(140.dp)) { opener.launch(arrayOf("*/*")) }
            }
            PxHeader("Saved")
            if (docs.isEmpty()) PxFootnote("Saved figures show here. A table's Chart and the 2D graph's ⋮ › Open as figure start one too.")
        }
        items(docs.chunked(columns), key = { row -> row.joinToString { it.id } }) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { d ->
                    Box(Modifier.weight(1f)) {
                        SavedFigureRow(d, onOpen = { onOpen(d) }, onRename = { renaming = d },
                            onDuplicate = { FigureStore.save(context, d.withId(FigureStore.newId()).copy(name = d.name + " " + tr("copy"), edited = System.currentTimeMillis())); docs = FigureStore.list(context) },
                            onShare = { shareBytes(context, safeFileName(d.name) + ".casfig", "application/octet-stream", Figure.encode(d).toByteArray()) },
                            onDelete = { deleting = d })
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** A saved figure: a picture of it, its name and when it was saved, and its menu. */
@Composable
private fun SavedFigureRow(d: Figure.Doc, onOpen: () -> Unit, onRename: () -> Unit, onDuplicate: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val theme = appFigureTheme()
    var scene by remember(d.id, d.edited) { mutableStateOf<Scene?>(null) }
    LaunchedEffect(d.id, d.edited, theme) {
        val eval = figureEval(d)
        scene = withContext(Dispatchers.Default) { runCatching { Figure.build(d, eval, theme, 320.0).scene }.getOrNull() }
    }
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.surfaceContainerHigh).clickable(onClick = onOpen).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 112.dp, height = 80.dp).clip(RoundedCornerShape(14.dp)).background(Color(if (d.theme == 0) theme.background else Figure.THEMES.getOrElse(d.theme) { theme }.background))) {
            FigureCanvas(scene, Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(d.name.ifBlank { tr("Untitled figure") }, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(tr("{0} layers", d.axes.sumOf { it.layers.size }) + " · " + java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date(d.edited)),
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1)
        }
        Box {
            IconButton(onClick = { menu = true }) { AppIcon(TableIcons.More, contentDescription = tr("More options"), tint = colors.onSurfaceVariant) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(tr("Rename")) }, leadingIcon = { AppIcon(TableIcons.Edit, null) }, onClick = { menu = false; onRename() })
                DropdownMenuItem(text = { Text(tr("Duplicate")) }, leadingIcon = { AppIcon(TableIcons.Copy, null) }, onClick = { menu = false; onDuplicate() })
                DropdownMenuItem(text = { Text(tr("Share file")) }, leadingIcon = { AppIcon(TableIcons.Share, null) }, onClick = { menu = false; onShare() })
                DropdownMenuItem(text = { Text(tr("Delete"), color = colors.error) }, leadingIcon = { AppIcon(TableIcons.Delete, null, tint = colors.error) }, onClick = { menu = false; onDelete() })
            }
        }
    }
}

// ---- The editor --------------------------------------------------------------------------------

private enum class FigurePage { None, Gallery, Axes, Figure, Python }

/**
 * A figure open for editing: the figure live at the top, then the current axes' layers (or the
 * open layer's inputs and look), + for the gallery of layer types. Changes are saved a moment
 * after they're made ([onSave]); [eval] works the inputs out.
 */
@Composable
fun FigureEditor(start: Figure.Doc, eval: (Figure.Doc) -> (MathRow) -> Expr, onSave: (Figure.Doc) -> Unit, onClose: () -> Unit, autosave: Boolean = true) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var doc by remember { mutableStateOf(start) }
    var axesAt by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var page by remember { mutableStateOf(FigurePage.None) }
    var dataOpen by remember { mutableStateOf(false) }
    var built by remember { mutableStateOf<Figure.Built?>(null) }
    val undo = remember { ArrayDeque<Figure.Doc>() }
    var undoCount by remember { mutableIntStateOf(0) }
    val theme = appFigureTheme()
    val tablet = isTabletLayout()

    fun change(next: Figure.Doc, record: Boolean = true) {
        if (record) { undo.addLast(doc); if (undo.size > 40) undo.removeFirst(); undoCount = undo.size }
        doc = next.copy(edited = System.currentTimeMillis())
    }
    val axes = doc.axes.getOrElse(axesAt) { Figure.Axes() }
    fun setAxes(a: Figure.Axes, record: Boolean = true) = change(doc.copy(axes = doc.axes.mapIndexed { k, x -> if (k == axesAt) a else x }), record)
    fun setLayer(k: Int, l: Figure.Layer, record: Boolean = true) = setAxes(axes.copy(layers = axes.layers.mapIndexed { i, x -> if (i == k) l else x }), record)

    // Drawn off the main thread a moment after each change, and saved.
    LaunchedEffect(doc, theme) {
        delay(40)
        val d = doc; val ev = eval(d)
        built = withContext(Dispatchers.Default) { runCatching { Figure.build(d, ev, theme) }.getOrNull() }
    }
    // In a notebook: saved with it as it changes. In Plots: saved when asked (Save, or on leaving).
    var savedDoc by remember { mutableStateOf(if (autosave) null else start.takeIf { FigureStore.exists(context, it.id) }) }
    var asking by remember { mutableStateOf(false) }
    val dirty = !autosave && savedDoc?.let { Figure.encode(it.copy(edited = 0)) != Figure.encode(doc.copy(edited = 0)) } != false
    if (autosave) {
        LaunchedEffect(doc) { delay(500); withContext(Dispatchers.IO) { onSave(doc) } }
        val latest = androidx.compose.runtime.rememberUpdatedState(doc)
        androidx.compose.runtime.DisposableEffect(Unit) { onDispose { onSave(latest.value) } }
    }
    // Saving (Plots): under its name, as a new figure, or over a saved figure of the same name.
    var naming by remember { mutableStateOf<Boolean?>(null) }
    var afterSave by remember { mutableStateOf<() -> Unit>({}) }
    var overwrite by remember { mutableStateOf<Pair<Figure.Doc, () -> Unit>?>(null) }
    fun store(d: Figure.Doc) { val n = d.copy(edited = System.currentTimeMillis()); doc = n; onSave(n); savedDoc = n }
    /** Saved as [name] ([asNew]: as a new figure). One already saved with that name is replaced ([confirmed], or asked first). */
    fun saveAs(name: String, asNew: Boolean, confirmed: Boolean, then: () -> Unit) {
        val clean = name.trim().ifEmpty { tr("Figure") }
        val other = FigureStore.list(context).firstOrNull { it.id != doc.id && it.name.trim().equals(clean, ignoreCase = true) }
        val mine = (if (asNew) doc.withId(FigureStore.newId()) else doc).copy(name = clean)
        val replace = {
            // Renamed onto another: that one is replaced, and this one's old save goes (one figure, not two).
            if (!asNew && savedDoc != null) runCatching { FigureStore.delete(context, doc.id) }
            store(mine.withId(other!!.id)); then()
        }
        if (other == null) { store(mine); then() } else if (confirmed) replace() else overwrite = other to replace
    }
    /** Save: a figure saved before goes back under its name; a new one is named first. */
    fun quickSave(then: () -> Unit = {}) { if (savedDoc == null) { afterSave = then; naming = false } else saveAs(doc.name, asNew = false, confirmed = false, then = then) }
    val close = { if (autosave) { onSave(doc); onClose() } else if (dirty) asking = true else onClose() }
    // The top bar's saved figures: leaving, as Back does.
    if (!autosave) LaunchedEffect(PlotsNav.closeAsked) { if (PlotsNav.closeAsked) { PlotsNav.closeAsked = false; close() } }
    naming?.let { asNew ->
        var name by remember { mutableStateOf(doc.name.ifBlank { tr("Figure") }) }
        val clash = remember(name) { FigureStore.list(context).firstOrNull { it.id != doc.id && it.name.trim().equals(name.trim(), ignoreCase = true) } }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { naming = null },
            title = { Text(tr(if (asNew) "Save as a new figure" else "Save figure")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, singleLine = true, label = { Text(tr("Name")) }, modifier = Modifier.fillMaxWidth())
                    if (clash != null) Text(tr("A saved figure is called “{0}”: saving replaces it.", clash.name), style = MaterialTheme.typography.bodySmall, color = colors.error)
                }
            },
            confirmButton = { TextButton(onClick = { naming = null; saveAs(name, asNew, confirmed = true, then = afterSave); afterSave = {} }) { Text(tr(if (clash != null) "Replace" else "Save")) } },
            dismissButton = { TextButton(onClick = { naming = null; afterSave = {} }) { Text(tr("Cancel")) } },
        )
    }
    overwrite?.let { (other, replace) ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { overwrite = null },
            title = { Text(tr("Replace “{0}”?", other.name)) },
            text = { Text(tr("A saved figure already has this name. Saving replaces it with this one.")) },
            confirmButton = { TextButton(onClick = { overwrite = null; replace() }) { Text(tr("Replace"), color = colors.error) } },
            dismissButton = { TextButton(onClick = { overwrite = null; naming = true }) { Text(tr("Save as new")) } },
        )
    }
    if (asking) androidx.compose.material3.AlertDialog(
        onDismissRequest = { asking = false },
        title = { Text(tr(if (savedDoc == null) "Save this figure?" else "Save changes?")) },
        text = { Text(tr(if (savedDoc == null) "It goes into Plots' saved figures." else "Your changes to this figure aren't saved yet.")) },
        confirmButton = { TextButton(onClick = { asking = false; quickSave(then = onClose) }) { Text(tr("Save")) } },
        dismissButton = {
            Row {
                TextButton(onClick = { asking = false; onClose() }) { Text(tr("Discard"), color = colors.error) }
                TextButton(onClick = { asking = false }) { Text(tr("Cancel")) }
            }
        },
    )

    // The open layer's inputs as text, typed on the phone's keyboard (read as math: sin(x), x^2, [1, 4, 9]).
    val sel = selected?.takeIf { it in axes.layers.indices }
    val layer = sel?.let { axes.layers[it] }
    var focus by remember { mutableStateOf<Int?>(null) }
    val texts = remember(axesAt, sel, layer?.kind) {
        androidx.compose.runtime.mutableStateListOf(*(layer?.kind?.inputs?.indices?.map { k -> TextFieldValue(com.example.cas.engine.PlainMath.text(layer.input(k))) } ?: emptyList()).toTypedArray())
    }
    LaunchedEffect(texts) { focus = null }
    // The figure's tables (files, sheets) and the one shown.
    val sheets = doc.sheets
    var sheetAt by remember { mutableIntStateOf(0) }
    if (sheetAt !in sheets.indices) sheetAt = 0
    val sheet = sheets[sheetAt]
    // Every table's column names, each shown in the inputs as one chip and deleted as one.
    val columnNames = remember(sheets.map { it.table.names }) { sheets.flatMap { sh -> sh.table.names.map { it.trim() } }.filter { Figure.isColumnName(it) } }
    fun typed(k: Int, v: TextFieldValue) {
        if (k !in texts.indices) return
        texts[k] = wholeColumns(texts[k], v, columnNames)
        val s0 = selected; val l = s0?.let { axes.layers.getOrNull(it) } ?: return
        setLayer(s0, l.copy(inputs = texts.map { com.example.cas.engine.PlainMath.parse(it.text) }), record = false)
    }
    // Math typed on the calculator's keypad with only its box in view: your own fitted model
    // (input −1), or a parametric or polar curve's inputs. (Layer, input.)
    var mathEdit by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var formulaVersion by remember { mutableIntStateOf(0) }
    val formulaEditor = remember(mathEdit, axesAt) {
        mathEdit?.let { (k, i) ->
            axes.layers.getOrNull(k)?.let { l -> com.example.cas.editor.Editor(if (i < 0) Figure.formulaRow(l.style.formula) else MathCodec.copy(l.input(i))) }
        }
    }
    formulaEditor?.onChange = {
        formulaVersion++
        val m = mathEdit; val l = m?.let { axes.layers.getOrNull(it.first) }
        if (m != null && l != null) {
            val (k, i) = m
            if (i < 0) setLayer(k, l.copy(style = l.style.copy(formula = Figure.formulaOf(formulaEditor.root))), record = false)
            else setLayer(k, l.copy(inputs = List(maxOf(l.inputs.size, i + 1)) { j -> if (j == i) MathCodec.copy(formulaEditor.root) else l.input(j) }), record = false)
        }
    }
    // Enter: a curve's next input, or done.
    val formulaKeys = remember(formulaEditor) {
        FieldKeys(target = { formulaEditor }, version = { formulaVersion }, onEnter = {
            val m = mathEdit; val l = m?.let { axes.layers.getOrNull(it.first) }
            mathEdit = if (m != null && l != null && m.second >= 0 && m.second + 1 < l.kind.inputs.size) m.first to m.second + 1 else null
        })
    }
    BackHandler { if (mathEdit != null) mathEdit = null else if (selected != null) selected = null else close() }
    // While the keyboard is up, the preview shrinks to a strip.
    val typing = focus != null && layer != null && androidx.compose.foundation.layout.WindowInsets.ime.getBottom(LocalDensity.current) > 0
    var tab by remember { mutableIntStateOf(0) }

    /** A column (of table [at]) put to use by its name (freq, Signal); a name that can't be typed (spaces, signs, none) is made into one first. */
    fun useColumn(c: Int, at: Int = sheetAt): String {
        val t = doc.sheets.getOrNull(at)?.table ?: return ""
        val name = t.names.getOrElse(c) { "" }.trim()
        if (Figure.isColumnName(name) && name != "e" && name != "i") return name
        val taken = doc.sheets.flatMap { sh -> sh.table.names.map { it.trim() } }.toSet()
        val base = Figure.columnName(name)
        val letter = (listOfNotNull(base.takeIf { it.length > 1 }) + (if (base.length > 1) (2..99).map { "${base}_$it" } else emptyList()) +
            listOf("x", "y", "u", "v", "w", "a", "b", "c", "d", "f", "g", "h", "k", "m", "n", "p", "q", "r", "s", "z"))
            .first { it !in taken && it != "e" && it != "i" }
        change(doc.withTable(at, com.example.cas.graph.DataTable(t.names.mapIndexed { k, n -> if (k == c) letter else n }, t.columns, t.x, t.y, t.sigmaX, t.sigmaY, t.formats, t.frozen, t.styles)))
        Toast.makeText(context, tr("Column “{0}” is now {1}, so it can be typed", name, letter), Toast.LENGTH_SHORT).show()
        return letter
    }
    /** A layer of [kind] made from columns [cols] (by position), added and opened. */
    fun addFromColumns(kind: Figure.Kind, cols: List<Int>) {
        val names = cols.map { useColumn(it) }
        val r = { n: String -> com.example.cas.engine.PlainMath.parse(n) }
        val l = when (kind) {
            Figure.Kind.Scatter, Figure.Kind.Fit, Figure.Kind.Hist2D, Figure.Kind.Hexbin -> Figure.Layer(kind, names.map(r))
            Figure.Kind.Plot, Figure.Kind.Step, Figure.Kind.Stem -> Figure.Layer(kind, listOf(r(names.first()), r(names.last())), style = Figure.Style(label = names.last()))
            Figure.Kind.Box, Figure.Kind.Violin, Figure.Kind.Events -> Figure.Layer(kind, listOf(r(names.joinToString(","))), text = names.joinToString(", "))
            else -> Figure.Layer(kind, listOf(r(names.first())), style = Figure.Style(label = names.first()))
        }
        val now = doc.axes.getOrElse(axesAt) { Figure.Axes() }
        change(doc.copy(axes = doc.axes.mapIndexed { k, a -> if (k == axesAt) a.copy(layers = a.layers + l) else a }.ifEmpty { listOf(Figure.Axes(listOf(l))) }))
        tab = 0; selected = now.layers.size
    }

    // Deleting a layer (swiped away, or its Delete): asks first when Settings say so.
    var deleting by remember { mutableStateOf<Int?>(null) }
    fun deleteLayer(k: Int) {
        val now = doc.axes.getOrElse(axesAt) { Figure.Axes() }
        if (k !in now.layers.indices) return
        setAxes(now.copy(layers = now.layers.filterIndexed { i, _ -> i != k }))
        selected = null
    }
    fun askDelete(k: Int) { if (AppSettings.confirmDeleteEntry) deleting = k else deleteLayer(k) }
    deleting?.let { k ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(tr("Delete this layer?")) },
            text = { Text(tr(axes.layers.getOrNull(k)?.kind?.title ?: "Layer")) },
            confirmButton = { TextButton(onClick = { deleting = null; deleteLayer(k) }) { Text(tr("Delete"), color = colors.error) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(tr("Cancel")) } },
        )
    }

    val pageDone = { page = FigurePage.None }
    // The figure as it was when a settings page opened: Cancel puts it back.
    val pageBefore = remember(page) { doc }
    val cancelPage = { doc = pageBefore; axesAt = axesAt.coerceAtMost(pageBefore.axes.lastIndex); pageDone() }
    when (page) {
        FigurePage.Gallery -> LayerGallery(onPick = { kind ->
            page = FigurePage.None
            val l = newLayer(kind)
            setAxes(axes.copy(layers = axes.layers + l))
            selected = axes.layers.size
        }, onDismiss = pageDone)
        // As the graphs' settings: changes show at once; Cancel puts back how it was.
        // On a tablet they take the place of the layers, beside the figure (see Controls); on a phone, a dialog.
        FigurePage.Axes -> if (!tablet) GraphSettingsFrame("", true, onDone = pageDone, onDismiss = cancelPage, title = "Axes") { AxesSettings(axes) { setAxes(it) } }
        FigurePage.Figure -> if (!tablet) GraphSettingsFrame("", true, onDone = pageDone, onDismiss = cancelPage, title = "Figure") {
            FigureSettings(doc) { d -> change(d); axesAt = axesAt.coerceAtMost(d.axes.lastIndex) }
        }
        FigurePage.Python -> PythonPage(doc, built, onBack = pageDone)
        FigurePage.None -> Unit
    }
    if (dataOpen) DataTableEditor(sheet.table, key = doc.id + "data" + sheetAt, onSave = { change(doc.withTable(sheetAt, it)) }, onDismiss = { dataOpen = false }, roles = false)

    // Tables from files: CSV, Excel (.xlsx, .xls), OpenDocument (.ods), several at once; each sheet of a workbook its own table.
    val scope = rememberCoroutineScope()
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val added = withContext(Dispatchers.IO) {
                uris.flatMap { uri ->
                    runCatching {
                        val file = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
                            ?.substringBeforeLast('.') ?: "Table"
                        val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                        val book = com.example.cas.graph.Spreadsheet.sheets(bytes)?.filter { it.table.columns.isNotEmpty() }
                        book?.map { sh -> Figure.Sheet(if (book.size > 1) sh.name.ifBlank { file } else file, com.example.cas.graph.DataTable.fromCsv(sh.table)) }
                            ?: listOf(Figure.Sheet(file, com.example.cas.graph.DataTable.fromCsv(com.example.cas.graph.Csv.parse(bytes.toString(Charsets.UTF_8)))))
                    }.getOrDefault(emptyList())
                }
            }
            if (added.isEmpty()) { Toast.makeText(context, tr("Couldn't read that file as a table"), Toast.LENGTH_LONG).show(); return@launch }
            val before = doc.sheets.size
            val next = Figure.addSheets(doc, added)
            change(next)
            sheetAt = (next.sheets.size - added.size).coerceAtLeast(0)
            Toast.makeText(context, if (added.size == 1) tr("Added the table “{0}”", added[0].name) else tr("Added {0} tables", added.size), Toast.LENGTH_SHORT).show()
            if (before == 0) sheetAt = 0
        }
    }
    val importFiles = {
        importer.launch(arrayOf(
            "text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream", "application/zip",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel.sheet.macroEnabled.12",
            "application/vnd.oasis.opendocument.spreadsheet", "application/x-msexcel",
        ))
    }
    /** The tables as tabs: tap one to show it; hold it to rename or remove it; + for more files. */
    val sheetTabs: @Composable () -> Unit = {
        SheetTabs(sheets, sheetAt, onPick = { sheetAt = it }, onImport = importFiles,
            onRename = { k, name -> change(doc.withSheets(doc.sheets.mapIndexed { i, sh -> if (i == k) Figure.Sheet(name, sh.table) else sh })) },
            onRemove = { k -> change(doc.withSheets(doc.sheets.filterIndexed { i, _ -> i != k })); sheetAt = 0 })
    }

    val ratio = (1 / doc.aspect).toFloat().coerceIn(0.5f, 3f)

    /**
     * The figure, fitted into its box. With several axes: tap one to edit it (it's outlined), and
     * drag a line between them to make columns or rows wider or taller.
     */
    @Composable
    fun Preview(modifier: Modifier, compact: Boolean = false, fill: Boolean = false) {
        val density = LocalDensity.current
        val latest = androidx.compose.runtime.rememberUpdatedState(doc)
        val latestBuilt = androidx.compose.runtime.rememberUpdatedState(built)
        Box((if (compact) modifier.height(170.dp) else if (fill) modifier.aspectRatio(ratio, matchHeightConstraintsFirst = true) else modifier.aspectRatio(ratio)).clip(RoundedCornerShape(20.dp)).background(Color(if (doc.theme == 0) theme.background else Figure.THEMES.getOrElse(doc.theme) { theme }.background))) {
            FigureCanvas(built?.scene, Modifier.fillMaxSize())
            // The figure (as built, so it matches what's drawn) to the screen, as FigureCanvas fits it.
            class Fit(val k: Float, val dx: Float, val dy: Float, val sw: Float, val sh: Float)
            fun fit(w: Float, h: Float): Fit {
                val sc = latestBuilt.value?.scene
                val sw = sc?.width?.toFloat() ?: 480f; val sh = sc?.height?.toFloat() ?: (480f * latest.value.aspect.coerceIn(0.3, 2.0).toFloat())
                val k = minOf(w / sw, h / sh)
                return Fit(k, (w - sw * k) / 2, (h - sh * k) / 2, sw, sh)
            }
            var dragging by remember { mutableStateOf<Pair<Boolean, Int>?>(null) }
            // Something in the figure being moved by the finger: a note (or its arrow's tip), a line or band
            // (or one edge of a band), or the legend. [inputs] are the layer's inputs it changes, each along x
            // (true) or y, and where each was on the figure when the drag began.
            class Moving(val layer: Int, val inputs: List<Pair<Int, Boolean>>, val starts: DoubleArray, val start: Offset, val worked: Figure.Worked,
                         val legend: DoubleArray? = null, var moved: Offset = Offset.Zero)
            var moving by remember { mutableStateOf<Moving?>(null) }
            /** What's under the finger at [p] (the box being [w] × [h]): something to move, or null. */
            fun grab(p: Offset, w: Float, h: Float, reach: Float, grabR: Float): Moving? {
                val d = latest.value
                val f = fit(w, h)
                val wk = latestBuilt.value?.worked?.getOrNull(axesAt) ?: return null
                val ax = d.axes.getOrNull(axesAt) ?: return null
                if (wk.frame.size < 8) return null
                fun sx(fx: Double) = f.dx + fx.toFloat() * f.k
                fun sy(fy: Double) = f.dy + fy.toFloat() * f.k
                val left = sx(wk.frame[0]); val top = sy(wk.frame[1]); val right = sx(wk.frame[2]); val bottom = sy(wk.frame[3])
                // Notes and arrow tips first (they're small), the nearest.
                var best: Moving? = null; var bestD = grabR
                ax.layers.forEachIndexed { k, l ->
                    if (!l.visible || (l.kind != Figure.Kind.Text && l.kind != Figure.Kind.Annotate)) return@forEachIndexed
                    val v = wk.drawn.firstOrNull { it.layer === l }?.values ?: return@forEachIndexed
                    for (i in if (l.kind == Figure.Kind.Text) listOf(0) else listOf(2, 0)) {
                        if (i + 1 >= v.size) continue
                        val (fx, fy) = wk.toFigure(v[i], v[i + 1]) ?: continue
                        val q = Offset(sx(fx), sy(fy))
                        val dd = (q - p).getDistance()
                        if (dd < bestD) { bestD = dd; best = Moving(k, listOf(i to true, i + 1 to false), doubleArrayOf(fx, fy), p, wk) }
                    }
                }
                if (best != null) return best
                // The legend, anywhere on its box.
                val lb = wk.legendBox
                if (ax.legend >= 0 && lb.size == 4 && p.x in sx(lb[0])..sx(lb[0] + lb[2]) && p.y in sy(lb[1])..sy(lb[1] + lb[3]))
                    return Moving(-1, emptyList(), DoubleArray(0), p, wk, legend = lb.copyOf())
                // Lines and bands: a line near the finger; a band by an edge (that edge) or inside (all of it).
                if (p.x < left - reach || p.x > right + reach || p.y < top - reach || p.y > bottom + reach) return null
                ax.layers.forEachIndexed { k, l ->
                    if (!l.visible) return@forEachIndexed
                    val dr = wk.drawn.firstOrNull { it.layer === l && it.error == null } ?: return@forEachIndexed
                    val across = l.kind == Figure.Kind.VLine || l.kind == Figure.Kind.VSpan
                    // A place along this layer's axis on screen.
                    fun at(v: Double): Float? {
                        val (fx, fy) = (if (across) wk.toFigure(v, wk.frame[6]) else wk.toFigure(wk.frame[4], v)) ?: return null
                        return if (across) sx(fx) else sy(fy)
                    }
                    val finger = if (across) p.x else p.y
                    when (l.kind) {
                        Figure.Kind.HLine, Figure.Kind.VLine -> {
                            if (dr.values.size != 1) return@forEachIndexed
                            val q = at(dr.values[0]) ?: return@forEachIndexed
                            if (abs(q - finger) < reach) return Moving(k, listOf(0 to across), doubleArrayOf(figureAt(wk, dr.values[0], across) ?: return@forEachIndexed), p, wk)
                        }
                        Figure.Kind.HSpan, Figure.Kind.VSpan -> {
                            // Which input holds the low end and which the high one.
                            val typed = (0..1).map { com.example.cas.engine.PlainMath.text(l.input(it)).toDoubleOrNull() }
                            val (iLo, iHi) = if (typed[0] != null && typed[1] != null && typed[0]!! > typed[1]!!) 1 to 0 else 0 to 1
                            val lo = dr.values[0]; val hi = dr.values[1]
                            val qLo = at(lo) ?: return@forEachIndexed; val qHi = at(hi) ?: return@forEachIndexed
                            val fLo = figureAt(wk, lo, across) ?: return@forEachIndexed; val fHi = figureAt(wk, hi, across) ?: return@forEachIndexed
                            when {
                                abs(qLo - finger) < reach * 0.7f -> return Moving(k, listOf(iLo to across), doubleArrayOf(fLo), p, wk)
                                abs(qHi - finger) < reach * 0.7f -> return Moving(k, listOf(iHi to across), doubleArrayOf(fHi), p, wk)
                                finger in minOf(qLo, qHi)..maxOf(qLo, qHi) -> return Moving(k, listOf(iLo to across, iHi to across), doubleArrayOf(fLo, fHi), p, wk)
                            }
                        }
                        else -> Unit
                    }
                }
                return null
            }
            /** [m] moved by the finger's [total] offset: its inputs rewritten as rounded numbers (the open layer's fields follow), or the legend placed. */
            fun follow(m: Moving, total: Offset, w: Float, h: Float) {
                val d = latest.value
                val f = fit(w, h)
                val ax = d.axes.getOrNull(axesAt) ?: return
                val fr = m.worked.frame
                if (m.legend != null) {
                    val nx = (m.legend[0] + total.x / f.k - fr[0]) / (fr[2] - fr[0]); val ny = (m.legend[1] + total.y / f.k - fr[1]) / (fr[3] - fr[1])
                    val a = ax.copy(legend = Figure.LEGEND_FREE, legendX = nx.coerceIn(0.0, 1.0), legendY = ny.coerceIn(0.0, 1.0))
                    change(d.copy(axes = d.axes.mapIndexed { k, x -> if (k == axesAt) a else x }), record = false)
                    return
                }
                val l = ax.layers.getOrNull(m.layer) ?: return
                fun round(v: Double, lo: Double, hi: Double): String {
                    val step = abs(hi - lo) / 400
                    val places = if (step > 0) (-kotlin.math.floor(kotlin.math.log10(step))).toInt().coerceIn(0, 8) else 2
                    return java.math.BigDecimal(v).setScale(places, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
                }
                val put = HashMap<Int, String>()
                m.inputs.forEachIndexed { j, (input, across) ->
                    val fig = m.starts[j] + (if (across) total.x else total.y) / f.k
                    val (x, y) = (if (across) m.worked.toData(fig, fr[3]) else m.worked.toData(fr[0], fig)) ?: return
                    put[input] = if (across) round(x, fr[4], fr[5]) else round(y, fr[6], fr[7])
                }
                val ins = List(maxOf(l.inputs.size, (put.keys.maxOrNull() ?: 0) + 1)) { j -> put[j]?.let { com.example.cas.engine.PlainMath.parse(it) } ?: l.input(j) }
                change(d.copy(axes = d.axes.mapIndexed { k, a -> if (k == axesAt) a.copy(layers = a.layers.mapIndexed { i, x2 -> if (i == m.layer) l.copy(inputs = ins) else x2 }) else a }), record = false)
                if (selected == m.layer) put.forEach { (j, v) -> if (j < texts.size) texts[j] = TextFieldValue(v) }
            }
            Canvas(Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { p ->
                        if (Figure.axesCount(latest.value.layout) < 2) return@detectTapGestures
                        val f = fit(size.width.toFloat(), size.height.toFloat())
                        val sx = (p.x - f.dx) / f.k; val sy = (p.y - f.dy) / f.k
                        val hit = Figure.cellBoxes(latest.value).indexOfFirst { b -> sx >= b[0] && sx <= b[0] + b[2] && sy >= b[1] && sy <= b[1] + b[3] }
                        if (hit >= 0) { axesAt = hit; selected = null }
                    }
                }
                .pointerInput(Unit) {
                    val reach = with(density) { 22.dp.toPx() }
                    val grabR = with(density) { 36.dp.toPx() }
                    // Only a drag that starts on something movable is taken: anywhere else, the page scrolls.
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val w = size.width.toFloat(); val h = size.height.toFloat()
                        val d = latest.value
                        val target = grab(down.position, w, h, reach, grabR)
                        var divider: Pair<Boolean, Int>? = null
                        if (target == null && Figure.axesCount(d.layout) >= 2) {
                            val f = fit(w, h); val p = down.position
                            val (xe, ye) = Figure.edges(d)
                            val top = Figure.titleSpace(d).toFloat()
                            val col = (1 until xe.size - 1).minByOrNull { abs(f.dx + xe[it].toFloat() * f.sw * f.k - p.x) }?.takeIf { abs(f.dx + xe[it].toFloat() * f.sw * f.k - p.x) < reach }
                            val row = (1 until ye.size - 1).minByOrNull { abs(f.dy + (top + ye[it].toFloat() * (f.sh - top)) * f.k - p.y) }?.takeIf { abs(f.dy + (top + ye[it].toFloat() * (f.sh - top)) * f.k - p.y) < reach }
                            divider = when { col != null -> true to col; row != null -> false to row; else -> null }
                        }
                        if (target == null && divider == null) return@awaitEachGesture
                        val first = awaitTouchSlopOrCancellation(down.id) { ch, _ -> ch.consume() } ?: return@awaitEachGesture
                        change(d, record = true)
                        if (target != null) moving = target else dragging = divider
                        var total = first.position - down.position
                        if (target != null) { target.moved = total; follow(target, total, w, h) }
                        drag(first.id) { ch ->
                            val delta = ch.position - ch.previousPosition
                            ch.consume()
                            total += delta
                            val m = moving
                            if (m != null) { m.moved = total; follow(m, total, w, h); return@drag }
                            val (across, j) = dragging ?: return@drag
                            val dd = latest.value
                            val f = fit(w, h)
                            val (xe, ye) = Figure.edges(dd)
                            val e = (if (across) xe else ye).copyOf()
                            val span = if (across) f.sw * f.k else (f.sh - Figure.titleSpace(dd).toFloat()) * f.k
                            e[j] = (e[j] + (if (across) delta.x else delta.y) / span).coerceIn(e[j - 1] + 0.12, e[j + 1] - 0.12)
                            val shares = (1 until e.size).map { e[it] - e[it - 1] }
                            change(if (across) dd.copy(widths = shares) else dd.copy(heights = shares), record = false)
                        }
                        moving = null; dragging = null
                    }
                }) {
                val f = fit(size.width, size.height)
                val d = doc
                // With several axes, the open one outlined, inside its cell so no edge is cut off.
                if (Figure.axesCount(d.layout) > 1) Figure.cellBoxes(d, f.sw.toDouble()).getOrNull(axesAt)?.let { b ->
                    val inset = 5.dp.toPx(); val sw = 2.dp.toPx()
                    val l = f.dx + b[0].toFloat() * f.k + inset; val t = f.dy + b[1].toFloat() * f.k + inset
                    val r = f.dx + (b[0] + b[2]).toFloat() * f.k - inset; val btm = f.dy + (b[1] + b[3]).toFloat() * f.k - inset
                    if (r > l && btm > t) drawRoundRect(colors.primary, Offset(l, t), androidx.compose.ui.geometry.Size(r - l, btm - t),
                        androidx.compose.ui.geometry.CornerRadius(14.dp.toPx(), 14.dp.toPx()), style = Stroke(sw, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
                }
                // While a line between cells is dragged, a mark on it (nothing otherwise: drag anywhere near the line).
                val (xe, ye) = Figure.edges(d)
                val top = Figure.titleSpace(d).toFloat()
                val long = 28.dp.toPx(); val thick = 5.dp.toPx()
                for (j in 1 until xe.size - 1) {
                    val x = f.dx + xe[j].toFloat() * f.sw * f.k; val y = f.dy + (top + (f.sh - top) / 2) * f.k
                    if (dragging == (true to j)) drawRoundRect(colors.primary, Offset(x - thick / 2, y - long / 2), androidx.compose.ui.geometry.Size(thick, long), androidx.compose.ui.geometry.CornerRadius(thick, thick))
                }
                for (j in 1 until ye.size - 1) {
                    val y = f.dy + (top + ye[j].toFloat() * (f.sh - top)) * f.k; val x = f.dx + f.sw / 2 * f.k
                    if (dragging == (false to j)) drawRoundRect(colors.primary, Offset(x - long / 2, y - thick / 2), androidx.compose.ui.geometry.Size(long, thick), androidx.compose.ui.geometry.CornerRadius(thick, thick))
                }
                // A text being moved, ringed.
                moving?.let { m ->
                    // The legend outlined; anything else ringed under the finger.
                    val lb = m.legend
                    if (lb != null) drawRoundRect(colors.primary, Offset(f.dx + (lb[0].toFloat() * f.k) + m.moved.x, f.dy + lb[1].toFloat() * f.k + m.moved.y),
                        androidx.compose.ui.geometry.Size(lb[2].toFloat() * f.k, lb[3].toFloat() * f.k), androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()), style = Stroke(2.dp.toPx()))
                    else drawCircle(colors.primary, 14.dp.toPx(), m.start + m.moved, style = Stroke(2.dp.toPx()))
                }
            }
        }
    }

    @Composable
    fun Controls(modifier: Modifier) {
        Box(modifier) {
            if (tablet && (page == FigurePage.Axes || page == FigurePage.Figure)) {
                // The axes' or figure's settings beside the figure, which shows each change at once.
                SettingsPane(if (page == FigurePage.Axes) "Axes" else "Figure", onDone = pageDone, onCancel = cancelPage) {
                    if (page == FigurePage.Axes) AxesSettings(axes) { setAxes(it) }
                    else FigureSettings(doc) { d -> change(d); axesAt = axesAt.coerceAtMost(d.axes.lastIndex) }
                }
            } else if (layer != null && sel != null) {
                LayerEditor(
                    layer, built?.error(axesAt, layer), built?.worked?.getOrNull(axesAt)?.drawn?.firstOrNull { it.layer === layer }?.color,
                    table = sheet.table, sheets = sheets, sheetAt = sheetAt, sheetTabs = sheetTabs,
                    onColumn = { at, c ->
                        // Put in where the cursor is (the last input typed in, or the first empty one).
                        val name = useColumn(c, at)
                        val k = focus ?: texts.indexOfFirst { it.text.isBlank() }.takeIf { it >= 0 } ?: 0
                        if (k in texts.indices) {
                            val v = texts[k]
                            val a = v.selection.min.coerceIn(0, v.text.length); val b = v.selection.max.coerceIn(0, v.text.length)
                            val before = v.text.substring(0, a); val after = v.text.substring(b)
                            fun word(ch: Char?) = ch != null && (ch.isLetterOrDigit() || ch == '_')
                            val lead = when { l0isMulti(layer) && before.isNotBlank() && !before.trimEnd().endsWith(",") -> ", "; word(before.lastOrNull()) -> " "; else -> "" }
                            val trail = if (word(after.firstOrNull())) " " else ""
                            val put = lead + name + trail
                            typed(k, TextFieldValue(before + put + after, TextRange(a + put.length)))
                        }
                    },
                    columns = columnNames,
                    onEditMath = { i -> mathEdit = sel to i },
                    fitted = built?.worked?.getOrNull(axesAt)?.drawn?.firstOrNull { it.layer === layer }?.takeIf { it.error == null }?.values,
                    onEditData = { dataOpen = true },
                    onImport = importFiles,
                    texts = texts, onText = ::typed, onFocus = { k -> focus = k },
                    onChange = { setLayer(sel, it) },
                    onClose = { selected = null },
                    onMove = { d ->
                        val to = sel + d
                        if (to in axes.layers.indices) {
                            setAxes(axes.copy(layers = axes.layers.toMutableList().apply { add(to, removeAt(sel)) }))
                            selected = to
                        }
                    },
                    onDuplicate = { setAxes(axes.copy(layers = axes.layers.toMutableList().apply { add(sel + 1, layer.copy()) })); selected = sel + 1 },
                    onDelete = { askDelete(sel) },
                )
            } else {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        EditorChip(FigureIcons.Axes, "Axes") { page = FigurePage.Axes }
                        EditorChip(FigureIcons.Layout, "Figure") { page = FigurePage.Figure }
                        EditorChip(FigureIcons.Python, "Python") { page = FigurePage.Python }
                    }
                    // Layers, or the figure's whole table.
                    androidx.compose.material3.SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                        listOf(tr("Layers") + " (${axes.layers.size})", tr("Data") + if (sheets.size > 1) " (${sheets.size})" else " (${sheet.table.rowCount} × ${sheet.table.names.size})").forEachIndexed { k, name ->
                            SegmentedButton(selected = tab == k, onClick = { tab = k }, shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(k, 2), icon = {}, label = { Text(name, maxLines = 1) })
                        }
                    }
                    if (tab == 1) DataPanel(sheet.table, Modifier.weight(1f).fillMaxWidth(), sheetTabs,
                        onEdit = { dataOpen = true },
                        onImport = importFiles,
                        onAdd = ::addFromColumns,
                        onPlotAll = {
                            val t = sheet.table
                            val xs = useColumn(0)
                            val ys = (1 until t.names.size).map { useColumn(it) }
                            val ls = ys.map { y -> Figure.Layer(Figure.Kind.Scatter, listOf(com.example.cas.engine.PlainMath.parse(xs), com.example.cas.engine.PlainMath.parse(y)), style = Figure.Style(label = y)) }
                            change(doc.copy(axes = doc.axes.mapIndexed { k, a -> if (k == axesAt) a.copy(layers = a.layers + ls, xLabel = a.xLabel.ifBlank { xs }) else a }))
                            tab = 0
                        },
                    ) else
                    // The first layer is drawn on top. Drag a layer by its handle to move it; swipe it either way to delete it.
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp)) {
                        key(axesAt) {
                            ReorderableColumn(axes.layers, key = { it }, onMove = { from, to ->
                                val now = doc.axes.getOrElse(axesAt) { Figure.Axes() }
                                if (from in now.layers.indices && to in now.layers.indices) setAxes(now.copy(layers = now.layers.toMutableList().apply { add(to, removeAt(from)) }))
                            }) { l, handle ->
                                val k = axes.layers.indexOf(l)
                                val dr = built?.worked?.getOrNull(axesAt)?.drawn?.firstOrNull { it.layer === l }
                                SwipeToRemove(onRemove = { askDelete(axes.layers.indexOf(l)) }, asks = { AppSettings.confirmDeleteEntry }) {
                                    LayerRow(l, dr?.color, built?.error(axesAt, l), handle, columnNames, onOpen = { selected = axes.layers.indexOf(l) }, onToggle = { if (k >= 0) setLayer(k, l.copy(visible = !l.visible)) })
                                }
                            }
                        }
                        if (axes.layers.isEmpty()) { PxFootnote("Add a layer with +: a line, points, bars, a histogram, a box plot, contours… Its inputs are math: an expression in \$x\$, a list like [1, 4, 9], or a column of the figure's table (sin(y) takes the sine of each number in column y).") }
                    }
                }
            }
        }
    }

    // Each column of the table keeps one color everywhere: its chips in inputs and lists, and its name in the table.
    val columnColors = remember(sheets.map { it.table.names }, colors) { val pal = plotColors(colors); sheets.flatMap { it.table.names }.mapIndexed { k, n -> n.trim() to pal[k % pal.size] }.toMap() }
    androidx.compose.runtime.CompositionLocalProvider(LocalColumnColors provides columnColors) {
    Column(Modifier.fillMaxSize()) {
        // The bar: back to the library, the name, undo and export.
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(colors.surfaceContainerHigh).clickable(onClick = { if (selected != null) selected = null else close() }), contentAlignment = Alignment.Center) {
                AppIcon(TableIcons.Back, contentDescription = tr("Back"), tint = colors.onSurface)
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) {
                if (doc.name.isEmpty()) Text(tr("Untitled figure"), style = MaterialTheme.typography.titleLarge, color = colors.outline)
                BasicTextField(doc.name, { change(doc.copy(name = it), record = false) }, singleLine = true, textStyle = MaterialTheme.typography.titleLarge.copy(color = colors.onSurface),
                    cursorBrush = SolidColor(colors.primary), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), modifier = Modifier.fillMaxWidth())
            }
            val canUndo = undoCount > 0
            IconButton(enabled = canUndo, onClick = { undo.removeLastOrNull()?.let { doc = it; undoCount = undo.size; selected = null } }) {
                AppIcon(TableIcons.Undo, contentDescription = tr("Undo"), tint = if (canUndo) colors.onSurface else colors.outline)
            }
            // Save, Save as: the figure's name, or a new one; a name already saved is replaced (asked first).
            if (!autosave) Box {
                var saveMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { saveMenu = true }) {
                    AppIcon(TableIcons.Save, contentDescription = tr("Save"), tint = if (dirty) colors.primary else colors.onSurfaceVariant)
                }
                DropdownMenu(expanded = saveMenu, onDismissRequest = { saveMenu = false }) {
                    DropdownMenuItem(text = { Text(tr("Save")) }, enabled = dirty, leadingIcon = { AppIcon(TableIcons.Save, null) }, onClick = { saveMenu = false; quickSave() })
                    DropdownMenuItem(text = { Text(tr("Save as…")) }, leadingIcon = { AppIcon(TableIcons.Copy, null) }, onClick = { saveMenu = false; naming = true })
                }
            }
            ExportMenu(doc, built)
        }
        val fe = formulaEditor
        if (fe != null) {
            // Math on the keypad: the figure, the one box being typed in, and the keypad; nothing else.
            val (mk, mi) = mathEdit ?: (0 to -1)
            val mathLayer = axes.layers.getOrNull(mk)
            val label = if (mi < 0) tr("Your own model") else mathLayer?.kind?.inputs?.getOrNull(mi) ?: ""
            val prefix = if (mi < 0) "y =" else null
            if (tablet) Row(Modifier.weight(1f).fillMaxWidth()) {
                // The figure takes the room the box being typed in leaves, so the box is always in view.
                Column(Modifier.weight(1f).fillMaxHeight().padding(12.dp)) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Preview(Modifier, fill = true) }
                    FormulaBox(fe, formulaVersion, label, prefix, mathLayer?.let { built?.error(axesAt, it) }) { mathEdit = null }
                }
                Keypad(formulaKeys, Modifier.width(tabletKeypadWidth()).fillMaxHeight().background(colors.surfaceContainerLow), tablet = true)
            } else {
                Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) { Preview(Modifier, fill = true) }
                FormulaBox(fe, formulaVersion, label, prefix, mathLayer?.let { built?.error(axesAt, it) }) { mathEdit = null }
                if (formulaKeys.keypadHidden) Box(Modifier.fillMaxWidth()) { ShowKeypadButton(onClick = { formulaKeys.keypadHidden = false }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) }
                else Keypad(formulaKeys)
            }
        } else if (tablet) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.weight(1.3f).fillMaxHeight().padding(12.dp), verticalArrangement = Arrangement.Center) { Preview(Modifier.fillMaxWidth()) }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    Controls(Modifier.fillMaxSize())
                    if (layer == null && tab == 0 && page != FigurePage.Axes && page != FigurePage.Figure) AddLayerButton(Modifier.align(Alignment.BottomEnd).padding(20.dp)) { page = FigurePage.Gallery }
                }
            }
        } else {
            // While typing, a short strip, so the inputs stay in view. A tall figure (several axes, a square
            // shape) would leave little room under it: then the page scrolls, the figure going up out of the way.
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val figureHeight = (maxWidth - 24.dp) / ratio
                val viewport = maxHeight
                val scrolls = !typing && figureHeight > viewport * 0.45f
                // One layout either way (so a field being typed in keeps its place and focus).
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState(), enabled = scrolls)) {
                    Preview(Modifier.padding(horizontal = 12.dp).fillMaxWidth(), compact = typing)
                    Controls(Modifier.fillMaxWidth().height(if (scrolls) viewport else (viewport - if (typing) 170.dp else figureHeight).coerceAtLeast(120.dp)))
                }
                if (layer == null && tab == 0) AddLayerButton(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp)) { page = FigurePage.Gallery }
            }
        }
    }
    }
}

/** Where value [v] of an axes is across the figure: its x ([across]) or its y, in the figure's units. */
private fun figureAt(w: Figure.Worked, v: Double, across: Boolean): Double? =
    if (across) w.toFigure(v, w.frame[6])?.first else w.toFigure(w.frame[4], v)?.second

/** A settings page in the editor's side (tablets): its title with Cancel and Done, and its rows scrolled under them. */
@Composable
private fun SettingsPane(title: String, onDone: () -> Unit, onCancel: () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().padding(end = 12.dp, bottom = 12.dp).clip(RoundedCornerShape(28.dp)).background(colors.surfaceContainerHigh)) {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr(title), style = MaterialTheme.typography.headlineSmall, color = colors.onSurface, modifier = Modifier.weight(1f))
            TextButton(onClick = onCancel) { Text(tr("Cancel")) }
            TextButton(onClick = onDone) { Text(tr("Done")) }
        }
        androidx.compose.runtime.CompositionLocalProvider(LocalCompactRows provides true) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)) { content() }
        }
    }
}

/** The table's columns' colors by name (set by the figure editor). */
internal val LocalColumnColors = androidx.compose.runtime.compositionLocalOf { emptyMap<String, Color>() }

/** A column's chip colors: its own color (the icon half), a tone of it (the name's half) and the ink on each. */
private class ChipColors(val strong: Color, val onStrong: Color, val tonal: Color, val ink: Color)

@Composable
private fun chipColors(name: String): ChipColors {
    val colors = MaterialTheme.colorScheme
    val strong = LocalColumnColors.current[name.trim()] ?: colors.primary
    val dark = colors.surface.luminance() < 0.5f
    val onStrong = if (strong.luminance() > 0.45f) Color(0xFF1B1B12) else Color.White
    val tonal = strong.copy(alpha = if (dark) 0.26f else 0.18f).compositeOver(colors.surfaceContainerHigh)
    val ink = if (dark) androidx.compose.ui.graphics.lerp(strong, Color.White, 0.25f) else androidx.compose.ui.graphics.lerp(strong, Color.Black, 0.45f)
    return ChipColors(strong, onStrong, tonal, ink)
}

/** A math input shown as math; tapped, it's typed on the calculator's keypad. */
@Composable
private fun MathInputBox(label: String, row: MathRow, onEdit: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp, bottom = 4.dp))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainerHighest).clickable(onClickLabel = tr("Edit"), onClick = onEdit).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                if (row.items.isEmpty()) Text(tr("empty"), style = MaterialTheme.typography.bodyMedium, color = colors.outline) else MathView(row, 20.sp, colors.onSurface)
            }
            AppIcon(TableIcons.Edit, contentDescription = null, tint = colors.onSurfaceVariant)
        }
    }
}

/** The figure's tables as tabs (a file's or a workbook sheet's name): tap to show one, hold to rename or remove it, + to add files. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SheetTabs(sheets: List<Figure.Sheet>, at: Int, onPick: (Int) -> Unit, onImport: () -> Unit, onRename: (Int, String) -> Unit, onRemove: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf<Int?>(null) }
    var renaming by remember { mutableStateOf<Int?>(null) }
    renaming?.let { k ->
        var name by remember(k) { mutableStateOf(sheets.getOrNull(k)?.name.orEmpty()) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(tr("Rename table")) },
            text = { OutlinedTextField(name, { name = it }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { onRename(k, name.trim()); renaming = null }) { Text(tr("Done")) } },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(tr("Cancel")) } },
        )
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        sheets.forEachIndexed { k, sh ->
            val on = k == at
            Box {
                Row(
                    Modifier.height(36.dp).clip(RoundedCornerShape(12.dp)).background(if (on) colors.secondaryContainer else colors.surfaceContainerHigh)
                        .combinedClickable(onClick = { onPick(k) }, onLongClick = { menu = k }).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(TableIcons.Table, contentDescription = null, tint = if (on) colors.onSecondaryContainer else colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(sh.name.ifBlank { tr("Table") + " ${k + 1}" }, style = TextStyle(fontFamily = CasFonts.Maple, fontSize = 13.sp, color = if (on) colors.onSecondaryContainer else colors.onSurface), maxLines = 1)
                }
                DropdownMenu(expanded = menu == k, onDismissRequest = { menu = null }) {
                    DropdownMenuItem(text = { Text(tr("Rename")) }, onClick = { menu = null; renaming = k })
                    if (sheets.size > 1) DropdownMenuItem(text = { Text(tr("Remove"), color = colors.error) }, onClick = { menu = null; onRemove(k) })
                }
            }
        }
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(colors.surfaceContainerHigh).clickable(onClickLabel = tr("Add tables from files"), onClick = onImport), contentAlignment = Alignment.Center) {
            AppIcon(TableIcons.Add, contentDescription = tr("Add tables from files"), tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

/** Your own fitted model being typed on the keypad: y = its formula, with the cursor; any error under it. */
@Composable
private fun FormulaBox(e: com.example.cas.editor.Editor, version: Int, label: String, prefix: String?, error: String?, onDone: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = colors.primary, modifier = Modifier.weight(1f))
            TextButton(onClick = onDone) { Text(tr("Done")) }
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainerHighest).border(2.dp, colors.primary, RoundedCornerShape(16.dp))
                .clickable { e.setCursor(e.root, e.root.items.size) }.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (prefix != null) {
                MathView(MathRow(prefix.split(" ").filter { it.isNotEmpty() }.map { Sym(it) }.toMutableList()), 22.sp, colors.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
            }
            Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                MathView(e.root, 22.sp, colors.onSurface, accent = colors.primary, cursorRow = e.row, cursorIndex = e.index, version = version, onTap = { r, i -> e.setCursor(r, i) })
            }
        }
        if (error != null) Text(tr(error), style = MaterialTheme.typography.bodySmall, color = colors.error, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
    }
}

/**
 * The dialog's window makes room for the phone's keyboard and lets it come up: some tablets
 * otherwise keep it hidden over a full-screen dialog.
 */
@Composable
private fun KeyboardResizes() {
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.SideEffect {
        (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED,
        )
    }
}

/** A new layer of [kind], with inputs that show something at once where that's natural. */
private fun newLayer(kind: Figure.Kind): Figure.Layer {
    fun m(t: String) = MathRow(t.map { Sym(it.toString()) }.toMutableList())
    val inputs = when (kind) {
        Figure.Kind.Parametric -> listOf(MathRow(mutableListOf(com.example.cas.editor.Func("cos", listOf(m("3t"))))), MathRow(mutableListOf(com.example.cas.editor.Func("sin", listOf(m("2t"))))))
        Figure.Kind.Polar -> listOf(MathRow(mutableListOf(Sym("1"), Sym("+"), com.example.cas.editor.Func("cos", listOf(m("θ"))))))
        Figure.Kind.Text -> listOf(m("0"), m("0"))
        Figure.Kind.Annotate -> listOf(m("0"), m("0"), m("1"), m("1"))
        else -> emptyList()
    }
    return Figure.Layer(kind, inputs, text = if (kind == Figure.Kind.Text || kind == Figure.Kind.Annotate) "Note" else "")
}

@Composable
private fun EditorChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.height(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.surfaceContainerHighest).clickable(onClickLabel = tr(label), onClick = onClick).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIcon(icon, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(tr(label), style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
    }
}

@Composable
private fun AddLayerButton(modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(colors.primaryContainer).clickable(onClickLabel = tr("Add a layer"), onClick = onClick), contentAlignment = Alignment.Center) {
        AppIcon(TableIcons.Add, contentDescription = tr("Add a layer"), tint = colors.onPrimaryContainer, modifier = Modifier.size(28.dp))
    }
}

/** A layer in the list: its icon in its color, its type, any error, and the eye that hides it. */
@Composable
private fun LayerRow(l: Figure.Layer, color: Int?, error: String?, handle: Modifier, columns: List<String>, onOpen: () -> Unit, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint = color?.let { Color(it).copy(alpha = 1f) } ?: colors.outline
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surfaceContainerHigh).clickable(onClick = onOpen).padding(start = 0.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIcon(TableIcons.Grip, contentDescription = tr("Drag to reorder"), tint = colors.onSurfaceVariant, modifier = handle.size(40.dp).padding(10.dp))
        Box(Modifier.size(42.dp).clip(CircleShape).background(tint.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
            AppIcon(FigureIcons.of(l.kind), contentDescription = null, tint = colors.onSurface, accent = tint)
        }
        Spacer(Modifier.width(12.dp))
        // Just what it draws (and what's wrong, if anything is).
        Column(Modifier.weight(1f)) {
            Text(tr(l.kind.title), style = MaterialTheme.typography.titleMedium, color = if (l.visible) colors.onSurface else colors.outline)
            if (error != null) Text(tr(error), style = MaterialTheme.typography.bodySmall, color = colors.error, maxLines = 2)
        }
        IconButton(onClick = onToggle) { AppIcon(if (l.visible) FigureIcons.Shown else FigureIcons.Hidden, contentDescription = tr(if (l.visible) "Hide" else "Show"), tint = colors.onSurfaceVariant) }
    }
}

/** The gallery: every layer type, by group, each with its icon; a dialog like the colormap picker. */
@Composable
private fun LayerGallery(onPick: (Figure.Kind) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tablet = isTabletLayout()
    var query by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(28.dp), color = colors.surfaceContainerHigh, modifier = (if (tablet) Modifier.width(720.dp) else Modifier.fillMaxWidth(0.94f)).heightIn(max = 780.dp)) {
            Column(Modifier.padding(top = 20.dp, bottom = 12.dp)) {
                Text(tr("Add a layer"), style = MaterialTheme.typography.headlineSmall, color = colors.onSurface, modifier = Modifier.padding(horizontal = 24.dp))
                OutlinedTextField(query, { query = it }, singleLine = true, placeholder = { Text(tr("Search: histogram, violin, contour…")) }, shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp))
                val q = query.trim().lowercase()
                BoxWithConstraints(Modifier.weight(1f, fill = false)) {
                    val perRow = maxOf(2, ((maxWidth - 48.dp) / 110.dp).toInt())
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                        Figure.Group.entries.forEach { g ->
                            val kinds = Figure.Kind.entries.filter { it.group == g && (q.isEmpty() || tr(it.title).lowercase().contains(q) || it.py.contains(q) || it.title.lowercase().contains(q)) }
                            if (kinds.isEmpty()) return@forEach
                            Text(tr(g.title), style = MaterialTheme.typography.labelLarge, color = colors.primary, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                            kinds.chunked(perRow).forEach { row ->
                                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { k ->
                                        // A card as the colormaps' are: the picture on top, its name under it.
                                        Column(
                                            Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(colors.surfaceContainerHighest).clickable(onClickLabel = tr(k.title)) { onPick(k) }.padding(10.dp),
                                        ) {
                                            Box(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp)).background(colors.surfaceContainerLow), contentAlignment = Alignment.Center) {
                                                AppIcon(FigureIcons.of(k), contentDescription = null, tint = colors.onSurface, accent = colors.primary, modifier = Modifier.size(28.dp))
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Text(tr(k.title), style = MaterialTheme.typography.labelLarge, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(tr("Cancel")) }
                }
            }
        }
    }
}

/** Whether a layer's first input takes several things split by commas (box plots, violins, stacked areas). */
private fun l0isMulti(l: Figure.Layer?) = l?.kind in setOf(Figure.Kind.Box, Figure.Kind.Violin, Figure.Kind.Events, Figure.Kind.Stack)

/** Where [names] (a table's columns) stand whole in [text]: not inside a longer name. */
private fun columnSpans(text: String, names: List<String>): List<IntRange> {
    if (names.isEmpty()) return emptyList()
    val out = ArrayList<IntRange>()
    var k = 0
    fun word(ch: Char?) = ch != null && (ch.isLetterOrDigit() || ch == '_')
    // Side by side (freqSignal) they're two columns, multiplied.
    fun startsAt(at: Int, depth: Int = 0): String? = if (depth > 8) null else names.filter { text.startsWith(it, at) && (!word(text.getOrNull(at + it.length)) || startsAt(at + it.length, depth + 1) != null) }.maxByOrNull { it.length }
    var lastEnd = -1
    while (k < text.length) {
        val before = text.getOrNull(k - 1)
        val hit = if (before != null && (before.isLetter() || before == '_') && k != lastEnd) null else startsAt(k)
        if (hit != null) { out += k until k + hit.length; k += hit.length; lastEnd = k } else k++
    }
    return out
}

/** [new] typed over [old]; a key that deletes one character of a column's name deletes the whole name. */
private fun wholeColumns(old: TextFieldValue, new: TextFieldValue, names: List<String>): TextFieldValue {
    if (new.text.length != old.text.length - 1) return new
    val at = (0 until new.text.length).firstOrNull { old.text[it] != new.text[it] } ?: new.text.length
    val span = columnSpans(old.text, names).firstOrNull { at in it && it.last > it.first } ?: return new
    return TextFieldValue(old.text.removeRange(span.first, span.last + 1), TextRange(span.first))
}

/**
 * Column names in an input set apart as chips: a thin space each side (so the chip drawn under
 * them has room) and their letters in the chip's ink. [spans] says where the chips are in the
 * text as shown.
 */
private class ColumnChips(val names: List<String>, val front: androidx.compose.ui.unit.TextUnit, val back: androidx.compose.ui.unit.TextUnit) : androidx.compose.ui.text.input.VisualTransformation {
    // A space each side, stretched to the chip's icon half in front and its end after; the name in the chip's
    // own font and size, left invisible: the real chip is laid over it, so the field and the chips match exactly.
    private val lead = " "
    private val trail = " "

    /** Each column's place in the shown text, its spaces included, and its name. */
    fun spans(text: String): List<Pair<IntRange, String>> = columnSpans(text, names).mapIndexed { k, r ->
        val shift = k * (lead.length + trail.length)
        ((r.first + shift) until (r.last + 1 + shift + lead.length + trail.length)) to text.substring(r.first, r.last + 1)
    }

    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val spans = columnSpans(text.text, names)
        if (spans.isEmpty()) return androidx.compose.ui.text.input.TransformedText(text, androidx.compose.ui.text.input.OffsetMapping.Identity)
        val shown = androidx.compose.ui.text.buildAnnotatedString {
            var at = 0
            for (r in spans) {
                append(text.text.substring(at, r.first))
                var st = length
                append(lead)
                addStyle(androidx.compose.ui.text.SpanStyle(fontSize = 1.sp, letterSpacing = front), st, length)
                st = length
                append(text.text.substring(r.first, r.last + 1))
                addStyle(androidx.compose.ui.text.SpanStyle(color = Color.Transparent, fontFamily = CasFonts.Maple, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), st, length)
                st = length
                append(trail)
                addStyle(androidx.compose.ui.text.SpanStyle(fontSize = 1.sp, letterSpacing = back), st, length)
                at = r.last + 1
            }
            append(text.text.substring(at))
        }
        // Where each place in the typed text is in the shown text: a chip's start before its spaces, its end after them.
        val n = text.text.length
        val to = IntArray(n + 1) { o -> o + lead.length * spans.count { it.first < o } + trail.length * spans.count { it.last + 1 <= o } }
        val map = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int) = to[offset.coerceIn(0, n)]
            override fun transformedToOriginal(offset: Int): Int { var o = 0; while (o < n && to[o + 1] <= offset) o++; return o }
        }
        return androidx.compose.ui.text.input.TransformedText(shown, map)
    }
    override fun equals(other: Any?) = other is ColumnChips && other.names == names && other.front == front && other.back == back
    override fun hashCode() = names.hashCode()
}

/** The keypad typing into one [com.example.cas.editor.Editor] (your own fitted model). */
private class FieldKeys(private val target: () -> com.example.cas.editor.Editor?, private val version: () -> Int, private val onEnter: () -> Unit) : KeypadHost {
    private var hidden by mutableStateOf(false)
    override val angleUnit get() = KeypadState.angle
    override val panelOpen get() = KeypadState.panelOpen
    override val selectedTab get() = KeypadState.tab
    override fun toggleAngle() = KeypadState.toggleAngle()
    override fun togglePanel() = KeypadState.togglePanel()
    override fun selectTab(index: Int) = KeypadState.selectTab(index)
    override fun moveLeft() { target()?.moveLeft() }
    override fun moveRight() { target()?.moveRight() }
    override fun press(action: KeyAction) {
        val ed = target() ?: return
        when (action) {
            is KeyAction.Type -> ed.type(action.text)
            is KeyAction.Insert -> { ed.insert(action.make(), action.slot); if (action.path.isNotEmpty()) ed.enter(action.path) }
            KeyAction.Fraction -> ed.insertFraction()
            is KeyAction.Power -> ed.insertPower(action.exponent?.let { t -> MathRow(t.map { Sym(it.toString()) }.toMutableList()) })
            KeyAction.Paren -> ed.smartParen()
            KeyAction.ListBrackets -> { ed.type("["); ed.type("]"); ed.moveLeft() }
            KeyAction.Backspace -> ed.backspace()
            KeyAction.Clear -> ed.clear()
            KeyAction.Enter -> onEnter()
            is KeyAction.Sequence -> action.steps.forEach { press(it) }
            is KeyAction.Paste -> ed.insertRow(action.row)
            KeyAction.PickMatrix, KeyAction.MoreConstants, KeyAction.OpenSymbolBuilder -> Unit
        }
    }
    override fun insertMatrix(rows: Int, cols: Int) { target()?.insert(com.example.cas.editor.Matrix(rows, cols, growable = true), 0) }
    override val canUndo get() = version().let { target()?.canUndo ?: false }
    override val canRedo get() = version().let { target()?.canRedo ?: false }
    override fun undo() { target()?.undo() }
    override fun redo() { target()?.redo() }
    override var keypadHidden: Boolean
        get() = hidden
        set(v) { hidden = v }
    override val coordinates get() = KeypadState.coordinates
    override fun selectCoordinates(c: com.example.cas.cas.Coordinates) = KeypadState.selectCoordinates(c)
    override fun coordinatesOf(kind: com.example.cas.cas.CoordinateKind) = KeypadState.coordinatesOf(kind)
    override val unitSystem get() = KeypadState.unitSystem
    override fun selectUnitSystem(units: com.example.cas.engine.UnitSystem) = KeypadState.selectUnitSystem(units)
}

/** A layer's input, typed on the phone's keyboard and shown as math under it as it's read. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun InputField(label: String, value: TextFieldValue, columns: List<String>, onText: (TextFieldValue) -> Unit, onFocus: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val text = value.text
    val density = LocalDensity.current
    // The chip's icon half (7 + 15 + 5 dp, and the name's 6 dp) and its end (10 dp), as ColumnChip lays them out.
    val chips = remember(columns, density) { with(density) { ColumnChips(columns, 33.dp.toSp(), 10.dp.toSp()) } }
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    var layout by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    // On a tablet (and anywhere) the keyboard comes up when the field is tapped.
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val style = TextStyle(fontFamily = CasFonts.Mono, fontSize = 16.sp, color = colors.onSurface)
    Column(Modifier.fillMaxWidth()) {
        BasicTextField(
            value, onText, singleLine = true, visualTransformation = chips, textStyle = style, cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
            interactionSource = interaction, onTextLayout = { layout = it },
            modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) { onFocus(); keyboard?.show() } },
        ) { inner ->
            androidx.compose.material3.OutlinedTextFieldDefaults.DecorationBox(
                value = text, innerTextField = {
                    // Each column name sits in a rounded chip, drawn under its letters.
                    Box {
                        inner()
                        // The chips themselves, over the room the names keep (only on a layout of the text as it is now).
                        val lay = layout
                        if (lay != null && lay.layoutInput.text.text == chips.filter(androidx.compose.ui.text.AnnotatedString(text)).text.text) {
                            chips.spans(text).forEach { (r, name) ->
                                if (r.last >= lay.layoutInput.text.length) return@forEach
                                val line = lay.getLineForOffset(r.first)
                                val mid = (lay.getLineTop(line) + lay.getLineBottom(line)) / 2
                                val left = lay.getHorizontalPosition(r.first, true)
                                // Centred on the line, whatever height the chip measures.
                                ColumnChip(name, modifier = Modifier.layout { m, c ->
                                    val p = m.measure(c.copy(minWidth = 0, minHeight = 0))
                                    layout(0, 0) { p.place(left.roundToInt(), (mid - p.height / 2f).roundToInt()) }
                                })
                            }
                        }
                    }
                },
                enabled = true, singleLine = true, visualTransformation = chips, interactionSource = interaction,
                label = { Text(label) },
                placeholder = { Text(tr("e.g. sin(x), x^2/2, [1, 4, 9], a column, Signal[1:50]"), style = style.copy(color = colors.outline), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                container = { androidx.compose.material3.OutlinedTextFieldDefaults.Container(enabled = true, isError = false, interactionSource = interaction, colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(), shape = RoundedCornerShape(16.dp)) },
            )
        }
        // How it's read: math, with the table's columns as chips.
        if (text.isNotBlank()) Box(Modifier.padding(start = 12.dp, top = 4.dp).horizontalScroll(rememberScrollState())) {
            ReadAs(text, columns, 15.sp, colors.onSurfaceVariant)
        }
    }
}

/** An input as it's read: the calculator's math, or, with columns in it, the columns as chips among the rest. */
@Composable
private fun ReadAs(text: String, columns: List<String>, size: androidx.compose.ui.unit.TextUnit, ink: Color) {
    val spans = remember(text, columns) { columnSpans(text, columns) }
    if (spans.isEmpty()) { MathView(remember(text) { com.example.cas.engine.PlainMath.parse(text) }, size, ink); return }
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        var at = 0
        fun rest(t: String) = t.replace("*", " · ").replace("-", " − ").replace("+", " + ").replace("/", " / ").replace(Regex(" +"), " ")
        for (r in spans) {
            if (r.first > at) Text(rest(text.substring(at, r.first)), style = TextStyle(fontSize = size, color = ink))
            ColumnChip(text.substring(r.first, r.last + 1))
            at = r.last + 1
        }
        if (at < text.length) Text(rest(text.substring(at)), style = TextStyle(fontSize = size, color = ink))
    }
}

/** A table column's name as a chip: a rounded container in the theme's primary tone. */
@Composable
private fun ColumnChip(name: String, size: androidx.compose.ui.unit.TextUnit = 14.sp, modifier: Modifier = Modifier) {
    // Split: the column's own color with a table glyph, then its name on a tone of that color.
    val c = chipColors(name)
    Row(modifier.height(androidx.compose.ui.unit.Dp(size.value * 1.75f)).clip(CircleShape), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.fillMaxHeight().background(c.strong).padding(start = 7.dp, end = 5.dp), contentAlignment = Alignment.Center) {
            AppIcon(TableIcons.Table, contentDescription = null, tint = c.onStrong, modifier = Modifier.size((size.value + 1).dp))
        }
        Box(Modifier.fillMaxHeight().background(c.tonal).padding(start = 6.dp, end = 10.dp), contentAlignment = Alignment.Center) {
            Text(name, style = TextStyle(fontFamily = CasFonts.Maple, fontSize = size, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = c.ink), maxLines = 1)
        }
    }
}

/**
 * A table shown whole: the row numbers, each column's name with its mean and spread, and every
 * row (scrolled, as many as there are). Each column is as wide as its numbers; drag the bar at a
 * column name's right edge to make it wider or narrower. Tapping a column's name: [onHeader].
 */
@Composable
private fun DataGrid(t: com.example.cas.graph.DataTable, modifier: Modifier, onHeader: ((Int) -> Unit)?, headerMenu: (@Composable (Int, () -> Unit) -> Unit)? = null, onDoubleTap: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val mono = TextStyle(fontFamily = CasFonts.Mono, fontSize = 14.sp, color = colors.onSurface)
    val numW = 40.dp
    val n = t.names.size
    val stats = remember(t) { (0 until n).map { t.stats(it) } }
    fun shown(c: Int, r: Int): String {
        val raw = t.cell(c, r)
        return if (com.example.cas.graph.Sheet.isFormula(raw)) t.value(c, r)?.let { com.example.cas.graph.DataTable.text(it) } ?: raw else raw
    }
    // Widths from the longest of the first rows (and the name), kept while the columns stay.
    val widths = remember(n, t.names) {
        androidx.compose.runtime.mutableStateListOf(*Array(n) { c ->
            val chars = maxOf(t.names[c].length + 2, (0 until minOf(t.rowCount, 60)).maxOfOrNull { shown(c, it).length } ?: 0, 6)
            (chars * 8.6f + 18f).coerceIn(64f, 200f).dp
        })
    }
    // Double-tapped: the table editor.
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainerHigh)
        .then(if (onDoubleTap != null) Modifier.pointerInput(Unit) { detectTapGestures(onDoubleTap = { onDoubleTap() }) } else Modifier)
        .horizontalScroll(rememberScrollState())) {
        Column(Modifier.width(numW + widths.fold(0.dp) { a, b -> a + b })) {
            Row(Modifier.background(colors.surfaceContainerHighest)) {
                Box(Modifier.size(numW, 52.dp))
                (0 until n).forEach { c ->
                    var menu by remember { mutableStateOf(false) }
                    val cellW = widths.getOrElse(c) { 96.dp }
                    Box {
                        Column(
                            Modifier.size(cellW, 52.dp).clickable(enabled = onHeader != null || headerMenu != null) { if (headerMenu != null) menu = true else onHeader?.invoke(c) }.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center,
                        ) {
                            if (t.names[c].isBlank()) Text("—", style = mono.copy(color = colors.outline)) else ColumnChip(t.names[c].trim())
                            stats[c]?.let { st -> Text("x̄ " + com.example.cas.graph.Figure.tickText(st.mean) + (st.sd?.let { " ± " + com.example.cas.graph.Figure.tickText(it) } ?: ""), style = mono.copy(fontSize = 10.sp, color = colors.onSurfaceVariant), maxLines = 1) }
                        }
                        if (headerMenu != null) DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) { headerMenu(c) { menu = false } }
                        // The bar to drag for the column's width.
                        Box(Modifier.align(Alignment.CenterEnd).width(16.dp).height(52.dp).pointerInput(c) {
                            detectDragGestures { ptr, d ->
                                ptr.consume()
                                if (c in widths.indices) widths[c] = (widths[c] + with(density) { d.x.toDp() }).coerceIn(48.dp, 360.dp)
                            }
                        }, contentAlignment = Alignment.Center) {
                            Box(Modifier.width(3.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(colors.outlineVariant))
                        }
                    }
                }
            }
            LazyColumn(Modifier.fillMaxWidth()) {
                items(t.rowCount) { r ->
                    Row(Modifier.background(if (r % 2 == 1) colors.surfaceContainer else colors.surfaceContainerHigh)) {
                        Box(Modifier.size(numW, 32.dp), contentAlignment = Alignment.Center) { Text("${r + 1}", style = mono.copy(fontSize = 11.sp, color = colors.onSurfaceVariant)) }
                        (0 until n).forEach { c ->
                            Box(Modifier.size(widths.getOrElse(c) { 96.dp }, 32.dp).padding(start = 8.dp, end = 14.dp), contentAlignment = Alignment.CenterEnd) { Text(shown(c, r), style = mono, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The figure's table, whole, with what can be done with it: edit it (the full table editor),
 * import a CSV, plot every column against the first, and, from a column's name, a layer of it.
 */
@Composable
private fun DataPanel(t: com.example.cas.graph.DataTable, modifier: Modifier, sheetTabs: @Composable () -> Unit, onEdit: () -> Unit, onImport: () -> Unit, onAdd: (Figure.Kind, List<Int>) -> Unit, onPlotAll: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.padding(horizontal = 12.dp)) {
        sheetTabs()
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EditorChip(TableIcons.Edit, "Edit", onEdit)
            EditorChip(TableIcons.ImportData, "Import files", onImport)
            if (t.names.size >= 2) EditorChip(FigureIcons.of(Figure.Kind.Scatter), "Plot all", onPlotAll)
        }
        Text(tr("Tap a column's name for a layer of it; double-tap the table to edit it."), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        val first = t.names.getOrElse(0) { "x" }.ifBlank { "1st column" }
        DataGrid(t, Modifier.weight(1f).fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp), onHeader = null, onDoubleTap = onEdit, headerMenu = { c, done ->
            @Composable fun item(label: String, kind: Figure.Kind, cols: List<Int>) =
                DropdownMenuItem(text = { Text(label) }, leadingIcon = { AppIcon(FigureIcons.of(kind), null) }, onClick = { done(); onAdd(kind, cols) })
            if (c > 0) {
                item(tr("Scatter against {0}", first), Figure.Kind.Scatter, listOf(0, c))
                item(tr("Line against {0}", first), Figure.Kind.Plot, listOf(0, c))
                item(tr("Fitted curve against {0}", first), Figure.Kind.Fit, listOf(0, c))
                item(tr("Bars"), Figure.Kind.Bar, listOf(c))
            }
            item(tr("Histogram"), Figure.Kind.Hist, listOf(c))
            item(tr("Density curve"), Figure.Kind.Kde, listOf(c))
            item(tr("Box plot"), Figure.Kind.Box, listOf(c))
            item(tr("ECDF"), Figure.Kind.Ecdf, listOf(c))
        })
    }
}

private val WIDTHS = listOf(0.5, 0.75, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0)
private val ALPHAS = listOf(0.15, 0.3, 0.5, 0.7, 0.85, 1.0)
private val SIZES = listOf(1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0, 6.0, 7.0, 9.0)
private val CAPS = listOf(0.0, 1.5, 3.0, 4.5, 6.0)
private val ERR_WIDTHS = listOf(0.4, 0.6, 0.9, 1.2, 1.6, 2.2)
private val LEVELS = listOf(4, 6, 8, 10, 12, 16, 20, 30)
private val BINS = listOf(0, 5, 10, 20, 40)

/**
 * A setting on a straight (linear) slider with fine steps of [step]: its name and value above.
 * The value follows the finger at once; [onChange] hears each step.
 */
@Composable
private fun SliderRow(title: String, value: Double, range: ClosedFloatingPointRange<Double>, step: Double, shown: (Double) -> String, onChange: (Double) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var v by remember(value) { mutableStateOf(value.coerceIn(range)) }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr(title), style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, modifier = Modifier.weight(1f))
            Text(shown(v), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
        }
        ExpressiveSlider(
            value = v.toFloat(),
            onValueChange = { f ->
                val q = (Math.round((f - range.start) / step) * step + range.start).coerceIn(range)
                if (q != v) { v = q; onChange(q) }
            },
            valueRange = range.start.toFloat()..range.endInclusive.toFloat(),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = tr(title) },
        )
    }
}

private fun nearest(list: List<Double>, v: Double) = list.indices.minByOrNull { kotlin.math.abs(list[it] - v) } ?: 0

/**
 * The open layer: its inputs on the keypad, the figure's data (its columns as chips that type
 * themselves into the input), its text, its look (the same Color dialog as a graph's line, with
 * the layer's own options under it), the legend name, and moving, copying or deleting it.
 */
@Composable
private fun LayerEditor(
    l: Figure.Layer, error: String?, color: Int?, table: com.example.cas.graph.DataTable, sheets: List<Figure.Sheet>, sheetAt: Int, sheetTabs: @Composable () -> Unit,
    onColumn: (Int, Int) -> Unit, onEditData: () -> Unit, onImport: () -> Unit,
    texts: List<TextFieldValue>, onText: (Int, TextFieldValue) -> Unit, columns: List<String>, onEditMath: (Int) -> Unit, fitted: DoubleArray?,
    onFocus: (Int?) -> Unit, onChange: (Figure.Layer) -> Unit, onClose: () -> Unit, onMove: (Int) -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val s = l.style
    var look by remember { mutableStateOf(false) }
    val tint = color?.let { Color(it) } ?: colors.primary
    if (look) LayerLookDialog(l, tint, onApply = { onChange(l.copy(style = it)); look = false }, onDismiss = { look = false })
    androidx.compose.runtime.CompositionLocalProvider(LocalCompactRows provides true) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(tint.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                AppIcon(FigureIcons.of(l.kind), contentDescription = null, tint = colors.onSurface, accent = tint, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tr(l.kind.title), style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
                Text("plt." + l.kind.py + "(…)", style = TextStyle(fontFamily = CasFonts.Mono, fontSize = 13.sp, color = colors.onSurfaceVariant))
            }
            TextButton(onClick = onClose) { Text(tr("Done")) }
        }
        MathText(tr(l.kind.about), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        PxHeader("Inputs")
        if (l.kind == Figure.Kind.Parametric || l.kind == Figure.Kind.Polar) {
            // Curves are math: each input on the calculator's keypad, as your own fitted model is.
            l.kind.inputs.forEachIndexed { k, name -> MathInputBox(name, l.input(k)) { onEditMath(k) } }
        } else {
            // A field's x and y show only when z (or u, v) uses table columns: a function or a matrix doesn't need them.
            val field = l.kind in setOf(Figure.Kind.Contour, Figure.Kind.ContourF, Figure.Kind.Heatmap, Figure.Kind.Quiver, Figure.Kind.Stream)
            val usesColumns = texts.drop(2).any { columnSpans(it.text, columns).isNotEmpty() } || texts.take(2).any { it.text.isNotBlank() }
            texts.forEachIndexed { k, t -> if (!field || k >= 2 || usesColumns) InputField(l.kind.inputs.getOrElse(k) { "" }, t, columns, { onText(k, it) }) { onFocus(k) } }
            if (field && !usesColumns) Text(tr("x and y appear when you use table columns."), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        if (error != null) Text(tr(error), style = MaterialTheme.typography.bodyMedium, color = colors.error)
        // Every table's columns as buttons: tap one to put it in at the cursor.
        val named = sheets.flatMapIndexed { si, sh -> sh.table.names.indices.filter { sh.table.names[it].isNotBlank() }.map { si to it } }
        if (named.isNotEmpty() && l.kind != Figure.Kind.Parametric && l.kind != Figure.Kind.Polar) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                named.forEach { (si, c) ->
                    Box(Modifier.clip(CircleShape).clickable(onClickLabel = tr("Put in")) { onColumn(si, c) }.padding(vertical = 4.dp)) { ColumnChip(sheets[si].table.names[c].trim()) }
                }
            }
        }
        l.kind.text?.let { name ->
            OutlinedTextField(l.text, { onChange(l.copy(text = it)) }, label = { Text(tr(name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        PxHeader("Data")
        // The tables; a column's name puts it in the input being typed.
        sheetTabs()
        if (table.names.isNotEmpty() && table.rowCount > 0) DataGrid(table, Modifier.fillMaxWidth().heightIn(max = 280.dp), onHeader = { c -> onColumn(sheetAt, c) }, onDoubleTap = onEditData)
        Text(tr("Tap a column's name to put it in the input; double-tap the table to edit it."), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EditorChip(TableIcons.Table, "Edit table", onEditData)
            EditorChip(TableIcons.ImportData, "Import files", onImport)
        }
        // A fitted curve's model: a ready-made one, or its own formula.
        if (l.kind == Figure.Kind.Fit) {
            PxHeader("Model")
            val custom = s.model >= Figure.CUSTOM_FIT
            // As the calculator's tabs: your own model first (the pen), then the ready-made ones, each with its curve.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val tabs = listOf(-1) + com.example.cas.graph.FitModel.entries.indices
                tabs.forEach { k ->
                    val on = if (k < 0) custom else !custom && s.model == k
                    val name = if (k < 0) tr("Your own") else tr(com.example.cas.graph.FitModel.entries[k].title)
                    Box(
                        Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(16.dp)).background(if (on) colors.primary else colors.surfaceContainerHigh)
                            .clickable(onClickLabel = name) {
                                if (k < 0) { if (!custom) onChange(l.copy(style = s.copy(model = Figure.CUSTOM_FIT, formula = s.formula.ifBlank { "a*e^(-b*x) + c" }))); onEditMath(-1) }
                                else onChange(l.copy(style = s.copy(model = k)))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        AppIcon(if (k < 0) TableIcons.Edit else FigureIcons.FitModels[k], contentDescription = name, tint = if (on) colors.onPrimary else colors.onSurface,
                            accent = if (on) colors.onPrimary else colors.primary, modifier = Modifier.size(22.dp))
                    }
                }
            }
            val params = if (custom) Figure.customModel(s.formula)?.params.orEmpty() else com.example.cas.graph.FitModel.entries.getOrElse(s.model) { com.example.cas.graph.FitModel.Linear }.params
            if (!custom) {
                val m = com.example.cas.graph.FitModel.entries.getOrElse(s.model) { com.example.cas.graph.FitModel.Linear }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr(m.title), style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                    Spacer(Modifier.width(10.dp))
                    MathText("\$" + m.latex + "\$", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                }
            }
            if (custom) {
                // Tapped: typed on the calculator's keypad.
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainerHighest).clickable(onClickLabel = tr("Edit the model")) { onEditMath(-1) }.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                        MathView(remember(s.formula) { MathRow(mutableListOf<com.example.cas.editor.Node>(Sym("y"), Sym("=")).apply { addAll(Figure.formulaRow(s.formula).items) }) }, 20.sp, colors.onSurface)
                    }
                    AppIcon(TableIcons.Edit, contentDescription = null, tint = colors.onSurfaceVariant)
                }
            }
            // The fitted numbers, under the model (the legend shows the model itself and R²).
            if (fitted != null && fitted.size == params.size && params.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    params.forEachIndexed { k, p ->
                        val v = fitted[k]
                        val shown = if (v.isFinite()) java.math.BigDecimal(v).round(java.math.MathContext(5)).stripTrailingZeros().toPlainString().replace("-", "−") else "?"
                        Box(Modifier.clip(RoundedCornerShape(12.dp)).background(colors.surfaceContainerHigh).padding(horizontal = 12.dp, vertical = 6.dp)) {
                            MathView(remember(p, shown) { MathRow((listOf<com.example.cas.editor.Node>(Sym(p), Sym("=")) + shown.map { Sym(it.toString()) }).toMutableList()) }, 18.sp, colors.onSurface)
                        }
                    }
                }
            }
        }
        PxHeader("Look")
        // Layers drawn through a colormap: the colormap picker, and their colorbar's label.
        if (l.kind.mapped) {
            var picking by remember { mutableStateOf(false) }
            val (map, reversed) = com.example.cas.graph.Colormap.parse(s.cmap)
            if (picking) ColormapPickerDialog(map, reversed, onPick = { m, r -> onChange(l.copy(style = s.copy(cmap = com.example.cas.graph.Colormap.save(m, r)))); picking = false }, onDismiss = { picking = false })
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainerHigh).clickable(onClickLabel = tr("Colormap")) { picking = true }.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(72.dp).height(24.dp).clip(RoundedCornerShape(8.dp)).background(Brush.horizontalGradient(colormapStops(map, reversed = reversed))))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("Colormap"), style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                    Text(map.label + if (reversed) " · " + tr("Reversed") else "", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                AppIcon(TableIcons.ChevronRight, contentDescription = null, tint = colors.onSurfaceVariant)
            }
            if (l.kind == Figure.Kind.Contour || l.kind == Figure.Kind.ContourF)
                SliderRow("Levels", s.levels.toDouble(), 2.0..60.0, 1.0, { "${it.toInt()}" }) { onChange(l.copy(style = s.copy(levels = it.roundToInt()))) }
            if (l.kind == Figure.Kind.Hist2D || l.kind == Figure.Kind.Hexbin)
                SliderRow("Bins", s.bins.toDouble(), 0.0..120.0, 1.0, { if (it < 1) tr("Auto") else "${it.toInt()}" }) { onChange(l.copy(style = s.copy(bins = it.roundToInt()))) }
            if (l.kind != Figure.Kind.Contour) {
                SliderRow("Opacity", s.alpha, 0.05..1.0, 0.01, { "${(it * 100).roundToInt()} %" }) { onChange(l.copy(style = s.copy(alpha = it))) }
                OutlinedTextField(s.cbar, { onChange(l.copy(style = s.copy(cbar = it))) }, label = { Text(tr("Colorbar label")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
        // Contour lines also take one color; the rest of the colormapped layers take none.
        if (!l.kind.mapped || l.kind == Figure.Kind.Contour) Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainerHigh).clickable(onClickLabel = tr("Look")) { look = true }.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(tint))
            Spacer(Modifier.width(12.dp))
            Canvas(Modifier.width(48.dp).height(12.dp)) {
                val w = 2.5.dp.toPx()
                drawLine(tint.copy(alpha = s.alpha.toFloat()), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), w, cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    pathEffect = com.example.cas.graph.LineStyle.of(s.line).pattern(w.toDouble())?.let { d -> PathEffect.dashPathEffect(FloatArray(d.size) { k -> maxOf(d[k].toFloat(), 0.01f) }) })
            }
            Spacer(Modifier.width(12.dp))
            Text(tr("Color, line and more"), style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, modifier = Modifier.weight(1f))
            AppIcon(TableIcons.ChevronRight, contentDescription = null, tint = colors.onSurfaceVariant)
        }
        OutlinedTextField(s.label, { onChange(l.copy(style = s.copy(label = it))) }, label = { Text(tr("Name in the legend")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            LayerAction(TableIcons.ChevronUp, "Up") { onMove(-1) }
            LayerAction(TableIcons.ChevronDown, "Down") { onMove(1) }
            LayerAction(TableIcons.Copy, "Copy", onDuplicate)
            LayerAction(TableIcons.Delete, "Delete", onDelete)
        }
    }
    }
}

/**
 * A layer's look, in the same Color dialog as a graph's line: the color (Default is the next in
 * the theme's cycle), line style and thickness, then the layer's own options under them
 * (marker, size, opacity, bins). Nothing changes until Done.
 */
@Composable
private fun LayerLookDialog(l: Figure.Layer, current: Color, onApply: (Figure.Style) -> Unit, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf(l.style) }
    var line by remember { mutableStateOf(l.style.line) }
    var width by remember { mutableStateOf((l.style.width / 0.6).toFloat().coerceIn(1f, 8f)) }
    ColorPickerDialog(
        initial = current,
        onPick = { c -> onApply(draft.copy(argb = c?.toArgb() ?: 0, color = if (c == null) -1 else draft.color, line = line, width = width * 0.6)) },
        // The color left as it was: only the rest changes, and a default color stays the theme's.
        onKeep = { onApply(draft.copy(line = line, width = width * 0.6)) },
        onDismiss = onDismiss,
        // Points have a size rather than a line (under the colors).
        lineStyle = if ((l.kind.lined || l.kind == Figure.Kind.Events) && l.kind != Figure.Kind.Scatter) line else null,
        thickness = width,
        onStyle = { st, w -> line = st; width = w },
        title = "Look",
        extra = {
            val k = l.kind
            androidx.compose.runtime.CompositionLocalProvider(LocalCompactRows provides true) {
                // Text: its size (its color above). An arrow with a note: the note's size and the arrow's head, both in the color above.
                if (k == Figure.Kind.Text || k == Figure.Kind.Annotate) {
                    SliderRow("Text size", draft.size, 1.5..12.0, 0.1, { "${(Figure.textSize(draft) * 10).roundToInt() / 10.0} pt" }) { draft = draft.copy(size = it) }
                    if (k == Figure.Kind.Annotate) {
                        SliderRow("Gap to the note", draft.gap, 0.0..40.0, 0.5, { String.format(java.util.Locale.US, "%.1f pt", it) }) { draft = draft.copy(gap = it) }
                        ArrowHeads(draft) { draft = it }
                        if (draft.arrowTip != com.example.cas.graph.VectorField.Tip.None.ordinal) SliderRow("Head size", draft.arrowTipSize, 0.3..3.0, 0.05, { "× " + String.format(java.util.Locale.US, "%.2f", it) }) { draft = draft.copy(arrowTipSize = it) }
                    }
                    return@CompositionLocalProvider
                }
                // Arrows: the same options as the 2D graph's vector fields.
                if (k == Figure.Kind.Quiver) ArrowChoices(draft) { draft = it }
                if (k.lined && k != Figure.Kind.Contour && k != Figure.Kind.Quiver && k != Figure.Kind.Stream && k != Figure.Kind.Kde && k != Figure.Kind.Scatter) MarkerChoice(draft.marker) { draft = draft.copy(marker = it) }
                if (k == Figure.Kind.Scatter) MarkerChoice(draft.marker.coerceAtLeast(0)) { draft = draft.copy(marker = it) }
                if (k == Figure.Kind.Scatter || k == Figure.Kind.Fit || draft.marker >= 0) SliderRow(if (k == Figure.Kind.Scatter) "Point size" else "Marker size", draft.size, 0.5..15.0, 0.1, { String.format(java.util.Locale.US, "%.1f", it) }) { draft = draft.copy(size = it) }
                // ± errors, when the layer has any: bars with caps, or a shaded band.
                Figure.Kind.ERRORS[k]?.let { (ky, kx) ->
                    if (l.input(ky).items.isNotEmpty() || l.input(kx).items.isNotEmpty()) {
                        PxChoiceRow("Errors", listOf("Bars", "Band"), if (draft.errBand) 1 else 0) { draft = draft.copy(errBand = it == 1) }
                        if (!draft.errBand) {
                            SliderRow("Caps", draft.errCap, 0.0..10.0, 0.1, { if (it < 0.05) tr("None") else String.format(java.util.Locale.US, "%.1f", it) }) { draft = draft.copy(errCap = it) }
                            SliderRow("Error bar thickness", draft.errWidth, 0.2..5.0, 0.05, { String.format(java.util.Locale.US, "%.2f", it) }) { draft = draft.copy(errWidth = it) }
                        }
                    }
                }
                SliderRow("Opacity", draft.alpha, 0.05..1.0, 0.01, { "${(it * 100).roundToInt()} %" }) { draft = draft.copy(alpha = it) }
                if (k == Figure.Kind.Hist || k == Figure.Kind.Hist2D || k == Figure.Kind.Hexbin) {
                    SliderRow("Bins", draft.bins.toDouble(), 0.0..120.0, 1.0, { if (it < 1) tr("Auto") else "${it.toInt()}" }) { draft = draft.copy(bins = it.roundToInt()) }
                    if (k == Figure.Kind.Hist) {
                        PxToggleRow("Density", "Areas add up to 1 (to compare with a density curve)", draft.density) { draft = draft.copy(density = it) }
                        PxToggleRow("Cumulative", "Running totals", draft.cumulative) { draft = draft.copy(cumulative = it) }
                        PxToggleRow("Outline only", "histtype=\"step\"", draft.line == 1) { draft = draft.copy(line = if (it) 1 else 0); line = if (it) 1 else 0 }
                    }
                }
            }
        },
    )
}

/**
 * Arrows as the 2D graph's vector fields have them: colored by length along a colormap (tap the
 * strip to pick one) or in the layer's color, their length, scale, head and its size, and how many.
 */
@Composable
private fun ArrowChoices(s: Figure.Style, onChange: (Figure.Style) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var picking by remember { mutableStateOf(false) }
    val (map, reversed) = com.example.cas.graph.Colormap.parse(s.cmap)
    if (picking) ColormapPickerDialog(map, reversed, onPick = { m, r -> onChange(s.copy(cmap = com.example.cas.graph.Colormap.save(m, r))); picking = false }, onDismiss = { picking = false })
    Text(tr("Arrows"), style = MaterialTheme.typography.labelLarge, color = colors.primary)
    Text(tr("Color"), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    Segments(listOf("This color", "By length"), if (s.arrowByLength) 1 else 0, "Arrow color") { onChange(s.copy(arrowByLength = it == 1)) }
    if (s.arrowByLength) Box(
        Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(14.dp)).background(Brush.horizontalGradient(colormapStops(map, reversed = reversed)))
            .clickable(onClickLabel = tr("Change the colormap")) { picking = true },
        contentAlignment = Alignment.CenterStart,
    ) {
        MathText(map.label + (if (reversed) " (reversed)" else "") + " · short \$\\to\$ long", style = MaterialTheme.typography.labelLarge, color = Color.White,
            modifier = Modifier.padding(start = 10.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.35f)).padding(horizontal = 8.dp, vertical = 2.dp))
    }
    Text(tr("Length"), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    Segments(com.example.cas.graph.VectorField.Length.entries.map { it.label }, s.arrowLength, "Arrow length") { onChange(s.copy(arrowLength = it)) }
    SliderRow("Scale", s.arrowScale, 0.2..3.0, 0.05, { "× " + String.format(java.util.Locale.US, "%.2f", it) }) { onChange(s.copy(arrowScale = it)) }
    ArrowHeads(s, onChange)
    if (s.arrowTip != com.example.cas.graph.VectorField.Tip.None.ordinal) SliderRow("Head size", s.arrowTipSize, 0.3..3.0, 0.05, { "× " + String.format(java.util.Locale.US, "%.2f", it) }) { onChange(s.copy(arrowTipSize = it)) }
    SliderRow("Arrows across", s.arrows.toDouble(), 4.0..60.0, 1.0, { "${it.toInt()}" }) { onChange(s.copy(arrows = it.roundToInt())) }
}

/** The arrowhead's shape, each drawn as it looks (as the 2D graph's vector fields offer them). */
@Composable
private fun ArrowHeads(s: Figure.Style, onChange: (Figure.Style) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Text(tr("Arrowhead"), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        com.example.cas.graph.VectorField.Tip.entries.forEach { tip ->
            val chosen = s.arrowTip == tip.ordinal
            Box(
                Modifier.size(width = 52.dp, height = 36.dp).clip(RoundedCornerShape(10.dp)).background(if (chosen) colors.secondaryContainer else colors.surfaceContainerHigh)
                    .clickable(onClickLabel = tip.label) { onChange(s.copy(arrowTip = tip.ordinal)) },
                contentAlignment = Alignment.Center,
            ) {
                val tint = if (chosen) colors.onSecondaryContainer else colors.onSurfaceVariant
                Canvas(Modifier.size(width = 34.dp, height = 20.dp)) {
                    val y = size.height / 2
                    val h = com.example.cas.graph.VectorField.head(tip, size.width.toDouble() - 2, y.toDouble(), 1.0, 0.0, 9.dp.toPx().toDouble())
                    drawLine(tint, Offset(2f, y), Offset(h.shaftEndX.toFloat(), y), 1.8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    h.fills.forEach { pts -> drawPath(Path().apply { moveTo(pts[0].toFloat(), pts[1].toFloat()); var k = 2; while (k + 1 < pts.size) { lineTo(pts[k].toFloat(), pts[k + 1].toFloat()); k += 2 }; close() }, tint) }
                    h.strokes.forEach { sg -> drawLine(tint, Offset(sg[0].toFloat(), sg[1].toFloat()), Offset(sg[2].toFloat(), sg[3].toFloat()), 1.8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round) }
                    h.dot?.let { d -> drawCircle(tint, d[2].toFloat(), Offset(d[0].toFloat(), d[1].toFloat())) }
                }
            }
        }
    }
}

@Composable
private fun LayerAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClickLabel = tr(label), onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AppIcon(icon, contentDescription = null, tint = colors.onSurface)
        Text(tr(label), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun MarkerChoice(selected: Int, onPick: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val options = listOf(-1) + com.example.cas.graph.Marker.bases.map { it.ordinal }
    Column(Modifier.padding(vertical = 6.dp)) {
        PxRowTexts("Marker", if (selected < 0) "None" else com.example.cas.graph.Marker.of(selected).label)
        Row(Modifier.fillMaxWidth().padding(top = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { m ->
                val on = m == selected
                val ink = if (on) colors.onPrimaryContainer else colors.onSurface
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(if (on) colors.primaryContainer else colors.surfaceContainerHighest).clickable { onPick(m) }, contentAlignment = Alignment.Center) {
                    if (m < 0) Text("—", color = ink)
                    else Canvas(Modifier.size(20.dp)) {
                        val (fills, strokes) = com.example.cas.graph.Marker.of(m).outline(size.width / 2.0, size.height / 2.0, size.width * 0.32)
                        fills.forEach { poly -> drawPath(Path().apply { moveTo(poly[0].toFloat(), poly[1].toFloat()); var k = 2; while (k + 1 < poly.size) { lineTo(poly[k].toFloat(), poly[k + 1].toFloat()); k += 2 }; close() }, ink) }
                        strokes.forEach { seg -> drawLine(ink, Offset(seg[0].toFloat(), seg[1].toFloat()), Offset(seg[2].toFloat(), seg[3].toFloat()), 2.dp.toPx()) }
                    }
                }
            }
        }
    }
}


/** A number typed with the phone's keyboard; empty means automatic. */
@Composable
private fun NumberField(label: String, value: Double?, modifier: Modifier, onChange: (Double?) -> Unit) {
    var text by remember(value) { mutableStateOf(value?.let { com.example.cas.graph.Figure.tickText(it).replace("−", "-") } ?: "") }
    OutlinedTextField(text, { t -> text = t; val v = t.replace("−", "-").replace(',', '.').toDoubleOrNull(); if (t.isBlank()) onChange(null) else if (v != null) onChange(v) },
        label = { Text(tr(label)) }, placeholder = { Text(tr("auto")) }, singleLine = true, modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done))
}

/** The axes' page: titles, limits, scales, grid, frame and legend. */
@Composable
private fun AxesSettings(a: Figure.Axes, onChange: (Figure.Axes) -> Unit) {
    val colors = MaterialTheme.colorScheme
    PxHeader("Titles")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(a.title, { onChange(a.copy(title = it)) }, label = { Text(tr("Title")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(a.xLabel, { onChange(a.copy(xLabel = it)) }, label = { Text(tr("x label")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(a.yLabel, { onChange(a.copy(yLabel = it)) }, label = { Text(tr("y label")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    }
    PxHeader("Limits")
    // Automatic (fitted to what's drawn) unless limits are typed.
    var manual by remember { mutableStateOf(listOf(a.xMin, a.xMax, a.yMin, a.yMax).any { it != null }) }
    PxCards({ PxToggleRow("Automatic limits", "Fitted to what's drawn", !manual) { auto -> manual = !auto; if (auto) onChange(a.copy(xMin = null, xMax = null, yMin = null, yMax = null)) } })
    if (manual) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("x from", a.xMin, Modifier.weight(1f)) { onChange(a.copy(xMin = it)) }
            NumberField("x to", a.xMax, Modifier.weight(1f)) { onChange(a.copy(xMax = it)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("y from", a.yMin, Modifier.weight(1f)) { onChange(a.copy(yMin = it)) }
            NumberField("y to", a.yMax, Modifier.weight(1f)) { onChange(a.copy(yMax = it)) }
        }
    }
    PxHeader("Scales")
    PxCards(
        { PxToggleRow("Logarithmic x", "set_xscale(\"log\")", a.xLog) { onChange(a.copy(xLog = it)) } },
        { PxToggleRow("Logarithmic y", "set_yscale(\"log\")", a.yLog) { onChange(a.copy(yLog = it)) } },
        { PxToggleRow("Equal scales", "A unit as long on both axes", a.equal) { onChange(a.copy(equal = it)) } },
        { PxToggleRow("Turn x round", "High to low: invert_xaxis()", a.xInvert) { onChange(a.copy(xInvert = it)) } },
        { PxToggleRow("Turn y round", "High to low: invert_yaxis()", a.yInvert) { onChange(a.copy(yInvert = it)) } },
    )
    PxHeader("Look")
    PxCards(
        { PxChoiceRow("Grid", listOf("Off", "Major", "Minor too"), a.grid.coerceIn(0, 2)) { onChange(a.copy(grid = it)) } },
        { PxChoiceRow("Frame", listOf("Box", "L", "At zero", "None"), a.spines.coerceIn(0, 3)) { onChange(a.copy(spines = it)) } },
        { PxToggleRow("Numbers on x", "The values under the ticks", a.xNumbers) { onChange(a.copy(xNumbers = it)) } },
        { PxToggleRow("Numbers on y", "The values beside the ticks", a.yNumbers) { onChange(a.copy(yNumbers = it)) } },
        { PxChoiceRow("Ticks", listOf("Out", "In"), if (a.ticksIn) 1 else 0) { onChange(a.copy(ticksIn = it == 1)) } },
    )
    PxHeader("Legend")
    PxCards(
        { PxChoiceRow("Legend", listOf("Off", "Best", "Placed"), when { a.legend < 0 -> 0; a.legend == 0 -> 1; else -> 2 }) { onChange(a.copy(legend = when (it) { 0 -> -1; 1 -> 0; else -> if (a.legend > 0) a.legend else 3 })) } },
        if (a.legend >= 0) ({ PxToggleRow("Legend box", "A frame behind the legend", a.legendFrame) { onChange(a.copy(legendFrame = it)) } }) else null,
        if (a.legend > 0) ({
            Column(PxRowPadding) {
                PxRowTexts("Place", null)
                Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (r in 0 until 3) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (c in 0 until 3) {
                            val p = r * 3 + c + 1
                            Box(Modifier.size(width = 56.dp, height = 32.dp).clip(RoundedCornerShape(8.dp)).background(if (p == a.legend) colors.primary else colors.surfaceContainerHighest).clickable { onChange(a.copy(legend = p)) })
                        }
                    }
                }
            }
        }) else null,
    )
    PxFootnote("A layer is in the legend when it has a name (its page › Legend).")
}

/** The figure's page: its title, layout, shape and look. */
@Composable
private fun FigureSettings(d: Figure.Doc, onChange: (Figure.Doc) -> Unit) {
    val colors = MaterialTheme.colorScheme
    PxHeader("Title")
    OutlinedTextField(d.title, { onChange(d.copy(title = it)) }, label = { Text(tr("Title over the figure")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    PxHeader("Layout")
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Figure.LAYOUTS.forEach { lay ->
            val on = lay == d.layout
            Box(Modifier.size(width = 84.dp, height = 64.dp).clip(RoundedCornerShape(16.dp)).background(if (on) colors.primaryContainer else colors.surfaceContainerHigh)
                .then(if (on) Modifier.border(2.dp, colors.primary, RoundedCornerShape(16.dp)) else Modifier).clickable { onChange(Figure.withLayout(d, lay)) }.padding(10.dp)) {
                val (cells, cols, rows) = Figure.cells(lay)
                Canvas(Modifier.fillMaxSize()) {
                    val cw = size.width / cols; val ch = size.height / rows
                    cells.forEach { c -> drawRoundRect(if (on) colors.primary else colors.onSurfaceVariant, Offset(c[0] * cw + 2, c[1] * ch + 2), androidx.compose.ui.geometry.Size(c[2] * cw - 4, c[3] * ch - 4), androidx.compose.ui.geometry.CornerRadius(6f, 6f), alpha = 0.55f) }
                }
            }
        }
    }
    PxHeader("Shape")
    val shapes = listOf(0.5, 0.667, 0.75, 1.0, 1.3)
    PxCards({ PxChoiceRow("Height", listOf("2 : 1", "3 : 2", "4 : 3", "1 : 1", "3 : 4"), nearest(shapes, d.aspect)) { onChange(d.copy(aspect = shapes[it])) } })
    PxHeader("Style")
    // As the colormaps: a card each, a little plot in the style's colors over its name.
    val app = appFigureTheme()
    Figure.THEMES.indices.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { k ->
                val t = if (k == 0) app else Figure.THEMES[k]
                val on = k == d.theme
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (on) colors.secondaryContainer else colors.surfaceContainerHighest)
                        .then(if (on) Modifier.border(2.dp, colors.primary, RoundedCornerShape(16.dp)) else Modifier).clickable { onChange(d.copy(theme = k)) }.padding(8.dp),
                ) {
                    Canvas(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp)).background(Color(t.background))) {
                        drawRect(Color(t.face), Offset(6f, 6f), androidx.compose.ui.geometry.Size(size.width - 12f, size.height - 12f))
                        for (g in 1..3) drawLine(Color(t.grid), Offset(6f + (size.width - 12f) * g / 4, 6f), Offset(6f + (size.width - 12f) * g / 4, size.height - 6f), 1f)
                        t.palette.take(3).forEachIndexed { c, col ->
                            val path = Path()
                            for (q in 0..24) { val x = 8f + (size.width - 16f) * q / 24; val y = size.height / 2 + (size.height * 0.28f) * kotlin.math.sin(q / 24f * 6.28f + c * 1.6f); if (q == 0) path.moveTo(x, y) else path.lineTo(x, y) }
                            drawPath(path, Color(col), style = Stroke(2.dp.toPx()))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(tr(if (k == 0) "App colors" else t.name), style = MaterialTheme.typography.labelLarge, color = if (on) colors.onSecondaryContainer else colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

/** The figure as a matplotlib script: to read, copy or share. */
@Composable
private fun PythonPage(d: Figure.Doc, built: Figure.Built?, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val code = remember(d, built) { built?.let { runCatching { FigurePython.write(d, it) }.getOrNull() } ?: "" }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    FullScreenPage("Python", onBack = onBack) {
        Text(tr("The same figure as a matplotlib script, with the numbers it was drawn from."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EditorChip(TableIcons.Copy, "Copy") { clipboard.setText(androidx.compose.ui.text.AnnotatedString(code)); Toast.makeText(context, tr("Copied"), Toast.LENGTH_SHORT).show() }
            EditorChip(TableIcons.Share, "Share .py") { shareBytes(context, safeFileName(d.name) + ".py", "text/x-python", code.toByteArray()) }
        }
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerHigh).horizontalScroll(rememberScrollState()).padding(16.dp)) {
            Text(code.lines().take(400).joinToString("\n") { if (it.length > 160) it.take(157) + "…" else it }, style = TextStyle(fontFamily = CasFonts.Mono, fontSize = 12.sp, color = colors.onSurface, lineHeight = 17.sp))
        }
    }
}

/** Share or save the figure: PDF, PNG, SVG, the Python script, or the figure file. */
@Composable
private fun ExportMenu(d: Figure.Doc, built: Figure.Built?) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun picture(f: ExportFormat) {
        val scene = built?.scene ?: return
        scope.launch {
            val bytes = withContext(Dispatchers.Default) { runCatching { SceneExport.bytes(context, scene, f) }.getOrNull() } ?: return@launch
            shareBytes(context, safeFileName(d.name) + "." + f.extension, f.mime, bytes)
        }
    }
    Box {
        IconButton(onClick = { open = true }) { AppIcon(TableIcons.Share, contentDescription = tr("Share figure"), tint = colors.onSurface) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(ExportFormat.PDF, ExportFormat.PNG, ExportFormat.SVG).forEach { f ->
                DropdownMenuItem(text = { Text(f.label) }, leadingIcon = { AppIcon(TableIcons.Share, null) }, onClick = { open = false; picture(f) })
            }
            DropdownMenuItem(text = { Text(tr("Python script")) }, leadingIcon = { AppIcon(FigureIcons.Python, null) }, onClick = {
                open = false
                built?.let { b -> shareBytes(context, safeFileName(d.name) + ".py", "text/x-python", FigurePython.write(d, b).toByteArray()) }
            })
            DropdownMenuItem(text = { Text(tr("Figure file")) }, leadingIcon = { AppIcon(TableIcons.Document, null) }, onClick = {
                open = false; shareBytes(context, safeFileName(d.name) + ".casfig", "application/octet-stream", Figure.encode(d).toByteArray())
            })
        }
    }
}

/** A figure in a notebook opened full screen: the same editor, with the notebook's definitions. */
@Composable
fun FigureEditorDialog(start: Figure.Doc, variables: Map<String, Expr>, functions: Map<String, UserFunction>, onSave: (Figure.Doc) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        KeyboardResizes()
        Surface(Modifier.fillMaxSize(), color = colors.surface) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                FigureEditor(start, eval = { figureEval(it, variables, functions) }, onSave = onSave, onClose = onDismiss)
            }
        }
    }
}
