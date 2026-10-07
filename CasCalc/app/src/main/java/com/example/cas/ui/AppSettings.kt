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
    /** 0 small, 1 medium, 2 large. */
    var mathSize by mutableStateOf(1)
        private set
    var keypadSize by mutableStateOf(1)
        private set
    /** On tablets (the wide layout): the keypad on the left (0) or the right (1). */
    var keypadSide by mutableStateOf(1)
    /** On tablets, the width of the graphs' list of lines, in dp (dragged at its edge). */
    var graphListWidth by mutableStateOf(320)
        private set
    var showGrid by mutableStateOf(true)
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
    var keySounds by mutableStateOf(false)
        private set
    /** Zeros, extrema and crossings marked on the tapped curve. */
    var specialPoints by mutableStateOf(true)
        private set
    /** Half the width of the 2D graph's starting view: 5, 10 or 20. */
    var viewHalfWidth by mutableStateOf(10)
        private set
    /** Complex plots: 0 standard (half resolution), 1 high (full), 2 low (a quarter). */
    var complexQuality by mutableStateOf(2)
        private set
    /** 2D fields f(x, y): 0 low (12 px cells), 1 medium (6 px), 2 high (3 px). */
    var fieldQuality by mutableStateOf(0)
        private set
    /** 3D surfaces: 0 low, 1 medium, 2 high detail. */
    var surfaceDetail by mutableStateOf(0)
        private set

    val mathScale: Float get() = when (mathSize) { 0 -> 0.85f; 2 -> 1.18f; else -> 1f }
    val keypadScale: Float get() = when (keypadSize) { 0 -> 0.88f; 2 -> 1.14f; else -> 1f }

    fun init(context: Context) {
        val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs = p
        I18n.load(context)
        language = p.getString("language", "") ?: ""
        guidesSeen = p.getString("geometryGuides", "") ?: ""
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
        mathSize = p.getInt("mathSize", size)
        keypadSize = p.getInt("keypadSize", size)
        keypadSide = p.getInt("keypadSide", 1)
        graphListWidth = p.getInt("graphListWidth", 320)
        showGrid = p.getBoolean("showGrid", true)
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
        keySounds = p.getBoolean("keySounds", false)
        specialPoints = p.getBoolean("specialPoints", true)
        viewHalfWidth = p.getInt("viewHalfWidth", 10)
        complexQuality = p.getInt("complexQuality", 2)
        surfaceDetail = p.getInt("surfaceDetail", 0)
        fieldQuality = p.getInt("fieldQuality", 0)
        com.example.cas.engine.Formatter.groupDigits = groupDigits
        com.example.cas.engine.Formatter.numberFormat = numberFormat
        com.example.cas.engine.Formatter.sciAfter = sciAfter
        com.example.cas.engine.Formatter.sciDecimals = sciDecimals
        com.example.cas.engine.Formatter.polarComplex = polarComplex
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
    fun changeMathSize(v: Int) { mathSize = v; save("mathSize", v) }
    fun changeKeypadSize(v: Int) { keypadSize = v; save("keypadSize", v) }
    fun changeKeypadSide(v: Int) { keypadSide = v; save("keypadSide", v) }
    fun changeShowGrid(v: Boolean) { showGrid = v; save("showGrid", v) }
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
    fun changeKeySounds(v: Boolean) { keySounds = v; save("keySounds", v) }
    fun changeSpecialPoints(v: Boolean) { specialPoints = v; save("specialPoints", v) }
    fun changeViewHalfWidth(v: Int) { viewHalfWidth = v; save("viewHalfWidth", v) }
    fun changeComplexQuality(v: Int) { complexQuality = v; save("complexQuality", v) }
    fun changeFieldQuality(v: Int) { fieldQuality = v; save("fieldQuality", v) }
    fun changeSurfaceDetail(v: Int) { surfaceDetail = v; save("surfaceDetail", v) }

    /** The explicit and implicit grid sizes for 3D surfaces at the chosen detail. */
    /**
     * Grid sizes for z = f(x, y) and for implicit surfaces at each 3D detail level. Surfaces are
     * sampled on every core and drawn in one call, so these are finer than they used to be (24,
     * 36 and 52; 16, 22 and 30).
     */
    val surfaceGrid: Pair<Int, Int> get() = when (surfaceDetail) { 0 -> 40 to 20; 2 -> 96 to 40; else -> 64 to 30 }
}

/**
 * The calculator's tabs as set in Settings › Calculator tabs: their order, and which are shown.
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
