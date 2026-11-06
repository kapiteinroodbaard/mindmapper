package io.github.wlenaerts.mindmapper.ui.presentation

import androidx.activity.compose.LocalActivity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.wlenaerts.mindmapper.R
import io.github.wlenaerts.mindmapper.core.view.Viewport
import io.github.wlenaerts.mindmapper.ui.editor.EditorViewModel

import io.github.wlenaerts.mindmapper.ui.map.MIN_ZOOM
import io.github.wlenaerts.mindmapper.ui.map.MapCanvas
import io.github.wlenaerts.mindmapper.ui.map.fitViewport
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import io.github.wlenaerts.mindmapper.core.timing.schedule
import kotlinx.coroutines.delay

/** Full-screen, read-only view used as the presenter's guide. Shares the editor's document. */
@Composable
fun PresentationScreen(viewModel: EditorViewModel, onExit: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val map = state.map ?: return
    val layout = state.layout ?: return
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    var viewport by remember { mutableStateOf<Viewport?>(null) }
    var focusedId by remember { mutableStateOf<String?>(null) }
    val progress = remember { Animatable(0f) }
    val timer = viewModel.presentationTimer
    val timerState by timer.state.collectAsState()
    val schedule = remember(map) { map.schedule() }
    var elapsedSeconds by remember { mutableLongStateOf(timer.elapsedMillis() / 1000) }
    LaunchedEffect(timerState) {
        elapsedSeconds = timer.elapsedMillis() / 1000
        while (timerState.running) {
            delay(TIMER_REFRESH_MILLIS)
            elapsedSeconds = timer.elapsedMillis() / 1000
        }
    }
    val scheduledId = schedule.scheduledAt(elapsedSeconds)

    ImmersiveMode()

    BoxWithConstraints(Modifier.fillMaxSize().testTag("presentation")) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val overview = fitViewport(layout, width, height, density)
        LaunchedEffect(width, height) { if (viewport == null || focusedId == null) viewport = overview }

        fun animateTo(target: Viewport) {
            val from = viewport ?: target
            scope.launch {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) {
                    val t = value
                    viewport = Viewport(
                        from.scale + (target.scale - from.scale) * t,
                        from.offsetX + (target.offsetX - from.offsetX) * t,
                        from.offsetY + (target.offsetY - from.offsetY) * t,
                    )
                }
            }
        }
        fun showOverview() {
            focusedId = null
            animateTo(overview)
        }
        fun focus(id: String) {
            val bounds = layout.subtreeBounds(map, id) ?: return
            focusedId = id
            animateTo(Viewport.fit(bounds, width, height, 32f * density, MIN_ZOOM * density, FOCUS_MAX_ZOOM * density))
        }

        BackHandler { if (focusedId != null) showOverview() else onExit() }

        viewport?.let { vp ->
            MapCanvas(
                map = map,
                layout = layout,
                renderer = viewModel.renderer,
                images = viewModel.imageCache,
                viewport = vp,
                onViewportChange = { viewport = it },
                description = stringResource(R.string.mindmap_canvas),
                onTap = { id -> if (id == null || id == map.root.id) showOverview() else focus(id) },
                highlightId = scheduledId,
            )
        }
        if (!schedule.isEmpty) {
            TimerPill(
                elapsedSeconds = elapsedSeconds,
                branchText = scheduledId?.let { map.find(it)?.text },
                overTime = elapsedSeconds >= schedule.totalSeconds,
                onToggle = timer::toggle,
                onReset = timer::reset,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            )
        }
        Row(Modifier.align(Alignment.TopEnd).padding(16.dp)) {
            FilledTonalIconButton(onClick = { showOverview() }) {
                Icon(painterResource(R.drawable.ic_zoom_out_map), contentDescription = stringResource(R.string.action_overview))
            }
            FilledTonalIconButton(onClick = onExit) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_exit_presentation))
            }
        }
    }
}

/**
 * The count-up timer: elapsed time and the text of the scheduled main branch, red without a name once
 * the total is reached. A tap starts or pauses it, a long press resets it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimerPill(
    elapsedSeconds: Long,
    branchText: String?,
    overTime: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier,
) {
    val description = stringResource(R.string.timer_description)
    Surface(
        modifier = modifier
            .widthIn(max = 360.dp)
            .clip(CircleShape)
            .combinedClickable(onClick = onToggle, onLongClick = onReset)
            .semantics { contentDescription = description }
            .testTag("presentation_timer"),
        shape = CircleShape,
        color = if (overTime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (overTime) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSecondaryContainer,
        shadowElevation = 4.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                formatElapsed(elapsedSeconds),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                modifier = Modifier.testTag("timer_elapsed"),
            )
            if (!overTime && branchText != null) {
                Text(
                    branchText.lines().first(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("timer_branch"),
                )
            }
        }
    }
}

/** Hides the system bars and keeps the screen on while composed. */
@Composable
private fun ImmersiveMode() {
    val view = LocalView.current
    val activity = LocalActivity.current ?: return
    DisposableEffect(view) {
        val window = activity.window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

/** Zooming in on a branch never magnifies more than this, so text stays comfortable to read. */
private const val FOCUS_MAX_ZOOM = 2.5f

/** How often the running timer is re-read; often enough to show every second on time. */
private const val TIMER_REFRESH_MILLIS = 200L
