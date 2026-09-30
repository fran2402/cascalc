package com.example.cas.editor

/**
 * Cursor-based editing of a [MathRow] tree. The cursor sits between two
 * items of [row] at [index]. All commands keep the tree valid, so the
 * expression can be evaluated or drawn at any moment.
 */
class Editor(initial: MathRow = MathRow()) {

    var root: MathRow = initial
        private set
    var row: MathRow = initial
        private set
    var index: Int = initial.items.size
        private set

    /** Called after every change, so the UI can redraw. */
    var onChange: () -> Unit = {}

    val isEmpty get() = root.isEmpty

    private fun changed() {
        // Matrices from the matrix key grow as their last row or column fills.
        growMatrices(root, row)
        onChange()
    }

    // ---- Undo and redo --------------------------------------------------------------
    // Every edit saves the expression and cursor first; moving the cursor doesn't.

    private class Snapshot(val code: String, val path: List<Int>, val index: Int)

    private val undoStack = ArrayDeque<Snapshot>()
    private val redoStack = ArrayDeque<Snapshot>()

    val canUndo get() = undoStack.isNotEmpty()
    val canRedo get() = redoStack.isNotEmpty()

    /** The cursor's row as (item index, slot index) pairs from the root down. */
    private fun pathOf(r: MathRow): List<Int> {
        val out = ArrayList<Int>()
        var cur = r
        while (true) {
            val node = cur.parent ?: break
            val outer = node.parent ?: break
            out.add(0, node.slots.indexOf(cur))
            out.add(0, outer.items.indexOf(node))
            cur = outer
        }
        return out
    }

    private fun snapshot() = Snapshot(MathCodec.encode(root), pathOf(row), index)

    private fun restore(s: Snapshot) {
        root = MathCodec.decode(s.code)
        var r = root
        for (k in s.path.indices step 2) {
            val node = r.items.getOrNull(s.path[k]) ?: break
            r = node.slots.getOrNull(s.path[k + 1]) ?: break
        }
        row = r
        index = s.index.coerceIn(0, r.items.size)
    }

    private fun record() {
        undoStack.addLast(snapshot())
        if (undoStack.size > 200) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo() {
        val s = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(snapshot())
        restore(s)
        changed()
    }

    fun redo() {
        val s = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(snapshot())
        restore(s)
        changed()
    }

    /** Moves the cursor into nested slots: (item index, slot index) pairs from the current row. */
    fun enter(path: List<Int>) {
        var r = row
        // Only whole (item, slot) pairs; a stray last number is ignored.
        for (k in 0 until path.size - 1 step 2) {
            val node = r.items.getOrNull(path[k]) ?: break
            r = node.slots.getOrNull(path[k + 1]) ?: break
        }
        row = r
        index = 0
        changed()
    }

    fun setCursor(row: MathRow, index: Int) {
        this.row = row
        this.index = index.coerceIn(0, row.items.size)
        changed()
    }

    fun clear() {
        record()
        root = MathRow()
        row = root
        index = 0
        changed()
    }

    fun load(content: MathRow) {
        record()
        root = content
        row = root
        index = root.items.size
        changed()
    }

    // ---- Insertion ------------------------------------------------------

    fun insert(node: Node, enterSlot: Int? = null) {
        // u·v, u×v, ∘ and ⊗ right after a matrix (or a letter, or a bracketed group): that is the
        // first operand, and the cursor goes on to the second box, rather than giving □·□.
        if (node is Func && node.name in PRODUCTS && enterSlot == 0) {
            val take = operandBefore()
            if (take > 0) {
                record()
                val start = index - take
                repeat(take) { node.args[0].add(row.removeAt(start)) }
                row.add(start, node)
                row = node.args[1]
                index = 0
                changed()
                return
            }
        }
        record()
        row.add(index, node)
        index++
        if (enterSlot != null) {
            row = node.slots[enterSlot]
            index = row.items.size
        }
        changed()
    }

    /**
     * How many items just before the cursor make one operand for a product key: a matrix (with
     * any ⁻¹, ᵀ after it), a bracketed group, or a single letter. 0 if there's none.
     */
    private fun operandBefore(): Int {
        var k = index
        while (k > 0 && row.items[k - 1] is Pow) k--
        val prev = row.items.getOrNull(k - 1) ?: return 0
        val start = when {
            prev is Matrix -> k - 1
            prev is Sym && prev.text == ")" -> {
                var depth = 0
                var j = k - 1
                while (j >= 0) {
                    when ((row.items[j] as? Sym)?.text) { ")" -> depth++; "(" -> { depth--; if (depth == 0) break } }
                    j--
                }
                if (j < 0) return 0 else j
            }
            prev is Sym && prev.text.length == 1 && prev.text[0].isLetter() && k == index -> k - 1
            else -> return 0
        }
        return index - start
    }

    /** Pairs that combine as they're typed: <= and =< give ≤, >= and => give ≥. */
    private val combines = mapOf("<=" to "≤", "=<" to "≤", ">=" to "≥", "=>" to "≥")

    fun type(text: String) {
        val before = (row.items.getOrNull(index - 1) as? Sym)?.text
        val combined = combines[(before ?: "") + text]
        if (combined != null) {
            record()
            row.items[index - 1] = Sym(combined).also { it.parent = row }
            changed()
            return
        }
        insert(Sym(text))
    }

    /** Pastes a copy of [content] at the cursor. */
    fun insertRow(content: MathRow) {
        record()
        MathCodec.copy(content).items.toList().forEach { n ->
            n.parent?.let { it.items.remove(n) }
            row.add(index++, n)
        }
        changed()
    }

    /**
     * ÷ builds a fraction. Whatever operand sits just before the cursor
     * (a number, a bracket, a power…) moves into the numerator and the
     * cursor goes to the denominator; with nothing before it, the cursor
     * goes to an empty numerator.
     */
    fun insertFraction() {
        record()
        val start = operandStart(row, index)
        val frac = Frac()
        val taken = (start until index).map { row.items[it] }
        repeat(taken.size) { row.removeAt(start) }
        taken.forEach { frac.num.add(it) }
        row.add(start, frac)
        index = start + 1
        row = if (taken.isEmpty()) frac.num else frac.den
        index = 0
        changed()
    }

    /** Adds a superscript after the previous item. With [exponent] it's filled in and the cursor stays put. */
    fun insertPower(exponent: MathRow? = null) {
        // ⁻¹, ᵀ and ᴴ pressed inside a matrix apply to the whole matrix: step out of it first.
        if (exponent?.plainText() in setOf("−1", "T", "H")) {
            var node: Node? = row.parent
            var outer: Matrix? = null
            while (node != null) {
                if (node is Matrix) outer = node
                node = node.parent?.parent
            }
            outer?.let { m ->
                val holder = m.parent ?: return@let
                row = holder
                index = holder.items.indexOf(m) + 1
            }
        }
        val prev = row.items.getOrNull(index - 1)
        // Nothing to raise (start of a box, or after an operator or an opening bracket): give the
        // base its own brackets, with the cursor inside them, then the exponent box.
        val nothingBefore = prev == null ||
            (prev is Sym && prev.text in setOf("+", "−", "×", "÷", "(", ",", "=", "<", ">", "≤", "≥", "mod", ":="))
        if (nothingBefore) {
            record()
            row.add(index, Sym("("))
            row.add(index + 1, Sym(")"))
            row.add(index + 2, Pow(exponent ?: MathRow()))
            index += 1
            changed()
            return
        }
        val pow = Pow(exponent ?: MathRow())
        insert(pow, enterSlot = if (exponent == null) 0 else null)
    }

    /** "( )" key: opens a bracket, or closes one when that makes sense (like Google Calculator). */
    fun smartParen() {
        val open = row.items.take(index).count { (it as? Sym)?.text == "(" } -
            row.items.take(index).count { (it as? Sym)?.text == ")" }
        val prev = row.items.getOrNull(index - 1)
        val prevEndsOperand = prev != null && endsOperand(prev)
        type(if (open > 0 && prevEndsOperand) ")" else "(")
    }

    // ---- Cursor movement --------------------------------------------------

    fun moveRight() {
        if (index < row.items.size) {
            val next = row.items[index]
            if (next.slots.isNotEmpty()) { row = next.slots.first(); index = 0 } else index++
        } else {
            val parent = row.parent ?: return
            val slotIndex = parent.slots.indexOf(row)
            if (slotIndex < parent.slots.lastIndex) {
                row = parent.slots[slotIndex + 1]; index = 0
            } else {
                val outer = parent.parent!!
                row = outer; index = outer.items.indexOf(parent) + 1
            }
        }
        changed()
    }

    fun moveLeft() {
        if (index > 0) {
            val prev = row.items[index - 1]
            if (prev.slots.isNotEmpty()) { row = prev.slots.last(); index = row.items.size } else index--
        } else {
            val parent = row.parent ?: return
            val slotIndex = parent.slots.indexOf(row)
            if (slotIndex > 0) {
                row = parent.slots[slotIndex - 1]; index = row.items.size
            } else {
                val outer = parent.parent!!
                row = outer; index = outer.items.indexOf(parent)
            }
        }
        changed()
    }

    // ---- Deleting -------------------------------------------------------

    /**
     * Parts of a template that shouldn't be deleted: the → in a limit's x → a, and the
     * |z − a| = r circle (its |…| and =) under a contour integral. Backspace steps over them.
     */
    /** The slot a construction is "about": an integral's body, a root's radicand, a fraction's top. */
    private fun isMainSlot(owner: Node, slot: MathRow): Boolean = when (owner) {
        is Integral -> owner.body === slot
        is Sqrt -> owner.arg === slot
        is Root -> owner.arg === slot
        is Frac -> owner.num === slot
        is BigOp -> owner.body === slot
        is Derivative -> owner.body === slot
        is Func -> owner.args.firstOrNull() === slot
        is Pow -> owner.exp === slot
        is Binom -> owner.n === slot
        else -> false
    }

    private fun isProtected(n: Node): Boolean {
        val owner = row.parent as? Func ?: return false
        val slot = owner.slots.indexOf(row)
        return when {
            owner.name in setOf("lim", "taylor") && slot == 1 -> n is Sym && n.text == "→"
            owner.name == "contour" && slot == 1 -> (n is Sym && n.text == "=") || (n is Func && n.name == "abs")
            owner.name == "residue" && slot == 1 -> n is Sym && n.text == "="
            else -> false
        }
    }

    fun backspace() {
        record()
        // Inside an empty main field of a construction (the body of an integral, under a root…):
        // one press removes the whole construction.
        val owner = row.parent
        // (An integral's variable box holds x by default, so the other slots being empty is
        // judged without it.)
        // Any construction goes once its main field is empty, even with its limits filled in,
        // except a fraction, which only goes when its bottom is empty too (it'd be lost otherwise).
        val removable = owner != null && row.isEmpty && isMainSlot(owner, row) &&
            (owner !is Frac || owner.den.isEmpty)
        if (owner != null && removable) {
            val parentRow = owner.parent ?: return
            val at = parentRow.items.indexOf(owner)
            if (at >= 0) {
                parentRow.items.removeAt(at)
                this.row = parentRow
                index = at
                changed()
                return
            }
        }
        if (index > 0 && isProtected(row.items[index - 1])) {
            // Step back past it instead: the template needs it.
            val prev = row.items[index - 1]
            if (prev is Func && prev.slots.isNotEmpty()) { row = prev.slots.last(); index = row.items.size } else index--
            changed()
            return
        }
        if (index > 0) {
            val prev = row.items[index - 1]
            if (prev.slots.isEmpty() || prev.allSlotsEmpty) {
                row.removeAt(index - 1); index--
            } else {
                // Step into the structure instead of deleting its contents at once.
                row = prev.slots.last(); index = row.items.size
            }
            changed()
            return
        }
        val parent = row.parent ?: return
        val outer = parent.parent!!
        val at = outer.items.indexOf(parent)
        val slotIndex = parent.slots.indexOf(row)
        when {
            parent.allSlotsEmpty -> { outer.removeAt(at); row = outer; index = at }
            parent.slots.size == 1 -> {
                // Unwrap √(…), sin(…), superscripts: keep the contents.
                outer.removeAt(at)
                val kept = row.items.toList()
                kept.forEachIndexed { k, n -> row.items.remove(n); outer.add(at + k, n) }
                row = outer; index = at
            }
            slotIndex > 0 -> { row = parent.slots[slotIndex - 1]; index = row.items.size }
            else -> { row = outer; index = at }
        }
        changed()
    }

    companion object {
        /** Products whose key takes what's before the cursor as the first operand. */
        private val PRODUCTS = setOf("dot", "cross", "hadamard", "kron")

        private val OPERATORS = setOf("+", "−", "×", "÷", "mod", ",", "(", "=", ":=", "→", "<", ">", "≤", "≥")

        fun endsOperand(n: Node): Boolean = when (n) {
            is Sym -> n.text !in OPERATORS
            else -> true
        }

        /** Index where the operand ending at [end] starts, e.g. the "12" in "3+12". */
        fun operandStart(row: MathRow, end: Int): Int {
            var i = end
            while (i > 0) {
                val n = row.items[i - 1]
                val t = (n as? Sym)?.text
                if (t == ")") {
                    var depth = 0
                    var j = i - 1
                    while (j >= 0) {
                        val tj = (row.items[j] as? Sym)?.text
                        if (tj == ")") depth++
                        if (tj == "(") { depth--; if (depth == 0) break }
                        j--
                    }
                    i = maxOf(j, 0)
                    continue
                }
                if (t != null && t in OPERATORS) break
                i--
            }
            return i
        }
    }
}
