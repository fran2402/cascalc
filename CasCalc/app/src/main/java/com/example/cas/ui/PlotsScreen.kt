package com.example.cas.ui

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
    var open by mutableStateOf(false)
    /** The figure being edited: a saved one, or a new one not saved yet. */
    var doc by mutableStateOf<Figure.Doc?>(null)
    var request by mutableStateOf<Figure.Doc?>(null)

    fun show(d: Figure.Doc) { doc = d; open = true }
}

/** New figures: blank, from templates, from a table, from the 2D graph's lines. */
object FigureFactory {
    private fun m(text: String) = MathRow(text.map { Sym(it.toString()) }.toMutableList())

    fun blank(name: String = "") = Figure.Doc(FigureStore.newId(), "", listOf(Figure.Axes()), name = name, edited = System.currentTimeMillis())

    /** A figure of a table: its first two letter-named columns as [kind] (scatter, line, bars, histogram, box). */
    fun fromTable(t: DataTable, kind: Figure.Kind): Figure.Doc {
        val letters = t.names.map { it.trim() }.filter { n -> n.isNotEmpty() && n.all { it.isLetter() } }
        val a = letters.getOrNull(0) ?: "x"; val b = letters.getOrNull(1) ?: a
        val layer = when (kind) {
            Figure.Kind.Scatter -> Figure.Layer(kind, listOf(m(a), m(b)))
            Figure.Kind.Plot -> Figure.Layer(kind, listOf(m(b), m(a)), style = Figure.Style(marker = 0))
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
        val l = Figure.Layer(Figure.Kind.Plot, listOf(MathCodec.copy(row)), style = Figure.Style(label = "$" + com.example.cas.engine.Latex.of(row) + "$"))
        return Figure.Doc(FigureStore.newId(), "", listOf(Figure.Axes(listOf(l), xLabel = "\$x\$", legend = 0)), name = "Figure", edited = System.currentTimeMillis())
    }

    /**
     * The 2D graph as a figure, over its view: curves y = f(x) as lines and r = f(θ) as polar
     * curves, points and lists of points as scatters (their numbers in the figure's table), each
     * in its line's color, style, thickness and point shape.
     */
    fun fromGraph(functions: List<PlotFunction>, view: com.example.cas.graph.Viewport?): Figure.Doc {
        val names = ArrayList<String>(); val columns = ArrayList<List<String>>()
        val pairs = listOf("x" to "y", "u" to "v", "p" to "q", "s" to "w") + ('a'..'h').map { "x$it" to "y$it" }
        val layers = ArrayList<Figure.Layer>()
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
        for (f in functions) when (val k = f.plot) {
            is Plot2DKind.Explicit -> rhs(f.editor.root, "y")?.takeIf { it.items.isNotEmpty() }?.let { r ->
                layers += Figure.Layer(Figure.Kind.Plot, listOf(r), style = style(f, "$" + com.example.cas.engine.Latex.of(r) + "$"))
            }
            is Plot2DKind.Polar -> rhs(f.editor.root, "r")?.takeIf { it.items.isNotEmpty() }?.let { r ->
                layers += Figure.Layer(Figure.Kind.Polar, listOf(r), style = style(f, "\$r = " + com.example.cas.engine.Latex.of(r) + "\$"))
            }
            is Plot2DKind.PointList, is Plot2DKind.Point -> {
                val (xs, ys) = when (k) {
                    is Plot2DKind.PointList -> k.xs to k.ys
                    is Plot2DKind.Point -> runCatching { doubleArrayOf(k.x(DoubleArray(8))) to doubleArrayOf(k.y(DoubleArray(8))) }.getOrNull() ?: continue
                    else -> continue
                }
                val (a, b) = pairs.getOrNull(names.size / 2) ?: continue
                names += a; names += b
                columns += xs.map(::num); columns += ys.map(::num)
                val label = if (xs.size == 1) "(${num(xs[0])}, ${num(ys[0])})" else "Data"
                layers += Figure.Layer(Figure.Kind.Scatter, listOf(m(a), m(b)), style = style(f, label).copy(marker = f.pointShape, size = 3.0))
            }
            else -> Unit
        }
        val n = columns.maxOfOrNull { it.size } ?: 0
        val table = if (names.isEmpty()) Figure.emptyData() else DataTable(names, columns.map { c -> c + List(n - c.size) { "" } }, null, null)
        val axes = Figure.Axes(layers, xLabel = "\$x\$", yLabel = "\$y\$", xMin = view?.xMin, xMax = view?.xMax, yMin = view?.yMin, yMax = view?.yMax, legend = 0)
        return Figure.Doc(FigureStore.newId(), "", listOf(axes), data = table, name = "From 2D graph", edited = System.currentTimeMillis())
    }

    class Template(val name: String, val detail: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val make: () -> Figure.Doc)

    val templates: List<Template> get() = listOf(
        Template("Blank", "An empty figure", FigureIcons.NewFigure) { blank("Figure") },
        Template("Function", "A curve with its legend", Figure.Kind.Plot.let { FigureIcons.of(it) }) {
            val row = MathRow(mutableListOf(com.example.cas.editor.Func("sin", listOf(m("x")))))
            Figure.Doc(FigureStore.newId(), "", listOf(Figure.Axes(listOf(Figure.Layer(Figure.Kind.Plot, listOf(row), style = Figure.Style(label = "\$\\sin x\$"))), xLabel = "\$x\$", yLabel = "\$y\$")), name = "Function", edited = System.currentTimeMillis())
        },
        Template("Data", "A table, as points and a histogram", FigureIcons.of(Figure.Kind.Hist)) {
            val t = DataTable(listOf("x", "y"), listOf(listOf("1", "2", "3", "4", "5", "6", "7", "8"), listOf("2.1", "3.9", "6.2", "7.8", "10.1", "12.2", "13.8", "16.1")), null, null)
            Figure.withLayout(Figure.Doc(FigureStore.newId(), "", listOf(
                Figure.Axes(listOf(Figure.Layer(Figure.Kind.Scatter, listOf(m("x"), m("y")), style = Figure.Style(label = "data")), Figure.Layer(Figure.Kind.Plot, listOf(m("2x")), style = Figure.Style(line = 1, label = "\$y = 2x\$"))), xLabel = "\$x\$", yLabel = "\$y\$"),
                Figure.Axes(listOf(Figure.Layer(Figure.Kind.Hist, listOf(m("y")))), xLabel = "\$y\$", yLabel = "count"),
            ), "AB", aspect = 0.5, data = t, name = "Data", edited = System.currentTimeMillis()), "AB")
        },
        Template("Fields", "Contours and arrows of f(x, y)", FigureIcons.of(Figure.Kind.ContourF)) {
            val f = MathRow(mutableListOf(Sym("x"), com.example.cas.editor.Pow(m("2")), Sym("−"), Sym("y"), com.example.cas.editor.Pow(m("2"))))
            Figure.Doc(FigureStore.newId(), "", listOf(Figure.Axes(listOf(Figure.Layer(Figure.Kind.ContourF, listOf(f), style = Figure.Style(levels = 12)), Figure.Layer(Figure.Kind.Quiver, listOf(m("2x"), m("−2y")), style = Figure.Style(color = 5))), equal = true, grid = 0)), name = "Fields", edited = System.currentTimeMillis())
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
    val vars = variables + Figure.tableVariables(d.data)
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

// ---- The page -----------------------------------------------------------------------------------

/**
 * Plots, full screen (from the 2D graph's Open as figure, a table's Chart, or the saved
 * graphs): the figure being edited. Back returns to where it came from, asking first whether
 * to save changes. Turned on in Settings › Graphs.
 */
@Composable
fun PlotsPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val page = rememberPageMotion(onBack)
    val start = PlotsNav.doc ?: run { LaunchedEffect(Unit) { onBack() }; return }
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        page.Content {
            Surface(Modifier.fillMaxSize(), color = colors.surface) {
                Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                    key(start.id) {
                        FigureEditor(start, eval = { figureEval(it) }, autosave = false,
                            onSave = { d -> runCatching { FigureStore.save(context, d) }; PlotsNav.doc = d },
                            onClose = page.close)
                    }
                }
            }
        }
    }
}

// ---- The editor --------------------------------------------------------------------------------

/** The keypad typing into one [Editor] at a time (a layer's input). */
private class FieldKeys(private val target: () -> Editor?, private val version: () -> Int, private val onEnter: () -> Unit) : KeypadHost {
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
    override fun insertMatrix(rows: Int, cols: Int) { target()?.insert(Matrix(rows, cols, growable = true), 0) }
    override val canUndo get() = version().let { target()?.canUndo ?: false }
    override val canRedo get() = version().let { target()?.canRedo ?: false }
    override fun undo() { target()?.undo() }
    override fun redo() { target()?.redo() }
    override var keypadHidden: Boolean
        get() = hidden
        set(v) { hidden = v }
    override val listKey: Boolean get() = true
    override val coordinates get() = KeypadState.coordinates
    override fun selectCoordinates(c: com.example.cas.cas.Coordinates) = KeypadState.selectCoordinates(c)
    override fun coordinatesOf(kind: com.example.cas.cas.CoordinateKind) = KeypadState.coordinatesOf(kind)
    override val unitSystem get() = KeypadState.unitSystem
    override fun selectUnitSystem(units: com.example.cas.engine.UnitSystem) = KeypadState.selectUnitSystem(units)
}

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
    val save = { onSave(doc); savedDoc = doc }
    val close = { if (autosave) { onSave(doc); onClose() } else if (dirty) asking = true else onClose() }
    BackHandler { if (selected != null) selected = null else close() }
    if (asking) androidx.compose.material3.AlertDialog(
        onDismissRequest = { asking = false },
        title = { Text(tr(if (savedDoc == null) "Save this figure?" else "Save changes?")) },
        text = { Text(tr(if (savedDoc == null) "It goes into Saved graphs, under Plots." else "Your changes to this figure aren't saved yet.")) },
        confirmButton = { TextButton(onClick = { asking = false; save(); onClose() }) { Text(tr("Save")) } },
        dismissButton = {
            Row {
                TextButton(onClick = { asking = false; onClose() }) { Text(tr("Discard"), color = colors.error) }
                TextButton(onClick = { asking = false }) { Text(tr("Cancel")) }
            }
        },
    )

    // The open layer's inputs, each an editor the keypad types into.
    val sel = selected?.takeIf { it in axes.layers.indices }
    val layer = sel?.let { axes.layers[it] }
    var version by remember { mutableIntStateOf(0) }
    var focus by remember { mutableStateOf<Int?>(null) }
    val editors = remember(axesAt, sel, layer?.kind) {
        layer?.kind?.inputs?.indices?.map { k -> Editor(MathCodec.copy(layer.input(k))) } ?: emptyList()
    }
    LaunchedEffect(editors) { focus = null }
    editors.forEach { e ->
        e.onChange = {
            version++
            val s = selected; val l = s?.let { axes.layers.getOrNull(it) }
            if (s != null && l != null) setLayer(s, l.copy(inputs = editors.map { MathCodec.copy(it.root) }), record = false)
        }
    }
    val fieldKeys = remember(editors) {
        FieldKeys(target = { focus?.let { editors.getOrNull(it) } }, version = { version }, onEnter = {
            val f = focus
            focus = if (f != null && f + 1 < editors.size) f + 1 else null
        })
    }
    val typing = focus != null && layer != null

    val pageDone = { page = FigurePage.None }
    when (page) {
        FigurePage.Gallery -> LayerGallery(onPick = { kind ->
            page = FigurePage.None
            val l = newLayer(kind)
            setAxes(axes.copy(layers = axes.layers + l))
            selected = axes.layers.size
        }, onDismiss = pageDone)
        // As the graphs' settings: changes show at once; Cancel puts back how it was.
        FigurePage.Axes -> {
            val before = remember { doc }
            GraphSettingsFrame("", true, onDone = pageDone, onDismiss = { doc = before; pageDone() }, title = "Axes") { AxesSettings(axes) { setAxes(it) } }
        }
        FigurePage.Figure -> {
            val before = remember { doc }
            GraphSettingsFrame("", true, onDone = pageDone, onDismiss = { doc = before; axesAt = axesAt.coerceAtMost(before.axes.lastIndex); pageDone() }, title = "Figure") {
                FigureSettings(doc) { d -> change(d); axesAt = axesAt.coerceAtMost(d.axes.lastIndex) }
            }
        }
        FigurePage.Python -> PythonPage(doc, built, onBack = pageDone)
        FigurePage.None -> Unit
    }
    if (dataOpen) DataTableEditor(doc.data, key = doc.id + "data", onSave = { change(doc.copy(data = it)) }, onDismiss = { dataOpen = false })

    val ratio = (1 / doc.aspect).toFloat().coerceIn(0.5f, 3f)

    @Composable
    fun Preview(modifier: Modifier, compact: Boolean = false) {
        Box((if (compact) modifier.height(170.dp) else modifier.aspectRatio(ratio)).clip(RoundedCornerShape(20.dp)).background(Color(if (doc.theme == 0) theme.background else Figure.THEMES.getOrElse(doc.theme) { theme }.background))) {
            FigureCanvas(built?.scene, Modifier.fillMaxSize())
            // The current axes' cell outlined, when there are several.
            val (cells, cols, rows) = Figure.cells(doc.layout)
            if (cells.size > 1) Canvas(Modifier.fillMaxSize()) {
                val cell = cells.getOrNull(axesAt) ?: return@Canvas
                val top = if (doc.title.isNotBlank()) size.height * 24f / (480f * doc.aspect.toFloat()) else 0f
                val cw = size.width / cols; val ch = (size.height - top) / rows
                drawRoundRect(colors.primary, Offset(cell[0] * cw + 3, top + cell[1] * ch + 3), androidx.compose.ui.geometry.Size(cell[2] * cw - 6, cell[3] * ch - 6),
                    androidx.compose.ui.geometry.CornerRadius(14f, 14f), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
            }
        }
    }

    @Composable
    fun Controls(modifier: Modifier) {
        Box(modifier) {
            if (layer != null && sel != null) {
                LayerEditor(
                    layer, built?.error(axesAt, layer), built?.worked?.getOrNull(axesAt)?.drawn?.firstOrNull { it.layer === layer }?.color,
                    columns = doc.data.names.map { it.trim() }.filter { n -> n.isNotEmpty() && n.all { it.isLetter() } },
                    onColumn = { name ->
                        val e = editors.getOrNull(focus ?: 0) ?: return@LayerEditor
                        if (focus == null) { focus = 0; e.setCursor(e.root, e.root.items.size) }
                        name.forEach { c -> e.type(c.toString()) }
                    },
                    onEditData = { dataOpen = true },
                    onImport = { t -> change(doc.copy(data = t)) },
                    editors = editors, focus = focus, version = version,
                    onFocus = { k -> focus = k; fieldKeys.keypadHidden = false },
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
                    onDelete = { setAxes(axes.copy(layers = axes.layers.filterIndexed { i, _ -> i != sel })); selected = null },
                )
            } else {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val letters = doc.layout.split(';').joinToString("") { it.trim() }.filter { it != '.' }.toList().distinct()
                        if (letters.size > 1) letters.forEachIndexed { k, ch ->
                            val on = k == axesAt
                            Box(Modifier.size(40.dp).clip(CircleShape).background(if (on) colors.primary else colors.surfaceContainerHighest).clickable { axesAt = k }, contentAlignment = Alignment.Center) {
                                Text(ch.toString(), color = if (on) colors.onPrimary else colors.onSurface, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        EditorChip(FigureIcons.Axes, "Axes") { page = FigurePage.Axes }
                        EditorChip(FigureIcons.Layout, "Figure") { page = FigurePage.Figure }
                        EditorChip(TableIcons.Table, "Data") { dataOpen = true }
                        EditorChip(FigureIcons.Python, "Python") { page = FigurePage.Python }
                    }
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(axes.layers) { k, l ->
                            val dr = built?.worked?.getOrNull(axesAt)?.drawn?.firstOrNull { it.layer === l }
                            LayerRow(l, dr?.color, built?.error(axesAt, l), onOpen = { selected = k }, onToggle = { setLayer(k, l.copy(visible = !l.visible)) })
                        }
                        if (axes.layers.isEmpty()) item { PxFootnote("Add a layer with +: a line, points, bars, a histogram, a box plot, contours… Its inputs are math: an expression in \$x\$, a list like [1, 4, 9], or a column of the figure's table.") }
                    }
                }
            }
        }
    }

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
            if (!autosave) IconButton(enabled = dirty, onClick = save) {
                AppIcon(TableIcons.Check, contentDescription = tr("Save"), tint = if (dirty) colors.primary else colors.outline)
            }
            ExportMenu(doc, built)
        }
        if (tablet) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.weight(1.3f).fillMaxHeight().padding(12.dp), verticalArrangement = Arrangement.Center) { Preview(Modifier.fillMaxWidth()) }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    Controls(Modifier.fillMaxSize())
                    if (layer == null) AddLayerButton(Modifier.align(Alignment.BottomEnd).padding(20.dp)) { page = FigurePage.Gallery }
                }
                if (typing) Keypad(fieldKeys, Modifier.width(tabletKeypadWidth()).fillMaxHeight().background(colors.surfaceContainerLow), tablet = true)
            }
        } else {
            // While typing, a short strip, so the inputs stay in view.
            Preview(Modifier.padding(horizontal = 12.dp).fillMaxWidth(), compact = typing)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Controls(Modifier.fillMaxSize())
                if (layer == null) AddLayerButton(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp)) { page = FigurePage.Gallery }
                if (typing && fieldKeys.keypadHidden) ShowKeypadButton(onClick = { fieldKeys.keypadHidden = false }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp))
            }
            if (typing && !fieldKeys.keypadHidden) Keypad(fieldKeys)
        }
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

/** A layer in the list: its icon in its color, its type and inputs, any error, and the eye that hides it. */
@Composable
private fun LayerRow(l: Figure.Layer, color: Int?, error: String?, onOpen: () -> Unit, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint = color?.let { Color(it).copy(alpha = 1f) } ?: colors.outline
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surfaceContainerHigh).clickable(onClick = onOpen).padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(tint.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
            AppIcon(FigureIcons.of(l.kind), contentDescription = null, tint = colors.onSurface, accent = tint)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr(l.kind.title), style = MaterialTheme.typography.titleSmall, color = if (l.visible) colors.onSurface else colors.outline)
                Spacer(Modifier.width(6.dp))
                Text("plt." + l.kind.py, style = TextStyle(fontFamily = CasFonts.Mono, fontSize = 12.sp, color = colors.onSurfaceVariant))
            }
            val first = l.inputs.firstOrNull { it.items.isNotEmpty() }
            when {
                error != null -> Text(tr(error), style = MaterialTheme.typography.bodySmall, color = colors.error, maxLines = 2)
                first != null -> Box(Modifier.horizontalScroll(rememberScrollState())) { MathView(first, 16.sp, if (l.visible) colors.onSurface else colors.outline) }
                l.style.label.isNotBlank() -> MathText(l.style.label, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                else -> Text(tr("Tap to set its inputs"), style = MaterialTheme.typography.bodySmall, color = colors.outline)
            }
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

/** A layer's input: its name, and the math in a box the keypad types into. */
@Composable
private fun InputField(label: String, e: Editor, focused: Boolean, version: Int, onFocus: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (focused) colors.primary else colors.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp, bottom = 4.dp))
        Box(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainerHighest)
                .then(if (focused) Modifier.border(2.dp, colors.primary, RoundedCornerShape(16.dp)) else Modifier)
                .clickable { e.setCursor(e.root, e.root.items.size); onFocus() }.padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.horizontalScroll(rememberScrollState())) {
                if (e.isEmpty && !focused) Text(tr("empty"), style = MaterialTheme.typography.bodyMedium, color = colors.outline)
                else MathView(e.root, 20.sp, colors.onSurface, accent = colors.primary, cursorRow = if (focused) e.row else null, cursorIndex = e.index, version = version,
                    onTap = { r, i -> e.setCursor(r, i); onFocus() })
            }
        }
    }
}

private val WIDTHS = listOf(0.5, 0.75, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0)
private val ALPHAS = listOf(0.15, 0.3, 0.5, 0.7, 0.85, 1.0)
private val SIZES = listOf(1.5, 2.0, 3.0, 4.0, 5.0, 7.0)
private val LEVELS = listOf(4, 6, 8, 10, 12, 16, 20, 30)
private val BINS = listOf(0, 5, 10, 20, 40)
private val CMAPS = listOf("viridis", "plasma", "magma", "inferno", "cividis", "turbo", "coolwarm", "RdBu", "Spectral", "twilight", "gray", "theme")

private fun nearest(list: List<Double>, v: Double) = list.indices.minByOrNull { kotlin.math.abs(list[it] - v) } ?: 0

/**
 * The open layer: its inputs on the keypad, the figure's data (its columns as chips that type
 * themselves into the input), its text, its look (the same Color dialog as a graph's line, with
 * the layer's own options under it), the legend name, and moving, copying or deleting it.
 */
@Composable
private fun LayerEditor(
    l: Figure.Layer, error: String?, color: Int?, columns: List<String>, onColumn: (String) -> Unit, onEditData: () -> Unit, onImport: (com.example.cas.graph.DataTable) -> Unit,
    editors: List<Editor>, focus: Int?, version: Int,
    onFocus: (Int?) -> Unit, onChange: (Figure.Layer) -> Unit, onClose: () -> Unit, onMove: (Int) -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val s = l.style
    var look by remember { mutableStateOf(false) }
    val tint = color?.let { Color(it) } ?: colors.primary
    if (look) LayerLookDialog(l, tint, onApply = { onChange(l.copy(style = it)); look = false }, onDismiss = { look = false })
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val text = context.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
            onImport(com.example.cas.graph.DataTable.fromCsv(com.example.cas.graph.Csv.parse(text)))
        }.onFailure { Toast.makeText(context, tr("Couldn't read that file as a table"), Toast.LENGTH_LONG).show() }
    }
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
        editors.forEachIndexed { k, e -> InputField(l.kind.inputs[k], e, focus == k, version) { onFocus(k) } }
        if (error != null) Text(tr(error), style = MaterialTheme.typography.bodyMedium, color = colors.error)
        l.kind.text?.let { name ->
            OutlinedTextField(l.text, { onChange(l.copy(text = it)) }, label = { Text(tr(name)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                supportingText = { Text(tr(if (name == "Labels") "Split by commas" else "\$math\$ between dollar signs")) })
        }
        PxHeader("Data")
        if (columns.isNotEmpty()) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            columns.forEach { c ->
                Box(Modifier.height(36.dp).widthIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).background(colors.secondaryContainer).clickable(onClickLabel = tr("Use {0}", c)) { onColumn(c) }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    MathView(MathRow(c.map { Sym(it.toString()) }.toMutableList()), 18.sp, colors.onSecondaryContainer)
                }
            }
        }
        Text(tr(if (columns.isEmpty()) "The figure's table has no columns named with letters yet." else "Tap a column to type it into the input."), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EditorChip(TableIcons.Table, "Edit table", onEditData)
            EditorChip(TableIcons.ImportData, "Import CSV") { importer.launch(arrayOf("text/*", "application/csv", "*/*")) }
        }
        PxHeader("Look")
        Row(
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
        OutlinedTextField(s.label, { onChange(l.copy(style = s.copy(label = it))) }, label = { Text(tr("Name in the legend")) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            supportingText = { Text(tr(if (l.kind == Figure.Kind.Fit) "Empty: its equation and R²" else "Empty: not in the legend. \$math\$ between dollar signs")) })
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
 * (marker, size, opacity, bins, colormap, levels, fitted model). Nothing changes until Done.
 */
@Composable
private fun LayerLookDialog(l: Figure.Layer, current: Color, onApply: (Figure.Style) -> Unit, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf(l.style) }
    var line by remember { mutableStateOf(l.style.line) }
    var width by remember { mutableStateOf((l.style.width / 0.6).toFloat().coerceIn(1f, 8f)) }
    ColorPickerDialog(
        initial = current,
        onPick = { c -> onApply(draft.copy(argb = c?.toArgb() ?: 0, color = if (c == null) -1 else draft.color, line = line, width = width * 0.6)) },
        onDismiss = onDismiss,
        lineStyle = if (l.kind.lined || l.kind == Figure.Kind.Events) line else null,
        thickness = width,
        onStyle = { st, w -> line = st; width = w },
        title = "Look",
        extra = {
            val k = l.kind
            androidx.compose.runtime.CompositionLocalProvider(LocalCompactRows provides true) {
                if (k == Figure.Kind.Fit) PxChoiceRow("Model", com.example.cas.graph.FitModel.entries.map { it.title }, draft.model.coerceIn(0, com.example.cas.graph.FitModel.entries.lastIndex)) { draft = draft.copy(model = it) }
                if (k.lined && k != Figure.Kind.Contour && k != Figure.Kind.Quiver && k != Figure.Kind.Stream && k != Figure.Kind.Kde) MarkerChoice(draft.marker) { draft = draft.copy(marker = it) }
                if (k == Figure.Kind.Scatter || k == Figure.Kind.Fit || draft.marker >= 0) PxSteppedRow("Marker size", null, nearest(SIZES, draft.size), SIZES.indices) { draft = draft.copy(size = SIZES[it]) }
                PxSteppedRow("Opacity", "${(ALPHAS[nearest(ALPHAS, draft.alpha)] * 100).toInt()} %", nearest(ALPHAS, draft.alpha), ALPHAS.indices) { draft = draft.copy(alpha = ALPHAS[it]) }
                if (k == Figure.Kind.Hist || k == Figure.Kind.Hist2D || k == Figure.Kind.Hexbin) {
                    PxChoiceRow("Bins", BINS.map { if (it == 0) "Auto" else "$it" }, BINS.indexOf(draft.bins).coerceAtLeast(0)) { draft = draft.copy(bins = BINS[it]) }
                    if (k == Figure.Kind.Hist) {
                        PxToggleRow("Density", "Areas add up to 1 (to compare with a density curve)", draft.density) { draft = draft.copy(density = it) }
                        PxToggleRow("Cumulative", "Running totals", draft.cumulative) { draft = draft.copy(cumulative = it) }
                        PxToggleRow("Outline only", "histtype=\"step\"", draft.line == 1) { draft = draft.copy(line = if (it) 1 else 0); line = if (it) 1 else 0 }
                    }
                }
                if (k.mapped) {
                    ColormapChoice(draft.cmap) { draft = draft.copy(cmap = it) }
                    if (k == Figure.Kind.Contour || k == Figure.Kind.ContourF) PxSteppedRow("Levels", LEVELS[LEVELS.indexOf(draft.levels).coerceAtLeast(0)].toString(), LEVELS.indexOf(draft.levels).coerceAtLeast(3), LEVELS.indices) { draft = draft.copy(levels = LEVELS[it]) }
                }
            }
        },
    )
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

@Composable
private fun ColormapChoice(selected: String, onPick: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.padding(vertical = 6.dp)) {
        PxRowTexts("Colormap", com.example.cas.graph.Colormap.byName(selected).label)
        Row(Modifier.fillMaxWidth().padding(top = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CMAPS.forEach { name ->
                val map = com.example.cas.graph.Colormap.byName(name)
                val on = name == selected
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onPick(name) }.padding(4.dp)) {
                    Box(Modifier.width(64.dp).height(24.dp).clip(RoundedCornerShape(8.dp)).background(Brush.horizontalGradient((0..8).map { Color(0xFF000000.toInt() or map.rgb(it / 8.0)) }))
                        .then(if (on) Modifier.border(3.dp, colors.primary, RoundedCornerShape(8.dp)) else Modifier))
                    Text(map.label, style = MaterialTheme.typography.labelSmall, color = if (on) colors.primary else colors.onSurfaceVariant)
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
        Text(tr("\$math\$ between dollar signs: \$t\$ (s), \$\\sigma^2\$"), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
    PxHeader("Limits")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField("x from", a.xMin, Modifier.weight(1f)) { onChange(a.copy(xMin = it)) }
        NumberField("x to", a.xMax, Modifier.weight(1f)) { onChange(a.copy(xMax = it)) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField("y from", a.yMin, Modifier.weight(1f)) { onChange(a.copy(yMin = it)) }
        NumberField("y to", a.yMax, Modifier.weight(1f)) { onChange(a.copy(yMax = it)) }
    }
    PxHeader("Scales")
    PxCards(
        { PxToggleRow("Logarithmic x", "set_xscale(\"log\")", a.xLog) { onChange(a.copy(xLog = it)) } },
        { PxToggleRow("Logarithmic y", "set_yscale(\"log\")", a.yLog) { onChange(a.copy(yLog = it)) } },
        { PxToggleRow("Equal scales", "A unit as long on both axes", a.equal) { onChange(a.copy(equal = it)) } },
    )
    PxHeader("Look")
    PxCards(
        { PxChoiceRow("Grid", listOf("Off", "Major", "Minor too"), a.grid.coerceIn(0, 2)) { onChange(a.copy(grid = it)) } },
        { PxChoiceRow("Frame", listOf("Box", "L", "At zero", "None"), a.spines.coerceIn(0, 3)) { onChange(a.copy(spines = it)) } },
    )
    PxHeader("Legend")
    PxCards(
        { PxChoiceRow("Legend", listOf("Off", "Best", "Placed"), when { a.legend < 0 -> 0; a.legend == 0 -> 1; else -> 2 }) { onChange(a.copy(legend = when (it) { 0 -> -1; 1 -> 0; else -> if (a.legend > 0) a.legend else 3 })) } },
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
        Surface(Modifier.fillMaxSize(), color = colors.surface) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                FigureEditor(start, eval = { figureEval(it, variables, functions) }, onSave = onSave, onClose = onDismiss)
            }
        }
    }
}
