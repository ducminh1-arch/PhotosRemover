import Foundation

public enum DuplicateSetType: String, Equatable, CaseIterable {
    case exact = "Exact"
    case similar = "Similar"
    case videos = "Videos"
}

public struct DuplicateSet: Identifiable, Equatable {
    public let id: String
    public let title: String
    public let type: DuplicateSetType
    public var photos: [PhotoItem]

    public init(id: String, title: String, type: DuplicateSetType, photos: [PhotoItem]) {
        self.id = id
        self.title = title
        self.type = type
        self.photos = photos
    }

    public var totalSize: Int64 {
        return photos.reduce(0) { $0 + $1.sizeBytes }
    }

    public var selectedCount: Int {
        return photos.filter { $0.isSelected }.count
    }

    public var selectedSize: Int64 {
        return photos.filter { $0.isSelected }.reduce(0) { $0 + $1.sizeBytes }
    }

    public var isAllSelected: Bool {
        return !photos.isEmpty && photos.allSatisfy { $0.isSelected }
    }

    public var hasBestPhoto: Bool {
        return photos.contains { $0.isBest }
    }
}
