import SwiftUI

public struct StorageDonutChartView: View {
    public let breakdown: StorageBreakdown
    @State private var animatedFraction: CGFloat = 0.0
    @State private var showOtherDetails = false

    public init(breakdown: StorageBreakdown) {
        self.breakdown = breakdown
    }

    private var segments: [StorageSegment] {
        DeviceStorageService.calculateStorageArcs(
            totalDeviceBytes: breakdown.deviceTotalBytes,
            freeDeviceBytes: breakdown.deviceFreeBytes,
            photosDupBytes: breakdown.photosDuplicateBytes,
            videosDupBytes: breakdown.videosDuplicateBytes,
            photosUniqueBytes: breakdown.photosUniqueBytes,
            videosUniqueBytes: breakdown.videosUniqueBytes
        )
    }

    private var usedFormatted: String {
        DeviceStorageService.formatBytesLocale(breakdown.deviceUsedBytes)
    }

    private var totalFormatted: String {
        DeviceStorageService.formatBytesLocale(breakdown.deviceTotalBytes)
    }

    private var freeFormatted: String {
        DeviceStorageService.formatBytesLocale(breakdown.deviceFreeBytes)
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            // Header
            HStack {
                Text("Dung lượng thiết bị")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(.primary)

                Spacer()

                if breakdown.totalDuplicateBytes > 0 {
                    Text("Tiết kiệm \(PhotoItem.formatByteSize(breakdown.totalDuplicateBytes))")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(Color(red: 0.9, green: 0.2, blue: 0.2))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color(red: 0.9, green: 0.2, blue: 0.2).opacity(0.12))
                        .cornerRadius(6)
                }
            }

            // Donut Chart
            HStack {
                Spacer()
                ZStack {
                    // Gray background track
                    Circle()
                        .stroke(Color(UIColor.systemGray5), lineWidth: 22)
                        .frame(width: 150, height: 150)

                    // Segments with 600ms easeOut animation
                    ForEach(segments) { slice in
                        let length = slice.endFraction - slice.startFraction
                        let currentLength = length * animatedFraction
                        Circle()
                            .trim(from: slice.startFraction, to: slice.startFraction + currentLength)
                            .stroke(slice.color, style: StrokeStyle(lineWidth: 22, lineCap: .butt))
                            .rotationEffect(.degrees(-90))
                            .frame(width: 150, height: 150)
                    }

                    // Center info
                    VStack(spacing: 2) {
                        Text("Đã dùng \(usedFormatted)")
                            .font(.system(size: 15, weight: .black))
                            .foregroundColor(.primary)
                        Text("/ \(totalFormatted)")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundColor(.secondary)
                    }
                }
                Spacer()
            }
            .padding(.vertical, 6)

            // 6-item 2-column Legend
            VStack(spacing: 8) {
                // Row 1: Ảnh trùng lặp (Đỏ) & Video trùng lặp (Cam)
                HStack {
                    legendItem(
                        label: "Ảnh trùng lặp",
                        bytes: breakdown.photosDuplicateBytes,
                        color: DeviceStorageService.colorPhotoDuplicate
                    )
                    Spacer()
                    legendItem(
                        label: "Video trùng lặp",
                        bytes: breakdown.videosDuplicateBytes,
                        color: DeviceStorageService.colorVideoDuplicate
                    )
                }

                // Row 2: Ảnh duy nhất (Xanh) & Video duy nhất (Tím)
                HStack {
                    legendItem(
                        label: "Ảnh duy nhất",
                        bytes: breakdown.photosUniqueBytes,
                        color: DeviceStorageService.colorPhotoUnique
                    )
                    Spacer()
                    legendItem(
                        label: "Video duy nhất",
                        bytes: breakdown.videosUniqueBytes,
                        color: DeviceStorageService.colorVideoUnique
                    )
                }

                // Row 3: Khác (Xám) & Còn trống (Xám nhạt)
                HStack {
                    legendItem(
                        label: "Khác",
                        bytes: breakdown.otherBytes,
                        color: DeviceStorageService.colorOther,
                        isClickable: true,
                        action: { showOtherDetails = true }
                    )
                    Spacer()
                    legendItem(
                        label: "Còn trống",
                        bytes: breakdown.deviceFreeBytes,
                        color: DeviceStorageService.colorFree
                    )
                }

                // Interactive hint pill
                Button(action: { showOtherDetails = true }) {
                    HStack(spacing: 6) {
                        Image(systemName: "info.circle")
                            .font(.system(size: 13))
                            .foregroundColor(Color(red: 0.4, green: 0.45, blue: 0.55))
                        Text("Nhấn vào \"Khác\" để xem phân tích dữ liệu \(DeviceStorageService.formatBytesLocale(breakdown.otherBytes))")
                            .font(.system(size: 11.5, weight: .medium))
                            .foregroundColor(Color(red: 0.25, green: 0.3, blue: 0.4))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 7)
                    .background(Color(UIColor.tertiarySystemGroupedBackground))
                    .cornerRadius(10)
                }
                .buttonStyle(PlainButtonStyle())
                .padding(.top, 4)
            }
            .padding(.top, 4)
        }
        .padding(16)
        .background(Color(UIColor.secondarySystemGroupedBackground))
        .cornerRadius(18)
        .shadow(color: Color.black.opacity(0.04), radius: 6, x: 0, y: 2)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Dung lượng thiết bị: Đã dùng \(usedFormatted) trên \(totalFormatted)")
        .accessibilityValue(
            "Ảnh trùng lặp: \(DeviceStorageService.formatBytesLocale(breakdown.photosDuplicateBytes)), " +
            "Video trùng lặp: \(DeviceStorageService.formatBytesLocale(breakdown.videosDuplicateBytes)), " +
            "Ảnh duy nhất: \(DeviceStorageService.formatBytesLocale(breakdown.photosUniqueBytes)), " +
            "Video duy nhất: \(DeviceStorageService.formatBytesLocale(breakdown.videosUniqueBytes)), " +
            "Khác: \(DeviceStorageService.formatBytesLocale(breakdown.otherBytes)), " +
            "Còn trống: \(freeFormatted)"
        )
        .onAppear {
            withAnimation(.easeOut(duration: 0.6)) {
                animatedFraction = 1.0
            }
        }
        .sheet(isPresented: $showOtherDetails) {
            OtherStorageDetailsView(breakdown: breakdown)
        }
    }

    private func legendItem(
        label: String,
        bytes: Int64,
        color: Color,
        isClickable: Bool = false,
        action: (() -> Void)? = nil
    ) -> some View {
        Button(action: {
            if isClickable { action?() }
        }) {
            HStack(spacing: 8) {
                Circle()
                    .fill(color)
                    .frame(width: 10, height: 10)

                VStack(alignment: .leading, spacing: 1) {
                    HStack(spacing: 4) {
                        Text(label)
                            .font(.system(size: 11, weight: .medium))
                            .foregroundColor(.secondary)
                        if isClickable {
                            Image(systemName: "info.circle")
                                .font(.system(size: 11))
                                .foregroundColor(.secondary)
                        }
                    }
                    Text(DeviceStorageService.formatBytesLocale(bytes))
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(.primary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.vertical, isClickable ? 2 : 0)
        }
        .buttonStyle(PlainButtonStyle())
        .disabled(!isClickable)
    }
}

public struct OtherStorageDetailsView: View {
    public let breakdown: StorageBreakdown
    @Environment(\.dismiss) private var dismiss

    public var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    // Header card
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Dữ liệu \"Khác\" bao gồm gì?")
                                .font(.system(size: 17, weight: .bold))
                                .foregroundColor(.primary)
                            Text("Toàn bộ tài nguyên ngoài Thư viện Ảnh & Video")
                                .font(.system(size: 12))
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        Text(DeviceStorageService.formatBytesLocale(breakdown.otherBytes))
                            .font(.system(size: 14, weight: .black))
                            .foregroundColor(Color(red: 0.3, green: 0.35, blue: 0.45))
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(Color.gray.opacity(0.12))
                            .cornerRadius(10)
                    }
                    .padding(.top, 4)

                    Text("Ứng dụng này chuyên dọn dẹp Thư viện Ảnh & Video. Phần \(DeviceStorageService.formatBytesLocale(breakdown.otherBytes)) này là toàn bộ các tài nguyên khác đang chiếm bộ nhớ máy của bạn:")
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                        .lineSpacing(3)

                    // Categories
                    VStack(spacing: 10) {
                        otherCategoryCard(
                            icon: "square.grid.2x2.fill",
                            iconColor: Color(red: 0.23, green: 0.51, blue: 0.96),
                            title: "Ứng dụng & Dữ liệu ứng dụng (Apps & Games)",
                            subtitle: "Facebook, Zalo, TikTok, YouTube, Messenger, các game... Dữ liệu tin nhắn, ảnh/video lưu riêng trong Zalo/Telegram thuộc nhóm này.",
                            estimate: "Thường chiếm ~35 - 50 GB"
                        )

                        otherCategoryCard(
                            icon: "applelogo",
                            iconColor: Color(red: 0.1, green: 0.75, blue: 0.5),
                            title: "Hệ điều hành iOS (iOS System)",
                            subtitle: "Bản thân hệ điều hành iOS, tệp nhân hệ thống và các bản cập nhật phần mềm định kỳ.",
                            estimate: "Thường chiếm ~10 - 15 GB"
                        )

                        otherCategoryCard(
                            icon: "bolt.fill",
                            iconColor: Color(red: 0.96, green: 0.62, blue: 0.04),
                            title: "Dữ liệu hệ thống & Bộ nhớ đệm (Cache)",
                            subtitle: "Bộ nhớ đệm Safari, streaming video tạm thời, giọng nói Siri, fonts, logs hệ thống.",
                            estimate: "Thường chiếm ~5 - 10 GB"
                        )

                        otherCategoryCard(
                            icon: "folder.fill",
                            iconColor: Color(red: 0.55, green: 0.36, blue: 0.96),
                            title: "Tệp tin trong ứng dụng Tệp (Files)",
                            subtitle: "Tệp tải về từ Safari, tài liệu PDF, file nén .zip được lưu trên máy.",
                            estimate: "Thường chiếm ~2 - 5 GB"
                        )

                        otherCategoryCard(
                            icon: "trash.fill",
                            iconColor: Color(red: 0.94, green: 0.27, blue: 0.27),
                            title: "Album \"Đã xóa gần đây\" (Trash)",
                            subtitle: "Ảnh/video bạn đã xóa trong 30 ngày qua nhưng chưa dọn sạch khỏi album Thùng rác của máy.",
                            estimate: "Được giữ trong 30 ngày để khôi phục"
                        )
                    }

                    Spacer().frame(height: 10)

                    // Open Settings button
                    Button(action: {
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            UIApplication.shared.open(url)
                        }
                    }) {
                        HStack {
                            Image(systemName: "gear")
                                .font(.system(size: 16, weight: .bold))
                            Text("Mở Cài đặt iPhone (Xem từng App)")
                                .font(.system(size: 15, weight: .bold))
                        }
                        .foregroundColor(Color(red: 0.45, green: 0.2, blue: 0.0))
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .background(
                            LinearGradient(
                                gradient: Gradient(colors: [
                                    Color(red: 1.0, green: 0.78, blue: 0.0),
                                    Color(red: 1.0, green: 0.7, blue: 0.0)
                                ]),
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .cornerRadius(14)
                    }

                    Button(action: { dismiss() }) {
                        Text("Đã hiểu")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(.secondary)
                            .frame(maxWidth: .infinity)
                            .frame(height: 44)
                            .background(Color(UIColor.tertiarySystemGroupedBackground))
                            .cornerRadius(14)
                    }
                }
                .padding(20)
            }
            .navigationBarTitle("Chi tiết bộ nhớ", displayMode: .inline)
            .navigationBarItems(trailing: Button("Đóng") { dismiss() })
        }
    }

    private func otherCategoryCard(
        icon: String,
        iconColor: Color,
        title: String,
        subtitle: String,
        estimate: String
    ) -> some View {
        HStack(alignment: .top, spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 10)
                    .fill(iconColor.opacity(0.12))
                    .frame(width: 36, height: 36)
                Image(systemName: icon)
                    .font(.system(size: 18))
                    .foregroundColor(iconColor)
            }

            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(.primary)
                Text(subtitle)
                    .font(.system(size: 11.5))
                    .foregroundColor(.secondary)
                    .lineSpacing(2)
                Text(estimate)
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(iconColor)
                    .padding(.top, 2)
            }
            Spacer()
        }
        .padding(12)
        .background(Color(UIColor.secondarySystemGroupedBackground))
        .cornerRadius(14)
    }
}
