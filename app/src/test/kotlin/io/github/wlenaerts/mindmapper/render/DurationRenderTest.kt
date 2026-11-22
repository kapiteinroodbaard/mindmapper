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
import io.github.wlenaerts.mindmapper.core.model.Side
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

    private fun render(map: MindMap, theme: MapTheme, highlightId: String?, highlightScale: Float = 1.8f, neighbourIds: Set<String>? = null): Bitmap {
        val layout = renderer.layout(map)
        val b = layout.bounds
        val bitmap = Bitmap.createBitmap((b.width + 80).toInt(), (b.height + 80).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(theme.background)
        canvas.translate(40 - b.left, 40 - b.top)
        renderer.draw(canvas, map, layout, theme, { null }, highlightId = highlightId, highlightScale = highlightScale, neighbourIds = neighbourIds)
        return bitmap
    }

    private fun sameBitmaps(a: Bitmap, b: Bitmap): Boolean =
        (0 until a.height).all { y -> (0 until a.width).all { x -> a.getPixel(x, y) == b.getPixel(x, y) } }

    @Test
    fun highlightThickensOnlyTheHighlightedBranchAndItsSubtopics() {
        val withSubtopics = timed.copy(root = timed.root.copy(children = timed.root.children.map {
            it.copy(children = listOf(Topic(id = "${it.id}1", text = "Sub ${it.text}")))
        }))
        for (theme in listOf(MapTheme.Light, MapTheme.Dark)) {
            for (style in listOf(BranchStyle.STANDARD, BranchStyle.BLURRED, BranchStyle.PLANT)) {
                val m = withSubtopics.copy(root = withSubtopics.root.copy(children = withSubtopics.root.children.map { it.copy(style = style) }))
                val layout = renderer.layout(m)
                val plainBitmap = render(m, theme, null)
                assertTrue("$style unscaled highlight changes nothing", sameBitmaps(plainBitmap, render(m, theme, "c", highlightScale = 1f)))
                val highlighted = render(m, theme, "c")
                // The main branch starts inside the central topic, so the branch area runs from there.
                val area = layout.subtreeBounds(m, "c")!!.union(layout["root"]!!.rect)
                val sub = layout["c1"]!!.rect
                val box = layout["c"]!!.rect
                val dx = 40 - layout.bounds.left
                val dy = 40 - layout.bounds.top
                var changedNear = 0
                var changedAtSubtopic = 0
                var changedAroundBox = 0
                var changedAtSubtopicEnd = 0
                for (y in 0 until plainBitmap.height) for (x in 0 until plainBitmap.width) {
                    if (plainBitmap.getPixel(x, y) == highlighted.getPixel(x, y)) continue
                    val lx = x - dx
                    val ly = y - dy
                    val near = lx >= area.left - 20 && lx <= area.right + 20 && ly >= area.top - 20 && ly <= area.bottom + 20
                    assertTrue("$style pixel ($x, $y) changed away from the highlighted branch", near)
                    changedNear++
                    if (lx >= sub.left && lx <= sub.right && ly >= sub.bottom - 10 && ly <= sub.bottom + 10) changedAtSubtopic++
                    val aroundBox = lx >= box.left - 5 && lx <= box.right + 5 && ly >= box.top - 5 && ly <= box.bottom + 5 &&
                        !(lx > box.left + 2 && lx < box.right - 2 && ly > box.top + 2 && ly < box.bottom - 2)
                    if (aroundBox) changedAroundBox++
                    // The plant leaf sits past the outer end of the underline and points up, above the line.
                    val beyondEnd = if (layout["c1"]!!.side == Side.LEFT) lx < sub.left - 1 && lx >= sub.left - 20 else lx > sub.right + 1 && lx <= sub.right + 20
                    if (beyondEnd && ly >= sub.bottom - 20 && ly <= sub.bottom - 4) changedAtSubtopicEnd++
                }
                assertTrue("$style highlight is visible", changedNear > 100)
                assertTrue("$style subtopic underline thickens too", changedAtSubtopic > 10)
                assertTrue("$style main topic box grows with its branch", changedAroundBox > 50)
                if (style == BranchStyle.PLANT) assertTrue("plant leaf at the subtopic end grows", changedAtSubtopicEnd > 20)
            }
        }
    }

    @Test
    fun neighboursFadeLessThanTheOtherMainBranches() {
        val m = MindMap(root = Topic(id = "root", text = "Centre", children = listOf("a", "b", "c", "d").map { Topic(id = it, text = it.uppercase()) }))
        val layout = renderer.layout(m)
        val dx = 40 - layout.bounds.left
        val dy = 40 - layout.bounds.top
        for (theme in listOf(MapTheme.Light, MapTheme.Dark)) {
            val plainBitmap = render(m, theme, null)
            assertTrue("without neighbours nothing fades", sameBitmaps(render(m, theme, "c", highlightScale = 1f), render(m, theme, "c", highlightScale = 1f, neighbourIds = null)))
            val faded = render(m, theme, "c", highlightScale = 1f, neighbourIds = setOf("b", "d"))
            fun difference(id: String): Long {
                val r = layout[id]!!.rect
                var sum = 0L
                for (y in (r.top + dy).toInt()..(r.bottom + dy).toInt()) for (x in (r.left + dx).toInt()..(r.right + dx).toInt()) {
                    val a = plainBitmap.getPixel(x, y)
                    val b = faded.getPixel(x, y)
                    sum += listOf(16, 8, 0).sumOf { kotlin.math.abs((a shr it and 0xFF) - (b shr it and 0xFF)) }
                }
                return sum
            }
            assertEquals("the highlighted branch is not faded", 0L, difference("c"))
            assertTrue("a neighbour fades", difference("b") > 0)
            assertTrue("the far branch fades more than a neighbour", difference("a") > difference("b"))
        }
    }
}
