package io.github.wlenaerts.mindmapper.document

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import io.github.wlenaerts.mindmapper.render.ImageSource

/** Decodes embedded images on demand and keeps recently drawn ones in memory. */
class ImageCache(private val bytesOf: (String) -> ByteArray?, maxBytes: Int = 64 * 1024 * 1024) : ImageSource {
    private val cache = object : LruCache<String, Bitmap>(maxBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    override fun bitmap(path: String): Bitmap? {
        cache.get(path)?.let { return it }
        val bytes = bytesOf(path) ?: return null
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        cache.put(path, bitmap)
        return bitmap
    }
}
