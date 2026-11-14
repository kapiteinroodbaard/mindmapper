package io.github.wlenaerts.mindmapper.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.wlenaerts.mindmapper.R
import io.github.wlenaerts.mindmapper.core.layout.Rect
import io.github.wlenaerts.mindmapper.core.layout.TopicBox
import io.github.wlenaerts.mindmapper.core.model.Side
import io.github.wlenaerts.mindmapper.core.model.TopicImage
import io.github.wlenaerts.mindmapper.core.view.Viewport
import kotlin.math.roundToInt

private val HandleSize = 36.dp
private val Gap = 6.dp
private val CornerHandleSize = 22.dp
private val ImageDeleteSize = 32.dp
private val ImageDeleteInset = 14.dp
private const val SELECTION_INSET = 5f

/**
 * The actions of the selected topic, drawn on top of the map next to that topic: "+" handles for a
 * subtopic (outer side; both sides of the central topic in the radial layout) and a topic after it
 * (below), and an action bar above the topic, or below it when there is no room above.
 * When the topic has an image ([image], in layout units), the image gets a delete button and a round
 * resize handle on each corner, and the action bar offers no image action.
 * Nothing is shown when the topic is completely outside the canvas.
 */
@Composable
fun TopicActions(
    box: TopicBox,
    viewport: Viewport,
    radial: Boolean,
    onAddChild: (Side?) -> Unit,
    onAddSibling: () -> Unit,
    onEditText: () -> Unit,
    onImage: () -> Unit,
    onIcon: () -> Unit,
    onStyle: () -> Unit,
    onDuration: () -> Unit,
    onDelete: () -> Unit,
    image: ImageTools? = null,
) {
    val density = LocalDensity.current
    val gap = with(density) { Gap.toPx() }
    val handle = with(density) { HandleSize.toPx() }
    val isRoot = box.depth == 0
    // Measured from the selection outline, which the renderer draws just outside the topic.
    val r = box.rect.inset(SELECTION_INSET).toScreen(viewport)

    val addChild = stringResource(R.string.action_add_child)
    OnScreen(r) { width, height ->
        if (isRoot) {
            Handle(addChild, Modifier.placed(width, height) { w, h -> IntOffset((r.right + gap).roundToInt(), (r.centerY - h / 2f).roundToInt()) }) {
                onAddChild(if (radial) Side.RIGHT else null)
            }
            if (radial) {
                Handle(addChild, Modifier.placed(width, height) { w, h -> IntOffset((r.left - gap - w).roundToInt(), (r.centerY - h / 2f).roundToInt()) }) {
                    onAddChild(Side.LEFT)
                }
            }
        } else {
            val left = box.side == Side.LEFT
            Handle(addChild, Modifier.placed(width, height) { w, h ->
                val x = if (left) r.left - gap - w else r.right + gap
                IntOffset(x.roundToInt(), (r.centerY - h / 2f).roundToInt())
            }) { onAddChild(null) }
            Handle(stringResource(R.string.action_add_sibling), Modifier.placed(width, height) { w, _ ->
                IntOffset((r.centerX - w / 2f).roundToInt(), (r.bottom + gap).roundToInt())
            }, iconRes = R.drawable.ic_add_sibling, onClick = onAddSibling)
        }
        if (image != null) ImageHandles(image, viewport, width, height)
        // Keep clear of the corner handles of an image at the top of the topic.
        val clearance = if (image != null) with(density) { (CornerHandleSize / 2).toPx() } else 0f
        ActionBar(
            depth = box.depth,
            hasImage = image != null,
            modifier = Modifier.placed(width, height) { w, h ->
                val above = r.top - gap - clearance - h
                // Below the topic (and below the "topic after" handle) when there is no room above.
                val below = r.bottom + gap + if (isRoot) 0f else handle + gap
                IntOffset((r.centerX - w / 2f).roundToInt(), (if (above >= 0) above else below).roundToInt())
            },
            onEditText = onEditText,
            onImage = onImage,
            onIcon = onIcon,
            onStyle = onStyle,
            onDuration = onDuration,
            onDelete = onDelete,
        )
    }
}

/** Runs [content] with the canvas size, only when [r] overlaps the canvas. */
@Composable
private fun OnScreen(r: Rect, content: @Composable (width: Int, height: Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        if (r.right >= 0 && r.left <= width && r.bottom >= 0 && r.top <= height) content(width, height)
    }
}

/**
 * Places the element at the offset [position] computes from its measured size, kept inside the
 * [canvasWidth]×[canvasHeight] canvas. The node spans the canvas so the offset stays inside its bounds;
 * only the element itself handles touches, so the map still receives them everywhere else.
 */
private fun Modifier.placed(canvasWidth: Int, canvasHeight: Int, position: (width: Int, height: Int) -> IntOffset) =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        layout(canvasWidth, canvasHeight) {
            val p = position(placeable.width, placeable.height)
            placeable.place(
                p.x.coerceIn(0, (canvasWidth - placeable.width).coerceAtLeast(0)),
                p.y.coerceIn(0, (canvasHeight - placeable.height).coerceAtLeast(0)),
            )
        }
    }

@Composable
private fun Handle(
    label: String,
    modifier: Modifier,
    iconRes: Int? = null,
    onClick: () -> Unit,
) {
    FilledIconButton(onClick = onClick, modifier = modifier.size(HandleSize)) {
        if (iconRes == null) {
            Icon(Icons.Filled.Add, contentDescription = label, modifier = Modifier.size(20.dp))
        } else {
            Icon(painterResource(iconRes), contentDescription = label, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ActionBar(
    depth: Int,
    hasImage: Boolean,
    modifier: Modifier,
    onEditText: () -> Unit,
    onImage: () -> Unit,
    onIcon: () -> Unit,
    onStyle: () -> Unit,
    onDuration: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp, shadowElevation = 6.dp) {
        Row(
            Modifier.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isRoot = depth == 0
            IconButton(onClick = onEditText) { Icon(Icons.Filled.Edit, stringResource(R.string.action_edit_text)) }
            // An image is removed and resized with its own tools, so only a topic without one offers this.
            if (!hasImage) {
                IconButton(onClick = onImage) { Icon(painterResource(R.drawable.ic_image), stringResource(R.string.action_image)) }
            }
            IconButton(onClick = onIcon) { Icon(painterResource(R.drawable.mm_icon_star), stringResource(R.string.action_icon)) }
            // The central topic and main branches have a style; delete comes last on every topic but the central one.
            if (depth <= 1) {
                val label = stringResource(if (isRoot) R.string.action_style else R.string.action_branch_style)
                IconButton(onClick = onStyle) { Icon(painterResource(R.drawable.ic_palette), label) }
            }
            // Only main branches have a planned duration; the central topic shows their total.
            if (depth == 1) {
                IconButton(onClick = onDuration) {
                    Icon(painterResource(R.drawable.mm_icon_timer), stringResource(R.string.action_duration))
                }
            }
            if (!isRoot) {
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, stringResource(R.string.action_delete)) }
            }
        }
    }
}

/**
 * The image of the selected topic: [rect] in layout units, its current [width] and [aspect] (width / height),
 * and what deleting and resizing do. [onResize] is called with the new width during a drag, [onResizeEnd]
 * when the drag ends.
 */
class ImageTools(
    val rect: Rect,
    val width: Float,
    val aspect: Float,
    val onDelete: () -> Unit,
    val onResize: (Float) -> Unit,
    val onResizeEnd: () -> Unit,
)

/** The delete button and the four corner handles on the selected topic's image. */
@Composable
private fun ImageHandles(image: ImageTools, viewport: Viewport, width: Int, height: Int) {
    val density = LocalDensity.current
    val r = image.rect.toScreen(viewport)
    val inset = with(density) { ImageDeleteInset.toPx() }
    val resizeLabel = stringResource(R.string.image_resize)
    // Corners as outward directions: -1 is left or up, 1 is right or down.
    for ((sx, sy) in listOf(-1 to -1, 1 to -1, -1 to 1, 1 to 1)) {
        CornerHandle(
            label = resizeLabel,
            tag = "image_handle_${if (sy < 0) "top" else "bottom"}_${if (sx < 0) "left" else "right"}",
            image = image,
            scale = viewport.scale,
            sx = sx,
            sy = sy,
            modifier = Modifier.placed(width, height) { w, h ->
                val x = if (sx < 0) r.left else r.right
                val y = if (sy < 0) r.top else r.bottom
                IntOffset((x - w / 2f).roundToInt(), (y - h / 2f).roundToInt())
            },
        )
    }
    FilledIconButton(
        onClick = image.onDelete,
        modifier = Modifier
            .placed(width, height) { w, _ -> IntOffset((r.right - inset - w).roundToInt(), (r.top + inset).roundToInt()) }
            .size(ImageDeleteSize)
            .testTag("image_delete"),
    ) {
        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.image_remove), modifier = Modifier.size(18.dp))
    }
}

/**
 * A round handle on an image corner. Dragging it outward along the corner's diagonal grows the image. The
 * handle moves while the layout reflows, so the drag is measured in root coordinates, not in its own.
 */
@Composable
private fun CornerHandle(label: String, tag: String, image: ImageTools, scale: Float, sx: Int, sy: Int, modifier: Modifier) {
    val current by rememberUpdatedState(image)
    val currentScale by rememberUpdatedState(scale)
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier
            .size(CornerHandleSize)
            .onGloballyPositioned { origin = it.positionInRoot() }
            .pointerInput(sx, sy) {
                var start = Offset.Zero
                var startWidth = 0f
                detectDragGestures(
                    onDragStart = { position -> start = origin + position; startWidth = current.width },
                    onDragEnd = { current.onResizeEnd() },
                    onDragCancel = { current.onResizeEnd() },
                    onDrag = { change, _ ->
                        change.consume()
                        val moved = origin + change.position - start
                        // Both axes count, the vertical one weighted by the aspect ratio, averaged.
                        val delta = (sx * moved.x + sy * moved.y * current.aspect) / 2 / currentScale
                        current.onResize((startWidth + delta).coerceIn(TopicImage.MIN_WIDTH, TopicImage.MAX_WIDTH))
                    },
                )
            }
            .semantics { contentDescription = label }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(CornerHandleSize - 6.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.onPrimary, CircleShape),
        )
    }
}

/** The rect in screen pixels under [viewport]. */
private fun Rect.toScreen(viewport: Viewport): Rect = Rect(
    left * viewport.scale + viewport.offsetX,
    top * viewport.scale + viewport.offsetY,
    right * viewport.scale + viewport.offsetX,
    bottom * viewport.scale + viewport.offsetY,
)
