package com.basefit.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {
    private const val TAG = "FileUtils"
    private const val RESOURCES_DIR = "exercise_resources"
    private const val THUMBNAILS_DIR = "thumbnails"
    private const val MAX_FILE_SIZE = 50 * 1024 * 1024L

    fun getResourcesDirectory(context: Context): File {
        val dir = File(context.filesDir, RESOURCES_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getThumbnailsDirectory(context: Context): File {
        val dir = File(context.filesDir, THUMBNAILS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun copyFileToAppStorage(
        context: Context,
        sourceUri: Uri,
        resourceType: String
    ): Pair<String, Long>? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return null

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val extension = getFileExtension(context, sourceUri) ?: getDefaultExtension(resourceType)
            val fileName = "${resourceType.lowercase()}_$timeStamp.$extension"

            val destDir = getResourcesDirectory(context)
            val destFile = File(destDir, fileName)

            inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            Pair(destFile.absolutePath, destFile.length())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy file", e)
            null
        }
    }

    fun getFileExtension(context: Context, uri: Uri): String? {
        val mimeType = context.contentResolver.getType(uri)
        return when {
            mimeType?.startsWith("image/gif") == true -> "gif"
            mimeType?.startsWith("image/") == true -> "jpg"
            mimeType?.startsWith("video/") == true -> "mp4"
            else -> {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    val name = cursor.getString(nameIndex)
                    val lastDot = name.lastIndexOf('.')
                    if (lastDot >= 0 && lastDot < name.length - 1) {
                        name.substring(lastDot + 1)
                    } else {
                        null
                    }
                }
            }
        }
    }

    private fun getDefaultExtension(resourceType: String): String {
        return when (resourceType.lowercase()) {
            "gif" -> "gif"
            "image" -> "jpg"
            "video" -> "mp4"
            else -> "bin"
        }
    }

    fun getFileName(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }
    }

    fun getFileSize(context: Context, uri: Uri): Long? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            cursor.getLong(sizeIndex)
        }
    }

    fun isFileSizeValid(context: Context, uri: Uri): Boolean {
        val size = getFileSize(context, uri) ?: return false
        return size <= MAX_FILE_SIZE
    }

    fun createVideoThumbnail(context: Context, videoPath: String): String? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            
            val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()

            bitmap?.let {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "thumb_$timeStamp.jpg"
                val thumbDir = getThumbnailsDirectory(context)
                val thumbFile = File(thumbDir, fileName)

                FileOutputStream(thumbFile).use { output ->
                    it.compress(Bitmap.CompressFormat.JPEG, 80, output)
                }
                it.recycle()

                thumbFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create video thumbnail", e)
            null
        }
    }

    fun createImageThumbnail(context: Context, imagePath: String): String? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(imagePath, options)

            val targetSize = 512
            val scaleFactor = maxOf(
                options.outWidth / targetSize,
                options.outHeight / targetSize
            ).coerceAtLeast(1)

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = scaleFactor
            }

            val bitmap = BitmapFactory.decodeFile(imagePath, decodeOptions) ?: return null

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "thumb_$timeStamp.jpg"
            val thumbDir = getThumbnailsDirectory(context)
            val thumbFile = File(thumbDir, fileName)

            FileOutputStream(thumbFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
            }
            bitmap.recycle()

            thumbFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create image thumbnail", e)
            null
        }
    }

    fun getVideoDuration(videoPath: String): Int? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            duration?.toIntOrNull()?.div(1000)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get video duration", e)
            null
        }
    }

    fun deleteFile(path: String): Boolean {
        return try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete file", e)
            false
        }
    }

    fun formatFileSize(bytes: Long?): String {
        if (bytes == null) return "未知"
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    fun formatDuration(seconds: Int?): String {
        if (seconds == null) return ""
        val minutes = seconds / 60
        val secs = seconds % 60
        return if (minutes > 0) {
            "${minutes}分${secs}秒"
        } else {
            "${secs}秒"
        }
    }

    fun isValidUrl(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            uri.scheme in listOf("http", "https") && uri.host?.isNotEmpty() == true
        } catch (e: Exception) {
            false
        }
    }
}
