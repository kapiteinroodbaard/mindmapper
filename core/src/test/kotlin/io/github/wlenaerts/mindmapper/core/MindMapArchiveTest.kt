package io.github.wlenaerts.mindmapper.core

import io.github.wlenaerts.mindmapper.core.format.MindMapArchive
import io.github.wlenaerts.mindmapper.core.format.MindMapDocument
import io.github.wlenaerts.mindmapper.core.format.MindMapFormatException
import io.github.wlenaerts.mindmapper.core.model.CentralShape
import io.github.wlenaerts.mindmapper.core.model.LayoutKind
import io.github.wlenaerts.mindmapper.core.model.MindMap
import io.github.wlenaerts.mindmapper.core.model.Side
import io.github.wlenaerts.mindmapper.core.model.Topic
import io.github.wlenaerts.mindmapper.core.model.TopicImage
import io.github.wlenaerts.mindmapper.core.model.newMindMap
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class MindMapArchiveTest {

    private val imageA = TopicImage("images/a.webp", 160f, 1.5f)
    private val imageB = TopicImage("images/b.webp", 320f, 0.75f)
    private val sample = MindMap(
        layout = LayoutKind.RIGHT,
        centralShape = CentralShape.DIAMOND,
        centralColor = 0xFF123456.toInt(),
        trunkWidth = 28f,
        trunkTaper = 3.5f,
        root = Topic(
            id = "root", text = "Centre",
            children = listOf(
                Topic(id = "a", text = "A", icon = "star", image = imageA, side = Side.LEFT, children = listOf(Topic(id = "a1", text = "A1"))),
                Topic(id = "b", text = "B\nsecond line", bold = false, italic = true, underline = true, image = imageB),
            ),
        ),
    )
    private val images = mapOf(imageA.path to byteArrayOf(1, 2, 3), imageB.path to byteArrayOf(4, 5))

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z -> entries.forEach { (name, bytes) -> z.putNextEntry(ZipEntry(name)); z.write(bytes); z.closeEntry() } }
        return out.toByteArray()
    }

    @Test
    fun roundTripKeepsEverything() {
        val bytes = MindMapArchive.write(MindMapDocument(sample, images), thumbnailPng = byteArrayOf(9))
        val read = MindMapArchive.read(ByteArrayInputStream(bytes))
        assertEquals(sample, read.map)
        assertArrayEquals(images.getValue(imageA.path), read.images.getValue(imageA.path))
        assertArrayEquals(images.getValue(imageB.path), read.images.getValue(imageB.path))
        assertArrayEquals(byteArrayOf(9), MindMapArchive.readThumbnail(ByteArrayInputStream(bytes)))
    }

    @Test
    fun containerHasExpectedEntries() {
        val bytes = MindMapArchive.write(MindMapDocument(sample, images), thumbnailPng = byteArrayOf(9))
        val names = buildList { ZipInputStream(ByteArrayInputStream(bytes)).use { z -> while (true) add((z.nextEntry ?: break).name) } }
        assertEquals(setOf("map.json", "images/a.webp", "images/b.webp", "thumbnail.png"), names.toSet())
    }

    @Test
    fun mapJsonHasFormatVersion() {
        assertTrue(MindMapArchive.encodeMap(newMindMap("x")).contains("\"formatVersion\": 1"))
    }

    @Test
    fun unusedImagesAreNotStored() {
        val bytes = MindMapArchive.write(MindMapDocument(newMindMap("x"), images), null)
        assertTrue(MindMapArchive.read(ByteArrayInputStream(bytes)).images.isEmpty())
    }

    @Test(expected = MindMapFormatException.NotAMindMap::class)
    fun rejectsNonZip() {
        MindMapArchive.read(ByteArrayInputStream("hello, not a zip".toByteArray()))
    }

    @Test(expected = MindMapFormatException.NotAMindMap::class)
    fun rejectsZipWithoutMapJson() {
        MindMapArchive.read(ByteArrayInputStream(zip("readme.txt" to "hi".toByteArray())))
    }

    @Test(expected = MindMapFormatException.Damaged::class)
    fun rejectsInvalidJson() {
        MindMapArchive.read(ByteArrayInputStream(zip("map.json" to "{ not json".toByteArray())))
    }

    @Test(expected = MindMapFormatException.MissingImage::class)
    fun rejectsMissingImage() {
        val json = MindMapArchive.encodeMap(sample).toByteArray()
        MindMapArchive.read(ByteArrayInputStream(zip("map.json" to json)))
    }

    @Test
    fun rejectsNewerVersion() {
        val json = """{"formatVersion": 99, "root": {"id": "r", "text": "x"}}""".toByteArray()
        try {
            MindMapArchive.read(ByteArrayInputStream(zip("map.json" to json)))
            throw AssertionError("expected NewerVersion")
        } catch (e: MindMapFormatException.NewerVersion) {
            assertEquals(99, e.version)
        }
    }

    @Test
    fun recognisesMindMapFiles() {
        val bytes = MindMapArchive.write(MindMapDocument(sample, images), null)
        assertTrue(MindMapArchive.isMindMap(ByteArrayInputStream(bytes)))
        assertFalse(MindMapArchive.isMindMap(ByteArrayInputStream(zip("x.txt" to byteArrayOf(1)))))
        assertFalse(MindMapArchive.isMindMap(ByteArrayInputStream(byteArrayOf(1, 2, 3))))
    }

    @Test
    fun ignoresUnknownKeys() {
        val json = """{"formatVersion": 1, "future": true, "root": {"id": "r", "text": "x", "colour": "red"}}"""
        assertEquals("x", MindMapArchive.decodeMap(json).root.text)
    }

    @Test
    fun fileWithoutTrunkAndFormattingUsesDefaults() {
        val m = MindMapArchive.decodeMap("""{"formatVersion": 1, "root": {"id": "r", "text": "x"}}""")
        assertEquals(MindMap.DEFAULT_TRUNK_WIDTH, m.trunkWidth)
        assertEquals(MindMap.DEFAULT_TRUNK_TAPER, m.trunkTaper)
        assertEquals(null, m.root.bold)
        assertEquals(null, m.root.italic)
        assertEquals(null, m.root.underline)
    }

    @Test
    fun outOfRangeTrunkIsClamped() {
        val m = MindMapArchive.decodeMap("""{"formatVersion": 1, "trunkWidth": 100, "trunkTaper": 0, "root": {"id": "r", "text": "x"}}""")
        assertEquals(MindMap.MAX_TRUNK_WIDTH, m.trunkWidth)
        assertEquals(MindMap.MIN_TRUNK_TAPER, m.trunkTaper)
    }

    @Test
    fun unsetFormattingIsLeftOut() {
        val json = MindMapArchive.encodeMap(newMindMap("x"))
        assertFalse(json.contains("bold"))
        assertFalse(json.contains("italic"))
        assertFalse(json.contains("underline"))
        assertTrue(json.contains("\"formatVersion\": 1"))
    }
}
