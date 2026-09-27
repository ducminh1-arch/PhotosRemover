package com.photosremover.app.data.model

import android.net.Uri

data class VideoItem(
    val id: Long,
    val uri: Uri? = null,
    val sizeBytes: Long,
    val dateTakenMs: Long,
    val durationMs: Long,           // Video duration in milliseconds
    val width: Int,
    val height: Int,
    val isFavorite: Boolean = false,
    val sha256: String? = null,
    val frameHashes: LongArray? = null,  // 3 dHash values: [start, mid, end]
    val isBest: Boolean = false,
    val isSelected: Boolean = false
) {
    val formattedSize: String
        get() = PhotoItem.formatByteSize(sizeBytes)

    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
            }
        }

    val aspectRatio: Float
        get() = if (height > 0) width.toFloat() / height.toFloat() else 1f

    // equals/hashCode override needed because frameHashes is LongArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VideoItem) return false
        return id == other.id &&
                uri == other.uri &&
                sizeBytes == other.sizeBytes &&
                dateTakenMs == other.dateTakenMs &&
                durationMs == other.durationMs &&
                width == other.width &&
                height == other.height &&
                isFavorite == other.isFavorite &&
                sha256 == other.sha256 &&
                frameHashes.contentEquals(other.frameHashes) &&
                isBest == other.isBest &&
                isSelected == other.isSelected
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (uri?.hashCode() ?: 0)
        result = 31 * result + sizeBytes.hashCode()
        result = 31 * result + dateTakenMs.hashCode()
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + isFavorite.hashCode()
        result = 31 * result + (sha256?.hashCode() ?: 0)
        result = 31 * result + (frameHashes?.contentHashCode() ?: 0)
        result = 31 * result + isBest.hashCode()
        result = 31 * result + isSelected.hashCode()
        return result
    }
}

private fun LongArray?.contentEquals(other: LongArray?): Boolean {
    if (this == null && other == null) return true
    if (this == null || other == null) return false
    return this.contentEquals(other)
}
