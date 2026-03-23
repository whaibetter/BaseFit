package com.basefit.app.data.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

object AvatarManager {
    private const val AVATAR_DIR = "avatars"
    private const val MAX_AVATAR_SIZE = 256
    private const val AVATAR_QUALITY = 85
    private const val THUMBNAIL_SIZE = 64

    interface AvatarStorageAdapter {
        suspend fun saveAvatar(bitmap: Bitmap, userId: Long): String?
        suspend fun loadAvatar(userId: Long): Bitmap?
        suspend fun deleteAvatar(userId: Long): Boolean
        fun getAvatarPath(userId: Long): String?
    }

    class LocalStorageAdapter(private val context: Context) : AvatarStorageAdapter {
        private val avatarDir: File by lazy {
            File(context.filesDir, AVATAR_DIR).also { it.mkdirs() }
        }

        override suspend fun saveAvatar(bitmap: Bitmap, userId: Long): String? = withContext(Dispatchers.IO) {
            try {
                val file = File(avatarDir, "avatar_${userId}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, AVATAR_QUALITY, out)
                }
                file.absolutePath
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }

        override suspend fun loadAvatar(userId: Long): Bitmap? = withContext(Dispatchers.IO) {
            val file = File(avatarDir, "avatar_${userId}.jpg")
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else null
        }

        override suspend fun deleteAvatar(userId: Long): Boolean = withContext(Dispatchers.IO) {
            val file = File(avatarDir, "avatar_${userId}.jpg")
            file.delete()
        }

        override fun getAvatarPath(userId: Long): String? {
            val file = File(avatarDir, "avatar_${userId}.jpg")
            return if (file.exists()) file.absolutePath else null
        }
    }

    suspend fun processAndSaveAvatar(
        context: Context,
        sourceUri: Uri,
        userId: Long,
        storageAdapter: AvatarStorageAdapter = LocalStorageAdapter(context)
    ): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return@withContext null
            val originalBitmap = decodeSampledBitmap(inputStream, MAX_AVATAR_SIZE, MAX_AVATAR_SIZE)
            inputStream.close()

            if (originalBitmap == null) return@withContext null

            val rotatedBitmap = correctImageOrientation(context, sourceUri, originalBitmap)
            val scaledBitmap = scaleBitmap(rotatedBitmap, MAX_AVATAR_SIZE)

            if (rotatedBitmap != originalBitmap) {
                originalBitmap.recycle()
            }

            val result = storageAdapter.saveAvatar(scaledBitmap, userId)

            scaledBitmap.recycle()

            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeSampledBitmap(
        inputStream: InputStream,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        val bytes = inputStream.readBytes()

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun correctImageOrientation(
        context: Context,
        uri: Uri,
        bitmap: Bitmap
    ): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            inputStream.close()

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                else -> return bitmap
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            if (rotatedBitmap != bitmap) {
                bitmap.recycle()
            }
            rotatedBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        if (scaledBitmap != bitmap) {
            bitmap.recycle()
        }
        return scaledBitmap
    }

    fun getThumbnail(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= THUMBNAIL_SIZE && height <= THUMBNAIL_SIZE) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = THUMBNAIL_SIZE
            newHeight = (THUMBNAIL_SIZE / ratio).toInt()
        } else {
            newHeight = THUMBNAIL_SIZE
            newWidth = (THUMBNAIL_SIZE * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun createCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val canvas = android.graphics.Canvas(output)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
        }

        val rect = android.graphics.Rect(0, 0, size, size)
        val rectF = android.graphics.RectF(rect)

        canvas.drawOval(rectF, paint)

        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)

        val left = (bitmap.width - size) / 2
        val top = (bitmap.height - size) / 2
        val srcRect = android.graphics.Rect(left, top, left + size, top + size)

        canvas.drawBitmap(bitmap, srcRect, rect, paint)

        return output
    }

    interface CloudStorageAdapter {
        suspend fun uploadAvatar(localPath: String, userId: Long): String?
        suspend fun downloadAvatar(remoteUrl: String): Bitmap?
        fun getAvatarUrl(userId: Long): String?
    }
}