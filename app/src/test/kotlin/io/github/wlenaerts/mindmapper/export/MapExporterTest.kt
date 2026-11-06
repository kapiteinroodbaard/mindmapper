package io.github.wlenaerts.mindmapper.export

import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.wlenaerts.mindmapper.core.model.MindMap
import io.github.wlenaerts.mindmapper.core.model.Topic
import io.github.wlenaerts.mindmapper.render.ImageSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream

// Real (native) graphics so drawn pixels can be checked.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(AndroidJUnit4::class)
class MapExporterTest {
    private val exporter = MapExporter(ApplicationProvider.getApplicationContext())
    private val noImages = ImageSource { null }
    private val map = MindMap(root = Topic(text = "Centre", children = List(5) { Topic(text = "Branch $it", icon = "star") }))

    @Test
    fun pngHasWhiteBackgroundAndScaledSize() {
        val out = ByteArrayOutputStream()
        exporter.writePng(map, noImages, out)
        val bitmap = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
        assertTrue(bitmap.width > 400 && bitmap.height > 100)
        assertEquals(Color.WHITE, bitmap.getPixel(1, 1))
    }

    @Test
    fun thumbnailIsSmall() {
        val bytes = exporter.thumbnail(map, noImages)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertTrue(maxOf(bitmap.width, bitmap.height) <= 512)
    }

    private fun inkedPixels(m: MindMap): Int {
        val out = ByteArrayOutputStream()
        exporter.writePng(m, noImages, out)
        val bitmap = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
        var count = 0
        for (y in 0 until bitmap.height step 2) for (x in 0 until bitmap.width step 2) if (bitmap.getPixel(x, y) != Color.WHITE) count++
        return count
    }

    @Test
    fun trunkWidthAndTaperRenderAtTheirLimits() {
        val thin = inkedPixels(map.copy(trunkWidth = MindMap.MIN_TRUNK_WIDTH, trunkTaper = MindMap.MAX_TRUNK_TAPER))
        val thick = inkedPixels(map.copy(trunkWidth = MindMap.MAX_TRUNK_WIDTH, trunkTaper = MindMap.MIN_TRUNK_TAPER))
        val thickFastTaper = inkedPixels(map.copy(trunkWidth = MindMap.MAX_TRUNK_WIDTH, trunkTaper = MindMap.MAX_TRUNK_TAPER))
        assertTrue("thick $thick thin $thin", thick > thin)
        assertTrue("taper thins the trunk sooner", thickFastTaper < thick)
    }

    @Test
    fun exportLeavesOutPlannedDurations() {
        val timed = map.copy(root = map.root.copy(children = map.root.children.map { it.copy(durationSeconds = 300) }))
        fun png(m: MindMap) = ByteArrayOutputStream().also { exporter.writePng(m, noImages, it) }.toByteArray()
        val withDurations = png(timed).let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        val without = png(map).let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        assertEquals(without.width, withDurations.width)
        assertEquals(without.height, withDurations.height)
        assertEquals(inkedPixels(map), inkedPixels(timed))
    }
}
