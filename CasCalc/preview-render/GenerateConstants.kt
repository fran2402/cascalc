import com.example.cas.engine.*
import com.example.cas.editor.*
fun js(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
fun main() {
    val sb = StringBuilder("// Generated from Constants.kt\nconst CONSTS_ALL=[\n")
    for (k in Constant.entries) {
        val pieces = k.pieces.joinToString(",") { "{t:${js(it.text)},sub:${js(it.sub)},sup:${js(it.sup)},it:${it.italic}}" }
        val vals = UnitSystem.entries.joinToString(",") { u -> js(MathCodec.encode(Formatter.row(com.example.cas.cas.Numeric.approx(k.value(u))))) }
        sb.append("  {id:${js(k.id)},p:[$pieces],d:${js(k.description)},u:${js(k.unit)},exact:${k.exact},v:[$vals]},\n")
    }
    sb.append("];\n")
    // A couple of worked examples: the same inputs in different unit systems.
    fun ex(name: String, r: MathRow, u: UnitSystem) {
        val v = Evaluator(units = u).evaluate(r)
        sb.append("EX.$name={e:${js(MathCodec.encode(r))},x:${js(MathCodec.encode(Formatter.answer(v).exact))},a:${js(Formatter.answer(v).approx?.let { MathCodec.encode(it) } ?: "")}};\n")
    }
    // c × ℏ, then ÷: the × stops the numerator at ℏ, as on the keypad.
    ex("cPlanck", row(Const("c0"), Sym("×"), Frac(row(Const("hbar")), row(Const("G")))), UnitSystem.Planck)
    ex("cAtomic", row(Const("c0")), UnitSystem.Atomic)
    ex("cInPlanck", row(Const("c0")), UnitSystem.Planck)
    ex("chargePlanck", row(Const("qe"), Pow(row("2"))), UnitSystem.Planck)
    ex("hbarAtomic", row(Const("hbar"), Sym("+"), Const("me"), Sym("+"), Const("qe")), UnitSystem.Atomic)
    ex("meNatural", row(Const("me")), UnitSystem.Natural)
    ex("alphaSI", row(Frac(row(Const("qe"), Pow(row("2"))), row(Sym("4"), Sym("π"), Const("eps0"), Const("hbar"), Const("c0")))), UnitSystem.SI)
    java.io.File("/home/claude/casshot/consts.js").writeText(sb.toString())
    println(Constant.entries.size)
}
