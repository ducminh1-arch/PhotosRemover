package com.photosremover.app.domain

import android.graphics.Bitmap
import android.graphics.Color
import com.photosremover.app.data.model.DuplicateSet
import com.photosremover.app.data.model.DuplicateSetType
import com.photosremover.app.data.model.ScanPhase
import com.photosremover.app.data.model.ScanProgress
import com.photosremover.app.data.model.SimilarSensitivity
import com.photosremover.app.data.model.VideoItem
import com.photosremover.app.data.repository.VideoRepository
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

class VideoEngine(private val repository: VideoRepository) {

    data class VideoScanResult(
        val exactSets: List<DuplicateSet>,
        val similarSets: List<DuplicateSet>,
        val totalLibraryVideoBytes: Long = 0L
    )

    sealed class ScanProgressUpdate {
        data class Progress(val progress: ScanProgress) : ScanProgressUpdate()
        data class Finished(val result: VideoScanResult) : ScanProgressUpdate()
    }

    fun scanVideos(
        videos: List<VideoItem>,
        sensitivity: SimilarSensitivity
    ): Flow<ScanProgressUpdate> = flow {
        val total = videos.size
        emit(ScanProgressUpdate.Progress(ScanProgress(ScanPhase.VIDEO_CHECKING, 0, total)))

        // 1. EXACT DUPLICATE DETECTION via SHA-256
        val exactResult = findExactDuplicates(videos) { current, totalToHash, found ->
            emit(
                ScanProgressUpdate.Progress(
                    ScanProgress(
                        phase = ScanPhase.VIDEO_CHECKING,
                        current = current,
                        total = totalToHash,
                        videoSetsFound = found
                    )
                )
            )
        }

        val exactSets = exactResult.exactSets
        val exactVideoIds = exactResult.exactVideoIds
        val exactRepresentativeIds = exactResult.representativeVideoIds

        // 2. SIMILAR DETECTION: candidates = non-exact + 1 representative per exact group
        val similarCandidates = videos.filter { v ->
            !exactVideoIds.contains(v.id) || exactRepresentativeIds.contains(v.id)
        }

        // 3. SIMILAR via 3-frame dHash
        val similarSets = findSimilarDuplicates(
            candidates = similarCandidates,
            sensitivity = sensitivity
        ) { current, totalToCompare, found ->
            emit(
                ScanProgressUpdate.Progress(
                    ScanProgress(
                        phase = ScanPhase.VIDEO_CHECKING,
                        current = current,
                        total = totalToCompare,
                        videoSetsFound = exactSets.size + found
                    )
                )
            )
        }

        emit(
            ScanProgressUpdate.Finished(
                VideoScanResult(
                    exactSets = exactSets,
                    similarSets = similarSets,
                    totalLibraryVideoBytes = videos.sumOf { it.sizeBytes }
                )
            )
        )
    }.flowOn(Dispatchers.Default)

    // ── Exact detection ──────────────────────────────────────────────────────

    private data class ExactIntermediateResult(
        val exactSets: List<DuplicateSet>,
        val exactVideoIds: Set<Long>,
        val representativeVideoIds: Set<Long>
    )

    private suspend fun findExactDuplicates(
        videos: List<VideoItem>,
        onProgress: suspend (current: Int, total: Int, found: Int) -> Unit
    ): ExactIntermediateResult {
        // Group by file size first (cheap pre-filter)
        val sizeBuckets = videos.groupBy { it.sizeBytes }.filter { it.value.size >= 2 }
        val candidates = sizeBuckets.values.flatten()
        val totalCandidates = candidates.size

        if (totalCandidates == 0) {
            return ExactIntermediateResult(emptyList(), emptySet(), emptySet())
        }

        var processed = 0
        var foundCount = 0
        val hashGroups = mutableMapOf<String, MutableList<VideoItem>>()

        for (video in candidates) {
            coroutineContext.ensureActive()
            val hash = computeSha256(video)
            if (hash != null) {
                hashGroups.getOrPut(hash) { mutableListOf() }.add(video.copy(sha256 = hash))
            }
            processed++
            if (processed % 3 == 0 || processed == totalCandidates) {
                onProgress(processed, totalCandidates, foundCount)
            }
        }

        val exactSets = mutableListOf<DuplicateSet>()
        val exactVideoIds = mutableSetOf<Long>()
        val representativeVideoIds = mutableSetOf<Long>()
        var setIndex = 1

        for ((_, group) in hashGroups) {
            if (group.size >= 2) {
                val best = pickBestVideo(group)
                val processed = group
                    .sortedByDescending { it.sizeBytes }
                    .map { v ->
                        v.copy(isBest = v.id == best.id, isSelected = v.id != best.id && !v.isFavorite)
                    }
                    .sortedByDescending { it.isBest }

                exactSets.add(
                    DuplicateSet(
                        id = "exact_video_$setIndex",
                        title = "Set: $setIndex",
                        type = DuplicateSetType.EXACT_VIDEO,
                        videos = processed
                    )
                )
                setIndex++
                foundCount++
                for (v in group) exactVideoIds.add(v.id)
                representativeVideoIds.add(best.id)
            }
        }

        val sortedExactSets = exactSets
            .sortedByDescending { it.totalSize }
            .mapIndexed { idx, set ->
                set.copy(
                    id = "exact_video_${idx + 1}",
                    title = "Set: ${idx + 1}"
                )
            }

        onProgress(totalCandidates, totalCandidates, foundCount)
        return ExactIntermediateResult(sortedExactSets, exactVideoIds, representativeVideoIds)
    }

    // ── Similar detection ─────────────────────────────────────────────────────

    private suspend fun findSimilarDuplicates(
        candidates: List<VideoItem>,
        sensitivity: SimilarSensitivity,
        onProgress: suspend (current: Int, total: Int, found: Int) -> Unit
    ): List<DuplicateSet> {
        val total = candidates.size
        if (total < 2) return emptyList()

        // 1. Compute 3-frame fingerprint for each video
        val videosWithHashes = mutableListOf<VideoItem>()
        var current = 0
        for (video in candidates) {
            coroutineContext.ensureActive()
            val hashes = computeFrameHashes(video)
            if (hashes != null) {
                videosWithHashes.add(video.copy(frameHashes = hashes))
            }
            current++
            if (current % 5 == 0 || current == total) {
                onProgress(current, total, 0)
            }
        }

        // 2. Bucket by aspect ratio for efficiency
        val aspectBuckets = videosWithHashes.groupBy { v ->
            val ratio = v.aspectRatio
            if (ratio.isNaN() || ratio.isInfinite() || ratio <= 0f) 1.0f
            else (ratio * 10).roundToInt() / 10f
        }

        // 3. Union-Find similarity grouping
        val parent = mutableMapOf<Long, Long>()

        fun find(id: Long): Long {
            var root = id
            while (parent.containsKey(root)) root = parent[root]!!
            var curr = id
            while (curr != root) {
                val next = parent[curr] ?: break
                parent[curr] = root
                curr = next
            }
            return root
        }

        fun union(id1: Long, id2: Long) {
            val r1 = find(id1); val r2 = find(id2)
            if (r1 != r2) parent[r1] = r2
        }

        for ((_, bucket) in aspectBuckets) {
            val size = bucket.size
            if (size < 2) continue
            for (i in 0 until size) {
                val va = bucket[i]
                val ha = va.frameHashes ?: continue
                for (j in i + 1 until size) {
                    val vb = bucket[j]
                    val hb = vb.frameHashes ?: continue
                    // Also filter: duration difference > 20% → not similar
                    val maxDuration = maxOf(va.durationMs, vb.durationMs)
                    val durationDiff = kotlin.math.abs(va.durationMs - vb.durationMs)
                    if (maxDuration > 0 && durationDiff.toFloat() / maxDuration > 0.20f) continue

                    val dist = averageFrameDistance(ha, hb)
                    if (dist <= sensitivity.maxHammingDistance) {
                        union(va.id, vb.id)
                    }
                }
            }
        }

        // 4. Collect groups
        val groupsMap = mutableMapOf<Long, MutableList<VideoItem>>()
        for (v in videosWithHashes) {
            groupsMap.getOrPut(find(v.id)) { mutableListOf() }.add(v)
        }

        val similarSets = mutableListOf<DuplicateSet>()
        var setIndex = 1
        for ((_, group) in groupsMap) {
            if (group.size >= 2) {
                val best = pickBestVideo(group)
                val processed = group
                    .sortedByDescending { it.sizeBytes }
                    .map { v ->
                        v.copy(isBest = v.id == best.id, isSelected = false)
                    }
                    .sortedByDescending { it.isBest }
                similarSets.add(
                    DuplicateSet(
                        id = "similar_video_$setIndex",
                        title = "Set: $setIndex",
                        type = DuplicateSetType.SIMILAR_VIDEO,
                        videos = processed
                    )
                )
                setIndex++
            }
        }

        val sortedSimilarSets = similarSets
            .sortedByDescending { it.totalSize }
            .mapIndexed { idx, set ->
                set.copy(
                    id = "similar_video_${idx + 1}",
                    title = "Set: ${idx + 1}"
                )
            }

        return sortedSimilarSets
    }

    // ── Fingerprinting ────────────────────────────────────────────────────────

    /**
     * Extracts 3 frames at 10%, 50%, 90% of the video duration and
     * computes a 64-bit dHash per frame. Returns null if any frame fails.
     */
    private suspend fun computeFrameHashes(video: VideoItem): LongArray? {
        val uri = video.uri ?: return null
        val duration = video.durationMs
        if (duration <= 0) return null

        val timestamps = longArrayOf(
            (duration * 0.10).toLong(),
            (duration * 0.50).toLong(),
            (duration * 0.90).toLong()
        )

        val hashes = LongArray(3)
        for (i in timestamps.indices) {
            val frame = repository.loadVideoFrame(uri, timestamps[i]) ?: return null
            hashes[i] = computeDHash(frame)
            if (!frame.isRecycled) frame.recycle()
        }
        return hashes
    }

    /**
     * Average Hamming distance across 3 frame pairs.
     */
    private fun averageFrameDistance(a: LongArray, b: LongArray): Int {
        if (a.size != b.size) return Int.MAX_VALUE
        var total = 0
        for (i in a.indices) {
            total += java.lang.Long.bitCount(a[i] xor b[i])
        }
        return total / a.size
    }

    private fun computeDHash(bitmap: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
        return try {
            var hash = 0L
            for (y in 0 until 8) {
                for (x in 0 until 8) {
                    val gLeft = toGrayscale(scaled.getPixel(x, y))
                    val gRight = toGrayscale(scaled.getPixel(x + 1, y))
                    if (gLeft > gRight) hash = hash or (1L shl (y * 8 + x))
                }
            }
            hash
        } finally {
            if (scaled != bitmap && !scaled.isRecycled) scaled.recycle()
        }
    }

    private fun toGrayscale(color: Int): Int {
        return (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)).toInt()
    }

    private fun computeSha256(video: VideoItem): String? {
        val uri = video.uri ?: return null
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
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Throwable) {
            null
        }
    }

    fun pickBestVideo(videos: List<VideoItem>): VideoItem {
        return videos.maxWithOrNull(
            compareBy<VideoItem> { it.isFavorite }
                .thenBy { it.width * it.height }
                .thenBy { it.durationMs }
                .thenBy { it.sizeBytes }
        ) ?: videos.first()
    }
}
