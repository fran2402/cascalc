package com.example.cas

import com.example.cas.graph.FolderTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderTreeTest {
    // 0: folder A, 1: line, 2: folder B inside A, 3: line, 4: line, 5: folder C at the top, 6: line, 7: line outside all? (no: in C)
    private val list = listOf(0, null, 1, null, null, 0, null)

    @Test fun linesAreInTheirFolders() {
        assertEquals(listOf(0), FolderTree.enclosing(list, 1))
        assertEquals(listOf(0, 2), FolderTree.enclosing(list, 3))
        assertEquals(listOf(5), FolderTree.enclosing(list, 6))
        assertEquals(listOf(0), FolderTree.enclosing(list, 2))
    }

    @Test fun aFolderHoldsItsSubfolders() {
        assertEquals(listOf(1, 2, 3, 4), FolderTree.members(list, 0))
        assertEquals(listOf(3, 4), FolderTree.members(list, 2))
        assertEquals(listOf(6), FolderTree.members(list, 5))
    }

    @Test fun nestingNeedsAFolderAbove() {
        assertFalse(FolderTree.canNest(list, 0))
        assertTrue(FolderTree.canNest(list, 5))
        // B is already inside A; one deeper needs a folder above at level 1 or more.
        assertFalse(FolderTree.canNest(list, 2))
    }
}
