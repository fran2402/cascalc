package com.example.cas.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.drop
import com.example.cas.cas.Expr
import com.example.cas.cas.MathError
import com.example.cas.editor.Editor
import com.example.cas.editor.MathCodec
import com.example.cas.editor.MathRow
import com.example.cas.editor.Matrix
import com.example.cas.editor.Sym
import com.example.cas.editor.row
import com.example.cas.engine.AngleUnit
import com.example.cas.engine.Answer
import com.example.cas.engine.Evaluator
import com.example.cas.engine.Formatter
import com.example.cas.engine.GraphRequest
import com.example.cas.engine.Graphing
import com.example.cas.cas.Flt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class HistoryItem(val expression: MathRow, val answer: Answer, decimalFirst: Boolean = false, val time: Long = System.currentTimeMillis()) {
    /** Showing the decimal approximation instead of the exact answer. */
    var showApprox by mutableStateOf((decimalFirst || answer.preferApprox) && answer.approx != null)

    /** What this result would add to a graph, if anything ("graph this"). */
    val graph: GraphRequest? by lazy { runCatching { Graphing.request(expression, answer.value) }.getOrNull() }

    /** Pinned: kept whatever the history limit, and listed under Pinned. */
    var pinned by mutableStateOf(false)
    /** The folder it's filed in, if any. */
    var folder by mutableStateOf<String?>(null)

    /** The question and answer as plain text and as LaTeX, lowercase without spaces, for searching. */
    val searchText: String by lazy {
        val latex = runCatching { com.example.cas.engine.Latex.of(expression) + "=" + com.example.cas.engine.Latex.of(answer.exact) }.getOrDefault("")
        (Formatter.plain(expression) + "=" + Formatter.plain(answer.exact) + "≈" + (answer.approx?.let { Formatter.plain(it) } ?: "")).lowercase().replace(" ", "") +
            "\n" + normalizeLatex(latex)
    }

    companion object {
        /** LaTeX for matching: without sizing (\left, \right), spacing or spaces, lowercase. */
        fun normalizeLatex(t: String) = t.replace("\\left", "").replace("\\right", "").replace("\\,", "").replace("\\;", "").replace("\\!", "")
            .replace("\\mathrm{d}", "d").replace("\\operatorname", "").replace("{", "").replace("}", "").replace(" ", "").lowercase()
    }
}

class CalculatorViewModel(app: Application) : AndroidViewModel(app), KeypadHost {
    private val prefs = app.getSharedPreferences("calculator", Context.MODE_PRIVATE)

    val editor = Editor()

    /** Bumped on every edit so the math redraws. */
    var version by mutableIntStateOf(0)
        private set

    val history = mutableStateListOf<HistoryItem>()

    /** Values stored with :=, e.g. a := 5. */
    val variables = mutableStateMapOf<String, Expr>()

    init {
        KeypadState.init(prefs)
        // Rad/Deg and the rest can change in another mode: the preview follows.
        viewModelScope.launch {
            androidx.compose.runtime.snapshotFlow { Triple(KeypadState.angle, KeypadState.unitSystem, KeypadState.coordinates) }
                .drop(1).collect { schedulePreview() }
        }
    }
    // Shared with the graphs: the keypad stays as it was when switching modes.
    val angle get() = KeypadState.angle
    var historyMode by mutableStateOf(false)
    var preview by mutableStateOf<Answer?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set

    private var lastValue: Expr? = null
    private var previewJob: Job? = null

    // ---- Settings ----------------------------------------------------------------------

    /** Significant digits in decimal answers. */
    var digits by mutableIntStateOf(prefs.getInt("digits", 10).coerceIn(4, 15))
        private set

    /** Show the decimal first on answers that have both forms. */
    var decimalFirst by mutableStateOf(prefs.getBoolean("decimalFirst", false))
        private set

    fun changeDigits(n: Int) {
        digits = n.coerceIn(4, 15)
        Formatter.significantDigits = digits
        prefs.edit().putInt("digits", digits).apply()
        // Re-format past answers with the new precision.
        val redone = history.map { HistoryItem(it.expression, Formatter.answer(it.answer.value), decimalFirst).also { n2 -> n2.showApprox = it.showApprox && n2.answer.approx != null } }
        history.clear(); history.addAll(redone)
        schedulePreview()
        save()
    }

    fun changeDecimalFirst(on: Boolean) {
        decimalFirst = on
        prefs.edit().putBoolean("decimalFirst", on).apply()
        history.forEach { it.showApprox = (on || it.answer.preferApprox) && it.answer.approx != null }
    }

    /** Past answers formatted again (after a number setting changed), each shown as the settings say. */
    fun reformat() {
        val redone = history.map { HistoryItem(it.expression, Formatter.answer(it.answer.value), decimalFirst) }
        history.clear(); history.addAll(redone)
        schedulePreview()
        save()
    }

    init {
        Formatter.significantDigits = digits
        editor.onChange = {
            version++
            error = null
            schedulePreview()
        }
        load()
    }

    override var keypadHidden by mutableStateOf(false)

    /** The history card showing its actions; null for the newest. */
    var focused by mutableStateOf<HistoryItem?>(null)

    /** Whether [item]'s card is the one showing its actions. */
    fun isFocused(item: HistoryItem): Boolean = (focused?.takeIf { it in history } ?: history.lastOrNull()) === item

    fun deleteHistory(item: HistoryItem) {
        history.remove(item)
        save()
    }

    // ---- Finding things in the history -------------------------------------------------

    /** Text to find in questions and answers (as typed: "sin", "x^2", "3.14"). */
    var historyQuery by mutableStateOf("")
    /** What's listed: null for everything, [PINNED] for pinned calculations, or a folder's name. */
    var historyFilter by mutableStateOf<String?>(null)

    /** The folders in use, in alphabetical order. */
    val historyFolders: List<String> get() = history.mapNotNull { it.folder }.distinct().sortedBy { it.lowercase() }

    /** The calculations the search and filter leave, oldest first. */
    fun visibleHistory(): List<HistoryItem> {
        val q = historyQuery.trim().lowercase().replace(" ", "").replace("−", "-").replace("*", "×")
        val f = historyFilter
        if (q.isEmpty() && f == null) return history
        return history.filter { item ->
            (f == null || (f == PINNED && item.pinned) || (f != PINNED && item.folder == f)) &&
                (q.isEmpty() || item.searchText.replace("−", "-").contains(q) || (q.contains('\\') || q.contains('{')) && item.searchText.contains(HistoryItem.normalizeLatex(historyQuery)))
        }
    }

    fun togglePin(item: HistoryItem) { item.pinned = !item.pinned; save() }

    /** Files [item] in folder [name] (null or blank takes it out of its folder). */
    fun moveToFolder(item: HistoryItem, name: String?) {
        item.folder = name?.trim()?.takeIf { it.isNotEmpty() }
        if (historyFilter != null && historyFilter != PINNED && historyFilter !in historyFolders) historyFilter = null
        save()
    }

    /** Renames a folder, for every calculation in it. */
    fun renameFolder(from: String, to: String) {
        val name = to.trim().takeIf { it.isNotEmpty() } ?: return
        history.filter { it.folder == from }.forEach { it.folder = name }
        if (historyFilter == from) historyFilter = name
        save()
    }

    /** Empties a folder: its calculations stay in the history, outside any folder. */
    fun removeFolder(name: String) {
        history.filter { it.folder == name }.forEach { it.folder = null }
        if (historyFilter == name) historyFilter = null
        save()
    }

    override val canUndo get() = version.let { editor.canUndo }
    override val canRedo get() = version.let { editor.canRedo }
    override fun undo() = editor.undo()
    override fun redo() = editor.redo()

    /** Puts a number from the graph (a tapped point) into the expression. */
    fun insertNumber(v: Double) = reuse(Formatter.row(Flt(v)))
    fun insertComplex(v: com.example.cas.cas.CD) = reuse(Formatter.row(com.example.cas.cas.Numeric.fromCD(v)))

    override val unitSystem get() = KeypadState.unitSystem

    override fun selectUnitSystem(units: com.example.cas.engine.UnitSystem) {
        KeypadState.selectUnitSystem(units)
        schedulePreview()
    }

    override val coordinates get() = KeypadState.coordinates

    override fun selectCoordinates(c: com.example.cas.cas.Coordinates) {
        KeypadState.selectCoordinates(c)
        schedulePreview()
    }

    /** Letters chosen earlier for a coordinate system (or its defaults). */
    override fun coordinatesOf(kind: com.example.cas.cas.CoordinateKind) = KeypadState.coordinatesOf(kind)

    override val angleUnit get() = KeypadState.angle
    override val panelOpen get() = KeypadState.panelOpen
    override val selectedTab get() = KeypadState.tab

    /** True right after "=", until the next key: an operator then continues from the answer. */
    private var justEvaluated = false

    /** Keys that need something before them, so after "=" they start from "ans" (like Google Calculator). */
    private fun continuesFromAnswer(action: KeyAction) = when (action) {
        is KeyAction.Type -> action.text in setOf("+", "−", "×", "!", "%", "mod")
        KeyAction.Fraction, is KeyAction.Power -> true
        else -> false
    }

    override fun press(action: KeyAction) {
        if (AppSettings.continueFromAnswer && justEvaluated && editor.isEmpty && ansValue() != null && continuesFromAnswer(action)) editor.type("ans")
        justEvaluated = false
        when (action) {
            is KeyAction.Type -> editor.type(action.text)
            is KeyAction.Insert -> {
                editor.insert(action.make(), action.slot)
                if (action.path.isNotEmpty()) editor.enter(action.path)
            }
            KeyAction.Fraction -> editor.insertFraction()
            is KeyAction.Power -> editor.insertPower(action.exponent?.let { row(it) })
            KeyAction.Paren -> editor.smartParen()
            KeyAction.Backspace -> editor.backspace()
            KeyAction.Clear -> editor.clear()
            KeyAction.Enter -> enter()
            is KeyAction.Sequence -> action.steps.forEach { press(it) }
            is KeyAction.Paste -> editor.insertRow(action.row)
            KeyAction.PickMatrix, KeyAction.MoreConstants, KeyAction.OpenSymbolBuilder, KeyAction.ListBrackets -> Unit // handled by the screen (dialogs)
        }
    }

    override fun insertMatrix(rows: Int, cols: Int) = editor.insert(Matrix(rows, cols, growable = true), 0)

    override fun moveLeft() = editor.moveLeft()
    override fun moveRight() = editor.moveRight()
    fun tapAt(row: MathRow, index: Int) = editor.setCursor(row, index)

    override fun toggleAngle() {
        KeypadState.toggleAngle()
        schedulePreview()
    }

    override fun togglePanel() = KeypadState.togglePanel()

    override fun selectTab(index: Int) = KeypadState.selectTab(index)

    /** Puts a copy of a past expression or result at the cursor. */
    fun reuse(content: MathRow) {
        // Matrices from the history can be extended again, as when first typed.
        val copy = com.example.cas.editor.makeMatricesGrowable(content)
        val needsBrackets = copy.items.drop(1).any { (it as? Sym)?.text in setOf("+", "−", "=", ",") } && !editor.isEmpty
        if (needsBrackets) editor.type("(")
        editor.insertRow(copy)
        if (needsBrackets) editor.type(")")
        // From the full-screen history: back to the calculator, where it's been put.
        historyMode = false
    }

    /** An example from a guide, typed in place of what's there. */
    fun tryExample(row: MathRow) {
        editor.clear()
        reuse(row)
    }

    /** Clears the history, except pinned calculations and those in folders. */
    fun clearHistory() {
        history.removeAll { !it.pinned && it.folder == null }
        save()
    }

    /** How many calculations Clear history would remove. */
    val clearableCount: Int get() = history.count { !it.pinned && it.folder == null }

    fun clearVariables() {
        variables.clear()
        save()
    }

    // ---- Evaluation -----------------------------------------------------------

    /** Functions defined with f(x) := …, usable in later calculations as f(2), f′(x)… */
    private val userFunctions = androidx.compose.runtime.mutableStateMapOf<String, com.example.cas.engine.UserFunction>()

    override val definedSymbols: Set<String> get() = variables.keys + userFunctions.keys

    override fun undefine(name: String) {
        variables.remove(name)
        userFunctions.remove(name)
        save()
        schedulePreview()
    }

    /**
     * What Ans stands for: the newest answer still in the history (so deleting a calculation
     * makes Ans the one before it), or the last result when the history is empty or off.
     */
    private fun ansValue(): Expr? = history.lastOrNull()?.answer?.value ?: lastValue

    // Equations are solved on Enter (and in the preview, when it's clear what for).
    private fun evaluator() = Evaluator(angle, ansValue(), variables.toMap(), unitSystem, coordinates, userFunctions.toMap(), solveEquations = true)

    /** An equation with several letters waiting for the user to say what to solve for. */
    class SolveChoice(val row: MathRow, val candidates: List<String>, val count: Int)
    var solveChoice by mutableStateOf<SolveChoice?>(null)
        private set

    /** Solve the waiting equation for [vars] (as many as it asked for). */
    fun solveFor(vars: List<String>) { solveChoice = null; enter(vars) }
    fun dismissSolveChoice() { solveChoice = null }

    private fun schedulePreview() {
        previewJob?.cancel()
        if (editor.isEmpty || !AppSettings.livePreview) { preview = null; return }
        val snapshot = MathCodec.copy(editor.root)
        val evaluator = evaluator()
        previewJob = viewModelScope.launch {
            delay(150)
            preview = withContext(Dispatchers.Default) {
                // Symbolic work can take a while; a preview that isn't ready quickly is skipped.
                withTimeoutOrNull(1500) {
                    runCatching { Formatter.answer(evaluator.evaluate(snapshot)) }.getOrNull()
                }
            }
        }
    }

    private fun enter(solveFor: List<String>? = null) {
        if (editor.isEmpty || busy) return
        // Growable matrices keep only the rows and columns in use.
        val snapshot = com.example.cas.editor.trimMatrices(editor.root)
        val evaluator = evaluator().also { it.solveFor = solveFor }
        busy = true
        viewModelScope.launch {
            // Evaluate and format off the main thread; nothing that goes wrong may crash the app.
            val result = withContext(Dispatchers.Default) {
                try {
                    val v = evaluator.evaluate(snapshot)
                    Result.success(v to Formatter.answer(v))
                } catch (e: com.example.cas.engine.ChooseUnknowns) {
                    Result.failure(e)
                } catch (e: MathError) {
                    Result.failure(e)
                } catch (e: ArithmeticException) {
                    Result.failure(MathError("That's too large to work out"))
                } catch (e: StackOverflowError) {
                    Result.failure(MathError("That's too complicated to work out"))
                } catch (e: OutOfMemoryError) {
                    Result.failure(MathError("That's too large to work out"))
                } catch (e: RuntimeException) {
                    Result.failure(MathError("Couldn't work this out"))
                }
            }
            busy = false
            result.onSuccess { (v, answer) ->
                evaluator.definedFunction?.let { (name, fn) -> userFunctions[name] = fn }
                evaluator.assigned?.let { (name, value) -> variables[name] = value } ?: run { if (evaluator.definedFunction == null) lastValue = v }
                history += HistoryItem(snapshot, answer, decimalFirst)
                focused = null
                // Keep only as many as the history length setting allows (0: all).
                val limit = AppSettings.historyLimit
                while (limit > 0 && history.size > limit) history.removeAt(0)
                justEvaluated = true
                save()
                editor.clear()
                preview = null
            }.onFailure {
                // Several letters: ask which to solve for, then enter again.
                if (it is com.example.cas.engine.ChooseUnknowns) solveChoice = SolveChoice(MathCodec.copy(snapshot), it.candidates, it.count)
                else error = it.message
            }
        }
    }

    // ---- Saving -----------------------------------------------------------------

    private fun save() {
        // The limit counts the loose calculations; pinned ones and those in folders are kept.
        val limit = if (AppSettings.historyLimit > 0) AppSettings.historyLimit else Int.MAX_VALUE
        val loose = history.filter { !it.pinned && it.folder == null }
        val dropped = loose.take(maxOf(0, loose.size - limit)).toHashSet()
        val text = history.filter { it !in dropped }.joinToString("\n") { item ->
            listOf(
                MathCodec.encode(item.expression),
                MathCodec.encode(item.answer.exact),
                item.answer.approx?.let { MathCodec.encode(it) } ?: "",
                if (item.pinned) "p" else "",
                item.folder?.replace("\t", " ")?.replace("\n", " ") ?: "",
                item.time.toString(),
            ).joinToString("\t")
        }
        val vars = variables.entries.joinToString("\n") { (k, v) -> k + "\t" + MathCodec.encode(Formatter.row(v)) }
        prefs.edit().putString("history", text).putString("variables", vars).apply()
    }

    private fun load() {
        prefs.getString("history", "").orEmpty().lines().filter { it.isNotBlank() }.forEach { line ->
            runCatching {
                val parts = line.split("\t")
                val expr = MathCodec.decode(parts[0])
                val exact = MathCodec.decode(parts[1])
                val approx = parts.getOrNull(2)?.takeIf { it.isNotEmpty() }?.let { MathCodec.decode(it) }
                // Answers are saved as 2D rows; parsing one back gives the value again.
                val value = Evaluator().evaluate(exact)
                // Calculations saved before times were kept have 0: listed under "Earlier".
                history += HistoryItem(expr, Answer(value, exact, approx), decimalFirst, parts.getOrNull(5)?.toLongOrNull() ?: 0L).also { item ->
                    item.pinned = parts.getOrNull(3) == "p"
                    item.folder = parts.getOrNull(4)?.takeIf { it.isNotEmpty() }
                }
            }
        }
        prefs.getString("variables", "").orEmpty().lines().filter { it.isNotBlank() }.forEach { line ->
            runCatching {
                val (name, code) = line.split("\t", limit = 2)
                variables[name] = Evaluator().evaluate(MathCodec.decode(code))
            }
        }
        lastValue = history.lastOrNull()?.answer?.value
    }

    companion object {
        private const val MAX_HISTORY = 100
        /** [historyFilter] for the pinned calculations. */
        const val PINNED = "\u0000pinned"
    }
}

/** The coordinate system for ∇ and friends, with the letters chosen for each kind, from settings. */
fun loadCoordinates(prefs: android.content.SharedPreferences): com.example.cas.cas.Coordinates {
    val kind = runCatching { com.example.cas.cas.CoordinateKind.valueOf(prefs.getString("coords", "Cartesian")!!) }.getOrDefault(com.example.cas.cas.CoordinateKind.Cartesian)
    return coordinatesFor(prefs, kind)
}

fun coordinatesFor(prefs: android.content.SharedPreferences, kind: com.example.cas.cas.CoordinateKind): com.example.cas.cas.Coordinates {
    val names = prefs.getString("coords_${kind.name}", null)?.split(",")?.takeIf { it.size == 3 && it.toSet().size == 3 } ?: kind.defaults
    return com.example.cas.cas.Coordinates(kind, names)
}

fun saveCoordinates(prefs: android.content.SharedPreferences, c: com.example.cas.cas.Coordinates) {
    prefs.edit().putString("coords", c.kind.name).putString("coords_${c.kind.name}", c.names.joinToString(",")).apply()
}
