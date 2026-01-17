import SwiftUI

struct MainView: View {
    @EnvironmentObject var authManager: FirebaseAuthManager
    private let sdk = SharedSdkProvider.shared.sdk
    @StateObject private var ttsManager = TtsManager.shared
    @State private var showSettings = false
    @State private var showDialog = false
    @State private var snackbarMessage: String?
    @State private var showSnackbar = false
    @State private var syncTimer = Timer.publish(every: 60, on: .main, in: .common).autoconnect()
    
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
                        }
                        
                        Button(action: { showSettings = true }) {
                            Label(NSLocalizedString("settings", comment: ""), systemImage: "gearshape")
                        }

                        Button(action: { showDialog = true }) {
                            Label(NSLocalizedString("dialog_title", comment: ""), systemImage: "message")
                        }
                        
                        Button(role: .destructive, action: logout) {
                            Label(NSLocalizedString("logout", comment: ""), systemImage: "rectangle.portrait.and.arrow.right")
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
            .sheet(isPresented: $showSettings) {
                NavigationStack {
                    SettingsView(ttsManager: ttsManager)
                }
            }
            .sheet(isPresented: $showDialog) {
                NavigationStack {
                    DialogView()
                }
            }
            .overlay(alignment: .bottom) {
                if showSnackbar, let message = snackbarMessage {
                    SnackbarView(message: message)
                        .padding()
                        .transition(.move(edge: .bottom))
                }
            }
            .task {
                _ = try? await sdk.offlineQueueProcessor.flush()
            }
            .task {
                await startRealtimeSync()
            }
            .onReceive(syncTimer) { _ in
                Task {
                    _ = try? await sdk.offlineQueueProcessor.flush()
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
