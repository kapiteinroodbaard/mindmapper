package io.github.wlenaerts.mindmapper.editor

import io.github.wlenaerts.mindmapper.ui.presentation.PresentationTimer
import io.github.wlenaerts.mindmapper.ui.presentation.formatElapsed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresentationTimerTest {
    private var now = 1_000_000L
    private val timer = PresentationTimer { now }

    @Test
    fun startsPausedAtZero() {
        assertFalse(timer.state.value.running)
        now += 5_000
        assertEquals(0, timer.elapsedMillis())
    }

    @Test
    fun pauseAndResumeContinueFromThePausedTime() {
        timer.toggle()
        now += 130_000
        timer.toggle()
        assertEquals(130_000, timer.elapsedMillis())
        now += 60_000
        assertEquals(130_000, timer.elapsedMillis())
        timer.toggle()
        now += 2_000
        assertTrue(timer.state.value.running)
        assertEquals(132_000, timer.elapsedMillis())
    }

    @Test
    fun resetGoesBackToZeroPaused() {
        timer.toggle()
        now += 372_000
        timer.reset()
        assertFalse(timer.state.value.running)
        now += 1_000
        assertEquals(0, timer.elapsedMillis())
    }

    @Test
    fun elapsedFormat() {
        assertEquals("0:00", formatElapsed(0))
        assertEquals("6:12", formatElapsed(372))
        assertEquals("59:59", formatElapsed(3599))
        assertEquals("1:00:05", formatElapsed(3605))
    }
}
