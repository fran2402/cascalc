package com.example.cas.engine

/**
 * Markdown for the notebooks' text blocks, with math: headings (# to ######, or a line
 * underlined with === or ---), paragraphs, **bold**, *italic*, ***both***, ~~strikethrough~~,
 * `code`, [links](url), <https://autolinks>, ![images](url), bullet, numbered and task lists
 * (nested by indenting), > quotes (nested too), ``` code blocks ```, horizontal rules, tables
 * with | and alignment, hard line breaks (two spaces or \ at the end of a line), \ escapes,
 * $inline math$ and $$display math$$.
 */
object Markdown {
    enum class Align { None, Left, Center, Right }

    sealed class Inline {
        data class Text(val text: String) : Inline()
        data class Bold(val children: List<Inline>) : Inline()
        data class Italic(val children: List<Inline>) : Inline()
        data class Strike(val children: List<Inline>) : Inline()
        data class Code(val text: String) : Inline()
        data class Math(val latex: String) : Inline()
        data class Link(val children: List<Inline>, val url: String) : Inline()
        data class Image(val alt: String, val url: String) : Inline()
        object Break : Inline()
    }

    sealed class Block {
        data class Heading(val level: Int, val inlines: List<Inline>) : Block()
        data class Paragraph(val inlines: List<Inline>) : Block()
        /** [checked] is null for a plain item, true or false for a task; [line] is the item's line in the source (for ticking it). */
        data class Item(val blocks: List<Block>, val checked: Boolean?, val line: Int)
        data class ListBlock(val ordered: Boolean, val start: Int, val items: List<Item>) : Block()
        data class Quote(val blocks: List<Block>) : Block()
        data class Code(val language: String, val text: String) : Block()
        data class MathBlock(val latex: String) : Block()
        object Rule : Block()
        data class Table(val header: List<List<Inline>>, val align: List<Align>, val rows: List<List<List<Inline>>>) : Block()
    }

    fun parse(text: String): List<Block> = blocks(text.replace("\r", "").replace("\t", "    ").lines(), 0)

    // ---- Blocks --------------------------------------------------------------------------------

    private val heading = Regex("""^ {0,3}(#{1,6})(?:\s+(.*?))?\s*#*\s*$""")
    private val rule = Regex("""^ {0,3}([-*_])(?:\s*\1){2,}\s*$""")
    private val bullet = Regex("""^( *)([-*+])\s+(.*)$""")
    private val ordered = Regex("""^( *)(\d{1,9})([.)])\s+(.*)$""")
    private val fence = Regex("""^ {0,3}(```+|~~~+)\s*(\S*).*$""")
    private val tableDelimiter = Regex("""^\s*\|?\s*:?-+:?\s*(\|\s*:?-+:?\s*)*\|?\s*$""")
    private val task = Regex("""^\[([ xX])]\s+(.*)$""")

    private fun indentOf(s: String) = s.length - s.trimStart(' ').length

    private fun blocks(lines: List<String>, offset: Int): List<Block> {
        val out = ArrayList<Block>()
        var k = 0
        val para = ArrayList<String>()
        fun flush() { if (para.isNotEmpty()) { out += Block.Paragraph(paragraphInlines(para)); para.clear() } }
        while (k < lines.size) {
            val line = lines[k]
            val t = line.trim()
            if (t.isEmpty()) { flush(); k++; continue }
            // A paragraph line underlined with === or --- is a heading.
            if (para.isNotEmpty() && (Regex("""^ {0,3}=+\s*$""").matches(line) || Regex("""^ {0,3}-+\s*$""").matches(line))) {
                val level = if (t.startsWith("=")) 1 else 2
                out += Block.Heading(level, inlines(para.joinToString(" ") { it.trim() })); para.clear(); k++; continue
            }
            val fm = fence.matchEntire(line)
            if (fm != null) {
                flush()
                val marker = fm.groupValues[1]
                val body = ArrayList<String>()
                k++
                while (k < lines.size && !lines[k].trim().startsWith(marker.take(3))) { body += lines[k]; k++ }
                k++
                out += Block.Code(fm.groupValues[2], body.joinToString("\n"))
                continue
            }
            if (t.startsWith("$$")) {
                flush()
                val rest = t.removePrefix("$$")
                if (rest.endsWith("$$") && rest.length >= 2) { out += Block.MathBlock(rest.removeSuffix("$$").trim()); k++; continue }
                val body = arrayListOf(rest)
                k++
                while (k < lines.size && !lines[k].trim().endsWith("$$")) { body += lines[k]; k++ }
                if (k < lines.size) body += lines[k].trim().removeSuffix("$$")
                k++
                out += Block.MathBlock(body.joinToString(" ").trim())
                continue
            }
            val hm = heading.matchEntire(line)
            if (hm != null) { flush(); out += Block.Heading(hm.groupValues[1].length, inlines(hm.groupValues[2])); k++; continue }
            if (rule.matches(line)) { flush(); out += Block.Rule; k++; continue }
            if (t.startsWith(">")) {
                flush()
                val body = ArrayList<String>()
                val start = k
                while (k < lines.size && lines[k].trim().startsWith(">")) { body += lines[k].trim().removePrefix(">").removePrefix(" "); k++ }
                out += Block.Quote(blocks(body, offset + start))
                continue
            }
            // A table: a row with |, then a delimiter row (---, :--, --:, :-:).
            if ('|' in t && k + 1 < lines.size && tableDelimiter.matches(lines[k + 1]) && lines[k + 1].contains('-')) {
                flush()
                val header = cells(t)
                val align = cells(lines[k + 1].trim()).map { c ->
                    val l = c.startsWith(":"); val r = c.endsWith(":")
                    when { l && r -> Align.Center; r -> Align.Right; l -> Align.Left; else -> Align.None }
                }
                k += 2
                val rows = ArrayList<List<List<Inline>>>()
                while (k < lines.size && '|' in lines[k] && lines[k].isNotBlank()) {
                    val c = cells(lines[k].trim())
                    rows += header.indices.map { i -> inlines(c.getOrElse(i) { "" }) }
                    k++
                }
                out += Block.Table(header.map { inlines(it) }, header.indices.map { align.getOrElse(it) { Align.None } }, rows)
                continue
            }
            val b = bullet.matchEntire(line); val o = ordered.matchEntire(line)
            if (b != null || o != null) {
                flush()
                val isOrdered = b == null
                val base = indentOf(line)
                val start = o?.groupValues?.get(2)?.toIntOrNull() ?: 1
                val items = ArrayList<Block.Item>()
                while (k < lines.size) {
                    val cur = lines[k]
                    val mb = bullet.matchEntire(cur); val mo = ordered.matchEntire(cur)
                    val m = (if (isOrdered) mo else mb) ?: break
                    if (indentOf(cur) != base) break
                    val content = if (isOrdered) m.groupValues[4] else m.groupValues[3]
                    val contentIndent = cur.length - content.length
                    val itemLine = offset + k
                    val body = arrayListOf(content)
                    k++
                    // The item's other lines: indented under it, or a lazy continuation of its text.
                    while (k < lines.size) {
                        val next = lines[k]
                        if (next.isBlank()) {
                            if (k + 1 < lines.size && lines[k + 1].isNotBlank() && indentOf(lines[k + 1]) >= contentIndent.coerceAtMost(base + 2)) { body += ""; k++; continue }
                            break
                        }
                        val ind = indentOf(next)
                        if (ind > base) { body += next.drop(minOf(ind, contentIndent.coerceAtLeast(base + 1))); k++; continue }
                        if (bullet.matches(next) || ordered.matches(next) || next.trim().startsWith(">") || heading.matches(next) || fence.matches(next)) break
                        body += next.trim(); k++
                    }
                    var checked: Boolean? = null
                    task.matchEntire(body[0])?.let { tm -> checked = tm.groupValues[1] != " "; body[0] = tm.groupValues[2] }
                    items += Block.Item(blocks(body, itemLine), checked, itemLine)
                }
                out += Block.ListBlock(isOrdered, start, items)
                continue
            }
            para += line
            k++
        }
        flush()
        return out
    }

    /** A table row's cells, without the outer pipes; \| is a pipe inside a cell. */
    private fun cells(row: String): List<String> {
        val s = row.trim().removePrefix("|").let { if (it.endsWith("|") && !it.endsWith("\\|")) it.dropLast(1) else it }
        val out = ArrayList<String>(); val cur = StringBuilder()
        var k = 0
        while (k < s.length) {
            if (s[k] == '\\' && k + 1 < s.length && s[k + 1] == '|') { cur.append('|'); k += 2; continue }
            if (s[k] == '|') { out += cur.toString().trim(); cur.clear() } else cur.append(s[k])
            k++
        }
        out += cur.toString().trim()
        return out
    }

    /** A paragraph's lines joined: a line ending in two spaces or \ breaks the line there. */
    private fun paragraphInlines(lines: List<String>): List<Inline> {
        val out = ArrayList<Inline>()
        lines.forEachIndexed { k, raw ->
            val hard = raw.endsWith("  ") || raw.trimEnd().endsWith("\\") && !raw.trimEnd().endsWith("\\\\")
            val text = raw.trim().let { if (it.endsWith("\\") && hard) it.dropLast(1) else it }
            out += inlines(text)
            if (k < lines.lastIndex) out += if (hard) Inline.Break else Inline.Text(" ")
        }
        return merge(out)
    }

    // ---- Inlines -------------------------------------------------------------------------------

    fun inlines(s: String): List<Inline> {
        val out = ArrayList<Inline>()
        val text = StringBuilder()
        fun flush() { if (text.isNotEmpty()) { out += Inline.Text(text.toString()); text.clear() } }
        var k = 0
        while (k < s.length) {
            val c = s[k]
            when {
                c == '\\' && k + 1 < s.length && s[k + 1] in "\\`*_{}[]()#+-.!|~\$>" -> { text.append(s[k + 1]); k += 2 }
                c == '`' -> {
                    val ticks = s.drop(k).takeWhile { it == '`' }.length
                    val end = s.indexOf("`".repeat(ticks), k + ticks)
                    if (end < 0) { text.append(s, k, k + ticks); k += ticks }
                    else { flush(); out += Inline.Code(s.substring(k + ticks, end).trim()); k = end + ticks }
                }
                c == '$' && s.startsWith("$$", k) -> {
                    val end = s.indexOf("$$", k + 2)
                    if (end < 0) { text.append("$$"); k += 2 } else { flush(); out += Inline.Math(s.substring(k + 2, end).trim()); k = end + 2 }
                }
                c == '$' -> {
                    val end = s.indexOf('$', k + 1)
                    if (end < 0 || end == k + 1) { text.append(c); k++ } else { flush(); out += Inline.Math(s.substring(k + 1, end)); k = end + 1 }
                }
                c == '!' && s.startsWith("![", k) -> {
                    val link = link(s, k + 1)
                    if (link == null) { text.append(c); k++ } else { flush(); out += Inline.Image(link.first, link.second); k = link.third }
                }
                c == '[' -> {
                    val link = link(s, k)
                    if (link == null) { text.append(c); k++ } else { flush(); out += Inline.Link(inlines(link.first), link.second); k = link.third }
                }
                c == '<' && Regex("""^<(https?://[^>\s]+|mailto:[^>\s]+)>""").find(s.substring(k)) != null -> {
                    val m = Regex("""^<([^>]+)>""").find(s.substring(k))!!
                    flush(); out += Inline.Link(listOf(Inline.Text(m.groupValues[1])), m.groupValues[1]); k += m.value.length
                }
                c == '~' && s.startsWith("~~", k) -> {
                    val end = s.indexOf("~~", k + 2)
                    if (end < 0) { text.append("~~"); k += 2 } else { flush(); out += Inline.Strike(inlines(s.substring(k + 2, end))); k = end + 2 }
                }
                c == '*' || c == '_' -> {
                    val run = s.drop(k).takeWhile { it == c }.length.coerceAtMost(3)
                    // _ inside a word (snake_case) is just a letter.
                    val inWord = c == '_' && k > 0 && s[k - 1].isLetterOrDigit()
                    val delim = c.toString().repeat(run)
                    val end = if (inWord) -1 else closing(s, delim, k + run)
                    if (end < 0) { text.append(delim); k += run }
                    else {
                        flush()
                        val inner = inlines(s.substring(k + run, end))
                        out += when (run) { 1 -> Inline.Italic(inner); 2 -> Inline.Bold(inner); else -> Inline.Bold(listOf(Inline.Italic(inner))) }
                        k = end + run
                    }
                }
                else -> { text.append(c); k++ }
            }
        }
        flush()
        return merge(out)
    }

    /** Where [delim] closes, not right after whitespace, from [from]; −1 if it doesn't. */
    private fun closing(s: String, delim: String, from: Int): Int {
        if (from >= s.length || s[from] == ' ') return -1
        var k = from
        while (true) {
            val at = s.indexOf(delim, k)
            if (at < 0) return -1
            val before = s[at - 1]
            val after = s.getOrNull(at + delim.length)
            if (before != ' ' && after != delim[0] && at > from) return at
            k = at + 1
        }
    }

    /** [text](url) starting at [k]: the text, the url and where it ends. */
    private fun link(s: String, k: Int): Triple<String, String, Int>? {
        if (s.getOrNull(k) != '[') return null
        var depth = 0; var j = k
        while (j < s.length) { if (s[j] == '[') depth++; if (s[j] == ']') { depth--; if (depth == 0) break }; j++ }
        if (j >= s.length || s.getOrNull(j + 1) != '(') return null
        val end = s.indexOf(')', j + 2)
        if (end < 0) return null
        val url = s.substring(j + 2, end).trim().substringBefore(' ')
        return Triple(s.substring(k + 1, j), url, end + 1)
    }

    private fun merge(list: List<Inline>): List<Inline> {
        val out = ArrayList<Inline>()
        for (i in list) { val last = out.lastOrNull(); if (i is Inline.Text && last is Inline.Text) out[out.lastIndex] = Inline.Text(last.text + i.text) else out += i }
        return out
    }

    /** The plain text of inlines (for search, alt texts and the like). */
    fun plain(list: List<Inline>): String = list.joinToString("") { i ->
        when (i) {
            is Inline.Text -> i.text; is Inline.Code -> i.text; is Inline.Math -> i.latex; is Inline.Image -> i.alt; Inline.Break -> "\n"
            is Inline.Bold -> plain(i.children); is Inline.Italic -> plain(i.children); is Inline.Strike -> plain(i.children); is Inline.Link -> plain(i.children)
        }
    }

    /** [text] with the task on source line [line] ticked or unticked. */
    fun toggleTask(text: String, line: Int): String {
        val lines = text.lines().toMutableList()
        val l = lines.getOrNull(line) ?: return text
        lines[line] = when {
            l.contains("[ ]") -> l.replaceFirst("[ ]", "[x]")
            l.contains("[x]") -> l.replaceFirst("[x]", "[ ]")
            l.contains("[X]") -> l.replaceFirst("[X]", "[ ]")
            else -> l
        }
        return lines.joinToString("\n")
    }

    // ---- LaTeX ---------------------------------------------------------------------------------

    private fun tex(s: String) = s.replace("\\", "\\textbackslash{}").replace("&", "\\&").replace("%", "\\%").replace("#", "\\#")
        .replace("_", "\\_").replace("{", "\\{").replace("}", "\\}").replace("~", "\\textasciitilde{}").replace("^", "\\textasciicircum{}").replace("$", "\\$")

    fun inlineLatex(list: List<Inline>): String = list.joinToString("") { i ->
        when (i) {
            is Inline.Text -> tex(i.text)
            is Inline.Bold -> "\\textbf{${inlineLatex(i.children)}}"
            is Inline.Italic -> "\\emph{${inlineLatex(i.children)}}"
            is Inline.Strike -> "\\sout{${inlineLatex(i.children)}}"
            is Inline.Code -> "\\texttt{${tex(i.text)}}"
            is Inline.Math -> "\$${i.latex}\$"
            is Inline.Link -> "\\href{${i.url.replace("%", "\\%").replace("#", "\\#")}}{${inlineLatex(i.children)}}"
            is Inline.Image -> "[${tex(i.alt.ifBlank { "image" })}]"
            Inline.Break -> "\\\\\n"
        }
    }

    fun latex(blocks: List<Block>): String = buildString {
        for (b in blocks) when (b) {
            is Block.Heading -> appendLine((when (b.level) { 1 -> "\\section*{"; 2 -> "\\subsection*{"; 3 -> "\\subsubsection*{"; else -> "\\paragraph*{" }) + inlineLatex(b.inlines) + "}")
            is Block.Paragraph -> { appendLine(inlineLatex(b.inlines)); appendLine() }
            is Block.ListBlock -> {
                val env = if (b.ordered) "enumerate" else "itemize"
                appendLine("\\begin{$env}" + if (b.ordered && b.start != 1) "\\setcounter{enumi}{${b.start - 1}}" else "")
                for (item in b.items) {
                    val mark = when (item.checked) { true -> "[\$\\boxtimes\$]"; false -> "[\$\\square\$]"; null -> "" }
                    append("\\item$mark ")
                    append(latex(item.blocks).trim())
                    appendLine()
                }
                appendLine("\\end{$env}")
            }
            is Block.Quote -> { appendLine("\\begin{quote}"); append(latex(b.blocks)); appendLine("\\end{quote}") }
            is Block.Code -> { appendLine("\\begin{verbatim}"); appendLine(b.text); appendLine("\\end{verbatim}") }
            is Block.MathBlock -> { appendLine("\\[ ${b.latex} \\]"); appendLine() }
            Block.Rule -> { appendLine("\\noindent\\rule{\\linewidth}{0.4pt}"); appendLine() }
            is Block.Table -> {
                val spec = b.align.joinToString("") { when (it) { Align.Center -> "c"; Align.Right -> "r"; else -> "l" } }
                appendLine("\\begin{center}\\begin{tabular}{$spec}")
                appendLine("\\hline")
                appendLine(b.header.joinToString(" & ") { "\\textbf{${inlineLatex(it)}}" } + " \\\\ \\hline")
                for (r in b.rows) appendLine(r.joinToString(" & ") { inlineLatex(it) } + " \\\\")
                appendLine("\\hline")
                appendLine("\\end{tabular}\\end{center}")
            }
        }
    }
}
