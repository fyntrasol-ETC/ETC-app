import SwiftUI
import AVKit

// MARK: - Video Lesson Screen

struct VideoLessonScreen: View {
    let course: Course
    let lesson: CourseLesson
    let onBack: () -> Void

    @Environment(\.appSizeClass) var sc
    @StateObject private var progressService = LessonProgressService.shared

    @State private var player: AVPlayer? = nil
    @State private var isPlaying = false
    @State private var progress: Double = 0.0
    @State private var durationSeconds: Double = 1
    @State private var timeObserverToken: Any?

    @State private var isCompleted = false
    @State private var isMarkingComplete = false

    private var currentTime: String {
        let current = Int(progress * durationSeconds)
        return String(format: "%d:%02d", current / 60, current % 60)
    }
    private var totalTime: String {
        let total = Int(durationSeconds)
        return String(format: "%d:%02d", total / 60, total % 60)
    }

    private func resolveVideoURL(_ videoURL: String) -> URL? {
        if videoURL.hasPrefix("http") {
            return URL(string: videoURL)
        } else {
            return Bundle.main.url(forResource: videoURL, withExtension: "mp4")
        }
    }

    private func setupPlayer() {
        guard let url = resolveVideoURL(lesson.videoURL) else {
            print("⚠️ Could not find video for lesson.videoURL = \(lesson.videoURL). Check the filename matches a file added to the Xcode target.")
            return
        }
        let item = AVPlayerItem(url: url)
        let newPlayer = AVPlayer(playerItem: item)
        player = newPlayer

        Task {
            if let duration = try? await item.asset.load(.duration) {
                let seconds = CMTimeGetSeconds(duration)
                if seconds.isFinite && seconds > 0 {
                    durationSeconds = seconds
                }
            }
        }

        let interval = CMTime(seconds: 0.25, preferredTimescale: 600)
        timeObserverToken = newPlayer.addPeriodicTimeObserver(forInterval: interval, queue: .main) { time in
            guard durationSeconds > 0 else { return }
            progress = CMTimeGetSeconds(time) / durationSeconds
        }
    }

    private func teardownPlayer() {
        if let token = timeObserverToken {
            player?.removeTimeObserver(token)
            timeObserverToken = nil
        }
        player?.pause()
        player = nil
    }

    private func seedCompletionState() {
        if progressService.enrollments[course.id] == nil {
            progressService.listenAll()
        }
        isCompleted = progressService.enrollments[course.id]?.completedLessonIds.contains(lesson.id) ?? false
    }

    var body: some View {
        let pad           = AppSizing.Padding.forClass(sc)
        let videoHeight   = AppSizing.mediaHeight(base: 220, sizeClass: sc)
        let navBtnSize    = AppSizing.iconSize(base: 36, sizeClass: sc)
        let navBtnRadius  = AppSizing.scaled(10, sc)
        let playBtnSize   = AppSizing.iconSize(base: 56, sizeClass: sc)

        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {

                // ── Video Player — intentionally stays dark in both
                // modes, standard convention for video UIs.
                ZStack {
                    if let player {
                        VideoPlayer(player: player)
                            .disabled(true)
                            .ignoresSafeArea(edges: .top)
                    } else {
                        LinearGradient(
                            colors: [Color(red: 0.08, green: 0.10, blue: 0.22),
                                     Color(red: 0.12, green: 0.16, blue: 0.35)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                        .ignoresSafeArea(edges: .top)
                    }

                    Button {
                        withAnimation(.spring()) {
                            isPlaying.toggle()
                            isPlaying ? player?.play() : player?.pause()
                        }
                    } label: {
                        Circle()
                            .fill(Color.white.opacity(0.18))
                            .frame(width: playBtnSize, height: playBtnSize)
                            .overlay(
                                Image(systemName: isPlaying ? "pause.fill" : "play.fill")
                                    .font(.system(size: AppSizing.fontSize(base: 22, sizeClass: sc)))
                                    .foregroundColor(.white)
                                    .offset(x: isPlaying ? 0 : AppSizing.scaled(2, sc))
                            )
                    }

                    VStack {
                        HStack {
                            Button(action: onBack) {
                                ZStack {
                                    RoundedRectangle(cornerRadius: navBtnRadius)
                                        .fill(Color.white.opacity(0.18))
                                        .frame(width: navBtnSize, height: navBtnSize)
                                    Image(systemName: "chevron.left")
                                        .font(.system(size: AppSizing.fontSize(base: 14, sizeClass: sc), weight: .semibold))
                                        .foregroundColor(.white)
                                }
                            }
                            Spacer()
                        }
                        .padding(.horizontal, pad.md)
                        .padding(.top, pad.md)
                        Spacer()
                    }
                    .frame(height: videoHeight)

                    VStack(spacing: 0) {
                        Spacer()

                        VideoProgressBar(progress: $progress) { newValue in
                            guard durationSeconds > 0 else { return }
                            let target = CMTime(seconds: newValue * durationSeconds, preferredTimescale: 600)
                            player?.seek(to: target)
                        }
                        .padding(.horizontal, pad.md)

                        HStack {
                            Text("\(currentTime) / \(totalTime)")
                                .font(.system(size: AppSizing.fontSize(base: 11, sizeClass: sc), weight: .medium))
                                .foregroundColor(.white.opacity(0.80))
                                .monospacedDigit()

                            Spacer()

                            Text("1.0×")
                                .font(.system(size: AppSizing.fontSize(base: 11, sizeClass: sc), weight: .semibold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.white.opacity(0.18))
                                .cornerRadius(6)
                        }
                        .padding(.horizontal, pad.md)
                        .padding(.top, AppSizing.scaled(6, sc))
                        .padding(.bottom, AppSizing.scaled(10, sc))
                    }
                    .frame(height: videoHeight)
                }
                .frame(height: videoHeight)

                // ── Lesson Title ─────────────────────────────────────
                VStack(alignment: .center, spacing: AppSizing.scaled(2, sc)) {
                    Text(lesson.title)
                        .font(AppTheme.Typography.title2(sc))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                        .multilineTextAlignment(.center)

                    Text("Lesson \(lesson.order + 1) of \(course.lessons.count) · \(course.title)")
                        .font(.system(size: AppSizing.fontSize(base: 12, sizeClass: sc)))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                }
                .frame(maxWidth: .infinity)
                .padding(.horizontal, pad.md)
                .padding(.vertical, AppSizing.scaled(12, sc))
                .background(AppTheme.Colors.surface)     // was: Color.white

                // ── Transcript ─────────────────────────────────────────
                ScrollView {
                    TranscriptTab(sc: sc, pad: pad)
                        .frame(maxWidth: AppSizing.maxContentWidth(sc))
                        .frame(maxWidth: .infinity)
                        .padding(.bottom, 110)
                }
                .background(AppTheme.Colors.backgroundFallback)
            }

            // ── Bottom Bar (Mark Complete only) ──────────────────────
            Button {
                guard !isCompleted, !isMarkingComplete else { return }
                isMarkingComplete = true
                let sorted = course.lessons.sorted { $0.order < $1.order }
                let next = sorted.first { $0.order == lesson.order + 1 }
                Task {
                    await progressService.markLessonComplete(
                        courseId: course.id,
                        lessonId: lesson.id,
                        nextLessonId: next?.id
                    )
                    XPService.shared.recordActivity(.lesson)
                    DailyChallengeService.shared.markComplete(.watchLesson)

                    await MainActor.run {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            isCompleted = true
                            isMarkingComplete = false
                        }
                    }
                }
            } label: {
                HStack(spacing: AppSizing.scaled(6, sc)) {
                    Image(systemName: isCompleted ? "checkmark.circle.fill" : "checkmark")
                        .font(.system(size: AppSizing.fontSize(base: 13, sizeClass: sc), weight: .bold))
                    Text(isCompleted ? "Completed" : "Mark complete")
                        .font(.system(size: AppSizing.fontSize(base: 14, sizeClass: sc), weight: .bold))
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: AppSizing.Button.forClass(sc).height)
                .background(
                    isCompleted
                        ? Color(red: 0.55, green: 0.58, blue: 0.60)
                        : Color(red: 0.18, green: 0.72, blue: 0.45)
                )
                .cornerRadius(AppSizing.Button.forClass(sc).cornerRadius)
            }
            .disabled(isCompleted || isMarkingComplete)
            .padding(.horizontal, pad.md)
            .padding(.vertical, 12)
            .frame(maxWidth: AppSizing.maxContentWidth(sc))
            .frame(maxWidth: .infinity)
            .background(AppTheme.Colors.surface.shadow(color: .black.opacity(0.08), radius: 16, x: 0, y: -4))   // was: Color.white
        }
        .ignoresSafeArea(edges: .bottom)
        .onAppear {
            setupPlayer()
            seedCompletionState()
        }
        .onDisappear { teardownPlayer() }
    }
}

// MARK: - Video Progress Bar (lives inside the video player — stays as-is)

private struct VideoProgressBar: View {
    @Binding var progress: Double
    var onSeek: (Double) -> Void = { _ in }

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule()
                    .fill(Color.white.opacity(0.25))
                    .frame(height: 3)
                Capsule()
                    .fill(AppTheme.Colors.primaryFallback)
                    .frame(width: geo.size.width * progress, height: 3)
                Circle()
                    .fill(Color.white)
                    .frame(width: 12, height: 12)
                    .offset(x: geo.size.width * progress - 6)
            }
        }
        .frame(height: 12)
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { val in
                    let w = UIScreen.main.bounds.width - 32
                    let newValue = min(max(val.location.x / w, 0), 1)
                    progress = newValue
                }
                .onEnded { val in
                    let w = UIScreen.main.bounds.width - 32
                    let newValue = min(max(val.location.x / w, 0), 1)
                    onSeek(newValue)
                }
        )
    }
}

// MARK: - Transcript Tab

private struct TranscriptTab: View {
    let sc: AppSizing.SizeClass
    let pad: AppSizing.Padding

    var body: some View {
        VStack(alignment: .leading, spacing: AppSizing.scaled(12, sc)) {
            Text("Welcome back. In this lesson we focus on the rhythm of natural speech — linking words together, stressing the right syllables, and softening sounds the way fluent speakers do…")
                .font(.system(size: AppSizing.fontSize(base: 14, sizeClass: sc)))
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                .multilineTextAlignment(.center)
                .lineSpacing(AppSizing.scaled(5, sc))
                .padding(pad.md)
                .frame(maxWidth: .infinity)
                .background(AppTheme.Colors.surface)      // was: Color.white
                .cornerRadius(AppSizing.cardRadius(sc))
                .shadow(color: AppTheme.Shadow.card.color,
                        radius: AppTheme.Shadow.card.radius,
                        x: AppTheme.Shadow.card.x,
                        y: AppTheme.Shadow.card.y)
        }
        .padding(pad.md)
    }
}
