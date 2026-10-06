package com.example.cas.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf

/** Symbols made in the symbol builder, newest first, kept in preferences. */
object SavedSymbols {
    private var prefs: SharedPreferences? = null
    val list = mutableStateListOf<String>()

    fun init(context: Context) {
        val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs = p
        list.clear()
        list.addAll(p.getString("symbols", "").orEmpty().split('\n').filter { com.example.cas.cas.CustomSymbol.decode(it) != null })
    }

    /** Adds a symbol at the top (moving it there if it was already saved). */
    fun add(symbol: String) {
        list.remove(symbol)
        list.add(0, symbol)
        save()
    }

    fun remove(symbol: String) {
        list.remove(symbol)
        save()
    }

    private fun save() { prefs?.edit()?.putString("symbols", list.joinToString("\n"))?.apply() }
}

/**
 * Letters, built symbols and constants pinned from their long-press card: they move to the
 * front of their group, after its special keys. Kept in preferences, in the order pinned.
 */
object PinnedKeys {
    private var prefs: SharedPreferences? = null
    val list = mutableStateListOf<String>()

    fun init(context: Context) {
        val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs = p
        list.clear()
        list.addAll(p.getString("pinned", "").orEmpty().split('\n').filter { it.isNotEmpty() })
    }

    fun isPinned(id: String) = id in list

    fun toggle(id: String) {
        if (!list.remove(id)) list.add(id)
        prefs?.edit()?.putString("pinned", list.joinToString("\n"))?.apply()
    }
}

/**
 * The colormaps offered first on the complex plane, in the order you've put them; the rest are
 * under "More colormaps". Kept in preferences by matplotlib name.
 */
object FavoriteColormaps {
    private var prefs: SharedPreferences? = null
    val list = mutableStateListOf<String>()

    fun init(context: Context) {
        val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs = p
        list.clear()
        val saved = p.getString("colormaps", null)
        list.addAll(saved?.split('\n')?.filter { it.isNotEmpty() } ?: com.example.cas.graph.Colormap.DEFAULT_FAVORITES)
        // The maps made from the theme's own colors came later: offered once, after Theme.
        if (saved != null && !p.getBoolean("themeMapsOffered", false)) {
            val extra = listOf("theme_tonal", "theme_duo", "theme_diverging", "theme_loop").filter { it !in list }
            list.addAll((list.indexOf("theme") + 1).coerceIn(0, list.size), extra)
            p.edit().putBoolean("themeMapsOffered", true).apply(); save()
        } else if (saved == null) p.edit().putBoolean("themeMapsOffered", true).apply()
    }

    fun add(name: String) { if (name !in list) { list.add(name); save() } }
    fun remove(name: String) { list.remove(name); save() }

    /** Moves the favorite at [from] to [to] (dragging it in the list). */
    fun move(from: Int, to: Int) {
        if (from !in list.indices || to !in list.indices || from == to) return
        list.add(to, list.removeAt(from))
        save()
    }

    private fun save() { prefs?.edit()?.putString("colormaps", list.joinToString("\n"))?.apply() }
}

/** Units pinned in the unit picker: shown first, under Pinned, for quick access. Kept by symbol. */
object PinnedUnits {
    private var prefs: SharedPreferences? = null
    val list = mutableStateListOf<String>()

    fun init(context: Context) {
        val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs = p
        list.clear()
        list.addAll(p.getString("pinnedUnits", "").orEmpty().split('\n').filter { it.isNotEmpty() })
    }

    fun isPinned(symbol: String) = symbol in list
    fun toggle(symbol: String) {
        if (!list.remove(symbol)) list.add(symbol)
        prefs?.edit()?.putString("pinnedUnits", list.joinToString("\n"))?.apply()
    }
}
