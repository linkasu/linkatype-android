import Foundation
import Combine
import Shared

class FirebaseAuthManager: ObservableObject {
    static let shared = FirebaseAuthManager()

    @Published var isAuthenticated = false
    @Published var currentUserId: String?
    @Published var currentUserEmail: String?
    @Published var mode: String?
    @Published var pendingMigrationDecision = false
    @Published var migrationError: String?

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
            self.mode = "online"
            self.isAuthenticated = true
            self.currentUserId = response.user.id
            self.currentUserEmail = response.user.email
        }
    }

    private func applyOfflineAuth() {
        let deviceId = sdk.ensureDeviceId()
        DispatchQueue.main.async {
            self.mode = "offline"
            self.isAuthenticated = true
            self.currentUserId = deviceId
            self.currentUserEmail = Self.offlineEmail(deviceId)
        }
    }

    private func applyCachedAuthOnline() {
        guard let userId = userDefaults.string(forKey: cachedUserIdKey) else {
            clearAuth(resetMode: false)
            return
        }
        let email = userDefaults.string(forKey: cachedUserEmailKey)
        DispatchQueue.main.async {
            self.mode = "online"
            self.isAuthenticated = true
            self.currentUserId = userId
            self.currentUserEmail = email
        }
    }

    private func clearAuth(resetMode: Bool) {
        clearCachedAuth()
        if resetMode {
            sdk.clearMode()
        }
        DispatchQueue.main.async {
            self.mode = resetMode ? nil : self.mode
            self.pendingMigrationDecision = false
            self.migrationError = nil
            self.isAuthenticated = false
            self.currentUserId = nil
            self.currentUserEmail = nil
        }
    }

    private func refreshIfPossible() async {
        let currentMode = sdk.modeName()
        if currentMode == "offline" {
            applyOfflineAuth()
            return
        }

        if currentMode == nil {
            clearAuth(resetMode: true)
            return
        }

        DispatchQueue.main.async {
            self.mode = "online"
        }

        let refreshToken = sdk.tokenStorage.getRefreshToken()
        if refreshToken == nil || refreshToken?.isEmpty == true {
            clearAuth(resetMode: false)
            return
        }

        if !NetworkMonitor.shared.isConnected {
            applyCachedAuthOnline()
            return
        }

        do {
            let response = try await sdk.authRepository.refresh()
            applyAuth(response)
        } catch {
            clearAuth(resetMode: true)
        }
    }

    func signIn(email: String, password: String) async throws {
        let previousMode = sdk.modeName()
        sdk.setOnlineMode()
        _ = sdk.ensureDeviceId()

        let response = try await sdk.authRepository.login(email: email, password: password)
        applyAuth(response)

        if previousMode == "offline" {
            let hasLocalData = (try? await sdk.localDataMigrationService.hasLocalDataForMigration()) ?? false
            if hasLocalData {
                DispatchQueue.main.async {
                    self.pendingMigrationDecision = true
                }
            }
        }
    }

    func signUp(email: String, password: String) async throws {
        let previousMode = sdk.modeName()
        sdk.setOnlineMode()
        _ = sdk.ensureDeviceId()

        let response = try await sdk.authRepository.register(email: email, password: password)
        applyAuth(response)

        if previousMode == "offline" {
            let hasLocalData = (try? await sdk.localDataMigrationService.hasLocalDataForMigration()) ?? false
            if hasLocalData {
                DispatchQueue.main.async {
                    self.pendingMigrationDecision = true
                }
            }
        }
    }

    func enterOfflineMode() {
        sdk.tokenStorage.clear()
        sdk.setOfflineMode()
        _ = sdk.ensureDeviceId()
        applyOfflineAuth()
    }

    func prepareOnlineMode() {
        sdk.setOnlineMode()
        _ = sdk.ensureDeviceId()
        DispatchQueue.main.async {
            self.mode = "online"
            self.isAuthenticated = false
            self.pendingMigrationDecision = false
            self.migrationError = nil
            self.currentUserId = nil
            self.currentUserEmail = nil
        }
    }

    func resolveMigration(syncLocalData: Bool) async {
        do {
            if syncLocalData {
                try await sdk.localDataMigrationService.syncLocalDataToRemote()
            } else {
                try await sdk.localDataMigrationService.replaceLocalDataWithRemote()
            }
            DispatchQueue.main.async {
                self.pendingMigrationDecision = false
                self.migrationError = nil
            }
        } catch {
            DispatchQueue.main.async {
                self.pendingMigrationDecision = false
                self.migrationError = NSLocalizedString("auth_migration_failed", comment: "")
            }
        }
    }

    func signOut() throws {
        Task {
            try? await sdk.authRepository.logout()
            clearAuth(resetMode: true)
        }
    }

    func resetPassword(email: String) async throws {
        try await sdk.authRepository.resetPassword(email: email)
    }

    func getUserId() -> String? {
        return currentUserId
    }

    func deleteAccount() async throws {
        guard mode == "online" else {
            throw NSError(
                domain: "linka",
                code: 1,
                userInfo: [NSLocalizedDescriptionKey: NSLocalizedString("auth_online_required_title", comment: "")]
            )
        }
        try await sdk.accountRepository.deleteAccount(deleteFirebase: true)
        try? await sdk.authRepository.logout()
        clearAuth(resetMode: true)
    }

    private func cacheAuth(userId: String, email: String?) {
        userDefaults.set(userId, forKey: cachedUserIdKey)
        userDefaults.set(email, forKey: cachedUserEmailKey)
    }

    private func clearCachedAuth() {
        userDefaults.removeObject(forKey: cachedUserIdKey)
        userDefaults.removeObject(forKey: cachedUserEmailKey)
    }

    private static func offlineEmail(_ deviceId: String) -> String {
        return "\(deviceId)@local.device"
    }
}
