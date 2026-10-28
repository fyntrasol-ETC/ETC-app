//
//  OnboardingFlow.swift
//  Evolving The Conversation
//
//  Created by Zainab's Mac on 15/06/2026.
//

import SwiftUI


// ─────────────────────────────────────────────────────────────
// MARK: - Onboarding Flow (Splash → 3 feature slides → Login)
// ─────────────────────────────────────────────────────────────
 
struct OnboardingFlow: View {
    var onFinished: () -> Void   // called when user taps "Get Started"
    var onSkip: () -> Void       // called when user taps "Skip" — goes straight to Login
 
    /// 0 = Splash, 1-3 = feature slides
    @State private var page: Int = 1
 
    var body: some View {
        ZStack {
            switch page {
            case 0:
                SplashScreen()
                    .transition(.opacity)
                    .onAppear {
                        // Auto-advance after 2 seconds
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                            withAnimation(.easeInOut(duration: 0.5)) { page = 1 }
                        }
                    }
 
            case 1, 2, 3:
                OnboardingPagerView(
                    currentPage: $page,
                    onSkip: onSkip,
                    onFinish: onFinished
                )
                .transition(.opacity)
 
            default:
                EmptyView()
            }
        }
        .animation(.easeInOut(duration: 0.4), value: page)
    }
}
 
// ─────────────────────────────────────────────────────────────
// MARK: - Onboarding Pager  (slides 1-3)
// ─────────────────────────────────────────────────────────────
 
private struct OnboardingPage {
    let icon: String
    let iconColor: Color
    let iconBg: Color
    let title: String
    let subtitle: String
}
 
private let onboardingPages: [OnboardingPage] = [
    OnboardingPage(
        icon: "book.fill",
        iconColor: AppTheme.Colors.primaryFallback,
        iconBg: AppTheme.Colors.primaryLightFallback,
        title: "Learn English Every Day",
        subtitle: "Improve your communication skills through structured, bite-sized lessons that fit your routine."
    ),
    OnboardingPage(
        icon: "mic.fill",
        iconColor: AppTheme.Colors.greenFallback,
        iconBg: AppTheme.Colors.greenFallback.opacity(0.14),
        title: "Practice with Confidence",
        subtitle: "Enhance speaking, listening, grammar, and vocabulary with an AI tutor that listens and guides."
    ),
    OnboardingPage(
        icon: "chart.line.uptrend.xyaxis",
        iconColor: Color(red: 0.961, green: 0.651, blue: 0.137),   // orange
        iconBg: Color(red: 0.961, green: 0.651, blue: 0.137).opacity(0.14),
        title: "Track Your Progress",
        subtitle: "Monitor achievements, keep your streak alive, and stay motivated every step of the way."
    )
]
 
struct OnboardingPagerView: View {
    @Binding var currentPage: Int   // 1-based index into onboardingPages
    var onSkip: () -> Void
    var onFinish: () -> Void
 
    @Environment(\.appSizeClass) var sc
 
    /// Map currentPage (1-3) to array index (0-2)
    private var pageIndex: Int { currentPage - 1 }
    private var isLast: Bool { currentPage == onboardingPages.count }
    private let pad: AppSizing.Padding = AppSizing.Padding.forClass(.compact)
 
    var body: some View {
        ZStack(alignment: .bottom) {
            AppTheme.Colors.backgroundFallback.ignoresSafeArea()
 
            // Slide content
            TabView(selection: $currentPage) {
                ForEach(1...onboardingPages.count, id: \.self) { index in
                    OnboardingSlide(page: onboardingPages[index - 1])
                        .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .animation(.easeInOut, value: currentPage)
            // Leave room for the bottom bar
            .padding(.bottom, 120)
 
            // Bottom bar: dots + button
            VStack(spacing: 20) {
                ETCPageDots(
                    total: onboardingPages.count,
                    current: pageIndex,
                    activeColor: AppTheme.Colors.primaryFallback,
                    inactiveColor: AppTheme.Colors.borderFallback
                )
 
                ETCButton(
                    title: isLast ? "Get Started" : "Next",
                    icon: isLast ? "arrow.right" : nil
                ) {
                    if isLast {
                        onFinish()
                    } else {
                        withAnimation { currentPage += 1 }
                    }
                }
                .padding(.horizontal, AppSizing.Padding.forClass(sc).md)
            }
            .padding(.bottom, AppSizing.Padding.forClass(sc).xl)
            .background(
                // Subtle fade so content doesn't clash with the button
                LinearGradient(
                    colors: [AppTheme.Colors.backgroundFallback.opacity(0), AppTheme.Colors.backgroundFallback],
                    startPoint: .top, endPoint: .bottom
                )
                .frame(height: 160)
            )
            VStack {
                 HStack {
                     Spacer()
                     Button("Skip", action: onSkip)
                         .font(AppTheme.Typography.subheadline(sc))
                         .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                         .padding(.top, 16)
                         .padding(.trailing, 24)
                 }
                 Spacer()
             }
         }
        }
    }

 
// ─────────────────────────────────────────────────────────────
// MARK: - Single Onboarding Slide
// ─────────────────────────────────────────────────────────────
 
private struct OnboardingSlide: View {
    let page: OnboardingPage
    @Environment(\.appSizeClass) var sc
 
    var body: some View {
        VStack(spacing: 0) {
            Spacer()
 
            // Feature icon
            ETCIconBox(
                systemName: page.icon,
                color: page.iconColor,
                bgColor: page.iconBg
            )
            .padding(.bottom, AppSizing.Padding.forClass(sc).xl)
 
            // Title
            Text(page.title)
                .font(.system(size: AppSizing.fontSize(base: 28, sizeClass: sc), weight: .bold))
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                .multilineTextAlignment(.center)
                .padding(.horizontal, AppSizing.Padding.forClass(sc).lg)
                .padding(.bottom, AppSizing.Padding.forClass(sc).sm)
 
            // Subtitle
            Text(page.subtitle)
                .font(AppTheme.Typography.body(sc))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                .multilineTextAlignment(.center)
                .lineSpacing(4)
                .padding(.horizontal, AppSizing.Padding.forClass(sc).xl)
 
            Spacer()
            Spacer()
        }
        .frame(maxWidth: AppSizing.maxContentWidth(sc), maxHeight: .infinity)
        .frame(maxWidth: .infinity)
    }
}
