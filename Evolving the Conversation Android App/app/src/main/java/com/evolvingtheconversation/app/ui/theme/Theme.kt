package com.evolvingtheconversation.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.evolvingtheconversation.app.data.LanguageManager

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    secondary = Accent,
    tertiary = Warning,
    background = Background,
    surface = Surface,
    error = Error,
    onSurface = Color(0xFF1F2937)
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color.White,
    secondary = Accent,
    tertiary = Warning,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    error = Error,
    onSurface = Color(0xFFE5E7EB)
)

val LocalAppLanguage = staticCompositionLocalOf { LanguageManager.LANG_ENGLISH }

@Composable
fun tr(en: String, es: String): String {
    return if (LocalAppLanguage.current == LanguageManager.LANG_SPANISH) es else en
}

// ── Every screen's local color palette reads THIS to decide light vs dark.
// This is what was missing — screens had their own hardcoded Color() vals
// with no connection to MaterialTheme or ThemeManager at all. ──────────────
val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun EvolvingTheConversationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}