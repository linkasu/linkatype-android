import Foundation
import FirebaseAnalytics

class FirebaseAnalyticsManager {
  static let shared = FirebaseAnalyticsManager()
  
  private init() {}
  
  func logSayEvent() {
    Analytics.logEvent("say", parameters: nil)
  }
  
  func logSpotlightEvent() {
    Analytics.logEvent("spotlight", parameters: nil)
  }
  
  func logDownloadCategoryCacheEvent() {
    Analytics.logEvent("download_category_cache", parameters: nil)
  }
}

