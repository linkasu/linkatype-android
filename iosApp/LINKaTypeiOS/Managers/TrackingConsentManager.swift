import Foundation
import Combine

class TrackingConsentManager: ObservableObject {
  static let shared = TrackingConsentManager()
  
  @Published private(set) var isAnalyticsEnabled: Bool
  
  private let userDefaultsKey = "telemetry_consent_v2"
  private let userDefaults: UserDefaults
  
  private init(userDefaults: UserDefaults = .standard) {
    self.userDefaults = userDefaults
    
    self.isAnalyticsEnabled = userDefaults.string(forKey: userDefaultsKey) == "granted"
  }
  
  func setAnalyticsEnabled(_ enabled: Bool) {
    guard enabled != isAnalyticsEnabled else { return }
    
    userDefaults.set(enabled ? "granted" : "denied", forKey: userDefaultsKey)
    isAnalyticsEnabled = enabled
  }
}
