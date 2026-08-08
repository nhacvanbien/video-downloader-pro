package com.smarttool.videodownloader.feature.browser.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.data.repository.VideoTaskItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * Downloads an image the user picked from a page (a plain URL or a `data:image` URI)
 * into `MediaStore`/`Downloads` and records it alongside the app's other saved media.
 *
 * Takes the application [Context] rather than an Activity — writing to `MediaStore` is
 * not Activity-scoped, and this needs to be callable from a ViewModel.
 */
class DownloadImageUseCase(
    private val context: Context,
    private val videoTaskItemRepository: VideoTaskItemRepository,
) {
    suspend operator fun invoke(imageUrl: String): File? = withContext(Dispatchers.IO) {
        try {
            if (imageUrl.startsWith("data:image")) {
                return@withContext saveBase64Image(imageUrl)
            }

            val client = OkHttpClient()
            val response = client.newCall(Request.Builder().url(imageUrl).build()).execute()

            if (!response.isSuccessful) {
                Timber.e("Image download failed: ${response.code}")
                return@withContext null
            }

            val fileName = generateFileName(URL(imageUrl).toString())
            val outputFile: File
            val fileSize: Long

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = insertPendingImage(fileName) ?: return@withContext null
                val descriptor = context.contentResolver.openFileDescriptor(uri, "w")
                    ?: return@withContext null

                fileSize = response.body.contentLength() ?: 0

                FileOutputStream(descriptor.fileDescriptor).use { output ->
                    response.body.byteStream().copyTo(output)
                }
                descriptor.close()

                outputFile = File(realPathFromUri(uri) ?: return@withContext null)
            } else {
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                outputFile = File(downloadsDir, fileName)

                response.body.byteStream().use { input ->
                    FileOutputStream(outputFile).use { output -> input.copyTo(output) }
                }

                fileSize = outputFile.length()
            }

            saveDownloadedImage(outputFile, fileName, fileSize, imageUrl)
            outputFile
        } catch (e: Exception) {
            Timber.e(e, "Image download failed: $imageUrl")
            null
        }
    }

    private suspend fun saveBase64Image(base64String: String): File? = withContext(Dispatchers.IO) {
        try {
            val base64Data = base64String.substringAfter("base64,", "")
            if (base64Data.isBlank()) {
                Timber.e("Invalid base64 data")
                return@withContext null
            }

            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val fileName = "downloaded_base64_image_${System.currentTimeMillis()}.jpg"

            val outputFile: File
            val fileSize: Long

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = insertPendingImage(fileName) ?: return@withContext null
                val descriptor = context.contentResolver.openFileDescriptor(uri, "w")
                    ?: return@withContext null

                FileOutputStream(descriptor.fileDescriptor).use { it.write(decodedBytes) }
                descriptor.close()

                outputFile = File(realPathFromUri(uri) ?: return@withContext null)
                fileSize = decodedBytes.size.toLong()
            } else {
                val picturesDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                outputFile = File(picturesDir, fileName)

                FileOutputStream(outputFile).use { it.write(decodedBytes) }
                fileSize = outputFile.length()
            }

            saveDownloadedImage(outputFile, fileName, fileSize, base64String)
            outputFile
        } catch (e: Exception) {
            Timber.e(e, "Saving base64 image failed")
            null
        }
    }

    private suspend fun saveDownloadedImage(
        outputFile: File,
        fileName: String,
        fileSize: Long,
        sourceUrl: String,
    ) {
        videoTaskItemRepository.insertVideoTaskItem(
            VideoTaskItem(
                mId = outputFile.absolutePath.hashCode().toString(),
                fileName = fileName,
                filePath = outputFile.absolutePath,
                fileSize = fileSize,
                fileDate = System.currentTimeMillis(),
                url = sourceUrl,
                mimeType = "image",
            ),
        )
    }

    private fun insertPendingImage(fileName: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        return context.contentResolver
            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    private fun realPathFromUri(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            if (cursor.moveToFirst()) return cursor.getString(columnIndex)
        }

        return null
    }

    private fun generateFileName(url: String): String {
        val extension = url.substringAfterLast(".", "jpg")
        return "downloaded_image_${System.currentTimeMillis()}.$extension"
    }
}
