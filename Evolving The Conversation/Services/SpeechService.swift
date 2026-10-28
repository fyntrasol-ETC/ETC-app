
import AVFoundation

// MARK: - Speech Helper
final class SpeechService {
    static let shared = SpeechService()
    private let synthesizer = AVSpeechSynthesizer()

    private init() {}

    func speak(_ text: String, language: String = "en-US") {
        // Stop anything currently speaking so rapid taps don't queue up/overlap
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }

        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: language)
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * 0.9 // slightly slower, clearer for a learner app
        synthesizer.speak(utterance)
    }
}
