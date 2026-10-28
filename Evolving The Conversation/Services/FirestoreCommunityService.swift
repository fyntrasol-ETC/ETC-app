import FirebaseFirestore
import FirebaseAuth

// MARK: - Firestore Post Document
struct FirestorePost: Identifiable, Codable {
    @DocumentID var id: String?
    var authorUid: String
    var authorName: String
    var authorColorHex: String        // NEW
    var text: String
    var category: String
    var createdAt: Timestamp
    var likeCount: Int
    var commentCount: Int
    var likedByUids: [String]
    var isEdited: Bool = false   // ADD THIS

}

// MARK: - Firestore Comment Document
struct FirestoreComment: Identifiable, Codable {
    @DocumentID var id: String?
    var authorUid: String
    var authorName: String
    var authorColorHex: String        // NEW
    var text: String
    var createdAt: Timestamp
    var parentId: String?
    var likeCount: Int = 0
    var likedByUids: [String] = []
    var isEdited: Bool = false   // ADD THIS

}

enum FirestoreCommunityError: LocalizedError {
    case notSignedIn
    case textTooLong

    var errorDescription: String? {
        switch self {
        case .notSignedIn:  return "You need to be signed in to do that."
        case .textTooLong:  return "That text is too long."
        }
    }
}

struct FirestoreCommunityService {
    private static let db = Firestore.firestore()
    private static var postsRef: CollectionReference { db.collection("community_posts") }
    private static func commentsRef(postId: String) -> CollectionReference {
        postsRef.document(postId).collection("comments")
    }

    static func createPost(text: String, category: PostCategory, authorName: String) async throws {
        guard let uid = Auth.auth().currentUser?.uid else { throw FirestoreCommunityError.notSignedIn }
        guard text.count <= 2000 else { throw FirestoreCommunityError.textTooLong }

        let data: [String: Any] = [
            "authorUid": uid,
            "authorName": authorName,
            "authorColorHex": colorHex(for: uid),   // ADD THIS
            "text": text,
            "category": category.rawValue,
            "createdAt": FieldValue.serverTimestamp(),
            "likeCount": 0,
            "commentCount": 0,
            "likedByUids": [],
            "isEdited": false                       // ADD THIS
        ]
        try await postsRef.addDocument(data: data)
    }

    // ── Posts: live listener ──────────────────────────────────────
    static func listenToPosts(onChange: @escaping ([FirestorePost]) -> Void) -> ListenerRegistration {
        postsRef
            .order(by: "createdAt", descending: true)
            .addSnapshotListener { snapshot, error in
                guard let snapshot else {
                    print("listenToPosts error: \(error?.localizedDescription ?? "unknown")")
                    return
                }
                let posts = snapshot.documents.compactMap { try? $0.data(as: FirestorePost.self) }
                onChange(posts)
            }
    }

    // ── Posts: delete (rule: authorUid == auth.uid only) ──────────
    static func deletePost(postId: String) async throws {
        try await postsRef.document(postId).delete()
    }

    // ── Posts: toggle like ─────────────────────────────────────────
    // Writes ONLY likeCount + likedByUids together, matching the update
    // rule's hasOnly(['likeCount', 'likedByUids']) branch — works for
    // any signed-in user, not just the post author.
    static func toggleLike(postId: String) async throws {
        guard let uid = Auth.auth().currentUser?.uid else {
            throw FirestoreCommunityError.notSignedIn
        }
        let ref = postsRef.document(postId)

        _ = try await db.runTransaction { txn, errorPointer in
            let snapshot: DocumentSnapshot
            do {
                snapshot = try txn.getDocument(ref)
            } catch let e as NSError {
                errorPointer?.pointee = e
                return nil
            }

            var likedByUids = snapshot.data()?["likedByUids"] as? [String] ?? []
            var likeCount = snapshot.data()?["likeCount"] as? Int ?? 0

            if likedByUids.contains(uid) {
                likedByUids.removeAll { $0 == uid }
                likeCount = max(0, likeCount - 1)
            } else {
                likedByUids.append(uid)
                likeCount += 1
            }

            txn.updateData([
                "likedByUids": likedByUids,
                "likeCount": likeCount
            ], forDocument: ref)

            return nil
        }
    }

    // ── Posts: live comment count (read-only, sidesteps the commentCount
    //    write restriction described above) ─────────────────────────
    static func fetchCommentCount(postId: String) async -> Int {
        do {
            let snapshot = try await commentsRef(postId: postId).count.getAggregation(source: .server)
            return Int(truncating: snapshot.count)
        } catch {
            print("fetchCommentCount error: \(error.localizedDescription)")
            return 0
        }
    }

    static func addComment(
        postId: String,
        text: String,
        authorName: String,
        parentId: String? = nil
    ) async throws {
        guard let uid = Auth.auth().currentUser?.uid else {
            throw FirestoreCommunityError.notSignedIn
        }
        guard text.count <= 1000 else { throw FirestoreCommunityError.textTooLong }

        var data: [String: Any] = [
            "authorUid": uid,
            "authorName": authorName,
            "authorColorHex": colorHex(for: uid),   // ADD THIS
            "text": text,
            "createdAt": FieldValue.serverTimestamp(),
            "likeCount": 0,
            "likedByUids": [],
            "isEdited": false                       // ADD THIS
        ]
        if let parentId { data["parentId"] = parentId }
        try await commentsRef(postId: postId).addDocument(data: data)
    }

    // ── Comments: toggle like ───────────────────────────────────────
    // Matches the same update-rule branch as posts:
    // diff(...).affectedKeys().hasOnly(['likeCount', 'likedByUids'])
    // so it works for any signed-in user, not just the comment's author.
    static func toggleCommentLike(postId: String, commentId: String) async throws {
        guard let uid = Auth.auth().currentUser?.uid else {
            throw FirestoreCommunityError.notSignedIn
        }
        let ref = commentsRef(postId: postId).document(commentId)

        _ = try await db.runTransaction { txn, errorPointer in
            let snapshot: DocumentSnapshot
            do {
                snapshot = try txn.getDocument(ref)
            } catch let e as NSError {
                errorPointer?.pointee = e
                return nil
            }

            var likedByUids = snapshot.data()?["likedByUids"] as? [String] ?? []
            var likeCount = snapshot.data()?["likeCount"] as? Int ?? 0

            if likedByUids.contains(uid) {
                likedByUids.removeAll { $0 == uid }
                likeCount = max(0, likeCount - 1)
            } else {
                likedByUids.append(uid)
                likeCount += 1
            }

            txn.updateData([
                "likedByUids": likedByUids,
                "likeCount": likeCount
            ], forDocument: ref)

            return nil
        }
    }

    // ── Comments: live listener ────────────────────────────────────
    static func listenToComments(postId: String, onChange: @escaping ([FirestoreComment]) -> Void) -> ListenerRegistration {
        commentsRef(postId: postId)
            .order(by: "createdAt", descending: false)
            .addSnapshotListener { snapshot, error in
                guard let snapshot else {
                    print("listenToComments error: \(error?.localizedDescription ?? "unknown")")
                    return
                }
                let comments = snapshot.documents.compactMap { try? $0.data(as: FirestoreComment.self) }
                onChange(comments)
            }
    }
   
    static func deleteComment(postId: String, commentId: String) async throws {
        try await commentsRef(postId: postId).document(commentId).delete()
    }
    static func editPost(postId: String, newText: String) async throws {
        guard newText.count <= 2000 else { throw FirestoreCommunityError.textTooLong }
        try await postsRef.document(postId).updateData([
            "text": newText,
            "isEdited": true    // ADD THIS
        ])
    }

    static func editComment(postId: String, commentId: String, newText: String) async throws {
        guard newText.count <= 1000 else { throw FirestoreCommunityError.textTooLong }
        try await commentsRef(postId: postId).document(commentId).updateData([
            "text": newText,
            "isEdited": true    // ADD THIS
        ])
    }
}
