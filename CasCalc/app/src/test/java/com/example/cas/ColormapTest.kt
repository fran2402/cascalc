package com.example.cas

import com.example.cas.cas.CD
import com.example.cas.graph.ColoringOptions
import com.example.cas.graph.Colormap
import com.example.cas.graph.DomainColoring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ColormapTest {
    private fun rgb(c: Int) = Triple((c shr 16) and 0xFF, (c shr 8) and 0xFF, c and 0xFF)
    private fun close(a: Int, b: Int, tol: Int = 2) = rgb(a).toList().zip(rgb(b).toList()).all { (x, y) -> abs(x - y) <= tol }

    @Test fun classicIsTheHueWheel() {
        assertEquals(Triple(255, 0, 0), rgb(Colormap.CLASSIC.rgb(0.0)))
        assertEquals(Triple(0, 255, 0), rgb(Colormap.CLASSIC.rgb(1.0 / 3)))
        assertEquals(Triple(0, 0, 255), rgb(Colormap.CLASSIC.rgb(2.0 / 3)))
    }

    @Test fun cyclicMapsJoinUp() {
        for (m in Colormap.ALL.filter { it.cyclic }) assertTrue(m.name, close(m.rgb(0.0), m.rgb(0.99999), 6))
    }

    @Test fun viridisEnds() {
        // matplotlib's viridis runs from #440154 to #FDE725.
        assertTrue(close(Colormap.VIRIDIS.rgb(0.0), 0x440154))
        assertTrue(close(Colormap.VIRIDIS.rgb(1.0), 0xFDE725))
    }

    @Test fun classicOptionMatchesTheOldColouring() {
        // f = i: arg π/2, a quarter of the way round (chartreuse–green), at the middle brightness.
        val c = DomainColoring.color(CD(0.0, 1.0), ColoringOptions(modulusBands = false))
        assertEquals(c, DomainColoring.color(CD(0.0, 1.0), ColoringOptions(modulusBands = false, colormap = Colormap.CLASSIC)))
    }

    @Test fun zerosAndPolesStayBlackAndWhite() {
        for (m in Colormap.ALL) {
            assertEquals(m.name, Triple(0, 0, 0), rgb(DomainColoring.color(CD(0.0), ColoringOptions(modulusBands = false, colormap = m))))
            assertEquals(m.name, Triple(255, 255, 255), rgb(DomainColoring.color(CD(Double.POSITIVE_INFINITY), ColoringOptions(colormap = m))))
        }
    }

    @Test fun unitModulusShowsTheMapsOwnColour() {
        // |f| = 1 is the middle brightness, so the map's color shows unchanged.
        val w = CD(-1.0, 1e-12)
        val c = DomainColoring.color(w, ColoringOptions(modulusBands = false, colormap = Colormap.VIRIDIS))
        assertTrue(close(c, Colormap.VIRIDIS.rgb(1.0)))
    }

    @Test fun everyMatplotlibMapIsThere() {
        // matplotlib's 86, the classic wheel, the theme's own map, the three more made from the
        // theme's colors and four more single hues.
        assertEquals(95, Colormap.ALL.size)
        assertEquals(Colormap.ALL.size, Colormap.ALL.map { it.name }.toSet().size)
        for (n in listOf("RdBu", "coolwarm", "tab10", "cubehelix", "berlin", "gist_ncar")) assertEquals(n, Colormap.byName(n).name)
    }

    @Test fun themeMapFollowsTheUiColor() {
        val before = Colormap.themePrimary
        try {
            // The theme map is a ramp in the primary color's own hue: its deep end has that hue.
            fun hue(c: Int): Float { val hsv = FloatArray(3); val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val b = c and 0xFF
                val max = maxOf(r, g, b); val min = minOf(r, g, b); val d = (max - min).toFloat(); if (d == 0f) return 0f
                val h = when (max) { r -> 60 * (((g - b) / d) % 6); g -> 60 * ((b - r) / d + 2); else -> 60 * ((r - g) / d + 4) }
                return if (h < 0) h + 360 else h }
            for (c in listOf(0xFF3A6EA5.toInt(), 0xFF386A20.toInt(), 0xFF6750A4.toInt(), 0xFF8E4957.toInt())) {
                Colormap.themePrimary = c
                val d = kotlin.math.abs(hue(Colormap.THEME.rgb(0.8)) - hue(c and 0xFFFFFF))
                assertTrue("hue of ${Integer.toHexString(c)}", minOf(d, 360 - d) < 25)
            }
            // The theme map is first among the default favorites; the old Tonal name reads as it.
            assertEquals("theme", Colormap.DEFAULT_FAVORITES.first())
            assertEquals(Colormap.THEME, Colormap.byName("theme_tonal"))
        } finally { Colormap.themePrimary = before }
    }

    @Test fun generatedRampsRunLightToDark() {
        for (n in listOf("Yellows", "Teals", "Magentas", "Pinks")) {
            val m = Colormap.byName(n)
            assertEquals(n, m.name)
            fun lum(c: Int) = 0.2126 * ((c shr 16) and 0xFF) + 0.7152 * ((c shr 8) and 0xFF) + 0.0722 * (c and 0xFF)
            var prev = 999.0
            for (k in 0..10) { val v = lum(m.rgb(k / 10.0)); assertTrue("$n gets darker", v < prev + 1e-9); prev = v }
        }
    }

    @Test fun reversedNamesRoundTrip() {
        assertEquals(Colormap.VIRIDIS to true, Colormap.parse("viridis_r"))
        assertEquals(Colormap.VIRIDIS to false, Colormap.parse("viridis"))
        assertEquals("RdBu_r", Colormap.save(Colormap.byName("RdBu"), true))
        // Saves from before the list grew used upper-case names.
        assertEquals(Colormap.byName("twilight_shifted") to false, Colormap.parse("TWILIGHT_SHIFTED"))
    }

    @Test fun reversedRunsBackwards() {
        val w = CD(-1.0, 1e-12)
        val c = DomainColoring.color(w, ColoringOptions(modulusBands = false, colormap = Colormap.VIRIDIS, reversed = true))
        assertTrue(close(c, Colormap.VIRIDIS.rgb(0.0)))
    }

    @Test fun qualitativeMapsKeepTheirColours() {
        // tab10's first color, #1f77b4, unblended.
        assertTrue(close(Colormap.byName("tab10").rgb(0.02), 0x1F77B4, 0))
    }

    @Test fun namesAreOneWordAndDistinct() {
        for (m in Colormap.ALL) assertTrue(m.label, m.label.none { it == '_' || it == ' ' })
        assertEquals(Colormap.ALL.size, Colormap.ALL.map { it.label.lowercase() }.toSet().size)
        assertEquals("Dusk", Colormap.byName("twilight_shifted").label)
        assertEquals("Viridis", Colormap.VIRIDIS.label)
    }

    @Test fun byNameFallsBackToClassic() {
        assertEquals(Colormap.MAGMA, Colormap.byName("MAGMA"))
        assertEquals(Colormap.CLASSIC, Colormap.byName("nonsense"))
        assertEquals(Colormap.CLASSIC, Colormap.byName(null))
    }

    @Test fun themeMapsFollowTheTheme() {
        val tonal = Colormap.THEME
        Colormap.themePrimary = 0xFF1E5BB8.toInt(); Colormap.themeTertiary = 0xFFB8341E.toInt()
        val blue = tonal.rgb(0.8)
        Colormap.themePrimary = 0xFF2E7D32.toInt()
        val green = tonal.rgb(0.8)
        assertTrue(blue != green)
        // Split: primary at one end, tertiary at the other, near white in the middle.
        val split = Colormap.byName("theme_diverging")
        val mid = split.rgb(0.5)
        assertTrue(((mid shr 16) and 0xFF) > 200 && ((mid shr 8) and 0xFF) > 200 && (mid and 0xFF) > 200)
        // Loop joins up.
        val loop = Colormap.byName("theme_loop")
        assertTrue(loop.cyclic)
        assertTrue(close(loop.rgb(0.0), loop.rgb(1.0)))
        Colormap.themePrimary = 0xFF5B6133.toInt(); Colormap.themeTertiary = 0xFF3A665A.toInt()
    }
}
