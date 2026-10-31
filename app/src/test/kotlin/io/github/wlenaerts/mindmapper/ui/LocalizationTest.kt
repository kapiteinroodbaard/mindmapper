package io.github.wlenaerts.mindmapper.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.wlenaerts.mindmapper.ui.theme.MindmapperTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class LocalizationTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun launch() = compose.setContent { MindmapperTheme { MindmapperApp() } }

    @Test
    @Config(qualifiers = "nl")
    fun dutchSystemLanguageShowsDutch() {
        launch()
        compose.onNodeWithText("Mindmaps voor het voorbereiden en geven van presentaties").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en")
    fun englishSystemLanguageShowsEnglish() {
        launch()
        compose.onNodeWithText("Mind maps for preparing and giving presentations").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "de")
    fun unsupportedLanguageFallsBackToEnglish() {
        launch()
        compose.onNodeWithText("Mind maps for preparing and giving presentations").assertIsDisplayed()
    }
}
