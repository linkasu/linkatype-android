import SwiftUI

struct SettingsView: View {
    @ObservedObject var ttsManager: TtsManager
    @EnvironmentObject var authManager: FirebaseAuthManager
    @ObservedObject private var trackingConsentManager = TrackingConsentManager.shared
    @Environment(\.dismiss) var dismiss
    
    @State private var useYandex = true
    @State private var selectedVoiceId = ""
    @State private var volume: Float = 1.0
    @State private var rate: Float = 1.0
    @State private var pitch: Float = 1.0
    @State private var cacheEnabled = false
    @State private var cacheSizeLimitMb: Double = 1000
    @State private var cacheInfo = ""
    @State private var statusText = NSLocalizedString("tts_status_ready", comment: "")
    @State private var availableVoices: [TtsVoice] = []
    @State private var showDeleteAccountAlert = false
    @State private var isDeleting = false
    @State private var deleteAccountError: String?
    
    var body: some View {
        Form {
            Section(header: Text(NSLocalizedString("settings_section_tts", comment: ""))) {
                Toggle(NSLocalizedString("settings_use_yandex", comment: ""), isOn: $useYandex)
                    .onChange(of: useYandex) { newValue in
                        ttsManager.setUseYandex(newValue)
                        loadVoices()
                    }
                
                if !availableVoices.isEmpty {
                    Picker(NSLocalizedString("settings_voice", comment: ""), selection: $selectedVoiceId) {
                        ForEach(availableVoices) { voice in
                            Text(voice.title).tag(voice.id)
                        }
                    }
                    .onChange(of: selectedVoiceId) { newValue in
                        ttsManager.setVoiceId(newValue)
                    }
                }
                
                VStack(alignment: .leading) {
                    Text(NSLocalizedString("settings_volume", comment: ""))
                    Slider(value: $volume, in: 0...1)
                        .onChange(of: volume) { newValue in
                            ttsManager.setVolume(newValue)
                        }
                }
                
                VStack(alignment: .leading) {
                    Text(NSLocalizedString("settings_rate", comment: ""))
                    Slider(value: $rate, in: 0.1...2)
                        .onChange(of: rate) { newValue in
                            ttsManager.setRate(newValue)
                        }
                }
                
                VStack(alignment: .leading) {
                    Text(NSLocalizedString("settings_pitch", comment: ""))
                    Slider(value: $pitch, in: 0.5...2)
                        .onChange(of: pitch) { newValue in
                            ttsManager.setPitch(newValue)
                        }
                }
                
                HStack(spacing: 12) {
                    Button(NSLocalizedString("settings_test_tts", comment: "")) {
                        ttsManager.speak(NSLocalizedString("settings_test_phrase", comment: ""))
                    }
                    .frame(maxWidth: .infinity)
                    
                    Button(NSLocalizedString("settings_stop_tts", comment: "")) {
                        ttsManager.stop()
                    }
                    .frame(maxWidth: .infinity)
                    
                    Button(NSLocalizedString("settings_play_last", comment: "")) {
                        ttsManager.playLastAudio()
                    }
                    .frame(maxWidth: .infinity)
                }
                
                Text(statusText)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Section(header: Text(NSLocalizedString("settings_section_cache", comment: ""))) {
                Toggle(NSLocalizedString("settings_cache_enabled", comment: ""), isOn: $cacheEnabled)
                    .onChange(of: cacheEnabled) { newValue in
                        ttsManager.setCacheEnabled(newValue)
                    }
                
                VStack(alignment: .leading) {
                    Text(NSLocalizedString("settings_cache_limit", comment: ""))
                    Text(String(format: NSLocalizedString("settings_cache_limit_value", comment: ""), Int(cacheSizeLimitMb)))
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Slider(value: $cacheSizeLimitMb, in: 100...10000, step: 100)
                        .onChange(of: cacheSizeLimitMb) { newValue in
                            ttsManager.setCacheSizeLimitMb(newValue)
                        }
                        .disabled(!cacheEnabled)
                }
                
                Text(cacheInfo)
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                HStack {
                    Button(NSLocalizedString("settings_clear_cache", comment: "")) {
                        Task {
                            await ttsManager.clearCache()
                            await updateCacheInfo()
                        }
                    }
                    .disabled(!cacheEnabled)
                    
                    Spacer()
                    
                    Button(NSLocalizedString("settings_refresh_cache", comment: "")) {
                        Task {
                            await updateCacheInfo()
                        }
                    }
                }
            }

            Section(header: Text(NSLocalizedString("settings_section_privacy", comment: ""))) {
                Toggle(isOn: Binding(
                    get: { trackingConsentManager.isTrackingEnabled },
                    set: { trackingConsentManager.setTrackingEnabled($0) }
                )) {
                    Text(NSLocalizedString("settings_allow_tracking", comment: ""))
                }
                Text(NSLocalizedString("settings_tracking_description", comment: ""))
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            
            Section(header: Text(NSLocalizedString("settings_section_account", comment: ""))) {
                Button(role: .destructive) {
                    showDeleteAccountAlert = true
                } label: {
                    HStack {
                        if isDeleting {
                            ProgressView()
                                .frame(maxWidth: .infinity)
                        } else {
                            Text(NSLocalizedString("settings_delete_account", comment: ""))
                                .frame(maxWidth: .infinity)
                        }
                    }
                }
                .disabled(isDeleting)
                
                if let error = deleteAccountError {
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.red)
                }
            }
        }
        .navigationTitle(NSLocalizedString("settings", comment: ""))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button(NSLocalizedString("ok", comment: "")) {
                    dismiss()
                }
            }
        }
        .onAppear {
            loadInitialValues()
            loadVoices()
        }
        .onReceive(ttsManager.eventPublisher) { event in
            handleTtsEvent(event)
        }
        .alert(NSLocalizedString("settings_delete_account_title", comment: ""), isPresented: $showDeleteAccountAlert) {
            Button(NSLocalizedString("settings_delete_account_confirm", comment: ""), role: .destructive) {
                deleteAccount()
            }
            Button(NSLocalizedString("cancel", comment: ""), role: .cancel) {}
        } message: {
            Text(NSLocalizedString("settings_delete_account_message", comment: ""))
        }
    }
    
    private func loadInitialValues() {
        useYandex = ttsManager.getUseYandex()
        selectedVoiceId = ttsManager.getVoiceId()
        volume = ttsManager.getVolume()
        rate = ttsManager.getRate()
        pitch = ttsManager.getPitch()
        cacheEnabled = ttsManager.getCacheEnabled()
        cacheSizeLimitMb = ttsManager.getCacheSizeLimitMb()
        
        Task {
            await updateCacheInfo()
        }
    }
    
    private func loadVoices() {
        if useYandex {
            availableVoices = ttsManager.getYandexVoices()
        } else {
            availableVoices = ttsManager.getOfflineVoices()
        }
        
        if !availableVoices.contains(where: { $0.id == selectedVoiceId }) {
            if let first = availableVoices.first {
                selectedVoiceId = first.id
            }
        }
    }
    
    private func updateCacheInfo() async {
        let info = await ttsManager.getCacheInfo()
        await MainActor.run {
            cacheInfo = String(format: NSLocalizedString("settings_cache_info", comment: ""), info.fileCount, info.sizeMb, Int(info.sizeLimitMb))
        }
    }
    
    private func handleTtsEvent(_ event: TtsEvent) {
        switch event {
        case .speakingStarted:
            statusText = NSLocalizedString("tts_status_speaking", comment: "")
        case .speakingCompleted:
            statusText = NSLocalizedString("tts_status_ready", comment: "")
        case .error(let message):
            statusText = String(format: NSLocalizedString("tts_status_error", comment: ""), message)
        case .temporarilyUnavailable(let message):
            statusText = message
        case .status(let message):
            statusText = message
        case .downloadStarted:
            statusText = NSLocalizedString("tts_status_download", comment: "")
        case .downloadProgress(let current, let total):
            statusText = String(format: NSLocalizedString("tts_status_download_progress", comment: ""), current, total)
        case .downloadCompleted:
            statusText = NSLocalizedString("tts_status_ready", comment: "")
        }
    }
    
    private func deleteAccount() {
        deleteAccountError = nil
        isDeleting = true
        
        Task {
            do {
                try await authManager.deleteAccount()
                await MainActor.run {
                    isDeleting = false
                    dismiss()
                }
            } catch {
                await MainActor.run {
                    isDeleting = false
                    deleteAccountError = error.localizedDescription
                }
            }
        }
    }
}
