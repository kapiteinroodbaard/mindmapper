package io.github.wlenaerts.mindmapper.render

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.wlenaerts.mindmapper.core.model.MindMap
import io.github.wlenaerts.mindmapper.core.model.Topic
import io.github.wlenaerts.mindmapper.core.model.TopicImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(AndroidJUnit4::class)
class RenderTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val metrics = TopicMetrics()
    private val map = MindMap(root = Topic(text = "Centre"))

    @Test
    fun everyIconResolves() {
        assertTrue(IconSet.icons.size >= 50)
        IconSet.icons.values.forEach { assertNotNull(context.getDrawable(it)) }
    }

    @Test
    fun longTextWrapsAtMaximumWidth() {
        val short = metrics.measure(map, Topic(text = "Short"), 2)
        val long = metrics.measure(map, Topic(text = "A much longer topic text that cannot possibly fit on a single line of the map"), 2)
        assertTrue(long.width <= metrics.style(2).maxTextWidth + 2 * metrics.style(2).padH + 1)
        assertTrue(long.height > short.height * 1.5f)
    }

    @Test
    fun imageWidthScalesTopic() {
        val small = metrics.measure(map, Topic(text = "x", image = TopicImage("images/a.webp", 160f, 1f)), 1)
        val large = metrics.measure(map, Topic(text = "x", image = TopicImage("images/a.webp", 320f, 1f)), 1)
        assertEquals(160f, large.width - small.width, 0.5f)
        assertEquals(160f, large.height - small.height, 0.5f)
    }

    @Test
    fun formattingChangesPaintAndWidth() {
        val text = "Formatted topic"
        val plain = metrics.content(Topic(text = text), 2)
        val bold = metrics.content(Topic(text = text, bold = true), 2)
        val italicUnderlined = metrics.content(Topic(text = text, italic = true, underline = true), 2)
        assertFalse(plain.paint.typeface.isBold)
        assertTrue(bold.paint.typeface.isBold)
        // The box is measured with the formatted paint, so its size follows the formatting.
        assertEquals(bold.paint.measureText(text), bold.contentWidth, 1f)
        assertTrue(italicUnderlined.paint.typeface.isItalic)
        assertTrue(italicUnderlined.paint.isUnderlineText)
    }

    @Test
    fun mainBranchesAreBoldByDefaultAndCanBeRegular() {
        assertTrue(metrics.content(Topic(text = "Main"), 1).paint.typeface.isBold)
        assertFalse(metrics.content(Topic(text = "Main", bold = false), 1).paint.typeface.isBold)
    }

    @Test
    fun wrappedCentralTextIsCentred() {
        val c = metrics.content(Topic(text = "A long central topic text that wraps onto several lines of the shape"), 0)
        assertTrue(c.text.lineCount > 1)
        val lines = 0 until c.text.lineCount
        val left = c.textX + lines.minOf { c.text.getLineLeft(it) }
        val right = c.contentWidth - (c.textX + lines.maxOf { c.text.getLineRight(it) })
        assertEquals(left, right, 1f)
        assertEquals(0f, left, 1f)
    }
}
