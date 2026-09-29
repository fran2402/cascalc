package com.example.cas.graph

/**
 * Folders in a graph's list of lines. The list is flat: each item is a line (null) or a folder
 * (its nesting level, 0 at the top). A folder holds the items after it up to the next folder at
 * its own level or higher, so a folder at level 1 right after one at level 0 is inside it.
 */
object FolderTree {
    /** The folders item [index] is in, outermost first, as indices. */
    fun enclosing(levels: List<Int?>, index: Int): List<Int> {
        val stack = ArrayList<Int>()
        for (k in 0 until index.coerceAtMost(levels.size)) {
            val level = levels[k] ?: continue
            while (stack.isNotEmpty() && levels[stack.last()]!! >= level) stack.removeAt(stack.lastIndex)
            stack += k
        }
        return stack
    }

    /** Everything folder [index] holds: the items after it until a folder at its level or higher. */
    fun members(levels: List<Int?>, index: Int): List<Int> {
        val level = levels.getOrNull(index) ?: return emptyList()
        return (index + 1 until levels.size).takeWhile { k -> levels[k].let { it == null || it > level } }
    }

    /** Whether folder [index] can go one level deeper: there's a folder above it at its level or higher. */
    fun canNest(levels: List<Int?>, index: Int): Boolean {
        val level = levels.getOrNull(index) ?: return false
        val above = (index - 1 downTo 0).firstOrNull { levels[it] != null } ?: return false
        return level <= levels[above]!!
    }
}
