import Foundation
import SwiftUI

public struct DeviceStorageInfo: Equatable {
    public let totalBytes: Int64
    public let freeBytes: Int64

    public init(totalBytes: Int64, freeBytes: Int64) {
        self.totalBytes = totalBytes
        self.freeBytes = freeBytes
    }

    public var usedBytes: Int64 {
        return max(0, totalBytes - freeBytes)
    }
}

public struct StorageSegment: Identifiable, Equatable {
    public let id: String
    public let label: String
    public let bytes: Int64
    public let color: Color
    public var startFraction: CGFloat = 0.0
    public var endFraction: CGFloat = 0.0
    public var startAngle: Double = -90.0
    public var sweepAngle: Double = 0.0

    public init(
        id: String,
        label: String,
        bytes: Int64,
        color: Color,
        startFraction: CGFloat = 0.0,
        endFraction: CGFloat = 0.0,
        startAngle: Double = -90.0,
        sweepAngle: Double = 0.0
    ) {
        self.id = id
        self.label = label
        self.bytes = bytes
        self.color = color
        self.startFraction = startFraction
        self.endFraction = endFraction
        self.startAngle = startAngle
        self.sweepAngle = sweepAngle
    }
}

public final class DeviceStorageService {
    public static let shared = DeviceStorageService()

    public static let colorPhotoDuplicate = Color(red: 0.94, green: 0.27, blue: 0.27) // Red
    public static let colorVideoDuplicate = Color(red: 0.96, green: 0.62, blue: 0.04) // Orange
    public static let colorPhotoUnique = Color(red: 0.23, green: 0.51, blue: 0.96)    // Blue
    public static let colorVideoUnique = Color(red: 0.55, green: 0.36, blue: 0.96)    // Purple
    public static let colorOther = Color(red: 0.58, green: 0.64, blue: 0.72)          // Gray (apps, OS, data)
    public static let colorFree = Color(red: 0.88, green: 0.91, blue: 0.94)           // Light Gray (free space)

    private init() {}

    public func queryDeviceStorage() -> DeviceStorageInfo {
        let homeURL = URL(fileURLWithPath: NSHomeDirectory())
        let keys: Set<URLResourceKey> = [
            .volumeTotalCapacityKey,
            .volumeAvailableCapacityForImportantUsageKey,
            .volumeAvailableCapacityKey
        ]
        let values = try? homeURL.resourceValues(forKeys: keys)
        let total = Int64(values?.volumeTotalCapacity ?? 0)
        let free = Int64(values?.volumeAvailableCapacityForImportantUsage ?? values?.volumeAvailableCapacity ?? 0)
        return DeviceStorageInfo(totalBytes: total, freeBytes: free)
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
     * - Gaps between arcs: ~1.5 degrees per segment if > 1 segment.
     * - Minimum 3.0 degrees for any segment with bytes > 0.
     * - "Khác" and "Trống" shrink proportionally to satisfy minimums.
     * - Total angles + gaps = 360.0° exactly.
     */
    public static func calculateStorageArcs(
        totalDeviceBytes: Int64,
        freeDeviceBytes: Int64,
        photosDupBytes: Int64,
        videosDupBytes: Int64,
        photosUniqueBytes: Int64,
        videosUniqueBytes: Int64,
        gapDegrees: Double = 1.5,
        minDegreeThreshold: Double = 3.0
    ) -> [StorageSegment] {
        let safePhotosDup = max(0, photosDupBytes)
        let safeVideosDup = max(0, videosDupBytes)
        let safePhotosUnique = max(0, photosUniqueBytes)
        let safeVideosUnique = max(0, videosUniqueBytes)

        let totalMedia = safePhotosDup + safeVideosDup + safePhotosUnique + safeVideosUnique
        let effectiveTotal = (totalDeviceBytes > 0) ? totalDeviceBytes : totalMedia
        if effectiveTotal <= 0 {
            return [
                StorageSegment(
                    id: "empty",
                    label: "Không có dữ liệu",
                    bytes: 0,
                    color: colorFree,
                    startFraction: 0.0,
                    endFraction: 1.0,
                    startAngle: -90.0,
                    sweepAngle: 360.0
                )
            ]
        }

        let safeFree = min(effectiveTotal, max(0, freeDeviceBytes))
        let usedBytes = max(0, effectiveTotal - safeFree)
        let otherBytes = max(0, usedBytes - totalMedia)
        let freeBytesAdjusted = max(0, effectiveTotal - (totalMedia + otherBytes))

        let rawItems: [StorageSegment] = [
            StorageSegment(id: "photo_dup", label: "Ảnh trùng lặp", bytes: safePhotosDup, color: colorPhotoDuplicate),
            StorageSegment(id: "video_dup", label: "Video trùng lặp", bytes: safeVideosDup, color: colorVideoDuplicate),
            StorageSegment(id: "photo_unique", label: "Ảnh duy nhất", bytes: safePhotosUnique, color: colorPhotoUnique),
            StorageSegment(id: "video_unique", label: "Video duy nhất", bytes: safeVideosUnique, color: colorVideoUnique),
            StorageSegment(id: "other", label: "Khác", bytes: otherBytes, color: colorOther),
            StorageSegment(id: "free", label: "Còn trống", bytes: freeBytesAdjusted, color: colorFree)
        ]

        let activeItems = rawItems.filter { $0.bytes > 0 }
        if activeItems.isEmpty {
            return [
                StorageSegment(
                    id: "empty",
                    label: "Còn trống",
                    bytes: effectiveTotal,
                    color: colorFree,
                    startFraction: 0.0,
                    endFraction: 1.0,
                    startAngle: -90.0,
                    sweepAngle: 360.0
                )
            ]
        }

        if activeItems.count == 1 {
            var single = activeItems[0]
            single.startFraction = 0.0
            single.endFraction = 1.0
            single.startAngle = -90.0
            single.sweepAngle = 360.0
            return [single]
        }

        let count = activeItems.count
        let totalGapAngle = Double(count) * gapDegrees
        let availableArcDegrees = 360.0 - totalGapAngle

        let sumActiveBytes = activeItems.reduce(0) { $0 + $1.bytes }
        let divisor = (sumActiveBytes > 0) ? Double(sumActiveBytes) : Double(effectiveTotal)

        // Step 1: Initial raw angles
        var angles = activeItems.map { item in
            (Double(item.bytes) / divisor) * availableArcDegrees
        }

        // Step 2: Minimum 3.0° rule
        var totalDeficit: Double = 0.0
        var needsBoost = [Bool](repeating: false, count: count)
        for i in 0..<count {
            if angles[i] < minDegreeThreshold {
                totalDeficit += (minDegreeThreshold - angles[i])
                angles[i] = minDegreeThreshold
                needsBoost[i] = true
            }
        }

        // Step 3: Shrink "Khác" and "Trống" proportionally
        if totalDeficit > 0.0 {
            let khacIndex = activeItems.firstIndex { $0.id == "other" }
            let trongIndex = activeItems.firstIndex { $0.id == "free" }

            var shrinkableKhac: Double = 0.0
            if let idx = khacIndex, !needsBoost[idx] {
                shrinkableKhac = max(0.0, angles[idx] - minDegreeThreshold)
            }

            var shrinkableTrong: Double = 0.0
            if let idx = trongIndex, !needsBoost[idx] {
                shrinkableTrong = max(0.0, angles[idx] - minDegreeThreshold)
            }

            let totalShrinkable = shrinkableKhac + shrinkableTrong
            if totalShrinkable > 0.0 {
                let reduction = min(totalDeficit, totalShrinkable)
                if shrinkableKhac > 0.0, let idx = khacIndex {
                    let drop = reduction * (shrinkableKhac / totalShrinkable)
                    angles[idx] = max(minDegreeThreshold, angles[idx] - drop)
                }
                if shrinkableTrong > 0.0, let idx = trongIndex {
                    let drop = reduction * (shrinkableTrong / totalShrinkable)
                    angles[idx] = max(minDegreeThreshold, angles[idx] - drop)
                }
                totalDeficit -= reduction
            }

            if totalDeficit > 0.0 {
                var otherPool: Double = 0.0
                for i in 0..<count {
                    if !needsBoost[i] && i != khacIndex && i != trongIndex {
                        otherPool += max(0.0, angles[i] - minDegreeThreshold)
                    }
                }
                if otherPool > 0.0 {
                    let reduction = min(totalDeficit, otherPool)
                    for i in 0..<count {
                        if !needsBoost[i] && i != khacIndex && i != trongIndex {
                            let avail = max(0.0, angles[i] - minDegreeThreshold)
                            if avail > 0.0 {
                                angles[i] -= reduction * (avail / otherPool)
                            }
                        }
                    }
                }
            }
        }

        // Step 4: Normalize to exact availableArcDegrees
        let currentSum = angles.reduce(0.0, +)
        if currentSum > 0.0 && currentSum != availableArcDegrees {
            let scale = availableArcDegrees / currentSum
            for i in 0..<count {
                angles[i] *= scale
            }
        }

        // Step 5: Assign angles and fractions
        var currentAngle: Double = -90.0
        var result: [StorageSegment] = []
        for i in 0..<count {
            let sweep = angles[i]
            var item = activeItems[i]
            item.sweepAngle = sweep
            item.startAngle = currentAngle
            let startDegNormalized = (currentAngle + 90.0).truncatingRemainder(dividingBy: 360.0)
            let positiveStartDeg = (startDegNormalized >= 0) ? startDegNormalized : (startDegNormalized + 360.0)
            item.startFraction = CGFloat(positiveStartDeg / 360.0)
            item.endFraction = CGFloat((positiveStartDeg + sweep) / 360.0)
            result.append(item)
            currentAngle += (sweep + gapDegrees)
        }
        return result
    }

    public static func formatBytesLocale(_ bytes: Int64, locale: Locale = .current) -> String {
        guard bytes > 0 else { return "0 B" }
        let kb = Double(bytes) / 1024.0
        let mb = kb / 1024.0
        let gb = mb / 1024.0
        let formatter = NumberFormatter()
        formatter.locale = locale
        formatter.minimumFractionDigits = 0
        if gb >= 1.0 {
            formatter.maximumFractionDigits = 2
            let numStr = formatter.string(from: NSNumber(value: gb)) ?? String(format: "%.2f", gb)
            return "\(numStr) GB"
        } else if mb >= 1.0 {
            formatter.maximumFractionDigits = 1
            let numStr = formatter.string(from: NSNumber(value: mb)) ?? String(format: "%.1f", mb)
            return "\(numStr) MB"
        } else if kb >= 1.0 {
            formatter.maximumFractionDigits = 0
            let numStr = formatter.string(from: NSNumber(value: kb)) ?? String(format: "%.0f", kb)
            return "\(numStr) KB"
        } else {
            return "\(bytes) B"
        }
    }
}
