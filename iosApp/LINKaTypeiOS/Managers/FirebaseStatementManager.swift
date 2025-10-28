import Foundation
import FirebaseDatabase
import Combine

class FirebaseStatementManager: ObservableObject {
    @Published var statements: [String: String] = [:]
    
    private var ref: DatabaseReference?
    private var handle: DatabaseHandle?
    private var currentCategoryId: String?
    
    func startListening(userId: String, categoryId: String) {
        stopListening()
        currentCategoryId = categoryId
        
        var combinedResults: [String: String] = [:]
        let dispatchGroup = DispatchGroup()
        
        dispatchGroup.enter()
        ref = Database.database().reference().child("users/\(userId)/Statement/\(categoryId)")
        
        handle = ref?.observe(.value) { [weak self] snapshot in
            guard let self = self else { 
                dispatchGroup.leave()
                return 
            }
            
            for child in snapshot.children {
                if let childSnapshot = child as? DataSnapshot,
                   let dict = childSnapshot.value as? [String: Any],
                   let statement = Statement(from: dict) {
                    combinedResults[statement.id] = statement.label
                }
            }
            
            dispatchGroup.leave()
        }
        
        dispatchGroup.enter()
        let categoryRef = Database.database().reference().child("users/\(userId)/Category/\(categoryId)/statements")
        
        categoryRef.observeSingleEvent(of: .value) { snapshot in
            if snapshot.exists() {
                for child in snapshot.children {
                    if let childSnapshot = child as? DataSnapshot,
                       let dict = childSnapshot.value as? [String: Any],
                       let statement = Statement(from: dict) {
                        if combinedResults[statement.id] == nil {
                            combinedResults[statement.id] = statement.label
                        }
                    }
                }
            }
            
            dispatchGroup.leave()
        }
        
        dispatchGroup.notify(queue: .main) { [weak self] in
            self?.statements = combinedResults
        }
    }

    
    func stopListening() {
        if let handle = handle {
            ref?.removeObserver(withHandle: handle)
        }
        ref = nil
        handle = nil
        currentCategoryId = nil
    }
    
    func create(label: String, userId: String, categoryId: String) async throws {
        let ref = Database.database().reference().child("users/\(userId)/Statement/\(categoryId)").childByAutoId()
        let id = ref.key ?? UUID().uuidString
        let data: [String: Any] = [
            "id": id,
            "label": label,
            "created": Int64(Date().timeIntervalSince1970 * 1000)
        ]
        try await ref.updateChildValues(data)
    }
    
    func edit(id: String, label: String, userId: String, categoryId: String) async throws {
        let ref = Database.database().reference().child("users/\(userId)/Statement/\(categoryId)/\(id)")
        try await ref.updateChildValues(["label": label])
    }
    
    func remove(id: String, userId: String, categoryId: String) async throws {
        let ref = Database.database().reference().child("users/\(userId)/Statement/\(categoryId)/\(id)")
        try await ref.removeValue()
    }
}

