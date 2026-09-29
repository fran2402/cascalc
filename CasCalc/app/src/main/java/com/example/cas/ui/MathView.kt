package com.example.cas.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.example.cas.editor.BigOp
import com.example.cas.editor.BigOpKind
import com.example.cas.editor.Binom
import com.example.cas.editor.Scripted
import com.example.cas.editor.Const
import com.example.cas.editor.Derivative
import com.example.cas.editor.Frac
import com.example.cas.editor.Func
import com.example.cas.editor.Integral
import com.example.cas.editor.MathRow
import com.example.cas.editor.Matrix
import com.example.cas.editor.Node
import com.example.cas.editor.Pow
import com.example.cas.editor.Root
import com.example.cas.editor.Sqrt
import com.example.cas.editor.Sym
import com.example.cas.engine.Constant
import com.example.cas.engine.Formatter
import com.example.cas.ui.theme.CasFonts
import com.example.cas.ui.theme.GlyphFallback
import com.example.cas.ui.theme.LocalGlyphFallback
import com.example.cas.ui.theme.LocalMathGlyphs
import com.example.cas.ui.theme.MathGlyphs
import kotlin.math.max
import kotlin.math.roundToInt

/*
 * A small TeX-like renderer for editor trees. Every piece reports a "math
 * baseline" so rows line up the way printed maths does: fraction bars sit on
 * the maths axis, exponents ride above the previous item, brackets stretch
 * to their contents, and every empty slot shows a box you can tap.
 */

/** Baseline used to line up maths. */
val MathBaseline = HorizontalAlignmentLine { a, b -> minOf(a, b) }

/** Everything the renderer needs, provided once per [MathView]. A new instance on every edit makes all pieces redraw. */
class MathEnv(
    val size: TextUnit,
    val color: Color,
    val accent: Color,
    val faint: Color,
    val glyphs: GlyphFallback,
    val cursorRow: MathRow?,
    val cursorIndex: Int,
    /** Key labels draw empty slots as small dots instead of boxes. */
    val emptyAsDot: Boolean,
    /** Computer Modern for the maths display; false for key labels, which use Google Sans Flex. */
    val computerModern: Boolean,
    val mathGlyphs: MathGlyphs,
    val onTap: ((MathRow, Int) -> Unit)?,
) {
    fun scale(level: Int) = when (level) { 0 -> 1f; 1 -> 0.7f; 2 -> 0.56f; else -> 0.5f }
    fun size(level: Int): TextUnit = size * scale(level)
}

val LocalMath = compositionLocalOf<MathEnv> { error("MathView missing") }

@Composable
fun MathView(
    row: MathRow,
    fontSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    accent: Color = color,
    cursorRow: MathRow? = null,
    cursorIndex: Int = 0,
    @Suppress("UNUSED_PARAMETER") version: Int = 0,
    emptyAsDot: Boolean = false,
    computerModern: Boolean = true,
    onTap: ((MathRow, Int) -> Unit)? = null,
) {
    val env = MathEnv(
        size = fontSize,
        color = color,
        accent = accent,
        faint = color.copy(alpha = 0.45f),
        glyphs = LocalGlyphFallback.current,
        cursorRow = cursorRow,
        cursorIndex = cursorIndex,
        emptyAsDot = emptyAsDot,
        computerModern = computerModern,
        mathGlyphs = LocalMathGlyphs.current,
        onTap = onTap,
    )
    CompositionLocalProvider(LocalMath provides env) {
        Box(modifier) { RowView(row, 0) }
    }
}

// ---------------------------------------------------------------------------
// Helpers

private fun Placeable.axis(): Int {
    val m = this[MathBaseline]
    if (m != AlignmentLine.Unspecified) return m
    val b = this[FirstBaseline]
    return if (b != AlignmentLine.Unspecified) b else height
}

private val Loose = Constraints()

private fun Density.em(env: MathEnv, level: Int) = env.size(level).toPx()

/** Distance from the baseline up to the maths axis (where fraction bars and minus signs sit). */
private fun Density.mathAxis(env: MathEnv, level: Int) = em(env, level) * 0.28f

@Composable
private fun tapTo(row: MathRow, index: Int): Modifier {
    val onTap = LocalMath.current.onTap ?: return Modifier
    return Modifier.pointerInput(row, index, onTap) {
        detectTapGestures { o -> onTap(row, if (o.x < size.width / 2) index else index + 1) }
    }
}

@Composable
private fun MathText(text: String, level: Int, modifier: Modifier = Modifier, italic: Boolean = false, color: Color? = null) {
    val env = LocalMath.current
    BasicText(
        text = if (env.computerModern) env.mathGlyphs.style(text, italic) else env.glyphs.style(text),
        modifier = modifier,
        style = TextStyle(
            fontSize = env.size(level),
            fontFamily = when {
                env.computerModern -> if (italic) CasFonts.CmItalic else CasFonts.CmRoman
                else -> if (italic) CasFonts.MathItalic else CasFonts.Math
            },
            color = color ?: env.color,
        ),
        softWrap = false,
        maxLines = 1,
    )
}

/** Lays children side by side, lined up on their math baselines. */
@Composable
private fun AxisRow(level: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val env = LocalMath.current
    Layout(content, modifier) { ms, _ ->
        val ps = ms.map { it.measure(Loose) }
        val asc = max(ps.maxOfOrNull { it.axis() } ?: 0, (em(env, level) * 0.7f).roundToInt())
        val desc = max(ps.maxOfOrNull { it.height - it.axis() } ?: 0, (em(env, level) * 0.22f).roundToInt())
        layout(ps.sumOf { it.width }, asc + desc, mapOf(MathBaseline to asc)) {
            var x = 0
            ps.forEach { it.place(x, asc - it.axis()); x += it.width }
        }
    }
}

// ---------------------------------------------------------------------------
// Rows

@Composable
fun RowView(row: MathRow, level: Int) {
    val env = LocalMath.current
    val active = env.cursorRow === row
    val items = row.items.toList()
    Layout(
        content = {
            // The top-level input shows just the cursor when empty; inner slots show a box.
            if (items.isEmpty() && row.parent != null) Placeholder(row, level, active)
            items.forEachIndexed { i, n -> NodeView(n, row, i, level) }
            if (active) Cursor(Modifier.layoutId("cursor"))
        },
    ) { ms, _ ->
        val em = em(env, level)
        val cursorM = ms.firstOrNull { it.layoutId == "cursor" }
        val parts = ms.filter { it.layoutId != "cursor" }
        val placeables = arrayOfNulls<Placeable>(parts.size)
        val axes = IntArray(parts.size)

        // Pass 1: everything except stretchy brackets.
        parts.forEachIndexed { k, m ->
            if (m.layoutId != "paren") {
                val p = m.measure(Loose)
                placeables[k] = p
                axes[k] = p.axis()
            }
        }
        // Exponents sit 0.38 em up, and rise further only when the thing before
        // them is taller than plain text (a bracket, fraction or matrix).
        // Comparing against the text ascent matters: a letter's layout ascent
        // includes the font's internal leading, which is not a reason to lift.
        val textAsc = parts.indices.filter { parts[it].layoutId == "sym" }.maxOfOrNull { axes[it] } ?: (em * 0.78f).roundToInt()
        val baseAsc = parts.indices.filter { parts[it].layoutId != "pow" && placeables[it] != null }.maxOfOrNull { axes[it] } ?: textAsc
        parts.forEachIndexed { k, m ->
            if (m.layoutId == "pow" && k > 0) {
                val prevAsc = if (parts[k - 1].layoutId == "paren") max(baseAsc, textAsc) else axes[k - 1]
                val extra = prevAsc - textAsc
                if (extra > 0) axes[k] += extra
            }
        }
        // The row's ascent and descent: at least one line of text, and as much as its tallest part.
        var asc = (em * 0.78f).roundToInt()
        var desc = (em * 0.24f).roundToInt()
        parts.indices.forEach { k ->
            val p = placeables[k] ?: return@forEach
            asc = max(asc, axes[k])
            desc = max(desc, p.height - axes[k])
        }
        // Pass 2: each pair of brackets as tall as what's between them (inner pairs first, so outer
        // ones enclose them); an unmatched bracket spans to the end or start of the row.
        val itemOffsetForParens = if (items.isEmpty() && row.parent != null) 1 else 0
        fun bracketText(k: Int) = (items.getOrNull(k - itemOffsetForParens) as? Sym)?.text
        val partner = IntArray(parts.size) { -1 }
        val open = ArrayDeque<Int>()
        parts.forEachIndexed { k, m ->
            if (m.layoutId != "paren") return@forEachIndexed
            when (bracketText(k)) {
                "(", "[", "{" -> open.addLast(k)
                ")", "]", "}" -> open.removeLastOrNull()?.let { j -> partner[j] = k; partner[k] = j }
            }
        }
        val parenIndices = parts.indices.filter { parts[it].layoutId == "paren" }
        val span = { k: Int ->
            val p = partner[k]
            when {
                p >= 0 -> minOf(k, p) + 1 until maxOf(k, p)
                bracketText(k) in setOf("(", "[", "{") -> k + 1 until parts.size
                else -> 0 until k
            }
        }
        parenIndices.sortedBy { k -> span(k).let { it.last - it.first } }.forEach { k ->
            // Measured already, as the partner of an earlier bracket (each child is measured once).
            if (placeables[k] != null) return@forEach
            var a = (em * 0.78f).roundToInt()
            var d = (em * 0.24f).roundToInt()
            for (j in span(k)) {
                val p = placeables.getOrNull(j) ?: continue
                if (parts[j].layoutId == "paren" && partner[j] != k) {
                    // An already-sized inner bracket: enclose it with a little room.
                    val pad = (em * 0.06f).roundToInt()
                    a = max(a, axes[j] + pad); d = max(d, p.height - axes[j] + pad)
                } else if (parts[j].layoutId != "paren") {
                    a = max(a, axes[j]); d = max(d, p.height - axes[j])
                }
            }
            placeables[k] = parts[k].measure(Constraints.fixed((em * 0.34f).roundToInt(), a + d))
            axes[k] = a
            // Both brackets of a pair get the same size.
            val p = partner[k]
            if (p >= 0) { placeables[p] = parts[p].measure(Constraints.fixed((em * 0.34f).roundToInt(), a + d)); axes[p] = a }
        }
        parenIndices.forEach { k -> asc = max(asc, axes[k]); desc = max(desc, placeables[k]!!.height - axes[k]) }
        val cursor = cursorM?.measure(Constraints.fixed(2.dp.roundToPx(), max(asc + desc, (em * 1.05f).roundToInt())))
        val width = placeables.sumOf { it!!.width }
        layout(width, asc + desc, mapOf(MathBaseline to asc)) {
            var x = 0
            var cursorX = 0
            val itemOffset = if (items.isEmpty() && row.parent != null) 1 else 0
            placeables.forEachIndexed { k, p ->
                if (k - itemOffset == env.cursorIndex && items.isNotEmpty()) cursorX = x
                p!!.place(x, asc - axes[k])
                x += p.width
            }
            if (items.isNotEmpty() && env.cursorIndex >= items.size) cursorX = x
            if (items.isEmpty()) cursorX = if (row.parent != null) (em * 0.12f).roundToInt() else cursor?.width ?: 0
            cursor?.place(cursorX - cursor.width / 2, (asc + desc - cursor.height) / 2)
        }
    }
}

@Composable
private fun Cursor(modifier: Modifier) {
    val env = LocalMath.current
    val blink = rememberInfiniteTransition(label = "cursor")
    val alpha by blink.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(530, delayMillis = 300), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(modifier.background(env.accent.copy(alpha = alpha)))
}

@Composable
private fun Placeholder(row: MathRow, level: Int, active: Boolean) {
    val env = LocalMath.current
    val onTap = env.onTap
    val tap = if (onTap == null) Modifier else Modifier.pointerInput(row, onTap) { detectTapGestures { onTap(row, 0) } }
    Layout(
        content = {
            Canvas(tap) {
                val c = if (active) env.accent else env.faint
                if (env.emptyAsDot) {
                    drawCircle(c, radius = size.width * 0.2f, center = Offset(size.width / 2, size.height * 0.62f))
                } else {
                    val s = 1.5.dp.toPx()
                    drawRoundRect(
                        c,
                        topLeft = Offset(s / 2, s / 2),
                        size = Size(size.width - s, size.height - s),
                        cornerRadius = CornerRadius(size.width * 0.12f),
                        style = Stroke(s),
                    )
                }
            }
        },
    ) { ms, _ ->
        val em = em(env, level)
        val w = (em * if (env.emptyAsDot) 0.36f else 0.56f).roundToInt()
        val h = (em * 0.7f).roundToInt()
        val p = ms[0].measure(Constraints.fixed(w, h))
        val gap = (em * 0.06f).roundToInt()
        layout(w + 2 * gap, h, mapOf(MathBaseline to h)) { p.place(gap, 0) }
    }
}

// ---------------------------------------------------------------------------
// Nodes

@Composable
private fun NodeView(n: Node, row: MathRow, index: Int, level: Int) {
    when (n) {
        is Sym -> SymView(n, row, index, level)
        is Const -> ConstView(n, level, tapTo(row, index).layoutId("sym"), power = row.items.getOrNull(index + 1) as? Pow)
        is Frac -> FracLayout(level, line = true, num = { RowView(n.num, childLevel(level)) }, den = { RowView(n.den, childLevel(level)) })
        is Pow -> {
            val before = row.items.getOrNull(index - 1)
            // Already drawn by the constant before it (c₀²); keep an empty spot so the cursor can still reach it.
            val lastPiece = (before as? Const)?.let { Constant.byId(it.id)?.pieces?.last() }
            if (lastPiece != null && lastPiece.sub.isNotEmpty() && lastPiece.sup.isEmpty()) Box(Modifier.layoutId("pow")) else PowView(n, level)
        }
        is Sqrt -> RadicalView(level, index = null, body = { RowView(n.arg, level) })
        is Root -> RadicalView(level, index = { RowView(n.index, level + 2) }, body = { RowView(n.arg, level) })
        is Func -> FuncView(n, row, index, level)
        is BigOp -> BigOpView(n, level)
        is Integral -> IntegralView(n, level)
        is Derivative -> DerivativeView(n, level)
        is Scripted -> Scripts(
            level,
            tapTo(row, index),
            base = { RowView(n.base, level) },
            sub = if (n.sub.isEmpty) null else ({ RowView(n.sub, level + 1) }),
            sup = if (n.sup.isEmpty) null else ({ RowView(n.sup, level + 1) }),
        )
        is Binom -> Fenced(Delim.Paren, level) {
            FracLayout(level, line = false, num = { RowView(n.n, childLevel(level)) }, den = { RowView(n.k, childLevel(level)) })
        }
        is Matrix -> Fenced(Delim.Bracket, level) { MatrixGrid(n, level) }
    }
}

/** Fractions keep full size at the top level, then shrink. */
private fun childLevel(level: Int) = if (level == 0) 0 else level + 1

/** Functions written with a symbol rather than their name. */
private val FUNCTION_NAMES = mapOf(
    "gamma" to "Γ", "zeta" to "ζ",
    "grad" to "∇", "div" to "∇·", "curl" to "∇×", "laplacian" to "∇²", "jacobian" to "J", "hessian" to "H",
    "perm" to "P", "normpdf" to "φ", "normcdf" to "Φ", "binompdf" to "Bin", "poissonpdf" to "Pois", "total" to "Σ",
    "sd" to "s", "psd" to "σ", "median" to "med", "digamma" to "ψ", "zetaprime" to "ζ′",
    "hadamard" to "∘", "kron" to "⊗", "lambertw" to "W",
)

private val SPACED = setOf("+", "−", "×", "=", "mod", "<", ">", "≤", "≥", "or", ":=", "≠", "≈", "∈", "→", "↦", "±")

@Composable
private fun SymView(s: Sym, row: MathRow, index: Int, level: Int) {
    val env = LocalMath.current
    val density = LocalDensity.current
    val tap = tapTo(row, index)
    when (val t = s.text) {
        "(", ")" -> DelimCanvas(Delim.Paren, left = t == "(", level, tap.layoutId("paren"))
        // Google Sans Flex's own thin space is only 1/16 em, too narrow to show
        // digit groups, so spacing is drawn at a fixed fraction of the font size.
        Formatter.THIN_SPACE -> Spacer(tap.width(with(density) { (env.size(level).toPx() * 0.2f).toDp() }))
        else -> {
            val spaced = t in SPACED && level == 0 && !(t == "−" && index == 0)
            // The previous answer shows as "Ans", like the key.
            val shown = if (t == "ans") "Ans" else t
            // Variables are italic; constants, the "d" of derivatives and the transpose T are upright.
            val custom = com.example.cas.cas.CustomSymbol.decode(t)
            if (custom != null) {
                // A built symbol: its letter with the accent above, then the scripts.
                CustomSymbolView(custom, level, tap.layoutId("sym"))
            } else if (t.length == 2 && t.startsWith(com.example.cas.engine.LatexParser.UPRIGHT)) {
                // An upright letter from LaTeX (\mathrm{m}): roman, without the marker.
                MathText(t.substring(1), level, tap.layoutId("sym"))
            } else if (env.computerModern && t.isNotEmpty() && t.all { it == '′' }) {
                // A typed prime: raised and small, as TeX draws y′ (its prime glyph is made for that).
                Scripts(level, tap.layoutId("sym"), base = { Box(Modifier) }, sup = { MathText(t, level + 1) })
            } else if (env.computerModern && t.length > 1 && t[0].isLetter() && t.drop(1).all { it == '′' }) {
                Scripts(level, tap.layoutId("sym"), base = { MathText(t.take(1), level, italic = true) }, sup = { MathText(t.drop(1), level + 1) })
            } else if (Regex("C[0-9]+").matches(t)) {
                // Constants of integration C₁, C₂: an italic C with the number as a subscript.
                Scripts(level, tap.layoutId("sym"), base = { MathText("C", level, italic = true) }, sub = { MathText(t.drop(1), level + 1) })
            } else {
                // (On key labels "d" is the derivative's d; in the display it's always typed as a variable.)
                // Variables are italic, including y′ and y″.
                val letter = t.isNotEmpty() && t[0].isLetter() && t.drop(1).all { it == '′' }
                // Letters are italic, i, e and π included; capital Greek stays upright (as in TeX), and so
                // does T, the transpose.
                val upperGreek = t[0] in 'Α'..'Ω'
                val italic = letter && t != "T" && !upperGreek && !(t == "d" && !env.computerModern)
                val pad = with(density) { (env.size(level).toPx() * if (spaced) 0.16f else 0f).toDp() }
                // A little room after commas in lists like solve(x + y = 3, x − y = 1).
                val after = with(density) { (env.size(level).toPx() * if (t == ",") 0.25f else 0f).toDp() }
                MathText(shown, level, tap.layoutId("sym").padding(start = pad, end = pad + after), italic = italic)
            }
        }
    }
}

@Composable
private fun ConstView(c: Const, level: Int, modifier: Modifier, power: Pow? = null) {
    val pieces = Constant.byId(c.id)?.pieces ?: listOf(com.example.cas.engine.Piece(c.id))
    val last = pieces.last()
    // A power right after a subscripted constant sits above the subscript, as in TeX (c₀², mₑ²).
    val stackPower = power != null && last.sub.isNotEmpty() && last.sup.isEmpty()
    // A constant that already has a superscript gets brackets before a power: (e⁻)², not e⁻².
    if (power != null && last.sup.isNotEmpty()) {
        Fenced(Delim.Paren, level) { ConstPieces(pieces, level, modifier, null) }
    } else {
        ConstPieces(pieces, level, modifier, if (stackPower) power else null)
    }
}

@Composable
private fun ConstPieces(pieces: List<com.example.cas.engine.Piece>, level: Int, modifier: Modifier, power: Pow?) {
    val stackPower = power != null
    Layout(
        modifier = modifier,
        content = {
            pieces.forEachIndexed { k, p ->
                Scripts(
                    level,
                    base = { MathText(p.text, level, italic = p.italic) },
                    sub = p.sub.takeIf { it.isNotEmpty() }?.let { t -> @Composable { MathText(t, level + 1) } },
                    sup = if (k == pieces.lastIndex && stackPower) ({ RowView(power!!.exp, level + 1) })
                    else if (p.sup.isNotEmpty()) ({ MathText(p.sup, level + 1) })
                    else null,
                )
            }
        },
    ) { ms, _ ->
        val ps = ms.map { it.measure(Loose) }
        val asc = ps.maxOf { it.axis() }
        val desc = ps.maxOf { it.height - it.axis() }
        layout(ps.sumOf { it.width }, asc + desc, mapOf(MathBaseline to asc)) {
            var x = 0
            ps.forEach { it.place(x, asc - it.axis()); x += it.width }
        }
    }
}

/** Base with optional subscript and superscript to its right. */
@Composable
private fun Scripts(
    level: Int,
    modifier: Modifier = Modifier,
    base: @Composable () -> Unit,
    sub: (@Composable () -> Unit)? = null,
    sup: (@Composable () -> Unit)? = null,
) {
    val env = LocalMath.current
    Layout(
        modifier = modifier,
        content = {
            Box(Modifier.layoutId("base")) { base() }
            if (sub != null) Box(Modifier.layoutId("sub")) { sub() }
            if (sup != null) Box(Modifier.layoutId("sup")) { sup() }
        },
    ) { ms, _ ->
        val em = em(env, level)
        val b = ms.first { it.layoutId == "base" }.measure(Loose)
        val sb = ms.firstOrNull { it.layoutId == "sub" }?.measure(Loose)
        val sp = ms.firstOrNull { it.layoutId == "sup" }?.measure(Loose)
        val baseAxis = b.axis()
        val subDrop = (em * 0.22f).roundToInt()
        val supRise = (em * 0.42f).roundToInt()
        // Heights above the shared baseline.
        val asc = maxOf(baseAxis, sp?.let { supRise + it.axis() } ?: 0, sb?.let { it.axis() - subDrop } ?: 0)
        val desc = maxOf(b.height - baseAxis, sb?.let { subDrop + it.height - it.axis() } ?: 0)
        val w = b.width + max(sb?.width ?: 0, sp?.width ?: 0)
        layout(w, asc + desc, mapOf(MathBaseline to asc)) {
            b.place(0, asc - baseAxis)
            sb?.place(b.width, asc + subDrop - sb.axis())
            sp?.place(b.width, asc - supRise - sp.axis())
        }
    }
}

@Composable
private fun PowView(p: Pow, level: Int) {
    val env = LocalMath.current
    Layout(content = { RowView(p.exp, level + 1) }, modifier = Modifier.layoutId("pow")) { ms, _ ->
        val e = ms[0].measure(Loose)
        val raise = (em(env, level) * 0.38f).roundToInt()
        val axis = e.axis() + raise
        val pad = (em(env, level) * 0.04f).roundToInt()
        layout(e.width + pad, max(e.height, axis), mapOf(MathBaseline to axis)) { e.place(pad, 0) }
    }
}

@Composable
private fun FracLayout(level: Int, line: Boolean, num: @Composable () -> Unit, den: @Composable () -> Unit) {
    val env = LocalMath.current
    Layout(
        content = {
            Box { num() }
            Box { den() }
            Box(Modifier.background(if (line) env.color else Color.Transparent))
        },
    ) { ms, _ ->
        val em = em(env, level)
        val n = ms[0].measure(Loose)
        val d = ms[1].measure(Loose)
        val t = max(1, (em * 0.055f).roundToInt())
        val gap = (em * if (line) 0.1f else 0.04f).roundToInt()
        val pad = (em * 0.12f).roundToInt()
        val w = max(n.width, d.width) + 2 * pad
        val bar = ms[2].measure(Constraints.fixed(w, t))
        val lineY = n.height + gap
        val axis = lineY + t / 2 + mathAxis(env, level).roundToInt()
        layout(w, lineY + t + gap + d.height, mapOf(MathBaseline to axis)) {
            n.place((w - n.width) / 2, 0)
            bar.place(0, lineY)
            d.place((w - d.width) / 2, lineY + t + gap)
        }
    }
}

@Composable
private fun RadicalView(level: Int, index: (@Composable () -> Unit)?, body: @Composable () -> Unit) {
    val env = LocalMath.current
    Layout(
        content = {
            Box { body() }
            Canvas(Modifier) {
                val sw = max(1.dp.toPx(), size.height * 0.045f)
                val tick = size.height * 0.62f
                val sign = env.size(level).toPx() * 0.5f
                val path = Path().apply {
                    moveTo(sw, tick)
                    lineTo(sign * 0.3f, tick - sign * 0.12f)
                    lineTo(sign * 0.62f, size.height - sw)
                    lineTo(sign, sw / 2)
                    lineTo(size.width, sw / 2)
                }
                drawPath(path, env.color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            if (index != null) Box { index() }
        },
    ) { ms, _ ->
        val em = em(env, level)
        val b = ms[0].measure(Loose)
        val idx = if (ms.size > 2) ms[2].measure(Loose) else null
        val sign = (em * 0.5f).roundToInt()
        val top = (em * 0.16f).roundToInt()
        val h = b.height + top
        val shift = max(0, (idx?.width ?: 0) - (sign * 0.45f).roundToInt())
        val w = shift + sign + b.width + (em * 0.06f).roundToInt()
        val canvas = ms[1].measure(Constraints.fixed(w - shift, h))
        val idxLift = idx?.let { max(0, it.height - (h * 0.42f).roundToInt()) } ?: 0
        val axis = b.axis() + top + idxLift
        layout(w, h + idxLift, mapOf(MathBaseline to axis)) {
            canvas.place(shift, idxLift)
            b.place(shift + sign, top + idxLift)
            idx?.place(max(0, shift + (sign * 0.45f).roundToInt() - idx.width), 0)
        }
    }
}

@Composable
private fun FuncView(f: Func, row: MathRow, index: Int, level: Int) {
    val env = LocalMath.current
    val tap = tapTo(row, index)
    when (f.name) {
        "abs" -> Fenced(Delim.Bar, level) { RowView(f.args[0], level) }
        "floor" -> Fenced(Delim.Floor, level) { RowView(f.args[0], level) }
        "ceil" -> Fenced(Delim.Ceil, level) { RowView(f.args[0], level) }
        // Fractional part: {x}.
        "frac" -> Fenced(Delim.Brace, level) { RowView(f.args[0], level) }
        // Nearest integer: ⌊x⌉.
        "round" -> Fenced(Delim.Floor, level, right = Delim.Ceil) { RowView(f.args[0], level) }
        // Products of two matrices or vectors, written between them: A · B, A × B, A ∘ B, A ⊗ B.
        "dot", "cross", "hadamard", "kron" -> if (f.args.size == 2) AxisRow(level) {
            RowView(f.args[0], level)
            MathText(" " + when (f.name) { "dot" -> "·"; "cross" -> "×"; "hadamard" -> "∘"; else -> "⊗" } + " ", level, tap)
            RowView(f.args[1], level)
        } else AxisRow(level) {
            MathText(FUNCTION_NAMES[f.name] ?: f.name, level, tap)
            FuncArguments(f, level)
        }
        // lim with "x → a" underneath, then the expression.
        "lim" -> AxisRow(level) {
            Layout(content = { MathText("lim", level, tap); RowView(f.args[1], level + 1) }) { ms, _ ->
                val top = ms[0].measure(Loose)
                val bottom = ms[1].measure(Loose)
                val w = max(top.width, bottom.width)
                layout(w, top.height + bottom.height, mapOf(MathBaseline to top.axis())) {
                    top.place((w - top.width) / 2, 0)
                    bottom.place((w - bottom.width) / 2, top.height - (em(env, level) * 0.12f).roundToInt())
                }
            }
            MathText("\u2009", level)
            // A body that's already one bracketed group (maybe with a power) gets no extra brackets.
            if (alreadyBracketed(f.args[0])) RowView(f.args[0], level)
            else Fenced(Delim.Paren, level) { RowView(f.args[0], level) }
        }
        "log" -> AxisRow(level) {
            Scripts(level, tap, base = { MathText("log", level) }, sub = { RowView(f.args[0], level + 1) })
            Fenced(Delim.Paren, level) { RowView(f.args[1], level) }
        }
        // ∮ with its circle underneath, then f dz.
        "contour" -> ContourView(f, level)
        // Res with the point underneath: Res_{z=a}(f).
        "residue" -> AxisRow(level) {
            Scripts(level, tap, base = { MathText("Res", level) }, sub = { RowView(f.args[1], level + 1) })
            Fenced(Delim.Paren, level) { RowView(f.args[0], level) }
        }
        // Names that carry a script: Φ⁻¹(p), s²(list), Bin≤(n, p, k).
        "invnorm", "var", "binomcdf" -> AxisRow(level) {
            when (f.name) {
                "invnorm" -> Scripts(level, tap, base = { MathText("Φ", level) }, sup = { MathText("−1", level + 1) })
                "var" -> Scripts(level, tap, base = { MathText("s", level, italic = true) }, sup = { MathText("2", level + 1) })
                else -> Scripts(level, tap, base = { MathText("Bin", level) }, sub = { MathText("≤", level + 1) })
            }
            FuncArguments(f, level)
        }
        else -> AxisRow(level) {
            MathText(FUNCTION_NAMES[f.name] ?: f.name, level, tap)
            val lone = f.args.singleOrNull()?.items?.singleOrNull()
            if (lone is Matrix || (lone is Func && lone.name == "abs")) {
                // det[…] and ln|x| read better without extra brackets.
                RowView(f.args[0], level)
            } else {
                FuncArguments(f, level)
            }
        }
    }
}

// TeX's display ∫ and ∮ (MathJax_Size2): ink from −0.862 em to 1.36 em (2.222 em tall) and
// from x = 0.055 em to 0.944 em, in the glyph's own font size.
private const val OPERATOR_HEIGHT_EM = 2.222f
private const val OPERATOR_ASCENT_EM = 1.36f
private const val OPERATOR_LEFT_EM = 0.055f
/** Glyph ink width over height, so the box fits the ink exactly. */
private const val OPERATOR_WIDTH = (0.944f - 0.055f) / OPERATOR_HEIGHT_EM

/** A large operator from TeX's fonts, scaled so its ink fills the box it's measured into. */
@Composable
private fun OperatorGlyph(glyph: String) {
    val env = LocalMath.current
    val typeface = env.mathGlyphs.operators
    Canvas(Modifier) {
        val textSize = size.height / OPERATOR_HEIGHT_EM
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = textSize
            color = env.color.toArgb()
        }
        drawIntoCanvas { it.nativeCanvas.drawText(glyph, -OPERATOR_LEFT_EM * textSize, OPERATOR_ASCENT_EM * textSize, paint) }
    }
}

/** (a, b, c): a function's arguments in brackets. */
@Composable
private fun FuncArguments(f: Func, level: Int) {
    Fenced(Delim.Paren, level) {
        AxisRow(level) {
            f.args.forEachIndexed { i, a ->
                if (i > 0) MathText(", ", level)
                RowView(a, level)
            }
        }
    }
}

/**
 * ∮ over a circle: the ∫ sign with a ring through it (no bundled font has ∮),
 * the circle |z − a| = r underneath, then f and dz.
 */
@Composable
private fun ContourView(f: Func, level: Int) {
    val env = LocalMath.current
    // The variable is the letter inside |…| in the circle, z unless typed otherwise.
    val variable = (f.args[1].items.firstOrNull() as? Func)?.args?.firstOrNull()?.items
        ?.firstOrNull { it is Sym && it.text.length == 1 && it.text[0].isLetter() }?.let { (it as Sym).text } ?: "z"
    AxisRow(level) {
        Layout(
            content = {
                OperatorGlyph("∮")
                RowView(f.args[1], level + 1)
            },
        ) { ms, _ ->
            val em = em(env, level)
            val symH = (em * 1.6f).roundToInt()
            val symW = (symH * OPERATOR_WIDTH).roundToInt()
            val sym = ms[0].measure(Constraints.fixed(symW, symH))
            val lo = ms[1].measure(Loose)
            val loX = (symW * 0.45f).roundToInt()
            val h = symH + max(0, lo.height - (symH * 0.25f).roundToInt())
            val w = max(symW, loX + lo.width) + (em * 0.1f).roundToInt()
            val axis = symH / 2 + mathAxis(env, level).roundToInt()
            layout(w, h, mapOf(MathBaseline to axis)) {
                sym.place(0, 0)
                lo.place(loX, h - lo.height)
            }
        }
        RowView(f.args[0], level)
        MathText("\u00A0d", level)
        MathText(variable, level, italic = true)
    }
}

/** True while the cursor is in any slot of [node], however deeply nested. */
private fun cursorInside(env: MathEnv, node: Node): Boolean {
    var r = env.cursorRow
    while (r != null) {
        val owner = r.parent ?: return false
        if (owner === node) return true
        r = owner.parent
    }
    return false
}

@Composable
private fun BigOpView(b: BigOp, level: Int) {
    val env = LocalMath.current
    AxisRow(level) {
        Layout(
            content = {
                RowView(b.upper, level + 1)
                Canvas(Modifier) {
                    val sw = max(1.2.dp.toPx(), size.height * 0.07f)
                    val w = size.width
                    val h = size.height
                    val path = Path()
                    if (b.kind == BigOpKind.Sum) {
                        path.moveTo(w * 0.92f, h * 0.12f)
                        path.lineTo(w * 0.9f, sw / 2)
                        path.lineTo(w * 0.08f, sw / 2)
                        path.lineTo(w * 0.55f, h * 0.5f)
                        path.lineTo(w * 0.08f, h - sw / 2)
                        path.lineTo(w * 0.9f, h - sw / 2)
                        path.lineTo(w * 0.92f, h * 0.88f)
                    } else {
                        path.moveTo(w * 0.04f, sw / 2); path.lineTo(w * 0.96f, sw / 2)
                        path.moveTo(w * 0.22f, sw / 2); path.lineTo(w * 0.22f, h)
                        path.moveTo(w * 0.78f, sw / 2); path.lineTo(w * 0.78f, h)
                    }
                    drawPath(path, env.color, style = Stroke(sw, join = StrokeJoin.Round))
                }
                AxisRow(level + 1) {
                    RowView(b.variable, level + 1)
                    MathText("=", level + 1)
                    RowView(b.lower, level + 1)
                }
            },
        ) { ms, _ ->
            val em = em(env, level)
            val up = ms[0].measure(Loose)
            val symW = (em * 0.95f).roundToInt()
            val symH = (em * 1.15f).roundToInt()
            val sym = ms[1].measure(Constraints.fixed(symW, symH))
            val lo = ms[2].measure(Loose)
            val g = (em * 0.08f).roundToInt()
            val w = maxOf(up.width, symW, lo.width) + (em * 0.15f).roundToInt()
            val symY = up.height + g
            val axis = symY + symH / 2 + mathAxis(env, level).roundToInt()
            layout(w, symY + symH + g + lo.height, mapOf(MathBaseline to axis)) {
                up.place((w - up.width) / 2, 0)
                sym.place((w - symW) / 2, symY)
                lo.place((w - lo.width) / 2, symY + symH + g)
            }
        }
        RowView(b.body, level)
    }
}

@Composable
private fun IntegralView(n: Integral, level: Int) {
    val env = LocalMath.current
    // Limit boxes show while you edit the integral; left empty, it's an antiderivative and they hide.
    val showLimits = !(n.lower.isEmpty && n.upper.isEmpty) || cursorInside(env, n)
    AxisRow(level) {
        Layout(
            content = {
                OperatorGlyph("∫")
                if (showLimits) RowView(n.upper, level + 1) else Box(Modifier)
                if (showLimits) RowView(n.lower, level + 1) else Box(Modifier)
            },
        ) { ms, _ ->
            val em = em(env, level)
            val symH = (em * 1.6f).roundToInt()
            val symW = (symH * OPERATOR_WIDTH).roundToInt()
            val sym = ms[0].measure(Constraints.fixed(symW, symH))
            val up = ms[1].measure(Loose)
            val lo = ms[2].measure(Loose)
            // The upper limit sits clear of the ∫'s top hook.
            val upX = (symW * 1.15f).roundToInt()
            val loX = (symW * 0.55f).roundToInt()
            val top = max(0, up.height - (symH * 0.3f).roundToInt())
            val h = top + symH + max(0, lo.height - (symH * 0.3f).roundToInt())
            val w = max(upX + up.width, loX + lo.width) + (em * 0.1f).roundToInt()
            val axis = top + symH / 2 + mathAxis(env, level).roundToInt()
            layout(w, h, mapOf(MathBaseline to axis)) {
                sym.place(0, top)
                up.place(upX, 0)
                lo.place(loX, h - lo.height)
            }
        }
        RowView(n.body, level)
        MathText("\u00A0d", level)
        VariableBox(n.variable, level, cursorInside(env, n))
    }
}

@Composable
private fun DerivativeView(d: Derivative, level: Int) {
    val env = LocalMath.current
    // ∂ for partial derivatives, an upright d otherwise.
    val dee = if (d.partial) "∂" else "d"
    val editing = cursorInside(env, d)
    AxisRow(level) {
        // With an order: dⁿ over dxⁿ. The order box shows while you edit the derivative.
        val showOrder = !d.order.isEmpty || cursorInside(env, d)
        FracLayout(
            level,
            line = true,
            num = {
                if (showOrder) Scripts(level, base = { MathText(dee, level) }, sup = { RowView(d.order, level + 1) })
                else MathText(dee, level)
            },
            den = {
                AxisRow(level) {
                    MathText(dee, level)
                    if (showOrder) Scripts(level, base = { VariableBox(d.variable, level, editing) }, sup = { OrderMirror(d.order, level + 1) })
                    else VariableBox(d.variable, level, editing)
                }
            },
        )
        Fenced(Delim.Paren, level) { RowView(d.body, level) }
        // "|x=a" only when a point is given (or being typed).
        if (!d.at.isEmpty || cursorInside(env, d)) {
            Scripts(
                level,
                base = { DelimCanvasFixed(Delim.Bar, level) },
                sub = {
                    AxisRow(level + 1) {
                        MathText(d.variable.plainText().ifEmpty { "x" }, level + 1, italic = true, color = env.faint)
                        MathText("=", level + 1)
                        RowView(d.at, level + 1)
                    }
                },
            )
        }
    }
}

/**
 * The variable of a derivative or integral (the x of dx). It's an ordinary
 * editable box, so tap it (or press ← from the start of the body) and type
 * another letter; while the derivative or integral is being edited it's tinted
 * to show it can be changed.
 */
@Composable
private fun VariableBox(variable: MathRow, level: Int, editing: Boolean) {
    val env = LocalMath.current
    Box(
        if (editing) Modifier.clip(RoundedCornerShape(3.dp)).background(env.accent.copy(alpha = 0.16f)) else Modifier,
    ) { RowView(variable, level) }
}

/** The order repeated on dxⁿ: drawn only (the editable box is on the dⁿ). */
@Composable
private fun OrderMirror(order: MathRow, level: Int) {
    val env = LocalMath.current
    MathText(order.plainText().ifEmpty { "n" }, level, color = if (order.isEmpty) env.faint else null)
}

@Composable
private fun MatrixGrid(m: Matrix, level: Int) {
    val env = LocalMath.current
    Layout(content = { m.cells.forEach { RowView(it, level) } }) { ms, _ ->
        val em = em(env, level)
        val ps = ms.map { it.measure(Loose) }
        val colW = IntArray(m.cols) { c -> (0 until m.rows).maxOf { r -> ps[r * m.cols + c].width } }
        val rowAsc = IntArray(m.rows) { r -> (0 until m.cols).maxOf { c -> ps[r * m.cols + c].axis() } }
        val rowDesc = IntArray(m.rows) { r -> (0 until m.cols).maxOf { c -> ps[r * m.cols + c].let { it.height - it.axis() } } }
        val hGap = (em * 0.7f).roundToInt()
        val vGap = (em * 0.22f).roundToInt()
        val w = colW.sum() + hGap * (m.cols - 1) + (em * 0.2f).roundToInt()
        val h = (0 until m.rows).sumOf { rowAsc[it] + rowDesc[it] } + vGap * (m.rows - 1)
        layout(w, h, mapOf(MathBaseline to h / 2 + mathAxis(env, level).roundToInt())) {
            var y = 0
            for (r in 0 until m.rows) {
                var x = (em * 0.1f).roundToInt()
                for (c in 0 until m.cols) {
                    val p = ps[r * m.cols + c]
                    p.place(x + (colW[c] - p.width) / 2, y + rowAsc[r] - p.axis())
                    x += colW[c] + hGap
                }
                y += rowAsc[r] + rowDesc[r] + vGap
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Delimiters

enum class Delim { Paren, Bracket, Bar, Floor, Ceil, Brace }

/** Content with delimiters on both sides, as tall as the content. */
@Composable
private fun Fenced(kind: Delim, level: Int, right: Delim = kind, content: @Composable () -> Unit) {
    val env = LocalMath.current
    Layout(
        content = {
            Box { content() }
            DelimCanvas(kind, left = true, level, Modifier)
            DelimCanvas(right, left = false, level, Modifier)
        },
    ) { ms, _ ->
        val em = em(env, level)
        val c = ms[0].measure(Loose)
        val pad = (em * 0.06f).roundToInt()
        val h = max(c.height, (em * 1.0f).roundToInt()) + 2 * pad
        val dw = (em * if (kind == Delim.Bar) 0.22f else 0.34f).roundToInt()
        val l = ms[1].measure(Constraints.fixed(dw, h))
        val r = ms[2].measure(Constraints.fixed(dw, h))
        val top = (h - c.height) / 2
        layout(dw * 2 + c.width, h, mapOf(MathBaseline to c.axis() + top)) {
            l.place(0, 0)
            c.place(dw, top)
            r.place(dw + c.width, 0)
        }
    }
}

@Composable
private fun DelimCanvasFixed(kind: Delim, level: Int) {
    val env = LocalMath.current
    Layout(content = { DelimCanvas(kind, left = false, level, Modifier) }) { ms, _ ->
        val em = em(env, level)
        val h = (em * 1.4f).roundToInt()
        val p = ms[0].measure(Constraints.fixed((em * 0.22f).roundToInt(), h))
        layout(p.width, h, mapOf(MathBaseline to (h * 0.72f).roundToInt())) { p.place(0, 0) }
    }
}

@Composable
private fun DelimCanvas(kind: Delim, left: Boolean, level: Int, modifier: Modifier) {
    val env = LocalMath.current
    Canvas(modifier) {
        val sw = max(1.dp.toPx(), env.size(level).toPx() * 0.065f)
        val w = size.width
        val h = size.height
        val inner = if (left) w * 0.8f else w * 0.2f
        val outer = if (left) w * 0.25f else w * 0.75f
        val stroke = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            Delim.Paren -> {
                // Inset the ends: a text line's height includes the font's leading,
                // so a full-height bracket would tower over the digits beside it.
                val inset = sw + env.size(level).toPx() * 0.1f
                val bulge = if (left) w * 0.02f else w * 0.98f
                val path = Path().apply {
                    moveTo(inner, inset)
                    quadraticTo(bulge, h / 2, inner, h - inset)
                }
                drawPath(path, env.color, style = stroke)
            }
            Delim.Bracket -> {
                val path = Path().apply {
                    moveTo(inner, sw / 2); lineTo(outer, sw / 2); lineTo(outer, h - sw / 2); lineTo(inner, h - sw / 2)
                }
                drawPath(path, env.color, style = stroke)
            }
            Delim.Bar -> drawLine(env.color, Offset(w / 2, sw), Offset(w / 2, h - sw), sw, StrokeCap.Round)
            Delim.Brace -> {
                // A curly brace: two S-curves meeting at a point in the middle.
                val tip = if (left) w * 0.12f else w * 0.88f
                val mid = w * 0.5f
                val inset = sw + env.size(level).toPx() * 0.06f
                val path = Path().apply {
                    moveTo(inner, inset)
                    cubicTo(mid, inset, mid, h * 0.5f - (h * 0.12f), mid, h * 0.42f)
                    quadraticTo(mid, h * 0.5f, tip, h * 0.5f)
                    quadraticTo(mid, h * 0.5f, mid, h * 0.58f)
                    cubicTo(mid, h * 0.5f + (h * 0.12f), mid, h - inset, inner, h - inset)
                }
                drawPath(path, env.color, style = stroke)
            }
            Delim.Floor -> {
                val path = Path().apply { moveTo(outer, sw / 2); lineTo(outer, h - sw / 2); lineTo(inner, h - sw / 2) }
                drawPath(path, env.color, style = stroke)
            }
            Delim.Ceil -> {
                val path = Path().apply { moveTo(inner, sw / 2); lineTo(outer, sw / 2); lineTo(outer, h - sw / 2) }
                drawPath(path, env.color, style = stroke)
            }
        }
    }
}

/**
 * Shrinks its content (never enlarges it) so it fits the space it's given,
 * e.g. a binomial or matrix label inside a short key.
 */
@Composable
fun FitInside(fraction: Float = 0.8f, content: @Composable () -> Unit) {
    Layout(content) { ms, c ->
        val p = ms[0].measure(Loose)
        val availW = if (c.hasBoundedWidth) c.maxWidth * fraction else Float.MAX_VALUE
        val availH = if (c.hasBoundedHeight) c.maxHeight * fraction else Float.MAX_VALUE
        val s = minOf(1f, availW / p.width.coerceAtLeast(1), availH / p.height.coerceAtLeast(1))
        val w = (p.width * s).roundToInt()
        val h = (p.height * s).roundToInt()
        layout(w, h) {
            p.placeWithLayer((w - p.width) / 2, (h - p.height) / 2) {
                scaleX = s
                scaleY = s
            }
        }
    }
}


/** Whether a row is a single (…) group, optionally followed by powers: (1 + 1/n)ⁿ. */
internal fun alreadyBracketed(row: MathRow): Boolean {
    val items = row.items
    if ((items.firstOrNull() as? Sym)?.text != "(") return false
    var depth = 0
    for (k in items.indices) {
        when ((items[k] as? Sym)?.text) { "(" -> depth++; ")" -> depth-- }
        if (depth == 0) return items.drop(k + 1).all { it is Pow }
    }
    return false
}


/**
 * A symbol from the symbol builder, drawn as TeX would: the letter (italic if Latin or small
 * Greek, upright if capital Greek), its accent centred above (shifted a little right on
 * italic letters, as TeX does), and its subscript and superscript.
 */
@Composable
internal fun CustomSymbolView(sym: com.example.cas.cas.CustomSymbol, level: Int, modifier: Modifier = Modifier) {
    val base = sym.base
    val italic = base.length == 1 && base[0].isLetter() && base[0] !in 'Α'..'Ω'
    val scriptRow = { s: String -> MathRow(s.map { Sym(it.toString()) }.toMutableList()) }
    val accented: @Composable () -> Unit = { AccentedLetter(base, sym.accent, italic, level) }
    if (sym.sub.isEmpty() && sym.sup.isEmpty()) {
        Box(modifier) { accented() }
    } else {
        Scripts(
            level,
            modifier,
            base = accented,
            sub = if (sym.sub.isEmpty()) null else ({ RowView(scriptRow(sym.sub), level + 1) }),
            sup = if (sym.sup.isEmpty()) null else ({ RowView(scriptRow(sym.sup), level + 1) }),
        )
    }
}

@Composable
private fun AccentedLetter(base: String, accent: com.example.cas.cas.Accent?, italic: Boolean, level: Int) {
    val env = LocalMath.current
    if (accent == null) { MathText(base, level, italic = italic); return }
    // TeX's vector arrow is small. An arrow's ink is centred in its box (unlike the other accents),
    // so it's placed at full size and scaled down about its own centre.
    val isVector = accent == com.example.cas.cas.Accent.Vector
    Layout(content = {
        MathText(base, level, italic = italic)
        MathText(accent.glyph, level, if (isVector) Modifier.graphicsLayer { scaleX = 0.7f; scaleY = 0.7f } else Modifier)
    }) { ms, _ ->
        val b = ms[0].measure(Loose)
        val a = ms[1].measure(Loose)
        val em = em(env, level)
        // The accent's ink sits high in its box: overlap it well down onto the letter, and further
        // on letters without an ascender (x, v, α…), as TeX lowers accents to the x-height.
        val short = !isVector && base.length == 1 && base[0] in "acegmnopqrsuvwxyzαγεηικμνοπρστυφχψω"
        val overlap = (a.height * 0.55f + if (short) em * 0.22f else 0f).roundToInt()
        val top = max(0, a.height - overlap)
        val w = max(b.width, a.width)
        val shift = if (italic) (em * 0.08f).roundToInt() else 0
        val axis = top + b.axis()
        layout(w, top + b.height, mapOf(MathBaseline to axis)) {
            b.place((w - b.width) / 2, top)
            a.place((w - a.width) / 2 + shift, 0)
        }
    }
}
