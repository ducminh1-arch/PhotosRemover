import SwiftUI

public struct ScanningView: View {
    @ObservedObject public var viewModel: MainViewModel

    public init(viewModel: MainViewModel) {
        self.viewModel = viewModel
    }

    public var body: some View {
        VStack(spacing: 20) {
            Spacer()

            // Laser Phone Scanner
            LaserScannerView()

            Spacer().frame(height: 20)

            Text("Scanning photos, please wait…")
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(Color(red: 0.12, green: 0.16, blue: 0.23))

            // Dynamic Counter
            Text(viewModel.scanProgress.total > 0 ? "\(viewModel.scanProgress.current) / \(viewModel.scanProgress.total)" : "\(viewModel.scanProgress.current)")
                .font(.system(size: 34, weight: .black))
                .foregroundColor(Color(red: 0.12, green: 0.16, blue: 0.23))

            // Phase Text
            Text(phaseDescription)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(Color(red: 85.0/255.0, green: 66.0/255.0, blue: 0.0))

            // Progress Bar
            ProgressView(value: viewModel.scanProgress.progressFraction, total: 1.0)
                .accentColor(Color(red: 0.12, green: 0.16, blue: 0.23))
                .frame(width: 220)
                .scaleEffect(x: 1, y: 1.8, anchor: .center)

            Text("Scanning time depends on number of files.")
                .font(.system(size: 12, weight: .medium))
                .foregroundColor(Color(red: 85.0/255.0, green: 66.0/255.0, blue: 0.0))

            Spacer()

            // Cancel Scan Button
            Button(action: {
                viewModel.cancelScan()
            }) {
                Text("Cancel Scan")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(Color(red: 0.12, green: 0.16, blue: 0.23))
                    .padding(.horizontal, 28)
                    .padding(.vertical, 10)
                    .background(Color.white.opacity(0.35))
                    .cornerRadius(12)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color(red: 0.12, green: 0.16, blue: 0.23).opacity(0.3), lineWidth: 1)
                    )
            }
            .padding(.bottom, 24)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(red: 1.0, green: 200.0/255.0, blue: 0.0))
    }

    private var phaseDescription: String {
        switch viewModel.scanProgress.phase {
        case .indexing: return "Analyzing photo & video metadata…"
        case .exactChecking: return "Checking exact duplicates (SHA-256)…"
        case .similarChecking: return "Comparing similar photos (dHash)…"
        case .videoChecking: return "Analyzing duplicate videos…"
        case .completed: return "Completed!"
        case .cancelled: return "Cancelled"
        case .idle: return "Preparing…"
        }
    }
}
