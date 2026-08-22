package com.smarttool.videodownloader.feature.downloads.presentation

import androidx.core.net.toUri
import java.util.Locale

/**
 * Best-effort "where did this come from" label for a download row.
 *
 * A completed row only keeps the direct media URL it was fetched from — the page it was found
 * on is not stored (see `GenericDownloadWorker.getTaskFromInput`, which builds the task from
 * `URL_KEY` alone). Those media URLs live on branded CDNs (`v16-webapp.tiktok.com`,
 * `scontent.cdninstagram.com`, `video.xx.fbcdn.net`), so the host still names the source in
 * practice — hence keyword matching on the host rather than an exact domain lookup.
 *
 * Falls back to the registrable domain name, capitalised, so an unrecognised host still reads
 * as a source instead of disappearing.
 */
private val SOURCE_KEYWORDS: List<Pair<String, String>> = listOf(
    "tiktok" to "TikTok",
    "douyin" to "Douyin",
    "fbcdn" to "Facebook",
    "fbsbx" to "Facebook",
    "fb.watch" to "Facebook",
    "facebook" to "Facebook",
    "cdninstagram" to "Instagram",
    "instagram" to "Instagram",
    "youtube" to "YouTube",
    "googlevideo" to "YouTube",
    "ytimg" to "YouTube",
    "youtu.be" to "YouTube",
    "twimg" to "X",
    "twitter" to "X",
    "dailymotion" to "Dailymotion",
    "vimeo" to "Vimeo",
    "pinimg" to "Pinterest",
    "pinterest" to "Pinterest",
    "redd" to "Reddit",
    "twitch" to "Twitch",
    "linkedin" to "LinkedIn",
    "snapchat" to "Snapchat",
    "telegram" to "Telegram",
    "likee" to "Likee",
    "kuaishou" to "Kuaishou",
    "bilibili" to "Bilibili",
    "soundcloud" to "SoundCloud",
    "vk.com" to "VK",
    "vkuser" to "VK",
    "ok.ru" to "OK",
    "tumblr" to "Tumblr",
    "imgur" to "Imgur",
    "9gag" to "9GAG",
)

fun downloadSourceLabel(url: String): String? {
    if (url.isBlank()) return null

    val host = runCatching { url.toUri().host }.getOrNull()?.lowercase(Locale.ROOT)
        ?: return null

    SOURCE_KEYWORDS.firstOrNull { (keyword, _) -> host.contains(keyword) }
        ?.let { (_, label) -> return label }

    // Take the rightmost label that actually names something. Counting a fixed number of labels
    // in from the right breaks on multi-part suffixes — "foo.com.vn" would be reported as "Com" —
    // and CDN hosts pad the left with equally meaningless labels ("scontent", "media", "v16").
    val name = host.split(".")
        .filter { it.isNotEmpty() }
        .lastOrNull { it.length > 2 && it !in GENERIC_HOST_LABELS }
        ?: return null

    return name.replaceFirstChar { it.titlecase(Locale.ROOT) }
}

/** Host labels that carry no brand: public suffixes, country codes, and CDN boilerplate. */
private val GENERIC_HOST_LABELS = setOf(
    "com", "net", "org", "gov", "edu", "int", "mil", "info", "biz", "name", "pro", "xyz",
    "app", "dev", "site", "web", "online", "store", "live", "link", "click", "top", "cyou",
    "www", "cdn", "cdns", "static", "media", "medias", "content", "contents", "scontent",
    "video", "videos", "vid", "vids", "img", "imgs", "image", "images", "stream", "streams",
    "assets", "asset", "files", "file", "data", "download", "downloads", "public", "upload",
    "uploads", "server", "servers", "host", "hosting", "proxy", "cache", "edge", "origin",
    "api", "www1", "www2", "www3",
    "vnm", "usa", "gbr", "deu", "fra", "jpn", "kor", "chn", "ind", "idn", "tha", "rus", "bra",
)

/** Uppercase container/format tag for a row's subtitle ("MP4", "MP3"), from a file name or extension. */
fun downloadFormatLabel(fileNameOrExt: String): String? {
    val ext = fileNameOrExt.substringAfterLast('.', "")
        .ifBlank { fileNameOrExt }
        .trim()
        .takeIf { it.isNotBlank() && it.length <= 5 && it.all(Char::isLetterOrDigit) }
        ?: return null

    return ext.uppercase(Locale.ROOT)
}

/** `mm:ss` (or `h:mm:ss`) for a duration badge; null when the duration is unknown. */
fun formatDurationBadge(durationMs: Long): String? {
    if (durationMs <= 0) return null

    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}
