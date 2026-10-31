package io.github.wlenaerts.mindmapper.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.wlenaerts.mindmapper.core.edit.DropTarget
import io.github.wlenaerts.mindmapper.core.edit.resolveDrop
import io.github.wlenaerts.mindmapper.core.layout.MapLayout
import io.github.wlenaerts.mindmapper.core.model.MindMap
import io.github.wlenaerts.mindmapper.core.view.Viewport
import io.github.wlenaerts.mindmapper.render.ImageSource
import io.github.wlenaerts.mindmapper.render.MapRenderer
import io.github.wlenaerts.mindmapper.render.MapTheme

/** Zoom limits relative to 1 layout unit = 1 dp. */
const val MIN_ZOOM = 0.2f
const val MAX_ZOOM = 4f

/**
 * Draws [map] with pan and pinch zoom. When [onDrop] is given, long-press-and-drag on a topic moves it.
 * [viewport] is in pixels; the caller owns it so it can fit or animate it.
 */
@Composable
fun MapCanvas(
    map: MindMap,
    layout: MapLayout,
    renderer: MapRenderer,
    images: ImageSource,
    viewport: Viewport,
    onViewportChange: (Viewport) -> Unit,
    modifier: Modifier = Modifier,
    selectedId: String? = null,
    description: String = "",
    onTap: (topicId: String?) -> Unit = {},
    onDoubleTap: (topicId: String) -> Unit = {},
    onDrop: ((draggedId: String, target: DropTarget) -> Unit)? = null,
) {
    val theme = if (isSystemInDarkTheme()) MapTheme.Dark else MapTheme.Light
    val density = LocalDensity.current.density
    val currentViewport by rememberUpdatedState(viewport)
    val currentMap by rememberUpdatedState(map)
    val currentLayout by rememberUpdatedState(layout)
    val currentOnViewportChange by rememberUpdatedState(onViewportChange)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentOnDrop by rememberUpdatedState(onDrop)

    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragStart by remember { mutableStateOf(Offset.Zero) }
    var dragNow by remember { mutableStateOf(Offset.Zero) }
    var dropTarget by remember { mutableStateOf<DropTarget?>(null) }

    fun hit(position: Offset): String? =
        currentLayout.hit(currentViewport.toLayoutX(position.x), currentViewport.toLayoutY(position.y))?.topicId

    var gestures = modifier
        .fillMaxSize()
        .testTag("map_canvas")
        .semantics { contentDescription = description }
        .pointerInput(Unit) {
            detectTransformGestures { centroid, pan, zoom, _ ->
                currentOnViewportChange(
                    currentViewport.pan(pan.x, pan.y).zoom(zoom, centroid.x, centroid.y, MIN_ZOOM * density, MAX_ZOOM * density),
                )
            }
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { currentOnTap(hit(it)) },
                onDoubleTap = { position -> hit(position)?.let { currentOnDoubleTap(it) } },
            )
        }
    if (onDrop != null) {
        gestures = gestures.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { position ->
                    val id = hit(position)
                    if (id != null && id != currentMap.root.id) {
                        draggedId = id
                        dragStart = position
                        dragNow = position
                    }
                },
                onDrag = { change, amount ->
                    val id = draggedId ?: return@detectDragGesturesAfterLongPress
                    change.consume()
                    dragNow += amount
                    dropTarget = resolveDrop(
                        currentMap, currentLayout, id,
                        currentViewport.toLayoutX(dragNow.x), currentViewport.toLayoutY(dragNow.y),
                    )
                },
                onDragEnd = {
                    val id = draggedId
                    val target = dropTarget
                    if (id != null && target != null) currentOnDrop?.invoke(id, target)
                    draggedId = null
                    dropTarget = null
                },
                onDragCancel = {
                    draggedId = null
                    dropTarget = null
                },
            )
        }
    }

    Canvas(modifier = gestures) {
        drawRect(Color(theme.background))
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            native.save()
            native.translate(viewport.offsetX, viewport.offsetY)
            native.scale(viewport.scale, viewport.scale)
            renderer.draw(native, map, layout, theme, images, selectedId = selectedId, dropTarget = dropTarget)
            draggedId?.let { id ->
                val dx = (dragNow.x - dragStart.x) / viewport.scale
                val dy = (dragNow.y - dragStart.y) / viewport.scale
                native.saveLayerAlpha(null, 150)
                renderer.drawTopicOnly(native, map, layout, id, theme, images, dx, dy)
                native.restore()
            }
            native.restore()
        }
    }
}

/** The viewport that shows the whole map in a canvas of [width]×[height] pixels. */
fun fitViewport(layout: MapLayout, width: Float, height: Float, density: Float): Viewport =
    Viewport.fit(layout.bounds, width, height, 24f * density, MIN_ZOOM * density, 1f * density)
