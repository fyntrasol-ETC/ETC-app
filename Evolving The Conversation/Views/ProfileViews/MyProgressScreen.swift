import SwiftUI
import FirebaseAuth
import FirebaseFirestore

// ─────────────────────────────────────────────────────────────
// MARK: - My Progress Screen
// ─────────────────────────────────────────────────────────────
struct MyProgressScreen: View {
    @Environment(\.appSizeClass) var sc
    @Environment(\.dismiss) var dismiss

    @ObservedObject private var xpService = XPService.shared
    @ObservedObject private var progressService = LessonProgressService.shared

    // ── FIX: rawValue kept in English for internal logic/identity;
    // added displayName for the localized label shown in the UI ──
    private enum ProgressPeriod: String, CaseIterable {
        case week = "Week", month = "Month", year = "Year"

        var displayName: String {
            switch self {
            case .week:  return NSLocalizedString("Week", comment: "")
            case .month: return NSLocalizedString("Month", comment: "")
            case .year:  return NSLocalizedString("Year", comment: "")
            }
        }
    }

    @State private var period: ProgressPeriod = .week
    @State private var points: [XPService.ActivityPoint] = []
    @State private var chartLoading = true

    @State private var skillStats = SkillStatsService.SkillBreakdownStats()

    // Synced from users/{uid}.stats — matches Android exactly.
    @State private var totalHoursValue: Double = 0
    @State private var totalLessonsCompleted: Int = 0

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        VStack(spacing: 0) {
            if Auth.auth().currentUser == nil {
                headerBand
                // "Sign in to see your progress." already exists in Localizable.strings
                Text(NSLocalizedString("Sign in to see your progress.", comment: ""))
                    .font(AppTheme.Typography.body(sc))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    .padding(pad.lg)
                Spacer()
            } else {
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 0) {
                        headerBand

                        // ── Stats card — overlaps the header, like HomeScreen's cards ──
                        statsCard(pad: pad)
                            .padding(.horizontal, pad.lg)
                            .offset(y: -40)
                            .padding(.bottom, -40)

                        VStack(spacing: pad.lg) {
                            activitySection(pad: pad)
                            skillBreakdownSection(pad: pad)
                        }
                        .padding(.horizontal, pad.lg)
                        .padding(.bottom, 40)
                        .frame(maxWidth: AppSizing.maxContentWidth(sc))
                        .frame(maxWidth: .infinity)
                    }
                }
            }
        }
        .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
        .navigationBarBackButtonHidden(true)
        .onAppear {
            progressService.listenAll()
            xpService.start()
        }
        .task { await loadSkillStats() }
        .task { await loadUserStats() }
        .task(id: period) { await loadActivity() }
    }

    // MARK: - Header band (blue gradient, rounded bottom corners)
    private var headerBand: some View {
        ZStack {
            LinearGradient(
                colors: [AppTheme.Colors.primaryFallback, AppTheme.Colors.primaryDarkFallback],
                startPoint: .topLeading, endPoint: .bottomTrailing
            )
            .ignoresSafeArea(edges: .top)
            .clipShape(
                UnevenRoundedRectangle(
                    topLeadingRadius: 0,
                    bottomLeadingRadius: 28,
                    bottomTrailingRadius: 28,
                    topTrailingRadius: 0
                )
            )

            HStack(spacing: 10) {
                ETCBackButton(action: { dismiss() })
                // ── FIX: raw "My Progress" literal — now localized (already
                // exists in strings as the drawer entry, reused here) ──
                Text(NSLocalizedString("My Progress", comment: ""))
                    .font(.system(size: AppSizing.fontSize(base: 26, sizeClass: sc), weight: .heavy))
                    .foregroundColor(.white)
                Spacer()
            }
            .padding(.horizontal, AppSizing.Padding.forClass(sc).lg)
            .padding(.top, 56)
            .padding(.bottom, 64)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Stats card (Hours / Lessons / XP + streak pill)
    private func statsCard(pad: AppSizing.Padding) -> some View {
        VStack(spacing: pad.md) {
            HStack(spacing: 0) {
                // ── FIX: "Hours" / "Lessons" / "XP" now localized ──
                ProgressStatCell(value: formattedHours, label: NSLocalizedString("Hours", comment: ""))
                Divider().frame(height: 52)
                ProgressStatCell(value: "\(totalLessonsCompleted)", label: NSLocalizedString("Lessons", comment: ""))
                Divider().frame(height: 52)
                ProgressStatCell(value: "\(xpService.xp)", label: NSLocalizedString("XP", comment: ""))
            }

            HStack {
                Spacer()
                // ── FIX: format string uses a localized template so the
                // "day streak" wording translates too ──
                Text("🔥 " + String(format: NSLocalizedString("%d day streak", comment: ""), xpService.streak))
                    .font(.system(size: AppSizing.fontSize(base: 16, sizeClass: sc), weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 10)
                    .background(
                        LinearGradient(
                            colors: [Color(red: 1.0, green: 0.70, blue: 0.22), AppTheme.Colors.orangeFallback],
                            startPoint: .leading, endPoint: .trailing
                        )
                    )
                    .clipShape(Capsule())
                Spacer()
            }
        }
        .padding(.vertical, pad.lg)
        .background(AppTheme.Colors.surface)
        .cornerRadius(AppSizing.cardRadius(sc))
        .shadow(
            color: AppTheme.Shadow.card.color,
            radius: AppTheme.Shadow.card.radius,
            x: AppTheme.Shadow.card.x,
            y: AppTheme.Shadow.card.y
        )
    }

    // MARK: - Activity section (period toggle + bar chart)
    private func activitySection(pad: AppSizing.Padding) -> some View {
        VStack(alignment: .leading, spacing: pad.md) {
            HStack {
                // ── FIX: "Activity" already exists in strings — now localized ──
                Text(NSLocalizedString("Activity", comment: ""))
                    .font(AppTheme.Typography.title2(sc))
                    .fontWeight(.heavy)
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                Spacer()

                HStack(spacing: 4) {
                    ForEach(ProgressPeriod.allCases, id: \.self) { p in
                        let selected = period == p
                        // ── FIX: use p.displayName (localized) instead of p.rawValue ──
                        Text(p.displayName)
                            .font(.system(size: AppSizing.fontSize(base: 14, sizeClass: sc), weight: .bold))
                            .foregroundColor(selected ? .white : AppTheme.Colors.textSecondaryFallback)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 9)
                            .background(
                                Group {
                                    if selected {
                                        LinearGradient(
                                            colors: [AppTheme.Colors.primaryFallback, AppTheme.Colors.primaryDarkFallback],
                                            startPoint: .top, endPoint: .bottom
                                        )
                                    } else {
                                        Color.clear
                                    }
                                }
                            )
                            .clipShape(Capsule())
                            .onTapGesture { period = p }
                    }
                }
                .padding(4)
                .background(AppTheme.Colors.segmentedBg)
                .clipShape(Capsule())
            }

            VStack {
                if chartLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 40)
                } else if points.allSatisfy({ $0.xp == 0 }) {
                    // ── FIX: was interpolating period.rawValue.lowercased() directly
                    // into an un-localized string. Now uses period.displayName inside
                    // a localized format string, so both the sentence and the period
                    // name translate together. Add
                    // "No activity yet this %@ — complete a lesson, quiz, or practice session to see it here."
                    // to Localizable.strings for Spanish. ──
                    Text(String(format: NSLocalizedString(
                        "No activity yet this %@ — complete a lesson, quiz, or practice session to see it here.",
                        comment: ""
                    ), period.displayName.lowercased()))
                        .font(AppTheme.Typography.subheadline(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 30)
                } else {
                    ActivityBarChart(points: points)
                        .frame(height: 130)
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

    // MARK: - Skill breakdown section
    private func skillBreakdownSection(pad: AppSizing.Padding) -> some View {
        VStack(alignment: .leading, spacing: pad.md) {
            // ── FIX: "Skill breakdown" already exists in strings — now localized ──
            Text(NSLocalizedString("Skill breakdown", comment: ""))
                .font(AppTheme.Typography.title2(sc))
                .fontWeight(.heavy)
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)

            VStack(spacing: pad.lg) {
                // ── FIX: all titles/subtitles/labels below now localized ──
                SkillBreakdownHeader(
                    systemIcon: "mic.fill",
                    tint: AppTheme.Colors.primaryFallback,
                    title: NSLocalizedString("Speaking", comment: ""),
                    subtitle: NSLocalizedString("AI assessed your speaking and gives feedback", comment: "")
                )
                HStack {
                    Spacer()
                    ProgressStatCell(value: "\(skillStats.confidenceScore)",
                                      label: NSLocalizedString("Confidence Score", comment: ""))
                    Spacer()
                }

                Divider()

                SkillBreakdownHeader(
                    systemIcon: "headphones",
                    tint: AppTheme.Colors.greenFallback,
                    title: NSLocalizedString("Listening", comment: ""),
                    subtitle: NSLocalizedString("Based on your dictation practice", comment: "")
                )
                HStack {
                    ProgressStatCell(value: "\(skillStats.correctAnswers)", label: NSLocalizedString("Correct", comment: ""))
                    ProgressStatCell(value: "\(skillStats.wrongAnswers)", label: NSLocalizedString("Wrong", comment: ""))
                    ProgressStatCell(value: "\(skillStats.totalDictations)", label: NSLocalizedString("Total", comment: ""))
                }

                Divider()

                SkillBreakdownHeader(
                    systemIcon: "doc.text.fill",
                    tint: AppTheme.Colors.orangeFallback,
                    title: NSLocalizedString("Grammar", comment: ""),
                    subtitle: NSLocalizedString("Quizzes completed across all topics", comment: "")
                )
                HStack {
                    Spacer()
                    ProgressStatCell(value: "\(skillStats.grammarQuizzesSolved)",
                                      label: NSLocalizedString("Quizzes Solved", comment: ""))
                    Spacer()
                }

                if skillStats.isEmpty {
                    Text(NSLocalizedString("Stats fill in as you practice speaking, listening and grammar.", comment: ""))
                        .font(AppTheme.Typography.caption(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                }
            }
            .padding(pad.lg)
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

    // MARK: - Data loading

    private func loadActivity() async {
        chartLoading = true
        switch period {
        case .week:  points = await xpService.fetchWeekActivity()
        case .month: points = await xpService.fetchMonthActivity()
        case .year:  points = await xpService.fetchYearActivity()
        }
        chartLoading = false
    }

    private func loadSkillStats() async {
        skillStats = await SkillStatsService.shared.fetchTodayStats()
    }

    private func loadUserStats() async {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        guard let doc = try? await Firestore.firestore().collection("users").document(uid).getDocument() else { return }
        let stats = doc.get("stats") as? [String: Any]
        totalHoursValue = (stats?["totalHours"] as? Double) ?? 0
        totalLessonsCompleted = (stats?["totalLessonsCompleted"] as? Int) ?? 0
    }

    private var formattedHours: String {
        totalHoursValue >= 10
            ? String(Int(totalHoursValue))
            : String(format: "%.1f", totalHoursValue)
    }

    func parseDurationMinutes(_ text: String) -> Double {
        let lower = text.lowercased()
        if lower.contains("h") {
            let parts = lower.split(separator: "h")
            let hours = Double(parts.first?.trimmingCharacters(in: .whitespaces) ?? "") ?? 0
            var minutes = 0.0
            if parts.count > 1 {
                let minPart = parts[1].filter { $0.isNumber }
                minutes = Double(minPart) ?? 0
            }
            return hours * 60 + minutes
        }
        let leading = lower.prefix { $0.isNumber }
        return Double(leading) ?? 0
    }
}

// MARK: - Header back button (translucent white circle, matches Android's back chevron)
private struct ProgressHeaderBackButton: View {
    let action: () -> Void
    @Environment(\.appSizeClass) var sc

    var body: some View {
        Button(action: action) {
            ZStack {
                Circle()
                    .fill(Color.white.opacity(0.20))
                    .frame(width: AppSizing.iconSize(base: 34, sizeClass: sc),
                           height: AppSizing.iconSize(base: 34, sizeClass: sc))
                Image(systemName: "chevron.left")
                    .font(.system(size: AppSizing.fontSize(base: 15, sizeClass: sc), weight: .semibold))
                    .foregroundColor(.white)
            }
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Skill breakdown header row (icon circle + title + subtitle)
private struct SkillBreakdownHeader: View {
    let systemIcon: String
    let tint: Color
    let title: String
    let subtitle: String
    @Environment(\.appSizeClass) var sc

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(tint.opacity(0.12))
                    .frame(width: AppSizing.iconSize(base: 40, sizeClass: sc),
                           height: AppSizing.iconSize(base: 40, sizeClass: sc))
                Image(systemName: systemIcon)
                    .font(.system(size: AppSizing.fontSize(base: 17, sizeClass: sc)))
                    .foregroundColor(tint)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(AppTheme.Typography.headline(sc))
                    .fontWeight(.bold)
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                Text(subtitle)
                    .font(AppTheme.Typography.caption(sc))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
            }
            Spacer(minLength: 0)
        }
    }
}

// MARK: - Stat cell (value + label, used in stats card and skill breakdown)
private struct ProgressStatCell: View {
    let value: String
    let label: String
    @Environment(\.appSizeClass) var sc

    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.system(size: AppSizing.fontSize(base: 28, sizeClass: sc), weight: .heavy))
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)
            Text(label)
                .font(AppTheme.Typography.subheadline(sc))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Activity bar chart
private struct ActivityBarChart: View {
    let points: [XPService.ActivityPoint]

    var body: some View {
        let maxXp = max(points.map(\.xp).max() ?? 1, 1)
        HStack(alignment: .bottom, spacing: 0) {
            ForEach(points) { point in
                VStack(spacing: 8) {
                    Spacer(minLength: 0)
                    if point.xp > 0 {
                        RoundedRectangle(cornerRadius: 6)
                            .fill(
                                LinearGradient(
                                    colors: [Color(red: 0.31, green: 0.56, blue: 0.97), AppTheme.Colors.primaryFallback],
                                    startPoint: .top, endPoint: .bottom
                                )
                            )
                            .frame(width: 20, height: max(CGFloat(point.xp) / CGFloat(maxXp) * 84, 6))
                    }
                    Text(point.label)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                }
                .frame(maxWidth: .infinity)
            }
        }
    }
}
