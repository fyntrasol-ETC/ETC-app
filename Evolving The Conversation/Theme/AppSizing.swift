import SwiftUI

// MARK: - App Sizing Manager
/// Handles responsive sizing across all iPhone and iPad models.
struct AppSizing {

    // MARK: - Device Size Class
    enum SizeClass {
        case compact   // iPhone portrait
        case regular   // iPad 9" / 10" / 11"
        case large     // iPad Pro 12.9" / 13"

        static func current(horizontalSizeClass: UserInterfaceSizeClass?,
                            verticalSizeClass: UserInterfaceSizeClass?) -> SizeClass {
            let screenWidth = UIScreen.main.bounds.width
            if horizontalSizeClass == .regular {
                return screenWidth >= 1024 ? .large : .regular
            }
            return .compact
        }
    }
    struct Radius {
        static let pill: CGFloat = 999
    }
    // MARK: - Font Scale
    // iPad needs MUCH larger text — using screen-width-relative scaling
    static func fontSize(base: CGFloat, sizeClass: SizeClass) -> CGFloat {
        switch sizeClass {
        case .compact:  return base
        case .regular:  return base * 1.55   // was 1.18 — too small
        case .large:    return base * 1.80   // was 1.30 — too small
        }
    }

    // MARK: - Icon Size
    // For touch targets / circular elements (buttons, avatars, badges)
    static func iconSize(base: CGFloat, sizeClass: SizeClass) -> CGFloat {
        switch sizeClass {
        case .compact:  return base
        case .regular:  return base * 1.60
        case .large:    return base * 1.90
        }
    }

    // MARK: - Media / Banner Height
    // For full-bleed blocks (hero banners, video players, thumbnails) — these are
    // already big, so they grow more gently than fonts/icons.
    static func mediaHeight(base: CGFloat, sizeClass: SizeClass) -> CGFloat {
        switch sizeClass {
        case .compact:  return base
        case .regular:  return base * 1.35
        case .large:    return base * 1.55
        }
    }

    // MARK: - Generic Scalar Scale
    // For one-off literals (insets, corner radii, small gaps) that aren't covered
    // by a more specific helper above. Milder curve than fontSize/iconSize.
    static func scaled(_ base: CGFloat, _ s: SizeClass) -> CGFloat {
        switch s {
        case .compact:  return base
        case .regular:  return base * 1.40
        case .large:    return base * 1.60
        }
    }

    // MARK: - Grid Columns
    // Card grids (e.g. Courses) — iPad gets more columns instead of 2 stretched cards.
    static func gridColumns(_ s: SizeClass) -> Int {
        switch s {
        case .compact:  return 2
        case .regular:  return 3
        case .large:    return 4
        }
    }

    // MARK: - Padding / Spacing
    struct Padding {
        let xs:  CGFloat
        let sm:  CGFloat
        let md:  CGFloat
        let lg:  CGFloat
        let xl:  CGFloat
        let xxl: CGFloat

        static func forClass(_ s: SizeClass) -> Padding {
            switch s {
            case .compact:
                return Padding(xs: 4,  sm: 8,  md: 16, lg: 24, xl: 32, xxl: 48)
            case .regular:
                return Padding(xs: 10, sm: 20, md: 32, lg: 48, xl: 64, xxl: 96)
            case .large:
                return Padding(xs: 12, sm: 24, md: 40, lg: 60, xl: 80, xxl: 120)
            }
        }
    }

    // MARK: - Button
    struct Button {
        let height:        CGFloat
        let cornerRadius:  CGFloat

        static func forClass(_ s: SizeClass) -> Button {
            switch s {
            case .compact:  return Button(height: 54,  cornerRadius: 14)
            case .regular:  return Button(height: 72,  cornerRadius: 18)
            case .large:    return Button(height: 84,  cornerRadius: 22)
            }
        }
    }

    // MARK: - Text Field
    struct TextField {
        let height:       CGFloat
        let cornerRadius: CGFloat

        static func forClass(_ s: SizeClass) -> TextField {
            switch s {
            case .compact:  return TextField(height: 52, cornerRadius: 12)
            case .regular:  return TextField(height: 70, cornerRadius: 16)
            case .large:    return TextField(height: 80, cornerRadius: 18)
            }
        }
    }

    // MARK: - Onboarding Icon Box
    static func onboardingIconBox(_ s: SizeClass) -> CGFloat {
        switch s {
        case .compact:  return 100
        case .regular:  return 180   // was 130
        case .large:    return 220   // was 160
        }
    }

    // MARK: - Card Radius
    static func cardRadius(_ s: SizeClass) -> CGFloat {
        switch s {
        case .compact:  return 16
        case .regular:  return 24
        case .large:    return 30
        }
    }

    // MARK: - Max content width (for iPad centering)
    // Key fix: iPad should use a much wider column, not be squished to 480
    static func maxContentWidth(_ s: SizeClass) -> CGFloat {
        switch s {
        case .compact:  return .infinity
        case .regular:  return 680    // was 480 — too narrow
        case .large:    return 820    // was 540 — too narrow
        }
    }

    // MARK: - App icon size (login screen icon)
    static func appIconSize(_ s: SizeClass) -> CGFloat {
        switch s {
        case .compact:  return 60
        case .regular:  return 100
        case .large:    return 120
        }
    }
}

// MARK: - Environment Key
private struct SizeClassKey: EnvironmentKey {
    static let defaultValue: AppSizing.SizeClass = .compact
}

extension EnvironmentValues {
    var appSizeClass: AppSizing.SizeClass {
        get { self[SizeClassKey.self] }
        set { self[SizeClassKey.self] = newValue }
    }
}

// MARK: - View Modifier: Inject SizeClass
struct AppSizeClassModifier: ViewModifier {
    @Environment(\.horizontalSizeClass) var hSize
    @Environment(\.verticalSizeClass)   var vSize

    func body(content: Content) -> some View {
        let sc = AppSizing.SizeClass.current(horizontalSizeClass: hSize, verticalSizeClass: vSize)
        content.environment(\.appSizeClass, sc)
    }
}

extension View {
    func appSizeClass() -> some View {
        modifier(AppSizeClassModifier())
    }
}
