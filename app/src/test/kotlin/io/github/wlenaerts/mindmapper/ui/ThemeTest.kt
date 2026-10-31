package io.github.wlenaerts.mindmapper.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.wlenaerts.mindmapper.ui.theme.DarkColors
import io.github.wlenaerts.mindmapper.ui.theme.LightColors
import io.github.wlenaerts.mindmapper.ui.theme.MindmapperTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class ThemeTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun backgroundInTheme(): Color {
        var background = Color.Unspecified
        compose.setContent { MindmapperTheme { background = MaterialTheme.colorScheme.background } }
        compose.waitForIdle()
        return background
    }

    @Test
    @Config(qualifiers = "night")
    fun systemDarkModeUsesDarkColours() {
        assertEquals(DarkColors.background, backgroundInTheme())
    }

    @Test
    @Config(qualifiers = "notnight")
    fun systemLightModeUsesLightColours() {
        assertEquals(LightColors.background, backgroundInTheme())
    }
}
