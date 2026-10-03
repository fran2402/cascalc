package com.example.cas

import com.example.cas.editor.MathRow
import com.example.cas.editor.Node
import com.example.cas.editor.Sym
import com.example.cas.graph.Geometry
import com.example.cas.graph.Geometry.Point
import com.example.cas.graph.Viewport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sqrt

class GeometryTest {
    /** A row typed key by key: one node per character, except words in braces ({Segment}). */
    private fun row(text: String): List<Node> {
        val out = ArrayList<Node>()
        var k = 0
        while (k < text.length) {
            if (text[k] == '{') { val e = text.indexOf('}', k); out += Sym(text.substring(k + 1, e)); k = e + 1; continue }
            if (text[k] != ' ') out += Sym(text[k].toString())
            k++
        }
        return out
    }

    /** Numbers: plain ones, names of numbers already made, and a slider a = 2. */
    private val number = { nodes: List<Node>, known: Map<String, Double> ->
        Geometry.plainNumber(nodes) ?: (nodes.singleOrNull() as? Sym)?.text?.let { known[it] ?: if (it == "a") 2.0 else null }
            ?: throw Geometry.GeometryError("Can't read that number")
    }

    private fun build(vararg lines: String): List<Geometry.Outcome?> = Geometry.build(lines.map { Geometry.parse(row(it)) }, number, degrees = true)

    private fun obj(vararg lines: String): Geometry.Obj = build(*lines).last()!!.let { it.obj ?: error(it.error ?: "nothing") }

    private fun assertPoint(x: Double, y: Double, o: Any?) {
        val p = o as Point
        assertEquals(x, p.x, 1e-9); assertEquals(y, p.y, 1e-9)
    }

    @Test fun readsStatements() {
        assertNotNull(Geometry.parse(row("A=(1,2)")))
        assertTrue(Geometry.parse(row("A=(1,2)"))!!.isFree)
        // Commands typed letter by letter, in any case, or as one word.
        assertNotNull(Geometry.parse(row("s=segment(A,B)")))
        assertNotNull(Geometry.parse(row("{Segment}(A,B)")))
        // The graph's own lines stay its own: y = x², a plain point, a slider, a lowercase pair.
        assertNull(Geometry.parse(row("y=x^2")))
        assertNull(Geometry.parse(row("(2,3)")))
        assertNull(Geometry.parse(row("a=3")))
        assertNull(Geometry.parse(row("f=(1,2)")))
        // A = (a, 2) uses a slider, so it isn't dragged as a free point.
        assertTrue(!Geometry.parse(row("A=(a,2)"))!!.isFree)
    }

    @Test fun pointsMidpointAndOrderFree() {
        // M uses A and B from lines below it.
        val out = build("M=Midpoint(A,B)", "A=(0,0)", "B=(4,2)")
        assertPoint(2.0, 1.0, out[0]!!.obj)
        assertPoint(2.0, 2.0, obj("A=(a,2)"))
    }

    @Test fun circlesAndIntersections() {
        val lines = arrayOf("A=(0,0)", "B=(2,0)", "c=Circle(A,B)", "d=Circle(B,A)")
        val both = obj(*lines, "Intersect(c,d)") as Geometry.Many
        assertEquals(2, both.items.size)
        assertPoint(1.0, -sqrt(3.0), both.items[0])
        assertPoint(1.0, sqrt(3.0), obj(*lines, "P=Intersect(c,d,2)"))
        // A line through a circle, and a segment that stops short of it.
        assertEquals(2, (obj("A=(0,0)", "c=Circle(A,1)", "l=Line((−2,0),(2,0))", "Intersect(l,c)") as Geometry.Many).items.size)
        assertPoint(1.0, 0.0, obj("A=(0,0)", "c=Circle(A,1)", "s=Segment((0,0),(3,0))", "Intersect(s,c)"))
        assertEquals("They don't cross", build("A=(0,0)", "c=Circle(A,1)", "s=Segment((2,0),(3,0))", "P=Intersect(s,c,1)").last()!!.error)
    }

    @Test fun circleThroughThreePointsAndIncircle() {
        val c = obj("Circle((0,0),(2,0),(0,2))") as Geometry.Circle
        assertPoint(1.0, 1.0, c.center); assertEquals(sqrt(2.0), c.r, 1e-12)
        val i = obj("Incircle((0,0),(3,0),(0,4))") as Geometry.Circle
        assertEquals(1.0, i.r, 1e-12); assertPoint(1.0, 1.0, i.center)
    }

    @Test fun measurements() {
        val tri = arrayOf("A=(0,0)", "B=(3,0)", "C=(0,4)", "t=Polygon(A,B,C)")
        assertEquals(5.0, Geometry.value(obj("A=(0,0)", "B=(3,4)", "Distance(A,B)"))!!, 1e-12)
        assertEquals(6.0, Geometry.value(obj(*tri, "Area(t)"))!!, 1e-12)
        assertEquals(12.0, Geometry.value(obj(*tri, "Perimeter(t)"))!!, 1e-12)
        assertEquals(PI / 2, Geometry.value(obj(*tri, "Angle(B,A,C)"))!!, 1e-12)
        // Counterclockwise from the first arm: the other way round is the reflex angle.
        assertEquals(3 * PI / 2, Geometry.value(obj(*tri, "Angle(C,A,B)"))!!, 1e-12)
        assertEquals(2.4, Geometry.value(obj(*tri, "l=Line(B,C)", "Distance(A,l)"))!!, 1e-12)
        assertEquals(-4.0 / 3, Geometry.value(obj(*tri, "Slope(Segment(B,C))"))!!, 1e-12)
        // A number made on one line is used in another.
        assertEquals(5.0, (obj("A=(0,0)", "B=(3,4)", "d=Distance(A,B)", "c=Circle(A,d)") as Geometry.Circle).r, 1e-12)
    }

    @Test fun constructionsAndTransforms() {
        val p = obj("A=(0,0)", "B=(2,2)", "PerpendicularBisector(A,B)") as Geometry.Line
        // Through the midpoint, at right angles.
        val d = p.b - p.a
        assertEquals(0.0, d.x * 2 + d.y * 2, 1e-12)
        assertPoint(1.0, 1.0, p.a)
        assertPoint(-2.0, 1.0, obj("A=(2,1)", "Reflect(A,Line((0,0),(0,1)))"))
        assertPoint(0.0, 2.0, obj("A=(2,0)", "Rotate(A,90)"))
        assertPoint(3.0, 3.0, obj("A=(1,1)", "Dilate(A,3)"))
        assertPoint(4.0, 6.0, obj("A=(1,1)", "Translate(A,(3,5))"))
        val tangents = obj("c=Circle((0,0),1)", "Tangent((2,0),c)") as Geometry.Many
        assertEquals(2, tangents.items.size)
        assertPoint(1.0, 1.0, obj("Centroid(Polygon((0,0),(2,0),(2,2),(0,2)))"))
    }

    @Test fun errorsSayWhy() {
        assertEquals("Q isn't defined", build("A=(0,0)", "Segment(A,Q)").last()!!.error)
        assertEquals("This depends on itself", build("A=Midpoint(A,B)", "B=(1,1)")[0]!!.error)
        assertEquals("A polygon needs three corners or more", build("Polygon((0,0),(1,1))").last()!!.error)
        assertTrue(build("Circle((0,0),(1,1),(2,2))").last()!!.error!!.contains("one line"))
    }

    @Test fun drawing() {
        val view = Viewport(-5.0, 5.0, -5.0, 5.0)
        // A line reaches past both edges of the view.
        val line = Geometry.draw(obj("Line((0,0),(1,1))"), view).lines.single()
        assertTrue(line.first().first < -5 && line.last().first > 5)
        val tri = Geometry.draw(obj("Polygon((0,0),(1,0),(0,1))"), view)
        assertEquals(3, tri.fill!!.size)
        val angle = Geometry.draw(obj("A=(0,0)", "Angle((1,0),A,(0,1))"), view)
        assertEquals("90.0°", angle.labels.single().second)
    }

    @Test fun pointNames() {
        assertEquals("A", Geometry.nextPointName(emptySet()))
        assertEquals("C", Geometry.nextPointName(setOf("A", "B")))
        val all = ('A'..'Z').map { it.toString() }.toSet()
        assertEquals("A", com.example.cas.cas.CustomSymbol.decode(Geometry.nextPointName(all))!!.base)
    }

    // ---- Round 2 ----

    @Test fun arithmeticWithCommands() {
        assertEquals(2.5, Geometry.value(obj("A=(0,0)", "B=(3,4)", "Distance(A,B)/2"))!!, 1e-12)
        assertPoint(2.0, 1.0, obj("A=(0,0)", "B=(4,2)", "M=(A+B)/2"))
        assertPoint(4.0, 2.0, obj("A=(1,1)", "v=Vector((0,0),(3,1))", "B=A+v"))
        assertPoint(3.0, 2.0, obj("A=(1,2)", "P=A+(2,0)"))
        assertEquals(5.0, Geometry.value(obj("A=(3,4)", "d=x(A)+y(A)-2"))!!, 1e-12)
        assertPoint(6.0, 3.0, obj("A=(2,1)", "B=3A"))
        // A typed product of letters isn't taken for a command: R·a·y without a bracket.
        assertNull(Geometry.parse(row("y=Ray")))
        // Arithmetic on points needs a name: y = A x keeps A as the graph's slider.
        assertNull(Geometry.parse(row("y=Ax")))
    }

    @Test fun pointsOnPaths() {
        val c = arrayOf("A=(0,0)", "c=Circle(A,2)")
        assertPoint(0.0, 2.0, obj(*c, "P=Point(c,0.25)"))
        assertTrue(Geometry.parse(row("P=Point(c,0.25)"))!!.onPath)
        assertTrue(Geometry.parse(row("P=Point(c)"))!!.onPath)
        assertPoint(1.0, 0.5, obj("s=Segment((0,0),(2,1))", "P=Point(s,0.5)"))
        // Back again: the parameter of the nearest point.
        val circle = obj(*c) as Geometry.Circle
        assertEquals(0.125, Geometry.parameterOf(circle, Point(5.0, 5.0)), 1e-12)
        val square = obj("Polygon((0,0),(1,0),(1,1),(0,1))")
        assertPoint(1.0, 0.5, Geometry.pointAt(square, 0.375))
        assertEquals(0.375, Geometry.parameterOf(square, Point(1.2, 0.5)), 1e-6)
    }

    @Test fun arcsAndSectors() {
        val semi = obj("Semicircle((−1,0),(1,0))") as Geometry.Arc
        assertEquals(PI, semi.start, 1e-12); assertEquals(PI, semi.sweep, 1e-12)
        val sector = obj("O=(0,0)", "CircularSector(O,(1,0),(0,3))")
        assertEquals(PI / 4, Geometry.value(obj("O=(0,0)", "s=CircularSector(O,(1,0),(0,3))", "Area(s)"))!!, 1e-12)
        assertEquals(PI / 2, Geometry.value(obj("O=(0,0)", "s=CircularArc(O,(1,0),(0,3))", "Length(s)"))!!, 1e-12)
        assertTrue(Geometry.draw(sector, Viewport(-2.0, 2.0, -2.0, 2.0)).fill != null)
        // An arc only meets a line where the arc is: the upper half of the circle.
        assertPoint(0.0, 1.0, obj("a=Semicircle((1,0),(−1,0))", "Intersect(a,Line((0,−5),(0,5)))"))
        // Through B: (0, 1) is on the way from (1, 0) to (−1, 0) counterclockwise.
        val arc = obj("CircumcircularArc((1,0),(0,1),(−1,0))") as Geometry.Arc
        assertEquals(PI, arc.sweep, 1e-9)
        val back = obj("CircumcircularArc((1,0),(0,−1),(−1,0))") as Geometry.Arc
        assertEquals(-PI, back.sweep, 1e-9)
    }

    @Test fun regularPolygon() {
        val sq = obj("RegularPolygon((0,0),(1,0),4)") as Geometry.Polygon
        assertEquals(4, sq.points.size)
        assertPoint(1.0, 1.0, sq.points[2]); assertPoint(0.0, 1.0, sq.points[3])
        assertEquals(sqrt(3.0) / 4 * 4, Geometry.value(obj("t=RegularPolygon((0,0),(2,0),3)", "Area(t)"))!!, 1e-9)
    }

    @Test fun conics() {
        // Foci (±3, 0), a = 5: x²/25 + y²/16 = 1.
        val e = obj("Ellipse((−3,0),(3,0),5)") as Geometry.Conic
        val shape = e.shape as Geometry.ConicShape.Ellipse
        assertEquals(5.0, shape.a, 1e-9); assertEquals(4.0, shape.b, 1e-9)
        assertEquals(0.0, Geometry.conicValue(e.c, Point(0.0, 4.0)), 1e-9)
        assertEquals(20 * PI, Geometry.value(obj("k=Ellipse((−3,0),(3,0),5)", "Area(k)"))!!, 1e-9)
        // Through a point instead of a: the same ellipse.
        val e2 = obj("Ellipse((−3,0),(3,0),(5,0))") as Geometry.Conic
        assertEquals(4.0, (e2.shape as Geometry.ConicShape.Ellipse).b, 1e-9)
        // It meets the y axis at (0, ±4), and a circle of radius 4.5 in four points.
        val axis = obj("k=Ellipse((−3,0),(3,0),5)", "Intersect(k,Line((0,0),(0,1)))") as Geometry.Many
        assertPoint(0.0, -4.0, axis.items[0]); assertPoint(0.0, 4.0, axis.items[1])
        assertEquals(4, (obj("k=Ellipse((−3,0),(3,0),5)", "c=Circle((0,0),4.5)", "Intersect(k,c)") as Geometry.Many).items.size)
        // Hyperbola with foci (±5, 0), a = 3: x²/9 − y²/16 = 1.
        val h = obj("Hyperbola((−5,0),(5,0),3)") as Geometry.Conic
        assertEquals(0.0, Geometry.conicValue(h.c, Point(3.0, 0.0)), 1e-9)
        assertEquals(0.0, Geometry.conicValue(h.c, Point(5.0, 16.0 / 3)), 1e-9)
        assertEquals(2, Geometry.draw(h, Viewport(-10.0, 10.0, -10.0, 10.0)).lines.size)
        // Parabola, focus (0, 1), directrix y = −1: y = x²/4.
        val p = obj("Parabola((0,1),Line((0,−1),(1,−1)))") as Geometry.Conic
        assertEquals(0.0, Geometry.conicValue(p.c, Point(2.0, 1.0)), 1e-9)
        assertEquals(0.0, Geometry.conicValue(p.c, Point(-4.0, 4.0)), 1e-9)
        val pts = (0..10).map { Geometry.conicAt(p.shape, -0.9 + 0.18 * it)!! }
        pts.forEach { assertEquals(it.x * it.x / 4, it.y, 1e-6) }
        // Through five points of the unit circle: the unit circle.
        val c5 = obj("Conic((1,0),(0,1),(−1,0),(0,−1),(0.6,0.8))") as Geometry.Conic
        assertEquals(0.0, Geometry.conicValue(c5.c, Point(-0.8, -0.6)), 1e-9)
        // Tangents from (0, 5) to x²/25 + y²/16 = 1 touch it where y = 16/5.
        val tangents = obj("k=Ellipse((−3,0),(3,0),5)", "Tangent((0,5),k)") as Geometry.Many
        assertEquals(2, tangents.items.size)
        tangents.items.forEach { assertEquals(16.0 / 5, (it as Geometry.Line).b.y, 1e-6) }
        // Moved: the ellipse slides, its equation follows.
        val moved = obj("k=Ellipse((−3,0),(3,0),5)", "Translate(k,(1,2))") as Geometry.Conic
        assertEquals(0.0, Geometry.conicValue(moved.c, Point(1.0, 6.0)), 1e-6)
        // A point on it, dragged round.
        assertPoint(0.0, 4.0, obj("k=Ellipse((−3,0),(3,0),5)", "P=Point(k,0.25)"))
    }

    @Test fun functionGraphs() {
        val f = { x: Double -> x * x }
        fun build2(vararg lines: String) = Geometry.build(lines.map { Geometry.parse(row(it)) }, number, functionOf = { if (it == "f") f else null }, xRange = -10.0..10.0)
        val out = build2("l=Line((0,4),(1,4))", "Intersect(f,l)").last()!!
        val both = out.obj as? Geometry.Many ?: error(out.error ?: "no object")
        assertPoint(-2.0, 4.0, both.items[0]); assertPoint(2.0, 4.0, both.items[1])
        // The tangent at x = 1 has slope 2.
        val t = build2("A=(1,0)", "Tangent(A,f)").last()!!.obj as Geometry.Line
        assertEquals(2.0, (t.b.y - t.a.y) / (t.b.x - t.a.x), 1e-6)
        assertPoint(3.0, 9.0, build2("P=Point(f,3)").last()!!.obj)
        val circle = build2("c=Circle((0,0),2)", "Intersect(f,c)")
        assertEquals(2, (circle.last()!!.obj as Geometry.Many).items.size)
    }

    @Test fun names() {
        assertEquals("f", Geometry.nextObjectName(emptySet()))
        assertEquals("h", Geometry.nextObjectName(setOf("f", "g")))
        assertEquals("α", Geometry.nextAngleName(emptySet()))
    }

    // ---- Round 3 ----

    @Test fun triangleCenters() {
        val t = arrayOf("A=(0,0)", "B=(4,0)", "C=(0,3)")
        assertPoint(2.0, 1.5, obj(*t, "Circumcenter(A,B,C)"))
        // A right angle at A: the altitudes meet there.
        assertPoint(0.0, 0.0, obj(*t, "Orthocenter(A,B,C)"))
        assertPoint(1.0, 1.0, obj(*t, "Incenter(A,B,C)"))
        assertPoint(4.0 / 3, 1.0, obj(*t, "Centroid(A,B,C)"))
        // Euler's line: H = 3G − 2O, for a scalene triangle too.
        val h = obj("Orthocenter((0,0),(5,0),(1,3))") as Point
        // H is where the altitude from (1, 3) (x = 1) meets the one from (0, 0).
        assertEquals(1.0, h.x, 1e-9); assertEquals(4.0 / 3, h.y, 1e-9)
    }

    @Test fun conicParts() {
        val e = arrayOf("k=Ellipse((−3,0),(3,0),5)")
        val f = obj(*e, "Foci(k)") as Geometry.Many
        assertEquals(setOf(-3.0, 3.0), f.items.map { Math.round((it as Point).x).toDouble() }.toSet())
        assertEquals(4, (obj(*e, "Vertex(k)") as Geometry.Many).items.size)
        val asym = obj("h=Hyperbola((−5,0),(5,0),3)", "Asymptote(h)") as Geometry.Many
        // y = ±4x/3.
        asym.items.forEach { l -> l as Geometry.Line; assertEquals(4.0 / 3, kotlin.math.abs((l.b.y - l.a.y) / (l.b.x - l.a.x)), 1e-9) }
        // y = x²/4: focus (0, 1), directrix y = −1, vertex (0, 0).
        val par = arrayOf("p=Parabola((0,1),Line((0,−1),(1,−1)))")
        assertPoint(0.0, 1.0, obj(*par, "Foci(p)"))
        assertPoint(0.0, 0.0, obj(*par, "Vertex(p)"))
        val d = obj(*par, "Directrix(p)") as Geometry.Line
        assertEquals(-1.0, d.a.y, 1e-9); assertEquals(-1.0, d.b.y, 1e-9)
        // The polar of (2, 0) for the unit circle is x = 1/2.
        val polar = obj("c=Circle((0,0),1)", "Polar((2,0),c)") as Geometry.Line
        assertEquals(0.5, polar.a.x, 1e-12); assertEquals(0.5, polar.b.x, 1e-12)
    }

    @Test fun areChecks() {
        assertEquals(Geometry.Bool(true), obj("AreCollinear((0,0),(1,1),(3,3))"))
        assertEquals(Geometry.Bool(false), obj("AreCollinear((0,0),(1,1),(3,4))"))
        assertEquals(Geometry.Bool(true), obj("AreConcyclic((1,0),(0,1),(−1,0),(0,−1))"))
        assertEquals(Geometry.Bool(true), obj("AreParallel(Line((0,0),(1,2)),Line((5,5),(6,7)))"))
        assertEquals(Geometry.Bool(true), obj("ArePerpendicular(Line((0,0),(1,2)),Line((0,0),(−2,1)))"))
        assertEquals(Geometry.Bool(true), obj("A=(0,0)", "B=(3,4)", "AreEqual(Distance(A,B),5)"))
    }

    @Test fun locus() {
        // The midpoint of a fixed point and a point running round a circle traces half the circle, centered between.
        val lines = arrayOf("c=Circle((0,0),2)", "Q=Point(c,0)", "A=(4,0)", "M=Midpoint(A,Q)", "L=Locus(M,Q)")
        val out = build(*lines)
        val locus = out.last()!!.obj as? Geometry.Polyline ?: error(out.last()!!.error ?: "no locus")
        locus.pieces.flatten().forEach { p -> assertEquals(1.0, kotlin.math.hypot(p.x - 2, p.y), 1e-9) }
        // The locus is listed after what it uses, but works out wherever it's written.
        val first = build("L=Locus(M,Q)", *lines.dropLast(1).toTypedArray())
        assertTrue(first[0]!!.obj is Geometry.Polyline)
        assertEquals("A must be a point on an object, like A = Point(c, 0.5)", build(*lines.dropLast(1).toTypedArray(), "Locus(M,A)").last()!!.error)
        // A point can go on a locus too.
        assertNotNull(build(*lines, "P=Point(L,0.5)").last()!!.obj as? Point)
    }

    // ---- Round 4 ----

    @Test fun moreCommands() {
        val fit = obj("FitLine((0,1),(1,3),(2,5))") as Geometry.Line
        assertEquals(2.0, (fit.b.y - fit.a.y) / (fit.b.x - fit.a.x), 1e-12)
        assertTrue(obj("Polyline((0,0),(1,1),(2,0))") is Geometry.Polyline)
        val sector = obj("CircumcircularSector((1,0),(0,1),(−1,0))") as Geometry.Arc
        assertTrue(sector.sector); assertEquals(PI, sector.sweep, 1e-9)
        // The compass: a segment's length as the radius.
        assertEquals(5.0, (obj("s=Segment((0,0),(3,4))", "Circle((1,1),s)") as Geometry.Circle).r, 1e-12)
        // Inversion in the unit circle: (2, 0) goes to (1/2, 0).
        assertPoint(0.5, 0.0, obj("c=Circle((0,0),1)", "Reflect((2,0),c)"))
    }

    @Test fun rootsAndExtrema() {
        val f = { x: Double -> x * x - 4 }
        fun b(vararg lines: String) = Geometry.build(lines.map { Geometry.parse(row(it)) }, number, functionOf = { if (it == "f") f else null }, xRange = -10.0..10.0)
        val roots = b("Root(f)").last()!!.obj as Geometry.Many
        assertPoint(-2.0, 0.0, roots.items[0]); assertPoint(2.0, 0.0, roots.items[1])
        assertPoint(0.0, -4.0, b("Extremum(f)").last()!!.obj)
    }

    @Test fun relations() {
        assertEquals("They're parallel", (obj("Relation(Line((0,0),(1,1)),Line((0,1),(1,2)))") as Geometry.Text).value)
        assertEquals("They're perpendicular", (obj("Relation(Line((0,0),(1,1)),Line((0,0),(1,−1)))") as Geometry.Text).value)
        assertEquals("The point is on it", (obj("Relation((0,1),Circle((0,0),1))") as Geometry.Text).value)
        assertEquals("They're equal", (obj("A=(0,0)", "B=(3,4)", "Relation(Distance(A,B),5)") as Geometry.Text).value)
        assertEquals("They're parallel, and are the same length", (obj("Relation(Segment((0,0),(1,0)),Segment((0,1),(1,1)))") as Geometry.Text).value)
    }
}
