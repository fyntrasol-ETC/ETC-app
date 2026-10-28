import SwiftUI

struct MonthlyCalendarSheet: View {
    @Binding var selectedDate: Date
    @ObservedObject var classService: LiveClassService
    var onDone: () -> Void
    @Environment(\.appSizeClass) var sc

    @State private var displayedMonth: Date = Date()

    @State private var previewDate: Date? = nil
    @State private var previewClasses: [LiveClass] = []

    private var calendar: Calendar { Calendar.current }

    private var monthTitle: String {
        let f = DateFormatter()
        f.dateFormat = "MMMM yyyy"
        return f.string(from: displayedMonth)
    }

    private var daysInGrid: [Date?] {
        guard let monthInterval = calendar.dateInterval(of: .month, for: displayedMonth),
              let firstWeekday = calendar.dateComponents([.weekday], from: monthInterval.start).weekday
        else { return [] }

        let leadingBlanks = (firstWeekday - calendar.firstWeekday + 7) % 7
        var days: [Date?] = Array(repeating: nil, count: leadingBlanks)

        var current = monthInterval.start
        while current < monthInterval.end {
            days.append(current)
            current = calendar.date(byAdding: .day, value: 1, to: current)!
        }
        return days
    }

    private var canGoPrevious: Bool {
        let startOfThisMonth = calendar.dateInterval(of: .month, for: Date())?.start ?? Date()
        let startOfDisplayed = calendar.dateInterval(of: .month, for: displayedMonth)?.start ?? Date()
        return startOfDisplayed > startOfThisMonth
    }

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        ZStack {
            VStack(spacing: pad.lg) {
                HStack {
                    Text("Live class calendar")
                        .font(AppTheme.Typography.title2(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    Spacer()
                    Button(action: onDone) {
                        Image(systemName: "xmark")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                            .frame(width: 32, height: 32)
                            .background(Color(.systemGray6))   // already dynamic
                            .clipShape(Circle())
                    }
                }

                HStack {
                    Button {
                        withAnimation { changeMonth(by: -1) }
                    } label: {
                        Image(systemName: "chevron.left")
                            .foregroundColor(canGoPrevious ? AppTheme.Colors.primaryFallback : AppTheme.Colors.textSecondaryFallback.opacity(0.4))
                    }
                    .disabled(!canGoPrevious)

                    Spacer()
                    Text(monthTitle)
                        .font(AppTheme.Typography.headline(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    Spacer()

                    Button {
                        withAnimation { changeMonth(by: 1) }
                    } label: {
                        Image(systemName: "chevron.right")
                            .foregroundColor(AppTheme.Colors.primaryFallback)
                    }
                }

                weekdayHeader

                LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 7), spacing: 10) {
                    ForEach(Array(daysInGrid.enumerated()), id: \.offset) { _, date in
                        if let date {
                            let dayClasses = classService.classes(on: date)
                            MonthDayCell(
                                date: date,
                                isSelected: calendar.isDate(date, inSameDayAs: selectedDate),
                                isPast: date < calendar.startOfDay(for: Date()),
                                eventLabel: dayClasses.first?.teacherName,
                                extraCount: max(0, dayClasses.count - 1),
                                sc: sc
                            ) {
                                handleTap(on: date, dayClasses: dayClasses)
                            }
                        } else {
                            Color.clear.frame(height: 56)
                        }
                    }
                }

                Spacer()
            }
            .padding(pad.lg)
            .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
            .onAppear { displayedMonth = selectedDate }

            if let previewDate {
                            ClassPreviewPopup(
                                date: previewDate,
                                classes: previewClasses,
                                sc: sc,
                                onClose: { self.previewDate = nil }
                            )
                        }        }
    }

    private func handleTap(on date: Date, dayClasses: [LiveClass]) {
        selectedDate = calendar.startOfDay(for: date)
        if dayClasses.isEmpty {
            onDone()
        } else {
            previewClasses = dayClasses
            previewDate = date
        }
    }

    private var weekdayHeader: some View {
        HStack {
            ForEach(shortWeekdaySymbols, id: \.self) { symbol in
                Text(symbol)
                    .font(.system(size: AppSizing.fontSize(base: 12, sizeClass: sc), weight: .semibold))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private var shortWeekdaySymbols: [String] {
        let symbols = calendar.veryShortWeekdaySymbols
        let shift = calendar.firstWeekday - 1
        return Array(symbols[shift...] + symbols[..<shift])
    }

    private func changeMonth(by value: Int) {
        if let newMonth = calendar.date(byAdding: .month, value: value, to: displayedMonth) {
            displayedMonth = newMonth
        }
    }
}

// MARK: - Day cell

private struct MonthDayCell: View {
    let date: Date
    let isSelected: Bool
    let isPast: Bool
    let eventLabel: String?
    let extraCount: Int
    let sc: AppSizing.SizeClass
    let action: () -> Void

    private var dayNumber: String {
        let f = DateFormatter(); f.dateFormat = "d"; return f.string(from: date)
    }
    private var isToday: Bool { Calendar.current.isDateInToday(date) }
    private var hasEvent: Bool { eventLabel != nil }

    var body: some View {
        Button(action: action) {
            ZStack(alignment: .topTrailing) {
                VStack(spacing: 2) {
                    Text(dayNumber)
                        .font(.system(size: AppSizing.fontSize(base: 14, sizeClass: sc), weight: isToday ? .bold : .semibold))
                        .foregroundColor(numberColor)

                    if let eventLabel {
                        Text(eventLabel)
                            .font(.system(size: AppSizing.fontSize(base: 9, sizeClass: sc), weight: .semibold))
                            .foregroundColor(labelColor)
                            .lineLimit(2)
                            .multilineTextAlignment(.center)
                            .minimumScaleFactor(0.75)
                            .padding(.horizontal, 3)
                    } else {
                        Spacer(minLength: 0)
                    }
                }
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .padding(.vertical, 4)
                .background(
                    RoundedRectangle(cornerRadius: 10)
                        .fill(backgroundFill)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(isToday && !isSelected ? AppTheme.Colors.primaryFallback : Color.clear, lineWidth: 1.5)
                )

                if extraCount > 0 {
                    Text("+\(extraCount)")
                        .font(.system(size: AppSizing.fontSize(base: 8, sizeClass: sc), weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 4)
                        .padding(.vertical, 2)
                        .background(AppTheme.Colors.primaryDarkFallback)
                        .clipShape(Capsule())
                        .offset(x: 6, y: -6)
                }
            }
        }
        .buttonStyle(.plain)
        .disabled(isPast)
        .opacity(isPast ? 0.4 : 1)
    }

    private var backgroundFill: Color {
        if isSelected { return AppTheme.Colors.primaryFallback }
        if hasEvent { return AppTheme.Colors.primaryLightFallback }   // already dynamic
        return Color.clear
    }

    private var numberColor: Color {
        if isSelected { return .white }
        if isPast { return AppTheme.Colors.textSecondaryFallback.opacity(0.35) }
        return AppTheme.Colors.textPrimaryFallback
    }

    private var labelColor: Color {
        isSelected ? .white.opacity(0.9) : AppTheme.Colors.primaryFallback
    }
}

// MARK: - Tap-to-preview popup card
 private struct ClassPreviewPopup: View {
    let date: Date
    let classes: [LiveClass]
    let sc: AppSizing.SizeClass
    let onClose: () -> Void

    private var dateLabel: String {
        let f = DateFormatter()
        f.dateFormat = "EEEE, MMM d"
        return f.string(from: date)
    }

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        ZStack {
            Color.black.opacity(0.35)
                .ignoresSafeArea()
                .onTapGesture { onClose() }

            VStack(spacing: pad.md) {
                HStack {
                    Text(dateLabel)
                        .font(AppTheme.Typography.headline(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    Spacer()
                    Button(action: onClose) {
                        Image(systemName: "xmark")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                            .frame(width: 26, height: 26)
                            .background(Color(.systemGray6))    // already dynamic
                            .clipShape(Circle())
                    }
                }

                VStack(spacing: pad.sm) {
                    ForEach(classes) { cls in
                        HStack(spacing: pad.md) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(AppTheme.Colors.primaryLightFallback)
                                    .frame(width: 40, height: 40)
                                Image(systemName: "person.fill")
                                    .font(.system(size: 16))
                                    .foregroundColor(AppTheme.Colors.primaryFallback)
                            }
                            VStack(alignment: .leading, spacing: 3) {
                                Text(cls.title)
                                    .font(AppTheme.Typography.subheadline(sc))
                                    .fontWeight(.semibold)
                                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                                Text("\(cls.teacherName) · \(cls.date.formatted(date: .omitted, time: .shortened))")
                                    .font(.system(size: AppSizing.fontSize(base: 12, sizeClass: sc)))
                                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                            }
                            Spacer()
                        }
                        .padding(pad.sm)
                        .background(Color(.systemGray6))       // already dynamic
                        .cornerRadius(10)
                    }
                }
            }
            .padding(pad.lg)
            .background(AppTheme.Colors.surfaceElevated)       // was: Color.white
            .cornerRadius(20)
            .shadow(color: .black.opacity(0.15), radius: 20, x: 0, y: 10)
            .padding(.horizontal, pad.xl)
        }
    }
}
