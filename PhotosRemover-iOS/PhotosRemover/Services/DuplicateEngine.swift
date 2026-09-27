import Foundation
import UIKit
import Photos
import CryptoKit

public struct EngineScanResult {
    public let exactSets: [DuplicateSet]
    public let similarSets: [DuplicateSet]
    public let skippedICloudCount: Int
    public let totalLibraryPhotoBytes: Int64

    public init(
        exactSets: [DuplicateSet],
        similarSets: [DuplicateSet],
        skippedICloudCount: Int = 0,
        totalLibraryPhotoBytes: Int64 = 0
    ) {
        self.exactSets = exactSets
        self.similarSets = similarSets
        self.skippedICloudCount = skippedICloudCount
        self.totalLibraryPhotoBytes = totalLibraryPhotoBytes
    }
}

public enum ScanProgressUpdate {
    case progress(ScanProgress)
    case finished(EngineScanResult)
}

public final class DuplicateEngine {
    private let libraryService: PhotoLibraryService

    public init(libraryService: PhotoLibraryService = .shared) {
        self.libraryService = libraryService
    }

    public func scanLibrary(
        photos: [PhotoItem],
        sensitivity: SimilarSensitivity,
        onUpdate: @escaping (ScanProgressUpdate) -> Void
    ) async {
        let total = photos.count
        onUpdate(.progress(ScanProgress(phase: .indexing, current: 0, total: total)))

        // 1. EXACT DUPLICATE DETECTION
        let (exactSets, exactPhotoIds, representativeIds, skippedICloud) = await findExactDuplicates(
            photos: photos
        ) { current, totalToHash, exactFound in
            onUpdate(.progress(
                ScanProgress(
                    phase: .exactChecking,
                    current: current,
                    total: totalToHash,
                    exactSetsFound: exactFound
                )
            ))
        }

        // 2. PREPARE CANDIDATES FOR SIMILAR DETECTION
        // Rule: 1 representative from each Exact Set + all photos not in any Exact Set
        // Never include other exact duplicates so no photo appears in both tabs!
        let similarCandidates = photos.filter { photo in
            !exactPhotoIds.contains(photo.id) || representativeIds.contains(photo.id)
        }

        // 3. SIMILAR PHOTO DETECTION
        let similarSets = await findSimilarDuplicates(
            candidates: similarCandidates,
            sensitivity: sensitivity
        ) { current, totalToCompare, similarFound in
            onUpdate(.progress(
                ScanProgress(
                    phase: .similarChecking,
                    current: current,
                    total: totalToCompare,
                    exactSetsFound: exactSets.count,
                    similarSetsFound: similarFound
                )
            ))
        }

        let totalPhotoBytes = photos.reduce(0) { $0 + $1.sizeBytes }
        onUpdate(.finished(EngineScanResult(
            exactSets: exactSets,
            similarSets: similarSets,
            skippedICloudCount: skippedICloud,
            totalLibraryPhotoBytes: totalPhotoBytes
        )))
    }

    public func findExactDuplicates(
        photos: [PhotoItem],
        onProgress: @escaping (Int, Int, Int) -> Void
    ) async -> ([DuplicateSet], Set<String>, Set<String>, Int) {
        // Group by file size
        let sizeBuckets = Dictionary(grouping: photos, by: { $0.sizeBytes })
            .filter { $0.value.count >= 2 }

        let candidatePhotos = sizeBuckets.values.flatMap { $0 }
        let totalCandidates = candidatePhotos.count

        if totalCandidates == 0 {
            return ([], [], [], 0)
        }

        var processed = 0
        var foundCount = 0
        var skippedICloudCount = 0
        var hashGroups: [String: [PhotoItem]] = [:]

        for photo in candidatePhotos {
            var hash: String? = photo.sha256
            if hash == nil, let asset = photo.asset {
                hash = await computeSha256(for: asset)
                if hash == nil {
                    skippedICloudCount += 1
                }
            }
            if let h = hash {
                var itemWithHash = photo
                itemWithHash.sha256 = h
                hashGroups[h, default: []].append(itemWithHash)
            }
            processed += 1
            if processed % 5 == 0 || processed == totalCandidates {
                onProgress(processed, totalCandidates, foundCount)
            }
        }

        var exactSets: [DuplicateSet] = []
        var exactPhotoIds = Set<String>()
        var representativeIds = Set<String>()
        var setIndex = 1

        for (_, group) in hashGroups where group.count >= 2 {
            let bestPhoto = pickBestPhoto(from: group)
            let processedPhotos = group.map { item -> PhotoItem in
                var updated = item
                let isBest = (item.id == bestPhoto.id)
                updated.isBest = isBest
                // Auto-select rule for Exact: Keep Best, pre-select duplicates ONLY if not favorite
                updated.isSelected = !isBest && !item.isFavorite
                return updated
            }

            let set = DuplicateSet(
                id: "exact_\(setIndex)",
                title: "Set: \(setIndex)",
                type: .exact,
                photos: processedPhotos
            )
            exactSets.append(set)
            setIndex += 1
            foundCount += 1

            for p in group {
                exactPhotoIds.insert(p.id)
            }
            representativeIds.insert(bestPhoto.id)
        }

        // Sort exactSets by totalSize descending so heaviest duplicate sets appear first
        let sortedExactSets = exactSets
            .sorted { $0.totalSize > $1.totalSize }
            .enumerated()
            .map { idx, s in
                let sortedPhotos = s.photos.sorted { a, b in
                    if a.isBest != b.isBest { return a.isBest }
                    return a.sizeBytes > b.sizeBytes
                }
                return DuplicateSet(
                    id: "exact_\(idx + 1)",
                    title: "Set: \(idx + 1)",
                    type: s.type,
                    photos: sortedPhotos
                )
            }

        onProgress(totalCandidates, totalCandidates, foundCount)
        return (sortedExactSets, exactPhotoIds, representativeIds, skippedICloudCount)
    }

    private func findSimilarDuplicates(
        candidates: [PhotoItem],
        sensitivity: SimilarSensitivity,
        onProgress: @escaping (Int, Int, Int) -> Void
    ) async -> [DuplicateSet] {
        let total = candidates.count
        guard total >= 2 else { return [] }

        // Compute dHash for all candidates
        var photosWithHash: [PhotoItem] = []
        photosWithHash.reserveCapacity(total)
        var current = 0

        for photo in candidates {
            if let dHash = photo.dHash {
                photosWithHash.append(photo)
            } else if let asset = photo.asset, let hash = await computeDHash(for: asset) {
                var updated = photo
                updated.dHash = hash
                photosWithHash.append(updated)
            }
            current += 1
            if current % 10 == 0 || current == total {
                onProgress(current, total, 0)
            }
        }

        return groupSimilarPhotos(photosWithHash: photosWithHash, sensitivity: sensitivity)
    }

    public func groupSimilarPhotos(photosWithHash: [PhotoItem], sensitivity: SimilarSensitivity) -> [DuplicateSet] {
        guard photosWithHash.count >= 2 else { return [] }

        // Group by aspect ratio bucket (rounded to 1 decimal)
        let aspectBuckets = Dictionary(grouping: photosWithHash) { photo -> Float in
            return (photo.aspectRatio * 10).rounded() / 10.0
        }

        // Union-Find for grouping
        var parent: [String: String] = [:]

        func find(_ id: String) -> String {
            var root = id
            while let p = parent[root] {
                root = p
            }
            var curr = id
            while curr != root {
                let next = parent[curr] ?? root
                parent[curr] = root
                curr = next
            }
            return root
        }

        func union(_ id1: String, _ id2: String) {
            let root1 = find(id1)
            let root2 = find(id2)
            if root1 != root2 {
                parent[root1] = root2
            }
        }

        for (_, bucket) in aspectBuckets where bucket.count >= 2 {
            let count = bucket.count
            for i in 0..<count {
                let pA = bucket[i]
                guard let hashA = pA.dHash else { continue }
                for j in (i + 1)..<count {
                    let pB = bucket[j]
                    guard let hashB = pB.dHash else { continue }
                    let distance = hammingDistance(hashA, hashB)
                    if distance <= sensitivity.maxHammingDistance {
                        union(pA.id, pB.id)
                    }
                }
            }
        }

        // Collect grouped sets
        var grouped: [String: [PhotoItem]] = [:]
        for photo in photosWithHash {
            let root = find(photo.id)
            grouped[root, default: []].append(photo)
        }

        var similarSets: [DuplicateSet] = []
        var setIndex = 1

        for (_, group) in grouped where group.count >= 2 {
            let bestPhoto = pickBestPhoto(from: group)
            // Rule for Similar Tab: DO NOT pre-select! Mark Best only
            let processedPhotos = group.map { item -> PhotoItem in
                var updated = item
                updated.isBest = (item.id == bestPhoto.id)
                updated.isSelected = false
                return updated
            }

            let set = DuplicateSet(
                id: "similar_\(setIndex)",
                title: "Set: \(setIndex)",
                type: .similar,
                photos: processedPhotos
            )
            similarSets.append(set)
            setIndex += 1
        }

        // Sort similarSets by totalSize descending so heaviest duplicate sets appear first
        let sortedSimilarSets = similarSets
            .sorted { $0.totalSize > $1.totalSize }
            .enumerated()
            .map { idx, s in
                let sortedPhotos = s.photos.sorted { a, b in
                    if a.isBest != b.isBest { return a.isBest }
                    return a.sizeBytes > b.sizeBytes
                }
                return DuplicateSet(
                    id: "similar_\(idx + 1)",
                    title: "Set: \(idx + 1)",
                    type: s.type,
                    photos: sortedPhotos
                )
            }

        return sortedSimilarSets
    }

    public static func benchmarkPairwiseComparisons(hashes: [UInt64]) -> (pairCount: Int64, durationMs: Double) {
        let n = hashes.count
        var matchingPairs: Int64 = 0
        let start = DispatchTime.now()
        for i in 0..<n {
            let h1 = hashes[i]
            for j in (i + 1)..<n {
                let h2 = hashes[j]
                let dist = (h1 ^ h2).nonzeroBitCount
                if dist <= 5 {
                    matchingPairs += 1
                }
            }
        }
        let end = DispatchTime.now()
        let nanoTime = end.uptimeNanoseconds - start.uptimeNanoseconds
        let ms = Double(nanoTime) / 1_000_000.0
        return (matchingPairs, ms)
    }

    public func pickBestPhoto(from photos: [PhotoItem]) -> PhotoItem {
        return photos.sorted { a, b in
            if a.isFavorite != b.isFavorite {
                return a.isFavorite && !b.isFavorite
            }
            let resA = a.pixelWidth * a.pixelHeight
            let resB = b.pixelWidth * b.pixelHeight
            if resA != resB {
                return resA > resB
            }
            if a.sizeBytes != b.sizeBytes {
                return a.sizeBytes > b.sizeBytes
            }
            return a.dateTaken < b.dateTaken
        }.first ?? photos[0]
    }

    public func computeSha256(for asset: PHAsset) async -> String? {
        guard let data = await libraryService.requestAssetData(for: asset) else {
            return nil
        }
        let digest = SHA256.hash(data: data)
        return digest.compactMap { String(format: "%02x", $0) }.joined()
    }

    public func computeDHash(for asset: PHAsset) async -> UInt64? {
        guard let image = await libraryService.requestThumbnail(for: asset, targetSize: CGSize(width: 64, height: 64)) else {
            return nil
        }
        return computeDHash(from: image)
    }

    public func computeDHash(from image: UIImage) -> UInt64? {
        guard let cgImage = image.cgImage else { return nil }

        let width = 9
        let height = 8
        let bytesPerPixel = 1
        let bytesPerRow = width * bytesPerPixel
        var rawPixels = [UInt8](repeating: 0, count: width * height)

        let colorSpace = CGColorSpaceCreateDeviceGray()
        guard let context = CGContext(
            data: &rawPixels,
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: bytesPerRow,
            space: colorSpace,
            bitmapInfo: CGImageAlphaInfo.none.rawValue
        ) else {
            return nil
        }

        context.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))

        var hash: UInt64 = 0
        for y in 0..<8 {
            for x in 0..<8 {
                let left = rawPixels[y * 9 + x]
                let right = rawPixels[y * 9 + (x + 1)]
                if left > right {
                    hash |= (1 << (y * 8 + x))
                }
            }
        }
        return hash
    }

    public func hammingDistance(_ hash1: UInt64, _ hash2: UInt64) -> Int {
        return (hash1 ^ hash2).nonzeroBitCount
    }

    // MARK: - Video Duplicate Scanning

    public func scanVideos(
        videos: [PhotoItem],
        sensitivity: SimilarSensitivity,
        onProgress: @escaping (Int, Int, Int) -> Void
    ) async -> [DuplicateSet] {
        guard videos.count >= 2 else { return [] }

        // 1. Exact Video Duplicates via SHA-256
        let (exactVideoSets, exactVideoIds, representativeIds, _) = await findExactDuplicates(
            photos: videos
        ) { current, total, found in
            onProgress(current, total, found)
        }

        let typedExactVideoSets = exactVideoSets.map { set in
            DuplicateSet(id: "video_" + set.id, title: set.title, type: .videos, photos: set.photos)
        }

        // 2. Similar Video Candidates: 1 representative from exact + all non-exact
        let similarCandidates = videos.filter { v in
            !exactVideoIds.contains(v.id) || representativeIds.contains(v.id)
        }

        // 3. Similar Video Duplicates via 3-frame dHash & duration comparison
        let similarVideoSets = await findSimilarVideos(
            candidates: similarCandidates,
            sensitivity: sensitivity
        ) { current, total, found in
            onProgress(current, total, typedExactVideoSets.count + found)
        }

        return typedExactVideoSets + similarVideoSets
    }

    public func findSimilarVideos(
        candidates: [PhotoItem],
        sensitivity: SimilarSensitivity,
        onProgress: @escaping (Int, Int, Int) -> Void
    ) async -> [DuplicateSet] {
        guard candidates.count >= 2 else { return [] }

        var videosWithHashes: [(video: PhotoItem, hashes: [UInt64])] = []
        var processed = 0
        let total = candidates.count

        for video in candidates {
            if let asset = video.asset, video.durationSeconds > 0 {
                let t1 = video.durationSeconds * 0.1
                let t2 = video.durationSeconds * 0.5
                let t3 = video.durationSeconds * 0.9

                var frameHashes: [UInt64] = []
                for t in [t1, t2, t3] {
                    if let frame = await libraryService.requestVideoFrame(for: asset, at: t),
                       let hash = computeDHash(from: frame) {
                        frameHashes.append(hash)
                    }
                }
                if frameHashes.count == 3 {
                    videosWithHashes.append((video: video, hashes: frameHashes))
                }
            }
            processed += 1
            if processed % 3 == 0 || processed == total {
                onProgress(processed, total, 0)
            }
        }

        guard videosWithHashes.count >= 2 else { return [] }

        // Union-Find similarity grouping
        var parent: [String: String] = [:]
        func find(_ id: String) -> String {
            var root = id
            while let p = parent[root] { root = p }
            var curr = id
            while let p = parent[curr] {
                parent[curr] = root
                curr = p
            }
            return root
        }

        func union(_ id1: String, _ id2: String) {
            let root1 = find(id1)
            let root2 = find(id2)
            if root1 != root2 {
                parent[root1] = root2
            }
        }

        for i in 0..<videosWithHashes.count {
            let itemA = videosWithHashes[i]
            for j in (i + 1)..<videosWithHashes.count {
                let itemB = videosWithHashes[j]

                // Filter: duration difference > 20% -> not similar
                let maxDur = max(itemA.video.durationSeconds, itemB.video.durationSeconds)
                let durDiff = abs(itemA.video.durationSeconds - itemB.video.durationSeconds)
                if maxDur > 0 && (durDiff / maxDur) > 0.20 { continue }

                // Frame distance average across 3 frames
                var distSum = 0
                for k in 0..<3 {
                    distSum += hammingDistance(itemA.hashes[k], itemB.hashes[k])
                }
                let avgDist = distSum / 3
                if avgDist <= sensitivity.maxHammingDistance {
                    union(itemA.video.id, itemB.video.id)
                }
            }
        }

        var groupsMap: [String: [PhotoItem]] = [:]
        for item in videosWithHashes {
            let root = find(item.video.id)
            groupsMap[root, default: []].append(item.video)
        }

        var similarSets: [DuplicateSet] = []
        var setIndex = 1
        for (_, group) in groupsMap where group.count >= 2 {
            let best = pickBestPhoto(from: group)
            let processed = group.map { v in
                var mod = v
                mod.isBest = (v.id == best.id)
                mod.isSelected = false
                return mod
            }
            let set = DuplicateSet(
                id: "similar_video_\(setIndex)",
                title = "Set: \(setIndex)",
                type: .videos,
                photos: processed
            )
            similarSets.append(set)
            setIndex += 1
        }

        return similarSets
    }
}
