import SwiftUI
import Photos

public struct PhotoCardView: View {
    public let photo: PhotoItem
    public var isExcluded: Bool = false
    public let onCardTap: () -> Void
    public let onToggleSelect: () -> Void
    public var onToggleExclude: (() -> Void)? = nil

    @State private var thumbnailImage: UIImage?

    public init(
        photo: PhotoItem,
        isExcluded: Bool = false,
        onCardTap: @escaping () -> Void,
        onToggleSelect: @escaping () -> Void,
        onToggleExclude: (() -> Void)? = nil
    ) {
        self.photo = photo
        self.isExcluded = isExcluded
        self.onCardTap = onCardTap
        self.onToggleSelect = onToggleSelect
        self.onToggleExclude = onToggleExclude
    }

    public var body: some View {
        ZStack {
            // Thumbnail image
            if let img = thumbnailImage {
                Image(uiImage: img)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: 110, height: 135)
                    .clipped()
            } else {
                Rectangle()
                    .fill(Color(UIColor.systemGray5))
                    .frame(width: 110, height: 135)
                    .overlay(
                        ProgressView()
                    )
            }

            // Red overlay when selected for deletion
            if photo.isSelected {
                Color.red.opacity(0.25)
                    .frame(width: 110, height: 135)
            }

            // Top-Left: "BEST" badge and/or Excluded Shield badge
            VStack {
                HStack(spacing: 4) {
                    if photo.isBest {
                        Text("BEST")
                            .font(.system(size: 9, weight: .black))
                            .foregroundColor(.white)
                            .padding(.horizontal, 5)
                            .padding(.vertical, 2)
                            .background(Color(red: 1.0, green: 0.7, blue: 0.0))
                            .cornerRadius(4)
                    }
                    if isExcluded {
                        Image(systemName: "shield.fill")
                            .font(.system(size: 9, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 4)
                            .padding(.vertical, 2)
                            .background(Color(red: 0.15, green: 0.2, blue: 0.35))
                            .cornerRadius(4)
                    }
                    Spacer()
                }
                Spacer()
            }
            .padding(6)

            // Video Duration Badge
            if photo.isVideo {
                VStack {
                    Spacer()
                    HStack {
                        HStack(spacing: 3) {
                            Image(systemName: "play.fill")
                                .font(.system(size: 7))
                            Text(photo.formattedDuration)
                                .font(.system(size: 9, weight: .bold))
                        }
                        .foregroundColor(.white)
                        .padding(.horizontal, 5)
                        .padding(.vertical, 2)
                        .background(Color.black.opacity(0.65))
                        .cornerRadius(4)

                        Spacer()
                    }
                }
                .padding(.horizontal, 6)
                .padding(.bottom, 24)
            }

            // Top-Right: Selection Checkbox
            VStack {
                HStack {
                    Spacer()
                    Button(action: onToggleSelect) {
                        ZStack {
                            Circle()
                                .fill(photo.isSelected ? Color.red : Color.black.opacity(0.45))
                                .frame(width: 24, height: 24)
                            Circle()
                                .stroke(Color.white, lineWidth: 1.5)
                                .frame(width: 24, height: 24)

                            if photo.isSelected {
                                Image(systemName: "checkmark")
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundColor(.white)
                            }
                        }
                    }
                }
                Spacer()
            }
            .padding(6)

            // Bottom: Solid Accent Size Ribbon (matching reference 558 KB)
            VStack {
                Spacer()
                HStack {
                    Spacer()
                    Text(photo.formattedSize)
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(Color(red: 0.47, green: 0.21, blue: 0.06))
                    Spacer()
                }
                .padding(.vertical, 3)
                .background(Color(red: 1.0, green: 0.7, blue: 0.0))
            }
        }
        .frame(width: 110, height: 135)
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(photo.isSelected ? Color.red : Color(UIColor.separator), lineWidth: photo.isSelected ? 2.5 : 1)
        )
        .contentShape(Rectangle())
        .onTapGesture {
            onCardTap()
        }
        .contextMenu {
            if let onToggleExclude = onToggleExclude {
                Button(action: onToggleExclude) {
                    Label(
                        isExcluded ? "Bỏ loại trừ khỏi quét" : "Loại trừ ảnh này khỏi quét",
                        systemImage: isExcluded ? "shield.slash.fill" : "shield.fill"
                    )
                }
            }
            Button(action: onToggleSelect) {
                Label(
                    photo.isSelected ? "Bỏ chọn xóa" : "Chọn để xóa",
                    systemImage: photo.isSelected ? "xmark.circle" : "trash"
                )
            }
            Button(action: onCardTap) {
                Label("Xem chi tiết", systemImage: "eye")
            }
        }
        .task {
            loadThumbnail()
        }
    }

    private func loadThumbnail() {
        let manager = PHImageManager.default()
        let options = PHImageRequestOptions()
        options.deliveryMode = .fastFormat
        options.isNetworkAccessAllowed = true

        manager.requestImage(
            for: photo.asset,
            targetSize: CGSize(width: 160, height: 200),
            contentMode: .aspectFill,
            options: options
        ) { image, _ in
            self.thumbnailImage = image
        }
    }
}
