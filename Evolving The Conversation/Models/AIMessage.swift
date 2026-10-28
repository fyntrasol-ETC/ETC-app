import FirebaseFirestore

/// A single turn in the AI conversation, persisted to Firestore at
/// users/{uid}/aiConversationMessages/{messageId}
///
/// Schema is intentionally flat and plain (no iOS-only types) so the
/// Android client can read/write the exact same fields:
///   text:      String
///   isUser:    Bool
///   createdAt: Timestamp (server-generated)
struct AIMessage: Identifiable, Equatable {
    let id: String
    let text: String
    let isUser: Bool
    let createdAt: Timestamp

    init?(id: String, data: [String: Any]) {
        guard
            let text = data["text"] as? String,
            let isUser = data["isUser"] as? Bool
        else { return nil }

        self.id = id
        self.text = text
        self.isUser = isUser
        self.createdAt = data["createdAt"] as? Timestamp ?? Timestamp()
    }
}
