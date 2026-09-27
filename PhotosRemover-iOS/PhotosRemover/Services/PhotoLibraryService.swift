import Foundation
import Photos
import UIKit
import AVFoundation

public final class PhotoLibraryService {
    public static let shared = PhotoLibraryService()

    private let imageManager = PHImageManager.default()
    private let resourceManager = PHAssetResourceManager.default()

    private init() {}

    public func authorizationStatus() -> PHAuthorizationStatus {
        return PHPhotoLibrary.authorizationStatus(for: .readWrite)
    }

    public func requestAuthorization() async -> PHAuthorizationStatus {
        return await PHPhotoLibrary.requestAuthorization(for: .readWrite)
    }

    public func fetchAllPhotos(excludedIds: Set<String> = []) async -> [PhotoItem] {
        return await withCheckedContinuation { continuation in
            DispatchQueue.global(qos: .userInitiated).async {
                let fetchOptions = PHFetchOptions()
                fetchOptions.sortDescriptors = [NSSortDescriptor(key: "creationDate", ascending: false)]
                fetchOptions.predicate = NSPredicate(format: "mediaType == %d", PHAssetMediaType.image.rawValue)

                let assets = PHAsset.fetchAssets(with: fetchOptions)
                var items: [PhotoItem] = []
                items.reserveCapacity(assets.count)

                assets.enumerateObjects { asset, _, _ in
                    if excludedIds.contains(asset.localIdentifier) { return }

                    let resources = PHAssetResource.assetResources(for: asset)
                    let sizeBytes = Self.resolveFileSize(for: asset, resources: resources, resourceManager: self.resourceManager)

                    let item = PhotoItem(
                        id: asset.localIdentifier,
                        asset: asset,
                        sizeBytes: sizeBytes,
                        dateTaken: asset.creationDate ?? Date(),
                        pixelWidth: asset.pixelWidth,
                        pixelHeight: asset.pixelHeight,
                        isFavorite: asset.isFavorite
                    )
                    items.append(item)
                }

                continuation.resume(returning: items)
            }
        }
    }

    public func fetchAllVideos(excludedIds: Set<String> = []) async -> [PhotoItem] {
        return await withCheckedContinuation { continuation in
            DispatchQueue.global(qos: .userInitiated).async {
                let fetchOptions = PHFetchOptions()
                fetchOptions.sortDescriptors = [NSSortDescriptor(key: "creationDate", ascending: false)]
                fetchOptions.predicate = NSPredicate(format: "mediaType == %d", PHAssetMediaType.video.rawValue)

                let assets = PHAsset.fetchAssets(with: fetchOptions)
                var items: [PhotoItem] = []
                items.reserveCapacity(assets.count)

                assets.enumerateObjects { asset, _, _ in
                    if excludedIds.contains(asset.localIdentifier) { return }

                    let resources = PHAssetResource.assetResources(for: asset)
                    let sizeBytes = Self.resolveFileSize(for: asset, resources: resources, resourceManager: self.resourceManager)

                    let item = PhotoItem(
                        id: asset.localIdentifier,
                        asset: asset,
                        sizeBytes: sizeBytes,
                        dateTaken: asset.creationDate ?? Date(),
                        pixelWidth: asset.pixelWidth,
                        pixelHeight: asset.pixelHeight,
                        isFavorite: asset.isFavorite,
                        isVideo: true,
                        durationSeconds: asset.duration
                    )
                    items.append(item)
                }

                continuation.resume(returning: items)
            }
        }
    }

    public func requestVideoFrame(for asset: PHAsset, at timestamp: TimeInterval, targetSize: CGSize = CGSize(width: 64, height: 64)) async -> UIImage? {
        return await withCheckedContinuation { continuation in
            let options = PHVideoRequestOptions()
            options.isNetworkAccessAllowed = true
            options.deliveryMode = .fastFormat

            self.imageManager.requestAVAsset(forVideo: asset, options: options) { avAsset, _, _ in
                guard let avAsset = avAsset else {
                    continuation.resume(returning: nil)
                    return
                }
                let generator = AVAssetImageGenerator(asset: avAsset)
                generator.appliesPreferredTrackTransform = true
                generator.maximumSize = targetSize
                let time = CMTime(seconds: timestamp, preferredTimescale: 600)
                do {
                    let cgImage = try generator.copyCGImage(at: time, actualTime: nil)
                    continuation.resume(returning: UIImage(cgImage: cgImage))
                } catch {
                    continuation.resume(returning: nil)
                }
            }
        }
    }

    public static func resolveFileSize(for asset: PHAsset, resources: [PHAssetResource], resourceManager: PHAssetResourceManager) -> Int64 {
        if let primaryResource = resources.first(where: { $0.type == .photo }) ?? resources.first {
            if let sizeVal = primaryResource.value(forKey: "fileSize") as? Int64, sizeVal > 0 {
                return sizeVal
            }
            // Fallback: byte counting via requestData if KVC fileSize is not available
            var countedBytes: Int64 = 0
            let semaphore = DispatchSemaphore(value: 0)
            let options = PHAssetResourceRequestOptions()
            options.isNetworkAccessAllowed = false
            resourceManager.requestData(for: primaryResource, options: options, dataReceivedHandler: { chunk in
                countedBytes += Int64(chunk.count)
            }, completionHandler: { _ in
                semaphore.signal()
            })
            _ = semaphore.wait(timeout: .now() + 0.5)
            if countedBytes > 0 {
                return countedBytes
            }
        }
        return Int64(asset.pixelWidth * asset.pixelHeight * 3 / 8)
    }

    public func requestThumbnail(for asset: PHAsset, targetSize: CGSize = CGSize(width: 64, height: 64)) async -> UIImage? {
        return await withCheckedContinuation { continuation in
            let options = PHImageRequestOptions()
            options.deliveryMode = .fastFormat
            options.isNetworkAccessAllowed = true
            options.isSynchronous = false

            imageManager.requestImage(
                for: asset,
                targetSize: targetSize,
                contentMode: .aspectFill,
                options: options
            ) { image, _ in
                continuation.resume(returning: image)
            }
        }
    }

    public func requestHighResImage(for asset: PHAsset) async -> UIImage? {
        return await withCheckedContinuation { continuation in
            let options = PHImageRequestOptions()
            options.deliveryMode = .highQualityFormat
            options.isNetworkAccessAllowed = true
            options.isSynchronous = false

            imageManager.requestImage(
                for: asset,
                targetSize: PHImageManagerMaximumSize,
                contentMode: .aspectFit,
                options: options
            ) { image, _ in
                continuation.resume(returning: image)
            }
        }
    }

    public func requestAssetData(for asset: PHAsset) async -> Data? {
        return await withCheckedContinuation { continuation in
            let resources = PHAssetResource.assetResources(for: asset)
            guard let resource = resources.first(where: { $0.type == .photo }) ?? resources.first else {
                continuation.resume(returning: nil)
                return
            }

            let data = NSMutableData()
            let options = PHAssetResourceRequestOptions()
            options.isNetworkAccessAllowed = false // Skip network downloads for exact duplicate hashing

            resourceManager.requestData(for: resource, options: options, dataReceivedHandler: { chunk in
                data.append(chunk)
            }, completionHandler: { error in
                if error == nil && data.length > 0 {
                    continuation.resume(returning: data as Data)
                } else {
                    continuation.resume(returning: nil)
                }
            })
        }
    }

    public func deleteAssets(_ assets: [PHAsset]) async throws {
        try await PHPhotoLibrary.shared().performChanges {
            PHAssetChangeRequest.deleteAssets(assets as NSArray)
        }
    }
}
