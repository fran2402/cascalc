package com.example.cas.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.cas.editor.Editor
import com.example.cas.editor.MathCodec
import com.example.cas.editor.MathRow
import com.example.cas.editor.Selection
import com.example.cas.editor.Sym
import com.example.cas.engine.Formatter

/**
 * Selecting math as in an Android text field, for any [Editor] (the calculator's line, a
 * graph's lines): hold to select the number, letter or piece there, drag on to extend it, drag
 * either handle (they may cross), and Select all, Cut, Copy (as LaTeX) and Paste (LaTeX read back
 * into fractions, roots…). After a tap, the cursor's handle moves the cursor. [target] is the
 * editor being worked on (null when none is).
 */
class MathSelection(private val target: () -> Editor?) {
    /** The bar is showing (after holding the math, or tapping the cursor's handle). */
    var selecting by mutableStateOf(false)
        private set
    /** The teardrop under the cursor after a tap. */
    var cursorHandle by mutableStateOf(false)

    val hasSelection get() = target()?.hasSelection == true

    /** The selection's two ends as placed, anywhere in the expression; the editor's anchor is always on A's side. */
    private var selA: Pair<MathRow, Int>? = null
    private var selB: Pair<MathRow, Int>? = null
    /** The word the long press took: dragging on from it extends the selection from there. */
    private var word: Pair<MathRow, Pair<Int, Int>>? = null
    /** Which placed end the handle being dragged moves (decided when the drag starts, so ends can cross). */
    private var movingA = true

    /** Holding at (row, index): that word (a number, a letter, a fraction…) selected; nothing there: all of it. */
    fun start(row: MathRow?, index: Int) {
        val e = target() ?: return
        if (row == null || e.root.isEmpty) e.selectAll() else e.selectWord(row, index)
        selA = e.row to e.selectionStart; selB = e.row to e.selectionEnd
        word = e.row to (e.selectionStart to e.selectionEnd)
        selecting = true; cursorHandle = false
    }

    /** The finger, still down after the long press, at (row, index): from the word to there, either way. */
    fun dragSelect(row: MathRow, index: Int) {
        val e = target() ?: return
        val (wr, range) = word ?: return
        val (s0, s1) = range
        if (row === wr && index in s0..s1) { e.select(wr, s0, s1); selA = wr to s0; selB = wr to s1; return }
        val after = if (row === wr) index >= s1 else (Selection.between(wr, s0, row, index)?.let { (_, a, b) -> b >= a } ?: true)
        selA = wr to (if (after) s0 else s1); selB = row to index
        e.selectBetween(wr, if (after) s0 else s1, row, index)
    }

    fun beginHandleDrag(start: Boolean) {
        val e = target() ?: return
        val aFirst = (e.anchor ?: e.selectionStart) == e.selectionStart
        movingA = start == aFirst
    }

    /** The grabbed handle at (row, index): its end moves there, the other stays. */
    fun dragHandle(row: MathRow, index: Int) {
        val e = target() ?: return
        if (selA == null || selB == null) return
        if (movingA) selA = row to index else selB = row to index
        val a = selA!!; val b = selB!!
        e.selectBetween(a.first, a.second, b.first, b.second)
    }

    fun moveCursorTo(row: MathRow, index: Int) { target()?.setCursor(row, index); cursorHandle = true }
    fun showPasteBar() { selecting = true; cursorHandle = false }
    fun selectAll() { val e = target() ?: return; e.selectAll(); selA = e.root to 0; selB = e.root to e.root.items.size; selecting = true; cursorHandle = false }
    fun end() { target()?.clearSelection(); forget() }
    private fun forget() { selecting = false; selA = null; selB = null; word = null }

    /** A tap in the math: selecting ends, and the cursor (put there by the caller) gets its handle. */
    fun tapped() { forget(); cursorHandle = true }

    /** Before a key: a selection is replaced (true when the key was Backspace or AC, which only remove it). */
    fun beforeKey(action: KeyAction): Boolean {
        cursorHandle = false
        val e = target() ?: return false
        if (e.hasSelection) {
            val removed = e.deleteSelection()
            forget()
            return removed && (action == KeyAction.Backspace || action == KeyAction.Clear)
        }
        if (selecting) { e.clearSelection(); forget() }
        return false
    }

    private var clipCode: String? = null
    private var clipText: String? = null

    /** The selection as LaTeX for the clipboard (kept in full for pasting back here), or null. */
    fun copy(): String? {
        val e = target() ?: return null
        if (!e.hasSelection) return null
        val s = e.selected()
        clipCode = MathCodec.encode(s)
        clipText = runCatching { com.example.cas.engine.Latex.of(s) }.getOrElse { Formatter.plain(s) }
        return clipText
    }

    fun cut(): String? = copy()?.also { target()?.deleteSelection(); forget() }

    /** Pastes [text]: what was copied here comes back exactly; other text is read as LaTeX (or plain). */
    fun paste(text: String?) {
        val e = target() ?: return
        if (text.isNullOrBlank()) return
        val latex = text.trim().removeSurrounding("$$").removeSurrounding("$").removeSurrounding("\\(", "\\)").removeSurrounding("\\[", "\\]").trim()
        val row = if (text == clipText && clipCode != null) MathCodec.decode(clipCode!!)
        else runCatching { com.example.cas.engine.LatexParser.parse(latex) }.getOrNull()?.takeIf { !it.isEmpty }
            ?: MathRow(text.trim().map { Sym(it.toString()) }.toMutableList())
        e.deleteSelection()
        e.insertRow(row)
        forget()
    }
}

/**
 * Holding the math to select: seen before the math's own taps (which would otherwise take the
 * touch). Held still, the piece there is selected; dragging on while still holding extends it.
 * [coords] are this element's coordinates, to place the touch among [geometry]'s rows.
 */
internal fun Modifier.holdToSelect(sel: MathSelection, geometry: MathGeometry, coords: () -> LayoutCoordinates?, onLongPress: () -> Unit = {}): Modifier =
    pointerInput(sel, geometry) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var released = false
            val held = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (true) {
                    val c = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: run { released = true; return@withTimeoutOrNull }
                    if (!c.pressed) { released = true; return@withTimeoutOrNull }
                    if ((c.position - down.position).getDistance() > viewConfiguration.touchSlop) { released = true; return@withTimeoutOrNull }
                }
            }
            // A tap or a scroll: left to the math and the scrolling.
            if (released || held != null) return@awaitEachGesture
            fun place(p: Offset) = coords()?.takeIf { it.isAttached }?.let { geometry.hit(it.localToRoot(p)) }
            onLongPress()
            val at = place(down.position)
            sel.start(at?.first, at?.second ?: 0)
            down.consume()
            while (true) {
                val c = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                c.consume()
                if (!c.pressed) break
                if (at != null) place(c.position)?.let { (r, i) -> sel.dragSelect(r, i) }
            }
        }
    }

/**
 * The handles for [editor] in a box whose coordinates are [area]: the two selection handles
 * while something is selected, else the cursor's (which fades after a few seconds). [version]
 * changes with every edit, so they follow it.
 */
@Composable
internal fun SelectionOverlay(sel: MathSelection, editor: Editor, geometry: MathGeometry, area: LayoutCoordinates?, version: Int) {
    LaunchedEffect(sel.cursorHandle, version) { if (sel.cursorHandle) { kotlinx.coroutines.delay(4000); sel.cursorHandle = false } }
    if (sel.selecting && editor.hasSelection) SelectionHandles(sel, geometry, area, editor.row, editor.selectionStart, editor.selectionEnd, version)
    else if (sel.cursorHandle && !editor.hasSelection) CursorHandle(sel, geometry, area, editor.row, editor.index, version)
}

/**
 * The two teardrop handles under the selection's ends, as in any Android text field: drag one
 * to move that end anywhere in the expression (into a fraction, past a root…); the selection
 * takes in whole structures as it needs to.
 */
@Composable
internal fun SelectionHandles(sel: MathSelection, geometry: MathGeometry, area: androidx.compose.ui.layout.LayoutCoordinates?, row: MathRow, from: Int, to: Int, @Suppress("UNUSED_PARAMETER") version: Int) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val size = 22.dp
    val sizePx = with(density) { size.toPx() }
    @Suppress("UNUSED_EXPRESSION") geometry.tick
    val a = area?.takeIf { it.isAttached } ?: return
    val origin = a.positionInRoot()
    listOf(true, false).forEach { start ->
        val caretOrNull = geometry.caret(row, if (start) from else to)
        // Kept for the drag in progress (its gesture outlives this composition).
        val latest = androidx.compose.runtime.rememberUpdatedState(caretOrNull)
        val caret = caretOrNull ?: return@forEach
        val local = caret - origin
        if (local.x < -sizePx || local.x > a.size.width + sizePx) return@forEach
        // Dragged from where it was grabbed; the place is read just above the handle's tip.
        var drag by remember(start) { mutableStateOf(Offset.Zero) }
        var grab by remember(start) { mutableStateOf<Offset?>(null) }
        Canvas(
            Modifier
                .offset { androidx.compose.ui.unit.IntOffset((local.x - if (start) sizePx else 0f).toInt(), local.y.toInt()) }
                .size(size)
                .pointerInput(start) {
                    detectDragGestures(
                        onDragStart = { grab = latest.value; drag = Offset.Zero; sel.beginHandleDrag(start) },
                        onDragEnd = { grab = null },
                        onDragCancel = { grab = null },
                    ) { change, d ->
                        change.consume()
                        drag += d
                        val g = grab ?: return@detectDragGestures
                        val tip = g + drag - Offset(0f, with(density) { 14.dp.toPx() })
                        geometry.hit(tip)?.let { (r, index) -> sel.dragHandle(r, index) }
                    }
                },
        ) {
            // A circle with one square corner pointing up at the selection's edge.
            val r = this.size.width / 2
            drawCircle(colors.primary, r, Offset(r, r))
            if (start) drawRect(colors.primary, Offset(r, 0f), androidx.compose.ui.geometry.Size(r, r))
            else drawRect(colors.primary, Offset(0f, 0f), androidx.compose.ui.geometry.Size(r, r))
        }
    }
}

/** The teardrop under the cursor after a tap: drag it to move the cursor anywhere; tap it for Paste. */
@Composable
internal fun CursorHandle(sel: MathSelection, geometry: MathGeometry, area: androidx.compose.ui.layout.LayoutCoordinates?, row: MathRow, index: Int, @Suppress("UNUSED_PARAMETER") version: Int) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val size = 22.dp
    val sizePx = with(density) { size.toPx() }
    @Suppress("UNUSED_EXPRESSION") geometry.tick
    val a = area?.takeIf { it.isAttached } ?: return
    val caretOrNull = geometry.caret(row, index)
    val latest = androidx.compose.runtime.rememberUpdatedState(caretOrNull)
    val caret = caretOrNull ?: return
    val local = caret - a.positionInRoot()
    if (local.x < -sizePx || local.x > a.size.width + sizePx) return
    var drag by remember { mutableStateOf(Offset.Zero) }
    var grab by remember { mutableStateOf<Offset?>(null) }
    Canvas(
        Modifier
            .offset { androidx.compose.ui.unit.IntOffset((local.x - sizePx / 2).toInt(), local.y.toInt()) }
            .size(size)
            .pointerInput(Unit) { detectTapGestures { sel.showPasteBar() } }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { grab = latest.value; drag = Offset.Zero }, onDragEnd = { grab = null }, onDragCancel = { grab = null }) { change, d ->
                    change.consume()
                    drag += d
                    val g = grab ?: return@detectDragGestures
                    val tip = g + drag - Offset(0f, with(density) { 14.dp.toPx() })
                    geometry.hit(tip)?.let { (r, i) -> sel.moveCursorTo(r, i) }
                }
            },
    ) {
        // A circle with a point at the top: Android's insertion handle.
        val r = this.size.width / 2
        drawCircle(colors.primary, r, Offset(r, r * 1.1f))
        drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(r, 0f); lineTo(r * 1.75f, r * 0.75f); lineTo(r * 0.25f, r * 0.75f); close() }, colors.primary)
    }
}

/**
 * Shown while selecting in the input (after holding it): select all, cut, copy and paste, and ×
 * to stop. While it shows, a tap in the math (or ◀ ▶) moves the selection's end; any key replaces
 * what's selected.
 */
@Composable
internal fun SelectionBar(sel: MathSelection, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    @Suppress("DEPRECATION") val clipboard = LocalClipboardManager.current
    Row(
        modifier.padding(bottom = 4.dp).clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerHighest).horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        @Composable fun act(label: String, icon: ImageVector, enabled: Boolean = true, onClick: () -> Unit) =
            TextButton(onClick = onClick, enabled = enabled) {
                AppIcon(icon, contentDescription = null, tint = if (enabled) colors.primary else colors.outline, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp)); Text(tr(label))
            }
        val has = sel.hasSelection
        act("Select all", TableIcons.SelectAll) { sel.selectAll() }
        act("Cut", TableIcons.Cut, has) { sel.cut()?.let { clipboard.setText(androidx.compose.ui.text.AnnotatedString(it)) } }
        act("Copy", TableIcons.Copy, has) { sel.copy()?.let { clipboard.setText(androidx.compose.ui.text.AnnotatedString(it)) }; sel.end() }
        act("Paste", TableIcons.Paste, clipboard.hasText()) { sel.paste(clipboard.getText()?.text) }
        IconButton(onClick = { sel.end() }) { AppIcon(TableIcons.Close, contentDescription = tr("Done selecting"), tint = colors.onSurfaceVariant) }
    }
}

