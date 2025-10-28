import SwiftUI
import FirebaseCore
import FirebaseDatabase

@main
struct LINKaTypeiOSApp: App {
    @StateObject private var authManager = FirebaseAuthManager.shared
    
    init() {
        FirebaseApp.configure()
        
        // ВАЖНО: setPersistenceEnabled должен вызываться ДО любого использования Database
        Database.database().isPersistenceEnabled = true
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(authManager)
        }
    }
}

