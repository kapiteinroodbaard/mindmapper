package io.github.wlenaerts.mindmapper.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import io.github.wlenaerts.mindmapper.R
import io.github.wlenaerts.mindmapper.core.edit.DropTarget
import io.github.wlenaerts.mindmapper.core.layout.MapLayout
import io.github.wlenaerts.mindmapper.core.layout.MapLayouter
import io.github.wlenaerts.mindmapper.core.layout.Rect
import io.github.wlenaerts.mindmapper.core.layout.TopicBox
import io.github.wlenaerts.mindmapper.core.layout.BranchShades
import io.github.wlenaerts.mindmapper.core.model.BranchColorScheme
import io.github.wlenaerts.mindmapper.core.model.BranchStyle
import io.github.wlenaerts.mindmapper.core.model.CentralShape
import io.github.wlenaerts.mindmapper.core.model.MindMap
import io.github.wlenaerts.mindmapper.core.model.Side
import io.github.wlenaerts.mindmapper.core.model.Topic
import io.github.wlenaerts.mindmapper.core.model.subtreeIds
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.random.Random

/** Supplies decoded images for topic image paths; null while unavailable. */
fun interface ImageSource {
    fun bitmap(path: String): Bitmap?
}

/**
 * Draws a laid-out map onto any [Canvas] in layout units (the caller applies scale and translation).
 * Used for the screen, PDF and PNG output so all three look the same.
 */
class MapRenderer(private val context: Context, val metrics: TopicMetrics = TopicMetrics()) {

    init {
        metrics.minutesLabel = { minutes -> context.getString(R.string.duration_minutes, minutes) }
    }

    /** Lays out [map]; [showDurations] makes room for planned durations, which exports leave out. */
    fun layout(map: MindMap, showDurations: Boolean = true): MapLayout =
        MapLayouter.layout(map, { topic, depth -> metrics.measure(map, topic, depth, showDurations) }, showDurations)

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
        highlightId: String? = null,
        highlightScale: Float = 1f,
        neighbourIds: Set<String>? = null,
    ) {
        val topics = map.topics().associateBy { it.id }
        val colors = branchColors(map, layout, theme)
        // The highlighted main branch and all its subtopics are drawn [highlightScale] times as thick.
        val highlighted = highlightId?.takeIf { layout[it]?.depth == 1 }?.let { id -> map.find(id)?.let(::subtreeIds) }.orEmpty()
        // With [neighbourIds], the other main branches fade: the neighbours a little, the rest further.
        val alphas = if (highlighted.isEmpty() || neighbourIds == null) emptyMap() else buildMap {
            for (main in map.root.children) {
                if (main.id == highlightId) continue
                val alpha = if (main.id in neighbourIds) NEIGHBOUR_ALPHA else FAR_ALPHA
                subtreeIds(main).forEach { put(it, alpha) }
            }
        }
        // Branches first, so topics are drawn on top of them.
        drawFaded(canvas, layout.boxes, alphas) { box ->
            if (box.depth == 0 || box.topicId in hiddenIds) return@drawFaded
            val parent = layout[box.parentId ?: return@drawFaded] ?: return@drawFaded
            drawBranch(canvas, map, parent, box, colors.getValue(box.topicId), theme, if (box.topicId in highlighted) highlightScale else 1f)
        }
        drawFaded(canvas, layout.boxes, alphas) { box ->
            if (box.topicId in hiddenIds) return@drawFaded
            val topic = topics[box.topicId] ?: return@drawFaded
            drawTopic(canvas, map, topic, box, colors[box.topicId], theme, images, layout.showsDurations, if (box.topicId in highlighted) highlightScale else 1f)
        }
        selectedId?.let { id -> layout[id]?.let { drawSelection(canvas, it.rect, theme.selection) } }
        dropTarget?.let { target -> layout[target.anchorId]?.let { drawDropIndicator(canvas, it, target, theme.dropIndicator) } }
    }

    /**
     * Calls [drawBox] for every box, faded ones first, each fade level in one layer of its [alphas] value so
     * overlapping parts of a faded branch do not show through each other. Boxes without an alpha are opaque.
     */
    private inline fun drawFaded(canvas: Canvas, boxes: List<TopicBox>, alphas: Map<String, Int>, drawBox: (TopicBox) -> Unit) {
        for (alpha in alphas.values.toSortedSet()) {
            canvas.saveLayerAlpha(null, alpha)
            boxes.forEach { if (alphas[it.topicId] == alpha) drawBox(it) }
            canvas.restore()
        }
        boxes.forEach { if (it.topicId !in alphas) drawBox(it) }
    }

    /** Draws only topic [topicId], moved by ([dx], [dy]); used for the drag ghost. */
    fun drawTopicOnly(canvas: Canvas, map: MindMap, layout: MapLayout, topicId: String, theme: MapTheme, images: ImageSource, dx: Float, dy: Float) {
        val box = layout[topicId] ?: return
        val topic = map.find(topicId) ?: return
        canvas.save()
        canvas.translate(dx, dy)
        val color = if (box.depth >= 1) branchColor(map, layout, theme, topicId) else null
        if (box.depth >= 2) drawUnderline(canvas, box, color!!, branchWidths(map, box.depth).second, theme)
        drawTopic(canvas, map, topic, box, color, theme, images, layout.showsDurations)
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
        val w1 = height * 0.05f
        val endX = box.left + BranchGeometry.mainBoxJoin(style, box, w1 / 2)
        drawLine(
            canvas, style, x0 = width * 0.04f, y0 = startY, x1 = endX, y1 = box.centerY, w0 = height * 0.11f, w1 = w1,
            color = color, theme = theme, seed = if (style == BranchStyle.ROPE) ROPE_SAMPLE_SEED else SAMPLE_SEED, start = null, end = box, decorate = true,
        )
        drawMainBox(canvas, style, box, color, theme)
    }

    /**
     * The branch colour of every topic but the central one: a main branch's own colour, else its colour in
     * the map's scheme; subtopics take their main branch's colour.
     */
    fun branchColors(map: MindMap, layout: MapLayout, theme: MapTheme): Map<String, Int> {
        val topics = map.root.children.associateBy { it.id }
        val colors = HashMap<String, Int>()
        // Boxes are in depth-first order, so a parent's colour is known before its children's.
        for (box in layout.boxes) {
            when {
                box.depth == 0 -> Unit
                box.depth == 1 -> colors[box.topicId] = topics[box.topicId]?.branchColor ?: schemeColor(map, layout, theme, box)
                else -> colors[box.topicId] = colors[box.parentId] ?: schemeColor(map, layout, theme, box)
            }
        }
        return colors
    }

    /** The branch colour of topic [topicId]; the central topic has none and gets its own colour. */
    fun branchColor(map: MindMap, layout: MapLayout, theme: MapTheme, topicId: String): Int =
        branchColors(map, layout, theme)[topicId] ?: map.centralColor

    // Shades suit what the map is drawn on: the background image's average (cached by [theme]), else the colour.
    private fun schemeColor(map: MindMap, layout: MapLayout, theme: MapTheme, box: TopicBox): Int = when (map.branchColors) {
        BranchColorScheme.PALETTE -> theme.branchColor(box.colorSlot)
        BranchColorScheme.SHADES -> BranchShades.shade(
            map.centralColor, box.clockwise, layout.mainBranchCount,
            darkBackground = relativeLuminance(theme.text) > 0.5f,
            backgroundLuminance = map.backgroundImage?.let(luminances::get) ?: relativeLuminance(theme.background),
        )
    }

    /**
     * Fills a [width] by [height] area (in the canvas's current units) with the map's background: [theme]'s
     * background colour, then the background image scaled to cover the area, centred and cropped.
     */
    fun drawBackground(canvas: Canvas, map: MindMap, theme: MapTheme, images: ImageSource, width: Float, height: Float) {
        // Only the area itself: the canvas may draw beyond it (on screen, under the system bars).
        fill.color = theme.background
        canvas.drawRect(0f, 0f, width, height, fill)
        val bitmap = map.backgroundImage?.let(images::bitmap) ?: return
        val scale = maxOf(width / bitmap.width, height / bitmap.height)
        val w = bitmap.width * scale
        val h = bitmap.height * scale
        val dst = RectF((width - w) / 2, (height - h) / 2, (width + w) / 2, (height + h) / 2)
        canvas.save()
        canvas.clipRect(0f, 0f, width, height)
        canvas.drawBitmap(bitmap, null, dst, bitmapPaint)
        canvas.restore()
    }

    private val luminances = HashMap<String, Float>()

    /** Average relative luminance of the map's background image, or null while it has none or it is unavailable. */
    fun backgroundLuminance(map: MindMap, images: ImageSource): Float? {
        val path = map.backgroundImage ?: return null
        luminances[path]?.let { return it }
        val bitmap = images.bitmap(path) ?: return null
        val pixel = Bitmap.createScaledBitmap(bitmap, 1, 1, true).let { one -> one.getPixel(0, 0).also { if (one !== bitmap) one.recycle() } }
        return relativeLuminance(pixel).also { luminances[path] = it }
    }

    /** The appearance of [map]: see [MapTheme.forMap]. */
    fun theme(map: MindMap, systemDark: Boolean, images: ImageSource): MapTheme =
        MapTheme.forMap(map, systemDark, backgroundLuminance(map, images))

    /** Where topic [topicId]'s image is drawn, in layout units, or null when it has none. */
    fun imageRect(map: MindMap, layout: MapLayout, topicId: String): Rect? {
        val box = layout[topicId] ?: return null
        val topic = map.find(topicId) ?: return null
        val image = topic.image ?: return null
        val duration = if (layout.showsDurations) metrics.shownDurationSeconds(map, topic, box.depth) else null
        val c = metrics.content(topic, box.depth, duration)
        val left = box.rect.centerX - c.contentWidth / 2 + c.imageX
        val top = box.rect.centerY - c.contentHeight / 2 + c.imageY
        return Rect(left, top, left + image.width, top + image.height)
    }

    /** Start and end width of the branch leading to a topic at [depth]; main branches follow the map's trunk width. */
    private fun branchWidths(map: MindMap, depth: Int): Pair<Float, Float> = when (depth) {
        1 -> map.trunkWidth to 5f
        2 -> 4.5f to 2.5f
        else -> 2.5f to 1.5f
    }

    private fun drawBranch(canvas: Canvas, map: MindMap, parent: TopicBox, child: TopicBox, color: Int, theme: MapTheme, scale: Float) {
        val right = child.side != Side.LEFT
        var (w0, w1) = branchWidths(map, child.depth)
        // A road keeps its width; a main road is at most three quarters as wide as its box is high.
        if (child.style == BranchStyle.ROAD) {
            if (child.depth == 1) w0 = minOf(w0, child.rect.height * 0.75f)
            w1 = w0
        }
        // Branches start and end just inside a main topic's box, deep enough that the box covers their flat ends.
        val (startX, startY) = when {
            parent.depth == 0 -> parent.rect.centerX to parent.rect.centerY
            parent.depth == 1 -> {
                val join = BranchGeometry.mainBoxJoin(parent.style, parent.rect, w0 * scale / 2, outer = true)
                (if (right) parent.rect.right - join else parent.rect.left + join) to parent.rect.centerY
            }
            else -> (if (right) parent.rect.right else parent.rect.left) to parent.rect.bottom
        }
        val inset = if (child.depth == 1) BranchGeometry.mainBoxJoin(child.style, child.rect, w1 * scale / 2) else 0f
        val endX = if (right) child.rect.left + inset else child.rect.right - inset
        val endY = if (child.depth == 1) child.rect.centerY else child.rect.bottom
        // A main branch starts inside the central topic: it keeps its full width up to the topic's edge and
        // only tapers from there, at the map's taper rate.
        val main = child.depth == 1
        drawLine(
            canvas, child.style, startX, startY, endX, endY, w0, w1, color, theme, child.topicId.hashCode(),
            start = parent.rect, end = child.rect, decorate = main, taper = if (main) map.trunkTaper else 1f, inside = if (main) parent.rect else null,
            scale = scale,
        )
        // Subtopics have no box: an underline in the branch colour carries the topic.
        if (child.depth >= 2) drawUnderline(canvas, child, color, w1, theme, scale)
    }

    /**
     * Draws one tapered branch line in [style]. [start] and [end] are the boxes it connects, kept clear of
     * decorations; [decorate] marks a main branch, which gets the plant style's leaves and tendrils, the rope's
     * full-size looping, the snake's head and the arrow's head and chevrons. [scale] thickens the line and
     * enlarges the decorations without moving them.
     */
    private fun drawLine(
        canvas: Canvas, style: BranchStyle, x0: Float, y0: Float, x1: Float, y1: Float, w0: Float, w1: Float,
        color: Int, theme: MapTheme, seed: Int, start: Rect?, end: Rect?, decorate: Boolean,
        taper: Float = 1f, inside: Rect? = null, scale: Float = 1f,
    ) {
        val line = when (style) {
            BranchStyle.STANDARD, BranchStyle.BLURRED, BranchStyle.PLANT, BranchStyle.ARROW -> BranchGeometry.curve(x0, y0, x1, y1)
            BranchStyle.ROPE -> BranchGeometry.curve(x0, y0, x1, y1, BranchGeometry.denseSteps(x0, y0, x1, y1))
            BranchStyle.SNAKE -> {
                val base = BranchGeometry.curve(x0, y0, x1, y1, BranchGeometry.denseSteps(x0, y0, x1, y1))
                if (decorate) BranchGeometry.wave(base, (4f + w0 * 0.2f).coerceAtMost(10f), 70f, inside) else BranchGeometry.wave(base, 2.5f, 26f)
            }
            BranchStyle.STRAIGHT -> BranchGeometry.straight(x0, y0, x1, y1)
            BranchStyle.STEEL -> BranchGeometry.angular(x0, y0, x1, y1, inside)
            BranchStyle.ROAD -> BranchGeometry.orthogonal(x0, y0, x1, y1, maxOf(12f, w0 * 0.8f), inside)
            BranchStyle.LIGHTNING -> {
                val amplitude = (hypot(x1 - x0, y1 - y0) * 0.06f).coerceIn(3f, 10f) + w0 * 0.25f
                BranchGeometry.zigzag(x0, y0, x1, y1, seed, amplitude, calm = inside)
            }
        }
        val halves = BranchGeometry.halfWidths(line, w0, w1, taper, inside)
        val shown = if (scale == 1f) halves else FloatArray(halves.size) { halves[it] * scale }
        when (style) {
            BranchStyle.STEEL -> drawSteel(canvas, line, halves, shown, color)
            BranchStyle.ROPE -> {
                val loop = ropeLoop(line, halves, seed, decorate, inside)
                if (loop == null) drawRope(canvas, line, shown, color) else {
                    val looped = BranchGeometry.looping(line, shown, loop.start, loop.radius, loop.dir)
                    drawRope(canvas, looped.line, looped.halves, color, looped.split)
                }
            }
            BranchStyle.SNAKE -> drawSnake(canvas, line, shown, color, if (decorate) mainSnakeHead(w0 * scale) else null, inside)
            BranchStyle.ROAD -> drawRoad(canvas, line, shown, color)
            BranchStyle.ARROW -> drawArrow(canvas, line, shown, color, head = decorate, chevrons = decorate, inside = inside)
            else -> {
                if (style == BranchStyle.BLURRED) {
                    val spread = 1f + w0 / 16f
                    drawGlow(canvas, color, theme) { ring -> canvas.drawPath(BranchGeometry.outline(line, shown, ring * spread), fill) }
                }
                fill.color = color
                canvas.drawPath(BranchGeometry.outline(line, shown), fill)
                // Plant parts are placed along the unscaled line so they stay put while the line pulses.
                if (style == BranchStyle.PLANT && decorate) drawPlant(canvas, line, halves, color, seed, start, end, scale)
            }
        }
    }

    /** Where a rope's looping goes: [start] units along the line, [radius], curling to side [dir]. */
    private class RopeLoop(val start: Float, val radius: Float, val dir: Float)

    /**
     * The [seed]-fixed looping of a rope along [line]: on the middle part of what shows outside [inside],
     * large on a [main] branch and small on a subtopic's, or none when there is no room for a loop with a hole.
     */
    private fun ropeLoop(line: Centreline, halves: FloatArray, seed: Int, main: Boolean, inside: Rect?): RopeLoop? {
        val lengths = BranchGeometry.lengths(line)
        val from = lengths[line.exitIndex(inside)]
        val visible = lengths.last() - from
        val random = Random(seed)
        val wanted = if (main) maxOf(halves[0] * 2.2f, 13f + 9f * random.nextFloat()) else 7f
        val radius = wanted.coerceAtMost(visible / 5f)
        val start = from + visible * (0.25f + 0.25f * random.nextFloat())
        val dir = if (random.nextBoolean()) 1f else -1f
        return if (radius < maxOf(6f, halves[0] * 2.2f)) null else RopeLoop(start, radius, dir)
    }

    /**
     * A steel truss: two chords along the edges, braces zigzagging between them with rivets at the joints, and
     * a bolted plate on every corner. Joints are spaced along the unscaled [halves], so they stay put while the
     * [shown] line pulses.
     */
    private fun drawSteel(canvas: Canvas, line: Centreline, halves: FloatArray, shown: FloatArray, color: Int) {
        val dark = mix(color, BLACK, 0.35f)
        val width = maxOf(1.2f, shown[0] * 0.22f)
        stroke.color = color
        stroke.strokeWidth = width
        canvas.drawPath(BranchGeometry.edge(line, shown, 1f), stroke)
        canvas.drawPath(BranchGeometry.edge(line, shown, -1f), stroke)
        val placed = BranchGeometry.Walker(line, halves)
        val drawn = BranchGeometry.Walker(line, shown)
        val joints = ArrayList<BranchGeometry.Walker.Sample>()
        var s = 0f
        while (s <= placed.total) {
            joints += drawn.at(s)
            s += maxOf(7f, placed.at(s).h * 1.9f)
        }
        val braces = Path()
        for (i in 1 until joints.size) {
            val a = joints[i - 1]
            val b = joints[i]
            val side = if (i % 2 == 1) 1f else -1f
            braces.moveTo(a.x + a.nx * a.h * side, a.y + a.ny * a.h * side)
            braces.lineTo(b.x - b.nx * b.h * side, b.y - b.ny * b.h * side)
        }
        stroke.strokeWidth = width * 0.75f
        canvas.drawPath(braces, stroke)
        for (i in 1 until line.size - 1) {
            if (line.miter[i] > 1.02f) drawGusset(canvas, line.xs[i], line.ys[i], shown[i] * 1.25f, width, color, dark)
        }
        fill.color = dark
        for (q in joints) for (side in floatArrayOf(1f, -1f)) canvas.drawCircle(q.x + q.nx * q.h * side, q.y + q.ny * q.h * side, width * 0.7f, fill)
    }

    /** A hexagonal plate of [radius] with three bolts, where two steel pieces meet at a corner. */
    private fun drawGusset(canvas: Canvas, x: Float, y: Float, radius: Float, width: Float, color: Int, dark: Int) {
        val plate = Path()
        for (k in 0 until 6) {
            val a = (k * 60 + 30) * Math.PI / 180
            val px = x + kotlin.math.cos(a).toFloat() * radius
            val py = y + kotlin.math.sin(a).toFloat() * radius
            if (k == 0) plate.moveTo(px, py) else plate.lineTo(px, py)
        }
        plate.close()
        fill.color = color
        canvas.drawPath(plate, fill)
        stroke.color = dark
        stroke.strokeWidth = 1f
        canvas.drawPath(plate, stroke)
        fill.color = mix(color, WHITE, 0.6f)
        for (k in 0 until 3) {
            val a = k * 2 * Math.PI / 3
            canvas.drawCircle(x + kotlin.math.cos(a).toFloat() * radius * 0.5f, y + kotlin.math.sin(a).toFloat() * radius * 0.5f, maxOf(0.8f, width * 0.55f), fill)
        }
    }

    /**
     * A twisted rope: the body with dark edges, and slanted dark strands with lighter ones beside them. The
     * part after [split] is drawn over the part before it, so a looping crosses over itself.
     */
    private fun drawRope(canvas: Canvas, line: Centreline, halves: FloatArray, color: Int, split: Int = line.size - 1) {
        val dark = mix(color, BLACK, 0.35f)
        val light = mix(color, WHITE, 0.35f)
        val walker = BranchGeometry.Walker(line, halves)
        val strand = Path()
        for ((from, to) in listOf(0 to split, split to line.size - 1)) {
            if (to <= from) continue
            fill.color = color
            canvas.drawPath(BranchGeometry.outline(line, halves, 0f, from, to), fill)
            stroke.color = dark
            stroke.strokeWidth = 0.8f
            canvas.drawPath(BranchGeometry.edge(line, halves, 1f, from, to), stroke)
            canvas.drawPath(BranchGeometry.edge(line, halves, -1f, from, to), stroke)
            stroke.strokeCap = Paint.Cap.BUTT
            var s = walker.lengths[from]
            while (s < walker.lengths[to]) {
                val h = walker.at(s).h
                val period = maxOf(4f, h * 1.5f)
                for ((shade, shift, thickness) in listOf(Triple(dark, 0f, 0.45f), Triple(light, period * 0.45f, 0.18f))) {
                    strand.rewind()
                    for (k in 0..4) {
                        val p = walker.at(s + shift + period * 0.8f * k / 4)
                        val across = p.h * (0.92f - 1.84f * k / 4)
                        if (k == 0) strand.moveTo(p.x + p.nx * across, p.y + p.ny * across) else strand.lineTo(p.x + p.nx * across, p.y + p.ny * across)
                    }
                    stroke.color = shade
                    stroke.strokeWidth = maxOf(0.8f, h * thickness)
                    canvas.drawPath(strand, stroke)
                }
                s += period
            }
            stroke.strokeCap = Paint.Cap.ROUND
        }
    }

    /** The size of a snake's head: [length] along the body, [width] across. */
    private class SnakeHead(val length: Float, val width: Float)

    /** A main branch's snake head, sized from the branch's starting width [w0]. */
    private fun mainSnakeHead(w0: Float) = SnakeHead(maxOf(14f, w0 * 1.7f), maxOf(10f, w0 * 1.15f))

    /**
     * A snake: the body with a row of dark scales, ending in [head] (if any) whose tongue reaches the end of
     * [line]. A head too long for the part outside [inside] is made smaller.
     */
    private fun drawSnake(canvas: Canvas, line: Centreline, halves: FloatArray, color: Int, head: SnakeHead?, inside: Rect?) {
        val walker = BranchGeometry.Walker(line, halves)
        val from = walker.lengths[line.exitIndex(inside)]
        val room = walker.total - from
        var length = head?.length ?: 0f
        var width = head?.width ?: 0f
        var tongue = maxOf(5f, length * 0.22f)
        if (head != null && length + tongue > room * 0.6f) {
            val shrink = room * 0.6f / (length + tongue)
            length *= shrink; width *= shrink; tongue *= shrink
        }
        val bodyEnd = if (head == null) walker.total else walker.total - length - tongue
        fill.color = color
        canvas.drawPath(BranchGeometry.outline(line, halves, 0f, 0, walker.indexAt(bodyEnd)), fill)
        if (halves[0] >= 3f) {
            val scales = Path()
            var s = from + 4f
            while (s < bodyEnd - 2f) {
                val q = walker.at(s)
                val d = q.h * 0.55f
                scales.moveTo(q.x + q.tx * d, q.y + q.ty * d)
                scales.lineTo(q.x + q.nx * d * 0.8f, q.y + q.ny * d * 0.8f)
                scales.lineTo(q.x - q.tx * d, q.y - q.ty * d)
                scales.lineTo(q.x - q.nx * d * 0.8f, q.y - q.ny * d * 0.8f)
                scales.close()
                s += maxOf(5f, q.h * 1.3f)
            }
            fill.color = mix(color, BLACK, 0.4f)
            canvas.drawPath(scales, fill)
        }
        if (head == null) return
        val neck = walker.at(bodyEnd)
        val last = line.size - 1
        val angle = atan2(line.ys[last] - neck.y, line.xs[last] - neck.x)
        drawSnakeHead(canvas, neck.x, neck.y, angle, neck.h, length, width, tongue, color)
    }

    /** A snake's head at ([x], [y]) looking in direction [angle], widening from a neck of half width [neck]. */
    private fun drawSnakeHead(canvas: Canvas, x: Float, y: Float, angle: Float, neck: Float, length: Float, width: Float, tongue: Float, color: Int) {
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat())
        stroke.color = mix(color, BLACK, 0.55f)
        stroke.strokeWidth = maxOf(1.2f, width * 0.08f)
        canvas.drawPath(Path().apply {
            moveTo(length * 0.9f, 0f); lineTo(length + tongue * 0.7f, 0f); lineTo(length + tongue, -tongue * 0.35f)
            moveTo(length + tongue * 0.7f, 0f); lineTo(length + tongue, tongue * 0.35f)
        }, stroke)
        fill.color = color
        canvas.drawPath(Path().apply {
            moveTo(-1f, -neck)
            cubicTo(length * 0.2f, -width * 0.62f, length * 0.7f, -width * 0.6f, length, 0f)
            cubicTo(length * 0.7f, width * 0.6f, length * 0.2f, width * 0.62f, -1f, neck)
            close()
        }, fill)
        fill.color = mix(color, BLACK, 0.4f)
        for (i in 0 until 3) canvas.drawCircle(length * (0.25f + i * 0.15f), 0f, width * 0.08f, fill)
        for (side in floatArrayOf(-1f, 1f)) {
            fill.color = WHITE
            canvas.drawCircle(length * 0.62f, side * width * 0.24f, maxOf(1.4f, width * 0.12f), fill)
            fill.color = 0xFF111111.toInt()
            canvas.drawCircle(length * 0.66f, side * width * 0.24f, maxOf(0.8f, width * 0.065f), fill)
        }
        canvas.restore()
    }

    /** A road: slightly darkened surface, white edge lines when there is room, and a dashed centre line. */
    private fun drawRoad(canvas: Canvas, line: Centreline, halves: FloatArray, color: Int) {
        fill.color = mix(color, BLACK, 0.1f)
        canvas.drawPath(BranchGeometry.outline(line, halves), fill)
        val h = halves[0]
        stroke.color = WHITE
        stroke.strokeCap = Paint.Cap.BUTT
        if (h >= 4f) {
            stroke.strokeWidth = maxOf(0.8f, h * 0.09f)
            canvas.drawPath(BranchGeometry.edge(line, halves, 0.78f), stroke)
            canvas.drawPath(BranchGeometry.edge(line, halves, -0.78f), stroke)
        }
        stroke.strokeWidth = maxOf(0.7f, h * 0.1f)
        stroke.pathEffect = DashPathEffect(floatArrayOf(maxOf(3f, h * 1.1f), maxOf(2.5f, h * 0.8f)), 0f)
        canvas.drawPath(BranchGeometry.edge(line, halves, 0f), stroke)
        stroke.pathEffect = null
        stroke.strokeCap = Paint.Cap.ROUND
    }

    /** An arrow: the shaft, with a [head] at the end of [line] and white [chevrons] along the part outside [inside]. */
    private fun drawArrow(canvas: Canvas, line: Centreline, halves: FloatArray, color: Int, head: Boolean, chevrons: Boolean, inside: Rect?) {
        fill.color = color
        if (!head) {
            canvas.drawPath(BranchGeometry.outline(line, halves), fill)
            return
        }
        val walker = BranchGeometry.Walker(line, halves)
        val tip = halves.last()
        val length = maxOf(8f, tip * 3.4f)
        val half = maxOf(5f, tip * 2.6f)
        val stop = (walker.total - length).coerceAtLeast(0f)
        canvas.drawPath(BranchGeometry.outline(line, halves, 0f, 0, walker.indexAt(stop)), fill)
        val p = walker.at(stop)
        val last = line.size - 1
        canvas.drawPath(Path().apply {
            moveTo(p.x + p.nx * half, p.y + p.ny * half); lineTo(line.xs[last], line.ys[last]); lineTo(p.x - p.nx * half, p.y - p.ny * half); close()
        }, fill)
        if (!chevrons) return
        stroke.color = withAlpha(WHITE, 0.82f)
        stroke.strokeJoin = Paint.Join.ROUND
        var s = walker.lengths[line.exitIndex(inside)] + 18f
        while (s < stop - 10f) {
            val r = walker.at(s)
            val d = r.h * 0.55f
            stroke.strokeWidth = maxOf(1f, r.h * 0.22f)
            canvas.drawPath(Path().apply {
                moveTo(r.x - r.tx * d + r.nx * d, r.y - r.ty * d + r.ny * d)
                lineTo(r.x + r.tx * d * 0.4f, r.y + r.ty * d * 0.4f)
                lineTo(r.x - r.tx * d - r.nx * d, r.y - r.ty * d - r.ny * d)
            }, stroke)
            s += 24f
        }
        stroke.strokeJoin = Paint.Join.MITER
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

    private fun drawPlant(canvas: Canvas, line: Centreline, halves: FloatArray, color: Int, seed: Int, start: Rect?, end: Rect?, scale: Float) {
        fill.color = color
        stroke.color = color
        for (part in BranchGeometry.plantParts(line, halves, seed, start, end)) {
            if (part.leaf) {
                canvas.drawPath(BranchGeometry.leaf(part.x, part.y, part.angle, part.length * scale), fill)
            } else {
                stroke.strokeWidth = part.thickness * scale
                canvas.drawPath(BranchGeometry.tendril(part.x, part.y, part.angle, part.length * scale, part.turn), stroke)
            }
        }
    }

    /** The line under a subtopic, in its branch's style; [scale] thickens it and enlarges the plant leaf. */
    private fun drawUnderline(canvas: Canvas, box: TopicBox, color: Int, baseWidth: Float, theme: MapTheme, scale: Float = 1f) {
        val r = box.rect
        val width = baseWidth * scale
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
                canvas.drawPath(BranchGeometry.leaf(if (right) r.right else r.left, r.bottom, angle, 13f * scale), fill)
            }
            BranchStyle.LIGHTNING -> {
                stroke.color = color
                canvas.drawPath(BranchGeometry.zigzagUnderline(r.left, r.right, r.bottom, 2.2f + width / 2), stroke)
            }
            else -> {
                // Runs from where the branch arrives to the outer end, like the branch it continues.
                val right = box.side != Side.LEFT
                val inner = if (right) r.left else r.right
                val outer = if (right) r.right else r.left
                val line = BranchGeometry.sampledLine(inner, r.bottom, outer, r.bottom)
                fun even(half: Float) = FloatArray(line.size) { half }
                when (box.style) {
                    BranchStyle.STEEL -> maxOf(width, 2.5f).let { drawSteel(canvas, line, even(it), even(it), color) }
                    BranchStyle.ROPE -> drawRope(canvas, line, even(maxOf(width * 0.9f, 2.2f)), color)
                    BranchStyle.SNAKE -> drawSnake(canvas, BranchGeometry.wave(line, 2f, 26f), even(width / 2), color, SnakeHead(10f * scale, 7f * scale), null)
                    BranchStyle.ROAD -> drawRoad(canvas, line, even((if (box.depth == 2) 4.5f else 2.5f) * scale / 2), color)
                    else -> drawArrow(canvas, line, even(width / 2), color, head = true, chevrons = false, inside = null)
                }
            }
        }
    }

    /**
     * The box of a main topic in [style], with the blurred style's glow around it. A [scale] above 1 grows
     * an outline in the box colour around it, so the box pulses with its branch without moving its text.
     */
    private fun drawMainBox(canvas: Canvas, style: BranchStyle, r: Rect, color: Int, theme: MapTheme, scale: Float = 1f, right: Boolean = true) {
        if (style == BranchStyle.BLURRED) {
            drawGlow(canvas, color, theme) { ring -> canvas.drawRoundRect(r.inset(ring * 1.6f).toRectF(), 10f + ring, 10f + ring, fill) }
        }
        fill.color = color
        val path = BranchGeometry.mainBoxPath(style, r, right)
        canvas.drawPath(path, fill)
        if (scale > 1f) {
            stroke.color = color
            stroke.strokeWidth = (scale - 1f) * MAIN_BOX_PULSE
            stroke.strokeJoin = Paint.Join.ROUND
            canvas.drawPath(path, stroke)
            stroke.strokeJoin = Paint.Join.MITER
        }
        when (style) {
            BranchStyle.STEEL -> {
                // An inner frame and a rivet in every corner.
                stroke.color = mix(color, BLACK, 0.35f)
                stroke.strokeWidth = 2f
                canvas.drawRect(r.inset(-3f).toRectF(), stroke)
                fill.color = withAlpha(WHITE, 0.8f)
                for (x in floatArrayOf(r.left + 7f, r.right - 7f)) for (y in floatArrayOf(r.top + 7f, r.bottom - 7f)) canvas.drawCircle(x, y, 2f, fill)
            }
            BranchStyle.ROPE -> {
                val edge = BranchGeometry.ropeBoxLine(r)
                drawRope(canvas, edge, FloatArray(edge.size) { 3f }, mix(color, BLACK, 0.12f))
            }
            BranchStyle.SNAKE -> {
                stroke.color = mix(color, BLACK, 0.35f)
                stroke.strokeWidth = 1.5f
                stroke.pathEffect = DashPathEffect(floatArrayOf(3f, 3f), 0f)
                val inner = r.inset(-4f)
                canvas.drawRoundRect(inner.toRectF(), inner.height / 2, inner.height / 2, stroke)
                stroke.pathEffect = null
            }
            BranchStyle.ROAD -> {
                // Framed like a road sign.
                stroke.color = WHITE
                stroke.strokeWidth = 2f
                canvas.drawRoundRect(r.inset(-3.5f).toRectF(), 4f, 4f, stroke)
            }
            else -> Unit
        }
    }

    private fun drawTopic(
        canvas: Canvas,
        map: MindMap,
        topic: Topic,
        box: TopicBox,
        branchColor: Int?,
        theme: MapTheme,
        images: ImageSource,
        showDurations: Boolean,
        scale: Float = 1f,
    ) {
        val r = box.rect
        val textColor: Int
        when (box.depth) {
            0 -> {
                fill.color = map.centralColor
                canvas.drawPath(centralShapePath(map.centralShape, r), fill)
                textColor = topic.textColor ?: contrastingTextColor(map.centralColor)
            }
            1 -> {
                val color = branchColor!!
                drawMainBox(canvas, box.style, r, color, theme, scale, right = box.side != Side.LEFT)
                textColor = topic.textColor ?: contrastingTextColor(color)
            }
            else -> textColor = topic.textColor ?: theme.text
        }
        val duration = if (showDurations) metrics.shownDurationSeconds(map, topic, box.depth) else null
        val c = metrics.content(topic, box.depth, duration)
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
        c.duration?.let { line ->
            line.paint.color = textColor
            canvas.save()
            canvas.translate(originX + c.durationX, originY + c.durationY)
            line.draw(canvas)
            canvas.restore()
        }
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
/** Gives the rope sample a looping that curls upwards, so it stays inside the tile. */
private const val ROPE_SAMPLE_SEED = 8
private const val GLOW_SPREAD = 5f
/** Outline width added around a pulsing main box per unit of scale above 1. */
private const val MAIN_BOX_PULSE = 10f
/** Opacity of the main branches scheduled next to the highlighted one: faded, still readable. */
private const val NEIGHBOUR_ALPHA = 140
/** Opacity of all other main branches while one is highlighted. */
private const val FAR_ALPHA = 46

private const val BLACK = 0xFF000000.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()

/** Opaque [color] moved a fraction [f] of the way towards [other]. */
private fun mix(color: Int, other: Int, f: Float): Int {
    fun channel(shift: Int): Int {
        val a = (color shr shift) and 0xFF
        val b = (other shr shift) and 0xFF
        return (a + (b - a) * f).toInt().coerceIn(0, 255) shl shift
    }
    return (0xFF shl 24) or channel(16) or channel(8) or channel(0)
}

/** [color] with its alpha replaced by [alpha] (0 to 1). */
private fun withAlpha(color: Int, alpha: Float): Int = (color and 0x00FFFFFF) or ((alpha * 255).toInt().coerceIn(0, 255) shl 24)
