package com.smarttool.videodownloader.feature.media.presentation

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpCookie
import java.net.URI

/**
 * Owns the ExoPlayer and its `PlayerView` for one playback session.
 *
 * The View has to be created once and reused: rebuilding it on recomposition would
 * restart the surface and drop playback. The media route creates one holder per
 * navigation entry and releases it when that entry goes away.
 */
@OptIn(UnstableApi::class)
class MediaPlayerHolder(
    private val context: Context,
    url: Uri,
    private val headers: Map<String, String>,
    speed: Float,
    fillMode: Boolean,
    looping: () -> Boolean,
    onPlayingChanged: (Boolean) -> Unit,
) {

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setRenderersFactory(createRenderFactory())
        .setMediaSourceFactory(createMediaFactory(url.toString().startsWith("http")))
        .build()

    val playerView: PlayerView = PlayerView(context).apply {
        player = this@MediaPlayerHolder.player
        useController = false
        resizeMode = resizeModeFor(fillMode)
    }

    init {
        applyCookies(url)

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && looping()) {
                    player.seekTo(0)
                    player.play()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                onPlayingChanged(isPlaying)
            }
        })

        player.setMediaItem(MediaItem.fromUri(url))
        player.setPlaybackSpeed(speed)
        player.prepare()
        player.play()
    }

    fun togglePlayback() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun setFillMode(fill: Boolean) {
        playerView.resizeMode = resizeModeFor(fill)
    }

    fun release() {
        player.release()
    }

    private fun resizeModeFor(fill: Boolean): Int = if (fill) {
        AspectRatioFrameLayout.RESIZE_MODE_FILL
    } else {
        AspectRatioFrameLayout.RESIZE_MODE_FIT
    }

    /** Seeds the cookie store so authenticated streams resolve, as the View player did. */
    private fun applyCookies(url: Uri) {
        val cookies = headers["Cookie"]?.split(";").orEmpty()

        for (cookiePair in cookies) {
            val parts = cookiePair.split("=")
            val key = parts.firstOrNull() ?: continue
            val value = parts.lastOrNull() ?: continue

            runCatching {
                DEFAULT_COOKIE_MANAGER.cookieStore.add(URI(url.toString()), HttpCookie(key, value))
            }
        }
    }

    private fun createRenderFactory(): RenderersFactory =
        DefaultRenderersFactory(context.applicationContext)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                var decoderInfos = MediaCodecSelector.DEFAULT
                    .getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)

                if (MimeTypes.VIDEO_H264 == mimeType) {
                    // MediaCodecSelector.DEFAULT returns an unmodifiable list.
                    decoderInfos = ArrayList(decoderInfos)
                    decoderInfos.reverse()
                }

                decoderInfos
            }

    private fun createMediaFactory(isUrl: Boolean): DefaultMediaSourceFactory {
        if (!isUrl) {
            return DefaultMediaSourceFactory(context)
                .setDataSourceFactory(DefaultDataSource.Factory(context))
        }

        if (headers.isEmpty()) {
            val factory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setKeepPostFor302Redirects(true)

            return DefaultMediaSourceFactory(context).setDataSourceFactory(factory)
        }

        val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)
            .setUserAgent(headers["User-Agent"])
            .setDefaultRequestProperties(headers)

        return DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory)
    }

    companion object {
        val DEFAULT_COOKIE_MANAGER: CookieManager = CookieManager().apply {
            setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
        }
    }
}
