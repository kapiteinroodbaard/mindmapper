package io.github.wlenaerts.mindmapper.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
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
    fun newMindMapOpensEditorWithToolbar() {
        compose.setContent { MindmapperTheme { MindmapperApp() } }
        compose.onNodeWithText("New mind map").performClick()
        compose.onNodeWithText("Add subtopic").assertIsDisplayed()
        compose.onNodeWithContentDescription("Present").assertIsDisplayed()
    }
}
