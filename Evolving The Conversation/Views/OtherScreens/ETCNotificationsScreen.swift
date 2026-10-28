import SwiftUI

// MARK: - Notifications Screen
struct ETCNotificationsScreen: View {
    @Binding var isPresented: Bool
    @Environment(\.appSizeClass) var sc
    @Environment(\.dismiss) var dismiss
    
    
    private let notifications: [AppNotification] = [
        AppNotification(
            id: "1",
            icon: "book",
            iconColor: AppTheme.Colors.primaryFallback,
            iconBg: AppTheme.Colors.primaryLightFallback,
            title: "New lesson available",
            body: "'Advanced Idioms' is now live in Spoken English.",
            time: "2m",
            isUnread: true
        ),
        AppNotification(
            id: "2",
            icon: "video",
            iconColor: AppTheme.Colors.red,
            iconBg: Color(red: 1, green: 0.92, blue: 0.92),
            title: "Live class in 1 hour",
            body: "IELTS Speaking Workshop with Daniel R.",
            time: "1h",
            isUnread: true
        ),
        AppNotification(
            id: "3",
            icon: "rosette",
            iconColor: AppTheme.Colors.greenFallback,
            iconBg: Color(red: 0.90, green: 0.97, blue: 0.93),
            title: "Quiz result",
            body: "You scored 90% on Grammar · Tenses 🎉",
            time: "3h",
            isUnread: false
        ),
        AppNotification(
            id: "4",
            icon: "sparkles",
            iconColor: AppTheme.Colors.orangeFallback,
            iconBg: Color(red: 1.0, green: 0.95, blue: 0.88),
            title: "Weekend offer",
            body: "50% off Yearly Premium — ends Sunday.",
            time: "1d",
            isUnread: false
        ),
    ]
    // NOTE: the notification titles/bodies above are sample data, not UI
    // chrome — they're intentionally left un-localized for now (matches
    // the note in Localizable.strings). Send them for translation
    // separately if you want them localized too.
    
    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)
        
        VStack(spacing: 0) {
            // ── FIX: was a raw "Notifications" string — now localized ──
            ETCScreenBackBar(title: NSLocalizedString("Notifications", comment: "")) { dismiss() }
            
            ScrollView {
                VStack(spacing: pad.sm) {
                    ForEach(notifications) { notif in
                        NotificationCard(notif: notif)
                    }
                }
                .padding(.horizontal, pad.lg)
                .padding(.top, pad.md)
                .padding(.bottom, 40)
                .frame(maxWidth: AppSizing.maxContentWidth(sc))
                .frame(maxWidth: .infinity)
            }   // existing scroll content unchanged
        }
        .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
    }
}

// MARK: - Notification Card
private struct NotificationCard: View {
    let notif: AppNotification
    @Environment(\.appSizeClass) var sc

    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            // Icon
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(notif.iconBg)
                    .frame(
                        width: AppSizing.iconSize(base: 44, sizeClass: sc),
                        height: AppSizing.iconSize(base: 44, sizeClass: sc)
                    )
                Image(systemName: notif.icon)
                    .font(.system(size: AppSizing.fontSize(base: 18, sizeClass: sc), weight: .medium))
                    .foregroundColor(notif.iconColor)
            }

            // Text content
            VStack(alignment: .leading, spacing: 4) {
                HStack(alignment: .firstTextBaseline) {
                    Text(notif.title)
                        .font(AppTheme.Typography.headline(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                        .fixedSize(horizontal: false, vertical: true)

                    Spacer()

                    Text(notif.time)
                        .font(AppTheme.Typography.caption(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                }

                Text(notif.body)
                    .font(AppTheme.Typography.subheadline(sc))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(AppSizing.Padding.forClass(sc).md)
        .background(AppTheme.Colors.surface)
        .cornerRadius(AppSizing.cardRadius(sc))
        .shadow(
            color: AppTheme.Shadow.card.color,
            radius: AppTheme.Shadow.card.radius,
            x: AppTheme.Shadow.card.x,
            y: AppTheme.Shadow.card.y
        )
        .overlay(
            // Unread indicator
            notif.isUnread ?
            RoundedRectangle(cornerRadius: AppSizing.cardRadius(sc))
                .strokeBorder(AppTheme.Colors.primaryFallback.opacity(0.25), lineWidth: 1.5)
            : nil
        )
    }
}
