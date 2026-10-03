package com.example.cas.graph

import com.example.cas.editor.Node
import com.example.cas.editor.Sym
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * GeoGebra-style constructions in the 2D graph (alpha): named points, and objects built from
 * them with commands, each on its own line of the list:
 *
 *     A = (1, 2)            a free point (drag it on the graph)
 *     c = Circle(A, B)      the circle about A through B
 *     P = Intersect(c, l, 1)
 *     d = Distance(A, B)    a number, shown in the list and usable in other lines
 *
 * A line is read from the editor's row: names (a letter, or a symbol from the symbol builder),
 * command words (typed as one word or letter by letter, any case), brackets and commas. Anything
 * else is a number, worked out by the app ([Construction]'s `number`), so sliders and fractions
 * work inside commands. Objects can use objects from any other line, above or below.
 */
object Geometry {
    // ---- Objects ---------------------------------------------------------------------------

    sealed class Obj

    data class Point(val x: Double, val y: Double) : Obj() {
        operator fun minus(o: Point) = Point(x - o.x, y - o.y)
        operator fun plus(o: Point) = Point(x + o.x, y + o.y)
        fun times(k: Double) = Point(x * k, y * k)
        val length get() = hypot(x, y)
    }

    /** The infinite line through [a] and [b]. */
    data class Line(val a: Point, val b: Point) : Obj()
    data class Segment(val a: Point, val b: Point) : Obj()
    /** From [a] through [b] and on. */
    data class Ray(val a: Point, val b: Point) : Obj()
    /** An arrow from [a] to [b]. */
    data class Vector(val a: Point, val b: Point) : Obj()
    data class Circle(val center: Point, val r: Double) : Obj()
    data class Polygon(val points: List<Point>) : Obj()

    enum class Unit { None, Length, Angle, Area }

    /** A number: a length, an area, an angle (in radians) or a plain value. */
    data class Number(val value: Double, val unit: Unit = Unit.None) : Obj()

    /** An angle at [vertex], counterclockwise from [start] (radians) through [sweep]; its value is [sweep]. */
    data class Angle(val vertex: Point, val start: Double, val sweep: Double) : Obj()

    /** Several objects from one command: the points where two circles cross, the tangents from a point. */
    data class Many(val items: List<Obj>) : Obj()

    class GeometryError(message: String) : Exception(message)

    /** A name used before it's defined (on another line still to be worked out). */
    private class Missing(val name: String) : Exception("$name isn't defined")

    // ---- Statements ------------------------------------------------------------------------

    sealed class Ex {
        class Ref(val name: String) : Ex()
        class Call(val command: Command, val args: List<Ex>) : Ex()
        class Coord(val x: List<Node>, val y: List<Node>) : Ex()
        class Numeric(val nodes: List<Node>) : Ex()
    }

    /** A line of the list read as a construction: its name (if it has "name =") and what it is. */
    class Statement(val name: String?, val expr: Ex) {
        /** A point written with plain numbers (A = (1, 2)): it can be dragged. */
        val isFree get() = name != null && expr is Ex.Coord && plainNumber(expr.x) != null && plainNumber(expr.y) != null
    }

    /** A command: its name, how it's written, and what it makes. */
    class Command(val name: String, val usage: String, val help: String, val aliases: List<String> = emptyList()) {
        val arity get() = usage.substringAfter('(').substringBefore(')').split(',').size
    }

    val COMMANDS = listOf(
        Command("Segment", "Segment(A, B)", "The segment from A to B"),
        Command("Line", "Line(A, B)", "The line through A and B; Line(A, l) is the line through A parallel to l"),
        Command("Ray", "Ray(A, B)", "The ray from A through B"),
        Command("Vector", "Vector(A, B)", "The arrow from A to B"),
        Command("Circle", "Circle(A, B)", "The circle about A through B; Circle(A, r) with radius r; Circle(A, B, C) through three points"),
        Command("Polygon", "Polygon(A, B, C)", "The polygon with these corners (three or more)"),
        Command("Midpoint", "Midpoint(A, B)", "The point halfway from A to B (or the middle of a segment, the center of a circle)"),
        Command("Intersect", "Intersect(a, b)", "Where two lines, segments, circles or polygons cross; Intersect(a, b, n) picks the nth point"),
        Command("PerpendicularLine", "PerpendicularLine(A, l)", "The line through A at right angles to l", listOf("Perpendicular")),
        Command("ParallelLine", "ParallelLine(A, l)", "The line through A parallel to l", listOf("Parallel")),
        Command("PerpendicularBisector", "PerpendicularBisector(A, B)", "The line at right angles to AB through its middle"),
        Command("AngleBisector", "AngleBisector(A, B, C)", "The line through B halving the angle ABC"),
        Command("Tangent", "Tangent(A, c)", "The tangents from A to the circle c (two, one or none)"),
        Command("Centroid", "Centroid(poly)", "The center of mass of a polygon (or of its corners)"),
        Command("Incircle", "Incircle(A, B, C)", "The circle inside the triangle ABC touching its sides"),
        Command("Distance", "Distance(A, B)", "How far apart two points are, or a point and a line"),
        Command("Length", "Length(s)", "The length of a segment or vector, or the perimeter of a polygon"),
        Command("Perimeter", "Perimeter(poly)", "The perimeter of a polygon, or a circle's circumference"),
        Command("Area", "Area(poly)", "The area of a polygon or circle"),
        Command("Angle", "Angle(A, B, C)", "The angle at B from A round to C (counterclockwise)"),
        Command("Slope", "Slope(l)", "The slope of a line or segment"),
        Command("Radius", "Radius(c)", "A circle's radius"),
        Command("Reflect", "Reflect(obj, l)", "The mirror image in a line (or through a point)"),
        Command("Rotate", "Rotate(obj, θ, A)", "Turned by θ about A (about the origin without A)"),
        Command("Translate", "Translate(obj, v)", "Moved by the vector v (a vector, or a pair (dx, dy))"),
        Command("Dilate", "Dilate(obj, k, A)", "Scaled by k from A (from the origin without A)"),
    )

    private val byName: Map<String, Command> = COMMANDS.flatMap { c -> (listOf(c.name) + c.aliases).map { it.lowercase() to c } }.toMap()

    fun command(word: String): Command? = byName[word.lowercase()]

    private fun text(n: Node?) = (n as? Sym)?.text

    /** A name: one letter (Latin or Greek), or a symbol from the symbol builder (A₁, P′). */
    fun isName(n: Node?): Boolean {
        val t = text(n) ?: return false
        if (com.example.cas.cas.CustomSymbol.isCustom(t)) return com.example.cas.cas.CustomSymbol.decode(t) != null
        // (Not e or i, which are numbers; nor π.)
        return t.length == 1 && t[0].isLetter() && t != "e" && t != "i" && t != "π"
    }

    /** A point's name: a capital letter, or a built symbol on one (GeoGebra's convention). */
    fun isPointName(n: Node?): Boolean {
        val t = text(n) ?: return false
        val base = com.example.cas.cas.CustomSymbol.decode(t)?.base ?: t
        return isName(n) && base.firstOrNull()?.isUpperCase() == true
    }

    /** The command a row starts with (one word, or letters spelling it) and how many nodes it takes. */
    private fun leadingCommand(items: List<Node>): Pair<Command, Int>? {
        text(items.firstOrNull())?.let { t -> if (t.length > 1 && t.all { it.isLetter() }) command(t)?.let { return it to 1 } }
        val sb = StringBuilder()
        var k = 0
        var best: Pair<Command, Int>? = null
        while (k < items.size) {
            val t = text(items[k]) ?: break
            if (t.length != 1 || !t[0].isLetter()) break
            sb.append(t); k++
            command(sb.toString())?.let { best = it to k }
        }
        return best
    }

    private fun splitArgs(items: List<Node>): List<List<Node>> {
        val out = ArrayList<List<Node>>()
        var depth = 0; var start = 0
        items.forEachIndexed { k, n ->
            when (text(n)) { "(", "[" -> depth++; ")", "]" -> depth--; "," -> if (depth == 0) { out += items.subList(start, k); start = k + 1 } }
        }
        out += items.subList(start, items.size)
        return out
    }

    /** Index of the bracket closing the one at [open], or −1. */
    private fun closing(items: List<Node>, open: Int): Int {
        var depth = 0
        for (k in open until items.size) when (text(items[k])) { "(", "[" -> depth++; ")", "]" -> { depth--; if (depth == 0) return k } }
        return -1
    }

    private fun parseCall(items: List<Node>): Ex.Call? {
        val (cmd, used) = leadingCommand(items) ?: return null
        if (text(items.getOrNull(used)) != "(") return null
        val end = closing(items, used)
        if (end != items.lastIndex) return null
        val inner = items.subList(used + 1, end)
        val args = if (inner.isEmpty()) emptyList() else splitArgs(inner).map { parseArg(it) }
        return Ex.Call(cmd, args)
    }

    private fun coord(items: List<Node>): Ex.Coord? {
        if (text(items.firstOrNull()) != "(" || closing(items, 0) != items.lastIndex) return null
        val parts = splitArgs(items.subList(1, items.size - 1))
        if (parts.size != 2 || parts.any { it.isEmpty() }) return null
        return Ex.Coord(parts[0], parts[1])
    }

    private fun parseArg(items: List<Node>): Ex {
        if (items.size == 1 && isName(items[0])) return Ex.Ref(text(items[0])!!)
        parseCall(items)?.let { return it }
        coord(items)?.let { return it }
        return Ex.Numeric(items)
    }

    /**
     * The row as a construction, or null when it isn't one (a function, an equation, a plain
     * point (2, 3) without a name: the graph's usual lines).
     */
    fun parse(items: List<Node>): Statement? {
        val eq = run {
            var depth = 0
            items.indexOfFirst { n -> when (text(n)) { "(", "[" -> depth++; ")", "]" -> depth-- }; depth == 0 && text(n) == "=" }
        }
        val name = if (eq == 1 && isName(items[0])) text(items[0]) else null
        if (eq >= 0 && name == null) return null
        val rhs = if (eq >= 0) items.subList(eq + 1, items.size) else items
        parseCall(rhs)?.let { return Statement(name, it) }
        // A = (1, 2): a point, named with a capital (f = (1, 2) stays the graph's own point).
        if (name != null && isPointName(items[0])) coord(rhs)?.let { return Statement(name, it) }
        return null
    }

    /** A plain number written in [nodes] (digits, a point, a minus), or null. */
    fun plainNumber(nodes: List<Node>): Double? {
        if (nodes.isEmpty() || !nodes.all { it is Sym }) return null
        return nodes.joinToString("") { (it as Sym).text }.replace("−", "-").toDoubleOrNull()
    }

    // ---- Working a construction out ------------------------------------------------------

    /** What a line came to: its object, or why it couldn't be made. */
    class Outcome(val obj: Obj?, val error: String?)

    /**
     * Works out every statement (null for lines that aren't constructions), each with the objects
     * named on the other lines. [number] works out a number from its nodes, given the numbers
     * named so far; [degrees] is how a typed angle (Rotate) is read.
     */
    fun build(
        statements: List<Statement?>, number: (List<Node>, Map<String, Double>) -> Double, degrees: Boolean = false,
        /** Called with each statement's index just before it's worked out (to know whose sliders a number makes). */
        onStatement: (Int) -> kotlin.Unit = {},
    ): List<Outcome?> {
        val out = arrayOfNulls<Outcome>(statements.size)
        val env = LinkedHashMap<String, Obj>()
        val defined = statements.mapNotNull { it?.name }.toSet()
        val waiting = statements.indices.filter { statements[it] != null }.toMutableList()
        // As many passes as lines, so each can use a line below it; what's left waits on itself.
        repeat(statements.size + 1) {
            val before = waiting.size
            val it2 = waiting.iterator()
            while (it2.hasNext()) {
                val k = it2.next()
                val s = statements[k]!!
                try {
                    onStatement(k)
                    val obj = Evaluation(env, defined, number, degrees).obj(s.expr)
                    out[k] = Outcome(obj, null)
                    s.name?.let { env[it] = obj }
                    it2.remove()
                } catch (e: Missing) {
                    // Waits for a later pass (unless no line defines it).
                    if (e.name !in defined) { out[k] = Outcome(null, e.message); it2.remove() }
                } catch (e: GeometryError) {
                    out[k] = Outcome(null, e.message); it2.remove()
                } catch (e: RuntimeException) {
                    out[k] = Outcome(null, e.message ?: "Can't make this"); it2.remove()
                }
            }
            if (waiting.size == before) return@repeat
        }
        waiting.forEach { k -> out[k] = Outcome(null, "This depends on itself") }
        return out.toList()
    }

    private class Evaluation(val env: Map<String, Obj>, val defined: Set<String>, val numberOf: (List<Node>, Map<String, Double>) -> Double, val degrees: Boolean) {
        fun numbers(): Map<String, Double> = env.mapNotNull { (k, v) -> value(v)?.let { k to it } }.toMap()

        fun obj(e: Ex): Obj = when (e) {
            // A capital is a point's name, so an unknown one is missing (not a new slider, as a or k is).
            is Ex.Ref -> env[e.name] ?: if (e.name in defined || isPointName(Sym(e.name))) throw Missing(e.name) else Number(numberOf(listOf(Sym(e.name)), numbers()))
            is Ex.Coord -> Point(numberOf(e.x, numbers()), numberOf(e.y, numbers()))
            is Ex.Numeric -> Number(numberOf(e.nodes, numbers()))
            is Ex.Call -> call(e.command, e.args.map { obj(it) })
        }

        fun point(o: Obj, what: String = "a point"): Point = o as? Point ?: throw GeometryError("Expected $what here")
        fun num(o: Obj): Double = value(o) ?: throw GeometryError("Expected a number here")
        fun angle(o: Obj): Double = when (o) {
            is Angle -> o.sweep
            else -> num(o).let { if (degrees) Math.toRadians(it) else it }
        }

        fun call(c: Command, a: List<Obj>): Obj {
            fun need(vararg counts: Int) { if (a.size !in counts) throw GeometryError("${c.name} takes ${counts.joinToString(" or ")} things: ${c.usage}") }
            return when (c.name) {
                "Segment" -> { need(2); Segment(point(a[0]), point(a[1])) }
                "Line" -> { need(2)
                    val p = point(a[0])
                    when (val q = a[1]) {
                        is Point -> { if (p == q) throw GeometryError("The two points are the same"); Line(p, q) }
                        else -> direction(q).let { d -> Line(p, p + d) }
                    }
                }
                "Ray" -> { need(2); Ray(point(a[0]), point(a[1])) }
                "Vector" -> when (a.size) {
                    1 -> Vector(Point(0.0, 0.0), point(a[0]))
                    else -> { need(2); Vector(point(a[0]), point(a[1])) }
                }
                "Circle" -> when (a.size) {
                    2 -> { val c0 = point(a[0]); when (val b = a[1]) { is Point -> Circle(c0, (b - c0).length); else -> Circle(c0, abs(num(b))) } }
                    3 -> circumcircle(point(a[0]), point(a[1]), point(a[2]))
                    else -> { need(2, 3); throw IllegalStateException() }
                }
                "Polygon" -> { if (a.size < 3) throw GeometryError("A polygon needs three corners or more"); Polygon(a.map { point(it) }) }
                "Midpoint" -> when (a.size) {
                    1 -> when (val o = a[0]) {
                        is Segment -> mid(o.a, o.b)
                        is Circle -> o.center
                        is Vector -> mid(o.a, o.b)
                        else -> throw GeometryError("Midpoint of a segment, or of two points")
                    }
                    else -> { need(2); mid(point(a[0]), point(a[1])) }
                }
                "Intersect" -> { need(2, 3)
                    val all = intersections(a[0], a[1])
                    if (a.size == 3) {
                        val n = num(a[2]).toInt()
                        all.getOrNull(n - 1) ?: throw GeometryError(if (all.isEmpty()) "They don't cross" else "There ${if (all.size == 1) "is only 1 point" else "are only ${all.size} points"}")
                    } else all.singleOrNull() ?: Many(all)
                }
                "PerpendicularLine" -> { need(2); val p = point(a[0]); val d = direction(a[1]); Line(p, p + Point(-d.y, d.x)) }
                "ParallelLine" -> { need(2); val p = point(a[0]); Line(p, p + direction(a[1])) }
                "PerpendicularBisector" -> {
                    val (p, q) = when (a.size) { 1 -> (a[0] as? Segment ?: throw GeometryError("Expected a segment")).let { it.a to it.b }; else -> { need(2); point(a[0]) to point(a[1]) } }
                    if (p == q) throw GeometryError("The two points are the same")
                    val m = mid(p, q); val d = q - p
                    Line(m, m + Point(-d.y, d.x))
                }
                "AngleBisector" -> { need(3)
                    val p = point(a[0]); val b = point(a[1]); val q = point(a[2])
                    val u = unit(p - b); val v = unit(q - b)
                    val d = (u + v).let { if (it.length < 1e-12) Point(-u.y, u.x) else it }
                    Line(b, b + d)
                }
                "Tangent" -> { need(2)
                    val p = point(a[0]); val circle = a[1] as? Circle ?: throw GeometryError("Tangent(A, c): c is a circle")
                    tangents(p, circle).let { it.singleOrNull() ?: Many(it) }
                }
                "Centroid" -> { need(1)
                    when (val o = a[0]) { is Polygon -> centroid(o.points); else -> throw GeometryError("Expected a polygon") }
                }
                "Incircle" -> { need(3); incircle(point(a[0]), point(a[1]), point(a[2])) }
                "Distance" -> { need(2)
                    val p = point(a[0])
                    Number(when (val o = a[1]) {
                        is Point -> (o - p).length
                        is Line, is Segment, is Ray -> distanceTo(p, o)
                        is Circle -> abs((p - o.center).length - o.r)
                        else -> throw GeometryError("Distance to a point, a line or a circle")
                    }, Unit.Length)
                }
                "Length" -> { need(1)
                    Number(when (val o = a[0]) {
                        is Segment -> (o.b - o.a).length
                        is Vector -> (o.b - o.a).length
                        is Point -> o.length
                        is Polygon -> perimeter(o.points)
                        is Circle -> 2 * Math.PI * o.r
                        else -> throw GeometryError("Length of a segment, vector or polygon")
                    }, Unit.Length)
                }
                "Perimeter" -> { need(1)
                    Number(when (val o = a[0]) {
                        is Polygon -> perimeter(o.points)
                        is Circle -> 2 * Math.PI * o.r
                        else -> throw GeometryError("Perimeter of a polygon or circle")
                    }, Unit.Length)
                }
                "Area" -> {
                    if (a.size >= 3) Number(abs(signedArea(a.map { point(it) })), Unit.Area)
                    else { need(1)
                        Number(when (val o = a[0]) {
                            is Polygon -> abs(signedArea(o.points))
                            is Circle -> Math.PI * o.r * o.r
                            else -> throw GeometryError("Area of a polygon or circle")
                        }, Unit.Area)
                    }
                }
                "Angle" -> { need(3)
                    val p = point(a[0]); val b = point(a[1]); val q = point(a[2])
                    if (p == b || q == b) throw GeometryError("The angle's arms have no length")
                    val s = atan2(p.y - b.y, p.x - b.x); val e = atan2(q.y - b.y, q.x - b.x)
                    var sweep = e - s
                    while (sweep < 0) sweep += 2 * Math.PI
                    while (sweep >= 2 * Math.PI) sweep -= 2 * Math.PI
                    Angle(b, s, sweep)
                }
                "Slope" -> { need(1)
                    val d = direction(a[0])
                    if (abs(d.x) < 1e-15) throw GeometryError("A vertical line has no slope")
                    Number(d.y / d.x)
                }
                "Radius" -> { need(1); Number((a[0] as? Circle ?: throw GeometryError("Expected a circle")).r, Unit.Length) }
                "Reflect" -> { need(2)
                    when (val m = a[1]) {
                        is Point -> transform(a[0], scale = -1.0) { p -> m + m - p }
                        else -> { val l = asLine(m); transform(a[0]) { p -> mirror(p, l.first, l.second) } }
                    }
                }
                "Rotate" -> { need(2, 3)
                    val t = angle(a[1]); val c0 = if (a.size == 3) point(a[2]) else Point(0.0, 0.0)
                    transform(a[0]) { p -> rotate(p, c0, t) }
                }
                "Translate" -> { need(2)
                    val v = when (val o = a[1]) { is Vector -> o.b - o.a; is Point -> o; else -> throw GeometryError("Translate by a vector, or a pair (dx, dy)") }
                    transform(a[0]) { p -> p + v }
                }
                "Dilate" -> { need(2, 3)
                    val k = num(a[1]); val c0 = if (a.size == 3) point(a[2]) else Point(0.0, 0.0)
                    transform(a[0], scale = k) { p -> c0 + (p - c0).times(k) }
                }
                else -> throw GeometryError("${c.name} isn't available yet")
            }
        }
    }

    // ---- Geometry ------------------------------------------------------------------------

    /** A number's value (a length, an area, an angle), or null for shapes. */
    fun value(o: Obj): Double? = when (o) {
        is Number -> o.value
        is Angle -> o.sweep
        else -> null
    }

    private fun mid(p: Point, q: Point) = Point((p.x + q.x) / 2, (p.y + q.y) / 2)
    private fun unit(p: Point): Point { val l = p.length; if (l < 1e-300) throw GeometryError("The points are the same"); return Point(p.x / l, p.y / l) }

    /** A line-like object's two defining points. */
    private fun asLine(o: Obj): Pair<Point, Point> = when (o) {
        is Line -> o.a to o.b
        is Segment -> o.a to o.b
        is Ray -> o.a to o.b
        is Vector -> o.a to o.b
        else -> throw GeometryError("Expected a line, segment or ray")
    }

    private fun direction(o: Obj): Point = when (o) {
        is Point -> o
        else -> asLine(o).let { (p, q) -> unit(q - p) }
    }

    private fun circumcircle(a: Point, b: Point, c: Point): Circle {
        val d = 2 * (a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y))
        if (abs(d) < 1e-12) throw GeometryError("The three points are on one line")
        val a2 = a.x * a.x + a.y * a.y; val b2 = b.x * b.x + b.y * b.y; val c2 = c.x * c.x + c.y * c.y
        val center = Point((a2 * (b.y - c.y) + b2 * (c.y - a.y) + c2 * (a.y - b.y)) / d, (a2 * (c.x - b.x) + b2 * (a.x - c.x) + c2 * (b.x - a.x)) / d)
        return Circle(center, (a - center).length)
    }

    private fun incircle(a: Point, b: Point, c: Point): Circle {
        val la = (b - c).length; val lb = (c - a).length; val lc = (a - b).length
        val p = la + lb + lc
        val area = abs(signedArea(listOf(a, b, c)))
        if (area < 1e-12) throw GeometryError("The three points are on one line")
        return Circle(Point((la * a.x + lb * b.x + lc * c.x) / p, (la * a.y + lb * b.y + lc * c.y) / p), 2 * area / p)
    }

    fun signedArea(pts: List<Point>): Double = pts.indices.sumOf { k -> val p = pts[k]; val q = pts[(k + 1) % pts.size]; p.x * q.y - q.x * p.y } / 2

    private fun perimeter(pts: List<Point>) = pts.indices.sumOf { k -> (pts[(k + 1) % pts.size] - pts[k]).length }

    private fun centroid(pts: List<Point>): Point {
        val a = signedArea(pts)
        if (abs(a) < 1e-12) return Point(pts.sumOf { it.x } / pts.size, pts.sumOf { it.y } / pts.size)
        var cx = 0.0; var cy = 0.0
        for (k in pts.indices) {
            val p = pts[k]; val q = pts[(k + 1) % pts.size]
            val cross = p.x * q.y - q.x * p.y
            cx += (p.x + q.x) * cross; cy += (p.y + q.y) * cross
        }
        return Point(cx / (6 * a), cy / (6 * a))
    }

    private fun distanceTo(p: Point, o: Obj): Double {
        val (a, b) = asLine(o)
        val d = b - a
        val len2 = d.x * d.x + d.y * d.y
        var t = ((p.x - a.x) * d.x + (p.y - a.y) * d.y) / len2
        t = when (o) { is Segment -> t.coerceIn(0.0, 1.0); is Ray -> maxOf(t, 0.0); else -> t }
        return (p - (a + d.times(t))).length
    }

    private fun mirror(p: Point, a: Point, b: Point): Point {
        val d = b - a
        val t = ((p.x - a.x) * d.x + (p.y - a.y) * d.y) / (d.x * d.x + d.y * d.y)
        val foot = a + d.times(t)
        return foot + foot - p
    }

    private fun rotate(p: Point, c: Point, t: Double): Point {
        val q = p - c
        return Point(c.x + q.x * cos(t) - q.y * sin(t), c.y + q.x * sin(t) + q.y * cos(t))
    }

    /** [o] with every point moved by [f]; a circle's radius scaled by |[scale]|. */
    private fun transform(o: Obj, scale: Double = 1.0, f: (Point) -> Point): Obj = when (o) {
        is Point -> f(o)
        is Line -> Line(f(o.a), f(o.b))
        is Segment -> Segment(f(o.a), f(o.b))
        is Ray -> Ray(f(o.a), f(o.b))
        is Vector -> Vector(f(o.a), f(o.b))
        is Circle -> Circle(f(o.center), o.r * abs(scale))
        is Polygon -> Polygon(o.points.map(f))
        is Angle -> o
        is Many -> Many(o.items.map { transform(it, scale, f) })
        is Number -> throw GeometryError("A number can't be moved")
    }

    private fun tangents(p: Point, c: Circle): List<Obj> {
        val d = (p - c.center).length
        if (d < c.r - 1e-12) return emptyList()
        val base = atan2(p.y - c.center.y, p.x - c.center.x)
        if (abs(d - c.r) < 1e-9) { val n = p - c.center; return listOf(Line(p, p + Point(-n.y, n.x))) }
        val alpha = kotlin.math.acos(c.r / d)
        return listOf(base + alpha, base - alpha).map { t -> Line(p, Point(c.center.x + c.r * cos(t), c.center.y + c.r * sin(t))) }
    }

    // ---- Intersections -------------------------------------------------------------------

    /** The straight pieces and circles an object is made of. */
    private fun pieces(o: Obj): List<Obj> = when (o) {
        is Line, is Segment, is Ray, is Vector, is Circle -> listOf(o)
        is Polygon -> o.points.indices.map { k -> Segment(o.points[k], o.points[(k + 1) % o.points.size]) }
        is Many -> o.items.flatMap { pieces(it) }
        else -> throw GeometryError("Intersect lines, segments, rays, circles or polygons")
    }

    /** Where [a] and [b] cross, in a steady order (left to right, then bottom to top), duplicates removed. */
    fun intersections(a: Obj, b: Obj): List<Point> {
        val out = ArrayList<Point>()
        for (p in pieces(a)) for (q in pieces(b)) out += cross(p, q)
        val unique = ArrayList<Point>()
        for (p in out) if (unique.none { (it - p).length < 1e-9 }) unique += p
        return unique.sortedWith(compareBy({ Math.round(it.x * 1e9) }, { it.y }))
    }

    /** Whether parameter [t] along a straight piece lies on it (all of a line; t ≥ 0 on a ray; 0 ≤ t ≤ 1 on a segment). */
    private fun onPiece(o: Obj, t: Double) = when (o) {
        is Segment, is Vector -> t >= -1e-9 && t <= 1 + 1e-9
        is Ray -> t >= -1e-9
        else -> true
    }

    private fun cross(p: Obj, q: Obj): List<Point> = when {
        p is Circle && q is Circle -> circles(p, q)
        p is Circle -> cross(q, p)
        q is Circle -> {
            val (a, b) = asLine(p)
            val d = b - a; val f = a - q.center
            val A = d.x * d.x + d.y * d.y; val B = 2 * (f.x * d.x + f.y * d.y); val C = f.x * f.x + f.y * f.y - q.r * q.r
            val disc = B * B - 4 * A * C
            when {
                disc < -1e-12 * A * q.r * q.r -> emptyList()
                abs(disc) <= 1e-12 * A * q.r * q.r -> listOf(-B / (2 * A))
                else -> listOf((-B - sqrt(disc)) / (2 * A), (-B + sqrt(disc)) / (2 * A))
            }.filter { onPiece(p, it) }.map { a + d.times(it) }
        }
        else -> {
            val (a, b) = asLine(p); val (c, e) = asLine(q)
            val r = b - a; val s = e - c
            val den = r.x * s.y - r.y * s.x
            if (abs(den) < 1e-12 * r.length * s.length) emptyList()
            else {
                val t = ((c.x - a.x) * s.y - (c.y - a.y) * s.x) / den
                val u = ((c.x - a.x) * r.y - (c.y - a.y) * r.x) / den
                if (onPiece(p, t) && onPiece(q, u)) listOf(a + r.times(t)) else emptyList()
            }
        }
    }

    private fun circles(c1: Circle, c2: Circle): List<Point> {
        val d = (c2.center - c1.center).length
        if (d < 1e-12 || d > c1.r + c2.r + 1e-9 || d < abs(c1.r - c2.r) - 1e-9) return emptyList()
        val a = (c1.r * c1.r - c2.r * c2.r + d * d) / (2 * d)
        val h = sqrt(maxOf(0.0, c1.r * c1.r - a * a))
        val u = (c2.center - c1.center).times(1 / d)
        val m = c1.center + u.times(a)
        if (h < 1e-9) return listOf(m)
        return listOf(Point(m.x - u.y * h, m.y + u.x * h), Point(m.x + u.y * h, m.y - u.x * h))
    }

    // ---- Drawing -------------------------------------------------------------------------

    /**
     * What to draw for an object in a view: polylines (in graph coordinates), points, a polygon
     * to fill, arrowheads, and labels (an angle's value at its arc).
     */
    class Drawing(
        val lines: List<List<Pair<Double, Double>>> = emptyList(),
        val points: List<Point> = emptyList(),
        val fill: List<Pair<Double, Double>>? = null,
        val arrow: Boolean = false,
        val labels: List<Pair<Point, String>> = emptyList(),
    )

    fun draw(o: Obj, view: Viewport, angleText: (Double) -> String = { "%.1f°".format(Math.toDegrees(it)) }): Drawing {
        // Far enough to cross the whole view from anywhere in it.
        val far = 4 * (abs(view.xMax - view.xMin) + abs(view.yMax - view.yMin)) + hypot(view.xMin + view.xMax, view.yMin + view.yMax)
        fun pt(p: Point) = p.x to p.y
        return when (o) {
            is Point -> Drawing(points = listOf(o))
            is Segment -> Drawing(lines = listOf(listOf(pt(o.a), pt(o.b))))
            is Vector -> Drawing(lines = listOf(listOf(pt(o.a), pt(o.b))), arrow = true)
            is Line -> {
                val d = unit(o.b - o.a)
                // Centered on the point of the line nearest the view's middle, so it reaches both edges.
                val mid = Point((view.xMin + view.xMax) / 2, (view.yMin + view.yMax) / 2)
                val t = (mid.x - o.a.x) * d.x + (mid.y - o.a.y) * d.y
                val c = o.a + d.times(t)
                Drawing(lines = listOf(listOf(pt(c + d.times(-far)), pt(c + d.times(far)))))
            }
            is Ray -> Drawing(lines = listOf(listOf(pt(o.a), pt(o.a + unit(o.b - o.a).times(far)))))
            is Circle -> {
                val n = 240
                Drawing(lines = listOf((0..n).map { k -> val t = 2 * Math.PI * k / n; (o.center.x + o.r * cos(t)) to (o.center.y + o.r * sin(t)) }))
            }
            is Polygon -> {
                val path = o.points.map { pt(it) }
                Drawing(lines = listOf(path + path.first()), fill = path)
            }
            is Angle -> {
                // An arc a fraction of the view across, and the value just outside it.
                val r = 0.06 * minOf(abs(view.xMax - view.xMin), abs(view.yMax - view.yMin))
                val n = maxOf(8, (o.sweep / (2 * Math.PI) * 96).toInt())
                val arc = (0..n).map { k -> val t = o.start + o.sweep * k / n; (o.vertex.x + r * cos(t)) to (o.vertex.y + r * sin(t)) }
                val midA = o.start + o.sweep / 2
                val wedge = listOf(pt(o.vertex)) + arc
                Drawing(lines = listOf(arc), fill = wedge, labels = listOf(Point(o.vertex.x + 1.9 * r * cos(midA), o.vertex.y + 1.9 * r * sin(midA)) to angleText(o.sweep)))
            }
            is Many -> o.items.map { draw(it, view, angleText) }.let { ds ->
                Drawing(lines = ds.flatMap { it.lines }, points = ds.flatMap { it.points }, labels = ds.flatMap { it.labels })
            }
            is Number -> Drawing()
        }
    }

    /** The first point name not taken: A, B, …, Z, then A₁, B₁, … */
    fun nextPointName(used: Set<String>): String {
        for (c in 'A'..'Z') if (c.toString() !in used) return c.toString()
        var k = 1
        while (true) {
            for (c in 'A'..'Z') {
                val name = com.example.cas.cas.CustomSymbol(c.toString(), sub = k.toString()).encode()
                if (name !in used) return name
            }
            k++
        }
    }
}
