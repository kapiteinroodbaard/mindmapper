package io.github.wlenaerts.mindmapper.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.wlenaerts.mindmapper.AppInfo
import io.github.wlenaerts.mindmapper.ui.theme.MindmapperTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class AppShellTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun launch() = compose.setContent { MindmapperTheme { MindmapperApp() } }

    @Test
    fun startScreenIsShownOnLaunch() {
        launch()
        compose.onNodeWithText("Mindmapper").assertIsDisplayed()
        compose.onNodeWithText("Mind maps for preparing and giving presentations").assertIsDisplayed()
    }

    @Test
    fun navigatesToAboutAndBack() {
        launch()
        compose.onNodeWithContentDescription("About").performClick()
        compose.onNodeWithTag("about").assertIsDisplayed()
        compose.onNodeWithText("GNU GPL v3").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Mind maps for preparing and giving presentations").assertIsDisplayed()
    }

    @Test
    fun aboutShowsInstalledVersion() {
        launch()
        compose.onNodeWithContentDescription("About").performClick()
        val versionName = compose.activity.packageManager
            .getPackageInfo(compose.activity.packageName, 0).versionName
        compose.onNodeWithText("Version $versionName").assertIsDisplayed()
    }

    @Test
    fun sourceCodeLinkOpensBrowserIntent() {
        launch()
        compose.onNodeWithContentDescription("About").performClick()
        compose.onNodeWithText("Source code").performClick()
        val intent = shadowOf(compose.activity.application).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(Uri.parse(AppInfo.SOURCE_CODE_URL), intent.data)
    }

    @Test
    fun aboutSurvivesRecreation() {
        val tester = StateRestorationTester(compose)
        tester.setContent { MindmapperTheme { MindmapperApp() } }
        compose.onNodeWithContentDescription("About").performClick()
        tester.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("about").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land")
    fun tabletLandscapeShowsAllControls() {
        launch()
        compose.onNodeWithContentDescription("About").assertIsDisplayed()
        compose.onNodeWithText("Mind maps for preparing and giving presentations").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-port")
    fun phonePortraitShowsAllControls() {
        launch()
        compose.onNodeWithContentDescription("About").assertIsDisplayed()
        compose.onNodeWithText("Mind maps for preparing and giving presentations").assertIsDisplayed()
    }
}
