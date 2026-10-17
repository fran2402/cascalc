package com.example.cas.graph

import com.example.cas.editor.MathCodec
import com.example.cas.editor.Sym
import com.example.cas.engine.LatexParser
import com.example.cas.engine.Notebook

/**
 * A notebook on A4 pages (points), typeset in Computer Modern like the history's PDF: the title,
 * text as wrapped paragraphs (a # line as a heading, $math$ set as math), each calculation with
 * its answer on the right, and each graph as a framed figure with a grid.
 */
object NotebookExport {
    private const val W = SessionExport.PAGE_WIDTH
    private const val H = SessionExport.PAGE_HEIGHT
    private const val MARGIN = 56.0
    private val CURVES = intArrayOf(0xFF1F5FA8.toInt(), 0xFFC0392B.toInt(), 0xFF2E8B57.toInt(), 0xFF8E44AD.toInt(), 0xFFD68910.toInt())

    fun pages(d: Notebook.Doc, run: Notebook.Run, subtitle: String, ink: Int = 0xFF000000.toInt(), muted: Int = 0xFF5C5C5C.toInt()): List<Scene> {
        val out = ArrayList<Scene>()
        val width = W - 2 * MARGIN
        var scene = Scene(W, H, 0xFFFFFFFF.toInt())
        var y = MARGIN
        fun room(h: Double) { if (y + h > H - MARGIN && y > MARGIN + 1) { out += scene; scene = Scene(W, H, 0xFFFFFFFF.toInt()); y = MARGIN } }
        scene.add(Scene.Label(MARGIN, y + 14, d.title.ifBlank { "Notebook" }, 22.0, ink, Scene.Anchor.Start, Scene.Font.Roman))
        scene.add(Scene.Label(MARGIN, y + 34, subtitle, 10.0, muted, Scene.Anchor.Start, Scene.Font.Roman))
        y += 58
        fun fit(row: com.example.cas.editor.MathRow, size: Double, color: Int, max: Double): MathScene.Box {
            val b = MathScene.layout(row, size, color)
            return if (b.width <= max) b else MathScene.layout(row, maxOf(7.0, size * max / b.width), color)
        }

        /** A paragraph: words and $math$ pieces set along lines that wrap at the margin. */
        fun paragraph(text: String, size: Double, color: Int) {
            class Piece(val box: MathScene.Box?, val word: String?, val w: Double, val ascent: Double, val descent: Double)
            val pieces = ArrayList<Piece>()
            for ((math, part) in LatexParser.inline(text)) {
                if (math) {
                    val b = runCatching { fit(LatexParser.parse(part), size, color, width) }.getOrNull() ?: continue
                    pieces += Piece(b, null, b.width, b.ascent, b.descent)
                } else part.split(' ').filter { it.isNotEmpty() }.forEach { w ->
                    pieces += Piece(null, w, MathScene.metrics.width(w, false, size), size * 0.72, size * 0.24)
                }
            }
            val space = MathScene.metrics.width(" ", false, size)
            var line = ArrayList<Piece>(); var lineW = 0.0
            fun flush() {
                if (line.isEmpty()) return
                val asc = line.maxOf { it.ascent }; val desc = line.maxOf { it.descent }
                room(asc + desc + 4)
                val base = y + asc
                var x = MARGIN
                for (p in line) {
                    if (p.box != null) p.box.draw(scene, x, base) else scene.add(Scene.Label(x, base, p.word!!, size, color, Scene.Anchor.Start, Scene.Font.Roman, baseline = true))
                    x += p.w + space
                }
                y = base + desc + size * 0.35
                line = ArrayList(); lineW = 0.0
            }
            for (p in pieces) {
                if (line.isNotEmpty() && lineW + space + p.w > width) flush()
                lineW += (if (line.isEmpty()) 0.0 else space) + p.w
                line += p
            }
            flush()
        }

        for (b in d.blocks) when (b) {
            is Notebook.Block.Text -> {
                for (raw in b.text.lines()) {
                    val t = raw.trim()
                    when {
                        t.startsWith("#") -> { y += 6; paragraph(t.trimStart('#').trim(), 15.0, ink); y += 2 }
                        t.isEmpty() -> y += 6
                        else -> paragraph(t, 11.0, ink)
                    }
                }
                y += 6
            }
            is Notebook.Block.Math -> if (b.row.items.isNotEmpty()) {
                val o = run.outcomes[b.id]
                val q = fit(b.row, 12.0, ink, width)
                val answer = o?.answer?.let { a ->
                    val r = MathCodec.copy(a.exact)
                    if (!a.isStatement) { r.add(0, Sym(" ")); r.add(0, Sym(if (a.isApproximate) "≈" else "=")) }
                    fit(r, 13.0, ink, width - 24)
                }
                val err = o?.error
                room(q.height + (answer?.height ?: 0.0) + 22)
                val qBase = y + q.ascent
                q.draw(scene, MARGIN + 12, qBase)
                y = qBase + q.descent + 6
                if (answer != null) {
                    val aBase = y + answer.ascent
                    answer.draw(scene, MARGIN + width - answer.width, aBase)
                    y = aBase + answer.descent + 6
                } else if (err != null) {
                    scene.add(Scene.Label(MARGIN + width, y + 9, err, 9.0, muted, Scene.Anchor.End, Scene.Font.Roman))
                    y += 14
                }
                y += 8
            }
            is Notebook.Block.Graph -> {
                val p = run.plots[b.id] ?: continue
                if (p.error != null || p.xs.isEmpty()) continue
                val gh = 190.0
                room(gh + 30)
                val left = MARGIN + 20; val right = MARGIN + width - 20; val top = y; val bottom = y + gh
                val x0 = p.xs.first(); val x1 = p.xs.last()
                fun sx(x: Double) = left + (x - x0) / (x1 - x0) * (right - left)
                fun sy(v: Double) = bottom - (v - p.yMin) / (p.yMax - p.yMin) * (bottom - top)
                // Grid at round steps, then the axes, the frame and the curves.
                fun step(span: Double): Double { val raw = span / 6; val e = Math.pow(10.0, Math.floor(Math.log10(raw))); return listOf(1.0, 2.0, 5.0, 10.0).first { it * e >= raw } * e }
                val grid = ArrayList<DoubleArray>()
                val sxStep = step(x1 - x0); var gx = Math.ceil(x0 / sxStep) * sxStep
                while (gx <= x1 + 1e-9) { grid += doubleArrayOf(sx(gx), top, sx(gx), bottom); scene.add(Scene.Label(sx(gx), bottom + 11, num(gx), 8.0, muted, Scene.Anchor.Middle, Scene.Font.Roman)); gx += sxStep }
                val syStep = step(p.yMax - p.yMin); var gy = Math.ceil(p.yMin / syStep) * syStep
                while (gy <= p.yMax + 1e-9) { grid += doubleArrayOf(left, sy(gy), right, sy(gy)); scene.add(Scene.Label(left - 3, sy(gy) + 3, num(gy), 8.0, muted, Scene.Anchor.End, Scene.Font.Roman)); gy += syStep }
                scene.add(Scene.Stroke(grid, 0xFFE3E3E3.toInt(), 0.5))
                if (p.yMin < 0 && p.yMax > 0) scene.add(Scene.Stroke(listOf(doubleArrayOf(left, sy(0.0), right, sy(0.0))), 0xFF8A8A8A.toInt(), 0.8))
                if (x0 < 0 && x1 > 0) scene.add(Scene.Stroke(listOf(doubleArrayOf(sx(0.0), top, sx(0.0), bottom)), 0xFF8A8A8A.toInt(), 0.8))
                scene.add(Scene.Stroke(listOf(doubleArrayOf(left, top, right, top, right, bottom, left, bottom, left, top)), 0xFF444444.toInt(), 0.7))
                p.curves.forEachIndexed { k, cv ->
                    val runs = ArrayList<DoubleArray>(); var cur = ArrayList<Double>()
                    for (i in p.xs.indices) {
                        val v = cv[i]
                        if (!v.isFinite() || v < p.yMin - (p.yMax - p.yMin) || v > p.yMax + (p.yMax - p.yMin)) { if (cur.size >= 4) runs += cur.toDoubleArray(); cur = ArrayList(); continue }
                        cur += sx(p.xs[i]); cur += sy(v.coerceIn(p.yMin, p.yMax))
                    }
                    if (cur.size >= 4) runs += cur.toDoubleArray()
                    scene.add(Scene.Stroke(runs, CURVES[k % CURVES.size], 1.4))
                    p.labels.getOrNull(k)?.let { l ->
                        runCatching { MathScene.layout(LatexParser.parse(l), 9.0, CURVES[k % CURVES.size]) }.getOrNull()?.let { box -> box.draw(scene, right - box.width - 6, top + 12 + k * 13) }
                    }
                }
                y = bottom + 26
            }
        }
        out += scene
        return out
    }

    private fun num(v: Double): String = if (kotlin.math.abs(v - Math.rint(v)) < 1e-9) Math.rint(v).toLong().toString().replace("-", "−")
        else String.format(java.util.Locale.ROOT, "%.3g", v).trimEnd('0').trimEnd('.').replace("-", "−")
}
