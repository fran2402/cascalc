package com.example.cas.graph

import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * A colour map for the phase of f(z) on the complex plane, as in matplotlib. [CLASSIC] is the
 * usual domain-colouring wheel (the hue of the HSV wheel, shaded as HSL); the rest are every map
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
    /** Qualitative maps are a few separate colours, shown without blending. */
    private val qualitative: Boolean,
    private val samples: IntArray?,
) {
    /** Shown in the app: the name as matplotlib writes it. */
    val label: String get() = name

    /** The colour (RGB, no alpha) at [t] from 0 to 1 (arg f from −π to π for the non-cyclic maps). */
    fun rgb(t: Double): Int {
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
        /** The usual domain-colouring wheel. */
        val CLASSIC = Colormap("classic", "Cyclic", cyclic = true, qualitative = false, samples = null)

        /** Every map: the classic wheel, then matplotlib's in the order of its page. */
        val ALL: List<Colormap> by lazy {
            listOf(CLASSIC) + COLORMAP_DATA.map { d ->
                Colormap(d.name, d.category, d.cyclic, d.qualitative, IntArray(d.hex.length / 6) { k -> d.hex.substring(6 * k, 6 * k + 6).toInt(16) })
            }
        }

        val VIRIDIS get() = byName("viridis")
        val MAGMA get() = byName("magma")

        /** The groups in order, for listing. */
        val CATEGORIES: List<String> by lazy { ALL.map { it.category }.distinct() }

        /** The maps offered first until the list is changed. */
        val DEFAULT_FAVOURITES = listOf("classic", "twilight", "twilight_shifted", "viridis", "plasma", "magma", "cividis", "turbo")

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

        /** The fully saturated hue at [t] turns round the colour wheel (red at 0). */
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
