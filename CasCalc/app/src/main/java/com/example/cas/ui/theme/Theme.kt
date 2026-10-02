package com.example.cas.ui.theme

import android.content.Context
import android.graphics.Paint
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.core.content.res.ResourcesCompat
import com.example.cas.R

// Olive fallback palettes, close to Google Calculator's; used before Android 12
// (on 12+ the colors come from the wallpaper, like every Pixel app).
private val DarkFallback = darkColorScheme(
    primary = Color(0xFFC5CB86), onPrimary = Color(0xFF2F3300),
    primaryContainer = Color(0xFF525727), onPrimaryContainer = Color(0xFFE6E9A8),
    secondary = Color(0xFFC7C9A8), onSecondary = Color(0xFF2F3219),
    secondaryContainer = Color(0xFF3D3F28), onSecondaryContainer = Color(0xFFE0E1C4),
    tertiary = Color(0xFFFFE3C0), onTertiary = Color(0xFF4A3610),
    tertiaryContainer = Color(0xFF5F4A22), onTertiaryContainer = Color(0xFFFFDDB6),
    background = Color(0xFF12130C), onBackground = Color(0xFFE5E3D6),
    surface = Color(0xFF12130C), onSurface = Color(0xFFE5E3D6),
    surfaceVariant = Color(0xFF47473B), onSurfaceVariant = Color(0xFFC8C7B5),
    surfaceContainerLowest = Color(0xFF0D0E08), surfaceContainerLow = Color(0xFF1B1C14),
    surfaceContainer = Color(0xFF1F2018), surfaceContainerHigh = Color(0xFF292A22),
    surfaceContainerHighest = Color(0xFF34352C),
    outline = Color(0xFF929181), outlineVariant = Color(0xFF47473B),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
)

private val LightFallback = lightColorScheme(
    primary = Color(0xFF5B6133), onPrimary = Color.White,
    primaryContainer = Color(0xFFDEE5A0), onPrimaryContainer = Color(0xFF1A1E00),
    secondary = Color(0xFF5E6044), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E4C4), onSecondaryContainer = Color(0xFF1B1D0A),
    tertiary = Color(0xFF7A5A2C), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB6), onTertiaryContainer = Color(0xFF2A1800),
    background = Color(0xFFFCF9EE), onBackground = Color(0xFF1C1C16),
    surface = Color(0xFFFCF9EE), onSurface = Color(0xFF1C1C16),
    surfaceVariant = Color(0xFFE5E3D1), onSurfaceVariant = Color(0xFF47473B),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF6F4E8),
    surfaceContainer = Color(0xFFF1EEE2), surfaceContainerHigh = Color(0xFFEBE9DC),
    surfaceContainerHighest = Color(0xFFE5E3D6),
    outline = Color(0xFF787767), outlineVariant = Color(0xFFC8C7B5),
    error = Color(0xFFBA1A1A), onError = Color.White,
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CasTheme(content: @Composable () -> Unit) {
    val dark = when (com.example.cas.ui.AppSettings.theme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val context = LocalContext.current
    val colors: ColorScheme = remember(context, dark, com.example.cas.ui.AppSettings.dynamicColor, com.example.cas.ui.AppSettings.themeColor) {
        appColorScheme(context, dark)
    }
    val glyphs = remember(context) { GlyphFallback(context.applicationContext) }
    val mathGlyphs = remember(context) { MathGlyphs(context.applicationContext) }
    CompositionLocalProvider(LocalGlyphFallback provides glyphs, LocalMathGlyphs provides mathGlyphs) {
        // M3 Expressive: springier motion for every component that animates.
        androidx.compose.material3.MaterialExpressiveTheme(
            colorScheme = colors,
            motionScheme = if (com.example.cas.ui.AppSettings.expressiveMotion) androidx.compose.material3.MotionScheme.expressive()
            else androidx.compose.material3.MotionScheme.standard(),
            typography = RoundedTypography,
            content = content,
        )
    }
}

/**
 * The app's colors, light or dark: the wallpaper's (Material You, Android 12+) when that's on,
 * otherwise grown from the chosen app color, or the built-in olive. Also used to export a graph
 * in the other theme.
 */
fun appColorScheme(context: Context, dark: Boolean): ColorScheme = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && com.example.cas.ui.AppSettings.dynamicColor ->
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    // Your own color, or olive by default.
    com.example.cas.ui.AppSettings.themeColor != 0 -> schemeOf(com.example.cas.ui.TonalScheme.from(com.example.cas.ui.AppSettings.themeColor, dark), dark)
    dark -> DarkFallback
    else -> LightFallback
}

/** A generated scheme as Material's; the roles it doesn't set keep Material's defaults. */
private fun schemeOf(r: com.example.cas.ui.TonalScheme.Roles, dark: Boolean): ColorScheme {
    val base = if (dark) DarkFallback else LightFallback
    return base.copy(
        primary = Color(r.primary), onPrimary = Color(r.onPrimary),
        primaryContainer = Color(r.primaryContainer), onPrimaryContainer = Color(r.onPrimaryContainer),
        secondary = Color(r.secondary), onSecondary = Color(r.onSecondary),
        secondaryContainer = Color(r.secondaryContainer), onSecondaryContainer = Color(r.onSecondaryContainer),
        tertiary = Color(r.tertiary), onTertiary = Color(r.onTertiary),
        tertiaryContainer = Color(r.tertiaryContainer), onTertiaryContainer = Color(r.onTertiaryContainer),
        background = Color(r.background), onBackground = Color(r.onBackground),
        surface = Color(r.surface), onSurface = Color(r.onSurface),
        surfaceVariant = Color(r.surfaceVariant), onSurfaceVariant = Color(r.onSurfaceVariant),
        surfaceContainerLowest = Color(r.surfaceContainerLowest), surfaceContainerLow = Color(r.surfaceContainerLow),
        surfaceContainer = Color(r.surfaceContainer), surfaceContainerHigh = Color(r.surfaceContainerHigh),
        surfaceContainerHighest = Color(r.surfaceContainerHighest),
        surfaceBright = Color(if (dark) r.surfaceContainerHighest else r.surface), surfaceDim = Color(if (dark) r.surface else r.surfaceContainerHighest),
        surfaceTint = Color(r.primary),
        inverseSurface = Color(r.inverseSurface), inverseOnSurface = Color(r.inverseOnSurface), inversePrimary = Color(r.inversePrimary),
        outline = Color(r.outline), outlineVariant = Color(r.outlineVariant),
    )
}

/** The "=" key: bright peach in dark mode, soft peach in light mode, like Google Calculator. */
val ColorScheme.equalsKey: Pair<Color, Color>
    // Dark or light as the app is shown (the theme setting can differ from the system's).
    @Composable get() = if (surface.luminance() < 0.5f) tertiary to onTertiary else tertiaryContainer to onTertiaryContainer

@OptIn(ExperimentalTextApi::class)
/** Google Sans Flex, always fully rounded (ROND 100) — the only look used outside the math. */
fun googleSansFlex(weight: Int, width: Float = 100f, slant: Float = 0f): FontFamily =
    FontFamily(
        Font(
            resId = R.font.google_sans_flex,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight),
                FontVariation.width(width),
                FontVariation.slant(slant),
                FontVariation.Setting("ROND", 100f),
            ),
        ),
    )

@OptIn(ExperimentalTextApi::class)
fun roboto(weight: Int): FontFamily = FontFamily(
    Font(
        resId = R.font.roboto,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
    ),
)

/**
 * Material's type scale with every style in Google Sans Flex, fully rounded,
 * so dialogs, menus and buttons match the rest of the app. (Only math uses
 * Computer Modern.)
 */
val RoundedTypography: Typography by lazy {
    val base = Typography()
    fun TextStyle.rounded() = copy(fontFamily = googleSansFlex(weight = (fontWeight ?: FontWeight.Normal).weight))
    Typography(
        displayLarge = base.displayLarge.rounded(), displayMedium = base.displayMedium.rounded(), displaySmall = base.displaySmall.rounded(),
        headlineLarge = base.headlineLarge.rounded(), headlineMedium = base.headlineMedium.rounded(), headlineSmall = base.headlineSmall.rounded(),
        titleLarge = base.titleLarge.rounded(), titleMedium = base.titleMedium.rounded(), titleSmall = base.titleSmall.rounded(),
        bodyLarge = base.bodyLarge.rounded(), bodyMedium = base.bodyMedium.rounded(), bodySmall = base.bodySmall.rounded(),
        labelLarge = base.labelLarge.rounded(), labelMedium = base.labelMedium.rounded(), labelSmall = base.labelSmall.rounded(),
    )
}

object CasFonts {
    /** TeX's \mathcal and \mathfrak shapes (drawn on plain letters; see MathAlphabets). */
    val CmCal = FontFamily(Font(R.font.cm_cal))
    val CmFrak = FontFamily(Font(R.font.cm_frak))
    /** Math display: Computer Modern, as in LaTeX (MathJax's TeX fonts). */
    val CmRoman = FontFamily(Font(R.font.cm_main))
    val CmItalic = FontFamily(Font(R.font.cm_italic))
    /**
     * New Computer Modern (subsets): ℵ ℶ ℷ ℸ, blackboard bold and italic ϰ from its math font,
     * the Hebrew alphabet from its roman, and ħ (for ℏ) from its italic, which the older
     * Computer Modern fonts above don't have.
     */
    val NcmMath = FontFamily(Font(R.font.ncm_math))
    val NcmHebrew = FontFamily(Font(R.font.ncm_hebrew))
    val NcmItalic = FontFamily(Font(R.font.ncm_italic))
    /** New Computer Modern's roman for what Computer Modern lacks: Latin-1 and Latin Extended-A (Å, é, ø, µ), ‰, ℃, ℉, Å, ☉, ☽ ☾. */
    val NcmRoman = FontFamily(Font(R.font.ncm_roman))
    /** New Computer Modern's italic for the same Latin letters, in italic runs. */
    val NcmLatinItalic = FontFamily(Font(R.font.ncm_latin_italic))
    /** Math on keys: Google Sans Flex, rounded like the rest of the interface. */
    val Math = googleSansFlex(weight = 400)
    val MathItalic = googleSansFlex(weight = 400, slant = -10f)
    val Key = googleSansFlex(weight = 400)
    val KeyMedium = googleSansFlex(weight = 500)
    val Ui = googleSansFlex(weight = 500)
}

/**
 * Google Sans Flex first, Roboto for anything it doesn't have (π, α, Σ, ∫…),
 * checked character by character against the bundled font.
 */
class GlyphFallback(context: Context) {
    private val paint = Paint().apply { typeface = ResourcesCompat.getFont(context, R.font.google_sans_flex) }
    private val robotoPaint = Paint().apply { typeface = ResourcesCompat.getFont(context, R.font.roboto) }
    private val cache = HashMap<Int, Boolean>()
    private val robotoCache = HashMap<Int, Boolean>()
    private val families = HashMap<Int, FontFamily>()
    private val slanted = HashMap<Int, FontFamily>()

    fun has(codePoint: Int): Boolean = FontCoverage.covers(FontCoverage.GOOGLE_SANS_FLEX, codePoint)

    private fun robotoHas(codePoint: Int): Boolean = FontCoverage.covers(FontCoverage.ROBOTO, codePoint)

    /** Google Sans Flex, then Roboto, then Computer Modern (for → ∠ ℜ ℑ on key labels). */
    fun style(text: String, weight: Int = 400): AnnotatedString = buildAnnotatedString {
        val fallback = families.getOrPut(weight) { roboto(weight) }
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val chunk = text.substring(i, i + Character.charCount(cp))
            val alpha = com.example.cas.editor.MathAlphabets.decode(cp)
            val special = if (cp in 0x5D0..0x5EA || cp in 0x2135..0x2138 || alpha?.first == com.example.cas.editor.MathAlphabets.Style.DoubleStruck) newComputerModern(cp, false) else null
            when {
                // ℏ on keys and in the sans display: the Maltese ħ, in the key font's own weight, italic.
                cp == 0x210F -> withStyle(SpanStyle(fontFamily = slanted.getOrPut(weight) { googleSansFlex(weight, slant = -10f) })) { append("ħ") }
                special != null -> withStyle(SpanStyle(fontFamily = special.first)) { append(special.second) }
                // 𝒜, 𝔄, 𝔞…: TeX's Caligraphic or Fraktur shape on the plain letter.
                alpha != null && alpha.first != com.example.cas.editor.MathAlphabets.Style.DoubleStruck -> withStyle(SpanStyle(fontFamily = if (alpha.first == com.example.cas.editor.MathAlphabets.Style.Calligraphic) CasFonts.CmCal else CasFonts.CmFrak, fontStyle = androidx.compose.ui.text.font.FontStyle.Normal)) { append(alpha.second) }
                Character.isWhitespace(cp) || has(cp) -> append(chunk)
                robotoHas(cp) -> withStyle(SpanStyle(fontFamily = fallback)) { append(chunk) }
                else -> withStyle(SpanStyle(fontFamily = CasFonts.CmRoman)) { append(chunk) }
            }
            i += chunk.length
        }
    }
}

val LocalGlyphFallback = staticCompositionLocalOf<GlyphFallback> { error("Wrap the UI in CasTheme") }

/**
 * Character coverage for the Computer Modern math fonts. Letters that are
 * variables use the math italic; everything else the upright roman. A
 * character missing from the chosen one comes from the other (digits in an
 * italic run, Greek like π in a roman run, as TeX does), and anything in
 * neither from Roboto.
 */
class MathGlyphs(context: Context) {
    /** TeX's display-size large operators (∫, ∮), from MathJax_Size2. */
    val operators: android.graphics.Typeface? = ResourcesCompat.getFont(context, R.font.cm_size2)

    private val roman = Paint().apply { typeface = ResourcesCompat.getFont(context, R.font.cm_main) }
    private val italic = Paint().apply { typeface = ResourcesCompat.getFont(context, R.font.cm_italic) }
    private val cache = HashMap<Long, Boolean>()
    private val fallback = roboto(400)

    /** [which] 0 = Computer Modern roman, 1 = math italic: the fonts' own tables, not Paint.hasGlyph (see FontCoverage). */
    @Suppress("UNUSED_PARAMETER")
    private fun has(paint: Paint, which: Int, cp: Int): Boolean =
        FontCoverage.covers(if (which == 0) FontCoverage.CM_ROMAN else FontCoverage.CM_ITALIC, cp)

    fun style(text: String, italicRun: Boolean, upright: Boolean = false): AnnotatedString = buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val chunk = text.substring(i, i + Character.charCount(cp))
            val inMain = if (italicRun) has(italic, 1, cp) else has(roman, 0, cp)
            val inOther = if (italicRun) has(roman, 0, cp) else has(italic, 1, cp)
            val alpha = com.example.cas.editor.MathAlphabets.decode(cp)
            val special = newComputerModern(cp, italicRun)
            when {
                // Upright text (\text, \mathrm): lowercase Greek from New Computer Modern's upright set.
                upright && isLowerGreek(cp) -> withStyle(SpanStyle(fontFamily = CasFonts.NcmMath, fontStyle = androidx.compose.ui.text.font.FontStyle.Normal)) { append(chunk) }
                special != null -> withStyle(SpanStyle(fontFamily = special.first, fontStyle = androidx.compose.ui.text.font.FontStyle.Normal)) { append(special.second) }
                // 𝒜, 𝔄, 𝔞…: TeX's Caligraphic or Fraktur shape on the plain letter.
                alpha != null -> withStyle(SpanStyle(fontFamily = if (alpha.first == com.example.cas.editor.MathAlphabets.Style.Calligraphic) CasFonts.CmCal else CasFonts.CmFrak, fontStyle = androidx.compose.ui.text.font.FontStyle.Normal)) { append(alpha.second) }
                Character.isWhitespace(cp) || inMain -> append(chunk)
                inOther -> withStyle(SpanStyle(fontFamily = if (italicRun) CasFonts.CmRoman else CasFonts.CmItalic)) { append(chunk) }
                // Not in Computer Modern: New Computer Modern (the same design, extended) before Roboto.
                italicRun && FontCoverage.covers(FontCoverage.NCM_LATIN_ITALIC, cp) -> withStyle(SpanStyle(fontFamily = CasFonts.NcmLatinItalic, fontStyle = androidx.compose.ui.text.font.FontStyle.Normal)) { append(chunk) }
                FontCoverage.covers(FontCoverage.NCM_ROMAN, cp) -> withStyle(SpanStyle(fontFamily = CasFonts.NcmRoman, fontStyle = androidx.compose.ui.text.font.FontStyle.Normal)) { append(chunk) }
                else -> withStyle(SpanStyle(fontFamily = fallback)) { append(chunk) }
            }
            i += chunk.length
        }
    }
}

/** Lowercase Greek and its variants (ϑ ϕ ϖ ϱ ϵ), which Computer Modern has only in italic. */
internal fun isLowerGreek(cp: Int) = cp in 0x3B1..0x3C9 || cp == 0x3D1 || cp == 0x3D5 || cp == 0x3D6 || cp == 0x3F1 || cp == 0x3F5

/** Capital Greek letters shaped like Latin ones: TeX sets them as the upright Latin letter. */
private val LATIN_LOOKALIKES = mapOf(
    'Α' to 'A', 'Β' to 'B', 'Ε' to 'E', 'Ζ' to 'Z', 'Η' to 'H', 'Ι' to 'I', 'Κ' to 'K',
    'Μ' to 'M', 'Ν' to 'N', 'Ο' to 'O', 'Ρ' to 'P', 'Τ' to 'T', 'Χ' to 'X',
)

/**
 * Characters drawn from New Computer Modern, or as the Latin letter they look like: the font
 * and the text to draw, or null. ℏ is drawn as ħ, an italic h with a bar (as \hbar).
 */
internal fun newComputerModern(cp: Int, italic: Boolean): Pair<androidx.compose.ui.text.font.FontFamily, String>? = when {
    cp == 0x210F || cp == 0x127 -> CasFonts.NcmItalic to "ħ"
    cp in 0x5D0..0x5EA -> CasFonts.NcmHebrew to String(Character.toChars(cp))
    cp in 0x2135..0x2138 -> CasFonts.NcmMath to String(Character.toChars(cp))
    com.example.cas.editor.MathAlphabets.decode(cp)?.first == com.example.cas.editor.MathAlphabets.Style.DoubleStruck -> CasFonts.NcmMath to String(Character.toChars(cp))
    // ϰ: the math font's italic kappa variant (U+1D718).
    cp == 0x3F0 -> CasFonts.NcmMath to String(Character.toChars(0x1D718))
    cp < 0x10000 && cp.toChar() in LATIN_LOOKALIKES -> CasFonts.CmRoman to LATIN_LOOKALIKES.getValue(cp.toChar()).toString()
    else -> null
}

val LocalMathGlyphs = staticCompositionLocalOf<MathGlyphs> { error("Wrap the UI in CasTheme") }
