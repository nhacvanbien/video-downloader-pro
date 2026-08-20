package com.smarttool.videodownloader.core.network

import kotlinx.coroutines.CancellationException
import kotlin.random.Random

/**
 * TikTok's anti-bot gate is server-side and probabilistic: the same request can come
 * back with a solvable JS challenge (yt-dlp solves it locally, no native deps needed)
 * or a hard block that surfaces as the "attempting impersonation" warning followed by
 * an extraction error. Retrying a few times converts some of those hard blocks into
 * successes, mirroring what manually re-submitting the same link does.
 */
object TikTokExtractionSupport {

    // TikTok/Akamai-side block signatures (JS challenge or soft block resolves on retry).
    // Specific to TikTok's own anti-bot gate — a plain re-request against another host
    // failing with one of these messages wouldn't mean the same thing.
    private val TIKTOK_BLOCK_PATTERNS = listOf(
        "impersonation",
        "unable to extract universal data",
        "video not available, status code",
        "unable to download api page",
        "unable to download webpage",
        "failed to parse json",
    )

    // Transient network/DNS signatures — seen in the field as the actual root cause behind
    // some of the TikTok block signatures above (e.g. Akamai edge/DNS hiccup), but not
    // TikTok-specific at all: any host's extraction can hit a DNS or socket hiccup, so
    // these get a couple of retries regardless of host (see [retryTikTokExtraction]).
    private val NETWORK_TRANSIENT_PATTERNS = listOf(
        "no address associated with hostname",
        "temporary failure in name resolution",
        "unable to resolve host",
        "connection reset",
        "connect timed out",
        "timeout",
    )

    /** Retry budget for a non-TikTok host hitting a plain network hiccup — see [retryTikTokExtraction]. */
    private const val NETWORK_RETRY_ATTEMPTS = 2

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

    private fun matches(message: String?, patterns: List<String>): Boolean {
        val lower = message?.lowercase() ?: return false
        return patterns.any { lower.contains(it) }
    }

    /**
     * Runs [block], retrying on a known-transient failure signature. TikTok gets the full
     * treatment — up to [attempts] tries against both its own block signatures and plain
     * network hiccups, with [block] getting a fresh User-Agent to try each time via the
     * attempt number it's called with (see [randomUserAgent]). Every other host still gets
     * [NETWORK_RETRY_ATTEMPTS] tries, but only against the plain network patterns — a
     * DNS/timeout/reset hiccup isn't TikTok-specific, so it shouldn't only get retried
     * there. Blocks the calling thread between attempts — callers already run this off the
     * main thread (worker/IO dispatcher). Delay grows with each attempt (doubling) plus up
     * to 500ms of jitter, since a fixed delay tends to re-hit the same Akamai edge/DNS
     * hiccup instead of giving it time to clear.
     *
     * @param isActive Polled before every attempt (including the first) and again after
     * each backoff sleep. Cancelling the coroutine that called this only stops it at its
     * next suspension point, which this blocking loop never reaches — without this check,
     * a caller that killed the current attempt's process (e.g. via `destroyProcessById`,
     * because the user backed out of the page) would see that as just another retriable
     * failure and spawn a brand new process under the same taskId that nothing can
     * reference to kill again. Defaults to always-active for callers that don't track one.
     */
    fun <T> retryTikTokExtraction(
        host: String?,
        attempts: Int = 5,
        initialDelayMillis: Long = 2_000,
        isActive: () -> Boolean = { true },
        block: (attempt: Int) -> T,
    ): T {
        val isTikTok = isTikTokHost(host)
        val effectiveAttempts = if (isTikTok) attempts else NETWORK_RETRY_ATTEMPTS

        var delayMillis = initialDelayMillis
        for (attempt in 1..effectiveAttempts) {
            if (!isActive()) throw CancellationException("retryTikTokExtraction cancelled before attempt $attempt")
            try {
                return block(attempt)
            } catch (e: Throwable) {
                val retriable = if (isTikTok) {
                    matches(e.message, TIKTOK_BLOCK_PATTERNS) || matches(e.message, NETWORK_TRANSIENT_PATTERNS)
                } else {
                    matches(e.message, NETWORK_TRANSIENT_PATTERNS)
                }
                if (attempt == effectiveAttempts || !retriable) throw e
                if (!isActive()) throw CancellationException("retryTikTokExtraction cancelled after attempt $attempt")
                Thread.sleep(delayMillis + Random.nextLong(500))
                delayMillis *= 2
            }
        }
        error("unreachable")
    }
}
