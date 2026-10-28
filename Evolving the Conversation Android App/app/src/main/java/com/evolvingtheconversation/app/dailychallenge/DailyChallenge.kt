package com.evolvingtheconversation.app.dailychallenge

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Matches your app's real 5 daily-task types. */
enum class DailyChallengeType(val id: String, val xpReward: Int) {
    WATCH_LESSON("watch_lesson", 30),
    GRAMMAR_QUIZ("grammar_quiz", 30),
    GENERAL_QUIZ("general_quiz", 30),
    ESSAY_WRITING("essay_writing", 30),
    AI_SPEAKING("ai_speaking", 30);

    val title: String
        get() = when (this) {
            WATCH_LESSON -> "Complete a video lesson"
            GRAMMAR_QUIZ -> "Finish a grammar quiz"
            GENERAL_QUIZ -> "Finish a quiz"
            ESSAY_WRITING -> "Write a short essay"
            AI_SPEAKING -> "Talk with your AI tutor"
        }

    val subtitle: String
        get() = when (this) {
            WATCH_LESSON -> "Watch any lesson from your enrolled course."
            GRAMMAR_QUIZ -> "Pick any grammar topic and finish the quiz."
            GENERAL_QUIZ -> "Answer a mixed quiz from the Quiz Center."
            ESSAY_WRITING -> "Submit one essay for AI feedback."
            AI_SPEAKING -> "Have a short conversation with your AI tutor."
        }

    companion object {
        fun fromId(id: String): DailyChallengeType? = entries.find { it.id == id }
    }
}

data class DailyChallenge(
    val dateKey: String,
    val type: DailyChallengeType,
    val isCompleted: Boolean = false
)

/**
 * Singleton — same shared-instance pattern as the rest of your services.
 * Also exposes `celebrationXp`: a one-shot event HomeScreen listens to so it
 * can pop the confetti + "+XP" overlay right when a challenge is completed,
 * no matter which screen the user completed it from.
 */
object DailyChallengeService {

    private val db = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null

    private val _challenge = MutableStateFlow<DailyChallenge?>(null)
    val challenge: StateFlow<DailyChallenge?> = _challenge.asStateFlow()

    // Set when the user taps "Do it now" on the Daily Challenge screen for a
    // given type. markComplete() only actually completes the challenge if this
    // matches — so organically finishing a lesson/quiz/etc. elsewhere in the
    // app, without going through the Daily Challenge screen, no longer
    // auto-completes it. Consumed (cleared) on the next markComplete call
    // regardless of outcome, so it never leaks into an unrelated later session.
    private val _activeAttempt = MutableStateFlow<DailyChallengeType?>(null)

    /** Call when the user taps "Do it now" for `type`, right before navigating away. */
    fun beginAttempt(type: DailyChallengeType) {
        _activeAttempt.value = type
    }

    // Fires once with the XP amount when a challenge is freshly completed.
    // HomeScreen sets it back to null after showing the popup.
    private val _celebrationXp = MutableStateFlow<Int?>(null)
    val celebrationXp: StateFlow<Int?> = _celebrationXp.asStateFlow()

    fun consumeCelebration() {
        _celebrationXp.value = null
    }

    fun todayKey(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        fmt.timeZone = TimeZone.getDefault()
        return fmt.format(Date())
    }

    private fun yesterdayKey(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        fmt.timeZone = TimeZone.getDefault()
        return fmt.format(cal.time)
    }

    fun listen() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dateKey = todayKey()
        if (listener != null && _challenge.value?.dateKey == dateKey) return

        listener?.remove()
        val ref = db.collection("users").document(uid)
            .collection("dailyChallenges").document(dateKey)

        listener = ref.addSnapshotListener { snapshot, _ ->
            val data = snapshot?.data
            val type = (data?.get("type") as? String)?.let { DailyChallengeType.fromId(it) }
            if (data != null && type != null) {
                val completed = data["isCompleted"] as? Boolean ?: false
                _challenge.value = DailyChallenge(dateKey, type, completed)
            } else {
                createTodayChallenge(uid, dateKey, ref)
            }
        }
    }

    private fun createTodayChallenge(uid: String, dateKey: String, ref: DocumentReference) {
        val allTypes = DailyChallengeType.entries
        val seed = (dateKey + uid).hashCode()
        val type = allTypes[Math.floorMod(seed, allTypes.size)]

        ref.set(
            mapOf(
                "type" to type.id,
                "isCompleted" to false,
                "createdAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )
        _challenge.value = DailyChallenge(dateKey, type, false)
    }

    /** Safe to call from anywhere — only does something if `type` matches today's open challenge. */
    fun markComplete(type: DailyChallengeType) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val current = _challenge.value ?: return
        if (current.type != type || current.isCompleted) return

        val ref = db.collection("users").document(uid)
            .collection("dailyChallenges").document(current.dateKey)
        ref.set(mapOf("isCompleted" to true), SetOptions.merge())

        _challenge.value = current.copy(isCompleted = true)
        _celebrationXp.value = type.xpReward

        awardXpAndStreak(uid, type.xpReward)
    }

    /**
     * Awards Daily Challenge XP and updates the day's learning streak on the
     * user's profile doc (users/{uid}.xp / .streak / .lastStreakDate). Mirrors
     * AuthViewModel.recordActivity's day-keyed logic — duplicated here since
     * this is a plain singleton with no ViewModel access. Safe to call more
     * than once per day: the streak only bumps the first time on a given day.
     */
    private fun awardXpAndStreak(uid: String, xpAmount: Int) {
        val userRef = db.collection("users").document(uid)
        val today = todayKey()
        val yesterday = yesterdayKey()

        db.runTransaction { txn ->
            val snapshot = txn.get(userRef)
            val lastStreakDate = snapshot.getString("lastStreakDate") ?: ""
            val currentStreak = (snapshot.getLong("streak") ?: 0L).toInt()

            val updates = hashMapOf<String, Any>(
                "xp" to FieldValue.increment(xpAmount.toLong())
            )
            if (lastStreakDate != today) {
                val newStreak = if (lastStreakDate == yesterday) currentStreak + 1 else 1
                updates["streak"] = newStreak
                updates["lastStreakDate"] = today
            }
            txn.update(userRef, updates)
        }
    }

    fun reset() {
        listener?.remove()
        listener = null
        _challenge.value = null
        _celebrationXp.value = null
    }
}