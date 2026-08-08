package com.smarttool.videodownloader.core.scheduler

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

interface BaseSchedulers {
    val computation: CoroutineDispatcher
    val io: CoroutineDispatcher
    val videoService: CoroutineDispatcher
}

class BaseSchedulersImpl constructor() : BaseSchedulers {
    override val computation: CoroutineDispatcher = Dispatchers.Default
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val videoService: CoroutineDispatcher =
        Executors.newFixedThreadPool(16).asCoroutineDispatcher()
}
