import SwiftUI

struct LiveClassesScreen: View {
    var onBack: (() -> Void)? = nil

    @EnvironmentObject private var teacherVM: TeacherAuthViewModel
    @ObservedObject private var classService = LiveClassService.shared
    @EnvironmentObject private var authVM: AuthViewModel
    @Environment(\.appSizeClass) var sc
    @Environment(\.openURL) private var openURL

    @State private var timer: Timer? = nil
    @State private var now = Date()
    @State private var selectedDate: Date = Calendar.current.startOfDay(for: Date())
    @State private var showTeacherGate = false
    @State private var showTeacherDashboard = false
    @State private var showMonthCalendar = false

    private var calendar: Calendar { Calendar.current }

    var body: some View {
        VStack(spacing: 0) {
            headerBar

            ScrollView(showsIndicators: false) {
                VStack(spacing: AppSizing.Padding.forClass(sc).md) {
                    countdownHero
                    calendarLauncherSection
                    selectedDaySessionsSection
                }
                .padding(.horizontal, AppSizing.Padding.forClass(sc).lg)
                .padding(.top, AppSizing.Padding.forClass(sc).md)
                .padding(.bottom, 100)
                .frame(maxWidth: AppSizing.maxContentWidth(sc))
                .frame(maxWidth: .infinity)
            }
        }
        .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
        .onAppear {
            classService.listen()
            startTimer()
        }
        .onDisappear { timer?.invalidate() }
        .sheet(isPresented: $showMonthCalendar) {
            MonthlyCalendarSheet(
                selectedDate: $selectedDate,
                classService: classService,
                onDone: { showMonthCalendar = false }
            )
            .appSizeClass()
        }
        .fullScreenCover(isPresented: $showTeacherGate) {
            TeacherSignInScreen(
                onBack: { showTeacherGate = false },
                onSignedIn: { showTeacherGate = false; showTeacherDashboard = true }
            )
            .environmentObject(teacherVM)
            .environmentObject(authVM)
            .appSizeClass()
        }
        .fullScreenCover(isPresented: $showTeacherDashboard) {
            TeacherDashboardScreen(onBack: { showTeacherDashboard = false })
                .environmentObject(teacherVM)
                .appSizeClass()
        }
    }

    private var headerBar: some View {
        let pad = AppSizing.Padding.forClass(sc)
        return HStack(spacing: 0) {
            ETCBackButton(title: "Live Classes") { onBack?() }
            Spacer()
            Button {
                if teacherVM.isTeacherSignedIn { showTeacherDashboard = true }
                else { showTeacherGate = true }
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "graduationcap.fill")
                    Text(teacherVM.isTeacherSignedIn ? "My classes" : "Teacher")
                }
                .font(.system(size: AppSizing.fontSize(base: 13, sizeClass: sc), weight: .semibold))
                .foregroundColor(AppTheme.Colors.primaryFallback)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(AppTheme.Colors.primaryLightFallback)   // already dynamic — untouched
                .clipShape(Capsule())
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, pad.lg)
        .padding(.top, pad.sm)
        .padding(.bottom, pad.md)
        .background(AppTheme.Colors.backgroundFallback)
    }

    // ── Hero box: today's featured class only ────────────────────────
    @ViewBuilder
    private var countdownHero: some View {
        let pad = AppSizing.Padding.forClass(sc)

        if let featured = classService.featuredClass(now: now) {
            let isLive = now >= featured.date

            VStack(spacing: pad.md) {
                HStack(spacing: 8) {
                    Circle().fill(isLive ? Color.green : Color.red).frame(width: 8, height: 8)
                    Text(isLive ? "LIVE NOW" : "STARTS IN")
                        .font(.system(size: AppSizing.fontSize(base: 11, sizeClass: sc), weight: .semibold))
                        .foregroundColor(.white.opacity(0.90))
                        .tracking(1.5)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                if !isLive {
                    let remaining = max(0, Int(featured.date.timeIntervalSince(now)))
                    let h = remaining / 3600, m = (remaining % 3600) / 60, s = remaining % 60

                    HStack(spacing: pad.sm) {
                        CountdownTile(value: h, label: "hrs", sc: sc)
                        CountdownTile(value: m, label: "min", sc: sc)
                        CountdownTile(value: s, label: "sec", sc: sc)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }

                VStack(alignment: .center, spacing: 6) {
                    Text(featured.title)
                        .font(.system(size: AppSizing.fontSize(base: 20, sizeClass: sc), weight: .bold))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                    Text("with \(featured.teacherName)")
                        .font(AppTheme.Typography.subheadline(sc))
                        .foregroundColor(.white.opacity(0.80))
                }
                .frame(maxWidth: .infinity)

                // "Join now" pill sits on the blue gradient hero — stays
                // white in both modes for contrast, same as CourseDetailScreen's
                // play button.
                Button { joinClass(featured) } label: {
                    HStack(spacing: 10) {
                        Image(systemName: isLive ? "video.fill" : "video")
                            .foregroundColor(isLive ? .green : AppTheme.Colors.primaryFallback)
                        Text("Join now")
                            .font(.system(size: AppSizing.fontSize(base: 17, sizeClass: sc), weight: .semibold))
                            .foregroundColor(AppTheme.Colors.primaryFallback)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: AppSizing.Button.forClass(sc).height)
                    .background(Color.white)
                    .cornerRadius(AppSizing.Radius.pill)
                }
                .buttonStyle(.plain)
            }
            .padding(pad.md)
            .background(LinearGradient(colors: [AppTheme.Colors.primaryFallback, AppTheme.Colors.primaryDarkFallback],
                                        startPoint: .topLeading, endPoint: .bottomTrailing))
            .cornerRadius(AppSizing.cardRadius(sc))
            .shadow(color: AppTheme.Colors.primaryFallback.opacity(0.35), radius: 14, x: 0, y: 8)
        } else {
            VStack(spacing: pad.sm) {
                Image(systemName: "calendar.badge.clock")
                    .font(.system(size: 28))
                    .foregroundColor(.white.opacity(0.9))
                Text("No meeting for today")
                    .font(.system(size: AppSizing.fontSize(base: 17, sizeClass: sc), weight: .bold))
                    .foregroundColor(.white)
                Text("Check back tomorrow, or ask your teacher to schedule one.")
                    .font(AppTheme.Typography.subheadline(sc))
                    .foregroundColor(.white.opacity(0.85))
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
            .padding(pad.lg)
            .background(LinearGradient(colors: [AppTheme.Colors.primaryFallback, AppTheme.Colors.primaryDarkFallback],
                                        startPoint: .topLeading, endPoint: .bottomTrailing))
            .cornerRadius(AppSizing.cardRadius(sc))
        }
    }

    // ── Calendar launcher (opens MonthlyCalendarSheet) ───────────────
    private var calendarLauncherSection: some View {
        let pad = AppSizing.Padding.forClass(sc)
        return Button {
            showMonthCalendar = true
        } label: {
            HStack(spacing: pad.md) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(AppTheme.Colors.primaryLightFallback)
                        .frame(width: AppSizing.iconSize(base: 48, sizeClass: sc),
                               height: AppSizing.iconSize(base: 48, sizeClass: sc))
                    Image(systemName: "calendar")
                        .font(.system(size: AppSizing.fontSize(base: 20, sizeClass: sc)))
                        .foregroundColor(AppTheme.Colors.primaryFallback)
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text("Live class calendar")
                        .font(AppTheme.Typography.headline(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    Text(selectionLabel)
                        .font(AppTheme.Typography.subheadline(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: AppSizing.fontSize(base: 14, sizeClass: sc), weight: .semibold))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
            }
            .padding(pad.md)
            .background(AppTheme.Colors.surface)     // was: Color.white
            .cornerRadius(AppSizing.cardRadius(sc))
            .shadow(color: AppTheme.Shadow.card.color, radius: AppTheme.Shadow.card.radius,
                    x: AppTheme.Shadow.card.x, y: AppTheme.Shadow.card.y)
        }
        .buttonStyle(.plain)
    }

        private var selectionLabel: String {
            if calendar.isDateInToday(selectedDate) { return NSLocalizedString("Viewing today", comment: "") }
            let f = DateFormatter()
            f.dateFormat = "EEEE, MMM d"
            return String(format: NSLocalizedString("Viewing %@", comment: ""), f.string(from: selectedDate))
        }

    @ViewBuilder
    private var selectedDaySessionsSection: some View {
        let pad = AppSizing.Padding.forClass(sc)
        let sessions = classService.classes(on: selectedDate)

        VStack(alignment: .leading, spacing: pad.sm) {
            Text(calendar.isDateInToday(selectedDate) ? "Today's sessions" : "Upcoming sessions")
                .font(.system(size: AppSizing.fontSize(base: 17, sizeClass: sc), weight: .bold))
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)

            if sessions.isEmpty {
                Text("No classes added.")
                    .font(AppTheme.Typography.subheadline(sc))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    .padding(.vertical, 6)
            } else {
                ForEach(sessions) { session in
                    HStack(spacing: pad.md) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 12)
                                .fill(AppTheme.Colors.primaryLightFallback)
                                .frame(width: AppSizing.iconSize(base: 48, sizeClass: sc),
                                       height: AppSizing.iconSize(base: 48, sizeClass: sc))
                            Image(systemName: "calendar")
                                .font(.system(size: AppSizing.fontSize(base: 20, sizeClass: sc)))
                                .foregroundColor(AppTheme.Colors.primaryFallback)
                        }
                        VStack(alignment: .leading, spacing: 4) {
                            Text(session.title)
                                .font(AppTheme.Typography.headline(sc))
                                .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                            Text(String(format: NSLocalizedString("%@ · %@", comment: ""),
                                        session.date.formatted(date: .omitted, time: .shortened), session.teacherName))                                .font(AppTheme.Typography.subheadline(sc))
                                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        }
                        Spacer()
                        Button { joinClass(session) } label: {
                            Text("Join")
                                .font(.system(size: AppSizing.fontSize(base: 13, sizeClass: sc), weight: .semibold))
                                .foregroundColor(AppTheme.Colors.primaryFallback)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 7)
                                .background(AppTheme.Colors.primaryLightFallback)
                                .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(pad.md)
                    .background(AppTheme.Colors.surface)     // was: Color.white
                    .cornerRadius(AppSizing.cardRadius(sc))
                    .shadow(color: AppTheme.Shadow.card.color, radius: AppTheme.Shadow.card.radius,
                            x: AppTheme.Shadow.card.x, y: AppTheme.Shadow.card.y)
                }
            }
        }
    }

    private func joinClass(_ liveClass: LiveClass) {
        guard let url = URL(string: liveClass.meetingLink) else { return }
        openURL(url)
    }

    private func startTimer() {
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in now = Date() }
    }
}

// Overlay tile on the colored gradient hero — stays translucent white
// in both modes, same reasoning as Home's stat cards.
private struct CountdownTile: View {
    let value: Int
    let label: String
    let sc: AppSizing.SizeClass
    var body: some View {
        VStack(spacing: 4) {
            Text(String(format: "%02d", value))
                .font(.system(size: AppSizing.fontSize(base: 32, sizeClass: sc), weight: .bold))
                .foregroundColor(.white)
                .monospacedDigit()
            Text(label)
                .font(.system(size: AppSizing.fontSize(base: 11, sizeClass: sc), weight: .medium))
                .foregroundColor(.white.opacity(0.75))
        }
        .frame(width: AppSizing.iconSize(base: 72, sizeClass: sc))
        .padding(.vertical, 12)
        .background(Color.white.opacity(0.18))
        .cornerRadius(14)
    }
}
