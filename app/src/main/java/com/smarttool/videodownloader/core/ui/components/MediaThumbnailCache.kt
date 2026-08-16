package com.smarttool.videodownloader.core.ui.components

import android.graphics.Bitmap
import android.util.LruCache

/**
 * Process-wide in-memory cache for decoded [MediaThumbnail] bitmaps, keyed by file path.
 * Tab switches in MainRoute dispose and recreate the whole composable subtree (no nested
 * back stacks), so without this cache every switch back to a tab re-decodes every visible
 * thumbnail from disk, flashing the fallback icon in the meantime.
 */
object MediaThumbnailCache {
    private val cache = object : LruCache<String, Bitmap>(cacheSizeBytes()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    private fun cacheSizeBytes(): Int = (Runtime.getRuntime().maxMemory() / 8).toInt()
}
