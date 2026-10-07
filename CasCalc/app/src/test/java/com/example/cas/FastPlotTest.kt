package com.example.cas

import com.example.cas.graph.Bounds
import com.example.cas.graph.Camera
import com.example.cas.graph.Field
import com.example.cas.graph.Mesh
import com.example.cas.graph.Parallel
import com.example.cas.graph.Surface3D
import com.example.cas.graph.Viewport
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicIntegerArray

/** The plotting that runs on every core gives exactly what one thread did. */
class FastPlotTest {
    @Test fun everyRowOnce() {
        val seen = AtomicIntegerArray(1000)
        assertTrue(Parallel.rows(1000, { }) { _, a, b -> for (r in a until b) seen.incrementAndGet(r) })
        for (r in 0 until 1000) assertEquals(1, seen.get(r))
    }

    @Test fun eachWorkerItsOwnState() {
        // Each worker's state is only ever touched by that worker.
        val states = java.util.Collections.synchronizedList(ArrayList<IntArray>())
        Parallel.rows(5000, { IntArray(1).also { states += it } }) { s, a, b -> for (r in a until b) s[0]++ }
        assertEquals(5000, states.sumOf { it[0] })
    }

    @Test fun failuresComeBack() {
        val e = runCatching { Parallel.rows(100, { }) { _, a, _ -> if (a >= 50) throw IllegalStateException("boom") } }.exceptionOrNull()
        assertEquals("boom", e?.message)
    }

    @Test fun stopLeavesTheRest() = assertFalse(Parallel.rows(10_000, { }, stop = { true }) { _, _, _ -> })

    @Test fun fieldMatchesOneThread() {
        val view = Viewport(-3.0, 2.0, -1.0, 4.0)
        val f = { x: Double, y: Double -> kotlin.math.sin(x * y) + x }
        val v = Field.sample(f, view, 137, 91)
        for (j in 0 until 91) for (i in 0 until 137) {
            val x = view.xMin + (i + 0.5) / 137 * view.width
            val y = view.yMax - (j + 0.5) / 91 * view.height
            assertEquals(f(x, y), v[j * 137 + i], 0.0)
        }
    }

    @Test fun fieldRangeOfAHugeGrid() {
        val v = DoubleArray(400_000) { it / 400_000.0 }
        val (lo, hi) = Field.range(v)
        assertEquals(0.02, lo, 1e-3); assertEquals(0.98, hi, 1e-3)
    }

    private val box = Bounds(-2.0, 2.0, -2.0, 2.0, -2.0, 2.0)

    @Test fun projectionMatchesTheFaces() {
        val polys = Surface3D.explicit({ x, y -> kotlin.math.sin(x) * kotlin.math.cos(y) }, box, 30) +
            Surface3D.implicit({ x, y, z -> x * x + y * y + z * z - 1 }, box, 14)
        for (cam in listOf(Camera(), Camera(yaw = 2.1, pitch = -0.4, zoom = 1.7), Camera(yaw = -0.9, pitch = 1.2))) {
            val faces = Surface3D.faces(polys, box, cam, 500f, 400f)
            val p = Surface3D.project(Mesh(polys, box), cam, 500f, 400f)
            // Same faces far to near (ties may swap), each with the same corners and shade.
            for ((o, face) in faces.withIndex()) {
                val k = p.order[o]
                val a = p.mesh.start[k]
                assertEquals(face.xs.size, p.mesh.start[k + 1] - a)
                if (face.depth != faces.getOrNull(o + 1)?.depth && face.depth != faces.getOrNull(o - 1)?.depth) {
                    for (c in face.xs.indices) { assertEquals(face.xs[c], p.px[a + c], 0.05f); assertEquals(face.ys[c], p.py[a + c], 0.05f) }
                    assertEquals(face.shade, p.shade[k], 1e-3f)
                }
            }
        }
    }

    @Test fun pickFindsTheSameFace() {
        val polys = Surface3D.explicit({ _, _ -> 0.5 }, box, 20)
        val mesh = Mesh(polys, box)
        val p = Surface3D.project(mesh, Camera(), 400f, 400f)
        val faces = Surface3D.faces(polys, box, Camera(), 400f, 400f)
        for ((x, y) in listOf(200f to 200f, 150f to 230f, 260f to 180f)) {
            val old = Surface3D.pick(faces, x, y)!!
            val k = Surface3D.pick(p, x, y)
            assertArrayEquals(old.center, DoubleArray(3) { mesh.center[3 * k + it] }, 1e-9)
        }
        assertEquals(-1, Surface3D.pick(p, 2f, 2f))
    }

    @Test fun trianglesForFacesAndLines() {
        val polys = Surface3D.explicit({ x, y -> x * y / 4 }, box, 48)
        val p = Surface3D.project(Mesh(polys, box), Camera(), 400f, 400f)
        val t = Surface3D.triangles(p, 1.5f, { _, _ -> 0xFF112233.toInt() }) { 0xFF000000.toInt() }
        val lines = polys.sumOf { Integer.bitCount(it.edges) }
        assertEquals((polys.size * 2 + lines * 2) * 6, t.count)
        assertEquals(t.count / 2, t.colors.size)
        // 48 cells across: mesh lines on every 2nd grid line, so 25 lines each way, not 49.
        assertTrue(lines < polys.size * 4 / 2 + 200)
    }
}
