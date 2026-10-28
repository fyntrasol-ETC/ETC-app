package com.evolvingtheconversation.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// This creates the DataStore instance
private val Context.dataStore by preferencesDataStore(name = "user_settings")

class UserPreferences(private val context: Context) {

    companion object {
        // The key used to identify our boolean value
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    // A flow that emits the current status (defaults to false)
    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ONBOARDING_COMPLETED] ?: false
        }

    suspend fun saveOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    // ── DARK MODE CACHE (per uid) ───────────────────────────────────────
    // Just a fast local mirror of Firestore's users/{uid}/settings/darkMode,
    // so cold start / reboot doesn't flash the wrong theme. Firestore stays
    // the source of truth — see ThemeManager.
    private fun darkModeKey(uid: String) = booleanPreferencesKey("darkMode_$uid")

    fun darkModeFlow(uid: String): Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[darkModeKey(uid)] ?: false }

    suspend fun saveDarkModeCache(uid: String, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[darkModeKey(uid)] = enabled
        }
    }

    // ── LANGUAGE CACHE (per uid) ─────────────────────────────────────────
    // Same role as the dark mode cache above, but for
    // Firestore's users/{uid}/settings/language. Firestore stays the
    // source of truth — see LanguageManager. Defaults to English ("en").
    private fun languageKey(uid: String) = stringPreferencesKey("language_$uid")

    fun languageFlow(uid: String): Flow<String> = context.dataStore.data
        .map { preferences -> preferences[languageKey(uid)] ?: LanguageManager.LANG_ENGLISH }

    suspend fun saveLanguageCache(uid: String, languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[languageKey(uid)] = languageCode
        }
    }
}