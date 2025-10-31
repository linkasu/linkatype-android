import SwiftUI
import FirebaseCore
import FirebaseDatabase

@main
struct LINKaTypeiOSApp: App {
    @StateObject private var authManager = FirebaseAuthManager.shared
    @State private var hasRequestedTracking = false
    
    init() {
        FirebaseApp.configure()
        
        // ВАЖНО: setPersistenceEnabled должен вызываться ДО любого использования Database
        Database.database().isPersistenceEnabled = true
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(authManager)
                .onAppear {
                    if !hasRequestedTracking {
                        hasRequestedTracking = true
                        Task {
                            _ = await AppTrackingManager.shared.requestTrackingAuthorization()
                        }
                    }
                }
        }
    }
}

