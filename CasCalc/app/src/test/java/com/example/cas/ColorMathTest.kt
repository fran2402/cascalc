package com.example.cas

import com.example.cas.ui.ColorMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ColorMathTest {
    @Test fun hexRoundTrip() = assertEquals("#1E88E5", ColorMath.hex(ColorMath.parseHex("#1e88e5")!!))
    @Test fun shortHex() = assertEquals(ColorMath.Rgb(255, 170, 0), ColorMath.parseHex("fa0"))
    @Test fun badHex() = assertEquals(null, ColorMath.parseHex("#12345"))
    @Test fun hsvOfRed() = assertEquals(Triple(0.0, 100.0, 100.0), ColorMath.toHsv(ColorMath.Rgb(255, 0, 0)))
    @Test fun hsvRoundTrip() {
        val c = ColorMath.Rgb(30, 136, 229)
        val (h, s, v) = ColorMath.toHsv(c)
        assertEquals(c, ColorMath.fromHsv(h, s, v))
    }
    // Reference values from Björn Ottosson's OKLab post.
    @Test fun oklabOfWhite() {
        val (l, a, b) = ColorMath.toOklab(ColorMath.Rgb(255, 255, 255))
        assertTrue(abs(l - 1) < 1e-4 && abs(a) < 1e-4 && abs(b) < 1e-4)
    }
    @Test fun oklabOfRed() {
        val (l, a, b) = ColorMath.toOklab(ColorMath.Rgb(255, 0, 0))
        assertTrue("$l $a $b", abs(l - 0.627955) < 1e-4 && abs(a - 0.224863) < 1e-4 && abs(b - 0.125846) < 1e-4)
    }
    @Test fun oklabRoundTrip() {
        for (c in listOf(ColorMath.Rgb(30, 136, 229), ColorMath.Rgb(216, 27, 96), ColorMath.Rgb(0, 0, 0), ColorMath.Rgb(128, 128, 128))) {
            val (l, a, b) = ColorMath.toOklab(c)
            assertEquals(c, ColorMath.fromOklab(l, a, b))
        }
    }
    @Test fun argb() = assertEquals(0xFF1E88E5.toInt(), ColorMath.Rgb(30, 136, 229).argb)
}
