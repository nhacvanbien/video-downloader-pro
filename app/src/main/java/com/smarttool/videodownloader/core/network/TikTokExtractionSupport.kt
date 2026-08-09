package com.smarttool.videodownloader.core.network

import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlin.random.Random

/**
 * TikTok's anti-bot gate is server-side and probabilistic: the same request can come
 * back with a solvable JS challenge (yt-dlp solves it locally, no native deps needed)
 * or a hard block that surfaces as the "attempting impersonation" warning followed by
 * an extraction error. Retrying a few times converts some of those hard blocks into
 * successes, mirroring what manually re-submitting the same link does.
 */
object TikTokExtractionSupport {

    private const val DEVICE_ID_MIN = 7_250_000_000_000_000_000L
    private const val DEVICE_ID_MAX = 7_325_099_899_999_994_577L

    private val RETRIABLE_MESSAGE_PATTERNS = listOf(
        // TikTok/Akamai-side signatures (JS challenge or soft block resolves on retry)
        "impersonation",
        "unable to extract universal data",
        "video not available, status code",
        "unable to download api page",
        "unable to download webpage",
        "failed to parse json",
        // Transient network/DNS signatures — seen in the field as the actual root cause of
        // the above (e.g. Akamai edge/DNS hiccup), not a permanent block.
        "no address associated with hostname",
        "temporary failure in name resolution",
        "unable to resolve host",
        "connection reset",
        "connect timed out",
        "timeout",
    )

    fun isTikTokHost(host: String?): Boolean = host?.contains("tiktok.") == true

    /** Matches the range yt-dlp itself uses when it generates a device id (tiktok.py). */
    fun generateDeviceId(): String = Random.nextLong(DEVICE_ID_MIN, DEVICE_ID_MAX).toString()

    /**
     * Opts into yt-dlp's TikTok mobile-API extraction path (`_extract_aweme_app`), which
     * needs no TLS impersonation at all — only enabled when a device id is supplied.
     */
    fun YoutubeDLRequest.applyTikTokDeviceId(host: String?, deviceId: String) {
        if (isTikTokHost(host)) {
            addOption("--extractor-args", "tiktok:device_id=$deviceId")
        }
    }

    private fun isRetriableFailure(message: String?): Boolean {
        val lower = message?.lowercase() ?: return false
        return RETRIABLE_MESSAGE_PATTERNS.any { lower.contains(it) }
    }

    /**
     * Runs [block], retrying on TikTok's known-transient failure signature. No-op passthrough
     * for every other host. Blocks the calling thread between attempts — callers already run
     * this off the main thread (worker/IO dispatcher). Delay grows with each attempt (doubling)
     * plus up to 500ms of jitter, since a fixed delay tends to re-hit the same Akamai edge/DNS
     * hiccup instead of giving it time to clear.
     */
    fun <T> retryTikTokExtraction(
        host: String?,
        attempts: Int = 5,
        initialDelayMillis: Long = 2_000,
        block: () -> T,
    ): T {
        if (!isTikTokHost(host)) return block()

        var delayMillis = initialDelayMillis
        for (attempt in 1..attempts) {
            try {
                return block()
            } catch (e: Throwable) {
                if (attempt == attempts || !isRetriableFailure(e.message)) throw e
                Thread.sleep(delayMillis + Random.nextLong(500))
                delayMillis *= 2
            }
        }
        error("unreachable")
    }
}
