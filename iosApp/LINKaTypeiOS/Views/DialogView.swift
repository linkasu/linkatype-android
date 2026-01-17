import SwiftUI
import Shared
import AVFoundation

struct DialogView: View {
    private let sdk = SharedSdkProvider.shared.sdk

    @State private var chats: [DialogChat] = []
    @State private var activeChatId: String?
    @State private var messages: [DialogMessage] = []
    @State private var inputText = ""
    @State private var isRecording = false
    @State private var recorder: AVAudioRecorder?
    @State private var showError = false
    @State private var errorMessage = ""

    var body: some View {
        VStack(spacing: 12) {
            Picker("", selection: Binding(
                get: { activeChatId ?? "" },
                set: { activeChatId = $0.isEmpty ? nil : $0 }
            )) {
                ForEach(chats, id: \.id) { chat in
                    Text(chat.title ?? NSLocalizedString("dialog_untitled", comment: "")).tag(chat.id)
                }
            }
            .pickerStyle(.menu)

            List(messages, id: \.id) { message in
                Text("\(message.role): \(message.content)")
            }

            HStack {
                TextField(NSLocalizedString("dialog_message_hint", comment: ""), text: $inputText)
                    .textFieldStyle(.roundedBorder)

                Button(NSLocalizedString("dialog_send", comment: "")) {
                    Task { await sendMessage() }
                }
                .disabled(inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                Button(isRecording ? NSLocalizedString("dialog_record", comment: "") : NSLocalizedString("dialog_record", comment: "")) {
                    toggleRecording()
                }
            }
        }
        .padding()
        .task { await loadChats() }
        .onChange(of: activeChatId) { newValue in
            if let chatId = newValue {
                Task { await loadMessages(chatId: chatId) }
            }
        }
        .alert(errorMessage, isPresented: $showError) {
            Button(NSLocalizedString("ok", comment: ""), role: .cancel) {}
        }
    }

    private func loadChats() async {
        do {
            let list = try await sdk.dialogRepository.listChats()
            let resolved = list.isEmpty ? [try await sdk.dialogRepository.createChat(title: nil)] : list
            await MainActor.run {
                chats = resolved
                activeChatId = resolved.first?.id
            }
            if let chatId = resolved.first?.id {
                await loadMessages(chatId: chatId)
            }
        } catch {
            // ignore
        }
    }

    private func loadMessages(chatId: String) async {
        do {
            let list = try await sdk.dialogRepository.listMessages(chatId: chatId, limit: 200, before: nil)
            await MainActor.run {
                messages = list
            }
        } catch {
            // ignore
        }
    }

    private func sendMessage() async {
        guard let chatId = activeChatId else { return }
        let text = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
        if text.isEmpty { return }
        inputText = ""
        do {
            let result = try await sdk.dialogRepository.sendMessage(
                chatId: chatId,
                role: "disabled_person",
                content: text,
                source: "typed",
                created: KotlinLong(value: Int64(Date().timeIntervalSince1970 * 1000)),
                includeSuggestions: true
            )
            await MainActor.run {
                messages.append(result.message)
            }
        } catch {
            // ignore
        }
    }

    private func toggleRecording() {
        if isRecording {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private func startRecording() {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("dialog_record.m4a")
        let settings: [String: Any] = [
            AVFormatIDKey: kAudioFormatMPEG4AAC,
            AVSampleRateKey: 16000,
            AVNumberOfChannelsKey: 1,
            AVEncoderBitRateKey: 96000
        ]
        do {
            recorder = try AVAudioRecorder(url: url, settings: settings)
            recorder?.record()
            isRecording = true
        } catch {
            recorder = nil
            isRecording = false
        }
    }

    private func stopRecording() {
        recorder?.stop()
        guard let url = recorder?.url else {
            recorder = nil
            isRecording = false
            return
        }
        recorder = nil
        isRecording = false

        guard let data = try? Data(contentsOf: url) else { return }
        Task {
            await sendAudio(data)
        }
    }

    private func sendAudio(_ data: Data) async {
        guard let chatId = activeChatId else { return }
        if data.count > maxAudioBytes {
            await MainActor.run {
                errorMessage = NSLocalizedString("dialog_audio_too_large", comment: "")
                showError = true
            }
            return
        }
        do {
            let _ = try await sdk.dialogRepository.sendAudioMessage(
                chatId: chatId,
                role: "disabled_person",
                audioBytes: data.toKotlinByteArray(),
                mimeType: "audio/mp4",
                filename: "dialog_record.m4a",
                created: KotlinLong(value: Int64(Date().timeIntervalSince1970 * 1000)),
                source: "audio",
                includeSuggestions: true
            )
        } catch {
            // ignore
        }
    }

    private var maxAudioBytes: Int { 8 * 1024 * 1024 }
}
