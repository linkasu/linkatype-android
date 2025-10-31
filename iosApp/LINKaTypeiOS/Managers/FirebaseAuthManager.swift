import Foundation
import FirebaseAuth
import FirebaseDatabase
import Combine

class FirebaseAuthManager: ObservableObject {
    static let shared = FirebaseAuthManager()
    
    @Published var isAuthenticated = false
    @Published var currentUser: User?
    
    private var authStateHandle: AuthStateDidChangeListenerHandle?
    
    init() {
        authStateHandle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            // КРИТИЧНО: Обновление @Published должно быть на main thread
            DispatchQueue.main.async {
                self?.isAuthenticated = user != nil
                self?.currentUser = user
            }
        }
    }
    
    deinit {
        if let handle = authStateHandle {
            Auth.auth().removeStateDidChangeListener(handle)
        }
    }
    
    func signIn(email: String, password: String) async throws {
        try await Auth.auth().signIn(withEmail: email, password: password)
    }
    
    func signUp(email: String, password: String) async throws {
        try await Auth.auth().createUser(withEmail: email, password: password)
    }
    
    func signOut() throws {
        try Auth.auth().signOut()
    }
    
    func resetPassword(email: String) async throws {
        try await Auth.auth().sendPasswordReset(withEmail: email)
    }
    
    func getUserId() -> String? {
        return Auth.auth().currentUser?.uid
    }
    
    func deleteAccount() async throws {
        guard let userId = getUserId() else {
            throw NSError(domain: "FirebaseAuthManager", code: -1, userInfo: [NSLocalizedDescriptionKey: "No user logged in"])
        }
        
        let ref = Database.database().reference().child("users/\(userId)")
        try await ref.removeValue()
        
        try await Auth.auth().currentUser?.delete()
    }
}

