import SwiftUI
import FirebaseCore

@main
struct LINKaTypeiOSApp: App {
    @StateObject private var authManager = FirebaseAuthManager.shared
    
    init() {
        FirebaseApp.configure()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(authManager)
        }
    }
}
