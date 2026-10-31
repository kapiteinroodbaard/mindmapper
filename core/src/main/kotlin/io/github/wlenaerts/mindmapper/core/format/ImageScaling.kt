package io.github.wlenaerts.mindmapper.core.format

import kotlin.math.roundToInt

object ImageScaling {
    /** Longest side of an embedded image in pixels. */
    const val MAX_EMBEDDED_SIDE = 1600

    /** The size to store an image of [width]×[height] at: unchanged when small enough, else scaled to [maxSide]. */
    fun embeddedSize(width: Int, height: Int, maxSide: Int = MAX_EMBEDDED_SIDE): Pair<Int, Int> {
        val longest = maxOf(width, height)
        if (longest <= maxSide) return width to height
        val factor = maxSide.toDouble() / longest
        return (width * factor).roundToInt().coerceAtLeast(1) to (height * factor).roundToInt().coerceAtLeast(1)
    }
}
