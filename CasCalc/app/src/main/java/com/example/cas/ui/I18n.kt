package com.example.cas.ui

import android.content.Context
import com.example.cas.i18n.Translations

/**
 * The app's languages. English is the source; the others are read from assets/i18n/<code>.tsv
 * (see [Translations]), and anything not translated there stays English.
 */
object I18n {
    /** A language offered in settings: its code, and its name in itself. */
    class Language(val code: String, val name: String)

    val languages = listOf(Language("en", "English"), Language("fy", "Frysk"), Language("sh", "Srpskohrvatski"))

    @Volatile private var tables: Map<String, Translations> = emptyMap()

    fun load(context: Context) {
        tables = languages.drop(1).associate { l ->
            l.code to runCatching { context.assets.open("i18n/${l.code}.tsv").bufferedReader().use { Translations.parse(it.readText()) } }.getOrDefault(Translations.EMPTY)
        }
    }

    /** The system's language as one of ours: Frisian for fy, Serbo-Croatian for sh, sr, hr, bs. */
    fun systemCode(): String = when (java.util.Locale.getDefault().language) {
        "fy" -> "fy"
        "sh", "sr", "hr", "bs", "cnr" -> "sh"
        else -> "en"
    }

    /** The language in use (read as Compose state, so the screen follows a change). */
    val code: String get() = AppSettings.language.ifEmpty { systemCode() }

    fun translate(english: String): String {
        val c = code
        if (c == "en") return english
        return tables[c]?.lookup(english, if (c == "sh" && AppSettings.ekavian) 1 else 0) ?: english
    }
}

/** [english] in the app's language (itself when English, or when it has no translation yet). */
fun tr(english: String): String = I18n.translate(english)

/** [english] translated, with {0}, {1}… filled in from [args]: tr("Step {0} of {1}", 2, 3). */
fun tr(english: String, vararg args: Any?): String = Translations.format(I18n.translate(english), args)
