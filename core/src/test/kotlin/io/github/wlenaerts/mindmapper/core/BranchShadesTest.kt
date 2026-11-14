package io.github.wlenaerts.mindmapper.core

import io.github.wlenaerts.mindmapper.core.layout.BranchShades
import io.github.wlenaerts.mindmapper.core.layout.MapLayouter
import io.github.wlenaerts.mindmapper.core.model.LayoutKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BranchShadesTest {
    private val red = 0xFFC0392B.toInt()

    @Test
    fun fourBranchesGetDistinctShadesLightToDarkInTheCentralHue() {
        val shades = (0 until 4).map { BranchShades.shade(red, it, 4) }
        assertEquals(4, shades.toSet().size)
        val hsl = shades.map(BranchShades::toHsl)
        val lightness = hsl.map { it.third }
        assertEquals(lightness.sortedDescending(), lightness)
        assertEquals(BranchShades.LIGHTEST, lightness.first(), 0.01f)
        assertEquals(BranchShades.DARKEST, lightness.last(), 0.01f)
        val hue = BranchShades.toHsl(red).first
        hsl.forEach { assertEquals(hue, it.first, 2f) }
    }

    @Test
    fun oneBranchGetsTheMiddleShade() {
        val l = BranchShades.toHsl(BranchShades.shade(red, 0, 1)).third
        assertEquals((BranchShades.LIGHTEST + BranchShades.DARKEST) / 2, l, 0.01f)
    }

    @Test
    fun greyCentralColourStillGivesAVisibleRamp() {
        val grey = 0xFF808080.toInt()
        assertTrue(BranchShades.toHsl(BranchShades.shade(grey, 0, 3)).second > 0.3f)
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
