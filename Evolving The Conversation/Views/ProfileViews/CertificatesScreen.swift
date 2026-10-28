import SwiftUI

struct CertificatesScreen: View {
    @Environment(\.appSizeClass) var sc
    @Environment(\.dismiss) var dismiss
    @ObservedObject private var certService = CertificateService.shared
    @ObservedObject private var progressService = LessonProgressService.shared

    private var columns: [GridItem] {
        [GridItem(.flexible(), spacing: AppSizing.Padding.forClass(sc).sm),
         GridItem(.flexible(), spacing: AppSizing.Padding.forClass(sc).sm)]
    }

    // Earned certificates lead the grid (most recently earned first);
    // locked ones trail in catalog order.
    private var sortedCourses: [Course] {
        CourseCatalog.all.sorted { a, b in
            let earnedA = certService.earnedCertificates[a.id]
            let earnedB = certService.earnedCertificates[b.id]

            switch (earnedA, earnedB) {
            case let (a?, b?):
                return a.earnedAt > b.earnedAt   // both earned — newest first
            case (.some, .none):
                return true                       // earned beats locked
            case (.none, .some):
                return false
            case (.none, .none):
                return false                      // keep catalog order among locked
            }
        }
    }

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        VStack(spacing: 0) {
            ETCScreenBackBar(title: "Certificates") { dismiss() }

            ScrollView(showsIndicators: false) {
                LazyVGrid(columns: columns, spacing: pad.sm) {
                    ForEach(sortedCourses) { course in
                        if let earned = certService.earnedCertificates[course.id] {
                            CertCard(course: course, earnedAt: earned.earnedAt)
                        } else {
                            LockedCertCard(courseTitle: course.title)
                        }
                    }
                }
                .padding(pad.lg)
                .frame(maxWidth: AppSizing.maxContentWidth(sc))
                .frame(maxWidth: .infinity)
            }
        }
        .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
        .navigationBarBackButtonHidden(true)
        .onAppear {
            certService.start()
            progressService.listenAll()
        }
    }
}

private struct CertCard: View {
    let course: Course
    let earnedAt: Date
    @Environment(\.appSizeClass) var sc

    private var formattedDate: String {
        let f = DateFormatter()
        f.dateFormat = "MMM yyyy"
        return String(format: NSLocalizedString("Issued %@", comment: ""), f.string(from: earnedAt))
    }

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        let iconH = AppSizing.iconSize(base: 120, sizeClass: sc)

        VStack(spacing: 0) {
            ZStack {
                course.accentColor
                Image(systemName: "rosette")
                    .font(.system(size: AppSizing.fontSize(base: 44, sizeClass: sc), weight: .regular))
                    .foregroundColor(.white)
            }
            .frame(height: iconH)
            .cornerRadius(AppSizing.cardRadius(sc), corners: [.topLeft, .topRight])

            VStack(spacing: 4) {
                Text(course.title)
                    .font(AppTheme.Typography.headline(sc))
                    .fontWeight(.bold)
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    .multilineTextAlignment(.center)
                Text(formattedDate)
                    .font(AppTheme.Typography.caption(sc))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
            }
            .padding(pad.sm)
            .frame(maxWidth: .infinity)
            .background(AppTheme.Colors.surface)
            .cornerRadius(AppSizing.cardRadius(sc), corners: [.bottomLeft, .bottomRight])
        }
        .cornerRadius(AppSizing.cardRadius(sc))
        .overlay(
            RoundedRectangle(cornerRadius: AppSizing.cardRadius(sc))
                .stroke(AppTheme.Colors.borderFallback.opacity(0.5), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.08), radius: 8, x: 0, y: 3)
    }
}

private struct LockedCertCard: View {
    let courseTitle: String
    @Environment(\.appSizeClass) var sc

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        let iconH = AppSizing.iconSize(base: 120, sizeClass: sc)

        VStack(spacing: pad.sm) {
            Image(systemName: "lock.fill")
                .font(.system(size: AppSizing.fontSize(base: 36, sizeClass: sc), weight: .regular))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
            Text(String(format: NSLocalizedString("Complete %@ to unlock", comment: ""), courseTitle))                .font(AppTheme.Typography.caption(sc))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .frame(height: iconH + 70)
        .background(AppTheme.Colors.surface)
        .cornerRadius(AppSizing.cardRadius(sc))
        .overlay(
            RoundedRectangle(cornerRadius: AppSizing.cardRadius(sc))
                .strokeBorder(style: StrokeStyle(lineWidth: 1.5, dash: [6]))
                .foregroundColor(AppTheme.Colors.borderFallback)
        )
    }
}

// MARK: - Corner radius helper
extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

struct RoundedCorner: Shape {
    var radius: CGFloat = 0
    var corners: UIRectCorner = .allCorners

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(roundedRect: rect,
                                byRoundingCorners: corners,
                                cornerRadii: CGSize(width: radius, height: radius))
        return Path(path.cgPath)
    }
}
