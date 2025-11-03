import Foundation
import AVFoundation
import Combine

class TtsManager: NSObject, ObservableObject, AVSpeechSynthesizerDelegate, AVAudioPlayerDelegate {
    static let shared = TtsManager()
    
    @Published var isSpeaking = false
    
    private let synthesizer = AVSpeechSynthesizer()
    private var audioPlayer: AVAudioPlayer?
    private let cacheManager = TtsCacheManager()
    private let userDefaults = UserDefaults.standard
    
    private let eventSubject = PassthroughSubject<TtsEvent, Never>()
    var eventPublisher: AnyPublisher<TtsEvent, Never> {
        eventSubject.eraseToAnyPublisher()
    }
    
    private var lastAudioFile: URL?
    private var lastErrorMessage: String?
    
    private let volumeKey = "tts_volume"
    private let rateKey = "tts_rate"
    private let pitchKey = "tts_pitch"
    private let useYandexKey = "tts_use_yandex"
    private let voiceIdKey = "tts_voice_id"
    
    private let defaultYandexVoice = "zahar"
    private let ttsEndpoint = "https://tts.linka.su/tts"
    private let voicesEndpoint = "https://tts.linka.su/voices"
    
    private let yandexVoices = [
        YandexVoice(voiceURI: "zahar", text: "Захар"),
        YandexVoice(voiceURI: "ermil", text: "Ермил"),
        YandexVoice(voiceURI: "jane", text: "Джейн"),
        YandexVoice(voiceURI: "oksana", text: "Оксана"),
        YandexVoice(voiceURI: "alena", text: "Алёна"),
        YandexVoice(voiceURI: "filipp", text: "Филипп"),
        YandexVoice(voiceURI: "omazh", text: "Ома")
    ]
    
    private var cachedYandexVoices: [YandexVoice]?
    private var voicesLoadingInProgress = false
    
    override init() {
        super.init()
        synthesizer.delegate = self
        loadYandexVoicesAsync()
    }
    
    func getVolume() -> Float {
        return userDefaults.float(forKey: volumeKey) != 0 ? userDefaults.float(forKey: volumeKey) : 1.0
    }
    
    func setVolume(_ value: Float) {
        userDefaults.set(value, forKey: volumeKey)
    }
    
    func getRate() -> Float {
        return userDefaults.float(forKey: rateKey) != 0 ? userDefaults.float(forKey: rateKey) : 1.0
    }
    
    func setRate(_ value: Float) {
        userDefaults.set(value, forKey: rateKey)
    }
    
    func getPitch() -> Float {
        return userDefaults.float(forKey: pitchKey) != 0 ? userDefaults.float(forKey: pitchKey) : 1.0
    }
    
    func setPitch(_ value: Float) {
        userDefaults.set(value, forKey: pitchKey)
    }
    
    func getUseYandex() -> Bool {
        return userDefaults.object(forKey: useYandexKey) as? Bool ?? true
    }
    
    func setUseYandex(_ value: Bool) {
        userDefaults.set(value, forKey: useYandexKey)
    }
    
    func getVoiceId() -> String {
        return userDefaults.string(forKey: voiceIdKey) ?? defaultYandexVoice
    }
    
    func setVoiceId(_ value: String) {
        userDefaults.set(value, forKey: voiceIdKey)
    }
    
    private func loadYandexVoicesAsync() {
        guard !voicesLoadingInProgress else { return }
        voicesLoadingInProgress = true
        
        Task {
            do {
                let voices = try await fetchVoicesFromApi()
                if !voices.isEmpty {
                    await MainActor.run {
                        self.cachedYandexVoices = voices
                    }
                }
            } catch {
                await MainActor.run {
                    self.lastErrorMessage = "Ошибка загрузки голосов: \(error.localizedDescription)"
                }
            }
            await MainActor.run {
                self.voicesLoadingInProgress = false
            }
        }
    }
    
    private func fetchVoicesFromApi() async throws -> [YandexVoice] {
        guard let url = URL(string: voicesEndpoint) else {
            return []
        }
        
        let (data, response) = try await URLSession.shared.data(from: url)
        
        guard let httpResponse = response as? HTTPURLResponse,
              httpResponse.statusCode == 200 else {
            return []
        }
        
        let decoder = JSONDecoder()
        let voices = try decoder.decode([YandexVoice].self, from: data)
        return voices
    }
    
    func getYandexVoices() -> [TtsVoice] {
        let voices = cachedYandexVoices ?? yandexVoices
        return voices.map { voice in
            TtsVoice(
                id: voice.voiceURI,
                title: voice.text,
                provider: .yandex,
                isDefault: voice.voiceURI == defaultYandexVoice
            )
        }
    }
    
    func getOfflineVoices() -> [TtsVoice] {
        let allVoices = AVSpeechSynthesisVoice.speechVoices()
        return allVoices.map { voice in
            TtsVoice(
                id: voice.identifier,
                title: voice.name,
                locale: voice.language,
                provider: .offline
            )
        }.sorted { voice1, voice2 in
            let v1IsRu = voice1.locale?.hasPrefix("ru") == true
            let v2IsRu = voice2.locale?.hasPrefix("ru") == true
            if v1IsRu && !v2IsRu { return true }
            if v2IsRu && !v1IsRu { return false }
            return voice1.title < voice2.title
        }
    }
    
    func speak(_ text: String, download: Bool = false) {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        
        if download || getUseYandex() {
            Task {
                await speakOnline(trimmed, download: download)
            }
        } else {
            speakOffline(trimmed)
        }
    }
    
    func stop() {
        synthesizer.stopSpeaking(at: .immediate)
        audioPlayer?.stop()
        audioPlayer = nil
        isSpeaking = false
        eventSubject.send(.speakingCompleted)
    }
    
    func playLastAudio() {
        guard let file = lastAudioFile, FileManager.default.fileExists(atPath: file.path) else {
            eventSubject.send(.error("Нет сохраненных аудиофайлов"))
            return
        }
        Task {
            await playAudioFile(file)
        }
    }
    
    func downloadPhrasesToCache(_ phrases: [String], voice: String?, onProgress: @escaping (Int, Int) -> Void) {
        Task {
            eventSubject.send(.downloadStarted)
            let total = phrases.count
            let targetVoice = resolveYandexVoiceId(voice)
            
            for (index, phrase) in phrases.enumerated() {
                let cacheKey = cacheManager.generateCacheKey(text: phrase, voice: targetVoice)
                if !cacheManager.isCached(cacheKey) {
                    if let bytes = await requestYandexAudio(text: phrase, voice: targetVoice) {
                        _ = cacheManager.saveToCache(cacheKey, data: bytes)
                    }
                }
                await MainActor.run {
                    onProgress(index + 1, total)
                }
                eventSubject.send(.downloadProgress(current: index + 1, total: total))
            }
            eventSubject.send(.downloadCompleted(path: nil))
        }
    }
    
    func getCacheInfo() async -> (fileCount: Int, sizeMb: Double, sizeLimitMb: Double) {
        return await cacheManager.getCacheInfo()
    }
    
    func getCacheEnabled() -> Bool {
        return cacheManager.getCacheEnabled()
    }
    
    func setCacheEnabled(_ enabled: Bool) {
        cacheManager.setCacheEnabled(enabled)
    }
    
    func getCacheSizeLimitMb() -> Double {
        return cacheManager.getCacheSizeLimitMb()
    }
    
    func setCacheSizeLimitMb(_ limit: Double) {
        cacheManager.setCacheSizeLimitMb(limit)
    }
    
    func clearCache() async {
        await cacheManager.clearCache()
    }
    
    private func speakOffline(_ text: String) {
        let utterance = AVSpeechUtterance(string: text)
        utterance.rate = getRate() * 0.5
        utterance.pitchMultiplier = getPitch()
        utterance.volume = getVolume()
        
        let voiceId = getVoiceId()
        if let voice = AVSpeechSynthesisVoice(identifier: voiceId) {
            utterance.voice = voice
        } else if let voice = AVSpeechSynthesisVoice(language: "ru-RU") {
            utterance.voice = voice
        }
        
        isSpeaking = true
        eventSubject.send(.speakingStarted)
        synthesizer.speak(utterance)
    }
    
    private func speakOnline(_ text: String, download: Bool) async {
        audioPlayer?.stop()
        audioPlayer = nil
        
        let voice = resolveYandexVoiceId(getVoiceId())
        let cacheEnabled = cacheManager.getCacheEnabled() && !download
        let cacheKey = cacheManager.generateCacheKey(text: text, voice: voice)
        
        var audioFile: URL?
        
        if cacheEnabled {
            audioFile = cacheManager.getCachedFile(cacheKey)
        }
        
        if audioFile == nil {
            guard let bytes = await requestYandexAudio(text: text, voice: voice) else {
                await MainActor.run {
                    eventSubject.send(.error("Ошибка загрузки аудио"))
                }
                return
            }
            
            if download {
                let savedPath = await saveToDownloads(bytes, text: text)
                await MainActor.run {
                    eventSubject.send(.downloadCompleted(path: savedPath))
                }
                return
            }
            
            if cacheEnabled {
                audioFile = cacheManager.saveToCache(cacheKey, data: bytes)
            } else {
                audioFile = writeTempFile(bytes)
            }
        }
        
        guard let file = audioFile else {
            await MainActor.run {
                eventSubject.send(.error("Не удалось подготовить аудио"))
            }
            return
        }
        
        await MainActor.run {
            isSpeaking = true
            eventSubject.send(.speakingStarted)
        }
        await playAudioFile(file)
    }
    
    private func requestYandexAudio(text: String, voice: String) async -> Data? {
        guard let url = URL(string: ttsEndpoint) else { return nil }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = 30
        
        let payload: [String: String] = ["text": text, "voice": voice]
        guard let jsonData = try? JSONSerialization.data(withJSONObject: payload) else {
            return nil
        }
        request.httpBody = jsonData
        
        do {
            let (data, response) = try await URLSession.shared.data(for: request)

            if let httpResponse = response as? HTTPURLResponse {
                if httpResponse.statusCode == 200 || httpResponse.statusCode == 201 {
                    return data
                } else {
                    lastErrorMessage = "HTTP \(httpResponse.statusCode)"
                    if (500...599).contains(httpResponse.statusCode) {
                        await notifyTemporarilyUnavailable()
                    }
                    return nil
                }
            }
            return nil
        } catch {
            lastErrorMessage = error.localizedDescription
            await notifyTemporarilyUnavailable()
            return nil
        }
    }

    private func notifyTemporarilyUnavailable() async {
        let message = NSLocalizedString("tts_temporarily_unavailable", comment: "")
        await MainActor.run {
            self.eventSubject.send(.temporarilyUnavailable(message))
        }
    }
    
    private func playAudioFile(_ file: URL) async {
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
            try AVAudioSession.sharedInstance().setActive(true)
            
            let player = try AVAudioPlayer(contentsOf: file)
            player.delegate = self
            player.volume = getVolume()
            
            await MainActor.run {
                self.audioPlayer = player
            }
            
            player.play()
            lastAudioFile = file
        } catch {
            await MainActor.run {
                isSpeaking = false
                eventSubject.send(.error("Ошибка воспроизведения: \(error.localizedDescription)"))
            }
        }
    }
    
    private func writeTempFile(_ data: Data) -> URL? {
        let tempDir = FileManager.default.temporaryDirectory
        let fileName = "tts_\(Date().timeIntervalSince1970).mp3"
        let fileURL = tempDir.appendingPathComponent(fileName)
        
        do {
            try data.write(to: fileURL)
            return fileURL
        } catch {
            return nil
        }
    }
    
    private func saveToDownloads(_ data: Data, text: String) async -> String? {
        let fileName = "LINKa_\(text.prefix(32)).mp3".replacingOccurrences(of: "[^A-Za-z0-9._-]", with: "_", options: .regularExpression)
        let documentsPath = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let fileURL = documentsPath.appendingPathComponent(fileName)
        
        do {
            try data.write(to: fileURL)
            return fileURL.path
        } catch {
            return nil
        }
    }
    
    private func resolveYandexVoiceId(_ requestedVoice: String?) -> String {
        guard let voice = requestedVoice, !voice.isEmpty, voice != "current" else {
            return defaultYandexVoice
        }
        
        if yandexVoices.contains(where: { $0.voiceURI == voice }) {
            return voice
        }
        
        return defaultYandexVoice
    }
    
    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        DispatchQueue.main.async {
            self.isSpeaking = false
        }
        eventSubject.send(.speakingCompleted)
    }
    
    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didCancel utterance: AVSpeechUtterance) {
        DispatchQueue.main.async {
            self.isSpeaking = false
        }
        eventSubject.send(.speakingCompleted)
    }
    
    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        DispatchQueue.main.async {
            self.isSpeaking = false
        }
        eventSubject.send(.speakingCompleted)
    }
    
    func audioPlayerDecodeErrorDidOccur(_ player: AVAudioPlayer, error: Error?) {
        DispatchQueue.main.async {
            self.isSpeaking = false
        }
        eventSubject.send(.error(error?.localizedDescription ?? "Ошибка декодирования"))
    }
}
