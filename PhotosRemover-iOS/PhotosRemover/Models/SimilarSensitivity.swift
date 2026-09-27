import Foundation

public enum SimilarSensitivity: String, CaseIterable, Identifiable {
    case strict = "Strict"
    case normal = "Normal"
    case loose = "Loose"

    public var id: String { rawValue }

    public var maxHammingDistance: Int {
        switch self {
        case .strict: return 3
        case .normal: return 5
        case .loose: return 10
        }
    }

    public var displayLabel: String {
        switch self {
        case .strict: return "Strict (≤ 3 bits)"
        case .normal: return "Normal (≤ 5 bits)"
        case .loose: return "Loose (≤ 10 bits)"
        }
    }
}
