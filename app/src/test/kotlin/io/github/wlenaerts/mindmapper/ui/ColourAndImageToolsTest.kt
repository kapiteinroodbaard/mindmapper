package io.github.wlenaerts.mindmapper.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.geometry.Offset
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.wlenaerts.mindmapper.core.layout.Rect
import io.github.wlenaerts.mindmapper.core.layout.TopicBox
import io.github.wlenaerts.mindmapper.core.model.BranchStyle
import io.github.wlenaerts.mindmapper.core.model.Side
import io.github.wlenaerts.mindmapper.core.view.Viewport
import io.github.wlenaerts.mindmapper.render.MapRenderer
import io.github.wlenaerts.mindmapper.render.MapTheme
import io.github.wlenaerts.mindmapper.ui.editor.BranchStyleDialog
import io.github.wlenaerts.mindmapper.ui.editor.ColorSwatches
import io.github.wlenaerts.mindmapper.ui.editor.ImageTools
import io.github.wlenaerts.mindmapper.ui.editor.TopicActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ColourAndImageToolsTest {
    @get:Rule
    val compose = createComposeRule()

    private val box = TopicBox("a", "root", Rect(200f, 300f, 400f, 460f), depth = 2, side = Side.RIGHT, colorSlot = 0)

    private fun showTools(image: ImageTools?) = compose.setContent {
        TopicActions(
            box = box, viewport = Viewport(1f, 0f, 0f), radial = true,
            onAddChild = {}, onAddSibling = {}, onEditText = {}, onImage = {}, onIcon = {}, onStyle = {}, onDuration = {}, onDelete = {},
            image = image,
        )
    }

    @Test
    fun deleteButtonRemovesTheImageAndHandlesSitOnTheCorners() {
        var deleted = false
        showTools(ImageTools(Rect(220f, 310f, 380f, 390f), 160f, 2f, onDelete = { deleted = true }, onResize = {}, onResizeEnd = {}))
        compose.onNodeWithTag("image_delete").performClick()
        assertTrue(deleted)
        compose.onAllNodesWithContentDescription("Resize image").assertCountEquals(4)
        // A topic with an image offers no image action in its action bar.
        compose.onAllNodesWithContentDescription("Image").assertCountEquals(0)
    }

    @Test
    fun topicWithoutImageHasImageActionAndNoTools() {
        showTools(null)
        compose.onAllNodesWithContentDescription("Image").assertCountEquals(1)
        compose.onAllNodesWithContentDescription("Resize image").assertCountEquals(0)
    }

    @Test
    fun draggingTheBottomRightHandleOutwardGrowsAndEndsOnce() {
        val widths = mutableListOf<Float>()
        var ends = 0
        showTools(ImageTools(Rect(220f, 310f, 380f, 390f), 160f, 2f, onDelete = {}, onResize = { widths += it }, onResizeEnd = { ends++ }))
        compose.onNodeWithTag("image_handle_bottom_right").performTouchInput {
            swipe(center, center + Offset(80f, 40f), durationMillis = 300)
        }
        compose.waitForIdle()
        assertTrue("resized: $widths", widths.isNotEmpty() && widths.last() > 160f)
        assertEquals(1, ends)
    }

    @Test
    fun pickingABranchColourReportsIt() {
        var picked: Int? = -1
        val renderer = MapRenderer(ApplicationProvider.getApplicationContext())
        compose.setContent {
            BranchStyleDialog(
                current = BranchStyle.STANDARD, color = 0xFF2E86AB.toInt(), customColor = null, theme = MapTheme.Light,
                renderer = renderer, onPick = {}, onColor = { picked = it }, onDismiss = {},
            )
        }
        compose.onNodeWithTag("branch_colour_3").performClick()
        assertEquals(ColorSwatches[3], picked)
        compose.onNodeWithTag("branch_colour_auto").performClick()
        assertEquals(null, picked)
    }
}
