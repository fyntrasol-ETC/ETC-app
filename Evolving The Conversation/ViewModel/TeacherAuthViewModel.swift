import Foundation
import FirebaseAuth
import FirebaseFirestore
import Combine

@MainActor
final class TeacherAuthViewModel: ObservableObject {
    @Published var currentTeacher: Teacher?
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let db = Firestore.firestore()
    var isTeacherSignedIn: Bool { currentTeacher != nil }

    /// Verifies the entered email/password match the currently logged-in
    /// Firebase user (via reauthenticate), then saves the teacher profile
    /// under the SAME uid — no separate account is created.
    func signInAsTeacher(name: String, email: String, password: String) async {
        errorMessage = nil

        guard let user = Auth.auth().currentUser else {
            errorMessage = "You must be logged in to the app first."; return
        }

        guard !name.trimmingCharacters(in: .whitespaces).isEmpty,
              !password.isEmpty else {
            errorMessage = "Please fill in all fields."; return
        }

        let loggedInEmail = (user.email ?? "").lowercased().trimmingCharacters(in: .whitespaces)
        let enteredEmail  = email.lowercased().trimmingCharacters(in: .whitespaces)
        guard !loggedInEmail.isEmpty, loggedInEmail == enteredEmail else {
            errorMessage = "Your email does not match the account you are logged in with."; return
        }

        // Only email/password accounts can reauthenticate this way.
        guard user.providerData.contains(where: { $0.providerID == "password" }) else {
            errorMessage = "Teacher sign-in isn't available for Google/Apple accounts yet."; return
        }

        isLoading = true
        defer { isLoading = false }

        do {
            let credential = EmailAuthProvider.credential(withEmail: loggedInEmail, password: password)
            try await user.reauthenticate(with: credential)

            let teacher = Teacher(uid: user.uid, name: name, email: loggedInEmail)
            try db.collection("teachers").document(user.uid).setData(from: teacher)
            currentTeacher = teacher
        } catch let e as NSError {
            switch AuthErrorCode(rawValue: e.code) {
            case .wrongPassword: errorMessage = "Incorrect password."
            default:              errorMessage = "Your email does not match the account you are logged in with."
            }
        }
    }

    /// Loads an existing teacher profile for the current user, if any —
    /// call on app/screen appear so a teacher doesn't have to re-verify every time.
    func loadExistingTeacherProfile() async {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        if let doc = try? await db.collection("teachers").document(uid).getDocument(),
           let teacher = try? doc.data(as: Teacher.self) {
            currentTeacher = teacher
        }
    }

    func signOutAsTeacher() {
        currentTeacher = nil
    }
}
