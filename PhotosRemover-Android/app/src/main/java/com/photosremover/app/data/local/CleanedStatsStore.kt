package com.photosremover.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.photosremover.app.data.model.SimilarSensitivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CleanedStatsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cleanpix_stats", Context.MODE_PRIVATE)

    private val _cleanedCountFlow = MutableStateFlow(getCleanedCount())
    val cleanedCountFlow: StateFlow<Int> = _cleanedCountFlow.asStateFlow()

    private val _cleanedSizeFlow = MutableStateFlow(getCleanedSizeBytes())
    val cleanedSizeFlow: StateFlow<Long> = _cleanedSizeFlow.asStateFlow()

    private val _sensitivityFlow = MutableStateFlow(getSensitivity())
    val sensitivityFlow: StateFlow<SimilarSensitivity> = _sensitivityFlow.asStateFlow()

    fun getCleanedCount(): Int = prefs.getInt(KEY_CLEANED_COUNT, 0)
    fun getCleanedSizeBytes(): Long = prefs.getLong(KEY_CLEANED_SIZE_BYTES, 0L)

    fun getSensitivity(): SimilarSensitivity {
        val name = prefs.getString(KEY_SENSITIVITY, SimilarSensitivity.NORMAL.name)
        return try {
            SimilarSensitivity.valueOf(name ?: SimilarSensitivity.NORMAL.name)
        } catch (_: Exception) {
            SimilarSensitivity.NORMAL
        }
    }

    fun recordCleaned(count: Int, sizeBytes: Long) {
        val newCount = getCleanedCount() + count
        val newSize = getCleanedSizeBytes() + sizeBytes
        prefs.edit()
            .putInt(KEY_CLEANED_COUNT, newCount)
            .putLong(KEY_CLEANED_SIZE_BYTES, newSize)
            .apply()

        _cleanedCountFlow.value = newCount
        _cleanedSizeFlow.value = newSize
    }

    fun setSensitivity(sensitivity: SimilarSensitivity) {
        prefs.edit()
            .putString(KEY_SENSITIVITY, sensitivity.name)
            .apply()
        _sensitivityFlow.value = sensitivity
    }

    companion object {
        private const val KEY_CLEANED_COUNT = "key_cleaned_count"
        private const val KEY_CLEANED_SIZE_BYTES = "key_cleaned_size_bytes"
        private const val KEY_SENSITIVITY = "key_sensitivity"
    }
}
