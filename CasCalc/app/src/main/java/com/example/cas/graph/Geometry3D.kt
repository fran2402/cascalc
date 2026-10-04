package com.example.cas.graph

import com.example.cas.editor.Func
import com.example.cas.editor.MathRow
import com.example.cas.editor.Node
import com.example.cas.editor.Sym
import com.example.cas.graph.Geometry.Command
import com.example.cas.graph.Geometry.Ex
import com.example.cas.graph.Geometry.GeometryError
import com.example.cas.graph.Geometry.Statement
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geometry in space, for the 3D graph's geometry mode: points written (x, y, z), lines, planes,
 * spheres, circles, polygons and solids (pyramids, prisms, cubes), built on each other with
 * commands like Plane(A, B, C) or Intersect(l, p), measured (distances, angles, areas, volumes)
 * and moved (reflected in a plane, turned about a line). Lines are read by the 2D geometry's
 * reader, with this set of commands; the objects are drawn into the 3D graph's box.
 */
object Geometry3D {

    // ---- Objects --------------------------------------------------------------------------

    sealed class Obj

    data class Point(val x: Double, val y: Double, val z: Double) : Obj() {
        operator fun plus(o: Point) = Point(x + o.x, y + o.y, z + o.z)
        operator fun minus(o: Point) = Point(x - o.x, y - o.y, z - o.z)
        fun times(k: Double) = Point(x * k, y * k, z * k)
        infix fun dot(o: Point) = x * o.x + y * o.y + z * o.z
        infix fun cross(o: Point) = Point(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
        val length get() = sqrt(x * x + y * y + z * z)
        fun array() = doubleArrayOf(x, y, z)
    }

    data class Line(val a: Point, val b: Point) : Obj()
    data class Segment(val a: Point, val b: Point) : Obj()
    data class Ray(val a: Point, val b: Point) : Obj()
    data class Vector(val a: Point, val b: Point) : Obj()
    /** Through [p], at right angles to the unit vector [n]. */
    data class Plane(val p: Point, val n: Point) : Obj()
    data class Sphere(val center: Point, val r: Double) : Obj()
    /** A circle about [center] in the plane at right angles to the unit vector [n]. */
    data class Circle(val center: Point, val n: Point, val r: Double) : Obj()
    data class Polygon(val points: List<Point>) : Obj()
    /** A solid as its faces (each a loop of corners, counterclockwise from outside): a pyramid, prism or cube. */
    data class Solid(val faces: List<List<Point>>, val kind: String) : Obj()
    /** The angle at [vertex] from direction [u] to direction [v] (unit vectors), and its size. */
    data class Angle(val vertex: Point, val u: Point, val v: Point, val size: Double) : Obj()
    enum class Unit { None, Length, Angle, Area, Volume }
    data class Number(val value: Double, val unit: Unit = Unit.None) : Obj()
    data class Bool(val value: Boolean) : Obj()
    data class Many(val items: List<Obj>) : Obj()

    // ---- Commands -------------------------------------------------------------------------

    val COMMANDS = listOf(
        Command("Point", "Point(l, t)", "A point on a line, segment or circle, at t along it; Point(A, v) is A moved by v"),
        Command("Segment", "Segment(A, B)", "The segment from A to B"),
        Command("Line", "Line(A, B)", "The line through A and B; Line(A, l) through A parallel to l"),
        Command("Ray", "Ray(A, B)", "The ray from A through B"),
        Command("Vector", "Vector(A, B)", "The arrow from A to B (from the origin with one point)"),
        Command("Plane", "Plane(A, B, C)", "The plane through three points; Plane(A, p) through A parallel to the plane p; Plane(A, l) through A and the line l; Plane(poly) a polygon's plane"),
        Command("PerpendicularPlane", "PerpendicularPlane(A, l)", "The plane through A at right angles to a line or vector"),
        Command("PerpendicularLine", "PerpendicularLine(A, p)", "The line through A at right angles to a plane, or to a line (through its nearest point)", listOf("Perpendicular")),
        Command("ParallelLine", "ParallelLine(A, l)", "The line through A parallel to l", listOf("Parallel")),
        Command("Sphere", "Sphere(C, r)", "The sphere about C with radius r, or through a point"),
        Command("Circle", "Circle(A, B, C)", "The circle through three points; Circle(C, r, v) about C with radius r, at right angles to a vector or line (or in a plane)"),
        Command("Polygon", "Polygon(A, B, C)", "The polygon with these corners"),
        Command("Pyramid", "Pyramid(poly, A)", "The pyramid on a polygon with apex A; Pyramid(A, B, C, D) a tetrahedron (the last point is the apex)", listOf("Tetrahedron")),
        Command("Prism", "Prism(poly, A)", "The prism on a polygon, its first corner's edge going to A (or along a vector)"),
        Command("Cube", "Cube(A, B)", "The cube with edge AB, standing up from it"),
        Command("Midpoint", "Midpoint(A, B)", "The point halfway from A to B (or the middle of a segment)"),
        Command("Center", "Center(s)", "The center of a sphere, circle or polygon"),
        Command("Centroid", "Centroid(poly)", "The center of mass of a polygon or solid's corners (or of points)"),
        Command("Intersect", "Intersect(a, b)", "Where two objects meet: lines, planes, spheres; a plane and a sphere meet in a circle, two planes in a line; Intersect(a, b, n) picks the nth point"),
        Command("ClosestPoint", "ClosestPoint(s, A)", "The point of a line, plane or sphere nearest A"),
        Command("Distance", "Distance(A, s)", "How far apart two points, a point and a line, plane or sphere, two skew lines or two parallel planes are"),
        Command("Angle", "Angle(A, B, C)", "The angle at B; Angle(a, b) between two lines or vectors, a line and a plane, or two planes"),
        Command("Length", "Length(s)", "The length of a segment or vector, a polygon's perimeter, a circle's circumference"),
        Command("Perimeter", "Perimeter(poly)", "The perimeter of a polygon or circle"),
        Command("Area", "Area(s)", "The area of a polygon or circle, a sphere's or a solid's surface"),
        Command("Volume", "Volume(s)", "The volume of a sphere or solid"),
        Command("Radius", "Radius(s)", "A sphere's or circle's radius"),
        Command("Dot", "Dot(u, v)", "The dot product of two vectors", listOf("DotProduct")),
        Command("Cross", "Cross(u, v)", "The cross product of two vectors, as a vector", listOf("CrossProduct")),
        Command("UnitVector", "UnitVector(v)", "The vector of length 1 along a vector or line"),
        Command("PerpendicularVector", "PerpendicularVector(p)", "A plane's normal vector (from its point), or one at right angles to a vector", listOf("Normal")),
        Command("Direction", "Direction(l)", "A line's direction, as a vector"),
        Command("Reflect", "Reflect(obj, p)", "The mirror image in a plane, or through a point"),
        Command("Rotate", "Rotate(obj, θ, l)", "Turned by θ about a line (about the z-axis without l)"),
        Command("Translate", "Translate(obj, v)", "Moved by a vector"),
        Command("Dilate", "Dilate(obj, k, A)", "Scaled by k from A (from the origin without A)"),
        Command("AreCollinear", "AreCollinear(A, B, C)", "Whether three points are on one line"),
        Command("AreCoplanar", "AreCoplanar(A, B, C, D)", "Whether four points are in one plane"),
        Command("AreParallel", "AreParallel(a, b)", "Whether two lines, a line and a plane, or two planes are parallel"),
        Command("ArePerpendicular", "ArePerpendicular(a, b)", "Whether two lines, a line and a plane, or two planes are at right angles"),
    )

    private val byName: Map<String, Command> = COMMANDS.flatMap { c -> (listOf(c.name) + c.aliases).map { it.lowercase() to c } }.toMap()
    fun command(word: String): Command? = byName[word.lowercase()]

    /** The row as a construction in space, or null (a surface, a curve, an unnamed (a, b, c) point). */
    fun parse(items: List<Node>): Statement? = Geometry.parseWith(items, ::command, space = true)

    /** A point written with plain numbers (A = (1, 2, 3)): it can be moved. */
    fun isFree(s: Statement): Boolean = s.name != null && (s.expr as? Ex.Triple)?.let { plain(it.x) != null && plain(it.y) != null && plain(it.z) != null } == true

    private fun plain(e: Ex): Double? = when (e) {
        is Ex.Lit -> e.value
        is Ex.Neg -> plain(e.a)?.let { -it }
        else -> null
    }

    // ---- Working a construction out ---------------------------------------------------------

    class Outcome(val obj: Obj?, val error: String?)

    private class Missing(val name: String) : Exception("$name isn't defined")

    /** Every statement worked out, each with the objects named on other lines (as the 2D geometry's build). */
    fun build(statements: List<Statement?>, number: (List<Node>, Map<String, Double>) -> Double, degrees: Boolean = false, onStatement: (Int) -> kotlin.Unit = {}): List<Outcome?> {
        val out = arrayOfNulls<Outcome>(statements.size)
        val env = LinkedHashMap<String, Obj>()
        val defined = statements.mapNotNull { it?.name }.toSet()
        val waiting = statements.indices.filter { statements[it] != null }.toMutableList()
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

    fun value(o: Obj): Double? = when (o) {
        is Number -> o.value
        is Angle -> o.size
        else -> null
    }

    private fun digits(v: Double): MathRow {
        val s = java.math.BigDecimal(v).round(java.math.MathContext(17)).stripTrailingZeros().toPlainString()
        val row = MathRow()
        s.forEach { ch -> row.add(row.items.size, Sym(if (ch == '-') "−" else ch.toString())) }
        return row
    }

    private class Evaluation(val env: Map<String, Obj>, val defined: Set<String>, val numberOf: (List<Node>, Map<String, Double>) -> Double, val degrees: Boolean) {
        fun numbers(): Map<String, Double> = env.mapNotNull { (k, v) -> value(v)?.let { k to it } }.toMap()

        fun obj(e: Ex): Obj = when (e) {
            is Ex.Ref -> env[e.name] ?: if (e.name in defined || Geometry.isPointName(Sym(e.name))) throw Missing(e.name) else Number(numberOf(listOf(Sym(e.name)), numbers()))
            is Ex.Triple -> Point(num(obj(e.x)), num(obj(e.y)), num(obj(e.z)))
            is Ex.Pair -> throw GeometryError("A point in space has three coordinates: (x, y, z)")
            is Ex.Lit -> Number(e.value)
            is Ex.Delegate -> Number(numberOf(e.nodes, numbers()))
            is Ex.Call -> call(e.command, e.args.map { obj(it) })
            is Ex.Neg -> when (val a = obj(e.a)) {
                is Point -> a.times(-1.0)
                is Vector -> Vector(a.b, a.a)
                else -> Number(-num(a), (a as? Number)?.unit ?: Unit.None)
            }
            is Ex.Bin -> arithmetic(e.op, obj(e.a), obj(e.b))
            is Ex.Coordinate -> {
                val p = when (val a = obj(e.of)) { is Point -> a; is Vector -> a.b - a.a; else -> throw GeometryError("${e.which}( ) takes a point or vector") }
                Number(when (e.which) { 'x' -> p.x; 'y' -> p.y; else -> p.z })
            }
            is Ex.Fn -> Number(numberOf(listOf(Func(e.name, e.args.map { digits(num(obj(it))) })), numbers()))
            is Ex.Sqrt -> Number(sqrt(num(obj(e.a))))
            is Ex.Root -> Number(Math.pow(num(obj(e.a)), 1 / num(obj(e.index))))
            is Ex.Degrees -> Number(Math.toRadians(num(obj(e.a))), Unit.Angle)
        }

        fun num(o: Obj): Double = value(o) ?: throw GeometryError("Expected a number here")
        fun point(o: Obj): Point = o as? Point ?: throw GeometryError("Expected a point here")
        fun angle(o: Obj): Double = when {
            o is Number && o.unit == Unit.Angle -> o.value
            o is Angle -> o.size
            else -> num(o).let { if (degrees) Math.toRadians(it) else it }
        }

        fun arithmetic(op: Char, a: Obj, b: Obj): Obj {
            fun vec(o: Obj): Point? = when (o) { is Point -> o; is Vector -> o.b - o.a; else -> null }
            val va = vec(a); val vb = vec(b); val na = value(a); val nb = value(b)
            return when {
                na != null && nb != null -> Number(when (op) { '+' -> na + nb; '-' -> na - nb; '*' -> na * nb; '/' -> na / nb; else -> Math.pow(na, nb) })
                va != null && vb != null -> when (op) {
                    '+' -> va + vb
                    '-' -> va - vb
                    '*' -> Number(va dot vb)
                    else -> throw GeometryError("Points can be added, subtracted and scaled")
                }
                va != null && nb != null -> when (op) { '*' -> va.times(nb); '/' -> va.times(1 / nb); else -> throw GeometryError("A point and a number only multiply or divide") }
                na != null && vb != null && op == '*' -> vb.times(na)
                else -> throw GeometryError("Can't do arithmetic with that")
            }
        }

        fun call(c: Command, a: List<Obj>): Obj {
            fun need(vararg counts: Int) { if (a.size !in counts) throw GeometryError("${c.name} takes ${counts.joinToString(" or ")} things: ${c.usage}") }
            return when (c.name) {
                "Point" -> { need(1, 2)
                    when (val o = a[0]) {
                        is Point -> if (a.size == 2) o + vecOf(a[1]) else o
                        else -> pointAt(o, if (a.size == 2) num(a[1]) else 0.0)
                    }
                }
                "Segment" -> { need(2); Segment(point(a[0]), point(a[1])) }
                "Line" -> { need(2)
                    val p = point(a[0])
                    when (val q = a[1]) { is Point -> { if ((q - p).length < 1e-12) throw GeometryError("The two points are the same"); Line(p, q) }; else -> Line(p, p + direction(q)) }
                }
                "Ray" -> { need(2); Ray(point(a[0]), point(a[1])) }
                "Vector" -> if (a.size == 1) Vector(Point(0.0, 0.0, 0.0), vecOf(a[0])) else { need(2); Vector(point(a[0]), point(a[1])) }
                "Plane" -> when (a.size) {
                    1 -> when (val o = a[0]) { is Polygon -> planeThrough(o.points[0], o.points[1], o.points[2]); is Plane -> o; else -> throw GeometryError("Plane(poly): a polygon") }
                    2 -> { val p = point(a[0])
                        when (val o = a[1]) {
                            is Plane -> Plane(p, o.n)
                            is Line, is Segment, is Ray, is Vector -> { val (q, r) = ends(o); planeThrough(p, q, r) }
                            else -> throw GeometryError("Plane(A, p): p is a plane or a line")
                        }
                    }
                    else -> { need(3); planeThrough(point(a[0]), point(a[1]), point(a[2])) }
                }
                "PerpendicularPlane" -> { need(2); Plane(point(a[0]), direction(a[1])) }
                "PerpendicularLine" -> { need(2)
                    val p = point(a[0])
                    when (val o = a[1]) {
                        is Plane -> Line(p, p + o.n)
                        is Line, is Segment, is Ray, is Vector -> {
                            val foot = closest(o, p, unbounded = true)
                            if ((foot - p).length < 1e-12) {
                                // On the line: any direction at right angles to it; one across, level if it can be.
                                val d = direction(o)
                                Line(p, p + unit(d cross (if (abs(d.z) < 0.9) Point(0.0, 0.0, 1.0) else Point(1.0, 0.0, 0.0))))
                            } else Line(p, foot)
                        }
                        else -> throw GeometryError("PerpendicularLine(A, p): p is a plane or a line")
                    }
                }
                "ParallelLine" -> { need(2); val p = point(a[0]); Line(p, p + direction(a[1])) }
                "Sphere" -> { need(2)
                    val c0 = point(a[0])
                    Sphere(c0, when (val o = a[1]) { is Point -> (o - c0).length; is Segment -> (o.b - o.a).length; else -> abs(num(o)) })
                }
                "Circle" -> when (a.size) {
                    3 -> if (a.all { it is Point }) circleThrough(point(a[0]), point(a[1]), point(a[2]))
                        else {
                            val c0 = point(a[0])
                            val r = when (val o = a[1]) { is Point -> (o - c0).length; else -> abs(num(o)) }
                            Circle(c0, when (val o = a[2]) { is Plane -> o.n; else -> direction(o) }, r)
                        }
                    else -> throw GeometryError("Circle(A, B, C), or Circle(C, r, v)")
                }
                "Polygon" -> { if (a.size < 3) throw GeometryError("A polygon needs three corners or more"); Polygon(a.map { point(it) }) }
                "Pyramid" -> {
                    val (base, apex) = when {
                        a.size == 2 && a[0] is Polygon -> (a[0] as Polygon).points to point(a[1])
                        a.size >= 4 -> a.dropLast(1).map { point(it) } to point(a.last())
                        else -> throw GeometryError("Pyramid(poly, A), or Pyramid(A, B, C, D)")
                    }
                    pyramid(base, apex)
                }
                "Prism" -> { need(2)
                    val base = (a[0] as? Polygon)?.points ?: throw GeometryError("Prism(poly, A): poly is a polygon")
                    val shift = when (val o = a[1]) { is Point -> o - base[0]; else -> vecOf(o) }
                    prism(base, shift)
                }
                "Cube" -> { need(2); cube(point(a[0]), point(a[1])) }
                "Midpoint" -> if (a.size == 1) { val (p, q) = ends(a[0]); mid(p, q) } else { need(2); mid(point(a[0]), point(a[1])) }
                "Center" -> { need(1)
                    when (val o = a[0]) { is Sphere -> o.center; is Circle -> o.center; is Polygon -> centroid(o.points); is Solid -> centroid(o.faces.flatten().distinct()); else -> throw GeometryError("Center of a sphere, circle or polygon") }
                }
                "Centroid" -> if (a.size >= 2) centroid(a.map { point(it) }) else when (val o = a[0]) {
                    is Polygon -> centroid(o.points); is Solid -> centroid(o.faces.flatten().distinct()); else -> throw GeometryError("Expected a polygon, a solid or points")
                }
                "Intersect" -> { need(2, 3)
                    val hit = intersect(a[0], a[1])
                    if (a.size == 3) {
                        val pts = (hit as? Many)?.items ?: listOf(hit)
                        pts.getOrNull(num(a[2]).toInt() - 1) ?: throw GeometryError("There ${if (pts.size == 1) "is only 1 point" else "are only ${pts.size} points"}")
                    } else hit
                }
                "ClosestPoint" -> { need(2); closest(a[0], point(a[1]), unbounded = false) }
                "Distance" -> { need(2)
                    val x = a[0]; val y = a[1]
                    Number(when {
                        x is Point -> distance(x, y)
                        y is Point -> distance(y, x)
                        isStraight(x) && isStraight(y) -> skew(x, y)
                        x is Plane && y is Plane -> { if ((x.n cross y.n).length > 1e-9) 0.0 else abs((y.p - x.p) dot x.n) }
                        else -> throw GeometryError("Distance between points, lines, planes or spheres")
                    }, Unit.Length)
                }
                "Angle" -> when (a.size) {
                    3 -> { val b = point(a[1]); val u = unit(point(a[0]) - b); val v = unit(point(a[2]) - b); Angle(b, u, v, acos((u dot v).coerceIn(-1.0, 1.0))) }
                    2 -> Number(angleBetween(a[0], a[1]), Unit.Angle)
                    else -> throw GeometryError("Angle(A, B, C), or Angle(a, b)")
                }
                "Length" -> { need(1)
                    Number(when (val o = a[0]) {
                        is Segment -> (o.b - o.a).length; is Vector -> (o.b - o.a).length; is Point -> o.length
                        is Polygon -> perimeter(o.points); is Circle -> 2 * PI * o.r
                        else -> throw GeometryError("Length of a segment, vector, polygon or circle")
                    }, Unit.Length)
                }
                "Perimeter" -> { need(1); Number(when (val o = a[0]) { is Polygon -> perimeter(o.points); is Circle -> 2 * PI * o.r; else -> throw GeometryError("Perimeter of a polygon or circle") }, Unit.Length) }
                "Area" -> {
                    if (a.size >= 3) Number(area(a.map { point(it) }), Unit.Area) else { need(1)
                        Number(when (val o = a[0]) {
                            is Polygon -> area(o.points); is Circle -> PI * o.r * o.r; is Sphere -> 4 * PI * o.r * o.r
                            is Solid -> o.faces.sumOf { area(it) }
                            else -> throw GeometryError("Area of a polygon, circle, sphere or solid")
                        }, Unit.Area)
                    }
                }
                "Volume" -> { need(1)
                    Number(when (val o = a[0]) { is Sphere -> 4.0 / 3 * PI * o.r * o.r * o.r; is Solid -> volume(o); else -> throw GeometryError("Volume of a sphere or solid") }, Unit.Volume)
                }
                "Radius" -> { need(1); Number(when (val o = a[0]) { is Sphere -> o.r; is Circle -> o.r; else -> throw GeometryError("Radius of a sphere or circle") }, Unit.Length) }
                "Dot" -> { need(2); Number(vecOf(a[0]) dot vecOf(a[1])) }
                "Cross" -> { need(2); Vector(Point(0.0, 0.0, 0.0), vecOf(a[0]) cross vecOf(a[1])) }
                "UnitVector" -> { need(1); val from = (a[0] as? Vector)?.a ?: Point(0.0, 0.0, 0.0); Vector(from, from + unit(direction(a[0]))) }
                "PerpendicularVector" -> { need(1)
                    when (val o = a[0]) {
                        is Plane -> Vector(o.p, o.p + o.n)
                        else -> { val d = vecOf(o); val q = d cross (if (abs(d.z) < 0.9 * d.length) Point(0.0, 0.0, 1.0) else Point(1.0, 0.0, 0.0)); Vector(Point(0.0, 0.0, 0.0), unit(q).times(d.length)) }
                    }
                }
                "Direction" -> { need(1); val (p, q) = ends(a[0]); Vector(Point(0.0, 0.0, 0.0), q - p) }
                "Reflect" -> { need(2)
                    when (val m = a[1]) {
                        is Plane -> transform(a[0]) { p -> p - m.n.times(2 * ((p - m.p) dot m.n)) }
                        is Point -> transform(a[0]) { p -> m.times(2.0) - p }
                        else -> throw GeometryError("Reflect(obj, p): p is a plane or a point")
                    }
                }
                "Rotate" -> { need(2, 3)
                    val t = angle(a[1])
                    val (o, d) = if (a.size == 3) ends(a[2]).let { (p, q) -> p to unit(q - p) } else Point(0.0, 0.0, 0.0) to Point(0.0, 0.0, 1.0)
                    transform(a[0]) { p -> rotate(p, o, d, t) }
                }
                "Translate" -> { need(2); val v = vecOf(a[1]); transform(a[0]) { p -> p + v } }
                "Dilate" -> { need(2, 3)
                    val k = num(a[1]); val c0 = if (a.size == 3) point(a[2]) else Point(0.0, 0.0, 0.0)
                    transform(a[0], scale = k) { p -> c0 + (p - c0).times(k) }
                }
                "AreCollinear" -> { need(3); val p = point(a[0]); Bool(((point(a[1]) - p) cross (point(a[2]) - p)).length <= 1e-9 * maxOf(1.0, (point(a[1]) - p).length * (point(a[2]) - p).length)) }
                "AreCoplanar" -> { need(4)
                    val p = point(a[0]); val u = point(a[1]) - p; val v = point(a[2]) - p; val w = point(a[3]) - p
                    Bool(abs((u cross v) dot w) <= 1e-9 * maxOf(1.0, u.length * v.length * w.length))
                }
                "AreParallel", "ArePerpendicular" -> { need(2)
                    val ang = angleBetween(a[0], a[1])
                    Bool(if (c.name == "AreParallel") abs(ang) < 1e-9 || abs(ang - PI) < 1e-9 else abs(ang - PI / 2) < 1e-9)
                }
                else -> throw GeometryError("${c.name} isn't available in 3D")
            }
        }

        fun vecOf(o: Obj): Point = when (o) { is Vector -> o.b - o.a; is Point -> o; else -> direction(o) }
    }

    // ---- Helpers --------------------------------------------------------------------------

    private fun unit(p: Point): Point { val l = p.length; if (l < 1e-300) throw GeometryError("The points are the same"); return p.times(1 / l) }
    private fun mid(p: Point, q: Point) = Point((p.x + q.x) / 2, (p.y + q.y) / 2, (p.z + q.z) / 2)
    private fun centroid(pts: List<Point>) = Point(pts.sumOf { it.x } / pts.size, pts.sumOf { it.y } / pts.size, pts.sumOf { it.z } / pts.size)
    private fun isStraight(o: Obj) = o is Line || o is Segment || o is Ray || o is Vector

    private fun ends(o: Obj): kotlin.Pair<Point, Point> = when (o) {
        is Line -> o.a to o.b; is Segment -> o.a to o.b; is Ray -> o.a to o.b; is Vector -> o.a to o.b
        else -> throw GeometryError("Expected a line, segment, ray or vector")
    }

    private fun direction(o: Obj): Point = when (o) {
        is Point -> unit(o)
        is Plane -> o.n
        else -> ends(o).let { (p, q) -> unit(q - p) }
    }

    private fun planeThrough(a: Point, b: Point, c: Point): Plane {
        val n = (b - a) cross (c - a)
        if (n.length < 1e-12 * maxOf(1.0, (b - a).length * (c - a).length)) throw GeometryError("The points are on one line")
        return Plane(a, unit(n))
    }

    private fun circleThrough(a: Point, b: Point, c: Point): Circle {
        val u = b - a; val v = c - a
        val n = u cross v
        val nn = n dot n
        if (nn < 1e-24) throw GeometryError("The three points are on one line")
        // The center: a + (|u|² v × n + |v|² n × u) / (2 |n|²).
        val center = a + ((v cross n).times(u dot u) + (n cross u).times(v dot v)).times(1 / (2 * nn))
        return Circle(center, unit(n), (a - center).length)
    }

    private fun perimeter(pts: List<Point>) = pts.indices.sumOf { k -> (pts[(k + 1) % pts.size] - pts[k]).length }

    /** A flat polygon's area (half the length of the summed cross products). */
    fun area(pts: List<Point>): Double {
        var s = Point(0.0, 0.0, 0.0)
        for (k in pts.indices) s += pts[k] cross pts[(k + 1) % pts.size]
        return s.length / 2
    }

    /** A closed solid's volume: the signed volumes of tetrahedra from the origin to each face. */
    fun volume(s: Solid): Double = abs(s.faces.sumOf { f -> (1 until f.size - 1).sumOf { k -> (f[0] dot (f[k] cross f[k + 1])) / 6 } })

    private fun pyramid(base: List<Point>, apex: Point): Solid {
        val c = centroid(base)
        // The base wound so its outside faces away from the apex.
        val b = if ((normalOf(base) dot (apex - c)) > 0) base.reversed() else base
        return Solid(listOf(b) + b.indices.map { k -> listOf(b[(k + 1) % b.size], b[k], apex) }, if (base.size == 3) "Tetrahedron" else "Pyramid")
    }

    private fun prism(base: List<Point>, shift: Point): Solid {
        val b = if ((normalOf(base) dot shift) > 0) base.reversed() else base
        val top = b.map { it + shift }
        return Solid(listOf(b, top.reversed()) + b.indices.map { k -> val j = (k + 1) % b.size; listOf(b[k], b[j], top[j], top[k]) }.map { it.reversed() }, "Prism")
    }

    private fun cube(a: Point, b: Point): Solid {
        val u = b - a
        if (u.length < 1e-12) throw GeometryError("The two points are the same")
        val side = unit(u cross (if (abs(unit(u).z) < 0.95) Point(0.0, 0.0, 1.0) else Point(1.0, 0.0, 0.0))).times(u.length)
        val up = unit(side cross u).times(u.length).let { if (it.z < 0 || (abs(it.z) < 1e-12 && it.x < 0)) it.times(-1.0) else it }
        return prism(listOf(a, b, b + side, a + side), up).copy(kind = "Cube")
    }

    private fun normalOf(pts: List<Point>): Point {
        var s = Point(0.0, 0.0, 0.0)
        for (k in pts.indices) s += pts[k] cross pts[(k + 1) % pts.size]
        return s
    }

    private fun rotate(p: Point, o: Point, d: Point, t: Double): Point {
        // Rodrigues: v cos t + (d × v) sin t + d (d · v)(1 − cos t).
        val v = p - o
        return o + v.times(cos(t)) + (d cross v).times(sin(t)) + d.times((d dot v) * (1 - cos(t)))
    }

    private fun transform(o: Obj, scale: Double = 1.0, f: (Point) -> Point): Obj = when (o) {
        is Point -> f(o)
        is Line -> Line(f(o.a), f(o.b)); is Segment -> Segment(f(o.a), f(o.b)); is Ray -> Ray(f(o.a), f(o.b)); is Vector -> Vector(f(o.a), f(o.b))
        is Plane -> { val p = f(o.p); Plane(p, unit(f(o.p + o.n) - p)) }
        is Sphere -> Sphere(f(o.center), o.r * abs(scale))
        is Circle -> { val c = f(o.center); Circle(c, unit(f(o.center + o.n) - c), o.r * abs(scale)) }
        is Polygon -> Polygon(o.points.map(f))
        is Solid -> Solid(o.faces.map { it.map(f) }, o.kind)
        is Angle -> o
        is Many -> Many(o.items.map { transform(it, scale, f) })
        is Number, is Bool -> throw GeometryError("A number can't be moved")
    }

    /** The point at [t] along a line, segment, ray (in steps of its two points' distance) or once round a circle. */
    fun pointAt(o: Obj, t: Double): Point = when (o) {
        is Segment -> o.a + (o.b - o.a).times(t.coerceIn(0.0, 1.0))
        is Vector -> o.a + (o.b - o.a).times(t.coerceIn(0.0, 1.0))
        is Ray -> o.a + (o.b - o.a).times(maxOf(t, 0.0))
        is Line -> o.a + (o.b - o.a).times(t)
        is Circle -> circleAt(o, 2 * PI * t)
        is Point -> o
        else -> throw GeometryError("A point can go on a line, segment or circle")
    }

    private fun basis(n: Point): kotlin.Pair<Point, Point> {
        val e1 = unit(n cross (if (abs(n.z) < 0.9) Point(0.0, 0.0, 1.0) else Point(1.0, 0.0, 0.0)))
        return e1 to (n cross e1)
    }

    private fun circleAt(c: Circle, t: Double): Point { val (e1, e2) = basis(c.n); return c.center + e1.times(c.r * cos(t)) + e2.times(c.r * sin(t)) }

    /** The point of [o] nearest [p] ([unbounded]: a segment or ray read as its whole line). */
    private fun closest(o: Obj, p: Point, unbounded: Boolean): Point = when (o) {
        is Point -> o
        is Line, is Segment, is Ray, is Vector -> {
            val (a, b) = ends(o); val d = b - a
            var t = ((p - a) dot d) / (d dot d)
            if (!unbounded) t = when (o) { is Segment, is Vector -> t.coerceIn(0.0, 1.0); is Ray -> maxOf(t, 0.0); else -> t }
            a + d.times(t)
        }
        is Plane -> p - o.n.times((p - o.p) dot o.n)
        is Sphere -> { val d = p - o.center; if (d.length < 1e-12) o.center + Point(0.0, 0.0, o.r) else o.center + unit(d).times(o.r) }
        is Circle -> {
            val inPlane = p - o.n.times((p - o.center) dot o.n) - o.center
            if (inPlane.length < 1e-12) circleAt(o, 0.0) else o.center + unit(inPlane).times(o.r)
        }
        else -> throw GeometryError("ClosestPoint on a line, plane, sphere or circle")
    }

    private fun distance(p: Point, o: Obj): Double = when (o) {
        is Point -> (o - p).length
        is Sphere -> abs((p - o.center).length - o.r)
        is Line, is Segment, is Ray, is Vector, is Plane, is Circle -> (closest(o, p, unbounded = false) - p).length
        else -> throw GeometryError("Distance to a point, line, plane, sphere or circle")
    }

    /** Between two lines: 0 if they meet, else the length of their common perpendicular. */
    private fun skew(x: Obj, y: Obj): Double {
        val (a, b) = ends(x); val (c, d) = ends(y)
        val n = (b - a) cross (d - c)
        return if (n.length < 1e-12) ((c - a) cross unit(b - a)).length else abs((c - a) dot unit(n))
    }

    private fun angleBetween(x: Obj, y: Obj): Double {
        fun dir(o: Obj) = when (o) { is Vector -> unit(o.b - o.a); is Point -> unit(o); else -> direction(o) }
        val lineish = { o: Obj -> o is Line || o is Segment || o is Ray }
        return when {
            x is Plane && y is Plane -> acos(abs(x.n dot y.n).coerceIn(0.0, 1.0))
            x is Plane || y is Plane -> { val n = if (x is Plane) x.n else (y as Plane).n; val d = dir(if (x is Plane) y else x); PI / 2 - acos(abs(n dot d).coerceIn(0.0, 1.0)) }
            // Two vectors: from one to the other (0 to π); lines: the smaller angle between them.
            else -> { val c = dir(x) dot dir(y); if (lineish(x) || lineish(y)) acos(abs(c).coerceIn(0.0, 1.0)) else acos(c.coerceIn(-1.0, 1.0)) }
        }
    }

    private fun onPiece(o: Obj, t: Double) = when (o) { is Segment, is Vector -> t in -1e-9..1 + 1e-9; is Ray -> t >= -1e-9; else -> true }

    private fun intersect(x: Obj, y: Obj): Obj {
        fun many(pts: List<Point>): Obj = when (pts.size) { 0 -> throw GeometryError("They don't meet"); 1 -> pts[0]; else -> Many(pts) }
        return when {
            isStraight(x) && isStraight(y) -> {
                val (a, b) = ends(x); val (c, d) = ends(y)
                val r = b - a; val s = d - c; val n = r cross s
                if (n.length < 1e-12 * r.length * s.length) throw GeometryError("They're parallel")
                // The nearest points of each line to the other; they meet if those are the same.
                val t = (((c - a) cross s) dot n) / (n dot n)
                val u = (((c - a) cross r) dot n) / (n dot n)
                val p = a + r.times(t); val q = c + s.times(u)
                if ((p - q).length > 1e-7 * maxOf(1.0, p.length)) throw GeometryError("They don't meet (they're skew)")
                if (!onPiece(x, t) || !onPiece(y, u)) throw GeometryError("They don't meet")
                p
            }
            isStraight(x) && y is Plane -> linePlane(x, y)
            x is Plane && isStraight(y) -> linePlane(y, x)
            x is Plane && y is Plane -> {
                val d = x.n cross y.n
                if (d.length < 1e-12) throw GeometryError("The planes are parallel")
                // A point on both: solve along the two normals.
                val h1 = x.n dot x.p; val h2 = y.n dot y.p
                val nn = x.n dot y.n
                val det = 1 - nn * nn
                val c1 = (h1 - h2 * nn) / det; val c2 = (h2 - h1 * nn) / det
                val p = x.n.times(c1) + y.n.times(c2)
                Line(p, p + unit(d))
            }
            isStraight(x) && y is Sphere -> many(lineSphere(x, y))
            x is Sphere && isStraight(y) -> many(lineSphere(y, x))
            x is Plane && y is Sphere -> planeSphere(x, y)
            x is Sphere && y is Plane -> planeSphere(y, x)
            x is Sphere && y is Sphere -> {
                val dv = y.center - x.center; val d = dv.length
                if (d < 1e-12 || d > x.r + y.r + 1e-9 || d < abs(x.r - y.r) - 1e-9) throw GeometryError("The spheres don't meet in a circle")
                val a = (x.r * x.r - y.r * y.r + d * d) / (2 * d)
                val n = unit(dv)
                Circle(x.center + n.times(a), n, sqrt(maxOf(0.0, x.r * x.r - a * a)))
            }
            isStraight(x) && y is Polygon -> linePolygon(x, y)
            x is Polygon && isStraight(y) -> linePolygon(y, x)
            else -> throw GeometryError("Intersect lines, planes, spheres or polygons")
        }
    }

    private fun linePlane(l: Obj, p: Plane): Point {
        val (a, b) = ends(l); val d = b - a
        val den = d dot p.n
        if (abs(den) < 1e-12 * d.length) throw GeometryError("The line is parallel to the plane")
        val t = ((p.p - a) dot p.n) / den
        if (!onPiece(l, t)) throw GeometryError("They don't meet")
        return a + d.times(t)
    }

    private fun lineSphere(l: Obj, s: Sphere): List<Point> {
        val (a, b) = ends(l); val d = b - a; val f = a - s.center
        val qa = d dot d; val qb = 2 * (f dot d); val qc = (f dot f) - s.r * s.r
        val disc = qb * qb - 4 * qa * qc
        val ts = when {
            disc < -1e-12 * qa * s.r * s.r -> emptyList()
            abs(disc) <= 1e-12 * qa * s.r * s.r -> listOf(-qb / (2 * qa))
            else -> listOf((-qb - sqrt(disc)) / (2 * qa), (-qb + sqrt(disc)) / (2 * qa))
        }
        return ts.filter { onPiece(l, it) }.map { a + d.times(it) }
    }

    private fun planeSphere(p: Plane, s: Sphere): Obj {
        val h = (s.center - p.p) dot p.n
        if (abs(h) > s.r + 1e-12) throw GeometryError("The plane misses the sphere")
        val c = s.center - p.n.times(h)
        val r = sqrt(maxOf(0.0, s.r * s.r - h * h))
        return if (r < 1e-9) c else Circle(c, p.n, r)
    }

    private fun linePolygon(l: Obj, poly: Polygon): Point {
        val pl = planeThrough(poly.points[0], poly.points[1], poly.points[2])
        val p = linePlane(l, pl)
        // Inside if it's on the same side of every edge.
        val n = pl.n
        val signs = poly.points.indices.map { k -> val a = poly.points[k]; val b = poly.points[(k + 1) % poly.points.size]; ((b - a) cross (p - a)) dot n }
        if (!(signs.all { it >= -1e-9 } || signs.all { it <= 1e-9 })) throw GeometryError("The line misses the polygon")
        return p
    }

    // ---- Drawing --------------------------------------------------------------------------

    /**
     * How an object is drawn in the box [b]: faces (planes cut to the box, spheres, polygons and
     * solids, drawn see-through), lines (cut to the box), points, an arrowhead, and labels.
     */
    class Drawing(
        val faces: List<List<DoubleArray>> = emptyList(),
        val lines: List<List<DoubleArray>> = emptyList(),
        val points: List<DoubleArray> = emptyList(),
        val arrow: Boolean = false,
        val labels: List<kotlin.Pair<DoubleArray, String>> = emptyList(),
    )

    fun draw(o: Obj, b: Bounds, angleText: (Double) -> String = { "%.1f°".format(Math.toDegrees(it)) }): Drawing = when (o) {
        is Point -> Drawing(points = listOf(o.array()))
        is Segment -> Drawing(lines = listOf(listOf(o.a.array(), o.b.array())))
        is Vector -> Drawing(lines = listOf(listOf(o.a.array(), o.b.array())), arrow = true)
        is Line -> clip(o.a, o.b - o.a, Double.NEGATIVE_INFINITY, b)
        is Ray -> clip(o.a, o.b - o.a, 0.0, b)
        is Plane -> {
            val face = planeInBox(o, b)
            if (face.size < 3) Drawing() else Drawing(faces = listOf(face), lines = listOf(face + listOf(face[0])))
        }
        is Sphere -> {
            val rows = 14; val cols = 24
            fun at(i: Int, j: Int): DoubleArray {
                val th = PI * i / rows; val ph = 2 * PI * j / cols
                return doubleArrayOf(o.center.x + o.r * sin(th) * cos(ph), o.center.y + o.r * sin(th) * sin(ph), o.center.z + o.r * cos(th))
            }
            Drawing(faces = (0 until rows).flatMap { i -> (0 until cols).map { j -> listOf(at(i, j), at(i + 1, j), at(i + 1, j + 1), at(i, j + 1)) } })
        }
        is Circle -> Drawing(lines = listOf((0..96).map { k -> circleAt(o, 2 * PI * k / 96).array() }))
        is Polygon -> { val pts = o.points.map { it.array() }; Drawing(faces = listOf(pts), lines = listOf(pts + listOf(pts[0]))) }
        is Solid -> Drawing(faces = o.faces.map { f -> f.map { it.array() } }, lines = o.faces.map { f -> (f + f[0]).map { it.array() } })
        is Angle -> {
            val r = 0.08 * maxOf(b.x1 - b.x0, b.y1 - b.y0, b.z1 - b.z0)
            // The arc from u towards v, in their plane.
            val w = (o.v - o.u.times(o.u dot o.v)).let { if (it.length < 1e-12) basis(o.u).first else unit(it) }
            val arc = (0..32).map { k -> val t = o.size * k / 32; (o.vertex + o.u.times(r * cos(t)) + w.times(r * sin(t))).array() }
            val m = o.size / 2
            Drawing(lines = listOf(arc), faces = listOf(listOf(o.vertex.array()) + arc), labels = listOf((o.vertex + o.u.times(2 * r * cos(m)) + w.times(2 * r * sin(m))).array() to angleText(o.size)))
        }
        is Many -> o.items.map { draw(it, b, angleText) }.let { ds -> Drawing(ds.flatMap { it.faces }, ds.flatMap { it.lines }, ds.flatMap { it.points }, labels = ds.flatMap { it.labels }) }
        is Number, is Bool -> Drawing()
    }

    /** A line or ray from [a] along [d], cut to where it's inside the box (from [from] along it). */
    private fun clip(a: Point, d: Point, from: Double, b: Bounds): Drawing {
        var lo = from; var hi = Double.POSITIVE_INFINITY
        val p = doubleArrayOf(a.x, a.y, a.z); val v = doubleArrayOf(d.x, d.y, d.z)
        val mins = doubleArrayOf(b.x0, b.y0, b.z0); val maxs = doubleArrayOf(b.x1, b.y1, b.z1)
        for (k in 0..2) {
            if (abs(v[k]) < 1e-15) { if (p[k] < mins[k] || p[k] > maxs[k]) return Drawing(); continue }
            val t1 = (mins[k] - p[k]) / v[k]; val t2 = (maxs[k] - p[k]) / v[k]
            lo = maxOf(lo, minOf(t1, t2)); hi = minOf(hi, maxOf(t1, t2))
        }
        if (!(lo < hi) || !lo.isFinite() && lo > 0) return Drawing()
        val s = maxOf(lo, if (from.isFinite()) from else lo)
        return Drawing(lines = listOf(listOf((a + d.times(s)).array(), (a + d.times(hi)).array())))
    }

    /** Where a plane cuts the box: the corners of that polygon, in order round it. */
    fun planeInBox(p: Plane, b: Bounds): List<DoubleArray> {
        val xs = doubleArrayOf(b.x0, b.x1); val ys = doubleArrayOf(b.y0, b.y1); val zs = doubleArrayOf(b.z0, b.z1)
        val corners = (0..7).map { k -> Point(xs[k and 1], ys[(k shr 1) and 1], zs[(k shr 2) and 1]) }
        val edges = listOf(0 to 1, 2 to 3, 4 to 5, 6 to 7, 0 to 2, 1 to 3, 4 to 6, 5 to 7, 0 to 4, 1 to 5, 2 to 6, 3 to 7)
        val pts = ArrayList<Point>()
        for ((i, j) in edges) {
            val a = corners[i]; val c = corners[j]
            val fa = (a - p.p) dot p.n; val fc = (c - p.p) dot p.n
            if (fa == 0.0) pts += a
            if (fa * fc < 0) pts += a + (c - a).times(fa / (fa - fc))
        }
        val unique = ArrayList<Point>()
        for (q in pts) if (unique.none { (it - q).length < 1e-9 }) unique += q
        if (unique.size < 3) return emptyList()
        val c = centroid(unique)
        val (e1, e2) = basis(p.n)
        return unique.sortedBy { q -> kotlin.math.atan2((q - c) dot e2, (q - c) dot e1) }.map { it.array() }
    }

    // ---- Names ----------------------------------------------------------------------------

    /** A point in space as text: (1, 2, 3). */
    fun text(p: Point, number: (Double) -> String) = "(" + number(p.x) + ", " + number(p.y) + ", " + number(p.z) + ")"
}
