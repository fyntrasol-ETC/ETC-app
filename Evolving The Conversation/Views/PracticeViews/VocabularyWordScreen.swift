import SwiftUI

// MARK: - Vocabulary Word Screen
struct VocabularyWordScreen: View {
    let level: VocabularyLevel
    let onBack: () -> Void
    @Environment(\.appSizeClass) var sc
    @ObservedObject private var store = SavedVocabularyStore.shared

    @State private var currentIndex: Int = 0
    @State private var words: [VocabularyWord]
    @State private var flashcardIndex: Int = 0

    init(level: VocabularyLevel, onBack: @escaping () -> Void) {
        self.level = level
        self.onBack = onBack
        _words = State(initialValue: VocabularyData.words(for: level).shuffled())
    }

    private var currentWord: VocabularyWord { words[currentIndex] }

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        ZStack {
            AppTheme.Colors.backgroundFallback.ignoresSafeArea()

            VStack(spacing: 0) {
                HStack(spacing: AppSizing.scaled(12, sc)) {
                    ETCBackButton(action: onBack)
                    Text(level.screenTitle)
                        .font(AppTheme.Typography.title2(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    Spacer()
                }
                .padding(.horizontal, pad.lg)
                .padding(.vertical, pad.md)
                .background(AppTheme.Colors.surface)

                ScrollView(showsIndicators: false) {
                    VStack(spacing: pad.md) {
                        WordCard(
                            word: currentWord,
                            isSaved: store.isSaved(currentWord),
                            onToggleSave: { store.toggle(currentWord) }
                        )
                        .padding(.top, pad.md)

                        FlashcardsSection(words: words, flashcardIndex: $flashcardIndex)
                    }
                    .padding(.horizontal, pad.lg)
                    .padding(.bottom, 40)
                    .frame(maxWidth: AppSizing.maxContentWidth(sc))
                    .frame(maxWidth: .infinity)
                }
            }
        }
    }
}

// MARK: - Word Card
private struct WordCard: View {
    let word: VocabularyWord
    let isSaved: Bool
    let onToggleSave: () -> Void

    @Environment(\.appSizeClass) var sc

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        let cardRadius = AppSizing.cardRadius(sc)

        VStack(spacing: AppSizing.scaled(16, sc)) {
            Text("WORD OF THE DAY")
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

// MARK: - Page Controls (chevrons only, no dots)
private struct PageControls: View {
    @Binding var currentPage: Int
    let totalPages: Int
    @Environment(\.appSizeClass) var sc

    var body: some View {
        HStack {
            ETCNavIconButton(
                systemName: "chevron.left",
                tint: currentPage > 0
                    ? AppTheme.Colors.textPrimaryFallback
                    : AppTheme.Colors.textSecondaryFallback.opacity(0.4),
                bgColor: AppTheme.Colors.surface
            ) {
                withAnimation(.easeInOut(duration: 0.18)) {
                    currentPage = max(0, currentPage - 1)
                }
            }
            .disabled(currentPage <= 0)

            Spacer()

            ETCNavIconButton(
                systemName: "chevron.right",
                tint: currentPage < totalPages - 1
                    ? AppTheme.Colors.textPrimaryFallback
                    : AppTheme.Colors.textSecondaryFallback.opacity(0.4),
                bgColor: AppTheme.Colors.surface
            ) {
                withAnimation(.easeInOut(duration: 0.18)) {
                    currentPage = min(totalPages - 1, currentPage + 1)
                }
            }
            .disabled(currentPage >= totalPages - 1)
        }
        .padding(.top, 4)
    }
}

// MARK: - Flashcards Section
private struct FlashcardsSection: View {
    let words: [VocabularyWord]
    @Binding var flashcardIndex: Int
    @Environment(\.appSizeClass) var sc

    private let maxPages = 5
    private let cardsPerPage = 2

    private let palette: [Color] = [
        Color(red: 0.294, green: 0.357, blue: 0.961), // blue
        Color(red: 0.176, green: 0.741, blue: 0.494), // green
        Color(red: 0.608, green: 0.435, blue: 0.910), // purple
        Color(red: 0.961, green: 0.651, blue: 0.137), // orange
        Color(red: 0.910, green: 0.318, blue: 0.475), // pink/red
    ]

    private var flashcardWords: [VocabularyWord] {
        Array(words.prefix(maxPages * cardsPerPage))
    }

    private var pages: [[VocabularyWord]] {
        stride(from: 0, to: flashcardWords.count, by: cardsPerPage).map {
            Array(flashcardWords[$0..<min($0 + cardsPerPage, flashcardWords.count)])
        }
    }

    private func color(for globalIndex: Int) -> Color {
        palette[globalIndex % palette.count]
    }

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        VStack(alignment: .leading, spacing: pad.sm) {
            HStack {
                Text("Flashcards")
                    .font(AppTheme.Typography.title2(sc))
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                Spacer()
            }

            TabView(selection: $flashcardIndex) {
                ForEach(Array(pages.enumerated()), id: \.offset) { pageIndex, pair in
                    HStack(spacing: AppSizing.scaled(12, sc)) {
                        ForEach(Array(pair.enumerated()), id: \.element.id) { offsetInPair, word in
                            let globalIndex = pageIndex * cardsPerPage + offsetInPair
                            FlashCard(word: word, color: color(for: globalIndex))
                        }
                        if pair.count == 1 {
                            Spacer(minLength: 0)
                        }
                    }
                    .padding(.horizontal, 4)
                    .tag(pageIndex)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .frame(height: flashcardHeight)

            PageControls(currentPage: $flashcardIndex, totalPages: pages.count)
        }
        .onChange(of: words) { _ in flashcardIndex = 0 }
    }

    private var flashcardHeight: CGFloat {
        switch sc {
        case .compact: return 150
        case .regular: return 180
        case .large:   return 220
        }
    }
}
// MARK: - Flash Card
private struct FlashCard: View {
    let word: VocabularyWord
    let color: Color
    @Environment(\.appSizeClass) var sc
    @State private var isFlipped = false

    var body: some View {
        Button { withAnimation(.spring()) { isFlipped.toggle() } } label: {
            ZStack {
                // Front
                VStack(spacing: 8) {
                    Text(word.word)
                        .font(.system(size: AppSizing.fontSize(base: 20, sizeClass: sc), weight: .bold))
                        .foregroundColor(.white)
                    Text("tap to flip")
                        .font(AppTheme.Typography.caption(sc))
                        .foregroundColor(.white.opacity(0.75))
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(color)
                .cornerRadius(AppSizing.cardRadius(sc))
                .opacity(isFlipped ? 0 : 1)

                // Back — counter-rotated so the text reads correctly once flipped
                Text(word.definition)
                    .font(.system(size: AppSizing.fontSize(base: 13, sizeClass: sc), weight: .medium))
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)
                    .lineLimit(3)
                    .minimumScaleFactor(0.75)
                    .padding(.horizontal, 12)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(color.opacity(0.85))
                    .cornerRadius(AppSizing.cardRadius(sc))
                    .rotation3DEffect(.degrees(180), axis: (x: 0, y: 1, z: 0))
                    .opacity(isFlipped ? 1 : 0)
            }
            .rotation3DEffect(.degrees(isFlipped ? 180 : 0), axis: (x: 0, y: 1, z: 0))
        }
        .buttonStyle(.plain)
    }
}
