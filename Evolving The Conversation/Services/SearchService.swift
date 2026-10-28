import FirebaseAuth
import FirebaseFirestore

// MARK: - Search Result

struct SearchResult: Identifiable {
    enum Kind { case course, lesson }

    let id: String
    let kind: Kind
    let course: Course
    let lesson: CourseLesson?   // nil when kind == .course
}

// MARK: - App Screen Destination
//
// The static, always-available screens surfaced from the Profile tab menu
// (My Progress, Certificates, Premium Plans, Settings, Help Center) and
// the Practice tab menu (Speaking/Listening/Writing Practice, Grammar,
// Vocabulary, Quiz Center, AI Tutor). Unlike courses/lessons these don't
// belong to CourseCatalog, so they're modeled separately and matched the
// same way (prefix match on the name).
enum AppScreenDestination: String, CaseIterable, Identifiable {
    // Profile tab
    case myProgress     = "My Progress"
    case certificates   = "Certificates"
    case premiumPlans   = "Premium Plans"
    case settings       = "Settings"
    case helpCenter     = "Help Center"
    case changePassword = "Change Password"
    case deleteAccount  = "Delete Account"

    // Practice tab
    case speakingPractice  = "Speaking Practice"
    case listeningPractice = "Listening Practice"
    case writingPractice   = "Writing Practice"
    case grammar            = "Grammar"
    case vocabulary          = "Vocabulary"
    case quizCenter          = "Quiz Center"
    case aiTutor             = "AI Tutor"

    // Home tab
    case dailyChallenge = "Daily Challenge"
    case liveClasses    = "Live Classes"

    var id: String { rawValue }

    var icon: String {
        switch self {
        case .myProgress:        return "chart.bar.fill"
        case .certificates:      return "rosette"
        case .premiumPlans:      return "trophy.fill"
        case .settings:          return "gearshape.fill"
        case .helpCenter:        return "questionmark.circle.fill"
        case .changePassword:    return "lock.rotation"
        case .deleteAccount:     return "person.crop.circle.badge.minus"
        case .speakingPractice:  return "mic"
        case .listeningPractice: return "headphones"
        case .writingPractice:   return "pencil"
        case .grammar:           return "doc.text.fill"
        case .vocabulary:        return "sparkles"
        case .quizCenter:        return "target"
        case .aiTutor:           return "waveform"
        case .dailyChallenge:    return "flame.fill"
        case .liveClasses:       return "video.fill"
        }
    }
}
// MARK: - Search Service
//
// Two independent jobs:
//  1. Search the (static, local) course catalog — courses and lessons are
//     just data in CourseCatalog.all, so this never needs a network call.
//  2. Track the user's last 5 search terms in Firestore, synced to their
//     account: users/{uid}/recentSearches/{term}. Using the (lowercased)
//     term itself as the doc ID means re-searching something just bumps
//     its timestamp instead of creating a duplicate entry.
@MainActor
final class SearchService {
    static let shared = SearchService()
    private init() {}

    private let db = Firestore.firestore()

    // MARK: - Matching
    //
    // "Starts with" rather than "contains" — typing "e" should surface
    // things like "English Grammar Pro" or "Everyday conversations", not
    // every string that happens to contain the letter e anywhere. Checks
    // both the full string and each individual word so a match on any
    // word's start counts (e.g. "grammar" matches "English Grammar Pro").
    private func matchesPrefix(_ text: String, query: String) -> Bool {
        let lower = text.lowercased()
        if lower.hasPrefix(query) { return true }
        return lower.split(separator: " ").contains { $0.hasPrefix(query) }
    }

    // MARK: - Catalog search

    func search(_ query: String) -> [SearchResult] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !q.isEmpty else { return [] }

        var results: [SearchResult] = []

        for course in CourseCatalog.all {
            let courseMatches =
                matchesPrefix(course.title, query: q) ||
                matchesPrefix(course.author, query: q) ||
                matchesPrefix(course.category, query: q)

            if courseMatches {
                results.append(SearchResult(id: "course_\(course.id)", kind: .course, course: course, lesson: nil))
            }

            for lesson in course.lessons where matchesPrefix(lesson.title, query: q) {
                results.append(SearchResult(id: "lesson_\(course.id)_\(lesson.id)", kind: .lesson, course: course, lesson: lesson))
            }
        }

        return results
    }

    // MARK: - Screen search
    //
    // Matches against the app's static menu screens — both the Profile
    // tab (My Progress, Certificates, Premium Plans, Settings, Help
    // Center) and the Practice tab (Speaking/Listening/Writing Practice,
    // Grammar, Vocabulary, Quiz Center, AI Tutor) — using the same prefix
    // rule as the catalog search above.
    func searchScreens(_ query: String) -> [AppScreenDestination] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !q.isEmpty else { return [] }
        return AppScreenDestination.allCases.filter { matchesPrefix($0.rawValue, query: q) }
    }

    // MARK: - Recent searches (Firestore, synced to the account)

    private func docId(for term: String) -> String {
        term.trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
            .replacingOccurrences(of: "/", with: "-")
    }

    /// Call once per real search — either when the user submits the field
    /// (return key) or taps a result. Safe to call repeatedly with the
    /// same term; it just refreshes that term's timestamp.
    func recordSearch(_ term: String) {
        let trimmed = term.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, let uid = Auth.auth().currentUser?.uid else { return }

        db.collection("users").document(uid)
            .collection("recentSearches").document(docId(for: trimmed))
            .setData([
                "term": trimmed,
                "timestamp": FieldValue.serverTimestamp()
            ], merge: true) { error in
                if let error {
                    print("⚠️ recordSearch failed: \(error.localizedDescription)")
                }
            }
    }

    /// Last 5 searches, most recent first.
    func fetchRecentSearches() async -> [String] {
        guard let uid = Auth.auth().currentUser?.uid else { return [] }

        guard let snapshot = try? await db.collection("users").document(uid)
            .collection("recentSearches")
            .order(by: "timestamp", descending: true)
            .limit(to: 5)
            .getDocuments()
        else { return [] }

        return snapshot.documents.compactMap { $0.get("term") as? String }
    }
}
