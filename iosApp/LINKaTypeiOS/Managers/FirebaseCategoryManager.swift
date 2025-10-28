import Foundation
import FirebaseDatabase
import Combine

class FirebaseCategoryManager: ObservableObject {
    @Published var categories: [String: String] = [:]
    
    private var ref: DatabaseReference?
    private var handle: DatabaseHandle?
    
    func startListening(userId: String) {
        ref = Database.database().reference().child("users/\(userId)/Category")
        
        handle = ref?.observe(.value) { [weak self] snapshot in
            guard let self = self else { return }
            var result: [String: String] = [:]
            
            for child in snapshot.children {
                if let childSnapshot = child as? DataSnapshot,
                   let dict = childSnapshot.value as? [String: Any],
                   let category = Category(from: dict) {
                    result[category.id] = category.label
                }
            }
            
            DispatchQueue.main.async {
                self.categories = result
            }
        }
    }
    
    func stopListening() {
        if let handle = handle {
            ref?.removeObserver(withHandle: handle)
        }
        ref = nil
        handle = nil
    }
    
    func create(label: String, userId: String) async throws {
        let ref = Database.database().reference().child("users/\(userId)/Category").childByAutoId()
        let id = ref.key ?? UUID().uuidString
        let data: [String: Any] = [
            "id": id,
            "label": label,
            "created": Int64(Date().timeIntervalSince1970 * 1000)
        ]
        try await ref.updateChildValues(data)
    }
    
    func edit(id: String, label: String, userId: String) async throws {
        let ref = Database.database().reference().child("users/\(userId)/Category/\(id)")
        try await ref.updateChildValues(["label": label])
    }
    
    func remove(id: String, userId: String) async throws {
        let ref = Database.database().reference().child("users/\(userId)/Category/\(id)")
        try await ref.removeValue()
    }
}

