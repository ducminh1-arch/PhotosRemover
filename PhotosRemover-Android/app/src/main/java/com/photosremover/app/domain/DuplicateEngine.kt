package com.photosremover.app.domain

import android.graphics.Bitmap
import android.graphics.Color
import com.photosremover.app.data.model.DuplicateSet
import com.photosremover.app.data.model.DuplicateSetType
import com.photosremover.app.data.model.PhotoItem
import com.photosremover.app.data.model.ScanPhase
import com.photosremover.app.data.model.ScanProgress
import com.photosremover.app.data.model.SimilarSensitivity
import com.photosremover.app.data.repository.PhotoRepository
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.InputStream
import java.security.MessageDigest
import kotlin.math.roundToInt

class DuplicateEngine(private val repository: PhotoRepository) {

    data class EngineScanResult(
        val exactSets: List<DuplicateSet>,
        val similarSets: List<DuplicateSet>,
        val totalLibraryPhotoBytes: Long = 0L
    )

    fun scanLibrary(
        photos: List<PhotoItem>,
        sensitivity: SimilarSensitivity
    ): Flow<ScanProgressUpdate> = flow {
        val total = photos.size
        emit(ScanProgressUpdate.Progress(ScanProgress(ScanPhase.INDEXING, 0, total)))

        // 1. EXACT DUPLICATE DETECTION
        val exactResult = findExactDuplicates(photos) { current, totalToHash, exactFound ->
            emit(
                ScanProgressUpdate.Progress(
                    ScanProgress(
                        phase = ScanPhase.EXACT_CHECKING,
                        current = current,
                        total = totalToHash,
                        exactSetsFound = exactFound
                    )
                )
            )
        }

        val exactSets = exactResult.exactSets
        val exactPhotoIds = exactResult.exactPhotoIds
        val exactRepresentativeIds = exactResult.representativePhotoIds

        // 2. PREPARE CANDIDATES FOR SIMILAR DETECTION
        // RULE: 1 representative from each Exact Set + all photos not in any Exact Set
        // NEVER include other exact duplicates so no photo appears in both tabs!
        val similarCandidates = photos.filter { photo ->
            !exactPhotoIds.contains(photo.id) || exactRepresentativeIds.contains(photo.id)
        }

        // 3. SIMILAR PHOTO DETECTION
        val similarSets = findSimilarDuplicates(
            candidates = similarCandidates,
            sensitivity = sensitivity
        ) { current, totalToCompare, similarFound ->
            emit(
                ScanProgressUpdate.Progress(
                    ScanProgress(
                        phase = ScanPhase.SIMILAR_CHECKING,
                        current = current,
                        total = totalToCompare,
                        exactSetsFound = exactSets.size,
                        similarSetsFound = similarFound
                    )
                )
            )
        }

        emit(
            ScanProgressUpdate.Finished(
                EngineScanResult(
                    exactSets = exactSets,
                    similarSets = similarSets,
                    totalLibraryPhotoBytes = photos.sumOf { it.sizeBytes }
                )
            )
        )
    }.flowOn(Dispatchers.Default)

    sealed class ScanProgressUpdate {
        data class Progress(val progress: ScanProgress) : ScanProgressUpdate()
        data class Finished(val result: EngineScanResult) : ScanProgressUpdate()
    }

    private data class ExactIntermediateResult(
        val exactSets: List<DuplicateSet>,
        val exactPhotoIds: Set<Long>,
        val representativePhotoIds: Set<Long>
    )

    private suspend fun findExactDuplicates(
        photos: List<PhotoItem>,
        onProgress: suspend (current: Int, total: Int, found: Int) -> Unit
    ): ExactIntermediateResult {
        // Step 1: Group by file size
        val sizeBuckets = photos.groupBy { it.sizeBytes }
            .filter { it.value.size >= 2 }

        val candidatePhotos = sizeBuckets.values.flatten()
        val totalCandidates = candidatePhotos.size

        if (totalCandidates == 0) {
            return ExactIntermediateResult(emptyList(), emptySet(), emptySet())
        }

        var processed = 0
        var foundCount = 0
        val hashGroups = mutableMapOf<String, MutableList<PhotoItem>>()

        for (photo in candidatePhotos) {
            coroutineContext.ensureActive()
            val hash = computeSha256(photo)
            if (hash != null) {
                val photoWithHash = photo.copy(sha256 = hash)
                hashGroups.getOrPut(hash) { mutableListOf() }.add(photoWithHash)
            }
            processed++
            if (processed % 5 == 0 || processed == totalCandidates) {
                onProgress(processed, totalCandidates, foundCount)
            }
        }

        // Build exact sets
        val exactSets = mutableListOf<DuplicateSet>()
        val exactPhotoIds = mutableSetOf<Long>()
        val representativePhotoIds = mutableSetOf<Long>()
        var setIndex = 1

        for ((_, group) in hashGroups) {
            if (group.size >= 2) {
                // Find Best Photo
                val bestPhoto = pickBestPhoto(group)
                val processedPhotos = group
                    .sortedByDescending { it.sizeBytes }
                    .map { item ->
                        val isBest = item.id == bestPhoto.id
                        // Auto-select rule for Exact: Keep Best, pre-select duplicates UNLESS isFavorite
                        item.copy(
                            isBest = isBest,
                            isSelected = !isBest && !item.isFavorite
                        )
                    }
                    .sortedByDescending { it.isBest }

                exactSets.add(
                    DuplicateSet(
                        id = "exact_$setIndex",
                        title = "Set: $setIndex",
                        type = DuplicateSetType.EXACT,
                        photos = processedPhotos
                    )
                )
                setIndex++
                foundCount++

                for (p in group) {
                    exactPhotoIds.add(p.id)
                }
                representativePhotoIds.add(bestPhoto.id)
            }
        }

        // Sort exact sets descending by totalSize so largest duplicate sets appear first
        val sortedExactSets = exactSets
            .sortedByDescending { it.totalSize }
            .mapIndexed { idx, set ->
                set.copy(
                    id = "exact_${idx + 1}",
                    title = "Set: ${idx + 1}"
                )
            }

        onProgress(totalCandidates, totalCandidates, foundCount)

        return ExactIntermediateResult(
            exactSets = sortedExactSets,
            exactPhotoIds = exactPhotoIds,
            representativePhotoIds = representativePhotoIds
        )
    }

    private suspend fun findSimilarDuplicates(
        candidates: List<PhotoItem>,
        sensitivity: SimilarSensitivity,
        onProgress: suspend (current: Int, total: Int, found: Int) -> Unit
    ): List<DuplicateSet> {
        val total = candidates.size
        if (total < 2) return emptyList()

        // 1. Compute dHash for all candidates
        val photosWithHash = mutableListOf<PhotoItem>()
        var current = 0
        for (photo in candidates) {
            coroutineContext.ensureActive()
            val hash = computeDHash(photo)
            if (hash != null) {
                photosWithHash.add(photo.copy(dHash = hash))
            }
            current++
            if (current % 10 == 0 || current == total) {
                onProgress(current, total, 0)
            }
        }

        // 2. Bucket photos by aspect ratio (rounded to 1 decimal place) to optimize comparisons
        val aspectBuckets = photosWithHash.groupBy { photo ->
            val ratio = photo.aspectRatio
            if (ratio.isNaN() || ratio.isInfinite() || ratio <= 0f) {
                1.0f
            } else {
                (ratio * 10).roundToInt() / 10f
            }
        }

        // 3. Union-Find to group similar photos
        val parent = mutableMapOf<Long, Long>()

        fun find(id: Long): Long {
            var root = id
            while (parent.containsKey(root)) {
                root = parent[root]!!
            }
            // Path compression
            var curr = id
            while (curr != root) {
                val next = parent[curr] ?: break
                parent[curr] = root
                curr = next
            }
            return root
        }

        fun union(id1: Long, id2: Long) {
            val root1 = find(id1)
            val root2 = find(id2)
            if (root1 != root2) {
                parent[root1] = root2
            }
        }

        // Compare pairs within each aspect ratio bucket
        for ((_, bucketPhotos) in aspectBuckets) {
            val size = bucketPhotos.size
            if (size < 2) continue
            for (i in 0 until size) {
                val photoA = bucketPhotos[i]
                val hashA = photoA.dHash ?: continue
                for (j in i + 1 until size) {
                    val photoB = bucketPhotos[j]
                    val hashB = photoB.dHash ?: continue
                    val distance = hammingDistance(hashA, hashB)
                    if (distance <= sensitivity.maxHammingDistance) {
                        union(photoA.id, photoB.id)
                    }
                }
            }
        }

        // 4. Collect grouped sets
        val groupedSetsMap = mutableMapOf<Long, MutableList<PhotoItem>>()
        for (photo in photosWithHash) {
            val root = find(photo.id)
            groupedSetsMap.getOrPut(root) { mutableListOf() }.add(photo)
        }

        val similarSets = mutableListOf<DuplicateSet>()
        var setIndex = 1

        for ((_, group) in groupedSetsMap) {
            if (group.size >= 2) {
                val bestPhoto = pickBestPhoto(group)
                // RULE for Similar Tab: DO NOT PRE-SELECT! Mark Best only
                val processedPhotos = group
                    .sortedByDescending { it.sizeBytes }
                    .map { item ->
                        item.copy(
                            isBest = item.id == bestPhoto.id,
                            isSelected = false
                        )
                    }
                    .sortedByDescending { it.isBest }

                similarSets.add(
                    DuplicateSet(
                        id = "similar_$setIndex",
                        title = "Set: $setIndex",
                        type = DuplicateSetType.SIMILAR,
                        photos = processedPhotos
                    )
                )
                setIndex++
            }
        }

        val sortedSimilarSets = similarSets
            .sortedByDescending { it.totalSize }
            .mapIndexed { idx, set ->
                set.copy(
                    id = "similar_${idx + 1}",
                    title = "Set: ${idx + 1}"
                )
            }

        return sortedSimilarSets
    }

    /**
     * Priority for Best Photo:
     * 1. Favorite flag
     * 2. Highest resolution (width * height)
     * 3. Largest file size
     * 4. Earliest date taken
     */
    fun pickBestPhoto(photos: List<PhotoItem>): PhotoItem {
        return photos.maxWithOrNull(
            compareBy<PhotoItem> { it.isFavorite }
                .thenBy { it.width * it.height }
                .thenBy { it.sizeBytes }
                .thenByDescending { -it.dateTakenMs }
        ) ?: photos.first()
    }

    /**
     * Compute SHA-256 hash using 64 KB streaming buffer
     */
    fun computeSha256(photo: PhotoItem): String? {
        val uri = photo.uri ?: return null
        val stream: InputStream = repository.openInputStream(uri) ?: return null
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            stream.use { input ->
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val hashBytes = digest.digest()
            val sb = StringBuilder()
            for (b in hashBytes) {
                sb.append(String.format("%02x", b))
            }
            sb.toString()
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Compute 64-bit dHash (Difference Hash)
     * 1. Scale down to 9x8 pixels
     * 2. Convert to grayscale
     * 3. Compare adjacent horizontal pixels (pixel[x] > pixel[x+1])
     */
    suspend fun computeDHash(photo: PhotoItem): Long? {
        val uri = photo.uri ?: return null
        val thumbnail = repository.loadThumbnail(uri, 64, 64) ?: return null
        return try {
            val scaled = Bitmap.createScaledBitmap(thumbnail, 9, 8, true)
            try {
                var hash = 0L

                for (y in 0 until 8) {
                    for (x in 0 until 8) {
                        val pLeft = scaled.getPixel(x, y)
                        val pRight = scaled.getPixel(x + 1, y)

                        val grayLeft = toGrayscale(pLeft)
                        val grayRight = toGrayscale(pRight)

                        if (grayLeft > grayRight) {
                            hash = hash or (1L shl (y * 8 + x))
                        }
                    }
                }
                hash
            } finally {
                if (scaled != thumbnail && !scaled.isRecycled) {
                    scaled.recycle()
                }
            }
        } catch (_: Throwable) {
            null
        } finally {
            if (!thumbnail.isRecycled) {
                thumbnail.recycle()
            }
        }
    }

    /**
     * Compute dHash from an existing Bitmap (useful for tests and in-memory processing)
     */
    fun computeDHashFromBitmap(bitmap: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
        return try {
            var hash = 0L

            for (y in 0 until 8) {
                for (x in 0 until 8) {
                    val pLeft = scaled.getPixel(x, y)
                    val pRight = scaled.getPixel(x + 1, y)

                    val grayLeft = toGrayscale(pLeft)
                    val grayRight = toGrayscale(pRight)

                    if (grayLeft > grayRight) {
                        hash = hash or (1L shl (y * 8 + x))
                    }
                }
            }
            hash
        } finally {
            if (scaled != bitmap) {
                scaled.recycle()
            }
        }
    }

    private fun toGrayscale(color: Int): Int {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }

    fun hammingDistance(hash1: Long, hash2: Long): Int {
        return java.lang.Long.bitCount(hash1 xor hash2)
    }

    companion object {
        fun benchmarkHammingComparisons(hashes: LongArray, maxDistance: Int): Long {
            val start = System.currentTimeMillis()
            var matches = 0
            val size = hashes.size
            for (i in 0 until size) {
                val hA = hashes[i]
                for (j in i + 1 until size) {
                    val dist = java.lang.Long.bitCount(hA xor hashes[j])
                    if (dist <= maxDistance) {
                        matches++
                    }
                }
            }
            return System.currentTimeMillis() - start
        }
    }
}
