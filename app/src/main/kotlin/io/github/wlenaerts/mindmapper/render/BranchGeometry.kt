package io.github.wlenaerts.mindmapper.render

import android.graphics.Path
import io.github.wlenaerts.mindmapper.core.layout.Rect
import io.github.wlenaerts.mindmapper.core.layout.Size
import io.github.wlenaerts.mindmapper.core.model.BranchStyle
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * The middle line of a branch, sampled into points with unit normals. [miter] widens the outline at sharp
 * corners so a zigzag keeps its width there.
 */
class Centreline(val xs: FloatArray, val ys: FloatArray, val nx: FloatArray, val ny: FloatArray, val miter: FloatArray) {
    val size: Int get() = xs.size

    /** Index of the first point outside [r], or 0 when there is none. */
    fun exitIndex(r: Rect?): Int = r?.let { (0 until size).firstOrNull { i -> !it.contains(xs[i], ys[i]) } } ?: 0

    companion object {
        /** Builds a line from points, taking tangents from the neighbouring points. */
        fun of(xs: FloatArray, ys: FloatArray): Centreline {
            val n = xs.size
            val nx = FloatArray(n)
            val ny = FloatArray(n)
            val miter = FloatArray(n) { 1f }
            for (i in 0 until n) {
                // Unit directions of the segments before and after the point.
                val (ax, ay) = if (i > 0) unit(xs[i] - xs[i - 1], ys[i] - ys[i - 1]) else unit(xs[1] - xs[0], ys[1] - ys[0])
                val (bx, by) = if (i < n - 1) unit(xs[i + 1] - xs[i], ys[i + 1] - ys[i]) else ax to ay
                val (tx, ty) = unit(ax + bx, ay + by).takeIf { abs(ax + bx) + abs(ay + by) > 0.001f } ?: (bx to by)
                nx[i] = -ty
                ny[i] = tx
                // 1 / cos(half the turn), capped so very sharp corners do not spike.
                miter[i] = (1f / (tx * bx + ty * by).coerceAtLeast(0.5f))
            }
            return Centreline(xs, ys, nx, ny, miter)
        }

        private fun unit(dx: Float, dy: Float): Pair<Float, Float> {
            val len = hypot(dx, dy).takeIf { it > 0.0001f } ?: 1f
            return dx / len to dy / len
        }
    }
}

/** Line shapes, outlines and box shapes for the branch styles. Pure geometry, shared by all outputs. */
object BranchGeometry {
    private const val CURVE_STEPS = 40

    /** The smooth S-curve used by the standard and plant styles, leaving and arriving horizontally. */
    fun curve(x0: Float, y0: Float, x3: Float, y3: Float): Centreline {
        val mx = (x0 + x3) / 2
        val xs = FloatArray(CURVE_STEPS + 1)
        val ys = FloatArray(CURVE_STEPS + 1)
        val nx = FloatArray(CURVE_STEPS + 1)
        val ny = FloatArray(CURVE_STEPS + 1)
        for (i in 0..CURVE_STEPS) {
            val t = i / CURVE_STEPS.toFloat()
            val u = 1 - t
            xs[i] = u * u * u * x0 + 3 * u * u * t * mx + 3 * u * t * t * mx + t * t * t * x3
            ys[i] = u * u * u * y0 + 3 * u * u * t * y0 + 3 * u * t * t * y3 + t * t * t * y3
            val dx = 3 * u * u * (mx - x0) + 3 * t * t * (x3 - mx)
            val dy = 6 * u * t * (y3 - y0)
            val len = hypot(dx, dy).takeIf { it > 0.0001f } ?: 1f
            nx[i] = -dy / len
            ny[i] = dx / len
        }
        return Centreline(xs, ys, nx, ny, FloatArray(CURVE_STEPS + 1) { 1f })
    }

    /** A straight line, sampled finely so the taper stays smooth. */
    fun straight(x0: Float, y0: Float, x1: Float, y1: Float): Centreline {
        val steps = 20
        return Centreline.of(
            FloatArray(steps + 1) { x0 + (x1 - x0) * it / steps },
            FloatArray(steps + 1) { y0 + (y1 - y0) * it / steps },
        )
    }

    /**
     * A lightning zigzag from start to end. The corners alternate sides of the straight line at a random but
     * [seed]-fixed distance, so the bolt looks the same every time; both end points stay where they are.
     * Corners inside [calm] (the box the bolt starts in) stay on the straight line, so the bolt leaves the box
     * cleanly.
     */
    fun zigzag(x0: Float, y0: Float, x1: Float, y1: Float, seed: Int, amplitude: Float, calm: Rect? = null): Centreline {
        val length = hypot(x1 - x0, y1 - y0)
        val segments = (length / 26f).roundToInt().coerceIn(3, 9)
        val random = Random(seed)
        val (ux, uy) = if (length > 0.0001f) (x1 - x0) / length to (y1 - y0) / length else 1f to 0f
        val corners = segments + 1
        val cx = FloatArray(corners)
        val cy = FloatArray(corners)
        val sign = if (random.nextBoolean()) 1f else -1f
        for (i in 0 until corners) {
            // Corners are spaced a little irregularly along the line.
            val jitter = if (i == 0 || i == segments) 0f else (random.nextFloat() - 0.5f) * 0.5f
            val along = (i + jitter) / segments
            val offset = if (i == 0 || i == segments) 0f else sign * (if (i % 2 == 0) 1 else -1) * amplitude * (0.6f + 0.4f * random.nextFloat())
            val bx = x0 + (x1 - x0) * along
            val by = y0 + (y1 - y0) * along
            val still = calm?.inset(amplitude)?.contains(bx, by) == true
            cx[i] = if (still) bx else bx - uy * offset
            cy[i] = if (still) by else by + ux * offset
        }
        // A few points per segment keep the taper smooth; the corners stay sharp.
        val perSegment = 4
        val xs = FloatArray(segments * perSegment + 1)
        val ys = FloatArray(segments * perSegment + 1)
        for (s in 0 until segments) for (k in 0 until perSegment) {
            val f = k / perSegment.toFloat()
            xs[s * perSegment + k] = cx[s] + (cx[s + 1] - cx[s]) * f
            ys[s * perSegment + k] = cy[s] + (cy[s + 1] - cy[s]) * f
        }
        xs[xs.size - 1] = cx[segments]
        ys[ys.size - 1] = cy[segments]
        return Centreline.of(xs, ys)
    }

    /**
     * Half widths along [line], tapering from [w0] to [w1]. Points within [inside] keep width [w0]; beyond it
     * the width follows `(1 - progress)^taper`, so a higher [taper] thins out sooner.
     */
    fun halfWidths(line: Centreline, w0: Float, w1: Float, taper: Float = 1f, inside: Rect? = null): FloatArray {
        val last = line.size - 1
        val exit = line.exitIndex(inside) / last.toFloat()
        return FloatArray(line.size) { i ->
            val t = i / last.toFloat()
            val progress = if (t <= exit) 0f else (t - exit) / (1 - exit)
            val remaining = (1 - progress).toDouble().pow(taper.toDouble()).toFloat()
            (w1 + (w0 - w1) * remaining) / 2
        }
    }

    /** The filled outline of [line] with the given half widths, each widened by [extra]. */
    fun outline(line: Centreline, halves: FloatArray, extra: Float = 0f): Path = Path().apply {
        val n = line.size
        for (i in 0 until n) {
            val h = (halves[i] + extra) * line.miter[i]
            val x = line.xs[i] + line.nx[i] * h
            val y = line.ys[i] + line.ny[i] * h
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        for (i in n - 1 downTo 0) {
            val h = (halves[i] + extra) * line.miter[i]
            lineTo(line.xs[i] - line.nx[i] * h, line.ys[i] - line.ny[i] * h)
        }
        close()
    }

    /** The size of a main topic box in [style] that fits padded content of size [w] by [h] inside its shape. */
    fun mainBoxSize(style: BranchStyle, w: Float, h: Float): Size = when (style) {
        BranchStyle.STANDARD, BranchStyle.BLURRED -> Size(w, h)
        BranchStyle.PLANT -> Size(w + 1.6f * h, h * 1.25f)
        BranchStyle.LIGHTNING -> Size(w + 0.6f * h, h)
        BranchStyle.STRAIGHT -> Size(w + 0.5f * h, h)
    }

    /** How far inside [r] the box outline lies at the box's vertical centre, where main branches end. */
    fun mainBoxInset(style: BranchStyle, r: Rect): Float = if (style == BranchStyle.LIGHTNING) r.height * 0.15f else 0f

    /** The outline of a main topic box in [style] filling [r]. */
    fun mainBoxPath(style: BranchStyle, r: Rect): Path = Path().apply {
        when (style) {
            BranchStyle.STANDARD, BranchStyle.BLURRED -> addRoundRect(r.toRectF(), 10f, 10f, Path.Direction.CW)
            BranchStyle.PLANT -> {
                // A leaf: pointed tips at the left and right, a gently bulging middle over the content.
                val h = r.height
                val content = r.width - 1.6f * h / 1.25f
                val inner = (r.width - content) / 2
                val shoulder = h * 0.06f
                moveTo(r.left, r.centerY)
                cubicTo(r.left + inner * 0.5f, r.top + h * 0.25f, r.left + inner * 0.7f, r.top + shoulder, r.left + inner, r.top + shoulder)
                quadTo(r.centerX, r.top - shoulder, r.right - inner, r.top + shoulder)
                cubicTo(r.right - inner * 0.7f, r.top + shoulder, r.right - inner * 0.5f, r.top + h * 0.25f, r.right, r.centerY)
                cubicTo(r.right - inner * 0.5f, r.bottom - h * 0.25f, r.right - inner * 0.7f, r.bottom - shoulder, r.right - inner, r.bottom - shoulder)
                quadTo(r.centerX, r.bottom + shoulder, r.left + inner, r.bottom - shoulder)
                cubicTo(r.left + inner * 0.7f, r.bottom - shoulder, r.left + inner * 0.5f, r.bottom - h * 0.25f, r.left, r.centerY)
                close()
            }
            BranchStyle.LIGHTNING -> {
                val skew = r.height * 0.3f
                moveTo(r.left + skew, r.top); lineTo(r.right, r.top); lineTo(r.right - skew, r.bottom); lineTo(r.left, r.bottom); close()
            }
            BranchStyle.STRAIGHT -> {
                val cut = r.height / 4
                moveTo(r.left + cut, r.top); lineTo(r.right - cut, r.top); lineTo(r.right, r.top + cut)
                lineTo(r.right, r.bottom - cut); lineTo(r.right - cut, r.bottom); lineTo(r.left + cut, r.bottom)
                lineTo(r.left, r.bottom - cut); lineTo(r.left, r.top + cut); close()
            }
        }
    }

    /** One leaf or tendril of the plant style, growing from ([x], [y]) in direction [angle]. */
    data class PlantPart(val leaf: Boolean, val x: Float, val y: Float, val angle: Float, val length: Float, val turn: Float, val thickness: Float) {
        val tipX: Float get() = x + cos(angle) * length
        val tipY: Float get() = y + sin(angle) * length
    }

    /**
     * Leaves and tendrils along the middle part of a main branch, alternating sides, sized from the local
     * width. Placement is driven by [seed], so a branch always grows the same plant; parts that would touch
     * the [start] or [end] box are left out.
     */
    fun plantParts(line: Centreline, halves: FloatArray, seed: Int, start: Rect?, end: Rect?): List<PlantPart> {
        val random = Random(seed)
        val last = line.size - 1
        val from = maxOf((last * 0.15f).toInt(), line.exitIndex(start) + 2)
        val to = (last * 0.85f).toInt()
        val spacing = 24f
        var travelled = spacing * 0.6f
        var count = 0
        val parts = ArrayList<PlantPart>()
        val clear = listOfNotNull(start, end).map { it.inset(4f) }
        for (i in from..to) {
            if (i > 0) travelled += hypot(line.xs[i] - line.xs[i - 1], line.ys[i] - line.ys[i - 1])
            if (travelled < spacing) continue
            travelled = 0f
            val side = if (count % 2 == 0) 1f else -1f
            val half = halves[i]
            val x = line.xs[i] + line.nx[i] * half * side
            val y = line.ys[i] + line.ny[i] * half * side
            val forward = atan2(-line.nx[i], line.ny[i])
            val jitter = (random.nextFloat() - 0.5f) * 0.3f
            val grow = 0.85f + 0.25f * random.nextFloat()
            // Mostly leaves, every third part a tendril.
            val leaf = count % 3 != 2
            count++
            val length = if (leaf) (half * 2.4f + 10f).coerceIn(10f, 22f) * grow else (half * 3f + 20f).coerceIn(20f, 36f) * grow
            val part = PlantPart(
                leaf, x, y, forward + side * (if (leaf) 0.75f else 0.9f) + jitter, length,
                turn = -side, thickness = (half * 0.35f).coerceIn(1.5f, 3f),
            )
            if (clear.any { it.contains(part.tipX, part.tipY) || it.contains(x, y) }) continue
            parts += part
        }
        return parts
    }

    /** A leaf of [length] at ([x], [y]) pointing in direction [angle] (radians). */
    fun leaf(x: Float, y: Float, angle: Float, length: Float): Path {
        val c = cos(angle)
        val s = sin(angle)
        val width = length * 0.42f
        fun px(along: Float, across: Float) = x + c * along - s * across
        fun py(along: Float, across: Float) = y + s * along + c * across
        return Path().apply {
            moveTo(x, y)
            cubicTo(px(length * 0.25f, width), py(length * 0.25f, width), px(length * 0.7f, width * 0.9f), py(length * 0.7f, width * 0.9f), px(length, 0f), py(length, 0f))
            cubicTo(px(length * 0.7f, -width * 0.9f), py(length * 0.7f, -width * 0.9f), px(length * 0.25f, -width), py(length * 0.25f, -width), x, y)
            close()
        }
    }

    /** A tendril leaving ([x], [y]) in direction [angle] that curls into a spiral, turning to side [turn] (±1). */
    fun tendril(x: Float, y: Float, angle: Float, length: Float, turn: Float): Path = Path().apply {
        moveTo(x, y)
        var px = x
        var py = y
        var a = angle
        val steps = 28
        val step = length / steps
        for (i in 1..steps) {
            // The curve tightens towards the end, ending in a small curl.
            val f = i / steps.toFloat()
            a += turn * (0.05f + 0.42f * f * f)
            val len = step * (1.15f - 0.6f * f)
            px += cos(a) * len
            py += sin(a) * len
            lineTo(px, py)
        }
    }

    /** A zigzag underline from ([x0], [y]) to ([x1], [y]) with peaks of [amplitude]. */
    fun zigzagUnderline(x0: Float, x1: Float, y: Float, amplitude: Float): Path = Path().apply {
        val period = 9f
        val teeth = (abs(x1 - x0) / period).roundToInt().coerceAtLeast(2)
        moveTo(x0, y)
        for (i in 1..teeth) {
            val x = x0 + (x1 - x0) * i / teeth
            lineTo(x, if (i == teeth) y else y + if (i % 2 == 1) -amplitude else amplitude)
        }
    }
}
