package io.github.wlenaerts.mindmapper.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.Picture
import android.graphics.RectF
import android.graphics.Region
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.wlenaerts.mindmapper.core.edit.setBranchStyle
import io.github.wlenaerts.mindmapper.core.layout.Rect
import io.github.wlenaerts.mindmapper.core.model.BranchStyle
import io.github.wlenaerts.mindmapper.core.model.MindMap
import io.github.wlenaerts.mindmapper.core.model.Topic
import io.github.wlenaerts.mindmapper.export.MapExporter
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import kotlin.math.hypot
import kotlin.math.pow

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(AndroidJUnit4::class)
class BranchStyleRenderTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val metrics = TopicMetrics()
    private val renderer = MapRenderer(context, metrics)

    private fun bounds(path: Path) = RectF().also { path.computeBounds(it, true) }

    /** The tapered ribbon as it was drawn before branch styles existed. */
    private fun legacyRibbon(x0: Float, y0: Float, x3: Float, y3: Float, w0: Float, w1: Float, taper: Float, inside: Rect?): Path {
        val mx = (x0 + x3) / 2
        val steps = 40
        fun point(t: Float): Pair<Float, Float> {
            val u = 1 - t
            return (u * u * u * x0 + 3 * u * u * t * mx + 3 * u * t * t * mx + t * t * t * x3) to
                (u * u * u * y0 + 3 * u * u * t * y0 + 3 * u * t * t * y3 + t * t * t * y3)
        }
        val exit = inside?.let { r -> (0..steps).map { it / steps.toFloat() }.firstOrNull { t -> point(t).let { (x, y) -> !r.contains(x, y) } } } ?: 0f
        val upper = ArrayList<Pair<Float, Float>>()
        val lower = ArrayList<Pair<Float, Float>>()
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val u = 1 - t
            val (x, y) = point(t)
            val dx = 3 * u * u * (mx - x0) + 3 * t * t * (x3 - mx)
            val dy = 6 * u * t * (y3 - y0)
            val len = hypot(dx, dy).takeIf { it > 0.0001f } ?: 1f
            val progress = if (t <= exit) 0f else (t - exit) / (1 - exit)
            val half = (w1 + (w0 - w1) * (1 - progress).toDouble().pow(taper.toDouble()).toFloat()) / 2
            upper += (x - dy / len * half) to (y + dx / len * half)
            lower += (x + dy / len * half) to (y - dx / len * half)
        }
        return Path().apply {
            moveTo(upper[0].first, upper[0].second)
            upper.drop(1).forEach { lineTo(it.first, it.second) }
            lower.reversed().forEach { lineTo(it.first, it.second) }
            close()
        }
    }

    @Test
    fun standardOutlineMatchesThePreviousRibbon() {
        val inside = Rect(-80f, -30f, 80f, 30f)
        listOf(
            floatArrayOf(0f, 0f, 200f, -120f, 30f, 5f, 3f),
            floatArrayOf(0f, 0f, -150f, 90f, 12f, 5f, 1f),
            floatArrayOf(40f, 10f, 120f, 60f, 4.5f, 2.5f, 1f),
        ).forEachIndexed { i, v ->
            val (x0, y0, x1, y1, w0) = v
            val w1 = v[5]
            val taper = v[6]
            val r = if (i < 2) inside else null
            val line = BranchGeometry.curve(x0, y0, x1, y1)
            val now = bounds(BranchGeometry.outline(line, BranchGeometry.halfWidths(line, w0, w1, taper, r)))
            val before = bounds(legacyRibbon(x0, y0, x1, y1, w0, w1, taper, r))
            assertEquals(before.left, now.left, 0.01f)
            assertEquals(before.top, now.top, 0.01f)
            assertEquals(before.right, now.right, 0.01f)
            assertEquals(before.bottom, now.bottom, 0.01f)
        }
    }

    @Test
    fun paddedContentFitsInsideEveryMainBox() {
        val style = metrics.style(1)
        for (text in listOf("A", "Main topic", "A main topic with a rather long text that wraps")) {
            val c = metrics.content(Topic(text = text), 1)
            val w = c.contentWidth + 2 * style.padH
            val h = c.contentHeight + 2 * style.padV
            for (s in BranchStyle.entries) {
                val size = metrics.measure(MindMap(root = Topic()), Topic(text = text, style = s), 1)
                val box = Rect(0f, 0f, size.width, size.height)
                val region = Region().apply { setPath(BranchGeometry.mainBoxPath(s, box), Region(-10, -10, size.width.toInt() + 10, size.height.toInt() + 10)) }
                // 4 units in from each corner: the rounded box's corner radius only cuts into the padding.
                val left = (box.centerX - w / 2 + 4).toInt()
                val right = (box.centerX + w / 2 - 4).toInt()
                val top = (box.centerY - h / 2 + 4).toInt()
                val bottom = (box.centerY + h / 2 - 4).toInt()
                listOf(left to top, right to top, left to bottom, right to bottom).forEach { (x, y) ->
                    assertTrue("$s '$text' corner ($x, $y)", region.contains(x, y))
                }
                // Main branches end on the box outline at the vertical centre.
                val inset = BranchGeometry.mainBoxInset(s, box)
                assertTrue("$s left end", region.contains((box.left + inset + 1).toInt(), box.centerY.toInt()))
            }
        }
    }

    @Test
    fun lightningIsStableAndKeepsItsEnds() {
        val a = BranchGeometry.zigzag(0f, 0f, 200f, -80f, seed = 42, amplitude = 8f)
        val b = BranchGeometry.zigzag(0f, 0f, 200f, -80f, seed = 42, amplitude = 8f)
        val other = BranchGeometry.zigzag(0f, 0f, 200f, -80f, seed = 43, amplitude = 8f)
        assertArrayEquals(a.xs, b.xs, 0f)
        assertArrayEquals(a.ys, b.ys, 0f)
        assertFalse(a.ys.contentEquals(other.ys))
        assertEquals(0f, a.xs.first(), 0f)
        assertEquals(200f, a.xs.last(), 0.001f)
        assertEquals(-80f, a.ys.last(), 0.001f)
        // The bolt really zigzags: it leaves the straight line.
        assertTrue((0 until a.size).any { i -> kotlin.math.abs(a.ys[i] - (-80f * a.xs[i] / 200f)) > 3f })
    }

    @Test
    fun lightningBranchIsUnchangedByEditsElsewhere() {
        val map = MindMap(
            root = Topic(id = "root", text = "Centre", children = listOf(
                Topic(id = "a", text = "Bolt", style = BranchStyle.LIGHTNING),
                Topic(id = "b", text = "Other"),
            )),
        )
        val edited = map.copy(root = map.root.copy(children = listOf(map.root.children[0], map.root.children[1].copy(text = "Edited text"))))
        val layout = renderer.layout(map)
        assertEquals(layout["a"]!!.rect, renderer.layout(edited)["a"]!!.rect)
        val before = render(map)
        val after = render(edited)
        val bolt = layout["a"]!!.rect
        val root = layout["root"]!!.rect
        // Compare the area between the central topic and the bolt's topic, where only the bolt is drawn.
        var drawn = 0
        for (x in (root.right + ORIGIN).toInt() until (bolt.left + ORIGIN).toInt()) for (y in 0 until SIZE) {
            assertEquals(before.getPixel(x, y), after.getPixel(x, y))
            if (before.getPixel(x, y) != Color.WHITE) drawn++
        }
        assertTrue(drawn > 50)
    }

    @Test
    fun widthsStillTaperPerLevel() {
        val main = BranchGeometry.halfWidths(BranchGeometry.zigzag(0f, 0f, 200f, 0f, 1, 6f), 12f, 5f)
        val sub = BranchGeometry.halfWidths(BranchGeometry.zigzag(0f, 0f, 60f, 20f, 2, 3f), 4.5f, 2.5f)
        assertTrue(main.last() * 2 >= sub.first() * 2)
        assertTrue(main.first() > main.last())
        assertTrue(sub.first() > sub.last())
    }

    @Test
    fun plantDecorationsStayClearOfTopicsAndAreStable() {
        val start = Rect(-80f, -30f, 80f, 30f)
        val end = Rect(260f, -160f, 360f, -120f)
        val line = BranchGeometry.curve(0f, 0f, end.left, end.centerY)
        val halves = BranchGeometry.halfWidths(line, 20f, 5f, 1f, start)
        val parts = BranchGeometry.plantParts(line, halves, seed = 9, start = start, end = end)
        assertTrue(parts.size >= 3)
        assertTrue(parts.any { it.leaf } && parts.any { !it.leaf })
        assertEquals(parts, BranchGeometry.plantParts(line, halves, seed = 9, start = start, end = end))
        for (p in parts) {
            assertFalse(start.contains(p.x, p.y) || start.contains(p.tipX, p.tipY))
            assertFalse(end.contains(p.x, p.y) || end.contains(p.tipX, p.tipY))
        }
    }

    @Test
    fun styledMapsExportToPngAndRecordForPdf() {
        val exporter = MapExporter(context)
        var map = MindMap(root = Topic(id = "root", text = "Centre", children = BranchStyle.entries.map { s ->
            Topic(id = s.serialName, text = s.serialName, children = listOf(Topic(text = "Sub", children = listOf(Topic(text = "Deep")))))
        }))
        BranchStyle.entries.forEach { map = map.setBranchStyle(it.serialName, it) }
        val noImages = ImageSource { null }
        val png = ByteArrayOutputStream().also { exporter.writePng(map, noImages, it) }
        assertTrue(png.size() > 1000)
        // PdfDocument does not run under Robolectric; a recording canvas exercises the same non-bitmap path.
        val layout = renderer.layout(map)
        val picture = Picture()
        val canvas = picture.beginRecording(layout.bounds.width.toInt() + 1, layout.bounds.height.toInt() + 1)
        canvas.translate(-layout.bounds.left, -layout.bounds.top)
        renderer.draw(canvas, map, layout, MapTheme.Light, noImages)
        picture.endRecording()
        val replay = Bitmap.createBitmap(picture.width, picture.height, Bitmap.Config.ARGB_8888)
        Canvas(replay).drawPicture(picture)
        assertTrue((0 until replay.width step 4).any { x -> (0 until replay.height step 4).any { y -> replay.getPixel(x, y) != 0 } })
    }

    @Test
    fun everyStyleDrawsASample() {
        for (s in BranchStyle.entries) {
            val bitmap = Bitmap.createBitmap(120, 72, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            renderer.drawStyleSample(Canvas(bitmap), s, 0xFFE4572E.toInt(), MapTheme.Light, 120f, 72f)
            val coloured = (0 until 120).sumOf { x -> (0 until 72).count { y -> bitmap.getPixel(x, y) != Color.WHITE } }
            assertTrue("$s draws", coloured > 300)
        }
    }

    /** Renders [map] with the central topic at ([ORIGIN], [ORIGIN]) on a fixed-size bitmap. */
    private fun render(map: MindMap): Bitmap {
        val layout = renderer.layout(map)
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        val canvas = Canvas(bitmap)
        canvas.translate(ORIGIN, ORIGIN)
        renderer.draw(canvas, map, layout, MapTheme.Light, ImageSource { null })
        return bitmap
    }

    private companion object {
        const val SIZE = 1000
        const val ORIGIN = 500f
    }
}
