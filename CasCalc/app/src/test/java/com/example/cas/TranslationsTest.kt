package com.example.cas

import com.example.cas.i18n.Translations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** How translation files are read, and the template for writing one (i18n/template.tsv). */
class TranslationsTest {
    /** The app module's folder: the tests run there, or from the project above it. */
    private val app: File = listOf(File("."), File("app"), File("CasCalc/app")).first { File(it, "src/main/java").isDirectory }
    /** The template, and any translations in the app (assets/i18n). */
    private val files: List<File> get() = listOf(File(app, "../i18n/template.tsv")) +
        (File(app, "src/main/assets/i18n").listFiles()?.filter { it.extension == "tsv" } ?: emptyList())

    @Test fun readsLinesEscapesCommentsAndName() {
        val t = Translations.parse("#name\tFrysk\n# a comment\nSave\tBewarje\nTwo\\nlines\tTwa\\nrigels\nNo translation\t\n")
        assertEquals("Frysk", t.name)
        assertEquals("Bewarje", t.lookup("Save"))
        assertEquals("Twa\nrigels", t.lookup("Two\nlines"))
        assertNull(t.lookup("No translation"))
        assertNull(t.lookup("a comment"))
    }

    @Test fun placeholders() {
        assertEquals("Stap 2 fan 3", Translations.format("Stap {0} fan {1}", arrayOf(2, 3)))
    }

    @Test fun everyLineIsWellFormed() {
        for (file in files) {
            val keys = HashSet<String>()
            file.readLines().filter { it.isNotBlank() && !it.startsWith("#") }.forEach { line ->
                assertTrue("${file.name}: one tab in \"$line\"", line.count { it == '\t' } == 1)
                assertTrue("${file.name}: \"${line.substringBefore('\t')}\" twice", keys.add(line.substringBefore('\t')))
            }
        }
    }

    @Test fun translationsKeepPlaceholders() {
        val placeholder = Regex("""\{\d}""")
        for (file in files) for ((english, shown) in Translations.parse(file.readText()).entries) {
            assertEquals("${file.name}: placeholders in \"$english\"", placeholder.findAll(english).map { it.value }.toSet(), placeholder.findAll(shown).map { it.value }.toSet())
        }
    }

    /** Each English line is a string the app shows, so a typo or a renamed string is caught. */
    @Test fun everyKeyIsInTheApp() {
        val source = listOf("ui", "graph").flatMap { File(app, "src/main/java/com/example/cas/$it").listFiles()!!.filter { f -> f.extension == "kt" } }.joinToString("\n") { it.readText() }
        fun kotlin(s: String, dollars: Boolean) = buildString {
            for (c in s) when (c) {
                '\\' -> append("\\\\"); '"' -> append("\\\""); '\n' -> append("\\n")
                '$' -> append(if (dollars) "\\$" else "$")
                else -> append(c)
            }
        }
        for (file in files) for (english in file.readLines().filter { it.isNotBlank() && !it.startsWith("#") }.map { line -> Translations.parse("$line x").entries.keys.firstOrNull() ?: line.substringBefore('\t') }) {
            assertTrue("${file.name}: \"$english\" isn't a string in the app", "\"${kotlin(english, true)}\"" in source || "\"${kotlin(english, false)}\"" in source)
        }
    }
}
