package com.photosremover.app.data.model

enum class DuplicateSetType {
    EXACT,
    SIMILAR,
    EXACT_VIDEO,
    SIMILAR_VIDEO
}

data class DuplicateSet(
    val id: String,
    val title: String,
    val type: DuplicateSetType,
    val photos: List<PhotoItem> = emptyList(),
    val videos: List<VideoItem> = emptyList()
) {
    val isVideo: Boolean
        get() = type == DuplicateSetType.EXACT_VIDEO || type == DuplicateSetType.SIMILAR_VIDEO

    val totalSize: Long
        get() = if (isVideo) videos.sumOf { it.sizeBytes } else photos.sumOf { it.sizeBytes }

    val selectedCount: Int
        get() = if (isVideo) videos.count { it.isSelected } else photos.count { it.isSelected }

    val selectedSize: Long
        get() = if (isVideo) videos.filter { it.isSelected }.sumOf { it.sizeBytes }
                else photos.filter { it.isSelected }.sumOf { it.sizeBytes }

    val isAllSelected: Boolean
        get() = if (isVideo) videos.isNotEmpty() && videos.all { it.isSelected }
                else photos.isNotEmpty() && photos.all { it.isSelected }

    val hasBestPhoto: Boolean
        get() = if (isVideo) videos.any { it.isBest } else photos.any { it.isBest }

    val itemCount: Int
        get() = if (isVideo) videos.size else photos.size
}
