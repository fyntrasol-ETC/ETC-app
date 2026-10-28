import FirebaseFirestore

/// Handles persistence of the AI Conversation chat history.
/// Path: users/{uid}/aiConversationMessages/{messageId}
final class AIConversationService {
    private let db = Firestore.firestore()

    private func collection(for uid: String) -> CollectionReference {
        db.collection("users").document(uid).collection("aiConversationMessages")
    }

    /// One-time fetch, ordered oldest → newest. Useful for seeding
    /// the Gemini chat session's history when the screen first appears.
    func fetchMessages(uid: String) async throws -> [AIMessage] {
        let snapshot = try await collection(for: uid)
            .order(by: "createdAt", descending: false)
            .getDocuments()

        return snapshot.documents.compactMap { AIMessage(id: $0.documentID, data: $0.data()) }
    }

    /// Live listener — keeps the conversation in sync in real time,
    /// including if the same account is open on another device (e.g. Android).
    /// Call `.remove()` on the returned registration when the screen disappears.
    func listen(uid: String, onChange: @escaping ([AIMessage]) -> Void) -> ListenerRegistration {
        collection(for: uid)
            .order(by: "createdAt", descending: false)
            .addSnapshotListener { snapshot, error in
                guard let documents = snapshot?.documents else {
                    if let error { print("AIConversationService listen error: \(error.localizedDescription)") }
                    return
                }
                let messages = documents.compactMap { AIMessage(id: $0.documentID, data: $0.data()) }
                onChange(messages)
            }
    }

    /// Appends a single message to the conversation.
    @discardableResult
    func addMessage(uid: String, text: String, isUser: Bool) async throws -> String {
        let ref = collection(for: uid).document()
        try await ref.setData([
            "text": text,
            "isUser": isUser,
            "createdAt": FieldValue.serverTimestamp()
        ])
        return ref.documentID
    }

    /// Optional: lets a user start a fresh conversation from scratch.
    func clearConversation(uid: String) async throws {
        let snapshot = try await collection(for: uid).getDocuments()
        guard !snapshot.documents.isEmpty else { return }
        let batch = db.batch()
        snapshot.documents.forEach { batch.deleteDocument($0.reference) }
        try await batch.commit()
    }
}
