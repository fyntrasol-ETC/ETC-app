package com.evolvingtheconversation.app.ai

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * One saved turn of the AI tutor conversation.
 * Mirrors the flat schema in Firestore rules: text / isUser / createdAt.
 */
data class StoredMessage(
    val id: String,
    val text: String,
    val isUser: Boolean
)

/**
 * Reads and writes users/{uid}/aiConversationMessages.
 *
 * This is the persistence layer that makes the AI Tutor conversation survive
 * leaving the screen, backgrounding the app, or restarting the process —
 * the screen never holds the "real" conversation, Firestore does.
 */
object AIConversationService {

    private val db = FirebaseFirestore.getInstance()

    private fun messagesCollection(uid: String) =
        db.collection("users").document(uid).collection("aiConversationMessages")

    /**
     * One-time fetch, oldest first. Used to:
     *  - populate the screen instantly on open (before the live listener catches up)
     *  - seed the Gemini chat session with prior context
     */
    suspend fun fetchHistory(uid: String, limit: Long = 40): List<StoredMessage> {
        val snapshot = messagesCollection(uid)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(limit)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val text = doc.getString("text") ?: return@mapNotNull null
            val isUser = doc.getBoolean("isUser") ?: false
            StoredMessage(id = doc.id, text = text, isUser = isUser)
        }
    }

    /**
     * Live updates for the conversation. The screen should render straight from this —
     * it's what keeps the UI, Firestore, and (after re-seeding) the AI's memory all in sync.
     */
    fun listenToMessages(uid: String): Flow<List<StoredMessage>> = callbackFlow {
        val registration: ListenerRegistration = messagesCollection(uid)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    val text = doc.getString("text") ?: return@mapNotNull null
                    val isUser = doc.getBoolean("isUser") ?: false
                    StoredMessage(id = doc.id, text = text, isUser = isUser)
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveMessage(uid: String, text: String, isUser: Boolean) {
        val data = hashMapOf(
            "text" to text,
            "isUser" to isUser,
            "createdAt" to FieldValue.serverTimestamp()
        )
        messagesCollection(uid).add(data).await()
    }
}