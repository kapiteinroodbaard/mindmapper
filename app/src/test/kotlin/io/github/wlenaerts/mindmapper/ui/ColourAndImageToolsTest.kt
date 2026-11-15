package io.github.wlenaerts.mindmapper.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    private fun showTools(image: ImageTools?, box: TopicBox = this.box) = compose.setContent {
        TopicActions(
            box = box, viewport = Viewport(1f, 0f, 0f), radial = true,
            onAddChild = {}, onAddSibling = {}, onEditText = {}, onImage = {}, onIcon = {}, onStyle = {}, onDuration = {}, onDelete = {},
            image = image,
        )
    }

    @Test
    fun deleteButtonRemovesTheImageAndHandlesSitOnTheCorners() {
        var deleted = false
        showTools(ImageTools(Rect(220f, 310f, 380f, 390f), 160f, listOf(1), onDelete = { deleted = true }, onResize = {}, onResizeEnd = {}))
        compose.onNodeWithTag("image_delete").performClick()
        assertTrue(deleted)
        compose.onAllNodesWithContentDescription("Resize image").assertCountEquals(2)
        compose.onNodeWithTag("image_handle_top_right").assertExists()
        compose.onNodeWithTag("image_handle_bottom_right").assertExists()
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
        showTools(ImageTools(Rect(220f, 310f, 380f, 390f), 160f, listOf(1), onDelete = {}, onResize = { widths += it }, onResizeEnd = { ends++ }))
        compose.onNodeWithTag("image_handle_bottom_right").performTouchInput {
            swipe(center, center + Offset(80f, 40f), durationMillis = 300)
        }
        compose.waitForIdle()
        assertTrue("resized: $widths", widths.isNotEmpty() && widths.last() > 160f)
        assertEquals(1, ends)
    }

    @Test
    fun leftSideTopicHasHandlesOnItsLeftCornersOnly() {
        val left = box.copy(side = Side.LEFT)
        showTools(ImageTools(Rect(220f, 310f, 380f, 390f), 160f, ImageTools.outwardDirections(left), {}, {}, {}), left)
        compose.onAllNodesWithContentDescription("Resize image").assertCountEquals(2)
        compose.onNodeWithTag("image_handle_top_left").assertExists()
        compose.onNodeWithTag("image_handle_bottom_left").assertExists()
    }

    @Test
    fun centralTopicHasFourHandles() {
        val root = box.copy(depth = 0, parentId = null)
        showTools(ImageTools(Rect(220f, 310f, 380f, 390f), 160f, ImageTools.outwardDirections(root), {}, {}, {}), root)
        compose.onAllNodesWithContentDescription("Resize image").assertCountEquals(4)
    }

    /**
     * Shows a right-side topic whose image re-lays out on every resize: anchored on its left edge ([centred]
     * false, the edge moves by the full width change) or centred (it moves by half). Returns the current rect.
     */
    private fun showResizableTools(centred: Boolean): () -> Rect {
        var current = Rect(0f, 0f, 0f, 0f)
        val host = TopicBox("a", "root", Rect(20f, 300f, 220f, 460f), depth = 2, side = Side.RIGHT, colorSlot = 0)
        compose.setContent {
            var width by remember { mutableStateOf(160f) }
            val rect = if (centred) Rect(120f - width / 2, 310f, 120f + width / 2, 390f) else Rect(40f, 310f, 40f + width, 390f)
            current = rect
            TopicActions(
                box = host, viewport = Viewport(1f, 0f, 0f), radial = true,
                onAddChild = {}, onAddSibling = {}, onEditText = {}, onImage = {}, onIcon = {}, onStyle = {}, onDuration = {}, onDelete = {},
                image = ImageTools(rect, width, listOf(1), onDelete = {}, onResize = { width = it }, onResizeEnd = {}),
            )
        }
        return { current }
    }

    /** Drags the bottom-right handle by ([dx], [dy]) in small steps, letting the layout follow after each. */
    private fun dragHandle(dx: Float, dy: Float, steps: Int = 25) {
        val handle = compose.onNodeWithTag("image_handle_bottom_right")
        handle.performTouchInput { down(center) }
        repeat(steps) {
            handle.performTouchInput { moveBy(Offset(dx / steps, dy / steps)) }
            compose.waitForIdle()
        }
        // Small last steps, as a finger coming to rest makes, let a half-width edge catch up.
        repeat(6) {
            handle.performTouchInput { moveBy(Offset(if (it % 2 == 0) 0.5f else -0.5f, 0f)) }
            compose.waitForIdle()
        }
        handle.performTouchInput { up() }
        compose.waitForIdle()
    }

    @Test
    fun handleFollowsTheFingerWhenTheEdgeMovesByTheFullWidthChange() {
        val rect = showResizableTools(centred = false)
        val start = rect().right
        dragHandle(100f, 0f)
        assertEquals(start + 100f, rect().right, 4f)
    }

    @Test
    fun handleFollowsTheFingerWhenTheImageIsCentred() {
        val rect = showResizableTools(centred = true)
        val start = rect().right
        dragHandle(100f, 0f)
        assertEquals(start + 100f, rect().right, 4f)
    }

    @Test
    fun verticalMovementAloneDoesNotResize() {
        val rect = showResizableTools(centred = false)
        val start = rect().width
        dragHandle(0f, 60f)
        assertEquals(start, rect().width, 0.5f)
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
