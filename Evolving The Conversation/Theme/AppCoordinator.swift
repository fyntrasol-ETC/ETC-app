import SwiftUI

// ─────────────────────────────────────────────────────────────
// MARK: - App Coordinator
// ─────────────────────────────────────────────────────────────

private enum LaunchKey {
    static let hasOnboarded = "etc_hasOnboarded"
}

struct AppCoordinator: View {
    @AppStorage(LaunchKey.hasOnboarded) private var hasOnboarded = false
    @StateObject private var authVM = AuthViewModel()
    @StateObject private var teacherVM = TeacherAuthViewModel()   // ← add this
    @StateObject private var languageVM = LanguageManager()
    @State private var showingSplash = true

    var body: some View {
        Group {
            if showingSplash {
                SplashScreen()
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            withAnimation(.easeInOut(duration: 0.5)) {
                                showingSplash = false
                            }
                        }
                    }

            } else if !hasOnboarded {
                OnboardingFlow(
                        onFinished: { hasOnboarded = true },
                        onSkip:     { hasOnboarded = true }
                    )
            } else {
                switch authVM.authState {
                case .unknown:
                    Color(red: 0.05, green: 0.08, blue: 0.22).ignoresSafeArea()
                case .signedOut:
                    AuthFlow()
                case .signedIn:
                    MainTabView()
                }
            }
        }
        .id(languageVM.currentLanguage)
        .environment(\.locale, Locale(identifier: languageVM.currentLanguage))   // ← ADD THIS LINE
        .environmentObject(languageVM)
        .environmentObject(authVM)
        .environmentObject(teacherVM)
        .appSizeClass()
        .preferredColorScheme(authVM.isDarkMode ? .dark : .light)  // ← this one line does it all
    }
}
// ─────────────────────────────────────────────────────────────
// MARK: - Auth Flow  (Login ↔ SignUp ↔ ForgotPassword)
// ─────────────────────────────────────────────────────────────

private enum AuthScreen {
    case login
    case signUp
    case forgotPassword
}

struct AuthFlow: View {
    @State private var screen: AuthScreen = .login

    var body: some View {
        ZStack {
            switch screen {
            case .login:
                LoginScreen(
                    onForgotPassword: { push(.forgotPassword) },
                    onCreateAccount:  { push(.signUp) }
                )
                .transition(.asymmetric(
                    insertion: .move(edge: .leading),
                    removal:   .move(edge: .leading)
                ))

            case .signUp:
                SignUpScreen(
                    onBack: { pop() }
                )
                .transition(.asymmetric(
                    insertion: .move(edge: .trailing),
                    removal:   .move(edge: .trailing)
                ))

            case .forgotPassword:
                ForgotPasswordScreen(
                    onBack: { pop() }
                )
                .transition(.asymmetric(
                    insertion: .move(edge: .trailing),
                    removal:   .move(edge: .trailing)
                ))
            }
        }
        .animation(.easeInOut(duration: 0.28), value: screen)
    }

    private func push(_ s: AuthScreen) { withAnimation { screen = s } }
    private func pop()                 { withAnimation { screen = .login } }
}
