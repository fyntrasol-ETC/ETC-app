import SwiftUI

struct DeleteAccountScreen: View {
    @EnvironmentObject private var authVM: AuthViewModel
    @Environment(\.appSizeClass) var sc
    @Environment(\.dismiss) var dismiss

    @State private var password    = ""
    @State private var showConfirm = false

    private var isEmailUser: Bool {
        authVM.currentUser?.provider == .email
    }

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        
        VStack(spacing: 0) {
            ETCScreenBackBar(title: "Delete Account") { dismiss() }
            
            ScrollView(showsIndicators: false) {
                VStack(spacing: pad.lg) {
                    
                    // ── Icon ──────────────────────────────────
                    let circleSize = AppSizing.iconSize(base: 90, sizeClass: sc)
                    ZStack {
                        Circle()
                            .fill(Color.red.opacity(0.10))
                            .frame(width: circleSize, height: circleSize)
                        Image(systemName: "person.crop.circle.badge.minus")
                            .font(.system(
                                size: AppSizing.iconSize(base: 36, sizeClass: sc),
                                weight: .medium
                            ))
                            .foregroundColor(.red)
                    }
                    .padding(.top, pad.md)
                    
                    // ── Header ────────────────────────────────
                    VStack(spacing: 6) {
                        Text("Delete Account")
                            .font(AppTheme.Typography.title1(sc))
                            .fontWeight(.bold)
                            .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                        Text("This action is permanent and cannot be undone. All your data will be lost.")
                            .font(AppTheme.Typography.body(sc))
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                            .multilineTextAlignment(.center)
                    }
                    
                    // ── Error banner ──────────────────────────
                    if let err = authVM.errorMessage { ErrorBanner(message: err) }
                    
                    // ── Password field (email users only) ─────
                    if isEmailUser {
                        ETCTextField(
                            label: "Confirm your password",
                            placeholder: "Enter your password to continue",
                            text: $password,
                            leadingIcon: "lock",
                            isSecure: true
                        )
                    }
                    
                    // ── Warning card ──────────────────────────
                    HStack(spacing: 12) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundColor(.orange)
                        Text("Deleting your account will permanently remove all progress, courses, and personal data.")
                            .font(AppTheme.Typography.subheadline(sc))
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    }
                    .padding(pad.md)
                    .background(Color.orange.opacity(0.08))
                    .cornerRadius(AppSizing.cardRadius(sc))
                    .overlay(
                        RoundedRectangle(cornerRadius: AppSizing.cardRadius(sc))
                            .stroke(Color.orange.opacity(0.25), lineWidth: 1)
                    )
                    
                    // ── Delete button ─────────────────────────
                    ETCButton(
                        title: "Delete My Account",
                        style: .destructive,
                        isLoading: authVM.isLoading
                    ) {
                        showConfirm = true
                    }
                    
                    ETCButton(title: "Cancel", style: .secondary) { dismiss() }
                    
                    Spacer(minLength: pad.xl)
                }
                .padding(.horizontal, pad.lg)
                .frame(maxWidth: AppSizing.maxContentWidth(sc))
                .frame(maxWidth: .infinity)
            }
        }
        .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
        .navigationBarHidden(true)
        .confirmationDialog(
            "Are you absolutely sure?",
            isPresented: $showConfirm,
            titleVisibility: .visible
        ) {
            Button("Delete Account", role: .destructive) {
                Task { await authVM.deleteAccount(password: password) }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This cannot be undone.")
        }
        
    }
}
