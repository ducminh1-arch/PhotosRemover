package com.photosremover.app.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import com.photosremover.app.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class VideoRepository(private val context: Context) {

    private val contentResolver: ContentResolver get() = context.contentResolver

    suspend fun fetchAllVideos(excludedIds: Set<Long> = emptySet()): List<VideoItem> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<VideoItem>()
        val projection = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.Video.Media.IS_FAVORITE)
            }
        }.toTypedArray()

        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        try {
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val dateTakenCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val dateModifiedCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val favoriteCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Video.Media.IS_FAVORITE)
                } else -1

                while (cursor.moveToNext()) {
                    if (idCol < 0) continue
                    val id = cursor.getLong(idCol)
                    if (id in excludedIds) continue
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val dateTaken = if (dateTakenCol >= 0) cursor.getLong(dateTakenCol) else 0L
                    val dateAdded = if (dateAddedCol >= 0) cursor.getLong(dateAddedCol) else 0L
                    val dateModified = if (dateModifiedCol >= 0) cursor.getLong(dateModifiedCol) else 0L
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 1920
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 1080
                    val duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
                    val isFavorite = if (favoriteCol >= 0) cursor.getInt(favoriteCol) == 1 else false

                    val finalDate = when {
                        dateTaken > 0 -> dateTaken
                        dateAdded > 0 -> dateAdded * 1000L
                        dateModified > 0 -> dateModified * 1000L
                        else -> System.currentTimeMillis()
                    }

                    if (size > 0) {
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        videoList.add(
                            VideoItem(
                                id = id,
                                uri = contentUri,
                                sizeBytes = size,
                                dateTakenMs = finalDate,
                                durationMs = duration,
                                width = if (width > 0) width else 1920,
                                height = if (height > 0) height else 1080,
                                isFavorite = isFavorite
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        videoList
    }

    /**
     * Extract a single frame from the video at [timeMs] milliseconds.
     * Uses OPTION_CLOSEST_SYNC to get the nearest keyframe (fast).
     */
    suspend fun loadVideoFrame(uri: android.net.Uri, timeMs: Long): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val timeMicros = timeMs * 1000L
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    timeMicros,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    64, 64
                )
            } else {
                retriever.getFrameAtTime(timeMicros, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }
        } catch (_: Exception) {
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    fun openInputStream(uri: android.net.Uri): InputStream? {
        return try {
            contentResolver.openInputStream(uri)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun deleteVideos(uris: List<android.net.Uri>): PhotoRepository.DeleteResult =
        withContext(Dispatchers.IO) {
            if (uris.isEmpty()) return@withContext PhotoRepository.DeleteResult.Success
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val pendingIntent = try {
                        MediaStore.createTrashRequest(contentResolver, uris, true)
                    } catch (_: Exception) {
                        MediaStore.createDeleteRequest(contentResolver, uris)
                    }
                    PhotoRepository.DeleteResult.RequiresUserConsent(pendingIntent.intentSender)
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    for (uri in uris) {
                        try {
                            contentResolver.delete(uri, null, null)
                        } catch (secEx: SecurityException) {
                            if (secEx is android.app.RecoverableSecurityException) {
                                return@withContext PhotoRepository.DeleteResult.RequiresUserConsent(
                                    secEx.userAction.actionIntent.intentSender
                                )
                            } else throw secEx
                        }
                    }
                    PhotoRepository.DeleteResult.Success
                } else {
                    for (uri in uris) { contentResolver.delete(uri, null, null) }
                    PhotoRepository.DeleteResult.Success
                }
            } catch (e: Throwable) {
                PhotoRepository.DeleteResult.Error(e)
            }
        }
}
