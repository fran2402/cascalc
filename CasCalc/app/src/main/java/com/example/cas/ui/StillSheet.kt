package com.example.cas.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

/**
 * Scrolling inside a sheet stays inside it: what a list doesn't use (pulling down at its top,
 * a fling running out) is kept here instead of moving the sheet, so a sheet never slides away or
 * jitters while its content is scrolled. The sheet still goes by its handle, a tap outside or Back.
 */
private object SheetScrollGuard : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
}

/** A modal bottom sheet that opens fully (no half-open state to bounce between) and doesn't move as its content scrolls. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StillSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState, containerColor = containerColor) {
        Column(Modifier.nestedScroll(SheetScrollGuard), content = content)
    }
}
