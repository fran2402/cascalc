package com.example.cas.ui

import androidx.compose.foundation.background
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

/**
 * How to use the app, for Settings: each mode with examples to look at and its gestures and
 * tips, then the keys, history, data tables, fitting, spreadsheet formulas, the unit converter
 * and files, and every spreadsheet function with search.
 */
object HowTo {
    class Topic(
        val title: String,
        val icon: ImageVector,
        val intro: String,
        val tips: List<ModeGuides.Tip>,
        val examples: List<ModeGuides.Example> = emptyList(),
    )

    private fun t(icon: String, title: String, text: String) = ModeGuides.Tip(icon, title, text)

    val topics: List<Topic> = listOf(
        ModeGuides.calculator.let { g -> Topic(g.title, Icons.Outlined.Calculate, g.tagline, g.gestures + g.tips, g.examples) },
        Topic(
            "Keys and typing", Icons.Outlined.Keyboard,
            "The keys type math the way it's written: fractions stack, powers rise, and the cursor moves through them.",
            listOf(
                t("hold", "Hold any key", "What it does, the formula behind it and an example"),
                t("swipe", "Swipe the function keys", "The next group: trigonometry, logs, calculus, matrices, constants…"),
                t("tap", "The arrows", "Move the cursor; at the edge of a fraction or a power they step out of it"),
                t("tap", "Delete", "Deletes back; in an empty part of a fraction it takes the fraction away and keeps the rest"),
                t("units", "The ⚛ group", "Physical constants with their units, in SI or another system"),
                t("folder", "Variables and functions", "\$a = 3\$ stores a number; \$f(x) = x^2\$ defines a function to use anywhere"),
                t("tune", "Symbol builder", "Accents, sub- and superscripts and Greek letters for your own symbols, kept for next time"),
            ),
        ),
        Topic(
            "History", Icons.Outlined.History,
            "Every calculation is kept, newest at the bottom; open the history for all of them, sorted by day.",
            listOf(
                t("tap", "Tap a question or an answer", "Puts it at the cursor to use again; hold it to copy"),
                t("tap", "Tap a card", "Its actions: Graph (or Use), Steps, copy, pin, and ⋮ for folders, sharing and delete"),
                t("swipe", "Swipe a card", "Deletes it (Settings can ask first)"),
                t("folder", "Pins and folders", "A pinned calculation stays whatever the history limit; folders group them, and their chips filter the list"),
                t("export", "Search and export", "Search by what you typed or the answer; export as LaTeX or a PDF"),
            ),
        ),
        ModeGuides.graph2D.let { g -> Topic(g.title, Icons.AutoMirrored.Outlined.ShowChart, g.tagline, g.gestures + g.tips, g.examples) },
        ModeGuides.graph3D.let { g -> Topic(g.title, Icons.Outlined.ViewInAr, g.tagline, g.gestures + g.tips, g.examples) },
        ModeGuides.complex.let { g -> Topic(g.title, Icons.Outlined.Lens, g.tagline, g.gestures + g.tips, g.examples) },
        Topic(
            "Data tables", Icons.Outlined.TableChart,
            "Points can come from a table: typed, pasted from a spreadsheet, or imported from a CSV file. In the 2D graph, ＋ › Table adds one; tap a table's line to open it.",
            listOf(
                t("table", "Columns and roles", "Tap a column's card for its role: \$x\$, \$y\$, \$\\sigma(x)\$ or \$\\sigma(y)\$ (error bars), or not used. Without an \$x\$, rows are numbered 1, 2, 3…"),
                t("tap", "Typing", "Tap a cell to type in it; Next moves down the column, adding a row at the end"),
                t("tap", "The ＋ button", "Bottom left: a row, a column, or a table pasted from the clipboard (CSV or tab-separated)"),
                t("hold", "Rows", "Tap a row's number to insert a row above or below, or remove it"),
                t("drag", "Resizing", "Drag the grip between two column cards, or under a row's number; double-tap it for the usual size"),
                t("swap", "Sort, fill and clear", "In a column's menu: sort the rows by it, fill it with 1, 2, 3…, or clear it"),
            ),
        ),
        Topic(
            "Fitting", Icons.Outlined.Timeline,
            "Fit any formula with unknowns to the points in the 2D graph, by least squares.",
            listOf(
                t("graph", "Type a model", "A line with letters other than \$x\$, like \$y = ax + b\$ or \$y = A e^{-x/\\tau}\$: each gets a slider"),
                t("tap", "Tap Fit", "Under the line, once there are points: the sliders move to the best fit"),
                t("hold", "Hold Fit", "The statistics: each parameter ± its standard error, \$R^2\$, RMSE, the reduced \$\\chi^2\$ and \$\\nu\$, with the curve over the points and the residuals"),
                t("table", "Error bars", "With a \$\\sigma(y)\$ column, the fit is weighted by it and \$\\chi^2/\\nu\$ says how well the uncertainties match the scatter"),
            ),
        ),
        Topic(
            "Spreadsheet formulas", Icons.Outlined.Functions,
            "Beta, turned on under Calculator. Data tables then work like a spreadsheet, with over 370 of Excel's functions.",
            listOf(
                t("table", "Formulas", "A cell starting with = is worked out: =B1*2, =SUM(A:A), =IF(A1>0, A1, 0). It shows its value until you tap it"),
                t("tune", "References", "Columns are lettered A, B, C…; A1 is a cell, A1:B10 a range, A:A a whole column, and \$A\$1 stays put when copied"),
                t("swap", "Fill down", "A column's menu copies its first formula to every row below, its references moving with it"),
                t("graph", "Plot what's worked out", "A formula column can be \$x\$, \$y\$ or an error bar like any other"),
                t("hold", "Errors", "#DIV/0!, #VALUE!, #REF!, #NAME?, #NUM!, #N/A and #CYCLE! (a formula that uses itself), edged in red"),
            ),
        ),
        Topic(
            "Unit converter", Icons.Outlined.Science,
            "Beta, turned on under Calculator, then in the ⋮ menu: any units, however they're combined.",
            listOf(
                t("units", "Type or pick", "Units like km/s/Mpc, erg, lea/Å; the picker has every unit with its prefixes"),
                t("tap", "× 10ⁿ and ±", "Beside the value, for keyboards without e or a minus: 3 × 10⁸ is typed as 3, × 10ⁿ, 8"),
                t("swap", "Physical equivalences", "\$c\$, \$h\$, \$\\hbar\$ and \$k_B\$ can connect mass, energy, frequency, wavelength and temperature"),
            ),
        ),
        Topic(
            "Files and export", Icons.Outlined.FolderOpen,
            "Graphs are saved as you go; save them as files to keep or share.",
            listOf(
                t("folder", "Saved graphs", "The file button lists your graphs; save, rename, duplicate and open them"),
                t("export", "Graph files", ".g2d, .g3d and .gcp files open from a file manager, a download or a message: choose CAS Calculator in Open with"),
                t("export", "Export", "PNG, SVG or PDF, pgfplots-style with LaTeX fonts, or STL for 3D printing a surface"),
            ),
        ),
    )
}

/** The guide, in Settings' place on a phone: one topic after another, or listed on the left on a tablet. */
@Composable
fun HowToUsePage(onBack: () -> Unit) {
    SectionedPage(
        "How to use",
        onBack = onBack,
        intro = "What each part of CAS Calculator does, with examples and gestures.",
        sections = HowTo.topics.map { topic -> PageSection(topic.title, topic.icon) { HowToTopic(topic) } } +
            PageSection("Spreadsheet functions", Icons.Outlined.Functions) { FunctionReference(onPick = null) },
    )
}

/** One topic: its intro, the examples (as they'd be typed) and the tips. */
@Composable
fun ColumnScope.HowToTopic(topic: HowTo.Topic) {
    val colors = MaterialTheme.colorScheme
    MathText(topic.intro, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    if (topic.examples.isNotEmpty()) {
        Text("Examples", style = MaterialTheme.typography.titleSmall, color = colors.primary)
        topic.examples.forEach { e ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (e.advanced) colors.tertiaryContainer.copy(alpha = 0.45f) else colors.surfaceContainerHigh).padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(e.label, style = MaterialTheme.typography.labelLarge, color = if (e.advanced) colors.tertiary else colors.primary)
                Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp)) { MathView(e.row, 19.sp, colors.onSurface) }
                MathText(e.note, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
    if (topic.tips.isNotEmpty()) {
        Text("How", style = MaterialTheme.typography.titleSmall, color = colors.primary)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.surfaceContainer).padding(vertical = 6.dp)) {
            topic.tips.forEach { tip ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(colors.secondaryContainer), contentAlignment = Alignment.Center) {
                        Icon(tipIcon(tip.icon), contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tip.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                        MathText(tip.text, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
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
        Text(
            "Start a cell with =. Cells are A1, B2…; ranges A1:A10 or whole columns A:A; \$A\$1 stays put when filled down. Operators + − * / ^ % & and comparisons = <> < > <= >=. Text goes in \"quotes\".",
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp),
        )
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            placeholder = { Text("Search, e.g. lookup, NORM, date") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        // The groups as chips; the chosen one is filled, so no check mark is needed.
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (listOf<String?>(null) + com.example.cas.graph.Sheet.CATEGORIES).forEach { c ->
                FilterChip(selected = group == c, onClick = { group = c }, label = { Text(c ?: "All") })
            }
        }
        if (shown.isEmpty()) Text("No functions match", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
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
