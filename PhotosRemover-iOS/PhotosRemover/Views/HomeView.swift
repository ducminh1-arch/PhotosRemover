import SwiftUI

public struct HomeView: View {
    @ObservedObject public var viewModel: MainViewModel

    public init(viewModel: MainViewModel) {
        self.viewModel = viewModel
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Top Bar
            HStack {
                Button(action: {
                    withAnimation {
                        viewModel.isDrawerOpen = true
                    }
                }) {
                    Image(systemName: "line.3.horizontal")
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundColor(.primary)
                }

                Spacer()

                Text("Remo")
                    .font(.system(size: 20, weight: .black))
                    .foregroundColor(.primary)

                Spacer()

                // Spacing placeholder
                Image(systemName: "line.3.horizontal")
                    .font(.system(size: 22))
                    .opacity(0)
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)

            // Limited library access warning banner
            if viewModel.authStatus == .limited {
                HStack(spacing: 12) {
                    Image(systemName: "photo.on.rectangle.angled")
                        .font(.system(size: 24))
                        .foregroundColor(Color(red: 0x2F / 255.0, green: 0x6B / 255.0, blue: 0xFF / 255.0))

                    VStack(alignment: .leading, spacing: 2) {
                        Text("Partial Photo Access")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(Color(red: 0.12, green: 0.25, blue: 0.7))
                        Text("Remo only sees selected photos. Allow full access in Settings for total library cleaning.")
                            .font(.system(size: 11))
                            .foregroundColor(Color(red: 0.18, green: 0.42, blue: 1.0))
                    }

                    Spacer()

                    Button(action: {
                        viewModel.openSettings()
                    }) {
                        Text("Settings")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color(red: 0x2F / 255.0, green: 0x6B / 255.0, blue: 0xFF / 255.0))
                            .cornerRadius(8)
                    }
                }
                .padding(12)
                .background(Color(red: 0.93, green: 0.96, blue: 1.0))
                .cornerRadius(12)
                .padding(.horizontal, 20)
                .padding(.top, 8)
            } else if viewModel.authStatus == .denied || viewModel.authStatus == .restricted {
                HStack(spacing: 12) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 24))
                        .foregroundColor(Color(red: 0.9, green: 0.2, blue: 0.2))

                    VStack(alignment: .leading, spacing: 2) {
                        Text("Photo Access Denied")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(Color(red: 0.6, green: 0.1, blue: 0.1))
                        Text("Photo library access is disabled. Please enable it in Settings to scan.")
                            .font(.system(size: 11))
                            .foregroundColor(Color(red: 0.7, green: 0.2, blue: 0.2))
                    }

                    Spacer()

                    Button(action: {
                        viewModel.openSettings()
                    }) {
                        Text("Settings")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color(red: 0.9, green: 0.2, blue: 0.2))
                            .cornerRadius(8)
                    }
                }
                .padding(12)
                .background(Color(red: 1.0, green: 0.92, blue: 0.92))
                .cornerRadius(12)
                .padding(.horizontal, 20)
                .padding(.top, 8)
            } else if !viewModel.hasPermission {
                HStack(spacing: 12) {
                    Image(systemName: "lock.shield.fill")
                        .font(.system(size: 24))
                        .foregroundColor(Color(red: 1.0, green: 0.7, blue: 0.0))

                    VStack(alignment: .leading, spacing: 2) {
                        Text("Photo Permission Required")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(Color(red: 0.5, green: 0.3, blue: 0.0))
                        Text("Allow access to detect duplicate photos.")
                            .font(.system(size: 11))
                            .foregroundColor(Color(red: 0.5, green: 0.3, blue: 0.0))
                    }

                    Spacer()

                    Button(action: {
                        Task {
                            await viewModel.requestPermission()
                        }
                    }) {
                        Text("Grant")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color(red: 1.0, green: 0.7, blue: 0.0))
                            .cornerRadius(8)
                    }
                }
                .padding(12)
                .background(Color(red: 1.0, green: 0.97, blue: 0.88))
                .cornerRadius(12)
                .padding(.horizontal, 20)
                .padding(.top, 8)
            }

            ScrollView(showsIndicators: false) {
                VStack(spacing: 16) {
                    // Hero Card
                    VStack(spacing: 16) {
                        ZStack {
                            Circle()
                                .fill(Color.white.opacity(0.18))
                                .frame(width: 80, height: 80)

                            Image(systemName: "photo.stack.fill")
                                .font(.system(size: 38))
                                .foregroundColor(Color(red: 1.0, green: 0.75, blue: 0.0))
                        }

                        Text("Clean Your Gallery")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.white)

                        Text("Find 100% exact duplicates & similar burst photos with one tap.")
                            .font(.system(size: 13))
                            .foregroundColor(.white.opacity(0.85))
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 16)

                        // Stats strip inside hero
                        HStack {
                            VStack(spacing: 4) {
                                Text("\(viewModel.statsStore.cleanedCount)")
                                    .font(.system(size: 18, weight: .bold))
                                    .foregroundColor(.white)
                                Text("Cleaned")
                                    .font(.system(size: 11))
                                    .foregroundColor(.white.opacity(0.75))
                            }
                            .frame(maxWidth: .infinity)

                            Rectangle()
                                .fill(Color.white.opacity(0.2))
                                .frame(width: 1, height: 32)

                            VStack(spacing: 4) {
                                Text(PhotoItem.formatByteSize(viewModel.statsStore.cleanedBytes))
                                    .font(.system(size: 18, weight: .bold))
                                    .foregroundColor(Color(red: 1.0, green: 0.75, blue: 0.0))
                                Text("Space Saved")
                                    .font(.system(size: 11))
                                    .foregroundColor(.white.opacity(0.75))
                            }
                            .frame(maxWidth: .infinity)
                        }
                        .padding(.vertical, 12)
                        .background(Color.white.opacity(0.12))
                        .cornerRadius(12)
                    }
                    .padding(24)
                    .background(
                        LinearGradient(
                            gradient: Gradient(colors: [
                                Color(red: 0.18, green: 0.42, blue: 1.0),
                                Color(red: 0.12, green: 0.25, blue: 0.7)
                            ]),
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                    .cornerRadius(24)
                    .padding(.horizontal, 20)

                    // Storage Breakdown Donut Chart
                    if let breakdown = viewModel.storageBreakdown {
                        StorageDonutChartView(breakdown: breakdown)
                            .padding(.horizontal, 20)
                    }
                }
                .padding(.bottom, 12)
            }

            // Large Scan CTA Button
            Button(action: {
                if !viewModel.hasPermission {
                    Task {
                        await viewModel.requestPermission()
                        if viewModel.hasPermission {
                            viewModel.startScan()
                        }
                    }
                } else {
                    viewModel.startScan()
                }
            }) {
                HStack(spacing: 12) {
                    Image(systemName: "sparkles")
                        .font(.system(size: 20, weight: .bold))
                    Text("Start scan to find duplicates")
                        .font(.system(size: 17, weight: .bold))
                }
                .foregroundColor(Color(red: 0.45, green: 0.2, blue: 0.0))
                .frame(maxWidth: .infinity)
                .frame(height: 58)
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
                .cornerRadius(18)
                .shadow(color: Color(red: 1.0, green: 0.7, blue: 0.0).opacity(0.4), radius: 8, x: 0, y: 4)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 24)
        }
        .background(Color(UIColor.systemGroupedBackground))
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.willEnterForegroundNotification)) { _ in
            viewModel.refreshDeviceStorage()
        }
    }
}
