import com.example.cas.cas.MathError
import com.example.cas.cas.Printer
import com.example.cas.cas.Mat
import com.example.cas.editor.*
import com.example.cas.engine.Evaluator
import com.example.cas.engine.Formatter
import com.example.cas.engine.Latex
import com.example.cas.ui.FunctionTabs
import com.example.cas.ui.KeyAction

fun r(t: String): MathRow { val o = MathRow(); for (c in t) o.add(Sym(c.toString())); return o }
fun samples(): List<Pair<String, () -> MathRow>> = listOf(
    "x" to { r("x") }, "2" to { r("2") }, "1/2" to { row(Frac(r("1"), r("2"))) },
    "list" to { r("1,2,4") }, "matrix" to { row(Matrix(2, 2, listOf(r("1"), r("2"), r("3"), r("4")))) },
    "vector" to { row(Matrix(3, 1, listOf(r("x"), r("y"), r("z")))) }, "z" to { r("z") },
)
fun fill(n: Node, s: () -> MathRow) { n.slots.forEach { if (it.isEmpty) it.items.addAll(s().items.map { i -> i }) } }
fun evalRow(x: MathRow): Result<String> = runCatching {
    val v = Evaluator().evaluate(MathCodec.copy(x))
    Formatter.answer(v); Latex.of(x); Printer.plain(v)
}
fun main() {
    var bugs = 0; var expectedFail = 0; var ok = 0
    val bugList = ArrayList<String>()
    val soft = ArrayList<String>()
    for (tab in FunctionTabs) for (key in tab.keys.flatten()) {
        val a = key.action as? KeyAction.Insert ?: continue
        // A filling that works on its own.
        var base: MathRow? = null; var baseName = ""; var scalar = false
        for ((name, s) in samples()) {
            val node = a.make(); fill(node, s)
            val rr = row(node)
            val res = runCatching { Evaluator().evaluate(MathCodec.copy(rr)) }
            if (res.isSuccess) { base = rr; baseName = name; scalar = res.getOrNull() !is Mat && res.getOrNull() !is com.example.cas.cas.Eq && res.getOrNull() !is com.example.cas.cas.Seq; break }
            val e = res.exceptionOrNull()
            if (e !is MathError) { bugs++; bugList += "${key.spoken} alone with $name: ${e!!::class.simpleName}: ${e.message}" }
        }
        if (base == null) { soft += "${key.spoken}: no sample works alone"; continue }
        val b = base!!
        val wraps = listOf<Pair<String, MathRow>>(
            "lim x→1" to row(Func("lim", listOf(MathCodec.copy(b), MathRow(mutableListOf(Sym("x"), Sym("→"), Sym("1")))))),
            "∫₀¹ dx" to row(Integral(r("0"), r("1"), MathCodec.copy(b), r("x"))),
            "d/dx" to row(Derivative(r("x"), MathCodec.copy(b))),
            "+ 1" to MathRow((MathCodec.copy(b).items + Sym("+") + Sym("1")).toMutableList()),
            "Σ k=1..3" to row(BigOp(BigOpKind.Sum, r("k"), r("1"), r("3"), MathCodec.copy(b))),
            "( )²" to row(Sym("("), *MathCodec.copy(b).items.toTypedArray(), Sym(")"), Pow(r("2"))),
            "sin( )" to row(Func("sin", listOf(MathCodec.copy(b)))),
        )
        for ((wn, w) in wraps) {
            val res = evalRow(w)
            val e = res.exceptionOrNull()
            when {
                e == null -> ok++
                e is MathError -> {
                    expectedFail++
                    // Scalars in anything, and matrices/vectors in limits, integrals, derivatives and sums, should work.
                    val matrixCase = !scalar && wn in setOf("lim x→1", "∫₀¹ dx", "d/dx", "Σ k=1..3")
                    if ((scalar && baseName in setOf("x", "2", "1/2") && wn in setOf("lim x→1", "∫₀¹ dx", "d/dx", "+ 1", "( )²")) || matrixCase) soft += "${key.spoken} [$baseName] in $wn: ${e.message}"
                }
                else -> { bugs++; bugList += "${key.spoken} [$baseName] in $wn: ${e::class.simpleName}: ${e.message}" }
            }
        }
    }
    println("worked: $ok, clear messages: $expectedFail, crashes: $bugs")
    bugList.distinct().forEach { println("CRASH  $it") }
    println("--- scalar cases that gave a message (worth a look):")
    soft.distinct().forEach { println("MSG    $it") }
}
