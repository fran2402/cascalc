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
        if (name == THEME_NAME) return themeSamples(TONAL)!!.let { r -> r[((0.14 + 0.86 * t.coerceIn(0.0, 1.0)) * (r.size - 1)).roundToInt()] }
        val s = samples ?: themeSamples(name) ?: return hue(t)
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
         * The app's own colors: a ramp in exactly the hue of the UI's primary color, from light to
         * deep, following it when the colors change. It's the default until the list of favorites
         * is changed.
         */
        val THEME = Colormap(THEME_NAME, "Theme", cyclic = false, qualitative = false, samples = null)

        /** The UI's primary color (ARGB), set by the app's theme; the [THEME] map follows it. */
        @Volatile var themePrimary: Int = 0xFF5B6133.toInt()
        /** The UI's tertiary color (ARGB): the second hue of the two-color theme maps. */
        @Volatile var themeTertiary: Int = 0xFF3A665A.toInt()

        /**
         * Maps made from the theme's own colors rather than matched to matplotlib's: name, label
         * and whether it's cyclic. They follow the colors when the theme changes.
         *  - Duo: from deep primary to light tertiary, the hue turning between them.
         *  - Diverging: deep primary, through near-white, to deep tertiary (for signs, ± values).
         *  - Loop: cyclic, light, deep primary, light, deep tertiary and back (for arg f, angles).
         */
        /** The [THEME] map's ramp, worked out with the others (not listed: it's [THEME] itself, saved as "theme"). */
        private const val TONAL = "theme_tonal"
        private val THEME_MAPS = listOf(Triple("theme_duo", "Duo", false),
            Triple("theme_diverging", "Split", false), Triple("theme_loop", "Loop", true))

        @Volatile private var themeCache: Triple<Int, Int, Map<String, IntArray>>? = null

        /** The samples of a theme map for the current theme colors (worked out once per theme). */
        private fun themeSamples(name: String): IntArray? {
            if (!name.startsWith("theme_")) return null
            val p = themePrimary; val q = themeTertiary
            themeCache?.let { (a, b, m) -> if (a == p && b == q) return m[name] }
            val hp = hueOf(p); val hq = hueOf(q)
            val n = 256
            val tonalP = ramp(hp, n); val tonalQ = ramp(hq, n)
            fun deep(r: IntArray, d: Double) = r[(d.coerceIn(0.0, 1.0) * (n - 1)).roundToInt()]
            val maps = mapOf(
                "theme_tonal" to tonalP,
                "theme_duo" to IntArray(n) { k ->
                    val t = k / (n - 1.0)
                    // The hue turns the short way round from primary to tertiary.
                    var dh = hq - hp; if (dh > 180) dh -= 360; if (dh < -180) dh += 360
                    lch(0.35 + 0.55 * t, 0.13 - 0.04 * t, hp + dh * t)
                },
                "theme_diverging" to IntArray(n) { k ->
                    val t = k / (n - 1.0)
                    if (t < 0.5) deep(tonalP, 0.9 * (1 - 2 * t)) else deep(tonalQ, 0.9 * (2 * t - 1))
                },
                "theme_loop" to IntArray(n) { k ->
                    val t = k / n.toDouble()
                    val d = kotlin.math.sin(Math.PI * ((2 * t) % 1.0)) * 0.85
                    if (t < 0.5) deep(tonalP, 0.08 + d) else deep(tonalQ, 0.08 + d)
                },
            )
            themeCache = Triple(p, q, maps)
            return maps[name]
        }

        /** An ARGB color's OKLCH hue, in degrees. */
        private fun hueOf(argb: Int): Double {
            fun lin(c: Int): Double { val v = c / 255.0; return if (v <= 0.04045) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4) }
            val r = lin((argb shr 16) and 0xFF); val g = lin((argb shr 8) and 0xFF); val b = lin(argb and 0xFF)
            val l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
            val m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
            val s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
            val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
            val bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
            return (Math.toDegrees(kotlin.math.atan2(bb, a)) + 360) % 360
        }

        /** An OKLCH color in sRGB (RGB, no alpha), its chroma lowered until it fits. */
        private fun lch(l: Double, c0: Double, hue: Double): Int {
            var c = c0
            val hr = Math.toRadians(hue)
            var rgb: DoubleArray
            while (true) {
                rgb = oklab(l, c * kotlin.math.cos(hr), c * kotlin.math.sin(hr))
                if (rgb.all { it in -0.0005..1.0005 } || c <= 0.0) break
                c -= 0.004
            }
            fun enc(v: Double): Int {
                val x = v.coerceIn(0.0, 1.0)
                val e = if (x <= 0.0031308) 12.92 * x else 1.055 * Math.pow(x, 1 / 2.4) - 0.055
                return (e * 255).roundToInt().coerceIn(0, 255)
            }
            return (enc(rgb[0]) shl 16) or (enc(rgb[1]) shl 8) or enc(rgb[2])
        }

        /**
         * Single-hue sequential maps for the hues matplotlib's set has none of (a clean yellow,
         * teal, magenta, pink), so every theme color has a map of its own: name to OKLCH hue.
         */
        private val EXTRA_HUES = listOf("Yellows" to 100.0, "Teals" to 195.0, "Magentas" to 325.0, "Pinks" to 355.0)

        /**
         * A sequential ramp at one OKLCH hue, as matplotlib's Blues or Greens run: from near white
         * to deep, lightness falling evenly, chroma rising to the middle and easing off at the
         * dark end, each color brought into sRGB by lowering its chroma.
         */
        internal fun ramp(hue: Double, n: Int = 256): IntArray = IntArray(n) { k ->
            val t = k / (n - 1.0)
            val l = 0.97 - 0.67 * t
            var c = 0.17 * (if (t < 0.6) t / 0.6 else 1 - 0.35 * (t - 0.6) / 0.4)
            val hr = Math.toRadians(hue)
            var rgb: DoubleArray
            while (true) {
                rgb = oklab(l, c * kotlin.math.cos(hr), c * kotlin.math.sin(hr))
                if (rgb.all { it in -0.0005..1.0005 } || c <= 0.0) break
                c -= 0.004
            }
            fun enc(v: Double): Int {
                val x = v.coerceIn(0.0, 1.0)
                val e = if (x <= 0.0031308) 12.92 * x else 1.055 * Math.pow(x, 1 / 2.4) - 0.055
                return (e * 255).roundToInt().coerceIn(0, 255)
            }
            (enc(rgb[0]) shl 16) or (enc(rgb[1]) shl 8) or enc(rgb[2])
        }

        /** Linear sRGB from OKLab. */
        private fun oklab(l: Double, a: Double, b: Double): DoubleArray {
            val l_ = l + 0.3963377774 * a + 0.2158037573 * b
            val m_ = l - 0.1055613458 * a - 0.0638541728 * b
            val s_ = l - 0.0894841775 * a - 1.2914855480 * b
            val l3 = l_ * l_ * l_; val m3 = m_ * m_ * m_; val s3 = s_ * s_ * s_
            return doubleArrayOf(
                4.0767416621 * l3 - 3.3077115913 * m3 + 0.2309699292 * s3,
                -1.2684380046 * l3 + 2.6097574011 * m3 - 0.3413193965 * s3,
                -0.0041960863 * l3 - 0.7034186147 * m3 + 1.7076147010 * s3,
            )
        }

        /** Every map: the classic wheel, then matplotlib's in the order of its page. */
        val ALL: List<Colormap> by lazy {
            listOf(CLASSIC, THEME) + THEME_MAPS.map { (n, _, cyc) -> Colormap(n, "Theme", cyclic = cyc, qualitative = false, samples = null) } + COLORMAP_DATA.map { d ->
                Colormap(d.name, d.category, d.cyclic, d.qualitative, IntArray(d.hex.length / 6) { k -> d.hex.substring(6 * k, 6 * k + 6).toInt(16) })
            } + EXTRA_HUES.map { (n, h) -> Colormap(n, "Sequential (more hues)", cyclic = false, qualitative = false, samples = ramp(h)) }
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
            "theme_duo" to "Duo", "theme_diverging" to "Split", "theme_loop" to "Loop",
        )

        /** The maps offered first until the list is changed. */
        val DEFAULT_FAVORITES = listOf("theme", "theme_duo", "theme_diverging", "theme_loop", "classic", "twilight", "twilight_shifted", "viridis", "plasma", "magma", "cividis", "turbo")

        /**
         * A map by its saved name; older saves used upper-case names (VIRIDIS, TWILIGHT_SHIFTED),
         * so case is ignored when nothing matches exactly. Unknown names give [CLASSIC].
         */
        fun byName(name: String?): Colormap {
            if (name == null) return CLASSIC
            // "Tonal" was its own map for a while; it's the theme map now.
            if (name == TONAL) return THEME
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
