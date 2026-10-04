package com.example.cas

import com.example.cas.editor.MathRow
import com.example.cas.editor.Node
import com.example.cas.editor.Sym
import com.example.cas.graph.Bounds
import com.example.cas.graph.Geometry
import com.example.cas.graph.Geometry3D
import com.example.cas.graph.Geometry3D.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sqrt

/** Geometry in space (3D geometry mode) and on the complex plane. */
class Geometry3DTest {
    private fun row(text: String): List<Node> {
        val out = ArrayList<Node>()
        var k = 0
        while (k < text.length) {
            if (text[k] == '{') { val end = text.indexOf('}', k); out += Sym(text.substring(k + 1, end)); k = end + 1 }
            else { out += Sym(text[k].toString()); k++ }
        }
        return out
    }
    private val number = { nodes: List<Node>, known: Map<String, Double> ->
        val t = nodes.joinToString("") { (it as Sym).text }
        known[t] ?: when (t) { "π" -> PI; else -> t.replace("−", "-").toDouble() }
    }
    private fun build(vararg lines: String) = Geometry3D.build(lines.map { Geometry3D.parse(row(it)) }, number, degrees = true)
    private fun obj(vararg lines: String): Geometry3D.Obj = build(*lines).last()!!.let { it.obj ?: error(it.error ?: "nothing") }
    private fun num(vararg lines: String) = Geometry3D.value(obj(*lines))!!
    private fun assertPoint(x: Double, y: Double, z: Double, o: Any?) {
        val p = o as Point
        assertEquals(x, p.x, 1e-9); assertEquals(y, p.y, 1e-9); assertEquals(z, p.z, 1e-9)
    }
    private val tri = arrayOf("A=(0,0,0)", "B=(2,0,0)", "C=(0,2,0)", "D=(0,0,2)")

    @Test fun readsPointsAndLeavesSurfacesAlone() {
        assertTrue(Geometry3D.isFree(Geometry3D.parse(row("A=(1,2,3)"))!!))
        assertNull(Geometry3D.parse(row("z=x+y")))
        // An unnamed point stays the 3D graph's own.
        assertNull(Geometry3D.parse(row("(1,2,3)")))
        assertPoint(1.0, 1.0, 0.0, obj(*tri, "Midpoint(B,C)"))
    }

    @Test fun planesAndIntersections() {
        // The plane through B, C and D is x + y + z = 2; the line from the origin along (1,1,1) meets it at (2/3, 2/3, 2/3).
        val third = 2.0 / 3
        assertPoint(third, third, third, obj(*tri, "p={Plane}(B,C,D)", "E=(1,1,1)", "{Intersect}({Line}(A,E),p)"))
        assertEquals(2 / sqrt(3.0), num(*tri, "p={Plane}(B,C,D)", "{Distance}(A,p)"), 1e-12)
        // Two planes meet in a line; z = 0 and x = 0 meet along the y-axis.
        val l = obj(*tri, "{Intersect}({Plane}(A,B,C),{Plane}(A,C,D))") as Geometry3D.Line
        assertEquals(0.0, l.a.x, 1e-9); assertEquals(0.0, l.a.z, 1e-9); assertEquals(0.0, l.b.x, 1e-9)
        // A plane through a sphere's center cuts a great circle.
        val c = obj(*tri, "s={Sphere}(A,3)", "{Intersect}({Plane}(A,B,C),s)") as Geometry3D.Circle
        assertEquals(3.0, c.r, 1e-12)
    }

    @Test fun measuresAndSolids() {
        assertEquals(4.0 / 3, num(*tri, "{Volume}({Pyramid}(A,B,C,D))"), 1e-12)
        assertEquals(8.0, num(*tri, "{Volume}({Cube}(A,B))"), 1e-12)
        assertEquals(24.0, num(*tri, "{Area}({Cube}(A,B))"), 1e-12)
        assertEquals(4.0 / 3 * PI * 27, num(*tri, "{Volume}({Sphere}(A,3))"), 1e-9)
        assertEquals(PI / 2, num(*tri, "{Angle}(B,A,C)"), 1e-12)
        // A line along (1,1,1) makes asin(1/√3) with the floor.
        assertEquals(kotlin.math.asin(1 / sqrt(3.0)), num(*tri, "E=(1,1,1)", "{Angle}({Line}(A,E),{Plane}(A,B,C))"), 1e-12)
        assertEquals(sqrt(2.0), num(*tri, "{Distance}({Line}(B,C),{Line}(A,D))"), 1e-12)
        assertEquals(2.0, num(*tri, "{Area}({Polygon}(A,B,C))"), 1e-12)
    }

    @Test fun transforms() {
        assertPoint(0.0, 0.0, -2.0, obj(*tri, "{Reflect}(D,{Plane}(A,B,C))"))
        // A quarter turn about the z-axis takes B to (0, 2, 0).
        assertPoint(0.0, 2.0, 0.0, obj(*tri, "{Rotate}(B,90°)"))
        assertPoint(0.0, 2.0, 0.0, obj(*tri, "{Rotate}(B,90°,{Line}(A,D))"))
        assertPoint(2.0, 0.0, 2.0, obj(*tri, "v={Vector}(A,D)", "{Translate}(B,v)"))
    }

    @Test fun drawingCutsToTheBox() {
        val b = Bounds(-1.0, 1.0, -1.0, 1.0, -1.0, 1.0)
        // The plane z = 0 crosses the box in a square.
        assertEquals(4, Geometry3D.planeInBox(Geometry3D.Plane(Point(0.0, 0.0, 0.0), Point(0.0, 0.0, 1.0)), b).size)
        val line = Geometry3D.draw(Geometry3D.Line(Point(0.0, 0.0, 0.0), Point(1.0, 0.0, 0.0)), b).lines.single()
        assertEquals(-1.0, line[0][0], 1e-12); assertEquals(1.0, line[1][0], 1e-12)
    }

    // ---- The complex plane ----

    private fun cbuild(vararg lines: String) = Geometry.build(lines.map { Geometry.parse(row(it), complex = true) }, number, degrees = true, complex = true,
        complexFunctionOf = { if (it == "f") { p -> Geometry.Point(p.x * p.x - p.y * p.y, 2 * p.x * p.y) } else null })
    private fun cobj(vararg lines: String) = cbuild(*lines).last()!!.let { it.obj ?: error(it.error ?: "nothing") }

    @Test fun complexArithmetic() {
        val p = cobj("A=1+2i", "B=3−i", "P=A·B") as Geometry.Point
        assertEquals(5.0, p.x, 1e-12); assertEquals(5.0, p.y, 1e-12)
        val q = cobj("A=1+2i", "B=3−i", "Q=A/B") as Geometry.Point
        assertEquals(0.1, q.x, 1e-12); assertEquals(0.7, q.y, 1e-12)
        assertEquals(sqrt(5.0), Geometry.value(cobj("A=1+2i", "{Modulus}(A)"))!!, 1e-12)
        val c = cobj("A=1+2i", "{Conjugate}(A)") as Geometry.Point
        assertEquals(-2.0, c.y, 0.0)
    }

    @Test fun draggedPointsReadBack() {
        // A dragged point on the complex plane is rewritten as a number, its sign as typed (−).
        val p = cobj("A=2−0.5i") as Geometry.Point
        assertEquals(2.0, p.x, 1e-12); assertEquals(-0.5, p.y, 1e-12)
        val q = cobj("A=0+3i") as Geometry.Point
        assertEquals(0.0, q.x, 1e-12); assertEquals(3.0, q.y, 1e-12)
        // In space a dragged point stays free (A = (x, y, z)).
        assertTrue(Geometry3D.isFree(Geometry3D.parse(row("A=(1.5,−2,0)"))!!))
    }

    @Test fun rootsAndImages() {
        val roots = cobj("{RootsOfUnity}(4)") as Geometry.Many
        assertEquals(4, roots.items.size)
        val i = roots.items[1] as Geometry.Point
        assertEquals(0.0, i.x, 1e-12); assertEquals(1.0, i.y, 1e-12)
        // f(z) = z² takes 1 + i to 2i.
        val w = cobj("A=1+i", "{Image}(f,A)") as Geometry.Point
        assertEquals(0.0, w.x, 1e-12); assertEquals(2.0, w.y, 1e-12)
        // The unit circle goes to the unit circle (traced twice).
        val img = cobj("O=(0,0)", "c={Circle}(O,1)", "{Image}(f,c)") as Geometry.Polyline
        assertTrue(img.pieces.flatten().all { kotlin.math.abs(kotlin.math.hypot(it.x, it.y) - 1) < 1e-9 })
    }
}
