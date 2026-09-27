import SwiftUI

public struct BottomActionBarView: View {
    public let selectedCount: Int
    public let selectedSizeBytes: Int64
    public let onDeleteTap: () -> Void

    public init(
        selectedCount: Int,
        selectedSizeBytes: Int64,
        onDeleteTap: @escaping () -> Void
    ) {
        self.selectedCount = selectedCount
        self.selectedSizeBytes = selectedSizeBytes
        self.onDeleteTap = onDeleteTap
    }

    public var body: some View {
        VStack(spacing: 0) {
            Divider()

            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(selectedCount > 0 ? "\(selectedCount) photos selected" : "No photos selected")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(selectedCount > 0 ? .primary : .secondary)

                    Text(selectedCount > 0 ? "Free up \(PhotoItem.formatByteSize(selectedSizeBytes))" : "Tap photos to select")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                }

                Spacer()

                Button(action: onDeleteTap) {
                    HStack(spacing: 6) {
                        Image(systemName: "trash.fill")
                            .font(.system(size: 14, weight: .bold))
                        Text("Delete")
                            .font(.system(size: 15, weight: .bold))
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 12)
                    .background(selectedCount > 0 ? Color.red : Color(UIColor.systemGray4))
                    .cornerRadius(12)
                }
                .disabled(selectedCount == 0)
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)
            .padding(.bottom, 8)
            .background(Color(UIColor.systemBackground))
        }
    }
}
