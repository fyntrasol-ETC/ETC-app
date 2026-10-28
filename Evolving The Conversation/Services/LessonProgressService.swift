import FirebaseFirestore
import FirebaseAuth
import Combine

struct Enrollment: Codable, Equatable {
    var completedLessonIds: [String] = []
    var lastLessonId: String? = nil

    func state(for lesson: CourseLesson, in course: Course) -> LessonState {
        if completedLessonIds.contains(lesson.id) { return .done }
        let firstIncompleteOrder = course.lessons
            .filter { !completedLessonIds.contains($0.id) }
            .map(\.order)
            .min()
        return lesson.order == firstIncompleteOrder ? .inProgress : .locked
    }

    func progress(for course: Course) -> Double {
        guard !course.lessons.isEmpty else { return 0 }
        return Double(completedLessonIds.count) / Double(course.lessons.count)
    }
}

@MainActor
final class LessonProgressService: ObservableObject {
    static let shared = LessonProgressService()
    @Published var enrollments: [String: Enrollment] = [:]   // courseId -> Enrollment

    private let db = Firestore.firestore()
    private var listener: ListenerRegistration?
    private var listeningUid: String? = nil
    private var authHandle: AuthStateDidChangeListenerHandle?

    // MUST match Android's AuthViewModel.markLessonComplete(), which calls
    // recordActivity(20, minutesSpent = 15.0, ...) with a flat estimate,
    // NOT the lesson's real video duration. If either platform switches to
    // parsing real duration, Hours will drift between iOS and Android even
    // though both write to the same stats.totalHours field. See
    // progress_sync_spec.md (referenced in AuthViewModel.recordActivity's
    // doc comment) for the shared table of estimates.
    private let lessonCompletionMinutes: Double = 15.0

    private init() {
        authHandle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            guard let self else { return }
            if let uid = user?.uid {
                self.startListening(uid: uid)
            } else {
                self.stopListening()
                self.enrollments = [:]
            }
        }
    }

    deinit {
        if let authHandle {
            Auth.auth().removeStateDidChangeListener(authHandle)
        }
    }

    private func ref(_ courseId: String) -> DocumentReference? {
        guard let uid = Auth.auth().currentUser?.uid else { return nil }
        return db.collection("users").document(uid).collection("enrollments").document(courseId)
    }

    func listenAll() {
        guard let uid = Auth.auth().currentUser?.uid else {
            stopListening()
            enrollments = [:]
            return
        }
        startListening(uid: uid)
    }

    private func startListening(uid: String) {
        if listeningUid == uid, listener != nil { return }

        stopListening()
        enrollments = [:]
        listeningUid = uid

        listener = db.collection("users").document(uid).collection("enrollments")
            .addSnapshotListener { [weak self] snapshot, _ in
                guard let self, let docs = snapshot?.documents else { return }
                guard self.listeningUid == uid else { return }

                var fresh: [String: Enrollment] = [:]
                for doc in docs {
                    fresh[doc.documentID] = Enrollment(
                        completedLessonIds: doc.data()["completedLessonIds"] as? [String] ?? [],
                        lastLessonId: doc.data()["lastLessonId"] as? String
                    )
                }
                self.enrollments = fresh
            }
    }

    func stopListening() {
        listener?.remove()
        listener = nil
        listeningUid = nil
    }

    func reset() {
        stopListening()
        enrollments = [:]
    }

    func enroll(courseId: String, firstLessonId: String) async {
        guard let ref = ref(courseId) else { return }
        let data: [String: Any] = [
            "enrolledAt": FieldValue.serverTimestamp(),
            "updatedAt": FieldValue.serverTimestamp(),
            "lastLessonId": firstLessonId,
            "completedLessonIds": FieldValue.arrayUnion([])
        ]
        try? await ref.setData(data, merge: true)

        // Matches Android's enrollInCourse(), which bumps
        // stats.coursesEnrolledCount only for a genuinely NEW enrollment
        // (it checks doc.exists() first, same as here happening only on
        // the "unlock" path which only runs once per course per user).
        if let uid = Auth.auth().currentUser?.uid {
            try? await db.collection("users").document(uid)
                .updateData(["stats.coursesEnrolledCount": FieldValue.increment(Int64(1))])
        }
    }

    /// Marks a lesson complete AND syncs `stats.totalHours` /
    /// `stats.totalLessonsCompleted` — same fields, same fixed per-lesson
    /// time estimate, and same "first completion only" guard as Android's
    /// AuthViewModel.markLessonComplete(). Reads completedLessonIds fresh
    /// from Firestore before deciding whether to increment stats, rather
    /// than trusting the caller's local UI state — this is what Android
    /// does too, and it's what actually prevents double-counting if two
    /// completions race.
    func markLessonComplete(courseId: String, lessonId: String, nextLessonId: String?) async {
        guard let ref = ref(courseId) else { return }

        let existingDoc = try? await ref.getDocument()
        let existingIds = Set(existingDoc?.data()?["completedLessonIds"] as? [String] ?? [])
        let alreadyCompleted = existingIds.contains(lessonId)

        var data: [String: Any] = [
            "completedLessonIds": FieldValue.arrayUnion([lessonId]),
            "updatedAt": FieldValue.serverTimestamp()
        ]
        if let nextLessonId { data["lastLessonId"] = nextLessonId }
        try? await ref.setData(data, merge: true)

        if !alreadyCompleted, let uid = Auth.auth().currentUser?.uid {
            let userRef = db.collection("users").document(uid)
            // FIX: setData(merge:) does NOT treat dotted string keys as nested
            // paths in the Swift SDK — it creates a literal field named
            // "stats.totalHours" as a sibling of the "stats" map, not a key
            // inside it. updateData(_:) is what actually parses dot notation
            // as a field path (same as enroll()'s "stats.coursesEnrolledCount"
            // call above, which is why that one worked correctly).
            try? await userRef.updateData([
                "stats.totalLessonsCompleted": FieldValue.increment(Int64(1)),
                "stats.totalHours": FieldValue.increment(lessonCompletionMinutes / 60.0),
                "updatedAt": FieldValue.serverTimestamp()
            ])
        }

        // optimistic local update — instant UI, no waiting on the listener round-trip
        var current = enrollments[courseId] ?? Enrollment()
        if !current.completedLessonIds.contains(lessonId) { current.completedLessonIds.append(lessonId) }
        current.lastLessonId = nextLessonId ?? current.lastLessonId
        enrollments[courseId] = current
    }}
