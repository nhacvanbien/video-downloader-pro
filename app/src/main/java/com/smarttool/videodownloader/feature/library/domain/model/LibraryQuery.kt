package com.smarttool.videodownloader.feature.library.domain.model

data class LibraryQuery(
    val filter: MediaFilter = MediaFilter.All,
    val search: String = "",
    val sort: SortState = SortState.NAME,
)
