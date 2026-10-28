package com.evolvingtheconversation.app.data.search

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.recentSearchDataStore by preferencesDataStore(name = "recent_searches")
private const val DELIMITER = "\u001F" // unit separator — safe, won't appear in typed queries
private const val MAX_RECENT = 10

/**
 * Persists the user's last 10 searches via Jetpack DataStore (Preferences).
 * Newest first, no duplicates — re-searching an existing term just moves it
 * back to the top.
 *
 * Scoped per-user: the preference key is namespaced with [uid] so switching
 * accounts on the same device never shows one user's recent searches to
 * another. If [uid] is null (shouldn't normally happen since Search is only
 * reachable while signed in) a shared "guest" bucket is used as a fallback
 * rather than crashing.
 */
object RecentSearchStore {

    private fun keyFor(uid: String?) = stringPreferencesKey("recent_searches_${uid ?: "guest"}")

    fun recentSearchesFlow(context: Context, uid: String?): Flow<List<String>> {
        val key = keyFor(uid)
        return context.recentSearchDataStore.data.map { prefs ->
            prefs[key]
                ?.split(DELIMITER)
                ?.filter { it.isNotBlank() }
                ?: emptyList()
        }
    }

    suspend fun addSearch(context: Context, uid: String?, query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        val key = keyFor(uid)
        context.recentSearchDataStore.edit { prefs ->
            val existing = prefs[key]
                ?.split(DELIMITER)
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            val updated = listOf(trimmed) + existing.filterNot { it.equals(trimmed, ignoreCase = true) }
            prefs[key] = updated.take(MAX_RECENT).joinToString(DELIMITER)
        }
    }
}