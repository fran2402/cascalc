package com.example.cas

import com.example.cas.i18n.Translations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The translation files (assets/i18n) and how they're read. */
class TranslationsTest {
    /** The app module's folder: the tests run there, or from the project above it. */
    private val app: File = listOf(File("."), File("app"), File("CasCalc/app")).first { File(it, "src/main/assets/i18n").isDirectory }
    private fun table(code: String) = Translations.parse(File(app, "src/main/assets/i18n/$code.tsv").readText())
    private val codes = listOf("fy", "sh")

    @Test fun readsLinesEscapesAndComments() {
        val t = Translations.parse("# a comment\nSave\tBewarje\nTwo\\nlines\tTwa\\nrigels\nNo translation\t\n")
        assertEquals("Bewarje", t.lookup("Save"))
        assertEquals("Twa\nrigels", t.lookup("Two\nlines"))
        assertNull(t.lookup("No translation"))
        assertNull(t.lookup("a comment"))
    }

    @Test fun spellingVariantsAndPlaceholders() {
        assertEquals("vrijeme", Translations.resolve("{vrijeme|vreme}", 0))
        assertEquals("vreme", Translations.resolve("{vrijeme|vreme}", 1))
        // Placeholders and lone braces are left alone.
        assertEquals("Korak {0} od {1}", Translations.resolve("Korak {0} od {1}", 1))
        assertEquals("Korak 2 od 3", Translations.format("Korak {0} od {1}", arrayOf(2, 3)))
        val sh = table("sh")
        assertEquals("Historija", sh.lookup("History", 0))
        assertEquals("Istorija", sh.lookup("History", 1))
        assertEquals("Lijevo", sh.lookup("Left", 0))
        assertEquals("Levo", sh.lookup("Left", 1))
    }

    @Test fun everyLineIsWellFormed() {
        for (code in codes) {
            val file = File(app, "src/main/assets/i18n/$code.tsv")
            val keys = HashSet<String>()
            file.readLines().filter { it.isNotBlank() && !it.startsWith("#") }.forEach { line ->
                assertTrue("$code: no tab in \"$line\"", '\t' in line)
                assertTrue("$code: more than one tab in \"$line\"", line.count { it == '\t' } == 1)
                assertTrue("$code: \"${line.substringBefore('\t')}\" twice", keys.add(line.substringBefore('\t')))
            }
        }
    }

    @Test fun translationsKeepPlaceholdersAndResolveCleanly() {
        val placeholder = Regex("""\{\d}""")
        for (code in codes) {
            val t = table(code)
            for ((english, _) in t.entries) for (variant in 0..1) {
                val shown = t.lookup(english, variant)!!
                assertEquals("$code: placeholders in \"$english\"", placeholder.findAll(english).map { it.value }.toSet(), placeholder.findAll(shown).map { it.value }.toSet())
                assertTrue("$code: unresolved variant in \"$shown\"", '|' !in shown || '|' in english)
                assertTrue("$code: \$ signs differ in \"$english\"", english.count { it == '$' } == shown.count { it == '$' })
            }
        }
    }

    /** Each English line is a string the app shows, so a typo or a renamed string is caught. */
    @Test fun everyKeyIsInTheApp() {
        val source = File(app, "src/main/java/com/example/cas/ui").listFiles()!!.filter { it.extension == "kt" }.joinToString("\n") { it.readText() }
        fun kotlin(s: String, dollars: Boolean) = buildString {
            for (c in s) when (c) {
                '\\' -> append("\\\\"); '"' -> append("\\\""); '\n' -> append("\\n")
                '$' -> append(if (dollars) "\\$" else "$")
                else -> append(c)
            }
        }
        for (code in codes) for (english in table(code).entries.keys) {
            assertTrue("$code: \"$english\" isn't a string in the app", "\"${kotlin(english, true)}\"" in source || "\"${kotlin(english, false)}\"" in source)
        }
    }

    @Test fun bothLanguagesCoverTheSameStrings() {
        assertEquals(table("fy").entries.keys, table("sh").entries.keys)
    }
}
