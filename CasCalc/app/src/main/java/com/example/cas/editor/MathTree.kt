package com.example.cas.editor

/**
 * The expression being edited is a tree: a [MathRow] is a horizontal list of
 * [Node]s, and structured nodes (fractions, powers, roots, integrals…) own
 * child rows ("slots") that the cursor can move into. This is what lets the
 * editor show real 2D math with a box for every empty slot.
 */
class MathRow(val items: MutableList<Node> = mutableListOf()) {
    var parent: Node? = null
        internal set

    init { items.forEach { it.parent = this } }

    val isEmpty get() = items.isEmpty()

    fun add(index: Int, node: Node) { node.parent = this; items.add(index, node) }
    fun add(node: Node) = add(items.size, node)
    fun removeAt(index: Int): Node = items.removeAt(index).also { it.parent = null }

    /** Plain text of simple rows, used for variable names and debugging. */
    fun plainText(): String = items.joinToString("") { (it as? Sym)?.text ?: "□" }

    override fun toString() = MathCodec.encode(this)
}

sealed class Node {
    var parent: MathRow? = null
        internal set

    /** Editable child rows, in cursor order. */
    abstract val slots: List<MathRow>

    internal fun adopt(vararg rows: MathRow) = rows.forEach { it.parent = this }

    val allSlotsEmpty get() = slots.all { it.isEmpty }
}

/** A single token: digit, point, operator, bracket, variable, i, π, e, "mod", "ans"… */
class Sym(val text: String) : Node() {
    override val slots = emptyList<MathRow>()
}

/** A named physical constant, e.g. c0 (speed of light). See [Constants]. */
class Const(val id: String) : Node() {
    override val slots = emptyList<MathRow>()
}

class Frac(val num: MathRow = MathRow(), val den: MathRow = MathRow()) : Node() {
    init { adopt(num, den) }
    override val slots = listOf(num, den)
}

/** Superscript applied to whatever comes before it. An exponent of "T" means transpose. */
class Pow(val exp: MathRow = MathRow()) : Node() {
    init { adopt(exp) }
    override val slots = listOf(exp)
}

class Sqrt(val arg: MathRow = MathRow()) : Node() {
    init { adopt(arg) }
    override val slots = listOf(arg)
}

class Root(val index: MathRow = MathRow(), val arg: MathRow = MathRow()) : Node() {
    init { adopt(index, arg) }
    override val slots = listOf(index, arg)
}

/**
 * A function with bracketed arguments: sin, cos, ln, gcd… `log` has two
 * slots (base, argument); `abs`, `floor` and `ceil` draw as delimiters.
 */
class Func(val name: String, val args: List<MathRow>) : Node() {
    constructor(name: String, arity: Int = 1) : this(name, List(arity) { MathRow() })
    init { adopt(*args.toTypedArray()) }
    override val slots = args
}

enum class BigOpKind { Sum, Product }

/** Σ or Π: variable = lower … upper, then the body. */
class BigOp(
    val kind: BigOpKind,
    val variable: MathRow = MathRow(),
    val lower: MathRow = MathRow(),
    val upper: MathRow = MathRow(),
    val body: MathRow = MathRow(),
) : Node() {
    init { adopt(variable, lower, upper, body) }
    override val slots = listOf(variable, lower, upper, body)
}

/** ∫ from lower to upper of body d(variable). */
class Integral(
    val lower: MathRow = MathRow(),
    val upper: MathRow = MathRow(),
    val body: MathRow = MathRow(),
    val variable: MathRow = MathRow(),
) : Node() {
    init { adopt(lower, upper, body, variable) }
    override val slots = listOf(lower, upper, body, variable)
}

/**
 * dⁿ/d(variable)ⁿ of body (n from [order], empty means 1), evaluated at
 * variable = at. With [partial] it's written ∂ⁿ/∂xⁿ (the other letters are
 * held constant, which is how every derivative here is taken anyway).
 */
class Derivative(
    val variable: MathRow = MathRow(),
    val body: MathRow = MathRow(),
    val at: MathRow = MathRow(),
    val order: MathRow = MathRow(),
    val partial: Boolean = false,
) : Node() {
    init { adopt(variable, body, at, order) }
    override val slots = listOf(variable, body, at, order)
}

/** (n choose k). */
/**
 * A base with a subscript and/or superscript, x_i or a_{ij}^2. Only used to
 * show formulas (the key help, written in LaTeX); the calculator's own powers
 * are [Pow] nodes.
 */
class Scripted(val base: MathRow = MathRow(), val sub: MathRow = MathRow(), val sup: MathRow = MathRow()) : Node() {
    init { adopt(base, sub, sup) }
    override val slots = listOf(base, sub, sup)
}

class Binom(val n: MathRow = MathRow(), val k: MathRow = MathRow()) : Node() {
    init { adopt(n, k) }
    override val slots = listOf(n, k)
}

/**
 * A matrix. A [growable] one (from the matrix key) keeps one empty row and one empty column
 * after the ones in use: typing into the last row or column adds another ([grow]), and the
 * trailing empty ones aren't part of the matrix ([usedRows] × [usedCols]). Fixed ones (the
 * 2- and 3-vectors) keep their size.
 */
class Matrix(rows: Int, cols: Int, cells: List<MathRow> = List(rows * cols) { MathRow() }, val growable: Boolean = false) : Node() {
    var rows = rows
        private set
    var cols = cols
        private set
    val cells: MutableList<MathRow> = cells.toMutableList()
    init {
        require(cells.size == rows * cols)
        adopt(*cells.toTypedArray())
    }
    override val slots: List<MathRow> get() = cells
    fun cell(r: Int, c: Int) = cells[r * cols + c]

    /** Rows and columns in use: trailing empty ones don't count (at least 1 × 1). */
    val usedRows: Int get() = if (!growable) rows else ((0 until rows).lastOrNull { r -> (0 until cols).any { c -> !cell(r, c).isEmpty } } ?: 0) + 1
    val usedCols: Int get() = if (!growable) cols else ((0 until cols).lastOrNull { c -> (0 until rows).any { r -> !cell(r, c).isEmpty } } ?: 0) + 1

    /**
     * Keeps exactly one spare row and column: adds an empty row and/or column when the last one
     * has something in it, and removes the last one when it and the one before are both empty
     * (never the one holding the cursor, [cursorRow]). Returns whether the size changed.
     */
    fun grow(cursorRow: MathRow? = null): Boolean {
        if (!growable) return false
        var changed = false
        fun rowEmpty(r: Int) = (0 until cols).all { c -> cell(r, c).isEmpty }
        fun colEmpty(c: Int) = (0 until rows).all { r -> cell(r, c).isEmpty }
        fun rowHasCursor(r: Int) = (0 until cols).any { c -> cell(r, c) === cursorRow }
        fun colHasCursor(c: Int) = (0 until rows).any { r -> cell(r, c) === cursorRow }
        while (rows > 2 && rowEmpty(rows - 1) && rowEmpty(rows - 2) && !rowHasCursor(rows - 1)) {
            repeat(cols) { cells.removeAt(cells.lastIndex) }
            rows--
            changed = true
        }
        while (cols > 2 && colEmpty(cols - 1) && colEmpty(cols - 2) && !colHasCursor(cols - 1)) {
            for (r in rows - 1 downTo 0) cells.removeAt(r * cols + cols - 1)
            cols--
            changed = true
        }
        if ((0 until cols).any { c -> !cell(rows - 1, c).isEmpty }) {
            repeat(cols) { val m = MathRow(); adopt(m); cells.add(m) }
            rows++
            changed = true
        }
        if ((0 until rows).any { r -> !cell(r, cols - 1).isEmpty }) {
            for (r in rows - 1 downTo 0) { val m = MathRow(); adopt(m); cells.add((r + 1) * cols, m) }
            cols++
            changed = true
        }
        return changed
    }

    companion object {
        /** A copy with only the rows and columns in use, and not growable (for answers and history). */
        fun trimmed(m: Matrix): Matrix = Matrix(m.usedRows, m.usedCols, (0 until m.usedRows).flatMap { r -> (0 until m.usedCols).map { c -> MathCodec.copy(m.cell(r, c)) } })
    }
}

fun row(vararg nodes: Node) = MathRow(nodes.toMutableList())
fun row(text: String) = MathRow(text.map { Sym(it.toString()) }.toMutableList())

/**
 * Compact text form of a tree, used to save history and to copy rows.
 *   row  := node*
 *   node := "'" text ";"            symbol
 *         | "$" id ";"               constant
 *         | kind "{" row ("|" row)* "}"
 */
object MathCodec {
    fun encode(row: MathRow): String = buildString { encodeRow(row) }

    private fun StringBuilder.encodeRow(row: MathRow) = row.items.forEach { encodeNode(it) }

    private fun StringBuilder.encodeNode(n: Node) {
        when (n) {
            // A backslash escapes ; and itself, so symbols like ";" survive (φ(x; μ, σ)).
            is Sym -> append('\'').append(n.text.replace("\\", "\\\\").replace(";", "\\;")).append(';')
            is Const -> append('$').append(n.id).append(';')
            else -> {
                append(
                    when (n) {
                        is Frac -> "frac"
                        is Pow -> "pow"
                        is Sqrt -> "sqrt"
                        is Root -> "root"
                        is Func -> "fn:" + n.name
                        is BigOp -> if (n.kind == BigOpKind.Sum) "sum" else "prod"
                        is Integral -> "int"
                        is Derivative -> if (n.partial) "pdiff" else "diff"
                        is Binom -> "binom"
                        is Matrix -> (if (n.growable) "gmat:" else "mat:") + "${n.rows}x${n.cols}"
                        is Scripted -> "scr"
                        else -> error("unknown node")
                    },
                )
                append('{')
                n.slots.forEachIndexed { i, s -> if (i > 0) append('|'); encodeRow(s) }
                append('}')
            }
        }
    }

    fun decode(text: String): MathRow {
        var i = 0
        fun readUntil(stop: Char): String {
            val start = i
            while (text[i] != stop) i++
            return text.substring(start, i).also { i++ }
        }
        /** A symbol's text up to its unescaped ';' (\; and \\ are a literal ; and \). */
        fun readSymbol(): String {
            val sb = StringBuilder()
            while (text[i] != ';') {
                if (text[i] == '\\' && i + 1 < text.length) i++
                sb.append(text[i++])
            }
            i++
            return sb.toString()
        }
        fun parseRow(): MathRow {
            val r = MathRow()
            while (i < text.length && text[i] != '|' && text[i] != '}') {
                when (text[i]) {
                    '\'' -> { i++; r.add(Sym(readSymbol())) }
                    '$' -> { i++; r.add(Const(readUntil(';'))) }
                    else -> {
                        val kind = readUntil('{')
                        val slots = mutableListOf<MathRow>()
                        while (true) {
                            slots += parseRow()
                            val c = text[i++]
                            if (c == '}') break
                        }
                        r.add(
                            when {
                                kind == "frac" -> Frac(slots[0], slots[1])
                                kind == "pow" -> Pow(slots[0])
                                kind == "sqrt" -> Sqrt(slots[0])
                                kind == "root" -> Root(slots[0], slots[1])
                                kind.startsWith("fn:") -> Func(kind.removePrefix("fn:"), slots)
                                kind == "sum" -> BigOp(BigOpKind.Sum, slots[0], slots[1], slots[2], slots[3])
                                kind == "prod" -> BigOp(BigOpKind.Product, slots[0], slots[1], slots[2], slots[3])
                                kind == "int" -> Integral(slots[0], slots[1], slots[2], slots[3])
                                kind == "diff" -> Derivative(slots[0], slots[1], slots[2], slots.getOrElse(3) { MathRow() })
                                kind == "pdiff" -> Derivative(slots[0], slots[1], slots[2], slots.getOrElse(3) { MathRow() }, partial = true)
                                kind == "binom" -> Binom(slots[0], slots[1])
                                kind == "scr" -> Scripted(slots[0], slots[1], slots[2])
                                kind.startsWith("mat:") || kind.startsWith("gmat:") -> {
                                    val (rr, cc) = kind.substringAfter(":").split("x").map { it.toInt() }
                                    Matrix(rr, cc, slots, growable = kind.startsWith("gmat:"))
                                }
                                else -> throw IllegalArgumentException("Unknown node $kind")
                            },
                        )
                    }
                }
            }
            return r
        }
        return parseRow()
    }

    fun copy(row: MathRow): MathRow = decode(encode(row))
}

/** Grows (or trims) every growable matrix in [row] to keep one spare row and column. */
fun growMatrices(row: MathRow, cursorRow: MathRow? = null): Boolean {
    var changed = false
    for (n in row.items) {
        if (n is Matrix && n.grow(cursorRow)) changed = true
        n.slots.forEach { if (growMatrices(it, cursorRow)) changed = true }
    }
    return changed
}

/**
 * A copy of [row] with every matrix growable again, with its spare row and column (for an
 * expression or answer taken from the history, so it can be extended as when first typed).
 */
fun makeMatricesGrowable(row: MathRow): MathRow {
    val copy = MathCodec.copy(row)
    fun walk(r: MathRow) {
        for (k in r.items.indices) {
            val n = r.items[k]
            n.slots.forEach { walk(it) }
            if (n is Matrix && !n.growable) {
                val cells = (0..n.rows).flatMap { i -> (0..n.cols).map { j -> if (i < n.rows && j < n.cols) MathCodec.copy(n.cell(i, j)) else MathRow() } }
                r.items[k] = Matrix(n.rows + 1, n.cols + 1, cells, growable = true).also { it.parent = r }
            }
        }
    }
    walk(copy)
    return copy
}

/** A copy of [row] with growable matrices cut down to the rows and columns in use. */
fun trimMatrices(row: MathRow): MathRow {
    val copy = MathCodec.copy(row)
    fun walk(r: MathRow) {
        for (k in r.items.indices) {
            val n = r.items[k]
            n.slots.forEach { walk(it) }
            if (n is Matrix && n.growable) r.items[k] = Matrix.trimmed(n).also { it.parent = r }
        }
    }
    walk(copy)
    return copy
}
