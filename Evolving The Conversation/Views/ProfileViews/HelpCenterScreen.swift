import SwiftUI

struct HelpCenterScreen: View {
    @Environment(\.appSizeClass) var sc
    @Environment(\.dismiss) var dismiss
    
    struct FAQ: Identifiable {
        let id = UUID()
        let question: String
        let answer: String
    }
    
    // ── FIX: question/answer were raw literals in a String field, then
    // rendered with Text(faq.question) inside FAQRow — never localized.
    // Wrapping at creation time fixes it, since all these keys already
    // exist in Localizable.strings. ──
    private let faqs: [FAQ] = [
        FAQ(question: NSLocalizedString("How do I reset my streak?", comment: ""),
            answer: NSLocalizedString("Your streak resets if you miss a day. You can use a streak freeze from the shop to protect it.", comment: "")),
        FAQ(question: NSLocalizedString("How are certificates issued?", comment: ""),
            answer: NSLocalizedString("Certificates are issued automatically when you complete all lessons in a course with a passing score.", comment: "")),
        FAQ(question: NSLocalizedString("Can I learn offline?", comment: ""),
            answer: NSLocalizedString("Yes! Premium members can download lessons for offline access from the course detail screen.", comment: "")),
    ]
    
    @State private var expanded: UUID? = nil
    
    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        
        VStack(spacing: 0) {
            // ── FIX: was raw "Help Center" literal handed to a String param,
            // rendered with Text(title) inside ETCBackButton — never localized. ──
            ETCScreenBackBar(title: NSLocalizedString("Help Center", comment: "")) { dismiss() }
            
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: pad.lg) {
                    // ── FAQ ───────────────────────────────────
                    VStack(alignment: .leading, spacing: pad.sm) {
                        // This one was already fine — a literal typed directly
                        // inside Text(...) auto-localizes. Left unchanged, but
                        // wrapping explicitly doesn't hurt and keeps things consistent.
                        Text(NSLocalizedString("FAQ", comment: ""))
                            .font(AppTheme.Typography.title2(sc))
                            .fontWeight(.bold)
                            .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                        
                        VStack(spacing: pad.sm) {
                            ForEach(faqs) { faq in
                                FAQRow(faq: faq, expanded: $expanded)
                            }
                        }
                    }
                    
                    // ── Contact support ───────────────────────
                    VStack(alignment: .leading, spacing: pad.sm) {
                        Text(NSLocalizedString("Contact support", comment: ""))
                            .font(AppTheme.Typography.title2(sc))
                            .fontWeight(.bold)
                            .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                        
                        HStack(spacing: pad.sm) {
                            // ── FIX: label was a raw literal into a String param,
                            // rendered with Text(label) inside ContactButton — never localized. ──
                            ContactButton(
                                icon: "envelope.fill",
                                label: NSLocalizedString("Email", comment: ""),
                                color: AppTheme.Colors.primaryFallback
                            ) { }
                            ContactButton(
                                icon: "bubble.left.fill",
                                label: NSLocalizedString("WhatsApp", comment: ""),
                                color: AppTheme.Colors.greenFallback
                            ) { }
                        }
                    }
                }//
                .padding(pad.lg)
                .frame(maxWidth: AppSizing.maxContentWidth(sc))
                .frame(maxWidth: .infinity)
            }
        }
        .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
        .navigationBarBackButtonHidden(true)
    }
}

private struct FAQRow: View {
    let faq: HelpCenterScreen.FAQ
    @Binding var expanded: UUID?
    @Environment(\.appSizeClass) var sc

    private var isOpen: Bool { expanded == faq.id }

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        VStack(alignment: .leading, spacing: 0) {
            Button {
                withAnimation(.easeInOut(duration: 0.25)) {
                    expanded = isOpen ? nil : faq.id
                }
            } label: {
                HStack {
                    Text(faq.question)
                        .font(AppTheme.Typography.body(sc))
                        .fontWeight(.medium)
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                        .multilineTextAlignment(.leading)
                    Spacer()
                    Image(systemName: "chevron.down")
                        .font(.system(
                            size: AppSizing.fontSize(base: 13, sizeClass: sc),
                            weight: .semibold
                        ))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        .rotationEffect(.degrees(isOpen ? 180 : 0))
                }
                .padding(pad.md)
            }

            if isOpen {
                Text(faq.answer)
                    .font(AppTheme.Typography.body(sc))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    .padding(.horizontal, pad.md)
                    .padding(.bottom, pad.md)
            }
        }
        .background(AppTheme.Colors.surface)
        .cornerRadius(AppSizing.cardRadius(sc))
        .shadow(
            color: AppTheme.Shadow.card.color,
            radius: AppTheme.Shadow.card.radius,
            x: 0, y: AppTheme.Shadow.card.y
        )
    }
}

private struct ContactButton: View {
    let icon: String
    let label: String
    let color: Color
    let action: () -> Void
    @Environment(\.appSizeClass) var sc

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        Button(action: action) {
            VStack(spacing: pad.sm) {
                Image(systemName: icon)
                    .font(.system(
                        size: AppSizing.fontSize(base: 28, sizeClass: sc),
                        weight: .regular
                    ))
                    .foregroundColor(color)
                Text(label)
                    .font(AppTheme.Typography.body(sc))
                    .fontWeight(.medium)
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
            }
            .frame(maxWidth: .infinity)
            .padding(pad.lg)
            .background(AppTheme.Colors.surface)
            .cornerRadius(AppSizing.cardRadius(sc))
            .shadow(
                color: AppTheme.Shadow.card.color,
                radius: AppTheme.Shadow.card.radius,
                x: 0, y: AppTheme.Shadow.card.y
            )
        }
    }
}
