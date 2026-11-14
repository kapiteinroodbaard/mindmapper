package io.github.wlenaerts.mindmapper.core.layout

import kotlin.math.abs

/** Branch colours of the shades scheme: shades of the central topic's colour, light to dark clockwise. */
object BranchShades {
    const val LIGHTEST = 0.78f
    const val DARKEST = 0.28f

    /** Low enough to keep grey central colours as visibly tinted greys; high enough to keep strong hues. */
    private const val MIN_SATURATION = 0.35f

    /** The shade for the main branch at clockwise [position] of [count], in the hue of [central] (ARGB). */
    fun shade(central: Int, position: Int, count: Int): Int {
        val (hue, saturation, _) = toHsl(central)
        val lightness = if (count <= 1) (LIGHTEST + DARKEST) / 2 else LIGHTEST + (DARKEST - LIGHTEST) * position / (count - 1)
        return fromHsl(hue, maxOf(saturation, MIN_SATURATION), lightness)
    }

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
