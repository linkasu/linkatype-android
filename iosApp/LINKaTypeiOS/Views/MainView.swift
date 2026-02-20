import SwiftUI

struct MainView: View {
    @EnvironmentObject var authManager: FirebaseAuthManager
    private let sdk = SharedSdkProvider.shared.sdk
    @StateObject private var ttsManager = TtsManager.shared
    @StateObject private var networkMonitor = NetworkMonitor.shared
    @State private var showSettings = false
    @State private var showDialog = false
    @State private var snackbarMessage: String?
    @State private var showSnackbar = false
    @State private var syncTimer = Timer.publish(every: 60, on: .main, in: .common).autoconnect()
    private let isUiTest = ProcessInfo.processInfo.arguments.contains("ui_test")
    private var isOfflineMode: Bool { authManager.mode == "offline" }
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                InputGroupView(ttsManager: ttsManager)
                    .padding()
                
                Divider()
                
                BankGroupView(ttsManager: ttsManager)
            }
            .navigationTitle(NSLocalizedString("app_name", comment: ""))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        Button(action: {
                            NotificationCenter.default.post(name: .clearInput, object: nil)
                        }) {
                            Label(NSLocalizedString("clear", comment: ""), systemImage: "xmark.circle")
                                .accessibilityIdentifier("menu_clear")
                        }
                        .accessibilityIdentifier("menu_clear")
                        
                        Button(action: { showSettings = true }) {
                            Label(NSLocalizedString("settings", comment: ""), systemImage: "gearshape")
                                .accessibilityIdentifier("menu_settings")
                        }
                        .accessibilityIdentifier("menu_settings")

                        Button(action: { showDialog = true }) {
                            Label(NSLocalizedString("dialog_title", comment: ""), systemImage: "message")
                                .accessibilityIdentifier("menu_dialog")
                        }
                        .accessibilityIdentifier("menu_dialog")
                        
                        if isOfflineMode {
                            Button(action: openOnlineAuth) {
                                Label(NSLocalizedString("auth_online_required_action", comment: ""), systemImage: "person.crop.circle.badge.plus")
                                    .accessibilityIdentifier("menu_login_online")
                            }
                            .accessibilityIdentifier("menu_login_online")
                        } else {
                            Button(role: .destructive, action: logout) {
                                Label(NSLocalizedString("logout", comment: ""), systemImage: "rectangle.portrait.and.arrow.right")
                                    .accessibilityIdentifier("menu_logout")
                            }
                            .accessibilityIdentifier("menu_logout")
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                    .accessibilityIdentifier("main_menu")
                }
            }
            .sheet(isPresented: $showSettings) {
                NavigationStack {
                    SettingsView(ttsManager: ttsManager)
                }
                .accessibilityIdentifier("settings_view")
            }
            .sheet(isPresented: $showDialog) {
                NavigationStack {
                    DialogView()
                }
                .accessibilityIdentifier("dialog_view")
            }
            .overlay(alignment: .bottom) {
                if showSnackbar, let message = snackbarMessage {
                    SnackbarView(message: message)
                        .padding()
                        .transition(.move(edge: .bottom))
                }
            }
            .overlay(alignment: .topLeading) {
                if isUiTest {
                    HStack(spacing: 8) {
                        Button("UI Settings") { showSettings = true }
                            .accessibilityIdentifier("ui_test_settings")
                        Button("UI Dialog") { showDialog = true }
                            .accessibilityIdentifier("ui_test_dialog")
                    }
                    .font(.caption2)
                    .padding(4)
                    .opacity(0.02)
                }
            }
            .task {
                guard !isOfflineMode else { return }
                _ = try? await sdk.offlineQueueProcessor.flush()
            }
            .task {
                guard !isOfflineMode else { return }
                await startRealtimeSync()
            }
            .onReceive(syncTimer) { _ in
                guard !isOfflineMode else { return }
                Task {
                    _ = try? await sdk.offlineQueueProcessor.flush()
                }
            }
            .onReceive(networkMonitor.$isConnected) { isConnected in
                if isConnected && !isOfflineMode {
                    Task {
                        _ = try? await sdk.offlineQueueProcessor.flush()
                    }
                }
            }
            .onReceive(ttsManager.eventPublisher) { event in
                handleTtsEvent(event)
            }
        }
    }
    
    private func logout() {
        try? authManager.signOut()
    }

    private func openOnlineAuth() {
        authManager.prepareOnlineMode()
    }
    
    private func handleTtsEvent(_ event: TtsEvent) {
        switch event {
        case .downloadCompleted(let path):
            if let path = path {
                let format = NSLocalizedString("tts_download_saved", comment: "")
                showSnackbarMessage(String(format: format, path))
            } else {
                showSnackbarMessage(NSLocalizedString("tts_download_finished", comment: ""))
            }
        case .error(let message):
            let format = NSLocalizedString("tts_status_error", comment: "")
            showSnackbarMessage(String(format: format, message))
        case .temporarilyUnavailable(let message):
            showSnackbarMessage(message)
        default:
            break
        }
    }
    
    private func showSnackbarMessage(_ message: String) {
        snackbarMessage = message
        withAnimation {
            showSnackbar = true
        }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
            withAnimation {
                showSnackbar = false
            }
        }
    }

    private func startRealtimeSync() async {
        while !Task.isCancelled {
            if isOfflineMode {
                return
            }
            do {
                let response = try await sdk.changesSyncer.pollOnce(limit: 100, timeoutSeconds: 25)
                if !response.changes.isEmpty {
                    FirebaseAnalyticsManager.shared.logRealtimeSyncEvent(changesCount: Int(response.changes.count))
                    NotificationCenter.default.post(name: .realtimeDidUpdate, object: nil)
                }
            } catch {
                FirebaseAnalyticsManager.shared.logRealtimeSyncError(message: error.localizedDescription)
                try? await Task.sleep(nanoseconds: 3_000_000_000)
            }
        }
    }
}

struct SnackbarView: View {
    let message: String
    
    var body: some View {
        Text(message)
            .padding()
            .background(Color.black.opacity(0.8))
            .foregroundColor(.white)
            .cornerRadius(8)
    }
}

extension Notification.Name {
    static let clearInput = Notification.Name("clearInput")
    static let realtimeDidUpdate = Notification.Name("realtimeDidUpdate")
}
