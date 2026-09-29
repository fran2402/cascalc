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
