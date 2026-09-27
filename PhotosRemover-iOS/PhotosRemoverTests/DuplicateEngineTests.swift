import XCTest
import Photos
@testable import PhotosRemover

final class DuplicateEngineTests: XCTestCase {

    func testHammingDistance() {
        let engine = DuplicateEngine()
        let h1: UInt64 = 0b10101010
        let h2: UInt64 = 0b10101010
        XCTAssertEqual(engine.hammingDistance(h1, h2), 0)

        let h3: UInt64 = 0b10101011
        XCTAssertEqual(engine.hammingDistance(h1, h3), 1)

        let h4: UInt64 = 0b10111111
        XCTAssertEqual(engine.hammingDistance(h1, h4), 3)

        XCTAssertEqual(engine.hammingDistance(0, UInt64.max), 64)
    }

    func testFormatByteSize() {
        XCTAssertEqual(PhotoItem.formatByteSize(0), "0 B")
        XCTAssertEqual(PhotoItem.formatByteSize(500), "500 B")
        XCTAssertEqual(PhotoItem.formatByteSize(558 * 1024), "558 KB")
        XCTAssertEqual(PhotoItem.formatByteSize(Int64(12.7 * 1024 * 1024)), "12.7 MB")
        XCTAssertEqual(PhotoItem.formatByteSize(Int64(2.5 * 1024 * 1024 * 1024)), "2.50 GB")
    }

    func testSensitivityThresholds() {
        XCTAssertEqual(SimilarSensitivity.strict.maxHammingDistance, 3)
        XCTAssertEqual(SimilarSensitivity.normal.maxHammingDistance, 5)
        XCTAssertEqual(SimilarSensitivity.loose.maxHammingDistance, 10)
    }

    func testPickBestPhotoPriority() {
        let engine = DuplicateEngine()

        let normal = PhotoItem(
            id: "1",
            sizeBytes: 2_000_000,
            dateTaken: Date(timeIntervalSince1970: 1000),
            pixelWidth: 1920,
            pixelHeight: 1080,
            isFavorite: false
        )

        let highRes = PhotoItem(
            id: "2",
            sizeBytes: 4_000_000,
            dateTaken: Date(timeIntervalSince1970: 2000),
            pixelWidth: 4032,
            pixelHeight: 3024,
            isFavorite: false
        )

        let favorite = PhotoItem(
            id: "3",
            sizeBytes: 1_000_000,
            dateTaken: Date(timeIntervalSince1970: 3000),
            pixelWidth: 800,
            pixelHeight: 600,
            isFavorite: true
        )

        // 1. Favorite takes top priority
        let bestWithFav = engine.pickBestPhoto(from: [normal, highRes, favorite])
        XCTAssertEqual(bestWithFav.id, "3")

        // 2. Without favorite, high resolution wins
        let bestWithoutFav = engine.pickBestPhoto(from: [normal, highRes])
        XCTAssertEqual(bestWithoutFav.id, "2")
    }

    func testFavoritePhotosNeverAutoSelectedForDeletion() async {
        let engine = DuplicateEngine()

        let p1 = PhotoItem(
            id: "1",
            sizeBytes: 2_000_000,
            dateTaken: Date(timeIntervalSince1970: 1000),
            pixelWidth: 1920,
            pixelHeight: 1080,
            isFavorite: false,
            sha256: "exact_hash_1"
        )

        let p2Favorite = PhotoItem(
            id: "2",
            sizeBytes: 2_000_000,
            dateTaken: Date(timeIntervalSince1970: 2000),
            pixelWidth: 1920,
            pixelHeight: 1080,
            isFavorite: true,
            sha256: "exact_hash_1"
        )

        let (exactSets, _, _, _) = await engine.findExactDuplicates(photos: [p1, p2Favorite]) { _, _, _ in }
        XCTAssertEqual(exactSets.count, 1)

        let set = exactSets[0]
        let favInSet = set.photos.first { $0.id == "2" }
        XCTAssertNotNil(favInSet)
        XCTAssertFalse(favInSet!.isSelected, "Favorite photo must never be auto-selected for deletion!")
    }

    func testExactDuplicatesDifferentContentNotGrouped() async {
        let engine = DuplicateEngine()

        // Two photos with exact same file size (2,000,000 bytes) but different sha256 hashes
        let photo1 = PhotoItem(
            id: "photo_1",
            sizeBytes: 2_000_000,
            dateTaken: Date(timeIntervalSince1970: 1000),
            pixelWidth: 1920,
            pixelHeight: 1080,
            isFavorite: false,
            sha256: "sha256_content_AAA"
        )

        let photo2DifferentContent = PhotoItem(
            id: "photo_2",
            sizeBytes: 2_000_000,
            dateTaken: Date(timeIntervalSince1970: 2000),
            pixelWidth: 1920,
            pixelHeight: 1080,
            isFavorite: false,
            sha256: "sha256_content_BBB"
        )

        let (noGroupSets, _, _, _) = await engine.findExactDuplicates(photos: [photo1, photo2DifferentContent]) { _, _, _ in }
        XCTAssertTrue(noGroupSets.isEmpty, "Photos with same file size but different sha256 MUST NOT be grouped together!")

        // Third photo with same content as photo 1
        let photo3SameContent = PhotoItem(
            id: "photo_3",
            sizeBytes: 2_000_000,
            dateTaken: Date(timeIntervalSince1970: 3000),
            pixelWidth: 1920,
            pixelHeight: 1080,
            isFavorite: false,
            sha256: "sha256_content_AAA"
        )

        let (exactGroupSets, _, _, _) = await engine.findExactDuplicates(photos: [photo1, photo2DifferentContent, photo3SameContent]) { _, _, _ in }
        XCTAssertEqual(exactGroupSets.count, 1, "Only photos with identical sha256 should be grouped!")
        XCTAssertEqual(exactGroupSets[0].photos.count, 2)
        XCTAssertTrue(exactGroupSets[0].photos.contains { $0.id == "photo_1" })
        XCTAssertTrue(exactGroupSets[0].photos.contains { $0.id == "photo_3" })
        XCTAssertFalse(exactGroupSets[0].photos.contains { $0.id == "photo_2" })
    }

    func testSimilarSensitivityGroupingThreeLevels() {
        let engine = DuplicateEngine()
        let baseHash: UInt64 = 0x0000000000000000

        // Base photo
        let basePhoto = PhotoItem(
            id: "base",
            sizeBytes: 1_000_000,
            dateTaken: Date(timeIntervalSince1970: 1000),
            pixelWidth: 1000,
            pixelHeight: 1000,
            dHash: baseHash
        )

        // Photo A: 2 bits diff (0b11) -> matches Strict (<=3), Normal (<=5), Loose (<=10)
        let photoA = PhotoItem(
            id: "photoA_2bits",
            sizeBytes: 1_000_000,
            dateTaken: Date(timeIntervalSince1970: 1100),
            pixelWidth: 1000,
            pixelHeight: 1000,
            dHash: 0b11
        )

        // Photo B: 4 bits diff (0b1111) -> matches Normal (<=5) and Loose (<=10), but NOT Strict (<=3)
        let photoB = PhotoItem(
            id: "photoB_4bits",
            sizeBytes: 1_000_000,
            dateTaken: Date(timeIntervalSince1970: 1200),
            pixelWidth: 1000,
            pixelHeight: 1000,
            dHash: 0b1111
        )

        // Photo C: 8 bits diff (0xFF) -> matches Loose (<=10), but NOT Strict or Normal
        let photoC = PhotoItem(
            id: "photoC_8bits",
            sizeBytes: 1_000_000,
            dateTaken: Date(timeIntervalSince1970: 1300),
            pixelWidth: 1000,
            pixelHeight: 1000,
            dHash: 0xFF
        )

        // Strict (threshold <= 3)
        let strictSets = engine.groupSimilarPhotos(photosWithHash: [basePhoto, photoA, photoB, photoC], sensitivity: .strict)
        XCTAssertEqual(strictSets.count, 1)
        XCTAssertEqual(strictSets[0].photos.count, 2)
        XCTAssertTrue(strictSets[0].photos.contains { $0.id == "photoA_2bits" })
        XCTAssertFalse(strictSets[0].photos.contains { $0.id == "photoB_4bits" })
        XCTAssertFalse(strictSets[0].photos.contains { $0.id == "photoC_8bits" })

        // Normal (threshold <= 5)
        let normalSets = engine.groupSimilarPhotos(photosWithHash: [basePhoto, photoA, photoB, photoC], sensitivity: .normal)
        XCTAssertEqual(normalSets.count, 1)
        XCTAssertEqual(normalSets[0].photos.count, 3)
        XCTAssertTrue(normalSets[0].photos.contains { $0.id == "photoA_2bits" })
        XCTAssertTrue(normalSets[0].photos.contains { $0.id == "photoB_4bits" })
        XCTAssertFalse(normalSets[0].photos.contains { $0.id == "photoC_8bits" })

        // Loose (threshold <= 10)
        let looseSets = engine.groupSimilarPhotos(photosWithHash: [basePhoto, photoA, photoB, photoC], sensitivity: .loose)
        XCTAssertEqual(looseSets.count, 1)
        XCTAssertEqual(looseSets[0].photos.count, 4)
        XCTAssertTrue(looseSets[0].photos.contains { $0.id == "photoC_8bits" })
    }

    func testNoPhotoAppearsInBothExactAndSimilar() async {
        let engine = DuplicateEngine()

        // Exact duplicates E1 and E2
        let e1 = PhotoItem(
            id: "E1",
            sizeBytes: 2_000_000,
            dateTaken: Date(timeIntervalSince1970: 1000),
            pixelWidth: 1000,
            pixelHeight: 1000,
            sha256: "hash_E",
            dHash: 0x1234
        )

        let e2 = PhotoItem(
            id: "E2",
            sizeBytes: 2_000_000,
            dateTaken: Date(timeIntervalSince1970: 2000),
            pixelWidth: 1000,
            pixelHeight: 1000,
            sha256: "hash_E",
            dHash: 0x1234
        )

        // Photo S: visually similar to E1 (dHash 0x1235, distance 1), but different sha256
        let s = PhotoItem(
            id: "S",
            sizeBytes: 3_000_000,
            dateTaken: Date(timeIntervalSince1970: 3000),
            pixelWidth: 1000,
            pixelHeight: 1000,
            sha256: "hash_S",
            dHash: 0x1235
        )

        var finalResult: EngineScanResult?
        await engine.scanLibrary(photos: [e1, e2, s], sensitivity: .normal) { update in
            if case .finished(let result) = update {
                finalResult = result
            }
        }

        XCTAssertNotNil(finalResult)
        let exactIds = Set(finalResult!.exactSets.flatMap { $0.photos.map { $0.id } })
        let similarIds = Set(finalResult!.similarSets.flatMap { $0.photos.map { $0.id } })

        // Ensure non-representative exact duplicate E2 NEVER appears in Similar tab
        XCTAssertFalse(similarIds.contains("E2"), "Non-representative exact duplicate must NEVER appear in Similar tab!")
        // Ensure E1 and E2 are in Exact tab
        XCTAssertTrue(exactIds.contains("E1") && exactIds.contains("E2"))
    }

    func testBenchmarkPairwiseComparisons10kAnd30k() {
        var hashes10k = [UInt64](repeating: 0, count: 10_000)
        for i in 0..<10_000 {
            hashes10k[i] = UInt64(i * 31 + 7) ^ (UInt64(i) << 16)
        }

        let result10k = DuplicateEngine.benchmarkPairwiseComparisons(hashes: hashes10k)
        print("iOS Benchmark 10k: \(result10k.durationMs) ms")
        XCTAssertLessThan(result10k.durationMs, 5000.0)

        var hashes30k = [UInt64](repeating: 0, count: 30_000)
        for i in 0..<30_000 {
            hashes30k[i] = UInt64(i * 31 + 7) ^ (UInt64(i) << 16)
        }

        let result30k = DuplicateEngine.benchmarkPairwiseComparisons(hashes: hashes30k)
        print("iOS Benchmark 30k: \(result30k.durationMs) ms")
        XCTAssertLessThan(result30k.durationMs, 5000.0, "Pairwise comparisons for 30,000 photos should take well under 5 seconds")
    }

    func testVideoExactDuplicateDetection() async {
        let engine = DuplicateEngine()
        let v1 = PhotoItem(
            id: "V1",
            sizeBytes: 50_000_000,
            dateTaken: Date(timeIntervalSince1970: 1000),
            pixelWidth: 1920,
            pixelHeight: 1080,
            isVideo: true,
            durationSeconds: 15.0,
            sha256: "video_hash_abc"
        )
        let v2 = PhotoItem(
            id: "V2",
            sizeBytes: 50_000_000,
            dateTaken: Date(timeIntervalSince1970: 2000),
            pixelWidth: 1920,
            pixelHeight: 1080,
            isVideo: true,
            durationSeconds: 15.0,
            sha256: "video_hash_abc"
        )
        let v3 = PhotoItem(
            id: "V3",
            sizeBytes: 30_000_000,
            dateTaken: Date(timeIntervalSince1970: 3000),
            pixelWidth: 1920,
            pixelHeight: 1080,
            isVideo: true,
            durationSeconds: 10.0,
            sha256: "video_hash_xyz"
        )

        let sets = await engine.scanVideos(videos: [v1, v2, v3], sensitivity: .normal) { _, _, _ in }
        XCTAssertEqual(sets.count, 1)
        XCTAssertEqual(sets.first?.type, .videos)
        XCTAssertEqual(sets.first?.photos.count, 2)
    }

    func testExclusionStoreToggleAndClear() {
        let store = ExclusionStore.shared
        store.clearAllExclusions()
        XCTAssertEqual(store.count, 0)
        XCTAssertFalse(store.isExcluded("asset_100"))

        let added = store.toggleExclusion("asset_100")
        XCTAssertTrue(added)
        XCTAssertTrue(store.isExcluded("asset_100"))
        XCTAssertEqual(store.count, 1)

        let removed = store.toggleExclusion("asset_100")
        XCTAssertFalse(removed)
        XCTAssertFalse(store.isExcluded("asset_100"))
        XCTAssertEqual(store.count, 0)
    }

    func testExclusionFiltersOutPhotoBeforeDuplicateScan() async {
        let engine = DuplicateEngine()

        let p1 = PhotoItem(
            id: "photo_1",
            sizeBytes: 1_000_000,
            dateTaken: Date(timeIntervalSince1970: 1000),
            pixelWidth: 1000,
            pixelHeight: 1000,
            sha256: "exact_hash_xyz"
        )
        let p2 = PhotoItem(
            id: "photo_2",
            sizeBytes: 1_000_000,
            dateTaken: Date(timeIntervalSince1970: 2000),
            pixelWidth: 1000,
            pixelHeight: 1000,
            sha256: "exact_hash_xyz"
        )
        let p3 = PhotoItem(
            id: "photo_3",
            sizeBytes: 1_000_000,
            dateTaken: Date(timeIntervalSince1970: 3000),
            pixelWidth: 1000,
            pixelHeight: 1000,
            sha256: "exact_hash_xyz"
        )

        // All 3 identical photos without exclusion -> 1 set of 3 photos
        let allPhotos = [p1, p2, p3]
        let (allSets, _, _, _) = await engine.findExactDuplicates(photos: allPhotos) { _, _, _ in }
        XCTAssertEqual(allSets.count, 1)
        XCTAssertEqual(allSets[0].photos.count, 3)

        // Exclude photo_1 before scanning -> only 2 photos in set (photo_2 and photo_3)
        let excludedIds: Set<String> = ["photo_1"]
        let filteredPhotos = allPhotos.filter { !excludedIds.contains($0.id) }
        let (filteredSets, _, _, _) = await engine.findExactDuplicates(photos: filteredPhotos) { _, _, _ in }
        XCTAssertEqual(filteredSets.count, 1)
        XCTAssertEqual(filteredSets[0].photos.count, 2)
        XCTAssertFalse(filteredSets[0].photos.contains { $0.id == "photo_1" })
        XCTAssertTrue(filteredSets[0].photos.contains { $0.id == "photo_2" })
        XCTAssertTrue(filteredSets[0].photos.contains { $0.id == "photo_3" })

        // Exclude photo_2 as well -> only 1 photo remains -> sets count is 0 (set is discarded)
        let doubleExcluded: Set<String> = ["photo_1", "photo_2"]
        let remainingSingle = allPhotos.filter { !doubleExcluded.contains($0.id) }
        let (singleSets, _, _, _) = await engine.findExactDuplicates(photos: remainingSingle) { _, _, _ in }
        XCTAssertEqual(singleSets.count, 0, "Set with only 1 photo should not be formed")
    }

    func testStorageBreakdownCalculations() {
        let totalPhotoBytes: Int64 = 25_000_000
        let dupPhotoBytes: Int64 = 10_000_000
        let totalVideoBytes: Int64 = 60_000_000
        let dupVideoBytes: Int64 = 25_000_000
        let totalLibraryBytes: Int64 = totalPhotoBytes + totalVideoBytes

        let breakdown = StorageBreakdown(
            photosTotalBytes: totalPhotoBytes,
            photosDuplicateBytes: dupPhotoBytes,
            videosTotalBytes: totalVideoBytes,
            videosDuplicateBytes: dupVideoBytes,
            librarySizeBytes: totalLibraryBytes
        )

        // 1. photosTotalBytes matches cumulative sum
        XCTAssertEqual(breakdown.photosTotalBytes, 25_000_000)

        // 2. photosDuplicateBytes <= photosTotalBytes and non-negative
        XCTAssertTrue(breakdown.photosDuplicateBytes <= breakdown.photosTotalBytes)
        XCTAssertTrue(breakdown.photosDuplicateBytes >= 0)

        // 3. Unique bytes match expected difference
        XCTAssertEqual(breakdown.photosUniqueBytes, 15_000_000)
        XCTAssertEqual(breakdown.videosUniqueBytes, 35_000_000)
        XCTAssertEqual(breakdown.totalDuplicateBytes, 35_000_000)
        XCTAssertEqual(breakdown.librarySizeBytes, 85_000_000)
    }

    func testStorageBreakdownZeroAndEdgeCases() {
        let empty = StorageBreakdown(
            photosTotalBytes: 0,
            photosDuplicateBytes: 0,
            videosTotalBytes: 0,
            videosDuplicateBytes: 0,
            librarySizeBytes: 0
        )

        XCTAssertEqual(empty.photosUniqueBytes, 0)
        XCTAssertEqual(empty.videosUniqueBytes, 0)
        XCTAssertEqual(empty.totalDuplicateBytes, 0)
        XCTAssertEqual(empty.librarySizeBytes, 0)

        // Duplicate bytes exceed total (safety check)
        let safe = StorageBreakdown(
            photosTotalBytes: 10_000_000,
            photosDuplicateBytes: 15_000_000, // Should be clamped
            videosTotalBytes: 0,
            videosDuplicateBytes: 0,
            librarySizeBytes: 10_000_000
        )
        XCTAssertEqual(safe.photosUniqueBytes, 0)
    }

    func testDeviceStorageCalculateArcsSumAndThresholds() {
        let totalBytes: Int64 = 128_000_000_000 // 128 GB
        let freeBytes: Int64 = 40_000_000_000   // 40 GB
        let photosDup: Int64 = 100_000_000      // 100 MB (tiny relative to 128GB -> < 3 deg)
        let videosDup: Int64 = 500_000_000      // 500 MB
        let photosUnique: Int64 = 5_000_000_000 // 5 GB
        let videosUnique: Int64 = 8_000_000_000 // 8 GB
        let gapDegrees: Double = 1.5

        let segments = DeviceStorageService.calculateStorageArcs(
            totalDeviceBytes: totalBytes,
            freeDeviceBytes: freeBytes,
            photosDupBytes: photosDup,
            videosDupBytes: videosDup,
            photosUniqueBytes: photosUnique,
            videosUniqueBytes: videosUnique,
            gapDegrees: gapDegrees,
            minDegreeThreshold: 3.0
        )

        // 1. All segments with bytes > 0 have angle >= 3.0 degrees
        for segment in segments {
            if segment.bytes > 0 {
                XCTAssertGreaterThanOrEqual(
                    segment.sweepAngle,
                    2.99,
                    "Segment \(segment.label) (\(segment.bytes) bytes) sweep \(segment.sweepAngle) must be >= 3.0 deg"
                )
            }
        }

        // 2. Sum of all sweep angles + (count * gapDegrees) equals 360.0 degrees
        let totalSweeps = segments.reduce(0.0) { $0 + $1.sweepAngle }
        let totalGaps = Double(segments.count) * gapDegrees
        let totalCircle = totalSweeps + totalGaps
        XCTAssertEqual(totalCircle, 360.0, accuracy: 0.1, "Total circle must equal 360 degrees")

        // 3. Khác (Other) is never negative
        let other = segments.first { $0.id == "other" }
        XCTAssertNotNil(other)
        XCTAssertGreaterThanOrEqual(other!.bytes, 0, "Khác must be >= 0")
    }

    func testDeviceStorageCalculateArcsZeroAndSingleCategory() {
        // Zero total device storage -> does not crash, returns single 360 deg segment
        let emptyArcs = DeviceStorageService.calculateStorageArcs(
            totalDeviceBytes: 0,
            freeDeviceBytes: 0,
            photosDupBytes: 0,
            videosDupBytes: 0,
            photosUniqueBytes: 0,
            videosUniqueBytes: 0
        )
        XCTAssertFalse(emptyArcs.isEmpty)
        XCTAssertEqual(emptyArcs.first?.sweepAngle ?? 0, 360.0, accuracy: 0.01)

        // Machine almost full (free = 0)
        let fullArcs = DeviceStorageService.calculateStorageArcs(
            totalDeviceBytes: 64_000_000_000,
            freeDeviceBytes: 0,
            photosDupBytes: 1_000_000_000,
            videosDupBytes: 2_000_000_000,
            photosUniqueBytes: 5_000_000_000,
            videosUniqueBytes: 10_000_000_000
        )
        let fullSweeps = fullArcs.reduce(0.0) { $0 + $1.sweepAngle }
        let fullGaps = Double(fullArcs.count) * 1.5
        XCTAssertEqual(fullSweeps + fullGaps, 360.0, accuracy: 0.1)
    }
}
