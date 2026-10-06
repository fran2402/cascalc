package com.example.cas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.cas.ui.AppScreen
import com.example.cas.ui.theme.CasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        com.example.cas.ui.CrashLog.install(this)
        com.example.cas.ui.AppSettings.init(this)
        com.example.cas.ui.SceneExport.installMetrics(this)
        openedFile(intent)
        // The icon draws itself on a cold start (not when the screen turns or the theme changes).
        val coldStart = savedInstanceState == null
        setContent {
            CasTheme {
                var launching by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(coldStart) }
                // Keep the screen on while the app is open, if chosen in settings.
                val view = androidx.compose.ui.platform.LocalView.current
                val keepOn = com.example.cas.ui.AppSettings.keepScreenOn
                androidx.compose.runtime.SideEffect { view.keepScreenOn = keepOn }
                AppScreen()
                // The last crash's stack trace, if the app closed unexpectedly.
                com.example.cas.ui.CrashReportDialog()
                if (launching) com.example.cas.ui.LaunchAnimation(onDone = { launching = false })
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        openedFile(intent)
    }

    /** A graph file opened from elsewhere (a file manager, an attachment): the app imports it. */
    private fun openedFile(intent: android.content.Intent?) {
        // A home screen shortcut: a mode, or the unit converter.
        if (intent?.action == com.example.cas.ui.Shortcuts.ACTION) {
            com.example.cas.ui.Shortcuts.target = intent.getStringExtra(com.example.cas.ui.Shortcuts.EXTRA)
            return
        }
        val uri = when (intent?.action) {
            android.content.Intent.ACTION_VIEW -> intent.data
            android.content.Intent.ACTION_SEND -> sharedStream(intent)
            else -> null
        }
        uri?.let { com.example.cas.ui.OpenedGraphFile.uri = it }
    }

    @Suppress("DEPRECATION")
    private fun sharedStream(intent: android.content.Intent): android.net.Uri? = intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM)
}
