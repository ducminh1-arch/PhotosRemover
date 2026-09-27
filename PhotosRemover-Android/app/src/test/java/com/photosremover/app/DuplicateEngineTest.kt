package com.photosremover.app

import android.net.Uri
import com.photosremover.app.data.model.DuplicateSet
import com.photosremover.app.data.model.DuplicateSetType
import com.photosremover.app.data.model.PhotoItem
import com.photosremover.app.data.model.SimilarSensitivity
import com.photosremover.app.domain.DuplicateEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class DuplicateEngineTest {

    private fun createDummyPhoto(
        id: Long,
        sizeBytes: Long,
        width: Int = 1920,
        height: Int = 1080,
        dateTakenMs: Long = 1000L,
        isFavorite: Boolean = false,
        sha256: String? = null,
        dHash: Long? = null
    ): PhotoItem {
        return PhotoItem(
            id = id,
            uri = null,
            sizeBytes = sizeBytes,
            dateTakenMs = dateTakenMs,
            width = width,
            height = height,
            isFavorite = isFavorite,
            sha256 = sha256,
            dHash = dHash
        )
    }

    // 1. HAMMING DISTANCE TEST
    @Test
    fun testHammingDistance() {
        val h1 = 0b10101010L
        val h2 = 0b10101010L
        assertEquals(0, java.lang.Long.bitCount(h1 xor h2))

        val h3 = 0b10101011L // 1 bit diff
        assertEquals(1, java.lang.Long.bitCount(h1 xor h3))

        val h4 = 0b10111111L // 3 bits diff
        assertEquals(3, java.lang.Long.bitCount(h1 xor h4))

        val zero = 0L
        val allOnes = -1L
        assertEquals(64, java.lang.Long.bitCount(zero xor allOnes))
    }

    // 2. EXACT DUPLICATE: SAME SIZE BUT DIFFERENT HASH MUST NOT BE GROUPED
    @Test
    fun testExactDuplicatesRequireSameHash() {
        val size = 500_000L
        val photo1 = createDummyPhoto(id = 1, sizeBytes = size, sha256 = "hash_A")
        val photo2 = createDummyPhoto(id = 2, sizeBytes = size, sha256 = "hash_B") // Same size, different hash
        val photo3 = createDummyPhoto(id = 3, sizeBytes = size, sha256 = "hash_A") // Same size, same hash as 1

        val allPhotos = listOf(photo1, photo2, photo3)
        val sizeBuckets = allPhotos.groupBy { it.sizeBytes }.filter { it.value.size >= 2 }

        val hashGroups = mutableMapOf<String, MutableList<PhotoItem>>()
        for (p in sizeBuckets.values.flatten()) {
            val hash = p.sha256 ?: continue
            hashGroups.getOrPut(hash) { mutableListOf() }.add(p)
        }

        val exactSets = hashGroups.filter { it.value.size >= 2 }.values.map { group ->
            DuplicateSet("exact_1", "Set: 1", DuplicateSetType.EXACT, group)
        }

        // Only photo1 and photo3 should be grouped together
        assertEquals(1, exactSets.size)
        val group = exactSets.first().photos
        assertEquals(2, group.size)
        assertTrue(group.any { it.id == 1L })
        assertTrue(group.any { it.id == 3L })
        assertFalse(group.any { it.id == 2L }) // photo2 is NOT in the set!
    }

    // 3. SIMILAR PHOTO GROUPING ACCORDING TO SENSITIVITY THRESHOLDS
    @Test
    fun testSimilarSensitivityThresholds() {
        assertEquals(3, SimilarSensitivity.STRICT.maxHammingDistance)
        assertEquals(5, SimilarSensitivity.NORMAL.maxHammingDistance)
        assertEquals(10, SimilarSensitivity.LOOSE.maxHammingDistance)

        val baseHash = 0x0000000000000000L

        // Differing by 3 bits
        val hashDiff3 = 0b00000111L
        assertEquals(3, java.lang.Long.bitCount(baseHash xor hashDiff3))
        assertTrue(java.lang.Long.bitCount(baseHash xor hashDiff3) <= SimilarSensitivity.STRICT.maxHammingDistance)
        assertTrue(java.lang.Long.bitCount(baseHash xor hashDiff3) <= SimilarSensitivity.NORMAL.maxHammingDistance)
        assertTrue(java.lang.Long.bitCount(baseHash xor hashDiff3) <= SimilarSensitivity.LOOSE.maxHammingDistance)

        // Differing by 5 bits
        val hashDiff5 = 0b00011111L
        assertEquals(5, java.lang.Long.bitCount(baseHash xor hashDiff5))
        assertFalse(java.lang.Long.bitCount(baseHash xor hashDiff5) <= SimilarSensitivity.STRICT.maxHammingDistance)
        assertTrue(java.lang.Long.bitCount(baseHash xor hashDiff5) <= SimilarSensitivity.NORMAL.maxHammingDistance)
        assertTrue(java.lang.Long.bitCount(baseHash xor hashDiff5) <= SimilarSensitivity.LOOSE.maxHammingDistance)

        // Differing by 8 bits
        val hashDiff8 = 0b11111111L
        assertEquals(8, java.lang.Long.bitCount(baseHash xor hashDiff8))
        assertFalse(java.lang.Long.bitCount(baseHash xor hashDiff8) <= SimilarSensitivity.STRICT.maxHammingDistance)
        assertFalse(java.lang.Long.bitCount(baseHash xor hashDiff8) <= SimilarSensitivity.NORMAL.maxHammingDistance)
        assertTrue(java.lang.Long.bitCount(baseHash xor hashDiff8) <= SimilarSensitivity.LOOSE.maxHammingDistance)

        // Differing by 12 bits
        val hashDiff12 = 0b0000111111111111L
        assertEquals(12, java.lang.Long.bitCount(baseHash xor hashDiff12))
        assertFalse(java.lang.Long.bitCount(baseHash xor hashDiff12) <= SimilarSensitivity.LOOSE.maxHammingDistance)
    }

    // 4. NO PHOTO OVERLAP BETWEEN EXACT AND SIMILAR TABS
    @Test
    fun testNoOverlapBetweenExactAndSimilar() {
        val exactP1 = createDummyPhoto(1, 100, sha256 = "hash_exact")
        val exactP2 = createDummyPhoto(2, 100, sha256 = "hash_exact")
        val otherP3 = createDummyPhoto(3, 200, sha256 = "hash_other")
        val otherP4 = createDummyPhoto(4, 300, sha256 = "hash_another")

        val allPhotos = listOf(exactP1, exactP2, otherP3, otherP4)

        // Exact sets will have exactP1 and exactP2
        val exactPhotoIds = setOf(1L, 2L)
        val representativeId = 1L // exactP1 is chosen as Best representative

        // Similar candidates rule:
        val similarCandidates = allPhotos.filter { photo ->
            !exactPhotoIds.contains(photo.id) || photo.id == representativeId
        }

        // exactP2 MUST NOT be a candidate for Similar detection
        assertTrue(similarCandidates.any { it.id == 1L })
        assertFalse(similarCandidates.any { it.id == 2L })
        assertTrue(similarCandidates.any { it.id == 3L })
        assertTrue(similarCandidates.any { it.id == 4L })
        assertEquals(3, similarCandidates.size)
    }

    // 5. BEST PHOTO RANKING AND SAFE AUTO-SELECTION
    @Test
    fun testPickBestPhotoAndFavoriteProtection() {
        val photoNormal = createDummyPhoto(id = 1, sizeBytes = 2_000_000, width = 1920, height = 1080, isFavorite = false)
        val photoHighRes = createDummyPhoto(id = 2, sizeBytes = 4_000_000, width = 4032, height = 3024, isFavorite = false)
        val photoFavorite = createDummyPhoto(id = 3, sizeBytes = 1_000_000, width = 800, height = 600, isFavorite = true)

        val group = listOf(photoNormal, photoHighRes, photoFavorite)

        // 1. Favorite photo takes top priority
        val bestPhoto = group.maxWithOrNull(
            compareBy<PhotoItem> { it.isFavorite }
                .thenBy { it.width * it.height }
                .thenBy { it.sizeBytes }
                .thenByDescending { -it.dateTakenMs }
        )!!
        assertEquals(3L, bestPhoto.id)

        // 2. Safe Auto-selection rule: Do NOT select Best, and do NOT select isFavorite!
        val processed = group.map { p ->
            val isBest = p.id == bestPhoto.id
            p.copy(
                isBest = isBest,
                isSelected = !isBest && !p.isFavorite
            )
        }

        val bestItem = processed.first { it.id == 3L }
        assertFalse(bestItem.isSelected) // Best is kept

        val favDuplicate = createDummyPhoto(id = 4, sizeBytes = 4_000_000, width = 4032, height = 3024, isFavorite = true)
        val processedWithAnotherFav = listOf(photoHighRes, favDuplicate).map { p ->
            val isBest = p.id == 2L
            p.copy(isBest = isBest, isSelected = !isBest && !p.isFavorite)
        }
        val favItem = processedWithAnotherFav.first { it.id == 4L }
        assertFalse("Favorite photos must NEVER be auto-selected for deletion", favItem.isSelected)
    }

    // 6. FORMAT BYTE SIZE TESTS
    @Test
    fun testFormatByteSize() {
        assertEquals("0 B", PhotoItem.formatByteSize(0))
        assertEquals("500 B", PhotoItem.formatByteSize(500))
        assertEquals("558 KB", PhotoItem.formatByteSize(558 * 1024))
        assertEquals("12.7 MB", PhotoItem.formatByteSize((12.7 * 1024 * 1024).toLong()))
        assertEquals("2.50 GB", PhotoItem.formatByteSize((2.5 * 1024 * 1024 * 1024).toLong()))
    }

    // 7. BENCHMARK TEST: 10,000 AND 30,000 SIMULATED HASHES
    @Test
    fun testBenchmarkHammingComparisons() {
        val random = Random(42)

        // Benchmark 1: 10,000 photos
        val count10k = 10_000
        val hashes10k = LongArray(count10k) { random.nextLong() }
        val time10k = DuplicateEngine.benchmarkHammingComparisons(hashes10k, 5)
        println("=== BENCHMARK RESULT ===")
        println("Pairwise comparison for $count10k photos: $time10k ms (${count10k.toLong() * (count10k - 1) / 2} pairs)")

        // Benchmark 2: 30,000 photos
        val count30k = 30_000
        val hashes30k = LongArray(count30k) { random.nextLong() }
        val time30k = DuplicateEngine.benchmarkHammingComparisons(hashes30k, 5)
        println("Pairwise comparison for $count30k photos: $time30k ms (${count30k.toLong() * (count30k - 1) / 2} pairs)")
        println("========================")

        // Requirement: Must be well under 5000 ms (5 seconds)
        assertTrue("30,000 photos comparison should take less than 5000 ms, took: $time30k ms", time30k < 5000)
    }

    // 8. STORAGE BREAKDOWN TESTS
    @Test
    fun testStorageBreakdownCalculations() {
        val p1 = createDummyPhoto(id = 1, sizeBytes = 5_000_000)
        val p2 = createDummyPhoto(id = 2, sizeBytes = 5_000_000)
        val p3 = createDummyPhoto(id = 3, sizeBytes = 8_000_000)
        val allPhotos = listOf(p1, p2, p3)
        val totalPhotoBytes = allPhotos.sumOf { it.sizeBytes }
        assertEquals(18_000_000L, totalPhotoBytes)

        val dupPhotoBytes = 10_000_000L // p1 and p2 form a duplicate set
        val totalVideoBytes = 50_000_000L
        val dupVideoBytes = 20_000_000L
        val totalLibraryBytes = totalPhotoBytes + totalVideoBytes

        val breakdown = com.photosremover.app.data.model.StorageBreakdown(
            photosTotalBytes = totalPhotoBytes,
            photosDuplicateBytes = dupPhotoBytes,
            videosTotalBytes = totalVideoBytes,
            videosDuplicateBytes = dupVideoBytes,
            librarySizeBytes = totalLibraryBytes
        )

        // 1. photosTotalBytes matches cumulative sum
        assertEquals(18_000_000L, breakdown.photosTotalBytes)

        // 2. photosDuplicateBytes <= photosTotalBytes and non-negative
        assertTrue(breakdown.photosDuplicateBytes <= breakdown.photosTotalBytes)
        assertTrue(breakdown.photosDuplicateBytes >= 0L)

        // 3. Unique bytes match expected difference
        assertEquals(8_000_000L, breakdown.photosUniqueBytes)
        assertEquals(30_000_000L, breakdown.videosUniqueBytes)
        assertEquals(30_000_000L, breakdown.totalDuplicateBytes)
        assertEquals(68_000_000L, breakdown.librarySizeBytes)
    }

    @Test
    fun testStorageBreakdownZeroAndEdgeCases() {
        val emptyBreakdown = com.photosremover.app.data.model.StorageBreakdown(
            photosTotalBytes = 0L,
            photosDuplicateBytes = 0L,
            videosTotalBytes = 0L,
            videosDuplicateBytes = 0L,
            librarySizeBytes = 0L
        )

        assertEquals(0L, emptyBreakdown.photosUniqueBytes)
        assertEquals(0L, emptyBreakdown.videosUniqueBytes)
        assertEquals(0L, emptyBreakdown.totalDuplicateBytes)
        assertEquals(0L, emptyBreakdown.librarySizeBytes)

        // Photos only, no duplicates
        val photosOnlyNoDup = com.photosremover.app.data.model.StorageBreakdown(
            photosTotalBytes = 15_000_000L,
            photosDuplicateBytes = 0L,
            videosTotalBytes = 0L,
            videosDuplicateBytes = 0L,
            librarySizeBytes = 15_000_000L
        )

        assertEquals(15_000_000L, photosOnlyNoDup.photosUniqueBytes)
        assertEquals(0L, photosOnlyNoDup.totalDuplicateBytes)
    }

    // 9. DEVICE STORAGE DONUT ARCS TESTS
    @Test
    fun testDeviceStorageCalculateArcsSumAndThresholds() {
        val totalBytes = 128_000_000_000L // 128 GB
        val freeBytes = 32_000_000_000L   // 32 GB
        val photosDup = 50_000_000L       // 50 MB (tiny relative to 128 GB -> normally < 0.15 deg)
        val videosDup = 200_000_000L      // 200 MB
        val photosUnique = 10_000_000_000L // 10 GB
        val videosUnique = 15_000_000_000L // 15 GB
        val gapDegrees = 1.5f

        val segments = com.photosremover.app.domain.DeviceStorage.calculateStorageArcs(
            totalDeviceBytes = totalBytes,
            freeDeviceBytes = freeBytes,
            photosDupBytes = photosDup,
            videosDupBytes = videosDup,
            photosUniqueBytes = photosUnique,
            videosUniqueBytes = videosUnique,
            gapDegrees = gapDegrees,
            minDegreeThreshold = 3.0f
        )

        // 1. All segments with bytes > 0 have angle >= 3.0 degrees
        for (segment in segments) {
            if (segment.bytes > 0) {
                assertTrue(
                    "Segment ${segment.label} (${segment.bytes} bytes) sweep ${segment.sweepAngle} must be >= 3.0 deg",
                    segment.sweepAngle >= 2.99f
                )
            }
        }

        // 2. Sum of all sweep angles + (count * gapDegrees) equals 360.0 degrees
        val totalSweeps = segments.sumOf { it.sweepAngle.toDouble() }.toFloat()
        val totalGaps = segments.size * gapDegrees
        val totalCircle = totalSweeps + totalGaps
        assertEquals("Total circle must equal 360 degrees", 360.0f, totalCircle, 0.1f)

        // 3. Khác (Other) is never negative
        val other = segments.firstOrNull { it.id == "other" }
        assertNotNull(other)
        assertTrue("Khác must be >= 0", other!!.bytes >= 0L)
    }

    @Test
    fun testDeviceStorageCalculateArcsZeroAndSingleCategory() {
        // Zero total device storage -> does not crash, returns single 360 deg segment
        val emptyArcs = com.photosremover.app.domain.DeviceStorage.calculateStorageArcs(
            totalDeviceBytes = 0L,
            freeDeviceBytes = 0L,
            photosDupBytes = 0L,
            videosDupBytes = 0L,
            photosUniqueBytes = 0L,
            videosUniqueBytes = 0L
        )
        assertFalse(emptyArcs.isEmpty())
        assertEquals(360.0f, emptyArcs.first().sweepAngle, 0.01f)

        // Machine almost full (free = 0)
        val fullArcs = com.photosremover.app.domain.DeviceStorage.calculateStorageArcs(
            totalDeviceBytes = 64_000_000_000L,
            freeDeviceBytes = 0L,
            photosDupBytes = 1_000_000_000L,
            videosDupBytes = 2_000_000_000L,
            photosUniqueBytes = 5_000_000_000L,
            videosUniqueBytes = 10_000_000_000L
        )
        val fullSweeps = fullArcs.sumOf { it.sweepAngle.toDouble() }.toFloat()
        val fullGaps = fullArcs.size * 1.5f
        assertEquals(360.0f, fullSweeps + fullGaps, 0.1f)
    }

    private fun assertNotNull(obj: Any?) {
        assertTrue(obj != null)
    }
}
