import SwiftUI

struct ProfileScreen: View {
    @EnvironmentObject private var authVM: AuthViewModel
    @Environment(\.appSizeClass) var sc
    @Binding var selectedTab: ETCTab
    @Binding var showTabBar: Bool

    @State private var showDrawer        = false
    @State private var showNotifications = false
    @State private var showSearch        = false
    @State private var destination: ProfileDestination? = nil

    enum ProfileDestination {
        case progress, certificates, premium, settings, helpCenter
        case videoLessons, grammar, vocabulary, quizzes
    }

    var body: some View {
        NavigationStack {
            ZStack(alignment: .leading) {
                mainContent
                    .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea(edges: .bottom))

                if showDrawer {
                    ETCDrawerMenu(isPresented: $showDrawer) { dest in
                        handleDrawerNavigation(dest)
                    }
                    .environmentObject(authVM)
                    .zIndex(10)
                    .transition(.move(edge: .leading))
                }
            }
            .background(Color(red: 0.07, green: 0.09, blue: 0.18).ignoresSafeArea())
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(item: $destination) { dest in
                switch dest {
                case .progress:      MyProgressScreen()
                case .certificates:  CertificatesScreen()
                case .premium:       PremiumPlansScreen()
                    // In ProfileScreen navigationDestination, replace .settings case:
                    case .settings:
                        SettingsScreen(showTabBar: $showTabBar)
                            .environmentObject(authVM)
                case .helpCenter:    HelpCenterScreen()
                case .videoLessons:
                    if let course = CourseCatalog.all.first(where: { $0.title == "Spoken English Mastery" }),
                       let lesson = course.lessons.first(where: { $0.order == 13 }) {
                        VideoLessonScreen(
                            course: course,
                            lesson: lesson,
                            onBack: { destination = nil }
                        )
                    } else {
                        EmptyView()
                            .onAppear { destination = nil }
                    }
                case .grammar:
                    GrammarScreen(onBack: { destination = nil })
                case .vocabulary:
                    VocabularyLevelsScreen(onBack: { destination = nil }).appSizeClass()
                case .quizzes:
                    QuizScreen(title: "Daily Quiz", category: "Mixed",
                               onBack: { destination = nil })
                }
            }
        }
        // ── Hide tab bar when a sub-screen is pushed or drawer opens ──
        .onChange(of: destination) { dest in
            withAnimation { showTabBar = dest == nil }
        }
        .onChange(of: showDrawer) { open in
            withAnimation { showTabBar = !open }
        }
        // Load this user's enrollments + certificates for the stats card
        .onAppear {
            CertificateService.shared.start()
            LessonProgressService.shared.listenAll()
        }
        // If a different account signs in during this app session, make sure
        // we drop the previous user's cached counts and refetch for the new one.
        .onChange(of: authVM.currentUser?.uid) { _ in
            CertificateService.shared.start()
            LessonProgressService.shared.listenAll()
        }
        .fullScreenCover(isPresented: $showSearch) {
            ETCSearchScreen(isPresented: $showSearch)
                .environmentObject(authVM).appSizeClass()
        }
        .fullScreenCover(isPresented: $showNotifications) {
            ETCNotificationsScreen(isPresented: $showNotifications).appSizeClass()
        }
    }

    private func handleDrawerNavigation(_ dest: ETCDrawerMenu.DrawerDestination) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.32) {
            switch dest {
            case .home:         selectedTab = .home
            case .courses:      selectedTab = .courses
            case .community:    selectedTab = .community
            case .practice:     break
            case .liveClasses:  break
            case .videoLessons: destination = .videoLessons
            case .grammar:      destination = .grammar
            case .vocabulary:   destination = .vocabulary
            case .quizzes:      destination = .quizzes
            case .certificates: destination = .certificates
            case .progress:     destination = .progress
            case .settings:     destination = .settings
            case .helpCenter:   destination = .helpCenter
            }
        }
    }

    private var mainContent: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 0) {
                ProfileHeaderBand(
                    onDrawer:          { withAnimation(.easeInOut(duration: 0.28)) { showDrawer = true } },
                    onNotification:    { showNotifications = true },
                    notificationBadge: 3
                )
                .environmentObject(authVM)
//
//                StatsCard()
//                    .padding(.horizontal, AppSizing.Padding.forClass(sc).lg)
//                    .offset(y: -32)

                VStack(spacing: AppSizing.Padding.forClass(sc).sm) {
                    // ── FIX: every title below was a raw literal — now localized ──
                    ProfileMenuRow(icon: "chart.bar.fill",
                                   iconColor: AppTheme.Colors.primaryFallback,
                                   title: NSLocalizedString("My Progress", comment: ""))      { destination = .progress }
                    ProfileMenuRow(icon: "rosette",
                                   iconColor: AppTheme.Colors.primaryFallback,
                                   title: NSLocalizedString("Certificates", comment: ""))     { destination = .certificates }
                    ProfileMenuRow(icon: "trophy.fill",
                                   iconColor: AppTheme.Colors.primaryFallback,
                                   title: NSLocalizedString("Premium Plans", comment: ""))    { destination = .premium }
                    ProfileMenuRow(icon: "gearshape.fill",
                                   iconColor: AppTheme.Colors.primaryFallback,
                                   title: NSLocalizedString("Settings", comment: ""))         { destination = .settings }
                    ProfileMenuRow(icon: "questionmark.circle.fill",
                                   iconColor: AppTheme.Colors.primaryFallback,
                                   title: NSLocalizedString("Help Center", comment: ""))      { destination = .helpCenter }
                }
                .padding(.horizontal, AppSizing.Padding.forClass(sc).lg)
                .padding(.top, -16)
                .padding(.bottom, 100)
            }
        }
    }
}

// MARK: - ProfileDestination: Hashable
extension ProfileScreen.ProfileDestination: Hashable {}

// MARK: - Profile Header Band
private struct ProfileHeaderBand: View {
    @EnvironmentObject private var authVM: AuthViewModel
    @Environment(\.appSizeClass) var sc

    var onDrawer: () -> Void
    var onNotification: () -> Void
    var notificationBadge: Int = 0

    var body: some View {
        let avatarSize = AppSizing.iconSize(base: 90, sizeClass: sc)
        let pad = AppSizing.Padding.forClass(sc)

        ZStack {
            Color(red: 0.07, green: 0.09, blue: 0.18)
                .frame(maxWidth: .infinity)
                .clipShape(UnevenRoundedRectangle(
                    topLeadingRadius: 0,
                    bottomLeadingRadius: 32,
                    bottomTrailingRadius: 32,
                    topTrailingRadius: 0))
                .ignoresSafeArea(edges: .top)

            VStack(spacing: 0) {
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
                    // ── FIX: was Text("Profile") passed as a variable-like literal in a
                    // component that renders verbatim — wrapped in NSLocalizedString ──
                    Text(NSLocalizedString("Profile", comment: ""))
                        .font(AppTheme.Typography.title2(sc))
                        .foregroundColor(.white).fontWeight(.bold)
                    Spacer()
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
                .padding(.horizontal, pad.md)
                .padding(.top, pad.md)
                .padding(.bottom, 5)

                ZStack {
                    Circle()
                        .fill(LinearGradient(
                            colors: [AppTheme.Colors.primaryFallback, AppTheme.Colors.greenFallback],
                            startPoint: .topLeading, endPoint: .bottomTrailing))
                        .frame(width: avatarSize, height: avatarSize)
                        .overlay(Circle().stroke(Color.white, lineWidth: 3))
                    Text(initials)
                        .font(.system(size: AppSizing.fontSize(base: 32, sizeClass: sc), weight: .bold))
                        .foregroundColor(.white)
                }

                Text(authVM.currentUser?.displayName ?? "User")
                    .font(AppTheme.Typography.title1(sc))
                    .foregroundColor(.white).fontWeight(.bold)
                    .padding(.top, pad.sm)

                HStack(spacing: 6) {
                    Image(systemName: "star.fill")
                        .font(.system(size: AppSizing.fontSize(base: 11, sizeClass: sc)))
                        .foregroundColor(AppTheme.Colors.greenFallback)
                    // ── FIX: was a raw "Premium member" literal — now localized.
                    // NOTE: add "Premium member" = "Miembro Premium"; to Localizable.strings
                    // (only "Premium" alone currently exists there) ──
                    Text(NSLocalizedString("Premium member", comment: ""))
                        .font(AppTheme.Typography.subheadline(sc))
                        .foregroundColor(AppTheme.Colors.greenFallback).fontWeight(.semibold)
                }
                .padding(.horizontal, 14).padding(.vertical, 6)
                .background(AppTheme.Colors.greenFallback.opacity(0.18))
                .cornerRadius(AppTheme.Radius.pill)
                .padding(.top, pad.xs)
                .padding(.bottom, 30)
                
            }
        }
    }

    private var initials: String {
        let name = authVM.currentUser?.displayName ?? authVM.currentUser?.email ?? "?"
        let parts = name.split(separator: " ")
        if parts.count >= 2 {
            return String((parts[0].first ?? "?")).uppercased()
                 + String((parts[1].first ?? "?")).uppercased()
        }
        return String((name.first ?? "?")).uppercased()
    }
}

private struct ProfileMenuRow: View {
    let icon: String; let iconColor: Color; let title: String; let action: () -> Void
    @Environment(\.appSizeClass) var sc
    var body: some View {
        Button(action: action) {
            HStack(spacing: AppSizing.Padding.forClass(sc).md) {
                Image(systemName: icon)
                    .font(.system(size: AppSizing.fontSize(base: 20, sizeClass: sc), weight: .medium))
                    .foregroundColor(iconColor)
                    .frame(width: AppSizing.iconSize(base: 36, sizeClass: sc),
                           height: AppSizing.iconSize(base: 36, sizeClass: sc))
                Text(title)
                    .font(AppTheme.Typography.headline(sc))
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: AppSizing.fontSize(base: 14, sizeClass: sc), weight: .semibold))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
            }
            .padding(.horizontal, AppSizing.Padding.forClass(sc).md)
            .padding(.vertical, AppSizing.Padding.forClass(sc).md)
            .background(AppTheme.Colors.surface)
            .cornerRadius(AppSizing.cardRadius(sc))
            .shadow(color: AppTheme.Shadow.card.color, radius: AppTheme.Shadow.card.radius,
                    x: AppTheme.Shadow.card.x, y: AppTheme.Shadow.card.y)
        }
    }
}
