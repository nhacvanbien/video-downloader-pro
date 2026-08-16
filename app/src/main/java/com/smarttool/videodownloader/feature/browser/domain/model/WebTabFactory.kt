package com.smarttool.videodownloader.feature.browser.domain.model

import android.util.Patterns
import com.smarttool.videodownloader.feature.tab.domain.model.TabModel

class WebTabFactory {
    companion object {

        const val SEARCH_URL = "https://www.google.com/search?q=%s"

        fun createWebTabFromInput(input: String, searchUrlTemplate: String = SEARCH_URL): WebTab {
            if (input.isEmpty()) return WebTab.HOME_TAB

            return when {
                input.startsWith("http://") || input.startsWith("https://") ->
                    WebTab(input, null)

                Patterns.WEB_URL.matcher(input).matches() ->
                    WebTab("https://$input", null)

                else -> WebTab(String.format(searchUrlTemplate, input), null)
            }
        }

        fun createTabModelFromInput(
            input: String,
            searchUrlTemplate: String = SEARCH_URL,
        ): TabModel {
            if (input.isEmpty()) return TabModel(url = input, isSelected = true)

            return when {
                input.startsWith("http://") || input.startsWith("https://") ->
                    TabModel(url = input, isSelected = true)

                Patterns.WEB_URL.matcher(input).matches() ->
                    TabModel(url = "https://$input", isSelected = true)

                else -> TabModel(url = String.format(searchUrlTemplate, input), isSelected = true)
            }
        }
    }
}
