package io.github.wlenaerts.mindmapper.render

import io.github.wlenaerts.mindmapper.core.layout.MapLayouter
import io.github.wlenaerts.mindmapper.core.model.MindMap

/**
 * Colours used to draw a map. [palette] has one colour per branch colour slot; [glowAlphas] are the
 * opacities of the blurred style's soft rings, faintest (outermost) first.
 */
data class MapTheme(
    val background: Int,
    val text: Int,
    val palette: List<Int>,
    val selection: Int,
    val dropIndicator: Int,
    val glowAlphas: List<Float>,
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
        )

        /**
         * The appearance of [map]. Without a background of its own it follows the system ([systemDark]).
         * With one, the light or dark appearance is chosen by how light the background is (the image's
         * [imageLuminance] when it has one and it is known, else the colour), and the map's background colour
         * replaces the theme's.
         */
        fun forMap(map: MindMap, systemDark: Boolean, imageLuminance: Float?): MapTheme {
            val luminance = imageLuminance?.takeIf { map.backgroundImage != null } ?: map.background?.let(::relativeLuminance)
            val base = when {
                luminance != null -> if (luminance > LIGHT_THRESHOLD) Light else Dark
                else -> if (systemDark) Dark else Light
            }
            return map.background?.let { base.copy(background = it) } ?: base
        }

        init {
            check(Light.palette.size == MapLayouter.PALETTE_SIZE && Dark.palette.size == MapLayouter.PALETTE_SIZE)
        }
    }
}

/** Above this relative luminance a colour counts as light: dark text and the light appearance go on it. */
private const val LIGHT_THRESHOLD = 0.4f

/** Relative luminance (0 black to 1 white) of an ARGB colour. */
fun relativeLuminance(color: Int): Float {
    fun channel(shift: Int): Double {
        val c = ((color shr shift) and 0xFF) / 255.0
        return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }
    return (0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)).toFloat()
}

/** Black or white, whichever contrasts better with [background]. */
fun contrastingTextColor(background: Int): Int =
    if (relativeLuminance(background) > LIGHT_THRESHOLD) 0xFF1F1D24.toInt() else 0xFFFFFFFF.toInt()
