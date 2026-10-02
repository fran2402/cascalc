package com.example.cas.graph

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Spreadsheet formulas for the data table (beta), as Excel writes them: a cell starting with
 * "=" is worked out from other cells. Columns are lettered A, B, C… and rows numbered from 1;
 * references can be relative (A1) or absolute ($A$1), ranges are A1:B10 or whole columns A:A.
 * Results are numbers (TRUE and FALSE are 1 and 0); errors read as Excel's (#DIV/0!, #REF!…).
 */
object Sheet {
    /** A worked-out cell: a number, or an error like "#DIV/0!". */
    class Value(val number: Double?, val error: String? = null) {
        override fun toString() = error ?: number?.let { DataTable.text(it) } ?: ""
    }

    private class Err(val code: String) : Exception(code)

    /** "A" for 0, "Z" for 25, "AA" for 26… */
    fun columnName(c: Int): String {
        var n = c + 1
        val sb = StringBuilder()
        while (n > 0) { val r = (n - 1) % 26; sb.append('A' + r); n = (n - 1) / 26 }
        return sb.reverse().toString()
    }

    fun columnIndex(name: String): Int = name.uppercase().fold(0) { acc, ch -> acc * 26 + (ch - 'A' + 1) } - 1

    /** Whether the cell's text is a formula. */
    fun isFormula(text: String) = text.trimStart().startsWith("=") && text.trim().length > 1

    /**
     * Every cell's value, working formulas out from the cells they use (each once, in any order;
     * a formula that comes back to itself is #CYCLE!). [columns] are the cells as typed.
     */
    class Book(private val columns: List<List<String>>) {
        private val cache = HashMap<Long, Value>()
        private val inProgress = HashSet<Long>()
        val rows get() = columns.maxOfOrNull { it.size } ?: 0

        private fun key(c: Int, r: Int) = c.toLong() shl 32 or r.toLong()

        fun text(c: Int, r: Int): String = columns.getOrNull(c)?.getOrNull(r) ?: ""

        /** The value of the cell in column [c], row [r] (both from 0). */
        fun value(c: Int, r: Int): Value {
            val k = key(c, r)
            cache[k]?.let { return it }
            val t = text(c, r)
            if (!isFormula(t)) return Value(DataTable.number(t)).also { cache[k] = it }
            if (!inProgress.add(k)) return Value(null, "#CYCLE!")
            val v = try {
                val n = Parser(t.trim().drop(1), this, c, r).run()
                if (n.isNaN()) Value(null, "#NUM!") else if (n.isInfinite()) Value(null, "#DIV/0!") else Value(n)
            } catch (e: Err) { Value(null, e.code) } catch (e: StackOverflowError) { Value(null, "#CYCLE!") }
            inProgress.remove(k)
            cache[k] = v
            return v
        }

        /** The numbers in a range (empty and text cells skipped, as Excel's SUM does); errors pass through. */
        internal fun range(c0: Int, r0: Int, c1: Int, r1: Int): List<Double> {
            val out = ArrayList<Double>()
            for (c in minOf(c0, c1)..maxOf(c0, c1)) for (r in minOf(r0, r1)..minOf(maxOf(r0, r1), rows - 1)) {
                val v = value(c, r)
                v.error?.let { throw Err(it) }
                v.number?.let { out += it }
            }
            return out
        }

        /** The cells of a range in order, empty or not (for criteria and paired ranges). */
        internal fun cells(c0: Int, r0: Int, c1: Int, r1: Int): List<Pair<Value, String>> {
            val out = ArrayList<Pair<Value, String>>()
            for (c in minOf(c0, c1)..maxOf(c0, c1)) for (r in minOf(r0, r1)..minOf(maxOf(r0, r1), rows - 1)) out += value(c, r) to text(c, r)
            return out
        }
    }

    /** One argument of a function: a number, a string, or a range of cells. */
    private sealed class Arg {
        class Num(val v: Double) : Arg()
        class Str(val s: String) : Arg()
        class Range(val c0: Int, val r0: Int, val c1: Int, val r1: Int) : Arg()
        /** An argument that came out as an error, kept so IF and IFERROR can pass over it. */
        class Error(val code: String) : Arg()
    }

    private class Parser(val s: String, val book: Book, val atC: Int, val atR: Int) {
        var i = 0

        fun run(): Double {
            val v = num(expr())
            skip()
            if (i < s.length) throw Err("#VALUE!")
            return v
        }

        fun skip() { while (i < s.length && s[i] == ' ') i++ }
        fun peek(t: String): Boolean { skip(); return s.startsWith(t, i) }
        fun eat(t: String): Boolean { if (peek(t)) { i += t.length; return true }; return false }

        fun num(a: Arg): Double = when (a) {
            is Arg.Num -> a.v
            is Arg.Error -> throw Err(a.code)
            is Arg.Str -> DataTable.number(a.s) ?: throw Err("#VALUE!")
            // A single cell used as a number.
            is Arg.Range -> if (a.c0 == a.c1 && a.r0 == a.r1) book.value(a.c0, a.r0).let { v -> v.error?.let { throw Err(it) }; v.number ?: 0.0 } else throw Err("#VALUE!")
        }

        fun expr(): Arg {
            val a = additive()
            for (op in listOf("<=", ">=", "<>", "=", "<", ">")) if (eat(op)) {
                val x = num(a); val y = num(additive())
                return Arg.Num(if (when (op) { "<=" -> x <= y; ">=" -> x >= y; "<>" -> x != y; "=" -> x == y; "<" -> x < y; else -> x > y }) 1.0 else 0.0)
            }
            return a
        }

        fun additive(): Arg {
            var a = term()
            while (true) {
                a = when {
                    eat("+") -> Arg.Num(num(a) + num(term()))
                    eat("-") -> Arg.Num(num(a) - num(term()))
                    eat("&") -> Arg.Str(text(a) + text(term()))
                    else -> return a
                }
            }
        }

        fun text(a: Arg) = when (a) { is Arg.Str -> a.s; else -> DataTable.text(num(a)) }

        fun term(): Arg {
            var a = power()
            while (true) {
                a = when {
                    eat("*") -> Arg.Num(num(a) * num(power()))
                    eat("/") -> { val d = num(power()); if (d == 0.0) throw Err("#DIV/0!"); Arg.Num(num(a) / d) }
                    else -> return a
                }
            }
        }

        fun power(): Arg {
            val a = unary()
            if (eat("^")) return Arg.Num(num(a).pow(num(power())))
            return a
        }

        fun unary(): Arg {
            if (eat("-")) return Arg.Num(-num(unary()))
            if (eat("+")) return unary()
            val a = primary()
            if (eat("%")) return Arg.Num(num(a) / 100)
            return a
        }

        fun primary(): Arg {
            skip()
            if (i >= s.length) throw Err("#VALUE!")
            val ch = s[i]
            if (ch == '(') { i++; val a = expr(); if (!eat(")")) throw Err("#VALUE!"); return a }
            if (ch == '"') {
                val end = s.indexOf('"', i + 1).takeIf { it > 0 } ?: throw Err("#VALUE!")
                val t = s.substring(i + 1, end); i = end + 1; return Arg.Str(t)
            }
            if (ch.isDigit() || ch == '.') {
                val m = Regex("""\d*\.?\d+([eE][+-]?\d+)?|\d+\.""").matchAt(s, i) ?: throw Err("#VALUE!")
                i += m.value.length; return Arg.Num(m.value.toDouble())
            }
            // A cell, a range, a whole column, or a function.
            Regex("""\$?([A-Za-z]{1,3})\$?(\d+)(?::\$?([A-Za-z]{1,3})\$?(\d+))?""").matchAt(s, i)?.takeIf { m -> !s.startsWith("(", m.range.last + 1) }?.let { m ->
                i += m.value.length
                val c0 = columnIndex(m.groupValues[1]); val r0 = m.groupValues[2].toInt() - 1
                if (r0 < 0) throw Err("#REF!")
                if (m.groupValues[3].isEmpty()) return Arg.Range(c0, r0, c0, r0)
                return Arg.Range(c0, r0, columnIndex(m.groupValues[3]), m.groupValues[4].toInt() - 1)
            }
            Regex("""\$?([A-Za-z]{1,3}):\$?([A-Za-z]{1,3})(?![A-Za-z0-9(])""").matchAt(s, i)?.let { m ->
                i += m.value.length
                return Arg.Range(columnIndex(m.groupValues[1]), 0, columnIndex(m.groupValues[2]), Int.MAX_VALUE / 2)
            }
            val name = Regex("""[A-Za-z][A-Za-z0-9.]*""").matchAt(s, i)?.value ?: throw Err("#VALUE!")
            i += name.length
            val upper = name.uppercase()
            if (upper == "TRUE") return Arg.Num(1.0)
            if (upper == "FALSE") return Arg.Num(0.0)
            if (!eat("(")) throw Err("#NAME?")
            val args = ArrayList<Arg>()
            if (!eat(")")) {
                do { args += argument() } while (eat(",") || eat(";"))
                if (!eat(")")) throw Err("#VALUE!")
            }
            return Arg.Num(call(upper, args))
        }

        /** One argument; an error in it is kept and the rest of it skipped, up to the next , or ). */
        fun argument(): Arg {
            val start = i
            return try { expr() } catch (e: Err) {
                i = start
                var depth = 0; var quoted = false
                while (i < s.length) {
                    val ch = s[i]
                    if (ch == '"') quoted = !quoted
                    else if (!quoted) {
                        if (ch == '(') depth++
                        else if (ch == ')') { if (depth == 0) break; depth-- }
                        else if ((ch == ',' || ch == ';') && depth == 0) break
                    }
                    i++
                }
                Arg.Error(e.code)
            }
        }

        /** Every number in the arguments: ranges spread out, single values as they are. */
        fun numbers(args: List<Arg>): List<Double> = args.flatMap { a -> if (a is Arg.Range) book.range(a.c0, a.r0, a.c1, a.r1) else listOf(num(a)) }

        fun need(args: List<Arg>, n: IntRange) { if (args.size !in n) throw Err("#VALUE!") }

        /** "≥5", "<>0", "3": a COUNTIF-style condition on a number. */
        fun criterion(a: Arg): (Double?) -> Boolean {
            val t = if (a is Arg.Str) a.s.trim() else return { v -> v == num(a) }
            val m = Regex("""^(<=|>=|<>|=|<|>)?\s*(.*)$""").find(t)!!
            val op = m.groupValues[1]; val target = DataTable.number(m.groupValues[2])
            return { v ->
                if (v == null || target == null) op == "<>" && v != target
                else when (op) { "<=" -> v <= target; ">=" -> v >= target; "<>" -> v != target; "<" -> v < target; ">" -> v > target; else -> v == target }
            }
        }

        fun rangeOf(a: Arg): Arg.Range = a as? Arg.Range ?: throw Err((a as? Arg.Error)?.code ?: "#VALUE!")

        fun paired(a: Arg, b: Arg): Pair<DoubleArray, DoubleArray> {
            val x = book.cells(rangeOf(a).c0, rangeOf(a).r0, rangeOf(a).c1, rangeOf(a).r1)
            val y = book.cells(rangeOf(b).c0, rangeOf(b).r0, rangeOf(b).c1, rangeOf(b).r1)
            if (x.size != y.size) throw Err("#N/A")
            val k = x.indices.filter { x[it].first.number != null && y[it].first.number != null }
            return DoubleArray(k.size) { x[k[it]].first.number!! } to DoubleArray(k.size) { y[k[it]].first.number!! }
        }

        fun call(f: String, a: List<Arg>): Double {
            fun one(): Double { need(a, 1..1); return num(a[0]) }
            fun stats(sample: Boolean): Double {
                val v = numbers(a); val n = v.size
                if (n < (if (sample) 2 else 1)) throw Err("#DIV/0!")
                val mean = v.average()
                return v.sumOf { (it - mean) * (it - mean) } / (if (sample) n - 1 else n)
            }
            return when (f) {
                "SUM" -> numbers(a).sum()
                "AVERAGE", "MEAN" -> numbers(a).takeIf { it.isNotEmpty() }?.average() ?: throw Err("#DIV/0!")
                "MIN" -> numbers(a).minOrNull() ?: 0.0
                "MAX" -> numbers(a).maxOrNull() ?: 0.0
                "COUNT" -> numbers(a).size.toDouble()
                "COUNTA" -> a.sumOf { x -> if (x is Arg.Range) book.cells(x.c0, x.r0, x.c1, x.r1).count { it.second.isNotBlank() } else 1 }.toDouble()
                "COUNTBLANK" -> a.sumOf { x -> val r = rangeOf(x); book.cells(r.c0, r.r0, r.c1, r.r1).count { it.second.isBlank() } }.toDouble()
                "PRODUCT" -> numbers(a).fold(1.0) { p, v -> p * v }
                "SUMSQ" -> numbers(a).sumOf { it * it }
                "MEDIAN" -> numbers(a).sorted().let { v -> if (v.isEmpty()) throw Err("#NUM!"); if (v.size % 2 == 1) v[v.size / 2] else (v[v.size / 2 - 1] + v[v.size / 2]) / 2 }
                "STDEV", "STDEV.S" -> sqrt(stats(true))
                "STDEV.P", "STDEVP" -> sqrt(stats(false))
                "VAR", "VAR.S" -> stats(true)
                "VAR.P", "VARP" -> stats(false)
                "LARGE", "SMALL" -> {
                    need(a, 2..2)
                    val v = numbers(listOf(a[0])).sorted().let { if (f == "LARGE") it.reversed() else it }
                    val k = num(a[1]).toInt()
                    v.getOrNull(k - 1) ?: throw Err("#NUM!")
                }
                "SUMPRODUCT" -> {
                    val cols = a.map { x -> val r = rangeOf(x); book.cells(r.c0, r.r0, r.c1, r.r1).map { it.first.number ?: 0.0 } }
                    if (cols.map { it.size }.distinct().size != 1) throw Err("#VALUE!")
                    cols[0].indices.sumOf { k -> cols.fold(1.0) { p, c -> p * c[k] } }
                }
                "COUNTIF", "SUMIF", "AVERAGEIF" -> {
                    need(a, 2..3)
                    val r = rangeOf(a[0]); val test = criterion(a[1])
                    val cells = book.cells(r.c0, r.r0, r.c1, r.r1)
                    val sums = if (a.size == 3) rangeOf(a[2]).let { s -> book.cells(s.c0, s.r0, s.c1, s.r1) } else cells
                    val hit = cells.indices.filter { test(cells[it].first.number) }
                    when (f) {
                        "COUNTIF" -> hit.size.toDouble()
                        "SUMIF" -> hit.sumOf { sums.getOrNull(it)?.first?.number ?: 0.0 }
                        else -> hit.mapNotNull { sums.getOrNull(it)?.first?.number }.takeIf { it.isNotEmpty() }?.average() ?: throw Err("#DIV/0!")
                    }
                }
                "SLOPE", "INTERCEPT", "CORREL", "PEARSON", "RSQ" -> {
                    need(a, 2..2)
                    // Excel's order: known y's first, then known x's.
                    val (y, x) = paired(a[0], a[1])
                    if (x.size < 2) throw Err("#DIV/0!")
                    val mx = x.average(); val my = y.average()
                    val sxy = x.indices.sumOf { (x[it] - mx) * (y[it] - my) }
                    val sxx = x.sumOf { (it - mx) * (it - mx) }; val syy = y.sumOf { (it - my) * (it - my) }
                    if (sxx == 0.0) throw Err("#DIV/0!")
                    when (f) {
                        "SLOPE" -> sxy / sxx
                        "INTERCEPT" -> my - sxy / sxx * mx
                        "RSQ" -> sxy * sxy / (sxx * syy)
                        else -> sxy / sqrt(sxx * syy)
                    }
                }
                "ABS" -> abs(one())
                "SQRT" -> one().let { if (it < 0) throw Err("#NUM!") else sqrt(it) }
                "EXP" -> kotlin.math.exp(one())
                "LN" -> one().let { if (it <= 0) throw Err("#NUM!") else ln(it) }
                "LOG10" -> one().let { if (it <= 0) throw Err("#NUM!") else kotlin.math.log10(it) }
                "LOG" -> { need(a, 1..2); val x = num(a[0]); val b = if (a.size == 2) num(a[1]) else 10.0; if (x <= 0 || b <= 0 || b == 1.0) throw Err("#NUM!"); ln(x) / ln(b) }
                "POWER" -> { need(a, 2..2); num(a[0]).pow(num(a[1])) }
                "MOD" -> { need(a, 2..2); val d = num(a[1]); if (d == 0.0) throw Err("#DIV/0!"); num(a[0]).let { it - d * floor(it / d) } }
                "INT" -> floor(one())
                "SIGN" -> kotlin.math.sign(one())
                "ROUND", "ROUNDUP", "ROUNDDOWN" -> {
                    need(a, 1..2)
                    val x = num(a[0]); val d = if (a.size == 2) num(a[1]).toInt() else 0
                    val mode = when (f) { "ROUNDUP" -> java.math.RoundingMode.UP; "ROUNDDOWN" -> java.math.RoundingMode.DOWN; else -> java.math.RoundingMode.HALF_UP }
                    java.math.BigDecimal(x.toString()).setScale(d, mode).toDouble()
                }
                "FLOOR", "CEILING" -> {
                    need(a, 1..2)
                    val x = num(a[0]); val m = if (a.size == 2) num(a[1]) else 1.0
                    if (m == 0.0) 0.0 else if (f == "FLOOR") floor(x / m) * m else kotlin.math.ceil(x / m) * m
                }
                "PI" -> { need(a, 0..0); Math.PI }
                "SIN" -> kotlin.math.sin(one()); "COS" -> kotlin.math.cos(one()); "TAN" -> kotlin.math.tan(one())
                "ASIN" -> kotlin.math.asin(one()); "ACOS" -> kotlin.math.acos(one()); "ATAN" -> kotlin.math.atan(one())
                "ATAN2" -> { need(a, 2..2); kotlin.math.atan2(num(a[1]), num(a[0])) }
                "SINH" -> kotlin.math.sinh(one()); "COSH" -> kotlin.math.cosh(one()); "TANH" -> kotlin.math.tanh(one())
                "DEGREES" -> Math.toDegrees(one()); "RADIANS" -> Math.toRadians(one())
                "FACT" -> one().let { n -> if (n < 0) throw Err("#NUM!"); (1..floor(n).toInt()).fold(1.0) { p, k -> p * k } }
                "COMBIN" -> { need(a, 2..2); val n = floor(num(a[0])).toInt(); val k = floor(num(a[1])).toInt(); if (k < 0 || k > n) throw Err("#NUM!"); (1..k).fold(1.0) { p, j -> p * (n - k + j) / j } }
                "IF" -> { need(a, 2..3); if (num(a[0]) != 0.0) num(a[1]) else if (a.size == 3) num(a[2]) else 0.0 }
                "IFERROR" -> { need(a, 2..2); try { num(a[0]).also { if (it.isNaN()) throw Err("#NUM!") } } catch (e: Err) { num(a[1]) } }
                "AND" -> if (numbers(a).all { it != 0.0 }) 1.0 else 0.0
                "OR" -> if (numbers(a).any { it != 0.0 }) 1.0 else 0.0
                "NOT" -> if (one() == 0.0) 1.0 else 0.0
                "ROW" -> if (a.isEmpty()) atR + 1.0 else (rangeOf(a[0]).r0 + 1).toDouble()
                "COLUMN" -> if (a.isEmpty()) atC + 1.0 else (rangeOf(a[0]).c0 + 1).toDouble()
                else -> throw Err("#NAME?")
            }
        }
    }

    /**
     * A formula copied [dRows] rows down (and [dCols] columns across): relative references move
     * with it, $-fixed parts stay, as Excel's fill does.
     */
    fun shift(formula: String, dRows: Int, dCols: Int = 0): String =
        Regex("""(?<![A-Za-z0-9.$])(\$?)([A-Za-z]{1,3})(\$?)(\d+)(?![A-Za-z0-9(])""").replace(formula) { m ->
            // Leave text in quotes alone.
            val before = formula.substring(0, m.range.first)
            if (before.count { it == '"' } % 2 == 1) return@replace m.value
            val (colFixed, col, rowFixed, row) = m.destructured
            val c = if (colFixed.isEmpty()) columnName((columnIndex(col) + dCols).coerceAtLeast(0)) else col.uppercase()
            val r = if (rowFixed.isEmpty()) (row.toInt() + dRows).coerceAtLeast(1) else row.toInt()
            "$colFixed$c$rowFixed$r"
        }

    /** The functions, for the ƒx sheet: name, how to write it, what it does. */
    val FUNCTIONS: List<Triple<String, String, String>> = listOf(
        Triple("SUM", "SUM(A1:A10)", "Adds the numbers"),
        Triple("AVERAGE", "AVERAGE(B:B)", "The mean"),
        Triple("MIN / MAX", "MAX(A1:A10)", "Smallest or largest"),
        Triple("COUNT", "COUNT(A:A)", "How many numbers"),
        Triple("MEDIAN", "MEDIAN(A:A)", "The middle value"),
        Triple("STDEV / STDEV.P", "STDEV(B1:B20)", "Standard deviation (sample or population)"),
        Triple("VAR / VAR.P", "VAR(B:B)", "Variance"),
        Triple("LARGE / SMALL", "LARGE(A:A, 2)", "The k-th largest or smallest"),
        Triple("PRODUCT · SUMSQ", "SUMSQ(A:A)", "Product, sum of squares"),
        Triple("SUMPRODUCT", "SUMPRODUCT(A:A, B:B)", "Sum of products, row by row"),
        Triple("COUNTIF · SUMIF · AVERAGEIF", "COUNTIF(A:A, \">5\")", "With a condition"),
        Triple("SLOPE · INTERCEPT", "SLOPE(B:B, A:A)", "Least-squares line (y's first)"),
        Triple("CORREL · RSQ", "RSQ(B:B, A:A)", "Correlation, R²"),
        Triple("IF · AND · OR · NOT", "IF(A1>0, A1, 0)", "Choices and logic"),
        Triple("IFERROR", "IFERROR(1/A1, 0)", "A fallback for errors"),
        Triple("ROUND · ROUNDUP · ROUNDDOWN", "ROUND(A1, 2)", "To a number of decimals"),
        Triple("INT · FLOOR · CEILING · MOD", "MOD(A1, 3)", "Whole numbers and remainders"),
        Triple("ABS · SIGN · SQRT · POWER", "SQRT(A1^2+B1^2)", "Sizes, roots and powers"),
        Triple("EXP · LN · LOG · LOG10", "LOG(A1, 2)", "Exponentials and logarithms"),
        Triple("SIN · COS · TAN · ATAN2 …", "SIN(RADIANS(A1))", "Trigonometry, in radians"),
        Triple("DEGREES · RADIANS · PI", "DEGREES(PI()/4)", "Angle units"),
        Triple("FACT · COMBIN", "COMBIN(10, 3)", "Factorials and combinations"),
        Triple("ROW · COLUMN", "ROW()", "Where the cell is"),
    )
}
