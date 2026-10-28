import SwiftUI
import FirebaseAuth
import FirebaseFirestore
import Combine

struct EarnedCertificate: Identifiable {
    var id: String { courseId }   // courseId doubles as the Firestore doc id
    let courseId: String
    let courseTitle: String
    let earnedAt: Date
}

@MainActor
final class CertificateService: ObservableObject {
    static let shared = CertificateService()
    private init() {}

    // courseId -> record, for every certificate Firestore confirms is unlocked
    @Published var earnedCertificates: [String: EarnedCertificate] = [:]

    // Set briefly whenever a NEW certificate is unlocked, so a view can show a toast.
    @Published var justUnlocked: EarnedCertificate? = nil

    private var certListener: ListenerRegistration?
    private var progressCancellable: AnyCancellable?
    private let db = Firestore.firestore()

    /// Watches this user's certificates collection AND watches
    /// LessonProgressService's enrollments, so a certificate is written
    /// automatically the moment a course hits 100%.
    func start() {
        guard let uid = Auth.auth().currentUser?.uid else { return }

        certListener?.remove()
        let ref = db.collection("users").document(uid).collection("certificates")
        certListener = ref.addSnapshotListener { [weak self] snapshot, _ in
            guard let self, let docs = snapshot?.documents else { return }
            var result: [String: EarnedCertificate] = [:]
            for doc in docs {
                let data = doc.data()
                let courseId = doc.documentID
                let title = data["courseTitle"] as? String ?? "Course"
                let ts = (data["earnedAt"] as? Timestamp)?.dateValue() ?? Date()
                result[courseId] = EarnedCertificate(courseId: courseId, courseTitle: title, earnedAt: ts)
            }
            self.earnedCertificates = result
        }

        progressCancellable?.cancel()
        progressCancellable = LessonProgressService.shared.$enrollments
            .sink { [weak self] enrollments in
                self?.checkForNewCertificates(enrollments)
            }
    }

    /// Compares live progress against CourseCatalog; any fully-completed
    /// course that doesn't already have a certificate doc gets one written.
    private func checkForNewCertificates(_ enrollments: [String: Enrollment]) {
        guard let uid = Auth.auth().currentUser?.uid else { return }

        for course in CourseCatalog.all {
            guard let enrollment = enrollments[course.id] else { continue }
            guard enrollment.progress(for: course) >= 1.0 else { continue }
            guard earnedCertificates[course.id] == nil else { continue }   // already have it

            let ref = db.collection("users").document(uid)
                .collection("certificates").document(course.id)

            // Double-check directly against Firestore before creating, so a
            // stale local cache can't cause a duplicate-write attempt.
            ref.getDocument { [weak self] snapshot, _ in
                guard let self else { return }
                if snapshot?.exists == true { return }

                let record = EarnedCertificate(courseId: course.id, courseTitle: course.title, earnedAt: Date())
                ref.setData([
                    "courseTitle": course.title,
                    "earnedAt": FieldValue.serverTimestamp()
                ]) { error in
                    if error == nil {
                        Task { @MainActor in self.justUnlocked = record }
                    } else {
                        print("⚠️ Failed to save certificate: \(error!.localizedDescription)")
                    }
                }
            }
        }
    }

    func dismissToast() {
        justUnlocked = nil
    }

    func reset() {
        certListener?.remove()
        certListener = nil
        progressCancellable?.cancel()
        progressCancellable = nil
        earnedCertificates = [:]
        justUnlocked = nil
    }
}
