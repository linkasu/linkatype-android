import Foundation
import FirebaseAnalytics

class FirebaseAnalyticsManager {
  static let shared = FirebaseAnalyticsManager()
  
  private init() {}
  
  func logSayEvent() {
    guard TrackingConsentManager.shared.isAnalyticsEnabled else { return }
    Analytics.logEvent("say", parameters: nil)
  }
  
  func logSpotlightEvent() {
    guard TrackingConsentManager.shared.isAnalyticsEnabled else { return }
    Analytics.logEvent("spotlight", parameters: nil)
  }
  
  func logDownloadCategoryCacheEvent() {
    guard TrackingConsentManager.shared.isAnalyticsEnabled else { return }
    Analytics.logEvent("download_category_cache", parameters: nil)
  }

  func logRealtimeSyncEvent(changesCount: Int) {
    guard TrackingConsentManager.shared.isAnalyticsEnabled else { return }
    Analytics.logEvent("realtime_sync", parameters: ["changes": NSNumber(value: changesCount)])
  }

  func logRealtimeSyncError(message: String) {
    guard TrackingConsentManager.shared.isAnalyticsEnabled else { return }
    Analytics.logEvent("realtime_sync_error", parameters: ["message": message])
  }

  func logDialogOpened() {
    guard TrackingConsentManager.shared.isAnalyticsEnabled else { return }
    Analytics.logEvent("dialog_mode_opened", parameters: nil)
  }

  func logDialogClosed() {
    guard TrackingConsentManager.shared.isAnalyticsEnabled else { return }
    Analytics.logEvent("dialog_mode_closed", parameters: nil)
  }

  func logDialogChatCreate() {
    guard TrackingConsentManager.shared.isAnalyticsEnabled else { return }
    Analytics.logEvent("dialog_chat_create", parameters: nil)
  }

  func logDialogChatSelect(messageCount: Int) {
    guard TrackingConsentManager.shared.isAnalyticsEnabled else { return }
    Analytics.logEvent("dialog_chat_select", parameters: ["message_count": NSNumber(value: messageCount)])
  }

  func logDialogChatDelete(messageCount: Int) {
    guard TrackingConsentManager.shared.isAnalyticsEnabled else { return }
    Analytics.logEvent("dialog_chat_delete", parameters: ["message_count": NSNumber(value: messageCount)])
  }

  func logDialogMessageSend(source: String, textLength: Int? = nil, audioBytes: Int? = nil) {
    guard TrackingConsentManager.shared.isAnalyticsEnabled else { return }
    var parameters: [String: Any] = ["source": source]
    if let textLength {
      parameters["text_length"] = NSNumber(value: textLength)
    }
    if let audioBytes {
      parameters["audio_bytes"] = NSNumber(value: audioBytes)
    }
    Analytics.logEvent("dialog_message_send", parameters: parameters)
  }

  func logDialogRecordStart() {
    guard TrackingConsentManager.shared.isAnalyticsEnabled else { return }
    Analytics.logEvent("dialog_record_start", parameters: nil)
  }

  func logDialogRecordStop() {
    guard TrackingConsentManager.shared.isAnalyticsEnabled else { return }
    Analytics.logEvent("dialog_record_stop", parameters: nil)
  }
}
