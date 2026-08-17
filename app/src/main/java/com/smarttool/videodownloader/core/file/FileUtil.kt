package com.smarttool.videodownloader.core.file

//import com.allVideoDownloaderXmaster.OpenForTesting
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.text.DecimalFormat
import java.util.Arrays

//@OpenForTesting
class FileUtil constructor(
    private val appContext: Context,
    private val preferences: AppPreferencesDataSource,
) {

    companion object {
        var INITIIALIZED = false

        // For downloads and tmp data
        var IS_EXTERNAL_STORAGE_USE = true

        // For downloads
        var IS_APP_DATA_DIR_USE = false

        const val FOLDER_NAME = "SuperVideoDownloader"
        const val TMP_DATA_FOLDER_NAME = "video_downloader_pro_max"

        /**
         * Deliberately distinct from [FOLDER_NAME]: when `IS_APP_DATA_DIR_USE` is on, downloads
         * land in `<externalFilesDir>/FOLDER_NAME`, and the private area must never share a
         * directory with them (its `.nomedia` would hide the normal library too).
         */
        const val PRIVATE_FOLDER_NAME = "private_media"

        private const val KB = 1024
        private const val MB = 1024 * 1024
        private const val GB = 1024 * 1024 * 1024
        fun getFileSizeReadable(length: Double): String {

            val decimalFormat = DecimalFormat("#.##")
            return when {
                length > GB -> decimalFormat.format(length / GB) + " GB"
                length > MB -> decimalFormat.format(length / MB) + " MB"
                length > KB -> decimalFormat.format(length / KB) + " KB"
                else -> decimalFormat.format(length) + " B"
            }
        }

        fun getVideoDuration(context: Context, fileUri: Uri): Long {
            val retriever = MediaMetadataRetriever()
            return try {
                retriever.setDataSource(context, fileUri)
                val durationStr =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationStr?.toLong() ?: 0L
            } finally {
                retriever.release()
            }
        }

        fun getFreeDiskSpace(path: File): Long {
            if (!path.exists()) {
                throw IllegalArgumentException("Path does not exist")
            }

            val stats = StatFs(path.absolutePath)
            return stats.availableBlocksLong * stats.blockSizeLong
        }

        fun isExternalStorageWritable(): Boolean {
            return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        }
    }

    val folderDir: File
        get() {
            if (!INITIIALIZED) {
                throw Error("File Util Not Initialized")
            }

            val context = appContext

            when {
                IS_EXTERNAL_STORAGE_USE && !IS_APP_DATA_DIR_USE -> {
                    val publicDownloads = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            .toURI()
                    )
                    val subfolder = downloadLocationSubfolderName()
                    return if (subfolder.isBlank()) publicDownloads else File(publicDownloads, subfolder)
                }

                IS_EXTERNAL_STORAGE_USE && IS_APP_DATA_DIR_USE -> {
                    return File(context.getExternalFilesDir(null), FOLDER_NAME)

                }

                else -> {
                    return File(context.filesDir.absolutePath, FOLDER_NAME)
                }
            }
        }

    /**
     * App-private storage. Android 10+ excludes app-specific directories from MediaStore on its
     * own; the `.nomedia` marker is what keeps the pre-Q scanner out of it too.
     */
    val privateDir: File
        get() = File(appContext.getExternalFilesDir(null), PRIVATE_FOLDER_NAME).also { dir ->
            dir.mkdirs()
            val noMedia = File(dir, ".nomedia")
            if (!noMedia.exists()) {
                runCatching { noMedia.createNewFile() }
                    .onFailure { Timber.w(it, "Could not create .nomedia in $dir") }
            }
        }

    /** User-chosen subfolder name under the public Downloads directory, or "" for the root. */
    private fun downloadLocationSubfolderName(): String =
        FileNameCleaner.cleanFileName(preferences.downloadLocationSubfolderBlocking())

    /** `RELATIVE_PATH` value for MediaStore.Downloads inserts, honoring the chosen subfolder. */
    private fun downloadsRelativePath(): String {
        val subfolder = downloadLocationSubfolderName()
        return if (subfolder.isBlank()) {
            Environment.DIRECTORY_DOWNLOADS
        } else {
            "${Environment.DIRECTORY_DOWNLOADS}/$subfolder"
        }
    }

    val tmpDir: File
        get() {
            if (!INITIIALIZED) {
                throw Error("File Util Not Initialized")
            }

            return getTmpDataDir(appContext, IS_EXTERNAL_STORAGE_USE)
        }

    val listFiles: Map<String, Pair<Long, Uri>>
        get() {
            val context = appContext
            val result = mutableMapOf<String, Pair<Long, Uri>>()

            val externalPrivateFilesObjs = getPrivateDownloadsDirFilesObj(context, true)
            val internalPrivateFilesObjs = getPrivateDownloadsDirFilesObj(context, false)
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                val externalPublicFilesObjs = getPublicDownloadsDirFilesObjOld(context, true)
                val internalPublicFilesObjs = getPublicDownloadsDirFilesObjOld(context, false)
                result.putAll(externalPublicFilesObjs)
                result.putAll(internalPublicFilesObjs)
            } else {
                val externalPublicFilesObjsNew = getPublicDownloadsDirFilesObjNew()
                result.putAll(externalPublicFilesObjsNew)
            }
            result.putAll(externalPrivateFilesObjs)
            result.putAll(internalPrivateFilesObjs)

            return result

        }

    fun isFileWithNameNotExists(context: Context, uri: Uri, newName: String): Boolean {
        return if (isFileApiSupportedByUri(context, uri)) {
            !File(uri.toFile().parentFile, newName).exists()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                !isDownloadedVideoContentExistsByName(context.contentResolver, uri, newName)
            } else {
                throw Exception("File api support ERROR")
            }
        }
    }

    /**
     * Moves [from] to [to]. On the MediaStore path (API 29+ public Downloads), the destination
     * display name may get deduplicated by [moveFileToDownloadsFolder] or MediaProvider itself,
     * so the returned path reflects where the file actually ended up, not [to].
     * Returns the final absolute path on success, or null on failure.
     */
    fun moveMedia(context: Context, from: Uri, to: Uri): String? {
        if (isFileApiSupportedByUri(context, to)) {
            Timber.d("moveMedia via File API: $from -> $to")
            val newFile = to.toFile()

            return if (from.toFile().renameTo(newFile)) newFile.absolutePath else null
        } else {
            Timber.d("moveMedia via MediaStore: $from -> $to")
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                moveFileToDownloadsFolder(
                    context.contentResolver, from.toFile(), to.toFile().name
                )
            } else {
                throw Exception("File API support ERROR!!!")
            }
        }
    }

    fun renameMedia(context: Context, from: Uri, newName: String): Pair<String, Uri>? {
        try {
            val cleanedFileName = FileNameCleaner.cleanFileName(newName) + ".mp4"
            val isNewFileNotExists = isFileWithNameNotExists(context, from, newName)

            if (cleanedFileName.isEmpty()) {
                throw Error("Empty file name")
            }

            if (!isUriExists(context, from)) {
                throw FileNotFoundException("File not found: $from")
            }

            if (!isNewFileNotExists) {
                throw Exception("File already exists")
            }
            if (isFileApiSupportedByUri(context, from)) {
                val fromFile = from.toFile()
                val toFile = File(fromFile.parentFile, cleanedFileName)
                if (toFile.exists()) {
                    throw Exception("File already exists: $toFile")
                }
                fromFile.renameTo(toFile)

                return Pair(toFile.name, Uri.fromFile(toFile))
            } else {
                val newUri = renameVideoContentFromDownloads(context, from, cleanedFileName)

                return Pair(cleanedFileName, newUri ?: from)
            }
        } catch (e: Throwable) {
            Timber.e(e, "renameMediaUri failed: $from")
            Toast.makeText(context, context.getString(R.string.string_error), Toast.LENGTH_SHORT).show()
        }

        return null
    }

    fun renameMedia(context: Context, filePath: String, newName: String): Pair<String, String>? {
        try {
            // Làm sạch tên file và đảm bảo có đuôi .mp4
            val cleanedFileName = FileNameCleaner.cleanFileName(newName) + ".mp4"

            // Kiểm tra tên file mới có rỗng không
            if (cleanedFileName.isEmpty()) {
                throw Error("Tên file không được để trống")
            }

            val oldFile = File(filePath)

            // Kiểm tra file cũ có tồn tại không
            if (!oldFile.exists()) {
                throw FileNotFoundException("Không tìm thấy file: $filePath")
            }

            val newFile = File(oldFile.parent, cleanedFileName)

            // Kiểm tra file mới đã tồn tại chưa
            if (newFile.exists()) {
                throw Exception("File đã tồn tại: ${newFile.absolutePath}")
            }

            // Đổi tên file
            val renamed = oldFile.renameTo(newFile)
            if (renamed) {
                // Trả về tên file mới và đường dẫn đầy đủ mới
                return Pair(newFile.name, newFile.absolutePath)
            } else {
                throw Exception("Không thể đổi tên file: ${oldFile.absolutePath}")
            }
        } catch (e: Throwable) {
            Timber.e(e, "renameMedia failed: filePath=$filePath newName=$newName")
            Toast.makeText(context,
                context.getString(R.string.string_error_renaming_file), Toast.LENGTH_SHORT).show()
        }

        return null
    }

    fun renameImage(context: Context, filePath: String, newName: String): Pair<String, String>? {
        try {
            Timber.d("renameMedia: filePath=$filePath newName=$newName")

            // Làm sạch tên file và đảm bảo có phần mở rộng của ảnh
            val cleanedFileName = FileNameCleaner.cleanFileNameImage(newName) + ".jpg"

            // Kiểm tra tên file mới có rỗng không
            if (cleanedFileName.isEmpty()) {
                throw Error("Tên file không được để trống")
            }

            val oldFile = File(filePath)

            // Kiểm tra file cũ có tồn tại không
            if (!oldFile.exists()) {
                throw FileNotFoundException("Không tìm thấy file: $filePath")
            }

            Timber.d("renameImage: cleanedFileName: $cleanedFileName")

            val newFile = File(oldFile.parent, cleanedFileName)

            // Kiểm tra file mới đã tồn tại chưa
            if (newFile.exists()) {
                throw Exception("File đã tồn tại: ${newFile.absolutePath}")
            }

            // Đổi tên file
            val renamed = oldFile.renameTo(newFile)
            if (renamed) {
                // Trả về tên file mới và đường dẫn đầy đủ mới
                return Pair(newFile.name, newFile.absolutePath)
            } else {
                throw Exception("Không thể đổi tên file: ${oldFile.absolutePath}")
            }
        } catch (e: Throwable) {
            Timber.e(e, "renameImage failed: filePath=$filePath newName=$newName")
            Toast.makeText(context,
                context.getString(R.string.string_error_renaming_image), Toast.LENGTH_SHORT).show()
        }

        return null
    }


    @Deprecated("This old")

    fun deleteMedia(context: Context, uri: Uri): Boolean {
        return try {
            if (!isUriExists(context, uri)) {
                throw FileNotFoundException("File not found: $uri")
            }
            val deleted = if (isFileApiSupportedByUri(context, uri)) {
                uri.toFile().delete()
            } else {
                deleteDownloadedVideoContent(context, uri)
            }
            deleted
        } catch (e: Throwable) {
            Timber.e(e, "deleteMedia failed: uri=$uri")
            Toast.makeText(context, context.getString(R.string.string_error), Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun deleteMedia(context: Context, filePath: String): Boolean {
        return try {
            val file = File(filePath)

            // Kiểm tra nếu file tồn tại
            if (!file.exists()) {
                Timber.e("deleteMedia: file not found: $filePath")
                return false
            }

            // Nếu file là loại "content://", cần dùng contentResolver để xóa
            if (filePath.startsWith("content://")) {
                val uri = Uri.parse(filePath)
                val deleted = context.contentResolver.delete(uri, null, null) > 0
                Timber.d("deleteMedia via contentResolver: deleted=$deleted filePath=$filePath")
                return deleted
            }

            // Xóa file trực tiếp từ hệ thống
            file.setWritable(true) // Đảm bảo có quyền ghi
            val deleted = file.delete()

            if (!deleted) {
                Timber.e("deleteMedia: could not delete file: $filePath")
            } else {
                Timber.d("deleteMedia: file deleted: $filePath")
            }

            return deleted
        } catch (e: Exception) {
            Timber.e(e, "deleteMedia failed: filePath=$filePath")
            Toast.makeText(context,
                context.getString(R.string.string_error_deleting_file), Toast.LENGTH_SHORT).show()
            false
        }
    }


    fun isUriExists(context: Context, uri: Uri): Boolean {
//        if (isFileApiSupportedByUri(context, uri)) {
//            return uri.toFile().exists()
//        }
//
//        try {
//            context.contentResolver.openInputStream(uri)?.close()
//        } catch (e: FileNotFoundException) {
//            return false
//        } catch (e: Exception) {
//            // Handle other exceptions as needed
//        }
//
//        // If there were no exceptions, the URI exists
//        return true

        return try {
            when (uri.scheme) {
                "file" -> File(uri.path!!).exists()
                "content" -> context.contentResolver.openInputStream(uri)?.use { true } ?: false
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun getContentLength(context: Context, uri: Uri): Long {
        return if (isFileApiSupportedByUri(context, uri)) {
            uri.toFile().length()
        } else {
            getContentSize(context, uri)
        }
    }

    fun isFileApiSupportedByUri(context: Context, uri: Uri): Boolean {
        val isExternalTo = isExternalUri(uri)

        val privateDir = getPrivateDownloadsDir(context, isExternalTo)
        val isAppDir = uri.toString().startsWith(Uri.fromFile(privateDir).toString())

        return !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isAppDir)
    }

    private fun getContentSize(context: Context, uri: Uri): Long {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && !cursor.isNull(sizeIndex)) {
                cursor.getLong(sizeIndex)
            } else {
                -1 // Return -1 if size is unknown or an error occurred
            }
        } ?: -1 // Return -1 if the query failed
    }

    private fun renameVideoContentFromDownloads(context: Context, uri: Uri, newName: String): Uri? {
        // Check if the URI is a document URI
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // Rename the document using the DocumentsContract API
            return DocumentsContract.renameDocument(context.contentResolver, uri, newName)
        } else {
            // Rename the file using the ContentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, newName)
                put(MediaStore.Downloads.RELATIVE_PATH, downloadsRelativePath())
            }

            context.contentResolver.update(uri, values, null, null)

            return Uri.parse(uri.toString())
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun isDownloadedVideoContentExistsByName(
        contentResolver: ContentResolver, contentOrig: Uri, fileName: String
    ): Boolean {
        val isExternal = isExternalUri(contentOrig)
        val contentUri = if (isExternal) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Downloads.INTERNAL_CONTENT_URI
        }
        // Query the Downloads collection for files with the given name
        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        val cursor = contentResolver.query(
            contentUri, projection, selection, selectionArgs, null
        )

        // Check if the cursor is not null and has at least one row
        val exists = (cursor?.count ?: 0) > 0

        // Close the cursor
        cursor?.close()

        return exists
    }

    private fun getTmpDataDir(context: Context, isExternal: Boolean): File {
        val path = if (isExternal) {
            "${context.getExternalFilesDir(null)}/$TMP_DATA_FOLDER_NAME"
        } else {
            "${context.filesDir.absolutePath}/$TMP_DATA_FOLDER_NAME"
        }

        val file = File(path)
        if (!file.exists()) {
            file.mkdirs()
        }

        return file
    }

    private fun getPrivateDownloadsDirFilesObj(
        context: Context, isExternal: Boolean
    ): Map<String, Pair<Long, Uri>> {
        val filesMap = mutableMapOf<String, Pair<Long, Uri>>()

        val path = getPrivateDownloadsDir(context, isExternal).absolutePath

        val file = File(path)
        if (!file.exists()) {
            file.mkdirs()
        }

        val files = file.listFiles()

        if (files != null) {
            for (f in files) {
                filesMap[f.name] = Pair(f.length(), Uri.fromFile(f))
            }
        }

        return filesMap
    }

    private fun getPrivateDownloadsDir(context: Context, isExternal: Boolean): File {
        val path = if (isExternal) {
            "${context.getExternalFilesDir(null)}/$FOLDER_NAME"
        } else {
            "${context.filesDir.absolutePath}/$FOLDER_NAME"
        }

        return File(path)
    }

    private fun getPublicDownloadsDirFilesObjNew(): Map<String, Pair<Long, Uri>> {
        val downloadsDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toURI()
        )
        val filesList =
            downloadsDir.listFiles()?.filter { it.isFile && it.extension == "mp4" }?.toTypedArray()
                ?: emptyArray<File>()
        val filesMap = mutableMapOf<String, Pair<Long, Uri>>()

        for (file in filesList) {
            filesMap[file.name] = Pair(file.name.hashCode().toLong(), Uri.fromFile(file))
        }

        return filesMap
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getPublicDownloadsDirFilesObjOld(
        context: Context, isExternalStorage: Boolean
    ): Map<String, Pair<Long, Uri>> {
        val filesMap = mutableMapOf<String, Pair<Long, Uri>>()
        val targetUri = if (isExternalStorage) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Downloads.INTERNAL_CONTENT_URI
        }

        context.contentResolver.query(
            targetUri,
            arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME),
            null,
            null, null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    targetUri, id
                )

                val isUriExists = isUriExists(context, contentUri)
                if (isUriExists) {
                    filesMap[name] = Pair(id, contentUri)
                }
            }
        }

        return filesMap
    }

//    private fun deleteDownloadedVideoContent(context: Context, uri: Uri) {
//        if (DocumentsContract.isDocumentUri(context, uri)) {
//            DocumentsContract.deleteDocument(context.contentResolver, uri)
//        } else {
//            context.contentResolver.delete(uri, null, null)
//        }
//    }

    private fun deleteDownloadedVideoContent(context: Context, uri: Uri): Boolean {
        return try {
            val deletedRows = if (DocumentsContract.isDocumentUri(context, uri)) {
                DocumentsContract.deleteDocument(context.contentResolver, uri)
                1 // Giả sử xóa thành công, do API không trả về trạng thái
            } else {
                context.contentResolver.delete(uri, null, null)
            }
            deletedRows > 0
        } catch (e: Exception) {
            Timber.e(e, "deleteDownloadedVideoContent failed: uri=$uri")
            false
        }
    }


    private fun isExternalUri(uri: Uri): Boolean {
        val context = appContext

        val ext1 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            null
        }
        val ext2 = Uri.fromFile(context.getExternalFilesDir(null))
        val ext3 =
            Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))

        val result = uri.toString().contains(ext1.toString()) || uri.toString()
            .contains(ext2.toString()) || uri.toString().contains(ext3.toString())

        return result
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun moveFileToDownloadsFolder(
        contentResolver: ContentResolver,
        sourceFile: File,
        fileName: String,
        onProgress: ((Float) -> Unit)? = null,
    ): String? {
        val isFolderExternal = isExternalUri(folderDir.toUri())
        val collectionUri = if (isFolderExternal) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Downloads.INTERNAL_CONTENT_URI
        }
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(File(fileName).extension.lowercase())
            ?: "video/mp4"
        return moveFileToMediaStoreCollection(
            contentResolver, sourceFile, fileName, folderDir, collectionUri,
            downloadsRelativePath(), mimeType, onProgress,
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun moveFileToPicturesFolder(
        contentResolver: ContentResolver,
        sourceFile: File,
        fileName: String,
        onProgress: ((Float) -> Unit)? = null,
    ): String? {
        val picturesDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toURI()
        )
        return moveFileToMediaStoreCollection(
            contentResolver, sourceFile, fileName, picturesDir,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Environment.DIRECTORY_PICTURES,
            "image/jpeg", onProgress,
        )
    }

    /**
     * Copies [sourceFile] into the MediaStore [collectionUri] (Downloads or Images) at
     * [relativePath], deleting the source on success. [destinationDir] is only used for the
     * free-space check and to build the returned absolute path.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun moveFileToMediaStoreCollection(
        contentResolver: ContentResolver,
        sourceFile: File,
        fileName: String,
        destinationDir: File,
        collectionUri: Uri,
        relativePath: String,
        mimeType: String,
        onProgress: ((Float) -> Unit)? = null,
    ): String? {
        val availableSpace = destinationDir.freeSpace

        if (availableSpace < sourceFile.length()) {
            throw Error("Not available space $availableSpace, file size: ${sourceFile.length()}")
        }

        var name = fileName
        var counter = 1
        while (mediaStoreDisplayNameExists(contentResolver, collectionUri, name)) {
            name = "$name($counter)"
            counter++
        }

        val cleaned = FileNameCleaner.cleanFileName(name)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, cleaned)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }

        var fileUri = contentResolver.insert(collectionUri, values)
        if (fileUri == null) {
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, suffixBeforeExtension(cleaned, "_e"))
            fileUri = contentResolver.insert(collectionUri, values)
        }

        // Copy the file into the collection
        val isMoved = fileUri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                val copied = sourceFile.inputStream().use { inputStream ->
                    copyWithProgress(inputStream, outputStream, sourceFile.length(), onProgress)
                }
                if (copied > 0) {
                    // Delete the source file
                    sourceFile.delete()
                    true
                } else {
                    Timber.w("moveFileToMediaStoreCollection copied 0 bytes: $sourceFile")
                    false
                }
            }
        } ?: false

        if (!isMoved) {
            return null
        }

        // MediaProvider may rename the display name again on its own to resolve a collision,
        // so re-query it instead of trusting `cleaned` for the final on-disk path.
        val actualDisplayName = queryDisplayName(contentResolver, fileUri!!) ?: cleaned
        return File(destinationDir, actualDisplayName).absolutePath
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun queryDisplayName(contentResolver: ContentResolver, uri: Uri): String? {
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun mediaStoreDisplayNameExists(
        contentResolver: ContentResolver, collectionUri: Uri, displayName: String
    ): Boolean {
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(displayName)

        val cursor = contentResolver.query(
            collectionUri, projection, selection, selectionArgs, null
        )

        val exists = cursor?.moveToFirst() ?: false
        cursor?.close()

        return exists
    }

    /**
     * Streams [input] into [output], reporting 0f..1f as it goes. Callbacks fire only when the
     * whole-percent changes, so a multi-GB copy still updates the UI a bounded number of times.
     */
    private fun copyWithProgress(
        input: java.io.InputStream,
        output: java.io.OutputStream,
        totalBytes: Long,
        onProgress: ((Float) -> Unit)?,
    ): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var copied = 0L
        var lastReportedPercent = -1

        while (true) {
            val read = input.read(buffer)
            if (read < 0) break

            output.write(buffer, 0, read)
            copied += read

            if (onProgress != null && totalBytes > 0) {
                val fraction = (copied.toDouble() / totalBytes).coerceIn(0.0, 1.0)
                val percent = (fraction * 100).toInt()
                if (percent != lastReportedPercent) {
                    lastReportedPercent = percent
                    onProgress(fraction.toFloat())
                }
            }
        }

        output.flush()
        return copied
    }

    /** `clip.mp4` + `(1)` -> `clip(1).mp4`; keeps the extension usable on collision renames. */
    private fun suffixBeforeExtension(fileName: String, suffix: String): String {
        val extension = fileName.substringAfterLast('.', "")
        return if (extension.isEmpty()) {
            "$fileName$suffix"
        } else {
            "${fileName.substringBeforeLast('.')}$suffix.$extension"
        }
    }

    /**
     * Moves a downloaded file out of public MediaStore storage into [privateDir], which is
     * app-private and never indexed by MediaStore/Gallery. Returns the new absolute path, or
     * `null` if the copy failed. If deleting the original afterwards fails, the copy is kept
     * rather than lost.
     */
    fun moveToPrivateStorage(
        context: Context,
        filePath: String,
        onProgress: ((Float) -> Unit)? = null,
    ): String? {
        val source = File(filePath)
        if (!source.exists()) return null

        var name = source.name
        var counter = 1
        while (File(privateDir, name).exists()) {
            name = suffixBeforeExtension(source.name, "($counter)")
            counter++
        }

        val dest = File(privateDir, name)
        return try {
            source.inputStream().use { input ->
                dest.outputStream().use { output ->
                    copyWithProgress(input, output, source.length(), onProgress)
                }
            }
            if (!deleteFromPublicStorage(context, filePath)) {
                Timber.w("moveToPrivateStorage: could not delete original: $filePath")
            }
            dest.absolutePath
        } catch (e: Exception) {
            Timber.e(e, "moveToPrivateStorage failed: $filePath")
            dest.delete()
            null
        }
    }

    /**
     * Deletes a public-storage file *and* its MediaStore row. Deleting only the file leaves a
     * dangling row behind, which Gallery apps keep listing until the next media scan — which
     * would defeat the point of moving something into the private area.
     */
    private fun deleteFromPublicStorage(context: Context, filePath: String): Boolean {
        val deletedViaMediaStore = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                context.contentResolver.delete(
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                    "${MediaStore.MediaColumns.DATA} = ?",
                    arrayOf(filePath),
                ) > 0
            }.getOrDefault(false)
        } else {
            false
        }

        val file = File(filePath)
        val deleted = deletedViaMediaStore || !file.exists() || file.delete()

        // Purges the row if the file was removed through the File API instead.
        MediaScannerConnection.scanFile(context, arrayOf(filePath), null, null)

        return deleted
    }

    /**
     * Moves a file out of [privateDir] back into public MediaStore storage (Downloads for
     * video, Pictures for image). Returns the new absolute path, or `null` on failure.
     */
    fun moveOutOfPrivateStorage(
        context: Context,
        filePath: String,
        isImage: Boolean,
        onProgress: ((Float) -> Unit)? = null,
    ): String? {
        val source = File(filePath)
        if (!source.exists()) return null

        val isQPlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        // Pre-Q takes the plain-rename path in moveMedia, which is instant and has nothing
        // meaningful to report; only the Q+ MediaStore copies stream through onProgress.
        val movedPath = when {
            isImage && isQPlus ->
                moveFileToPicturesFolder(context.contentResolver, source, source.name, onProgress)

            isImage -> {
                val picturesDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                picturesDir.mkdirs()
                moveMedia(context, source.toUri(), File(picturesDir, source.name).toUri())
            }

            isQPlus -> {
                folderDir.mkdirs()
                moveFileToDownloadsFolder(context.contentResolver, source, source.name, onProgress)
            }

            else -> {
                folderDir.mkdirs()
                moveMedia(context, source.toUri(), File(folderDir, source.name).toUri())
            }
        }

        // The Q+ branches insert into MediaStore themselves; the pre-Q ones are plain renames
        // and only become visible to Gallery once scanned.
        if (movedPath != null) {
            MediaScannerConnection.scanFile(context, arrayOf(movedPath), null, null)
        }

        return movedPath
    }
}

object FileNameCleaner {
    private const val MAX_FILE_NAME_LENGTH = 100
    private val illegalChars = intArrayOf(
        34,
        60,
        62,
        124,
        0,
        1,
        2,
        3,
        4,
        5,
        6,
        7,
        8,
        9,
        10,
        11,
        12,
        13,
        14,
        15,
        16,
        17,
        18,
        19,
        20,
        21,
        22,
        23,
        24,
        25,
        26,
        27,
        28,
        29,
        30,
        31,
        58,
        42,
        63,
        92,
        47
    )

    init {
        Arrays.sort(illegalChars)
    }

    fun cleanFileName(badFileName: String): String {
        val cleanName = StringBuilder()
        for (element in badFileName) {
            val c = element.code
            if (Arrays.binarySearch(illegalChars, c) < 0) {
                cleanName.append(c.toChar())
            }
        }
        var finalName = cleanName.toString()
            .replace(".mp4", "")
            .replace("/", "").replace("\\", "")
            .replace(":", "")
            .replace("*", "")
            .replace("?", "")
            .replace("\"", "")
            .replace("`", "")
            .replace("\'", "")
            .replace("<", "")
            .replace(">", "")
            .replace(".", "_")
            .replace("|", "")
            .replace(Regex("\\s*-\\s*"), "-")
            .replace(" ", "_").trim()
        if (finalName.isEmpty()) {
            finalName = "Untitled"
        }

        if (finalName.length > MAX_FILE_NAME_LENGTH) {
            return finalName.substring(0, MAX_FILE_NAME_LENGTH)
        }

        return finalName
    }

    fun cleanFileNameImage(badFileName: String): String {
        val cleanName = StringBuilder()
        for (element in badFileName) {
            val c = element.code
            if (Arrays.binarySearch(illegalChars, c) < 0) {
                cleanName.append(c.toChar())
            }
        }
        var finalName = cleanName.toString()
            .replace(".jpg", "")
            .replace("/", "").replace("\\", "")
            .replace(":", "")
            .replace("*", "")
            .replace("?", "")
            .replace("\"", "")
            .replace("`", "")
            .replace("\'", "")
            .replace("<", "")
            .replace(">", "")
            .replace(".", "_")
            .replace("|", "")
            .replace(Regex("\\s*-\\s*"), "-")
            .replace(" ", "_").trim()
        if (finalName.isEmpty()) {
            finalName = "Untitled"
        }

        if (finalName.length > MAX_FILE_NAME_LENGTH) {
            return finalName.substring(0, MAX_FILE_NAME_LENGTH)
        }

        return finalName
    }
}