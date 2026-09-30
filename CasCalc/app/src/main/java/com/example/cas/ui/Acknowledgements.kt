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
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Dataset
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

/**
 * One thing the app is built on or with: its name, who made it, what it does in this app,
 * a line on what it is, its licence, and a link.
 */
data class Credit(val name: String, val by: String, val use: String, val about: String, val licence: String, val url: String)

/** A group of credits with a line saying what the group is. */
data class CreditGroup(val title: String, val intro: String, val credits: List<Credit>)

val CREDITS: List<CreditGroup> = listOf(
    CreditGroup("Fonts", "Every font is bundled with the app under an open font license (SIL Open Font License or GUST Font License), whose text is in the app's source.", listOf(
        Credit("Google Sans Flex", "Google", "Keys, labels, menus and all other text",
            "A variable font; the app always uses its fully rounded setting (ROND 100).", "SIL Open Font License 1.1", "https://fonts.google.com/specimen/Google+Sans+Flex"),
        Credit("Roboto", "Christian Robertson, Google", "Characters Google Sans Flex doesn't have",
            "Android's own typeface, used as the fallback.", "SIL Open Font License 1.1", "https://fonts.google.com/specimen/Roboto"),
        Credit("MathJax TeX fonts", "The MathJax Consortium, after Donald Knuth's Computer Modern", "All the maths: Main, Math Italic, Caligraphic, Fraktur and Size2 (the large ∫ and ∮)",
            "The fonts LaTeX documents are set in, so answers look as they would in print.", "SIL Open Font License 1.1", "https://github.com/mathjax/MathJax/tree/legacy-v2/fonts"),
        Credit("New Computer Modern", "Antonis Tsolomitis, after Donald Knuth's Computer Modern", "Blackboard bold (ℝ, ℂ…), Hebrew letters, upright Greek, ħ and the Greek letters Computer Modern lacks",
            "Computer Modern extended with Unicode maths and more scripts, so these match the rest of the maths.", "GUST Font License", "https://ctan.org/pkg/newcomputermodern"),
        Credit("Material Symbols", "Google", "Icons",
            "Google's icon set, through Compose's extended icons.", "Apache License 2.0", "https://fonts.google.com/icons"),
    )),
    CreditGroup("Libraries", "Open-source code the app is built with.", listOf(
        Credit("Kotlin", "JetBrains", "The language the whole app is written in",
            "The computer algebra, the graphs and the interface are all plain Kotlin.", "Apache License 2.0", "https://kotlinlang.org"),
        Credit("Jetpack Compose", "Google", "The interface",
            "Android's toolkit for drawing screens from code; the maths renderer is built on its layouts.", "Apache License 2.0", "https://developer.android.com/compose"),
        Credit("Material 3 Expressive", "Google", "Components, colors and springy motion",
            "material3 1.4.0-alpha18, the last release with the expressive API that builds on SDK 36.", "Apache License 2.0", "https://m3.material.io"),
        Credit("AndroidX (Activity, Lifecycle, Core)", "Google", "Saving files, view models, sharing",
            "Android's support libraries.", "Apache License 2.0", "https://developer.android.com/jetpack/androidx"),
    )),
    CreditGroup("Data", "Numbers the app looks up rather than works out.", listOf(
        Credit("CODATA 2022 recommended values", "NIST and the CODATA Task Group on Fundamental Constants", "Every physical constant and its unit",
            "The internationally agreed values, updated every four years; exact ones are fixed by the 2019 SI.", "Public domain (US government work)", "https://physics.nist.gov/cuu/Constants/"),
        Credit("matplotlib colormaps", "Nathaniel Smith and Stéfan van der Walt (viridis, plasma, magma), Jamie Nuñez et al. (cividis), Bastian Bechtold (twilight), Anton Mikhailov (turbo)", "The colors of the complex plane",
            "Every map on matplotlib's Choosing Colormaps page (86 of them), sampled at 256 points.", "CC0 (viridis family), BSD (matplotlib), Apache 2.0 (turbo)", "https://matplotlib.org/stable/users/explain/colors/colormaps.html"),
    )),
    CreditGroup("Numerical methods", "Published methods behind the answers that aren't exact.", listOf(
        Credit("Gauss–Kronrod quadrature", "Carl Friedrich Gauss, Aleksandr Kronrod", "Definite integrals without a closed form",
            "Fits a polynomial through carefully chosen points, and estimates its own error.", "Published method", "https://en.wikipedia.org/wiki/Gauss%E2%80%93Kronrod_quadrature_formula"),
        Credit("Durand–Kerner method", "Émile Durand, Immo Kerner", "Roots of polynomials of degree 3 and up that don't factor",
            "Finds all the roots at once, complex ones included.", "Published method", "https://en.wikipedia.org/wiki/Durand%E2%80%93Kerner_method"),
        Credit("Lanczos approximation", "Cornelius Lanczos", "The gamma function and factorials of non-integers",
            "A short series that gives Γ(z) to about 15 digits.", "Published method", "https://en.wikipedia.org/wiki/Lanczos_approximation"),
        Credit("Borwein's algorithm", "Peter Borwein", "The Riemann zeta function",
            "A fast-converging series for ζ(s), with the functional equation for Re s < 1/2.", "Published method", "https://en.wikipedia.org/wiki/Riemann_zeta_function"),
        Credit("Lentz's continued fraction", "William J. Lentz", "The error function far from 0 (and so the normal distribution)",
            "Evaluates continued fractions from the top down.", "Published method", "https://en.wikipedia.org/wiki/Error_function"),
        Credit("Acklam's inverse normal", "Peter John Acklam", "Φ⁻¹, refined by Newton's method",
            "A rational approximation to the normal quantile.", "Published method", "https://en.wikipedia.org/wiki/Normal_distribution"),
        Credit("Newton's method", "Isaac Newton, Joseph Raphson", "Lambert W, solving equations numerically",
            "Follows the tangent to a root, doubling the correct digits each step.", "Published method", "https://en.wikipedia.org/wiki/Newton%27s_method"),
        Credit("Levenberg–Marquardt", "Kenneth Levenberg, Donald Marquardt", "Fit: least squares through a list of points",
            "Between Gauss–Newton and gradient descent, damped when a step doesn't help.", "Published method", "https://en.wikipedia.org/wiki/Levenberg%E2%80%93Marquardt_algorithm"),
    )),
    CreditGroup("Graphics", "How the graphs are drawn.", listOf(
        Credit("Marching squares", "After Lorensen and Cline's marching cubes", "Equations in x and y, and the edges of inequalities",
            "Finds where a function changes sign across a grid, cell by cell.", "Published method", "https://en.wikipedia.org/wiki/Marching_squares"),
        Credit("Marching tetrahedra", "Akio Doi, Akio Koide", "Implicit surfaces in 3D",
            "The 3D version: cuts space into tetrahedra and finds the surface in each.", "Published method", "https://en.wikipedia.org/wiki/Marching_tetrahedra"),
        Credit("Painter's algorithm", "—", "Drawing 3D surfaces",
            "Draws faces from the farthest to the nearest, so near ones cover far ones.", "Published method", "https://en.wikipedia.org/wiki/Painter%27s_algorithm"),
        Credit("Domain coloring", "Frank Farris; samuelj.li's complex function plotter", "The complex plane",
            "Color shows the argument of f(z), brightness its size.", "Published method", "https://en.wikipedia.org/wiki/Domain_coloring"),
        Credit("OKLab", "Björn Ottosson", "The color picker's OKLab mode, and app colors grown from yours",
            "A color space where equal steps look equally different.", "MIT (reference code)", "https://bottosson.github.io/posts/oklab/"),
        Credit("Material tonal palettes", "Google (Material Color Utilities)", "The app's colors when Material You is off",
            "Tones of one hue at fixed lightness steps, so text always stands out; rebuilt here in OKLab.", "Apache License 2.0 (the recipe)", "https://m3.material.io/styles/color/system/how-the-system-works"),
    )),
    CreditGroup("Inspiration", "Apps whose ideas this one borrows.", listOf(
        Credit("Desmos", "Desmos Studio", "Graphs: typing any line, restrictions, points, sliders and lists",
            "The graphing calculator many people learn with.", "—", "https://www.desmos.com/calculator"),
        Credit("Google Calculator", "Google", "The look, the keypad, and continuing from an answer",
            "The calculator on Pixel phones.", "—", "https://play.google.com/store/apps/details?id=com.google.android.calculator"),
        Credit("TI-Nspire CAS and HP Prime", "Texas Instruments, HP", "A 2D maths editor with exact answers",
            "Handheld computer algebra calculators.", "—", "https://en.wikipedia.org/wiki/Computer_algebra_system"),
    )),
)

/** The acknowledgements: each group with a line on what it is, then a card per credit that opens its link. */
@Composable
fun AcknowledgementsDialog(onDismiss: () -> Unit) {
    val tablet = isTabletLayout()
    // One page on a phone; on a tablet the groups are listed on the left and their cards sit two across.
    SectionedPage(
        "Acknowledgements",
        onBack = onDismiss,
        intro = "CAS Calculator is built on the work of many people. Tap any entry to read more.",
        sections = CREDITS.map { group ->
            PageSection(group.title, iconFor(group.title)) {
                Text(group.intro, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (tablet) {
                    group.credits.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            pair.forEach { c -> CreditCard(c, Modifier.weight(1f).fillMaxHeight()) }
                            if (pair.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        }
                    }
                } else group.credits.forEach { c -> CreditCard(c, Modifier.fillMaxWidth()) }
            }
        },
    )
}

private fun iconFor(group: String): androidx.compose.ui.graphics.vector.ImageVector = when (group) {
    "Fonts" -> Icons.Outlined.FontDownload
    "Libraries" -> Icons.AutoMirrored.Outlined.LibraryBooks
    "Data" -> Icons.Outlined.Dataset
    "Numerical methods" -> Icons.Outlined.Functions
    "Graphics" -> Icons.AutoMirrored.Outlined.ShowChart
    "Inspiration" -> Icons.Outlined.Lightbulb
    else -> Icons.Outlined.Info
}

/** One credit: name, who made it, what it's used for, a line about it and its licence. Tap to open its page. */
@Composable
private fun CreditCard(c: Credit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val uri = LocalUriHandler.current
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceContainer)
            .clickable(onClickLabel = "Open ${c.name}") { runCatching { uri.openUri(c.url) } }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.name, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
        if (c.by != "—") Text(c.by, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        Text("Used for: " + c.use, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
        Text(c.about, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        if (c.licence != "—") {
            Text(
                c.licence,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSecondaryContainer,
                modifier = Modifier.padding(top = 4.dp).clip(CircleShape).background(colors.secondaryContainer).padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}
