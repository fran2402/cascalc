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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cas.editor.Sym
import com.example.cas.editor.row

/**
 * Which letter to solve for, when an equation has several (y = 2x + 1: x or y?). The equation
 * shows at the top; each letter is a tile. One equation: a tap on a letter solves for it. Several
 * equations with more letters than equations: pick as many letters as there are equations, then
 * Solve.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SolveForDialog(choice: CalculatorViewModel.SolveChoice, onSolve: (List<String>) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val picked = remember(choice) { mutableStateListOf<String>() }
    val one = choice.count == 1
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.widthIn(max = 440.dp), shape = RoundedCornerShape(28.dp), color = colors.surfaceContainerHigh) {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp)) {
                Text(tr("Solve for"), style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
                // The equation, as typed.
                Box(
                    Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerHighest)
                        .horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { MathView(choice.row, 22.sp, colors.onSurface) }
                Text(
                    if (one) tr("It has several letters. Which one should it be solved for?")
                    else tr("Choose ${choice.count} letters to solve for, one for each equation; the others stay as they are."),
                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp),
                )
                FlowRow(Modifier.padding(top = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    choice.candidates.forEach { v ->
                        val on = v in picked
                        Box(
                            Modifier.size(64.dp).clip(RoundedCornerShape(if (on) 32.dp else 18.dp))
                                .background(if (on) colors.primary else colors.secondaryContainer)
                                .border(if (on) 0.dp else 1.dp, colors.outlineVariant, RoundedCornerShape(18.dp))
                                .clickable(role = if (one) Role.Button else Role.Checkbox) {
                                    when {
                                        one -> onSolve(listOf(v))
                                        on -> picked.remove(v)
                                        picked.size < choice.count -> picked += v
                                    }
                                }
                                .semantics { contentDescription = "Solve for $v"; if (!one) selected = on },
                            contentAlignment = Alignment.Center,
                        ) { MathView(row(Sym(v)), 28.sp, if (on) colors.onPrimary else colors.onSecondaryContainer) }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (!one) Text("${picked.size} of ${choice.count}", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(tr("Cancel")) }
                    if (!one) TextButton(onClick = { onSolve(choice.candidates.filter { it in picked }) }, enabled = picked.size == choice.count) { Text(tr("Solve")) }
                }
            }
        }
    }
}
