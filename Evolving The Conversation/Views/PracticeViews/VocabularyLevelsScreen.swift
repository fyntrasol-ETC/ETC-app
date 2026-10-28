import SwiftUI

// MARK: - Vocabulary Levels Screen
struct VocabularyLevelsScreen: View {
    let onBack: () -> Void
    @Environment(\.appSizeClass) var sc

    @State private var selectedLevel: VocabularyLevel? = nil
    @State private var showSaved = false

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        ZStack {
            AppTheme.Colors.backgroundFallback.ignoresSafeArea()

            VStack(spacing: 0) {
                // Nav
                HStack(spacing: AppSizing.scaled(12, sc)) {
                    ETCBackButton(action: onBack)
                    Text("Vocabulary")
                        .font(AppTheme.Typography.title2(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    Spacer()
                }
                .padding(.horizontal, pad.lg)
                .padding(.vertical, pad.md)
                .background(AppTheme.Colors.surface)

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: pad.md) {

                        Text("Choose a difficulty level to start practicing.")
                            .font(AppTheme.Typography.body(sc))
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                            .padding(.top, pad.md)

                        ForEach(VocabularyLevel.allCases) { level in
                            LevelCard(level: level) {
                                selectedLevel = level
                            }
                        }

                        SavedVocabularyRow {
                            showSaved = true
                        }
                        .padding(.top, pad.xs)
                    }
                    .padding(.horizontal, pad.lg)
                    .padding(.bottom, 40)
                    .frame(maxWidth: AppSizing.maxContentWidth(sc))
                    .frame(maxWidth: .infinity)
                }
            }
        }
        .fullScreenCover(item: $selectedLevel) { level in
            VocabularyWordScreen(level: level, onBack: { selectedLevel = nil })
                .appSizeClass()
        }
        .fullScreenCover(isPresented: $showSaved) {
            SavedVocabularyScreen(onBack: { showSaved = false })
                .appSizeClass()
        }
    }
}

// MARK: - Level Card
private struct LevelCard: View {
    let level: VocabularyLevel
    let action: () -> Void
    @Environment(\.appSizeClass) var sc

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        let wordCount = VocabularyData.words(for: level).count

        Button(action: action) {
            VStack(alignment: .leading, spacing: 6) {
                Text(level.title)
                    .font(AppTheme.Typography.title2(sc))
                    .fontWeight(.bold)
                    .foregroundColor(.white)

                Text(level.subtitle)
                    .font(AppTheme.Typography.subheadline(sc))
                    .foregroundColor(.white.opacity(0.85))

                Text(String(format: NSLocalizedString("%d words · Tap to practice", comment: ""), wordCount))                    .font(AppTheme.Typography.caption(sc))
                    .foregroundColor(.white.opacity(0.75))
                    .padding(.top, 4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(pad.md)
            .background(
                LinearGradient(colors: level.gradientColors,
                               startPoint: .topLeading,
                               endPoint: .bottomTrailing)
            )
            .cornerRadius(AppSizing.cardRadius(sc))
            .shadow(color: level.gradientColors[0].opacity(0.30), radius: 12, x: 0, y: 6)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Saved Vocabulary Row
private struct SavedVocabularyRow: View {
    let action: () -> Void
    @Environment(\.appSizeClass) var sc
    @ObservedObject private var store = SavedVocabularyStore.shared

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        Button(action: action) {
            HStack(spacing: pad.sm) {
                ZStack {
                    RoundedRectangle(cornerRadius: 10)
                        .fill(AppTheme.Colors.primaryLightFallback)
                        .frame(width: AppSizing.iconSize(base: 36, sizeClass: sc),
                               height: AppSizing.iconSize(base: 36, sizeClass: sc))
                    Image(systemName: "bookmark.fill")
                        .font(.system(size: AppSizing.fontSize(base: 15, sizeClass: sc)))
                        .foregroundColor(AppTheme.Colors.primaryFallback)
                }

                Text("Saved Vocabulary")
                    .font(AppTheme.Typography.body(sc))
                    .fontWeight(.semibold)
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)

                Spacer()

                if !store.savedIDs.isEmpty {
                    Text("\(store.savedIDs.count)")
                        .font(.system(size: AppSizing.fontSize(base: 12, sizeClass: sc), weight: .semibold))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                }

                Image(systemName: "chevron.right")
                    .font(.system(size: AppSizing.fontSize(base: 13, sizeClass: sc), weight: .semibold))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
            }
            .padding(.vertical, pad.sm)
            .padding(.horizontal, pad.sm)
            .background(AppTheme.Colors.surface)
            .cornerRadius(AppSizing.scaled(12, sc))
            .overlay(
                RoundedRectangle(cornerRadius: AppSizing.scaled(12, sc))
                    .stroke(AppTheme.Colors.borderFallback, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }
}
