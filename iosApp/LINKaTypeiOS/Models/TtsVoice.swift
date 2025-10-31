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

struct YandexVoice {
    let voiceURI: String
    let text: String
}

