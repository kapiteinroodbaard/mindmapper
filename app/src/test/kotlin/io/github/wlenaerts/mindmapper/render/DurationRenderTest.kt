package io.github.wlenaerts.mindmapper.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Region
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.wlenaerts.mindmapper.core.edit.setDuration
import io.github.wlenaerts.mindmapper.core.layout.Rect
import io.github.wlenaerts.mindmapper.core.model.BranchStyle
import io.github.wlenaerts.mindmapper.core.model.MindMap
import io.github.wlenaerts.mindmapper.core.model.Topic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(AndroidJUnit4::class)
class DurationRenderTest {
    private val renderer = MapRenderer(ApplicationProvider.getApplicationContext())
    private val metrics = renderer.metrics
    private val plain = MindMap(
        root = Topic(id = "root", text = "Centre", children = listOf(
            Topic(id = "a", text = "A"), Topic(id = "b", text = "B"), Topic(id = "c", text = "C"),
        )),
    )
    private val timed = plain.setDuration("a", 3).setDuration("c", 5)

    @Test
    fun durationLabelsShowOwnDurationAndTotal() {
        assertEquals(16 * 60, metrics.shownDurationSeconds(timed.setDuration("b", 8), timed.root, 0))
        assertEquals(180, metrics.shownDurationSeconds(timed, timed.find("a")!!, 1))
        assertNull(metrics.shownDurationSeconds(timed, timed.find("b")!!, 1))
        assertNull(metrics.shownDurationSeconds(plain, plain.root, 0))
        assertEquals("5 min", metrics.minutesLabel(5))
    }

    @Test
    fun timedTopicsAreTallerThanWithoutDurations() {
        val layout = renderer.layout(timed)
        val without = renderer.layout(timed, showDurations = false)
        assertTrue(layout.showsDurations)
        assertFalse(without.showsDurations)
        assertTrue(layout["a"]!!.rect.height > without["a"]!!.rect.height)
        assertTrue(layout["root"]!!.rect.height > without["root"]!!.rect.height)
        assertEquals(without["b"]!!.rect.height, layout["b"]!!.rect.height, 0.01f)
    }

    @Test
    fun exportLayoutIgnoresDurations() {
        val exportTimed = renderer.layout(timed, showDurations = false).bounds
        val exportPlain = renderer.layout(plain, showDurations = false).bounds
        assertEquals(exportPlain, exportTimed)
    }

    @Test
    fun paddedContentWithDurationFitsInsideEveryMainBox() {
        val style = metrics.style(1)
        for (text in listOf("A", "A main topic with a rather long text that wraps")) {
            val topic = Topic(text = text, durationSeconds = 600)
            val c = metrics.content(topic, 1, 600)
            val w = c.contentWidth + 2 * style.padH
            val h = c.contentHeight + 2 * style.padV
            for (s in BranchStyle.entries) {
                val size = metrics.measure(MindMap(root = Topic(children = listOf(topic))), topic.copy(style = s), 1, showDurations = true)
                val box = Rect(0f, 0f, size.width, size.height)
                val region = Region().apply { setPath(BranchGeometry.mainBoxPath(s, box), Region(-10, -10, size.width.toInt() + 10, size.height.toInt() + 10)) }
                val left = (box.centerX - w / 2 + 4).toInt()
                val right = (box.centerX + w / 2 - 4).toInt()
                val top = (box.centerY - h / 2 + 4).toInt()
                val bottom = (box.centerY + h / 2 - 4).toInt()
                listOf(left to top, right to top, left to bottom, right to bottom).forEach { (x, y) ->
                    assertTrue("$s '$text' corner ($x, $y)", region.contains(x, y))
                }
            }
        }
    }

    private fun render(map: MindMap, theme: MapTheme, highlightId: String?): Bitmap {
        val layout = renderer.layout(map)
        val b = layout.bounds
        val bitmap = Bitmap.createBitmap((b.width + 80).toInt(), (b.height + 80).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(theme.background)
        canvas.translate(40 - b.left, 40 - b.top)
        renderer.draw(canvas, map, layout, theme, { null }, highlightId = highlightId)
        return bitmap
    }

    @Test
    fun highlightOnlyChangesPixelsAroundTheMainTopic() {
        for (theme in listOf(MapTheme.Light, MapTheme.Dark)) {
            for (style in listOf(BranchStyle.STANDARD, BranchStyle.BLURRED)) {
                val m = timed.copy(root = timed.root.copy(children = timed.root.children.map { it.copy(style = style) }))
                val layout = renderer.layout(m)
                val plainBitmap = render(m, theme, null)
                val highlighted = render(m, theme, "c")
                val box = layout["c"]!!.rect
                val dx = 40 - layout.bounds.left
                val dy = 40 - layout.bounds.top
                var changedNear = 0
                for (y in 0 until plainBitmap.height) for (x in 0 until plainBitmap.width) {
                    if (plainBitmap.getPixel(x, y) == highlighted.getPixel(x, y)) continue
                    val near = x - dx >= box.left - 20 && x - dx <= box.right + 20 && y - dy >= box.top - 20 && y - dy <= box.bottom + 20
                    assertTrue("$style pixel ($x, $y) changed away from the highlighted topic", near)
                    changedNear++
                }
                assertTrue("$style highlight is visible", changedNear > 100)
            }
        }
    }
}
