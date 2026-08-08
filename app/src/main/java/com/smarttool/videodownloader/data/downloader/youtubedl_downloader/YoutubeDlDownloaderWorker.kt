package com.smarttool.videodownloader.data.downloader.youtubedl_downloader

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.core.net.toUri
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.core.network.Proxy
import com.smarttool.videodownloader.data.downloader.generic_downloader.GenericDownloader
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState
import com.smarttool.videodownloader.data.downloader.generic_downloader.workers.GenericDownloadWorkerWrapper
import com.smarttool.videodownloader.data.network.entity.VideoFormatEntity
import com.smarttool.videodownloader.feature.browser.domain.CookieUtils
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.File
import java.util.Date
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.coroutines.resume

class YoutubeDlDownloaderWorker(appContext: Context, workerParams: WorkerParameters) :
    GenericDownloadWorkerWrapper(appContext, workerParams) {
    companion object {
        var isCanceled = false

        const val IS_FINISHED_DOWNLOAD_ACTION_ERROR_KEY = "IS_FINISHED_DOWNLOAD_ACTION_ERROR_KEY"
        const val STOP_SAVE_ACTION = "STOP_AND_SAVE"
        const val DOWNLOAD_FILENAME_KEY = "download_filename"
        const val IS_FINISHED_DOWNLOAD_ACTION_KEY = "action"
        private const val TRESHOLD = 10 * 1024 * 1024
    }

    private lateinit var tmpFile: File
    private var isLiveCounter: Int = 0
    private var isDownloadOk: Boolean = false
    private var isDownloadJustStarted: Boolean = false
    private var monitorProcess: Job? = null

    private var progressCached = 0

    private val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var disposable: Job? = null

    private var progressDisposable: Job? = null

    private var cookieFile: File? = null

    private var lastTmpDirSize = 0L

    @Volatile
    var time = 0L

    override fun afterDone() {
        monitorProcess?.cancel()
        workerScope.cancel()
    }

    override fun handleAction(
        action: String, task: VideoTaskItem, headers: Map<String, String>, isFileRemove: Boolean
    ) {
        when (action) {
            GenericDownloader.DownloaderActions.DOWNLOAD -> {
                isCanceled = false
                startDownload(task, headers)
            }

            GenericDownloader.DownloaderActions.CANCEL -> {
                isCanceled = true
                cancelDownload(task, headers)
            }

            GenericDownloader.DownloaderActions.PAUSE -> {
                isCanceled = false
                pauseDownload(task, headers)
            }

            GenericDownloader.DownloaderActions.RESUME -> {
                isCanceled = false
                resumeDownload(task, headers)
            }

            STOP_SAVE_ACTION -> {
                stopAndSave(task)
            }
        }
    }

    private fun stopAndSave(task: VideoTaskItem) {
        Timber.d("stopAndSave: task: $task")
        val taskId = inputData.getString(GenericDownloader.DOWNLOAD_ID_KEY)

        if (taskId != null) {
            YoutubeDL.getInstance().destroyProcessById(taskId)

            val partsFolder = File(
                "${fileUtil.tmpDir}/$taskId"
            )
            val firstPart = partsFolder.listFiles()?.firstOrNull()

            val dist = File(fileUtil.folderDir.absolutePath, "${task.title}.mp4")

            if (firstPart != null && firstPart.exists()) {
                try {
                    val moved =
                        fileUtil.moveMedia(applicationContext, firstPart.toUri(), dist.toUri())
                    if (moved) {
                        finishWork(task.also { it.taskState = VideoTaskState.SUCCESS })
                    } else {
                        finishWork(task.also { it.taskState = VideoTaskState.ERROR })
                    }
                } catch (e: Throwable) {
                    finishWork(task.also { it.taskState = VideoTaskState.ERROR })
                }
            } else {
                finishWork(task.also { it.taskState = VideoTaskState.ERROR })
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun startDownload(
        task: VideoTaskItem, headers: Map<String, String>, isContinue: Boolean = false
    ) {
        val taskId = inputData.getString(GenericDownloader.DOWNLOAD_ID_KEY)!!

        val rawHeaders =
            GenericDownloader.loadHeadersStringFromSharedPreferences(applicationContext, taskId)

        val decompressedRaw = rawHeaders?.let { GenericDownloader.decompressString(it) }

        val decodedHeadersString = String(Base64.decode(decompressedRaw, Base64.DEFAULT))
        val vFormat = Gson().fromJson(decodedHeadersString, VideoFormatEntity::class.java)

        val url = inputData.getString(
            GenericDownloader.ORIGIN_KEY
        ) ?: throw Throwable("URL is NULL")

        Timber.i("Start youtube-dl download: taskId=$taskId format=${vFormat.formatId} url=$url")

        val name = task.title
        val downloadDir = fileUtil.folderDir

        notificationsHelper.hideNotification(taskId.hashCode())
        notificationsHelper.hideNotification(taskId.hashCode() + 1)

        val request = YoutubeDLRequest(url)

        cookieFile = CookieUtils.addCookiesToRequest(
            applicationContext, url, request, inputData.getString(GenericDownloader.ORIGIN_KEY)
        )

        tmpFile = File(
            "${fileUtil.tmpDir}/$taskId"
        )

        if (!tmpFile.exists()) {
            tmpFile.mkdir()
        }

        monitorProcess = workerScope.launch {
            while (isActive) {
                val folderSize = try {
                    calculateFolderSize(tmpFile)
                } catch (e: Throwable) {
                    -1L
                }

                if (folderSize > 0 && folderSize != lastTmpDirSize) {
                    val downloadedTmpFolderSize =
                        FileUtil.getFileSizeReadable(folderSize.toDouble())
                    lastTmpDirSize = folderSize

                    if (progressCached > 0) {
                        isDownloadOk = true
                        monitorProcess?.cancel()
                        return@launch
                    }

                    if (isDownloadJustStarted && !isDownloadOk) {
                        ++isLiveCounter
                        if (isLiveCounter > 2) {
                            isLiveCounter = 3

                            val downloaded = lastTmpDirSize
                            saveProgress(
                                taskId,
                                LineInfo(
                                    "LIVE",
                                    downloaded.toDouble(),
                                    downloaded.toDouble(),
                                    sourceLine = "Downloading live stream...downloaded: $downloadedTmpFolderSize, press stop and save, to stop downloading and save downloaded at any time...!"
                                ),
                                task.also { item ->
                                    item.taskState = VideoTaskState.DOWNLOADING
                                    item.lineInfo = downloadedTmpFolderSize
                                    item.downloadSize = downloaded
                                    item.totalSize = downloaded
                                })
                            showProgress(
                                taskId,
                                taskId,
                                name,
                                99,
                                "Downloading Live Stream... $downloadedTmpFolderSize",
                                tmpFile
                            )
                        }
                    }
                }

                delay(1000)
            }
        }

        request.addOption("--progress")

        val threadsCount = preferences.m3u8ThreadCountBlocking() + 1
        request.addOption("-N", threadsCount)

        request.addOption("--recode-video", "mp4")
        request.addOption("--merge-output-format", "mp4")
        // any another downloader has issues
        request.addOption("--hls-prefer-native")
        // without this download will start again from beginning after error
        request.addOption("--hls-use-mpegts")

        if (isContinue) {
            request.addOption("--continue")
        }

//        $youtube-dl --proxy http://user:password@your_proxy.com:port url
        val currentProxy = proxyController.getCurrentRunningProxy()
        if (currentProxy != Proxy.noProxy()) {
            val user = proxyController.getProxyCredentials().first
            val password = proxyController.getProxyCredentials().second
            if (user.isNotEmpty() && password.isNotEmpty()) {
                request.addOption(
                    "--proxy",
                    "http://${user}:${password}@${currentProxy.host}:${currentProxy.port}"
                )
            } else {
                request.addOption("--proxy", "${currentProxy.host}:${currentProxy.port}")
            }
        }

        showProgress(taskId, taskId, name, 0, "Fetching info, Please wait...", tmpFile)
        runBlocking {
            saveProgress(taskId,
                line = LineInfo(taskId, 0.0, 0.0, sourceLine = "Fetching info, Please wait..."),
                task.also { it.taskState = VideoTaskState.PREPARE })
        }

        request.addOption("-o", "${tmpFile.absolutePath}/${name}.%(ext)s")


        val videoOnly = vFormat.vcodec != "none" && vFormat.acodec == "none"
        if (videoOnly) {
            request.addOption("-f", "${vFormat.formatId}+bestaudio")
        } else {
            request.addOption("-f", "${vFormat.formatId}")
        }

        vFormat.httpHeaders?.forEach {
            if (it.key != "Cookie") {
                request.addOption("--add-header", "${it.key}:${it.value}")
            }
        }

        try {
            val interval = 1000

            showProgress(taskId, taskId, name, 0, "Starting...", tmpFile)
            runBlocking {
                saveProgress(taskId,
                    line = LineInfo(taskId, 0.0, 0.0, sourceLine = "Starting..."),
                    task.also { it.taskState = VideoTaskState.DOWNLOADING })
            }
            disposable?.cancel()

            val freeSpace = FileUtil.getFreeDiskSpace(fileUtil.folderDir)
            if (freeSpace < TRESHOLD) {
                (task.also {
                    task.mId = taskId
                    task.taskState = VideoTaskState.ERROR
                    task.errorMessage = "Not enough space"
                })
                return
            }

            disposable = workerScope.launch {
                try {
                    val dlResponse: YoutubeDLResponse =
                        YoutubeDL.getInstance().execute(request, taskId) { pr, _, line ->
                            if (line.contains("[download] Destination:")) {
                                isDownloadJustStarted = true
                            }
                            if (line.contains(Regex("""\[download] {3}\d+"""))) {
                                isDownloadOk = true
                            }

                            val lineInfo: LineInfo? = try {
                                parseInfoFromLine(line)
                            } catch (e: Throwable) {
                                null
                            }

                            progressCached = pr.toInt()

                            if (Date().time - time > interval && !getDone()) {
                                time = Date().time

                                val totalBytes = (lineInfo?.total ?: 0).toLong()

                                val downloadBytes = (totalBytes * (pr / 100)).toLong()
                                val downloadBytesFixed = if (downloadBytes > 0) {
                                    downloadBytes
                                } else {
                                    0
                                }
                                task.percent = pr
                                task.totalSize = totalBytes
                                task.downloadSize = downloadBytesFixed
                                task.taskState = VideoTaskState.DOWNLOADING

                                if (progressDisposable != null) {
                                    progressDisposable?.cancel()
                                    progressDisposable = null
                                }
                                runBlocking {
                                    saveProgress(taskId, lineInfo, task)
                                }
                                showProgress(
                                    taskId, taskId, name, pr.toInt(), line ?: "", tmpFile
                                )
                                val freeSpace = FileUtil.getFreeDiskSpace(fileUtil.folderDir)
                                if (freeSpace < TRESHOLD) {
                                    finishWork(task.also {
                                        it.mId = taskId
                                        it.taskState = VideoTaskState.ERROR
                                        it.errorMessage = "Not enough space"
                                    })
                                    return@execute
                                }
                            }
                        }

                    // Seems like youtubedlp has a bug and sometimes skip removing of merged fragments
                    val list = tmpFile.listFiles()
                    val finalFile = if (!list.isNullOrEmpty()) {
                        tmpFile.walkTopDown()
                            .filter { it.isFile && it.extension.equals("mp4", ignoreCase = true) }
                            .firstOrNull()
                    } else {
                        null
                    }
                    if (dlResponse.exitCode == 0 && finalFile != null) {

                        val toUri =
                            Uri.fromFile(File(fixFileName("${downloadDir.absolutePath}/${finalFile.name}")))

                        val moved = fileUtil.moveMedia(
                            this@YoutubeDlDownloaderWorker.applicationContext,
                            Uri.fromFile(finalFile),
                            toUri
                        )

                        if (this@YoutubeDlDownloaderWorker.cookieFile != null) {
                            this@YoutubeDlDownloaderWorker.cookieFile!!.delete()
                        }

                        var movedFilePath = ""

                        if (moved) {
                            tmpFile.deleteRecursively()

                            movedFilePath = toUri.path.toString()

                        }

                        finishWork(VideoTaskItem(url).also { f ->
                            f.filePath = movedFilePath
                            f.fileName = extractFileName(movedFilePath).first
                            f.title = task.title
                            f.errorCode = if (moved) 0 else 1
                            f.percent = 100F
                            f.taskState =
                                if (moved) VideoTaskState.SUCCESS else VideoTaskState.ERROR
                        })
                    } else {
                        val fixedList = tmpFile.listFiles()?.filter { !it.name.contains("part") }
                        if (this@YoutubeDlDownloaderWorker.cookieFile != null) {
                            this@YoutubeDlDownloaderWorker.cookieFile!!.delete()
                        }

                        fixedList?.firstOrNull().let {
                            if (it != null) {
                                finishWork(VideoTaskItem(url).also { f ->
                                    f.fileName = it.name
                                    f.errorCode = 1
                                    f.taskState = VideoTaskState.ERROR
                                })
                            } else {
                                Timber.w("startDownload: no output file produced for $url")
                                finishWork(VideoTaskItem(url).also { f ->
                                    f.errorCode = 1
                                    f.taskState = VideoTaskState.ERROR
                                })
                            }
                        }
                    }
                } catch (e: Throwable) {
                    handleError(taskId, url, progressCached, e, tmpFile.name, name)
                }
            }
        } catch (e: Throwable) {
            Timber.e(e, "youtube-dl download failed: $url")
            if (this@YoutubeDlDownloaderWorker.cookieFile != null) {
                this@YoutubeDlDownloaderWorker.cookieFile!!.delete()
            }
            handleError(taskId, url, progressCached, e, tmpFile.name, name)
        }
    }

    fun extractFileName(path: String): Triple<String, String, String> {
        val fullFileName = path.substringAfterLast("/")  // Tên file có đuôi
        val fileNameWithoutExt = fullFileName.substringBeforeLast(".") // Tên file không có đuôi
        val parentFolder = path.substringBeforeLast("/") // Thư mục chứa file

        return Triple(fullFileName, fileNameWithoutExt, parentFolder)
    }

    private fun calculateFolderSize(directory: File): Long {
        var length = 0L
        if (directory.isDirectory) {
            for (file in directory.listFiles() ?: emptyArray()) {
                length += calculateFolderSize(file)
            }
        } else {
            length += directory.length()
        }
        return length
    }

    private fun handleError(
        taskId: String,
        url: String,
        progressCached: Int,
        throwable: Throwable,
        tmpFileName: String,
        name: String
    ) {
        Timber.e(throwable, "Download error: taskId=$taskId url=$url progress=$progressCached")

        finishWork(VideoTaskItem(url).also { f ->
            if (isCanceled && throwable is YoutubeDL.CanceledException) {
                f.taskState = VideoTaskState.CANCELED
                f.errorCode = 0
            } else if (throwable is YoutubeDL.CanceledException) {
                f.taskState = VideoTaskState.PAUSE
                f.errorCode = 0
            } else {
                f.taskState = VideoTaskState.ERROR
                f.errorCode = 1
                f.errorMessage = throwable.message?.replace(Regex("WARNING:.+\n"), "") ?: ""
            }
            f.fileName = name
            f.percent = progressCached.toFloat()

        })

    }

    //[download]   0.3% of ~  49.94MiB at  438.62KiB/s ETA 04:41 (frag 2/201)
    private fun parseInfoFromLine(line: String?): LineInfo? {
        if (line == null) {
            return null
        }
        return if (line.startsWith("[download]")) {
            val tmp = line.split(Regex(" +"))
            val percent = tmp[1].replace("%", "").trim().toDoubleOrNull()

            var indx = 3
            if (line.contains("~")) indx = 4
            val totalStr = tmp[indx]

            val p: Pattern = Pattern.compile("\\p{L}")
            val tM: Matcher = p.matcher(totalStr)
            if (tM.find()) {
                val indxT = totalStr.substring(0, tM.start())
                val valT = totalStr.substring(tM.start())
                val totalParsed = LineInfo.parse("$indxT $valT")

                return if (tmp.last().contains(")")) {
                    val downloadedFrag =
                        tmp.last().split("/")[0].replace("(frag ", "").toIntOrNull()
                    val totalFrag = tmp.last().split("/")[0].replace(") ", "").toIntOrNull()


                    LineInfo(
                        "download",
                        totalParsed * percent!! / 100,
                        totalParsed,
                        downloadedFrag,
                        totalFrag,
                        sourceLine = line
                    )
                } else {
                    LineInfo(
                        "download", totalParsed * percent!! / 100, totalParsed, sourceLine = line
                    )
                }
            }

            return null
        } else {
            LineInfo("download", 0.0, 0.0, sourceLine = line)
        }
    }

    private class LineInfo(
        val id: String,
        val progress: Double,
        val total: Double,
        val fragDownloaded: Int? = null,
        val fragTotal: Int? = null,
        val sourceLine: String
    ) {
        companion object {
            private const val KB_FACTOR: Long = 1000
            private const val KIB_FACTOR: Long = 1024
            private const val MB_FACTOR = 1000 * KB_FACTOR
            private const val MIB_FACTOR = 1024 * KIB_FACTOR
            private const val GB_FACTOR = 1000 * MB_FACTOR
            private const val GIB_FACTOR = 1024 * MIB_FACTOR

            fun parse(arg0: String): Double {
                val spaceNdx = arg0.indexOf(" ")
                val ret = arg0.substring(0, spaceNdx).toDouble()
                when (arg0.substring(spaceNdx + 1)) {
                    "GB" -> return ret * GB_FACTOR
                    "GiB" -> return ret * GIB_FACTOR
                    "MB" -> return ret * MB_FACTOR
                    "MiB" -> return ret * MIB_FACTOR
                    "KB" -> return ret * KB_FACTOR
                    "KiB" -> return ret * KIB_FACTOR
                    "B" -> return ret
                }
                return (-1).toDouble()
            }
        }

        override fun toString(): String {
            return "${FileUtil.getFileSizeReadable(progress)} / ${
                FileUtil.getFileSizeReadable(
                    total
                )
            }  frag: $fragDownloaded / $fragTotal"
        }
    }

    private fun showProgress(
        id: String, taskId: String, name: String, progress: Int, line: String, tmpFile: File
    ) {
        val text = line.replace(tmpFile.toString(), "")

        val taskItem = VideoTaskItem("").also {
            it.mId = taskId
            it.fileName = name
            it.taskState = VideoTaskState.DOWNLOADING
            it.percent = progress.toFloat()
            it.lineInfo = text
        }
        val data = notificationsHelper.createNotificationBuilder(taskItem)

        showNotification(data.first, data.second)
        showNotificationAsync(data.first, data.second)
    }


    @SuppressLint("CheckResult")
    override fun finishWork(item: VideoTaskItem?) {
        Timber.d("finishWork: $item")
        if (getDone()) {
            try {
                getContinuation().resume(Result.success())
            } catch (e: Throwable) {
                Timber.e(e, "finishWork could not resume continuation: $item")
            }
            return
        }

        val taskId = inputData.getString(GenericDownloader.DOWNLOAD_ID_KEY)

        if (taskId != null) {
            GenericDownloader.deleteHeadersStringFromSharedPreferences(applicationContext, taskId)
        }

        notificationsHelper.hideNotification(taskId.hashCode())
        if (item != null) {
            showNotification(
                taskId.hashCode() + 1, notificationsHelper.createNotificationBuilder(item.also {
                    it.mId = taskId.toString()
                }).second
            )
        }

        disposable?.cancel()
        progressDisposable?.cancel()
        disposable = null
        progressDisposable = null
        cookieFile?.delete()

        if (taskId != null) {
            if (item != null) {
                runBlocking {
                    saveProgress(
                        taskId,
                        line = LineInfo(taskId, 0.0, 0.0, sourceLine = item.errorMessage ?: ""),
                        item
                    )
                }
                setDone()

                try {
                    if (item.taskState == VideoTaskState.ERROR) {
                        getContinuation().resume(Result.failure())
                    } else {
                        getContinuation().resume(Result.success())
                    }
                } catch (_: Exception) {
                    try {
                        getContinuation().resume(Result.failure())
                    } catch (_: Throwable) {

                    }
                }
            } else {
                try {
                    getContinuation().resume(Result.failure())
                } catch (_: Throwable) {

                }
            }
        } else {
            try {
                getContinuation().resume(Result.failure())
            } catch (_: Throwable) {

            }
        }
    }

    private suspend fun saveProgress(
        taskId: String, line: LineInfo? = null, task: VideoTaskItem
    ) {
        if (getDone() && task.taskState == VideoTaskState.DOWNLOADING) {
            Timber.d(
                "saveProgress task returned cause DONE!!!"
            )
            return
        }
        val isBytesNoTouch = line?.total == null || line.total == 0.0
        val iProgressUpdate = task.downloadSize.toInt() > 0

        val progressList = progressRepository.getProgressInfos().first()
        val dbTask = progressList.find { it.id == taskId }

        if (!isBytesNoTouch) {
            dbTask?.progressTotal = (line?.total ?: task.totalSize).toLong()
        }

        if (task.taskState != VideoTaskState.SUCCESS) {
            if (!isBytesNoTouch && iProgressUpdate) {
                dbTask?.progressDownloaded = task.downloadSize
            }
        } else {
            dbTask?.progressDownloaded = dbTask?.progressTotal ?: -1
        }

        dbTask?.fragmentsTotal = line?.fragTotal ?: 1
        dbTask?.fragmentsDownloaded = line?.fragDownloaded ?: 0
        dbTask?.downloadStatus = task.taskState

        dbTask?.infoLine = line?.sourceLine ?: ""

        if (line?.id == "LIVE" && dbTask?.isLive != true) {
            dbTask?.isLive = true
        }

        if (dbTask != null) {
            if (getDone() && task.taskState == VideoTaskState.DOWNLOADING) {
                Timber.d(
                    "saveProgress task returned cause DONE!!!"
                )
            } else {
                progressRepository.saveProgressInfo(dbTask)
            }
        }
    }

    private fun resumeDownload(task: VideoTaskItem, headers: Map<String, String>) {
        startDownload(task, headers, true)
    }

    private fun pauseDownload(task: VideoTaskItem, headers: Map<String, String>) {
        Timber.d("pauseDownload: task: $task")
        if (getDone()) return

        val id = inputData.getString(GenericDownloader.DOWNLOAD_ID_KEY)
        if (id != null) {
            YoutubeDL.getInstance().destroyProcessById(id)

            WorkManager.getInstance(applicationContext).cancelAllWorkByTag(id)

            if (task.taskState != VideoTaskState.DOWNLOADING) {
                finishWork(task.also {
                    it.mId = id.toString()
                    it.taskState = VideoTaskState.PAUSE
                })
            }
        }
    }

    private fun cancelDownload(task: VideoTaskItem, headers: Map<String, String>) {
        Timber.d("cancelDownload: task: $task")
        val taskId = inputData.getString(GenericDownloader.DOWNLOAD_ID_KEY)
        val isFileRemove = inputData.getBoolean(GenericDownloader.IS_FILE_REMOVE_KEY, false)

        if (taskId != null) {
            val fileToRemove = File("${fileUtil.tmpDir}/$taskId")

            if (isFileRemove) {
                fileToRemove.deleteRecursively()
            }

            YoutubeDL.getInstance().destroyProcessById(taskId)

            if (task.taskState != VideoTaskState.DOWNLOADING) {
                finishWork(task.also {
                    it.mId = taskId.toString()
                    it.taskState = VideoTaskState.CANCELED
                })
            }
        }
    }

    private fun isM3u8OrMpd(url: String): Boolean {
        return url.contains(".m3u8") || url.contains(".mpd") || url.contains(".txt")
    }
}