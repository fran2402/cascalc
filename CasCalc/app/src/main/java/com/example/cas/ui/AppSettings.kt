package com.example.cas.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * App-wide settings, kept in preferences and observed by Compose. The calculator's own
 * settings (digits, decimals first) stay in its view model; these are the rest.
 */
object AppSettings {
    private var prefs: SharedPreferences? = null

    /** The app's language code (en, fy, sh); empty follows the system (see [I18n]). */
    var language by mutableStateOf("")
        private set
    /** The graphs whose geometry guide has been seen: p the 2D graph, c the complex plane, s the 3D graph. */
    private var guidesSeen by mutableStateOf("")
    /** The first-run tour has been taken or skipped. */
    var tourSeen by mutableStateOf(true)
        private set
    fun markTourSeen() { tourSeen = true; save("tourSeen", true) }
    fun geometryGuideSeen(space: Char) = space in guidesSeen
    fun markGeometryGuideSeen(space: Char) { if (space !in guidesSeen) { guidesSeen += space; save("geometryGuides", guidesSeen) } }
    /** The calculator's tabs in the order chosen, by title (see [CalcTabs]); empty is the built-in order. */
    var tabOrder by mutableStateOf<List<String>>(emptyList())
        private set
    /** The tabs chosen to show, by title; null until changed (every tab but the extra ones). */
    var shownTabs by mutableStateOf<Set<String>?>(null)
        private set
    fun changeTabs(order: List<String>, shown: Set<String>) {
        tabOrder = order; shownTabs = shown
        save("tabOrder", order.joinToString("\n")); save("shownTabs", shown.joinToString("\n"))
    }
    /** Back to the built-in tabs, in their order. */
    fun resetTabs() {
        tabOrder = emptyList(); shownTabs = null
        prefs?.edit()?.remove("tabOrder")?.remove("shownTabs")?.apply()
    }
    /** 0 follow the system, 1 light, 2 dark. */
    var theme by mutableStateOf(0)
        private set
    /** Colors from the wallpaper (Android 12+). */
    var dynamicColor by mutableStateOf(true)
        private set
    /** The color the scheme is built from when dynamic color is off (ARGB); 0 is the built-in olive. */
    var themeColor by mutableStateOf(0)
        private set
    var haptics by mutableStateOf(true)
        private set
    /** Digits in groups of three: 1 000 000. */
    var groupDigits by mutableStateOf(true)
        private set
    /** Five steps, 0 small to 4 large (2 medium). */
    var mathSize by mutableStateOf(2)
        private set
    var keypadSize by mutableStateOf(2)
        private set
    /** On tablets (the wide layout): the keypad on the left (0) or the right (1). */
    var keypadSide by mutableStateOf(1)
    /** On tablets, the width of the graphs' list of lines, in dp (dragged at its edge). */
    var graphListWidth by mutableStateOf(320)
        private set
    var showGrid by mutableStateOf(true)
    /** Plots (figures after matplotlib) in notebooks and from the 2D graph: off until turned on. */
    var plots by mutableStateOf(false)
    /** Notebooks (the ⋮ menu's Notebooks): off until turned on. */
    var notebooks by mutableStateOf(false)
    /**
     * The settings for features still being finished (Plots, Notebooks): hidden, and those features
     * off, until the code is typed into the calculator (see [isLabsCode]).
     */
    var labs by mutableStateOf(false)
        private set
    /** A legend with each line's name, on the graphs and in exported ones. */
    var showLegend by mutableStateOf(true)
        private set
    /** Numbers along the 2D graph's axes. */
    var axisNumbers by mutableStateOf(true)
        private set

    /** Numbers: 0 auto, 1 scientific (a × 10ⁿ), 2 engineering (n a multiple of 3). */
    var numberFormat by mutableStateOf(0)
        private set
    /** Numbers with more digits than this show as a × 10ⁿ (exact whole numbers too); 5 to 30. */
    var sciAfter by mutableStateOf(10)
        private set
    /** Decimals in a of a × 10ⁿ; 1 to 12. */
    var sciDecimals by mutableStateOf(6)
        private set
    /** Complex decimal answers as r·e^{iθ} instead of a + bi. */
    var polarComplex by mutableStateOf(false)
        private set
    /** The answer shown under the input while typing. */
    var livePreview by mutableStateOf(true)
        private set
    /** Worked steps for integrals, derivatives, limits, sums and ∮ loop integrals, from the history (always on). */
    val showSteps get() = true
    /** Pictures instead of labels on the function keys (π, e, ∫, Σ, the statistics and complex keys), always on. */
    val keyIcons get() = true
    /** The unit converter in the ⋮ menu. */
    val unitConverter get() = true
    /** Excel-style formulas (=SUM(A:A), =B1*2…) in the data table. */
    val sheetFormulas get() = true
    /** Geometry in the graphs (named points, Segment(A, B), Circle, Intersect…, and the Construct tools), always on. */
    val geometry get() = true
    /** The converter's last value, from and to units, so it opens where it was left. */
    var converterState by mutableStateOf("1\nkm/s/Mpc\n1/s")
        private set
    /** How many calculations the history keeps: 50, 100, 500, or 0 for all. */
    var historyLimit by mutableStateOf(100)
        private set
    var confirmClearHistory by mutableStateOf(true)
        private set
    /** Ask before deleting a single calculation from the history. */
    var confirmDeleteEntry by mutableStateOf(false)
        private set
    /** Long-press a key for its explanation. */
    var keyHelp by mutableStateOf(true)
        private set
    /** An operator straight after "=" starts from the answer. */
    var continueFromAnswer by mutableStateOf(true)
        private set
    /** Springy expressive motion, or the calmer standard motion. */
    var expressiveMotion by mutableStateOf(true)
        private set
    var keepScreenOn by mutableStateOf(false)
        private set
    /** Open at the calculator every time, rather than where the app was left. */
    var startInCalculator by mutableStateOf(false)
        private set
    var keySounds by mutableStateOf(false)
        private set
    /** Zeros, extrema and crossings marked on the tapped curve. */
    var specialPoints by mutableStateOf(true)
    /** A faded little graph beside answers that are functions of one letter. */
    var answerPreviews by mutableStateOf(true)
        private set
    /** Half the width of the 2D graph's starting view: 5, 10 or 20. */
    var viewHalfWidth by mutableStateOf(10)
        private set
    /** Complex plots, 0 low to 4 high: see [complexDivisor]. */
    var complexQuality by mutableStateOf(0)
        private set
    /** 2D fields f(x, y), 0 low to 4 high: see [fieldCell]. */
    var fieldQuality by mutableStateOf(0)
        private set
    /** 3D surfaces, 0 low to 4 high: see [surfaceGrid]. */
    var surfaceDetail by mutableStateOf(0)
        private set

    val mathScale: Float get() = listOf(0.85f, 0.92f, 1f, 1.09f, 1.18f)[mathSize.coerceIn(0, 4)]
    val keypadScale: Float get() = listOf(0.88f, 0.94f, 1f, 1.07f, 1.14f)[keypadSize.coerceIn(0, 4)]

    /** The names of the five steps of every detail setting, low to high. */
    val DETAIL_STEPS = listOf("Low", "Medium-low", "Medium", "Medium-high", "High")
    val MATH_STEPS = listOf("Small", "Medium-small", "Medium", "Medium-large", "Large")
    val KEYPAD_STEPS = listOf("Compact", "Medium-compact", "Medium", "Medium-tall", "Tall")

    /** A 2D field's cell size in pixels at the chosen detail. */
    val fieldCell: Int get() = listOf(8, 6, 4, 3, 2)[fieldQuality.coerceIn(0, 4)]

    /**
     * The complex plane's sharpest pass, as screen pixels per sample: 2 (half resolution) up to
     * every pixel, then a little finer than the screen at the top (drawn scaled down, so edges are smooth).
     */
    val complexDivisor: Float get() = listOf(2f, 1.5f, 1f, 0.85f, 0.7f)[complexQuality.coerceIn(0, 4)]

    fun init(context: Context) {
        val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs = p
        I18n.load(context)
        language = p.getString("language", "") ?: ""
        guidesSeen = p.getString("geometryGuides", "") ?: ""
        tourSeen = p.getBoolean("tourSeen", false)
        tabOrder = p.getString("tabOrder", null)?.split('\n')?.filter { it.isNotEmpty() } ?: emptyList()
        shownTabs = p.getString("shownTabs", null)?.split('\n')?.filter { it.isNotEmpty() }?.toSet()
        SavedSymbols.init(context)
        PinnedKeys.init(context)
        FavoriteColormaps.init(context)
        PinnedUnits.init(context)
        theme = p.getInt("theme", 0)
        dynamicColor = p.getBoolean("dynamicColor", true)
        themeColor = p.getInt("themeColor", 0)
        haptics = p.getBoolean("haptics", true)
        groupDigits = p.getBoolean("groupDigits", true)
        // Until changed: small math and a compact keypad on phones, medium on tablets.
        val size = if (context.resources.configuration.smallestScreenWidthDp >= 600) 1 else 0
        // Five steps now; the three of before (small, medium, large) were 0, 1, 2.
        mathSize = p.getInt("mathLevel", p.getInt("mathSize", size) * 2).coerceIn(0, 4)
        keypadSize = p.getInt("keypadLevel", p.getInt("keypadSize", size) * 2).coerceIn(0, 4)
        keypadSide = p.getInt("keypadSide", 1)
        graphListWidth = p.getInt("graphListWidth", 320)
        showGrid = p.getBoolean("showGrid", true)
        labs = p.getBoolean("labs", false)
        // Off for everyone else, whatever was set before.
        plots = labs && p.getBoolean("plots", false)
        notebooks = labs && p.getBoolean("notebooks", false)
        showLegend = p.getBoolean("showLegend", true)
        axisNumbers = p.getBoolean("axisNumbers", true)
        numberFormat = p.getInt("numberFormat", 0)
        sciAfter = p.getInt("sciAfter", 10).coerceIn(5, 30)
        sciDecimals = p.getInt("sciDecimals", 6).coerceIn(1, 12)
        polarComplex = p.getBoolean("polarComplex", false)
        livePreview = p.getBoolean("livePreview", true)
        converterState = p.getString("converterState", null) ?: converterState
        historyLimit = p.getInt("historyLimit", 100)
        confirmClearHistory = p.getBoolean("confirmClearHistory", true)
        confirmDeleteEntry = p.getBoolean("confirmDeleteEntry", false)
        keyHelp = p.getBoolean("keyHelp", true)
        continueFromAnswer = p.getBoolean("continueFromAnswer", true)
        expressiveMotion = p.getBoolean("expressiveMotion", true)
        keepScreenOn = p.getBoolean("keepScreenOn", false)
        startInCalculator = p.getBoolean("startInCalculator", false)
        keySounds = p.getBoolean("keySounds", false)
        specialPoints = p.getBoolean("specialPoints", true)
        answerPreviews = p.getBoolean("answerPreviews", true)
        viewHalfWidth = p.getInt("viewHalfWidth", 10)
        // Likewise; complex quality was stored as 2 low, 0 medium, 1 high.
        complexQuality = p.getInt("complexLevel", when (p.getInt("complexQuality", 2)) { 0 -> 2; 1 -> 4; else -> 0 }).coerceIn(0, 4)
        surfaceDetail = p.getInt("surfaceLevel", p.getInt("surfaceDetail", 0) * 2).coerceIn(0, 4)
        fieldQuality = p.getInt("fieldLevel", p.getInt("fieldQuality", 0) * 2).coerceIn(0, 4)
        com.example.cas.engine.Formatter.groupDigits = groupDigits
        com.example.cas.engine.Formatter.numberFormat = numberFormat
        com.example.cas.engine.Formatter.sciAfter = sciAfter
        com.example.cas.engine.Formatter.sciDecimals = sciDecimals
        com.example.cas.engine.Formatter.polarComplex = polarComplex
    }

    /** The settings page's own keys, cleared by [resetAll] (not the history, graphs, symbols, pins or tabs). */
    private val RESETTABLE = listOf(
        "language", "theme", "dynamicColor", "themeColor", "haptics", "groupDigits", "mathSize", "keypadSize", "keypadSide",
        "showGrid", "plots", "notebooks", "showLegend", "axisNumbers", "numberFormat", "sciAfter", "sciDecimals", "polarComplex", "livePreview",
        "historyLimit", "confirmClearHistory", "confirmDeleteEntry", "keyHelp", "continueFromAnswer", "expressiveMotion",
        "keepScreenOn", "startInCalculator", "keySounds", "specialPoints", "answerPreviews", "viewHalfWidth", "complexQuality", "surfaceDetail", "fieldQuality",
        "mathLevel", "keypadLevel", "complexLevel", "surfaceLevel", "fieldLevel",
    )

    /** Every setting back to its default, saved, and in effect at once. */
    fun resetAll(context: Context) {
        val p = prefs ?: context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        p.edit().apply { RESETTABLE.forEach { remove(it) } }.commit()
        init(context)
    }

    fun changeGraphListWidth(dp: Int) { graphListWidth = dp; save("graphListWidth", dp) }

    private fun save(key: String, value: Any) {
        prefs?.edit()?.apply {
            when (value) { is Int -> putInt(key, value); is Boolean -> putBoolean(key, value); is String -> putString(key, value) }
            apply()
        }
    }

    fun changeLanguage(v: String) { language = v; save("language", v) }
    fun changeTheme(v: Int) { theme = v; save("theme", v) }
    fun changeDynamicColor(v: Boolean) { dynamicColor = v; save("dynamicColor", v) }
    fun changeThemeColor(v: Int) { themeColor = v; save("themeColor", v) }
    fun changeHaptics(v: Boolean) { haptics = v; save("haptics", v) }
    fun changeGroupDigits(v: Boolean) { groupDigits = v; com.example.cas.engine.Formatter.groupDigits = v; save("groupDigits", v) }
    fun changeMathSize(v: Int) { mathSize = v; save("mathLevel", v) }
    fun changeKeypadSize(v: Int) { keypadSize = v; save("keypadLevel", v) }
    fun changeKeypadSide(v: Int) { keypadSide = v; save("keypadSide", v) }
    fun changeShowGrid(v: Boolean) { showGrid = v; save("showGrid", v) }
    fun changePlots(v: Boolean) { plots = v; save("plots", v) }
    fun changeNotebooks(v: Boolean) { notebooks = v; save("notebooks", v) }

    /** The code that, typed into the calculator and entered, shows the settings for Plots and Notebooks: kept as its SHA-256 only. */
    private const val LABS_HASH = "3622e8bf59c66a27e9214846f24356900c5f021fd43293b100b18fbccccbb6d6"

    /** Whether [typed] is the Labs code. */
    fun isLabsCode(typed: String): Boolean = java.security.MessageDigest.getInstance("SHA-256").digest(typed.toByteArray()).joinToString("") { "%02x".format(it) } == LABS_HASH
    fun unlockLabs() { labs = true; save("labs", true) }
    fun changeShowLegend(v: Boolean) { showLegend = v; save("showLegend", v) }
    fun changeAxisNumbers(v: Boolean) { axisNumbers = v; save("axisNumbers", v) }
    fun changeSciDecimals(v: Int) { sciDecimals = v; com.example.cas.engine.Formatter.sciDecimals = v; save("sciDecimals", v) }
    fun changeSciAfter(v: Int) { sciAfter = v; com.example.cas.engine.Formatter.sciAfter = v; save("sciAfter", v) }
    fun changeNumberFormat(v: Int) { numberFormat = v; com.example.cas.engine.Formatter.numberFormat = v; save("numberFormat", v) }
    fun changePolarComplex(v: Boolean) { polarComplex = v; com.example.cas.engine.Formatter.polarComplex = v; save("polarComplex", v) }
    fun changeLivePreview(v: Boolean) { livePreview = v; save("livePreview", v) }
    fun changeConverterState(value: String, from: String, to: String) { converterState = "$value\n$from\n$to"; save("converterState", converterState) }
    fun changeHistoryLimit(v: Int) { historyLimit = v; save("historyLimit", v) }
    fun changeConfirmClearHistory(v: Boolean) { confirmClearHistory = v; save("confirmClearHistory", v) }
    fun changeConfirmDeleteEntry(v: Boolean) { confirmDeleteEntry = v; save("confirmDeleteEntry", v) }
    fun changeKeyHelp(v: Boolean) { keyHelp = v; save("keyHelp", v) }
    fun changeContinueFromAnswer(v: Boolean) { continueFromAnswer = v; save("continueFromAnswer", v) }
    fun changeExpressiveMotion(v: Boolean) { expressiveMotion = v; save("expressiveMotion", v) }
    fun changeKeepScreenOn(v: Boolean) { keepScreenOn = v; save("keepScreenOn", v) }
    fun changeStartInCalculator(v: Boolean) { startInCalculator = v; save("startInCalculator", v) }
    fun changeKeySounds(v: Boolean) { keySounds = v; save("keySounds", v) }
    fun changeSpecialPoints(v: Boolean) { specialPoints = v; save("specialPoints", v) }
    fun changeAnswerPreviews(v: Boolean) { answerPreviews = v; save("answerPreviews", v) }
    fun changeViewHalfWidth(v: Int) { viewHalfWidth = v; save("viewHalfWidth", v) }
    fun changeComplexQuality(v: Int) { complexQuality = v; save("complexLevel", v) }
    fun changeFieldQuality(v: Int) { fieldQuality = v; save("fieldLevel", v) }
    fun changeSurfaceDetail(v: Int) { surfaceDetail = v; save("surfaceLevel", v) }

    /** The explicit and implicit grid sizes for 3D surfaces at the chosen detail. */
    /**
     * Grid sizes for z = f(x, y) and for implicit surfaces at each 3D detail level. Surfaces are
     * sampled on every core and drawn in one call, so these are finer than they used to be (24,
     * 36 and 52; 16, 22 and 30).
     */
    val surfaceGrid: Pair<Int, Int> get() = listOf(40 to 20, 52 to 25, 64 to 30, 80 to 35, 96 to 40)[surfaceDetail.coerceIn(0, 4)]
}

/**
 * The calculator's tabs as set in Settings › Keypad › Calculator tabs: their order, and which are shown.
 * By position in [FunctionTabs]; the extra tabs start hidden. At least one tab always shows.
 */
object CalcTabs {
    /** Every tab, in the order chosen (any tab the saved order doesn't know goes at the end). */
    fun order(): List<Int> {
        val byTitle = FunctionTabs.withIndex().associate { (i, t) -> t.title to i }
        val saved = AppSettings.tabOrder.mapNotNull { byTitle[it] }
        return saved + FunctionTabs.indices.filter { it !in saved }
    }

    /** Whether a tab shows: chosen in Settings, or by default every tab but the extra ones. */
    fun isShown(i: Int): Boolean = AppSettings.shownTabs?.let { FunctionTabs[i].title in it } ?: !FunctionTabs[i].optional

    /** The tabs shown above the keypad, in order. */
    fun shown(): List<Int> = order().filter { isShown(it) }.ifEmpty { listOf(0) }

    /** The tab to show for a selected one: itself if it's shown, else the first shown. */
    fun current(selected: Int): Int = shown().let { s -> if (selected in s) selected else s.first() }
}

/** How many groups the tab bar fits across before it scrolls: the built-in ones. */
val DEFAULT_TAB_COUNT: Int get() = FunctionTabs.count { !it.optional }
