import Foundation

// Android owns the Metrics V2 rollout. iOS keeps this closed no-op sink until
// its Keychain-backed installation transport is available.
class TelemetryManager {
  static let shared = TelemetryManager()
  
  private init() {}
  
  func logSayEvent() {}
  
  func logSpotlightEvent() {}
  
  func logDownloadCategoryCacheEvent() {}

  func logRealtimeSyncEvent(changesCount: Int) {}

  func logRealtimeSyncError(message: String) {}

  func logDialogOpened() {}

  func logDialogClosed() {}

  func logDialogChatCreate() {}

  func logDialogChatSelect(messageCount: Int) {}

  func logDialogChatDelete(messageCount: Int) {}

  func logDialogMessageSend(source: String, textLength: Int? = nil, audioBytes: Int? = nil) {}

  func logDialogRecordStart() {}

  func logDialogRecordStop() {}
}
