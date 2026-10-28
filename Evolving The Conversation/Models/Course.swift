import SwiftUI

struct Course: Identifiable, Codable, Equatable {
    let id: String                  // stable slug, e.g. "spoken_english_mastery" — used as Firestore doc id
    let title: String
    let author: String
    let duration: String
    let students: String
    let category: String
    let icon: String
    let accentColorHex: String
    let accentColorDarkHex: String
    let lessons: [CourseLesson]

    var accentColor: Color { Color(hex: accentColorHex) ?? .gray }
    var accentColorDark: Color { Color(hex: accentColorDarkHex) ?? .gray }
}

struct CourseLesson: Identifiable, Codable, Equatable {
    let id: String          // stable slug, e.g. "spoken_english_intro_1"
    let order: Int          // 0-based — drives locked/inProgress/done sequencing
    let title: String
    let duration: String
    let videoURL: String    // dummy now ("dummy://...") — PM swaps this later, zero other code changes
}

enum LessonState: String, Codable { case done, inProgress, locked }

// NOTE: Color(hex:) lives in the palette utility file (the one with
// javaHashCode/colorHex) as a failable `init?(hex:)`. Do not redeclare it
// here — a second declaration causes "Invalid redeclaration of 'init(hex:)'"
// and forces Swift to resolve to the optional-returning version everywhere,
// which is what caused the two unwrap errors above.
