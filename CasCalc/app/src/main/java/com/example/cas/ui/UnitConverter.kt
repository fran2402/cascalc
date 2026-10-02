package com.example.cas.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cas.engine.Units
import com.example.cas.ui.theme.CasFonts

/** Ready-made conversions to try: value, from, to, and what it shows. */
private val EXAMPLES = listOf(
    listOf("70", "km/s/Mpc", "1/s", "Hubble constant"),
    listOf("1", "erg", "SI", "erg in SI"),
    listOf("1", "eV", "K", "eV as a temperature"),
    listOf("500", "nm", "eV", "Photon energy"),
    listOf("1", "Msun", "kg", "Solar mass"),
    listOf("1", "Jy", "W m^-2 Hz^-1", "Jansky"),
    listOf("1", "m_p", "MeV", "Proton rest energy"),
    listOf("21", "cm", "GHz", "The 21 cm line"),
    listOf("1", "G", "T", "Gauss"),
    listOf("98.6", "°F", "°C", "Body temperature"),
    listOf("60", "mph", "km/h", "Speed"),
    listOf("1", "ly", "pc", "Light-year"),
)

private enum class Field { Value, From, To }

/**
 * The unit converter (beta): a value and its unit, then the unit to convert to, with the
 * answer large in LaTeX. Units are typed as expressions (km/s/Mpc, kg m² s⁻², W/(m² sr Hz))
 * or picked from the catalog; "SI", "base" and "cgs" write the answer in that system. When
 * dimensions differ, c, h (or ħ) and k_B can bridge them, as in eV ↔ K or nm ↔ eV. On a
 * phone it's one column; on a wide screen the inputs sit beside the results.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterPage(onBack: () -> Unit, onUse: ((Double) -> Unit)? = null) {
    val saved = remember { AppSettings.converterState.split('\n').let { if (it.size == 3) it else listOf("1", "km/s/Mpc", "1/s") } }
    var value by rememberSaveable { mutableStateOf(saved[0]) }
    var from by rememberSaveable { mutableStateOf(saved[1]) }
    var to by rememberSaveable { mutableStateOf(saved[2]) }
    var digits by rememberSaveable { mutableIntStateOf(6) }
    var useC by rememberSaveable { mutableStateOf(true) }
    var useH by rememberSaveable { mutableStateOf(true) }
    var useHbar by rememberSaveable { mutableStateOf(false) }
    var useKB by rememberSaveable { mutableStateOf(true) }
    var focused by rememberSaveable { mutableStateOf(Field.From) }
    var browsing by remember { mutableStateOf(false) }
    var spin by remember { mutableFloatStateOf(0f) }

    val close = { AppSettings.changeConverterState(value, from, to); onBack() }
    val state = remember(value, from, to, useC, useH, useHbar, useKB) {
        ConverterState.compute(value, from, to, Units.Bridges(c = useC, h = useH, hbar = useHbar, kB = useKB))
    }
    val swap = {
        // The answer's number becomes the new value, so swapping twice comes back.
        state.result?.let { if (state.toText.lowercase() !in setOf("si", "base", "cgs")) value = Units.plain(it.value, 10) }
        val t = to.trim()
        if (t.lowercase() in setOf("si", "base", "cgs")) state.result?.let { to = from; from = plainUnit(it.to) } else { to = from; from = t }
        spin += 180f
    }
    fun insert(symbol: String) {
        val target = if (focused == Field.To) to else from
        val joined = if (target.isBlank() || target.last() !in "/*( ") symbol else target + symbol
        if (focused == Field.To) to = joined else from = joined
    }

    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler(onBack = close)
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Header(onBack = close, onBrowse = { browsing = true })
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val wide = maxWidth >= 720.dp
                    val inputs: @Composable ColumnScope.() -> Unit = {
                        FromCard(value, { value = it }, from, { from = it }, state, onFocus = { focused = it })
                        SwapButton(spin, onSwap = swap)
                        ToCard(to, { to = it }, state, digits, onFocus = { focused = it }, onUse = onUse?.let { use -> { v: Double -> AppSettings.changeConverterState(value, from, to); use(v) } })
                        Suggestions(if (focused == Field.To) to else from, visible = focused != Field.Value) { token, symbol ->
                            if (focused == Field.To) to = to.dropLast(token.length) + symbol else from = from.dropLast(token.length) + symbol
                        }
                    }
                    val results: @Composable ColumnScope.() -> Unit = {
                        state.bridge?.let { Notice(Icons.Outlined.Info, it.describe(), MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer) }
                        state.error?.let { Notice(Icons.Outlined.ErrorOutline, it, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer) }
                        if (state.alternatives.isNotEmpty()) AlsoCard(state, digits) { to = it }
                        DetailsCard(state, digits)
                        ConstantsCard(useC, { useC = it }, useH, { useH = it; if (it) useHbar = false }, useHbar, { useHbar = it; if (it) useH = false }, useKB, { useKB = it })
                        DigitsCard(digits) { digits = it }
                    }
                    val examples: @Composable ColumnScope.() -> Unit = {
                        Examples { ex -> value = ex[0]; from = ex[1]; to = ex[2] }
                    }
                    if (wide) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                inputs()
                                examples()
                            }
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = results)
                        }
                    } else {
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            inputs()
                            results()
                            examples()
                        }
                    }
                }
            }
        }
        if (browsing) UnitCatalog(onPick = { insert(it); browsing = false }, onDismiss = { browsing = false }, into = if (focused == Field.To) "To" else "From")
    }
}

/** What the converter shows for the current inputs. */
private class ConverterState(
    val toText: String,
    val fromQ: Units.Quantity?,
    val fromError: String?,
    val toQ: Units.Quantity?,
    val toError: String?,
    val result: Units.Result?,
    val error: String?,
    val si: Double?,
) {
    val bridge get() = result?.bridge
    val alternatives: List<Units.Alternative> get() {
        val r = result ?: return emptyList()
        val siOut = r.to.toSI(r.value)
        return Units.alternatives(siOut, r.to.dims, exclude = r.to.parts.singleOrNull()?.takeIf { it.prefix == null }?.let { setOf(it.unit.symbol) } ?: emptySet())
    }

    companion object {
        fun compute(valueText: String, from: String, to: String, bridges: Units.Bridges): ConverterState {
            val v = parseValue(valueText)
            val (fq, fe) = runCatching { Units.parse(from) }.fold({ it to null }, { null to (it.message ?: "Can't read that unit") })
            val (tq, te) = if (fq == null) (null to null) else runCatching { Units.target(to, fq.dims) }.fold({ it to null }, { null to (it.message ?: "Can't read that unit") })
            var error: String? = null
            val result = if (fq != null && tq != null && v != null) runCatching { Units.convert(v, fq, tq, bridges) }.getOrElse { error = it.message; null } else null
            if (v == null && valueText.isNotBlank()) error = "That value isn't a number"
            return ConverterState(to, fq, fe, tq, te, result, error, if (fq != null && v != null) fq.toSI(v) else null)
        }
    }
}

/** A typed value: 3e8, 3×10^8, 3·10⁸, −2.5 or 1,5. */
private fun parseValue(text: String): Double? {
    var t = text.trim().replace('−', '-').replace(',', '.').replace(" ", "")
    val sup = mapOf('⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4', '⁵' to '5', '⁶' to '6', '⁷' to '7', '⁸' to '8', '⁹' to '9', '⁻' to '-')
    t = Regex("""[×·*x]10\^?([⁰¹²³⁴⁵⁶⁷⁸⁹⁻\-+]*\d*)""").replace(t) { m -> "e" + m.groupValues[1].map { sup[it] ?: it }.joinToString("") }
    return t.toDoubleOrNull()
}

/** A unit expression as typed: kg m^2 s^-2. */
private fun plainUnit(q: Units.Quantity): String = q.parts.joinToString(" ") { p ->
    (p.prefix?.symbol ?: "") + p.unit.symbol + if (p.power == 1.0) "" else "^" + (if (p.power == Math.rint(p.power)) p.power.toInt().toString() else p.power.toString())
}

@Composable
private fun Header(onBack: () -> Unit, onBrowse: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.onSurface) }
        Text("Unit converter", style = MaterialTheme.typography.headlineSmall, color = colors.onSurface, modifier = Modifier.padding(start = 4.dp).semantics { heading() })
        Spacer(Modifier.width(10.dp))
        BetaBadge()
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onBrowse) { Icon(Icons.Outlined.Category, contentDescription = "Browse units", tint = colors.onSurfaceVariant) }
    }
}

/** The value and its unit, in a large rounded card. */
@Composable
private fun FromCard(value: String, onValue: (String) -> Unit, unit: String, onUnit: (String) -> Unit, state: ConverterState, onFocus: (Field) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(colors.surfaceContainerHigh).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("From", style = MaterialTheme.typography.labelLarge, color = colors.primary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value, onValue,
                textStyle = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 40.sp, color = colors.onSurface),
                singleLine = true,
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onFocus(Field.Value) }.semantics { contentDescription = "Value" },
            )
            // Keyboards for numbers often lack e and minus: these add them.
            SmallPill("\$\\times 10^{n}\$", "Times a power of ten") { onValue(if ('e' in value) value else value + "e") }
            Spacer(Modifier.width(6.dp))
            SmallPill("\$\\pm\$", "Change the sign") { onValue(if (value.startsWith("-")) value.drop(1) else "-$value") }
        }
        UnitField(unit, onUnit, "Unit, like km/s/Mpc", onFocus = { onFocus(Field.From) })
        UnitPreview(state.fromQ, state.fromError)
    }
}

/** The unit to convert to, the systems, and the answer, large, in the primary container. */
@Composable
private fun ToCard(unit: String, onUnit: (String) -> Unit, state: ConverterState, digits: Int, onFocus: (Field) -> Unit, onUse: ((Double) -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    @Suppress("DEPRECATION") val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(colors.primaryContainer).animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("To", style = MaterialTheme.typography.labelLarge, color = colors.onPrimaryContainer)
        UnitField(unit, onUnit, "Unit, or SI, base, cgs", onFocus = { onFocus(Field.To) }, container = colors.surfaceContainerLowest.copy(alpha = 0.6f))
        // The systems, as a connected button group.
        val systems = listOf("SI", "base", "cgs")
        val chosen = systems.indexOfFirst { it.equals(unit.trim(), ignoreCase = true) }
        ConnectedButtonGroup(systems.size, selected = chosen, onSelect = { onUnit(systems[it]) }, description = { "In ${systems[it]} units" }) { k, on ->
            Text(listOf("SI", "SI base", "CGS")[k], style = MaterialTheme.typography.labelLarge, color = if (on) colors.onPrimary else colors.onSurface)
        }
        UnitPreview(state.toQ, state.toError, onContainer = true)
        val r = state.result
        AnimatedVisibility(r != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            if (r != null) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    MathText(
                        "\$= ${Units.number(r.value, digits)}\\;${r.to.latex().takeIf { it != "1" } ?: ""}\$",
                        style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 30.sp),
                        color = colors.onPrimaryContainer,
                        mathScale = 1f,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionPill(Icons.Default.ContentCopy, "Copy") {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(Units.plain(r.value, digits) + " " + plainUnit(r.to)))
                        android.widget.Toast.makeText(context, "Copied", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    if (onUse != null) ActionPill(Icons.AutoMirrored.Filled.KeyboardReturn, "Use in calculator") { onUse(r.value) }
                }
            }
        }
    }
}

/** A unit expression field: rounded, filled, monospace-free; the parsed unit shows under it in LaTeX. */
@Composable
private fun UnitField(text: String, onText: (String) -> Unit, hint: String, onFocus: () -> Unit, container: Color = MaterialTheme.colorScheme.surfaceContainerHighest) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(18.dp)).background(container).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
        if (text.isEmpty()) Text(hint, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        BasicTextField(
            text, onText,
            textStyle = MaterialTheme.typography.titleMedium.copy(color = colors.onSurface),
            singleLine = true,
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
            modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) onFocus() }.semantics { contentDescription = hint },
        )
    }
}

/** The unit as read, in LaTeX, with what it measures; or why it can't be read. */
@Composable
private fun UnitPreview(q: Units.Quantity?, error: String?, onContainer: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val ink = if (onContainer) colors.onPrimaryContainer else colors.onSurfaceVariant
    when {
        error != null -> MathText(error, style = MaterialTheme.typography.bodySmall, color = colors.error)
        q != null && q.parts.isNotEmpty() -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState())) {
                MathText("\$${q.latex()}\$", style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 18.sp), color = ink, mathScale = 1f)
            }
            Units.quantityName(q.dims)?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = if (onContainer) colors.onSecondaryContainer else colors.onSecondaryContainer,
                    modifier = Modifier.clip(CircleShape).background(colors.secondaryContainer).padding(horizontal = 10.dp, vertical = 3.dp))
            }
        }
    }
}

/** The swap button between the cards: an Expressive cookie shape that turns half a turn on each swap. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SwapButton(spin: Float, onSwap: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tap = rememberKeyTap()
    val turn by animateFloatAsState(spin, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow), label = "swap")
    val shape = remember { CookieShape(9) }
    Box(Modifier.fillMaxWidth().height(0.dp).zIndex(1f), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .offset(y = 0.dp)
                .size(60.dp)
                .rotate(turn)
                .clip(shape)
                .background(colors.tertiary)
                .clickable(onClickLabel = "Swap the units") { tap(); onSwap() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.SwapVert, contentDescription = "Swap", tint = colors.onTertiary, modifier = Modifier.size(28.dp).rotate(-turn))
        }
    }
}

/** Material 3 Expressive's cookie: a circle with [lobes] soft scallops around its edge. */
private class CookieShape(val lobes: Int) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
        val path = androidx.compose.ui.graphics.Path()
        val cx = size.width / 2; val cy = size.height / 2
        val r = minOf(cx, cy)
        val steps = lobes * 24
        for (k in 0..steps) {
            val a = 2 * Math.PI * k / steps - Math.PI / 2
            // Radius dips slightly between lobes: 1 at a lobe, 0.88 between.
            val rr = r * (0.94 + 0.06 * kotlin.math.cos(lobes * (a + Math.PI / 2)))
            val x = (cx + rr * kotlin.math.cos(a)).toFloat(); val y = (cy + rr * kotlin.math.sin(a)).toFloat()
            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

/** Units starting with what's being typed, as chips: tap one to finish the word. */
@Composable
private fun Suggestions(text: String, visible: Boolean, onPick: (token: String, symbol: String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val token = text.takeLastWhile { it !in " /*()^" }
    val list = remember(token) { if (token.isEmpty() || Units.lookup(token) != null) emptyList() else Units.suggest(token) }
    AnimatedVisibility(visible && list.isNotEmpty(), enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            list.forEach { u ->
                Row(
                    Modifier.height(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.secondaryContainer)
                        .clickable(onClickLabel = "Use ${u.name}") { onPick(token, if (u.symbol == "fl oz") "floz" else u.symbol) }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MathText("\$${u.latex}\$", style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 16.sp), color = colors.onSecondaryContainer, mathScale = 1f)
                    Spacer(Modifier.width(8.dp))
                    Text(u.name, style = MaterialTheme.typography.labelMedium, color = colors.onSecondaryContainer)
                }
            }
        }
    }
}

@Composable
private fun Notice(icon: ImageVector, text: String, container: Color, content: Color) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(container).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = content)
        Spacer(Modifier.width(12.dp))
        MathText(text, style = MaterialTheme.typography.bodyMedium, color = content)
    }
}

/** The same amount in other units of its kind; tap one to convert to it. */
@Composable
private fun AlsoCard(state: ConverterState, digits: Int, onPick: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Section("Also equals") {
        state.alternatives.forEachIndexed { k, a ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(if (k == 0) 20.dp else 8.dp)).background(colors.surfaceContainer)
                    .clickable(onClickLabel = "Convert to ${a.text}") { onPick(a.text) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                    MathText("\$${Units.number(a.value, digits)}\\;${a.latex}\$", style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 18.sp), color = colors.onSurface, mathScale = 1f)
                }
            }
        }
    }
}

/** The dimension, what it measures, and the value in SI base units. */
@Composable
private fun DetailsCard(state: ConverterState, digits: Int) {
    val colors = MaterialTheme.colorScheme
    val q = state.fromQ ?: return
    Section("Details") {
        val row = @Composable { label: String, math: String ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.width(110.dp))
                Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) { MathText(math, style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 17.sp), color = colors.onSurface, mathScale = 1f) }
            }
        }
        row("Dimension", "\$${q.dims.latex()}\$")
        Units.quantityName(q.dims)?.let { row("Measures", it) }
        state.si?.let { si ->
            val base = Units.inSystem(q.dims, "base")
            row("In SI base", "\$${Units.number(si / base.factor, digits)}\\;${base.latex().takeIf { it != "1" } ?: ""}\$")
        }
        row("Size", "\$1\\;${q.latex()} = ${Units.number(q.factor / Units.inSystem(q.dims, "base").factor, digits)}\\;${Units.inSystem(q.dims, "base").latex().takeIf { it != "1" } ?: ""}\$")
    }
}

/** Which constants may bridge different dimensions. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConstantsCard(c: Boolean, onC: (Boolean) -> Unit, h: Boolean, onH: (Boolean) -> Unit, hbar: Boolean, onHbar: (Boolean) -> Unit, kB: Boolean, onKB: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Section("Physical equivalences") {
        Text("When the dimensions differ, these constants may connect them: mass and energy, energy and frequency or wavelength, energy and temperature.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ToggleChip("\$c\$", "speed of light", c, onC)
            ToggleChip("\$h\$", "Planck", h, onH)
            ToggleChip("\$\\hbar\$", "reduced Planck", hbar, onHbar)
            ToggleChip("\$k_B\$", "Boltzmann", kB, onKB)
        }
    }
}

@Composable
private fun DigitsCard(digits: Int, onDigits: (Int) -> Unit) {
    Section("Significant figures: $digits") {
        ExpressiveSlider(
            value = digits.toFloat(),
            onValueChange = { onDigits(it.toInt()) },
            valueRange = 2f..15f,
            steps = 12,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Significant figures" },
        )
    }
}

/** Ready-made conversions, as chips. */
@Composable
private fun Examples(onPick: (List<String>) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Section("Try") {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EXAMPLES.forEach { ex ->
                Column(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerHighest)
                        .clickable(onClickLabel = "Try ${ex[3]}") { onPick(ex) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(ex[3], style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    val a = runCatching { Units.parse(ex[1]).latex() }.getOrDefault(ex[1])
                    val b = if (ex[2] == "SI") "\\mathrm{SI}" else runCatching { Units.parse(ex[2]).latex() }.getOrDefault(ex[2])
                    MathText("\$${ex[0]}\\;$a \\to $b\$", style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 16.sp), color = colors.onSurface, mathScale = 1f)
                }
            }
        }
    }
}

/** A titled group in a rounded card. */
@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(colors.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = colors.primary, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp).semantics { heading() })
        content()
    }
}

@Composable
private fun SmallPill(label: String, spoken: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.height(40.dp).clip(RoundedCornerShape(14.dp)).background(colors.secondaryContainer).clickable(onClickLabel = spoken, onClick = onClick).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { MathText(label, style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 17.sp), color = colors.onSecondaryContainer, mathScale = 1f) }
}

@Composable
private fun ActionPill(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tap = rememberKeyTap()
    Row(
        Modifier.height(40.dp).clip(CircleShape).background(colors.surfaceContainerLowest.copy(alpha = 0.7f)).clickable(onClickLabel = label) { tap(); onClick() }.padding(start = 14.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = colors.onPrimaryContainer)
    }
}

/** A filter chip whose label is math: filled and round when on, outlined and squarer when off. */
@Composable
private fun ToggleChip(math: String, name: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val corner by animateFloatAsState(if (on) 50f else 25f, spring(stiffness = Spring.StiffnessMedium), label = "chip")
    Row(
        Modifier.height(40.dp).clip(RoundedCornerShape(corner.toInt())).background(if (on) colors.secondaryContainer else colors.surfaceContainerHigh)
            .clickable(onClickLabel = (if (on) "Don't use " else "Use ") + name) { onChange(!on) }
            .padding(horizontal = 14.dp)
            .semantics { contentDescription = name + if (on) ", on" else ", off" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MathText(math, style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 17.sp), color = if (on) colors.onSecondaryContainer else colors.onSurfaceVariant, mathScale = 1f)
        Spacer(Modifier.width(6.dp))
        Text(name, style = MaterialTheme.typography.labelMedium, color = if (on) colors.onSecondaryContainer else colors.onSurfaceVariant)
    }
}

/** Every unit, by category: tap one to put it in the field being edited. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun UnitCatalog(onPick: (String) -> Unit, onDismiss: () -> Unit, into: String) {
    val colors = MaterialTheme.colorScheme
    var category by rememberSaveable { mutableStateOf(Units.CATEGORIES.first()) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("Units", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            Text("Tap one to put it in “$into”. Every unit that takes SI prefixes is marked.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Units.CATEGORIES.forEach { c ->
                    val on = c == category
                    Box(
                        Modifier.height(36.dp).clip(if (on) CircleShape else RoundedCornerShape(10.dp)).background(if (on) colors.primary else colors.surfaceContainerHighest)
                            .clickable(onClickLabel = "Show $c") { category = c }.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(c, style = MaterialTheme.typography.labelLarge, color = if (on) colors.onPrimary else colors.onSurface) }
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(Units.ALL.filter { it.category == category }, key = { it.symbol }) { u ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainer)
                            .clickable(onClickLabel = "Use ${u.name}") { onPick(if (u.symbol == "fl oz") "floz" else u.symbol) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.widthIn(min = 64.dp)) { MathText("\$${u.latex}\$", style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 20.sp), color = colors.onSurface, mathScale = 1f) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(u.name, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                            Text(
                                (listOf(u.symbol) + u.aliases.take(2)).joinToString(" · ") + if (u.prefixes) " · takes prefixes" else "",
                                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
