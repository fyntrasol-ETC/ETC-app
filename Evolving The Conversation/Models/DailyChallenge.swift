import SwiftUI
import FirebaseAuth
import FirebaseFirestore
import Combine

// MARK: - Daily Challenge Type
enum DailyChallengeType: String, CaseIterable, Codable, Identifiable {
    case watchLesson   = "watch_lesson"
    case grammarQuiz   = "grammar_quiz"
    case generalQuiz   = "general_quiz"
    case essayWriting  = "essay_writing"
    case aiSpeaking    = "ai_speaking"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .watchLesson:  return "Complete a video lesson"
        case .grammarQuiz:  return "Finish a grammar quiz"
        case .generalQuiz:  return "Finish a quiz"
        case .essayWriting: return "Write a short essay"
        case .aiSpeaking:   return "Talk with your AI tutor"
        }
    }

    var subtitle: String {
        switch self {
        case .watchLesson:  return "Watch any lesson from your enrolled course."
        case .grammarQuiz:  return "Pick any grammar topic and finish the quiz."
        case .generalQuiz:  return "Answer a mixed quiz from the Quiz Center."
        case .essayWriting: return "Submit one essay for AI feedback."
        case .aiSpeaking:   return "Have a short conversation with your AI tutor."
        }
    }

    var icon: String {
        switch self {
        case .watchLesson:  return "play.rectangle.fill"
        case .grammarQuiz:  return "doc.text.fill"
        case .generalQuiz:  return "target"
        case .essayWriting: return "pencil"
        case .aiSpeaking:   return "mic.fill"
        }
    }

    var xpReward: Int { 50 }
}

// MARK: - Daily Challenge Model
struct DailyChallenge {
    var dateKey: String        // "yyyy-MM-dd"
    var type: DailyChallengeType
    var isCompleted: Bool = false
}

// MARK: - Daily Challenge Service
@MainActor
final class DailyChallengeService: ObservableObject {
    static let shared = DailyChallengeService()
    private init() {}

    @Published var challenge: DailyChallenge? = nil

    private var listener: ListenerRegistration?
    private let db = Firestore.firestore()

    static func todayKey() -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.timeZone = .current
        return f.string(from: Date())
    }

    /// Starts (or refreshes) the listener for today's challenge doc.
    /// Safe to call repeatedly — it re-attaches only if needed.
    func listen() {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        let dateKey = Self.todayKey()

        // If we already have a listener for today, don't re-attach.
        if listener != nil, challenge?.dateKey == dateKey { return }

        listener?.remove()
        let ref = db.collection("users").document(uid)
            .collection("dailyChallenges").document(dateKey)

        listener = ref.addSnapshotListener { [weak self] snapshot, _ in
            guard let self else { return }
            if let data = snapshot?.data(),
               let typeRaw = data["type"] as? String,
               let type = DailyChallengeType(rawValue: typeRaw) {
                let completed = data["isCompleted"] as? Bool ?? false
                self.challenge = DailyChallenge(dateKey: dateKey, type: type, isCompleted: completed)
            } else {
                self.createTodayChallenge(uid: uid, dateKey: dateKey, ref: ref)
            }
        }
    }

    /// Deterministic pick seeded by date + uid, so every user gets a stable
    /// (but different-feeling) challenge each day without needing a server
    /// function — and it's reproducible even before Firestore round-trips.
    private func createTodayChallenge(uid: String, dateKey: String, ref: DocumentReference) {
        let allTypes = DailyChallengeType.allCases
        let seed = (dateKey + uid).hashValue
        let index = abs(seed) % allTypes.count
        let type = allTypes[index]

        ref.setData([
            "type": type.rawValue,
            "isCompleted": false,
            "createdAt": FieldValue.serverTimestamp()
        ], merge: true)

        // Optimistic local value; listener confirms once Firestore replies.
        self.challenge = DailyChallenge(dateKey: dateKey, type: type, isCompleted: false)
    }

    /// Call when the user finishes the activity matching today's challenge.
    func markComplete(_ type: DailyChallengeType) {
        guard let uid = Auth.auth().currentUser?.uid,
              let challenge, challenge.type == type, !challenge.isCompleted else { return }

        let ref = db.collection("users").document(uid)
            .collection("dailyChallenges").document(challenge.dateKey)
        ref.setData(["isCompleted": true], merge: true) { error in
            if let error {
                print("⚠️ Failed to mark challenge complete: \(error.localizedDescription)")
            }
        }
        XPService.shared.recordActivity(.dailyChallenge)   // ← add this
    }

    func reset() {
        listener?.remove()
        listener = nil
        challenge = nil
    }
}
