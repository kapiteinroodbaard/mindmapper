package io.github.wlenaerts.mindmapper.core

import io.github.wlenaerts.mindmapper.core.edit.move
import io.github.wlenaerts.mindmapper.core.edit.setDuration
import io.github.wlenaerts.mindmapper.core.timing.Slot
import io.github.wlenaerts.mindmapper.core.timing.schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleTest {
    private val timed = map(topic("A"), topic("B"), topic("C"), topic("D"))
        .setDuration("A", 3).setDuration("C", 5).setDuration("D", 8)

    @Test
    fun slotsFollowMainBranchOrderAndSkipBranchesWithoutDuration() {
        val s = timed.schedule()
        assertEquals(listOf(Slot("A", 0, 180), Slot("C", 180, 480), Slot("D", 480, 960)), s.slots)
        assertEquals(960, s.totalSeconds)
    }

    @Test
    fun reorderChangesTheSchedule() {
        val s = timed.move("C", "root", 0).schedule()
        assertEquals(listOf(Slot("C", 0, 300), Slot("A", 300, 480), Slot("D", 480, 960)), s.slots)
    }

    @Test
    fun noDurations() {
        val s = map(topic("A"), topic("B")).schedule()
        assertTrue(s.isEmpty)
        assertEquals(0, s.totalSeconds)
        assertNull(s.scheduledAt(0))
    }

    @Test
    fun slotBoundaries() {
        val s = timed.schedule()
        assertEquals("A", s.scheduledAt(0))
        assertEquals("A", s.scheduledAt(179))
        assertEquals("C", s.scheduledAt(180))
        assertEquals("D", s.scheduledAt(959))
        assertNull(s.scheduledAt(960))
        assertNull(s.scheduledAt(-1))
    }
}
