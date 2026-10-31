package io.github.wlenaerts.mindmapper

import android.app.Application
import android.net.Uri
import io.github.wlenaerts.mindmapper.core.format.MindMapDocument
import io.github.wlenaerts.mindmapper.document.DocumentRepository
import io.github.wlenaerts.mindmapper.document.RecentFilesStore
import io.github.wlenaerts.mindmapper.document.Settings
import io.github.wlenaerts.mindmapper.export.MapExporter

class MindmapperApplication : Application() {
    val repository by lazy { DocumentRepository(contentResolver) }
    val recentFiles by lazy { RecentFilesStore(this) }
    val settings by lazy { Settings(this) }
    val exporter by lazy { MapExporter(this) }

    /**
     * A document already read (and validated) by the start screen, handed to the editor so it is not read
     * twice. Holds at most one document.
     */
    private var pending: Pair<Uri, OpenedDocument>? = null

    fun handOver(uri: Uri, document: OpenedDocument) {
        pending = uri to document
    }

    fun takeOver(uri: Uri): OpenedDocument? = pending?.takeIf { it.first == uri }?.second.also { pending = null }
}

/** A document read from storage together with what the app may do with its file. */
data class OpenedDocument(val document: MindMapDocument, val name: String, val writable: Boolean)
