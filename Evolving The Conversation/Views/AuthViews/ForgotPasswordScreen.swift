import SwiftUI

// MARK: - Forgot Password Screen
struct ForgotPasswordScreen: View {
    @State private var email     = ""
    @State private var isLoading = false
    @State private var sent      = false

    @Environment(\.appSizeClass) var sc
    var onBack: () -> Void = {}

    // Scaled back-button tap target
    private var backBtnSize: CGFloat {
        switch sc {
        case .compact: return 38
        case .regular: return 52
        case .large:   return 60
        }
    }

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        ScrollView {
            VStack(spacing: pad.lg) {

                // ── Back button ───────────────────────────
                HStack {
                    Button { onBack() } label: {
                        Image(systemName: "chevron.left")
                            .font(.system(
                                size: AppSizing.fontSize(base: 16, sizeClass: sc),
                                weight: .semibold
                            ))
                            .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                            .frame(width: backBtnSize, height: backBtnSize)
                            .background(AppTheme.Colors.surface)
                            .cornerRadius(backBtnSize * 0.26)
                            .shadow(color: .black.opacity(0.06), radius: 4, y: 2)
                    }
                    Spacer()
                }
                .padding(.top, pad.sm)

                // ── Lock icon ─────────────────────────────
                let circleSize = AppSizing.iconSize(base: 110, sizeClass: sc)
                let iconSize   = AppSizing.iconSize(base: 44,  sizeClass: sc)

                ZStack {
                    Circle()
                        .fill(AppTheme.Colors.primaryLightFallback)
                        .frame(width: circleSize, height: circleSize)
                    Image(systemName: "lock.rotation")
                        .font(.system(size: iconSize, weight: .medium))
                        .foregroundColor(AppTheme.Colors.primaryFallback)
                }
                .padding(.top, pad.md)

                // ── Header ────────────────────────────────
                VStack(spacing: 8) {
                    Text("Forgot Password?")
                        .font(AppTheme.Typography.title1(sc))
                        .fontWeight(.bold)
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    Text("Enter your email and we'll send you a reset link.")
                        .font(AppTheme.Typography.body(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        .multilineTextAlignment(.center)
                        .fixedSize(horizontal: false, vertical: true)
                }

                // ── Form or success state ─────────────────
                if sent {
                    VStack(spacing: pad.md) {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: AppSizing.iconSize(base: 56, sizeClass: sc)))
                            .foregroundColor(AppTheme.Colors.greenFallback)

                        Text("Reset link sent!")
                            .font(AppTheme.Typography.headline(sc))
                            .foregroundColor(AppTheme.Colors.textPrimaryFallback)

                        Text("Check your inbox and follow the link to reset your password.")
                            .font(AppTheme.Typography.body(sc))
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                            .multilineTextAlignment(.center)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .padding(pad.lg)
                    .background(AppTheme.Colors.greenFallback.opacity(0.08))
                    .cornerRadius(AppSizing.cardRadius(sc))

                } else {
                    ETCTextField(
                        label: "Email",
                        placeholder: "you@example.com",
                        text: $email,
                        leadingIcon: "envelope"
                    )

                    ETCButton(title: "Send Reset Link", isLoading: isLoading) {
                        isLoading = true
                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
                            isLoading = false
                            withAnimation { sent = true }
                        }
                    }
                }

                Spacer(minLength: pad.xl)
            }
            .padding(.horizontal, pad.lg)    // consistent with LoginScreen
            .frame(maxWidth: AppSizing.maxContentWidth(sc))
            .frame(maxWidth: .infinity)
        }
        .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
    }
}
