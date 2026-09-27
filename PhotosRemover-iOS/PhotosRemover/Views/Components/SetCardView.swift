import SwiftUI

public struct SetCardView: View {
    public let set: DuplicateSet
    public var isPhotoExcluded: ((String) -> Bool)? = nil
    public let onPhotoTap: (PhotoItem) -> Void
    public let onTogglePhotoSelect: (String) -> Void
    public let onToggleSetSelect: () -> Void
    public let onSelectAllExceptBest: () -> Void
    public var onTogglePhotoExclude: ((String) -> Void)? = nil

    public init(
        set: DuplicateSet,
        isPhotoExcluded: ((String) -> Bool)? = nil,
        onPhotoTap: @escaping (PhotoItem) -> Void,
        onTogglePhotoSelect: @escaping (String) -> Void,
        onToggleSetSelect: @escaping () -> Void,
        onSelectAllExceptBest: @escaping () -> Void,
        onTogglePhotoExclude: ((String) -> Void)? = nil
    ) {
        self.set = set
        self.isPhotoExcluded = isPhotoExcluded
        self.onPhotoTap = onPhotoTap
        self.onTogglePhotoSelect = onTogglePhotoSelect
        self.onToggleSetSelect = onToggleSetSelect
        self.onSelectAllExceptBest = onSelectAllExceptBest
        self.onTogglePhotoExclude = onTogglePhotoExclude
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            // Header
            HStack {
                Button(action: onToggleSetSelect) {
                    HStack(spacing: 8) {
                        ZStack {
                            Circle()
                                .fill(set.isAllSelected ? Color.red : Color(UIColor.systemGray4))
                                .frame(width: 20, height: 20)

                            if set.isAllSelected {
                                Image(systemName: "checkmark")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(.white)
                            }
                        }

                        Text(set.title)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.primary)

                        Text("(\(set.photos.count) photos • \(PhotoItem.formatByteSize(set.totalSize)))")
                            .font(.system(size: 12))
                            .foregroundColor(.secondary)
                    }
                }
                .buttonStyle(PlainButtonStyle())

                Spacer()

                // Auto-pick action
                if set.type == .similar || !set.isAllSelected {
                    Button(action: onSelectAllExceptBest) {
                        Text("Auto-pick")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(Color(red: 0.18, green: 0.42, blue: 1.0))
                    }
                }
            }
            .padding(.horizontal, 14)
            .padding(.top, 12)

            // Horizontal Photo ScrollView
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(set.photos) { photo in
                        PhotoCardView(
                            photo: photo,
                            isExcluded: isPhotoExcluded?(photo.id) ?? false,
                            onCardTap: { onPhotoTap(photo) },
                            onToggleSelect: { onTogglePhotoSelect(photo.id) },
                            onToggleExclude: { onTogglePhotoExclude?(photo.id) }
                        )
                    }
                }
                .padding(.horizontal, 14)
                .padding(.bottom, 12)
            }
        }
        .background(Color(UIColor.secondarySystemGroupedBackground))
        .cornerRadius(16)
        .shadow(color: Color.black.opacity(0.04), radius: 6, x: 0, y: 2)
        .padding(.horizontal, 14)
        .padding(.vertical, 4)
    }
}
