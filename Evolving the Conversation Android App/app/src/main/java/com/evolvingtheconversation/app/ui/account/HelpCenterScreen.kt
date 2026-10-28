package com.evolvingtheconversation.app.ui.screens.learning

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evolvingtheconversation.app.ui.components.BackButton
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
private val BlueStart   = Color(0xFF1E5EF3)   // brand accent — unchanged
private val GreenAccent = Color(0xFF0CBD85)   // brand accent — unchanged
private val DividerColor: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2C2C2E) else Color(0xFFF0F0F5)

// Held outside a composable, so question/answer are resource IDs — resolved
// with stringResource() only where they're actually rendered below.
private data class FaqItem(val questionResId: Int, val answerResId: Int)

private val faqs = listOf(
    FaqItem(R.string.help_faq_reset_streak_question, R.string.help_faq_reset_streak_answer),
    FaqItem(R.string.help_faq_certificates_question, R.string.help_faq_certificates_answer),
    FaqItem(R.string.help_faq_offline_question, R.string.help_faq_offline_answer),
)

@Composable
fun HelpCenterScreen(onBack: () -> Unit = {}) {
    val expanded = remember { mutableStateMapOf<Int, Boolean>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // TOP BAR
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onBack = onBack)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.help_center_title), fontWeight = FontWeight.Bold,
                fontSize = 20.sp, color = TextPrimary)
        }

        Spacer(Modifier.height(4.dp))

        // FAQ SECTION
        Text(stringResource(R.string.help_faq_section_title), fontWeight = FontWeight.Bold,
            fontSize = 17.sp, color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp))

        Spacer(Modifier.height(10.dp))

        Column(
            modifier            = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            faqs.forEachIndexed { i, faq ->
                val isOpen = expanded[i] == true
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(14.dp),
                    colors    = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded[i] = !isOpen }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(stringResource(faq.questionResId), fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = TextPrimary,
                                modifier   = Modifier.weight(1f))
                            Icon(
                                if (isOpen) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                tint     = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        AnimatedVisibility(
                            visible = isOpen,
                            enter   = expandVertically(),
                            exit    = shrinkVertically()
                        ) {
                            HorizontalDivider(color = DividerColor)
                            Text(
                                stringResource(faq.answerResId),
                                fontSize   = 13.sp,
                                color      = TextSecondary,
                                lineHeight = 20.sp,
                                modifier   = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // CONTACT SUPPORT SECTION
        Text(stringResource(R.string.help_contact_support_title), fontWeight = FontWeight.Bold,
            fontSize = 17.sp, color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp))

        Spacer(Modifier.height(10.dp))

        Row(
            modifier              = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ContactCard(
                icon  = Icons.Outlined.Email,
                label = stringResource(R.string.help_contact_email),
                tint  = BlueStart,
                modifier = Modifier.weight(1f)
            )
            ContactCard(
                icon  = Icons.Outlined.Chat,
                label = stringResource(R.string.help_contact_whatsapp),
                tint  = GreenAccent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ContactCard(
    icon     : ImageVector,
    label    : String,
    tint     : Color,
    modifier : Modifier = Modifier
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick   = {}
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = label,
                tint = tint, modifier = Modifier.size(28.dp))
            Text(label, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
fun HelpCenterScreenPreview() { HelpCenterScreen() }