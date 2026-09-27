package com.photosremover.app.data.model

import android.net.Uri

data class PhotoItem(
    val id: Long,
    val uri: Uri? = null,
    val sizeBytes: Long,
    val dateTakenMs: Long,
    val width: Int,
    val height: Int,
    val isFavorite: Boolean = false,
    val sha256: String? = null,
    val dHash: Long? = null,
    val isBest: Boolean = false,
    val isSelected: Boolean = false
) {
    val formattedSize: String
        get() = formatByteSize(sizeBytes)

    val aspectRatio: Float
        get() = if (height > 0) width.toFloat() / height.toFloat() else 1f

    companion object {
        fun formatByteSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(java.util.Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(java.util.Locale.US, "%.0f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}
