import com.example.cas.editor.*
import com.example.cas.engine.Evaluator
import com.example.cas.graph.*
import com.example.cas.cas.freeVars

fun f(name: String, vararg a: MathRow) = Func(name, a.toList())
fun js(d: Double) = String.format(java.util.Locale.US, "%.2f", d)

/** Mirrors Graph2DScreen: grid, labels, curves, zeros/extrema/intercepts/intersections. */
fun graph2d(name: String, rows: List<MathRow>, params: Map<String, Double>, w: Double, h: Double, trace: Int?, highlighted: Int = 0): String {
    val fns = rows.map { r ->
        val e = Evaluator().evaluate(r)
        val ps = (e.freeVars() - "x").sorted()
        val c = Compiler.compile(e, listOf("x") + ps)
        val g: (Double) -> Double = { x -> c(doubleArrayOf(x) + ps.map { params[it] ?: 1.0 }.toDoubleArray()) }
        g
    }
    val v = Viewport.standard(h / w)
    fun sx(x: Double) = (x - v.xMin) / v.width * w
    fun sy(y: Double) = (v.yMax - y) / v.height * h
    val tx = (w / 90).toInt().coerceAtLeast(3); val ty = (h / 90).toInt().coerceAtLeast(3)
    val stepX = Plot2D.niceStep(v.width, tx); val stepY = Plot2D.niceStep(v.height, ty)
    val sb = StringBuilder("$name={w:$w,h:$h,origin:[${js(sx(0.0))},${js(sy(0.0))}],")
    sb.append("minorX:[" + Plot2D.ticks(v.xMin, v.xMax, tx * 5).joinToString(",") { js(sx(it)) } + "],")
    sb.append("minorY:[" + Plot2D.ticks(v.yMin, v.yMax, ty * 5).joinToString(",") { js(sy(it)) } + "],")
    sb.append("majorX:[" + Plot2D.ticks(v.xMin, v.xMax, tx).joinToString(",") { "[${js(sx(it))},\"${if (kotlin.math.abs(it) > stepX / 2) Plot2D.label(it, stepX) else ""}\"]" } + "],")
    sb.append("majorY:[" + Plot2D.ticks(v.yMin, v.yMax, ty).joinToString(",") { "[${js(sy(it))},\"${if (kotlin.math.abs(it) > stepY / 2) Plot2D.label(it, stepY) else ""}\"]" } + "],")
    sb.append("curves:[" + fns.mapIndexed { i, g ->
        "{c:$i,lines:[" + Plot2D.sample(g, v, (w.toInt() / 2).coerceIn(200, 900)).joinToString(",") { line -> "[" + line.joinToString(",") { (x, y) -> "[${js(sx(x))},${js(sy(y))}]" } + "]" } + "]}"
    }.joinToString(",") + "],")
    val pts = ArrayList<String>()
    val labels = ArrayList<Triple<Double, Double, String>>()
    fns.forEachIndexed { i, g ->
        if (i != highlighted) return@forEachIndexed
        Plot2D.zeros(g, v.xMin, v.xMax, 400).forEach { pts += "[${js(sx(it))},${js(sy(0.0))},$i]"; labels += Triple(it, 0.0, "zero") }
        Plot2D.extrema(g, v.xMin, v.xMax, 400).forEach { (x, k) -> pts += "[${js(sx(x))},${js(sy(g(x)))},$i]"; labels += Triple(x, g(x), if (k == Plot2D.Kind.Maximum) "maximum" else "minimum") }
        val y0 = g(0.0); if (y0.isFinite()) { pts += "[${js(sx(0.0))},${js(sy(y0))},$i]"; labels += Triple(0.0, y0, "y-intercept") }
    }
    for (a in fns.indices) for (b in a + 1 until fns.size) if (a == highlighted || b == highlighted) Plot2D.zeros({ x -> fns[a](x) - fns[b](x) }, v.xMin, v.xMax, 400).forEach { x ->
        pts += "[${js(sx(x))},${js(sy(fns[a](x)))},$a]"; labels += Triple(x, fns[a](x), "intersection")
    }
    sb.append("points:[" + pts.joinToString(",") + "]")
    if (trace != null) {
        val (x, y, l) = labels.filter { it.third == "intersection" }.getOrElse(trace) { labels[0] }
        fun short(d: Double) = if (kotlin.math.abs(d) < 1e-10) "0" else (Math.round(d * 10000) / 10000.0).let { if (it == Math.rint(it)) it.toLong().toString() else it.toString() }.replace("-", "−")
        sb.append(",trace:{x:${js(sx(x))},y:${js(sy(y))},text:\"$l  (${short(x)}, ${short(y)})\",c:0}")
    }
    sb.append("};\n")
    return sb.toString()
}

fun graph3d(name: String, rows: List<MathRow>, w: Float, h: Float, cam: Camera, range: Double): String {
    val grids = rows.map { r ->
        val c = Compiler.compile(Evaluator().evaluate(r), listOf("x", "y"))
        Surface3D.sample({ x, y -> c(doubleArrayOf(x, y)) }, range, 36)
    }
    val faces = Surface3D.faces(grids, cam, w, h)
    val sb = StringBuilder("$name={w:$w,h:$h,")
    sb.append("faces:[" + faces.joinToString(",") { f -> "[" + (0..3).joinToString(",") { "${js(f.xs[it].toDouble())},${js(f.ys[it].toDouble())}" } + ",${js(f.shade.toDouble())},${js(f.height.toDouble())},${f.surface}]" } + "],")
    sb.append("box:[" + Surface3D.box(cam, w, h).joinToString(",") { "[${js(it.x1.toDouble())},${js(it.y1.toDouble())},${js(it.x2.toDouble())},${js(it.y2.toDouble())}]" } + "],")
    sb.append("labels:[" + Surface3D.axisLabels(cam, w, h).joinToString(",") { (n, p) -> "[\"$n\",${js(p.first.toDouble())},${js(p.second.toDouble())}]" } + "],")
    val g = grids[0]; val step = Plot2D.niceStep(g.zMax - g.zMin, 4)
    sb.append("zmax:\"${Plot2D.label(g.zMax, step / 10)}\",zmin:\"${Plot2D.label(g.zMin, step / 10)}\"};\n")
    return sb.toString()
}

fun main() {
    val sinx = row(f("sin", row("x")))
    val para = row(Frac(row(Sym("x"), Pow(row("2"))), row("4")), Sym("−"), Sym("2"))
    val asinbx = row(Sym("a"), f("sin", row("bx")))
    val out = StringBuilder()
    out.append("const ROWS={sinx:\"${MathCodec.encode(sinx)}\",para:\"${MathCodec.encode(para)}\",asinbx:\"${MathCodec.encode(asinbx)}\",surf:\"${MathCodec.encode(row(f("sin", row("x")), f("cos", row("y"))))}\",saddle:\"${MathCodec.encode(row(Sym("x"), Pow(row("2")), Sym("−"), Sym("y"), Pow(row("2"))))}\"};\n")
    out.append("const " + graph2d("G2A", listOf(sinx, para), emptyMap(), 412.0, 590.0, trace = 1))
    out.append("const " + graph2d("G2B", listOf(asinbx, para), mapOf("a" to 2.0, "b" to 1.5), 412.0, 262.0, trace = null))
    out.append("const " + graph2d("G2C", listOf(sinx, row(Frac(row("1"), row("2")))), emptyMap(), 412.0, 440.0, trace = 1))
    out.append("const G2C_ROWS=[\"${MathCodec.encode(sinx)}\",\"${MathCodec.encode(row(Frac(row("1"), row("2"))))}\"];\n")
    out.append("const " + graph3d("G3", listOf(row(f("sin", row("x")), f("cos", row("y")))), 412f, 560f, Camera(), 3.0))
    java.io.File("/home/claude/casshot/graphs.js").writeText(out.toString())
    println("ok " + out.length)
}
