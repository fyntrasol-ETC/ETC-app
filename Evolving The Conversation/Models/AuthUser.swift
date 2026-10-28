import Foundation

// MARK: - Auth User Model
struct AuthUser: Equatable {
    let uid: String
    let email: String?
    let displayName: String?
    let provider: AuthProvider

    enum AuthProvider: String {
        case email  = "password"
        case google = "google.com"
        case apple  = "apple.com"
    }
}
