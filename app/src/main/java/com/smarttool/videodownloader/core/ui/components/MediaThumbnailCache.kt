package com.smarttool.videodownloader.core.ui.components

import android.graphics.Bitmap
import android.util.LruCache
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-wide in-memory cache for decoded [MediaThumbnail] bitmaps, keyed by file path.
 * Tab switches in MainRoute dispose and recreate the whole composable subtree (no nested
 * back stacks), so without this cache every switch back to a tab re-decodes every visible
 * thumbnail from disk, flashing the fallback icon in the meantime.
 *
 * Failures are remembered too. A lazy list disposes rows as they leave the viewport and
 * re-composes them on the way back, so a file that yields no frame would otherwise re-run
 * `ThumbnailUtils` *and* a `MediaMetadataRetriever` probe on every pass — the scroll jank that
 * a bitmap-only cache cannot prevent, because it never gets anything to store.
 */
object MediaThumbnailCache {
    private val cache = object : LruCache<String, Bitmap>(cacheSizeBytes()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    private val undecodable = Collections.synchronizedSet(mutableSetOf<String>())
    private val resolvedKinds = ConcurrentHashMap<String, MediaKind>()

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        undecodable.remove(key)
        cache.put(key, bitmap)
    }

    /**
     * Only meaningful for local files: a remote poster that failed once may just have been a
     * dropped request, and giving up on it for the rest of the process would be wrong.
     */
    fun markUndecodable(key: String) {
        if (!key.startsWith("http")) undecodable.add(key)
    }

    fun isUndecodable(key: String): Boolean = undecodable.contains(key)

    /** Remembers that a file claiming to be video turned out to carry no video track. */
    fun putResolvedKind(key: String, kind: MediaKind) {
        resolvedKinds[key] = kind
    }

    fun resolvedKind(key: String): MediaKind? = resolvedKinds[key]

    private fun cacheSizeBytes(): Int = (Runtime.getRuntime().maxMemory() / 8).toInt()
}
