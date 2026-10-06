package com.example.cas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Settings › Calculator tabs: which groups of keys sit above the keypad, and in what order.
 * The ones on the keypad are listed first (drag the handle to reorder, switch one off to put it
 * away); below them, every tab that isn't, the extra ones among them, each with Add.
 */
@Composable
fun CalcTabsPage(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val order = CalcTabs.order()
    val shown = order.filter { CalcTabs.isShown(it) }
    val hidden = order.filter { !CalcTabs.isShown(it) }
    fun commit(newShown: List<Int>, newHidden: List<Int>) =
        AppSettings.changeTabs((newShown + newHidden).map { FunctionTabs[it].title }, newShown.map { FunctionTabs[it].title }.toSet())

    FullScreenPage("Calculator tabs", onBack = onBack) {
        Text(
            tr("The groups of keys above the keypad. Drag a tab to reorder it, switch it off to put it away, and add more from below."),
            style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
        )
        // The tab bar as it will look on the keypad.
        TabBarPreview(shown)

        SettingsSection("On the keypad")
        ReorderableColumn(shown, key = { it }, onMove = { from, to ->
            // Read afresh: several moves can come before the page is drawn again.
            val now = CalcTabs.order()
            val s = now.filter { CalcTabs.isShown(it) }.toMutableList(); s.add(to, s.removeAt(from)); commit(s, now.filter { !CalcTabs.isShown(it) })
        }) { i, handle ->
            TabRow(i, handle) {
                // The last tab can't go: the keypad always has one.
                Switch(
                    checked = true, enabled = shown.size > 1,
                    onCheckedChange = { commit(shown - i, listOf(i) + hidden) },
                    modifier = Modifier.semantics { contentDescription = tr("Show {0}", tr(FunctionTabs[i].title)) },
                )
            }
        }

        SettingsSection("More tabs")
        if (hidden.isEmpty()) Text(tr("Every tab is on the keypad."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            hidden.forEach { i ->
                TabRow(i, handle = null) {
                    FilledTonalButton(onClick = { commit(shown + i, hidden - i) }, modifier = Modifier.height(40.dp)) {
                        AppIcon(TableIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Add"))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { AppSettings.resetTabs() }) { Text(tr("Reset to the default tabs")) }
        }
    }
}

/** A tab in the list: its handle (on the keypad only), icon, name and what's on it, then [end]. */
@Composable
private fun TabRow(i: Int, handle: Modifier?, end: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tab = FunctionTabs[i]
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerHigh)
            .padding(start = if (handle != null) 0.dp else 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (handle != null) AppIcon(TableIcons.Grip, contentDescription = tr("Drag to reorder"), tint = colors.onSurfaceVariant, modifier = handle.size(44.dp).padding(10.dp))
        Box(Modifier.size(40.dp).clip(CircleShape).background(colors.secondaryContainer), contentAlignment = Alignment.Center) {
            LabelView(tab.icon, colors.onSecondaryContainer, fontSize = 16f, iconSize = 22.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr(tab.title), style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            }
            if (tab.about.isNotEmpty()) MathText(tr(tab.about), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(8.dp))
        end()
    }
}

/** The tab bar as the keypad will show it, the first tab picked. */
@Composable
private fun TabBarPreview(shown: List<Int>) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.surfaceContainerLow).padding(8.dp)
            .semantics { contentDescription = tr("The keypad's tabs") },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        shown.forEachIndexed { pos, i ->
            val selected = pos == 0
            val outer = 18.dp; val inner = 7.dp
            val shape = when {
                selected -> RoundedCornerShape(outer)
                pos == shown.lastIndex -> RoundedCornerShape(topStart = inner, bottomStart = inner, topEnd = outer, bottomEnd = outer)
                else -> RoundedCornerShape(inner)
            }
            Box(
                Modifier.weight(1f).height(36.dp).clip(shape).background(if (selected) colors.primary else colors.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) { LabelView(FunctionTabs[i].icon, if (selected) colors.onPrimary else colors.onSurfaceVariant, fontSize = 15f, iconSize = 18.dp) }
        }
    }
}
