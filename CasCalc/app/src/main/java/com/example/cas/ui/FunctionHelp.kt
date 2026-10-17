package com.example.cas.ui

import com.example.cas.editor.Func

/**
 * A function's help by its name in the math (gcd, perm, normpdf…), found through the key that
 * inserts it, so the steps can say what a function is with the same title and formula as its
 * key's long-press card.
 */
object FunctionHelp {
    private val spokenByName: Map<String, String> by lazy {
        val keys = (FunctionTabs.flatMap { it.keys } + MainKeys + ExtraKeyPages.flatten()).flatten()
        val out = HashMap<String, String>()
        for (k in keys) {
            val insert = k.action as? KeyAction.Insert ?: continue
            val node = runCatching { insert.make() }.getOrNull() as? Func ?: continue
            out.putIfAbsent(node.name, k.spoken)
        }
        out
    }

    /** The title and formula (LaTeX, its lines joined) for the function [name], or null if no key inserts it. */
    fun of(name: String): Pair<String, String>? {
        val spoken = spokenByName[name] ?: return null
        val help = runCatching { KeyHelps.of(spoken) }.getOrNull() ?: return null
        val formula = help.formula.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString(",\\quad ")
        return help.title to formula
    }
}
