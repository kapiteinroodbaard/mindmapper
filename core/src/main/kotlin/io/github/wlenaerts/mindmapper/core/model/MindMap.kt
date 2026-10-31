package io.github.wlenaerts.mindmapper.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

@Serializable
enum class LayoutKind {
    /** Main branches on both sides of the central topic, clockwise from the top right. */
    @SerialName("radial") RADIAL,

    /** All main branches stacked to the right of the central topic. */
    @SerialName("right") RIGHT,
}

@Serializable
enum class CentralShape {
    @SerialName("circle") CIRCLE,
    @SerialName("ellipse") ELLIPSE,
    @SerialName("rectangle") RECTANGLE,
    @SerialName("rounded_rectangle") ROUNDED_RECTANGLE,
    @SerialName("diamond") DIAMOND,
    @SerialName("hexagon") HEXAGON,
    @SerialName("capsule") CAPSULE,
}

@Serializable
enum class Side {
    @SerialName("left") LEFT,
    @SerialName("right") RIGHT,
}

/**
 * An image embedded in the document. [path] is the entry name inside the container (`images/<id>.webp`),
 * [width] the display width in layout units and [aspect] the image's width divided by its height.
 */
@Serializable
data class TopicImage(val path: String, val width: Float, val aspect: Float) {
    val height: Float get() = width / aspect

    companion object {
        const val MIN_WIDTH = 48f
        const val MAX_WIDTH = 480f
        const val DEFAULT_WIDTH = 160f
    }
}

/** A topic in the tree. [side] is only meaningful for main branches (children of the central topic). */
@Serializable
data class Topic(
    val id: String = newId(),
    val text: String = "",
    val icon: String? = null,
    val image: TopicImage? = null,
    val side: Side? = null,
    val children: List<Topic> = emptyList(),
)

/** A whole mind map. Immutable: every edit produces a new instance. */
@Serializable
data class MindMap(
    val layout: LayoutKind = LayoutKind.RADIAL,
    val centralShape: CentralShape = CentralShape.ROUNDED_RECTANGLE,
    @Serializable(with = HexColorSerializer::class)
    val centralColor: Int = DEFAULT_CENTRAL_COLOR,
    val root: Topic,
) {
    /** Every topic in depth-first order, starting with the central topic. */
    fun topics(): Sequence<Topic> = sequence {
        suspend fun SequenceScope<Topic>.visit(topic: Topic) {
            yield(topic)
            topic.children.forEach { visit(it) }
        }
        visit(root)
    }

    fun find(id: String): Topic? = topics().firstOrNull { it.id == id }

    /** The parent of topic [id], or null for the central topic or an unknown id. */
    fun parentOf(id: String): Topic? = topics().firstOrNull { parent -> parent.children.any { it.id == id } }

    /** Number of steps from the central topic (0) to topic [id], or -1 when unknown. */
    fun depthOf(id: String): Int {
        fun search(topic: Topic, depth: Int): Int {
            if (topic.id == id) return depth
            for (child in topic.children) {
                val found = search(child, depth + 1)
                if (found >= 0) return found
            }
            return -1
        }
        return search(root, 0)
    }

    /** Whether [candidate] is [ancestor] itself or lies in its subtree. */
    fun isInSubtree(ancestor: String, candidate: String): Boolean =
        find(ancestor)?.let { start -> subtreeIds(start).contains(candidate) } ?: false

    fun imagePaths(): Set<String> = topics().mapNotNull { it.image?.path }.toSet()

    companion object {
        const val DEFAULT_CENTRAL_COLOR: Int = 0xFF3D5A80.toInt()
    }
}

fun subtreeIds(topic: Topic): Set<String> = buildSet {
    fun visit(t: Topic) {
        add(t.id)
        t.children.forEach { visit(it) }
    }
    visit(topic)
}

fun newId(): String = UUID.randomUUID().toString()

/** A new map with only a central topic. */
fun newMindMap(centralText: String): MindMap = MindMap(root = Topic(text = centralText))

/** Serializes an ARGB colour as `#RRGGBB` (alpha is always opaque). */
object HexColorSerializer : KSerializer<Int> {
    override val descriptor = PrimitiveSerialDescriptor("HexColor", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeString("#%06X".format(value and 0xFFFFFF))
    }

    override fun deserialize(decoder: Decoder): Int {
        val text = decoder.decodeString().removePrefix("#")
        require(text.length == 6) { "Colour '$text' must be #RRGGBB" }
        return (0xFF000000.toInt()) or text.toInt(16)
    }
}
