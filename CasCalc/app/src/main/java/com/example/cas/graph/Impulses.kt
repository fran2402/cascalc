package com.example.cas.graph

import com.example.cas.cas.Add
import com.example.cas.cas.Algebra
import com.example.cas.cas.Expr
import com.example.cas.cas.Fn
import com.example.cas.cas.Mul
import com.example.cas.cas.Sym
import com.example.cas.cas.ZERO
import com.example.cas.cas.contains
import com.example.cas.cas.div
import com.example.cas.cas.fn
import com.example.cas.cas.freeOf
import com.example.cas.cas.mul
import com.example.cas.cas.neg
import com.example.cas.cas.subst

/**
 * The impulses in y = f(x): each term c·g(x)·δ(kx + b) is an arrow of height c·g(a)/|k| at
 * x = a = −b/k, drawn on top of the rest of the curve (where δ counts as 0), as an engineer draws
 * δ. The position and height may use sliders.
 */
object Impulses {
    class Impulse(val at: Expr, val height: Expr)

    fun of(e: Expr, x: Sym): List<Impulse> {
        if (!e.contains { it is Fn && it.name == "dirac" }) return emptyList()
        val terms = Algebra.expand(e).let { if (it is Add) it.terms else listOf(it) }
        return terms.mapNotNull { t ->
            val fs = if (t is Mul) t.factors else listOf(t)
            val d = fs.filterIsInstance<Fn>().singleOrNull { it.name == "dirac" } ?: return@mapNotNull null
            val c = Algebra.coefficients(Algebra.expand(d.args[0]), x)?.takeIf { it.size == 2 && it.all { k -> k.freeOf(x) } } ?: return@mapNotNull null
            val (b, k) = c
            if (k == ZERO) return@mapNotNull null
            val a = Algebra.simplify(neg(div(b, k)))
            val rest = mul(fs.filter { it !== d }).subst(x, a)
            Impulse(a, Algebra.simplify(div(rest, fn("abs", k))))
        }
    }
}
