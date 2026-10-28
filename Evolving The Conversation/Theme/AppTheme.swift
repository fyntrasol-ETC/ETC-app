import SwiftUI

struct AppTheme {

    struct Colors {
        // Helper: builds a Color that automatically swaps based on
        // the current userInterfaceStyle (light/dark). No manual
        // re-fetching needed anywhere that uses these — SwiftUI asks
        // for the right value every time it redraws.
        private static func dynamic(light: (r: Double, g: Double, b: Double),
                                     dark: (r: Double, g: Double, b: Double)) -> Color {
            Color(UIColor { trait in
                let c = trait.userInterfaceStyle == .dark ? dark : light
                return UIColor(red: c.r, green: c.g, blue: c.b, alpha: 1)
            })
        }

        // MARK: - Primary / Accent (brand colors — same in both modes)
        static let primaryFallback       = Color(red: 0.169, green: 0.357, blue: 0.910)
        static let primaryDarkFallback   = Color(red: 0.102, green: 0.247, blue: 0.749)
        static let greenFallback         = Color(red: 0.176, green: 0.741, blue: 0.494)
        static let orangeFallback        = Color(red: 0.961, green: 0.651, blue: 0.137)
        static let purpleFallback        = Color(red: 0.486, green: 0.235, blue: 0.929)
        static let red                   = Color(red: 0.898, green: 0.224, blue: 0.208)

        // Bright red used for the live-class date badge — accent color,
        // stays the same in both modes; only its pale background flips.
        static let liveClassRed = Color(red: 0.878, green: 0.145, blue: 0.145)
        static let liveClassRedBg = dynamic(
            light: (0.996, 0.894, 0.894),
            dark:  (0.30, 0.12, 0.12)
        )
        static let segmentedBg = dynamic(
            light: (0.933, 0.941, 0.961),
            dark:  (0.16, 0.17, 0.20)
        )
        // MARK: - Surfaces & backgrounds (these flip with dark mode)
        static let backgroundFallback = dynamic(
            light: (0.96, 0.96, 0.96),
            dark:  (0.07, 0.07, 0.09)
        )
        static let surface = dynamic(               // replaces raw Color.white on cards/bars
            light: (1.0, 1.0, 1.0),
            dark:  (0.11, 0.11, 0.14)
        )
        static let surfaceElevated = dynamic(        // popups/sheets sitting above surface
            light: (1.0, 1.0, 1.0),
            dark:  (0.15, 0.15, 0.19)
        )
        static let primaryLightFallback = dynamic(   // soft brand-tinted chip/pill bg
            light: (0.933, 0.945, 0.992),
            dark:  (0.122, 0.157, 0.294)
        )

        // MARK: - Text
        static let textPrimaryFallback = dynamic(
            light: (0.059, 0.090, 0.165),
            dark:  (0.94, 0.94, 0.96)
        )
        static let textSecondaryFallback = dynamic(
            light: (0.392, 0.455, 0.545),
            dark:  (0.62, 0.64, 0.68)
        )
        static let textOnPrimary = Color.white

        // MARK: - Border
        static let borderFallback = dynamic(
            light: (0.886, 0.910, 0.941),
            dark:  (0.24, 0.25, 0.29)
        )
        
    }

    // MARK: - Typography (unchanged)
    struct Typography {
        static func largeTitle(_ sc: AppSizing.SizeClass) -> Font { .system(size: AppSizing.fontSize(base: 32, sizeClass: sc), weight: .bold) }
        static func title1(_ sc: AppSizing.SizeClass) -> Font { .system(size: AppSizing.fontSize(base: 26, sizeClass: sc), weight: .bold) }
        static func title2(_ sc: AppSizing.SizeClass) -> Font { .system(size: AppSizing.fontSize(base: 22, sizeClass: sc), weight: .bold) }
        static func headline(_ sc: AppSizing.SizeClass) -> Font { .system(size: AppSizing.fontSize(base: 17, sizeClass: sc), weight: .semibold) }
        static func body(_ sc: AppSizing.SizeClass) -> Font { .system(size: AppSizing.fontSize(base: 15, sizeClass: sc), weight: .regular) }
        static func subheadline(_ sc: AppSizing.SizeClass) -> Font { .system(size: AppSizing.fontSize(base: 14, sizeClass: sc), weight: .regular) }
        static func caption(_ sc: AppSizing.SizeClass) -> Font { .system(size: AppSizing.fontSize(base: 12, sizeClass: sc), weight: .regular) }
    }

    struct Radius {
        static let small: CGFloat = 8, medium: CGFloat = 12, large: CGFloat = 16, xlarge: CGFloat = 24, pill: CGFloat = 100
    }

    struct Shadow {
        static let card = (color: Color.black.opacity(0.06), radius: CGFloat(12), x: CGFloat(0), y: CGFloat(4))
        static let button = (color: Color.black.opacity(0.12), radius: CGFloat(8), x: CGFloat(0), y: CGFloat(3))
    }
}

extension View {
    func appCardStyle(_ sc: AppSizing.SizeClass) -> some View {
        self
            .background(AppTheme.Colors.surface)
            .cornerRadius(AppSizing.cardRadius(sc))
            .overlay(RoundedRectangle(cornerRadius: AppSizing.cardRadius(sc))
                .stroke(Color.black.opacity(0.06), lineWidth: 1))
            .shadow(color: AppTheme.Shadow.card.color, radius: AppTheme.Shadow.card.radius,
                    x: AppTheme.Shadow.card.x, y: AppTheme.Shadow.card.y)
    }
}
