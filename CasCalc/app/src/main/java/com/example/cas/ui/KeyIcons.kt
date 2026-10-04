package com.example.cas.ui

import androidx.compose.ui.graphics.vector.ImageVector

/*
 * Beta icons for the calculator's function keys (Settings › Calculator › New calculator icons):
 * each key's own math symbol (π, e, ∫, Σ, x̄, σ, Φ, ℜ…) drawn with the app's round strokes,
 * two-tone like the data table's icons ([TableIcons]): the operator or constant in the accent
 * color, the boxes and letters it works on in ink. Drawn on a 24×24
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
    // Small pieces the symbols share.
    private fun box(x: Float, y: Float, w: Float) = "M$x ${y}h${w}v${w}h-${w}z"
    private val sigma = "M17.5 4.5h-11l6 7.5l-6 7.5h11"
    private val nabla = "M3 5h16L11 20z"
    private val bell = "M2.5 19c4 0 5.5-12 9.5-12s5.5 12 9.5 12"
    private val y = listOf("M3.5 9l3.5 8.5", "M11 9l-6 12.5")
    private val comma = key("Comma", inkThin = listOf(box(2.5f, 6.5f, 6f), box(15.5f, 6.5f, 6f)),
        accentFill = listOf("M10.3 15.6a2 2 0 1 1 3.6 1.2c-.4 1.7-1.6 3.1-3.3 3.8l-.6-1c1-.6 1.6-1.4 1.8-2.2a2 2 0 0 1-1.5-1.8z"))
    private val imaginary = key("I", accent = listOf("M12.5 10l-1.8 7.2c-.3 1.4.5 2.2 1.6 1.6l1.2-.7"), accentFill = listOf(ring(13.4f, 5.6f, 1.6f)))

    /**
     * Icons by key, as the keys are spoken (KeySpec.spoken). Each is the operation's own symbol,
     * stroked like the rest of the app's icons: the operator or constant in the accent color, the
     * boxes and letters it works on in ink.
     */
    private val byKey: Map<String, ImageVector> by lazy {
        mapOf(
            // Basic.
            "pi" to key("Pi", accent = listOf("M4 8.5c.8-1.8 2.2-2.8 4-2.8h12", "M9.5 5.8c0 6-1 10.5-3 13.7", "M15.5 5.8v10.7c0 1.9.9 3 2.4 3c1 0 1.8-.5 2.4-1.4")),
            "e" to key("E", accent = listOf("M6.5 13h11c0-4.3-2.5-7-5.7-7S5.8 9 5.8 13.2c0 4 2.6 6.8 6.1 6.8c2.3 0 4.1-1 5.3-2.8")),
            "power" to key("Power", ink = listOf("M4 10h9v10H4z"), accent = listOf("M15.5 3.5h5v5h-5z")),
            "nth root" to key("Root", ink = listOf("M2.5 14l2.5-1.5L8.5 20L13 4h8.5", box(14f, 10.5f, 5.5f)), accent = listOf("M3 3.5h4.5V8H3z")),
            "factorial" to key("Factorial", ink = listOf("M4 19.5V10", "M4 12.5c1.4-1.9 2.9-2.7 4.4-2.7c1.9 0 3.1 1.3 3.1 3.6v6.1"), accent = listOf("M17.5 4.5v10"), accentFill = listOf(ring(17.5f, 19f, 1.6f))),
            "natural log" to key("Ln", accent = listOf("M4.5 4v13.5c0 1.3.6 2 1.7 2", "M9.5 19.5V10", "M9.5 12.4c1.3-1.7 2.7-2.6 4.2-2.6c1.9 0 3.1 1.3 3.1 3.6v6.1"), inkThin = listOf(box(18.5f, 15.5f, 4f))),
            "log base a" to key("Log", ink = listOf("M3 4.5v12.5", ring(7.8f, 14f, 2.4f, 3f), ring(13.5f, 14f, 2.4f, 3f), "M15.9 11v7.5c0 2-1.2 3-2.7 3c-1 0-1.8-.4-2.3-1.1"), accentThin = listOf(box(18.5f, 16.5f, 4f))),
            "absolute value" to key("Abs", ink = listOf("M3 4v16", "M21 4v16"), accent = listOf("M7 7l5 9.5L17 7")),
            "mod" to key("Mod", accent = listOf("M2.5 18.5v-7", "M2.5 13c.7-1.2 1.5-1.8 2.4-1.8s1.9.8 1.9 2.2v5.1", "M6.8 13.4c.7-1.4 1.5-2.2 2.4-2.2s1.9.8 1.9 2.2v5.1", ring(15.2f, 15f, 2.2f, 3.4f), ring(20f, 15f, 1.9f, 3.4f), "M21.9 6v12.5")),
            "i, the imaginary unit" to imaginary,
            "equals sign" to key("Equals", ink = listOf("M5 9h14"), accent = listOf("M5 15h14")),
            "less than" to key("Less", accent = listOf("M17 5L7 12l10 7")),
            "greater than" to key("Greater", accent = listOf("M7 5l10 7L7 19")),
            "comma" to comma,
            "previous answer" to key("Ans", accent = listOf("M2.5 19.5L6.5 6l4 13.5", "M3.9 14.8h5.2"), ink = listOf("M12.5 19.5v-6.5", "M12.5 14.8c.9-1.2 1.9-1.8 2.9-1.8c1.4 0 2.2.9 2.2 2.4v4.1", "M23 13.8c-.4-.6-1-.9-1.7-.9c-.9 0-1.6.5-1.6 1.3c0 1.8 3.5 1.1 3.5 3c0 .9-.8 1.6-1.9 1.6c-.8 0-1.5-.3-1.9-1")),
            // Calculus.
            "partial derivative" to key("Partial", accent = listOf("M15.5 6.5c-1-2-2.8-2.9-4.6-2.4s-2.7 2.4-1.3 3.3c1.8 1.2 5.6 1.5 5.9 5.6c.4 3.8-2.2 6.5-5 6.5s-4.5-2-4-4.5s2.6-4 5.2-4s4 1 4 1")),
            "integral" to key("Integral", accent = listOf("M14 4.2c-.7-.8-1.7-.9-2.5-.3c-1 .8-1 2.5-1.1 4.5l-.6 9c-.1 2-.2 3.5-1.2 4.3c-.8.6-1.8.5-2.5-.3"), inkThin = listOf(box(15.5f, 10f, 5f))),
            "limit" to key("Limit", ink = listOf("M3.5 4v10", "M7 8.5V14", "M10.5 14V8.5", "M10.5 10c.6-1 1.3-1.5 2-1.5c1 0 1.6.7 1.6 2V14", "M14.1 10.3c.6-1.2 1.3-1.8 2-1.8c1 0 1.7.7 1.7 2V14"), inkFill = listOf(ring(7f, 5.3f, 1.2f)),
                accent = listOf("M4 19h13.5", "M15 16.5l2.5 2.5l-2.5 2.5")),
            "sum" to key("Sum", accent = listOf(sigma)),
            "product" to key("Product", accent = listOf("M5 5h14", "M8 5v14", "M16 5v14"), inkThin = listOf("M5.5 19h5", "M13.5 19h5")),
            "Taylor series" to key("Taylor", ink = listOf("M7 4.5c-1.4-.5-2.6.3-2.6 2.2V18.5", "M2.5 9.5h4.2", "M22 5h-5l3 4.5l-3 4.5h5"), accent = listOf("M8.5 10c.9-.9 1.8-.9 2.7 0s1.8.9 2.7 0", "M8.5 14c.9-.9 1.8-.9 2.7 0s1.8.9 2.7 0")),
            "y prime" to key("Prime", ink = y, accent = listOf("M15.5 4l-1.5 5")),
            "y double prime" to key("Second", ink = y, accent = listOf("M14.5 4l-1.5 5", "M18 4l-1.5 5")),
            "solve differential equation" to key("Dsolve", ink = listOf("M2.5 9l3 7", "M8.5 9l-5 10.5", "M13 11h4", "M13 14.5h4"), accent = listOf("M11 4l-1.2 4", box(18.5f, 10f, 4f))),
            "infinity" to key("Infinity", accent = listOf("M12 12c-2-3-4-4.5-6-4.5a4.5 4.5 0 0 0 0 9c2 0 4-1.5 6-4.5s4-4.5 6-4.5a4.5 4.5 0 0 1 0 9c-2 0-4-1.5-6-4.5z")),
            "gradient" to key("Gradient", accent = listOf("M3.5 5h17L12 20.5z"), inkThin = listOf("M7.2 6.8l5.6 10.2")),
            "divergence" to key("Divergence", ink = listOf(nabla), accentFill = listOf(ring(20.5f, 12.5f, 1.8f))),
            "curl" to key("Curl", ink = listOf("M2.5 5h13L9 18z"), accent = listOf("M16.5 10l5 5", "M21.5 10l-5 5")),
            "Jacobian matrix" to key("Jacobian", accent = listOf("M6 4H3.5v16H6", "M18 4h2.5v16H18"), ink = listOf("M14.5 6.5v8c0 2-1.2 3-2.8 3c-1.2 0-2.1-.6-2.6-1.6")),
            "Hessian matrix" to key("Hessian", accent = listOf("M6 4H3.5v16H6", "M18 4h2.5v16H18"), ink = listOf("M8.5 6.5v11", "M15.5 6.5v11", "M8.5 12h7")),
            // Statistics.
            "mean" to key("Mean", ink = listOf("M7 10l9.5 10", "M16.5 10L7 20"), accent = listOf("M6 5.5h11.5")),
            "median" to key("Median", ink = listOf("M7 10l9.5 10", "M16.5 10L7 20"), accent = listOf("M5.5 6.5c1.5-2 3.6-2 5.6-.5s4.1 1.5 5.6-.5")),
            "sample standard deviation" to key("Sd", accent = listOf("M16 8.5c-1-1.6-2.5-2.3-4.2-2.3c-2.2 0-3.8 1.3-3.8 3c0 4.3 8.5 2.4 8.5 6.6c0 1.9-1.8 3.4-4.4 3.4c-1.9 0-3.5-.8-4.4-2.4")),
            "population standard deviation" to key("Psd", accent = listOf(ring(10f, 14.5f, 5.5f), "M10 9H21")),
            "sample variance" to key("Variance", ink = listOf("M12.5 10.5c-.8-1.3-2-1.9-3.4-1.9c-1.8 0-3.1 1-3.1 2.4c0 3.5 6.9 1.9 6.9 5.3c0 1.6-1.5 2.8-3.6 2.8c-1.6 0-2.9-.7-3.6-2"), accent = listOf("M15.5 5c.4-1 1.2-1.6 2.3-1.6c1.3 0 2.2.9 2.2 2c0 2.2-4.4 3.2-4.4 6h4.8")),
            "n choose k" to key("Choose", accent = listOf("M7 3.5c-2.5 2-3.5 5-3.5 8.5s1 6.5 3.5 8.5", "M17 3.5c2.5 2 3.5 5 3.5 8.5s-1 6.5-3.5 8.5"),
                ink = listOf("M9.5 10.5V5.5", "M9.5 7c.8-1 1.6-1.5 2.5-1.5c1.2 0 2 .8 2 2.2v2.8", "M10 13.5v7", "M14 15.5l-4 3", "M11.6 17.3L14 20.5")),
            "permutations" to key("Permutations", accent = listOf("M8 20V4h5a4 4 0 0 1 0 8H8"), ink = listOf("M2.5 20v-4", "M2.5 17c.6-.8 1.2-1.2 1.9-1.2c.9 0 1.5.6 1.5 1.7V20", "M17 14.5v6", "M20.5 16l-3.5 2.4", "M18.5 17.6l2 2.9")),
            "normal density" to key("NormalPdf", accent = listOf(ring(12f, 12f, 5.5f, 4.5f), "M12 3.5v17")),
            "normal distribution function" to key("NormalCdf", accent = listOf(ring(12f, 12f, 6.5f, 4.5f), "M12 4v16"), ink = listOf("M9.5 4h5", "M9.5 20h5")),
            "inverse normal" to key("InvNorm", accent = listOf(ring(9f, 13f, 5.5f, 4f), "M9 6v14"), ink = listOf("M7 6h4", "M7 20h4", "M16.5 6h3", "M21.5 3.5v6")),
            "binomial probability" to key("Binom", accent = listOf("M5 4.5v15h6.5a4 4 0 0 0 0-8H5", "M5 11.5h5.5a3.5 3.5 0 0 0 0-7H5"), inkThin = listOf(box(17.5f, 15.5f, 4f))),
            "cumulative binomial probability" to key("BinomCdf", accent = listOf("M4 4.5v15h6.5a4 4 0 0 0 0-8H4", "M4 11.5h5.5a3.5 3.5 0 0 0 0-7H4"), ink = listOf("M21.5 12l-4.5 2.5l4.5 2.5", "M17 20h4.5")),
            "Poisson probability" to key("Poisson", accent = listOf("M5 20l6.5-10.5", "M6.5 4c1.6 0 2.7.8 3.4 2.6L17.5 20")),
            "sum of a list" to key("Total", accent = listOf("M13 4.5H4l5 7.5l-5 7.5h9"), ink = listOf("M15 11l6 8.5", "M21 11l-6 8.5")),
            "comma for lists" to comma,
            // Complex numbers.
            "real part" to key("Re", accent = listOf("M3.5 20V4.5h5a3.8 3.8 0 0 1 0 7.6H3.5", "M8 12.1L12 20"), ink = listOf("M14.5 15.5h6.3c0-2.4-1.4-3.8-3.1-3.8s-3.2 1.6-3.2 4s1.4 4.1 3.3 4.1c1.2 0 2.2-.5 2.8-1.5")),
            "imaginary part" to key("Im", accent = listOf("M5 4.5v15", "M2.5 4.5h5", "M2.5 19.5h5"), ink = listOf("M10 19.5v-7", "M10 14c.8-1.2 1.6-1.8 2.6-1.8s2 .8 2 2.2v5.1", "M14.6 14.4c.8-1.4 1.6-2.2 2.7-2.2c1 0 2 .8 2 2.2v5.1")),
            "conjugate" to key("Conj", ink = listOf("M3.5 10h9l-9 10h9"), accent = listOf("M18.5 3.5v6", "M15.9 5l5.2 3", "M15.9 8l5.2-3")),
            "argument" to key("Arg", accent = listOf("M14 4.5L3 19.5h11"), ink = listOf("M15.5 12.5h6l-6 7h6")),
            "e to the i theta" to key("Euler", ink = listOf("M3.5 15.5h7.8c0-3-1.8-5-4-5s-4 2.2-4 5s1.8 5 4.2 5c1.6 0 2.9-.7 3.6-2"), accent = listOf("M14.5 6.5v5", ring(19f, 7.5f, 2.2f, 3.5f), "M16.8 7.5h4.4"), accentFill = listOf(ring(14.5f, 3.6f, 1.1f))),
            "gamma function" to key("Gamma", accent = listOf("M5 20V4.5h10v2.5"), inkThin = listOf(box(15.5f, 12f, 5f))),
            "Riemann zeta function" to key("Zeta", accent = listOf("M8 4h8c-4 3.5-8 7.5-8 11c0 2.2 1.6 3 4 3c1.8 0 3 .6 3 2c0 .8-.5 1.5-1.3 1.8")),
            "Lambert W function" to key("LambertW", accent = listOf("M2.5 4.5l4 15l5.5-12l5.5 12l4-15")),
            "Bessel function of the first kind" to key("BesselJ", accent = listOf("M13 4.5v10.5c0 2.5-1.5 4-3.6 4c-1.5 0-2.6-.8-3.2-2"), inkThin = listOf(box(16f, 15f, 4.5f))),
            "Bessel function of the second kind" to key("BesselY", accent = listOf("M3.5 4.5l5.5 7.5v7.5", "M14.5 4.5l-5.5 7.5"), inkThin = listOf(box(16f, 15f, 4.5f))),
            "contour integral" to key("Contour", accent = listOf("M14.5 3.7c-.7-.8-1.7-.9-2.5-.3c-1 .8-1 2.5-1.1 4.5l-.6 9c-.1 2-.2 3.5-1.2 4.3c-.8.6-1.8.5-2.5-.3"), ink = listOf(ring(11.7f, 12.5f, 4f))),
            "residue" to key("Residue", accent = listOf("M2.5 19V5h4.2a3.5 3.5 0 0 1 0 7H2.5", "M6 12l3.5 7"), ink = listOf("M11 15h5c0-2-1.2-3.2-2.6-3.2s-2.6 1.4-2.6 3.4s1.2 3.6 2.8 3.6c1 0 1.8-.4 2.3-1.2", "M22 12.6c-.5-.7-1.2-1-2-1c-1.1 0-1.9.6-1.9 1.5c0 2 4.2 1.2 4.2 3.4c0 1-.9 1.8-2.2 1.8c-1 0-1.8-.4-2.3-1.1")),
            "z" to key("Z", accent = listOf("M6.5 8.5h10.5l-10.5 11h11")),
            "w" to key("W", accent = listOf("M3 9l3.2 10.5l5.8-9l5.8 9L21 9")),
        )
    }

    /** The icon for a key, if it has one. */
    fun forKey(spoken: String): ImageVector? = byKey[spoken]

    /** Every key icon, by key, for the settings preview. */
    val all: Map<String, ImageVector> get() = byKey
}
