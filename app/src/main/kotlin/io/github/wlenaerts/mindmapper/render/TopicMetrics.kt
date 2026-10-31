package io.github.wlenaerts.mindmapper.render

import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import io.github.wlenaerts.mindmapper.core.layout.Size
import io.github.wlenaerts.mindmapper.core.model.CentralShape
import io.github.wlenaerts.mindmapper.core.model.MindMap
import io.github.wlenaerts.mindmapper.core.model.Topic
import kotlin.math.ceil

/** Sizes and inner layout of topics in layout units. Shared by measuring and drawing so both agree. */
class TopicMetrics {

    data class Style(val textSize: Float, val bold: Boolean, val maxTextWidth: Float, val padH: Float, val padV: Float)

    fun style(depth: Int): Style = when (depth) {
        0 -> Style(24f, true, 260f, 22f, 16f)
        1 -> Style(18f, true, 240f, 14f, 9f)
        else -> Style(15f, false, 220f, 4f, 5f)
    }

    /** Where content goes inside a topic box, relative to the box's top-left corner. */
    data class Content(
        val text: StaticLayout,
        val textX: Float,
        val textY: Float,
        val iconSize: Float,
        val iconX: Float,
        val iconY: Float,
        val imageX: Float,
        val imageY: Float,
        val contentWidth: Float,
        val contentHeight: Float,
    )

    private val paints = HashMap<Int, TextPaint>()

    fun paint(depth: Int): TextPaint = paints.getOrPut(depth.coerceAtMost(2)) {
        val style = style(depth)
        TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            textSize = style.textSize
            typeface = if (style.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }

    // Layouts are cached by (text, depth); measuring and drawing reuse them.
    private val textCache = HashMap<Pair<String, Int>, StaticLayout>()

    private fun textLayout(text: String, depth: Int): StaticLayout = textCache.getOrPut(text to depth.coerceAtMost(2)) {
        val paint = paint(depth)
        val style = style(depth)
        val shown = text.ifEmpty { " " }
        val natural = ceil(shown.lines().maxOf { paint.measureText(it) }).toInt()
        val width = natural.coerceIn(1, style.maxTextWidth.toInt())
        StaticLayout.Builder.obtain(shown, 0, shown.length, paint, width)
            .setAlignment(if (depth == 0) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .build()
    }

    /** Content measured without the box padding or central shape. */
    fun content(topic: Topic, depth: Int): Content {
        val text = textLayout(topic.text, depth)
        val textW = (0 until text.lineCount).maxOf { text.getLineWidth(it) }.coerceAtLeast(if (topic.text.isEmpty()) 24f else 1f)
        val textH = text.height.toFloat()
        val iconSize = if (topic.icon != null) style(depth).textSize * 1.25f else 0f
        val iconGap = if (topic.icon != null) 6f else 0f
        val rowW = iconSize + iconGap + textW
        val rowH = maxOf(textH, iconSize)
        val image = topic.image
        val imageW = image?.width ?: 0f
        val imageH = image?.height ?: 0f
        val imageGap = if (image != null) 8f else 0f
        val width = maxOf(rowW, imageW)
        val height = imageH + imageGap + rowH
        val rowX = if (depth == 0) (width - rowW) / 2 else 0f
        val rowY = imageH + imageGap
        return Content(
            text = text,
            textX = rowX + iconSize + iconGap,
            textY = rowY + (rowH - textH) / 2,
            iconSize = iconSize,
            iconX = rowX,
            iconY = rowY + (rowH - iconSize) / 2,
            imageX = (width - imageW) / 2,
            imageY = 0f,
            contentWidth = width,
            contentHeight = height,
        )
    }

    fun measure(map: MindMap, topic: Topic, depth: Int): Size {
        val c = content(topic, depth)
        val s = style(depth)
        val w = c.contentWidth + 2 * s.padH
        val h = c.contentHeight + 2 * s.padV
        if (depth != 0) return Size(w, h)
        return when (map.centralShape) {
            CentralShape.RECTANGLE, CentralShape.ROUNDED_RECTANGLE -> Size(w, h)
            CentralShape.CAPSULE -> Size(w + h * 0.6f, h)
            // Each shape is sized so the padded content rectangle fits inside it.
            CentralShape.ELLIPSE -> Size(w * 1.42f, h * 1.42f)
            CentralShape.CIRCLE -> kotlin.math.hypot(w, h).let { Size(it, it) }
            CentralShape.DIAMOND -> Size(w * 1.8f, h * 2.25f)
            CentralShape.HEXAGON -> Size(w + h, h)
        }
    }
}
