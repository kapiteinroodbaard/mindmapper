package io.github.wlenaerts.mindmapper.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.wlenaerts.mindmapper.core.layout.BranchShades
import io.github.wlenaerts.mindmapper.core.model.BranchColorScheme
import io.github.wlenaerts.mindmapper.core.model.MindMap
import io.github.wlenaerts.mindmapper.core.model.Topic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(AndroidJUnit4::class)
class ColourRenderTest {
    private val renderer = MapRenderer(ApplicationProvider.getApplicationContext())
    private val purple = 0xFF8E44AD.toInt()
    private val red = 0xFFFF0000.toInt()
    private val map = MindMap(
        root = Topic(id = "root", text = "Centre", children = listOf(
            Topic(id = "a", text = "A", children = listOf(Topic(id = "a1", text = "Sub"))),
            Topic(id = "b", text = "B", children = listOf(Topic(id = "b1", text = "Sub"))),
            Topic(id = "c", text = "C"),
            Topic(id = "d", text = "D"),
        )),
    )

    private fun colors(m: MindMap, theme: MapTheme = MapTheme.Light) = renderer.branchColors(m, renderer.layout(m), theme)

    @Test
    fun customBranchColourAppliesToTheBranchAndItsSubtopicsOnly() {
        val m = map.copy(root = map.root.copy(children = map.root.children.map { if (it.id == "a") it.copy(branchColor = purple) else it }))
        val c = colors(m)
        assertEquals(purple, c["a"])
        assertEquals(purple, c["a1"])
        assertEquals(colors(map)["b"], c["b"])
        assertTrue(c["b"] != purple)
    }

    @Test
    fun customColourWinsInTheShadesScheme() {
        val m = map.copy(branchColors = BranchColorScheme.SHADES)
            .let { it.copy(root = it.root.copy(children = it.root.children.map { t -> if (t.id == "b") t.copy(branchColor = purple) else t })) }
        assertEquals(purple, colors(m)["b1"])
    }

    @Test
    fun shadesDifferPerBranchAndFollowTheCentralColour() {
        val m = map.copy(branchColors = BranchColorScheme.SHADES, centralColor = 0xFFC0392B.toInt())
        val c = colors(m)
        val mains = listOf("a", "b", "c", "d").map { c.getValue(it) }
        assertEquals(4, mains.toSet().size)
        assertEquals(c["a"], c["a1"])
        val green = colors(m.copy(centralColor = 0xFF2D6A4F.toInt()))
        assertTrue(green["a"] != c["a"])
    }

    @Test
    fun shadesAreLighterInTheDarkAppearance() {
        val m = map.copy(branchColors = BranchColorScheme.SHADES, centralColor = 0xFF2E86AB.toInt())
        val light = colors(m, MapTheme.Light)
        val dark = colors(m, MapTheme.Dark)
        listOf("a", "b", "c", "d").forEach { id ->
            assertTrue(id, BranchShades.toHsl(dark.getValue(id)).third > BranchShades.toHsl(light.getValue(id)).third)
        }
    }

    @Test
    fun shadesContrastWithANavyBackgroundColour() {
        val navy = 0xFF1D3557.toInt()
        val m = map.copy(branchColors = BranchColorScheme.SHADES, centralColor = 0xFF2E86AB.toInt(), background = navy)
        val c = colors(m, MapTheme.forMap(m, systemDark = false, imageLuminance = null))
        listOf("a", "b", "c", "d").forEach { id ->
            val ratio = BranchShades.contrastRatio(relativeLuminance(c.getValue(id)), relativeLuminance(navy))
            assertTrue("$id: $ratio", ratio >= 1.5f)
        }
    }

    @Test
    fun textColourIsUsedForTextPixels() {
        val m = map.copy(root = map.root.copy(children = map.root.children.map { t ->
            if (t.id == "a") t.copy(children = listOf(Topic(id = "a1", text = "Sub", textColor = red))) else t
        }))
        val layout = renderer.layout(m)
        val r = layout["a1"]!!.rect
        val bitmap = Bitmap.createBitmap((r.width + 8).toInt(), (r.height + 8).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.translate(4 - r.left, 4 - r.top)
        renderer.draw(canvas, m, layout, MapTheme.Light, { null })
        var reddish = 0
        for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
            val p = bitmap.getPixel(x, y)
            if (Color.red(p) > 200 && Color.green(p) < 80 && Color.blue(p) < 80) reddish++
        }
        assertTrue("red text pixels: $reddish", reddish > 20)
    }

    @Test
    fun appearanceFollowsTheSystemWithoutOwnBackground() {
        assertEquals(MapTheme.Dark, MapTheme.forMap(map, systemDark = true, imageLuminance = null))
        assertEquals(MapTheme.Light, MapTheme.forMap(map, systemDark = false, imageLuminance = null))
    }

    @Test
    fun darkBackgroundColourGivesDarkAppearanceInLightMode() {
        val navy = 0xFF1D3557.toInt()
        val theme = MapTheme.forMap(map.copy(background = navy), systemDark = false, imageLuminance = null)
        assertEquals(MapTheme.Dark.text, theme.text)
        assertEquals(navy, theme.background)
    }

    @Test
    fun lightBackgroundColourGivesLightAppearanceInDarkMode() {
        val yellow = 0xFFFFF3C4.toInt()
        val theme = MapTheme.forMap(map.copy(background = yellow), systemDark = true, imageLuminance = null)
        assertEquals(MapTheme.Light.text, theme.text)
        assertEquals(yellow, theme.background)
    }

    @Test
    fun brightImageGivesLightAppearanceInDarkMode() {
        val m = map.copy(backgroundImage = "images/bg.webp")
        val bright = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFFF0F0E0.toInt()) }
        val theme = renderer.theme(m, systemDark = true) { bright }
        assertEquals(MapTheme.Light.text, theme.text)
        val dark = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF101820.toInt()) }
        assertEquals(MapTheme.Dark.text, MapRenderer(ApplicationProvider.getApplicationContext()).theme(m, systemDark = false) { dark }.text)
    }

    @Test
    fun backgroundImageCoversTheArea() {
        val m = map.copy(background = Color.BLUE, backgroundImage = "images/bg.webp")
        val image = Bitmap.createBitmap(10, 20, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        val bitmap = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
        renderer.drawBackground(Canvas(bitmap), m, MapTheme.forMap(m, false, null), { image }, 100f, 50f)
        listOf(0 to 0, 99 to 0, 0 to 49, 99 to 49, 50 to 25).forEach { (x, y) -> assertEquals(Color.GREEN, bitmap.getPixel(x, y)) }
    }

    @Test
    fun backgroundStaysInsideItsArea() {
        val m = map.copy(background = Color.BLUE, backgroundImage = "images/bg.webp")
        val image = Bitmap.createBitmap(10, 40, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        renderer.drawBackground(Canvas(bitmap), m, MapTheme.forMap(m, false, null), { image }, 50f, 50f)
        assertEquals(Color.GREEN, bitmap.getPixel(25, 25))
        listOf(75 to 25, 25 to 75, 75 to 75).forEach { (x, y) -> assertEquals(Color.WHITE, bitmap.getPixel(x, y)) }
    }

    @Test
    fun numberedIconsExist() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        (0..9).forEach { n -> assertNotNull(IconSet.drawableFor("counter_$n")?.let { context.getDrawable(it) }) }
        assertEquals((0..9).map { "counter_$it" }, IconSet.icons.keys.take(10))
    }
}
