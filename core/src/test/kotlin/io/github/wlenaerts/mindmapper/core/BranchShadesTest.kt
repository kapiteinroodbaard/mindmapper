package io.github.wlenaerts.mindmapper.core

import io.github.wlenaerts.mindmapper.core.layout.BranchShades
import io.github.wlenaerts.mindmapper.core.layout.MapLayouter
import io.github.wlenaerts.mindmapper.core.model.LayoutKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BranchShadesTest {
    private val red = 0xFFC0392B.toInt()
    private val blue = 0xFF2E86AB.toInt()
    private val white = BranchShades.luminance(0xFFFFFFFF.toInt())
    private val darkTheme = BranchShades.luminance(0xFF17171B.toInt())
    private val navy = BranchShades.luminance(0xFF1D3557.toInt())

    private fun shades(central: Int, dark: Boolean, background: Float, count: Int = 4) =
        (0 until count).map { BranchShades.shade(central, it, count, dark, background) }

    private fun lightness(color: Int) = BranchShades.toHsl(color).third

    @Test
    fun fourBranchesGetDistinctShadesLightToDarkInTheCentralHue() {
        val shades = shades(red, dark = false, background = white)
        assertEquals(4, shades.toSet().size)
        val hsl = shades.map(BranchShades::toHsl)
        val lightness = hsl.map { it.third }
        assertEquals(lightness.sortedDescending(), lightness)
        assertEquals(BranchShades.LIGHT_LIGHTEST, lightness.first(), 0.01f)
        assertEquals(BranchShades.LIGHT_DARKEST, lightness.last(), 0.01f)
        val hue = BranchShades.toHsl(red).first
        hsl.forEach { assertEquals(hue, it.first, 2f) }
    }

    @Test
    fun oneBranchGetsTheMiddleShade() {
        val l = lightness(BranchShades.shade(red, 0, 1, false, white))
        assertEquals((BranchShades.LIGHT_LIGHTEST + BranchShades.LIGHT_DARKEST) / 2, l, 0.01f)
    }

    @Test
    fun shadesOnADarkBackgroundAreLighterAndStillOrdered() {
        val light = shades(red, dark = false, background = white).map(::lightness)
        val dark = shades(red, dark = true, background = darkTheme).map(::lightness)
        assertEquals(dark.sortedDescending(), dark)
        light.zip(dark).forEach { (l, d) -> assertTrue("$d > $l", d > l) }
        assertEquals(BranchShades.DARK_LIGHTEST, dark.first(), 0.01f)
    }

    @Test
    fun blueShadesKeepTheContrastFloor() {
        listOf(false to white, true to darkTheme, true to navy).forEach { (dark, background) ->
            val shades = shades(blue, dark, background)
            shades.forEach { c ->
                val ratio = BranchShades.contrastRatio(BranchShades.luminance(c), background)
                assertTrue("ratio $ratio on $background", ratio >= BranchShades.MIN_CONTRAST)
            }
            val l = shades.map(::lightness)
            assertEquals(l.sortedDescending(), l)
        }
    }

    @Test
    fun midOrangeBackgroundReachesTheFloorOrTheLimit() {
        val orange = BranchShades.luminance(0xFFE67E22.toInt())
        listOf(false, true).forEach { dark ->
            shades(blue, dark, orange).forEach { c ->
                val ratio = BranchShades.contrastRatio(BranchShades.luminance(c), orange)
                val l = lightness(c)
                assertTrue(
                    "ratio $ratio at lightness $l",
                    ratio >= BranchShades.MIN_CONTRAST || l <= BranchShades.MIN_LIGHTNESS + 0.01f || l >= BranchShades.MAX_LIGHTNESS - 0.01f,
                )
            }
        }
    }

    @Test
    fun greyCentralColourStillGivesAVisibleRamp() {
        val grey = 0xFF808080.toInt()
        assertTrue(BranchShades.toHsl(BranchShades.shade(grey, 0, 3, false, white)).second > 0.3f)
    }

    @Test
    fun hslRoundTrip() {
        listOf(red, 0xFF2A9D8F.toInt(), 0xFF3D5A80.toInt(), 0xFFE9C46A.toInt()).forEach { c ->
            val (h, s, l) = BranchShades.toHsl(c)
            val back = BranchShades.fromHsl(h, s, l)
            for (shift in listOf(16, 8, 0)) assertEquals(((c shr shift) and 0xFF).toFloat(), ((back shr shift) and 0xFF).toFloat(), 2f)
        }
    }

    @Test
    fun layoutGivesClockwisePositions() {
        val m = map(branch("a", 1), topic("b"), topic("c"), topic("d")).copy(layout = LayoutKind.RADIAL)
        val layout = MapLayouter.layout(m, fixedMeasure)
        assertEquals(4, layout.mainBranchCount)
        assertEquals(setOf(0, 1, 2, 3), m.root.children.map { layout[it.id]!!.clockwise }.toSet())
        // Subtopics share their main branch's position.
        assertEquals(layout["a"]!!.clockwise, layout["a.0"]!!.clockwise)
        assertEquals(-1, layout["root"]!!.clockwise)
    }
}
