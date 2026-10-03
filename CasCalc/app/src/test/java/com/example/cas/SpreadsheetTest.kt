package com.example.cas

import com.example.cas.graph.Spreadsheet
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SpreadsheetTest {
    private fun zip(vararg files: Pair<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z -> files.forEach { (n, t) -> z.putNextEntry(ZipEntry(n)); z.write(t.toByteArray()); z.closeEntry() } }
        return out.toByteArray()
    }

    private val ns = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private val rns = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

    private fun xlsx(sheet: String, strings: String = "") = zip(
        "[Content_Types].xml" to "<Types/>",
        "xl/workbook.xml" to """<workbook xmlns="$ns" xmlns:r="$rns"><sheets><sheet name="Data" sheetId="1" r:id="rId1"/></sheets></workbook>""",
        "xl/_rels/workbook.xml.rels" to """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="x" Target="worksheets/sheet1.xml"/></Relationships>""",
        "xl/sharedStrings.xml" to """<sst xmlns="$ns">$strings</sst>""",
        "xl/worksheets/sheet1.xml" to """<worksheet xmlns="$ns"><sheetData>$sheet</sheetData></worksheet>""",
    )

    @Test fun excelWithSharedStringHeaderAndFormula() {
        val bytes = xlsx(
            """<row r="1"><c r="A1" t="s"><v>0</v></c><c r="B1" t="s"><v>1</v></c></row>
               <row r="2"><c r="A2"><v>1</v></c><c r="B2"><v>2.5</v></c></row>
               <row r="3"><c r="A3"><v>2</v></c><c r="B3"><f>B2*2</f><v>5</v></c></row>
               <row r="5"><c r="A5"><v>3</v></c><c r="B5"><v>-1E-3</v></c></row>""",
            "<si><t>time</t></si><si><r><t>hei</t></r><r><t>ght</t></r></si>",
        )
        assertEquals(Spreadsheet.Format.Xlsx, Spreadsheet.format(bytes))
        val t = Spreadsheet.parse(bytes)!!
        assertEquals(listOf("time", "height"), t.names)
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0), t.columns[0], 0.0)
        assertArrayEquals(doubleArrayOf(2.5, 5.0, -0.001), t.columns[1], 0.0)
    }

    @Test fun excelSkipsBlankColumnsAndErrors() {
        val bytes = xlsx(
            """<row r="1"><c r="B1"><v>1</v></c><c r="D1"><v>10</v></c></row>
               <row r="2"><c r="B2"><v>2</v></c><c r="D2" t="e"><v>#DIV/0!</v></c></row>
               <row r="3"><c r="B3"><v>3</v></c><c r="D3"><v>30</v></c></row>""",
        )
        val t = Spreadsheet.parse(bytes)!!
        assertArrayEquals(doubleArrayOf(1.0, 3.0), t.columns[0], 0.0)
        assertArrayEquals(doubleArrayOf(10.0, 30.0), t.columns[1], 0.0)
    }

    @Test fun openDocumentWithRepeats() {
        val content = """<?xml version="1.0"?>
            <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
              xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0" xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
            <office:body><office:spreadsheet><table:table table:name="Sheet1">
              <table:table-column table:number-columns-repeated="3"/>
              <table:table-row><table:table-cell office:value-type="string"><text:p>x</text:p></table:table-cell>
                <table:table-cell office:value-type="string"><text:p>y</text:p></table:table-cell><table:table-cell table:number-columns-repeated="1020"/></table:table-row>
              <table:table-row table:number-rows-repeated="2"><table:table-cell office:value-type="float" office:value="4"><text:p>4</text:p></table:table-cell>
                <table:table-cell office:value-type="percentage" office:value="0.5"><text:p>50%</text:p></table:table-cell></table:table-row>
              <table:table-row><table:table-cell office:value-type="float" office:value="7"/><table:table-cell office:value-type="float" office:value="-2.25"/></table:table-row>
              <table:table-row table:number-rows-repeated="1048570"><table:table-cell table:number-columns-repeated="1024"/></table:table-row>
            </table:table></office:spreadsheet></office:body></office:document-content>"""
        val bytes = zip("mimetype" to "application/vnd.oasis.opendocument.spreadsheet", "content.xml" to content)
        assertEquals(Spreadsheet.Format.Ods, Spreadsheet.format(bytes))
        val t = Spreadsheet.parse(bytes)!!
        assertEquals(listOf("x", "y"), t.names)
        assertArrayEquals(doubleArrayOf(4.0, 4.0, 7.0), t.columns[0], 0.0)
        assertArrayEquals(doubleArrayOf(0.5, 0.5, -2.25), t.columns[1], 0.0)
    }

    private fun resource(name: String) = javaClass.classLoader!!.getResourceAsStream(name)!!.use { it.readBytes() }

    @Test fun oldExcelEverySheetByName() {
        val bytes = resource("multi.xls")
        assertEquals(Spreadsheet.Format.Xls, Spreadsheet.format(bytes))
        val sheets = Spreadsheet.sheets(bytes)!!
        assertEquals(listOf("Run A", "Notes", "Run B"), sheets.map { it.name })
        val a = sheets[0].table
        assertEquals(listOf("time", "height µ"), a.names)
        // The last row's formula has no worked-out value saved, so that row is left out.
        assertArrayEquals(doubleArrayOf(0.0, 1.0, 2.0), a.columns[0], 0.0)
        assertArrayEquals(doubleArrayOf(1.5, 3.0, -0.25), a.columns[1], 0.0)
        assertEquals(0, sheets[1].table.columns.size)
        val b = sheets[2].table
        assertArrayEquals(doubleArrayOf(10.0, 20.0, 30.0, 40.0, 0.125, 123456789.0), b.columns[0], 0.0)
        assertArrayEquals(doubleArrayOf(100.0, 400.0, 900.0, 1600.0, -7.5e-3, 1e20), b.columns[1], 0.0)
        // The first sheet with numbers, for a single import.
        assertArrayEquals(a.columns[1], Spreadsheet.parse(bytes)!!.columns[1], 0.0)
    }

    @Test fun oldExcelSharedStringsAcrossContinueRecords() {
        val t = Spreadsheet.parse(resource("big.xls"))!!
        assertEquals(listOf("Zeit ✓", "Wert"), t.names)
        assertEquals(2000, t.rows)
        assertArrayEquals(DoubleArray(2000) { it + 0.5 }, t.columns[0], 0.0)
        assertArrayEquals(DoubleArray(2000) { 2.0 * it }, t.columns[1], 0.0)
    }

    @Test fun excelSeveralSheets() {
        val bytes = zip(
            "xl/workbook.xml" to """<workbook xmlns="$ns" xmlns:r="$rns"><sheets><sheet name="First" sheetId="1" r:id="rId1"/><sheet name="Second" sheetId="2" r:id="rId2"/></sheets></workbook>""",
            "xl/_rels/workbook.xml.rels" to """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId2" Target="/xl/worksheets/b.xml"/><Relationship Id="rId1" Target="worksheets/a.xml"/></Relationships>""",
            "xl/worksheets/a.xml" to """<worksheet xmlns="$ns"><sheetData><row r="1"><c r="A1"><v>1</v></c><c r="B1"><v>2</v></c></row></sheetData></worksheet>""",
            "xl/worksheets/b.xml" to """<worksheet xmlns="$ns"><sheetData><row r="1"><c r="A1" t="inlineStr"><is><t>n</t></is></c></row><row r="2"><c r="A2"><v>7</v></c></row><row r="3"><c r="A3"><v>8</v></c></row></sheetData></worksheet>""",
        )
        val sheets = Spreadsheet.sheets(bytes)!!
        assertEquals(listOf("First", "Second"), sheets.map { it.name })
        assertArrayEquals(doubleArrayOf(2.0), sheets[0].table.columns[1], 0.0)
        assertEquals(listOf("n"), sheets[1].table.names)
        assertArrayEquals(doubleArrayOf(7.0, 8.0), sheets[1].table.columns[0], 0.0)
    }

    @Test fun openDocumentSeveralSheets() {
        val content = """<office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
              xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0" xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
            <office:body><office:spreadsheet>
            <table:table table:name="One"><table:table-row><table:table-cell office:value-type="float" office:value="1"/><table:table-cell office:value-type="float" office:value="2"/></table:table-row></table:table>
            <table:table table:name="Two"><table:table-row><table:table-cell office:value-type="float" office:value="3"/><table:table-cell office:value-type="float" office:value="4"/></table:table-row></table:table>
            </office:spreadsheet></office:body></office:document-content>"""
        val sheets = Spreadsheet.sheets(zip("content.xml" to content))!!
        assertEquals(listOf("One", "Two"), sheets.map { it.name })
        assertArrayEquals(doubleArrayOf(4.0), sheets[1].table.columns[1], 0.0)
    }

    @Test fun notASpreadsheet() {
        assertEquals(null, Spreadsheet.sheets("x,y\n1,2".toByteArray()))
        assertEquals(Spreadsheet.Format.None, Spreadsheet.format("x,y\n1,2".toByteArray()))
        // A compound file with no workbook in it reads as no sheets.
        val ole = ByteArray(1024).also { b -> listOf(0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1).forEachIndexed { k, v -> b[k] = v.toByte() } }
        assertEquals(Spreadsheet.Format.Xls, Spreadsheet.format(ole))
        assertEquals(0, Spreadsheet.sheets(ole)!!.size)
    }
}
