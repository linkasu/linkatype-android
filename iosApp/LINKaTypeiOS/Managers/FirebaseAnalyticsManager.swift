import Foundation
import FirebaseAnalytics

class FirebaseAnalyticsManager {
  static let shared = FirebaseAnalyticsManager()
  
  private init() {}
  
  func logSayEvent() {
    guard TrackingConsentManager.shared.isTrackingEnabled else { return }
    Analytics.logEvent("say", parameters: nil)
  }
  
  func logSpotlightEvent() {
    guard TrackingConsentManager.shared.isTrackingEnabled else { return }
    Analytics.logEvent("spotlight", parameters: nil)
  }
  
  func logDownloadCategoryCacheEvent() {
    guard TrackingConsentManager.shared.isTrackingEnabled else { return }
    Analytics.logEvent("download_category_cache", parameters: nil)
  }
}
