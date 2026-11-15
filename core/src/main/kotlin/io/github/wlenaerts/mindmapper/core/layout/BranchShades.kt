package io.github.wlenaerts.mindmapper.core.layout

import kotlin.math.abs

/**
 * Branch colours of the shades scheme: shades of the central topic's colour, light to dark clockwise, in a
 * lightness range that suits the background so every shade stays visible on it.
 */
object BranchShades {
    const val LIGHT_LIGHTEST = 0.65f
    const val LIGHT_DARKEST = 0.25f
    const val DARK_LIGHTEST = 0.88f
    const val DARK_DARKEST = 0.45f

    /** The least contrast ratio a shade keeps with the background. */
    const val MIN_CONTRAST = 1.5f

    /** How far a shade's lightness may be moved away from the background. */
    const val MIN_LIGHTNESS = 0.05f
    const val MAX_LIGHTNESS = 0.95f
    private const val STEP = 0.02f

    /** Low enough to keep grey central colours as visibly tinted greys; high enough to keep strong hues. */
    private const val MIN_SATURATION = 0.35f

    /**
     * The shade for the main branch at clockwise [position] of [count], in the hue of [central] (ARGB), for a
     * map whose appearance is dark when [darkBackground] and whose background has relative luminance
     * [backgroundLuminance]. A shade too close to the background is made darker (light background) or lighter
     * (dark background) until it reaches [MIN_CONTRAST] or the lightness limit.
     */
    fun shade(central: Int, position: Int, count: Int, darkBackground: Boolean, backgroundLuminance: Float): Int {
        val (hue, s, _) = toHsl(central)
        val saturation = maxOf(s, MIN_SATURATION)
        val lightest = if (darkBackground) DARK_LIGHTEST else LIGHT_LIGHTEST
        val darkest = if (darkBackground) DARK_DARKEST else LIGHT_DARKEST
        var lightness = if (count <= 1) (lightest + darkest) / 2 else lightest + (darkest - lightest) * position / (count - 1)
        var color = fromHsl(hue, saturation, lightness)
        val step = if (darkBackground) STEP else -STEP
        while (contrastRatio(luminance(color), backgroundLuminance) < MIN_CONTRAST) {
            val next = (lightness + step).coerceIn(MIN_LIGHTNESS, MAX_LIGHTNESS)
            if (next == lightness) break
            lightness = next
            color = fromHsl(hue, saturation, lightness)
        }
        return color
    }

    /** Relative luminance (0 black to 1 white) of an ARGB colour, as defined by WCAG. */
    fun luminance(color: Int): Float {
        fun channel(shift: Int): Double {
            val c = ((color shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        return (0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)).toFloat()
    }

    /** WCAG contrast ratio (1 to 21) between two relative luminances. */
    fun contrastRatio(l1: Float, l2: Float): Float = (maxOf(l1, l2) + 0.05f) / (minOf(l1, l2) + 0.05f)

    /** Hue (0 to 360), saturation and lightness (0 to 1) of an ARGB colour. */
    fun toHsl(color: Int): Triple<Float, Float, Float> {
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val l = (max + min) / 2
        val d = max - min
        if (d == 0f) return Triple(0f, 0f, l)
        val s = d / (1 - abs(2 * l - 1))
        val h = when (max) {
            r -> 60 * (((g - b) / d).mod(6f))
            g -> 60 * ((b - r) / d + 2)
            else -> 60 * ((r - g) / d + 4)
        }
        return Triple(h, s, l)
    }

    /** An opaque ARGB colour from hue (0 to 360), saturation and lightness (0 to 1). */
    fun fromHsl(h: Float, s: Float, l: Float): Int {
        val c = (1 - abs(2 * l - 1)) * s
        val x = c * (1 - abs((h / 60).mod(2f) - 1))
        val m = l - c / 2
        val (r, g, b) = when {
            h < 60 -> Triple(c, x, 0f)
            h < 120 -> Triple(x, c, 0f)
            h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c)
            h < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun channel(v: Float) = ((v + m) * 255).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
    }
}
