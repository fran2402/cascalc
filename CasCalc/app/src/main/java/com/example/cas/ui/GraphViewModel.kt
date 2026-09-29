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
import com.example.cas.cas.Eq
import com.example.cas.cas.Expr
import com.example.cas.cas.MathError
import com.example.cas.cas.Sym
import com.example.cas.cas.freeVars
import com.example.cas.cas.isConstant
import com.example.cas.cas.freeOf
import com.example.cas.cas.subst
import com.example.cas.editor.Editor
import com.example.cas.editor.MathCodec
import com.example.cas.editor.MathRow
import com.example.cas.editor.Matrix
import com.example.cas.editor.row
import com.example.cas.engine.AngleUnit
import com.example.cas.engine.Evaluator
import com.example.cas.graph.Camera
import com.example.cas.graph.Compiler
import com.example.cas.graph.RealFunction
import com.example.cas.graph.Viewport

/** One function on a graph: its editor, and the compiled version ready to plot. */
class PlotFunction(initial: MathRow, val colorIndex: Int) {
    val editor = Editor(initial)
    var version by mutableIntStateOf(0)
    var visible by mutableStateOf(true)
    var compiled: RealFunction? = null
        internal set
    /** For the complex plane: f(z) compiled to complex arithmetic. */
    var complexCompiled: com.example.cas.graph.ComplexFunction? = null
        internal set
    /** Letters other than the plotting variables, each shown with a slider. */
    var parameters: List<String> = emptyList()
        internal set
    /** For the 2D graph: what kind of curve or region this is, compiled. */
    var plot: Plot2DKind? = null
        internal set
    /** For the 3D graph: an implicit surface F(x, y, z) = 0, compiled in (x, y, z) + sliders; null for z = f(x, y). */
    var implicit3D: RealFunction? = null
        internal set
    /** Line style in the 2D graph: 0 solid, 1 dashed, 2 dotted; and thickness in dp. */
    var lineStyle by mutableStateOf(0)
    var thickness by mutableStateOf(3f)
    /** Conditions after commas (y = x², 0 < x < 2): each compared chain's parts in (x, y, t, θ, r) + sliders. */
    var restrictions: List<Pair<List<RealFunction>, List<String>>> = emptyList()
        internal set
    /** f(t) = … with a variable that isn't plotted: this line only defines f. */
    var definesFunction: String? = null
        internal set
    /** In 3D: a point (a, b, c) or a space curve (x(t), y(t), z(t)), its three coordinates compiled. */
    var space: List<RealFunction>? = null
        internal set
    var spaceIsCurve: Boolean = false
        internal set
    /** "a = 3": this line sets a slider letter instead of being plotted. */
    var definition: Pair<String, Double>? = null
        internal set
    /** On the complex plane: an equation like |z − 1| = 2, drawn as a curve; a real function of (x, y) + sliders. */
    var complexCurve: RealFunction? = null
        internal set
    /** On the complex plane: a ∮ line, drawn as its circle with the integral's value. */
    var contour: ContourCircle? = null
        internal set
    var error by mutableStateOf<String?>(null)
        internal set
    /** A colour picked by long-pressing the dot (ARGB), or null for the theme's colour for [colorIndex]. */
    var customColor by mutableStateOf<Int?>(null)
    /** On the complex plane: the colours for arg f. */
    var colormap by mutableStateOf(com.example.cas.graph.Colormap.CLASSIC)
}

/** A contour integral typed on the complex plane: its circle and its value. */
class ContourCircle(val centerRe: Double, val centerIm: Double, val radius: Double, val value: com.example.cas.cas.Expr)

/**
 * A 2D graph line compiled for drawing. Functions take the plotting variables
 * first ((x), (θ), (t) or (x, y)) and then the slider values.
 */
sealed class Plot2DKind {
    /** The label shown before the typed expression, when the expression doesn't include its own "=". */
    abstract val label: String?

    class Explicit(val f: RealFunction, override val label: String?) : Plot2DKind()
    class Polar(val r: RealFunction, override val label: String?) : Plot2DKind()
    class Parametric(val x: RealFunction, val y: RealFunction) : Plot2DKind() { override val label = "(x, y) =" }
    class Implicit(val f: RealFunction) : Plot2DKind() { override val label: String? = null }
    class Region(val parts: List<RealFunction>, val ops: List<String>) : Plot2DKind() { override val label: String? = null }
    /** A point (a, b) with numbers (or sliders) for coordinates. */
    class Point(val x: RealFunction, val y: RealFunction) : Plot2DKind() { override val label: String? = null }
    /** A list of points [(x₁, y₁), (x₂, y₂), …], drawn as dots and used by Fit. */
    class PointList(val xs: DoubleArray, val ys: DoubleArray) : Plot2DKind() { override val label: String? = null }
    /** A column vector, drawn as an arrow from the origin (or from [fromX], [fromY]). */
    class Vector(val x: RealFunction, val y: RealFunction, val fromX: RealFunction? = null, val fromY: RealFunction? = null) : Plot2DKind() { override val label: String? = null }
}

/**
 * Shared state for the 2D and 3D graphs: a list of functions, each typed on
 * the same keypad as the calculator and compiled for plotting, plus
 * sliders for any extra letters (y = a·sin(bx) gets sliders for a and b).
 */
abstract class GraphViewModel(app: Application, private val key: String, val plotVars: List<String>, defaults: List<MathRow>) :
    AndroidViewModel(app), KeypadHost {

    private val prefs = app.getSharedPreferences("calculator", Context.MODE_PRIVATE)

    val functions = mutableStateListOf<PlotFunction>()
    val parameters = mutableStateMapOf<String, Double>()
    /** Each slider's range, −10 to 10 unless changed. (Declared before init, which loads it.) */
    val ranges = mutableStateMapOf<String, Pair<Double, Double>>()
    /** Saved projects of this kind of graph, newest first. (Declared before init, which loads them.) */
    val projects = mutableStateListOf<Project>()

    /** The function being edited, if the keypad is open. */
    var active by mutableStateOf<PlotFunction?>(null)
        private set

    /** Bumped when anything that changes the picture changes. */
    var version by mutableIntStateOf(0)
        private set

    var angle by mutableStateOf(AngleUnit.valueOf(prefs.getString("angle", AngleUnit.Radians.name)!!))
        private set
    // In the complex plotter the function keys start open on the ℂ tab (ζ, Γ, Re, Im…);
    // real graphs start with just the number pad.
    var tab by mutableIntStateOf(
        prefs.getInt("${key}_tab", if (plotVars == listOf("z")) FunctionTabs.indexOfFirst { it.icon == KeyLabel.Icon(IconId.ComplexC) } else 0)
            .coerceIn(0, FunctionTabs.lastIndex),
    )
        private set
    var panelExpanded by mutableStateOf(prefs.getBoolean("${key}_panel", plotVars == listOf("z")))
        private set

    private var nextColor = 0

    init {
        // Earlier versions started each graph with example functions. If they're still there
        // untouched, start empty instead; anything typed by hand is kept.
        val oldExamples = mapOf(
            "g2" to "fn:sin{'x;}\nfrac{'x;pow{'2;}|'4;}'−;'2;",
            "g3" to "fn:sin{'x;}fn:cos{'y;}",
            "gc" to "frac{'(;'z;pow{'2;}'−;'1;');'(;'z;'−;'2;'−;'i;');pow{'2;}|'z;pow{'2;}'+;'2;'+;'2;'i;}",
        )
        if (prefs.getString("${key}_functions", null) == oldExamples[key]) prefs.edit().remove("${key}_functions").apply()
        applyData({ prefs.getString("${key}_$it", null) }, defaults)
        loadProjects()
        // (Compiled in the init block at the end of the class, once every property exists.)
    }

    /**
     * Fills the graph from its five saved values (lines, colors, styles, slider ranges and slider
     * values), read through [get] by name; used on start-up and to open a saved project.
     */
    private fun applyData(get: (String) -> String?, fallback: List<MathRow> = emptyList()) {
        val rows = get("functions")?.lines()?.filter { it.isNotBlank() }?.mapNotNull { runCatching { MathCodec.decode(it) }.getOrNull() } ?: fallback
        // Each line's color slot, so reopened lines keep their theme colors.
        val slots = get("slots")?.split(",")?.map { it.toIntOrNull()?.takeIf { k -> k in 0 until PLOT_COLOR_COUNT } }
        rows.forEachIndexed { i, r -> addFunction(r, slots?.getOrNull(i)) }
        get("colors")?.split(",")?.forEachIndexed { i, c ->
            functions.getOrNull(i)?.customColor = c.toLongOrNull()?.toInt()
        }
        get("styles")?.split(",")?.forEachIndexed { i, st ->
            // style:thickness, then the colormap's name on the complex plane.
            val parts = st.split(":")
            val (style, width) = parts.getOrNull(0)?.toIntOrNull() to parts.getOrNull(1)?.toFloatOrNull()
            functions.getOrNull(i)?.let { f ->
                style?.let { f.lineStyle = it.coerceIn(0, 2) }; width?.let { f.thickness = it.coerceIn(1f, 10f) }
                f.colormap = com.example.cas.graph.Colormap.byName(parts.getOrNull(2))
            }
        }
        get("ranges").orEmpty().lines().forEach { line ->
            val bits = line.split('\t')
            if (bits.size == 3) { val lo = bits[1].toDoubleOrNull(); val hi = bits[2].toDoubleOrNull(); if (lo != null && hi != null && lo < hi) ranges[bits[0]] = lo to hi }
        }
        get("parameters").orEmpty().lines().filter { '\t' in it }.forEach { line ->
            val (k, v) = line.split('\t'); v.toDoubleOrNull()?.let { parameters[k] = it }
        }
    }

    /** The graph as its saved values (lines, colors, styles, color slots, ranges, sliders), in the forms [save] writes. */
    private fun currentData(): Map<String, String> = mapOf(
        "functions" to functions.joinToString("\n") { MathCodec.encode(it.editor.root) },
        "colors" to functions.joinToString(",") { f -> f.customColor?.let { (it.toLong() and 0xFFFFFFFFL).toString() } ?: "" },
        "styles" to functions.joinToString(",") { f -> "${f.lineStyle}:${f.thickness}:${f.colormap.name}" },
        "slots" to functions.joinToString(",") { it.colorIndex.toString() },
        "ranges" to ranges.entries.joinToString("\n") { "${it.key}\t${it.value.first}\t${it.value.second}" },
        "parameters" to parameters.entries.joinToString("\n") { "${it.key}\t${it.value}" },
    )

    // ---- Saved projects ---------------------------------------------------------------------

    /** A graph saved by name: its lines, colors, styles and sliders. */
    data class Project(val id: Long, val name: String, val savedAt: Long, val data: Map<String, String>)

    private fun loadProjects() {
        projects.clear()
        val json = prefs.getString("${key}_projects", null) ?: return
        runCatching {
            val array = org.json.JSONArray(json)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val d = o.getJSONObject("data")
                projects += Project(o.getLong("id"), o.getString("name"), o.getLong("savedAt"), d.keys().asSequence().associateWith { d.getString(it) })
            }
        }
        projects.sortByDescending { it.savedAt }
    }

    private fun storeProjects() {
        val array = org.json.JSONArray()
        projects.forEach { p ->
            array.put(org.json.JSONObject().apply {
                put("id", p.id); put("name", p.name); put("savedAt", p.savedAt)
                put("data", org.json.JSONObject(p.data))
            })
        }
        prefs.edit().putString("${key}_projects", array.toString()).apply()
    }

    /** Saves the graph as it is now under [name]; saving over an existing name replaces it. */
    fun saveProject(name: String) {
        val clean = name.trim().ifEmpty { "Graph ${projects.size + 1}" }
        val existing = projects.firstOrNull { it.name == clean }
        val p = Project(existing?.id ?: System.currentTimeMillis(), clean, System.currentTimeMillis(), currentData())
        existing?.let { projects.remove(it) }
        projects.add(0, p)
        storeProjects()
    }

    /** Replaces the graph with a saved project. */
    fun openProject(p: Project) {
        active = null
        functions.clear()
        parameters.clear()
        ranges.clear()
        playing.clear()
        applyData({ p.data[it] })
        functions.forEach { recompile(it) }
        version++
        save()
        prefs.edit()
            .putString("${key}_parameters", parameters.entries.joinToString("\n") { "${it.key}\t${it.value}" })
            .putString("${key}_ranges", ranges.entries.joinToString("\n") { "${it.key}\t${it.value.first}\t${it.value.second}" })
            .apply()
    }

    fun renameProject(p: Project, name: String) {
        val i = projects.indexOf(p)
        if (i < 0 || name.isBlank()) return
        projects[i] = p.copy(name = name.trim())
        storeProjects()
    }

    fun deleteProject(p: Project) {
        projects.remove(p)
        storeProjects()
    }

    private fun addFunction(r: MathRow, slot: Int? = null): PlotFunction {
        // A new line takes the first color no other line has (or the next in turn if all six are used).
        val used = functions.map { it.colorIndex }.toSet()
        val index = slot ?: (0 until PLOT_COLOR_COUNT).firstOrNull { it !in used } ?: (nextColor % PLOT_COLOR_COUNT)
        nextColor = index + 1
        val f = PlotFunction(r, index)
        f.editor.onChange = {
            f.version++
            // Lines can use functions defined on other lines, so a list with definitions recompiles whole.
            if (functions.any { com.example.cas.engine.UserFunction.definition(it.editor.root.items) != null }) functions.forEach { recompile(it) }
            else recompile(f)
            save()
        }
        functions += f
        return f
    }

    fun add() {
        val f = addFunction(MathRow())
        active = f
        keypadHidden = false
        version++
        save()
    }

    /** Every point from the list lines on the graph. */
    private fun listPoints(): Pair<DoubleArray, DoubleArray> {
        val lists = functions.mapNotNull { (it.plot as? Plot2DKind.PointList)?.takeIf { _ -> it.visible } }
        return lists.flatMap { it.xs.toList() }.toDoubleArray() to lists.flatMap { it.ys.toList() }.toDoubleArray()
    }

    /** Fit is offered for a function of x with unknowns, and only once the graph has a list of points. */
    fun canFit(f: PlotFunction): Boolean =
        f.plot is Plot2DKind.Explicit && f.parameters.isNotEmpty() && functions.any { it.plot is Plot2DKind.PointList }

    /**
     * Fits the function's unknowns to the list points by least squares, starting from the
     * sliders' values, and sets the sliders to the result (widening their ranges if needed).
     * Returns false if it couldn't be fitted; the sliders are left as they were.
     */
    fun fit(f: PlotFunction): Boolean {
        val compiled = (f.plot as? Plot2DKind.Explicit)?.f ?: return false
        val (xs, ys) = listPoints()
        val names = f.parameters
        val start = DoubleArray(names.size) { parameters[names[it]] ?: 1.0 }
        val model = { x: Double, p: DoubleArray ->
            try { compiled(doubleArrayOf(x) + p) } catch (e: RuntimeException) { Double.NaN }
        }
        val result = com.example.cas.graph.Fit.leastSquares(model, xs, ys, start) ?: return false
        names.forEachIndexed { k, name ->
            val value = result.parameters[k]
            val (lo, hi) = rangeOf(name)
            if (value < lo || value > hi) {
                val span = maxOf(10.0, kotlin.math.abs(value) * 2)
                setSlider(name, value, minOf(lo, -span), maxOf(hi, span))
            } else setParameter(name, value)
        }
        return true
    }

    /** Moves a line from one place in the list to another (dragging rows to reorder them). */
    fun move(from: Int, to: Int) {
        if (from !in functions.indices || to !in functions.indices || from == to) return
        val f = functions.removeAt(from)
        functions.add(to, f)
        version++
        save()
    }

    fun remove(f: PlotFunction) {
        if (active === f) active = null
        functions.remove(f)
        version++
        save()
    }

    fun edit(f: PlotFunction?) {
        active = f
        if (f != null) keypadHidden = false
    }

    fun toggleVisible(f: PlotFunction) { f.visible = !f.visible; version++ }

    fun setParameter(name: String, value: Double) {
        playing.remove(name) // dragging a slider stops its animation
        parameters[name] = value
        version++
        prefs.edit().putString("${key}_parameters", parameters.entries.joinToString("\n") { "${it.key}\t${it.value}" }).apply()
    }

    /** Values stored in the calculator with :=, except the plotting variables themselves. */
    private fun storedVariables(): Map<String, Expr> {
        val out = HashMap<String, Expr>()
        prefs.getString("variables", "").orEmpty().lines().filter { it.isNotBlank() }.forEach { line ->
            runCatching {
                val (name, code) = line.split("\t", limit = 2)
                if (name !in plotVars) out[name] = Evaluator().evaluate(MathCodec.decode(code))
            }
        }
        return out
    }

    private fun recompile(f: PlotFunction) {
        version++
        if (f.editor.isEmpty) { f.compiled = null; f.complexCompiled = null; f.plot = null; f.error = null; return }
        if (plotVars == listOf("x")) { recompile2D(f); return }
        if (plotVars.size == 2) { recompile3D(f); return }
        if (isComplex) { recompileComplex(f); return }
        try {
            var e = evaluatorFor(f).evaluate(MathCodec.copy(f.editor.root))
            // On the complex plane r and θ mean |z| and arg z, so f can be written in polar form.
            if (isComplex) {
                e = e.subst(com.example.cas.cas.Sym("r"), com.example.cas.cas.fn("abs", com.example.cas.cas.Sym("z")))
                    .subst(com.example.cas.cas.Sym("θ"), com.example.cas.cas.fn("arg", com.example.cas.cas.Sym("z")))
            }
            // "y = …" (or "z = …" in 3D) is the same as just "…".
            if (e is Eq) {
                val lhs = e.lhs
                e = if (lhs is Sym && lhs.name == outputVar) e.rhs else throw MathError("Write it as $outputVar = …")
            }
            val params = (e.freeVars() - plotVars.toSet()).sorted()
            params.forEach { if (it !in parameters) parameters[it] = 1.0 }
            if (isComplex) {
                f.complexCompiled = com.example.cas.graph.ComplexCompiler.compile(e, plotVars + params)
                f.compiled = null
            } else {
                f.compiled = Compiler.compile(e, plotVars + params)
            }
            f.parameters = params
            f.error = null
        } catch (ex: MathError) {
            f.compiled = null
            f.complexCompiled = null
            f.error = ex.message
        } catch (ex: StackOverflowError) {
            f.compiled = null; f.error = "That's too complicated to graph"
        } catch (ex: RuntimeException) {
            f.compiled = null
            f.complexCompiled = null
            f.error = "Can't graph this"
        }
    }

    /**
     * 2D graph lines can be y = f(x), r = f(θ), (x(t), y(t)), an equation in x and y,
     * or an inequality; see [com.example.cas.graph.PlotSpec].
     */
    private fun recompile2D(f: PlotFunction) {
        f.definesFunction = null
        f.restrictions = emptyList()
        try {
            val ev = evaluatorFor(f)
            var items: List<com.example.cas.editor.Node> = f.editor.root.items
            // f(x) = …: drawn when its variable is x (as Desmos does); f(t) = … only defines f.
            com.example.cas.engine.UserFunction.definition(items)?.takeIf { it.first !in userFunctions(except = f) }?.let { (name, vs, body) ->
                if (vs != listOf("x")) {
                    ev.evaluate(MathCodec.copy(f.editor.root))
                    f.definesFunction = name
                    f.plot = null; f.compiled = null; f.parameters = emptyList(); f.definition = null; f.error = null
                    return
                }
                items = body
            }
            fun rowOf(nodes: List<com.example.cas.editor.Node>) = MathCodec.copy(MathRow(nodes.toMutableList()))
            // [(x₁, y₁), (x₂, y₂), …]: a list of points.
            if ((items.firstOrNull() as? com.example.cas.editor.Sym)?.text == "[" && (items.lastOrNull() as? com.example.cas.editor.Sym)?.text == "]") {
                val entries = splitTopLevel(items.subList(1, items.size - 1)).filter { it.isNotEmpty() }
                val xs = ArrayList<Double>(); val ys = ArrayList<Double>()
                for (entry in entries) {
                    val pair = splitPair(entry) ?: throw MathError("Write each point as (x, y)")
                    xs += com.example.cas.cas.Numeric.real(ev.evaluate(rowOf(pair[0])))
                    ys += com.example.cas.cas.Numeric.real(ev.evaluate(rowOf(pair[1])))
                }
                if (xs.isEmpty()) throw MathError("Put points in the list, like [(1, 2), (3, 4)]")
                f.plot = Plot2DKind.PointList(xs.toDoubleArray(), ys.toDoubleArray())
                f.compiled = null; f.parameters = emptyList(); f.restrictions = emptyList(); f.definition = null; f.error = null
                return
            }
            // Conditions after commas restrict where the line is drawn: y = x², 0 < x < 2.
            val segments = splitTopLevel(items)
            val curve = segments[0]
            val conditions = segments.drop(1).filter { it.isNotEmpty() }.map { seg ->
                ev.evaluate(rowOf(seg)) as? com.example.cas.cas.Rel ?: throw MathError("After a comma, write a condition like 0 < x < 2")
            }
            // A parametric pair is written (f(t), g(t)): brackets around two parts split by a comma.
            val pair = splitPair(curve)
            val parts = pair?.map { ev.evaluate(rowOf(it)) } ?: listOf(ev.evaluate(rowOf(curve)))
            if (parts.size == 1 && conditions.isEmpty() && asDefinition(f, parts[0])) return
            // (2, 3): a point, when neither coordinate has t in it.
            // A pair is a parametric curve when t runs through both coordinates ((cos t, sin t));
            // if t is in only one, it's a slider and the pair is a point that moves with it ((8, t)).
            val t = com.example.cas.cas.Sym("t")
            // A 2-vector (or a matrix times one) is drawn as an arrow from the origin.
            val vector = parts.singleOrNull()?.let { e ->
                (e as? com.example.cas.cas.Mat)?.takeIf { it.rows == 2 && it.cols == 1 }
            }
            if (vector != null && conditions.isEmpty()) {
                // A vector is a fixed arrow, so every letter in it is a slider (x, y, t included),
                // and letters that already have sliders keep their current values.
                val params = vector.cells.flatMap { it.freeVars() }.distinct().sorted()
                params.forEach { if (it !in parameters) parameters[it] = 1.0 }
                f.plot = Plot2DKind.Vector(Compiler.compile(vector.cells[0], params), Compiler.compile(vector.cells[1], params))
                f.compiled = null; f.parameters = params; f.restrictions = emptyList(); f.definition = null; f.error = null
                return
            }
            val isPoint = parts.size == 2 && parts.any { it.freeOf(t) }
            val spec = if (isPoint) null else com.example.cas.graph.PlotSpec.classify(parts)
            // Sliders: what the kind of line leaves over (y = 3t gives t a slider), and letters in
            // conditions that aren't coordinates.
            val params = ((spec?.parameters ?: parts.flatMap { it.freeVars() }.distinct()) +
                conditions.flatMap { it.freeVars() }.filter { it !in plotLetters }).distinct().sorted()
            params.forEach { if (it !in parameters) parameters[it] = 1.0 }
            val typedEquals = items.any { (it as? com.example.cas.editor.Sym)?.text in setOf("=", "<", ">", "≤", "≥") }
            f.plot = when (spec) {
                null -> Plot2DKind.Point(Compiler.compile(parts[0], params), Compiler.compile(parts[1], params))
                is com.example.cas.graph.PlotSpec.Explicit -> Plot2DKind.Explicit(Compiler.compile(spec.f, listOf("x") + params), if (typedEquals) null else "y =")
                is com.example.cas.graph.PlotSpec.Polar -> Plot2DKind.Polar(Compiler.compile(spec.r, listOf("θ") + params), if (typedEquals) null else "r =")
                is com.example.cas.graph.PlotSpec.Parametric -> Plot2DKind.Parametric(Compiler.compile(spec.x, listOf("t") + params), Compiler.compile(spec.y, listOf("t") + params))
                is com.example.cas.graph.PlotSpec.Implicit -> Plot2DKind.Implicit(Compiler.compile(spec.f, listOf("x", "y") + params))
                is com.example.cas.graph.PlotSpec.Region -> Plot2DKind.Region(spec.rel.parts.map { Compiler.compile(it, listOf("x", "y") + params) }, spec.rel.ops)
            }
            f.restrictions = conditions.map { rel -> rel.parts.map { Compiler.compile(it, listOf("x", "y", "t", "θ", "r") + params) } to rel.ops }
            // Explicit curves keep using the zero/extremum finders.
            f.compiled = (f.plot as? Plot2DKind.Explicit)?.f
            f.parameters = params
            f.definition = null
            f.error = null
        } catch (ex: MathError) {
            f.compiled = null; f.plot = null; f.error = ex.message
        } catch (ex: StackOverflowError) {
            f.compiled = null; f.error = "That's too complicated to graph"
        } catch (ex: RuntimeException) {
            f.compiled = null; f.plot = null; f.error = "Can't graph this"
        }
    }


    // ---- Restored: these were lost when the 2D compile step was rewritten ----------------

    /**
     * Functions defined on lines like f(x) = x², for every other line to use (f(x + 1), f′(x)).
     * Two passes so a definition can use one written below it. [except] leaves a line out
     * (its own definition), so f(x) = f(x) + 1 can't recurse.
     */
    fun userFunctions(except: PlotFunction? = null): Map<String, com.example.cas.engine.UserFunction> {
        val out = LinkedHashMap<String, com.example.cas.engine.UserFunction>()
        repeat(2) {
            for (g in functions) {
                if (g === except || com.example.cas.engine.UserFunction.definition(g.editor.root.items) == null) continue
                runCatching {
                    val ev = Evaluator(angle, null, storedVariables(), unitSystem, coordinates, out.toMap())
                    ev.evaluate(MathCodec.copy(g.editor.root))
                    ev.definedFunction?.let { (name, fn) -> out[name] = fn }
                }
            }
        }
        return out
    }

    private fun evaluatorFor(f: PlotFunction) = Evaluator(angle, null, storedVariables(), unitSystem, coordinates, userFunctions(except = f))

    /** Whether a point passes every condition written after commas on its line. */
    fun allowed(f: PlotFunction, x: Double, y: Double, t: Double = Double.NaN, theta: Double = Double.NaN, r: Double = Double.NaN): Boolean {
        if (f.restrictions.isEmpty()) return true
        for ((parts, ops) in f.restrictions) {
            val v = DoubleArray(parts.size) { k -> call(f, parts[k], x, y, t, theta, r) }
            if (!com.example.cas.graph.Curves.holds(v, ops)) return false
        }
        return true
    }

    /** Splits items at top-level commas (not inside brackets). */
    private fun splitTopLevel(items: List<com.example.cas.editor.Node>): List<List<com.example.cas.editor.Node>> {
        val out = ArrayList<List<com.example.cas.editor.Node>>()
        var depth = 0
        var start = 0
        items.forEachIndexed { k, n ->
            when ((n as? com.example.cas.editor.Sym)?.text) {
                "(" -> depth++
                ")" -> depth--
                "," -> if (depth == 0) { out += items.subList(start, k); start = k + 1 }
            }
        }
        out += items.subList(start, items.size)
        return out
    }

    /** Letters that are coordinates rather than sliders, for each kind of graph. */
    private val plotLetters: Set<String>
        get() = when {
            isComplex -> setOf("z", "w", "x", "y", "r", "θ")
            plotVars.size == 2 -> setOf("x", "y", "z")
            else -> setOf("x", "y", "r", "θ", "t")
        }

    /** "a = 3" (a number for a letter that isn't a coordinate) defines that letter instead of plotting. */
    private fun asDefinition(f: PlotFunction, e: com.example.cas.cas.Expr): Boolean {
        val lhs = (e as? com.example.cas.cas.Eq)?.lhs as? com.example.cas.cas.Sym
        if (e !is com.example.cas.cas.Eq || lhs == null || lhs.name in plotLetters || !e.rhs.isConstant || e.rhs is com.example.cas.cas.Mat) {
            f.definition = null
            return false
        }
        val value = runCatching { com.example.cas.cas.Numeric.real(e.rhs) }.getOrElse { throw MathError("${lhs.name} needs a real number") }
        f.definition = lhs.name to value
        parameters[lhs.name] = value
        f.compiled = null; f.complexCompiled = null; f.plot = null; f.implicit3D = null; f.complexCurve = null; f.contour = null
        f.parameters = emptyList()
        f.error = null
        return true
    }

    /** Letters defined by a line like a = 3; they get no slider. */
    val definedLetters: Set<String> get() = functions.mapNotNull { it.definition?.first }.toSet()

    /**
     * The complex plane: w = f(z) (or just f(z)) is coloured; an equation that's real on
     * both sides (|z − 1| = 2, x² + y² = 4) is drawn as a curve; ∮ around a circle draws the
     * circle with the integral's value. x, y, r, θ mean ℜz, ℑz, |z|, arg z.
     */
    private fun recompileComplex(f: PlotFunction) {
        f.complexCompiled = null; f.compiled = null; f.complexCurve = null; f.contour = null
        try {
            val ev = evaluatorFor(f)
            val single = f.editor.root.items.singleOrNull() as? com.example.cas.editor.Func
            if (single?.name == "contour") {
                // Its circle, from the |z − a| = r written underneath, and its value.
                val value = ev.evaluate(MathCodec.copy(f.editor.root))
                val spec = ev.evaluate(MathCodec.copy(single.args[1])) as? com.example.cas.cas.Eq ?: throw MathError("Write the circle as |z − a| = r")
                val inner = ((spec.lhs as? com.example.cas.cas.Fn)?.takeIf { it.name == "abs" } ?: throw MathError("Write the circle as |z − a| = r")).args[0]
                val z = com.example.cas.cas.Sym("z")
                val cs = com.example.cas.cas.Algebra.coefficients(com.example.cas.cas.Algebra.expand(inner), z) ?: throw MathError("Write the circle as |z − a| = r")
                val center = com.example.cas.cas.Numeric.eval(com.example.cas.cas.Algebra.simplify(com.example.cas.cas.neg(cs[0])))
                f.contour = ContourCircle(center.re, center.im, com.example.cas.cas.Numeric.real(spec.rhs), value)
                f.parameters = emptyList(); f.definition = null; f.error = null
                return
            }
            var e = ev.evaluate(MathCodec.copy(f.editor.root))
            if (asDefinition(f, e)) return
            val z = com.example.cas.cas.Sym("z")
            fun polar(x: com.example.cas.cas.Expr) = x
                .subst(com.example.cas.cas.Sym("r"), com.example.cas.cas.fn("abs", z))
                .subst(com.example.cas.cas.Sym("θ"), com.example.cas.cas.fn("arg", z))
                .subst(com.example.cas.cas.Sym("x"), com.example.cas.cas.fn("Re", z))
                .subst(com.example.cas.cas.Sym("y"), com.example.cas.cas.fn("Im", z))
            if (e is com.example.cas.cas.Eq && (e.lhs as? com.example.cas.cas.Sym)?.name == "w") e = e.rhs
            if (e is com.example.cas.cas.Eq) {
                // A curve: where lhs − rhs = 0, both sides real.
                val g = polar(com.example.cas.cas.sub(e.lhs, e.rhs))
                val params = (g.freeVars() - plotLetters).sorted()
                params.forEach { if (it !in parameters) parameters[it] = 1.0 }
                val c = com.example.cas.graph.ComplexCompiler.compile(g, listOf("z") + params)
                val sample = { x: Double, y: Double, ps: DoubleArray -> c(com.example.cas.cas.CD(x, y), ps) }
                val probe = DoubleArray(params.size) { 1.0 }
                if (listOf(0.3 to 0.7, -1.1 to 0.4, 1.7 to -2.2).any { (x, y) -> sample(x, y, probe).let { v -> v.im.isFinite() && kotlin.math.abs(v.im) > 1e-9 * (1 + kotlin.math.abs(v.re)) } }) {
                    throw MathError("Both sides must be real to draw a curve, like |z − 1| = 2")
                }
                f.complexCurve = RealFunction { args -> sample(args[0], args[1], args.copyOfRange(2, args.size)).re }
                f.parameters = params; f.definition = null; f.error = null
                return
            }
            e = polar(e)
            val params = (e.freeVars() - plotLetters).sorted()
            params.forEach { if (it !in parameters) parameters[it] = 1.0 }
            f.complexCompiled = com.example.cas.graph.ComplexCompiler.compile(e, listOf("z") + params)
            f.parameters = params
            f.definition = null
            f.error = null
        } catch (ex: MathError) {
            f.error = ex.message
        } catch (ex: StackOverflowError) {
            f.error = "That's too complicated to graph"
        } catch (ex: RuntimeException) {
            f.error = "Can't graph this"
        }
    }

    /** 3D lines: z = f(x, y) (or just f(x, y)) is a surface over the plane; any other equation in x, y, z is implicit. */
    private fun recompile3D(f: PlotFunction) {
        f.space = null
        try {
            // (a, b, c): a point; (x(t), y(t), z(t)): a curve in space.
            val items = f.editor.root.items
            val t0 = (items.firstOrNull() as? com.example.cas.editor.Sym)?.text
            val t1 = (items.lastOrNull() as? com.example.cas.editor.Sym)?.text
            if (t0 == "(" && t1 == ")" && items.size >= 7) {
                val parts = splitTopLevel(items.subList(1, items.size - 1))
                if (parts.size == 3 && parts.all { it.isNotEmpty() }) {
                    val ev = evaluatorFor(f)
                    val es = parts.map { ev.evaluate(MathCodec.copy(MathRow(it.toMutableList()))) }
                    val curve = es.any { !it.freeOf(com.example.cas.cas.Sym("t")) }
                    val params = es.flatMap { it.freeVars() }.distinct().filter { it != "t" }.sorted()
                    params.forEach { if (it !in parameters) parameters[it] = 1.0 }
                    f.space = es.map { Compiler.compile(it, (if (curve) listOf("t") else emptyList()) + params) }
                    f.spaceIsCurve = curve
                    f.compiled = null; f.implicit3D = null
                    f.parameters = params; f.definition = null; f.error = null
                    return
                }
            }
            val ev3 = evaluatorFor(f)
            var e = ev3.evaluate(MathCodec.copy(f.editor.root))
            // f(x, y) = … is drawn as the surface z = f(x, y); other definitions only define.
            ev3.definedFunction?.let { (_, fn) ->
                if (fn.variables == listOf("x", "y")) e = fn.body
                else { f.compiled = null; f.implicit3D = null; f.parameters = emptyList(); f.definition = null; f.error = null; return }
            }
            if (asDefinition(f, e)) return
            when (val spec = com.example.cas.graph.PlotSpec3D.classify(e)) {
                is com.example.cas.graph.PlotSpec3D.Explicit -> {
                    spec.parameters.forEach { if (it !in parameters) parameters[it] = 1.0 }
                    f.compiled = Compiler.compile(spec.f, listOf("x", "y") + spec.parameters)
                    f.implicit3D = null
                    f.parameters = spec.parameters
                }
                is com.example.cas.graph.PlotSpec3D.Implicit -> {
                    spec.parameters.forEach { if (it !in parameters) parameters[it] = 1.0 }
                    f.implicit3D = Compiler.compile(spec.f, listOf("x", "y", "z") + spec.parameters)
                    f.compiled = null
                    f.parameters = spec.parameters
                }
            }
            f.error = null
        } catch (ex: MathError) {
            f.compiled = null; f.implicit3D = null; f.error = ex.message
        } catch (ex: StackOverflowError) {
            f.compiled = null; f.implicit3D = null; f.error = "That's too complicated to graph"
        } catch (ex: RuntimeException) {
            f.compiled = null; f.implicit3D = null; f.error = "Can't graph this"
        }
    }

    private fun splitPair(items: List<com.example.cas.editor.Node>): List<List<com.example.cas.editor.Node>>? {
        fun t(n: com.example.cas.editor.Node?) = (n as? com.example.cas.editor.Sym)?.text
        if (items.size < 5 || t(items.first()) != "(" || t(items.last()) != ")") return null
        var depth = 0
        var comma = -1
        for (k in 1 until items.size - 1) {
            when (t(items[k])) { "(" -> depth++; ")" -> depth--; "," -> if (depth == 0) { if (comma >= 0) return null; comma = k } }
            if (depth < 0) return null
        }
        if (comma < 0) return null
        return listOf(items.subList(1, comma), items.subList(comma + 1, items.size - 1))
    }

    /** Evaluates a compiled 2D function at [at] (its plotting variables) with the current slider values. */
    /**
     * [fn] of (x, y) with the sliders read once, for the many evaluations of a shaded region or
     * an equation's curve (reading each slider from Compose state every time is slow).
     */
    fun caller2(f: PlotFunction, fn: RealFunction): (Double, Double) -> Double {
        val args = DoubleArray(2 + f.parameters.size)
        f.parameters.forEachIndexed { i, p -> args[2 + i] = parameters[p] ?: 1.0 }
        return { x, y ->
            args[0] = x; args[1] = y
            try { fn(args) } catch (e: RuntimeException) { Double.NaN }
        }
    }

    /** [allowed] for (x, y) with the sliders read once. */
    fun allowedCaller(f: PlotFunction): (Double, Double) -> Boolean {
        if (f.restrictions.isEmpty()) return { _, _ -> true }
        val nan = Double.NaN
        val checks = f.restrictions.map { (parts, ops) ->
            val calls = parts.map { part ->
                val args = DoubleArray(5 + f.parameters.size)
                f.parameters.forEachIndexed { i, p -> args[5 + i] = parameters[p] ?: 1.0 }
                args[2] = nan; args[3] = nan; args[4] = nan
                val one: (Double, Double) -> Double = { x, y -> args[0] = x; args[1] = y; try { part(args) } catch (e: RuntimeException) { nan } }
                one
            }
            val values = DoubleArray(parts.size)
            val check: (Double, Double) -> Boolean = { x, y -> for (k in calls.indices) values[k] = calls[k](x, y); com.example.cas.graph.Curves.holds(values, ops) }
            check
        }
        return { x, y -> checks.all { it(x, y) } }
    }

    fun call(f: PlotFunction, fn: RealFunction, vararg at: Double): Double {
        val args = DoubleArray(at.size + f.parameters.size)
        at.copyInto(args)
        f.parameters.forEachIndexed { i, p -> args[at.size + i] = parameters[p] ?: 1.0 }
        // Undefined here (a pole, 0/0 on the complex plane…): a gap in the graph, never a crash.
        return try { fn(args) } catch (e: RuntimeException) { Double.NaN }
    }

    // ---- Animated sliders ------------------------------------------------------------

    /** Sliders that are playing, with their direction (+1 or −1). */
    val playing = mutableStateMapOf<String, Int>()

    fun togglePlay(name: String) {
        if (name in playing) {
            playing.remove(name)
            setParameter(name, parameters[name] ?: 1.0) // save where it stopped
        } else playing[name] = 1
    }

    fun rangeOf(name: String) = ranges[name] ?: (-10.0 to 10.0)

    /** Sets a slider's value (typed) and range; the value is kept inside the range. */
    fun setSlider(name: String, value: Double, min: Double, max: Double) {
        ranges[name] = min to max
        setParameter(name, value.coerceIn(min, max))
        prefs.edit().putString("${key}_ranges", ranges.entries.joinToString("\n") { "${it.key}\t${it.value.first}\t${it.value.second}" }).apply()
    }

    /** Moves every playing slider on by [seconds], bouncing between its ends at a tenth of its range a second. */
    fun advance(seconds: Double) {
        for ((name, dir) in playing.toMap()) {
            val (lo, hi) = rangeOf(name)
            var v = (parameters[name] ?: 0.0) + dir * (hi - lo) / 10 * seconds
            var d = dir
            if (v > hi) { v = 2 * hi - v; d = -1 }
            if (v < lo) { v = 2 * lo - v; d = 1 }
            parameters[name] = v
            playing[name] = d
        }
        if (playing.isNotEmpty()) version++
    }

    override val padEquals: Boolean get() = true
    override val listKey: Boolean get() = !isComplex && plotVars == listOf("x")

    /** Letters that sit above the keypad while editing, for typing curves quickly. */
    override val quickVariables: List<String>
        get() = when {
            isComplex -> listOf("z", "x", "y", "r", "θ")
            plotVars.size == 2 -> listOf("x", "y", "z")
            else -> listOf("x", "y", "r", "θ", "t")
        }

    /** Adds a new line started from a template (r =, a parametric pair…). */
    fun addTemplate(template: MathRow, cursorIndex: Int) {
        add()
        active?.editor?.let { ed ->
            ed.load(template)
            ed.setCursor(ed.root, cursorIndex.coerceIn(0, ed.root.items.size))
        }
    }

    /** Plotting f(z) on the complex plane rather than real graphs. */
    val isComplex get() = plotVars == listOf("z")

    private val outputVar get() = when { isComplex -> "w"; plotVars.size == 1 -> "y"; else -> "z" }

    override val mainVariable: String get() = if (isComplex) "z" else "x"

    /** Slider values for [f], in the order its compiled code expects. */
    fun parameterValues(f: PlotFunction) = DoubleArray(f.parameters.size) { parameters[f.parameters[it]] ?: 1.0 }

    /** Evaluates function [f] at a point of the plotting variables, with the current slider values. */
    fun evaluate(f: PlotFunction, vararg at: Double): Double {
        val c = f.compiled ?: return Double.NaN
        val args = DoubleArray(plotVars.size + f.parameters.size)
        at.copyInto(args)
        f.parameters.forEachIndexed { i, p -> args[plotVars.size + i] = parameters[p] ?: 1.0 }
        return try { c(args) } catch (e: RuntimeException) { Double.NaN }
    }

    private fun save() {
        prefs.edit()
            .putString("${key}_functions", functions.joinToString("\n") { MathCodec.encode(it.editor.root) })
            .putString("${key}_colors", functions.joinToString(",") { f -> f.customColor?.let { (it.toLong() and 0xFFFFFFFFL).toString() } ?: "" })
            .putString("${key}_styles", functions.joinToString(",") { f -> "${f.lineStyle}:${f.thickness}:${f.colormap.name}" })
            .putString("${key}_slots", functions.joinToString(",") { it.colorIndex.toString() })
            .apply()
    }

    /** Line style (0 solid, 1 dashed, 2 dotted) and thickness in dp. */
    fun setStyle(f: PlotFunction, style: Int, thickness: Float) {
        f.lineStyle = style; f.thickness = thickness
        version++
        save()
    }

    /** The colours for arg f on the complex plane. */
    fun setColormap(f: PlotFunction, map: com.example.cas.graph.Colormap) {
        f.colormap = map
        version++
        save()
    }

    /** Sets (or with null, resets) a function's colour. */
    fun setColor(f: PlotFunction, argb: Int?) {
        f.customColor = argb
        version++
        save()
    }

    // ---- Showing and hiding the keypad -------------------------------------------------

    override var keypadHidden by mutableStateOf(false)

    // ---- KeypadHost: keys edit the active function -------------------------------

    override var coordinates by mutableStateOf(loadCoordinates(prefs))
        private set

    override fun selectCoordinates(c: com.example.cas.cas.Coordinates) {
        coordinates = c
        saveCoordinates(prefs, c)
        functions.forEach { recompile(it) }
    }

    /** Letters chosen earlier for a coordinate system (or its defaults). */
    override fun coordinatesOf(kind: com.example.cas.cas.CoordinateKind) = coordinatesFor(prefs, kind)

    override val canUndo get() = active?.let { it.version; it.editor.canUndo } ?: false
    override val canRedo get() = active?.let { it.version; it.editor.canRedo } ?: false
    override fun undo() { active?.editor?.undo() }
    override fun redo() { active?.editor?.redo() }

    /** Adds functions sent from the calculator ("graph this"), skipping ones already there. */
    open fun show(rows: List<MathRow>): List<PlotFunction> {
        val existing = functions.associateBy { MathCodec.encode(it.editor.root) }
        val shown = rows.map { r -> existing[MathCodec.encode(r)] ?: addFunction(MathCodec.copy(r)).also { recompile(it) } }
        shown.forEach { it.visible = true }
        active = null
        version++
        save()
        return shown
    }

    override var unitSystem by mutableStateOf(runCatching { com.example.cas.engine.UnitSystem.valueOf(prefs.getString("units", "SI")!!) }.getOrDefault(com.example.cas.engine.UnitSystem.SI))
        private set

    override fun selectUnitSystem(units: com.example.cas.engine.UnitSystem) {
        unitSystem = units
        prefs.edit().putString("units", units.name).apply()
        functions.forEach { recompile(it) }
    }

    override val angleUnit get() = angle
    override val panelOpen get() = panelExpanded
    override val selectedTab get() = tab

    override fun toggleAngle() {
        angle = if (angle == AngleUnit.Radians) AngleUnit.Degrees else AngleUnit.Radians
        prefs.edit().putString("angle", angle.name).apply()
        functions.forEach { recompile(it) }
    }

    override fun togglePanel() {
        panelExpanded = !panelExpanded
        prefs.edit().putBoolean("${key}_panel", panelExpanded).apply()
    }

    override fun selectTab(index: Int) {
        tab = index
        prefs.edit().putInt("${key}_tab", index).apply()
    }

    override fun moveLeft() { active?.editor?.moveLeft() }
    override fun moveRight() { active?.editor?.moveRight() }

    override fun press(action: KeyAction) {
        val ed = active?.editor ?: return
        when (action) {
            is KeyAction.Type -> ed.type(action.text)
            is KeyAction.Insert -> {
                ed.insert(action.make(), action.slot)
                if (action.path.isNotEmpty()) ed.enter(action.path)
            }
            KeyAction.Fraction -> ed.insertFraction()
            is KeyAction.Power -> ed.insertPower(action.exponent?.let { row(it) })
            KeyAction.Paren -> ed.smartParen()
            KeyAction.ListBrackets -> { ed.type("["); ed.type("]"); ed.moveLeft() }
            KeyAction.Backspace -> ed.backspace()
            KeyAction.Clear -> ed.clear()
            // Enter (bottom right) finishes the line, as Back does.
            KeyAction.Enter -> edit(null)
            is KeyAction.Sequence -> action.steps.forEach { press(it) }
            KeyAction.PickMatrix, KeyAction.MoreConstants, KeyAction.OpenSymbolBuilder -> Unit
        }
    }

    override fun insertMatrix(rows: Int, cols: Int) { active?.editor?.insert(Matrix(rows, cols, growable = true), 0) }

    fun tapAt(f: PlotFunction, r: MathRow, index: Int) {
        active = f
        f.editor.setCursor(r, index)
    }

    companion object {
        const val PLOT_COLOR_COUNT = 6
    }

    // Kotlin runs initialisers top to bottom, so saved functions are compiled here, last:
    // compiling needs the unit and coordinate settings declared above. (Doing it in the first
    // init block made every saved graph fail with "Can't graph this" on reopening.)
    init {
        functions.forEach { recompile(it) }
    }
}

class Graph2DViewModel(app: Application) : GraphViewModel(app, "g2", listOf("x"), defaults = emptyList()) {
    /** Null until the screen knows its size. */
    var view by mutableStateOf<Viewport?>(null)

    /** Draw a polar grid (circles and rays) instead of the square grid, and read points as (r, θ). */
    var polarGrid by mutableStateOf(false)

    /** Equal scales on both axes (a circle looks round), keeping the centre and the x range. */
    fun zoomSquare(width: Int, height: Int) {
        val v = view ?: return
        if (width == 0 || height == 0) return
        val cy = (v.yMin + v.yMax) / 2
        val h = v.width * height / width
        view = com.example.cas.graph.Viewport(v.xMin, v.xMax, cy - h / 2, cy + h / 2)
    }

    /** The first point picked with "Area": the function and x = a. The next tap on the graph gives b. */
    var areaStart by mutableStateOf<Pair<PlotFunction, Double>?>(null)
    /** ∫ₐᵇ f dx and the total area between the curve and the axis, once both points are picked. */
    var area by mutableStateOf<AreaResult?>(null)

    fun finishArea(b: Double) {
        val (f, a) = areaStart ?: return
        areaStart = null
        val fn = (f.plot as? Plot2DKind.Explicit)?.f ?: return
        area = runCatching {
            val (signed, total) = com.example.cas.graph.Plot2D.area({ x -> call(f, fn, x) }, a, b)
            AreaResult(f, a, b, signed, total)
        }.getOrElse { AreaResult(f, a, b, Double.NaN, Double.NaN) }
    }

    /** The curve last tapped; its zeros, extrema and crossings are marked. */
    var focus by mutableStateOf<PlotFunction?>(null)

    /** The curve whose special points are shown: the one being edited, else the one tapped. */
    val highlighted: PlotFunction? get() = active ?: focus?.takeIf { it in functions }

    /** Sent from the calculator: highlight the first so its crossings (the solutions) are marked. */
    override fun show(rows: List<MathRow>): List<PlotFunction> = super.show(rows).also { focus = it.firstOrNull() }
}

class Graph3DViewModel(app: Application) : GraphViewModel(app, "g3", listOf("x", "y"), defaults = emptyList()) {
    var camera by mutableStateOf(Camera())
    var xMin by mutableStateOf(-3.0)
    var xMax by mutableStateOf(3.0)
    var yMin by mutableStateOf(-3.0)
    var yMax by mutableStateOf(3.0)
    /** Null: fit z to the surfaces (and use the x range for implicit ones). */
    var zRange by mutableStateOf<Pair<Double, Double>?>(null)

    /** Zooms the x and y ranges about their centres (the − and + buttons). */
    fun scaleRanges(factor: Double) {
        val cx = (xMin + xMax) / 2; val hx = (xMax - xMin) / 2 * factor
        val cy = (yMin + yMax) / 2; val hy = (yMax - yMin) / 2 * factor
        xMin = cx - hx; xMax = cx + hx; yMin = cy - hy; yMax = cy + hy
        zRange = zRange?.let { (a, b) -> val c = (a + b) / 2; val h = (b - a) / 2 * factor; c - h to c + h }
    }
}

/**
 * Complex functions f(z), drawn by domain colouring on the complex plane.
 * The plotted function is the one being edited or tapped, else the first shown.
 */
class ComplexViewModel(app: Application) : GraphViewModel(app, "gc", listOf("z"), defaults = emptyList()) {
    var view by mutableStateOf<Viewport?>(null)
    var options by mutableStateOf(com.example.cas.graph.ColoringOptions())
    /** Circles of constant |z| and rays of constant arg z over the colouring. */
    var polarGrid by mutableStateOf(false)
    /** Drawing a loop for ∮ f dz instead of moving the view. */
    var contourMode by mutableStateOf(false)
    var contour by mutableStateOf<List<com.example.cas.cas.CD>>(emptyList())
    var contourResult by mutableStateOf<com.example.cas.cas.CD?>(null)

    /** The function coloured: the one being edited if it's a function, else the first visible one. */
    val plotted: PlotFunction? get() = active?.takeIf { it.complexCompiled != null }
        ?: functions.firstOrNull { it.visible && it.complexCompiled != null }

    /** Closes the drawn loop and integrates the plotted function around it. */
    fun finishContour() {
        val f = plotted ?: return
        val c = f.complexCompiled ?: return
        contourResult = runCatching { com.example.cas.graph.DomainColoring.contourIntegral(c, parameterValues(f), contour) }.getOrNull()
    }

    fun clearContour() { contour = emptyList(); contourResult = null }
}

/** The area picked on the 2D graph: [signed] = ∫ₐᵇ f dx, [total] = ∫ₐᵇ |f| dx (NaN if it couldn't be computed). */
class AreaResult(val f: PlotFunction, val a: Double, val b: Double, val signed: Double, val total: Double)
