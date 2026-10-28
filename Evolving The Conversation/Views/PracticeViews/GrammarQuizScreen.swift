import SwiftUI

// MARK: - Grammar Quiz Screen (MCQ-only)
struct GrammarQuizScreen: View {
    let topic: String
    let onBack: () -> Void
    @Environment(\.appSizeClass) var sc

    @State private var questions: [GrammarMCQQuestion]
    @State private var currentIndex = 0
    @State private var selectedOption: Int? = nil
    @State private var score: Double = 0
    @State private var showResult = false

    init(topic: String, onBack: @escaping () -> Void) {
        self.topic = topic
        self.onBack = onBack
        _questions = State(initialValue: GrammarQuizData.buildQuiz(for: topic))
    }

    private var current: GrammarMCQQuestion { questions[currentIndex] }
    private var progress: Double { Double(currentIndex) / Double(max(questions.count, 1)) }

    var body: some View {
        if questions.isEmpty {
            emptyState
        } else if showResult {
            QuizResultScreen(
                score: score,
                total: questions.count,
                category: topic,
                onRetry: resetQuiz,
                onBack: onBack
            )
            .appSizeClass()
        } else {
            quizContent
        }
    }

    private func resetQuiz() {
        questions = GrammarQuizData.buildQuiz(for: topic)
        currentIndex = 0
        score = 0
        selectedOption = nil
        showResult = false
    }

    private func advance() {
        if currentIndex < questions.count - 1 {
            withAnimation {
                currentIndex += 1
                selectedOption = nil
            }
        } else {
            withAnimation { showResult = true }
            XPService.shared.recordActivity(.quiz)
            // NEW: bump today's "Grammar quizzes solved" count for the
            // Skill Breakdown card on MyProgressScreen. Fires exactly once
            // per finished quiz, same as the XP call above.
            SkillStatsService.shared.recordGrammarQuizSolved()
        }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Text("No questions available for \(topic) yet.")
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
            ETCButton(title: "Back") { onBack() }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
    }

    private var quizContent: some View {
        let pad = AppSizing.Padding.forClass(sc)

        return ZStack {
            AppTheme.Colors.backgroundFallback.ignoresSafeArea()

            VStack(spacing: 0) {
                HStack(spacing: AppSizing.scaled(12, sc)) {
                    ETCBackButton(action: onBack)
                    Text("Quiz · \(topic)")
                        .font(AppTheme.Typography.title2(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    Spacer()
                }
                .padding(.horizontal, pad.lg)
                .padding(.vertical, pad.md)
                .background(AppTheme.Colors.surface)
                ScrollView(showsIndicators: false) {
                    VStack(spacing: pad.lg) {

                        // Progress bar
                        HStack(spacing: pad.sm) {
                            GeometryReader { geo in
                                ZStack(alignment: .leading) {
                                    Capsule()
                                        .fill(AppTheme.Colors.borderFallback)
                                        .frame(height: AppSizing.scaled(6, sc))
                                    Capsule()
                                        .fill(
                                            LinearGradient(colors: [AppTheme.Colors.greenFallback, AppTheme.Colors.primaryFallback],
                                                           startPoint: .leading, endPoint: .trailing)
                                        )
                                        .frame(width: geo.size.width * progress,
                                               height: AppSizing.scaled(6, sc))
                                        .animation(.easeInOut, value: progress)
                                }
                            }
                            .frame(height: AppSizing.scaled(6, sc))

                            Text("\(currentIndex + 1)/\(questions.count)")
                                .font(AppTheme.Typography.caption(sc))
                                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                                .frame(minWidth: AppSizing.scaled(30, sc), alignment: .trailing)
                        }
                        .padding(.top, pad.md)

                        Text("MULTIPLE CHOICE")
                            .font(.system(size: AppSizing.fontSize(base: 11, sizeClass: sc), weight: .bold))
                            .foregroundColor(AppTheme.Colors.primaryFallback)
                            .tracking(1)
                            .padding(.horizontal, AppSizing.scaled(12, sc))
                            .padding(.vertical, AppSizing.scaled(5, sc))
                            .background(AppTheme.Colors.primaryLightFallback)
                            .clipShape(Capsule())
                            .frame(maxWidth: .infinity, alignment: .leading)

                        Text(current.question)
                            .font(.system(size: AppSizing.fontSize(base: 20, sizeClass: sc), weight: .bold))
                            .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                            .multilineTextAlignment(.leading)
                            .frame(maxWidth: .infinity, alignment: .leading)

                        VStack(spacing: AppSizing.scaled(10, sc)) {
                            ForEach(Array(current.options.enumerated()), id: \.offset) { i, option in
                                GrammarQuizOptionRow(
                                    text: option,
                                    index: i,
                                    selected: selectedOption,
                                    isCorrectOption: option == current.correctAnswer
                                ) {
                                    guard selectedOption == nil else { return }
                                    selectedOption = i
                                    if option == current.correctAnswer { score += 1 }
                                }
                            }
                        }

                        if selectedOption != nil {
                            ETCButton(title: currentIndex < questions.count - 1 ? "Next Question" : "See Results") {
                                advance()
                            }
                            .transition(.move(edge: .bottom).combined(with: .opacity))
                        }
                    }
                    .padding(.horizontal, pad.lg)
                    .padding(.bottom, 40)
                    .frame(maxWidth: AppSizing.maxContentWidth(sc))
                    .frame(maxWidth: .infinity)
                }
            }
        }
        .id(current.id)
    }
}

// MARK: - Grammar Quiz Option Row
private struct GrammarQuizOptionRow: View {
    let text: String
    let index: Int
    let selected: Int?
    let isCorrectOption: Bool
    let action: () -> Void
    @Environment(\.appSizeClass) var sc

    private enum OptionState { case idle, correct, wrong }

    private var state: OptionState {
        guard selected != nil else { return .idle }
        if isCorrectOption { return .correct }
        if index == selected { return .wrong }
        return .idle
    }

    private var bgColor: Color {
        switch state {
        case .idle: return AppTheme.Colors.surface
        case .correct: return Color(red: 0.90, green: 0.97, blue: 0.93)
        case .wrong:   return Color(red: 1.0, green: 0.93, blue: 0.93)
        }
    }

    private var borderColor: Color {
        switch state {
        case .idle:    return AppTheme.Colors.borderFallback
        case .correct: return AppTheme.Colors.greenFallback
        case .wrong:   return Color(red: 0.878, green: 0.224, blue: 0.224)
        }
    }

    private var textColor: Color {
        switch state {
        case .idle:    return AppTheme.Colors.textPrimaryFallback
        case .correct: return AppTheme.Colors.greenFallback
        case .wrong:   return Color(red: 0.878, green: 0.224, blue: 0.224)
        }
    }

    var body: some View {
        Button(action: action) {
            HStack {
                Text(text)
                    .font(AppTheme.Typography.body(sc))
                    .foregroundColor(textColor)
                    .frame(maxWidth: .infinity, alignment: .leading)

                if state == .correct {
                    Image(systemName: "checkmark.circle")
                        .font(.system(size: AppSizing.fontSize(base: 18, sizeClass: sc)))
                        .foregroundColor(AppTheme.Colors.greenFallback)
                } else if state == .wrong {
                    Image(systemName: "xmark.circle")
                        .font(.system(size: AppSizing.fontSize(base: 18, sizeClass: sc)))
                        .foregroundColor(Color(red: 0.878, green: 0.224, blue: 0.224))
                }
            }
            .padding(.horizontal, AppSizing.Padding.forClass(sc).md)
            .padding(.vertical, AppSizing.scaled(16, sc))
            .background(bgColor)
            .cornerRadius(AppSizing.cardRadius(sc))
            .overlay(
                RoundedRectangle(cornerRadius: AppSizing.cardRadius(sc))
                    .stroke(borderColor, lineWidth: state == .idle ? 1 : 1.5)
            )
        }
        .buttonStyle(.plain)
        .disabled(selected != nil)
    }
}
