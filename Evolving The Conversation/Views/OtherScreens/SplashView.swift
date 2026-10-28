import SwiftUI

// MARK: - Splash Screen
struct SplashScreen: View {
    @Environment(\.appSizeClass) var sc

    // Ripple animation
    @State private var ripple1 = false
    @State private var ripple2 = false
    @State private var ripple3 = false

    // Bouncing dots
    @State private var dot1Up = false
    @State private var dot2Up = false
    @State private var dot3Up = false

    var body: some View {
        ZStack {
            // ── Background gradient (bright blue → dark navy/black) ──
            LinearGradient(
                colors: [
                    Color(red: 0.18, green: 0.38, blue: 0.98),
                    Color(red: 0.18, green: 0.38, blue: 0.98),   // bright blue top
                    // bright blue top
                    Color(red: 0.05, green: 0.08, blue: 0.22)    // near-black navy bottom
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                Spacer()

                // ── Ripple rounded-rect rings + content ───────────────
                ZStack {
                    // Ripple rings — rounded rectangles, NOT circles
                    RippleRoundedRect(scale: ripple1 ? 2.2 : 1, opacity: ripple1 ? 0 : 0.15)
                    RippleRoundedRect(scale: ripple2 ? 1.8 : 1, opacity: ripple2 ? 0 : 0.12)
                    RippleRoundedRect(scale: ripple3 ? 1.4 : 1, opacity: ripple3 ? 0 : 0.10)

                    // Content — NO card background, floats directly on gradient
                    VStack(spacing: AppSizing.Padding.forClass(sc).lg) {

                        // App icon — white rounded square only, no extra wrapper
                        ZStack {
                            RoundedRectangle(cornerRadius: AppSizing.cardRadius(sc))
                                .fill(Color.white)
                                .frame(
                                    width:  AppSizing.onboardingIconBox(sc) * 0.85,
                                    height: AppSizing.onboardingIconBox(sc) * 0.85
                                )
                                .shadow(color: .black.opacity(0.15), radius: 16, y: 6)

                            Image("logo")
                                .resizable()
                                .scaledToFit()
                                .foregroundColor(Color(red: 0.18, green: 0.38, blue: 0.98))
                                .frame(
                                    width:  AppSizing.onboardingIconBox(sc) * 0.50,
                                    height: AppSizing.onboardingIconBox(sc) * 0.50
                                )
                        }

                        // Title & subtitle
                        VStack(spacing: 10) {
                            Text("Evolving The\nConversation")
                                .font(AppTheme.Typography.title1(sc))
                                .foregroundColor(.white)
                                .multilineTextAlignment(.center)
                                .fontWeight(.bold)

                            Text("Learn English with Confidence")
                                .font(AppTheme.Typography.body(sc))
                                .foregroundColor(.white.opacity(0.80))
                        }

                        // ── Bouncing dots ─────────────────
                        HStack(spacing: 8) {
                            BouncingDot(isUp: dot1Up)
                            BouncingDot(isUp: dot2Up)
                            BouncingDot(isUp: dot3Up)
                        }
                    }
                    // ← NO .background, NO .cornerRadius, NO .overlay here
                    .padding(.horizontal, AppSizing.Padding.forClass(sc).lg)
                }

                Spacer()
                Spacer()
            }
        }
        .onAppear { startAnimations() }
    }

    // ── Animation triggers ────────────────────────────────────────────
    private func startAnimations() {
        withAnimation(.easeOut(duration: 2.0).repeatForever(autoreverses: false)) {
            ripple1 = true
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.65) {
            withAnimation(.easeOut(duration: 2.0).repeatForever(autoreverses: false)) {
                ripple2 = true
            }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.3) {
            withAnimation(.easeOut(duration: 2.0).repeatForever(autoreverses: false)) {
                ripple3 = true
            }
        }

        animateDot(delay: 0.0)  { dot1Up = $0 }
        animateDot(delay: 0.18) { dot2Up = $0 }
        animateDot(delay: 0.36) { dot3Up = $0 }
    }

    private func animateDot(delay: Double, setter: @escaping (Bool) -> Void) {
        func bounce() {
            DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
                withAnimation(.easeOut(duration: 0.28)) { setter(true) }
                DispatchQueue.main.asyncAfter(deadline: .now() + delay + 0.28) {
                    withAnimation(.easeIn(duration: 0.28)) { setter(false) }
                    DispatchQueue.main.asyncAfter(deadline: .now() + delay + 0.56 + 0.5) {
                        bounce()
                    }
                }
            }
        }
        bounce()
    }
}

// MARK: - Ripple Rounded Rectangle (replaces RippleRing)
private struct RippleRoundedRect: View {
    let scale: CGFloat
    let opacity: Double

    var body: some View {
        RoundedRectangle(cornerRadius: 60)          // big corner radius = soft square feel
            .stroke(Color.white, lineWidth: 1.5)
            .frame(width: 260, height: 260)
            .scaleEffect(scale)
            .opacity(opacity)
    }
}

// MARK: - Bouncing Dot
private struct BouncingDot: View {
    let isUp: Bool

    var body: some View {
        Circle()
            .fill(Color.white)
            .frame(width: 8, height: 8)
            .offset(y: isUp ? -6 : 0)
    }
}
