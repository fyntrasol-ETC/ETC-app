import SwiftUI
import FirebaseAuth

struct ChangePasswordScreen: View {
    @EnvironmentObject private var authVM: AuthViewModel
    @Environment(\.appSizeClass) var sc
    @Environment(\.dismiss) var dismiss

    @State private var current   = ""
    @State private var newPass   = ""
    @State private var confirm   = ""
    @State private var isLoading = false
    @State private var success   = false
    @State private var errorMsg: String?

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        VStack(spacing: 0) {
            ETCScreenBackBar(title: NSLocalizedString("Change Password", comment: "")) { dismiss() }

            ScrollView(showsIndicators: false) {
                VStack(spacing: pad.lg) {

                    // ── Icon ──────────────────────────────────
                    let circleSize = AppSizing.iconSize(base: 90, sizeClass: sc)
                    ZStack {
                        Circle()
                            .fill(AppTheme.Colors.primaryLightFallback)
                            .frame(width: circleSize, height: circleSize)
                        Image(systemName: "lock.rotation")
                            .font(.system(
                                size: AppSizing.iconSize(base: 36, sizeClass: sc),
                                weight: .medium
                            ))
                            .foregroundColor(AppTheme.Colors.primaryFallback)
                    }
                    .padding(.top, pad.md)

                    // ── Header ────────────────────────────────
                    VStack(spacing: 6) {
                        Text(NSLocalizedString("Change Password", comment: ""))
                            .font(AppTheme.Typography.title1(sc))
                            .fontWeight(.bold)
                            .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                        Text(NSLocalizedString("Enter your current password, then choose a new one.", comment: ""))
                            .font(AppTheme.Typography.body(sc))
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                            .multilineTextAlignment(.center)
                    }

                    if success {
                        // ── Success state ─────────────────────
                        VStack(spacing: pad.md) {
                            Image(systemName: "checkmark.circle.fill")
                                .font(.system(size: AppSizing.iconSize(base: 56, sizeClass: sc)))
                                .foregroundColor(AppTheme.Colors.greenFallback)
                            Text(NSLocalizedString("Password updated!", comment: ""))
                                .font(AppTheme.Typography.headline(sc))
                                .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                        }
                        .padding(pad.lg)
                        .frame(maxWidth: .infinity)
                        .background(AppTheme.Colors.greenFallback.opacity(0.08))
                        .cornerRadius(AppSizing.cardRadius(sc))

                        ETCButton(title: NSLocalizedString("Done", comment: "")) { dismiss() }

                    } else {
                        // ── Error banner ──────────────────────
                        if let err = errorMsg { ErrorBanner(message: err) }

                        // ── Fields ────────────────────────────
                        VStack(spacing: pad.md) {
                            ETCTextField(
                                label: NSLocalizedString("Current password", comment: ""),
                                placeholder: NSLocalizedString("Enter current password", comment: ""),
                                text: $current,
                                leadingIcon: "lock",
                                isSecure: true
                            )
                            ETCTextField(
                                label: NSLocalizedString("New password", comment: ""),
                                placeholder: NSLocalizedString("At least 6 characters", comment: ""),
                                text: $newPass,
                                leadingIcon: "lock.open",
                                isSecure: true
                            )
                            ETCTextField(
                                label: NSLocalizedString("Confirm new password", comment: ""),
                                placeholder: NSLocalizedString("Re-enter new password", comment: ""),
                                text: $confirm,
                                leadingIcon: "lock.open",
                                isSecure: true
                            )
                        }

                        ETCButton(title: NSLocalizedString("Update Password", comment: ""), isLoading: isLoading) {
                            Task { await changePassword() }
                        }

                        ETCButton(title: NSLocalizedString("Cancel", comment: ""), style: .secondary) { dismiss() }
                    }

                    Spacer(minLength: pad.xl)
                }
                .padding(.horizontal, pad.lg)
                .frame(maxWidth: AppSizing.maxContentWidth(sc))
                .frame(maxWidth: .infinity)
            }
        }
        .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
        .navigationBarHidden(true)
    }

    private func changePassword() async {
        errorMsg = nil
        if current.isEmpty { errorMsg = NSLocalizedString("Please enter your current password.", comment: ""); return }
        if newPass.count < 8 { errorMsg = NSLocalizedString("New password must be at least 8 characters.", comment: ""); return }
        if !newPass.contains(where: { $0.isNumber }) || !newPass.contains(where: { $0.isLetter }) {
            errorMsg = NSLocalizedString("New password must contain at least one letter and one number.", comment: ""); return
        }
        if newPass != confirm { errorMsg = NSLocalizedString("Passwords don't match.", comment: ""); return }

        isLoading = true
        defer { isLoading = false }

        guard let user = Auth.auth().currentUser, let email = user.email else {
            errorMsg = NSLocalizedString("Could not find your account.", comment: ""); return
        }

        let credential = EmailAuthProvider.credential(withEmail: email, password: current)
        do {
            try await user.reauthenticate(with: credential)
            try await user.updatePassword(to: newPass)
            withAnimation { success = true }
        } catch let e as NSError {
            switch AuthErrorCode(rawValue: e.code) {
            case .wrongPassword: errorMsg = NSLocalizedString("Current password is incorrect.", comment: "")
            case .weakPassword:  errorMsg = NSLocalizedString("New password is too weak.", comment: "")
            default:             errorMsg = e.localizedDescription
            }
        }
    }
}
