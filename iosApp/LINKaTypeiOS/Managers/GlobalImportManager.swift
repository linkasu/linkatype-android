import Foundation
import Shared

final class GlobalImportManager: ObservableObject {
    @Published var categories: [GlobalCategory] = []
    @Published var isLoading = false
    @Published var importingId: String?

    private let sdk = SharedSdkProvider.shared.sdk

    @MainActor
    func loadCategories() async {
        isLoading = true
        defer { isLoading = false }
        do {
            let list = try await sdk.globalRepository.listCategories(includeStatements: true)
            categories = list
        } catch {
            categories = []
        }
    }

    func importCategory(id: String) async -> String? {
        importingId = id
        defer { importingId = nil }
        do {
            let status = try await sdk.globalRepository.importCategory(categoryId: id, force: false)
            return status.status
        } catch {
            return nil
        }
    }
}
