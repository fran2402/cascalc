package com.example.cas.editor

/**
 * Lists inside a graph line, as in Desmos: y = [1, 2, 3]x is three lines, y = x, y = 2x and
 * y = 3x. The first list in the line (one that isn't the whole line, which is a list of points)
 * is replaced by each of its entries in turn, bracketed, giving one row per entry.
 */
object ListFamily {
    /** At most this many lines from one list. */
    const val MAX = 50

    fun expand(row: MathRow): List<MathRow>? {
        val items = row.items
        fun t(n: Node?) = (n as? Sym)?.text
        val open = items.indexOfFirst { t(it) == "[" }
        if (open < 0) return null
        var depth = 0
        var close = -1
        for (k in open until items.size) {
            when (t(items[k])) { "[" -> depth++; "]" -> { depth--; if (depth == 0) { close = k; break } } }
        }
        if (close < 0) return null
        // The whole line in brackets is a list of points, not a family.
        if (open == 0 && close == items.lastIndex) return null
        val entries = ArrayList<List<Node>>()
        var start = open + 1
        var level = 0
        for (k in open + 1 until close) {
            when (t(items[k])) {
                "(", "[" -> level++
                ")", "]" -> level--
                "," -> if (level == 0) { entries += items.subList(start, k); start = k + 1 }
            }
        }
        entries += items.subList(start, close)
        if (entries.any { it.isEmpty() } || entries.size > MAX) return null
        return entries.map { entry ->
            val copy = MathCodec.copy(MathRow((items.subList(0, open) + Sym("(") + entry + Sym(")") + items.subList(close + 1, items.size)).toMutableList()))
            copy
        }
    }
}
