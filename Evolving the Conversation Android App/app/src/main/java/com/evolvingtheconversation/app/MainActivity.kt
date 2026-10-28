package com.evolvingtheconversation.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.evolvingtheconversation.app.data.LanguageManager
import com.evolvingtheconversation.app.data.ThemeManager
import com.evolvingtheconversation.app.navigation.NavGraph
import com.evolvingtheconversation.app.ui.theme.EvolvingTheConversationTheme
import com.evolvingtheconversation.app.ui.theme.LocalAppLanguage
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Cold-start / process-restart load only. Runtime login/logout no
            // longer depends on this — loadThemeForUser/loadLanguageForUser
            // calls were added directly in LoginScreen/CreateAccountScreen instead.
            LaunchedEffect(Unit) {
                FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                    ThemeManager.loadThemeForUser(this@MainActivity, uid)
                    LanguageManager.loadLanguageForUser(this@MainActivity, uid)
                }
            }

            val isDarkMode by ThemeManager.isDarkMode.collectAsState()
            val currentLanguage by LanguageManager.currentLanguage.collectAsState()

            // LocalAppLanguage is provided reactively here, exactly like
            // LocalIsDarkTheme already is for dark mode — every tr() call
            // recomposes instantly when currentLanguage changes. No recreate()
            // needed, no Activity restart.
            EvolvingTheConversationTheme(darkTheme = isDarkMode) {
                CompositionLocalProvider(LocalAppLanguage provides currentLanguage) {
                    NavGraph(this)
                }
            }
        }
    }
}