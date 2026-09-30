package com.example.cas.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * A full-screen page over the app (settings, acknowledgements): a back arrow and title at
 * the top, the content scrolling underneath. Back (the arrow or the system gesture) closes it.
 */
@Composable
fun FullScreenPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BackHandler(onBack = onBack)
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 4.dp))
                }
                Column(
                    Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    content = content,
                )
            }
        }
    }
}

/** A part of a [SectionedPage]: its title and icon in the list, and what it shows. */
class PageSection(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val content: @Composable ColumnScope.() -> Unit)

/**
 * A full-screen page in sections (settings, acknowledgements). On a phone it's one scrolling
 * column, each section under its heading. On a tablet it's two panes, as Android's own
 * settings are: the sections listed on the left, the chosen one on the right in a card of a
 * comfortable reading width.
 */
@Composable
fun SectionedPage(title: String, sections: List<PageSection>, onBack: () -> Unit, intro: String? = null) {
    if (!isTabletLayout()) {
        FullScreenPage(title, onBack = onBack) {
            intro?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            sections.forEach { s ->
                SettingsSection(s.title)
                s.content(this)
            }
        }
        return
    }
    val colors = MaterialTheme.colorScheme
    var chosen by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
    Dialog(onDismissRequest = onBack, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler(onBack = onBack)
        Surface(Modifier.fillMaxSize(), color = colors.surfaceContainerLow) {
            Row(Modifier.fillMaxSize().safeDrawingPadding()) {
                // The sections, as a list down the left.
                Column(
                    Modifier.width(300.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.onSurface) }
                        Text(title, style = MaterialTheme.typography.headlineSmall, color = colors.onSurface, maxLines = 1, modifier = Modifier.padding(start = 4.dp))
                    }
                    sections.forEachIndexed { i, s ->
                        val on = i == chosen
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(if (on) colors.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable(onClickLabel = "Show ${s.title}") { chosen = i }
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(s.icon, contentDescription = null, tint = if (on) colors.onSecondaryContainer else colors.onSurfaceVariant)
                            Spacer(Modifier.width(14.dp))
                            Text(s.title, style = MaterialTheme.typography.titleMedium, color = if (on) colors.onSecondaryContainer else colors.onSurface)
                        }
                    }
                }
                // The chosen section, in a card.
                val s = sections[chosen.coerceIn(0, sections.lastIndex)]
                Box(Modifier.weight(1f).fillMaxHeight().padding(top = 12.dp, end = 16.dp, bottom = 16.dp)) {
                    // A fresh scroll for each section, starting at its top.
                    androidx.compose.runtime.key(chosen) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(28.dp))
                            .background(colors.surface)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 32.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(s.title, style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
                            if (chosen == 0) intro?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
                            s.content(this)
                        }
                    }
                    }
                }
            }
        }
    }
}
