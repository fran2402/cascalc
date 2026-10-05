package com.example.cas.graph

import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * A color map for the phase of f(z) on the complex plane, as in matplotlib. [CLASSIC] is the
 * usual domain-coloring wheel (the hue of the HSV wheel, shaded as HSL); the rest are every map
 * on matplotlib's "Choosing Colormaps" page, sampled from matplotlib (see [COLORMAP_DATA]) and
 * blended between samples. Cyclic maps join up at ±π, so they show no seam along the negative
 * real axis; the others run from arg f = −π to π, with a seam there.
 */
class Colormap private constructor(
    /** matplotlib's name for it (and how it's saved): viridis, RdBu, twilight_shifted… */
    val name: String,
    /** Its group on matplotlib's page: Perceptually uniform, Sequential, Diverging… */
    val category: String,
    val cyclic: Boolean,
    /** Qualitative maps are a few separate colors, shown without blending. */
    private val qualitative: Boolean,
    private val samples: IntArray?,
) {
    /** Shown in the app: one word, from matplotlib's name (Viridis, RdBu, Dusk for twilight_shifted). */
    val label: String get() = LABELS[name] ?: name.replaceFirstChar { it.uppercaseChar() }

    /** The color (RGB, no alpha) at [t] from 0 to 1 (arg f from −π to π for the non-cyclic maps). */
    fun rgb(t: Double): Int {
        // The theme's map: the sequential map matching the app's colors, from just past its
        // near-white end (which would hide the phase under the modulus shading).
        if (name == THEME_NAME) return themeMap().rgb(0.14 + 0.86 * t.coerceIn(0.0, 1.0))
        val s = samples ?: return hue(t)
        val n = s.size
        if (qualitative) return s[(t.coerceIn(0.0, 1.0) * n).toInt().coerceIn(0, n - 1)]
        if (cyclic) {
            val x = (t - floor(t)) * n
            val k = floor(x).toInt().coerceIn(0, n - 1)
            return mix(s[k], s[(k + 1) % n], x - k)
        }
        val x = t.coerceIn(0.0, 1.0) * (n - 1)
        val k = floor(x).toInt().coerceIn(0, n - 2)
        return mix(s[k], s[k + 1], x - k)
    }

    override fun equals(other: Any?) = other is Colormap && other.name == name
    override fun hashCode() = name.hashCode()
    override fun toString() = name

    companion object {
        /** The usual domain-coloring wheel. */
        val CLASSIC = Colormap("classic", "Cyclic", cyclic = true, qualitative = false, samples = null)

        private const val THEME_NAME = "theme"

        /**
         * The app's own colors: whichever of matplotlib's sequential maps matches the primary
         * color of the UI (Greens for a green theme, Blues for a blue one…), following it when
         * the colors change. It's the default until the list of favorites is changed.
         */
        val THEME = Colormap(THEME_NAME, "Theme", cyclic = false, qualitative = false, samples = null)

        /** The UI's primary color (ARGB), set by the app's theme; the [THEME] map follows it. */
        @Volatile var themePrimary: Int = 0xFF5B6133.toInt()

        /** The sequential map for [themePrimary]: by its hue, or Greys for a near-grey theme. */
        fun themeMap(): Colormap {
            val r = ((themePrimary shr 16) and 0xFF) / 255.0
            val g = ((themePrimary shr 8) and 0xFF) / 255.0
            val b = (themePrimary and 0xFF) / 255.0
            val max = maxOf(r, g, b); val min = minOf(r, g, b); val d = max - min
            if (d < 0.08) return byName("Greys")
            var h = when (max) {
                r -> 60 * (((g - b) / d) % 6)
                g -> 60 * ((b - r) / d + 2)
                else -> 60 * ((r - g) / d + 4)
            }
            if (h < 0) h += 360
            val name = when {
                h < 18 || h >= 340 -> "Reds"
                h < 40 -> "Oranges"
                h < 58 -> "YlOrBr"
                h < 95 -> "YlGn"
                h < 155 -> "Greens"
                h < 195 -> "BuGn"
                h < 245 -> "Blues"
                h < 290 -> "Purples"
                else -> "RdPu"
            }
            return byName(name)
        }

        /** Every map: the classic wheel, then matplotlib's in the order of its page. */
        val ALL: List<Colormap> by lazy {
            listOf(CLASSIC, THEME) + COLORMAP_DATA.map { d ->
                Colormap(d.name, d.category, d.cyclic, d.qualitative, IntArray(d.hex.length / 6) { k -> d.hex.substring(6 * k, 6 * k + 6).toInt(16) })
            }
        }

        val VIRIDIS get() = byName("viridis")
        val MAGMA get() = byName("magma")

        /** The groups in order, for listing. */
        val CATEGORIES: List<String> by lazy { ALL.map { it.category }.distinct() }

        /** One-word names for matplotlib's longer ones (the gist_ maps, nipy_spectral…). */
        private val LABELS = mapOf(
            "twilight_shifted" to "Dusk", "gist_yarg" to "Yarg", "gist_gray" to "Graphite", "gist_heat" to "Heat",
            "gist_earth" to "Earth", "gist_stern" to "Stern", "gist_rainbow" to "Spectrum", "gist_ncar" to "Ncar",
            "nipy_spectral" to "Nipy", "CMRmap" to "CMRmap", "theme" to "Theme",
        )

        /** The maps offered first until the list is changed. */
        val DEFAULT_FAVORITES = listOf("theme", "classic", "twilight", "twilight_shifted", "viridis", "plasma", "magma", "cividis", "turbo")

        /**
         * A map by its saved name; older saves used upper-case names (VIRIDIS, TWILIGHT_SHIFTED),
         * so case is ignored when nothing matches exactly. Unknown names give [CLASSIC].
         */
        fun byName(name: String?): Colormap {
            if (name == null) return CLASSIC
            return ALL.firstOrNull { it.name == name } ?: ALL.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: CLASSIC
        }

        /** A saved name, where a trailing _r means the map reversed (viridis_r), as in matplotlib. */
        fun parse(saved: String?): Pair<Colormap, Boolean> {
            if (saved != null && saved.endsWith("_r") && ALL.none { it.name == saved }) return byName(saved.removeSuffix("_r")) to true
            return byName(saved) to false
        }

        /** The name to save: with _r when reversed. */
        fun save(map: Colormap, reversed: Boolean) = map.name + if (reversed) "_r" else ""

        /** The fully saturated hue at [t] turns round the color wheel (red at 0). */
        private fun hue(t: Double): Int {
            val h = (t - floor(t)) * 6
            val x = 1 - kotlin.math.abs(h % 2 - 1)
            val (r, g, b) = when (h.toInt().coerceIn(0, 5)) {
                0 -> Triple(1.0, x, 0.0); 1 -> Triple(x, 1.0, 0.0); 2 -> Triple(0.0, 1.0, x)
                3 -> Triple(0.0, x, 1.0); 4 -> Triple(x, 0.0, 1.0); else -> Triple(1.0, 0.0, x)
            }
            return ((r * 255).roundToInt() shl 16) or ((g * 255).roundToInt() shl 8) or (b * 255).roundToInt()
        }

        private fun mix(a: Int, b: Int, f: Double): Int {
            fun ch(shift: Int) = (((a shr shift) and 0xFF) * (1 - f) + ((b shr shift) and 0xFF) * f).roundToInt().coerceIn(0, 255)
            return (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
        }
    }
}
