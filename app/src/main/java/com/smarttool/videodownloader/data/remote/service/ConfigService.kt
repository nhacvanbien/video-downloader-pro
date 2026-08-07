package com.smarttool.videodownloader.data.remote.service

import com.smarttool.videodownloader.data.network.entity.SupportedPage
import retrofit2.http.GET

interface ConfigService {

    @GET("supported_pages.json")
    suspend fun getSupportedPages(): List<SupportedPage>
}
