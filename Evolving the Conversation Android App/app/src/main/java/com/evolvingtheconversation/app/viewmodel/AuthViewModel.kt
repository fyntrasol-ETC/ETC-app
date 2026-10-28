package com.evolvingtheconversation.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.AuthResult as FirebaseAuthResult
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.evolvingtheconversation.app.data.CourseCatalog
import java.util.Calendar

sealed class AuthResult {
    object Idle : AuthResult()
    object Loading : AuthResult()
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

// ── NEW: per-course progress summary used by HomeScreen to find the most
// recently studied, not-yet-finished course. ──────────────────────────────
data class EnrollmentSummary(
    val completedLessonIds: Set<String>,
    val updatedAtMillis: Long
)

// ── Which "Skill breakdown" bar an activity should nudge on the Progress
// screen. NONE for activities that don't map to a specific skill (e.g. the
// daily app-open XP). See progress_sync_spec.md for the shared formula. ────
enum class ActivitySkill { SPEAKING, LISTENING, GRAMMAR, VOCABULARY, NONE }

// ── Progress screen read models ─────────────────────────────────────────
data class ProgressOverview(
    val xp: Int = 0,
    val streak: Int = 0,
    val totalHours: Double = 0.0,
    val totalLessonsCompleted: Int = 0,
    val coursesEnrolledCount: Int = 0,
    val certsCount: Int = 0,
    val skillBreakdown: Map<String, Int> = emptyMap() // raw points, NOT percentages
)

data class DailyActivity(
    val dateKey: String,
    val xpEarned: Int,
    val lessonsCompleted: Int,
    val minutesSpent: Double
)

data class MonthlyActivity(
    val monthKey: String,
    val xpEarned: Int,
    val lessonsCompleted: Int,
    val minutesSpent: Double
)

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    var authState by mutableStateOf<AuthResult>(AuthResult.Idle)
        private set

    val currentUser get() = auth.currentUser

    // ─────────────────────────────────────────────
    // IN-MEMORY CACHE (fixes the "Home screen stats take a moment to load
    // every time you come back to it" issue).
    //
    // The very first time a screen asks for xp/streak/enrollments in a
    // session there's nothing to show yet, so a network round-trip is
    // unavoidable. But once we've fetched it once, we keep it here and
    // hand it back INSTANTLY (synchronously, no network wait) on every
    // later call, while still kicking off a fresh fetch in the background
    // to keep it correct. So callers get called back twice: immediately
    // with cached data (if any), then again once the fresh data arrives.
    //
    // Keyed to the signed-in uid and cleared on logout, so a second user
    // signing in on the same device never briefly sees the first user's
    // cached numbers.
    // ─────────────────────────────────────────────
    private var cachedUid: String? = null
    var cachedXp by mutableStateOf<Int?>(null)
        private set
    var cachedStreak by mutableStateOf<Int?>(null)
        private set
    var cachedEnrollments by mutableStateOf<Map<String, EnrollmentSummary>?>(null)
        private set

    private fun ensureCacheForCurrentUser(uid: String) {
        if (cachedUid != uid) {
            cachedUid = uid
            cachedXp = null
            cachedStreak = null
            cachedEnrollments = null
        }
    }

    private fun clearCache() {
        cachedUid = null
        cachedXp = null
        cachedStreak = null
        cachedEnrollments = null
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────
    private fun todayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private fun dateKeyFormat(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = java.util.TimeZone.getDefault() }
    private fun monthKeyFormat(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM", Locale.US).apply { timeZone = java.util.TimeZone.getDefault() }

    private fun todayDateKey(): String = dateKeyFormat().format(Date())

    private fun yesterdayDateKey(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return dateKeyFormat().format(cal.time)
    }

    // ── Shared streak-reset check ──────────────────────────────────────────
    // recordActivity() only touches `streak` when the user actually does
    // something, so on its own it can never "notice" a missed day — the
    // stored value just sits there stale until the user's next activity
    // silently recalculates it. This helper is called from every stats read
    // (fetchUserStats for Home, fetchProgressOverview for Progress) so a
    // missed day gets reflected the moment the user reopens the app, even
    // before they've done anything today.
    //
    // Rules:
    //  - lastStreakDate == today or yesterday  -> streak is still valid, leave it.
    //  - lastStreakDate is empty (never logged an activity) -> new user, not a miss.
    //  - anything else (2+ days ago) -> a day was missed, reset streak to 0.
    //
    // Must be called inside a Firestore transaction so the reset can never
    // race with a concurrent recordActivity() write.
    private fun computeSyncedStreak(
        txn: Transaction,
        ref: DocumentReference,
        snapshot: DocumentSnapshot,
        today: String,
        yesterday: String
    ): Int {
        val lastStreakDate = snapshot.getString("lastStreakDate") ?: ""
        val storedStreak = (snapshot.getLong("streak") ?: 0L).toInt()

        val missedADay = lastStreakDate.isNotEmpty() &&
                lastStreakDate != today &&
                lastStreakDate != yesterday

        return if (missedADay && storedStreak != 0) {
            txn.update(ref, mapOf("streak" to 0))
            0
        } else {
            storedStreak
        }
    }

    private fun buildNewUserDoc(
        name: String,
        email: String,
        photoURL: String?,
        provider: String
    ): HashMap<String, Any?> = hashMapOf(
        "name" to name,
        "email" to email,
        "photoURL" to photoURL,
        "authProvider" to provider,
        "createdAt" to FieldValue.serverTimestamp(),
        "isPremium" to false,
        "premiumPlan" to null,
        "premiumExpiresAt" to null,
        "streak" to 0,
        "lastActiveDate" to FieldValue.serverTimestamp(),
        "xp" to 0,
        "settings" to mapOf(
            "darkMode" to false,
            "pushNotificationsEnabled" to true,
            "language" to "English"
        ),
        "stats" to mapOf(
            "totalHours" to 0.0,
            "totalLessonsCompleted" to 0,
            "coursesEnrolledCount" to 0,
            "certsCount" to 0
        ),
        "skillBreakdown" to mapOf(
            "speaking" to 0,
            "listening" to 0,
            "grammar" to 0,
            "vocabulary" to 0
        ),
        "dailyChallenge" to mapOf(
            "date" to todayString(),
            "goalCount" to 5,
            "progressCount" to 0,
            "xpReward" to 50
        )
    )

    private fun ensureUserDocExists(
        uid: String,
        name: String,
        email: String,
        photoURL: String?,
        provider: String,
        onDone: (Boolean, String?) -> Unit
    ) {
        val ref = db.collection("users").document(uid)
        ref.get().addOnCompleteListener { getTask ->
            if (!getTask.isSuccessful) {
                onDone(false, getTask.exception?.localizedMessage ?: "Failed to check profile")
                return@addOnCompleteListener
            }
            if (getTask.result?.exists() == true) {
                // Existing user — just mark them active, don't touch anything else.
                ref.update("lastActiveDate", FieldValue.serverTimestamp())
                    .addOnCompleteListener { updateTask ->
                        onDone(updateTask.isSuccessful, updateTask.exception?.localizedMessage)
                    }
            } else {
                ref.set(buildNewUserDoc(name, email, photoURL, provider))
                    .addOnCompleteListener { setTask ->
                        onDone(setTask.isSuccessful, setTask.exception?.localizedMessage)
                    }
            }
        }
    }

    // ─────────────────────────────────────────────
    // XP + STREAK + PROGRESS TRACKING
    // ─────────────────────────────────────────────

    /**
     * Call whenever the user does something that should award XP (lesson, quiz,
     * AI speaking turn, etc.). Always adds the XP. Also bumps the day's streak —
     * but only the FIRST time this is called on a given calendar day, so calling
     * it multiple times in one day is safe and won't over-increment the streak.
     *
     * Also feeds the Progress screen:
     *  - minutesSpent: rough estimate of time spent on this activity (see
     *    progress_sync_spec.md for the shared table of estimates — keep this
     *    in sync with the iOS app).
     *  - lessonCompleted: true only for an actual course lesson finishing —
     *    drives the "Lessons" stat and the chart's lesson count.
     *  - skill: which Skill-breakdown bar this should nudge, or NONE.
     *
     * Writes a merge+increment record to users/{uid}/activityLog/{today} and
     * users/{uid}/monthlyStats/{thisMonth} so the Progress screen's
     * Week/Month/Year chart has something to read.
     *
     * NOTE: this function itself always applies its XP/stat deltas when
     * called — it does not know whether the *caller's* action was already
     * done before. Callers that represent a one-time achievement (like
     * completing a specific lesson) must guard against re-calling this for
     * something that already happened — see markLessonComplete() below.
     */
    fun recordActivity(
        xpAmount: Int,
        minutesSpent: Double = 0.0,
        lessonCompleted: Boolean = false,
        skill: ActivitySkill = ActivitySkill.NONE,
        onDone: (Boolean) -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid ?: return onDone(false)
        val ref = db.collection("users").document(uid)
        val today = todayDateKey()
        val yesterday = yesterdayDateKey()
        val monthKey = monthKeyFormat().format(Date())

        db.runTransaction { txn ->
            val snapshot = txn.get(ref)
            val lastStreakDate = snapshot.getString("lastStreakDate") ?: ""
            val currentStreak = (snapshot.getLong("streak") ?: 0L).toInt()

            val updates = hashMapOf<String, Any>(
                "xp" to FieldValue.increment(xpAmount.toLong()),
                "stats.totalHours" to FieldValue.increment(minutesSpent / 60.0)
            )

            if (lessonCompleted) {
                updates["stats.totalLessonsCompleted"] = FieldValue.increment(1)
            }

            if (skill != ActivitySkill.NONE) {
                val field = when (skill) {
                    ActivitySkill.SPEAKING -> "skillBreakdown.speaking"
                    ActivitySkill.LISTENING -> "skillBreakdown.listening"
                    ActivitySkill.GRAMMAR -> "skillBreakdown.grammar"
                    ActivitySkill.VOCABULARY -> "skillBreakdown.vocabulary"
                    ActivitySkill.NONE -> ""
                }
                updates[field] = FieldValue.increment(xpAmount.toLong())
            }

            if (lastStreakDate != today) {
                // currentStreak here reflects whatever was last written — if a
                // stats read already zeroed it out after a missed day, this
                // correctly starts back at 1 instead of resuming the old count.
                val newStreak = if (lastStreakDate == yesterday) currentStreak + 1 else 1
                updates["streak"] = newStreak
                updates["lastStreakDate"] = today
            }

            txn.update(ref, updates)

            txn.set(
                ref.collection("activityLog").document(today),
                mapOf(
                    "xpEarned" to FieldValue.increment(xpAmount.toLong()),
                    "lessonsCompleted" to FieldValue.increment(if (lessonCompleted) 1 else 0),
                    "minutesSpent" to FieldValue.increment(minutesSpent),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )

            txn.set(
                ref.collection("monthlyStats").document(monthKey),
                mapOf(
                    "xpEarned" to FieldValue.increment(xpAmount.toLong()),
                    "lessonsCompleted" to FieldValue.increment(if (lessonCompleted) 1 else 0),
                    "minutesSpent" to FieldValue.increment(minutesSpent),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )

            null
        }.addOnCompleteListener { onDone(it.isSuccessful) }
    }

    /** +5 XP for opening the app, once per calendar day. Independent of streak —
     * this fires just for showing up, whether or not the user does an activity. */
    fun awardDailyLoginXpIfNeeded(onDone: (Boolean) -> Unit = {}) {
        val uid = auth.currentUser?.uid ?: return onDone(false)
        val ref = db.collection("users").document(uid)
        val today = todayDateKey()

        db.runTransaction { txn ->
            val snapshot = txn.get(ref)
            val lastLoginXpDate = snapshot.getString("lastLoginXpDate") ?: ""
            if (lastLoginXpDate != today) {
                txn.update(
                    ref,
                    mapOf(
                        "xp" to FieldValue.increment(5L),
                        "lastLoginXpDate" to today
                    )
                )
            }
        }.addOnCompleteListener { onDone(it.isSuccessful) }
    }

    /** For HomeScreen to display the real xp/streak instead of hardcoded numbers.
     * Cached — returns instantly from memory if we've fetched before in this
     * session, then silently refreshes from Firestore in the background.
     *
     * The refresh runs inside a transaction so it can also detect and apply a
     * missed-day streak reset (see computeSyncedStreak) atomically — this is
     * what makes a broken streak show up as soon as the user reopens the app,
     * rather than only after their next logged activity. */
    fun fetchUserStats(onResult: (xp: Int, streak: Int) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onResult(0, 0)
        ensureCacheForCurrentUser(uid)

        val cxp = cachedXp
        val cstreak = cachedStreak
        if (cxp != null && cstreak != null) {
            onResult(cxp, cstreak)
        }

        val ref = db.collection("users").document(uid)
        val today = todayDateKey()
        val yesterday = yesterdayDateKey()

        db.runTransaction { txn ->
            val snapshot = txn.get(ref)
            val xp = (snapshot.getLong("xp") ?: 0L).toInt()
            val streak = computeSyncedStreak(txn, ref, snapshot, today, yesterday)
            xp to streak
        }.addOnSuccessListener { (xp, streak) ->
            cachedXp = xp
            cachedStreak = streak
            onResult(xp, streak)
        }.addOnFailureListener {
            if (cachedXp == null || cachedStreak == null) onResult(0, 0)
        }
    }

    /** For the Progress screen's stat cards + skill breakdown bars, in one read.
     * Also runs the missed-day streak check (see computeSyncedStreak), so the
     * Progress screen's streak pill can never show a stale, already-broken
     * streak either. */
    fun fetchProgressOverview(onResult: (ProgressOverview) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onResult(ProgressOverview())
        val ref = db.collection("users").document(uid)
        val today = todayDateKey()
        val yesterday = yesterdayDateKey()

        db.runTransaction { txn ->
            val doc = txn.get(ref)
            val stats = doc.get("stats") as? Map<*, *>
            val skill = doc.get("skillBreakdown") as? Map<*, *>
            ProgressOverview(
                xp = (doc.getLong("xp") ?: 0L).toInt(),
                streak = computeSyncedStreak(txn, ref, doc, today, yesterday),
                totalHours = (stats?.get("totalHours") as? Number)?.toDouble() ?: 0.0,
                totalLessonsCompleted = (stats?.get("totalLessonsCompleted") as? Number)?.toInt() ?: 0,
                coursesEnrolledCount = (stats?.get("coursesEnrolledCount") as? Number)?.toInt() ?: 0,
                certsCount = (stats?.get("certsCount") as? Number)?.toInt() ?: 0,
                skillBreakdown = mapOf(
                    "speaking" to ((skill?.get("speaking") as? Number)?.toInt() ?: 0),
                    "listening" to ((skill?.get("listening") as? Number)?.toInt() ?: 0),
                    "grammar" to ((skill?.get("grammar") as? Number)?.toInt() ?: 0),
                    "vocabulary" to ((skill?.get("vocabulary") as? Number)?.toInt() ?: 0)
                )
            )
        }.addOnSuccessListener { onResult(it) }
            .addOnFailureListener { onResult(ProgressOverview()) }
    }

    /** Last [days] calendar days (oldest → newest), one entry per day, for the
     * Progress screen's Week/Month chart. Backed by users/{uid}/activityLog. */
    fun fetchActivityLog(days: Int, onResult: (List<DailyActivity>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onResult(emptyList())
        val fmt = dateKeyFormat()
        val cal = Calendar.getInstance()
        val keys = (0 until days).map { offset ->
            val c = cal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, -offset)
            fmt.format(c.time)
        }.reversed()

        db.collection("users").document(uid)
            .collection("activityLog")
            .whereGreaterThanOrEqualTo(FieldPath.documentId(), keys.first())
            .get()
            .addOnSuccessListener { snapshot ->
                val byKey = snapshot.documents.associateBy { it.id }
                val result = keys.map { key ->
                    val doc = byKey[key]
                    DailyActivity(
                        dateKey = key,
                        xpEarned = (doc?.getLong("xpEarned") ?: 0L).toInt(),
                        lessonsCompleted = (doc?.getLong("lessonsCompleted") ?: 0L).toInt(),
                        minutesSpent = doc?.getDouble("minutesSpent") ?: 0.0
                    )
                }
                onResult(result)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    /** Last 12 calendar months (oldest → newest), for the Progress screen's
     * Year chart. Backed by users/{uid}/monthlyStats. */
    fun fetchMonthlyActivity(onResult: (List<MonthlyActivity>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onResult(emptyList())
        val fmt = monthKeyFormat()
        val cal = Calendar.getInstance()
        val keys = (0 until 12).map { offset ->
            val c = cal.clone() as Calendar
            c.add(Calendar.MONTH, -offset)
            fmt.format(c.time)
        }.reversed()

        db.collection("users").document(uid)
            .collection("monthlyStats")
            .whereGreaterThanOrEqualTo(FieldPath.documentId(), keys.first())
            .get()
            .addOnSuccessListener { snapshot ->
                val byKey = snapshot.documents.associateBy { it.id }
                val result = keys.map { key ->
                    val doc = byKey[key]
                    MonthlyActivity(
                        monthKey = key,
                        xpEarned = (doc?.getLong("xpEarned") ?: 0L).toInt(),
                        lessonsCompleted = (doc?.getLong("lessonsCompleted") ?: 0L).toInt(),
                        minutesSpent = doc?.getDouble("minutesSpent") ?: 0.0
                    )
                }
                onResult(result)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    // ─────────────────────────────────────────────
    // EMAIL / PASSWORD SIGN UP
    // ─────────────────────────────────────────────
    fun signUp(name: String, email: String, password: String) {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()

        if (cleanName.isBlank() || cleanEmail.isBlank() || cleanPassword.isBlank()) {
            authState = AuthResult.Error("Name, email and password cannot be empty")
            return
        }

        authState = AuthResult.Loading
        auth.createUserWithEmailAndPassword(cleanEmail, cleanPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(cleanName)
                        .build()

                    auth.currentUser?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                val uid = auth.currentUser?.uid
                                if (uid == null) {
                                    authState = AuthResult.Error("Sign up failed: no user id")
                                    return@addOnCompleteListener
                                }
                                db.collection("users").document(uid)
                                    .set(buildNewUserDoc(cleanName, cleanEmail, null, "email"))
                                    .addOnCompleteListener { fireTask ->
                                        authState = if (fireTask.isSuccessful) {
                                            AuthResult.Success
                                        } else {
                                            AuthResult.Error(
                                                fireTask.exception?.localizedMessage
                                                    ?: "Account created, but failed to save profile"
                                            )
                                        }
                                    }
                            } else {
                                authState = AuthResult.Error(
                                    profileTask.exception?.localizedMessage ?: "Failed to save name"
                                )
                            }
                        }
                } else {
                    authState = AuthResult.Error(task.exception?.localizedMessage ?: "Sign up failed")
                }
            }
    }

    // ─────────────────────────────────────────────
    // EMAIL / PASSWORD LOGIN
    // ─────────────────────────────────────────────
    fun login(email: String, password: String) {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()

        if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
            authState = AuthResult.Error("Email and password cannot be empty")
            return
        }

        authState = AuthResult.Loading
        auth.signInWithEmailAndPassword(cleanEmail, cleanPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid
                    if (uid == null) {
                        authState = AuthResult.Error("Login failed: no user id")
                        return@addOnCompleteListener
                    }
                    ensureUserDocExists(
                        uid = uid,
                        name = user.displayName ?: "User",
                        email = user.email ?: cleanEmail,
                        photoURL = user.photoUrl?.toString(),
                        provider = "email"
                    ) { success, errorMsg ->
                        authState = if (success) {
                            AuthResult.Success
                        } else {
                            AuthResult.Error(errorMsg ?: "Failed to sync profile")
                        }
                    }
                } else {
                    authState = AuthResult.Error(task.exception?.localizedMessage ?: "Login failed")
                }
            }
    }

    // ─────────────────────────────────────────────
    // FORGOT PASSWORD
    // ─────────────────────────────────────────────
    fun sendPasswordReset(email: String) {
        val cleanEmail = email.trim()

        if (cleanEmail.isBlank()) {
            authState = AuthResult.Error("Email is required")
            return
        }

        authState = AuthResult.Loading
        auth.sendPasswordResetEmail(cleanEmail)
            .addOnCompleteListener { task ->
                authState = if (task.isSuccessful) {
                    AuthResult.Success
                } else {
                    AuthResult.Error(task.exception?.localizedMessage ?: "Failed to send reset link")
                }
            }
    }

    // ─────────────────────────────────────────────
    // CHANGE PASSWORD (reauthenticate first)
    // ─────────────────────────────────────────────
    fun changePassword(currentPassword: String, newPassword: String) {
        val cleanCurrentPassword = currentPassword.trim()
        val cleanNewPassword = newPassword.trim()

        val user = auth.currentUser
        val email = user?.email

        if (user == null || email == null) {
            authState = AuthResult.Error("User is not logged in")
            return
        }
        if (cleanCurrentPassword.isBlank() || cleanNewPassword.isBlank()) {
            authState = AuthResult.Error("All fields are required")
            return
        }

        authState = AuthResult.Loading
        val credential = EmailAuthProvider.getCredential(email, cleanCurrentPassword)

        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    user.updatePassword(cleanNewPassword)
                        .addOnCompleteListener { updateTask ->
                            authState = if (updateTask.isSuccessful) {
                                AuthResult.Success
                            } else {
                                AuthResult.Error(
                                    updateTask.exception?.localizedMessage ?: "Failed to update password"
                                )
                            }
                        }
                } else {
                    authState = AuthResult.Error("Current password is incorrect")
                }
            }
    }
// ── SKILL BREAKDOWN STATS — daily, per-module data for the Progress screen's
// new skill cards. Lives at users/{uid}/skillStats/{dateKey}, exactly like
// activityLog — a new document per calendar day, so it naturally "resets"
// every day without any cron job. Never touches xp/streak/stats.totalHours.
// ─────────────────────────────────────────────

    fun recordSpeakingConfidence(confidenceScore: Int, onDone: (Boolean) -> Unit = {}) {
        val uid = auth.currentUser?.uid ?: return onDone(false)
        db.collection("users").document(uid)
            .collection("skillStats").document(todayDateKey())
            .set(
                mapOf(
                    "speakingConfidence" to confidenceScore,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .addOnCompleteListener { onDone(it.isSuccessful) }
    }

    fun recordDictationAnswer(wasCorrect: Boolean, onDone: (Boolean) -> Unit = {}) {
        val uid = auth.currentUser?.uid ?: return onDone(false)
        val field = if (wasCorrect) "dictationCorrect" else "dictationWrong"
        db.collection("users").document(uid)
            .collection("skillStats").document(todayDateKey())
            .set(
                mapOf(
                    field to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .addOnCompleteListener { onDone(it.isSuccessful) }
    }

    fun recordGrammarQuizSolved(onDone: (Boolean) -> Unit = {}) {
        val uid = auth.currentUser?.uid ?: return onDone(false)
        db.collection("users").document(uid)
            .collection("skillStats").document(todayDateKey())
            .set(
                mapOf(
                    "grammarQuizzesSolved" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .addOnCompleteListener { onDone(it.isSuccessful) }
    }

    fun recordWordLearned(onDone: (Boolean) -> Unit = {}) {
        val uid = auth.currentUser?.uid ?: return onDone(false)
        db.collection("users").document(uid)
            .collection("skillStats").document(todayDateKey())
            .set(
                mapOf(
                    "wordsLearned" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .addOnCompleteListener { onDone(it.isSuccessful) }
    }
    // ─────────────────────────────────────────────
    // DELETE ACCOUNT (reauthenticate → delete Firestore doc → delete Auth user)
    // ─────────────────────────────────────────────
    fun deleteAccount(password: String) {
        val cleanPassword = password.trim()

        val user = auth.currentUser
        val email = user?.email

        if (user == null || email == null) {
            authState = AuthResult.Error("User is not logged in")
            return
        }
        if (cleanPassword.isBlank()) {
            authState = AuthResult.Error("Password is required")
            return
        }

        authState = AuthResult.Loading
        val credential = EmailAuthProvider.getCredential(email, cleanPassword)

        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    db.collection("users").document(user.uid).delete()
                        .addOnCompleteListener { firestoreDeleteTask ->
                            if (firestoreDeleteTask.isSuccessful) {
                                // Only delete the Auth account if Firestore cleanup worked
                                user.delete()
                                    .addOnCompleteListener { deleteTask ->
                                        authState = if (deleteTask.isSuccessful) {
                                            AuthResult.Success
                                        } else {
                                            AuthResult.Error(
                                                deleteTask.exception?.localizedMessage
                                                    ?: "Failed to delete account"
                                            )
                                        }
                                    }
                            } else {
                                // Don't touch the Auth account if Firestore delete failed
                                authState = AuthResult.Error(
                                    firestoreDeleteTask.exception?.localizedMessage
                                        ?: "Failed to delete profile data. Please try again."
                                )
                            }
                        }
                } else {
                    authState = AuthResult.Error("Incorrect password")
                }
            }
    }

    // ─────────────────────────────────────────────
    // COURSE ENROLLMENT & PROGRESS (users/{uid}/enrollments/{courseId})
    // ─────────────────────────────────────────────

    fun enrollInCourse(courseId: String, firstLessonId: String, onDone: (Boolean) -> Unit = {}) {
        val uid = auth.currentUser?.uid ?: return onDone(false)
        val ref = db.collection("users").document(uid)
            .collection("enrollments").document(courseId)

        ref.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                // already enrolled, don't reset progress
                onDone(true)
            } else {
                val data = hashMapOf(
                    "enrolledAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastLessonId" to firstLessonId,
                    "completedLessonIds" to listOf<String>()
                )
                ref.set(data).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // New enrollment — bump the "courses enrolled" stat used
                        // by the Progress screen. Doesn't affect existing users
                        // re-enrolling, since that branch returns early above.
                        db.collection("users").document(uid)
                            .update("stats.coursesEnrolledCount", FieldValue.increment(1))
                    }
                    onDone(task.isSuccessful)
                }
            }
        }.addOnFailureListener { onDone(false) }
    }

    /**
     * Marks a lesson as complete for this user.
     *
     * IMPORTANT: XP, the "lessons completed" stat, and the certificate check
     * must only ever apply the FIRST time a given lesson is completed. If the
     * user revisits a lesson they already finished and hits the button again,
     * this should still update `lastLessonId`/`updatedAt` (so "continue
     * learning" reflects what they're looking at) but must NOT add XP again
     * or double-count it on the Progress screen.
     *
     * So we read the current completedLessonIds first, and only fire
     * recordActivity()/checkAndAwardCertificate() if the lesson wasn't
     * already in that set.
     */
    fun markLessonComplete(
        courseId: String,
        lessonId: String,
        onDone: (Boolean) -> Unit = {},
        onCertificateEarned: (courseTitle: String) -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid ?: return onDone(false)
        val ref = db.collection("users").document(uid)
            .collection("enrollments").document(courseId)

        ref.get().addOnSuccessListener { doc ->
            @Suppress("UNCHECKED_CAST")
            val existingIds = (doc.get("completedLessonIds") as? List<String>)?.toSet() ?: emptySet()
            val alreadyCompleted = lessonId in existingIds

            val update = hashMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp(),
                "lastLessonId" to lessonId,
                "completedLessonIds" to FieldValue.arrayUnion(lessonId)
            )
            ref.set(update, SetOptions.merge())
                .addOnCompleteListener { task ->
                    onDone(task.isSuccessful)
                    if (task.isSuccessful && !alreadyCompleted) {
                        // First time completing this lesson — award XP, bump the
                        // "Lessons" stat, and check for a certificate. Revisiting
                        // an already-completed lesson skips all of this.
                        recordActivity(20, minutesSpent = 15.0, lessonCompleted = true)
                        checkAndAwardCertificate(uid, courseId, onCertificateEarned)
                    }
                }
        }.addOnFailureListener { onDone(false) }
    }

    fun fetchEnrollment(
        courseId: String,
        onResult: (completedLessonIds: Set<String>, lastLessonId: String?, isEnrolled: Boolean) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onResult(emptySet(), null, false)
        db.collection("users").document(uid)
            .collection("enrollments").document(courseId)
            .get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                val ids = (doc.get("completedLessonIds") as? List<String>)?.toSet() ?: emptySet()
                val last = doc.getString("lastLessonId")
                onResult(ids, last, doc.exists())
            }
            .addOnFailureListener { onResult(emptySet(), null, false) }
    }

    private fun checkAndAwardCertificate(
        uid: String,
        courseId: String,
        onCertificateEarned: (String) -> Unit
    ) {
        val course = CourseCatalog.getCourse(courseId) ?: return
        val totalLessons = course.lessons.size

        db.collection("users").document(uid)
            .collection("enrollments").document(courseId)
            .get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                val completedIds = (doc.get("completedLessonIds") as? List<String>)?.toSet() ?: emptySet()
                if (completedIds.size < totalLessons) return@addOnSuccessListener

                val certRef = db.collection("users").document(uid)
                    .collection("certificates").document(courseId)

                certRef.get().addOnSuccessListener { certDoc ->
                    if (certDoc.exists()) return@addOnSuccessListener // already earned, don't re-award

                    val certData = hashMapOf(
                        "courseTitle" to course.title,
                        "earnedAt" to FieldValue.serverTimestamp()
                    )
                    certRef.set(certData).addOnCompleteListener { certTask ->
                        if (certTask.isSuccessful) {
                            // Bump the "certificates earned" stat for the Progress screen.
                            db.collection("users").document(uid)
                                .update("stats.certsCount", FieldValue.increment(1))
                            onCertificateEarned(course.title)
                        }
                    }
                }
            }
    }

    //NEW: courseId -> earnedAt millis, for CertificatesScreen ──
    fun fetchAllCertificates(onResult: (Map<String, Long>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onResult(emptyMap())
        db.collection("users").document(uid)
            .collection("certificates")
            .get()
            .addOnSuccessListener { snapshot ->
                val result = mutableMapOf<String, Long>()
                for (doc in snapshot.documents) {
                    result[doc.id] = doc.getTimestamp("earnedAt")?.toDate()?.time ?: 0L
                }
                onResult(result)
            }
            .addOnFailureListener { onResult(emptyMap()) }
    }

    fun fetchAllEnrollments(onResult: (Map<String, Set<String>>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onResult(emptyMap())
        db.collection("users").document(uid)
            .collection("enrollments")
            .get()
            .addOnSuccessListener { snapshot ->
                val result = mutableMapOf<String, Set<String>>()
                for (doc in snapshot.documents) {
                    @Suppress("UNCHECKED_CAST")
                    val ids = (doc.get("completedLessonIds") as? List<String>)?.toSet() ?: emptySet()
                    result[doc.id] = ids
                }
                onResult(result)
            }
            .addOnFailureListener { onResult(emptyMap()) }
    }

    // ── Same data, plus each enrollment's last-updated time, so HomeScreen
    // can pick the course the user most recently studied. Cached so
    // revisiting Home doesn't flash back to "no course in progress" while
    // it reloads. ──────────────────────────────────────────────────────
    fun fetchAllEnrollmentsDetailed(onResult: (Map<String, EnrollmentSummary>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onResult(emptyMap())
        ensureCacheForCurrentUser(uid)

        // Serve what we already know immediately — no loading flash.
        cachedEnrollments?.let { onResult(it) }

        db.collection("users").document(uid)
            .collection("enrollments")
            .get()
            .addOnSuccessListener { snapshot ->
                val result = mutableMapOf<String, EnrollmentSummary>()
                for (doc in snapshot.documents) {
                    @Suppress("UNCHECKED_CAST")
                    val ids = (doc.get("completedLessonIds") as? List<String>)?.toSet() ?: emptySet()
                    val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                    result[doc.id] = EnrollmentSummary(ids, updatedAt)
                }
                cachedEnrollments = result
                onResult(result)
            }
            .addOnFailureListener {
                if (cachedEnrollments == null) onResult(emptyMap())
            }
    }

    // ─────────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────────
    fun logout() {
        auth.signOut()
        // Wipe the in-memory cache so the next signed-in user (even on the
        // same device) never briefly sees this user's cached xp/streak/enrollments.
        clearCache()
    }

    // ─────────────────────────────────────────────
    // GOOGLE SIGN-IN (Credential Manager)
    // ─────────────────────────────────────────────
    fun signInWithGoogle(context: Context, webClientId: String) {
        authState = AuthResult.Loading
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential is GoogleIdTokenCredential) {
                    val idToken = credential.idToken
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

                    val authTaskResult: FirebaseAuthResult = try {
                        auth.signInWithCredential(firebaseCredential).await()
                    } catch (e: Exception) {
                        authState = AuthResult.Error(e.localizedMessage ?: "Google sign-in failed")
                        return@launch
                    }

                    val user = auth.currentUser
                    val uid = user?.uid
                    if (uid == null) {
                        authState = AuthResult.Error("Google sign-in failed: no user id")
                        return@launch
                    }

                    val isNewUser = authTaskResult.additionalUserInfo?.isNewUser == true
                    val ref = db.collection("users").document(uid)

                    val firestoreSuccess = try {
                        if (isNewUser) {
                            ref.set(
                                buildNewUserDoc(
                                    name = user.displayName ?: "User",
                                    email = user.email ?: "",
                                    photoURL = user.photoUrl?.toString(),
                                    provider = "google"
                                )
                            ).await()
                        } else {
                            ref.update("lastActiveDate", FieldValue.serverTimestamp()).await()
                        }
                        true
                    } catch (e: Exception) {
                        authState = AuthResult.Error(e.localizedMessage ?: "Failed to sync profile")
                        false
                    }

                    if (firestoreSuccess) {
                        authState = AuthResult.Success
                    }
                } else {
                    authState = AuthResult.Error("Could not retrieve Google credential")
                }
            } catch (e: GetCredentialException) {
                authState = AuthResult.Error(e.localizedMessage ?: "Google sign-in was cancelled or failed")
            }
        }
    }

    fun resetState() {
        authState = AuthResult.Idle
    }
}