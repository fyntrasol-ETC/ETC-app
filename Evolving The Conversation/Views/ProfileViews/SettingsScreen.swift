import SwiftUI
import SafariServices

struct SettingsScreen: View {
    @EnvironmentObject private var authVM: AuthViewModel
    @EnvironmentObject private var languageVM: LanguageManager
    @Environment(\.appSizeClass) var sc
    @Environment(\.dismiss) var dismiss
    @Binding var showTabBar: Bool

    @State private var pushNotifications = true
    @State private var destination: SettingsDestination? = nil
    @State private var showingLanguageDialog = false
    @State private var pendingLanguage: String = "en"
    @State private var showingPrivacySheet = false

    private let privacyPolicyURL = URL(string: "https://doc-hosting.flycricket.io/etc-privacy-policy/26f8f559-ac8f-4216-aaef-b31a973badb4/privacy")!

    enum SettingsDestination {
        case changePassword, deleteAccount
    }

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        ZStack {
            VStack(spacing: 0) {
                ETCScreenBackBar(title: NSLocalizedString("Settings", comment: "")) { dismiss() }

                ScrollView(showsIndicators: false) {
                    VStack(spacing: pad.md) {

                        SettingsCard {
                            HStack {
                                Spacer()
                                Text(NSLocalizedString("Dark mode", comment: ""))
                                    .font(AppTheme.Typography.body(sc))
                                    .fontWeight(.semibold)
                                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                                Spacer()
                                Toggle("", isOn: Binding(
                                    get: { authVM.isDarkMode },
                                    set: { authVM.saveDarkMode($0) }
                                ))
                                .tint(AppTheme.Colors.primaryFallback)
                                .fixedSize()
                            }
                            .padding(.horizontal, pad.md)
                            .padding(.vertical, pad.md)

                            Divider().padding(.horizontal, pad.md)

                            HStack {
                                Spacer()
                                Text(NSLocalizedString("Push notifications", comment: ""))
                                    .font(AppTheme.Typography.body(sc))
                                    .fontWeight(.semibold)
                                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                                Spacer()
                                Toggle("", isOn: $pushNotifications)
                                    .tint(AppTheme.Colors.primaryFallback)
                                    .fixedSize()
                            }
                            .padding(.horizontal, pad.md)
                            .padding(.vertical, pad.md)

                            Divider().padding(.horizontal, pad.md)

                            Button {
                                pendingLanguage = languageVM.currentLanguage
                                withAnimation(.easeOut(duration: 0.2)) {
                                    showingLanguageDialog = true
                                }
                            } label: {
                                HStack {
                                    Spacer()
                                    Image(systemName: "character.book.closed.fill")
                                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                                    Text(NSLocalizedString("Language", comment: ""))
                                        .font(AppTheme.Typography.body(sc))
                                        .fontWeight(.semibold)
                                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                                    Spacer()
                                    Text(languageVM.currentLanguage == "es"
                                         ? NSLocalizedString("Español", comment: "")
                                         : NSLocalizedString("English", comment: ""))
                                        .font(AppTheme.Typography.body(sc))
                                        .foregroundColor(AppTheme.Colors.primaryFallback)
                                    Image(systemName: "chevron.right")
                                        .font(.caption)
                                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                                }
                                .padding(.horizontal, pad.md)
                                .padding(.vertical, pad.md)
                            }
                        }

                        SettingsCard {
                            SettingsCenteredRow(title: NSLocalizedString("Privacy", comment: "")) {
                                showingPrivacySheet = true
                            }
                            // Security row temporarily removed — uncomment to restore.
                            // Divider().padding(.horizontal, pad.md)
                            // SettingsCenteredRow(title: NSLocalizedString("Security", comment: "")) { }
                            Divider().padding(.horizontal, pad.md)
                            SettingsCenteredRow(title: NSLocalizedString("Change password", comment: "")) {
                                destination = .changePassword
                            }
                        }

                        SettingsCard {
                            SettingsCenteredRow(
                                title: NSLocalizedString("Delete account", comment: ""),
                                tint: Color(red: 0.85, green: 0.2, blue: 0.2)
                            ) {
                                destination = .deleteAccount
                            }
                        }
                    }
                    .padding(pad.lg)
                    .frame(maxWidth: AppSizing.maxContentWidth(sc))
                    .frame(maxWidth: .infinity)
                }
            }
            .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
            .navigationBarHidden(true)
            .navigationDestination(item: $destination) { dest in
                switch dest {
                case .changePassword:
                    ChangePasswordScreen()
                        .environmentObject(authVM)
                case .deleteAccount:
                    DeleteAccountScreen()
                        .environmentObject(authVM)
                }
            }
            .onAppear{
                showTabBar = false
            }
            .onChange(of: destination) { dest in
                withAnimation { showTabBar = dest == nil }
            }
            .sheet(isPresented: $showingPrivacySheet) {
                SafariView(url: privacyPolicyURL)
                    .ignoresSafeArea()
            }

            if showingLanguageDialog {
                LanguageChooserDialog(
                    pendingLanguage: $pendingLanguage,
                    onOK: {
                        languageVM.setLanguage(pendingLanguage)
                        withAnimation(.easeIn(duration: 0.15)) { showingLanguageDialog = false }
                    },
                    onDismiss: {
                        withAnimation(.easeIn(duration: 0.15)) { showingLanguageDialog = false }
                    }
                )
            }
        }
    }
}

// MARK: - Supporting views

private struct SettingsCard<Content: View>: View {
    @Environment(\.appSizeClass) var sc
    @ViewBuilder let content: Content

    var body: some View {
        VStack(spacing: 0) { content }
            .background(AppTheme.Colors.surface)
            .cornerRadius(AppSizing.cardRadius(sc))
            .shadow(
                color: AppTheme.Shadow.card.color,
                radius: AppTheme.Shadow.card.radius,
                x: 0, y: AppTheme.Shadow.card.y
            )
    }
}

private struct SettingsCenteredRow: View {
    let title: String
    var tint: Color = AppTheme.Colors.textPrimaryFallback
    let action: () -> Void
    @Environment(\.appSizeClass) var sc

    var body: some View {
        Button(action: action) {
            HStack {
                Spacer()
                Text(title)
                    .font(AppTheme.Typography.body(sc))
                    .fontWeight(.semibold)
                    .foregroundColor(tint)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(
                        size: AppSizing.fontSize(base: 14, sizeClass: sc),
                        weight: .semibold
                    ))
                    .foregroundColor(tint)
            }
            .padding(.horizontal, AppSizing.Padding.forClass(sc).md)
            .padding(.vertical, AppSizing.Padding.forClass(sc).md)
        }
    }
}

// MARK: - In-app Safari view for Privacy Policy

private struct SafariView: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> SFSafariViewController {
        SFSafariViewController(url: url)
    }

    func updateUIViewController(_ uiViewController: SFSafariViewController, context: Context) {}
}

// MARK: - Android-style Language Chooser Dialog

private struct LanguageChooserDialog: View {
    @Binding var pendingLanguage: String
    let onOK: () -> Void
    let onDismiss: () -> Void

    private var languages: [(code: String, label: String)] {
        [
            ("en", NSLocalizedString("English", comment: "")),
            ("es", NSLocalizedString("Español", comment: ""))
        ]
    }

    var body: some View {
        ZStack {
            Color.black.opacity(0.55)
                .ignoresSafeArea()
                .onTapGesture { onDismiss() }

            VStack(alignment: .leading, spacing: 0) {
                Text(NSLocalizedString("Choose language", comment: ""))
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.top, 24)
                    .padding(.bottom, 20)

                ForEach(languages, id: \.code) { lang in
                    Button {
                        pendingLanguage = lang.code
                    } label: {
                        HStack {
                            Text(lang.label)
                                .font(.system(size: 16))
                                .foregroundColor(.white)
                            Spacer()
                            RadioCircle(isSelected: pendingLanguage == lang.code)
                        }
                        .padding(.horizontal, 24)
                        .padding(.vertical, 14)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }

                HStack {
                    Spacer()
                    Button {
                        onOK()
                    } label: {
                        Text(NSLocalizedString("OK", comment: ""))
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(Color(red: 0.35, green: 0.55, blue: 1.0))
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 18)
                .padding(.top, 10)
            }
            .frame(maxWidth: 320)
            .background(Color(red: 0.16, green: 0.16, blue: 0.20))
            .cornerRadius(20)
            .shadow(color: .black.opacity(0.35), radius: 24, y: 8)
        }
        .transition(.opacity.combined(with: .scale(scale: 0.95)))
    }
}

private struct RadioCircle: View {
    let isSelected: Bool

    var body: some View {
        ZStack {
            Circle()
                .stroke(isSelected ? Color(red: 0.45, green: 0.5, blue: 1.0) : Color.white.opacity(0.4), lineWidth: 2)
                .frame(width: 22, height: 22)
            if isSelected {
                Circle()
                    .fill(Color(red: 0.45, green: 0.5, blue: 1.0))
                    .frame(width: 12, height: 12)
            }
        }
    }
}

// MARK: - SettingsDestination: Hashable
extension SettingsScreen.SettingsDestination: Hashable {}
