import SwiftUI

// Reimplements Java/Kotlin's String.hashCode() exactly, so the same uid
// produces the same palette index on both iOS and Android. Swift's native
// hashValue is randomized per launch and would never match — don't use it here.
func javaHashCode(_ s: String) -> Int32 {
    var hash: Int32 = 0
    for char in s.utf16 {
        hash = 31 &* hash &+ Int32(char)
    }
    return hash
}

// Must match Android's PALETTE order exactly — same array, same order.
private let colorPalette = ["#2979FF", "#34A853", "#F5A623", "#7C4DFF", "#E53935", "#00ACC1"]

func colorHex(for uid: String) -> String {
    let hash = abs(Int(javaHashCode(uid)))
    return colorPalette[hash % colorPalette.count]
}

// Turns a hex string like "#2979FF" into a SwiftUI Color.
extension Color {
    init?(hex: String) {
        var hexString = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        hexString = hexString.replacingOccurrences(of: "#", with: "")

        var rgbValue: UInt64 = 0
        guard Scanner(string: hexString).scanHexInt64(&rgbValue) else { return nil }

        let r = Double((rgbValue & 0xFF0000) >> 16) / 255
        let g = Double((rgbValue & 0x00FF00) >> 8) / 255
        let b = Double(rgbValue & 0x0000FF) / 255

        self.init(red: r, green: g, blue: b)
    }
}
