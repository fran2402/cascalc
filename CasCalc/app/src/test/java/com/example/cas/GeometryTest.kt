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
}
