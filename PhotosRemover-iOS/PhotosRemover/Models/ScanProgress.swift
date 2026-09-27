import Foundation

public enum ScanPhase: Equatable {
    case idle
    case indexing
    case exactChecking
    case similarChecking
    case videoChecking
    case completed
    case cancelled
}

public struct ScanProgress: Equatable {
    public var phase: ScanPhase
    public var current: Int
    public var total: Int
    public var exactSetsFound: Int
    public var similarSetsFound: Int
    public var videoSetsFound: Int

    public init(
        phase: ScanPhase = .idle,
        current: Int = 0,
        total: Int = 0,
        exactSetsFound: Int = 0,
        similarSetsFound: Int = 0,
        videoSetsFound: Int = 0
    ) {
        self.phase = phase
        self.current = current
        self.total = total
        self.exactSetsFound = exactSetsFound
        self.similarSetsFound = similarSetsFound
        self.videoSetsFound = videoSetsFound
    }

    public var progressFraction: Double {
        guard total > 0 else { return 0.0 }
        return Double(current) / Double(total)
    }
}
