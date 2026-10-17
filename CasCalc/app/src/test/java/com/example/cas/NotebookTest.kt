package com.example.cas

import com.example.cas.editor.Frac
import com.example.cas.editor.MathRow
import com.example.cas.editor.Node
import com.example.cas.editor.Pow
import com.example.cas.editor.Sym
import com.example.cas.engine.Formatter
import com.example.cas.engine.Notebook
import com.example.cas.engine.Notebook.Block
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotebookTest {
    private fun m(vararg parts: Any): MathRow = MathRow(parts.flatMap { p -> when (p) { is String -> p.map { Sym(it.toString()) }; is Node -> listOf(p); else -> error("") } }.toMutableList())
    private fun def(name: String, value: String) = m(name).also { it.add(Sym(":=")); m(value).items.toList().forEach { n -> it.add(n) } }

    private val blocks = listOf(
        Block.Text(1, "# Setup\nA ball at \$v_0\$ straight up."),
        Block.Math(2, MathRow(mutableListOf<Node>().apply { addAll(def("g", "10").items); add(Sym(",")); addAll(def("v", "20").items) })),
        Block.Math(3, m("v", Frac(m("v"), m("2g")))),
        Block.Graph(4, m("vt−5t", Pow(m("2"))), 0.0, 4.0),
    )

    @Test fun definitionsHoldBelow() {
        val run = Notebook.run(blocks)
        assertEquals(listOf("g", "v"), run.outcomes[2]!!.defines)
        // v · v/(2g) = 20 · 20/20 = 20
        assertEquals("20", Formatter.plain(run.outcomes[3]!!.answer!!.exact))
        val plot = run.plots[4]!!
        assertNull(plot.error)
        assertEquals(1, plot.curves.size)
        // The peak of 20t − 5t² is 20 at t = 2.
        assertTrue(plot.curves[0].max() in 19.9..20.0)
    }

    @Test fun fileRoundTrip() {
        val doc = Notebook.Doc("a", "Projectile\tlab", blocks, 42)
        val back = Notebook.decode("a", Notebook.encode(doc))
        assertEquals("Projectile\tlab", back.title)
        assertEquals(4, back.blocks.size)
        assertEquals((blocks[0] as Block.Text).text, (back.blocks[0] as Block.Text).text)
        assertEquals(Formatter.plain((blocks[2] as Block.Math).row), Formatter.plain((back.blocks[2] as Block.Math).row))
        assertEquals(4.0, (back.blocks[3] as Block.Graph).xMax)
    }

    @Test fun latexExport() {
        val doc = Notebook.Doc("a", "Lab", blocks, 0)
        val tex = Notebook.latex(doc, Notebook.run(blocks))
        assertTrue(tex, tex.contains("\\section*{Setup}"))
        assertTrue(tex, tex.contains("\\addplot"))
        assertTrue(tex, Regex("""\$[^$]*v[^$]*\$""").containsMatchIn(tex))
        assertNotNull(tex, tex.lines().firstOrNull { it.startsWith("\\[") && it.contains("= 20") })
    }

    @Test fun pdfPages() {
        val doc = Notebook.Doc("a", "Lab", blocks, 0)
        val pages = com.example.cas.graph.NotebookExport.pages(doc, Notebook.run(blocks), "today")
        assertEquals(1, pages.size)
        // Title, the heading and paragraph, two calculations and a graph with its grid and curve.
        assertTrue(pages[0].items.size > 20)
    }
}
