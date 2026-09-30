package com.example.cas.graph

/**
 * CasCalc's own graph files: .g2d (2D graphs), .g3d (3D graphs) and .gcp (complex plots). A
 * file holds one graph as saved (its lines, colors, styles, notes, sliders and ranges), so it
 * opens again exactly as it was, on this device or another.
 *
 * Plain UTF-8 text: a first line naming the format and version, then one entry per line,
 * `key<TAB>value`, with backslash, tab and newline in values written as \\, \t and \n.
 * `kind` and `name` come first; the rest is the graph's data.
 */
object GraphFile {
    private const val MAGIC = "CasCalc graph"
    private const val VERSION = 1

    /** The three kinds of graph, each with its file extension. */
    enum class Kind(val extension: String, val label: String) {
        Graph2D("g2d", "2D graph"), Graph3D("g3d", "3D graph"), Complex("gcp", "Complex plot");

        val mime: String get() = "application/octet-stream"

        companion object {
            fun ofExtension(ext: String): Kind? = entries.firstOrNull { it.extension.equals(ext, ignoreCase = true) }
        }
    }

    data class Contents(val kind: Kind, val name: String, val data: Map<String, String>)

    fun write(c: Contents): ByteArray = buildString {
        append("$MAGIC $VERSION\n")
        append("kind\t${c.kind.extension}\n")
        append("name\t${escape(c.name)}\n")
        c.data.toSortedMap().forEach { (k, v) -> append(escape(k)).append('\t').append(escape(v)).append('\n') }
    }.toByteArray(Charsets.UTF_8)

    /**
     * Reads a file; [fileName] (if known) gives the kind when the file doesn't say. Throws
     * [IllegalArgumentException] with a readable reason for anything that isn't a graph file.
     */
    fun read(bytes: ByteArray, fileName: String? = null): Contents {
        val text = bytes.toString(Charsets.UTF_8).removePrefix("﻿")
        val lines = text.split('\n').map { it.removeSuffix("\r") }
        val head = lines.firstOrNull().orEmpty()
        require(head.startsWith(MAGIC)) { "This isn't a CAS Scientific Calculator graph file" }
        val version = head.removePrefix(MAGIC).trim().toIntOrNull() ?: 0
        require(version in 1..VERSION) { "This graph file is from a newer version of the app" }
        val entries = LinkedHashMap<String, String>()
        lines.drop(1).filter { it.isNotEmpty() }.forEach { line ->
            val tab = line.indexOf('\t')
            require(tab > 0) { "The graph file is damaged" }
            entries[unescape(line.substring(0, tab))] = unescape(line.substring(tab + 1))
        }
        val kind = entries.remove("kind")?.let { Kind.ofExtension(it) }
            ?: fileName?.substringAfterLast('.', "")?.let { Kind.ofExtension(it) }
            ?: throw IllegalArgumentException("The graph file doesn't say what kind of graph it is")
        val name = entries.remove("name")?.takeIf { it.isNotBlank() }
            ?: fileName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
            ?: kind.label
        require("functions" in entries) { "The graph file has no lines" }
        return Contents(kind, name, entries)
    }

    private fun escape(s: String) = buildString {
        s.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\"); '\t' -> append("\\t"); '\n' -> append("\\n"); '\r' -> append("\\r")
                else -> append(ch)
            }
        }
    }

    private fun unescape(s: String) = buildString {
        var i = 0
        while (i < s.length) {
            val ch = s[i]
            if (ch == '\\' && i + 1 < s.length) {
                when (s[i + 1]) { 'n' -> append('\n'); 't' -> append('\t'); 'r' -> append('\r'); else -> append(s[i + 1]) }
                i += 2
            } else { append(ch); i++ }
        }
    }
}
