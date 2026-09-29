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
object FavouriteColormaps {
    private var prefs: SharedPreferences? = null
    val list = mutableStateListOf<String>()

    fun init(context: Context) {
        val p = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs = p
        list.clear()
        val saved = p.getString("colormaps", null)
        list.addAll(saved?.split('\n')?.filter { it.isNotEmpty() } ?: com.example.cas.graph.Colormap.DEFAULT_FAVOURITES)
    }

    fun add(name: String) { if (name !in list) { list.add(name); save() } }
    fun remove(name: String) { list.remove(name); save() }

    /** Moves the favourite at [from] to [to] (dragging it in the list). */
    fun move(from: Int, to: Int) {
        if (from !in list.indices || to !in list.indices || from == to) return
        list.add(to, list.removeAt(from))
        save()
    }

    private fun save() { prefs?.edit()?.putString("colormaps", list.joinToString("\n"))?.apply() }
}
