package com.photosremover.app.ui.viewmodel

import android.app.Application
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photosremover.app.data.local.CleanedStatsStore
import com.photosremover.app.data.local.ExclusionStore
import com.photosremover.app.data.model.DuplicateSet
import com.photosremover.app.data.model.DuplicateSetType
import com.photosremover.app.data.model.PhotoItem
import com.photosremover.app.data.model.ScanPhase
import com.photosremover.app.data.model.ScanProgress
import com.photosremover.app.data.model.SimilarSensitivity
import com.photosremover.app.data.model.StorageBreakdown
import com.photosremover.app.data.repository.PhotoRepository
import com.photosremover.app.data.repository.VideoRepository
import com.photosremover.app.domain.DeviceStorage
import com.photosremover.app.domain.DuplicateEngine
import com.photosremover.app.domain.VideoEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    SCANNING,
    RESULTS,
    PREVIEW
}

sealed class MainUiEvent {
    data class RequestDeleteConsent(val intentSender: IntentSender, val count: Int, val bytes: Long) : MainUiEvent()
    data class ShowToast(val message: String) : MainUiEvent()
    data class ShowError(val title: String, val message: String) : MainUiEvent()
}

// Tab types shown in the ResultsScreen
enum class ResultsTab {
    EXACT,
    SIMILAR,
    VIDEOS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PhotoRepository(application.applicationContext)
    private val videoRepository = VideoRepository(application.applicationContext)
    private val statsStore = CleanedStatsStore(application.applicationContext)
    val exclusionStore = ExclusionStore(application.applicationContext)
    private val engine = DuplicateEngine(repository)
    private val videoEngine = VideoEngine(videoRepository)

    val excludedIds: StateFlow<Set<Long>> = exclusionStore.excludedIdsFlow

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    // Photo sets
    private val _exactSets = MutableStateFlow<List<DuplicateSet>>(emptyList())
    val exactSets: StateFlow<List<DuplicateSet>> = _exactSets.asStateFlow()

    private val _similarSets = MutableStateFlow<List<DuplicateSet>>(emptyList())
    val similarSets: StateFlow<List<DuplicateSet>> = _similarSets.asStateFlow()

    // All library photos & videos (for large media sorting & cleaner)
    private val _allPhotos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val allPhotos: StateFlow<List<PhotoItem>> = _allPhotos.asStateFlow()

    private val _allVideos = MutableStateFlow<List<com.photosremover.app.data.model.VideoItem>>(emptyList())
    val allVideos: StateFlow<List<com.photosremover.app.data.model.VideoItem>> = _allVideos.asStateFlow()

    // Video sets
    private val _exactVideoSets = MutableStateFlow<List<DuplicateSet>>(emptyList())
    val exactVideoSets: StateFlow<List<DuplicateSet>> = _exactVideoSets.asStateFlow()

    private val _similarVideoSets = MutableStateFlow<List<DuplicateSet>>(emptyList())
    val similarVideoSets: StateFlow<List<DuplicateSet>> = _similarVideoSets.asStateFlow()

    private val _storageBreakdown = MutableStateFlow<StorageBreakdown?>(null)
    val storageBreakdown: StateFlow<StorageBreakdown?> = _storageBreakdown.asStateFlow()

    private val _selectedTab = MutableStateFlow(ResultsTab.EXACT)
    val selectedTab: StateFlow<ResultsTab> = _selectedTab.asStateFlow()

    private val _previewPhoto = MutableStateFlow<PhotoItem?>(null)
    val previewPhoto: StateFlow<PhotoItem?> = _previewPhoto.asStateFlow()

    private val _previewSet = MutableStateFlow<DuplicateSet?>(null)
    val previewSet: StateFlow<DuplicateSet?> = _previewSet.asStateFlow()

    val cleanedCount: StateFlow<Int> = statsStore.cleanedCountFlow
    val cleanedSize: StateFlow<Long> = statsStore.cleanedSizeFlow
    val sensitivity: StateFlow<SimilarSensitivity> = statsStore.sensitivityFlow

    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen.asStateFlow()

    private val _uiEvents = MutableSharedFlow<MainUiEvent>()
    val uiEvents: SharedFlow<MainUiEvent> = _uiEvents.asSharedFlow()

    private var scanJob: Job? = null
    private var pendingDeleteCount = 0
    private var pendingDeleteBytes = 0L
    private var pendingLargePhotosToDelete = listOf<PhotoItem>()
    private var pendingLargeVideosToDelete = listOf<com.photosremover.app.data.model.VideoItem>()

    fun setSensitivity(sensitivity: SimilarSensitivity) {
        statsStore.setSensitivity(sensitivity)
    }

    fun setDrawerOpen(open: Boolean) {
        _isDrawerOpen.value = open
    }

    fun setTab(tab: ResultsTab) {
        _selectedTab.value = tab
    }

    fun startScan() {
        scanJob?.cancel()
        _currentScreen.value = AppScreen.SCANNING
        _scanProgress.value = ScanProgress(phase = ScanPhase.INDEXING)

        scanJob = viewModelScope.launch {
            try {
                val excludedIds = exclusionStore.getExcludedIds()

                // ── Phase 1: Scan photos ───────────────────────────────────────
                val photos = try {
                    repository.fetchAllPhotos(excludedIds)
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
                _allPhotos.value = photos

                if (photos.isEmpty()) {
                    _exactSets.value = emptyList()
                    _similarSets.value = emptyList()
                } else {
                    engine.scanLibrary(photos, sensitivity.value).collect { update ->
                        when (update) {
                            is DuplicateEngine.ScanProgressUpdate.Progress -> {
                                _scanProgress.value = update.progress
                            }
                            is DuplicateEngine.ScanProgressUpdate.Finished -> {
                                _exactSets.value = update.result.exactSets
                                _similarSets.value = update.result.similarSets
                                _scanProgress.value = ScanProgress(
                                    phase = ScanPhase.VIDEO_CHECKING,
                                    current = 0,
                                    total = 0,
                                    exactSetsFound = update.result.exactSets.size,
                                    similarSetsFound = update.result.similarSets.size
                                )
                            }
                        }
                    }
                }

                // ── Phase 2: Scan videos (non-critical, isolated try-catch) ────
                // If video scanning fails for ANY reason, we still show photo results.
                var totalVideoBytes = 0L
                try {
                    val videos = videoRepository.fetchAllVideos(excludedIds)
                    _allVideos.value = videos
                    totalVideoBytes = videos.sumOf { it.sizeBytes }
                    if (videos.isEmpty()) {
                        _exactVideoSets.value = emptyList()
                        _similarVideoSets.value = emptyList()
                    } else {
                        videoEngine.scanVideos(videos, sensitivity.value).collect { update ->
                            when (update) {
                                is VideoEngine.ScanProgressUpdate.Progress -> {
                                    _scanProgress.value = update.progress.copy(
                                        exactSetsFound = _exactSets.value.size,
                                        similarSetsFound = _similarSets.value.size
                                    )
                                }
                                is VideoEngine.ScanProgressUpdate.Finished -> {
                                    _exactVideoSets.value = update.result.exactSets
                                    _similarVideoSets.value = update.result.similarSets
                                }
                            }
                        }
                    }
                } catch (_: kotlinx.coroutines.CancellationException) {
                    throw kotlinx.coroutines.CancellationException() // re-throw cancellation
                } catch (e: Exception) {
                    // Video scanning failed – log but don't crash the whole scan
                    e.printStackTrace()
                    _exactVideoSets.value = emptyList()
                    _similarVideoSets.value = emptyList()
                    // Show non-blocking note so user still sees photo results
                    _uiEvents.emit(MainUiEvent.ShowToast("Video scan skipped: ${e.javaClass.simpleName}"))
                }

                // ── Compute storage breakdown ─────────────────────────────────
                val totalPhotoBytes = photos.sumOf { it.sizeBytes }
                val photosDupBytes = (_exactSets.value + _similarSets.value).sumOf { it.totalSize }
                val videosDupBytes = (_exactVideoSets.value + _similarVideoSets.value).sumOf { it.totalSize }
                val totalLibraryBytes = totalPhotoBytes + totalVideoBytes
                val deviceStorage = DeviceStorage.queryDeviceStorage(getApplication())
                _storageBreakdown.value = StorageBreakdown(
                    photosTotalBytes = totalPhotoBytes,
                    photosDuplicateBytes = photosDupBytes.coerceAtMost(totalPhotoBytes),
                    videosTotalBytes = totalVideoBytes,
                    videosDuplicateBytes = videosDupBytes.coerceAtMost(totalVideoBytes),
                    librarySizeBytes = totalLibraryBytes,
                    deviceTotalBytes = deviceStorage.totalBytes,
                    deviceFreeBytes = deviceStorage.freeBytes
                )

                // ── Done: always navigate to RESULTS ──────────────────────────
                _scanProgress.value = ScanProgress(
                    phase = ScanPhase.COMPLETED,
                    current = photos.size,
                    total = photos.size,
                    exactSetsFound = _exactSets.value.size,
                    similarSetsFound = _similarSets.value.size,
                    videoSetsFound = _exactVideoSets.value.size + _similarVideoSets.value.size
                )

                // Auto-select tab with most results
                _selectedTab.value = when {
                    _exactSets.value.isNotEmpty() -> ResultsTab.EXACT
                    _similarSets.value.isNotEmpty() -> ResultsTab.SIMILAR
                    (_exactVideoSets.value + _similarVideoSets.value).isNotEmpty() -> ResultsTab.VIDEOS
                    else -> ResultsTab.EXACT
                }

                _currentScreen.value = AppScreen.RESULTS

            } catch (_: kotlinx.coroutines.CancellationException) {
                _scanProgress.value = ScanProgress(phase = ScanPhase.CANCELLED)
                _currentScreen.value = AppScreen.HOME
            } catch (e: SecurityException) {
                e.printStackTrace()
                _scanProgress.value = ScanProgress(phase = ScanPhase.IDLE)
                _currentScreen.value = AppScreen.HOME
                _uiEvents.emit(MainUiEvent.ShowError(
                    "Permission Denied",
                    "The app needs photo/video access permission. Please grant it in Settings and try again.\n\nError: ${e.message}"
                ))
            } catch (e: Exception) {
                e.printStackTrace()
                _scanProgress.value = ScanProgress(phase = ScanPhase.IDLE)
                _currentScreen.value = AppScreen.HOME
                _uiEvents.emit(MainUiEvent.ShowError(
                    "Scan Failed",
                    "An unexpected error occurred: ${e.javaClass.simpleName}\n${e.message ?: ""}\n\nPlease try again."
                ))
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _scanProgress.value = ScanProgress(phase = ScanPhase.CANCELLED)
        _currentScreen.value = AppScreen.HOME
    }

    fun navigateToHome() {
        _currentScreen.value = AppScreen.HOME
    }

    fun navigateToResults() {
        _currentScreen.value = AppScreen.RESULTS
    }

    fun openPreview(photo: PhotoItem, set: DuplicateSet) {
        _previewPhoto.value = photo
        _previewSet.value = set
        _currentScreen.value = AppScreen.PREVIEW
    }

    fun closePreview() {
        _previewPhoto.value = null
        _previewSet.value = null
        _currentScreen.value = AppScreen.RESULTS
    }

    // ── Selection helpers ─────────────────────────────────────────────────────

    fun togglePhotoSelection(setId: String, photoId: Long) {
        val updateList = fun(list: List<DuplicateSet>): List<DuplicateSet> {
            return list.map { set ->
                if (set.id == setId) {
                    if (set.isVideo) {
                        val updatedVideos = set.videos.map { v ->
                            if (v.id == photoId) v.copy(isSelected = !v.isSelected) else v
                        }
                        set.copy(videos = updatedVideos)
                    } else {
                        val updatedPhotos = set.photos.map { p ->
                            if (p.id == photoId) p.copy(isSelected = !p.isSelected) else p
                        }
                        set.copy(photos = updatedPhotos)
                    }
                } else set
            }
        }

        when (_selectedTab.value) {
            ResultsTab.EXACT -> _exactSets.value = updateList(_exactSets.value)
            ResultsTab.SIMILAR -> _similarSets.value = updateList(_similarSets.value)
            ResultsTab.VIDEOS -> {
                _exactVideoSets.value = updateList(_exactVideoSets.value)
                _similarVideoSets.value = updateList(_similarVideoSets.value)
            }
        }

        _previewPhoto.value?.let { current ->
            if (current.id == photoId) {
                _previewPhoto.value = current.copy(isSelected = !current.isSelected)
            }
        }
    }

    fun toggleSetSelection(setId: String) {
        val updateList = fun(list: List<DuplicateSet>): List<DuplicateSet> {
            return list.map { set ->
                if (set.id == setId) {
                    val willSelectAll = !set.isAllSelected
                    if (set.isVideo) {
                        val updatedVideos = set.videos.map { v ->
                            if (willSelectAll) v.copy(isSelected = !v.isBest && !v.isFavorite)
                            else v.copy(isSelected = false)
                        }
                        set.copy(videos = updatedVideos)
                    } else {
                        val updatedPhotos = set.photos.map { p ->
                            if (willSelectAll) p.copy(isSelected = !p.isBest && !p.isFavorite)
                            else p.copy(isSelected = false)
                        }
                        set.copy(photos = updatedPhotos)
                    }
                } else set
            }
        }

        when (_selectedTab.value) {
            ResultsTab.EXACT -> _exactSets.value = updateList(_exactSets.value)
            ResultsTab.SIMILAR -> _similarSets.value = updateList(_similarSets.value)
            ResultsTab.VIDEOS -> {
                _exactVideoSets.value = updateList(_exactVideoSets.value)
                _similarVideoSets.value = updateList(_similarVideoSets.value)
            }
        }
    }

    fun selectAllExceptBestInSet(setId: String) {
        val updateList = fun(list: List<DuplicateSet>): List<DuplicateSet> {
            return list.map { set ->
                if (set.id == setId) {
                    if (set.isVideo) {
                        set.copy(videos = set.videos.map { v ->
                            v.copy(isSelected = !v.isBest && !v.isFavorite)
                        })
                    } else {
                        set.copy(photos = set.photos.map { p ->
                            p.copy(isSelected = !p.isBest && !p.isFavorite)
                        })
                    }
                } else set
            }
        }

        when (_selectedTab.value) {
            ResultsTab.EXACT -> _exactSets.value = updateList(_exactSets.value)
            ResultsTab.SIMILAR -> _similarSets.value = updateList(_similarSets.value)
            ResultsTab.VIDEOS -> {
                _exactVideoSets.value = updateList(_exactVideoSets.value)
                _similarVideoSets.value = updateList(_similarVideoSets.value)
            }
        }
    }

    // ── Deletion ──────────────────────────────────────────────────────────────

    fun deleteSelectedPhotos() {
        viewModelScope.launch {
            when (_selectedTab.value) {
                ResultsTab.EXACT, ResultsTab.SIMILAR -> deleteSelectedPhotoItems()
                ResultsTab.VIDEOS -> deleteSelectedVideoItems()
            }
        }
    }

    private suspend fun deleteSelectedPhotoItems() {
        val activeSets = if (_selectedTab.value == ResultsTab.EXACT) _exactSets.value else _similarSets.value
        val selectedPhotos = activeSets.flatMap { it.photos }.filter { it.isSelected }
        if (selectedPhotos.isEmpty()) return

        val uris = selectedPhotos.mapNotNull { it.uri }
        pendingDeleteCount = selectedPhotos.size
        pendingDeleteBytes = selectedPhotos.sumOf { it.sizeBytes }

        when (val result = repository.deletePhotos(uris)) {
            is PhotoRepository.DeleteResult.Success ->
                onDeletionConfirmed(pendingDeleteCount, pendingDeleteBytes)
            is PhotoRepository.DeleteResult.RequiresUserConsent ->
                _uiEvents.emit(MainUiEvent.RequestDeleteConsent(result.intentSender, pendingDeleteCount, pendingDeleteBytes))
            is PhotoRepository.DeleteResult.Error ->
                _uiEvents.emit(MainUiEvent.ShowToast("Delete failed: ${result.exception.localizedMessage}"))
        }
    }

    private suspend fun deleteSelectedVideoItems() {
        val allVideoSets = _exactVideoSets.value + _similarVideoSets.value
        val selectedVideos = allVideoSets.flatMap { it.videos }.filter { it.isSelected }
        if (selectedVideos.isEmpty()) return

        val uris = selectedVideos.mapNotNull { it.uri }
        pendingDeleteCount = selectedVideos.size
        pendingDeleteBytes = selectedVideos.sumOf { it.sizeBytes }

        when (val result = videoRepository.deleteVideos(uris)) {
            is PhotoRepository.DeleteResult.Success ->
                onVideoDeletionConfirmed(pendingDeleteCount, pendingDeleteBytes)
            is PhotoRepository.DeleteResult.RequiresUserConsent ->
                _uiEvents.emit(MainUiEvent.RequestDeleteConsent(result.intentSender, pendingDeleteCount, pendingDeleteBytes))
            is PhotoRepository.DeleteResult.Error ->
                _uiEvents.emit(MainUiEvent.ShowToast("Delete failed: ${result.exception.localizedMessage}"))
        }
    }

    fun loadAllMediaIfNeeded() {
        viewModelScope.launch {
            val excludedIds = exclusionStore.getExcludedIds()
            if (_allPhotos.value.isEmpty()) {
                try {
                    _allPhotos.value = repository.fetchAllPhotos(excludedIds)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (_allVideos.value.isEmpty()) {
                try {
                    _allVideos.value = videoRepository.fetchAllVideos(excludedIds)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun deleteLargeMediaItems(
        photosToDelete: List<PhotoItem>,
        videosToDelete: List<com.photosremover.app.data.model.VideoItem>
    ) {
        viewModelScope.launch {
            val photoUris = photosToDelete.mapNotNull { it.uri }
            val videoUris = videosToDelete.mapNotNull { it.uri }
            val allUris = photoUris + videoUris
            if (allUris.isEmpty()) return@launch

            val totalCount = photosToDelete.size + videosToDelete.size
            val totalBytes = photosToDelete.sumOf { it.sizeBytes } + videosToDelete.sumOf { it.sizeBytes }

            pendingDeleteCount = totalCount
            pendingDeleteBytes = totalBytes
            pendingLargePhotosToDelete = photosToDelete
            pendingLargeVideosToDelete = videosToDelete

            // Try deleting photos
            if (photoUris.isNotEmpty()) {
                when (val result = repository.deletePhotos(photoUris)) {
                    is PhotoRepository.DeleteResult.RequiresUserConsent -> {
                        _uiEvents.emit(MainUiEvent.RequestDeleteConsent(result.intentSender, totalCount, totalBytes))
                        return@launch
                    }
                    is PhotoRepository.DeleteResult.Error -> {
                        _uiEvents.emit(MainUiEvent.ShowToast("Lỗi khi xóa ảnh: ${result.exception.localizedMessage}"))
                        return@launch
                    }
                    is PhotoRepository.DeleteResult.Success -> {}
                }
            }

            // Try deleting videos
            if (videoUris.isNotEmpty()) {
                when (val result = videoRepository.deleteVideos(videoUris)) {
                    is PhotoRepository.DeleteResult.RequiresUserConsent -> {
                        _uiEvents.emit(MainUiEvent.RequestDeleteConsent(result.intentSender, totalCount, totalBytes))
                        return@launch
                    }
                    is PhotoRepository.DeleteResult.Error -> {
                        _uiEvents.emit(MainUiEvent.ShowToast("Lỗi khi xóa video: ${result.exception.localizedMessage}"))
                        return@launch
                    }
                    is PhotoRepository.DeleteResult.Success -> {}
                }
            }

            onLargeMediaDeletionConfirmed(photosToDelete, videosToDelete, totalCount, totalBytes)
            pendingLargePhotosToDelete = emptyList()
            pendingLargeVideosToDelete = emptyList()
        }
    }

    fun onLargeMediaDeletionConfirmed(
        photosToDelete: List<PhotoItem>,
        videosToDelete: List<com.photosremover.app.data.model.VideoItem>,
        count: Int,
        bytes: Long
    ) {
        statsStore.recordCleaned(count, bytes)
        val deletedPhotoIds = photosToDelete.map { it.id }.toSet()
        val deletedVideoIds = videosToDelete.map { it.id }.toSet()

        _allPhotos.value = _allPhotos.value.filterNot { it.id in deletedPhotoIds }
        _allVideos.value = _allVideos.value.filterNot { it.id in deletedVideoIds }

        // Remove deleted items from existing duplicate sets if present
        val filterPhotoSets = fun(list: List<DuplicateSet>): List<DuplicateSet> {
            return list.mapNotNull { set ->
                val remaining = set.photos.filterNot { it.id in deletedPhotoIds }
                if (remaining.size >= 2) set.copy(photos = remaining) else null
            }
        }
        _exactSets.value = filterPhotoSets(_exactSets.value)
        _similarSets.value = filterPhotoSets(_similarSets.value)

        val filterVideoSets = fun(list: List<DuplicateSet>): List<DuplicateSet> {
            return list.mapNotNull { set ->
                val remaining = set.videos.filterNot { it.id in deletedVideoIds }
                if (remaining.size >= 2) set.copy(videos = remaining) else null
            }
        }
        _exactVideoSets.value = filterVideoSets(_exactVideoSets.value)
        _similarVideoSets.value = filterVideoSets(_similarVideoSets.value)

        refreshDeviceStorage()

        viewModelScope.launch {
            _uiEvents.emit(MainUiEvent.ShowToast("Đã dọn dẹp $count tệp lớn (${PhotoItem.formatByteSize(bytes)})!"))
        }
    }

    fun onDeletionConfirmed(count: Int, bytes: Long) {
        if (pendingLargePhotosToDelete.isNotEmpty() || pendingLargeVideosToDelete.isNotEmpty()) {
            onLargeMediaDeletionConfirmed(pendingLargePhotosToDelete, pendingLargeVideosToDelete, count, bytes)
            pendingLargePhotosToDelete = emptyList()
            pendingLargeVideosToDelete = emptyList()
            return
        }

        statsStore.recordCleaned(count, bytes)

        val filterSets = fun(list: List<DuplicateSet>): List<DuplicateSet> {
            return list.mapNotNull { set ->
                val remaining = set.photos.filter { !it.isSelected }
                if (remaining.size >= 2) set.copy(photos = remaining) else null
            }
        }

        if (_selectedTab.value == ResultsTab.EXACT) {
            _exactSets.value = filterSets(_exactSets.value)
        } else {
            _similarSets.value = filterSets(_similarSets.value)
        }

        _storageBreakdown.value?.let { current ->
            val newPhotosDup = (_exactSets.value + _similarSets.value).sumOf { it.totalSize }
            val dev = DeviceStorage.queryDeviceStorage(getApplication())
            _storageBreakdown.value = current.copy(
                photosTotalBytes = (current.photosTotalBytes - bytes).coerceAtLeast(0L),
                photosDuplicateBytes = newPhotosDup.coerceAtMost(current.photosTotalBytes),
                librarySizeBytes = (current.librarySizeBytes - bytes).coerceAtLeast(0L),
                deviceTotalBytes = dev.totalBytes,
                deviceFreeBytes = dev.freeBytes
            )
        }

        viewModelScope.launch {
            _uiEvents.emit(MainUiEvent.ShowToast("Cleaned $count photos (${PhotoItem.formatByteSize(bytes)})!"))
        }
    }

    private fun onVideoDeletionConfirmed(count: Int, bytes: Long) {
        statsStore.recordCleaned(count, bytes)

        val filterVideoSets = fun(list: List<DuplicateSet>): List<DuplicateSet> {
            return list.mapNotNull { set ->
                val remaining = set.videos.filter { !it.isSelected }
                if (remaining.size >= 2) set.copy(videos = remaining) else null
            }
        }

        _exactVideoSets.value = filterVideoSets(_exactVideoSets.value)
        _similarVideoSets.value = filterVideoSets(_similarVideoSets.value)

        _storageBreakdown.value?.let { current ->
            val newVideosDup = (_exactVideoSets.value + _similarVideoSets.value).sumOf { it.totalSize }
            val dev = DeviceStorage.queryDeviceStorage(getApplication())
            _storageBreakdown.value = current.copy(
                videosTotalBytes = (current.videosTotalBytes - bytes).coerceAtLeast(0L),
                videosDuplicateBytes = newVideosDup.coerceAtMost(current.videosTotalBytes),
                librarySizeBytes = (current.librarySizeBytes - bytes).coerceAtLeast(0L),
                deviceTotalBytes = dev.totalBytes,
                deviceFreeBytes = dev.freeBytes
            )
        }

        viewModelScope.launch {
            _uiEvents.emit(MainUiEvent.ShowToast("Cleaned $count videos (${PhotoItem.formatByteSize(bytes)})!"))
        }
    }

    fun refreshDeviceStorage() {
        val current = _storageBreakdown.value ?: return
        val dev = DeviceStorage.queryDeviceStorage(getApplication())
        val newPhotosDup = (_exactSets.value + _similarSets.value).sumOf { it.totalSize }
        val newVideosDup = (_exactVideoSets.value + _similarVideoSets.value).sumOf { it.totalSize }
        _storageBreakdown.value = current.copy(
            photosDuplicateBytes = newPhotosDup.coerceAtMost(current.photosTotalBytes),
            videosDuplicateBytes = newVideosDup.coerceAtMost(current.videosTotalBytes),
            deviceTotalBytes = dev.totalBytes,
            deviceFreeBytes = dev.freeBytes
        )
    }

    // ── Exceptions / Whitelist Handlers ─────────────────────────────────────────

    fun isExcluded(id: Long): Boolean = exclusionStore.isExcluded(id)

    fun toggleExclusion(id: Long) {
        val nowExcluded = exclusionStore.toggleExclusion(id)
        if (nowExcluded) {
            // 1. Deselect item so it is never deleted
            unselectItem(id)

            // 2. Remove from active displayed duplicate sets
            removePhotoFromActiveSets(id)

            viewModelScope.launch {
                _uiEvents.emit(MainUiEvent.ShowToast("Đã loại trừ khỏi quét"))
            }
        } else {
            viewModelScope.launch {
                _uiEvents.emit(MainUiEvent.ShowToast("Đã hủy loại trừ — ảnh sẽ xuất hiện trong lần quét tiếp theo"))
            }
        }
    }

    private fun unselectItem(id: Long) {
        val unselectFromList = fun(list: List<DuplicateSet>): List<DuplicateSet> {
            return list.map { set ->
                if (set.isVideo) {
                    set.copy(videos = set.videos.map { if (it.id == id) it.copy(isSelected = false) else it })
                } else {
                    set.copy(photos = set.photos.map { if (it.id == id) it.copy(isSelected = false) else it })
                }
            }
        }
        _exactSets.value = unselectFromList(_exactSets.value)
        _similarSets.value = unselectFromList(_similarSets.value)
        _exactVideoSets.value = unselectFromList(_exactVideoSets.value)
        _similarVideoSets.value = unselectFromList(_similarVideoSets.value)
    }

    private fun removePhotoFromActiveSets(id: Long) {
        val filterSets = fun(list: List<DuplicateSet>): List<DuplicateSet> {
            return list.mapNotNull { set ->
                if (set.isVideo) {
                    val remaining = set.videos.filterNot { it.id == id }
                    if (remaining.size <= 1) null
                    else {
                        val hasBest = remaining.any { it.isBest }
                        val updated = if (!hasBest && remaining.isNotEmpty()) {
                            remaining.mapIndexed { idx, item -> if (idx == 0) item.copy(isBest = true, isSelected = false) else item }
                        } else remaining
                        set.copy(videos = updated)
                    }
                } else {
                    val remaining = set.photos.filterNot { it.id == id }
                    if (remaining.size <= 1) null
                    else {
                        val hasBest = remaining.any { it.isBest }
                        val updated = if (!hasBest && remaining.isNotEmpty()) {
                            remaining.mapIndexed { idx, item -> if (idx == 0) item.copy(isBest = true, isSelected = false) else item }
                        } else remaining
                        set.copy(photos = updated)
                    }
                }
            }
        }

        _exactSets.value = filterSets(_exactSets.value)
        _similarSets.value = filterSets(_similarSets.value)
        _exactVideoSets.value = filterSets(_exactVideoSets.value)
        _similarVideoSets.value = filterSets(_similarVideoSets.value)

        // Close preview if the current previewed photo was excluded
        if (_previewPhoto.value?.id == id) {
            closePreview()
        }
    }

    fun clearAllExclusions() {
        exclusionStore.clearAllExclusions()
        viewModelScope.launch {
            _uiEvents.emit(MainUiEvent.ShowToast("Đã xóa toàn bộ danh sách loại trừ"))
        }
    }
}
