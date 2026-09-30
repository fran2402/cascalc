package com.example.cas.graph

import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.Deflater

/**
 * A graph as a list of simple shapes, for exporting: lines, filled shapes, dots, text and
 * images, in units where the whole drawing is [width] × [height] (y down, like the screen).
 * The same scene is written as SVG here, and drawn onto an Android canvas for PDF, PNG and
 * JPG, so every format shows the same picture. Colors are ARGB, alpha included.
 */
class Scene(val width: Double, val height: Double, val background: Int) {
    sealed class Item

    /** Open polylines, each as x₀, y₀, x₁, y₁…; [dash] is on and off lengths, or null for solid. */
    class Stroke(val paths: List<DoubleArray>, val color: Int, val strokeWidth: Double, val dash: DoubleArray? = null) : Item()

    /** Closed polygons filled together (touching ones don't double up). */
    class Fill(val polygons: List<DoubleArray>, val color: Int) : Item()

    class Circle(val cx: Double, val cy: Double, val r: Double, val fill: Int?, val stroke: Int? = null, val strokeWidth: Double = 0.0) : Item()

    enum class Anchor { Start, Middle, End }

    /** Sans is the app's Google Sans Flex; Roman and Italic are LaTeX's Computer Modern. */
    enum class Font { Sans, Roman, Italic }

    /** Text with its [anchor] at x and its middle at y, turned [angle] degrees (anticlockwise) about that point. */
    class Label(
        val x: Double, val y: Double, val text: String, val size: Double, val color: Int,
        val anchor: Anchor = Anchor.Start, val font: Font = Font.Sans, val angle: Double = 0.0,
        /** Letters drawn in math italic within a roman label (the z of "Re z", the i of "2i"). */
        val italic: Set<Char> = emptySet(),
        /** Maths set as runs (italic letters, raised exponents, lowered indices), instead of [text]. */
        val spans: List<Span>? = null,
    ) : Item() {
        /** The label as runs: [spans], or [text] split by font. */
        fun allSpans(): List<Span> = spans ?: runs().map { (t, ital) -> Span(t, ital) }

        /** The text in runs of one font: (piece, italic?). */
        fun runs(): List<Pair<String, Boolean>> {
            spans?.let { sp -> return sp.map { it.text to it.italic } }
            if (italic.isEmpty()) return listOf(text to (font == Font.Italic))
            val out = ArrayList<Pair<String, Boolean>>()
            for (c in text) {
                val it = c in italic || font == Font.Italic
                if (out.isNotEmpty() && out.last().second == it) out[out.lastIndex] = (out.last().first + c) to it else out += c.toString() to it
            }
            return out
        }
    }

    /**
     * A run of a maths label: its text, italic (math italic) or roman, and [shift] 1 for an
     * exponent, −1 for an index (drawn smaller, raised or lowered), 0 on the line.
     */
    class Span(val text: String, val italic: Boolean, val shift: Int = 0) {
        val scale get() = if (shift == 0) 1.0 else 0.7
        /** How far the run's middle moves up, in label sizes. */
        val rise get() = when { shift > 0 -> 0.38; shift < 0 -> -0.22; else -> 0.0 }
    }

    /** Everything up to the matching [ClipEnd] is cut to this rectangle (a plot's frame). */
    class ClipStart(val x: Double, val y: Double, val w: Double, val h: Double) : Item()
    object ClipEnd : Item()

    /** A picture ([pixels] ARGB, row 0 at the top) stretched over the rectangle. */
    class Image(val x: Double, val y: Double, val w: Double, val h: Double, val pixels: IntArray, val pixelWidth: Int, val pixelHeight: Int) : Item()

    val items = ArrayList<Item>()

    fun add(item: Item) { items += item }
}

/** The export formats; PDF first, as the default. */
enum class ExportFormat(val label: String, val extension: String, val mime: String) {
    PDF("PDF", "pdf", "application/pdf"),
    PNG("PNG", "png", "image/png"),
    JPG("JPG", "jpg", "image/jpeg"),
    SVG("SVG", "svg", "image/svg+xml"),
}

/** Writes a [Scene] as an SVG document. */
object SvgWriter {
    private fun n(v: Double) = String.format(Locale.US, "%.2f", v).trimEnd('0').trimEnd('.').ifEmpty { "0" }.let { if (it == "-0") "0" else it }

    private fun rgb(c: Int) = String.format(Locale.US, "#%06X", c and 0xFFFFFF)

    /** fill="#rrggbb" plus its opacity when not opaque. */
    private fun paint(attr: String, c: Int): String {
        val a = (c ushr 24) and 0xFF
        return "$attr=\"${rgb(c)}\"" + if (a < 255) " $attr-opacity=\"${n(a / 255.0)}\"" else ""
    }

    private fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun pathData(points: DoubleArray, closed: Boolean) = buildString {
        var k = 0
        while (k + 1 < points.size) {
            append(if (k == 0) "M" else "L").append(n(points[k])).append(' ').append(n(points[k + 1]))
            k += 2
        }
        if (closed) append('Z')
    }

    /**
     * The SVG for [scene]. Font files in [embedded] (the Computer Modern OTFs) are put inside the
     * file, so its maths text shows in LaTeX's font on any computer.
     */
    fun write(scene: Scene, embedded: Map<Scene.Font, ByteArray> = emptyMap()): String = buildString {
        var clips = 0
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"${n(scene.width)}\" height=\"${n(scene.height)}\" viewBox=\"0 0 ${n(scene.width)} ${n(scene.height)}\">\n")
        if (embedded.isNotEmpty()) {
            append("<style>")
            embedded.forEach { (font, bytes) ->
                append("@font-face{font-family:'CM${font.name}';src:url(data:font/otf;base64,${java.util.Base64.getEncoder().encodeToString(bytes)}) format('opentype');}")
            }
            append("</style>\n")
        }
        append("<defs><clipPath id=\"frame\"><rect width=\"${n(scene.width)}\" height=\"${n(scene.height)}\"/></clipPath></defs>\n")
        append("<rect width=\"${n(scene.width)}\" height=\"${n(scene.height)}\" ${paint("fill", scene.background)}/>\n")
        append("<g clip-path=\"url(#frame)\">\n")
        for (item in scene.items) when (item) {
            is Scene.Stroke -> {
                val d = item.paths.filter { it.size >= 4 }.joinToString("") { pathData(it, false) }
                if (d.isEmpty()) continue
                append("<path d=\"$d\" fill=\"none\" ${paint("stroke", item.color)} stroke-width=\"${n(item.strokeWidth)}\" stroke-linecap=\"round\" stroke-linejoin=\"round\"")
                item.dash?.let { append(" stroke-dasharray=\"${it.joinToString(" ") { v -> n(maxOf(v, 0.01)) }}\"") }
                append("/>\n")
            }
            is Scene.Fill -> {
                val d = item.polygons.filter { it.size >= 6 }.joinToString("") { pathData(it, true) }
                if (d.isEmpty()) continue
                append("<path d=\"$d\" ${paint("fill", item.color)}/>\n")
            }
            is Scene.Circle -> {
                append("<circle cx=\"${n(item.cx)}\" cy=\"${n(item.cy)}\" r=\"${n(item.r)}\" ")
                append(item.fill?.let { paint("fill", it) } ?: "fill=\"none\"")
                item.stroke?.let { append(" ${paint("stroke", it)} stroke-width=\"${n(item.strokeWidth)}\"") }
                append("/>\n")
            }
            is Scene.Label -> {
                val anchor = when (item.anchor) { Scene.Anchor.Start -> "start"; Scene.Anchor.Middle -> "middle"; Scene.Anchor.End -> "end" }
                val own = if (item.font in embedded) "CM${item.font.name}, " else ""
                val family = when (item.font) {
                    Scene.Font.Sans -> "Google Sans Flex, Google Sans, Roboto, Arial, sans-serif"
                    // The embedded font, else Computer Modern as LaTeX has it, under its usual installed names.
                    else -> own + "Latin Modern Roman, CMU Serif, Computer Modern, cmr10, Times New Roman, serif"
                }
                append("<text x=\"${n(item.x)}\" y=\"${n(item.y)}\" font-family=\"$family\" font-size=\"${n(item.size)}\" text-anchor=\"$anchor\" dominant-baseline=\"central\" ${paint("fill", item.color)}")
                // (The embedded italic is italic already; slanting it again would double it.)
                if (item.font == Scene.Font.Italic && item.font !in embedded) append(" font-style=\"italic\"")
                if (item.angle != 0.0) append(" transform=\"rotate(${n(-item.angle)} ${n(item.x)} ${n(item.y)})\"")
                append(">")
                val runs = item.runs()
                if (item.spans != null && item.spans.any { it.shift != 0 }) {
                    // Raised and lowered runs: each moves up or down from the one before (dy), smaller.
                    var at = 0.0
                    item.spans.forEach { sp ->
                        val target = -sp.rise * item.size
                        append("<tspan")
                        if (sp.italic && item.font != Scene.Font.Italic) {
                            if (Scene.Font.Italic in embedded) append(" font-family=\"CMItalic, Latin Modern Roman, CMU Serif, serif\"") else append(" font-style=\"italic\"")
                        }
                        if (target != at) append(" dy=\"${n(target - at)}\"")
                        if (sp.shift != 0) append(" font-size=\"${n(item.size * sp.scale)}\"")
                        append(">").append(escape(sp.text)).append("</tspan>")
                        at = target
                    }
                } else if (runs.size == 1) append(escape(item.text))
                else runs.forEach { (piece, ital) ->
                    // Italic pieces of a roman label: the embedded italic font, or a slant.
                    if (ital && item.font != Scene.Font.Italic) {
                        append("<tspan")
                        if (Scene.Font.Italic in embedded) append(" font-family=\"CMItalic, Latin Modern Roman, CMU Serif, serif\"") else append(" font-style=\"italic\"")
                        append(">").append(escape(piece)).append("</tspan>")
                    } else append(escape(piece))
                }
                append("</text>\n")
            }
            is Scene.ClipStart -> {
                clips++
                append("<clipPath id=\"c$clips\"><rect x=\"${n(item.x)}\" y=\"${n(item.y)}\" width=\"${n(item.w)}\" height=\"${n(item.h)}\"/></clipPath>\n")
                append("<g clip-path=\"url(#c$clips)\">\n")
            }
            Scene.ClipEnd -> append("</g>\n")
            is Scene.Image -> {
                val png = java.util.Base64.getEncoder().encodeToString(Png.encode(item.pixels, item.pixelWidth, item.pixelHeight))
                append("<image x=\"${n(item.x)}\" y=\"${n(item.y)}\" width=\"${n(item.w)}\" height=\"${n(item.h)}\" preserveAspectRatio=\"none\" href=\"data:image/png;base64,$png\"/>\n")
            }
        }
        append("</g>\n</svg>\n")
    }
}

/** A minimal PNG encoder (8-bit RGBA), for pictures inside SVG files. */
object Png {
    fun encode(argb: IntArray, width: Int, height: Int): ByteArray {
        require(argb.size >= width * height) { "Not enough pixels" }
        val raw = ByteArray(height * (1 + width * 4))
        var o = 0
        for (j in 0 until height) {
            raw[o++] = 0 // no filter
            for (i in 0 until width) {
                val c = argb[j * width + i]
                raw[o++] = (c shr 16).toByte(); raw[o++] = (c shr 8).toByte(); raw[o++] = c.toByte(); raw[o++] = (c ushr 24).toByte()
            }
        }
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(raw)
        deflater.finish()
        val z = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (!deflater.finished()) z.write(buf, 0, deflater.deflate(buf))
        deflater.end()
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 13, 10, 26, 10))
        fun int(v: Int) = byteArrayOf((v ushr 24).toByte(), (v ushr 16).toByte(), (v ushr 8).toByte(), v.toByte())
        fun chunk(type: String, data: ByteArray) {
            out.write(int(data.size))
            val t = type.toByteArray(Charsets.US_ASCII)
            out.write(t); out.write(data)
            val crc = CRC32().apply { update(t); update(data) }
            out.write(int(crc.value.toInt()))
        }
        chunk("IHDR", int(width) + int(height) + byteArrayOf(8, 6, 0, 0, 0))
        chunk("IDAT", z.toByteArray())
        chunk("IEND", ByteArray(0))
        return out.toByteArray()
    }
}
