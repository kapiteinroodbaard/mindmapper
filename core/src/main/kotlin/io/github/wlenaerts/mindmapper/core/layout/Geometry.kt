package io.github.wlenaerts.mindmapper.core.layout

data class Size(val width: Float, val height: Float)

data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2
    val centerY: Float get() = (top + bottom) / 2

    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom

    fun overlaps(other: Rect): Boolean =
        left < other.right && other.left < right && top < other.bottom && other.top < bottom

    fun union(other: Rect): Rect =
        Rect(minOf(left, other.left), minOf(top, other.top), maxOf(right, other.right), maxOf(bottom, other.bottom))

    fun inset(amount: Float): Rect = Rect(left - amount, top - amount, right + amount, bottom + amount)
}
