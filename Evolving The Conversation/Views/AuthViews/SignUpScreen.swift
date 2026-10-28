import SwiftUI

// MARK: - Sign Up Screen
struct SignUpScreen: View {
    @EnvironmentObject private var authVM: AuthViewModel

    @State private var fullName = ""
    @State private var email    = ""
    @State private var password = ""
    @State private var confirm  = ""
    @State private var accepted = false

    // Local validation error (before hitting Firebase)
    @State private var localError: String?

    @Environment(\.appSizeClass) var sc

    var onBack:    () -> Void = {}
    var onTerms:   () -> Void = {}
    var onPrivacy: () -> Void = {}

    private var backBtnSize: CGFloat {
        switch sc {
        case .compact: return 38
        case .regular: return 52
        case .large:   return 60
        }
    }

    private var checkboxSize: CGFloat {
        switch sc {
        case .compact: return 22
        case .regular: return 30
        case .large:   return 36
        }
    }

    // Shown error: local validation takes priority over Firebase error
    private var displayedError: String? { localError ?? authVM.errorMessage }

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        ScrollView {
            VStack(spacing: pad.lg) {

                // ── Back button ───────────────────────────
                HStack {
                    ETCBackButton(title: "") {
                        onBack()
                    }

                    Spacer()
                }
                .padding(.top, pad.sm)

                // ── Header ────────────────────────────────
                VStack(alignment: .leading, spacing: 8) {
                    Text("Create account")
                        .font(AppTheme.Typography.title1(sc))
                        .fontWeight(.bold)
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    Text("Start learning English with confidence.")
                        .font(AppTheme.Typography.body(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                // ── Error banner ──────────────────────────
                if let error = displayedError {
                    ErrorBanner(message: error)
                }

                // ── Fields ────────────────────────────────
                VStack(spacing: pad.md) {
                    ETCTextField(label: "Full name",
                                 placeholder: "Jane Cooper",
                                 text: $fullName,
                                 leadingIcon: "person")
                    ETCTextField(label: "Email",
                                 placeholder: "you@example.com",
                                 text: $email,
                                 leadingIcon: "envelope")
                    ETCTextField(label: "Password",
                                 placeholder: "Create a password",
                                 text: $password,
                                 leadingIcon: "lock",
                                 isSecure: true)
                    ETCTextField(label: "Confirm password",
                                 placeholder: "Re-enter password",
                                 text: $confirm,
                                 leadingIcon: "lock",
                                 isSecure: true)
                }

                // ── Terms checkbox ────────────────────────
                HStack(alignment: .center, spacing: pad.sm) {
                    Button {
                        withAnimation { accepted.toggle() }
                    } label: {
                        RoundedRectangle(cornerRadius: checkboxSize * 0.22)
                            .stroke(
                                accepted ? AppTheme.Colors.primaryFallback
                                         : AppTheme.Colors.borderFallback,
                                lineWidth: 1.5
                            )
                            .background(
                                RoundedRectangle(cornerRadius: checkboxSize * 0.22)
                                    .fill(accepted ? AppTheme.Colors.primaryFallback : Color.clear)
                            )
                            .frame(width: checkboxSize, height: checkboxSize)
                            .overlay(
                                Image(systemName: "checkmark")
                                    .font(.system(
                                        size: AppSizing.fontSize(base: 12, sizeClass: sc),
                                        weight: .bold
                                    ))
                                    .foregroundColor(.white)
                                    .opacity(accepted ? 1 : 0)
                            )
                    }

                    HStack(spacing: 4) {
                        Text("I accept the")
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        Button("Terms") { onTerms() }
                            .fontWeight(.semibold)
                            .foregroundColor(AppTheme.Colors.primaryFallback)
                        Text("&")
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        Button("Privacy Policy") { onPrivacy() }
                            .fontWeight(.semibold)
                            .foregroundColor(AppTheme.Colors.primaryFallback)
                    }
                    .font(AppTheme.Typography.subheadline(sc))
                }

                // ── Create account button ─────────────────
                ETCButton(title: "Create account", isLoading: authVM.isLoading) {
                    localError = validate()
                    guard localError == nil, accepted else { return }
                    Task { await authVM.signUp(fullName: fullName, email: email, password: password) }
                }
                .padding(.bottom, pad.xl)
            }
            .padding(.horizontal, pad.lg)
            .frame(maxWidth: AppSizing.maxContentWidth(sc))
            .frame(maxWidth: .infinity)
        }
        .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
    }

    // ── Local validation ─────────────────────────────────────────────
    private func validate() -> String? {
        if fullName.trimmingCharacters(in: .whitespaces).isEmpty {
            return NSLocalizedString("Please enter your full name.", comment: "")
        }
        if !email.contains("@") {
            return NSLocalizedString("Please enter a valid email address.", comment: "")
        }
        if password.count < 8 {
            return NSLocalizedString("Password must be at least 8 characters.", comment: "")
        }
        if !password.contains(where: { $0.isNumber }) || !password.contains(where: { $0.isLetter }) {
            return NSLocalizedString("Password must contain at least one letter and one number.", comment: "")
        }
        if password != confirm {
            return NSLocalizedString("Passwords don't match.", comment: "")
        }
        if !accepted {
            return NSLocalizedString("Please accept the Terms & Privacy Policy.", comment: "")
        }
        return nil
    }
}
