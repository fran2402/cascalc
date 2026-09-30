package com.example.cas.ui

import android.view.HapticFeedbackConstants
import android.content.Context
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import com.example.cas.engine.LatexParser
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.draw.shadow
import com.example.cas.ui.theme.LocalMathGlyphs
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Brush
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.core.content.FileProvider
import com.example.cas.engine.GraphRequest
import com.example.cas.engine.Latex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Replay
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cas.editor.Const
import com.example.cas.editor.row
import com.example.cas.engine.AngleUnit
import com.example.cas.engine.UnitSystem
import com.example.cas.cas.CoordinateKind
import androidx.compose.material.icons.filled.Edit
import com.example.cas.engine.Constant
import com.example.cas.engine.Formatter
import com.example.cas.ui.theme.CasFonts
import com.example.cas.ui.theme.LocalGlyphFallback
import com.example.cas.ui.theme.equalsKey

@Composable
fun CalculatorScreen(vm: CalculatorViewModel, onGraph: (GraphRequest) -> Unit, modifier: Modifier = Modifier) {
    BackHandler(enabled = vm.historyMode) { vm.historyMode = false }
    if (isTabletLayout()) {
        // Tablets: the keyboard in a column on one side (Settings chooses), history and input on the other.
        val colors = MaterialTheme.colorScheme
        val keypad = @Composable { Keypad(vm, Modifier.width(tabletKeypadWidth()).fillMaxHeight().background(colors.surfaceContainerLow), tablet = true) }
        Row(modifier.fillMaxSize()) {
            if (AppSettings.keypadSide == 0) keypad()
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Display(vm, onGraph, Modifier.fillMaxSize())
                if (vm.busy) Busy(Modifier.align(Alignment.BottomStart).padding(16.dp))
            }
            if (AppSettings.keypadSide != 0) keypad()
        }
        return
    }
    Column(modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            Display(vm, onGraph, Modifier.fillMaxSize())
            if (vm.busy) Busy(Modifier.align(Alignment.BottomStart).padding(16.dp))
            if (vm.keypadHidden && !vm.historyMode) {
                ShowKeypadButton(onClick = { vm.keypadHidden = false }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp))
            }
        }
        AnimatedVisibility(visible = !vm.historyMode && !vm.keypadHidden, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Keypad(vm)
        }
    }
}

/**
 * The function tabs and number pad, driving any [KeypadHost]. Key heights
 * follow the screen, like Google Calculator.
 */
@Composable
fun Keypad(host: KeypadHost, modifier: Modifier = Modifier, tablet: Boolean = false) {
    var showMatrixPicker by remember { mutableStateOf(false) }
    var showConstants by remember { mutableStateOf(false) }
    var showBuilder by remember { mutableStateOf(false) }
    val onKey: (KeyAction) -> Unit = { action ->
        when (action) {
            // A matrix that grows as you fill its last row and column: starts 1 × 1 plus the empty row and column.
            KeyAction.PickMatrix -> host.insertMatrix(2, 2)
            KeyAction.MoreConstants -> showConstants = true
            KeyAction.OpenSymbolBuilder -> showBuilder = true
            else -> host.press(action)
        }
    }
    if (showBuilder) SymbolBuilderPage(
        onDone = { symbol -> showBuilder = false; if (symbol != null) { SavedSymbols.add(symbol); host.press(KeyAction.Type(symbol)) } },
    )
    val screen = LocalConfiguration.current.screenHeightDp.dp
    // A little shorter than Google Calculator's keys, to leave more room for the maths.
    // The same size in all four modes (calculator, 2D, 3D, complex).
    // On a tablet the keyboard has a whole column to itself, so its keys are taller.
    val mainRow = if (tablet) (screen * 0.075f * AppSettings.keypadScale).coerceIn(44.dp, 88.dp)
    else (screen * 0.052f * AppSettings.keypadScale).coerceIn(34.dp, 74.dp)
    val fnRow = mainRow * 0.76f
    // On a tablet the keys sit at the bottom of their column, where the thumbs are, with no handle
    // (the keyboard is always there).
    Column(modifier, verticalArrangement = if (tablet) Arrangement.Bottom else Arrangement.Top) {
        if (!tablet) KeypadHandle(onHide = { host.keypadHidden = true })
        // The graphs' other plotting letters (y, r, θ, t…) as chips, one tap each.
        if (host.quickVariables.isNotEmpty()) QuickVariables(host)
        ControlRow(host)
        AnimatedVisibility(visible = host.panelOpen, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            // A clear gap (20 dp in all, vs 8 dp between rows) so the function keys and the
            // number pad read as two separate groups. It goes away with the panel.
            Box(Modifier.padding(bottom = 12.dp)) { FunctionPanel(host, fnRow, onKey) }
        }
        // The variable key types z instead of x while plotting complex functions.
        val pad = if (host.padEquals) MainKeys.map { r ->
            // Graphs: an equals sign where the variable was (Enter stays bottom right); in 2D the
            // AC key becomes [ ] for lists of points, colored like the ( ) key.
            r.map { k ->
                when {
                    k.spoken == "x" -> KeySpec(KeyLabel.Text("="), KeyAction.Type("="), k.role, "equals sign")
                    k.spoken == "all clear" && host.listKey -> KeySpec(KeyLabel.Text("[ ]"), KeyAction.ListBrackets, KeyRole.Operator, "list brackets")
                    else -> k
                }
            }
        } else if (host.mainVariable == "x") MainKeys else MainKeys.map { r ->
            r.map { k -> if (k.spoken == "x") KeySpec(KeyLabel.Math(row(com.example.cas.editor.Sym(host.mainVariable))), KeyAction.Type(host.mainVariable), k.role, host.mainVariable) else k }
        }
        KeyGrid(pad, mainRow, onKey, fontSize = 28f, modifier = Modifier.padding(bottom = 12.dp), host = host)
    }
    if (showMatrixPicker) {
        MatrixPickerDialog(
            onPick = { r, c -> showMatrixPicker = false; host.insertMatrix(r, c) },
            onDismiss = { showMatrixPicker = false },
        )
    }
    if (showConstants) {
        ConstantsSheet(
            units = host.unitSystem,
            onPick = { id -> showConstants = false; host.press(KeyAction.Insert(null) { Const(id) }) },
            onDismiss = { showConstants = false },
        )
    }
}

@Composable
fun CalculatorLeadingAction(vm: CalculatorViewModel) {
    val colors = MaterialTheme.colorScheme
    IconButton(onClick = { vm.historyMode = !vm.historyMode }) {
        Icon(
            Icons.Outlined.History,
            contentDescription = if (vm.historyMode) "Back to keypad" else "Show history",
            tint = if (vm.historyMode) colors.primary else colors.onSurfaceVariant,
        )
    }
}

@Composable
fun CalculatorTrailingAction(vm: CalculatorViewModel) {
    var menu by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    var acknowledgements by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear the history?") },
            text = { Text("All ${vm.history.size} calculations will be removed. This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; vm.clearHistory() }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
    if (settings) AppSettingsPage(vm, onBack = { settings = false }, onAcknowledgements = { settings = false; acknowledgements = true })
    if (acknowledgements) AcknowledgementsDialog(onDismiss = { acknowledgements = false })
    @Suppress("DEPRECATION") val clipboard = LocalClipboardManager.current
    val colors = MaterialTheme.colorScheme
    run {
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = colors.onSurfaceVariant)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Copy last answer") },
                    enabled = vm.history.isNotEmpty(),
                    onClick = {
                        menu = false
                        vm.history.lastOrNull()?.let { clipboard.setText(AnnotatedString(it.answer.text)) }
                    },
                )
                if (vm.variables.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Clear variables (" + vm.variables.keys.sorted().joinToString(", ") + ")") },
                        onClick = { menu = false; vm.clearVariables() },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = { menu = false; settings = true },
                )
                DropdownMenuItem(
                    text = { Text("Acknowledgements") },
                    onClick = { menu = false; acknowledgements = true },
                )
                DropdownMenuItem(
                    text = { Text("Clear history") },
                    enabled = vm.history.isNotEmpty(),
                    onClick = { menu = false; if (AppSettings.confirmClearHistory) confirmClear = true else vm.clearHistory() },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Display: past calculations above, the expression being typed at the bottom.

@Composable
private fun Display(vm: CalculatorViewModel, onGraph: (GraphRequest) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val list = rememberLazyListState()
    LaunchedEffect(vm.history.size) { list.animateScrollToItem(0) }

    Box(modifier.fillMaxWidth()) {
        LazyColumn(
            state = list,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "input") { InputPanel(vm) }
            items(vm.history.asReversed(), key = { System.identityHashCode(it) }) { item ->
                var confirm by remember { mutableStateOf(false) }
                if (confirm) {
                    AlertDialog(
                        onDismissRequest = { confirm = false },
                        title = { Text("Delete this calculation?") },
                        text = { Text("It will be removed from the history. This can't be undone.") },
                        confirmButton = { TextButton(onClick = { confirm = false; vm.deleteHistory(item) }) { Text("Delete") } },
                        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
                    )
                }
                // Swiping deletes, or asks first when that's turned on in settings.
                SwipeToDelete(onDelete = { if (AppSettings.confirmDeleteEntry) confirm = true else vm.deleteHistory(item) }) { HistoryCard(item, vm, onGraph) }
            }
            if (vm.history.isEmpty() && vm.historyMode) {
                item {
                    Text(
                        "Your calculations will appear here",
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        color = colors.onSurfaceVariant,
                        style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 16.sp),
                    )
                }
            }
        }
    }
}

@Composable
private fun InputPanel(vm: CalculatorViewModel) {
    val colors = MaterialTheme.colorScheme
    val scroll = rememberScrollState()
    LaunchedEffect(vm.version) {
        if (vm.editor.row === vm.editor.root && vm.editor.index == vm.editor.root.items.size) scroll.animateScrollTo(scroll.maxValue)
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalAlignment = Alignment.End) {
        // Where the maths sits in its box, so a tap beside it (the space to its left, or just past
        // its end) puts the cursor at the start or the end of the line.
        var mathLeft by remember { mutableStateOf(0f) }
        var mathRight by remember { mutableStateOf(0f) }
        Box(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .pointerInput(Unit) {
                    detectTapGestures { o ->
                        val root = vm.editor.root
                        vm.tapAt(root, if (o.x < mathLeft || (o.x <= mathRight && o.x < (mathLeft + mathRight) / 2)) 0 else root.items.size)
                    }
                },
            contentAlignment = Alignment.CenterEnd,
        ) {
            val endRoom = with(LocalDensity.current) { 16.dp.toPx() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                // In the box's own coordinates, as taps are.
                modifier = Modifier.onPlaced { c -> val x = c.positionInParent().x; mathLeft = x; mathRight = x + c.size.width - endRoom },
            ) {
                MathView(
                    row = vm.editor.root,
                    fontSize = MathSizes.input,
                    color = colors.onSurface,
                    accent = colors.primary,
                    cursorRow = vm.editor.row,
                    cursorIndex = vm.editor.index,
                    version = vm.version,
                    onTap = vm::tapAt,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .semantics { contentDescription = "Expression" },
                )
                // Room past the end: a tap here reaches the end of the line.
                Spacer(Modifier.width(16.dp))
            }
        }
        Box(Modifier.heightIn(min = 36.dp).horizontalScroll(rememberScrollState()), contentAlignment = Alignment.CenterEnd) {
            val error = vm.error
            val preview = vm.preview
            when {
                vm.busy -> LinearProgressIndicator(Modifier.width(96.dp))
                error != null -> Text(error, color = colors.error, style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 16.sp))
                preview != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!preview.isStatement) Text(if (preview.isApproximate) "≈ " else "= ", color = colors.onSurfaceVariant, style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = MathSizes.preview))
                    MathView(preview.exact, MathSizes.preview, colors.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryCard(item: HistoryItem, vm: CalculatorViewModel, onGraph: (GraphRequest) -> Unit) {
    val colors = MaterialTheme.colorScheme
    @Suppress("DEPRECATION") val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shown = if (item.showApprox && item.answer.approx != null) item.answer.approx else item.answer.exact
    // The card draws into a layer too, so "share as image" can take a picture of it.
    val layer = rememberGraphicsLayer()
    var shareMenu by remember { mutableStateOf(false) }
    val latex = { Latex.of(item.expression) + (if (item.answer.isStatement) "\\quad " else " = ") + Latex.of(shown) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .drawWithContent {
                layer.record { this@drawWithContent.drawContent() }
                drawLayer(layer)
            }
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceContainer)
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f).padding(top = 4.dp).horizontalScroll(rememberScrollState())) {
                MathView(
                    item.expression,
                    MathSizes.historyInput,
                    colors.onSurfaceVariant,
                    modifier = Modifier.combinedClickable(
                        onClickLabel = "Use this expression",
                        onLongClick = { clipboard.setText(AnnotatedString(Formatter.plain(item.expression))) },
                        onClick = { vm.reuse(item.expression) },
                    ),
                )
            }
            item.graph?.let { g ->
                IconButton(onClick = { onGraph(g) }) {
                    Icon(
                        if (g.dimensions == 1) TabIcons.Complex else Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = when (g.dimensions) { 1 -> "Plot on the complex plane"; 2 -> "Graph this"; else -> "Graph this in 3D" },
                        tint = colors.primary,
                    )
                }
            }
            Box {
                IconButton(onClick = { shareMenu = true }) {
                    Icon(Icons.Default.Share, contentDescription = "Share or delete", tint = colors.onSurfaceVariant)
                }
                DropdownMenu(expanded = shareMenu, onDismissRequest = { shareMenu = false }) {
                    DropdownMenuItem(text = { Text("Share as image") }, onClick = {
                        shareMenu = false
                        scope.launch { shareImage(context, layer.toImageBitmap().asAndroidBitmap()) }
                    })
                    DropdownMenuItem(text = { Text("Share as LaTeX") }, onClick = {
                        shareMenu = false
                        shareText(context, latex())
                    })
                    DropdownMenuItem(text = { Text("Copy LaTeX") }, onClick = {
                        shareMenu = false
                        clipboard.setText(AnnotatedString(latex()))
                    })
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = { shareMenu = false; vm.deleteHistory(item) },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (item.answer.approx != null) ApproxChip(item.showApprox) { item.showApprox = !item.showApprox }
            Spacer(Modifier.weight(1f))
            if (!item.answer.isStatement) {
                Text(if (item.answer.isApproximate) "≈" else "=", color = colors.primary, style = TextStyle(fontFamily = CasFonts.CmRoman, fontSize = MathSizes.historyAnswer))
                Spacer(Modifier.width(12.dp))
            }
            Box(Modifier.horizontalScroll(rememberScrollState())) {
                MathView(
                    shown,
                    MathSizes.historyAnswer,
                    colors.onSurface,
                    modifier = Modifier.combinedClickable(
                        onClickLabel = "Use this answer",
                        onLongClick = { clipboard.setText(AnnotatedString(Formatter.plain(shown))) },
                        onClick = { vm.reuse(shown) },
                    ),
                )
            }
        }
    }
}

/** Switches a card between the exact answer and its decimal approximation. */
@Composable
private fun ApproxChip(expanded: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val turn by animateFloatAsState(if (expanded) 90f else 0f, label = "chevron")
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (expanded) colors.primary else colors.primaryContainer)
            .clickable(onClickLabel = if (expanded) "Show exact answer" else "Show decimal", onClick = onClick)
            .padding(start = 10.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (expanded) colors.onPrimary else colors.onPrimaryContainer
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp).rotate(turn))
        Spacer(Modifier.width(4.dp))
        Text(if (expanded) "exact" else "≈", color = fg, style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 16.sp))
    }
}

// ---------------------------------------------------------------------------
// Keypad

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlRow(vm: KeypadHost) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = vm::togglePanel) {
            Icon(
                if (vm.panelOpen) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                contentDescription = if (vm.panelOpen) "Hide functions" else "Show functions",
                tint = colors.onSurfaceVariant,
            )
        }
        val onConstants = FunctionTabs[vm.selectedTab].icon == KeyLabel.Icon(IconId.Atom)
        val onNabla = FunctionTabs[vm.selectedTab].icon == KeyLabel.Icon(IconId.Area)
        if (onNabla) {
            // On the ∇ tab the switch picks the coordinate system (shown by its letters), and ✎ renames them.
            var editing by remember { mutableStateOf(false) }
            SingleChoiceSegmentedButtonRow(Modifier.height(36.dp)) {
                CoordinateKind.entries.forEachIndexed { i, kind ->
                    val c = if (vm.coordinates.kind == kind) vm.coordinates else vm.coordinatesOf(kind)
                    SegmentedButton(
                        selected = vm.coordinates.kind == kind,
                        onClick = { vm.selectCoordinates(c) },
                        shape = SegmentedButtonDefaults.itemShape(i, CoordinateKind.entries.size),
                        icon = {},
                        label = {
                            Text(
                                // Hair spaces keep three segments, ✎ and undo/redo within a 412 dp row.
                                c.names.joinToString("\u200A"),
                                maxLines = 1,
                                modifier = Modifier.semantics { contentDescription = "${kind.label} coordinates ${c.names.joinToString(", ")}" },
                                style = TextStyle(fontFamily = CasFonts.CmItalic, fontSize = 16.sp),
                            )
                        },
                    )
                }
            }
            IconButton(onClick = { editing = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Choose the coordinate letters", tint = colors.onSurfaceVariant)
            }
            if (editing) CoordinatesDialog(vm.coordinates, onDone = { vm.selectCoordinates(it); editing = false }, onDismiss = { editing = false })
        } else if (onConstants) {
            // On the constants tab the switch picks the unit system instead of the angle unit.
            SingleChoiceSegmentedButtonRow(Modifier.height(36.dp)) {
                UnitSystem.entries.forEachIndexed { i, units ->
                    SegmentedButton(
                        selected = vm.unitSystem == units,
                        onClick = { vm.selectUnitSystem(units) },
                        shape = SegmentedButtonDefaults.itemShape(i, UnitSystem.entries.size),
                        icon = {},
                        label = { Text(units.label, maxLines = 1, style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 13.sp)) },
                    )
                }
            }
        } else {
            SingleChoiceSegmentedButtonRow(Modifier.height(36.dp)) {
                AngleUnit.entries.forEachIndexed { i, unit ->
                    SegmentedButton(
                        selected = vm.angleUnit == unit,
                        onClick = { if (vm.angleUnit != unit) vm.toggleAngle() },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        icon = {},
                        label = { Text(if (unit == AngleUnit.Radians) "Rad" else "Deg", style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 14.sp)) },
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.weight(1f))
        // The cursor buttons sit at the right as squarish blocks, as in the keypad design.
        listOf(
            Triple(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Cursor left", vm::moveLeft),
            Triple(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Cursor right", vm::moveRight),
        ).forEach { (icon, label, act) ->
            Box(
                Modifier
                    .padding(start = 6.dp)
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceContainerHigh)
                    .clickable(onClickLabel = label) { act() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = label, tint = colors.onSurface)
            }
        }
    }
}

@Composable
private fun FunctionPanel(vm: KeypadHost, rowHeight: Dp, onKey: (KeyAction) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val glyphs = LocalGlyphFallback.current
    val tab = FunctionTabs[vm.selectedTab]
    Column {
        GroupBar(vm)
        // One rounded surface holds all of a group's keys, so they read as one block.
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(colors.surfaceContainerLow)
                .pointerInput(vm.selectedTab) {
                    // Swiping sideways moves to the next or previous group, as the dots show.
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragEnd = { total = 0f },
                        onHorizontalDrag = { change, delta ->
                            total += delta
                            if (kotlin.math.abs(total) > 80f) {
                                vm.selectTab((vm.selectedTab + if (total < 0) 1 else -1).coerceIn(0, FunctionTabs.lastIndex))
                                total = 0f
                            }
                            change.consume()
                        },
                    )
                },
        ) {
            // Every group is the same height (three rows, as many as the fixed groups have), so
            // nothing shifts as they change; the long lists scroll inside it.
            val gridHeight = rowHeight * 3 + 7.dp * 2 + 16.dp
            AnimatedContent(
                targetState = vm.selectedTab,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally { w -> if (forward) w else -w } + fadeIn()) togetherWith
                        (slideOutHorizontally { w -> if (forward) -w else w } + fadeOut())
                },
                label = "group",
            ) { index ->
            val shown = FunctionTabs[index]
            // The letters group starts with the symbol builder and the symbols built with it.
            // Pinned letters, symbols and constants come first (after the special keys).
            val rows = when (shown.title) {
                "Symbols" -> letterRows(SavedSymbols.list, PinnedKeys.list)
                "Physical constants" -> constantRows(PinnedKeys.list)
                else -> shown.keys
            }
            if (!shown.scrolls) {
                KeyGrid(rows, rowHeight, onKey, fontSize = if (shown.columns >= 5) 18f else 20f, modifier = Modifier.height(gridHeight).padding(vertical = 8.dp), fill = true, host = vm)
            } else {
                // Constants and symbols are long lists: they scroll up and down in place.
                val scroll = key(index) { rememberScrollState() }
                Box(Modifier.height(gridHeight)) {
                    KeyGrid(rows, rowHeight, onKey, fontSize = if (shown.columns >= 5) 18f else 20f, modifier = Modifier.verticalScroll(scroll).padding(vertical = 8.dp), host = vm)
                    if (scroll.value < scroll.maxValue) {
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(18.dp)
                                .background(Brush.verticalGradient(listOf(Color.Transparent, colors.surfaceContainerLow))),
                        )
                    }
                }
            }
            }
        }
        GroupDots(vm)
    }
}

/**
 * The groups, as Material 3 Expressive's connected button group: the segments sit together with
 * a hairline between them, and the chosen one is filled and fully rounded while the rest keep
 * small inner corners. Each keeps its own icon.
 */
@Composable
private fun GroupBar(vm: KeypadHost) {
    val colors = MaterialTheme.colorScheme
    val tap = rememberKeyTap()
    // Icons only, so all nine groups fit across the screen and nothing scrolls or shifts.
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        FunctionTabs.forEachIndexed { i, tab ->
            val selected = vm.selectedTab == i
            val outer = 20.dp
            val inner = 8.dp
            val shape = when {
                selected -> RoundedCornerShape(outer)
                i == 0 -> RoundedCornerShape(topStart = outer, bottomStart = outer, topEnd = inner, bottomEnd = inner)
                i == FunctionTabs.lastIndex -> RoundedCornerShape(topStart = inner, bottomStart = inner, topEnd = outer, bottomEnd = outer)
                else -> RoundedCornerShape(inner)
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(shape)
                    .background(if (selected) colors.primary else colors.surfaceContainerHigh)
                    .clickable(onClickLabel = tab.title) { tap(); vm.selectTab(i) }
                    .semantics { contentDescription = if (selected) "${tab.title}, selected" else tab.title },
                contentAlignment = Alignment.Center,
            ) {
                LabelView(tab.icon, if (selected) colors.onPrimary else colors.onSurfaceVariant, fontSize = 16f, iconSize = 20.dp)
            }
        }
    }
}

/** One dot per group, showing which is open; swiping the keys moves between them. */
@Composable
private fun GroupDots(vm: KeypadHost) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FunctionTabs.indices.forEach { i ->
            val on = i == vm.selectedTab
            Box(
                Modifier
                    .height(5.dp)
                    .width(if (on) 16.dp else 5.dp)
                    .clip(CircleShape)
                    .background(if (on) colors.primary else colors.outlineVariant),
            )
        }
    }
}

@Composable
private fun KeyGrid(rows: List<List<KeySpec>>, rowHeight: Dp, onKey: (KeyAction) -> Unit, fontSize: Float, modifier: Modifier = Modifier, fill: Boolean = false, host: KeypadHost? = null) {
    val columns = rows.maxOfOrNull { it.size } ?: 1
    // The key whose explanation is open, by what it is rather than where it is: pinning moves
    // keys around, and the card must stay with its own key.
    var helpFor by remember { mutableStateOf<String?>(null) }
    fun id(spec: KeySpec) = spec.spoken + "\u0000" + spec.pinId
    Column(modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        rows.forEach { keys ->
            // With [fill] the rows share the group's fixed height, so fewer rows means taller keys.
            Row(
                (if (fill) Modifier.weight(1f) else Modifier.height(rowHeight)).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                keys.forEach { spec -> CalcKey(spec, fontSize, onKey, Modifier.weight(1f), host, onHelp = { helpFor = id(spec) }) }
                // A short last row keeps the key widths of the full rows.
                repeat(columns - keys.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    val open = helpFor?.let { h -> rows.asSequence().flatten().firstOrNull { id(it) == h } }
    if (open != null) {
        val typed = (open.action as? KeyAction.Type)?.text
        val defined = typed != null && open.pinnable && host != null && typed in host.definedSymbols
        KeyHelpDialog(
            open.spoken,
            onDismiss = { helpFor = null },
            // A symbol you built can be removed from its card.
            onRemove = typed?.takeIf { open.spoken == "saved symbol" }?.let { t ->
                { SavedSymbols.remove(t); if (PinnedKeys.isPinned(t)) PinnedKeys.toggle(t); helpFor = null }
            },
            pinned = if (open.pinnable) PinnedKeys.isPinned(open.pinId) else null,
            onPin = { PinnedKeys.toggle(open.pinId) },
            onUndefine = if (defined && typed != null) ({ host?.undefine(typed); helpFor = null }) else null,
            symbol = typed?.takeIf { open.pinnable },
        )
    } else if (helpFor != null) helpFor = null
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalcKey(spec: KeySpec, fontSize: Float, onKey: (KeyAction) -> Unit, modifier: Modifier, host: KeypadHost? = null, onHelp: () -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current
    // A letter or symbol with a value (a := 5) is tinted, so you can see what's defined.
    val typed = (spec.action as? KeyAction.Type)?.text
    val defined = typed != null && spec.pinnable && host != null && typed in host.definedSymbols
    val pinned = spec.pinnable && PinnedKeys.isPinned(spec.pinId)
    val (bg, fg) = if (defined) colors.tertiaryContainer to colors.onTertiaryContainer else when (spec.role) {
        KeyRole.Digit -> colors.surfaceContainerHigh to colors.onSurface
        // Function keys are a neutral block on the panel, as in the design; operators stay tinted.
        KeyRole.Function -> colors.surfaceContainerHighest to colors.onSurface
        KeyRole.Operator -> colors.secondaryContainer to colors.onSecondaryContainer
        KeyRole.Clear -> colors.primaryContainer to colors.onPrimaryContainer
        KeyRole.Equals -> colors.equalsKey
    }
    // Expressive press: the pill squares up, then springs back.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateIntAsState(
        targetValue = if (pressed) 28 else 50,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "corner",
    )
    Box(
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(percent = corner))
            .background(bg)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onLongClick = when {
                    spec.action == KeyAction.Backspace -> ({ onKey(KeyAction.Clear) })
                    // Digits say nothing a long-press could add.
                    spec.role == KeyRole.Digit -> null
                    else -> ({
                        if (AppSettings.haptics) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        // The explanation, unless turned off in settings (pinnable keys always open it, for Pin).
                        if (AppSettings.keyHelp || spec.pinnable) onHelp()
                    })
                },
                onLongClickLabel = if (spec.action == KeyAction.Backspace) "Clear all" else if (spec.role == KeyRole.Digit) null else "Explain",
                onClick = {
                    if (AppSettings.haptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    if (AppSettings.keySounds) view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                    onKey(spec.action)
                },
            )
            .semantics { contentDescription = spec.spoken },
        contentAlignment = Alignment.Center,
    ) {
        LabelView(spec.label, fg, fontSize, iconSize = if (spec.label == KeyLabel.BackspaceIcon || spec.label == KeyLabel.EnterIcon) 28.dp else 24.dp)
        if (pinned) {
            Icon(
                Icons.Default.PushPin, contentDescription = "Pinned", tint = fg.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = 4.dp).size(11.dp).rotate(-30f),
            )
        }
    }
}

/**
 * A key's explanation as a dialog over the darkened screen: its name, the
 * formula (LaTeX, drawn by the calculator's own renderer), a line of theory
 * with inline maths, and how to use it. It stays until Dismiss (or Back).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeyHelpDialog(
    spoken: String,
    onDismiss: () -> Unit,
    onRemove: (() -> Unit)? = null,
    /** Null when the key can't be pinned. */
    pinned: Boolean? = null,
    onPin: () -> Unit = {},
    onUndefine: (() -> Unit)? = null,
    symbol: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val help = KeyHelps.of(spoken)
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(help.title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (help.formula.isNotEmpty()) {
                    val lines = remember(help.formula) { LatexParser.lines(help.formula) }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.surfaceContainerHighest)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        // All on one line, scrolling sideways, rather than stacked.
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                            lines.forEach { MathView(it, 22.sp, colors.onSurface) }
                        }
                    }
                }
                if (help.about.isNotEmpty()) TextWithMaths(help.about, MaterialTheme.typography.bodyLarge, colors.onSurface)
                // Pin, undefine and remove, as buttons on the card.
                if (pinned != null || onUndefine != null || onRemove != null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (pinned != null) FilledTonalButton(onClick = onPin) {
                            Icon(if (pinned) Icons.Outlined.PushPin else Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (pinned) "Unpin" else "Pin")
                        }
                        if (onUndefine != null) FilledTonalButton(onClick = onUndefine) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Undefine" + (symbol?.let { s -> com.example.cas.cas.CustomSymbol.decode(s)?.let { "" } ?: " $s" } ?: ""))
                        }
                        if (onRemove != null) OutlinedButton(onClick = onRemove) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = colors.error)
                            Spacer(Modifier.width(8.dp))
                            Text("Remove symbol", color = colors.error)
                        }
                    }
                }
                if (help.usage.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("How to use", style = MaterialTheme.typography.labelLarge, color = colors.primary)
                        TextWithMaths(help.usage, MaterialTheme.typography.bodyMedium, colors.onSurfaceVariant)
                    }
                }
                help.link?.let { url ->
                    // The source: for constants, NIST's CODATA page.
                    val uri = androidx.compose.ui.platform.LocalUriHandler.current
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClickLabel = "Open the NIST page") { runCatching { uri.openUri(url) } }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "NIST CODATA value and uncertainty",
                            style = MaterialTheme.typography.bodyMedium.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
                            color = colors.primary,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Dismiss") } },
    )
}

/** Text with inline maths written \( … \): words flow and wrap, the maths is drawn in Computer Modern. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TextWithMaths(text: String, style: TextStyle, color: Color) {
    FlowRow(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        LatexParser.inline(text).forEach { (isMaths, piece) ->
            if (isMaths) {
                val maths = remember(piece) { LatexParser.parse(piece) }
                MathView(maths, style.fontSize * 1.15f, color, modifier = Modifier.align(Alignment.CenterVertically))
            } else {
                // One Text per word so lines can wrap between words.
                val words = piece.split(" ")
                val firstWord = words.indexOfFirst { it.isNotEmpty() }
                words.forEachIndexed { k, word ->
                    if (word.isNotEmpty()) {
                        // Keep the space between maths and the word after it ("… \(x\) here").
                        val lead = if (k == firstWord && piece.startsWith(" ")) " " else ""
                        Text(
                            lead + if (k < words.lastIndex || piece.endsWith(" ")) "$word " else word,
                            style = style,
                            color = color,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickVariables(host: KeypadHost) {
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current
    // Scrolls sideways when there are more letters than fit (the 3D graph has seven).
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        host.quickVariables.forEach { v ->
            Box(
                Modifier
                    .height(36.dp)
                    .widthIn(min = 52.dp)
                    .clip(CircleShape)
                    .background(colors.tertiaryContainer)
                    .clickable(onClickLabel = "Type $v") { if (AppSettings.haptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); host.press(KeyAction.Type(v)) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Letters in italic Computer Modern, as they appear in the maths.
                Text(v, color = colors.onTertiaryContainer, style = TextStyle(fontFamily = CasFonts.CmItalic, fontSize = 20.sp))
            }
        }
    }
}

/** Swipe a history card sideways (either way) to delete it; a red strip with a bin shows underneath. */
@Composable
private fun SwipeToDelete(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            // When it asks first, the card snaps back and the dialog decides.
            if (value != SwipeToDismissBoxValue.Settled) { onDelete(); !AppSettings.confirmDeleteEntry } else false
        },
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            val end = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Box(
                Modifier.fillMaxSize().padding(horizontal = 12.dp).clip(RoundedCornerShape(24.dp)).background(colors.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (end) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = colors.onErrorContainer)
            }
        },
    ) { content() }
}

/**
 * The bar on top of the keypad: the hide-keyboard button on the right hides the
 * whole keypad (function keys and number pad); pulling the handle down does the same.
 */
@Composable
private fun KeypadHandle(onHide: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var pulled by remember { mutableStateOf(0f) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { pulled = 0f },
                    onVerticalDrag = { _, dy -> pulled += dy },
                    onDragEnd = { if (pulled > 24.dp.toPx()) onHide() },
                )
            }
            .clickable(onClickLabel = "Hide the keyboard", onClick = onHide),
        contentAlignment = Alignment.Center,
    ) {
        // Just the pill: swipe it down (or tap it) to hide the keyboard.
        Box(Modifier.size(width = 36.dp, height = 4.dp).clip(CircleShape).background(colors.onSurfaceVariant.copy(alpha = 0.4f)))    }
}

/** Brings a hidden keypad back. */
@Composable
fun ShowKeypadButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    SmallFloatingActionButton(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = "Show the keyboard" },
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Icon(Icons.Default.Keyboard, contentDescription = null)
    }
}

/** A key or tab label: text, maths notation (in Google Sans Flex), or an icon. */
@Composable
private fun LabelView(label: KeyLabel, fg: Color, fontSize: Float, iconSize: Dp) {
    val glyphs = LocalGlyphFallback.current
    when (label) {
        is KeyLabel.Text -> Text(
            glyphs.style(label.text),
            color = fg,
            style = TextStyle(
                fontFamily = CasFonts.Key,
                fontSize = (if (label.text.length > 3) fontSize * 0.85f else fontSize).sp,
            ),
        )
        is KeyLabel.Math -> FitInside { MathView(label.row, (fontSize * 0.9f).sp, fg, emptyAsDot = true, computerModern = label.latex) }
        is KeyLabel.Icon -> Icon(iconFor(label.id), contentDescription = null, tint = fg, modifier = Modifier.size(iconSize))
        is KeyLabel.Matrix -> MatrixLabel(label, fg)
        KeyLabel.BackspaceIcon -> Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = null, tint = fg, modifier = Modifier.size(iconSize))
        KeyLabel.EnterIcon -> Icon(Icons.AutoMirrored.Filled.KeyboardReturn, contentDescription = null, tint = fg, modifier = Modifier.size(iconSize))
    }
}

private fun iconFor(id: IconId): ImageVector = when (id) {
    IconId.Roots -> TabIcons.Roots
    IconId.Balance -> TabIcons.Balance
    IconId.Area -> TabIcons.Area
    IconId.Matrix -> TabIcons.Matrix
    IconId.SymbolBuilder -> TabIcons.SymbolBuilder
    IconId.Stats -> TabIcons.Stats
    IconId.Nabla -> TabIcons.Nabla
    IconId.PolarGrid -> PlotIcons.PolarGrid
    IconId.ComplexC -> TabIcons.ComplexC
    IconId.Atom -> TabIcons.Atom
    IconId.Abs -> TabIcons.Abs
    IconId.Letters -> TabIcons.Letters
    IconId.Simplify -> Icons.Default.AutoFixHigh
    IconId.Expand -> Icons.Default.OpenInFull
    IconId.Factor -> Icons.Default.CloseFullscreen
    IconId.Apart -> Icons.AutoMirrored.Filled.CallSplit
    IconId.Together -> Icons.Default.CallMerge
    IconId.Answer -> Icons.Default.Replay
    IconId.MoreConstants -> Icons.Default.MoreHoriz
    IconId.Triangle -> TabIcons.Triangle
}



// ---------------------------------------------------------------------------
// Dialogs

@Composable
private fun MatrixPickerDialog(onPick: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var rows by remember { mutableStateOf(2) }
    var cols by remember { mutableStateOf(2) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Matrix size") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("$rows × $cols", style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 20.sp), color = colors.onSurface)
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (r in 1..5) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (c in 1..5) {
                                val on = r <= rows && c <= cols
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (on) colors.primaryContainer else colors.surfaceContainerHigh)
                                        .border(1.dp, if (on) colors.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable(onClickLabel = "$r by $c") { rows = r; cols = c },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(rows, cols) }) { Text("Insert") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConstantsSheet(units: UnitSystem, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "Physical constants · ${units.label} units",
            color = colors.onSurface,
            style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 18.sp),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            items(Constant.entries) { k ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClickLabel = "Insert ${k.description}") { onPick(k.id) }
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(72.dp)) { MathView(row(Const(k.id)), 22.sp, colors.onSurface) }
                    Column(Modifier.weight(1f)) {
                        Text(k.description, color = colors.onSurface, style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 15.sp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MathView(Formatter.row(com.example.cas.cas.Numeric.approx(k.value(units))), 15.sp, colors.onSurfaceVariant)
                            // SI units only mean something in SI; other systems are in their own units.
                            if (units == UnitSystem.SI && k.unit.isNotEmpty()) {
                                Text("  " + k.unit, color = colors.onSurfaceVariant, style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 13.sp))
                            }
                        }
                    }
                    if (k.exact && units == UnitSystem.SI) {
                        Text("exact", color = colors.primary, style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 13.sp))
                    }
                }
            }
        }
    }
}


@Composable
internal fun SettingsSection(title: String) =
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))

@Composable
private fun SettingsToggle(title: String, detail: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.onSurface, style = MaterialTheme.typography.bodyLarge)
            if (detail != null) Text(detail, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Swatches for the app's color (the first is the built-in olive), then any color of your own. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeColorChoice() {
    val colors = MaterialTheme.colorScheme
    var picker by remember { mutableStateOf(false) }
    val current = AppSettings.themeColor
    val custom = TonalScheme.PRESETS.none { it.second == current }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("App color", color = colors.onSurface, style = MaterialTheme.typography.bodyLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            @Composable
            fun Swatch(fill: Color, selected: Boolean, label: String, onClick: () -> Unit, content: @Composable () -> Unit = {}) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(fill)
                        .then(if (selected) Modifier.border(3.dp, colors.onSurface, CircleShape) else Modifier)
                        .clickable(onClickLabel = label, onClick = onClick)
                        .semantics { contentDescription = label + if (selected) ", chosen" else "" },
                    contentAlignment = Alignment.Center,
                ) { content() }
            }
            TonalScheme.PRESETS.forEach { (name, seed) ->
                // Olive shows the built-in palette's own green.
                Swatch(Color(if (seed == 0) 0xFF5B6133.toInt() else seed), current == seed, name, onClick = { AppSettings.changeThemeColor(seed) })
            }
            Swatch(if (custom) Color(current) else colors.surfaceContainerHighest, custom, "Your own color", onClick = { picker = true }) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = if (custom) Color.White else colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
    if (picker) ColorPickerDialog(
        initial = Color(if (current == 0) 0xFF5B6133.toInt() else current),
        // "Default" goes back to olive.
        onPick = { c -> picker = false; AppSettings.changeThemeColor(c?.toArgb()?.let { if (it == 0) 1 else it } ?: 0) },
        onDismiss = { picker = false },
    )
}

@Composable
private fun SettingsChoice(title: String, options: List<String>, selected: Int, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { k, name ->
                SegmentedButton(
                    selected = selected == k,
                    onClick = { onChange(k) },
                    shape = SegmentedButtonDefaults.itemShape(k, options.size),
                    icon = {},
                    label = { Text(name, maxLines = 1) },
                )
            }
        }
    }
}

@Composable
fun AppSettingsPage(vm: CalculatorViewModel? = null, onBack: () -> Unit, onAcknowledgements: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tablet = isTabletLayout()
    // One scrolling page on a phone; on a tablet, the sections down the left and one at a time on the right.
    SectionedPage("Settings", onBack = onBack, sections = listOf(
        PageSection("Appearance", Icons.Outlined.Palette) {
            SettingsChoice("Theme", listOf("System", "Light", "Dark"), AppSettings.theme, AppSettings::changeTheme)
            val wallpaperColors = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
            if (wallpaperColors) {
                SettingsToggle("Colors from your wallpaper", "Material You dynamic color", AppSettings.dynamicColor, AppSettings::changeDynamicColor)
            }
            // Without Material You, the colors grow from one you choose.
            if (!wallpaperColors || !AppSettings.dynamicColor) ThemeColorChoice()
            SettingsChoice("Maths size", listOf("Small", "Medium", "Large"), AppSettings.mathSize, AppSettings::changeMathSize)
            SettingsChoice("Keypad size", listOf("Compact", "Medium", "Tall"), AppSettings.keypadSize, AppSettings::changeKeypadSize)
            // Only on tablets (and unfolded foldables), where the keyboard sits beside the maths.
            if (tablet) SettingsChoice("Keyboard side", listOf("Left", "Right"), AppSettings.keypadSide, AppSettings::changeKeypadSide)
            SettingsToggle("Expressive motion", "Springy animations; off for calmer ones", AppSettings.expressiveMotion, AppSettings::changeExpressiveMotion)
        },
        PageSection("Calculator", Icons.Outlined.Calculate) {
            SettingsToggle("Live answer", "The result under what you're typing", AppSettings.livePreview, AppSettings::changeLivePreview)
            SettingsToggle("Continue from the answer", "An operator after = starts with Ans", AppSettings.continueFromAnswer, AppSettings::changeContinueFromAnswer)
            SettingsToggle("Explanations on long-press", "Formula, theory and how to use each key", AppSettings.keyHelp, AppSettings::changeKeyHelp)
        },
        PageSection("History", Icons.Outlined.History) {
            SettingsChoice("History keeps", listOf("50", "100", "500", "All"), when (AppSettings.historyLimit) { 50 -> 0; 100 -> 1; 500 -> 2; else -> 3 }) {
                AppSettings.changeHistoryLimit(listOf(50, 100, 500, 0)[it])
            }
            SettingsToggle("Ask before clearing history", null, AppSettings.confirmClearHistory, AppSettings::changeConfirmClearHistory)
            SettingsToggle("Ask before deleting", "A calculation, or a line in a graph", AppSettings.confirmDeleteEntry, AppSettings::changeConfirmDeleteEntry)
        },
        PageSection("Numbers", Icons.Outlined.Pin) {
            if (vm != null) Column {
                Text("${vm.digits} significant digits", color = colors.onSurface, style = MaterialTheme.typography.bodyLarge)
                ExpressiveSlider(
                    value = vm.digits.toFloat(),
                    onValueChange = { vm.changeDigits(it.roundToInt()) },
                    valueRange = 4f..15f,
                    steps = 10,
                    modifier = Modifier.semantics { contentDescription = "Significant digits" },
                )
            }
            if (vm != null) SettingsToggle("Show decimals first", "The exact form stays one tap away", vm.decimalFirst, vm::changeDecimalFirst)
            SettingsToggle("Group digits", "1 000 000 rather than 1000000", AppSettings.groupDigits, AppSettings::changeGroupDigits)
            SettingsChoice("Number format", listOf("Auto", "Scientific", "Engineering"), AppSettings.numberFormat, AppSettings::changeNumberFormat)
            SettingsChoice("Complex decimals", listOf("a + bi", "Polar"), if (AppSettings.polarComplex) 1 else 0) { AppSettings.changePolarComplex(it == 1) }
        },
        PageSection("Graphs", Icons.AutoMirrored.Outlined.ShowChart) {
            SettingsToggle("Grid lines", "The axes always show", AppSettings.showGrid, AppSettings::changeShowGrid)
            SettingsToggle("Legend", "Each line's name in the corner, and in exports. Hold a line to rename it", AppSettings.showLegend, AppSettings::changeShowLegend)
            SettingsToggle("Mark points on curves", "Zeros, extrema and crossings of the tapped curve", AppSettings.specialPoints, AppSettings::changeSpecialPoints)
            SettingsChoice("Starting view", listOf("±5", "±10", "±20"), when (AppSettings.viewHalfWidth) { 5 -> 0; 20 -> 2; else -> 1 }) {
                AppSettings.changeViewHalfWidth(listOf(5, 10, 20)[it])
            }
            SettingsChoice("Complex plot quality", listOf("Standard", "High"), AppSettings.complexQuality, AppSettings::changeComplexQuality)
            SettingsChoice("3D surface detail", listOf("Low", "Medium", "High"), AppSettings.surfaceDetail, AppSettings::changeSurfaceDetail)
        },
        PageSection("Touch and screen", Icons.Outlined.TouchApp) {
            SettingsToggle("Haptic feedback", "A tap on each key and button", AppSettings.haptics, AppSettings::changeHaptics)
            SettingsToggle("Key sounds", "A click on each key (uses the system's touch sounds)", AppSettings.keySounds, AppSettings::changeKeySounds)
            SettingsToggle("Keep the screen on", "While the app is open", AppSettings.keepScreenOn, AppSettings::changeKeepScreenOn)
        },
        PageSection("About", Icons.Outlined.Info) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClickLabel = "Open the acknowledgements") { onAcknowledgements() }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Acknowledgements", color = colors.onSurface, style = MaterialTheme.typography.bodyLarge)
                    Text("Fonts, libraries, data and methods, with links", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
            }
        },
    ))
}

/** Shares a bitmap through the system share sheet, via a file in the cache. */
private suspend fun shareImage(context: Context, bitmap: Bitmap) {
    val uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "answer.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, context.packageName + ".files", file)
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Share answer"))
}

private fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Share LaTeX"))
}


/** Letters offered for coordinates: the usual ones in maths and physics. */
// The usual choices (x, y, z, r, ρ, θ, φ) come first so they're visible without scrolling.
private val COORDINATE_LETTERS = listOf("x", "y", "z", "r", "ρ", "θ", "φ", "s", "ϕ", "ψ", "u", "v", "w", "t", "q", "ξ", "η", "ζ")

/**
 * Picks the letters for the current coordinate system: one row of choices per
 * coordinate, with the system's usual letters one tap away.
 */
@Composable
private fun CoordinatesDialog(current: com.example.cas.cas.Coordinates, onDone: (com.example.cas.cas.Coordinates) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var names by remember { mutableStateOf(current.names) }
    val valid = names.toSet().size == 3
    val roles = when (current.kind) {
        CoordinateKind.Cartesian -> listOf("First", "Second", "Third")
        CoordinateKind.Cylindrical -> listOf("Distance from the axis", "Angle around the axis", "Height")
        CoordinateKind.Spherical -> listOf("Distance from the origin", "Angle from the z-axis", "Angle around the z-axis")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${current.kind.label} coordinates") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                roles.forEachIndexed { k, role ->
                    Text(role, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        COORDINATE_LETTERS.forEach { letter ->
                            val on = names[k] == letter
                            val taken = !on && letter in names
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (on) colors.primary else colors.surfaceContainerHigh)
                                    .clickable(enabled = !taken, onClickLabel = "Use $letter") { names = names.toMutableList().also { it[k] = letter } },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    letter,
                                    color = if (on) colors.onPrimary else colors.onSurface.copy(alpha = if (taken) 0.3f else 1f),
                                    style = TextStyle(fontFamily = CasFonts.CmItalic, fontSize = 20.sp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onDone(com.example.cas.cas.Coordinates(current.kind, names)) }) { Text("Done") } },
        dismissButton = { TextButton(onClick = { names = current.kind.defaults }) { Text("Reset to ${current.kind.defaults.joinToString(", ")}") } },
    )
}


/** Maths sizes on the calculator screen, kept small so more fits. */
object MathSizes {
    val input get() = (30 * AppSettings.mathScale).sp
    val preview get() = (17 * AppSettings.mathScale).sp
    val historyInput get() = (19 * AppSettings.mathScale).sp
    val historyAnswer get() = (23 * AppSettings.mathScale).sp
}


/**
 * A linear-algebra key: a bracketed grid of dots standing for a matrix, with a prefix (det, λ),
 * a raised suffix (−1, T, H) or a second grid after an operator (·, ×, ∘, ⊗), as in the icons.
 */
@Composable
private fun MatrixLabel(label: KeyLabel.Matrix, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (label.prefix.isNotEmpty()) {
            Text(label.prefix, color = color, style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 15.sp), maxLines = 1)
        }
        DotMatrix(color, wide = label.wide)
        if (label.sup.isNotEmpty()) {
            Text(
                label.sup,
                color = color,
                style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 11.sp),
                // Raised clear of the bracket, like an exponent.
                modifier = Modifier.align(Alignment.Top).offset(y = (-4).dp),
                maxLines = 1,
            )
        }
        if (label.second) {
            Text(label.op, color = color, style = TextStyle(fontFamily = CasFonts.Ui, fontSize = 14.sp))
            DotMatrix(color, wide = false, tall = label.secondTall)
        }
    }
}

/** The bracketed grid of dots itself: 3×3, or 3×1 for a row vector and 1×3 for a column. */
@Composable
private fun DotMatrix(color: Color, wide: Boolean = false, tall: Boolean = false) {
    val cols = if (wide) 3 else if (tall) 1 else 3
    val rows = if (wide) 1 else if (tall) 3 else 3
    Canvas(Modifier.size(width = (7 + cols * 5).dp, height = (6 + rows * 5).dp)) {
        val stroke = 1.6.dp.toPx()
        val r = 1.1.dp.toPx()
        val top = 1.dp.toPx()
        val bottom = size.height - 1.dp.toPx()
        val lip = 3.dp.toPx()
        // Square brackets on either side.
        listOf(true, false).forEach { left ->
            val x = if (left) 1.dp.toPx() else size.width - 1.dp.toPx()
            val dir = if (left) 1f else -1f
            drawLine(color, Offset(x, top), Offset(x, bottom), stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(x, top), Offset(x + dir * lip, top), stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(x, bottom), Offset(x + dir * lip, bottom), stroke, cap = StrokeCap.Round)
        }
        val stepX = 5.dp.toPx()
        val stepY = 5.dp.toPx()
        val x0 = (size.width - (cols - 1) * stepX) / 2
        val y0 = (size.height - (rows - 1) * stepY) / 2
        for (i in 0 until rows) for (j in 0 until cols) {
            drawCircle(color, r, Offset(x0 + j * stepX, y0 + i * stepY))
        }
    }
}
