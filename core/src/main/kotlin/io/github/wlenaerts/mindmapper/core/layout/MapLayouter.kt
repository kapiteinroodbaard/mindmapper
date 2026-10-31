package io.github.wlenaerts.mindmapper.core.layout

import io.github.wlenaerts.mindmapper.core.model.LayoutKind
import io.github.wlenaerts.mindmapper.core.model.MindMap
import io.github.wlenaerts.mindmapper.core.model.Side
import io.github.wlenaerts.mindmapper.core.model.Topic
import io.github.wlenaerts.mindmapper.core.model.subtreeIds
import kotlin.math.abs

/**
 * The placed box of one topic, in layout units. [side] is null for the central topic, [colorSlot] is the
 * palette slot of the main branch the topic belongs to (-1 for the central topic).
 */
data class TopicBox(
    val topicId: String,
    val parentId: String?,
    val rect: Rect,
    val depth: Int,
    val side: Side?,
    val colorSlot: Int,
)

/** The result of laying out a map: a box per topic (in depth-first order) and the overall bounds. */
class MapLayout(val boxes: List<TopicBox>) {
    private val byId = boxes.associateBy { it.topicId }

    val bounds: Rect = boxes.map { it.rect }.reduce(Rect::union)

    operator fun get(topicId: String): TopicBox? = byId[topicId]

    /** The topmost box containing the point, ignoring topics in [exclude]. Deeper topics win. */
    fun hit(x: Float, y: Float, exclude: Set<String> = emptySet()): TopicBox? =
        boxes.filter { it.topicId !in exclude && it.rect.contains(x, y) }.maxByOrNull { it.depth }

    /** Bounds of topic [topicId] and everything below it. */
    fun subtreeBounds(map: MindMap, topicId: String): Rect? {
        val topic = map.find(topicId) ?: return null
        return subtreeIds(topic)
            .mapNotNull { byId[it]?.rect }
            .reduceOrNull(Rect::union)
    }
}

/** Places topics for both layout kinds. Measurement is injected so this stays free of Android. */
object MapLayouter {
    /** Horizontal distance from a parent to its children, by the children's depth. */
    fun horizontalGap(depth: Int): Float = when (depth) {
        1 -> 64f
        2 -> 40f
        else -> 32f
    }

    /** Vertical distance between neighbouring sibling subtrees, by the siblings' depth. */
    fun verticalGap(depth: Int): Float = if (depth == 1) 28f else 12f

    fun layout(map: MindMap, measure: (topic: Topic, depth: Int) -> Size): MapLayout = LayoutRun(map, measure).run()

    private class LayoutRun(private val map: MindMap, private val measure: (Topic, Int) -> Size) {
        private val sizes = HashMap<String, Size>()
        private val heights = HashMap<String, Float>()
        private val boxes = ArrayList<TopicBox>()

        fun run(): MapLayout {
            measureAll(map.root, 0)
            val mains = map.root.children
            val sides = when (map.layout) {
                LayoutKind.RIGHT -> mains.map { Side.RIGHT }
                LayoutKind.RADIAL -> balanceSides(mains.map { it.side }, mains.map { subtreeHeight(it, 1) })
            }
            val right = mains.indices.filter { sides[it] == Side.RIGHT }
            val left = mains.indices.filter { sides[it] == Side.LEFT }
            // Clockwise from the top right: down the right side, then up the left side.
            val clockwise = right + left
            val slots = colorSlots(clockwise.size, cyclic = map.layout == LayoutKind.RADIAL)
            val slotOf = clockwise.withIndex().associate { (position, index) -> index to slots[position] }

            val rootSize = sizes.getValue(map.root.id)
            val rootRect = Rect(-rootSize.width / 2, -rootSize.height / 2, rootSize.width / 2, rootSize.height / 2)
            boxes += TopicBox(map.root.id, null, rootRect, 0, null, -1)

            // The left side is filled bottom to top (clockwise), so on screen it reads in reverse order.
            stack(right, 0f) { index, center ->
                place(mains[index], map.root.id, 1, Side.RIGHT, rootRect.right + horizontalGap(1), center, slotOf.getValue(index))
            }
            stack(left.reversed(), 0f) { index, center ->
                place(mains[index], map.root.id, 1, Side.LEFT, rootRect.left - horizontalGap(1), center, slotOf.getValue(index))
            }

            // Keep depth-first order (matching the model) so parents are drawn before their children.
            val order = map.topics().map { it.id }.withIndex().associate { (i, id) -> id to i }
            boxes.sortBy { order.getValue(it.topicId) }
            return MapLayout(boxes)
        }

        private fun measureAll(topic: Topic, depth: Int) {
            sizes[topic.id] = measure(topic, depth)
            topic.children.forEach { measureAll(it, depth + 1) }
        }

        private fun subtreeHeight(topic: Topic, depth: Int): Float = heights.getOrPut(topic.id) {
            val own = sizes.getValue(topic.id).height
            if (topic.children.isEmpty()) {
                own
            } else {
                val children = topic.children.sumOf { subtreeHeight(it, depth + 1).toDouble() }.toFloat() +
                    verticalGap(depth + 1) * (topic.children.size - 1)
                maxOf(own, children)
            }
        }

        private fun place(topic: Topic, parentId: String, depth: Int, side: Side, anchorX: Float, centerY: Float, slot: Int) {
            val size = sizes.getValue(topic.id)
            val x = if (side == Side.RIGHT) anchorX else anchorX - size.width
            val rect = Rect(x, centerY - size.height / 2, x + size.width, centerY + size.height / 2)
            boxes += TopicBox(topic.id, parentId, rect, depth, side, slot)
            val childAnchor = if (side == Side.RIGHT) rect.right + horizontalGap(depth + 1) else rect.left - horizontalGap(depth + 1)
            stack(topic.children.indices.toList(), centerY, depth + 1, topic.children) { index, center ->
                place(topic.children[index], topic.id, depth + 1, side, childAnchor, center, slot)
            }
        }

        /** Stacks the main branches at [indices] top to bottom, centred on [centerY]. */
        private fun stack(indices: List<Int>, centerY: Float, placeChild: (Int, Float) -> Unit) =
            stack(indices, centerY, 1, map.root.children, placeChild)

        /** Stacks the subtrees of [topics] at [indices] top to bottom, centred on [centerY], with even gaps. */
        private fun stack(indices: List<Int>, centerY: Float, depth: Int, topics: List<Topic>, placeChild: (Int, Float) -> Unit) {
            if (indices.isEmpty()) return
            val h = indices.map { subtreeHeight(topics[it], depth) }
            val total = h.sum() + verticalGap(depth) * (indices.size - 1)
            var y = centerY - total / 2
            for ((i, index) in indices.withIndex()) {
                placeChild(index, y + h[i] / 2)
                y += h[i] + verticalGap(depth)
            }
        }
    }

    /**
     * Assigns sides to main branches: fixed ones keep their side, the others are split at one point so
     * that the first ones go right and the rest left, minimising the height difference.
     */
    fun balanceSides(fixed: List<Side?>, heights: List<Float>): List<Side> {
        val n = fixed.size
        var bestK = 0
        var bestDiff = Float.MAX_VALUE
        for (k in 0..n) {
            var right = 0f
            var left = 0f
            for (i in 0 until n) {
                val side = fixed[i] ?: if (i < k) Side.RIGHT else Side.LEFT
                if (side == Side.RIGHT) right += heights[i] else left += heights[i]
            }
            val diff = abs(right - left)
            // On a tie prefer more branches on the right, where reading starts.
            if (diff <= bestDiff) {
                bestDiff = diff
                bestK = k
            }
        }
        return List(n) { i -> fixed[i] ?: if (i < bestK) Side.RIGHT else Side.LEFT }
    }

    /**
     * Palette slots for [count] main branches in clockwise order. Consecutive slots always differ; when
     * [cyclic], the last branch also differs from the first.
     */
    fun colorSlots(count: Int, cyclic: Boolean, paletteSize: Int = PALETTE_SIZE): List<Int> {
        val slots = MutableList(count) { it % paletteSize }
        if (cyclic && count > 2 && slots.last() == slots.first()) {
            val previous = slots[count - 2]
            slots[count - 1] = (0 until paletteSize).first { it != previous && it != slots.first() }
        }
        return slots
    }

    const val PALETTE_SIZE = 10
}
