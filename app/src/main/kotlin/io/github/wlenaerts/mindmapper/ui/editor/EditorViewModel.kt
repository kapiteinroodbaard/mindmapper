package io.github.wlenaerts.mindmapper.ui.editor

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.github.wlenaerts.mindmapper.MindmapperApplication
import io.github.wlenaerts.mindmapper.R
import io.github.wlenaerts.mindmapper.core.edit.DropTarget
import io.github.wlenaerts.mindmapper.core.edit.EditHistory
import io.github.wlenaerts.mindmapper.core.edit.addChild
import io.github.wlenaerts.mindmapper.core.edit.addSiblingAfter
import io.github.wlenaerts.mindmapper.core.edit.delete
import io.github.wlenaerts.mindmapper.core.edit.setCentralColor
import io.github.wlenaerts.mindmapper.core.edit.setBranchStyle
import io.github.wlenaerts.mindmapper.core.edit.setCentralShape
import io.github.wlenaerts.mindmapper.core.edit.setIcon
import io.github.wlenaerts.mindmapper.core.edit.setImage
import io.github.wlenaerts.mindmapper.core.edit.setImageWidth
import io.github.wlenaerts.mindmapper.core.edit.setLayout
import io.github.wlenaerts.mindmapper.core.edit.setText
import io.github.wlenaerts.mindmapper.core.edit.setTextFormat
import io.github.wlenaerts.mindmapper.core.edit.setTrunk
import io.github.wlenaerts.mindmapper.core.format.MindMapDocument
import io.github.wlenaerts.mindmapper.core.layout.MapLayout
import io.github.wlenaerts.mindmapper.core.model.BranchStyle
import io.github.wlenaerts.mindmapper.core.model.CentralShape
import io.github.wlenaerts.mindmapper.core.model.LayoutKind
import io.github.wlenaerts.mindmapper.core.model.MindMap
import io.github.wlenaerts.mindmapper.core.model.Side
import io.github.wlenaerts.mindmapper.core.model.Topic
import io.github.wlenaerts.mindmapper.core.model.newMindMap
import io.github.wlenaerts.mindmapper.document.ImageCache
import io.github.wlenaerts.mindmapper.document.ImageImporter
import io.github.wlenaerts.mindmapper.document.isValidTitle
import io.github.wlenaerts.mindmapper.document.titleFromFileName
import io.github.wlenaerts.mindmapper.export.ExportFormat
import io.github.wlenaerts.mindmapper.render.MapRenderer
import io.github.wlenaerts.mindmapper.render.TopicMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class EditorState(
    val loading: Boolean = true,
    val map: MindMap? = null,
    val layout: MapLayout? = null,
    val selectedId: String? = null,
    val title: String = "",
    val uri: Uri? = null,
    val writable: Boolean = false,
    val dirty: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val autosave: Boolean = true,
    /** Id of a freshly added topic whose text editor should open. */
    val editTextOf: String? = null,
    /** A string resource to show once as a message. */
    val message: Int? = null,
    /** Set when the document could not be loaded; the editor closes. */
    val loadFailed: Boolean = false,
) {
    /** Whether "Save" can write to the current file, or must ask for a location. */
    val canSaveInPlace: Boolean get() = uri != null && writable

    val autosaveActive: Boolean get() = autosave && canSaveInPlace
}

class EditorViewModel(application: Application, savedState: SavedStateHandle) : AndroidViewModel(application) {
    private val app = application as MindmapperApplication
    val renderer = MapRenderer(application)

    /** Embedded images by path; only grows during a session, unused ones are dropped on save. */
    private val images = ConcurrentHashMap<String, ByteArray>()
    val imageCache = ImageCache({ images[it] })

    private lateinit var history: EditHistory
    private var savedMap: MindMap? = null
    private var autosaveJob: Job? = null

    private val _state = MutableStateFlow(EditorState(autosave = app.settings.autosave))
    val state: StateFlow<EditorState> = _state.asStateFlow()

    init {
        val uriText = savedState.get<String>("uri")
        if (uriText == null) {
            start(MindMapDocument(newMindMap(application.getString(R.string.default_central_topic)), emptyMap()), null, "", writable = false, saved = false)
        } else {
            val uri = Uri.parse(uriText)
            val handed = app.takeOver(uri)
            if (handed != null) {
                start(handed.document, uri, handed.name, handed.writable, saved = true)
            } else {
                viewModelScope.launch {
                    val loaded = withContext(Dispatchers.IO) {
                        runCatching {
                            val doc = app.repository.read(uri)
                            Triple(doc, app.repository.displayName(uri), app.repository.canWrite(uri))
                        }
                    }
                    loaded.onSuccess { (doc, name, writable) -> start(doc, uri, name, writable, saved = true) }
                        .onFailure { _state.update { it.copy(loading = false, loadFailed = true, message = R.string.error_file_unavailable) } }
                }
            }
        }
    }

    private fun start(document: MindMapDocument, uri: Uri?, name: String, writable: Boolean, saved: Boolean) {
        images.putAll(document.images)
        history = EditHistory(document.map)
        savedMap = if (saved) document.map else null
        _state.update {
            it.copy(
                loading = false,
                map = document.map,
                layout = renderer.layout(document.map),
                uri = uri,
                title = titleFromFileName(name),
                writable = writable,
                dirty = !saved,
                // A brand-new map starts with its central topic selected, ready to add branches.
                selectedId = if (uri == null) document.map.root.id else null,
            )
        }
    }

    private fun edit(change: (MindMap) -> MindMap, select: String? = _state.value.selectedId, editText: String? = null) {
        val map = _state.value.map ?: return
        val next = change(map)
        if (next == map || !::history.isInitialized) return
        history.push(next)
        show(next, select, editText)
    }

    private fun show(map: MindMap, select: String?, editText: String? = null) {
        _state.update {
            it.copy(
                map = map,
                layout = renderer.layout(map),
                selectedId = select?.takeIf { id -> map.find(id) != null },
                dirty = map != savedMap,
                canUndo = history.canUndo,
                canRedo = history.canRedo,
                editTextOf = editText,
            )
        }
        scheduleAutosave()
    }

    fun select(id: String?) = _state.update { it.copy(selectedId = id) }

    fun consumeEditText() = _state.update { it.copy(editTextOf = null) }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    /** Adds a child to the selected topic; [side] places a new main branch in the radial layout. */
    fun addChild(side: Side? = null) {
        val parent = _state.value.selectedId ?: return
        val topic = Topic()
        edit({ it.addChild(parent, topic, side) }, select = topic.id, editText = topic.id)
    }

    fun addSibling() {
        val id = _state.value.selectedId ?: return
        val map = _state.value.map ?: return
        if (id == map.root.id) return
        val topic = Topic()
        edit({ it.addSiblingAfter(id, topic) }, select = topic.id, editText = topic.id)
    }

    fun setText(id: String, text: String) = edit({ it.setText(id, text.trimEnd()) })

    /** The formatting topic [id] is drawn with, including the bold default of its level. */
    fun textFormat(id: String): TopicMetrics.Format? {
        val map = _state.value.map ?: return null
        val topic = map.find(id) ?: return null
        return renderer.metrics.format(topic, map.depthOf(id))
    }

    /** Sets text and formatting as one edit; bold equal to the level's default is stored as "default". */
    fun setTextAndFormat(id: String, text: String, format: TopicMetrics.Format) {
        val map = _state.value.map ?: return
        val defaultBold = renderer.metrics.style(map.depthOf(id)).bold
        val bold = format.bold.takeIf { it != defaultBold }
        edit({ it.setText(id, text.trimEnd()).setTextFormat(id, bold, format.italic, format.underline) })
    }

    fun deleteSelected() {
        val id = _state.value.selectedId ?: return
        val map = _state.value.map ?: return
        val parent = map.parentOf(id)?.id
        edit({ it.delete(id) }, select = parent)
    }

    fun setIcon(id: String, icon: String?) = edit({ it.setIcon(id, icon) })

    fun setImageWidth(id: String, width: Float) = edit({ it.setImageWidth(id, width) })

    fun removeImage(id: String) = edit({ it.setImage(id, null) })

    fun importImage(id: String, uri: Uri) {
        viewModelScope.launch {
            val imported = withContext(Dispatchers.IO) { runCatching { ImageImporter(app.contentResolver).import(uri) } }
            imported.onSuccess { image ->
                images[image.path] = image.bytes
                edit({ it.setImage(id, image.image) })
            }.onFailure { _state.update { it.copy(message = R.string.error_image) } }
        }
    }

    fun setLayout(kind: LayoutKind) = edit({ it.setLayout(kind) })

    fun setCentralShape(shape: CentralShape) = edit({ it.setCentralShape(shape) })

    fun setCentralColor(color: Int) = edit({ it.setCentralColor(color) })

    fun setTrunk(width: Float, taper: Float) = edit({ it.setTrunk(width, taper) })

    fun setBranchStyle(id: String, style: BranchStyle) = edit({ it.setBranchStyle(id, style) })

    /**
     * Renames the map to [name]. A saved map's file is renamed; a never-saved map keeps the name as the
     * suggested file name. When the file cannot be renamed, a message suggests "Save as".
     */
    fun rename(name: String, onDone: (Boolean) -> Unit = {}) {
        val title = name.trim()
        val s = _state.value
        if (!isValidTitle(title)) return onDone(false)
        val uri = s.uri
        if (uri == null) {
            _state.update { it.copy(title = title) }
            return onDone(true)
        }
        if (title == s.title) return onDone(true)
        if (!s.writable) {
            _state.update { it.copy(message = R.string.rename_not_possible) }
            return onDone(false)
        }
        // A pending autosave must not write to the old location after the rename.
        autosaveJob?.cancel()
        viewModelScope.launch {
            val renamed = withContext(Dispatchers.IO) {
                runCatching { app.repository.rename(uri, "$title.mindmap") }.getOrNull()?.also { newUri ->
                    if (newUri != uri) {
                        app.repository.persistAccess(newUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        app.repository.releaseAccess(uri)
                        app.recentFiles.remove(uri.toString())
                    }
                    app.recentFiles.touch(newUri.toString(), app.repository.displayName(newUri))
                }
            }
            if (renamed == null) {
                _state.update { it.copy(message = R.string.rename_not_possible) }
            } else {
                _state.update { it.copy(uri = renamed, title = titleFromFileName(app.repository.displayName(renamed))) }
            }
            scheduleAutosave()
            onDone(renamed != null)
        }
    }

    fun drop(draggedId: String, target: DropTarget) = edit({ target.apply(it, draggedId) }, select = draggedId)

    fun undo() {
        if (!::history.isInitialized || !history.canUndo) return
        show(history.undo(), _state.value.selectedId)
    }

    fun redo() {
        if (!::history.isInitialized || !history.canRedo) return
        show(history.redo(), _state.value.selectedId)
    }

    fun setAutosave(enabled: Boolean) {
        app.settings.autosave = enabled
        _state.update { it.copy(autosave = enabled) }
        scheduleAutosave()
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        val s = _state.value
        if (!s.autosaveActive || !s.dirty) return
        autosaveJob = viewModelScope.launch {
            delay(AUTOSAVE_DELAY_MS)
            saveNow()
        }
    }

    /** Saves immediately when autosave applies (used when the app goes to the background or the editor closes). */
    fun autosaveNow() {
        val s = _state.value
        if (s.autosaveActive && s.dirty) {
            autosaveJob?.cancel()
            viewModelScope.launch { saveNow() }
        }
    }

    /** Saves to the current file. Returns false when there is no writable file (the UI then asks "Save as"). */
    fun save(onDone: (Boolean) -> Unit = {}): Boolean {
        if (!_state.value.canSaveInPlace) return false
        viewModelScope.launch { onDone(saveNow()) }
        return true
    }

    /** Saves to a newly chosen [uri], which becomes the document's file. */
    fun saveAs(uri: Uri, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            // A document created through the picker is always writable by us.
            val name = withContext(Dispatchers.IO) {
                app.repository.persistAccess(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                app.repository.displayName(uri)
            }
            _state.update { it.copy(uri = uri, writable = true, title = titleFromFileName(name)) }
            onDone(saveNow())
        }
    }

    private suspend fun saveNow(): Boolean {
        val s = _state.value
        val map = s.map ?: return false
        val uri = s.uri ?: return false
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val used = map.imagePaths().associateWith { images.getValue(it) }
                val thumbnail = runCatching { app.exporter.thumbnail(map, imageCache) }.getOrNull()
                app.repository.write(uri, MindMapDocument(map, used), thumbnail)
                app.recentFiles.touch(uri.toString(), app.repository.displayName(uri))
            }
        }
        return if (result.isSuccess) {
            savedMap = map
            _state.update { it.copy(dirty = it.map != map) }
            true
        } else {
            _state.update { it.copy(writable = false, message = R.string.error_save) }
            false
        }
    }

    /** Writes an export of the current map to [target]. */
    fun export(format: ExportFormat, target: Uri, onDone: (Boolean) -> Unit) {
        val map = _state.value.map ?: return
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    app.contentResolver.openOutputStream(target, "wt")!!.use { app.exporter.write(format, map, imageCache, it) }
                }.isSuccess
            }
            if (!ok) _state.update { it.copy(message = R.string.error_export) }
            onDone(ok)
        }
    }

    /** Writes an export to the app's cache for sharing and returns the file. */
    fun exportForSharing(format: ExportFormat, onDone: (File?) -> Unit) {
        val map = _state.value.map ?: return
        val name = exportFileName(format)
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = File(app.cacheDir, "exports").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
                    File(dir, name).also { f -> f.outputStream().use { app.exporter.write(format, map, imageCache, it) } }
                }.getOrNull()
            }
            if (file == null) _state.update { it.copy(message = R.string.error_export) }
            onDone(file)
        }
    }

    fun exportFileName(format: ExportFormat): String {
        val title = _state.value.title.ifBlank { _state.value.map?.root?.text?.lines()?.firstOrNull().orEmpty() }
        val safe = title.replace(Regex("""[\\/:*?"<>|\n]"""), " ").trim().ifBlank { "mindmap" }
        return "$safe.${format.extension}"
    }

    /** Suggested file name for "Save as". */
    fun saveFileName(): String = exportFileName(ExportFormat.PDF).removeSuffix(".pdf") + ".mindmap"

    private companion object {
        const val AUTOSAVE_DELAY_MS = 2000L
    }
}
