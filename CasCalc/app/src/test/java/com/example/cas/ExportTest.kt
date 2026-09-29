package com.example.cas

import com.example.cas.graph.Png
import com.example.cas.graph.Scene
import com.example.cas.graph.SvgWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.imageio.ImageIO
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
        val img = ImageIO.read(Png.encode(px, 4, 3).inputStream())
        assertEquals(4, img.width)
        assertEquals(3, img.height)
        assertEquals(0xFFFF0000.toInt(), img.getRGB(0, 0))
        assertEquals(0x8000FF00.toInt(), img.getRGB(1, 0))
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
