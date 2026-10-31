package io.github.wlenaerts.mindmapper.core

import io.github.wlenaerts.mindmapper.core.format.ImageScaling
import io.github.wlenaerts.mindmapper.core.recent.RecentFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentFilesTest {

    @Test
    fun mostRecentFirstAndLimit() {
        var r = RecentFiles()
        for (i in 1..16) r = r.touch("u$i", "f$i", i.toLong())
        assertEquals(15, r.ordered.size)
        assertEquals("u16", r.ordered.first().uri)
        assertTrue(r.ordered.none { it.uri == "u1" })
    }

    @Test
    fun touchMovesToTopWithoutDuplicates() {
        val r = RecentFiles().touch("a", "a", 1).touch("b", "b", 2).touch("a", "a", 3)
        assertEquals(listOf("a", "b"), r.ordered.map { it.uri })
    }

    @Test
    fun pinnedStayOnTopAndAreNeverDropped() {
        var r = RecentFiles().touch("old", "old", 0).setPinned("old", true)
        for (i in 1..30) r = r.touch("u$i", "f$i", i.toLong())
        assertEquals("old", r.ordered.first().uri)
        assertEquals(16, r.ordered.size)
        r = r.touch("old", "old", 100)
        assertTrue("touch keeps pin", r.ordered.first().pinned)
        r = r.setPinned("old", false)
        assertEquals(15, r.ordered.size)
    }

    @Test
    fun removeAndPersist() {
        val r = RecentFiles().touch("a", "a", 1).touch("b", "b", 2).setPinned("a", true).remove("b")
        assertEquals(r.entries, RecentFiles.decode(r.encode()).entries)
        assertEquals(listOf("a"), r.ordered.map { it.uri })
        assertTrue(RecentFiles.decode("garbage").entries.isEmpty())
    }

    @Test
    fun embeddedImageSize() {
        assertEquals(1600 to 1200, ImageScaling.embeddedSize(4000, 3000))
        assertEquals(900 to 1600, ImageScaling.embeddedSize(1800, 3200))
        assertEquals(800 to 600, ImageScaling.embeddedSize(800, 600))
    }
}
