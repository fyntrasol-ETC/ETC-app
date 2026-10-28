package com.evolvingtheconversation.app.viewmodel

import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Exclude

fun currentUserDisplayName(): String {
    val user = FirebaseAuth.getInstance().currentUser
    return user?.displayName?.takeIf { it.isNotBlank() }
        ?: user?.email?.substringBefore("@")
        ?: "You"
}

fun currentUserInitial(): String =
    currentUserDisplayName().firstOrNull()?.uppercaseChar()?.toString() ?: "U"

// Used only for the "you" avatar in the comment-input row — never written
// to Firestore, so it's fine for this one spot to stay a Compose Color.
val CurrentUserColor = Color(0xFF1E5EF3)

// Deterministic color per uid so it renders consistently on both platforms
// without needing to store a Color object (Firestore can't hold one).
private val PALETTE = listOf("#2979FF", "#34A853", "#F5A623", "#7C4DFF", "#E53935", "#00ACC1")
fun colorHexForUid(uid: String): String =
    PALETTE[Math.abs(uid.hashCode()) % PALETTE.size]

enum class PostCategory(val label: String, val firestoreValue: String) {
    QUESTION("Question", "questions"),
    TIP("Tip", "tips"),
    OTHER("Other", "general");

    companion object {
        fun fromFirestore(value: String) =
            values().find { it.firestoreValue == value } ?: OTHER
    }
}

// NOTE: no-arg defaults are required for Firestore's toObject() mapping.
// CHANGED: replyToCommentId (String, "" = no parent) -> parentId (String?, null = no parent).
// This matches what iOS sends now — a real null on top-level comments instead of "".
data class CommentItem(
    @get:Exclude var id: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorColorHex: String = "#1E5EF3",
    val text: String = "",
    val createdAt: Timestamp? = null,
    val isEdited: Boolean = false,
    val parentId: String? = null,
    val likeCount: Int = 0,
    val likedByUids: List<String> = emptyList()
) {
    @get:Exclude
    val isReply: Boolean get() = parentId != null
}

data class CommunityPost(
    @get:Exclude var id: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorColorHex: String = "#1E5EF3",
    val text: String = "",
    val category: String = "general",
    val createdAt: Timestamp? = null,
    val likeCount: Int = 0,
    // NOTE: this field still exists on the doc (and is still written as 0 on create,
    // since the security rules require it at creation time), but it is no longer
    // mutated afterward. Don't trust it for display — see CommunityViewModel.commentCounts,
    // which computes the live count via a query instead. Kept here only so `toObject()`
    // mapping doesn't break on old/existing documents.
    val commentCount: Int = 0,
    val likedByUids: List<String> = emptyList(),
    val isEdited: Boolean = false,
    @get:Exclude var comments: List<CommentItem> = emptyList() // populated separately, never written to Firestore
) {
    @get:Exclude
    val authorInitial: String get() = authorName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"

    // Accurate only once comments have been loaded (i.e. after startListeningToComments
    // has fired at least once for this post — typically when the bottom sheet is open).
    // For the feed/list screen before that happens, use ViewModel.commentCounts[post.id] instead.
    @get:Exclude
    val loadedTopLevelCommentCount: Int get() = comments.count { it.parentId == null }
}

fun formatTimeAgo(ts: Timestamp?): String {
    if (ts == null) return "just now"
    val diffMs = System.currentTimeMillis() - ts.toDate().time
    val minutes = diffMs / (60 * 1000)
    val hours = diffMs / (60 * 60 * 1000)
    val days = diffMs / (24 * 60 * 60 * 1000)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}