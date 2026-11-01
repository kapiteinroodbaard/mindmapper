package io.github.wlenaerts.mindmapper.core

import io.github.wlenaerts.mindmapper.core.edit.DropTarget
import io.github.wlenaerts.mindmapper.core.edit.EditHistory
import io.github.wlenaerts.mindmapper.core.edit.addChild
import io.github.wlenaerts.mindmapper.core.edit.addSiblingAfter
import io.github.wlenaerts.mindmapper.core.edit.delete
import io.github.wlenaerts.mindmapper.core.edit.move
import io.github.wlenaerts.mindmapper.core.edit.resolveDrop
import io.github.wlenaerts.mindmapper.core.edit.setCentralShape
import io.github.wlenaerts.mindmapper.core.edit.setIcon
import io.github.wlenaerts.mindmapper.core.edit.setImage
import io.github.wlenaerts.mindmapper.core.edit.setImageWidth
import io.github.wlenaerts.mindmapper.core.edit.setLayout
import io.github.wlenaerts.mindmapper.core.edit.setText
import io.github.wlenaerts.mindmapper.core.edit.setTextFormat
import io.github.wlenaerts.mindmapper.core.edit.setTrunk
import io.github.wlenaerts.mindmapper.core.model.MindMap
import io.github.wlenaerts.mindmapper.core.layout.MapLayouter
import io.github.wlenaerts.mindmapper.core.model.CentralShape
import io.github.wlenaerts.mindmapper.core.model.LayoutKind
import io.github.wlenaerts.mindmapper.core.model.Side
import io.github.wlenaerts.mindmapper.core.model.Topic
import io.github.wlenaerts.mindmapper.core.model.TopicImage
import io.github.wlenaerts.mindmapper.core.model.newMindMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MapEditsTest {

    private fun ids(t: Topic) = t.children.map { it.id }

    @Test
    fun newMapHasOnlyCentralTopic() {
        val m = newMindMap("Idea")
        assertEquals("Idea", m.root.text)
        assertTrue(m.root.children.isEmpty())
    }

    @Test
    fun addEditDelete() {
        var m = map(topic("a"), topic("b"))
        m = m.addChild("root", Topic(id = "c"))
        assertEquals(listOf("a", "b", "c"), ids(m.root))
        m = m.addSiblingAfter("a", Topic(id = "a2"))
        assertEquals(listOf("a", "a2", "b", "c"), ids(m.root))
        m = m.setText("a2", "Hello")
        assertEquals("Hello", m.find("a2")!!.text)
        m = m.addChild("a", Topic(id = "x")).delete("a")
        assertNull(m.find("x"))
        assertEquals(listOf("a2", "b", "c"), ids(m.root))
        assertSame(m, m.delete("root"))
        assertSame(m, m.addSiblingAfter("root", Topic()))
    }

    @Test
    fun siblingOfMainBranchKeepsItsSide() {
        val m = map(topic("a").copy(side = Side.LEFT)).addSiblingAfter("a", Topic(id = "b"))
        assertEquals(Side.LEFT, m.find("b")!!.side)
    }

    @Test
    fun textFormatStoresOffAsNull() {
        var m = map(topic("a")).setTextFormat("a", bold = false, italic = true, underline = false)
        assertEquals(false, m.find("a")!!.bold)
        assertEquals(true, m.find("a")!!.italic)
        assertNull(m.find("a")!!.underline)
        m = m.setTextFormat("a", bold = null, italic = false, underline = true)
        assertNull(m.find("a")!!.bold)
        assertNull(m.find("a")!!.italic)
        assertEquals(true, m.find("a")!!.underline)
    }

    @Test
    fun trunkIsClamped() {
        val m = map().setTrunk(100f, 0f)
        assertEquals(MindMap.MAX_TRUNK_WIDTH, m.trunkWidth)
        assertEquals(MindMap.MIN_TRUNK_TAPER, m.trunkTaper)
        assertEquals(20f, map().setTrunk(20f, 3f).trunkWidth)
    }

    @Test
    fun addChildOfCentralTopicTakesSide() {
        val m = map(topic("a")).addChild("root", Topic(id = "b"), Side.LEFT).addChild("a", Topic(id = "c"), Side.LEFT)
        assertEquals(Side.LEFT, m.find("b")!!.side)
        assertNull("side only applies to main branches", m.find("c")!!.side)
    }

    @Test
    fun layoutSwitchClearsSides() {
        val m = map(topic("a").copy(side = Side.LEFT), topic("b").copy(side = Side.RIGHT))
        assertSame(m, m.setLayout(LayoutKind.RADIAL))
        val switched = m.setLayout(LayoutKind.RIGHT)
        assertTrue(switched.root.children.all { it.side == null })
    }

    @Test
    fun iconImageStyle() {
        var m = map(topic("a")).setIcon("a", "star").setImage("a", TopicImage("images/x.webp", 160f, 2f))
        assertEquals("star", m.find("a")!!.icon)
        m = m.setImageWidth("a", 9999f)
        assertEquals(TopicImage.MAX_WIDTH, m.find("a")!!.image!!.width)
        m = m.setImage("a", null).setLayout(LayoutKind.RIGHT).setCentralShape(CentralShape.HEXAGON)
        assertNull(m.find("a")!!.image)
        assertEquals(LayoutKind.RIGHT, m.layout)
        assertEquals(CentralShape.HEXAGON, m.centralShape)
    }

    @Test
    fun moveReparentsReordersAndSetsSide() {
        val m = map(branch("a", 2), topic("b"), topic("c"))
        val reparented = m.move("b", "a", 2)
        assertEquals(listOf("a.0", "a.1", "b"), ids(reparented.find("a")!!))
        assertEquals(listOf("b", "c", "a"), ids(m.move("a", "root", 3).root))
        assertEquals(listOf("c", "a", "b"), ids(m.move("c", "root", 0).root))
        assertEquals(Side.LEFT, m.move("a", "root", 3, Side.LEFT).find("a")!!.side)
        // A main branch moved below another topic loses its side.
        assertNull(m.move("b", "a", 0).find("b")!!.side)
    }

    @Test
    fun moveIntoOwnSubtreeIsRejected() {
        val m = map(branch("a", 2))
        assertSame(m, m.move("a", "a.0", 0))
        assertSame(m, m.move("a", "a", 0))
        assertSame(m, m.move("root", "a", 0))
    }

    @Test
    fun historyUndoRedo() {
        val h = EditHistory(map(topic("a")), limit = 3)
        assertFalse(h.canUndo)
        h.push(h.current.delete("a"))
        assertTrue(h.current.root.children.isEmpty())
        h.undo()
        assertEquals(listOf("a"), ids(h.current.root))
        assertTrue(h.canRedo)
        h.redo()
        assertTrue(h.current.root.children.isEmpty())
        h.undo()
        h.push(h.current.setText("a", "new"))
        assertFalse("new change clears redo", h.canRedo)
        repeat(5) { h.push(h.current.setText("a", "v$it")) }
        var undos = 0
        while (h.canUndo) { h.undo(); undos++ }
        assertEquals(3, undos)
    }

    @Test
    fun dropResolution() {
        val m = map(branch("a", 2), topic("b"), topic("c"), topic("d"))
        val layout = MapLayouter.layout(m, fixedMeasure)
        val b = layout["b"]!!.rect
        val child = resolveDrop(m, layout, "c", b.centerX, b.centerY)!!
        assertEquals(DropTarget.Kind.CHILD, child.kind)
        assertEquals("b", child.parentId)
        val before = resolveDrop(m, layout, "c", b.centerX, b.top + 1)!!
        assertEquals(DropTarget.Kind.BEFORE, before.kind)
        assertEquals(listOf("a", "c", "b", "d"), ids(before.apply(m, "c").root))
        val after = resolveDrop(m, layout, "a.0", layout["a.1"]!!.rect.centerX, layout["a.1"]!!.rect.bottom - 1)!!
        assertEquals(listOf("a.1", "a.0"), ids(after.apply(m, "a.0").find("a")!!))
        // Onto its own child: nothing valid there.
        val own = layout["a.0"]!!.rect
        assertNull(resolveDrop(m, layout, "a", own.centerX, own.centerY))
        // Onto the left half of the central topic moves a right-side branch to the left.
        val root = layout["root"]!!.rect
        assertEquals(Side.RIGHT, layout["a"]!!.side)
        val toLeft = resolveDrop(m, layout, "a", root.left + 2, root.centerY)!!
        assertEquals(Side.LEFT, toLeft.side)
        assertEquals(Side.LEFT, MapLayouter.layout(toLeft.apply(m, "a"), fixedMeasure)["a"]!!.side)
        // Next to a main branch on the other side, the branch joins that side.
        val d = layout["d"]!!
        assertEquals(Side.LEFT, d.side)
        assertEquals(Side.LEFT, resolveDrop(m, layout, "a", d.rect.centerX, d.rect.top + 1)!!.side)
    }
}
