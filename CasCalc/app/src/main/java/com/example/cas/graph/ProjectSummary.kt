package com.example.cas.graph

/**
 * What a saved graph's card shows, read from its stored data without drawing it: each line's
 * code (only when short enough to draw), whether it's a data set, and its colour. Data sets and
 * other long lines are never decoded: thousands of points drawn as maths would stall the page.
 */
object ProjectSummary {
    /** Lines longer than this (in stored code) are summarised rather than drawn. */
    const val LONG = 240

    /**
     * One line: [code] to draw, or null when it's too long; its colour slot or own colour.
     * [isData]: a long list of numbers (a data set) rather than a long formula.
     */
    data class Line(val code: String?, val slot: Int, val color: Int?, val isData: Boolean = false)

    data class Summary(val lines: List<Line>, val dataSets: Int)

    /** [noteCode] is how notes and folders are stored (they aren't shown). */
    fun of(data: Map<String, String>, noteCode: String): Summary {
        val codes = data["functions"].orEmpty().split("\n")
        val slots = data["slots"].orEmpty().split(",")
        val colors = data["colors"].orEmpty().split(",")
        val lines = codes.mapIndexedNotNull { i, code ->
            if (code.isBlank() || code == noteCode) return@mapIndexedNotNull null
            val long = code.length > LONG
            Line(
                code.takeIf { !long },
                slots.getOrNull(i)?.toIntOrNull() ?: i,
                colors.getOrNull(i)?.toLongOrNull()?.toInt(),
                isData = long && code.count { it == ',' } > 20,
            )
        }
        return Summary(lines, lines.count { it.isData })
    }
}
