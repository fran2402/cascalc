package com.example.cas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cas.engine.LatexParser
import com.example.cas.engine.Markdown
import com.example.cas.engine.Markdown.Block
import com.example.cas.engine.Markdown.Inline

/** A text block's Markdown, drawn: headings, styled text, lists and tasks (tap a box to tick it), quotes, code, math, tables, rules. */
@Composable
fun MarkdownView(text: String, onToggleTask: (Int) -> Unit, modifier: Modifier = Modifier) {
    val blocks = remember(text) { Markdown.parse(text) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) { blocks.forEach { MdBlock(it, onToggleTask) } }
}

@Composable
private fun MdBlock(b: Block, onToggleTask: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val body = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface)
    when (b) {
        // Each level a clear step down, all larger than the text (16 sp) but the last.
        is Block.Heading -> {
            val size = listOf(30, 25, 21, 18.5, 17, 16).map { it.toDouble() }[b.level.coerceIn(1, 6) - 1]
            MdInlines(b.inlines, body.copy(fontSize = size.sp, lineHeight = (size * 1.25).sp, fontWeight = if (b.level <= 2) FontWeight.Bold else FontWeight.SemiBold),
                Modifier.padding(top = if (b.level <= 2) 8.dp else 4.dp))
        }
        is Block.Paragraph -> MdInlines(b.inlines, body)
        is Block.ListBlock -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            b.items.forEachIndexed { k, item ->
                Row {
                    Box(Modifier.widthIn(min = 26.dp).padding(end = 6.dp), contentAlignment = Alignment.TopEnd) {
                        when {
                            item.checked != null -> Box(
                                Modifier.padding(top = 3.dp).size(18.dp).clip(RoundedCornerShape(4.dp))
                                    .background(if (item.checked) colors.primary else Color.Transparent)
                                    .border(2.dp, if (item.checked) colors.primary else colors.outline, RoundedCornerShape(4.dp))
                                    .clickable(onClickLabel = tr(if (item.checked) "Untick" else "Tick")) { onToggleTask(item.line) },
                                contentAlignment = Alignment.Center,
                            ) { if (item.checked) AppIcon(TableIcons.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(14.dp)) }
                            b.ordered -> Text("${b.start + k}.", style = body.copy(color = colors.onSurfaceVariant))
                            else -> Text("•", style = body.copy(color = colors.primary))
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        item.blocks.forEach { inner ->
                            // A ticked task reads as done.
                            if (item.checked == true && inner is Block.Paragraph) MdInlines(inner.inlines, body.copy(color = colors.onSurfaceVariant, textDecoration = TextDecoration.LineThrough))
                            else MdBlock(inner, onToggleTask)
                        }
                    }
                }
            }
        }
        is Block.Quote -> Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(colors.primary.copy(alpha = 0.6f)))
            Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                b.blocks.forEach { inner -> if (inner is Block.Paragraph) MdInlines(inner.inlines, body.copy(color = colors.onSurfaceVariant, fontStyle = FontStyle.Italic)) else MdBlock(inner, onToggleTask) }
            }
        }
        is Block.Code -> Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surfaceContainerHighest).padding(12.dp)) {
            if (b.language.isNotBlank()) Text(b.language, style = MaterialTheme.typography.labelSmall, color = colors.primary)
            Box(Modifier.horizontalScroll(rememberScrollState())) { Text(b.text, style = TextStyle(fontFamily = com.example.cas.ui.theme.CasFonts.Mono, fontSize = 14.sp, color = colors.onSurface)) }
        }
        is Block.MathBlock -> Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
            val row = remember(b.latex) { runCatching { LatexParser.parse(b.latex) }.getOrNull() }
            if (row != null) MathView(row, 22.sp, colors.onSurface) else Text(b.latex, style = body)
        }
        Block.Rule -> HorizontalDivider(Modifier.padding(vertical = 6.dp), color = colors.outlineVariant)
        is Block.Table -> MdTable(b)
    }
}

/** A table, column by column so each column takes the width of its widest cell. */
@Composable
private fun MdTable(t: Block.Table) {
    val colors = MaterialTheme.colorScheme
    val body = TextStyle(fontFamily = com.example.cas.ui.theme.CasFonts.Mono, fontSize = 14.sp, color = colors.onSurface)
    Box(Modifier.horizontalScroll(rememberScrollState()).clip(RoundedCornerShape(12.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(12.dp))) {
        Row {
            t.header.indices.forEach { c ->
                val align = when (t.align.getOrNull(c)) { Markdown.Align.Center -> Alignment.Center; Markdown.Align.Right -> Alignment.CenterEnd; else -> Alignment.CenterStart }
                Column(Modifier.width(IntrinsicSize.Max)) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 40.dp).background(colors.surfaceContainerHighest).padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = align) {
                        MdInlines(t.header[c], body.copy(fontWeight = FontWeight.SemiBold))
                    }
                    t.rows.forEachIndexed { r, row ->
                        Box(Modifier.fillMaxWidth().heightIn(min = 40.dp).background(if (r % 2 == 1) colors.surfaceContainer else Color.Transparent).padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = align) {
                            MdInlines(row.getOrElse(c) { emptyList() }, body)
                        }
                    }
                }
            }
        }
    }
}

/** One run of styled text: words, math, code, links (tap to open). */
private class Tok(val text: String?, val style: SpanStyle, val url: String? = null, val math: String? = null, val code: Boolean = false, val newline: Boolean = false)

private fun toks(list: List<Inline>, style: SpanStyle, url: String?, out: ArrayList<Tok>, link: Color) {
    for (i in list) when (i) {
        is Inline.Text -> i.text.split(' ').forEachIndexed { k, w -> if (w.isNotEmpty()) out += Tok(w, style, url) }
        is Inline.Bold -> toks(i.children, style.merge(SpanStyle(fontWeight = FontWeight.Bold)), url, out, link)
        is Inline.Italic -> toks(i.children, style.merge(SpanStyle(fontStyle = FontStyle.Italic)), url, out, link)
        is Inline.Strike -> toks(i.children, style.merge(SpanStyle(textDecoration = TextDecoration.LineThrough)), url, out, link)
        is Inline.Link -> toks(i.children, style.merge(SpanStyle(color = link, textDecoration = TextDecoration.Underline)), i.url, out, link)
        is Inline.Code -> out += Tok(i.text, style.merge(SpanStyle(fontFamily = com.example.cas.ui.theme.CasFonts.Mono)), url, code = true)
        is Inline.Math -> out += Tok(null, style, url, math = i.latex)
        is Inline.Image -> out += Tok("[" + i.alt.ifBlank { tr("image") } + "]", style.merge(SpanStyle(fontStyle = FontStyle.Italic)), i.url)
        Inline.Break -> out += Tok(null, style, newline = true)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MdInlines(list: List<Inline>, style: TextStyle, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val uri = LocalUriHandler.current
    val tokens = remember(list, style, colors.primary) { ArrayList<Tok>().also { toks(list, SpanStyle(), null, it, colors.primary) } }
    FlowRow(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        tokens.forEach { t ->
            val tap = if (t.url != null) Modifier.clickable(onClickLabel = t.url) { runCatching { uri.openUri(t.url) } } else Modifier
            when {
                t.newline -> Spacer(Modifier.fillMaxWidth())
                t.math != null -> Box(tap.padding(end = 4.dp)) { MathText("\$${t.math}\$", style = style, color = style.color) }
                t.code -> Text(t.text!!, style = style.merge(TextStyle(fontSize = style.fontSize * 0.92f)).merge(t.style.toTextStyle()), modifier = tap.padding(end = 4.dp).clip(RoundedCornerShape(6.dp)).background(colors.surfaceContainerHighest).padding(horizontal = 4.dp))
                else -> Text(t.text!! + " ", style = style.merge(t.style.toTextStyle()), modifier = tap)
            }
        }
    }
}

private fun SpanStyle.toTextStyle() = TextStyle(color = color, fontWeight = fontWeight, fontStyle = fontStyle, fontFamily = fontFamily, textDecoration = textDecoration)

// ---- Writing Markdown ---------------------------------------------------------------------------

/** Puts [left] and [right] around the selection (or around a placeholder when nothing is selected). */
internal fun TextFieldValue.wrap(left: String, right: String = left, placeholder: String = ""): TextFieldValue {
    val s = selection.min; val e = selection.max
    val inner = if (s == e) placeholder else text.substring(s, e)
    val t = text.substring(0, s) + left + inner + right + text.substring(e)
    return TextFieldValue(t, TextRange(s + left.length, s + left.length + inner.length))
}

/** Starts each line the selection touches with [prefix] (or takes it off when they all have it). */
internal fun TextFieldValue.prefixLines(prefix: String, numbered: Boolean = false): TextFieldValue {
    val start = text.lastIndexOf('\n', (selection.min - 1).coerceAtLeast(0)).let { if (selection.min == 0 || it < 0) 0 else it + 1 }
    val end = text.indexOf('\n', selection.max).let { if (it < 0) text.length else it }
    val lines = text.substring(start, end).split('\n')
    val pattern = if (numbered) Regex("""^\d+\.\s""") else null
    val all = lines.all { if (pattern != null) pattern.containsMatchIn(it) else it.startsWith(prefix) }
    val changed = lines.mapIndexed { k, l ->
        when {
            all && pattern != null -> l.replaceFirst(pattern, "")
            all -> l.removePrefix(prefix)
            numbered -> "${k + 1}. $l"
            else -> prefix + l
        }
    }.joinToString("\n")
    val t = text.substring(0, start) + changed + text.substring(end)
    return TextFieldValue(t, TextRange(start + changed.length))
}

/** Puts [snippet] at the cursor, on lines of its own when [block]. */
internal fun TextFieldValue.insert(snippet: String, block: Boolean = false): TextFieldValue {
    val s = selection.min
    val before = text.substring(0, s)
    val lead = if (block && before.isNotEmpty() && !before.endsWith("\n")) "\n" else ""
    val trail = if (block) "\n" else ""
    val t = before + lead + snippet + trail + text.substring(selection.max)
    return TextFieldValue(t, TextRange(s + lead.length + snippet.length + trail.length))
}

/** The formatting bar over an open text block: each button writes the Markdown for you. */
@Composable
fun MarkdownToolbar(value: TextFieldValue, onChange: (TextFieldValue) -> Unit, modifier: Modifier = Modifier, wrap: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    class Tool(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector?, val text: String?, val apply: (TextFieldValue) -> TextFieldValue)
    val tools = listOf(
        Tool("Bold", TableIcons.Bold, null) { it.wrap("**", placeholder = tr("bold")) },
        Tool("Italic", TableIcons.Italic, null) { it.wrap("*", placeholder = tr("italic")) },
        Tool("Strikethrough", TableIcons.MdStrike, null) { it.wrap("~~", placeholder = tr("text")) },
        Tool("Heading", TableIcons.MdHeading, null) { v ->
            // # → ## → ### → none.
            val lineStart = v.text.lastIndexOf('\n', (v.selection.min - 1).coerceAtLeast(0)).let { if (v.selection.min == 0 || it < 0) 0 else it + 1 }
            val hashes = v.text.drop(lineStart).takeWhile { it == '#' }.length
            val rest = v.text.substring(lineStart).removePrefix("#".repeat(hashes)).removePrefix(" ")
            val prefix = when (hashes) { 0 -> "# "; 1 -> "## "; 2 -> "### "; else -> "" }
            val t = v.text.substring(0, lineStart) + prefix + rest
            TextFieldValue(t, TextRange((v.selection.max - hashes - (if (hashes > 0) 1 else 0) + prefix.length).coerceIn(0, t.length)))
        },
        Tool("Bullet list", TableIcons.Bullets, null) { it.prefixLines("- ") },
        Tool("Numbered list", TableIcons.Numbering, null) { it.prefixLines("", numbered = true) },
        Tool("Task list", TableIcons.MdTask, null) { it.prefixLines("- [ ] ") },
        Tool("Quote", TableIcons.MdQuote, null) { it.prefixLines("> ") },
        Tool("Code", TableIcons.MdCode, null) { v -> if (v.text.substring(v.selection.min, v.selection.max).contains('\n')) v.wrap("```\n", "\n```") else v.wrap("`", placeholder = "code") },
        Tool("Math", TableIcons.MdMath, null) { it.wrap("$", placeholder = "x^2") },
        Tool("Display math", TableIcons.MdDisplayMath, null) { it.insert("$$ \\int_0^1 x\\,dx $$", block = true) },
        Tool("Link", TableIcons.MdLink, null) { v ->
            val s = v.selection.min; val e = v.selection.max
            val label = if (s == e) tr("link") else v.text.substring(s, e)
            val t = v.text.substring(0, s) + "[" + label + "](https://)" + v.text.substring(e)
            TextFieldValue(t, TextRange(s + label.length + 3, s + label.length + 11))
        },
        Tool("Table", TableIcons.Table, null) { it.insert("| " + tr("Column") + " 1 | " + tr("Column") + " 2 |\n|---|---|\n|   |   |", block = true) },
        Tool("Horizontal rule", TableIcons.MdRule, null) { it.insert("---", block = true) },
    )
    // A scrolling row on a phone; on a tablet's side column, wrapped onto as many rows as it needs.
    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun Holder(content: @Composable () -> Unit) =
        if (wrap) FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
        else Row(modifier.horizontalScroll(rememberScrollState()).padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) { content() }
    Holder {
        tools.forEach { tool ->
            Box(
                Modifier.size(width = 44.dp, height = 40.dp).clip(RoundedCornerShape(12.dp)).background(colors.surfaceContainerHighest).clickable(onClickLabel = tr(tool.label)) { onChange(tool.apply(value)) },
                contentAlignment = Alignment.Center,
            ) {
                if (tool.icon != null) AppIcon(tool.icon, contentDescription = tr(tool.label), tint = colors.onSurface, accent = colors.primary, modifier = Modifier.size(22.dp))
                else Text(tool.text!!, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = colors.onSurface)
            }
        }
    }
}
