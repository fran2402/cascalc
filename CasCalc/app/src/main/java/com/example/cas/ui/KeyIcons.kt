package com.example.cas.ui

import androidx.compose.ui.graphics.vector.ImageVector

/*
 * Beta icons for the calculator's function keys (Settings › Calculator › New calculator icons):
 * each key's operation drawn as a small picture, two-tone like the data table's icons ([TableIcons]):
 * what it works on in the ink color, what it makes or does in the accent color. Drawn on a 24×24
 * grid with 2 dp round strokes (thin ones 1.6 dp). The linear algebra and trigonometry keys, and
 * the letters and constants, keep their labels.
 */

/** An ellipse as a path, centered at ([x], [y]). */
private fun ring(x: Float, y: Float, r: Float, ry: Float = r) = "M${x - r} ${y}a$r $ry 0 1 0 ${2 * r} 0a$r $ry 0 1 0 ${-2 * r} 0z"

private fun key(name: String, ink: List<String> = emptyList(), accent: List<String> = emptyList(), inkThin: List<String> = emptyList(), accentThin: List<String> = emptyList(),
                inkFill: List<String> = emptyList(), accentFill: List<String> = emptyList(), accentShade: List<String> = emptyList()): ImageVector =
    tableIcon(
        "Key$name", stroke = ink + accent, thin = inkThin + accentThin, fill = inkFill + accentFill, shade = accentShade,
        accent = (accent + accentThin + accentFill + accentShade).toSet(),
    )

object KeyIcons {
    private val axes = listOf("M3 20.5h18", "M3.5 3v18")
    private val comma = key("Comma", inkFill = listOf(ring(4.5f, 17f, 1.4f), ring(19.5f, 17f, 1.4f)),
        accentFill = listOf("M10.3 15.6a2 2 0 1 1 3.6 1.2c-.4 1.7-1.6 3.1-3.3 3.8l-.6-1c1-.6 1.6-1.4 1.8-2.2a2 2 0 0 1-1.5-1.8z"))
    private val imaginary = key("I", inkThin = listOf(ring(12f, 12f, 7.5f), "M2.5 12h19", "M12 2.5v19"), accent = listOf("M12 12V6.5"), accentFill = listOf(ring(12f, 4.5f, 2f)))

    /** Icons by key, as the keys are spoken (KeySpec.spoken). */
    private val byKey: Map<String, ImageVector> by lazy {
        mapOf(
            // Basic.
            "pi" to key("Pi", ink = listOf("M3.5 12h17"), inkThin = listOf(ring(12f, 12f, 8.5f)), accent = listOf("M3.5 12a8.5 8.5 0 0 1 17 0")),
            "e" to key("E", inkThin = axes, accent = listOf("M3.5 18.5c6 0 9.5-.8 12.5-4.5S19.5 7 20.5 3.5"), inkFill = listOf(ring(3.5f, 18.5f, 1.6f))),
            "power" to key("Power", ink = listOf("M4 10h9v10H4z"), accent = listOf("M15.5 3.5h5v5h-5z")),
            "nth root" to key("Root", ink = listOf("M2.5 14l2.5-1.5L8.5 20L13 4h8.5"), accent = listOf("M3 3.5h4.5V8H3z")),
            "factorial" to key("Factorial", inkThin = listOf("M3 20.5h18"), accent = listOf("M5 20V5", "M9.7 20V9", "M14.3 20v-7.5", "M19 20v-4")),
            "natural log" to key("Ln", inkThin = axes, accent = listOf("M5.5 21C6.5 13 9 9.5 20.5 7.5"), inkFill = listOf(ring(9.6f, 11.6f, 1.5f))),
            "log base a" to key("Log", inkThin = axes, accent = listOf("M5.5 21C6.5 13 9 9.5 20.5 7.5"), ink = listOf("M15 14.5h5v5h-5z")),
            "absolute value" to key("Abs", ink = listOf("M3 4v16", "M21 4v16"), accent = listOf("M7 7l5 9.5L17 7")),
            "mod" to key("Mod", inkThin = listOf(ring(12f, 12f, 8.5f)), accent = listOf("M12 5.5a6.5 6.5 0 1 1-5.6 3.2", "M4.5 5.3l1.9 3.4l3.4-1.9"), inkFill = listOf(ring(12f, 12f, 1.5f))),
            "i, the imaginary unit" to imaginary,
            "equals sign" to key("Equals", ink = listOf("M5 9h14"), accent = listOf("M5 15h14")),
            "less than" to key("Less", accent = listOf("M17 5L7 12l10 7")),
            "greater than" to key("Greater", accent = listOf("M7 5l10 7L7 19")),
            "comma" to comma,
            "previous answer" to key("Ans", ink = listOf("M4.5 4.5h9", "M4.5 8h6"), accent = listOf("M19.5 6v5a3 3 0 0 1-3 3H6", "M9.5 10.5L6 14l3.5 3.5")),
            // Calculus.
            "partial derivative" to key("Partial", accent = listOf("M15.5 6.5c-1-2-2.8-2.9-4.6-2.4s-2.7 2.4-1.3 3.3c1.8 1.2 5.6 1.5 5.9 5.6c.4 3.8-2.2 6.5-5 6.5s-4.5-2-4-4.5s2.6-4 5.2-4s4 1 4 1"), inkThin = listOf("M17.5 15.5h4v4h-4z")),
            "integral" to key("Integral", ink = listOf("M3 20h18", "M3 17C6.5 17 8.5 6 12 6s5 7.5 9 6"), accentShade = listOf("M7.5 20v-6.3C9 9.5 10.3 6 12 6s3.2 3.6 5 5.6V20z")),
            "limit" to key("Limit", inkThin = listOf("M17.5 3v2.5", "M17.5 8v2.5", "M17.5 13v2.5", "M17.5 18v2.5"), accent = listOf("M3 19c6 0 10-3 12.5-13", "M13.2 7.3l2.3-1.6l.7 2.7")),
            "sum" to key("Sum", inkThin = listOf("M3.5 20.5h17"), accent = listOf("M6 20v-4", "M10 20v-7", "M14 20V10", "M18 20V5")),
            "product" to key("Product", accent = listOf("M5 5h14", "M8 5v14", "M16 5v14"), inkThin = listOf("M5.5 19h5", "M13.5 19h5")),
            "Taylor series" to key("Taylor", inkThin = listOf("M2.5 15.5C6 4 9 4 12 12s6 8 9.5-3.5"), accent = listOf("M5 21C8 9 10 9 12 12s4 3 7-9"), inkFill = listOf(ring(12f, 12f, 1.6f))),
            "y prime" to key("Prime", accent = listOf("M5 20.9L20 9.3"), inkThin = listOf("M3 19Q12 19 21 5"), inkFill = listOf(ring(12f, 15.5f, 1.8f))),
            "y double prime" to key("Second", inkThin = listOf("M3 5Q12 29 21 5"), accent = listOf("M8.5 9.5c1.5 3 5.5 3 7 0", "M13.6 9l1.9.5l.3-1.9")),
            "solve differential equation" to key("Dsolve", inkThin = listOf("M4 7l2.5-1", "M10.5 6.5l2.5-1.5", "M17 5.5l3-1.5", "M4 13l2.5-.5", "M10.5 12.3l2.5-1", "M17 11l3-1.5", "M4 19.5h2.5", "M10.5 19l2.5-.5", "M17 18l3-1"), accent = listOf("M3 17c5 0 8-9 18-11")),
            "infinity" to key("Infinity", accent = listOf("M12 12c-2-3-4-4.5-6-4.5a4.5 4.5 0 0 0 0 9c2 0 4-1.5 6-4.5s4-4.5 6-4.5a4.5 4.5 0 0 1 0 9c-2 0-4-1.5-6-4.5z")),
            "gradient" to key("Gradient", inkThin = listOf(ring(12f, 14f, 8.5f, 5.5f), ring(12f, 14f, 4f, 2.5f)), accent = listOf("M12 14V4.5", "M9 7.5l3-3l3 3")),
            "divergence" to key("Divergence", inkFill = listOf(ring(12f, 12f, 2f)), accent = listOf("M12 8.5V3", "M9.8 5.2L12 3l2.2 2.2", "M12 15.5V21", "M9.8 18.8L12 21l2.2-2.2", "M8.5 12H3", "M5.2 9.8L3 12l2.2 2.2", "M15.5 12H21", "M18.8 9.8L21 12l-2.2 2.2")),
            "curl" to key("Curl", inkFill = listOf(ring(12f, 12f, 2f)), accent = listOf("M18.5 12a6.5 6.5 0 1 1-1.9-4.6", "M17 3.5l-.4 3.9l3.9.4")),
            "Jacobian matrix" to key("Jacobian", inkThin = listOf("M4 4h16v16H4z"), accent = listOf("M4 12c5-3 11 3 16 0", "M12 4c-3 5 3 11 0 16")),
            "Hessian matrix" to key("Hessian", inkThin = listOf(ring(12f, 7f, 8f, 3f)), accent = listOf("M4 7c0 10 16 10 16 0"), accentFill = listOf(ring(12f, 14.5f, 1.6f))),
            // Statistics.
            "mean" to key("Mean", inkThin = listOf("M3 20.5h18"), ink = listOf("M5.5 20v-6", "M10 20V8", "M14.5 20v-9", "M19 20v-4.5"), accent = listOf("M3 12h18")),
            "median" to key("Median", inkFill = listOf(ring(4f, 18f, 1.6f), ring(8f, 15f, 1.6f), ring(16f, 9f, 1.6f), ring(20f, 6f, 1.6f)), accentFill = listOf(ring(12f, 12f, 2.6f))),
            "sample standard deviation" to key("Sd", ink = listOf("M2.5 19c4 0 5.5-12 9.5-12s5.5 12 9.5 12"), accent = listOf("M7.5 15h9", "M9.5 13l-2 2l2 2", "M14.5 13l2 2l-2 2")),
            "population standard deviation" to key("Psd", ink = listOf("M2.5 19c4 0 5.5-12 9.5-12s5.5 12 9.5 12"), accent = listOf("M8 20.5v-8.5", "M16 20.5v-8.5")),
            "sample variance" to key("Variance", ink = listOf("M2.5 19c4 0 5.5-12 9.5-12s5.5 12 9.5 12"), accentShade = listOf("M8 19v-7.3c1.2-2.6 2.6-4.7 4-4.7s2.8 2.1 4 4.7V19z")),
            "n choose k" to key("Choose", inkFill = listOf(ring(5f, 12f, 1.8f), ring(10f, 12f, 1.8f), ring(15f, 12f, 1.8f), ring(20f, 12f, 1.8f)), accentThin = listOf("M5 8h5a4 4 0 0 1 0 8H5a4 4 0 0 1 0-8z")),
            "permutations" to key("Permutations", inkFill = listOf(ring(6f, 4.5f, 1.8f), ring(12f, 4.5f, 1.8f), ring(18f, 4.5f, 1.8f), ring(6f, 19.5f, 1.8f), ring(12f, 19.5f, 1.8f), ring(18f, 19.5f, 1.8f)), accent = listOf("M6 7.5l12 9", "M12 7.5l-6 9", "M18 7.5l-6 9")),
            "normal density" to key("NormalPdf", inkThin = listOf("M2.5 20.5h19"), accent = listOf("M2.5 19c4 0 5.5-12 9.5-12s5.5 12 9.5 12")),
            "normal distribution function" to key("NormalCdf", inkThin = listOf("M3 20.5h18", "M3 4h18"), accent = listOf("M3 19.5c7 0 7-15 18-15")),
            "inverse normal" to key("InvNorm", ink = listOf("M2.5 19c4 0 5.5-12 9.5-12s5.5 12 9.5 12"), accentShade = listOf("M2.5 19c2.2 0 3.4-2.4 4.6-5.2V19z"), accent = listOf("M7.1 20.5v-7.2")),
            "binomial probability" to key("Binom", inkThin = listOf("M3 20.5h18"), accent = listOf("M5 20v-3", "M8.5 20v-8", "M12 20V7", "M15.5 20v-8", "M19 20v-3")),
            "cumulative binomial probability" to key("BinomCdf", inkThin = axes, accent = listOf("M4 19h4v-4h4V9h4V6h4.5")),
            "Poisson probability" to key("Poisson", inkThin = listOf("M3 20.5h18"), accent = listOf("M4.5 20v-9", "M8.5 20V6", "M12.5 20V10", "M16.5 20v-6", "M20.5 20v-3")),
            "sum of a list" to key("Total", ink = listOf("M8 4.5h10", "M8 8.5h10", "M8 12.5h10"), accent = listOf("M4.5 16h15", "M8 20h10")),
            "comma for lists" to comma,
            // Complex numbers.
            "real part" to key("Re", inkThin = axes + listOf("M16 9v11"), inkFill = listOf(ring(16f, 7f, 2f)), accent = listOf("M3.5 20.5H16")),
            "imaginary part" to key("Im", inkThin = axes + listOf("M3.5 7H14"), inkFill = listOf(ring(16f, 7f, 2f)), accent = listOf("M3.5 20.5V7")),
            "conjugate" to key("Conj", inkThin = listOf("M3 12h18", "M15 8v8"), inkFill = listOf(ring(15f, 6f, 2f)), accentFill = listOf(ring(15f, 18f, 2f))),
            "argument" to key("Arg", inkThin = axes, ink = listOf("M3.5 20.5L16 8"), inkFill = listOf(ring(17f, 7f, 2f)), accent = listOf("M10 20.5a6.5 6.5 0 0 0-1.9-4.6")),
            "e to the i theta" to key("Euler", inkThin = listOf(ring(12f, 12f, 8f), "M12 12h8"), accent = listOf("M12 12l5.7-5.7", "M20 12a8 8 0 0 0-2.3-5.7"), accentFill = listOf(ring(17.7f, 6.3f, 1.8f))),
            "gamma function" to key("Gamma", inkThin = axes, accent = listOf("M5 3.5c.8 7 2 11.5 5 11.5s4.5-.8 6.5 1.5S19.5 21 20.5 21")),
            "Riemann zeta function" to key("Zeta", inkThin = listOf("M5.5 3v18"), accent = listOf("M12 12a1.5 1.5 0 1 1 1.5 1.5a3.5 3.5 0 1 1-3.5-3.5a5.5 5.5 0 1 1 5.5 5.5")),
            "Lambert W function" to key("LambertW", inkThin = axes, accent = listOf("M5.5 20c0-4 1.2-6.5 3.5-7.6S15 9.5 21 5.5")),
            "Bessel function of the first kind" to key("BesselJ", inkThin = listOf("M3 13h18"), accent = listOf("M3 5c2 0 2.5 13 5 13s3-9.5 5-9.5s2.5 6 4 6s2-3 4-3")),
            "Bessel function of the second kind" to key("BesselY", inkThin = listOf("M3 13h18"), accent = listOf("M4.5 21c.5-8 2-12 4-12s3 7 5 7s2.5-5 4-5s2 2.5 3 2.5")),
            "contour integral" to key("Contour", inkFill = listOf(ring(12f, 12f, 1.6f)), accent = listOf("M19 12a7 7 0 1 1-2.05-4.95", "M17.6 3.3l-.6 3.8l3.8.5")),
            "residue" to key("Residue", ink = listOf("M10 10l4 4", "M14 10l-4 4"), accentThin = listOf(ring(12f, 12f, 7f)), accent = listOf("M19 8.5v3.5l3-1.5")),
            "z" to key("Z", inkThin = axes, accentFill = listOf(ring(15f, 9f, 2.2f)), accentThin = listOf("M3.5 20.5L13.5 10.5")),
            "w" to key("W", inkFill = listOf(ring(6.5f, 16.5f, 2f)), accent = listOf("M9 14.5l7-5.5", "M13 8.8l3 .2l-.6 2.9"), accentFill = listOf(ring(18f, 7.5f, 2.2f))),
        )
    }

    /** The icon for a key, if it has one. */
    fun forKey(spoken: String): ImageVector? = byKey[spoken]

    /** Every key icon, by key, for the settings preview. */
    val all: Map<String, ImageVector> get() = byKey
}
