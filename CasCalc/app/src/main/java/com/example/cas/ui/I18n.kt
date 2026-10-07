package com.example.cas.ui

import android.content.Context
import com.example.cas.i18n.Translations

/**
 * The app's languages. English is the source; any other is a file assets/i18n/<code>.tsv (see
 * [Translations]), found when the app starts, and anything not translated there stays English.
 * None ship yet, so the app is English and Settings has no language choice until one is added.
 */
object I18n {
    /** A language offered in settings: its code, and its name in itself. */
    class Language(val code: String, val name: String)

    /** Each translation, parsed on first use: at launch only the language in use is read. */
    @Volatile private var tables: Map<String, Lazy<Translations>> = emptyMap()

    /** English, then each translation found. */
    val languages: List<Language> get() = listOf(Language("en", "English")) +
        tables.filterValues { it.value.entries.isNotEmpty() }.map { (code, t) -> Language(code, t.value.name ?: code) }

    fun load(context: Context) {
        val assets = context.applicationContext.assets
        val files = runCatching { assets.list("i18n")?.toList() }.getOrNull().orEmpty().filter { it.endsWith(".tsv") }
        tables = files.associate { file ->
            file.removeSuffix(".tsv") to lazy { runCatching { assets.open("i18n/$file").bufferedReader().use { Translations.parse(it.readText()) } }.getOrDefault(Translations.EMPTY) }
        }
    }

    /** The system's language, if there's a translation for it. */
    fun systemCode(): String = java.util.Locale.getDefault().language.takeIf { it in tables } ?: "en"

    /** The language in use (read as Compose state, so the screen follows a change). */
    val code: String get() = AppSettings.language.takeIf { it == "en" || it in tables } ?: systemCode()

    fun translate(english: String): String {
        val c = code
        if (c == "en") return english
        return tables[c]?.value?.lookup(english) ?: english
    }
}

/** [english] in the app's language (itself when English, or when it has no translation yet). */
fun tr(english: String): String = I18n.translate(english)

/** [english] translated, with {0}, {1}… filled in from [args]: tr("Step {0} of {1}", 2, 3). */
fun tr(english: String, vararg args: Any?): String = Translations.format(I18n.translate(english), args)
