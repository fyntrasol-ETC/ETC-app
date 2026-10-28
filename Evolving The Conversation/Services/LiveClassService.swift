import Foundation
import FirebaseFirestore
import Combine

@MainActor
final class LiveClassService: ObservableObject {
    static let shared = LiveClassService()
    private let db = Firestore.firestore()

    /// How long a class stays "live" in the hero box after its start time
    /// before handing off to the next class of the day.
    static let liveGraceWindow: TimeInterval = 5 * 60

    @Published var allClasses: [LiveClass] = []
    private var listener: ListenerRegistration?

    func listen() {
        guard listener == nil else { return }
        listener = db.collection("liveClasses")
            .order(by: "date")
            .addSnapshotListener { [weak self] snapshot, _ in
                guard let docs = snapshot?.documents else { return }
                self?.allClasses = docs.compactMap { doc in
                    do {
                        return try doc.data(as: LiveClass.self)
                    } catch {
                        print("⚠️ Failed to decode liveClass \(doc.documentID): \(error)")
                        return nil
                    }
                }
            }
    }

    func stop() {
        listener?.remove()
        listener = nil
    }
    func deleteClass(_ liveClass: LiveClass) async throws {
        guard let id = liveClass.id else { return }
        try await db.collection("liveClasses").document(id).delete()
    }
    func addClass(_ liveClass: LiveClass) async throws {
        _ = try db.collection("liveClasses").addDocument(from: liveClass)
    }

    /// Only classes from today onward — anything whose calendar day has
    /// already passed (i.e. we're past midnight of that day) drops out
    /// of every list automatically, for both students and teachers.
    var activeClasses: [LiveClass] {
        let startOfToday = Calendar.current.startOfDay(for: Date())
        return allClasses.filter { $0.date >= startOfToday }
    }

    func classes(on date: Date) -> [LiveClass] {
        let cal = Calendar.current
        return activeClasses.filter { cal.isDate($0.date, inSameDayAs: date) }.sorted { $0.date < $1.date }
    }

    /// All active (today or future) dates that have at least one class —
    /// used to mark days on the monthly calendar.
    var datesWithClasses: Set<DateComponents> {
        let cal = Calendar.current
        return Set(activeClasses.map { cal.dateComponents([.year, .month, .day], from: $0.date) })
    }

    /// Every class scheduled for today, earliest first.
    func todaysClasses(now: Date = Date()) -> [LiveClass] {
        let cal = Calendar.current
        return allClasses
            .filter { cal.isDate($0.date, inSameDayAs: now) }
            .sorted { $0.date < $1.date }
    }

    /// The class the hero box should feature right now: the earliest
    /// class *today* whose live grace window hasn't elapsed yet. This
    /// naturally covers three states as time passes:
    ///  - date > now                          → upcoming, show countdown
    ///  - now within [date, date+grace]        → live now, show "Join now"
    ///  - date + grace <= now                  → expired, move to the next one
    func featuredClass(now: Date = Date()) -> LiveClass? {
        todaysClasses(now: now).first { $0.date.addingTimeInterval(Self.liveGraceWindow) > now }
    }

    func classesForTeacher(_ teacherId: String) -> [LiveClass] {
        activeClasses.filter { $0.teacherId == teacherId }.sorted { $0.date < $1.date }
    }
}
