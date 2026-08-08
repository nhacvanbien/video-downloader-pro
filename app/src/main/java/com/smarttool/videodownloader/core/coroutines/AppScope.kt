package com.smarttool.videodownloader.core.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * A [CoroutineScope] that lives as long as the process.
 *
 * For work a screen asks for but must not own. Starting a download is the case that
 * matters: the browser tab requests it and is then closed, which clears that tab's
 * ViewModel store — on `viewModelScope` the enqueue is cancelled mid-flight and the
 * download is lost with nothing shown to the user.
 *
 * [SupervisorJob] so one failed operation cannot take the rest of the app's fire-and-
 * forget work down with it.
 */
class AppScope(
    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Default,
) : CoroutineScope
