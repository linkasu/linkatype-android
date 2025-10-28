import Foundation

struct Statement: Identifiable, Codable {
    let id: String
    let label: String
    let created: Int64
    
    init(id: String, label: String, created: Int64) {
        self.id = id
        self.label = label
        self.created = created
    }
    
    init?(from dict: [String: Any]) {
        guard let id = dict["id"] as? String else {
            return nil
        }
        
        // Поддержка обоих полей: "label" и "text" для совместимости
        let label = (dict["label"] as? String) ?? (dict["text"] as? String)
        guard let labelValue = label else {
            return nil
        }
        
        // created может быть Int64 или Int
        let created: Int64
        if let createdInt64 = dict["created"] as? Int64 {
            created = createdInt64
        } else if let createdInt = dict["created"] as? Int {
            created = Int64(createdInt)
        } else {
            return nil
        }
        
        self.id = id
        self.label = labelValue
        self.created = created
    }
    
    func toDict() -> [String: Any] {
        return [
            "id": id,
            "label": label,
            "created": created
        ]
    }
}

