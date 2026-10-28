package com.evolvingtheconversation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import java.util.Date

data class TeacherProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = ""
)

data class LiveClass(
    val id: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val title: String = "",
    val meetingLink: String = "",
    val dateMillis: Long = 0L,
    val createdAt: Long = 0L
)

sealed class TeacherAuthResult {
    object Idle : TeacherAuthResult()
    object Loading : TeacherAuthResult()
    object Success : TeacherAuthResult()
    data class Error(val message: String) : TeacherAuthResult()
}

/**
 * "Teacher mode" for the CURRENT signed-in account — there is no separate
 * teacher account or registration. The person re-enters the same email +
 * password they use to log into the app to confirm it's really them, then
 * gets access to add classes under their own uid.
 *
 * Important: reauthenticate() does NOT change who's signed in — it just
 * proves they still know the password for the account already active. So,
 * unlike an earlier version of this feature, nothing here ever swaps or
 * signs out the app's FirebaseAuth session. Exiting teacher mode is just
 * clearing local state.
 */
class TeacherViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    var authState by mutableStateOf<TeacherAuthResult>(TeacherAuthResult.Idle)
        private set

    var currentTeacher by mutableStateOf<TeacherProfile?>(null)
        private set

    fun resetState() {
        authState = TeacherAuthResult.Idle
    }

    fun signInAsTeacher(name: String, email: String, password: String) {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()
        val user = auth.currentUser
        val accountEmail = user?.email

        if (cleanName.isBlank() || cleanEmail.isBlank() || cleanPassword.isBlank()) {
            authState = TeacherAuthResult.Error("Please fill in every field")
            return
        }
        if (user == null || accountEmail == null) {
            authState = TeacherAuthResult.Error("You need to be logged in to the app first")
            return
        }
        if (!cleanEmail.equals(accountEmail, ignoreCase = true)) {
            authState = TeacherAuthResult.Error("Your email/password doesn't match the account you're logged in with")
            return
        }

        authState = TeacherAuthResult.Loading
        val credential = EmailAuthProvider.getCredential(accountEmail, cleanPassword)
        user.reauthenticate(credential)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    authState = TeacherAuthResult.Error("Your email/password doesn't match the account you're logged in with")
                    return@addOnCompleteListener
                }

                // Firestore rules require a /teachers/{uid} doc to exist before
                // this uid can create anything in /liveClasses — make sure one
                // does, without touching anything else on the user's account.
                val profile = hashMapOf(
                    "uid" to user.uid,
                    "name" to cleanName,
                    "email" to accountEmail,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("teachers").document(user.uid)
                    .set(profile, SetOptions.merge())
                    .addOnCompleteListener { setTask ->
                        if (setTask.isSuccessful) {
                            currentTeacher = TeacherProfile(uid = user.uid, name = cleanName, email = accountEmail)
                            authState = TeacherAuthResult.Success
                        } else {
                            authState = TeacherAuthResult.Error(
                                setTask.exception?.localizedMessage ?: "Couldn't set up your teacher profile"
                            )
                        }
                    }
            }
    }

    /** Leaving "My Classes" — just clears local state. No FirebaseAuth sign-out. */
    fun exitTeacherMode() {
        currentTeacher = null
        authState = TeacherAuthResult.Idle
    }

    fun addLiveClass(title: String, meetingLink: String, dateMillis: Long, onDone: (Boolean, String?) -> Unit) {
        val teacher = currentTeacher
        if (teacher == null) {
            onDone(false, "Not signed in as a teacher")
            return
        }
        val cleanTitle = title.trim()
        val cleanLink = meetingLink.trim()
        if (cleanTitle.isBlank() || cleanLink.isBlank()) {
            onDone(false, "Title and meeting link are required")
            return
        }

        val data = hashMapOf(
            "teacherId" to teacher.uid,
            "teacherName" to teacher.name,
            "title" to cleanTitle,
            "meetingLink" to cleanLink,
            "date" to Timestamp(Date(dateMillis)),   // ← was: dateMillis (Long)
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("liveClasses").add(data)
            .addOnSuccessListener { onDone(true, null) }
            .addOnFailureListener { onDone(false, it.localizedMessage) }
    }

    fun fetchMyLiveClasses(onResult: (List<LiveClass>) -> Unit) {
        val teacher = currentTeacher ?: return onResult(emptyList())
        db.collection("liveClasses")
            .whereEqualTo("teacherId", teacher.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.documents.mapNotNull { it.toLiveClass() }.sortedBy { it.dateMillis })
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun deleteLiveClass(classId: String, onDone: (Boolean) -> Unit = {}) {
        db.collection("liveClasses").document(classId).delete()
            .addOnCompleteListener { onDone(it.isSuccessful) }
    }
}

/** Student-facing (and teacher-facing) read of every scheduled class.
 * Rules allow read to any signed-in user, so this works for students too. */
fun fetchAllLiveClasses(onResult: (List<LiveClass>) -> Unit) {
    FirebaseFirestore.getInstance().collection("liveClasses")
        .get()
        .addOnSuccessListener { snapshot -> onResult(snapshot.documents.mapNotNull { it.toLiveClass() }) }
        .addOnFailureListener { onResult(emptyList()) }
}

private fun DocumentSnapshot.toLiveClass(): LiveClass? {
    return try {
        val teacherId = getString("teacherId") ?: return null
        val dateMillis = when (val raw = get("date")) {
            is Long -> raw
            is Number -> raw.toLong()
            is com.google.firebase.Timestamp -> raw.toDate().time
            is String -> raw.toLongOrNull() ?: return null
            else -> return null
        }
        LiveClass(
            id = id,
            teacherId = teacherId,
            teacherName = getString("teacherName") ?: "",
            title = getString("title") ?: "",
            meetingLink = getString("meetingLink") ?: "",
            dateMillis = dateMillis,
            createdAt = getTimestamp("createdAt")?.toDate()?.time ?: 0L
        )
    } catch (e: Exception) {
        // A single malformed/legacy document (e.g. old test data with a
        // differently-typed field) should never take down the whole list.
        null
    }
}