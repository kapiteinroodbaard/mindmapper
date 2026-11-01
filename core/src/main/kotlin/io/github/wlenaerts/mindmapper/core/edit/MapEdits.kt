package io.github.wlenaerts.mindmapper.core.edit

import io.github.wlenaerts.mindmapper.core.model.CentralShape
import io.github.wlenaerts.mindmapper.core.model.LayoutKind
import io.github.wlenaerts.mindmapper.core.model.MindMap
import io.github.wlenaerts.mindmapper.core.model.Side
import io.github.wlenaerts.mindmapper.core.model.Topic
import io.github.wlenaerts.mindmapper.core.model.TopicImage

/** Edit operations. Each returns a new map; an operation that does not apply returns the map unchanged. */

fun MindMap.updateTopic(id: String, transform: (Topic) -> Topic): MindMap {
    fun visit(topic: Topic): Topic =
        if (topic.id == id) transform(topic) else topic.copy(children = topic.children.map(::visit))
    return copy(root = visit(root))
}

fun MindMap.setText(id: String, text: String): MindMap = updateTopic(id) { it.copy(text = text) }

fun MindMap.setIcon(id: String, icon: String?): MindMap = updateTopic(id) { it.copy(icon = icon) }

fun MindMap.setImage(id: String, image: TopicImage?): MindMap = updateTopic(id) { it.copy(image = image) }

fun MindMap.setImageWidth(id: String, width: Float): MindMap = updateTopic(id) { topic ->
    topic.image?.let { topic.copy(image = it.copy(width = width.coerceIn(TopicImage.MIN_WIDTH, TopicImage.MAX_WIDTH))) } ?: topic
}

/**
 * Switches the layout. Sides chosen for main branches are cleared on a switch, so switching to the
 * layout around the centre always starts from an evenly balanced distribution.
 */
fun MindMap.setLayout(layout: LayoutKind): MindMap {
    if (layout == this.layout) return this
    return copy(layout = layout, root = root.copy(children = root.children.map { it.copy(side = null) }))
}

/** Sets bold, italic and underline of topic [id]; false for italic and underline is stored as null. */
fun MindMap.setTextFormat(id: String, bold: Boolean?, italic: Boolean, underline: Boolean): MindMap = updateTopic(id) {
    it.copy(bold = bold, italic = italic.takeIf { on -> on }, underline = underline.takeIf { on -> on })
}

fun MindMap.setTrunk(width: Float, taper: Float): MindMap = copy(
    trunkWidth = width.coerceIn(MindMap.MIN_TRUNK_WIDTH, MindMap.MAX_TRUNK_WIDTH),
    trunkTaper = taper.coerceIn(MindMap.MIN_TRUNK_TAPER, MindMap.MAX_TRUNK_TAPER),
)

fun MindMap.setCentralShape(shape: CentralShape): MindMap = copy(centralShape = shape)

fun MindMap.setCentralColor(color: Int): MindMap = copy(centralColor = color)

/** Appends [child] as the last child of [parentId]. Under the central topic, [side] places the new main branch. */
fun MindMap.addChild(parentId: String, child: Topic, side: Side? = null): MindMap {
    val placed = if (parentId == root.id) child.copy(side = side) else child
    return updateTopic(parentId) { it.copy(children = it.children + placed) }
}

/** Inserts [sibling] directly after topic [id]; not possible for the central topic. */
fun MindMap.addSiblingAfter(id: String, sibling: Topic): MindMap {
    val parent = parentOf(id) ?: return this
    val index = parent.children.indexOfFirst { it.id == id }
    val placed = if (parent.id == root.id) sibling.copy(side = parent.children[index].side) else sibling
    return insert(parent.id, index + 1, placed)
}

/** Removes topic [id] with its subtree; the central topic cannot be deleted. */
fun MindMap.delete(id: String): MindMap {
    if (id == root.id) return this
    fun visit(topic: Topic): Topic = topic.copy(children = topic.children.filter { it.id != id }.map(::visit))
    return copy(root = visit(root))
}

private fun MindMap.insert(parentId: String, index: Int, topic: Topic): MindMap = updateTopic(parentId) { parent ->
    val children = parent.children.toMutableList()
    children.add(index.coerceIn(0, children.size), topic)
    parent.copy(children = children)
}

/**
 * Moves topic [id] with its subtree to position [index] among the children of [parentId] (index as seen
 * before the move). Under the central topic the branch gets [side] (null keeps its current side); elsewhere
 * the side is cleared. Moving the central topic, or into the topic's own subtree, is rejected.
 */
fun MindMap.move(id: String, parentId: String, index: Int, side: Side? = null): MindMap {
    if (id == root.id || isInSubtree(id, parentId)) return this
    val topic = find(id) ?: return this
    val oldParent = parentOf(id) ?: return this
    if (find(parentId) == null) return this
    val oldIndex = oldParent.children.indexOfFirst { it.id == id }
    val target = if (oldParent.id == parentId && oldIndex < index) index - 1 else index
    val moved = if (parentId == root.id) topic.copy(side = side ?: topic.side) else topic.copy(side = null)
    val removed = copy(root = removeFrom(root, id))
    return removed.insert(parentId, target, moved)
}

private fun removeFrom(topic: Topic, id: String): Topic =
    topic.copy(children = topic.children.filter { it.id != id }.map { removeFrom(it, id) })
