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
            // pgfplots' default cycle list: blue, red, brown!60!black, black, then teal, orange, violet.
            val LIGHT = Style(0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFFD9D9D9.toInt(),
                listOf(0xFF0000FF, 0xFFFF0000, 0xFF734D26, 0xFF000000, 0xFF008080, 0xFFFF8000, 0xFF800080).map { it.toInt() })
            val DARK = Style(0xFF141414.toInt(), 0xFFEDEDED.toInt(), 0xFF3A3A3A.toInt(),
                listOf(0xFF6F8CFF, 0xFFFF6B6B, 0xFFD9A066, 0xFFEDEDED, 0xFF33C2C2, 0xFFFFA64D, 0xFFC57BFF).map { it.toInt() })
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
