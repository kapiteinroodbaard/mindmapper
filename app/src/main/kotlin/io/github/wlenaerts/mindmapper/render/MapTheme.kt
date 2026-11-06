package io.github.wlenaerts.mindmapper.render

import io.github.wlenaerts.mindmapper.core.layout.MapLayouter

/**
 * Colours used to draw a map. [palette] has one colour per branch colour slot; [glowAlphas] are the
 * opacities of the blurred style's soft rings, faintest (outermost) first; [highlightAlphas] are the
 * same for the stronger glow around the scheduled main topic in presentation mode.
 */
data class MapTheme(
    val background: Int,
    val text: Int,
    val palette: List<Int>,
    val selection: Int,
    val dropIndicator: Int,
    val glowAlphas: List<Float>,
    val highlightAlphas: List<Float>,
) {
    fun branchColor(slot: Int): Int = palette[slot.mod(palette.size)]

    companion object {
        // Ordered with large hue jumps so that neighbouring slots contrast clearly.
        val Light = MapTheme(
            background = 0xFFFFFFFF.toInt(),
            text = 0xFF1F1D24.toInt(),
            palette = listOf(
                0xFFE4572E, 0xFF2E86AB, 0xFF7CB518, 0xFF8E44AD, 0xFFF39C12,
                0xFF16A085, 0xFFD6336C, 0xFF3F51B5, 0xFFB7950B, 0xFF00897B,
            ).map { it.toInt() },
            selection = 0xFF1E88E5.toInt(),
            dropIndicator = 0xFF1E88E5.toInt(),
            glowAlphas = listOf(0.06f, 0.09f, 0.13f, 0.18f, 0.26f),
            highlightAlphas = listOf(0.10f, 0.16f, 0.24f, 0.34f, 0.46f, 0.60f),
        )

        val Dark = MapTheme(
            background = 0xFF17171B.toInt(),
            text = 0xFFECE9F0.toInt(),
            palette = listOf(
                0xFFFF7A52, 0xFF4FB3DE, 0xFF9CD43A, 0xFFB57BD6, 0xFFFFB443,
                0xFF3CC9A8, 0xFFF0628F, 0xFF7E8EF0, 0xFFE0C043, 0xFF35B8A8,
            ).map { it.toInt() },
            selection = 0xFF90CAF9.toInt(),
            dropIndicator = 0xFF90CAF9.toInt(),
            glowAlphas = listOf(0.09f, 0.13f, 0.18f, 0.24f, 0.32f),
            highlightAlphas = listOf(0.14f, 0.22f, 0.32f, 0.44f, 0.58f, 0.72f),
        )

        init {
            check(Light.palette.size == MapLayouter.PALETTE_SIZE && Dark.palette.size == MapLayouter.PALETTE_SIZE)
        }
    }
}

/** Black or white, whichever contrasts better with [background]. */
fun contrastingTextColor(background: Int): Int {
    fun channel(shift: Int): Double {
        val c = ((background shr shift) and 0xFF) / 255.0
        return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }
    val luminance = 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    return if (luminance > 0.4) 0xFF1F1D24.toInt() else 0xFFFFFFFF.toInt()
}
