import Foundation

struct Category: Identifiable, Codable {
    let id: String
    let label: String
    let created: Int64
    
    init(id: String, label: String, created: Int64) {
        self.id = id
        self.label = label
        self.created = created
    }
    
    init?(from dict: [String: Any]) {
        guard let id = dict["id"] as? String,
              let label = dict["label"] as? String,
              let created = dict["created"] as? Int64 else {
            return nil
        }
        self.id = id
        self.label = label
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

