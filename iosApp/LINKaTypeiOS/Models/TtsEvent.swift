import Foundation

enum TtsEvent {
    case speakingStarted
    case speakingCompleted
    case error(String)
    case temporarilyUnavailable(String)
    case status(String)
    case downloadStarted
    case downloadProgress(current: Int, total: Int)
    case downloadCompleted(path: String?)
}
