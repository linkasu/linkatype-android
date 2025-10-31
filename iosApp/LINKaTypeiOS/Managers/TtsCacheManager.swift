import Foundation
import CryptoKit

class TtsCacheManager {
    private let cacheDirectory: URL
    private let userDefaults = UserDefaults.standard
    
    private let cacheEnabledKey = "cache_enabled"
    private let cacheSizeLimitMbKey = "cache_size_limit_mb"
    
    init() {
        let documentsPath = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        cacheDirectory = documentsPath.appendingPathComponent("tts_cache", isDirectory: true)
        
        try? FileManager.default.createDirectory(at: cacheDirectory, withIntermediateDirectories: true)
    }
    
    func getCacheEnabled() -> Bool {
        return userDefaults.bool(forKey: cacheEnabledKey)
    }
    
    func setCacheEnabled(_ enabled: Bool) {
        userDefaults.set(enabled, forKey: cacheEnabledKey)
    }
    
    func getCacheSizeLimitMb() -> Double {
        let limit = userDefaults.double(forKey: cacheSizeLimitMbKey)
        return limit > 0 ? limit : 1000.0
    }
    
    func setCacheSizeLimitMb(_ limit: Double) {
        userDefaults.set(limit, forKey: cacheSizeLimitMbKey)
    }
    
    func generateCacheKey(text: String, voice: String) -> String {
        let input = "\(text)_\(voice)"
        let hash = SHA256.hash(data: Data(input.utf8))
        return hash.compactMap { String(format: "%02x", $0) }.joined()
    }
    
    func isCached(_ key: String) -> Bool {
        let fileURL = cacheDirectory.appendingPathComponent("\(key).mp3")
        return FileManager.default.fileExists(atPath: fileURL.path)
    }
    
    func getCachedFile(_ key: String) -> URL? {
        let fileURL = cacheDirectory.appendingPathComponent("\(key).mp3")
        guard FileManager.default.fileExists(atPath: fileURL.path) else {
            return nil
        }
        return fileURL
    }
    
    func saveToCache(_ key: String, data: Data) -> URL? {
        let fileURL = cacheDirectory.appendingPathComponent("\(key).mp3")
        do {
            try data.write(to: fileURL)
            cleanupCacheIfNeeded()
            return fileURL
        } catch {
            print("Error saving to cache: \(error)")
            return nil
        }
    }
    
    func clearCache() async {
        do {
            let files = try FileManager.default.contentsOfDirectory(at: cacheDirectory, includingPropertiesForKeys: nil)
            for file in files {
                try FileManager.default.removeItem(at: file)
            }
        } catch {
            print("Error clearing cache: \(error)")
        }
    }
    
    func getCacheInfo() async -> (fileCount: Int, sizeMb: Double, sizeLimitMb: Double) {
        let files = (try? FileManager.default.contentsOfDirectory(at: cacheDirectory, includingPropertiesForKeys: [.fileSizeKey])) ?? []
        var totalSize: Int64 = 0
        
        for file in files {
            if let size = try? file.resourceValues(forKeys: [.fileSizeKey]).fileSize {
                totalSize += Int64(size)
            }
        }
        
        let sizeMb = Double(totalSize) / 1_048_576.0
        return (files.count, sizeMb, getCacheSizeLimitMb())
    }
    
    private func cleanupCacheIfNeeded() {
        Task {
            let info = await getCacheInfo()
            if info.sizeMb > info.sizeLimitMb {
                await cleanupOldFiles()
            }
        }
    }
    
    private func cleanupOldFiles() async {
        do {
            let files = try FileManager.default.contentsOfDirectory(
                at: cacheDirectory,
                includingPropertiesForKeys: [.contentModificationDateKey, .fileSizeKey]
            )
            
            let sortedFiles = files.sorted { file1, file2 in
                let date1 = (try? file1.resourceValues(forKeys: [.contentModificationDateKey]).contentModificationDate) ?? Date.distantPast
                let date2 = (try? file2.resourceValues(forKeys: [.contentModificationDateKey]).contentModificationDate) ?? Date.distantPast
                return date1 < date2
            }
            
            var currentSize: Int64 = 0
            for file in sortedFiles.reversed() {
                if let size = try? file.resourceValues(forKeys: [.fileSizeKey]).fileSize {
                    currentSize += Int64(size)
                }
            }
            
            let limitBytes = Int64(getCacheSizeLimitMb() * 1_048_576.0)
            
            for file in sortedFiles {
                if currentSize <= limitBytes {
                    break
                }
                
                if let size = try? file.resourceValues(forKeys: [.fileSizeKey]).fileSize {
                    try? FileManager.default.removeItem(at: file)
                    currentSize -= Int64(size)
                }
            }
        } catch {
            print("Error cleaning up cache: \(error)")
        }
    }
}

