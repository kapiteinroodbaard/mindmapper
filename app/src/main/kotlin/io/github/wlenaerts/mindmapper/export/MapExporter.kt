package io.github.wlenaerts.mindmapper.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import io.github.wlenaerts.mindmapper.core.layout.Rect
import io.github.wlenaerts.mindmapper.core.model.MindMap
import io.github.wlenaerts.mindmapper.core.view.ExportSize
import io.github.wlenaerts.mindmapper.core.view.ExportSizes
import io.github.wlenaerts.mindmapper.render.ImageSource
import io.github.wlenaerts.mindmapper.render.MapRenderer
import io.github.wlenaerts.mindmapper.render.MapTheme
import java.io.ByteArrayOutputStream
import java.io.OutputStream

enum class ExportFormat(val extension: String, val mimeType: String) {
    PDF("pdf", "application/pdf"),
    PNG("png", "image/png"),
}

/**
 * Renders complete maps off screen: PDF, PNG and the thumbnail stored in `.mindmap` files. Always uses
 * the light theme on white, whatever the app theme. Has its own renderer so it can run off the main thread.
 */
class MapExporter(context: Context) {
    private val renderer = MapRenderer(context.applicationContext)

    @Synchronized
    fun write(format: ExportFormat, map: MindMap, images: ImageSource, out: OutputStream) = when (format) {
        ExportFormat.PDF -> writePdf(map, images, out)
        ExportFormat.PNG -> writePng(map, images, out)
    }

    @Synchronized
    fun writePdf(map: MindMap, images: ImageSource, out: OutputStream) {
        val layout = renderer.layout(map)
        val size = ExportSizes.pdf(layout.bounds)
        val pdf = PdfDocument()
        try {
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(size.width, size.height, 1).create())
            drawInto(page.canvas, map, layout.bounds, size, ExportSizes.PDF_MARGIN, images)
            pdf.finishPage(page)
            pdf.writeTo(out)
        } finally {
            pdf.close()
        }
    }

    @Synchronized
    fun writePng(map: MindMap, images: ImageSource, out: OutputStream) {
        val bitmap = renderBitmap(map, images, ExportSizes.PNG_MAX_SIDE)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
    }

    /** A small PNG preview for the recent-files list. */
    @Synchronized
    fun thumbnail(map: MindMap, images: ImageSource): ByteArray {
        val bitmap = renderBitmap(map, images, THUMBNAIL_SIDE)
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
    }

    @Synchronized
    fun renderBitmap(map: MindMap, images: ImageSource, maxSide: Int): Bitmap {
        val layout = renderer.layout(map)
        var limit = maxSide
        while (true) {
            val size = ExportSizes.png(layout.bounds, limit)
            try {
                val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
                drawInto(Canvas(bitmap), map, layout.bounds, size, ExportSizes.PNG_MARGIN, images)
                return bitmap
            } catch (e: OutOfMemoryError) {
                // Very large maps on devices with little memory: retry at half the resolution.
                if (limit <= 1024) throw e
                limit /= 2
            }
        }
    }

    private fun drawInto(canvas: Canvas, map: MindMap, bounds: Rect, size: ExportSize, margin: Float, images: ImageSource) {
        canvas.drawColor(Color.WHITE)
        canvas.scale(size.scale, size.scale)
        canvas.translate(margin - bounds.left, margin - bounds.top)
        renderer.draw(canvas, map, renderer.layout(map), MapTheme.Light, images)
    }

    private companion object {
        const val THUMBNAIL_SIDE = 512
    }
}
