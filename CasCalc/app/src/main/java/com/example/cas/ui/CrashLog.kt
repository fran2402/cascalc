package com.example.cas.ui

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File

/**
 * Keeps the stack trace of a crash, so the next start can show it and it can be copied into a
 * bug report. Nothing leaves the phone unless it's copied or sent in a report.
 */
object CrashLog {
    private fun file(context: Context) = File(context.filesDir, "last_crash.txt")

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val c = app.resources.configuration
                val header = "CAS Scientific Calculator crash · Android ${android.os.Build.VERSION.SDK_INT} · ${android.os.Build.MODEL} · " +
                    "${c.screenWidthDp}×${c.screenHeightDp} dp\n\n"
                file(app).writeText(header + error.stackTraceToString().take(20_000))
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun take(context: Context): String? = runCatching { file(context).takeIf { it.exists() }?.readText() }.getOrNull()

    /** Once shown, the crash is kept aside (not shown again) for the next bug report. */
    fun clear(context: Context) {
        runCatching { val f = file(context); if (f.exists()) { val kept = File(context.filesDir, "previous_crash.txt"); kept.delete(); f.renameTo(kept) } }
    }

    /** The last crash, shown or not, for a bug report. */
    fun last(context: Context): String? = take(context) ?: runCatching { File(context.filesDir, "previous_crash.txt").takeIf { it.exists() }?.readText() }.getOrNull()
}

/** After a crash: what went wrong, with Copy for a bug report. */
@Composable
fun CrashReportDialog() {
    val context = LocalContext.current
    var text by remember { mutableStateOf(CrashLog.take(context)) }
    val shown = text ?: return
    @Suppress("DEPRECATION") val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val close = { CrashLog.clear(context); text = null }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = close,
        title = { Text(tr("The app closed unexpectedly")) },
        text = {
            Column {
                Text(tr("This is what went wrong. Report it (an email with it attached) to help fix it."), style = MaterialTheme.typography.bodyMedium)
                Text(
                    shown,
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                    modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { scope.launch { Feedback.reportBug(context); close() } }) { Text(tr("Report")) }
        },
        dismissButton = {
            androidx.compose.foundation.layout.Row {
                TextButton(onClick = { clipboard.setText(AnnotatedString(shown)); close() }) { Text(tr("Copy")) }
                TextButton(onClick = close) { Text(tr("Close")) }
            }
        },
    )
}
