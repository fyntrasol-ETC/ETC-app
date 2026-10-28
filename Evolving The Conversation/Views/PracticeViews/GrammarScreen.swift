import SwiftUI

// MARK: - Grammar Screen
struct GrammarScreen: View {
    let onBack: () -> Void
    @Environment(\.appSizeClass) var sc
    @State private var selectedTopic: GrammarTopic? = nil

    struct GrammarTopic: Identifiable {
        let id = UUID()
        let title: String
    }

    private let topics: [GrammarTopic] = [
        GrammarTopic(title: "Tenses"),
        GrammarTopic(title: "Articles"),
        GrammarTopic(title: "Prepositions"),
        GrammarTopic(title: "Passive Voice"),
        GrammarTopic(title: "Active Voice"),
        GrammarTopic(title: "Modals"),
        GrammarTopic(title: "Conditionals"),
        GrammarTopic(title: "Reported Speech"),
        GrammarTopic(title: "Punctuation"),
    ]

    private var columns: [GridItem] {
        Array(repeating: GridItem(.flexible(), spacing: AppSizing.scaled(12, sc)),
              count: AppSizing.gridColumns(sc))
    }

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        ZStack {
            AppTheme.Colors.backgroundFallback.ignoresSafeArea()

            VStack(spacing: 0) {
                // Nav
                HStack(spacing: AppSizing.scaled(12, sc)) {
                    ETCBackButton(action: onBack)
                    Text("Grammar")
                        .font(AppTheme.Typography.title2(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    Spacer()
                }
                .padding(.horizontal, pad.lg)
                .padding(.vertical, pad.md)
                .background(AppTheme.Colors.surface)
                
                ScrollView(showsIndicators: false) {
                    LazyVGrid(columns: columns, spacing: AppSizing.scaled(12, sc)) {
                        ForEach(topics) { topic in
                            GrammarTopicCard(topic: topic) {
                                selectedTopic = topic
                            }
                        }
                    }
                    .padding(pad.lg)
                    .padding(.bottom, 40)
                    .frame(maxWidth: AppSizing.maxContentWidth(sc))
                    .frame(maxWidth: .infinity)
                }
            }
        }
        .fullScreenCover(item: $selectedTopic) { topic in
            GrammarQuizScreen(
                topic: topic.title,
                onBack: { selectedTopic = nil }
            )
            .appSizeClass()
        }
    }
}

// MARK: - Grammar Topic Card
private struct GrammarTopicCard: View {
    let topic: GrammarScreen.GrammarTopic
    let action: () -> Void
    @Environment(\.appSizeClass) var sc

    var body: some View {
        let iconSize = AppSizing.iconSize(base: 48, sizeClass: sc)
        let cardRadius = AppSizing.cardRadius(sc)
        let pad = AppSizing.Padding.forClass(sc)

        Button(action: action) {
            VStack(spacing: AppSizing.scaled(12, sc)) {
                ZStack {
                    RoundedRectangle(cornerRadius: AppSizing.scaled(12, sc))
                        .fill(AppTheme.Colors.primaryLightFallback)
                        .frame(width: iconSize, height: iconSize)
                    Image(systemName: "doc.text")
                        .font(.system(size: AppSizing.fontSize(base: 22, sizeClass: sc), weight: .medium))
                        .foregroundColor(AppTheme.Colors.primaryFallback)
                }

                VStack(spacing: 4) {
                    Text(topic.title)
                        .font(AppTheme.Typography.headline(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                        .multilineTextAlignment(.center)
                    
                }
            }
            .frame(maxWidth: .infinity)
            .padding(pad.md)
            .background(AppTheme.Colors.surface)
            .cornerRadius(cardRadius)
            .shadow(color: AppTheme.Shadow.card.color,
                    radius: AppTheme.Shadow.card.radius,
                    x: AppTheme.Shadow.card.x,
                    y: AppTheme.Shadow.card.y)
        }
        .buttonStyle(.plain)
    }
}
