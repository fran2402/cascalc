package com.example.cas

import com.example.cas.ui.ColorMath
import com.example.cas.ui.TonalScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow

class TonalSchemeTest {
    private fun luminance(argb: Int): Double {
        val c = ColorMath.fromArgb(argb)
        fun lin(u: Int) = (u / 255.0).let { if (it <= 0.04045) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }
        return 0.2126 * lin(c.r) + 0.7152 * lin(c.g) + 0.0722 * lin(c.b)
    }
    private fun contrast(x: Int, y: Int): Double {
        val (a, b) = luminance(x) to luminance(y)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }
    /** CIELAB L* of a color. */
    private fun lstar(argb: Int): Double {
        val y = luminance(argb)
        return if (y > 216.0 / 24389) 116 * Math.cbrt(y) - 16 else y * 24389 / 27
    }

    private val seeds = TonalScheme.PRESETS.map { it.second }.filter { it != 0 } + listOf(0xFF00FF00.toInt(), 0xFFFFFF00.toInt(), 0xFF0000FF.toInt())

    @Test fun textIsReadableOnItsBackground() {
        for (seed in seeds) for (dark in listOf(false, true)) {
            val s = TonalScheme.from(seed, dark)
            listOf(
                s.primary to s.onPrimary, s.primaryContainer to s.onPrimaryContainer,
                s.secondaryContainer to s.onSecondaryContainer, s.tertiary to s.onTertiary,
                s.tertiaryContainer to s.onTertiaryContainer, s.surface to s.onSurface,
                s.surfaceContainerHighest to s.onSurface, s.surfaceVariant to s.onSurfaceVariant,
            ).forEach { (bg, fg) -> assertTrue("seed ${Integer.toHexString(seed)} dark=$dark: ${contrast(bg, fg)}", contrast(bg, fg) >= 4.5) }
        }
    }

    @Test fun tonesAreLightness() {
        for (seed in seeds) for (t in listOf(10.0, 30.0, 50.0, 80.0, 95.0)) {
            val c = TonalScheme.tone(t, 1.0, 0.1)
            assertTrue("tone $t gave ${lstar(c)}", abs(lstar(c) - t) < 1.5)
        }
    }

    @Test fun surfacesStepUpInLightness() {
        for (dark in listOf(false, true)) {
            val s = TonalScheme.from(0xFF3F6FD8.toInt(), dark)
            val steps = listOf(s.surfaceContainerLowest, s.surfaceContainerLow, s.surfaceContainer, s.surfaceContainerHigh, s.surfaceContainerHighest).map { lstar(it) }
            // Light: lowest is brightest; dark: lowest is darkest.
            val ordered = if (dark) steps else steps.reversed()
            assertEquals(ordered.sorted(), ordered)
        }
    }

    @Test fun greySeedGivesGreys() {
        val s = TonalScheme.from(0xFF777777.toInt(), dark = false)
        val c = ColorMath.fromArgb(s.primary)
        assertTrue("$c", maxOf(c.r, c.g, c.b) - minOf(c.r, c.g, c.b) <= 2)
    }

    @Test fun primaryKeepsTheSeedsHue() {
        val (_, a, b) = ColorMath.toOklab(ColorMath.fromArgb(TonalScheme.from(0xFFC23B32.toInt(), dark = false).primary))
        // Red: a clearly positive (towards red), not blue or green.
        assertTrue("a=$a b=$b", a > 0.05 && a > abs(b) * 0.5)
    }
}
