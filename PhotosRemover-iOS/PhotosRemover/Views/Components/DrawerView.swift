import SwiftUI

public struct DrawerView: View {
    @ObservedObject public var statsStore: CleanedStatsStore
    public var excludedCount: Int = 0
    public let onClose: () -> Void
    public var onClearExclusions: (() -> Void)? = nil
    public let onFeedback: () -> Void
    public let onRate: () -> Void
    public let onAbout: () -> Void

    @State private var showClearExclusionConfirm: Bool = false

    public init(
        statsStore: CleanedStatsStore,
        excludedCount: Int = 0,
        onClose: @escaping () -> Void,
        onClearExclusions: (() -> Void)? = nil,
        onFeedback: @escaping () -> Void,
        onRate: @escaping () -> Void,
        onAbout: @escaping () -> Void
    ) {
        self.statsStore = statsStore
        self.excludedCount = excludedCount
        self.onClose = onClose
        self.onClearExclusions = onClearExclusions
        self.onFeedback = onFeedback
        self.onRate = onRate
        self.onAbout = onAbout
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Header
            HStack {
                Text("Menu & Stats")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.primary)

                Spacer()

                Button(action: onClose) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 24))
                        .foregroundColor(.secondary)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 24)
            .padding(.bottom, 16)

            ScrollView {
                VStack(spacing: 20) {
                    // BrandPrimary Stats Card with BrandAccent counter
                    VStack(spacing: 10) {
                        ZStack {
                            Circle()
                                .fill(Color.white)
                                .frame(width: 80, height: 80)
                                .overlay(
                                    Circle().stroke(Color(red: 1.0, green: 0.7, blue: 0.0), lineWidth: 3)
                                )

                            Text("\(statsStore.cleanedCount)")
                                .font(.system(size: 32, weight: .black))
                                .foregroundColor(Color(red: 0x2F / 255.0, green: 0x6B / 255.0, blue: 0xFF / 255.0))
                        }

                        Text("Photos cleaned so far")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)

                        Text("Storage freed: \(PhotoItem.formatByteSize(statsStore.cleanedBytes))")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(Color(red: 1.0, green: 0.7, blue: 0.0))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 24)
                    .background(
                        LinearGradient(
                            gradient: Gradient(colors: [
                                Color(red: 0x2F / 255.0, green: 0x6B / 255.0, blue: 0xFF / 255.0),
                                Color(red: 0x1A / 255.0, green: 0x4D / 255.0, blue: 0xBE / 255.0)
                            ]),
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                    .cornerRadius(20)
                    .padding(.horizontal, 16)

                    // Sensitivity Section
                    VStack(alignment: .leading, spacing: 10) {
                        HStack(spacing: 8) {
                            Image(systemName: "slider.horizontal.3")
                                .foregroundColor(Color(red: 0.18, green: 0.42, blue: 1.0))
                            Text("Similar Sensitivity")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(.primary)
                        }

                        ForEach(SimilarSensitivity.allCases) { option in
                            Button(action: {
                                statsStore.sensitivity = option
                            }) {
                                HStack {
                                    Image(systemName: statsStore.sensitivity == option ? "largecircle.fill.circle" : "circle")
                                        .foregroundColor(statsStore.sensitivity == option ? Color(red: 0.18, green: 0.42, blue: 1.0) : .secondary)

                                    Text(option.displayLabel)
                                        .font(.system(size: 13, weight: statsStore.sensitivity == option ? .bold : .regular))
                                        .foregroundColor(statsStore.sensitivity == option ? Color(red: 0.18, green: 0.42, blue: 1.0) : .primary)

                                    Spacer()
                                }
                                .padding(.vertical, 4)
                            }
                            .buttonStyle(PlainButtonStyle())
                        }
                    }
                    .padding(16)
                    .background(Color(UIColor.secondarySystemGroupedBackground))
                    .cornerRadius(16)
                    .padding(.horizontal, 16)

                    // Menu Options
                    VStack(spacing: 0) {
                        menuRow(
                            icon: "shield.slash.fill",
                            title: "Ảnh đã loại trừ (\(excludedCount))",
                            action: {
                                if excludedCount > 0 {
                                    showClearExclusionConfirm = true
                                }
                            }
                        )
                        Divider().padding(.leading, 48)
                        menuRow(icon: "envelope.fill", title: "Feedback", action: onFeedback)
                        Divider().padding(.leading, 48)
                        menuRow(icon: "star.fill", title: "Rate App", action: onRate)
                        Divider().padding(.leading, 48)
                        menuRow(icon: "info.circle.fill", title: "About Remo", action: onAbout)
                    }
                    .background(Color(UIColor.secondarySystemGroupedBackground))
                    .cornerRadius(16)
                    .padding(.horizontal, 16)
                }
                .padding(.bottom, 32)
            }
        }
        .frame(width: 310)
        .background(Color(UIColor.systemGroupedBackground))
        .alert(isPresented: $showClearExclusionConfirm) {
            Alert(
                title: Text("Bỏ loại trừ tất cả"),
                message: Text("Bạn có muốn đặt lại danh sách loại trừ (\(excludedCount) ảnh) để quét lại không?"),
                primaryButton: .destructive(Text("Đặt lại")) {
                    onClearExclusions?()
                },
                secondaryButton: .cancel(Text("Hủy"))
            )
        }
    }

    private func menuRow(icon: String, title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 14) {
                Image(systemName: icon)
                    .font(.system(size: 18))
                    .foregroundColor(.secondary)
                    .frame(width: 24)

                Text(title)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(.primary)

                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
        }
        .buttonStyle(PlainButtonStyle())
    }
}
