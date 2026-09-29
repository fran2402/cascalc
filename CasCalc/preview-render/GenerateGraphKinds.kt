import com.example.cas.editor.*
import com.example.cas.engine.Evaluator
import com.example.cas.graph.*
import com.example.cas.cas.Eq
import kotlin.math.*

fun r(text: String): MathRow { val out = MathRow(); for (c in text) when (c) { '²' -> out.add(Pow(row("2"))); else -> out.add(Sym(c.toString())) }; return out }
fun js(d: Double) = String.format(java.util.Locale.US, "%.1f", d)
fun q(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

/** Mirrors Graph2DScreen.plot / drawPolarGrid, output as screen-space shapes. */
class Scene(val w: Double, val h: Double, halfWidth: Double = 10.0) {
    val v = Viewport.standard(h / w, halfWidth)
    fun sx(x: Double) = (x - v.xMin) / v.width * w
    fun sy(y: Double) = (v.yMax - y) / v.height * h
    val parts = ArrayList<String>()
    val rows = ArrayList<String>()

    fun add(row: MathRow, color: Int, params: Map<String, Double> = emptyMap(), pair: Pair<MathRow, MathRow>? = null) {
        val ev = Evaluator()
        val spec = PlotSpec.classify(if (pair != null) listOf(ev.evaluate(pair.first), ev.evaluate(pair.second)) else listOf(ev.evaluate(row)))
        val ps = spec.parameters
        fun comp(e: com.example.cas.cas.Expr, vars: List<String>): (DoubleArray) -> Double { val c = Compiler.compile(e, vars + ps); return { a -> c(a + ps.map { params[it] ?: 1.0 }.toDoubleArray()) } }
        rows += "{row:${q(MathCodec.encode(row))},c:$color,label:${q(when (spec) { is PlotSpec.Explicit -> if (row.items.any { (it as? Sym)?.text == "=" }) "" else "y ="; is PlotSpec.Polar -> if (row.items.any { (it as? Sym)?.text == "=" }) "" else "r ="; is PlotSpec.Parametric -> "(x, y) ="; else -> "" })}}"
        fun lines(ls: List<List<Pair<Double, Double>>>) = ls.joinToString(",") { l -> "[" + l.joinToString(",") { (x, y) -> "[${js(sx(x))},${js(sy(y))}]" } + "]" }
        fun segs(ss: List<DoubleArray>) = ss.joinToString(",") { s -> "[${js(sx(s[0]))},${js(sy(s[1]))},${js(sx(s[2]))},${js(sy(s[3]))}]" }
        val nx = (w / 6).toInt(); val ny = (h / 6).toInt()
        when (spec) {
            is PlotSpec.Explicit -> { val f = comp(spec.f, listOf("x")); parts += "{c:$color,lines:[${lines(Plot2D.sample({ x -> f(doubleArrayOf(x)) }, v, 400))}]}" }
            is PlotSpec.Polar -> { val f = comp(spec.r, listOf("θ")); parts += "{c:$color,lines:[${lines(Curves.polar({ t -> f(doubleArrayOf(t)) }, v))}]}" }
            is PlotSpec.Parametric -> { val fx = comp(spec.x, listOf("t")); val fy = comp(spec.y, listOf("t")); parts += "{c:$color,lines:[${lines(Curves.parametric({ t -> fx(doubleArrayOf(t)) }, { t -> fy(doubleArrayOf(t)) }, 0.0, 2 * PI, v))}]}" }
            is PlotSpec.Implicit -> { val f = comp(spec.f, listOf("x", "y")); parts += "{c:$color,segs:[${segs(Curves.implicit({ x, y -> f(doubleArrayOf(x, y)) }, v, nx, ny))}]}" }
            is PlotSpec.Region -> {
                val fs = spec.rel.parts.map { comp(it, listOf("x", "y")) }
                val vals = DoubleArray(fs.size)
                val mx = (w / 4).toInt(); val my = (h / 4).toInt()
                val mask = Curves.region({ x, y -> for (k in fs.indices) vals[k] = fs[k](doubleArrayOf(x, y)); Curves.holds(vals, spec.rel.ops) }, v, mx, my)
                val runs = ArrayList<String>()
                for (j in 0 until my) { var i = 0; while (i < mx) { if (!mask[j * mx + i]) { i++; continue }; val s0 = i; while (i < mx && mask[j * mx + i]) i++; runs += "[$j,$s0,$i]" } }
                val edges = spec.rel.ops.indices.flatMap { k -> Curves.implicit({ x, y -> fs[k](doubleArrayOf(x, y)) - fs[k + 1](doubleArrayOf(x, y)) }, v, nx, ny) }
                parts += "{c:$color,mask:{nx:$mx,ny:$my,runs:[${runs.joinToString(",")}]},segs:[${segs(edges)}],dashed:${spec.rel.ops.all { it == "<" || it == ">" }}}"
            }
        }
    }

    fun polarGrid(): String {
        val corners = listOf(v.xMin to v.yMin, v.xMin to v.yMax, v.xMax to v.yMin, v.xMax to v.yMax)
        val far = corners.maxOf { (x, y) -> hypot(x, y) }
        val step = Plot2D.niceStep(min(v.width, v.height) / 2, 4)
        val px = w / v.width
        val circles = ArrayList<String>(); var rr = step / 2; var k = 1
        while (rr <= far + step) { circles += "[${js(rr * px)},${k % 2 == 0}]"; rr += step / 2; k++ }
        val labels = ArrayList<String>(); var lr = step
        while (lr <= far) { labels += "[${js(sx(lr))},${js(sy(0.0))},${q(Plot2D.label(lr, step))}]"; lr += step }
        val names = listOf("0", "π/6", "π/3", "π/2", "2π/3", "5π/6", "π", "7π/6", "4π/3", "3π/2", "5π/3", "11π/6")
        val ox = sx(0.0); val oy = sy(0.0)
        val labelR = minOf(ox, w - ox, oy, h - oy) - 18
        val angles = (0 until 12).joinToString(",") { a -> "[${js(ox + labelR * cos(a * PI / 6))},${js(oy - labelR * sin(a * PI / 6))},${q(names[a])}]" }
        return "{circles:[${circles.joinToString(",")}],labels:[${labels.joinToString(",")}],angles:[$angles]}"
    }

    fun export(name: String, polar: Boolean, trace: String = "null"): String {
        val tx = Plot2D.ticks(v.xMin, v.xMax, (w / 90).toInt()); val ty = Plot2D.ticks(v.yMin, v.yMax, (h / 90).toInt())
        val stepX = Plot2D.niceStep(v.width, (w / 90).toInt()); val stepY = Plot2D.niceStep(v.height, (h / 90).toInt())
        return "const $name={w:$w,h:$h,origin:[${js(sx(0.0))},${js(sy(0.0))}],polar:${if (polar) polarGrid() else "null"}," +
            "minorX:[${Plot2D.ticks(v.xMin, v.xMax, (w / 90).toInt() * 5).joinToString(",") { js(sx(it)) }}],minorY:[${Plot2D.ticks(v.yMin, v.yMax, (h / 90).toInt() * 5).joinToString(",") { js(sy(it)) }}]," +
            "majorX:[${tx.joinToString(",") { "[${js(sx(it))},${q(if (abs(it) > stepX / 2) Plot2D.label(it, stepX) else "")}]" }}],majorY:[${ty.joinToString(",") { "[${js(sy(it))},${q(if (abs(it) > stepY / 2) Plot2D.label(it, stepY) else "")}]" }}]," +
            "parts:[${parts.joinToString(",")}],rows:[${rows.joinToString(",")}],trace:$trace};\n"
    }
}

fun main() {
    val out = StringBuilder("// Generated by graphs2.kt (Curves, PlotSpec, Plot2D)\n")
    // 1: polar curves with the polar grid: a rose and a cardioid; trace in (r, θ)
    val a = Scene(412.0, 470.0, 2.2)
    val rose = row(Sym("r"), Sym("="), Func("cos", listOf(r("3θ"))))
    val cardioid = row(Sym("1"), Sym("+"), Func("cos", listOf(r("θ"))))
    a.add(rose, 0); a.add(cardioid, 1)
    val th = PI / 3; val rr = 1 + cos(th)
    val trace = "{x:${js(a.sx(rr * cos(th)))},y:${js(a.sy(rr * sin(th)))},text:${q("r = 1.5, θ = 1.0472")},c:1}"
    out.append(a.export("G6", true, trace))
    // 2: an implicit curve, a region, and a parametric curve
    val b = Scene(412.0, 500.0, 4.0)
    b.add(r("x²+y²≤9"), 3)
    b.add(r("x²−y²=1"), 0)
    val pair = row(Sym("("), Sym("2"), Func("sin", listOf(r("3t"))), Sym(","), Sym("2"), Func("sin", listOf(r("2t"))), Sym(")"))
    b.add(pair, 1, pair = row(Sym("2"), Func("sin", listOf(r("3t")))) to row(Sym("2"), Func("sin", listOf(r("2t")))))
    out.append(b.export("G7", false))
    // 3: editing r = 1 + a cos θ with the slider for a playing (shown at a = 1.6: a limaçon with an inner loop)
    val c = Scene(412.0, 230.0, 3.2)
    c.add(row(Sym("r"), Sym("="), Sym("1"), Sym("+"), Sym("a"), Func("cos", listOf(r("θ")))), 0, mapOf("a" to 1.6))
    out.append(c.export("G8", true))
    // 4: the area under sin x from 0 to π (picked with the Area button)
    val d = Scene(412.0, 470.0, 4.0)
    val sinRow = row(Func("sin", listOf(r("x"))))
    d.add(sinRow, 0)
    val (signed, total) = Plot2D.area({ Math.sin(it) }, 0.0, Math.PI)
    val shade = (0..120).joinToString(",") { k -> val x = Math.PI * k / 120; "[${js(d.sx(x))},${js(d.sy(Math.sin(x)))}]" }
    out.append(d.export("G10", false).removeSuffix("};\n") + ",area:{pts:[[${js(d.sx(0.0))},${js(d.sy(0.0))}],$shade,[${js(d.sx(Math.PI))},${js(d.sy(0.0))}]],signed:${q(String.format(java.util.Locale.US, "%.4f", signed))},total:${q(String.format(java.util.Locale.US, "%.4f", total))},a:\"0\",b:\"3.1416\"}};\n")
    // 5: Desmos-style lines: a definition, a restricted line using it, a point
    val g = Scene(412.0, 470.0, 5.0)
    val defRow = row(Sym("f"), Sym("("), Sym("x"), Sym(")"), Sym("="), Frac(r("x²"), r("4")))
    val defEv = Evaluator(); defEv.evaluate(defRow)
    val fns = mapOf(defEv.definedFunction!!)
    fun evalF(x: MathRow) = Evaluator(functions = fns).evaluate(x)
    val f1 = Compiler.compile(evalF(r("f(x)")), listOf("x"))
    g.parts += "{c:0,lines:[" + Plot2D.sample({ x -> f1(doubleArrayOf(x)) }, g.v, 400).joinToString(",") { l -> "[" + l.joinToString(",") { (x, y) -> "[${js(g.sx(x))},${js(g.sy(y))}]" } + "]" } + "]}"
    g.rows += "{row:${q(MathCodec.encode(defRow))},c:0,label:\"\"}"
    // f(x − 2) + 1, 0 < x < 5: drawn only where the condition holds, dashed.
    val restrictedRow = MathRow((r("f(x−2)+1").items + Sym(",") + r("0<x<5").items).toMutableList())
    val f2 = Compiler.compile(evalF(r("f(x−2)+1")), listOf("x"))
    val cond = evalF(r("0<x<5")) as com.example.cas.cas.Rel
    val cparts = cond.parts.map { Compiler.compile(it, listOf("x")) }
    val restricted = { x: Double -> if (Curves.holds(DoubleArray(cparts.size) { cparts[it](doubleArrayOf(x)) }, cond.ops)) f2(doubleArrayOf(x)) else Double.NaN }
    g.parts += "{c:1,dash:true,lines:[" + Plot2D.sample(restricted, g.v, 400).joinToString(",") { l -> "[" + l.joinToString(",") { (x, y) -> "[${js(g.sx(x))},${js(g.sy(y))}]" } + "]" } + "]}"
    g.rows += "{row:${q(MathCodec.encode(restrictedRow))},c:1,label:\"\"}"
    val pointRow = row(Sym("("), Sym("2"), Sym(","), Sym("3"), Sym(")"))
    g.parts += "{c:2,point:[${js(g.sx(2.0))},${js(g.sy(3.0))}]}"
    g.rows += "{row:${q(MathCodec.encode(pointRow))},c:2,label:\"\"}"
    out.append(g.export("G11", false))
    java.io.File("/home/claude/casshot/graphs2.js").writeText(out.toString())
    println("ok")
}
