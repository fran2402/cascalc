package com.example.cas.graph

/**
 * Folders in a graph's list of lines. The list is flat, and every item (line, note or folder)
 * has a depth: 0 at the top, 1 inside a folder at depth 0, and so on. A folder holds the items
 * right after it that are deeper than it, so a line at depth 0 after a folder is outside it:
 * adding a line never drops it into a folder, and moving a folder takes only its own items.
 *
 * Moves are by visible steps: a closed folder is passed over (or carried) as one block, and a
 * line moved past an open folder's heading goes into it (or out of it, going the other way).
 */
object FolderTree {
    /** One item: its depth and whether it's a folder (open or [collapsed]). */
    data class Item(val depth: Int, val folder: Boolean, val collapsed: Boolean = false)

    /** A rearrangement: the new order (as old indices) and each item's depth in that order. */
    data class Change(val order: List<Int>, val depths: List<Int>)

    /**
     * Depths made consistent: an item is at most one deeper than the folder it's in, and a
     * folder's scope ends at the first item not deeper than it.
     */
    fun normalize(items: List<Item>): List<Int> {
        val open = ArrayList<Int>() // depths of the folders the next item can be in
        return items.map { item ->
            val d = item.depth.coerceIn(0, (open.lastOrNull() ?: -1) + 1)
            while (open.isNotEmpty() && open.last() >= d) open.removeAt(open.lastIndex)
            if (item.folder) open += d
            d
        }
    }

    /** The folders item [index] is in, outermost first, as indices. */
    fun enclosing(items: List<Item>, index: Int): List<Int> {
        val stack = ArrayList<Int>()
        for (k in 0..index.coerceAtMost(items.lastIndex)) {
            val d = items[k].depth
            while (stack.isNotEmpty() && items[stack.last()].depth >= d) stack.removeAt(stack.lastIndex)
            if (k < index && items[k].folder) stack += k
        }
        return stack
    }

    /** Everything folder [index] holds: the items after it that are deeper than it. */
    fun members(items: List<Item>, index: Int): List<Int> {
        val item = items.getOrNull(index) ?: return emptyList()
        if (!item.folder) return emptyList()
        return (index + 1 until items.size).takeWhile { items[it].depth > item.depth }
    }

    /** Whether a closed folder hides item [index]. */
    fun hidden(items: List<Item>, index: Int): Boolean = enclosing(items, index).any { items[it].collapsed }

    /** The item and, for a folder, everything in it: what moves together. */
    private fun unit(items: List<Item>, index: Int): IntRange = index..(members(items, index).lastOrNull() ?: index)

    /**
     * Moves item [index] (with its contents, if a folder) one visible step up ([dir] = −1) or
     * down (+1). Returns null when it's already at that end.
     */
    fun step(items: List<Item>, index: Int, dir: Int): Change? {
        val own = unit(items, index)
        val moving = items[index]
        val (span, depth) = if (dir > 0) {
            val n = own.last + 1
            if (n >= items.size) return null
            val next = items[n]
            when {
                // A line goes into an open folder, as its first line.
                next.folder && !next.collapsed && !moving.folder -> (n..n) to next.depth + 1
                // Past a closed folder (or any folder, when moving a folder): the whole block.
                next.folder -> unit(items, n) to next.depth
                else -> (n..n) to next.depth
            }
        } else {
            if (index == 0) return null
            // The nearest visible item above; anything hidden in between belongs to it.
            var p = index - 1
            while (p > 0 && hidden(items, p)) p--
            (p until index) to items[p].depth
        }
        val order = ArrayList<Int>(items.size)
        val depths = ArrayList<Int>(items.size)
        val shift = depth - moving.depth
        fun put(range: IntRange, by: Int) = range.forEach { order += it; depths += items[it].depth + by }
        if (dir > 0) {
            put(0 until own.first, 0); put(span, 0); put(own, shift); put(span.last + 1 until items.size, 0)
        } else {
            put(0 until span.first, 0); put(own, shift); put(span, 0); put(own.last + 1 until items.size, 0)
        }
        return settle(items, order, depths)
    }

    /** Whether item [index] can go into a folder right above it (the folder just before it at its depth). */
    fun canNest(items: List<Item>, index: Int): Boolean = nestTarget(items, index) != null

    private fun nestTarget(items: List<Item>, index: Int): Int? {
        val d = items.getOrNull(index)?.depth ?: return null
        val above = (index - 1 downTo 0).firstOrNull { items[it].depth <= d } ?: return null
        return above.takeIf { items[it].depth == d && items[it].folder }
    }

    /** Puts item [index] (with its contents) into the folder above it, as its last item. */
    fun nest(items: List<Item>, index: Int): Change? {
        nestTarget(items, index) ?: return null
        val own = unit(items, index)
        return settle(items, items.indices.toList(), items.mapIndexed { k, it -> it.depth + if (k in own) 1 else 0 })
    }

    /** Takes item [index] (with its contents) out of its folder, to just after that folder. */
    fun unnest(items: List<Item>, index: Int): Change? {
        val parent = enclosing(items, index).lastOrNull() ?: return null
        val own = unit(items, index)
        val end = unit(items, parent).last
        val order = ArrayList<Int>(); val depths = ArrayList<Int>()
        fun put(range: IntRange, by: Int) = range.forEach { order += it; depths += items[it].depth + by }
        put(0 until own.first, 0); put(own.last + 1..end, 0); put(own, -1); put(end + 1 until items.size, 0)
        return settle(items, order, depths)
    }

    private fun settle(items: List<Item>, order: List<Int>, depths: List<Int>): Change =
        Change(order, normalize(order.mapIndexed { k, i -> items[i].copy(depth = depths[k]) }))

    /**
     * Depths from the earlier format, where only folders had a level and a folder held every item
     * after it up to the next folder at its level or higher.
     */
    fun fromLegacy(levels: List<Int?>): List<Int> {
        val stack = ArrayList<Int>() // levels of the folders around the current item
        return levels.map { level ->
            if (level == null) (stack.lastOrNull()?.plus(1)) ?: 0
            else {
                while (stack.isNotEmpty() && stack.last() >= level) stack.removeAt(stack.lastIndex)
                stack += level
                level
            }
        }
    }
}
