package io.github.wlenaerts.mindmapper.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasTestTag
import androidx.lifecycle.ViewModelProvider
import io.github.wlenaerts.mindmapper.ui.start.StartViewModel
import androidx.compose.ui.test.hasText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.wlenaerts.mindmapper.MindmapperApplication
import io.github.wlenaerts.mindmapper.ui.theme.MindmapperTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w1280dp-h800dp-land")
class StartScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun recentFileIsListedAndCanBeRemoved() {
        compose.setContent { MindmapperTheme { MindmapperApp() } }
        // Use the application the view model sees: AndroidX caches the view model factory (and its
        // Application) across Robolectric tests, so it can differ from compose.activity.application.
        val vm = ViewModelProvider(compose.activity)[StartViewModel::class.java]
        vm.getApplication<MindmapperApplication>().recentFiles.touch("content://example/doc/1", "Plan.mindmap")
        compose.waitForIdle()
        compose.onNode(hasTestTag("recent_Plan.mindmap"), useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("More options for Plan.mindmap", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Remove from list").performClick()
        compose.waitForIdle()
        compose.onNode(hasText("Mind maps you open or save appear here."), useUnmergedTree = true).assertExists()
    }

    @Test
    fun newMindMapShowsCentralTopicActionsOnTheMap() {
        compose.setContent { MindmapperTheme { MindmapperApp() } }
        compose.onNodeWithText("New mind map").performClick()
        // Radial layout: a subtopic handle on both sides of the central topic.
        compose.onAllNodesWithContentDescription("Add subtopic").assertCountEquals(2)
        compose.onNodeWithContentDescription("Central topic style").assertIsDisplayed()
        compose.onNodeWithContentDescription("Delete").assertDoesNotExist()
        compose.onNodeWithContentDescription("Add topic after").assertDoesNotExist()
        // No toolbar: its buttons had visible labels.
        compose.onNodeWithText("Add subtopic").assertDoesNotExist()
        compose.onNodeWithContentDescription("Present").assertIsDisplayed()
    }

    @Test
    fun styleIsNotInTheOverflowMenuButOnTheCentralTopic() {
        compose.setContent { MindmapperTheme { MindmapperApp() } }
        compose.onNodeWithText("New mind map").performClick()
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Export as PDF").assertIsDisplayed()
        compose.onNodeWithText("Central topic style").assertDoesNotExist()
        compose.onNodeWithText("Export as PDF").performClick()
        compose.onNodeWithText("Cancel", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun centralTopicStyleDialogHasTrunkSettings() {
        compose.setContent { MindmapperTheme { MindmapperApp() } }
        compose.onNodeWithText("New mind map").performClick()
        compose.onNodeWithContentDescription("Central topic style").performClick()
        compose.onNodeWithText("Trunk width").assertIsDisplayed()
        compose.onNodeWithText("Trunk taper").assertExists()
    }

    @Test
    fun mainTopicHasBranchStylePickerAndSubtopicHasNone() {
        compose.setContent { MindmapperTheme { MindmapperApp() } }
        compose.onNodeWithText("New mind map").performClick()
        compose.onAllNodesWithContentDescription("Add subtopic")[0].performClick()
        compose.onNodeWithText("OK").performClick()
        // The new main topic is selected: style and delete.
        compose.onNodeWithContentDescription("Branch style").assertIsDisplayed()
        compose.onNodeWithContentDescription("Delete").assertIsDisplayed()
        compose.onNodeWithContentDescription("Branch style").performClick()
        compose.onNodeWithTag("branch_style_standard").assertIsSelected()
        compose.onNodeWithTag("branch_style_lightning").assertIsNotSelected().performClick()
        compose.onNodeWithTag("branch_style_lightning").assertIsSelected()
        compose.onNodeWithTag("branch_style_standard").assertIsNotSelected()
        compose.onNodeWithText("OK").performClick()
        // A subtopic inherits the style and has no style action.
        compose.onNodeWithContentDescription("Add subtopic").performClick()
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithContentDescription("Delete").assertIsDisplayed()
        compose.onNodeWithContentDescription("Branch style").assertDoesNotExist()
    }

    @Test
    fun durationActionOnlyOnMainTopics() {
        compose.setContent { MindmapperTheme { MindmapperApp() } }
        compose.onNodeWithText("New mind map").performClick()
        // The central topic is selected: no duration action.
        compose.onNodeWithContentDescription("Central topic style").assertIsDisplayed()
        compose.onNodeWithContentDescription("Planned duration").assertDoesNotExist()
        compose.onAllNodesWithContentDescription("Add subtopic")[0].performClick()
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithContentDescription("Planned duration").assertIsDisplayed()
        compose.onNodeWithContentDescription("Delete").assertIsDisplayed()
        compose.onNodeWithContentDescription("Add subtopic").performClick()
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithContentDescription("Delete").assertIsDisplayed()
        compose.onNodeWithContentDescription("Planned duration").assertDoesNotExist()
    }

    @Test
    fun durationDialogAcceptsOnlyTheAllowedRange() {
        compose.setContent { MindmapperTheme { MindmapperApp() } }
        compose.onNodeWithText("New mind map").performClick()
        compose.onAllNodesWithContentDescription("Add subtopic")[0].performClick()
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithContentDescription("Planned duration").performClick()
        compose.onNodeWithTag("duration_minutes").performTextReplacement("181")
        compose.onNodeWithText("OK").assertIsNotEnabled()
        compose.onNodeWithTag("duration_minutes").performTextReplacement("5")
        compose.onNodeWithText("OK").assertIsEnabled().performClick()
        // Reopening shows the stored duration; clearing it is allowed.
        compose.onNodeWithContentDescription("Planned duration").performClick()
        compose.onNodeWithTag("duration_minutes").assertTextContains("5")
        compose.onNodeWithTag("duration_minutes").performTextReplacement("")
        compose.onNodeWithText("OK").assertIsEnabled()
    }

    @Test
    fun renameDialogRejectsInvalidNames() {
        compose.setContent { MindmapperTheme { MindmapperApp() } }
        compose.onNodeWithText("New mind map").performClick()
        compose.onNodeWithContentDescription("Rename").performClick()
        compose.onNodeWithTag("map_name").performTextReplacement("")
        compose.onNodeWithText("OK").assertIsNotEnabled()
        compose.onNodeWithTag("map_name").performTextReplacement("a/b")
        compose.onNodeWithText("OK").assertIsNotEnabled()
        compose.onNodeWithTag("map_name").performTextReplacement("Plan 2026")
        compose.onNodeWithText("OK").assertIsEnabled().performClick()
        compose.onNodeWithText("Plan 2026 •").assertIsDisplayed()
    }

    @Test
    fun textDialogOffersFormatting() {
        compose.setContent { MindmapperTheme { MindmapperApp() } }
        compose.onNodeWithText("New mind map").performClick()
        compose.onNodeWithContentDescription("Edit text").performClick()
        compose.onNodeWithContentDescription("Bold").assertIsOn()
        compose.onNodeWithContentDescription("Italic").assertIsOff().performClick().assertIsOn()
        compose.onNodeWithContentDescription("Underline").assertIsOff()
    }
}
