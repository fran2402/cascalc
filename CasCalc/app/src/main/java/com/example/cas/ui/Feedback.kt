package com.example.cas.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Bug reports and feature requests, as email drafts to the developer. A bug report has the
 * app's log attached (this app's own lines only, plus the last crash, if there was one) and
 * nothing is sent until it's sent from the email app.
 */
object Feedback {
    const val ADDRESS = "stimacfran@gmail.com"

    /** About the phone and the app, at the top of the log. */
    private fun about(context: Context): String {
        val c = context.resources.configuration
        val version = runCatching {
            @Suppress("DEPRECATION") context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "?"
        return "CasCalc $version · Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT}) · " +
            "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} · ${c.screenWidthDp}×${c.screenHeightDp} dp · " +
            "${java.util.Locale.getDefault()}"
    }

    /** This app's recent log lines (any app may read its own without a permission). */
    private fun logcat(): String = runCatching {
        val p = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "threadtime", "-t", "3000", "--pid=${android.os.Process.myPid()}"))
        val text = p.inputStream.bufferedReader().use { it.readText() }
        p.waitFor()
        text
    }.getOrElse { "(the log couldn't be read: ${it.message})" }

    /** The log file to attach: about the phone, the last crash, then the log. */
    private fun writeLog(context: Context): File {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "cascalc-log.txt")
        val crash = CrashLog.last(context)
        file.writeText(buildString {
            appendLine(about(context))
            appendLine()
            if (crash != null) { appendLine("── Last crash ──"); appendLine(crash); appendLine() }
            appendLine("── Log ──")
            append(logcat().takeLast(400_000))
        })
        return file
    }

    /** Opens an email draft for a bug report with the log attached. */
    suspend fun reportBug(context: Context) {
        val uri = withContext(Dispatchers.IO) { FileProvider.getUriForFile(context, context.packageName + ".files", writeLog(context)) }
        val body = "What happened:\n\n\nWhat you expected instead:\n\n\nSteps to make it happen again:\n1. \n2. \n\n" +
            "(The app's log is attached. ${about(context)})\n"
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(ADDRESS))
            putExtra(Intent.EXTRA_SUBJECT, "CasCalc bug report")
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Only email apps: a draft, not a message or a note.
            selector = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
        }
        start(context, send, Intent(send).apply { selector = null })
    }

    /** Opens an email draft for a feature request. */
    fun requestFeature(context: Context) {
        val subject = "CasCalc feature request"
        val body = "The feature:\n\n\nWhat it would help with:\n\n\n(${about(context)})\n"
        val mail = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$ADDRESS?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(ADDRESS))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        start(context, mail, null)
    }

    /** Starts [intent], or [fallback] (any app that can send it) when no email app takes it. */
    private fun start(context: Context, intent: Intent, fallback: Intent?) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            try {
                if (fallback == null) throw ActivityNotFoundException()
                context.startActivity(Intent.createChooser(fallback, "Send with"))
            } catch (_: ActivityNotFoundException) {
                android.widget.Toast.makeText(context, "No email app found. Write to $ADDRESS", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}
