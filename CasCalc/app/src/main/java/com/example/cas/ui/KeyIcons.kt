package com.example.cas.ui

import androidx.compose.ui.graphics.vector.ImageVector

/*
 * Beta icons for the calculator's function keys (Settings › Calculator › New calculator icons):
 * each key's own math symbol (π, e, ∫, Σ, x̄, σ, Φ, ℜ…) drawn with the app's round strokes,
 * two-tone like the data table's icons ([TableIcons]): the operator or constant in the accent
 * color, the boxes and letters it works on in ink. Drawn on a 24-unit-high grid (wider for the
 * longer words: asinh, det, rref) with 2 dp round strokes (thin ones 1.6 dp). The letters and
 * constants keep their labels.
 */

/** An ellipse as a path, centered at ([x], [y]). */
private fun ring(x: Float, y: Float, r: Float, ry: Float = r) = "M${x - r} ${y}a$r $ry 0 1 0 ${2 * r} 0a$r $ry 0 1 0 ${-2 * r} 0z"

private fun key(name: String, width: Float = 24f, ink: List<String> = emptyList(), accent: List<String> = emptyList(), inkThin: List<String> = emptyList(), accentThin: List<String> = emptyList(),
                inkFill: List<String> = emptyList(), accentFill: List<String> = emptyList(), accentShade: List<String> = emptyList()): ImageVector =
    tableIcon(
        "Key$name", stroke = ink + accent, thin = inkThin + accentThin, fill = inkFill + accentFill, shade = accentShade,
        accent = (accent + accentThin + accentFill + accentShade).toSet(), width = width,
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
            "log base a" to key("Log", ink = listOf("M3 4.5v12.5", ring(7.8f, 14f, 2.4f, 3f), ring(13.5f, 14f, 2.4f, 3f), "M15.9 11v7.5c0 2-1.2 3-2.7 3c-1 0-1.8-.4-2.3-1.1"), accentThin = listOf(box(18.5f, 16.5f, 4f))),
            "absolute value" to key("Abs", ink = listOf("M3 4v16", "M21 4v16"), accent = listOf("M7 7l5 9.5L17 7")),
            "i, the imaginary unit" to imaginary,
            "equals sign" to key("Equals", accent = listOf("M5 9h14", "M5 15h14")),
            "less than" to key("Less", accent = listOf("M17 5L7 12l10 7")),
            "greater than" to key("Greater", accent = listOf("M7 5l10 7L7 19")),
            "comma" to comma,
            "partial derivative" to key("Partial", accent = listOf("M15.5 6.5c-1-2-2.8-2.9-4.6-2.4s-2.7 2.4-1.3 3.3c1.8 1.2 5.6 1.5 5.9 5.6c.4 3.8-2.2 6.5-5 6.5s-4.5-2-4-4.5s2.6-4 5.2-4s4 1 4 1")),
            "integral" to key("Integral", accent = listOf("M14 4.2c-.7-.8-1.7-.9-2.5-.3c-1 .8-1 2.5-1.1 4.5l-.6 9c-.1 2-.2 3.5-1.2 4.3c-.8.6-1.8.5-2.5-.3"), inkThin = listOf(box(15.5f, 10f, 5f))),
            "limit" to key("Limit", ink = listOf("M3.5 4v10", "M7 8.5V14", "M10.5 14V8.5", "M10.5 10c.6-1 1.3-1.5 2-1.5c1 0 1.6.7 1.6 2V14", "M14.1 10.3c.6-1.2 1.3-1.8 2-1.8c1 0 1.7.7 1.7 2V14"), inkFill = listOf(ring(7f, 5.3f, 1.2f)),
                accent = listOf("M4 19h13.5", "M15 16.5l2.5 2.5l-2.5 2.5")),
            "sum" to key("Sum", accent = listOf(sigma)),
            "product" to key("Product", accent = listOf("M5 5h14", "M8 5v14", "M16 5v14"), inkThin = listOf("M5.5 19h5", "M13.5 19h5")),
            "Taylor series" to key("Taylor", accent = listOf("M12.5 4.5H3.5l4.7 7.5l-4.7 7.5h9"), ink = listOf("M14 11l5 8.5", "M19 11l-5 8.5"), accentThin = listOf("M19.6 9V5", "M19.6 6.5c.6-.9 1.2-1.4 2-1.4c.9 0 1.4.6 1.4 1.6V9")),
            "y prime" to key("Prime", ink = y, accent = listOf("M15.5 4l-1.5 5")),
            "y double prime" to key("Second", ink = y, accent = listOf("M14.5 4l-1.5 5", "M18 4l-1.5 5")),
            "solve differential equation" to key("Dsolve", ink = listOf("M1.5 10l3.2 8", "M8 10l-5.5 11.5", "M12 13h4", "M12 16.5h4", "M22.5 5.1c-1.6-.6-2.8.2-2.8 2.2v12.2", "M18.5 10h3.6"), accent = listOf("M10.2 4.5l-1.3 4")),
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
            "conjugate" to key("Conj", ink = listOf("M3.5 10h9l-9 10h9"), accent = listOf("M18.5 3.5v6", "M15.9 5l5.2 3", "M15.9 8l5.2-3")),
            "argument" to key("Arg", accent = listOf("M14 4.5L3 19.5h11"), ink = listOf("M15.5 12.5h6l-6 7h6")),
            "e to the i theta" to key("Euler", ink = listOf("M3.5 15.5h7.8c0-3-1.8-5-4-5s-4 2.2-4 5s1.8 5 4.2 5c1.6 0 2.9-.7 3.6-2"), accent = listOf("M14.5 6.5v5", ring(19f, 7.5f, 2.2f, 3.5f), "M16.8 7.5h4.4"), accentFill = listOf(ring(14.5f, 3.6f, 1.1f))),
            "gamma function" to key("Gamma", accent = listOf("M5 20V4.5h10v2.5"), inkThin = listOf(box(15.5f, 12f, 5f))),
            "Riemann zeta function" to key("Zeta", accent = listOf("M8 4h8c-4 3.5-8 7.5-8 11c0 2.2 1.6 3 4 3c1.8 0 3 .6 3 2c0 .8-.5 1.5-1.3 1.8")),
            "Lambert W function" to key("LambertW", accent = listOf("M2.5 4.5l4 15l5.5-12l5.5 12l4-15")),
            "Bessel function of the first kind" to key("BesselJ", accent = listOf("M13 4.5v10.5c0 2.5-1.5 4-3.6 4c-1.5 0-2.6-.8-3.2-2"), inkThin = listOf(box(16f, 15f, 4.5f))),
            "Bessel function of the second kind" to key("BesselY", accent = listOf("M3.5 4.5l5.5 7.5v7.5", "M14.5 4.5l-5.5 7.5"), inkThin = listOf(box(16f, 15f, 4.5f))),
            "contour integral" to key("Contour", accent = listOf("M14.5 3.7c-.7-.8-1.7-.9-2.5-.3c-1 .8-1 2.5-1.1 4.5l-.6 9c-.1 2-.2 3.5-1.2 4.3c-.8.6-1.8.5-2.5-.3"), ink = listOf(ring(11.7f, 12.5f, 4f))),
            "z" to key("Z", accent = listOf("M6.5 8.5h10.5l-10.5 11h11")),
            "w" to key("W", accent = listOf("M3 9l3.2 10.5l5.8-9l5.8 9L21 9")),
            // Words (trigonometry, linear algebra, ln, mod, n!, Ans, Re, Im, Res), generated by preview-render/key_letters.py:
            // one set of letters on a shared baseline and x-height, round letters dipping just past both, each on a canvas sized to it.
            // BEGIN GENERATED
            "sin" to key("Sin", ink = listOf("M8.5 11.5c-.6-1.1-1.5-1.8-2.6-1.8c-1.4 0-2.4.9-2.4 2.2c0 3 5.2 1.8 5.2 5c0 1.6-1.1 2.8-2.8 2.8c-1.2 0-2.3-.6-2.9-1.8", "M12.1 10v9.5", "M15.5 19.5V10", "M15.5 12.6c.9-1.7 2-2.6 3.1-2.6c1.5 0 2.3 1.1 2.3 3v6.5"), inkFill = listOf("M10.85 6.4a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z")),
            "inverse sin" to key("ASin", width = 30f, ink = listOf("M15.3 11.5c-.6-1.1-1.5-1.8-2.6-1.8c-1.4 0-2.4.9-2.4 2.2c0 3 5.2 1.8 5.2 5c0 1.6-1.1 2.8-2.8 2.8c-1.2 0-2.3-.6-2.9-1.8", "M18.9 10v9.5", "M22.3 19.5V10", "M22.3 12.6c.9-1.7 2-2.6 3.1-2.6c1.5 0 2.3 1.1 2.3 3v6.5"), accent = listOf("M2.3 14.75a2.5 5 0 1 0 5 0a2.5 5 0 1 0 -5 0z", "M7.3 10v9.5"), inkFill = listOf("M17.65 6.4a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z")),
            "hyperbolic sin" to key("Sinh", width = 30f, ink = listOf("M7.5 11.5c-.6-1.1-1.5-1.8-2.6-1.8c-1.4 0-2.4.9-2.4 2.2c0 3 5.2 1.8 5.2 5c0 1.6-1.1 2.8-2.8 2.8c-1.2 0-2.3-.6-2.9-1.8", "M11.1 10v9.5", "M14.5 19.5V10", "M14.5 12.6c.9-1.7 2-2.6 3.1-2.6c1.5 0 2.3 1.1 2.3 3v6.5"), accent = listOf("M22.5 4.5v15", "M22.5 12.6c.9-1.7 2-2.6 3.1-2.6c1.5 0 2.3 1.1 2.3 3v6.5"), inkFill = listOf("M9.85 6.4a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z")),
            "inverse hyperbolic sin" to key("ASinh", width = 38f, ink = listOf("M15.3 11.5c-.6-1.1-1.5-1.8-2.6-1.8c-1.4 0-2.4.9-2.4 2.2c0 3 5.2 1.8 5.2 5c0 1.6-1.1 2.8-2.8 2.8c-1.2 0-2.3-.6-2.9-1.8", "M18.9 10v9.5", "M22.3 19.5V10", "M22.3 12.6c.9-1.7 2-2.6 3.1-2.6c1.5 0 2.3 1.1 2.3 3v6.5"), accent = listOf("M2.3 14.75a2.5 5 0 1 0 5 0a2.5 5 0 1 0 -5 0z", "M7.3 10v9.5", "M30.3 4.5v15", "M30.3 12.6c.9-1.7 2-2.6 3.1-2.6c1.5 0 2.3 1.1 2.3 3v6.5"), inkFill = listOf("M17.65 6.4a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z")),
            "cos" to key("Cos", width = 25f, ink = listOf("M6.9 11.6c-.6-1.1-1.5-1.8-2.5-1.8c-1.5 0-2.5 2.2-2.5 5s1 5 2.5 5c1 0 1.9-.6 2.5-1.8", "M9.5 14.75a2.5 5 0 1 0 5 0a2.5 5 0 1 0 -5 0z", "M22.5 11.5c-.6-1.1-1.5-1.8-2.6-1.8c-1.4 0-2.4.9-2.4 2.2c0 3 5.2 1.8 5.2 5c0 1.6-1.1 2.8-2.8 2.8c-1.2 0-2.3-.6-2.9-1.8")),
            "inverse cos" to key("ACos", width = 32f, ink = listOf("M14.2 11.6c-.6-1.1-1.5-1.8-2.5-1.8c-1.5 0-2.5 2.2-2.5 5s1 5 2.5 5c1 0 1.9-.6 2.5-1.8", "M16.8 14.75a2.5 5 0 1 0 5 0a2.5 5 0 1 0 -5 0z", "M29.8 11.5c-.6-1.1-1.5-1.8-2.6-1.8c-1.4 0-2.4.9-2.4 2.2c0 3 5.2 1.8 5.2 5c0 1.6-1.1 2.8-2.8 2.8c-1.2 0-2.3-.6-2.9-1.8"), accent = listOf("M2 14.75a2.5 5 0 1 0 5 0a2.5 5 0 1 0 -5 0z", "M7 10v9.5")),
            "hyperbolic cos" to key("Cosh", width = 33f, ink = listOf("M6.9 11.6c-.6-1.1-1.5-1.8-2.5-1.8c-1.5 0-2.5 2.2-2.5 5s1 5 2.5 5c1 0 1.9-.6 2.5-1.8", "M9.5 14.75a2.5 5 0 1 0 5 0a2.5 5 0 1 0 -5 0z", "M22.5 11.5c-.6-1.1-1.5-1.8-2.6-1.8c-1.4 0-2.4.9-2.4 2.2c0 3 5.2 1.8 5.2 5c0 1.6-1.1 2.8-2.8 2.8c-1.2 0-2.3-.6-2.9-1.8"), accent = listOf("M25.3 4.5v15", "M25.3 12.6c.9-1.7 2-2.6 3.1-2.6c1.5 0 2.3 1.1 2.3 3v6.5")),
            "inverse hyperbolic cos" to key("ACosh", width = 40f, ink = listOf("M14.2 11.6c-.6-1.1-1.5-1.8-2.5-1.8c-1.5 0-2.5 2.2-2.5 5s1 5 2.5 5c1 0 1.9-.6 2.5-1.8", "M16.8 14.75a2.5 5 0 1 0 5 0a2.5 5 0 1 0 -5 0z", "M29.8 11.5c-.6-1.1-1.5-1.8-2.6-1.8c-1.4 0-2.4.9-2.4 2.2c0 3 5.2 1.8 5.2 5c0 1.6-1.1 2.8-2.8 2.8c-1.2 0-2.3-.6-2.9-1.8"), accent = listOf("M2 14.75a2.5 5 0 1 0 5 0a2.5 5 0 1 0 -5 0z", "M7 10v9.5", "M32.6 4.5v15", "M32.6 12.6c.9-1.7 2-2.6 3.1-2.6c1.5 0 2.3 1.1 2.3 3v6.5")),
            "tan" to key("Tan", ink = listOf("M3.5 5.5v11.8c0 1.4.6 2.2 1.7 2.2c.5 0 1-.2 1.3-.5", "M2 10h4.2", "M9 14.75a2.5 5 0 1 0 5 0a2.5 5 0 1 0 -5 0z", "M14 10v9.5", "M16.6 19.5V10", "M16.6 12.6c.9-1.7 2-2.6 3.1-2.6c1.5 0 2.3 1.1 2.3 3v6.5")),
            "inverse tan" to key("ATan", width = 32f, ink = listOf("M11.3 5.5v11.8c0 1.4.6 2.2 1.7 2.2c.5 0 1-.2 1.3-.5", "M9.8 10h4.2", "M16.8 14.75a2.5 5 0 1 0 5 0a2.5 5 0 1 0 -5 0z", "M21.8 10v9.5", "M24.4 19.5V10", "M24.4 12.6c.9-1.7 2-2.6 3.1-2.6c1.5 0 2.3 1.1 2.3 3v6.5"), accent = listOf("M2.2 14.75a2.5 5 0 1 0 5 0a2.5 5 0 1 0 -5 0z", "M7.2 10v9.5")),
            "hyperbolic tan" to key("Tanh", width = 32f, ink = listOf("M3.5 5.5v11.8c0 1.4.6 2.2 1.7 2.2c.5 0 1-.2 1.3-.5", "M2 10h4.2", "M9 14.75a2.5 5 0 1 0 5 0a2.5 5 0 1 0 -5 0z", "M14 10v9.5", "M16.6 19.5V10", "M16.6 12.6c.9-1.7 2-2.6 3.1-2.6c1.5 0 2.3 1.1 2.3 3v6.5"), accent = listOf("M24.6 4.5v15", "M24.6 12.6c.9-1.7 2-2.6 3.1-2.6c1.5 0 2.3 1.1 2.3 3v6.5")),
            "inverse hyperbolic tan" to key("ATanh", width = 40f, ink = listOf("M11.3 5.5v11.8c0 1.4.6 2.2 1.7 2.2c.5 0 1-.2 1.3-.5", "M9.8 10h4.2", "M16.8 14.75a2.5 5 0 1 0 5 0a2.5 5 0 1 0 -5 0z", "M21.8 10v9.5", "M24.4 19.5V10", "M24.4 12.6c.9-1.7 2-2.6 3.1-2.6c1.5 0 2.3 1.1 2.3 3v6.5"), accent = listOf("M2.2 14.75a2.5 5 0 1 0 5 0a2.5 5 0 1 0 -5 0z", "M7.2 10v9.5", "M32.4 4.5v15", "M32.4 12.6c.9-1.7 2-2.6 3.1-2.6c1.5 0 2.3 1.1 2.3 3v6.5")),
            "natural log" to key("Ln", accent = listOf("M7.5 4.5v12.8c0 1.4.6 2.2 1.8 2.2", "M11.9 19.5V10", "M11.9 12.6c.9-1.7 2-2.6 3.1-2.6c1.5 0 2.3 1.1 2.3 3v6.5")),
            "mod" to key("Mod", width = 29f, accent = listOf("M2.4 19.5V10", "M2.4 12.4c.7-1.5 1.5-2.4 2.5-2.4c1.2 0 2 .9 2 2.6v6.9", "M6.9 12.6c.7-1.6 1.5-2.6 2.5-2.6c1.2 0 2 .9 2 2.6v6.9", "M14 14.75a2.5 5 0 1 0 5 0a2.5 5 0 1 0 -5 0z", "M21.6 14.75a2.5 5 0 1 0 5 0a2.5 5 0 1 0 -5 0z", "M26.6 4.5v15")),
            "factorial" to key("Factorial", ink = listOf("M7.1 19.5V10", "M7.1 12.6c.9-1.7 2-2.6 3.1-2.6c1.5 0 2.3 1.1 2.3 3v6.5"), accent = listOf("M16 4.5v10.5"), accentFill = listOf("M14.55 18.9a1.45 1.45 0 1 0 2.9 0a1.45 1.45 0 1 0 -2.9 0z")),
            "previous answer" to key("Ans", width = 28f, ink = listOf("M2.4 19.5L5.9 4.5L9.4 19.5", "M3.7 14.3h4.4", "M12 19.5V10", "M12 12.6c.9-1.7 2-2.6 3.1-2.6c1.5 0 2.3 1.1 2.3 3v6.5", "M25.4 11.5c-.6-1.1-1.5-1.8-2.6-1.8c-1.4 0-2.4.9-2.4 2.2c0 3 5.2 1.8 5.2 5c0 1.6-1.1 2.8-2.8 2.8c-1.2 0-2.3-.6-2.9-1.8")),
            "real part" to key("Re", ink = listOf("M14.1 15h5c0-3.3-1.1-5.3-2.6-5.3s-2.4 2.3-2.4 5.1s1 5.1 2.6 5.1c1 0 1.8-.6 2.4-1.6"), accent = listOf("M4.9 19.5V4.5h3.2a3.6 3.6 0 0 1 0 7.2H4.9", "M7.9 11.7l3.6 7.8")),
            "imaginary part" to key("Im", ink = listOf("M11 19.5V10", "M11 12.4c.7-1.5 1.5-2.4 2.5-2.4c1.2 0 2 .9 2 2.6v6.9", "M15.5 12.6c.7-1.6 1.5-2.6 2.5-2.6c1.2 0 2 .9 2 2.6v6.9"), accent = listOf("M6.2 4.5v15", "M4 4.5h4.4", "M4 19.5h4.4")),
            "residue" to key("Residue", width = 27f, ink = listOf("M11.5 15h5c0-3.3-1.1-5.3-2.6-5.3s-2.4 2.3-2.4 5.1s1 5.1 2.6 5.1c1 0 1.8-.6 2.4-1.6", "M24.5 11.5c-.6-1.1-1.5-1.8-2.6-1.8c-1.4 0-2.4.9-2.4 2.2c0 3 5.2 1.8 5.2 5c0 1.6-1.1 2.8-2.8 2.8c-1.2 0-2.3-.6-2.9-1.8"), accent = listOf("M2.3 19.5V4.5h3.2a3.6 3.6 0 0 1 0 7.2H2.3", "M5.3 11.7l3.6 7.8")),
            "matrix" to key("Matrix", accent = listOf("M8.5 4.5H6v15h2.5", "M15.5 4.5H18v15h-2.5"), accentFill = listOf("M8.8 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M12.4 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M8.8 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M12.4 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z")),
            "inverse" to key("Inverse", ink = listOf("M4.5 4.5H2v15h2.5", "M11.5 4.5H14v15h-2.5"), accent = listOf("M16.6 6.5h2.4", "M20.4 4.4l1.6-1.2v7.6"), inkFill = listOf("M4.8 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M8.4 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M4.8 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M8.4 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z")),
            "transpose" to key("Transpose", ink = listOf("M4.9 4.5H2.4v15h2.5", "M11.9 4.5H14.4v15h-2.5"), accent = listOf("M17 3.2h4.6", "M19.3 3.2v7.8"), inkFill = listOf("M5.2 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M8.8 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M5.2 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M8.8 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z")),
            "conjugate transpose" to key("Adjoint", ink = listOf("M5 4.5H2.5v15h2.5", "M12 4.5H14.5v15h-2.5"), accent = listOf("M17.1 3.2v7.8", "M21.5 3.2v7.8", "M17.1 7.1h4.4"), inkFill = listOf("M5.3 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M8.9 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M5.3 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M8.9 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z")),
            "determinant" to key("Det", width = 39f, ink = listOf("M27.1 4.5H24.6v15h2.5", "M34.1 4.5H36.6v15h-2.5"), accent = listOf("M2.4 14.75a2.5 5 0 1 0 5 0a2.5 5 0 1 0 -5 0z", "M7.4 4.5v15", "M10 15h5c0-3.3-1.1-5.3-2.6-5.3s-2.4 2.3-2.4 5.1s1 5.1 2.6 5.1c1 0 1.8-.6 2.4-1.6", "M19.1 5.5v11.8c0 1.4.6 2.2 1.7 2.2c.5 0 1-.2 1.3-.5", "M17.6 10h4.2"), inkFill = listOf("M27.4 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M31 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M27.4 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M31 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z")),
            "dot product" to key("Dot", width = 37f, ink = listOf("M4.9 4.5H2.4v15h2.5", "M11.9 4.5H14.4v15h-2.5", "M25.1 4.5H22.6v15h2.5", "M32.1 4.5H34.6v15h-2.5"), inkFill = listOf("M5.2 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M8.8 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M5.2 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M8.8 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M25.4 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M29 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M25.4 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M29 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z"), accentFill = listOf("M17 12a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0z")),
            "cross product" to key("Cross", width = 38f, ink = listOf("M4.9 4.5H2.4v15h2.5", "M11.9 4.5H14.4v15h-2.5", "M26.1 4.5H23.6v15h2.5", "M33.1 4.5H35.6v15h-2.5"), accent = listOf("M17 10l4 4", "M21 10l-4 4"), inkFill = listOf("M5.2 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M8.8 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M5.2 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M8.8 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M26.4 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M30 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M26.4 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M30 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z")),
            "Hadamard product" to key("Hadamard", width = 38f, ink = listOf("M4.9 4.5H2.4v15h2.5", "M11.9 4.5H14.4v15h-2.5", "M26.1 4.5H23.6v15h2.5", "M33.1 4.5H35.6v15h-2.5"), accent = listOf("M17.1 12a1.9 1.9 0 1 0 3.8 0a1.9 1.9 0 1 0 -3.8 0z"), inkFill = listOf("M5.2 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M8.8 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M5.2 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M8.8 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M26.4 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M30 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M26.4 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M30 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z")),
            "Kronecker product" to key("Kronecker", width = 39f, ink = listOf("M4.6 4.5H2.1v15h2.5", "M11.6 4.5H14.1v15h-2.5", "M27.4 4.5H24.9v15h2.5", "M34.4 4.5H36.9v15h-2.5"), accent = listOf("M16.7 12a2.8 2.8 0 1 0 5.6 0a2.8 2.8 0 1 0 -5.6 0z", "M17.5 10l4 4", "M21.5 10l-4 4"), inkFill = listOf("M4.9 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M8.5 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M4.9 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M8.5 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M27.7 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M31.3 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M27.7 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M31.3 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z")),
            "trace" to key("Trace", width = 29f, ink = listOf("M17.5 4.5H15v15h2.5", "M24.5 4.5H27v15h-2.5"), accent = listOf("M3.5 5.5v11.8c0 1.4.6 2.2 1.7 2.2c.5 0 1-.2 1.3-.5", "M2 10h4.2", "M9 19.5V10", "M9 13.2c.7-2 1.8-3.2 3.4-3.2"), inkFill = listOf("M17.8 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M21.4 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M17.8 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M21.4 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z")),
            "eigenvalues" to key("Eigenvalues", accent = listOf("M8.8 19.5l3.5-6.6", "M9.4 4.5c1.1 0 1.9.6 2.4 1.9L15.2 19.5")),
            "eigenvectors" to key("Eigenvectors", ink = listOf("M9.5 10l5 9.5", "M14.5 10l-5 9.5"), accent = listOf("M11.6 6.2h6", "M15.5 4.2l2.1 2l-2.1 2")),
            "characteristic polynomial" to key("Charpoly", width = 29f, ink = listOf("M2.3 10v12.5", "M2.3 14.75a2.5 5 0 1 0 5 0a2.5 5 0 1 0 -5 0z", "M12.5 4.5c-1.7 1.9-2.6 4.6-2.6 7.5s.9 5.6 2.6 7.5", "M24.1 4.5c1.7 1.9 2.6 4.6 2.6 7.5s-.9 5.6-2.6 7.5"), accent = listOf("M15.1 19.5l3.5-6.6", "M15.7 4.5c1.1 0 1.9.6 2.4 1.9L21.5 19.5")),
            "reduced row echelon form" to key("Rref", width = 43f, ink = listOf("M31.1 4.5H28.6v15h2.5", "M38.1 4.5H40.6v15h-2.5"), accent = listOf("M2.4 19.5V10", "M2.4 13.2c.7-2 1.8-3.2 3.4-3.2", "M8.4 19.5V10", "M8.4 13.2c.7-2 1.8-3.2 3.4-3.2", "M14.4 15h5c0-3.3-1.1-5.3-2.6-5.3s-2.4 2.3-2.4 5.1s1 5.1 2.6 5.1c1 0 1.8-.6 2.4-1.6", "M26 5.1c-1.6-.6-2.8.2-2.8 2.2v12.2", "M22 10h3.6"), inkFill = listOf("M31.4 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M35 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M31.4 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M35 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z")),
            "rank" to key("Rank", width = 30f, ink = listOf("M18.1 4.5H15.6v15h2.5", "M25.1 4.5H27.6v15h-2.5"), accent = listOf("M2.4 19.5V10", "M2.4 13.2c.7-2 1.8-3.2 3.4-3.2", "M8.4 4.5v15", "M12.8 10l-4.4 4.8", "M9.9 13.2l3.1 6.3"), inkFill = listOf("M18.4 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M22 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M18.4 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M22 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z")),
            // END GENERATED
        )
    }

    /** The icon for a key, if it has one. */
    fun forKey(spoken: String): ImageVector? = byKey[spoken]

    /** Every key icon, by key, for the settings preview. */
    val all: Map<String, ImageVector> get() = byKey
}
