import SwiftUI

struct PracticeScreen: View {
    @EnvironmentObject private var authVM: AuthViewModel
    @Environment(\.appSizeClass) var sc
    @Binding var selectedTab: ETCTab

    @State private var showSearch        = false
    @State private var showNotifications = false
    @State private var destination: PracticeDestination? = nil
    @Binding var showTabBar: Bool

    enum PracticeDestination: Identifiable, Hashable {
        case grammar, vocabulary, quiz, speaking, listening, writing, aiConversation
        var id: Int { hashValue }
    }

    struct PracticeItem: Identifiable {
        let id = UUID()
        let icon: String
        let iconColor: Color
        let iconBg: Color
        let title: String
        let subtitle: String
        var badge: String? = nil
        var destination: PracticeDestination? = nil
    }

    // ── FIX: every title/subtitle/badge below was a raw literal handed to
    // a String field, then rendered with Text(item.title) inside PracticeRow —
    // that path never localizes. Wrapping each one in NSLocalizedString here
    // (at creation time) makes it actually translate. ──
    private let items: [PracticeItem] = [
        PracticeItem(icon: "mic", iconColor: AppTheme.Colors.primaryFallback,
                     iconBg: AppTheme.Colors.primaryLightFallback,
                     title: NSLocalizedString("Speaking Practice", comment: ""),
                     subtitle: NSLocalizedString("AI conversations & pronunciation", comment: ""),
                     badge: NSLocalizedString("AI Tutor", comment: ""),
                     destination: .speaking),

        PracticeItem(icon: "headphones", iconColor: AppTheme.Colors.purpleFallback,
                     iconBg: Color(red: 0.93, green: 0.90, blue: 0.99),
                     title: NSLocalizedString("Listening Practice", comment: ""),
                     subtitle: NSLocalizedString("Podcasts, dialogues & dictation", comment: ""),
                     destination: .listening),

        PracticeItem(icon: "pencil", iconColor: AppTheme.Colors.greenFallback,
                     iconBg: Color(red: 0.90, green: 0.97, blue: 0.93),
                     title: NSLocalizedString("Writing Practice", comment: ""),
                     subtitle: NSLocalizedString("Essays with instant AI feedback", comment: ""),
                     destination: .writing),

        PracticeItem(icon: "doc.text.fill", iconColor: AppTheme.Colors.orangeFallback,
                     iconBg: Color(red: 1.0, green: 0.95, blue: 0.88),
                     title: NSLocalizedString("Grammar", comment: ""),
                     // ── NOTE: "Tenses, articles, conditionals…" is not in
                     // Localizable.strings yet — add it (see bottom). ──
                     subtitle: NSLocalizedString("Tenses, articles, conditionals…", comment: ""),
                     destination: .grammar),

        PracticeItem(icon: "sparkles", iconColor: AppTheme.Colors.red,
                     iconBg: Color(red: 1.0, green: 0.92, blue: 0.92),
                     title: NSLocalizedString("Vocabulary", comment: ""),
                     subtitle: NSLocalizedString("Flashcards & word of the day", comment: ""),
                     destination: .vocabulary),

        PracticeItem(icon: "target", iconColor: AppTheme.Colors.primaryFallback,
                     iconBg: AppTheme.Colors.primaryLightFallback,
                     title: NSLocalizedString("Quiz Center", comment: ""),
                     subtitle: NSLocalizedString("MCQ, fill blanks, match pairs", comment: ""),
                     destination: .quiz),
    ]

    var body: some View {
            VStack(spacing: 0) {
                ETCScreenHeader(
                    // ── FIX: was raw "Practice" / "Sharpen every skill" ──
                    title: NSLocalizedString("Practice", comment: ""),
                    subtitle: NSLocalizedString("Sharpen every skill", comment: ""),
                    onSearch: { showSearch = true },
                    onNotification: { showNotifications = true },
                    notificationBadge: 3,
                    style: .plain
                )

                ScrollView(showsIndicators: false) {
                    VStack(spacing: AppSizing.Padding.forClass(sc).sm) {
                        aiTutorBanner
                            .padding(.top, AppSizing.Padding.forClass(sc).md)

                        ForEach(items) { item in
                            PracticeRow(item: item) {
                                guard let dest = item.destination else { return }
                                destination = dest
                            }
                        }
                    }
                    .padding(.horizontal, AppSizing.Padding.forClass(sc).lg)
                    .padding(.bottom, 100)
                    .frame(maxWidth: AppSizing.maxContentWidth(sc))
                    .frame(maxWidth: .infinity)
                }
            }
            .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())

            ETCTabBar(selectedTab: $selectedTab)
                .ignoresSafeArea(edges: .bottom)
        
        .fullScreenCover(isPresented: $showSearch) {
            ETCSearchScreen(isPresented: $showSearch)
                .environmentObject(authVM)
                .appSizeClass()
        }
        .fullScreenCover(isPresented: $showNotifications) {
            ETCNotificationsScreen(isPresented: $showNotifications)
                .appSizeClass()
        }
        
        .fullScreenCover(item: $destination) { dest in
            switch dest {
            case .speaking:
                SpeakingPracticeScreen(onBack: { destination = nil }).appSizeClass()
            case .listening:
                ListeningPracticeScreen(onBack: { destination = nil }).appSizeClass()
            case .writing:
                WritingPracticeScreen(onBack: { destination = nil }).appSizeClass()
            case .grammar:
                GrammarScreen(onBack: { destination = nil }).appSizeClass()
            case .vocabulary:
                VocabularyLevelsScreen(onBack: { destination = nil }).appSizeClass()
            case .aiConversation:
                AIConversationScreen(onBack: { destination = nil })
                    .environmentObject(authVM)
                    .appSizeClass()
            case .quiz:
                QuizScreen(title: "General", category: "Grammar", onBack: { destination = nil })
                    .appSizeClass()
            }
        }
    }    // MARK: - AI Tutor Banner
    private var aiTutorBanner: some View {
        let pad = AppSizing.Padding.forClass(sc)

        return Button { destination = .aiConversation } label: {
            HStack(spacing: pad.md) {

                // Mic icon box
                ZStack {
                    RoundedRectangle(cornerRadius: 14)
                        .fill(Color.white.opacity(0.22))
                        .frame(
                            width:  AppSizing.iconSize(base: 52, sizeClass: sc),
                            height: AppSizing.iconSize(base: 52, sizeClass: sc)
                        )
                    Image(systemName: "mic.fill")
                        .font(.system(size: AppSizing.fontSize(base: 22, sizeClass: sc), weight: .medium))
                        .foregroundColor(.white)
                }

                // Text
                VStack(alignment: .leading, spacing: 4) {
                    // ── This literal typed directly in Text(...) already
                    // auto-localizes correctly (Text(LocalizedStringKey)) — no change needed. ──
                    Text("Talk with your AI tutor")
                        .font(.system(
                            size: AppSizing.fontSize(base: 18, sizeClass: sc),
                            weight: .bold
                        ))
                        .foregroundColor(.white)

                    // ── FIX: this one embeds a dynamic number, so it must be
                    // a format string, not a literal. Add the key below to
                    // Localizable.strings. ──
                    Text(String(format: NSLocalizedString("Confidence score: %d · tap to start", comment: ""), 82))
                        .font(AppTheme.Typography.subheadline(sc))
                        .foregroundColor(.white.opacity(0.85))
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(size: AppSizing.fontSize(base: 16, sizeClass: sc), weight: .semibold))
                    .foregroundColor(.white.opacity(0.80))
            }
            .padding(pad.md)
            .background(
                LinearGradient(
                    colors: [AppTheme.Colors.primaryFallback,
                             AppTheme.Colors.primaryDarkFallback],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .cornerRadius(AppSizing.cardRadius(sc))
            .shadow(
                color: AppTheme.Colors.primaryFallback.opacity(0.35),
                radius: 12, x: 0, y: 6
            )
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Practice Row
private struct PracticeRow: View {
    let item: PracticeScreen.PracticeItem
    let action: () -> Void

    @Environment(\.appSizeClass) var sc

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        let iconSize = AppSizing.iconSize(base: 52, sizeClass: sc)

        Button(action: action) {
            HStack(spacing: pad.md) {

                // Icon box
                ZStack {
                    RoundedRectangle(cornerRadius: 14)
                        .fill(item.iconBg)
                        .frame(width: iconSize, height: iconSize)
                    Image(systemName: item.icon)
                        .font(.system(
                            size: AppSizing.fontSize(base: 22, sizeClass: sc),
                            weight: .medium
                        ))
                        .foregroundColor(item.iconColor)
                }

                // Title + subtitle
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 8) {
                        Text(item.title)
                            .font(AppTheme.Typography.headline(sc))
                            .foregroundColor(AppTheme.Colors.textPrimaryFallback)

                        if let badge = item.badge {
                            Text(badge)
                                .font(.system(
                                    size: AppSizing.fontSize(base: 11, sizeClass: sc),
                                    weight: .semibold
                                ))
                                .foregroundColor(AppTheme.Colors.greenFallback)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 3)
                                .background(
                                    AppTheme.Colors.greenFallback.opacity(0.12)
                                )
                                .clipShape(Capsule())
                        }
                    }

                    Text(item.subtitle)
                        .font(AppTheme.Typography.subheadline(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(
                        size: AppSizing.fontSize(base: 14, sizeClass: sc),
                        weight: .semibold
                    ))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
            }
            .padding(pad.md)
            .background(AppTheme.Colors.surface)
            .cornerRadius(AppSizing.cardRadius(sc))
            .shadow(
                color: AppTheme.Shadow.card.color,
                radius: AppTheme.Shadow.card.radius,
                x: AppTheme.Shadow.card.x,
                y: AppTheme.Shadow.card.y
            )
        }
        .buttonStyle(.plain)
    }
}
