package com.example.cas

import com.example.cas.editor.*
import com.example.cas.engine.Evaluator
import com.example.cas.graph.Sketch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** The faded sketch beside answers: only for curves in one letter, over a stretch that shows them. */
class SketchTest {
    private fun r(text: String): MathRow { val out = MathRow(); for (c in text) out.add(when (c) { '²' -> Pow(row("2")); '³' -> Pow(row("3")); else -> Sym(c.toString()) }); return out }
    private fun r(vararg parts: Any): MathRow = MathRow(parts.flatMap { p -> if (p is String) r(p).items else listOf(p as Node) }.toMutableList())
    private fun f(name: String, vararg a: Any) = Func(name, a.map { if (it is String) r(it) else it as MathRow })
    private fun sketch(row: MathRow) = Sketch.of(Evaluator().evaluate(row))

    private val cases = mapOf(
        "sinexp" to r(f("sin", "x"), Sym("e"), Pow(r(Sym("−"), Frac(r("x"), r("5"))))),
        "poly" to r("x²−4"),
        "lorentz" to r(Frac(r("2"), r("x²+1"))),
        "ln" to r(f("ln", "2x")),
        "tan" to r(f("tan", "x")),
        "cubic" to r("x³−3x"),
        "exp" to r(Sym("e"), Pow(r("x"))),
        "sinc" to r(Frac(r(f("sin", "x")), r("x"))),
    )

    @Test fun curves() = cases.forEach { (n, row) -> assertNotNull(n, sketch(row)) }

    @Test fun notCurves() {
        assertNull(sketch(r(Frac(r("1"), r("3")))))
        assertNull(sketch(r("x+y")))
        assertNull(Sketch.of(com.example.cas.cas.Eq(com.example.cas.cas.Sym("x"), com.example.cas.cas.Num(2))))
    }

    // A pole breaks the line instead of joining across it.
    @Test fun poleBreaks() = assertEquals(true, sketch(cases.getValue("tan"))!!.ys.any { it.isNaN() })

    @Test fun dump() {
        val out = System.getenv("SKETCH_OUT") ?: return
        val sb = StringBuilder("<html><body style='background:#f6f3e7;display:flex;flex-wrap:wrap;gap:20px'>")
        for ((n, row) in cases) {
            val c = sketch(row)!!
            val d = StringBuilder()
            var open = false
            for (k in c.ys.indices) { val y = c.ys[k]; if (y.isNaN()) { open = false; continue }; d.append(if (open) "L" else "M").append("%.1f,%.1f".format(c.xs[k] * 240, 4 + (1 - y) * 40)); open = true }
            sb.append("<div>$n<br><svg width=240 height=48 style='background:#fff'><path d='$d' fill=none stroke='#4a5d23' stroke-width=2/></svg></div>")
        }
        java.io.File(out).writeText(sb.append("</body></html>").toString())
    }
}
