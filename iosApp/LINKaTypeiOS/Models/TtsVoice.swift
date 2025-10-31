import Foundation

struct TtsVoice: Identifiable {
    let id: String
    let title: String
    let locale: String?
    let provider: VoiceProvider
    let isDefault: Bool
    
    init(id: String, title: String, locale: String? = nil, provider: VoiceProvider, isDefault: Bool = false) {
        self.id = id
        self.title = title
        self.locale = locale
        self.provider = provider
        self.isDefault = isDefault
    }
}

enum VoiceProvider {
    case yandex
    case offline
}

struct YandexVoice: Codable {
    let voiceURI: String
    let text: String
    let langCode: String?
    let lang: String?
    let gender: String?
    let role: [String]?
    
    enum CodingKeys: String, CodingKey {
        case voiceURI = "id"
        case text = "name"
        case langCode = "lang_code"
        case lang
        case gender
        case role
    }
    
    init(voiceURI: String, text: String, langCode: String? = nil, lang: String? = nil, gender: String? = nil, role: [String]? = nil) {
        self.voiceURI = voiceURI
        self.text = text
        self.langCode = langCode
        self.lang = lang
        self.gender = gender
        self.role = role
    }
}

