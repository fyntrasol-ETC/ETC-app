package com.evolvingtheconversation.app.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

private const val TAG = "CommunityViewModel"

class CommunityViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val postsRef = db.collection("community_posts")

    private fun requireUid(): String? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) Log.e(TAG, "Blocked write: no authenticated user.")
        return uid
    }

    fun isMine(authorUid: String): Boolean =
        authorUid == FirebaseAuth.getInstance().currentUser?.uid

    fun isLikedByMe(post: CommunityPost): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        return uid in post.likedByUids
    }

    fun isCommentLikedByMe(comment: CommentItem): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        return uid in comment.likedByUids
    }

    private val _posts = mutableStateListOf<CommunityPost>()
    val posts: List<CommunityPost> get() = _posts

    // Live, top-level-only comment counts keyed by postId. Backed by a
    // real-time snapshot listener on a count() aggregate query (see
    // startListeningToCommentCount below) — NOT a one-shot fetch. That
    // matters: a one-shot query only updates when *this device* triggers
    // a refresh, so it stays correct for your own writes but goes stale
    // the moment someone else — another account, iOS, another Android
    // device — adds or deletes a comment on the same post. A live listener
    // picks up any change from any source automatically.
    private val _commentCounts = mutableStateMapOf<String, Int>()
    val commentCounts: Map<String, Int> get() = _commentCounts

    private var postsListener: ListenerRegistration? = null
    private val commentListeners = mutableMapOf<String, ListenerRegistration>()
    private val commentCountListeners = mutableMapOf<String, ListenerRegistration>()

    init {
        postsListener = postsRef
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Posts listener failed", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                val fresh = snapshot.documents.mapNotNull { doc ->
                    val existing = _posts.find { it.id == doc.id }
                    doc.toObject(CommunityPost::class.java)
                        ?.copy(id = doc.id, comments = existing?.comments ?: emptyList())
                }
                _posts.clear()
                _posts.addAll(fresh)

                // Start a count listener the first time we see each post.
                // containsKey guard means we don't re-attach a listener every
                // time any post changes (e.g. someone likes a post).
                fresh.forEach { post ->
                    if (!commentCountListeners.containsKey(post.id)) {
                        startListeningToCommentCount(post.id)
                    }
                }
            }
    }

    override fun onCleared() {
        postsListener?.remove()
        commentListeners.values.forEach { it.remove() }
        commentCountListeners.values.forEach { it.remove() }
        super.onCleared()
    }
    // Counts every document in the comments subcollection — top-level comments
// AND replies together — with no field filter. The old whereEqualTo("parentId",
// null) filter broke here because iOS omits the parentId key entirely on
// top-level comments (vs Android explicitly writing null), and Firestore's
// whereEqualTo(field, null) only matches an explicit null, not a missing
// field — so every iOS-created top-level comment was silently excluded.
// Removing the filter fixes that mismatch and also gives the "total
// comments + replies" count, matching iOS's fetchCommentCount (which
// already counts everything, unfiltered).

    private fun startListeningToCommentCount(postId: String) {
        val reg = postsRef.document(postId).collection("comments")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Comment count listener failed for $postId", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    _commentCounts[postId] = snapshot.size()
                }
            }
        commentCountListeners[postId] = reg
    }

    fun addPost(text: String, category: PostCategory) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val uid = requireUid() ?: return

        val post = hashMapOf(
            "authorUid" to uid,
            "authorName" to currentUserDisplayName(),
            "authorColorHex" to colorHexForUid(uid),
            "text" to trimmed,
            "category" to category.firestoreValue,
            "createdAt" to FieldValue.serverTimestamp(),
            "likeCount" to 0,
            "commentCount" to 0,
            "likedByUids" to emptyList<String>(),
            "isEdited" to false
        )
        postsRef.add(post)
            .addOnFailureListener { Log.e(TAG, "addPost failed", it) }
    }

    fun editPost(postId: String, newText: String) {
        val trimmed = newText.trim()
        if (trimmed.isEmpty()) return
        val post = _posts.find { it.id == postId } ?: return
        if (!isMine(post.authorUid)) return

        postsRef.document(postId)
            .update(mapOf("text" to trimmed, "isEdited" to true))
            .addOnFailureListener { Log.e(TAG, "editPost failed", it) }
    }

    fun deletePost(postId: String) {
        val post = _posts.find { it.id == postId } ?: return
        if (!isMine(post.authorUid)) return
        postsRef.document(postId).delete()
            .addOnFailureListener { Log.e(TAG, "deletePost failed", it) }
    }

    fun toggleLike(postId: String) {
        val uid = requireUid() ?: return
        val post = _posts.find { it.id == postId } ?: return
        val alreadyLiked = uid in post.likedByUids

        postsRef.document(postId).update(
            mapOf(
                "likedByUids" to if (alreadyLiked) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid),
                "likeCount" to FieldValue.increment(if (alreadyLiked) -1L else 1L)
            )
        ).addOnFailureListener { Log.e(TAG, "toggleLike failed", it) }
    }

    // ── Comments (and replies) ─────────────────────────────────────────

    fun startListeningToComments(postId: String) {
        if (commentListeners.containsKey(postId)) return
        val reg = postsRef.document(postId).collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Comments listener failed for $postId", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                val comments = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CommentItem::class.java)?.copy(id = doc.id)
                }
                val index = _posts.indexOfFirst { it.id == postId }
                if (index != -1) _posts[index] = _posts[index].copy(comments = comments)
            }
        commentListeners[postId] = reg
    }

    fun stopListeningToComments(postId: String) {
        commentListeners.remove(postId)?.remove()
    }

    fun addComment(postId: String, text: String, parentId: String? = null) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val uid = requireUid() ?: return
        val isReply = parentId != null

        Log.d(TAG, "addComment: postId=$postId parentId=$parentId isReply=$isReply")

        val comment = hashMapOf(
            "authorUid" to uid,
            "authorName" to currentUserDisplayName(),
            "authorColorHex" to colorHexForUid(uid),
            "text" to trimmed,
            "createdAt" to FieldValue.serverTimestamp(),
            "isEdited" to false,
            "parentId" to parentId,
            "likeCount" to 0,
            "likedByUids" to emptyList<String>()
        )

        // No manual count refresh needed anymore — the live count listener
        // (startListeningToCommentCount) picks this up automatically, for
        // this device and everyone else's.
        postsRef.document(postId).collection("comments").add(comment)
            .addOnFailureListener { Log.e(TAG, "addComment failed", it) }
    }

    fun editComment(postId: String, commentId: String, newText: String) {
        val trimmed = newText.trim()
        if (trimmed.isEmpty()) return
        postsRef.document(postId).collection("comments").document(commentId)
            .update(mapOf("text" to trimmed, "isEdited" to true))
            .addOnFailureListener { Log.e(TAG, "editComment failed", it) }
    }

    fun deleteComment(postId: String, commentId: String) {
        postsRef.document(postId).collection("comments").document(commentId).delete()
            .addOnFailureListener { Log.e(TAG, "deleteComment failed", it) }
    }

    fun toggleCommentLike(postId: String, commentId: String) {
        val uid = requireUid() ?: return
        val post = _posts.find { it.id == postId } ?: return
        val comment = post.comments.find { it.id == commentId } ?: return
        val alreadyLiked = uid in comment.likedByUids

        postsRef.document(postId).collection("comments").document(commentId).update(
            mapOf(
                "likedByUids" to if (alreadyLiked) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid),
                "likeCount" to FieldValue.increment(if (alreadyLiked) -1L else 1L)
            )
        ).addOnFailureListener { Log.e(TAG, "toggleCommentLike failed", it) }
    }
}