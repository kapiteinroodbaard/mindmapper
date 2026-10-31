package io.github.wlenaerts.mindmapper.core.edit

import io.github.wlenaerts.mindmapper.core.layout.MapLayout
import io.github.wlenaerts.mindmapper.core.model.LayoutKind
import io.github.wlenaerts.mindmapper.core.model.MindMap
import io.github.wlenaerts.mindmapper.core.model.Side
import io.github.wlenaerts.mindmapper.core.model.subtreeIds

/** Where a dragged topic would land: as child [index] of [parentId], on [side] when it becomes a main branch. */
data class DropTarget(
    val parentId: String,
    val index: Int,
    val side: Side?,
    val kind: Kind,
    /** The topic the indicator is drawn on. */
    val anchorId: String,
) {
    enum class Kind { CHILD, BEFORE, AFTER }

    fun apply(map: MindMap, draggedId: String): MindMap = map.move(draggedId, parentId, index, side)
}

/** Resolves the drop of [draggedId] at layout point ([x], [y]); null when there is nothing valid there. */
fun resolveDrop(map: MindMap, layout: MapLayout, draggedId: String, x: Float, y: Float): DropTarget? {
    if (draggedId == map.root.id) return null
    val dragged = map.find(draggedId) ?: return null
    val box = layout.hit(x, y, exclude = subtreeIds(dragged)) ?: return null
    val radial = map.layout == LayoutKind.RADIAL

    if (box.topicId == map.root.id) {
        val side = if (radial) (if (x < box.rect.centerX) Side.LEFT else Side.RIGHT) else null
        return DropTarget(map.root.id, map.root.children.size, side, DropTarget.Kind.CHILD, map.root.id)
    }

    val relative = (y - box.rect.top) / box.rect.height
    val parentId = box.parentId ?: return null
    val parent = map.find(parentId) ?: return null
    val siblingIndex = parent.children.indexOfFirst { it.id == box.topicId }
    // Next to a main branch in the radial layout, the dropped branch joins that branch's side.
    val side = if (radial && parentId == map.root.id) box.side else null
    return when {
        relative < 0.25f -> DropTarget(parentId, siblingIndex, side, DropTarget.Kind.BEFORE, box.topicId)
        relative > 0.75f -> DropTarget(parentId, siblingIndex + 1, side, DropTarget.Kind.AFTER, box.topicId)
        else -> {
            val target = map.find(box.topicId) ?: return null
            DropTarget(target.id, target.children.size, null, DropTarget.Kind.CHILD, box.topicId)
        }
    }
}
