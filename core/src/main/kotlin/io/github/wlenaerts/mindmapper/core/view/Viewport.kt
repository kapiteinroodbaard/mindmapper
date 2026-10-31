package io.github.wlenaerts.mindmapper.core.view

import io.github.wlenaerts.mindmapper.core.layout.Rect

/** Maps layout units to screen pixels: `screen = layout * scale + offset`. */
data class Viewport(val scale: Float, val offsetX: Float, val offsetY: Float) {
    fun toLayoutX(screenX: Float): Float = (screenX - offsetX) / scale
    fun toLayoutY(screenY: Float): Float = (screenY - offsetY) / scale

    /** Zooms by [factor] around the screen point ([focusX], [focusY]), clamped to [minScale]..[maxScale]. */
    fun zoom(factor: Float, focusX: Float, focusY: Float, minScale: Float, maxScale: Float): Viewport {
        val newScale = (scale * factor).coerceIn(minScale, maxScale)
        val applied = newScale / scale
        return Viewport(newScale, focusX - (focusX - offsetX) * applied, focusY - (focusY - offsetY) * applied)
    }

    fun pan(dx: Float, dy: Float): Viewport = copy(offsetX = offsetX + dx, offsetY = offsetY + dy)

    companion object {
        /** The viewport that shows [bounds] centred in a [width]×[height] screen with [padding] pixels around it. */
        fun fit(bounds: Rect, width: Float, height: Float, padding: Float, minScale: Float, maxScale: Float): Viewport {
            val availableW = (width - 2 * padding).coerceAtLeast(1f)
            val availableH = (height - 2 * padding).coerceAtLeast(1f)
            val scale = minOf(availableW / bounds.width.coerceAtLeast(1f), availableH / bounds.height.coerceAtLeast(1f))
                .coerceIn(minScale, maxScale)
            return Viewport(scale, width / 2 - bounds.centerX * scale, height / 2 - bounds.centerY * scale)
        }
    }
}
