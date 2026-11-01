package io.github.wlenaerts.mindmapper.editor

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.wlenaerts.mindmapper.MindmapperApplication
import io.github.wlenaerts.mindmapper.OpenedDocument
import io.github.wlenaerts.mindmapper.core.edit.resolveDrop
import io.github.wlenaerts.mindmapper.core.format.MindMapArchive
import io.github.wlenaerts.mindmapper.R
import io.github.wlenaerts.mindmapper.core.format.MindMapDocument
import io.github.wlenaerts.mindmapper.core.model.CentralShape
import io.github.wlenaerts.mindmapper.core.model.Side
import io.github.wlenaerts.mindmapper.core.model.newMindMap
import io.github.wlenaerts.mindmapper.render.TopicMetrics
import io.github.wlenaerts.mindmapper.ui.editor.EditorViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import android.net.Uri
import android.os.Looper
import java.io.File

@RunWith(AndroidJUnit4::class)
class EditorViewModelTest {
    private val app = ApplicationProvider.getApplicationContext<MindmapperApplication>()

    private fun newEditor() = EditorViewModel(app, SavedStateHandle())

    @Test
    fun newMapStartsWithSelectedCentralTopicAndIsUnsaved() {
        val vm = newEditor()
        val state = vm.state.value
        assertEquals(state.map!!.root.id, state.selectedId)
        assertTrue(state.dirty)
        assertFalse(state.canSaveInPlace)
    }

    @Test
    fun addEditDeleteUndoRedo() {
        val vm = newEditor()
        vm.addChild()
        val child = vm.state.value.selectedId!!
        assertEquals(child, vm.state.value.editTextOf)
        vm.setText(child, "Intro")
        vm.addSibling()
        assertEquals(2, vm.state.value.map!!.root.children.size)
        vm.deleteSelected()
        assertEquals(1, vm.state.value.map!!.root.children.size)
        vm.undo()
        assertEquals(2, vm.state.value.map!!.root.children.size)
        vm.redo()
        assertEquals(1, vm.state.value.map!!.root.children.size)
        assertEquals("Intro", vm.state.value.map!!.root.children.single().text)
    }

    @Test
    fun dropMovesTopic() {
        val vm = newEditor()
        vm.addChild(); val a = vm.state.value.selectedId!!
        vm.select(vm.state.value.map!!.root.id)
        vm.addChild(); val b = vm.state.value.selectedId!!
        val s = vm.state.value
        val box = s.layout!![a]!!.rect
        val target = resolveDrop(s.map!!, s.layout!!, b, box.centerX, box.centerY)!!
        vm.drop(b, target)
        assertEquals(listOf(b), vm.state.value.map!!.find(a)!!.children.map { it.id })
    }

    @Test
    fun saveAsWritesValidFileAndClearsDirty() {
        val vm = newEditor()
        vm.setCentralShape(CentralShape.DIAMOND)
        val file = File(app.cacheDir, "test.mindmap")
        var result: Boolean? = null
        vm.saveAs(Uri.fromFile(file)) { result = it }
        repeat(50) { if (result == null) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(20) } }
        assertEquals(true, result)
        assertFalse(vm.state.value.dirty)
        val read = file.inputStream().use { MindMapArchive.read(it) }
        assertEquals(CentralShape.DIAMOND, read.map.centralShape)
        assertTrue(file.inputStream().use { MindMapArchive.readThumbnail(it) }!!.isNotEmpty())
    }

    private fun idle(until: () -> Boolean) {
        repeat(50) { if (!until()) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(20) } }
    }

    @Test
    fun textAndFormatAreOneUndoableEdit() {
        val vm = newEditor()
        vm.addChild()
        val id = vm.state.value.selectedId!!
        // A main branch is bold by default: keeping bold stores the default, not an explicit value.
        vm.setTextAndFormat(id, "Intro", TopicMetrics.Format(bold = true, italic = true, underline = false))
        val topic = vm.state.value.map!!.find(id)!!
        assertEquals("Intro", topic.text)
        assertNull(topic.bold)
        assertEquals(true, topic.italic)
        assertNull(topic.underline)
        assertEquals(TopicMetrics.Format(bold = true, italic = true, underline = false), vm.textFormat(id))
        vm.setTextAndFormat(id, "Intro 2", TopicMetrics.Format(bold = false, italic = false, underline = true))
        assertEquals(false, vm.state.value.map!!.find(id)!!.bold)
        vm.undo()
        val undone = vm.state.value.map!!.find(id)!!
        assertEquals("Intro", undone.text)
        assertEquals(true, undone.italic)
        assertNull(undone.bold)
    }

    @Test
    fun addChildOnTheLeftOfTheCentralTopic() {
        val vm = newEditor()
        vm.addChild(Side.LEFT)
        val id = vm.state.value.selectedId!!
        assertEquals(Side.LEFT, vm.state.value.layout!![id]!!.side)
    }

    @Test
    fun renameNeverSavedMapSuggestsName() {
        val vm = newEditor()
        vm.rename("Ideas")
        assertEquals("Ideas", vm.state.value.title)
        assertEquals("Ideas.mindmap", vm.saveFileName())
    }

    @Test
    fun renameSavedMapRenamesFileAndRecentEntry() {
        val vm = newEditor()
        val dir = File(app.cacheDir, "rename").apply { deleteRecursively(); mkdirs() }
        var saved: Boolean? = null
        vm.saveAs(Uri.fromFile(File(dir, "Plan.mindmap"))) { saved = it }
        idle { saved != null }
        var renamed: Boolean? = null
        vm.rename("Plan 2026") { renamed = it }
        idle { renamed != null }
        assertEquals(true, renamed)
        assertEquals("Plan 2026", vm.state.value.title)
        assertFalse(File(dir, "Plan.mindmap").exists())
        assertTrue(File(dir, "Plan 2026.mindmap").exists())
        assertEquals(Uri.fromFile(File(dir, "Plan 2026.mindmap")), vm.state.value.uri)
        val recent = app.recentFiles.files.value.entries.map { it.uri }
        assertTrue(Uri.fromFile(File(dir, "Plan 2026.mindmap")).toString() in recent)
        assertFalse(Uri.fromFile(File(dir, "Plan.mindmap")).toString() in recent)
    }

    @Test
    fun renameReadOnlyMapShowsMessage() {
        val dir = File(app.cacheDir, "readonly").apply { deleteRecursively(); mkdirs() }
        val file = File(dir, "Shared.mindmap").apply { writeBytes(byteArrayOf()) }
        // Opened from another app without write access.
        val uri = Uri.fromFile(file)
        app.handOver(uri, OpenedDocument(MindMapDocument(newMindMap("x"), emptyMap()), "Shared.mindmap", writable = false))
        val vm = EditorViewModel(app, SavedStateHandle(mapOf("uri" to uri.toString())))
        vm.rename("New name")
        assertEquals(R.string.rename_not_possible, vm.state.value.message)
        assertEquals("Shared", vm.state.value.title)
        assertTrue(file.exists())
    }
}
