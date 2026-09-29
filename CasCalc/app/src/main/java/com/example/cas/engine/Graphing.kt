package com.example.cas.engine

import com.example.cas.cas.Eq
import com.example.cas.cas.Expr
import com.example.cas.cas.Mat
import com.example.cas.cas.Seq
import com.example.cas.cas.freeVars
import com.example.cas.editor.Func
import com.example.cas.editor.MathCodec
import com.example.cas.editor.MathRow
import com.example.cas.editor.Sym

/** What a calculator result can send to the graphs. */
data class GraphRequest(val rows: List<MathRow>, val dimensions: Int)

object Graphing {
    /**
     * - solve(f(x) = g(x)) sends both sides, so the solutions are where the curves cross;
     * - anything that depends on x only goes to the 2D graph, on x and y to the 3D graph;
     * - y = f(x) (e.g. from dsolve without constants) sends f(x).
     */
    fun request(input: MathRow, value: Expr): GraphRequest? {
        val only = input.items.singleOrNull()
        if (only is Func && only.name == "solve") {
            val eq = only.args[0].items
            val at = eq.indexOfFirst { (it as? Sym)?.text == "=" }
            val commas = eq.any { (it as? Sym)?.text == "," }
            if (at > 0 && !commas && only.args[1].plainText().let { it.isEmpty() || it == "x" }) {
                val lhs = MathRow(eq.subList(0, at).map { MathCodec.copy(MathRow(mutableListOf(it))).items[0] }.toMutableList())
                val rhs = MathRow(eq.drop(at + 1).map { MathCodec.copy(MathRow(mutableListOf(it))).items[0] }.toMutableList())
                val vars = runCatching { Evaluator().evaluate(lhs).freeVars() + Evaluator().evaluate(rhs).freeVars() }.getOrNull()
                if (vars != null && vars == setOf("x")) return GraphRequest(listOf(lhs, rhs), 2)
            }
        }
        val e = if (value is Eq && value.lhs == com.example.cas.cas.Sym("y")) value.rhs else value
        if (e is Eq || e is Seq || e is Mat || e is com.example.cas.cas.Rel) return null
        return when (e.freeVars()) {
            setOf("x") -> GraphRequest(listOf(Formatter.row(e)), 2)
            // Functions of z go to the complex plane (dimensions = 1: one complex variable).
            setOf("z") -> GraphRequest(listOf(Formatter.row(e)), 1)
            setOf("x", "y") -> GraphRequest(listOf(Formatter.row(e)), 3)
            else -> null
        }
    }
}
