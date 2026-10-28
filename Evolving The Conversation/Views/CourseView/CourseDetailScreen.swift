import SwiftUI

// MARK: - Course Detail Screen

struct CourseDetailScreen: View {
    let course: Course
    let onBack: () -> Void
    @Environment(\.appSizeClass) var sc
    @EnvironmentObject private var authVM: AuthViewModel
    @StateObject private var progress = LessonProgressService.shared
    @State private var activeLesson: CourseLesson? = nil
    @ObservedObject private var certService = CertificateService.shared

    @State private var isUnlocked: Bool = false
    @State private var showPremiumPlans: Bool = false
    @State private var showPaymentToast: Bool = false
    @State private var showCertificatesFromToast: Bool = false

    private var enrollment: Enrollment { progress.enrollments[course.id] ?? Enrollment() }

    private var sortedLessons: [CourseLesson] {
        course.lessons.sorted { $0.order < $1.order }
    }

    private var enrollButtonWidth: CGFloat {
        switch sc {
        case .compact: return 180
        case .regular: return 240
        case .large:   return 280
        }
    }

    private var userKey: String {
        authVM.currentUser?.email ?? "guest"
    }
    private var unlockDefaultsKey: String {
        "course_unlocked_\(userKey)_\(course.id)"
    }

    private func loadUnlockState() {
        isUnlocked = UserDefaults.standard.bool(forKey: unlockDefaultsKey)
    }

    private func unlockCourse() {
        isUnlocked = true
        UserDefaults.standard.set(true, forKey: unlockDefaultsKey)

        if let firstLesson = sortedLessons.first {
            Task { await progress.enroll(courseId: course.id, firstLessonId: firstLesson.id) }
        }

        withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
            showPaymentToast = true
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.2) {
            withAnimation(.easeInOut(duration: 0.25)) {
                showPaymentToast = false
            }
        }
    }

    var body: some View {
        let pad      = AppSizing.Padding.forClass(sc)
        let heroH    = AppSizing.mediaHeight(base: 220, sizeClass: sc)
        let playSize = AppSizing.iconSize(base: 64, sizeClass: sc)
        let navSize  = AppSizing.iconSize(base: 36, sizeClass: sc)
        let navR     = AppSizing.scaled(10, sc)
        let avatarSz = AppSizing.iconSize(base: 44, sizeClass: sc)
        let cardR    = AppSizing.scaled(14, sc)

        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {

                // ── Hero ─────────────────────────────────────────────
                ZStack(alignment: .bottom) {
                    LinearGradient(
                        colors: [course.accentColor, course.accentColorDark],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                    .ignoresSafeArea(edges: .top)

                    // Play button floats on the colored gradient — stays
                    // white in both modes for contrast.
                    Circle()
                        .fill(Color.white)
                        .frame(width: playSize, height: playSize)
                        .overlay(
                            Image(systemName: "play.fill")
                                .font(.system(size: AppSizing.fontSize(base: 24, sizeClass: sc)))
                                .foregroundColor(course.accentColor)
                                .offset(x: AppSizing.scaled(2, sc))
                        )
                        .padding(.bottom, AppSizing.scaled(28, sc))

                    VStack {
                        HStack {
                            Button(action: onBack) {
                                ZStack {
                                    RoundedRectangle(cornerRadius: navR)
                                        .fill(Color.white.opacity(0.25))
                                        .frame(width: navSize, height: navSize)
                                    Image(systemName: "chevron.left")
                                        .font(.system(size: AppSizing.fontSize(base: 14, sizeClass: sc), weight: .semibold))
                                        .foregroundColor(.white)
                                }
                            }
                            Spacer()
                        }
                        .padding(.horizontal, pad.md)
                        .padding(.top, pad.md)
                        Spacer()
                    }
                }
                .frame(height: heroH)

                // ── Body ─────────────────────────────────────────────
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {

                        VStack(alignment: .center, spacing: AppSizing.scaled(8, sc)) {
                            Text(course.category.uppercased())
                                .font(.system(size: AppSizing.fontSize(base: 11, sizeClass: sc), weight: .bold))
                                .foregroundColor(AppTheme.Colors.greenFallback)
                                .tracking(0.5)

                            Text(String(format: NSLocalizedString("Complete %@", comment: ""), course.title))                                .font(AppTheme.Typography.title1(sc))
                                .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                                .multilineTextAlignment(.center)

                            HStack(spacing: AppSizing.scaled(12, sc)) {
                                Label("4.9 (2.1k)", systemImage: "star.fill")
                                    .overlay(
                                        Image(systemName: "star.fill")
                                            .foregroundColor(.orange)
                                            .allowsHitTesting(false),
                                        alignment: .leading
                                    )
                                Label(course.duration, systemImage: "clock")
                                Label("\(course.lessons.count) lessons", systemImage: "doc")
                            }
                            .font(.system(size: AppSizing.fontSize(base: 12, sizeClass: sc)))
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)

                            Text("Build natural, confident speaking habits through guided practice, real conversations, and instant feedback on pronunciation.")
                                .font(AppTheme.Typography.subheadline(sc))
                                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                                .multilineTextAlignment(.center)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, pad.md)
                        .padding(.top, pad.md)

                        // Instructor card
                        HStack(spacing: AppSizing.scaled(12, sc)) {
                            Circle()
                                .fill(LinearGradient(
                                    colors: [AppTheme.Colors.primaryFallback, AppTheme.Colors.greenFallback],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing))
                                .frame(width: avatarSz, height: avatarSz)
                                .overlay(
                                    Text(String(course.author.split(separator: " ").compactMap(\.first).prefix(2)))
                                        .font(.system(size: AppSizing.fontSize(base: 14, sizeClass: sc), weight: .bold))
                                        .foregroundColor(.white)
                                )

                            VStack(alignment: .leading, spacing: 2) {
                                Text(course.author)
                                    .font(AppTheme.Typography.headline(sc))
                                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                                Text("Senior English Coach · 14 yrs")
                                    .font(AppTheme.Typography.caption(sc))
                                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                            }
                            Spacer()
                            Image(systemName: "checkmark.seal")
                                .font(.system(size: AppSizing.fontSize(base: 17, sizeClass: sc)))
                                .foregroundColor(AppTheme.Colors.primaryFallback)
                        }
                        .padding(pad.md)
                        .background(AppTheme.Colors.surface)      // was: Color.white
                        .cornerRadius(cardR)
                        .overlay(RoundedRectangle(cornerRadius: cardR)
                            .stroke(AppTheme.Colors.borderFallback, lineWidth: 1))
                        .padding(.horizontal, pad.md)
                        .padding(.top, pad.md)

                        // Curriculum
                        Text("Curriculum")
                            .font(AppTheme.Typography.title2(sc))
                            .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                            .padding(.horizontal, pad.md)
                            .padding(.top, pad.lg)
                            .padding(.bottom, pad.sm)

                        ForEach(Array(sortedLessons.enumerated()), id: \.element.id) { i, lesson in
                            let state = enrollment.state(for: lesson, in: course)
                            let isPaymentLocked = !isUnlocked && i > 0

                            ZStack {
                                LessonRow(index: i, lesson: lesson, state: state) {
                                    if state != .locked && !isPaymentLocked {
                                        activeLesson = lesson
                                    }
                                }
                                .blur(radius: isPaymentLocked ? 6 : 0)
                                .allowsHitTesting(!isPaymentLocked)

                                if isPaymentLocked {
                                    Image(systemName: "lock.fill")
                                        .font(.system(size: AppSizing.fontSize(base: 15, sizeClass: sc), weight: .semibold))
                                        .foregroundColor(.white)
                                        .padding(10)
                                        .background(Circle().fill(Color.black.opacity(0.55)))
                                        .allowsHitTesting(false)
                                }
                            }
                            .padding(.horizontal, pad.md)
                            .padding(.bottom, AppSizing.scaled(8, sc))
                        }
                    }
                    .padding(.bottom, 100)
                    .frame(maxWidth: AppSizing.maxContentWidth(sc))
                    .frame(maxWidth: .infinity)
                }
                .background(AppTheme.Colors.backgroundFallback)
            }

            // ── Bottom bar ───────────────────────────────────────────
            if !isUnlocked {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Lifetime access")
                            .font(AppTheme.Typography.caption(sc))
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        Text("$49.00")
                            .font(AppTheme.Typography.title1(sc))
                            .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    }
                    Spacer()

                    ETCButton(
                        title: "Enroll now",
                        action: { showPremiumPlans = true }
                    )
                    .frame(maxWidth: enrollButtonWidth)
                }
                .padding(.horizontal, pad.md)
                .padding(.vertical, 12)
                .frame(maxWidth: AppSizing.maxContentWidth(sc))
                .frame(maxWidth: .infinity)
                .background(AppTheme.Colors.surface.shadow(color: .black.opacity(0.08), radius: 16, x: 0, y: -4))   // was: Color.white
            }
        }
        .overlay(alignment: .top) {
            if showPaymentToast {
                PaymentToast(sc: sc)
                    .padding(.top, AppSizing.scaled(56, sc))
                    .transition(.move(edge: .top).combined(with: .opacity))
                    .zIndex(10)
            }
        }
        .overlay(alignment: .top) {
            if let earned = certService.justUnlocked, earned.courseTitle == course.title {
                CertificateEarnedToast(courseTitle: earned.courseTitle, sc: sc)
                    .padding(.top, AppSizing.scaled(56, sc))
                    .transition(.move(edge: .top).combined(with: .opacity))
                    .zIndex(11)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        withAnimation { certService.dismissToast() }
                        showCertificatesFromToast = true
                    }
                    .task {
                        try? await Task.sleep(nanoseconds: 4_000_000_000)
                        withAnimation { certService.dismissToast() }
                    }
            }
        }
        .ignoresSafeArea(edges: .bottom)
        .onAppear {
            loadUnlockState()
            certService.start()
        }
        .fullScreenCover(isPresented: $showPremiumPlans) {
            PremiumPlansScreen(onPurchaseComplete: { unlockCourse() })
                .appSizeClass()
        }
        .fullScreenCover(item: $activeLesson) { lesson in
            VideoLessonScreen(course: course, lesson: lesson, onBack: { activeLesson = nil })
                .appSizeClass()
        }
        .fullScreenCover(isPresented: $showCertificatesFromToast) {
            CertificatesScreen().appSizeClass()
        }
    }
}

// MARK: - Payment Toast (brand-green background — fine in both modes)

private struct PaymentToast: View {
    let sc: AppSizing.SizeClass

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: AppSizing.fontSize(base: 16, sizeClass: sc)))
                .foregroundColor(.white)
            Text("Payment successful")
                .font(AppTheme.Typography.subheadline(sc))
                .fontWeight(.semibold)
                .foregroundColor(.white)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(AppTheme.Colors.greenFallback)
        .cornerRadius(14)
        .shadow(color: .black.opacity(0.20), radius: 12, y: 4)
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Certificate Earned Toast (brand-blue background — fine in both modes)

private struct CertificateEarnedToast: View {
    let courseTitle: String
    let sc: AppSizing.SizeClass

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle().fill(Color.white.opacity(0.25))
                    .frame(width: 40, height: 40)
                Image(systemName: "rosette")
                    .foregroundColor(.white)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text("🎉 New certificate!")
                    .font(AppTheme.Typography.headline(sc))
                    .foregroundColor(.white)
                Text(String(format: NSLocalizedString("%@ — view it in Certificates", comment: ""), courseTitle))                    .font(AppTheme.Typography.caption(sc))
                    .foregroundColor(.white.opacity(0.85))
            }
            Spacer()
        }
        .padding(14)
        .background(AppTheme.Colors.primaryFallback)
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.2), radius: 12, y: 4)
        .padding(.horizontal, 16)
    }
}

// MARK: - Lesson Row

private struct LessonRow: View {
    let index: Int
    let lesson: CourseLesson
    let state: LessonState
    let action: () -> Void
    @Environment(\.appSizeClass) var sc

    var body: some View {
        let iconSz    = AppSizing.iconSize(base: 32, sizeClass: sc)
        let rowRadius = AppSizing.scaled(12, sc)
        let hPad      = AppSizing.scaled(16, sc)
        let vPad      = AppSizing.scaled(12, sc)

        Button(action: action) {
            HStack(spacing: AppSizing.scaled(12, sc)) {

                ZStack {
                    switch state {
                    case .done:
                        // Pale green tint — hardcoded light value, same
                        // category as HomeCategoryItem's pastel chips.
                        // Flagging for a dynamic pass later; not broken,
                        // just won't look ideal in dark mode yet.
                        Circle()
                            .fill(Color(red: 0.91, green: 0.99, blue: 0.95))
                            .frame(width: iconSz, height: iconSz)
                        Image(systemName: "checkmark")
                            .font(.system(size: AppSizing.fontSize(base: 13, sizeClass: sc), weight: .semibold))
                            .foregroundColor(AppTheme.Colors.greenFallback)

                    case .inProgress:
                        Circle()
                            .fill(AppTheme.Colors.primaryLightFallback)
                            .frame(width: iconSz, height: iconSz)
                        Image(systemName: "play.fill")
                            .font(.system(size: AppSizing.fontSize(base: 12, sizeClass: sc)))
                            .foregroundColor(AppTheme.Colors.primaryFallback)
                            .offset(x: AppSizing.scaled(1, sc))

                    case .locked:
                        Circle()
                            .fill(AppTheme.Colors.backgroundFallback)
                            .frame(width: iconSz, height: iconSz)
                        Text("\(index + 1)")
                            .font(.system(size: AppSizing.fontSize(base: 11, sizeClass: sc), weight: .semibold))
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    }
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text(lesson.title)
                        .font(.system(size: AppSizing.fontSize(base: 13, sizeClass: sc), weight: .semibold))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    Text(lesson.duration)
                        .font(.system(size: AppSizing.fontSize(base: 11, sizeClass: sc)))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                }

                Spacer()

                if state == .inProgress {
                    Text("Resume")
                        .font(.system(size: AppSizing.fontSize(base: 12, sizeClass: sc), weight: .semibold))
                        .foregroundColor(AppTheme.Colors.primaryFallback)
                }
            }
            .padding(.vertical, vPad)
            .padding(.horizontal, hPad)
            .background(AppTheme.Colors.surface)      // was: Color.white
            .cornerRadius(rowRadius)
            .overlay(
                RoundedRectangle(cornerRadius: rowRadius)
                    .stroke(
                        state == .inProgress
                            ? AppTheme.Colors.primaryFallback
                            : AppTheme.Colors.borderFallback,
                        lineWidth: state == .inProgress ? 1.5 : 1
                    )
            )
        }
        .buttonStyle(.plain)
        .disabled(state == .locked)
    }
}
