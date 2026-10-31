package io.github.wlenaerts.mindmapper.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.wlenaerts.mindmapper.ui.about.AboutScreen
import io.github.wlenaerts.mindmapper.ui.editor.EditorScreen
import io.github.wlenaerts.mindmapper.ui.editor.EditorViewModel
import io.github.wlenaerts.mindmapper.ui.presentation.PresentationScreen
import io.github.wlenaerts.mindmapper.ui.start.OpenSource
import io.github.wlenaerts.mindmapper.ui.start.StartEvent
import io.github.wlenaerts.mindmapper.ui.start.StartScreen
import io.github.wlenaerts.mindmapper.ui.start.StartViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.serialization.Serializable

@Serializable
object StartRoute

@Serializable
object AboutRoute

/** The editor for the document at [uri], or a new map when null. */
@Serializable
data class EditorRoute(val uri: String? = null)

@Serializable
object PresentationRoute

/** A document handed to the app by another app (view or share), with the intent's grant flags. */
data class IncomingDocument(val uri: Uri, val flags: Int)

@Composable
fun MindmapperApp(incoming: Flow<IncomingDocument> = emptyFlow()) {
    val navController = rememberNavController()
    val startViewModel: StartViewModel = viewModel()

    LaunchedEffect(Unit) {
        startViewModel.events.collect { event ->
            when (event) {
                is StartEvent.OpenEditor -> navController.openEditor(event.uri.toString())
            }
        }
    }
    LaunchedEffect(incoming) {
        incoming.collect { startViewModel.open(it.uri, OpenSource.OTHER_APP, it.flags) }
    }

    NavHost(navController = navController, startDestination = StartRoute) {
        composable<StartRoute> {
            StartScreen(
                viewModel = startViewModel,
                onAbout = { navController.navigate(AboutRoute) },
                onNew = { navController.openEditor(null) },
            )
        }
        composable<AboutRoute> {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable<EditorRoute> {
            val editor: EditorViewModel = viewModel()
            EditorScreen(
                viewModel = editor,
                onClose = { navController.popBackStack(StartRoute, inclusive = false) },
                onPresent = { navController.navigate(PresentationRoute) },
            )
        }
        composable<PresentationRoute> {
            // Presentation shares the editor's view model, so it shows exactly the document being edited.
            val editorEntry = navController.previousBackStackEntry ?: return@composable
            val editor: EditorViewModel = viewModel(editorEntry)
            PresentationScreen(viewModel = editor, onExit = { navController.popBackStack() })
        }
    }
}

/** Opens the editor, replacing an editor that is already open. */
private fun NavHostController.openEditor(uri: String?) {
    navigate(EditorRoute(uri)) {
        popUpTo(StartRoute)
        launchSingleTop = false
    }
}
