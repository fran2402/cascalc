package com.example.cas.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

/** One thing the app is built on or with: what it is, what it's used for, its licence, a link. */
data class Credit(val name: String, val use: String, val licence: String, val url: String)

val CREDITS: List<Pair<String, List<Credit>>> = listOf(
    "Fonts" to listOf(
        Credit("Google Sans Flex", "Keys, labels and text", "SIL Open Font License 1.1", "https://fonts.google.com/specimen/Google+Sans+Flex"),
        Credit("Roboto", "Characters Google Sans Flex doesn't have", "SIL Open Font License 1.1", "https://fonts.google.com/specimen/Roboto"),
        Credit("MathJax TeX fonts (Computer Modern)", "The maths: Main, Math Italic and Size2 (∫, ∮)", "SIL Open Font License 1.1", "https://github.com/mathjax/MathJax/tree/legacy-v2/fonts"),
        Credit("Material Symbols", "Icons", "Apache License 2.0", "https://fonts.google.com/icons"),
    ),
    "Libraries" to listOf(
        Credit("Jetpack Compose", "The interface", "Apache License 2.0", "https://developer.android.com/compose"),
        Credit("Material 3 Expressive (material3 1.4.0-alpha18)", "Components, theme and motion", "Apache License 2.0", "https://m3.material.io"),
        Credit("Kotlin", "The language", "Apache License 2.0", "https://kotlinlang.org"),
    ),
    "Data" to listOf(
        Credit("CODATA 2022 recommended values (NIST)", "Physical constants", "Public (NIST)", "https://physics.nist.gov/cuu/Constants/"),
    ),
    "Methods" to listOf(
        Credit("Lanczos approximation", "The gamma function", "Published method", "https://en.wikipedia.org/wiki/Lanczos_approximation"),
        Credit("Borwein's algorithm", "The Riemann zeta function", "Published method", "https://en.wikipedia.org/wiki/Riemann_zeta_function"),
        Credit("Gauss–Kronrod quadrature", "Numerical integrals", "Published method", "https://en.wikipedia.org/wiki/Gauss%E2%80%93Kronrod_quadrature_formula"),
        Credit("Marching squares", "Implicit curves and regions in 2D", "Published method", "https://en.wikipedia.org/wiki/Marching_squares"),
        Credit("Marching tetrahedra", "Implicit surfaces in 3D", "Published method", "https://en.wikipedia.org/wiki/Marching_tetrahedra"),
        Credit("Domain coloring", "Complex function plots", "Published method", "https://en.wikipedia.org/wiki/Domain_coloring"),
        Credit("OKLab, by Björn Ottosson", "The color picker's OKLab mode", "MIT (reference code)", "https://bottosson.github.io/posts/oklab/"),
        Credit("Acklam's inverse normal approximation", "Φ⁻¹, refined by Newton steps", "Published method", "https://en.wikipedia.org/wiki/Normal_distribution#Generating_values_from_normal_distribution"),
    ),
    "Inspiration" to listOf(
        Credit("Desmos", "Graphing ideas: user functions, restrictions, points, sliders", "—", "https://www.desmos.com/calculator"),
        Credit("Google Calculator", "The keypad and continuing from an answer", "—", "https://play.google.com/store/apps/details?id=com.google.android.calculator"),
    ),
)

/** The acknowledgements, grouped, each with a link; tap one to open it. */
@Composable
fun AcknowledgementsDialog(onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val uri = LocalUriHandler.current
    // A full-screen page, not a popup.
    FullScreenPage("Acknowledgements", onBack = onDismiss) {
            run {
                CREDITS.forEach { (section, credits) ->
                    Text(section, style = MaterialTheme.typography.titleSmall, color = colors.primary)
                    credits.forEach { c ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClickLabel = "Open ${c.name}") { runCatching { uri.openUri(c.url) } }
                                .padding(vertical = 4.dp),
                        ) {
                            Text(c.name, style = MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.Underline), color = colors.onSurface)
                            Text(c.use, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            Text(c.licence, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
    }
}
