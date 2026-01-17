import Foundation
import Combine
import Shared

class FirebaseCategoryManager: ObservableObject {
    @Published var categories: [String: String] = [:]

    private let sdk = SharedSdkProvider.shared.sdk

    func startListening(userId: String) {
        Task {
            await loadCategories()
        }
    }

    func refresh() {
        Task {
            await loadCategories()
        }
    }

    func stopListening() {
        // No-op for backend-driven updates.
    }

    func create(label: String, userId: String) async throws {
        _ = try await sdk.categoriesRepository.create(label: label, created: nil, aiUse: nil)
        await loadCategories()
    }

    func edit(id: String, label: String, userId: String) async throws {
        _ = try await sdk.categoriesRepository.update(id: id, label: label, aiUse: nil)
        await loadCategories()
    }

    func remove(id: String, userId: String) async throws {
        try await sdk.categoriesRepository.delete(id: id)
        await loadCategories()
    }

    private func loadCategories() async {
        do {
            let list = try await sdk.categoriesRepository.list()
            let mapped = Dictionary(uniqueKeysWithValues: list.map { ($0.id, $0.label) })
            await MainActor.run {
                self.categories = mapped
            }
        } catch {
            // Ignore errors; keep existing cache.
        }
    }
}
