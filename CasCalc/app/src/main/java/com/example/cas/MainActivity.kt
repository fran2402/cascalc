package com.example.cas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.cas.ui.AppScreen
import com.example.cas.ui.theme.CasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        com.example.cas.ui.AppSettings.init(this)
        setContent {
            CasTheme {
                // Keep the screen on while the app is open, if chosen in settings.
                val view = androidx.compose.ui.platform.LocalView.current
                val keepOn = com.example.cas.ui.AppSettings.keepScreenOn
                androidx.compose.runtime.SideEffect { view.keepScreenOn = keepOn }
                AppScreen()
            }
        }
    }
}
