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
            drawBranch(canvas, parent, box, theme.branchColor(box.colorSlot))
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
        if (box.depth >= 2) {
            stroke.color = theme.branchColor(box.colorSlot)
            stroke.strokeWidth = branchWidths(box.depth).second
            canvas.drawLine(box.rect.left, box.rect.bottom, box.rect.right, box.rect.bottom, stroke)
        }
        drawTopic(canvas, map, topic, box, theme, images)
        canvas.restore()
    }

    /** Start and end width of the branch leading to a topic at [depth]. */
    private fun branchWidths(depth: Int): Pair<Float, Float> = when (depth) {
        1 -> 12f to 5f
        2 -> 4.5f to 2.5f
        else -> 2.5f to 1.5f
    }

    private fun drawBranch(canvas: Canvas, parent: TopicBox, child: TopicBox, color: Int) {
        val right = child.side != Side.LEFT
        val (startX, startY) = when {
            parent.depth == 0 -> parent.rect.centerX to parent.rect.centerY
            parent.depth == 1 -> (if (right) parent.rect.right else parent.rect.left) to parent.rect.centerY
            else -> (if (right) parent.rect.right else parent.rect.left) to parent.rect.bottom
        }
        val endX = if (right) child.rect.left else child.rect.right
        val endY = if (child.depth == 1) child.rect.centerY else child.rect.bottom
        val (w0, w1) = branchWidths(child.depth)
        fill.color = color
        canvas.drawPath(ribbon(startX, startY, endX, endY, w0, w1), fill)
        if (child.depth >= 2) {
            // Subtopics have no box: an underline in the branch colour carries the topic.
            stroke.color = color
            stroke.strokeWidth = w1
            canvas.drawLine(child.rect.left, child.rect.bottom, child.rect.right, child.rect.bottom, stroke)
        }
    }

    /** A filled cubic Bézier ribbon whose width tapers from [w0] to [w1]. */
    private fun ribbon(x0: Float, y0: Float, x3: Float, y3: Float, w0: Float, w1: Float): Path {
        val mx = (x0 + x3) / 2
        val c1x = mx
        val c1y = y0
        val c2x = mx
        val c2y = y3
        val steps = 28
        val upper = FloatArray((steps + 1) * 2)
        val lower = FloatArray((steps + 1) * 2)
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val u = 1 - t
            val x = u * u * u * x0 + 3 * u * u * t * c1x + 3 * u * t * t * c2x + t * t * t * x3
            val y = u * u * u * y0 + 3 * u * u * t * c1y + 3 * u * t * t * c2y + t * t * t * y3
            val dx = 3 * u * u * (c1x - x0) + 6 * u * t * (c2x - c1x) + 3 * t * t * (x3 - c2x)
            val dy = 3 * u * u * (c1y - y0) + 6 * u * t * (c2y - c1y) + 3 * t * t * (y3 - c2y)
            val len = hypot(dx, dy).takeIf { it > 0.0001f } ?: 1f
            val half = (w0 + (w1 - w0) * t) / 2
            val nx = -dy / len * half
            val ny = dx / len * half
            upper[i * 2] = x + nx; upper[i * 2 + 1] = y + ny
            lower[i * 2] = x - nx; lower[i * 2 + 1] = y - ny
        }
        return Path().apply {
            moveTo(upper[0], upper[1])
            for (i in 1..steps) lineTo(upper[i * 2], upper[i * 2 + 1])
            for (i in steps downTo 0) lineTo(lower[i * 2], lower[i * 2 + 1])
            close()
        }
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
                fill.color = color
                canvas.drawRoundRect(r.toRectF(), 10f, 10f, fill)
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
        val paint = metrics.paint(box.depth)
        paint.color = textColor
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
