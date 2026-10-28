package com.evolvingtheconversation.app.data

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * App-wide dark mode state. A plain singleton (not a ViewModel) because it
 * needs to be observed from MainActivity — above the NavGraph and above any
 * screen-scoped ViewModel — as well as from SettingsScreen.
 *
 * Firestore (users/{uid}/settings/darkMode) is the source of truth.
 * UserPreferences (DataStore) is just a fast local cache, keyed per uid, so
 * app restart / device reboot doesn't flash light mode while we wait on
 * Firestore.
 */
object ThemeManager {

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Call once we know who's signed in: at app cold start (if a session
     * already exists) and right after login/signup succeed, BEFORE
     * navigating to Home.
     *
     * Loads the cached value instantly (no flash), then confirms/corrects
     * from Firestore and re-caches whatever Firestore says.
     */
    fun loadThemeForUser(context: Context, uid: String) {
        val userPrefs = UserPreferences(context)
        scope.launch {
            // 1. Instant local cache
            val cached = userPrefs.darkModeFlow(uid).first()
            _isDarkMode.value = cached

            // 2. Firestore is authoritative
            try {
                val doc = db.collection("users").document(uid).get().await()
                val settings = doc.get("settings") as? Map<*, *>
                // Doc or field missing → default to Light Mode.
                val darkMode = (settings?.get("darkMode") as? Boolean) ?: false
                _isDarkMode.value = darkMode
                userPrefs.saveDarkModeCache(uid, darkMode)
            } catch (e: Exception) {
                // Firestore unreachable — keep whatever the cache/default gave us.
            }
        }
    }

    /**
     * Call when the user flips the Settings toggle. Updates every screen
     * immediately (all of them read this same StateFlow through
     * MaterialTheme), then persists to Firestore and the local cache.
     */
    fun setDarkMode(context: Context, uid: String, enabled: Boolean) {
        _isDarkMode.value = enabled

        val userPrefs = UserPreferences(context)
        scope.launch { userPrefs.saveDarkModeCache(uid, enabled) }

        db.collection("users").document(uid)
            .set(mapOf("settings" to mapOf("darkMode" to enabled)), SetOptions.merge())
    }

    /**
     * Call on logout / account deletion so the theme doesn't leak into the
     * next login session before loadThemeForUser() runs for the new user.
     */
    fun resetToDefault() {
        _isDarkMode.value = false
    }
}