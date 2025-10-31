import SwiftUI

struct SpotlightTextItem: Identifiable {
    let id = UUID()
    let text: String
}

struct InputGroupView: View {
    @ObservedObject var ttsManager: TtsManager
    @State private var textSlots = ["", "", ""]
    @State private var currentSlot = 0
    @State private var spotlightItem: SpotlightTextItem?
    
    private let slotLabels = ["chat_slot_one", "chat_slot_two", "chat_slot_three"]
    
    var body: some View {
        VStack(spacing: 16) {
            HStack(spacing: 8) {
                TextField(NSLocalizedString("input_hint", comment: ""), text: $textSlots[currentSlot], axis: .vertical)
                    .textFieldStyle(.roundedBorder)
                    .lineLimit(3...6)
                
                Menu {
                    ForEach(0..<3) { index in
                        Button(NSLocalizedString(slotLabels[index], comment: "")) {
                            switchSlot(to: index)
                        }
                    }
                } label: {
                    Image(systemName: "square.stack.3d.up")
                        .padding(8)
                        .background(Color.blue.opacity(0.1))
                        .cornerRadius(8)
                }
            }
            
            HStack(spacing: 12) {
                Button(action: handleSayButton) {
                    HStack {
                        Image(systemName: ttsManager.isSpeaking ? "stop.fill" : "play.fill")
                        Text(NSLocalizedString(ttsManager.isSpeaking ? "stop" : "say", comment: ""))
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                }
                .disabled(textSlots[currentSlot].trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !ttsManager.isSpeaking)
                
                Button(action: {
                    FirebaseAnalyticsManager.shared.logSpotlightEvent()
                    spotlightItem = SpotlightTextItem(text: textSlots[currentSlot])
                }) {
                    Image(systemName: "arrow.up.left.and.arrow.down.right")
                        .padding()
                        .background(Color.green)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                }
                .disabled(textSlots[currentSlot].trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .clearInput)) { _ in
            textSlots[currentSlot] = ""
        }
        .fullScreenCover(item: $spotlightItem) { item in
            SpotlightView(text: item.text, isPresented: Binding(
                get: { spotlightItem != nil },
                set: { if !$0 { spotlightItem = nil } }
            ))
        }
    }
    
    private func handleSayButton() {
        if ttsManager.isSpeaking {
            ttsManager.stop()
        } else {
            let text = textSlots[currentSlot]
            ttsManager.speak(text)
            FirebaseAnalyticsManager.shared.logSayEvent()
        }
    }
    
    private func switchSlot(to index: Int) {
        guard index != currentSlot else { return }
        currentSlot = index
    }
}

