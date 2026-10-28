import FirebaseAuth
import FirebaseFirestore

// MARK: - Skill Stats Service
//
// Backs the "Skill breakdown" section of MyProgressScreen. Mirrors the
// Android app's Firestore shape exactly: users/{uid}/skillStats/{today}
// with fields `speakingConfidence`, `dictationCorrect`, `dictationWrong`,
// `grammarQuizzesSolved` (see the Android MyProgressScreen.kt comments —
// "MUST match AuthViewModel.recordActivity() ... shared rule").
//
// Each recording method is called exactly once per real user action
// (one AI analysis, one dictation sentence checked, one quiz finished),
// so nothing here needs its own dedup logic — the call sites are
// responsible for only firing once per completion, same pattern as
// XPService.recordActivity().
@MainActor
final class SkillStatsService {
    static let shared = SkillStatsService()
    private init() {}

    private let db = Firestore.firestore()

    private func todayKey() -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.timeZone = .current
        return f.string(from: Date())
    }

    private func todayRef() -> DocumentReference? {
        guard let uid = Auth.auth().currentUser?.uid else { return nil }
        return db.collection("users").document(uid)
            .collection("skillStats").document(todayKey())
    }

    /// Speaking — stores the latest AI confidence score for today.
    /// Not cumulative (matches the Android reader, which just displays
    /// today's `speakingConfidence` as-is).
    func recordSpeakingConfidence(_ score: Int) {
        todayRef()?.setData([
            "speakingConfidence": score,
            "updatedAt": FieldValue.serverTimestamp()
        ], merge: true)
    }

    /// Listening — call once per dictation sentence checked.
    /// Bumps either dictationCorrect or dictationWrong by 1.
    func recordDictationResult(isCorrect: Bool) {
        let field = isCorrect ? "dictationCorrect" : "dictationWrong"
        todayRef()?.setData([
            field: FieldValue.increment(Int64(1)),
            "updatedAt": FieldValue.serverTimestamp()
        ], merge: true)
    }

    /// Grammar — call once when a quiz is finished (i.e. alongside the
    /// existing XPService.shared.recordActivity(.quiz) call).
    func recordGrammarQuizSolved() {
        todayRef()?.setData([
            "grammarQuizzesSolved": FieldValue.increment(Int64(1)),
            "updatedAt": FieldValue.serverTimestamp()
        ], merge: true)
    }

    // MARK: - Read (used by MyProgressScreen)

    struct SkillBreakdownStats {
        var confidenceScore: Int = 0
        var correctAnswers: Int = 0
        var wrongAnswers: Int = 0
        var grammarQuizzesSolved: Int = 0

        var totalDictations: Int { correctAnswers + wrongAnswers }
        var isEmpty: Bool {
            confidenceScore == 0 && correctAnswers == 0 &&
            wrongAnswers == 0 && grammarQuizzesSolved == 0
        }
    }

    func fetchTodayStats() async -> SkillBreakdownStats {
        guard let ref = todayRef(),
              let doc = try? await ref.getDocument(),
              doc.exists
        else { return SkillBreakdownStats() }

        return SkillBreakdownStats(
            confidenceScore: doc.get("speakingConfidence") as? Int ?? 0,
            correctAnswers: doc.get("dictationCorrect") as? Int ?? 0,
            wrongAnswers: doc.get("dictationWrong") as? Int ?? 0,
            grammarQuizzesSolved: doc.get("grammarQuizzesSolved") as? Int ?? 0
        )
    }
}
