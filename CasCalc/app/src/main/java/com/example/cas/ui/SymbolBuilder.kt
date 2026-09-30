package com.example.cas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cas.cas.Accent
import com.example.cas.cas.CustomSymbol
import com.example.cas.editor.MathAlphabets
import com.example.cas.editor.MathRow
import com.example.cas.editor.Sym

/** The alphabets a symbol's letter can come from, in the order they're offered. */
private enum class Alphabet(val label: String, val letters: List<String>) {
    Latin("Latin", ('a'..'z').map { it.toString() } + ('A'..'Z').map { it.toString() }),
    // Every Greek letter, including those shaped like Latin ones (ο, Α, Β…), and the variants.
    Greek("Greek", "αβγδεζηθικλμνξοπρστυφχψω".map { it.toString() } + "ϵϑϰϖϱςϕ".map { it.toString() } + "ΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩ".map { it.toString() }),
    // The mathematicians' ℵ ℶ ℷ ℸ, then the Hebrew alphabet.
    Hebrew("Hebrew", listOf("ℵ", "ℶ", "ℷ", "ℸ") + "אבגדהוזחטיכלמנסעפצקרשת".map { it.toString() }),
    Calligraphic("Calligraphic", ('A'..'Z').map { MathAlphabets.calligraphic(it) }),
    Fraktur("Fraktur", ('A'..'Z').map { MathAlphabets.fraktur(it) } + ('a'..'z').map { MathAlphabets.fraktur(it) }),
    Blackboard("Blackboard", ('A'..'Z').map { MathAlphabets.doubleStruck(it) }),
}

/** What can go in a subscript or superscript, in three groups. */
private val SCRIPT_GROUPS: List<Pair<String, List<String>>> = listOf(
    "Numbers and signs" to ("0123456789".map { it.toString() } + listOf("+", "−", "=", "(", ")", ",", "′", "″", "*", "†", "‡", "∘", "⋆", "⊥", "∥", "±", "∞", "·", "×")),
    "Latin" to (('a'..'z').map { it.toString() } + ('A'..'Z').map { it.toString() }),
    "Greek" to ("αβγδεζηθικλμνξπρστυφχψω".map { it.toString() } + "ΓΔΘΛΞΠΣΦΨΩ".map { it.toString() }),
)

/** The four places a script can go: before or after the letter, below or above. */
private enum class Slot(val label: String) { PreSup("Before, above"), PreSub("Before, below"), Sup("Superscript"), Sub("Subscript") }

/**
 * Builds a symbol with a live preview: a letter from any of six alphabets (bold if you like), an
 * accent (each shown on that letter), and scripts after the letter or before it (²₁H), typed
 * from a pad. Or type the whole symbol in LaTeX. On a tablet the builder is in two columns.
 * Saving puts it at the top of the letters tab and types it. [onDone] gets the symbol, or null.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymbolBuilderPage(onDone: (String?) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tap = rememberKeyTap()
    val tablet = isTabletLayout()
    var base by remember { mutableStateOf("x") }
    var accent by remember { mutableStateOf<Accent?>(null) }
    var bold by remember { mutableStateOf(false) }
    val scripts = remember { androidx.compose.runtime.mutableStateMapOf<Slot, String>() }
    var slot by remember { mutableStateOf(Slot.Sub) }
    var alphabet by remember { mutableStateOf(Alphabet.Latin) }
    var scriptGroup by remember { mutableStateOf(0) }
    fun s(k: Slot) = scripts[k].orEmpty()
    val symbol = CustomSymbol(base, accent, s(Slot.Sub), s(Slot.Sup), s(Slot.PreSub), s(Slot.PreSup), bold)
    // The LaTeX field: follows the builder, and fills it in when what's typed is a symbol.
    var latex by remember { mutableStateOf(symbol.latex) }
    var latexError by remember { mutableStateOf(false) }
    fun changed() { latex = CustomSymbol(base, accent, s(Slot.Sub), s(Slot.Sup), s(Slot.PreSub), s(Slot.PreSup), bold).latex; latexError = false }
    fun typedLatex(t: String) {
        latex = t
        val parsed = CustomSymbol.fromLatex(t)
        latexError = parsed == null
        if (parsed != null) {
            base = parsed.base; accent = parsed.accent; bold = parsed.bold
            scripts[Slot.Sub] = parsed.sub; scripts[Slot.Sup] = parsed.sup; scripts[Slot.PreSub] = parsed.preSub; scripts[Slot.PreSup] = parsed.preSup
            Alphabet.entries.firstOrNull { parsed.base in it.letters }?.let { alphabet = it }
        }
    }

    // The symbol as it will look, its LaTeX to read or type, and bold.
    val preview: @Composable ColumnScope.() -> Unit = {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(colors.surfaceContainerHigh).padding(vertical = if (tablet) 36.dp else 22.dp),
            contentAlignment = Alignment.Center,
        ) {
            MathView(MathRow(mutableListOf(Sym(symbol.encode()))), if (tablet) 96.sp else 72.sp, colors.onSurface, computerModern = true)
        }
        OutlinedTextField(
            value = latex,
            onValueChange = ::typedLatex,
            singleLine = true,
            label = { Text("LaTeX") },
            isError = latexError,
            supportingText = { Text(if (latexError) "Not one symbol yet: e.g. \\hat{x}_{1}, {}^{14}_{6}C, \\mathbb{R}, \\aleph_0" else "Type or paste the symbol in LaTeX, or build it below") },
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    val letter: @Composable ColumnScope.() -> Unit = {
        Step("1", "Letter")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Alphabet.entries.forEach { a -> FilterChip(selected = alphabet == a, onClick = { alphabet = a }, label = { Text(a.label) }) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            alphabet.letters.forEach { l ->
                Chip(selected = l == base, description = "Letter $l", onClick = { tap(); base = l; changed() }) {
                    MathView(MathRow(mutableListOf(Sym(CustomSymbol(l, bold = bold).encode()))), 22.sp, colors.onSurface, computerModern = true)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Bold", style = MaterialTheme.typography.bodyLarge)
                Text("For vectors and matrices, as \\boldsymbol", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Switch(checked = bold, onCheckedChange = { bold = it; changed() })
        }
    }

    val accents: @Composable ColumnScope.() -> Unit = {
        Step("2", "Accent")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(selected = accent == null, description = "No accent", onClick = { tap(); accent = null; changed() }) {
                Text("None", style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
            }
            // Each accent shown on the chosen letter.
            Accent.entries.forEach { a ->
                Chip(selected = accent == a, description = a.label, onClick = { tap(); accent = a; changed() }) {
                    MathView(MathRow(mutableListOf(Sym(CustomSymbol(base, a, bold = bold).encode()))), 24.sp, colors.onSurface, computerModern = true)
                }
            }
        }
    }

    val scriptSection: @Composable ColumnScope.() -> Unit = {
        Step("3", "Scripts")
        Text("Tap a box, then type into it. Scripts can go after the letter, or before it (as in ¹⁴₆C).", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        // The four boxes round the letter, where they'll appear.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScriptSlot(Slot.PreSup.label, s(Slot.PreSup), slot == Slot.PreSup) { slot = Slot.PreSup }
                ScriptSlot(Slot.PreSub.label, s(Slot.PreSub), slot == Slot.PreSub) { slot = Slot.PreSub }
            }
            Box(Modifier.width(64.dp), contentAlignment = Alignment.Center) {
                MathView(MathRow(mutableListOf(Sym(CustomSymbol(base, accent, bold = bold).encode()))), 36.sp, colors.onSurface, computerModern = true)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScriptSlot(Slot.Sup.label, s(Slot.Sup), slot == Slot.Sup) { slot = Slot.Sup }
                ScriptSlot(Slot.Sub.label, s(Slot.Sub), slot == Slot.Sub) { slot = Slot.Sub }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SCRIPT_GROUPS.forEachIndexed { k, (name, _) -> FilterChip(selected = scriptGroup == k, onClick = { scriptGroup = k }, label = { Text(name) }) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SCRIPT_GROUPS[scriptGroup].second.forEach { c ->
                Chip(selected = false, description = "Add $c", onClick = {
                    tap()
                    if (s(slot).length < 8) { scripts[slot] = s(slot) + c; changed() }
                }) {
                    MathView(MathRow(mutableListOf(Sym(c))), 20.sp, colors.onSurface, computerModern = true)
                }
            }
            Chip(selected = false, description = "Delete the last character", onClick = { tap(); scripts[slot] = s(slot).dropLast(1); changed() }) {
                Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = null, tint = colors.onSurface)
            }
            Chip(selected = false, description = "Clear", onClick = { tap(); scripts[slot] = ""; changed() }) {
                Text("Clear", style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
            }
        }
    }

    val buttons: @Composable ColumnScope.() -> Unit = {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
            OutlinedButton(onClick = { onDone(null) }) { Text("Cancel") }
            Button(onClick = { onDone(symbol.encode()) }) { Text("Save and insert") }
        }
    }

    val saved: @Composable ColumnScope.() -> Unit = {
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

    FullScreenPage("Build a symbol", onBack = { onDone(null) }) {
        if (tablet) {
            // Two columns: the symbol and its letter and accent; its scripts and your symbols.
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) { preview(); letter(); accents() }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) { scriptSection(); buttons(); saved() }
            }
        } else {
            preview(); letter(); accents(); scriptSection(); buttons(); saved()
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

/** A script box: tap it to type into it; it shows what's in it. */
@Composable
private fun ScriptSlot(title: String, content: String, active: Boolean, onSelect: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) colors.secondaryContainer else colors.surfaceContainer)
            .then(if (active) Modifier.border(2.dp, colors.primary, RoundedCornerShape(16.dp)) else Modifier)
            .clickable(onClickLabel = "Type into $title", onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = if (active) colors.onSecondaryContainer else colors.onSurfaceVariant, maxLines = 1)
        Box(Modifier.height(28.dp), contentAlignment = Alignment.CenterStart) {
            if (content.isEmpty()) Text("empty", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant.copy(alpha = 0.6f))
            else MathView(MathRow(content.map { Sym(it.toString()) }.toMutableList()), 20.sp, colors.onSurface, computerModern = true)
        }
    }
}
