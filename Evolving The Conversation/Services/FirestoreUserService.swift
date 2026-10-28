import FirebaseFirestore
import FirebaseAuth

struct FirestoreUserService {
    static func ensureUserDocument(for user: AuthUser) async {
        let db  = Firestore.firestore()
        let ref = db.collection("users").document(user.uid)

        do {
            let snapshot = try await ref.getDocument()
            if !snapshot.exists {
                let data: [String: Any] = [
                    "name": user.displayName ?? "",
                    "email": user.email ?? "",
                    "createdAt": FieldValue.serverTimestamp(),
                    "xp": 0,
                    "streak": 0,
                    "isPremium": false,
                    "premiumPlan": NSNull()
                ]
                try await ref.setData(data)
            }
        } catch {
            print("ensureUserDocument failed: \(error.localizedDescription)")
        }
    }
}
