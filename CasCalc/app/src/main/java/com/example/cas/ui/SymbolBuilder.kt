package com.example.cas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cas.cas.Accent
import com.example.cas.cas.CustomSymbol
import com.example.cas.editor.MathAlphabets
import com.example.cas.editor.MathRow
import com.example.cas.editor.Sym

private val LATIN = ('a'..'z').map { it.toString() } + ('A'..'Z').map { it.toString() }
private val GREEK_LETTERS = listOf("α", "β", "γ", "δ", "ε", "ζ", "η", "θ", "ι", "κ", "λ", "μ", "ν", "ξ", "π", "ρ", "σ", "τ", "υ", "φ", "χ", "ψ", "ω", "Γ", "Δ", "Θ", "Λ", "Ξ", "Π", "Σ", "Φ", "Ψ", "Ω")
private val CALLIGRAPHIC = ('A'..'Z').map { MathAlphabets.calligraphic(it) }
private val FRAKTUR = ('A'..'Z').map { MathAlphabets.fraktur(it) } + ('a'..'z').map { MathAlphabets.fraktur(it) }
/** What can go in a subscript or superscript. */
private val SCRIPT_CHARS = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "+", "−", "′", "*") +
    listOf("a", "b", "c", "i", "j", "k", "l", "m", "n", "p", "q", "r", "s", "t", "x", "y", "z") +
    listOf("α", "β", "γ", "δ", "μ", "ν", "σ", "τ", "T")

/**
 * Builds a symbol in four steps, with a live preview: a letter, an accent (each shown on that
 * letter), then a subscript and a superscript tapped in from a pad. Saving puts it at the top
 * of the letters tab and types it. [onDone] gets the symbol, or null for cancel.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymbolBuilderPage(onDone: (String?) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tap = rememberKeyTap()
    var base by remember { mutableStateOf("x") }
    var accent by remember { mutableStateOf<Accent?>(null) }
    var sub by remember { mutableStateOf("") }
    var sup by remember { mutableStateOf("") }
    var target by remember { mutableStateOf(0) } // 0 subscript, 1 superscript
    var alphabet by remember { mutableStateOf(0) }
    val symbol = CustomSymbol(base, accent, sub, sup)

    FullScreenPage("Build a symbol", onBack = { onDone(null) }) {
        // The symbol as it will look, and its LaTeX.
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(colors.surfaceContainerHigh).padding(vertical = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MathView(MathRow(mutableListOf(Sym(symbol.encode()))), 64.sp, colors.onSurface, computerModern = true)
                Text(symbol.latex, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }

        Step("1", "Letter")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("Latin", "Greek", "Calligraphic", "Fraktur").forEachIndexed { k, name ->
                SegmentedButton(
                    selected = alphabet == k,
                    onClick = { alphabet = k },
                    shape = SegmentedButtonDefaults.itemShape(k, 4),
                    icon = {},
                    label = { Text(name, maxLines = 1, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val letters = when (alphabet) { 1 -> GREEK_LETTERS; 2 -> CALLIGRAPHIC; 3 -> FRAKTUR; else -> LATIN }
            letters.forEach { l ->
                Chip(selected = l == base, description = "Letter $l", onClick = { tap(); base = l }) {
                    MathView(MathRow(mutableListOf(Sym(l))), 22.sp, colors.onSurface, computerModern = true)
                }
            }
        }

        Step("2", "Accent")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(selected = accent == null, description = "No accent", onClick = { tap(); accent = null }) {
                Text("None", style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
            }
            // Each accent shown on the chosen letter.
            Accent.entries.forEach { a ->
                Chip(selected = accent == a, description = a.label, onClick = { tap(); accent = a }) {
                    MathView(MathRow(mutableListOf(Sym(CustomSymbol(base, a).encode()))), 24.sp, colors.onSurface, computerModern = true)
                }
            }
        }

        Step("3", "Subscript and superscript")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ScriptSlot("Subscript", sub, target == 0, Modifier.weight(1f)) { target = 0 }
            ScriptSlot("Superscript", sup, target == 1, Modifier.weight(1f)) { target = 1 }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SCRIPT_CHARS.forEach { c ->
                Chip(selected = false, description = "Add $c", onClick = {
                    tap()
                    if (target == 0) { if (sub.length < 6) sub += c } else { if (sup.length < 6) sup += c }
                }) {
                    MathView(MathRow(mutableListOf(Sym(c))), 20.sp, colors.onSurface, computerModern = true)
                }
            }
            Chip(selected = false, description = "Delete the last character", onClick = {
                tap()
                if (target == 0) sub = sub.dropLast(1) else sup = sup.dropLast(1)
            }) { Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = null, tint = colors.onSurface) }
            Chip(selected = false, description = "Clear", onClick = { tap(); if (target == 0) sub = "" else sup = "" }) {
                Text("Clear", style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
            OutlinedButton(onClick = { onDone(null) }) { Text("Cancel") }
            Button(onClick = { onDone(symbol.encode()) }) { Text("Save and insert") }
        }

        if (SavedSymbols.list.isNotEmpty()) {
            Step("", "Your symbols")
            SavedSymbols.list.toList().forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { MathView(MathRow(mutableListOf(Sym(s))), 28.sp, colors.onSurface, computerModern = true) }
                    Text(CustomSymbol.decode(s)?.latex.orEmpty(), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
                    IconButton(onClick = { SavedSymbols.remove(s) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove this symbol", tint = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun Step(number: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
        if (number.isNotEmpty()) {
            Box(Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary).padding(horizontal = 9.dp, vertical = 2.dp)) {
                Text(number, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Chip(selected: Boolean, description: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) colors.primaryContainer else colors.surfaceContainerHigh)
            .then(if (selected) Modifier.border(2.dp, colors.primary, RoundedCornerShape(14.dp)) else Modifier)
            .clickable(onClickLabel = description, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** A subscript or superscript box: tap it to type into it; it shows what's in it. */
@Composable
private fun ScriptSlot(title: String, content: String, active: Boolean, modifier: Modifier, onSelect: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) colors.secondaryContainer else colors.surfaceContainer)
            .then(if (active) Modifier.border(2.dp, colors.primary, RoundedCornerShape(16.dp)) else Modifier)
            .clickable(onClickLabel = "Type into the $title", onClick = onSelect)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = if (active) colors.onSecondaryContainer else colors.onSurfaceVariant)
        Box(Modifier.height(32.dp), contentAlignment = Alignment.CenterStart) {
            if (content.isEmpty()) Text("empty", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant.copy(alpha = 0.6f))
            else MathView(MathRow(content.map { Sym(it.toString()) }.toMutableList()), 22.sp, colors.onSurface, computerModern = true)
        }
    }
}
