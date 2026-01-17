import Foundation
import Shared

final class SharedSdkProvider {
    static let shared = SharedSdkProvider()

    let sdk: SharedSdk

    private init() {
        sdk = SharedSdk(baseUrl: "https://backend.linka.su", platformContext: IosPlatformContext())
    }
}
