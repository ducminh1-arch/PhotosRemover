import Foundation

public final class CleanedStatsStore: ObservableObject {
    public static let shared = CleanedStatsStore()

    private let keyCleanedCount = "cleanpix_cleaned_count"
    private let keyCleanedBytes = "cleanpix_cleaned_bytes"
    private let keySensitivity = "cleanpix_similar_sensitivity"

    @Published public private(set) var cleanedCount: Int = 0
    @Published public private(set) var cleanedBytes: Int64 = 0
    @Published public var sensitivity: SimilarSensitivity = .normal {
        didSet {
            UserDefaults.standard.set(sensitivity.rawValue, forKey: keySensitivity)
        }
    }

    private init() {
        self.cleanedCount = UserDefaults.standard.integer(forKey: keyCleanedCount)
        self.cleanedBytes = Int64(UserDefaults.standard.double(forKey: keyCleanedBytes))
        if let saved = UserDefaults.standard.string(forKey: keySensitivity),
           let sens = SimilarSensitivity(rawValue: saved) {
            self.sensitivity = sens
        } else {
            self.sensitivity = .normal
        }
    }

    public func recordCleaned(count: Int, bytes: Int64) {
        cleanedCount += count
        cleanedBytes += bytes
        UserDefaults.standard.set(cleanedCount, forKey: keyCleanedCount)
        UserDefaults.standard.set(Double(cleanedBytes), forKey: keyCleanedBytes)
    }
}
