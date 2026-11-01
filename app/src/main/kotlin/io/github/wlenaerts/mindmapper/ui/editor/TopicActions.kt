package io.github.wlenaerts.mindmapper.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.wlenaerts.mindmapper.R
import io.github.wlenaerts.mindmapper.core.layout.Rect
import io.github.wlenaerts.mindmapper.core.layout.TopicBox
import io.github.wlenaerts.mindmapper.core.model.Side
import io.github.wlenaerts.mindmapper.core.view.Viewport
import kotlin.math.roundToInt

private val HandleSize = 36.dp
private val Gap = 6.dp
private const val SELECTION_INSET = 5f

/**
 * The actions of the selected topic, drawn on top of the map next to that topic: "+" handles for a
 * subtopic (outer side; both sides of the central topic in the radial layout) and a topic after it
 * (below), and an action bar above the topic, or below it when there is no room above.
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
    onDelete: () -> Unit,
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
        ActionBar(
            isRoot = isRoot,
            modifier = Modifier.placed(width, height) { w, h ->
                val above = r.top - gap - h
                // Below the topic (and below the "topic after" handle) when there is no room above.
                val below = r.bottom + gap + if (isRoot) 0f else handle + gap
                IntOffset((r.centerX - w / 2f).roundToInt(), (if (above >= 0) above else below).roundToInt())
            },
            onEditText = onEditText,
            onImage = onImage,
            onIcon = onIcon,
            onStyle = onStyle,
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
    isRoot: Boolean,
    modifier: Modifier,
    onEditText: () -> Unit,
    onImage: () -> Unit,
    onIcon: () -> Unit,
    onStyle: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp, shadowElevation = 6.dp) {
        Row(
            Modifier.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onEditText) { Icon(Icons.Filled.Edit, stringResource(R.string.action_edit_text)) }
            IconButton(onClick = onImage) { Icon(painterResource(R.drawable.ic_image), stringResource(R.string.action_image)) }
            IconButton(onClick = onIcon) { Icon(painterResource(R.drawable.mm_icon_star), stringResource(R.string.action_icon)) }
            if (isRoot) {
                IconButton(onClick = onStyle) { Icon(painterResource(R.drawable.ic_palette), stringResource(R.string.action_style)) }
            } else {
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, stringResource(R.string.action_delete)) }
            }
        }
    }
}

/** The rect in screen pixels under [viewport]. */
private fun Rect.toScreen(viewport: Viewport): Rect = Rect(
    left * viewport.scale + viewport.offsetX,
    top * viewport.scale + viewport.offsetY,
    right * viewport.scale + viewport.offsetX,
    bottom * viewport.scale + viewport.offsetY,
)
