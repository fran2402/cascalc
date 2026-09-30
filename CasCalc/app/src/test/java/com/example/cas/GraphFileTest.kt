package com.example.cas

import com.example.cas.graph.GraphFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphFileTest {
    private val data = mapOf(
        "functions" to "y=x^2\n\\frac{1}{x}\ttab",
        "colors" to ",-16776961",
        "notes" to "[null,{\"text\":\"a\\\\b\"}]",
        "parameters" to "a\t1.5\nb\t2",
    )

    @Test fun roundTrip() {
        for (kind in GraphFile.Kind.entries) {
            val c = GraphFile.Contents(kind, "Pendulum\twith tab", data)
            assertEquals(c, GraphFile.read(GraphFile.write(c)))
        }
    }

    @Test fun kindFromTheFileName() {
        val bytes = GraphFile.write(GraphFile.Contents(GraphFile.Kind.Graph3D, "", data)).toString(Charsets.UTF_8)
            .lines().filterNot { it.startsWith("kind\t") }.joinToString("\n").toByteArray()
        val c = GraphFile.read(bytes, "Saddle.g3d")
        assertEquals(GraphFile.Kind.Graph3D, c.kind)
        assertEquals("Saddle", c.name)
    }

    @Test fun rejectsOtherFiles() {
        val e = runCatching { GraphFile.read("x,y\n1,2".toByteArray(), "data.g2d") }.exceptionOrNull()
        assertTrue(e is IllegalArgumentException)
        val newer = runCatching { GraphFile.read("CasCalc graph 99\nkind\tg2d\nfunctions\tx".toByteArray()) }.exceptionOrNull()
        assertTrue(newer!!.message!!.contains("newer"))
    }

}
