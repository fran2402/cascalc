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

    private fun spelled(text: String) = ListFamily.spelledOut(row(text))?.let { MathCodec.encode(it).replace("'", "").replace(";", "") }

    @Test fun rangeStepsByOne() = assertEquals(listOf("y=(1)x", "y=(2)x", "y=(3)x", "y=(4)x"), rows("y=[1...4]x"))
    @Test fun rangeWithAStep() = assertEquals(listOf("(1)", "(3)", "(5)", "(7)"), rows("y=[1,3,...7]")!!.map { it.removePrefix("y=") })
    @Test fun rangeDownwards() = assertEquals(3, rows("y=[3...1]")!!.size)
    @Test fun ellipsisCharacter() = assertEquals(5, rows("y=[0…4]")!!.size)
    @Test fun comprehension() = assertEquals(listOf("y=((1)x)", "y=((2)x)", "y=((3)x)"), rows("y=[nxforn=[1...3]]"))
    @Test fun pointsFromAComprehension() = assertEquals("[((1),(1)(1)),((2),(2)(2))]", spelled("[(n,nn)forn=[1,2]]"))
    @Test fun plainPointListsAreLeftAlone() = assertNull(spelled("[(1,2),(3,4)]"))
    @Test fun tooLongIsRefused() = assertNull(rows("y=[1...1000]"))

    @Test fun theLineItselfIsUntouched() {
        val r = row("y=[1,2]x")
        val before = r.items.map { it.parent }
        ListFamily.expand(r)
        assertEquals(before, r.items.map { it.parent })
    }
}
