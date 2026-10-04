package com.example.cas.graph

import com.example.cas.editor.Func
import com.example.cas.editor.MathRow
import com.example.cas.editor.Node
import com.example.cas.editor.Sym
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geometry mode in the 2D graph (alpha): constructions, named points, and objects built from
 * them with commands, each on its own line of the list:
 *
 *     A = (1, 2)              a free point (drag it on the graph)
 *     c = Circle(A, B)        the circle about A through B
 *     P = Point(c, 0.25)      a point on c (drag it round the circle)
 *     Q = Intersect(c, l, 1)
 *     d = Distance(A, B) / 2  numbers, with arithmetic mixed in
 *     M = (A + B) / 2         points and vectors add, subtract and scale
 *
 * A line is read from the editor's row: names (a letter, or a symbol from the symbol builder),
 * command words (typed as one word or letter by letter, any case), brackets, commas and
 * arithmetic. What isn't geometry (a constant, sin of a number) is worked out by the app
 * ([build]'s `number`), so sliders and fractions work inside commands. Objects can use objects
 * from any other line, above or below, and functions f(x) defined on the graph's own lines.
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

    /** Part of a circle, counterclockwise from [start] through [sweep] (radians); a [sector] is filled to the center. */
    data class Arc(val center: Point, val r: Double, val start: Double, val sweep: Double, val sector: Boolean = false) : Obj()

    /**
     * A conic Ax² + Bxy + Cy² + Dx + Ey + F = 0 (coefficients in [c]): an ellipse, hyperbola or
     * parabola, named by [kind] as it was made.
     */
    class Conic(val c: DoubleArray, val kind: String) : Obj() {
        val shape: ConicShape by lazy { classify(c) }
        override fun equals(other: Any?) = other is Conic && other.c.contentEquals(c)
        override fun hashCode() = c.contentHashCode()
    }

    /** A curve given by its points: a locus (several pieces where it breaks off). */
    data class Polyline(val pieces: List<List<Point>>) : Obj()

    /** A sentence about objects: Relation(a, b). */
    data class Text(val value: String) : Obj()

    /** A yes or no: AreParallel(l, m), AreCollinear(A, B, C)… */
    data class Bool(val value: Boolean) : Obj()

    /** The graph of a function f(x) from the graph's own lines, for intersecting and tangents. */
    class FunctionGraph(val name: String, val f: (Double) -> Double) : Obj()

    /** A function of z defined on the complex plane's lines (w = f(z)): it maps points (Image(f, c)). */
    class ComplexMap(val name: String, val f: (Point) -> Point?) : Obj()

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

    /** The row isn't a construction after all. */
    private class NotGeometry : Exception()

    // ---- Statements ------------------------------------------------------------------------

    sealed class Ex {
        class Ref(val name: String) : Ex()
        /** A command and its arguments, with each argument's nodes as typed (a point on a path rewrites its parameter). */
        class Call(val command: Command, val args: List<Ex>, val argNodes: List<List<Node>>) : Ex()
        class Pair(val x: Ex, val y: Ex) : Ex()
        /** (x, y, z): a point in space. */
        class Triple(val x: Ex, val y: Ex, val z: Ex) : Ex()
        class Lit(val value: Double) : Ex()
        /** Something only the app can work out (π, a constant, a fraction of plain numbers). */
        class Delegate(val nodes: List<Node>) : Ex()
        class Bin(val op: Char, val a: Ex, val b: Ex) : Ex()
        class Neg(val a: Ex) : Ex()
        /** x(A) or y(A): a coordinate. */
        class Coordinate(val which: Char, val of: Ex) : Ex()
        /** A function key (sin, ln…) with geometry inside: its arguments worked out, then the function. */
        class Fn(val name: String, val args: List<Ex>) : Ex()
        class Sqrt(val a: Ex) : Ex()
        class Root(val a: Ex, val index: Ex) : Ex()
        /** A number marked with ° (radians inside). */
        class Degrees(val a: Ex) : Ex()

        /** Every reference and call inside, for deciding what a row is. */
        fun walk(visit: (Ex) -> kotlin.Unit) {
            visit(this)
            when (this) {
                is Call -> args.forEach { it.walk(visit) }
                is Pair -> { x.walk(visit); y.walk(visit) }
                is Triple -> { x.walk(visit); y.walk(visit); z.walk(visit) }
                is Bin -> { a.walk(visit); b.walk(visit) }
                is Neg -> a.walk(visit)
                is Coordinate -> of.walk(visit)
                is Fn -> args.forEach { it.walk(visit) }
                is Sqrt -> a.walk(visit)
                is Root -> { a.walk(visit); index.walk(visit) }
                is Degrees -> a.walk(visit)
                else -> {}
            }
        }
    }

    /** A line of the list read as a construction: its name (if it has "name =") and what it is. */
    class Statement(val name: String?, val expr: Ex) {
        /** A point written with plain numbers (A = (1, 2)): it can be dragged. */
        val isFree get() = name != null && expr is Ex.Pair && plain(expr.x) != null && plain(expr.y) != null

        /** A point on a path (P = Point(c, 0.3)): dragged along it, its parameter rewritten. */
        val onPath get() = name != null && expr is Ex.Call && expr.command.name == "Point" &&
            (expr.args.size == 1 || expr.args.size == 2 && plain(expr.args[1]) != null)

        /** The nodes of the path a point is on, as typed (for rewriting it). */
        val pathNodes: List<Node>? get() = (expr as? Ex.Call)?.takeIf { onPath }?.argNodes?.firstOrNull()

        private fun plain(e: Ex): Double? = when (e) {
            is Ex.Lit -> e.value
            is Ex.Neg -> plain(e.a)?.let { -it }
            else -> null
        }
    }

    /** A command: its name, how it's written, and what it makes. */
    class Command(val name: String, val usage: String, val help: String, val aliases: List<String> = emptyList())

    val COMMANDS = listOf(
        Command("Point", "Point(c, t)", "A point on a line, circle, conic, polygon or function graph, at t along it (drag it along)"),
        Command("Segment", "Segment(A, B)", "The segment from A to B; Segment(A, a) of length a to the right of A"),
        Command("Line", "Line(A, B)", "The line through A and B; Line(A, l) is the line through A parallel to l"),
        Command("Ray", "Ray(A, B)", "The ray from A through B"),
        Command("Vector", "Vector(A, B)", "The arrow from A to B"),
        Command("Circle", "Circle(A, B)", "The circle about A through B; Circle(A, r) with radius r; Circle(A, B, C) through three points"),
        Command("Semicircle", "Semicircle(A, B)", "The half circle on AB, counterclockwise from A"),
        Command("CircularArc", "CircularArc(O, A, B)", "The arc about O from A counterclockwise to the ray OB", listOf("Arc")),
        Command("CircumcircularArc", "CircumcircularArc(A, B, C)", "The arc from A through B to C"),
        Command("CircularSector", "CircularSector(O, A, B)", "The sector about O from A counterclockwise to the ray OB", listOf("Sector")),
        Command("Polygon", "Polygon(A, B, C)", "The polygon with these corners (three or more)"),
        Command("Polyline", "Polyline(A, B, C)", "The open path through these points in order"),
        Command("CircumcircularSector", "CircumcircularSector(A, B, C)", "The sector of the circle through A, B and C, from A through B to C"),
        Command("RegularPolygon", "RegularPolygon(A, B, n)", "The regular polygon with n sides on AB, counterclockwise"),
        Command("Ellipse", "Ellipse(F, G, a)", "The ellipse with foci F and G and semi-major axis a (or through a point)"),
        Command("Hyperbola", "Hyperbola(F, G, a)", "The hyperbola with foci F and G and semi-major axis a (or through a point)"),
        Command("Parabola", "Parabola(F, l)", "The parabola with focus F and directrix l"),
        Command("Conic", "Conic(A, B, C, D, E)", "The conic through five points"),
        Command("Midpoint", "Midpoint(A, B)", "The point halfway from A to B (or the middle of a segment, the center of a circle)"),
        Command("Center", "Center(c)", "The center of a circle, arc or conic"),
        Command("Intersect", "Intersect(a, b)", "Where two objects cross (lines, circles, conics, polygons, arcs, function graphs); Intersect(a, b, n) picks the nth point"),
        Command("PerpendicularLine", "PerpendicularLine(A, l)", "The line through A at right angles to l", listOf("Perpendicular")),
        Command("ParallelLine", "ParallelLine(A, l)", "The line through A parallel to l", listOf("Parallel")),
        Command("PerpendicularBisector", "PerpendicularBisector(A, B)", "The line at right angles to AB through its middle"),
        Command("AngleBisector", "AngleBisector(A, B, C)", "The line through B halving the angle ABC"),
        Command("Tangent", "Tangent(A, c)", "The tangents from A to a circle or conic, or to a function graph at x = x(A)"),
        Command("Centroid", "Centroid(poly)", "The center of mass of a polygon (or of its corners)"),
        Command("Incircle", "Incircle(A, B, C)", "The circle inside the triangle ABC touching its sides"),
        Command("Circumcenter", "Circumcenter(A, B, C)", "The center of the circle through A, B and C"),
        Command("Orthocenter", "Orthocenter(A, B, C)", "Where the triangle's altitudes meet"),
        Command("Incenter", "Incenter(A, B, C)", "The center of the circle inside the triangle"),
        Command("Vertex", "Vertex(p, n)", "The nth corner of a polygon, or a conic's vertices"),
        Command("Foci", "Foci(c)", "An ellipse's or hyperbola's two foci, or a parabola's focus", listOf("Focus")),
        Command("Asymptote", "Asymptote(h)", "A hyperbola's two asymptotes"),
        Command("Directrix", "Directrix(p)", "A parabola's directrix"),
        Command("Polar", "Polar(A, c)", "The polar line of A with respect to a circle or conic"),
        Command("Locus", "Locus(P, Q)", "The curve P traces as Q, a point on an object, moves along it"),
        Command("FitLine", "FitLine(A, B, C)", "The best-fitting line through points (least squares)"),
        Command("Root", "Root(f)", "Where a function's graph crosses the x-axis (in and around the view)"),
        Command("Extremum", "Extremum(f)", "A function's highest and lowest turning points (in and around the view)"),
        Command("Relation", "Relation(a, b)", "How two objects relate: equal, parallel, perpendicular, on each other…"),
        Command("ClosestPoint", "ClosestPoint(c, A)", "The point of a line, circle, polygon, conic or function graph nearest A"),
        Command("Inflection", "Inflection(f)", "Where a function's graph changes from bending one way to the other (in and around the view)", listOf("InflectionPoint")),
        Command("CommonTangent", "CommonTangent(c, d)", "The lines touching both circles (up to four)", listOf("CommonTangents")),
        Command("NinePointCircle", "NinePointCircle(A, B, C)", "The circle through the midpoints of the triangle's sides (and the feet of its altitudes)"),
        Command("EulerLine", "EulerLine(A, B, C)", "The line through the triangle's circumcenter, centroid and orthocenter"),
        Command("Excircle", "Excircle(A, B, C)", "The circle outside the triangle touching side BC and the other two sides extended"),
        Command("TriangleCenter", "TriangleCenter(A, B, C, n)", "Kimberling's nth center: 1 incenter, 2 centroid, 3 circumcenter, 4 orthocenter, 5 nine-point center, 6 symmedian point, 7 Gergonne point, 8 Nagel point"),
        Command("MajorAxis", "MajorAxis(c)", "The line along an ellipse's or hyperbola's major axis"),
        Command("MinorAxis", "MinorAxis(c)", "The line along an ellipse's or hyperbola's minor axis"),
        Command("UnitVector", "UnitVector(v)", "The vector of length 1 along a vector or line"),
        Command("PerpendicularVector", "PerpendicularVector(v)", "The vector at right angles to v, as long as it", listOf("OrthogonalVector")),
        Command("UnitPerpendicularVector", "UnitPerpendicularVector(v)", "The vector of length 1 at right angles to v or a line", listOf("UnitOrthogonalVector")),
        Command("Direction", "Direction(l)", "A line's direction, as a vector"),
        Command("Conjugate", "Conjugate(z)", "The mirror image in the real axis: a − bi for a + bi (complex plane); also of shapes"),
        Command("Modulus", "Modulus(z)", "|z|: how far the point is from 0 (complex plane)"),
        Command("Argument", "Argument(z)", "arg z: the angle from the positive real axis (complex plane)"),
        Command("RootsOfUnity", "RootsOfUnity(n)", "The n points with zⁿ = 1, on the unit circle (complex plane)"),
        Command("ComplexRoots", "ComplexRoots(z, n)", "The n points w with wⁿ = z (complex plane)", listOf("NthRoots")),
        Command("Image", "Image(f, c)", "Where f(z), defined on the complex plane, takes a point, line, circle or curve (complex plane)"),
        Command("Distance", "Distance(A, B)", "How far apart two points are, or a point and a line or circle"),
        Command("Length", "Length(s)", "The length of a segment, vector or arc, or the perimeter of a polygon"),
        Command("Perimeter", "Perimeter(poly)", "The perimeter of a polygon, or a circle's circumference"),
        Command("Area", "Area(poly)", "The area of a polygon, circle, sector or ellipse"),
        Command("Angle", "Angle(A, B, C)", "The angle at B from A round to C (counterclockwise); Angle(l, m) between two lines or vectors"),
        Command("Slope", "Slope(l)", "The slope of a line or segment"),
        Command("Radius", "Radius(c)", "A circle's or arc's radius"),
        Command("Circumference", "Circumference(c)", "The distance round a circle or ellipse"),
        Command("Eccentricity", "Eccentricity(c)", "How stretched a conic is: 0 a circle, under 1 an ellipse, 1 a parabola, over 1 a hyperbola"),
        Command("LinearEccentricity", "LinearEccentricity(c)", "The distance from a conic's center to a focus"),
        Command("SemiMajorAxisLength", "SemiMajorAxisLength(c)", "Half an ellipse's or hyperbola's major axis (a)"),
        Command("SemiMinorAxisLength", "SemiMinorAxisLength(c)", "Half an ellipse's or hyperbola's minor axis (b)"),
        Command("Dot", "Dot(u, v)", "The dot product of two vectors", listOf("DotProduct")),
        Command("Cross", "Cross(u, v)", "The 2D cross product of two vectors (the signed area they span)", listOf("CrossProduct")),
        Command("AreCongruent", "AreCongruent(a, b)", "Whether two segments, circles, angles or polygons are the same size and shape"),
        Command("AreCollinear", "AreCollinear(A, B, C)", "Whether three points are on one line"),
        Command("AreConcyclic", "AreConcyclic(A, B, C, D)", "Whether four points are on one circle"),
        Command("AreParallel", "AreParallel(l, m)", "Whether two lines are parallel"),
        Command("ArePerpendicular", "ArePerpendicular(l, m)", "Whether two lines are at right angles"),
        Command("AreEqual", "AreEqual(a, b)", "Whether two numbers or points are the same"),
        Command("Reflect", "Reflect(obj, l)", "The mirror image in a line (or through a point)"),
        Command("Rotate", "Rotate(obj, θ, A)", "Turned by θ about A (about the origin without A)"),
        Command("Translate", "Translate(obj, v)", "Moved by the vector v (a vector, or a pair (dx, dy))"),
        Command("Dilate", "Dilate(obj, k, A)", "Scaled by k from A (from the origin without A)"),
    )

    private val byName: Map<String, Command> = COMMANDS.flatMap { c -> (listOf(c.name) + c.aliases).map { it.lowercase() to c } }.toMap()

    fun command(word: String): Command? = byName[word.lowercase()]

    private fun text(n: Node?) = (n as? Sym)?.text

    /**
     * A name: one letter (Latin or Greek), or a symbol from the symbol builder (A₁, P′). Not e,
     * i or π (numbers), nor x and y (coordinates).
     */
    fun isName(n: Node?): Boolean {
        val t = text(n) ?: return false
        if (com.example.cas.cas.CustomSymbol.isCustom(t)) return com.example.cas.cas.CustomSymbol.decode(t) != null
        return t.length == 1 && t[0].isLetter() && t !in setOf("e", "i", "π", "x", "y")
    }

    /** A point's name: a capital letter, or a built symbol on one. */
    fun isPointName(n: Node?): Boolean {
        val t = text(n) ?: return false
        val base = com.example.cas.cas.CustomSymbol.decode(t)?.base ?: t
        return isName(n) && base.firstOrNull()?.isUpperCase() == true
    }

    private fun isPointName(name: String) = isPointName(Sym(name))

    /** The command starting at [from] (one word, or letters spelling it) and how many nodes it takes. */
    private fun commandAt(items: List<Node>, from: Int, command: (String) -> Command? = ::command): kotlin.Pair<Command, Int>? {
        text(items.getOrNull(from))?.let { t -> if (t.length > 1 && t.all { it.isLetter() } && text(items.getOrNull(from + 1)) == "(") command(t)?.let { return it to 1 } }
        val sb = StringBuilder()
        var k = from
        var best: kotlin.Pair<Command, Int>? = null
        while (k < items.size) {
            val t = text(items[k]) ?: break
            if (t.length != 1 || !t[0].isLetter()) break
            sb.append(t); k++
            // Only if a bracket follows: "Ray" spelled out, not the product R·a·y.
            command(sb.toString())?.let { if (text(items.getOrNull(k)) == "(") best = it to (k - from) }
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

    // ---- Reading expressions -------------------------------------------------------------

    /** Reads a row (or part of one) as an expression: + − × / ^, brackets, pairs, calls, names. */
    internal class Reader(val items: List<Node>, val lookup: (String) -> Command? = ::command) {
        var k = 0

        fun all(): Ex {
            val e = sum()
            if (k != items.size) throw NotGeometry()
            return e
        }

        private fun t(at: Int = k) = text(items.getOrNull(at))

        fun sum(): Ex {
            var e = product()
            while (true) {
                e = when (t()) {
                    "+" -> { k++; Ex.Bin('+', e, product()) }
                    "−", "-" -> { k++; Ex.Bin('-', e, product()) }
                    else -> return e
                }
            }
        }

        fun product(): Ex {
            var e = unary()
            while (true) {
                e = when (t()) {
                    "·", "×", "*" -> { k++; Ex.Bin('*', e, unary()) }
                    "/", "÷" -> { k++; Ex.Bin('/', e, unary()) }
                    else -> if (k < items.size && startsAtom()) Ex.Bin('*', e, unary()) else return e
                }
            }
        }

        fun unary(): Ex = when (t()) {
            "−", "-" -> { k++; Ex.Neg(unary()) }
            "+" -> { k++; unary() }
            else -> power()
        }

        fun power(): Ex {
            var e = atom()
            while (true) {
                val n = items.getOrNull(k)
                when {
                    n is com.example.cas.editor.Pow -> { k++; e = Ex.Bin('^', e, Reader(n.exp.items, lookup).all()) }
                    text(n) == "°" -> { k++; e = Ex.Degrees(e) }
                    text(n) == "²" -> { k++; e = Ex.Bin('^', e, Ex.Lit(2.0)) }
                    else -> return e
                }
            }
        }

        fun startsAtom(): Boolean {
            val n = items[k]
            val s = text(n) ?: return true
            return s !in setOf("+", "−", "-", "·", "×", "*", "/", "÷", ")", "]", ",", "=", "°")
        }

        fun atom(): Ex {
            val n = items.getOrNull(k) ?: throw NotGeometry()
            val s = text(n)
            // A command and its arguments.
            commandAt(items, k, lookup)?.let { (cmd, used) ->
                val open = k + used
                val end = closing(items, open)
                if (end < 0) throw NotGeometry()
                val inner = items.subList(open + 1, end)
                val parts = if (inner.isEmpty()) emptyList() else splitArgs(inner)
                k = end + 1
                return Ex.Call(cmd, parts.map { Reader(it, lookup).all() }, parts)
            }
            // x(A), y(A): a coordinate.
            if ((s == "x" || s == "y" || s == "z") && t(k + 1) == "(") {
                val end = closing(items, k + 1)
                if (end < 0) throw NotGeometry()
                val inner = Reader(items.subList(k + 2, end), lookup).all()
                k = end + 1
                return Ex.Coordinate(s[0], inner)
            }
            when {
                s == "(" -> {
                    val end = closing(items, k)
                    if (end < 0) throw NotGeometry()
                    val parts = splitArgs(items.subList(k + 1, end))
                    k = end + 1
                    return when (parts.size) {
                        1 -> Reader(parts[0], lookup).all()
                        2 -> Ex.Pair(Reader(parts[0], lookup).all(), Reader(parts[1], lookup).all())
                        // (x, y, z): a point in space (for 3D geometry).
                        3 -> Ex.Triple(Reader(parts[0], lookup).all(), Reader(parts[1], lookup).all(), Reader(parts[2], lookup).all())
                        else -> throw NotGeometry()
                    }
                }
                s != null && (s[0].isDigit() || s == ".") -> {
                    val sb = StringBuilder()
                    while (k < items.size && text(items[k])?.let { it.length == 1 && (it[0].isDigit() || it == ".") } == true) { sb.append(text(items[k])); k++ }
                    return Ex.Lit(sb.toString().toDoubleOrNull() ?: throw NotGeometry())
                }
                isName(n) -> { k++; return Ex.Ref(s!!) }
                n is com.example.cas.editor.Frac -> { k++; return Ex.Bin('/', Reader(n.num.items, lookup).all(), Reader(n.den.items, lookup).all()) }
                n is com.example.cas.editor.Sqrt -> { k++; return Ex.Sqrt(Reader(n.arg.items, lookup).all()) }
                n is com.example.cas.editor.Root -> { k++; return Ex.Root(Reader(n.arg.items, lookup).all(), Reader(n.index.items, lookup).all()) }
                n is Func -> { k++; return Ex.Fn(n.name, n.args.map { Reader(it.items, lookup).all() }) }
                // Anything else (π, e, a constant): the app works it out.
                else -> { k++; return Ex.Delegate(listOf(n)) }
            }
        }
    }

    /**
     * The row as a construction, or null when it isn't one (a function, an equation, a plain
     * point (2, 3) without a name: the graph's usual lines).
     */
    fun parse(items: List<Node>, complex: Boolean = false): Statement? = parseWith(items, ::command, complex)

    /**
     * [parse] with another set of commands (3D geometry's). On the complex plane [complex] also
     * takes A = 1 + 2i as a point.
     */
    internal fun parseWith(items: List<Node>, lookup: (String) -> Command?, complex: Boolean = false, space: Boolean = false): Statement? {
        if (items.isEmpty()) return null
        val eq = run {
            var depth = 0
            items.indexOfFirst { n -> when (text(n)) { "(", "[" -> depth++; ")", "]" -> depth-- }; depth == 0 && text(n) == "=" }
        }
        val name = if (eq == 1 && isName(items[0])) text(items[0]) else null
        if (eq >= 0 && name == null) return null
        val rhs = if (eq >= 0) items.subList(eq + 1, items.size) else items
        // Equations of x and y (y = 2x, x² + y² = 4) are the graph's own.
        if (rhs.any { text(it) == "=" }) return null
        val expr = try { Reader(rhs, lookup).all() } catch (e: NotGeometry) { return null } catch (e: RuntimeException) { return null }
        var call = false; var capital = false; var coordinate = false
        expr.walk { e ->
            when (e) {
                is Ex.Call -> call = true
                is Ex.Ref -> if (isPointName(e.name)) capital = true
                is Ex.Coordinate -> coordinate = true
                else -> {}
            }
        }
        val geometric = call || coordinate ||
            // A = (1, 2): a point, named with a capital (f = (1, 2) stays the graph's own point).
            name != null && isPointName(items[0]) && (if (space) expr is Ex.Triple else expr is Ex.Pair) ||
            // A = 1 + 2i: a point of the complex plane, written as a number.
            complex && name != null && isPointName(items[0]) && rhs.any { text(it) == "i" } ||
            // v = B − A, M = (A + B)/2: arithmetic on points (but y = A sin x keeps its slider A).
            capital && rhs.none { text(it) == "x" || text(it) == "y" || (space && text(it) == "z") } && name != null
        return if (geometric) Statement(name, expr) else null
    }

    /** A plain number written in [nodes] (digits, a point, a minus), or null. */
    fun plainNumber(nodes: List<Node>): Double? {
        if (nodes.isEmpty() || !nodes.all { it is Sym }) return null
        return nodes.joinToString("") { (it as Sym).text }.replace("−", "-").toDoubleOrNull()
    }

    // ---- Working a construction out ------------------------------------------------------

    /** What a line came to: its object, or why it couldn't be made; for a point on a path, the path (to drag it along). */
    class Outcome(val obj: Obj?, val error: String?, val path: Obj? = null)

    /**
     * Works out every statement (null for lines that aren't constructions), each with the objects
     * named on the other lines. [number] works out what only the app can (π, a slider) from its
     * nodes, given the numbers named so far; [degrees] is how a typed angle (Rotate) is read;
     * [functionOf] finds a function f(x) defined on the graph's lines, and [xRange] is where its
     * graph is searched (for crossings).
     */
    fun build(
        statements: List<Statement?>, number: (List<Node>, Map<String, Double>) -> Double, degrees: Boolean = false,
        /** Called with each statement's index just before it's worked out (to know whose sliders a number makes). */
        onStatement: (Int) -> kotlin.Unit = {},
        functionOf: (String) -> ((Double) -> Double)? = { null },
        xRange: ClosedFloatingPointRange<Double> = -50.0..50.0,
        /** Objects to use instead of what their lines say (a locus moving its point along). */
        overrides: Map<String, Obj> = emptyMap(),
        /** Inside a locus's own runs, loci aren't worked out again. */
        loci: Boolean = true,
        /** On the complex plane: points are numbers (A = 1 + 2i, A·B, A/B, √A), with the complex commands. */
        complex: Boolean = false,
        /** Functions of z defined on the complex plane's lines (for Image and f(A)). */
        complexFunctionOf: (String) -> ((Point) -> Point?)? = { null },
    ): List<Outcome?> {
        val out = arrayOfNulls<Outcome>(statements.size)
        val env = LinkedHashMap<String, Obj>()
        val defined = statements.mapNotNull { it?.name }.toSet()
        val waiting = statements.indices.filter { statements[it] != null }.toMutableList()
        // As many passes as lines, so each can use a line below it; what's left waits on itself.
        // Loci run the whole construction again, so they wait for a pass where nothing else can be done.
        var lociTurn = false
        repeat(2 * statements.size + 2) {
            val before = waiting.size
            val it2 = waiting.iterator()
            while (it2.hasNext()) {
                val k = it2.next()
                val s = statements[k]!!
                try {
                    onStatement(k)
                    // A locus waits until everything else is worked out (it runs the whole construction again).
                    if (loci && isLocus(s.expr) && !lociTurn) continue
                    val locus = { traced: String, mover: String ->
                        if (!loci) throw GeometryError("A locus can't use another locus")
                        locusOf(statements, out, traced, mover, number, degrees, functionOf, xRange, complex, complexFunctionOf)
                    }
                    val ev = Evaluation(env, defined, number, degrees, functionOf, xRange, locus, complex, complexFunctionOf)
                    val obj = s.name?.let { overrides[it] } ?: ev.obj(s.expr)
                    out[k] = Outcome(obj, null, if (s.onPath) ev.obj((s.expr as Ex.Call).args[0]) else null)
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
            // Nothing more without the loci: their turn. After they've had it, nothing at all: stop.
            if (waiting.size == before) { if (lociTurn || waiting.none { j -> isLocus(statements[j]!!.expr) }) return@repeat; lociTurn = true }
            else lociTurn = false
        }
        waiting.forEach { k -> out[k] = Outcome(null, "This depends on itself") }
        return out.toList()
    }

    private fun isLocus(e: Ex) = e is Ex.Call && e.command.name == "Locus"

    /**
     * The curve [traced] makes as [mover] (a point on a path) runs along its path: the whole
     * construction worked out again for each place, the mover put there.
     */
    private fun locusOf(
        statements: List<Statement?>, done: Array<Outcome?>, traced: String, mover: String,
        number: (List<Node>, Map<String, Double>) -> Double, degrees: Boolean,
        functionOf: (String) -> ((Double) -> Double)?, xRange: ClosedFloatingPointRange<Double>,
        complex: Boolean = false, complexFunctionOf: (String) -> ((Point) -> Point?)? = { null },
    ): Obj {
        val k = statements.indexOfFirst { it?.name == mover }
        if (k < 0) throw GeometryError("$mover isn't defined")
        if (statements[k]?.onPath != true) throw GeometryError("$mover must be a point on an object, like $mover = Point(c, 0.5)")
        val path = done[k]?.path ?: throw Missing(mover)
        val tracedAt = statements.indexOfFirst { it?.name == traced }
        if (tracedAt < 0) throw GeometryError("$traced isn't defined")
        // How far the parameter runs: once round a closed path; along a line, a function or an open conic, across the view.
        val span = xRange.endInclusive - xRange.start
        val (lo, hi) = when (path) {
            is Line, is Ray -> asLine(path).let { (a, b) -> val step = maxOf((b - a).length, 1e-9); (if (path is Ray) 0.0 else -span / step) to span / step }
            is FunctionGraph -> xRange.start to xRange.endInclusive
            is Conic -> when (path.shape) { is ConicShape.Ellipse -> 0.0 to 1.0; is ConicShape.Hyperbola -> -1.999 to 1.999; else -> -0.995 to 0.995 }
            else -> 0.0 to 1.0
        }
        val n = 240
        val pieces = ArrayList<MutableList<Point>>()
        var current = ArrayList<Point>()
        var last: Point? = null
        for (i in 0..n) {
            val t = lo + (hi - lo) * i / n
            val at = runCatching { pointAt(path, t) }.getOrNull()
            val p = at?.let { place ->
                val outcomes = build(statements, number, degrees, functionOf = functionOf, xRange = xRange, overrides = mapOf(mover to place), loci = false, complex = complex, complexFunctionOf = complexFunctionOf)
                outcomes[tracedAt]?.obj as? Point
            }
            // A break where the point isn't defined, or jumps a long way (across a branch).
            val jump = p != null && last != null && (p - last).length > span / 4
            if (p == null || jump || (path is Conic && path.shape is ConicShape.Hyperbola && i > 0 && (t >= 0) != (lo + (hi - lo) * (i - 1) / n >= 0))) {
                if (current.size > 1) pieces += current
                current = ArrayList()
            }
            if (p != null) current += p
            last = p
        }
        if (current.size > 1) pieces += current
        if (pieces.isEmpty()) throw GeometryError("$traced doesn't move with $mover")
        return Polyline(pieces)
    }

    /** A number as a row of digits (for a function key whose arguments were worked out here). */
    private fun digits(v: Double): MathRow {
        val s = java.math.BigDecimal(v).round(java.math.MathContext(17)).stripTrailingZeros().toPlainString()
        val row = MathRow()
        s.forEach { ch -> row.add(row.items.size, Sym(if (ch == '-') "−" else ch.toString())) }
        return row
    }

    private class Evaluation(
        val env: Map<String, Obj>, val defined: Set<String>, val numberOf: (List<Node>, Map<String, Double>) -> Double,
        val degrees: Boolean, val functionOf: (String) -> ((Double) -> Double)?, val xRange: ClosedFloatingPointRange<Double>,
        val locus: (String, String) -> Obj = { _, _ -> throw GeometryError("Locus isn't available here") },
        val complex: Boolean = false,
        val complexFunctionOf: (String) -> ((Point) -> Point?)? = { null },
    ) {
        fun numbers(): Map<String, Double> = env.mapNotNull { (k, v) -> value(v)?.let { k to it } }.toMap()

        fun obj(e: Ex): Obj = when (e) {
            // A capital is a point's name, so an unknown one is missing (not a new slider, as a or k is).
            is Ex.Ref -> env[e.name] ?: when {
                e.name in defined || isPointName(e.name) -> throw Missing(e.name)
                complex -> complexFunctionOf(e.name)?.let { ComplexMap(e.name, it) } ?: Number(numberOf(listOf(Sym(e.name)), numbers()))
                else -> functionOf(e.name)?.let { FunctionGraph(e.name, it) } ?: Number(numberOf(listOf(Sym(e.name)), numbers()))
            }
            is Ex.Pair -> Point(num(obj(e.x)), num(obj(e.y)))
            is Ex.Triple -> throw GeometryError("A point in space (x, y, z) is for 3D graphs")
            is Ex.Lit -> Number(e.value)
            // On the complex plane, i is the point (0, 1).
            is Ex.Delegate -> if (complex && e.nodes.singleOrNull()?.let { (it as? Sym)?.text } == "i") Point(0.0, 1.0) else Number(numberOf(e.nodes, numbers()))
            // A locus names its points rather than taking their values.
            is Ex.Call -> if (e.command.name == "Locus") call(e.command, listOf(Number(0.0), Number(0.0)), e) else call(e.command, e.args.map { obj(it) }, e)
            is Ex.Neg -> when (val a = obj(e.a)) {
                is Point -> a.times(-1.0)
                is Vector -> Vector(a.b, a.a)
                else -> Number(-num(a), unitOf(a))
            }
            is Ex.Bin -> arithmetic(e.op, obj(e.a), obj(e.b))
            is Ex.Coordinate -> if (e.which == 'z') throw GeometryError("z( ) is for 3D graphs") else when (val a = obj(e.of)) {
                is Point -> Number(if (e.which == 'x') a.x else a.y)
                is Vector -> (a.b - a.a).let { Number(if (e.which == 'x') it.x else it.y) }
                else -> throw GeometryError("${e.which}( ) takes a point or vector")
            }
            is Ex.Fn -> {
                val objs = e.args.map { obj(it) }
                val z = objs.singleOrNull() as? Point
                if (complex && z != null) complexFn(e.name, z)
                else if (complex && objs.size == 1 && objs[0] is Number && e.name == "abs") Number(abs(num(objs[0])))
                else {
                    // f(A) with f a function of z on the complex plane: the point it goes to.
                    val args = objs.map { digits(num(it)) }
                    Number(numberOf(listOf(Func(e.name, args)), numbers()))
                }
            }
            is Ex.Sqrt -> obj(e.a).let { a -> if (complex && a is Point) cpow(a, 0.5) else Number(sqrt(num(a))) }
            is Ex.Root -> Number(Math.pow(num(obj(e.a)), 1 / num(obj(e.index))))
            is Ex.Degrees -> Number(Math.toRadians(num(obj(e.a))), Unit.Angle)
        }

        private fun unitOf(o: Obj) = when (o) { is Number -> o.unit; is Angle -> Unit.Angle; else -> Unit.None }

        /** + − × / ^ on numbers, and on points and vectors (added, scaled; a · b is the dot product). */
        fun arithmetic(op: Char, a: Obj, b: Obj): Obj {
            // On the complex plane points are numbers: sums, products, quotients and powers of them.
            // f(A): a function of z defined on the complex plane, applied to a point (or a shape).
            if (complex && op == '*' && a is ComplexMap) return image(a, b)
            if (complex && (a is Point || b is Point) && a !is Vector && b !is Vector) {
                fun c(o: Obj): Point = o as? Point ?: Point(num(o), 0.0)
                val p = c(a); val q = c(b)
                return when (op) {
                    '+' -> p + q
                    '-' -> p - q
                    '*' -> cmul(p, q)
                    '/' -> cdiv(p, q)
                    else -> if (b !is Point) cpow(p, num(b)) else cexp(cmul(q, clog(p)))
                }
            }
            fun vec(o: Obj): Point? = when (o) { is Point -> o; is Vector -> o.b - o.a; else -> null }
            val va = vec(a); val vb = vec(b)
            val na = value(a); val nb = value(b)
            return when {
                na != null && nb != null -> {
                    val u = when (op) {
                        '+', '-' -> if (unitOf(a) == unitOf(b)) unitOf(a) else Unit.None
                        '*' -> if (unitOf(a) == Unit.None) unitOf(b) else if (unitOf(b) == Unit.None) unitOf(a) else Unit.None
                        '/' -> if (unitOf(b) == Unit.None) unitOf(a) else Unit.None
                        else -> Unit.None
                    }
                    Number(when (op) { '+' -> na + nb; '-' -> na - nb; '*' -> na * nb; '/' -> na / nb; else -> Math.pow(na, nb) }, u)
                }
                va != null && vb != null -> when (op) {
                    '+' -> va + vb
                    '-' -> va - vb
                    '*' -> Number(va.x * vb.x + va.y * vb.y)
                    else -> throw GeometryError("Points can be added, subtracted and scaled")
                }
                va != null && nb != null -> when (op) {
                    '*' -> va.times(nb)
                    '/' -> va.times(1 / nb)
                    else -> throw GeometryError("A point and a number only multiply or divide")
                }
                na != null && vb != null && op == '*' -> vb.times(na)
                else -> throw GeometryError("Can't do arithmetic with that")
            }
        }

        fun point(o: Obj, what: String = "a point"): Point = o as? Point ?: throw GeometryError("Expected $what here")
        fun num(o: Obj): Double = value(o) ?: throw GeometryError("Expected a number here")
        fun angle(o: Obj): Double = when {
            o is Angle -> o.sweep
            o is Number && o.unit == Unit.Angle -> o.value
            else -> num(o).let { if (degrees) Math.toRadians(it) else it }
        }

        fun call(c: Command, a: List<Obj>, call: Ex.Call): Obj {
            fun need(vararg counts: Int) { if (a.size !in counts) throw GeometryError("${c.name} takes ${counts.joinToString(" or ")} things: ${c.usage}") }
            return when (c.name) {
                "Point" -> { need(1, 2)
                    when (val o = a[0]) {
                        is Point -> if (a.size == 2) o + (vecOf(a[1])) else o
                        else -> pointAt(o, if (a.size == 2) num(a[1]) else 0.0)
                    }
                }
                "Segment" -> { need(2)
                    val p = point(a[0])
                    when (val q = a[1]) { is Point -> Segment(p, q); else -> Segment(p, p + Point(num(q), 0.0)) }
                }
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
                    2 -> { val c0 = point(a[0]); when (val b = a[1]) {
                        is Point -> Circle(c0, (b - c0).length)
                        // A segment's length as the radius (the compass).
                        is Segment -> Circle(c0, (b.b - b.a).length)
                        else -> Circle(c0, abs(num(b)))
                    } }
                    3 -> circumcircle(point(a[0]), point(a[1]), point(a[2]))
                    else -> { need(2, 3); throw IllegalStateException() }
                }
                "Semicircle" -> { need(2)
                    val p = point(a[0]); val q = point(a[1])
                    if (p == q) throw GeometryError("The two points are the same")
                    val m = mid(p, q)
                    Arc(m, (p - m).length, atan2(p.y - m.y, p.x - m.x), PI)
                }
                "CircularArc", "CircularSector" -> { need(3)
                    val o = point(a[0]); val p = point(a[1]); val q = point(a[2])
                    if (o == p || o == q) throw GeometryError("The arc has no radius")
                    val s = atan2(p.y - o.y, p.x - o.x)
                    Arc(o, (p - o).length, s, ccw(atan2(q.y - o.y, q.x - o.x) - s), sector = c.name == "CircularSector")
                }
                "CircumcircularArc" -> { need(3)
                    val p = point(a[0]); val m = point(a[1]); val q = point(a[2])
                    val circle = circumcircle(p, m, q)
                    val o = circle.center
                    val s = atan2(p.y - o.y, p.x - o.x)
                    val toM = ccw(atan2(m.y - o.y, m.x - o.x) - s); val toQ = ccw(atan2(q.y - o.y, q.x - o.x) - s)
                    // Through B: counterclockwise if B comes before C that way round, else clockwise.
                    if (toM <= toQ) Arc(o, circle.r, s, toQ) else Arc(o, circle.r, s, toQ - 2 * PI)
                }
                "Polygon" -> { if (a.size < 3) throw GeometryError("A polygon needs three corners or more"); Polygon(a.map { point(it) }) }
                "Polyline" -> { if (a.size < 2) throw GeometryError("A polyline needs two points or more"); Polyline(listOf(a.map { point(it) })) }
                "CircumcircularSector" -> { need(3)
                    val arc = call(Command("CircumcircularArc", "", ""), a, call) as Arc
                    Arc(arc.center, arc.r, arc.start, arc.sweep, sector = true)
                }
                "FitLine" -> {
                    val pts = a.flatMap { o -> when (o) { is Point -> listOf(o); is Many -> o.items.map { point(it) }; is Polygon -> o.points; else -> listOf(point(o)) } }
                    if (pts.size < 2) throw GeometryError("A best-fit line needs two points or more")
                    val mx = pts.sumOf { it.x } / pts.size; val my = pts.sumOf { it.y } / pts.size
                    val sxx = pts.sumOf { (it.x - mx) * (it.x - mx) }; val sxy = pts.sumOf { (it.x - mx) * (it.y - my) }
                    // Least squares of y on x; points straight above each other give the vertical line.
                    if (sxx < 1e-12 * (1 + pts.sumOf { (it.y - my) * (it.y - my) })) Line(Point(mx, my), Point(mx, my + 1))
                    else Line(Point(mx, my), Point(mx + 1, my + sxy / sxx))
                }
                "Root", "Extremum" -> { need(1)
                    val f = a[0] as? FunctionGraph ?: throw GeometryError("${c.name}(f): f is a function, like f(x) = x² − 2")
                    val xs = if (c.name == "Root") roots(f.f, xRange.start, xRange.endInclusive, 4000)
                        else {
                            // Turning points: where the slope changes sign.
                            val h = (xRange.endInclusive - xRange.start) * 1e-6
                            roots({ x -> (f.f(x + h) - f.f(x - h)) / (2 * h) }, xRange.start, xRange.endInclusive, 4000)
                        }
                    val pts = xs.map { Point(it, if (c.name == "Root") 0.0 else f.f(it)) }.filter { it.y.isFinite() }
                    if (pts.isEmpty()) throw GeometryError(if (c.name == "Root") "No zeros in and around the view" else "No turning points in and around the view")
                    pts.singleOrNull() ?: Many(pts)
                }
                "Relation" -> { need(2); Text(relation(a[0], a[1])) }
                "Conjugate", "Modulus", "Argument", "RootsOfUnity", "ComplexRoots", "Image" -> {
                    if (!complex) throw GeometryError("${c.name} is for the complex plane")
                    complexCommand(c, a)
                }
                "ClosestPoint" -> { need(2)
                    val p = point(a[1])
                    when (val o = a[0]) {
                        is Point -> o
                        is FunctionGraph -> {
                            // Nearest by sampling the visible stretch, then narrowing in.
                            fun d(x: Double) = runCatching { o.f(x) }.getOrDefault(Double.NaN).let { y -> if (y.isFinite()) (x - p.x) * (x - p.x) + (y - p.y) * (y - p.y) else Double.MAX_VALUE }
                            val lo = xRange.start; val hi = xRange.endInclusive; val n = 4000
                            var best = (0..n).map { lo + (hi - lo) * it / n }.minBy { d(it) }
                            var step = (hi - lo) / n
                            repeat(60) { val l = best - step; val r = best + step; best = listOf(l, best, r).minBy { d(it) }; step /= 1.6 }
                            if (d(best) == Double.MAX_VALUE) throw GeometryError("The function has no points near here")
                            Point(best, o.f(best))
                        }
                        is Many -> o.items.map { call(c, listOf(it, p), call) as Point }.minBy { (it - p).length }
                        else -> pointAt(o, parameterOf(o, p))
                    }
                }
                "Inflection" -> { need(1)
                    val f = a[0] as? FunctionGraph ?: throw GeometryError("Inflection(f): f is a function, like f(x) = x³ − x")
                    val h = (xRange.endInclusive - xRange.start) * 1e-4
                    val xs = roots({ x -> f.f(x + h) - 2 * f.f(x) + f.f(x - h) }, xRange.start, xRange.endInclusive, 4000)
                    val pts = xs.map { Point(it, f.f(it)) }.filter { it.y.isFinite() }
                    if (pts.isEmpty()) throw GeometryError("No inflection points in and around the view")
                    pts.singleOrNull() ?: Many(pts)
                }
                "CommonTangent" -> { need(2)
                    val p = a[0] as? Circle ?: throw GeometryError("CommonTangent(c, d): two circles")
                    val q = a[1] as? Circle ?: throw GeometryError("CommonTangent(c, d): two circles")
                    commonTangents(p, q).let { if (it.isEmpty()) throw GeometryError("One circle is inside the other: no common tangents") else it.singleOrNull() ?: Many(it) }
                }
                "NinePointCircle" -> { need(3)
                    val p = point(a[0]); val q = point(a[1]); val r = point(a[2])
                    circumcircle(mid(q, r), mid(r, p), mid(p, q))
                }
                "EulerLine" -> { need(3)
                    val p = point(a[0]); val q = point(a[1]); val r = point(a[2])
                    val o = circumcircle(p, q, r).center
                    val g = Point((p.x + q.x + r.x) / 3, (p.y + q.y + r.y) / 3)
                    if ((g - o).length < 1e-9 * maxOf(1.0, (q - p).length)) throw GeometryError("An equilateral triangle's centers are all one point")
                    Line(o, g)
                }
                "Excircle" -> { need(3)
                    val p = point(a[0]); val q = point(a[1]); val r = point(a[2])
                    val la = (q - r).length; val lb = (r - p).length; val lc = (p - q).length
                    val area = abs(signedArea(listOf(p, q, r)))
                    if (area < 1e-12) throw GeometryError("The three points are on one line")
                    val w = -la + lb + lc
                    Circle(Point((-la * p.x + lb * q.x + lc * r.x) / w, (-la * p.y + lb * q.y + lc * r.y) / w), 2 * area / w)
                }
                "TriangleCenter" -> { need(4)
                    val p = point(a[0]); val q = point(a[1]); val r = point(a[2]); val n = num(a[3]).toInt()
                    triangleCenter(p, q, r, n)
                }
                "MajorAxis", "MinorAxis" -> { need(1)
                    val (cx, cy, phi) = when (val o = a[0]) {
                        is Circle -> Triple(o.center.x, o.center.y, 0.0)
                        is Conic -> when (val sh = o.shape) {
                            is ConicShape.Ellipse -> Triple(sh.cx, sh.cy, sh.phi)
                            is ConicShape.Hyperbola -> Triple(sh.cx, sh.cy, sh.phi)
                            else -> throw GeometryError("Only an ellipse or hyperbola has axes")
                        }
                        else -> throw GeometryError("Expected an ellipse or hyperbola")
                    }
                    val c0 = Point(cx, cy)
                    Line(c0, c0 + if (c.name == "MajorAxis") rotated(phi, 1.0, 0.0) else rotated(phi, 0.0, 1.0))
                }
                "UnitVector", "PerpendicularVector", "UnitPerpendicularVector", "Direction" -> { need(1)
                    val d = when (val o = a[0]) { is Point -> o; else -> asLine(o).let { (p, q) -> q - p } }
                    if (d.length < 1e-300) throw GeometryError("A vector of no length has no direction")
                    val v = when (c.name) {
                        "UnitVector" -> unit(d)
                        "PerpendicularVector" -> Point(-d.y, d.x)
                        "UnitPerpendicularVector" -> unit(Point(-d.y, d.x))
                        else -> d
                    }
                    val from = (a[0] as? Vector)?.a ?: Point(0.0, 0.0)
                    Vector(from, from + v)
                }
                "Circumference" -> { need(1)
                    Number(when (val o = a[0]) {
                        is Circle -> 2 * PI * o.r
                        is Conic -> (o.shape as? ConicShape.Ellipse)?.let { ellipseCircumference(it.a, it.b) } ?: throw GeometryError("Only an ellipse goes all the way round")
                        else -> throw GeometryError("Circumference of a circle or ellipse")
                    }, Unit.Length)
                }
                "Eccentricity", "LinearEccentricity", "SemiMajorAxisLength", "SemiMinorAxisLength" -> { need(1)
                    val (sa, sb, kind) = when (val o = a[0]) {
                        is Circle -> Triple(o.r, o.r, 'e')
                        is Conic -> when (val sh = o.shape) {
                            is ConicShape.Ellipse -> Triple(sh.a, sh.b, 'e')
                            is ConicShape.Hyperbola -> Triple(sh.a, sh.b, 'h')
                            is ConicShape.Parabola -> Triple(Double.NaN, Double.NaN, 'p')
                            ConicShape.None -> throw GeometryError("This conic has no points")
                        }
                        else -> throw GeometryError("Expected a circle or conic")
                    }
                    if (kind == 'p') when (c.name) {
                        "Eccentricity" -> Number(1.0)
                        "LinearEccentricity" -> Number(abs(parabolaVertex(((a[0] as Conic).shape as ConicShape.Parabola)).second), Unit.Length)
                        else -> throw GeometryError("A parabola has no axes of finite length")
                    } else {
                        val lin = if (kind == 'e') sqrt(maxOf(0.0, sa * sa - sb * sb)) else sqrt(sa * sa + sb * sb)
                        when (c.name) {
                            "Eccentricity" -> Number(lin / sa)
                            "LinearEccentricity" -> Number(lin, Unit.Length)
                            "SemiMajorAxisLength" -> Number(sa, Unit.Length)
                            else -> Number(sb, Unit.Length)
                        }
                    }
                }
                "Dot", "Cross" -> { need(2)
                    val u = vecOf(a[0]); val v = vecOf(a[1])
                    Number(if (c.name == "Dot") u.x * v.x + u.y * v.y else u.x * v.y - u.y * v.x)
                }
                "AreCongruent" -> { need(2)
                    fun near(x: Double, y: Double) = abs(x - y) <= 1e-9 * maxOf(1.0, abs(x), abs(y))
                    val x = a[0]; val y = a[1]
                    Bool(when {
                        (x is Segment || x is Vector) && (y is Segment || y is Vector) -> near(asLine(x).let { (p, q) -> (q - p).length }, asLine(y).let { (p, q) -> (q - p).length })
                        x is Circle && y is Circle -> near(x.r, y.r)
                        x is Arc && y is Arc -> near(x.r, y.r) && near(abs(x.sweep), abs(y.sweep))
                        x is Angle && y is Angle -> near(x.sweep, y.sweep) || near(x.sweep, 2 * PI - y.sweep)
                        x is Polygon && y is Polygon -> congruent(x.points, y.points)
                        value(x) != null && value(y) != null -> near(value(x)!!, value(y)!!)
                        else -> throw GeometryError("AreCongruent compares two segments, circles, arcs, angles or polygons")
                    })
                }
                "RegularPolygon" -> { need(3)
                    val p = point(a[0]); val q = point(a[1]); val n = num(a[2]).toInt()
                    if (n < 3) throw GeometryError("A regular polygon needs three sides or more")
                    if (p == q) throw GeometryError("The two points are the same")
                    val pts = ArrayList<Point>(listOf(p, q))
                    val turn = 2 * PI / n
                    while (pts.size < n) { val u = pts[pts.size - 1]; val v = pts[pts.size - 2]; pts += rotate(v, u, -(PI - turn)) }
                    Polygon(pts)
                }
                "Ellipse", "Hyperbola" -> { need(3)
                    val f = point(a[0]); val g = point(a[1])
                    val ellipse = c.name == "Ellipse"
                    val semi = when (val s = a[2]) {
                        is Point -> if (ellipse) ((s - f).length + (s - g).length) / 2 else abs((s - f).length - (s - g).length) / 2
                        else -> abs(num(s))
                    }
                    focal(f, g, semi, ellipse)
                }
                "Parabola" -> { need(2)
                    val f = point(a[0]); val (p, q) = asLine(a[1])
                    val n = unit(Point(-(q - p).y, (q - p).x))
                    val k = n.x * p.x + n.y * p.y
                    if (abs(n.x * f.x + n.y * f.y - k) < 1e-12) throw GeometryError("The focus is on the directrix")
                    // (x − fx)² + (y − fy)² = (n·(x, y) − k)²
                    Conic(doubleArrayOf(1 - n.x * n.x, -2 * n.x * n.y, 1 - n.y * n.y, -2 * f.x + 2 * k * n.x, -2 * f.y + 2 * k * n.y, f.x * f.x + f.y * f.y - k * k), "Parabola")
                }
                "Conic" -> { need(5); conicThrough(a.map { point(it) }) }
                "Midpoint", "Center" -> when (a.size) {
                    1 -> when (val o = a[0]) {
                        is Segment -> mid(o.a, o.b)
                        is Vector -> mid(o.a, o.b)
                        is Circle -> o.center
                        is Arc -> o.center
                        is Conic -> center(o) ?: throw GeometryError("A parabola has no center")
                        is Polygon -> centroid(o.points)
                        else -> throw GeometryError("Midpoint of a segment, or of two points")
                    }
                    else -> { need(2); mid(point(a[0]), point(a[1])) }
                }
                "Intersect" -> { need(2, 3)
                    val all = intersections(a[0], a[1], xRange)
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
                    val p = point(a[0])
                    when (val o = a[1]) {
                        is Circle -> tangents(p, o).let { it.singleOrNull() ?: Many(it) }
                        is Conic -> conicTangents(p, o).let { it.singleOrNull() ?: Many(it) }
                        is FunctionGraph -> {
                            val x = p.x; val h = 1e-5 * maxOf(1.0, abs(x))
                            val y = o.f(x); val m = (o.f(x + h) - o.f(x - h)) / (2 * h)
                            if (!y.isFinite() || !m.isFinite()) throw GeometryError("The function has no tangent there")
                            Line(Point(x, y), Point(x + 1, y + m))
                        }
                        else -> throw GeometryError("Tangent(A, c): c is a circle, a conic or a function")
                    }
                }
                "Centroid" -> if (a.size >= 2) a.map { point(it) }.let { pts -> Point(pts.sumOf { it.x } / pts.size, pts.sumOf { it.y } / pts.size) } else {
                    when (val o = a[0]) { is Polygon -> centroid(o.points); else -> throw GeometryError("Expected a polygon, or points") }
                }
                "Incircle" -> { need(3); incircle(point(a[0]), point(a[1]), point(a[2])) }
                "Circumcenter" -> { need(3); circumcircle(point(a[0]), point(a[1]), point(a[2])).center }
                "Incenter" -> { need(3); incircle(point(a[0]), point(a[1]), point(a[2])).center }
                "Orthocenter" -> { need(3)
                    val p = point(a[0]); val q = point(a[1]); val r = point(a[2])
                    if (abs(signedArea(listOf(p, q, r))) < 1e-12) throw GeometryError("The three points are on one line")
                    // The centroid G and circumcenter O: H = 3G − 2O (Euler's line).
                    val o = circumcircle(p, q, r).center
                    Point(p.x + q.x + r.x - 2 * o.x, p.y + q.y + r.y - 2 * o.y)
                }
                "Vertex" -> when (val o = a[0]) {
                    is Polygon -> if (a.size == 2) {
                        val n = num(a[1]).toInt()
                        o.points.getOrNull(n - 1) ?: throw GeometryError("The polygon has ${o.points.size} corners")
                    } else Many(o.points)
                    is Conic -> conicVertices(o).let { it.singleOrNull() ?: Many(it) }
                    is Segment -> Many(listOf(o.a, o.b))
                    else -> throw GeometryError("Vertex of a polygon or conic")
                }
                "Foci" -> { need(1); foci(a[0] as? Conic ?: throw GeometryError("Foci of a conic")).let { it.singleOrNull() ?: Many(it) } }
                "Asymptote" -> { need(1)
                    val s = (a[0] as? Conic)?.shape as? ConicShape.Hyperbola ?: throw GeometryError("Only a hyperbola has asymptotes")
                    val c0 = Point(s.cx, s.cy)
                    Many(listOf(1.0, -1.0).map { sign -> Line(c0, c0 + rotated(s.phi, s.a, sign * s.b)) })
                }
                "Directrix" -> { need(1)
                    val o = a[0] as? Conic ?: throw GeometryError("Expected a parabola")
                    val s = o.shape as? ConicShape.Parabola ?: throw GeometryError("Only a parabola has one directrix here")
                    val (v, p) = parabolaVertex(s)
                    // The line x′ = x′(vertex) − p, at right angles to the axis.
                    val foot = rotated(s.phi, v.x - p, v.y)
                    Line(foot, foot + rotated(s.phi, 0.0, 1.0))
                }
                "Polar" -> { need(2)
                    val p = point(a[0])
                    val c = when (val o = a[1]) {
                        is Circle -> doubleArrayOf(1.0, 0.0, 1.0, -2 * o.center.x, -2 * o.center.y, o.center.x * o.center.x + o.center.y * o.center.y - o.r * o.r)
                        is Conic -> o.c
                        else -> throw GeometryError("Polar(A, c): c is a circle or conic")
                    }
                    // The line (A, B/2, D/2; B/2, C, E/2; D/2, E/2, F)·(x₀, y₀, 1) · (x, y, 1) = 0.
                    val la = c[0] * p.x + c[1] / 2 * p.y + c[3] / 2
                    val lb = c[1] / 2 * p.x + c[2] * p.y + c[4] / 2
                    val lc = c[3] / 2 * p.x + c[4] / 2 * p.y + c[5]
                    lineFrom(la, lb, lc) ?: throw GeometryError("The polar is at infinity (A is the center)")
                }
                "Locus" -> { need(2)
                    val traced = (call.args[0] as? Ex.Ref)?.name ?: throw GeometryError("Locus(P, Q): P is a point's name")
                    val mover = (call.args[1] as? Ex.Ref)?.name ?: throw GeometryError("Locus(P, Q): Q is a point's name")
                    locus(traced, mover)
                }
                "AreCollinear" -> { need(3); val p = point(a[0]); val q = point(a[1]); val r = point(a[2])
                    Bool(abs(signedArea(listOf(p, q, r))) <= 1e-9 * maxOf(1.0, (q - p).length * (r - p).length)) }
                "AreConcyclic" -> { need(4)
                    val pts = a.map { point(it) }
                    Bool(runCatching { circumcircle(pts[0], pts[1], pts[2]) }.getOrNull()?.let { c -> abs((pts[3] - c.center).length - c.r) <= 1e-9 * maxOf(1.0, c.r) } ?: false)
                }
                "AreParallel", "ArePerpendicular" -> { need(2)
                    val u = direction(a[0]); val v = direction(a[1])
                    val cross = u.x * v.y - u.y * v.x; val dot = u.x * v.x + u.y * v.y
                    Bool(if (c.name == "AreParallel") abs(cross) < 1e-9 else abs(dot) < 1e-9)
                }
                "AreEqual" -> { need(2)
                    val x = a[0]; val y = a[1]
                    Bool(when {
                        x is Point && y is Point -> (x - y).length <= 1e-9 * maxOf(1.0, x.length)
                        value(x) != null && value(y) != null -> abs(value(x)!! - value(y)!!) <= 1e-9 * maxOf(1.0, abs(value(x)!!))
                        else -> x == y
                    })
                }
                "Distance" -> { need(2)
                    val p = point(a[0])
                    Number(when (val o = a[1]) {
                        is Point -> (o - p).length
                        is Line, is Segment, is Ray, is Vector -> distanceTo(p, o)
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
                        is Circle -> 2 * PI * o.r
                        is Arc -> o.r * abs(o.sweep)
                        else -> throw GeometryError("Length of a segment, vector, arc or polygon")
                    }, Unit.Length)
                }
                "Perimeter" -> { need(1)
                    Number(when (val o = a[0]) {
                        is Polygon -> perimeter(o.points)
                        is Circle -> 2 * PI * o.r
                        is Arc -> o.r * abs(o.sweep) + if (o.sector) 2 * o.r else 0.0
                        else -> throw GeometryError("Perimeter of a polygon or circle")
                    }, Unit.Length)
                }
                "Area" -> {
                    if (a.size >= 3) Number(abs(signedArea(a.map { point(it) })), Unit.Area)
                    else { need(1)
                        Number(when (val o = a[0]) {
                            is Polygon -> abs(signedArea(o.points))
                            is Circle -> PI * o.r * o.r
                            is Arc -> o.r * o.r * abs(o.sweep) / 2
                            is Conic -> (o.shape as? ConicShape.Ellipse)?.let { PI * it.a * it.b } ?: throw GeometryError("Only an ellipse has an area")
                            else -> throw GeometryError("Area of a polygon, circle or sector")
                        }, Unit.Area)
                    }
                }
                "Angle" -> if (a.size == 2) {
                    // Between two lines or vectors: from the first round to the second, at their crossing.
                    val u = direction(a[0]); val v = direction(a[1])
                    val vertex = when {
                        a[0] is Vector && a[1] is Vector -> (a[0] as Vector).a
                        abs(u.x * v.y - u.y * v.x) > 1e-12 -> runCatching { intersections(Line(asLine(a[0]).first, asLine(a[0]).first + u), Line(asLine(a[1]).first, asLine(a[1]).first + v)).first() }.getOrElse { asLine(a[0]).first }
                        else -> asLine(a[0]).first
                    }
                    val s = atan2(u.y, u.x)
                    Angle(vertex, s, ccw(atan2(v.y, v.x) - s))
                } else { need(3)
                    val p = point(a[0]); val b = point(a[1]); val q = point(a[2])
                    if (p == b || q == b) throw GeometryError("The angle's arms have no length")
                    val s = atan2(p.y - b.y, p.x - b.x)
                    Angle(b, s, ccw(atan2(q.y - b.y, q.x - b.x) - s))
                }
                "Slope" -> { need(1)
                    val d = direction(a[0])
                    if (abs(d.x) < 1e-15) throw GeometryError("A vertical line has no slope")
                    Number(d.y / d.x)
                }
                "Radius" -> { need(1)
                    Number(when (val o = a[0]) { is Circle -> o.r; is Arc -> o.r; else -> throw GeometryError("Expected a circle") }, Unit.Length)
                }
                "Reflect" -> { need(2)
                    when (val m = a[1]) {
                        // In a circle: inversion, P′ on the ray from the center with |OP|·|OP′| = r².
                        is Circle -> {
                            fun invert(p: Point): Point {
                                val d = p - m.center; val l2 = d.x * d.x + d.y * d.y
                                if (l2 < 1e-24) throw GeometryError("The center has no image in the circle")
                                return m.center + d.times(m.r * m.r / l2)
                            }
                            when (val o = a[0]) {
                                is Point -> invert(o)
                                is Many -> Many(o.items.map { invert(point(it)) })
                                else -> throw GeometryError("Reflecting in a circle works on points here")
                            }
                        }
                        is Point -> transform(a[0], scale = -1.0) { p -> m + m - p }
                        else -> { val l = asLine(m); transform(a[0], mirror = true) { p -> mirror(p, l.first, l.second) } }
                    }
                }
                "Rotate" -> { need(2, 3)
                    val t = angle(a[1]); val c0 = if (a.size == 3) point(a[2]) else Point(0.0, 0.0)
                    transform(a[0], turn = t) { p -> rotate(p, c0, t) }
                }
                "Translate" -> { need(2)
                    val v = vecOf(a[1])
                    transform(a[0]) { p -> p + v }
                }
                "Dilate" -> { need(2, 3)
                    val k = num(a[1]); val c0 = if (a.size == 3) point(a[2]) else Point(0.0, 0.0)
                    transform(a[0], scale = k) { p -> c0 + (p - c0).times(k) }
                }
                else -> throw GeometryError("${c.name} isn't available yet")
            }
        }

        fun vecOf(o: Obj): Point = when (o) { is Vector -> o.b - o.a; is Point -> o; else -> throw GeometryError("Expected a vector, or a pair (dx, dy)") }

        /** The complex plane's own commands. */
        fun complexCommand(c: Command, a: List<Obj>): Obj {
            fun need(n: Int) { if (a.size != n) throw GeometryError("${c.name} takes $n things: ${c.usage}") }
            return when (c.name) {
                "Conjugate" -> { need(1); transform(a[0], mirror = true) { p -> Point(p.x, -p.y) } }
                "Modulus" -> { need(1); Number(point(a[0]).length, Unit.Length) }
                "Argument" -> { need(1); val p = point(a[0]); Number(atan2(p.y, p.x), Unit.Angle) }
                "RootsOfUnity", "ComplexRoots" -> {
                    val (z, n) = if (c.name == "RootsOfUnity") { need(1); Point(1.0, 0.0) to num(a[0]).toInt() } else { need(2); point(a[0]) to num(a[1]).toInt() }
                    if (n < 1) throw GeometryError("n is a whole number, 1 or more")
                    val r = Math.pow(z.length, 1.0 / n); val t0 = atan2(z.y, z.x) / n
                    Many((0 until n).map { k -> Point(r * cos(t0 + 2 * PI * k / n), r * sin(t0 + 2 * PI * k / n)) })
                }
                "Image" -> { need(2)
                    val f = a[0] as? ComplexMap ?: throw GeometryError("Image(f, c): f is a function of z, like f(z) = z²")
                    image(f, a[1])
                }
                else -> throw GeometryError("${c.name} isn't available yet")
            }
        }

        /** Where [f] takes an object: a point to a point; a path, traced along, to a curve. */
        fun image(f: ComplexMap, o: Obj): Obj = when (o) {
            is Point -> f.f(o) ?: throw GeometryError("f isn't defined there")
            is Many -> Many(o.items.map { image(f, it) })
            else -> {
                val (lo, hi, n) = when (o) {
                    is Line -> Triple(-40.0, 40.0, 1600)
                    is Ray -> Triple(0.0, 40.0, 800)
                    else -> Triple(0.0, 1.0, 600)
                }
                val pieces = ArrayList<List<Point>>()
                var cur = ArrayList<Point>()
                var last: Point? = null
                for (k in 0..n) {
                    val t = lo + (hi - lo) * k / n
                    val w = runCatching { pointAt(o, t) }.getOrNull()?.let { f.f(it) }?.takeIf { it.x.isFinite() && it.y.isFinite() }
                    // A break where f isn't defined or jumps (across a pole or a branch cut).
                    if (w == null || last != null && (w - last).length > 50) { if (cur.size > 1) pieces += cur; cur = ArrayList() }
                    if (w != null) cur += w
                    last = w
                }
                if (cur.size > 1) pieces += cur
                if (pieces.isEmpty()) throw GeometryError("f isn't defined along it")
                Polyline(pieces)
            }
        }
    }

    // ---- Complex numbers (the complex plane's points) ---------------------------------------

    private fun cmul(p: Point, q: Point) = Point(p.x * q.x - p.y * q.y, p.x * q.y + p.y * q.x)
    private fun cdiv(p: Point, q: Point): Point {
        val d = q.x * q.x + q.y * q.y
        if (d < 1e-300) throw GeometryError("Can't divide by 0")
        return Point((p.x * q.x + p.y * q.y) / d, (p.y * q.x - p.x * q.y) / d)
    }
    private fun cexp(p: Point) = exp(p.x).let { r -> Point(r * cos(p.y), r * sin(p.y)) }
    private fun clog(p: Point): Point {
        if (p.length < 1e-300) throw GeometryError("ln 0 isn't defined")
        return Point(kotlin.math.ln(p.length), atan2(p.y, p.x))
    }
    private fun cpow(p: Point, k: Double): Point {
        if (p.length < 1e-300) return if (k > 0) Point(0.0, 0.0) else throw GeometryError("0 has no negative power")
        val r = Math.pow(p.length, k); val t = atan2(p.y, p.x) * k
        return Point(r * cos(t), r * sin(t))
    }

    /** A function key on a point of the complex plane: |z|, arg z, z̄, Re, Im, and e^z, ln, sin… of z. */
    private fun complexFn(name: String, z: Point): Obj = when (name) {
        "abs" -> Number(z.length, Unit.Length)
        "arg" -> Number(atan2(z.y, z.x), Unit.Angle)
        "conj" -> Point(z.x, -z.y)
        "re", "Re" -> Number(z.x)
        "im", "Im" -> Number(z.y)
        "exp" -> cexp(z)
        "ln", "log" -> clog(z)
        "sqrt" -> cpow(z, 0.5)
        "sin" -> Point(sin(z.x) * kotlin.math.cosh(z.y), cos(z.x) * kotlin.math.sinh(z.y))
        "cos" -> Point(cos(z.x) * kotlin.math.cosh(z.y), -sin(z.x) * kotlin.math.sinh(z.y))
        else -> throw GeometryError("$name of a point isn't available here")
    }

    // ---- Geometry ------------------------------------------------------------------------

    /** A number's value (a length, an area, an angle), or null for shapes. */
    fun value(o: Obj): Double? = when (o) {
        is Number -> o.value
        is Angle -> o.sweep
        else -> null
    }

    /** How two objects relate, in words (Relation(a, b)). */
    private fun relation(a: Obj, b: Obj): String {
        fun near(x: Double, y: Double) = abs(x - y) <= 1e-9 * maxOf(1.0, abs(x), abs(y))
        return when {
            a is Point && b is Point -> if ((a - b).length < 1e-9) "The points are the same" else "The points are different (${fmt((a - b).length)} apart)"
            isStraight(a) && isStraight(b) -> {
                val u = direction(a); val v = direction(b)
                val cross = u.x * v.y - u.y * v.x; val dot = u.x * v.x + u.y * v.y
                when {
                    abs(cross) < 1e-9 -> if (distanceTo(asLine(a).first, Line(asLine(b).first, asLine(b).second)) < 1e-9) "They're on the same line" else "They're parallel"
                    abs(dot) < 1e-9 -> "They're perpendicular"
                    else -> "They cross at " + fmtAngle(kotlin.math.acos(abs(dot)))
                } + if (a is Segment && b is Segment && near((a.b - a.a).length, (b.b - b.a).length)) ", and are the same length" else ""
            }
            a is Point || b is Point -> {
                val p = (if (a is Point) a else b) as Point; val o = if (a is Point) b else a
                val on = when (o) {
                    is Circle -> near((p - o.center).length, o.r)
                    is Conic -> abs(conicValue(o.c, p)) < 1e-9 * (1 + o.c.maxOf { abs(it) })
                    is Line, is Segment, is Ray, is Vector -> distanceTo(p, o) < 1e-9
                    is Polygon -> pieces(o).any { distanceTo(p, it) < 1e-9 }
                    else -> false
                }
                if (on) "The point is on it" else "The point isn't on it"
            }
            value(a) != null && value(b) != null -> if (near(value(a)!!, value(b)!!)) "They're equal" else "They're different (${fmt(value(a)!!)} and ${fmt(value(b)!!)})"
            a is Circle && b is Circle -> when {
                (a.center - b.center).length < 1e-9 && near(a.r, b.r) -> "The circles are the same"
                (a.center - b.center).length < 1e-9 -> "The circles are concentric"
                near((a.center - b.center).length, a.r + b.r) || near((a.center - b.center).length, abs(a.r - b.r)) -> "The circles touch"
                else -> "The circles meet in ${circles(a, b).size} points"
            }
            else -> intersections(a, b).size.let { n -> if (n == 0) "They don't meet" else "They meet in $n point" + if (n == 1) "" else "s" }
        }
    }

    private fun fmt(v: Double) = java.math.BigDecimal(v).round(java.math.MathContext(5)).stripTrailingZeros().toPlainString()
    private fun fmtAngle(r: Double) = fmt(Math.toDegrees(r)) + "°"

    /** An angle brought into [0, 2π). */
    private fun ccw(a: Double): Double { var s = a % (2 * PI); if (s < 0) s += 2 * PI; return s }

    private fun mid(p: Point, q: Point) = Point((p.x + q.x) / 2, (p.y + q.y) / 2)
    private fun unit(p: Point): Point { val l = p.length; if (l < 1e-300) throw GeometryError("The points are the same"); return Point(p.x / l, p.y / l) }

    /** A line-like object's two defining points. */
    private fun asLine(o: Obj): kotlin.Pair<Point, Point> = when (o) {
        is Line -> o.a to o.b
        is Segment -> o.a to o.b
        is Ray -> o.a to o.b
        is Vector -> o.a to o.b
        else -> throw GeometryError("Expected a line, segment or ray")
    }

    private fun isStraight(o: Obj) = o is Line || o is Segment || o is Ray || o is Vector

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
        t = when (o) { is Segment, is Vector -> t.coerceIn(0.0, 1.0); is Ray -> maxOf(t, 0.0); else -> t }
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

    /**
     * [o] with every point moved by [f] (a similarity: [scale] for radii, [turn] for angles,
     * [mirror] reverses arcs); a conic is moved through five of its points.
     */
    private fun transform(o: Obj, scale: Double = 1.0, turn: Double = 0.0, mirror: Boolean = false, f: (Point) -> Point): Obj = when (o) {
        is Point -> f(o)
        is Line -> Line(f(o.a), f(o.b))
        is Segment -> Segment(f(o.a), f(o.b))
        is Ray -> Ray(f(o.a), f(o.b))
        is Vector -> Vector(f(o.a), f(o.b))
        is Circle -> Circle(f(o.center), o.r * abs(scale))
        is Polygon -> Polygon(o.points.map(f))
        is Arc -> {
            // Its ends moved, the arc running from the image of its start.
            val c = f(o.center)
            val p = f(Point(o.center.x + o.r * cos(o.start), o.center.y + o.r * sin(o.start)))
            val s = atan2(p.y - c.y, p.x - c.x)
            Arc(c, o.r * abs(scale), s, if (mirror) -o.sweep else o.sweep, o.sector)
        }
        is Conic -> conicThrough(conicSamples(o).map(f)).let { Conic(it.c, o.kind) }
        is Angle -> o
        is Many -> Many(o.items.map { transform(it, scale, turn, mirror, f) })
        is Number, is Bool, is Text -> throw GeometryError("A number can't be moved")
        is FunctionGraph -> throw GeometryError("A function's graph can't be moved here")
        is ComplexMap -> throw GeometryError("A function can't be moved")
        is Polyline -> Polyline(o.pieces.map { it.map(f) })
    }

    /** The lines touching both circles: outer ones, and inner ones when the circles are apart. */
    private fun commonTangents(p: Circle, q: Circle): List<Obj> {
        val d = q.center - p.center
        val dist = d.length
        if (dist < 1e-12) return emptyList()
        val out = ArrayList<Obj>()
        // A line n·X = c with |n| = 1 is tangent to both when n·P − c = r₁ and n·Q − c = s·r₂ (s = 1 outer, −1 inner),
        // so n·(Q − P) = s·r₂ − r₁.
        for (s in listOf(1.0, -1.0)) {
            val k = (s * q.r - p.r) / dist
            if (abs(k) > 1 + 1e-12) continue
            val h = sqrt(maxOf(0.0, 1 - k * k))
            val u = Point(d.x / dist, d.y / dist)
            val normals = if (h < 1e-9) listOf(u.times(k)) else listOf(1.0, -1.0).map { t -> Point(u.x * k - t * u.y * h, u.y * k + t * u.x * h) }
            for (n in normals) {
                // The touching point on the first circle, and the line along it.
                val touch = p.center - n.times(p.r)
                out += Line(touch, touch + Point(-n.y, n.x))
            }
        }
        return out
    }

    /** Kimberling's triangle centers 1 to 8, by their barycentric coordinates. */
    private fun triangleCenter(p: Point, q: Point, r: Point, n: Int): Point {
        val a = (q - r).length; val b = (r - p).length; val c = (p - q).length
        if (abs(signedArea(listOf(p, q, r))) < 1e-12) throw GeometryError("The three points are on one line")
        val s = (a + b + c) / 2
        val w = when (n) {
            1 -> Triple(a, b, c)
            2 -> Triple(1.0, 1.0, 1.0)
            3 -> Triple(a * a * (b * b + c * c - a * a), b * b * (c * c + a * a - b * b), c * c * (a * a + b * b - c * c))
            4 -> Triple(1 / (b * b + c * c - a * a), 1 / (c * c + a * a - b * b), 1 / (a * a + b * b - c * c))
            5 -> Triple(a * a * (b * b + c * c) - (b * b - c * c) * (b * b - c * c), b * b * (c * c + a * a) - (c * c - a * a) * (c * c - a * a), c * c * (a * a + b * b) - (a * a - b * b) * (a * a - b * b))
            6 -> Triple(a * a, b * b, c * c)
            7 -> Triple(1 / (s - a), 1 / (s - b), 1 / (s - c))
            8 -> Triple(s - a, s - b, s - c)
            else -> throw GeometryError("Triangle centers 1 to 8 are known here")
        }
        // A weight at infinity is that corner itself (the orthocenter of a right triangle).
        if (w.first.isInfinite()) return p
        if (w.second.isInfinite()) return q
        if (w.third.isInfinite()) return r
        val sum = w.first + w.second + w.third
        if (!sum.isFinite() || abs(sum) < 1e-300) throw GeometryError("That center is at infinity for this triangle")
        return Point((w.first * p.x + w.second * q.x + w.third * r.x) / sum, (w.first * p.y + w.second * q.y + w.third * r.y) / sum)
    }

    /** The perimeter of an ellipse with semi-axes [a] and [b], by Gauss–Kummer's series via the AGM. */
    private fun ellipseCircumference(a: Double, b: Double): Double {
        // 2π (a² + b²)/2 less 2ⁿ⁻¹ cₙ² at each AGM step, over the AGM: exact to rounding.
        var x = maxOf(a, b); var y = minOf(a, b)
        var sum = (x * x + y * y) / 2
        var pow = 0.5
        repeat(40) {
            val nx = (x + y) / 2; val ny = sqrt(x * y)
            pow *= 2
            sum -= pow * ((x - y) / 2) * ((x - y) / 2)
            x = nx; y = ny
            if (abs(x - y) < 1e-16 * x) return 4 * PI * sum / (x + y)
        }
        return 4 * PI * sum / (x + y)
    }

    /** Whether two polygons are the same size and shape: their sides and corners match, in some turn or mirrored. */
    private fun congruent(p: List<Point>, q: List<Point>): Boolean {
        if (p.size != q.size) return false
        fun near(x: Double, y: Double) = abs(x - y) <= 1e-9 * maxOf(1.0, abs(x), abs(y))
        fun shape(pts: List<Point>) = pts.indices.map { k ->
            val prev = pts[(k - 1 + pts.size) % pts.size]; val here = pts[k]; val next = pts[(k + 1) % pts.size]
            val u = prev - here; val v = next - here
            (next - here).length to abs(atan2(u.x * v.y - u.y * v.x, u.x * v.x + u.y * v.y))
        }
        val sp = shape(p)
        for (order in listOf(q, q.reversed())) {
            val sq = shape(order)
            for (shift in sq.indices) if (sp.indices.all { k -> val (l1, a1) = sp[k]; val (l2, a2) = sq[(k + shift) % sq.size]; near(l1, l2) && near(a1, a2) }) return true
        }
        return false
    }

    private fun tangents(p: Point, c: Circle): List<Obj> {
        val d = (p - c.center).length
        if (d < c.r - 1e-12) return emptyList()
        val base = atan2(p.y - c.center.y, p.x - c.center.x)
        if (abs(d - c.r) < 1e-9) { val n = p - c.center; return listOf(Line(p, p + Point(-n.y, n.x))) }
        val alpha = kotlin.math.acos(c.r / d)
        return listOf(base + alpha, base - alpha).map { t -> Line(p, Point(c.center.x + c.r * cos(t), c.center.y + c.r * sin(t))) }
    }

    // ---- Points on paths -----------------------------------------------------------------

    /**
     * The point at [t] along a path: a segment from 0 to 1, a line or ray in steps of its two
     * points' distance, once round a circle or ellipse from 0 to 1, round a polygon by its
     * perimeter, along an arc from 0 to 1, x = t on a function's graph.
     */
    fun pointAt(o: Obj, t: Double): Point = when (o) {
        is Segment -> o.a + (o.b - o.a).times(t.coerceIn(0.0, 1.0))
        is Vector -> o.a + (o.b - o.a).times(t.coerceIn(0.0, 1.0))
        is Ray -> o.a + (o.b - o.a).times(maxOf(t, 0.0))
        is Line -> o.a + (o.b - o.a).times(t)
        is Circle -> Point(o.center.x + o.r * cos(2 * PI * t), o.center.y + o.r * sin(2 * PI * t))
        is Arc -> (o.start + o.sweep * t.coerceIn(0.0, 1.0)).let { a -> Point(o.center.x + o.r * cos(a), o.center.y + o.r * sin(a)) }
        is Polygon -> {
            val total = perimeter(o.points)
            var left = (t - kotlin.math.floor(t)) * total
            var k = 0
            while (true) {
                val p = o.points[k % o.points.size]; val q = o.points[(k + 1) % o.points.size]
                val len = (q - p).length
                if (left <= len || k >= o.points.size - 1) break
                left -= len; k++
            }
            val p = o.points[k % o.points.size]; val q = o.points[(k + 1) % o.points.size]
            val len = (q - p).length
            if (len < 1e-300) p else p + (q - p).times(minOf(left / len, 1.0))
        }
        is Conic -> conicAt(o.shape, t) ?: throw GeometryError("This conic has no points")
        is FunctionGraph -> o.f(t).let { y -> if (y.isFinite()) Point(t, y) else throw GeometryError("The function isn't defined at x = ${shortText(t)}") }
        is Point -> o
        is Polyline -> {
            val pts = o.pieces.flatten()
            val lengths = (0 until pts.size - 1).map { (pts[it + 1] - pts[it]).length }
            var left = t.coerceIn(0.0, 1.0) * lengths.sum()
            var k = 0
            while (k < lengths.size - 1 && left > lengths[k]) { left -= lengths[k]; k++ }
            if (lengths.isEmpty() || lengths[k] == 0.0) pts.first() else pts[k] + (pts[k + 1] - pts[k]).times(left / lengths[k])
        }
        else -> throw GeometryError("A point can go on a line, circle, arc, polygon, conic or function graph")
    }

    private fun shortText(v: Double) = java.math.BigDecimal(v).round(java.math.MathContext(6)).stripTrailingZeros().toPlainString()

    /** The parameter of the point of path [o] nearest [p] (the inverse of [pointAt]). */
    fun parameterOf(o: Obj, p: Point): Double = when (o) {
        is Segment, is Vector, is Ray, is Line -> {
            val (a, b) = asLine(o)
            val d = b - a
            val t = ((p.x - a.x) * d.x + (p.y - a.y) * d.y) / (d.x * d.x + d.y * d.y)
            when (o) { is Segment, is Vector -> t.coerceIn(0.0, 1.0); is Ray -> maxOf(t, 0.0); else -> t }
        }
        is Circle -> ccw(atan2(p.y - o.center.y, p.x - o.center.x)) / (2 * PI)
        is Arc -> {
            val a = atan2(p.y - o.center.y, p.x - o.center.x)
            val along = if (o.sweep >= 0) ccw(a - o.start) else -ccw(o.start - a)
            // Past either end: the nearer end.
            if (along / o.sweep in 0.0..1.0) along / o.sweep else {
                val ends = listOf(0.0, 1.0)
                ends.minBy { (pointAt(o, it) - p).length }
            }
        }
        is FunctionGraph -> p.x
        is Polygon, is Conic, is Polyline -> nearestParameter(o, p)
        else -> 0.0
    }

    /** The parameter nearest [p] by sampling then refining (polygons and conics). */
    private fun nearestParameter(o: Obj, p: Point): Double {
        val (lo, hi) = when {
            o is Conic && o.shape !is ConicShape.Ellipse -> -1.999 to 1.999
            else -> 0.0 to 1.0
        }
        val n = 720
        var best = lo; var bestD = Double.MAX_VALUE
        for (k in 0..n) {
            val t = lo + (hi - lo) * k / n
            val d = runCatching { (pointAt(o, t) - p).length }.getOrDefault(Double.MAX_VALUE)
            if (d < bestD) { bestD = d; best = t }
        }
        var a = best - (hi - lo) / n; var b = best + (hi - lo) / n
        repeat(60) {
            val m1 = a + (b - a) / 3; val m2 = b - (b - a) / 3
            val d1 = runCatching { (pointAt(o, m1) - p).length }.getOrDefault(Double.MAX_VALUE)
            val d2 = runCatching { (pointAt(o, m2) - p).length }.getOrDefault(Double.MAX_VALUE)
            if (d1 < d2) b = m2 else a = m1
        }
        val t = (a + b) / 2
        return if (o is Conic && o.shape is ConicShape.Ellipse || o is Polygon) t - kotlin.math.floor(t) else t
    }

    // ---- Conics --------------------------------------------------------------------------

    /** A conic's shape, from its coefficients: what it is, where, and how it's turned. */
    sealed class ConicShape {
        /** Center, turn of the major axis, semi-axes. */
        class Ellipse(val cx: Double, val cy: Double, val phi: Double, val a: Double, val b: Double) : ConicShape()
        /** Center, turn of the axis through the vertices, semi-axes (x′ = ±a cosh u, y′ = b sinh u). */
        class Hyperbola(val cx: Double, val cy: Double, val phi: Double, val a: Double, val b: Double) : ConicShape()
        /** In the turned frame (by [phi]) x′ = [k2] y′² + [k1] y′ + [k0]: the curve as y′ runs. */
        class Parabola(val phi: Double, val k2: Double, val k1: Double, val k0: Double) : ConicShape()
        /** No points, or a degenerate pair of lines. */
        object None : ConicShape()
    }

    fun classify(c: DoubleArray): ConicShape {
        val scale = c.maxOf { abs(it) }.takeIf { it > 0 } ?: return ConicShape.None
        val (A, B, C, D, E, F) = c.map { it / scale }.let { listOf(it[0], it[1], it[2], it[3], it[4], it[5]) }.let { Six(it) }
        val th = 0.5 * atan2(B, A - C)
        val co = cos(th); val si = sin(th)
        val a2 = A * co * co + B * co * si + C * si * si
        val c2 = A * si * si - B * co * si + C * co * co
        val d2 = D * co + E * si
        val e2 = -D * si + E * co
        val eps = 1e-10
        fun world(xp: Double, yp: Double) = Point(xp * co - yp * si, xp * si + yp * co)
        return when {
            abs(a2) > eps && abs(c2) > eps -> {
                val x0 = -d2 / (2 * a2); val y0 = -e2 / (2 * c2)
                val k = -(F - a2 * x0 * x0 - c2 * y0 * y0)
                val center = world(x0, y0)
                when {
                    a2 * c2 > 0 -> if (k / a2 > eps) {
                        val ax = sqrt(k / a2); val bx = sqrt(k / c2)
                        if (ax >= bx) ConicShape.Ellipse(center.x, center.y, th, ax, bx) else ConicShape.Ellipse(center.x, center.y, th + PI / 2, bx, ax)
                    } else ConicShape.None
                    abs(k) < eps -> ConicShape.None
                    k / a2 > 0 -> ConicShape.Hyperbola(center.x, center.y, th, sqrt(k / a2), sqrt(-k / c2))
                    else -> ConicShape.Hyperbola(center.x, center.y, th + PI / 2, sqrt(k / c2), sqrt(-k / a2))
                }
            }
            abs(c2) > eps && abs(d2) > eps -> ConicShape.Parabola(th, -c2 / d2, -e2 / d2, -F / d2)
            // A′x′² + D′x′ + E′y′ + F = 0, turned a quarter more so it runs along y″ (x′ = −y″, y′ = x″).
            abs(a2) > eps && abs(e2) > eps -> ConicShape.Parabola(th + PI / 2, -a2 / e2, d2 / e2, -F / e2)
            else -> ConicShape.None
        }
    }

    private class Six(val v: List<Double>) {
        operator fun component1() = v[0]; operator fun component2() = v[1]; operator fun component3() = v[2]
        operator fun component4() = v[3]; operator fun component5() = v[4]; operator fun component6() = v[5]
    }

    /** The point of a conic at parameter [t]: once round an ellipse for 0 to 1; −2 to 2 over a hyperbola's two branches; −1 to 1 along a parabola. */
    fun conicAt(s: ConicShape, t: Double): Point? = when (s) {
        is ConicShape.Ellipse -> rotated(s.phi, s.a * cos(2 * PI * t), s.b * sin(2 * PI * t)).let { Point(it.x + s.cx, it.y + s.cy) }
        is ConicShape.Hyperbola -> {
            val branch = if (t >= 0) 1.0 else -1.0
            val u = kotlin.math.tan(PI / 2 * ((if (t >= 0) t else t + 2) - 1).coerceIn(-0.9999, 0.9999))
            rotated(s.phi, branch * s.a * kotlin.math.cosh(u), s.b * kotlin.math.sinh(u)).let { Point(it.x + s.cx, it.y + s.cy) }
        }
        is ConicShape.Parabola -> {
            val y = kotlin.math.tan(PI / 2 * t.coerceIn(-0.9999, 0.9999))
            rotated(s.phi, s.k2 * y * y + s.k1 * y + s.k0, y)
        }
        ConicShape.None -> null
    }

    private fun rotated(phi: Double, x: Double, y: Double) = Point(x * cos(phi) - y * sin(phi), x * sin(phi) + y * cos(phi))

    /** Ax² + Bxy + Cy² + Dx + Ey + F at a point. */
    fun conicValue(c: DoubleArray, p: Point) = c[0] * p.x * p.x + c[1] * p.x * p.y + c[2] * p.y * p.y + c[3] * p.x + c[4] * p.y + c[5]

    private fun center(o: Conic): Point? = when (val s = o.shape) {
        is ConicShape.Ellipse -> Point(s.cx, s.cy)
        is ConicShape.Hyperbola -> Point(s.cx, s.cy)
        else -> null
    }

    /** The ellipse (or hyperbola) with foci [f], [g] and semi-major axis [a]. */
    private fun focal(f: Point, g: Point, a: Double, ellipse: Boolean): Conic {
        val c = (g - f).length / 2
        if (ellipse && a <= c) throw GeometryError("The semi-major axis must be longer than half the distance between the foci")
        if (!ellipse && (a >= c || a <= 0)) throw GeometryError("The semi-major axis must be shorter than half the distance between the foci")
        val m = mid(f, g)
        val phi = if (c > 0) atan2(g.y - f.y, g.x - f.x) else 0.0
        val b2 = if (ellipse) a * a - c * c else c * c - a * a
        // x′²/a² ± y′²/b² = 1 with x′ = (x − m)·u, y′ = (x − m)·v.
        val p = 1 / (a * a); val q = (if (ellipse) 1.0 else -1.0) / b2
        val co = cos(phi); val si = sin(phi)
        val A = p * co * co + q * si * si
        val B = 2 * (p - q) * co * si
        val C = p * si * si + q * co * co
        val D = -2 * A * m.x - B * m.y
        val E = -2 * C * m.y - B * m.x
        val F = A * m.x * m.x + B * m.x * m.y + C * m.y * m.y - 1
        return Conic(doubleArrayOf(A, B, C, D, E, F), if (ellipse) "Ellipse" else "Hyperbola")
    }

    /** The conic through five points: each coefficient a 5 × 5 determinant (the null space of their equations). */
    private fun conicThrough(pts: List<Point>): Conic {
        val rows = pts.map { p -> doubleArrayOf(p.x * p.x, p.x * p.y, p.y * p.y, p.x, p.y, 1.0) }
        val coef = DoubleArray(6) { j ->
            val minor = Array(5) { i -> DoubleArray(5) { k -> rows[i][if (k < j) k else k + 1] } }
            (if (j % 2 == 0) 1.0 else -1.0) * det(minor)
        }
        if (coef.all { abs(it) < 1e-12 }) throw GeometryError("Five points that don't fix one conic (four on a line?)")
        val c = Conic(coef, "Conic")
        if (c.shape == ConicShape.None) throw GeometryError("These points give two lines, not a curve")
        return c
    }

    private fun det(m: Array<DoubleArray>): Double {
        val a = m.map { it.copyOf() }.toTypedArray()
        val n = a.size
        var d = 1.0
        for (col in 0 until n) {
            val piv = (col until n).maxBy { abs(a[it][col]) }
            if (abs(a[piv][col]) < 1e-300) return 0.0
            if (piv != col) { val t = a[piv]; a[piv] = a[col]; a[col] = t; d = -d }
            d *= a[col][col]
            for (r in col + 1 until n) {
                val f = a[r][col] / a[col][col]
                for (k in col until n) a[r][k] -= f * a[col][k]
            }
        }
        return d
    }

    /** The line ax + by + c = 0, or null if a = b = 0. */
    private fun lineFrom(a: Double, b: Double, c: Double): Line? {
        val n2 = a * a + b * b
        if (n2 < 1e-24) return null
        val foot = Point(-a * c / n2, -b * c / n2)
        return Line(foot, foot + Point(-b, a))
    }

    /** A parabola's vertex in its turned frame, and its focal length p (focus p along the axis). */
    private fun parabolaVertex(s: ConicShape.Parabola): kotlin.Pair<Point, Double> {
        val y = -s.k1 / (2 * s.k2)
        val x = s.k2 * y * y + s.k1 * y + s.k0
        return Point(x, y) to 1 / (4 * s.k2)
    }

    private fun conicVertices(o: Conic): List<Point> = when (val s = o.shape) {
        is ConicShape.Ellipse -> listOf(Point(s.a, 0.0), Point(-s.a, 0.0), Point(0.0, s.b), Point(0.0, -s.b)).map { rotated(s.phi, it.x, it.y) + Point(s.cx, s.cy) }
        is ConicShape.Hyperbola -> listOf(Point(s.a, 0.0), Point(-s.a, 0.0)).map { rotated(s.phi, it.x, it.y) + Point(s.cx, s.cy) }
        is ConicShape.Parabola -> listOf(parabolaVertex(s).first.let { rotated(s.phi, it.x, it.y) })
        ConicShape.None -> throw GeometryError("This conic has no points")
    }

    private fun foci(o: Conic): List<Point> = when (val s = o.shape) {
        is ConicShape.Ellipse -> sqrt(maxOf(0.0, s.a * s.a - s.b * s.b)).let { c -> listOf(c, -c).map { rotated(s.phi, it, 0.0) + Point(s.cx, s.cy) } }
        is ConicShape.Hyperbola -> sqrt(s.a * s.a + s.b * s.b).let { c -> listOf(c, -c).map { rotated(s.phi, it, 0.0) + Point(s.cx, s.cy) } }
        is ConicShape.Parabola -> parabolaVertex(s).let { (v, p) -> listOf(rotated(s.phi, v.x + p, v.y)) }
        ConicShape.None -> throw GeometryError("This conic has no points")
    }

    /** Five points spread over a conic (to move it through a transformation). */
    private fun conicSamples(o: Conic): List<Point> = when (o.shape) {
        is ConicShape.Ellipse -> (0 until 5).map { conicAt(o.shape, it / 5.0)!! }
        is ConicShape.Hyperbola -> listOf(0.5, 1.0, 1.5, -0.6, -1.4).map { conicAt(o.shape, it)!! }
        is ConicShape.Parabola -> listOf(-0.6, -0.3, 0.0, 0.3, 0.6).map { conicAt(o.shape, it)!! }
        ConicShape.None -> throw GeometryError("This conic has no points")
    }

    /** Tangent lines from [p] to a conic: where (X − p) is along the curve, found along it. */
    private fun conicTangents(p: Point, o: Conic): List<Obj> {
        val c = o.c
        fun grad(q: Point) = Point(2 * c[0] * q.x + c[1] * q.y + c[3], c[1] * q.x + 2 * c[2] * q.y + c[4])
        // On the conic: the one tangent there.
        if (abs(conicValue(c, p)) < 1e-9 * (1 + c.maxOf { abs(it) })) {
            val n = grad(p)
            return listOf(Line(p, p + Point(-n.y, n.x)))
        }
        val touch = curveRoots(o) { q -> val g = grad(q); (q.x - p.x) * g.x + (q.y - p.y) * g.y }
        return touch.filter { (it - p).length > 1e-9 }.map { Line(p, it) }
    }

    // ---- Intersections -------------------------------------------------------------------

    /** The pieces an object is made of, for intersecting: straight ones, circles and arcs, conics, function graphs. */
    private fun pieces(o: Obj): List<Obj> = when (o) {
        is Line, is Segment, is Ray, is Vector, is Circle, is Arc, is Conic, is FunctionGraph -> listOf(o)
        is Polygon -> o.points.indices.map { k -> Segment(o.points[k], o.points[(k + 1) % o.points.size]) }
        is Polyline -> o.pieces.flatMap { piece -> (0 until piece.size - 1).map { Segment(piece[it], piece[it + 1]) } }
        is Many -> o.items.flatMap { pieces(it) }
        else -> throw GeometryError("Intersect lines, circles, arcs, conics, polygons or function graphs")
    }

    /** Where [a] and [b] cross, in a steady order (left to right, then bottom to top), duplicates removed. */
    fun intersections(a: Obj, b: Obj, xRange: ClosedFloatingPointRange<Double> = -50.0..50.0): List<Point> {
        val out = ArrayList<Point>()
        for (p in pieces(a)) for (q in pieces(b)) out += cross(p, q, xRange)
        val unique = ArrayList<Point>()
        for (p in out) if (p.x.isFinite() && p.y.isFinite() && unique.none { (it - p).length < 1e-7 }) unique += p
        return unique.sortedWith(compareBy({ Math.round(it.x * 1e7) }, { it.y }))
    }

    /** Whether parameter [t] along a straight piece lies on it (all of a line; t ≥ 0 on a ray; 0 ≤ t ≤ 1 on a segment). */
    private fun onPiece(o: Obj, t: Double) = when (o) {
        is Segment, is Vector -> t >= -1e-9 && t <= 1 + 1e-9
        is Ray -> t >= -1e-9
        else -> true
    }

    /** Whether a point of an arc's circle lies on the arc itself. */
    private fun onArc(o: Arc, p: Point): Boolean {
        val a = atan2(p.y - o.center.y, p.x - o.center.x)
        val along = if (o.sweep >= 0) ccw(a - o.start) else ccw(o.start - a)
        return along <= abs(o.sweep) + 1e-9 || along >= 2 * PI - 1e-9
    }

    /** A curve's implicit function: zero on it (a line's side, a circle's power, a conic's value). */
    private fun implicit(o: Obj): ((Point) -> Double)? = when (o) {
        is Circle -> { p -> (p.x - o.center.x) * (p.x - o.center.x) + (p.y - o.center.y) * (p.y - o.center.y) - o.r * o.r }
        is Arc -> { p -> (p.x - o.center.x) * (p.x - o.center.x) + (p.y - o.center.y) * (p.y - o.center.y) - o.r * o.r }
        is Conic -> { p -> conicValue(o.c, p) }
        is Line, is Segment, is Ray, is Vector -> { val (a, b) = asLine(o); val d = b - a; { p -> (p.x - a.x) * d.y - (p.y - a.y) * d.x } }
        else -> null
    }

    /** Whether a point found on [o]'s full curve is on [o] itself (an arc's part, a segment's stretch). */
    private fun keeps(o: Obj, p: Point): Boolean = when (o) {
        is Arc -> onArc(o, p)
        is Segment, is Vector, is Ray -> {
            val (a, b) = asLine(o); val d = b - a
            onPiece(o, ((p.x - a.x) * d.x + (p.y - a.y) * d.y) / (d.x * d.x + d.y * d.y))
        }
        else -> true
    }

    private fun cross(p: Obj, q: Obj, xRange: ClosedFloatingPointRange<Double>): List<Point> = when {
        isStraight(p) && isStraight(q) -> {
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
        // A function's graph: searched along x.
        p is FunctionGraph || q is FunctionGraph -> {
            val f = (if (p is FunctionGraph) p else q) as FunctionGraph
            val other = if (p is FunctionGraph) q else p
            val h: (Double) -> Double = when (other) {
                is FunctionGraph -> { x -> f.f(x) - other.f(x) }
                else -> { val g = implicit(other) ?: throw GeometryError("Can't intersect that with a function"); { x -> g(Point(x, f.f(x))) } }
            }
            roots(h, xRange.start, xRange.endInclusive, 4000).map { Point(it, f.f(it)) }.filter { keeps(other, it) }
        }
        q is Circle && p !is Circle && p !is Arc && p !is Conic -> cross(q, p, xRange)
        (p is Circle || p is Arc) && isStraight(q) -> {
            val (center, r) = if (p is Circle) p.center to p.r else (p as Arc).center to p.r
            val (a, b) = asLine(q)
            val d = b - a; val f = a - center
            val A = d.x * d.x + d.y * d.y; val B = 2 * (f.x * d.x + f.y * d.y); val C = f.x * f.x + f.y * f.y - r * r
            val disc = B * B - 4 * A * C
            when {
                disc < -1e-12 * A * r * r -> emptyList()
                abs(disc) <= 1e-12 * A * r * r -> listOf(-B / (2 * A))
                else -> listOf((-B - sqrt(disc)) / (2 * A), (-B + sqrt(disc)) / (2 * A))
            }.filter { onPiece(q, it) }.map { a + d.times(it) }.filter { keeps(p, it) }
        }
        isStraight(p) && (q is Circle || q is Arc) -> cross(q, p, xRange)
        (p is Circle || p is Arc) && (q is Circle || q is Arc) -> {
            val c1 = if (p is Circle) p else (p as Arc).let { Circle(it.center, it.r) }
            val c2 = if (q is Circle) q else (q as Arc).let { Circle(it.center, it.r) }
            circles(c1, c2).filter { keeps(p, it) && keeps(q, it) }
        }
        // A straight piece through a conic: a quadratic along it.
        isStraight(p) && q is Conic -> lineConic(p, q)
        p is Conic && isStraight(q) -> lineConic(q, p)
        // Curved and curved with a conic: one followed along, the other's equation along it.
        p is Conic -> { val g = implicit(q)!!; curveRoots(p, g).filter { keeps(q, it) } }
        q is Conic -> { val g = implicit(p)!!; curveRoots(q, g).filter { keeps(p, it) } }
        else -> emptyList()
    }

    private fun lineConic(l: Obj, o: Conic): List<Point> {
        val (a, b) = asLine(l); val d = b - a; val c = o.c
        val qa = c[0] * d.x * d.x + c[1] * d.x * d.y + c[2] * d.y * d.y
        val qb = 2 * c[0] * a.x * d.x + c[1] * (a.x * d.y + a.y * d.x) + 2 * c[2] * a.y * d.y + c[3] * d.x + c[4] * d.y
        val qc = conicValue(c, a)
        val ts = if (abs(qa) < 1e-12 * (abs(qb) + abs(qc) + 1e-300)) {
            if (abs(qb) < 1e-300) emptyList() else listOf(-qc / qb)
        } else {
            val disc = qb * qb - 4 * qa * qc
            when {
                disc < -1e-12 * qb * qb -> emptyList()
                disc <= 1e-12 * qb * qb -> listOf(-qb / (2 * qa))
                else -> listOf((-qb - sqrt(disc)) / (2 * qa), (-qb + sqrt(disc)) / (2 * qa))
            }
        }
        return ts.filter { onPiece(l, it) }.map { a + d.times(it) }
    }

    /** Where [g] is zero along a conic (sampled over its parameter, then bisected). */
    private fun curveRoots(o: Conic, g: (Point) -> Double): List<Point> {
        val shape = o.shape
        val ranges = when (shape) {
            is ConicShape.Ellipse -> listOf(0.0 to 1.0)
            is ConicShape.Hyperbola -> listOf(-1.9999 to -0.0001, 0.0001 to 1.9999)
            is ConicShape.Parabola -> listOf(-0.9999 to 0.9999)
            ConicShape.None -> return emptyList()
        }
        return ranges.flatMap { (lo, hi) -> roots({ t -> g(conicAt(shape, t)!!) }, lo, hi, 4000).map { conicAt(shape, it)!! } }
    }

    /** Zeros of [h] on [lo, hi]: sign changes over [n] steps, bisected (jumps through a pole left out). */
    private fun roots(h: (Double) -> Double, lo: Double, hi: Double, n: Int): List<Double> {
        val out = ArrayList<Double>()
        var x0 = lo; var y0 = runCatching { h(lo) }.getOrDefault(Double.NaN)
        val scale = (0..32).mapNotNull { k -> runCatching { h(lo + (hi - lo) * k / 32) }.getOrNull()?.takeIf { it.isFinite() }?.let { abs(it) } }.maxOrNull() ?: 1.0
        for (k in 1..n) {
            val x1 = lo + (hi - lo) * k / n
            val y1 = runCatching { h(x1) }.getOrDefault(Double.NaN)
            if (y0.isFinite() && y1.isFinite()) {
                if (y0 == 0.0) out += x0
                else if (y0 * y1 < 0) {
                    var a = x0; var b = x1; var fa = y0
                    repeat(80) {
                        val m = (a + b) / 2; val fm = h(m)
                        if (fa * fm <= 0) b = m else { a = m; fa = fm }
                    }
                    val r = (a + b) / 2
                    // A real zero, not a jump across a pole.
                    if (abs(h(r)) < 1e-6 * maxOf(1.0, scale)) out += r
                } else if (k > 1) {
                    // A touching zero (no sign change): a small minimum of |h| near zero.
                    val mid = (x0 + x1) / 2
                    val ym = runCatching { h(mid) }.getOrDefault(Double.NaN)
                    if (ym.isFinite() && abs(ym) < abs(y0) && abs(ym) < abs(y1) && abs(ym) < 1e-9 * maxOf(1.0, scale)) out += mid
                }
            }
            x0 = x1; y0 = y1
        }
        if (y0 == 0.0) out += x0
        return out
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
        val lines: List<List<kotlin.Pair<Double, Double>>> = emptyList(),
        val points: List<Point> = emptyList(),
        val fill: List<kotlin.Pair<Double, Double>>? = null,
        val arrow: Boolean = false,
        val labels: List<kotlin.Pair<Point, String>> = emptyList(),
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
                Drawing(lines = listOf((0..n).map { k -> val t = 2 * PI * k / n; (o.center.x + o.r * cos(t)) to (o.center.y + o.r * sin(t)) }))
            }
            is Arc -> {
                val n = maxOf(8, (abs(o.sweep) / (2 * PI) * 240).toInt())
                val arc = (0..n).map { k -> val t = o.start + o.sweep * k / n; (o.center.x + o.r * cos(t)) to (o.center.y + o.r * sin(t)) }
                if (o.sector) Drawing(lines = listOf(listOf(pt(o.center)) + arc + listOf(pt(o.center))), fill = listOf(pt(o.center)) + arc)
                else Drawing(lines = listOf(arc))
            }
            is Conic -> Drawing(lines = conicPaths(o.shape, far))
            is Polygon -> {
                val path = o.points.map { pt(it) }
                Drawing(lines = listOf(path + path.first()), fill = path)
            }
            is Angle -> {
                // An arc a fraction of the view across, and the value just outside it.
                val r = 0.06 * minOf(abs(view.xMax - view.xMin), abs(view.yMax - view.yMin))
                val n = maxOf(8, (o.sweep / (2 * PI) * 96).toInt())
                val arc = (0..n).map { k -> val t = o.start + o.sweep * k / n; (o.vertex.x + r * cos(t)) to (o.vertex.y + r * sin(t)) }
                val midA = o.start + o.sweep / 2
                val wedge = listOf(pt(o.vertex)) + arc
                Drawing(lines = listOf(arc), fill = wedge, labels = listOf(Point(o.vertex.x + 1.9 * r * cos(midA), o.vertex.y + 1.9 * r * sin(midA)) to angleText(o.sweep)))
            }
            is Many -> o.items.map { draw(it, view, angleText) }.let { ds ->
                Drawing(lines = ds.flatMap { it.lines }, points = ds.flatMap { it.points }, labels = ds.flatMap { it.labels })
            }
            is Polyline -> Drawing(lines = o.pieces.map { piece -> piece.map { pt(it) } })
            // A function's graph is drawn by its own line; a number isn't drawn.
            is FunctionGraph, is ComplexMap, is Number, is Bool, is Text -> Drawing()
        }
    }

    /** A conic as polylines reaching [far] from its center (two for a hyperbola's branches). */
    private fun conicPaths(s: ConicShape, far: Double): List<List<kotlin.Pair<Double, Double>>> = when (s) {
        is ConicShape.Ellipse -> listOf((0..360).map { k -> conicAt(s, k / 360.0)!!.let { it.x to it.y } })
        is ConicShape.Hyperbola -> {
            val umax = kotlin.math.asinh(far / minOf(s.a, s.b))
            listOf(1.0, -1.0).map { branch ->
                (0..400).map { k ->
                    val u = umax * (2.0 * k / 400 - 1)
                    rotated(s.phi, branch * s.a * kotlin.math.cosh(u), s.b * kotlin.math.sinh(u)).let { (it.x + s.cx) to (it.y + s.cy) }
                }
            }
        }
        is ConicShape.Parabola -> {
            val ymax = far + sqrt(far / maxOf(abs(s.k2), 1e-12))
            listOf((0..600).map { k ->
                val v = 2.0 * k / 600 - 1
                val y = ymax * v * v * v
                rotated(s.phi, s.k2 * y * y + s.k1 * y + s.k0, y).let { it.x to it.y }
            })
        }
        ConicShape.None -> emptyList()
    }

    /** Polylines an object is drawn with, for finding what a tap is on (empty for points and numbers). */
    fun outline(o: Obj, view: Viewport): List<List<kotlin.Pair<Double, Double>>> = when (o) {
        is Point, is Number, is Angle, is FunctionGraph, is ComplexMap, is Bool, is Text -> emptyList()
        else -> draw(o, view).lines
    }

    // ---- Names ---------------------------------------------------------------------------

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

    /** A name for a line, circle or number: f, g, h, … (skipping letters with other meanings), then f₁, … */
    fun nextObjectName(used: Set<String>): String {
        val letters = "fghjklmnpqsuvwabcdo".toList()
        for (c in letters) if (c.toString() !in used) return c.toString()
        var k = 1
        while (true) {
            for (c in letters) {
                val name = com.example.cas.cas.CustomSymbol(c.toString(), sub = k.toString()).encode()
                if (name !in used) return name
            }
            k++
        }
    }

    /** A name for an angle: α, β, γ, … */
    fun nextAngleName(used: Set<String>): String {
        for (c in "αβγδεζηικλμνξρστυφχψω") if (c.toString() !in used) return c.toString()
        return nextObjectName(used)
    }
}
