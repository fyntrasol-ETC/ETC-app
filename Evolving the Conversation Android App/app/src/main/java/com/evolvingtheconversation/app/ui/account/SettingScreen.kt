package com.evolvingtheconversation.app.ui.screens.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evolvingtheconversation.app.data.LanguageManager
import com.evolvingtheconversation.app.data.ThemeManager
import com.evolvingtheconversation.app.ui.components.BackButton
import com.google.firebase.auth.FirebaseAuth
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme
import com.evolvingtheconversation.app.R

private val SurfaceBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF4F6FB)
private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA4AF) else Color(0xFF6B7280)
private val BlueStart     = Color(0xFF1E5EF3)
private val DividerColor: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2C2C2E) else Color(0xFFF0F0F5)

private const val PRIVACY_POLICY_URL =
    "https://doc-hosting.flycricket.io/etc-privacy-policy/82587b25-06b4-4d99-96a2-5b11bad0e315/privacy"

@Composable
fun SettingsScreen(
    onBack             : () -> Unit = {},
    onChangePassword   : () -> Unit = {},
    onDeleteAccount    : () -> Unit = {}
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    val darkMode by ThemeManager.isDarkMode.collectAsState()
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    var notifications by remember { mutableStateOf(true) }
    var showLanguagePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // ── TOP BAR ───────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BackButton(onBack = onBack)
            Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold,
                fontSize = 20.sp, color = TextPrimary)
        }

        Spacer(Modifier.height(4.dp))

        // ── GROUP 1 — Preferences ─────────────────────────────────────────────
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_dark_mode), fontSize = 15.sp, color = TextPrimary)
                Switch(
                    checked = darkMode,
                    onCheckedChange = { enabled ->
                        uid?.let { ThemeManager.setDarkMode(context, it, enabled) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White, checkedTrackColor = BlueStart,
                        uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFD1D5DB)
                    )
                )
            }

            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_push_notifications), fontSize = 15.sp, color = TextPrimary)
                Switch(
                    checked = notifications, onCheckedChange = { notifications = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White, checkedTrackColor = BlueStart,
                        uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFD1D5DB)
                    )
                )
            }

            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))

            // ── Language row — opens a real picker ──────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable { showLanguagePicker = true }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Translate, contentDescription = null,
                        tint = TextSecondary, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.settings_language), fontSize = 15.sp, color = TextPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (currentLanguage == LanguageManager.LANG_SPANISH)
                            stringResource(R.string.language_spanish)
                        else
                            stringResource(R.string.language_english),
                        fontSize = 14.sp, color = BlueStart
                    )
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null,
                        tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── GROUP 2 — Account ─────────────────────────────────────────────────
        SettingsCard {
            // Privacy — opens the privacy policy in the browser/default handler
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri(PRIVACY_POLICY_URL) }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_privacy), fontSize = 15.sp, color = TextPrimary)
                Icon(Icons.Outlined.ChevronRight, contentDescription = null,
                    tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChangePassword() }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_change_password), fontSize = 15.sp, color = TextPrimary)
                Icon(Icons.Outlined.ChevronRight, contentDescription = null,
                    tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── GROUP 3 — Danger zone ─────────────────────────────────────────────
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDeleteAccount() }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_delete_account), fontSize = 15.sp,
                    color = Color(0xFFE53935), fontWeight = FontWeight.SemiBold)
                Icon(Icons.Outlined.ChevronRight, contentDescription = null,
                    tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showLanguagePicker) {
        LanguagePickerDialog(
            currentLanguage = currentLanguage,
            onDismiss = { showLanguagePicker = false },
            onSelect = { code ->
                android.util.Log.d("LangDebug", "onSelect fired, code=$code, uid=$uid")
                LanguageManager.setLanguage(context, uid, code)
                showLanguagePicker = false
            }
        )
    }
}

@Composable
private fun LanguagePickerDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_picker_title)) },
        text = {
            Column {
                LanguageOptionRow(
                    label = stringResource(R.string.language_english),
                    selected = currentLanguage == LanguageManager.LANG_ENGLISH,
                    onClick = { onSelect(LanguageManager.LANG_ENGLISH) }
                )
                LanguageOptionRow(
                    label = stringResource(R.string.language_spanish),
                    selected = currentLanguage == LanguageManager.LANG_SPANISH,
                    onClick = { onSelect(LanguageManager.LANG_SPANISH) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.language_picker_confirm)) }
        }
    )
}

@Composable
private fun LanguageOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 15.sp)
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        content   = { Column(content = content) }
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
fun SettingsScreenPreview() { SettingsScreen() }