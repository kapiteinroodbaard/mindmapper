package io.github.wlenaerts.mindmapper.ui.start

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.wlenaerts.mindmapper.MindmapperApplication
import io.github.wlenaerts.mindmapper.OpenedDocument
import io.github.wlenaerts.mindmapper.R
import io.github.wlenaerts.mindmapper.core.format.MindMapFormatException
import io.github.wlenaerts.mindmapper.core.recent.RecentFile
import io.github.wlenaerts.mindmapper.core.recent.RecentFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** How a document reached the app; decides how access is kept and how failures are reported. */
enum class OpenSource { PICKER, RECENT, OTHER_APP }

sealed interface StartEvent {
    data class OpenEditor(val uri: Uri) : StartEvent
}

/** A thumbnail, or the information that the file can no longer be read. */
sealed interface Thumbnail {
    data class Image(val bitmap: Bitmap) : Thumbnail
    data object None : Thumbnail
    data object Unavailable : Thumbnail
}

class StartViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MindmapperApplication

    val recentFiles: StateFlow<RecentFiles> = app.recentFiles.files

    private val _thumbnails = MutableStateFlow<Map<String, Thumbnail>>(emptyMap())
    val thumbnails: StateFlow<Map<String, Thumbnail>> = _thumbnails.asStateFlow()

    /** An error message resource to show, or a recent entry that turned out to be unavailable. */
    private val _error = MutableStateFlow<Int?>(null)
    val error: StateFlow<Int?> = _error.asStateFlow()
    private val _unavailable = MutableStateFlow<RecentFile?>(null)
    val unavailable: StateFlow<RecentFile?> = _unavailable.asStateFlow()

    private val _events = MutableSharedFlow<StartEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<StartEvent> = _events.asSharedFlow()

    fun loadThumbnail(uri: String) {
        if (_thumbnails.value.containsKey(uri)) return
        viewModelScope.launch {
            val thumbnail = withContext(Dispatchers.IO) {
                runCatching {
                    app.repository.readThumbnail(Uri.parse(uri))?.let { bytes ->
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { Thumbnail.Image(it) }
                    } ?: Thumbnail.None
                }.getOrDefault(Thumbnail.Unavailable)
            }
            _thumbnails.update { it + (uri to thumbnail) }
        }
    }

    fun open(uri: Uri, source: OpenSource, intentFlags: Int = 0) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val document = app.repository.read(uri)
                    val writable = when (source) {
                        OpenSource.PICKER -> app.repository.persistAccess(uri, intentFlags)
                        OpenSource.RECENT -> app.repository.canWrite(uri)
                        OpenSource.OTHER_APP -> app.repository.persistAccess(uri, intentFlags)
                    }
                    OpenedDocument(document, app.repository.displayName(uri), writable)
                }
            }
            result.onSuccess { opened ->
                app.recentFiles.touch(uri.toString(), opened.name)
                app.handOver(uri, opened)
                _events.emit(StartEvent.OpenEditor(uri))
            }.onFailure { e ->
                val recent = recentFiles.value.entries.firstOrNull { it.uri == uri.toString() }
                when {
                    e is MindMapFormatException.NotAMindMap -> _error.value = R.string.error_not_mindmap
                    e is MindMapFormatException.NewerVersion -> _error.value = R.string.error_newer_version
                    e is MindMapFormatException -> _error.value = R.string.error_damaged
                    source == OpenSource.RECENT && recent != null -> {
                        _thumbnails.update { it + (recent.uri to Thumbnail.Unavailable) }
                        _unavailable.value = recent
                    }
                    else -> _error.value = R.string.error_file_unavailable
                }
            }
        }
    }

    fun dismissError() {
        _error.value = null
        _unavailable.value = null
    }

    fun setPinned(uri: String, pinned: Boolean) = app.recentFiles.setPinned(uri, pinned)

    fun remove(uri: String) {
        app.recentFiles.remove(uri)
        app.repository.releaseAccess(Uri.parse(uri))
        _thumbnails.update { it - uri }
        _unavailable.value = null
    }

    /** Thumbnails may be stale after editing; reload them when the start screen is shown again. */
    fun refreshThumbnails() = _thumbnails.update { emptyMap() }
}
