package com.example.cas.graph

import com.example.cas.editor.Const
import com.example.cas.editor.Frac
import com.example.cas.editor.Func
import com.example.cas.editor.MathRow
import com.example.cas.editor.Node
import com.example.cas.editor.Pow
import com.example.cas.editor.Root
import com.example.cas.editor.Scripted
import com.example.cas.editor.Sqrt
import com.example.cas.editor.Sym
import com.example.cas.engine.LatexParser

/**
 * Maths set as LaTeX sets it, for exported graphs (the legend): letters in math italic, numbers
 * and function names upright, exponents raised, fractions stacked on a bar, roots under a
 * radical sign and brackets as tall as what they hold, all in Computer Modern. A row becomes a
 * [Box], which draws itself into a [Scene] as labels (placed by their baselines) and strokes.
 */
object MathScene {
    /** How wide a piece of text is in Computer Modern, at a size. */
    fun interface Metrics {
        fun width(text: String, italic: Boolean, size: Double): Double
    }

    /**
     * The fonts' widths: set by the app from the real fonts (so PDF, PNG and SVG match), else a
     * fair guess (half an em a character), which is what tests use.
     */
    @Volatile var metrics: Metrics = Metrics { text, _, size -> text.length * size * 0.5 }

    /** Something laid out: its width, how far it reaches above and below its baseline, and how to draw it. */
    class Box(val width: Double, val ascent: Double, val descent: Double, val draw: (Scene, Double, Double) -> Unit) {
        val height get() = ascent + descent
    }

    private val EMPTY = Box(0.0, 0.0, 0.0) { _, _, _ -> }

    fun layout(row: MathRow, size: Double, color: Int): Box = Setter(size, color).row(row, size)

    private val RELATIONS = setOf("=", "<", ">", "≤", "≥", "≈", "≠", "→", ":=", "∈")
    private val BINARY = setOf("+", "−", "-", "×", "·", "±", "÷")
    private val INVERSE = mapOf("asin" to "sin", "acos" to "cos", "atan" to "tan", "asinh" to "sinh", "acosh" to "cosh", "atanh" to "tanh")
    private val UNSPACED = setOf("abs", "floor", "ceil", "frac", "round", "dot", "cross", "hadamard", "kron", "conj")

    private class Setter(val base: Double, val color: Int) {
        val m get() = metrics

        fun text(t: String, italic: Boolean, size: Double): Box {
            val w = m.width(t, italic, size)
            return Box(w, 0.72 * size, 0.22 * size) { scene, x, y ->
                scene.add(Scene.Label(x, y, t, size, color, Scene.Anchor.Start, if (italic) Scene.Font.Italic else Scene.Font.Roman, baseline = true))
            }
        }

        fun gap(w: Double) = Box(w, 0.0, 0.0) { _, _, _ -> }

        fun hbox(parts: List<Box>): Box {
            if (parts.isEmpty()) return EMPTY
            return Box(parts.sumOf { it.width }, parts.maxOf { it.ascent }, parts.maxOf { it.descent }) { scene, x, y ->
                var at = x
                parts.forEach { p -> p.draw(scene, at, y); at += p.width }
            }
        }

        /** A bracket as tall as [inner] (the font's own size for ordinary contents). */
        fun delim(t: String, inner: Box, size: Double): Box {
            val normal = 0.97 * size
            val s = if (inner.height > normal * 1.1) size * inner.height / normal else size
            val w = m.width(t, false, s)
            // Centred on the maths axis, like TeX's \left and \right.
            val axis = 0.25 * size
            val shift = axis - (0.75 * s - 0.25 * s) / 2
            return Box(w, 0.75 * s + shift, 0.25 * s - shift) { scene, x, y ->
                scene.add(Scene.Label(x, y - shift, t, s, color, Scene.Anchor.Start, Scene.Font.Roman, baseline = true))
            }
        }

        fun fenced(open: String, inner: Box, close: String, size: Double) = hbox(listOf(delim(open, inner, size), inner, delim(close, inner, size)))

        fun bars(inner: Box, size: Double): Box {
            val w = 0.28 * size
            val lw = 0.045 * size
            val top = maxOf(inner.ascent, 0.75 * size); val bottom = maxOf(inner.descent, 0.25 * size)
            val bar = Box(w, top, bottom) { scene, x, y -> scene.add(Scene.Stroke(listOf(doubleArrayOf(x + w / 2, y - top, x + w / 2, y + bottom)), color, lw)) }
            return hbox(listOf(bar, inner, bar))
        }

        /** [b] with an exponent and/or an index after it, as TeX places them. */
        fun scripts(b: Box, sup: Box?, sub: Box?, size: Double): Box {
            val supShift = sup?.let { maxOf(0.42 * size, b.ascent - 0.38 * size) } ?: 0.0
            val subShift = sub?.let { maxOf(0.2 * size, b.descent + 0.1 * size) } ?: 0.0
            val w = maxOf(sup?.width ?: 0.0, sub?.width ?: 0.0) + 0.05 * size
            val asc = maxOf(b.ascent, sup?.let { supShift + it.ascent } ?: 0.0)
            val desc = maxOf(b.descent, sub?.let { subShift + it.descent } ?: 0.0)
            return Box(b.width + w, asc, desc) { scene, x, y ->
                b.draw(scene, x, y)
                sup?.draw(scene, x + b.width + 0.03 * size, y - supShift)
                sub?.draw(scene, x + b.width + 0.03 * size, y + subShift)
            }
        }

        fun frac(num: Box, den: Box, size: Double): Box {
            val axis = 0.25 * size
            val t = 0.045 * size
            val g = 0.12 * size
            val w = maxOf(num.width, den.width) + 0.2 * size
            val numBase = axis + t / 2 + g + num.descent
            val denBase = axis - t / 2 - g - den.ascent
            return Box(w, numBase + num.ascent, -denBase + den.descent) { scene, x, y ->
                num.draw(scene, x + (w - num.width) / 2, y - numBase)
                den.draw(scene, x + (w - den.width) / 2, y - denBase)
                scene.add(Scene.Fill(listOf(doubleArrayOf(x + 0.05 * size, y - axis - t / 2, x + w - 0.05 * size, y - axis - t / 2, x + w - 0.05 * size, y - axis + t / 2, x + 0.05 * size, y - axis + t / 2)), color))
            }
        }

        fun radical(body: Box, index: Box?, size: Double): Box {
            val lw = 0.045 * size
            val top = body.ascent + 0.14 * size
            val bottom = maxOf(body.descent, 0.2 * size)
            val sign = 0.62 * size
            val pre = index?.let { maxOf(0.0, it.width - 0.3 * size) } ?: 0.0
            val w = pre + sign + body.width + 0.08 * size
            return Box(w, top + lw, bottom) { scene, x, y ->
                val x0 = x + pre
                // The tick, the long stroke down, up to the top, then the bar over the body.
                scene.add(Scene.Stroke(listOf(doubleArrayOf(
                    x0 + 0.05 * size, y - 0.28 * size, x0 + 0.17 * size, y - 0.34 * size,
                    x0 + 0.33 * size, y + bottom * 0.9, x0 + sign - 0.04 * size, y - top,
                    x0 + sign + body.width + 0.06 * size, y - top,
                )), color, lw))
                body.draw(scene, x0 + sign, y)
                index?.draw(scene, x, y - 0.45 * size)
            }
        }

        private fun italicLetter(t: String) = t.length == 1 && (t[0] in 'a'..'z' || t[0] in 'A'..'Z' || t[0] in 'α'..'ω' || t[0] == 'ϕ' || t[0] == 'ϑ')

        /** The pieces of a row, with exponents attached to what they follow and TeX's spacing. */
        fun row(r: MathRow, size: Double): Box {
            val parts = ArrayList<Box>()
            val items = r.items
            var k = 0
            while (k < items.size) {
                val n = items[k]
                // Brackets in the row are as tall as what's between them.
                if (n is Sym && n.text in setOf("(", "[")) {
                    val close = matching(items, k)
                    if (close > k) {
                        // (A plain list of the nodes between: they stay where they are in the line.)
                        val inner = row(MathRow(items.subList(k + 1, close).toMutableList()), size)
                        var b = fenced(n.text, inner, (items[close] as Sym).text, size)
                        var j = close + 1
                        while (j < items.size && items[j] is Pow) { b = scripts(b, row((items[j] as Pow).exp, size * 0.7), null, size); j++ }
                        parts += b; k = j; continue
                    }
                }
                val before = items.getOrNull(k - 1)
                // An operator name (sin, log: a function, or a word the parser read) after a number,
                // letter or bracket gets a thin space, as TeX gives 2 sin x.
                val opName = n is Func && n.name !in UNSPACED || n is Sym && n.text.length > 1 && n.text.all { it.isLetter() }
                if (opName && before != null && (before !is Sym || before.text.lastOrNull()?.let { it.isLetterOrDigit() || it in ")]" } == true)) parts += gap(0.1667 * size)
                var b = node(n, size)
                var j = k + 1
                while (j < items.size && items[j] is Pow) { b = scripts(b, row((items[j] as Pow).exp, size * 0.7), null, size); j++ }
                parts += b
                k = j
            }
            return hbox(parts)
        }

        private fun matching(items: List<Node>, open: Int): Int {
            var depth = 0
            for (j in open until items.size) {
                when ((items[j] as? Sym)?.text) { "(", "[" -> depth++; ")", "]" -> { depth--; if (depth == 0) return j } }
            }
            return -1
        }

        fun node(n: Node, size: Double): Box = when (n) {
            is Sym -> sym(n.text, size)
            is Const -> {
                val c = com.example.cas.engine.Constant.byId(n.id)
                if (c == null) text(n.id, false, size) else hbox(c.pieces.map { p ->
                    scripts(text(p.text, p.italic, size), p.sup.takeIf { it.isNotEmpty() }?.let { text(it, false, size * 0.7) }, p.sub.takeIf { it.isNotEmpty() }?.let { text(it, false, size * 0.7) }, size)
                })
            }
            is Pow -> scripts(EMPTY, row(n.exp, size * 0.7), null, size)
            is Frac -> frac(row(n.num, size * 0.75), row(n.den, size * 0.75), size)
            is Sqrt -> radical(row(n.arg, size), null, size)
            is Root -> radical(row(n.arg, size), row(n.index, size * 0.55), size)
            is Scripted -> scripts(row(n.base, size), if (n.sup.isEmpty) null else row(n.sup, size * 0.7), if (n.sub.isEmpty) null else row(n.sub, size * 0.7), size)
            is Func -> func(n, size)
            is com.example.cas.editor.Integral -> hbox(listOf(
                scripts(integralSign(size, loop = false), if (n.upper.isEmpty) null else row(n.upper, size * 0.6), if (n.lower.isEmpty) null else row(n.lower, size * 0.6), size),
                gap(0.1667 * size), row(n.body, size), gap(0.1667 * size), text("d", false, size), row(n.variable, size),
            ))
            is com.example.cas.editor.BigOp -> {
                val lower = if (n.lower.isEmpty && n.variable.isEmpty) null
                    else hbox(listOf(row(n.variable, size * 0.6), text("=", false, size * 0.6), row(n.lower, size * 0.6)))
                hbox(listOf(
                    bigOp(if (n.kind == com.example.cas.editor.BigOpKind.Sum) "Σ" else "Π", lower, if (n.upper.isEmpty) null else row(n.upper, size * 0.6), size),
                    gap(0.1667 * size), row(n.body, size),
                ))
            }
            // Anything bigger (sums, integrals, matrices): written out in a line.
            else -> hbox(Legend.spans(MathRow(mutableListOf(n))).map { sp -> text(sp.text, sp.italic, if (sp.shift == 0) size else size * 0.7) })
        }

        fun sym(t: String, size: Double): Box = when {
            t.startsWith(LatexParser.UPRIGHT) -> text(t.removePrefix(LatexParser.UPRIGHT), false, size)
            // In scripts TeX leaves relations and operators unspaced (|z|=1 under ∮).
            t in RELATIONS && size < base * 0.9 -> text(t, false, size)
            t in BINARY && size < base * 0.9 -> text(if (t == "-") "−" else t, false, size)
            t in RELATIONS -> hbox(listOf(gap(0.2778 * size), text(t, false, size), gap(0.2778 * size)))
            t in BINARY -> hbox(listOf(gap(0.2222 * size), text(if (t == "-") "−" else t, false, size), gap(0.2222 * size)))
            t == "," -> hbox(listOf(text(",", false, size), gap(0.1667 * size)))
            t == com.example.cas.engine.Formatter.THIN_SPACE -> gap(0.1667 * size)
            italicLetter(t) -> text(t, true, size)
            else -> com.example.cas.cas.CustomSymbol.decode(t)?.let { text(it.plain, true, size) } ?: text(t, false, size)
        }

        /** ∫, larger than the text and centred on the maths axis; with [loop], a small circle through it (∮). */
        fun integralSign(size: Double, loop: Boolean): Box {
            val s = size * 1.45
            val w = m.width("∫", false, s)
            val axis = 0.25 * size
            val shift = axis - (0.75 * s - 0.25 * s) / 2
            return Box(w, 0.75 * s + shift, 0.25 * s - shift) { scene, x, y ->
                scene.add(Scene.Label(x, y - shift, "∫", s, color, Scene.Anchor.Start, Scene.Font.Roman, baseline = true))
                if (loop) {
                    val cx = x + w * 0.5; val cy = y - axis; val r = 0.2 * size
                    scene.add(Scene.Stroke(listOf(DoubleArray(2 * 33) { k -> val t = (k / 2) * 2 * Math.PI / 32; if (k % 2 == 0) cx + r * Math.cos(t) else cy + r * Math.sin(t) }), color, 0.05 * size))
                }
            }
        }

        /** Σ or Π, large, with its limits under and over it. */
        fun bigOp(sign: String, lower: Box?, upper: Box?, size: Double): Box {
            val s = size * 1.35
            val sb = text(sign, false, s)
            val w = maxOf(sb.width, lower?.width ?: 0.0, upper?.width ?: 0.0)
            val axis = 0.25 * size
            val shift = axis - (0.7 * s - 0.0) / 2
            val lowerDrop = 0.12 * size + (lower?.ascent ?: 0.0)
            val upperRise = 0.7 * s + 0.1 * size + (upper?.descent ?: 0.0)
            return Box(w, maxOf(0.7 * s + shift, upper?.let { shift + upperRise + it.ascent } ?: 0.0), maxOf(-shift, lower?.let { -shift + lowerDrop + it.descent } ?: 0.0)) { scene, x, y ->
                sb.draw(scene, x + (w - sb.width) / 2, y - shift)
                lower?.draw(scene, x + (w - lower.width) / 2, y - shift + lowerDrop)
                upper?.draw(scene, x + (w - upper.width) / 2, y - shift - upperRise)
            }
        }

        fun func(f: Func, size: Double): Box {
            val a = f.args
            return when {
                // ∮ over the loop (|z| = 1 underneath), the body, then dz.
                f.name == "contour" && a.size == 2 -> hbox(listOf(
                    scripts(integralSign(size, loop = true), null, row(a[1], size * 0.6), size),
                    gap(0.1667 * size), row(a[0], size), gap(0.1667 * size), text("d", false, size), text("z", true, size),
                ))
                f.name == "abs" -> bars(row(a[0], size), size)
                f.name in INVERSE -> hbox(listOf(scripts(text(INVERSE.getValue(f.name), false, size), text("−1", false, size * 0.7), null, size), fenced("(", row(a[0], size), ")", size)))
                f.name == "log" && a.size == 2 -> hbox(listOf(scripts(text("log", false, size), null, row(a[0], size * 0.7), size), fenced("(", row(a[1], size), ")", size)))
                else -> {
                    val parts = ArrayList<Box>()
                    a.forEachIndexed { i, r -> if (i > 0) parts += hbox(listOf(text(",", false, size), gap(0.1667 * size))); parts += row(r, size) }
                    hbox(listOf(text(f.name, false, size), fenced("(", hbox(parts), ")", size)))
                }
            }
        }
    }
}
