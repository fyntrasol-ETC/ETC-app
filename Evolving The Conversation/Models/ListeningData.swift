import SwiftUI
import AVFoundation
import Combine

// ─────────────────────────────────────────────────────────────
// MARK: - Data Models
// ─────────────────────────────────────────────────────────────

/// Difficulty level for an audio lesson. Lives here (not in the view file)
/// so the Listening Practice screen never has to hardcode lesson content —
/// it just groups `ListeningData.audioLessons` by `level`.
enum LessonLevel: String, CaseIterable, Identifiable {
    case beginner
    case intermediate
    case advanced

    var id: String { rawValue }

    var title: String {
        switch self {
        case .beginner: return "Beginner"
        case .intermediate: return "Intermediate"
        case .advanced: return "Advanced"
        }
    }

    var icon: String {
        switch self {
        case .beginner: return "leaf"
        case .intermediate: return "flame"
        case .advanced: return "bolt.fill"
        }
    }

    var tintColor: Color {
        switch self {
        case .beginner: return .green
        case .intermediate: return .orange
        case .advanced: return .red
        }
    }
}

struct AudioLessonItem: Identifiable {
    let id = UUID()
    let title: String
    let duration: String
    let fileName: String       // file name WITHOUT extension, e.g. "lesson_common_idioms"
    let level: LessonLevel
}

struct PodcastEpisodeItem: Identifiable {
    let id = UUID()
    let title: String
    let duration: String
    let fileName: String
}

struct PodcastItem: Identifiable {
    let id = UUID()
    let title: String
    let color: Color
    let episodes: [PodcastEpisodeItem]
}

struct DialogueItem: Identifiable {
    let id = UUID()
    let title: String          // "Airport", "Restaurant", ...
    let icon: String           // SF Symbol name
    let duration: String
    let fileName: String
}

struct DictationSentence: Identifiable {
    let id = UUID()
    let text: String
    let fileName: String
    let level: LessonLevel
}

/// Describes whatever is currently loaded in the player, so the
/// "Now Playing" card always knows what to display.
struct NowPlayingInfo: Equatable {
    var title: String
    var category: String
    var duration: String
    var fileName: String
}

// ─────────────────────────────────────────────────────────────
// MARK: - Audio Manager
// ─────────────────────────────────────────────────────────────

/// A single shared audio player used across the whole Listening
/// Practice feature. Every "play" button in the UI calls into this
/// one object, which is why the Now Playing card can always reflect
/// whatever was tapped last.
final class AudioManager: NSObject, ObservableObject {

    static let shared = AudioManager()

    @Published var isPlaying: Bool = false
    @Published var currentTime: TimeInterval = 0
    @Published var duration: TimeInterval = 0
    @Published var progress: Double = 0          // 0...1, kept in sync with currentTime/duration
    @Published var nowPlaying: NowPlayingInfo? = nil

    private var player: AVAudioPlayer?
    private var progressTimer: Timer?

    private override init() {
        super.init()
        configureAudioSession()
    }

    private func configureAudioSession() {
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("AudioManager: failed to configure audio session - \(error)")
        }
    }

    /// Loads and immediately plays the given audio file, and updates
    /// the Now Playing card with the supplied metadata.
    ///
    /// The Now Playing card is updated *first*, before the bundle lookup,
    /// so the UI always reflects whatever was just tapped — even if the
    /// mp3 hasn't been added to the Xcode project yet. If the file is
    /// found, playback starts normally; if not, Now Playing still shows
    /// the tapped item, just in a paused state, and a message is logged.
    ///
    /// The mp3 must be added to the Xcode project (dragged into the
    /// project navigator with "Copy items if needed" and the app
    /// target checked) so it ends up in the main bundle.
    func play(fileName: String, title: String, category: String, durationLabel: String) {
        nowPlaying = NowPlayingInfo(title: title, category: category, duration: durationLabel, fileName: fileName)
        currentTime = 0
        progress = 0

        guard let url = Bundle.main.url(forResource: fileName, withExtension: "wav") else {
            print("AudioManager: could not find audio file named \(fileName).wav in the app bundle")
            player = nil
            isPlaying = false
            duration = 0
            stopProgressTimer()
            return
        }

        do {
            let newPlayer = try AVAudioPlayer(contentsOf: url)
            newPlayer.delegate = self
            player = newPlayer

            duration = newPlayer.duration

            newPlayer.prepareToPlay()
            newPlayer.play()
            isPlaying = true
            startProgressTimer()
        } catch {
            print("AudioManager: failed to play \(fileName) - \(error)")
            isPlaying = false
        }
    }

    func togglePlayPause() {
        guard let player = player else { return }
        if player.isPlaying {
            player.pause()
            isPlaying = false
            stopProgressTimer()
        } else {
            player.play()
            isPlaying = true
            startProgressTimer()
        }
    }

    func stop() {
        player?.stop()
        player = nil
        isPlaying = false
        currentTime = 0
        progress = 0
        nowPlaying = nil
        stopProgressTimer()
    }

    /// Seeks to a fraction (0...1) of the total duration. Used when the
    /// user drags on the progress bar.
    func seek(to progressValue: Double) {
        guard let player = player else { return }
        let clamped = min(max(0, progressValue), 1)
        let newTime = player.duration * clamped
        player.currentTime = newTime
        currentTime = newTime
        progress = clamped
    }

    private func startProgressTimer() {
        stopProgressTimer()
        progressTimer = Timer.scheduledTimer(withTimeInterval: 0.25, repeats: true) { [weak self] _ in
            self?.tick()
        }
    }

    private func stopProgressTimer() {
        progressTimer?.invalidate()
        progressTimer = nil
    }

    private func tick() {
        guard let player = player else { return }
        currentTime = player.currentTime
        duration = player.duration
        progress = duration > 0 ? currentTime / duration : 0
    }

    static func formattedTime(_ seconds: TimeInterval) -> String {
        guard seconds.isFinite, seconds >= 0 else { return "0:00" }
        let total = Int(seconds)
        let m = total / 60
        let s = total % 60
        return String(format: "%d:%02d", m, s)
    }
}

extension AudioManager: AVAudioPlayerDelegate {
    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        isPlaying = false
        progress = 1
        currentTime = duration
        stopProgressTimer()
    }
}

// ─────────────────────────────────────────────────────────────
// MARK: - Sample Data
// ─────────────────────────────────────────────────────────────

/// Replace the file names below with your real mp3 file names once
/// they're added to the Xcode project.
enum ListeningData {

    // 12 audio lessons, 4 per level, all covering practical spoken English:
    // idioms, phone conversations, greetings, and similar everyday topics.
    static let audioLessons: [AudioLessonItem] = [
        // Beginner
        AudioLessonItem(title: "Meeting Someone New", duration: "2:25", fileName: "lesson_beginner1", level: .beginner),
        AudioLessonItem(title: "First Day at Work", duration: "1:12", fileName: "lesson_beginner2", level: .beginner),
        AudioLessonItem(title: "Ordering Breakfast at a Café", duration: "1:51", fileName: "lesson_beginner3", level: .beginner),
        AudioLessonItem(title: "Shopping at the Supermarket", duration: "1:53", fileName: "lesson_beginner4", level: .beginner),

        // Intermediate
        AudioLessonItem(title: "Planning a Weekend Trip", duration: "1:11", fileName: "lesson_intermediate1", level: .intermediate),
        AudioLessonItem(title: "Working on a University Project", duration: "2:07", fileName: "lesson_intermediate2", level: .intermediate),
        AudioLessonItem(title: "Preparing for a Job Interview", duration: "1:27", fileName: "lesson_intermediate3", level: .intermediate),
        AudioLessonItem(title: "Making Plans with Friends", duration: "1:08", fileName: "lesson_intermediate4", level: .intermediate),

        // Advanced
        AudioLessonItem(title: "The Future of Artificial Intelligence", duration: "2:37", fileName: "lesson_advanced1", level: .advanced),
        AudioLessonItem(title: "Building a Sustainable City", duration: "2:32", fileName: "lesson_advanced2", level: .advanced),
        AudioLessonItem(title: "Leading Through Change", duration: "1:21", fileName: "lesson_advanced3", level: .advanced),
        AudioLessonItem(title: "A Journey of Lifelong Learning", duration: "1:53", fileName: "lesson_advanced4", level: .advanced)
    ]

    static let podcasts: [PodcastItem] = [
        PodcastItem(title: "Daily English", color: AppTheme.Colors.greenFallback, episodes: [
            PodcastEpisodeItem(title: "Morning Routines", duration: "6:12", fileName: "daily_english_ep1"),
            PodcastEpisodeItem(title: "At the Coffee Shop", duration: "5:48", fileName: "daily_english_ep2"),
            PodcastEpisodeItem(title: "Weekend Plans", duration: "6:30", fileName: "daily_english_ep3"),
            PodcastEpisodeItem(title: "Talking About Weather", duration: "5:20", fileName: "daily_english_ep4"),
            PodcastEpisodeItem(title: "Everyday Errands", duration: "6:05", fileName: "daily_english_ep5")
        ]),
        PodcastItem(title: "Real Talk", color: AppTheme.Colors.orangeFallback, episodes: [
            PodcastEpisodeItem(title: "Modern Friendships", duration: "7:15", fileName: "real_talk_ep1"),
            PodcastEpisodeItem(title: "Work-Life Balance", duration: "7:40", fileName: "real_talk_ep2"),
            PodcastEpisodeItem(title: "Social Media Habits", duration: "6:55", fileName: "real_talk_ep3"),
            PodcastEpisodeItem(title: "Money Talk", duration: "7:20", fileName: "real_talk_ep4"),
            PodcastEpisodeItem(title: "Personal Growth", duration: "7:05", fileName: "real_talk_ep5")
        ])
    ]

    // Only 5 dialogues for now — these are the only ones with audio files
    // actually present in the bundle. The Android data file references 12
    // more (office, interview, bank, pharmacy, taxi, gym, school,
    // post_office, library, salon, bakery, gas_station) but those .wav
    // files haven't been added yet. Add more entries here once their audio
    // files are dropped into the Xcode project.
    static let dialogues: [DialogueItem] = [
        DialogueItem(title: "Airport", icon: "airplane", duration: "4:00", fileName: "airport"),
        DialogueItem(title: "Restaurant", icon: "fork.knife", duration: "3:00", fileName: "restaurant"),
        DialogueItem(title: "Hotel", icon: "bed.double", duration: "4:00", fileName: "hotel"),
        DialogueItem(title: "Shopping", icon: "bag", duration: "3:00", fileName: "shopping"),
        DialogueItem(title: "Hospital", icon: "cross.case", duration: "4:00", fileName: "hospital")
    ]
    static let dictationSentences: [DictationSentence] = [
        // Beginner
        DictationSentence(text: "I would like a cup of coffee please.", fileName: "dictation_beginner1", level: .beginner),
        DictationSentence(text: "What time does the store open today.", fileName: "dictation_beginner2", level: .beginner),
        DictationSentence(text: "Can you help me find the bus station.", fileName: "dictation_beginner3", level: .beginner),
        DictationSentence(text: "She usually wakes up early in the morning.", fileName: "dictation_beginner4", level: .beginner),

        // Intermediate
        DictationSentence(text: "I have been trying to reach you all week.", fileName: "dictation_intermediate1", level: .intermediate),
        DictationSentence(text: "The meeting was rescheduled to next Thursday.", fileName: "dictation_intermediate2", level: .intermediate),
        DictationSentence(text: "He apologized for arriving late to the interview.", fileName: "dictation_intermediate3", level: .intermediate),
        DictationSentence(text: "We should double check the numbers before sending.", fileName: "dictation_intermediate4", level: .intermediate),

        // Advanced
        DictationSentence(text: "The committee reached a consensus after lengthy deliberation.", fileName: "dictation_advanced1", level: .advanced),
        DictationSentence(text: "Economic uncertainty continues to influence investor confidence.", fileName: "dictation_advanced2", level: .advanced),
        DictationSentence(text: "Her argument was both persuasive and well documented.", fileName: "dictation_advanced3", level: .advanced),
        DictationSentence(text: "The policy sparked debate among lawmakers and analysts alike.", fileName: "dictation_advanced4", level: .advanced)
    ]
}
