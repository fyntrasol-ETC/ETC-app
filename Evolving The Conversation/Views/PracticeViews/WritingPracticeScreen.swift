

import SwiftUI


// ─────────────────────────────────────────────────────────────
// MARK: - Writing Practice Screen
// ─────────────────────────────────────────────────────────────

struct WritingPracticeScreen: View {
    var onBack: (() -> Void)? = nil

    @Environment(\.appSizeClass) var sc
    @State private var essayText = ""

    struct GrammarSuggestion: Identifiable {
        let id = UUID()
        let original: String
        let corrected: String
        let type: String
        let line: Int
        let severity: SeverityLevel
        var isAccepted: Bool = false

        enum SeverityLevel { case error, warning }
    }
    static let prompts: [String] = [
        "Some people prefer to study alone, others in groups. Discuss both views and give your opinion.",
        "Technology has made communication easier but relationships weaker. Do you agree or disagree?",
        "Should university education be free for everyone? Discuss.",
        "Describe a skill you'd like to learn and explain why it's important to you.",
        "Many people believe social media does more harm than good. What is your view?",
        "Discuss the advantages and disadvantages of working from home.",
        "Some people think success comes from hard work, others believe it comes from luck. Discuss both views.",
        "Should governments invest more in public transport than roads? Give your opinion."
    ]

    @State private var currentPrompt: String

    init(onBack: (() -> Void)? = nil) {
        self.onBack = onBack
        _currentPrompt = State(initialValue: Self.prompts.randomElement()!)
    }
    
    private let aiService = AIService()
        @State private var isLoading = false
        @State private var errorMessage: String? = nil
        @State private var feedbackTitle = "Strong structure!"
        @State private var feedbackMessage = "Clear intro and conclusion. Try varying sentence length and adding one more example in paragraph two."
        @State private var hasReceivedFeedback = false
    
    @State private var suggestions: [GrammarSuggestion] = []

    private var wordCount: Int {
        essayText.split(separator: " ").count
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                ETCScreenBackBar(title: "Writing Practice") {
                    onBack?()
                }

                ScrollView(showsIndicators: false) {
                    VStack(spacing: AppSizing.Padding.forClass(sc).md) {

                        // Writing prompt card
                        writingPromptCard

                        // Essay section
                        essaySection

                        // Action buttons
                        actionButtons

                        // AI Feedback
                        if hasReceivedFeedback {
                            aiFeedbackSection
                        }

                        // Grammar suggestions
                        if !suggestions.isEmpty {
                            grammarSuggestionsSection
                        }
                        
                    }
                    .padding(.horizontal, AppSizing.Padding.forClass(sc).lg)
                    .padding(.top, AppSizing.Padding.forClass(sc).md)
                    .padding(.bottom, 100)
                    .frame(maxWidth: AppSizing.maxContentWidth(sc))
                    .frame(maxWidth: .infinity)
                }
            }
            .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
        }
    }

    // MARK: - Writing Prompt Card
    private var writingPromptCard: some View {
        let pad = AppSizing.Padding.forClass(sc)
        return VStack(spacing: pad.sm) {
            Text("WRITING PROMPT")
                .font(.system(size: AppSizing.fontSize(base: 11, sizeClass: sc), weight: .semibold))
                .foregroundColor(AppTheme.Colors.primaryFallback)
                .tracking(1.2)

            Text("\"\(currentPrompt)\"")
                .font(AppTheme.Typography.body(sc))
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity)
        .padding(pad.md)
        .background(AppTheme.Colors.primaryLightFallback)
        .cornerRadius(AppSizing.cardRadius(sc))
    }

    // MARK: - Essay Section
    private var essaySection: some View {
        let pad = AppSizing.Padding.forClass(sc)
        return VStack(alignment: .leading, spacing: pad.sm) {
            Text("Your essay")
                .font(.system(size: AppSizing.fontSize(base: 17, sizeClass: sc), weight: .bold))
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)

            VStack(spacing: 0) {
                ZStack(alignment: .topLeading) {
                    // Dark text area background — matches screenshot
                    RoundedRectangle(cornerRadius: AppSizing.cardRadius(sc))
                        .fill(Color(red: 0.20, green: 0.20, blue: 0.22))

                    TextEditor(text: $essayText)
                        .scrollContentBackground(.hidden)
                        .background(Color.clear)
                        .font(AppTheme.Typography.body(sc))
                        .foregroundColor(.white)
                        .padding(pad.sm)
                        .frame(minHeight: AppSizing.mediaHeight(base: 180, sizeClass: sc))

                    if essayText.isEmpty {
                        Text("Start writing here…")
                            .font(AppTheme.Typography.body(sc))
                            .foregroundColor(Color.white.opacity(0.35))
                            .padding(pad.sm + 4)
                            .allowsHitTesting(false)
                    }
                }
                .frame(minHeight: AppSizing.mediaHeight(base: 180, sizeClass: sc))

                HStack {
                    Text("\(wordCount) / 250 words")
                        .font(AppTheme.Typography.caption(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    Spacer()
                    Text("Auto-saved")
                        .font(.system(size: AppSizing.fontSize(base: 13, sizeClass: sc), weight: .semibold))
                        .foregroundColor(AppTheme.Colors.greenFallback)
                }
                .padding(.horizontal, pad.xs)
                .padding(.top, 8)
            }
        }
    }
    private func submitForReview() {
            guard !essayText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
            isLoading = true
            errorMessage = nil
            Task {
                do {
                    let result = try await aiService.analyzeWriting(essay: essayText, prompt: currentPrompt)
                    feedbackTitle = result.feedbackTitle
                    feedbackMessage = result.feedbackMessage
                    suggestions = result.suggestions.map {
                        GrammarSuggestion(original: $0.original, corrected: $0.corrected,
                                           type: $0.type, line: $0.line,
                                           severity: $0.severity == "error" ? .error : .warning)
                    }
                    hasReceivedFeedback = true
                } catch {
                    errorMessage = "Couldn't check your writing. Please try again."
                }
                isLoading = false
            }
        }
    
    // MARK: - Action Buttons
    private var actionButtons: some View {
        let pad = AppSizing.Padding.forClass(sc)
        let btn = AppSizing.Button.forClass(sc)
        return HStack(spacing: pad.sm) {
            Button { submitForReview() } label: {
                HStack(spacing: 8) {
                    if isLoading {
                        ProgressView()
                            .tint(.white)
                    } else {
                        Image(systemName: "paperplane.fill")
                            .font(.system(size: AppSizing.fontSize(base: 14, sizeClass: sc)))
                        Text("Submit")
                            .font(AppTheme.Typography.headline(sc))
                            .fontWeight(.semibold)
                    }
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: btn.height)
                .background(AppTheme.Colors.primaryFallback)
                .cornerRadius(btn.cornerRadius)
                .shadow(
                    color: AppTheme.Colors.primaryFallback.opacity(0.35),
                    radius: 10, x: 0, y: 5
                )
            }
            .buttonStyle(.plain)
            .disabled(isLoading)
        }
    }

    // MARK: - AI Feedback Section
    private var aiFeedbackSection: some View {
        let pad = AppSizing.Padding.forClass(sc)
        return VStack(alignment: .leading, spacing: pad.sm) {
            Text("AI feedback")
                .font(.system(size: AppSizing.fontSize(base: 17, sizeClass: sc), weight: .bold))
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)

            HStack(alignment: .top, spacing: pad.md) {
                ZStack {
                    RoundedRectangle(cornerRadius: 10)
                        .fill(AppTheme.Colors.greenFallback.opacity(0.15))
                        .frame(
                            width: AppSizing.iconSize(base: 40, sizeClass: sc),
                            height: AppSizing.iconSize(base: 40, sizeClass: sc)
                        )
                    Image(systemName: "sparkles")
                        .font(.system(size: AppSizing.fontSize(base: 18, sizeClass: sc)))
                        .foregroundColor(AppTheme.Colors.greenFallback)
                }

                VStack(alignment: .leading, spacing: 5) {
                    Text(feedbackTitle)
                        .font(AppTheme.Typography.headline(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    Text(feedbackMessage)
                        .font(AppTheme.Typography.subheadline(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            .padding(pad.md)
            .background(AppTheme.Colors.greenFallback.opacity(0.06))
            .cornerRadius(AppSizing.cardRadius(sc))
            .overlay(
                RoundedRectangle(cornerRadius: AppSizing.cardRadius(sc))
                    .stroke(AppTheme.Colors.greenFallback.opacity(0.20), lineWidth: 1)
            )
        }
    }

    // MARK: - Grammar Suggestions Section
    private var grammarSuggestionsSection: some View {
        let pad = AppSizing.Padding.forClass(sc)
        return VStack(alignment: .leading, spacing: pad.sm) {
            Text("Grammar suggestions")
                .font(.system(size: AppSizing.fontSize(base: 17, sizeClass: sc), weight: .bold))
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)

            ForEach(suggestions.indices, id: \.self) { i in
                HStack(spacing: pad.md) {
                    // Severity icon
                    Image(systemName: "exclamationmark.circle.fill")
                        .font(.system(size: AppSizing.fontSize(base: 20, sizeClass: sc)))
                        .foregroundColor(
                            suggestions[i].severity == .error
                            ? AppTheme.Colors.red
                            : AppTheme.Colors.orangeFallback
                        )
                        .frame(width: 28)

                    VStack(alignment: .leading, spacing: 3) {
                        Text("\(suggestions[i].type) · line \(suggestions[i].line)")
                            .font(AppTheme.Typography.subheadline(sc))
                            .foregroundColor(Color.red)

                        if suggestions[i].severity == .error {
                            Text("You wrote: \"\(suggestions[i].original)\"")
                                .font(AppTheme.Typography.subheadline(sc))
                                .foregroundColor(AppTheme.Colors.textPrimaryFallback)

                            Text("Should be: \"\(suggestions[i].corrected)\"")
                                .font(AppTheme.Typography.subheadline(sc))
                                .foregroundColor(.green)
                        } else {
                            Text("\(suggestions[i].original) \(suggestions[i].corrected)")
                                .font(AppTheme.Typography.headline(sc))
                                .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                        }
                    }

                    Spacer()

                    Button {
                        suggestions[i].isAccepted.toggle()
                    } label: {
                        Image(systemName: "checkmark")
                            .font(.system(size: AppSizing.fontSize(base: 16, sizeClass: sc), weight: .semibold))
                            .foregroundColor(
                                suggestions[i].isAccepted
                                ? AppTheme.Colors.greenFallback
                                : AppTheme.Colors.borderFallback
                            )
                    }
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
        }
    }
}

