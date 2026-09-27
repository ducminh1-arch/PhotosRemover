package com.photosremover.app.data.model

enum class ScanPhase {
    IDLE,
    INDEXING,
    EXACT_CHECKING,
    SIMILAR_CHECKING,
    VIDEO_CHECKING,
    COMPLETED,
    CANCELLED
}

data class ScanProgress(
    val phase: ScanPhase = ScanPhase.IDLE,
    val current: Int = 0,
    val total: Int = 0,
    val exactSetsFound: Int = 0,
    val similarSetsFound: Int = 0,
    val videoSetsFound: Int = 0
) {
    val progressFraction: Float
        get() = if (total > 0) (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
}

