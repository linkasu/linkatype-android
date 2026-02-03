import Foundation
import Combine
import Shared

class FirebaseAuthManager: ObservableObject {
    static let shared = FirebaseAuthManager()

    @Published var isAuthenticated = false
    @Published var currentUserId: String?
    @Published var currentUserEmail: String?

    private let sdk = SharedSdkProvider.shared.sdk
    private let userDefaults = UserDefaults.standard
    private var cancellables = Set<AnyCancellable>()

    private let cachedUserIdKey = "auth_user_id"
    private let cachedUserEmailKey = "auth_user_email"

    init() {
        NetworkMonitor.shared.$isConnected
            .removeDuplicates()
            .sink { [weak self] isConnected in
                guard isConnected else { return }
                Task { await self?.refreshIfPossible() }
            }
            .store(in: &cancellables)

        Task {
            await refreshIfPossible()
        }
    }

    private func applyAuth(_ response: AuthResponse) {
        cacheAuth(userId: response.user.id, email: response.user.email)
        DispatchQueue.main.async {
            self.isAuthenticated = true
            self.currentUserId = response.user.id
            self.currentUserEmail = response.user.email
        }
    }

    private func clearAuth() {
        clearCachedAuth()
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
        if !NetworkMonitor.shared.isConnected {
            applyCachedAuth()
            return
        }
        do {
            let response = try await sdk.authRepository.refresh()
            applyAuth(response)
        } catch {
            if NetworkMonitor.shared.isConnected {
                clearAuth()
            } else {
                applyCachedAuth()
            }
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

    private func cacheAuth(userId: String, email: String?) {
        userDefaults.set(userId, forKey: cachedUserIdKey)
        userDefaults.set(email, forKey: cachedUserEmailKey)
    }

    private func applyCachedAuth() {
        guard let userId = userDefaults.string(forKey: cachedUserIdKey) else {
            clearAuth()
            return
        }
        let email = userDefaults.string(forKey: cachedUserEmailKey)
        DispatchQueue.main.async {
            self.isAuthenticated = true
            self.currentUserId = userId
            self.currentUserEmail = email
        }
    }

    private func clearCachedAuth() {
        userDefaults.removeObject(forKey: cachedUserIdKey)
        userDefaults.removeObject(forKey: cachedUserEmailKey)
    }
}
