import SwiftUI
import Shared
import AVFoundation

struct DialogView: View {
    @EnvironmentObject var authManager: FirebaseAuthManager
    private let sdk = SharedSdkProvider.shared.sdk

    @StateObject private var ttsManager = TtsManager.shared
    @State private var chats: [DialogChat] = []
    @State private var activeChatId: String?
    @State private var messages: [DialogMessage] = []
    @State private var inputText = ""
    @State private var isRecording = false
    @State private var recorder: AVAudioRecorder?
    @State private var showError = false
    @State private var errorMessage = ""
    @State private var suggestions: [String] = []
    @State private var isProcessing = false
    @State private var isDrawerOpen = false
    @State private var isLoadingChats = false
    @State private var isLoadingMessages = false
    @State private var scrollTarget: String?
    @State private var showDeleteConfirm = false
    @State private var chatPendingDelete: DialogChat?

    var body: some View {
        ZStack(alignment: .leading) {
            mainContent
                .overlay {
                    if isDrawerOpen {
                        Color.black.opacity(0.25)
                            .ignoresSafeArea()
                            .onTapGesture { toggleDrawer(false) }
                    }
                }
                .disabled(isDrawerOpen)

            drawerContent
        }
        .navigationTitle(NSLocalizedString("dialog_title", comment: ""))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: { toggleDrawer(!isDrawerOpen) }) {
                    Image(systemName: "line.3.horizontal")
                }
            }
        }
        .task { await loadChats() }
        .onChange(of: activeChatId) { newValue in
            if let chatId = newValue {
                Task { await loadMessages(chatId: chatId) }
            }
        }
        .alert(errorMessage, isPresented: $showError) {
            Button(NSLocalizedString("ok", comment: ""), role: .cancel) {}
        }
        .confirmationDialog(
            NSLocalizedString("dialog_delete_chat_confirm", comment: ""),
            isPresented: $showDeleteConfirm,
            titleVisibility: .visible
        ) {
            Button(NSLocalizedString("dialog_delete_chat", comment: ""), role: .destructive) {
                if let chat = chatPendingDelete {
                    Task { await deleteChat(chat) }
                }
            }
            Button(NSLocalizedString("cancel", comment: ""), role: .cancel) {}
        }
        .accessibilityIdentifier("dialog_view")
        .onAppear {
            FirebaseAnalyticsManager.shared.logDialogOpened()
        }
        .onDisappear {
            FirebaseAnalyticsManager.shared.logDialogClosed()
        }
    }

    private var mainContent: some View {
        VStack(spacing: 0) {
            if isLoadingMessages {
                ProgressView()
                    .progressViewStyle(.circular)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                messageList
            }
        }
        .background(Color(.systemBackground))
        .safeAreaInset(edge: .bottom) {
            inputPanel
        }
    }

    private var messageList: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 12) {
                    if messages.isEmpty {
                        Text(NSLocalizedString("dialog_empty", comment: ""))
                            .foregroundColor(.secondary)
                            .padding(.top, 24)
                    }

                    ForEach(messages, id: \.id) { message in
                        DialogMessageRow(message: message)
                            .id(message.id)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .padding(.bottom, 24)
            }
            .onChange(of: scrollTarget) { target in
                if let target {
                    withAnimation(.easeOut(duration: 0.2)) {
                        proxy.scrollTo(target, anchor: .bottom)
                    }
                }
            }
        }
    }

    private var inputPanel: some View {
        VStack(alignment: .leading, spacing: 12) {
            if !suggestions.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(suggestions, id: \.self) { text in
                            Button(action: { Task { await sendSuggestion(text) } }) {
                                Text(text)
                                    .font(.footnote)
                                    .lineLimit(2)
                                    .padding(.vertical, 8)
                                    .padding(.horizontal, 12)
                                    .background(Capsule().fill(Color(.secondarySystemBackground)))
                            }
                        }
                    }
                }
            }

            ZStack(alignment: .topLeading) {
                TextEditor(text: $inputText)
                    .frame(minHeight: 44, maxHeight: 120)
                    .padding(8)
                    .background(RoundedRectangle(cornerRadius: 12).fill(Color(.secondarySystemBackground)))

                if inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text(NSLocalizedString("dialog_message_hint", comment: ""))
                        .foregroundColor(.secondary)
                        .padding(.top, 16)
                        .padding(.leading, 14)
                }
            }

            HStack(spacing: 12) {
                Button(action: { Task { await sendMessage() } }) {
                    Label(NSLocalizedString("dialog_send", comment: ""), systemImage: "paperplane.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .disabled(inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                Button(action: { toggleRecording() }) {
                    Label(
                        NSLocalizedString(isRecording ? "dialog_stop_record" : "dialog_record", comment: ""),
                        systemImage: isRecording ? "stop.circle.fill" : "mic.fill"
                    )
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .tint(isRecording ? .red : .accentColor)

                Button(action: { inputText = "" }) {
                    Label(NSLocalizedString("clear", comment: ""), systemImage: "xmark.circle")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }

            if isProcessing {
                Text(NSLocalizedString("dialog_recognizing", comment: ""))
                    .font(.footnote)
                    .foregroundColor(.secondary)
            } else if isRecording {
                Text(NSLocalizedString("dialog_recording", comment: ""))
                    .font(.footnote)
                    .foregroundColor(.red)
            }
        }
        .padding(16)
        .background(
            Color(.systemBackground)
                .shadow(color: Color.black.opacity(0.08), radius: 12, x: 0, y: -2)
        )
    }

    private var drawerContent: some View {
        let drawerWidth = min(320, UIScreen.main.bounds.width * 0.82)
        return VStack(spacing: 0) {
            HStack {
                Text(NSLocalizedString("dialog_chats_title", comment: ""))
                    .font(.title2)
                    .fontWeight(.semibold)
                Spacer()
                Button(action: { Task { await createChat() } }) {
                    Label(NSLocalizedString("dialog_new_chat", comment: ""), systemImage: "plus")
                }
                .buttonStyle(.bordered)
            }
            .padding(16)

            Divider()

            if isLoadingChats {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(chats, id: \.id) { chat in
                            DialogChatRow(
                                chat: chat,
                                isSelected: chat.id == activeChatId,
                                onSelect: { selectChat(chat) },
                                onDelete: { requestDelete(chat) }
                            )
                        }
                    }
                    .padding(16)
                }
            }
        }
        .frame(width: drawerWidth)
        .background(Color(.systemBackground))
        .offset(x: isDrawerOpen ? 0 : -drawerWidth)
        .animation(.easeInOut(duration: 0.25), value: isDrawerOpen)
        .ignoresSafeArea()
    }

    private func toggleDrawer(_ open: Bool) {
        withAnimation(.easeInOut(duration: 0.25)) {
            isDrawerOpen = open
        }
    }

    private func requestDelete(_ chat: DialogChat) {
        chatPendingDelete = chat
        showDeleteConfirm = true
    }

    private func loadChats() async {
        await MainActor.run { isLoadingChats = true }
        do {
            let list = try await sdk.dialogRepository.listChats()
            let resolved = list.isEmpty ? [try await sdk.dialogRepository.createChat(title: nil)] : list
            let sorted = sortChats(resolved)
            await MainActor.run {
                chats = sorted
                if activeChatId == nil || !sorted.contains(where: { $0.id == activeChatId }) {
                    activeChatId = sorted.first?.id
                }
                isLoadingChats = false
            }
        } catch {
            await MainActor.run { isLoadingChats = false }
        }
    }

    private func createChat() async {
        do {
            let chat = try await sdk.dialogRepository.createChat(title: nil)
            await MainActor.run {
                chats = sortChats([chat] + chats)
                activeChatId = chat.id
                toggleDrawer(false)
            }
            FirebaseAnalyticsManager.shared.logDialogChatCreate()
        } catch {
            presentError(NSLocalizedString("auth_error_generic", comment: ""))
        }
    }

    private func selectChat(_ chat: DialogChat) {
        activeChatId = chat.id
        toggleDrawer(false)
        let count = Int(chat.messageCount?.int32Value ?? 0)
        FirebaseAnalyticsManager.shared.logDialogChatSelect(messageCount: count)
    }

    private func deleteChat(_ chat: DialogChat) async {
        let count = Int(chat.messageCount?.int32Value ?? 0)
        FirebaseAnalyticsManager.shared.logDialogChatDelete(messageCount: count)
        do {
            try await sdk.dialogRepository.deleteChat(id: chat.id)
        } catch {
            // ignore
        }

        await MainActor.run {
            chats.removeAll { $0.id == chat.id }
            if activeChatId == chat.id {
                activeChatId = chats.first?.id
            }
        }

        if chats.isEmpty {
            await createChat()
        }
    }

    private func loadMessages(chatId: String) async {
        await MainActor.run { isLoadingMessages = true }
        do {
            let list = try await sdk.dialogRepository.listMessages(chatId: chatId, limit: 200, before: nil)
            await MainActor.run {
                messages = list
                scrollTarget = list.last?.id
                isLoadingMessages = false
                suggestions = []
            }
        } catch {
            await MainActor.run { isLoadingMessages = false }
        }
    }

    private func sendMessage() async {
        guard let chatId = activeChatId else { return }
        let text = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
        if text.isEmpty { return }
        await MainActor.run { inputText = "" }
        ttsManager.speak(text)
        FirebaseAnalyticsManager.shared.logDialogMessageSend(source: "typed", textLength: text.count)
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
                appendMessage(result.message)
                updateSuggestions(result.suggestions)
            }
        } catch {
            // ignore
        }
    }

    private func toggleRecording() {
        if isRecording {
            stopRecording()
        } else {
            Task {
                if await ensureRecordPermission() {
                    startRecording()
                }
            }
        }
    }

    private func ensureRecordPermission() async -> Bool {
        let session = AVAudioSession.sharedInstance()
        switch session.recordPermission {
        case .granted:
            return true
        case .denied:
            presentError(NSLocalizedString("dialog_record_permission_denied", comment: ""))
            return false
        case .undetermined:
            return await withCheckedContinuation { continuation in
                session.requestRecordPermission { granted in
                    DispatchQueue.main.async {
                        if !granted {
                            self.presentError(NSLocalizedString("dialog_record_permission_denied", comment: ""))
                        }
                        continuation.resume(returning: granted)
                    }
                }
            }
        @unknown default:
            return false
        }
    }

    private func startRecording() {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("dialog_record.wav")
        let settings: [String: Any] = [
            AVFormatIDKey: kAudioFormatLinearPCM,
            AVSampleRateKey: 16000,
            AVNumberOfChannelsKey: 1,
            AVLinearPCMBitDepthKey: 16,
            AVLinearPCMIsFloatKey: false,
            AVLinearPCMIsBigEndianKey: false
        ]
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playAndRecord, mode: .spokenAudio, options: [.defaultToSpeaker])
            try session.setActive(true)
            recorder = try AVAudioRecorder(url: url, settings: settings)
            recorder?.record()
            isRecording = true
            FirebaseAnalyticsManager.shared.logDialogRecordStart()
        } catch {
            recorder = nil
            isRecording = false
            presentError(NSLocalizedString("dialog_send_error", comment: ""))
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
        FirebaseAnalyticsManager.shared.logDialogRecordStop()
        Task { await sendAudio(data) }
    }

    private func sendAudio(_ data: Data) async {
        guard let chatId = activeChatId else { return }
        if authManager.mode == "offline" {
            presentError(NSLocalizedString("auth_online_required_title", comment: ""))
            return
        }
        if data.count > maxAudioBytes {
            presentError(NSLocalizedString("dialog_audio_too_large", comment: ""))
            return
        }
        await MainActor.run { isProcessing = true }
        FirebaseAnalyticsManager.shared.logDialogMessageSend(source: "audio", audioBytes: data.count)
        do {
            let result = try await sdk.dialogRepository.sendAudioMessage(
                chatId: chatId,
                role: "speaker",
                audioBytes: data.toKotlinByteArray(),
                mimeType: "audio/wav",
                filename: "dialog_record.wav",
                created: KotlinLong(value: Int64(Date().timeIntervalSince1970 * 1000)),
                source: "audio",
                includeSuggestions: true
            )
            let transcript = result.transcript
            let message = (transcript?.isEmpty == false && result.message.content.isEmpty)
                ? result.message.doCopy(
                    id: result.message.id,
                    chatId: result.message.chatId,
                    role: result.message.role,
                    content: transcript ?? "",
                    source: result.message.source,
                    created: result.message.created,
                    updatedAt: result.message.updatedAt
                )
                : result.message
            await MainActor.run {
                appendMessage(message)
                updateSuggestions(result.suggestions)
                isProcessing = false
            }
        } catch {
            presentError(NSLocalizedString("dialog_send_error", comment: ""))
            await MainActor.run { isProcessing = false }
        }
    }

    private func sendSuggestion(_ text: String) async {
        guard let chatId = activeChatId else { return }
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return }
        await MainActor.run { clearSuggestions() }
        ttsManager.speak(trimmed)
        FirebaseAnalyticsManager.shared.logDialogMessageSend(source: "suggestion", textLength: trimmed.count)
        do {
            let result = try await sdk.dialogRepository.sendMessage(
                chatId: chatId,
                role: "disabled_person",
                content: trimmed,
                source: "suggestion",
                created: KotlinLong(value: Int64(Date().timeIntervalSince1970 * 1000)),
                includeSuggestions: true
            )
            await MainActor.run {
                appendMessage(result.message)
                updateSuggestions(result.suggestions)
            }
        } catch {
            // ignore
        }
    }

    private func updateSuggestions(_ list: [String]?) {
        let items = list ?? []
        suggestions = Array(items.prefix(maxSuggestions))
    }

    private func clearSuggestions() {
        suggestions = []
    }

    private func appendMessage(_ message: DialogMessage) {
        messages.append(message)
        scrollTarget = message.id
        updateChatMeta(message)
    }

    private func updateChatMeta(_ message: DialogMessage) {
        guard let index = chats.firstIndex(where: { $0.id == message.chatId }) else { return }
        let chat = chats[index]
        let lastMessageAt = KotlinLong(value: message.created)
        let count = (chat.messageCount?.int32Value ?? 0) + 1
        let updated = chat.doCopy(
            id: chat.id,
            title: chat.title,
            created: chat.created,
            updatedAt: lastMessageAt,
            lastMessageAt: lastMessageAt,
            messageCount: KotlinInt(value: Int32(count))
        )
        chats[index] = updated
        chats = sortChats(chats)
    }

    private func sortChats(_ list: [DialogChat]) -> [DialogChat] {
        list.sorted { lhs, rhs in
            let left = lhs.lastMessageAt?.int64Value ?? lhs.updatedAt?.int64Value ?? lhs.created
            let right = rhs.lastMessageAt?.int64Value ?? rhs.updatedAt?.int64Value ?? rhs.created
            return left > right
        }
    }

    private func presentError(_ message: String) {
        errorMessage = message
        showError = true
    }

    private var maxAudioBytes: Int { 8 * 1024 * 1024 }
    private var maxSuggestions: Int { 5 }
}

private struct DialogMessageRow: View {
    let message: DialogMessage

    private var isUser: Bool { message.role == "disabled_person" }

    var body: some View {
        VStack(alignment: isUser ? .trailing : .leading, spacing: 6) {
            Text("\(roleLabel) • \(formatTime(message.created))")
                .font(.caption)
                .foregroundColor(.secondary)

            Text(contentText)
                .font(.body)
                .padding(.vertical, 10)
                .padding(.horizontal, 12)
                .background(bubbleColor)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .frame(maxWidth: UIScreen.main.bounds.width * 0.76, alignment: isUser ? .trailing : .leading)
        }
        .frame(maxWidth: .infinity, alignment: isUser ? .trailing : .leading)
    }

    private var roleLabel: String {
        if isUser {
            return NSLocalizedString("dialog_you", comment: "")
        }
        return NSLocalizedString("dialog_speaker", comment: "")
    }

    private var contentText: String {
        let text = message.content
        if text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return NSLocalizedString("dialog_audio_message", comment: "")
        }
        return text
    }

    private var bubbleColor: Color {
        if isUser {
            return Color.accentColor.opacity(0.12)
        }
        return Color(.secondarySystemBackground)
    }

    private func formatTime(_ millis: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(millis) / 1000)
        let formatter = DateFormatter()
        formatter.locale = Locale.current
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: date)
    }
}

private struct DialogChatRow: View {
    let chat: DialogChat
    let isSelected: Bool
    let onSelect: () -> Void
    let onDelete: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(chat.title ?? NSLocalizedString("dialog_untitled", comment: ""))
                    .font(.headline)
                    .foregroundColor(.primary)
                    .lineLimit(1)

                Text(subtitleText)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()

            Button(action: onDelete) {
                Image(systemName: "trash")
            }
            .buttonStyle(.borderless)
            .foregroundColor(.secondary)
            .accessibilityLabel(NSLocalizedString("dialog_delete_chat", comment: ""))
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(Color(.secondarySystemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(isSelected ? Color.accentColor : Color.clear, lineWidth: 2)
        )
        .onTapGesture { onSelect() }
    }

    private var subtitleText: String {
        if let last = chat.lastMessageAt?.int64Value {
            return formatDayTime(last)
        }
        if let count = chat.messageCount?.int32Value {
            return String(format: NSLocalizedString("dialog_message_count", comment: ""), Int(count))
        }
        return NSLocalizedString("dialog_empty", comment: "")
    }

    private func formatDayTime(_ millis: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(millis) / 1000)
        let formatter = DateFormatter()
        formatter.locale = Locale.current
        formatter.dateFormat = "dd MMM, HH:mm"
        return formatter.string(from: date)
    }
}
