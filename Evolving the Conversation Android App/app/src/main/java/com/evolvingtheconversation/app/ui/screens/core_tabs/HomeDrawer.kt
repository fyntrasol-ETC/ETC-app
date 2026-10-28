package com.evolvingtheconversation.app.ui.screens.core_tabs

import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme
import com.evolvingtheconversation.app.R

// ─── Tokens ──────────────────────────────────────────────────────────────────
private val DrawerBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val DrawerText: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val DrawerSub: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val DrawerDivider: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFE5E7EB)
private val DrawerRed     = Color(0xFFDC2626)
private val DrawerBlue    = Color(0xFF1E5EF3)
private val DrawerBlueBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1B2540) else Color(0xFFEEF3FF)

// New — inline pale-tint icon chip backgrounds
private val CloseButtonBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFF3F4F6)
private val LogoutIconBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF3A1F1F) else Color(0xFFFFEEEE)

// ─── Nav items with routes ────────────────────────────────────────────────────
// labelResId instead of a raw String — this list is built outside a
// Composable scope, so it can't call stringResource() itself. The actual
// string is resolved down in HomeDrawerContent, which IS a Composable.
private data class DrawerItem(
    val icon      : ImageVector,
    val labelResId: Int,
    val route     : String
)

private val drawerItems = listOf(
    DrawerItem(Icons.Outlined.Home,             R.string.drawer_home,           "home"),
    DrawerItem(Icons.Outlined.MenuBook,         R.string.drawer_my_courses,     "courses"),
    DrawerItem(Icons.Outlined.AutoStories,      R.string.drawer_grammar,        "grammar"),
    DrawerItem(Icons.Outlined.Style,            R.string.drawer_vocabulary,     "vocabulary"),
    DrawerItem(Icons.Outlined.Mic,               R.string.drawer_practice,       "practice"),
    DrawerItem(Icons.Outlined.Quiz,             R.string.drawer_quizzes,        "quiz/general"),
    DrawerItem(Icons.Outlined.WorkspacePremium, R.string.drawer_certificates,   "certificates"),
    DrawerItem(Icons.Outlined.LiveTv,           R.string.drawer_live_classes,   "live_classes"),
    DrawerItem(Icons.Outlined.People,           R.string.drawer_community,      "community"),
    DrawerItem(Icons.Outlined.BarChart,         R.string.drawer_progress,       "my_progress"),
    DrawerItem(Icons.Outlined.Settings,         R.string.drawer_settings,       "settings"),
    DrawerItem(Icons.Outlined.HelpOutline,      R.string.drawer_help_center,    "help_center"),
)

// ─── Drawer ───────────────────────────────────────────────────────────────────
@Composable
fun HomeDrawerContent(
    onClose     : () -> Unit = {},
    onItemClick : (String) -> Unit = {},
    onLogout    : () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val currentUserName = FirebaseAuth.getInstance().currentUser?.displayName
        ?: stringResource(R.string.drawer_default_user_name)

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(DrawerBg)
    ) {

        // ── HEADER ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DrawerBg)
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 20.dp)
        ) {
            // X close button — top right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(38.dp)
                    .shadow(3.dp, RoundedCornerShape(10.dp),
                        spotColor = Color.Black.copy(0.12f))
                    .background(CloseButtonBg, RoundedCornerShape(10.dp))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.drawer_close_description),
                    tint     = DrawerText,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.padding(top = 4.dp)) {
                // ── App icon — gradient rounded square like reference image ───
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .shadow(
                            elevation    = 10.dp,
                            shape        = RoundedCornerShape(18.dp),
                            ambientColor = DrawerBlue.copy(0.20f),
                            spotColor    = DrawerBlue.copy(0.35f)
                        )
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF4F8EF7), Color(0xFF1535C9))
                            ),
                            RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.ChatBubble,
                        contentDescription = null,
                        tint     = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    // Reuses the app's existing R.string.app_name rather than a
                    // new key — this is literally the same app title string
                    // already declared for the manifest/launcher label.
                    text       = stringResource(R.string.app_name),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 17.sp,
                    color      = DrawerText,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text     = stringResource(R.string.drawer_user_premium_label, currentUserName),
                    fontSize = 13.sp,
                    color    = DrawerSub
                )
            }
        }

        HorizontalDivider(color = DrawerDivider, thickness = 1.dp)

        // ── MENU ITEMS (scrollable) ───────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(vertical = 10.dp)
        ) {
            drawerItems.forEach { item ->
                DrawerMenuItem(
                    icon    = item.icon,
                    label   = stringResource(item.labelResId),
                    onClick = { onItemClick(item.route) }
                )
            }
        }

        // ── DIVIDER + LOGOUT ──────────────────────────────────────────────────
        HorizontalDivider(
            color    = DrawerDivider,
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLogout() }
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(LogoutIconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Logout,
                    contentDescription = stringResource(R.string.drawer_logout_description),
                    tint     = DrawerRed,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                stringResource(R.string.drawer_logout_label),
                fontWeight = FontWeight.SemiBold,
                fontSize   = 15.sp,
                color      = DrawerRed
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ─── Single menu row ─────────────────────────────────────────────────────────
@Composable
private fun DrawerMenuItem(
    icon    : ImageVector,
    label   : String,
    onClick : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(DrawerBlueBg, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint     = DrawerBlue,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text       = label,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Medium,
            color      = DrawerText
        )
    }
}