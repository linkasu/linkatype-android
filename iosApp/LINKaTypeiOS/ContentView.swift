import SwiftUI

struct ContentView: View {
    @EnvironmentObject var authManager: FirebaseAuthManager
    
    var body: some View {
        if authManager.isAuthenticated {
            MainView()
        } else {
            AuthView()
        }
    }
}

