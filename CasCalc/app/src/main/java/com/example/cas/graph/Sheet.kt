package com.example.cas.graph

import com.example.cas.graph.SheetMath.betaI
import com.example.cas.graph.SheetMath.chi2Cdf
import com.example.cas.graph.SheetMath.fCdf
import com.example.cas.graph.SheetMath.invert
import com.example.cas.graph.SheetMath.lnChoose
import com.example.cas.graph.SheetMath.lnGamma
import com.example.cas.graph.SheetMath.normCdf
import com.example.cas.graph.SheetMath.normInv
import com.example.cas.graph.SheetMath.normPdf
import com.example.cas.graph.SheetMath.tCdf
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Spreadsheet formulas for the data table (beta), as Excel writes them: a cell starting with
 * "=" is worked out from other cells. Columns are lettered A, B, C… and rows numbered from 1;
 * references can be relative (A1) or absolute ($A$1), ranges are A1:B10 or whole columns A:A.
 * Results are numbers (TRUE and FALSE are 1 and 0) or text; errors read as Excel's (#DIV/0!…).
 */
object Sheet {
    /** A worked-out cell: a number, some text, or an error like "#DIV/0!". */
    class Value(val number: Double?, val error: String? = null, val text: String? = null) {
        val isBlank get() = number == null && error == null && text.isNullOrEmpty()
        override fun toString() = error ?: text ?: number?.let { DataTable.text(it) } ?: ""
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
        val rows = columns.maxOfOrNull { it.size } ?: 0

        private fun key(c: Int, r: Int) = c.toLong() shl 32 or r.toLong()

        fun text(c: Int, r: Int): String = columns.getOrNull(c)?.getOrNull(r) ?: ""

        /** The value of the cell in column [c], row [r] (both from 0). */
        fun value(c: Int, r: Int): Value {
            val k = key(c, r)
            cache[k]?.let { return it }
            val t = text(c, r)
            if (!isFormula(t)) {
                val n = DataTable.number(t)
                return Value(n, text = if (n == null && t.isNotBlank()) t else null).also { cache[k] = it }
            }
            if (!inProgress.add(k)) return Value(null, "#CYCLE!")
            val v = try {
                val p = Parser(t.trim().drop(1), this, c, r)
                p.result(p.run())
            } catch (e: Err) { Value(null, e.code) } catch (e: StackOverflowError) { Value(null, "#CYCLE!") }
            inProgress.remove(k)
            cache[k] = v
            return v
        }
    }

    /** One argument of a function: a number, a string, a range of cells, or an error. */
    private sealed class Arg {
        class Num(val v: Double) : Arg()
        class Str(val s: String) : Arg()
        class Range(c0: Int, r0: Int, c1: Int, r1: Int) : Arg() {
            val c0 = minOf(c0, c1); val c1 = maxOf(c0, c1); val r0 = minOf(r0, r1); val r1 = maxOf(r0, r1)
            val single get() = c0 == c1 && r0 == r1
        }
        /** An argument that came out as an error, kept so IF and IFERROR can pass over it. */
        class Error(val code: String) : Arg()
    }

    private val EPOCH: LocalDate = LocalDate.of(1899, 12, 30)
    private fun dateOf(serial: Double): LocalDate = EPOCH.plusDays(floor(serial).toLong())
    private fun serialOf(d: LocalDate) = ChronoUnit.DAYS.between(EPOCH, d).toDouble()
    private val ERROR_TYPES = listOf("#NULL!", "#DIV/0!", "#VALUE!", "#REF!", "#NAME?", "#NUM!", "#N/A")
    private val TRUE = Arg.Num(1.0)
    private val FALSE = Arg.Num(0.0)
    private fun bool(b: Boolean) = if (b) TRUE else FALSE
    private fun fmt(v: Double) = DataTable.text(v)

    /** An Excel pattern with * and ? (and ~ to escape them) as a regular expression. */
    private fun wild(p: String): Regex {
        val sb = StringBuilder()
        var k = 0
        while (k < p.length) {
            val ch = p[k]
            when {
                ch == '~' && k + 1 < p.length -> { sb.append(Regex.escape(p[k + 1].toString())); k++ }
                ch == '*' -> sb.append(".*")
                ch == '?' -> sb.append(".")
                else -> sb.append(Regex.escape(ch.toString()))
            }
            k++
        }
        return Regex(sb.toString(), setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    }

    private class Parser(val s: String, val book: Book, val atC: Int, val atR: Int) {
        var i = 0

        fun run(): Arg {
            val v = expr()
            skip()
            if (i < s.length) throw Err(if (s[i] == ')') "#VALUE!" else "#NAME?")
            return v
        }

        /** What the formula shows: a lone cell's value, a number, text or an error. */
        fun result(a: Arg): Value = when (a) {
            is Arg.Num -> if (a.v.isNaN() || a.v.isInfinite()) Value(null, "#NUM!") else Value(a.v)
            is Arg.Str -> Value(null, text = a.s)
            is Arg.Error -> Value(null, a.code)
            is Arg.Range -> if (!a.single) Value(null, "#VALUE!") else book.value(a.c0, a.r0).let { if (it.isBlank) Value(0.0) else it }
        }

        fun skip() { while (i < s.length && s[i].isWhitespace()) i++ }
        fun peek(t: String): Boolean { skip(); return s.startsWith(t, i) }
        fun eat(t: String): Boolean { if (peek(t)) { i += t.length; return true }; return false }

        fun cell(a: Arg.Range) = book.value(a.c0, a.r0)

        fun num(a: Arg): Double = when (a) {
            is Arg.Num -> a.v
            is Arg.Error -> throw Err(a.code)
            is Arg.Str -> DataTable.number(a.s) ?: when (a.s.uppercase()) { "TRUE" -> 1.0; "FALSE" -> 0.0; else -> throw Err("#VALUE!") }
            is Arg.Range -> {
                if (!a.single) throw Err("#VALUE!")
                val v = cell(a)
                v.error?.let { throw Err(it) }
                v.number ?: if (v.text.isNullOrBlank()) 0.0 else throw Err("#VALUE!")
            }
        }

        fun int(a: Arg) = num(a).let { if (it.isNaN()) throw Err("#NUM!") else floor(it) }.toLong()

        fun str(a: Arg): String = when (a) {
            is Arg.Str -> a.s
            is Arg.Num -> fmt(a.v)
            is Arg.Error -> throw Err(a.code)
            is Arg.Range -> {
                if (!a.single) throw Err("#VALUE!")
                val v = cell(a)
                v.error?.let { throw Err(it) }
                v.text ?: v.number?.let { fmt(it) } ?: ""
            }
        }

        /** A number or a string, as the argument is (blank cells are 0). */
        fun scalar(a: Arg): Any = when (a) {
            is Arg.Num -> a.v
            is Arg.Str -> a.s
            is Arg.Error -> throw Err(a.code)
            is Arg.Range -> {
                if (!a.single) throw Err("#VALUE!")
                val v = cell(a)
                v.error?.let { throw Err(it) }
                v.number ?: v.text ?: 0.0
            }
        }

        fun truth(a: Arg) = num(a) != 0.0

        /** Excel's order: numbers before text, text without regard to case. */
        fun compare(x: Any, y: Any): Int = when {
            x is Double && y is Double -> x.compareTo(y)
            x is String && y is String -> x.compareTo(y, ignoreCase = true)
            x is Double -> -1
            else -> 1
        }

        fun expr(): Arg {
            val a = concat()
            for (op in listOf("<=", ">=", "<>", "=", "<", ">")) if (eat(op)) {
                val x = scalar(a); val y = scalar(concat())
                val c = compare(x, y)
                return bool(when (op) { "<=" -> c <= 0; ">=" -> c >= 0; "<>" -> c != 0; "=" -> c == 0; "<" -> c < 0; else -> c > 0 })
            }
            return a
        }

        fun concat(): Arg {
            var a = additive()
            while (eat("&")) a = Arg.Str(str(a) + str(additive()))
            return a
        }

        fun additive(): Arg {
            var a = term()
            while (true) {
                a = when {
                    eat("+") -> Arg.Num(num(a) + num(term()))
                    eat("-") -> Arg.Num(num(a) - num(term()))
                    else -> return a
                }
            }
        }

        fun term(): Arg {
            var a = power()
            while (true) {
                a = when {
                    eat("*") -> Arg.Num(num(a) * num(power()))
                    eat("/") -> { val x = num(a); val d = num(power()); if (d == 0.0) throw Err("#DIV/0!"); Arg.Num(x / d) }
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
            var a = primary()
            while (eat("%")) a = Arg.Num(num(a) / 100)
            return a
        }

        fun primary(): Arg {
            skip()
            if (i >= s.length) throw Err("#VALUE!")
            val ch = s[i]
            if (ch == '(') { i++; val a = expr(); if (!eat(")")) throw Err("#VALUE!"); return a }
            if (ch == '"') {
                // "" inside quotes is a quote.
                val sb = StringBuilder(); i++
                while (true) {
                    if (i >= s.length) throw Err("#VALUE!")
                    if (s[i] == '"') { if (i + 1 < s.length && s[i + 1] == '"') { sb.append('"'); i += 2; continue }; i++; break }
                    sb.append(s[i]); i++
                }
                return Arg.Str(sb.toString())
            }
            if (ch == '#') {
                ERROR_TYPES.firstOrNull { s.startsWith(it, i, ignoreCase = true) }?.let { i += it.length; return Arg.Error(it) }
            }
            if (ch.isDigit() || ch == '.') {
                val m = Regex("""\d*\.?\d+([eE][+-]?\d+)?|\d+\.""").matchAt(s, i) ?: throw Err("#VALUE!")
                i += m.value.length; return Arg.Num(m.value.toDouble())
            }
            // A cell, a range, a whole column, or a function.
            Regex("""\$?([A-Za-z]{1,3})\$?(\d+)(?::\$?([A-Za-z]{1,3})\$?(\d+))?(?![A-Za-z0-9._(])""").matchAt(s, i)?.let { m ->
                i += m.value.length
                val c0 = columnIndex(m.groupValues[1]); val r0 = m.groupValues[2].toInt() - 1
                if (r0 < 0) throw Err("#REF!")
                if (m.groupValues[3].isEmpty()) return Arg.Range(c0, r0, c0, r0)
                val r1 = m.groupValues[4].toInt() - 1
                if (r1 < 0) throw Err("#REF!")
                return Arg.Range(c0, r0, columnIndex(m.groupValues[3]), r1)
            }
            Regex("""\$?([A-Za-z]{1,3}):\$?([A-Za-z]{1,3})(?![A-Za-z0-9(])""").matchAt(s, i)?.let { m ->
                i += m.value.length
                return Arg.Range(columnIndex(m.groupValues[1]), 0, columnIndex(m.groupValues[2]), Int.MAX_VALUE / 2)
            }
            val name = Regex("""[A-Za-z_][A-Za-z0-9._]*""").matchAt(s, i)?.value ?: throw Err("#VALUE!")
            i += name.length
            val upper = name.uppercase().removePrefix("_XLFN.")
            if (!eat("(")) return when (upper) { "TRUE" -> TRUE; "FALSE" -> FALSE; else -> throw Err("#NAME?") }
            val args = ArrayList<Arg>()
            if (!eat(")")) {
                do { args += argument() } while (eat(",") || eat(";"))
                if (!eat(")")) throw Err("#VALUE!")
            }
            return call(upper, args)
        }

        /** One argument; an error in it is kept and the rest of it skipped, up to the next , or ). */
        fun argument(): Arg {
            skip()
            // An empty argument, as in ROUND(A1,) or VLOOKUP(x, r, 2, ).
            if (i < s.length && (s[i] == ',' || s[i] == ';' || s[i] == ')')) return Arg.Str("")
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

        // ---------- ranges ----------

        fun rangeOf(a: Arg): Arg.Range = a as? Arg.Range ?: throw Err((a as? Arg.Error)?.code ?: "#VALUE!")
        fun height(r: Arg.Range) = maxOf(0, minOf(r.r1, book.rows - 1) - r.r0 + 1)
        fun width(r: Arg.Range) = r.c1 - r.c0 + 1

        /** A range's values row by row (whole columns stop at the last row). */
        fun values(r: Arg.Range): List<Value> {
            if (r.single) return listOf(cell(r))
            val out = ArrayList<Value>()
            for (row in r.r0 until r.r0 + height(r)) for (c in r.c0..r.c1) out += book.value(c, row)
            return out
        }

        /** An argument's values: a range's cells, or the value itself. */
        fun flat(a: Arg): List<Value> = when (a) {
            is Arg.Range -> values(a)
            is Arg.Num -> listOf(Value(a.v))
            is Arg.Str -> listOf(Value(null, text = a.s))
            is Arg.Error -> listOf(Value(null, a.code))
        }

        /** Every number in the arguments: in ranges, text and empty cells are skipped (as SUM does). */
        fun numbers(args: List<Arg>, ignoreErrors: Boolean = false): List<Double> = args.flatMap { a ->
            if (a is Arg.Range) values(a).mapNotNull { v -> if (!ignoreErrors) v.error?.let { throw Err(it) }; v.number }
            else if (ignoreErrors && a is Arg.Error) emptyList() else listOf(num(a))
        }

        /** The numbers as AVERAGEA counts them: text in ranges is 0. */
        fun numbersA(args: List<Arg>): List<Double> = args.flatMap { a ->
            if (a is Arg.Range) values(a).mapNotNull { v -> v.error?.let { throw Err(it) }; v.number ?: if (v.text.isNullOrEmpty()) null else 0.0 }
            else listOf(num(a))
        }

        fun need(args: List<Arg>, n: IntRange) { if (args.size !in n) throw Err("#VALUE!") }
        fun given(a: List<Arg>, k: Int) = a.getOrNull(k)?.takeUnless { it is Arg.Str && it.s.isEmpty() }
        fun opt(a: List<Arg>, k: Int, d: Double) = given(a, k)?.let { num(it) } ?: d
        fun optB(a: List<Arg>, k: Int, d: Boolean) = given(a, k)?.let { truth(it) } ?: d

        /** Two ranges as (x, y) pairs, rows where both are numbers. */
        fun paired(a: Arg, b: Arg): Pair<DoubleArray, DoubleArray> {
            val x = flat(a); val y = flat(b)
            if (x.size != y.size) throw Err("#N/A")
            x.forEach { v -> v.error?.let { throw Err(it) } }; y.forEach { v -> v.error?.let { throw Err(it) } }
            val k = x.indices.filter { x[it].number != null && y[it].number != null }
            return DoubleArray(k.size) { x[k[it]].number!! } to DoubleArray(k.size) { y[k[it]].number!! }
        }

        /** A COUNTIF-style condition: 5, ">5", "<>0", "apple", "a*", "" (blank). */
        fun criterion(a: Arg): (Value) -> Boolean {
            val c = scalar(a)
            if (c is Double) return { v -> v.number == c }
            val m = Regex("""^(<=|>=|<>|=|<|>)?(.*)$""", RegexOption.DOT_MATCHES_ALL).find(c as String)!!
            val op = m.groupValues[1]; val rest = m.groupValues[2]
            val n = DataTable.number(rest)
            val pattern = wild(rest)
            return { v ->
                val shown = v.text ?: v.number?.let { fmt(it) } ?: ""
                when {
                    v.error != null -> false
                    rest.isEmpty() -> when (op) { "<>" -> !v.isBlank; "", "=" -> v.isBlank; else -> false }
                    n != null -> v.number?.let { x -> when (op) { "<=" -> x <= n; ">=" -> x >= n; "<>" -> x != n; "<" -> x < n; ">" -> x > n; else -> x == n } } ?: (op == "<>")
                    op == "" || op == "=" -> pattern.matches(shown)
                    op == "<>" -> !pattern.matches(shown)
                    else -> v.text?.let { t -> val k = t.compareTo(rest, ignoreCase = true); when (op) { "<=" -> k <= 0; ">=" -> k >= 0; "<" -> k < 0; else -> k > 0 } } ?: false
                }
            }
        }

        /** The same-shaped range starting where [r] starts, as SUMIF uses its sum range. */
        fun resized(r: Arg.Range, like: Arg.Range) = Arg.Range(r.c0, r.r0, r.c0 + width(like) - 1, r.r0 + maxOf(height(like), 1) - 1)

        /** Which cells pass every (range, condition) pair, for COUNTIFS and the like. */
        fun hits(pairs: List<Arg>): List<Int> {
            if (pairs.isEmpty() || pairs.size % 2 != 0) throw Err("#VALUE!")
            val first = rangeOf(pairs[0])
            val lists = pairs.chunked(2).map { (r, c) -> values(resized(rangeOf(r), first)) to criterion(c) }
            val n = lists[0].first.size
            return (0 until n).filter { k -> lists.all { (vs, test) -> vs.getOrNull(k)?.let(test) ?: false } }
        }

        fun matches(v: Value, target: Any, wildcard: Boolean): Boolean = when (target) {
            is Double -> v.number == target
            else -> v.text?.let { t -> if (wildcard) wild(target as String).matches(t) else t.equals(target as String, ignoreCase = true) } ?: false
        }

        /** How a cell compares with [target]: numbers before text; null for blanks and errors. */
        fun order(v: Value, target: Any): Int? = when {
            v.error != null || v.isBlank -> null
            v.number != null -> if (target is Double) v.number.compareTo(target) else -1
            else -> if (target is String) v.text!!.compareTo(target, ignoreCase = true) else 1
        }

        /** The position (from 0) of [target] in [list]: exact (type 0), or by Excel's sorted search (1 or −1). */
        fun find(list: List<Value>, target: Any, type: Int): Int? {
            if (type == 0) return list.indexOfFirst { matches(it, target, wildcard = target is String) }.takeIf { it >= 0 }
            var best: Int? = null
            for ((k, v) in list.withIndex()) {
                val c = order(v, target) ?: continue
                if (type > 0) { if (c <= 0) best = k else break } else { if (c >= 0) best = k else break }
            }
            return best
        }

        /** XLOOKUP's search: exact (0), or the next smaller (−1) or larger (1), or wildcards (2); from the end with −1. */
        fun xfind(list: List<Value>, target: Any, mode: Int, search: Int): Int? {
            val indices = if (search < 0) list.indices.reversed() else list.indices
            indices.firstOrNull { matches(list[it], target, wildcard = mode == 2 && target is String) }?.let { return it }
            if (mode != 1 && mode != -1) return null
            var best: Int? = null
            for (k in indices) {
                val c = order(list[k], target) ?: continue
                if (mode == -1 && c > 0 || mode == 1 && c < 0) continue
                val b = best?.let { list[it] }
                if (b == null) { best = k; continue }
                val vs = order(list[k], b.number ?: b.text!!) ?: continue
                if (mode == -1 && vs > 0 || mode == 1 && vs < 0) best = k
            }
            return best
        }

        // ---------- functions ----------

        fun call(f: String, a: List<Arg>): Arg {
            logic(f, a)?.let { return it }
            lookup(f, a)?.let { return it }
            text(f, a)?.let { return it }
            date(f, a)?.let { return it }
            (math(f, a) ?: stats(f, a) ?: distributions(f, a) ?: finance(f, a) ?: engineering(f, a))?.let { return Arg.Num(it) }
            engineeringText(f, a)?.let { return it }
            throw Err("#NAME?")
        }

        fun errorOf(a: Arg): String? = when (a) {
            is Arg.Error -> a.code
            is Arg.Range -> if (a.single) cell(a).error else null
            else -> null
        }

        fun logic(f: String, a: List<Arg>): Arg? = when (f) {
            "TRUE" -> TRUE
            "FALSE" -> FALSE
            "IF" -> { need(a, 1..3); if (truth(a[0])) a.getOrElse(1) { TRUE } else a.getOrElse(2) { FALSE } }
            "IFS" -> {
                if (a.size < 2 || a.size % 2 != 0) throw Err("#VALUE!")
                a.chunked(2).firstOrNull { truth(it[0]) }?.get(1) ?: throw Err("#N/A")
            }
            "SWITCH" -> {
                if (a.size < 3) throw Err("#VALUE!")
                val x = scalar(a[0])
                val rest = a.drop(1)
                rest.chunked(2).firstOrNull { it.size == 2 && compare(scalar(it[0]), x) == 0 }?.get(1)
                    ?: if (rest.size % 2 == 1) rest.last() else throw Err("#N/A")
            }
            "IFERROR" -> { need(a, 2..2); if (errorOf(a[0]) != null) a[1] else a[0] }
            "IFNA" -> { need(a, 2..2); if (errorOf(a[0]) == "#N/A") a[1] else a[0] }
            "AND" -> bool(numbers(a).all { it != 0.0 })
            "OR" -> bool(numbers(a).any { it != 0.0 })
            "XOR" -> bool(numbers(a).count { it != 0.0 } % 2 == 1)
            "NOT" -> { need(a, 1..1); bool(!truth(a[0])) }
            "NA" -> Arg.Error("#N/A")
            "ISNUMBER" -> { need(a, 1..1); bool(flat(a[0]).singleOrNull()?.number != null) }
            "ISTEXT" -> { need(a, 1..1); bool(flat(a[0]).singleOrNull()?.text != null) }
            "ISNONTEXT" -> { need(a, 1..1); bool(flat(a[0]).singleOrNull()?.text == null) }
            "ISBLANK" -> { need(a, 1..1); bool((a[0] as? Arg.Range)?.let { it.single && book.text(it.c0, it.r0).isBlank() } ?: false) }
            "ISERROR" -> { need(a, 1..1); bool(errorOf(a[0]) != null) }
            "ISERR" -> { need(a, 1..1); bool(errorOf(a[0]).let { it != null && it != "#N/A" }) }
            "ISNA" -> { need(a, 1..1); bool(errorOf(a[0]) == "#N/A") }
            "ISEVEN" -> { need(a, 1..1); bool(int(a[0]) % 2 == 0L) }
            "ISODD" -> { need(a, 1..1); bool(int(a[0]) % 2 != 0L) }
            "ISLOGICAL" -> { need(a, 1..1); FALSE }
            "ISREF" -> { need(a, 1..1); bool(a[0] is Arg.Range) }
            "ISFORMULA" -> { need(a, 1..1); val r = rangeOf(a[0]); bool(isFormula(book.text(r.c0, r.r0))) }
            "ERROR.TYPE" -> { need(a, 1..1); errorOf(a[0])?.let { Arg.Num(ERROR_TYPES.indexOf(it) + 1.0) } ?: Arg.Error("#N/A") }
            "N" -> { need(a, 1..1); Arg.Num(flat(a[0]).singleOrNull()?.number ?: 0.0) }
            "T" -> { need(a, 1..1); Arg.Str(flat(a[0]).singleOrNull()?.text ?: "") }
            "TYPE" -> { need(a, 1..1); Arg.Num(when { errorOf(a[0]) != null -> 16.0; (a[0] as? Arg.Range)?.single == false -> 64.0; scalar(a[0]) is String -> 2.0; else -> 1.0 }) }
            "ROW" -> if (a.isEmpty()) Arg.Num(atR + 1.0) else Arg.Num(rangeOf(a[0]).r0 + 1.0)
            "COLUMN" -> if (a.isEmpty()) Arg.Num(atC + 1.0) else Arg.Num(rangeOf(a[0]).c0 + 1.0)
            else -> null
        }

        fun lookup(f: String, a: List<Arg>): Arg? = when (f) {
            "CHOOSE" -> { if (a.size < 2) throw Err("#VALUE!"); val k = int(a[0]).toInt(); a.getOrNull(k)?.takeIf { k >= 1 } ?: throw Err("#VALUE!") }
            "ROWS" -> { need(a, 1..1); val r = rangeOf(a[0]); Arg.Num(if (r.r1 >= Int.MAX_VALUE / 2) book.rows.toDouble() else r.r1 - r.r0 + 1.0) }
            "COLUMNS" -> { need(a, 1..1); Arg.Num(width(rangeOf(a[0])).toDouble()) }
            "INDEX" -> {
                need(a, 2..3)
                val r = rangeOf(a[0])
                var row = int(a[1]).toInt(); var col = if (a.size == 3) int(a[2]).toInt() else 1
                // In a single row, one number picks the column.
                if (a.size == 2 && r.r0 == r.r1 && width(r) > 1) { col = row; row = 1 }
                if (row < 0 || col < 0 || col > width(r) || (r.r1 < Int.MAX_VALUE / 2 && row > r.r1 - r.r0 + 1)) throw Err("#REF!")
                when {
                    row == 0 && col == 0 -> r
                    row == 0 -> Arg.Range(r.c0 + col - 1, r.r0, r.c0 + col - 1, r.r1)
                    col == 0 -> Arg.Range(r.c0, r.r0 + row - 1, r.c1, r.r0 + row - 1)
                    else -> Arg.Range(r.c0 + col - 1, r.r0 + row - 1, r.c0 + col - 1, r.r0 + row - 1)
                }
            }
            "MATCH" -> {
                need(a, 2..3)
                val k = find(flat(a[1]), scalar(a[0]), opt(a, 2, 1.0).toInt().coerceIn(-1, 1)) ?: throw Err("#N/A")
                Arg.Num(k + 1.0)
            }
            "XMATCH" -> {
                need(a, 2..4)
                Arg.Num(xfind(flat(a[1]), scalar(a[0]), opt(a, 2, 0.0).toInt(), opt(a, 3, 1.0).toInt())?.plus(1.0) ?: throw Err("#N/A"))
            }
            "VLOOKUP", "HLOOKUP" -> {
                need(a, 3..4)
                val t = rangeOf(a[1]); val n = int(a[2]).toInt()
                val vertical = f == "VLOOKUP"
                if (n < 1) throw Err("#VALUE!")
                if (n > (if (vertical) width(t) else t.r1 - t.r0 + 1)) throw Err("#REF!")
                val keys = if (vertical) values(Arg.Range(t.c0, t.r0, t.c0, t.r1)) else values(Arg.Range(t.c0, t.r0, t.c1, t.r0))
                val k = find(keys, scalar(a[0]), if (optB(a, 3, true)) 1 else 0) ?: throw Err("#N/A")
                if (vertical) Arg.Range(t.c0 + n - 1, t.r0 + k, t.c0 + n - 1, t.r0 + k) else Arg.Range(t.c0 + k, t.r0 + n - 1, t.c0 + k, t.r0 + n - 1)
            }
            "XLOOKUP" -> {
                need(a, 3..6)
                val keys = flat(a[1]); val out = rangeOf(a[2])
                val k = xfind(keys, scalar(a[0]), opt(a, 4, 0.0).toInt(), opt(a, 5, 1.0).toInt())
                when {
                    k != null -> if (width(rangeOf(a[1])) == 1) Arg.Range(out.c0, out.r0 + k, out.c1, out.r0 + k) else Arg.Range(out.c0 + k, out.r0, out.c0 + k, out.r1)
                    given(a, 3) != null -> a[3]
                    else -> throw Err("#N/A")
                }
            }
            "LOOKUP" -> {
                need(a, 2..3)
                val k = find(flat(a[1]), scalar(a[0]), 1) ?: throw Err("#N/A")
                val out = rangeOf(a.getOrElse(2) { a[1] })
                if (out.c0 == out.c1) Arg.Range(out.c0, out.r0 + k, out.c0, out.r0 + k) else Arg.Range(out.c0 + k, out.r0, out.c0 + k, out.r0)
            }
            "OFFSET" -> {
                need(a, 3..5)
                val r = rangeOf(a[0])
                val dr = int(a[1]).toInt(); val dc = int(a[2]).toInt()
                val h = given(a, 3)?.let { int(it).toInt() } ?: (r.r1 - r.r0 + 1)
                val w = given(a, 4)?.let { int(it).toInt() } ?: width(r)
                if (r.r0 + dr < 0 || r.c0 + dc < 0 || h < 1 || w < 1) throw Err("#REF!")
                Arg.Range(r.c0 + dc, r.r0 + dr, r.c0 + dc + w - 1, r.r0 + dr + h - 1)
            }
            "INDIRECT" -> {
                need(a, 1..2)
                val p = Parser(str(a[0]).trim(), book, atC, atR)
                val r = try { p.primary() } catch (e: Err) { null }
                p.skip()
                if (r !is Arg.Range || p.i < p.s.length) throw Err("#REF!")
                r
            }
            "ADDRESS" -> {
                need(a, 2..5)
                val row = int(a[0]); val col = int(a[1]).toInt()
                if (row < 1 || col < 1) throw Err("#VALUE!")
                val abs = opt(a, 2, 1.0).toInt()
                Arg.Str((if (abs == 1 || abs == 3) "$" else "") + columnName(col - 1) + (if (abs == 1 || abs == 2) "$" else "") + row)
            }
            else -> null
        }

        fun text(f: String, a: List<Arg>): Arg? {
            fun s0() = str(a[0])
            return when (f) {
                "LEN" -> { need(a, 1..1); Arg.Num(s0().length.toDouble()) }
                "UPPER" -> { need(a, 1..1); Arg.Str(s0().uppercase()) }
                "LOWER" -> { need(a, 1..1); Arg.Str(s0().lowercase()) }
                "PROPER" -> { need(a, 1..1); Arg.Str(Regex("""\p{L}+""").replace(s0().lowercase()) { m -> m.value.replaceFirstChar { it.uppercase() } }) }
                "TRIM" -> { need(a, 1..1); Arg.Str(s0().trim().replace(Regex(" +"), " ")) }
                "CLEAN" -> { need(a, 1..1); Arg.Str(s0().filter { it.code >= 32 }) }
                "LEFT", "RIGHT" -> {
                    need(a, 1..2)
                    val n = opt(a, 1, 1.0).toInt().also { if (it < 0) throw Err("#VALUE!") }
                    Arg.Str(if (f == "LEFT") s0().take(n) else s0().takeLast(n))
                }
                "MID" -> {
                    need(a, 3..3)
                    val start = int(a[1]).toInt(); val n = int(a[2]).toInt()
                    if (start < 1 || n < 0) throw Err("#VALUE!")
                    Arg.Str(s0().drop(start - 1).take(n))
                }
                "CONCAT", "CONCATENATE" -> Arg.Str(a.joinToString("") { x -> if (x is Arg.Range && !x.single) values(x).joinToString("") { v -> v.error?.let { throw Err(it) }; v.toString() } else str(x) })
                "TEXTJOIN" -> {
                    if (a.size < 3) throw Err("#VALUE!")
                    val sep = str(a[0]); val skipEmpty = truth(a[1])
                    val parts = a.drop(2).flatMap { x -> if (x is Arg.Range) values(x).map { v -> v.error?.let { throw Err(it) }; v.toString() } else listOf(str(x)) }
                    Arg.Str(parts.filter { !skipEmpty || it.isNotEmpty() }.joinToString(sep))
                }
                "REPT" -> { need(a, 2..2); val n = int(a[1]).toInt(); if (n < 0) throw Err("#VALUE!"); Arg.Str(s0().repeat(n)) }
                "SUBSTITUTE" -> {
                    need(a, 3..4)
                    val t = s0(); val old = str(a[1]); val new = str(a[2])
                    if (old.isEmpty()) Arg.Str(t)
                    else if (a.size == 4) {
                        val n = int(a[3]).toInt(); if (n < 1) throw Err("#VALUE!")
                        var at = -1
                        repeat(n) { at = t.indexOf(old, at + 1); if (at < 0) return Arg.Str(t) }
                        Arg.Str(t.substring(0, at) + new + t.substring(at + old.length))
                    } else Arg.Str(t.replace(old, new))
                }
                "REPLACE" -> {
                    need(a, 4..4)
                    val t = s0(); val start = int(a[1]).toInt(); val n = int(a[2]).toInt()
                    if (start < 1 || n < 0) throw Err("#VALUE!")
                    val from = minOf(start - 1, t.length)
                    Arg.Str(t.substring(0, from) + str(a[3]) + t.substring(minOf(from + n, t.length)))
                }
                "FIND", "SEARCH" -> {
                    need(a, 2..3)
                    val what = s0(); val t = str(a[1]); val start = opt(a, 2, 1.0).toInt()
                    if (start < 1 || start > t.length + 1) throw Err("#VALUE!")
                    val k = if (f == "FIND") t.indexOf(what, start - 1) else wild(what).find(t, start - 1)?.range?.first ?: -1
                    if (k < 0) throw Err("#VALUE!")
                    Arg.Num(k + 1.0)
                }
                "EXACT" -> { need(a, 2..2); bool(s0() == str(a[1])) }
                "VALUE", "NUMBERVALUE" -> {
                    need(a, 1..3)
                    var t = s0().trim()
                    t = if (f == "NUMBERVALUE" && given(a, 1)?.let { str(it) } == ",") t.replace(".", "").replace(",", ".") else t.replace(",", "")
                    val pct = t.endsWith("%")
                    val n = DataTable.number(t.removeSuffix("%").removePrefix("$")) ?: throw Err("#VALUE!")
                    Arg.Num(if (pct) n / 100 else n)
                }
                "TEXT" -> { need(a, 2..2); Arg.Str(formatText(num(a[0]), str(a[1]))) }
                "FIXED" -> {
                    need(a, 1..3)
                    val d = opt(a, 1, 2.0).toInt()
                    val x = java.math.BigDecimal(num(a[0]).toString()).setScale(d, java.math.RoundingMode.HALF_UP)
                    val pattern = (if (optB(a, 2, false)) "0" else "#,##0") + (if (d > 0) "." + "0".repeat(d) else "")
                    Arg.Str(java.text.DecimalFormat(pattern, java.text.DecimalFormatSymbols(java.util.Locale.US)).format(x))
                }
                "DOLLAR" -> {
                    need(a, 1..2)
                    val d = opt(a, 1, 2.0).toInt().coerceAtLeast(0)
                    val x = num(a[0])
                    val body = java.text.DecimalFormat("#,##0" + (if (d > 0) "." + "0".repeat(d) else ""), java.text.DecimalFormatSymbols(java.util.Locale.US)).format(abs(x))
                    Arg.Str(if (x < 0) "($$body)" else "$$body")
                }
                "CHAR", "UNICHAR" -> { need(a, 1..1); val n = int(a[0]).toInt(); if (n < 1 || n > 0x10FFFF) throw Err("#VALUE!"); Arg.Str(String(Character.toChars(n))) }
                "CODE", "UNICODE" -> { need(a, 1..1); val t = s0(); if (t.isEmpty()) throw Err("#VALUE!"); Arg.Num(t.codePointAt(0).toDouble()) }
                "TEXTBEFORE", "TEXTAFTER" -> {
                    need(a, 2..3)
                    val t = s0(); val sep = str(a[1]); val n = opt(a, 2, 1.0).toInt()
                    if (n == 0 || sep.isEmpty()) throw Err("#VALUE!")
                    var at = if (n > 0) -1 else t.length
                    repeat(abs(n)) {
                        at = if (n > 0) t.indexOf(sep, at + 1) else t.lastIndexOf(sep, at - 1)
                        if (at < 0) throw Err("#N/A")
                    }
                    Arg.Str(if (f == "TEXTBEFORE") t.substring(0, at) else t.substring(at + sep.length))
                }
                "ROMAN" -> {
                    need(a, 1..2)
                    var n = int(a[0]).toInt(); if (n < 0 || n > 3999) throw Err("#VALUE!")
                    val sb = StringBuilder()
                    for ((v, r) in listOf(1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC", 50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")) while (n >= v) { sb.append(r); n -= v }
                    Arg.Str(sb.toString())
                }
                "ARABIC" -> {
                    need(a, 1..1)
                    val t = s0().trim().uppercase()
                    val map = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
                    val d = t.removePrefix("-").map { map[it] ?: throw Err("#VALUE!") }
                    val n = d.indices.sumOf { k -> if (k + 1 < d.size && d[k] < d[k + 1]) -d[k] else d[k] }
                    Arg.Num(if (t.startsWith("-")) -n.toDouble() else n.toDouble())
                }
                "BASE" -> {
                    need(a, 2..3)
                    val n = int(a[0]); val radix = int(a[1]).toInt()
                    if (n < 0 || radix !in 2..36) throw Err("#NUM!")
                    Arg.Str(n.toString(radix).uppercase().padStart(opt(a, 2, 0.0).toInt(), '0'))
                }
                "DECIMAL" -> {
                    need(a, 2..2)
                    val radix = int(a[1]).toInt(); if (radix !in 2..36) throw Err("#NUM!")
                    Arg.Num(s0().trim().toLongOrNull(radix)?.toDouble() ?: throw Err("#NUM!"))
                }
                else -> null
            }
        }

        fun holidays(a: Arg?): Set<LocalDate> = a?.let { flat(it).mapNotNull { v -> v.number?.let { n -> dateOf(n) } }.toSet() } ?: emptySet()

        fun date(f: String, a: List<Arg>): Arg? {
            fun d(k: Int) = num(a[k]).also { if (it < 0) throw Err("#NUM!") }.let { dateOf(it) }
            fun seconds(k: Int) = num(a[k]).let { Math.round((it - floor(it)) * 86400) }
            fun thirty(s: LocalDate, e: LocalDate, european: Boolean): Double {
                var sd = s.dayOfMonth; var ed = e.dayOfMonth
                if (european) { if (sd == 31) sd = 30; if (ed == 31) ed = 30 }
                else { if (sd == 31 || (s.monthValue == 2 && sd == s.lengthOfMonth())) sd = 30; if (ed == 31 && sd >= 30) ed = 30 }
                return ((e.year - s.year) * 360 + (e.monthValue - s.monthValue) * 30 + (ed - sd)).toDouble()
            }
            return when (f) {
                "DATE" -> {
                    need(a, 3..3)
                    var y = int(a[0]); if (y in 0..1899) y += 1900
                    if (y < 1900 || y > 9999) throw Err("#NUM!")
                    Arg.Num(serialOf(LocalDate.of(y.toInt(), 1, 1).plusMonths(int(a[1]) - 1).plusDays(int(a[2]) - 1)))
                }
                "DATEVALUE" -> {
                    need(a, 1..1)
                    val t = str(a[0]).trim()
                    val dt = runCatching { LocalDate.parse(t) }.getOrNull()
                        ?: Regex("""(\d{1,2})[./](\d{1,2})[./](\d{4})""").matchEntire(t)?.destructured?.let { (dd, mm, yy) -> runCatching { LocalDate.of(yy.toInt(), mm.toInt(), dd.toInt()) }.getOrNull() }
                        ?: throw Err("#VALUE!")
                    Arg.Num(serialOf(dt))
                }
                "TODAY" -> Arg.Num(serialOf(LocalDate.now()))
                "NOW" -> { val t = java.time.LocalDateTime.now(); Arg.Num(serialOf(t.toLocalDate()) + t.toLocalTime().toSecondOfDay() / 86400.0) }
                "YEAR" -> { need(a, 1..1); Arg.Num(d(0).year.toDouble()) }
                "MONTH" -> { need(a, 1..1); Arg.Num(d(0).monthValue.toDouble()) }
                "DAY" -> { need(a, 1..1); Arg.Num(d(0).dayOfMonth.toDouble()) }
                "HOUR" -> { need(a, 1..1); Arg.Num((seconds(0) / 3600 % 24).toDouble()) }
                "MINUTE" -> { need(a, 1..1); Arg.Num((seconds(0) / 60 % 60).toDouble()) }
                "SECOND" -> { need(a, 1..1); Arg.Num((seconds(0) % 60).toDouble()) }
                "TIME" -> { need(a, 3..3); val sec = num(a[0]) * 3600 + num(a[1]) * 60 + num(a[2]); if (sec < 0) throw Err("#NUM!"); Arg.Num((sec / 86400).let { it - floor(it) }) }
                "WEEKDAY" -> {
                    need(a, 1..2)
                    val w = d(0).dayOfWeek.value // Monday 1 … Sunday 7
                    val type = opt(a, 1, 1.0).toInt()
                    Arg.Num(when (type) { 1, 17 -> w % 7 + 1; 2, 11 -> w; 3 -> w - 1; in 12..16 -> (w - (type - 10) + 7) % 7 + 1; else -> throw Err("#NUM!") }.toDouble())
                }
                "WEEKNUM" -> {
                    need(a, 1..2)
                    val dt = d(0)
                    val type = opt(a, 1, 1.0).toInt()
                    if (type == 21) Arg.Num(dt.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR).toDouble())
                    else {
                        val start = if (type == 2 || type == 11) DayOfWeek.MONDAY.value else DayOfWeek.SUNDAY.value
                        val offset = (dt.withDayOfYear(1).dayOfWeek.value - start + 7) % 7
                        Arg.Num(((dt.dayOfYear - 1 + offset) / 7 + 1).toDouble())
                    }
                }
                "ISOWEEKNUM" -> { need(a, 1..1); Arg.Num(d(0).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR).toDouble()) }
                "DAYS" -> { need(a, 2..2); Arg.Num(floor(num(a[0])) - floor(num(a[1]))) }
                "DAYS360" -> { need(a, 2..3); Arg.Num(thirty(d(0), d(1), optB(a, 2, false))) }
                "EDATE" -> { need(a, 2..2); Arg.Num(serialOf(d(0).plusMonths(int(a[1])))) }
                "EOMONTH" -> { need(a, 2..2); val dt = d(0).plusMonths(int(a[1])); Arg.Num(serialOf(dt.withDayOfMonth(dt.lengthOfMonth()))) }
                "DATEDIF" -> {
                    need(a, 3..3)
                    val s = d(0); val e = d(1)
                    if (e < s) throw Err("#NUM!")
                    Arg.Num(when (str(a[2]).uppercase()) {
                        "Y" -> ChronoUnit.YEARS.between(s, e)
                        "M" -> ChronoUnit.MONTHS.between(s, e)
                        "D" -> ChronoUnit.DAYS.between(s, e)
                        "YM" -> ChronoUnit.MONTHS.between(s, e) % 12
                        "MD" -> ChronoUnit.DAYS.between(s.plusMonths(ChronoUnit.MONTHS.between(s, e)), e)
                        "YD" -> ChronoUnit.DAYS.between(s.plusYears(ChronoUnit.YEARS.between(s, e)), e)
                        else -> throw Err("#NUM!")
                    }.toDouble())
                }
                "NETWORKDAYS" -> {
                    need(a, 2..3)
                    val holidays = holidays(a.getOrNull(2))
                    var s = d(0); var e = d(1)
                    val sign = if (e < s) { val t = s; s = e; e = t; -1 } else 1
                    var n = 0
                    var x = s
                    while (!x.isAfter(e)) { if (x.dayOfWeek.value <= 5 && x !in holidays) n++; x = x.plusDays(1) }
                    Arg.Num(sign * n.toDouble())
                }
                "WORKDAY" -> {
                    need(a, 2..3)
                    val holidays = holidays(a.getOrNull(2))
                    var x = d(0); var left = int(a[1])
                    val step = if (left < 0) -1L else 1L
                    while (left != 0L) { x = x.plusDays(step); if (x.dayOfWeek.value <= 5 && x !in holidays) left -= step }
                    Arg.Num(serialOf(x))
                }
                "YEARFRAC" -> {
                    need(a, 2..3)
                    var s = d(0); var e = d(1); if (e < s) { val t = s; s = e; e = t }
                    val days = ChronoUnit.DAYS.between(s, e).toDouble()
                    Arg.Num(when (opt(a, 2, 0.0).toInt()) {
                        0 -> thirty(s, e, false) / 360
                        4 -> thirty(s, e, true) / 360
                        1 -> days / (s.year..e.year).map { if (java.time.Year.isLeap(it.toLong())) 366.0 else 365.0 }.average()
                        2 -> days / 360
                        3 -> days / 365
                        else -> throw Err("#NUM!")
                    })
                }
                else -> null
            }
        }

        /** A number as TEXT shows it: "0.00", "#,##0", "0%", "0.00E+00", or a date like "yyyy-mm-dd". */
        fun formatText(x: Double, format: String): String {
            val bare = format.replace(Regex("\"[^\"]*\""), "")
            if (Regex("""[yYdDhHsS]|[mM]""").containsMatchIn(bare) && !bare.contains('0') && !bare.contains('#')) {
                val dt = dateOf(x); val secs = Math.round((x - floor(x)) * 86400)
                val h = (secs / 3600 % 24).toInt(); val mi = (secs / 60 % 60).toInt(); val sec = (secs % 60).toInt()
                val ampm = format.contains("AM/PM", ignoreCase = true)
                val tokens = Regex("""yyyy|yy|mmmm|mmm|mm|m|dddd|ddd|dd|d|hh|h|ss|s|AM/PM|"[^"]*"|.""", RegexOption.IGNORE_CASE).findAll(format).map { it.value }.toList()
                fun kind(t: String) = t.lowercase().firstOrNull()?.takeIf { it in "ymdhs" && t[0].isLetter() && !t.equals("AM/PM", true) }
                val sb = StringBuilder()
                tokens.forEachIndexed { k, t ->
                    // m after an hour or before seconds is minutes.
                    val minutes = tokens.subList(0, k).lastOrNull { kind(it) != null }?.let { kind(it) == 'h' } == true ||
                        tokens.drop(k + 1).firstOrNull { kind(it) != null }?.let { kind(it) == 's' } == true
                    val hh = if (ampm) (if (h % 12 == 0) 12 else h % 12) else h
                    sb.append(when (t.lowercase()) {
                        "yyyy" -> dt.year.toString(); "yy" -> (dt.year % 100).toString().padStart(2, '0')
                        "mmmm" -> dt.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.US)
                        "mmm" -> dt.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.US)
                        "mm" -> if (minutes) mi.toString().padStart(2, '0') else dt.monthValue.toString().padStart(2, '0')
                        "m" -> if (minutes) mi.toString() else dt.monthValue.toString()
                        "dddd" -> dt.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.US)
                        "ddd" -> dt.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.US)
                        "dd" -> dt.dayOfMonth.toString().padStart(2, '0'); "d" -> dt.dayOfMonth.toString()
                        "hh" -> hh.toString().padStart(2, '0'); "h" -> hh.toString()
                        "ss" -> sec.toString().padStart(2, '0'); "s" -> sec.toString()
                        "am/pm" -> if (h < 12) "AM" else "PM"
                        else -> if (t.startsWith("\"")) t.trim('"') else t
                    })
                }
                return sb.toString()
            }
            val sections = format.split(';')
            val f = if (x < 0 && sections.size > 1) sections[1] else sections[0]
            val v = if (x < 0 && sections.size > 1) -x else x
            val pattern = f.replace(Regex("\"([^\"]*)\"")) { "'" + it.groupValues[1] + "'" }.replace("E+", "E").replace("e+", "E").replace("e-", "E-")
            val out = try { java.text.DecimalFormat(pattern, java.text.DecimalFormatSymbols(java.util.Locale.US)).format(v) } catch (e: IllegalArgumentException) { throw Err("#VALUE!") }
            return if (f.contains("E+", ignoreCase = true)) out.replace(Regex("E(\\d)"), "E+$1") else out
        }

        fun math(f: String, a: List<Arg>): Double? {
            fun one(): Double { need(a, 1..1); return num(a[0]) }
            fun two(): Pair<Double, Double> { need(a, 2..2); return num(a[0]) to num(a[1]) }
            fun checked(x: Double) = if (x.isNaN()) throw Err("#NUM!") else x
            return when (f) {
                "SUM" -> numbers(a).sum()
                "PRODUCT" -> numbers(a).fold(1.0) { p, v -> p * v }
                "SUMSQ" -> numbers(a).sumOf { it * it }
                "SUMPRODUCT" -> {
                    val cols = a.map { x -> flat(x).map { v -> v.error?.let { throw Err(it) }; v.number ?: 0.0 } }
                    if (cols.isEmpty() || cols.map { it.size }.distinct().size != 1) throw Err("#VALUE!")
                    cols[0].indices.sumOf { k -> cols.fold(1.0) { p, c -> p * c[k] } }
                }
                "SUMX2MY2", "SUMX2PY2", "SUMXMY2" -> {
                    need(a, 2..2)
                    val (x, y) = paired(a[0], a[1])
                    x.indices.sumOf { k -> when (f) { "SUMX2MY2" -> x[k] * x[k] - y[k] * y[k]; "SUMX2PY2" -> x[k] * x[k] + y[k] * y[k]; else -> (x[k] - y[k]).pow(2) } }
                }
                "SUMIF" -> {
                    need(a, 2..3)
                    val r = rangeOf(a[0]); val test = criterion(a[1])
                    val cells = values(r)
                    val sums = if (a.size == 3) values(resized(rangeOf(a[2]), r)) else cells
                    cells.indices.filter { test(cells[it]) }.sumOf { sums.getOrNull(it)?.number ?: 0.0 }
                }
                "SUMIFS" -> {
                    if (a.size < 3) throw Err("#VALUE!")
                    val v = values(resized(rangeOf(a[0]), rangeOf(a[1])))
                    hits(a.drop(1)).sumOf { v.getOrNull(it)?.number ?: 0.0 }
                }
                "ABS" -> abs(one())
                "SQRT" -> one().let { if (it < 0) throw Err("#NUM!") else sqrt(it) }
                "SQRTPI" -> one().let { if (it < 0) throw Err("#NUM!") else sqrt(it * Math.PI) }
                "EXP" -> exp(one())
                "LN" -> one().let { if (it <= 0) throw Err("#NUM!") else ln(it) }
                "LOG10" -> one().let { if (it <= 0) throw Err("#NUM!") else kotlin.math.log10(it) }
                "LOG" -> { need(a, 1..2); val x = num(a[0]); val b = opt(a, 1, 10.0); if (x <= 0 || b <= 0) throw Err("#NUM!"); if (b == 1.0) throw Err("#DIV/0!"); ln(x) / ln(b) }
                "POWER" -> { val (x, y) = two(); if (x == 0.0 && y < 0) throw Err("#DIV/0!"); checked(x.pow(y)) }
                "MOD" -> { val (x, d) = two(); if (d == 0.0) throw Err("#DIV/0!"); x - d * floor(x / d) }
                "QUOTIENT" -> { val (x, d) = two(); if (d == 0.0) throw Err("#DIV/0!"); kotlin.math.truncate(x / d) }
                "INT" -> floor(one())
                "TRUNC" -> { need(a, 1..2); val p = 10.0.pow(opt(a, 1, 0.0).toInt()); kotlin.math.truncate(num(a[0]) * p) / p }
                "SIGN" -> kotlin.math.sign(one())
                "ROUND", "ROUNDUP", "ROUNDDOWN" -> {
                    need(a, 1..2)
                    val x = num(a[0]); val d = opt(a, 1, 0.0).toInt()
                    val mode = when (f) { "ROUNDUP" -> java.math.RoundingMode.UP; "ROUNDDOWN" -> java.math.RoundingMode.DOWN; else -> java.math.RoundingMode.HALF_UP }
                    java.math.BigDecimal(x.toString()).setScale(d, mode).toDouble()
                }
                "MROUND" -> {
                    val (x, m) = two()
                    if (m == 0.0) 0.0 else if (x * m < 0) throw Err("#NUM!") else java.math.BigDecimal((x / m).toString()).setScale(0, java.math.RoundingMode.HALF_UP).toDouble() * m
                }
                "EVEN", "ODD" -> {
                    val x = one(); val s = if (x < 0) -1 else 1
                    var n = kotlin.math.ceil(abs(x))
                    if (f == "EVEN" && n % 2 != 0.0) n++
                    if (f == "ODD" && n % 2 == 0.0) n++
                    s * n
                }
                "FLOOR", "CEILING" -> {
                    need(a, 1..2)
                    val x = num(a[0]); val m = opt(a, 1, if (x < 0) -1.0 else 1.0)
                    if (m == 0.0) 0.0 else if (x > 0 && m < 0) throw Err("#NUM!") else if (f == "FLOOR") floor(x / m) * m else kotlin.math.ceil(x / m) * m
                }
                "CEILING.MATH", "FLOOR.MATH" -> {
                    need(a, 1..3)
                    val x = num(a[0]); val m = abs(opt(a, 1, 1.0)); val away = opt(a, 2, 0.0) != 0.0 && x < 0
                    if (m == 0.0) 0.0
                    else if (f == "CEILING.MATH") (if (away) floor(x / m) else kotlin.math.ceil(x / m)) * m
                    else (if (away) kotlin.math.ceil(x / m) else floor(x / m)) * m
                }
                "CEILING.PRECISE", "ISO.CEILING" -> { need(a, 1..2); val m = abs(opt(a, 1, 1.0)); if (m == 0.0) 0.0 else kotlin.math.ceil(num(a[0]) / m) * m }
                "FLOOR.PRECISE" -> { need(a, 1..2); val m = abs(opt(a, 1, 1.0)); if (m == 0.0) 0.0 else floor(num(a[0]) / m) * m }
                "GCD", "LCM" -> {
                    val v = numbers(a).map { if (it < 0) throw Err("#NUM!"); floor(it).toLong() }
                    fun gcd(x: Long, y: Long): Long = if (y == 0L) x else gcd(y, x % y)
                    if (f == "GCD") v.fold(0L) { g, x -> gcd(g, x) }.toDouble()
                    else if (v.any { it == 0L }) 0.0 else v.fold(1L) { l, x -> l / gcd(l, x) * x }.toDouble()
                }
                "PI" -> { need(a, 0..0); Math.PI }
                "RAND" -> { need(a, 0..0); Math.random() }
                "RANDBETWEEN" -> { val (lo, hi) = two(); val l = kotlin.math.ceil(lo); val h = floor(hi); if (h < l) throw Err("#NUM!"); l + floor(Math.random() * (h - l + 1)) }
                "SIN" -> kotlin.math.sin(one())
                "COS" -> kotlin.math.cos(one())
                "TAN" -> kotlin.math.tan(one())
                "COT" -> one().let { if (it == 0.0) throw Err("#DIV/0!"); 1 / kotlin.math.tan(it) }
                "CSC" -> one().let { if (it == 0.0) throw Err("#DIV/0!"); 1 / kotlin.math.sin(it) }
                "SEC" -> 1 / kotlin.math.cos(one())
                "ASIN" -> checked(kotlin.math.asin(one()))
                "ACOS" -> checked(kotlin.math.acos(one()))
                "ATAN" -> kotlin.math.atan(one())
                "ACOT" -> Math.PI / 2 - kotlin.math.atan(one())
                "ATAN2" -> { val (x, y) = two(); if (x == 0.0 && y == 0.0) throw Err("#DIV/0!"); kotlin.math.atan2(y, x) }
                "SINH" -> kotlin.math.sinh(one())
                "COSH" -> kotlin.math.cosh(one())
                "TANH" -> kotlin.math.tanh(one())
                "COTH" -> one().let { if (it == 0.0) throw Err("#DIV/0!"); 1 / kotlin.math.tanh(it) }
                "CSCH" -> one().let { if (it == 0.0) throw Err("#DIV/0!"); 1 / kotlin.math.sinh(it) }
                "SECH" -> 1 / kotlin.math.cosh(one())
                "ASINH" -> kotlin.math.asinh(one())
                "ACOSH" -> checked(kotlin.math.acosh(one()))
                "ATANH" -> one().let { if (abs(it) >= 1) throw Err("#NUM!"); kotlin.math.atanh(it) }
                "ACOTH" -> one().let { if (abs(it) <= 1) throw Err("#NUM!"); 0.5 * ln((it + 1) / (it - 1)) }
                "DEGREES" -> Math.toDegrees(one())
                "RADIANS" -> Math.toRadians(one())
                "FACT" -> one().let { n -> if (n < 0 || n > 170) throw Err("#NUM!"); (1..floor(n).toInt()).fold(1.0) { p, k -> p * k } }
                "FACTDOUBLE" -> one().let { n -> if (n < -1) throw Err("#NUM!"); var p = 1.0; var k = floor(n); while (k > 1) { p *= k; k -= 2 }; p }
                "COMBIN" -> { val (n0, k0) = two(); val n = floor(n0); val k = floor(k0); if (k < 0 || n < 0 || k > n) throw Err("#NUM!"); exp(lnChoose(n, k)).let { if (it < 1e15) Math.round(it).toDouble() else it } }
                "COMBINA" -> { val (n0, k0) = two(); val n = floor(n0); val k = floor(k0); if (k < 0 || n < 0) throw Err("#NUM!"); if (k == 0.0) 1.0 else exp(lnChoose(n + k - 1, k)).let { if (it < 1e15) Math.round(it).toDouble() else it } }
                "PERMUT" -> { val (n0, k0) = two(); val n = floor(n0); val k = floor(k0); if (k < 0 || n < 0 || k > n) throw Err("#NUM!"); (0 until k.toInt()).fold(1.0) { p, j -> p * (n - j) } }
                "PERMUTATIONA" -> { val (n, k) = two(); if (n < 0 || k < 0) throw Err("#NUM!"); floor(n).pow(floor(k)) }
                "MULTINOMIAL" -> { val v = numbers(a).map { if (it < 0) throw Err("#NUM!"); floor(it) }; exp(lnGamma(v.sum() + 1) - v.sumOf { lnGamma(it + 1) }).let { if (it < 1e15) Math.round(it).toDouble() else it } }
                "SERIESSUM" -> {
                    need(a, 4..4)
                    val x = num(a[0]); val n = num(a[1]); val m = num(a[2])
                    numbers(listOf(a[3])).withIndex().sumOf { (k, c) -> c * x.pow(n + k * m) }
                }
                "MDETERM" -> {
                    need(a, 1..1)
                    val r = rangeOf(a[0]); val n = width(r)
                    if (r.r1 - r.r0 + 1 != n) throw Err("#VALUE!")
                    val m = Array(n) { i -> DoubleArray(n) { j -> book.value(r.c0 + j, r.r0 + i).number ?: throw Err("#VALUE!") } }
                    var det = 1.0
                    for (c in 0 until n) {
                        val p = (c until n).maxByOrNull { abs(m[it][c]) }!!
                        if (m[p][c] == 0.0) return 0.0
                        if (p != c) { val t = m[p]; m[p] = m[c]; m[c] = t; det = -det }
                        det *= m[c][c]
                        for (rr in c + 1 until n) { val k = m[rr][c] / m[c][c]; for (cc in c until n) m[rr][cc] -= k * m[c][cc] }
                    }
                    det
                }
                "SUBTOTAL" -> {
                    if (a.size < 2) throw Err("#VALUE!")
                    val name = listOf("AVERAGE", "COUNT", "COUNTA", "MAX", "MIN", "PRODUCT", "STDEV", "STDEVP", "SUM", "VAR", "VARP").getOrNull(int(a[0]).toInt() % 100 - 1) ?: throw Err("#VALUE!")
                    num(call(name, a.drop(1)))
                }
                "AGGREGATE" -> {
                    if (a.size < 3) throw Err("#VALUE!")
                    val skipErrors = int(a[1]).toInt() in listOf(2, 3, 6, 7)
                    aggregate(int(a[0]).toInt(), numbers(listOf(a[2]), ignoreErrors = skipErrors), given(a, 3)?.let { num(it) })
                }
                else -> null
            }
        }

        // ---------- statistics on a list ----------

        fun mean(v: List<Double>) = if (v.isEmpty()) throw Err("#DIV/0!") else v.average()
        fun variance(v: List<Double>, sample: Boolean): Double {
            if (v.size < (if (sample) 2 else 1)) throw Err("#DIV/0!")
            val m = v.average()
            return v.sumOf { (it - m) * (it - m) } / (if (sample) v.size - 1 else v.size)
        }
        fun median(v: List<Double>) = v.sorted().let { s -> if (s.isEmpty()) throw Err("#NUM!"); if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2 }
        fun mode(v: List<Double>): Double {
            val counts = LinkedHashMap<Double, Int>()
            v.forEach { counts[it] = (counts[it] ?: 0) + 1 }
            val most = counts.values.maxOrNull() ?: throw Err("#N/A")
            if (most < 2) throw Err("#N/A")
            return counts.entries.first { it.value == most }.key
        }
        fun percentile(v: List<Double>, k: Double, exclusive: Boolean): Double {
            val s = v.sorted(); val n = s.size
            if (n == 0) throw Err("#NUM!")
            val h = if (exclusive) (n + 1) * k - 1 else (n - 1) * k
            if (k < 0 || k > 1 || h < -1e-12 || h > n - 1 + 1e-12) throw Err("#NUM!")
            val lo = floor(h + 1e-12).toInt().coerceIn(0, n - 1); val frac = (h - lo).coerceAtLeast(0.0)
            return if (lo + 1 < n) s[lo] + frac * (s[lo + 1] - s[lo]) else s[lo]
        }
        fun kth(v: List<Double>, k: Double, largest: Boolean): Double {
            val s = v.sorted().let { if (largest) it.reversed() else it }
            return s.getOrNull(kotlin.math.ceil(k).toInt() - 1)?.takeIf { k >= 1 } ?: throw Err("#NUM!")
        }

        fun aggregate(fn: Int, v: List<Double>, k: Double?): Double {
            fun kk() = k ?: throw Err("#VALUE!")
            return when (fn) {
                1 -> mean(v); 2, 3 -> v.size.toDouble(); 4 -> v.maxOrNull() ?: 0.0; 5 -> v.minOrNull() ?: 0.0
                6 -> v.fold(1.0) { p, x -> p * x }; 7 -> sqrt(variance(v, true)); 8 -> sqrt(variance(v, false)); 9 -> v.sum()
                10 -> variance(v, true); 11 -> variance(v, false); 12 -> median(v); 13 -> mode(v)
                14 -> kth(v, kk(), true); 15 -> kth(v, kk(), false)
                16 -> percentile(v, kk(), false); 17 -> percentile(v, kk() / 4, false)
                18 -> percentile(v, kk(), true); 19 -> percentile(v, kk() / 4, true)
                else -> throw Err("#VALUE!")
            }
        }

        /** Least squares through (x, y): slope, intercept, Sxx, Sxy, Syy, the means and the count. */
        fun line(ys: Arg, xs: Arg): DoubleArray {
            val (y, x) = paired(ys, xs)
            if (x.size < 2) throw Err("#DIV/0!")
            val mx = x.average(); val my = y.average()
            val sxy = x.indices.sumOf { (x[it] - mx) * (y[it] - my) }
            val sxx = x.sumOf { (it - mx) * (it - mx) }; val syy = y.sumOf { (it - my) * (it - my) }
            if (sxx == 0.0) throw Err("#DIV/0!")
            return doubleArrayOf(sxy / sxx, my - sxy / sxx * mx, sxx, sxy, syy, mx, my, x.size.toDouble())
        }

        fun tInv(p: Double, df: Double) = invert(p, -1.0, 1.0) { tCdf(it, df) }

        fun stats(f: String, a: List<Arg>): Double? = when (f) {
            "AVERAGE", "MEAN" -> mean(numbers(a))
            "AVERAGEA" -> mean(numbersA(a))
            "MIN" -> numbers(a).minOrNull() ?: 0.0
            "MAX" -> numbers(a).maxOrNull() ?: 0.0
            "MINA" -> numbersA(a).minOrNull() ?: 0.0
            "MAXA" -> numbersA(a).maxOrNull() ?: 0.0
            "COUNT" -> a.sumOf { x -> if (x is Arg.Range) values(x).count { it.number != null } else if (x is Arg.Num || (x is Arg.Str && DataTable.number(x.s) != null)) 1 else 0 }.toDouble()
            "COUNTA" -> a.sumOf { x -> if (x is Arg.Range) values(x).count { !it.isBlank } else 1 }.toDouble()
            "COUNTBLANK" -> a.sumOf { x -> values(rangeOf(x)).count { it.isBlank } }.toDouble()
            "COUNTIF" -> { need(a, 2..2); values(rangeOf(a[0])).count(criterion(a[1])).toDouble() }
            "COUNTIFS" -> hits(a).size.toDouble()
            "AVERAGEIF" -> {
                need(a, 2..3)
                val r = rangeOf(a[0]); val test = criterion(a[1]); val cells = values(r)
                val sums = if (a.size == 3) values(resized(rangeOf(a[2]), r)) else cells
                mean(cells.indices.filter { test(cells[it]) }.mapNotNull { sums.getOrNull(it)?.number })
            }
            "AVERAGEIFS", "MAXIFS", "MINIFS" -> {
                if (a.size < 3) throw Err("#VALUE!")
                val v = values(resized(rangeOf(a[0]), rangeOf(a[1])))
                val picked = hits(a.drop(1)).mapNotNull { v.getOrNull(it)?.number }
                when (f) { "AVERAGEIFS" -> mean(picked); "MAXIFS" -> picked.maxOrNull() ?: 0.0; else -> picked.minOrNull() ?: 0.0 }
            }
            "MEDIAN" -> median(numbers(a))
            "MODE", "MODE.SNGL" -> mode(numbers(a))
            "GEOMEAN" -> numbers(a).let { v -> if (v.isEmpty() || v.any { it <= 0 }) throw Err("#NUM!"); exp(v.sumOf { ln(it) } / v.size) }
            "HARMEAN" -> numbers(a).let { v -> if (v.isEmpty() || v.any { it <= 0 }) throw Err("#NUM!"); v.size / v.sumOf { 1 / it } }
            "AVEDEV" -> numbers(a).let { v -> val m = mean(v); v.sumOf { abs(it - m) } / v.size }
            "DEVSQ" -> numbers(a).let { v -> val m = mean(v); v.sumOf { (it - m) * (it - m) } }
            "STDEV", "STDEV.S" -> sqrt(variance(numbers(a), true))
            "STDEV.P", "STDEVP" -> sqrt(variance(numbers(a), false))
            "VAR", "VAR.S" -> variance(numbers(a), true)
            "VAR.P", "VARP" -> variance(numbers(a), false)
            "STDEVA" -> sqrt(variance(numbersA(a), true))
            "STDEVPA" -> sqrt(variance(numbersA(a), false))
            "VARA" -> variance(numbersA(a), true)
            "VARPA" -> variance(numbersA(a), false)
            "SKEW" -> {
                val v = numbers(a); val n = v.size.toDouble()
                if (n < 3) throw Err("#DIV/0!")
                val m = v.average(); val sd = sqrt(variance(v, true)); if (sd == 0.0) throw Err("#DIV/0!")
                n / ((n - 1) * (n - 2)) * v.sumOf { ((it - m) / sd).pow(3) }
            }
            "SKEW.P" -> {
                val v = numbers(a); val m = mean(v); val sd = sqrt(variance(v, false)); if (sd == 0.0) throw Err("#DIV/0!")
                v.sumOf { ((it - m) / sd).pow(3) } / v.size
            }
            "KURT" -> {
                val v = numbers(a); val n = v.size.toDouble()
                if (n < 4) throw Err("#DIV/0!")
                val m = v.average(); val sd = sqrt(variance(v, true)); if (sd == 0.0) throw Err("#DIV/0!")
                n * (n + 1) / ((n - 1) * (n - 2) * (n - 3)) * v.sumOf { ((it - m) / sd).pow(4) } - 3 * (n - 1) * (n - 1) / ((n - 2) * (n - 3))
            }
            "LARGE", "SMALL" -> { need(a, 2..2); kth(numbers(listOf(a[0])), num(a[1]), f == "LARGE") }
            "PERCENTILE", "PERCENTILE.INC", "PERCENTILE.EXC" -> { need(a, 2..2); percentile(numbers(listOf(a[0])), num(a[1]), f.endsWith("EXC")) }
            "QUARTILE", "QUARTILE.INC", "QUARTILE.EXC" -> {
                need(a, 2..2)
                val q = floor(num(a[1])); if (q < 0 || q > 4) throw Err("#NUM!")
                percentile(numbers(listOf(a[0])), q / 4, f.endsWith("EXC"))
            }
            "RANK", "RANK.EQ", "RANK.AVG" -> {
                need(a, 2..3)
                val x = num(a[0]); val v = numbers(listOf(a[1])); val asc = opt(a, 2, 0.0) != 0.0
                if (x !in v) throw Err("#N/A")
                val before = v.count { if (asc) it < x else it > x }; val ties = v.count { it == x }
                if (f == "RANK.AVG") before + (ties + 1) / 2.0 else before + 1.0
            }
            "PERCENTRANK", "PERCENTRANK.INC", "PERCENTRANK.EXC" -> {
                need(a, 2..3)
                val s = numbers(listOf(a[0])).sorted(); val x = num(a[1]); val n = s.size
                if (n == 0 || x < s.first() || x > s.last()) throw Err("#N/A")
                val digits = opt(a, 2, 3.0).toInt(); if (digits < 1) throw Err("#NUM!")
                val k = s.indexOfLast { it <= x }
                val pos = if (s[k] == x) s.indexOfFirst { it == x }.toDouble() else k + (x - s[k]) / (s[k + 1] - s[k])
                val r = if (f.endsWith("EXC")) (pos + 1) / (n + 1) else if (n == 1) 1.0 else pos / (n - 1)
                val p = 10.0.pow(digits); floor(r * p + 1e-9) / p
            }
            "TRIMMEAN" -> {
                need(a, 2..2)
                val s = numbers(listOf(a[0])).sorted(); val pct = num(a[1])
                if (pct < 0 || pct >= 1) throw Err("#NUM!")
                val cut = floor(s.size * pct / 2).toInt()
                mean(s.subList(cut, s.size - cut))
            }
            "STANDARDIZE" -> { need(a, 3..3); val sd = num(a[2]); if (sd <= 0) throw Err("#NUM!"); (num(a[0]) - num(a[1])) / sd }
            "SLOPE" -> { need(a, 2..2); line(a[0], a[1])[0] }
            "INTERCEPT" -> { need(a, 2..2); line(a[0], a[1])[1] }
            "RSQ" -> { need(a, 2..2); val l = line(a[0], a[1]); if (l[4] == 0.0) throw Err("#DIV/0!"); l[3] * l[3] / (l[2] * l[4]) }
            "CORREL", "PEARSON" -> { need(a, 2..2); val l = line(a[0], a[1]); if (l[4] == 0.0) throw Err("#DIV/0!"); l[3] / sqrt(l[2] * l[4]) }
            "STEYX" -> { need(a, 2..2); val l = line(a[0], a[1]); if (l[7] < 3) throw Err("#DIV/0!"); sqrt((l[4] - l[3] * l[3] / l[2]) / (l[7] - 2)) }
            "FORECAST", "FORECAST.LINEAR" -> { need(a, 3..3); val l = line(a[1], a[2]); l[1] + l[0] * num(a[0]) }
            "TREND" -> { need(a, 3..3); val l = line(a[0], a[1]); l[1] + l[0] * num(a[2]) }
            "GROWTH" -> {
                need(a, 3..3)
                val (y, x) = paired(a[0], a[1])
                if (y.any { it <= 0 }) throw Err("#NUM!")
                if (x.size < 2) throw Err("#DIV/0!")
                val mx = x.average(); val ly = y.map { ln(it) }; val my = ly.average()
                val sxx = x.sumOf { (it - mx) * (it - mx) }; if (sxx == 0.0) throw Err("#DIV/0!")
                val b = x.indices.sumOf { (x[it] - mx) * (ly[it] - my) } / sxx
                exp(my - b * mx + b * num(a[2]))
            }
            "COVAR", "COVARIANCE.P", "COVARIANCE.S" -> {
                need(a, 2..2)
                val (x, y) = paired(a[0], a[1]); val n = x.size
                val sample = f == "COVARIANCE.S"
                if (n < (if (sample) 2 else 1)) throw Err("#DIV/0!")
                val mx = x.average(); val my = y.average()
                x.indices.sumOf { (x[it] - mx) * (y[it] - my) } / (if (sample) n - 1 else n)
            }
            "FISHER" -> { need(a, 1..1); val x = num(a[0]); if (abs(x) >= 1) throw Err("#NUM!"); 0.5 * ln((1 + x) / (1 - x)) }
            "FISHERINV" -> { need(a, 1..1); kotlin.math.tanh(num(a[0])) }
            "CONFIDENCE", "CONFIDENCE.NORM", "CONFIDENCE.T" -> {
                need(a, 3..3)
                val alpha = num(a[0]); val sd = num(a[1]); val n = floor(num(a[2]))
                if (alpha <= 0 || alpha >= 1 || sd <= 0 || n < 1) throw Err("#NUM!")
                if (f == "CONFIDENCE.T") { if (n < 2) throw Err("#DIV/0!"); tInv(1 - alpha / 2, n - 1) * sd / sqrt(n) } else normInv(1 - alpha / 2) * sd / sqrt(n)
            }
            "Z.TEST", "ZTEST" -> {
                need(a, 2..3)
                val v = numbers(listOf(a[0])); val m = mean(v)
                val sd = given(a, 2)?.let { num(it) } ?: sqrt(variance(v, true))
                1 - normCdf((m - num(a[1])) / (sd / sqrt(v.size.toDouble())))
            }
            "T.TEST", "TTEST" -> {
                need(a, 4..4)
                val tails = int(a[2]).toInt(); val type = int(a[3]).toInt()
                if (tails !in 1..2 || type !in 1..3) throw Err("#NUM!")
                val (t, df) = if (type == 1) {
                    val (x, y) = paired(a[0], a[1]); val d = x.indices.map { x[it] - y[it] }
                    if (d.size < 2) throw Err("#DIV/0!")
                    mean(d) / sqrt(variance(d, true) / d.size) to d.size - 1.0
                } else {
                    val x = numbers(listOf(a[0])); val y = numbers(listOf(a[1]))
                    val n1 = x.size.toDouble(); val n2 = y.size.toDouble()
                    val v1 = variance(x, true); val v2 = variance(y, true)
                    if (type == 2) {
                        val sp = ((n1 - 1) * v1 + (n2 - 1) * v2) / (n1 + n2 - 2)
                        (mean(x) - mean(y)) / sqrt(sp * (1 / n1 + 1 / n2)) to n1 + n2 - 2
                    } else {
                        val se = v1 / n1 + v2 / n2
                        (mean(x) - mean(y)) / sqrt(se) to se * se / ((v1 / n1).pow(2) / (n1 - 1) + (v2 / n2).pow(2) / (n2 - 1))
                    }
                }
                tails * (1 - tCdf(abs(t), df))
            }
            "F.TEST", "FTEST" -> {
                need(a, 2..2)
                val x = numbers(listOf(a[0])); val y = numbers(listOf(a[1]))
                val v1 = variance(x, true); val v2 = variance(y, true)
                if (v2 == 0.0) throw Err("#DIV/0!")
                val p = fCdf(v1 / v2, x.size - 1.0, y.size - 1.0)
                2 * minOf(p, 1 - p)
            }
            "CHISQ.TEST", "CHITEST" -> {
                need(a, 2..2)
                val r = rangeOf(a[0])
                val (obs, ex) = paired(a[0], a[1])
                if (ex.any { it == 0.0 }) throw Err("#DIV/0!")
                val chi = obs.indices.sumOf { (obs[it] - ex[it]).pow(2) / ex[it] }
                val h = height(r); val w = width(r)
                val df = if (h > 1 && w > 1) (h - 1.0) * (w - 1) else obs.size - 1.0
                1 - chi2Cdf(chi, df)
            }
            else -> null
        }

        fun binom(k: Double, n: Double, p: Double): Double = when {
            p == 0.0 -> if (k == 0.0) 1.0 else 0.0
            p == 1.0 -> if (k == n) 1.0 else 0.0
            else -> exp(lnChoose(n, k) + k * ln(p) + (n - k) * ln(1 - p))
        }

        fun distributions(f: String, a: List<Arg>): Double? {
            fun cum(k: Int) = truth(a.getOrElse(k) { throw Err("#VALUE!") })
            fun prob(x: Double) = x.also { if (it <= 0 || it >= 1) throw Err("#NUM!") }
            fun pos(x: Double) = x.also { if (it <= 0) throw Err("#NUM!") }
            return when (f) {
                "NORM.DIST", "NORMDIST" -> { need(a, 4..4); val sd = pos(num(a[2])); val z = (num(a[0]) - num(a[1])) / sd; if (cum(3)) normCdf(z) else normPdf(z) / sd }
                "NORM.S.DIST" -> { need(a, 2..2); val z = num(a[0]); if (cum(1)) normCdf(z) else normPdf(z) }
                "NORMSDIST" -> { need(a, 1..1); normCdf(num(a[0])) }
                "NORM.INV", "NORMINV" -> { need(a, 3..3); num(a[1]) + pos(num(a[2])) * normInv(prob(num(a[0]))) }
                "NORM.S.INV", "NORMSINV" -> { need(a, 1..1); normInv(prob(num(a[0]))) }
                "PHI" -> { need(a, 1..1); normPdf(num(a[0])) }
                "GAUSS" -> { need(a, 1..1); normCdf(num(a[0])) - 0.5 }
                "LOGNORM.DIST", "LOGNORMDIST" -> {
                    need(a, 3..4)
                    val x = pos(num(a[0])); val sd = pos(num(a[2]))
                    val z = (ln(x) - num(a[1])) / sd
                    if (f == "LOGNORMDIST" || cum(3)) normCdf(z) else normPdf(z) / (x * sd)
                }
                "LOGNORM.INV", "LOGINV" -> { need(a, 3..3); exp(num(a[1]) + pos(num(a[2])) * normInv(prob(num(a[0])))) }
                "T.DIST" -> { need(a, 3..3); val df = num(a[1]); if (df < 1) throw Err("#NUM!"); if (cum(2)) tCdf(num(a[0]), df) else SheetMath.tPdf(num(a[0]), df) }
                "T.DIST.2T" -> { need(a, 2..2); val x = num(a[0]); if (x < 0) throw Err("#NUM!"); 2 * (1 - tCdf(x, num(a[1]))) }
                "T.DIST.RT" -> { need(a, 2..2); 1 - tCdf(num(a[0]), num(a[1])) }
                "TDIST" -> { need(a, 3..3); val x = num(a[0]); if (x < 0) throw Err("#NUM!"); int(a[2]) * (1 - tCdf(x, num(a[1]))) }
                "T.INV" -> { need(a, 2..2); tInv(prob(num(a[0])), num(a[1])) }
                "T.INV.2T", "TINV" -> { need(a, 2..2); abs(tInv(prob(num(a[0])) / 2, num(a[1]))) }
                "CHISQ.DIST" -> { need(a, 3..3); val x = num(a[0]); val k = num(a[1]); if (x < 0) throw Err("#NUM!"); if (cum(2)) chi2Cdf(x, k) else SheetMath.chi2Pdf(x, k) }
                "CHISQ.DIST.RT", "CHIDIST" -> { need(a, 2..2); val x = num(a[0]); if (x < 0) throw Err("#NUM!"); 1 - chi2Cdf(x, num(a[1])) }
                "CHISQ.INV" -> { need(a, 2..2); val p = num(a[0]); if (p < 0 || p >= 1) throw Err("#NUM!"); val k = num(a[1]); invert(p, 0.0, maxOf(k, 1.0)) { chi2Cdf(it, k) } }
                "CHISQ.INV.RT", "CHIINV" -> { need(a, 2..2); val p = prob(num(a[0])); val k = num(a[1]); invert(1 - p, 0.0, maxOf(k, 1.0)) { chi2Cdf(it, k) } }
                "F.DIST" -> { need(a, 4..4); val x = num(a[0]); if (x < 0) throw Err("#NUM!"); val d1 = num(a[1]); val d2 = num(a[2]); if (cum(3)) fCdf(x, d1, d2) else SheetMath.fPdf(x, d1, d2) }
                "F.DIST.RT", "FDIST" -> { need(a, 3..3); val x = num(a[0]); if (x < 0) throw Err("#NUM!"); 1 - fCdf(x, num(a[1]), num(a[2])) }
                "F.INV" -> { need(a, 3..3); val p = prob(num(a[0])); val d1 = num(a[1]); val d2 = num(a[2]); invert(p, 0.0, 2.0) { fCdf(it, d1, d2) } }
                "F.INV.RT", "FINV" -> { need(a, 3..3); val p = prob(num(a[0])); val d1 = num(a[1]); val d2 = num(a[2]); invert(1 - p, 0.0, 2.0) { fCdf(it, d1, d2) } }
                "BINOM.DIST", "BINOMDIST" -> {
                    need(a, 4..4)
                    val k = floor(num(a[0])); val n = floor(num(a[1])); val p = num(a[2])
                    if (k < 0 || k > n || p < 0 || p > 1) throw Err("#NUM!")
                    if (cum(3)) (0..k.toInt()).sumOf { binom(it.toDouble(), n, p) } else binom(k, n, p)
                }
                "BINOM.DIST.RANGE" -> {
                    need(a, 3..4)
                    val n = floor(num(a[0])); val p = num(a[1]); val s1 = floor(num(a[2])); val s2 = given(a, 3)?.let { floor(num(it)) } ?: s1
                    if (s1 < 0 || s2 < s1 || s2 > n || p < 0 || p > 1) throw Err("#NUM!")
                    (s1.toInt()..s2.toInt()).sumOf { binom(it.toDouble(), n, p) }
                }
                "BINOM.INV", "CRITBINOM" -> {
                    need(a, 3..3)
                    val n = floor(num(a[0])); val p = num(a[1]); val alpha = num(a[2])
                    if (n < 0 || p < 0 || p > 1 || alpha <= 0 || alpha >= 1) throw Err("#NUM!")
                    var c = 0.0; var k = 0
                    while (k < n) { c += binom(k.toDouble(), n, p); if (c >= alpha - 1e-12) break; k++ }
                    k.toDouble()
                }
                "POISSON.DIST", "POISSON" -> {
                    need(a, 3..3)
                    val k = floor(num(a[0])); val l = num(a[1])
                    if (k < 0 || l < 0) throw Err("#NUM!")
                    if (cum(2)) (if (l == 0.0) 1.0 else SheetMath.gammaQ(k + 1, l))
                    else if (l == 0.0) (if (k == 0.0) 1.0 else 0.0) else exp(k * ln(l) - l - lnGamma(k + 1))
                }
                "EXPON.DIST", "EXPONDIST" -> { need(a, 3..3); val x = num(a[0]); val l = pos(num(a[1])); if (x < 0) throw Err("#NUM!"); if (cum(2)) 1 - exp(-l * x) else l * exp(-l * x) }
                "GAMMA.DIST", "GAMMADIST" -> {
                    need(a, 4..4)
                    val x = num(a[0]); val al = pos(num(a[1])); val be = pos(num(a[2])); if (x < 0) throw Err("#NUM!")
                    if (cum(3)) SheetMath.gammaCdf(x, al, be) else SheetMath.gammaPdf(x, al, be)
                }
                "GAMMA.INV", "GAMMAINV" -> {
                    need(a, 3..3)
                    val p = num(a[0]); if (p < 0 || p >= 1) throw Err("#NUM!")
                    val al = pos(num(a[1])); val be = pos(num(a[2]))
                    invert(p, 0.0, al * be + 1) { SheetMath.gammaCdf(it, al, be) }
                }
                "BETA.DIST", "BETADIST" -> {
                    val lo = if (f == "BETADIST") 3 else 4
                    need(a, lo..lo + 2)
                    val al = pos(num(a[1])); val be = pos(num(a[2]))
                    val from = opt(a, lo, 0.0); val to = opt(a, lo + 1, 1.0)
                    val x = num(a[0]); if (x < from || x > to || from == to) throw Err("#NUM!")
                    val t = (x - from) / (to - from)
                    if (f == "BETADIST" || cum(3)) betaI(al, be, t) else SheetMath.betaPdf(t, al, be) / (to - from)
                }
                "BETA.INV", "BETAINV" -> {
                    need(a, 3..5)
                    val p = num(a[0]); if (p < 0 || p > 1) throw Err("#NUM!")
                    val al = pos(num(a[1])); val be = pos(num(a[2])); val from = opt(a, 3, 0.0); val to = opt(a, 4, 1.0)
                    from + (to - from) * invert(p, 0.0, 1.0) { betaI(al, be, it) }
                }
                "WEIBULL.DIST", "WEIBULL" -> {
                    need(a, 4..4)
                    val x = num(a[0]); val al = pos(num(a[1])); val be = pos(num(a[2])); if (x < 0) throw Err("#NUM!")
                    if (cum(3)) 1 - exp(-(x / be).pow(al)) else al / be.pow(al) * x.pow(al - 1) * exp(-(x / be).pow(al))
                }
                "HYPGEOM.DIST", "HYPGEOMDIST" -> {
                    need(a, 4..5)
                    val k = floor(num(a[0])); val n = floor(num(a[1])); val big = floor(num(a[2])); val pop = floor(num(a[3]))
                    if (k < 0 || n > pop || big > pop || k > minOf(n, big) || n - k > pop - big) throw Err("#NUM!")
                    fun pmf(j: Double) = exp(lnChoose(big, j) + lnChoose(pop - big, n - j) - lnChoose(pop, n))
                    if (f == "HYPGEOM.DIST" && cum(4)) (maxOf(0.0, n - (pop - big)).toInt()..k.toInt()).sumOf { pmf(it.toDouble()) } else pmf(k)
                }
                "NEGBINOM.DIST", "NEGBINOMDIST" -> {
                    need(a, 3..4)
                    val k = floor(num(a[0])); val r = floor(num(a[1])); val p = num(a[2])
                    if (k < 0 || r < 1 || p < 0 || p > 1) throw Err("#NUM!")
                    if (f == "NEGBINOM.DIST" && cum(3)) betaI(r, k + 1, p) else exp(lnChoose(k + r - 1, r - 1) + r * ln(p) + k * ln(1 - p))
                }
                "GAMMA" -> { need(a, 1..1); val x = num(a[0]); if (x <= 0 && x == floor(x)) throw Err("#NUM!"); SheetMath.gamma(x) }
                "GAMMALN", "GAMMALN.PRECISE" -> { need(a, 1..1); lnGamma(pos(num(a[0]))) }
                "ERF", "ERF.PRECISE" -> { need(a, 1..2); if (a.size == 2) SheetMath.erf(num(a[1])) - SheetMath.erf(num(a[0])) else SheetMath.erf(num(a[0])) }
                "ERFC", "ERFC.PRECISE" -> { need(a, 1..1); SheetMath.erfc(num(a[0])) }
                else -> null
            }
        }

        fun finance(f: String, a: List<Arg>): Double? {
            fun pmt(r: Double, n: Double, pv: Double, fv: Double, type: Double) =
                if (r == 0.0) -(pv + fv) / n else -(r * (pv * (1 + r).pow(n) + fv)) / ((1 + r * type) * ((1 + r).pow(n) - 1))
            fun fv(r: Double, n: Double, pmt: Double, pv: Double, type: Double) =
                if (r == 0.0) -(pv + pmt * n) else -(pv * (1 + r).pow(n) + pmt * (1 + r * type) * ((1 + r).pow(n) - 1) / r)
            fun ipmt(r: Double, per: Double, n: Double, pv: Double, fv: Double, type: Double): Double {
                if (per < 1 || per > n) throw Err("#NUM!")
                if (type == 1.0 && per == 1.0) return 0.0
                val i = fv(r, per - 1, pmt(r, n, pv, fv, type), pv, type) * r
                return if (type == 1.0) i / (1 + r) else i
            }
            fun solve(guess: Double, g: (Double) -> Double): Double {
                var r = guess
                repeat(200) {
                    val y = g(r); val h = 1e-7 * maxOf(1.0, abs(r))
                    val d = (g(r + h) - g(r - h)) / (2 * h)
                    if (d == 0.0 || d.isNaN()) throw Err("#NUM!")
                    val next = r - y / d
                    if (next <= -1) r = (r - 1) / 2 else { if (abs(next - r) < 1e-12) return next; r = next }
                }
                if (abs(g(r)) < 1e-6) return r
                throw Err("#NUM!")
            }
            return when (f) {
                "PMT" -> { need(a, 3..5); pmt(num(a[0]), num(a[1]), num(a[2]), opt(a, 3, 0.0), opt(a, 4, 0.0)) }
                "FV" -> { need(a, 3..5); fv(num(a[0]), num(a[1]), num(a[2]), opt(a, 3, 0.0), opt(a, 4, 0.0)) }
                "PV" -> {
                    need(a, 3..5)
                    val r = num(a[0]); val n = num(a[1]); val p = num(a[2]); val fvv = opt(a, 3, 0.0); val t = opt(a, 4, 0.0)
                    if (r == 0.0) -(fvv + p * n) else -(fvv + p * (1 + r * t) * ((1 + r).pow(n) - 1) / r) / (1 + r).pow(n)
                }
                "NPER" -> {
                    need(a, 3..5)
                    val r = num(a[0]); val p = num(a[1]); val pv = num(a[2]); val fvv = opt(a, 3, 0.0); val t = opt(a, 4, 0.0)
                    if (r == 0.0) { if (p == 0.0) throw Err("#NUM!"); -(pv + fvv) / p }
                    else ln((p * (1 + r * t) - fvv * r) / (p * (1 + r * t) + pv * r)).also { if (it.isNaN()) throw Err("#NUM!") } / ln(1 + r)
                }
                "RATE" -> {
                    need(a, 3..6)
                    val n = num(a[0]); val p = num(a[1]); val pv = num(a[2]); val fvv = opt(a, 3, 0.0); val t = opt(a, 4, 0.0)
                    solve(opt(a, 5, 0.1)) { r -> if (abs(r) < 1e-12) pv + p * n + fvv else pv * (1 + r).pow(n) + p * (1 + r * t) * ((1 + r).pow(n) - 1) / r + fvv }
                }
                "IPMT" -> { need(a, 4..6); ipmt(num(a[0]), num(a[1]), num(a[2]), num(a[3]), opt(a, 4, 0.0), opt(a, 5, 0.0)) }
                "PPMT" -> {
                    need(a, 4..6)
                    val r = num(a[0]); val n = num(a[2]); val pv = num(a[3]); val fvv = opt(a, 4, 0.0); val t = opt(a, 5, 0.0)
                    pmt(r, n, pv, fvv, t) - ipmt(r, num(a[1]), n, pv, fvv, t)
                }
                "CUMIPMT", "CUMPRINC" -> {
                    need(a, 6..6)
                    val r = num(a[0]); val n = num(a[1]); val pv = num(a[2]); val s = num(a[3]).toInt(); val e = num(a[4]).toInt(); val t = num(a[5])
                    if (r <= 0 || n <= 0 || pv <= 0 || s < 1 || e < s || e > n) throw Err("#NUM!")
                    (s..e).sumOf { k -> val i = ipmt(r, k.toDouble(), n, pv, 0.0, t); if (f == "CUMIPMT") i else pmt(r, n, pv, 0.0, t) - i }
                }
                "ISPMT" -> { need(a, 4..4); val r = num(a[0]); val per = num(a[1]); val n = num(a[2]); val pv = num(a[3]); pv * r * (per / n - 1) }
                "NPV" -> { if (a.size < 2) throw Err("#VALUE!"); val r = num(a[0]); numbers(a.drop(1)).withIndex().sumOf { (k, x) -> x / (1 + r).pow(k + 1) } }
                "IRR" -> {
                    need(a, 1..2)
                    val v = numbers(listOf(a[0])); if (v.none { it > 0 } || v.none { it < 0 }) throw Err("#NUM!")
                    solve(opt(a, 1, 0.1)) { r -> v.withIndex().sumOf { (k, x) -> x / (1 + r).pow(k) } }
                }
                "MIRR" -> {
                    need(a, 3..3)
                    val v = numbers(listOf(a[0])); val fr = num(a[1]); val rr = num(a[2]); val n = v.size
                    val pos = v.withIndex().sumOf { (k, x) -> if (x > 0) x / (1 + rr).pow(k) else 0.0 }
                    val neg = v.withIndex().sumOf { (k, x) -> if (x < 0) x / (1 + fr).pow(k) else 0.0 }
                    if (pos == 0.0 || neg == 0.0) throw Err("#DIV/0!")
                    (-pos * (1 + rr).pow(n - 1) / neg).pow(1.0 / (n - 1)) - 1
                }
                "XNPV", "XIRR" -> {
                    need(a, if (f == "XNPV") 3..3 else 2..3)
                    val off = if (f == "XNPV") 1 else 0
                    val (v, d) = paired(a[off], a[off + 1])
                    if (v.isEmpty()) throw Err("#NUM!")
                    fun x(r: Double) = v.indices.sumOf { v[it] / (1 + r).pow((d[it] - d[0]) / 365) }
                    if (f == "XNPV") x(num(a[0])) else solve(opt(a, 2, 0.1)) { x(it) }
                }
                "EFFECT" -> { need(a, 2..2); val r = num(a[0]); val p = floor(num(a[1])); if (r <= 0 || p < 1) throw Err("#NUM!"); (1 + r / p).pow(p) - 1 }
                "NOMINAL" -> { need(a, 2..2); val r = num(a[0]); val p = floor(num(a[1])); if (r <= 0 || p < 1) throw Err("#NUM!"); p * ((1 + r).pow(1 / p) - 1) }
                "RRI" -> { need(a, 3..3); val n = num(a[0]); if (n <= 0) throw Err("#NUM!"); (num(a[2]) / num(a[1])).pow(1 / n) - 1 }
                "PDURATION" -> { need(a, 3..3); val r = num(a[0]); val pv = num(a[1]); val fvv = num(a[2]); if (r <= 0 || pv <= 0 || fvv <= 0) throw Err("#NUM!"); (ln(fvv) - ln(pv)) / ln(1 + r) }
                "FVSCHEDULE" -> { need(a, 2..2); numbers(listOf(a[1])).fold(num(a[0])) { p, r -> p * (1 + r) } }
                "SLN" -> { need(a, 3..3); val life = num(a[2]); if (life == 0.0) throw Err("#DIV/0!"); (num(a[0]) - num(a[1])) / life }
                "SYD" -> { need(a, 4..4); val c = num(a[0]); val s = num(a[1]); val life = num(a[2]); val per = num(a[3]); if (life <= 0 || per < 1 || per > life) throw Err("#NUM!"); (c - s) * (life - per + 1) * 2 / (life * (life + 1)) }
                "DDB" -> {
                    need(a, 4..5)
                    val c = num(a[0]); val s = num(a[1]); val life = num(a[2]); val per = num(a[3]); val factor = opt(a, 4, 2.0)
                    if (life <= 0 || per < 1 || per > life) throw Err("#NUM!")
                    var book = c; var dep = 0.0
                    repeat(kotlin.math.ceil(per).toInt()) { dep = minOf(book * factor / life, maxOf(0.0, book - s)); book -= dep }
                    dep
                }
                "DB" -> {
                    need(a, 4..5)
                    val c = num(a[0]); val s = num(a[1]); val life = num(a[2]); val per = num(a[3]).toInt(); val month = opt(a, 4, 12.0)
                    if (c <= 0 || life <= 0 || per < 1) throw Err("#NUM!")
                    val rate = Math.round((1 - (s / c).pow(1 / life)) * 1000) / 1000.0
                    var total = 0.0; var dep = 0.0
                    for (k in 1..per) {
                        dep = when { k == 1 -> c * rate * month / 12; k.toDouble() == life + 1 -> (c - total) * rate * (12 - month) / 12; else -> (c - total) * rate }
                        total += dep
                    }
                    dep
                }
                else -> null
            }
        }

        fun engineering(f: String, a: List<Arg>): Double? = when (f) {
            "DELTA" -> { need(a, 1..2); if (num(a[0]) == opt(a, 1, 0.0)) 1.0 else 0.0 }
            "GESTEP" -> { need(a, 1..2); if (num(a[0]) >= opt(a, 1, 0.0)) 1.0 else 0.0 }
            "BITAND", "BITOR", "BITXOR" -> {
                need(a, 2..2)
                val x = int(a[0]); val y = int(a[1]); if (x < 0 || y < 0) throw Err("#NUM!")
                when (f) { "BITAND" -> x and y; "BITOR" -> x or y; else -> x xor y }.toDouble()
            }
            "BITLSHIFT", "BITRSHIFT" -> {
                need(a, 2..2)
                val x = int(a[0]); var k = int(a[1]).toInt(); if (x < 0 || abs(k) > 53) throw Err("#NUM!")
                if (f == "BITRSHIFT") k = -k
                (if (k >= 0) x shl k else x shr -k).toDouble()
            }
            "BESSELJ", "BESSELY", "BESSELI", "BESSELK" -> {
                need(a, 2..2)
                val x = num(a[0]); val n = int(a[1]).toInt(); if (n < 0) throw Err("#NUM!")
                when (f) {
                    "BESSELJ" -> com.example.cas.cas.Statistics.besselJ(n.toDouble(), x)
                    "BESSELY" -> { if (x <= 0) throw Err("#NUM!"); com.example.cas.cas.Statistics.besselY(n.toDouble(), x) }
                    "BESSELI" -> SheetMath.besselI(n, x)
                    else -> { if (x <= 0) throw Err("#NUM!"); SheetMath.besselK(n, x) }
                }
            }
            "BIN2DEC" -> { need(a, 1..1); fromBase(str(a[0]), 2) }
            "OCT2DEC" -> { need(a, 1..1); fromBase(str(a[0]), 8) }
            "HEX2DEC" -> { need(a, 1..1); fromBase(str(a[0]), 16) }
            "IMREAL" -> { need(a, 1..1); complex(a[0]).first }
            "IMAGINARY" -> { need(a, 1..1); complex(a[0]).second }
            "IMABS" -> { need(a, 1..1); complex(a[0]).let { kotlin.math.hypot(it.first, it.second) } }
            "IMARGUMENT" -> { need(a, 1..1); complex(a[0]).let { if (it.first == 0.0 && it.second == 0.0) throw Err("#DIV/0!"); kotlin.math.atan2(it.second, it.first) } }
            "CONVERT" -> {
                need(a, 3..3)
                val alias = mapOf("C" to "°C", "cel" to "°C", "F" to "°F", "fah" to "°F", "kel" to "K", "hr" to "h", "mn" to "min", "sec" to "s", "lbm" to "lb", "ozm" to "oz", "day" to "d", "ang" to "Å", "ltr" to "L", "l" to "L", "HP" to "hp")
                fun unit(t: String) = try { com.example.cas.engine.Units.parse(alias[t] ?: t) } catch (e: Exception) { throw Err("#N/A") }
                val from = unit(str(a[1])); val to = unit(str(a[2]))
                if (!from.dims.same(to.dims)) throw Err("#N/A")
                to.fromSI(from.toSI(num(a[0])))
            }
            else -> null
        }

        /** Bits Excel gives each base's 10 digits: binary 10, octal 30, hex 40. */
        fun bits(base: Int) = when (base) { 2 -> 10; 8 -> 30; else -> 40 }

        /** A number written in [base], up to 10 digits; a full 10 with the top bit set is negative (two's complement). */
        fun fromBase(t: String, base: Int): Double {
            val s = t.trim()
            if (s.isEmpty() || s.length > 10) throw Err("#NUM!")
            val v = s.toLongOrNull(base) ?: throw Err("#NUM!")
            val b = bits(base)
            return (if (s.length == 10 && v >= (1L shl (b - 1))) v - (1L shl b) else v).toDouble()
        }

        fun toBase(n: Long, base: Int, places: Int?): String {
            val b = bits(base)
            if (n < -(1L shl (b - 1)) || n >= (1L shl (b - 1))) throw Err("#NUM!")
            if (n < 0) return (n + (1L shl b)).toString(base).uppercase()
            val s = n.toString(base).uppercase()
            if (places != null) { if (places < s.length) throw Err("#NUM!"); return s.padStart(places, '0') }
            return s
        }

        /** A complex number written as Excel's IM functions write them: "3+4i", "-2j", "5". */
        fun complex(a: Arg): Pair<Double, Double> {
            if (a is Arg.Num) return a.v to 0.0
            val t = str(a).trim().replace(" ", "")
            if (t.isEmpty()) return 0.0 to 0.0
            DataTable.number(t)?.let { return it to 0.0 }
            val num = """(?:\d+\.?\d*|\.\d+)(?:[eE][+-]?\d+)?"""
            Regex("""^([+-]?$num)([+-])($num)?[ij]$""").matchEntire(t)?.let { m ->
                val (re, sign, im) = m.destructured
                return re.toDouble() to (if (sign == "-") -1.0 else 1.0) * (im.toDoubleOrNull() ?: 1.0)
            }
            Regex("""^([+-]?)($num)?[ij]$""").matchEntire(t)?.let { m ->
                val (sign, im) = m.destructured
                return 0.0 to (if (sign == "-") -1.0 else 1.0) * (im.toDoubleOrNull() ?: 1.0)
            }
            throw Err("#NUM!")
        }

        fun complexText(re: Double, im: Double, suffix: String = "i"): String {
            if (re.isNaN() || im.isNaN() || re.isInfinite() || im.isInfinite()) throw Err("#NUM!")
            val scale = maxOf(abs(re), abs(im))
            val r = if (abs(re) < 1e-15 * maxOf(scale, 1.0)) 0.0 else re
            val i = if (abs(im) < 1e-15 * maxOf(scale, 1.0)) 0.0 else im
            val imPart = when (i) { 1.0 -> suffix; -1.0 -> "-$suffix"; else -> fmt(i) + suffix }
            return when {
                i == 0.0 -> fmt(r)
                r == 0.0 -> imPart
                else -> fmt(r) + (if (i > 0) "+" else "") + imPart
            }
        }

        fun engineeringText(f: String, a: List<Arg>): Arg? {
            val bases = mapOf("BIN" to 2, "OCT" to 8, "HEX" to 16, "DEC" to 10)
            Regex("""(BIN|OCT|HEX|DEC)2(BIN|OCT|HEX)""").matchEntire(f)?.let { m ->
                val (from, to) = m.destructured
                need(a, 1..2)
                val n = if (from == "DEC") int(a[0]) else fromBase(str(a[0]), bases.getValue(from)).toLong()
                return Arg.Str(toBase(n, bases.getValue(to), given(a, 1)?.let { int(it).toInt() }))
            }
            fun c(k: Int) = complex(a[k])
            fun out(z: Pair<Double, Double>) = Arg.Str(complexText(z.first, z.second))
            fun mul(x: Pair<Double, Double>, y: Pair<Double, Double>) = (x.first * y.first - x.second * y.second) to (x.first * y.second + x.second * y.first)
            fun polar(r: Double, t: Double) = r * kotlin.math.cos(t) to r * kotlin.math.sin(t)
            fun all() = a.flatMap { x -> if (x is Arg.Range && !x.single) flat(x).filter { !it.isBlank }.map { complex(Arg.Str(it.toString())) } else listOf(complex(x)) }
            return when (f) {
                "COMPLEX" -> {
                    need(a, 2..3)
                    val sfx = given(a, 2)?.let { str(it) } ?: "i"
                    if (sfx != "i" && sfx != "j") throw Err("#VALUE!")
                    Arg.Str(complexText(num(a[0]), num(a[1]), sfx))
                }
                "IMCONJUGATE" -> { need(a, 1..1); val z = c(0); out(z.first to -z.second) }
                "IMSUM" -> out(all().fold(0.0 to 0.0) { s, z -> (s.first + z.first) to (s.second + z.second) })
                "IMSUB" -> { need(a, 2..2); val x = c(0); val y = c(1); out((x.first - y.first) to (x.second - y.second)) }
                "IMPRODUCT" -> out(all().fold(1.0 to 0.0) { p, z -> mul(p, z) })
                "IMDIV" -> { need(a, 2..2); val x = c(0); val y = c(1); val d = y.first * y.first + y.second * y.second; if (d == 0.0) throw Err("#NUM!"); out(mul(x, y.first / d to -y.second / d)) }
                "IMPOWER" -> { need(a, 2..2); val z = c(0); val n = num(a[1]); out(polar(kotlin.math.hypot(z.first, z.second).pow(n), n * kotlin.math.atan2(z.second, z.first))) }
                "IMSQRT" -> { need(a, 1..1); val z = c(0); out(polar(sqrt(kotlin.math.hypot(z.first, z.second)), kotlin.math.atan2(z.second, z.first) / 2)) }
                "IMEXP" -> { need(a, 1..1); val z = c(0); out(polar(exp(z.first), z.second)) }
                "IMLN", "IMLOG10", "IMLOG2" -> {
                    need(a, 1..1)
                    val z = c(0); val r = kotlin.math.hypot(z.first, z.second); if (r == 0.0) throw Err("#NUM!")
                    val b = when (f) { "IMLOG10" -> ln(10.0); "IMLOG2" -> ln(2.0); else -> 1.0 }
                    out(ln(r) / b to kotlin.math.atan2(z.second, z.first) / b)
                }
                "IMSIN" -> { need(a, 1..1); val (x, y) = c(0); out(kotlin.math.sin(x) * kotlin.math.cosh(y) to kotlin.math.cos(x) * kotlin.math.sinh(y)) }
                "IMCOS" -> { need(a, 1..1); val (x, y) = c(0); out(kotlin.math.cos(x) * kotlin.math.cosh(y) to -kotlin.math.sin(x) * kotlin.math.sinh(y)) }
                else -> null
            }
        }
    }

    /**
     * A formula copied [dRows] rows down (and [dCols] columns across): relative references move
     * with it, $-fixed parts stay, as Excel's fill does.
     */
    fun shift(formula: String, dRows: Int, dCols: Int = 0): String =
        Regex("""(?<![A-Za-z0-9.$])(\$?)([A-Za-z]{1,3})(\$?)(\d+)(?![A-Za-z0-9.(])""").replace(formula) { m ->
            // Leave text in quotes alone.
            val before = formula.substring(0, m.range.first)
            if (before.count { it == '"' } % 2 == 1) return@replace m.value
            val (colFixed, col, rowFixed, row) = m.destructured
            val c = if (colFixed.isEmpty()) columnName((columnIndex(col) + dCols).coerceAtLeast(0)) else col.uppercase()
            val r = if (rowFixed.isEmpty()) (row.toInt() + dRows).coerceAtLeast(1) else row.toInt()
            "$colFixed$c$rowFixed$r"
        }

    /** One entry of the ƒx list: its group, the functions, how one is written, what they do. */
    class Help(val category: String, val names: String, val example: String, val what: String)

    val CATEGORIES = listOf("Math", "Statistics", "Distributions", "Lookup", "Logic", "Text", "Date & time", "Financial", "Engineering")

    /** The functions, for the ƒx list, by group. */
    val FUNCTIONS: List<Help> = listOf(
        Help("Math", "SUM · PRODUCT · SUMSQ", "SUM(A1:A10)", "Adds (or multiplies, or adds the squares of) the numbers"),
        Help("Math", "SUMIF · SUMIFS", "SUMIFS(B:B, A:A, \">5\")", "Adds where conditions hold"),
        Help("Math", "SUMPRODUCT", "SUMPRODUCT(A:A, B:B)", "Sum of products, row by row"),
        Help("Math", "SUMX2MY2 · SUMX2PY2 · SUMXMY2", "SUMXMY2(A:A, B:B)", "Sums of squares of two ranges"),
        Help("Math", "SUBTOTAL · AGGREGATE", "AGGREGATE(14, 6, A:A, 2)", "A statistic by number, optionally skipping errors"),
        Help("Math", "ROUND · ROUNDUP · ROUNDDOWN · MROUND", "ROUND(A1, 2)", "To a number of decimals or a multiple"),
        Help("Math", "INT · TRUNC · EVEN · ODD", "TRUNC(A1, 1)", "Whole numbers"),
        Help("Math", "FLOOR · CEILING (.MATH, .PRECISE)", "CEILING.MATH(A1, 5)", "Down or up to a multiple"),
        Help("Math", "MOD · QUOTIENT · GCD · LCM", "GCD(12, 18)", "Remainders and divisors"),
        Help("Math", "ABS · SIGN · SQRT · SQRTPI · POWER", "SQRT(A1^2+B1^2)", "Sizes, roots and powers"),
        Help("Math", "EXP · LN · LOG · LOG10", "LOG(A1, 2)", "Exponentials and logarithms"),
        Help("Math", "SIN · COS · TAN · COT · SEC · CSC", "SIN(RADIANS(A1))", "Trigonometry, in radians"),
        Help("Math", "ASIN · ACOS · ATAN · ATAN2 · ACOT", "ATAN2(A1, B1)", "Inverse trigonometry"),
        Help("Math", "SINH · COSH · TANH · ASINH · ACOSH · ATANH …", "TANH(A1)", "Hyperbolic functions and their inverses"),
        Help("Math", "DEGREES · RADIANS · PI", "DEGREES(PI()/4)", "Angle units"),
        Help("Math", "FACT · FACTDOUBLE · COMBIN · COMBINA", "COMBIN(10, 3)", "Factorials and combinations"),
        Help("Math", "PERMUT · PERMUTATIONA · MULTINOMIAL", "PERMUT(10, 3)", "Arrangements"),
        Help("Math", "SERIESSUM", "SERIESSUM(A1, 0, 1, B1:B5)", "A power series Σ aₖ xⁿ⁺ᵏᵐ"),
        Help("Math", "MDETERM", "MDETERM(A1:C3)", "Determinant of a square range"),
        Help("Math", "RAND · RANDBETWEEN", "RANDBETWEEN(1, 6)", "Random numbers (new on every change)"),
        Help("Statistics", "AVERAGE · AVERAGEA · MEDIAN · MODE", "AVERAGE(B:B)", "Mean, middle, most common"),
        Help("Statistics", "AVERAGEIF · AVERAGEIFS", "AVERAGEIF(A:A, \">0\")", "Mean where conditions hold"),
        Help("Statistics", "GEOMEAN · HARMEAN · TRIMMEAN", "TRIMMEAN(A:A, 0.2)", "Other means"),
        Help("Statistics", "MIN · MAX · MINA · MAXA · MINIFS · MAXIFS", "MAXIFS(B:B, A:A, \"<10\")", "Smallest or largest"),
        Help("Statistics", "COUNT · COUNTA · COUNTBLANK", "COUNT(A:A)", "How many numbers, filled or empty cells"),
        Help("Statistics", "COUNTIF · COUNTIFS", "COUNTIF(A:A, \">5\")", "How many meet conditions (wildcards * ?)"),
        Help("Statistics", "STDEV · STDEV.P · STDEVA · STDEVPA", "STDEV(B1:B20)", "Standard deviation (sample or population)"),
        Help("Statistics", "VAR · VAR.P · VARA · VARPA · DEVSQ · AVEDEV", "VAR(B:B)", "Spread"),
        Help("Statistics", "SKEW · SKEW.P · KURT", "SKEW(A:A)", "Shape of the distribution"),
        Help("Statistics", "LARGE · SMALL · RANK · RANK.AVG", "RANK(A1, A:A)", "Order"),
        Help("Statistics", "PERCENTILE · QUARTILE (.INC, .EXC)", "QUARTILE(A:A, 3)", "Percentiles and quartiles"),
        Help("Statistics", "PERCENTRANK (.INC, .EXC)", "PERCENTRANK(A:A, A1)", "Where a value sits, 0 to 1"),
        Help("Statistics", "SLOPE · INTERCEPT · STEYX", "SLOPE(B:B, A:A)", "Least-squares line (y's first)"),
        Help("Statistics", "CORREL · PEARSON · RSQ", "RSQ(B:B, A:A)", "Correlation, R²"),
        Help("Statistics", "FORECAST · TREND · GROWTH", "FORECAST(10, B:B, A:A)", "Predict from a line or an exponential"),
        Help("Statistics", "COVAR · COVARIANCE.S · COVARIANCE.P", "COVARIANCE.S(A:A, B:B)", "Covariance"),
        Help("Statistics", "STANDARDIZE · FISHER · FISHERINV", "STANDARDIZE(A1, 10, 2)", "z-scores and Fisher's transform"),
        Help("Statistics", "CONFIDENCE.NORM · CONFIDENCE.T", "CONFIDENCE.T(0.05, 2.1, 30)", "Half-width of a confidence interval"),
        Help("Statistics", "T.TEST · Z.TEST · F.TEST · CHISQ.TEST", "T.TEST(A:A, B:B, 2, 3)", "p-values of tests"),
        Help("Distributions", "NORM.DIST · NORM.INV · NORM.S.DIST · NORM.S.INV", "NORM.DIST(A1, 0, 1, TRUE)", "Normal distribution and its inverse"),
        Help("Distributions", "PHI · GAUSS", "GAUSS(1.96)", "Normal density, and P(0 < Z < z)"),
        Help("Distributions", "LOGNORM.DIST · LOGNORM.INV", "LOGNORM.DIST(A1, 0, 1, TRUE)", "Log-normal"),
        Help("Distributions", "T.DIST (.2T, .RT) · T.INV (.2T)", "T.INV.2T(0.05, 10)", "Student's t"),
        Help("Distributions", "CHISQ.DIST (.RT) · CHISQ.INV (.RT)", "CHISQ.INV.RT(0.05, 3)", "χ²"),
        Help("Distributions", "F.DIST (.RT) · F.INV (.RT)", "F.INV.RT(0.05, 2, 10)", "F"),
        Help("Distributions", "BINOM.DIST · BINOM.INV · BINOM.DIST.RANGE", "BINOM.DIST(3, 10, 0.5, FALSE)", "Binomial"),
        Help("Distributions", "POISSON.DIST · EXPON.DIST", "POISSON.DIST(2, 3, TRUE)", "Poisson and exponential"),
        Help("Distributions", "GAMMA.DIST · GAMMA.INV · BETA.DIST · BETA.INV", "BETA.DIST(0.3, 2, 5, TRUE)", "Gamma and beta"),
        Help("Distributions", "WEIBULL.DIST · HYPGEOM.DIST · NEGBINOM.DIST", "WEIBULL.DIST(1, 2, 1, TRUE)", "Weibull, hypergeometric, negative binomial"),
        Help("Distributions", "GAMMA · GAMMALN · ERF · ERFC", "ERF(1)", "Special functions"),
        Help("Lookup", "VLOOKUP · HLOOKUP", "VLOOKUP(5, A:C, 3, FALSE)", "Find a row (or column), return a cell from it"),
        Help("Lookup", "XLOOKUP · XMATCH", "XLOOKUP(\"b\", A:A, B:B, \"none\")", "Find with fallbacks, from either end"),
        Help("Lookup", "INDEX · MATCH", "INDEX(B:B, MATCH(MAX(A:A), A:A, 0))", "A cell by position; a value's position"),
        Help("Lookup", "LOOKUP · CHOOSE", "CHOOSE(2, \"a\", \"b\", \"c\")", "Sorted lookup; pick from a list"),
        Help("Lookup", "OFFSET · INDIRECT · ADDRESS", "SUM(OFFSET(A1, 0, 0, 5))", "References built from numbers or text"),
        Help("Lookup", "ROW · COLUMN · ROWS · COLUMNS", "ROW()", "Where the cell is, how big a range is"),
        Help("Logic", "IF · IFS · SWITCH", "IFS(A1<0, \"neg\", A1=0, \"zero\", TRUE, \"pos\")", "Choices"),
        Help("Logic", "AND · OR · XOR · NOT · TRUE · FALSE", "AND(A1>0, B1>0)", "Logic"),
        Help("Logic", "IFERROR · IFNA · NA", "IFERROR(1/A1, 0)", "Fallbacks for errors"),
        Help("Logic", "ISNUMBER · ISTEXT · ISBLANK · ISERROR · ISNA …", "ISBLANK(A1)", "What a cell holds"),
        Help("Logic", "ISEVEN · ISODD · ISFORMULA · ERROR.TYPE · N · T · TYPE", "ISEVEN(A1)", "More about a value"),
        Help("Text", "CONCAT · TEXTJOIN · &", "TEXTJOIN(\", \", TRUE, A1:A5)", "Join text"),
        Help("Text", "LEFT · RIGHT · MID · LEN", "LEFT(A1, 3)", "Parts of text"),
        Help("Text", "FIND · SEARCH · SUBSTITUTE · REPLACE", "SUBSTITUTE(A1, \",\", \".\")", "Find and replace"),
        Help("Text", "TEXTBEFORE · TEXTAFTER", "TEXTAFTER(A1, \"=\")", "Text around a separator"),
        Help("Text", "UPPER · LOWER · PROPER · TRIM · CLEAN · REPT", "PROPER(A1)", "Tidy text"),
        Help("Text", "TEXT · FIXED · DOLLAR · VALUE", "TEXT(A1, \"0.00E+00\")", "Numbers to text and back"),
        Help("Text", "EXACT · CHAR · CODE · UNICHAR · UNICODE", "CODE(\"A\")", "Characters"),
        Help("Text", "ROMAN · ARABIC · BASE · DECIMAL", "BASE(255, 16)", "Other numerals"),
        Help("Date & time", "DATE · TODAY · NOW · DATEVALUE", "DATE(2025, 3, 14)", "Dates as day numbers (1 = 1900-01-01)"),
        Help("Date & time", "YEAR · MONTH · DAY · WEEKDAY · WEEKNUM", "WEEKDAY(A1, 2)", "Parts of a date"),
        Help("Date & time", "TIME · HOUR · MINUTE · SECOND", "HOUR(NOW())", "Times as fractions of a day"),
        Help("Date & time", "DAYS · DAYS360 · DATEDIF · YEARFRAC", "DATEDIF(A1, B1, \"M\")", "Between two dates"),
        Help("Date & time", "EDATE · EOMONTH · WORKDAY · NETWORKDAYS", "NETWORKDAYS(A1, B1)", "Months ahead, working days"),
        Help("Financial", "PMT · PV · FV · NPER · RATE", "PMT(5%/12, 360, 200000)", "Loans and savings"),
        Help("Financial", "IPMT · PPMT · CUMIPMT · CUMPRINC", "IPMT(5%/12, 1, 360, 200000)", "Interest and principal of payments"),
        Help("Financial", "NPV · IRR · MIRR · XNPV · XIRR", "IRR(A1:A6)", "Cash-flow value and return"),
        Help("Financial", "EFFECT · NOMINAL · RRI · PDURATION · FVSCHEDULE", "EFFECT(5%, 12)", "Rates and growth"),
        Help("Financial", "SLN · SYD · DDB · DB", "DDB(10000, 1000, 5, 1)", "Depreciation"),
        Help("Engineering", "CONVERT", "CONVERT(A1, \"mi\", \"km\")", "Units, from the converter's list"),
        Help("Engineering", "DEC2BIN · BIN2DEC · DEC2HEX · HEX2DEC · …", "DEC2HEX(255)", "Binary, octal and hex"),
        Help("Engineering", "BITAND · BITOR · BITXOR · BITLSHIFT · BITRSHIFT", "BITAND(12, 10)", "Bits"),
        Help("Engineering", "BESSELJ · BESSELY · BESSELI · BESSELK", "BESSELJ(A1, 0)", "Bessel functions"),
        Help("Engineering", "DELTA · GESTEP", "GESTEP(A1, 5)", "Steps"),
        Help("Engineering", "COMPLEX · IMREAL · IMAGINARY · IMABS · IMARGUMENT", "IMABS(\"3+4i\")", "Complex numbers as text"),
        Help("Engineering", "IMSUM · IMSUB · IMPRODUCT · IMDIV · IMPOWER · IMSQRT …", "IMSQRT(\"-4\")", "Complex arithmetic, exp, ln, sin, cos"),
    )
}
