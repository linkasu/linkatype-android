import SwiftUI

struct ContentView: View {
    @EnvironmentObject var authManager: FirebaseAuthManager
    @EnvironmentObject var trackingManager: TrackingConsentManager
    @State private var hasRequestedATT = false
    
    var body: some View {
        if authManager.isAuthenticated {
            MainView()
                .onAppear {
                    requestATTIfNeeded()
                }
        } else {
            AuthView()
                .onAppear {
                    requestATTIfNeeded()
                }
        }
    }
    
    private func requestATTIfNeeded() {
        guard !hasRequestedATT && !trackingManager.hasShownATTRequest else { return }
        hasRequestedATT = true
        
        Task { @MainActor in
            let granted = await AppTrackingManager.shared.requestTrackingAuthorization()
            trackingManager.markATTRequestShown()
            trackingManager.setTrackingEnabled(granted)
        }
    }
}

