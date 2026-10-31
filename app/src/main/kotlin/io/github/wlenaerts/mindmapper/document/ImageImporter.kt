package io.github.wlenaerts.mindmapper.document

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import io.github.wlenaerts.mindmapper.core.format.ImageScaling
import io.github.wlenaerts.mindmapper.core.model.TopicImage
import io.github.wlenaerts.mindmapper.core.model.newId
import java.io.ByteArrayOutputStream

/** An image ready to embed: the entry path, compressed bytes and the model reference. */
data class ImportedImage(val path: String, val bytes: ByteArray, val image: TopicImage)

/** Decodes a picked image, downscales it to at most 1600 px on the longest side and compresses it to WebP. */
class ImageImporter(private val resolver: ContentResolver) {

    fun import(uri: Uri): ImportedImage {
        val source = ImageDecoder.createSource(resolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val (w, h) = ImageScaling.embeddedSize(info.size.width, info.size.height)
            decoder.setTargetSize(w, h)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        return fromBitmap(bitmap)
    }

    companion object {
        fun fromBitmap(source: Bitmap): ImportedImage {
            val (w, h) = ImageScaling.embeddedSize(source.width, source.height)
            val bitmap = if (w != source.width || h != source.height) Bitmap.createScaledBitmap(source, w, h, true) else source
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, out)
            val path = "images/${newId()}.webp"
            val aspect = bitmap.width.toFloat() / bitmap.height
            return ImportedImage(path, out.toByteArray(), TopicImage(path, TopicImage.DEFAULT_WIDTH, aspect))
        }
    }
}
