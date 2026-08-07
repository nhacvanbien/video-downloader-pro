package com.smarttool.videodownloader.feature.browser.presentation

/**
 * Receives image-download taps that originate inside the page, from the button
 * [WebAppInterface] injects over every image.
 */
interface DownloadImageHandler {
    fun onDownloadImageRequested(imageUrl: String)
}
