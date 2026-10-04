package com.example.cas.i18n

/**
 * One language's strings, read from a tab-separated file: each line is the English text, a
 * tab, and its translation. Lines starting with # are comments, except a first line
 * "#name<tab>Frysk", which names the language in itself for the settings. \n and \t in either
 * column are a new line and a tab. The English is the key, so a string with no line here stays
 * English. Placeholders {0}, {1}… are filled in from [format]'s arguments.
 */
class Translations(val entries: Map<String, String>, val name: String? = null) {

    /** The translation of [english], or null if there is none. */
    fun lookup(english: String): String? = entries[english]

    companion object {
        val EMPTY = Translations(emptyMap())

        fun parse(text: String): Translations {
            val out = LinkedHashMap<String, String>()
            var name: String? = null
            text.lineSequence().forEach { raw ->
                val line = raw.trimEnd('\r')
                if (line.startsWith("#name\t")) { name = line.substringAfter('\t').trim().ifEmpty { null }; return@forEach }
                if (line.isBlank() || line.startsWith("#")) return@forEach
                val tab = line.indexOf('\t')
                if (tab <= 0) return@forEach
                val key = unescape(line.substring(0, tab))
                val value = unescape(line.substring(tab + 1))
                if (value.isNotEmpty()) out[key] = value
            }
            return Translations(out, name)
        }

        private fun unescape(s: String): String = if ('\\' !in s) s else buildString {
            var k = 0
            while (k < s.length) {
                val c = s[k]
                if (c == '\\' && k + 1 < s.length) {
                    when (s[k + 1]) { 'n' -> append('\n'); 't' -> append('\t'); '\\' -> append('\\'); else -> { append(c); append(s[k + 1]) } }
                    k += 2
                } else { append(c); k++ }
            }
        }

        /** [template] with {0}, {1}… replaced by [args]. */
        fun format(template: String, args: Array<out Any?>): String {
            if (args.isEmpty()) return template
            var s = template
            args.forEachIndexed { i, a -> s = s.replace("{$i}", a.toString()) }
            return s
        }
    }
}
