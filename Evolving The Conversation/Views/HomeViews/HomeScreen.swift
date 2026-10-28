import SwiftUI

// MARK: - Home Screen
struct HomeScreen: View {
    @EnvironmentObject private var authVM: AuthViewModel
    @ObservedObject private var challengeService = DailyChallengeService.shared
    @State private var showDailyChallenge = false
    @Environment(\.appSizeClass) var sc
    @State private var showFeaturedCourse = false
    @Binding var selectedTab: ETCTab
    @State private var showLiveClasses = false
    @Binding var showTabBar: Bool
    @ObservedObject private var certService = CertificateService.shared
    @ObservedObject private var liveClassService = LiveClassService.shared

    @State private var showDrawer = false
    @State private var showSearch = false
    @State private var showNotifications = false
    @ObservedObject private var xpService = XPService.shared

    @State private var showSpeaking = false
    @State private var showListening = false
    @State private var showVideoLesson = false
    @State private var showGrammar = false
    @State private var showVocabulary = false
    @State private var showQuizzes = false
    @State private var showCertificates = false
    @State private var showProgress = false
    @State private var showSettings = false
    @State private var showHelpCenter = false

    @State private var liveClassTimer: Timer? = nil
    @State private var now = Date()

    @ObservedObject private var progressService = LessonProgressService.shared
    @State private var activeCourseForDetail: Course? = nil

    private var activeCourse: (course: Course, progress: Double)? {
        for course in CourseCatalog.all {
            if let enrollment = progressService.enrollments[course.id] {
                let pct = enrollment.progress(for: course)
                if pct > 0 && pct < 1.0 {
                    return (course, pct)
                }
            }
        }
        return nil
    }

    private func handleDrawerNavigation(
        _ destination: ETCDrawerMenu.DrawerDestination
    ) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.32) {
            switch destination {
            case .home: selectedTab = .home
            case .courses: selectedTab = .courses
            case .community: selectedTab = .community
            case .practice: selectedTab = .speak
            case .liveClasses: showLiveClasses = true
            case .videoLessons: showVideoLesson = true
            case .grammar: showGrammar = true
            case .vocabulary: showVocabulary = true
            case .quizzes: showQuizzes = true
            case .certificates: showCertificates = true
            case .progress: showProgress = true
            case .settings: showSettings = true
            case .helpCenter: showHelpCenter = true
            }
        }
    }

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        ZStack(alignment: .leading) {
            AppTheme.Colors.backgroundFallback
                .ignoresSafeArea()

            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    HomeNavHeader(
                        userName: firstName,
                        onDrawer: {
                            withAnimation(.easeInOut(duration: 0.28)) {
                                showDrawer = true
                            }
                        },
                        onSearch: { showSearch = true },
                        onNotification: { showNotifications = true },
                        notificationBadge: 3,
                        streak: xpService.streak,
                        xp: xpService.xp
                    )
                    .ignoresSafeArea(edges: .top)

                    VStack(spacing: pad.lg) {
                        VStack(spacing: pad.md) {
                               Group {
                                   if completedCoursesCount > 0 {
                                       HomeCompletedCoursesBadge(count: completedCoursesCount) {
                                           selectedTab = .courses
                                       }
                                   }
                                   if let active = activeCourse {
                                       HomeContinueLearningCard(
                                           progress: active.progress,
                                           courseTitle: active.course.title,
                                           onResume: { activeCourseForDetail = active.course }
                                       )
                                   } else {
                                       HomeNoCourseCard(onEnroll: { selectedTab = .courses })
                                   }
                               }

                           }
                           .offset(y: -(pad.lg + (sc == .regular ? 80 : 30)) * 0.6)
                           .padding(.bottom, -20)
                   

                        Button {
                            showDailyChallenge = true
                        } label: {
                            HomeDailyChallengeCard(
                                title: NSLocalizedString("Daily Challenge", comment: ""),
                                subtitle: challengeService.challenge?
                                    .isCompleted == true
                                    ? NSLocalizedString("Completed today ✓", comment: "")
                                    : NSLocalizedString("Tap to see today's challenge", comment: ""),
                                progress: challengeService.challenge?
                                    .isCompleted == true ? 1.0 : 0.0
                            )
                        }
                        .buttonStyle(.plain)

                        HomeCategoriesSection(
                            onGrammar: { showGrammar = true },
                            onVocabulary: { showVocabulary = true },
                            onSpeak: { showSpeaking = true },
                            onListening: { showListening = true }
                        )
                        HomeFeaturedCourseSection(
                            onSeeAll: { selectedTab = .courses },
                            onTap: { showFeaturedCourse = true }
                        )
                        HomeUpcomingLiveClass(
                            featuredClass: liveClassService.featuredClass(now: now),
                            now: now,
                            onTap: { showLiveClasses = true }
                        )
                        HomeMotivationCard(
                            quote: NSLocalizedString(
                                "\"The limits of my language are the limits of my world.\" Keep going — you're closer than yesterday.",
                                comment: ""
                            )
                        )
                    }
                    .padding(.horizontal, pad.lg)
                    .padding(.top, pad.lg)
                    .padding(.bottom, 100)
                    .frame(maxWidth: AppSizing.maxContentWidth(sc))
                    .frame(maxWidth: .infinity)
                }
            }
            .background(AppTheme.Colors.backgroundFallback)
            .ignoresSafeArea(edges: .top)

            if showDrawer {
                ETCDrawerMenu(isPresented: $showDrawer) { destination in
                    handleDrawerNavigation(destination)
                }
                .environmentObject(authVM)
                .zIndex(10)
                .transition(.move(edge: .leading))
            }
        }
        .overlay(alignment: .top) {
            if let earnedXP = xpService.justEarnedXP {
                XPEarnedToast(amount: earnedXP, sc: sc)
                    .padding(.top, AppSizing.scaled(140, sc))
                    .transition(.scale.combined(with: .opacity))
                    .zIndex(11)
            }
        }
        .onAppear {
            progressService.listenAll()
            challengeService.listen()
            certService.start()
            liveClassService.listen()
            startLiveClassTimer()
        }
        .onDisappear {
            liveClassTimer?.invalidate()
        }
        .fullScreenCover(isPresented: $showDailyChallenge) {
            DailyChallengeScreen(onBack: { showDailyChallenge = false })
                .environmentObject(authVM)
                .appSizeClass()
        }
        .fullScreenCover(isPresented: $showVideoLesson) {
            if let course = CourseCatalog.all.first(where: {
                $0.title == "Spoken English Mastery"
            }),
                let lesson = course.lessons.first(where: { $0.order == 13 })
            {
                VideoLessonScreen(
                    course: course,
                    lesson: lesson,
                    onBack: { showVideoLesson = false }
                ).appSizeClass()
            } else {
                EmptyView().onAppear { showVideoLesson = false }
            }
        }
        .fullScreenCover(isPresented: $showSpeaking) {
            SpeakingPracticeScreen(onBack: { showSpeaking = false })
                .appSizeClass()
        }
        .fullScreenCover(isPresented: $showListening) {
            ListeningPracticeScreen(onBack: { showListening = false })
                .appSizeClass()
        }
        .fullScreenCover(isPresented: $showGrammar) {
            GrammarScreen(onBack: { showGrammar = false }).appSizeClass()
        }
        .fullScreenCover(isPresented: $showVocabulary) {
            VocabularyLevelsScreen(onBack: { showVocabulary = false })
                .appSizeClass()
        }
        .fullScreenCover(isPresented: $showQuizzes) {
            QuizScreen(
                title: "Daily Quiz",
                category: "Mixed",
                onBack: { showQuizzes = false }
            ).appSizeClass()
        }
        .fullScreenCover(isPresented: $showLiveClasses) {
            LiveClassesScreen(onBack: { showLiveClasses = false })
                .appSizeClass()
        }
        .fullScreenCover(isPresented: $showCertificates) {
            CertificatesScreen().appSizeClass()
        }
        .fullScreenCover(isPresented: $showProgress) {
            MyProgressScreen().appSizeClass()
        }
        .fullScreenCover(isPresented: $showSettings) {
            SettingsScreen(showTabBar: $showTabBar).environmentObject(authVM)
                .appSizeClass()
        }
        .fullScreenCover(isPresented: $showHelpCenter) {
            HelpCenterScreen().appSizeClass()
        }
        .fullScreenCover(isPresented: $showSearch) {
            ETCSearchScreen(isPresented: $showSearch)
                .environmentObject(authVM).appSizeClass()
        }
        .fullScreenCover(isPresented: $showNotifications) {
            ETCNotificationsScreen(isPresented: $showNotifications)
                .appSizeClass()
        }
        .fullScreenCover(isPresented: $showFeaturedCourse) {
            if let course = CourseCatalog.all.first(where: {
                $0.title == "Spoken English Mastery"
            }) {
                CourseDetailScreen(
                    course: course,
                    onBack: { showFeaturedCourse = false }
                ).appSizeClass()
            } else {
                EmptyView().onAppear { showFeaturedCourse = false }
            }
        }
        .fullScreenCover(item: $activeCourseForDetail) { course in
            CourseDetailScreen(
                course: course,
                onBack: { activeCourseForDetail = nil }
            )
            .appSizeClass()
        }
        .onChange(of: showDrawer) { open in
            withAnimation { showTabBar = !open }
        }
    }

    private func startLiveClassTimer() {
        guard liveClassTimer == nil else { return }
        liveClassTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
            now = Date()
        }
    }

    private var completedCoursesCount: Int {
        CourseCatalog.all.reduce(0) { count, course in
            let pct =
                progressService.enrollments[course.id]?.progress(for: course)
                ?? 0
            return pct >= 1.0 ? count + 1 : count
        }
    }
    private var firstName: String {
        let name =
            authVM.currentUser?.displayName ?? authVM.currentUser?.email
            ?? "User"
        return name.components(separatedBy: " ").first
            ?? name.components(separatedBy: "@").first
            ?? "User"
    }
}

private struct XPEarnedToast: View {
    let amount: Int
    let sc: AppSizing.SizeClass

    var body: some View {
        VStack(spacing: 6) {
            Text("🎉")
                .font(.system(size: AppSizing.fontSize(base: 30, sizeClass: sc)))
            Text("Congratulations!")
                .font(.system(size: AppSizing.fontSize(base: 17, sizeClass: sc), weight: .bold))
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)
            Text(String(format: NSLocalizedString("You've earned +%d XP", comment: ""), amount))
                .font(AppTheme.Typography.subheadline(sc))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
        }
        .padding(.vertical, 20)
        .padding(.horizontal, 28)
        .background(AppTheme.Colors.surface)
        .cornerRadius(18)
        .shadow(color: .black.opacity(0.15), radius: 20, y: 8)
    }
}

private struct HomeNavHeader: View {
    let userName: String
    let onDrawer: () -> Void
    let onSearch: () -> Void
    let onNotification: () -> Void
    var notificationBadge: Int = 0
    let streak: Int
    let xp: Int

    @Environment(\.appSizeClass) var sc

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        ZStack {
            Color(red: 0.169, green: 0.357, blue: 0.910)
                .frame(maxWidth: .infinity)
                .clipShape(UnevenRoundedRectangle(
                    topLeadingRadius: 0,
                    bottomLeadingRadius: 32,
                    bottomTrailingRadius: 32,
                    topTrailingRadius: 0))
                .ignoresSafeArea(edges: .top)

            VStack(spacing: 0) {
                ZStack {
                    Text("Good Morning,")
                        .font(AppTheme.Typography.title2(sc))
                        .foregroundColor(.white)
                        .fontWeight(.bold)
                        .frame(maxWidth: .infinity, alignment: .center)

                    HStack {
                        Button(action: onDrawer) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(Color.white.opacity(0.12))
                                    .frame(width: AppSizing.iconSize(base: 40, sizeClass: sc),
                                           height: AppSizing.iconSize(base: 40, sizeClass: sc))
                                Image(systemName: "line.3.horizontal")
                                    .font(.system(size: AppSizing.fontSize(base: 18, sizeClass: sc), weight: .medium))
                                    .foregroundColor(.white)
                            }
                        }

                        Spacer()

                        HStack(spacing: 8) {
                            Button(action: onSearch) {
                                ZStack {
                                    RoundedRectangle(cornerRadius: 12)
                                        .fill(Color.white.opacity(0.12))
                                        .frame(width: AppSizing.iconSize(base: 40, sizeClass: sc),
                                               height: AppSizing.iconSize(base: 40, sizeClass: sc))
                                    Image(systemName: "magnifyingglass")
                                        .font(.system(size: AppSizing.fontSize(base: 18, sizeClass: sc)))
                                        .foregroundColor(.white)
                                }
                            }
                            Button(action: onNotification) {
                                ZStack(alignment: .topTrailing) {
                                    ZStack {
                                        RoundedRectangle(cornerRadius: 12)
                                            .fill(Color.white.opacity(0.12))
                                            .frame(width: AppSizing.iconSize(base: 40, sizeClass: sc),
                                                   height: AppSizing.iconSize(base: 40, sizeClass: sc))
                                        Image(systemName: "bell")
                                            .font(.system(size: AppSizing.fontSize(base: 18, sizeClass: sc)))
                                            .foregroundColor(.white)
                                    }
                                    if notificationBadge > 0 {
                                        Circle().fill(Color.red).frame(width: 10, height: 10).offset(x: 2, y: -2)
                                    }
                                }
                            }
                        }
                    }
                }
                .padding(.horizontal, pad.lg)
                .padding(.top, 70)
                .padding(.bottom, 10)

                Text("\(userName) 👋")
                    .font(.system(size: AppSizing.fontSize(base: 28, sizeClass: sc), weight: .bold))
                    .foregroundColor(.white)
                    .padding(.top, 10)
                    .padding(.bottom, pad.md)

                HStack(spacing: pad.md) {
                    HomeStatCard(
                        icon: "flame.fill",
                        iconColor: Color(red: 1.0, green: 0.55, blue: 0.15),
                        value: "\(streak)",
                        label: NSLocalizedString("Day streak", comment: "")
                    )
                    HomeStatCard(
                        icon: "trophy.fill",
                        iconColor: Color(red: 0.961, green: 0.651, blue: 0.137),
                        value: "\(xp.formatted())",
                        label: NSLocalizedString("XP earned", comment: "")
                    )
                }
                .padding(.horizontal, pad.lg)
                .padding(.bottom, pad.lg + (sc == .regular ? 50 : 10))
                .padding(.top, 15)
            }
        }
    }
}

private struct HomeIconButton: View {
    let systemName: String
    var badge: Int = 0
    let action: () -> Void
    @Environment(\.appSizeClass) var sc

    var body: some View {
        let sz = AppSizing.iconSize(base: 40, sizeClass: sc)
        Button(action: action) {
            ZStack(alignment: .topTrailing) {
                Image(systemName: systemName)
                    .font(.system(size: AppSizing.fontSize(base: 18, sizeClass: sc), weight: .medium))
                    .foregroundColor(.white)
                    .frame(width: sz, height: sz)
                    .background(Color.white.opacity(0.20))
                    .cornerRadius(AppSizing.fontSize(base: 12, sizeClass: sc))
                if badge > 0 {
                    Circle()
                        .fill(Color(red: 0.957, green: 0.263, blue: 0.212))
                        .frame(width: 9, height: 9)
                        .offset(x: 1, y: -1)
                }
            }
        }
    }
}

private struct HomeDailyChallengeCard: View {
    let title: String
    let subtitle: String
    let progress: Double
    @Environment(\.appSizeClass) var sc

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        HStack(spacing: pad.md) {
            ZStack {
                Circle()
                    .fill(Color.white.opacity(0.4))
                    .frame(width: AppSizing.iconSize(base: 52, sizeClass: sc),
                           height: AppSizing.iconSize(base: 52, sizeClass: sc))
                Image("target")
                    .resizable()
                    .scaledToFit()
                    .frame(width: AppSizing.iconSize(base: 28, sizeClass: sc),
                           height: AppSizing.iconSize(base: 28, sizeClass: sc))
            }
            VStack(alignment: .leading, spacing: 6) {
                Text(title)
                    .font(.system(size: AppSizing.fontSize(base: 17, sizeClass: sc), weight: .bold))
                    .foregroundColor(.white)
                Text(subtitle)
                    .font(AppTheme.Typography.caption(sc))
                    .foregroundColor(.white.opacity(0.85))
            }
            Spacer(minLength: 0)
            Image(systemName: "chevron.right")
                .font(.system(size: AppSizing.fontSize(base: 14, sizeClass: sc), weight: .semibold))
                .foregroundColor(.white.opacity(0.85))
        }
        .padding(pad.md)
        .background(Color(red: 0.122, green: 0.686, blue: 0.478))
        .cornerRadius(AppSizing.cardRadius(sc))
    }
}

private struct HomeStatCard: View {
    let icon: String
    let iconColor: Color
    let value: String
    let label: String
    @Environment(\.appSizeClass) var sc

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        HStack(spacing: pad.sm) {
            Image(systemName: icon)
                .font(.system(size: AppSizing.fontSize(base: 22, sizeClass: sc)))
                .foregroundColor(iconColor)
            VStack(alignment: .leading, spacing: 2) {
                Text(value)
                    .font(.system(size: AppSizing.fontSize(base: 20, sizeClass: sc), weight: .bold))
                    .foregroundColor(.white)
                Text(label)
                    .font(AppTheme.Typography.caption(sc))
                    .foregroundColor(.white.opacity(0.80))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, pad.md)
        .padding(.vertical, pad.sm + 4)
        .background(Color.white.opacity(0.18))
        .cornerRadius(AppSizing.cardRadius(sc))
    }
}

private struct HomeContinueLearningCard: View {
    let progress: Double
    let courseTitle: String
    var onResume: () -> Void = {}
    @Environment(\.appSizeClass) var sc

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        HStack(spacing: pad.md) {
            ZStack {
                Circle()
                    .stroke(Color(red: 0.176, green: 0.741, blue: 0.494).opacity(0.20), lineWidth: 8)
                Circle()
                    .trim(from: 0, to: progress)
                    .stroke(Color(red: 0.176, green: 0.741, blue: 0.494),
                            style: StrokeStyle(lineWidth: 8, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                VStack(spacing: 1) {
                    Text("\(Int(progress * 100))%")
                        .font(.system(size: AppSizing.fontSize(base: 17, sizeClass: sc), weight: .bold))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    Text("done")
                        .font(AppTheme.Typography.caption(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                }
            }
            .frame(width: AppSizing.iconSize(base: 74, sizeClass: sc),
                   height: AppSizing.iconSize(base: 74, sizeClass: sc))

            VStack(alignment: .leading, spacing: 8) {
                Text("CONTINUE LEARNING")
                    .font(.system(size: AppSizing.fontSize(base: 11, sizeClass: sc), weight: .bold))
                    .foregroundColor(Color(red: 0.176, green: 0.741, blue: 0.494))
                    .tracking(0.8)
                Text(courseTitle)
                    .font(.system(size: AppSizing.fontSize(base: 16, sizeClass: sc), weight: .bold))
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    .fixedSize(horizontal: false, vertical: true)
                Button(action: onResume) {
                    HStack(spacing: 6) {
                        Image(systemName: "play.fill")
                            .font(.system(size: AppSizing.fontSize(base: 12, sizeClass: sc)))
                        Text("Resume")
                            .font(.system(size: AppSizing.fontSize(base: 14, sizeClass: sc), weight: .semibold))
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, pad.md)
                    .padding(.vertical, 8)
                    .background(AppTheme.Colors.primaryFallback)
                    .cornerRadius(AppSizing.Radius.pill)
                }
                .buttonStyle(.plain)
            }
            Spacer(minLength: 0)
        }
        .padding(pad.md)
        .background(AppTheme.Colors.surface)
        .cornerRadius(AppSizing.cardRadius(sc))
        .shadow(color: AppTheme.Shadow.card.color, radius: AppTheme.Shadow.card.radius, x: 0, y: AppTheme.Shadow.card.y)
    }
}

private struct HomeNoCourseCard: View {
    var onEnroll: () -> Void = {}
    @Environment(\.appSizeClass) var sc

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        HStack(spacing: pad.md) {
            ZStack {
                Circle()
                    .fill(AppTheme.Colors.primaryLightFallback)
                    .frame(width: AppSizing.iconSize(base: 74, sizeClass: sc),
                           height: AppSizing.iconSize(base: 74, sizeClass: sc))
                Image(systemName: "book.closed")
                    .font(.system(size: AppSizing.fontSize(base: 26, sizeClass: sc)))
                    .foregroundColor(AppTheme.Colors.primaryFallback)
            }

            VStack(alignment: .leading, spacing: 8) {
                Text("NO COURSE IN PROGRESS")
                    .font(.system(size: AppSizing.fontSize(base: 11, sizeClass: sc), weight: .bold))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    .tracking(0.8)
                Text("Enroll now to start learning")
                    .font(.system(size: AppSizing.fontSize(base: 16, sizeClass: sc), weight: .bold))
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    .fixedSize(horizontal: false, vertical: true)
                Button(action: onEnroll) {
                    HStack(spacing: 6) {
                        Image(systemName: "plus")
                            .font(.system(size: AppSizing.fontSize(base: 12, sizeClass: sc)))
                        Text("Enroll now")
                            .font(.system(size: AppSizing.fontSize(base: 14, sizeClass: sc), weight: .semibold))
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, pad.md)
                    .padding(.vertical, 8)
                    .background(AppTheme.Colors.primaryFallback)
                    .cornerRadius(AppSizing.Radius.pill)
                }
                .buttonStyle(.plain)
            }
            Spacer(minLength: 0)
        }
        .padding(pad.md)
        .background(AppTheme.Colors.surface)
        .cornerRadius(AppSizing.cardRadius(sc))
        .shadow(color: AppTheme.Shadow.card.color, radius: AppTheme.Shadow.card.radius, x: 0, y: AppTheme.Shadow.card.y)
    }
}

private struct HomeCategoriesSection: View {
    @Environment(\.appSizeClass) var sc
    let onGrammar: () -> Void
    let onVocabulary: () -> Void
    let onSpeak: () -> Void
    let onListening: () -> Void

    private struct Category {
        let icon: String
        let label: String
        let iconColor: Color
        let bgColor: Color
        let action: () -> Void
    }

    var body: some View {
        VStack(alignment: .leading, spacing: AppSizing.Padding.forClass(sc).md) {
            Text("Categories")
                .font(.system(size: AppSizing.fontSize(base: 20, sizeClass: sc), weight: .bold))
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)

            HStack(spacing: AppSizing.Padding.forClass(sc).sm) {
                HomeCategoryItem(
                    icon: "doc.text.fill",
                    label: NSLocalizedString("Grammar", comment: ""),
                    iconColor: Color(red: 0.169, green: 0.357, blue: 0.910),
                    bgColor: Color(red: 0.933, green: 0.945, blue: 0.992),
                    action: onGrammar
                )
                HomeCategoryItem(
                    icon: "sparkles",
                    label: NSLocalizedString("Vocabulary", comment: ""),
                    iconColor: Color(red: 0.122, green: 0.686, blue: 0.478),
                    bgColor: Color(red: 0.878, green: 0.965, blue: 0.929),
                    action: onVocabulary
                )
                HomeCategoryItem(
                    icon: "mic.fill",
                    label: NSLocalizedString("Speaking", comment: ""),
                    iconColor: Color(red: 0.961, green: 0.651, blue: 0.137),
                    bgColor: Color(red: 0.996, green: 0.949, blue: 0.871),
                    action: onSpeak
                )
                HomeCategoryItem(
                    icon: "headphones",
                    label: NSLocalizedString("Listening", comment: ""),
                    iconColor: Color(red: 0.482, green: 0.227, blue: 0.929),
                    bgColor: Color(red: 0.937, green: 0.910, blue: 0.988),
                    action: onListening
                )
            }
        }
    }
}
private struct HomeCategoryItem: View {
    let icon: String
    let label: String
    let iconColor: Color
    let bgColor: Color
    let action: () -> Void
    @Environment(\.appSizeClass) var sc

    var body: some View {
        let boxSize = AppSizing.iconSize(base: 56, sizeClass: sc)
        Button(action: action) {
            VStack(spacing: 8) {
                ZStack {
                    RoundedRectangle(cornerRadius: 16).fill(bgColor)
                        .frame(width: boxSize, height: boxSize)
                    Image(systemName: icon)
                        .font(.system(size: AppSizing.fontSize(base: 22, sizeClass: sc)))
                        .foregroundColor(iconColor)
                }
                Text(label)
                    .font(AppTheme.Typography.caption(sc))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
    }
}

private struct HomeFeaturedCourseSection: View {
    @Environment(\.appSizeClass) var sc
    let onSeeAll: () -> Void
    let onTap: () -> Void

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        VStack(alignment: .leading, spacing: pad.md) {
            HStack {
                Text("Featured course")
                    .font(.system(size: AppSizing.fontSize(base: 20, sizeClass: sc), weight: .bold))
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                Spacer()
                Button(action: onSeeAll) {
                    Text("See all")
                        .font(.system(size: AppSizing.fontSize(base: 15, sizeClass: sc), weight: .semibold))
                        .foregroundColor(AppTheme.Colors.primaryFallback)
                }
            }

            Button(action: onTap) {
                VStack(spacing: 0) {
                    ZStack(alignment: .topLeading) {
                        LinearGradient(
                            colors: [Color(red: 0.259, green: 0.498, blue: 0.957),
                                     Color(red: 0.169, green: 0.357, blue: 0.910)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                        .frame(height: AppSizing.iconSize(base: 130, sizeClass: sc))

                        Image(systemName: "graduationcap.fill")
                            .font(.system(size: AppSizing.fontSize(base: 70, sizeClass: sc)))
                            .foregroundColor(.white.opacity(0.25))
                            .frame(maxWidth: .infinity,
                                   maxHeight: AppSizing.iconSize(base: 130, sizeClass: sc),
                                   alignment: .bottomTrailing)
                            .padding(.trailing, pad.lg)

                        Text("BESTSELLER")
                            .font(.system(size: AppSizing.fontSize(base: 11, sizeClass: sc), weight: .bold))
                            .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                            .padding(.horizontal, 12).padding(.vertical, 5)
                            .background(AppTheme.Colors.surface)
                            .cornerRadius(AppSizing.Radius.pill)
                            .padding(pad.md)
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Complete Spoken English Mastery")
                            .font(.system(size: AppSizing.fontSize(base: 17, sizeClass: sc), weight: .bold))
                            .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                        HStack(spacing: pad.md) {
                            HomeCourseMetaItem(
                                icon: "star.fill",
                                iconColor: Color(red: 0.961, green: 0.651, blue: 0.137),
                                text: "4.9"
                            )
                            HomeCourseMetaItem(
                                icon: "clock",
                                iconColor: AppTheme.Colors.textSecondaryFallback,
                                text: "8h 20m"
                            )
                            HomeCourseMetaItem(
                                icon: "person.2",
                                iconColor: AppTheme.Colors.textSecondaryFallback,
                                text: "12.4k"
                            )
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(pad.md)
                    .background(AppTheme.Colors.surface)
                }
            }
            .buttonStyle(.plain)
            .background(AppTheme.Colors.surface)
            .cornerRadius(AppSizing.cardRadius(sc))
            .shadow(color: AppTheme.Shadow.card.color, radius: AppTheme.Shadow.card.radius, x: 0, y: AppTheme.Shadow.card.y)
        }
    }
}

private struct HomeCourseMetaItem: View {
    let icon: String
    let iconColor: Color
    let text: String
    @Environment(\.appSizeClass) var sc
    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
                .font(.system(size: AppSizing.fontSize(base: 12, sizeClass: sc)))
                .foregroundColor(iconColor)
            Text(text)
                .font(AppTheme.Typography.subheadline(sc))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
        }
    }
}

private struct HomeCompletedCoursesBadge: View {
    let count: Int
    var onTap: () -> Void = {}
    @Environment(\.appSizeClass) var sc

    private var message: String {
        count == 1
            ? NSLocalizedString("1 course completed", comment: "")
            : String(format: NSLocalizedString("%d courses completed", comment: ""), count)
    }

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        Button(action: onTap) {
            HStack(spacing: pad.sm) {
                ZStack {
                    Circle()
                        .fill(Color(red: 0.176, green: 0.741, blue: 0.494).opacity(0.15))
                        .frame(width: AppSizing.iconSize(base: 32, sizeClass: sc),
                               height: AppSizing.iconSize(base: 32, sizeClass: sc))
                    Image(systemName: "checkmark.seal.fill")
                        .font(.system(size: AppSizing.fontSize(base: 15, sizeClass: sc)))
                        .foregroundColor(Color(red: 0.176, green: 0.741, blue: 0.494))
                }
                Text(message)
                    .font(.system(size: AppSizing.fontSize(base: 14, sizeClass: sc), weight: .semibold))
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: AppSizing.fontSize(base: 12, sizeClass: sc), weight: .semibold))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
            }
            .padding(.horizontal, pad.md)
            .padding(.vertical, AppSizing.scaled(10, sc))
            .background(AppTheme.Colors.surface)
            .cornerRadius(AppSizing.cardRadius(sc))
            .shadow(color: AppTheme.Shadow.card.color, radius: AppTheme.Shadow.card.radius, x: 0, y: AppTheme.Shadow.card.y)
        }
        .buttonStyle(.plain)
    }
}

private struct HomeUpcomingLiveClass: View {
    let featuredClass: LiveClass?
    let now: Date
    var onTap: () -> Void = {}
    @Environment(\.appSizeClass) var sc

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        VStack(alignment: .leading, spacing: pad.md) {
            Text("Live class")
                .font(.system(size: AppSizing.fontSize(base: 20, sizeClass: sc), weight: .bold))
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)

            if let cls = featuredClass {
                scheduledCard(cls, pad: pad)
            } else {
                Button(action: onTap) {
                    emptyCard(pad: pad)
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func scheduledCard(_ cls: LiveClass, pad: AppSizing.Padding) -> some View {
        let isLive = now >= cls.date
        let remaining = max(0, Int(cls.date.timeIntervalSince(now)))
        let h = remaining / 3600, m = (remaining % 3600) / 60, s = remaining % 60

        return Button(action: onTap) {
            VStack(alignment: .leading, spacing: pad.sm) {
                HStack(spacing: 8) {
                    Circle().fill(isLive ? Color.green : Color.red).frame(width: 7, height: 7)
                    Text(isLive ? "LIVE NOW" : "STARTS IN")
                        .font(.system(size: AppSizing.fontSize(base: 10, sizeClass: sc), weight: .semibold))
                        .foregroundColor(isLive ? .green : AppTheme.Colors.liveClassRed)
                        .tracking(1.0)
                    Spacer()
                    if !isLive {
                        Text(String(format: "%02d:%02d:%02d", h, m, s))
                            .font(.system(size: AppSizing.fontSize(base: 13, sizeClass: sc), weight: .bold))
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                            .monospacedDigit()
                    }
                }

                HStack(spacing: pad.md) {
                    VStack(spacing: 0) {
                        Text(dayNumber(cls.date))
                            .font(.system(size: AppSizing.fontSize(base: 22, sizeClass: sc), weight: .bold))
                            .foregroundColor(AppTheme.Colors.liveClassRed)
                        Text(monthAbbrev(cls.date))
                            .font(.system(size: AppSizing.fontSize(base: 11, sizeClass: sc), weight: .semibold))
                            .foregroundColor(AppTheme.Colors.liveClassRed)
                    }
                    .frame(width: AppSizing.iconSize(base: 52, sizeClass: sc),
                           height: AppSizing.iconSize(base: 52, sizeClass: sc))
                    .background(AppTheme.Colors.liveClassRedBg)
                    .cornerRadius(12)

                    VStack(alignment: .leading, spacing: 4) {
                        Text(cls.title)
                            .font(.system(size: AppSizing.fontSize(base: 15, sizeClass: sc), weight: .bold))
                            .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                            .fixedSize(horizontal: false, vertical: true)
                        Text(String(format: NSLocalizedString("Today · %@ · with %@", comment: ""),
                                    cls.date.formatted(date: .omitted, time: .shortened), cls.teacherName))
                            .font(AppTheme.Typography.caption(sc))
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    }
                    Spacer(minLength: 0)
                    Text("Join")
                        .font(.system(size: AppSizing.fontSize(base: 14, sizeClass: sc), weight: .semibold))
                        .foregroundColor(isLive ? .white : AppTheme.Colors.primaryFallback)
                        .padding(.horizontal, pad.md).padding(.vertical, 8)
                        .background(isLive ? Color.green : AppTheme.Colors.primaryLightFallback)
                        .cornerRadius(AppSizing.Radius.pill)
                }
            }
            .padding(pad.md)
            .background(AppTheme.Colors.surface)
            .cornerRadius(AppSizing.cardRadius(sc))
            .shadow(color: AppTheme.Shadow.card.color, radius: AppTheme.Shadow.card.radius, x: 0, y: AppTheme.Shadow.card.y)
        }
        .buttonStyle(.plain)
    }

    private func emptyCard(pad: AppSizing.Padding) -> some View {
        HStack(spacing: pad.md) {
            ZStack {
                Circle()
                    .fill(AppTheme.Colors.primaryLightFallback)
                    .frame(width: AppSizing.iconSize(base: 52, sizeClass: sc),
                           height: AppSizing.iconSize(base: 52, sizeClass: sc))
                Image(systemName: "calendar")
                    .font(.system(size: AppSizing.fontSize(base: 20, sizeClass: sc)))
                    .foregroundColor(AppTheme.Colors.primaryFallback)
            }
            VStack(alignment: .leading, spacing: 4) {
                Text("No meeting for today")
                    .font(.system(size: AppSizing.fontSize(base: 15, sizeClass: sc), weight: .bold))
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                Text("Tap to see live classes")
                    .font(AppTheme.Typography.caption(sc))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
            }
            Spacer(minLength: 0)
            Image(systemName: "chevron.right")
                .font(.system(size: AppSizing.fontSize(base: 14, sizeClass: sc), weight: .semibold))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
        }
        .padding(pad.md)
        .background(AppTheme.Colors.surface)
        .cornerRadius(AppSizing.cardRadius(sc))
        .shadow(color: AppTheme.Shadow.card.color, radius: AppTheme.Shadow.card.radius, x: 0, y: AppTheme.Shadow.card.y)
    }

    private func dayNumber(_ date: Date) -> String {
        let f = DateFormatter(); f.dateFormat = "d"; return f.string(from: date)
    }
    private func monthAbbrev(_ date: Date) -> String {
        let f = DateFormatter(); f.dateFormat = "MMM"; return f.string(from: date).uppercased()
    }
}

private struct HomeMotivationCard: View {
    let quote: String
    @Environment(\.appSizeClass) var sc
    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        HStack(alignment: .top, spacing: pad.sm) {
            Image(systemName: "sparkles")
                .font(.system(size: AppSizing.fontSize(base: 18, sizeClass: sc)))
                .foregroundColor(Color(red: 0.122, green: 0.686, blue: 0.478))
                .padding(.top, 2)
            Text(quote)
                .font(.system(size: AppSizing.fontSize(base: 14, sizeClass: sc), weight: .medium))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
        }
        .padding(pad.md)
        .background(Color(red: 0.082, green: 0.082, blue: 0.122))
        .cornerRadius(AppSizing.cardRadius(sc))
    }
}
