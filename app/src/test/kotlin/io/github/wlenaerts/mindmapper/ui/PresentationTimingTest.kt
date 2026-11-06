package io.github.wlenaerts.mindmapper.ui

import android.os.Looper
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.wlenaerts.mindmapper.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Duration

@RunWith(AndroidJUnit4::class)
class PresentationTimingTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun newMapWithMainBranch(text: String, minutes: Int?) {
        compose.onNodeWithText("New mind map").performClick()
        compose.onAllNodesWithContentDescription("Add subtopic")[0].performClick()
        compose.onNodeWithTag("topic_text").performTextReplacement(text)
        compose.onNodeWithText("OK").performClick()
        if (minutes != null) {
            compose.onNodeWithContentDescription("Planned duration").performClick()
            compose.onNodeWithTag("duration_minutes").performTextReplacement(minutes.toString())
            compose.onNodeWithText("OK").performClick()
        }
        compose.onNodeWithContentDescription("Present").performClick()
    }

    /** Advances the system clock, which the timer reads, and waits until the timer shows [expected]. */
    private fun advanceUntilTimerShows(seconds: Long, expected: String) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds))
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithTag("timer_elapsed", useUnmergedTree = true).assertTextEquals(expected) }.isSuccess
        }
    }

    @Test
    fun noTimerWithoutDurations() {
        newMapWithMainBranch("Intro", minutes = null)
        compose.onNodeWithTag("presentation").assertExists()
        compose.onNodeWithTag("presentation_timer").assertDoesNotExist()
    }

    @Test
    fun timerStartsOnTapPausesAndResets() {
        newMapWithMainBranch("Intro", minutes = 5)
        compose.onNodeWithTag("timer_elapsed", useUnmergedTree = true).assertTextEquals("0:00")
        compose.onNodeWithTag("timer_branch", useUnmergedTree = true).assertTextEquals("Intro")
        // Paused: time passing does not count.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(30))
        compose.waitForIdle()
        compose.onNodeWithTag("timer_elapsed", useUnmergedTree = true).assertTextEquals("0:00")
        compose.onNodeWithTag("presentation_timer").performClick()
        advanceUntilTimerShows(65, "1:05")
        compose.onNodeWithTag("presentation_timer").performTouchInput { longClick() }
        compose.onNodeWithTag("timer_elapsed", useUnmergedTree = true).assertTextEquals("0:00")
    }

    @Test
    fun overTimeHidesTheBranchName() {
        newMapWithMainBranch("Intro", minutes = 1)
        compose.onNodeWithTag("presentation_timer").performClick()
        advanceUntilTimerShows(61, "1:01")
        compose.onNodeWithTag("timer_branch", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun rotationKeepsTheRunningTimer() {
        newMapWithMainBranch("Intro", minutes = 5)
        compose.onNodeWithTag("presentation_timer").performClick()
        advanceUntilTimerShows(40, "0:40")
        compose.activityRule.scenario.recreate()
        advanceUntilTimerShows(20, "1:00")
    }

    @Test
    fun presentingAgainStartsAtZero() {
        newMapWithMainBranch("Intro", minutes = 5)
        compose.onNodeWithTag("presentation_timer").performClick()
        advanceUntilTimerShows(30, "0:30")
        compose.onNodeWithContentDescription("Stop presenting").performClick()
        compose.onNodeWithContentDescription("Present").performClick()
        compose.onNodeWithTag("timer_elapsed", useUnmergedTree = true).assertTextEquals("0:00")
    }
}
