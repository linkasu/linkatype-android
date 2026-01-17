import Foundation
import Combine
import Shared

class FirebaseStatementManager: ObservableObject {
    @Published var statements: [String: String] = [:]

    private let sdk = SharedSdkProvider.shared.sdk
    private var currentCategoryId: String?

    func startListening(userId: String, categoryId: String) {
        currentCategoryId = categoryId
        Task {
            await loadStatements(categoryId: categoryId)
        }
    }

    func stopListening() {
        currentCategoryId = nil
    }

    func refresh() {
        guard let categoryId = currentCategoryId else { return }
        Task {
            await loadStatements(categoryId: categoryId)
        }
    }

    func create(label: String, userId: String, categoryId: String) async throws {
        _ = try await sdk.statementsRepository.create(categoryId: categoryId, text: label, created: nil)
        await loadStatements(categoryId: categoryId)
    }

    func edit(id: String, label: String, userId: String, categoryId: String) async throws {
        _ = try await sdk.statementsRepository.update(id: id, text: label)
        await loadStatements(categoryId: categoryId)
    }

    func remove(id: String, userId: String, categoryId: String) async throws {
        try await sdk.statementsRepository.delete(id: id)
        await loadStatements(categoryId: categoryId)
    }

    private func loadStatements(categoryId: String) async {
        do {
            let list = try await sdk.statementsRepository.listByCategory(categoryId: categoryId)
            let mapped = Dictionary(uniqueKeysWithValues: list.map { ($0.id, $0.text) })
            await MainActor.run {
                self.statements = mapped
            }
        } catch {
            // Ignore errors; keep existing cache.
        }
    }
}
