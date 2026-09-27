import Foundation

public final class ExclusionStore: ObservableObject {
    public static let shared = ExclusionStore()
    private let key = "remo_excluded_asset_ids"

    @Published public private(set) var excludedIds: Set<String> = []

    private init() {
        excludedIds = Set(UserDefaults.standard.stringArray(forKey: key) ?? [])
    }

    public func isExcluded(_ id: String) -> Bool {
        return excludedIds.contains(id)
    }

    public func toggleExclusion(_ id: String) -> Bool {
        let nowExcluded: Bool
        if excludedIds.contains(id) {
            excludedIds.remove(id)
            nowExcluded = false
        } else {
            excludedIds.insert(id)
            nowExcluded = true
        }
        UserDefaults.standard.set(Array(excludedIds), forKey: key)
        return nowExcluded
    }

    public func addExclusion(_ id: String) {
        if !excludedIds.contains(id) {
            excludedIds.insert(id)
            UserDefaults.standard.set(Array(excludedIds), forKey: key)
        }
    }

    public func removeExclusion(_ id: String) {
        if excludedIds.contains(id) {
            excludedIds.remove(id)
            UserDefaults.standard.set(Array(excludedIds), forKey: key)
        }
    }

    public func clearAllExclusions() {
        excludedIds.removeAll()
        UserDefaults.standard.removeObject(forKey: key)
    }

    public var count: Int {
        return excludedIds.count
    }
}
