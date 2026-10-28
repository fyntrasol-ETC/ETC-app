package com.evolvingtheconversation.app.ui.screens.core_tabs

import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme
import com.evolvingtheconversation.app.R

private val BlueStart           = Color(0xFF1E5EF3)
private val SurfaceBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFFBFBFD)
private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val HeaderDarkStart     = Color(0xFF0D1B2A)   // already dark — unchanged
private val AvatarGradientStart = Color(0xFF4F9CF9)
private val AvatarGradientEnd   = Color(0xFF1E5EF3)
private val PremiumGreen        = Color(0xFF0CBD85)
private val MenuIconBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF23304A) else Color(0xFFEEF3FF)
private val MenuIconGradientTop: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A3A5C) else Color(0xFFF3F7FF)
private val CardGradientBottom: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF232326) else Color(0xFFFAFBFF)
private val ChevronCircleBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFF7F8FC)
private val MenuIconTint        = BlueStart

/** Same tinted-shadow trick used across the app's other "premium" screens —
 *  bumped up for a heavier, more elevated 3D feel. */
private fun Modifier.premiumLift(
    shape: RoundedCornerShape,
    elevation: androidx.compose.ui.unit.Dp = 18.dp,
    tint: Color = Color.Black
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = tint.copy(alpha = 0.22f),
    spotColor = tint.copy(alpha = 0.32f)
)

@Composable
fun ProfileScreen(
    onLogout            : () -> Unit = {},
    onProgressClick     : () -> Unit = {},
    onCertificatesClick : () -> Unit = {},
    onPremiumClick      : () -> Unit = {},
    onSettingsClick     : () -> Unit = {},
    onHelpClick         : () -> Unit = {},
    onMenuClick         : () -> Unit = {},   // ← drawer
    onNotifClick        : () -> Unit = {}    // ← notifications screen
) {
    val scrollState = rememberScrollState()
    val currentUserName = FirebaseAuth.getInstance().currentUser?.displayName ?: stringResource(R.string.profile_default_user_name)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(SurfaceBg)
            .padding(bottom = 72.dp)
    ) {

        // ── DARK HEADER ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 22.dp,
                    shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp),
                    ambientColor = Color.Black.copy(alpha = 0.55f),
                    spotColor = Color.Black.copy(alpha = 0.55f)
                )
                .background(
                    brush = Brush.verticalGradient(listOf(Color(0xFF15263D), HeaderDarkStart)),
                    shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
                )
                .statusBarsPadding()
                .padding(top = 16.dp, bottom = 40.dp, start = 18.dp, end = 18.dp)
        ) {
            // ── Menu button (opens drawer) — glossy glass, matches HomeScreen ──
            ProfileGlassIconButton(
                icon = Icons.Default.Menu,
                description = stringResource(R.string.profile_menu_description),
                onClick = onMenuClick,
                modifier = Modifier.align(Alignment.TopStart)
            )

            // ── Title ──────────────────────────────────────────────────────
            Text(
                stringResource(R.string.profile_title),
                fontWeight = FontWeight.Bold,
                fontSize   = 20.sp,
                color      = Color.White,
                modifier   = Modifier.align(Alignment.TopCenter)
            )

            // ── Notification button (opens NotificationsScreen) — outlined bell
            // with the unread dot sitting on the bell's own corner, not the box. ──
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(44.dp)
                    .shadow(
                        elevation    = 10.dp,
                        shape        = RoundedCornerShape(14.dp),
                        ambientColor = Color.Black.copy(alpha = 0.45f),
                        spotColor    = Color.Black.copy(alpha = 0.45f)
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.32f), Color.White.copy(alpha = 0.14f))
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.6f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onNotifClick() },
                contentAlignment = Alignment.Center
            ) {
                Box {
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = stringResource(R.string.profile_notifications_description),
                        tint     = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    // Positioned on the bell's own corner rather than the box's corner.
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 2.dp)
                            .shadow(4.dp, CircleShape, spotColor = Color(0xFFFF3B30).copy(alpha = 0.7f))
                            .background(Brush.radialGradient(listOf(Color(0xFFFF6259), Color(0xFFFF3B30))), CircleShape)
                            .border(1.5.dp, HeaderDarkStart, CircleShape)
                    )
                }
            }

            // ── Avatar + name + badge ──────────────────────────────────────
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(top = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .shadow(22.dp, CircleShape, spotColor = BlueStart.copy(alpha = 0.6f), ambientColor = BlueStart.copy(alpha = 0.45f))
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF6FB1FF), AvatarGradientStart, AvatarGradientEnd)
                            )
                        )
                        .border(
                            width = 3.dp,
                            brush = Brush.linearGradient(listOf(Color.White, Color.White.copy(alpha = 0.5f))),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        currentUserName.firstOrNull()?.uppercase() ?: "U",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 40.sp,
                        color      = Color.White
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    currentUserName,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 22.sp,
                    color      = Color.White,
                    textAlign  = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(50.dp),
                            ambientColor = PremiumGreen.copy(alpha = 0.5f),
                            spotColor = PremiumGreen.copy(alpha = 0.6f)
                        )
                        .clip(RoundedCornerShape(50.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF0A3D2E), Color(0xFF0F6B4E))))
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(listOf(PremiumGreen.copy(alpha = 0.7f), Color.Transparent)),
                            shape = RoundedCornerShape(50.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(stringResource(R.string.profile_premium_star_symbol), color = PremiumGreen, fontSize = 13.sp)
                    Text(
                        stringResource(R.string.profile_premium_member_label),
                        color      = PremiumGreen,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── MENU ITEMS ────────────────────────────────────────────────────────
        Column(
            modifier            = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileMenuItem(Icons.Default.BarChart,       stringResource(R.string.profile_menu_my_progress),   onProgressClick)
            ProfileMenuItem(Icons.Default.CardMembership, stringResource(R.string.profile_menu_certificates),  onCertificatesClick)
            ProfileMenuItem(Icons.Default.EmojiEvents,    stringResource(R.string.profile_menu_premium_plans), onPremiumClick)
            ProfileMenuItem(Icons.Default.Settings,       stringResource(R.string.profile_menu_settings),      onSettingsClick)
            ProfileMenuItem(Icons.Default.HelpOutline,    stringResource(R.string.profile_menu_help_center),   onHelpClick)
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Glossy glass icon button used in the dark header ─────────────────────────
@Composable
private fun ProfileGlassIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .shadow(
                elevation    = 10.dp,
                shape        = RoundedCornerShape(14.dp),
                ambientColor = Color.Black.copy(alpha = 0.45f),
                spotColor    = Color.Black.copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.32f), Color.White.copy(alpha = 0.14f))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.6f), Color.Transparent)
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint     = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

// ─── Profile menu item ────────────────────────────────────────────────────────
@Composable
private fun ProfileMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Card(
        onClick   = onClick,
        modifier  = Modifier
            .fillMaxWidth()
            .premiumLift(RoundedCornerShape(14.dp), elevation = 12.dp),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier          = Modifier
                .background(Brush.verticalGradient(listOf(CardWhite, CardGradientBottom)))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = MenuIconTint.copy(alpha = 0.35f),
                        spotColor = MenuIconTint.copy(alpha = 0.4f)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(MenuIconGradientTop, MenuIconBg)))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(listOf(Color.White, Color.Transparent)),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint     = MenuIconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                label,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 15.sp,
                color      = TextPrimary,
                modifier   = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .shadow(3.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.15f))
                    .background(ChevronCircleBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint     = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
fun ProfileScreenPreview() { ProfileScreen() }