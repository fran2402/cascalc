package com.example.cas.ui

import com.example.cas.editor.BigOp
import com.example.cas.editor.BigOpKind
import com.example.cas.editor.Binom
import com.example.cas.editor.Const
import com.example.cas.editor.Derivative
import com.example.cas.editor.Func
import com.example.cas.editor.Integral
import com.example.cas.editor.MathRow
import com.example.cas.editor.Matrix
import com.example.cas.editor.Node
import com.example.cas.editor.Pow
import com.example.cas.editor.Root
import com.example.cas.editor.Sqrt
import com.example.cas.editor.Scripted
import com.example.cas.editor.Sym
import com.example.cas.editor.row

/** Anything a keypad can drive: the calculator, or a function being edited on a graph. */
interface KeypadHost {
    val angleUnit: com.example.cas.engine.AngleUnit
    val panelOpen: Boolean
    val selectedTab: Int
    fun toggleAngle()
    fun togglePanel()
    fun selectTab(index: Int)
    fun moveLeft()
    fun moveRight()
    fun press(action: KeyAction)
    fun insertMatrix(rows: Int, cols: Int)
    val canUndo: Boolean
    val canRedo: Boolean
    fun undo()
    fun redo()
    /** The keypad slid away (the handle on top hides it, a keyboard button brings it back). */
    var keypadHidden: Boolean
    /** The letter on the number pad's variable key: x, or z when plotting complex functions. */
    val mainVariable: String get() = "x"
    /** Letters shown as chips above the keypad (the graphs' other plotting variables). */
    val quickVariables: List<String> get() = emptyList()
    /** In the graphs, the number pad's variable key is a plain = sign (letters are on the chips). */
    val padEquals: Boolean get() = false
    /** In the 2D graph, the AC key becomes [ ] for lists of points. */
    val listKey: Boolean get() = false
    /** Coordinates for ∇, ∇·, ∇×, ∇², J and H, chosen like Rad/Deg while the ∇ tab is open. */
    val coordinates: com.example.cas.cas.Coordinates
    fun selectCoordinates(c: com.example.cas.cas.Coordinates)
    fun coordinatesOf(kind: com.example.cas.cas.CoordinateKind): com.example.cas.cas.Coordinates
    /** Units for physical constants, chosen like Rad/Deg while the constants tab is open. */
    val unitSystem: com.example.cas.engine.UnitSystem
    fun selectUnitSystem(units: com.example.cas.engine.UnitSystem)
    /** Letters and symbols with a value (a := 5) or a definition (f(x) := …); their keys are colored. */
    val definedSymbols: Set<String> get() = emptySet()
    /** Forgets a letter's value or a function's definition. */
    fun undefine(name: String) {}
}

/** What pressing a key does to the expression. */
sealed interface KeyAction {
    data class Type(val text: String) : KeyAction
    /** Inserts a fresh node from [make]; the cursor moves into slot [slot], or stays after it if null. */
    /**
     * Inserts a node and puts the cursor in [slot]; [path] then steps further in,
     * as (item index, slot index) pairs, e.g. into the inner ∫ of ∬.
     */
    class Insert(val slot: Int?, val path: List<Int> = emptyList(), val make: () -> Node) : KeyAction
    data object Fraction : KeyAction
    data class Power(val exponent: String?) : KeyAction
    data object Paren : KeyAction
    /** [ ] for a list of points in the 2D graph, with the cursor between them. */
    data object ListBrackets : KeyAction
    data object Backspace : KeyAction
    data object Clear : KeyAction
    data object Enter : KeyAction
    /** Opens the symbol builder (handled by the screen). */
    data object OpenSymbolBuilder : KeyAction
    data object PickMatrix : KeyAction
    data object MoreConstants : KeyAction
    class Sequence(vararg val steps: KeyAction) : KeyAction
}

enum class KeyRole { Digit, Operator, Function, Clear, Equals }

/** Pictures for operations that have no standard math symbol. */
enum class IconId {
    Simplify, Expand, Factor, Apart, Together, Answer, MoreConstants,
    // Tab icons (TabIcons.kt)
    Roots, Triangle, Balance, Area, Nabla, Matrix, ComplexC, Atom, Abs, Stats, Letters, PolarGrid, SymbolBuilder,
}

sealed interface KeyLabel {
    data class Text(val text: String) : KeyLabel
    /**
     * Drawn with the math renderer (x², ⁿ√x, log_a(x)); [latex] draws it in Computer Modern
     * like the math itself, as the letters group does, rather than in Google Sans Flex.
     */
    class Math(val row: MathRow, val latex: Boolean = false) : KeyLabel
    data class Icon(val id: IconId) : KeyLabel

    /**
     * A linear-algebra key drawn as a bracketed grid of dots standing for a matrix, with
     * [prefix] before it (det, λ, rref), [sup] raised after it (−1, T, H), or a [second] grid
     * after [op] (·, ×, ∘, ⊗). [wide] is a row vector, [secondTall] a column vector.
     */
    data class Matrix(
        val prefix: String = "",
        val sup: String = "",
        val op: String = "",
        val second: Boolean = false,
        val wide: Boolean = false,
        val secondTall: Boolean = false,
    ) : KeyLabel

    data object BackspaceIcon : KeyLabel
    data object EnterIcon : KeyLabel
}

class KeySpec(val label: KeyLabel, val action: KeyAction, val role: KeyRole, val spoken: String)

private fun text(t: String, action: KeyAction, role: KeyRole = KeyRole.Function, spoken: String = t) =
    KeySpec(KeyLabel.Text(t), action, role, spoken)

private fun math(r: MathRow, action: KeyAction, spoken: String) = KeySpec(KeyLabel.Math(r), action, KeyRole.Function, spoken)

private fun type(t: String, spoken: String = t) = text(t, KeyAction.Type(t), spoken = spoken)


private fun constant(id: String, spoken: String) = math(constantLabel(id), KeyAction.Insert(null) { Const(id) }, spoken)

/** A constant's key label: the constant itself, but a prime (Wien's b′) on the line, not raised. */
private fun constantLabel(id: String): MathRow {
    val pieces = com.example.cas.engine.Constant.byId(id)?.pieces ?: return row(Const(id))
    val p = pieces.singleOrNull()?.takeIf { it.sub.isEmpty() && it.sup.isNotEmpty() && it.sup.all { c -> c == '′' } } ?: return row(Const(id))
    return MathRow((listOf(Sym(p.text)) + p.sup.map { Sym("'") }).toMutableList<com.example.cas.editor.Node>())
}

private fun icon(id: IconId, action: KeyAction, spoken: String) = KeySpec(KeyLabel.Icon(id), action, KeyRole.Function, spoken)

/** A key whose label is written in math notation, e.g. "|A|" for det or "Av = λv" for eigenvectors. */
private fun notation(label: MathRow, action: KeyAction, spoken: String) = math(label, action, spoken)

private fun letters(vararg parts: String) = MathRow(parts.map { Sym(it) }.toMutableList())

/**
 * A tab of keys; [title] is what screen readers say, [icon] is what's shown.
 * Three rows are visible; tabs with more rows scroll.
 */
/**
 * A group of keys above the number pad. [columns] is how many keys fit across (at most five,
 * three for the wider trigonometry keys); [scrolls] marks the long groups (constants, symbols)
 * that scroll up and down inside their space.
 */
class FunctionTab(val title: String, val icon: KeyLabel, val keys: List<List<KeySpec>>, val columns: Int = 5, val scrolls: Boolean = false)

/** The five function pages above the number pad, like the tabs of a CAS keyboard. */
/**
 * The function pages above the number pad. Every operation appears once:
 * ∫, d/dx and lim are symbolic by default and turn numerical by themselves when
 * there's no exact answer (or when limits / a point are filled in and the
 * exact route fails). The decimal of any answer is behind its ≈ chip.
 */
private val GREEK = listOf(
    "α" to "alpha", "β" to "beta", "γ" to "gamma", "δ" to "delta", "ε" to "epsilon", "ζ" to "zeta",
    "η" to "eta", "θ" to "theta", "ι" to "iota", "κ" to "kappa", "λ" to "lambda", "μ" to "mu",
    "ν" to "nu", "ξ" to "xi", "π" to "letter pi", "ρ" to "rho", "σ" to "sigma", "τ" to "tau", "υ" to "upsilon",
    "φ" to "phi", "χ" to "chi", "ψ" to "psi", "ω" to "omega",
    "Γ" to "capital gamma", "Δ" to "capital delta", "Θ" to "capital theta", "Λ" to "capital lambda",
    "Ξ" to "capital xi", "Π" to "capital pi", "Σ" to "capital sigma", "Φ" to "capital phi",
    "Ψ" to "capital psi", "Ω" to "capital omega",
)

/**
 * Every Latin and Greek letter, six to a row, after := for storing values.
 * x, e and π have their own keys (number pad and √x tab), i, z and w are on
 * the ℂ tab, and Greek letters
 * that look like Latin ones (Α, Β, ο…) are left out.
 */
private /** The letters tab's first key: build your own symbol. */
val SymbolBuilderKey = KeySpec(KeyLabel.Icon(IconId.SymbolBuilder), KeyAction.OpenSymbolBuilder, KeyRole.Equals, "symbol builder")

/**
 * The letters tab as shown: the symbol builder, then the symbols you've built (newest first),
 * then every letter.
 */
fun letterRows(saved: List<String>, pinned: List<String> = emptyList()): List<List<KeySpec>> {
    val letters = LetterKeys.flatten()
    // The builder and := stay first; pinned keys come next, then the symbols you built, then the rest.
    val special = listOf(SymbolBuilderKey) + letters.filter { it.role == KeyRole.Equals }
    val rest = saved.map { KeySpec(KeyLabel.Math(row(Sym(it)), latex = true), KeyAction.Type(it), KeyRole.Operator, "saved symbol") } +
        letters.filter { it.role != KeyRole.Equals }
    return pinnedFirst(special, rest, pinned).chunked(6)
}

/** The constants group with pinned constants after the list key. */
fun constantRows(pinned: List<String>): List<List<KeySpec>> {
    val keys = ConstantKeys.flatten()
    return pinnedFirst(keys.filter { it.role == KeyRole.Equals }, keys.filter { it.role != KeyRole.Equals }, pinned).chunked(5)
}

/** What a key is pinned by: the text it types (letters, symbols), or its name (constants). */
val KeySpec.pinId: String get() = (action as? KeyAction.Type)?.text ?: spoken

/** Letters, symbols and constants can be pinned; the groups' special keys (builder, :=, the list) can't. */
val KeySpec.pinnable: Boolean get() = role != KeyRole.Equals && (label is KeyLabel.Math && (label.latex || spoken in ConstantNames))

private val ConstantNames by lazy { com.example.cas.engine.Constant.entries.map { it.description.substringBefore(" (") }.toSet() }

private fun pinnedFirst(special: List<KeySpec>, rest: List<KeySpec>, pinned: List<String>): List<KeySpec> {
    val byId = rest.associateBy { it.pinId }
    val front = pinned.mapNotNull { byId[it] }
    return special + front + rest.filter { it !in front }
}

val LetterKeys: List<List<KeySpec>> = run {
    val keys = ArrayList<KeySpec>()
    keys += KeySpec(KeyLabel.Text(":="), KeyAction.Type(":="), KeyRole.Equals, "store in variable")
    // Every letter, including those that also have their own keys elsewhere (x, e, i, z, w, π),
    // so all of them are in one place.
    // Drawn in LaTeX's fonts, as in the math: italic Latin and small Greek, upright capital Greek.
    fun letter(t: String, spoken: String) = KeySpec(KeyLabel.Math(row(Sym(t)), latex = true), KeyAction.Type(t), KeyRole.Function, spoken)
    ('a'..'z').forEach { c -> keys += letter(c.toString(), "letter $c") }
    ('A'..'Z').forEach { c -> keys += letter(c.toString(), "capital $c") }
    GREEK.forEach { (g, name) -> keys += letter(g, name) }
    // \mathcal capitals and \mathfrak capitals and small letters.
    ('A'..'Z').forEach { c -> keys += letter(com.example.cas.editor.MathAlphabets.calligraphic(c), "calligraphic $c") }
    ('A'..'Z').forEach { c -> keys += letter(com.example.cas.editor.MathAlphabets.fraktur(c), "fraktur capital $c") }
    ('a'..'z').forEach { c -> keys += letter(com.example.cas.editor.MathAlphabets.fraktur(c), "fraktur $c") }
    keys.chunked(6)
}

/**
 * Every constant from Wikipedia's list of physical constants, four to a row,
 * after a key that opens the full list with names, values and units.
 */
internal val ConstantKeys: List<List<KeySpec>> = run {
    val keys = ArrayList<KeySpec>()
    // In the Ans color, like the other keys that open or keep things rather than type them.
    keys += KeySpec(KeyLabel.Icon(IconId.MoreConstants), KeyAction.MoreConstants, KeyRole.Equals, "list of constants with names")
    com.example.cas.engine.Constant.entries.forEach { k -> keys += constant(k.id, k.description.substringBefore(" (")) }
    keys.chunked(5)
}

/**
 * The function pages above the number pad, in this order: powers and roots,
 * trigonometry, algebra, calculus, linear algebra, complex numbers, constants,
 * more functions, letters. Every operation appears once: ∫, d/dx and lim are symbolic by
 * default and turn numerical by themselves when there's no exact answer.
 */
val FunctionTabs: List<FunctionTab> = listOf(
    FunctionTab(
        "Basic", KeyLabel.Icon(IconId.Roots),
        listOf(
            listOf(
                math(row(Sym("π")), KeyAction.Type("π"), "pi"),
                math(row(Sym("e")), KeyAction.Type("e"), "e"),
                math(row(Sym("x"), Pow(row("n"))), KeyAction.Power(null), "power"),
                math(row(Root(row("n"), row("x"))), KeyAction.Insert(0) { Root() }, "nth root"),
                text("n!", KeyAction.Type("!"), spoken = "factorial"),
            ),
            listOf(
                notation(row(Func("ln", listOf(row("x")))), KeyAction.Insert(0) { Func("ln") }, "natural log"),
                math(row(Func("log", listOf(row("a"), row("x")))), KeyAction.Insert(0) { Func("log", 2) }, "log base a"),
                math(row(Func("abs", listOf(row("x")))), KeyAction.Insert(0) { Func("abs") }, "absolute value"),
                notation(letters("a", "mod", "b"), KeyAction.Type("mod"), "mod"),
                type("i", "i, the imaginary unit"),
            ),
            listOf(
                text("=", KeyAction.Type("="), spoken = "equals sign"),
                text("<", KeyAction.Type("<"), spoken = "less than"),
                text(">", KeyAction.Type(">"), spoken = "greater than"),
                text(",", KeyAction.Type(","), spoken = "comma"),
                KeySpec(KeyLabel.Text("Ans"), KeyAction.Type("ans"), KeyRole.Equals, "previous answer"),
            ),
        ),
        columns = 5,
    ),
    FunctionTab(
        "Trigonometry", KeyLabel.Icon(IconId.Triangle),
        listOf("sin", "cos", "tan").map { f ->
            listOf(
                notation(letters(f), KeyAction.Insert(0) { Func(f) }, f),
                notation(letters("a$f"), KeyAction.Insert(0) { Func("a$f") }, "inverse $f"),
                notation(letters(f + "h"), KeyAction.Insert(0) { Func(f + "h") }, "hyperbolic $f"),
                notation(letters("a${f}h"), KeyAction.Insert(0) { Func("a${f}h") }, "inverse hyperbolic $f"),
            )
        },
        columns = 4,
    ),
    FunctionTab(
        "Calculus", KeyLabel.Icon(IconId.Area),
        listOf(
            listOf(
                math(row(Sym("∂"), Sym("f")), KeyAction.Insert(1) { Derivative(row("x"), MathRow(), partial = true) }, "partial derivative"),
                math(row(Integral(body = row("f"), variable = row("x"))), KeyAction.Insert(2) { Integral(variable = row("x")) }, "integral"),
                notation(letters("lim"), KeyAction.Insert(0) { Func("lim", listOf(MathRow(), MathRow(mutableListOf(Sym("x"), Sym("→"), Sym("0"), Pow(MathRow()))))) }, "limit"),
                text("Σ", KeyAction.Insert(1) { BigOp(BigOpKind.Sum, variable = row("x")) }, spoken = "sum"),
                text("Π", KeyAction.Insert(1) { BigOp(BigOpKind.Product, variable = row("x")) }, spoken = "product"),
            ),
            listOf(
                notation(letters("f", "≈", "Σ"), KeyAction.Insert(0) { Func("taylor", listOf(MathRow(), letters("x", "→", "0"), row("5"))) }, "Taylor series"),
                notation(letters("y", "′"), KeyAction.Sequence(KeyAction.Type("y"), KeyAction.Type("′")), "y prime"),
                notation(letters("y″"), KeyAction.Sequence(KeyAction.Type("y"), KeyAction.Type("′"), KeyAction.Type("′")), "y double prime"),
                notation(letters("y′", "=", "f"), KeyAction.Insert(0) { Func("dsolve") }, "solve differential equation"),
                text("∞", KeyAction.Type("∞"), spoken = "infinity"),
            ),
            listOf(
                // ∇ with an empty order box (blank means the gradient, 2 the Laplacian).
                math(row(Func("grad", listOf(row("f"), MathRow()))), KeyAction.Insert(0) { Func("grad", listOf(MathRow(), MathRow())) }, "gradient"),
                notation(row(Func("div", listOf(row("F")))), KeyAction.Insert(0) { Func("div") }, "divergence"),
                notation(row(Func("curl", listOf(row("F")))), KeyAction.Insert(0) { Func("curl") }, "curl"),
                notation(row(Func("jacobian", listOf(row("F")))), KeyAction.Insert(0) { Func("jacobian") }, "Jacobian matrix"),
                notation(row(Func("hessian", listOf(row("f")))), KeyAction.Insert(0) { Func("hessian") }, "Hessian matrix"),
            ),
        ),
        columns = 5,
    ),
    FunctionTab(
        "Linear algebra", KeyLabel.Icon(IconId.Matrix),
        listOf(
            listOf(
                KeySpec(KeyLabel.Matrix(), KeyAction.PickMatrix, KeyRole.Function, "matrix"),
                KeySpec(KeyLabel.Matrix(sup = "−1"), KeyAction.Power("−1"), KeyRole.Function, "inverse"),
                KeySpec(KeyLabel.Matrix(sup = "T"), KeyAction.Power("T"), KeyRole.Function, "transpose"),
                KeySpec(KeyLabel.Matrix(sup = "H"), KeyAction.Power("H"), KeyRole.Function, "conjugate transpose"),
                KeySpec(KeyLabel.Matrix(prefix = "det"), KeyAction.Insert(0) { Func("det") }, KeyRole.Function, "determinant"),
            ),
            listOf(
                KeySpec(KeyLabel.Matrix(op = "·", second = true, wide = true, secondTall = true), KeyAction.Insert(0) { Func("dot", 2) }, KeyRole.Function, "dot product"),
                KeySpec(KeyLabel.Matrix(op = "×", second = true, wide = true, secondTall = true), KeyAction.Insert(0) { Func("cross", 2) }, KeyRole.Function, "cross product"),
                KeySpec(KeyLabel.Matrix(op = "∘", second = true), KeyAction.Insert(0) { Func("hadamard", 2) }, KeyRole.Function, "Hadamard product"),
                KeySpec(KeyLabel.Matrix(op = "⊗", second = true), KeyAction.Insert(0) { Func("kron", 2) }, KeyRole.Function, "Kronecker product"),
                KeySpec(KeyLabel.Matrix(prefix = "tr"), KeyAction.Insert(0) { Func("trace") }, KeyRole.Function, "trace"),
            ),
            listOf(
                KeySpec(KeyLabel.Text("λ"), KeyAction.Insert(0) { Func("eigvals") }, KeyRole.Function, "eigenvalues"),
                KeySpec(KeyLabel.Text("x⃗"), KeyAction.Insert(0) { Func("eigvecs") }, KeyRole.Function, "eigenvectors"),
                KeySpec(KeyLabel.Text("p(λ)"), KeyAction.Insert(0) { Func("charpoly") }, KeyRole.Function, "characteristic polynomial"),
                KeySpec(KeyLabel.Matrix(prefix = "rref"), KeyAction.Insert(0) { Func("rref") }, KeyRole.Function, "reduced row echelon form"),
                KeySpec(KeyLabel.Matrix(prefix = "rk"), KeyAction.Insert(0) { Func("rank") }, KeyRole.Function, "rank"),
            ),
        ),
        columns = 5,
    ),
    FunctionTab(
        "Statistics", KeyLabel.Icon(IconId.Stats),
        listOf(
            listOf(
                notation(letters("x̄"), KeyAction.Insert(0) { Func("mean") }, "mean"),
                notation(letters("med"), KeyAction.Insert(0) { Func("median") }, "median"),
                notation(letters("s"), KeyAction.Insert(0) { Func("sd") }, "sample standard deviation"),
                notation(letters("σ"), KeyAction.Insert(0) { Func("psd") }, "population standard deviation"),
                notation(row(Sym("s"), Pow(row("2"))), KeyAction.Insert(0) { Func("var") }, "sample variance"),
            ),
            listOf(
                math(row(Binom(row("n"), row("k"))), KeyAction.Insert(0) { Binom() }, "n choose k"),
                notation(row(Func("perm", listOf(row("n"), row("k")))), KeyAction.Insert(0) { Func("perm", 2) }, "permutations"),
                notation(row(Func("normpdf", listOf(row("x")))), KeyAction.Insert(0) { Func("normpdf", listOf(MathRow(), row("0"), row("1"))) }, "normal density"),
                notation(row(Func("normcdf", listOf(row("x")))), KeyAction.Insert(0) { Func("normcdf", listOf(MathRow(), row("0"), row("1"))) }, "normal distribution function"),
                notation(row(Func("invnorm", listOf(row("p")))), KeyAction.Insert(0) { Func("invnorm") }, "inverse normal"),
            ),
            listOf(
                notation(row(Func("binompdf", listOf(row("n"), row("p"), row("k")))), KeyAction.Insert(0) { Func("binompdf", 3) }, "binomial probability"),
                notation(row(Func("binomcdf", listOf(row("n"), row("p"), row("k")))), KeyAction.Insert(0) { Func("binomcdf", 3) }, "cumulative binomial probability"),
                notation(row(Func("poissonpdf", listOf(row("λ"), row("k")))), KeyAction.Insert(0) { Func("poissonpdf", 2) }, "Poisson probability"),
                notation(row(Func("total", listOf(row("x")))), KeyAction.Insert(0) { Func("total") }, "sum of a list"),
                text(",", KeyAction.Type(","), spoken = "comma for lists"),
            ),
        ),
        columns = 5,
    ),
    FunctionTab(
        "Complex numbers", KeyLabel.Icon(IconId.ComplexC),
        listOf(
            listOf(
                math(row(Sym("i")), KeyAction.Type("i"), "i, the imaginary unit"),
                notation(letters("ℜ", "z"), KeyAction.Insert(0) { Func("Re") }, "real part"),
                notation(letters("ℑ", "z"), KeyAction.Insert(0) { Func("Im") }, "imaginary part"),
                notation(row(Sym("z"), Pow(row("*"))), KeyAction.Insert(0) { Func("conj") }, "conjugate"),
                notation(letters("∠", "z"), KeyAction.Insert(0) { Func("arg") }, "argument"),
            ),
            listOf(
                math(row(Sym("e"), Pow(row("iθ"))), KeyAction.Sequence(KeyAction.Type("e"), KeyAction.Power("iθ")), "e to the i theta"),
                notation(row(Func("gamma", listOf(row("z")))), KeyAction.Insert(0) { Func("gamma") }, "gamma function"),
                notation(row(Func("zeta", listOf(row("z")))), KeyAction.Insert(0) { Func("zeta") }, "Riemann zeta function"),
                notation(row(Sym("W"), Sym("("), Sym("z"), Sym(")")), KeyAction.Insert(0) { Func("lambertw") }, "Lambert W function"),
                math(row(Scripted(row("J"), row("a"), MathRow()), Sym("("), Sym("z"), Sym(")")), KeyAction.Insert(0) { Func("besselj", 2) }, "Bessel function of the first kind"),
            ),
            listOf(
                math(row(Scripted(row("Y"), row("a"), MathRow()), Sym("("), Sym("z"), Sym(")")), KeyAction.Insert(0) { Func("bessely", 2) }, "Bessel function of the second kind"),
                notation(
                    // The key shows just the loop integral; the circle |z| = 1 is filled in when it's typed.
                    row(Sym("∮"), Sym(" "), Sym("f"), Sym(" "), Sym("d"), Sym("z")),
                    KeyAction.Insert(0) { Func("contour", listOf(MathRow(), MathRow(mutableListOf(Func("abs", listOf(row("z"))), Sym("="), Sym("1"))))) },
                    "contour integral",
                ),
                notation(letters("Res", " ", "f"), KeyAction.Insert(0) { Func("residue", listOf(MathRow(), letters("z", "=", "0"))) }, "residue"),
                type("z"),
                type("w"),
            ),
        ),
        columns = 5,
    ),
    FunctionTab("Physical constants", KeyLabel.Icon(IconId.Atom), ConstantKeys, columns = 5, scrolls = true),
    FunctionTab("Symbols", KeyLabel.Icon(IconId.Letters), LetterKeys, columns = 6, scrolls = true),
)

private fun digit(d: String) = text(d, KeyAction.Type(d), KeyRole.Digit)
private fun op(t: String, spoken: String, action: KeyAction = KeyAction.Type(t)) = text(t, action, KeyRole.Operator, spoken)

/** Google Calculator's number pad. */
val MainKeys: List<List<KeySpec>> = listOf(
    // x is on the number pad in a CAS: it's the letter you type most.
    // The variable is drawn as math, so it's italic like the x in the display.
    listOf(text("AC", KeyAction.Clear, KeyRole.Clear, "all clear"), op("( )", "brackets", KeyAction.Paren), KeySpec(KeyLabel.Math(row(Sym("x"))), KeyAction.Type("x"), KeyRole.Operator, "x"), op("÷", "divide", KeyAction.Fraction)),
    listOf(digit("7"), digit("8"), digit("9"), op("×", "times")),
    listOf(digit("4"), digit("5"), digit("6"), op("−", "minus")),
    listOf(digit("1"), digit("2"), digit("3"), op("+", "plus")),
    // Enter (↵) works out the answer (or finishes a graph line); it isn't an equals sign.
    listOf(digit("0"), text(".", KeyAction.Type("."), KeyRole.Digit, "point"), KeySpec(KeyLabel.BackspaceIcon, KeyAction.Backspace, KeyRole.Digit, "backspace"), KeySpec(KeyLabel.EnterIcon, KeyAction.Enter, KeyRole.Equals, "enter")),
)
