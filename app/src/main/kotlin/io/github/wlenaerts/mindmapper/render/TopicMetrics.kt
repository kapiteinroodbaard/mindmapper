package io.github.wlenaerts.mindmapper.render

import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import io.github.wlenaerts.mindmapper.core.layout.Size
import io.github.wlenaerts.mindmapper.core.model.CentralShape
import io.github.wlenaerts.mindmapper.core.model.MindMap
import io.github.wlenaerts.mindmapper.core.model.Topic
import io.github.wlenaerts.mindmapper.core.model.styleOrStandard
import io.github.wlenaerts.mindmapper.core.timing.schedule
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
        /** The planned duration line under the text, or null when none is shown. */
        val duration: StaticLayout?,
        val durationX: Float,
        val durationY: Float,
        val contentWidth: Float,
        val contentHeight: Float,
    ) {
        /** The paint [text] draws with; the caller sets its colour before drawing. */
        val paint: TextPaint get() = text.paint
    }

    /** Effective text formatting of a topic: [Topic.bold] falls back to the level's default. */
    data class Format(val bold: Boolean, val italic: Boolean, val underline: Boolean)

    fun format(topic: Topic, depth: Int): Format =
        Format(topic.bold ?: style(depth).bold, topic.italic == true, topic.underline == true)

    /** Formats a planned duration in whole minutes, such as "5 min". */
    var minutesLabel: (Int) -> String = { "$it min" }

    /**
     * The planned duration shown on [topic] at [depth], in seconds: its own duration on a main branch, the
     * total of all main branches on the central topic, and null when there is nothing to show.
     */
    fun shownDurationSeconds(map: MindMap, topic: Topic, depth: Int): Int? = when (depth) {
        0 -> map.schedule().totalSeconds.takeIf { it > 0 }
        1 -> topic.durationSeconds?.takeIf { it > 0 }
        else -> null
    }

    private val paints = HashMap<Pair<Int, Format>, TextPaint>()

    private val durationPaints = HashMap<Int, TextPaint>()

    private fun durationPaint(depth: Int): TextPaint = durationPaints.getOrPut(depth.coerceAtMost(2)) {
        TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            textSize = style(depth).textSize * DURATION_TEXT_SCALE
            typeface = Typeface.DEFAULT
        }
    }

    private val durationCache = HashMap<Pair<String, Int>, StaticLayout>()

    private fun durationLayout(seconds: Int, depth: Int): StaticLayout {
        val text = minutesLabel((seconds + 59) / 60)
        return durationCache.getOrPut(text to depth.coerceAtMost(2)) {
            val paint = durationPaint(depth)
            val width = ceil(paint.measureText(text)).toInt().coerceAtLeast(1)
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width).setIncludePad(false).build()
        }
    }

    fun paint(depth: Int, format: Format): TextPaint = paints.getOrPut(depth.coerceAtMost(2) to format) {
        val style = style(depth)
        TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            textSize = style.textSize
            val typefaceStyle = when {
                format.bold && format.italic -> Typeface.BOLD_ITALIC
                format.bold -> Typeface.BOLD
                format.italic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            typeface = Typeface.create(Typeface.DEFAULT, typefaceStyle)
            isUnderlineText = format.underline
        }
    }

    // Layouts are cached by (text, depth, format); measuring and drawing reuse them.
    private val textCache = HashMap<Triple<String, Int, Format>, StaticLayout>()

    private fun textLayout(text: String, depth: Int, format: Format): StaticLayout =
        textCache.getOrPut(Triple(text, depth.coerceAtMost(2), format)) {
            val paint = paint(depth, format)
            val style = style(depth)
            val shown = text.ifEmpty { " " }
            val natural = ceil(shown.lines().maxOf { paint.measureText(it) }).toInt()
            val width = natural.coerceIn(1, style.maxTextWidth.toInt())
            StaticLayout.Builder.obtain(shown, 0, shown.length, paint, width)
                .setAlignment(if (depth == 0) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .build()
        }

    /**
     * Content measured without the box padding or central shape. [durationSeconds] adds a duration line
     * under the text.
     */
    fun content(topic: Topic, depth: Int, durationSeconds: Int? = null): Content {
        val text = textLayout(topic.text, depth, format(topic, depth))
        // Wrapped lines can be narrower than the layout and are then centred inside it, so measure the
        // drawn extent of the lines and shift by its left edge to keep the text centred in the box.
        val lines = 0 until text.lineCount
        val inkLeft = lines.minOf { text.getLineLeft(it) }
        val inkRight = lines.maxOf { text.getLineRight(it) }
        val textW = (inkRight - inkLeft).coerceAtLeast(if (topic.text.isEmpty()) 24f else 1f)
        val textH = text.height.toFloat()
        val iconSize = if (topic.icon != null) style(depth).textSize * 1.25f else 0f
        val iconGap = if (topic.icon != null) 6f else 0f
        val rowW = iconSize + iconGap + textW
        val rowH = maxOf(textH, iconSize)
        val image = topic.image
        val imageW = image?.width ?: 0f
        val imageH = image?.height ?: 0f
        val imageGap = if (image != null) 8f else 0f
        val duration = durationSeconds?.let { durationLayout(it, depth) }
        val durationW = duration?.getLineWidth(0) ?: 0f
        val durationH = duration?.height?.toFloat() ?: 0f
        val durationGap = if (duration != null) 2f else 0f
        val width = maxOf(rowW, imageW, durationW)
        val height = imageH + imageGap + rowH + durationGap + durationH
        val rowX = if (depth == 0) (width - rowW) / 2 else 0f
        val rowY = imageH + imageGap
        return Content(
            text = text,
            textX = rowX + iconSize + iconGap - inkLeft,
            textY = rowY + (rowH - textH) / 2,
            iconSize = iconSize,
            iconX = rowX,
            iconY = rowY + (rowH - iconSize) / 2,
            imageX = (width - imageW) / 2,
            imageY = 0f,
            duration = duration,
            durationX = if (depth == 0) (width - durationW) / 2 else rowX + iconSize + iconGap,
            durationY = rowY + rowH + durationGap,
            contentWidth = width,
            contentHeight = height,
        )
    }

    fun measure(map: MindMap, topic: Topic, depth: Int, showDurations: Boolean = false): Size {
        val c = content(topic, depth, if (showDurations) shownDurationSeconds(map, topic, depth) else null)
        val s = style(depth)
        val w = c.contentWidth + 2 * s.padH
        val h = c.contentHeight + 2 * s.padV
        if (depth == 1) return BranchGeometry.mainBoxSize(topic.styleOrStandard, w, h)
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

/** Size of the duration line's text relative to the topic text of the same level. */
private const val DURATION_TEXT_SCALE = 0.75f
