package com.example.cas.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cas.editor.Frac
import com.example.cas.editor.MathRow
import com.example.cas.editor.Pow
import com.example.cas.editor.row
import com.example.cas.engine.Evaluator
import com.example.cas.engine.Formatter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/*
 * Settings, after Android's own (Pixel) settings: a main page with a search bar and the
 * categories, each in a tinted circle with a line saying what's set; each category on its own
 * page, under a large title with a round back button. Settings sit in groups of cards (large
 * corners at the ends of a group, small ones between, thin gaps); switches carry a check or a
 * cross; choices are tiles or connected buttons; sizes are stepped sliders with − and +; some
 * pages open with a live preview. On a tablet the categories list down the left instead.
 */

/** One category: its page's content, its place on the main page, and the words search finds it by. */
private class Category(
    val title: String,
    val icon: ImageVector,
    /** 0–3: which of the scheme's container colors its icon circle takes. */
    val tone: Int,
    /** The group on the main page it sits in. */
    val group: Int,
    val summary: @Composable () -> String,
    val keywords: List<String>,
    val content: @Composable ColumnScope.() -> Unit,
)

@Composable
fun AppSettingsPage(vm: CalculatorViewModel? = null, onBack: () -> Unit, onAcknowledgements: () -> Unit) {
    val tablet = isTabletLayout()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var howTo by remember { mutableStateOf(false) }
    var tabsPage by remember { mutableStateOf(false) }
    var resetting by remember { mutableStateOf(false) }
    var open by rememberSaveable { mutableStateOf<String?>(null) }

    val themes = listOf("System", "Light", "Dark")
    val sizes = listOf("Small", "Medium", "Large")
    val keypadSizes = listOf("Compact", "Medium", "Tall")
    val formats = listOf("Auto", "Scientific", "Engineering")

    val categories = listOf(
        Category("Appearance", TableIcons.Palette, 0, 0,
            summary = { tr(themes[AppSettings.theme]) + " · " + tr(sizes[AppSettings.mathSize]).lowercase() + " " + tr("math") },
            keywords = listOf("Theme", "Light", "Dark", "Colors from your wallpaper", "App color", "Math size", "Keypad size", "Expressive motion"),
        ) {
            PreviewCard(listOf("Calculator", "Graph")) { page -> if (page == 0) CalculatorPreview() else GraphPreview() }
            Header("Theme")
            Tiles(themes.indices.map { k -> Tile(themes[k], { AppIcon(listOf(TableIcons.ThemeSystem, TableIcons.ThemeLight, TableIcons.ThemeDark)[k], null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary) }) },
                AppSettings.theme, AppSettings::changeTheme)
            val wallpaper = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
            Cards(
                if (wallpaper) ({ ToggleRow("Colors from your wallpaper", "Material You dynamic color", AppSettings.dynamicColor, AppSettings::changeDynamicColor) }) else null,
                if (!wallpaper || !AppSettings.dynamicColor) ({ Box(RowPadding) { ThemeColorChoice() } }) else null,
            )
            Header("Size options")
            Cards(
                { SteppedRow("Math size", "Make answers and formulas bigger or smaller", AppSettings.mathSize, 0..2, AppSettings::changeMathSize) },
                { SteppedRow("Keypad size", "Make the keys taller or shorter", AppSettings.keypadSize, 0..2, AppSettings::changeKeypadSize) },
                if (tablet) ({ ChoiceRow("Keyboard side", listOf("Left", "Right"), AppSettings.keypadSide, AppSettings::changeKeypadSide) }) else null,
            )
            Header("Motion")
            Cards({ ToggleRow("Expressive motion", "Springy animations; off for calmer ones", AppSettings.expressiveMotion, AppSettings::changeExpressiveMotion) })
        },
        Category("Answers", TableIcons.ModeCalculator, 1, 0,
            summary = { listOfNotNull(if (vm?.decimalFirst == true) tr("Decimal first") else tr("Exact first"), tr("live answer").takeIf { AppSettings.livePreview }, tr("previews").takeIf { AppSettings.answerPreviews }).joinToString(", ") },
            keywords = listOf("Live answer", "Show answers as", "Exact", "Decimal", "Continue from the answer", "Ans", "Graph previews"),
        ) {
            Hero("Live answer", AppSettings.livePreview, AppSettings::changeLivePreview)
            Header("How it works")
            How("While you type", "The result shows under the input before you press Enter, so mistakes show up early.")
            Header("Answers")
            Cards(
                if (vm != null) ({ ChoiceRow("Show answers as", listOf("Exact", "Decimal"), if (vm.decimalFirst) 1 else 0) { vm.changeDecimalFirst(it == 1) } }) else null,
                { ToggleRow("Continue from the answer", "An operator after = starts with Ans", AppSettings.continueFromAnswer, AppSettings::changeContinueFromAnswer) },
                { ToggleRow("Graph previews", "A faded sketch beside answers that are curves", AppSettings.answerPreviews, AppSettings::changeAnswerPreviews) },
            )
            Footnote("Tap an answer to switch between its exact form and its decimal at any time.")
        },
        Category("Numbers", TableIcons.NumberFormat, 1, 0,
            summary = { "${vm?.digits ?: 10} " + tr("digits") + " · " + tr(formats[AppSettings.numberFormat]).lowercase() },
            keywords = listOf("Significant digits", "Group digits", "Number format", "Scientific", "Engineering", "Scientific notation", "Decimals", "Complex numbers", "Polar"),
        ) {
            NumbersPreview(vm)
            Header("Format")
            Tiles(listOf(
                Tile("Auto", { MathText("\$123\\,456\\,789\$", style = TextStyle(fontSize = 17.sp), mathScale = 1f) }),
                Tile("Scientific", { MathText("\$1.23{\\times}10^{8}\$", style = TextStyle(fontSize = 17.sp), mathScale = 1f) }),
                Tile("Engineering", { MathText("\$123{\\times}10^{6}\$", style = TextStyle(fontSize = 17.sp), mathScale = 1f) }),
            ), AppSettings.numberFormat) { AppSettings.changeNumberFormat(it); vm?.reformat() }
            Header("Precision")
            Cards(
                if (vm != null) ({ SteppedRow("Significant digits", "${vm.digits} · " + tr("how many digits a decimal keeps"), vm.digits, 4..15, vm::changeDigits) }) else null,
                { ToggleRow("Group digits", "123 456 789 rather than 123456789", AppSettings.groupDigits) { AppSettings.changeGroupDigits(it); vm?.reformat() } },
                { ChoiceRow("Complex numbers", listOf("\$a + bi\$", "\$re^{i\\theta}\$"), if (AppSettings.polarComplex) 1 else 0) { AppSettings.changePolarComplex(it == 1); vm?.reformat() } },
            )
            Header("Large and small numbers")
            Cards(
                { SteppedRow("Scientific notation past ${AppSettings.sciAfter} digits", if (AppSettings.numberFormat == 0) "Longer numbers, exact ones too, show as \$a \\times 10^n\$" else "Longer whole numbers show as \$a \\times 10^n\$", AppSettings.sciAfter, 5..30) { AppSettings.changeSciAfter(it); vm?.reformat() } },
                { SteppedRow("${AppSettings.sciDecimals} " + (if (AppSettings.sciDecimals == 1) "decimal" else "decimals") + " in \$a \\times 10^n\$", null, AppSettings.sciDecimals, 1..12) { AppSettings.changeSciDecimals(it); vm?.reformat() } },
            )
            Footnote("Exact answers stay exact; these settings change only how decimals are written.")
        },
        Category("Keypad", TableIcons.Keyboard, 2, 1,
            summary = { "${CalcTabs.shown().size} " + tr("tabs") + " · " + tr(keypadSizes[AppSettings.keypadSize]).lowercase() },
            keywords = listOf("Calculator tabs", "Haptic feedback", "Vibration", "Key sounds", "Explanations on long-press", "Keypad size", "Keyboard side"),
        ) {
            Intro("How the keys sound and feel, and which tabs sit above them.")
            Cards({ LinkRow("Calculator tabs", "${CalcTabs.shown().size} " + tr("on the keypad; reorder them or add more"), TableIcons.Keyboard, 2) { tabsPage = true } })
            Header("Feel")
            Cards(
                { ToggleRow("Haptic feedback", "A tap on each key and button", AppSettings.haptics, AppSettings::changeHaptics) },
                { ToggleRow("Key sounds", "A click on each key", AppSettings.keySounds, AppSettings::changeKeySounds) },
            )
            Header("Help on keys")
            Cards({ ToggleRow("Explanations on long-press", "Formula, theory and how to use each key", AppSettings.keyHelp, AppSettings::changeKeyHelp) })
            Footnote("Key sounds use your phone's touch sounds and follow its volume. The keypad's size is under Appearance.")
        },
        Category("Graphs", TableIcons.Mode2D, 2, 1,
            summary = { listOfNotNull(tr("Grid").takeIf { AppSettings.showGrid }, tr("legend").takeIf { AppSettings.showLegend }, tr("marked points").takeIf { AppSettings.specialPoints }).joinToString(", ").ifEmpty { tr("Plain") } },
            keywords = listOf("Starting view", "Grid lines", "Legend", "Mark points on curves", "Zeros", "Detail", "Quality", "2D fields", "3D surfaces", "Complex plots"),
        ) {
            Header("Display")
            Cards(
                { ChoiceRow("Starting view", listOf("\$\\pm 5\$", "\$\\pm 10\$", "\$\\pm 20\$"), when (AppSettings.viewHalfWidth) { 5 -> 0; 20 -> 2; else -> 1 }) { AppSettings.changeViewHalfWidth(listOf(5, 10, 20)[it]) } },
                { ToggleRow("Grid lines", "The axes always show", AppSettings.showGrid, AppSettings::changeShowGrid) },
                { ToggleRow("Legend", "Each line's name in the corner, and in exports. Hold a line to rename it", AppSettings.showLegend, AppSettings::changeShowLegend) },
                { ToggleRow("Mark points on curves", "Zeros, extrema and crossings of the tapped curve", AppSettings.specialPoints, AppSettings::changeSpecialPoints) },
            )
            Header("Detail")
            // Complex plot quality is stored as 2 low, 0 medium, 1 high.
            Cards(
                { SteppedRow("2D fields", listOf("Low", "Medium", "High")[AppSettings.fieldQuality], AppSettings.fieldQuality, 0..2, AppSettings::changeFieldQuality) },
                { SteppedRow("3D surfaces", listOf("Low", "Medium", "High")[AppSettings.surfaceDetail], AppSettings.surfaceDetail, 0..2, AppSettings::changeSurfaceDetail) },
                {
                    val level = when (AppSettings.complexQuality) { 2 -> 0; 1 -> 2; else -> 1 }
                    SteppedRow("Complex plots", listOf("Low", "Medium", "High")[level], level, 0..2) { AppSettings.changeComplexQuality(listOf(2, 0, 1)[it]) }
                },
            )
            Footnote("Higher detail is sharper but slower to draw.")
        },
        Category("History", TableIcons.History, 3, 2,
            summary = { if (AppSettings.historyLimit == 0) tr("Keeps everything") else tr("Keeps {0}", AppSettings.historyLimit) },
            keywords = listOf("History keeps", "Ask before clearing history", "Ask before deleting", "Confirm"),
        ) {
            Cards(
                { ChoiceRow("History keeps", listOf("50", "100", "500", "All"), when (AppSettings.historyLimit) { 50 -> 0; 100 -> 1; 500 -> 2; else -> 3 }) { AppSettings.changeHistoryLimit(listOf(50, 100, 500, 0)[it]) } },
            )
            Header("Confirmations")
            Cards(
                { ToggleRow("Ask before clearing history", "Pinned calculations and those in folders are always kept", AppSettings.confirmClearHistory, AppSettings::changeConfirmClearHistory) },
                { ToggleRow("Ask before deleting", "A calculation, or a line in a graph", AppSettings.confirmDeleteEntry, AppSettings::changeConfirmDeleteEntry) },
            )
        },
        Category("General", TableIcons.Settings, 3, 2,
            summary = { listOfNotNull(tr("Screen on").takeIf { AppSettings.keepScreenOn }, tr("shortcuts"), tr("reset")).joinToString(", ").replaceFirstChar { it.uppercase() } },
            keywords = listOf("Language", "Keep the screen on", "Home screen shortcuts", "Reset all settings"),
        ) {
            Header("Screen")
            Cards(
                { ToggleRow("Keep the screen on", "While the app is open", AppSettings.keepScreenOn, AppSettings::changeKeepScreenOn) },
                // Only once a translation has been added (assets/i18n).
                if (I18n.languages.size >= 2) ({
                    val codes = listOf("") + I18n.languages.map { it.code }
                    val names = listOf(tr("System")) + I18n.languages.map { it.name }
                    ChoiceRow(if (I18n.code == "en") "Language" else tr("Language") + " (Language)", names, codes.indexOf(AppSettings.language).coerceAtLeast(0)) { AppSettings.changeLanguage(codes[it]) }
                }) else null,
            )
            Header("Home screen shortcuts")
            Cards(*Shortcuts.entries.map { e -> @Composable { LinkRow(e.longLabel, "Add to the home screen", TableIcons.Shortcut, 1, trailing = TableIcons.Add) { Shortcuts.pin(context, e) } } }.toTypedArray())
            Header("Reset")
            Cards({ LinkRow("Reset all settings", "Back to how they were at install; your work is kept", TableIcons.Reset, 3, trailing = null) { resetting = true } })
        },
        Category("Help & feedback", TableIcons.Help, 3, 3,
            summary = { tr("Documentation, report a bug") },
            keywords = listOf("Documentation", "Report a bug", "Request a feature", "Acknowledgements", "Licenses"),
        ) {
            Cards(
                { LinkRow("Documentation", "${Docs.chapters.size} " + tr("chapters with") + " ${Docs.allExamples.size} " + tr("worked examples, every key, and the spreadsheet functions"), TableIcons.Help, 0) { howTo = true } },
            )
            Header("Feedback")
            Cards(
                { LinkRow("Report a bug", "An email to the developer with the app's log attached", TableIcons.Bug, 3) { scope.launch { Feedback.reportBug(context) } } },
                { LinkRow("Request a feature", "An email to the developer with your idea", TableIcons.Edit, 1) { Feedback.requestFeature(context) } },
            )
            Header("About")
            Cards({ LinkRow("Acknowledgements", "Fonts, libraries, data and methods, with links", TableIcons.Info, 2) { onAcknowledgements() } })
        },
    )

    if (tablet) {
        // Two panes, as before: the categories down the left, the chosen one on the right.
        SectionedPage("Settings", onBack = onBack, sections = categories.map { c -> PageSection(c.title, c.icon) { c.content(this) } })
    } else {
        PixelPage("Settings", onBack) { MainPage(categories) { open = it } }
        categories.firstOrNull { it.title == open }?.let { c ->
            PixelPage(c.title, onBack = { open = null }) { c.content(this) }
        }
    }
    if (howTo) DocsPage(onBack = { howTo = false })
    if (tabsPage) CalcTabsPage(onBack = { tabsPage = false })
    if (resetting) AlertDialog(
        onDismissRequest = { resetting = false },
        icon = { AppIcon(TableIcons.Reset, contentDescription = null) },
        title = { Text(tr("Reset all settings?")) },
        text = { Text(tr("Every setting goes back to how it was when the app was installed. Your history, graphs, projects, symbols and pinned keys stay as they are.")) },
        confirmButton = { TextButton(onClick = { resetting = false; AppSettings.resetAll(context); vm?.resetSettings() }) { Text(tr("Reset")) } },
        dismissButton = { TextButton(onClick = { resetting = false }) { Text(tr("Cancel")) } },
    )
}

// ---- Pages --------------------------------------------------------------------------------

/** A full-screen settings page: a round back button and a large title, the content scrolling below. */
@Composable
private fun PixelPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onBack, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler(onBack = onBack)
        Surface(Modifier.fillMaxSize(), color = colors.surface) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(colors.surfaceContainerHigh).clickable(onClickLabel = tr("Back"), onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) { AppIcon(TableIcons.Back, contentDescription = tr("Back"), tint = colors.onSurface) }
                    Spacer(Modifier.width(16.dp))
                    Text(tr(title), style = MaterialTheme.typography.headlineMedium, color = colors.onSurface, maxLines = 1)
                }
                Column(
                    Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    content = content,
                )
            }
        }
    }
}

/** The main page: search, then the categories in groups (or, while searching, the settings that match). */
@Composable
private fun ColumnScope.MainPage(categories: List<Category>, onOpen: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var query by rememberSaveable { mutableStateOf("") }
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp).height(56.dp).clip(CircleShape).background(colors.surfaceContainerHigh).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(TableIcons.Search, contentDescription = null, tint = colors.onSurfaceVariant)
        Spacer(Modifier.width(14.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text(tr("Search settings"), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            BasicTextField(
                query, { query = it }, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = tr("Search settings") },
            )
        }
        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { AppIcon(TableIcons.Close, contentDescription = tr("Clear"), tint = colors.onSurfaceVariant) }
    }
    val q = query.trim()
    if (q.isNotEmpty()) {
        // Every setting whose name (or its category's) has the words typed.
        val hits = categories.flatMap { c -> (listOf(c.title) + c.keywords).map { k -> c to k } }
            .filter { (c, k) -> q.split(' ').filter { it.isNotBlank() }.all { w -> tr(k).contains(w, ignoreCase = true) || k.contains(w, ignoreCase = true) || tr(c.title).contains(w, ignoreCase = true) } }
            .distinctBy { (c, k) -> c.title + k }
        if (hits.isEmpty()) Text(tr("No settings match"), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))
        else Cards(*hits.take(30).map { (c, k) -> @Composable { LinkRow(k, c.title, c.icon, c.tone, trailing = TableIcons.ChevronRight) { onOpen(c.title) } } }.toTypedArray())
        return
    }
    categories.groupBy { it.group }.values.forEachIndexed { i, group ->
        if (i > 0) Spacer(Modifier.height(16.dp))
        Cards(*group.map { c -> @Composable { LinkRow(c.title, c.summary(), c.icon, c.tone, trailing = null) { onOpen(c.title) } } }.toTypedArray())
    }
}

// ---- Building blocks ---------------------------------------------------------------------

/** A group of cards: the first and last have large outer corners, the ones between small, with thin gaps. Null rows are left out. */
@Composable
private fun Cards(vararg rows: (@Composable () -> Unit)?) {
    val shown = rows.filterNotNull()
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        shown.forEachIndexed { k, row ->
            val big = 26.dp; val small = 5.dp
            val shape = RoundedCornerShape(
                topStart = if (k == 0) big else small, topEnd = if (k == 0) big else small,
                bottomStart = if (k == shown.lastIndex) big else small, bottomEnd = if (k == shown.lastIndex) big else small,
            )
            Box(Modifier.fillMaxWidth().clip(shape).background(colors.surfaceContainerHigh)) { row() }
        }
    }
}

/** A section's name, in the accent color. */
@Composable
private fun Header(text: String) =
    Text(tr(text), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, top = 24.dp, bottom = 10.dp))

@Composable
private fun Intro(text: String) =
    Text(tr(text), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp))

/** An ⓘ and a line underneath the settings. */
@Composable
private fun Footnote(text: String) {
    Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 20.dp)) {
        AppIcon(TableIcons.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(10.dp))
        MathText(tr(text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** One point of a "How it works": an ⓘ, a title and a line. */
@Composable
private fun How(title: String, text: String) {
    Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
        AppIcon(TableIcons.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(tr(title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(tr(text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A switch with a check in its thumb when on and a cross when off, as in Android's settings. */
@Composable
private fun PxSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked, onCheckedChange = onChange,
        thumbContent = { AppIcon(if (checked) TableIcons.Check else TableIcons.Close, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) },
    )
}

private val RowPadding = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)

@Composable
private fun RowTexts(title: String, detail: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        MathText(tr(title), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = MaterialTheme.colorScheme.onSurface)
        if (detail != null) MathText(tr(detail), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A setting that's on or off: the whole card toggles it. */
@Composable
private fun ToggleRow(title: String, detail: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(role = Role.Switch) { onChange(!checked) }.then(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowTexts(title, detail, Modifier.weight(1f))
        Spacer(Modifier.width(16.dp))
        PxSwitch(checked, onChange)
    }
}

/** The tint pairs for icon circles: the scheme's container colors, so they follow the theme and Material You. */
@Composable
private fun tone(t: Int): Pair<Color, Color> = with(MaterialTheme.colorScheme) {
    when (t) { 0 -> primaryContainer to onPrimaryContainer; 1 -> secondaryContainer to onSecondaryContainer; 2 -> tertiaryContainer to onTertiaryContainer; else -> errorContainer to onErrorContainer }
}

/** A row that opens something, with its icon in a tinted circle; [trailing] is the chevron (or + for adding, or nothing). */
@Composable
private fun LinkRow(title: String, detail: String?, icon: ImageVector?, tone: Int, trailing: ImageVector? = TableIcons.ChevronRight, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(onClickLabel = tr(title), onClick = onClick).then(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            val (bg, fg) = tone(tone)
            Box(Modifier.size(44.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) { AppIcon(icon, contentDescription = null, tint = fg, accent = fg) }
            Spacer(Modifier.width(16.dp))
        }
        RowTexts(title, detail, Modifier.weight(1f))
        if (trailing != null) AppIcon(trailing, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A choice of a few options as connected buttons inside the card. */
@Composable
private fun ChoiceRow(title: String, options: List<String>, selected: Int, onChange: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(RowPadding) {
        RowTexts(title, null)
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            options.forEachIndexed { k, name ->
                val on = k == selected
                val end = 20.dp; val mid = 6.dp
                val shape = if (on) CircleShape else RoundedCornerShape(
                    topStart = if (k == 0) end else mid, bottomStart = if (k == 0) end else mid,
                    topEnd = if (k == options.lastIndex) end else mid, bottomEnd = if (k == options.lastIndex) end else mid,
                )
                Box(
                    Modifier.weight(1f).height(40.dp).clip(shape).background(if (on) colors.primary else colors.surfaceContainerHighest)
                        .clickable(role = Role.RadioButton) { onChange(k) }.semantics { this.selected = on },
                    contentAlignment = Alignment.Center,
                ) { MathText(tr(name), style = MaterialTheme.typography.labelLarge, color = if (on) colors.onPrimary else colors.onSurface, maxLines = 1) }
            }
        }
    }
}

/** A setting with a few steps: a slider with a dot at each step, and − and + at its ends. */
@Composable
private fun SteppedRow(title: String, detail: String?, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column(RowPadding) {
        RowTexts(title, detail)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (value > range.first) onChange(value - 1) }, enabled = value > range.first) { AppIcon(TableIcons.Remove, contentDescription = tr("Less")) }
            ExpressiveSlider(
                value = value.toFloat(),
                onValueChange = { v -> v.roundToInt().let { if (it != value) onChange(it) } },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = range.last - range.first - 1,
                modifier = Modifier.weight(1f).semantics { contentDescription = tr(title) },
            )
            IconButton(onClick = { if (value < range.last) onChange(value + 1) }, enabled = value < range.last) { AppIcon(TableIcons.Add, contentDescription = tr("More")) }
        }
    }
}

/** The page's main switch, large on the primary container (as Android's Earthquake alerts). */
@Composable
private fun Hero(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(40.dp)).background(colors.primaryContainer)
            .clickable(role = Role.Switch) { onChange(!checked) }.padding(horizontal = 28.dp, vertical = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(tr(title), style = MaterialTheme.typography.titleLarge, color = colors.onPrimaryContainer, modifier = Modifier.weight(1f))
        PxSwitch(checked, onChange)
    }
}

private class Tile(val label: String, val content: @Composable () -> Unit)

/** A choice as large tiles (as Android's Color contrast): the chosen one outlined, with a check. */
@Composable
private fun Tiles(tiles: List<Tile>, selected: Int, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.forEachIndexed { k, t ->
            val on = k == selected
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(24.dp)).background(colors.surfaceContainerHigh)
                        .then(if (on) Modifier.border(3.dp, colors.primary, RoundedCornerShape(24.dp)) else Modifier)
                        .clickable(role = Role.RadioButton, onClickLabel = tr(t.label)) { onSelect(k) }.semantics { this.selected = on },
                    contentAlignment = Alignment.Center,
                ) {
                    t.content()
                    if (on) Box(Modifier.align(Alignment.BottomEnd).padding(8.dp).size(26.dp).clip(CircleShape).background(colors.primary), contentAlignment = Alignment.Center) {
                        AppIcon(TableIcons.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
                    }
                }
                Text(tr(t.label), style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

// ---- Previews ----------------------------------------------------------------------------

/** A preview on the page's darkest (or lightest) surface, with dots to page through [pages]. */
@Composable
private fun PreviewCard(pages: List<String>, content: @Composable (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var page by rememberSaveable { mutableIntStateOf(0) }
    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(28.dp)).background(colors.surfaceContainerLowest).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(tr("Preview"), style = MaterialTheme.typography.labelLarge, color = colors.primary)
        Text(tr(pages[page]), style = MaterialTheme.typography.titleLarge, color = colors.primary, modifier = Modifier.padding(bottom = 10.dp))
        content(page)
        if (pages.size > 1) Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { page = (page - 1 + pages.size) % pages.size }) { AppIcon(TableIcons.Back, contentDescription = tr("Previous"), tint = colors.primary, modifier = Modifier.size(18.dp)) }
            pages.indices.forEach { k -> Box(Modifier.padding(horizontal = 4.dp).size(7.dp).clip(CircleShape).background(if (k == page) colors.onSurface else colors.outline)) }
            IconButton(onClick = { page = (page + 1) % pages.size }) { AppIcon(TableIcons.ChevronRight, contentDescription = tr("Next"), tint = colors.primary, modifier = Modifier.size(18.dp)) }
        }
    }
}

/** Two history cards and a few keys, at the chosen math and keypad sizes, in the current theme. */
@Composable
private fun CalculatorPreview() {
    val colors = MaterialTheme.colorScheme
    val rows = remember {
        listOf(
            MathRow(mutableListOf(Frac(row("1"), row("3")), com.example.cas.editor.Sym("+"), Frac(row("1"), row("6")))) to "\$= \\tfrac{1}{2}\$",
            MathRow(mutableListOf(com.example.cas.editor.Sym("x"), Pow(row("2")), com.example.cas.editor.Sym("="), com.example.cas.editor.Sym("9"))) to "\$\\Rightarrow x = -3,\\ x = 3\$",
        )
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { (q, a) ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surfaceContainer).padding(horizontal = 14.dp, vertical = 10.dp)) {
                MathView(q, MathSizes.historyInput * 0.8f, colors.onSurface)
                MathText(a, modifier = Modifier.align(Alignment.End), style = TextStyle(fontSize = MathSizes.historyAnswer * 0.8f), color = colors.onSurface, mathScale = 1f)
            }
        }
        val keyHeight = (30 * AppSettings.keypadScale).dp
        listOf(listOf("7", "8", "9", "÷"), listOf("4", "5", "6", "×")).forEach { line ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                line.forEachIndexed { k, key ->
                    Box(Modifier.weight(1f).height(keyHeight).clip(CircleShape).background(if (k == 3) colors.primaryContainer else colors.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                        Text(key, color = if (k == 3) colors.onPrimaryContainer else colors.onSurface, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

/** A small graph in the theme's colors: the axes and a curve, as the 2D graph draws them. */
@Composable
private fun GraphPreview() {
    val colors = MaterialTheme.colorScheme
    val line = colors.primary; val axis = colors.onSurfaceVariant; val grid = colors.outlineVariant.copy(alpha = 0.5f)
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val w = size.width; val h = size.height
        if (AppSettings.showGrid) for (k in 1 until 8) {
            drawLine(grid, Offset(w * k / 8, 0f), Offset(w * k / 8, h), 1f)
            if (k < 5) drawLine(grid, Offset(0f, h * k / 5), Offset(w, h * k / 5), 1f)
        }
        drawLine(axis, Offset(0f, h / 2), Offset(w, h / 2), 2f)
        drawLine(axis, Offset(w / 8, 0f), Offset(w / 8, h), 2f)
        val p = Path()
        for (i in 0..160) {
            val x = -1.0 + 16.0 * i / 160
            val y = kotlin.math.sin(x) * kotlin.math.exp(-x / 6)
            val px = (w / 8 + (x / 16.0) * w * 7 / 8 * 1.1).toFloat(); val py = (h / 2 - y * h * 0.38).toFloat()
            if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
        }
        drawPath(p, line, style = Stroke(5f, cap = StrokeCap.Round))
    }
}

/** Real answers written with the number settings as they are now, updating as they change. */
@Composable
private fun NumbersPreview(vm: CalculatorViewModel?) {
    val colors = MaterialTheme.colorScheme
    val digits = vm?.digits ?: 10
    val rows = remember(digits, AppSettings.numberFormat, AppSettings.sciAfter, AppSettings.sciDecimals, AppSettings.groupDigits, AppSettings.polarComplex) {
        listOf(
            MathRow(mutableListOf(Frac(row("1"), row("7")))),
            MathRow(mutableListOf(com.example.cas.editor.Sym("2"), Pow(row("64")))),
            row("10973×11251"),
        ).map { q ->
            q to runCatching { Formatter.answer(Evaluator().evaluate(com.example.cas.editor.MathCodec.copy(q))) }.getOrNull()
        }
    }
    PreviewCard(listOf("Answers")) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { (q, a) ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surfaceContainer).padding(horizontal = 14.dp, vertical = 10.dp)) {
                    MathView(q, 16.sp, colors.onSurfaceVariant)
                    if (a != null) Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (a.approx != null) "≈ " else "= ", color = colors.primary, fontSize = 19.sp)
                        MathView(a.approx ?: a.exact, 19.sp, colors.onSurface)
                    }
                }
            }
        }
    }
}
