import Foundation
import SwiftUI
import Photos

public enum AppScreen: Equatable {
    case home
    case scanning
    case results
    case preview
}

@MainActor
public final class MainViewModel: ObservableObject {
    @Published public var currentScreen: AppScreen = .home
    @Published public var scanProgress: ScanProgress = ScanProgress()
    @Published public var exactSets: [DuplicateSet] = []
    @Published public var similarSets: [DuplicateSet] = []
    @Published public var videoSets: [DuplicateSet] = []
    @Published public var selectedTab: DuplicateSetType = .exact
    @Published public var previewPhoto: PhotoItem?
    @Published public var previewSet: DuplicateSet?
    @Published public var isDrawerOpen: Bool = false
    @Published public var alertMessage: String?
    @Published public var showAlert: Bool = false
    @Published public var hasPermission: Bool = false
    @Published public var authStatus: PHAuthorizationStatus = .notDetermined
    @Published public var skippedICloudCount: Int = 0
    @Published public var storageBreakdown: StorageBreakdown? = nil

    public let statsStore = CleanedStatsStore.shared
    public let exclusionStore = ExclusionStore.shared
    private let libraryService = PhotoLibraryService.shared
    private let engine = DuplicateEngine()
    private var scanTask: Task<Void, Never>?

    public init() {
        checkPermission()
    }

    public func checkPermission() {
        let status = libraryService.authorizationStatus()
        self.authStatus = status
        self.hasPermission = (status == .authorized || status == .limited)
    }

    public func requestPermission() async {
        let status = await libraryService.requestAuthorization()
        self.authStatus = status
        self.hasPermission = (status == .authorized || status == .limited)
    }

    public func openSettings() {
        if let url = URL(string: UIApplication.openSettingsURLString), UIApplication.shared.canOpenURL(url) {
            UIApplication.shared.open(url)
        }
    }

    public func startScan() {
        scanTask?.cancel()
        currentScreen = .scanning
        scanProgress = ScanProgress(phase: .indexing)

        scanTask = Task {
            let excluded = exclusionStore.excludedIds

            // Phase 1: Photos scanning
            let photos = await libraryService.fetchAllPhotos(excludedIds: excluded)
            var currentExact: [DuplicateSet] = []
            var currentSimilar: [DuplicateSet] = []
            var skippedICloud = 0

            if !photos.isEmpty {
                await engine.scanLibrary(
                    photos: photos,
                    sensitivity: statsStore.sensitivity
                ) { [weak self] update in
                    guard let self = self else { return }
                    Task { @MainActor in
                        switch update {
                        case .progress(let progress):
                            self.scanProgress = progress
                        case .finished(let result):
                            currentExact = result.exactSets
                            currentSimilar = result.similarSets
                            skippedICloud = result.skippedICloudCount
                            self.exactSets = currentExact
                            self.similarSets = currentSimilar
                            self.skippedICloudCount = skippedICloud
                        }
                    }
                }
            }

            // Phase 2: Videos scanning
            let videos = await libraryService.fetchAllVideos(excludedIds: excluded)
            var currentVideos: [DuplicateSet] = []
            if !videos.isEmpty {
                await MainActor.run {
                    self.scanProgress = ScanProgress(
                        phase: .videoChecking,
                        current: 0,
                        total: videos.count,
                        exactSetsFound: currentExact.count,
                        similarSetsFound: currentSimilar.count
                    )
                }

                currentVideos = await engine.scanVideos(
                    videos: videos,
                    sensitivity: statsStore.sensitivity
                ) { [weak self] current, total, found in
                    guard let self = self else { return }
                    Task { @MainActor in
                        self.scanProgress = ScanProgress(
                            phase: .videoChecking,
                            current: current,
                            total: total,
                            exactSetsFound: currentExact.count,
                            similarSetsFound: currentSimilar.count,
                            videoSetsFound: found
                        )
                    }
                }
            }

            await MainActor.run {
                self.exactSets = currentExact
                self.similarSets = currentSimilar
                self.videoSets = currentVideos
                self.skippedICloudCount = skippedICloud

                let totalPhotoBytes = photos.reduce(0) { $0 + $1.sizeBytes }
                let totalVideoBytes = videos.reduce(0) { $0 + $1.sizeBytes }
                let photosDup = (currentExact + currentSimilar).reduce(0) { $0 + $1.totalSize }
                let videosDup = currentVideos.reduce(0) { $0 + $1.totalSize }
                let totalLib = totalPhotoBytes + totalVideoBytes
                let devStorage = DeviceStorageService.shared.queryDeviceStorage()
                self.storageBreakdown = StorageBreakdown(
                    photosTotalBytes: totalPhotoBytes,
                    photosDuplicateBytes: min(photosDup, totalPhotoBytes),
                    videosTotalBytes: totalVideoBytes,
                    videosDuplicateBytes: min(videosDup, totalVideoBytes),
                    librarySizeBytes: totalLib,
                    deviceTotalBytes: devStorage.totalBytes,
                    deviceFreeBytes: devStorage.freeBytes
                )

                self.scanProgress = ScanProgress(
                    phase: .completed,
                    current: photos.count + videos.count,
                    total: photos.count + videos.count,
                    exactSetsFound: currentExact.count,
                    similarSetsFound: currentSimilar.count,
                    videoSetsFound: currentVideos.count
                )

                // Auto-select tab with most items
                if !currentExact.isEmpty {
                    self.selectedTab = .exact
                } else if !currentSimilar.isEmpty {
                    self.selectedTab = .similar
                } else if !currentVideos.isEmpty {
                    self.selectedTab = .videos
                } else {
                    self.selectedTab = .exact
                }

                self.currentScreen = .results
            }
        }
    }

    public func cancelScan() {
        scanTask?.cancel()
        scanProgress = ScanProgress(phase: .cancelled)
        currentScreen = .home
    }

    public func openPreview(photo: PhotoItem, set: DuplicateSet) {
        self.previewPhoto = photo
        self.previewSet = set
        self.currentScreen = .preview
    }

    public func closePreview() {
        self.previewPhoto = nil
        self.previewSet = nil
        self.currentScreen = .results
    }

    private func getList(for tab: DuplicateSetType) -> [DuplicateSet] {
        switch tab {
        case .exact: return exactSets
        case .similar: return similarSets
        case .videos: return videoSets
        }
    }

    private func setList(_ list: [DuplicateSet], for tab: DuplicateSetType) {
        switch tab {
        case .exact: exactSets = list
        case .similar: similarSets = list
        case .videos: videoSets = list
        }
    }

    public func togglePhotoSelection(setId: String, photoId: String) {
        var list = getList(for: selectedTab)

        if let setIdx = list.firstIndex(where: { $0.id == setId }) {
            if let photoIdx = list[setIdx].photos.firstIndex(where: { $0.id == photoId }) {
                list[setIdx].photos[photoIdx].isSelected.toggle()
                setList(list, for: selectedTab)

                if previewPhoto?.id == photoId {
                    previewPhoto?.isSelected.toggle()
                }
            }
        }
    }

    public func toggleSetSelection(setId: String) {
        var list = getList(for: selectedTab)

        if let setIdx = list.firstIndex(where: { $0.id == setId }) {
            let willSelect = !list[setIdx].isAllSelected
            for i in 0..<list[setIdx].photos.count {
                if willSelect {
                    // Protect Best photo and Favorite photos when batch selecting
                    list[setIdx].photos[i].isSelected = !list[setIdx].photos[i].isBest && !list[setIdx].photos[i].isFavorite
                } else {
                    list[setIdx].photos[i].isSelected = false
                }
            }
            setList(list, for: selectedTab)
        }
    }

    public func selectAllExceptBest(setId: String) {
        var list = getList(for: selectedTab)

        if let setIdx = list.firstIndex(where: { $0.id == setId }) {
            for i in 0..<list[setIdx].photos.count {
                list[setIdx].photos[i].isSelected = !list[setIdx].photos[i].isBest && !list[setIdx].photos[i].isFavorite
            }
            setList(list, for: selectedTab)
        }
    }

    public func deleteSelectedPhotos() {
        let activeSets = getList(for: selectedTab)
        let selectedPhotos = activeSets.flatMap { $0.photos }.filter { $0.isSelected }
        guard !selectedPhotos.isEmpty else { return }

        let assetsToDelete = selectedPhotos.compactMap { $0.asset }
        let count = selectedPhotos.count
        let bytes = selectedPhotos.reduce(0) { $0 + $1.sizeBytes }

        Task {
            do {
                try await libraryService.deleteAssets(assetsToDelete)
                statsStore.recordCleaned(count: count, bytes: bytes)

                // Remove deleted photos
                let filterSets = { (sets: [DuplicateSet]) -> [DuplicateSet] in
                    return sets.compactMap { set in
                        let remaining = set.photos.filter { !itIsSelected($0, in: selectedPhotos) }
                        return remaining.count >= 2 ? DuplicateSet(id: set.id, title: set.title, type: set.type, photos: remaining) : nil
                    }
                }

                self.setList(filterSets(activeSets), for: self.selectedTab)

                if let current = self.storageBreakdown {
                    let newExact = self.exactSets
                    let newSimilar = self.similarSets
                    let newVideos = self.videoSets
                    let newPhotosDup = (newExact + newSimilar).reduce(0) { $0 + $1.totalSize }
                    let newVideosDup = newVideos.reduce(0) { $0 + $1.totalSize }
                    let isVideoTab = (self.selectedTab == .videos)
                    let devStorage = DeviceStorageService.shared.queryDeviceStorage()
                    self.storageBreakdown = StorageBreakdown(
                        photosTotalBytes: max(0, current.photosTotalBytes - (isVideoTab ? 0 : bytes)),
                        photosDuplicateBytes: min(newPhotosDup, current.photosTotalBytes),
                        videosTotalBytes: max(0, current.videosTotalBytes - (isVideoTab ? bytes : 0)),
                        videosDuplicateBytes: min(newVideosDup, current.videosTotalBytes),
                        librarySizeBytes: max(0, current.librarySizeBytes - bytes),
                        deviceTotalBytes: devStorage.totalBytes,
                        deviceFreeBytes: devStorage.freeBytes
                    )
                }

                let itemTypeStr = (self.selectedTab == .videos) ? "videos" : "photos"
                self.alertMessage = "Successfully cleaned \(count) \(itemTypeStr) (\(PhotoItem.formatByteSize(bytes)))!"
                self.showAlert = true
            } catch {
                self.alertMessage = "Failed to delete: \(error.localizedDescription)"
                self.showAlert = true
            }
        }
    }

    private func itIsSelected(_ photo: PhotoItem, in selected: [PhotoItem]) -> Bool {
        return selected.contains { $0.id == photo.id }
    }

    // ── Exceptions / Whitelist Handlers ─────────────────────────────────────────

    public func isExcluded(_ id: String) -> Bool {
        return exclusionStore.isExcluded(id)
    }

    public func toggleExclusion(for photo: PhotoItem) {
        toggleExclusion(photoId: photo.id)
    }

    public func toggleExclusion(photoId: String) {
        let nowExcluded = exclusionStore.toggleExclusion(photoId)
        if nowExcluded {
            // 1. Unselect if selected
            unselectItem(photoId)

            // 2. Remove from active displayed sets
            removePhotoFromActiveSets(photoId)

            self.alertMessage = "Đã loại trừ khỏi quét"
            self.showAlert = true
        } else {
            self.alertMessage = "Đã bỏ loại trừ — ảnh sẽ xuất hiện trong lần quét tiếp theo"
            self.showAlert = true
        }
    }

    private func unselectItem(_ id: String) {
        let unselectInSets = { (sets: [DuplicateSet]) -> [DuplicateSet] in
            sets.map { set in
                var updated = set
                for i in 0..<updated.photos.count {
                    if updated.photos[i].id == id {
                        updated.photos[i].isSelected = false
                    }
                }
                return updated
            }
        }
        exactSets = unselectInSets(exactSets)
        similarSets = unselectInSets(similarSets)
        videoSets = unselectInSets(videoSets)
    }

    private func removePhotoFromActiveSets(_ id: String) {
        let filterSets = { (sets: [DuplicateSet]) -> [DuplicateSet] in
            sets.compactMap { set -> DuplicateSet? in
                let remaining = set.photos.filter { $0.id != id }
                guard remaining.count >= 2 else { return nil }
                var updated = set
                var photos = remaining
                let hasBest = photos.contains { $0.isBest }
                if !hasBest && !photos.isEmpty {
                    photos[0].isBest = true
                    photos[0].isSelected = false
                }
                updated.photos = photos
                return updated
            }
        }

        exactSets = filterSets(exactSets)
        similarSets = filterSets(similarSets)
        videoSets = filterSets(videoSets)

        if previewPhoto?.id == id {
            closePreview()
        }
    }

    public func clearAllExclusions() {
        exclusionStore.clearAllExclusions()
        self.alertMessage = "Đã xóa toàn bộ danh sách loại trừ"
        self.showAlert = true
    }

    public func refreshDeviceStorage() {
        guard let current = storageBreakdown else { return }
        let devStorage = DeviceStorageService.shared.queryDeviceStorage()
        self.storageBreakdown = StorageBreakdown(
            photosTotalBytes: current.photosTotalBytes,
            photosDuplicateBytes: current.photosDuplicateBytes,
            videosTotalBytes: current.videosTotalBytes,
            videosDuplicateBytes: current.videosDuplicateBytes,
            librarySizeBytes: current.librarySizeBytes,
            deviceTotalBytes: devStorage.totalBytes,
            deviceFreeBytes: devStorage.freeBytes
        )
    }
}
