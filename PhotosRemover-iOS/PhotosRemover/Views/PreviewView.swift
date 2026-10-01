import SwiftUI
import Photos

public struct PreviewView: View {
    public let photo: PhotoItem
    public let set: DuplicateSet
    public var isExcluded: Bool = false
    public let onBack: () -> Void
    public let onToggleSelect: () -> Void
    public var onToggleExclude: (() -> Void)? = nil

    @State private var fullImage: UIImage?
    @State private var currentScale: CGFloat = 1.0
    @State private var offset: CGSize = .zero

    public init(
        photo: PhotoItem,
        set: DuplicateSet,
        isExcluded: Bool = false,
        onBack: @escaping () -> Void,
        onToggleSelect: @escaping () -> Void,
        onToggleExclude: (() -> Void)? = nil
    ) {
        self.photo = photo
        self.set = set
        self.isExcluded = isExcluded
        self.onBack = onBack
        self.onToggleSelect = onToggleSelect
        self.onToggleExclude = onToggleExclude
    }

    private var formattedDate: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        return formatter.string(from: photo.dateTaken)
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Primary Header (#2F6BFF)
            HStack {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.white)
                }

                Spacer()

                Text("Preview")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)

                Spacer()

                HStack(spacing: 16) {
                    if let onToggleExclude = onToggleExclude {
                        Button(action: onToggleExclude) {
                            Image(systemName: isExcluded ? "shield.fill" : "shield")
                                .font(.system(size: 20))
                                .foregroundColor(isExcluded ? Color(red: 1.0, green: 0.78, blue: 0.0) : .white)
                        }
                    }

                    Button(action: onToggleSelect) {
                        Image(systemName: photo.isSelected ? "trash.fill" : "trash")
                            .font(.system(size: 20))
                            .foregroundColor(photo.isSelected ? .red : .white)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color(red: 0x2F / 255.0, green: 0x6B / 255.0, blue: 0xFF / 255.0))

            // Full Photo Preview with Pinch-to-zoom
            ZStack {
                Color.black.edgesIgnoringSafeArea(.all)

                if let img = fullImage {
                    Image(uiImage: img)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .scaleEffect(currentScale)
                        .offset(offset)
                        .gesture(
                            MagnificationGesture()
                                .onChanged { val in
                                    currentScale = max(1.0, min(val, 4.0))
                                }
                                .onEnded { _ in
                                    if currentScale < 1.0 {
                                        currentScale = 1.0
                                    }
                                }
                        )
                        .gesture(
                            DragGesture()
                                .onChanged { val in
                                    if currentScale > 1.0 {
                                        offset = val.translation
                                    }
                                }
                                .onEnded { _ in
                                    if currentScale <= 1.0 {
                                        offset = .zero
                                    }
                                }
                        )
                } else {
                    ProgressView()
                        .tint(.white)
                }

                if photo.isBest {
                    VStack {
                        HStack {
                            Text("★ RECOMMENDED TO KEEP")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(Color(red: 1.0, green: 0.7, blue: 0.0))
                                .cornerRadius(8)
                            Spacer()
                        }
                        Spacer()
                    }
                    .padding(16)
                }

                if isExcluded {
                    VStack {
                        HStack {
                            Spacer()
                            HStack(spacing: 4) {
                                Image(systemName: "shield.fill")
                                    .font(.system(size: 11))
                                Text("ĐÃ LOẠI TRỪ KHỎI QUÉT")
                                    .font(.system(size: 11, weight: .bold))
                            }
                            .foregroundColor(.white)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(Color(red: 0.1, green: 0.15, blue: 0.25).opacity(0.85))
                            .cornerRadius(8)
                        }
                        Spacer()
                    }
                    .padding(16)
                }
            }

            // Bottom EXIF & Metadata Bar
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    VStack(alignment: .leading, spacing: 3) {
                        Text("File size: \(photo.formattedSize)")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(.white)
                        Text("Date: \(formattedDate)")
                            .font(.system(size: 12))
                            .foregroundColor(.gray)
                        Text("Resolution: \(photo.pixelWidth) × \(photo.pixelHeight)")
                            .font(.system(size: 12))
                            .foregroundColor(.gray)
                    }

                    Spacer()

                    HStack(spacing: 8) {
                        if let onToggleExclude = onToggleExclude {
                            Button(action: onToggleExclude) {
                                HStack(spacing: 4) {
                                    Image(systemName: isExcluded ? "shield.fill" : "shield")
                                        .font(.system(size: 12, weight: .bold))
                                    Text(isExcluded ? "Đã loại trừ" : "Loại trừ")
                                        .font(.system(size: 12, weight: .bold))
                                }
                                .foregroundColor(isExcluded ? .red : .white)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 8)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10)
                                        .stroke(isExcluded ? Color.red : Color.white.opacity(0.4), lineWidth: 1)
                                )
                            }
                        }

                        Button(action: onToggleSelect) {
                            HStack(spacing: 6) {
                                Image(systemName: photo.isSelected ? "checkmark" : "trash.fill")
                                    .font(.system(size: 13, weight: .bold))
                                Text(photo.isSelected ? "Marked" : "Delete")
                                    .font(.system(size: 12, weight: .bold))
                            }
                            .foregroundColor(photo.isSelected ? .white : Color(red: 0.45, green: 0.2, blue: 0.0))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(photo.isSelected ? Color.red : Color(red: 1.0, green: 0.78, blue: 0.0))
                            .cornerRadius(10)
                        }
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 14)
            .background(Color(red: 0.1, green: 0.1, blue: 0.12))
        }
        .task {
            loadHighResImage()
        }
    }

    private func loadHighResImage() {
        guard let asset = photo.asset else { return }
        let manager = PHImageManager.default()
        let options = PHImageRequestOptions()
        options.deliveryMode = .highQualityFormat
        options.isNetworkAccessAllowed = true

        manager.requestImage(
            for: asset,
            targetSize: PHImageManagerMaximumSize,
            contentMode: .aspectFit,
            options: options
        ) { image, _ in
            self.fullImage = image
        }
    }
}
