package com.example.cas.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.cas.R
import com.example.cas.graph.ExportFormat
import com.example.cas.graph.Scene
import com.example.cas.graph.SvgWriter
import com.example.cas.graph.Viewport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Exporting a graph: a [Scene] written as PDF (the default), PNG, JPG or SVG. SVG is written
 * directly ([SvgWriter]); the others draw the scene onto an Android canvas, a PDF page or a
 * bitmap, so all four show the same picture.
 */
object SceneExport {
    /** Raster exports are this many pixels across. */
    private const val RASTER_WIDTH = 2400.0
    /** PDF pages are the scene's size times this, in points. */
    private const val PDF_SCALE = 1.5f

    fun bytes(context: Context, scene: Scene, format: ExportFormat): ByteArray = when (format) {
        ExportFormat.SVG -> {
            fun bytes(id: Int) = runCatching { context.resources.openRawResource(id).use { it.readBytes() } }.getOrNull()
            val fonts = listOfNotNull(bytes(R.font.cm_main)?.let { Scene.Font.Roman to it }, bytes(R.font.cm_italic)?.let { Scene.Font.Italic to it }, bytes(R.font.cm_size2)?.let { Scene.Font.Size2 to it }).toMap()
            SvgWriter.write(scene, fonts).toByteArray(Charsets.UTF_8)
        }
        ExportFormat.PDF -> pdf(context, scene)
        ExportFormat.STL -> error("STL is made from the 3D model, not a picture")
        ExportFormat.PNG, ExportFormat.JPG -> {
            val s = (RASTER_WIDTH / scene.width).toFloat()
            val bitmap = Bitmap.createBitmap((scene.width * s).toInt().coerceAtLeast(1), (scene.height * s).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            draw(android.graphics.Canvas(bitmap), scene, s, font(context))
            val out = java.io.ByteArrayOutputStream()
            bitmap.compress(if (format == ExportFormat.PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, if (format == ExportFormat.PNG) 100 else 95, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }

    private fun pdf(context: Context, scene: Scene): ByteArray {
        val doc = PdfDocument()
        val w = (scene.width * PDF_SCALE).toInt().coerceAtLeast(1)
        val h = (scene.height * PDF_SCALE).toInt().coerceAtLeast(1)
        val page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, 1).create())
        draw(page.canvas, scene, PDF_SCALE, font(context))
        doc.finishPage(page)
        val out = java.io.ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    /**
     * Gives the export's math layout ([com.example.cas.graph.MathScene]) the real widths of the
     * Computer Modern fonts, so what's exported sits exactly as laid out.
     */
    fun installMetrics(context: Context) {
        val f = font(context)
        val roman = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = f.roman }
        val italic = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = f.italic }
        com.example.cas.graph.MathScene.metrics = com.example.cas.graph.MathScene.Metrics { text, ital, size ->
            val p = if (ital) italic else roman
            synchronized(p) { p.textSize = 100f; p.measureText(text) * size / 100.0 }
        }
    }

    /** The typefaces labels use: Google Sans Flex, Computer Modern roman and italic, and TeX's Size2 (∫, ∮). */
    class Fonts(val sans: Typeface?, val roman: Typeface?, val italic: Typeface?, val size2: Typeface? = null)

    fun font(context: Context): Fonts {
        fun get(id: Int) = runCatching { ResourcesCompat.getFont(context, id) }.getOrNull()
        return Fonts(get(R.font.google_sans_flex), get(R.font.cm_main), get(R.font.cm_italic), get(R.font.cm_size2))
    }

    /** Draws [scene] at [s] pixels (or points) per unit. */
    fun draw(canvas: android.graphics.Canvas, scene: Scene, s: Float, fonts: Fonts) {
        canvas.drawColor(scene.background)
        canvas.save()
        canvas.clipRect(0f, 0f, (scene.width * s).toFloat(), (scene.height * s).toFloat())
        fun path(points: DoubleArray, closed: Boolean) = Path().apply {
            var k = 0
            while (k + 1 < points.size) {
                val x = (points[k] * s).toFloat(); val y = (points[k + 1] * s).toFloat()
                if (k == 0) moveTo(x, y) else lineTo(x, y)
                k += 2
            }
            if (closed) close()
        }
        for (item in scene.items) when (item) {
            is Scene.Stroke -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = item.color
                    strokeWidth = (item.strokeWidth * s).toFloat()
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    item.dash?.let { d -> pathEffect = DashPathEffect(FloatArray(d.size) { k -> (maxOf(d[k], 0.01) * s).toFloat() }, 0f) }
                }
                item.paths.forEach { if (it.size >= 4) canvas.drawPath(path(it, false), paint) }
            }
            is Scene.Fill -> {
                val all = Path()
                item.polygons.forEach { if (it.size >= 6) all.addPath(path(it, true)) }
                canvas.drawPath(all, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = item.color })
            }
            is Scene.Circle -> {
                val cx = (item.cx * s).toFloat(); val cy = (item.cy * s).toFloat(); val r = (item.r * s).toFloat()
                item.fill?.let { canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = it }) }
                item.stroke?.let { canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = it; strokeWidth = (item.strokeWidth * s).toFloat() }) }
            }
            is Scene.Label -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = item.color
                    textSize = (item.size * s).toFloat()
                    typeface = when (item.font) { Scene.Font.Sans -> fonts.sans; Scene.Font.Roman -> fonts.roman; Scene.Font.Italic -> fonts.italic; Scene.Font.Size2 -> fonts.size2 }
                    textAlign = when (item.anchor) { Scene.Anchor.Start -> Paint.Align.LEFT; Scene.Anchor.Middle -> Paint.Align.CENTER; Scene.Anchor.End -> Paint.Align.RIGHT }
                }
                val fm = paint.fontMetrics
                val x = (item.x * s).toFloat(); val y = (item.y * s).toFloat()
                if (item.angle != 0.0) { canvas.save(); canvas.rotate(-item.angle.toFloat(), x, y) }
                val spans = item.allSpans()
                if (item.baseline) {
                    canvas.drawText(item.text, x, y, paint)
                } else if (spans.size == 1 && spans[0].shift == 0 && item.spans == null) {
                    canvas.drawText(item.text, x, y - (fm.ascent + fm.descent) / 2, paint)
                } else {
                    // Mixed fonts and sizes: each run measured in its own font and size (exponents and
                    // indices smaller, raised or lowered), the whole placed by the anchor.
                    paint.textAlign = Paint.Align.LEFT
                    val paints = spans.map { sp ->
                        Paint(paint).apply {
                            if (sp.italic && item.font != Scene.Font.Italic) typeface = fonts.italic
                            textSize = (item.size * sp.scale * s).toFloat()
                        }
                    }
                    val widths = spans.mapIndexed { k, sp -> paints[k].measureText(sp.text) }
                    var at = x - when (item.anchor) { Scene.Anchor.Start -> 0f; Scene.Anchor.Middle -> widths.sum() / 2; Scene.Anchor.End -> widths.sum() }
                    spans.forEachIndexed { k, sp ->
                        val m = paints[k].fontMetrics
                        val mid = y - (sp.rise * item.size * s).toFloat()
                        canvas.drawText(sp.text, at, mid - (m.ascent + m.descent) / 2, paints[k])
                        at += widths[k]
                    }
                }
                if (item.angle != 0.0) canvas.restore()
            }
            is Scene.ClipStart -> {
                canvas.save()
                canvas.clipRect((item.x * s).toFloat(), (item.y * s).toFloat(), ((item.x + item.w) * s).toFloat(), ((item.y + item.h) * s).toFloat())
            }
            Scene.ClipEnd -> canvas.restore()
            is Scene.Image -> {
                val bmp = Bitmap.createBitmap(item.pixels, item.pixelWidth, item.pixelHeight, Bitmap.Config.ARGB_8888)
                val dst = RectF((item.x * s).toFloat(), (item.y * s).toFloat(), ((item.x + item.w) * s).toFloat(), ((item.y + item.h) * s).toFloat())
                canvas.drawBitmap(bmp, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
                bmp.recycle()
            }
        }
        canvas.restore()
    }
}

/** Exported graphs are square, this many units across (points in a PDF, before its 1.5× scale). */
const val EXPORT_SIZE = 400.0

/**
 * What to export: the format, the region of the plane (and the z range in 3D), and light or
 * dark colors. [preview] asks for a quick low-resolution picture for the dialog's thumbnail.
 */
class ExportRequest(val format: ExportFormat, val view: Viewport, val dark: Boolean, val z: Pair<Double, Double>? = null, val preview: Boolean = false)

/**
 * Returns a function that exports a graph: it builds the scene with [build] (off the main thread),
 * then saves it where you choose (the system's save dialog) or shares it.
 */
@Composable
fun rememberGraphExporter(
    name: String,
    build: (ExportRequest) -> Scene,
    /** Files that aren't pictures (STL from the 3D graph), made directly. */
    model: ((ExportRequest) -> ByteArray)? = null,
): (ExportRequest, Boolean) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<ExportRequest?>(null) }
    fun failed(e: Throwable) = Toast.makeText(context, "Couldn't export the graph" + (e.message?.let { ": $it" } ?: ""), Toast.LENGTH_LONG).show()
    // One save dialog per format, so each suggests the right file type.
    val launchers = ExportFormat.entries.associateWith { format ->
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(format.mime)) { uri ->
            val request = pending
            pending = null
            if (uri == null || request == null) return@rememberLauncherForActivityResult
            scope.launch {
                runCatching {
                    val data = withContext(Dispatchers.Default) { if (request.format == ExportFormat.STL && model != null) model(request) else SceneExport.bytes(context, build(request), request.format) }
                    withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(data) } ?: error("the file couldn't be opened") }
                }.onSuccess { Toast.makeText(context, "Graph saved", Toast.LENGTH_SHORT).show() }.onFailure(::failed)
            }
        }
    }
    // Files are named with when they were made, e.g. graph-2026-09-30_14-05-12.pdf.
    fun stamped() = name + "-" + java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(java.util.Date())
    return { request, share ->
        val fileName = stamped()
        if (share) {
            scope.launch {
                runCatching {
                    val uri = withContext(Dispatchers.Default) {
                        val data = if (request.format == ExportFormat.STL && model != null) model(request) else SceneExport.bytes(context, build(request), request.format)
                        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                        val file = File(dir, "$fileName.${request.format.extension}")
                        file.writeBytes(data)
                        FileProvider.getUriForFile(context, context.packageName + ".files", file)
                    }
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = request.format.mime
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(send, "Share graph"))
                }.onFailure(::failed)
            }
        } else {
            pending = request
            launchers.getValue(request.format).launch("$fileName.${request.format.extension}")
        }
    }
}

/**
 * Export settings: the format (PDF first), the limits (the current view to start with, and z in
 * 3D) and light or dark colors, with a preview of the result; then Save (choose where) or Share.
 */
@Composable
fun ExportDialog(
    view: Viewport,
    build: (ExportRequest) -> Scene,
    onExport: (ExportRequest, Boolean) -> Unit,
    onDismiss: () -> Unit,
    z: Pair<Double, Double>? = null,
    /** The formats offered: pictures, and STL for the 3D graph. */
    formats: List<ExportFormat> = ExportFormat.entries - ExportFormat.STL,
    /** The graph as a CasCalc graph file (.g2d, .g3d, .gcp), shared (true) or saved. */
    graphFile: com.example.cas.graph.GraphFile.Kind? = null,
    onGraphFile: (Boolean) -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var format by remember { mutableStateOf(ExportFormat.PDF) }
    var dark by remember { mutableStateOf(colors.surface.luminance() < 0.5f) }
    fun text(v: Double) = String.format(Locale.US, "%.4g", v).let { if ('e' in it) it else if ('.' in it) it.trimEnd('0').trimEnd('.') else it }
    val fields = remember { mutableStateListOf(text(view.xMin), text(view.xMax), text(view.yMin), text(view.yMax), text(z?.first ?: 0.0), text(z?.second ?: 0.0)) }
    fun num(t: String) = t.trim().replace("−", "-").replace(",", ".").toDoubleOrNull()?.takeIf { it.isFinite() }
    val limits = fields.map { num(it) }
    val pairs = listOf(0 to 1, 2 to 3) + if (z != null) listOf(4 to 5) else emptyList()
    val valid = pairs.all { (a, b) -> limits[a] != null && limits[b] != null && limits[a]!! < limits[b]!! }
    fun request(preview: Boolean = false) = ExportRequest(
        format, Viewport(limits[0]!!, limits[1]!!, limits[2]!!, limits[3]!!), dark,
        z = if (z != null) limits[4]!! to limits[5]!! else null, preview = preview,
    )
    // The preview: redrawn a moment after anything changes, off the main thread.
    var preview by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    androidx.compose.runtime.LaunchedEffect(valid, fields.toList(), dark) {
        if (!valid) return@LaunchedEffect
        kotlinx.coroutines.delay(250)
        val r = request(preview = true)
        runCatching {
            withContext(Dispatchers.Default) {
                val scene = build(r)
                val s = (480 / scene.width).toFloat()
                val bmp = Bitmap.createBitmap((scene.width * s).toInt().coerceAtLeast(1), (scene.height * s).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                SceneExport.draw(android.graphics.Canvas(bmp), scene, s, SceneExport.font(context))
                bmp
            }
        }.onSuccess { preview = it.asImageBitmap() }
    }
    // What the file will look like.
    val previewBox: @Composable () -> Unit = {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp))
                .border(1.dp, colors.outlineVariant, RoundedCornerShape(16.dp))
                .background(colors.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            val p = preview
            if (p != null) androidx.compose.foundation.Image(p, contentDescription = "Preview of the export", modifier = Modifier.fillMaxSize())
            else Busy()
        }
    }
    val options: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            formats.forEachIndexed { k, f ->
                SegmentedButton(
                    selected = format == f,
                    onClick = { format = f },
                    shape = SegmentedButtonDefaults.itemShape(k, formats.size),
                    icon = {},
                    label = { Text(f.label, maxLines = 1) },
                )
            }
        }
        Text(
            when (format) {
                ExportFormat.PDF -> "Vector: sharp at any size, for documents and printing."
                ExportFormat.SVG -> "Vector: sharp at any size, editable in drawing programs."
                ExportFormat.PNG -> "Picture, 2400 × 2400 pixels, without loss."
                ExportFormat.JPG -> "Picture, 2400 × 2400 pixels, smaller files."
                ExportFormat.STL -> "3D model for printing: each surface closed off into a solid inside the box (the floor under z = f(x, y)), 100 mm across. The preview shows the graph."
            },
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
        )
        Text("Limits", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
        pairs.forEach { (a, b) ->
            val letter = listOf("x", "y", "z")[a / 2]
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(fields[a], { fields[a] = it }, singleLine = true, label = { Text("$letter from") }, modifier = Modifier.weight(1f))
                OutlinedTextField(fields[b], { fields[b] = it }, singleLine = true, label = { Text("$letter to") }, modifier = Modifier.weight(1f))
            }
        }
        if (!valid) Text("Each limit needs a number, and “from” must be less than “to”.", style = MaterialTheme.typography.bodySmall, color = colors.error)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("Light", "Dark").forEachIndexed { k, name ->
                SegmentedButton(
                    selected = dark == (k == 1),
                    onClick = { dark = k == 1 },
                    shape = SegmentedButtonDefaults.itemShape(k, 2),
                    icon = {},
                    label = { Text(name, maxLines = 1) },
                )
            }
        }
        // The graph itself, to open again in CasCalc.
        if (graphFile != null) {
            androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Text("Graph file (.${graphFile.extension})", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            Text(
                "The graph itself, with its lines, colors and sliders, to open again in CAS Scientific Calculator (Saved graphs › Import).",
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.FilledTonalButton(onClick = { onGraphFile(true) }) { Text("Share file") }
                androidx.compose.material3.FilledTonalButton(onClick = { onGraphFile(false) }) { Text("Save file") }
            }
        }
    }
    val buttons: @Composable () -> Unit = {
        Row {
            TextButton(enabled = valid, onClick = { onExport(request(), true) }) { Text("Share") }
            TextButton(enabled = valid, onClick = { onExport(request(), false) }) { Text("Save") }
        }
    }
    if (isTabletLayout()) {
        // Tablets: the preview large on the left, the options beside it.
        androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            androidx.compose.material3.Surface(shape = RoundedCornerShape(28.dp), color = colors.surfaceContainerHigh, modifier = Modifier.width(940.dp).heightIn(max = 720.dp)) {
                Column(Modifier.padding(24.dp)) {
                    Text("Export graph", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.weight(1f, fill = false), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Box(Modifier.weight(1.1f)) { previewBox() }
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) { options() }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        buttons()
                    }
                }
            }
        }
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export graph") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                previewBox()
                options()
            }
        },
        confirmButton = { buttons() },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
