package com.example.cas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cas.engine.LatexParser

/**
 * Text with inline math in Markdown style, $ … $ (or \( … \)): words flow and wrap,
 * and the math is drawn by the calculator's own renderer in Computer Modern.
 * Text without any math is a plain Text.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MathText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    val ink = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }
    val parts = remember(text) { LatexParser.inline(text) }
    if (parts.none { it.first }) {
        Text(parts.joinToString("") { it.second }, modifier, color = ink, style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
        return
    }
    FlowRow(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        var k = 0
        while (k < parts.size) {
            val (isMaths, piece) = parts[k]
            if (isMaths) {
                // Punctuation straight after the math stays on its line: "$x^2$," never wraps before the comma.
                val next = parts.getOrNull(k + 1)?.takeIf { !it.first && it.second.isNotEmpty() && !it.second[0].isWhitespace() }
                val glued = next?.second?.substringBefore(' ')
                Row(Modifier.align(Alignment.CenterVertically), verticalAlignment = Alignment.CenterVertically) {
                    val math = remember(piece) { LatexParser.parse(piece) }
                    MathView(math, style.fontSize * 1.1f, ink)
                    if (glued != null) Text(glued + if (next.second.length > glued.length) " " else "", style = style, color = ink)
                }
                if (next != null) {
                    val rest = next.second.substring(glued!!.length).trimStart(' ')
                    if (rest.isNotEmpty()) Words(rest, leadSpace = false, style, ink)
                    k += 2
                } else k++
            } else {
                Words(piece, leadSpace = k > 0 && piece.startsWith(" "), style, ink)
                k++
            }
        }
    }
}

/** One Text per word so lines can wrap between words. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun androidx.compose.foundation.layout.FlowRowScope.Words(piece: String, leadSpace: Boolean, style: TextStyle, color: Color) {
    val words = piece.split(" ")
    val firstWord = words.indexOfFirst { it.isNotEmpty() }
    words.forEachIndexed { k, word ->
        if (word.isNotEmpty()) {
            // Keep the space between math and the word after it ("… $x$ here").
            val lead = if (k == firstWord && leadSpace) " " else ""
            Text(
                lead + if (k < words.lastIndex || piece.endsWith(" ")) "$word " else word,
                style = style,
                color = color,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
}
