import SwiftUI

@main
struct PhotosRemoverApp: App {
    @StateObject private var viewModel = MainViewModel()

    var body: some Scene {
        WindowGroup {
            ZStack(alignment: .leading) {
                // Main Screen Switching
                Group {
                    switch viewModel.currentScreen {
                    case .home:
                        HomeView(viewModel: viewModel)
                    case .scanning:
                        ScanningView(viewModel: viewModel)
                    case .results:
                        ResultsView(viewModel: viewModel)
                    case .preview:
                        if let photo = viewModel.previewPhoto, let set = viewModel.previewSet {
                            PreviewView(
                                photo: photo,
                                set: set,
                                isExcluded: viewModel.isExcluded(photo.id),
                                onBack: { viewModel.closePreview() },
                                onToggleSelect: {
                                    viewModel.togglePhotoSelection(setId: set.id, photoId: photo.id)
                                },
                                onToggleExclude: {
                                    viewModel.toggleExclusion(for: photo)
                                }
                            )
                        }
                    }
                }

                // Drawer Backdrop
                if viewModel.isDrawerOpen {
                    Color.black.opacity(0.4)
                        .edgesIgnoringSafeArea(.all)
                        .onTapGesture {
                            withAnimation {
                                viewModel.isDrawerOpen = false
                            }
                        }
                        .transition(.opacity)

                    DrawerView(
                        statsStore: viewModel.statsStore,
                        excludedCount: viewModel.exclusionStore.count,
                        onClose: {
                            withAnimation {
                                viewModel.isDrawerOpen = false
                            }
                        },
                        onClearExclusions: {
                            viewModel.clearAllExclusions()
                        },
                        onFeedback: {
                            viewModel.alertMessage = "Thank you for using Remo!"
                            viewModel.showAlert = true
                        },
                        onRate: {
                            viewModel.alertMessage = "Please rate us 5 stars on the App Store!"
                            viewModel.showAlert = true
                        },
                        onAbout: {
                            viewModel.alertMessage = "Remo Duplicate Photos Remover v1.0.0"
                            viewModel.showAlert = true
                        }
                    )
                    .transition(.move(edge: .leading))
                    .zIndex(10)
                }
            }
            .animation(.easeInOut(duration: 0.25), value: viewModel.isDrawerOpen)
        }
    }
}
