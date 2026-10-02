package com.example.cas.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cas.editor.MathRow
import com.example.cas.engine.AngleUnit
import com.example.cas.engine.Steps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The worked steps for a calculation in the history (beta). Steps appear one at a time with
 * Next step, or all at once; each is a numbered node on a line, with the rule, a sentence and
 * the math, and smaller steps inside it. A sheet on a phone; on a tablet a two-pane dialog with
 * the question, answer and an outline of the steps beside them.
 */
@Composable
fun StepsView(expression: MathRow, angle: AngleUnit, onDismiss: () -> Unit) {
    // Worked out off the main thread: an integral can take a moment.
    val solution by produceState<Result<Steps.Solution?>?>(null, expression) {
        value = Result.success(withContext(Dispatchers.Default) { Steps.of(expression, angle) })
    }
    var shown by remember { mutableIntStateOf(1) }
    val total = solution?.getOrNull()?.steps?.size ?: 0
    val state = StepsState(shown, total, onNext = { shown = (shown + 1).coerceAtMost(total) }, onAll = { shown = total })
    if (isTabletLayout()) TabletSteps(expression, solution?.getOrNull(), solution != null, state, onDismiss)
    else PhoneSteps(expression, solution?.getOrNull(), solution != null, state, onDismiss)
}

private class StepsState(val shown: Int, val total: Int, val onNext: () -> Unit, val onAll: () -> Unit) {
    val done get() = shown >= total
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneSteps(expression: MathRow, solution: Steps.Solution?, ready: Boolean, state: StepsState, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.surfaceContainerLow) {
        val list = rememberLazyListState()
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f).navigationBarsPadding()) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Header(solution, expression)
                Spacer(Modifier.height(12.dp))
                Question(expression)
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f)) {
                Body(solution, ready, state, list, Modifier.padding(horizontal = 20.dp))
            }
            Controls(state, ready && solution != null, Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
        }
        LaunchedEffect(state.shown) { if (state.shown > 1) list.animateScrollToItem(state.shown - 1) }
    }
}

@Composable
private fun TabletSteps(expression: MathRow, solution: Steps.Solution?, ready: Boolean, state: StepsState, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.shown) { if (state.shown > 1) list.animateScrollToItem(state.shown - 1) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        androidx.compose.material3.Surface(shape = RoundedCornerShape(36.dp), color = colors.surfaceContainerLow, modifier = Modifier.width(1000.dp).heightIn(max = 760.dp).fillMaxHeight(0.9f)) {
            Row(Modifier.fillMaxSize()) {
                // The question, the answer and an outline of the steps.
                Column(Modifier.width(340.dp).fillMaxHeight().background(colors.surfaceContainer).verticalScroll(rememberScrollState()).padding(24.dp)) {
                    Header(solution, expression)
                    Spacer(Modifier.height(16.dp))
                    Question(expression)
                    solution?.let { s ->
                        Spacer(Modifier.height(12.dp))
                        AnswerCard(s.answer)
                        Spacer(Modifier.height(20.dp))
                        Text("Outline", style = MaterialTheme.typography.titleSmall, color = colors.primary, modifier = Modifier.semantics { heading() })
                        Spacer(Modifier.height(8.dp))
                        s.steps.forEachIndexed { k, step ->
                            val reached = k < state.shown
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                    .clickable(enabled = reached, onClickLabel = "Go to ${step.title}") { scope.launch { list.animateScrollToItem(k) } }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${k + 1}", style = MaterialTheme.typography.labelLarge, color = if (reached) colors.primary else colors.outline, modifier = Modifier.width(24.dp))
                                MathText(step.title, style = MaterialTheme.typography.bodyMedium, color = if (reached) colors.onSurface else colors.outline)
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 12.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Steps", style = MaterialTheme.typography.titleLarge, color = colors.onSurface, modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.onSurfaceVariant) }
                    }
                    Box(Modifier.weight(1f)) { Body(solution, ready, state, list, Modifier.padding(horizontal = 28.dp)) }
                    Controls(state, ready && solution != null, Modifier.padding(horizontal = 28.dp, vertical = 16.dp))
                }
            }
        }
    }
}

/** "Steps", the Beta badge and the method as a chip. */
@Composable
private fun Header(solution: Steps.Solution?, question: MathRow) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    @Suppress("DEPRECATION") val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Steps", style = MaterialTheme.typography.headlineSmall, color = colors.onSurface, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.width(10.dp))
        BetaBadge()
        Spacer(Modifier.weight(1f))
        solution?.let { s ->
            // Copy the whole working as text.
            IconButton(onClick = {
                clipboard.setText(androidx.compose.ui.text.AnnotatedString(Steps.text(question, s)))
                android.widget.Toast.makeText(context, "Steps copied", android.widget.Toast.LENGTH_SHORT).show()
            }) { Icon(Icons.Default.ContentCopy, contentDescription = "Copy the steps", tint = colors.onSurfaceVariant) }
        }
        solution?.let {
            MathText(
                it.method, style = MaterialTheme.typography.labelLarge, color = colors.onSecondaryContainer, maxLines = 1,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(colors.secondaryContainer).padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun Question(expression: MathRow) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerHigh).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Box(Modifier.horizontalScroll(rememberScrollState())) { MathView(expression, 22.sp, colors.onSurface) }
    }
}

@Composable
private fun AnswerCard(answer: MathRow) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.primaryContainer).padding(16.dp)) {
        Text("Answer", style = MaterialTheme.typography.labelLarge, color = colors.onPrimaryContainer)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.horizontalScroll(rememberScrollState())) { MathView(answer, 20.sp, colors.onPrimaryContainer) }
    }
}

/** The steps so far, or a spinner, or a note that there are none. */
@Composable
private fun Body(solution: Steps.Solution?, ready: Boolean, state: StepsState, list: LazyListState, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    when {
        !ready -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        solution == null -> Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Info, contentDescription = null, tint = colors.outline, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text("No steps for this one yet", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Text("Steps are in beta: calculus, algebra, complex numbers, matrices, differential equations, statistics and vector calculus for now.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        else -> LazyColumn(state = list, modifier = modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)) {
            itemsIndexed(solution.steps) { k, step ->
                AnimatedVisibility(k < state.shown, enter = fadeIn() + expandVertically(spring(stiffness = Spring.StiffnessMediumLow))) {
                    StepNode(k + 1, step, last = k == solution.steps.lastIndex || k == state.shown - 1)
                }
            }
            item { BetaNote() }
        }
    }
}

/** One step: its node and the line to the next, then the rule, the sentence, the math and its smaller steps. */
@Composable
private fun StepNode(number: Int, step: Steps.Step, last: Boolean) {
    val colors = MaterialTheme.colorScheme
    val (fill, ink) = when (step.kind) {
        Steps.Kind.Result -> colors.primary to colors.onPrimary
        Steps.Kind.Check -> colors.tertiaryContainer to colors.onTertiaryContainer
        Steps.Kind.Note -> colors.surfaceContainerHighest to colors.onSurfaceVariant
        Steps.Kind.Rule -> colors.secondaryContainer to colors.onSecondaryContainer
    }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(40.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(36.dp).clip(if (step.kind == Steps.Kind.Result) RoundedCornerShape(12.dp) else CircleShape).background(fill), contentAlignment = Alignment.Center) {
                when (step.kind) {
                    Steps.Kind.Check -> Icon(Icons.Default.Check, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
                    Steps.Kind.Result -> Icon(Icons.Default.Flag, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
                    else -> Text("$number", style = MaterialTheme.typography.labelLarge, color = ink)
                }
            }
            if (!last) Box(Modifier.width(2.dp).weight(1f).padding(vertical = 4.dp).clip(CircleShape).background(colors.outlineVariant))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f).padding(bottom = 20.dp, top = 6.dp)) {
            MathText(step.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, modifier = Modifier.semantics { heading() })
            step.text?.let { MathText(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp)) }
            step.math?.let { MathBox(it, if (step.kind == Steps.Kind.Result) colors.primaryContainer else colors.surfaceContainerHighest, if (step.kind == Steps.Kind.Result) colors.onPrimaryContainer else colors.onSurface) }
            // Smaller steps inside, lettered, flattened to one level.
            val inner = remember(step) { flatten(step.substeps) }
            if (inner.isNotEmpty()) Column(
                Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surfaceContainer).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                inner.forEachIndexed { k, s ->
                    Row {
                        Text("${'a' + k}", style = MaterialTheme.typography.labelLarge, color = colors.primary, modifier = Modifier.width(20.dp).padding(top = 1.dp))
                        Column(Modifier.weight(1f)) {
                            MathText(s.title, style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
                            s.text?.let { MathText(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
                            s.math?.let { MathBox(it, colors.surfaceContainerHigh, colors.onSurface, small = true) }
                        }
                    }
                }
            }
        }
    }
}

private fun flatten(steps: List<Steps.Step>): List<Steps.Step> = steps.flatMap { listOf(it) + flatten(it.substeps) }

@Composable
private fun MathBox(row: MathRow, background: Color, ink: Color, small: Boolean = false) {
    Box(
        Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(if (small) 12.dp else 16.dp)).background(background)
            .horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = if (small) 8.dp else 10.dp),
    ) { MathView(row, if (small) 16.sp else 19.sp, ink) }
}

/** Next step, or all of them; once they're all shown, a line saying so. */
@Composable
private fun Controls(state: StepsState, enabled: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!enabled || state.done) {
            Icon(Icons.Default.DoneAll, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            Text(if (enabled) "All ${state.total} steps" else "", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            return@Row
        }
        Text("${state.shown} of ${state.total}", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
        TextButton(onClick = state.onAll) { Text("Show all") }
        // The main action, large and filled.
        Row(
            Modifier.height(52.dp).clip(RoundedCornerShape(26.dp)).background(colors.primary).clickable(onClickLabel = "Next step") { state.onNext() }.padding(start = 18.dp, end = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.SkipNext, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Next step", style = MaterialTheme.typography.titleSmall, color = colors.onPrimary)
        }
    }
}

/** That steps are in beta, and a way to report a wrong one. */
@Composable
private fun BetaNote() {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp).clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainer).padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Steps are in beta and may skip some algebra.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
        TextButton(onClick = { scope.launch { Feedback.reportBug(context) } }) { Text("Report") }
    }
}

/** A small "Beta" pill. */
@Composable
fun BetaBadge(text: String = "Beta") {
    val colors = MaterialTheme.colorScheme
    Text(
        text.uppercase(), style = MaterialTheme.typography.labelSmall, color = colors.onTertiaryContainer,
        modifier = Modifier.clip(CircleShape).background(colors.tertiaryContainer).padding(horizontal = 8.dp, vertical = 2.dp).semantics { contentDescription = text },
    )
}
