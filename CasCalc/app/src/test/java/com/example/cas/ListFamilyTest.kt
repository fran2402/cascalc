package com.example.cas

import com.example.cas.editor.ListFamily
import com.example.cas.editor.MathCodec
import com.example.cas.editor.row
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListFamilyTest {
    private fun rows(text: String) = ListFamily.expand(row(text))?.map { MathCodec.encode(it).replace("'", "").replace(";", "") }

    @Test fun eachEntryIsALine() = assertEquals(listOf("y=(1)x", "y=(2)x", "y=(3)x"), rows("y=[1,2,3]x"))
    @Test fun entriesCanBeExpressions() = assertEquals(listOf("(x+1)²", "(x−a)²"), rows("[x+1,x−a]²"))
    @Test fun aListOfPointsIsNotAFamily() = assertNull(rows("[(1,2),(3,4)]"))
    @Test fun noList() = assertNull(rows("y=x"))
    @Test fun pointsInsideEntriesStayWhole() = assertEquals(2, rows("y=[(1),(2)]")!!.size)
}
