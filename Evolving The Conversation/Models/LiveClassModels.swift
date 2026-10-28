import Foundation
import FirebaseFirestore

struct Teacher: Codable, Equatable {
    let uid: String
    let name: String
    let email: String
}

struct LiveClass: Identifiable, Codable, Equatable {
    @DocumentID var id: String?
    let teacherId: String
    let teacherName: String
    let title: String
    let meetingLink: String
    let date: Date
}
