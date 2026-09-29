import com.example.cas.editor.*
import com.example.cas.engine.*
import com.example.cas.cas.Printer

fun f(name: String, vararg a: MathRow) = Func(name, a.toList())
fun r(text: String): MathRow { val out = MathRow(); for (c in text) when (c) { '²' -> out.add(Pow(row("2"))); '³' -> out.add(Pow(row("3"))); else -> out.add(Sym(c.toString())) }; return out }
fun x(n: String) = row(Sym("x"), Pow(row(n)))

fun main() {
    val ex = linkedMapOf<String, MathRow>(
        "solveQuad" to row(f("solve", row(Sym("x"), Pow(row("2")), Sym("−"), Sym("5"), Sym("x"), Sym("+"), Sym("6"), Sym("="), Sym("0")), row("x"))),
        "expandCube" to row(f("expand", row(Sym("("), Sym("x"), Sym("+"), Sym("1"), Sym(")"), Pow(row("3"))))),
        "intByParts" to row(Integral(MathRow(), MathRow(), row(Sym("x"), Sym("e"), Pow(row("x"))), row("x"))),
        "factorQuartic" to row(f("factor", row(Sym("x"), Pow(row("4")), Sym("−"), Sym("5"), Sym("x"), Pow(row("2")), Sym("+"), Sym("4")))),
        "sumSquares" to row(BigOp(BigOpKind.Sum, row("k"), row("1"), row("n"), row(Sym("k"), Pow(row("2"))))),
        "diffChain" to row(Derivative(row("x"), row(f("sin", x("2"))))),
        "intSin" to row(Integral(row("0"), row("π"), row(f("sin", row("x"))), row("x"))),
        "taylor" to row(f("taylor", row(Sym("e"), Pow(row("x"))), row(Sym("x"), Sym("→"), Sym("0")), row("4"))),
        "sqrt8" to row(Sqrt(row("8"))),
        "detSym" to row(f("det", row(Matrix(2, 2, listOf(row("a"), row("b"), row("c"), row("d")))))),
        "assign" to row(Sym("a"), Sym(":="), Sym("5")),
        "useVar" to row(Sym("2"), Sym("a"), Sym("+"), Sym("3"), Sym("a")),
        "solveSin" to row(f("solve", row(f("sin", row("x")), Sym("="), Frac(row("1"), row("2"))), row("x"))),
        "partial" to row(Integral(MathRow(), MathRow(), row(Frac(row("1"), row(Sym("x"), Pow(row("2")), Sym("−"), Sym("1")))), row("x"))),
        "simplifyFrac" to row(f("simplify", row(Frac(row(Sym("x"), Pow(row("2")), Sym("−"), Sym("1")), row("x−1"))))),
        "cubeRoots" to row(f("solve", row(Sym("x"), Pow(row("3")), Sym("="), Sym("8")), row("x"))),
        "invSym" to row(Matrix(2, 2, listOf(row("a"), row("b"), row("c"), row("d"))), Pow(row("−1"))),
        "intNumeric" to row(Integral(row("0"), row("1"), row(Sym("e"), Pow(row(Sym("−"), Sym("x"), Pow(row("2"))))), row("x"))),
        "diffFallback" to row(Derivative(row("x"), row(Sym("x"), Sym("!")), row("1"))),
        "minmax" to row(Func("max", listOf(row("π"), row(Sqrt(row("10")))))),
        "eigvecs" to row(f("eigvecs", row(Matrix(2, 2, listOf(row("2"), row("1"), row("1"), row("2")))))),
        "eigvalsIrr" to row(f("eigvals", row(Matrix(2, 2, listOf(row("0"), row("1"), row("1"), row("2")))))),
        "charpoly" to row(f("charpoly", row(Matrix(2, 2, listOf(row("2"), row("1"), row("1"), row("2")))))),
        "rref" to row(f("rref", row(Matrix(3, 3, listOf("1","2","3","4","5","6","7","8","9").map { row(it) })))),
        "eigvecs3" to row(f("eigvecs", row(Matrix(3, 3, listOf("1","1","0","0","2","2","0","0","3").map { row(it) })))),
        "cross" to row(f("cross", row(Matrix(1, 3, listOf(row("1"), row("2"), row("3")))), row(Matrix(1, 3, listOf(row("4"), row("5"), row("6")))))),
        "limSinc" to row(f("lim", row(Frac(row(f("sin", row("x"))), row("x"))), row(Sym("x"), Sym("→"), Sym("0")))),
        "apart" to row(f("apart", row(Frac(row("1"), row(Sym("x"), Pow(row("2")), Sym("−"), Sym("1")))))),
        "together" to row(f("together", row(Frac(row("1"), row("x")), Sym("+"), Frac(row("1"), row("x+1"))))),
        "c0sq" to row(Const("c0"), Pow(row("2"))),
        "energy" to row(Frac(row(Const("h"), Const("c0")), row(Sym("5"), Sym("0"), Sym("0"), Sym("×"), Sym("1"), Sym("0"), Pow(row("−9"))))),
        "gcdEx" to row(Func("gcd", listOf(row("84"), row("126")))),
        "sys2" to row(Func("solve", listOf(r("x+y=3,x−y=1"), r("x,y")))),
        "sysNonlin" to row(Func("solve", listOf(r("x²+y²=5,y=x+1"), r("x,y")))),
        "ineqOut" to row(Func("solve", listOf(r("x²≥4"), r("x")))),
        "ineqIn" to row(Func("solve", listOf(r("x²<4"), r("x")))),
        "ineqRat" to row(Func("solve", listOf(row(Frac(r("x+1"), r("x−1")), Sym(">"), Sym("0")), r("x")))),
        "odeHarm" to row(Func("dsolve", listOf(r("y′′+y=0,y(0)=1,y′(0)=0")))),
        "odeDamped" to row(Func("dsolve", listOf(r("y′′+2y′+5y=0")))),
        "odeGrowth" to row(Func("dsolve", listOf(r("y′=2y,y(0)=3")))),
        "limInf" to row(Func("lim", listOf(row(Frac(r("2x+1"), r("x+3"))), r("x→∞")))),
        "intInf" to row(Integral(r("0"), r("∞"), row(Sym("e"), Pow(r("−x"))), r("x"))),
        "d2" to row(Derivative(r("x"), row(Func("sin", listOf(row(Sym("x"), Pow(row("2")))))), MathRow(), r("2"))),
        "gradEx" to row(Func("grad", listOf(r("x²y")))),
        "curlEx" to row(Func("curl", listOf(row(Matrix(3, 1, listOf(r("−y"), r("x"), r("0"))))))),
        "dblInt" to row(Integral(r("0"), r("1"), row(Integral(r("0"), r("x"), r("xy"), r("y"))), r("x"))),
        "partialEx" to row(Derivative(r("x"), r("x²y³"), partial = true)),
        "lapEx" to row(Func("laplacian", listOf(r("x²−y²")))),
        "partialY" to row(Derivative(r("y"), r("x²y³"), partial = true)),
        "intDy" to row(Integral(body = r("2xy"), variable = r("y"))),
        "intDx" to row(Integral(body = r("2xy"), variable = r("x"))),
        "meanEx" to row(Func("mean", listOf(r("2,4,4,4,5,5,7,9")))),
        "sdEx" to row(Func("sd", listOf(r("2,4,4,4,5,5,7,9")))),
        "normEx" to row(Func("normcdf", listOf(r("1"), r("0"), r("1")))),
        "binomEx" to row(Func("binompdf", listOf(r("5"), row(Frac(r("1"), r("2"))), r("2")))),
        "permEx" to row(Func("perm", listOf(r("5"), r("3")))),
        "contourEx" to row(Func("contour", listOf(row(Frac(r("1"), r("z²+1"))), row(Func("abs", listOf(r("z−i"))), Sym("="), Sym("1"))))),
        "residueEx" to row(Func("residue", listOf(row(Frac(r("1"), r("z²+1"))), r("z=i")))),
        "contourZ" to row(Func("contour", listOf(row(Frac(row(Sym("e"), Pow(r("z"))), r("z³"))), row(Func("abs", listOf(r("z"))), Sym("="), Sym("1"))))),
        "lnNeg" to row(Func("ln", listOf(r("−2")))),
        "asin2" to row(Func("asin", listOf(r("2")))),
        "csqrt" to row(Sqrt(r("3+4i"))),
        "emptyMat" to row(Matrix(2, 2, listOf(r("1"), MathRow(), MathRow(), r("2"))), Sym("×"), Matrix(2, 1, listOf(r("3"), r("4")))),
        "limContour" to limContour(),
        "growMat" to row(Matrix(3, 3, listOf(r("1"), r("2"), MathRow(), r("3"), r("4"), MathRow(), MathRow(), MathRow(), MathRow()), growable = true)),
        "twoX" to row(Sym("2"), Sym("x"), Sym("+"), Sym("3"), Sym("x"), Sym("−"), Sym("x"), Pow(row("2")), Sym("+"), Sym("x"), Sym("x")),
    )
    val vars = mapOf("a" to com.example.cas.cas.num(5))
    val sb = StringBuilder("const EX={\n")
    for ((k, r) in ex) {
        val v = Evaluator(AngleUnit.Radians, null, if (k == "useVar") vars else emptyMap()).evaluate(r)
        val a = Formatter.answer(v)
        fun js(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        sb.append("  $k:{e:${js(MathCodec.encode(r))},x:${js(MathCodec.encode(a.exact))},a:${js(a.approx?.let { MathCodec.encode(it) } ?: "")}},  // ${Printer.plain(v)}\n")
    }
    sb.append("};\n")
    java.io.File("/home/claude/casshot/examples.js").writeText(sb.toString())
    print(sb)
}

/** lim_{R→∞} ∮_{|z|=R} e^{iz}/(z² + 1)² dz, as in the screenshot. */
fun limContour(): MathRow {
    val f = row(Frac(row(Sym("e"), Pow(r("iz"))), row(Sym("("), Sym("z"), Pow(row("2")), Sym("+"), Sym("1"), Sym(")"), Pow(row("2")))))
    val circle = row(Func("abs", listOf(r("z"))), Sym("="), Sym("R"))
    val contour = row(Func("contour", listOf(f, circle)))
    return row(Func("lim", listOf(contour, row(Sym("R"), Sym("→"), Sym("∞")))))
}
