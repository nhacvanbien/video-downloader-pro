package com.smarttool.videodownloader.core.network

import kotlin.random.Random

/**
 * TikTok's anti-bot gate is server-side and probabilistic: the same request can come
 * back with a solvable JS challenge (yt-dlp solves it locally, no native deps needed)
 * or a hard block that surfaces as the "attempting impersonation" warning followed by
 * an extraction error. Retrying a few times converts some of those hard blocks into
 * successes, mirroring what manually re-submitting the same link does.
 */
object TikTokExtractionSupport {

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

    private val CHROME_MAJOR_VERSION_RANGE = 118..131

    /**
     * yt-dlp's own default User-Agent (`std_headers` in its `utils/networking.py`) is a
     * module-level constant computed once and then reused for every request for the
     * lifetime of the embedded Python interpreter — which itself is a singleton kept alive
     * for the app process's whole lifetime, not re-created per call. Left alone, every
     * TikTok request the app ever makes — across all 5 retry attempts of one video and
     * across every other video in the same app session — carries the exact same
     * fingerprint. Generating a fresh one per attempt and forcing it via `--add-header`
     * (which overrides yt-dlp's default per YoutubeDL.py's `HTTPHeaderDict(std_headers,
     * http_headers)` merge) is what actually makes a "retry" a different request.
     */
    fun randomUserAgent(): String =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/${Random.nextInt(CHROME_MAJOR_VERSION_RANGE.first, CHROME_MAJOR_VERSION_RANGE.last + 1)}.0.0.0 Safari/537.36"

    private fun isRetriableFailure(message: String?): Boolean {
        val lower = message?.lowercase() ?: return false
        return RETRIABLE_MESSAGE_PATTERNS.any { lower.contains(it) }
    }

    /**
     * Runs [block], retrying on TikTok's known-transient failure signature. No-op passthrough
     * for every other host (called with attempt=1). Blocks the calling thread between
     * attempts — callers already run this off the main thread (worker/IO dispatcher). Delay
     * grows with each attempt (doubling) plus up to 500ms of jitter, since a fixed delay
     * tends to re-hit the same Akamai edge/DNS hiccup instead of giving it time to clear.
     * [block] receives the 1-based attempt number so the caller can vary the request (e.g.
     * rotate the User-Agent via [randomUserAgent]) — retrying the exact same request is
     * pointless against a block keyed on the request's fingerprint rather than pure chance.
     */
    fun <T> retryTikTokExtraction(
        host: String?,
        attempts: Int = 5,
        initialDelayMillis: Long = 2_000,
        block: (attempt: Int) -> T,
    ): T {
        if (!isTikTokHost(host)) return block(1)

        var delayMillis = initialDelayMillis
        for (attempt in 1..attempts) {
            try {
                return block(attempt)
            } catch (e: Throwable) {
                if (attempt == attempts || !isRetriableFailure(e.message)) throw e
                Thread.sleep(delayMillis + Random.nextLong(500))
                delayMillis *= 2
            }
        }
        error("unreachable")
    }
}
