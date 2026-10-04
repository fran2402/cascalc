package com.example.cas.i18n

/**
 * One language's strings, read from a tab-separated file: each line is the English text, a
 * tab, and its translation. Lines starting with # are comments; \n and \t in either column are a
 * new line and a tab. The English is the key, so a string with no line here stays English.
 *
 * A translation can hold spelling variants as {first|second}: Serbo-Croatian writes
 * {vrijeme|vreme} for its ijekavian and ekavian forms, and [variant] picks one (0 the first).
 * Placeholders {0}, {1}… (no bar) are filled in from [format]'s arguments.
 */
class Translations(val entries: Map<String, String>) {

    /** The translation of [english] with variant [variant] picked, or null if there is none. */
    fun lookup(english: String, variant: Int = 0): String? = entries[english]?.let { resolve(it, variant) }

    companion object {
        val EMPTY = Translations(emptyMap())

        fun parse(text: String): Translations {
            val out = LinkedHashMap<String, String>()
            text.lineSequence().forEach { raw ->
                val line = raw.trimEnd('\r')
                if (line.isBlank() || line.startsWith("#")) return@forEach
                val tab = line.indexOf('\t')
                if (tab <= 0) return@forEach
                val key = unescape(line.substring(0, tab))
                val value = unescape(line.substring(tab + 1))
                if (value.isNotEmpty()) out[key] = value
            }
            return Translations(out)
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

        /** [s] with each {a|b|…} replaced by its [variant]th choice (the first if there are fewer). */
        fun resolve(s: String, variant: Int): String {
            if ('|' !in s) return s
            val out = StringBuilder()
            var k = 0
            while (k < s.length) {
                val open = s.indexOf('{', k)
                if (open < 0) { out.append(s, k, s.length); break }
                val close = s.indexOf('}', open)
                val body = if (close > open) s.substring(open + 1, close) else null
                if (body == null || '|' !in body) {
                    // Not a variant (a {0} placeholder, or a lone brace): kept as it is.
                    out.append(s, k, open + 1); k = open + 1; continue
                }
                out.append(s, k, open)
                val choices = body.split('|')
                out.append(choices.getOrElse(variant) { choices[0] })
                k = close + 1
            }
            return out.toString()
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
