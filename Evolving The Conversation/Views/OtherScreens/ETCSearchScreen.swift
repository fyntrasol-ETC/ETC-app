import SwiftUI

// MARK: - Search Screen
struct ETCSearchScreen: View {
    @Binding var isPresented: Bool
    @Environment(\.appSizeClass) var sc
    @EnvironmentObject private var authVM: AuthViewModel

    @State private var searchText = ""
    @FocusState private var isFocused: Bool

    // Live catalog matches for the current searchText (course + lesson hits)
    @State private var results: [SearchResult] = []

    // Live matches against the Profile tab's screens (My Progress, etc.)
    @State private var screenResults: [AppScreenDestination] = []

    // Last 5 terms, synced via Firestore — populated on appear
    @State private var recentSearches: [String] = []

    // Navigation targets
    @State private var detailCourse: Course? = nil
    @State private var videoTarget: VideoTarget? = nil
    @State private var activeScreen: AppScreenDestination? = nil

    // Only used to satisfy SettingsScreen's showTabBar binding when it's
    // opened from here — the search screen is already full-screen and
    // modal, so tab-bar visibility underneath doesn't matter.
    @State private var settingsTabBarStub = true

    @ObservedObject private var progress = LessonProgressService.shared

    private struct VideoTarget: Identifiable {
        let course: Course
        let lesson: CourseLesson
        var id: String { lesson.id }
    }

    private var courseResults: [SearchResult] { results.filter { $0.kind == .course } }
    private var lessonResults: [SearchResult] { results.filter { $0.kind == .lesson } }

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        VStack(spacing: 0) {
            searchBar(pad: pad)
            Divider()

            if searchText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                recentSection(pad: pad)
            } else if results.isEmpty && screenResults.isEmpty {
                emptyResultsView(pad: pad)
            } else {
                resultsSection(pad: pad)
            }

            Spacer()
        }
        .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
        .onAppear {
            isFocused = true
            refreshRecent()
        }
        .onChange(of: searchText) { newValue in
            results = SearchService.shared.search(newValue)
            screenResults = SearchService.shared.searchScreens(newValue)
        }
        .fullScreenCover(item: $detailCourse) { course in
            CourseDetailScreen(course: course, onBack: { detailCourse = nil })
                .appSizeClass()
        }
        .fullScreenCover(item: $videoTarget) { target in
            VideoLessonScreen(course: target.course, lesson: target.lesson, onBack: { videoTarget = nil })
                .appSizeClass()
        }
        .fullScreenCover(item: $activeScreen) { screen in
            destinationView(for: screen)
        }
    }

    @ViewBuilder
    private func destinationView(for screen: AppScreenDestination) -> some View {
        switch screen {
        case .myProgress:
            MyProgressScreen().appSizeClass()
        case .certificates:
            CertificatesScreen().appSizeClass()
        case .premiumPlans:
            PremiumPlansScreen().appSizeClass()
        case .settings:
            SettingsScreen(showTabBar: $settingsTabBarStub)
                .environmentObject(authVM)
                .appSizeClass()
        case .helpCenter:
            HelpCenterScreen().appSizeClass()
        case .speakingPractice:
            SpeakingPracticeScreen(onBack: { activeScreen = nil }).appSizeClass()
        case .listeningPractice:
            ListeningPracticeScreen(onBack: { activeScreen = nil }).appSizeClass()
        case .writingPractice:
            WritingPracticeScreen(onBack: { activeScreen = nil }).appSizeClass()
        case .grammar:
            GrammarScreen(onBack: { activeScreen = nil }).appSizeClass()
        case .vocabulary:
            VocabularyLevelsScreen(onBack: { activeScreen = nil }).appSizeClass()
        case .quizCenter:
            QuizScreen(title: "General", category: "Grammar", onBack: { activeScreen = nil })
                .appSizeClass()
        case .changePassword:
            ChangePasswordScreen()
                .environmentObject(authVM)
                .appSizeClass()
        case .deleteAccount:
            DeleteAccountScreen()
                .environmentObject(authVM)
                .appSizeClass()
        case .dailyChallenge:
            DailyChallengeScreen(onBack: { activeScreen = nil })
                .environmentObject(authVM)
                .appSizeClass()
        case .liveClasses:
            LiveClassesScreen(onBack: { activeScreen = nil })
                .environmentObject(authVM)
                .appSizeClass()
        case .aiTutor:
            AIConversationScreen(onBack: { activeScreen = nil })
                .environmentObject(authVM)
                .appSizeClass()
        }
    }

    // MARK: - Search bar

    @ViewBuilder
    private func searchBar(pad: AppSizing.Padding) -> some View {
        HStack(spacing: 12) {
            Button { isPresented = false } label: {
                let size: CGFloat = AppSizing.iconSize(base: 40, sizeClass: sc)
                Image(systemName: "chevron.left")
                    .font(.system(size: AppSizing.fontSize(base: 16, sizeClass: sc), weight: .semibold))
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    .frame(width: size, height: size)
                    .background(AppTheme.Colors.surface)
                    .cornerRadius(size * 0.26)
                    .shadow(color: .black.opacity(0.06), radius: 4, y: 2)
            }
            .buttonStyle(.plain)

            HStack(spacing: 10) {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    .font(.system(size: AppSizing.fontSize(base: 16, sizeClass: sc)))

                TextField("Search courses, lessons, words...", text: $searchText)
                    .font(AppTheme.Typography.body(sc))
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    .focused($isFocused)
                    .submitLabel(.search)
                    .onSubmit { commitSearch(searchText) }

                if !searchText.isEmpty {
                    Button { searchText = "" ; results = [] ; screenResults = [] } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    }
                }
            }
            .padding(.horizontal, 14)
            .frame(height: AppSizing.TextField.forClass(sc).height)
            .background(AppTheme.Colors.surface)
            .overlay(
                RoundedRectangle(cornerRadius: AppSizing.TextField.forClass(sc).cornerRadius)
                    .stroke(AppTheme.Colors.borderFallback, lineWidth: 1)
            )
            .cornerRadius(AppSizing.TextField.forClass(sc).cornerRadius)
        }
        .padding(.horizontal, pad.lg)
        .padding(.vertical, pad.md)
        .background(AppTheme.Colors.surface)
        .shadow(color: .black.opacity(0.04), radius: 8, x: 0, y: 2)
    }

    // MARK: - Recent Searches

    @ViewBuilder
    private func recentSection(pad: AppSizing.Padding) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("RECENT")
                .font(.system(size: AppSizing.fontSize(base: 12, sizeClass: sc), weight: .semibold))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                .tracking(1.2)
                .padding(.horizontal, pad.lg)
                .padding(.top, pad.lg)
                .padding(.bottom, pad.sm)

            if recentSearches.isEmpty {
                Text("Search anything...")
                    .font(AppTheme.Typography.body(sc))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    .padding(.horizontal, pad.lg)
                    .padding(.top, pad.sm)
            } else {
                VStack(spacing: 0) {
                    ForEach(recentSearches, id: \.self) { term in
                        Button {
                            searchText = term
                            results = SearchService.shared.search(term)
                            screenResults = SearchService.shared.searchScreens(term)
                        } label: {
                            HStack(spacing: 14) {
                                Image(systemName: "clock")
                                    .font(.system(size: AppSizing.fontSize(base: 16, sizeClass: sc)))
                                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                                    .frame(width: 24)

                                Text(term)
                                    .font(AppTheme.Typography.body(sc))
                                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)

                                Spacer()

                                Image(systemName: "arrow.up.right")
                                    .font(.system(size: AppSizing.fontSize(base: 14, sizeClass: sc)))
                                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                            }
                            .padding(.horizontal, pad.lg)
                            .padding(.vertical, 16)
                            .contentShape(Rectangle())
                        }

                        if term != recentSearches.last {
                            Divider()
                                .padding(.leading, pad.lg + 24 + 14)
                        }
                    }
                }
            }
        }
    }

    // MARK: - Results

    @ViewBuilder
    private func resultsSection(pad: AppSizing.Padding) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                if !screenResults.isEmpty {
                    sectionHeader("QUICK LINKS", pad: pad)
                    VStack(spacing: 0) {
                        ForEach(screenResults) { screen in
                            screenRow(screen, pad: pad)
                            if screen.id != screenResults.last?.id {
                                Divider().padding(.leading, pad.lg)
                            }
                        }
                    }
                }

                if !courseResults.isEmpty {
                    sectionHeader("COURSES", pad: pad)
                    VStack(spacing: 0) {
                        ForEach(courseResults) { result in
                            courseRow(result.course, pad: pad)
                            if result.id != courseResults.last?.id {
                                Divider().padding(.leading, pad.lg)
                            }
                        }
                    }
                }

                if !lessonResults.isEmpty {
                    sectionHeader("LESSONS", pad: pad)
                    VStack(spacing: 0) {
                        ForEach(lessonResults) { result in
                            if let lesson = result.lesson {
                                lessonRow(result.course, lesson, pad: pad)
                                if result.id != lessonResults.last?.id {
                                    Divider().padding(.leading, pad.lg)
                                }
                            }
                        }
                    }
                }
            }
            .padding(.bottom, pad.xl)
        }
    }

    @ViewBuilder
    private func sectionHeader(_ title: String, pad: AppSizing.Padding) -> some View {
        Text(title)
            .font(.system(size: AppSizing.fontSize(base: 12, sizeClass: sc), weight: .semibold))
            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
            .tracking(1.2)
            .padding(.horizontal, pad.lg)
            .padding(.top, pad.lg)
            .padding(.bottom, pad.sm)
    }

    @ViewBuilder
    private func screenRow(_ screen: AppScreenDestination, pad: AppSizing.Padding) -> some View {
        Button {
            commitSearch(searchText)
            activeScreen = screen
        } label: {
            HStack(spacing: 14) {
                ZStack {
                    Circle()
                        .fill(AppTheme.Colors.primaryLightFallback)
                        .frame(width: 40, height: 40)
                    Image(systemName: screen.icon)
                        .foregroundColor(AppTheme.Colors.primaryFallback)
                        .font(.system(size: AppSizing.fontSize(base: 16, sizeClass: sc)))
                }

                Text(screen.rawValue)
                    .font(AppTheme.Typography.body(sc))
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(size: AppSizing.fontSize(base: 12, sizeClass: sc)))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
            }
            .padding(.horizontal, pad.lg)
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private func courseRow(_ course: Course, pad: AppSizing.Padding) -> some View {
        Button {
            commitSearch(searchText)
            checkEnrollmentAndNavigate(course: course, lesson: nil)
        } label: {
            HStack(spacing: 14) {
                RoundedRectangle(cornerRadius: 10)
                    .fill(LinearGradient(colors: [course.accentColor, course.accentColorDark],
                                          startPoint: .topLeading, endPoint: .bottomTrailing))
                    .frame(width: 40, height: 40)
                    .overlay(
                        Image(systemName: course.icon)
                            .foregroundColor(.white)
                            .font(.system(size: AppSizing.fontSize(base: 16, sizeClass: sc)))
                    )

                VStack(alignment: .leading, spacing: 2) {
                    Text(course.title)
                        .font(AppTheme.Typography.body(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    Text("\(course.category) · \(course.author)")
                        .font(.system(size: AppSizing.fontSize(base: 11, sizeClass: sc)))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(size: AppSizing.fontSize(base: 12, sizeClass: sc)))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
            }
            .padding(.horizontal, pad.lg)
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private func lessonRow(_ course: Course, _ lesson: CourseLesson, pad: AppSizing.Padding) -> some View {
        Button {
            commitSearch(searchText)
            checkEnrollmentAndNavigate(course: course, lesson: lesson)
        } label: {
            HStack(spacing: 14) {
                Image(systemName: "play.circle.fill")
                    .font(.system(size: AppSizing.fontSize(base: 22, sizeClass: sc)))
                    .foregroundColor(course.accentColor)
                    .frame(width: 40)

                VStack(alignment: .leading, spacing: 2) {
                    Text(lesson.title)
                        .font(AppTheme.Typography.body(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    Text("\(course.title) · \(lesson.duration)")
                        .font(.system(size: AppSizing.fontSize(base: 11, sizeClass: sc)))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(size: AppSizing.fontSize(base: 12, sizeClass: sc)))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
            }
            .padding(.horizontal, pad.lg)
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    // MARK: - Empty / no results

    @ViewBuilder
    private func emptyResultsView(pad: AppSizing.Padding) -> some View {
        VStack(spacing: pad.md) {
            Spacer().frame(height: pad.xl)

            Image(systemName: "magnifyingglass")
                .font(.system(size: AppSizing.iconSize(base: 48, sizeClass: sc)))
                .foregroundColor(AppTheme.Colors.borderFallback)

            Text("No results for \(searchText)")
                .font(AppTheme.Typography.headline(sc))
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)

            Text("Try a different search term or browse by category.")
                .font(AppTheme.Typography.subheadline(sc))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                .multilineTextAlignment(.center)
                .padding(.horizontal, pad.xl)
        }
    }

    // MARK: - Helpers

    private func refreshRecent() {
        Task {
            let terms = await SearchService.shared.fetchRecentSearches()
            await MainActor.run { recentSearches = terms }
        }
    }

    private func commitSearch(_ term: String) {
        guard !term.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        SearchService.shared.recordSearch(term)
        refreshRecent()
    }

    /// Lesson hit: enrolled -> straight into VideoLessonScreen, not enrolled -> CourseDetailScreen.
    /// Course hit: always opens CourseDetailScreen.
    private func checkEnrollmentAndNavigate(course: Course, lesson: CourseLesson?) {
        guard let lesson else {
            detailCourse = course
            return
        }
        let isEnrolled = progress.enrollments[course.id] != nil
        if isEnrolled {
            videoTarget = VideoTarget(course: course, lesson: lesson)
        } else {
            detailCourse = course
        }
    }
}
