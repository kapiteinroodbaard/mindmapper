package io.github.wlenaerts.mindmapper.core.format

import io.github.wlenaerts.mindmapper.core.model.CentralShape
import io.github.wlenaerts.mindmapper.core.model.HexColorSerializer
import io.github.wlenaerts.mindmapper.core.model.LayoutKind
import io.github.wlenaerts.mindmapper.core.model.MindMap
import io.github.wlenaerts.mindmapper.core.model.Topic
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** A map together with the bytes of every image it references, keyed by entry path. */
data class MindMapDocument(val map: MindMap, val images: Map<String, ByteArray>)

/** Why a file could not be read as a `.mindmap` container. */
sealed class MindMapFormatException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** Not a ZIP, or a ZIP without `map.json`. */
    class NotAMindMap : MindMapFormatException("Not a Mindmapper file")

    /** `map.json` exists but cannot be parsed. */
    class Damaged(cause: Throwable?) : MindMapFormatException("The file is damaged", cause)

    /** `map.json` references an image that is not in the container. */
    class MissingImage(val path: String) : MindMapFormatException("Missing image $path")

    /** Written by a newer app version with an incompatible format. */
    class NewerVersion(val version: Int) : MindMapFormatException("Format version $version is not supported")
}

/**
 * Reads and writes the `.mindmap` container: a ZIP with `map.json`, the images under `images/` and a
 * `thumbnail.png` preview.
 */
object MindMapArchive {
    const val FORMAT_VERSION = 1
    const val MAP_ENTRY = "map.json"
    const val THUMBNAIL_ENTRY = "thumbnail.png"
    const val IMAGE_DIR = "images/"

    // Guards against hostile archives; real documents are far smaller because images are downscaled.
    private const val MAX_ENTRY_BYTES = 64L * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 512L * 1024 * 1024

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        explicitNulls = false
    }

    @Serializable
    private data class MapFile(
        val formatVersion: Int,
        val layout: LayoutKind = LayoutKind.RADIAL,
        val centralShape: CentralShape = CentralShape.ROUNDED_RECTANGLE,
        @Serializable(with = HexColorSerializer::class)
        val centralColor: Int = MindMap.DEFAULT_CENTRAL_COLOR,
        val trunkWidth: Float = MindMap.DEFAULT_TRUNK_WIDTH,
        val trunkTaper: Float = MindMap.DEFAULT_TRUNK_TAPER,
        val root: Topic,
    )

    fun encodeMap(map: MindMap): String =
        json.encodeToString(MapFile.serializer(), MapFile(
            FORMAT_VERSION, map.layout, map.centralShape, map.centralColor, map.trunkWidth, map.trunkTaper, map.root,
        ))

    fun decodeMap(text: String): MindMap {
        val element = try {
            json.parseToJsonElement(text)
        } catch (e: SerializationException) {
            throw MindMapFormatException.Damaged(e)
        }
        val version = try {
            element.jsonObject["formatVersion"]?.jsonPrimitive?.int
        } catch (e: IllegalArgumentException) {
            throw MindMapFormatException.Damaged(e)
        } ?: throw MindMapFormatException.Damaged(null)
        if (version > FORMAT_VERSION) throw MindMapFormatException.NewerVersion(version)
        val file = try {
            json.decodeFromJsonElement(MapFile.serializer(), element)
        } catch (e: SerializationException) {
            throw MindMapFormatException.Damaged(e)
        } catch (e: IllegalArgumentException) {
            throw MindMapFormatException.Damaged(e)
        }
        return MindMap(
            file.layout,
            file.centralShape,
            file.centralColor,
            file.trunkWidth.coerceIn(MindMap.MIN_TRUNK_WIDTH, MindMap.MAX_TRUNK_WIDTH),
            file.trunkTaper.coerceIn(MindMap.MIN_TRUNK_TAPER, MindMap.MAX_TRUNK_TAPER),
            file.root,
        )
    }

    /** Builds the complete archive in memory, so a failure never leaves a half-written file behind. */
    fun write(document: MindMapDocument, thumbnailPng: ByteArray?): ByteArray {
        val buffer = ByteArrayOutputStream()
        ZipOutputStream(buffer).use { zip ->
            zip.putNextEntry(ZipEntry(MAP_ENTRY))
            zip.write(encodeMap(document.map).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            // Only images still referenced by the map are stored; removed images disappear on save.
            for (path in document.map.imagePaths().sorted()) {
                val bytes = document.images[path] ?: throw MindMapFormatException.MissingImage(path)
                zip.putNextEntry(ZipEntry(path))
                zip.write(bytes)
                zip.closeEntry()
            }
            if (thumbnailPng != null) {
                zip.putNextEntry(ZipEntry(THUMBNAIL_ENTRY))
                zip.write(thumbnailPng)
                zip.closeEntry()
            }
        }
        return buffer.toByteArray()
    }

    fun read(input: InputStream): MindMapDocument {
        val entries = readEntries(input) { true }
        val mapBytes = entries[MAP_ENTRY] ?: throw MindMapFormatException.NotAMindMap()
        val map = decodeMap(mapBytes.toString(Charsets.UTF_8))
        val images = map.imagePaths().associateWith { path ->
            entries[path] ?: throw MindMapFormatException.MissingImage(path)
        }
        return MindMapDocument(map, images)
    }

    /** Reads only the thumbnail, or null when the container has none. */
    fun readThumbnail(input: InputStream): ByteArray? = readEntries(input) { it == THUMBNAIL_ENTRY }[THUMBNAIL_ENTRY]

    /** Whether [input] looks like a `.mindmap` container (a ZIP with a readable `map.json`). */
    fun isMindMap(input: InputStream): Boolean = try {
        readEntries(input) { it == MAP_ENTRY }[MAP_ENTRY]?.let { decodeMap(it.toString(Charsets.UTF_8)) } != null
    } catch (e: MindMapFormatException) {
        e is MindMapFormatException.NewerVersion
    }

    private fun readEntries(input: InputStream, wanted: (String) -> Boolean): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        var total = 0L
        try {
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory || !wanted(entry.name)) continue
                    val out = ByteArrayOutputStream()
                    val chunk = ByteArray(16 * 1024)
                    var entryBytes = 0L
                    while (true) {
                        val n = zip.read(chunk)
                        if (n < 0) break
                        entryBytes += n
                        total += n
                        if (entryBytes > MAX_ENTRY_BYTES || total > MAX_TOTAL_BYTES) throw MindMapFormatException.Damaged(null)
                        out.write(chunk, 0, n)
                    }
                    result[entry.name] = out.toByteArray()
                }
            }
        } catch (e: ZipException) {
            throw MindMapFormatException.NotAMindMap()
        }
        return result
    }
}
