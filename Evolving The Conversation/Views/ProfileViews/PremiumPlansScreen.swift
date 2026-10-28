import SwiftUI

struct PremiumPlansScreen: View {
    @Environment(\.appSizeClass) var sc
    @Environment(\.dismiss) var dismiss
    @State private var selectedPlan: Plan = .yearly

    // Fired when the user taps "Upgrade now". No real billing is wired up
    // yet, so selecting a plan and tapping this button is treated as a
    // successful purchase for now — swap the body of the button action
    // out for a real payment call once that's ready. The caller (e.g.
    // CourseDetailScreen) is responsible for unlocking content + showing
    // its own confirmation once this fires.
    var onPurchaseComplete: (() -> Void)? = nil

    enum Plan { case monthly, yearly, lifetime }
 
    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        ZStack(alignment: .bottom) {
            ScrollView(showsIndicators: false) {
                VStack(spacing: pad.lg) {

                        // ── Back button ───────────────────────────
                        HStack {
                            // ── FIX: was raw "Premium Plans" literal — now localized ──
                            ETCBackButton(title: NSLocalizedString("Premium Plans", comment: "")) {
                                dismiss()
                            }

                            Spacer()
                        }
                        .padding(.top, pad.sm)

                    // Header
                    VStack(spacing: 8) {
                        // ── FIX: raw literals below wrapped in NSLocalizedString ──
                        Text(NSLocalizedString("Unlock everything", comment: ""))
                            .font(AppTheme.Typography.largeTitle(sc))
                            .fontWeight(.bold)
                            .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                            .multilineTextAlignment(.center)
                        Text(NSLocalizedString("Unlimited courses, offline access, certificates & your AI tutor.", comment: ""))
                            .font(AppTheme.Typography.body(sc))
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                            .multilineTextAlignment(.center)
                    }
                    .padding(.top, pad.md)

                    // Plan options
                    VStack(spacing: pad.sm) {
                        // ── FIX: label / badge / savingsLabel literals now localized ──
                        PlanRow(plan: .monthly,  label: NSLocalizedString("Monthly", comment: ""),
                                price: "$9.99", period: "/mo",
                                badge: nil,         savingsLabel: nil,
                                selected: $selectedPlan)
                        PlanRow(plan: .yearly,   label: NSLocalizedString("Yearly", comment: ""),
                                price: "$59.99", period: "/yr",
                                badge: NSLocalizedString("MOST POPULAR", comment: ""),
                                savingsLabel: NSLocalizedString("Save 50%", comment: ""),
                                selected: $selectedPlan)
                        PlanRow(plan: .lifetime, label: NSLocalizedString("Lifetime", comment: ""),
                                price: "$149",  period: " once",
                                badge: nil,         savingsLabel: nil,
                                selected: $selectedPlan)
                    }

                    // Features card
                    VStack(spacing: pad.sm) {
                        ForEach(features, id: \.self) { feat in
                            HStack(spacing: 12) {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(AppTheme.Colors.greenFallback)
                                    .font(.system(size: AppSizing.fontSize(base: 20, sizeClass: sc)))
                                Text(feat)
                                    .font(AppTheme.Typography.body(sc))
                                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                                Spacer()
                            }
                        }
                    }
                    .padding(pad.md)
                    .background(AppTheme.Colors.surface)
                    .cornerRadius(AppSizing.cardRadius(sc))
                    .shadow(
                        color: AppTheme.Shadow.card.color,
                        radius: AppTheme.Shadow.card.radius,
                        x: 0, y: AppTheme.Shadow.card.y
                    )
                    .padding(.bottom, 90) // space for fixed button
                }
                .padding(.horizontal, pad.lg)
                .frame(maxWidth: AppSizing.maxContentWidth(sc))
                .frame(maxWidth: .infinity)
            }

            // Fixed upgrade button
            VStack {
                // ── FIX: was raw "Upgrade now" literal — now localized ──
                ETCButton(title: NSLocalizedString("Upgrade now", comment: ""), icon: "sparkles") {
                    // TODO: wire up real payment here once billing is added.
                    // For now, selecting a plan + tapping this is treated
                    // as a successful purchase.
                    onPurchaseComplete?()
                    dismiss()
                }
                    .padding(.horizontal, pad.lg)
                    .padding(.bottom, pad.lg)
            }
            .background(
                LinearGradient(
                    colors: [AppTheme.Colors.backgroundFallback.opacity(0),
                             AppTheme.Colors.backgroundFallback],
                    startPoint: .top, endPoint: .bottom
                )
                .frame(height: 120)
            )
        }
        .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
//        .navigationTitle("Premium Plans")
//        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
//        .toolbar {
//            ToolbarItem(placement: .navigationBarLeading) {
//                ETCBackButton { dismiss() }
//            }
//        }
    }

    // ── FIX: features array items now localized ──
    private let features = [
        NSLocalizedString("Unlimited courses", comment: ""),
        NSLocalizedString("Offline access", comment: ""),
        NSLocalizedString("Verified certificates", comment: ""),
        NSLocalizedString("Live classes", comment: ""),
        NSLocalizedString("AI tutor & feedback", comment: "")
    ]
}

private struct PlanRow: View {
    let plan: PremiumPlansScreen.Plan
    let label: String
    let price: String
    let period: String
    let badge: String?
    let savingsLabel: String?
    @Binding var selected: PremiumPlansScreen.Plan
    @Environment(\.appSizeClass) var sc

    private var isSelected: Bool { selected == plan }

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        let btn = AppSizing.Button.forClass(sc)

        Button { selected = plan } label: {
            HStack(spacing: pad.md) {
                // Radio
                ZStack {
                    Circle()
                        .stroke(isSelected
                                ? AppTheme.Colors.primaryFallback
                                : AppTheme.Colors.borderFallback,
                                lineWidth: 2)
                        .frame(width: 24, height: 24)
                    if isSelected {
                        Circle()
                            .fill(AppTheme.Colors.primaryFallback)
                            .frame(width: 14, height: 14)
                    }
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text(label)
                        .font(AppTheme.Typography.headline(sc))
                        .fontWeight(.bold)
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    if let s = savingsLabel {
                        Text(s)
                            .font(AppTheme.Typography.caption(sc))
                            .foregroundColor(AppTheme.Colors.greenFallback)
                            .fontWeight(.semibold)
                    }
                }

                Spacer()

                HStack(alignment: .lastTextBaseline, spacing: 2) {
                    Text(price)
                        .font(.system(
                            size: AppSizing.fontSize(base: 20, sizeClass: sc),
                            weight: .bold
                        ))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    Text(period)
                        .font(AppTheme.Typography.caption(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                }
            }
            .padding(.horizontal, pad.md)
            .frame(height: btn.height)
            .background(AppTheme.Colors.surface)
            .cornerRadius(btn.cornerRadius)
            .overlay(
                RoundedRectangle(cornerRadius: btn.cornerRadius)
                    .stroke(isSelected
                            ? AppTheme.Colors.primaryFallback
                            : AppTheme.Colors.borderFallback,
                            lineWidth: isSelected ? 2 : 1)
            )
            .shadow(
                color: AppTheme.Shadow.card.color,
                radius: AppTheme.Shadow.card.radius,
                x: 0, y: AppTheme.Shadow.card.y
            )
            .overlay(alignment: .top) {
                if let badge {
                    Text(badge)
                        .font(.system(
                            size: AppSizing.fontSize(base: 10, sizeClass: sc),
                            weight: .bold
                        ))
                        .foregroundColor(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(AppTheme.Colors.greenFallback)
                        .cornerRadius(AppTheme.Radius.pill)
                        .offset(y: -12)
                }
            }
        }
    }
}
