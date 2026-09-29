import com.example.cas.editor.*
import com.example.cas.engine.*
fun js(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
fun main() {
    val ed = Editor()
    val out = StringBuilder()
    fun snap(name: String) {
        val v = runCatching { Formatter.answer(Evaluator().evaluate(MathCodec.copy(ed.root))).exact }.getOrNull()
        out.append("EX.$name={e:${js(MathCodec.encode(ed.root))},x:${js(v?.let { MathCodec.encode(it) } ?: "")},a:\"\"};\n")
    }
    // 1. type 3x²y
    ed.type("3"); ed.type("x"); ed.insertPower(row("2")); ed.type("y"); snap("div1")
    // 2. press ÷: the whole implicit product moves up, cursor in the denominator
    ed.insertFraction(); snap("div2")
    // 3. type 4x in the denominator
    ed.type("4"); ed.type("x"); snap("div3")
    java.io.File("/home/claude/casshot/divsteps.js").writeText(out.toString())
    print(out)
}
