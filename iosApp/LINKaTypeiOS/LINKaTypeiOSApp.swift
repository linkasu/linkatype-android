import SwiftUI

@main
struct LINKaTypeiOSApp: App {
    @StateObject private var authManager = FirebaseAuthManager.shared
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(authManager)
        }
    }
}
