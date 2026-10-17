package com.example.cas

import com.example.cas.engine.Markdown
import com.example.cas.engine.Markdown.Block
import com.example.cas.engine.Markdown.Inline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTest {
    @Test fun headingsAndInlines() {
        val b = Markdown.parse("# Title\n## Sub\nSome **bold**, *italic*, ***both***, ~~gone~~, `code`, \$x^2\$ and [a link](https://x.org).")
        assertEquals(Block.Heading(1, listOf(Inline.Text("Title"))), b[0])
        assertEquals(2, (b[1] as Block.Heading).level)
        val p = (b[2] as Block.Paragraph).inlines
        assertTrue(p.contains(Inline.Bold(listOf(Inline.Text("bold")))))
        assertTrue(p.contains(Inline.Italic(listOf(Inline.Text("italic")))))
        assertTrue(p.contains(Inline.Bold(listOf(Inline.Italic(listOf(Inline.Text("both")))))))
        assertTrue(p.contains(Inline.Strike(listOf(Inline.Text("gone")))))
        assertTrue(p.contains(Inline.Code("code")))
        assertTrue(p.contains(Inline.Math("x^2")))
        assertTrue(p.contains(Inline.Link(listOf(Inline.Text("a link")), "https://x.org")))
    }

    @Test fun snakeCaseAndEscapes() {
        val p = (Markdown.parse("a_b_c and \\*not italic\\*")[0] as Block.Paragraph).inlines
        assertEquals(listOf(Inline.Text("a_b_c and *not italic*")), p)
    }

    @Test fun listsTasksAndNesting() {
        val b = Markdown.parse("- one\n- [x] done\n  - nested\n1. first\n2. second\n- [ ] todo")
        val list = b[0] as Block.ListBlock
        assertEquals(false, list.ordered)
        assertEquals(2, list.items.size)
        assertEquals(true, list.items[1].checked)
        assertTrue(list.items[1].blocks.any { it is Block.ListBlock })
        val numbered = b[1] as Block.ListBlock
        assertTrue(numbered.ordered); assertEquals(2, numbered.items.size)
        assertEquals(false, (b[2] as Block.ListBlock).items[0].checked)
        assertEquals("- [x] todo", Markdown.toggleTask("- [ ] todo", 0))
    }

    @Test fun quotesCodeRulesMathTables() {
        val b = Markdown.parse("> quoted\n> > deeper\n\n```kotlin\nval x = 1\n```\n---\n\$\$\\int_0^1 x\\,dx\$\$\n\n| a | b |\n|:--|--:|\n| 1 | 2 |\nSetext\n===")
        val q = b[0] as Block.Quote
        assertTrue(q.blocks.any { it is Block.Quote })
        assertEquals(Block.Code("kotlin", "val x = 1"), b[1])
        assertEquals(Block.Rule, b[2])
        assertEquals(Block.MathBlock("\\int_0^1 x\\,dx"), b[3])
        val t = b[4] as Block.Table
        assertEquals(listOf(Markdown.Align.Left, Markdown.Align.Right), t.align)
        assertEquals(1, t.rows.size)
        assertEquals(Block.Heading(1, listOf(Inline.Text("Setext"))), b[5])
    }

    @Test fun hardBreaksAndLatex() {
        val p = (Markdown.parse("one  \ntwo")[0] as Block.Paragraph).inlines
        assertTrue(p.contains(Inline.Break))
        val tex = Markdown.latex(Markdown.parse("# H\n- **a** 50%\n- [x] b"))
        assertTrue(tex, tex.contains("\\section*{H}") && tex.contains("\\textbf{a} 50\\%") && tex.contains("\\begin{itemize}") && tex.contains("boxtimes"))
    }
}
