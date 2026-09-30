package com.example.cas

import com.example.cas.graph.FolderTree
import com.example.cas.graph.FolderTree.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderTreeTest {
    private fun line(d: Int) = Item(d, false)
    private fun folder(d: Int, closed: Boolean = false) = Item(d, true, closed)

    // F(0) a(1) G(1) b(2) c(2) d(0) H(0) e(1)
    private val list = listOf(folder(0), line(1), folder(1), line(2), line(2), line(0), folder(0), line(1))

    @Test fun linesAreInTheirFolders() {
        assertEquals(listOf(0), FolderTree.enclosing(list, 1))
        assertEquals(listOf(0, 2), FolderTree.enclosing(list, 3))
        assertEquals(listOf(6), FolderTree.enclosing(list, 7))
        // d is at depth 0: outside F although it comes after it.
        assertEquals(emptyList<Int>(), FolderTree.enclosing(list, 5))
    }

    @Test fun aFolderHoldsOnlyWhatsDeeper() {
        assertEquals(listOf(1, 2, 3, 4), FolderTree.members(list, 0))
        assertEquals(listOf(3, 4), FolderTree.members(list, 2))
        assertEquals(listOf(7), FolderTree.members(list, 6))
    }

    @Test fun aNewLineAtTheEndIsOutsideEveryFolder() {
        val added = list + line(0)
        assertEquals(emptyList<Int>(), FolderTree.enclosing(added, added.lastIndex))
    }

    @Test fun normalizeClampsDepths() {
        // A line can't be deeper than one inside the folder above it.
        assertEquals(listOf(0, 1, 0, 0), FolderTree.normalize(listOf(folder(0), line(3), line(0), line(2))))
    }

    @Test fun aClosedFolderMovesAsABlockAndTakesNothingWithIt() {
        // x(0) F(0, closed) a(1) b(1) y(0): moving F up past x keeps a and b in it, y outside.
        val items = listOf(line(0), folder(0, closed = true), line(1), line(1), line(0))
        val up = FolderTree.step(items, 1, -1)!!
        assertEquals(listOf(1, 2, 3, 0, 4), up.order)
        assertEquals(listOf(0, 1, 1, 0, 0), up.depths)
        // Moving x down jumps over the whole closed folder.
        val down = FolderTree.step(items, 0, 1)!!
        assertEquals(listOf(1, 2, 3, 0, 4), down.order)
        assertEquals(listOf(0, 1, 1, 0, 0), down.depths)
    }

    @Test fun linesGoInAndOutOfOpenFolders() {
        // x(0) F(0) a(1): x moved down goes into F as its first line.
        val items = listOf(line(0), folder(0), line(1))
        val inside = FolderTree.step(items, 0, 1)!!
        assertEquals(listOf(1, 0, 2), inside.order)
        assertEquals(listOf(0, 1, 1), inside.depths)
        // F(0) a(1) y(0): a moved down leaves F.
        val out = FolderTree.step(listOf(folder(0), line(1), line(0)), 1, 1)!!
        assertEquals(listOf(0, 2, 1), out.order)
        assertEquals(listOf(0, 0, 0), out.depths)
        assertNull(FolderTree.step(items, 2, 1))
    }

    @Test fun nestAndUnnest() {
        // F(0) x(0): x goes into F; then back out, after F's block.
        val items = listOf(folder(0), line(1), line(0))
        assertTrue(FolderTree.canNest(items, 2))
        assertEquals(listOf(0, 1, 1), FolderTree.nest(items, 2)!!.depths)
        val out = FolderTree.unnest(listOf(folder(0), line(1), line(1), line(0)), 1)!!
        assertEquals(listOf(0, 2, 1, 3), out.order)
        assertEquals(listOf(0, 1, 0, 0), out.depths)
        assertFalse(FolderTree.canNest(listOf(line(0), line(0)), 1))
    }

    @Test fun oldSavedFoldersKeepTheirLines() {
        // Before: F(level 0) a G(level 1) b c, then H(level 0) d.
        assertEquals(listOf(0, 1, 1, 2, 2, 0, 1), FolderTree.fromLegacy(listOf(0, null, 1, null, null, 0, null)))
    }
}
