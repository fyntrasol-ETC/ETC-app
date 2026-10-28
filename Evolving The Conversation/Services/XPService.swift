import FirebaseAuth
import FirebaseFirestore
import Combine

// MARK: - XP Service
//
// Mirrors the Android app's XP/streak logic 1:1, reading/writing the same
// fields on `users/{uid}`: `xp`, `streak`, `lastStreakDate`, `lastLoginXpDate`
// (dates as "yyyy-MM-dd" strings). Keeping the field names identical means
// this and the Android implementation are just two clients of the same
// server-side state — no per-platform XP totals.
//
// XP:   Lesson +20 · Quiz +10 · Daily Challenge +30 · AI Speaking +15 · Daily Login +5
// Streak: increments the first time *any* qualifying activity (lesson/quiz/
//         challenge/AI speaking) happens on a new day. Resets to 1 if a day
//         was missed. Daily login XP is separate from streak — it just fires
//         once per day for opening the app, whether or not the user does
//         an activity.
//
// NEW: every qualifying activity also writes to
//   users/{uid}/activityLog/{yyyy-MM-dd}.xpEarned   (increment)
//   users/{uid}/monthlyStats/{yyyy-MM}.xpEarned      (increment)
// These back MyProgressScreen's Week/Month/Year activity chart — same
// Firestore shape the Android MyProgressScreen.kt reads from
// (fetchWeekActivity / fetchMonthActivity / fetchYearActivity).
//
// Dedup note: this service does NOT dedupe on its own — it trusts each
// call site to invoke recordActivity() exactly once per real completion
// (e.g. VideoLessonScreen's `isComplete` guard makes sure re-watching an
// already-completed lesson never calls this again). If you add a new
// call site, make sure it's similarly guarded.
@MainActor
final class XPService: ObservableObject {
    static let shared = XPService()
    private init() {}

    @Published private(set) var xp: Int = 0
    @Published private(set) var streak: Int = 0
    @Published var justEarnedXP: Int? = nil
    private var xpToastTask: Task<Void, Never>? = nil

    private var listener: ListenerRegistration?
    private let db = Firestore.firestore()

    enum ActivityXP: Int {
        case lesson         = 20
        case quiz           = 10
        case dailyChallenge = 30
        case aiSpeaking     = 15
    }

    // MARK: - Date helpers
    private func dateKey(daysAgo: Int = 0) -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.timeZone = .current
        let date = daysAgo == 0
            ? Date()
            : (Calendar.current.date(byAdding: .day, value: -daysAgo, to: Date()) ?? Date())
        return f.string(from: date)
    }

    private func monthKey() -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM"
        f.timeZone = .current
        return f.string(from: Date())
    }

    // MARK: - Live stats (for HomeScreen / ProfileScreen / MyProgressScreen)
    func start() {
        guard listener == nil, let uid = Auth.auth().currentUser?.uid else { return }
        listener = db.collection("users").document(uid)
            .addSnapshotListener { [weak self] snapshot, _ in
                guard let self, let snapshot else { return }
                self.xp     = (snapshot.get("xp") as? Int) ?? 0
                self.streak = (snapshot.get("streak") as? Int) ?? 0
            }
    }

    func reset() {
        listener?.remove()
        listener = nil
        xp = 0
        streak = 0
    }

    // MARK: - Record a qualifying activity
    // Call this for: lesson complete, quiz complete, daily challenge
    // complete, AI speaking turn complete. Updates XP always; updates
    // streak only the first time this fires on a given calendar day;
    // always logs the XP against today's activityLog + this month's
    // monthlyStats doc for the progress chart.
    func recordActivity(_ activity: ActivityXP, onDone: @escaping (Bool) -> Void = { _ in }) {
        guard let uid = Auth.auth().currentUser?.uid else { onDone(false); return }
        let ref = db.collection("users").document(uid)
        let today = dateKey()
        let yesterday = dateKey(daysAgo: 1)
        let xpAmount = activity.rawValue

        let activityLogRef = ref.collection("activityLog").document(today)
        let monthlyStatsRef = ref.collection("monthlyStats").document(monthKey())

        db.runTransaction({ (txn, errorPointer) -> Any? in
            do {
                let snapshot = try txn.getDocument(ref)
                let lastStreakDate = snapshot.get("lastStreakDate") as? String ?? ""
                let currentStreak  = (snapshot.get("streak") as? Int) ?? 0

                var updates: [String: Any] = [
                    "xp": FieldValue.increment(Int64(xpAmount))
                ]

                if lastStreakDate != today {
                    let newStreak = (lastStreakDate == yesterday) ? currentStreak + 1 : 1
                    updates["streak"] = newStreak
                    updates["lastStreakDate"] = today
                }

                txn.updateData(updates, forDocument: ref)

                // Log XP for the activity chart (Week/Month/Year).
                txn.setData(
                    ["xpEarned": FieldValue.increment(Int64(xpAmount))],
                    forDocument: activityLogRef,
                    merge: true
                )
                txn.setData(
                    ["xpEarned": FieldValue.increment(Int64(xpAmount))],
                    forDocument: monthlyStatsRef,
                    merge: true
                )
            } catch {
                errorPointer?.pointee = error as NSError
                return nil
            }
            return nil
        }, completion: { _, error in
            onDone(error == nil)
        })
    }

    // MARK: - Daily login XP
    // Fires just for showing up, whether or not the user does an activity.
    // Separate from streak. (Not logged to activityLog — the chart tracks
    // activity XP, matching the Android side.)
    func awardDailyLoginXpIfNeeded(onDone: @escaping (Bool) -> Void = { _ in }) {
        guard let uid = Auth.auth().currentUser?.uid else { onDone(false); return }
        let ref = db.collection("users").document(uid)
        let today = dateKey()

        db.runTransaction({ (txn, errorPointer) -> Any? in
            do {
                let snapshot = try txn.getDocument(ref)
                let lastLoginXpDate = snapshot.get("lastLoginXpDate") as? String ?? ""
                if lastLoginXpDate != today {
                    txn.updateData([
                        "xp": FieldValue.increment(Int64(5)),
                        "lastLoginXpDate": today
                    ], forDocument: ref)
                }
            } catch {
                errorPointer?.pointee = error as NSError
                return nil
            }
            return nil
        }, completion: { _, error in
            onDone(error == nil)
        })
    }

    // MARK: - One-shot fetch (fallback if a screen doesn't want a live listener)
    func fetchUserStats(onResult: @escaping (_ xp: Int, _ streak: Int) -> Void) {
        guard let uid = Auth.auth().currentUser?.uid else { onResult(0, 0); return }
        db.collection("users").document(uid).getDocument { doc, error in
            guard let doc, error == nil else { onResult(0, 0); return }
            let xp     = (doc.get("xp") as? Int) ?? 0
            let streak = (doc.get("streak") as? Int) ?? 0
            onResult(xp, streak)
        }
    }

    // MARK: - Activity chart data (Week / Month / Year)
    // Mirrors the Android app's fetchWeekActivity / fetchMonthActivity /
    // fetchYearActivity in MyProgressScreen.kt exactly — same collections,
    // same grouping.

    struct ActivityPoint: Identifiable {
        let id = UUID()
        let label: String
        let xp: Int
    }

    func fetchWeekActivity() async -> [ActivityPoint] {
        guard let uid = Auth.auth().currentUser?.uid else { return [] }
        let cal = Calendar.current
        let dayFmt = DateFormatter(); dayFmt.dateFormat = "yyyy-MM-dd"; dayFmt.timeZone = .current
        let letterFmt = DateFormatter(); letterFmt.dateFormat = "EEEEE"; letterFmt.timeZone = .current

        let dates = (0..<7).map { cal.date(byAdding: .day, value: -$0, to: Date()) ?? Date() }.reversed().map { $0 }
        let keys = dates.map { dayFmt.string(from: $0) }

        guard let first = keys.first else { return [] }
        let ref = db.collection("users").document(uid).collection("activityLog")
        guard let snapshot = try? await ref
            .whereField(FieldPath.documentID(), isGreaterThanOrEqualTo: first)
            .getDocuments()
        else { return [] }

        let byKey = Dictionary(uniqueKeysWithValues: snapshot.documents.map { ($0.documentID, $0) })
        return zip(dates, keys).map { date, key in
            let xp = (byKey[key]?.get("xpEarned") as? Int) ?? 0
            return ActivityPoint(label: letterFmt.string(from: date), xp: xp)
        }
    }

    func fetchMonthActivity() async -> [ActivityPoint] {
        guard let uid = Auth.auth().currentUser?.uid else { return [] }
        let cal = Calendar.current
        let dayFmt = DateFormatter(); dayFmt.dateFormat = "yyyy-MM-dd"; dayFmt.timeZone = .current

        let dates = (0..<28).map { cal.date(byAdding: .day, value: -$0, to: Date()) ?? Date() }.reversed().map { $0 }
        let keys = dates.map { dayFmt.string(from: $0) }

        guard let first = keys.first else { return [] }
        let ref = db.collection("users").document(uid).collection("activityLog")
        guard let snapshot = try? await ref
            .whereField(FieldPath.documentID(), isGreaterThanOrEqualTo: first)
            .getDocuments()
        else { return [] }

        let byKey = Dictionary(uniqueKeysWithValues: snapshot.documents.map { ($0.documentID, $0) })
        return (0..<4).map { weekIndex in
            let weekKeys = keys[(weekIndex * 7)..<(weekIndex * 7 + 7)]
            let xpSum = weekKeys.reduce(0) { $0 + ((byKey[$1]?.get("xpEarned") as? Int) ?? 0) }
            return ActivityPoint(label: "Wk \(weekIndex + 1)", xp: xpSum)
        }
    }

    func fetchYearActivity() async -> [ActivityPoint] {
        guard let uid = Auth.auth().currentUser?.uid else { return [] }
        let cal = Calendar.current
        let monthFmt = DateFormatter(); monthFmt.dateFormat = "yyyy-MM"; monthFmt.timeZone = .current
        let labelFmt = DateFormatter(); labelFmt.dateFormat = "MMM"; labelFmt.timeZone = .current

        let months = (0..<12).map { cal.date(byAdding: .month, value: -$0, to: Date()) ?? Date() }.reversed().map { $0 }
        let keys = months.map { monthFmt.string(from: $0) }

        guard let first = keys.first else { return [] }
        let ref = db.collection("users").document(uid).collection("monthlyStats")
        guard let snapshot = try? await ref
            .whereField(FieldPath.documentID(), isGreaterThanOrEqualTo: first)
            .getDocuments()
        else { return [] }

        let byKey = Dictionary(uniqueKeysWithValues: snapshot.documents.map { ($0.documentID, $0) })
        return zip(months, keys).map { date, key in
            let xp = (byKey[key]?.get("xpEarned") as? Int) ?? 0
            return ActivityPoint(label: labelFmt.string(from: date), xp: xp)
        }
    }
}
