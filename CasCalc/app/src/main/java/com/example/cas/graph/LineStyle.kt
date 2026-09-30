package com.example.cas.graph

/**
 * How a curve is drawn, after matplotlib's line styles: solid ('-'), dashed ('--'), dotted
 * (':'), dash-dot ('-.'), and two of its parametrised ones, long dashes and dash-dot-dot.
 * Patterns are in multiples of the line's width, so they keep their look at any thickness
 * (a 0.01 dash with round caps is a dot). Saved by position, so new styles go at the end.
 */
enum class LineStyle(val label: String, private val units: DoubleArray?) {
    Solid("Solid", null),
    Dashed("Dashed", doubleArrayOf(4.0, 3.0)),
    Dotted("Dotted", doubleArrayOf(0.01, 2.5)),
    DashDot("Dash-dot", doubleArrayOf(4.0, 2.5, 0.01, 2.5)),
    LongDash("Long dash", doubleArrayOf(9.0, 3.5)),
    DashDotDot("Dash-dot-dot", doubleArrayOf(4.0, 2.5, 0.01, 2.5, 0.01, 2.5));

    /** The on-off lengths for a line [width] wide, or null for solid. */
    fun pattern(width: Double): DoubleArray? = units?.let { u -> DoubleArray(u.size) { u[it] * width } }

    companion object {
        fun of(index: Int) = entries.getOrElse(index) { Solid }
    }
}
