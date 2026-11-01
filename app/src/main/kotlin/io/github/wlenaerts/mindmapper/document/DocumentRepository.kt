package io.github.wlenaerts.mindmapper.document

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import io.github.wlenaerts.mindmapper.core.format.MindMapArchive
import io.github.wlenaerts.mindmapper.core.format.MindMapDocument
import java.io.File
import java.io.IOException

/** Reads and writes `.mindmap` files at content URIs chosen by the user. */
class DocumentRepository(private val resolver: ContentResolver) {

    /** Reads the document; throws `MindMapFormatException` for invalid files and [IOException] when unreadable. */
    fun read(uri: Uri): MindMapDocument =
        (resolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")).use { MindMapArchive.read(it) }

    fun readThumbnail(uri: Uri): ByteArray? =
        (resolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")).use { MindMapArchive.readThumbnail(it) }

    /** Writes the complete archive, built in memory first so a failure leaves the old file intact. */
    fun write(uri: Uri, document: MindMapDocument, thumbnailPng: ByteArray?) {
        val bytes = MindMapArchive.write(document, thumbnailPng)
        (resolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot write $uri")).use { it.write(bytes) }
    }

    fun displayName(uri: Uri): String {
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0)?.let { return it }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "mindmap.mindmap"
    }

    /** Keeps read/write access across restarts when the provider allows it. Returns whether writing is allowed. */
    fun persistAccess(uri: Uri, flags: Int): Boolean {
        val wanted = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { resolver.takePersistableUriPermission(uri, wanted) }
            .recoverCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        return canWrite(uri, flags)
    }

    /** Whether the app currently holds write access to [uri]. */
    fun canWrite(uri: Uri, intentFlags: Int = 0): Boolean {
        if (intentFlags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0) return true
        if (uri.scheme == ContentResolver.SCHEME_FILE) return true
        return resolver.persistedUriPermissions.any { it.uri == uri && it.isWritePermission }
    }

    /**
     * Renames the file at [uri] to [fileName] and returns its (possibly new) URI, or null when the location
     * does not support renaming or a file with that name already exists.
     */
    fun rename(uri: Uri, fileName: String): Uri? = when (uri.scheme) {
        ContentResolver.SCHEME_FILE -> {
            val file = File(uri.path ?: return null)
            val target = File(file.parentFile, fileName)
            if (target.exists() || !file.renameTo(target)) null else Uri.fromFile(target)
        }
        else -> runCatching { DocumentsContract.renameDocument(resolver, uri, fileName) }.getOrNull()
    }

    fun releaseAccess(uri: Uri) {
        runCatching {
            resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
    }
}

/** Characters that cannot appear in a map title, because the title is also the file name. */
private val invalidTitleCharacters = Regex("""[\\/:*?"<>|\n]""")

/** Whether [title] can be used as a map name and file name. */
fun isValidTitle(title: String): Boolean = title.isNotBlank() && !invalidTitleCharacters.containsMatchIn(title)

/** Removes a trailing `.mindmap` for display as a title. */
fun titleFromFileName(name: String): String = name.removeSuffix(".mindmap").removeSuffix(".bin").removeSuffix(".mindmap")
