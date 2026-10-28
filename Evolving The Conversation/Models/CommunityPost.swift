import SwiftUI
import Combine
import FirebaseFirestore
import FirebaseAuth

// MARK: - Post Category
enum PostCategory: String, Codable, CaseIterable {
    case question = "Question"
    case tip      = "Tip"
    case other    = "Other"

    var icon: String {
        switch self {
        case .question: return "questionmark.circle.fill"
        case .tip:       return "lightbulb.fill"
        case .other:     return "bubble.left.fill"
        }
    }

    var color: Color {
        switch self {
        case .question: return AppTheme.Colors.orangeFallback
        case .tip:       return AppTheme.Colors.greenFallback
        case .other:     return AppTheme.Colors.purpleFallback
        }
    }
}

// MARK: - UI Post Model (mapped live from FirestorePost)
struct CommunityPost: Identifiable, Equatable {
    let id: String
    var authorUid: String
    var name: String
    var createdAt: Date
    var body: String
    var category: PostCategory
    var likeCount: Int
    var likedByMe: Bool
    var commentCount: Int
    var isMine: Bool
    var isEdited: Bool = false   // ADD THIS

    var initial: String { String(name.prefix(1)).uppercased() }

    // Deterministic per-author color since Firestore doesn't store one.
    var authorColorHex: String   // NEW — replaces avatarColorName/avatarColor logic

    var avatarColor: Color {
        Color(hex: authorColorHex) ?? AppTheme.Colors.primaryFallback
    }

    var time: String { Self.relativeTime(from: createdAt) }

    static func relativeTime(from date: Date) -> String {
        let s = Date().timeIntervalSince(date)
        if s < 60 { return "Just now" }
        if s < 3600 { return "\(Int(s / 60))m ago" }
        if s < 86400 { return "\(Int(s / 3600))h ago" }
        if s < 604800 { return "\(Int(s / 86400))d ago" }
        let f = DateFormatter()
        f.dateFormat = "MMM d"
        return f.string(from: date)
    }
}
struct CommunityComment: Identifiable, Equatable {
    let id: String
    var authorUid: String
    var authorName: String
    var text: String
    var timestamp: Date
    var parentId: String?
    var likeCount: Int
    var likedByMe: Bool
    var isMine: Bool
    var authorColorHex: String    // ADD THIS
    var isEdited: Bool = false    // ADD THIS

    var authorInitial: String { String(authorName.prefix(1)).uppercased() }
}

// MARK: - Firestore-backed Store
@MainActor
final class CommunityStore: ObservableObject {
    static let shared = CommunityStore()

    @Published var posts: [CommunityPost] = []

    /// Live comment counts, keyed by postId. commentCount on the post doc
    /// itself can only legally be written by the post's author (per your
    /// update rule), so it's unreliable for anyone else's comments. We
    /// fetch the real count from the comments subcollection instead —
    /// that's a read, so it isn't blocked by the rule.
    private var commentCounts: [String: Int] = [:]

    /// Raw docs from the last snapshot, kept around so we can re-derive
    /// `likedByMe` / `isMine` when the signed-in user changes WITHOUT
    /// waiting for a new Firestore snapshot (which won't fire just
    /// because you switched accounts — the post data itself hasn't changed).
    private var latestFirestorePosts: [FirestorePost] = []

    private var postsListener: ListenerRegistration?
    private var authListenerHandle: AuthStateDidChangeListenerHandle?

    private init() {
        startListening()
        // Re-derive per-user fields (likedByMe, isMine) any time the
        // signed-in user changes — this is what fixes the "like state
        // carries over from the previous account" bug.
        authListenerHandle = Auth.auth().addStateDidChangeListener { [weak self] _, _ in
            self?.remapPostsForCurrentUser()
        }
    }

    deinit {
        postsListener?.remove()
        if let authListenerHandle {
            Auth.auth().removeStateDidChangeListener(authListenerHandle)
        }
    }

    private var currentUid: String? { Auth.auth().currentUser?.uid }

    // MARK: Listening
    func startListening() {
        postsListener?.remove()
        postsListener = FirestoreCommunityService.listenToPosts { [weak self] firestorePosts in
            guard let self else { return }
            self.latestFirestorePosts = firestorePosts
            self.remapPostsForCurrentUser()

            // Backfill live comment counts for posts we haven't checked yet.
            for fp in firestorePosts {
                guard let id = fp.id, self.commentCounts[id] == nil else { continue }
                Task { await self.refreshCommentCount(postId: id) }
            }
        }
    }

    private func remapPostsForCurrentUser() {
        let uid = currentUid
        posts = latestFirestorePosts.map { fp in
            let id = fp.id ?? UUID().uuidString
            return CommunityPost(
                id: id,
                authorUid: fp.authorUid,
                name: fp.authorName,
                createdAt: fp.createdAt.dateValue(),
                body: fp.text,
                category: PostCategory(rawValue: fp.category) ?? .other,
                likeCount: fp.likeCount,
                likedByMe: uid.map { fp.likedByUids.contains($0) } ?? false,
                commentCount: commentCounts[id] ?? fp.commentCount,
                isMine: uid == fp.authorUid,
                isEdited: fp.isEdited,              // now matches struct order
                authorColorHex: fp.authorColorHex   // now matches struct order
            )
        }
    }

    func refreshCommentCount(postId: String) async {
        let count = await FirestoreCommunityService.fetchCommentCount(postId: postId)
        commentCounts[postId] = count
        if let idx = posts.firstIndex(where: { $0.id == postId }) {
            posts[idx].commentCount = count
        }
    }

    // MARK: Posts
    func addPost(body: String, category: PostCategory, authorName: String) {
        Task {
            do {
                try await FirestoreCommunityService.createPost(
                    text: body,
                    category: category,
                    authorName: authorName
                )
            } catch {
                print("addPost failed: \(error.localizedDescription)")
            }
        }
    }

    func toggleLike(postId: String) {
        // Optimistic UI update — the snapshot listener reconciles the
        // real value a moment later regardless of success/failure.
        if let idx = posts.firstIndex(where: { $0.id == postId }) {
            posts[idx].likedByMe.toggle()
            posts[idx].likeCount += posts[idx].likedByMe ? 1 : -1
        }
        Task {
            do {
                try await FirestoreCommunityService.toggleLike(postId: postId)
            } catch {
                print("toggleLike failed: \(error.localizedDescription)")
            }
        }
    }
    func editPost(postId: String, newText: String) {
        // Optimistic local update — the snapshot listener will reconcile
        // shortly after with the real value, same pattern as toggleLike.
        if let idx = posts.firstIndex(where: { $0.id == postId }) {
            posts[idx].body = newText
            posts[idx].isEdited = true
        }
        Task {
            do {
                try await FirestoreCommunityService.editPost(postId: postId, newText: newText)
            } catch {
                print("editPost failed: \(error.localizedDescription)")
            }
        }
    }
    func deletePost(postId: String) {
        Task {
            do {
                try await FirestoreCommunityService.deletePost(postId: postId)
            } catch {
                print("deletePost failed: \(error.localizedDescription)")
            }
        }
    }
}
