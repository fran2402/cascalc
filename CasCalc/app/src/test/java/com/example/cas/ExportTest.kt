package com.example.cas

import com.example.cas.graph.Png
import com.example.cas.graph.Scene
import com.example.cas.graph.SvgWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.Inflater
import javax.xml.parsers.DocumentBuilderFactory

class ExportTest {
    private fun scene() = Scene(400.0, 300.0, 0xFFFCF9EE.toInt()).apply {
        add(Scene.Stroke(listOf(doubleArrayOf(0.0, 150.0, 400.0, 150.0)), 0xFF47473B.toInt(), 2.0))
        add(Scene.Stroke(listOf(doubleArrayOf(10.0, 10.0, 50.5, 20.25, 90.0, 5.0)), 0x805B6133.toInt(), 3.0, dash = doubleArrayOf(12.0, 9.0)))
        add(Scene.Fill(listOf(doubleArrayOf(0.0, 0.0, 10.0, 0.0, 10.0, 10.0)), 0x385B6133.toInt()))
        add(Scene.Circle(20.0, 20.0, 5.0, fill = 0xFFFFFFFF.toInt(), stroke = 0xFF000000.toInt(), strokeWidth = 2.0))
        add(Scene.Label(200.0, 160.0, "−2 < x & y", 11.0, 0xFF47473B.toInt(), Scene.Anchor.Middle))
        add(Scene.Image(0.0, 0.0, 400.0, 300.0, IntArray(6) { 0xFF102030.toInt() }, 3, 2))
    }

    @Test fun svgIsWellFormed() {
        val svg = SvgWriter.write(scene())
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(svg.byteInputStream())
        assertEquals("svg", doc.documentElement.tagName)
        assertEquals("400", doc.documentElement.getAttribute("width"))
        assertEquals(1, doc.getElementsByTagName("text").length)
        assertEquals("−2 < x & y", doc.getElementsByTagName("text").item(0).textContent)
        assertEquals(1, doc.getElementsByTagName("circle").length)
        assertEquals(1, doc.getElementsByTagName("image").length)
    }

    @Test fun svgKeepsTransparencyAndDashes() {
        val svg = SvgWriter.write(scene())
        assertTrue(svg, svg.contains("stroke=\"#5B6133\" stroke-opacity=\"0.5\""))
        assertTrue(svg, svg.contains("stroke-dasharray=\"12 9\""))
        assertTrue(svg, svg.contains("M10 10L50.5 20.25L90 5"))
        assertTrue(svg, svg.contains("fill=\"#5B6133\" fill-opacity=\"0.22\""))
    }

    @Test fun pngDecodes() {
        val px = IntArray(12) { k -> if (k % 2 == 0) 0xFFFF0000.toInt() else 0x8000FF00.toInt() }
        val (w, h, argb) = decodePng(Png.encode(px, 4, 3))
        assertEquals(4, w)
        assertEquals(3, h)
        assertEquals(0xFFFF0000.toInt(), argb[0])
        assertEquals(0x8000FF00.toInt(), argb[1])
    }

    /** A minimal decoder for 8-bit RGBA PNGs (javax.imageio isn't on Android's test classpath). */
    private fun decodePng(bytes: ByteArray): Triple<Int, Int, IntArray> {
        fun int(at: Int) = (0..3).fold(0) { a, k -> (a shl 8) or (bytes[at + k].toInt() and 0xFF) }
        assertEquals(listOf(137, 80, 78, 71, 13, 10, 26, 10), (0..7).map { bytes[it].toInt() and 0xFF })
        var w = 0; var h = 0
        val idat = ByteArrayOutputStream()
        var at = 8
        while (at < bytes.size) {
            val len = int(at)
            val type = String(bytes, at + 4, 4, Charsets.US_ASCII)
            if (type == "IHDR") {
                w = int(at + 8); h = int(at + 12)
                assertEquals(8, bytes[at + 16].toInt()); assertEquals(6, bytes[at + 17].toInt())
            }
            if (type == "IDAT") idat.write(bytes, at + 8, len)
            at += 12 + len
        }
        val stride = w * 4
        val raw = ByteArray(h * (stride + 1))
        Inflater().run { setInput(idat.toByteArray()); var n = 0; while (n < raw.size && !finished()) n += inflate(raw, n, raw.size - n); end() }
        val out = ByteArray(h * stride)
        for (y in 0 until h) {
            val f = raw[y * (stride + 1)].toInt()
            for (x in 0 until stride) {
                val v = raw[y * (stride + 1) + 1 + x].toInt() and 0xFF
                val a = if (x >= 4) out[y * stride + x - 4].toInt() and 0xFF else 0
                val b = if (y > 0) out[(y - 1) * stride + x].toInt() and 0xFF else 0
                val c = if (x >= 4 && y > 0) out[(y - 1) * stride + x - 4].toInt() and 0xFF else 0
                val pred = when (f) {
                    0 -> 0; 1 -> a; 2 -> b; 3 -> (a + b) / 2
                    else -> { val p = a + b - c; val pa = kotlin.math.abs(p - a); val pb = kotlin.math.abs(p - b); val pc = kotlin.math.abs(p - c)
                        if (pa <= pb && pa <= pc) a else if (pb <= pc) b else c }
                }
                out[y * stride + x] = (v + pred).toByte()
            }
        }
        val argb = IntArray(w * h) { k ->
            val o = k * 4
            ((out[o + 3].toInt() and 0xFF) shl 24) or ((out[o].toInt() and 0xFF) shl 16) or ((out[o + 1].toInt() and 0xFF) shl 8) or (out[o + 2].toInt() and 0xFF)
        }
        return Triple(w, h, argb)
    }

    @Test fun italicLettersInRomanLabels() {
        val sc = Scene(100.0, 100.0, 0xFFFFFFFF.toInt())
        sc.add(Scene.Label(50.0, 50.0, "Re z", 12.0, 0xFF000000.toInt(), Scene.Anchor.Middle, Scene.Font.Roman, italic = setOf('z')))
        val svg = SvgWriter.write(sc)
        assertTrue(svg, svg.contains(">Re <tspan font-style=\"italic\">z</tspan></text>"))
        assertEquals(listOf("Re " to false, "z" to true), (sc.items.single() as Scene.Label).runs())
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(svg.byteInputStream())
    }
}
