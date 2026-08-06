package com.smarttool.videodownloader.feature.browser.domain.model

/**
 * State the video-detection pipeline reports for the download affordance.
 *
 * Previously declared alongside the deleted `BrowserViewModel`. Distinct from the
 * presentation-layer `DownloadButtonUiState` enum, which is what the Compose screens
 * render — the hosts map from this type onto that one.
 */
abstract class DownloadButtonState
