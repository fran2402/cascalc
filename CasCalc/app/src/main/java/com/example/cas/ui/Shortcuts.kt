package com.example.cas.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.cas.R

/**
 * Home screen shortcuts: straight to the unit converter or a graph mode. They're on the app
 * icon (hold it) from res/xml/shortcuts.xml, and Settings can pin one to the home screen.
 */
object Shortcuts {
    const val ACTION = "com.example.cas.action.OPEN"
    const val EXTRA = "open"

    /** Where a shortcut asked to go ("converter" or a [Mode] name), waiting for the app to show it. */
    var target by mutableStateOf<String?>(null)

    /** The unit converter asked for by a shortcut, for the calculator screen to open. */
    var openConverter by mutableStateOf(false)

    class Entry(val id: String, val target: String, val label: String, val longLabel: String, val icon: Int)

    val entries = listOf(
        Entry("converter", "converter", "Units", "Unit converter", R.mipmap.shortcut_converter),
        Entry("graph2d", Mode.Graph2D.name, "2D graph", "2D graphing", R.mipmap.shortcut_graph2d),
        Entry("graph3d", Mode.Graph3D.name, "3D graph", "3D graphing", R.mipmap.shortcut_graph3d),
        Entry("complex", Mode.Complex.name, "Complex", "Complex plane", R.mipmap.shortcut_complex),
    )

    /** Asks the launcher to put [e] on the home screen (it shows its own confirmation). */
    fun pin(context: Context, e: Entry) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Toast.makeText(context, "Your home screen doesn't take shortcuts from apps; hold the app's icon and drag one out instead", Toast.LENGTH_LONG).show()
            return
        }
        val intent = Intent(ACTION).setClassName(context, "com.example.cas.MainActivity").putExtra(EXTRA, e.target)
        val info = ShortcutInfoCompat.Builder(context, "pinned_" + e.id)
            .setShortLabel(e.label)
            .setLongLabel(e.longLabel)
            .setIcon(IconCompat.createWithResource(context, e.icon))
            .setIntent(intent)
            .build()
        ShortcutManagerCompat.requestPinShortcut(context, info, null)
    }
}
