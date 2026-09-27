package com.photosremover.app.data.model

data class StorageBreakdown(
    val photosTotalBytes: Long,
    val photosDuplicateBytes: Long,   // exact + similar, phần có thể xóa
    val videosTotalBytes: Long,
    val videosDuplicateBytes: Long,
    val librarySizeBytes: Long,       // tổng photos + videos
    val deviceTotalBytes: Long = 0L,  // tổng dung lượng toàn máy (ví dụ 128 GB)
    val deviceFreeBytes: Long = 0L    // dung lượng còn trống trên máy
) {
    val photosUniqueBytes: Long
        get() = (photosTotalBytes - photosDuplicateBytes).coerceAtLeast(0L)

    val videosUniqueBytes: Long
        get() = (videosTotalBytes - videosDuplicateBytes).coerceAtLeast(0L)

    val totalDuplicateBytes: Long
        get() = photosDuplicateBytes + videosDuplicateBytes

    val deviceUsedBytes: Long
        get() = (deviceTotalBytes - deviceFreeBytes).coerceAtLeast(0L)

    val otherBytes: Long
        get() {
            val mediaBytes = photosDuplicateBytes + videosDuplicateBytes + photosUniqueBytes + videosUniqueBytes
            return (deviceUsedBytes - mediaBytes).coerceAtLeast(0L)
        }
}
