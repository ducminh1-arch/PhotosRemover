package com.photosremover.app.data.repository

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import com.photosremover.app.data.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class PhotoRepository(private val context: Context) {

    private val contentResolver: ContentResolver get() = context.contentResolver

    suspend fun fetchAllPhotos(excludedIds: Set<Long> = emptySet()): List<PhotoItem> = withContext(Dispatchers.IO) {
        val photoList = mutableListOf<PhotoItem>()
        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.Images.Media.IS_FAVORITE)
            }
        }.toTypedArray()

        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                val dateTakenCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                val dateModifiedCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                val widthCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                val favoriteCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Images.Media.IS_FAVORITE)
                } else -1

                while (cursor.moveToNext()) {
                    if (idCol < 0) continue
                    val id = cursor.getLong(idCol)
                    if (id in excludedIds) continue
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val dateTaken = if (dateTakenCol >= 0) cursor.getLong(dateTakenCol) else 0L
                    val dateAdded = if (dateAddedCol >= 0) cursor.getLong(dateAddedCol) else 0L
                    val dateModified = if (dateModifiedCol >= 0) cursor.getLong(dateModifiedCol) else 0L
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 1000
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 1000

                    val isFavorite = if (favoriteCol >= 0) {
                        cursor.getInt(favoriteCol) == 1
                    } else false

                    // Timestamp fallback: dateTaken -> dateAdded * 1000 -> dateModified * 1000
                    val finalDate = when {
                        dateTaken > 0 -> dateTaken
                        dateAdded > 0 -> dateAdded * 1000L
                        dateModified > 0 -> dateModified * 1000L
                        else -> System.currentTimeMillis()
                    }

                    if (size > 0) {
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        photoList.add(
                            PhotoItem(
                                id = id,
                                uri = contentUri,
                                sizeBytes = size,
                                dateTakenMs = finalDate,
                                width = if (width > 0) width else 1000,
                                height = if (height > 0) height else 1000,
                                isFavorite = isFavorite
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        photoList
    }

    fun openInputStream(uri: Uri): InputStream? {
        return try {
            contentResolver.openInputStream(uri)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun loadThumbnail(uri: Uri, targetWidth: Int = 64, targetHeight: Int = 64): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentResolver.loadThumbnail(uri, Size(targetWidth, targetHeight), null)
                } else {
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, options)
                    }

                    var inSampleSize = 1
                    if (options.outHeight > targetHeight || options.outWidth > targetWidth) {
                        val halfHeight = options.outHeight / 2
                        val halfWidth = options.outWidth / 2
                        while ((halfHeight / inSampleSize) >= targetHeight &&
                            (halfWidth / inSampleSize) >= targetWidth
                        ) {
                            inSampleSize *= 2
                        }
                    }

                    val decodeOptions = BitmapFactory.Options().apply {
                        this.inSampleSize = inSampleSize
                    }
                    contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, decodeOptions)
                    }
                }
            } catch (_: Exception) {
                null
            }
        }

    sealed class DeleteResult {
        object Success : DeleteResult()
        data class RequiresUserConsent(val intentSender: IntentSender) : DeleteResult()
        data class Error(val exception: Throwable) : DeleteResult()
    }

    suspend fun deletePhotos(uris: List<Uri>): DeleteResult = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext DeleteResult.Success

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Prefer createTrashRequest so users can restore if needed (retained for 30 days)
                val pendingIntent = try {
                    MediaStore.createTrashRequest(contentResolver, uris, true)
                } catch (_: Exception) {
                    MediaStore.createDeleteRequest(contentResolver, uris)
                }
                return@withContext DeleteResult.RequiresUserConsent(pendingIntent.intentSender)
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                // Android 10: try delete, catch RecoverableSecurityException
                for (uri in uris) {
                    try {
                        contentResolver.delete(uri, null, null)
                    } catch (secEx: SecurityException) {
                        if (secEx is RecoverableSecurityException) {
                            return@withContext DeleteResult.RequiresUserConsent(
                                secEx.userAction.actionIntent.intentSender
                            )
                        } else {
                            throw secEx
                        }
                    }
                }
                return@withContext DeleteResult.Success
            } else {
                // Android 9 and below
                for (uri in uris) {
                    contentResolver.delete(uri, null, null)
                }
                return@withContext DeleteResult.Success
            }
        } catch (e: Throwable) {
            DeleteResult.Error(e)
        }
    }
}
