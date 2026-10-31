package io.github.wlenaerts.mindmapper.core

import io.github.wlenaerts.mindmapper.core.layout.MapLayouter
import io.github.wlenaerts.mindmapper.core.layout.Rect
import io.github.wlenaerts.mindmapper.core.view.ExportSizes
import io.github.wlenaerts.mindmapper.core.view.Viewport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewTest {

    @Test
    fun fitCentresBounds() {
        val v = Viewport.fit(Rect(-100f, -50f, 100f, 50f), 1000f, 500f, 0f, 0.1f, 10f)
        assertEquals(5f, v.scale, 0.001f)
        assertEquals(500f, v.offsetX, 0.001f)
        assertEquals(250f, v.offsetY, 0.001f)
    }

    @Test
    fun zoomKeepsFocusPointAndClamps() {
        val v = Viewport(1f, 0f, 0f).zoom(2f, 100f, 100f, 0.2f, 4f)
        assertEquals(100f, v.toLayoutX(100f) * v.scale + v.offsetX, 0.001f)
        assertEquals(100f, v.toLayoutX(100f), 0.001f)
        assertEquals(50f, v.toLayoutX(150f) - v.toLayoutX(50f), 0.001f)
        assertEquals(4f, v.zoom(100f, 0f, 0f, 0.2f, 4f).scale, 0.001f)
    }

    @Test
    fun subtreeBoundsCoverBranch() {
        val m = map(branch("a", 3), topic("b"))
        val layout = MapLayouter.layout(m, fixedMeasure)
        val bounds = layout.subtreeBounds(m, "a")!!
        (0..2).forEach { val r = layout["a.$it"]!!.rect; assertTrue(bounds.contains(r.left, r.top) && bounds.contains(r.right, r.bottom)) }
        assertTrue(!bounds.contains(layout["b"]!!.rect.centerX, layout["b"]!!.rect.centerY))
    }

    @Test
    fun pngSizeIsDoubledAndCapped() {
        val small = ExportSizes.png(Rect(0f, 0f, 400f - 64f, 200f - 64f))
        assertEquals(800, small.width)
        assertEquals(400, small.height)
        val large = ExportSizes.png(Rect(0f, 0f, 6000f - 64f, 3000f - 64f))
        assertEquals(8192, large.width)
        assertEquals(4096, large.height)
    }

    @Test
    fun pdfPageFitsMapWithMargin() {
        val page = ExportSizes.pdf(Rect(0f, 0f, 500f, 300f))
        assertEquals(572, page.width)
        assertEquals(372, page.height)
        assertEquals(1f, page.scale, 0f)
        assertTrue(ExportSizes.pdf(Rect(0f, 0f, 30000f, 100f)).width <= 14400)
    }
}
