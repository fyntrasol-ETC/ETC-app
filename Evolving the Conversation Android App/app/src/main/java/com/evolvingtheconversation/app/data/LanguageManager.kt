package com.evolvingtheconversation.app.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
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
 * App-wide language state. Same shape as ThemeManager on purpose:
 * a plain singleton (not a ViewModel) observed from MainActivity, above
 * the NavGraph, so a locale change is visible to every screen at once.
 *
 * Firestore (users/{uid}/settings/language) is the source of truth.
 * UserPreferences (DataStore) is a fast local cache, keyed per uid, so
 * app restart / device reboot doesn't flash the wrong language while we
 * wait on Firestore — identical role to darkMode's cache.
 */
object LanguageManager {

    const val LANG_ENGLISH = "en"
    const val LANG_SPANISH = "es"

    private val _currentLanguage = MutableStateFlow(LANG_ENGLISH)
    val currentLanguage: StateFlow<String> = _currentLanguage

    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Guards against loadLanguageForUser() re-running with stale cached/Firestore
    // data every time MainActivity recreates itself after a language switch.
    // Without this, recreate() -> onCreate() -> LaunchedEffect(Unit) calls
    // loadLanguageForUser() again, which can read a not-yet-flushed DataStore
    // cache and stomp the just-picked language back to the old value.
    private var loadedForUid: String? = null

    fun loadLanguageForUser(context: Context, uid: String) {
        if (loadedForUid == uid) return
        loadedForUid = uid

        val userPrefs = UserPreferences(context)
        scope.launch {
            // 1. Instant local cache
            val cached = userPrefs.languageFlow(uid).first()
            _currentLanguage.value = cached
            applyLocale(cached)

            // 2. Firestore is authoritative
            try {
                val doc = db.collection("users").document(uid).get().await()
                val settings = doc.get("settings") as? Map<*, *>
                // Doc or field missing → default to English.
                val language = (settings?.get("language") as? String) ?: LANG_ENGLISH
                _currentLanguage.value = language
                applyLocale(language)
                userPrefs.saveLanguageCache(uid, language)
            } catch (e: Exception) {
                // Firestore unreachable — keep whatever the cache/default gave us.
            }
        }
    }

    /**
     * Call when the user picks a language in Settings. Updates every
     * screen immediately, then persists to Firestore and the local cache.
     */

    fun setLanguage(context: Context, uid: String?, languageCode: String) {
        android.util.Log.d("LangDebug", "setLanguage called with $languageCode")
        _currentLanguage.value = languageCode
        applyLocale(languageCode)
        android.util.Log.d("LangDebug", "_currentLanguage now = ${_currentLanguage.value}")

        if (uid == null) return

        val userPrefs = UserPreferences(context)
        scope.launch { userPrefs.saveLanguageCache(uid, languageCode) }

        db.collection("users").document(uid)
            .set(mapOf("settings" to mapOf("language" to languageCode)), SetOptions.merge())
    }

    /**
     * Call on logout / account deletion so the language doesn't leak into
     * the next login session before loadLanguageForUser() runs for the
     * new user. Mirror of ThemeManager.resetToDefault().
     */
    fun resetToDefault() {
        _currentLanguage.value = LANG_ENGLISH
        applyLocale(LANG_ENGLISH)
        loadedForUid = null   // allow the next login to reload fresh
    }

    private fun applyLocale(languageCode: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
    }
}