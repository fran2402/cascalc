package com.example.cas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ChangeHistory
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.IconButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import com.example.cas.ui.theme.CasFonts
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Lens
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A chapter's icon, by its key in [Docs]. */
private fun docsIcon(key: String): ImageVector = when (key) {
    "start" -> Icons.Outlined.PlayCircle
    "numbers" -> TableIcons.ModeCalculator
    "exp" -> Icons.AutoMirrored.Outlined.TrendingUp
    "trig" -> Icons.Outlined.ChangeHistory
    "complex", "complexplane" -> Icons.Outlined.Lens
    "algebra" -> Icons.Outlined.Functions
    "calculus" -> Icons.Outlined.Timeline
    "vector" -> Icons.Outlined.OpenWith
    "matrix" -> Icons.Outlined.GridOn
    "stats" -> Icons.Outlined.BarChart
    "special" -> Icons.Outlined.AutoAwesome
    "contour" -> Icons.Outlined.Loop
    "units" -> Icons.Outlined.Science
    "history" -> TableIcons.History
    "graph2d", "fit" -> TableIcons.Mode2D
    "graph3d" -> TableIcons.Mode3D
    "table", "sheet" -> TableIcons.Table
    "converter" -> Icons.Outlined.SwapHoriz
    "files" -> TableIcons.FolderOpen
    "keys" -> TableIcons.Keyboard
    "geometry" -> PlotIcons.Geometry
    else -> Icons.Outlined.Description
}

/**
 * The documentation, from Settings: a contents page (the chapters by part, with search), then
 * one chapter at a time, its sections numbered with "on this page" links, worked examples as
 * tables, notes and formulas, and previous and next at the bottom. On a tablet the chapters
 * stay listed on the left.
 */
@Composable
fun DocsPage(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tablet = isTabletLayout()
    var chapter by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<Int?>(if (tablet) 0 else null) }
    var jumpTo by remember { mutableStateOf<Int?>(null) }
    val back = { if (!tablet && chapter != null) chapter = null else onBack() }
    androidx.compose.ui.window.Dialog(onDismissRequest = back, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        androidx.activity.compose.BackHandler(onBack = back)
        androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = if (tablet) colors.surfaceContainerLow else colors.surface) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Back"), tint = colors.onSurface) }
                    Text(
                        if (!tablet && chapter != null) Docs.chapters[chapter!!].title else "Documentation",
                        style = MaterialTheme.typography.titleLarge, color = colors.onSurface, maxLines = 1,
                        modifier = Modifier.padding(start = 4.dp).weight(1f),
                    )
                }
                if (tablet) Row(Modifier.weight(1f).fillMaxWidth()) {
                    DocsNav(chapter ?: 0, onChapter = { chapter = it; jumpTo = null }, onSection = { c, s -> chapter = c; jumpTo = s }, modifier = Modifier.width(320.dp).fillMaxHeight())
                    Box(Modifier.weight(1f).fillMaxHeight().padding(end = 16.dp, bottom = 16.dp).clip(RoundedCornerShape(28.dp)).background(colors.surface), contentAlignment = Alignment.TopCenter) {
                        androidx.compose.runtime.key(chapter) {
                            DocsChapter(chapter ?: 0, jumpTo, onChapter = { chapter = it; jumpTo = null }, modifier = Modifier.widthIn(max = 760.dp))
                        }
                    }
                } else if (chapter == null) {
                    DocsContents(onChapter = { chapter = it; jumpTo = null }, onSection = { c, s -> chapter = c; jumpTo = s }, modifier = Modifier.weight(1f))
                } else {
                    androidx.compose.runtime.key(chapter) {
                        DocsChapter(chapter!!, jumpTo, onChapter = { chapter = it; jumpTo = null }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Chapters and sections whose titles or text match [query], as (chapter, section) pairs. */
private fun docsSearch(query: String): List<Pair<Int, Int>> {
    val q = query.trim()
    if (q.isEmpty()) return emptyList()
    fun Docs.Block.text(): String = when (this) {
        is Docs.Block.Para -> text
        is Docs.Block.Note -> text
        is Docs.Block.Bullets -> items.joinToString(" ")
        is Docs.Block.Table -> rows.joinToString(" ") { it.first + " " + it.second }
        is Docs.Block.Examples -> examples.joinToString(" ") { it.note + " " + com.example.cas.engine.Formatter.plain(it.row) }
        is Docs.Block.Keys -> groups.flatMap { g -> KeyHelps.groups.firstOrNull { it.first == g }?.second.orEmpty() }.joinToString(" ") { k -> KeyHelps.of(k).let { it.title + " " + it.about + " " + it.usage } }
        else -> ""
    }
    return Docs.chapters.flatMapIndexed { c, ch ->
        ch.sections.mapIndexedNotNull { s, sec ->
            if ((ch.title + " " + sec.title + " " + sec.blocks.joinToString(" ") { it.text() }).contains(q, ignoreCase = true)) c to s else null
        }
    }
}

/** The contents: a search box, then each part's chapters as numbered cards. */
@Composable
private fun DocsContents(onChapter: (Int) -> Unit, onSection: (Int, Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    val hits = remember(query) { docsSearch(query) }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Everything CAS Calculator does, with ${Docs.allExamples.size} worked examples. Every answer here was worked out by the calculator itself.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            placeholder = { Text(tr("Search the documentation")) },
            leadingIcon = { AppIcon(TableIcons.Search, contentDescription = null) },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        if (query.isNotBlank()) {
            if (hits.isEmpty()) Text(tr("Nothing matches"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(16.dp))
            hits.forEach { (c, s) ->
                val ch = Docs.chapters[c]
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainer).clickable { onSection(c, s) }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(docsIcon(ch.icon), contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${c + 1}.${s + 1}  ${ch.sections[s].title}", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                        Text(ch.title, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
            }
            return@Column
        }
        Docs.chapters.withIndex().groupBy { it.value.part }.forEach { (part, list) ->
            Text(part, style = MaterialTheme.typography.titleSmall, color = colors.primary, modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp))
            list.forEachIndexed { k, (c, ch) ->
                val first = k == 0; val last = k == list.lastIndex
                val shape = RoundedCornerShape(topStart = if (first) 24.dp else 6.dp, topEnd = if (first) 24.dp else 6.dp, bottomStart = if (last) 24.dp else 6.dp, bottomEnd = if (last) 24.dp else 6.dp)
                Row(
                    Modifier.fillMaxWidth().clip(shape).background(colors.surfaceContainer).clickable(onClickLabel = "Open ${ch.title}") { onChapter(c) }.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(colors.secondaryContainer), contentAlignment = Alignment.Center) {
                        Icon(docsIcon(ch.icon), contentDescription = null, tint = colors.onSecondaryContainer)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${c + 1}. ${ch.title}", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                        MathText(ch.summary, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** The tablet's navigation: every chapter by part, the open one with its sections under it. */
@Composable
private fun DocsNav(chapter: Int, onChapter: (Int) -> Unit, onSection: (Int, Int) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    val hits = remember(query) { docsSearch(query) }
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            placeholder = { Text(tr("Search")) },
            leadingIcon = { AppIcon(TableIcons.Search, contentDescription = null) },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        if (query.isNotBlank()) {
            hits.forEach { (c, s) ->
                Text(
                    "${c + 1}.${s + 1}  ${Docs.chapters[c].sections[s].title}", style = MaterialTheme.typography.bodyMedium, color = colors.onSurface,
                    modifier = Modifier.fillMaxWidth().clip(CircleShape).clickable { onSection(c, s) }.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
            return@Column
        }
        Docs.chapters.withIndex().groupBy { it.value.part }.forEach { (part, list) ->
            Text(part, style = MaterialTheme.typography.labelLarge, color = colors.primary, modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp))
            list.forEach { (c, ch) ->
                val on = c == chapter
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(CircleShape).background(if (on) colors.secondaryContainer else Color.Transparent).clickable { onChapter(c) }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(docsIcon(ch.icon), contentDescription = null, tint = if (on) colors.onSecondaryContainer else colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("${c + 1}. ${ch.title}", style = MaterialTheme.typography.titleSmall, color = if (on) colors.onSecondaryContainer else colors.onSurface, maxLines = 1)
                }
                if (on) ch.sections.forEachIndexed { s, sec ->
                    Text(
                        "${c + 1}.${s + 1}  ${sec.title}", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1,
                        modifier = Modifier.fillMaxWidth().clip(CircleShape).clickable { onSection(c, s) }.padding(start = 48.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** One chapter: its title, "on this page", each numbered section, then previous and next. */
@Composable
private fun DocsChapter(index: Int, jumpTo: Int?, onChapter: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val ch = Docs.chapters[index]
    val list = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Items: the header (0), then one per section.
    androidx.compose.runtime.LaunchedEffect(jumpTo) { jumpTo?.let { list.scrollToItem(it + 1) } }
    androidx.compose.foundation.lazy.LazyColumn(state = list, modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                        Icon(docsIcon(ch.icon), contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("${ch.part} · Chapter ${index + 1}", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                        Text(ch.title, style = MaterialTheme.typography.headlineMedium, color = colors.onSurface)
                    }
                }
                MathText(ch.summary, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                // On this page: the sections as chips.
                if (ch.sections.size > 1) {
                    Text(tr("On this page"), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ch.sections.forEachIndexed { s, sec ->
                            Text(
                                "${index + 1}.${s + 1} ${sec.title}", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant,
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp))
                                    .clickable { scope.launch { list.animateScrollToItem(s + 1) } }.padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
                androidx.compose.material3.HorizontalDivider(Modifier.padding(top = 8.dp), color = colors.outlineVariant)
            }
        }
        ch.sections.forEachIndexed { s, sec ->
            item(key = "s$s") {
                Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${index + 1}.${s + 1}", style = MaterialTheme.typography.titleLarge, color = colors.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(sec.title, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                    }
                    sec.blocks.forEach { DocsBlock(it) }
                }
            }
        }
        // Previous and next chapters.
        item {
            Row(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (index > 0) DocsPager("Previous", Docs.chapters[index - 1].title, alignEnd = false, modifier = Modifier.weight(1f)) { onChapter(index - 1) }
                else Spacer(Modifier.weight(1f))
                if (index < Docs.chapters.lastIndex) DocsPager("Next", Docs.chapters[index + 1].title, alignEnd = true, modifier = Modifier.weight(1f)) { onChapter(index + 1) }
                else Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DocsPager(label: String, title: String, alignEnd: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Text(if (alignEnd) "$label ›" else "‹ $label", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        Text(tr(title), style = MaterialTheme.typography.titleSmall, color = colors.primary, maxLines = 1)
    }
}

/** One block of a section. */
@Composable
private fun DocsBlock(b: Docs.Block) {
    val colors = MaterialTheme.colorScheme
    when (b) {
        is Docs.Block.Para -> MathText(b.text, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
        is Docs.Block.Note -> Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (b.warning) colors.errorContainer.copy(alpha = 0.6f) else colors.secondaryContainer.copy(alpha = 0.7f)).padding(14.dp),
        ) {
            Icon(if (b.warning) Icons.Outlined.WarningAmber else TableIcons.Tip, contentDescription = if (b.warning) "Note" else "Tip", tint = if (b.warning) colors.onErrorContainer else colors.onSecondaryContainer, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            MathText(b.text, style = MaterialTheme.typography.bodyMedium, color = if (b.warning) colors.onErrorContainer else colors.onSecondaryContainer)
        }
        is Docs.Block.Formula -> Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainer).horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) { MathText("\$${b.latex}\$", style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 20.sp), color = colors.onSurface, mathScale = 1f) }
        is Docs.Block.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            b.items.forEach { t ->
                Row {
                    Box(Modifier.padding(top = 9.dp, end = 12.dp).size(6.dp).clip(CircleShape).background(colors.primary))
                    MathText(t, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                }
            }
        }
        is Docs.Block.Examples -> DocsExamples(b.examples)
        is Docs.Block.Table -> Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(16.dp))) {
            b.rows.forEachIndexed { k, (a, d) ->
                if (k > 0) androidx.compose.material3.HorizontalDivider(color = colors.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(0.42f).horizontalScroll(rememberScrollState())) { MathText(a, style = MaterialTheme.typography.titleSmall, color = colors.onSurface) }
                    Spacer(Modifier.width(12.dp))
                    MathText(d, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.weight(0.58f))
                }
            }
        }
        is Docs.Block.Keys -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            b.groups.flatMap { g -> KeyHelps.groups.firstOrNull { it.first == g }?.second.orEmpty() }.forEach { key ->
                val help = KeyHelps.of(key)
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surfaceContainer).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(help.title, style = MaterialTheme.typography.titleSmall, color = colors.primary)
                    if (help.formula.isNotEmpty()) {
                        val lines = remember(help.formula) { com.example.cas.engine.LatexParser.lines(help.formula) }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                            lines.forEach { MathView(it, 19.sp, colors.onSurface) }
                        }
                    }
                    if (help.about.isNotEmpty()) MathText(help.about, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                    if (help.usage.isNotEmpty()) MathText(help.usage, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
        Docs.Block.SheetFunctions -> FunctionReference(onPick = null)
    }
}

/**
 * Worked examples as a table: what's typed (as the keys type it) and the answer the calculator
 * gives, with the decimal when there is one, and a note under each.
 */
@Composable
private fun DocsExamples(examples: List<Docs.Example>) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(20.dp))) {
        Row(Modifier.fillMaxWidth().background(colors.surfaceContainerHigh).padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text(tr("You type"), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(tr("Answer"), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
        }
        examples.forEachIndexed { k, e ->
            if (k > 0) androidx.compose.material3.HorizontalDivider(color = colors.outlineVariant)
            val answer = DocsResults.of(e)
            Column(Modifier.fillMaxWidth().background(if (k % 2 == 1) colors.surfaceContainerLowest.copy(alpha = 0.5f) else Color.Transparent).padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) { MathView(e.row, 18.sp, colors.onSurface) }
                if (answer != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState())) {
                        MathText("\$= ${answer.first}\$" + (answer.second?.takeIf { it != answer.first }?.let { "  \$\\approx $it\$" } ?: ""), style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 18.sp), color = colors.primary, mathScale = 1f, maxLines = 1)
                    }
                }
                if (e.note.isNotEmpty() || e.degrees) MathText(
                    listOfNotNull(e.note.takeIf { it.isNotEmpty() }, if (e.degrees && "degree" !in e.note) "In degrees" else null).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The spreadsheet functions, searchable and by group, each with how it's written and what it
 * does. With [onPick], tapping one picks its name.
 */
@Composable
fun FunctionReference(onPick: ((String) -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    var group by remember { mutableStateOf<String?>(null) }
    val all = com.example.cas.graph.Sheet.FUNCTIONS
    val shown = all.filter { h ->
        (group == null || h.category == group) &&
            (query.isBlank() || listOf(h.names, h.example, h.what).any { it.contains(query.trim(), ignoreCase = true) })
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(tr("Start a cell with =. Cells are A1, B2…; ranges A1:A10 or whole columns A:A; \$A\$1 stays put when filled down. Operators + − * / ^ % & and comparisons = <> < > <= >=. Text goes in \"quotes\"."),
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp),
        )
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            placeholder = { Text(tr("Search, e.g. lookup, NORM, date")) },
            leadingIcon = { AppIcon(TableIcons.Search, contentDescription = null) },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        // The groups as chips; the chosen one is filled, so no check mark is needed.
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (listOf<String?>(null) + com.example.cas.graph.Sheet.CATEGORIES).forEach { c ->
                FilterChip(selected = group == c, onClick = { group = c }, label = { Text(c ?: "All") })
            }
        }
        if (shown.isEmpty()) Text(tr("No functions match"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
        shown.forEachIndexed { k, h ->
            if (group == null && (k == 0 || shown[k - 1].category != h.category)) Text(
                h.category, style = MaterialTheme.typography.labelLarge, color = colors.primary,
                modifier = Modifier.padding(top = if (k == 0) 0.dp else 10.dp, bottom = 2.dp, start = 4.dp),
            )
            val name = h.example.substringBefore('(')
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainerHigh)
                    .then(if (onPick != null) Modifier.clickable(onClickLabel = "Use $name") { onPick(name) } else Modifier)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(h.names, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                Text("=${h.example}", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = colors.primary))
                Text(h.what, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}
