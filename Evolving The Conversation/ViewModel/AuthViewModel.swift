import SwiftUI
import FirebaseAuth
import FirebaseCore
import FirebaseFirestore
import GoogleSignIn
import AuthenticationServices
import CryptoKit
import Combine
// MARK: - Auth State
enum AuthState: Equatable {
    case unknown
    case signedOut
    case signedIn(AuthUser)
}

// MARK: - Auth Error
enum AuthError: LocalizedError {
    case missingClientID
    case noRootViewController
    case invalidCredential
    case appleSignInFailed(String)
    case googleSignInFailed(String)
    case emailSignInFailed(String)
    case signUpFailed(String)
    case unknown

    var errorDescription: String? {
        switch self {
        case .missingClientID:               return "Firebase client ID not found."
        case .noRootViewController:          return "Could not find a root view controller."
        case .invalidCredential:             return "Invalid credentials. Please try again."
        case .appleSignInFailed(let msg):    return msg
        case .googleSignInFailed(let msg):   return msg
        case .emailSignInFailed(let msg):    return msg
        case .signUpFailed(let msg):         return msg
        case .unknown:                       return "An unknown error occurred."
        }
    }
}

// MARK: - Auth ViewModel
@MainActor
final class AuthViewModel: NSObject, ObservableObject {

    @Published var authState: AuthState = .unknown
    @Published var isLoading            = false
    @Published var errorMessage: String?
    @Published var isDarkMode: Bool = false

    // Apple Sign-In nonce
    private var currentNonce: String?

    private var stateListener: AuthStateDidChangeListenerHandle?

    // Guards against the auth-state listener publishing a transient
    // "signed in with no displayName yet" state during sign-up. Without
    // this, createUser() fires the listener immediately (before the
    // profile change request with the display name has been committed),
    // so HomeScreen briefly falls back to showing the email instead of
    // the name until signUp() finishes and overwrites authState with the
    // real name a moment later — that's the visible "flash."
    private var isPerformingSignUp = false

    override init() {
        super.init()
        loadDeviceDarkMode()   // ← add this line
        listenToAuthState()
    }

    deinit {
        if let handle = stateListener {
            Auth.auth().removeStateDidChangeListener(handle)
        }
    }
    // Add this computed property so HomeScreen can read the current user
    var currentUser: AuthUser? {
        if case .signedIn(let user) = authState { return user }
        return nil
    }
    // ✅ fixed
    private let deviceDarkModeKey = "etc_darkMode_device"
    private func darkModeKey(for uid: String) -> String { "etc_darkMode_\(uid)" }

    /// Called on launch, before any auth state is known, so the auth
    /// screens (Login/SignUp/ForgotPassword) show the right appearance
    /// even with nobody signed in yet.
    func loadDeviceDarkMode() {
        isDarkMode = UserDefaults.standard.bool(forKey: deviceDarkModeKey)
    }

    /// Called once a user is signed in — per-user value takes over,
    /// falling back to whatever the device-level value already was.
    func loadDarkMode(for uid: String) {
        let key = darkModeKey(for: uid)
        if UserDefaults.standard.object(forKey: key) != nil {
            isDarkMode = UserDefaults.standard.bool(forKey: key)
        } else {
            isDarkMode = UserDefaults.standard.bool(forKey: deviceDarkModeKey)
        }
    }

    func saveDarkMode(_ value: Bool) {
        isDarkMode = value
        UserDefaults.standard.set(value, forKey: deviceDarkModeKey)
        guard let uid = currentUser?.uid else { return }
        UserDefaults.standard.set(value, forKey: darkModeKey(for: uid))
    }
    // ── Auth state listener ──────────────────────────────────────────
    private func listenToAuthState() {
        stateListener = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            guard let self else { return }
            // in listenToAuthState(), inside the `if let user` branch:
            if let user {
                let mapped = self.mapFirebaseUser(user)
                // During sign-up, skip publishing this intermediate state —
                // signUp() will publish the final one itself once the
                // display name has actually been committed.
                if !self.isPerformingSignUp {
                    self.authState = .signedIn(mapped)
                }
                self.loadDarkMode(for: mapped.uid)
                Task { await FirestoreUserService.ensureUserDocument(for: mapped) }
                XPService.shared.start()
                XPService.shared.awardDailyLoginXpIfNeeded()   // ← add this
            } else {
                self.authState = .signedOut
                XPService.shared.reset()   // ← add this
            }
        }
    }

    // ── Email / Password ─────────────────────────────────────────────
    func signIn(email: String, password: String) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do {
            try await Auth.auth().signIn(withEmail: email, password: password)
        } catch let e as NSError {
            errorMessage = AuthError.emailSignInFailed(friendlyMessage(e)).errorDescription
        }
    }
    private func mapFirebaseUser(_ user: FirebaseAuth.User) -> AuthUser {
        let providerID = user.providerData.first?.providerID ?? "password"
        return AuthUser(
            uid:         user.uid,
            email:       user.email,
            displayName: user.displayName,
            provider:    AuthUser.AuthProvider(rawValue: providerID) ?? .email
        )
    }
    func signUp(fullName: String, email: String, password: String) async {
        isLoading = true
        errorMessage = nil
        isPerformingSignUp = true
        defer {
            isLoading = false
            isPerformingSignUp = false
        }
        do {
            let result = try await Auth.auth().createUser(withEmail: email, password: password)
            let changeRequest = result.user.createProfileChangeRequest()
            changeRequest.displayName = fullName
            try await changeRequest.commitChanges()
            
            // ← Force reload so the auth state listener picks up the updated displayName
            try await result.user.reload()
            
            // ← Manually update authState immediately so UI shows name right away.
            // Because isPerformingSignUp was true this whole time, the listener's
            // own (blank-name) update above was skipped, so this is the *first*
            // time authState goes to .signedIn — no flash to the email fallback.
            let updated = mapFirebaseUser(result.user)
            self.authState = .signedIn(updated)
            
        } catch let e as NSError {
            errorMessage = AuthError.signUpFailed(friendlyMessage(e)).errorDescription
        }
    }   // ── Google Sign-In ───────────────────────────────────────────────
    func signInWithGoogle() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        guard let clientID = FirebaseApp.app()?.options.clientID else {
            errorMessage = AuthError.missingClientID.errorDescription
            return
        }

        let config = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.configuration = config

        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootVC = windowScene.windows.first?.rootViewController else {
            errorMessage = AuthError.noRootViewController.errorDescription
            return
        }

        do {
            let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: rootVC)
            guard let idToken = result.user.idToken?.tokenString else {
                errorMessage = AuthError.invalidCredential.errorDescription
                return
            }
            let credential = GoogleAuthProvider.credential(
                withIDToken: idToken,
                accessToken: result.user.accessToken.tokenString
            )
            try await Auth.auth().signIn(with: credential)
        } catch let e as NSError {
            errorMessage = AuthError.googleSignInFailed(friendlyMessage(e)).errorDescription
        }
    }

    // ── Apple Sign-In ────────────────────────────────────────────────
    func signInWithApple() {
        errorMessage = nil
        let nonce = randomNonceString()
        currentNonce = nonce

        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = sha256(nonce)

        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }
 
    // ── Sign Out ─────────────────────────────────────────────────────
    func signOut() {
        LessonProgressService.shared.reset()
        DailyChallengeService.shared.reset()
        CertificateService.shared.reset()
        XPService.shared.reset()   // ← add this

        try? Auth.auth().signOut()
        GIDSignIn.sharedInstance.signOut()
    }
    // ── Delete Account ───────────────────────────────────────────────
        func deleteAccount(password: String) async {
            errorMessage = nil
            isLoading = true
            defer { isLoading = false }

            guard let user = Auth.auth().currentUser else {
                errorMessage = "Could not find your account."; return
            }

            let isEmailUser = user.providerData.first?.providerID == "password"

            do {
                if isEmailUser {
                    guard !password.isEmpty else {
                        errorMessage = "Please enter your password."; return
                    }
                    guard let email = user.email else { return }
                    let credential = EmailAuthProvider.credential(withEmail: email, password: password)
                    try await user.reauthenticate(with: credential)
                }

                // Delete Firestore doc FIRST — must happen while still authenticated,
                // since the delete rule checks request.auth.uid == uid.
                let uid = user.uid
                try await Firestore.firestore().collection("users").document(uid).delete()

                // Then delete the Auth account.
                try await user.delete()
                LessonProgressService.shared.reset()   // ← add this
                DailyChallengeService.shared.reset()
                CertificateService.shared.reset()
                XPService.shared.reset()   // ← add this
                
            } catch let e as NSError {
                switch AuthErrorCode(rawValue: e.code) {
                case .wrongPassword:       errorMessage = "Incorrect password."
                case .requiresRecentLogin: errorMessage = "Please log out and log back in before deleting your account."
                default:                   errorMessage = e.localizedDescription
                }
            }
        }

    private func friendlyMessage(_ error: NSError) -> String {
        switch AuthErrorCode(rawValue: error.code) {
        case .wrongPassword, .invalidEmail:
            return "Incorrect email or password."
        case .emailAlreadyInUse:
            return "An account with this email already exists."
        case .weakPassword:
            return "Password must be at least 6 characters."
        case .userNotFound:
            return "No account found with that email."
        case .networkError:
            return "Network error. Please check your connection."
        default:
            return error.localizedDescription
        }
    }

    // ── Nonce helpers (Apple Sign-In) ────────────────────────────────
    private func randomNonceString(length: Int = 32) -> String {
        let charset = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        var result  = ""
        var remainingLength = length
        while remainingLength > 0 {
            let randoms: [UInt8] = (0..<16).map { _ in
                var random: UInt8 = 0
                _ = SecRandomCopyBytes(kSecRandomDefault, 1, &random)
                return random
            }
            randoms.forEach { random in
                if remainingLength == 0 { return }
                if random < charset.count {
                    result.append(charset[Int(random)])
                    remainingLength -= 1
                }
            }
        }
        return result
    }

    private func sha256(_ input: String) -> String {
        let data   = Data(input.utf8)
        let hashed = SHA256.hash(data: data)
        return hashed.compactMap { String(format: "%02x", $0) }.joined()
    }
}

// MARK: - Apple Sign-In Delegates
extension AuthViewModel: ASAuthorizationControllerDelegate {
    nonisolated func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        guard
            let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential,
            let tokenData = appleIDCredential.identityToken,
            let tokenString = String(data: tokenData, encoding: .utf8)
        else {
            Task { @MainActor in self.errorMessage = "Apple Sign-In failed." }
            return
        }

        let fullName = appleIDCredential.fullName

        // Jump to MainActor to safely read currentNonce
        Task { @MainActor in
            guard let nonce = self.currentNonce else {
                self.errorMessage = "Apple Sign-In failed: missing nonce."
                return
            }

            let credential = OAuthProvider.appleCredential(
                withIDToken: tokenString,
                rawNonce: nonce,
                fullName: fullName
            )

            self.isLoading = true
            defer { self.isLoading = false }
            do {
                try await Auth.auth().signIn(with: credential)
            } catch let e as NSError {
                self.errorMessage = self.friendlyMessage(e)
            }
        }
    }
}
extension AuthViewModel: ASAuthorizationControllerPresentationContextProviding {
    nonisolated func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow } ?? UIWindow()
    }
}
