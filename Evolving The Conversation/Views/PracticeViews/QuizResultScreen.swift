import SwiftUI

// MARK: - Quiz Result Screen
struct QuizResultScreen: View {
    let score: Double   // fractional — match-the-pairs can award partial credit
    let total: Int
    let category: String
    let onRetry: () -> Void
    let onBack: () -> Void
    @Environment(\.appSizeClass) var sc

    private let passThreshold: Double = 0.60

    private var percentage: Int {
        guard total > 0 else { return 0 }
        return Int((score / Double(total) * 100).rounded())
    }

    private var missed: Double { Double(total) - score }

    private var resultTitle: String {
        switch percentage {
        case 80...100: return "Excellent!"
        case 60...79:  return "Good effort!"
        default:       return "Keep going!"
        }
    }

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        ZStack {
            AppTheme.Colors.backgroundFallback.ignoresSafeArea()

            VStack(spacing: 0) {
                HStack(spacing: AppSizing.scaled(12, sc)) {
                    ETCBackButton(action: onBack)
                    Text("Quiz Results")
                        .font(AppTheme.Typography.title2(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    Spacer()
                }
                .padding(.horizontal, pad.lg)
                .padding(.vertical, pad.md)
                .background(AppTheme.Colors.surface)

                ScrollView(showsIndicators: false) {
                    VStack(spacing: pad.lg) {

                        // Hero score card
                        VStack(spacing: AppSizing.scaled(10, sc)) {
                            ZStack {
                                Circle()
                                    .fill(Color.white.opacity(0.2))
                                    .frame(width: AppSizing.iconSize(base: 64, sizeClass: sc),
                                           height: AppSizing.iconSize(base: 64, sizeClass: sc))
                                Image(systemName: "trophy.fill")
                                    .font(.system(size: AppSizing.fontSize(base: 26, sizeClass: sc)))
                                    .foregroundColor(.white)
                            }

                            Text("\(percentage)%")
                                .font(.system(size: AppSizing.fontSize(base: 40, sizeClass: sc), weight: .bold))
                                .foregroundColor(.white)

                            Text("\(formatted(score)) out of \(total) points")
                                .font(AppTheme.Typography.subheadline(sc))
                                .foregroundColor(.white.opacity(0.85))

                            Text(resultTitle)
                                .font(.system(size: AppSizing.fontSize(base: 13, sizeClass: sc), weight: .semibold))
                                .foregroundColor(.white)
                                .padding(.horizontal, AppSizing.scaled(14, sc))
                                .padding(.vertical, AppSizing.scaled(6, sc))
                                .background(Color.white.opacity(0.2))
                                .clipShape(Capsule())
                        }
                        .frame(maxWidth: .infinity)
                        .padding(pad.lg)
                        .background(
                            LinearGradient(colors: [AppTheme.Colors.primaryFallback, AppTheme.Colors.primaryDarkFallback],
                                           startPoint: .topLeading, endPoint: .bottomTrailing)
                        )
                        .cornerRadius(AppSizing.cardRadius(sc))
                        .padding(.top, pad.md)

                        // Progress Breakdown
                        VStack(alignment: .leading, spacing: pad.sm) {
                            Text("Progress Breakdown")
                                .font(AppTheme.Typography.headline(sc))
                                .foregroundColor(AppTheme.Colors.textPrimaryFallback)

                            HStack(spacing: pad.sm) {
                                BreakdownStat(value: formatted(score), label: "Correct",
                                              color: AppTheme.Colors.greenFallback,
                                              bg: Color(red: 0.90, green: 0.97, blue: 0.93))
                                BreakdownStat(value: formatted(missed), label: "Missed",
                                              color: Color(red: 0.878, green: 0.224, blue: 0.224),
                                              bg: Color(red: 1.0, green: 0.93, blue: 0.93))
                                BreakdownStat(value: "\(percentage)%", label: "Score",
                                              color: AppTheme.Colors.primaryFallback,
                                              bg: AppTheme.Colors.primaryLightFallback)
                            }
                        }

                        // Accuracy
                        VStack(alignment: .leading, spacing: pad.sm) {
                            Text("Accuracy")
                                .font(AppTheme.Typography.headline(sc))
                                .foregroundColor(AppTheme.Colors.textPrimaryFallback)

                            VStack(spacing: pad.sm) {
                                HStack {
                                    Text("Your score")
                                        .font(AppTheme.Typography.subheadline(sc))
                                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                                    Spacer()
                                    Text("\(percentage)%")
                                        .font(AppTheme.Typography.subheadline(sc))
                                        .fontWeight(.semibold)
                                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                                }

                                GeometryReader { geo in
                                    ZStack(alignment: .leading) {
                                        Capsule()
                                            .fill(AppTheme.Colors.borderFallback)
                                            .frame(height: AppSizing.scaled(8, sc))
                                        Capsule()
                                            .fill(
                                                LinearGradient(colors: [AppTheme.Colors.primaryFallback, AppTheme.Colors.greenFallback],
                                                               startPoint: .leading, endPoint: .trailing)
                                            )
                                            .frame(width: geo.size.width * min(1, Double(percentage) / 100),
                                                   height: AppSizing.scaled(8, sc))
                                        Rectangle()
                                            .fill(AppTheme.Colors.textSecondaryFallback.opacity(0.5))
                                            .frame(width: 2, height: AppSizing.scaled(14, sc))
                                            .offset(x: geo.size.width * passThreshold - 1,
                                                    y: -AppSizing.scaled(3, sc))
                                    }
                                }
                                .frame(height: AppSizing.scaled(14, sc))

                                HStack {
                                    Text("0%")
                                    Spacer()
                                    Text("Pass: \(Int(passThreshold * 100))%")
                                    Spacer()
                                    Text("100%")
                                }
                                .font(AppTheme.Typography.caption(sc))
                                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                            }
                            .padding(pad.md)
                            .background(AppTheme.Colors.surface)
                            .cornerRadius(AppSizing.cardRadius(sc))
                            .shadow(color: AppTheme.Shadow.card.color,
                                    radius: AppTheme.Shadow.card.radius,
                                    x: AppTheme.Shadow.card.x,
                                    y: AppTheme.Shadow.card.y)
                        }

                        // CTA buttons
                        VStack(spacing: AppSizing.scaled(10, sc)) {
                            ETCButton(title: "Retry Quiz") { onRetry() }
                            ETCButton(title: "Back to Practice", style: .secondary) { onBack() }
                        }
                    }
                    .padding(.horizontal, pad.lg)
                    .padding(.bottom, 60)
                    .frame(maxWidth: AppSizing.maxContentWidth(sc))
                    .frame(maxWidth: .infinity)
                }
            }
        }
    }

    private func formatted(_ value: Double) -> String {
        value == value.rounded() ? String(format: "%.0f", value) : String(format: "%.1f", value)
    }
}

private struct BreakdownStat: View {
    let value: String
    let label: String
    let color: Color
    let bg: Color
    @Environment(\.appSizeClass) var sc

    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.system(size: AppSizing.fontSize(base: 20, sizeClass: sc), weight: .bold))
                .foregroundColor(color)
            Text(label)
                .font(AppTheme.Typography.caption(sc))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, AppSizing.Padding.forClass(sc).sm)
        .background(bg)
        .cornerRadius(AppSizing.scaled(12, sc))
    }
}
