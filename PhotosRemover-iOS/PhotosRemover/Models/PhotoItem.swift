import Foundation
import Photos
import SwiftUI

public struct PhotoItem: Identifiable, Equatable {
    public let id: String
    public let asset: PHAsset?
    public let sizeBytes: Int64
    public let dateTaken: Date
    public let pixelWidth: Int
    public let pixelHeight: Int
    public let isFavorite: Bool
    public let isVideo: Bool
    public let durationSeconds: TimeInterval
    public var sha256: String?
    public var dHash: UInt64?
    public var isBest: Bool
    public var isSelected: Bool

    public init(
        id: String,
        asset: PHAsset? = nil,
        sizeBytes: Int64,
        dateTaken: Date,
        pixelWidth: Int,
        pixelHeight: Int,
        isFavorite: Bool = false,
        isVideo: Bool = false,
        durationSeconds: TimeInterval = 0,
        sha256: String? = nil,
        dHash: UInt64? = nil,
        isBest: Bool = false,
        isSelected: Bool = false
    ) {
        self.id = id
        self.asset = asset
        self.sizeBytes = sizeBytes
        self.dateTaken = dateTaken
        self.pixelWidth = pixelWidth
        self.pixelHeight = pixelHeight
        self.isFavorite = isFavorite
        self.isVideo = isVideo
        self.durationSeconds = durationSeconds
        self.sha256 = sha256
        self.dHash = dHash
        self.isBest = isBest
        self.isSelected = isSelected
    }

    public var formattedDuration: String {
        let totalSeconds = Int(durationSeconds)
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        return String(format: "%d:%02d", minutes, seconds)
    }

    public var aspectRatio: Float {
        guard pixelHeight > 0 else { return 1.0 }
        return Float(pixelWidth) / Float(pixelHeight)
    }

    public var formattedSize: String {
        return Self.formatByteSize(sizeBytes)
    }

    public static func formatByteSize(_ bytes: Int64) -> String {
        guard bytes > 0 else { return "0 B" }
        let kb = Double(bytes) / 1024.0
        let mb = kb / 1024.0
        let gb = mb / 1024.0

        if gb >= 1.0 {
            return String(format: "%.2f GB", gb)
        } else if mb >= 1.0 {
            return String(format: "%.1f MB", mb)
        } else if kb >= 1.0 {
            return String(format: "%.0f KB", kb)
        } else {
            return "\(bytes) B"
        }
    }

    public static func == (lhs: PhotoItem, rhs: PhotoItem) -> Bool {
        return lhs.id == rhs.id &&
               lhs.isSelected == rhs.isSelected &&
               lhs.isBest == rhs.isBest
    }
}
