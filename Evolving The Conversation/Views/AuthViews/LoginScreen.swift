import SwiftUI

// MARK: - Login Screen
struct LoginScreen: View {
    @EnvironmentObject private var authVM: AuthViewModel

    @State private var email    = ""
    @State private var password = ""

    @Environment(\.appSizeClass) var sc

    var onForgotPassword: () -> Void = {}
    var onCreateAccount:  () -> Void = {}

    var body: some View {
        let pad    = AppSizing.Padding.forClass(sc)
        let iconSz = AppSizing.appIconSize(sc)

        ScrollView {
            VStack(spacing: pad.lg) {

                // ── App Icon ──────────────────────────────
                Image("logo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: iconSz, height: iconSz)
                    .clipShape(RoundedRectangle(cornerRadius: iconSz * 0.22))
                    .shadow(color: AppTheme.Colors.primaryFallback.opacity(0.3),
                            radius: 14, y: 5)
                    .padding(.top, pad.xl)

                // ── Header ────────────────────────────────
                VStack(spacing: 8) {
                    Text("Welcome back")
                        .font(AppTheme.Typography.title1(sc))
                        .fontWeight(.bold)
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    Text("Log in to continue your learning journey.")
                        .font(AppTheme.Typography.body(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        .multilineTextAlignment(.center)
                }

                // ── Error banner ──────────────────────────
                if let error = authVM.errorMessage {
                    ErrorBanner(message: error)
                }

                // ── Fields ────────────────────────────────
                VStack(spacing: pad.md) {
                    ETCTextField(label: "Email",
                                 placeholder: "you@example.com",
                                 text: $email,
                                 leadingIcon: "envelope")
                    ETCTextField(label: "Password",
                                 placeholder: "Enter your password",
                                 text: $password,
                                 leadingIcon: "lock",
                                 isSecure: true)
                }

                // ── Forgot ────────────────────────────────
                HStack {
                    Spacer()
                    Button("Forgot password?") { onForgotPassword() }
                        .font(AppTheme.Typography.subheadline(sc))
                        .fontWeight(.semibold)
                        .foregroundColor(AppTheme.Colors.primaryFallback)
                }

                // ── Login button ──────────────────────────
                ETCButton(title: "Log in", isLoading: authVM.isLoading) {
                    Task { await authVM.signIn(email: email, password: password) }
                }

                // ── Divider ───────────────────────────────
                ETCDividerRow(label: "or continue with")

                // ── Social buttons ────────────────────────
                HStack(spacing: pad.sm) {
                    ETCSocialButton(title: "Google", icon: "google", isSystemImage: false, iconSize: 28) {
                        Task { await authVM.signInWithGoogle() }
                    }
                    ETCSocialButton(title: "Apple", icon: "apple", isSystemImage: false) {
                        authVM.signInWithApple()
                    }
                }

                // ── Create account ────────────────────────
                HStack(spacing: 4) {
                    Text("New here?")
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    Button("Create account") { onCreateAccount() }
                        .fontWeight(.semibold)
                        .foregroundColor(AppTheme.Colors.primaryFallback)
                }
                .font(AppTheme.Typography.subheadline(sc))
                .padding(.bottom, pad.xl)
            }
            .padding(.horizontal, pad.lg)
            .frame(maxWidth: AppSizing.maxContentWidth(sc))
            .frame(maxWidth: .infinity)
        }
        .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
    }
}
