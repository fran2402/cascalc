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
            "sin" to key("Sin", width = 27f, ink = listOf("M9.33 11.5C8.55 10.4 7.38 9.7 5.95 9.7C4.13 9.7 2.83 10.6 2.83 11.9C2.83 14.9 9.59 13.7 9.59 16.9C9.59 18.5 8.16 19.7 5.95 19.7C4.39 19.7 2.96 19.1 2.18 17.9", "M13.63 10V19.5", "M17.67 19.5V10", "M17.67 12.6C18.84 10.9 20.27 10 21.7 10C23.65 10 24.69 11.1 24.69 13V19.5"), inkFill = listOf("M12.38 6.4a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z")),
            "inverse sin" to key("ASin", width = 36f, ink = listOf("M18.58 11.5C17.8 10.4 16.63 9.7 15.2 9.7C13.38 9.7 12.08 10.6 12.08 11.9C12.08 14.9 18.84 13.7 18.84 16.9C18.84 18.5 17.41 19.7 15.2 19.7C13.64 19.7 12.21 19.1 11.43 17.9", "M22.88 10V19.5", "M26.92 19.5V10", "M26.92 12.6C28.09 10.9 29.52 10 30.95 10C32.9 10 33.94 11.1 33.94 13V19.5"), accent = listOf("M2.06 14.75C2.06 17.51 3.52 19.75 5.31 19.75C7.1 19.75 8.56 17.51 8.56 14.75C8.56 11.99 7.1 9.75 5.31 9.75C3.52 9.75 2.06 11.99 2.06 14.75Z", "M8.56 10V19.5"), inkFill = listOf("M21.63 6.4a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z")),
            "hyperbolic sin" to key("Sinh", width = 37f, ink = listOf("M9.32 11.5C8.54 10.4 7.37 9.7 5.94 9.7C4.12 9.7 2.82 10.6 2.82 11.9C2.82 14.9 9.58 13.7 9.58 16.9C9.58 18.5 8.15 19.7 5.94 19.7C4.38 19.7 2.95 19.1 2.17 17.9", "M13.62 10V19.5", "M17.66 19.5V10", "M17.66 12.6C18.83 10.9 20.26 10 21.69 10C23.64 10 24.68 11.1 24.68 13V19.5"), accent = listOf("M27.68 4.5V19.5", "M27.68 12.6C28.85 10.9 30.28 10 31.71 10C33.66 10 34.7 11.1 34.7 13V19.5"), inkFill = listOf("M12.37 6.4a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z")),
            "inverse hyperbolic sin" to key("ASinh", width = 46f, ink = listOf("M18.57 11.5C17.79 10.4 16.62 9.7 15.19 9.7C13.37 9.7 12.07 10.6 12.07 11.9C12.07 14.9 18.83 13.7 18.83 16.9C18.83 18.5 17.4 19.7 15.19 19.7C13.63 19.7 12.2 19.1 11.42 17.9", "M22.87 10V19.5", "M26.91 19.5V10", "M26.91 12.6C28.08 10.9 29.51 10 30.94 10C32.89 10 33.93 11.1 33.93 13V19.5"), accent = listOf("M2.05 14.75C2.05 17.51 3.51 19.75 5.3 19.75C7.09 19.75 8.55 17.51 8.55 14.75C8.55 11.99 7.09 9.75 5.3 9.75C3.51 9.75 2.05 11.99 2.05 14.75Z", "M8.55 10V19.5", "M36.93 4.5V19.5", "M36.93 12.6C38.1 10.9 39.53 10 40.96 10C42.91 10 43.95 11.1 43.95 13V19.5"), inkFill = listOf("M21.62 6.4a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z")),
            "cos" to key("Cos", width = 30f, ink = listOf("M8.1 11.6C7.32 10.5 6.15 9.8 4.85 9.8C2.9 9.8 1.6 12 1.6 14.8C1.6 17.6 2.9 19.8 4.85 19.8C6.15 19.8 7.32 19.2 8.1 18", "M11.1 14.75C11.1 17.51 12.56 19.75 14.35 19.75C16.14 19.75 17.6 17.51 17.6 14.75C17.6 11.99 16.14 9.75 14.35 9.75C12.56 9.75 11.1 11.99 11.1 14.75Z", "M27.62 11.5C26.84 10.4 25.67 9.7 24.24 9.7C22.42 9.7 21.12 10.6 21.12 11.9C21.12 14.9 27.88 13.7 27.88 16.9C27.88 18.5 26.45 19.7 24.24 19.7C22.68 19.7 21.25 19.1 20.47 17.9")),
            "inverse cos" to key("ACos", width = 40f, ink = listOf("M17.85 11.6C17.07 10.5 15.9 9.8 14.6 9.8C12.65 9.8 11.35 12 11.35 14.8C11.35 17.6 12.65 19.8 14.6 19.8C15.9 19.8 17.07 19.2 17.85 18", "M20.85 14.75C20.85 17.51 22.31 19.75 24.1 19.75C25.89 19.75 27.35 17.51 27.35 14.75C27.35 11.99 25.89 9.75 24.1 9.75C22.31 9.75 20.85 11.99 20.85 14.75Z", "M37.37 11.5C36.59 10.4 35.42 9.7 33.99 9.7C32.17 9.7 30.87 10.6 30.87 11.9C30.87 14.9 37.63 13.7 37.63 16.9C37.63 18.5 36.2 19.7 33.99 19.7C32.43 19.7 31 19.1 30.22 17.9"), accent = listOf("M2.37 14.75C2.37 17.51 3.83 19.75 5.62 19.75C7.41 19.75 8.87 17.51 8.87 14.75C8.87 11.99 7.41 9.75 5.62 9.75C3.83 9.75 2.37 11.99 2.37 14.75Z", "M8.87 10V19.5")),
            "hyperbolic cos" to key("Cosh", width = 40f, ink = listOf("M8.09 11.6C7.31 10.5 6.14 9.8 4.84 9.8C2.89 9.8 1.59 12 1.59 14.8C1.59 17.6 2.89 19.8 4.84 19.8C6.14 19.8 7.31 19.2 8.09 18", "M11.09 14.75C11.09 17.51 12.55 19.75 14.34 19.75C16.13 19.75 17.59 17.51 17.59 14.75C17.59 11.99 16.13 9.75 14.34 9.75C12.55 9.75 11.09 11.99 11.09 14.75Z", "M27.61 11.5C26.83 10.4 25.66 9.7 24.23 9.7C22.41 9.7 21.11 10.6 21.11 11.9C21.11 14.9 27.87 13.7 27.87 16.9C27.87 18.5 26.44 19.7 24.23 19.7C22.67 19.7 21.24 19.1 20.46 17.9"), accent = listOf("M30.87 4.5V19.5", "M30.87 12.6C32.04 10.9 33.47 10 34.9 10C36.85 10 37.89 11.1 37.89 13V19.5")),
            "inverse hyperbolic cos" to key("ACosh", width = 50f, ink = listOf("M17.84 11.6C17.06 10.5 15.89 9.8 14.59 9.8C12.64 9.8 11.34 12 11.34 14.8C11.34 17.6 12.64 19.8 14.59 19.8C15.89 19.8 17.06 19.2 17.84 18", "M20.84 14.75C20.84 17.51 22.3 19.75 24.09 19.75C25.88 19.75 27.34 17.51 27.34 14.75C27.34 11.99 25.88 9.75 24.09 9.75C22.3 9.75 20.84 11.99 20.84 14.75Z", "M37.36 11.5C36.58 10.4 35.41 9.7 33.98 9.7C32.16 9.7 30.86 10.6 30.86 11.9C30.86 14.9 37.62 13.7 37.62 16.9C37.62 18.5 36.19 19.7 33.98 19.7C32.42 19.7 30.99 19.1 30.21 17.9"), accent = listOf("M2.36 14.75C2.36 17.51 3.82 19.75 5.61 19.75C7.4 19.75 8.86 17.51 8.86 14.75C8.86 11.99 7.4 9.75 5.61 9.75C3.82 9.75 2.36 11.99 2.36 14.75Z", "M8.86 10V19.5", "M40.62 4.5V19.5", "M40.62 12.6C41.79 10.9 43.22 10 44.65 10C46.6 10 47.64 11.1 47.64 13V19.5")),
            "tan" to key("Tan", width = 30f, ink = listOf("M4.33 5.5V17.3C4.33 18.7 5.11 19.5 6.54 19.5C7.19 19.5 7.84 19.3 8.23 19", "M2.38 10H7.84", "M11.1 14.75C11.1 17.51 12.56 19.75 14.35 19.75C16.14 19.75 17.6 17.51 17.6 14.75C17.6 11.99 16.14 9.75 14.35 9.75C12.56 9.75 11.1 11.99 11.1 14.75Z", "M17.6 10V19.5", "M20.6 19.5V10", "M20.6 12.6C21.77 10.9 23.2 10 24.63 10C26.58 10 27.62 11.1 27.62 13V19.5")),
            "inverse tan" to key("ATan", width = 39f, ink = listOf("M13.58 5.5V17.3C13.58 18.7 14.36 19.5 15.79 19.5C16.44 19.5 17.09 19.3 17.48 19", "M11.63 10H17.09", "M20.35 14.75C20.35 17.51 21.81 19.75 23.6 19.75C25.39 19.75 26.85 17.51 26.85 14.75C26.85 11.99 25.39 9.75 23.6 9.75C21.81 9.75 20.35 11.99 20.35 14.75Z", "M26.85 10V19.5", "M29.85 19.5V10", "M29.85 12.6C31.02 10.9 32.45 10 33.88 10C35.83 10 36.87 11.1 36.87 13V19.5"), accent = listOf("M2.13 14.75C2.13 17.51 3.59 19.75 5.38 19.75C7.17 19.75 8.63 17.51 8.63 14.75C8.63 11.99 7.17 9.75 5.38 9.75C3.59 9.75 2.13 11.99 2.13 14.75Z", "M8.63 10V19.5")),
            "hyperbolic tan" to key("Tanh", width = 40f, ink = listOf("M4.32 5.5V17.3C4.32 18.7 5.1 19.5 6.53 19.5C7.18 19.5 7.83 19.3 8.22 19", "M2.37 10H7.83", "M11.09 14.75C11.09 17.51 12.55 19.75 14.34 19.75C16.13 19.75 17.59 17.51 17.59 14.75C17.59 11.99 16.13 9.75 14.34 9.75C12.55 9.75 11.09 11.99 11.09 14.75Z", "M17.59 10V19.5", "M20.59 19.5V10", "M20.59 12.6C21.76 10.9 23.19 10 24.62 10C26.57 10 27.61 11.1 27.61 13V19.5"), accent = listOf("M30.61 4.5V19.5", "M30.61 12.6C31.78 10.9 33.21 10 34.64 10C36.59 10 37.63 11.1 37.63 13V19.5")),
            "inverse hyperbolic tan" to key("ATanh", width = 49f, ink = listOf("M13.57 5.5V17.3C13.57 18.7 14.35 19.5 15.78 19.5C16.43 19.5 17.08 19.3 17.47 19", "M11.62 10H17.08", "M20.34 14.75C20.34 17.51 21.8 19.75 23.59 19.75C25.38 19.75 26.84 17.51 26.84 14.75C26.84 11.99 25.38 9.75 23.59 9.75C21.8 9.75 20.34 11.99 20.34 14.75Z", "M26.84 10V19.5", "M29.84 19.5V10", "M29.84 12.6C31.01 10.9 32.44 10 33.87 10C35.82 10 36.86 11.1 36.86 13V19.5"), accent = listOf("M2.12 14.75C2.12 17.51 3.58 19.75 5.37 19.75C7.16 19.75 8.62 17.51 8.62 14.75C8.62 11.99 7.16 9.75 5.37 9.75C3.58 9.75 2.12 11.99 2.12 14.75Z", "M8.62 10V19.5", "M39.86 4.5V19.5", "M39.86 12.6C41.03 10.9 42.46 10 43.89 10C45.84 10 46.88 11.1 46.88 13V19.5")),
            "natural log" to key("Ln", accent = listOf("M6.34 4.5V17.3C6.34 18.7 7.12 19.5 8.68 19.5", "M11.68 19.5V10", "M11.68 12.6C12.85 10.9 14.28 10 15.71 10C17.66 10 18.7 11.1 18.7 13V19.5")),
            "mod" to key("Mod", width = 35f, accent = listOf("M2.15 19.5V10", "M2.15 12.4C3.06 10.9 4.1 10 5.4 10C6.96 10 8 10.9 8 12.6V19.5", "M8 12.6C8.91 11 9.95 10 11.25 10C12.81 10 13.85 10.9 13.85 12.6V19.5", "M16.85 14.75C16.85 17.51 18.31 19.75 20.1 19.75C21.89 19.75 23.35 17.51 23.35 14.75C23.35 11.99 21.89 9.75 20.1 9.75C18.31 9.75 16.85 11.99 16.85 14.75Z", "M26.35 14.75C26.35 17.51 27.81 19.75 29.6 19.75C31.39 19.75 32.85 17.51 32.85 14.75C32.85 11.99 31.39 9.75 29.6 9.75C27.81 9.75 26.35 11.99 26.35 14.75Z", "M32.85 4.5V19.5")),
            "factorial" to key("Factorial", ink = listOf("M5.82 19.5V10", "M5.82 12.6C6.99 10.9 8.42 10 9.85 10C11.8 10 12.84 11.1 12.84 13V19.5"), accent = listOf("M17.01 4.5V15"), accentFill = listOf("M15.56 18.9a1.45 1.45 0 1 0 2.9 0a1.45 1.45 0 1 0 -2.9 0z")),
            "previous answer" to key("Ans", width = 34f, ink = listOf("M2.3 19.5 6.85 4.5 11.4 19.5", "M3.99 14.3H9.71", "M14.4 19.5V10", "M14.4 12.6C15.57 10.9 17 10 18.43 10C20.38 10 21.42 11.1 21.42 13V19.5", "M31.44 11.5C30.66 10.4 29.49 9.7 28.06 9.7C26.24 9.7 24.94 10.6 24.94 11.9C24.94 14.9 31.7 13.7 31.7 16.9C31.7 18.5 30.27 19.7 28.06 19.7C26.5 19.7 25.07 19.1 24.29 17.9")),
            "real part" to key("Re", ink = listOf("M14.54 15H21.04C21.04 11.7 19.61 9.7 17.66 9.7C15.71 9.7 14.54 12 14.54 14.8C14.54 17.6 15.84 19.9 17.92 19.9C19.22 19.9 20.26 19.3 21.04 18.3"), accent = listOf("M2.96 19.5V4.5H7.12C9.7 4.5 11.8 6.11 11.8 8.1C11.8 10.09 9.7 11.7 7.12 11.7L2.96 11.7", "M6.86 11.7 11.54 19.5")),
            "imaginary part" to key("Im", width = 25f, ink = listOf("M11.01 19.5V10", "M11.01 12.4C11.92 10.9 12.96 10 14.26 10C15.82 10 16.86 10.9 16.86 12.6V19.5", "M16.86 12.6C17.77 11 18.81 10 20.11 10C21.67 10 22.71 10.9 22.71 12.6V19.5"), accent = listOf("M5.15 4.5V19.5", "M2.29 4.5H8.01", "M2.29 19.5H8.01")),
            "residue" to key("Residue", width = 33f, ink = listOf("M13.9 15H20.4C20.4 11.7 18.97 9.7 17.02 9.7C15.07 9.7 13.9 12 13.9 14.8C13.9 17.6 15.2 19.9 17.28 19.9C18.58 19.9 19.62 19.3 20.4 18.3", "M30.42 11.5C29.64 10.4 28.47 9.7 27.04 9.7C25.22 9.7 23.92 10.6 23.92 11.9C23.92 14.9 30.68 13.7 30.68 16.9C30.68 18.5 29.25 19.7 27.04 19.7C25.48 19.7 24.05 19.1 23.27 17.9"), accent = listOf("M2.32 19.5V4.5H6.48C9.06 4.5 11.16 6.11 11.16 8.1C11.16 10.09 9.06 11.7 6.48 11.7L2.32 11.7", "M6.22 11.7 10.9 19.5")),
            "matrix" to key("Matrix", accent = listOf("M7.8 4.5H4.8V19.5H7.8", "M16.2 4.5H19.2V19.5H16.2"), accentFill = listOf("M8.44 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M12.76 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M8.44 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M12.76 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z")),
            "inverse" to key("Inverse", width = 29f, ink = listOf("M5.29 4.5H2.29V19.5H5.29", "M13.69 4.5H16.69V19.5H13.69"), accent = listOf("M19.69 6.5H22.81", "M24.63 4.4 26.71 3.2V10.8"), inkFill = listOf("M5.93 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M10.25 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M5.93 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M10.25 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z")),
            "transpose" to key("Transpose", width = 28f, ink = listOf("M5.31 4.5H2.31V19.5H5.31", "M13.71 4.5H16.71V19.5H13.71"), accent = listOf("M19.71 3.2H25.69", "M22.7 3.2V11"), inkFill = listOf("M5.95 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M10.27 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M5.95 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M10.27 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z")),
            "conjugate transpose" to key("Adjoint", width = 28f, ink = listOf("M5.44 4.5H2.44V19.5H5.44", "M13.84 4.5H16.84V19.5H13.84"), accent = listOf("M19.84 3.2V11", "M25.56 3.2V11", "M19.84 7.1H25.56"), inkFill = listOf("M6.08 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M10.4 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M6.08 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M10.4 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z")),
            "determinant" to key("Det", width = 47f, ink = listOf("M33.16 4.5H30.16V19.5H33.16", "M41.56 4.5H44.56V19.5H41.56"), accent = listOf("M2.44 14.75C2.44 17.51 3.9 19.75 5.69 19.75C7.48 19.75 8.94 17.51 8.94 14.75C8.94 11.99 7.48 9.75 5.69 9.75C3.9 9.75 2.44 11.99 2.44 14.75Z", "M8.94 4.5V19.5", "M11.94 15H18.44C18.44 11.7 17.01 9.7 15.06 9.7C13.11 9.7 11.94 12 11.94 14.8C11.94 17.6 13.24 19.9 15.32 19.9C16.62 19.9 17.66 19.3 18.44 18.3", "M23.39 5.5V17.3C23.39 18.7 24.17 19.5 25.6 19.5C26.25 19.5 26.9 19.3 27.29 19", "M21.44 10H26.9"), inkFill = listOf("M33.8 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M38.12 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M33.8 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M38.12 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z")),
            "dot product" to key("Dot", width = 31f, ink = listOf("M4 8.5H2v7h2", "M11 8.5h2v7h-2", "M24 5H22v14h2", "M27 5h2v14h-2"), inkFill = listOf("M4.65 12a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z", "M7.85 12a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z", "M24.25 9.5a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z", "M24.25 14.5a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z"), accentFill = listOf("M16 12a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0z")),
            "cross product" to key("Cross", width = 32f, ink = listOf("M4 8.5H2v7h2", "M11 8.5h2v7h-2", "M25 5H23v14h2", "M28 5h2v14h-2"), accent = listOf("M16 10l4 4", "M20 10l-4 4"), inkFill = listOf("M4.65 12a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z", "M7.85 12a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z", "M25.25 9.5a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z", "M25.25 14.5a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z")),
            "Hadamard product" to key("Hadamard", width = 32f, ink = listOf("M4 6H2v12h2", "M9 6h2v12h-2", "M23 6H21v12h2", "M28 6h2v12h-2"), accent = listOf("M14.1 12a1.9 1.9 0 1 0 3.8 0a1.9 1.9 0 1 0 -3.8 0z"), inkFill = listOf("M3.95 9.5a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z", "M6.55 9.5a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z", "M3.95 14.5a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z", "M6.55 14.5a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z", "M22.95 9.5a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z", "M25.55 9.5a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z", "M22.95 14.5a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z", "M25.55 14.5a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z")),
            "Kronecker product" to key("Kronecker", width = 34f, ink = listOf("M4.2 6H2.2v12h2", "M9.2 6h2v12h-2", "M24.8 6H22.8v12h2", "M29.8 6h2v12h-2"), accent = listOf("M14.2 12a2.8 2.8 0 1 0 5.6 0a2.8 2.8 0 1 0 -5.6 0z", "M15 10l4 4", "M19 10l-4 4"), inkFill = listOf("M4.15 9.5a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z", "M6.75 9.5a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z", "M4.15 14.5a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z", "M6.75 14.5a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z", "M24.75 9.5a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z", "M27.35 9.5a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z", "M24.75 14.5a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z", "M27.35 14.5a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0 -2.5 0z")),
            "trace" to key("Trace", width = 35f, ink = listOf("M21.37 4.5H18.37V19.5H21.37", "M29.77 4.5H32.77V19.5H29.77"), accent = listOf("M4.18 5.5V17.3C4.18 18.7 4.96 19.5 6.39 19.5C7.04 19.5 7.69 19.3 8.08 19", "M2.23 10H7.69", "M10.95 19.5V10", "M10.95 13.2C11.86 11.2 13.29 10 15.37 10"), inkFill = listOf("M22.01 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M26.33 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M22.01 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M26.33 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z")),
            "eigenvalues" to key("Eigenvalues", accent = listOf("M7.84 19.5 12.39 12.9", "M8.62 4.5C10.05 4.5 11.09 5.1 11.74 6.4L16.16 19.5")),
            "eigenvectors" to key("Eigenvectors", ink = listOf("M8.75 10 15.25 19.5", "M15.25 10 8.75 19.5"), accent = listOf("M13.95 6.2H21.75", "M19.02 4.2 21.75 6.2 19.02 8.2")),
            "characteristic polynomial" to key("Charpoly", width = 35f, ink = listOf("M2.21 10V22.5", "M2.21 14.75C2.21 17.51 3.67 19.75 5.46 19.75C7.25 19.75 8.71 17.51 8.71 14.75C8.71 11.99 7.25 9.75 5.46 9.75C3.67 9.75 2.21 11.99 2.21 14.75Z", "M15.09 4.5C12.88 6.4 11.71 9.1 11.71 12C11.71 14.9 12.88 17.6 15.09 19.5", "M29.41 4.5C31.62 6.4 32.79 9.1 32.79 12C32.79 14.9 31.62 17.6 29.41 19.5"), accent = listOf("M18.09 19.5 22.64 12.9", "M18.87 4.5C20.3 4.5 21.34 5.1 21.99 6.4L26.41 19.5")),
            "reduced row echelon form" to key("Rref", width = 51f, ink = listOf("M37.57 4.5H34.57V19.5H37.57", "M45.97 4.5H48.97V19.5H45.97"), accent = listOf("M2.03 19.5V10", "M2.03 13.2C2.94 11.2 4.37 10 6.45 10", "M9.45 19.5V10", "M9.45 13.2C10.36 11.2 11.79 10 13.87 10", "M16.87 15H23.37C23.37 11.7 21.94 9.7 19.99 9.7C18.04 9.7 16.87 12 16.87 14.8C16.87 17.6 18.17 19.9 20.25 19.9C21.55 19.9 22.59 19.3 23.37 18.3", "M31.57 5.1C29.49 4.5 27.93 5.3 27.93 7.3V19.5", "M26.37 10H31.05"), inkFill = listOf("M38.21 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M42.53 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M38.21 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M42.53 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z")),
            "rank" to key("Rank", width = 35f, ink = listOf("M21.5 4.5H18.5V19.5H21.5", "M29.9 4.5H32.9V19.5H29.9"), accent = listOf("M2.1 19.5V10", "M2.1 13.2C3.01 11.2 4.44 10 6.52 10", "M9.52 4.5V19.5", "M15.24 10 9.52 14.8", "M11.47 13.2 15.5 19.5"), inkFill = listOf("M22.14 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M26.46 9a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M22.14 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z", "M26.46 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0z")),
            // END GENERATED
        )
    }

    /** The icon for a key, if it has one. */
    fun forKey(spoken: String): ImageVector? = byKey[spoken]

    /** Every key icon, by key, for the settings preview. */
    val all: Map<String, ImageVector> get() = byKey
}
