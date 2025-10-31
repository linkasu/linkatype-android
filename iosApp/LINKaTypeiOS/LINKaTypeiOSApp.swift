import SwiftUI
import FirebaseCore
import FirebaseDatabase

@main
struct LINKaTypeiOSApp: App {
    @StateObject private var authManager = FirebaseAuthManager.shared
    
    init() {
        AppTrackingManager.shared.requestTrackingAuthorizationSync()
        
        FirebaseApp.configure()
        
        Database.database().isPersistenceEnabled = true
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(authManager)
        }
    }
}

