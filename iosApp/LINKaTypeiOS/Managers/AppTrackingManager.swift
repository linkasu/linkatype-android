import Foundation
import AppTrackingTransparency
import AdSupport

class AppTrackingManager {
  static let shared = AppTrackingManager()
  
  private init() {}
  
  func requestTrackingAuthorization() async -> Bool {
    if #available(iOS 14, *) {
      let status = await ATTrackingManager.requestTrackingAuthorization()
      return status == .authorized
    }
    return false
  }
  
  func getTrackingAuthorizationStatus() -> ATTrackingManager.AuthorizationStatus {
    if #available(iOS 14, *) {
      return ATTrackingManager.trackingAuthorizationStatus
    }
    return .notDetermined
  }
}




