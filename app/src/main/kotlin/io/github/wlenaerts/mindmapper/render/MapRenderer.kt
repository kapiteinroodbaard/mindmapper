package io.github.wlenaerts.mindmapper.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import io.github.wlenaerts.mindmapper.core.edit.DropTarget
import io.github.wlenaerts.mindmapper.core.layout.MapLayout
import io.github.wlenaerts.mindmapper.core.layout.MapLayouter
import io.github.wlenaerts.mindmapper.core.layout.Rect
import io.github.wlenaerts.mindmapper.core.layout.TopicBox
import io.github.wlenaerts.mindmapper.core.model.BranchStyle
import io.github.wlenaerts.mindmapper.core.model.CentralShape
import io.github.wlenaerts.mindmapper.core.model.MindMap
import io.github.wlenaerts.mindmapper.core.model.Side
import io.github.wlenaerts.mindmapper.core.model.Topic
import kotlin.math.hypot

/** Supplies decoded images for topic image paths; null while unavailable. */
fun interface ImageSource {
    fun bitmap(path: String): Bitmap?
}

/**
 * Draws a laid-out map onto any [Canvas] in layout units (the caller applies scale and translation).
 * Used for the screen, PDF and PNG output so all three look the same.
 */
class MapRenderer(private val context: Context, val metrics: TopicMetrics = TopicMetrics()) {

    fun layout(map: MindMap): MapLayout = MapLayouter.layout(map) { topic, depth -> metrics.measure(map, topic, depth) }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val icons = HashMap<String, Drawable?>()

    fun draw(
        canvas: Canvas,
        map: MindMap,
        layout: MapLayout,
        theme: MapTheme,
        images: ImageSource,
        selectedId: String? = null,
        dropTarget: DropTarget? = null,
        hiddenIds: Set<String> = emptySet(),
    ) {
        val topics = map.topics().associateBy { it.id }
        // Branches first, so topics are drawn on top of them.
        for (box in layout.boxes) {
            if (box.depth == 0 || box.topicId in hiddenIds) continue
            val parent = layout[box.parentId ?: continue] ?: continue
            drawBranch(canvas, map, parent, box, theme)
        }
        for (box in layout.boxes) {
            if (box.topicId in hiddenIds) continue
            val topic = topics[box.topicId] ?: continue
            drawTopic(canvas, map, topic, box, theme, images)
        }
        selectedId?.let { id -> layout[id]?.let { drawSelection(canvas, it.rect, theme.selection) } }
        dropTarget?.let { target -> layout[target.anchorId]?.let { drawDropIndicator(canvas, it, target, theme.dropIndicator) } }
    }

    /** Draws only topic [topicId], moved by ([dx], [dy]); used for the drag ghost. */
    fun drawTopicOnly(canvas: Canvas, map: MindMap, layout: MapLayout, topicId: String, theme: MapTheme, images: ImageSource, dx: Float, dy: Float) {
        val box = layout[topicId] ?: return
        val topic = map.find(topicId) ?: return
        canvas.save()
        canvas.translate(dx, dy)
        if (box.depth >= 2) drawUnderline(canvas, box, theme.branchColor(box.colorSlot), branchWidths(map, box.depth).second, theme)
        drawTopic(canvas, map, topic, box, theme, images)
        canvas.restore()
    }

    /**
     * Draws a preview of [style] filling [width] by [height]: a main branch growing into its box, in [color].
     * Used for the tiles of the branch style picker.
     */
    fun drawStyleSample(canvas: Canvas, style: BranchStyle, color: Int, theme: MapTheme, width: Float, height: Float) {
        val boxW = width * 0.4f
        val boxH = height * 0.34f
        val box = Rect(width - boxW - width * 0.06f, (height - boxH) / 2, width - width * 0.06f, (height + boxH) / 2)
        val startY = height * 0.82f
        val endX = box.left + BranchGeometry.mainBoxInset(style, box)
        drawLine(
            canvas, style, x0 = width * 0.04f, y0 = startY, x1 = endX, y1 = box.centerY, w0 = height * 0.11f, w1 = height * 0.05f,
            color = color, theme = theme, seed = SAMPLE_SEED, start = null, end = box, decorate = true,
        )
        drawMainBox(canvas, style, box, color, theme)
    }

    /** Start and end width of the branch leading to a topic at [depth]; main branches follow the map's trunk width. */
    private fun branchWidths(map: MindMap, depth: Int): Pair<Float, Float> = when (depth) {
        1 -> map.trunkWidth to 5f
        2 -> 4.5f to 2.5f
        else -> 2.5f to 1.5f
    }

    private fun drawBranch(canvas: Canvas, map: MindMap, parent: TopicBox, child: TopicBox, theme: MapTheme) {
        val right = child.side != Side.LEFT
        val (startX, startY) = when {
            parent.depth == 0 -> parent.rect.centerX to parent.rect.centerY
            parent.depth == 1 -> (if (right) parent.rect.right else parent.rect.left) to parent.rect.centerY
            else -> (if (right) parent.rect.right else parent.rect.left) to parent.rect.bottom
        }
        // A main branch ends where the box outline crosses the box's vertical centre.
        val inset = if (child.depth == 1) BranchGeometry.mainBoxInset(child.style, child.rect) else 0f
        val endX = if (right) child.rect.left + inset else child.rect.right - inset
        val endY = if (child.depth == 1) child.rect.centerY else child.rect.bottom
        val (w0, w1) = branchWidths(map, child.depth)
        val color = theme.branchColor(child.colorSlot)
        // A main branch starts inside the central topic: it keeps its full width up to the topic's edge and
        // only tapers from there, at the map's taper rate.
        val main = child.depth == 1
        drawLine(
            canvas, child.style, startX, startY, endX, endY, w0, w1, color, theme, child.topicId.hashCode(),
            start = parent.rect, end = child.rect, decorate = main, taper = if (main) map.trunkTaper else 1f, inside = if (main) parent.rect else null,
        )
        // Subtopics have no box: an underline in the branch colour carries the topic.
        if (child.depth >= 2) drawUnderline(canvas, child, color, w1, theme)
    }

    /**
     * Draws one tapered branch line in [style]. [start] and [end] are the boxes it connects, kept clear of
     * decorations; [decorate] adds the plant style's leaves and tendrils.
     */
    private fun drawLine(
        canvas: Canvas, style: BranchStyle, x0: Float, y0: Float, x1: Float, y1: Float, w0: Float, w1: Float,
        color: Int, theme: MapTheme, seed: Int, start: Rect?, end: Rect?, decorate: Boolean,
        taper: Float = 1f, inside: Rect? = null,
    ) {
        val line = when (style) {
            BranchStyle.STANDARD, BranchStyle.BLURRED, BranchStyle.PLANT -> BranchGeometry.curve(x0, y0, x1, y1)
            BranchStyle.STRAIGHT -> BranchGeometry.straight(x0, y0, x1, y1)
            BranchStyle.LIGHTNING -> {
                val amplitude = (hypot(x1 - x0, y1 - y0) * 0.06f).coerceIn(3f, 10f) + w0 * 0.25f
                BranchGeometry.zigzag(x0, y0, x1, y1, seed, amplitude, calm = inside)
            }
        }
        val halves = BranchGeometry.halfWidths(line, w0, w1, taper, inside)
        if (style == BranchStyle.BLURRED) {
            val spread = 1f + w0 / 16f
            drawGlow(canvas, color, theme) { ring -> canvas.drawPath(BranchGeometry.outline(line, halves, ring * spread), fill) }
        }
        fill.color = color
        canvas.drawPath(BranchGeometry.outline(line, halves), fill)
        if (style == BranchStyle.PLANT && decorate) drawPlant(canvas, line, halves, color, seed, start, end)
    }

    /** Draws the soft rings of the blurred style, widest and faintest first; [ring] gets the ring's spread. */
    private inline fun drawGlow(canvas: Canvas, color: Int, theme: MapTheme, ring: (Float) -> Unit) {
        val count = theme.glowAlphas.size
        theme.glowAlphas.forEachIndexed { i, alpha ->
            fill.color = withAlpha(color, alpha)
            stroke.color = fill.color
            ring(GLOW_SPREAD * (count - i) / count)
        }
    }

    private fun drawPlant(canvas: Canvas, line: Centreline, halves: FloatArray, color: Int, seed: Int, start: Rect?, end: Rect?) {
        fill.color = color
        stroke.color = color
        for (part in BranchGeometry.plantParts(line, halves, seed, start, end)) {
            if (part.leaf) {
                canvas.drawPath(BranchGeometry.leaf(part.x, part.y, part.angle, part.length), fill)
            } else {
                stroke.strokeWidth = part.thickness
                canvas.drawPath(BranchGeometry.tendril(part.x, part.y, part.angle, part.length, part.turn), stroke)
            }
        }
    }

    /** The line under a subtopic, in its branch's style. */
    private fun drawUnderline(canvas: Canvas, box: TopicBox, color: Int, width: Float, theme: MapTheme) {
        val r = box.rect
        stroke.strokeWidth = width
        when (box.style) {
            BranchStyle.STANDARD, BranchStyle.STRAIGHT -> {
                stroke.color = color
                canvas.drawLine(r.left, r.bottom, r.right, r.bottom, stroke)
            }
            BranchStyle.BLURRED -> {
                drawGlow(canvas, color, theme) { ring ->
                    stroke.strokeWidth = width + 2 * ring
                    canvas.drawLine(r.left, r.bottom, r.right, r.bottom, stroke)
                }
                stroke.color = color
                stroke.strokeWidth = width
                canvas.drawLine(r.left, r.bottom, r.right, r.bottom, stroke)
            }
            BranchStyle.PLANT -> {
                stroke.color = color
                canvas.drawLine(r.left, r.bottom, r.right, r.bottom, stroke)
                // A small leaf at the outer end, pointing away from the branch and slightly up.
                fill.color = color
                val right = box.side != Side.LEFT
                val angle = if (right) -0.55f else (Math.PI.toFloat() + 0.55f)
                canvas.drawPath(BranchGeometry.leaf(if (right) r.right else r.left, r.bottom, angle, 13f), fill)
            }
            BranchStyle.LIGHTNING -> {
                stroke.color = color
                canvas.drawPath(BranchGeometry.zigzagUnderline(r.left, r.right, r.bottom, 2.2f + width / 2), stroke)
            }
        }
    }

    /** The box of a main topic in [style], with the blurred style's glow around it. */
    private fun drawMainBox(canvas: Canvas, style: BranchStyle, r: Rect, color: Int, theme: MapTheme) {
        if (style == BranchStyle.BLURRED) {
            drawGlow(canvas, color, theme) { ring -> canvas.drawRoundRect(r.inset(ring * 1.6f).toRectF(), 10f + ring, 10f + ring, fill) }
        }
        fill.color = color
        canvas.drawPath(BranchGeometry.mainBoxPath(style, r), fill)
    }

    private fun drawTopic(canvas: Canvas, map: MindMap, topic: Topic, box: TopicBox, theme: MapTheme, images: ImageSource) {
        val r = box.rect
        val textColor: Int
        when (box.depth) {
            0 -> {
                fill.color = map.centralColor
                canvas.drawPath(centralShapePath(map.centralShape, r), fill)
                textColor = contrastingTextColor(map.centralColor)
            }
            1 -> {
                val color = theme.branchColor(box.colorSlot)
                drawMainBox(canvas, box.style, r, color, theme)
                textColor = contrastingTextColor(color)
            }
            else -> textColor = theme.text
        }
        val c = metrics.content(topic, box.depth)
        val originX = r.centerX - c.contentWidth / 2
        val originY = r.centerY - c.contentHeight / 2
        topic.image?.let { image ->
            val bitmap = images.bitmap(image.path)
            val dst = RectF(originX + c.imageX, originY + c.imageY, originX + c.imageX + image.width, originY + c.imageY + image.height)
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, null, dst, bitmapPaint)
            } else {
                fill.color = (theme.text and 0x00FFFFFF) or 0x22000000
                canvas.drawRect(dst, fill)
            }
        }
        icon(topic.icon)?.let { drawable ->
            val x = (originX + c.iconX).toInt()
            val y = (originY + c.iconY).toInt()
            drawable.setBounds(x, y, x + c.iconSize.toInt(), y + c.iconSize.toInt())
            drawable.setTint(textColor)
            drawable.draw(canvas)
        }
        c.paint.color = textColor
        canvas.save()
        canvas.translate(originX + c.textX, originY + c.textY)
        c.text.draw(canvas)
        canvas.restore()
    }

    private fun icon(name: String?): Drawable? {
        val id = IconSet.drawableFor(name) ?: return null
        return icons.getOrPut(name!!) { context.getDrawable(id)?.mutate() }
    }

    fun centralShapePath(shape: CentralShape, r: Rect): Path = Path().apply {
        val rf = r.toRectF()
        when (shape) {
            CentralShape.RECTANGLE -> addRect(rf, Path.Direction.CW)
            CentralShape.ROUNDED_RECTANGLE -> addRoundRect(rf, 18f, 18f, Path.Direction.CW)
            CentralShape.CAPSULE -> addRoundRect(rf, r.height / 2, r.height / 2, Path.Direction.CW)
            CentralShape.ELLIPSE, CentralShape.CIRCLE -> addOval(rf, Path.Direction.CW)
            CentralShape.DIAMOND -> {
                moveTo(r.centerX, r.top); lineTo(r.right, r.centerY); lineTo(r.centerX, r.bottom); lineTo(r.left, r.centerY); close()
            }
            CentralShape.HEXAGON -> {
                val inset = r.height / 2
                moveTo(r.left + inset, r.top); lineTo(r.right - inset, r.top); lineTo(r.right, r.centerY)
                lineTo(r.right - inset, r.bottom); lineTo(r.left + inset, r.bottom); lineTo(r.left, r.centerY); close()
            }
        }
    }

    private fun drawSelection(canvas: Canvas, r: Rect, color: Int) {
        stroke.color = color
        stroke.strokeWidth = 2.5f
        canvas.drawRoundRect(r.inset(5f).toRectF(), 12f, 12f, stroke)
    }

    private fun drawDropIndicator(canvas: Canvas, box: TopicBox, target: DropTarget, color: Int) {
        stroke.color = color
        stroke.strokeWidth = 4f
        val r = box.rect.inset(4f)
        when (target.kind) {
            DropTarget.Kind.CHILD -> {
                if (box.depth == 0 && target.side != null) {
                    // On the central topic, highlight the half that decides the side.
                    val half = if (target.side == Side.LEFT) Rect(r.left, r.top, r.centerX, r.bottom) else Rect(r.centerX, r.top, r.right, r.bottom)
                    canvas.drawRoundRect(half.toRectF(), 12f, 12f, stroke)
                } else {
                    canvas.drawRoundRect(r.toRectF(), 12f, 12f, stroke)
                }
            }
            DropTarget.Kind.BEFORE -> canvas.drawLine(r.left, r.top, r.right, r.top, stroke)
            DropTarget.Kind.AFTER -> canvas.drawLine(r.left, r.bottom, r.right, r.bottom, stroke)
        }
    }
}

fun Rect.toRectF(): RectF = RectF(left, top, right, bottom)

private const val SAMPLE_SEED = 7
private const val GLOW_SPREAD = 5f

/** [color] with its alpha replaced by [alpha] (0 to 1). */
private fun withAlpha(color: Int, alpha: Float): Int = (color and 0x00FFFFFF) or ((alpha * 255).toInt().coerceIn(0, 255) shl 24)
