package com.example.cas.graph

/**
 * Exported graphs drawn the way pgfplots draws them: the plot inside a thin frame, ticks
 * pointing inwards on all four sides, tick labels in Computer Modern outside the bottom and left
 * edges, the axis names in math italic (y turned on its side), and an optional light grid at the
 * major ticks. Everything is in the scene's units; the drawing is square.
 */
object Pgf {
    /** The plot area inside the frame. */
    class Frame(val left: Double, val top: Double, val right: Double, val bottom: Double) {
        val width get() = right - left
        val height get() = bottom - top
    }

    /** Light is pgfplots' own look (white page, black frame); dark is its negative. */
    class Style(val background: Int, val ink: Int, val grid: Int, val cycle: List<Int>) {
        companion object {
            // SciencePlots' "science" color cycle (github.com/garrettj403/SciencePlots): blue, green,
            // orange, red, purple, dark grey, grey. Lines take them in turn, in list order.
            val LIGHT = Style(0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFFD9D9D9.toInt(), SCIENCE)
            // The same hues lightened for a dark page (the greys turned light).
            val DARK = Style(0xFF141414.toInt(), 0xFFEDEDED.toInt(), 0xFF3A3A3A.toInt(),
                listOf(0xFF4C9BE8, 0xFF3DDC75, 0xFFFFAA33, 0xFFFF5A3C, 0xFFB48CCB, 0xFFBDBDBD, 0xFFE0E0E0).map { it.toInt() })
        }
    }

    /** SciencePlots' science.mplstyle color cycle, as ARGB. */
    val SCIENCE: List<Int> = listOf(0xFF0C5DA5, 0xFF00B945, 0xFFFF9500, 0xFFFF2C00, 0xFF845B97, 0xFF474747, 0xFF9E9E9E).map { it.toInt() }

    /**
     * One line of the legend: its name as runs of text, and its sample: a short stroke (with the
     * line's dash and width), a mark, a filled swatch (a region), or a colormap strip.
     */
    class LegendEntry(
        val spans: List<Scene.Span>,
        val color: Int,
        val line: Boolean = true,
        val dash: DoubleArray? = null,
        val width: Double = 1.5,
        val marker: Marker? = null,
        val markerSize: Double = 2.6,
        val fill: Int? = null,
        val strip: List<Int>? = null,
    )

    /**
     * The legend as SciencePlots draws it: in the top left corner inside the frame, with no box,
     * a short sample of each line and its name beside it, in the order of the list.
     */
    fun legend(scene: Scene, f: Frame, style: Style, entries: List<LegendEntry>, top: Double = f.top, panel: Int? = null) {
        if (entries.isEmpty()) return
        val size = TICK_SIZE * 0.95
        val row = size * 1.5
        val sample = 22.0
        val x0 = f.left + 9.0
        var y = top + 9.0 + row / 2
        // Over a picture (the complex plane) a pale panel keeps the names readable; its width
        // is estimated from the text (about half an em a character).
        panel?.let { c ->
            val w = sample + 12 + entries.maxOf { e -> e.spans.sumOf { it.text.length * size * 0.52 * it.scale } }
            val x1 = x0 - 5; val y1 = top + 5; val x2 = x0 + w; val y2 = top + 13 + row * entries.size
            scene.add(Scene.Fill(listOf(doubleArrayOf(x1, y1, x2, y1, x2, y2, x1, y2)), c))
        }
        for (e in entries) {
            e.strip?.let { colors ->
                val w = sample / colors.size
                colors.forEachIndexed { k, c -> scene.add(Scene.Fill(listOf(doubleArrayOf(x0 + k * w, y - 4, x0 + (k + 1) * w + 0.2, y - 4, x0 + (k + 1) * w + 0.2, y + 4, x0 + k * w, y + 4)), c)) }
            }
            e.fill?.let { c -> scene.add(Scene.Fill(listOf(doubleArrayOf(x0, y - 4.5, x0 + sample, y - 4.5, x0 + sample, y + 4.5, x0, y + 4.5)), c)) }
            if (e.line) scene.add(Scene.Stroke(listOf(doubleArrayOf(x0, y, x0 + sample, y)), e.color, e.width, e.dash))
            e.marker?.addTo(scene, x0 + sample / 2, y, e.markerSize, e.color)
            val text = e.spans.joinToString("") { it.text }
            scene.add(Scene.Label(x0 + sample + 6, y, text, size, style.ink, Scene.Anchor.Start, Scene.Font.Roman, spans = e.spans))
            y += row
        }
    }

    /** Tick labels and axis names (units; pgfplots' 10 pt on a 240 pt axis, scaled up). */
    const val TICK_SIZE = 13.0
    const val NAME_SIZE = 16.0
    const val TICK_LENGTH = 4.0
    const val FRAME_WIDTH = 0.7

    /** The frame for a square drawing [size] units across, with room for labels and names. */
    fun frame(size: Double) = Frame(left = 58.0, top = 14.0, right = size - 16.0, bottom = size - 46.0)

    /** A tick label as pgfplots prints it: no trailing zeros (2, 1.5, not 2.0, 1.50). */
    fun tick(v: Double, step: Double): String = Plot2D.label(v, step).let { if ('.' in it) it.trimEnd('0').trimEnd('.') else it }.let { if (it == "−0") "0" else it }

    fun sx(v: Viewport, f: Frame, x: Double) = f.left + (x - v.xMin) / v.width * f.width
    fun sy(v: Viewport, f: Frame, y: Double) = f.top + (v.yMax - y) / v.height * f.height

    /** The grid at the major ticks, drawn under the plot. */
    fun grid(scene: Scene, v: Viewport, f: Frame, style: Style) {
        val lines = Plot2D.ticks(v.xMin, v.xMax, 6).map { x -> doubleArrayOf(sx(v, f, x), f.top, sx(v, f, x), f.bottom) } +
            Plot2D.ticks(v.yMin, v.yMax, 6).map { y -> doubleArrayOf(f.left, sy(v, f, y), f.right, sy(v, f, y)) }
        scene.add(Scene.Stroke(lines, style.grid, 0.5))
    }

    /**
     * The frame, the ticks and their labels, and the axis names, drawn over the plot.
     * [ySuffix] follows each y label ("i" on the complex plane).
     */
    fun axes(
        scene: Scene, v: Viewport, f: Frame, style: Style, xName: String = "x", yName: String = "y", ySuffix: String = "",
        nameFont: Scene.Font = Scene.Font.Italic,
        /** Letters in math italic inside roman names and labels (z in "Re z", i in "2i"). */
        italic: Set<Char> = emptySet(),
    ) {
        val stepX = Plot2D.niceStep(v.width, 6)
        val stepY = Plot2D.niceStep(v.height, 6)
        val ticks = ArrayList<DoubleArray>()
        for (x in Plot2D.ticks(v.xMin, v.xMax, 6)) {
            val px = sx(v, f, x)
            ticks += doubleArrayOf(px, f.bottom, px, f.bottom - TICK_LENGTH)
            ticks += doubleArrayOf(px, f.top, px, f.top + TICK_LENGTH)
            scene.add(Scene.Label(px, f.bottom + 11, tick(x, stepX), TICK_SIZE, style.ink, Scene.Anchor.Middle, Scene.Font.Roman))
        }
        for (y in Plot2D.ticks(v.yMin, v.yMax, 6)) {
            val py = sy(v, f, y)
            ticks += doubleArrayOf(f.left, py, f.left + TICK_LENGTH, py)
            ticks += doubleArrayOf(f.right, py, f.right - TICK_LENGTH, py)
            val text = tick(y, stepY).let { if (ySuffix.isNotEmpty() && it != "0") (if (it == "1") "" else if (it == "−1") "−" else it) + ySuffix else it }
            scene.add(Scene.Label(f.left - 5, py, text, TICK_SIZE, style.ink, Scene.Anchor.End, Scene.Font.Roman, italic = italic))
        }
        scene.add(Scene.Stroke(ticks, style.ink, FRAME_WIDTH * 0.9))
        scene.add(Scene.Stroke(listOf(doubleArrayOf(f.left, f.top, f.right, f.top, f.right, f.bottom, f.left, f.bottom, f.left, f.top)), style.ink, FRAME_WIDTH))
        scene.add(Scene.Label((f.left + f.right) / 2, f.bottom + 32, xName, NAME_SIZE, style.ink, Scene.Anchor.Middle, nameFont, italic = italic))
        scene.add(Scene.Label(13.0, (f.top + f.bottom) / 2, yName, NAME_SIZE, style.ink, Scene.Anchor.Middle, nameFont, angle = 90.0, italic = italic))
    }
}
