package com.example.cas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cas.cas.Expr
import com.example.cas.editor.MathRow
import com.example.cas.engine.Formatter

/**
 * Use, on an answer with several values (x = −2, x = 2; eigenvalues; a system's solutions):
 * which one goes into the next line. Each value is a tile showing what it is (x = 2); a tap puts
 * in the value itself (2), as a decimal when the answer is shown as one. "All of them" puts in
 * the whole answer, as before.
 */
@Composable
fun UseChoiceDialog(choices: List<Pair<Expr, Expr>>, decimal: Boolean, onUse: (MathRow) -> Unit, onUseAll: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    fun rowOf(e: Expr): MathRow = Formatter.answer(e).let { a -> if (decimal && a.approx != null) a.approx else a.exact }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.widthIn(max = 440.dp), shape = RoundedCornerShape(28.dp), color = colors.surfaceContainerHigh) {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp)) {
                Text(tr("Use which value?"), style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
                Text(
                    tr("The answer has {0} values. Tap one to put it in the next line.", choices.size),
                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp),
                )
                Column(
                    Modifier.padding(top = 16.dp).heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    choices.forEach { (label, value) ->
                        val shown = rowOf(label)
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.secondaryContainer)
                                .clickable(onClickLabel = tr("Use this value")) { onUse(rowOf(value)) }
                                .semantics { contentDescription = "Use " + Formatter.plain(shown) }
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) { MathView(shown, 22.sp, colors.onSecondaryContainer) }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onUseAll) { Text(tr("All of them")) }
                    TextButton(onClick = onDismiss) { Text(tr("Cancel")) }
                }
            }
        }
    }
}
