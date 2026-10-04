package com.example.cas.ui

import androidx.compose.ui.unit.em

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
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
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

private enum class Field { From, To }

/**
 * The unit converter: a value and its unit, then the unit to convert to, with the
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
    // The field whose unit the picker is building, or null when it's closed.
    var picking by remember { mutableStateOf<Field?>(null) }
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

    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler(onBack = close)
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Header(onBack = close)
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val wide = maxWidth >= 720.dp
                    val inputs: @Composable ColumnScope.() -> Unit = {
                        FromCard(value, { value = it }, state, onPick = { picking = Field.From })
                        SwapButton(spin, onSwap = swap)
                        val compatible = remember(state.fromQ?.dims, useC, useH, useHbar, useKB) {
                            state.fromQ?.let { Units.compatible(it.dims, Units.Bridges(c = useC, h = useH, hbar = useHbar, kB = useKB)) }.orEmpty()
                        }
                        ToCard(to, { to = it }, state, digits, compatible, onPick = { picking = Field.To }, onUse = onUse?.let { use -> { v: Double -> AppSettings.changeConverterState(value, from, to); use(v) } })
                        state.bridge?.let { Notice(TableIcons.Info, it.describe(), MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer) }
                        state.error?.let { Notice(Icons.Outlined.ErrorOutline, it, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer) }
                    }
                    val results: @Composable ColumnScope.() -> Unit = {
                        if (state.alternatives.isNotEmpty()) AlsoCard(state, digits) { to = it }
                        DetailsCard(state, digits)
                        OptionsCard(digits, { digits = it }, useC, { useC = it }, useH, { useH = it; if (it) useHbar = false }, useHbar, { useHbar = it; if (it) useH = false }, useKB, { useKB = it })
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
        picking?.let { field ->
            UnitPicker(
                initial = if (field == Field.To) to.takeUnless { it.trim().lowercase() in setOf("si", "base", "cgs") }.orEmpty() else from,
                title = if (field == Field.To) "Convert to" else "Convert from",
                onDone = { if (field == Field.To) to = it else from = it; picking = null },
                onDismiss = { picking = null },
            )
        }
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
            return ConverterState(to, fq, fe, tq, te, result, error, if (fq != null && v != null && v.isFinite()) fq.toSI(java.math.BigDecimal(v.toString())).round(java.math.MathContext(15)).toDouble() else null)
        }
    }
}

/** [x] (in units of [of], SI by default) in [base]'s units, worked out in exact decimals. */
private fun exactRatio(x: Double, base: Units.Quantity, of: Units.Quantity? = null): Double {
    val mc = java.math.MathContext.DECIMAL128
    val v = java.math.BigDecimal(x.toString()).let { if (of != null) it.multiply(of.exactFactor ?: Units.exact(of.factor), mc) else it }
    return v.divide(base.exactFactor ?: Units.exact(base.factor), mc).round(java.math.MathContext(15)).toDouble()
}

/** A typed value: 3e8, 3×10^8, 3·10⁸, −2.5 or 1,5. */
private fun parseValue(text: String): Double? {
    var t = text.trim().replace('−', '-').replace(',', '.').replace(" ", "")
    val sup = mapOf('⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4', '⁵' to '5', '⁶' to '6', '⁷' to '7', '⁸' to '8', '⁹' to '9', '⁻' to '-')
    t = Regex("""[×·*x]10\^?([⁰¹²³⁴⁵⁶⁷⁸⁹⁻\-+]*\d*)""").replace(t) { m -> "e" + m.groupValues[1].map { sup[it] ?: it }.joinToString("") }
    return t.toDoubleOrNull()
}

/**
 * The typed value as math: 3e-8 shows as 3 × 10 with −8 raised, and every minus is the true
 * minus sign (−), not a hyphen. What's stored stays plain, 3e-8.
 */
private object ScientificTransformation : androidx.compose.ui.text.input.VisualTransformation {
    private const val TIMES = " × 10"
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val raw = text.text
        val e = raw.indexOfFirst { it == 'e' || it == 'E' }
        val shown = androidx.compose.ui.text.buildAnnotatedString {
            append((if (e < 0) raw else raw.substring(0, e)).replace('-', '−'))
            if (e >= 0) {
                append(TIMES)
                pushStyle(androidx.compose.ui.text.SpanStyle(baselineShift = androidx.compose.ui.text.style.BaselineShift(0.45f), fontSize = 0.62.em))
                append(raw.substring(e + 1).replace('-', '−'))
                pop()
            }
        }
        val grow = TIMES.length - 1
        val map = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int) = if (e < 0 || offset <= e) offset else offset + grow
            override fun transformedToOriginal(offset: Int) = when {
                e < 0 || offset <= e -> offset
                offset < e + TIMES.length -> e + 1
                else -> offset - grow
            }.coerceIn(0, raw.length)
        }
        return androidx.compose.ui.text.input.TransformedText(shown, map)
    }
}

/** A unit expression as typed: kg m^2 s^-2. */
private fun plainUnit(q: Units.Quantity): String = q.parts.joinToString(" ") { p ->
    (p.prefix?.symbol ?: "") + p.unit.symbol + if (p.power == 1.0) "" else "^" + (if (p.power == Math.rint(p.power)) p.power.toInt().toString() else p.power.toString())
}

@Composable
private fun Header(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Back"), tint = colors.onSurface) }
        Text(tr("Unit converter"), style = MaterialTheme.typography.headlineSmall, color = colors.onSurface, modifier = Modifier.padding(start = 4.dp).semantics { heading() })
        Spacer(Modifier.weight(1f))
    }
}

/** The value and its unit: the number large, and under it the unit as one big button that opens the picker. */
@Composable
private fun FromCard(value: String, onValue: (String) -> Unit, state: ConverterState, onPick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(colors.surfaceContainerHigh).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(tr("From"), style = MaterialTheme.typography.labelLarge, color = colors.primary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(tr("Value"), style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 40.sp), color = colors.onSurfaceVariant.copy(alpha = 0.6f))
                BasicTextField(
                    value, onValue,
                    textStyle = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 40.sp, color = colors.onSurface),
                    singleLine = true,
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = ScientificTransformation,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = tr("Value") },
                )
            }
            // Keyboards for numbers often lack e and minus: these add them.
            SmallPill("\$\\times 10^{n}\$", "Times a power of ten") { onValue(if ('e' in value) value else value + "e") }
            Spacer(Modifier.width(6.dp))
            SmallPill("\$\\pm\$", "Change the sign") { onValue(if (value.startsWith("-")) value.drop(1) else "-$value") }
        }
        UnitButton(state.fromQ, state.fromError, caption = null, container = colors.surfaceContainerHighest, ink = colors.onSurface, onPick = onPick, label = "From unit")
    }
}

/**
 * The answer first, large, then the unit it's in (a big button that opens the picker) and one
 * row of quick targets: the unit systems, then units the From unit converts to.
 */
@Composable
private fun ToCard(unit: String, onUnit: (String) -> Unit, state: ConverterState, digits: Int, compatible: List<String>, onPick: () -> Unit, onUse: ((Double) -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    @Suppress("DEPRECATION") val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val system = SYSTEMS.firstOrNull { it.first.equals(unit.trim(), ignoreCase = true) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(colors.primaryContainer).animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(tr("To"), style = MaterialTheme.typography.labelLarge, color = colors.onPrimaryContainer)
        val r = state.result
        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).horizontalScroll(rememberScrollState()), contentAlignment = Alignment.CenterStart) {
            if (r != null) MathText(
                "\$${Units.number(r.value, digits)}\$",
                style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 40.sp),
                color = colors.onPrimaryContainer,
                mathScale = 1f,
                modifier = Modifier.semantics { contentDescription = "Result ${Units.plain(r.value, digits)} ${plainUnit(r.to)}" },
            ) else Text(tr("—"), style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 40.sp), color = colors.onPrimaryContainer.copy(alpha = 0.5f))
        }
        UnitButton(
            state.toQ, state.toError,
            caption = system?.let { "In ${it.second} units" },
            container = colors.surfaceContainerLowest.copy(alpha = 0.7f), ink = colors.onPrimaryContainer, onPick = onPick, label = "To unit",
            empty = if (state.fromQ == null) "Choose the From unit first" else "Choose a unit",
        )
        // One tap: the systems, then units the From unit converts to.
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            SYSTEMS.forEach { (key, name) ->
                QuickChip(on = system?.first == key, onClick = { onUnit(key) }, spoken = "In $name units") {
                    Text(name, style = MaterialTheme.typography.labelLarge, color = it)
                }
            }
            if (compatible.isNotEmpty()) Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(24.dp).background(colors.onPrimaryContainer.copy(alpha = 0.25f)))
            compatible.forEach { sym ->
                val latex = remember(sym) { runCatching { Units.parse(sym).latex() }.getOrDefault(sym) }
                QuickChip(on = sym == unit.trim(), onClick = { onUnit(sym) }, spoken = "Convert to $sym") {
                    MathText("\$$latex\$", style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 16.sp), color = it, mathScale = 1f)
                }
            }
        }
        AnimatedVisibility(r != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            if (r != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionPill(TableIcons.Copy, "Copy") {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(Units.plain(r.value, digits) + " " + plainUnit(r.to)))
                    android.widget.Toast.makeText(context, "Copied", android.widget.Toast.LENGTH_SHORT).show()
                }
                if (onUse != null) ActionPill(Icons.AutoMirrored.Filled.KeyboardReturn, "Use in calculator") { onUse(r.value) }
            }
        }
    }
}

/** The unit systems the answer can be written in: what's typed, and its name. */
private val SYSTEMS = listOf("SI" to "SI", "base" to "SI base", "cgs" to "CGS")

/**
 * A unit as one big tappable button: its symbol in LaTeX, what it measures under it, and a
 * chevron, so it's clear that tapping changes it. Errors show in place of the name.
 */
@Composable
private fun UnitButton(q: Units.Quantity?, error: String?, caption: String?, container: Color, ink: Color, onPick: () -> Unit, label: String, empty: String = "Choose a unit") {
    val colors = MaterialTheme.colorScheme
    val tap = rememberKeyTap()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 68.dp).clip(RoundedCornerShape(22.dp)).background(container)
            .clickable(onClickLabel = tr("Change the unit")) { tap(); onPick() }
            .semantics { contentDescription = label + ": " + (q?.let { plainUnit(it) } ?: "none") }
            .padding(start = 18.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (q != null && q.parts.isNotEmpty()) Box(Modifier.horizontalScroll(rememberScrollState())) {
                MathText("\$${q.latex()}\$", style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 24.sp), color = ink, mathScale = 1f)
            } else Text(empty, style = MaterialTheme.typography.titleMedium, color = ink.copy(alpha = 0.7f))
            when {
                error != null -> MathText(error, style = MaterialTheme.typography.bodySmall, color = colors.error)
                else -> listOfNotNull(caption, q?.let { Units.quantityName(it.dims) }).joinToString(" · ").takeIf { it.isNotEmpty() }?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = ink.copy(alpha = 0.75f))
                }
            }
        }
        Box(Modifier.size(40.dp).clip(CircleShape).background(colors.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colors.onSecondaryContainer)
        }
    }
}

/** A quick-pick chip on the To card: filled and round when chosen, squarer when not. */
@Composable
private fun QuickChip(on: Boolean, onClick: () -> Unit, spoken: String, content: @Composable (Color) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val corner by animateFloatAsState(if (on) 50f else 28f, spring(stiffness = Spring.StiffnessMedium), label = "quick")
    Box(
        Modifier.height(38.dp).clip(RoundedCornerShape(corner.toInt())).background(if (on) colors.primary else colors.surfaceContainerLowest.copy(alpha = 0.7f))
            .clickable(onClickLabel = spoken, onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) { content(if (on) colors.onPrimary else colors.onPrimaryContainer) }
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
                .requiredSize(60.dp)
                .rotate(turn)
                .clip(shape)
                .background(colors.tertiary)
                .clickable(onClickLabel = tr("Swap the units")) { tap(); onSwap() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.SwapVert, contentDescription = tr("Swap"), tint = colors.onTertiary, modifier = Modifier.size(28.dp).rotate(-turn))
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
    val token = text.takeLastWhile { it !in " /*()^·×" }
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
        AppIcon(icon, contentDescription = null, tint = content)
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
                Text(tr(label), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.width(110.dp))
                Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) { MathText(math, style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 17.sp), color = colors.onSurface, mathScale = 1f) }
            }
        }
        row("Dimension", "\$${q.dims.latex()}\$")
        Units.quantityName(q.dims)?.let { row("Measures", it) }
        state.si?.let { si ->
            val base = Units.inSystem(q.dims, "base")
            row("In SI base", "\$${Units.number(exactRatio(si, base), digits)}\\;${base.latex().takeIf { it != "1" } ?: ""}\$")
        }
        row("Size", "\$1\\;${q.latex()} = ${Units.number(exactRatio(1.0, Units.inSystem(q.dims, "base"), q), digits)}\\;${Units.inSystem(q.dims, "base").latex().takeIf { it != "1" } ?: ""}\$")
    }
}

/** The settings, together: how many figures, and which constants may bridge different dimensions. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionsCard(digits: Int, onDigits: (Int) -> Unit, c: Boolean, onC: (Boolean) -> Unit, h: Boolean, onH: (Boolean) -> Unit, hbar: Boolean, onHbar: (Boolean) -> Unit, kB: Boolean, onKB: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Section("Options") {
        Text("Significant figures: $digits", style = MaterialTheme.typography.labelLarge, color = colors.onSurface, modifier = Modifier.padding(start = 4.dp))
        ExpressiveSlider(
            value = digits.toFloat(),
            onValueChange = { v -> kotlin.math.round(v).toInt().let { if (it != digits) onDigits(it) } },
            valueRange = 2f..15f,
            steps = 12,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = tr("Significant figures") },
        )
        Spacer(Modifier.height(6.dp))
        Text(tr("Physical equivalences"), style = MaterialTheme.typography.labelLarge, color = colors.onSurface, modifier = Modifier.padding(start = 4.dp))
        Text(tr("When the dimensions differ, these constants may connect them: mass and energy, energy and frequency or wavelength, energy and temperature."), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ToggleChip("\$c\$", "speed of light", c, onC)
            ToggleChip("\$h\$", "Planck", h, onH)
            ToggleChip("\$\\hbar\$", "reduced Planck", hbar, onHbar)
            ToggleChip("\$k_B\$", "Boltzmann", kB, onKB)
        }
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
        Text(tr(title), style = MaterialTheme.typography.titleSmall, color = colors.primary, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp).semantics { heading() })
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
        AppIcon(icon, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(tr(label), style = MaterialTheme.typography.labelLarge, color = colors.onPrimaryContainer)
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

/** No prefix first (the one used most), then every SI prefix: the common kilo to giga up front. */
private val PICKER_PREFIXES = listOf("", "k", "M", "G", "T", "P", "E", "Z", "Y", "R", "Q", "h", "da", "d", "c", "m", "µ", "n", "p", "f", "a", "z", "y", "r", "q")

/**
 * The unit picker, in the style of the constants sheet: the unit being built at the top (in
 * LaTeX, with what it measures and Done), the ways to join units (× ÷ powers brackets ⌫) and a
 * prefix row; then a search pill, a chip per category, and every unit in rounded groups, each
 * on a tile with its name and its size in SI units. Tap a unit to add it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun UnitPicker(initial: String, title: String, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tap = rememberKeyTap()
    var expr by rememberSaveable { mutableStateOf(initial) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var prefix by rememberSaveable { mutableStateOf("") }
    val sheet = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val parsed = remember(expr) { runCatching { Units.parse(expr) } }

    fun add(token: String) {
        tap()
        expr = if (expr.isBlank() || expr.last() in "/( ·") expr + token else "$expr $token"
    }
    fun op(text: String) {
        tap()
        expr = when (text) {
            "/" -> expr.trimEnd() + "/"
            "(" -> if (expr.isBlank() || expr.last() in "/( ·") "$expr(" else "$expr ("
            // An explicit times: (lea/Å)·(in/Å), back out of a denominator (see Units.parse).
            "·" -> expr.trimEnd() + "·"
            ")" -> expr.trimEnd() + ")"
            else -> expr.trimEnd() + text
        }
    }
    fun backspace() {
        tap()
        val t = expr.trimEnd()
        // A whole unit (with its prefix and power) at a time, or one operator.
        val m = Regex("""[^\s/()·]+$""").find(t)
        expr = if (m != null) t.substring(0, m.range.first).trimEnd() else t.dropLast(1).trimEnd()
    }
    val shown = remember(query, category) {
        val q = query.trim().lowercase()
        Units.ALL.filter { u ->
            (category == null || u.category == category) &&
                (q.isEmpty() || u.name.lowercase().contains(q) || u.symbol.lowercase().startsWith(q) || u.aliases.any { it.lowercase().startsWith(q) })
        }.groupBy { it.category }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = colors.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr(title), style = MaterialTheme.typography.titleLarge, color = colors.onSurface, modifier = Modifier.weight(1f))
            Box(
                Modifier.height(40.dp).clip(CircleShape).background(colors.primary)
                    .clickable(onClickLabel = tr("Use this unit")) { tap(); onDone(expr.trim()) }.padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) { Text(tr("Done"), style = MaterialTheme.typography.labelLarge, color = colors.onPrimary) }
        }
        // The unit so far: large, on the primary container, with what it measures.
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).clip(RoundedCornerShape(28.dp)).background(colors.primaryContainer).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                val q = parsed.getOrNull()
                Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                    when {
                        expr.isBlank() -> Text(tr("Tap units below"), style = MaterialTheme.typography.bodyLarge, color = colors.onPrimaryContainer.copy(alpha = 0.7f))
                        q != null -> MathText("\$${q.latex()}\$", style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 28.sp), color = colors.onPrimaryContainer, mathScale = 1f)
                        else -> Text(expr, style = MaterialTheme.typography.titleMedium, color = colors.onPrimaryContainer)
                    }
                }
                parsed.getOrNull()?.let { q -> Units.quantityName(q.dims)?.takeIf { expr.isNotBlank() } }?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = colors.onSecondaryContainer, modifier = Modifier.clip(CircleShape).background(colors.secondaryContainer).padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
            // Or type it: the field and the buttons edit the same expression.
            Row(
                Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainerLowest.copy(alpha = 0.75f)).padding(start = 14.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(TableIcons.Edit, contentDescription = null, tint = colors.onPrimaryContainer.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (expr.isEmpty()) Text(tr("or type one, like km/s/Mpc"), style = MaterialTheme.typography.bodyLarge, color = colors.onPrimaryContainer.copy(alpha = 0.6f))
                    BasicTextField(
                        expr, { expr = it },
                        textStyle = MaterialTheme.typography.titleMedium.copy(color = colors.onPrimaryContainer),
                        singleLine = true,
                        cursorBrush = SolidColor(colors.primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = tr("Type a unit") },
                    )
                }
                if (expr.isNotEmpty()) IconButton(onClick = { expr = "" }) { Icon(Icons.Default.Close, contentDescription = tr("Clear the unit"), tint = colors.onPrimaryContainer) }
            }
            if (expr.isNotBlank()) parsed.exceptionOrNull()?.message?.let { MathText(it, style = MaterialTheme.typography.bodySmall, color = colors.error) }
            Suggestions(expr, visible = true) { token, symbol -> expr = expr.dropLast(token.length) + symbol }
            // ×, ÷, powers, brackets and backspace, as one connected row.
            val ops = listOf("\$\\times\$" to "·", "\$\\div\$" to "/", "\$x^{2}\$" to "^2", "\$x^{3}\$" to "^3", "\$x^{-1}\$" to "^-1", "\$(\$" to "(", "\$)\$" to ")")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                ops.forEachIndexed { k, (label, text) ->
                    Box(
                        Modifier.weight(1f).height(44.dp)
                            .clip(if (k == 0) RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp, topEnd = 6.dp, bottomEnd = 6.dp) else RoundedCornerShape(6.dp))
                            .background(colors.surfaceContainerLowest.copy(alpha = 0.75f))
                            .clickable(onClickLabel = when (text) { "·" -> "Times"; "/" -> "Divided by"; "(" -> "Open bracket"; ")" -> "Close bracket"; else -> "To the power ${text.drop(1)}" }) { op(text) },
                        contentAlignment = Alignment.Center,
                    ) { MathText(label, style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 17.sp), color = colors.onPrimaryContainer, mathScale = 1f) }
                }
                Box(
                    Modifier.weight(1.2f).height(44.dp).clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 22.dp, bottomEnd = 22.dp)).background(colors.tertiary)
                        .clickable(onClickLabel = tr("Delete the last unit")) { backspace() },
                    contentAlignment = Alignment.Center,
                ) { Text(tr("⌫"), style = MaterialTheme.typography.titleMedium, color = colors.onTertiary) }
            }
            // Prefixes: the chosen one goes on the next unit tapped (if it takes prefixes).
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Prefix"), style = MaterialTheme.typography.labelMedium, color = colors.onPrimaryContainer, modifier = Modifier.padding(end = 6.dp))
                PICKER_PREFIXES.forEach { p ->
                    val on = p == prefix
                    Box(
                        Modifier.height(34.dp).widthIn(min = 38.dp).clip(if (on) CircleShape else RoundedCornerShape(8.dp))
                            .background(if (on) colors.primary else colors.surfaceContainerLowest.copy(alpha = 0.75f))
                            .clickable(onClickLabel = if (p.isEmpty()) "No prefix" else if (on) "Remove the prefix" else "Prefix ${prefixName(p)}") { prefix = if (on) "" else p }.padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(if (p.isEmpty()) "none" else p, style = MaterialTheme.typography.labelLarge, color = if (on) colors.onPrimary else colors.onPrimaryContainer) }
                }
            }
        }
        // The search, as a pill.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp).height(52.dp).clip(CircleShape).background(colors.surfaceContainerHighest).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(TableIcons.Search, contentDescription = null, tint = colors.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text(tr("Search units"), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                BasicTextField(
                    query, { query = it }, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                    cursorBrush = SolidColor(colors.primary),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = tr("Search units") },
                )
            }
            if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = tr("Clear the search"), tint = colors.onSurfaceVariant) }
        }
        // A chip for each category.
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf<String?>(null) + Units.CATEGORIES).forEach { c ->
                androidx.compose.material3.FilterChip(
                    selected = category == c,
                    onClick = { category = c },
                    label = { Text(c ?: "All") },
                    leadingIcon = null,
                )
            }
        }
        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 32.dp)) {
            if (shown.isEmpty()) item {
                Text("No unit matches “$query”. You can still type it above.", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(24.dp))
            }
            shown.forEach { (section, list) ->
                item(key = "h$section") {
                    Text(section, style = MaterialTheme.typography.labelLarge, color = colors.primary, modifier = Modifier.padding(start = 12.dp, top = 14.dp, bottom = 6.dp))
                }
                items(list, key = { it.symbol }) { u ->
                    val i = list.indexOf(u)
                    // One rounded group per category: big corners at its ends, small between rows.
                    val big = 20.dp; val small = 6.dp
                    val shape = RoundedCornerShape(
                        topStart = if (i == 0) big else small, topEnd = if (i == 0) big else small,
                        bottomStart = if (i == list.lastIndex) big else small, bottomEnd = if (i == list.lastIndex) big else small,
                    )
                    val p = if (u.prefixes) prefix else ""
                    val symbol = if (u.symbol == "fl oz") "floz" else u.symbol
                    val q = remember(p, u.symbol) { runCatching { Units.parse(p + symbol) }.getOrNull() }
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 2.dp).clip(shape).background(colors.surfaceContainer)
                            .clickable(onClickLabel = "Add ${prefixName(p)}${u.name}") { add(p + symbol) }
                            .padding(start = 10.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(colors.primaryContainer).padding(6.dp), contentAlignment = Alignment.Center) {
                            FitInside { MathText("\$${q?.latex() ?: u.latex}\$", style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 22.sp), color = colors.onPrimaryContainer, mathScale = 1f) }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            FlowRow(verticalArrangement = Arrangement.Center, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(prefixName(p) + u.name, color = colors.onSurface, style = MaterialTheme.typography.bodyLarge)
                                if (u.prefixes) Text(tr("prefixes"),
                                    color = colors.onTertiaryContainer,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.align(Alignment.CenterVertically).clip(CircleShape).background(colors.tertiaryContainer).padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            // Its size in SI base units, unless it's an SI base unit itself.
                            val size = q?.let { qq ->
                                val base = Units.inSystem(qq.dims, "base")
                                if (qq.affine != null || (qq.factor / base.factor).let { kotlin.math.abs(it - 1) < 1e-12 }) null
                                else "\$= ${Units.number(qq.factor / base.factor, 5)}\\;${base.latex().takeIf { it != "1" } ?: ""}\$"
                            }
                            Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                                if (size != null) MathText(size, style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = 15.sp), color = colors.onSurface, mathScale = 1f)
                                Text((if (size != null) "  " else "") + (listOf(u.symbol) + u.aliases).filter { t -> t.all { it.code < 128 } || t == u.symbol && '_' !in t }.distinct().take(2).joinToString(" · "), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { add(p + symbol) }) {
                            Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = "Add ${u.name}", tint = colors.primary)
                        }
                    }
                }
            }
        }
    }
}

private fun prefixName(p: String) = mapOf(
    "Q" to "quetta", "R" to "ronna", "Y" to "yotta", "Z" to "zetta", "E" to "exa", "P" to "peta", "T" to "tera", "G" to "giga", "M" to "mega", "k" to "kilo",
    "h" to "hecto", "da" to "deca", "d" to "deci", "c" to "centi", "m" to "milli", "µ" to "micro", "n" to "nano", "p" to "pico", "f" to "femto",
    "a" to "atto", "z" to "zepto", "y" to "yocto", "r" to "ronto", "q" to "quecto",
)[p] ?: ""
