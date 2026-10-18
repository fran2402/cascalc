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
import androidx.compose.material3.SegmentedButton
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
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
import androidx.compose.animation.togetherWith
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
    var acknowledgements by remember { mutableStateOf(false) }
    var resetting by remember { mutableStateOf(false) }
    // The pages open on a phone, from the main page in: each slides in over the one before.
    var stack by rememberSaveable { mutableStateOf(listOf<String>()) }
    val go = { route: String -> stack = stack + route }

    val themes = listOf("System", "Light", "Dark")
    val sizes = AppSettings.MATH_STEPS
    val keypadSizes = AppSettings.KEYPAD_STEPS
    val formats = listOf("Auto", "Scientific", "Engineering")

    val categories = listOf(
        Category("Appearance", TableIcons.Palette, 0, 0,
            summary = { tr(themes[AppSettings.theme]) + " · " + tr(sizes[AppSettings.mathSize]).lowercase() + " " + tr("math") },
            keywords = listOf("Theme", "Light", "Dark", "Colors from your wallpaper", "App color", "Math size", "Keypad size", "Expressive motion"),
        ) {
            PreviewCard(listOf("Calculator", "Graph")) { page -> if (page == 0) CalculatorPreview() else GraphPreview() }
            PxHeader("Theme")
            Tiles(themes.indices.map { k -> Tile(themes[k], { AppIcon(listOf(TableIcons.ThemeSystem, TableIcons.ThemeLight, TableIcons.ThemeDark)[k], null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary) }) },
                AppSettings.theme, AppSettings::changeTheme)
            val wallpaper = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
            PxCards(
                if (wallpaper) ({ PxToggleRow("Colors from your wallpaper", "Material You dynamic color", AppSettings.dynamicColor, AppSettings::changeDynamicColor) }) else null,
                if (!wallpaper || !AppSettings.dynamicColor) ({ Box(PxRowPadding) { ThemeColorChoice() } }) else null,
            )
            PxHeader("Size options")
            PxCards(
                { PxSteppedRow("Math size", tr(sizes[AppSettings.mathSize]) + " · " + tr("make answers and formulas bigger or smaller"), AppSettings.mathSize, 0..4, AppSettings::changeMathSize) },
                { PxSteppedRow("Keypad size", tr(keypadSizes[AppSettings.keypadSize]) + " · " + tr("make the keys taller or shorter"), AppSettings.keypadSize, 0..4, AppSettings::changeKeypadSize) },
                if (tablet) ({ PxChoiceRow("Keyboard side", listOf("Left", "Right"), AppSettings.keypadSide, AppSettings::changeKeypadSide) }) else null,
            )
            PxHeader("Motion")
            PxCards({ PxToggleRow("Expressive motion", "Springy animations; off for calmer ones", AppSettings.expressiveMotion, AppSettings::changeExpressiveMotion) })
        },
        Category("Answers", TableIcons.ModeCalculator, 2, 0,
            summary = { listOfNotNull(if (vm?.decimalFirst == true) tr("Decimal first") else tr("Exact first"), tr("live answer").takeIf { AppSettings.livePreview }, tr("previews").takeIf { AppSettings.answerPreviews }).joinToString(", ") },
            keywords = listOf("Live answer", "Show answers as", "Exact", "Decimal", "Continue from the answer", "Ans", "Graph previews", "Angles", "Radians", "Degrees", "Units for constants", "SI", "Planck", "Natural units"),
        ) {
            Hero("Live answer", AppSettings.livePreview, AppSettings::changeLivePreview)
            PxHeader("How it works")
            How("While you type", "The result shows under the input before you press Enter, so mistakes show up early.")
            PxHeader("Answers")
            PxCards(
                if (vm != null) ({ PxChoiceRow("Show answers as", listOf("Exact", "Decimal"), if (vm.decimalFirst) 1 else 0) { vm.changeDecimalFirst(it == 1) } }) else null,
                { PxToggleRow("Continue from the answer", "An operator after = starts with Ans", AppSettings.continueFromAnswer, AppSettings::changeContinueFromAnswer) },
                { PxToggleRow("Graph previews", "A faded sketch beside answers that are curves", AppSettings.answerPreviews, AppSettings::changeAnswerPreviews) },
            )
            PxHeader("Calculation")
            PxCards(
                { PxChoiceRow("Angles", listOf("Radians", "Degrees"), if (KeypadState.angle == com.example.cas.engine.AngleUnit.Degrees) 1 else 0) { if ((it == 1) != (KeypadState.angle == com.example.cas.engine.AngleUnit.Degrees)) KeypadState.toggleAngle() } },
                { PxChoiceRow("Units for constants", com.example.cas.engine.UnitSystem.entries.map { it.label }, KeypadState.unitSystem.ordinal) { KeypadState.selectUnitSystem(com.example.cas.engine.UnitSystem.entries[it]) } },
            )
            PxFootnote("Tap an answer to switch between its exact form and its decimal at any time. Angles and units can also be switched on the keypad.")
        },
        Category("Numbers", TableIcons.NumberFormat, 4, 0,
            summary = { "${vm?.digits ?: 10} " + tr("digits") + " · " + tr(formats[AppSettings.numberFormat]).lowercase() },
            keywords = listOf("Significant digits", "Group digits", "Number format", "Scientific", "Engineering", "Scientific notation", "Decimals", "Complex numbers", "Polar"),
        ) {
            NumbersPreview(vm)
            PxHeader("Format")
            Tiles(listOf(
                Tile("Auto", { MathText("\$123\\,456\\,789\$", style = TextStyle(fontSize = 17.sp), mathScale = 1f) }),
                Tile("Scientific", { MathText("\$1.23{\\times}10^{8}\$", style = TextStyle(fontSize = 17.sp), mathScale = 1f) }),
                Tile("Engineering", { MathText("\$123{\\times}10^{6}\$", style = TextStyle(fontSize = 17.sp), mathScale = 1f) }),
            ), AppSettings.numberFormat) { AppSettings.changeNumberFormat(it); vm?.reformat() }
            PxHeader("Precision")
            PxCards(
                if (vm != null) ({ PxSteppedRow("Significant digits", "${vm.digits} · " + tr("how many digits a decimal keeps"), vm.digits, 4..15, vm::changeDigits) }) else null,
                { PxToggleRow("Group digits", "123 456 789 rather than 123456789", AppSettings.groupDigits) { AppSettings.changeGroupDigits(it); vm?.reformat() } },
                { PxChoiceRow("Complex numbers", listOf("\$a + bi\$", "\$re^{i\\theta}\$"), if (AppSettings.polarComplex) 1 else 0) { AppSettings.changePolarComplex(it == 1); vm?.reformat() } },
            )
            PxHeader("Large and small numbers")
            PxCards(
                { PxSteppedRow("Scientific notation past ${AppSettings.sciAfter} digits", if (AppSettings.numberFormat == 0) "Longer numbers, exact ones too, show as \$a \\times 10^n\$" else "Longer whole numbers show as \$a \\times 10^n\$", AppSettings.sciAfter, 5..30) { AppSettings.changeSciAfter(it); vm?.reformat() } },
                { PxSteppedRow("${AppSettings.sciDecimals} " + (if (AppSettings.sciDecimals == 1) "decimal" else "decimals") + " in \$a \\times 10^n\$", null, AppSettings.sciDecimals, 1..12) { AppSettings.changeSciDecimals(it); vm?.reformat() } },
            )
            PxFootnote("Exact answers stay exact; these settings change only how decimals are written.")
        },
        Category("Keypad", TableIcons.Keyboard, 6, 1,
            summary = { "${CalcTabs.shown().size} " + tr("tabs") + " · " + tr(keypadSizes[AppSettings.keypadSize]).lowercase() },
            keywords = listOf("Calculator tabs", "Haptic feedback", "Vibration", "Key sounds", "Explanations on long-press", "Keypad size", "Keyboard side"),
        ) {
            Intro("How the keys sound and feel, and which tabs sit above them.")
            PxCards({ LinkRow("Calculator tabs", "${CalcTabs.shown().size} " + tr("on the keypad; reorder them or add more"), TableIcons.Keyboard, 6) { if (tablet) tabsPage = true else go("Calculator tabs") } })
            PxHeader("Feel")
            PxCards(
                { PxToggleRow("Haptic feedback", "A tap on each key and button", AppSettings.haptics, AppSettings::changeHaptics) },
                { PxToggleRow("Key sounds", "A click on each key", AppSettings.keySounds, AppSettings::changeKeySounds) },
            )
            PxHeader("Help on keys")
            PxCards({ PxToggleRow("Explanations on long-press", "Formula, theory and how to use each key", AppSettings.keyHelp, AppSettings::changeKeyHelp) })
            PxFootnote("Key sounds use your phone's touch sounds and follow its volume. The keypad's size is under Appearance.")
        },
        Category("Graphs", TableIcons.Mode2D, 8, 1,
            summary = { listOfNotNull(tr("Grid").takeIf { AppSettings.showGrid }, tr("legend").takeIf { AppSettings.showLegend }, tr("marked points").takeIf { AppSettings.specialPoints }).joinToString(", ").ifEmpty { tr("Plain") } },
            keywords = listOf("Starting view", "Grid lines", "Numbers on the axes", "Plots", "Figures", "matplotlib", "Ticks", "Legend", "Mark points on curves", "Zeros", "Detail", "Quality", "2D fields", "3D surfaces", "Complex plots"),
        ) {
            PxHeader("Display")
            PxCards(
                { PxChoiceRow("Starting view", listOf("\$\\pm 5\$", "\$\\pm 10\$", "\$\\pm 20\$"), when (AppSettings.viewHalfWidth) { 5 -> 0; 20 -> 2; else -> 1 }) { AppSettings.changeViewHalfWidth(listOf(5, 10, 20)[it]) } },
                { PxToggleRow("Grid lines", "The axes always show", AppSettings.showGrid, AppSettings::changeShowGrid) },
                { PxToggleRow("Numbers on the axes", "The tick values along the 2D graph's axes", AppSettings.axisNumbers, AppSettings::changeAxisNumbers) },
                { PxToggleRow("Legend", "Each line's name in the corner, and in exports. Hold a line to rename it", AppSettings.showLegend, AppSettings::changeShowLegend) },
                { PxToggleRow("Mark points on curves", "Zeros, extrema and crossings of the tapped curve", AppSettings.specialPoints, AppSettings::changeSpecialPoints) },
            )
            PxHeader("Plots")
            PxCards({ PxToggleRow("Plots (beta)", "Figures after matplotlib (histograms, box plots, contours, fits…) as a notebook block and from the 2D graph's ⋮ menu", AppSettings.plots, AppSettings::changePlots) })
            PxHeader("Detail")
            DetailCards()
            PxFootnote("Higher detail is sharper but slower to draw. Each graph's own settings have the same sliders.")
        },
        Category("History", TableIcons.History, 1, 2,
            summary = { if (AppSettings.historyLimit == 0) tr("Keeps everything") else tr("Keeps {0}", AppSettings.historyLimit) },
            keywords = listOf("History keeps", "Ask before clearing history", "Ask before deleting", "Confirm"),
        ) {
            PxCards(
                { PxChoiceRow("History keeps", listOf("50", "100", "500", "All"), when (AppSettings.historyLimit) { 50 -> 0; 100 -> 1; 500 -> 2; else -> 3 }) { AppSettings.changeHistoryLimit(listOf(50, 100, 500, 0)[it]) } },
            )
            PxHeader("Confirmations")
            PxCards(
                { PxToggleRow("Ask before clearing history", "Pinned calculations and those in folders are always kept", AppSettings.confirmClearHistory, AppSettings::changeConfirmClearHistory) },
                { PxToggleRow("Ask before deleting", "A calculation, a line in a graph, or a layer of a figure", AppSettings.confirmDeleteEntry, AppSettings::changeConfirmDeleteEntry) },
            )
        },
        Category("General", TableIcons.Settings, 3, 2,
            summary = { listOfNotNull(tr("Screen on").takeIf { AppSettings.keepScreenOn }, tr("shortcuts"), tr("reset")).joinToString(", ").replaceFirstChar { it.uppercase() } },
            keywords = listOf("Open at", "Start", "Language", "Keep the screen on", "Home screen shortcuts", "Reset all settings"),
        ) {
            PxHeader("Starting up")
            PxCards({ PxChoiceRow("Open at", listOf("Where I left off", "Calculator"), if (AppSettings.startInCalculator) 1 else 0) { AppSettings.changeStartInCalculator(it == 1) } })
            PxHeader("Screen")
            PxCards(
                { PxToggleRow("Keep the screen on", "While the app is open", AppSettings.keepScreenOn, AppSettings::changeKeepScreenOn) },
                // Only once a translation has been added (assets/i18n).
                if (I18n.languages.size >= 2) ({
                    val codes = listOf("") + I18n.languages.map { it.code }
                    val names = listOf(tr("System")) + I18n.languages.map { it.name }
                    PxChoiceRow(if (I18n.code == "en") "Language" else tr("Language") + " (Language)", names, codes.indexOf(AppSettings.language).coerceAtLeast(0)) { AppSettings.changeLanguage(codes[it]) }
                }) else null,
            )
            PxHeader("Home screen shortcuts")
            PxCards(*Shortcuts.entries.map { e -> @Composable { LinkRow(e.longLabel, "Add to the home screen", TableIcons.Shortcut, 3, trailing = TableIcons.Add) { Shortcuts.pin(context, e) } } }.toTypedArray())
            PxHeader("Reset")
            PxCards({ LinkRow("Reset all settings", "Back to how they were at install; your work is kept", TableIcons.Reset, 3, trailing = null) { resetting = true } })
        },
        Category("Help & feedback", TableIcons.Help, 5, 3,
            summary = { tr("Documentation, report a bug") },
            keywords = listOf("Documentation", "Take the tour", "Report a bug", "Request a feature", "Acknowledgements", "Licenses", "Version", "Privacy"),
        ) {
            PxCards(
                { LinkRow("Documentation", "${Docs.chapters.size} " + tr("chapters with") + " ${Docs.allExamples.size} " + tr("worked examples, every key, and the spreadsheet functions"), TableIcons.Help, 5) { howTo = true } },
                { LinkRow("Take the tour", "A one-minute look at the calculator", TableIcons.ModeCalculator, 2) { Tour.start(); onBack() } },
            )
            PxHeader("Feedback")
            PxCards(
                { LinkRow("Report a bug", "An email to the developer with the app's log attached", TableIcons.Bug, WARNING) { scope.launch { Feedback.reportBug(context) } } },
                { LinkRow("Request a feature", "An email to the developer with your idea", TableIcons.Edit, 7) { Feedback.requestFeature(context) } },
            )
            PxHeader("About")
            PxCards(
                { LinkRow("Acknowledgements", "Fonts, libraries, data and methods, with links", TableIcons.Info, 0) { if (tablet) acknowledgements = true else go("Acknowledgements") } },
                { InfoRow("Version", appVersion(context)) },
            )
            PxFootnote("The app works offline. Nothing you type leaves your phone, except a bug report you choose to send.")
        },
    )

    if (tablet) {
        // Two panes, as before: the categories down the left, the chosen one on the right.
        SectionedPage("Settings", onBack = onBack, sections = categories.map { c -> PageSection(c.title, c.icon) { c.content(this) } })
        if (tabsPage) CalcTabsPage(onBack = { tabsPage = false })
        if (acknowledgements) AcknowledgementsDialog(onDismiss = { acknowledgements = false })
    } else {
        val page = rememberPageMotion(onBack)
        val back = { if (stack.isNotEmpty()) stack = stack.dropLast(1) else page.close() }
        Dialog(onDismissRequest = back, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            BackHandler(onBack = back)
            page.Content {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    // Deeper pages come in from the right over the one before; going back reverses it.
                    androidx.compose.animation.AnimatedContent(
                        targetState = stack,
                        transitionSpec = {
                            val d = if (targetState.size >= initialState.size) 1 else -1
                            val slide = AppSettings.expressiveMotion
                            val enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(210, delayMillis = 60)) +
                                (if (slide) androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(330, easing = androidx.compose.animation.core.CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f))) { w -> d * w / 5 } else androidx.compose.animation.EnterTransition.None)
                            val exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120)) +
                                (if (slide) androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(330, easing = androidx.compose.animation.core.CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f))) { w -> -d * w / 5 } else androidx.compose.animation.ExitTransition.None)
                            enter togetherWith exit
                        },
                        label = "settings page",
                    ) { s ->
                        val route = s.lastOrNull()
                        PageFrame(route ?: "Settings", back) {
                            when (route) {
                                null -> MainPage(categories, go)
                                "Acknowledgements" -> AcknowledgementsContent()
                                "Calculator tabs" -> Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(horizontal = 8.dp)) { CalcTabsContent() }
                                else -> categories.firstOrNull { it.title == route }?.content?.invoke(this)
                            }
                        }
                    }
                }
            }
        }
    }
    if (howTo) DocsPage(onBack = { howTo = false })
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

/** A settings page: a round back button and a large title, the content scrolling below. */
@Composable
private fun PageFrame(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
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
            content = content,
        )
    }
}

/** The acknowledgements as a settings page: each group under its heading, a card per credit that opens its link. */
@Composable
private fun ColumnScope.AcknowledgementsContent() {
    val colors = MaterialTheme.colorScheme
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    Intro("CAS Calculator is built on the work of many people. Tap any entry to read more.")
    CREDITS.forEach { g ->
        PxHeader(g.title)
        Text(g.intro, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 10.dp))
        PxCards(*g.credits.map { c -> @Composable {
            Column(Modifier.fillMaxWidth().clickable(onClickLabel = "Open ${c.name}") { runCatching { uri.openUri(c.url) } }.then(PxRowPadding), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.name, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, modifier = Modifier.weight(1f))
                    AppIcon(TableIcons.External, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                if (c.by != "—") Text(c.by, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                MathText(tr("Used for:") + " " + c.use, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                if (c.license != "—") Text(c.license, style = MaterialTheme.typography.labelSmall, color = colors.onSecondaryContainer,
                    modifier = Modifier.padding(top = 4.dp).clip(CircleShape).background(colors.secondaryContainer).padding(horizontal = 8.dp, vertical = 2.dp))
            }
        } }.toTypedArray())
    }
}

/** A row that only shows a value (the version). */
@Composable
private fun InfoRow(title: String, value: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).then(PxRowPadding), verticalAlignment = Alignment.CenterVertically) {
        PxRowTexts(title, value, Modifier.weight(1f))
    }
}

/** The installed version, as "1.0 (build 1)". */
private fun appVersion(context: android.content.Context): String = runCatching {
    @Suppress("DEPRECATION")
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    @Suppress("DEPRECATION")
    val code = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
    "${info.versionName} (build $code)"
}.getOrDefault("?")

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
        else PxCards(*hits.take(30).map { (c, k) -> @Composable { LinkRow(k, c.title, c.icon, c.tone, trailing = TableIcons.ChevronRight) { onOpen(c.title) } } }.toTypedArray())
        return
    }
    categories.groupBy { it.group }.values.forEachIndexed { i, group ->
        if (i > 0) Spacer(Modifier.height(16.dp))
        PxCards(*group.map { c -> @Composable { LinkRow(c.title, c.summary(), c.icon, c.tone, trailing = null) { onOpen(c.title) } } }.toTypedArray())
    }
}

/** The three detail sliders, here and in each graph's settings (where [only] picks one). */
@Composable
internal fun DetailCards(only: Int? = null) {
    val steps = AppSettings.DETAIL_STEPS
    val rows: List<@Composable () -> Unit> = listOf(
        { PxSteppedRow("2D fields", steps[AppSettings.fieldQuality], AppSettings.fieldQuality, 0..4, AppSettings::changeFieldQuality) },
        { PxSteppedRow("3D surfaces", steps[AppSettings.surfaceDetail], AppSettings.surfaceDetail, 0..4, AppSettings::changeSurfaceDetail) },
        { PxSteppedRow("Complex plots", steps[AppSettings.complexQuality], AppSettings.complexQuality, 0..4, AppSettings::changeComplexQuality) },
    )
    PxCards(*(if (only == null) rows else listOf(rows[only])).toTypedArray())
}

// ---- Building blocks ---------------------------------------------------------------------

/**
 * Inside a dialog (the graphs' settings) the building blocks are drawn compact and flat, as the
 * color and colormap dialogs are: section names as small labels, rows without cards, choices as
 * segmented buttons and steps as a plain slider.
 */
internal val LocalCompactRows = androidx.compose.runtime.staticCompositionLocalOf { false }

/** A group of cards: the first and last have large outer corners, the ones between small, with thin gaps. Null rows are left out. */
@Composable
internal fun PxCards(vararg rows: (@Composable () -> Unit)?) {
    val shown = rows.filterNotNull()
    val colors = MaterialTheme.colorScheme
    if (LocalCompactRows.current) { Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { shown.forEach { it() } }; return }
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
internal fun PxHeader(text: String) {
    if (LocalCompactRows.current) Text(tr(text), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
    else Text(tr(text), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, top = 24.dp, bottom = 10.dp))
}

@Composable
private fun Intro(text: String) =
    Text(tr(text), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp))

/** An ⓘ and a line underneath the settings. */
@Composable
internal fun PxFootnote(text: String) {
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
internal fun PxSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked, onCheckedChange = onChange,
        thumbContent = { AppIcon(if (checked) TableIcons.Check else TableIcons.Close, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) },
    )
}

internal val PxRowPadding = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)

@Composable
internal fun PxRowTexts(title: String, detail: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        MathText(tr(title), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = MaterialTheme.colorScheme.onSurface)
        if (detail != null) MathText(tr(detail), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A setting that's on or off: the whole card toggles it. */
@Composable
internal fun PxToggleRow(title: String, detail: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    if (LocalCompactRows.current) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Switch) { onChange(!checked) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                MathText(tr(title), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                if (detail != null) MathText(tr(detail), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            PxSwitch(checked, onChange)
        }
        return
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(role = Role.Switch) { onChange(!checked) }.then(PxRowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PxRowTexts(title, detail, Modifier.weight(1f))
        Spacer(Modifier.width(16.dp))
        PxSwitch(checked, onChange)
    }
}

/** The tone of a warning: the only icon circle in the error color (Report a bug). */
private const val WARNING = -1

/**
 * The tint pairs for icon circles: the graph palette (the theme's hues round the color wheel,
 * Material You's when it's on), a soft circle of the hue with the icon in a stronger shade;
 * [WARNING] takes the error colors.
 */
@Composable
private fun tone(t: Int): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    if (t == WARNING) return scheme.errorContainer to scheme.onErrorContainer
    val c = plotColors(scheme)[t.mod(GraphViewModel.PLOT_COLOR_COUNT)]
    val dark = scheme.surface.luminance() < 0.5f
    val bg = c.copy(alpha = if (dark) 0.30f else 0.22f).compositeOver(scheme.surfaceContainerHigh)
    val fg = if (dark) androidx.compose.ui.graphics.lerp(c, Color.White, 0.3f) else androidx.compose.ui.graphics.lerp(c, Color.Black, 0.35f)
    return bg to fg
}

/** A row that opens something, with its icon in a tinted circle; [trailing] is the chevron (or + for adding, or nothing). */
@Composable
private fun LinkRow(title: String, detail: String?, icon: ImageVector?, tone: Int, trailing: ImageVector? = TableIcons.ChevronRight, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(onClickLabel = tr(title), onClick = onClick).then(PxRowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            val (bg, fg) = tone(tone)
            Box(Modifier.size(44.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) { AppIcon(icon, contentDescription = null, tint = fg, accent = fg) }
            Spacer(Modifier.width(16.dp))
        }
        PxRowTexts(title, detail, Modifier.weight(1f))
        if (trailing != null) AppIcon(trailing, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A choice of a few options as connected buttons inside the card. */
@Composable
internal fun PxChoiceRow(title: String, options: List<String>, selected: Int, onChange: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    if (LocalCompactRows.current) {
        // Two options sit beside the name; more go under it, across.
        @Composable fun buttons(modifier: Modifier) = androidx.compose.material3.SingleChoiceSegmentedButtonRow(modifier) {
            options.forEachIndexed { k, name ->
                SegmentedButton(
                    selected = k == selected, onClick = { onChange(k) },
                    shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(k, options.size), icon = {},
                    label = { MathText(tr(name), style = MaterialTheme.typography.labelLarge, color = if (k == selected) colors.onSecondaryContainer else colors.onSurface, maxLines = 1) },
                )
            }
        }
        if (options.size <= 2) Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            MathText(tr(title), style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            buttons(Modifier.width(200.dp))
        } else Column(Modifier.padding(vertical = 4.dp)) {
            MathText(tr(title), style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
            buttons(Modifier.fillMaxWidth().padding(top = 6.dp))
        }
        return
    }
    Column(PxRowPadding) {
        PxRowTexts(title, null)
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
internal fun PxSteppedRow(title: String, detail: String?, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    if (LocalCompactRows.current) {
        Column(Modifier.padding(vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MathText(tr(title), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                if (detail != null) MathText(tr(detail), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ExpressiveSlider(
                value = value.toFloat(),
                onValueChange = { v -> v.roundToInt().let { if (it != value) onChange(it) } },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = range.last - range.first - 1,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = tr(title) },
            )
        }
        return
    }
    Column(PxRowPadding) {
        PxRowTexts(title, detail)
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
