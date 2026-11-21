package io.github.wlenaerts.mindmapper.render

import android.graphics.Path
import io.github.wlenaerts.mindmapper.core.layout.Rect
import io.github.wlenaerts.mindmapper.core.layout.Size
import io.github.wlenaerts.mindmapper.core.model.BranchStyle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
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
    private const val TIP_STEPS = 64
    /** Corner radius of the standard and blurred main boxes. */
    private const val BOX_RADIUS = 10f
    /** Extra depth and height so that anti-aliased edges of a branch end stay under its box. */
    private const val JOIN_OVERLAP = 1.5f
    /** Distance between sample points of straight stretches, so the taper stays smooth. */
    private const val SAMPLE_SPACING = 3f
    private const val ARC_STEPS = 12
    private const val LOOP_STEPS = 120
    /** Corner radius of the road style's main box. */
    private const val ROAD_BOX_RADIUS = 6f
    /** Corner radius of the rope style's main box. */
    private const val ROPE_BOX_RADIUS = 10f

    /**
     * The smooth S-curve used by the standard and plant styles, leaving and arriving horizontally, sampled in
     * [steps] pieces.
     */
    fun curve(x0: Float, y0: Float, x3: Float, y3: Float, steps: Int = CURVE_STEPS): Centreline {
        val mx = (x0 + x3) / 2
        val xs = FloatArray(steps + 1)
        val ys = FloatArray(steps + 1)
        val nx = FloatArray(steps + 1)
        val ny = FloatArray(steps + 1)
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val u = 1 - t
            xs[i] = u * u * u * x0 + 3 * u * u * t * mx + 3 * u * t * t * mx + t * t * t * x3
            ys[i] = u * u * u * y0 + 3 * u * u * t * y0 + 3 * u * t * t * y3 + t * t * t * y3
            val dx = 3 * u * u * (mx - x0) + 3 * t * t * (x3 - mx)
            val dy = 6 * u * t * (y3 - y0)
            val len = hypot(dx, dy).takeIf { it > 0.0001f } ?: 1f
            nx[i] = -dy / len
            ny[i] = dx / len
        }
        return Centreline(xs, ys, nx, ny, FloatArray(steps + 1) { 1f })
    }

    /** Enough curve steps for decorations that follow the line closely: one about every three units. */
    fun denseSteps(x0: Float, y0: Float, x1: Float, y1: Float): Int =
        ((abs(x1 - x0) + abs(y1 - y0)) / SAMPLE_SPACING).roundToInt().coerceIn(CURVE_STEPS, 400)

    /**
     * The steel style's line: straight pieces with sharp corners. It runs level out of [inside] (the box it
     * starts in), slants across the height difference, and arrives level; the slant is 45 degrees when there
     * is room for it, steeper when there is not.
     */
    fun angular(x0: Float, y0: Float, x1: Float, y1: Float, inside: Rect?): Centreline {
        val sx = if (x1 >= x0) 1f else -1f
        val run = startRun(x0, x1, inside)
        val free = abs(x1 - x0) - run
        val dy = abs(y1 - y0)
        if (dy < 1f || free < 4f) return LineBuilder(x0, y0).apply { lineTo(x1, y1) }.build()
        val slant = minOf(dy, free * 0.6f)
        val c1 = x0 + sx * (run + (free - slant) / 2)
        return LineBuilder(x0, y0).apply { lineTo(c1, y0); lineTo(c1 + sx * slant, y1); lineTo(x1, y1) }.build()
    }

    /**
     * The road style's line: level, then vertical halfway between [inside] and the end, then level again,
     * with rounded elbows of up to [radius]. Without a height difference it is a straight road.
     */
    fun orthogonal(x0: Float, y0: Float, x1: Float, y1: Float, radius: Float, inside: Rect?): Centreline {
        val sx = if (x1 >= x0) 1f else -1f
        val sy = if (y1 >= y0) 1f else -1f
        val from = x0 + sx * startRun(x0, x1, inside)
        val dy = abs(y1 - y0)
        if (dy < 1f || abs(x1 - from) < 4f) return LineBuilder(x0, y0).apply { lineTo(x1, y1) }.build()
        val xm = (from + x1) / 2
        val r = minOf(radius, dy / 2, abs(xm - from), abs(x1 - xm))
        val half = PI.toFloat() / 2
        return LineBuilder(x0, y0).apply {
            lineTo(xm - sx * r, y0)
            arc(xm - sx * r, y0 + sy * r, r, -sy * half, if (sx > 0) 0f else -sy * 2 * half)
            lineTo(xm, y1 - sy * r)
            arc(xm + sx * r, y1 - sy * r, r, if (sx > 0) sy * 2 * half else 0f, sy * half)
            lineTo(x1, y1)
        }.build()
    }

    /** A straight line sampled about every three units, for decorations that follow the line closely. */
    fun sampledLine(x0: Float, y0: Float, x1: Float, y1: Float): Centreline = LineBuilder(x0, y0).apply { lineTo(x1, y1) }.build()

    /** How far a line from [x0] towards [x1] runs inside [inside] before it leaves it horizontally. */
    private fun startRun(x0: Float, x1: Float, inside: Rect?): Float {
        val r = inside ?: return 0f
        return (if (x1 >= x0) r.right - x0 else x0 - r.left).coerceIn(0f, abs(x1 - x0))
    }

    /**
     * [line] made to wave sideways by [amplitude] every [wavelength] units. The wave starts where the line
     * leaves [inside], swells, and fades out again before the end so the line arrives straight.
     */
    fun wave(line: Centreline, amplitude: Float, wavelength: Float, inside: Rect? = null): Centreline {
        val lengths = lengths(line)
        val total = lengths.last()
        val from = lengths[line.exitIndex(inside)]
        val xs = FloatArray(line.size)
        val ys = FloatArray(line.size)
        for (i in 0 until line.size) {
            val f = if (total > from) (lengths[i] - from) / (total - from) else 0f
            val envelope = if (f <= 0f) 0f else sin(PI.toFloat() * minOf(1f, f * 1.15f))
            val a = amplitude * envelope * sin(2 * PI.toFloat() * lengths[i] / wavelength)
            xs[i] = line.xs[i] + line.nx[i] * a
            ys[i] = line.ys[i] + line.ny[i] * a
        }
        return Centreline.of(xs, ys)
    }

    /** The edge of the rope style's main box [r], as a closed line for the rope drawn around it. */
    fun ropeBoxLine(r: Rect): Centreline = roundedRectLine(r, ROPE_BOX_RADIUS)

    /** The outline of a rounded rectangle as a closed line, starting and ending at the top just past the corner. */
    private fun roundedRectLine(r: Rect, radius: Float): Centreline {
        val half = PI.toFloat() / 2
        return LineBuilder(r.left + radius, r.top).apply {
            lineTo(r.right - radius, r.top)
            arc(r.right - radius, r.top + radius, radius, -half, 0f)
            lineTo(r.right, r.bottom - radius)
            arc(r.right - radius, r.bottom - radius, radius, 0f, half)
            lineTo(r.left + radius, r.bottom)
            arc(r.left + radius, r.bottom - radius, radius, half, 2 * half)
            lineTo(r.left, r.top + radius)
            arc(r.left + radius, r.top + radius, radius, 2 * half, 3 * half)
            lineTo(r.left + radius + 4f, r.top)
        }.build()
    }

    /** A line with half widths, after a looping was put into it; [split] is a point halfway round the loop. */
    class Looped(val line: Centreline, val halves: FloatArray, val split: Int)

    /**
     * [line] with one full looping of [radius], starting [start] units along it, curling to side [dir] (±1 along
     * the normal). The rope comes back to cross itself just past where the loop began and goes on in the same
     * direction.
     */
    fun looping(line: Centreline, halves: FloatArray, start: Float, radius: Float, dir: Float): Looped {
        val walker = Walker(line, halves)
        val span = radius * 0.9f
        val xs = ArrayList<Float>()
        val ys = ArrayList<Float>()
        val hs = ArrayList<Float>()
        var split = 0
        fun add(p: Walker.Sample, across: Float = 0f) {
            xs += p.x + p.nx * across; ys += p.y + p.ny * across; hs += p.h
        }
        var s = 0f
        while (s < walker.total) {
            if (s >= start && s < start + span) {
                val u = (s - start) / span
                val turn = 2 * PI.toFloat() * u
                add(walker.at(start + span * u + radius * sin(turn)), dir * radius * (1 - cos(turn)))
                if (split == 0 && u >= 0.5f) split = xs.size - 1
                s += span / LOOP_STEPS
            } else {
                add(walker.at(s))
                s += SAMPLE_SPACING
            }
        }
        add(walker.at(walker.total))
        return Looped(Centreline.of(xs.toFloatArray(), ys.toFloatArray()), hs.toFloatArray(), if (split == 0) xs.size - 1 else split)
    }

    /** Arc lengths along [line] up to each point. */
    fun lengths(line: Centreline): FloatArray = FloatArray(line.size).also {
        for (i in 1 until line.size) it[i] = it[i - 1] + hypot(line.xs[i] - line.xs[i - 1], line.ys[i] - line.ys[i - 1])
    }

    /** Looks up points along [line] by arc length, with their normals and half widths from [halves]. */
    class Walker(val line: Centreline, private val halves: FloatArray) {
        val lengths = lengths(line)
        val total: Float get() = lengths.last()

        class Sample(val x: Float, val y: Float, val nx: Float, val ny: Float, val h: Float) {
            val tx: Float get() = ny
            val ty: Float get() = -nx
        }

        /** The first point at least [s] along the line. */
        fun indexAt(s: Float): Int = lengths.indexOfFirst { it >= s }.let { if (it < 0) line.size - 1 else it }

        fun at(s: Float): Sample {
            val i = indexAt(s).coerceAtLeast(1)
            val a = lengths[i - 1]
            val f = ((s - a) / (lengths[i] - a).coerceAtLeast(0.0001f)).coerceIn(0f, 1f)
            fun lerp(v: FloatArray) = v[i - 1] + (v[i] - v[i - 1]) * f
            return Sample(lerp(line.xs), lerp(line.ys), lerp(line.nx), lerp(line.ny), lerp(halves))
        }
    }

    /** Builds a line from straight stretches (sampled finely) and arcs; corners between stretches stay sharp. */
    private class LineBuilder(x: Float, y: Float) {
        private val xs = arrayListOf(x)
        private val ys = arrayListOf(y)

        fun lineTo(x: Float, y: Float) {
            val x0 = xs.last()
            val y0 = ys.last()
            val length = hypot(x - x0, y - y0)
            if (length < 0.01f) return
            val n = (length / SAMPLE_SPACING).roundToInt().coerceAtLeast(1)
            for (k in 1..n) {
                xs += x0 + (x - x0) * k / n
                ys += y0 + (y - y0) * k / n
            }
        }

        /** An arc around ([cx], [cy]) from angle [from] to [to] (radians, y pointing down). */
        fun arc(cx: Float, cy: Float, r: Float, from: Float, to: Float) {
            if (r < 0.5f) return lineTo(cx + r * cos(to), cy + r * sin(to))
            for (k in 1..ARC_STEPS) {
                val a = from + (to - from) * k / ARC_STEPS
                xs += cx + r * cos(a)
                ys += cy + r * sin(a)
            }
        }

        fun build() = Centreline.of(xs.toFloatArray(), ys.toFloatArray())
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

    /**
     * The filled outline of [line] with the given half widths, each widened by [extra], from point [from] up to
     * and including point [to].
     */
    fun outline(line: Centreline, halves: FloatArray, extra: Float = 0f, from: Int = 0, to: Int = line.size - 1): Path = Path().apply {
        for (i in from..to) {
            val h = (halves[i] + extra) * line.miter[i]
            val x = line.xs[i] + line.nx[i] * h
            val y = line.ys[i] + line.ny[i] * h
            if (i == from) moveTo(x, y) else lineTo(x, y)
        }
        for (i in to downTo from) {
            val h = (halves[i] + extra) * line.miter[i]
            lineTo(line.xs[i] - line.nx[i] * h, line.ys[i] - line.ny[i] * h)
        }
        close()
    }

    /** One side of [line]'s outline: [side] 1 or -1 times the half widths, from point [from] to [to]. */
    fun edge(line: Centreline, halves: FloatArray, side: Float, from: Int = 0, to: Int = line.size - 1): Path = Path().apply {
        for (i in from..to) {
            val h = halves[i] * line.miter[i] * side
            if (i == from) moveTo(line.xs[i] + line.nx[i] * h, line.ys[i] + line.ny[i] * h) else lineTo(line.xs[i] + line.nx[i] * h, line.ys[i] + line.ny[i] * h)
        }
    }

    /** The size of a main topic box in [style] that fits padded content of size [w] by [h] inside its shape. */
    fun mainBoxSize(style: BranchStyle, w: Float, h: Float): Size = when (style) {
        BranchStyle.STANDARD, BranchStyle.BLURRED -> Size(w, h)
        BranchStyle.PLANT -> Size(w + 1.6f * h, h * 1.25f)
        BranchStyle.LIGHTNING -> Size(w + 0.6f * h, h)
        BranchStyle.STRAIGHT -> Size(w + 0.5f * h, h)
        BranchStyle.STEEL, BranchStyle.ROAD -> Size(w, h)
        // Room for the rope around the edge.
        BranchStyle.ROPE -> Size(w + 4f, h + 4f)
        BranchStyle.SNAKE -> Size(w + 0.5f * h, h)
        BranchStyle.ARROW -> Size(w + 2 * arrowPoint(h), h)
    }

    /** How far the arrow-shaped box's point sticks out. */
    private fun arrowPoint(h: Float): Float = (h * 0.37f).coerceAtMost(20f)

    /** How deep the notch in the arrow-shaped box's back is. */
    private fun arrowNotch(h: Float): Float = arrowPoint(h) * 0.55f

    /**
     * How far inside the left or right side of [r] a branch of half width [half] must end (or start) at the
     * box's vertical centre, so that the box covers the branch's flat end on both sides of its centreline.
     * [outer] is the side facing away from the central topic; only the arrow-shaped box differs per side.
     */
    fun mainBoxJoin(style: BranchStyle, r: Rect, half: Float, outer: Boolean = false): Float {
        val h = r.height
        val need = half + JOIN_OVERLAP
        val depth = when (style) {
            BranchStyle.STANDARD, BranchStyle.BLURRED -> roundedJoin(BOX_RADIUS, h, need)
            BranchStyle.ROPE -> roundedJoin(ROPE_BOX_RADIUS, h, need)
            BranchStyle.ROAD -> roundedJoin(ROAD_BOX_RADIUS, h, need)
            BranchStyle.SNAKE -> roundedJoin(h / 2, h, need)
            BranchStyle.STEEL -> 0f
            BranchStyle.STRAIGHT -> (need - (h / 2 - h / 4)).coerceAtLeast(0f)
            // The side slants by 0.3 h over the full height, so it lies 0.15 h in at the centre.
            BranchStyle.LIGHTNING -> h * 0.15f + 0.3f * need
            BranchStyle.PLANT -> plantTipDepth(r, need)
            // Into the point, as far as it is wide enough; at the back, the arrow's tip ends in the notch.
            BranchStyle.ARROW -> if (outer) arrowPoint(h) * need / (h / 2) else arrowNotch(h)
        }
        return depth + JOIN_OVERLAP
    }

    /** Inside the straight part of a side with corners of [radius], or as deep as the rounded corner needs. */
    private fun roundedJoin(radius: Float, h: Float, need: Float): Float {
        val beyond = (need - (h / 2 - radius)).coerceIn(0f, radius)
        return radius - sqrt(radius * radius - beyond * beyond)
    }

    /** How far from its tip the leaf-shaped box is at least [half] high on each side of its centre. */
    private fun plantTipDepth(r: Rect, half: Float): Float {
        val h = r.height
        val inner = plantInner(r)
        val shoulder = h * 0.06f
        // The tip curve, measured from the tip: x along the box, y as height above the centre.
        val x1 = inner * 0.5f; val y1 = h * 0.25f
        val x2 = inner * 0.7f; val y2 = h / 2 - shoulder
        val x3 = inner; val y3 = h / 2 - shoulder
        for (i in 0..TIP_STEPS) {
            val t = i / TIP_STEPS.toFloat()
            val u = 1 - t
            val y = 3 * u * u * t * y1 + 3 * u * t * t * y2 + t * t * t * y3
            if (y >= half) return 3 * u * u * t * x1 + 3 * u * t * t * x2 + t * t * t * x3
        }
        return inner
    }

    /** The length of each pointed end of the leaf-shaped box in [r]. */
    private fun plantInner(r: Rect): Float = 0.8f * r.height / 1.25f

    /** The outline of a main topic box in [style] filling [r]; an arrow-shaped box points [right] or left. */
    fun mainBoxPath(style: BranchStyle, r: Rect, right: Boolean = true): Path = Path().apply {
        when (style) {
            BranchStyle.STANDARD, BranchStyle.BLURRED -> addRoundRect(r.toRectF(), BOX_RADIUS, BOX_RADIUS, Path.Direction.CW)
            BranchStyle.STEEL -> addRect(r.toRectF(), Path.Direction.CW)
            BranchStyle.ROPE -> addRoundRect(r.toRectF(), ROPE_BOX_RADIUS, ROPE_BOX_RADIUS, Path.Direction.CW)
            BranchStyle.ROAD -> addRoundRect(r.toRectF(), ROAD_BOX_RADIUS, ROAD_BOX_RADIUS, Path.Direction.CW)
            BranchStyle.SNAKE -> addRoundRect(r.toRectF(), r.height / 2, r.height / 2, Path.Direction.CW)
            BranchStyle.ARROW -> {
                val point = arrowPoint(r.height)
                val notch = arrowNotch(r.height)
                if (right) {
                    moveTo(r.left, r.top); lineTo(r.right - point, r.top); lineTo(r.right, r.centerY)
                    lineTo(r.right - point, r.bottom); lineTo(r.left, r.bottom); lineTo(r.left + notch, r.centerY)
                } else {
                    moveTo(r.right, r.top); lineTo(r.left + point, r.top); lineTo(r.left, r.centerY)
                    lineTo(r.left + point, r.bottom); lineTo(r.right, r.bottom); lineTo(r.right - notch, r.centerY)
                }
                close()
            }
            BranchStyle.PLANT -> {
                // A leaf: pointed tips at the left and right, a gently bulging middle over the content.
                val h = r.height
                val inner = plantInner(r)
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
