import SwiftUI

// ─────────────────────────────────────────────────────────────
// MARK: - Speaking Practice Screen
// ─────────────────────────────────────────────────────────────

struct SpeakingPracticeScreen: View {
    var onBack: (() -> Void)? = nil

    @Environment(\.appSizeClass) var sc
    @State private var showAIConversation = false
    
    struct ConversationHistoryItem: Identifiable {
        let id = UUID()
        let title: String
        let score: Int
        let daysAgo: Int
    }
    static let prompts: [String] = [
        "Describe a place you'd love to visit and explain why it appeals to you.",
        "Talk about a book or movie that changed how you think.",
        "Describe your ideal weekend and what makes it relaxing for you.",
        "Talk about a challenge you overcame and what you learned from it.",
        "Describe a person who has influenced your life and explain how.",
        "Talk about a tradition from your culture that you find meaningful.",
        "Describe your dream job and why it appeals to you.",
        "Talk about a time you had to make a difficult decision."
    ]

    @State private var currentPrompt: String

    init(onBack: (() -> Void)? = nil) {
        self.onBack = onBack
        _currentPrompt = State(initialValue: Self.prompts.randomElement()!)
    }
    
    private let aiService = AIService()
        @StateObject private var audioRecorder = AudioRecorderManager()
        @State private var isProcessing = false
        @State private var confidenceScore = 82
        @State private var feedbackSummary = "Tap the mic and start speaking!"
        @State private var mistakes: [SpeakingFeedback.MistakeDTO] = []
        @State private var spokenTranscript: String = ""
        @State private var correctedText: String = ""
        @State private var hasResult = false
    
    private let history: [ConversationHistoryItem] = [
        ConversationHistoryItem(title: "Travel & holidays",      score: 92, daysAgo: 2),
        ConversationHistoryItem(title: "Job interview practice", score: 78, daysAgo: 2),
    ]

    var body: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                // Back bar
                ETCScreenBackBar(title: "Speaking Practice") {
                    onBack?()
                }

                ScrollView(showsIndicators: false) {
                    VStack(spacing: AppSizing.Padding.forClass(sc).md) {


                        // Today's speaking prompt
                        speakingPromptCard

                        // Mic record button
                        micSection

                        // Transcript + corrected line card
                        if hasResult {
                            transcriptCard
                        }

                        // Confidence score card
                        confidenceScoreCard


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
        .fullScreenCover(isPresented: $showAIConversation) {
            AIConversationScreen(onBack: { showAIConversation = false })
                .appSizeClass()
        }
    }

    // MARK: - Speaking Prompt Card
    private var speakingPromptCard: some View {
        let pad = AppSizing.Padding.forClass(sc)
        return VStack(alignment: .leading, spacing: pad.sm) {
            Text("Today's speaking prompt")
                .font(.system(size: AppSizing.fontSize(base: 17, sizeClass: sc), weight: .bold))
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)

            VStack(spacing: pad.sm) {
                Text("\"\(currentPrompt)\"")
                    .font(AppTheme.Typography.body(sc))
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.horizontal, pad.sm)
                
                HStack(spacing: 10) {
                    Text("2 min")
                        .font(.system(size: AppSizing.fontSize(base: 13, sizeClass: sc), weight: .semibold))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 5)
                        .background(AppTheme.Colors.backgroundFallback)
                        .clipShape(Capsule())
                }
            }
            .frame(maxWidth: .infinity)
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

    // MARK: - Mic Section
    private var micSection: some View {
        let size = AppSizing.iconSize(base: 80, sizeClass: sc)
        return VStack(spacing: 12) {
            Button {
                  if audioRecorder.isRecording {
                      handleStopRecording()
                  } else {
                      audioRecorder.requestPermission { granted in
                          guard granted else { return }
                          audioRecorder.startRecording()
                      }
                  }
              } label: {
                ZStack {
                    Circle()
                        .fill(AppTheme.Colors.primaryFallback)
                        .frame(width: size, height: size)
                        .shadow(
                            color: AppTheme.Colors.primaryFallback.opacity(0.35),
                            radius: 16, x: 0, y: 8
                        )
                    Image(systemName: audioRecorder.isRecording ? "stop.fill" : "mic.fill")
                        .font(.system(size: AppSizing.fontSize(base: 30, sizeClass: sc), weight: .medium))
                        .foregroundColor(.white)
                }
            }
            .buttonStyle(.plain)

            Text(audioRecorder.isRecording ? "Recording…" : "Tap to record")
                .font(AppTheme.Typography.subheadline(sc))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, AppSizing.Padding.forClass(sc).sm)
    }
    
    private func handleStopRecording() {
        guard let audioData = audioRecorder.stopRecording() else { return }
        isProcessing = true
        Task {
            do {
                let feedback = try await aiService.judgePronunciation(
                    audioData: audioData,
                    targetPrompt: currentPrompt
                )
                confidenceScore = feedback.confidenceScore
                feedbackSummary = feedback.summary
                mistakes = feedback.mistakes
                spokenTranscript = feedback.transcript
                correctedText = feedback.correctedText
                hasResult = true

                // NEW: this AI analysis just finished — record today's
                // Speaking confidence score for MyProgressScreen's Skill
                // Breakdown, and award Speaking XP once for this attempt
                // (each recording is a fresh practice attempt, not a
                // replay, so unlike video lessons it's fine — and
                // intended — for XP to be earned every time).
                SkillStatsService.shared.recordSpeakingConfidence(feedback.confidenceScore)
                XPService.shared.recordActivity(.aiSpeaking)
            } catch {
                feedbackSummary = "Couldn't analyze that recording — try again."
            }
            isProcessing = false
        }
    }

    // MARK: - Transcript Card (what you said vs. correct line)
    private var transcriptCard: some View {
        let pad = AppSizing.Padding.forClass(sc)
        return VStack(alignment: .leading, spacing: pad.sm) {
            Text("What you said")
                .font(AppTheme.Typography.subheadline(sc))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
            Text("\"\(spokenTranscript)\"")
                .font(AppTheme.Typography.body(sc))
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)

            Divider()

            Text("Correct line")
                .font(AppTheme.Typography.subheadline(sc))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
            Text("\"\(correctedText)\"")
                .font(AppTheme.Typography.body(sc))
                .foregroundColor(AppTheme.Colors.greenFallback)
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

    // MARK: - Confidence Score Card
    private var confidenceScoreCard: some View {
        let pad = AppSizing.Padding.forClass(sc)
        return VStack(alignment: .leading, spacing: pad.sm) {
            HStack(alignment: .center, spacing: pad.md) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Confidence score")
                        .font(AppTheme.Typography.subheadline(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    Text("\(confidenceScore)")
                        .font(.system(size: AppSizing.fontSize(base: 32, sizeClass: sc), weight: .bold))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    + Text("/100")
                        .font(.system(size: AppSizing.fontSize(base: 16, sizeClass: sc), weight: .regular))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                }

                VStack(alignment: .trailing, spacing: 8) {
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            Capsule()
                                .fill(AppTheme.Colors.borderFallback)
                                .frame(height: 10)
                            Capsule()
                                .fill(AppTheme.Colors.greenFallback)
                                .frame(width: geo.size.width * CGFloat(confidenceScore) / 100, height: 10)
                        }
                    }
                    .frame(height: 10)
                }
            }

            Text(feedbackSummary)
                .font(AppTheme.Typography.subheadline(sc))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)

            if isProcessing {
                HStack {
                    ProgressView()
                    Text("Analyzing…")
                        .font(AppTheme.Typography.subheadline(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                }
            }

            if !mistakes.isEmpty {
                VStack(alignment: .leading, spacing: pad.sm) {
                    ForEach(mistakes.indices, id: \.self) { i in
                        VStack(alignment: .leading, spacing: 3) {
                            Text(mistakes[i].issue)
                                .font(AppTheme.Typography.headline(sc))
                                .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                            Text(mistakes[i].tip)
                                .font(AppTheme.Typography.subheadline(sc))
                                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        }
                        .padding(.top, i == 0 ? pad.sm : 0)
                    }
                }
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
