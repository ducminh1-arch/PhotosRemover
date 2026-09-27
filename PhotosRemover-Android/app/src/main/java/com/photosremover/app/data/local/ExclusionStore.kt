package com.photosremover.app.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExclusionStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("remo_exclusions", Context.MODE_PRIVATE)

    private val _excludedIdsFlow = MutableStateFlow(getExcludedIds())
    val excludedIdsFlow: StateFlow<Set<Long>> = _excludedIdsFlow.asStateFlow()

    fun getExcludedIds(): Set<Long> =
        prefs.getStringSet(KEY_EXCLUDED_IDS, emptySet())
            ?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()

    fun isExcluded(id: Long): Boolean = id in getExcludedIds()

    fun toggleExclusion(id: Long): Boolean {
        val current = getExcludedIds().toMutableSet()
        val nowExcluded = if (current.contains(id)) {
            current.remove(id)
            false
        } else {
            current.add(id)
            true
        }
        prefs.edit().putStringSet(KEY_EXCLUDED_IDS, current.map { it.toString() }.toSet()).apply()
        _excludedIdsFlow.value = current
        return nowExcluded
    }

    fun addExclusion(id: Long) {
        val current = getExcludedIds().toMutableSet()
        if (current.add(id)) {
            prefs.edit().putStringSet(KEY_EXCLUDED_IDS, current.map { it.toString() }.toSet()).apply()
            _excludedIdsFlow.value = current
        }
    }

    fun removeExclusion(id: Long) {
        val current = getExcludedIds().toMutableSet()
        if (current.remove(id)) {
            prefs.edit().putStringSet(KEY_EXCLUDED_IDS, current.map { it.toString() }.toSet()).apply()
            _excludedIdsFlow.value = current
        }
    }

    fun clearAllExclusions() {
        prefs.edit().remove(KEY_EXCLUDED_IDS).apply()
        _excludedIdsFlow.value = emptySet()
    }

    val excludedCount: Int get() = getExcludedIds().size

    companion object {
        private const val KEY_EXCLUDED_IDS = "key_excluded_photo_ids"
    }
}
