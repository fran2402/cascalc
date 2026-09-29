package com.example.cas.editor

/**
 * Lists inside a graph line, as in Desmos: y = [1, 2, 3]x is three lines, y = x, y = 2x and
 * y = 3x. The first list in the line (one that isn't the whole line, which is a list of points)
 * is replaced by each of its entries in turn, bracketed, giving one row per entry.
 *
 * A list can also be a range, [1...10] (steps of 1) or [1, 3, ...11] (steps of the first two),
 * or a comprehension, [n² for n = [1...5]], which is the expression once for each value, with
 * the letter replaced by it. "..." is three points (or …); "for" is typed as its letters.
 */
object ListFamily {
    /** At most this many lines (or points) from one list. */
    const val MAX = 100

    private fun t(n: Node?) = (n as? Sym)?.text

    fun expand(row: MathRow): List<MathRow>? {
        // Worked on a copy: putting nodes in new rows re-parents them, which mustn't touch the line being edited.
        val items = MathCodec.copy(row).items
        val (open, close) = firstList(items) ?: return null
        // The whole line in brackets is a list of points, not a family.
        if (open == 0 && close == items.lastIndex) return null
        val entries = entries(items.subList(open + 1, close)) ?: return null
        return entries.map { entry ->
            MathCodec.copy(MathRow((items.subList(0, open) + Sym("(") + entry + Sym(")") + items.subList(close + 1, items.size)).toMutableList()))
        }
    }

    /**
     * A line that is a whole list written as a range or a comprehension, rewritten with its
     * entries listed ([(n, n²) for n = [1...3]] becomes [(1, 1²), (2, 2²), (3, 3²)]), so it can
     * be read as a list of points. Null when the line isn't one.
     */
    fun spelledOut(row: MathRow): MathRow? {
        val items = MathCodec.copy(row).items
        if (t(items.firstOrNull()) != "[" || t(items.lastOrNull()) != "]") return null
        val (open, close) = firstList(items) ?: return null
        if (open != 0 || close != items.lastIndex) return null
        val inner = items.subList(1, items.size - 1)
        if (forAt(inner) < 0 && dotsAt(inner) < 0) return null
        val entries = entries(inner) ?: return null
        val out = MathRow()
        out.add(Sym("["))
        entries.forEachIndexed { k, e ->
            if (k > 0) out.add(Sym(","))
            e.forEach { out.add(it) }
        }
        out.add(Sym("]"))
        return MathCodec.copy(out)
    }

    /** The first [ … ] in [items], as the indices of its brackets. */
    private fun firstList(items: List<Node>): Pair<Int, Int>? {
        val open = items.indexOfFirst { t(it) == "[" }
        if (open < 0) return null
        var depth = 0
        for (k in open until items.size) {
            when (t(items[k])) { "[" -> depth++; "]" -> { depth--; if (depth == 0) return open to k } }
        }
        return null
    }

    /** The entries of a list's inside: listed, a range, or a comprehension. Null if it's none of these. */
    private fun entries(inner: List<Node>): List<List<Node>>? {
        val f = forAt(inner)
        if (f >= 0) return comprehension(inner.subList(0, f), inner.subList(f + 3, inner.size))
        val parts = split(inner)
        if (parts.any { it.isEmpty() }) return null
        val range = range(parts)
        // "..." that isn't a range we can count out (too long, or not numbers) is refused.
        if (range == null && dotsAt(parts.last()) >= 0) return null
        val out = range?.map { numberNodes(it) } ?: parts
        return out.takeIf { it.isNotEmpty() && it.size <= MAX }
    }

    /** Splits at commas outside brackets. */
    private fun split(items: List<Node>): List<List<Node>> {
        val out = ArrayList<List<Node>>()
        var start = 0
        var level = 0
        for (k in items.indices) {
            when (t(items[k])) {
                "(", "[" -> level++
                ")", "]" -> level--
                "," -> if (level == 0) { out += items.subList(start, k); start = k + 1 }
            }
        }
        out += items.subList(start, items.size)
        return out
    }

    /** Where "..." (or …) starts in [items], outside brackets; −1 if nowhere. */
    private fun dotsAt(items: List<Node>): Int {
        var level = 0
        for (k in items.indices) {
            when (t(items[k])) {
                "(", "[" -> level++
                ")", "]" -> level--
                "…" -> if (level == 0) return k
                "." -> if (level == 0 && t(items.getOrNull(k + 1)) == "." && t(items.getOrNull(k + 2)) == ".") return k
            }
        }
        return -1
    }

    private fun dotsLength(items: List<Node>, at: Int) = if (t(items[at]) == "…") 1 else 3

    /** Where the letters f, o, r (the word "for") start, outside brackets; −1 if nowhere. */
    private fun forAt(items: List<Node>): Int {
        var level = 0
        for (k in items.indices) {
            when (t(items[k])) {
                "(", "[" -> level++
                ")", "]" -> level--
                "f" -> if (level == 0 && t(items.getOrNull(k + 1)) == "o" && t(items.getOrNull(k + 2)) == "r") return k
            }
        }
        return -1
    }

    /** A plain number written in [items] (digits, a point, a leading minus), or null. */
    private fun number(items: List<Node>): Double? {
        if (items.isEmpty() || items.any { it !is Sym }) return null
        return items.joinToString("") { (it as Sym).text }.replace("−", "-").toDoubleOrNull()
    }

    /**
     * [a...b] steps by 1; [a, b, ...c] steps by b − a. Both ends are included (up to rounding).
     * Null when [parts] isn't a range.
     */
    private fun range(parts: List<List<Node>>): List<Double>? {
        val last = parts.last()
        val d = dotsAt(last)
        if (d < 0) return null
        val end = number(last.subList(d + dotsLength(last, d), last.size)) ?: return null
        val before = last.subList(0, d)
        val starts = parts.dropLast(1).map { number(it) ?: return null } + (if (before.isEmpty()) emptyList() else listOf(number(before) ?: return null))
        if (starts.isEmpty()) return null
        val a = starts[0]
        val step = if (starts.size >= 2) starts[1] - starts[0] else if (end >= a) 1.0 else -1.0
        if (step == 0.0 || (end - a) / step < 0) return null
        val n = Math.floor((end - a) / step + 1e-9).toLong() + 1
        if (n > MAX) return null
        return (0 until n).map { k -> a + k * step }
    }

    /** [expr for v = [list]]: the expression once for each value, with v replaced by it. */
    private fun comprehension(expr: List<Node>, rest: List<Node>): List<List<Node>>? {
        // v = [ … ]
        val v = t(rest.firstOrNull()) ?: return null
        if (v.length != 1 || !v[0].isLetter() || t(rest.getOrNull(1)) != "=") return null
        val list = rest.drop(2)
        if (t(list.firstOrNull()) != "[" || t(list.lastOrNull()) != "]") return null
        val values = entries(list.subList(1, list.size - 1)) ?: return null
        if (expr.isEmpty()) return null
        return values.map { value -> substitute(expr, v, value) }
    }

    /** [expr] with every letter [v] (at any depth) replaced by [value], bracketed. */
    private fun substitute(expr: List<Node>, v: String, value: List<Node>): List<Node> {
        val row = MathCodec.copy(MathRow(expr.toMutableList()))
        fun walk(r: MathRow) {
            var k = 0
            while (k < r.items.size) {
                val n = r.items[k]
                if (n is Sym && n.text == v) {
                    r.removeAt(k)
                    val piece = listOf(Sym("(")) + MathCodec.copy(MathRow(value.toMutableList())).items.toList() + Sym(")")
                    piece.forEachIndexed { i, p -> r.add(k + i, p) }
                    k += piece.size
                } else {
                    n.slots.forEach { walk(it) }
                    k++
                }
            }
        }
        walk(row)
        return row.items.toList()
    }

    /** A number as editor symbols (−2.5 as "−", "2", ".", "5"). */
    private fun numberNodes(v: Double): List<Node> {
        val text = if (v == Math.rint(v) && kotlin.math.abs(v) < 1e15) v.toLong().toString() else java.math.BigDecimal(v).round(java.math.MathContext(12)).stripTrailingZeros().toPlainString()
        return text.replace("-", "−").map { Sym(it.toString()) }
    }
}
