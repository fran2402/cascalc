package com.example.cas.graph

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Reads data points from a spreadsheet for the 2D graph: Excel workbooks (.xlsx, .xlsm) and
 * OpenDocument ones (.ods), which is also what Google Sheets downloads as, besides .xlsx and
 * .csv. The first sheet with numbers in it is read; formulas give the value they were last
 * worked out to. The rows go through the same rules as a CSV file ([Csv.fromCells]).
 */
object Spreadsheet {
    enum class Format { Xlsx, Ods, Xls, None }

    /** What the file is, from its first bytes (and, for zips, what's inside). */
    fun format(bytes: ByteArray): Format = when {
        bytes.size >= 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() -> {
            val names = entries(bytes).keys
            when {
                "xl/workbook.xml" in names -> Format.Xlsx
                "content.xml" in names -> Format.Ods
                else -> Format.None
            }
        }
        // The old binary .xls (an OLE compound file).
        bytes.size >= 8 && bytes.take(8).map { it.toInt() and 0xFF } == listOf(0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1) -> Format.Xls
        else -> Format.None
    }

    /** The table in a .xlsx or .ods file, or null if it isn't one. */
    fun parse(bytes: ByteArray): Csv.Table? {
        val files = entries(bytes)
        val sheets = when {
            "xl/workbook.xml" in files -> xlsxSheets(files)
            "content.xml" in files -> odsSheets(files.getValue("content.xml"))
            else -> return null
        }
        val tables = sheets.map { Csv.fromCells(withoutEmptyColumns(it), ::number) }
        return tables.firstOrNull { it.columns.isNotEmpty() } ?: Csv.Table(emptyList(), emptyList())
    }

    private fun number(s: String): Double? =
        s.trim().replace("−", "-").replace(" ", "").toDoubleOrNull()?.takeIf { it.isFinite() }

    /** Columns with nothing in them are dropped, so a blank column between x and y doesn't matter. */
    private fun withoutEmptyColumns(rows: List<List<String>>): List<List<String>> {
        val width = rows.maxOfOrNull { it.size } ?: 0
        val used = (0 until width).filter { j -> rows.any { it.getOrNull(j)?.isNotBlank() == true } }
        return rows.map { r -> used.map { r.getOrElse(it) { "" } } }
    }

    private fun entries(bytes: ByteArray): Map<String, ByteArray> {
        val out = HashMap<String, ByteArray>()
        runCatching {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                while (true) {
                    val e = zip.nextEntry ?: break
                    // Only the parts read here; images and the like are skipped.
                    if (!e.isDirectory && (e.name.endsWith(".xml") || e.name.endsWith(".rels"))) out[e.name.removePrefix("/")] = zip.readBytes()
                }
            }
        }
        return out
    }

    private fun xml(bytes: ByteArray): Element {
        val f = DocumentBuilderFactory.newInstance()
        f.isNamespaceAware = true
        runCatching { f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        f.isExpandEntityReferences = false
        return f.newDocumentBuilder().parse(ByteArrayInputStream(bytes)).documentElement
    }

    private fun Element.children(name: String): List<Element> {
        val out = ArrayList<Element>()
        var n = firstChild
        while (n != null) { if (n is Element && n.localName == name) out += n; n = n.nextSibling }
        return out
    }

    private fun Element.descendants(name: String): List<Element> {
        val list = getElementsByTagNameNS("*", name)
        return (0 until list.length).map { list.item(it) as Element }
    }

    private fun Element.attr(name: String): String? =
        (0 until attributes.length).map { attributes.item(it) }.firstOrNull { (it.localName ?: it.nodeName) == name }?.nodeValue

    // --- Excel ---

    /** The sheets in workbook order, each as rows of cells. */
    private fun xlsxSheets(files: Map<String, ByteArray>): List<List<List<String>>> {
        val strings = files["xl/sharedStrings.xml"]?.let { b ->
            xml(b).children("si").map { si -> si.descendants("t").filter { it.parentNode.localName != "rPh" }.joinToString("") { it.textContent } }
        } ?: emptyList()
        val targets = runCatching {
            val rels = files["xl/_rels/workbook.xml.rels"]?.let { xml(it).children("Relationship").associate { r -> r.attr("Id") to r.attr("Target") } } ?: emptyMap()
            xml(files.getValue("xl/workbook.xml")).descendants("sheet").mapNotNull { s -> rels[s.attr("id")] }.map { t ->
                if (t.startsWith("/")) t.removePrefix("/") else "xl/" + t
            }
        }.getOrDefault(emptyList()).filter { it in files }
            .ifEmpty { files.keys.filter { Regex("xl/worksheets/sheet\\d+\\.xml").matches(it) }.sortedBy { it.filter(Char::isDigit).toInt() } }
        return targets.map { xlsxSheet(xml(files.getValue(it)), strings) }
    }

    private fun xlsxSheet(sheet: Element, strings: List<String>): List<List<String>> {
        val rows = sortedMapOf<Int, MutableMap<Int, String>>()
        var nextRow = 0
        for (row in sheet.descendants("row")) {
            val r = row.attr("r")?.toIntOrNull()?.minus(1) ?: nextRow
            nextRow = r + 1
            if (r > MAX_ROWS) break
            val cells = rows.getOrPut(r) { HashMap() }
            var nextColumn = 0
            for (c in row.children("c")) {
                val col = c.attr("r")?.let { columnOf(it) } ?: nextColumn
                nextColumn = col + 1
                if (col > MAX_COLUMNS) continue
                val v = c.children("v").firstOrNull()?.textContent
                cells[col] = when (c.attr("t")) {
                    "s" -> v?.trim()?.toIntOrNull()?.let { strings.getOrNull(it) } ?: ""
                    "inlineStr" -> c.descendants("t").joinToString("") { it.textContent }
                    "b" -> if (v?.trim() == "1") "1" else "0"
                    // An error (#DIV/0!) is a word, so its row is left out.
                    else -> v ?: ""
                }
            }
        }
        return denseRows(rows)
    }

    /** The column of a cell reference: "B3" → 1. */
    private fun columnOf(ref: String): Int? {
        var n = 0
        var k = 0
        while (k < ref.length && ref[k].isLetter()) { n = n * 26 + (ref[k].uppercaseChar() - 'A' + 1); k++ }
        return if (k == 0) null else n - 1
    }

    // --- OpenDocument ---

    private fun odsSheets(content: ByteArray): List<List<List<String>>> =
        xml(content).descendants("table").filter { it.namespaceURI?.contains("table") == true }.map { odsSheet(it) }

    private fun odsSheet(table: Element): List<List<String>> {
        val rows = sortedMapOf<Int, MutableMap<Int, String>>()
        var r = 0
        for (row in table.descendants("table-row")) {
            if (r > MAX_ROWS) break
            val cells = HashMap<Int, String>()
            var col = 0
            var n: Node? = row.firstChild
            while (n != null) {
                if (n is Element && (n.localName == "table-cell" || n.localName == "covered-table-cell")) {
                    val repeat = n.attr("number-columns-repeated")?.toIntOrNull() ?: 1
                    val text = odsValue(n)
                    // Blank cells are only counted (a sheet ends in a run of thousands of them).
                    if (text.isNotEmpty()) for (k in 0 until minOf(repeat, MAX_COLUMNS - col + 1)) cells[col + k] = text
                    col += repeat
                }
                n = n.nextSibling
            }
            val repeat = row.attr("number-rows-repeated")?.toIntOrNull() ?: 1
            if (cells.isNotEmpty()) for (k in 0 until minOf(repeat, MAX_ROWS - r + 1)) rows[r + k] = cells
            r += repeat
        }
        return denseRows(rows)
    }

    private fun odsValue(cell: Element): String = when (cell.attr("value-type")) {
        "float", "percentage", "currency" -> cell.attr("value") ?: ""
        "boolean" -> if (cell.attr("boolean-value") == "true") "1" else "0"
        else -> cell.children("p").joinToString("\n") { it.textContent }
    }

    private fun denseRows(rows: Map<Int, Map<Int, String>>): List<List<String>> = rows.values.map { cells ->
        val width = (cells.keys.maxOrNull() ?: -1) + 1
        List(width) { cells[it] ?: "" }
    }

    private const val MAX_ROWS = 200_000
    private const val MAX_COLUMNS = 1_000
}
