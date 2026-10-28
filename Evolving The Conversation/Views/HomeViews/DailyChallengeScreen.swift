import SwiftUI

struct DailyChallengeScreen: View {
    var onBack: (() -> Void)? = nil
    @Environment(\.appSizeClass) var sc
    @EnvironmentObject private var authVM: AuthViewModel

    @ObservedObject private var service = DailyChallengeService.shared
    @ObservedObject private var progressService = LessonProgressService.shared
    @State private var timeRemaining: String = "24:00:00"
    @State private var countdownTimer: Timer? = nil
    @State private var activeDestination: DailyChallengeType? = nil

    // Any course the user is currently enrolled in and hasn't finished yet —
    // used to decide where "Go to course" for the watchLesson challenge
    // should land. We no longer try to auto-pick a specific lesson; the
    // user picks it themselves from the course's curriculum, same as any
    // other lesson-watching flow in the app.
    private var activeCourseForChallenge: Course? {
        for course in CourseCatalog.all {
            if let enrollment = progressService.enrollments[course.id] {
                let pct = enrollment.progress(for: course)
                if pct < 1.0 { return course }
            }
        }
        return nil
    }

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        VStack(spacing: 0) {
            ETCScreenBackBar(title: "Daily Challenge") { onBack?() }

            ScrollView(showsIndicators: false) {
                VStack(spacing: pad.lg) {
                    if let challenge = service.challenge {
                        if challenge.isCompleted {
                            completedCard
                        } else {
                            challengeCard(challenge)
                        }
                    } else {
                        ProgressView("Loading today's challenge…")
                            .padding(.top, 60)
                    }
                }
                .padding(.horizontal, pad.lg)
                .padding(.top, pad.lg)
                .padding(.bottom, 60)
                .frame(maxWidth: AppSizing.maxContentWidth(sc))
                .frame(maxWidth: .infinity)
            }
        }
        .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
        .onAppear {
            service.listen()
            progressService.listenAll()
        }
        .fullScreenCover(item: $activeDestination) { type in
            destinationView(for: type)
        }
    }

    // MARK: - Not-yet-completed state
    private func challengeCard(_ challenge: DailyChallenge) -> some View {
        let pad = AppSizing.Padding.forClass(sc)
        let type = challenge.type

        return VStack(spacing: pad.lg) {
            VStack(spacing: pad.md) {
                ZStack {
                    Circle()
                        .fill(AppTheme.Colors.primaryLightFallback)
                        .frame(width: AppSizing.iconSize(base: 88, sizeClass: sc),
                               height: AppSizing.iconSize(base: 88, sizeClass: sc))
                    Image(systemName: type.icon)
                        .font(.system(size: AppSizing.fontSize(base: 34, sizeClass: sc)))
                        .foregroundColor(AppTheme.Colors.primaryFallback)
                }

                Text("TODAY'S CHALLENGE")
                    .font(.system(size: AppSizing.fontSize(base: 11, sizeClass: sc), weight: .bold))
                    .foregroundColor(AppTheme.Colors.primaryFallback)
                    .tracking(1.2)

                Text(type.title)
                    .font(AppTheme.Typography.title2(sc))
                    .fontWeight(.bold)
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    .multilineTextAlignment(.center)

                Text(type.subtitle)
                    .font(AppTheme.Typography.body(sc))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    .multilineTextAlignment(.center)

                HStack(spacing: 6) {
                    Image(systemName: "bolt.fill")
                        .foregroundColor(AppTheme.Colors.orangeFallback)
                    Text(String(format: NSLocalizedString("+%d XP on completion", comment: ""), type.xpReward))                        .font(AppTheme.Typography.subheadline(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                }
            }
            .padding(pad.lg)
            .frame(maxWidth: .infinity)
            .background(AppTheme.Colors.surface)
            .cornerRadius(AppSizing.cardRadius(sc))
            .shadow(color: AppTheme.Shadow.card.color, radius: AppTheme.Shadow.card.radius,
                    x: AppTheme.Shadow.card.x, y: AppTheme.Shadow.card.y)

            // watchLesson always has somewhere to go now — either the
            // in-progress course's curriculum, or the Courses tab to
            // enroll in one first. No more disabled/blocked state.
            ETCButton(title: type == .watchLesson ? "Go to course" : "Do it now") {
                activeDestination = type
            }
        }
    }

    // MARK: - Completed state
    private var completedCard: some View {
        let pad = AppSizing.Padding.forClass(sc)
        return VStack(spacing: pad.lg) {
            ZStack {
                Circle()
                    .fill(AppTheme.Colors.greenFallback.opacity(0.15))
                    .frame(width: AppSizing.iconSize(base: 100, sizeClass: sc),
                           height: AppSizing.iconSize(base: 100, sizeClass: sc))
                Image(systemName: "checkmark.seal.fill")
                    .font(.system(size: AppSizing.fontSize(base: 44, sizeClass: sc)))
                .foregroundColor(AppTheme.Colors.greenFallback)
            }

            Text("🎉 Congrats!")
                .font(AppTheme.Typography.title1(sc))
                .fontWeight(.bold)
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)

            Text("You've completed today's challenge. Come back tomorrow for a new one!")
                .font(AppTheme.Typography.body(sc))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                .multilineTextAlignment(.center)

            // Countdown to next challenge
            VStack(spacing: 6) {
                Text("NEXT CHALLENGE IN")
                    .font(.system(size: AppSizing.fontSize(base: 11, sizeClass: sc), weight: .bold))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    .tracking(1.2)
                Text(timeRemaining)
                    .font(.system(size: AppSizing.fontSize(base: 28, sizeClass: sc), weight: .bold, design: .monospaced))
                    .foregroundColor(AppTheme.Colors.primaryFallback)
            }
            .padding(pad.md)
            .frame(maxWidth: .infinity)
            .background(AppTheme.Colors.primaryLightFallback)
            .cornerRadius(AppSizing.cardRadius(sc))

            ETCButton(title: "Back to Home", style: .secondary) { onBack?() }
        }
        .padding(.top, 60)
        .onAppear { startCountdown() }
        .onDisappear { countdownTimer?.invalidate() }
    }

    // MARK: - Countdown logic
    private func startCountdown() {
        updateTimeRemaining()
        countdownTimer?.invalidate()
        countdownTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
            updateTimeRemaining()
        }
    }

    private func updateTimeRemaining() {
        let calendar = Calendar.current
        let now = Date()
        guard let nextMidnight = calendar.nextDate(
            after: now,
            matching: DateComponents(hour: 0, minute: 0, second: 0),
            matchingPolicy: .nextTime
        ) else { return }

        let seconds = max(0, Int(nextMidnight.timeIntervalSince(now)))
        let h = seconds / 3600
        let m = (seconds % 3600) / 60
        let s = seconds % 60
        timeRemaining = String(format: "%02d:%02d:%02d", h, m, s)
    }

    // MARK: - Destination routing
    @ViewBuilder
    private func destinationView(for type: DailyChallengeType) -> some View {
        switch type {
        case .watchLesson:
            // Send the user into the actual course flow instead of guessing
            // a lesson for them. Completing any lesson from here — via
            // VideoLessonScreen's "Mark complete" — already calls
            // DailyChallengeService.markComplete(.watchLesson) globally, so
            // there's nothing extra to wire up on this screen's onBack.
            if let course = activeCourseForChallenge {
                CourseDetailScreen(course: course, onBack: { activeDestination = nil })
                    .environmentObject(authVM)
                    .appSizeClass()
            } else {
                // Not enrolled anywhere yet — let them browse and enroll.
                CoursesScreen(selectedTab: .constant(.courses), showTabBar: .constant(true))
                    .environmentObject(authVM)
                    .appSizeClass()
            }
        case .grammarQuiz:
            GrammarScreen(onBack: {
                activeDestination = nil
                service.markComplete(.grammarQuiz)
            }).appSizeClass()
        case .generalQuiz:
            QuizScreen(title: "Daily Quiz", category: "Mixed", onBack: {
                activeDestination = nil
                service.markComplete(.generalQuiz)
            }).appSizeClass()
        case .essayWriting:
            WritingPracticeScreen(onBack: {
                activeDestination = nil
                service.markComplete(.essayWriting)
            }).appSizeClass()
        case .aiSpeaking:
            AIConversationScreen(onBack: {
                activeDestination = nil
                service.markComplete(.aiSpeaking)
            })
            .environmentObject(authVM)
            .appSizeClass()
        }
    }
}
