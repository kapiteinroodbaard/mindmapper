package io.github.wlenaerts.mindmapper.core.view

import io.github.wlenaerts.mindmapper.core.layout.Rect
import kotlin.math.roundToInt

/** Output size in pixels or points and the scale from layout units to it. */
data class ExportSize(val width: Int, val height: Int, val scale: Float)

object ExportSizes {
    const val PNG_SCALE = 2f
    const val PNG_MAX_SIDE = 8192
    const val PDF_MARGIN = 36f
    const val PDF_MAX_SIDE = 14400f
    const val PNG_MARGIN = 32f

    /** PNG at twice the layout size (including margin), limited to [maxSide] pixels on the longest side. */
    fun png(bounds: Rect, maxSide: Int = PNG_MAX_SIDE, scale: Float = PNG_SCALE): ExportSize {
        val w = bounds.width + 2 * PNG_MARGIN
        val h = bounds.height + 2 * PNG_MARGIN
        val s = minOf(scale, maxSide / maxOf(w, h))
        return ExportSize((w * s).roundToInt().coerceAtLeast(1), (h * s).roundToInt().coerceAtLeast(1), s)
    }

    /** One PDF page in points (1 layout unit = 1 pt) with a margin, scaled down to the PDF page limit. */
    fun pdf(bounds: Rect): ExportSize {
        val w = bounds.width + 2 * PDF_MARGIN
        val h = bounds.height + 2 * PDF_MARGIN
        val s = minOf(1f, PDF_MAX_SIDE / maxOf(w, h))
        return ExportSize((w * s).roundToInt().coerceAtLeast(1), (h * s).roundToInt().coerceAtLeast(1), s)
    }
}
