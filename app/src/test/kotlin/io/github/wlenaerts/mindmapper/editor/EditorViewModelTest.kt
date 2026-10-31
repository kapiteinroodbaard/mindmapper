package io.github.wlenaerts.mindmapper.editor

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.wlenaerts.mindmapper.MindmapperApplication
import io.github.wlenaerts.mindmapper.core.edit.resolveDrop
import io.github.wlenaerts.mindmapper.core.format.MindMapArchive
import io.github.wlenaerts.mindmapper.core.model.CentralShape
import io.github.wlenaerts.mindmapper.ui.editor.EditorViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
