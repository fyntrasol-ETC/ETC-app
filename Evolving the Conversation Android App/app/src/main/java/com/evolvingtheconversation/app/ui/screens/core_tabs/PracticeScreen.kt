package com.evolvingtheconversation.app.ui.screens.core_tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Style
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme
import com.evolvingtheconversation.app.R

private val BlueStart     = Color(0xFF1E5EF3)
private val BlueEnd       = Color(0xFF1E3A8A)
private val SurfaceBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFFBFBFD)
private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val GreenAccent   = Color(0xFF0CBD85)
private val SurfaceCardGradientTop: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color.White
private val SurfaceCardGradientBottom: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color(0xFFF7F8FC)
private val SurfaceCardGradientBottom2: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF232328) else Color(0xFFEFF1F6)
private val CardBorderColor: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2C2C2E) else Color(0xFFF1F2F6)
private val ChevronCircleBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFF7F8FC)
private val IconBoxGradientTop: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color.White

/** Same tinted-shadow trick used across the app's other "premium" screens —
 *  bumped up for a heavier, more elevated 3D feel. */
private fun Modifier.premiumLift(
    shape: RoundedCornerShape,
    elevation: androidx.compose.ui.unit.Dp = 14.dp,
    tint: Color = Color.Black
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = tint.copy(alpha = 0.20f),
    spotColor = tint.copy(alpha = 0.30f)
)

private data class PracticeItem(
    val icon          : ImageVector,
    val iconBg        : Color,
    val iconTint      : Color,
    val titleResId    : Int,
    val subtitleResId : Int,
    val hasBadge       : Boolean = false,
    val route          : String  = ""
)

private val practiceItems = listOf(
    PracticeItem(Icons.Outlined.Mic,          Color(0xFFEEF3FF), Color(0xFF1E5EF3),
        R.string.practice_item_speaking_title,  R.string.practice_item_speaking_subtitle, hasBadge = true, route = "speaking"),
    PracticeItem(Icons.Outlined.Headphones,   Color(0xFFF3EEFF), Color(0xFF7C3AED),
        R.string.practice_item_listening_title, R.string.practice_item_listening_subtitle, route = "listening"),
    PracticeItem(Icons.Outlined.Create,       Color(0xFFEAFFF7), Color(0xFF0CBD85),
        R.string.practice_item_writing_title,   R.string.practice_item_writing_subtitle, route = "writing"),
    PracticeItem(Icons.Outlined.LibraryBooks, Color(0xFFFFF8EE), Color(0xFFFFA800),
        R.string.practice_item_grammar_title,   R.string.practice_item_grammar_subtitle, route = "grammar"),
    PracticeItem(Icons.Outlined.Style,        Color(0xFFFFF0F0), Color(0xFFE53935),
        R.string.practice_item_vocabulary_title, R.string.practice_item_vocabulary_subtitle, route = "vocabulary"),
    PracticeItem(Icons.Outlined.Adjust,       Color(0xFFEEF8FF), Color(0xFF0891B2),
        R.string.practice_item_quiz_title,      R.string.practice_item_quiz_subtitle, route = "quiz"),
)

@Composable
fun PracticeScreen(
    onSpeakingClick   : () -> Unit = {},
    onListeningClick  : () -> Unit = {},
    onWritingClick    : () -> Unit = {},
    onGrammarClick    : () -> Unit = {},
    onVocabularyClick : () -> Unit = {},
    onQuizClick       : () -> Unit = {},
    onSearchClick     : () -> Unit = {},
    onNotifClick      : () -> Unit = {},
    onTutorClick      : () -> Unit = {}   // NEW: fires when "Talk with your AI tutor" is tapped
) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(SurfaceBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 72.dp)
        ) {
            // ── TOP BAR ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        stringResource(R.string.practice_title),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 22.sp,
                        color      = TextPrimary
                    )
                    Text(
                        stringResource(R.string.practice_subtitle),
                        fontSize = 13.sp,
                        color    = TextSecondary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Search button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .premiumLift(RoundedCornerShape(12.dp), elevation = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(listOf(SurfaceCardGradientTop, SurfaceCardGradientBottom)))
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(listOf(SurfaceCardGradientTop, SurfaceCardGradientBottom2)),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSearchClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Search,
                            contentDescription = stringResource(R.string.practice_search_description),
                            tint               = TextPrimary,
                            modifier           = Modifier.size(22.dp)
                        )
                    }

                    // Notification button with badge
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .premiumLift(RoundedCornerShape(12.dp), elevation = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(listOf(SurfaceCardGradientTop, SurfaceCardGradientBottom)))
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(listOf(SurfaceCardGradientTop, SurfaceCardGradientBottom2)),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onNotifClick() },
                        contentAlignment = Alignment.Center
                    ){
                        Box {
                            Icon(
                                imageVector        = Icons.Outlined.Notifications,
                                contentDescription = stringResource(R.string.practice_notifications_description),
                                tint               = TextPrimary,
                                modifier           = Modifier.size(22.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .shadow(3.dp, CircleShape, spotColor = Color(0xFFE53935).copy(alpha = 0.6f))
                                    .background(Brush.radialGradient(listOf(Color(0xFFFF6259), Color(0xFFE53935))), CircleShape)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── AI TUTOR BANNER ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .shadow(
                        elevation = 22.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = BlueStart.copy(alpha = 0.5f),
                        spotColor = BlueStart.copy(alpha = 0.55f)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF4F8EF7), BlueStart, BlueEnd))
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f), Color.Transparent)),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onTutorClick() }   // <-- now actually does something
                    .padding(horizontal = 18.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .shadow(8.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.25f))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.12f))
                                ),
                                CircleShape
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Mic,
                            contentDescription = null,
                            tint     = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.practice_ai_tutor_title),
                            fontWeight = FontWeight.Bold,
                            fontSize   = 17.sp,
                            color      = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.practice_ai_tutor_subtitle),
                            fontSize = 12.sp,
                            color    = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint     = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── PRACTICE ITEMS ────────────────────────────────────────────────
            Column(
                modifier            = Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                practiceItems.forEach { item ->
                    PracticeItemCard(item = item, onClick = {
                        when (item.route) {
                            "speaking"   -> onSpeakingClick()
                            "listening"  -> onListeningClick()
                            "writing"    -> onWritingClick()
                            "grammar"    -> onGrammarClick()
                            "vocabulary" -> onVocabularyClick()
                            "quiz"       -> onQuizClick()
                        }
                    })
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PracticeItemCard(item: PracticeItem, onClick: () -> Unit) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .premiumLift(RoundedCornerShape(16.dp), elevation = 16.dp, tint = item.iconTint),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(1.dp, CardBorderColor),
        onClick   = onClick
    ) {
        Row(
            modifier          = Modifier
                .background(Brush.verticalGradient(listOf(SurfaceCardGradientTop, SurfaceCardGradientBottom)))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(14.dp),
                        ambientColor = item.iconTint.copy(alpha = 0.35f),
                        spotColor = item.iconTint.copy(alpha = 0.4f)
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.verticalGradient(listOf(IconBoxGradientTop, item.iconBg)))
                    .border(1.dp, item.iconTint.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint     = item.iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(item.titleResId),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        color      = TextPrimary
                    )
                    if (item.hasBadge) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GreenAccent.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier              = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint     = GreenAccent,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    stringResource(R.string.practice_ai_tutor_badge),
                                    fontSize   = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = GreenAccent
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    stringResource(item.subtitleResId),
                    fontSize   = 12.sp,
                    color      = TextSecondary,
                    lineHeight = 17.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .shadow(3.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.15f))
                    .background(ChevronCircleBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint     = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}