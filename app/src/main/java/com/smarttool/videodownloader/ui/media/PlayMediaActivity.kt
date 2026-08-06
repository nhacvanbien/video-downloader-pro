package com.smarttool.videodownloader.ui.media

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
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
import com.smarttool.videodownloader.base.BaseComposeActivity
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.feature.media.presentation.MediaScreen
import com.smarttool.videodownloader.feature.media.presentation.MediaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.net.HttpCookie
import java.net.URI

@OptIn(UnstableApi::class)
class PlayMediaActivity : BaseComposeActivity() {

    private val viewModel: MediaViewModel by viewModel()

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = Uri.parse(intent.getStringExtra(VIDEO_URL).orEmpty())
        val title = intent.getStringExtra(VIDEO_NAME).orEmpty()
        val isDownloaded = intent.getBooleanExtra(IS_DOWNLOADED, false)
        val headers = parseHeaders()

        viewModel.load(title = title, showMoreAction = isDownloaded)

        val settings = viewModel.uiState.value
        applyOrientation(settings.fillMode)

        initPlayer(url, headers, settings.speed, settings.fillMode)
        observeProgress()

        setContent {
            AppTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                MediaScreen(
                    state = state,
                    playerView = playerView,
                    onBack = { finish() },
                    onMore = { /* detail sheet is opened from the library screens */ },
                    onPlayPause = ::togglePlayback,
                    onSeek = { player.seekTo(it) },
                    onCycleSpeed = { player.setPlaybackSpeed(viewModel.cycleSpeed()) },
                    onToggleLoop = { viewModel.toggleLooping() },
                    onToggleFill = ::toggleFillMode,
                )
            }
        }
    }

    private fun parseHeaders(): Map<String, String> {
        val raw = intent.getStringExtra(VIDEO_HEADERS) ?: return emptyMap()

        return runCatching {
            Json.parseToJsonElement(raw).jsonObject
                .map { it.key to it.value.toString() }
                .toMap()
        }.getOrDefault(emptyMap())
    }

    private fun initPlayer(
        url: Uri,
        headers: Map<String, String>,
        speed: Float,
        fillMode: Boolean,
    ) {
        applyCookies(url, headers)

        player = ExoPlayer.Builder(this)
            .setRenderersFactory(createRenderFactory())
            .setMediaSourceFactory(
                createMediaFactory(headers, url.toString().startsWith("http")),
            )
            .build()

        playerView = PlayerView(this).apply {
            player = this@PlayMediaActivity.player
            useController = false
            resizeMode = if (fillMode) {
                AspectRatioFrameLayout.RESIZE_MODE_FILL
            } else {
                AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        }

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && viewModel.uiState.value.looping) {
                    player.seekTo(0)
                    player.play()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModel.onPlaybackStateChanged(isPlaying)
            }
        })

        player.setMediaItem(MediaItem.fromUri(url))
        player.setPlaybackSpeed(speed)
        player.prepare()
        player.play()
    }

    /** Seeds the cookie store so authenticated streams resolve, as the View player did. */
    private fun applyCookies(url: Uri, headers: Map<String, String>) {
        val cookies = headers["Cookie"]?.split(";").orEmpty()

        for (cookiePair in cookies) {
            val parts = cookiePair.split("=")
            val key = parts.firstOrNull() ?: continue
            val value = parts.lastOrNull() ?: continue

            runCatching {
                DEFAULT_COOKIE_MANAGER?.cookieStore?.add(
                    URI(url.toString()),
                    HttpCookie(key, value),
                )
            }
        }
    }

    private fun observeProgress() {
        lifecycleScope.launch {
            while (true) {
                if (::player.isInitialized) {
                    viewModel.onProgress(
                        positionMs = player.currentPosition,
                        durationMs = player.duration.coerceAtLeast(0),
                    )
                }
                delay(PROGRESS_INTERVAL_MILLIS)
            }
        }
    }

    private fun togglePlayback() {
        if (player.isPlaying) player.pause() else player.play()
    }

    private fun toggleFillMode() {
        val fill = viewModel.toggleFillMode()

        playerView.resizeMode = if (fill) {
            AspectRatioFrameLayout.RESIZE_MODE_FILL
        } else {
            AspectRatioFrameLayout.RESIZE_MODE_FIT
        }

        applyOrientation(fill)
    }

    private fun applyOrientation(fillMode: Boolean) {
        requestedOrientation = if (fillMode) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun createRenderFactory(): RenderersFactory =
        DefaultRenderersFactory(applicationContext)
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

    private fun createMediaFactory(
        headers: Map<String, String>,
        isUrl: Boolean,
    ): DefaultMediaSourceFactory {
        if (!isUrl) {
            return DefaultMediaSourceFactory(this)
                .setDataSourceFactory(DefaultDataSource.Factory(this))
        }

        if (headers.isEmpty()) {
            val factory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setKeepPostFor302Redirects(true)

            return DefaultMediaSourceFactory(this).setDataSourceFactory(factory)
        }

        val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)
            .setUserAgent(headers["User-Agent"])
            .setDefaultRequestProperties(headers)

        return DefaultMediaSourceFactory(this).setDataSourceFactory(dataSourceFactory)
    }

    override fun onPause() {
        super.onPause()
        if (::player.isInitialized) player.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::player.isInitialized) player.release()
    }

    companion object {
        const val VIDEO_URL = "video_url"
        const val ITEM_TYPE = "item_type"
        const val VIDEO_HEADERS = "video_headers"
        const val VIDEO_NAME = "video_name"
        const val VIDEO_ITEM = "video_item"
        const val IS_DOWNLOADED = "is_downloaded"

        private const val PROGRESS_INTERVAL_MILLIS = 500L

        val DEFAULT_COOKIE_MANAGER: java.net.CookieManager? = java.net.CookieManager().apply {
            setCookiePolicy(java.net.CookiePolicy.ACCEPT_ORIGINAL_SERVER)
        }
    }
}
