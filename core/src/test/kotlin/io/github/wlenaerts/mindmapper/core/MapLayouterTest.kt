package io.github.wlenaerts.mindmapper.core

import io.github.wlenaerts.mindmapper.core.edit.setLayout
import io.github.wlenaerts.mindmapper.core.layout.MapLayouter
import io.github.wlenaerts.mindmapper.core.layout.Size
import io.github.wlenaerts.mindmapper.core.model.LayoutKind
import io.github.wlenaerts.mindmapper.core.model.Side
import io.github.wlenaerts.mindmapper.core.model.Topic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class MapLayouterTest {

    @Test
    fun fourEqualBranchesAreSplitClockwise() {
        val layout = MapLayouter.layout(map(topic("1"), topic("2"), topic("3"), topic("4")), fixedMeasure)
        assertEquals(Side.RIGHT, layout["1"]!!.side)
        assertEquals(Side.RIGHT, layout["2"]!!.side)
        assertEquals(Side.LEFT, layout["3"]!!.side)
        assertEquals(Side.LEFT, layout["4"]!!.side)
        assertTrue("1 above 2", layout["1"]!!.rect.top < layout["2"]!!.rect.top)
        assertTrue("4 above 3", layout["4"]!!.rect.top < layout["3"]!!.rect.top)
        assertTrue(layout["1"]!!.rect.left > layout["root"]!!.rect.right)
        assertTrue(layout["3"]!!.rect.right < layout["root"]!!.rect.left)
    }

    @Test
    fun oneLargeBranchIsAloneOnTheRight() {
        val layout = MapLayouter.layout(map(branch("1", 10), topic("2"), topic("3"), topic("4")), fixedMeasure)
        assertEquals(Side.RIGHT, layout["1"]!!.side)
        listOf("2", "3", "4").forEach { assertEquals(Side.LEFT, layout[it]!!.side) }
    }

    @Test
    fun chosenSideIsRespectedAndOthersRebalance() {
        val m = map(topic("1").copy(side = Side.LEFT), topic("2"), topic("3"), topic("4"))
        val layout = MapLayouter.layout(m, fixedMeasure)
        assertEquals(Side.LEFT, layout["1"]!!.side)
        val right = listOf("2", "3", "4").count { layout[it]!!.side == Side.RIGHT }
        assertEquals(2, right)
    }

    @Test
    fun rightLayoutIgnoresChosenSides() {
        val m = map(topic("1").copy(side = Side.LEFT), topic("2"), topic("3"), layout = LayoutKind.RIGHT)
        val layout = MapLayouter.layout(m, fixedMeasure)
        listOf("1", "2", "3").forEach { assertEquals(Side.RIGHT, layout[it]!!.side) }
        assertTrue(layout["1"]!!.rect.top < layout["2"]!!.rect.top && layout["2"]!!.rect.top < layout["3"]!!.rect.top)
        // Vertically centred on the central topic.
        assertEquals(layout["root"]!!.rect.centerY, layout["2"]!!.rect.centerY, 0.01f)
    }

    @Test
    fun childrenAreCentredOnParentInOrder() {
        val layout = MapLayouter.layout(map(branch("a", 3)), fixedMeasure)
        val parent = layout["a"]!!.rect
        val kids = (0..2).map { layout["a.$it"]!!.rect }
        assertEquals(parent.centerY, kids[1].centerY, 0.01f)
        assertTrue(kids[0].top < kids[1].top && kids[1].top < kids[2].top)
        assertEquals(kids[1].top - kids[0].bottom, kids[2].top - kids[1].bottom, 0.01f)
        kids.forEach { assertTrue(it.left > parent.right) }
    }

    @Test
    fun noOverlapInLargeRandomMap() {
        val random = Random(42)
        var counter = 0
        fun randomTopic(depth: Int): Topic {
            val n = if (depth >= 4) 0 else random.nextInt(1, 5)
            return Topic(id = "t${counter++}", text = "x", children = List(n) { randomTopic(depth + 1) })
        }
        val mains = List(7) { randomTopic(1) }
        for (kind in LayoutKind.entries) {
            val layout = MapLayouter.layout(map(*mains.toTypedArray(), layout = kind), { _, d -> Size(40f + d * 10, 20f + (d % 2) * 14) })
            assertTrue(layout.boxes.size > 100)
            val rects = layout.boxes.map { it.rect }
            for (i in rects.indices) for (j in i + 1 until rects.size) {
                assertFalse("overlap ${layout.boxes[i].topicId} / ${layout.boxes[j].topicId} in $kind", rects[i].overlaps(rects[j]))
            }
        }
    }

    @Test
    fun gapsCountWhenBalancingManySmallBranches() {
        // One tall branch against many single topics: without counting the gaps all small ones end up left.
        val layout = MapLayouter.layout(map(branch("big", 12), *(1..12).map { topic("s$it") }.toTypedArray()), fixedMeasure)
        fun sideHeight(side: Side): Float = layout.boxes.filter { it.depth >= 1 && it.side == side }.map { it.rect }
            .reduce { a, b -> a.union(b) }.height
        val right = sideHeight(Side.RIGHT)
        val left = sideHeight(Side.LEFT)
        assertTrue("right $right left $left", abs(right - left) <= 0.25f * maxOf(right, left))
    }

    @Test
    fun switchingLayoutClearsChosenSides() {
        val m = map(topic("1").copy(side = Side.RIGHT), topic("2").copy(side = Side.RIGHT), topic("3").copy(side = Side.RIGHT), topic("4"))
        val back = m.setLayout(LayoutKind.RIGHT).setLayout(LayoutKind.RADIAL)
        val layout = MapLayouter.layout(back, fixedMeasure)
        assertEquals(2, listOf("1", "2", "3", "4").count { layout[it]!!.side == Side.LEFT })
    }

    @Test
    fun colourSlotsDifferForNeighbours() {
        for (cyclic in listOf(true, false)) for (n in 1..25) {
            val slots = MapLayouter.colorSlots(n, cyclic)
            for (i in 1 until n) assertNotEquals("n=$n i=$i", slots[i - 1], slots[i])
            if (cyclic && n > 1) assertNotEquals("n=$n wrap", slots.first(), slots.last())
        }
    }

    @Test
    fun descendantsInheritMainBranchSlot() {
        val layout = MapLayouter.layout(map(branch("a", 2), branch("b", 2)), fixedMeasure)
        assertEquals(layout["a"]!!.colorSlot, layout["a.1"]!!.colorSlot)
        assertNotEquals(layout["a"]!!.colorSlot, layout["b"]!!.colorSlot)
        assertEquals(-1, layout["root"]!!.colorSlot)
    }

    @Test
    fun reorderingReassignsColours() {
        val before = MapLayouter.layout(map(topic("a"), topic("b"), topic("c")), fixedMeasure)
        val after = MapLayouter.layout(map(topic("b"), topic("a"), topic("c")), fixedMeasure)
        assertEquals(before["a"]!!.colorSlot, after["b"]!!.colorSlot)
    }
}
