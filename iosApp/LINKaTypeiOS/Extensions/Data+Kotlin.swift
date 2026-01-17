import Foundation
import Shared

extension Data {
    func toKotlinByteArray() -> KotlinByteArray {
        let bytes = [UInt8](self)
        let array = KotlinByteArray(size: Int32(bytes.count))
        for (index, byte) in bytes.enumerated() {
            array.set(index: Int32(index), value: Int8(bitPattern: byte))
        }
        return array
    }
}
