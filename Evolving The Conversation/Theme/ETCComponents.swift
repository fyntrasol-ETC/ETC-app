import SwiftUI

import SwiftUI

// ─────────────────────────────────────────────────────────────
// MARK: - Dashboard Drawer Menu
// ─────────────────────────────────────────────────────────────

struct ETCDrawerMenu: View {
    @Binding var isPresented: Bool
    @EnvironmentObject private var authVM: AuthViewModel
    @Environment(\.appSizeClass) var sc

    var onNavigate: ((DrawerDestination) -> Void)? = nil

    enum DrawerDestination {
        case home, courses, videoLessons, grammar, vocabulary
        case practice, quizzes, certificates, liveClasses
        case community, progress, settings, helpCenter
    }

    private struct DrawerItem {
        let icon: String
        let label: String
        let destination: DrawerDestination
    }

    private let items: [DrawerItem] = [
        DrawerItem(icon: "house",               label: NSLocalizedString("Home", comment: ""),          destination: .home),
        DrawerItem(icon: "book",                label: NSLocalizedString("My Courses", comment: ""),    destination: .courses),
        DrawerItem(icon: "doc.text",            label: NSLocalizedString("Grammar", comment: ""),       destination: .grammar),
        DrawerItem(icon: "sparkles",            label: NSLocalizedString("Vocabulary", comment: ""),    destination: .vocabulary),
        DrawerItem(icon: "mic",                 label: NSLocalizedString("Practice", comment: ""),      destination: .practice),
        DrawerItem(icon: "target",              label: NSLocalizedString("Quizzes", comment: ""),       destination: .quizzes),
        DrawerItem(icon: "rosette",             label: NSLocalizedString("Certificates", comment: ""),  destination: .certificates),
        DrawerItem(icon: "calendar",            label: NSLocalizedString("Live Classes", comment: ""),  destination: .liveClasses),
        DrawerItem(icon: "person.2",            label: NSLocalizedString("Community", comment: ""),     destination: .community),
        DrawerItem(icon: "chart.bar",           label: NSLocalizedString("Progress", comment: ""),      destination: .progress),
        DrawerItem(icon: "gearshape",           label: NSLocalizedString("Settings", comment: ""),      destination: .settings),
        DrawerItem(icon: "questionmark.circle", label: NSLocalizedString("Help Center", comment: ""),   destination: .helpCenter),
    ]

    var body: some View {
        // Use UIScreen for true full-screen dimensions — GeometryReader
        // inside a ZStack gets clipped by the tab bar safe area
        let screenSize = UIScreen.main.bounds.size

        ZStack(alignment: .leading) {
            Color.black.opacity(0.40)
                .ignoresSafeArea()
                .onTapGesture { close() }

            drawerPanel(screenSize: screenSize)
        }
        .ignoresSafeArea()
    }

    @ViewBuilder
    private func drawerPanel(screenSize: CGSize) -> some View {
        let drawerWidth: CGFloat = sc == .compact
            ? screenSize.width * 0.80
            : min(screenSize.width * 0.45, 320)

        let pad = AppSizing.Padding.forClass(sc)

        // Bottom safe area so Log out doesn't hide behind home indicator
        let bottomInset = (UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first?.windows.first?.safeAreaInsets.bottom) ?? 0

        VStack(spacing: 0) {
            drawerHeader(pad: pad)

            Divider()
                .padding(.horizontal, pad.md)

            // Only the nav items scroll — Log out below stays fixed
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    ForEach(items, id: \.label) { item in
                        DrawerRow(icon: item.icon, label: item.label) {
                            onNavigate?(item.destination)
                            close()
                        }
                    }
                }
                .padding(.vertical, pad.sm)
                .padding(.horizontal, pad.sm)
            }
            .frame(maxWidth: .infinity)

            // ── Fixed Log out row — always visible, never scrolls
            Divider()
                .padding(.horizontal, pad.md)

            Button {
                authVM.signOut()
                close()
            } label: {
                HStack(spacing: 14) {
                    Image(systemName: "rectangle.portrait.and.arrow.right")
                        .font(.system(
                            size: AppSizing.fontSize(base: 16, sizeClass: sc),
                            weight: .medium
                        ))
                        .foregroundColor(.red)
                        .frame(width: 26)
                   
                    Text(NSLocalizedString("Log out", comment: ""))
                        .font(AppTheme.Typography.body(sc))
                        .fontWeight(.semibold)
                        .foregroundColor(.red)
                    Spacer()
                }
                .padding(.vertical, sc == .compact ? 13 : 16)
                .padding(.horizontal, AppSizing.Padding.forClass(sc).sm)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .padding(.horizontal, pad.sm)
            // Extra bottom padding so it clears the home indicator
            .padding(.bottom, bottomInset + pad.md)
            .background(AppTheme.Colors.surface)
        }
        .frame(width: drawerWidth)
        .frame(height: screenSize.height)
        .background(AppTheme.Colors.surface)
        .shadow(color: .black.opacity(0.15), radius: 20, x: 6, y: 0)
        .frame(width: drawerWidth)
        .frame(height: screenSize.height)   // ← true screen height, ignores tab bar
        .shadow(color: .black.opacity(0.15), radius: 20, x: 6, y: 0)
    }
    // MARK: - Drawer Header (with X button + safe area fix)
    @ViewBuilder
    private func drawerHeader(pad: AppSizing.Padding) -> some View {
        // Grab the top safe area inset so the icon never overlaps the status bar
        let topInset = (UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first?.windows.first?.safeAreaInsets.top) ?? 44

        VStack(alignment: .leading, spacing: 0) {

            // ── Row: app icon  +  spacer  +  × button
            HStack(alignment: .top) {
                // App icon
                Image("logo")
                    .resizable()
                    .scaledToFit()
                    .frame(
                        width:  AppSizing.iconSize(base: 52, sizeClass: sc),
                        height: AppSizing.iconSize(base: 52, sizeClass: sc)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                Spacer()

                // × close button  (matches screenshot)
                Button(action: close) {
                    Image(systemName: "xmark")
                        .font(.system(size: AppSizing.fontSize(base: 15, sizeClass: sc),
                                      weight: .semibold))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                        .frame(width: 36, height: 36)
                        .background(Color(.systemGray6))
                        .clipShape(Circle())
                }
            }
            .padding(.bottom, 14)

            // App name
            Text(NSLocalizedString("Evolving The Conversation", comment: ""))
                .font(AppTheme.Typography.headline(sc))
                .fontWeight(.bold)
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.bottom, 6)

            // User name · Premium
            HStack(spacing: 6) {
                Text(
                    authVM.currentUser?.displayName?.components(separatedBy: " ").first
                    ?? authVM.currentUser?.email?.components(separatedBy: "@").first
                    ?? "User"
                )
                .font(AppTheme.Typography.subheadline(sc))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)

                Text("·")
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)

                Text(NSLocalizedString("Premium", comment: ""))
                    .font(.system(size: AppSizing.fontSize(base: 13, sizeClass: sc), weight: .semibold))
                    
                    .foregroundColor(AppTheme.Colors.primaryFallback)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(pad.md)
        // ↓ Push the entire header below the status bar
        .padding(.top, topInset + 8)
    }

    private func close() {
        withAnimation(.easeInOut(duration: 0.28)) {
            isPresented = false
        }
    }
}

// MARK: - Drawer Row
private struct DrawerRow: View {
    let icon: String
    let label: String
    let action: () -> Void

    @Environment(\.appSizeClass) var sc

    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                Image(systemName: icon)
                    .font(.system(size: AppSizing.fontSize(base: 18, sizeClass: sc), weight: .medium))
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    .frame(width: 26)

                Text(label)
                    .font(AppTheme.Typography.body(sc))
                    .fontWeight(.semibold)
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)

                Spacer()
            }
            .padding(.vertical, sc == .compact ? 13 : 16)
            .padding(.horizontal, AppSizing.Padding.forClass(sc).sm)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

struct ETCButton: View {
    let title: String
    var icon: String? = nil
    var style: Style = .primary
    var isLoading: Bool = false
    let action: () -> Void

    @Environment(\.appSizeClass) var sc

    enum Style {
        case primary, secondary, destructive
    }

    var body: some View {
        let pad  = AppSizing.Padding.forClass(sc)
        let btn  = AppSizing.Button.forClass(sc)

        Button(action: action) {
            HStack(spacing: 8) {
                if isLoading {
                    ProgressView().tint(foregroundColor)
                } else {
                    if let icon { Image(systemName: icon) }
                    Text(title).fontWeight(.semibold)
                }
            }
            .font(AppTheme.Typography.headline(sc))
            .foregroundColor(foregroundColor)
            .frame(maxWidth: .infinity)
            .frame(height: btn.height)
            .background(backgroundColor)
            .cornerRadius(btn.cornerRadius)
            .shadow(color: AppTheme.Shadow.button.color,
                    radius: AppTheme.Shadow.button.radius,
                    x: AppTheme.Shadow.button.x,
                    y: AppTheme.Shadow.button.y)
        }
        .disabled(isLoading)
    }

    private var backgroundColor: Color {
        switch style {
        case .primary:     return AppTheme.Colors.primaryFallback
        case .secondary:   return AppTheme.Colors.surface
        case .destructive: return AppTheme.Colors.red
        }
    }

    private var foregroundColor: Color {
        switch style {
        case .primary, .destructive: return .white
        case .secondary:             return AppTheme.Colors.primaryFallback
        }
    }
}

struct ETCBackButton: View {
    var title: String? = nil      // was: let title: String?
    let action: () -> Void

    @Environment(\.appSizeClass) private var sc

    private var size: CGFloat {
        switch sc {
        case .compact: return 38
        case .regular: return 52
        case .large: return 60
        }
    }

    var body: some View {
        HStack(spacing: 12) {
            Button(action: action) {
                Image(systemName: "chevron.left")
                    .font(
                        .system(
                            size: AppSizing.fontSize(base: 16, sizeClass: sc),
                            weight: .semibold
                        )
                    )
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                    .frame(width: size, height: size)
                    .background(AppTheme.Colors.surface)
                    .cornerRadius(size * 0.26)
                    .shadow(
                        color: .black.opacity(0.06),
                        radius: 4,
                        y: 2
                    )
            }
            .buttonStyle(.plain)

            Text(title ?? "")
                .font(
                    .system(
                        size: AppSizing.fontSize(base: 22, sizeClass: sc),
                        weight: .bold
                    )
                )
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)
        }
    }
}

struct ETCScreenBackBar: View {
    var title: String? = nil          // optional — pass nil to show back button only
    let action: () -> Void

    @Environment(\.appSizeClass) var sc

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        HStack(spacing: 0) {
            ETCBackButton(title: title, action: action)
            Spacer()
        }
        .padding(.horizontal, pad.lg)
        .padding(.top, pad.sm)
        .padding(.bottom, pad.md)
        .background(AppTheme.Colors.backgroundFallback)
    }
}

struct ETCScreenHeader: View {
    let title: String
    var subtitle: String? = nil

    // Drawer (hamburger) — only Home & Profile pass true
    var showDrawer: Bool = false
    var onDrawer: (() -> Void)? = nil

    // Action callbacks — callers can override if needed
    var onSearch: (() -> Void)? = nil
    var onNotification: (() -> Void)? = nil
    var notificationBadge: Int = 0

    // Visual style
    var style: HeaderStyle = .gradient   // Home uses gradient; others use plain

    @Environment(\.appSizeClass) var sc

    enum HeaderStyle {
        case gradient   // blue gradient bg  (Home)
        case dark       // dark navy bg      (Profile)
        case plain      // light / default   (Courses, Practice, Community …)
    }

    var body: some View {
        let pad = AppSizing.Padding.forClass(sc)

        ZStack {
            background
                .ignoresSafeArea(edges: .top)

            HStack(spacing: 10) {

                // ── Left: hamburger (optional) or title block
                if showDrawer {
                    ETCNavIconButton(
                        systemName: "line.3.horizontal",
                        tint: iconTint,
                        bgColor: iconBg
                    ) { onDrawer?() }

                    Spacer()
                } else {
                    // Title + subtitle sit on the left for non-drawer screens
                    VStack(alignment: .leading, spacing: 2) {
                        Text(title)
                            .font(AppTheme.Typography.title2(sc))
                            .fontWeight(.bold)
                            .foregroundColor(labelColor)

                        if let sub = subtitle {
                            Text(sub)
                                .font(AppTheme.Typography.subheadline(sc))
                                .foregroundColor(sublabelColor)
                        }
                    }

                    Spacer()
                }

                // ── Right: search + bell
                HStack(spacing: 8) {
                    ETCNavIconButton(
                        systemName: "magnifyingglass",
                        tint: iconTint,
                        bgColor: iconBg
                    ) { onSearch?() }

                    ETCNavIconButton(
                        systemName: "bell",
                        badgeCount: notificationBadge,
                        tint: iconTint,
                        bgColor: iconBg
                    ) { onNotification?() }
                }
            }
            .padding(.horizontal, pad.lg)
            .padding(.top, 8)
            .padding(.bottom, showDrawer ? 16 : 14)

            // For drawer screens, show title centred
            if showDrawer {
                Text(title)
                    .font(AppTheme.Typography.headline(sc))
                    .fontWeight(.semibold)
                    .foregroundColor(labelColor)
            }
        }
        .fixedSize(horizontal: false, vertical: true)
    }

    // MARK: - Theming helpers

    @ViewBuilder
    private var background: some View {
        switch style {
        case .gradient:
            LinearGradient(
                colors: [AppTheme.Colors.primaryFallback,
                         AppTheme.Colors.primaryDarkFallback],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .dark:
            Color(red: 0.07, green: 0.09, blue: 0.18)
        case .plain:
            AppTheme.Colors.backgroundFallback
        }
    }

    private var iconTint: Color {
        switch style {
        case .gradient, .dark: return .white
        case .plain: return AppTheme.Colors.textPrimaryFallback
        }
    }

    private var iconBg: Color {
        switch style {
        case .gradient: return Color.white.opacity(0.18)
        case .dark:     return Color(red: 0.13, green: 0.15, blue: 0.25)
        case .plain:    return AppTheme.Colors.surface
        }
    }

    private var labelColor: Color {
        switch style {
        case .gradient, .dark: return .white
        case .plain: return AppTheme.Colors.textPrimaryFallback
        }
    }

    private var sublabelColor: Color {
        switch style {
        case .gradient, .dark: return .white.opacity(0.70)
        case .plain: return AppTheme.Colors.textSecondaryFallback
        }
    }
}

// MARK: - Tab Items
enum ETCTab: CaseIterable {
    case home, courses, speak, community, profile

    var icon: String {
        switch self {
        case .home:      return "house"
        case .courses:   return "book"
        case .speak:     return "mic"
        case .community: return "person.2"
        case .profile:   return "person"
        }
    }

    var label: String {
        switch self {
        case .home:      return NSLocalizedString("Home", comment: "")
        case .courses:   return NSLocalizedString("Courses", comment: "")
        case .speak:     return NSLocalizedString("Speak", comment: "")
        case .community: return NSLocalizedString("Community", comment: "")
        case .profile:   return NSLocalizedString("Profile", comment: "")
        }
    }    }


struct ETCTabBar: View {
    @Binding var selectedTab: ETCTab
    @Environment(\.appSizeClass) var sc

    var body: some View {
        HStack(spacing: 0) {
            ForEach(ETCTab.allCases, id: \.self) { tab in
                Button {
                    withAnimation(.easeInOut(duration: 0.2)) { selectedTab = tab }
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: selectedTab == tab
                              ? tab.icon + ".fill"
                              : tab.icon)
                            .font(.system(
                                size: AppSizing.fontSize(base: 22, sizeClass: sc),
                                weight: .medium
                            ))
                            .foregroundColor(
                                selectedTab == tab
                                ? AppTheme.Colors.primaryFallback
                                : AppTheme.Colors.textSecondaryFallback
                            )

                        Text(tab.label)
                            .font(AppTheme.Typography.caption(sc))
                            .foregroundColor(
                                selectedTab == tab
                                ? AppTheme.Colors.primaryFallback
                                : AppTheme.Colors.textSecondaryFallback
                            )
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(
                        selectedTab == tab
                        ? AppTheme.Colors.primaryFallback.opacity(0.08)
                        : Color.clear
                    )
                    .cornerRadius(12)
                }
            }
        }
        .padding(.horizontal, AppSizing.Padding.forClass(sc).sm)
        .padding(.top, 8)
        .padding(.bottom, 4)
        .background(
            AppTheme.Colors.surface
                 .shadow(color: .black.opacity(0.08), radius: 16, x: 0, y: -4)
        )
    }
}
// ─────────────────────────────────────────────
// MARK: - ETCTextField
// ─────────────────────────────────────────────
struct ETCTextField: View {
    let label: String
    let placeholder: String
    @Binding var text: String
    var leadingIcon: String? = nil
    var isSecure: Bool = false
    @State private var showPassword = false

    @Environment(\.appSizeClass) var sc

    var body: some View {
        let tf = AppSizing.TextField.forClass(sc)

        VStack(alignment: .center, spacing: 6) {
            Text(label)
                .font(AppTheme.Typography.subheadline(sc))
                .fontWeight(.semibold)
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)

            HStack(spacing: 10) {
                if let icon = leadingIcon {
                    Image(systemName: icon)
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        .frame(width: 20)
                }

                if isSecure && !showPassword {
                    SecureField(placeholder, text: $text)
                        .font(AppTheme.Typography.body(sc))
                } else {
                    TextField(placeholder, text: $text)
                        .font(AppTheme.Typography.body(sc))
                        .keyboardType(label.lowercased().contains("email") ? .emailAddress : .default)
                        .autocapitalization(.none)
                }

                if isSecure {
                    Button { showPassword.toggle() } label: {
                        Image(systemName: showPassword ? "eye" : "eye.slash")
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    }
                }
            }
            .padding(.horizontal, 14)
            .frame(height: tf.height)
            .background(AppTheme.Colors.surface)
            .overlay(
                RoundedRectangle(cornerRadius: tf.cornerRadius)
                    .stroke(AppTheme.Colors.borderFallback, lineWidth: 1)
            )
            .cornerRadius(tf.cornerRadius)
        }
    }
}

// ─────────────────────────────────────────────
// MARK: - ETCIconBox  (Onboarding feature icon)
// ─────────────────────────────────────────────
struct ETCIconBox: View {
    let systemName: String
    var color: Color = AppTheme.Colors.primaryFallback
    var bgColor: Color = AppTheme.Colors.primaryLightFallback

    @Environment(\.appSizeClass) var sc

    var body: some View {
        let size = AppSizing.onboardingIconBox(sc)

        ZStack {
            RoundedRectangle(cornerRadius: size * 0.28)
                .fill(color)
                .frame(width: size * 0.75, height: size * 0.75)

            Image(systemName: systemName)
                .resizable()
                .scaledToFit()
                .foregroundColor(.white)
                .frame(width: size * 0.38, height: size * 0.38)
        }
        .frame(width: size, height: size)
        .background(
            RoundedRectangle(cornerRadius: size * 0.30)
                .fill(bgColor)
        )
    }
}

struct ETCSocialButton: View {
    let title: String
    let icon: String
    var isSystemImage = true
    var iconSize: CGFloat = 20   // ← new, lets callers bump up icons that have built-in padding
    let action: () -> Void

    @Environment(\.appSizeClass) var sc

    var body: some View {
        let btn = AppSizing.Button.forClass(sc)

        Button(action: action) {
            HStack(spacing: 10) {
                if isSystemImage {
                    Image(systemName: icon)
                        .resizable()
                        .scaledToFit()
                        .frame(width: iconSize, height: iconSize)
                } else {
                    Image(icon)
                        .resizable()
                        .scaledToFit()
                        .frame(width: iconSize, height: iconSize)
                }
                Text(title)
                    .font(AppTheme.Typography.headline(sc))
                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
            }
            .frame(maxWidth: .infinity)
            .frame(height: btn.height)
            .background(AppTheme.Colors.surface)
            .overlay(
                RoundedRectangle(cornerRadius: btn.cornerRadius)
                    .stroke(AppTheme.Colors.borderFallback, lineWidth: 1.5)
            )
            .cornerRadius(btn.cornerRadius)
        }
    }
}
// ─────────────────────────────────────────────
// MARK: - ETCDividerRow  ("or continue with")
// ─────────────────────────────────────────────
struct ETCDividerRow: View {
    let label: String
    @Environment(\.appSizeClass) var sc

    var body: some View {
        HStack(spacing: 12) {
            Rectangle().fill(AppTheme.Colors.borderFallback).frame(height: 1)
            Text(label)
                .font(AppTheme.Typography.subheadline(sc))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
            Rectangle().fill(AppTheme.Colors.borderFallback).frame(height: 1)
        }
    }
}
// ─────────────────────────────────────────────
// MARK: - ETCNavIconButton  (top-bar icon)
// ─────────────────────────────────────────────
struct ETCNavIconButton: View {
    let systemName: String
    var badgeCount: Int = 0
    var tint: Color = .white
    var bgColor: Color = Color.white.opacity(0.18)
    let action: () -> Void

    @Environment(\.appSizeClass) var sc

    var body: some View {
        let sz = AppSizing.iconSize(base: 40, sizeClass: sc)
        Button(action: action) {
            ZStack(alignment: .topTrailing) {
                Image(systemName: systemName)
                    .font(.system(size: AppSizing.iconSize(base: 18, sizeClass: sc)))
                    .foregroundColor(tint)
                    .frame(width: sz, height: sz)
                    .background(bgColor)
                    .cornerRadius(AppSizing.iconSize(base: 10, sizeClass: sc))

                if badgeCount > 0 {
                    Circle()
                        .fill(Color.red)
                        .frame(width: 10, height: 10)
                        .offset(x: 2, y: -2)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// MARK: - ETCPageDots  (Onboarding indicator)
// ─────────────────────────────────────────────
struct ETCPageDots: View {
    let total: Int
    let current: Int
    var activeColor: Color = AppTheme.Colors.primaryFallback
    var inactiveColor: Color = AppTheme.Colors.borderFallback

    var body: some View {
        HStack(spacing: 6) {
            ForEach(0..<total, id: \.self) { i in
                Capsule()
                    .fill(i == current ? activeColor : inactiveColor)
                    .frame(width: i == current ? 24 : 8, height: 8)
                    .animation(.spring(response: 0.3), value: current)
            }
        }
    }
}

// ─────────────────────────────────────────────
// MARK: - ETCSettingsRow
// ─────────────────────────────────────────────
struct ETCSettingsRow: View {
    let icon: String
    let title: String
    var tint: Color = AppTheme.Colors.textPrimaryFallback
    var showChevron: Bool = true
    let action: () -> Void

    @Environment(\.appSizeClass) var sc

    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                Image(systemName: icon)
                    .foregroundColor(tint)
                    .frame(width: 22)
                Text(title)
                    .font(AppTheme.Typography.body(sc))
                    .foregroundColor(tint)
                Spacer()
                if showChevron {
                    Image(systemName: "chevron.right")
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        .font(.caption)
                }
            }
            .padding(.vertical, 14)
            .padding(.horizontal, 16)
        }
    }
}
