import SwiftUI


// ─────────────────────────────────────────────────────────────
// MARK: - Listening Practice Screen
// ─────────────────────────────────────────────────────────────

struct ListeningPracticeScreen: View {
    var onBack: (() -> Void)? = nil

    @Environment(\.appSizeClass) var sc
    @ObservedObject private var audio = AudioManager.shared

    @State private var selectedPodcast: PodcastItem? = nil

    // Accordion state for the level sections (Beginner / Intermediate / Advanced).
    // Only one level is expanded at a time — opening one collapses any other.
    @State private var expandedLevel: LessonLevel? = nil

    // Whether the Dialogues grid is showing all of ListeningData.dialogues or just the first 7.
    @State private var showAllDialogues = false

    // ── Dictation exercise state ──
    // Lives directly on this screen now (no separate sheet/view). The
    // dictation card below just switches between three states based on
    // these flags: level picker+Start, an in-progress sentence, or the
    // finished summary.
    @State private var dictationLevel: LessonLevel = .beginner
    @State private var dictationStarted = false
    @State private var dictationFinished = false
    @State private var dictationIndex = 0
    @State private var dictationTypedAnswer = ""
    @State private var dictationShowCheck = false
    @State private var dictationCurrentResult: DictationResult?
    @State private var dictationResults: [DictationResult] = []

    // Lets us scroll back up to the Now Playing card whenever a dialogue,
    // lesson, or dictation sentence is played, even if the user has
    // scrolled further down the page.
    @State private var scrollProxy: ScrollViewProxy? = nil
    private let nowPlayingCardID = "nowPlayingCard"

    var body: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                ETCScreenBackBar(title: "Listening Practice") {
                    onBack?()
                }

                ScrollViewReader { proxy in
                    ScrollView(showsIndicators: false) {
                        VStack(spacing: AppSizing.Padding.forClass(sc).md) {

                            // Now Playing card
                            nowPlayingCard
                                .id(nowPlayingCardID)

                            // Audio lessons section (heading + Beginner/Intermediate/Advanced accordion)
                            audioLessonsSection

                            // Podcasts section
                            podcastsSection

                            // Dialogues section
                            dialoguesSection

                            // Dictation exercise (inline, lives on this same screen)
                            dictationSection

                        }
                        .padding(.horizontal, AppSizing.Padding.forClass(sc).lg)
                        .padding(.top, AppSizing.Padding.forClass(sc).md)
                        .padding(.bottom, 100)
                        .frame(maxWidth: AppSizing.maxContentWidth(sc))
                        .frame(maxWidth: .infinity)
                    }
                    .onAppear { scrollProxy = proxy }
                }
            }
            .background(AppTheme.Colors.backgroundFallback.ignoresSafeArea())
        }
        .sheet(item: $selectedPodcast) { podcast in
            PodcastEpisodesSheet(podcast: podcast)
        }
    }

    // MARK: - Now Playing Card
    private var nowPlayingCard: some View {
        let pad = AppSizing.Padding.forClass(sc)
        let playerSize = AppSizing.iconSize(base: 52, sizeClass: sc)
        let info = audio.nowPlaying

        return VStack(spacing: pad.sm) {
            HStack(spacing: pad.md) {
                // Play/Pause button
                Button {
                    audio.togglePlayPause()
                } label: {
                    ZStack {
                        Circle()
                            .fill(AppTheme.Colors.primaryFallback)
                            .frame(width: playerSize, height: playerSize)
                        Image(systemName: audio.isPlaying ? "pause.fill" : "play.fill")
                            .font(.system(size: AppSizing.fontSize(base: 20, sizeClass: sc), weight: .medium))
                            .foregroundColor(.white)
                    }
                }
                .buttonStyle(.plain)
                .disabled(info == nil)
                .opacity(info == nil ? 0.5 : 1)

                // Track info
                VStack(alignment: .leading, spacing: 4) {
                    Text("NOW PLAYING")
                        .font(.system(size: AppSizing.fontSize(base: 10, sizeClass: sc), weight: .semibold))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        .tracking(1.2)

                    Text(info?.title ?? "Nothing playing yet")
                        .font(.system(size: AppSizing.fontSize(base: 17, sizeClass: sc), weight: .bold))
                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                        .lineLimit(1)

                    Text(info != nil ? "\(info!.category) · \(info!.duration)" : "Choose a lesson, podcast, or dialogue")
                        .font(AppTheme.Typography.subheadline(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        .lineLimit(1)
                }

                Spacer()

            }

            // Progress bar (drag to seek)
            VStack(spacing: 6) {
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Capsule()
                            .fill(AppTheme.Colors.borderFallback)
                            .frame(height: 4)
                        Capsule()
                            .fill(AppTheme.Colors.primaryFallback)
                            .frame(width: geo.size.width * audio.progress, height: 4)
                    }
                    .contentShape(Rectangle())
                    .gesture(
                        DragGesture(minimumDistance: 0)
                            .onChanged { value in
                                guard info != nil, geo.size.width > 0 else { return }
                                let pct = min(max(0, value.location.x / geo.size.width), 1)
                                audio.seek(to: pct)
                            }
                    )
                }
                .frame(height: 4)

                HStack {
                    Text(AudioManager.formattedTime(audio.currentTime))
                        .font(AppTheme.Typography.caption(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    Spacer()
                    Text(AudioManager.formattedTime(audio.duration))
                        .font(AppTheme.Typography.caption(sc))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                }
            }
        }
        .padding(pad.md)
        .background(AppTheme.Colors.surface)
        .cornerRadius(AppSizing.cardRadius(sc))
        .shadow(
            color: AppTheme.Shadow.card.color,
            radius: AppTheme.Shadow.card.radius,
            x: AppTheme.Shadow.card.x,
            y: AppTheme.Shadow.card.y
        )
    }

    // MARK: - Audio Lessons Section (Beginner / Intermediate / Advanced accordion)
    private var audioLessonsSection: some View {
        let pad = AppSizing.Padding.forClass(sc)
        return VStack(alignment: .leading, spacing: pad.sm) {
            Text("Audio lessons")
                .font(.system(size: AppSizing.fontSize(base: 17, sizeClass: sc), weight: .bold))
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)

            VStack(spacing: pad.sm) {
                ForEach(LessonLevel.allCases) { level in
                    levelCard(level)
                }
            }
        }
    }

    private func lessons(for level: LessonLevel) -> [AudioLessonItem] {
        ListeningData.audioLessons.filter { $0.level == level }
    }

    private func levelCard(_ level: LessonLevel) -> some View {
        let pad = AppSizing.Padding.forClass(sc)
        let isExpanded = expandedLevel == level
        let levelLessons = lessons(for: level)

        return VStack(spacing: 0) {
            // Header row — tap to expand/collapse
            Button {
                withAnimation(.easeInOut(duration: 0.25)) {
                    expandedLevel = isExpanded ? nil : level
                }
            } label: {
                HStack(spacing: pad.md) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 10)
                            .fill(Color.white)
                            .frame(
                                width: AppSizing.iconSize(base: 40, sizeClass: sc),
                                height: AppSizing.iconSize(base: 40, sizeClass: sc)
                            )
                        Image(systemName: level.icon)
                            .font(.system(size: AppSizing.fontSize(base: 16, sizeClass: sc)))
                            .foregroundColor(level.tintColor)
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        Text(level.title)
                            .font(AppTheme.Typography.headline(sc))
                            .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                        Text("\(levelLessons.count) lessons")
                            .font(AppTheme.Typography.caption(sc))
                            .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    }

                    Spacer()

                    Image(systemName: "chevron.down")
                        .font(.system(size: AppSizing.fontSize(base: 14, sizeClass: sc), weight: .semibold))
                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        .rotationEffect(.degrees(isExpanded ? 180 : 0))
                }
                .padding(pad.md)
                .frame(maxWidth: .infinity)
                .background(level.tintColor.opacity(0.18))
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            // Expanded content — the lessons for this level
            if isExpanded {
                VStack(spacing: pad.xs) {
                    Divider()
                        .padding(.horizontal, pad.md)

                    ForEach(levelLessons) { lesson in
                        Button {
                            playAndScrollToTop(
                                fileName: lesson.fileName,
                                title: lesson.title,
                                category: "\(level.title) lesson",
                                durationLabel: lesson.duration
                            )
                        } label: {
                            HStack(spacing: pad.sm) {
                                Image(systemName: isCurrentlyPlaying(lesson.fileName) ? "waveform" : "play.circle.fill")
                                    .font(.system(size: AppSizing.fontSize(base: 16, sizeClass: sc)))
                                    .foregroundColor(AppTheme.Colors.primaryFallback)

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(lesson.title)
                                        .font(AppTheme.Typography.subheadline(sc))
                                        .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                                    Text(lesson.duration)
                                        .font(AppTheme.Typography.caption(sc))
                                        .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                                }

                                Spacer()
                            }
                            .padding(.horizontal, pad.md)
                            .padding(.vertical, 8)
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                    }

                    Spacer(minLength: pad.xs)
                }
                .background(AppTheme.Colors.surface)
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .cornerRadius(AppSizing.cardRadius(sc))
        .shadow(
            color: AppTheme.Shadow.card.color,
            radius: AppTheme.Shadow.card.radius,
            x: AppTheme.Shadow.card.x,
            y: AppTheme.Shadow.card.y
        )
        .clipped()
    }

    // MARK: - Podcasts Section
    private var podcastsSection: some View {
        let pad = AppSizing.Padding.forClass(sc)
        let cols = sc == .compact ? 2 : AppSizing.gridColumns(sc)

        return VStack(alignment: .leading, spacing: pad.sm) {
            Text("Podcasts")
                .font(.system(size: AppSizing.fontSize(base: 17, sizeClass: sc), weight: .bold))
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)

            LazyVGrid(
                columns: Array(repeating: GridItem(.flexible(), spacing: pad.sm), count: cols),
                spacing: pad.sm
            ) {
                ForEach(ListeningData.podcasts) { pod in
                    Button {
                        selectedPodcast = pod
                    } label: {
                        VStack(alignment: .leading, spacing: 0) {
                            // Coloured top area
                            ZStack {
                                pod.color
                                Image(systemName: "antenna.radiowaves.left.and.right")
                                    .font(.system(size: AppSizing.fontSize(base: 28, sizeClass: sc)))
                                    .foregroundColor(.white)
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: AppSizing.mediaHeight(base: 100, sizeClass: sc))
                            .cornerRadius(AppSizing.cardRadius(sc))

                            VStack(alignment: .leading, spacing: 3) {
                                Text(pod.title)
                                    .font(AppTheme.Typography.headline(sc))
                                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                                Text("\(pod.episodes.count) episodes")
                                    .font(AppTheme.Typography.subheadline(sc))
                                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                            }
                            .padding(pad.sm)
                        }
                        .background(AppTheme.Colors.surface)
                        .cornerRadius(AppSizing.cardRadius(sc))
                        .shadow(
                            color: AppTheme.Shadow.card.color,
                            radius: AppTheme.Shadow.card.radius,
                            x: AppTheme.Shadow.card.x,
                            y: AppTheme.Shadow.card.y
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    // MARK: - Dialogues Section
    private var dialoguesSection: some View {
        let pad = AppSizing.Padding.forClass(sc)
        let cols = 2   // dialogues need more per-card width for titles like "Post Office", "Interview"
        let allDialogues = ListeningData.dialogues
        let visibleDialogues = showAllDialogues ? allDialogues : Array(allDialogues.prefix(7))
        let hasMore = allDialogues.count > 7

        return VStack(alignment: .leading, spacing: pad.sm) {
            Text("Dialogues")
                .font(.system(size: AppSizing.fontSize(base: 17, sizeClass: sc), weight: .bold))
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)

            LazyVGrid(
                columns: Array(repeating: GridItem(.flexible(), spacing: pad.sm), count: cols),
                spacing: pad.sm
            ) {
                ForEach(visibleDialogues) { dialogue in
                    Button {
                        playAndScrollToTop(
                            fileName: dialogue.fileName,
                            title: dialogue.title,
                            category: "Dialogue",
                            durationLabel: dialogue.duration
                        )
                    } label: {
                        HStack(spacing: pad.sm) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(AppTheme.Colors.primaryLightFallback)
                                    .frame(
                                        width: AppSizing.iconSize(base: 36, sizeClass: sc),
                                        height: AppSizing.iconSize(base: 36, sizeClass: sc)
                                    )
                                Image(systemName: dialogue.icon)
                                    .font(.system(size: AppSizing.fontSize(base: 15, sizeClass: sc)))
                                    .foregroundColor(AppTheme.Colors.primaryFallback)
                            }

                            VStack(alignment: .leading, spacing: 2) {
                                Text(dialogue.title)
                                    .font(AppTheme.Typography.headline(sc))
                                    .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                                    .lineLimit(2)
                                    .minimumScaleFactor(0.75)
                                    .fixedSize(horizontal: false, vertical: true)
                                Text(dialogue.duration)
                                    .font(AppTheme.Typography.caption(sc))
                                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)

                            Spacer(minLength: 0)
                        }
                        .padding(pad.sm)
                        .background(AppTheme.Colors.surface)
                        .cornerRadius(AppSizing.cardRadius(sc))
                        .shadow(
                            color: AppTheme.Shadow.card.color,
                            radius: AppTheme.Shadow.card.radius,
                            x: AppTheme.Shadow.card.x,
                            y: AppTheme.Shadow.card.y
                        )
                    }
                    .buttonStyle(.plain)
                }
            }

            if hasMore {
                Button {
                    withAnimation(.easeInOut(duration: 0.25)) {
                        showAllDialogues.toggle()
                    }
                } label: {
                    Text(showAllDialogues ? "Show less" : "Show more")
                        .font(AppTheme.Typography.subheadline(sc))
                        .fontWeight(.semibold)
                        .foregroundColor(AppTheme.Colors.primaryFallback)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.top, 4)
                }
                .buttonStyle(.plain)
            }
        }
    }

    // MARK: - Dictation Section (inline — no sheet, no separate screen)
    //
    // Three states, all rendered directly under the "Dictation exercise"
    // heading on this same scroll view:
    //   1. Not started  -> level picker + Start button
    //   2. In progress   -> current sentence, play button, text field / result
    //   3. Finished      -> summary, with a "Done" button that resets back to (1)
    private var dictationSection: some View {
        let pad = AppSizing.Padding.forClass(sc)
        return VStack(alignment: .leading, spacing: pad.sm) {
            Text("Dictation exercise")
                .font(.system(size: AppSizing.fontSize(base: 17, sizeClass: sc), weight: .bold))
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)

            if !dictationStarted {
                dictationIntroCard
            } else if dictationFinished {
                dictationSummaryCard
            } else {
                dictationExerciseCard
            }
        }
    }

    private var dictationSentencesForLevel: [DictationSentence] {
        ListeningData.dictationSentences.filter { $0.level == dictationLevel }
    }

    private var dictationIntroCard: some View {
        let pad = AppSizing.Padding.forClass(sc)
        return VStack(spacing: pad.md) {
            Text("Listen and type what you hear. \(ListeningData.dictationSentences.count) sentences with instant scoring.")
                .font(AppTheme.Typography.body(sc))
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                .multilineTextAlignment(.center)

            HStack(spacing: 10) {
                ForEach(LessonLevel.allCases) { level in
                    Button {
                        dictationLevel = level
                    } label: {
                        Text(level.title)
                            .font(AppTheme.Typography.subheadline(sc))
                            .fontWeight(.semibold)
                            .foregroundColor(dictationLevel == level ? .white : AppTheme.Colors.textSecondaryFallback)
                            .padding(.horizontal, 18)
                            .padding(.vertical, 10)
                            .background(
                                Capsule().fill(dictationLevel == level ? dictationPillColor(level) : Color(.systemGray5))
                            )
                    }
                    .buttonStyle(.plain)
                }
            }

            Button {
                startDictation()
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "speaker.wave.2.fill")
                        .font(.system(size: AppSizing.fontSize(base: 14, sizeClass: sc)))
                    Text("Start")
                        .font(AppTheme.Typography.headline(sc))
                        .fontWeight(.semibold)
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: AppSizing.Button.forClass(sc).height)
                .background(AppTheme.Colors.primaryFallback)
                .cornerRadius(AppSizing.Button.forClass(sc).cornerRadius)
            }
            .buttonStyle(.plain)
        }
        .padding(pad.md)
        .background(AppTheme.Colors.surface)
        .cornerRadius(AppSizing.cardRadius(sc))
        .shadow(
            color: AppTheme.Shadow.card.color,
            radius: AppTheme.Shadow.card.radius,
            x: AppTheme.Shadow.card.x,
            y: AppTheme.Shadow.card.y
        )
    }

    private var dictationExerciseCard: some View {
        let pad = AppSizing.Padding.forClass(sc)
        return VStack(alignment: .leading, spacing: pad.md) {
            HStack {
                HStack(spacing: 4) {
                    Image(systemName: dictationLevel.icon)
                    Text(dictationLevel.title)
                }
                .font(AppTheme.Typography.subheadline(sc))
                .fontWeight(.semibold)
                .foregroundColor(dictationLevel.tintColor)

                Spacer()

                Text("Sentence \(dictationIndex + 1) of \(dictationSentencesForLevel.count)")
                    .font(AppTheme.Typography.subheadline(sc))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
            }

            HStack(spacing: 12) {
                Button {
                    playCurrentDictationSentence()
                } label: {
                    ZStack {
                        Circle()
                            .fill(AppTheme.Colors.primaryFallback)
                            .frame(width: 36, height: 36)
                        Image(systemName: audio.isPlaying ? "speaker.wave.3.fill" : "speaker.wave.2.fill")
                            .font(.system(size: 14))
                            .foregroundColor(.white)
                    }
                }
                .buttonStyle(.plain)

                Text("Listen, then type what you hear.")
                    .font(AppTheme.Typography.subheadline(sc))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)

                Spacer()
            }

            if let result = dictationCurrentResult, dictationShowCheck {
                Text(result.typed)
                    .font(AppTheme.Typography.body(sc))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(Color(.systemGray4))
                    )

                Text("Accuracy: \(Int(result.accuracy))%")
                    .font(AppTheme.Typography.headline(sc))
                    .fontWeight(.bold)
                    .foregroundColor(dictationAccuracyColor(result.accuracy))

                dictationHighlightedSentence(result)
                    .font(AppTheme.Typography.body(sc))
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Text("Correct answer: \(result.sentence)")
                    .font(AppTheme.Typography.caption(sc))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)

                Button {
                    goToNextDictationSentence()
                } label: {
                    Text(dictationIndex == dictationSentencesForLevel.count - 1 ? "Finish" : "Next")
                        .font(AppTheme.Typography.headline(sc))
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: AppSizing.Button.forClass(sc).height)
                        .background(AppTheme.Colors.primaryFallback)
                        .cornerRadius(AppSizing.Button.forClass(sc).cornerRadius)
                }
                .buttonStyle(.plain)
            } else {
                TextField("Type the sentence here", text: $dictationTypedAnswer, axis: .vertical)
                    .font(AppTheme.Typography.body(sc))
                    .padding()
                    .background(
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(Color(.systemGray4))
                    )
                    .autocorrectionDisabled(true)
                    .textInputAutocapitalization(.never)

                Button {
                    checkDictationAnswer()
                } label: {
                    Text("Check")
                        .font(AppTheme.Typography.headline(sc))
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: AppSizing.Button.forClass(sc).height)
                        .background(dictationTypedAnswer.trimmingCharacters(in: .whitespaces).isEmpty ? Color.gray : AppTheme.Colors.primaryFallback)
                        .cornerRadius(AppSizing.Button.forClass(sc).cornerRadius)
                }
                .buttonStyle(.plain)
                .disabled(dictationTypedAnswer.trimmingCharacters(in: .whitespaces).isEmpty)
            }

            Button {
                resetDictation()
            } label: {
                Text("Cancel")
                    .font(AppTheme.Typography.caption(sc))
                    .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                    .frame(maxWidth: .infinity, alignment: .center)
            }
            .buttonStyle(.plain)
        }
        .padding(pad.md)
        .background(AppTheme.Colors.surface)
        .cornerRadius(AppSizing.cardRadius(sc))
        .shadow(
            color: AppTheme.Shadow.card.color,
            radius: AppTheme.Shadow.card.radius,
            x: AppTheme.Shadow.card.x,
            y: AppTheme.Shadow.card.y
        )
    }

    private var dictationSummaryCard: some View {
        let pad = AppSizing.Padding.forClass(sc)
        let totalCorrect = dictationResults.filter { $0.accuracy >= 80 }.count
        let totalIncorrect = dictationResults.count - totalCorrect
        let overallAccuracy = dictationResults.isEmpty ? 0 : dictationResults.map { $0.accuracy }.reduce(0, +) / Double(dictationResults.count)

        return VStack(spacing: pad.md) {
            Image(systemName: "checkmark.seal.fill")
                .font(.system(size: 44))
                .foregroundColor(AppTheme.Colors.primaryFallback)

            Text("Dictation complete!")
                .font(AppTheme.Typography.headline(sc))
                .fontWeight(.bold)
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)

            VStack(spacing: 10) {
                dictationSummaryRow(label: "Overall accuracy", value: "\(Int(overallAccuracy))%")
                dictationSummaryRow(label: "Correct sentences", value: "\(totalCorrect)")
                dictationSummaryRow(label: "Incorrect sentences", value: "\(totalIncorrect)")
                dictationSummaryRow(label: "Total sentences", value: "\(dictationResults.count)")
            }
            .padding(pad.sm)
            .background(Color(.secondarySystemBackground))
            .cornerRadius(10)

            Button {
                resetDictation()
            } label: {
                Text("Done")
                    .font(AppTheme.Typography.headline(sc))
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: AppSizing.Button.forClass(sc).height)
                    .background(AppTheme.Colors.primaryFallback)
                    .cornerRadius(AppSizing.Button.forClass(sc).cornerRadius)
            }
            .buttonStyle(.plain)
        }
        .padding(pad.md)
        .background(Color.white)
        .cornerRadius(AppSizing.cardRadius(sc))
        .shadow(
            color: AppTheme.Shadow.card.color,
            radius: AppTheme.Shadow.card.radius,
            x: AppTheme.Shadow.card.x,
            y: AppTheme.Shadow.card.y
        )
    }

    private func dictationSummaryRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
            Spacer()
            Text(value)
                .fontWeight(.semibold)
                .foregroundColor(AppTheme.Colors.textPrimaryFallback)
        }
    }

    /// Selection-pill color per level (blue / green / purple) — matches the
    /// original design, independent of LessonLevel.tintColor which is used
    /// elsewhere for the lesson accordion icons.
    private func dictationPillColor(_ level: LessonLevel) -> Color {
        switch level {
        case .beginner: return .blue
        case .intermediate: return .green
        case .advanced: return .purple
        }
    }

    private func dictationAccuracyColor(_ accuracy: Double) -> Color {
        accuracy >= 80 ? .green : (accuracy >= 50 ? .orange : .red)
    }

    /// Builds a single wrapping line of text where each word from the
    /// ORIGINAL sentence is colored green if it matches what the user typed
    /// in that position, red otherwise.
    private func dictationHighlightedSentence(_ result: DictationResult) -> Text {
        var combined = Text("")
        for (index, word) in result.words.enumerated() {
            let segment = Text(word.text).foregroundColor(word.isCorrect ? .green : .red)
            combined = index == 0 ? segment : combined + Text(" ") + segment
        }
        return combined
    }

    // MARK: Dictation logic

    private func startDictation() {
        dictationStarted = true
        dictationFinished = false
        dictationIndex = 0
        dictationResults = []
        dictationTypedAnswer = ""
        dictationShowCheck = false
        dictationCurrentResult = nil
        playCurrentDictationSentence()
        withAnimation(.easeInOut(duration: 0.3)) {
            scrollProxy?.scrollTo(nowPlayingCardID, anchor: .top)
        }
    }

    private func playCurrentDictationSentence() {
        guard dictationSentencesForLevel.indices.contains(dictationIndex) else { return }
        let sentence = dictationSentencesForLevel[dictationIndex]
        audio.play(fileName: sentence.fileName, title: "Dictation \(dictationIndex + 1)", category: dictationLevel.title, durationLabel: "")
    }

    private func checkDictationAnswer() {
        guard dictationSentencesForLevel.indices.contains(dictationIndex) else { return }
        let sentence = dictationSentencesForLevel[dictationIndex]
        let result = Self.evaluateDictation(original: sentence.text, typed: dictationTypedAnswer)
        dictationCurrentResult = result
        dictationResults.append(result)
        dictationShowCheck = true

        // NEW: record this single sentence's result (correct/wrong) to
        // today's skillStats doc, for MyProgressScreen's Listening
        // breakdown (Correct / Wrong / Total). Uses the same >= 80%
        // threshold the summary card already uses to color-code results.
        SkillStatsService.shared.recordDictationResult(isCorrect: result.accuracy >= 80)
    }

    private func goToNextDictationSentence() {
        dictationShowCheck = false
        dictationCurrentResult = nil
        dictationTypedAnswer = ""

        if dictationIndex == dictationSentencesForLevel.count - 1 {
            audio.stop()
            dictationFinished = true
        } else {
            dictationIndex += 1
            playCurrentDictationSentence()
        }
    }

    /// Resets the dictation card back to its "not started" state — used by
    /// both the "Cancel" button mid-exercise and "Done" on the summary.
    private func resetDictation() {
        audio.stop()
        dictationStarted = false
        dictationFinished = false
        dictationIndex = 0
        dictationTypedAnswer = ""
        dictationShowCheck = false
        dictationCurrentResult = nil
        dictationResults = []
    }

    /// Compares the typed answer against the original sentence word-by-word.
    /// Comparison is case-insensitive and ignores leading/trailing punctuation,
    /// but the ORIGINAL word (with its real casing/punctuation) is kept for
    /// display so the highlighted sentence and "Correct answer" line read
    /// naturally.
    private static func evaluateDictation(original: String, typed: String) -> DictationResult {
        func normalize(_ s: String) -> String {
            s.lowercased().trimmingCharacters(in: .punctuationCharacters)
        }

        let originalTokens = original.split(separator: " ").map(String.init)
        let typedTokens = typed.split(separator: " ").map(String.init)

        var matchedCount = 0
        var highlighted: [DictationWord] = []

        for (index, token) in originalTokens.enumerated() {
            let isCorrect = index < typedTokens.count && normalize(typedTokens[index]) == normalize(token)
            if isCorrect { matchedCount += 1 }
            highlighted.append(DictationWord(text: token, isCorrect: isCorrect))
        }

        let accuracy = originalTokens.isEmpty ? 0 : (Double(matchedCount) / Double(originalTokens.count)) * 100

        return DictationResult(sentence: original, typed: typed, accuracy: accuracy, words: highlighted)
    }

    private func isCurrentlyPlaying(_ fileName: String) -> Bool {
        audio.nowPlaying?.fileName == fileName && audio.isPlaying
    }

    /// Plays the given audio and scrolls the screen back up so the Now
    /// Playing card is visible, even if the user tapped something further
    /// down the page (e.g. a dialogue, or a lesson inside an expanded level).
    private func playAndScrollToTop(fileName: String, title: String, category: String, durationLabel: String) {
        audio.play(fileName: fileName, title: title, category: category, durationLabel: durationLabel)
        withAnimation(.easeInOut(duration: 0.3)) {
            scrollProxy?.scrollTo(nowPlayingCardID, anchor: .top)
        }
    }
}


// ─────────────────────────────────────────────────────────────
// MARK: - Podcast Episodes Sheet
// ─────────────────────────────────────────────────────────────
// (Unchanged — podcasts still open in a sheet, since that wasn't part of
// what needed to move inline.)

private struct PodcastEpisodesSheet: View {
    let podcast: PodcastItem
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var audio = AudioManager.shared

    var body: some View {
        NavigationView {
            List(podcast.episodes) { episode in
                Button {
                    audio.play(
                        fileName: episode.fileName,
                        title: episode.title,
                        category: podcast.title,
                        durationLabel: episode.duration
                    )
                    dismiss()
                } label: {
                    HStack {
                        Image(systemName: "play.circle.fill")
                            .foregroundColor(AppTheme.Colors.primaryFallback)
                            .font(.system(size: 22))

                        VStack(alignment: .leading, spacing: 3) {
                            Text(episode.title)
                                .foregroundColor(AppTheme.Colors.textPrimaryFallback)
                                .fontWeight(.medium)
                            Text(episode.duration)
                                .font(.caption)
                                .foregroundColor(AppTheme.Colors.textSecondaryFallback)
                        }

                        Spacer()
                    }
                    .padding(.vertical, 4)
                }
            }
            .navigationTitle(podcast.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// MARK: - Dictation model types
// ─────────────────────────────────────────────────────────────
// Kept as small, file-private value types. The exercise/summary UI and all
// the logic that uses them now live directly on ListeningPracticeScreen
// above, instead of in a separate DictationExerciseView/sheet.

private struct DictationWord: Identifiable {
    let id = UUID()
    let text: String       // original casing/punctuation, e.g. "deliberation."
    let isCorrect: Bool
}

private struct DictationResult {
    let sentence: String
    let typed: String
    let accuracy: Double
    let words: [DictationWord]
}
