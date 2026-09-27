package com.photosremover.app.domain

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import androidx.compose.ui.graphics.Color
import com.photosremover.app.ui.theme.DangerRed
import java.util.Locale
import kotlin.math.max

data class DeviceStorageInfo(
    val totalBytes: Long,
    val freeBytes: Long
) {
    val usedBytes: Long
        get() = (totalBytes - freeBytes).coerceAtLeast(0L)
}

data class StorageSegment(
    val id: String,
    val label: String,
    val bytes: Long,
    val color: Color,
    val startAngle: Float = 0f,
    val sweepAngle: Float = 0f
)

object DeviceStorage {

    val ColorPhotoDuplicate = DangerRed                    // Red (#EF4444)
    val ColorVideoDuplicate = Color(0xFFF59E0B)           // Orange / Amber
    val ColorPhotoUnique = Color(0xFF3B82F6)              // Blue
    val ColorVideoUnique = Color(0xFF8B5CF6)              // Purple
    val ColorOther = Color(0xFF94A3B8)                    // Gray (apps, OS, data)
    val ColorFree = Color(0xFFE2E8F0)                     // Light Gray (free space)

    fun queryDeviceStorage(context: Context): DeviceStorageInfo {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
                if (storageStatsManager != null) {
                    val total = storageStatsManager.getTotalBytes(StorageManager.UUID_DEFAULT)
                    val free = storageStatsManager.getFreeBytes(StorageManager.UUID_DEFAULT)
                    DeviceStorageInfo(totalBytes = total, freeBytes = free)
                } else {
                    queryFromStatFs()
                }
            } else {
                queryFromStatFs()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            queryFromStatFs()
        }
    }

    private fun queryFromStatFs(): DeviceStorageInfo {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val total = stat.totalBytes
            val free = stat.availableBytes
            DeviceStorageInfo(totalBytes = total, freeBytes = free)
        } catch (e: Exception) {
            e.printStackTrace()
            DeviceStorageInfo(totalBytes = 0L, freeBytes = 0L)
        }
    }

    fun querySystemPartitionBytes(): Long {
        return try {
            val root = Environment.getRootDirectory()
            val stat = StatFs(root.path)
            stat.totalBytes
        } catch (e: Exception) {
            0L
        }
    }

    fun queryAudioBytes(context: Context): Long {
        return try {
            val projection = arrayOf(android.provider.MediaStore.Audio.Media.SIZE)
            context.contentResolver.query(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                var total = 0L
                val sizeCol = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.SIZE)
                if (sizeCol != -1) {
                    while (cursor.moveToNext()) {
                        total += cursor.getLong(sizeCol)
                    }
                }
                total
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun queryDownloadsBytes(): Long {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir.exists() && downloadsDir.isDirectory) {
                var size = 0L
                downloadsDir.listFiles()?.forEach { file ->
                    if (file.isFile) size += file.length()
                }
                size
            } else 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun openDeviceStorageSettings(context: Context) {
        val intents = listOf(
            android.content.Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
            android.content.Intent().setClassName("com.samsung.android.sm", "com.samsung.android.sm.storage.ui.StorageActivity"),
            android.content.Intent("com.samsung.android.sm.ACTION_BATTERY"),
            android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS),
            android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
        )
        for (intent in intents) {
            try {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // Try next intent
            }
        }
    }

    /**
     * Pure function to calculate storage arcs for Donut Chart:
     * - Order: clockwise starting at 12 o'clock (-90°):
     *   1. Duplicate Photos (Red)
     *   2. Duplicate Videos (Orange)
     *   3. Unique Photos (Blue)
     *   4. Unique Videos (Purple)
     *   5. Other - apps, OS, data (Gray)
     *   6. Free (Light Gray)
     * - Small gap between segments (gapDegrees ~ 1.5°).
     * - Minimum 3.0° for any segment with bytes > 0.
     * - "Khác" and "Trống" shrink proportionally to satisfy minimums.
     * - Total angles + gaps = 360.0° exactly.
     */
    fun calculateStorageArcs(
        totalDeviceBytes: Long,
        freeDeviceBytes: Long,
        photosDupBytes: Long,
        videosDupBytes: Long,
        photosUniqueBytes: Long,
        videosUniqueBytes: Long,
        gapDegrees: Float = 1.5f,
        minDegreeThreshold: Float = 3.0f
    ): List<StorageSegment> {
        val safePhotosDup = photosDupBytes.coerceAtLeast(0L)
        val safeVideosDup = videosDupBytes.coerceAtLeast(0L)
        val safePhotosUnique = photosUniqueBytes.coerceAtLeast(0L)
        val safeVideosUnique = videosUniqueBytes.coerceAtLeast(0L)

        val totalMediaBytes = safePhotosDup + safeVideosDup + safePhotosUnique + safeVideosUnique

        val effectiveTotal = if (totalDeviceBytes > 0L) totalDeviceBytes else totalMediaBytes
        if (effectiveTotal <= 0L) {
            return listOf(
                StorageSegment(
                    id = "empty",
                    label = "Không có dữ liệu",
                    bytes = 0L,
                    color = ColorFree,
                    startAngle = -90f,
                    sweepAngle = 360f
                )
            )
        }

        val safeFree = freeDeviceBytes.coerceIn(0L, effectiveTotal)
        val usedBytes = (effectiveTotal - safeFree).coerceAtLeast(0L)
        val otherBytes = (usedBytes - totalMediaBytes).coerceAtLeast(0L)
        val freeBytesAdjusted = (effectiveTotal - (totalMediaBytes + otherBytes)).coerceAtLeast(0L)

        val rawItems = listOf(
            StorageSegment("photo_dup", "Ảnh trùng lặp", safePhotosDup, ColorPhotoDuplicate),
            StorageSegment("video_dup", "Video trùng lặp", safeVideosDup, ColorVideoDuplicate),
            StorageSegment("photo_unique", "Ảnh duy nhất", safePhotosUnique, ColorPhotoUnique),
            StorageSegment("video_unique", "Video duy nhất", safeVideosUnique, ColorVideoUnique),
            StorageSegment("other", "Khác", otherBytes, ColorOther),
            StorageSegment("free", "Còn trống", freeBytesAdjusted, ColorFree)
        )

        val activeItems = rawItems.filter { it.bytes > 0L }
        if (activeItems.isEmpty()) {
            return listOf(
                StorageSegment(
                    id = "empty",
                    label = "Còn trống",
                    bytes = effectiveTotal,
                    color = ColorFree,
                    startAngle = -90f,
                    sweepAngle = 360f
                )
            )
        }

        if (activeItems.size == 1) {
            val single = activeItems.first()
            return listOf(single.copy(startAngle = -90f, sweepAngle = 360f))
        }

        val count = activeItems.size
        val totalGapAngle = count * gapDegrees
        val availableArcDegrees = 360f - totalGapAngle

        val sumActiveBytes = activeItems.sumOf { it.bytes }
        val divisor = if (sumActiveBytes > 0L) sumActiveBytes.toDouble() else effectiveTotal.toDouble()

        // Step 1: Initial raw angles proportional to bytes
        val angles = activeItems.map { item ->
            (item.bytes.toDouble() / divisor * availableArcDegrees).toFloat()
        }.toMutableList()

        // Step 2: Minimum 3.0° rule
        var totalDeficit = 0f
        val needsBoost = BooleanArray(count)
        for (i in 0 until count) {
            if (angles[i] < minDegreeThreshold) {
                totalDeficit += (minDegreeThreshold - angles[i])
                angles[i] = minDegreeThreshold
                needsBoost[i] = true
            }
        }

        // Step 3: Shrink "Khác" and "Trống" proportionally to pay for deficit
        if (totalDeficit > 0f) {
            val khacIndex = activeItems.indexOfFirst { it.id == "other" }
            val trongIndex = activeItems.indexOfFirst { it.id == "free" }

            val shrinkableKhac = if (khacIndex >= 0 && !needsBoost[khacIndex]) {
                max(0f, angles[khacIndex] - minDegreeThreshold)
            } else 0f

            val shrinkableTrong = if (trongIndex >= 0 && !needsBoost[trongIndex]) {
                max(0f, angles[trongIndex] - minDegreeThreshold)
            } else 0f

            val totalShrinkable = shrinkableKhac + shrinkableTrong
            if (totalShrinkable > 0f) {
                val reductionFromShrinkPool = minOf(totalDeficit, totalShrinkable)
                if (shrinkableKhac > 0f) {
                    val dropKhac = reductionFromShrinkPool * (shrinkableKhac / totalShrinkable)
                    angles[khacIndex] = max(minDegreeThreshold, angles[khacIndex] - dropKhac)
                }
                if (shrinkableTrong > 0f) {
                    val dropTrong = reductionFromShrinkPool * (shrinkableTrong / totalShrinkable)
                    angles[trongIndex] = max(minDegreeThreshold, angles[trongIndex] - dropTrong)
                }
                totalDeficit -= reductionFromShrinkPool
            }

            // Fallback: if deficit still remains, shrink any other segment above threshold
            if (totalDeficit > 0f) {
                val otherPool = (0 until count)
                    .filter { !needsBoost[it] && it != khacIndex && it != trongIndex }
                    .sumOf { max(0.0, (angles[it] - minDegreeThreshold).toDouble()) }
                    .toFloat()

                if (otherPool > 0f) {
                    val reduction = minOf(totalDeficit, otherPool)
                    for (i in 0 until count) {
                        if (!needsBoost[i] && i != khacIndex && i != trongIndex) {
                            val available = max(0f, angles[i] - minDegreeThreshold)
                            if (available > 0f) {
                                angles[i] -= reduction * (available / otherPool)
                            }
                        }
                    }
                }
            }
        }

        // Step 4: Normalize to ensure exact 360° total (including gaps)
        val currentSum = angles.sum()
        if (currentSum > 0f && currentSum != availableArcDegrees) {
            val scale = availableArcDegrees / currentSum
            for (i in 0 until count) {
                angles[i] *= scale
            }
        }

        // Step 5: Assign start and sweep angles clockwise starting at -90° (12 o'clock)
        var currentAngle = -90f
        return activeItems.mapIndexed { index, item ->
            val sweep = angles[index]
            val segment = item.copy(
                startAngle = currentAngle,
                sweepAngle = sweep
            )
            currentAngle += (sweep + gapDegrees)
            segment
        }
    }

    /**
     * Formats bytes according to current device locale (e.g. "47,22 GB" in vi, "47.22 GB" in en).
     */
    fun formatBytesLocale(bytes: Long, locale: Locale = Locale.getDefault()): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(locale, "%.2f GB", gb)
            mb >= 1.0 -> String.format(locale, "%.1f MB", mb)
            kb >= 1.0 -> String.format(locale, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }
}
