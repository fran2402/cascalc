package com.example.cas.graph

import com.example.cas.editor.MathCodec
import com.example.cas.editor.MathRow
import com.example.cas.editor.Sym

/**
 * Calculations from the history written out for homework or notes: as a LaTeX document, or as
 * A4 pages (points) typeset with the graphs' Computer Modern layout, for a PDF. Each is its
 * question, then "=" (or "≈") and its answer on the line below, aligned right.
 */
object SessionExport {
    /** One calculation: its question and the answer as shown; [approximate] writes ≈, [statement] no sign. */
    class Entry(val question: MathRow, val answer: MathRow, val approximate: Boolean = false, val statement: Boolean = false)

    const val PAGE_WIDTH = 595.0
    const val PAGE_HEIGHT = 842.0
    private const val MARGIN = 56.0

    /** A complete LaTeX document (amsmath), one numbered display per calculation. */
    fun latex(entries: List<Entry>, title: String): String = buildString {
        appendLine("\\documentclass[11pt]{article}")
        appendLine("\\usepackage{amsmath,amssymb}")
        appendLine("\\usepackage[a4paper,margin=2.5cm]{geometry}")
        appendLine("\\begin{document}")
        appendLine("\\section*{" + title.replace("&", "\\&").replace("%", "\\%") + "}")
        for (e in entries) {
            val q = com.example.cas.engine.Latex.of(e.question)
            val a = com.example.cas.engine.Latex.of(e.answer)
            val sign = if (e.statement) "\\quad " else if (e.approximate) " &\\approx " else " &= "
            appendLine("\\begin{align}")
            appendLine("  " + q + (if (e.statement) " " + sign + a else sign + a))
            appendLine("\\end{align}")
        }
        appendLine("\\end{document}")
    }

    /** The calculations on A4 pages, as many as they need: a title, then each one numbered, with rules between. */
    fun pages(entries: List<Entry>, title: String, subtitle: String, ink: Int = 0xFF000000.toInt(), muted: Int = 0xFF5C5C5C.toInt(), rule: Int = 0xFFD9D9D9.toInt()): List<Scene> {
        val out = ArrayList<Scene>()
        val width = PAGE_WIDTH - 2 * MARGIN
        var scene = Scene(PAGE_WIDTH, PAGE_HEIGHT, 0xFFFFFFFF.toInt())
        var y = MARGIN
        fun newPage() {
            out += scene
            scene = Scene(PAGE_WIDTH, PAGE_HEIGHT, 0xFFFFFFFF.toInt())
            y = MARGIN
        }
        scene.add(Scene.Label(MARGIN, y + 10, title, 20.0, ink, Scene.Anchor.Start, Scene.Font.Roman))
        scene.add(Scene.Label(MARGIN, y + 32, subtitle, 10.0, muted, Scene.Anchor.Start, Scene.Font.Roman))
        y += 56
        /** [row] laid out at [size], made smaller (down to 7 pt) if it's wider than the page. */
        fun fit(row: MathRow, size: Double, color: Int, room: Double): MathScene.Box {
            val b = MathScene.layout(row, size, color)
            return if (b.width <= room) b else MathScene.layout(row, maxOf(7.0, size * room / b.width), color)
        }
        entries.forEachIndexed { k, e ->
            val number = "${k + 1}."
            val q = fit(e.question, 12.0, muted, width - 24)
            val answerRow = MathCodec.copy(e.answer).also { r ->
                if (!e.statement) { r.add(0, Sym(" ")); r.add(0, Sym(if (e.approximate) "≈" else "=")) }
            }
            val a = fit(answerRow, 14.0, ink, width - 24)
            val needed = q.height + a.height + 26
            if (y + needed > PAGE_HEIGHT - MARGIN && y > MARGIN + 1) newPage()
            val qBase = y + q.ascent
            scene.add(Scene.Label(MARGIN, qBase, number, 10.0, muted, Scene.Anchor.Start, Scene.Font.Roman, baseline = true))
            q.draw(scene, MARGIN + 24, qBase)
            val aBase = qBase + q.descent + 8 + a.ascent
            a.draw(scene, MARGIN + width - a.width, aBase)
            y = aBase + a.descent + 9
            if (k < entries.lastIndex) scene.add(Scene.Stroke(listOf(doubleArrayOf(MARGIN, y, MARGIN + width, y)), rule, 0.5))
            y += 9
        }
        out += scene
        return out
    }
}
