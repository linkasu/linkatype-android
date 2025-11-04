import SwiftUI
import FirebaseCore
import FirebaseDatabase
import FirebaseAnalytics

@main
struct LINKaTypeiOSApp: App {
    @StateObject private var authManager = FirebaseAuthManager.shared
    @StateObject private var trackingManager = TrackingConsentManager.shared
    
    init() {
        FirebaseApp.configure()
        
        Analytics.setAnalyticsCollectionEnabled(false)
        
        Database.database().isPersistenceEnabled = true
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(authManager)
                .environmentObject(trackingManager)
        }
    }
}

