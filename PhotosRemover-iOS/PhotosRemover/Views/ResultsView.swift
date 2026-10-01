import SwiftUI

public struct ResultsView: View {
    @ObservedObject public var viewModel: MainViewModel

    public enum SetSortOrder: String, CaseIterable {
        case sizeDesc = "Lớn nhất"
        case sizeAsc = "Nhỏ nhất"
        case countDesc = "Nhiều tệp"
        case original = "Thứ tự gốc"
    }

    @State private var showDeleteAllWarning: Bool = false
    @State private var sortOrder: SetSortOrder = .sizeDesc

    public init(viewModel: MainViewModel) {
        self.viewModel = viewModel
    }

    private var activeSets: [DuplicateSet] {
        switch viewModel.selectedTab {
        case .exact: return viewModel.exactSets
        case .similar: return viewModel.similarSets
        case .videos: return viewModel.videoSets
        }
    }

    private var sortedSets: [DuplicateSet] {
        switch sortOrder {
        case .sizeDesc:
            return activeSets.sorted { $0.totalSize > $1.totalSize }
        case .sizeAsc:
            return activeSets.sorted { $0.totalSize < $1.totalSize }
        case .countDesc:
            return activeSets.sorted { $0.photos.count > $1.photos.count }
        case .original:
            return activeSets
        }
    }

    private var emptyTitle: String {
        switch viewModel.selectedTab {
        case .exact: return "No Exact Duplicates Found!"
        case .similar: return "No Similar Photos Found!"
        case .videos: return "No Duplicate Videos Found!"
        }
    }

    private var totalSelectedCount: Int {
        return activeSets.reduce(0) { $0 + $1.selectedCount }
    }

    private var totalSelectedSize: Int64 {
        return activeSets.reduce(0) { $0 + $1.selectedSize }
    }

    private var hasAnySetWithAllSelected: Bool {
        return activeSets.contains { $0.isAllSelected }
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Top Bar
            HStack {
                Button(action: {
                    viewModel.currentScreen = .home
                }) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.primary)
                }

                Spacer()

                Text("CleanPix Results")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.primary)

                Spacer()

                Button(action: {
                    withAnimation {
                        viewModel.isDrawerOpen = true
                    }
                }) {
                    Image(systemName: "line.3.horizontal")
                        .font(.system(size: 20))
                        .foregroundColor(.primary)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 10)

            // Segmented Tab Picker: Exact vs Similar vs Videos
            Picker("Duplicate Type", selection: $viewModel.selectedTab) {
                Text("Exact (\(viewModel.exactSets.count))").tag(DuplicateSetType.exact)
                Text("Similar (\(viewModel.similarSets.count))").tag(DuplicateSetType.similar)
                Text("Videos (\(viewModel.videoSets.count))").tag(DuplicateSetType.videos)
            }
            .pickerStyle(SegmentedPickerStyle())
            .padding(.horizontal, 16)
            .padding(.bottom, 8)

            // iCloud Skipped Photos Notice
            if viewModel.selectedTab == .exact && viewModel.skippedICloudCount > 0 {
                HStack(spacing: 8) {
                    Image(systemName: "icloud.slash")
                        .foregroundColor(Color(red: 0x2F / 255.0, green: 0x6B / 255.0, blue: 0xFF / 255.0))
                    Text("\(viewModel.skippedICloudCount) ảnh trên iCloud chưa được quét")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(.secondary)
                    Spacer()
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color(red: 0x2F / 255.0, green: 0x6B / 255.0, blue: 0xFF / 255.0).opacity(0.1))
                .cornerRadius(8)
                .padding(.horizontal, 16)
                .padding(.bottom, 6)
            }

            // Sort Toolbar Strip
            if !activeSets.isEmpty {
                HStack {
                    Text("\(activeSets.count) nhóm • \(PhotoItem.formatByteSize(activeSets.reduce(0) { $0 + $1.totalSize }))")
                        .font(.system(size: 12.5, weight: .bold))
                        .foregroundColor(.primary)

                    Spacer()

                    Menu {
                        ForEach(SetSortOrder.allCases, id: \.self) { order in
                            Button(action: { sortOrder = order }) {
                                HStack {
                                    Text(order.rawValue)
                                    if sortOrder == order {
                                        Image(systemName: "checkmark")
                                    }
                                }
                            }
                        }
                    } label: {
                        HStack(spacing: 4) {
                            Image(systemName: "arrow.up.arrow.down")
                                .font(.system(size: 11))
                            Text(sortOrder.rawValue)
                                .font(.system(size: 12, weight: .medium))
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color(.systemGray6))
                        .cornerRadius(6)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 6)
                .background(Color(.systemBackground))
            }

            // Sets List or Empty State
            if activeSets.isEmpty {
                ScrollView {
                    VStack(spacing: 14) {
                        Spacer().frame(height: 4)

                        if let breakdown = viewModel.storageBreakdown {
                            StorageDonutChartView(breakdown: breakdown)
                                .padding(.horizontal, 14)
                                .padding(.bottom, 4)
                        }

                        Spacer().frame(height: 36)

                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 56))
                            .foregroundColor(Color(red: 1.0, green: 0.7, blue: 0.0))

                        Text(emptyTitle)
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.primary)

                        Text("Your library is clean in this category.")
                            .font(.system(size: 13))
                            .foregroundColor(.secondary)

                        Spacer().frame(height: 36)
                    }
                }
            } else {
                ScrollView {
                    LazyVStack(spacing: 8) {
                        Spacer().frame(height: 4)

                        if let breakdown = viewModel.storageBreakdown {
                            StorageDonutChartView(breakdown: breakdown)
                                .padding(.horizontal, 14)
                                .padding(.bottom, 4)
                        }

                        ForEach(sortedSets) { set in
                            SetCardView(
                                set: set,
                                isPhotoExcluded: { photoId in
                                    viewModel.isExcluded(photoId)
                                },
                                onPhotoTap: { photo in
                                    viewModel.openPreview(photo: photo, set: set)
                                },
                                onTogglePhotoSelect: { photoId in
                                    viewModel.togglePhotoSelection(setId: set.id, photoId: photoId)
                                },
                                onToggleSetSelect: {
                                    viewModel.toggleSetSelection(setId: set.id)
                                },
                                onSelectAllExceptBest: {
                                    viewModel.selectAllExceptBest(setId: set.id)
                                },
                                onTogglePhotoExclude: { photoId in
                                    viewModel.toggleExclusion(photoId: photoId)
                                }
                            )
                        }
                        Spacer().frame(height: 16)
                    }
                }
            }

            // Sticky Bottom Action Bar
            BottomActionBarView(
                selectedCount: totalSelectedCount,
                selectedSizeBytes: totalSelectedSize,
                onDeleteTap: {
                    if hasAnySetWithAllSelected {
                        showDeleteAllWarning = true
                    } else {
                        viewModel.deleteSelectedPhotos()
                    }
                }
            )
        }
        .background(Color(UIColor.systemGroupedBackground))
        .alert(isPresented: $showDeleteAllWarning) {
            Alert(
                title: Text("Cảnh báo xóa ảnh"),
                message: Text("Bạn đã chọn xóa TOÀN BỘ ảnh trong một hoặc nhiều nhóm. Nếu xóa, bạn sẽ không còn bản sao nào của những ảnh này trong thư viện. Bạn có chắc chắn muốn tiếp tục?"),
                primaryButton: .destructive(Text("Xóa toàn bộ")) {
                    viewModel.deleteSelectedPhotos()
                },
                secondaryButton: .cancel(Text("Xem lại"))
            )
        }
        .alert(isPresented: $viewModel.showAlert) {
            Alert(
                title: Text("Photo Cleanup"),
                message: Text(viewModel.alertMessage ?? ""),
                dismissButton: .default(Text("OK"))
            )
        }
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.willEnterForegroundNotification)) { _ in
            viewModel.refreshDeviceStorage()
        }
    }
}
