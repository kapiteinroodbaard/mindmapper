package io.github.wlenaerts.mindmapper.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.wlenaerts.mindmapper.ui.about.AboutScreen
import io.github.wlenaerts.mindmapper.ui.start.StartScreen
import kotlinx.serialization.Serializable

@Serializable
object StartRoute

@Serializable
object AboutRoute

@Composable
fun MindmapperApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = StartRoute) {
        composable<StartRoute> {
            StartScreen(onAbout = { navController.navigate(AboutRoute) })
        }
        composable<AboutRoute> {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
