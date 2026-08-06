package com.smarttool.videodownloader.feature.browser.domain.usecase

import com.smarttool.videodownloader.feature.browser.domain.model.POPULAR_SITES
import com.smarttool.videodownloader.feature.browser.domain.model.PopularSite

class GetPopularSitesUseCase {
    operator fun invoke(): List<PopularSite> = POPULAR_SITES
}
