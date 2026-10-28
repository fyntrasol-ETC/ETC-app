import SwiftUI
import ObjectiveC
import Combine
// MARK: - Bundle swizzle (makes NSLocalizedString follow our chosen language, not the device's)

private var bundleKey: UInt8 = 0

class LocalizedBundle: Bundle, @unchecked Sendable {
    override func localizedString(forKey key: String, value: String?, table tableName: String?) -> String {
        guard let path = objc_getAssociatedObject(self, &bundleKey) as? String,
              let bundle = Bundle(path: path) else {
            return super.localizedString(forKey: key, value: value, table: tableName)
        }
        return bundle.localizedString(forKey: key, value: value, table: tableName)
    }
}

extension Bundle {
    static func setLanguage(_ languageCode: String) {
        object_setClass(Bundle.main, LocalizedBundle.self)

        let path = Bundle.main.path(forResource: languageCode, ofType: "lproj")
        objc_setAssociatedObject(Bundle.main, &bundleKey, path, .OBJC_ASSOCIATION_RETAIN)
    }
}

// MARK: - LanguageManager

final class LanguageManager: ObservableObject {
    @Published var currentLanguage: String {
        didSet {
            UserDefaults.standard.set(currentLanguage, forKey: "etc_selectedLanguage")
            Bundle.setLanguage(currentLanguage)
        }
    }

    init() {
        let saved = UserDefaults.standard.string(forKey: "etc_selectedLanguage") ?? "en"
        self.currentLanguage = saved
        Bundle.setLanguage(saved)
    }

    func setLanguage(_ code: String) {
        withAnimation {
            currentLanguage = code
        }
    }
}
