import SwiftUI


// MARK: - Saved Vocabulary Screen
/// Shows every saved word as a vertically scrolling list of cards.
/// Tapping the bookmark on any card unsaves it and it drops out of the list.
struct SavedVocabularyScreen: View {
    let onBack: () -> Void
    @Environment(\.appSizeClass) var sc
    @ObservedObject private var store = SavedVocabularyStore.shared

    private var words: [VocabularyWord] { store.savedWords }

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        ZStack {
            AppTheme.Colors.backgroundFallback.ignoresSafeArea()

            VStack(spacing: 0) {
                HStack(spacing: AppSizing.scaled(12, sc)) {
                    ETCBackButton(action: onBack)
                    Text("Saved Vocabulary")
                        .font(AppTheme.Typography.title2(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    Spacer()
                }
                .padding(.horizontal, pad.lg)
                .padding(.vertical, pad.md)
                .background(AppTheme.Colors.surface)

                if words.isEmpty {
                    emptyState(pad: pad)
                } else {
                    ScrollView(showsIndicators: false) {
                        LazyVStack(spacing: pad.md) {
                            ForEach(words) { word in
                                SavedWordCard(
                                    word: word,
                                    onUnsave: { store.toggle(word) }
                                )
                            }
                        }
                        .padding(.horizontal, pad.lg)
                        .padding(.top, pad.md)
                        .padding(.bottom, 40)
                        .frame(maxWidth: AppSizing.maxContentWidth(sc))
                        .frame(maxWidth: .infinity)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func emptyState(pad: AppSizing.Padding) -> some View {
        VStack(spacing: pad.sm) {
            Spacer()
            Image(systemName: "bookmark")
                .font(.system(size: AppSizing.fontSize(base: 40, sizeClass: sc)))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback.opacity(0.5))
            Text("No saved words yet")
                .font(AppTheme.Typography.headline(sc))
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)
            Text("Tap the bookmark icon on any word to save it here.")
                .font(AppTheme.Typography.subheadline(sc))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                .multilineTextAlignment(.center)
                .padding(.horizontal, pad.xl)
            Spacer()
            Spacer()
        }
    }
}

// MARK: - Saved Word Card
/// Same visual language as the main word card, minus the index badge and
/// nav arrows — each one just sits in the vertical list.
private struct SavedWordCard: View {
    let word: VocabularyWord
    let onUnsave: () -> Void

    @Environment(\.appSizeClass) var sc

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        let cardRadius = AppSizing.cardRadius(sc)

        VStack(spacing: AppSizing.scaled(16, sc)) {
            HStack {
                Text(word.level.title.uppercased())
                    .font(.system(size: AppSizing.fontSize(base: 11, sizeClass: sc), weight: .bold))
                    .foregroundColor(word.level.gradientColors[0])
                    .tracking(1.2)
                Spacer()
            }

            VStack(spacing: AppSizing.scaled(6, sc)) {
                HStack(alignment: .center, spacing: pad.sm) {
                    Text(word.word)
                        .font(.system(size: AppSizing.fontSize(base: 30, sizeClass: sc), weight: .bold))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)

                    Spacer()

                    Button(action: onUnsave) {
                        ZStack {
                            RoundedRectangle(cornerRadius: AppSizing.scaled(10, sc))
                                .fill(AppTheme.Colors.backgroundFallback)
                                .frame(width: AppSizing.iconSize(base: 40, sizeClass: sc),
                                       height: AppSizing.iconSize(base: 40, sizeClass: sc))
                            Image(systemName: "bookmark.fill")
                                .font(.system(size: AppSizing.fontSize(base: 16, sizeClass: sc)))
                                .foregroundColor(AppTheme.Colors.primaryFallback)
                        }
                    }
                    .buttonStyle(.plain)
                }

                HStack(spacing: AppSizing.scaled(8, sc)) {
                    Text("\(word.phonetic) · \(word.partOfSpeech)")
                        .font(AppTheme.Typography.subheadline(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)

                    Button {
                        SpeechService.shared.speak(word.word)
                    } label: {
                        Image(systemName: "speaker.wave.2")
                            .font(.system(size: AppSizing.fontSize(base: 14, sizeClass: sc)))
                            .foregroundColor(AppTheme.Colors.primaryFallback)
                    }
                    .buttonStyle(.plain)

                    Spacer()
                }
            }

            Text(word.definition)
                .font(AppTheme.Typography.body(sc))
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                .frame(maxWidth: .infinity, alignment: .leading)

            Text("\"\(word.example)\"")
                .font(AppTheme.Typography.subheadline(sc))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                .italic()
                .frame(maxWidth: .infinity, alignment: .center)
                .multilineTextAlignment(.center)
                .padding(pad.sm)
                .background(AppTheme.Colors.backgroundFallback)
                .cornerRadius(AppSizing.scaled(10, sc))

            HStack(alignment: .top, spacing: 0) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("SYNONYMS")
                        .font(.system(size: AppSizing.fontSize(base: 10, sizeClass: sc), weight: .semibold))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        .tracking(0.8)
                    Text(word.synonyms)
                        .font(AppTheme.Typography.body(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                VStack(alignment: .leading, spacing: 4) {
                    Text("ANTONYMS")
                        .font(.system(size: AppSizing.fontSize(base: 10, sizeClass: sc), weight: .semibold))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        .tracking(0.8)
                    Text(word.antonyms)
                        .font(AppTheme.Typography.body(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .padding(pad.md)
        .background(AppTheme.Colors.surface)
        .cornerRadius(cardRadius)
        .shadow(color: AppTheme.Shadow.card.color,
                radius: AppTheme.Shadow.card.radius,
                x: AppTheme.Shadow.card.x,
                y: AppTheme.Shadow.card.y)
        .id(word.id)
    }
}
// MARK: - Word Card (same visual design as VocabularyWordScreen's WordCard)
private struct WordCard: View {
    let word: VocabularyWord
    let index: Int
    let total: Int
    let isSaved: Bool
    let onToggleSave: () -> Void

    @Environment(\.appSizeClass) var sc

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        let cardRadius = AppSizing.cardRadius(sc)

        VStack(spacing: AppSizing.scaled(16, sc)) {
            Text("WORD \(index + 1) OF \(total)")
                .font(.system(size: AppSizing.fontSize(base: 11, sizeClass: sc), weight: .bold))
                .foregroundColor(AppTheme.Colors.greenFallback)
                .tracking(1.2)

            VStack(spacing: AppSizing.scaled(6, sc)) {
                HStack(alignment: .center, spacing: pad.sm) {
                    Text(word.word)
                        .font(.system(size: AppSizing.fontSize(base: 36, sizeClass: sc), weight: .bold))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)

                    Spacer()

                    Button(action: onToggleSave) {
                        ZStack {
                            RoundedRectangle(cornerRadius: AppSizing.scaled(10, sc))
                                .fill(AppTheme.Colors.backgroundFallback)
                                .frame(width: AppSizing.iconSize(base: 40, sizeClass: sc),
                                       height: AppSizing.iconSize(base: 40, sizeClass: sc))
                            Image(systemName: isSaved ? "bookmark.fill" : "bookmark")
                                .font(.system(size: AppSizing.fontSize(base: 16, sizeClass: sc)))
                                .foregroundColor(AppTheme.Colors.primaryFallback)
                        }
                    }
                }

                HStack(spacing: AppSizing.scaled(8, sc)) {
                    Text("\(word.phonetic) · \(word.partOfSpeech)")
                        .font(AppTheme.Typography.subheadline(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)

                    Button {
                        SpeechService.shared.speak(word.word)
                    } label: {
                        Image(systemName: "speaker.wave.2")
                            .font(.system(size: AppSizing.fontSize(base: 14, sizeClass: sc)))
                            .foregroundColor(AppTheme.Colors.primaryFallback)
                    }
                    .buttonStyle(.plain)

                    Spacer()
                }
            }

            Text(word.definition)
                .font(AppTheme.Typography.body(sc))
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                .frame(maxWidth: .infinity, alignment: .leading)

            Text("\"\(word.example)\"")
                .font(AppTheme.Typography.subheadline(sc))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                .italic()
                .frame(maxWidth: .infinity, alignment: .center)
                .multilineTextAlignment(.center)
                .padding(pad.sm)
                .background(AppTheme.Colors.backgroundFallback)
                .cornerRadius(AppSizing.scaled(10, sc))

            HStack(alignment: .top, spacing: 0) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("SYNONYMS")
                        .font(.system(size: AppSizing.fontSize(base: 10, sizeClass: sc), weight: .semibold))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        .tracking(0.8)
                    Text(word.synonyms)
                        .font(AppTheme.Typography.body(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                VStack(alignment: .leading, spacing: 4) {
                    Text("ANTONYMS")
                        .font(.system(size: AppSizing.fontSize(base: 10, sizeClass: sc), weight: .semibold))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        .tracking(0.8)
                    Text(word.antonyms)
                        .font(AppTheme.Typography.body(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .padding(pad.md)
        .background(AppTheme.Colors.surface)
        .cornerRadius(cardRadius)
        .shadow(color: AppTheme.Shadow.card.color,
                radius: AppTheme.Shadow.card.radius,
                x: AppTheme.Shadow.card.x,
                y: AppTheme.Shadow.card.y)
        .id(word.id)
    }
}

// MARK: - Prev / Next Navigation (no counter text, per current design)
private struct NavigationControls: View {
    @Binding var currentIndex: Int
    let total: Int
    @Environment(\.appSizeClass) var sc

    var body: some View {
        HStack {
            NavButton(systemName: "chevron.left", enabled: currentIndex > 0) {
                withAnimation(.easeInOut(duration: 0.18)) {
                    currentIndex = max(0, currentIndex - 1)
                }
            }

            Spacer()

            NavButton(systemName: "chevron.right", enabled: currentIndex < total - 1) {
                withAnimation(.easeInOut(duration: 0.18)) {
                    currentIndex = min(total - 1, currentIndex + 1)
                }
            }
        }
        .padding(.top, 4)
    }
}

private struct NavButton: View {
    let systemName: String
    let enabled: Bool
    let action: () -> Void
    @Environment(\.appSizeClass) var sc

    var body: some View {
        let size = AppSizing.iconSize(base: 44, sizeClass: sc)

        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: AppSizing.fontSize(base: 16, sizeClass: sc), weight: .semibold))
                .foregroundColor(
                    enabled
                    ? AppTheme.Colors.textPrimaryFallback
                    : AppTheme.Colors.textSecondaryFallback.opacity(0.4)
                )
                .frame(width: size, height: size)
                .background(AppTheme.Colors.surface)
                .clipShape(Circle())
                .shadow(color: .black.opacity(0.06), radius: 4, y: 2)
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}
