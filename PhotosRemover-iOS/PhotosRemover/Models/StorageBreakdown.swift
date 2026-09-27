import Foundation

public struct StorageBreakdown: Equatable {
    public let photosTotalBytes: Int64
    public let photosDuplicateBytes: Int64   // exact + similar, phần có thể xóa
    public let videosTotalBytes: Int64
    public let videosDuplicateBytes: Int64
    public let librarySizeBytes: Int64        // tổng photos + videos
    public let deviceTotalBytes: Int64        // tổng dung lượng toàn máy (ví dụ 128 GB)
    public let deviceFreeBytes: Int64         // dung lượng còn trống trên máy

    public init(
        photosTotalBytes: Int64,
        photosDuplicateBytes: Int64,
        videosTotalBytes: Int64,
        videosDuplicateBytes: Int64,
        librarySizeBytes: Int64,
        deviceTotalBytes: Int64 = 0,
        deviceFreeBytes: Int64 = 0
    ) {
        self.photosTotalBytes = photosTotalBytes
        self.photosDuplicateBytes = photosDuplicateBytes
        self.videosTotalBytes = videosTotalBytes
        self.videosDuplicateBytes = videosDuplicateBytes
        self.librarySizeBytes = librarySizeBytes
        self.deviceTotalBytes = deviceTotalBytes
        self.deviceFreeBytes = deviceFreeBytes
    }

    public var photosUniqueBytes: Int64 {
        return max(0, photosTotalBytes - photosDuplicateBytes)
    }

    public var videosUniqueBytes: Int64 {
        return max(0, videosTotalBytes - videosDuplicateBytes)
    }

    public var totalDuplicateBytes: Int64 {
        return photosDuplicateBytes + videosDuplicateBytes
    }

    public var deviceUsedBytes: Int64 {
        return max(0, deviceTotalBytes - deviceFreeBytes)
    }

    public var otherBytes: Int64 {
        let mediaBytes = photosDuplicateBytes + videosDuplicateBytes + photosUniqueBytes + videosUniqueBytes
        return max(0, deviceUsedBytes - mediaBytes)
    }
}
