import Foundation
import FirebaseAnalytics
import Combine

class TrackingConsentManager: ObservableObject {
  static let shared = TrackingConsentManager()
  
  @Published private(set) var isTrackingEnabled: Bool
  
  private let userDefaultsKey = "tracking_consent_enabled"
  private let attRequestedKey = "att_request_shown"
  private let userDefaults: UserDefaults
  
  private init(userDefaults: UserDefaults = .standard) {
    self.userDefaults = userDefaults
    
    if let storedValue = userDefaults.object(forKey: userDefaultsKey) as? Bool {
      self.isTrackingEnabled = storedValue
      Analytics.setAnalyticsCollectionEnabled(storedValue)
    } else {
      self.isTrackingEnabled = false
      userDefaults.set(false, forKey: userDefaultsKey)
      Analytics.setAnalyticsCollectionEnabled(false)
    }
  }
  
  var hasShownATTRequest: Bool {
    return userDefaults.bool(forKey: attRequestedKey)
  }
  
  func markATTRequestShown() {
    userDefaults.set(true, forKey: attRequestedKey)
  }
  
  func setTrackingEnabled(_ enabled: Bool) {
    guard enabled != isTrackingEnabled else { return }
    
    userDefaults.set(enabled, forKey: userDefaultsKey)
    isTrackingEnabled = enabled
    Analytics.setAnalyticsCollectionEnabled(enabled)
  }
}
