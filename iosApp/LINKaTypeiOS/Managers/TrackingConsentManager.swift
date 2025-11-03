import Foundation
import FirebaseAnalytics
import Combine

class TrackingConsentManager: ObservableObject {
  static let shared = TrackingConsentManager()
  
  @Published private(set) var isTrackingEnabled: Bool
  
  private let userDefaultsKey = "tracking_consent_enabled"
  private let userDefaults: UserDefaults
  
  private init(userDefaults: UserDefaults = .standard) {
    self.userDefaults = userDefaults
    
    if let storedValue = userDefaults.object(forKey: userDefaultsKey) as? Bool {
      self.isTrackingEnabled = storedValue
      Analytics.setAnalyticsCollectionEnabled(storedValue)
    } else {
      self.isTrackingEnabled = true
      userDefaults.set(true, forKey: userDefaultsKey)
      Analytics.setAnalyticsCollectionEnabled(true)
      Task { @MainActor in
        _ = await AppTrackingManager.shared.requestTrackingAuthorization()
      }
    }
  }
  
  func setTrackingEnabled(_ enabled: Bool) {
    guard enabled != isTrackingEnabled else { return }
    
    userDefaults.set(enabled, forKey: userDefaultsKey)
    isTrackingEnabled = enabled
    Analytics.setAnalyticsCollectionEnabled(enabled)
    
    if enabled {
      Task { @MainActor in
        _ = await AppTrackingManager.shared.requestTrackingAuthorization()
      }
    }
  }
}
