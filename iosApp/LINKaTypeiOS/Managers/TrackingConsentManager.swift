import Foundation
import FirebaseAnalytics
import Combine

class TrackingConsentManager: ObservableObject {
  static let shared = TrackingConsentManager()
  
  @Published private(set) var isAnalyticsEnabled: Bool
  
  private let userDefaultsKey = "analytics_enabled"
  private let userDefaults: UserDefaults
  
  private init(userDefaults: UserDefaults = .standard) {
    self.userDefaults = userDefaults
    
    if let storedValue = userDefaults.object(forKey: userDefaultsKey) as? Bool {
      self.isAnalyticsEnabled = storedValue
      Analytics.setAnalyticsCollectionEnabled(storedValue)
    } else {
      self.isAnalyticsEnabled = true
      userDefaults.set(true, forKey: userDefaultsKey)
      Analytics.setAnalyticsCollectionEnabled(true)
    }
  }
  
  func setAnalyticsEnabled(_ enabled: Bool) {
    guard enabled != isAnalyticsEnabled else { return }
    
    userDefaults.set(enabled, forKey: userDefaultsKey)
    isAnalyticsEnabled = enabled
    Analytics.setAnalyticsCollectionEnabled(enabled)
  }
}
