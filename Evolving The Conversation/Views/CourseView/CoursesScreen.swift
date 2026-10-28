import SwiftUI

// MARK: - Courses Screen

struct CoursesScreen: View {
    @Binding var selectedTab: ETCTab
    @State private var selectedFilter = "All"
    @State private var selectedCourse: Course? = nil
    @Environment(\.appSizeClass) var sc
    @State private var showSearch        = false
    @State private var showNotifications = false
    @Binding var showTabBar: Bool

    let filters = ["All", "Beginner", "Spoken", "Grammar", "Business"]

    let courses: [Course] = CourseCatalog.all

    var filtered: [Course] {
        selectedFilter == "All" ? courses : courses.filter { $0.category == selectedFilter }
    }

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        let gridSpacing = AppSizing.scaled(12, sc)
        let columns = Array(
            repeating: GridItem(.flexible(), spacing: gridSpacing),
            count: AppSizing.gridColumns(sc)
        )

            VStack(spacing: 0) {
                ETCScreenHeader(
                    title: "Courses",
                    subtitle: "Pick up where you left off",
                    onSearch: { /* your search action */ },
                    onNotification: { /* your notification action */ },
                    notificationBadge: 1,
                    style: .plain
                )

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: AppSizing.scaled(8, sc)) {
                        ForEach(filters, id: \.self) { f in
                            FilterChip(label: f, isActive: selectedFilter == f) {
                                withAnimation(.easeInOut(duration: 0.2)) { selectedFilter = f }
                            }
                        }
                    }
                    .padding(.horizontal, pad.md)
                    .padding(.vertical, pad.sm)
                }
                .background(AppTheme.Colors.backgroundFallback)

                ScrollView {
                    LazyVGrid(columns: columns, spacing: gridSpacing) {
                        ForEach(filtered) { course in
                            CourseCard(course: course)
                                .onTapGesture { selectedCourse = course }
                        }
                    }
                    .padding(pad.md)
                    .padding(.bottom, 90)
                }
                .background(AppTheme.Colors.backgroundFallback)
            }

            ETCTabBar(selectedTab: $selectedTab)
                .ignoresSafeArea(edges: .bottom)
        
        .ignoresSafeArea(edges: .bottom)
        .onAppear { LessonProgressService.shared.listenAll() }
        .fullScreenCover(item: $selectedCourse) { course in
            CourseDetailScreen(course: course, onBack: { selectedCourse = nil })
                .appSizeClass()
        }
    }
}

// MARK: - Filter Chip

private struct FilterChip: View {
    let label: String
    let isActive: Bool
    let action: () -> Void
    @Environment(\.appSizeClass) var sc

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: AppSizing.fontSize(base: 13, sizeClass: sc), weight: .medium))
                .foregroundColor(isActive ? .white : AppTheme.Colors.textSecondaryFallback)
                .padding(.horizontal, AppSizing.scaled(14, sc))
                .padding(.vertical, AppSizing.scaled(7, sc))
                .background(isActive ? AppTheme.Colors.primaryFallback : AppTheme.Colors.surface)  // was: Color.white
                .overlay(
                    Capsule().stroke(isActive ? Color.clear : AppTheme.Colors.borderFallback, lineWidth: 1.5)
                )
                .clipShape(Capsule())
        }
    }
}

// MARK: - Course Card

private struct CourseCard: View {
    let course: Course
    @Environment(\.appSizeClass) var sc
    @ObservedObject private var progressService = LessonProgressService.shared

    private var pct: Double {
        progressService.enrollments[course.id]?.progress(for: course) ?? 0
    }

    var body: some View {
        let thumbHeight = AppSizing.mediaHeight(base: 90, sizeClass: sc)

        VStack(alignment: .leading, spacing: 0) {
            ZStack(alignment: .bottomLeading) {
                LinearGradient(
                    colors: [course.accentColor, course.accentColorDark],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .frame(height: thumbHeight)

                Image(systemName: course.icon)
                    .font(.system(size: AppSizing.fontSize(base: 30, sizeClass: sc)))
                    .foregroundColor(.white.opacity(0.6))
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.bottom, 4)

                if pct > 0 {
                    // Sits on the colored thumbnail image — intentionally
                    // stays a translucent black/white pairing in both modes.
                    Text(String(format: NSLocalizedString("%d%% done", comment: ""), Int(pct * 100)))                        .font(.system(size: AppSizing.fontSize(base: 10, sizeClass: sc), weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, AppSizing.scaled(8, sc))
                        .padding(.vertical, AppSizing.scaled(3, sc))
                        .background(Color.black.opacity(0.55))
                        .clipShape(Capsule())
                        .padding(8)
                }
            }
            .clipped()

            VStack(alignment: .leading, spacing: AppSizing.scaled(4, sc)) {
                Text(course.title)
                    .font(.system(size: AppSizing.fontSize(base: 13, sizeClass: sc), weight: .bold))
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)

                Text(course.author)
                    .font(.system(size: AppSizing.fontSize(base: 11, sizeClass: sc)))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)

                HStack(spacing: AppSizing.scaled(8, sc)) {
                    Label(course.duration, systemImage: "clock")
                    Label(course.students, systemImage: "person.2")
                }
                .font(.system(size: AppSizing.fontSize(base: 10, sizeClass: sc)))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)

                GeometryReader { g in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 2)
                            .fill(AppTheme.Colors.borderFallback)
                            .frame(height: 3)
                        RoundedRectangle(cornerRadius: 2)
                            .fill(course.accentColor)
                            .frame(width: g.size.width * pct, height: 3)
                    }
                }
                .frame(height: 3)
            }
            .padding(AppSizing.scaled(10, sc))
        }
        .background(AppTheme.Colors.surface)          // was: Color.white
        .cornerRadius(AppTheme.Radius.large)
        .shadow(color: AppTheme.Shadow.card.color, radius: AppTheme.Shadow.card.radius,
                x: AppTheme.Shadow.card.x, y: AppTheme.Shadow.card.y)
    }
}
