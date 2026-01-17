import Foundation
import Combine
import Shared

class FirebaseAuthManager: ObservableObject {
    static let shared = FirebaseAuthManager()

    @Published var isAuthenticated = false
    @Published var currentUserId: String?
    @Published var currentUserEmail: String?

    private let sdk = SharedSdkProvider.shared.sdk

    init() {
        Task {
            await refreshIfPossible()
        }
    }

    private func applyAuth(_ response: AuthResponse) {
        DispatchQueue.main.async {
            self.isAuthenticated = true
            self.currentUserId = response.user.id
            self.currentUserEmail = response.user.email
        }
    }

    private func clearAuth() {
        DispatchQueue.main.async {
            self.isAuthenticated = false
            self.currentUserId = nil
            self.currentUserEmail = nil
        }
    }

    private func refreshIfPossible() async {
        let refreshToken = sdk.tokenStorage.getRefreshToken()
        if refreshToken == nil || refreshToken?.isEmpty == true {
            clearAuth()
            return
        }
        do {
            let response = try await sdk.authRepository.refresh()
            applyAuth(response)
        } catch {
            clearAuth()
        }
    }

    func signIn(email: String, password: String) async throws {
        let response = try await sdk.authRepository.login(email: email, password: password)
        applyAuth(response)
    }

    func signUp(email: String, password: String) async throws {
        let response = try await sdk.authRepository.register(email: email, password: password)
        applyAuth(response)
    }

    func signOut() throws {
        Task {
            try? await sdk.authRepository.logout()
            clearAuth()
        }
    }

    func resetPassword(email: String) async throws {
        try await sdk.authRepository.resetPassword(email: email)
    }

    func getUserId() -> String? {
        return currentUserId
    }

    func deleteAccount() async throws {
        try await sdk.accountRepository.deleteAccount(deleteFirebase: true)
        try? await sdk.authRepository.logout()
        clearAuth()
    }
}
