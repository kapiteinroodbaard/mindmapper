package io.github.wlenaerts.mindmapper.core

import io.github.wlenaerts.mindmapper.core.format.MindMapArchive
import io.github.wlenaerts.mindmapper.core.format.MindMapDocument
import io.github.wlenaerts.mindmapper.core.format.MindMapFormatException
import io.github.wlenaerts.mindmapper.core.model.BranchColorScheme
import io.github.wlenaerts.mindmapper.core.model.BranchStyle
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
        branchColors = BranchColorScheme.SHADES,
        background = 0xFFFFF3C4.toInt(),
        backgroundImage = "images/bg.webp",
        root = Topic(
            id = "root", text = "Centre",
            children = listOf(
                Topic(id = "a", text = "A", icon = "star", image = imageA, side = Side.LEFT, style = BranchStyle.PLANT, branchColor = 0xFF8E44AD.toInt(), durationSeconds = 300, children = listOf(Topic(id = "a1", text = "A1"))),
                Topic(id = "b", text = "B\nsecond line", bold = false, italic = true, underline = true, textColor = 0xFFE63946.toInt(), image = imageB),
            ),
        ),
    )
    private val images = mapOf(imageA.path to byteArrayOf(1, 2, 3), imageB.path to byteArrayOf(4, 5), "images/bg.webp" to byteArrayOf(7))

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
        assertEquals(setOf("map.json", "images/a.webp", "images/b.webp", "images/bg.webp", "thumbnail.png"), names.toSet())
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

    @Test
    fun branchStyleIsStoredByName() {
        val json = MindMapArchive.encodeMap(sample)
        assertTrue(json.contains("\"style\": \"plant\""))
        assertTrue(MindMapArchive.encodeMap(newMindMap("x")).let { !it.contains("style") && it.contains("\"formatVersion\": 1") })
    }

    @Test
    fun fileWithoutStylesIsStandard() {
        val m = MindMapArchive.decodeMap("""{"formatVersion": 1, "root": {"id": "r", "text": "x", "children": [{"id": "a", "text": "a"}]}}""")
        assertEquals(null, m.root.children.single().style)
    }

    @Test
    fun unknownStyleReadsAsStandard() {
        val m = MindMapArchive.decodeMap(
            """{"formatVersion": 1, "root": {"id": "r", "text": "x", "children": [{"id": "a", "text": "a", "style": "glitter"}, {"id": "b", "text": "b", "style": "lightning"}]}}""",
        )
        assertEquals(listOf(null, BranchStyle.LIGHTNING), m.root.children.map { it.style })
    }

    @Test
    fun stylesBelowMainBranchesAreDropped() {
        val m = MindMapArchive.decodeMap(
            """{"formatVersion": 1, "root": {"id": "r", "text": "x", "style": "plant", "children": [{"id": "a", "text": "a", "style": "standard", "children": [{"id": "a1", "text": "a1", "style": "blurred"}]}]}}""",
        )
        assertEquals(null, m.root.style)
        assertEquals(null, m.root.children.single().style)
        assertEquals(null, m.root.children.single().children.single().style)
    }

    @Test
    fun durationIsStoredInSeconds() {
        val json = MindMapArchive.encodeMap(sample)
        assertTrue(json.contains("\"durationSeconds\": 300"))
        // Branch b has no duration, so the key appears only once.
        assertEquals(1, Regex("durationSeconds").findAll(json).count())
        assertTrue(json.contains("\"formatVersion\": 1"))
    }

    @Test
    fun durationsBelowMainBranchesAreDropped() {
        val m = MindMapArchive.decodeMap(
            """{"formatVersion": 1, "root": {"id": "r", "text": "x", "durationSeconds": 60, "children": [{"id": "a", "text": "a", "durationSeconds": 120, "children": [{"id": "a1", "text": "a1", "durationSeconds": 60}]}, {"id": "b", "text": "b", "durationSeconds": 0}]}}""",
        )
        assertEquals(null, m.root.durationSeconds)
        assertEquals(listOf(120, null), m.root.children.map { it.durationSeconds })
        assertEquals(null, m.root.children.first().children.single().durationSeconds)
    }

    @Test
    fun coloursAndBackgroundAreStoredAsHex() {
        val json = MindMapArchive.encodeMap(sample)
        assertTrue(json.contains("\"branchColors\": \"shades\""))
        assertTrue(json.contains("\"background\": \"#FFF3C4\""))
        assertTrue(json.contains("\"backgroundImage\": \"images/bg.webp\""))
        assertTrue(json.contains("\"branchColor\": \"#8E44AD\""))
        assertTrue(json.contains("\"textColor\": \"#E63946\""))
        assertTrue(json.contains("\"formatVersion\": 1"))
    }

    @Test
    fun unsetColoursAndBackgroundAreLeftOut() {
        val json = MindMapArchive.encodeMap(newMindMap("x"))
        listOf("branchColors", "background", "branchColor", "textColor").forEach { assertFalse(it, json.contains(it)) }
    }

    @Test
    fun fileWithoutColoursUsesDefaults() {
        val m = MindMapArchive.decodeMap("""{"formatVersion": 1, "root": {"id": "r", "text": "x", "children": [{"id": "a", "text": "a"}]}}""")
        assertEquals(BranchColorScheme.PALETTE, m.branchColors)
        assertEquals(null, m.background)
        assertEquals(null, m.backgroundImage)
        assertFalse(m.hasOwnBackground)
        assertEquals(null, m.root.children.single().branchColor)
        assertEquals(null, m.root.children.single().textColor)
    }

    @Test
    fun unknownSchemeReadsAsPalette() {
        val m = MindMapArchive.decodeMap("""{"formatVersion": 1, "branchColors": "rainbow", "root": {"id": "r", "text": "x"}}""")
        assertEquals(BranchColorScheme.PALETTE, m.branchColors)
    }

    @Test
    fun branchColoursBelowMainBranchesAreDropped() {
        val m = MindMapArchive.decodeMap(
            """{"formatVersion": 1, "root": {"id": "r", "text": "x", "branchColor": "#112233", "children": [{"id": "a", "text": "a", "branchColor": "#445566", "children": [{"id": "a1", "text": "a1", "branchColor": "#778899", "textColor": "#ABCDEF"}]}]}}""",
        )
        assertEquals(null, m.root.branchColor)
        assertEquals(0xFF445566.toInt(), m.root.children.single().branchColor)
        val sub = m.root.children.single().children.single()
        assertEquals(null, sub.branchColor)
        assertEquals(0xFFABCDEF.toInt(), sub.textColor)
    }

    @Test
    fun backgroundImageIsStoredInTheContainerAndDroppedWhenRemoved() {
        val bytes = MindMapArchive.write(MindMapDocument(sample, images), thumbnailPng = null)
        assertArrayEquals(byteArrayOf(7), MindMapArchive.read(ByteArrayInputStream(bytes)).images.getValue("images/bg.webp"))
        val without = MindMapArchive.write(MindMapDocument(sample.copy(backgroundImage = null), images), thumbnailPng = null)
        val names = ZipInputStream(ByteArrayInputStream(without)).use { z -> generateSequence { z.nextEntry?.name }.toList() }
        assertFalse(names.contains("images/bg.webp"))
    }
}
