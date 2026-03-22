package com.basefit.app.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.basefit.app.data.entity.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*

/**
 * 媒体资源数据类
 */
data class MediaResource(
    val id: Long = 0,
    val exerciseId: Long,
    val type: MediaType,
    val fileName: String,
    val localPath: String?,      // 本地存储路径
    val remoteUrl: String? = null, // 远程URL（未来扩展）
    val thumbnailPath: String? = null, // 缩略图路径
    val orderIndex: Int = 0,     // 排序索引
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 媒体存储接口 - 提供可扩展性
 * 未来可以添加 CloudMediaStorage 实现
 */
interface MediaStorage {
    suspend fun saveMedia(exerciseId: Long, uri: Uri, type: MediaType): Result<MediaResource>
    suspend fun deleteMedia(localPath: String?): Result<Boolean>
    suspend fun getMediaPath(localPath: String?): String?
    suspend fun getThumbnail(localPath: String?, type: MediaType): Bitmap?
}

/**
 * 本地媒体存储实现
 */
class LocalMediaStorage(private val context: Context) : MediaStorage {
    
    private val mediaDir: File by lazy {
        File(context.filesDir, "exercise_media").apply {
            if (!exists()) mkdirs()
        }
    }
    
    override suspend fun saveMedia(exerciseId: Long, uri: Uri, type: MediaType): Result<MediaResource> = withContext(Dispatchers.IO) {
        try {
            // 为每个动作创建单独的目录
            val exerciseDir = File(mediaDir, exerciseId.toString()).apply {
                if (!exists()) mkdirs()
            }
            
            // 根据类型确定扩展名
            val extension = when (type) {
                MediaType.IMAGE -> "jpg"
                MediaType.GIF -> "gif"
                MediaType.VIDEO -> "mp4"
            }
            
            // 生成唯一文件名
            val fileName = "${UUID.randomUUID()}.$extension"
            val targetFile = File(exerciseDir, fileName)
            
            // 复制文件
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("无法打开文件"))
            
            // 如果是视频，生成缩略图
            var thumbnailPath: String? = null
            if (type == MediaType.VIDEO) {
                thumbnailPath = generateVideoThumbnail(targetFile, exerciseDir)
            }
            
            val media = MediaResource(
                exerciseId = exerciseId,
                type = type,
                fileName = fileName,
                localPath = targetFile.absolutePath,
                thumbnailPath = thumbnailPath
            )
            
            Result.success(media)
        } catch (e: Exception) {
            Log.e("LocalMediaStorage", "保存媒体失败", e)
            Result.failure(e)
        }
    }
    
    override suspend fun deleteMedia(localPath: String?): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            localPath?.let { path ->
                File(path).delete()
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e("LocalMediaStorage", "删除媒体失败", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getMediaPath(localPath: String?): String? = withContext(Dispatchers.IO) {
        localPath?.let { path ->
            if (File(path).exists()) path else null
        }
    }
    
    override suspend fun getThumbnail(localPath: String?, type: MediaType): Bitmap? = withContext(Dispatchers.IO) {
        if (type == MediaType.IMAGE || type == MediaType.GIF) {
            localPath?.let { BitmapFactory.decodeFile(it) }
        } else null
    }
    
    private fun generateVideoThumbnail(videoFile: File, outputDir: File): String? {
        return try {
            // 使用 MediaMetadataRetriever 提取视频缩略图
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(videoFile.absolutePath)
            val bitmap = retriever.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            
            bitmap?.let {
                val thumbnailFile = File(outputDir, "thumb_${videoFile.nameWithoutExtension}.jpg")
                FileOutputStream(thumbnailFile).use { output ->
                    it.compress(Bitmap.CompressFormat.JPEG, 85, output)
                }
                thumbnailFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e("LocalMediaStorage", "生成视频缩略图失败", e)
            null
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: LocalMediaStorage? = null
        
        fun getInstance(context: Context): LocalMediaStorage {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalMediaStorage(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}