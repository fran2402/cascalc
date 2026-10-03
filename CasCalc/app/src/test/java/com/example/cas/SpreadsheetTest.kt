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

    @Test fun notASpreadsheet() {
        assertEquals(Spreadsheet.Format.None, Spreadsheet.format("x,y\n1,2".toByteArray()))
        assertEquals(Spreadsheet.Format.Xls, Spreadsheet.format(byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte(), 0)))
    }
}
