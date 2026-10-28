import SwiftUI

// MARK: - Quiz Screen
struct QuizScreen: View {
    let title: String
    let category: String
    let onBack: () -> Void
    @Environment(\.appSizeClass) var sc
    @State private var shuffledRightOrder: [Int] = []
    @State private var questions: [QuizQuestion]
    @State private var currentIndex = 0
    @State private var showResult = false
    @State private var score: Double = 0

    // Per-question answer state — reset each time we move to a new question
    @State private var selectedOption: Int? = nil
    @State private var fillAnswer: String = ""
    @State private var fillSubmitted = false
    @State private var pairs: [Int: Int] = [:]
    @State private var pendingLeft: Int? = nil
    @State private var pendingRight: Int? = nil
    @State private var pairColors: [Int: Color] = [:]
    @State private var matchSubmitted = false

    init(title: String, category: String, onBack: @escaping () -> Void) {
        self.title = title
        self.category = category
        self.onBack = onBack
        let built = QuizData.buildQuiz()
        _questions = State(initialValue: built)
        _shuffledRightOrder = State(initialValue: Array(0..<built[0].rightItems.count).shuffled())
    }

    private var current: QuizQuestion { questions[currentIndex] }
    private var progress: Double { Double(currentIndex) / Double(questions.count) }

    private var hasAnswered: Bool {
        switch current.type {
        case .multipleChoice: return selectedOption != nil
        case .fillInTheBlank: return fillSubmitted
        case .matchThePairs:  return matchSubmitted
        }
    }

    var body: some View {
        if showResult {
            QuizResultScreen(
                score: score,
                total: questions.count,
                category: category,
                onRetry: resetQuiz,
                onBack: onBack
            )
            .appSizeClass()
        } else {
            quizContent
        }
    }

    private func resetQuiz() {
        questions = QuizData.buildQuiz()
        currentIndex = 0
        score = 0
        showResult = false
        resetAnswerState()
    }

    private func resetAnswerState() {
        selectedOption = nil
        fillAnswer = ""
        fillSubmitted = false
        pairs = [:]
        pendingLeft = nil
        pendingRight = nil
        pairColors = [:]
        matchSubmitted = false
        shuffledRightOrder = Array(0..<current.rightItems.count).shuffled()
    }

    private func advance() {
        if currentIndex < questions.count - 1 {
            withAnimation {
                currentIndex += 1
                resetAnswerState()
            }
        } else {
            withAnimation { showResult = true }
            XPService.shared.recordActivity(.quiz)   // ← add this
        }
    }

    private var badgeText: String {
        switch current.type {
        case .multipleChoice: return "MULTIPLE CHOICE"
        case .fillInTheBlank: return "FILL IN THE BLANK"
        case .matchThePairs:  return "MATCH THE PAIRS"
        }
    }

    private var quizContent: some View {
        let pad = AppSizing.Padding.forClass(sc)

        return ZStack {
            AppTheme.Colors.backgroundFallback.ignoresSafeArea()

            VStack(spacing: 0) {
                HStack(spacing: AppSizing.scaled(12, sc)) {
                    ETCBackButton(action: onBack)
                    Text("Quiz · \(category)")
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

                        Text(badgeText)
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

                        Group {
                            switch current.type {
                            case .multipleChoice: mcqBody
                            case .fillInTheBlank: fillBlankBody
                            case .matchThePairs:  matchPairsBody
                            }
                        }

                        if hasAnswered {
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
        // Ensures per-question @State (selectedOption, fillAnswer, pairs, etc.)
        // doesn't leak visually between very different question layouts.
        .id(current.id)
    }

    // MARK: - MCQ Body
    private var mcqBody: some View {
        VStack(spacing: AppSizing.scaled(10, sc)) {
            ForEach(Array(current.options.enumerated()), id: \.offset) { i, option in
                QuizOptionRow(
                    text: option,
                    index: i,
                    selected: selectedOption,
                    correctIndex: selectedOption != nil ? current.correctOptionIndex : nil
                ) {
                    guard selectedOption == nil else { return }
                    selectedOption = i
                    if i == current.correctOptionIndex { score += 1 }
                }
            }
        }
    }

    // MARK: - Fill in the Blank Body
    private var fillBlankBody: some View {
        let pad = AppSizing.Padding.forClass(sc)
        let isCorrect = fillAnswer.trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased() == current.blankAnswer.lowercased()

        return VStack(alignment: .leading, spacing: pad.sm) {
            HStack {
                TextField("Type your answer...", text: $fillAnswer)
                    .font(AppTheme.Typography.body(sc))
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    .disabled(fillSubmitted)
                    .padding(.horizontal, pad.md)
                    .padding(.vertical, AppSizing.scaled(14, sc))

                if fillSubmitted {
                    Image(systemName: isCorrect ? "checkmark" : "xmark")
                        .font(.system(size: AppSizing.fontSize(base: 15, sizeClass: sc), weight: .semibold))
                        .foregroundColor(isCorrect ? AppTheme.Colors.greenFallback
                                                    : Color(red: 0.878, green: 0.224, blue: 0.224))
                        .padding(.trailing, pad.md)
                }
            }
            .background(AppTheme.Colors.surface)
            .cornerRadius(AppSizing.cardRadius(sc))
            .overlay(
                RoundedRectangle(cornerRadius: AppSizing.cardRadius(sc))
                    .stroke(
                        fillSubmitted
                        ? (isCorrect ? AppTheme.Colors.greenFallback : Color(red: 0.878, green: 0.224, blue: 0.224))
                        : AppTheme.Colors.primaryFallback,
                        lineWidth: 1.5
                    )
            )

            if fillSubmitted {
                Text(isCorrect ? "Correct!" : "Correct answer: \(current.blankAnswer)")
                    .font(AppTheme.Typography.subheadline(sc))
                    .foregroundColor(isCorrect ? AppTheme.Colors.greenFallback
                                                : Color(red: 0.878, green: 0.224, blue: 0.224))
                    .padding(.horizontal, pad.sm)
                    .padding(.vertical, AppSizing.scaled(8, sc))
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        (isCorrect ? AppTheme.Colors.greenFallback : Color(red: 0.878, green: 0.224, blue: 0.224))
                            .opacity(0.1)
                    )
                    .cornerRadius(AppSizing.scaled(8, sc))
            } else {
                ETCButton(title: "Submit") {
                    fillSubmitted = true
                    if isCorrect { score += 1 }
                }
                .disabled(fillAnswer.trimmingCharacters(in: .whitespaces).isEmpty)
                .opacity(fillAnswer.trimmingCharacters(in: .whitespaces).isEmpty ? 0.5 : 1)
            }
        }
    }

    // MARK: - Match the Pairs Body
    private let matchPalette: [Color] = [
        Color(red: 0.294, green: 0.357, blue: 0.961), // blue
        Color(red: 0.176, green: 0.741, blue: 0.494), // green
        Color(red: 0.961, green: 0.651, blue: 0.137), // orange
    ]

    private var rightDisplayOrder: [Int] {
        shuffledRightOrder.count == current.rightItems.count
            ? shuffledRightOrder
            : Array(0..<current.rightItems.count)
    }

    private var matchPairsBody: some View {
        let pad = AppSizing.Padding.forClass(sc)
        let allPaired = pairs.count == current.leftItems.count

        return VStack(spacing: pad.sm) {
            HStack(alignment: .top, spacing: pad.sm) {
                VStack(spacing: pad.sm) {
                    ForEach(Array(current.leftItems.enumerated()), id: \.offset) { i, text in
                        MatchItemButton(text: text, state: matchState(forLeft: i), color: pairColors[i]) {
                            handleLeftTap(i)
                        }
                    }
                }
                VStack(spacing: pad.sm) {
                    ForEach(Array(rightDisplayOrder.enumerated()), id: \.offset) { _, originalIndex in
                        MatchItemButton(
                            text: current.rightItems[originalIndex],
                            state: matchState(forRight: originalIndex),
                            color: color(forRight: originalIndex)
                        ) {
                            handleRightTap(originalIndex)
                        }
                    }
                }
            }

            if matchSubmitted {
                let correctCount = pairs.filter { current.correctPairs[$0.key] == $0.value }.count
                Text("\(correctCount) of \(current.leftItems.count) pairs correct")
                    .font(AppTheme.Typography.subheadline(sc))
                    .foregroundColor(correctCount == current.leftItems.count
                                      ? AppTheme.Colors.greenFallback
                                      : Color(red: 0.878, green: 0.224, blue: 0.224))
            } else {
                ETCButton(title: allPaired ? "Submit" : "Match all pairs to continue") {
                    guard allPaired else { return }
                    matchSubmitted = true
                    let correctCount = pairs.filter { current.correctPairs[$0.key] == $0.value }.count
                    score += Double(correctCount) / Double(current.leftItems.count)
                }
                .disabled(!allPaired)
                .opacity(allPaired ? 1 : 0.5)
            }
        }
    }
    private func handleLeftTap(_ index: Int) {
        guard !matchSubmitted else { return }
        if pairs[index] != nil {
            pairs.removeValue(forKey: index)
            pairColors.removeValue(forKey: index)
            return
        }
        if let right = pendingRight {
            makePair(left: index, right: right)
            pendingRight = nil
        } else {
            pendingLeft = index
        }
    }

    private func handleRightTap(_ index: Int) {
        guard !matchSubmitted else { return }
        if let leftKey = pairs.first(where: { $0.value == index })?.key {
            pairs.removeValue(forKey: leftKey)
            pairColors.removeValue(forKey: leftKey)
            return
        }
        if let left = pendingLeft {
            makePair(left: left, right: index)
            pendingLeft = nil
        } else {
            pendingRight = index
        }
    }

    private func makePair(left: Int, right: Int) {
        pairs[left] = right
        pairColors[left] = matchPalette[pairColors.count % matchPalette.count]
    }

    private func matchState(forLeft index: Int) -> MatchItemState {
        if matchSubmitted {
            guard let right = pairs[index] else { return .idle }
            return current.correctPairs[index] == right ? .correct : .wrong
        }
        if pairs[index] != nil { return .paired }
        if pendingLeft == index { return .pending }
        return .idle
    }

    private func matchState(forRight index: Int) -> MatchItemState {
        if matchSubmitted {
            guard let leftKey = pairs.first(where: { $0.value == index })?.key else { return .idle }
            return current.correctPairs[leftKey] == index ? .correct : .wrong
        }
        if pairs.values.contains(index) { return .paired }
        if pendingRight == index { return .pending }
        return .idle
    }

    private func color(forRight index: Int) -> Color? {
        guard let leftKey = pairs.first(where: { $0.value == index })?.key else { return nil }
        return pairColors[leftKey]
    }
}

// MARK: - Quiz Option Row (MCQ)
private struct QuizOptionRow: View {
    let text: String
    let index: Int
    let selected: Int?
    let correctIndex: Int?
    let action: () -> Void
    @Environment(\.appSizeClass) var sc

    private enum OptionState { case idle, correct, wrong }

    private var state: OptionState {
        guard let selected = selected, let correct = correctIndex else { return .idle }
        if index == correct { return .correct }
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

// MARK: - Match Item Button
private enum MatchItemState { case idle, pending, paired, correct, wrong }

private struct MatchItemButton: View {
    let text: String
    let state: MatchItemState
    let color: Color?
    let action: () -> Void
    @Environment(\.appSizeClass) var sc

    private var bgColor: Color {
        switch state {
        case .idle: return AppTheme.Colors.surface
        case .pending: return AppTheme.Colors.primaryLightFallback
        case .paired:  return (color ?? AppTheme.Colors.primaryFallback).opacity(0.12)
        case .correct: return Color(red: 0.90, green: 0.97, blue: 0.93)
        case .wrong:   return Color(red: 1.0, green: 0.93, blue: 0.93)
        }
    }

    private var borderColor: Color {
        switch state {
        case .idle:    return AppTheme.Colors.borderFallback
        case .pending: return AppTheme.Colors.primaryFallback
        case .paired:  return color ?? AppTheme.Colors.primaryFallback
        case .correct: return AppTheme.Colors.greenFallback
        case .wrong:   return Color(red: 0.878, green: 0.224, blue: 0.224)
        }
    }

    private var textColor: Color {
        switch state {
        case .idle:    return AppTheme.Colors.textPrimaryFallback
        case .pending: return AppTheme.Colors.primaryFallback
        case .paired:  return color ?? AppTheme.Colors.primaryFallback
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
                    .lineLimit(2)
                    .minimumScaleFactor(0.8)

                if state == .correct {
                    Image(systemName: "checkmark")
                        .foregroundColor(AppTheme.Colors.greenFallback)
                } else if state == .wrong {
                    Image(systemName: "xmark")
                        .foregroundColor(Color(red: 0.878, green: 0.224, blue: 0.224))
                }
            }
            .padding(.horizontal, AppSizing.Padding.forClass(sc).md)
            .padding(.vertical, AppSizing.scaled(14, sc))
            .background(bgColor)
            .cornerRadius(AppSizing.cardRadius(sc))
            .overlay(
                RoundedRectangle(cornerRadius: AppSizing.cardRadius(sc))
                    .stroke(borderColor, lineWidth: state == .idle ? 1 : 1.5)
            )
        }
        .buttonStyle(.plain)
    }
}
